Ext.namespace('GeoExt.ux');
/**
 * GeoExt.ux.LegendPanel is a collection of GeoExt.ux.LegendGroup,
 * which are Ext.Panels that collect the legend graphics
 * (see GeoExt.ux.LegendPanel.addLegend, GeoExt.ux.LegendGroup.insert)
 * that usually goes to a GeoExt.LegendPanel
 * (see GeoExt.LegendPanel.addLegend).
 * 
 * GeoExt.ux.LegendPanel.addLegend ack as a dispatcher to place the
 * legend graphics into the corresponding GeoExt.ux.LegendGroup.
 * 
 * GeoExt.ux.LegendPanel config options are almost the same as
 * GeoExt.LegendPanel.
 */
GeoExt.ux.LegendPanel = Ext.extend(GeoExt.LegendPanel, {
	// Hashmap of legend groups.
	// Key: legendGroupName (String)
	// Value: GeoExt.ux.LegendGroup (extend Ext.Panel)
	legendGroups: null,
	legendWindow: null,

	bodyCssClass: 'legend',
	preferredTypes: ['gx_ux_ncwmslegend', 'gx_ux_wmslegend', 'gx_ux_vectorlegend'], // TODO Check layer type somehow...

	autoHeight: true,
	autoScroll: true,

	filter: function(record) {
		var layerJSon = record.getLayer().json;
		return layerJSon && layerJSon['hasLegend'];
	},

	mapPanel: null,

	// PATCH to make the scroll works in the legend
	//allowDD: true,

	initComponent: function() {
		var that = this;
		this.draggable = {

			// PATCH to make the scroll works in the legend
			// * WORKS: b4StartDrag, onBeforeDrag
			// * NOT EARLY ENOUGH: b4Drag, b4MouseDown
			// * IGNORED: beforeDragOut, beforeDragDrop, beforeDragEnter, beforeDragOver
			//onBeforeDrag: function(data, event) {
			//	that.allowDD = true;
			//	return true;
			//},
			// PATCH to make the scroll works in the legend
			//b4StartDrag: function(x, y) {
			//	if (that.allowDD) {
			//		this.proxy.show();
			//	}
			//	return that.allowDD;
			//},

			onDrag: function(e) {
				var pel = this.proxy.getEl();
				this.x = pel.getLeft(true);
				this.y = pel.getTop(true);

				var s = this.panel.getEl().shadow;
				if (s) {
					s.realign(this.x, this.y, pel.getWidth(), pel.getHeight());
				}
			},

			endDrag: function(e) {
				that.legendWindow.setPosition(this.x, this.y);
			}
		};

		if (this.mapPanel && this.mapPanel.layers) {
			this.layerStore = this.mapPanel.layers;
		}

		GeoExt.ux.LegendPanel.superclass.initComponent.call(this);
		this.legendGroups = {};

		// PATCH to make the scroll works in the legend
		//var that = this;
		//window.setTimeout(function() {
		//	that.getEl().first().first().on('scroll', function() {
		//		that.allowDD = false;
		//	});
		//}, 1);
	},

	/**
	 * Go out of autoheight/autowidth as soon as the user manually resize the legend window.
	 * See also Atlas.Legend.onResize
	 */
	onResize: function(adjWidth, adjHeight, rawWidth, rawHeight) {
		var w = adjWidth,
			h = adjHeight;

		if (h == 'auto' && Ext.isDefined(rawHeight) && this.legendWindow && !this.legendWindow.autoHeight) {
			this.autoHeight = false;
			h = rawHeight;
		}
		if (w == 'auto' && Ext.isDefined(rawWidth) && this.legendWindow && !this.legendWindow.autoWidth) {
			this.autoWidth = false;
			w = rawWidth;
		}

		GeoExt.ux.LegendPanel.superclass.onResize.call(this, w, h, rawWidth, rawHeight);
	},

	addLegend: function(record, index) {
		if (this.filter(record) === true) {
			var layer = record.getLayer();
			// Always insert new legend on top of its group
			// TODO maybe figure out how index is generated and do something with it (like legend order = layer list order)
			//index = index || 0;
			index = 0;
			var legend;
			var types = GeoExt.LayerLegend.getTypes(record,
					this.preferredTypes);

			if(layer.displayInLayerSwitcher && !record.get('hideInLegend') &&
					types.length > 0) {

				var legendGroup = this.getLegendGroup(record);

				// Configuration of the GeoExt.ux.WMSLegend (same as GeoExt.WMSLegend) and GeoExt.ux.VectorLegend (same as GeoExt.VectorLegend)
				var legendConfig = {
					xtype: types[0],
					id: this.getIdForLayer(layer),
					layerRecord: record,
					baseParams: {},
					hidden: !((!layer.map && layer.visibility) ||
						(layer.getVisibility() && layer.calculateInRange()))
				};

				Ext.applyIf(legendConfig.baseParams, layer.json['legendParameters']);

				legendGroup.insert(index, legendConfig);
			}
		}
	},

	removeLegend: function(record) {
		var legendGroup = this.getLegendGroup(record);
		if (legendGroup && legendGroup.items) {
			var legend = legendGroup.getComponent(this.getIdForLayer(record.getLayer()));
			if (legend) {
				legendGroup.remove(legend, true);
				legendGroup.doLayout();
			}
		}
	},

	getLegendGroup: function(record) {
		var layer = record.getLayer();
		var layerJSon = layer.json;
		if (!layerJSon) {
			return null;
		}

		var legendGroupName = layerJSon['legendGroup'] || '';
		// legendGroupName false, null, undefined, empty string, etc. should all goes to the same Legend Group.
		if (!legendGroupName) {
			legendGroupName = '';
		}

		if (!this.legendGroups[legendGroupName]) {
			// Configuration of GeoExt.ux.LegendGroup items
			var that = this;
			var params = {
				defaults: {
					cls: 'legend-item',
					labelCls: 'legend-item-header',
					ctCls: 'legend-group'
				},
				xtype: 'gx_ux_legendgroup',
				groupName: legendGroupName,
				headerCls: 'legend-group-header',
				hidden: false,
				listeners: {
					hide: function(comp){ that.showHidePanel(); },
					show: function(comp){ that.showHidePanel(); }
				}
			}
			// Add GeoExt.ux.LegendPanel.defaults to the GeoExt.ux.LegendGroup.defaults
			// (GeoExt.ux.LegendGroup.defaults params are the same as GeoExt.LegendPanel.defaults)
			Ext.applyIf(params.defaults, this.defaults);

			this.legendGroups[legendGroupName] = this.insert(0, params);
		}
		return this.legendGroups[legendGroupName];
	},

	doLayout: function() {
		GeoExt.ux.LegendPanel.superclass.doLayout.call(this);
		this.showHidePanel();
	},

	showHidePanel: function() {
		// All groups are considerate as hidden when the panel is hidden.
		var nbGroupVisible = 0;
		Ext.iterate(this.legendGroups, function(groupName, group) {
			if (!group.hidden) {
				nbGroupVisible++;
			}
		});

		if (nbGroupVisible > 0) {
			if (!this.isVisible()) {
				this.legendWindow.show();
				this.show();
			}
		} else {
			if (this.isVisible()) {
				this.legendWindow.hide();
				this.hide();
			}
		}
	}
});
/** api: xtype = gx_ux_legendpanel */
Ext.reg('gx_ux_legendpanel', GeoExt.ux.LegendPanel);
