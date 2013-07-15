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
		// WMS use cache when possible (by default)
		this.useCache = true;

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
			mapPanel.ol_on("gutterChange", function(evt) {
				that._gutterChange(evt.gutter);
			});
		}
	},

	// Override
	setOptions: function(optionsPanel) {
		// NOTE: This value of this variable is also set on loadstart.
		var cacheAvailable = this.canUseWebCache(this.layer.params);
		var useCacheCheckboxId = 'useCache_' + this.layerId;
		var useCacheCheckbox = {
			xtype: "checkbox",
			name: "useCache",
			id: useCacheCheckboxId,
			cls: "advancedOption",
			fieldLabel: "Use server cache",
			checked: cacheAvailable && this.useCache,
			disabled: !cacheAvailable,
			layer: this.layer,
			scope: this,
			handler: function(checkbox, checked) {
				// Modify the "useCache" flag if the user has click the checkbox
				// (do not modify the flag if the checkbox state is changed by
				// the action of disabling the ckeckbox)
				if (this.canUseWebCache(this.layer.params)) {
					this.useCache = !!checked;
				}
				// This trigger a redraw if needed (if the server URL has changed)
				this.setParameters({});
			}
		};
		optionsPanel.addOption(this, useCacheCheckbox);

		// Change the checkbox status when the layer parameters changes
		this.layer.events.on({
			// TODO Unregister this!!
			'loadstart': function() {
				// The checkbox is destroyed and recreated every time the panel is redrawn,
				// so we can not keep a reference to the component.
				var useCacheCheckboxObj = Ext.ComponentMgr.get(useCacheCheckboxId);
				if (useCacheCheckboxObj && useCacheCheckboxObj.layer == this.layer) {
					cacheAvailable = this.canUseWebCache(this.layer.params);
					useCacheCheckboxObj.setValue(cacheAvailable && this.useCache);
					if (cacheAvailable) {
						useCacheCheckboxObj.enable();
					} else {
						useCacheCheckboxObj.disable();
					}
				}
			},
			scope: this
		});
	},

	// override
	/*
	getPreviewUrl: function() {
		return 'http://images.all-free-download.com/images/graphiclarge/google_maps_97811.jpg';
	},
	*/

	// Called when the DPI change on the mapPanel
	_dpiChange: function(dpi) {
		var defaultDPI = this.mapPanel ? this.mapPanel.DEFAULT_DPI : 90;
		this._setTileSizeForDPI(dpi);
		if (dpi == defaultDPI) {
			this.setParameters({'format_options': null});
		} else {
			this.setParameters({'format_options': 'dpi:' + dpi});
		}
	},

	// Called when the gutter change on the mapPanel
	// TODO Find a way to dynamically change the layers gutter.
	// NOTE: According to this old post:
	//     http://lists.osgeo.org/pipermail/openlayers-users/2007-January/000461.html
	//     the tiles are loaded in a "frame" (probably a div) smaller than the image, then they are centered to hide the extra space (the gutter)
	//     I need to change the imageSize without changing the tileSize...
	_gutterChange: function(gutter) {
		var defaultGutter = this.mapPanel ? this.mapPanel.DEFAULT_GUTTER : 0;
		/*
		// Good try, but that doesn't work. Gutter is an OpenLayers option, not a URL parameter
		if (gutter == defaultGutter) {
			this.setParameters({'gutter': null});
		} else {
			this.setParameters({'gutter': gutter});
		}
		*/

		/*
		// Closer, but still not working. OpenLayer request bigger tile, overlap them, but do not crop them.
		var tileSize = (this.layer.tileSize) ? this.layer.tileSize : this.layer.map.getTileSize();
		this.layer.gutter = gutter;
		this.layer.imageSize = new OpenLayers.Size(tileSize.w + (2*gutter), tileSize.h + (2*gutter));
		this.layer.redraw();
		*/
	},

	_getTileSizeForDPI: function(dpi) {
		var defaultDPI = this.mapPanel ? this.mapPanel.DEFAULT_DPI : 90;
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
		var defaultDPI = this.mapPanel ? this.mapPanel.DEFAULT_DPI : 90;
		if (this.layer) {
			var currentTileSizeObj = this.layer.tileSize;
			var newTileSize = this._getTileSizeForDPI(dpi);
			var newTileSizeObj = new OpenLayers.Size(newTileSize, newTileSize);

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

		if (this.json['wmsRequestMimeType']) {
			layerParams.format = this.json['wmsRequestMimeType'];
		}

		// Select default style if needed
		if (this.json['styles']) {
			for (var i=0, len=this.json['styles'].length; i<len; i++) {
				var jsonStyle = this.json['styles'][i];
				if (jsonStyle["selected"]) {
					layerParams.styles = jsonStyle["name"];
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

	setWebCacheParameters: function(params) {
		// The WMS version is also used in the WMS requests and the legend graphics.
		params['version'] = this.json['cacheWmsVersion'] ? this.json['cacheWmsVersion'] : null;
	},
	setDirectParameters: function(params) {
		// The WMS version is also used in the WMS requests and the legend graphics.
		params['version'] = this.json['wmsVersion'] ? this.json['wmsVersion'] : null;
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

		if (this.json['options']) {
			for (var i=0, len=this.json['options'].length; i < len; i++) {
				var option = this.json['options'][i];
				if (option['name']) {
					var extraOptionName = option['name'].toUpperCase();
					var value = this.getParameter(extraOptionName, null);
					if (value) {
						params[extraOptionName] = value;
					}
				}
			}
		}

		// Some WMS server require 1.1.1 parameters even when the layer has been requested as 1.3.0 (and vice versa)
		// so the easiest and more strait forward way to ensure the feature request works is to always add both set
		// of parameters.

		// For version 1.3.0
		var projection = layer.projection;
		if (!projection) {
			projection = this.mapPanel.map.getProjection();
		}
		params.crs = projection;
		params.i = clickPosition.x;
		params.j = clickPosition.y;

		// For version 1.1.1
		params.srs = projection;
		params.x = clickPosition.x;
		params.y = clickPosition.y;

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
