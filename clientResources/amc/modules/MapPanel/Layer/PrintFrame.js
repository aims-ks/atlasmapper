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

Atlas.Layer.PrintFrame = OpenLayers.Class(Atlas.Layer.AbstractLayer, {
	/**
	 * Constructor: Atlas.Layer.XYZ
	 *
	 * Parameters:
	 * jsonLayer - {Object} Hashtable of layer attributes
	 * mapPanel - {Object} Instance of the MapPanel in which the layer is used
	 */
	initialize: function(mapPanel, jsonLayer, parent) {
		Atlas.Layer.AbstractLayer.prototype.initialize.apply(this, arguments);

		var that = this;
		this.setLayer(new OpenLayers.Layer.ux.PrintFrame(this.getTitle(), {
			dpi: (jsonLayer.dpi? jsonLayer.dpi : mapPanel.dpi),
			attributions: function() {
				var attributions = Atlas.conf['attributions'] ? Atlas.conf['attributions'] : '';
				var layers = that.mapPanel.map.layers;
				for (var i=0; i<layers.length; i++) {
					if (layers[i] &&
							layers[i].visibility &&
							layers[i].opacity > 0 &&
							layers[i].atlasLayer) {

						var layerAttributions = layers[i].atlasLayer.getAttributions();
						if (typeof(layerAttributions) === 'string') {
							// Verify if the layer is in scope.
							// NOTE: If a layer bounds is null, it's considered as beein in (in doubt, add attributions)
							var printFrameBounds = that.layer.getPrintedExtent();
							var layerBounds = layers[i].atlasLayer.getExtent();
							if (printFrameBounds == null || layerBounds == null ||
									printFrameBounds.intersectsBounds(layerBounds)) {
								if (attributions) {
									attributions += ', ';
								}
								attributions += layerAttributions;
							}
						}
					}
				}
				return attributions;
			}
		}));

		if (mapPanel) {
			var that = this;
			mapPanel.ol_on("dpiChange", function(evt) {
				that.dpiChange(evt.dpi);
			});
		}
	},

	// Called when the DPI change on the mapPanel
	dpiChange: function(dpi) {
		this.layer.setDPI(dpi);
	},

	// Override
	getExtent: function() {
		// The layer may move or get resized. the extent can not be cached.
		return this.layer.getDataExtent();
	},

	// Override
	canBeLocated: function() {
		// The extent is null during the drawing process, but the locate button should not be disabled.
		return true;
	},

	// Override
	setOptions: function(optionsPanel) {
		var dpiOptions = [
			[90, '90'],
			[180, '180'],
			[360, '360']
		];

		var currentValue = this.mapPanel.dpi;

		if (dpiOptions.length > 1) {
			var dpiSelectConfig = {
				xtype: "combo",
				fieldLabel: "Map DPI",
				value: currentValue,
				typeAhead: false,
				editable: true,
				triggerAction: 'all',
				lazyRender: true,
				mode: 'local',
				store: new Ext.data.ArrayStore({
					id: 0,
					fields: [
						'name',
						'title'
					],
					data: dpiOptions
				}),
				valueField: 'name',
				displayField: 'title',
				allowBlank: false,
				listeners: {
					select: this.onDPIChange,
					change: this.onDPIChange, // Fired when manually edited
					scope: this
				}
			};

			// IE is awful with width calculation. Better give it a safe value.
			if (Ext.isIE && (!Ext.ieVersion || Ext.ieVersion < 8)) {
				dpiSelectConfig.width = 115;
			}

			optionsPanel.addOption(this, dpiSelectConfig);
		}
	},

	onDPIChange: function(field) {
		if (field) {
			this.mapPanel.changeDpi(parseInt(field.getValue()));
		}
	}
});
