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

Atlas.Layer.KML = OpenLayers.Class(Atlas.Layer.AbstractLayer, {
	KML_ALLOW_JAVASCRIPT: false,
	supportLoadEvents: false,

	/**
	 * Constructor: Atlas.Layer.KML
	 *
	 * Parameters:
	 * jsonLayer - {Object} Hashtable of layer attributes
	 * mapPanel - {Object} Instance of the MapPanel in which the layer is used
	 */
	initialize: function(mapPanel, jsonLayer, parent) {
		Atlas.Layer.AbstractLayer.prototype.initialize.apply(this, arguments);

		if (this.json != null) {
			var kmlUrl = this.json['kmlUrl'];

			var layerOptions = null;
			if (typeof(this.json['olOptions']) !== 'undefined') {
				layerOptions = this.json['olOptions'];
			}

			var kml = new OpenLayers.Layer.ux.KML(
				this.getTitle(),
				kmlUrl,
				layerOptions
			);

			this.setLayer(kml);

			if (mapPanel) {
				var that = this;
				mapPanel.ol_on("dpiChange", function(evt) {
					that.layer.redraw();
				});
			}

			kml.events.on({
				loadend: function() {
					this.monitorFeatureRequest(kml);
				},
				scope: this
			});
		}
	},

	// Need to remove it at some point
	monitorFeatureRequest: function(layer) {
		var multiSelectDragFeature = OpenLayers.Control.ux.MultiSelectDragFeature.getInstance(this.mapPanel.map);
		layer.events.on({
			"scope": this,
			"featureselected": function(event) {
				this.onFeatureSelect(event, multiSelectDragFeature);
			},
			"featureunselected": function(event) {
				this.onFeatureUnselect(event);
			}
		});

		multiSelectDragFeature.addLayer(layer);
	},

	/*
	// Adding the documentName to the layer title is un-intuitive and the admin do not have any control over it. Better let it as it is.
	// Override
	getTitle: function() {
		var title = Atlas.Layer.AbstractLayer.prototype.getTitle.apply(this, arguments);
		if (this.json['documentName']) {
			return this.json['documentName'] + ' (' + title + ')';
		}
		return title;
	},*/

	// KML are vector; they can always be located, but their extent can only be calculated once the layer is loaded.
	canBeLocated: function() {
		return true;
	},

	// The extent can not be cached; it can return incomplete info if it's request before the layer is completely loaded.
	getExtent: function() {
		return this.computeExtent();
	},

	onFeatureSelect: function(event, select) {
		var feature = event.feature;

		var content = '';
		if (feature.attributes.name) {
			content += '<h2>' + feature.attributes.name + '</h2>';
		}
		var description = '';
		if (typeof(feature.attributes.description) !== 'undefined') {
			description += feature.attributes.description;
		}

		// Javascript in KML can be unsafe.
		if (!this.KML_ALLOW_JAVASCRIPT) {
			if (description.search("<script") != -1) {
				description = "Content contained Javascript! Escaped content below.<br />" + description.replace(/</g, "&lt;").replace(/>/g, "&gt;");
			}
		}

		content += description;

		if (content) {
			var that = this;
			var popupId = 'kml-popup';
			var popup = new OpenLayers.Popup.FramedCloud(
				popupId,
				feature.geometry.getBounds().getCenterLonLat(),
				new OpenLayers.Size(100, 100), // Initial content size
				content,
				null, true,
				function(event) {that.onPopupClose(event, select);}
			);
			feature.popup = popup;
			this.mapPanel.map.addPopup(popup);
		}
	},

	onFeatureUnselect: function(event) {
		var feature = event.feature;
		if(feature.popup) {
			this.mapPanel.map.removePopup(feature.popup);
			feature.popup.destroy();
			delete feature.popup;
		}
	},

	onPopupClose: function(evt, select) {
		select.unselectAll();
		// Stops an event from propagating.
		// Otherwise, the close button may trigger a feature request.
		OpenLayers.Event.stop(evt);
	}
});
