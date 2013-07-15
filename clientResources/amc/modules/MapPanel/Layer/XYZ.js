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

Atlas.Layer.XYZ = OpenLayers.Class(Atlas.Layer.AbstractLayer, {
	/**
	 * Constructor: Atlas.Layer.XYZ
	 *
	 * Parameters:
	 * jsonLayer - {Object} Hashtable of layer attributes
	 * mapPanel - {Object} Instance of the MapPanel in which the layer is used
	 */
	initialize: function(mapPanel, jsonLayer, parent) {
		Atlas.Layer.AbstractLayer.prototype.initialize.apply(this, arguments);

		var layer = {};
		if (this.json) {
			var title = this.getTitle();

			var serviceUrls = [];
			for (var i=0; i<this.json['serviceUrls'].length; i++) {
				serviceUrls[i] = this.parseServiceUrl(this.json['serviceUrls'][i]);
			}

			var crossOriginKeyword = null;
			if (typeof(this.json['crossOriginKeyword']) !== 'undefined') {
				crossOriginKeyword = this.json['crossOriginKeyword'];
			}

			var isBaseLayer = !!this.json['isBaseLayer'];

			var olOptions = OpenLayers.Util.extend({
					"isBaseLayer": isBaseLayer,
					"tileOptions": {
						"crossOriginKeyword": crossOriginKeyword
					}
				},
				this.json['olOptions']
			);

			// OpenLayers.Util.extend wont extend the tileOptions if this.json['olOptions'] already contains a tileOptions
			if (typeof(this.json['olOptions']) !== 'undefined' && typeof(this.json['olOptions']['tileOptions']) !== 'undefined') {
				olOptions['tileOptions'] = OpenLayers.Util.extend({
					"crossOriginKeyword": crossOriginKeyword
				}, this.json['olOptions']['tileOptions']);
			}

			if (typeof(this.json['osm']) === 'boolean' && this.json['osm'] === true) {
				layer = new OpenLayers.Layer.OSM(title, serviceUrls, olOptions);
			} else {
				layer = new OpenLayers.Layer.XYZ(title, serviceUrls, olOptions);
			}
		}

		this.setLayer(layer);
	},

	parseServiceUrl: function(serviceUrl) {
		return serviceUrl.replace("{layerName}", this.json['layerName'])
				.replace("{format}", this.json['format']);
	}
});
