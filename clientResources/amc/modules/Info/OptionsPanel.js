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

	locateButton: null,

	headerLabel: null,
	optionsFieldSet: null,
	// Options related to the layer type (see clientResources/amc/modules/MapPanel/Layer/NCWMS.js for an example)
	specificOptionsFieldSet: null,
	// Extra options to set user defined URL parameters.
	extraOptionsFieldSet: null,

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

		this.locateButton = new Ext.Button({
			text: 'Locate',
			handler: function() {
				that.mapPanel.ol_fireEvent('locateLayer', {layer: (that.currentNode ? that.currentNode.layer : null)});
			}
		});

		this.headerLabel = new Ext.form.Label({
			cls: 'emptyInfo',
			html: this.defaultContent
		});

		// TODO change variable name to layerNameLabel
		this.layernameLabel = new Ext.form.Label({
			cls: 'layerNameLabel',
			html: '',
			hidden: true
		});

		this.legendCheckbox = new Ext.form.Checkbox({
			fieldLabel: 'Show in legend',
			handler: function(checkbox, checked) {
				if (that.currentNode && that.currentNode.layer && that.currentNode.layer.atlasLayer && typeof(that.currentNode.layer.atlasLayer.setHideInLegend) === 'function') {
					that.currentNode.layer.atlasLayer.setHideInLegend(!checked);
				}
			},
			checked: true,
			hidden: true
		});

		// Update the checkbox status when the function layer.atlasLayer.setHideInLegend is called
		// NOTE: layer.atlasLayer.setHideInLegend is defined in Layer.AbstractLayer
		this.mapPanel.ol_on('legendVisibilityChange', function(evt) {
			if (that.currentNode && that.currentNode.layer && evt.layer == that.currentNode.layer) {
				that.legendCheckbox.setValue(!that.currentNode.layer.hideInLegend);
			}
		});

		var opacitySliderConfig = {
			fieldLabel: 'Opacity',
			node: this.currentNode,
			aggressive: false,
			animate: false,
			// This option do not works with group layers
			//changeVisibility: true,
			value: 0,
			plugins: new GeoExt.LayerOpacitySliderTip({template: '<div>{opacity}%</div>'})
		};

		// Fix the bug of the thumb on top of other elements.
		// WARNING: topThumbZIndex is not in the API doc of ExtJS.
		// IE NOTE: This bug do not occur with IE6 & 7, and the fix give errors,
		//     but the bug do occur with IE8 and later.
		if (!Ext.isIE6 && !Ext.isIE7) {
			opacitySliderConfig.topThumbZIndex = 'auto';
		}

		this.opacitySlider = new GeoExt.ux.GroupLayerOpacitySlider(opacitySliderConfig);

		// The opacity slider (thumb) goes to 0 when its value is
		// changed and it's not visible. This event prevent that bug.
		// NOTE: This event listener is automatically destroy when the
		//     panel is destroyed. The opacity slider should not be
		//     destroyed before that.
		this.on({
			'show': function() {
				this.opacitySlider.syncThumb();
			},
			scope: this
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
				this.locateButton
			]
		});

		this.onExtraOptionChange = function() {
			if (that.extraOptionsFieldSet && that.extraOptionsFieldSet.items) {
				var layer = (that.currentNode ? that.currentNode.layer : null);
				if (layer && layer.atlasLayer) {
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

					layer.atlasLayer.setParameters(newParams);
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

		this.specificOptionsFieldSet = new Ext.form.FieldSet({
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
				this.specificOptionsFieldSet
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
	 * Options: Fields in the options panel to control the layer URL parameters (such as CQL filter, time, depth, etc.)
	 */
	setLayerOptions: function(node) {
		var layer = (node ? node.layer : null);
		var hasLegendEnabled = false;
		var opacityEnabled = false;

		// Delete (remove & destroy) previous extra options, if any
		this.extraOptionsFieldSet.hide();
		this.extraOptionsFieldSet.removeAll(true);

		this.specificOptionsFieldSet.hide();
		this.specificOptionsFieldSet.removeAll(true);

		this.currentNode = node;

		if (layer) {
			if (layer.atlasLayer && typeof(layer.atlasLayer.canBeLocated) === 'function' && layer.atlasLayer.canBeLocated()) {
				this.locateButton.enable();
			} else {
				this.locateButton.disable();
			}

			if (layer.atlasLayer && layer.atlasLayer.json) {
				this.layernameLabel.setText(layer.atlasLayer._safeHtml(layer.atlasLayer.getTitle()), false);

				// Set extra options for the selected layer

				// Styles dropdown
				if (layer.atlasLayer.json['styles']) {
					var styleOptionName = 'STYLES';
					var styleOptions = [];
					Ext.each(layer.atlasLayer.json['styles'], function(jsonStyle) {
						var styleName = jsonStyle['name'];
						var styleTitle = (jsonStyle['title'] ? jsonStyle['title'] : styleName);
						// TODO Display de description on a box when a style is selected
						var styleDescription = jsonStyle['description'];

						// Remove the style name for the default style (that style need to be requested without Style parameter)
						if (jsonStyle['default'] === true) {
							styleName = '';
						}

						styleOptions[styleOptions.length] = [styleName, styleTitle];
					});

					var currentValue = layer.atlasLayer.getParameter(styleOptionName, "");

					if (styleOptions.length > 1) {
						// Sort styles
						styleOptions.sort(this._sortByName);

						// BUG: Duplicate names can not be select with the ExtJS select option.
						// Temporary(?) solution: Add a sequential number in front of it.

						// Fancy style name formatting
						Ext.each(styleOptions, function(style, index) {
							// style[0] = value
							// style[1] = label
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
							editable: false,
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

				// Set layer specific options (see clientResources/amc/modules/MapPanel/Layer/NCWMS.js for an example)
				layer.atlasLayer.setOptions(this);

				// User defined options in the Manual layer override
				// Default values are setup in AbstractLayer
				if (layer.atlasLayer.json['options']) {
					Ext.each(layer.atlasLayer.json['options'], function(option) {
						if (option && option['name']) {
							var inputObj = null;

							var inputConfig = {
								fieldLabel: (option['title'] ? option['title'] : option['name']),
								xtype: option['type'],
								name: option['name'],
								listeners: {
									scope: this,
									change: this.onExtraOptionChange, // For most fields
									select: this.onExtraOptionChange, // For combobox (dropdown list)
									specialkey: function(field, event) {
										if (event.getKey() == event.ENTER) {
											this.onExtraOptionChange();
										}
									}
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

							var extraOptionName = option['name'].toUpperCase();
							var actualValue = layer.atlasLayer.getParameter(extraOptionName, null);

							// Set the actual value.
							if (actualValue) {
								inputConfig.value = decodeURIComponent(actualValue);
							}

							inputObj = this.extraOptionsFieldSet.add(inputConfig);
						}
					}, this);
				}

				if (layer.atlasLayer.json['hasLegend'] && layer.atlasLayer && typeof(layer.atlasLayer.getHideInLegend) === 'function') {
					if (this.isRendered(layer)) {
						this.legendCheckbox.setValue(!layer.atlasLayer.getHideInLegend());
					}
					hasLegendEnabled = true;
				}
			}
			opacityEnabled = true;
		} else {
			this.locateButton.disable();
		}

		this.showHideOptions(node, hasLegendEnabled, opacityEnabled);
	},

	addOption: function(atlasLayer, extJSFormFieldConfig) {
		var extJSFormField = this.specificOptionsFieldSet.add(extJSFormFieldConfig);
		this.showHideSpecificOptions(atlasLayer);
		return extJSFormField;
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
				if (typeof(this.opacitySlider.setNode) === 'function') {
					this.opacitySlider.setNode(node);
				} else {
					this.opacitySlider.setLayer(layer);
				}
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
			this.specificOptionsFieldSet.hide();
		}
	},

	setContent: function(html) {
		this.showHideOptions(null, null, null, html);
	},

	showHideSpecificOptions: function(atlasLayer) {
		if (atlasLayer.layer) {
			// Show the options fieldset
			if (this.specificOptionsFieldSet.items.getCount() > 0) {
				// Hide the "Empty options" label
				this.headerLabel.hide();
				this.layernameLabel.show();

				// doLayout must be called after calling add
				this.specificOptionsFieldSet.doLayout();
				this.specificOptionsFieldSet.show();

				if (this.isRendered(atlasLayer.layer)) {
					this.specificOptionsFieldSet.enable();
				} else {
					this.specificOptionsFieldSet.disable();
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

	isRendered: function(layer) {
		return (typeof(layer.id) !== 'undefined');
	}
});

Ext.reg('atlas_optionspanel', Atlas.OptionsPanel);
