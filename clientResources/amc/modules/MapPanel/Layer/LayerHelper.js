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

Atlas.Layer.LayerHelper = {
	// Static method
	createLayer: function(mapPanel, layerJSon) {
		var atlasLayer = null;

		if (typeof(layerJSon['dataSourceType']) === 'undefined') {
			alert('Layer '+layerJSon['layerId']+' has no dataSourceType defined.');
			return;
		}

		switch (layerJSon['dataSourceType']) {
			case 'DUMMY':
				// Dummy layers are used to create a tree node that can contains a layer object that is not load on the map.
				atlasLayer = new Atlas.Layer.Dummy(mapPanel, layerJSon);
				break;
			case 'NCWMS':
				atlasLayer = new Atlas.Layer.NCWMS(mapPanel, layerJSon);
				break;
			case 'ARCGIS_MAPSERVER':
				atlasLayer = new Atlas.Layer.ArcGISMapServer(mapPanel, layerJSon);
				break;
			case 'ARCGIS_CACHE':
				atlasLayer = new Atlas.Layer.ArcGISCache(mapPanel, layerJSon);
				break;
			case 'WMS':
				atlasLayer = new Atlas.Layer.WMS(mapPanel, layerJSon);
				break;
			case 'WMTS':
				atlasLayer = new Atlas.Layer.WMTS(mapPanel, layerJSon);
				break;
			case 'KML':
				atlasLayer = new Atlas.Layer.KML(mapPanel, layerJSon);
				break;
			case 'GOOGLE':
				atlasLayer = new Atlas.Layer.Google(mapPanel, layerJSon);
				break;
			case 'BING':
				atlasLayer = new Atlas.Layer.Bing(mapPanel, layerJSon);
				break;
			case 'XYZ':
				atlasLayer = new Atlas.Layer.XYZ(mapPanel, layerJSon);
				break;
			case 'SERVICE':
			case 'GROUP':
				atlasLayer = new Atlas.Layer.Group(mapPanel, layerJSon);
				break;
			default:
				alert('Layer type '+layerJSon['dataSourceType']+' for layer '+layerJSon['layerId']+', is not implemented.');
		}

		return atlasLayer;
	}
};
