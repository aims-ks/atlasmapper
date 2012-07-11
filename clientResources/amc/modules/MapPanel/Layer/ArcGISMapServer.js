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

Atlas.Layer.ArcGISMapServer = OpenLayers.Class(Atlas.Layer.AbstractLayer, {
	/**
	 * Constructor: Atlas.Layer.ArcGISMapServer
	 *
	 * Parameters:
	 * jsonLayer - {Object} Hashtable of layer attributes
	 * mapPanel - {Object} Instance of the MapPanel in which the layer is used
	 */
	initialize: function(mapPanel, jsonLayer) {
		Atlas.Layer.AbstractLayer.prototype.initialize.apply(this, arguments);

		var url = this.json['wmsServiceUrl'];
		if (this.json['arcGISPath']) {
			url += '/' + this.json['arcGISPath'];
		}
		url += '/MapServer/export';

		this.layer = this.extendLayer(new OpenLayers.Layer.ArcGIS93Rest(
			this.getTitle(),
			url,
			this.getArcGISLayerParams(),
			this.getArcGISLayerOptions()
		));
	},

	getArcGISLayerParams: function() {
		var layerParams = {
			layers: "show:" + (this.json['layerName'] || this.json['layerId']),
			transparent: true
		};

		if (typeof(this.json['forcePNG24']) !== 'undefined' && this.json['forcePNG24']) {
			layerParams['format'] = 'PNG24';
		}

		if (typeof(this.json['olParams']) !== 'undefined') {
			layerParams = this.applyOlOverrides(layerParams, this.json['olParams']);
		}

		return layerParams;
	},

	getArcGISLayerOptions: function() {
		if (this.json == null) {
			return null;
		}

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
		if (this.json['projection']) {
			layerOptions.projection = this.json['projection'];
		} else if (this.mapPanel && this.mapPanel.map && this.mapPanel.map.getProjection() === 'EPSG:900913') {
			layerOptions.projection = 'EPSG:102100';
		}

		if (typeof(this.json['olOptions']) !== 'undefined') {
			layerOptions = this.applyOlOverrides(layerOptions, this.json['olOptions']);
		}

		return layerOptions;
	}
});
