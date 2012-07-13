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

Atlas.Layer.ArcGISCache = OpenLayers.Class(/*Atlas.Layer.AbstractLayer*/ Atlas.Layer.ArcGISMapServer, {
	/**
	 * Constructor: Atlas.Layer.ArcGISCache
	 *
	 * Parameters:
	 * jsonLayer - {Object} Hashtable of layer attributes
	 * mapPanel - {Object} Instance of the MapPanel in which the layer is used
	 */
	initialize: function(mapPanel, jsonLayer, parent) {

		// TODO Do not send all options to the client, use JSONP instead. Also change the logic; ArcGIS Cache are per Service, not per Layer.
		// http://openlayers.org/dev/examples/arcgiscache_jsonp.html

		// Temporary fix: Do not use the cache...
		Atlas.Layer.ArcGISMapServer.prototype.initialize.apply(this, arguments);

		/*
		Atlas.Layer.AbstractLayer.prototype.initialize.apply(this, arguments);
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

		this.layer = new OpenLayers.Layer.ArcGISCache(
			layerJSon['title'],
			url,
			layerOptions
		);
		*/
	}
});
