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

/**
 * Configuration options:
 * topLeft: Print frame top-left coordinates (2d array of double; [x, y]). Default: if not specified, the layer will ask the user to draw a rectangle on the map.
 * bottomRight: Print frame bottom-right coordinates (2d array of double; [x, y]). Default: if not specified, the layer will ask the user to draw a rectangle on the map.
 * dpi: Desired printed DPI, preferably matching a zoom level (90 * 2^x => for example: 90, 180, 360); everything is scaled up. Default: 90.
 *     Example:
 *         * 90: default resolution; no diferences.
 *         * 180: double resolution; text, lines, patterns, etc. are twice bigger.
 *         * 360: 4x resolution; text, lines, patterns, etc. are 4 times bigger.
 * strokeWidth: Width of the coordinates lines, scale lines and print frame border, in pixels. Default: 2.
 * frameWidth: Width of the white border around the print frame, in pixels. Default: 100.
 * labelsFontSize: Font size of the labels for the coordinate lines, in pixels. Default: 12.
 * attributionsFontSize: Font size of the attributions line, at the bottom of the print frame, in pixels. Default: 10.
 * attributions: String (or function that return a String) used for the attributions. Default: no attributions.
 *     This value is updated everytime:
 *         * The event 'attributionsChange' is fired on the map
 *         * The print frame is resized / dragged (the attribution may change when some layers become out of scope, for the print frame)
 *         * The map is paned or zoomed
 *         * The map layers selection has changed (layer added, removed or changed)
 * coordLinesWidth: Length of the larger coordinate lines, in pixels. Default: 8.
 * scaleFontSize: Font size of the labels of the scale widget, in pixels. Default: 9.
 * controlsRadius: Size of the handle used to move / resize the print frame, in pixels. Default: 12.
 * northArrowOffset: Offset of the north arrow (2d array of double; [x, y]), from the top left corner of the printed frame, in pixels (negative values allowed). Default: [20, 30].
 * scaleLineOffset: Offset of the scale line (2d array of double; [x, y]), from the bottom left corner of the printed frame, in pixels (negative values allowed). Default: [0, 50].
 * 
 */
OpenLayers.Layer.ux = OpenLayers.Layer.ux || {};
OpenLayers.Layer.ux.PrintFrame = OpenLayers.Class(OpenLayers.Layer.Vector, {
	CLASS_NAME: "OpenLayers.Layer.ux.PrintFrame",


	// Default values
	DEFAULT_DPI: 90,
	strokeWidth: 2, // in pixel
	labelsFontSize: 12, // in pixel
	attributionsFontSize: 10, // in pixel
	scaleFontSize: 9, // in pixel (font size used by OpenLayers: 9px)
	frameWidth: 100, // in pixel
	coordLinesWidth: 8, // in pixel

	_dpiRatio: null, // >1 to increase the the strokes width, font size, etc.
	_strokeWidth: null,
	_labelsFontSize: null,
	_attributionsFontSize: null,
	_scaleFontSize: null,
	_frameWidth: null,
	_coordLinesWidth: null,

	// Offset of the north arrow / scale line, from the top left corner of the printed frame, in unit of the map (degree, meter, etc.).
	// (array of floats [X, Y])
	northArrowLocation: null,
	scaleLineLocation: null,
	attributionsFeature: null,

	// private
	_attributionsLabel: null,

	// private
	_oneDegreeLength: 0, // length of one degree, in pixel, for the current zoom level. Used to calculate how many coordinate line has to be drawn.

	defaultLonLatProjection: new OpenLayers.Projection('EPSG:4326'),

	// Reference to the inner frame, the printed area of the map, used to know where to draw the coordinate lines
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

	// private
	_initiated: false,
	_frameFeatures: null,
	_coordLinesFeatures: null,
	_northArrowFeatures: null,
	_scaleLineFeatures: null,

	initialize: function(name, options) {
		// enable the indexer by setting zIndexing to true
		options.rendererOptions = options.rendererOptions || {};
		options.rendererOptions.zIndexing = true;
		
		OpenLayers.Layer.Vector.prototype.initialize.apply(this, arguments);

		this.options = this.options || {};
		this._dpiRatio = (this.options.dpi || this.DEFAULT_DPI) / this.DEFAULT_DPI;
	},

	setDPI: function(dpi) {
		var newDPIRatio = dpi / this.DEFAULT_DPI;
		if (newDPIRatio != this._dpiRatio) {
			this._dpiRatio = newDPIRatio

			// Find the actual frame bounds, in lon/lat
			var printedFrameBounds = this._deproject(this.getPrintedExtent().clone());

			this._redraw(
				[printedFrameBounds.left, printedFrameBounds.top],
				[printedFrameBounds.right, printedFrameBounds.bottom]
			);
		}
	},

	getPrintedExtent: function() {
		if (this.printedFrame && this.printedFrame.geometry) {
			return this.printedFrame.geometry.bounds;
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
			var frameHoleTopLeft = this.options.topLeft;
			var frameHoleBottomRight = this.options.bottomRight;

			if (frameHoleTopLeft && frameHoleBottomRight) {
				// Draw a print frame according to the given values

				this._init(frameHoleTopLeft, frameHoleBottomRight);
			} else {
				// Let the user draw a rectangle on the map, and use
				// that rectangle as a base to create the print frame.
				// The user can press "ESC" at anytime during the drawing
				// process to cancel the addition of the print frame layer.

				var cursorBackup = null;
				if (this.map.div && this.map.div.style) {
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

				// Keyboard listener used to cancel the layer when the key ESC is pressed
				OpenLayers.Event.observe(document, "keydown", escListener);
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

		// true: Coordinate lines are updated while the frame moves around.
		// false: Coordinate lines are hidden while the frame moves around, and redrawn when it stops moving.
		// NOTE: This is always false for IE (all version of IE are way too slow to handle that feature)
		var coordLinesLiveUpdate = true;

		if (isIE) {
			coordLinesLiveUpdate = false;
		}

		// Resizable
		var resize = new OpenLayers.Control.DragFeature(this, {
			onStart: startResize,
			onDrag: doResize,
			onComplete: endResize
		});

		// Little hack to enable the drag handler only on specified features
		resize.handlers.feature.geometryTypeMatches = function(feature) {
			return feature.isResizeHandle === true;
		};

		this.map.addControl(resize);
		resize.activate();

		// Resize handle starting to move
		function startResize(feature, pixel) {
			if (!coordLinesLiveUpdate) {
				that.removeFeatures(that._coordLinesFeatures);
			}
			lastPixel = pixel;
		}

		// Resize handle moving
		function doResize(feature, pixel) {
			that.removeFeatures(that._frameFeatures);
			// The feature that is dragged get redrawn by open layers, and by this method. The ghost has to be deleted manually.
			that.removeFeatures([feature]);

			that._frameFeatures = that._resizeFrame(pixel);
			that.addFeatures(that._frameFeatures);

			if (coordLinesLiveUpdate) {
				that.removeFeatures(that._coordLinesFeatures);
				that._coordLinesFeatures = that._drawCoordLines();
				that.addFeatures(that._coordLinesFeatures);
			}
			lastPixel = pixel;
		}

		// Resize handle stopped moving
		function endResize(feature, pixel) {
			// "outFeature" is not automatically called when the resize handle is moved over the limit (frame resized to a size smaller than 0).
			this.outFeature(feature); // NOTE: outFeature is a method of the DragFeature
			// deResize delete the ghosts, but maybe some browsers will have issue with the last one...
			that.removeFeatures([feature]);

			if (!coordLinesLiveUpdate) {
				that._coordLinesFeatures = that._drawCoordLines();
				that.addFeatures(that._coordLinesFeatures);
			}
			that._updateAttributions();
			for (var i = 0; i < that._frameFeatures.length; i++) {
				that._frameFeatures[i].state = OpenLayers.State.UPDATE;
			}
		}

		// Dragable
		var drag = new OpenLayers.Control.DragFeature(this, {
			onStart: startDrag,
			onDrag: doDrag,
			onComplete: endDrag
		});

		// Little hack to enable the drag handler only on specified features
		drag.handlers.feature.geometryTypeMatches = function(feature) {
			return feature.isDragHandle === true;
		};

		this.map.addControl(drag);
		drag.activate();



		// Drag handle starting to move
		function startDrag(feature, pixel) {
			if (!coordLinesLiveUpdate) {
				that.removeFeatures(that._coordLinesFeatures);
			}
			lastPixel = pixel;
		}

		// Drag handle moving
		function doDrag(feature, pixel) {
			var res = that.map.getResolution();
			// Move the frame
			for (var i = 0; i < that._frameFeatures.length; i++) {
				if (feature != that._frameFeatures[i]) {
					that._frameFeatures[i].geometry.move(res * (pixel.x - lastPixel.x), res * (lastPixel.y - pixel.y));
					that.drawFeature(that._frameFeatures[i]);
				}
			}
			// Move the arrow
			for (var i = 0; i < that._scaleLineFeatures.length; i++) {
				that._scaleLineFeatures[i].geometry.move(res * (pixel.x - lastPixel.x), res * (lastPixel.y - pixel.y));
				that.drawFeature(that._scaleLineFeatures[i]);
			}
			// Move the scale line (this feature is refreshed after move)
			for (var i = 0; i < that._northArrowFeatures.length; i++) {
				that._northArrowFeatures[i].geometry.move(res * (pixel.x - lastPixel.x), res * (lastPixel.y - pixel.y));
				that.drawFeature(that._northArrowFeatures[i]);
			}

			if (coordLinesLiveUpdate) {
				// Redraw the coordinate lines
				that.removeFeatures(that._coordLinesFeatures);
				that._coordLinesFeatures = that._drawCoordLines();
				that.addFeatures(that._coordLinesFeatures);
			}
			lastPixel = pixel;
		}

		// Drag handle stopped moving
		function endDrag(feature, pixel) {
			if (!coordLinesLiveUpdate) {
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
		}

		// Register events
		this.events.on({
			moveend: function(e) {
				if (e.zoomChanged) {
					this._onZoomChange();

					// Find the actual frame bounds, in lon/lat
					var printedFrameBounds = this._deproject(this.getPrintedExtent().clone());

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

		this.map.events.on({
			addlayer: function(e) {
				// e.layer
				this._updateAttributions();
			},
			removelayer: function(e) {
				// e.layer
				this._updateAttributions();
			},
			changelayer: function(e) {
				// e.layer, e.property
				this._updateAttributions();
			},
			attributionsChange: function(e) {
				// e.layer, e.attributions
				this._updateAttributions();
			},
			scope: this
		});
	},

	// The method dans "redraw" is already defined in OpenLayers...
	_redraw: function(frameHoleTopLeft, frameHoleBottomRight) {
		this._strokeWidth = (this.options.strokeWidth || this.strokeWidth) * this._dpiRatio;
		this._frameWidth = (this.options.frameWidth || this.frameWidth) * this._dpiRatio;
		this._labelsFontSize = (this.options.labelsFontSize || this.labelsFontSize) * this._dpiRatio;
		this._attributionsFontSize = (this.options.attributionsFontSize || this.attributionsFontSize) * this._dpiRatio;
		this._coordLinesWidth = (this.options.coordLinesWidth || this.coordLinesWidth) * this._dpiRatio;
		this._scaleFontSize = (this.options.scaleFontSize || this.scaleFontSize) * this._dpiRatio;


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
		// NOTE: Images (logos) can be added using externalGraphic (style attribute) on a point. It won't be easy, but it's a solution...
		var attributionsStr = null;
		if (typeof(this.options.attributions) === 'string') {
			attributionsStr = this.options.attributions;
		} else if (typeof(this.options.attributions) === 'function') {
			attributionsStr = this.options.attributions();
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
		var printedFrameBounds = this._deproject(this.getPrintedExtent().clone());

		// "pixel" represent the bottom-right corner of the frame, in pixels. We need the bottom-right corner of the hole of the frame.
		var pixelHoleBottomRight = bottomRightPixel.clone();
		pixelHoleBottomRight.x -= this._frameWidth;
		pixelHoleBottomRight.y -= this._frameWidth; // NOTE: That operation would be an addition if the value were in lon/lat.
		var newBottomRight = this._deproject(this.map.getLonLatFromPixel(pixelHoleBottomRight));

		return this._drawFrame([printedFrameBounds.left, printedFrameBounds.top], [newBottomRight.lon, newBottomRight.lat]);
	},


	/**
	 *    Move Handle
	 *   /
	 * (+)-------------------------
	 *  |         (Frame)         |
	 *  |   -------------------   |
	 *  |   |                 |   |
	 *  |   |                 |<--|--- Frame border
	 *  |   | (Printed Frame) |   |
	 *  |   |                 |   |
	 *  |   |                 |   |
	 *  |   -------------------   |
	 *  |          Attributions   |
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
		var controlsRadius = (this.options.controlsRadius || 12) * this._dpiRatio; 

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
			graphicZIndex: zIndex+2,
			externalGraphic: "http://www.centos.org/docs/2/rhl-gsg-en-7.2/figs/gimp/move.gif",

			fillColor: '#FFFFFF',
			fillOpacity: 1,
			strokeColor: "#000000",
			strokeWidth: this._strokeWidth,
			pointRadius: controlsRadius
		};
		var resizeStyle = {
			graphicZIndex: zIndex+3,
			externalGraphic: "http://www.sylights.com/images/diagrams/resize-handle.png",

			fillColor: '#FFFFFF',
			fillOpacity: 1,
			strokeColor: "#000000",
			strokeWidth: this._strokeWidth,
			pointRadius: controlsRadius,
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
		//frame.isDragHandle = true;
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
		moveFeature.isDragHandle = true;
		frameFeatures.push(moveFeature);

		var resizePoint = frameBottomRight.clone();
		var resizeFeature = new OpenLayers.Feature.Vector(resizePoint, null, resizeStyle);
		resizeFeature.isResizeHandle = true;
		frameFeatures.push(resizeFeature);

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

		// Step scaled to the resolution (bigger resolution = bigger font = less place between steps)
		var MAX_STEP_PIXELS = 600 * this._dpiRatio,
			MIN_STEP_PIXELS = 100 * this._dpiRatio;

		var bounds = this.getPrintedExtent();
		if (!bounds) {
			// The frame has not been drawn yet... This should not happen...
			return [];
		}
		var printedFrameBounds = this._deproject(bounds.clone());

		var coordLinesStep = null; // This will be configurable in the future
		var nbSubDivisions = null; // This will be configurable in the future

		// If coordLinesStep is null, undefined, empty string, 0 (0 is an invalid step) or smaller than zero
		if (!coordLinesStep || coordLinesStep <= 0) {
			coordLinesStep = 1; // 1 deg

			// Length of 1 deg, in pixels
			var lengthPixels = this._oneDegreeLength || 1;

			if (lengthPixels < MIN_STEP_PIXELS) {
				var recommendedNbSubDivisions = 1;
				// Upscale - Add more degrees per coordinate line
				while (lengthPixels < MIN_STEP_PIXELS) {
					if (coordLinesStep == 1) {
						lengthPixels *= 2;
						coordLinesStep *= 2;
						recommendedNbSubDivisions = 2;
					} else if (coordLinesStep == 2) {
						lengthPixels *= 2.5;
						coordLinesStep *= 2.5;
						recommendedNbSubDivisions = 5;
					} else if (coordLinesStep == 5) {
						lengthPixels *= 2;
						coordLinesStep *= 2;
						recommendedNbSubDivisions = 10;
					} else {
						lengthPixels *= 2;
						coordLinesStep *= 2;
						recommendedNbSubDivisions = 2;
					}
				}

				if (!nbSubDivisions) {
					nbSubDivisions = recommendedNbSubDivisions;
				}
			} else {
				var recommendedNbSubDivisions = 10;
				// Downscale - Add more divisions between degrees
				while (lengthPixels > MAX_STEP_PIXELS) {
					if (coordLinesStep == 1) {
						recommendedNbSubDivisions = 5;
						lengthPixels /= 2;
						coordLinesStep /= 2;
					} else if (coordLinesStep == 0.5) {
						recommendedNbSubDivisions = 10;
						lengthPixels /= 5;
						coordLinesStep /= 5;
					} else {
						recommendedNbSubDivisions = 10;
						lengthPixels /= 10;
						coordLinesStep /= 10;
					}
				}

				if (!nbSubDivisions) {
					nbSubDivisions = recommendedNbSubDivisions;
				}
			}
		}

		var subCoordLinesStep = coordLinesStep / nbSubDivisions;

		var subCoordLinesWidth = this._coordLinesWidth / 2;

		/**
		 * Coordinate lines - horizontals
		 */

		var starty = Math.round(printedFrameBounds.bottom / coordLinesStep) * coordLinesStep;
		while (starty - coordLinesStep >= printedFrameBounds.bottom) {
			starty = starty - coordLinesStep;
		}
		while (starty < printedFrameBounds.bottom) {
			starty = starty + coordLinesStep;
		}

		// List of features that has to be redraw according to the new position of frame
		var coordLinesFeatures = [];
		var subCoordLinesFeatures = [];

		// First Sub divisions before the first large line
		var startSuby = starty;
		while (startSuby - subCoordLinesStep >= printedFrameBounds.bottom) {
			startSuby = startSuby - subCoordLinesStep;
		}
		for (var suby = startSuby; coordLinesFeatures.length + subCoordLinesFeatures.length < maxCoordLines && suby < starty && suby <= printedFrameBounds.top; suby += subCoordLinesStep) {
			subCoordLinesFeatures.push(this._newLine(printedFrameBounds.left, suby, subCoordLinesWidth, 'west', coordSubDivisionLineStyle));
			subCoordLinesFeatures.push(this._newLine(printedFrameBounds.right, suby, subCoordLinesWidth, 'east', coordSubDivisionLineStyle));
		}

		var label;
		for (var y = starty; coordLinesFeatures.length < maxCoordLines && y <= printedFrameBounds.top; y += coordLinesStep) {
			label = this._getLabel(y, 'horizontal');
			coordLinesFeatures.push(this._newLine(printedFrameBounds.left, y, this._coordLinesWidth, 'west', coordLineStyle, 10, label));
			coordLinesFeatures.push(this._newLine(printedFrameBounds.right, y, this._coordLinesWidth, 'east', coordLineStyle, 10));
			// Sub divisions
			for (var suby = subCoordLinesStep; coordLinesFeatures.length + subCoordLinesFeatures.length < maxCoordLines && suby < coordLinesStep && y + suby <= printedFrameBounds.top; suby += subCoordLinesStep) {
				subCoordLinesFeatures.push(this._newLine(printedFrameBounds.left, y + suby, subCoordLinesWidth, 'west', coordSubDivisionLineStyle));
				subCoordLinesFeatures.push(this._newLine(printedFrameBounds.right, y + suby, subCoordLinesWidth, 'east', coordSubDivisionLineStyle));
			}
		}

		/**
		 * Coord lines - verticals
		 */

		var startx = Math.round(printedFrameBounds.left / coordLinesStep) * coordLinesStep;
		while (startx - coordLinesStep >= printedFrameBounds.left) {
			startx = startx - coordLinesStep;
		}
		while (startx < printedFrameBounds.left) {
			startx = startx + coordLinesStep;
		}

		// First Sub divisions before the first large line
		var startSubx = startx;
		while (startSubx - subCoordLinesStep >= printedFrameBounds.left) {
			startSubx = startSubx - subCoordLinesStep;
		}
		for (var subx = startSubx; coordLinesFeatures.length + subCoordLinesFeatures.length < maxCoordLines && subx < startx && subx <= printedFrameBounds.right; subx += subCoordLinesStep) {
			subCoordLinesFeatures.push(this._newLine(subx, printedFrameBounds.top, subCoordLinesWidth, 'north', coordSubDivisionLineStyle));
			subCoordLinesFeatures.push(this._newLine(subx, printedFrameBounds.bottom, subCoordLinesWidth, 'south', coordSubDivisionLineStyle));
		}

		for (var x = startx; coordLinesFeatures.length < maxCoordLines && x <= printedFrameBounds.right; x += coordLinesStep) {
			label = this._getLabel(x, 'vertical');
			coordLinesFeatures.push(this._newLine(x, printedFrameBounds.top, this._coordLinesWidth, 'north', coordLineStyle, this._labelsFontSize / 3 * 2, label));
			coordLinesFeatures.push(this._newLine(x, printedFrameBounds.bottom, this._coordLinesWidth, 'south', coordLineStyle));
			// Sub divisions
			for (var subx = subCoordLinesStep; coordLinesFeatures.length + subCoordLinesFeatures.length < maxCoordLines && subx < coordLinesStep && x + subx <= printedFrameBounds.right; subx += subCoordLinesStep) {
				subCoordLinesFeatures.push(this._newLine(x + subx, printedFrameBounds.top, subCoordLinesWidth, 'north', coordSubDivisionLineStyle));
				subCoordLinesFeatures.push(this._newLine(x + subx, printedFrameBounds.bottom, subCoordLinesWidth, 'south', coordSubDivisionLineStyle));
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

	/**
	 * This method is doing the same as the OpenLayers.Control.ScaleLine,
	 * but using vector instead of HTML elements.
	 * I'm not re-inventing the wheel, I copied their methods
	 * to do the calculation.
	 *
	 *
	 * |        |  Bottom Left corner of the printed frame
	 * |        |/
	 * |        *---------------
	 * |                           }  Scale offset
	 * |        |_10_km_|_         <- Top unit        \
	 * |        |  10 mi  |        <- Bottom unit     / Height
	 * |
	 * -------------------------
	 *
	 *          \
	 *           Start line
	 *
	 *          \______ ______/
	 *                 V
	 *             Max Width
	 */
	_drawScaleLine: function() {
		var that = this;
		var zIndex = 150;
		// Constants for better readability
		var X = 0, Y = 1;

		// Customizable values
		var height = 30 * this._dpiRatio;
		var maxWidth = 100 * this._dpiRatio;

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
			//fillColor: '#000000',
			//fillOpacity: 0.2,
			fillOpacity: 0, // "fill: false" tells the browser to not render the shape. I need the shape to enable the dragging.
			stroke: false
		};

		var res = this.map.getResolution();

		// Live reference to the bounds
		var printedFrameBounds = this.getPrintedExtent();
		if (!printedFrameBounds) {
			// The frame has not been drawn yet... This should not happen...
			return [];
		}

		// Calculate the initial location of the north arrow
		if (this.scaleLineLocation === null) {
			var scaleLineOffset = this.options.scaleLineOffset || [];
			scaleLineOffset = [
				(scaleLineOffset[X] || 0) * this._dpiRatio * res,
				(scaleLineOffset[Y] || 50) * this._dpiRatio * res
			];
			this.scaleLineLocation = [printedFrameBounds.left + scaleLineOffset[X], printedFrameBounds.bottom - scaleLineOffset[Y]];
		}

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

		var bboxScale = new OpenLayers.Feature.Vector(
			new OpenLayers.Geometry.Polygon([bboxScaleRing]),
			null, // attributes
			bboxScaleStyle
		);
		bboxScale.isScaleHandle = true;

		var scaleFeatures = [middleLine, startLine, topUnitLine, bottomUnitLine, topUnitLabel, bottomUnitLabel, bboxScale];

		// Draggable
		var drag = new OpenLayers.Control.DragFeature(this, {
			onStart: startDrag,
			onDrag: doDrag,
			onComplete: endDrag
		});
		// Little hack to enable the drag handler only on specified features
		drag.handlers.feature.geometryTypeMatches = function(feature) {
			return feature.isScaleHandle === true;
		};
		this.map.addControl(drag);
		drag.activate();

		// Drag handle starting to move
		function startDrag(feature, pixel) {
			lastPixel = pixel;
		}

		// Drag handle moving
		function doDrag(feature, pixel) {
			for (var i = 0; i < scaleFeatures.length; i++) {
				if (feature != scaleFeatures[i]) {
					var res = that.map.getResolution();
					scaleFeatures[i].geometry.move(res * (pixel.x - lastPixel.x), res * (lastPixel.y - pixel.y));
					that.drawFeature(scaleFeatures[i]);
				}
			}

			lastPixel = pixel;
		}

		// Drag handle stopped moving
		function endDrag(feature, pixel) {
			for (var i = 0; i < scaleFeatures.length; i++) {
				scaleFeatures[i].state = OpenLayers.State.UPDATE;
			}
		}

		// Disable the drag control when the element is deleted
		function beforeFeatureRemoved(e) {
			var feature = e.feature;
			if (feature === bboxScale) {
				// Get a fresh copy of the bounds
				printedFrameBounds = that.getPrintedExtent();

				// Recalculate the arrow location (centre)
				that.scaleLineLocation = [
					bboxScale.geometry.bounds.left,
					bboxScale.geometry.bounds.top - (height/2 * res)
				];

				// Remove controls and event listeners
				drag.deactivate();
				drag.destroy();
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
	 * |       *---|----------
	 * |       |  / \       \
	 * |       | //| \       |
	 * |       |//_|  \      | Arrow Height
	 * |       /_______\    /
	 * |       |            }  Label offset
	 * |       | NN  N      \
	 * |       | N N N       > Label
	 * |       | N  NN      /
	 * |       |
	 *
	 *         \__ ___/
	 *            V
	 *            Arrow Width
	 */
	_drawNorthArrow: function() {
		var that = this;
		var zIndex = 200;
		// Constants for better readability
		var X = 0, Y = 1;

		// Customizable values
		var arrowWidthPixels = 20 * this._dpiRatio;
		var arrowHeightPixels = 40 * this._dpiRatio;
		var arrowDitchPixels = 3 * this._dpiRatio;
		var labelOffsetPixels = 10 * this._dpiRatio;
		var labelFontSize = 20 * this._dpiRatio;

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

		var printedFrameBounds = this.getPrintedExtent();
		if (!printedFrameBounds) {
			// The frame has not been drawn yet... This should not happen...
			return [];
		}

		// Calculate the initial location of the north arrow
		if (this.northArrowLocation === null) {
			var northArrowOffset = this.options.northArrowOffset || [];
			northArrowOffset = [
				(northArrowOffset[X] || 20) * this._dpiRatio * res,
				(northArrowOffset[Y] || 30) * this._dpiRatio * res
			];
			this.northArrowLocation = [printedFrameBounds.left + northArrowOffset[X], printedFrameBounds.top - northArrowOffset[Y]];
		}

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

		var bboxArrow = new OpenLayers.Feature.Vector(
			new OpenLayers.Geometry.Polygon([bboxArrowRing]),
			null, // attributes
			bboxArrowStyle
		);
		bboxArrow.isNorthArrowHandle = true;

		var arrowFeatures = [arrow, whiteArrow, label, bboxArrow];

		// Draggable
		var drag = new OpenLayers.Control.DragFeature(this, {
			onStart: startDrag,
			onDrag: doDrag,
			onComplete: endDrag
		});
		// Little hack to enable the drag handler only on specified features
		drag.handlers.feature.geometryTypeMatches = function(feature) {
			return feature.isNorthArrowHandle === true;
		};
		this.map.addControl(drag);
		drag.activate();

		// Drag handle starting to move
		function startDrag(feature, pixel) {
			lastPixel = pixel;
		}

		// Drag handle moving
		function doDrag(feature, pixel) {
			for (var i = 0; i < arrowFeatures.length; i++) {
				if (feature != arrowFeatures[i]) {
					var res = that.map.getResolution();
					arrowFeatures[i].geometry.move(res * (pixel.x - lastPixel.x), res * (lastPixel.y - pixel.y));
					that.drawFeature(arrowFeatures[i]);
				}
			}

			lastPixel = pixel;
		}

		// Drag handle stopped moving
		function endDrag(feature, pixel) {
			for (var i = 0; i < arrowFeatures.length; i++) {
				arrowFeatures[i].state = OpenLayers.State.UPDATE;
			}
		}

		// Disable the drag control when the element is deleted
		function beforeFeatureRemoved(e) {
			var feature = e.feature;
			if (feature === bboxArrow) {
				// Get a fresh copy of the bounds
				printedFrameBounds = that.getPrintedExtent();

				// Recalculate the arrow location (centre)
				that.northArrowLocation = [
					bboxArrow.geometry.bounds.left + (arrowWidthPixels/2 * res),
					bboxArrow.geometry.bounds.top - (arrowHeightPixels/2 * res)
				];

				// Remove controls and event listeners
				drag.deactivate();
				drag.destroy();
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



	_onZoomChange: function() {
		var res = this.map.getResolution();

		// Recalculate the length of one degree
		this._oneDegreeLength = (this._reproject(new OpenLayers.Geometry.Point(1, 0)).x - this._reproject(new OpenLayers.Geometry.Point(0, 0)).x) / res;
		if (this._oneDegreeLength < 1) { this._oneDegreeLength = 1; }
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

		if (orientation === 'vertical') {
			value = this._correctLongitude(value);
		}

		var label = (value < 0 ? (value*-1) : value).toFixed(6).replace(/00*$/, '').replace(/\.\.*$/, '');
		if (orientation === 'vertical') {
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
