/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2011 Australian Institute of Marine Science
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
		if (Ext.isIE6) {
			this.setWidth(parseInt(this.ownerCt.getEl().dom.style.width) - 15);
		}
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
