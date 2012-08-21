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

Atlas.Layer.WMS = OpenLayers.Class(Atlas.Layer.AbstractLayer, {
	featureInfoMaxFeatures: 10,
	featureInfoFormat: 'text/html',

	// Tile size are 256 x 256.
	DEFAULT_TILE_SIZE: 256,
	MAX_TILE_SIZE: 1024,

	/**
	 * Constructor: Atlas.Layer.WMS
	 *
	 * Parameters:
	 * jsonLayer - {Object} Hashtable of layer attributes
	 * mapPanel - {Object} Instance of the MapPanel in which the layer is used
	 */
	initialize: function(mapPanel, jsonLayer, parent) {
		if (mapPanel && mapPanel.dpi != mapPanel.DEFAULT_DPI) {
			// Set the initial layer DPI
			// Clone jsonLayer
			jsonLayer = OpenLayers.Util.extend({}, jsonLayer);

			// Clone jsonLayer['olParams'] object or create a new one
			jsonLayer['olParams'] = OpenLayers.Util.extend({}, jsonLayer['olParams'] || {});
			jsonLayer['olParams']['format_options'] = 'dpi:' + mapPanel.dpi;

			// Set the initial layer tile size
			this.mapPanel = mapPanel; // This is done automatically later, but it's needed now...
			var newTileSize = this._getTileSizeForDPI(mapPanel.dpi);
			if (newTileSize != this.DEFAULT_TILE_SIZE) {
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

		// TODO Support Multiple URLS => this._getWMSExtraServiceUrls(),
		var layerParams = this.getWMSLayerParams();
		this.setLayer(new OpenLayers.Layer.WMS(
			this.getTitle(),
			this.getServiceUrl(layerParams),
			layerParams,
			this.getWMSLayerOptions()
		));

		if (mapPanel) {
			var that = this;
			mapPanel.ol_on("dpiChange", function(evt) {
				that._dpiChange(evt.dpi);
			});
		}
	},

	// Called when the DPI change on the mapPanel
	_dpiChange: function(dpi) {
		this._setTileSizeForDPI(dpi);
		this.setParameters({'format_options': 'dpi:' + dpi});
	},

	_getTileSizeForDPI: function(dpi) {
		var defaultDPI = this.mapPanel.DEFAULT_DPI;
		var tileSizeRatio = Math.ceil(dpi / defaultDPI);

		// Adjustment: The ratio must be in log 2, to fit with the date time line.
		var validRatio = 1;
		while (validRatio < tileSizeRatio) {
			// square the validRatio, to stay in log 2.
			validRatio = validRatio * 2;
		}

		// calculate the new tile size
		var newTileSize = this.DEFAULT_TILE_SIZE * validRatio;
		if (newTileSize > this.MAX_TILE_SIZE) {
			newTileSize = this.MAX_TILE_SIZE;
		}

		return newTileSize;
	},

	_setTileSizeForDPI: function(dpi) {
		if (this.layer) {
			var newTileSize = this._getTileSizeForDPI(dpi);
			var newTileSizeObj = new OpenLayers.Size(newTileSize, newTileSize);
			var currentTileSizeObj = this.layer.tileSize;

			if (newTileSizeObj != currentTileSizeObj) {
				// OpenLayers.Layer.Grid.setTileSize(OpenLayers.Size);
				this.layer.setTileSize(newTileSizeObj);
			}
		}
	},

	getWMSLayerParams: function() {
		if (this.json == null) {
			return null;
		}

		var isBaseLayer = !!this.json['isBaseLayer'];

		// Set the parameters used in the URL to request the tiles.
		var layerParams = {
			layers: this.json['layerName'] || this.json['layerId']
		};
		// The WMS version is also used in the WMS requests and the legend graphics.
		if (this.json['wmsVersion']) {
			layerParams.version = this.json['wmsVersion'];
		}

		if (this.json['wmsRequestMimeType']) {
			layerParams.format = this.json['wmsRequestMimeType'];
		}

		// Select default style if needed
		if (this.json['styles']) {
			for (var styleName in this.json['styles']) {
				var jsonStyle = this.json['styles'][styleName];
				if (styleName && jsonStyle["default"]) {
					layerParams.styles = styleName;
					break;
				}
			}
		}

		layerParams.transparent = !isBaseLayer;

		if (typeof(this.json['olParams']) !== 'undefined') {
			layerParams = this.applyOlOverrides(layerParams, this.json['olParams']);
		}

		return layerParams;
	},

	// TODO Use this method
	_getWMSExtraServiceUrls: function() {
		return this.json['extraWmsServiceUrls'];
	},

	getWMSLayerOptions: function() {
		if (this.json == null) {
			return null;
		}

		var isBaseLayer = !!this.json['isBaseLayer'];

		// Set the OpenLayer options, used by the library.
		var layerOptions = {
			isBaseLayer: isBaseLayer,
			displayInLayerSwitcher: true,
			wrapDateLine : true,
			buffer: 0
		};

		if (this.json['projection']) {
			layerOptions.projection = this.json['projection'];
		} else if (this.mapPanel) {
			layerOptions.projection = this.mapPanel.map.getProjectionObject()
		}

		if (Atlas.conf['mapOptions'] && Atlas.conf['mapOptions']['units']) {
			layerOptions.units = Atlas.conf['mapOptions']['units'];
		}

		if (isBaseLayer) {
			// Set the transition effect to something smooth.
			// This effect allow the user to view the base layer
			// when zooming in/out or panning, by resizing the
			// existing tiles and display them while waiting for
			// the new tiles.
			layerOptions.transitionEffect = 'resize';
		}

		if (typeof(this.json['olOptions']) !== 'undefined') {
			layerOptions = this.applyOlOverrides(layerOptions, this.json['olOptions']);
		}

		return layerOptions;
	},

	/**
	 * Method: getFeatureInfoURL
	 * Build an object with the relevant WMS options for the GetFeatureInfo request
	 *
	 * Parameters:
	 * url - {String} The url to be used for sending the request
	 * layers - {Array(<OpenLayers.Layer.WMS)} An array of layers
	 * clickPosition - {<OpenLayers.Pixel>} The position on the map where the mouse
	 *     event occurred.
	 * format - {String} The format from the corresponding GetMap request
	 *
	 * return {
	 *     url: String
	 *     params: { String: String }
	 * }
	 */
	// Override
	getFeatureInfoURL: function(url, layer, clickPosition, format) {
		var layerId = layer.atlasLayer.json['layerId'];

		var params = {
			service: "WMS",
			version: layer.params.VERSION,
			request: "GetFeatureInfo",
			layers: [this.json['layerName']],
			query_layers: [this.json['layerName']],
			bbox: this.mapPanel.map.getExtent().toBBOX(null,
				layer.reverseAxisOrder()),
			height: this.mapPanel.map.getSize().h,
			width: this.mapPanel.map.getSize().w,
			format: format
		};

		if (parseFloat(layer.params.VERSION) >= 1.3) {
			params.crs = this.mapPanel.map.getProjection();
			params.i = clickPosition.x;
			params.j = clickPosition.y;
		} else {
			params.srs = this.mapPanel.map.getProjection();
			params.x = clickPosition.x;
			params.y = clickPosition.y;
		}

		params.feature_count = this.featureInfoMaxFeatures;
		params.info_format = this.featureInfoFormat;

//		OpenLayers.Util.applyDefaults(params, this.vendorParams);

		return {
			url: url,
			params: OpenLayers.Util.upperCaseObject(params)
		};
	},

	// Override
	getFeatureInfoResponseFormat: function() {
		return new OpenLayers.Format.WMSGetFeatureInfo();
	},

	/**
	 * Return the HTML chunk that will be displayed in the balloon.
	 * @param xmlResponse RAW XML response
	 * @param textResponse RAW text response
	 * @return {String} The HTML content of the feature info balloon, or null if the layer info should not be shown.
	 */
	// Override
	processFeatureInfoResponse: function(responseEvent) {
		if (!this.isHtmlResponse(responseEvent.text)) {
			return null;
		}

		return '<h3>' + this.getTitle() + '</h3>' + responseEvent.text;
	},

	// override
	getCredentials: function() {
		return this.getTitle();
	}
});
