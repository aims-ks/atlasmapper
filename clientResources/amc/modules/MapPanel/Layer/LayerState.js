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

Atlas.Layer.LayerState = OpenLayers.Class({
	/**
	 * boolean, true to grey out the layer node; it can't be select/unselect
	 * (usually mean that a parent folder is disabling it)
	 */
	disabled: null,

	/**
	 * boolean, true to recreate the folder in open mode (only apply to folders).
	 */
	expanded: null,

	/**
	 * boolean, store the value of the check box of the node before it is moved
	 * (layer.getVisibility() is always false after unchecking its layer group
	 * because the layer has been hidden from the map)
	 */
	checked: null,

	/**
	 * boolean, visibility of the layer, before it get disabled.
	 */
	visible: null,

	initialize: function(config) {
		if (typeof(config) === 'object' && config != null) {
			// Copy the values of config to this instance.
			OpenLayers.Util.extend(this, config);
		}
	}
});
