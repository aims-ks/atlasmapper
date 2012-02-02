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

	currentNode: null,

	initComponent: function(){
		var that = this;

		if (this.html) {
			this.defaultContent = this.html;
			this.html = '';
		}

		var locateButton = new Ext.Button({
			text: 'Locate',
			handler: function() {
				that.mapPanel.ol_fireEvent('locateLayer', {layer: (that.currentNode ? that.currentNode.layer : null)});
			}
		});

		this.headerLabel = new Ext.form.Label({
			cls: 'emptyInfo',
			html: this.defaultContent
		});

		// TODO layerNameLabel
		this.layernameLabel = new Ext.form.Label({
			cls: 'layerNameLabel',
			html: '',
			hidden: true
		});

		this.legendCheckbox = new Ext.form.Checkbox({
			fieldLabel: 'Show in legend',
			handler: function(checkbox, checked) {
				if (that.currentNode && that.currentNode.layer && typeof(that.currentNode.layer.setHideInLegend) === 'function') {
					that.currentNode.layer.setHideInLegend(!checked);
				}
			},
			checked: true,
			hidden: true
		});

		// Update the checkbox status when the function layer.setHideInLegend is called
		// NOTE: layer.setHideInLegend is defined in MapPanel
		this.mapPanel.ol_on('legendVisibilityChange', function(evt) {
			if (that.currentNode && that.currentNode.layer && evt.layer == that.currentNode.layer) {
				that.legendCheckbox.setValue(!that.currentNode.layer.hideInLegend);
			}
		});

		this.opacitySlider = new GeoExt.ux.GroupLayerOpacitySlider({
			fieldLabel: 'Opacity',
			node: this.currentNode,
			aggressive: false,
			animate: false,
			// This option do not works with group layers
			//changeVisibility: true,
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

		// Determine if the layer need a reload by comparing the values
		// of the new parameters with the one in the layer URL.
		// layer: OpenLayers layer
		// newParams: Map of key value pairs
		// private
		this.changeLayerParams = function(layer, newParams) {
			// Change the URL of the layer to use the appropriate server
			// NOTE: setUrl must be called before mergeNewParams (mergeNewParams reload the tiles, setUrl don't; when called in wrong order, tiles are requested against the wrong server)
			var needReload = false;

			var newUrl = that.mapPanel.getWMSServiceUrl(layer.json, that._mergeParams(layer.params, newParams));
			if (newUrl != layer.url) {
				layer.setUrl(newUrl);
				needReload = true;
			}

			// Loop through all params and check if it's value is the
			// same as the one set for the layer. If not, ask for a
			// layer reload (stop as soon as one is different)
			if (!needReload) {
				var currentValue = null;
				Ext.iterate(newParams, function(key, value) {
					currentValue = that.getParameterActualValue(layer, key, null);
					if (currentValue != value) {
						needReload = true;
						// Stop the iteration
						return false;
					}
				});
			}

			if (needReload) {
				// Merge params add the new params or change the values
				// of existing one and reload the tiles.
				layer.mergeNewParams(newParams);
			}
		};

		this.onExtraOptionChange = function() {
			if (that.extraOptionsFieldSet && that.extraOptionsFieldSet.items) {
				var layer = (that.currentNode ? that.currentNode.layer : null);
				if (layer && layer.mergeNewParams) {
					var newParams = {};
					that.extraOptionsFieldSet.items.each(function(option) {
						var optionName = option.getName();
						if (option && optionName) {
							//var optionType = option.getXType();
							var optionValue = option.getValue();

							// Set the new parameter, or unset it if it has a null value (don't remove STYLES - it's mandatory).
							if (optionName != 'STYLES' && (typeof(optionValue) == 'undefined' || optionValue == null || optionValue == '')) {
								// Remove the param from the URL - Some server don't like to have en empty parameter
								// NOTE: OpenLayers filter null values
								newParams[optionName] = null;
							} else {
								newParams[optionName] = optionValue;
							}
						}
					});

					that.changeLayerParams(layer, newParams);
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
				var layer = (that.currentNode ? that.currentNode.layer : null);
				if (layer && layer.mergeNewParams) {
					var newParams = {};
					that.ncwmsOptionsFieldSet.items.each(function(option) {
						var optionName = option.getName();
						if (option && optionName) {
							//var optionType = option.getXType();
							var optionValue = option.getValue();

							// Set the new parameter, or unset it if it has a null value (don't remove STYLES - it's mandatory).
							if (optionName != 'STYLES' && (typeof(optionValue) == 'undefined' || optionValue == null || optionValue == '')) {
								// Remove the param from the URL - Some server don't like to have en empty parameter
								// NOTE: OpenLayers filter null values
								newParams[optionName] = null;
							} else {
								newParams[optionName] = optionValue;
							}
						}
					});

					that.changeLayerParams(layer, newParams);
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

	// Return a new map resulting of the merge of both maps.
	// This is required to determine is the WebCache URL can be used. Calling
	// layer.merge reload the tiles before the layer URL actually get modified.
	_mergeParams: function(layerParams, newParams) {
		var result = {};
		var paramName = "";
		for(paramName in layerParams){
			if(layerParams.hasOwnProperty(paramName)){
				result[paramName] = layerParams[paramName];
			}
		}
		for(paramName in newParams){
			if(newParams.hasOwnProperty(paramName)){
				result[paramName] = newParams[paramName];
			}
		}
		return result;
	},

	/**
	 * Auto-set the options for a given layer.
	 */
	setLayerOptions: function(node) {
		var layer = (node ? node.layer : null);
		var hasLegendEnabled = false;
		var opacityEnabled = false;

		// Delete (remove & destroy) previous extra options, if any
		this.extraOptionsFieldSet.hide();
		this.extraOptionsFieldSet.removeAll(true);

		this.ncwmsOptionsFieldSet.hide();
		this.ncwmsOptionsFieldSet.removeAll(true);

		this.currentNode = node;

		if (layer) {
			if (layer.json) {
				this.layernameLabel.setText(layer.json['title'], false);

				// Set extra options for the selected layer

				// Styles dropdown
				if (layer.json['styles']) {
					var styleOptionName = 'STYLES';
					var styleOptions = [];
					Ext.iterate(layer.json['styles'], function(styleName, jsonStyle) {
						// BUG: Duplicate names can not be select with the ExtJS select option.
						// Temporary(?) solution: Add a sequential number in front of it.
						var styleTitle = (jsonStyle['title'] ? jsonStyle['title'] : styleName);
						// TODO Display de description on a box when a style is selected
						var styleDescription = jsonStyle['description'];

						styleOptions[styleOptions.length] = [styleName, styleTitle];
					});

					var currentValue =
						this.getParameterActualValue(layer, styleOptionName, "");

					if (styleOptions.length > 1) {
						// Sort styles
						styleOptions.sort(this._sortByName);

						// Fancy style name formatting
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
							value: currentValue,
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
				if (layer.json['dataSourceType'] == 'NCWMS') {
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
							var resultMessage = 'Unknown error';
							try {
								var jsonData = Ext.util.JSON.decode(result.responseText);
								resultMessage = jsonData.data.result;
							} catch (err) {
								resultMessage = result.responseText;
							}
							// TODO Error on the page
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
							inputConfig.dateFormat = 'd/m/Y'; // TODO Get displayFormat from config
							inputConfig.timeFormat = 'H:i:s'; // TODO Get displayFormat from config
							// Set the URLs to load the available dates & times
							inputConfig.layer = layer;
						}

						// Greg's ux-ncplotpanel configuration
						if (option['type'] === 'ux-ncplotpanel') {
							inputConfig.format = 'd/m/Y'; // TODO Get displayFormat from config
							inputConfig.layer = layer;
							inputConfig.mapPanel = that.mapPanel;
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

				if (layer.json['hasLegend'] && typeof(layer.getHideInLegend) === 'function' && typeof(layer.setHideInLegend) === 'function') {
					if (this.isRendered(layer)) {
						this.legendCheckbox.setValue(!layer.getHideInLegend());
					}
					hasLegendEnabled = true;
				}
			}
			opacityEnabled = true;
		}

		this.showHideOptions(node, hasLegendEnabled, opacityEnabled);
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
	 * Inspired on godiva2.js
	 * http://localhost:13080/ncWMS/js/godiva2.js
	 */
	_setNCWMSOptions: function(result, request, layer) {
		if (layer == (this.currentNode ? this.currentNode.layer : null)) {
			var layerDetails = null;
			try {
				layerDetails = Ext.util.JSON.decode(result.responseText);
			} catch (err) {
				var resultMessage = result.responseText;
				// TODO Error on the page
				alert('Error while loading ncWMS options: ' + resultMessage);
				return;
			}
			
			if (layerDetails == null) {
				return;
			}

			if (layerDetails.exception) {
				// TODO Error on the page
				alert('Error while loading ncWMS options: ' +
					(layerDetails.exception.message ? layerDetails.exception.message : layerDetails.exception));
				return;
			}

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
		}
	},

	/**
	 * Show/Hide & Enable/Disable layer options
	 */
	showHideOptions: function(node, hasLegendEnabled, opacityEnabled, html) {
		var layer = (node ? node.layer : null);
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
				this.opacitySlider.setNode(node);
			}

			if (!Ext.isIE6) {
				this.doLayout();
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

			if (!Ext.isIE6) {
				this.doLayout();
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
		if (!layer.params) {
			return defaultValue;
		}

		if (typeof(layer.params[param]) !== 'undefined') {
			return layer.params[param];
		}

		// Try with to uppercase the parameter; OpenLayers usually put all parameters in uppercase.
		var uppercaseParam = param.toUpperCase();
		if (typeof(layer.params[uppercaseParam]) !== 'undefined') {
			return layer.params[uppercaseParam];
		}

		return defaultValue;
	},

	isRendered: function(layer) {
		return (typeof(layer.id) === 'undefined' ? false : true);
	}
});

Ext.reg('atlas_optionspanel', Atlas.OptionsPanel);
