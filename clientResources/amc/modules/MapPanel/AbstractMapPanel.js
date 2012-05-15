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


function clone(obj) {
	var target = {};
	for (var i in obj) {
		if (obj.hasOwnProperty(i)) {
			target[i] = obj[i];
		}
	}
	return target;
}

Atlas.AbstractMapPanel = {
	// Event types specific to the Map.
	EVENT_TYPES: [
		"addLayerIds", "addLayers", "layerAdded",
		"removeLayer", "layerRemoved",
		"locateLayer",
		'legendVisibilityChange',
		'render'
	],
	// The OpenLayers event object, set in initComponent function
	events: null,

	// The MapPanel is always in the center
	region: 'center',

	KML_ALLOW_JAVASCRIPT: false,

	center: null,
	zoom: 0,

	renderTo: null,
	embedded: false,

	// The feature request manager
	wmsFeatureInfo: null,

	// Avoid clash with "rendered", which is defined in GeoExt.MapPanel
	isRendered: false,

	defaultLonLatProjection: null,

	initComponent: function() {
		var that = this;

		this.events = new OpenLayers.Events(this, null,
			this.EVENT_TYPES);

		this.defaultLonLatProjection = new OpenLayers.Projection('EPSG:4326');

		var projection = null;
		if (Atlas.conf['projection']) {
			projection = new OpenLayers.Projection(Atlas.conf['projection']);
		} else {
			projection = this.defaultLonLatProjection;
		}

		if (Atlas.conf['startingLocation']) {
			var startingLocation = Atlas.conf['startingLocation'];

			if (startingLocation[0] != null && startingLocation[1] != null) {
				// Array of number, representing the centre of the map (Longitude, Latitude)
				this.center = new OpenLayers.LonLat(startingLocation[0], startingLocation[1]);
				if (projection != this.defaultLonLatProjection) {
					this.center = this.center.transform(this.defaultLonLatProjection, projection);
				}
			}
			if (startingLocation[2] != null) {
				// Number (default zoom level)
				this.zoom = startingLocation[2];
			}
		}

		/**
		 * Default controls are:
		 * OpenLayers.Control.Navigation  // Handles map browsing with mouse events (dragging, double-clicking, and scrolling the wheel).
		 * OpenLayers.Control.PanZoom     // Pan and Zoom controls, in the top left corner
		 * OpenLayers.Control.ArgParser   // Parse the location bar for lon, lat, zoom, and layers information
		 * OpenLayers.Control.Attribution // Adds attribution from layers to the map display
		 */
		var controls = [];
		if (this.embedded) {
			controls = [
				new OpenLayers.Control.PanZoom(),       // Pan and Zoom (minimalist) controls, in the top left corner
				new OpenLayers.Control.Navigation(),
				new OpenLayers.Control.ZoomBox()        // Enables zooming directly to a given extent, by drawing a box on the map.  The box is drawn by holding down shift, whilst dragging the mouse.
			];
		} else {
			controls = [
				new OpenLayers.Control.PanZoomBar(),    // Pan and Zoom (with a zoom bar) controls, in the top left corner
				new OpenLayers.Control.ScaleLine({geodesic: true}),     // Displays a small line indicator representing the current map scale on the map. ("geodesic: true" has to be set to recalculate the scale line when the map get span closer to the poles)
				//new OpenLayers.Control.Scale(),         // Displays the map scale (example: 1:1M).
				new OpenLayers.Control.MousePosition({
					displayProjection: this.defaultLonLatProjection
				}),                                     // Displays geographic coordinates of the mouse pointer
				new OpenLayers.Control.Navigation(),
				//new OpenLayers.Control.OverviewMap(), // Creates a small overview map
				new OpenLayers.Control.KeyboardDefaults(), // Adds panning and zooming functions, controlled with the keyboard.  By default arrow keys pan, +/- keys zoom & Page Up/Page Down/Home/End scroll by three quarters of a page.
				new OpenLayers.Control.ZoomBox()        // Enables zooming directly to a given extent, by drawing a box on the map.  The box is drawn by holding down shift, whilst dragging the mouse.
			];
		}

		var maxExtent = null;
		if (Atlas.conf['mapOptions'] && Atlas.conf['mapOptions']['maxExtent']) {
			maxExtent = new OpenLayers.Bounds(
				Atlas.conf['mapOptions']['maxExtent'][0],
				Atlas.conf['mapOptions']['maxExtent'][1],
				Atlas.conf['mapOptions']['maxExtent'][2],
				Atlas.conf['mapOptions']['maxExtent'][3]
			);
		} else {
			maxExtent = new OpenLayers.Bounds(-180.0, -90.0, 180.0, 90.0);
			if (projection != this.defaultLonLatProjection) {
				maxExtent = maxExtent.transform(this.defaultLonLatProjection, projection);
			}
		}

		// Designed for the GeoWebCache
		var mapOptions = {};

		// Copy all options of Atlas.conf['mapOptions'] to mapOptions.
		// NOTE: Do not assign mapOptions = Atlas.conf['mapOptions'] since the options of mapOptions can change and
		// the config must remained unchanged (it will be used by the other map panels when displaying multiple maps)
		for (optionKey in Atlas.conf['mapOptions']) {
			if (optionKey != 'projection' && optionKey != 'maxExtent' && optionKey != 'controls' && Atlas.conf['mapOptions'].hasOwnProperty(optionKey)) {
				mapOptions[optionKey] = Atlas.conf['mapOptions'][optionKey];
			}
		}

		// 3 options required object instanciation
		mapOptions['projection'] = projection;
		mapOptions['maxExtent'] = maxExtent;
		mapOptions['controls'] = controls;

		// This attribute should NOT be true or else it cause all sort of
		// weird side effects with base layers. The default value is false
		// but, for some reason, it is changed to true somewhere.
		if (typeof(mapOptions['allOverlays']) == 'undefined') { mapOptions['allOverlays'] = false; }

		// Set the default values
		if (typeof(mapOptions['maxResolution']) == 'undefined') { mapOptions['maxResolution'] = 0.703125; }
		if (typeof(mapOptions['numZoomLevels']) == 'undefined') { mapOptions['numZoomLevels'] = 16; }

		if (this.embedded) {
			//mapOptions.zoom = this.zoom; // BUG: This properties is ignored: http://trac.osgeo.org/openlayers/ticket/3362
			mapOptions.center = new OpenLayers.LonLat(this.center[0], this.center[1]);
		}

		if (typeof(this.renderTo) != 'undefined' && this.renderTo != null) {
			mapOptions.div = this.renderTo;
		}

		if (this.mapId) {
			mapOptions.id = this.mapId;
		}

		this.map = new OpenLayers.Map(mapOptions);
		this.map.render = function(div) {
			// Call the original render method
			OpenLayers.Map.prototype.render.apply(that.map, arguments);

			// Fire the render event.
			that.isRendered = true;
			that.ol_fireEvent("render");
		};

		// Add a dummy base layer to solve all the problems related with
		// maps without base layer.
		var dummyBaseLayerOptions = {
			maxExtent: maxExtent,
			isBaseLayer: true,
			visibility: true,
			displayInLayerSwitcher: false
		};
		var dummyBaseLayer = new OpenLayers.Layer("Base", dummyBaseLayerOptions);
		this.map.addLayer(dummyBaseLayer);

		// Work around the zoom bug: http://trac.osgeo.org/openlayers/ticket/3362
		if (this.embedded) {
			this.map.zoomTo(this.zoom);
		}

		// ExtJS event listener - Hide markers when the map is resized.
		// The maps are resized each time a new map is added/removed, which
		// trigger the events that show the markers.
		// The function to hide the markers has to be called manually.
		if (Atlas.core._hideMarkers) {
			this.ol_on('resize', function(evt) {Atlas.core._hideMarkers();});
		}

		if (Atlas.MapPanel.superclass && Atlas.MapPanel.superclass.initComponent) {
			Atlas.MapPanel.superclass.initComponent.call(this);
		}

		if (!this.layers) {
			this.layers = [];
		}

		// Initialise the feature request manager
		if (Atlas.MapPanel.MultiWMSGetFeatureInfo) {
			this.wmsFeatureInfo = new Atlas.MapPanel.MultiWMSGetFeatureInfo({
				map: this.map
			});
			this.wmsFeatureInfo.activate();
		}

		// Auto-set some extra layer attributes and methods
		// for layers that has a json attribute (I.E. those methods
		// do not apply to the markers)
		this.map.events.on({"preaddlayer": function(evt) {
			if (evt && evt.layer && evt.layer.json) {
				that._beforeLayerAdd(evt.layer);
			}
		}});
		this.map.events.on({"addlayer": function(evt) {
			if (evt && evt.layer && evt.layer.json) {
				that._afterLayerAdd(evt.layer);
			}
		}});
		this.map.events.on({"preremovelayer": function(evt) {
			if (evt && evt.layer && evt.layer.json) {
				that._beforeLayerRemove(evt.layer);
			}
		}});
		this.map.events.on({"removelayer": function(evt) {
			if (evt && evt.layer && evt.layer.json) {
				that._afterLayerRemove(evt.layer);
			}
		}});

		// Register the event listeners
		this.ol_on("addLayerIds", function(evt) {
			that.addLayersById(evt.layerIds);
		});
		this.ol_on("addLayers", function(evt) {
			that.addLayers(evt.layersJSon);
		});
		this.ol_on("removeLayer", function(evt) {
			that.removeLayer(evt.layer);
		});
		this.ol_on("locateLayer", function(evt) {
			that.locateLayer(evt.layer);
		});

		// Add default layers to the map
		var defaultLayers = Atlas.conf['defaultLayers'];
		var layerOrder = Atlas.conf['layerOrder'];
		// Normalise the layers and load them in the core cache.
		Atlas.core.loadNewLayersCache(defaultLayers);
		// Add layers in reverse order - the last added will be on top
		for(var i=defaultLayers.length-1; i>=0; i--){
			// Get the layer from the core cache and load it in the map
			// NOTE The layer from the core cache is normalized.
			this.addLayerById(defaultLayers[i].layerId);
		}
	},

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

	/**
	 * Return the Web Cache URL if every parameters are supported by the Web Cache server.
	 * Return the WMS Service URL otherwise.
	 */
	getWMSServiceUrl: function(layerJSon, layerParams) {
		var serviceUrl = layerJSon['wmsServiceUrl'];
		if (layerJSon['webCacheUrl'] &&
			this._canUseWebCache(layerJSon, layerParams)) {
			serviceUrl = layerJSon['webCacheUrl'];
		}
		return serviceUrl;
	},

	_canUseWebCache: function(layerJSon, layerParams) {
		if (typeof(layerJSon['cached']) !== 'undefined' && !layerJSon['cached']) {
			return false;
		}

		var supportedParams = layerJSon['webCacheSupportedParameters'];
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

	_applyOlOverrides: function(config, overrides) {
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

	_getTitle: function(layerJSon) {
		var title = layerJSon['title'] || layerJSon['layerName'] || layerJSon['layerId'];
		return title;
	},

	_getWMSExtraServiceUrls: function(layerJSon) {
		return layerJSon['extraWmsServiceUrls'];
	},

	_getWMSLayerParams: function(layerJSon) {
		var isBaseLayer = !!layerJSon['isBaseLayer'];

		// Set the parameters used in the URL to request the tiles.
		var layerParams = {
			layers: layerJSon['layerName'] || layerJSon['layerId']
		};
		// The WMS version is also used in the WMS requests and the legend graphics.
		if (layerJSon['wmsVersion']) {
			layerParams.version = layerJSon['wmsVersion'];
		}

		if (layerJSon['wmsRequestMimeType']) {
			layerParams.format = layerJSon['wmsRequestMimeType'];
		}

		// Select default style if needed
		if (layerJSon['styles']) {
			for (var styleName in layerJSon['styles']) {
				var jsonStyle = layerJSon['styles'][styleName];
				if (styleName && jsonStyle["default"]) {
					layerParams.styles = styleName;
					break;
				}
			}
		}

		layerParams.transparent = !isBaseLayer;

		if (typeof(layerJSon['olParams']) !== 'undefined') {
			layerParams = this._applyOlOverrides(layerParams, layerJSon['olParams']);
		}

		return layerParams;
	},

	_getWMSLayerOptions: function(layerJSon) {
		var isBaseLayer = !!layerJSon['isBaseLayer'];

		// Set the OpenLayer options, used by the library.
		var layerOptions = {
			isBaseLayer: isBaseLayer,
			displayInLayerSwitcher: true,
			wrapDateLine : true,
			buffer: 0
		};

		if (layerJSon['projection']) {
			layerOptions.projection = layerJSon['projection'];
		} else {
			layerOptions.projection = this.map.getProjectionObject()
		}

		if (Atlas.conf['mapOptions'] && Atlas.conf['mapOptions']['units']) {
			layerOptions.units = Atlas.conf['mapOptions']['units'];
		}

		if (isBaseLayer) {
			// Set the transition effect to something smooth.
			// This effect allow the user to view the base layer
			// when zooming in/out or panning, by resizing the
			// existing tiles and display them while waiting for
			// the new tiles.
			layerOptions.transitionEffect = 'resize';
		}

		if (typeof(layerJSon['olOptions']) !== 'undefined') {
			layerOptions = this._applyOlOverrides(layerOptions, layerJSon['olOptions']);
		}

		return layerOptions;
	},

	_getArcGISLayerOptions: function(layerJSon) {
		var layerOptions = {
			// Big tiles has less issues with labeling
			tileSize: new OpenLayers.Size(512, 512),
			isBaseLayer: false,
			wrapDateLine: true
			// Single tile make very nice map, with perfect labeling, but has some issues
			// near the date line and it cost more for the server (need more resources).
			//singleTile: true
		};
		// ESRI layers do not support
		// Google projection (EPSG:900913) but they support
		// ESRI projection (EPSG:102100) which is the same.
		if (layerJSon['projection']) {
			layerOptions.projection = layerJSon['projection'];
		} else if (this.map && this.map.getProjection() === 'EPSG:900913') {
			layerOptions.projection = 'EPSG:102100';
		}

		if (typeof(layerJSon['olOptions']) !== 'undefined') {
			layerOptions = this._applyOlOverrides(layerOptions, layerJSon['olOptions']);
		}

		return layerOptions;
	},

	_createNCWMSLayer: function(layerJSon) {
		// TODO Support Multiple URLS => this._getWMSExtraServiceUrls(layerJSon),
		var layerParams = this._getWMSLayerParams(layerJSon);
		return new OpenLayers.Layer.ux.NCWMS(
			this._getTitle(layerJSon),
			this.getWMSServiceUrl(layerJSon, layerParams),
			layerParams,
			this._getWMSLayerOptions(layerJSon)
		);
	},

	_createArcGISMapServerLayer: function(layerJSon) {
		var layerOptions = this._getArcGISLayerOptions(layerJSon);
		var layerParams = {
			layers: "show:" + (layerJSon['layerName'] || layerJSon['layerId']),
			transparent: true
		};

		if (typeof(layerJSon['olParams']) !== 'undefined') {
			layerParams = this._applyOlOverrides(layerParams, layerJSon['olParams']);
		}

		var url = layerJSon['wmsServiceUrl'];
		if (layerJSon['arcGISPath']) {
			url += '/' + layerJSon['arcGISPath'];
		}
		url += '/MapServer/export';

		return new OpenLayers.Layer.ArcGIS93Rest(
			layerJSon['title'],
			url,
			layerParams,
			layerOptions
		);
	},

	_createArcGISCacheLayer: function(layerJSon) {
		// TODO Do not send all options to the client, use JSONP instead. Also change the logic; ArcGIS Cache are per Service, not per Layer.
		// http://openlayers.org/dev/examples/arcgiscache_jsonp.html

		// Temporary fix: Do not use the cache...
		return this._createArcGISMapServerLayer(layerJSon);

		/*
		var layerOptions = this._getArcGISLayerOptions(layerJSon);

		var url = layerJSon['wmsServiceUrl'];
		if (layerJSon['arcGISPath']) {
			url += '/' + layerJSon['arcGISPath'];
		}
		url += '/MapServer/export';

		layerOptions.layers = "show:" + (layerJSon['layerName'] || layerJSon['layerId']);
		layerOptions.tileSize = new OpenLayers.Size(layerJSon['arcGISCacheTileCols'], layerJSon['arcGISCacheTileRows']);
		layerOptions.tileOrigin = new OpenLayers.LonLat(layerJSon['arcGISCacheTileOriginX'] , layerJSon['arcGISCacheTileOriginY']);
		layerOptions.maxExtent = new OpenLayers.Bounds(layerJSon['layerBoundingBox'][0], layerJSon['layerBoundingBox'][1], layerJSon['layerBoundingBox'][2], layerJSon['layerBoundingBox'][3]);
		layerOptions.resolutions = layerJSon['arcGISCacheTileResolutions'];
		layerOptions.transparent = true;

		return new OpenLayers.Layer.ArcGISCache(
			layerJSon['title'],
			url,
			layerOptions
		);
		*/
	},

	_createWMSLayer: function(layerJSon) {
		// TODO Support Multiple URLS => this._getWMSExtraServiceUrls(layerJSon),
		var layerParams = this._getWMSLayerParams(layerJSon);
		return new OpenLayers.Layer.WMS(
			this._getTitle(layerJSon),
			this.getWMSServiceUrl(layerJSon, layerParams),
			layerParams,
			this._getWMSLayerOptions(layerJSon)
		);
	},

	_createWMTSLayer: function(layerJSon) {
		// TODO Create a WMTS layer
		return this._createWMSLayer(layerJSon);
	},

	_createKMLLayer: function(layerJSon) {
		var kmlUrl = layerJSon['kmlUrl'];

		// Set the OpenLayer options, used by the library.
		var layerOptions = {
			strategies: [new OpenLayers.Strategy.Fixed()],
			//visibility: layerJSon['<initialState>']['<activated>'],
			//opacity: layerJSon['<initialState>']['<opacity>'],
			protocol: new OpenLayers.Protocol.HTTP({
				url: kmlUrl,
				format: new OpenLayers.Format.KML({
					extractStyles: true,
					extractAttributes: true
				})
			})
		};

		if (typeof(layerJSon['olOptions']) !== 'undefined') {
			layerOptions = this._applyOlOverrides(layerOptions, layerJSon['olOptions']);
		}

		var kml = new OpenLayers.Layer.Vector(
			this._getTitle(layerJSon),
			layerOptions
		);

		var select = new OpenLayers.Control.SelectFeature(kml);

		// OpenLayer events for KML layers
		var that = this;
		kml.events.on({
			"featureselected": function(event) {
				that._onFeatureSelect(event, select);
			},
			"featureunselected": function(event) {
				that._onFeatureUnselect(event);
			}
		});

		this.map.addControl(select);
		select.activate();

		return kml;
	},

	_createGoogleLayer: function(layerJSon) {
		var layerOptions = {
			// google.maps.MapTypeId.TERRAIN, google.maps.MapTypeId.ROADMAP, google.maps.MapTypeId.HYBRID, google.maps.MapTypeId.SATELLITE
			type: google.maps.MapTypeId[layerJSon['layerName'] || layerJSon['layerId']]
		};

		if (typeof(layerJSon['olOptions']) !== 'undefined') {
			layerOptions = this._applyOlOverrides(layerOptions, layerJSon['olOptions']);
		}

		return new OpenLayers.Layer.Google(
			// "Google Physical", "Google Streets", "Google Hybrid", "Google Satellite"
			this._getTitle(layerJSon),
			layerOptions
		);
	},


	_onPopupClose: function(evt, select) {
		select.unselectAll();
		// Stops an event from propagating.
		// Otherwise, the close button may trigger a feature request.
		OpenLayers.Event.stop(evt);
	},
	_onFeatureSelect: function(event, select) {
		var feature = event.feature;
		// Since KML is user-generated, do naive protection against
		// Javascript.
		var content = "<h2>"+feature.attributes.name + "</h2>" + feature.attributes.description;
		// Javascript in KML can be unsafe.
		if (!this.KML_ALLOW_JAVASCRIPT) {
			if (content.search("<script") != -1) {
				content = "Content contained Javascript! Escaped content below.<br />" + content.replace(/</g, "&lt;");
			}
		}
		var that = this;
		var popupId = 'kml-popup';
		var popup = new OpenLayers.Popup.FramedCloud(
			popupId,
			feature.geometry.getBounds().getCenterLonLat(),
			new OpenLayers.Size(100,100), // Initial content size
			content,
			null, true,
			function(event) {that._onPopupClose(event, select);}
		);
		feature.popup = popup;
		this.map.addPopup(popup);
	},
	_onFeatureUnselect: function(event) {
		var feature = event.feature;
		if(feature.popup) {
			this.map.removePopup(feature.popup);
			feature.popup.destroy();
			delete feature.popup;
		}
	},

	_createXYZLayer: function(layerJSon) {
		alert('Layer type XYZ is not yet implemented.');
		return null;
	},

	/**
	 * Add layers from an array of layer IDs
	 */
	addLayersById: function(layerIds, path) {
		var that = this;
		Atlas.core.requestLayersJSon(layerIds, function(layersJSon) {
			if (typeof(path) != 'undefined' && path.length > 0) {
				for (var i=0; i<layersJSon.length; i++) {
					// Clone the object, to avoid modifying the original
					layersJSon[i] = clone(layersJSon[i]);
					layersJSon[i].path = path;
				}
			}
			that.addLayers(layersJSon);
		});
	},

	addLayerById: function(layerId, path) {
		this.addLayersById([layerId], path);
	},

	/**
	 * layerJSon: Layer object as returned by Atlas.core.getLayerJSon
	 */
	addLayers: function(layersJSon) {
		for (var i=layersJSon.length-1; i>=0; i--) {
			this.addLayer(layersJSon[i]);
		}
	},

	/**
	 * layerJSon: Layer object as returned by Atlas.core.getLayerJSon
	 */
	addLayer: function(layerJSon) {
		var that = this;
		if (!this.isRendered) {
			this.ol_on("render", function(evt) {
				that.addLayer(layerJSon);
			});
			return;
		}

		var layer = null;
		switch (layerJSon['dataSourceType']) {
			case 'NCWMS':
				layer = this._createNCWMSLayer(layerJSon);
				break;
			case 'ARCGIS_MAPSERVER':
				layer = this._createArcGISMapServerLayer(layerJSon);
				break;
			case 'ARCGIS_CACHE':
				layer = this._createArcGISCacheLayer(layerJSon);
				break;
			case 'WMS':
				layer = this._createWMSLayer(layerJSon);
				break;
			case 'WMTS':
				layer = this._createWMTSLayer(layerJSon);
				break;
			case 'KML':
				layer = this._createKMLLayer(layerJSon);
				break;
			case 'GOOGLE':
				layer = this._createGoogleLayer(layerJSon);
				break;
			case 'XYZ':
				layer = this._createXYZLayer(layerJSon);
				break;
			case 'FOLDER':
			case 'GROUP':
				// Layer group
				// The Folder itself is represented as a pseudo layer on the Map (basic OpenLayers layer without graphics)
				// That pseudo layer is created and added by the GroupLayerLoader when needed (the insertion order is crucial).
				// NOTE: There is no difference between a FOLDER and a GROUP. It's simply to differentiate between ArcGIS
				//     layers with children (GROUP) and a ArcGIS Folders and Services (FOLDER).
				if (layerJSon['layers'] && layerJSon['layers'].length > 0) {
					// The "layers" attribute define the children layers of the group.
					// NOTE: Since each layer may appear in multiple folders, the attribute path can
					//     not be defined in the layer's configuration. It has to be dynamically
					//     created for each instance of the layer.

					// Path: Array of layerJSON object, with a unique ID for this instance of the group
					var pathSuffixId = "_" + new Date().getTime();
					var path = [];

					// The path is dynamically created for the FOLDER / GROUP (folder)
					if (layerJSon['path']) {
						// Clone the path
						path = layerJSon['path'].slice(0);
					}

					var pathPart = clone(layerJSon);
					pathPart.id = (layerJSon['layerName'] || layerJSon['layerId']) + pathSuffixId;
					path.push(pathPart);

					// Add all children under that path. If a child is a FOLDER / GROUP, it will pass
					// through this function again.
					this.addLayersById(layerJSon['layers'], path);
				}
				return;
			default:
				alert('Layer type '+layerJSon['dataSourceType']+' is not implemented.');
		}
		if (!layer) {
			// TODO THROW EXCEPTION
			return;
		}

		layer.json = layerJSon;
		layer.hideInLegend = false;

		if (typeof(layerJSon['path']) !== 'undefined') {
			layer.path = layerJSon['path'];
		}

		// TODO Remove this after implementing Save State
		if (typeof(layerJSon['selected']) !== 'undefined') {
			if (!layerJSon['selected']) {
				layer.visibility = false;
			}
			delete(layerJSon['selected']);
		}

		// Add the layer to the Map
		// NOTE: This method trigger _beforeLayerAdd and _afterLayerAdd
		this.map.addLayer(layer);
	},

	removeLayer: function(layer) {
		// Remove the layer from the Map
		// NOTE: This method trigger _afterLayerRemove
		this.map.removeLayer(layer);
	},

	// Private
	// This method augment the layer object by adding some methods to it.
	// It should be called before adding the layer to the map since those
	// methods may be called when the layer is added.
	_beforeLayerAdd: function(layer) {
		var that = this;
		var layerJSon = layer.json;

		// Add the functions setHideInLegend/getHideInLegend to all layers.
		// It would just be too much trouble if all layer class had
		// to be extend only to add those functions.
		layer.setHideInLegend = function(hide) {
			layer.hideInLegend = hide;
			that.map.events.triggerEvent("changelayer", {
				layer: layer,
				property: "hideInLegend"
			});
			that.ol_fireEvent('legendVisibilityChange', {layer: layer});
		};
		layer.getHideInLegend = function() {
			return layer.hideInLegend;
		};
	},

	// Private
	// This method fire events and set some attributes
	_afterLayerAdd: function(layer) {
		var layerJSon = layer.json;

		// Add feature request listener for that layer, if needed
		if (this.wmsFeatureInfo && layerJSon['wmsQueryable']) {
			this.wmsFeatureInfo.addLayer(layer);
		}

		if (layer.isBaseLayer) {
			this.map.setBaseLayer(layer);
		}

		/*
		// TODO Find how to do this when the layer is deleted and not moved... (using the loader._reordering maybe)
		// Add an event on the layer to destroy itself after it's removed from the map
		layer.events.on({'removed': function(evt) {
			// Destroy is a destructor: this is to alleviate cyclic
			// references which the Javascript garbage cleaner can not
			// take care of on its own.
			evt.layer.destroy();
		}});
		*/

		this.ol_fireEvent('layerAdded', {layerJSon: layerJSon});
	},

	// Private
	_beforeLayerRemove: function(layer) {
		var layerJSon = layer.json;

		if (layerJSon && layerJSon['wmsQueryable']) {
			this.wmsFeatureInfo.removeLayer(layer);
		}
	},

	// Private
	_afterLayerRemove: function(layer) {
		var layerJSon = layer.json;

		if (layerJSon) {
			this.ol_fireEvent('layerRemoved', {layerJSon: layerJSon});
		}
	},

	layerCanBeLocated: function(layer) {
		return (this.getLayerExtent(layer) != null);
	},

	locateLayer: function(layer) {
		var bounds = this.getLayerExtent(layer);
		if (bounds != null) {
			this.map.zoomToExtent(bounds, true);
		} else {
			alert("This layer can not be located");
		}
	},

	getLayerExtent: function(layer) {
		var bounds = null;
		if (layer.json && layer.json['layerBoundingBox']) {
			// Bounds order in JSon: left, bottom, right, top
			var boundsArray = layer.json['layerBoundingBox'];

			// Bounds order as requested by OpenLayers: left, bottom, right, top
			// NOTE: Re-projection can not work properly if the top or bottom overpass 85
			bounds = new OpenLayers.Bounds(
				(boundsArray[0] < -180 ? -180 : (boundsArray[0] > 180 ? 180 : boundsArray[0])),
				(boundsArray[1] < -85 ? -85 : (boundsArray[1] > 85 ? 85 : boundsArray[1])),
				(boundsArray[2] < -180 ? -180 : (boundsArray[2] > 180 ? 180 : boundsArray[2])),
				(boundsArray[3] < -85 ? -85 : (boundsArray[3] > 85 ? 85 : boundsArray[3]))
			);

			if (bounds != null) {
				bounds = bounds.transform(this.defaultLonLatProjection, this.map.getProjectionObject());
			}
		}
		if (bounds == null && typeof(layer.getDataExtent) === 'function') {
			bounds = layer.getDataExtent();
		}
		return bounds;
	}
};
