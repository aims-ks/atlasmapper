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

Atlas.Legend = Ext.extend(Ext.Window, {
	closable: false,
	frame: false,
	width: 200,
	boxMinWidth: 100,

	minimumVisibleHeight: 40,
	minimumVisibleWidth: 80,

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
		this.doInitialAlignment();
		this.hide();

		// Every time the browser window is resized, check if the legend is hidden and reposition it if needed.
		this.mapPanel.on('afterlayout', this.repositionIfHidden, this);
		this.on('move', this.repositionIfHidden, this);
		this.on('show', this.repositionIfHidden, this);
	},

	repositionIfHidden: function() {
		if (!this._isMoving && this.mapPanel.rendered && this.isVisible()) {
			// Size of the window
			// IMPORTANT: This is not the size of what the user can see
			//     in the browser, this is the total size of the whole
			//     page, including what is hidden by the scrollbars.
			//     This is quite tricky since every browser have
			//     their own idea about the value of the many existing
			//     attributes to get this.
			var windowWidth, windowHeight;

			// Get the viewport dimensions, defined in index.html
			var viewportSize = window.viewport;
			if (viewportSize) {
				// Size of the page, including what is hidden by the scroller
				windowWidth = viewportSize[0];
				windowHeight = viewportSize[1];
			} else {
				// There is no user defined viewport, get the size of the visible area instead
				// (using ExtJS, for better browser compatibility)
				var windowSize = Ext.getBody().getViewSize();
				windowWidth = windowSize['width'];
				windowHeight = windowSize['height'];
			}

			var legendPosition = this.getPosition();
			var legendWidth = this.getWidth();
			var legendHeight = this.getHeight();

			// Minimum visible legend width / height allow before
			// repositioning the legend.
			var needAdj = false;
			var adjPosition = [legendPosition[0], legendPosition[1]];

			// Too far off the screen to the left ?
			if (legendPosition[0] + legendWidth < this.minimumVisibleWidth) {
				needAdj = true;
				adjPosition[0] = this.minimumVisibleWidth - legendWidth;
			}

			// Too far off the screen to the right ?
			if (legendPosition[0] + this.minimumVisibleWidth > windowWidth) {
				needAdj = true;
				adjPosition[0] = windowWidth - this.minimumVisibleWidth;
			}

			// Too far off the screen to the top ?
			if (legendPosition[1] + legendHeight < this.minimumVisibleHeight) {
				needAdj = true;
				adjPosition[1] = this.minimumVisibleHeight - legendHeight;
			}

			// Too fat off the screen to the bottom ?
			if (legendPosition[1] + this.minimumVisibleHeight > windowHeight) {
				needAdj = true;
				adjPosition[1] = windowHeight - this.minimumVisibleHeight;
			}

			// WARNING! setPosition trigger 'move' event which trigger this method.
			//     To avoid infinite loop, we set the private temporary attribute '_isMoving'.
			//     (the 'move' event is still trigger and this method is still called but it does nothing)
			if (needAdj) {
				this._isMoving = true;
				this.setPosition(adjPosition[0], adjPosition[1]);
				delete this._isMoving;
			}
		}
	},

	doInitialAlignment: function() {
		// Align the top-right corner of the legend window
		// with the top-right corner of the mapPanel

		// NOTE: The Legend window is independent from the mapPanel,
		// but it use it to know where to be moved on the screen.
		// So, the align function can only be called once the mapPanel
		// as been rendered and layout.
		var afterLayout = function() {
			// The first time 'afterlayout' get called, the mapPanel width is too small (about 2px).
			// The second time, the mapPanel has its real width.
			if (this.mapPanel.body.getWidth() > 10) {
				this.mapPanel.un('afterlayout', afterLayout, this);
				this.alignTo(this.mapPanel.body, 'tr-tr', [-10, 10]);
			}
		};
		this.mapPanel.un('afterlayout', afterLayout, this);

		if (this.mapPanel.rendered) {
			afterLayout.call(this);
		} else {
			this.mapPanel.on('afterlayout', afterLayout, this);
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
	}
});
