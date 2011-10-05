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
						layer.setHideInLegend(true);
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
		if (layer && layer.json && layer.json['legendTitle']) {
			return layer.json['legendTitle'];
		}

		return GeoExt.ux.VectorLegend.superclass.getLayerTitle.apply(this, arguments);
	},

	/**
	 * Override GeoExt.LayerLegend.onStoreUpdate
	 * Manage 'Show in legend' checkbox event.
	 *
	 * Add the method layer.getHideInLegend() in the setVisibility call.
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
				!record.get('hideInLegend') && !layer.getHideInLegend());
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
		if (layer && layer.json) {
			var layerId = layer.json['layerId'];
			var legendUrl = this.getLegendUrl(layer.json);
			if (legendUrl) {
				hasLegendImage = true;
				if(!this.items || !this.getComponent(layerId)) {
					this.add({
						xtype: "gx_legendimage",
						url: legendUrl,
						itemId: layerId
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
