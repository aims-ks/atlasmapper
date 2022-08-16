/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2012 Australian Institute of Marine Science
 *
 *  Contact: Gael Lafond <g.lafond@aims.gov.au>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

// Namespace declaration (equivalent to Ext.namespace("Atlas.Layer");)
window["Atlas"] = window["Atlas"] || {};
window["Atlas"]["Layer"] = window["Atlas"]["Layer"] || {};

Atlas.Layer.AbstractLayer = OpenLayers.Class({
	MAX_URL_LENGTH: 40,

	// OpenLayers layer object
	layer: null,
	parent: null,

	// MapPanel instance in which the layer is used (GeoExtMapPanel, EmbeddedMapPanel, etc.)
	mapPanel: null,

	// JSON Layer, as defined by the layer catalog
	json: null,

	// Cached layer extent
	extent: null,

	supportLoadEvents: true,
	loaded: false,

	// Save state, using URL parameters
	layerState: null,

	// Use cache when possible (see WMS.js)
	useCache: false,

	/**
	 * Private Constructor: Atlas.Layer.AbstractLayer
	 *
	 * Parameters:
	 * jsonLayer - {Object} Hashtable of layer attributes
	 * mapPanel - {Object} Instance of the MapPanel in which the layer is used
	 */
	initialize: function(mapPanel, jsonLayer, parent) {
		this.mapPanel = mapPanel;
		this.json = jsonLayer;
		this.parent = parent;

		// Set default options values (the fields that appears on the Option panel)
		// See: modules/Info/OptionPanel.js
		if (this.json['options']) {
			this.json['olParams'] = this.json['olParams'] || {};
			for (var i=0, len=this.json['options'].length; i < len; i++) {
				var option = this.json['options'][i];
				if (option['name'] && typeof(option['defaultValue']) !== 'undefined') {
					this.json['olParams'][option['name'].toUpperCase()] = option['defaultValue'];
				}
			}
		}
	},


	/**
	 * Add attributes and functions to OpenLayers layers without extending every classes individually.
	 */
	setLayer: function(layer) {
		layer.atlasLayer = this;
		layer.hideInLegend = false;

		if (typeof(this.json['path']) !== 'undefined') {
			layer.path = this.json['path'];
		}

		// TODO Remove this after implementing Save State
		if (!layer.isBaseLayer && typeof(this.json['selected']) === 'boolean') {
			layer.visibility = this.json['selected'];
			delete(this.json['selected']);
		}

		this.layer = layer;

		//this._registerEvents();

//		if (this.parent != null && typeof(this.parent.addChild) === 'function') {
//			this.parent.addChild(this);
//		}
	},

/*
	_registerEvents: function() {
		this.layer.events.on({
			'loadstart': function(event) {
				this.loaded = false;
			},
			'loadend': function(event) {
				this.loaded = true;
			},
			// This event is not triggered anywhere, but maybe someday it will be...
			'loadcancel': function(event) {
				// Loading as been cancel, ack as if it was loaded...
				this.loaded = true;
			},
			scope: this
		});
	},
	_unregisterEvents: function(node) {
		// TODO Unregistered only the ones added here
		this.layer.events.remove('loadstart');
		this.layer.events.remove('loadend');
		this.layer.events.remove('loadcancel');
	},
*/

	getServiceUrl: function(layerParams, newParams) {
		if (this.json == null) {
			return null;
		}

		var serviceUrl = this.json['serviceUrl'];
		if (this.useCache &&
				this.canUseWebCache(layerParams, newParams)) {

			serviceUrl = this.json['webCacheUrl'];
			this.setWebCacheParameters(newParams ? newParams : layerParams);
		} else {
			this.setDirectParameters(newParams ? newParams : layerParams);
		}
		return serviceUrl;
	},

	// Override to set specific parameter according to the service used.
	// See: WMS.js
	setWebCacheParameters: function(params) {},
	setDirectParameters: function(params) {},

	canUseWebCache: function(layerParams, newParams) {
		if (this.json == null || !this.json['webCacheUrl'] || typeof this.json['cached'] === 'undefined' || !this.json['cached']) {
			return false;
		}

		var supportedParams = this.json['webCacheSupportedParameters'];
		// IE6 can't use Web Cache (GeoServer Web Cache send blank tiles as PNG, even when requested as GIF)
		// Equivalent to "if (Ext.isIE6)" without Ext dependencies
		var userAgent = navigator.userAgent.toLowerCase();
		if (!/opera/.test(userAgent) && /msie 6/.test(userAgent)) { return false; }

		// Check the current parameter for a unsupported one,
		// considering the value of the new parameter, case insensitive...
		for(var paramName in layerParams) {
			if(layerParams.hasOwnProperty(paramName)) {
				var newValue = layerParams[paramName];
				if (newParams) {
					for(var newParamName in newParams) {
						if(newParams.hasOwnProperty(newParamName) && newParamName && newParamName.toUpperCase() === paramName.toUpperCase()) {
							newValue = newParams[newParamName];
						}
					}
				}
				if (newValue && !this._webCacheSupportParam(paramName, newValue, supportedParams)) {
					return false;
				}
			}
		}

		// Check the new parameters for a unsupported one.
		if (newParams) {
			for(var newParamName in newParams) {
				if(newParams.hasOwnProperty(newParamName)) {
					newValue = newParams[newParamName];
					if (newValue && !this._webCacheSupportParam(newParamName, newValue, supportedParams)) {
						return false;
					}
				}
			}
		}

		return true;
	},

	_webCacheSupportParam: function(paramName, value, supportedParams) {
		if (!supportedParams || supportedParams.length <= 0) {
			// Supported parameters is not set:
			// The Web Cache server support everything
			return true;
		}

		for (var i=0, leni=supportedParams.length; i < leni; i++) {
			var supportedParam = supportedParams[i];
			if (supportedParam.toUpperCase() === paramName.toUpperCase()) {
				// Exception for STYLES
				if (paramName.toUpperCase() === 'STYLES') {
					if (typeof(this.json['styles']) === 'undefined') {
						return false;
					}
					var foundStyle = null;
					for (var j=0, lenj=this.json['styles'].length; j < lenj; j++) {
						if (typeof(this.json['styles'][j]['name']) !== 'undefined' && this.json['styles'][j]['name'] === value) {
							foundStyle = this.json['styles'][j];
						}
					}

					if (foundStyle === null ||
							typeof(foundStyle['cached']) === 'undefined' ||
							foundStyle['cached'] === null) {
						return false;
					}
					return !!foundStyle['cached'];
				}
				return true;
			}
		}
		return false;
	},

	// Add the functions setHideInLegend/getHideInLegend to all layers.
	// It would just be too much trouble if all layer class had
	// to be extend only to add those functions.
	setHideInLegend: function(hide) {
		if (this.layer != null) {
			this.layer.hideInLegend = hide;
			this.mapPanel.map.events.triggerEvent("changelayer", {
				layer: this.layer,
				property: "hideInLegend"
			});
			this.mapPanel.ol_fireEvent('legendVisibilityChange', {layer: this.layer});
		}
	},

	getHideInLegend: function() {
		if (this.layer == null) {
			return true;
		}
		return this.layer.hideInLegend;
	},

	canBeLocated: function() {
		return (this.getExtent() != null);
	},

	locate: function() {
		var bounds = this.getExtent();
		if (this.canBeLocated() && bounds != null && this.mapPanel && this.mapPanel.map) {
			// Parameters
			// bounds  {<OpenLayers.Bounds>|Array} If provided as an array, the array should consist of four values (left, bottom, right, top).
			// closest {Boolean} Find the zoom level that most closely fits the specified bounds.  Note that this may result in a zoom that does not exactly contain the entire extent.  Default is false.
			this.mapPanel.map.zoomToExtent(bounds, this.getLocateClosest());
		} else {
			alert("This layer can not be located");
		}
	},

	getLocateClosest: function() {
		return true;
	},

	getTitle: function() {
		if (this.json == null) {
			return null;
		}
		return this.json['title'] || this.json['layerName'] || this.json['layerId'];
	},

	/**
	 * Return a description for the layer. It can be used as a tooltip,
	 * or as a HTML page for the layer information.
	 * <b>Title (or ID if no title)</b>
	 * <b>Key:</b> value (for additional info such as year, author, etc.)
	 * <b>Key:</b> value
	 * ...
	 * Description (the abstract found in the GetCapabilities document)
	 */
	getDescription: function() {
		if (!this.json) {
			return null;
		}

		var desc = '<b>' + this._safeHtml(this.getTitle()) + '</b>';

		var previewUrl = this.getPreviewUrl();
		if (previewUrl) {
			desc += '<div class="descriptionLayerPreview"><img src="' + previewUrl + '" alt="Layer preview" /></div>';
		}

		if (this.json['description']) {
			desc += '<div class="description">';
			var format = (this.json['descriptionFormat'] || 'wiki').toLowerCase();
			if (format === 'text') {
				desc += this._urlsToHTML(this._lineBreaksToHTML(this._safeHtml(this.json['description'])));
			} else if (format === 'wiki') {
				desc += this._wikiFormat(this._safeHtml(this.json['description']));
			} else if (format === 'html') {
				desc += this.json['description'];
			}
			if (this.json['systemDescription']) {
				desc += '<div class="systemDescription">';
				desc += this.json['systemDescription'];
				desc += '</div>';
			}
			desc += '</div>';
		}

		if (this.json['layerId']) {
			desc += '<div class="descriptionLayerId">Layer id: <em>' + this.json['layerId'] + '</em></div>';
		}

		return desc;
	},

	getDownloadLinks: function() {
		var html = null;
		var downloadLinks = this.json['downloadLinks'];
		if (downloadLinks !== null) {
			if (typeof downloadLinks === 'string') {
				// backward compatibility
				html = this._wikiFormat(this._safeHtml(this.json['downloadLinks']));
			} else if (typeof downloadLinks === 'object') {
				if (Atlas.conf['downloadLoggerServiceUrl']) {
					var iframeName = this.getUniqueWindowName('downloadLogger_');
					html = '<ul class="downloadLoggerLinksList">';
					for (var url in downloadLinks) {
						if (downloadLinks.hasOwnProperty(url)) {
							var label = downloadLinks[url];
							html += '<li>';
							var downloadLoggerUrl = Atlas.conf['downloadLoggerServiceUrl'];
							downloadLoggerUrl += (downloadLoggerUrl.indexOf('?') >= 0 ? '&' : '?') +
									'url=' + encodeURIComponent(url) + '&' +
									'label=' + encodeURIComponent(label) + '&' +
									'theme=am';
							html += '<a href="'+downloadLoggerUrl+'" target="'+iframeName+'">'+this._safeHtml(label)+'</a>'
							html += '</li>';
						}
					}
					html += '</ul>';
					html += '<iframe name="'+iframeName+'" class="downloadLogger" frameBorder="0"></iframe>';
				} else {
					html = '<ul class="downloadLinksList">';
					for (var url in downloadLinks) {
						if (downloadLinks.hasOwnProperty(url)) {
							var label = downloadLinks[url];
							html += '<li><a href="'+url+'" target="_blank">'+this._safeHtml(label)+'</a></li>'
						}
					}
					html += '</ul>';
				}
			}
		}
		return html;
	},

	getUniqueWindowName: function(prefix) {
		var i=0, name = prefix + i;
		// Loop until we find a window that is not in used (maximum of 1000 windows allowed at once).
		while (typeof window[name] !== 'undefined' && i < 1000) {
			name = prefix + ++i;
		}
		return name;
	},

	// to override
	getPreviewUrl: function() {
		return null;
	},

	_safeHtml: function(input) {
		if (input === null || typeof input !== 'string') { return null; }
		return input.replace(/&/gi, "&amp;").replace(/</gi, "&lt;").replace(/>/gi, "&gt;");
	},

	/**
	 * Change all URLs in the input to a HTML link, and truncate long URL to
	 * maxUrlLength characters.
	 *
	 * See RFC 1738 for valid URL schema:
	 *     http://www.apps.ietf.org/rfc/rfc1738.html#sec-5
	 */
	_urlsToHTML: function(input, popup) {
		var newInput = '';
		var lastIndex = 0;

		if (typeof(popup) == 'undefined') {
			popup = true;
		}

		// Enumeration of chars that are not allow in the URLs. RFC 1738
		// The word boundary can not be used here (it includes brackets), so it's easier to simply do this enumeration (better control)
		// RFC 1738 - Allowed: alphanumerics, the special characters "$-_.+!*'()," and reserved characters ";", "/", "?", ":", "@", "=", "&", "#" and "%".
		// http://www.apps.ietf.org/rfc/rfc1738.html#sec-5
		//     5. BNF for specific URL schemes
		var urlChar = "a-zA-Z0-9\\$\\-_\\.\\+\\!\\*'\\(\\),;\\/\\?:@=&#%";

		// Enumeration of chars that are allow as the last char of a URLs (and not allow before the www, for partial URLs).
		// For example, this is useful when a sentence end with a URL: Click here www.url.com. ==> Click here <a href="...">www.url.com</a>.
		// There is no RFC for this, it's just common sense logic.
		var urlEndingChar = "a-zA-Z0-9/";

		// pattern:
		//     Well formed URL
		//      protocol   "://"   URL chars (multiple times)   URL ending char
		//     ( ---------------------------- 1 ------------------------------ )
		//     ( - 2 - )
		//         Example: http://google.com?search=abc
		//             1: http://google.com?search=abc
		//             2: http
		//     OR
		//     URL without explicit protocol (partial URL)
		//      start-of-string  OR  URL ending char   "www"   URL chars (multiple times)   URL ending char
		//     ( ---------------- 3 --------------- ) ( ------------------------ 4 ----------------------- )
		//         Example: www.google.com?search=abc
		//             3: [Space]
		//             4: www.google.com?search=abc
		var pattern = new RegExp("((sftp|ftp|http|https|file)://["+urlChar+"]+["+urlEndingChar+"])|([^"+urlEndingChar+"]|^)(www\\.["+urlChar+"]+["+urlEndingChar+"])", "gim");

		var matches = null;
		while (matches = pattern.exec(input)) {
			var url = null;
			var displayUrl = null;
			var prefix = "";

			var noProtocolUrl = matches[4];
			var protocolUrl = matches[1];

			if (noProtocolUrl != null && noProtocolUrl != "") {
				displayUrl = noProtocolUrl;
				url = "http://" + noProtocolUrl;
				prefix = matches[3];
			} else {
				displayUrl = protocolUrl;
				url = protocolUrl;
			}

			// Building the HTML link
			if (displayUrl != null && url != null) {
				var link = prefix + "<a href=\"" + url + "\"";
				if (popup) {
					link += " target=\"_blank\"";
				}
				link += ">" + this._truncateURLForDisplay(displayUrl) + "</a>";

				// Add the text from the last link to the beginning of this one
				newInput += input.substring(lastIndex, matches.index);

				// Add the link
				newInput += link;
			}

			lastIndex = matches.index + matches[0].length;
		}
		newInput = newInput + input.substring(lastIndex);

		return newInput;
	},

	_truncateURLForDisplay: function(url) {
		var maxUrlLength = this.MAX_URL_LENGTH || 40;

		if (maxUrlLength == 1) {
			return ".";
		}

		if (maxUrlLength == 2) {
			return "..";
		}

		if (maxUrlLength > 0 && maxUrlLength < url.length) {
			var beginningLength = Math.round((maxUrlLength-3) * 3.0/4);
			var endingLength = maxUrlLength - beginningLength - 3; // 3 is for the "..."
			if (beginningLength > 1 && endingLength == 0) {
				beginningLength--;
				endingLength = 1;
			}
			return url.substring(0, beginningLength) + "..." + url.substring(url.length - endingLength);
		}

		return url;
	},

	/**
	 * Strip HTML and apply some basic wiki format:
	 * Bold: *expression*
	 * Italic: /expression/
	 * Underline: _expression_
	 * Strikeout: -expression-
	 * Header (1st level): ==Heading 1==
	 * Header (2nd level): ===Heading 2===
	 * Header (3rd level): ====Heading 3====
	 * Bullet list:
	 *     * First element
	 *     ** Sub element
	 *     * 2nd element
	 * Numbered list:
	 *     # First element (appear as "1. First element")
	 *     ## Sub element  (appear as "    1. Sub element")
	 *     # 2nd element   (appear as "2. 2nd element")
	 * URL: [[http://google.com/|Google]]
	 *
	 * See: https://github.com/lahdekorpi/Wiky.php/blob/master/wiky.inc.php
	 *
	 * @param String, using some Wiki format syntax.
	 * @return HTML String.
	 * @private
	 */
	_wikiFormat: function(input) {
		if (input == null) { return null; }

		var wikiFormater = new Atlas.Utils.WikiFormater();
		return wikiFormater.format(input);
	},

	_lineBreaksToHTML: function(input) {
		// Replace all 3 types of line breaks with a HTML line break.
		return input.replace(/(\r\n|\n|\r)/gim, '<br/>\n');
	},

	getExtent: function() {
		if (this.extent == null) {
			this.extent = this.computeExtent();
		}
		return this.extent;
	},

	computeExtent: function() {
		var bounds = null;
		if (this.layer && this.layer.atlasLayer && this.layer.atlasLayer.json && this.layer.atlasLayer.json['layerBoundingBox']) {
			bounds = this.getExtent(this.layer.atlasLayer.json, this.mapPanel);
		}

		if (bounds == null && typeof(this.layer.getDataExtent) === 'function') {
			// getDataExtent() return already reprojected bounds
			bounds = this.layer.getDataExtent();
		}

		return bounds;
	},

	getExtent: function(json, mapPanel) {
		var bounds = null;
		if (json && json['layerBoundingBox']) {
			// Bounds order in JSon: left, bottom, right, top
			var boundsArray = json['layerBoundingBox'];

			// Bounds order as requested by OpenLayers: left, bottom, right, top
			// NOTE: Re-projection can not work properly if the top or bottom overpass 85
			var bounds = new OpenLayers.Bounds(
				(boundsArray[0] < -180 ? -180 : (boundsArray[0] > 180 ? 180 : boundsArray[0])),
				(boundsArray[1] < -85 ? -85 : (boundsArray[1] > 85 ? 85 : boundsArray[1])),
				(boundsArray[2] < -180 ? -180 : (boundsArray[2] > 180 ? 180 : boundsArray[2])),
				(boundsArray[3] < -85 ? -85 : (boundsArray[3] > 85 ? 85 : boundsArray[3]))
			);

			if (bounds != null && mapPanel != null) {
				bounds = bounds.transform(mapPanel.defaultLonLatProjection, mapPanel.map.getProjectionObject());
			}
		}

		return bounds;
	},

	applyOlOverrides: function(config, overrides) {
		if (overrides == null) {
			return config;
		}
		for (var key in overrides) {
			if (overrides.hasOwnProperty(key)) {
				config[key] = overrides[key];
			}
		}
		return config;
	},

	isLoading: function() {
		if (!this.supportLoadEvents) {
			return false;
		}
		if (!this.layer.visibility) {
			return false;
		}
		return !this.loaded;
	},

	isDummy: function() {
		return this instanceof Atlas.Layer.Dummy;
	},

	isGroup: function() {
		return this instanceof Atlas.Layer.Group;
	},

	hasRealLayer: function() {
		return typeof(this.layer) != 'undefined' &&
			this.layer != null &&
			!this.isDummy() &&
			!this.isGroup();
	},


	/**
	 * Feature Info methods.
	 */

	/**
	 * Method: getFeatureInfoURL
	 * Build an object with the relevant options for the GetFeatureInfo request
	 *
	 * Parameters:
	 * url - {String} The url to be used for sending the request
	 * layers - {Array(<OpenLayers.Layer.WMS)} An array of layers
	 * clickPosition - {<OpenLayers.Pixel>} The position on the map where the mouse
	 *     event occurred. This can be transform in a LonLat obj (in the unit of the map)
	 *     using map.getLonLatFromPixel(clickPosition)
	 * format - {String} The format from the corresponding GetMap request
	 *
	 * return {
	 *     url: String
	 *     params: { String: String }
	 * }
	 */
	// Need to be overridden
	getFeatureInfoURL: function(url, layer, clickPosition, format) {
		return null;
	},

	/**
	 * Return an OpenLayers.Format used to parse the response for this type of layer.
	 * (default: OpenLayers.Format.WMSGetFeatureInfo)
	 * http://dev.openlayers.org/docs/files/OpenLayers/Format-js.html
	 * @return {OpenLayers.Format}
	 */
	// Need to be overridden
	getFeatureInfoResponseFormat: function() {
		return null;
	},

	/**
	 * Return the HTML chunk that will be displayed in the balloon.
	 * @param responseEvent The response event. The text response is available with responseEvent.text,
	 *     and the XML DOM tree is accessible with responseEvent.request.responseXML. The helper method
	 *     this.xmlToHtml can be used to format the XML DOM tree.
	 * @return {String} The HTML content of the feature info balloon, or null if the layer info should not be shown.
	 */
	// Need to be overridden
	processFeatureInfoResponse: function(responseEvent) {
		return (responseEvent.text ? responseEvent.text : this.xmlToHtml(responseEvent.request.responseXML));
	},

	// Can be overridden, is special cases (for example; to send only one request for a ArcGIS service).
	getFeatureInfoLayerID: function() {
		return this.json['layerId'];
	},

	// Can be overridden
	getAttributions: function() {
		return null;
	},

	// Can be overridden
	setOptions: function(optionsPanel) {
		// optionsPanel.addOption(this, {definition of an ExtJS form field});
	},

	// Determine if the layer need a reload by comparing the values
	// of the new parameters with the one in the layer URL.
	// layer: OpenLayers layer
	// newParams: Map of key value pairs
	// private
	setParameters: function(newParams) {
		if (this.layer == null || typeof(this.layer.mergeNewParams) !== 'function') {
			return;
		}

		// Change the URL of the layer to use the appropriate server
		// NOTE: setUrl must be called before mergeNewParams (mergeNewParams reload the tiles, setUrl don't; when called in wrong order, tiles are requested against the wrong server)
		var needReload = false;

		var newUrl = this.getServiceUrl(this.layer.params, newParams);
		if (newUrl != this.layer.url) {
			this.layer.setUrl(newUrl);
			needReload = true;
		}

		// Loop through all params and check if it's value is the
		// same as the one set for the layer. If not, ask for a
		// layer reload (stop as soon as one is different)
		if (!needReload) {
			var currentValue = null;
			Ext.iterate(newParams, function(key, value) {
				currentValue = this.getParameter(key, null);
				if (currentValue != value) {
					needReload = true;
					// Stop the iteration
					return false;
				}
			}, this);
		}

		if (needReload) {
			// Merge params add the new params or change the values
			// of existing one and reload the tiles.
			this.layer.mergeNewParams(newParams);
		}
	},

	getParameter: function(param, defaultValue) {
		var value = this.getRawParameter(param, defaultValue);
		if (value) {
			return decodeURIComponent(value);
		}
		return value;
	},

	getRawParameter: function(param, defaultValue) {
		if (!this.layer || !this.layer.params) {
			return defaultValue;
		}

		if (typeof(this.layer.params[param]) !== 'undefined') {
			return this.layer.params[param];
		}

		// Try with to uppercase the parameter; OpenLayers usually put all parameters in uppercase.
		var uppercaseParam = param.toUpperCase();
		if (typeof(this.layer.params[uppercaseParam]) !== 'undefined') {
			return this.layer.params[uppercaseParam];
		}

		return defaultValue;
	},


	/**
	 * HELPERS
	 */

	/**
	 * Return true is the responseText parameter contains non empty HTML data.
	 * @param responseText
	 * @return {Boolean}
	 */
	isHtmlResponse: function(responseText) {
		if (responseText == null || responseText == '') {
			return false;
		}

		var match = responseText.match(/<body[^>]*>([\s\S]*)<\/body>/);
		return (match && !match[1].match(/^\s*$/));
	},

	// http://www.w3schools.com/Xml/xml_dom.asp
	xmlToHtml: function(xml) {
		var output = '';
		var lon = parseFloat(xml.getElementsByTagName('longitude')[0].childNodes[0].nodeValue);
		var lat = parseFloat(xml.getElementsByTagName('latitude')[0].childNodes[0].nodeValue);

		if (typeof(lon) != 'undefined' && typeof(lat) != 'undefined') {
			output += 'Longitude: ' + lon.toFixed(5) + '<br/>' +
					'Latitude: ' + lat.toFixed(5) + '<br/>';
		}

		var featureInfoNodes = xml.getElementsByTagName('FeatureInfo');
		for (var i=0; i<featureInfoNodes.length; i++) {
			output += this._parseXmlTag(featureInfoNodes[i], false);
		}

		return output;
	},
	_parseXmlTag: function(tag, printLabel) {
		// Undefined tagName => text tag between tags
		if (tag == null || typeof(tag.tagName) == 'undefined') {
			return '';
		}

		var output = '';
		if (printLabel) {
			output += tag.tagName + ': ';
		}
		if (this._hasRealChildNodes(tag)) {
			for (var i=0; i<tag.childNodes.length; i++) {
				output += this._parseXmlTag(tag.childNodes[i], true);
			}
		} else {
			// The only nodes are text nodes
			if (tag.tagName == 'time') {
				output += this._formatDate(tag.childNodes[0].nodeValue);
			} else {
				output += tag.childNodes[0].nodeValue;
			}
		}
		output += '<br />';
		return output;
	},
	// The date is returned by ncWMS is in the following format:
	// 2011-03-30T14:00:00.000Z
	// Javascript do not provide any good tools to format dates.
	// It's easier to simply do some string manipulations.
	_formatDate: function(dateStr) {
		return dateStr.replace('T', ' ').replace('\.000Z', '');
		/*
		var date = new Date(dateStr.replace('Z', ''));
		return date.getDate() + '/' +
			(date.getMonth()+1) + '/' +
			date.getFullYear() + ' ' +
			date.getHours() + ':' +
			date.getMinutes();
		*/
	},
	_hasRealChildNodes: function(tag) {
		for (var i=0; i<tag.childNodes.length; i++) {
			if (typeof(tag.childNodes[i].tagName) != 'undefined') {
				return true;
			}
		}
		return false;
	}
});
