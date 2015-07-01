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

/**
 * Instanciation:
 *     new OpenLayers.Layer.ux.PrintFrame(title, options);
 *         title: The string displayed in the layer switcher.
 *         options: An object containing the layer options (described bellow)
 *
 * options:
 *     dpi: Desired printed DPI, preferably matching a zoom level (90 * 2^x => for example: 90, 180, 360); everything is scaled up. Default: 90.
 *     frameOptions: An object containing options specific to the Print Frame (described bellow)
 *     attributions: String (or function that return a String) used for the attributions. Default: no attributions.
 *         This value is updated every time:
 *             * The event 'attributionsChange' is fired on the map
 *             * The print frame is resized / dragged (the attribution may change when some layers become out of scope, for the print frame)
 *             * The map is paned or zoomed
 *             * The map layers selection has changed (layer added, removed or changed)
 *
 * frameOptions:
 *     topLeft: Print frame top-left coordinates (2D array of double; [x, y]). Default: if not specified, the user will be asked to draw a rectangle on the map.
 *     bottomRight: Print frame bottom-right coordinates (2D array of double; [x, y]). Default: if not specified, the layer will ask the user to draw a rectangle on the map.
 *         Example:
 *             * 90: default resolution; no diferences.
 *             * 180: double resolution; text, lines, patterns, etc. are twice bigger.
 *             * 360: 4x resolution; text, lines, patterns, etc. are 4 times bigger.
 *     strokeWidth: Width of the coordinates lines, scale lines and print frame border, in pixels. Default: 2.
 *     frameWidth: Width of the white border around the print frame, in pixels. Default: 100.
 *     labelsFontSize: Font size of the labels for the coordinate lines, in pixels. Default: 12.
 *     attributionsFontSize: Font size of the attributions line, at the bottom of the print frame, in pixels. Default: 10.
 *     coordLinesWidth: Length of the larger coordinate lines, in pixels. Default: 8.
 *     scaleFontSize: Font size of the labels of the scale widget, in pixels. Default: 9.
 *     controlsRadius: Size of the handle used to move / resize the print frame, in pixels. Default: 12.
 *
 *     northArrowLocation: Coordinate (in lon/lat) of the center of the North arrow (not considering the label).
 *     northArrowOffset: Offset of the north arrow (2d array of double; [x, y]), from the top left corner of the printed frame, in pixels (negative values allowed). Default: [20, 30].
 *         NOTE: This attribute is ignored if northArrowLocation is specified.
 *
 *     scaleLineLocation: Coordinate (in lon/lat) of the center of the left edge of the scale line.
 *     scaleLineOffset: Offset of the scale line (2d array of double; [x, y]), from the bottom left corner of the printed frame, in pixels (negative values allowed). Default: [0, 50].
 *         NOTE: This attribute is ignored if scaleLineLocation is specified.
 *
 */
OpenLayers.Layer.ux = OpenLayers.Layer.ux || {};
OpenLayers.Layer.ux.PrintFrame = OpenLayers.Class(OpenLayers.Layer.Vector, {
	CLASS_NAME: "OpenLayers.Layer.ux.PrintFrame",

	// Default values
	DEFAULT_DPI: 90, // DPI (Dots Per Inch) used by GeoServer when no DPI value is specified
	strokeWidth: 1, // in pixel
	labelsFontSize: 12, // in pixel
	attributionsFontSize: 10, // in pixel
	scaleFontSize: 10, // in pixel (font size used by OpenLayers: 9px)
	frameWidth: 100, // in pixel
	coordLinesWidth: 8, // in pixel

	// Real value, in ratio with DPI - For internal usage
	_dpiRatio: null, // >1 to increase the the strokes width, font size, etc.
	_strokeWidth: null,
	_labelsFontSize: null,
	_attributionsFontSize: null,
	_scaleFontSize: null,
	_frameWidth: null,
	_coordLinesWidth: null,
	_topCoordLabelsDensity: 1,
	_leftCoordLabelsDensity: 1,

	// The offset (in pixels) between the clicked location and the
	// bottom right corner of the frame, calculated before resize start
	// and used during resize.
	_resizeOffset: null,
	// Location of the resize point, used to calculate the offset.
	_resizePoint: null,
	// Used to know if the mouse is still over the resizing handle.
	_resizeCornerFeature: null,

	// Offset of the north arrow / scale line, from the top left corner of the printed frame, in unit of the map (degree, meter, etc.).
	// (array of floats [X, Y])
	northArrowLocation: null,
	bboxArrow: null,
	scaleLineLocation: null,
	bboxScale: null,
	attributionsFeature: null,


	// private
	_attributionsLabel: null,

	// private
	_oneDegreeLength: 0, // length of one degree, in pixel, for the current zoom level. Used to calculate how many coordinate line has to be drawn.

	defaultLonLatProjection: new OpenLayers.Projection('EPSG:4326'),

	// Reference to the inner frame, the printed area of the map, used to know where to draw the coordinate lines and to generate a saved state
	/**
	 * (+)-----------------
	 *  |  (White frame)  |
	 *  |   -----------   |
	 *  |   | Printed |   |
	 *  |   |  Frame  |   |
	 *  |   -----------   |
	 *  |                 |
	 *  -----------------(>)
	 */
	printedFrame: null,

	// Anchor point of the widgets, used to generate a saved state
	northArrowAnchor: null, // North arrow anchor point
	scaleLineAnchor: null, // Scale line anchor point

	// private
	_coordLinesLiveUpdate: true,
	_initiated: false,
	_frameFeatures: null,
	_coordLinesFeatures: null,
	_northArrowFeatures: null,
	_scaleLineFeatures: null,

	initialize: function(name, options) {
		// enable the indexer by setting zIndexing to true
		options = options || {};
		options.rendererOptions = options.rendererOptions || {};
		options.rendererOptions.zIndexing = true;

		OpenLayers.Layer.Vector.prototype.initialize.apply(this, [name, options]);

		this.frameOptions = this.frameOptions || {};
		this._dpiRatio = (this.dpi || this.DEFAULT_DPI) / this.DEFAULT_DPI;
	},

	setDPI: function(dpi) {
		var newDPIRatio = dpi / this.DEFAULT_DPI;
		if (newDPIRatio != this._dpiRatio) {
			this._dpiRatio = newDPIRatio

			// Find the actual frame bounds, in lon/lat
			var printedFrameBounds = this.getPrintedExtent();

			this._redraw(
				[printedFrameBounds.left, printedFrameBounds.top],
				[printedFrameBounds.right, printedFrameBounds.bottom]
			);
		}
	},

	setTopCoordLabelsDensity: function(topCoordLabelsDensity) {
		this._topCoordLabelsDensity = topCoordLabelsDensity;
		// Redraw the lines
		this.removeFeatures(this._coordLinesFeatures);
		this._coordLinesFeatures = this._drawCoordLines();
		this.addFeatures(this._coordLinesFeatures);
	},
	getTopCoordLabelsDensity: function() {
		return this._topCoordLabelsDensity;
	},

	setLeftCoordLabelsDensity: function(leftCoordLabelsDensity) {
		this._leftCoordLabelsDensity = leftCoordLabelsDensity;
		// Redraw the lines
		this.removeFeatures(this._coordLinesFeatures);
		this._coordLinesFeatures = this._drawCoordLines();
		this.addFeatures(this._coordLinesFeatures);
	},
	getLeftCoordLabelsDensity: function() {
		return this._leftCoordLabelsDensity;
	},

	// Get the printed extent (the hole in the frame), in lon/lat projection
	getPrintedExtent: function() {
		var nativeExtent = this.getNativePrintedExtent();
		if (nativeExtent) {
			return this._deproject(this.getNativePrintedExtent().clone());
		}
		return null;
	},

	// Get the printed extent (the hole in the frame), in native projection
	getNativePrintedExtent: function() {
		if (this.printedFrame && this.printedFrame.geometry) {
			return this.printedFrame.geometry.bounds;
		}
		return null;
	},

	getNorthArrowAnchor: function() {
		if (this.northArrowAnchor) {
			return this._deproject(this.northArrowAnchor.clone());
		}
		return null;
	},

	getScaleLineAnchor: function() {
		if (this.scaleLineAnchor) {
			return this._deproject(this.scaleLineAnchor.clone());
		}
		return null;
	},

	afterAdd: function() {
		OpenLayers.Layer.Vector.prototype.afterAdd.apply(this, arguments);

		// This method is also called every time the layer order is changed.
		// The following has to be called only once, after the layer
		// is added to the map.
		if (!this._initiated) {
			this._initiated = true;
			var that = this;
			var frameHoleTopLeft = this.frameOptions.topLeft;
			var frameHoleBottomRight = this.frameOptions.bottomRight;
			var northArrowLocation = this.frameOptions.northArrowLocation;
			var scaleLineLocation = this.frameOptions.scaleLineLocation;

			if (northArrowLocation) {
				var reprojected = this._reproject(new OpenLayers.Geometry.Point(northArrowLocation[0], northArrowLocation[1]));
				this.northArrowLocation = [reprojected.x, reprojected.y];
			}
			if (scaleLineLocation) {
				var reprojected = this._reproject(new OpenLayers.Geometry.Point(scaleLineLocation[0], scaleLineLocation[1]));
				this.scaleLineLocation = [reprojected.x, reprojected.y];
			}

			if (frameHoleTopLeft && frameHoleBottomRight) {
				// Draw a print frame according to the given values

				// Do some corrections, in case there is confusion between left & right or top & bottom
				var top, left, bottom, right;
				if (frameHoleBottomRight[1] < frameHoleTopLeft[1]) {
					top = frameHoleTopLeft[1];
					bottom = frameHoleBottomRight[1];
				} else {
					top = frameHoleBottomRight[1];
					bottom = frameHoleTopLeft[1];
				}
				if (frameHoleTopLeft[0] < frameHoleBottomRight[0]) {
					left = frameHoleTopLeft[0];
					right = frameHoleBottomRight[0];
				} else {
					left = frameHoleBottomRight[0];
					right = frameHoleTopLeft[0];
				}

				this._init([left, top], [right, bottom]);
			} else {
				// Let the user draw a rectangle on the map, and use
				// that rectangle as a base to create the print frame.
				// The user can press "ESC" at anytime during the drawing
				// process to cancel the addition of the print frame layer.

				var cursorBackup = null;
				if (this.map && this.map.div && this.map.div.style) {
					cursorBackup = this.map.div.style.cursor;
					this.map.div.style.cursor = 'crosshair';
				}

				function escListener(evt) {
					var handled = false;
					switch (evt.keyCode) {
						case OpenLayers.Event.KEY_ESC: // esc
							drawBoxControl.deactivate();
							if (that.map) {
								that.map.removeControl(drawBoxControl);
								if (that.map.div && that.map.div.style) {
									that.map.div.style.cursor = cursorBackup;
								}
								that.map.removeLayer(that);
							}
							handled = true;
							break;
					}
					if (handled) {
						OpenLayers.Event.stop(evt);
					}
				}

				var drawBoxControl = new OpenLayers.Control.DrawFeature(
					this, // Draw the rectangle into "this" layer
					OpenLayers.Handler.RegularPolygon, {
						handlerOptions: {
							sides: 4,
							irregular: true // set to false for square, with rotation
						},
						featureAdded: function(feature) {
							var bounds = that._deproject(feature.geometry.bounds.clone());
							var frameHoleTopLeft = [bounds.left, bounds.top];
							var frameHoleBottomRight = [bounds.right, bounds.bottom];

							// Remove the drawing handler and the drew polygon, we don't need them anymore
							drawBoxControl.deactivate();
							that.map.removeControl(drawBoxControl);
							that.removeFeatures([feature]);
							
							// Remove the ESC listener. From this point, the layer has to
							// be removed using the [-] icon from the layer switcher.
							OpenLayers.Event.stopObserving(document, "keydown", escListener);

							// Draw the print frame with the desired coordinates
							that._init(frameHoleTopLeft, frameHoleBottomRight);
							if (that.map.div && that.map.div.style) {
								that.map.div.style.cursor = cursorBackup;
							}
						}
					}
				);

				this.map.addControl(drawBoxControl);
				drawBoxControl.activate();

				// Keyboard listener used to cancel the box drawing and delete the PrintFrame layer when the key ESC is pressed
				OpenLayers.Event.observe(document, "keydown", escListener);

				// Listener to cancel the box drawing if the layer is deleted before the box has been drawn.
				var map = this.map;
				this.events.register('removed', this, function() {
					OpenLayers.Event.stopObserving(document, "keydown", escListener);
					if (drawBoxControl) {
						drawBoxControl.deactivate();
						// "this.map" can not be used here since the layer has already be removed from the map ("this.map" is null)
						if (map && map.div && map.div.style) {
							map.div.style.cursor = cursorBackup;
						}
					}
				});
			}
		}
	},

	// This method need to access some attribute of the map, such as
	// the map projection, resolution (zoom level), etc.
	// Therefore, it can only be called after the layer has been added
	// to the map.
	_init: function(frameHoleTopLeft, frameHoleBottomRight) {
		var that = this;

		this._redraw(frameHoleTopLeft, frameHoleBottomRight);

		var isIE = OpenLayers.Util.getBrowserName() === 'msie';

		// Dragable
		// NOTE: To make all PrintFrame dragable, instead of only the
		//     one on top, this layer must be added to the
		//     multiSelectDragFeature, after activate a unique
		//     DragFeature for the whole layer.
		var dragControl = new OpenLayers.Control.DragFeature(this, {
			onStart: this.startDrag,
			onDrag: this.doDrag,
			onComplete: this.endDrag
		});

		// Little hack to enable the drag handler only on specified features
		dragControl.handlers.feature.geometryTypeMatches = function(feature) {
			return feature.isDragable === true;
		};

		this.map.addControl(dragControl);
		dragControl.activate();

		var multiSelectDragFeature = OpenLayers.Control.ux.MultiSelectDragFeature.getInstance(this.map);
		multiSelectDragFeature.addLayer(this);


		// true: Coordinate lines are updated while the frame moves around.
		// false: Coordinate lines are hidden while the frame moves around, and redrawn when it stops moving.
		// NOTE: This is always false for IE (all version of IE are way too slow to handle that feature)
		if (isIE) {
			this._coordLinesLiveUpdate = false;
		}

		// Register events
		this.events.on({
			moveend: function(e) {
				if (e.zoomChanged) {
					this._onZoomChange();

					// Find the actual frame bounds, in lon/lat
					var printedFrameBounds = this.getPrintedExtent();

					// Redraw the frame
					this.removeFeatures(this._frameFeatures);
					this._frameFeatures = this._drawFrame([printedFrameBounds.left, printedFrameBounds.top], [printedFrameBounds.right, printedFrameBounds.bottom]);
					this.addFeatures(this._frameFeatures);

					// Redraw the lines
					this.removeFeatures(this._coordLinesFeatures);
					this._coordLinesFeatures = this._drawCoordLines();
					this.addFeatures(this._coordLinesFeatures);

					// Redraw the arrow
					this.removeFeatures(this._northArrowFeatures);
					this._northArrowFeatures = this._drawNorthArrow();
					this.addFeatures(this._northArrowFeatures);
				}

				// Redraw the scale line (even when the map is panned, the scale line is geodesic)
				this.removeFeatures(this._scaleLineFeatures);
				this._scaleLineFeatures = this._drawScaleLine();
				this.addFeatures(this._scaleLineFeatures);

				this._updateAttributions();
			},
			scope: this
		});

		this._registerListeners();
	},







	startDrag: function(feature, pixel) {
		var that = this.layer;

		if (feature.isDragHandle) {
			// Drag handle starting to move
			if (!that._coordLinesLiveUpdate) {
				that.removeFeatures(that._coordLinesFeatures);
			}
			lastPixel = pixel;

		} else if (feature.isResizeHandle) {
			// Calculate the offset (in pixels) between the clicked location and the bottom right corner
			var resizePointPixel = that.map.getPixelFromLonLat(new OpenLayers.LonLat(that._resizePoint.x, that._resizePoint.y));
			that._resizeOffset = new OpenLayers.Pixel(resizePointPixel.x - pixel.x, resizePointPixel.y - pixel.y);

			// Resize handle starting to move - remove the coordinate lines with slow browsers (Internet Explorer)
			if (!that._coordLinesLiveUpdate) {
				that.removeFeatures(that._coordLinesFeatures);
			}
			lastPixel = pixel;

		} else if (feature.isScaleHandle) {
			lastPixel = pixel;

		} else if (feature.isNorthArrowHandle) {
			lastPixel = pixel;
		}
	},

	doDrag: function(feature, pixel) {
		var that = this.layer;

		if (feature.isDragHandle) {
			// Drag handle moving
			var res = that.map.getResolution();
			// Move the frame
			for (var i = 0; i < that._frameFeatures.length; i++) {
				if (feature != that._frameFeatures[i]) {
					that._moveFeature(that._frameFeatures[i], res * (pixel.x - lastPixel.x), res * (lastPixel.y - pixel.y));
					that.drawFeature(that._frameFeatures[i]);
				}
			}
			// Move the arrow
			for (var i = 0; i < that._scaleLineFeatures.length; i++) {
				that._moveFeature(that._scaleLineFeatures[i], res * (pixel.x - lastPixel.x), res * (lastPixel.y - pixel.y));
				that.drawFeature(that._scaleLineFeatures[i]);
			}
			// Move the scale line (this feature is refreshed after move)
			for (var i = 0; i < that._northArrowFeatures.length; i++) {
				that._moveFeature(that._northArrowFeatures[i], res * (pixel.x - lastPixel.x), res * (lastPixel.y - pixel.y));
				that.drawFeature(that._northArrowFeatures[i]);
			}

			if (that._coordLinesLiveUpdate) {
				// Redraw the coordinate lines
				that.removeFeatures(that._coordLinesFeatures);
				that._coordLinesFeatures = that._drawCoordLines();
				that.addFeatures(that._coordLinesFeatures);
			}
			lastPixel = pixel;

		} else if (feature.isResizeHandle) {
			// Resize handle moving
			that.removeFeatures(that._frameFeatures);
			// The feature that is dragged get redrawn by open layers, and by this method. The ghost has to be deleted manually.
			that.removeFeatures([feature]);
			that._frameFeatures = that._resizeFrame(new OpenLayers.Pixel(pixel.x + that._resizeOffset.x, pixel.y + that._resizeOffset.y));
			that.addFeatures(that._frameFeatures);

			if (that._coordLinesLiveUpdate) {
				that.removeFeatures(that._coordLinesFeatures);
				that._coordLinesFeatures = that._drawCoordLines();
				that.addFeatures(that._coordLinesFeatures);
				that._updateWidgetsPosition();
			}
			lastPixel = pixel;

		} else if (feature.isScaleHandle) {
			for (var i = 0; i < that._scaleLineFeatures.length; i++) {
				if (feature != that._scaleLineFeatures[i]) {
					var res = that.map.getResolution();
					that._moveFeature(that._scaleLineFeatures[i], res * (pixel.x - lastPixel.x), res * (lastPixel.y - pixel.y));
					that.drawFeature(that._scaleLineFeatures[i]);
				}
			}

			lastPixel = pixel;

		} else if (feature.isNorthArrowHandle) {
			for (var i = 0; i < that._northArrowFeatures.length; i++) {
				if (feature != that._northArrowFeatures[i]) {
					var res = that.map.getResolution();
					that._moveFeature(that._northArrowFeatures[i], res * (pixel.x - lastPixel.x), res * (lastPixel.y - pixel.y));
					that.drawFeature(that._northArrowFeatures[i]);
				}
			}

			lastPixel = pixel;
		}

		// Update the save state URL, if needed
		if (that.atlasLayer && that.atlasLayer.mapPanel && that.atlasLayer.mapPanel.pullState) {
			that.atlasLayer.mapPanel.pushState();
		}
	},

	endDrag: function(feature, pixel) {
		var that = this.layer;

		if (feature.isDragHandle) {
			// Drag handle stopped moving
			if (!that._coordLinesLiveUpdate) {
				// Redraw the lines, if they are not live
				that._coordLinesFeatures = that._drawCoordLines();
				that.addFeatures(that._coordLinesFeatures);
			}
			// Set the features state to UPDATE (probably not needed)
			for (var i = 0; i < that._frameFeatures.length; i++) {
				that._frameFeatures[i].state = OpenLayers.State.UPDATE;
			}
			for (var i = 0; i < that._northArrowFeatures.length; i++) {
				that._northArrowFeatures[i].state = OpenLayers.State.UPDATE;
			}
			that._updateAttributions();
			// Redraw the scale line
			that.removeFeatures(that._scaleLineFeatures);
			that._scaleLineFeatures = that._drawScaleLine();
			that.addFeatures(that._scaleLineFeatures);

		} else if (feature.isResizeHandle) {
			// Resize handle stopped moving
			// "outFeature" is not automatically called when the resize handle is moved over the limit
			// (frame resized to a size smaller than 0) because the mouse is not over the handle anymore.
			var pixelLonLat = that.map.getLonLatFromPixel(pixel);
			var pixelPoint = new OpenLayers.Geometry.Point(pixelLonLat.lon, pixelLonLat.lat);
			if (!pixelPoint.intersects(that._resizeCornerFeature.geometry)) {
				this.outFeature(feature); // NOTE: outFeature is a method of the DragFeature
			}

			// deResize delete the ghosts, but maybe some browsers will have issue with the last one...
			that.removeFeatures([feature]);

			if (!that._coordLinesLiveUpdate) {
				that._coordLinesFeatures = that._drawCoordLines();
				that.addFeatures(that._coordLinesFeatures);
				that._updateWidgetsPosition();
			}
			that._updateAttributions();
			for (var i = 0; i < that._frameFeatures.length; i++) {
				that._frameFeatures[i].state = OpenLayers.State.UPDATE;
			}

		} else if (feature.isScaleHandle) {
			for (var i = 0; i < that._scaleLineFeatures.length; i++) {
				that._scaleLineFeatures[i].state = OpenLayers.State.UPDATE;
			}
			that._updateScaleLineLocation();
			that._updateWidgetsPosition();

		} else if (feature.isNorthArrowHandle) {
			for (var i = 0; i < that._northArrowFeatures.length; i++) {
				that._northArrowFeatures[i].state = OpenLayers.State.UPDATE;
			}
			that._updateNorthArrowLocation();
			that._updateWidgetsPosition();
		}
	},

	_moveFeature: function(feature, x, y) {
		var geometry = null;
		if (typeof(feature.geometry) === 'object' && typeof(feature.geometry.move) === 'function') {
			geometry = feature.geometry;
		} else if (typeof(feature.move) === 'function') {
			geometry = feature;
		}
		if (geometry) {
			geometry.move(x, y);
		}
	},

	/**
	 * Ensure the widgets stay close to the edge of the print frame.
	 * Widgets: North arrow and scale line
	 */
	_updateWidgetsPosition: function() {
		if (this._scaleLineFeatures || this._northArrowFeatures) {
			// Constants for better readability
			var X = 0, Y = 1;

			// Find the actual frame bounds, in the unit of the map
			var printedFrameBounds = this.getNativePrintedExtent();
			var frameHoleTopLeft = [printedFrameBounds.left, printedFrameBounds.top];
			var frameHoleBottomRight = [printedFrameBounds.right, printedFrameBounds.bottom];

			var res = this.map.getResolution();
			var frameWidthDegree = res * this._frameWidth; // frame width, in the unit of the map (degree, meter, etc.)

			// The bottom right corner of the frame (including the white space)
			var frameTopLeft = [frameHoleTopLeft[X] - frameWidthDegree, frameHoleTopLeft[Y] + frameWidthDegree];
			var frameBottomRight = [frameHoleBottomRight[X] + frameWidthDegree, frameHoleBottomRight[Y] - frameWidthDegree];

			if (this._scaleLineFeatures) {
				var locationModified = false;
				var newX = this.scaleLineLocation[X];
				var newY = this.scaleLineLocation[Y];

				if (newX > frameBottomRight[X]) {
					newX = frameBottomRight[X];
					locationModified = true;
				}
				if (newX < frameTopLeft[X]) {
					newX = frameTopLeft[X];
					locationModified = true;
				}

				if (newY < frameBottomRight[Y]) {
					newY = frameBottomRight[Y];
					locationModified = true;
				}
				if (newY > frameTopLeft[Y]) {
					newY = frameTopLeft[Y];
					locationModified = true;
				}

				if (locationModified) {
					this.removeFeatures(this._scaleLineFeatures);

					this.scaleLineLocation[X] = newX;
					this.scaleLineLocation[Y] = newY;

					this._scaleLineFeatures = this._drawScaleLine();
					this.addFeatures(this._scaleLineFeatures);
				}
			}

			if (this._northArrowFeatures) {
				var locationModified = false;
				var newX = this.northArrowLocation[X];
				var newY = this.northArrowLocation[Y];

				if (newX > frameBottomRight[X]) {
					newX = frameBottomRight[X];
					locationModified = true;
				}
				if (newX < frameTopLeft[X]) {
					newX = frameTopLeft[X];
					locationModified = true;
				}

				if (newY < frameBottomRight[Y]) {
					newY = frameBottomRight[Y];
					locationModified = true;
				}
				if (newY > frameTopLeft[Y]) {
					newY = frameTopLeft[Y];
					locationModified = true;
				}

				if (locationModified) {
					this.removeFeatures(this._northArrowFeatures);

					this.northArrowLocation[X] = newX;
					this.northArrowLocation[Y] = newY;

					this._northArrowFeatures = this._drawNorthArrow();
					this.addFeatures(this._northArrowFeatures);
				}
			}
		}
	},

	_registerListeners: function() {
		if (this.map) {
			this.map.events.on({
				'addlayer': this.onMapLayersChange,
				'changelayer': this.onMapLayersChange,
				'attributionsChange': this.onMapLayersChange,
				'removelayer': this.onRemoveLayer,
				scope: this
			});
		}
	},

	_unregisterListeners: function(map) {
		if (map) {
			map.events.un({
				'addlayer': this.onMapLayersChange,
				'changelayer': this.onMapLayersChange,
				'attributionsChange': this.onMapLayersChange,
				'removelayer': this.onRemoveLayer,
				scope: this
			});
		}
	},

	onRemoveLayer: function(evt) {
		if (evt.layer !== this) {
			this._updateAttributions();
		} else {
			// The map has to be passed in parameter because this event
			// is fired after the layer has been removed, so its
			// attribute "map" has been set to null.
			this._unregisterListeners(evt.object);
		}
	},

	onMapLayersChange: function(evt) {
		if (evt.layer !== this) {
			this._updateAttributions();
		}
	},

	// The method dans "redraw" is already defined in OpenLayers...
	_redraw: function(frameHoleTopLeft, frameHoleBottomRight) {
		this._strokeWidth = (this.frameOptions.strokeWidth || this.strokeWidth) * this._dpiRatio;
		this._frameWidth = (this.frameOptions.frameWidth || this.frameWidth) * this._dpiRatio;
		this._labelsFontSize = (this.frameOptions.labelsFontSize || this.labelsFontSize) * this._dpiRatio;
		this._attributionsFontSize = (this.frameOptions.attributionsFontSize || this.attributionsFontSize) * this._dpiRatio;
		this._coordLinesWidth = (this.frameOptions.coordLinesWidth || this.coordLinesWidth) * this._dpiRatio;
		this._scaleFontSize = (this.frameOptions.scaleFontSize || this.scaleFontSize) * this._dpiRatio;


		this._onZoomChange();

		if (this._frameFeatures) { this.removeFeatures(this._frameFeatures); }
		this._frameFeatures = this._drawFrame(frameHoleTopLeft, frameHoleBottomRight);
		this.addFeatures(this._frameFeatures);

		this._updateAttributions();

		if (this._coordLinesFeatures) { this.removeFeatures(this._coordLinesFeatures); }
		this._coordLinesFeatures = this._drawCoordLines();
		this.addFeatures(this._coordLinesFeatures);

		if (this._northArrowFeatures) { this.removeFeatures(this._northArrowFeatures); }
		this._northArrowFeatures = this._drawNorthArrow();
		this.addFeatures(this._northArrowFeatures);

		if (this._scaleLineFeatures) { this.removeFeatures(this._scaleLineFeatures); }
		this._scaleLineFeatures = this._drawScaleLine();
		this.addFeatures(this._scaleLineFeatures);

		// For locate
		this.atlasLayer.extent = this.getDataExtent();
	},

	_updateAttributions: function() {
		// NOTE: Images (logos) are currently unsupported. They could be added using externalGraphic (style attribute) on a point. It won't be easy, but it's a solution...
		var attributionsStr = null;
		if (typeof(this.attributions) === 'string') {
			attributionsStr = this.attributions;
		} else if (typeof(this.attributions) === 'function') {
			attributionsStr = this.attributions();
		}
		if (this.attributionsFeature.style.label !== attributionsStr) {
			this._attributionsLabel = attributionsStr;
			this.attributionsFeature.style.label = attributionsStr;
			this.drawFeature(this.attributionsFeature);
		}
	},

	// method called when resizing the frame. The bottomRightPixel coordinate is the one of the bottom-right corner of the outer frame (not the printed frame)
	// private
	_resizeFrame: function(bottomRightPixel) {
		// Find the actual frame bounds, in lon/lat
		var printedFrameBounds = this.getPrintedExtent();

		// "pixel" represent the bottom-right corner of the frame, in pixels. We need the bottom-right corner of the hole of the frame.
		var pixelHoleBottomRight = bottomRightPixel.clone();
		pixelHoleBottomRight.x -= this._frameWidth;
		pixelHoleBottomRight.y -= this._frameWidth; // NOTE: That operation would be an addition if the value were in lon/lat.
		var newBottomRight = this._deproject(this.map.getLonLatFromPixel(pixelHoleBottomRight));

		return this._drawFrame([printedFrameBounds.left, printedFrameBounds.top], [newBottomRight.lon, newBottomRight.lat]);
	},

	/**
	 *    Move Handle
	 *   /  Move corner
	 * (+)-/-----------------------
	 *  |///      (Frame)         |
	 *  |///-------------------   |
	 *  |   |                 |   |
	 *  |   |                 |<--|--- Frame border
	 *  |   | (Printed Frame) |   |
	 *  |   |                 |   |
	 *  |   |                 |   |
	 *  |   -------------------///<--- Resize corner
	 *  |         Attributions ///|
	 *  -------------------------(>)
	 *                             \
	 *                              Resize Handle
	 * 
	 *              |
	 *              |
	 * --------------
	 *    '  '  |  '        } Coordinate line width
	 *                      } Offset (equals to attributions font size)
	 *   Attributions       } Attributions
	 */
	_drawFrame: function(frameHoleTopLeft, frameHoleBottomRight) {
		var zIndex = 50;
		// Constants for better readability
		var X = 0, Y = 1;

		// Frame styles
		// See: http://dev.openlayers.org/docs/files/OpenLayers/Feature/Vector-js.html#OpenLayers.Feature.Vector.OpenLayers.Feature.Vector.style
		var controlsRadius = this.frameOptions.controlsRadius || 20;

		var frameStyle = {
			graphicZIndex: zIndex,
			stroke: false,
			fillColor: '#FFFFFF',
			fillOpacity: 1
		};
		var frameBorderStyle = {
			graphicZIndex: zIndex+1,
			fill: false,
			strokeColor: '#000000',
			strokeWidth: this._strokeWidth
		};

		var moveStyle = {
			graphicZIndex: zIndex+3,
			externalGraphic: "resources/images/move-frame.png",

			fillColor: '#FFFFFF',
			fillOpacity: 1,
			pointRadius: controlsRadius
		};
		var moveCornerStyle = {
			graphicZIndex: zIndex+2,
			stroke: false,
			fillOpacity: 0 // "fill: false" tells the browser to not render the shape. I need the shape to enable the dragging.
		};
		var resizeStyle = {
			graphicZIndex: zIndex+3,
			externalGraphic: "resources/images/resize-frame.png",

			fillColor: '#FFFFFF',
			fillOpacity: 1,
			pointRadius: controlsRadius,
			cursor: 'se-resize'
		};
		var resizeCornerStyle = {
			graphicZIndex: zIndex+2,
			stroke: false,
			fillOpacity: 0, // "fill: false" tells the browser to not render the shape. I need the shape to enable the dragging.
			cursor: 'se-resize'
		};

		var attributionsStyle = {
			graphicZIndex: zIndex+1,
			fill: false,
			stroke: false,
			labelAlign: 'rm', // right - middle
			// Offset the center to the size of attributions font size)
			labelYOffset: (this._attributionsFontSize * -1.5),
			fontColor: '#000000',
			fontSize: this._attributionsFontSize + 'px',
			fontFamily: 'Verdana, Geneva, sans-serif',
			label: this._attributionsLabel || 'Loading...'
		};

		var res = this.map.getResolution();
		var frameWidthDegree = res * this._frameWidth; // frame width, in the unit of the map (degree, meter, etc.)
		var coordLinesWidthDegree = res * this._coordLinesWidth;

		// Adjust the value, to unsure no negative width / height
		frameHoleBottomRight[X] = this._correctLongitude(frameHoleBottomRight[X]);
		frameHoleTopLeft[X] = this._correctLongitude(frameHoleTopLeft[X]);

		// Allow passing over the date line
		if (frameHoleBottomRight[X] < frameHoleTopLeft[X]) { frameHoleBottomRight[X] += 360; }

		// Do not allow height lower than 0
		if (frameHoleBottomRight[Y] > frameHoleTopLeft[Y]) { frameHoleBottomRight[Y] = frameHoleTopLeft[Y]; }

		// When the print frame get smaller than 0, it wrap around the world (with wrapDateLine: true).
		// Unfortunatelly, there is no easy way to prevent that since a print frame of 360 degrees is
		// theorically valid.
		// The bug can be reduced by limitating the maximum print frame width to 300 degrees.
		if (frameHoleBottomRight[X] - frameHoleTopLeft[X] > 300) { frameHoleBottomRight[X] = frameHoleTopLeft[X]; }


		var frameTopLeft = this._reproject(new OpenLayers.Geometry.Point(frameHoleTopLeft[X], frameHoleTopLeft[Y]));
		frameTopLeft.x -= frameWidthDegree;
		frameTopLeft.y += frameWidthDegree;

		var frameBottomRight = this._reproject(new OpenLayers.Geometry.Point(frameHoleBottomRight[X], frameHoleBottomRight[Y]));
		frameBottomRight.x += frameWidthDegree;
		frameBottomRight.y -= frameWidthDegree;

		var attributionsPosition = this._reproject(new OpenLayers.Geometry.Point(frameHoleBottomRight[X], frameHoleBottomRight[Y]));
		attributionsPosition.y -= coordLinesWidthDegree;


		// create a polygon feature from a linear ring of points
		var framePointList = [];
		framePointList.push(frameTopLeft);
		framePointList.push(new OpenLayers.Geometry.Point(frameBottomRight.x, frameTopLeft.y));
		framePointList.push(frameBottomRight);
		framePointList.push(new OpenLayers.Geometry.Point(frameTopLeft.x, frameBottomRight.y));
		// Close the polygon
		framePointList.push(framePointList[0]);
		var frameRing = new OpenLayers.Geometry.LinearRing(framePointList);

		// create a polygon HOLE from a linear ring of points (a hole is just an other linear ring that is geometrically inside an other one)
		var frameHolePointList = [];
		frameHolePointList.push(new OpenLayers.Geometry.Point(frameHoleTopLeft[X], frameHoleTopLeft[Y]));
		frameHolePointList.push(new OpenLayers.Geometry.Point(frameHoleBottomRight[X], frameHoleTopLeft[Y]));
		frameHolePointList.push(new OpenLayers.Geometry.Point(frameHoleBottomRight[X], frameHoleBottomRight[Y]));
		frameHolePointList.push(new OpenLayers.Geometry.Point(frameHoleTopLeft[X], frameHoleBottomRight[Y]));
		// Close the polygon
		frameHolePointList.push(frameHolePointList[0]);
		var frameHoleRing = this._reproject(new OpenLayers.Geometry.LinearRing(frameHolePointList));

		// create a frame border, with the same dimensions as the hole in the frame (inner border)
		var frameBorder = frameHoleRing.clone();

		var frameFeatures = [];

		// frameFeature
		var frame = new OpenLayers.Feature.Vector(
			new OpenLayers.Geometry.Polygon([frameRing, frameHoleRing]),
			null, // attributes
			frameStyle
		);
		frameFeatures.push(frame);

		// Frame border
		this.printedFrame = new OpenLayers.Feature.Vector(
			new OpenLayers.Geometry.Polygon([frameBorder]),
			null, // attributes
			frameBorderStyle
		);
		frameFeatures.push(this.printedFrame);

		// Attributions
		this.attributionsFeature = new OpenLayers.Feature.Vector(attributionsPosition, null, attributionsStyle);
		frameFeatures.push(this.attributionsFeature);

		// Move icon (added before resize - displayed bellow, so it's always possible to resize even when the frame is very small)
		var movePoint = frameTopLeft.clone();
		var moveFeature = new OpenLayers.Feature.Vector(movePoint, null, moveStyle);
		moveFeature.isDragable = true;
		moveFeature.isDragHandle = true;
		frameFeatures.push(moveFeature);

		// 1 -- 2
		// |    |
		// 4 -- 3
		var moveCornerPointList = [];
		moveCornerPointList.push(frameTopLeft.clone());
		moveCornerPointList.push(new OpenLayers.Geometry.Point(frameTopLeft.x + frameWidthDegree, frameTopLeft.y));
		moveCornerPointList.push(new OpenLayers.Geometry.Point(frameTopLeft.x + frameWidthDegree, frameTopLeft.y - frameWidthDegree));
		moveCornerPointList.push(new OpenLayers.Geometry.Point(frameTopLeft.x, frameTopLeft.y - frameWidthDegree));
		// Close the polygon
		moveCornerPointList.push(moveCornerPointList[0]);
		var moveCorner = new OpenLayers.Geometry.LinearRing(moveCornerPointList);
		var moveCornerFeature = new OpenLayers.Feature.Vector(
			new OpenLayers.Geometry.Polygon(moveCorner),
			null, // attributes
			moveCornerStyle
		);
		moveCornerFeature.isDragable = true;
		moveCornerFeature.isDragHandle = true;
		frameFeatures.push(moveCornerFeature);

		this._resizePoint = frameBottomRight.clone();
		var resizeFeature = new OpenLayers.Feature.Vector(this._resizePoint, null, resizeStyle);
		resizeFeature.isDragable = true;
		resizeFeature.isResizeHandle = true;
		frameFeatures.push(resizeFeature);

		// 3 -- 4
		// |    |
		// 2 -- 1
		var resizeCornerPointList = [];
		resizeCornerPointList.push(frameBottomRight.clone());
		resizeCornerPointList.push(new OpenLayers.Geometry.Point(frameBottomRight.x - frameWidthDegree, frameBottomRight.y));
		resizeCornerPointList.push(new OpenLayers.Geometry.Point(frameBottomRight.x - frameWidthDegree, frameBottomRight.y + frameWidthDegree));
		resizeCornerPointList.push(new OpenLayers.Geometry.Point(frameBottomRight.x, frameBottomRight.y + frameWidthDegree));
		// Close the polygon
		resizeCornerPointList.push(resizeCornerPointList[0]);
		var resizeCorner = new OpenLayers.Geometry.LinearRing(resizeCornerPointList);
		this._resizeCornerFeature = new OpenLayers.Feature.Vector(
			new OpenLayers.Geometry.Polygon(resizeCorner),
			null, // attributes
			resizeCornerStyle
		);
		this._resizeCornerFeature.isDragable = true;
		this._resizeCornerFeature.isResizeHandle = true;
		frameFeatures.push(this._resizeCornerFeature);

		return frameFeatures;
	},


	/**
	 *                                  Coordinate line label
	 *                                 /
	 *                 3º E        6º E
	 *           .   .   |   .   .   |   .    } Coordinate lines - Horizontals
	 *         ----------------------------
	 *         |                          |
	 *  1º S --|                          |--
	 *         |                          |   \
	 *        -|                          |-   Coordinate lines
	 *         |                          |  \
	 *        -|     (Printed Frame)      |-  Coordinate Sub divisions
	 *         |                          |
	 *  4º S --|                          |--
	 *         |                          |
	 *        -|                          |-
	 *         |                          |
	 *         ----------------------------
	 *           '   '   |   '   '   |   '    } Coordinate lines - Horizontals
	 *
	 *
	 *       \/                            \/
	 *         Coordinate lines - Verticals
	 */
	_drawCoordLines: function() {
		var zIndex = 100;

		// The browser crash when there is too many lines. This
		// is a problem when zooming in with a print frame visible
		// somewhere on the map.
		// To fix this problem, this method will never return more than
		// "maxCoordLines" lines.
		var maxCoordLines = 1000;

		// Lines styles
		// See: http://dev.openlayers.org/docs/files/OpenLayers/Feature/Vector-js.html#OpenLayers.Feature.Vector.OpenLayers.Feature.Vector.style
		var coordLineStyle = {
			graphicZIndex: zIndex,
			fill: false,
			strokeColor: '#000000',
			fontColor: '#000000',
			fontSize: this._labelsFontSize + 'px',
			fontFamily: 'Verdana, Geneva, sans-serif',
			strokeWidth: this._strokeWidth
		};
		var coordSubDivisionLineStyle = {
			graphicZIndex: zIndex,
			fill: false,
			strokeColor: '#000000',
			strokeWidth: this._strokeWidth / 2
		};

		var bounds = this.getNativePrintedExtent();
		if (!bounds) {
			// The frame has not been drawn yet... This should not happen...
			return [];
		}
		var printedFrameBounds = this._deproject(bounds.clone());

		var subCoordLinesWidth = this._coordLinesWidth / 2;

		// List of features that has to be redraw according to the new position of frame
		var coordLinesFeatures = [];
		var subCoordLinesFeatures = [];
		var label;

		/**
		 * Coordinate lines - horizontals
		 */
		var topCoordLinesStep = this._calculateCoodLinesStep(this._topCoordLabelsDensity);
		var topSubCoordLinesStep = topCoordLinesStep["step"] / topCoordLinesStep["divisions"];

		var startx = Math.round(printedFrameBounds.left / topCoordLinesStep["step"]) * topCoordLinesStep["step"];
		while (startx - topCoordLinesStep["step"] >= printedFrameBounds.left) {
			startx = startx - topCoordLinesStep["step"];
		}
		while (startx < printedFrameBounds.left) {
			startx = startx + topCoordLinesStep["step"];
		}

		// First Sub divisions before the first large line
		var startSubx = startx;
		while (startSubx - topSubCoordLinesStep >= printedFrameBounds.left) {
			startSubx = startSubx - topSubCoordLinesStep;
		}
		for (var subx = startSubx; coordLinesFeatures.length + subCoordLinesFeatures.length < maxCoordLines && subx < startx && subx <= printedFrameBounds.right; subx += topSubCoordLinesStep) {
			subCoordLinesFeatures.push(this._newLine(subx, printedFrameBounds.top, subCoordLinesWidth, 'north', coordSubDivisionLineStyle));
			subCoordLinesFeatures.push(this._newLine(subx, printedFrameBounds.bottom, subCoordLinesWidth, 'south', coordSubDivisionLineStyle));
		}

		for (var x = startx; coordLinesFeatures.length < maxCoordLines && x <= printedFrameBounds.right; x += topCoordLinesStep["step"]) {
			label = this._getLabel(x, 'horizontal');
			coordLinesFeatures.push(this._newLine(x, printedFrameBounds.top, this._coordLinesWidth, 'north', coordLineStyle, this._labelsFontSize / 3 * 2, label));
			coordLinesFeatures.push(this._newLine(x, printedFrameBounds.bottom, this._coordLinesWidth, 'south', coordLineStyle));
			// Sub divisions
			for (var subx = topSubCoordLinesStep; coordLinesFeatures.length + subCoordLinesFeatures.length < maxCoordLines && subx < topCoordLinesStep["step"] && x + subx <= printedFrameBounds.right; subx += topSubCoordLinesStep) {
				subCoordLinesFeatures.push(this._newLine(x + subx, printedFrameBounds.top, subCoordLinesWidth, 'north', coordSubDivisionLineStyle));
				subCoordLinesFeatures.push(this._newLine(x + subx, printedFrameBounds.bottom, subCoordLinesWidth, 'south', coordSubDivisionLineStyle));
			}
		}

		/**
		 * Coordinate lines - verticals
		 */
		var leftCoordLinesStep = this._calculateCoodLinesStep(this._leftCoordLabelsDensity);
		var leftSubCoordLinesStep = leftCoordLinesStep["step"] / leftCoordLinesStep["divisions"];

		var starty = Math.round(printedFrameBounds.bottom / leftCoordLinesStep["step"]) * leftCoordLinesStep["step"];
		while (starty - leftCoordLinesStep["step"] >= printedFrameBounds.bottom) {
			starty = starty - leftCoordLinesStep["step"];
		}
		while (starty < printedFrameBounds.bottom) {
			starty = starty + leftCoordLinesStep["step"];
		}

		// First Sub divisions before the first large line
		var startSuby = starty;
		while (startSuby - leftSubCoordLinesStep >= printedFrameBounds.bottom) {
			startSuby = startSuby - leftSubCoordLinesStep;
		}
		for (var suby = startSuby; coordLinesFeatures.length + subCoordLinesFeatures.length < maxCoordLines && suby < starty && suby <= printedFrameBounds.top; suby += leftSubCoordLinesStep) {
			subCoordLinesFeatures.push(this._newLine(printedFrameBounds.left, suby, subCoordLinesWidth, 'west', coordSubDivisionLineStyle));
			subCoordLinesFeatures.push(this._newLine(printedFrameBounds.right, suby, subCoordLinesWidth, 'east', coordSubDivisionLineStyle));
		}

		for (var y = starty; coordLinesFeatures.length < maxCoordLines && y <= printedFrameBounds.top; y += leftCoordLinesStep["step"]) {
			label = this._getLabel(y, 'vertical');
			coordLinesFeatures.push(this._newLine(printedFrameBounds.left, y, this._coordLinesWidth, 'west', coordLineStyle, 10, label));
			coordLinesFeatures.push(this._newLine(printedFrameBounds.right, y, this._coordLinesWidth, 'east', coordLineStyle, 10));
			// Sub divisions
			for (var suby = leftSubCoordLinesStep; coordLinesFeatures.length + subCoordLinesFeatures.length < maxCoordLines && suby < leftCoordLinesStep["step"] && y + suby <= printedFrameBounds.top; suby += leftSubCoordLinesStep) {
				subCoordLinesFeatures.push(this._newLine(printedFrameBounds.left, y + suby, subCoordLinesWidth, 'west', coordSubDivisionLineStyle));
				subCoordLinesFeatures.push(this._newLine(printedFrameBounds.right, y + suby, subCoordLinesWidth, 'east', coordSubDivisionLineStyle));
			}
		}


		// There is too many lines... Do not return them
		if (coordLinesFeatures.length > maxCoordLines) {
			return [];
		}

		// Return only the main coordinate lines (not the sub division lines)
		if (coordLinesFeatures.length + subCoordLinesFeatures.length > maxCoordLines) {
			return coordLinesFeatures;
		}

		// Return all lines
		var allLines = subCoordLinesFeatures;
		for (var i=0; i<coordLinesFeatures.length; i++) {
			allLines.push(coordLinesFeatures[i]);
		}

		return allLines;
	},

	// Calculate the amount of space between each labels (in degree)
	// and the amount of intermediate lines between each labels.
	_calculateCoodLinesStep: function(coordLabelsDensity) {
		// Step scaled to the resolution (bigger resolution = bigger font = less place between steps)
		var MAX_STEP_PIXELS = 600 * this._dpiRatio,
			MIN_STEP_PIXELS = 100 * this._dpiRatio;

		var coordLinesStep = 1; // 1 deg
		var nbSubDivisions = null;

		// Length of 1 deg, in pixels
		var lengthPixels = this._oneDegreeLength || 1;

		if (lengthPixels < MIN_STEP_PIXELS) {
			nbSubDivisions = 1;
			// Upscale - Add more degrees per coordinate line
			while (lengthPixels < MIN_STEP_PIXELS) {
				if (coordLinesStep == 1) {
					lengthPixels *= 2;
					coordLinesStep *= 2;
					nbSubDivisions = 2;
				} else if (coordLinesStep == 2) {
					lengthPixels *= 2.5;
					coordLinesStep *= 2.5;
					nbSubDivisions = 5;
				} else if (coordLinesStep == 5) {
					lengthPixels *= 2;
					coordLinesStep *= 2;
					nbSubDivisions = 10;
				} else {
					lengthPixels *= 2;
					coordLinesStep *= 2;
					nbSubDivisions = 2;
				}
			}
		} else {
			nbSubDivisions = 10;
			// Downscale - Add more divisions between degrees
			while (lengthPixels > MAX_STEP_PIXELS) {
				if (coordLinesStep == 1) {
					nbSubDivisions = 5;
					lengthPixels /= 2;
					coordLinesStep /= 2;
				} else if (coordLinesStep == 0.5) {
					nbSubDivisions = 10;
					lengthPixels /= 5;
					coordLinesStep /= 5;
				} else {
					nbSubDivisions = 10;
					lengthPixels /= 10;
					coordLinesStep /= 10;
				}
			}
		}

		coordLinesStep /= coordLabelsDensity;
		nbSubDivisions = Math.floor(nbSubDivisions / coordLabelsDensity);

		return {
			"step": coordLinesStep,
			"divisions": nbSubDivisions
		};
	},

	/**
	 * This method is doing the same as the OpenLayers.Control.ScaleLine,
	 * but using vector instead of HTML elements.
	 * I'm not re-inventing the wheel, I copied their methods
	 * to do the calculation.
	 *
	 *
	 * |          |  Bottom Left corner of the printed frame
	 * |          |/
	 * |          *--------------------
	 * |                                 }  Scale offset
	 * |          |  10 km  |            <- Top unit        \
	 * |         (X)--------------       <- (X) = Anchor     > Height
	 * |          |    10 mi     |       <- Bottom unit     / 
	 * |
	 * --------------------------------
	 *
	 *            \
	 *             Start line (the left edge of the scale line)
	 *
	 *            \________ ________/
	 *                     V
	 *                 Max Width
	 *
	 * NOTE: The initial anchor location can be specified using: frameOptions.scaleLineLocation
	 */
	_drawScaleLine: function() {
		var that = this;
		var zIndex = 150;
		// Constants for better readability
		var X = 0, Y = 1;

		// Customizable values
		var height = 30 * this._dpiRatio;
		var maxWidth = 120 * this._dpiRatio;

		// Method copied from OpenLayers
		var widthAndLabels = this._getScaleLineWidthAndLabels(maxWidth);

		// Scale line styles
		// See: http://dev.openlayers.org/docs/files/OpenLayers/Feature/Vector-js.html#OpenLayers.Feature.Vector.OpenLayers.Feature.Vector.style
		var scaleLineStyle = {
			graphicZIndex: zIndex,
			fill: false,
			strokeColor: '#000000',
			strokeWidth: this._strokeWidth
		};
		var scaleLineLabelStyle = {
			graphicZIndex: zIndex,
			fill: false,
			stroke: false,
			fontColor: '#000000',
			labelOutlineColor: '#FFFFFF',
			labelOutlineWidth: 2 * this._dpiRatio,
			fontSize: this._scaleFontSize + 'px',
			fontFamily: 'Verdana, Geneva, sans-serif'
		};

		// Transparent
		var bboxScaleStyle = {
			graphicZIndex: zIndex+1,
			fillOpacity: 0, // "fill: false" tells the browser to not render the shape. I need the shape to enable the dragging.
			stroke: false
		};

		var res = this.map.getResolution();

		// Live reference to the bounds
		var printedFrameBounds = this.getNativePrintedExtent();
		if (!printedFrameBounds) {
			// The frame has not been drawn yet... This should not happen...
			return [];
		}

		// Calculate the initial location of the north arrow
		if (this.scaleLineLocation === null) {
			var scaleLineOffset = this.frameOptions.scaleLineOffset || [];
			scaleLineOffset = [
				(scaleLineOffset[X] || 0) * this._dpiRatio * res,
				(scaleLineOffset[Y] || 50) * this._dpiRatio * res
			];
			this.scaleLineLocation = [printedFrameBounds.left + scaleLineOffset[X], printedFrameBounds.bottom - scaleLineOffset[Y]];
		}

		this.scaleLineAnchor = new OpenLayers.Geometry.Point(this.scaleLineLocation[X], this.scaleLineLocation[Y]);

		var topLeft = new OpenLayers.Geometry.Point(this.scaleLineLocation[X], this.scaleLineLocation[Y] + height/2 * res);
		var middleLeft = new OpenLayers.Geometry.Point(this.scaleLineLocation[X], this.scaleLineLocation[Y]);
		var bottomLeft = new OpenLayers.Geometry.Point(this.scaleLineLocation[X], this.scaleLineLocation[Y] - height/2 * res);

		// IMPORTANT: Those value must stay as they are, they have been
		//     calculated by OpenLayers to be pixel accurate.
		//     DO NOT MULTIPLY THEM BY THE DPI RATIO!
		var topUnitWidth = widthAndLabels.top.width;
		var bottomUnitWidth = widthAndLabels.bottom.width;
		var largestUnitWidth = bottomUnitWidth > topUnitWidth ? bottomUnitWidth : topUnitWidth;

		var topUnitTopRight = new OpenLayers.Geometry.Point(this.scaleLineLocation[X] + topUnitWidth*res, topLeft.y);
		var topUnitBottomRight = new OpenLayers.Geometry.Point(this.scaleLineLocation[X] + topUnitWidth*res, middleLeft.y);
		var topUnitLabelLocation = new OpenLayers.Geometry.Point(this.scaleLineLocation[X] + (topUnitWidth/2)*res, middleLeft.y + height/4*res);

		var bottomUnitTopRight = new OpenLayers.Geometry.Point(this.scaleLineLocation[X] + bottomUnitWidth*res, middleLeft.y);
		var bottomUnitBottomRight = new OpenLayers.Geometry.Point(this.scaleLineLocation[X] + bottomUnitWidth*res, bottomLeft.y);
		var bottomUnitLabelLocation = new OpenLayers.Geometry.Point(this.scaleLineLocation[X] + (bottomUnitWidth/2)*res, middleLeft.y - height/4*res);

		var middleLineRight = new OpenLayers.Geometry.Point(this.scaleLineLocation[X] + largestUnitWidth*res, middleLeft.y);

		// create a polygon feature for the white triangle inside the arrow
		var bboxScalePointList = [];
		bboxScalePointList.push(new OpenLayers.Geometry.Point(topLeft.x, topLeft.y));
		bboxScalePointList.push(new OpenLayers.Geometry.Point(middleLineRight.x, topLeft.y));
		bboxScalePointList.push(new OpenLayers.Geometry.Point(middleLineRight.x, bottomLeft.y));
		bboxScalePointList.push(new OpenLayers.Geometry.Point(topLeft.x, bottomLeft.y));
		// Close the polygon
		bboxScalePointList.push(bboxScalePointList[0]);
		var bboxScaleRing = new OpenLayers.Geometry.LinearRing(bboxScalePointList);

		// create the middle line
		var middleLine = new OpenLayers.Feature.Vector(
			new OpenLayers.Geometry.LineString([middleLeft, middleLineRight]),
			null, // attributes
			scaleLineStyle
		);
		// create the start line
		var startLine = new OpenLayers.Feature.Vector(
			new OpenLayers.Geometry.LineString([topLeft, bottomLeft]),
			null, // attributes
			scaleLineStyle
		);
		// create the top unit line
		var topUnitLine = new OpenLayers.Feature.Vector(
			new OpenLayers.Geometry.LineString([topUnitTopRight, topUnitBottomRight]),
			null, // attributes
			scaleLineStyle
		);
		// create the bottom unit line
		var bottomUnitLine = new OpenLayers.Feature.Vector(
			new OpenLayers.Geometry.LineString([bottomUnitTopRight, bottomUnitBottomRight]),
			null, // attributes
			scaleLineStyle
		);

		// Labels
		// Clone the style object
		var topUnitScaleLineLabelStyle = OpenLayers.Util.extend({}, scaleLineLabelStyle);
		topUnitScaleLineLabelStyle.label = widthAndLabels.top.label;
		var topUnitLabel = new OpenLayers.Feature.Vector(
			topUnitLabelLocation,
			null,
			topUnitScaleLineLabelStyle
		);

		// Clone the style object
		var bottomUnitScaleLineLabelStyle = OpenLayers.Util.extend({}, scaleLineLabelStyle);
		bottomUnitScaleLineLabelStyle.label = widthAndLabels.bottom.label;
		var bottomUnitLabel = new OpenLayers.Feature.Vector(
			bottomUnitLabelLocation,
			null,
			bottomUnitScaleLineLabelStyle
		);

		this.bboxScale = new OpenLayers.Feature.Vector(
			new OpenLayers.Geometry.Polygon([bboxScaleRing]),
			null, // attributes
			bboxScaleStyle
		);
		this.bboxScale.isDragable = true;
		this.bboxScale.isScaleHandle = true;

		var scaleFeatures = [middleLine, startLine, topUnitLine, bottomUnitLine, topUnitLabel, bottomUnitLabel, this.bboxScale, this.scaleLineAnchor];

		// Disable the drag control when the element is deleted
		function beforeFeatureRemoved(e) {
			var feature = e.feature;
			if (feature === that.bboxScale) {
				// Get a fresh copy of the bounds
				printedFrameBounds = that.getNativePrintedExtent();

				// Recalculate the scale line location (centre)
				that._updateScaleLineLocation();

				// Remove controls and event listeners
				that.events.un({
					beforefeatureremoved: beforeFeatureRemoved
				})
			}
		}
		this.events.on({
			beforefeatureremoved: beforeFeatureRemoved
		});

		return scaleFeatures;
	},

	// Recalculate the scale line location (centre)
	_updateScaleLineLocation: function() {
		var res = this.map.getResolution();
		var height = 30 * this._dpiRatio;

		this.scaleLineLocation = [
			this.bboxScale.geometry.bounds.left,
			this.bboxScale.geometry.bounds.top - (height/2 * res)
		];
	},

	/**
	 * The arrow is a black triangle shape, with a white triangle for
	 * the left half of the arrow. The white triangle is smaller,
	 * giving the impression that the arrow has a black border.
	 * Its position is relative to the Top Left corner of the
	 * printed frame.
	 * 
	 * There is a simple representation of the initial position of the
	 * North arrow, with no offset.
	 *
	 * ----------------------
	 * |
	 * |         Top Left corner of the printed frame.
	 * |        /
	 * |       *-----|----------
	 * |       |    / \          \
	 * |       |   //| \          |
	 * |       |  // |  \         | Arrow Height
	 * |       | // (X)  \        | <- (X) = Anchor
	 * |       |//___|    \       | 
	 * |       /___________\     /
	 * |       |                 }  Label offset
	 * |       |  NN   NN        \
	 * |       |  NNNN NN         | Label
	 * |       |  NN NNNN         |
	 * |       |  NN   NN        /
	 * |       |
	 *
	 *         \_____ _____/
	 *               V
	 *               Arrow Width
	 *
	 * NOTE: The initial anchor location can be specified using: frameOptions.northArrowLocation
	 */
	_drawNorthArrow: function() {
		var that = this;
		var zIndex = 200;
		// Constants for better readability
		var X = 0, Y = 1;

		// Customizable values
		var arrowWidthPixels = 15 * this._dpiRatio;
		var arrowHeightPixels = 30 * this._dpiRatio;
		var arrowDitchPixels = 2 * this._dpiRatio;
		var labelOffsetPixels = 7.5 * this._dpiRatio;
		var labelFontSize = 15 * this._dpiRatio;

		// The line thickness is the approximate space (in pixels) between
		// the white triangle and the limit of the black arrow, giving
		// the impression of a black line.
		var lineThicknessPixels = 3 * this._dpiRatio;

		// Arrow styles
		// See: http://dev.openlayers.org/docs/files/OpenLayers/Feature/Vector-js.html#OpenLayers.Feature.Vector.OpenLayers.Feature.Vector.style
		var arrowStyle = {
			graphicZIndex: zIndex,
			fillColor: '#000000',
			fillOpacity: 1,
			stroke: false
		};
		var whiteArrowStyle = {
			graphicZIndex: zIndex+1,
			fillColor: '#FFFFFF',
			fillOpacity: 1,
			stroke: false
		};
		var arrowLabelStyle = {
			graphicZIndex: zIndex,
			label: 'N',
			labelOutlineColor: '#FFFFFF',
			labelOutlineWidth: 2 * this._dpiRatio,
			fontColor: '#000000',
			fontWeight: 'bold',
			fontSize: labelFontSize + 'px',
			fontFamily: 'Verdana, Geneva, sans-serif',
			fill: false,
			stroke: false
		};
		// Transparent
		var bboxArrowStyle = {
			graphicZIndex: zIndex+2,
			//fillColor: '#FFFFFF',
			//fillOpacity: 0.5,
			fillOpacity: 0, // "fill: false" tells the browser to not render the shape. I need the shape to enable the dragging.
			stroke: false
		};

		// Automatically calculated values, according to the parameters (in unit of the map; degree, meter, etc.)
		var res = this.map.getResolution();

		var printedFrameBounds = this.getNativePrintedExtent();
		if (!printedFrameBounds) {
			// The frame has not been drawn yet... This should not happen...
			return [];
		}

		// Calculate the location of the north arrow
		if (this.northArrowLocation === null) {
			var northArrowOffset = this.frameOptions.northArrowOffset || [];
			northArrowOffset = [
				(northArrowOffset[X] || 20) * this._dpiRatio * res,
				(northArrowOffset[Y] || 30) * this._dpiRatio * res
			];
			this.northArrowLocation = [printedFrameBounds.left + northArrowOffset[X], printedFrameBounds.top - northArrowOffset[Y]];
		}

		this.northArrowAnchor = new OpenLayers.Geometry.Point(this.northArrowLocation[X], this.northArrowLocation[Y]);

		var arrowTopLeft = [
			this.northArrowLocation[X] - (arrowWidthPixels/2 * res),
			this.northArrowLocation[Y] + (arrowHeightPixels/2 * res)
		];

		var arrowTop = new OpenLayers.Geometry.Point(
			arrowTopLeft[X] + (arrowWidthPixels/2)*res,
			arrowTopLeft[Y]
		);
		var arrowBottomLeft = new OpenLayers.Geometry.Point(
			arrowTopLeft[X],
			arrowTopLeft[Y] - arrowHeightPixels*res
		);
		var arrowBottomMiddle = new OpenLayers.Geometry.Point(
			arrowTopLeft[X] + (arrowWidthPixels/2)*res,
			arrowTopLeft[Y] - (arrowHeightPixels - arrowDitchPixels)*res
		);
		var arrowBottomRight = new OpenLayers.Geometry.Point(
			arrowTopLeft[X] + (arrowWidthPixels)*res,
			arrowTopLeft[Y] - arrowHeightPixels*res
		);

		var whiteArrowTop = new OpenLayers.Geometry.Point(
			arrowTopLeft[X] + (arrowWidthPixels/2)*res,
			// "arrowHeightPixels/arrowWidthPixels * 1.5" is not accurate trigonometry, but it's much faster and it doesn't have to be more accurate.
			arrowTopLeft[Y] - (lineThicknessPixels * arrowHeightPixels/arrowWidthPixels * 1.5)*res
		);
		var whiteArrowBottomLeft = new OpenLayers.Geometry.Point(
			arrowTopLeft[X] + (lineThicknessPixels)*res,
			arrowTopLeft[Y] - (arrowHeightPixels - lineThicknessPixels)*res
		);
		var whiteArrowBottomMiddle = new OpenLayers.Geometry.Point(
			arrowTopLeft[X] + (arrowWidthPixels/2)*res,
			arrowTopLeft[Y] - (arrowHeightPixels - arrowDitchPixels - lineThicknessPixels)*res
		);

		var labelLocation = new OpenLayers.Geometry.Point(
			arrowTopLeft[X] + (arrowWidthPixels/2)*res,
			arrowTopLeft[Y] - (arrowHeightPixels + labelOffsetPixels)*res
		);

		// create a polygon feature for the black triangle of the arrow
		var arrowPointList = [arrowTop, arrowBottomLeft, arrowBottomMiddle, arrowBottomRight, arrowTop];
		var arrowRing = new OpenLayers.Geometry.LinearRing(arrowPointList);

		// create a polygon feature for the white triangle inside the arrow
		var whiteArrowPointList = [whiteArrowTop, whiteArrowBottomLeft, whiteArrowBottomMiddle, whiteArrowTop];
		var whiteArrowRing = new OpenLayers.Geometry.LinearRing(whiteArrowPointList);

		// create a polygon feature for the white triangle inside the arrow
		var bboxArrowPointList = [];
		bboxArrowPointList.push(new OpenLayers.Geometry.Point(arrowBottomLeft.x, arrowTop.y));
		bboxArrowPointList.push(new OpenLayers.Geometry.Point(arrowBottomLeft.x, labelLocation.y - (labelFontSize/2 * res)));
		bboxArrowPointList.push(new OpenLayers.Geometry.Point(arrowBottomRight.x, labelLocation.y - (labelFontSize/2 * res)));
		bboxArrowPointList.push(new OpenLayers.Geometry.Point(arrowBottomRight.x, arrowTop.y));
		// Close the polygon
		bboxArrowPointList.push(bboxArrowPointList[0]);
		var bboxArrowRing = new OpenLayers.Geometry.LinearRing(bboxArrowPointList);

		var arrow = new OpenLayers.Feature.Vector(
			new OpenLayers.Geometry.Polygon([arrowRing]),
			null, // attributes
			arrowStyle
		);
		var whiteArrow = new OpenLayers.Feature.Vector(
			new OpenLayers.Geometry.Polygon([whiteArrowRing]),
			null, // attributes
			whiteArrowStyle
		);
		var label = new OpenLayers.Feature.Vector(
			labelLocation,
			null, // attributes
			arrowLabelStyle
		);

		this.bboxArrow = new OpenLayers.Feature.Vector(
			new OpenLayers.Geometry.Polygon([bboxArrowRing]),
			null, // attributes
			bboxArrowStyle
		);
		this.bboxArrow.isDragable = true;
		this.bboxArrow.isNorthArrowHandle = true;

		var arrowFeatures = [arrow, whiteArrow, label, this.bboxArrow, this.northArrowAnchor];

		// Disable the drag control when the element is deleted
		function beforeFeatureRemoved(e) {
			var feature = e.feature;
			if (feature === that.bboxArrow) {
				// Get a fresh copy of the bounds
				printedFrameBounds = that.getNativePrintedExtent();

				// Recalculate the arrow location (centre)
				that._updateNorthArrowLocation();

				// Remove controls and event listeners
				that.events.un({
					beforefeatureremoved: beforeFeatureRemoved
				})
			}
		}
		this.events.on({
			beforefeatureremoved: beforeFeatureRemoved
		});

		return arrowFeatures;
	},

	// Recalculate the arrow location (centre)
	_updateNorthArrowLocation: function() {
		var res = this.map.getResolution();
		var arrowWidthPixels = 15 * this._dpiRatio;
		var arrowHeightPixels = 30 * this._dpiRatio;

		this.northArrowLocation = [
			this.bboxArrow.geometry.bounds.left + (arrowWidthPixels/2 * res),
			this.bboxArrow.geometry.bounds.top - (arrowHeightPixels/2 * res)
		];
	},

	_onZoomChange: function() {
		var res = this.map.getResolution();

		// Recalculate the length of one degree
		this._oneDegreeLength = (this._reproject(new OpenLayers.Geometry.Point(1, 0)).x - this._reproject(new OpenLayers.Geometry.Point(0, 0)).x) / res;
		if (this._oneDegreeLength < 1) { this._oneDegreeLength = 1; }

		this._updateWidgetsPosition();
	},


	// Unchanged copy of OpenLayers.Control.ScaleLine.getBarLen
	// NOTE: I could simply call OpenLayers.Control.ScaleLine.prototype.getBarLen(maxLen),
	//     but that may won't work after the next OpenLayers update.
	_getBarLen: function(maxLen) {
		// nearest power of 10 lower than maxLen
		var digits = parseInt(Math.log(maxLen) / Math.log(10));
		var pow10 = Math.pow(10, digits);

		// ok, find first character
		var firstChar = parseInt(maxLen / pow10);

		// right, put it into the correct bracket
		var barLen;
		if(firstChar > 5) {
			barLen = 5;
		} else if(firstChar > 2) {
			barLen = 2;
		} else {
			barLen = 1;
		}

		// scale it up the correct power of 10
		return barLen * pow10;
	},
	// Copy of OpenLayers.Control.ScaleLine.update,
	// slightly modified to remove class dependency
	// and return the result instead of setting it
	// into the dom tree elements.
	_getScaleLineWidthAndLabels: function(maxWidth) {
		/* MODIFIED TO REMOVE ScaleLine CLASS DEPENDENCY */
		var geodesic = true; // This class only support geodesic scale line
		// Those values are not configurable with this class
		var topOutUnits = "km";
		var topInUnits = "m";
		var bottomOutUnits = "mi";
		var bottomInUnits = "ft";
		/* END OF - MODIFIED TO REMOVE ScaleLine CLASS DEPENDENCY */

		var res = this.map.getResolution();
		if (!res) {
			return;
		}

		var curMapUnits = this.map.getUnits();
		var inches = OpenLayers.INCHES_PER_UNIT;

		// convert maxWidth to map units
		var maxSizeData = maxWidth * res * inches[curMapUnits];
		var geodesicRatio = 1;
		if(geodesic === true) {
			var maxSizeGeodesic = (this.map.getGeodesicPixelSize().w ||
				0.000001) * maxWidth;
			var maxSizeKilometers = maxSizeData / inches["km"];
			geodesicRatio = maxSizeGeodesic / maxSizeKilometers;
			maxSizeData *= geodesicRatio;
		}

		// decide whether to use large or small scale units
		var topUnits;
		var bottomUnits;
		if(maxSizeData > 100000) {
			topUnits = topOutUnits;
			bottomUnits = bottomOutUnits;
		} else {
			topUnits = topInUnits;
			bottomUnits = bottomInUnits;
		}

		// and to map units units
		var topMax = maxSizeData / inches[topUnits];
		var bottomMax = maxSizeData / inches[bottomUnits];

		// now trim this down to useful block length
		var topRounded = this._getBarLen(topMax);
		var bottomRounded = this._getBarLen(bottomMax);

		// and back to display units
		topMax = topRounded / inches[curMapUnits] * inches[topUnits];
		bottomMax = bottomRounded / inches[curMapUnits] * inches[bottomUnits];

		// and to pixel units
		var topPx = topMax / res / geodesicRatio;
		var bottomPx = bottomMax / res / geodesicRatio;

		// now set the pixel widths
		// and the values inside them
		
		/* MODIFIED TO REMOVE DOM TREE DEPENDENCY */
		return {
			top: {
				width: topPx,
				label: topRounded + " " + topUnits
			},
			bottom: {
				width: bottomPx,
				label: bottomRounded + " " + bottomUnits
			}
		};
		/* END OF - MODIFIED TO REMOVE DOM TREE DEPENDENCY */
	},



	_correctLongitude: function(lon) {
		// Correction: Dragging/Resizing the print frame pass the date line will increase / decrease the value over the limit...
		while (lon > 180) { lon -= 360; }
		while (lon < -180) { lon += 360; }
		return lon;
	},

	// orientation: 'vertical' (N/S) or 'horizontal' (E/W)
	_getLabel: function(value, orientation) {
		var epsilon = 0.0000000000001;

		if (orientation === 'horizontal') {
			value = this._correctLongitude(value);
		}

		var label = (value < 0 ? (value*-1) : value).toFixed(6).replace(/00*$/, '').replace(/\.\.*$/, '');
		if (orientation === 'horizontal') {
			label += (value + epsilon < 0 ? 'º W' : 'º E');
		} else {
			label += (value + epsilon < 0 ? 'º S' : 'º N');
		}

		return label;
	},


	// direction: 'north', 'south', 'east' or 'west'
	// private
	_newLine: function(x, y, lengthPixels, direction, style, offset, label) {
		var point1 = this._reproject(new OpenLayers.Geometry.Point(x, y));
		var point2 = point1.clone();

		var res = this.map.getResolution();
		var length = res * lengthPixels;

		var labelAlign = 'cm'; // center - middle
		var labelXOffset = 0;
		var labelYOffset = 0;

		switch(direction) {
			case 'north':
				point2.y += length;
				labelYOffset = offset;
				break;
			case 'south':
				point2.y -= length;
				labelYOffset = (offset * -1);
				break;
			case 'east':
				point2.x += length;
				labelXOffset = offset;
				labelAlign = 'lm'; // left - middle
				break;
			case 'west':
			default:
				point2.x -= length;
				labelXOffset = (offset * -1);
				labelAlign = 'rm'; // right - middle
				break;
		}

		if (label != null) {
			// Clone the style object
			style = OpenLayers.Util.extend({}, style);
			style.label = label;
			style.labelAlign = labelAlign;
			style.labelXOffset = labelXOffset;
			style.labelYOffset = labelYOffset;
		}

		return new OpenLayers.Feature.Vector(
			new OpenLayers.Geometry.LineString([point2, point1]),
			null, // attributes
			style
		);
	},

	// Reproject a geometry obj to be place in a layer.
	// - Reproject from lon/lat to map projection
	// private
	_reproject: function(geometry) {
		if (this.map.projection != this.defaultLonLatProjection) {
			return geometry.transform(this.defaultLonLatProjection, this.map.projection);
		}
		return geometry;
	},

	// Undo a reprojection.
	// - Reproject from map projection to lon/lat
	// private
	_deproject: function(geometry) {
		if (this.map.projection != this.defaultLonLatProjection) {
			return geometry.transform(this.map.projection, this.defaultLonLatProjection);
		}
		return geometry;
	}
});
