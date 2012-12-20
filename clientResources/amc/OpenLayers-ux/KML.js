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
			// OpenLayers need this to reproject the KML, if needed.
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
	 */
	_overrideStyleAttributes: function(feature) {
		if (this._isPoint(feature)) {

			var fontSize = 13;

			var graphicHeight = 0;
			if (typeof(feature.style.graphicHeight) !== 'undefined' && !isNaN(feature.style.graphicHeight)) {
				graphicHeight = parseFloat(feature.style.graphicHeight);
			}

			// Default graphicYOffset is -(graphicHeight / 2)
			var graphicYOffset = graphicHeight / -2;
			if (typeof(feature.style.graphicYOffset) !== 'undefined' && !isNaN(feature.style.graphicYOffset)) {
				graphicYOffset = feature.style.graphicYOffset;
			}

			var yOffset = -graphicYOffset + fontSize;

			return {
				label: feature.attributes.name,
				fontSize: fontSize + 'px',
				labelXOffset: 0,
				labelYOffset: yOffset,
				labelSelect: true
			};
		}
		return null;
	},

	_isPoint: function(feature) {
		return (typeof(feature) !== 'undefined' &&
				typeof(feature.geometry) !== 'undefined' &&
				typeof(feature.geometry.x) !== 'undefined' &&
				typeof(feature.geometry.y) !== 'undefined');
	},

	// Override
	drawFeature: function(feature, style) {
		if (typeof style != "object") {
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
				//this.applyIf(style, this.overrideStyleAttributes(feature));
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

	CLASS_NAME: "OpenLayers.Format.ux.KML"
});
