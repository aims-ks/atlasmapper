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

Atlas.Layer.KML = OpenLayers.Class(Atlas.Layer.AbstractLayer, {
	KML_ALLOW_JAVASCRIPT: false,

	/**
	 * Constructor: Atlas.Layer.KML
	 *
	 * Parameters:
	 * jsonLayer - {Object} Hashtable of layer attributes
	 * mapPanel - {Object} Instance of the MapPanel in which the layer is used
	 */
	initialize: function(mapPanel, jsonLayer) {
		Atlas.Layer.AbstractLayer.prototype.initialize.apply(this, arguments);

		if (this.json != null) {
			var kmlUrl = this.json['kmlUrl'];

			// Set the OpenLayer options, used by the library.
			var layerOptions = {
				strategies: [new OpenLayers.Strategy.Fixed()],
				//visibility: layerJSon['<initialState>']['<activated>'],
				//opacity: layerJSon['<initialState>']['<opacity>'],
				protocol: new OpenLayers.Protocol.HTTP({
					url: kmlUrl,
					format: new OpenLayers.Format.KML({
						extractStyles: true,
						extractAttributes: true
					})
				})
			};

			if (typeof(this.json['olOptions']) !== 'undefined') {
				layerOptions = this.applyOlOverrides(layerOptions, this.json['olOptions']);
			}

			var kml = new OpenLayers.Layer.Vector(
				this.getTitle(),
				layerOptions
			);

			var select = new OpenLayers.Control.SelectFeature(kml);

			// OpenLayer events for KML layers
			var that = this;
			kml.events.on({
				"featureselected": function(event) {
					that.onFeatureSelect(event, select);
				},
				"featureunselected": function(event) {
					that.onFeatureUnselect(event);
				}
			});

			this.mapPanel.map.addControl(select);
			select.activate();

			this.layer = this.extendLayer(kml);
		}
	},


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
		// Since KML is user-generated, do naive protection against
		// Javascript.
		var content = "<h2>"+feature.attributes.name + "</h2>" + feature.attributes.description;
		// Javascript in KML can be unsafe.
		if (!this.KML_ALLOW_JAVASCRIPT) {
			if (content.search("<script") != -1) {
				content = "Content contained Javascript! Escaped content below.<br />" + content.replace(/</g, "&lt;");
			}
		}
		var that = this;
		var popupId = 'kml-popup';
		var popup = new OpenLayers.Popup.FramedCloud(
			popupId,
			feature.geometry.getBounds().getCenterLonLat(),
			new OpenLayers.Size(100,100), // Initial content size
			content,
			null, true,
			function(event) {that.onPopupClose(event, select);}
		);
		feature.popup = popup;
		this.mapPanel.map.addPopup(popup);
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
