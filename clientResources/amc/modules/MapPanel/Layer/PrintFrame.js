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
	supportLoadEvents: false,

	/**
	 * Constructor: Atlas.Layer.PrintFrame
	 *
	 * Parameters:
	 * mapPanel - {Object} Instance of the MapPanel in which the layer is used
	 * jsonLayer - {Object} Hashtable of layer attributes
	 * parent - {Object} Parent layer, used with folder (not applicable to print frames)
	 */
	initialize: function(mapPanel, jsonLayer, parent) {
		jsonLayer = jsonLayer || {};
		jsonLayer['title'] = jsonLayer['title'] || 'Print frame';
		if (!jsonLayer['description']) {
			jsonLayer['description'] = '<p>\n' +
					'	This layer is a tool to allow high quality maps to be created directly\n' +
					'	from a screenshot. It adds a moveable frame with labelled latitude and\n' +
					'	longitudes. It also includes a moveable north arrow and scale bar. A\n' +
					'	quality looking map can made by using a screenshot then cropping the\n' +
					'	image around the "Print frame".\n' +
					'</p>\n' +
					'<p>\n' +
					'	In the options you can also increase the Dots Per Inch (DPI) of the map\n' +
					'	to increase the size of the text and line rendering.\n' +
					'</p>\n' +
					'<div>\n' +
					'	To use the tool:\n' +
					'	<ol>\n' +
					'		<li>After clicking the "Print frame" button draw a rectangle on the map to select the region of interest.</li>\n' +
					'		<li>Adjust the "Print frame" to accurately frame desired location.</li>\n' +
					'		<li>Expand your browser window as necessary (larger is better).</li>\n' +
					'		<li>Use the "Locate" button in the "Print frame" Options to zoom as much as possible, without hiding the frame.</li>\n' +
					'		<li>Adjust the position of the north arrow and the scale line, if necessary.</li>\n' +
					'		<li>Adjust the styles of the layers to get the best map possible. Look in the Options tab of each layer to see available restyling options.</li>\n' +
					'		<li>Take a screenshot using the "Print Screen" key from your keyboard. On windows you can also use the Accessories/Snipping Tool application.</li>\n' +
					'		<li>Crop the image using an image editor software, to remove any part of the image exceeding the print frame. You can also directly paste the screenshot into Word or PowerPoint and use their crop tool.</li>\n' +
					'		<li>In Word or PowerPoint you can add additional text, arrows and titles.</li>\n' +
					'	</ol>\n' +
					'</div>\n' +
					'<br/>';
			jsonLayer['descriptionFormat'] = 'html';
		}

		Atlas.Layer.AbstractLayer.prototype.initialize.apply(this, [mapPanel, jsonLayer, parent]);

		var that = this;
		this.setLayer(new OpenLayers.Layer.ux.PrintFrame(this.getTitle(), {
			dpi: (jsonLayer.dpi ? jsonLayer.dpi : mapPanel.dpi),
			frameOptions: jsonLayer.frameOptions,
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
							var printFrameBounds = that.layer.getNativePrintedExtent();
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

		this._registerListeners();
	},

	_registerListeners: function() {
		if (this.mapPanel) {
			this.mapPanel.ol_on('dpiChange', this.onDpiChange, this);
			if (this.mapPanel.map) {
				this.mapPanel.map.events.on({
					'removelayer': this.onRemoveLayer,
					scope: this
				});
			}
		}
		if (this.layer) {
			this.layer.events.on({
				moveend: function(e) {
				},
				scope: this
			});
		}
	},

	_unregisterListeners: function() {
		if (this.mapPanel) {
			this.mapPanel.ol_un('dpiChange', this.onDpiChange, this);
			if (this.mapPanel.map) {
				this.mapPanel.map.events.un({
					'removelayer': this.onRemoveLayer,
					scope: this
				});
			}
		}
	},

	onRemoveLayer: function(evt) {
		if (evt.layer === this.layer) {
			this._unregisterListeners();
		}
	},

	// Called when the DPI change on the mapPanel
	onDpiChange: function(evt) {
		this.layer.setDPI(evt.dpi);
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

	getSavedState: function() {
		var extent = this.layer.getPrintedExtent();
		if (!extent) {
			return null;
		}

		// Same order as OpenLayers: left, bottom, right, top
		var savedState = '';
		savedState += this._coordToSignificantDigit(extent.left) + ':';
		savedState += this._coordToSignificantDigit(extent.bottom) + ':';
		savedState += this._coordToSignificantDigit(extent.right) + ':';
		savedState += this._coordToSignificantDigit(extent.top);

		// Add widget coordinates
		// North arrow
		var northArrowAnchor = this.layer.getNorthArrowAnchor();
		var arrowX = 0, arrowY = 0;
		if (northArrowAnchor) {
			arrowX = this._coordToSignificantDigit(northArrowAnchor.x);
			arrowY = this._coordToSignificantDigit(northArrowAnchor.y);
		}
		savedState += ':' + arrowX + ':' + arrowY;

		// Scale line
		var scaleLineAnchor = this.layer.getScaleLineAnchor();
		var scaleX = 0, scaleY = 0;
		if (scaleLineAnchor) {
			scaleX = this._coordToSignificantDigit(scaleLineAnchor.x);
			scaleY = this._coordToSignificantDigit(scaleLineAnchor.y);
		}
		savedState += ':' + scaleX + ':' + scaleY;

		return savedState;
	},

	_coordToSignificantDigit: function(value) {
		// Remove some digits, 5 is about 1 meter resolution. Also remove trailing 0s and the dot if needed:
		//     5.000002387394
		//     => (5.000002387394).toFixed(5) = "5.00000"
		//     => "5.00000".replace(/00*$/, '') = "5."
		//     => "5.".replace(/\.$/, '') = "5"
		//     => Return "5"
		return value.toFixed(5).replace(/00*$/, '').replace(/\.$/, '');
	}
});

// Static method
Atlas.Layer.PrintFrame.loadSavedState = function(mapPanel, savedState) {
	var savedStateValues = savedState.split(':');

	// It seems like there is a confusion between bottom and top.
	// The print frame do some correction if needed.
	var printFrame = new Atlas.Layer.PrintFrame(mapPanel, {'frameOptions': {
		'topLeft': [savedStateValues[0], savedStateValues[1]],
		'bottomRight': [savedStateValues[2], savedStateValues[3]],
		'northArrowLocation': [savedStateValues[4], savedStateValues[5]],
		'scaleLineLocation': [savedStateValues[6], savedStateValues[7]]
	}});

	return printFrame;
};
