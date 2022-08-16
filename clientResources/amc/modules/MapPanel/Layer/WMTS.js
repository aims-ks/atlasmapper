/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2012 Australian Institute of Marine Science
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

// Namespace declaration (equivalent to Ext.namespace("Atlas.Layer");)
window["Atlas"] = window["Atlas"] || {};
window["Atlas"]["Layer"] = window["Atlas"]["Layer"] || {};

Atlas.Layer.WMTS = OpenLayers.Class(Atlas.Layer.WMS, {
	/**
	 * Constructor: Atlas.Layer.WMTS
	 *
	 * Parameters:
	 * mapPanel - {Object} The MapPanel instance
	 * jsonLayer - {Object} Hashtable of layer attributes
	 * mapPanel - {Object} Instance of the MapPanel in which the layer is used
	 */
	initialize: function(mapPanel, jsonLayer, parent) {
		// WMTS can't use cache
		this.useCache = false;

		// OpenLayers do not trigger the loading events with WMTS layers
		this.supportLoadEvents = false;

		if (mapPanel && mapPanel.dpi !== mapPanel.DEFAULT_DPI) {
			// Set the initial layer DPI
			// Clone jsonLayer
			jsonLayer = OpenLayers.Util.extend({}, jsonLayer);

			// Clone jsonLayer['olParams'] object or create a new one
			jsonLayer['olParams'] = OpenLayers.Util.extend({}, jsonLayer['olParams'] || {});
			jsonLayer['olParams']['format_options'] = 'dpi:' + mapPanel.dpi;

			// Set the initial layer tile size
			this.mapPanel = mapPanel; // This is done automatically later, but it's needed now...
			var newTileSize = this._getTileSizeForDPI(mapPanel.dpi, jsonLayer);
			if (newTileSize !== this.DEFAULT_TILE_SIZE) {
				var newTileSizeObj = new OpenLayers.Size(newTileSize, newTileSize);

				// Clone jsonLayer
				jsonLayer = OpenLayers.Util.extend({}, jsonLayer);

				// Double tiles
				// Clone jsonLayer['olOptions'] object or create a new one
				jsonLayer['olOptions'] = OpenLayers.Util.extend({}, jsonLayer['olOptions'] || {});
				jsonLayer['olOptions']['tileSize'] = newTileSizeObj;
			}
		}

		Atlas.Layer.AbstractLayer.prototype.initialize.apply(this, arguments);

		// Find the default style
		var defaultStyle = "";
		if (this.json['styles']) {
			for (var i=0, len=this.json['styles'].length; i<len; i++) {
				var jsonStyle = this.json['styles'][i];
				if (jsonStyle["selected"]) {
					defaultStyle = jsonStyle["name"];
					break;
				}
			}
		}

		// OpenLayers code example for WMTS:
		//   https://gis.stackexchange.com/questions/331836/wmts-from-geoserver-to-openlayers-is-unknown-tilematrix-x

		// Example of a URL to a WMTS tile:
		//   http://localhost:8080/geoserver/gwc/service/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=nurc%3AArc_Sample&STYLE=&TILEMATRIXSET=EPSG%3A4326&TILEMATRIX=EPSG%3A4326%3A7&TILEROW=34&TILECOL=58&FORMAT=image%2Fjpeg

		// MatrixSet: get the one which has a SupportedCRS which matches the map projection
		/*
		<TileMatrixSet>
			<ows:Identifier>WebMercatorQuad</ows:Identifier>
			<ows:SupportedCRS>EPSG:3857</ows:SupportedCRS>
			<TileMatrix>...</TileMatrix>
		</TileMatrixSet>
		*/

		if (!mapPanel) {
			// Dummy layer, used for the description panel of the "Add layer" window
			this.setLayer(new OpenLayers.Layer.WMTS({
				name: this.getTitle(),
				url: this.json['serviceUrl'],
				layer: this.json['layerName'],
				style: defaultStyle,
				matrixSet: ""
			}));

		} else {
			var mapProjection = mapPanel.map.getProjectionObject().getCode();
			if (mapProjection === "EPSG:900913") {
				mapProjection = "EPSG:3857";
			}

			var matrixSets = this.json['matrixSets'];
			var matrixSetId = "";
			var matrixIds = [];
			if (matrixSets) {
				// Find the appropriate matrixSet for the map projection
				var matrixSet = matrixSets[mapProjection];
				if (matrixSet) {
					matrixSetId = matrixSet['id'];
					var matrixMap = matrixSet['matrixMap'];
					if (matrixMap) {
						for (var scaleDenominator in matrixMap) {
							if (matrixMap.hasOwnProperty(scaleDenominator)) {
								//matrixIds[zoomLevel] = matrixMap[zoomLevel];
								matrixIds.push({
									'identifier': matrixMap[scaleDenominator],
									'scaleDenominator': scaleDenominator
								});
							}
						}
					}
				}
			}

			this.setLayer(new OpenLayers.Layer.WMTS({
				'name': this.getTitle(),
				'url': this.json['serviceUrl'],
				'layer': this.json['layerName'],
				'style': defaultStyle,
				'format': 'image/png',
				'matrixSet': matrixSetId,
				'matrixIds': matrixIds,
				'isBaseLayer': false,
				'wrapX': true,
				'tileFullExtent': this.getExtent(this.json, mapPanel)
			}));

			// https://github.com/openlayers/ol2/blob/master/notes/2.12.md

			var that = this;
			mapPanel.ol_on("dpiChange", function(evt) {
				that._dpiChange(evt.dpi);
			});
			mapPanel.ol_on("gutterChange", function(evt) {
				that._gutterChange(evt.gutter);
			});
		}
	}

});
