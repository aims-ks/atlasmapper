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

/**
 * Remove decimal value in pixel coordinates.
 * This happen when the browser window is zoomed in.
 */
Atlas.MapPanel.fixCoordinate = function(coord) {
	if (coord.x) {
		coord.x = Math.floor(coord.x);
	}
	if (coord.y) {
		coord.y = Math.floor(coord.y);
	}
	return coord;
};

Atlas.MapPanel.GetFeatureInfo = OpenLayers.Class({
	popupBalloon: null,
	activePopupId: null,
	requestedLayers: null,
	responses: null,

	activeTabId: null,
	userTabId: null,
	tabs: [],

	mapPanel: null,

	featureInfoObjects: null,

	// Override
	initialize:function(options) {
		this.mapPanel = options.mapPanel;
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
						mapPanel: this.mapPanel,
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
		evt.xy = Atlas.MapPanel.fixCoordinate(evt.xy);
		var popupId = evt.xy.x + "-" + evt.xy.y;

		// Only consider responses for the latest request.
		// Ignore older responses.
		if (this.activePopupId == popupId) {
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
		var lastEvt, featureInfoHTMLArray = [];
		for (var i=0, len=this.responses.length; i<len; i++) {
			if (this.responses[i]) {
				// Grab the answers from the layers that answered already.

				var atlasLayer = this.responses[i].layer.atlasLayer;

				var featureInfoHTML = null;
				var responseReceived = false;
				if (this.responses[i].evt) {
					lastEvt = this.responses[i].evt;
					featureInfoHTML = atlasLayer.processFeatureInfoResponse(lastEvt);
					responseReceived = true;
				}
				featureInfoHTMLArray.push({
					id: atlasLayer.json['layerId'],
					title: atlasLayer.json['title'],
					content: featureInfoHTML ? featureInfoHTML : null,
					responseReceived: responseReceived
				});
			}
		}
		this.showPopup(lastEvt, featureInfoHTMLArray);
	},

	// NOTE: It would be more efficient to modify the popup content, but there is no method in the API to do that,
	//     so I'm recreating the popup every time I receive a new response.
	showPopup: function(evt, featureInfoHTMLArray) {
		if (evt && featureInfoHTMLArray && featureInfoHTMLArray.length > 0) {
			evt.xy = Atlas.MapPanel.fixCoordinate(evt.xy);
			var popupId = evt.xy.x + "-" + evt.xy.y;

			// private references to popups, usable within
			// the closeBoxCallback function.
			var that = this;
			var nbNoneEmptyContent = 0;

			if (this.popupBalloon && this.popupBalloon.id === popupId) {
				if (featureInfoHTMLArray && featureInfoHTMLArray.length) {
					for (var i=0, len=featureInfoHTMLArray.length; i<len; i++) {
						if (featureInfoHTMLArray[i].responseReceived) {
							var content = featureInfoHTMLArray[i].content;
							if (content) {
								nbNoneEmptyContent++;
								var tabObj = document.getElementById('tab_'+featureInfoHTMLArray[i].id);
								var contentObj = document.getElementById('content_'+featureInfoHTMLArray[i].id);
								if (tabObj && contentObj && contentObj.innerHTML !== content) {
									OpenLayers.Element.removeClass(tabObj, 'hidden');
									contentObj.innerHTML = content;
								}
							} else {
								var tabObj = document.getElementById('tab_'+featureInfoHTMLArray[i].id);
								if (tabObj) {
									document.getElementById('select').removeChild(tabObj);
								}
							}
						}
					}
				}
			} else {
				var featureInfoHeaderHTML = '<select class="popup-tab" id="select">';
				var featureInfoContentHTML = '';
				if (featureInfoHTMLArray && featureInfoHTMLArray.length) {
					for (var i=0, len=featureInfoHTMLArray.length; i<len; i++) {
						if (!featureInfoHTMLArray[i].responseReceived || featureInfoHTMLArray[i].content) {
							this.tabs.push(featureInfoHTMLArray[i].id);
							featureInfoHeaderHTML += '<option value="' + featureInfoHTMLArray[i].id + '" id="tab_' + featureInfoHTMLArray[i].id + '"' + (featureInfoHTMLArray[i].content ? '' : ' class="hidden"') + '>' +
									featureInfoHTMLArray[i].title +
									'</option>';

							var content = featureInfoHTMLArray[i].content;
							featureInfoContentHTML += '<div id="content_' + featureInfoHTMLArray[i].id + '" class="popup-content">' +
									(content ? content : '') +
									'</div>';
							if (content) {
								nbNoneEmptyContent++;
							}
						}
					}

					featureInfoHeaderHTML += '</select>';
					var featureInfoHTML = '<div class="popup">'+featureInfoHeaderHTML + featureInfoContentHTML+'</div>';

					this.popupBalloon = new OpenLayers.Popup.FramedCloud(
						popupId,
						this.mapPanel.map.getLonLatFromPixel(evt.xy),
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
					this.activeTabId = null;

					// Override
					this.popupBalloon.setSize = function(contentSize) {
						// Increase the popup size slightly to avoid unnecessary scrollers
						// * Google Chrome (Windows): Need larger width/height (+15) to allow place for the scroller
						// * Firefox (Windows): Need larger width (+20) to allow place for the scroller
						// NOTE: Increasing the height too much may have unexpected results.
						OpenLayers.Popup.FramedCloud.prototype.setSize.apply(this, [
							new OpenLayers.Size(contentSize.w + 10, contentSize.h + 10)
						]);
					};

					// panMapIfOutOfView is annoying: it will fired even if the user already moved the map after requesting the popup.
					this.popupBalloon.panMapIfOutOfView = false;
					this.mapPanel.map.addPopup(this.popupBalloon, true);

					this.addEventListener(document.getElementById('select'), 'change', function(evt) {
						// Most common case (of course don't expect that to work on IE)
						var id = this.value;
						if (!id) {
							// Every browsers
							evt = evt || event;
							var target = evt.target || evt.srcElement;
							id = target.value;
						}
						if (id) {
							that.swapTab(true, id);
						}
					});
				}
			}

			if (nbNoneEmptyContent) {
				// Show the default tab, or the tab that was active before recreating the popup (if the user clicked on a tab)
				// NOTE: Show and hide set CSS display
				this.popupBalloon.show();
				this.swapTab(false);
			} else {
				this.popupBalloon.hide();
			}
		}
	},

	addEventListener: function(element, name, fct) {
		if (element.addEventListener) {
			element.addEventListener(name, fct, false);
		} else if (element.attachEvent) {
			element.attachEvent('on' + name, fct);
		}
	},

	swapTab: function(userChoice, clickedTabId) {
		var tabId = null;
		if (typeof clickedTabId !== 'undefined') {
			tabId = clickedTabId;
		} else {
			// If the user didn't click a tab, get the previous user choice or the first visible tab.
			tabId = this.userTabId;
			if (!this.selectable(tabId)) {
				for (var i=0, len=this.tabs.length; i<len; i++) {
					if (this.selectable(this.tabs[i])) {
						tabId = this.tabs[i];
						break;
					}
				}
			}
		}

		if (tabId !== null && tabId !== this.activeTabId) {
			if (this.selectable(tabId)) {
				this._activateTab(tabId);
				if (userChoice) {
					this.userTabId = tabId;
				}
			}
		} else if (userChoice && this.selectable(tabId)) {
			this.userTabId = tabId;

			// The user has clicked the active tab. This is a trick
			// to triger a popup size update.
			this.popupBalloon.updateSize();
		}
	},

	_activateTab: function(id) {
		var tab = document.getElementById('tab_' + id);
		var content = document.getElementById('content_' + id);
		if (tab && content) {
			var oldActiveTabId = this.activeTabId;
			if (oldActiveTabId !== null) {
				var oldTab = document.getElementById('tab_' + oldActiveTabId);
				if (oldTab && oldTab.selected) {
					delete oldTab.selected;
					//OpenLayers.Element.removeClass(oldTab, 'active');
				}
				var oldContent = document.getElementById('content_' + oldActiveTabId);
				if (oldContent) {
					OpenLayers.Element.removeClass(oldContent, 'popup-content-active');
				}
			}

			//OpenLayers.Element.addClass(tab, 'active');
			tab.selected = 'selected';
			OpenLayers.Element.addClass(content, 'popup-content-active');
			this.activeTabId = id;

			// Update the balloon size to fit the content of the activated tab.
			this.popupBalloon.updateSize();
			// NOTE: The method has to be called again after the content has
			//     been completely rendered (all events on popups are related
			//     to the mouse, there is no event for after render. 1/100 sec
			//     is usually enough, but sometime it need up to 1/2 sec).
			var that = this;
			window.setTimeout(function() {
				that.popupBalloon.updateSize();
			}, 10);
			window.setTimeout(function() {
				that.popupBalloon.updateSize();
			}, 500);
		}
	},

	selectable: function(tabId) {
		if (tabId === null) {
			return false;
		}
		var tabObj = document.getElementById('tab_' + tabId);
		var contentObj = document.getElementById('content_' + tabId);
		return contentObj && tabObj
				&& !OpenLayers.Element.hasClass(tabObj, 'hidden')
				&& !OpenLayers.Element.hasClass(tabObj, 'closed');
	},

	closeTab: function(closeTabId) {
		if (typeof closeTabId !== 'undefined' && closeTabId) {
			var tabObj = document.getElementById('tab_' + closeTabId);
			if (tabObj) {
				OpenLayers.Element.addClass(tabObj, 'closed');
				this.swapTab(true);
			}
		}
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
				layerIndex = this.mapPanel.map.getLayerIndex(layer);
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
	mapPanel: null,

	initialize: function(options) {
		this.atlasLayer = options.atlasLayer;
		this.featureInfoManager = options.featureInfoManager;

		// Set the format with a costum format made for the layer type.
		// format is defined in OpenLayers.Control.WMSGetFeatureInfo.format
		this.format = this.atlasLayer.getFeatureInfoResponseFormat();

		this.mapPanel = options.mapPanel;
		// map is defined in OpenLayers.Control.map
		this.map = this.mapPanel.map;

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
		return this.layers.length;
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
		clickPosition = Atlas.MapPanel.fixCoordinate(clickPosition);
		if (this.mapPanel.featureRequestsEnabled) {
			// Only show feature requests for this popup
			// If request has been sent but not yet received, they will be ignored.
			this.featureInfoManager._setActivePopup(clickPosition.x + "-" + clickPosition.y);

			var layers = this.findLayers();
			if(layers.length === 0) {
				// Remove the waiting cursor
				OpenLayers.Control.WMSGetFeatureInfo.prototype.request.apply(this, [clickPosition, options]);
				return;
			}

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
		}
	},

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
