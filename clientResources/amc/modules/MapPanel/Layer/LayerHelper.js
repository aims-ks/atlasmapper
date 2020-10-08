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

Atlas.Layer.LayerHelper = {
    // Static method
    // Parent is a layer group (AtlasLayer) when the layer is a child of that group
    createLayer: function(mapPanel, layerJSon, parent) {
        var atlasLayer = null;

        if (layerJSon instanceof OpenLayers.Layer) {
            atlasLayer = new Atlas.Layer.AbstractLayer(mapPanel, {}, parent);
            atlasLayer.getExtent = function() {
                return this.computeExtent();
            };
            atlasLayer.setLayer(layerJSon);

        } else {
            if (typeof(layerJSon['layerType']) === 'undefined') {
                alert('Layer '+layerJSon['layerId']+' has no layerType defined.');
                return null;
            }

            switch (layerJSon['layerType']) {
                case 'DUMMY':
                    // Dummy layers are used to create a tree node that can contains a layer object that is not load on the map.
                    atlasLayer = new Atlas.Layer.Dummy(mapPanel, layerJSon, parent);
                    break;
                case 'NCWMS':
                    atlasLayer = new Atlas.Layer.NCWMS(mapPanel, layerJSon, parent);
                    break;
                case 'THREDDS':
                    atlasLayer = new Atlas.Layer.THREDDS(mapPanel, layerJSon, parent);
                    break;
                case 'ARCGIS_MAPSERVER':
                    atlasLayer = new Atlas.Layer.ArcGISMapServer(mapPanel, layerJSon, parent);
                    break;
                case 'ARCGIS_CACHE':
                    atlasLayer = new Atlas.Layer.ArcGISCache(mapPanel, layerJSon, parent);
                    break;
                case 'WMS':
                    atlasLayer = new Atlas.Layer.WMS(mapPanel, layerJSon, parent);
                    break;
                case 'WMTS':
                    atlasLayer = new Atlas.Layer.WMTS(mapPanel, layerJSon, parent);
                    break;
                case 'KML':
                    atlasLayer = new Atlas.Layer.KML(mapPanel, layerJSon, parent);
                    break;
                case 'GOOGLE':
                    atlasLayer = new Atlas.Layer.Google(mapPanel, layerJSon, parent);
                    break;
                case 'BING':
                    atlasLayer = new Atlas.Layer.Bing(mapPanel, layerJSon, parent);
                    break;
                case 'XYZ':
                    atlasLayer = new Atlas.Layer.XYZ(mapPanel, layerJSon, parent);
                    break;
                case 'SERVICE':
                case 'GROUP':
                    atlasLayer = new Atlas.Layer.Group(mapPanel, layerJSon, parent);
                    break;
                default:
                    alert('Layer type '+layerJSon['layerType']+' for layer '+layerJSon['layerId']+', is not implemented.');
            }
        }

        return atlasLayer;
    }
};
