// Namespace declaration (equivalent to Ext.namespace("Atlas");)
window["Atlas"] = window["Atlas"] || {};

// TODO Also extends from a class that is independent of ExtJS (for embeded maps)
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
	embeded: false,

	// The feature request manager
	wmsFeatureInfo: null,

	// Avoid clash with "rendered", which is defined in GeoExt.MapPanel
	isRendered: false,

	defaultLonLatProjection: null,

	initComponent: function() {
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
		if (this.embeded) {
			controls = [
				new OpenLayers.Control.PanZoom(),       // Pan and Zoom (minimalist) controls, in the top left corner
				new OpenLayers.Control.Navigation(),
				new OpenLayers.Control.ZoomBox()        // Enables zooming directly to a given extent, by drawing a box on the map.  The box is drawn by holding down shift, whilst dragging the mouse.
			];
		} else {
			controls = [
				new OpenLayers.Control.PanZoomBar(),    // Pan and Zoom (with a zoom bar) controls, in the top left corner
				new OpenLayers.Control.ScaleLine(),     // Displays a small line indicator representing the current map scale on the map.
				//new OpenLayers.Control.Scale(),         // Displays the map scale (example: 1:1M).
				new OpenLayers.Control.MousePosition(), // Displays geographic coordinates of the mouse pointer
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
		var mapOptions = Atlas.conf['mapOptions'];

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

		if (this.embeded) {
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
		if (this.embeded) {
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

		// Register the event listeners
		var that = this;
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
		// Normalise the layers and load them in the core cache.
		Atlas.core.loadNewLayersCache(defaultLayers);
		for(var layerId in defaultLayers){
			if(defaultLayers.hasOwnProperty(layerId) && layerId != 'defaults'){
				// Get the layer from the core cache and load it in the map
				this.addLayerById(layerId);
			}
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

	_getTitle: function(layerJSon) {
		var title = layerJSon['title'];
		if (!title) {
			title = layerJSon['layerId'];
		}
		return title;
	},

	_getWMSServiceUrl: function(layerJSon) {
		var serviceUrl = layerJSon['wmsServiceUrl'];
		if (layerJSon['webCacheUrl']) {
			serviceUrl = layerJSon['webCacheUrl'];
		}
		return serviceUrl;
	},

	_getWMSExtraServiceUrls: function(layerJSon) {
		return layerJSon['extraWmsServiceUrls'];
	},

	_getWMSLayerParams: function(layerJSon) {
		var isBaseLayer = layerJSon['isBaseLayer'];

		// Set the parameters used in the URL to request the tiles.
		var layerParams = {
			layers: layerJSon['layerId']
		};
		// The WMS version is also used in the WMS requests and the legend graphics.
		if (layerJSon['wmsVersion']) {
			layerParams.version = layerJSon['wmsVersion'];
		}

		if (layerJSon['wmsRequestMimeType']) {
			layerParams.format = layerJSon['wmsRequestMimeType'];
		}

		if (isBaseLayer) {
			layerParams.transparent = false;
		} else {
			layerParams.transparent = true;
		}
		return layerParams;
	},

	_getWMSLayerOptions: function(layerJSon) {
		var isBaseLayer = layerJSon['isBaseLayer'];

		// Set the OpenLayer options, used by the library.
		var layerOptions = {
			//visibility: layerJSon['<initialState>']['<activated>'],
			//opacity: layerJSon['<initialState>']['<opacity>'],
			isBaseLayer: isBaseLayer,
			displayInLayerSwitcher: true,
			wrapDateLine : true,
			buffer: 0,
			projection: this.map.getProjectionObject()
		};

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
		return layerOptions;
	},

	_createNCWMSLayer: function(layerJSon) {
		// TODO Support Multiple URLS => this._getWMSExtraServiceUrls(layerJSon),
		return new OpenLayers.Layer.ux.NCWMS(
			this._getTitle(layerJSon),
			this._getWMSServiceUrl(layerJSon),
			this._getWMSLayerParams(layerJSon),
			this._getWMSLayerOptions(layerJSon)
		);
	},

	_createWMSLayer: function(layerJSon) {
		// TODO Support Multiple URLS => this._getWMSExtraServiceUrls(layerJSon),
		return new OpenLayers.Layer.WMS(
			this._getTitle(layerJSon),
			this._getWMSServiceUrl(layerJSon),
			this._getWMSLayerParams(layerJSon),
			this._getWMSLayerOptions(layerJSon)
		);
	},

	_createWMTSLayer: function(layerJSon) {
		// TODO Create a WMTS layer
		return this._createWMSLayer(layerJSon);
	},

	_createKMLLayer: function(layerJSon) {
		var kmlUrl = layerJSon['serverUrls'] + layerJSon['layerFilename'];

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
		var googleLayer = new OpenLayers.Layer.Google(
			// "Google Physical", "Google Streets", "Google Hybrid", "Google Satellite"
			this._getTitle(layerJSon),
			{
				// google.maps.MapTypeId.TERRAIN, google.maps.MapTypeId.ROADMAP, google.maps.MapTypeId.HYBRID, google.maps.MapTypeId.SATELLITE
				type: google.maps.MapTypeId[layerJSon['layerId']]
			}
		);

		return googleLayer;
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
	addLayersById: function(layerIds) {
		var that = this;
		Atlas.core.requestLayersJSon(layerIds, function(layersJSon) {
			that.addLayers(layersJSon);
		});
	},

	addLayerById: function(layerId) {
		this.addLayersById([layerId]);
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
		switch (layerJSon['datasourceType']) {
			case 'NCWMS':
				layer = this._createNCWMSLayer(layerJSon);
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
			default:
				alert('Layer type '+layerJSon['datasourceType']+' is not implemented.');
		}
		if (!layer) {
			// TODO THROW EXCEPTION
			return;
		}

		layer.json = layerJSon;
		layer.hideInLegend = false;//!layerJSon['<initialState>']['<legendActivated>'];

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
		}
		layer.getHideInLegend = function() {
			return layer.hideInLegend;
		}

		// Add the layer to the Map
		this.map.addLayer(layer);

		// Add feature request listener for that layer, if needed
		if (this.wmsFeatureInfo && layerJSon['wmsQueryable']) {
			this.wmsFeatureInfo.addLayer(layer);
		}

		if (layer.isBaseLayer) {
			this.map.setBaseLayer(layer);
		}

		this.ol_fireEvent('layerAdded', {layerJSon: layerJSon});
	},

	removeLayer: function(layer) {
		var layerJSon = layer.json;

		if (layerJSon && layerJSon['wmsQueryable']) {
			this.wmsFeatureInfo.removeLayer(layer);
		}

		this.map.removeLayer(layer);
		// Destroy is a destructor: this is to alleviate cyclic
		// references which the Javascript garbage cleaner can not
		// take care of on its own.
		layer.destroy();

		if (layerJSon) {
			this.ol_fireEvent('layerRemoved', {layerJSon: layerJSon});
		}
	},

	locateLayer: function(layer) {
		var bounds = null;
		if (layer.json && layer.json['layerBoundingBox']) {
			// Bounds order in JSon: left, bottom, right, top
			var boundsArray = layer.json['layerBoundingBox']

			// Bounds order as requested by OpenLayers: left, bottom, right, top
			// NOTE: Reprojection can not work properly if the top or bottom overpass 85
			bounds = new OpenLayers.Bounds(
					boundsArray[0],
					(boundsArray[1] < -85 ? -85 : boundsArray[1]),
					boundsArray[2],
					(boundsArray[3] > 85 ? 85 : boundsArray[3])
			);

			if (bounds != null) {
				bounds = bounds.transform(this.defaultLonLatProjection, this.map.getProjectionObject());
			}
		}
		if (bounds == null) {
			bounds = layer.getDataExtent();
		}
		if (bounds != null) {
			this.map.zoomToExtent(bounds, true);
		} else {
			alert("This layer can not be located");
		}
	}
};
