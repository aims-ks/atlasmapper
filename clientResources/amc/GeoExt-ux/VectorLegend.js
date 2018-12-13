/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2011 Australian Institute of Marine Science
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

Ext.namespace("GeoExt.ux");

// widgets/VectorLegend.js
GeoExt.ux.VectorLegend = Ext.extend(GeoExt.VectorLegend, {

	initComponent: function() {
		GeoExt.ux.VectorLegend.superclass.initComponent.call(this);

		var that = this;
		var deleteButon = new Ext.form.Field({
			cls: 'x-legend-delete',
			autoCreate: {
				tag: 'input',
				type: 'image',
				src: Ext.BLANK_IMAGE_URL,
				alt: 'Hide'
			},
			listeners: {
				render: function(evt) {
					deleteButon.getEl().on('click', function(evt) {
						var layer = that.layerRecord.getLayer();
						layer.atlasLayer.setHideInLegend(true);
					});
				}
			}
		});
		this.items.insert(0, deleteButon);
	},

	/**
	 * Override GeoExt.LayerLegend.getLayerTitle
	 * Manage custom legend title.
	 */
	getLayerTitle: function(record) {
		var layer = record.getLayer();
		if (layer && layer.atlasLayer && layer.atlasLayer.json && layer.atlasLayer.json['legendTitle']) {
			return layer.atlasLayer.json['legendTitle'];
		}

		return GeoExt.ux.VectorLegend.superclass.getLayerTitle.apply(this, arguments);
	},

	/**
	 * Override GeoExt.LayerLegend.onStoreUpdate
	 * Manage 'Show in legend' checkbox event.
	 *
	 * Add a call to the method layer.atlasLayer.getHideInLegend() in the setVisibility call.
	 * It would be nicer to set the hideInLegend attribute
	 * of the record, but it's too difficult to access this variable
	 * everywhere in the application.
	 * NOTE: layer => OpenLayers, record => GeoExt
	 */
	onStoreUpdate: function(store, record, operation) {
		// if we don't have items, we are already awaiting garbage
		// collection after being removed by LegendPanel::removeLegend, and
		// updating will cause errors
		if (record === this.layerRecord && this.items.getCount() > 0) {
			var layer = record.getLayer();
			this.setVisible(layer.getVisibility() &&
				layer.calculateInRange() && layer.displayInLayerSwitcher &&
				!record.get('hideInLegend') && !layer.atlasLayer.getHideInLegend());
			this.update();
		}
	},

	getLegendUrl: function(jsonLayer) {
		var fullUrl = null;
		if (jsonLayer['legendUrl']) {
			fullUrl = jsonLayer['legendUrl'];
			if (jsonLayer['legendFilename']) {
				fullUrl += jsonLayer['legendFilename'];
			}
		}

		return fullUrl;
	},

	/** api: method[update]
	 *  Update rule titles and symbolizers.
	 */
	update: function() {
		// Check if an legend image has been specified for the vector/KML layer.
		// If not, use the default legend generated be GeoExt.
		var hasLegendImage = false;

		var layer = this.layerRecord.getLayer();
		if (layer && layer.atlasLayer && layer.atlasLayer.json) {
			var layerId = layer.atlasLayer.json['layerId'];
			var legendUrl = this.getLegendUrl(layer.atlasLayer.json);
			if (legendUrl) {
				hasLegendImage = true;
				if(!this.items || !this.getComponent(layerId)) {
					this.add({
						xtype: "gx_ux_legendimage",
						url: legendUrl,
						itemId: layerId,
						layer: layer
					});
				}
			}
		}

		if (!hasLegendImage) {
			GeoExt.ux.VectorLegend.superclass.update.apply(this, arguments);
		}
	}
});

/**
 * private: method[supports]
 * Private override
 */
GeoExt.ux.VectorLegend.supports = function(layerRecord) {
	return layerRecord.getLayer() instanceof OpenLayers.Layer.Vector;
};

/** api: legendtype = gx_ux_vectorlegend */
GeoExt.LayerLegend.types["gx_ux_vectorlegend"] = GeoExt.ux.VectorLegend;

/** api: xtype = gx_ux_vectorlegend */
Ext.reg('gx_ux_vectorlegend', GeoExt.ux.VectorLegend);
