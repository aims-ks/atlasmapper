Ext.namespace("GeoExt.ux");

// widgets/WMSLegend.js
GeoExt.ux.WMSLegend = Ext.extend(GeoExt.WMSLegend, {
	defaultStyleIsFirst: false,
	defaultStyle: null,

	initComponent: function() {
		GeoExt.ux.WMSLegend.superclass.initComponent.call(this);

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
		// Something somewhere call setText on this button...
		deleteButon.setText = function(val) {};
		this.items.insert(0, deleteButon);
	},


	getLegendUrl: function(layerName, layerNames) {
		var rec = this.layerRecord;
		var layer = rec.getLayer();
		layerNames = layerNames || [layer.params.LAYERS].join(",").split(",");
		var styleNames = layer.params.STYLES &&
							 [layer.params.STYLES].join(",").split(",");
		var idx = layerNames.indexOf(layerName);
		var styleName = styleNames && styleNames[idx];

		url = this._getLegendUrl(layerName, layerNames, styleName, styleNames, {
			REQUEST: "GetLegendGraphic",
			WIDTH: null,
			HEIGHT: null,
			EXCEPTIONS: "application/vnd.ogc.se_xml",
			LAYER: layerName,
			LAYERS: null,
			STYLE: (styleName !== '') ? styleName: null,
			STYLES: null,
			SRS: null,
			FORMAT: null
		});
		return url;
	},


	/**
	 * getLegendUrl using pre-processed parameter, to make it more generic.
	 */
	_getLegendUrl: function(layerName, layerNames, styleName, styleNames, legendBaseParams) {
		this._setAtlasMapperLegendUrlInLayerStyles();

		var rec = this.layerRecord;
		var url;
		var layer = rec.getLayer();

		// Check if we have a legend URL for the current style
		// (in the record's "styles" data field)
		var styles = rec && rec.get("styles");
		if(styles && styles.length > 0) {
			if(styleName) {
				Ext.each(styles, function(s) {
					url = (s.name == styleName && s.legend) && s.legend.href;
					return !url;
				});
			} else if(!styleNames && !layer.params.SLD && !layer.params.SLD_BODY) {
				// Set URL to the default style
				// Possible scenarios:
				// 1. Default style is usually set in the instance attributes
				// 2. Default style may be the first one
				// 3. Look in all styles to find the default one
				// 4. Don't set the URL, let this method find an other way to set it
				if (this.defaultStyle != null) {
					url = this.defaultStyle.legend && this.defaultStyle.legend.href;
				} else if (this.defaultStyleIsFirst === true) {
					url = styles[0].legend && styles[0].legend.href;
				} else {
					for (var i=0,len=styles.length; i<len; i++) {
						var s = styles[i];
						if (s._default === true) {
							url = styles[i].legend && styles[i].legend.href;
						}
					}
				}
			}
		}

		// Check if we have a global legend URL for this layer
		if(!url) {
			if (layer.json['legendUrl']) {
				url = layer.json['legendUrl'];
				if (layer.json['legendFilename']) {
					// Hardcoded URL (in config)
					url += layer.json['legendFilename'];
					return url;
				}
				// Add parameters
				url = layer.getFullRequestString(
					legendBaseParams,
					url
				);
			}
		}

		if(!url) {
			url = layer.getFullRequestString(
				legendBaseParams,
				// Never get the legend from GWC
				layer.json['wmsServiceUrl']
			);
		}

		// add scale parameter - also if we have the url from the record's
		// styles data field and it is actually a GetLegendGraphic request.
		if(this.useScaleParameter === true &&
				url.toLowerCase().indexOf("request=getlegendgraphic") != -1) {
			var scale = layer.map.getScale();
			url = Ext.urlAppend(url, "SCALE=" + scale);
		}
		var params = this.baseParams || {};
		Ext.applyIf(params, {FORMAT: 'image/gif'});
		if(url.indexOf('?') > 0) {
			url = Ext.urlEncode(params, url);
		}

		return url;
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

		return GeoExt.ux.WMSLegend.superclass.getLayerTitle.apply(this, arguments);
	},

	/**
	 * Create a custom legend URL for the current layer, if needed,
	 * and set it in the style of layer record.
	 */
	_setAtlasMapperLegendUrlInLayerStyles: function() {
		var layer = this.layerRecord.getLayer();
		var jsonLayer = layer.json;
		var layerName = layer.name;

		// Query a different layer.
		// Used to display the legend of a different layer.
		if (jsonLayer['legendLayerId']) {
			layerName = jsonLayer['legendLayerId'];
		}

		// Override of the LegendGraphics in the layer config, and let
		// GeoExt deal with it.
		// "legendUrl" & "legendFilename"
		if (jsonLayer['wmsStyles']) {
			var styles = [];
			Ext.iterate(jsonLayer['wmsStyles'], function(styleName, styleJsonObj) {
				var styleObj = {};
				if (styleName) {
					styleObj.name = styleName;
				}

				if (styleJsonObj['default'] === true) {
					styleObj._default = true;
					// Set the default style as an attribute to avoid having to search for it.
					this.defaultStyle = styleObj;
				}

				if(styleJsonObj['legendUrl']) {
					var url = styleJsonObj['legendUrl'];
					if (styleJsonObj['legendFilename']) {
						url += styleJsonObj['legendFilename'];
					}

					// Query a different URL.
					// Used to display the legend of a different URL.
					// TODO affect each styles individually
					styleObj.legend = {
						href: url
					};
				}
				styles.push(styleObj);
			}, this);
			if (styles.length > 0) {
				this.layerRecord.set("styles", styles);
			}
		}
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


	/**
	 * Override GeoExt.WMSLegend.update
	 * To consider the delete button
	 *
	 * private: method[update]
	 *  Update the legend, adding, removing or updating
	 *  the per-sublayer box component.
	 */
	update: function() {
		var layer = this.layerRecord.getLayer();
		// In some cases, this update function is called on a layer
		// that has just been removed, see ticket #238.
		// The following check bypass the update if map is not set.
		if(!(layer && layer.map)) {
			return;
		}
		GeoExt.WMSLegend.superclass.update.apply(this, arguments);
		
		var layerNames, layerName, i, len;

		layerNames = [layer.params.LAYERS].join(",").split(",");

		var destroyList = [];
		var deleteButtonCmp = this.items.get(0);
		var textCmp = this.items.get(1);
		this.items.each(function(cmp) {
			if (cmp != deleteButtonCmp) {
				i = layerNames.indexOf(cmp.itemId);
				if(i < 0 && cmp != textCmp) {
					destroyList.push(cmp);
				} else if(cmp !== textCmp){
					layerName = layerNames[i];
					var newUrl = this.getLegendUrl(layerName, layerNames);
					if(!OpenLayers.Util.isEquivalentUrl(newUrl, cmp.url)) {
						cmp.setUrl(newUrl);
					}
				}
			}
		}, this);
		for(i = 0, len = destroyList.length; i<len; i++) {
			var cmp = destroyList[i];
			// cmp.destroy() does not remove the cmp from
			// its parent container!
			this.remove(cmp);
			cmp.destroy();
		}

		for(i = 0, len = layerNames.length; i<len; i++) {
			layerName = layerNames[i];
			if(!this.items || !this.getComponent(layerName)) {
				this.add({
					xtype: "gx_legendimage",
					url: this.getLegendUrl(layerName, layerNames),
					itemId: layerName
				});
			}
		}
		this.doLayout();
	}
});

/**
 * private: method[supports]
 * Private override
 */
GeoExt.ux.WMSLegend.supports = function(layerRecord) {
	return layerRecord.getLayer() instanceof OpenLayers.Layer.WMS;
};

/** api: legendtype = gx_ux_wmslegend */
GeoExt.LayerLegend.types["gx_ux_wmslegend"] = GeoExt.ux.WMSLegend;

/** api: xtype = gx_ux_wmslegend */
Ext.reg('gx_ux_wmslegend', GeoExt.ux.WMSLegend);
