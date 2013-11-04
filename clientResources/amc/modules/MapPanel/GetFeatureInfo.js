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

/**
 * CHANGE IN PLAN - THIS CLASS INIT ONE GetFeatureInfo PER LAYERS TO BE ABLE TO FILTER THEM (One response per layer)
 *
 * This class can handle multiple type of layers (if layer have the attribute atlasLayer).
 * It extends from the OpenLayers.Control.WMSGetFeatureInfo, because most of the feature request
 * are done the same way (and it's the only GetFeatureInfo class in OpenLayers).
 *
 * new Atlas.MapPanel.GetFeatureInfo({
 *     map: mapPanel.map
 * });
 */
Atlas.MapPanel.GetFeatureInfo = OpenLayers.Class({
	popupBalloon: null,
	activePopupId: null,
	requestedLayers: null,
	responses: null,
	waitingForResponse: false,

	map: null,

	featureInfoObjects: null,

	// Override
	initialize:function(options) {
		this.map = options.map;
		this.featureInfoObjects = {};
	},

	addLayer: function(atlasLayer) {
		if (atlasLayer) {
			var key = atlasLayer.getFeatureInfoLayerID();
			if (key !== null) {
				// Do not add the same layer twice
				if (typeof(this.featureInfoObjects[key]) === 'undefined') {
					var featureInfo = new Atlas.MapPanel.SingleLayerFeatureInfo({
						featureInfoManager: this,
						map: this.map,
						atlasLayer: atlasLayer,
						eventListeners: {
							getfeatureinfo: this.parseFeatureInfoResponse,
							scope: this
						}
					});

					featureInfo.activate();

					this.featureInfoObjects[key] = featureInfo;
				} else {
					this.featureInfoObjects[key].addLayer(atlasLayer);
				}
			}
		}
	},

	removeLayer: function(atlasLayer) {
		if (atlasLayer) {
			var key = atlasLayer.getFeatureInfoLayerID();
			if (key !== null && typeof(this.featureInfoObjects[key]) === 'undefined') {
				var featureInfo = this.featureInfoObjects[key];
				featureInfo.removeLayer(atlasLayer);
				if (featureInfo.layerCount() <= 0) {
					if (featureInfo) {
						featureInfo.deactivate();
						featureInfo.destroy();
					}
					delete this.featureInfoObjects[key];
				}
			}
		}
	},

	// This event is called once per layer, for each feature requests.
	parseFeatureInfoResponse: function(evt) {
		var popupId = evt.xy.x + "-" + evt.xy.y;

		// Only consider responses for the latest request.
		// Ignore older responses.
		if (this.waitingForResponse && this.activePopupId == popupId) {
			var layerId = evt.request.layerId;

			for (var i=0, len=this.responses.length; i<len; i++) {
				if (this.responses[i]) {
					if (this.responses[i].layer.atlasLayer.json['layerId'] == layerId) {
						this.responses[i].evt = evt;
					}
				}
			}

			this._checkResponses();
		}
	},

	_checkResponses: function() {
		for (var i=0, len=this.responses.length; i<len && this.waitingForResponse; i++) {
			if (this.responses[i]) {
				// Still waiting for the response for the top layer (or the next one if the top didn't answer anything useful)
				var evt = this.responses[i].evt;
				if (evt == null) {
					return;
				}
				var atlasLayer = this.responses[i].layer.atlasLayer;

				var featureInfoHTML = atlasLayer.processFeatureInfoResponse(evt);
				if (featureInfoHTML) {
					this.waitingForResponse = false;
					this.showPopup(evt, featureInfoHTML);
				} else {
					// The response is empty, remove it to avoid parsing it again with the next layers.
					this.responses[i] = null;
				}
			}
		}
	},

	showPopup: function(evt, featureInfoHTML) {
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
			featureInfoHTML,
			null,
			true,
			function(evt) {
				that.deletePopup();
				this.hide();
				OpenLayers.Event.stop(evt);
			}
		);
		this.map.addPopup(this.popupBalloon, true);

		this.popupBalloon.show();
	},

	deletePopup: function() {
		delete this.popupBalloon;
		this.popupBalloon = null;
		this.activePopupId = null;
		this._resetResponses();
	},

	_resetResponses: function() {
		this.responses = null;
		this.requestedLayers = null;
	},

	_setActivePopup: function(newPopupId) {
		if (newPopupId != this.activePopupId) {
			this.activePopupId = newPopupId;
			this._resetResponses();
		}
	},

	_addRequestedLayer: function(layer) {
		if (this.responses == null) {
			this.responses = [];
		}
		if (this.requestedLayers == null) {
			this.requestedLayers = [];
		}
		this.requestedLayers.push(layer);

		this.responses = this._createSortedLayerResponses(this.requestedLayers);
	},

	_createSortedLayerResponses: function(layers) {
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
	}
});



Atlas.MapPanel.SingleLayerFeatureInfo = OpenLayers.Class(OpenLayers.Control.WMSGetFeatureInfo, {
	atlasLayer: null,
	featureInfoManager: null,

	initialize: function(options) {
		this.atlasLayer = options.atlasLayer;
		this.featureInfoManager = options.featureInfoManager;

		// Set the format with a costum format made for the layer type.
		// format is defined in OpenLayers.Control.WMSGetFeatureInfo.format
		this.format = this.atlasLayer.getFeatureInfoResponseFormat();

		// map is defined in OpenLayers.Control.map
		this.map = options.map;

		OpenLayers.Control.WMSGetFeatureInfo.prototype.initialize.apply(this, [{
			// Some layers are created only for feature requests, they are never visible.
			queryVisible: true,
			drillDown: true,
			layers: [this.atlasLayer.layer],
			eventListeners: options.eventListeners
		}]);
	},

	addLayer: function(atlasLayer) {
		this.layers.push(atlasLayer.layer);
	},

	removeLayer: function(atlasLayer) {
		var layerIndex = this.findLayerIndex(atlasLayer.layer);
		if (layerIndex >= 0) {
			this.layers.splice(layerIndex, 1);
		}
	},

	layerCount: function() {
		this.layers.length;
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
	// Override
	request: function(clickPosition, options) {
		// Only show feature requests for this popup
		// If request has been sent but not yet received, they will be ignored.
		this.featureInfoManager._setActivePopup(clickPosition.x + "-" + clickPosition.y);

		var layers = this.findLayers();
		if(layers.length == 0) {
			// Remove the waiting cursor
			return OpenLayers.Control.WMSGetFeatureInfo.prototype.request.apply(this, [clickPosition, options]);
		}

		this.featureInfoManager.waitingForResponse = true;

		options = options || {};
		if(this.drillDown === false) {
			OpenLayers.Control.WMSGetFeatureInfo.prototype.request.apply(this, [clickPosition, options]);
		} else {
			// All candidate layers should be equivalent
			var layer = layers[0];
			this.featureInfoManager._addRequestedLayer(layer);

			var urls = this.getInfoServers(layer);
			for (var j=0, ulen=urls.length; j<ulen; j++) {
				var url = urls[j];
				var urlOptions = layer.atlasLayer.getFeatureInfoURL(url, layer,
					clickPosition, layer.params.FORMAT);

				if (urlOptions != null) {
					urlOptions.callback = function(request) {
						// Add the layer ID to the request.
						// It will be used to know for which layer the
						// request was for, when parsing the response.
						request.layerId = layer.atlasLayer.json['layerId'];
						this.handleResponse(clickPosition, request);
					};
					urlOptions.scope = this;

					OpenLayers.Request.GET(urlOptions);
				}
			}
		}
	},

	/*
	request: function(clickPosition, options) {
		// Only show feature requests for this popup
		// If request has been sent but not yet received, they will be ignored.
		this.featureInfoManager._setActivePopup(clickPosition.x + "-" + clickPosition.y);

		var layers = this.findLayers();
		if(layers.length == 0) {
			return OpenLayers.Control.WMSGetFeatureInfo.prototype.request.apply(this, [clickPosition, options]);
		}

		this.featureInfoManager.waitingForResponse = true;

		options = options || {};
		if(this.drillDown === false) {
			OpenLayers.Control.WMSGetFeatureInfo.prototype.request.apply(this, [clickPosition, options]);
		} else {
			this.featureInfoManager._addResponses(layers);

			for(var i=0, len=layers.length; i<len; i++) {
				var layer = layers[i];

				var urls = this.getInfoServers(layer);
				for (var j=0, ulen=urls.length; j<ulen; j++) {
					var url = urls[j];
					var urlOptions = layer.atlasLayer.getFeatureInfoURL(url, layer,
						clickPosition, layer.params.FORMAT);

					if (urlOptions != null) {
						urlOptions.callback = function(request) {
							// Add the layer ID to the request.
							// It will be used to know for which layer the
							// request was for, when parsing the response.
							request.layerId = layer.atlasLayer.json['layerId'];
							this.handleResponse(clickPosition, request);
						};
						urlOptions.scope = this;

						OpenLayers.Request.GET(urlOptions);
					}
				}
			}
		}
	},
	*/

	/**
	 * Method: findLayers
	 * Internal method to get the layers, independent of whether we are
	 *     inspecting the map or using a client-provided array
	 */
	// Override (to remove the filter on WMS layers - this class is good for all type of layers)
	findLayers: function() {
		var candidates = this.layers || this.map.layers;
		var layers = [];
		var layer, url;
		for(var i = candidates.length - 1; i >= 0; --i) {
			layer = candidates[i];
			if(!this.queryVisible || layer.getVisibility()) {
				url = OpenLayers.Util.isArray(layer.url) ? layer.url[0] : layer.url;
				// if the control was not configured with a url, set it
				// to the first layer url
				if(this.drillDown === false && !this.url) {
					this.url = url;
				}
				if(this.drillDown === true || this.urlMatches(url)) {
					layers.push(layer);
				}
			}
		}
		return layers;
	},

	/**
	 * Return the base URL(s) used to send the feature request
	 */
	getInfoServers: function(layer) {
		var urls = [];
		// After the normalisation of the Core,
		// this test is always true.
		if (layer && layer.atlasLayer && layer.atlasLayer.json && layer.atlasLayer.json['wmsFeatureRequestLayers']) {
			var json = layer.atlasLayer.json['wmsFeatureRequestLayers'];
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


	// Override
	handleResponse: function(xy, request) {
		var doc = request.responseXML;
		if(!doc || !doc.documentElement) {
			doc = request.responseText;
		}
		var features = this.format.read(doc);
		this.triggerGetFeatureInfo(request, xy, features);
	}
});
