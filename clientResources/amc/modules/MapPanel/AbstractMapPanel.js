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
		"locateLayer", "saveStateChange",
		'legendVisibilityChange', 'dpiChange',
		'render'
	],

	// The OpenLayers event object, set in initComponent function
	events: null,

	// The MapPanel is always in the center
	region: 'center',

	mapId: 0,
	center: null,
	zoom: 0,

	// The API doc specifically say that the default DPI is 90,
	// but in fact, it's somewhere between 91 and 95.
	// Since this setting do not make a big difference, I prefer
	// to use the value specified in the API (that might be a bug
	// that will be fix later on).
	// http://docs.geoserver.org/latest/en/user/services/wms/vendor.html#format-options
	DEFAULT_DPI: 90,
	dpi: 90,

	renderTo: null,
	embedded: false,

	// The feature request manager
	featureInfo: null,

	// Avoid clash with "rendered", which is defined in GeoExt.MapPanel
	isRendered: false,

	urlState: null,
	// This is used to pull the map state to the top frame when the map is embedded,
	// useful to auto-update the URL in the embedded preview.
	pullState: false,

	defaultLonLatProjection: null,

	initComponent: function() {
		var that = this;

		// Basic save-state
		var parameters = OpenLayers.Util.getParameters();

		this.urlState = {
			layerIds: null,
			styles: null,
			visibilities: null,
			opacities: null,
			center: null,
			zoom: null,
			locate: null,
			loadDefaultLayers: false,
			pullState: false
		};
		if (typeof(parameters['l'+this.mapId]) !== 'undefined' && parameters['l'+this.mapId] != null) {
			this.urlState.layerIds = (parameters['l'+this.mapId].constructor == Array) ? parameters['l'+this.mapId] : [parameters['l'+this.mapId]];
		}
		if (typeof(parameters['s'+this.mapId]) !== 'undefined' && parameters['s'+this.mapId] != null) {
			this.urlState.styles = (parameters['s'+this.mapId].constructor == Array) ? parameters['s'+this.mapId] : [parameters['s'+this.mapId]];
		}
		if (typeof(parameters['v'+this.mapId]) !== 'undefined' && parameters['v'+this.mapId] != null) {
			this.urlState.visibilities = (parameters['v'+this.mapId].constructor == Array) ? parameters['v'+this.mapId] : [parameters['v'+this.mapId]];
		}
		if (typeof(parameters['o'+this.mapId]) !== 'undefined' && parameters['o'+this.mapId] != null) {
			this.urlState.opacities = (parameters['o'+this.mapId].constructor == Array) ? parameters['o'+this.mapId] : [parameters['o'+this.mapId]];
		}

		if (typeof(parameters['ll']) !== 'undefined' && parameters['ll'] != null && parameters['ll'].constructor == Array) {
			this.urlState.center = parameters['ll'];
		}
		if (typeof(parameters['z']) !== 'undefined' && parameters['z'] != null) {
			this.urlState.zoom = parseInt(parameters['z']);
		}
		if (typeof(parameters['loc']) !== 'undefined' && parameters['loc'] != null) {
			this.urlState.locate = parameters['loc'];
		}
		if (typeof(parameters['dl']) !== 'undefined' && parameters['dl'] != null) {
			this.urlState.loadDefaultLayers = (parameters['dl'].toLowerCase() === 't' || parameters['dl'].toLowerCase() === 'true');
		}

		if (typeof(parameters['pullState']) !== 'undefined' && parameters['pullState'] != null) {
			this.pullState = (parameters['pullState'].toLowerCase() === 't' || parameters['pullState'].toLowerCase() === 'true');
		}

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

		// URL overrides
		if (this.urlState != null) {
			if (this.urlState.center != null && this.urlState.center.length == 2) {
				this.center = new OpenLayers.LonLat(this.urlState.center[0], this.urlState.center[1]);
				if (projection != this.defaultLonLatProjection) {
					this.center = this.center.transform(this.defaultLonLatProjection, projection);
				}
			}
			if (this.urlState.zoom != null) {
				this.zoom = this.urlState.zoom;
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
				new OpenLayers.Control.Zoom(),          // Nice looking, simple zoom box
				new OpenLayers.Control.PinchZoom(),     // Mobile device zooming, with 2 fingers
				new OpenLayers.Control.ScaleLine({geodesic: true}),     // Displays a small line indicator representing the current map scale on the map. ("geodesic: true" has to be set to recalculate the scale line when the map get span closer to the poles)
				//new OpenLayers.Control.Scale(),         // Displays the map scale (example: 1:1M).
				new OpenLayers.Control.MousePosition({
					displayProjection: this.defaultLonLatProjection
				}),                                     // Displays geographic coordinates of the mouse pointer
				new OpenLayers.Control.Navigation(),    // Including TouchNavigation
				new OpenLayers.Control.KeyboardDefaults(), // Adds panning and zooming functions, controlled with the keyboard.  By default arrow keys pan, +/- keys zoom & Page Up/Page Down/Home/End scroll by three quarters of a page.
				new OpenLayers.Control.ZoomBox()        // Enables zooming directly to a given extent, by drawing a box on the map.  The box is drawn by holding down shift, whilst dragging the mouse.
			];
		} else {
			controls = [
				new OpenLayers.Control.PanZoomBar(),    // Pan and Zoom (with a zoom bar) controls, in the top left corner
				new OpenLayers.Control.PinchZoom(),     // Mobile device zooming, with 2 fingers
				new OpenLayers.Control.ScaleLine({geodesic: true}),     // Displays a small line indicator representing the current map scale on the map. ("geodesic: true" has to be set to recalculate the scale line when the map get span closer to the poles)
				//new OpenLayers.Control.Scale(),         // Displays the map scale (example: 1:1M).
				new OpenLayers.Control.MousePosition({
					displayProjection: this.defaultLonLatProjection
				}),                                     // Displays geographic coordinates of the mouse pointer
				new OpenLayers.Control.Navigation(),    // Including TouchNavigation
				/*
				new OpenLayers.Control.OverviewMap({
					layers: [
						new OpenLayers.Layer.WMS(
							"OverviewMap",
							"http://e-atlas.org.au/maps/wms",
							{layers: 'ea:World_NED_NE2'}
						)
					]
				}), // Creates a small overview map
				*/
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

		var mapOptions = {
			'projection' : projection,
			'maxExtent' : maxExtent,
			'controls' : controls
		};

		// Copy all options of Atlas.conf['mapOptions'] to mapOptions.
		// NOTE: Do not assign mapOptions = Atlas.conf['mapOptions'] since the options of mapOptions can change and
		// the config must remained unchanged (it will be used by the other map panels when displaying multiple maps)
		for (optionKey in Atlas.conf['mapOptions']) {
			if (optionKey != 'projection' && optionKey != 'maxExtent' && optionKey != 'controls' && Atlas.conf['mapOptions'].hasOwnProperty(optionKey)) {
				mapOptions[optionKey] = Atlas.conf['mapOptions'][optionKey];
			}
		}

		// This attribute should NOT be true or else it cause all sort of
		// weird side effects with base layers. The default value is false
		// but, it is changed to true at some point, probably by GeoEXT.
		if (typeof(mapOptions['allOverlays']) == 'undefined') { mapOptions['allOverlays'] = false; }

		// Set the default values for the map: those values are modified when a new base layer is added
		if (typeof(mapOptions['maxResolution']) == 'undefined') { mapOptions['maxResolution'] = 0.703125; }
		if (typeof(mapOptions['numZoomLevels']) == 'undefined') { mapOptions['numZoomLevels'] = 16; }

		/*
		if (this.embedded) {
			//mapOptions.zoom = this.zoom; // BUG: This properties is ignored: http://trac.osgeo.org/openlayers/ticket/3362
			mapOptions.center = new OpenLayers.LonLat(this.center[0], this.center[1]);
		}
		*/

		if (typeof(this.renderTo) != 'undefined' && this.renderTo != null) {
			mapOptions.div = this.renderTo;
		}

		if (this.mapId) {
			mapOptions.id = this.mapId;
		}

		this.map = new OpenLayers.Map(mapOptions);

		if (this.pullState) {
			this.map.events.on({
				"moveend": function(evt) {
					if (top && top.Atlas && top.Atlas.core && top.Atlas.core.mapPanels[0]) {
						top.Atlas.core.mapPanels[0].ol_fireEvent('saveStateChange', {urlSaveState: that._createUrlSaveState()});
					}
				},
				"addlayer": function(evt) {
					if (top && top.Atlas && top.Atlas.core && top.Atlas.core.mapPanels[0]) {
						top.Atlas.core.mapPanels[0].ol_fireEvent('saveStateChange', {urlSaveState: that._createUrlSaveState()});
					}
				}
			});
		}

		this.map.render = function(div) {
			// Call the original render method
			OpenLayers.Map.prototype.render.apply(that.map, arguments);

			window.setTimeout(function() {
				// Fire the render event, after 1 millisecond, just to be sure every process as finish...
				that.isRendered = true;
				that.ol_fireEvent("render");
			}, 1);
		};

		// Add a dummy base layer to solve all the problems related to
		// maps without base layer.
		var dummyBaseLayerOptions = {
			maxExtent: maxExtent,
			isBaseLayer: true,
			visibility: true,
			displayInLayerSwitcher: false
		};
		var dummyBaseLayer = new OpenLayers.Layer("Base", dummyBaseLayerOptions);
		this.map.addLayer(dummyBaseLayer);

		/*
		// Work around the zoom bug: http://trac.osgeo.org/openlayers/ticket/3362
		if (this.embedded) {
			this.map.zoomTo(this.zoom);
		}
		*/

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
		if (Atlas.MapPanel.GetFeatureInfo) {
			this.featureInfo = new Atlas.MapPanel.GetFeatureInfo({
				map: this.map
			});
		}


		// Auto-set some extra layer attributes and methods
		// for layers that has a json attribute (I.E. those methods
		// do not apply to the markers)
		this.map.events.on({"preaddlayer": function(evt) {
			if (evt && evt.layer && evt.layer.atlasLayer && evt.layer.atlasLayer.json) {
				that._beforeLayerAdd(evt.layer);
			}
		}});
		this.map.events.on({"addlayer": function(evt) {
			if (evt && evt.layer && evt.layer.atlasLayer && evt.layer.atlasLayer.json) {
				that._afterLayerAdd(evt.layer);
			}
		}});
		this.map.events.on({"preremovelayer": function(evt) {
			if (evt && evt.layer && evt.layer.atlasLayer && evt.layer.atlasLayer.json) {
				that._beforeLayerRemove(evt.layer);
			}
		}});
		this.map.events.on({"removelayer": function(evt) {
			if (evt && evt.layer && evt.layer.atlasLayer && evt.layer.atlasLayer.json) {
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
			evt.layer.atlasLayer.locate();
		});

		// Add layers specified by the "layers" URL parameter
		if (this.urlState != null && this.urlState.layerIds != null) {
			Atlas.core.requestLayersJSon(this.urlState.layerIds, function(layersJSon) {
				if (that.urlState.styles != null || that.urlState.visibilities != null || that.urlState.opacities != null) {
					for (var i=0; i<layersJSon.length; i++) {
						// Clone the object, to avoid modifying the original
						layersJSon[i] = clone(layersJSon[i]);

						// Apply URL styles
						if (that.urlState.styles != null && typeof(that.urlState.styles[i]) != 'undefined' && that.urlState.styles[i] != null && that.urlState.styles[i].length > 0) {
							if (typeof(layersJSon[i].styles) != 'undefined' && layersJSon[i].styles != null) {
								var found = false;
								// Remove previous default
								for (style in layersJSon[i].styles) {
									if (layersJSon[i].styles.hasOwnProperty(style)) {
										if (layersJSon[i].styles[style]["default"] == true) {
											delete(layersJSon[i].styles[style]["default"]);
										}
									}
								}
								// Add new default
								if (typeof(layersJSon[i].styles[that.urlState.styles[i]]) == 'undefined' || layersJSon[i].styles[that.urlState.styles[i]] == null) {
									layersJSon[i].styles[that.urlState.styles[i]] = {};
								}
								layersJSon[i].styles[that.urlState.styles[i]]["default"] = true;
							} else {
								layersJSon[i].styles = {};
								layersJSon[i].styles[that.urlState.styles[i]] = { "default": true };
							}
						}

						// Apply URL opacities
						if (that.urlState.opacities != null && typeof(that.urlState.opacities[i]) != 'undefined' && that.urlState.opacities[i] != null && that.urlState.opacities[i].length > 0) {
							if (typeof(layersJSon[i].olOptions) == 'undefined' || layersJSon[i].olOptions == null) {
								layersJSon[i].olOptions = {};
							}
							layersJSon[i].olOptions.opacity = that.urlState.opacities[i];
						}

						// Apply URL visibilities
						if (that.urlState.visibilities != null && typeof(that.urlState.visibilities[i]) != 'undefined' && that.urlState.visibilities[i] != null && that.urlState.visibilities[i].length > 0) {
							layersJSon[i]['selected'] = (that.urlState.visibilities[i].toLowerCase() === 't' || that.urlState.visibilities[i].toLowerCase() === 'true');
						}
					}
				}
				that.addLayers(layersJSon);
			});
		}

		if (this.urlState == null || this.urlState.layerIds == null || this.urlState.loadDefaultLayers == null || this.urlState.loadDefaultLayers === true) {
			// Add default layers to the map
			var defaultLayers = Atlas.conf['defaultLayers'];
			// Normalise the layers and load them in the core cache.
			Atlas.core.loadNewLayersCache(defaultLayers);
			// Add layers in reverse order - the last added will be on top
			for(var i=defaultLayers.length-1; i>=0; i--){
				// Get the layer from the core cache and load it in the map
				// NOTE The layer from the core cache is normalized.
				this.addLayerById(defaultLayers[i].layerId);
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

	// Events Listener
	ol_un: function(event, fct, scope) {
		var evtObj = {};
		evtObj[event] = fct;
		if (typeof(scope) !== 'undefined') {
			evtObj.scope = scope;
		}
		this.events.un(evtObj);
	},

	// Fire Events
	ol_fireEvent: function(event, attributes) {
		this.events.triggerEvent(event, attributes);
	},

	// Simply return the map Zoom level, patched for Bing layers (you did it again, Microsoft!)
	getStandardZoomLevel: function() {
		var zoom = this.map.getZoom();

		if (this.map.baseLayer instanceof OpenLayers.Layer.Bing) {
			zoom++;
		}

		return zoom;
	},

	// Simply set the map Zoom level, patched for Bing layers (you did it again, Microsoft!)
	setStandardZoomLevel: function(zoomLevel) {
		var zoom = zoomLevel;

		if (this.map.baseLayer instanceof OpenLayers.Layer.Bing) {
			zoom--;
		}

		this.map.zoomTo(zoom);
	},

	// Simply set the map Center and Zoom level, patched for Bing layers (you did it again, Microsoft!)
	setStandardCenter: function(center, zoomLevel) {
		var zoom = null;

		if (typeof(zoomLevel) !== 'undefined' && zoomLevel !== null) {
			zoom = zoomLevel;

			if (this.map.baseLayer instanceof OpenLayers.Layer.Bing) {
				zoom--;
			}
		}

		this.map.setCenter(center, zoom);
	},

	_createUrlSaveState: function() {
		if (this.map == null) {
			return null;
		}

		var state = {};

		// Zoom level (z)
		state['z'] = this.getStandardZoomLevel();

		// Center (ll)
		if (typeof(this.map.getCenter) === 'function') {
			var center = this.map.getCenter();
			if (center != null) {
				if (this.map.getProjectionObject() != this.defaultLonLatProjection) {
					center = center.transform(this.map.getProjectionObject(), this.defaultLonLatProjection);
				}
				// 5 decimals is about 1m precision.
				state['ll'] = center.lon.toFixed(5) + ',' + center.lat.toFixed(5);
			}
		}

		// LAYERS - Note that the embedded map will have only one map, so the only needed parameters here are l0, s0, etc.
		if (typeof(this.map.layers) !== 'undefined') {
			var l0 = "";
			var s0 = "";
			var o0 = "";
			var v0 = "";
			var first = true;
			for (var i=this.map.layers.length; i>=0; i--) {
				var layer = this.map.layers[i];
				if (layer != null &&
						typeof(layer.atlasLayer) !== 'undefined' && layer.atlasLayer != null &&
						typeof(layer.atlasLayer.json) !== 'undefined' && layer.atlasLayer.json != null) {

					var jsonLayer = layer.atlasLayer.json;
					if (this._isLayerNeededInUrl(jsonLayer)) {
						if (!first) {
							l0 += ',';
							s0 += ',';
							o0 += ',';
							v0 += ',';
						}
						// Layers (l0)
						l0 += jsonLayer['layerId']
						// Styles (s0)
						if (typeof(layer.params) !== 'undefined' && layer.params != null &&
								typeof(layer.params['STYLES']) !== 'undefined' && layer.params['STYLES'] != null &&
								layer.params['STYLES'].length > 0) {
							s0 += layer.params['STYLES'];
						}
						// Opacities (o0)
						if (typeof(layer.opacity) !== 'undefined' && layer.opacity != null && layer.opacity !== 1) {
							o0 += layer.opacity
						}
						// Visibilities (v0)
						if (typeof(layer.visibility) !== 'undefined' && layer.visibility === false) {
							v0 += 'f'
						}
						first = false;
					}
				}
			}
			// NOTE: ",,*" is twice faster than ",+"
			l0 = l0.replace(/,,*$/, '');
			s0 = s0.replace(/,,*$/, '');
			o0 = o0.replace(/,,*$/, '');
			v0 = v0.replace(/,,*$/, '');
			if (l0.length > 0) {
				state['l0'] = l0;
				if (s0.length > 0) { state['s0'] = s0; }
				if (o0.length > 0) { state['o0'] = o0; }
				if (v0.length > 0) { state['v0'] = v0; }
			}
		}

		return state;
	},

	_isLayerNeededInUrl: function(jsonLayer) {
		// Layer group are not added to the URL (but not their layers)
		if (jsonLayer['dataSourceType'] == 'SERVICE' || jsonLayer['dataSourceType'] == 'GROUP') {
			return false;
		}

		// Ignore fake layers, print frame layers, etc.
		if (typeof(jsonLayer['layerId']) === 'undefined' || jsonLayer['layerId'] == null) {
			return false;
		}

		// Add this to remove Default layers from the URL and change the logic at the end of AbstractMapPanel.initComponent
		/*
		for(var i=0; i<Atlas.conf['defaultLayers'].length; i++){
			if (Atlas.conf['defaultLayers'][i].layerId === jsonLayer['layerId']) {
				return false;
			}
		}
		*/

		return true;
	},

	/**
	 * Add layers from an array of layer IDs
	 * Parent is a layer group (AtlasLayer) when the layer is a child of that group
	 */
	addLayersById: function(layerIds, path, parent) {
		var that = this;
		Atlas.core.requestLayersJSon(layerIds, function(layersJSon) {
			if (typeof(path) != 'undefined' && path.length > 0) {
				for (var i=0; i<layersJSon.length; i++) {
					// Clone the object, to avoid modifying the original
					layersJSon[i] = clone(layersJSon[i]);
					layersJSon[i].path = path;
				}
			}
			that.addLayers(layersJSon, parent);
		});
	},

	addLayerById: function(layerId, path) {
		this.addLayersById([layerId], path);
	},

	/**
	 * layerJSon: Layer object as returned by Atlas.core.getLayerJSon
	 * Parent is a layer group (AtlasLayer) when the layer is a child of that group
	 */
	addLayers: function(layersJSon, parent) {
		for (var i=layersJSon.length-1; i>=0; i--) {
			this.addLayer(layersJSon[i], parent);
		}
	},

	/**
	 * layerJSon: Layer object as returned by Atlas.core.getLayerJSon
	 * Parent is a layer group (AtlasLayer) when the layer is a child of that group
	 */
	addLayer: function(layerJSon, parent) {
		var that = this;
		if (!this.isRendered) {
			this.ol_on("render", function(evt) {
				that.addLayer(layerJSon, parent);
			});
			return;
		}

		var atlasLayer = Atlas.Layer.LayerHelper.createLayer(this, layerJSon, parent);

		if (!atlasLayer) {
			// TODO THROW EXCEPTION
			return;
		}

		if (atlasLayer.hasRealLayer()) {
			// Add the layer to the Map
			// NOTE: This method trigger _beforeLayerAdd and _afterLayerAdd
			this.map.addLayer(atlasLayer.layer);
		}
	},

	removeLayer: function(layer) {
		// Remove the layer from the Map
		// NOTE: This method trigger _afterLayerRemove
		this.map.removeLayer(layer);
	},

	changeDpi: function(dpi) {
		if (this.dpi != dpi) {
			// for validation
			var dpiMin = 10, dpiMax = 1440;

			if (dpi >= dpiMin && dpi <= dpiMax) {
				this.dpi = dpi;
				this.ol_fireEvent('dpiChange', {dpi: dpi});
			} else {
				// TODO Error message on the page
				alert('Invalid DPI value. Please, select a value between ' + dpiMin + ' and ' + dpiMax + '.');
			}
		}
	},

	// Private
	_beforeLayerAdd: function(layer) {
	},

	// Private
	// This method fire events and set some attributes
	_afterLayerAdd: function(layer) {
		if (layer == null || layer.atlasLayer == null || layer.atlasLayer.json == null) {
			return;
		}

		var layerJSon = layer.atlasLayer.json;

		// Add feature request listener for that layer, if needed
		if (this.featureInfo && (layerJSon['wmsQueryable'] || layerJSon['dataSourceType'] == 'ARCGIS_MAPSERVER')) {
			this.featureInfo.addLayer(layer.atlasLayer);
		}

		if (layer.isBaseLayer) {
			// TODO Modify this after implementing Save State
			if (typeof(layerJSon['selected']) === 'boolean') {
				if (layerJSon['selected']) {
					this.map.setBaseLayer(layer);
				}
				delete(layerJSon['selected']);
			} else {
				this.map.setBaseLayer(layer);
			}
		}

		if (typeof(this.urlState) !== 'undefined' && this.urlState != null) {
			if (this.urlState.locate === layerJSon['layerId']) {
				this.ol_fireEvent('locateLayer', {layer: layer});
			}
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
		var layerJSon = layer.atlasLayer.json;

		if (layerJSon && (layerJSon['wmsQueryable'] || layerJSon['dataSourceType'] == 'ARCGIS_MAPSERVER')) {
			this.featureInfo.removeLayer(layer.atlasLayer);
		}
	},

	// Private
	_afterLayerRemove: function(layer) {
		if (layer == null || layer.atlasLayer == null || layer.atlasLayer.json == null) {
			return;
		}

		var layerJSon = layer.atlasLayer.json;

		if (layerJSon) {
			this.ol_fireEvent('layerRemoved', {layerJSon: layerJSon});
		}
	}
};
