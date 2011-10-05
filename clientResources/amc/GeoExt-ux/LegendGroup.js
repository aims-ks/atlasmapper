Ext.namespace('GeoExt.ux');

GeoExt.ux.LegendGroup = Ext.extend(Ext.Panel, {
	indexOffset: 0,
	nbVisibleItems: 0,

	initComponent: function() {
		GeoExt.ux.LegendGroup.superclass.initComponent.call(this);
		if (this.groupName) {
			this.add({
				xtype: "label",
				text: this.groupName,
				cls: (this.headerCls ? ' ' + this.headerCls : '')
			});
			this.indexOffset++;
		}
	},

	/**
	 * Override Ext.Container.doLayout 
	 */
	doLayout: function(shallow, force) {
		this.hideGroupIfEmpty();
		GeoExt.ux.LegendGroup.superclass.doLayout.call(this, shallow, force);
	},

	hideGroupIfEmpty: function() {
		this.nbVisibleItems = this.items.filter('hidden', 'false').getCount() - this.indexOffset;
		if (this.nbVisibleItems) {
			if (this.hidden) this.show();
		} else {
			if (!this.hidden) this.hide();
		}
	},

	insert: function(index, comp) {
		c = GeoExt.ux.LegendGroup.superclass.insert.call(
				this, index + this.indexOffset, comp);

		// Used to call event without loosing the "this" reference.
		var that = this;

		// ExtJS event listener
		c.on('show', function() {
			that.hideGroupIfEmpty();
		});
		// ExtJS event listener
		c.on('hide', function() {
			that.hideGroupIfEmpty();
		});

		return c;
	}
});
/** api: xtype = gx_ux_legendgroup */
Ext.reg('gx_ux_legendgroup', GeoExt.ux.LegendGroup);
