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

/**
 * Simple wrapper over OpenLayers.Control.SelectFeature, to facilitate usage.
 *
 * After initialising and activating your layer control, add the following lines
 * and the control will works with all other layers that also use this feature.
 * NOTE: This feature do not work when multiple controls are defined on the same layer.
 *
 * var multiSelectDragFeature = OpenLayers.Control.ux.MultiSelectDragFeature.getInstance(map);
 * multiSelectDragFeature.addLayer(layer);
 */

OpenLayers.Control.ux = OpenLayers.Control.ux || {};
OpenLayers.Control.ux.MultiSelectDragFeature = OpenLayers.Class(OpenLayers.Control.SelectFeature, {
	CLASS_NAME: "OpenLayers.Control.ux.MultiSelectDragFeature",

	initialize: function(layers, options) {
		OpenLayers.Control.SelectFeature.prototype.initialize.apply(this, arguments);
		this._registerListeners();
	},

	_registerListeners: function() {
		// 'activate' and 'deactivate' are the only events,
		// so I have to rely on those to add/remove my listeners.
		this.events.on({
			'activate': function(evt) {
				// Layer.setOpacity do not works when the layers are embedded in a container.
				// This event listener fix that problem by setting the opacity to the right element.
				if (this.map && this.map.events) {
					this.map.events.on({
						'changelayer': this._changeLayer,
						scope: this
					});
				}
			},
			'deactivate': function(evt) {
				this._unregisterListeners(this.map);
			},
			scope: this
		});
	},

	_unregisterListeners: function(map) {
		if (map) {
			map.events.un({
				'changelayer': this._changeLayer,
				scope: this
			});
		}
	},

	_changeLayer: function(evt) {
		// The modified property is "opacity"
		if (evt.layer && evt.property === 'opacity' &&
				// The affected layer is in the container
				OpenLayers.Util.indexOf(this.layers, evt.layer) != -1 &&
				// The layer renderer has a root
				evt.layer.renderer && evt.layer.renderer.root) {

			OpenLayers.Util.modifyDOMElement(evt.layer.renderer.root,
					null, null, null, null, null, null, evt.layer.opacity);
		}
	},

	addLayer: function(layer) {
		if (this.layers) {
			var isActive = this.active;
			this._deactivate();

			this.layers.push(layer);

			// Automatically remove the layer from the "container" when the layer is removed from the map.
			layer.events.on({
				'removed': function(evt) {
					this.removeLayer(layer);
				},
				scope: this
			});

			this._reactivate(isActive);
		} else {
			var initialLayer = this.layer;
			this.setLayer([]);

			// If it was configured with a layer, add that layer to
			// the array and set the event listeners and everything...
			if (initialLayer) {
				this.addLayer(initialLayer);
			}
			this.addLayer(layer);
		}
	},

	removeLayer: function(layer) {
		if (this.layers) {
			// Search the layer
			var index = OpenLayers.Util.indexOf(this.layers, layer);

			// Remove the layer if it has been found
			if (index != -1) {
				var isActive = this.active;
				this._deactivate();

				this.layers.splice(index, 1);

				this._reactivate(isActive);
			}
		}
	},

	_deactivate: function() {
		this.unselectAll();
		this.deactivate();
	},

	_reactivate: function(wasActive) {
		if (wasActive) {
			this.activate();
		}
	}
});

// Static methods
OpenLayers.Control.ux.MultiSelectDragFeature.getInstance = function(map) {
	if (map.multiSelectDragFeature == null) {
		map.multiSelectDragFeature = new OpenLayers.Control.ux.MultiSelectDragFeature([], {
			// Hover looks cools with search results but it's unusable with KML, and the map can only have ONE of those layer.
			hover: false
		});
		map.addControl(map.multiSelectDragFeature);
		map.multiSelectDragFeature.activate();
	}
	return map.multiSelectDragFeature;
};
