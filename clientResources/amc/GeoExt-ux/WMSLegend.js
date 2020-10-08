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

Ext.namespace("GeoExt.ux");

// widgets/WMSLegend.js
GeoExt.ux.WMSLegend = Ext.extend(GeoExt.WMSLegend, {
	defaultStyleIsFirst: false,
	defaultStyle: null,
	// legendDpiSupport: config options, stay untouched
	// _legendDpiSupport: private flag used to force image stretching, without changing the config option.
	legendDpiSupport: true, _legendDpiSupport: true,

	initComponent: function() {
		var mapPanel = this.layerRecord && this.layerRecord.getLayer() && this.layerRecord.getLayer().atlasLayer ? this.layerRecord.getLayer().atlasLayer.mapPanel : null;

		// Set the initial legend graphic DPI (if needed)
		// This has to be done before the image URL is generated.
		if (mapPanel && mapPanel.dpi != mapPanel.DEFAULT_DPI) {
			this.setDpiAttributes(mapPanel.dpi);
		}

		GeoExt.ux.WMSLegend.superclass.initComponent.call(this);

		// Set the initial legend text size DPI (if needed)
		// This has to be done after the object has been initialised.
		if (mapPanel && mapPanel.dpi != mapPanel.DEFAULT_DPI) {
			this.setItemsDpi(mapPanel.dpi);
		}

		var that = this;
		var deleteButton = new Ext.form.Field({
			cls: 'x-legend-delete',
			autoCreate: {
				tag: 'input',
				type: 'image',
				src: Ext.BLANK_IMAGE_URL,
				alt: 'Hide'
			},
			listeners: {
				render: function(evt) {
					deleteButton.getEl().on('click', function(evt) {
						var layer = that.layerRecord.getLayer();
						layer.atlasLayer.setHideInLegend(true);
					});
				}
			}
		});
		// Something somewhere call setText on this button...
		deleteButton.setText = function(val) {};
		this.items.insert(0, deleteButton);

		// Synchronised the legend graphic DPI with the map DPI
		// The listener is removed when the legend is removed
		if (mapPanel) {
			mapPanel.ol_on('dpiChange', this.onDpiChange, this);
			this.on('destroy', function() {
				mapPanel.ol_un('dpiChange', this.onDpiChange, this);
			}, that);
		}
	},

	onDpiChange: function(evt) {
		this.setDpi(evt.dpi);
		this.update();
	},

	setDpi: function(dpi) {
		this.setDpiAttributes(dpi);
		this.setItemsDpi(dpi);
	},

	setDpiAttributes: function(dpi) {
		// Change DPI value in LEGEND_OPTIONS
		if (this._supportLegendDpi()) {
			var legendOptions = this.baseParams['LEGEND_OPTIONS'] ? this.baseParams['LEGEND_OPTIONS'].split(';') : [];
			var keyFound = false;
			for (var i=0, len=legendOptions.length; i<len; i++) {
				var legendOption = legendOptions[i].split(':');
				if (legendOption[0] === 'dpi') {
					legendOptions[i] = 'dpi:' + dpi;
					keyFound = true;
					break;
				}
			}
			if (!keyFound) {
				legendOptions.push('dpi:' + dpi);
			}
			this.baseParams['LEGEND_OPTIONS'] = legendOptions.join(';');
		}
	},

	setItemsDpi: function(dpi) {
		// Change DPI in text label
		if (this.items) {
			var mapPanel = this.layerRecord && this.layerRecord.getLayer() && this.layerRecord.getLayer().atlasLayer ? this.layerRecord.getLayer().atlasLayer.mapPanel : null;
			var defaultDpi = mapPanel ? mapPanel.DEFAULT_DPI : 90;
			if (defaultDpi <= 0) {
				defaultDpi = 90;
			}
			var ratio = dpi / defaultDpi;
			var that = this;
			this.items.each(function(item) {
				// Create a function to be able to set and unset event listeners.
				var afterItemRender = function() {
					this.un('afterrender', afterItemRender, this);
					if (this.el) {
						that.setItemDpi(this, dpi, ratio);
					}
				}
				// Remove the even listener now, in case the DPI changes before the element has time to load.
				item.un('afterrender', afterItemRender, item);

				if (item.el) {
					afterItemRender.call(item);
				} else {
					item.on('afterrender', afterItemRender, item);
				}
			}, this);
		}
	},

	// Change DPI in text label
	setItemDpi: function(item, dpi, ratio) {
		if (item instanceof Ext.form.Label) {
			// ExtJS default text size for label is 12px.
			// If a different ExtJS template is used (like accessibility),
			// the font ratio will be wrong. But since this only affect
			// high DPI, we can ignore this issue.
			// NOTE: zoom attribute is a lot easier to use but IE do not
			//     support it properly. It change the displayed size but
			//     not the real estate (everything overlap)
			var fontSize = parseInt(12 * ratio);
			item.el.applyStyles('font-size: '+fontSize+'px;');

		} else if (item instanceof GeoExt.LegendImage) {
			// Create a function to be able to set and unset event listeners.
			var resizeImage = null;
			if (!this._supportLegendDpi()) {
				resizeImage = function() {
					item.el.removeListener('load', resizeImage, this);
					// Last attempt, if there is still no naturalWidth, just use the default of 110.
					var originalSize = this.getImgNaturalSize(item.el.dom);
					var originalWidth = originalSize[0];
					var imageWidth = originalWidth * ratio;
					item.el.applyStyles('width: '+imageWidth+'px;');
				};
			} else {
				resizeImage = function() {
					// Only called when a legend goes from a static image to a dynamic image...
					item.el.applyStyles('width: auto;');
				};
			}

			// Remove the even listener now, in case the DPI changes before the image has time to load.
			item.el.removeListener('load', resizeImage, this);

			// The "complete" attribute means "loaded". Works with many browsers, even IE6.
			if (item.el.dom && item.el.dom.complete) {
				resizeImage.call(this);
			} else {
				// There is no naturalWidth, the image might not be loaded yet.
				item.el.on('load', resizeImage, this);
			}
		}
	},

	/**
	 * Return the physical image width and height, rather than the dimensions of the image as it is
	 * displayed in the browser.
	 * Modern browsers can use the attributes "naturalWidth" and "naturalHeight", but some browsers(*)
	 * need a hack; load the image in a java script object and read the properties from that object.
	 * It is probably slower but it's better than having a small legend graphic.
	 * (*) Mostly all IE, many old browsers don't support "naturalWidth" and "naturalHeight".
	 *     Also some quite recent version of Firefox, for example, support the attribute but
	 *     returns garbage (always returns 1px).
	 * @param dom DOM element of a loaded image. If the image is not loaded, it should return [0, 0].
	 * @returns {Array} [width, height]
	 */
	getImgNaturalSize: function(dom) {
		if (typeof(dom.naturalWidth) !== 'undefined' && dom.naturalWidth > 1 &&
				typeof(dom.naturalHeight) !== 'undefined' && dom.naturalHeight > 1) {
			return [dom.naturalWidth, dom.naturalHeight];
		}
		var img = new Image();
		img.src = dom.src;
		return [img.width, img.height];
	},

	getLegendUrl: function(layerName, layerNames) {
		var rec = this.layerRecord;
		var layer = rec.getLayer();
		layerNames = layerNames || [layer.params.LAYERS].join(",").split(",");
		var styleNames = layer.params.STYLES &&
							 [layer.params.STYLES].join(",").split(",");
		var idx = layerNames.indexOf(layerName);
		var styleName = styleNames && styleNames[idx];

		url = this._getLegendUrl(layerName, layerNames, styleName, styleNames, {
			REQUEST: "GetLegendGraphic",
			WIDTH: null,
			HEIGHT: null,
			EXCEPTIONS: "application/vnd.ogc.se_xml",
			LAYER: layerName,
			LAYERS: null,
			STYLE: (styleName !== '') ? styleName: null,
			STYLES: null,
			SRS: null,
			FORMAT: null
		});

		return url;
	},

	/**
	 * getLegendUrl using pre-processed parameter, to make it more generic.
	 */
	_getLegendUrl: function(layerName, layerNames, styleName, styleNames, legendBaseParams) {
		this._setAtlasMapperLegendUrlInLayerStyles();
		this._setLegendDpiSupport(true);

		var rec = this.layerRecord;
		var url;
		var layer = rec.getLayer();

		// Check if we have a legend URL for the current style
		// (in the record's "styles" data field)
		var styles = rec && rec.get("styles");
		if(styles && styles.length > 0) {
			if(styleName) {
				Ext.each(styles, function(s) {
					url = (s.name == styleName && s.legend) && s.legend.href;
					return !url;
				});
			} else if(!styleNames && !layer.params.SLD && !layer.params.SLD_BODY) {
				// Set URL to the default style
				// Possible scenarios:
				// 1. Default style is usually set in the instance attributes
				// 2. Default style may be the first one
				// 3. Look in all styles to find the default one
				// 4. Don't set the URL, let this method find an other way to set it
				if (this.defaultStyle != null) {
					url = this.defaultStyle.legend && this.defaultStyle.legend.href;
				} else if (this.defaultStyleIsFirst === true) {
					url = styles[0].legend && styles[0].legend.href;
				} else {
					for (var i=0,len=styles.length; i<len; i++) {
						var s = styles[i];
						if (s._default === true) {
							url = styles[i].legend && styles[i].legend.href;
						}
					}
				}
			}
		}

		// Check if we have a global legend URL for this layer
		if(!url) {
			if (layer.atlasLayer.json['legendUrl']) {
				url = layer.atlasLayer.json['legendUrl'];
				if (layer.atlasLayer.json['legendFilename']) {
					// Hardcoded URL (in config)
					this._setLegendDpiSupport(false);
					url += layer.atlasLayer.json['legendFilename'];
					return url;
				}
				// Add parameters
				url = layer.getFullRequestString(
					legendBaseParams,
					url
				);
			}
		}

		if(!url) {
			url = layer.getFullRequestString(
				legendBaseParams,
				// Never get the legend from GWC
				layer.atlasLayer.json['serviceUrl']
			);
		}

		// Extract the parameters from the URL (do NOT split the value when there is a coma in it)
		var parameters = OpenLayers.Util.getParameters(url, {"splitArgs": false});
		// NOTE: Only play with the parameters if there is any parameters (i.e. do not mess around if it's a URL to a static image)
		if (parameters) {
			// Remove the parameters from the URL, to be able to modify them (instead of just re-adding the same parameter twice)
			url = url.substring(0, url.indexOf('?'));

			// add scale parameter - also if we have the url from the record's
			// styles data field and it is actually a GetLegendGraphic request.
			if (this.useScaleParameter === true) {
				var requestParameterKey = this._objectGetCaseInsensitiveKey(parameters, "request");
				var requestParameter = parameters[requestParameterKey];
				if (requestParameter && requestParameter.toLowerCase() == "getlegendgraphic") {
					parameters["SCALE"] = layer.map.getScale();
				}
			}

			// Get WMS Legend base parameters (including the one specified by the user in "legendParameters")
			var params = this.baseParams || {};
			// Legend are preferably GIF (unless specified in "legendParameters")
			// Ext.applyIf => Apply if not already present
			Ext.applyIf(params, {FORMAT: 'image/gif'});

			// Apply user Legend Parameters to the layer parameters
			Ext.apply(parameters, params);
		}

		if (parameters) {
			// Craft a new URL with the new parameters
			url += "?" + Ext.urlEncode(parameters);
		}

		return url;
	},

	_objectGetCaseInsensitiveKey: function(obj, key) {
		key = (key + "").toLowerCase();
		for (var prop in obj){
			if(obj.hasOwnProperty(prop) && key == (prop+ "").toLowerCase()){
				return prop;
			}
		}
		return null;
	},

	_setLegendDpiSupport: function(legendDpiSupport) {
		this._legendDpiSupport = legendDpiSupport;
	},
	_supportLegendDpi: function() {
		return this.legendDpiSupport && this._legendDpiSupport;
	},

	/**
	 * Override GeoExt.LayerLegend.getLayerTitle
	 * Manage custom legend title.
	 */
	getLayerTitle: function(record) {
		var layer = record.getLayer();
		if (layer && layer.atlasLayer.json && layer.atlasLayer.json['legendTitle']) {
			return layer.atlasLayer.json['legendTitle'];
		}

		return GeoExt.ux.WMSLegend.superclass.getLayerTitle.apply(this, arguments);
	},

	/**
	 * Create a custom legend URL for the current layer, if needed,
	 * and set it in the style of layer record.
	 */
	_setAtlasMapperLegendUrlInLayerStyles: function() {
		var layer = this.layerRecord.getLayer();
		var jsonLayer = layer.atlasLayer.json;
		var layerName = layer.name;

		// Query a different layer.
		// Used to display the legend of a different layer.
		if (jsonLayer['legendLayerId']) {
			layerName = jsonLayer['legendLayerId'];
		}

		// Override of the LegendGraphics in the layer config, and let
		// GeoExt deal with it.
		// "legendUrl" & "legendFilename"
		if (jsonLayer['styles']) {
			var styles = [];
			Ext.iterate(jsonLayer['styles'], function(styleName, styleJsonObj) {
				var styleObj = {};
				if (styleName) {
					styleObj.name = styleName;
				}

				if (styleJsonObj['default'] === true) {
					styleObj._default = true;
					// Set the default style as an attribute to avoid having to search for it.
					this.defaultStyle = styleObj;
				}

				if(styleJsonObj['legendUrl']) {
					var url = styleJsonObj['legendUrl'];
					if (styleJsonObj['legendFilename']) {
						url += styleJsonObj['legendFilename'];
					}

					// Query a different URL.
					// Used to display the legend of a different URL.
					// TODO affect each styles individually
					styleObj.legend = {
						href: url
					};
				}
				styles.push(styleObj);
			}, this);
			if (styles.length > 0) {
				this.layerRecord.set("styles", styles);
			}
		}
	},

	/**
	 * Override GeoExt.LayerLegend.onStoreUpdate
	 * Manage 'Show in legend' checkbox event.
	 *
	 * Add a call to the method layer.atlasLayer.getHideInLegend() in the setVisibility call.
	 * It would be nicer to set the hideInLegend attribute
	 * of the record, but it's too difficult to access this variable
	 * everywhere in the application.
	 * NOTE: layer => OpenLayers, record => GeoExt
	 */
	onStoreUpdate: function(store, record, operation) {
		// if we don't have items, we are already awaiting garbage
		// collection after being removed by LegendPanel::removeLegend, and
		// updating will cause errors
		if (record === this.layerRecord && this.items.getCount() > 0) {
			var layer = record.getLayer();
			this.setVisible(layer.getVisibility() &&
				layer.calculateInRange() && layer.displayInLayerSwitcher &&
				!record.get('hideInLegend') && !layer.atlasLayer.getHideInLegend());
			this.update();
		}
	},


	/**
	 * Override GeoExt.WMSLegend.update
	 * To consider the delete button
	 *
	 * private: method[update]
	 *  Update the legend, adding, removing or updating
	 *  the per-sublayer box component.
	 */
	update: function() {
		var layer = this.layerRecord.getLayer();
		// In some cases, this update function is called on a layer
		// that has just been removed, see ticket #238.
		// The following check bypass the update if map is not set.
		if(!(layer && layer.map)) {
			return;
		}
		GeoExt.WMSLegend.superclass.update.apply(this, arguments);
		
		var layerNames, layerName, i, len;

		layerNames = [layer.params.LAYERS].join(",").split(",");

		var destroyList = [];
		// It's much safer to compare indexes than objects
		var deleteButtonCmpIndex = 0;
		var textCmpIndex = 1;
		this.items.each(function(cmp, cmpIndex) {
			if (cmpIndex != deleteButtonCmpIndex) {
				i = layerNames.indexOf(cmp.itemId);
				if(i < 0 && cmpIndex != textCmpIndex) {
					destroyList.push(cmp);
				} else if(cmpIndex !== textCmpIndex){
					layerName = layerNames[i];
					var newUrl = this.getLegendUrl(layerName, layerNames);
					if(!OpenLayers.Util.isEquivalentUrl(newUrl, cmp.url)) {
						cmp.setUrl(newUrl);
					}
				}
			}
		}, this);
		for(i = 0, len = destroyList.length; i<len; i++) {
			var cmp = destroyList[i];
			// cmp.destroy() does not remove the cmp from
			// its parent container!
			this.remove(cmp);
			cmp.destroy();
		}

		for(i = 0, len = layerNames.length; i<len; i++) {
			layerName = layerNames[i];
			if(!this.items || !this.getComponent(layerName)) {
				this.add({
					xtype: "gx_ux_legendimage",
					url: this.getLegendUrl(layerName, layerNames),
					itemId: layerName,
					layer: layer,
					// Hide the legend when the image is broken
					listeners: {
						'hide': function() {
							this.hide();
						},
						'show': function() {
							this.show();
						},
						'scope': this
					}
				});
			}
		}
		this.doLayout();
	}
});

/**
 * private: method[supports]
 * Private override
 */
GeoExt.ux.WMSLegend.supports = function(layerRecord) {
	return layerRecord.getLayer() instanceof OpenLayers.Layer.WMS;
};

/** api: legendtype = gx_ux_wmslegend */
GeoExt.LayerLegend.types["gx_ux_wmslegend"] = GeoExt.ux.WMSLegend;

/** api: xtype = gx_ux_wmslegend */
Ext.reg('gx_ux_wmslegend', GeoExt.ux.WMSLegend);
