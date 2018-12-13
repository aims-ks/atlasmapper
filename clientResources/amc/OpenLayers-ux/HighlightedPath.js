/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2014 Australian Institute of Marine Science
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

/**
 * This class replicate the functionality of OpenLayers.Handler.Path,
 * with an extra line under the main line, to ack as a line highlight.
 *
 * API: http://dev.openlayers.org/docs/files/OpenLayers/Handler/Path-js.html
 *
 * The only difference is the following property that can be set in the
 * options object during instantiation of set as an object property.
 * Properties
 *     lineHighlightStyle {String | Object} Named render intent or full
 *                        symbolizer object.
 *                        This is the style applyed to the background of
 *                        the line, used as a highlight. The strokeWidth
 *                        as to be higher than the one used with the main
 *                        line.
 * Example:
 *     // Style for the main line and the icon bellow the mouse.
 *     var style = new OpenLayers.Style();
 *     style.addRules([
 *         new OpenLayers.Rule({symbolizer: {
 *             // http://dev.openlayers.org/docs/files/OpenLayers/Symbolizer/Point-js.html
 *             "Point": {
 *                 pointRadius: 16,
 *                 fillOpacity: 1,
 *                 graphicXOffset: -5, graphicYOffset: 15,
 *                 externalGraphic: "resources/images/cursor.png"
 *             },
 *             // http://dev.openlayers.org/docs/files/OpenLayers/Symbolizer/Line-js.html
 *             "Line": {
 *                 strokeWidth: 3,
 *                 strokeColor: "#000000"
 *             }
 *         }})
 *     ]);
 *
 *     path = new OpenLayers.Handler.ux.HighlightedPath(control, callbacks, {
 *         maxVertices: 2,
 *         // http://dev.openlayers.org/docs/files/OpenLayers/Symbolizer/Line-js.html
 *         lineHighlightStyle: {
 *             strokeWidth: 7, // With of 7 is higher than 3
 *             strokeColor: "#FFFFFF"
 *         },
 *         layerOptions: {
 *             styleMap: new OpenLayers.StyleMap(style)
 *         }
 *     });
 */
if (typeof(OpenLayers.Handler.ux) == 'undefined') {
	OpenLayers.Handler.ux = {};
}

OpenLayers.Handler.ux.HighlightedPath = OpenLayers.Class(OpenLayers.Handler.Path, {
	lineHighlight: null,
	lineHighlightStyle: null,

	createFeature: function(pixel) {
		var lonlat = this.layer.getLonLatFromViewPortPx(pixel);
		var geometry = new OpenLayers.Geometry.Point(
			lonlat.lon, lonlat.lat
		);
		this.point = new OpenLayers.Feature.Vector(geometry);

		this.line = new OpenLayers.Feature.Vector(
			new OpenLayers.Geometry.LineString([this.point.geometry]),
			null,
			this.style
		);
		this.callback("create", [this.point.geometry, this.getSketch()]);

		this.lineHighlight = new OpenLayers.Feature.Vector(
			new OpenLayers.Geometry.LineString([this.point.geometry]),
			null,
			this.lineHighlightStyle
		);
		this.callback("create", [this.point.geometry, this.lineHighlight]);

		this.point.geometry.clearBounds();
		this.layer.addFeatures([this.lineHighlight, this.line, this.point], {silent: true});

	},

	destroyFeature: function(force) {
		OpenLayers.Handler.Path.prototype.destroyFeature.apply(this, arguments);
		this.lineHighlight = null;
	},

	/**
	 * Method: destroyPersistedFeature
	 * Destroy the persisted feature.
	 */
	destroyPersistedFeature: function() {
		var layer = this.layer;
		if(layer && layer.features.length > 3) {
			// Line
			this.layer.features[1].destroy();
			// Background line
			this.layer.features[0].destroy();
		}
	},

	addPoint: function(pixel) {
		OpenLayers.Handler.Path.prototype.addPoint.apply(this, arguments);

		this.lineHighlight.geometry.addComponent(
			this.point.geometry, this.lineHighlight.geometry.components.length
		);
		this.callback("modify", [this.point.geometry, this.lineHighlight]);
		this.drawFeature();
	},

	insertXY: function(x, y) {
		OpenLayers.Handler.Path.prototype.insertXY.apply(this, arguments);

		this.lineHighlight.geometry.addComponent(
			new OpenLayers.Geometry.Point(x, y),
			this.getCurrentHighlightPointIndex()
		);
		this.drawFeature();
	},

	insertDeltaXY: function(dx, dy) {
		OpenLayers.Handler.Path.prototype.insertDeltaXY.apply(this, arguments);

		var previousIndex = this.getCurrentHighlightPointIndex() - 1;
		var p0 = this.lineHighlight.geometry.components[previousIndex];
		if (p0 && !isNaN(p0.x) && !isNaN(p0.y)) {
			this.insertXY(p0.x + dx, p0.y + dy);
		}
	},

	getCurrentHighlightPointIndex: function() {
		return this.lineHighlight.geometry.components.length - 1;
	},

	undo: function() {
		var geometry = this.lineHighlight.geometry;
		var components = geometry.components;
		var index = this.getCurrentHighlightPointIndex() - 1;
		var target = components[index];
		var undone = geometry.removeComponent(target);

		return OpenLayers.Handler.Path.prototype.undo.apply(this, arguments);
	},

	redo: function() {
		var target = this.redoStack && this.redoStack[this.redoStack.length - 1];
		if (target) {
			this.lineHighlight.geometry.addComponent(target, this.getCurrentHighlightPointIndex());
		}

		return OpenLayers.Handler.Path.prototype.redo.apply(this, arguments);
	},

	modifyFeature: function(pixel, drawing) {
		if(!this.line) {
			this.createFeature(pixel);
		}
		var lonlat = this.layer.getLonLatFromViewPortPx(pixel);
		this.point.geometry.x = lonlat.lon;
		this.point.geometry.y = lonlat.lat;
		this.callback("modify", [this.point.geometry, this.getSketch(), drawing]);
		this.callback("modify", [this.point.geometry, this.lineHighlight, drawing]);
		this.point.geometry.clearBounds();
		this.drawFeature();
	},

	drawFeature: function() {
		this.layer.drawFeature(this.lineHighlight, this.lineHighlightStyle);
		this.layer.drawFeature(this.line, this.style);
		this.layer.drawFeature(this.point, this.style);
	},

	finishGeometry: function() {
		var index = this.line.geometry.components.length - 1;
		this.line.geometry.removeComponent(this.line.geometry.components[index]);
		this.lineHighlight.geometry.removeComponent(this.lineHighlight.geometry.components[index]);
		this.removePoint();
		this.finalize();
	},

	CLASS_NAME: "OpenLayers.Handler.ux.HighlightedPath"
});
