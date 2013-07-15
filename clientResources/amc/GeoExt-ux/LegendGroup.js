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
	label: null,

	initComponent: function() {
		GeoExt.ux.LegendGroup.superclass.initComponent.call(this);
		if (this.groupName) {
			this.label = this.add({
				xtype: "label",
				text: this.groupName,
				cls: (this.headerCls ? ' ' + this.headerCls : '')
			});
			this.indexOffset++;

			var setDpi = function() {
				this.label.un('afterrender', setDpi, this);
				this.onDpiChange({ dpi: this.mapPanel.dpi });
			};

			this.label.on('afterrender', setDpi, this);
		}

		if (this.mapPanel) {
			this.mapPanel.ol_on('dpiChange', this.onDpiChange, this);
			this.on('destroy', function() {
				this.mapPanel.ol_un('dpiChange', this.onDpiChange, this);
			}, this);
		}
	},

	onDpiChange: function(evt) {
		if (this.label) {
			var dpi = evt.dpi;
			var defaultDpi = this.mapPanel ? this.mapPanel.DEFAULT_DPI : 90;
			if (defaultDpi <= 0) {
				defaultDpi = 90;
			}
			var ratio = dpi / defaultDpi;

			// ExtJS default text size for label is 12px.
			// If a different ExtJS template is used (like accessibility),
			// the font ratio will be wrong. But since this only affect
			// high DPI, we can ignore this issue.
			// NOTE: zoom attribute is a lot easier to use but IE do not
			//     support it properly. It change the displayed size but
			//     not the real estate (everything overlap)
			var fontSize = parseInt(12 * ratio);
			this.label.el.applyStyles('font-size: '+fontSize+'px;');
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

		var atlasLayer = c.layerRecord && c.layerRecord.getLayer() ? c.layerRecord.getLayer().atlasLayer : null;
		if (atlasLayer && atlasLayer.json) {
			c.legendDpiSupport = typeof(atlasLayer.json['legendDpiSupport']) === 'undefined' ? false : !!atlasLayer.json['legendDpiSupport'];
		}

		// Increase margin according to DPI
		var mapPanel = atlasLayer ? atlasLayer.mapPanel : null;
		if (mapPanel) {
			var normalMargin = 0.2;
			var defaultDpi = mapPanel ? mapPanel.DEFAULT_DPI : 90;

			var afterRender = function() {
				c.un('afterrender', afterRender, c);
				var initialRatio = mapPanel.dpi / defaultDpi;
				var initialMargin = initialRatio * normalMargin;
				this.getEl().applyStyles('margin: ' + initialMargin + 'em;');
			};
			c.on('afterrender', afterRender, c);

			c.onGroupDpiChange = function(evt) {
				var ratio = evt.dpi / defaultDpi;
				var margin = ratio * normalMargin;
				this.getEl().applyStyles('margin: ' + margin + 'em;');
			};

			mapPanel.ol_on('dpiChange', c.onGroupDpiChange, c);
			c.on('destroy', function() {
				mapPanel.ol_un('dpiChange', this.onGroupDpiChange, this);
			}, c);
		}

		return c;
	}
});
/** api: xtype = gx_ux_legendgroup */
Ext.reg('gx_ux_legendgroup', GeoExt.ux.LegendGroup);
