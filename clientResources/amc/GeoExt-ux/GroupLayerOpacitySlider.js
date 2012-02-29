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

Ext.namespace("GeoExt.ux");

GeoExt.ux.GroupLayerOpacitySlider = Ext.extend(GeoExt.LayerOpacitySlider, {
	node: null,

    constructor: function(config) {
		if (config.node && config.node.layer) {
			config.layer = config.node.layer
		}

		GeoExt.ux.GroupLayerOpacitySlider.superclass.constructor.call(this, config);
    },

	/**
	 * GroupLayerOpacitySlider need a reference to the node to propagate
	 * opacity changes to the rest of the branch.
	 */
	setNode: function(node) {
		this.node = node;
		this.setLayer(node ? node.layer : null);
	},

	changeLayerOpacity: function(slider, value) {
		if (this.layer) {
			value = value / (this.maxValue - this.minValue);
			if (this.inverse === true) {
				value = 1 - value;
			}

			var valueBefore = (this.layer.opacity !== null ? this.layer.opacity : 1);
			var valueAfter = value * this._getParentOpacity();

			// Correction due to real value imprecision.
			if (valueAfter > 1) { valueAfter = 1; }
			if (valueAfter < 0) { valueAfter = 0; }

			if (valueAfter == 0 && this.node.hasChildNodes()) {
				// 0 value works, but children layers loose their opacity information when the parent opacity is set to 0.
				// A value to high (>0.1) will show artifact on the map, a value too low (extremely close to 0) will introduce floating point error on deep tree.
				// 0.000001 works great.
				valueAfter = 0.000001;
			}

			this._changeChildrenLayerOpacity(valueBefore, valueAfter, this.node);

			this._settingOpacity = true;
			this.layer.setOpacity(valueAfter);
			delete this._settingOpacity;
		}
	},

	/**
	 * Recursively change the value of evey children of the node.
	 * valueBefore and valueAfter are used to calculate the modification
	 * ratio in the opacity.
	 */
	_changeChildrenLayerOpacity: function(valueBefore, valueAfter, node) {
		if (node && node.hasChildNodes()) {
			node.eachChild(function(child) {
				if (child.layer) {
					var childValueBefore = (child.layer.opacity !== null ? child.layer.opacity : 1);
					var childValueAfter = (valueBefore > 0 ? (valueAfter * childValueBefore) / valueBefore : 1);

					// Correction due to real value imprecision.
					if (childValueAfter > 1) { childValueAfter = 1; }
					if (childValueAfter < 0) { childValueAfter = 0; }

					this._changeChildrenLayerOpacity(childValueBefore, childValueAfter, child);

					child.layer.setOpacity(childValueAfter);
				}
			}, this);
		}
	},

	getOpacityValue: function(layer) {
		var value;
		if (layer && layer.opacity !== null) {
			// NOTE: Don't do a parseInt here, a precise value is needed to find the exact opacity of the current layer.
			value = layer.opacity * (this.maxValue - this.minValue);
		} else {
			value = this.maxValue;
		}
		if (this.inverse === true) {
			value = this.maxValue - this.minValue - value;
		}

		return Math.round(value / this._getParentOpacity());
	},

	/**
	 * Return the opacity of the parent node. Return 1 (100%) if
	 * the opacity is not set.
	 */
	_getParentOpacity: function() {
		var opacity = 1;
		if (this.node && this.node.parentNode && this.node.parentNode.layer && this.node.parentNode.layer.opacity != null) {
			opacity = this.node.parentNode.layer.opacity;
		}
		return opacity;
	}
});
