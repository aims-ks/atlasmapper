/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2013 Australian Institute of Marine Science
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

if (typeof(OpenLayers.Layer.ux) == 'undefined') {
	OpenLayers.Layer.ux = {};
}

OpenLayers.Layer.ux.KML = OpenLayers.Class(OpenLayers.Layer.Vector, {

	initialize: function(name, url, options) {
		if (!options) {
			options = {};
		}

		var kmlOptions = {
			strategies: [new OpenLayers.Strategy.Fixed()],
			// OpenLayers need to know in which projection the KML is, to reproject it properly.
			projection: new OpenLayers.Projection("EPSG:4326"),
			protocol: new OpenLayers.Protocol.HTTP({
				url: url,
				format: new OpenLayers.Format.ux.KML({
					extractStyles: true,
					extractAttributes: true
				})
			})
		};

		this.applyIf(options, kmlOptions);

		OpenLayers.Layer.Vector.prototype.initialize.apply(this, [name, options]);
	},

	/**
	 * Copy config attribute to cible, if not already set.
	 * Equivalent to Ext.applyIf(cible, config);
	 * NOTE: OpenLayers.Util.extend(cible, config) override
	 *     the attributes if they are defined in cible; we don't
	 *     want that to happen...
	 */
	applyIf: function(cible, config) {
		for(var key in config){
			if(config.hasOwnProperty(key) && typeof(cible[key]) === 'undefined'){
				cible[key] = config[key];
			}
		}
	},

	/**
	 * Add label above points.
	 * For a list of style attribute, see:
	 *     http://dev.openlayers.org/docs/files/OpenLayers/Feature/Vector-js.html#OpenLayers.Feature.Vector.OpenLayers.Feature.Vector.style
	 * IMPORTANT: There is an inconsistancy between "graphicYOffset" and "labelYOffset". The X offsets are fine.
	 *     Example: To move the point South of 100 pixels;
	 *         graphicYOffset += 100
	 *         labelYOffset -= 100
	 */
	_overrideStyleAttributes: function(feature) {
		if (this._isPoint(feature)) {
			var fontSize = 17; // Close to what Google Earth use

			var graphicWidth = 0;
			var graphicHeight = 0;
			// Grab the icon dimention from the KML style, if available
			if (typeof(feature.style.graphicWidth) !== 'undefined' && !isNaN(feature.style.graphicWidth)) {
				graphicWidth = parseFloat(feature.style.graphicWidth);
			}
			if (typeof(feature.style.graphicHeight) !== 'undefined' && !isNaN(feature.style.graphicHeight)) {
				graphicHeight = parseFloat(feature.style.graphicHeight);
			}

			// The default offset, when not specified in the KML, is not 0x0, it's half the image width / height
			// to position the center of the icon on the point lon / lat location.
			var graphicXOffset = graphicWidth / -2;
			var graphicYOffset = graphicHeight / -2;
			// Grab the icon offset from the KML style, if available
			if (typeof(feature.style.graphicXOffset) !== 'undefined' && !isNaN(feature.style.graphicXOffset)) {
				graphicXOffset = parseFloat(feature.style.graphicXOffset);
			}
			if (typeof(feature.style.graphicYOffset) !== 'undefined' && !isNaN(feature.style.graphicYOffset)) {
				graphicYOffset = parseFloat(feature.style.graphicYOffset);
			}

			var labelXOffset = graphicWidth; // Move the label East, to avoid overlap
			var labelYOffset = graphicHeight / -2; // Move the label South, to align with the middle of the icon instead of the top of the icon
			// Adjust the offset to follow the icon
			// IMPORTANT: There is an inconsistency between "graphicYOffset" and "labelYOffset". The X offsets are fine.
			//     Example: To move the point South of 100 pixels;
			//         graphicYOffset += 100
			//         labelYOffset -= 100
			labelXOffset += graphicXOffset;
			labelYOffset -= graphicYOffset;

			// Google Earth default text color is white
			var fontColor = '#FFFFFF';
			if (typeof(feature.style.fontColor) !== 'undefined') {
				fontColor = feature.style.fontColor;
			}

			var scale = 1;
			if (typeof(feature.style.scale) !== 'undefined') {
				scale = feature.style.scale;
			}

			var labelOutlineColor = "#000000";
			// If the text color is quite dark (< 15% luminosity), use white halo.
			if (this.getColorYIQ(fontColor) < 0.15) {
				labelOutlineColor = "#FFFFFF";
			}

			var style = {
				fontSize: (fontSize * scale) + 'px',
				fontFamily: 'sans-serif',
				fontWeight: 'bold',
				fontColor: fontColor,

				labelAlign: 'l',
				labelXOffset: labelXOffset,
				labelYOffset: labelYOffset,
				labelSelect: true,
				// Produce a black 'halo' around the text
				labelOutlineColor: labelOutlineColor,
				labelOutlineWidth: 3
			};

			// Google Earth hide the label when the scale is smaller than 0.5
			if (scale >= 0.5) {
				style.label = feature.attributes.name;
			}

			return style;
		}
		return null;
	},

	/**
	 * Convert an RGB color space into YIQ, which takes into account the different impacts of its constituent parts.
	 * See: http://24ways.org/2010/calculating-color-contrast/
	 * @param hexcolor The 6 characters hexadecimal color. It can starts with a '#'
	 * @returns {number} 1 means white, 0 black
	 */
	getColorYIQ: function(hexcolor) {
		if (hexcolor.charAt(0) === '#') {
			hexcolor = hexcolor.substring(1);
		}
		var r = parseInt(hexcolor.substr(0,2),16);
		var g = parseInt(hexcolor.substr(2,2),16);
		var b = parseInt(hexcolor.substr(4,2),16);
		return ((r*299)+(g*587)+(b*114))/255000;
	},

	_isPoint: function(feature) {
		return (typeof(feature) !== 'undefined' &&
				typeof(feature.geometry) !== 'undefined' &&
				typeof(feature.geometry.x) !== 'undefined' &&
				typeof(feature.geometry.y) !== 'undefined');
	},

	// Override
	drawFeature: function(feature, style) {
		if (typeof(style) !== "object") {
			if(!style && feature.state === OpenLayers.State.DELETE) {
				style = "delete";
			}
			var renderIntent = style || feature.renderIntent;
			style = feature.style || this.style;
			if (!style) {
				style = this.styleMap.createSymbolizer(feature, renderIntent);
			} else {
				// Add the missing attributes to the style found
				// in the KML (add labels)
				OpenLayers.Util.extend(style, this._overrideStyleAttributes(feature));
			}
		}
		OpenLayers.Layer.Vector.prototype.drawFeature.apply(this, [feature, style]);
	},

	getDocumentName: function() {
		return this.protocol && this.protocol.format ? this.protocol.format.documentName : null;
	},

	CLASS_NAME: "OpenLayers.Layer.ux.KML"
});


if (typeof(OpenLayers.Format.ux) == 'undefined') {
	OpenLayers.Format.ux = {};
}

OpenLayers.Format.ux.KML = OpenLayers.Class(OpenLayers.Format.KML, {
	documentName: null,

	// Override
	parseData: function(data, options) {
		if (typeof(data) === "string") {
			data = OpenLayers.Format.XML.prototype.read.apply(this, [data]);
		}
		this._parseDocumentTitle(data);

		return OpenLayers.Format.KML.prototype.parseData.apply(this, arguments);
	},

	_parseDocumentTitle: function(data) {
		if (data) {
			var documentNode = this.getElementsByTagNameNS(data, "*", "Document")[0];
			if (documentNode) {
				var documentNameNode = this.getChildEl(documentNode, "name");
				if (documentNameNode) {
					this.documentName = this.getChildValue(documentNameNode);
				}
			}
		}
	},

	// Override
	// Add the scale attribute to the Label styles
	parseStyle: function(node) {
		var style = OpenLayers.Format.KML.prototype.parseStyle.apply(this, arguments);

		var labelStyleNode = this.getElementsByTagNameNS(node, "*", "LabelStyle")[0];
		if (labelStyleNode) {
			var labelScale = this.parseProperty(labelStyleNode, "*", "scale");
			if (labelScale !== null) {
				style.scale = labelScale;
			}
		}

		return style;
	},

	CLASS_NAME: "OpenLayers.Format.ux.KML"
});
