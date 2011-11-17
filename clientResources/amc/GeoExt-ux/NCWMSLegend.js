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

Ext.namespace("GeoExt.ux");

GeoExt.ux.NCWMSLegend = Ext.extend(GeoExt.ux.WMSLegend, {

	/**
	 * Construct a WMS legend, without styles attributes and with
	 * PALETTE attribute when needed.
	 *
	 * NOTE: The PALETTE attribute is available in the legend URL of
	 *     the style definition of the capabilities document, but the
	 *     GeoTool WMS Java library (server side) do not currently have
	 *     the ability to extract it.
	 *     This method extract the palette name from the style name,
	 *     assuming that the style name is composed like this:
	 *         <dataset name>/<more names?>/<palette name>
	 *     If no slash ('/') is found, the style name is used as the
	 *     palette name.
	 *     If the style name do not follow the mentioned structure, the
	 *     palette name may not be extract correctly, implying that the
	 *     request will contains an erroneous palette parameter and the
	 *     ncWMS server will return an error (the client will not
	 *     display any legend graphics).
	 */
	getLegendUrl: function(layerName, layerNames) {
		var rec = this.layerRecord;
		var layer = rec.getLayer();
		layerNames = layerNames || [layer.params.LAYERS].join(",").split(",");
		var styleNames = layer.params.STYLES &&
							 [layer.params.STYLES].join(",").split(",");
		var idx = layerNames.indexOf(layerName);
		var styleName = styleNames && styleNames[idx];

		var urlBaseParams = {
			REQUEST: "GetLegendGraphic",
			WIDTH: null,
			HEIGHT: null,
			EXCEPTIONS: "application/vnd.ogc.se_xml",
			LAYER: layerName,
			LAYERS: null,
			SRS: null,
			FORMAT: null
		};

		if (styleName) {
			var lastSlashIdx = styleName.lastIndexOf('/');
			if (lastSlashIdx > -1) {
				var palette = styleName.substring(lastSlashIdx+1, styleName.length);
				urlBaseParams.PALETTE = palette;
			} else {
				urlBaseParams.PALETTE = styleName;
			}
		}

		return this._getLegendUrl(layerName, layerNames, styleName, styleNames, urlBaseParams);
	}
});


/**
 * private: method[supports]
 * Private override
 */
GeoExt.ux.NCWMSLegend.supports = function(layerRecord) {
	return layerRecord.getLayer() instanceof OpenLayers.Layer.ux.NCWMS;
};

/** api: legendtype = gx_ux_wmslegend */
GeoExt.LayerLegend.types["gx_ux_ncwmslegend"] = GeoExt.ux.NCWMSLegend;

/** api: xtype = gx_ux_wmslegend */
Ext.reg('gx_ux_ncwmslegend', GeoExt.ux.NCWMSLegend);
