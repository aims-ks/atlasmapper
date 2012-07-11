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

Atlas.Layer.Group = OpenLayers.Class(Atlas.Layer.AbstractLayer, {
	/**
	 * Constructor: Atlas.Layer.Group
	 *
	 * Parameters:
	 * jsonLayer - {Object} Hashtable of layer attributes
	 * mapPanel - {Object} Instance of the MapPanel in which the layer is used
	 */
	initialize: function(mapPanel, jsonLayer) {
		Atlas.Layer.AbstractLayer.prototype.initialize.apply(this, arguments);

		if (this.json != null) {
			if (this.json['layers'] && this.json['layers'].length > 0) {
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
					this.mapPanel.addLayersById(this.json['layers'], path);
				}
			}
		}


		// TODO!!
		this.layer = this.extendLayer({});
	},

	// TODO Check every children and listen on them
	isLoading: function() {
		return false;
	}
});
