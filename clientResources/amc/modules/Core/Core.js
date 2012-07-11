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

// Namespace declaration (equivalent to Ext.namespace("Atlas");)
window["Atlas"] = window["Atlas"] || {};

// The core play the role of an Event manager
// Core can't use any ExtJS object since it may be use in an embedded map.
Atlas.Core = OpenLayers.Class({
	// Main event types, used by modules that are usually loaded.
	EVENT_TYPES: [
		'createNewMap', 'mapAdded', 'removeMap', 'mapRemoved'
	],
	MAX_URL_LENGTH: 40,

	// Used to parse client config
	// NOTE: The version must match the version in the server /src/main/java/au/gov/aims/atlasmapperserver/ConfigManager.java
	CURRENT_MAIN_CONFIG_VERSION: 1.1,
	CURRENT_LAYER_CONFIG_VERSION: 1.1,

	// The OpenLayers event object, set in initialize function
	events: null,
	configFileUrl: null,
	live: false,

	mapPanels: null,

	layersFileUrl: null,

	// Map <layer ID, layer JSon config>
	layersJSonCache: null,
	layerInfoServiceUrl: null,

	// Optional string (usually a timestamp) used to ensure that the browser
	// download the latest files rather than using its cache.
	version: "",

	mapCounter: 0,

	initialize: function(configUrl, layersFileUrl, version, live) {
		this.events = new OpenLayers.Events(this, null,
			this.EVENT_TYPES);

		this.mapPanels = [];

		if (live) {
			this.live = true;
		}
		if (version) {
			this.version = version;
		}

		if (configUrl) {
			this.configFileUrl = configUrl;
			OpenLayers.Request.GET({
				url: this.configFileUrl,
				params: {
					ver: this.version // timestamp of last generation, to avoid caching
				},
				scope: this,
				success: this.preLoad,
				failure: function (result, request) {
					// TODO Error on the page
					alert('The application has failed to load its configuration.');
				}
			});
		} else {
			// TODO Error on the page
			alert("The application can not be loaded.\nNo config file specified...");
		}

		if (layersFileUrl) {
			this.layersFileUrl = layersFileUrl;
		}
	},

	// Private
	preLoad: function(response) {
		var jsonResponse = eval("(" + response.responseText + ")");
		if (jsonResponse && typeof(jsonResponse.success) != 'undefined') {
			if (jsonResponse.success) {
				Atlas.conf = jsonResponse.data;
				if (typeof(Atlas.conf.version) != 'undefined' && Atlas.conf.version > this.CURRENT_MAIN_CONFIG_VERSION) {
					var err = "The version of the client configuration file ("+Atlas.conf.version+") is not supported by this client (support up to version: "+this.CURRENT_MAIN_CONFIG_VERSION+").";
					alert(err);
					throw err;
				}
			} else {
				// TODO Error on the page
				alert('The application has failed to read its configuration.' +
					(jsonResponse.errors ? '\n'+jsonResponse.errors.reason : ''));
				return;
			}
		} else {
			Atlas.conf = jsonResponse
		}

		this.normalizeConfiguration();

		if (Atlas.conf && Atlas.conf['layerInfoServiceUrl']) {
			this.layerInfoServiceUrl = Atlas.conf['layerInfoServiceUrl'];
		}

		// PROXY
		// http://trac.osgeo.org/openlayers/wiki/FrequentlyAskedQuestions#HowdoIsetupaProxyHost
		// 1. Install the OpenLayer proxy openlayers/examples/proxy.cgi in /usr/lib/cgi-bin
		// 2. Configure
		//     2.1 It the variable allowedHosts, add 'e-atlas.org.au'
		// 3. Restart Apache
		//     3.1 sudo service apache2 stop
		//     3.2 sudo service apache2 start
		// 4. Test: http://localhost/cgi-bin/proxy.cgi?url=http://e-atlas.org.au
		// ScriptAlias /cgi-bin/ /usr/lib/cgi-bin/
		if (Atlas.conf && Atlas.conf['proxyUrl']) {
			// proxyUrl should looks like this: "/cgi-bin/proxy.cgi?url="
			OpenLayers.ProxyHost = Atlas.conf['proxyUrl'];
		}

		// Load the layers from the main config to the cache.
		this.loadLayersCache();

		var that = this;
		this.ol_on('createNewMap', function() {
			that.createNewMapPanel();
		});
		this.ol_on('removeMap', function(evt) {
			that.removeMapPanel(evt.index);
		});

		this.afterLoad();
	},

	// To override
	afterLoad: function() {},

	// Events Listener
	ol_on: function(event, fct, scope) {
		// Add the event type if it's not already added.
		// This has to be done for event that are not declared at
		// initialisation; event that are not part of this.EVENT_TYPES.
		// Since external modules may trigger new type of event,
		// it's not possible to define them all.
		this.events.addEventType(event);

		var evtObj = {};
		evtObj[event] = fct;
		if (typeof(scope) !== 'undefined') {
			evtObj.scope = scope;
		}
		this.events.on(evtObj);
	},

	// Fire Events
	ol_fireEvent: function(event, attributes) {
		this.events.triggerEvent(event, attributes);
	},

	getMapPanel: function(index) {
		if (typeof(this.mapPanels[index]) === 'undefined') {
			return null;
		}
		return this.mapPanels[index];
	},

	createNewMapPanel: function() {
		// TODO KML_ALLOW_JAVASCRIPT IN CONFIG
		var newMapPanel = new Atlas.MapPanel({
			mapId: (this.mapCounter++),
			KML_ALLOW_JAVASCRIPT: true
		});

		return this._addMapPanel(newMapPanel, true);
	},

	createNewEmbeddedMapPanel: function(renderTo) {
		// TODO KML_ALLOW_JAVASCRIPT IN CONFIG
		var newMapPanel = new Atlas.MapPanel({
			renderTo: renderTo,
			embedded: true,
			mapId: (this.mapCounter++),
			KML_ALLOW_JAVASCRIPT: true
		});

		return this._addMapPanel(newMapPanel, false);
	},

	_addMapPanel: function(newMapPanel, addMarker) {
		var mapIndex = this.mapPanels.length;
		var that = this;
		newMapPanel.map.events.register('movestart', this, function()    { that.moveStart(mapIndex);      });
		newMapPanel.map.events.register('moveend',   this, function()    { that.moveEnd(mapIndex);        });
		newMapPanel.map.events.register('mousemove', this, function(evt) { that.mouseMove(mapIndex, evt); });
		newMapPanel.map.events.register('mouseover', this, function(evt) { that.mouseOver(mapIndex, evt); });
		newMapPanel.map.events.register('mouseout',  this, function(evt) { that.mouseOut(mapIndex, evt);  });
		newMapPanel.ol_on('layerAdded', function() { that._moveMarkersOnTop(); });

		this.mapPanels.push(newMapPanel);

		if (addMarker) {
			this.initMarker(mapIndex); // Create a layer to show a cursor when the mouse move over an other map.
		}

		this.ol_fireEvent('mapAdded', {mapPanel: newMapPanel});

		return newMapPanel;
	},

	removeMapPanel: function(index) {
		if (typeof(this.mapPanels[index]) === 'undefined') {
			return null;
		}

		var removedMapPanel = this.mapPanels[index];
		this.mapPanels.splice(index,1);
		this.markers.splice(index,1);
		this.markersLayer.splice(index,1);

		// TODO do something with the removedMapPanel; remove from the browser, free memory, etc.
		this.ol_fireEvent('mapRemoved', {mapPanel: removedMapPanel});

		return removedMapPanel;
	},




	// Multi-Map events

// TODO Make an array of object to handle mapPanels, markers and markersLayer (they can get out of sync quite easily)

	movestarted: false,
	moving: false,
	markersLayer: new Array(), // TODO Init this in the constructor... otherwise that will be screwed if someone try to instantiate more than 1 core
	markers: new Array(),

	/**
	 * Initialise a layer containing an image of a cross to be
	 * display in this map when the cursor move over an other map.
	 */
	initMarker: function(mapIndex) {
		var map = this.mapPanels[mapIndex].map;

		var markerLayer = new OpenLayers.Layer.Markers("Marker");
		markerLayer.setVisibility(false);
		markerLayer.displayInLayerSwitcher = false;
		map.addLayer(markerLayer);

		this.markers[mapIndex] = new OpenLayers.Marker(new OpenLayers.LonLat(0,0),
			new OpenLayers.Icon('resources/images/cross.png', new OpenLayers.Size(20, 20), new OpenLayers.Pixel(-10, -10))
		);
		markerLayer.addMarker(this.markers[mapIndex]);
		// The marker should be automatically associated with its map,
		// but this association do not always occur...
		this.markers[mapIndex].map = map;

		this.markersLayer[mapIndex] = markerLayer;
	},

	moveStart: function(mapIndex) {
		this.movestarted = true;
		this._hideMarkers();
		return(false);
	},

	moveEnd: function(mapIndex) {
		if (this.moving) {
			return false;
		}
		this.moving = true;

		// When a map has move, move all other maps as well
		var masterMap = this.mapPanels[mapIndex].map;
		for (var i=0; i<this.mapPanels.length; i++) {
			if (i != mapIndex) {
				var anotherMap = this.mapPanels[i].map;
				anotherMap.setCenter(
					masterMap.getCenter().clone().transform(masterMap.getProjectionObject(), anotherMap.getProjectionObject()),
					masterMap.getZoom()
				);
			}
		}

		this.moving = false;
		this.movestarted = false;
		this._showMarkers(mapIndex);
		return false;
	},

	mouseMove: function(mapIndex, evt) {
		var masterMap = this.mapPanels[mapIndex].map;
		for (var i=0; i<this.markers.length; i++) {
			if (i != mapIndex) {
				var anotherMap = this.mapPanels[i].map;
				// Move the Marker of anotherMap to the same lon lat coordinate as the mouse pointer in the masterMap.
				// Pixels coordinates can not be used directly because the 2 view ports may be of different dimensions,
				// so the position [0,0] (in pixel) can be in position [100,0] on the other view port.
				this.markers[i].moveTo(
					anotherMap.getLayerPxFromLonLat(
						masterMap.getLonLatFromViewPortPx(evt.xy)));
			}
		}
		return false;
	},

	mouseOver: function(mapIndex, evt) {
		if (!this.movestarted) {
			this._showMarkers(mapIndex);
		}
		return false;
	},

	mouseOut: function(mapIndex, evt) {
		this._hideMarkers();
		return false;
	},

	_hideMarkers: function() {
		for (var i=0; i<this.markersLayer.length; i++) {
			this.markersLayer[i].setVisibility(false);
		}
	},

	_showMarkers: function(excludeIndex) {
		for (var i=0; i<this.markersLayer.length; i++) {
			if (i != excludeIndex) {
				this.markersLayer[i].setVisibility(true);
			}
		}
	},

	_moveMarkersOnTop: function() {
		for (var i=0; i<this.markersLayer.length; i++) {
			this.mapPanels[i].map.setLayerIndex(this.markersLayer[i], 1000);
		}
	},

	/**
	 * Recursive function
	 * (it is recursive because the Ajax request are asynchronous;
	 * the recursivity occurred when the Ajax request receive its answer)
	 *
	 * Request layers to the server. As soon as a response is received,
	 * the layers info are put in the cache and an other request is sent
	 * with the missing layers, if any... until all layers have been
	 * received. It also request children layers for layer groups, recursively.
	 *
	 * Parameter:
	 * layerIds: Array of layer ID or Alias ID
	 * callback: Function to call with the Array of JSon layers
	 * attemptsLeft: Private parameter (ignored for public call); number of similar requests allow to request
	 *     the same layers (the method won't run into an infinite loop, il will
	 *     give up after 5 failing attempts).
	 */

	// Private
	requestLayersJSon: function(layerIds, callback, attemptsLeft) {
		var that = this;

		if (typeof(attemptsLeft) !== 'number') {
			attemptsLeft = 5;
		}

		if (layerIds == null || layerIds.length <= 0) {
			return;
		}

		// Layers received
		var cachedLayersJSon = [];
		// Layers requested but missing from the response
		var missingLayerIds = [];
		// Layers required by an other layer (children)
		var missingDependencies = [];

		// Recursive function that add missing children to the missingDependencies collection.
		// (it is recursive because the layer structure is a tree)
		// NOTE: It only add the children that are not present in the cache.
		// NOTE: It can not collect the children of layers that are not present in the cache,
		//     so it usually have to be called more than once to be sure all dependencies
		//     has been loaded (requestLayersJSon is recursive).
		var addMissingDependencies = function(layerJSon) {
			if (layerJSon) {
				var layerIds = layerJSon['layers'];
				if (layerIds != null && layerIds.length > 0) {
					for (var i=0; i<layerIds.length; i++) {
						var dependencyId = layerIds[i];
						var dependencyLayerJSon = that.layersJSonCache[dependencyId];
						if (dependencyLayerJSon) {
							addMissingDependencies(dependencyLayerJSon);
						} else {
							missingDependencies.push(dependencyId);
						}
					}
				}
			}
		};

		// Parse the layersIds parameter to fill the collections used to
		// request the missing layers (layers not present in the cache).
		for (var i=0,len=layerIds.length; i<len; i++) {
			var layerId = layerIds[i];
			var layerJSon = this.layersJSonCache[layerId];
			if (layerJSon) {
				cachedLayersJSon.push(layerJSon);
				addMissingDependencies(layerJSon);
			} else {
				missingLayerIds.push(layerId);
			}
		}

		// Prepare and send the Ajax request to the server to get the missing layers (layers that are not in the cache)
		if ((missingDependencies.length > 0 || missingLayerIds.length > 0) && attemptsLeft > 0) {
			var url = null;
			var params = {};
			// When the client do not has access to the layer Info Service (access to the AtlasMapper server),
			// it load the whole catalog at once.
			if (this.layerInfoServiceUrl) {
				url = this.layerInfoServiceUrl;
				params = {
					client: Atlas.conf['clientId'],
					live: this.live,
					layerIds: missingLayerIds.concat(missingDependencies),
					ver: this.version
				};
			} else if (this.layersFileUrl) {
				// Load the whole layer catalog
				url = this.layersFileUrl;
				params = {
					ver: this.version
				};
			}

			if (url) {
				var that = this;

				// Function run when Ajax receive its answer.
				var received = function (response) {
					// Decode the response
					var jsonResponse = eval("(" + response.responseText + ")");
					if (jsonResponse) {
						var newLayers = null;
						if (this.layerInfoServiceUrl) {
							// Reception of the requested layers
							newLayers = jsonResponse.data;
						} else {
							// Reception of all layers at once - for client without a Info Service URL
							// The first request will takes a while, the subsequentes will be fast
							// since all layers will be cached.
							newLayers = jsonResponse;
						}
						if (newLayers) {
							that.loadNewLayersCache(newLayers);
						}
						// The layers should be part of the cache now
					}
					if (missingDependencies.length > 0) {
						// We have new layers to request. Reset the attempts counter.
						that.requestLayersJSon(layerIds, callback);
					} else {
						// The request has failed to return all requested layers.
						// This situation should occurred very rarely.
						that.requestLayersJSon(layerIds, callback, attemptsLeft-1);
					}
				};

				OpenLayers.Request.GET({
					url: url,
					params: params,
					scope: this,
					success: received,
					failure: function (result, request) {
						// TODO Error on the page
						alert('The application has failed to load the requested layers');
					}
				});
			}
		} else if (cachedLayersJSon.length > 0) {
			// Call the callback with found layers
			// NOTE: The callback is only called once all layers has been received, or when
			if (callback) {
				callback(cachedLayersJSon);
			}

			if (missingLayerIds.length > 0) {
				// TODO Error on the page
				alert('The application has failed to load the layers ['+missingLayerIds.join()+']');
			}
		}
	},

	getLayerJSon: function(layerId) {
		if (this.layersJSonCache[layerId]) {
			return this.layersJSonCache[layerId];
		}
		return null;
	},

	loadLayersCache: function() {
		if (!this.layersJSonCache) {
			// Initialise the cache
			this.layersJSonCache = {};

			if (Atlas.conf && Atlas.conf['layers']) {
				this.loadNewLayersCache(Atlas.conf['layers']);
			}
		}
	},

	loadNewLayersCache: function(newLayers) {
		// Load all layers, from the main config, in the cache
		var defaults = {};//newLayers['defaults'] ? newLayers['defaults'] : {};

		// The following will not work for Array across frames (not an issue here)
		// See: http://perfectionkills.com/instanceof-considered-harmful-or-how-to-write-a-robust-isarray/
		var isArray = newLayers.constructor == Array;

		if (isArray) {
			for(var i=0; i<newLayers.length; i++){
				var layerJSon = newLayers[i];
				this.loadLayerCache(layerJSon, layerJSon.layerId, defaults);
			}
		} else {
			for(var layerId in newLayers){
				if(newLayers.hasOwnProperty(layerId) /*&& layerId != 'defaults'*/){
					var layerJSon = newLayers[layerId];
					this.loadLayerCache(layerJSon, layerId, defaults);
				}
			}
		}
	},

	loadLayerCache: function(layerJSon, layerId, defaultsJSon) {
		if (!layerJSon) {layerJSon = {};}
		if (typeof(layerJSon) == 'string') {
			layerJSon = {'title': layerJSon};
		}

		if (typeof(layerJSon.version) != 'undefined' && layerJSon.version > this.CURRENT_LAYER_CONFIG_VERSION) {
			var err = "The version of the layer configuration ("+layerJSon.version+") is not supported by this client (support up to version: "+this.CURRENT_LAYER_CONFIG_VERSION+").";
			alert(err);
			throw err;
		}

		// Apply default settings to the current layer
		// if they are now already set.
		if (defaultsJSon) {
			// Equivalent to Ext.applyIf(layerJSon, defaultsJSon);
			if(layerJSon){
				for(var defaultProp in defaultsJSon){
					if(typeof(layerJSon[defaultProp]) === 'undefined'){
						layerJSon[defaultProp] = defaultsJSon[defaultProp];
					}
				}
			}
		}

		if (!layerJSon['layerId'] && layerId) {
			layerJSon['layerId'] = layerId;
		}
		if (!layerId) {
			layerId = layerJSon['layerId'];
		}

		layerJSon = this.normalizeLayerJSon(layerJSon);

		this.layersJSonCache[layerId] = layerJSon;
		// Link the JSon config to all aliases as well
		var aliasIds = layerJSon['aliasIds'];
		if (aliasIds) {
			for(var i = 0, len = aliasIds.length; i < len; i++){
				var aliasId = aliasIds[i];
				this.layersJSonCache[aliasId] = layerJSon;
			}
		}
	},

	normalizeConfiguration: function() {
		var dataSources = Atlas.conf['dataSources'];
		for (var dataSourceId in dataSources) {
			if (dataSources.hasOwnProperty(dataSourceId)) {
				dataSourceData = dataSources[dataSourceId];

				if (!dataSourceData['dataSourceType']) {
					dataSourceData['dataSourceType'] = "WMS";
				} else {
					dataSourceData['dataSourceType'] = dataSourceData['dataSourceType'].toUpperCase();
				}

				if (dataSourceData['dataSourceType'] == 'WMS' || dataSourceData['dataSourceType'] == 'NCWMS') {
					if (!dataSourceData['featureRequestsUrl'] && dataSourceData['wmsServiceUrl']) {
						dataSourceData['featureRequestsUrl'] = dataSourceData['wmsServiceUrl'];
					}
				}
			}
		}
	},

	/**
	 * Set the default values and execute basic correction of values
	 */
	normalizeLayerJSon: function(layerJSon) {
		// Normalise "dataSourceId"
		if (!layerJSon['dataSourceId'] && !layerJSon['dataSourceType']) {
			layerJSon['dataSourceId'] = 'default'; // TODO get the "first" ID found in Atlas.conf['dataSources']
		}

		var layerDataSource = null;
		if (layerJSon['dataSourceId']) {
			layerDataSource =
				Atlas.conf['dataSources'][layerJSon['dataSourceId']];
			if (!layerDataSource) {
				layerJSon['dataSourceId'] = 'default'; // TODO get the "first" ID found in Atlas.conf['dataSources']
				layerDataSource = Atlas.conf['dataSources'][layerJSon['dataSourceId']];
			}
		}

		// Normalise legend fields
		// hasLegend, default true
		layerJSon['hasLegend'] =
			!((layerJSon['hasLegend'] === 'false') ||
				(layerJSon['hasLegend'] === false));

		if (!layerJSon['legendUrl'] && layerDataSource && layerDataSource['legendUrl']) {
			layerJSon['legendUrl'] = layerDataSource['legendUrl'];
		}

		/*
		// Initial State
		if (!layerJSon['initialState']) {
			layerJSon['<initialState>'] = {};
		}
		// loaded: boolean, default false
		layerJSon['<initialState>']['<loaded>'] =
				(layerJSon['<initialState>']['<loaded>'] === 'true') ||
				(layerJSon['<initialState>']['<loaded>'] === true);
		// activated: boolean, default true
		layerJSon['<initialState>']['<activated>'] =
				!(
					(layerJSon['<initialState>']['<activated>'] === 'false') ||
					(layerJSon['<initialState>']['<activated>'] === false)
				);
		// activated: boolean, default true
		layerJSon['<initialState>']['<legendActivated>'] =
				layerJSon['<hasLegend>'] &&
				!(
					(layerJSon['<initialState>']['<legendActivated>'] === 'false') ||
					(layerJSon['<initialState>']['<legendActivated>'] === false)
				);

		// opacity: real [0, 1], default 1
		if (isNaN(layerJSon['<initialState>']['<opacity>'])) {
			layerJSon['<initialState>']['<opacity>'] = 1;
		} else {
			if (layerJSon['<initialState>']['<opacity>'] < 0) {
				layerJSon['<initialState>']['<opacity>'] = 0;
			} else if (layerJSon['<initialState>']['<opacity>'] > 1) {
				layerJSon['<initialState>']['<opacity>'] = 1;
			}
		}
		*/

		// Normalise "wmsFeatureRequestLayers"
		// Ensure they all looks like this:
		// [{layerId: featureRequestsUrl}, ...]
		if (layerDataSource) {
			if (!layerJSon['wmsFeatureRequestLayers']) {
				var layerId = layerJSon['layerName'] || layerJSon['layerId'];
				// Create 'wmsFeatureRequestLayers': [{layerId: featureRequestsUrl}]
				// for layer without wmsFeatureRequestLayers field.
				layerJSon['wmsFeatureRequestLayers'] = [{}];
				layerJSon['wmsFeatureRequestLayers'][0][layerId] =
					layerDataSource['featureRequestsUrl'];
			} else {
				// Replace 'wmsFeatureRequestLayers': [layerId, layerId, ...]
				// by 'wmsFeatureRequestLayers': [{layerId: featureRequestsUrl}, ...]
				var requestLayers = layerJSon['wmsFeatureRequestLayers'];
				for (var i=0; i<requestLayers.length; i++) {
					if (typeof(requestLayers[i]) === 'string') {
						var layerId = requestLayers[i];
						requestLayers[i] = {};
						requestLayers[i][layerId] =
							layerDataSource['featureRequestsUrl'];
					}
				}
			}
		}

		// Normalize the field "description" - Make links clickable
		if (!layerJSon['description']) {
			layerJSon['description'] = layerJSon['title'];
		}

		// Apply all server config to the layer, to allow easy override in the layer.
		if (layerDataSource) {
			for(var dataSourceProp in layerDataSource){
				if(layerDataSource.hasOwnProperty(dataSourceProp)
					&& typeof(layerJSon[dataSourceProp]) == 'undefined'){

					layerJSon[dataSourceProp] = layerDataSource[dataSourceProp];
				}
			}
		}

		return layerJSon;
	},

	safeHtml: function(input) {
		if (input == null) { return null; }
		return input.replace(/&/gi, "&amp;").replace(/</gi, "&lt;").replace(/>/gi, "&gt;");
	},

	/**
	 * Change all URLs in the input to a HTML link, and truncate long URL to
	 * maxUrlLength characters.
	 *
	 * See RFC 1738 for valid URL schema:
	 *     http://www.apps.ietf.org/rfc/rfc1738.html#sec-5
	 */
	urlsToHTML: function(input, popup, maxUrlLength) {
		var newInput = '';
		var lastIndex = 0;

		if (typeof(popup) == 'undefined') {
			popup = true;
		}
		if (typeof(maxUrlLength) == 'undefined') {
			maxUrlLength = this.MAX_URL_LENGTH;
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
				link += ">";

				// Truncate the displayed URL, when needed
				if (maxUrlLength == 1) {
					link += ".";
				} else if (maxUrlLength == 2) {
					link += "..";
				} else if (maxUrlLength > 0 && maxUrlLength < displayUrl.length) {
					var beginningLength = Math.round((maxUrlLength-3)/4.0*3);
					var endingLength = maxUrlLength - beginningLength - 3; // 3 is for the "..."
					if (beginningLength > 1 && endingLength == 0) {
						beginningLength--;
						endingLength = 1;
					}
					link += displayUrl.substring(0, beginningLength) + "..." + displayUrl.substring(displayUrl.length - endingLength);
				} else {
					link += displayUrl;
				}

				link += "</a>";

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

	lineBreaksToHTML: function(input) {
		// Replace all 3 types of line breaks with a HTML line break.
		return input.replace(/(\r\n|\n|\r)/gim, '<br/>\n');
	},

	/**
	 * Return a tooltip for the layer/node.
	 */
	getNodeQTip: function(jsonLayer) {
		return null;
	},
	getLayerQTip: function(jsonLayer) {
		return null;
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
	getLayerDescription: function(jsonLayer) {
		if (!jsonLayer) {
			return null;
		}

		var desc = '';
		if (jsonLayer['title']) {
			desc = '<b>' + jsonLayer['title'] + '</b>'
		} else if (jsonLayer['layerId']) {
			desc = '<b>' + jsonLayer['layerId'] + '</b>';
		}
		var additionalInfo = jsonLayer['additionalInfo'];
		if (additionalInfo) {
			for(var key in additionalInfo){
				if(additionalInfo.hasOwnProperty(key)){
					desc += '<br/><b>'+key+':</b> '+additionalInfo[key];
				}
			}
		}
		if (jsonLayer['description']) {
			desc += '<br/>' + this.urlsToHTML(this.lineBreaksToHTML(this.safeHtml(jsonLayer['description'])));
		}
		if (jsonLayer['htmlDescription']) {
			desc += '<br/>' + jsonLayer['htmlDescription'];
		}

		if (jsonLayer['layerId']) {
			desc += '<div class="descriptionLayerId">Layer id: <em>' + jsonLayer['layerId'] + '</em></div>';
		}
		return desc;
	}
});
