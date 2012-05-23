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

// TODO Make a method similar to setHideInLegend, to hide the legend without touching the "Hide in legend" checkbox.
// widgets/LegendImage.js
GeoExt.ux.LegendImage = Ext.extend(GeoExt.LegendImage, {

	layer: null,

	// Override
	setUrl: function(url) {
		//this.layer.setLegendError(false);
		GeoExt.ux.LegendImage.superclass.setUrl.call(this, url);
	},

	// Override
	onImageLoadError: function() {
		if (this.layer != null) {
			//this.layer.setLegendError(true);
			//this.layer.setHideInLegend(true);
		}
		this.getEl().dom.src = this.defaultImgSrc;
	}
});

/** api: xtype = gx_ux_legendimage */
Ext.reg('gx_ux_legendimage', GeoExt.ux.LegendImage);
