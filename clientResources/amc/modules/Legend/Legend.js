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

Atlas.Legend = Ext.extend(Ext.Window, {
	closable: false,
	frame: false,
	width:200,
	boxMinWidth: 100,

	autoHeight: true,

	border:false,
	plain:true,
	layout: 'fit',

	// There is a small bug with shadow + constrain
	// http://www.sencha.com/forum/showthread.php?118809-shadowOffset-is-undefined-in-Ext.Window
	shadow: false,

	mapPanel: null,

	initComponent: function() {
		Atlas.Legend.superclass.initComponent.call(this);
		var legendPanel = new Atlas.LegendPanel({
			legendWindow: this,
			mapPanel: this.mapPanel
		});

		this.add(legendPanel);

		// Render the legend and hide it until legend elements are added.
		this.show();
		this._align();
		this.hide();
	},

	_align: function() {
		// Align the top-right corner of the legend window
		// with the top-right corner of the mapPanel
		// TODO Position it according to a config parameter?

		// NOTE: The Legend window is independent from the mapPanel,
		// but it use it to know where to be moved on the screen.
		// So, the align function can only be called once the mapPanel
		// as been rendered and layout.
		if (this.mapPanel.rendered) {
			this.alignTo(this.mapPanel.body, 'tr-tr', [-10, 10]);
		} else {
			var that = this;
			this.mapPanel.on('afterlayout', function() {
				that.alignTo(that.mapPanel.body, 'tr-tr', [-10, 10]);
			});
		}
	},

	/**
	 * Go out of autoheight/autowidth as soon as the user manually resize the legend window.
	 * See also Atlas.LegendPanel.onResize
	 */
	// To block the autoheight to a maxHeight, see:
	// http://www.sencha.com/forum/showthread.php?25704-Panel-with-autoHeight-ignores-MaxHeight
	// maxHeight: Ext.getBody().getViewSize().height
	onResize: function(adjWidth, adjHeight, rawWidth, rawHeight) {
		var w = adjWidth,
			h = adjHeight;

		if (h == 'auto' && Ext.isDefined(rawHeight) && this.getHeight() != rawHeight) {
			this.autoHeight = false;
			h = rawHeight;
		}
		if (w == 'auto' && Ext.isDefined(rawWidth) && this.getWidth() != rawWidth) {
			this.autoWidth = false;
			w = rawWidth;
		}

		Atlas.Legend.superclass.onResize.call(this, w, h, rawWidth, rawHeight);
	},

	// Adjust the position when setPosition is called
	adjustPosition : function(x, y){
		var adjx = x, adjy = y;
		var mapPos = this.mapPanel.getPosition();

		// To limit the legend on the mapPanel (do not allow dd on the left panel);
		//     minx = mapPos[0]
		// To force the legend to always be visible (never hidden in the bottom of the browser window);
		//     maxy = mapPos[1] + this.mapPanel.getHeight() - this.getHeight()
		var minx = 0,
			maxx = mapPos[0] + this.mapPanel.getWidth() - this.getWidth(),
			miny = mapPos[1],
			maxy = mapPos[1] + this.mapPanel.getHeight() - 35;

		if (maxx < minx) { maxx = minx; }
		if (maxy < miny) { maxy = miny; }

		if (adjx < minx) { adjx = minx; }
		if (adjx > maxx) { adjx = maxx; }

		if (adjy < miny) { adjy = miny; }
		if (adjy > maxy) { adjy = maxy; }

		this.x = adjx;
		this.y = adjy;

		return {x: adjx, y: adjy};
	}
});
