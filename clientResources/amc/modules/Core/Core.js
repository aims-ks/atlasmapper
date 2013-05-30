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
		var masterMapPanel = this.mapPanels[mapIndex];
		for (var i=0; i<this.mapPanels.length; i++) {
			if (i != mapIndex) {
				var anotherMapPanel = this.mapPanels[i];
				anotherMapPanel.setStandardCenter(
					masterMapPanel.map.getCenter().clone().transform(masterMapPanel.map.getProjectionObject(), anotherMapPanel.map.getProjectionObject()),
					masterMapPanel.getStandardZoomLevel()
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
	 * attemptsLeft: Private parameter (ignored for public call); number of
	 *     attempts to perform to request the layers (i.e. the server may
	 *     have some problem performing the request due to overload. Multiple
	 *     attemps may be needed but not an infinite amount of it, otherwise
	 *     it will run into an infinite loop).
	 */

	// Protected
	requestLayersJSon: function(layerIds, callback, missingLayersCallback, attemptsLeft) {
		var that = this;

		if (typeof(attemptsLeft) !== 'number') {
			attemptsLeft = 3;
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
						that.requestLayersJSon(layerIds, callback, missingLayersCallback);
					} else {
						// Some layers are not in the cache. Try to load them.
						// The application will try to load the layers a maximum of 5 times.
						that.requestLayersJSon(layerIds, callback, missingLayersCallback, attemptsLeft-1);
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
				if (missingLayersCallback) {
					missingLayersCallback(missingLayerIds);
				} else {
					// TODO Error on the page
					alert('The application has failed to load the layers ['+missingLayerIds.join()+']');
				}
			}
		}
	},

	requestIso19115_19139State: function(urlStr, callback) {
		if (this.layerInfoServiceUrl) {
			url = this.layerInfoServiceUrl;
			params = {
				client: Atlas.conf['clientId'],
				live: this.live,
				iso19115_19139url: urlStr,
				ver: this.version
			};

			var that = this;
			var received = function (response) {
				// Decode the response
				var jsonResponse = eval("(" + response.responseText + ")");
				if (jsonResponse) {
					if (jsonResponse.success) {
						callback(jsonResponse.data);
					} else {
						// TODO Error on the page jsonResponse.errors
						alert('The application has failed to load the layers associated with the requested XML document.');
					}
				}
			};

			OpenLayers.Request.GET({
				url: url,
				params: params,
				scope: this,
				success: received,
				failure: function (result, request) {
					// TODO Error on the page
					alert('The application has failed to load the layers associated with the requested XML document.');
				}
			});

		} else {
			// TODO Error on the page
			alert('This application do not support layer loading from XML document. This feature is only available when the application is linked to an AtlasMapper server.');
		}
	},

	// layersMap: Map of layers, in the following format:
	//     {
	//         Server Type: {
	//             Server URL: [ Layer IDs ]
	//         }
	//     }
	// Explaination of the fields:
	//     * Server Type: The type of server; WMS, KML, etc.
	//     * Server URL: The URL used to request the layer, as found in the GetCapabilities document.
	//     * Layer IDs: All layers associated with that server.
	// Protected
	requestArbitraryLayersJSon: function(layersMap, callback, attemptsLeft) {
		// Get all layers, per layer type
		var polygonArray = [];
		var markerArray = [];

		var defaultLonLatProjection = new OpenLayers.Projection('EPSG:4326');
		var mapProjection = this.defaultLonLatProjection;
		if (Atlas.conf['projection']) {
			mapProjection = new OpenLayers.Projection(Atlas.conf['projection']);
		}

		for (type in layersMap) {
			if (layersMap.hasOwnProperty(type)) {
				// All WMS layers (or KML, etc.)
				var layersMapForType = layersMap[type];

				if (type === 'PTS') {
					for (var pointIndex = 0; pointIndex<layersMapForType.length; pointIndex++) {
						var ptsArray = layersMapForType[pointIndex];

						var point = new OpenLayers.Geometry.Point(ptsArray[0], ptsArray[1])
						if (mapProjection != defaultLonLatProjection) {
							point = point.transform(defaultLonLatProjection, mapProjection)
						}
						var markerFeature = new OpenLayers.Feature.Vector(point);
						markerArray.push(markerFeature);
					}

				} else if (type === 'POLY') {
					for (var polyIndex = 0; polyIndex<layersMapForType.length; polyIndex++) {
						var polygonPtsArray = layersMapForType[polyIndex];
						var olPointArray = [];
						var ptsLen = polygonPtsArray.length;

						// Ignore the last point if it has the same coordinates as the first one.
						if (polygonPtsArray[0][0] === polygonPtsArray[ptsLen-1][0] && polygonPtsArray[0][1] === polygonPtsArray[ptsLen-1][1]) {
							ptsLen--;
						}

						for (var i=0; i<ptsLen; i++) {
							var point = new OpenLayers.Geometry.Point(polygonPtsArray[i][0], polygonPtsArray[i][1])
							if (mapProjection != defaultLonLatProjection) {
								point = point.transform(defaultLonLatProjection, mapProjection)
							}
							olPointArray.push(point);
						}
						// Close the polygon
						olPointArray.push(olPointArray[0]);

						var linearRings = [];
						linearRings.push(new OpenLayers.Geometry.LinearRing(olPointArray));
						var polygon = new OpenLayers.Geometry.Polygon(linearRings);
						polygonArray.push(new OpenLayers.Feature.Vector(polygon));
					}

				} else if (type === 'KML') {
					var len = layersMapForType.length;
					for (var i=0; i<len; i++) {
						var kmlURL = layersMapForType[i];

						// Try to find the Server URL in the list of datasources
						var kmlLayerJSon = this._findKMLLayerJSon(kmlURL);

						if (kmlLayerJSon != null) {
							// That will not happen unless I develop the service
							callback(kmlLayerJSon);
						} else {
							// No info found. Creating fake layers for the KML layer.
							var layerId = OpenLayers.Util.createUniqueID("KML_");
							var layerJSON = {
								'dataSourceType': type,
								'kmlUrl': kmlURL
							};
							this.loadLayerCache(layerJSON, layerId);
							callback([layerJSON]);
						}
					}

				} else {
					for (serverUrl in layersMapForType) {
						if (layersMapForType.hasOwnProperty(serverUrl)) {
							var layersArray = layersMapForType[serverUrl];
							var len = layersArray.length;

							// Try to find the Server URL in the list of datasources
							var dataSourceId = this._findDataSource(serverUrl);

							if (dataSourceId != null) {
								// A data source has been found.
								// 1. Create a layer ID, as found in the catalogue, for each layers.
								// 2. Request those layers
								// 3. Create a fake layer for each missing layers (after a few attempts)
								var layersIds = [];
								var dataSourcePrefix = dataSourceId + '_';
								for (var i=0; i<len; i++) {
									layersIds.push(dataSourcePrefix + layersArray[i]);
								}
								this.requestLayersJSon(layersIds, callback, function(layerIds) {
									var cleanLayersId, cleanLayersIds = [];
									for (var j=0; j<layerIds; j++) {
										cleanLayersId = layerIds[i];
										// JavaScript do not have any startsWith... if (cleanLayersId.startsWith(dataSourcePrefix)) {
										if (cleanLayersId.slice(0, dataSourcePrefix.length) == dataSourcePrefix) {
											cleanLayersId = cleanLayersId.substring(dataSourcePrefix.length);
										}
										cleanLayersIds.push(cleanLayersId);
									}
									this._requestMissingLayersJSon(type, serverUrl, cleanLayersIds, callback);
								});
							} else {
								// No data source found. Creating fake layers for all of them.
								this._requestMissingLayersJSon(type, serverUrl, layersArray, callback);
							}
						}
					}
				}
			}
		}

		if (polygonArray.length > 0) {
			var polygonLayer = new OpenLayers.Layer.Vector("Polygons and Bounding Boxes");
			polygonLayer.addFeatures(polygonArray);
			callback([polygonLayer]);
		}

		if (markerArray.length > 0) {
			var markerLayer = new OpenLayers.Layer.Vector("Markers");
			markerLayer.addFeatures(markerArray);
			callback([markerLayer]);
		}
	},

	_findKMLLayerJSon: function(kmlURL) {
		// It's not possible to find the KML info without looking through all layers from the catalog. Server side service is required.
		return null;
	},

	_findDataSource: function(serverURL) {
		var dataSources = Atlas.conf['dataSources'];

		for (dataSourceId in dataSources) {
			if (dataSources.hasOwnProperty(dataSourceId)) {
				var dataSource = dataSources[dataSourceId];
				var dataSourceURL = dataSource['wmsServiceUrl'];

				if (this._equalsUrl(serverURL, dataSourceURL)) {
					return dataSourceId;
				}
			}
		}
		return null;
	},

	// Do a proper URL compare, checking host, port number, file, query string (any order), etc. Also, for WMS, .../ows?REQUESR=WMS&... is equivalent to .../wms?...
	_equalsUrl: function(urlStr1, urlStr2) {
		// Get rid of the most strait forward case
		if (urlStr1 === urlStr2) { return true; }

		var url1 = this._toUrlObj(urlStr1);
		var url2 = this._toUrlObj(urlStr2);

		if (url1 === null || url2 === null) { return false; }

		if (url1.protocol !== url2.protocol) { return false; }
		if (url1.host !== url2.host) { return false; }
		if (url1.path !== url2.path) { return false; }

		if (url1.file !== url2.file) {
			if (url1.file === null || url2.file === null) { return false; }

			// Exception: "wms" === "ows?SERVICE=WMS"
			var wms = (url1.file.toLowerCase() === 'wms' ? url1 : (url2.file.toLowerCase() === 'wms' ? url2 : null));
			var ows = (url1.file.toLowerCase() === 'ows' ? url1 : (url2.file.toLowerCase() === 'ows' ? url2 : null));

			if (wms === null || ows === null) { return false; }
			if (ows.parameters === null) { return false; }
			if (ows.parameters['SERVICE'].toUpperCase() === 'WMS') {
				// They are the same so far, but we still need to examine the parameters.
				// We know that the OWS one has a SERVICE parameter, but this parameter
				// may be missing from the WMS one, since it's implied.
				if (!wms.parameters || wms.parameters['SERVICE'] === null) {
					// The WMS URL do not have the SERVICE parameter. We can add it in
					// to help the validation of the parameters.
					if (!wms.parameters) {
						wms.parameters = {};
					}
					wms.parameters['SERVICE'] = ows.parameters['SERVICE'];
				}
			} else {
				return false;
			}
		}

		// Check URL parameters
		// NOTE: There is no easy way to check a Map size without looping
		//     through them all. It's easier to simply do the check both way.
		if (!this._includedIn(url1.parameters, url2.parameters) || !this._includedIn(url2.parameters, url1.parameters)) {
			return false;
		}

		// Anchor: There is no need to check the anchor, they should not affect service behaviour.

		return true;
	},

	/**
	 * http://www.domain.com/path/file.html?param1=value1&param2=value2#anchor
	 * {
	 *     'protocol': 'http',
	 *     'host': 'www.domain.com',
	 *     'port': 80,
	 *     'path': '/path/'
	 *     'file': 'file.html'
	 *     'parameters': {
	 *         'param1': 'value1',
	 *         'param2': 'value2'
	 *     },
	 *     'anchor': 'anchor'
	 * }
	 */
	_toUrlObj: function(urlStr) {
		if (!urlStr) { return null; }

		var defaultPorts = {
			'http': 80,
			'https': 443,
			'ftp': 21,
			'sftp': 22
		};

		var urlObj = {};

		// Protocol
		urlObj.protocol = urlStr.substring(0, urlStr.indexOf('://'));
		urlStr = urlStr.substring(urlObj.protocol.length + 3);

		// Host including port
		var nextSlash = urlStr.indexOf('/');
		if (nextSlash < 0) {
			urlObj.host = urlStr;
			urlStr = null;
		} else {
			urlObj.host = urlStr.substring(0, nextSlash);
			urlStr = urlStr.substring(urlObj.host.length);
		}

		// Splitting host and port
		var portIndex = urlObj.host.indexOf(':');
		if (portIndex < 0) {
			urlObj.port = defaultPorts[urlObj.protocol];
		} else {
			var hostWithPort = urlObj.host;
			urlObj.host = hostWithPort.substring(0, portIndex);
			urlObj.port = parseInt(hostWithPort.substring(portIndex + 1));
		}

		if (urlStr != null) {
			// Path including file
			var queryStrIndex = urlStr.indexOf('?');
			if (queryStrIndex < 0) {
				urlObj.file = urlStr;
				urlStr = null;
			} else {
				urlObj.file = urlStr.substring(0, queryStrIndex);
				urlStr = urlStr.substring(urlObj.file.length + 1);
			}

			// Splitting path and file
			var lastSlash = urlObj.file.lastIndexOf('/');
			if (lastSlash >= 0) {
				var pathWithFile = urlObj.file;
				urlObj.path = pathWithFile.substring(0, lastSlash + 1);
				urlObj.file = pathWithFile.substring(urlObj.path.length);
			}

			if (urlStr != null) {
				// Parameters
				var anchorIndex = urlStr.indexOf('#');
				var parametersStr = null;
				if (anchorIndex < 0) {
					parametersStr = urlStr;
					urlStr = null;
				} else {
					parametersStr = urlStr.substring(0, anchorIndex);
					urlObj.anchor = urlStr.substring(parametersStr.length + 1);
					urlStr = null;
				}

				// Parse parameters into an object
				var parameters = parametersStr.split('&');
				urlObj.parameters = {};
				for (var i=0; i<parameters.length; i++) {
					var parameter = parameters[i];
					var key = parameter.substring(0, parameter.indexOf('='));
					var value = parameter.substring(key.length + 1);
					if (key) {
						urlObj.parameters[key] = value;
					}
				}
			}
		}

		return urlObj;
	},

	_includedIn: function(param1, param2) {
		if (param1 === null) { return true; } // Null is included in everything.
		if (param2 === null) { return false; } // Nothing else than null is included in null.

		for (key in param1) {
			if (param1.hasOwnProperty(key)) {
				if (param1[key] !== param2[key]) {
					return false;
				}
			}
		}
		return true;
	},

	// Create a list of arbitrary layerJSON (no fancy titles, descriptions, etc.) with
	// the missing layers. Call the callback with those layers to add them in the map.
	_requestMissingLayersJSon: function(dataSourceType, serverUrl, layerIds, callback) {
		var arbitraryLayersJSon = [];

		for (var i=0; i<layerIds.length; i++) {
			var layerId = layerIds[i];

			var layerJSON = {
				'dataSourceType': dataSourceType,
				'wmsServiceUrl': serverUrl,
				'layerId': layerId
			};
			this.loadLayerCache(layerJSON, layerId);
			arbitraryLayersJSon.push(layerJSON);
		}

		callback(arbitraryLayersJSon);
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

	/**
	 * Return a tooltip for the layer/node.
	 */
	getNodeQTip: function(jsonLayer) {
		return null;
	},
	getLayerQTip: function(jsonLayer) {
		return null;
	}
});
