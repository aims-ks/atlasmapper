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

Atlas.OptionsPanel = Ext.extend(Ext.form.FormPanel, {
	mapPanel: null,

	defaultContent: null,

	width: 'auto',
	autoWidth: true,
	labelWidth: 100,

	headerLabel: null,
	optionsFieldSet: null,
	// Extra options to set user defined URL parameters.
	extraOptionsFieldSet: null,
	ncwmsOptionsFieldSet: null,

	layernameLabel: null,
	legendCheckbox: null,
	opacitySlider: null,

	currentLayer: null,

	initComponent: function(){
		var that = this;

		if (this.html) {
			this.defaultContent = this.html;
			this.html = '';
		}

		var locateButton = new Ext.Button({
			text: 'Locate',
			handler: function() {
				that.mapPanel.ol_fireEvent('locateLayer', {layer: that.currentLayer});
			}
		});

		this.headerLabel = new Ext.form.Label({
			cls: 'emptyInfo',
			html: this.defaultContent
		});

		this.layernameLabel = new Ext.form.Label({
			cls: 'lanernameLabel',
			html: '',
			hidden: true
		});

		this.legendCheckbox = new Ext.form.Checkbox({
			fieldLabel: 'Show in legend',
			handler: function(checkbox, checked) {
				if (that.currentLayer) {
					that.currentLayer.setHideInLegend(!checked);
				}
			},
			checked: true,
			hidden: true
		});

		// Update the checkbox status when the function layer.setHideInLegend is called
		// NOTE: layer.setHideInLegend is defined in MapPanel
		this.mapPanel.ol_on('legendVisibilityChange', function(evt) {
			if (that.currentLayer && evt.layer == that.currentLayer) {
				that.legendCheckbox.setValue(!that.currentLayer.hideInLegend);
			}
		});

		this.opacitySlider = new GeoExt.LayerOpacitySlider({
			fieldLabel: 'Opacity',
			layer: this.currentLayer,
			aggressive: false,
			animate: false,
			changeVisibility: true,
			value: 0,
			plugins: new GeoExt.LayerOpacitySliderTip({template: '<div>{opacity}%</div>'})
		});

		this.optionsFieldSet = new Ext.form.FieldSet({
			hidden: true,
			defaultType: 'textfield',
			defaults: {
				anchor:'100%'
			},
			items: [
				this.legendCheckbox,
				this.opacitySlider
			],
			buttons: [
				locateButton
			]
		});

		this.onExtraOptionChange = function() {
				if (that.extraOptionsFieldSet && that.extraOptionsFieldSet.items) {
					var layer = that.currentLayer;
					if (layer && layer.mergeNewParams) {
						var serviceUrl = layer.json['wmsServiceUrl'];
						var webCacheUrl = layer.json['webCacheUrl'];
						if (!webCacheUrl) {
							webCacheUrl = serviceUrl;
						}
						var supportedParams = layer.json['webCacheSupportedParameters'];

						var newParams = {};
						var canUseWebCache = true;
						that.extraOptionsFieldSet.items.each(function(option) {
							var optionName = option.getName();
							if (option && optionName) {
								var optionType = option.getXType();
								var optionValue = option.getValue();

								// Check if the parameter may stop us from using the WebCache - Ignore parameter with no values
								if (canUseWebCache && optionValue) {
									canUseWebCache = that.paramIsSupported(optionName, supportedParams);
								}

								// Set the new parameter, or unset it if it has a null value (don't remove STYLES - it's mandatory).
								if (optionName != 'STYLES' && (typeof(optionValue) == 'undefined' || optionValue == null || optionValue == '')) {
									// Remove the param from the URL - Some server don't like to have en empty parameter
									delete layer.params[optionName];
								} else {
									newParams[optionName] = optionValue;
								}
							}
						});

						// Change the URL of the layer to use the appropriate server
						if (canUseWebCache) {
							layer.setUrl(webCacheUrl);
						} else {
							layer.setUrl(serviceUrl);
						}

						layer.mergeNewParams(newParams);
					}
				}
			};

		this.extraOptionsFieldSet = new Ext.form.FieldSet({
			hidden: true,
			defaultType: 'textfield',
			defaults: {
				anchor:'100%'
			}
		});

		this.onNCWMSExtraOptionChange = function() {
			if (that.ncwmsOptionsFieldSet && that.ncwmsOptionsFieldSet.items) {
				var layer = that.currentLayer;
				if (layer && layer.mergeNewParams) {
					var serviceUrl = layer.json['wmsServiceUrl'];
					var webCacheUrl = layer.json['webCacheUrl'];
					if (!webCacheUrl) {
						webCacheUrl = serviceUrl;
					}
					var supportedParams = layer.json['webCacheSupportedParameters'];

					var newParams = {};
					var canUseWebCache = true;
					that.ncwmsOptionsFieldSet.items.each(function(option) {
						var optionName = option.getName();
						if (option && optionName) {
							var optionType = option.getXType();
							var optionValue = option.getValue();

							// Check if the parameter may stop us from using the WebCache - Ignore parameter with no values
							if (canUseWebCache && optionValue) {
								canUseWebCache = that.paramIsSupported(optionName, supportedParams);
							}

							// Set the new parameter, or unset it if it has a null value (don't remove STYLES - it's mandatory).
							if (optionName != 'STYLES' && (typeof(optionValue) == 'undefined' || optionValue == null || optionValue == '')) {
								// Remove the param from the URL - Some server don't like to have en empty parameter
								delete layer.params[optionName];
							} else {
								newParams[optionName] = optionValue;
							}
						}
					});

					// Change the URL of the layer to use the appropriate server
					if (canUseWebCache) {
						layer.setUrl(webCacheUrl);
					} else {
						layer.setUrl(serviceUrl);
					}

					layer.mergeNewParams(newParams);
				}
			}
		};

		this.ncwmsOptionsFieldSet = new Ext.form.FieldSet({
			hidden: true,
			defaultType: 'textfield',
			defaults: {
				anchor:'100%'
			}
		});

		var formConfig = {
			layout: 'anchor',
			defaults: {   // defaults applied to items
				layout: 'form',
				border: false
			},
			items: [
				this.headerLabel,
				this.layernameLabel,
				this.optionsFieldSet,
				this.extraOptionsFieldSet,
				this.ncwmsOptionsFieldSet
			]
		};
		Ext.applyIf(this, formConfig);

		Atlas.OptionsPanel.superclass.initComponent.call(this);

		// Prevent form's keyboard event to go to the map
		this.on('render', function(evt) {
			// Those events can only be set after the element has been rendered.
			that.getEl().on('keydown',  function(evt) { evt.stopPropagation(); });
			that.getEl().on('keyup',    function(evt) { evt.stopPropagation(); });
			that.getEl().on('keypress', function(evt) { evt.stopPropagation(); });
		});
	},

	/**
	 * Verify if the Web Cache support the URL parameter.
	 */
	paramIsSupported: function(paramName, supportedParams) {
		if (!supportedParams || supportedParams.length <= 0) {
			// Supported parameters is not set:
			// The Web Cache server support everything
			return true;
		}

		Ext.each(supportedParams, function(supportedParam) {
			if (supportedParam.toUpperCase() === paramName.toUpperCase()) {
				return true;
			}
		});
		return false;
	},

	/**
	 * Auto-set the options for a given layer.
	 */
	setLayerOptions: function(layer) {
		var hasLegendEnabled = false;
		var opacityEnabled = false;

		// Delete previous extra options, if any
		this.extraOptionsFieldSet.hide();
		this.extraOptionsFieldSet.removeAll();

		this.ncwmsOptionsFieldSet.hide();
		this.ncwmsOptionsFieldSet.removeAll();

		this.currentLayer = layer;

		if (layer) {
			if (layer.json) {
				this.layernameLabel.setText(layer.json['title'], false);

				// Set extra options for the selected layer

				// Styles dropdown
				if (layer.json['wmsStyles']) {
					var styleOptionName = 'STYLES';
					var styleOptions = [];
					Ext.iterate(layer.json['wmsStyles'], function(styleName, jsonStyle) {
						// BUG: Duplicate names can not be select with the ExtJS select option.
						// Temporary(?) solution: Add a sequential number in front of it.
						var styleTitle = (jsonStyle['title'] ? jsonStyle['title'] : styleName);
						// TODO Display de description on a box when a style is selected
						var styleDescription = jsonStyle['description'];

						var styleReqName = styleName;
						if (jsonStyle['default']) {
							// To use the default style, set the parameter to nothing.
							styleReqName = "";
						}
						styleOptions[styleOptions.length] = [styleReqName, styleTitle];
					});

					var defaultValue =
						this.getParameterActualValue(layer, styleOptionName, "");

					if (styleOptions.length > 1) {
						// Sort styles
						styleOptions.sort(this._sortByName);

						// Fancy style name formating
						Ext.each(styleOptions, function(style, index) {
							// Highlight the default style (can not add any HTML inside input element...)
							if (style[0] === '') {
								style[1] = '[' + style[1] + ']';
							}
							// Add number before the Style name, to avoid duplicates
							style[1] = (index+1) + '. ' + style[1];
							styleOptions[index] = style;
						});

						var styleSelect = {
							xtype: "combo",
							name: styleOptionName,
							fieldLabel: "Styles",
							value: defaultValue,
							typeAhead: false,
							triggerAction: 'all',
							lazyRender: true,
							mode: 'local',
							store: new Ext.data.ArrayStore({
								id: 0,
								fields: [
									'name',
									'title'
								],
								data: styleOptions
							}),
							valueField: 'name',
							displayField: 'title',
							allowBlank: false,
							listWidth: 300,
							listeners: {
								select: this.onExtraOptionChange
							}
						};

						// IE is awful with width calculation. Better give it a safe value.
						if (Ext.isIE && (!Ext.ieVersion || Ext.ieVersion < 8)) {
							styleSelect.width = 115;
						}

						this.extraOptionsFieldSet.add(styleSelect);
					}
				}

				// Set ncWMS options
				if (layer.json['datasourceType'] == 'NCWMS') {
					var serviceUrl = layer.json['wmsServiceUrl'];

					var url = serviceUrl + '?' + Ext.urlEncode({
						item: 'layerDetails',
						layerName: layer.json['layerId'],
						request: 'GetMetadata'
					});

					var that = this;
					OpenLayers.loadURL(
						url,
						"",
						this,
						function (result, request) {
							that._setNCWMSOptions(result, request, layer);
						},
						function (result, request) {
							// TODO Error on the page
							var jsonData = Ext.util.JSON.decode(result.responseText);
							var resultMessage = jsonData.data.result;
							alert('Error while loading ncWMS options: ' + resultMessage);
						}
					);
				}

				// User defined in the Manual layer override
				if (layer.json['layerOptions']) {
					Ext.each(layer.json['layerOptions'], function(option) {
						var inputObj = null;

						var inputConfig = {
							fieldLabel: (option['title'] ? option['title'] : option['name']),
							xtype: option['type'],
							name: option['name'],
							listeners: {
								change: this.onExtraOptionChange, // For most fields
								select: this.onExtraOptionChange  // For combobox (dropdown list)
							}
						};
						if (option['type'] === 'datefield') {
							inputConfig.format = 'd/m/Y'; // TODO Get displayFormat from config
						}
						if (option['type'] === 'ux-datefield') {
							inputConfig.format = 'd/m/Y'; // TODO Get displayFormat from config
							inputConfig.outputFormat = 'Y-m-d'; // TODO Get valueFormat from config
						}
						if (option['type'] === 'ux-ncdatetimefield') {
							inputConfig.format = 'd/m/Y'; // TODO Get displayFormat from config
							// Set the URLs to load the available dates & times
							inputConfig.layer = layer;
						}

						inputObj = this.extraOptionsFieldSet.add(inputConfig);

						var extraOptionName = option['name'].toUpperCase();
						var actualValue =
							this.getParameterActualValue(layer, extraOptionName, option['defaultValue']);

						// Set the actual value.
						if (actualValue) {
							inputObj.setValue(actualValue);
						}
					}, this);
				}

				if (layer.json['hasLegend']) {
					if (this.isRendered(layer)) {
						this.legendCheckbox.setValue(!layer.getHideInLegend());
					}
					hasLegendEnabled = true;
				}
			}
			opacityEnabled = true;
		}

		this.showHideOptions(layer, hasLegendEnabled, opacityEnabled);
	},

	// Ignore case sort
	_sortByName: function(a, b) {
		// Move nulls at the end (this should not append)
		if (!a || !a[1]) { return -1; }
		if (!b || !b[1]) { return 1; }

		var lca = a[1].toLowerCase();
		var lcb = b[1].toLowerCase();

		if (lcb > lca) { return -1; }
		// Never equals, we don't want to loose data
		return 1;
	},

	/**
	 * Inspired from godiva2.js
	 * http://localhost:13080/ncWMS/js/godiva2.js
	 */
	_setNCWMSOptions: function(result, request, layer) {
		var layerDetails = Ext.util.JSON.decode(result.responseText);

		// TODO Do something with those
		var units = layerDetails.units;
		var copyright = layerDetails.copyright;
		var palettes = layerDetails.palettes;

		// Z Axis (Elevation/Depth) option
		var zAxis = layerDetails.zaxis;
		if (zAxis != null) {
			var zAxisParam = 'ELEVATION';

			// Currently selected value.
			var zValue =
				this.getParameterActualValue(layer, zAxisParam, 0);

			var zAxisOptions = [];
			var zAxisLabel = (zAxis.positive ? 'Elevation' : 'Depth') + ' (' + zAxis.units + ')';
			// Populate the drop-down list of z values
			// Make z range selector invisible if there are no z values
			var zValues = zAxis.values;
			var zDiff = 1e10; // Set to some ridiculously-high value
			var defaultValue = 0;
			for (var j = 0; j < zValues.length; j++) {
				// Create an item in the drop-down list for this z level
				var zLabel = zAxis.positive ? zValues[j] : -zValues[j];
				zAxisOptions[j] = [zValues[j], zLabel];
				// Find the nearest value to the currently-selected
				// depth level
				var diff = Math.abs(parseFloat(zValues[j]) - zValue);
				if (diff < zDiff) {
					zDiff = diff;
					defaultValue = zValues[j];
				}
			}

			if (zAxisOptions.length > 1) {
				var zAxisSelect = {
					xtype: "combo",
					name: zAxisParam,
					fieldLabel: zAxisLabel,
					value: defaultValue,
					typeAhead: false,
					triggerAction: 'all',
					lazyRender: true,
					mode: 'local',
					store: new Ext.data.ArrayStore({
						id: 0,
						fields: [
							'name',
							'title'
						],
						data: zAxisOptions
					}),
					valueField: 'name',
					displayField: 'title',
					allowBlank: false,
					listeners: {
						select: this.onNCWMSExtraOptionChange
					}
				};
				this.ncwmsOptionsFieldSet.add(zAxisSelect);
			}
		}

		// Set the scale value if this is present in the metadata
		if (typeof layerDetails.scaleRange != 'undefined' &&
				layerDetails.scaleRange != null &&
				layerDetails.scaleRange.length > 1 &&
				layerDetails.scaleRange[0] != layerDetails.scaleRange[1]) {
			var scaleParam = 'COLORSCALERANGE';

			var scaleMinVal = null;
			var scaleMaxVal = null;

			var minmaxField = new Ext.ux.form.MinMaxField({
				fieldLabel: 'Color ranges',
				name: scaleParam,
				decimalPrecision: 4,
				listeners: {
					change: this.onNCWMSExtraOptionChange
				}
			});

			var actualValue =
				this.getParameterActualValue(layer, scaleParam, null);

			if (actualValue != null) {
				var values = actualValue.split(minmaxField.spacer);
				scaleMinVal = values[0];
				scaleMaxVal = values[1];
			}

			if (scaleMinVal == null) {
				scaleMinVal = parseFloat(layerDetails.scaleRange[0]);
			}
			if (scaleMaxVal == null) {
				scaleMaxVal = parseFloat(layerDetails.scaleRange[1]);
			}

			minmaxField.setValue(scaleMinVal, scaleMaxVal);

			this.ncwmsOptionsFieldSet.add(minmaxField);
		}

		this.showHideNcwmsOptions(layer);

		// NCDateTimeField
		// minValue maxValue
	},

	/**
	 * Show/Hide & Enable/Disable layer options
	 */
	showHideOptions: function(layer, hasLegendEnabled, opacityEnabled, html) {
		if (layer) {
			var hasExtraOptions = (this.extraOptionsFieldSet.items.getCount() > 0);

			// Hide the "Empty options" label
			this.headerLabel.hide();
			this.layernameLabel.show();

			// Conditionally show some options
			this.toggleLayerField(this.legendCheckbox, hasLegendEnabled);
			this.toggleLayerField(this.opacitySlider, opacityEnabled);

			// Show the options fieldset
			this.optionsFieldSet.doLayout();
			this.optionsFieldSet.show();

			// Show the extra options fieldset
			if (hasExtraOptions) {
				// doLayout must be called after calling add
				this.extraOptionsFieldSet.doLayout();
				this.extraOptionsFieldSet.show();
			}

			if (this.isRendered(layer)) {
				// Show the options, in read-only mode
				this.optionsFieldSet.enable();

				// Show the extra options, in read-only mode
				if (hasExtraOptions) {
					this.extraOptionsFieldSet.enable();
				}
			} else {
				// Show the options, in read-only mode
				this.optionsFieldSet.disable();

				// Show the extra options, in read-only mode
				if (hasExtraOptions) {
					this.extraOptionsFieldSet.disable();
				}
			}

			// This can only be done once the field set and the slider are visible.
			if (this.isRendered(layer)) {
				this.opacitySlider.setLayer(layer);
			}
		} else {
			// There is no layer

			// Show the "Empty options" label
			this.headerLabel.setText(html ? html : this.defaultContent, false);
			this.headerLabel.show();
			this.layernameLabel.hide();

			// Hide all options
			this.optionsFieldSet.hide();
			this.extraOptionsFieldSet.hide();
			this.ncwmsOptionsFieldSet.hide();
		}
	},

	setContent: function(html) {
		this.showHideOptions(null, null, null, html);
	},

	showHideNcwmsOptions: function(layer) {
		if (layer) {
			var hasNcwmsOptions = (this.ncwmsOptionsFieldSet.items.getCount() > 0);

			// Hide the "Empty options" label
			this.headerLabel.hide();
			this.layernameLabel.show();

			// Show the ncWMS options fieldset
			if (hasNcwmsOptions) {
				// doLayout must be called after calling add
				this.ncwmsOptionsFieldSet.doLayout();
				this.ncwmsOptionsFieldSet.show();

				if (this.isRendered(layer)) {
					this.ncwmsOptionsFieldSet.enable();
				} else {
					this.ncwmsOptionsFieldSet.disable();
				}
			}
		}
	},

	toggleLayerField: function(field, enabled) {
		if (enabled) {
			field.show();
		} else {
			field.hide();
		}
	},

	getParameterActualValue: function(layer, param, defaultValue) {
		if (layer.params && typeof(layer.params[param]) !== 'undefined') {
			return layer.params[param];
		}
		return defaultValue;
	},

	isRendered: function(layer) {
		return (layer.id ? true : false);
	}
});

Ext.reg('atlas_optionspanel', Atlas.OptionsPanel);
