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

Atlas.Layer.Google = OpenLayers.Class(Atlas.Layer.AbstractLayer, {
	supportLoadEvents: false,

	/**
	 * Constructor: Atlas.Layer.Google
	 *
	 * Parameters:
	 * jsonLayer - {Object} Hashtable of layer attributes
	 * mapPanel - {Object} Instance of the MapPanel in which the layer is used
	 */
	initialize: function(mapPanel, jsonLayer, parent) {
		Atlas.Layer.AbstractLayer.prototype.initialize.apply(this, arguments);

		if (this.json != null) {
			var layerOptions = {
				// google.maps.MapTypeId.TERRAIN, google.maps.MapTypeId.ROADMAP, google.maps.MapTypeId.HYBRID, google.maps.MapTypeId.SATELLITE
				type: google.maps.MapTypeId[this.json['layerName'] || this.json['layerId']]
			};

			if (typeof(this.json['olOptions']) !== 'undefined') {
				layerOptions = this.applyOlOverrides(layerOptions, this.json['olOptions']);
			}

			this.setLayer(new OpenLayers.Layer.Google(
				// "Google Physical", "Google Streets", "Google Hybrid", "Google Satellite"
				this.getTitle(),
				layerOptions
			));
		}
	}
});
