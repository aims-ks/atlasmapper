/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2012 Australian Institute of Marine Science
 *
 *  Contact: Gael Lafond <g.lafond@aims.org.au>
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
	// OpenLayers layer object
	layer: null,

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

	/**
	 * Private Constructor: Atlas.Layer.AbstractLayer
	 *
	 * Parameters:
	 * jsonLayer - {Object} Hashtable of layer attributes
	 * mapPanel - {Object} Instance of the MapPanel in which the layer is used
	 */
	initialize: function(mapPanel, jsonLayer) {
		this.mapPanel = mapPanel;
		this.json = jsonLayer;
	},


	/**
	 * Add attributes and functions to OpenLayers layers without extending every classes individually.
	 */
	extendLayer: function(layer) {
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

		//this._registerEvents();

		return layer;
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

	getServiceUrl: function(layerParams) {
		if (this.json == null) {
			return null;
		}

		var serviceUrl = this.json['wmsServiceUrl'];
		if (this.json['webCacheUrl'] &&
			this._canUseWebCache(layerParams)) {
			serviceUrl = this.json['webCacheUrl'];
		}
		return serviceUrl;
	},

	_canUseWebCache: function(layerParams) {
		if (this.json == null || (typeof(this.json['cached']) !== 'undefined' && !this.json['cached'])) {
			return false;
		}

		var supportedParams = this.json['webCacheSupportedParameters'];
		// IE6 can't use Web Cache (GeoServer Web Cache send blank tiles as PNG, even when requested as GIF)
		// Equivalent to "if (Ext.isIE6)" without Ext dependencies
		var userAgent = navigator.userAgent.toLowerCase();
		if (!/opera/.test(userAgent) && /msie 6/.test(userAgent)) { return false; }

		for(var paramName in layerParams){
			if(layerParams.hasOwnProperty(paramName)){
				if (layerParams[paramName] && !this._webCacheSupportParam(paramName, supportedParams)) {
					// console.log('Can NOT use Web Cache ['+paramName+']');
					return false;
				}
			}
		}
		// console.log('Can use Web Cache');
		return true;
	},

	_webCacheSupportParam: function(paramName, supportedParams) {
		if (!supportedParams || supportedParams.length <= 0) {
			// Supported parameters is not set:
			// The Web Cache server support everything
			return true;
		}

		for (var i=0; i < supportedParams.length; i++) {
			var supportedParam = supportedParams[i];
			if (supportedParam.toUpperCase() === paramName.toUpperCase()) {
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
		if (bounds != null) {
			this.mapPanel.map.zoomToExtent(bounds, true);
		} else {
			alert("This layer can not be located");
		}
	},

	getTitle: function() {
		if (this.json == null) {
			return null;
		}
		return this.json['title'] || this.json['layerName'] || this.json['layerId'];
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
			// Bounds order in JSon: left, bottom, right, top
			var boundsArray = this.layer.atlasLayer.json['layerBoundingBox'];

			// Bounds order as requested by OpenLayers: left, bottom, right, top
			// NOTE: Re-projection can not work properly if the top or bottom overpass 85
			var bounds = new OpenLayers.Bounds(
				(boundsArray[0] < -180 ? -180 : (boundsArray[0] > 180 ? 180 : boundsArray[0])),
				(boundsArray[1] < -85 ? -85 : (boundsArray[1] > 85 ? 85 : boundsArray[1])),
				(boundsArray[2] < -180 ? -180 : (boundsArray[2] > 180 ? 180 : boundsArray[2])),
				(boundsArray[3] < -85 ? -85 : (boundsArray[3] > 85 ? 85 : boundsArray[3]))
			);
		}
		if (bounds == null && typeof(this.layer.getDataExtent) === 'function') {
			bounds = this.layer.getDataExtent();
		}

		if (bounds != null && this.mapPanel != null) {
			bounds = bounds.transform(this.mapPanel.defaultLonLatProjection, this.mapPanel.map.getProjectionObject());
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
