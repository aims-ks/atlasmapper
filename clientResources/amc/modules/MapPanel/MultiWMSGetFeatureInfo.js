/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2011 Australian Institute of Marine Science
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

/**
 * new Atlas.MapPanel.MultiGetFeatureInfo({
 *     map: mapPanel.map
 * });
 */
Atlas.MapPanel.MultiWMSGetFeatureInfo = OpenLayers.Class(OpenLayers.Control.WMSGetFeatureInfo, {
	popupBalloon: null,
	activePopupId: null,
	responses: null,
	waitingForResponse: false,

	map: null,

	initialize:function(options) {
		this.map = options.map;

		OpenLayers.Control.WMSGetFeatureInfo.prototype.initialize.apply(this, [{
			// Some layers are created only for feature requests, they are never visible.
			queryVisible: true,
			drillDown: true,
			layers: [],
			eventListeners: {
				getfeatureinfo: this.parseFeatureInfoResponse
			}
		}]);
	},

	addLayer: function(layer) {
		this.layers.push(layer);
	},

	removeLayer: function(layer) {
		var layerIndex = this.findLayerIndex(layer);
		if (layerIndex >= 0) {
			this.layers.splice(layerIndex, 1);
		}
	},

	findLayerIndex: function(layer) {
		for (var i=0; i<this.layers.length; i++) {
			// layer.params.LAYERS also works
			if (this.layers[i].id == layer.id) {
				return i;
			}
		}
		return -1;
	},

	/**
	 * Method: request
	 * Sends a GetFeatureInfo request to each WMS servers imply in the feature request.
	 *
	 * Parameters:
	 * clickPosition - {<OpenLayers.Pixel>} The position on the map where the
	 *     mouse event occurred.
	 * options - {Object} additional options for this method.
	 *
	 * Valid options:
	 * - *hover* {Boolean} true if we do the request for the hover handler
	 */
	request: function(clickPosition, options) {
		// Only show feature requests for this popup
		// If request has been sent but not yet received, they will be ignored.
		this.activePopupId = clickPosition.x + "-" + clickPosition.y;

		var layers = this.findLayers();
		if(layers.length == 0) {
			return OpenLayers.Control.WMSGetFeatureInfo.prototype.request.apply(this, [clickPosition, options]);
		}

		options = options || {};
		if(this.drillDown === false) {
			OpenLayers.Control.WMSGetFeatureInfo.prototype.request.apply(this, [clickPosition, options]);
		} else {
			this.responses = this._getOrderedLayers(layers);
			this.waitingForResponse = true;
			this._requestCount = 0;
			this._numRequests = 0;
			this.features = [];

			for(var i=0, len=layers.length; i<len; i++) {
				var layer = layers[i];

				var urls = this.getInfoServers(layer);
				for (var j=0, ulen=urls.length; j<ulen; j++) {
					var url = urls[j];
					var wmsOptions = null;

					if (layer.json['datasourceType'] == 'NCWMS') {
						wmsOptions = this.buildWMSOptions(url, layer,
							clickPosition, layer.params.FORMAT, true);
					} else {
						wmsOptions = this.buildWMSOptions(url, layer,
							clickPosition, layer.params.FORMAT);
					}
					OpenLayers.Request.GET(wmsOptions);
				}
			}
		}
	},

	_getOrderedLayers: function(layers) {
		var layerList = [];
		for(var i=0, len=layers.length; i<len; i++) {
			var layer = layers[i];

			var layerIndex = 0;
			if (!layer.isBaseLayer) {
				layerIndex = this.map.getLayerIndex(layer);
			}

			layerList.push({'index': layerIndex, 'layer': layer, evt: null});
		}
		layerList.sort(function(a, b) {
			return b.index - a.index;
		});

		return layerList;
	},

	parseFeatureInfoResponse: function(evt) {
		var popupId = evt.xy.x + "-" + evt.xy.y;

		// Only consider responses for the latest request.
		// Ignore older responses.
		if (this.waitingForResponse && this.activePopupId == popupId) {
			var layerId = evt.request.layerId;

			for (var i=0, len=this.responses.length; i<len; i++) {
				if (this.responses[i].layer.json['layerId'] == layerId) {
					this.responses[i].evt = evt;
				}
			}

			this._checkResponses();
		}
	},

	_checkResponses: function() {
		for (var i=0, len=this.responses.length; i<len && this.waitingForResponse; i++) {
			// Still waiting for the response for the top layer (or the next one if the top didn't answer anything useful)
			var evt = this.responses[i].evt;
			if (evt == null) {
				return;
			}

			// Analyse the response
			if (this.responses[i].layer.json['datasourceType'] == 'NCWMS') {
				// Keep it if it has a value.
				var value = evt.request.responseXML.getElementsByTagName('value')[0].childNodes[0].nodeValue;
				if (typeof(value) != undefined && value != 'none') {
					this.waitingForResponse = false;
					this.showPopup(evt);
				}
			} else {
				// Keep this one if it has something in the body.
				var match = evt.text.match(/<body[^>]*>([\s\S]*)<\/body>/);
				if (match && !match[1].match(/^\s*$/)) {
					this.waitingForResponse = false;
					this.showPopup(evt);
				}
			}
		}
	},

	deletePopup: function() {
		delete this.popupBalloon;
		this.popupBalloon = null;
		this.activePopupId = null;
	},

	showPopup: function(evt) {
		var layerJSon = Atlas.core.getLayerJSon(evt.request.layerId);

		var popupId = evt.xy.x + "-" + evt.xy.y;

		// Only show feature requests for the latest request.
		// If request has been sent but not yet received, they will be ignored.
		if (this.popupBalloon) {
			this.deletePopup();
		}

		// private references to popups, usable within
		// the closeBoxCallback function.
		var that = this;
		this.popupBalloon = new OpenLayers.Popup.FramedCloud(
			popupId,
			this.map.getLonLatFromPixel(evt.xy),
			null,
			" ",
			null,
			true,
			function(evt) {
				that.deletePopup();
				this.hide();
				OpenLayers.Event.stop(evt);
			}
		);
		this.map.addPopup(this.popupBalloon, true);

		var popupTitle = '<h3>'+layerJSon['title']+'</h3>';

		var newHtml = evt.text;
		var xmlRegExp = /^\s*<\?xml/;
		if (xmlRegExp.test(evt.text)) {
			newHtml = this.xmlToHtml(evt.request.responseXML);
		//} else {
			// TODO Get the text and manipulate it to allow the use of Styles... evt.text contains a Body and it's not a good idea to have multiple Bodies...
			/*
			// Keep
			var match = evt.text.match(/<body[^>]*>([\s\S]*)<\/body>/);
			var text = null;
			if (match && !match[1].match(/^\s*$/)) {
				text = match[1];
			}

			// Dynamically create an IFrame
			var iframe = document.createElement('iframe');
			iframe.src = 'about:blank';
			document.body.appendChild(iframe); // YURK!!
			var doc = iframe.contentDocument;
			// Guess what! This patch is for old versions if IE... All exceptions in JS are ALWAYS for IE!!!!!!!
			if (doc == undefined || doc == null) {
				doc = iframe.contentWindow.document;
			}
			doc.open();
			doc.write(evt.text);
			doc.close();
			*/
		}

		this.popupBalloon.setContentHTML(popupTitle + newHtml);
		this.popupBalloon.show();
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
	},

	/**
	 * Return the base URL(s) used to send the feature request
	 */
	getInfoServers: function(layer) {
		var urls = [];
		// After the normalisation of the Core,
		// this test is always true.
		if (layer && layer.json && layer.json['wmsFeatureRequestLayers']) {
			var json = layer.json['wmsFeatureRequestLayers'];
			for (var i=0; i<json.length; i++) {
				var requestedLayer = json[i];
				// After the normalisation of the Core, they all
				// look like this: {<layerId>: <featureRequestsUrl>}
				// so this test is always true.
				if (typeof(requestedLayer) !== 'string') {
					// There should be only one row.
					for (var layerName in requestedLayer) {
						if (requestedLayer.hasOwnProperty(layerName)) {
							urls.push(requestedLayer[layerName]);
						}
					}
				}
			}
		}
		return urls;
	},

	/**
	 * Method: buildWMSOptions
	 * Build an object with the relevant WMS options for the GetFeatureInfo request
	 *
	 * Parameters:
	 * url - {String} The url to be used for sending the request
	 * layers - {Array(<OpenLayers.Layer.WMS)} An array of layers
	 * clickPosition - {<OpenLayers.Pixel>} The position on the map where the mouse
	 *     event occurred.
	 * format - {String} The format from the corresponding GetMap request
	 */
	buildWMSOptions: function(url, layer, clickPosition, format, isNcwms) {
		var layerId = layer.json['layerId'];
		var layerNames = this.getLayerNames(layer, url);
		var styleNames = this.getStyleNames(layer);

		var params = {
			service: "WMS",
			version: layer.params.VERSION,
			request: "GetFeatureInfo",
			layers: layerNames,
			query_layers: layerNames,
			styles: styleNames,
			bbox: this.map.getExtent().toBBOX(null,
				layer.reverseAxisOrder()),
			height: this.map.getSize().h,
			width: this.map.getSize().w,
			format: format
		};

		if (isNcwms) {
			if (parseFloat(layer.params.VERSION) >= 1.3) {
				params.crs = this.map.getProjection();
			} else {
				params.srs = this.map.getProjection();
			}
			params.i = clickPosition.x;
			params.j = clickPosition.y;
			params.info_format = 'text/xml';
		} else {
			if (parseFloat(layer.params.VERSION) >= 1.3) {
				params.crs = this.map.getProjection();
				params.i = clickPosition.x;
				params.j = clickPosition.y;
			} else {
				params.srs = this.map.getProjection();
				params.x = clickPosition.x;
				params.y = clickPosition.y;
			}
			params.feature_count = this.maxFeatures;
			params.info_format = this.infoFormat;
		}

		OpenLayers.Util.applyDefaults(params, this.vendorParams);

		return {
			url: url,
			params: OpenLayers.Util.upperCaseObject(params),
			callback: function(request) {
				// Add the layer ID to the request.
				// It will be used to know for which layer the
				// request was for, when parsing the response.
				request.layerId = layerId;
				this.handleResponse(clickPosition, request);
			},
			scope: this
		};
	},

	handleResponse: function(xy, request) {
		var doc = request.responseXML;
		if(!doc || !doc.documentElement) {
			doc = request.responseText;
		}
		var features = this.format.read(doc);
		this.triggerGetFeatureInfo(request, xy, features);
	},

	getLayerNames: function(layer, url) {
		// After the normalisation of the Core,
		// this test is always true.
		if (layer && layer.json && layer.json['wmsFeatureRequestLayers']) {
			var json = layer.json['wmsFeatureRequestLayers'];
			var layerNames = [];
			for (var i=0; i<json.length; i++) {
				var requestedLayer = json[i];
				// After the normalisation of the Core, they all
				// look like this: {<layerId>: <featureRequestsUrl>}
				// so this test is always true.
				if (typeof(requestedLayer) !== 'string') {
					// There should be only one row.
					for (var layerName in requestedLayer) {
						if (requestedLayer.hasOwnProperty(layerName)) {
							if (url == requestedLayer[layerName]) {
								layerNames.push(layerName);
							}
						}
					}
				}
			}
			return layerNames;
		}
		// This should never append.
		return layer.params.LAYERS;
	}
});
