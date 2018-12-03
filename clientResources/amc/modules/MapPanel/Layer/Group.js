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

Atlas.Layer.Group = OpenLayers.Class(Atlas.Layer.AbstractLayer, {
	children: null,
	supportLoadEvents: true,

	/**
	 * Constructor: Atlas.Layer.Group
	 *
	 * Parameters:
	 * jsonLayer - {Object} Hashtable of layer attributes
	 * mapPanel - {Object} Instance of the MapPanel in which the layer is used
	 */
	initialize: function(mapPanel, jsonLayer, parent) {
		Atlas.Layer.AbstractLayer.prototype.initialize.apply(this, arguments);

		this.children = [];

		if (this.json != null) {

			// Temporary patch
			// singleFusedMapCache (used by ArcGIS services): default false
			var singleFusedMapCache = typeof(this.json['singleFusedMapCache']) !== 'undefined' && this.json['singleFusedMapCache'];

			if (this.json['layers'] && this.json['layers'].length > 0) {
				if (singleFusedMapCache) { // I NEED A SERVICE LAYER TYPE!!
					// Path: Array of layerJSON object, with a unique ID for this instance of the group
					var pathSuffixId = "_" + new Date().getTime();
					var path = [];

					// The path is dynamically created for the GROUP (folder)
					if (this.json['path']) {
						// Clone the path
						path = this.json['path'].slice(0);
					}

					this.json['layerType'] = 'ARCGIS_CACHE';
					this.json['path'] = path;
					arcGISPath = '';
					for (var i=0, len=path.length; i<len; i++) {
						arcGISPath += path[i]['layerName'] + '/';
					}
					arcGISPath += this.json['layerName'];
					this.json['arcGISPath'] = arcGISPath;

					// Add all children under that path. If a child is a GROUP, it will pass
					// through this function again.
					if (this.mapPanel) {
						// layerJSON, folder Path, parent
						this.mapPanel.addLayer(this.json, parent);
					}
				} else {
					// The "layers" attribute define the children layers of the group.
					// NOTE: Since each layer may appear in multiple groups, the attribute path can
					//     not be defined in the layer's configuration. It has to be dynamically
					//     created for each instance of the layer.

					// Path: Array of layerJSON object, with a unique ID for this instance of the group
					var pathSuffixId = "_" + new Date().getTime();
					var path = [];

					// The path is dynamically created for the GROUP (folder)
					if (this.json['path']) {
						// Clone the path
						path = this.json['path'].slice(0);
					}

					var pathPart = clone(this.json);
					pathPart.id = (this.json['layerName'] || this.json['layerId']) + pathSuffixId;
					path.push(pathPart);

					// Add all children under that path. If a child is a GROUP, it will pass
					// through this function again.
					if (this.mapPanel) {
						// layerJSON, folder Path, parent
						this.mapPanel.addLayersById(this.json['layers'], path, this);
					}
				}
			}
		}

		this.setLayer(new OpenLayers.Layer(jsonLayer['title'], jsonLayer.olOptions));
		this.layer.id = jsonLayer['id'];
	},

	isLoading: function(ignoredLayer) {
		if (!this.supportLoadEvents) {
			return false;
		}

		var isLoaded = true;
		if (typeof(ignoredLayer) === 'undefined') {
			ignoredLayer = null;
		}
		for (var i=0; i < this.children.length && isLoaded; i++) {
			if (this.children[i] != ignoredLayer && this.children[i].isLoading()) {
				isLoaded = false;
			}
		}

		if (isLoaded) {
			this.loaded = true;
		}

		return !this.loaded;
	},

	addChild: function(childAtlasLayer) {
		this.children.push(childAtlasLayer);
		this._addChildEventListeners(childAtlasLayer);
	},

	_addChildEventListeners: function (childAtlasLayer) {
		if (childAtlasLayer && childAtlasLayer.layer && childAtlasLayer.layer.events) {
			childAtlasLayer.layer.events.on({
				'loadstart': function(event) {
					this._onChildLayerLoadStart(childAtlasLayer);
				},
				'loadend': function(event) {
					this._onChildLayerLoadEnd(childAtlasLayer);
				},
				// This event is not triggered anywhere, but maybe someday it will be...
				'loadcancel': function(event) {
					this._onChildLayerLoadCancel(childAtlasLayer);
				},
				scope: this
			});
		}
	},

	_onChildLayerLoadStart: function(childAtlasLayer) {
		if (this.loaded) {
			this.loaded = false;
			this.layer.events.triggerEvent('loadstart');
		}
	},

	_onChildLayerLoadEnd: function(childAtlasLayer) {
		childAtlasLayer.layer.loaded = true;
		if (!this.loaded && !this.isLoading(childAtlasLayer)) {
			this.layer.events.triggerEvent('loadend');
		}
	},

	_onChildLayerLoadCancel: function(childAtlasLayer) {
		this._onChildLayerLoadEnd();
	}
});
