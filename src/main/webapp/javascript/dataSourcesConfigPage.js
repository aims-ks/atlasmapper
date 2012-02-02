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
// http://dev.sencha.com/deploy/ext-4.0.2/examples/writer/writer.html

var dataSourceTypes = {
	'WMS': {
		display: 'WMS',
		qtipHtml: 'Web Map Service (WMS) is a standard protocol for serving georeferenced map images provided by geospatial data applications such as <strong>GeoServer</strong>. This is the most common protocol. You will be asked to provide a GetCapabilities document URL.',
		// Default choice when the window is created
		checked: true,
		// Bold to show the user that this is a common choice
		style: {
			fontWeight: 'bold'
		}
	},
	'NCWMS': {
		display: 'ncWMS',
		qtipHtml: 'ncWMS is a Web Map Service for geospatial data that are stored in <strong>CF-compliant NetCDF</strong> files. This type of data source is not very common. You will be asked to provide a GetCapabilities document URL.'
	},
	'ARCGISWMS': {
		display: 'ArcGIS WMS',
		disabled: true,
		qtipHtml: 'Sorry, not supported yet'
	},
	'GOOGLE': {
		display: 'Google',
		qtipHtml: 'Google base layers:<ul><li>Google Physical</li><li>Google Streets</li><li>Google Hybrid</li><li>Google Satellite</li></ul>'
	},
	'WMTS': {
		display: 'WMTS',
		disabled: true,
		qtipHtml: 'Sorry, not supported yet'
	},
	'KML': {
		display: 'KML',
		qtipHtml: 'KML is the file format used to display geographic data in Earth browser such as <strong>Google Earth</strong>, <strong>Google Maps</strong>, and <strong>Google Maps for mobile</strong>. This application has basic support for this type of data source.'
	},
	'tiles': {
		display: 'Static tiles',
		disabled: true,
		qtipHtml: 'Sorry, not supported yet'
	},
	'XYZ': {
		display: 'XYZ',
		disabled: true,
		qtipHtml: 'Sorry, not supported yet'
	}
};

// Validation type usually used with EditArea
Ext.apply(Ext.form.field.VTypes, {
	uniquedatasourceid: function(val) {
		if (val == null || val == '') {
			// The form will return an error for this field if it doesn't allow null value.
			return false;
		}

		Ext.Ajax.request({
			url: 'dataSourcesConfig.jsp?action=validateNewId',
			method: 'POST',
			params: 'dataSourceId=' + val,
			success: function(o) {
				if (o.responseText === 'true') {
					resetDataSourceIdValidator(true);
				} else {
					Ext.apply(Ext.form.VTypes, {
						uniquedatasourceidText: 'Data source ID already in use.'
					});
					resetDataSourceIdValidator(false);
				}
			},
			failure: function(o) {
				Ext.apply(Ext.form.VTypes, {
					uniquedatasourceidText: 'Communication with the server failed.'
				});
				resetDataSourceIdValidator(false);
			}
		});

		Ext.apply(Ext.form.VTypes, {
			uniquedatasourceidText: 'Validation in progress. Please wait...'
		});
		return false;
	},

	uniquedatasourceidText: 'Invalid data source ID.'
});

function resetDataSourceIdValidator(is_error) {
	Ext.apply(Ext.form.VTypes, {
		uniquedatasourceid : function(val) {
			Ext.Ajax.request({
				url: 'dataSourcesConfig.jsp?action=validateNewId',
				method: 'POST',
				params: 'dataSourceId=' + val,

				success: function(o) {
					if (o.responseText === 'true') {
						resetDataSourceIdValidator(true);
					} else {
						resetDataSourceIdValidator(false);
					}
				},
				failure: function(o) {
					resetDataSourceIdValidator(false);
				}
			});
			return is_error;
		}
	});
}

var frameset = null;

Ext.define('Writer.LayerServerConfigForm', {
	extend: 'Ext.form.Panel',
	alias: 'widget.writerlayerserverconfigform',

	requires: ['Ext.form.field.Text'],
	dataSourceType: null,

	defaultValues: null,

	initComponent: function() {
		this.defaultValues = Ext.create('Writer.LayerServerConfig', {
			'dataSourceType': 'WMS',
			'showInLegend': true,
			'legendParameters': 'FORMAT=image/png\nHEIGHT=10\nWIDTH=20',
			'webCacheParameters': 'LAYERS, TRANSPARENT, SERVICE, VERSION, REQUEST, EXCEPTIONS, FORMAT, SRS, BBOX, WIDTH, HEIGHT'
		});
		this.addEvents('create');


		/** Define items that appair on all Data Source Types **/

		var items = [
			{
				// Grids records must have an unmutable ID
				name: 'id',
				xtype: 'hiddenfield'
			}, {
				fieldLabel: 'Data source type',
				qtipHtml: 'Protocol used by the server to provide the layers. The data source type can not be modified. If you have to change it, you have to create a new data source entry using the values of this one.',
				xtype: 'displayfield',
				value: dataSourceTypes[this.dataSourceType].display
			}, {
				name: 'dataSourceType',
				xtype: 'hiddenfield',
				value: this.dataSourceType
			}, {
				fieldLabel: 'Data source ID',
				qtipHtml: 'This field is used internally to associate layers with a data source. It must be short, to minimise data transfer, and unique amount the other <em>Data source ID</em>. It\'s a good practice to only use lower case letters and number for this field.',
				name: 'dataSourceId',
				xtype: 'ajaxtextfield',
				ajax: {
					url: 'dataSourcesConfig.jsp',
					params: { action: 'validateId' },
					formValues: ['id'], formPanel: this
				},
				anchor: null, size: 15,
				allowBlank: false
			}, {
				fieldLabel: 'Data source name',
				qtipHtml: 'A human readable name for this data source. Must be short and descriptive. This field is used as a title for the tab in the <em>Add layer</em> window',
				name: 'dataSourceName',
				allowBlank: false
			}
		];

		var advancedItems = [];


		/** Define items that appair on some Data Source Types **/

		var browserSpecificEditAreaConfig = {
			xtype: 'editareafield',
			syntax: 'atlasmapperconfig',
			valueType: 'string',
			resizable: {transparent: true}, resizeHandles: 's'
		};
		// As usual, IE required specific workaround. This time, we simply disable the feature.
		if (Ext.isIE) {
			browserSpecificEditAreaConfig = {
				xtype: 'textareafield' // IE takes ages to display the EditArea
			}
		}

		var globalManualOverride = Ext.apply({
				fieldLabel: 'Layers\' global manual override (<a href="manualOverrideDoc.html" target="_blank">doc</a>)',
				qtipTitle: 'Layers\' global manual override',
				qtipHtml: 'JSON configuration used to override the layers information retrieved from the GetCapabilities documents. Every Atlas Mapper clients are affected by these changes.<br/>See the documentation for more information.',
				vtype: 'jsonfield',
				name: 'globalManualOverride',
				height: 400
			}, browserSpecificEditAreaConfig
		);
		var legendParameters = {
			// For special GeoServer legend params, see:
			// http://docs.geoserver.org/latest/en/user/services/wms/get_legend_graphic/legendgraphic.html#raster-legends-explained
			fieldLabel: 'Legend parameters',
			qtipHtml: 'List of URL parameters <i>key=value</i>, separated by coma or new line. Those parameters are added to the URL sent to request the legend graphics.',
			name: 'legendParameters',
			xtype: 'textareafield',
			resizable: {transparent: true}, resizeHandles: 's',
			height: 100
		};

		var blacklistedLayers = {
			fieldLabel: 'Black listed layers',
			qtipHtml: 'List of layer ids, separated by coma or new line. The layers listed here are ignored by all AtlasMapper clients.',
			name: 'blacklistedLayers',
			xtype: 'textareafield',
			resizable: {transparent: true}, resizeHandles: 's',
			height: 100
		};
		var showInLegend = {
			qtipHtml: 'Uncheck this box to disable the legend for all layers provided by this data source. This mean that the layers will not have its legend displayed in the AtlasMapper clients, and they will not have a check box in the layer <em>Options</em> to show its legend.',
			boxLabel: 'Show layers in legend',
			name: 'showInLegend',
			xtype: 'checkboxfield'
		};
		var legendUrl = {
			fieldLabel: 'Legend URL',
			qtipHtml: 'This field override the legend URL provided by the capabilities document. It\'s possible to override this URL by doing one of these:'+
				'<ul>'+
					'<li>leaving this field blank and setting a legend URL (attribute <i>legendUrl</i>) for each layer of this data source, using the <em>Layers\' global manual override</em> field</li>'+
					'<li>providing a legend URL to a static image here, that will be used for each layers of this data source</li>'+
				'</ul>',
			name: 'legendUrl'
		};

		var wmsServiceUrl = {
			fieldLabel: 'WMS service URL',
			qtipHtml: 'URL to the WMS service. This URL is used by a java library to download the WMS capabilities document. Setting this field alone with <em>Data source ID</em> and <em>Data source name</em> is usually enough. Note that a full URL to the capabilities document can also be provided, including additional parameters.',
			name: 'wmsServiceUrl'
		};
		var baseLayers = {
			fieldLabel: 'Base layers',
			qtipHtml: 'List of layer ids, separated by coma or new line. The layers listed here will considered as base layers by all the AtlasMapper clients.',
			name: 'baseLayers',
			xtype: 'textareafield',
			resizable: {transparent: true}, resizeHandles: 's',
			height: 100
		};
		var extraWmsServiceUrls = {
			fieldLabel: 'Secondary WMS service URLs',
			qtipHtml: 'List of URLs, separated by coma or new line. This field can be used to provide additional URLs to access the tiles, which is useful for doing server load balancing and allow the client\'s browser to download more tiles simultaneously.',
			name: 'extraWmsServiceUrls'
		};
		var webCacheUrl = {
			fieldLabel: 'Web Cache URL',
			qtipHtml: 'URL to the Web Cache server. This URL will be used to get the tiles for the layers. If the Web Cache server do not support all URL parameters, you have to provide a list of supported parameters in the next field.',
			name: 'webCacheUrl'
		};
		var webCacheParameters = {
			fieldLabel: 'Web Cache supported URL parameters',
			qtipHtml: 'Coma separated list of URL parameter supported by the Web Cache server. Leave this field blank if the Web Cache server support all parameters. The list of supported URL parameters for GeoWebCache is listed bellow as an example.',
			name: 'webCacheParameters'
		};
		/*
		var webCacheParametersExample = {
			fieldLabel: 'Example: GeoWebCache supported URL parameters',
			labelStyle: 'font-style:italic',
			qtipHtml: 'This is an example of "Web Cache supported URL parameters" for GeoWebCache server.',
			xtype: 'displayfield',
			value: '<em>LAYERS, TRANSPARENT, SERVICE, VERSION, REQUEST, EXCEPTIONS, FORMAT, SRS, BBOX, WIDTH, HEIGHT</em>'
		};
		*/
		var featureRequestsUrl = {
			fieldLabel: 'Feature requests URL',
			qtipHtml: 'This field override the feature requests URL provided by the GetCapabilities document.',
			name: 'featureRequestsUrl'
		};
		var kmlUrls = {
			fieldLabel: 'KML file URLs',
			qtipHtml: 'List of KML file URLs, separated by coma or new line. The file name (without extension) is used for the layer ID and the layer title. The layer title and many other attributes can be modified using the <em>Manual Override</em>.',
			name: 'kmlUrls',
			xtype: 'textareafield',
			resizable: {transparent: true}, resizeHandles: 's',
			height: 100
		};
		var comment = {
			fieldLabel: 'Comment',
			qtipHtml: 'Comment for administrators. This field is not display anywhere else.',
			name: 'comment',
			xtype: 'textareafield',
			resizable: {transparent: true}, resizeHandles: 's',
			height: 100
		};


		// Set the Form items for the different data source types
		switch(this.dataSourceType) {
			case 'WMS':
				items.push(wmsServiceUrl);
				items.push(baseLayers);
				items.push(comment);

				advancedItems.push(globalManualOverride);
				advancedItems.push(blacklistedLayers);
				advancedItems.push(showInLegend);
				advancedItems.push(legendParameters);
				advancedItems.push(legendUrl);
				//advancedItems.push(extraWmsServiceUrls);
				advancedItems.push(webCacheUrl);
				advancedItems.push(webCacheParameters);
				advancedItems.push(featureRequestsUrl);
				break;

			case 'NCWMS':
				items.push(Ext.apply(wmsServiceUrl, { fieldLabel: 'ncWMS service URL' }));
				items.push(baseLayers);
				items.push(comment);

				advancedItems.push(globalManualOverride);
				advancedItems.push(blacklistedLayers);
				advancedItems.push(showInLegend);
				advancedItems.push(legendParameters);
				advancedItems.push(legendUrl);
				//advancedItems.push(extraWmsServiceUrls);
				advancedItems.push(featureRequestsUrl);
				break;

			case 'WMTS':
				items.push(Ext.apply(wmsServiceUrl, { fieldLabel: 'WMTS service URL' }));
				items.push(baseLayers);
				items.push(comment);

				advancedItems.push(globalManualOverride);
				advancedItems.push(blacklistedLayers);
				advancedItems.push(showInLegend);
				advancedItems.push(legendParameters);
				advancedItems.push(legendUrl);
				//advancedItems.push(extraWmsServiceUrls);
				advancedItems.push(featureRequestsUrl);
				break;

			case 'KML':
				items.push(kmlUrls);
				items.push(comment);

				advancedItems.push(globalManualOverride);
				break;

			case 'GOOGLE':
				items.push(comment);

				advancedItems.push(globalManualOverride);
				break;
		}

		var tabs = [{
			// Normal config options
			title: 'General',
			items: items
		}];

		if (advancedItems.length > 0) {
			tabs.push({
				// Advanced config options
				title: 'Advanced',
				items: advancedItems
			});
		}

		Ext.apply(this, {
			activeRecord: null,
			border: false,
			autoScroll: true,
			defaultType: 'textfield',
			fieldDefaults: {
				msgTarget: 'side', // Display an icon next to the field when it's not valid
				anchor: '100%',
				qtipMaxWidth: 200,
				labelAlign: 'right',
				labelWidth: 150,
				hideEmptyLabel: false // Align fields with no label
			},
			items: [{
				xtype: 'tabpanel',
				border: false,
				height: '100%', width: '100%',
				defaults: {
					layout: 'anchor',
					autoScroll: true,
					border: false,
					bodyPadding: 5,
					defaultType: 'textfield'
				},
				items: tabs
			}]
		});

		this.callParent();
	},

	setActiveRecord: function(record){
		var form = this.getForm();
		if (record) {
			this.activeRecord = record;
			form.loadRecord(record);
		} else if (this.defaultValues) {
			this.activeRecord = this.defaultValues;
			form.loadRecord(this.defaultValues);
		} else {
			this.activeRecord = null;
			form.reset();
		}
		// The defaultValues record (and the reset) also change the dataSourceType.
		// It seems to be no way to prevent that (I can't remove the value
		// from the defaultValues instance since the model is unmutable),
		// but it can be restore here.
		form.setValues({dataSourceType: this.dataSourceType});
	},

	onSave: function(){
		frameset.setSavingMessage('Saving...');
		var active = this.activeRecord,
			form = this.getForm();

		if (!active) {
			frameset.setError('The record can not be found.');
			return false;
		}
		if (form.isValid()) {
			// Resync the model instance
			// NOTE: The "active" instance become out of sync everytime the grid get refresh; by creating an instance or updating an other one, for example. The instance can not be saved when it's out of sync.
			active = active.store.getById(active.internalId);
			form.updateRecord(active);
			frameset.setSavedMessage('Data source saved', 500);
			return true;
		}
		frameset.setError('Some fields contains errors.');
		return false;
	},

	onCreate: function(){
		frameset.setSavingMessage('Creating...');
		var form = this.getForm();

		if (form.isValid()) {
			this.fireEvent('create', this, form.getValues());
			form.reset();
			frameset.setSavedMessage('Data source created', 500);
			return true;
		}
		frameset.setError('Some fields contains errors.');
		return false;
	},

	reset: function(){
		this.getForm().reset();
		this.setActiveRecord(this.defaultValues);
	},

	onReset: function(){
		this.reset();
	}
});

Ext.define('Writer.LayerServerConfigGrid', {
	extend: 'Ext.grid.Panel',
	alias: 'widget.writerlayerserverconfiggrid',

	requires: [
		'Ext.form.field.Text',
		'Ext.toolbar.TextItem'
	],

	initComponent: function(){

		Ext.apply(this, {
			iconCls: 'icon-grid',
			dockedItems: [
				{
					xtype: 'toolbar',
					items: [
						{
							iconCls: 'icon-add',
							text: 'Add',
							scope: this,
							handler: this.onAddClick
						}, {
							iconCls: 'icon-delete',
							text: 'Delete',
							disabled: true,
							itemId: 'delete',
							scope: this,
							handler: this.onDeleteClick
						}
					]
				}
			],
			columns: [
				{
					header: 'Data source ID',
					width: 100,
					sortable: true,
					dataIndex: 'dataSourceId'
				}, {
					header: 'Data source name',
					flex: 1,
					sortable: true,
					dataIndex: 'dataSourceName'
				}, {
					// http://docs.sencha.com/ext-js/4-0/#/api/Ext.grid.column.Action
					header: 'Actions',
					xtype:'actioncolumn',
					width:50,
					defaults: {
						iconCls: 'grid-icon'
					},
					items: [
						{
							icon: '../resources/icons/edit.png',

							// Bug: defaults is ignored (http://www.sencha.com/forum/showthread.php?138446-actioncolumn-ignore-defaults)
							iconCls: 'grid-icon',

							tooltip: 'Edit',
							scope: this,
							handler: function(grid, rowIndex, colIndex) {
								var rec = grid.getStore().getAt(rowIndex);
								if (rec) {
									var dataSourceType = rec.data.dataSourceType;
									if (dataSourceType) {
										// Close the all windows and show the edit window.
										var window = this.getLayerConfigFormWindow(dataSourceType, 'save');
										if (window) {
											window.child('.form').setActiveRecord(rec);
											window.show();
										} else {
											frameset.setError('Unsupported data source type ['+dataSourceType+']');
										}
									}
								} else {
									frameset.setError('No record has been selected.');
								}
							}
						}
					]
				}
			]
		});
		this.callParent();
		this.getSelectionModel().on('selectionchange', this.onSelectChange, this);
	},

	getDataSourceTypeFormWindow: function() {
		// Create the radio buttons for each layer type
		var dataSourceTypesItems = [];
		for (var dataSourceType in dataSourceTypes) {
			if (dataSourceTypes.hasOwnProperty(dataSourceType)) {
				var item = {
					name: 'dataSourceType',
					boxLabel: dataSourceTypes[dataSourceType].display,
					inputValue: dataSourceType
				};
				// Apply the other fields to the item
				// qtipHtml, checked, style, disabled, etc.
				Ext.apply(item, dataSourceTypes[dataSourceType]);
				dataSourceTypesItems.push(item);
			}
		}

		var that = this;
		return Ext.create('Ext.window.Window', {
			title: 'New data source',
			width: 300,
			layout: 'fit',
			constrainHeader: true,
			closeAction: 'destroy',

			items: [
				{
					xtype: 'form',
					border: false,
					bodyPadding: 5,
					fieldDefaults: {
						anchor: '100%',
						labelAlign: 'right',
						labelWidth: 100
					},
					items: [
						{
							xtype: 'displayfield',
							value: 'This window configure the type of the new data source. It\'s used by the application to know which protocol to use to get the layers from the server.'
						}, {
							xtype: 'radiogroup',
							fieldLabel: 'Data source type',
							columns: 1,
							items: dataSourceTypesItems
						}
					]
				}
			],
			buttons: [
				{
					text: 'Next',
					handler: function() {
						var window = this.ownerCt.ownerCt;
						var form = window.child('form').getForm();
						if (form.isValid()) {
							// Close the current form window and show the next one
							var values = form.getFieldValues();
							var newWindow = that.getLayerConfigFormWindow(values.dataSourceType, 'add');
							if (newWindow) {
								// Apply default values
								newWindow.child('.form').reset();
								newWindow.show();
								window.close();
							} else {
								frameset.setError('Unsupported dataset type ['+dataSourceType+']');
							}
						} else {
							frameset.setError('Some fields contains errors.');
						}
					}
				}, {
					text: 'Cancel',
					handler: function() {
						var window = this.ownerCt.ownerCt;
						window.close();
					}
				}
			]
		});
	},

	getLayerConfigFormWindow: function(dataSourceType, action) {
		if (!dataSourceTypes[dataSourceType]) { return null; }

		var that = this;
		var buttons = [];
		if (action == 'save') {
			buttons = [
				{
					iconCls: 'icon-save',
					text: 'Apply',
					handler: function() {
						var window = this.ownerCt.ownerCt;
						var form = window.child('.form');
						if (form.onSave()) {
							window.close();
						}
					}
				}, {
					text: 'Cancel',
					handler: function() {
						var window = this.ownerCt.ownerCt;
						window.close();
					}
				}
			];
		} else {
			buttons = [
				{
					//iconCls: 'icon-back',
					text: 'Back',
					handler: function() {
						that.getDataSourceTypeFormWindow().show();
						this.ownerCt.ownerCt.close();
					}
				}, {
					iconCls: 'icon-add',
					text: 'Create',
					handler: function() {
						var window = this.ownerCt.ownerCt;
						var form = window.child('.form');
						if (form.onCreate()) {
							window.close();
						}
					}
				}, {
					text: 'Cancel',
					handler: function() {
						var window = this.ownerCt.ownerCt;
						window.close();
					}
				}
			];
		}

		return Ext.create('Ext.window.Window', {
			title: 'Data source configuration',
			width: 700,
			height: 430,
			maxHeight: Ext.getBody().getViewSize().height,
			layout: 'fit',
			constrainHeader: true,
			closeAction: 'hide',
			dataSourceType: dataSourceType,

			items: [
				{
					xtype: 'writerlayerserverconfigform',
					dataSourceType: dataSourceType,
					listeners: {
						scope: this,
						create: function(form, data){
							// Insert the data into the model.
							// If something goes wrong, the store will
							// manage the exception.
							this.store.insert(0, data);
						}
					}
				}
			],
			buttons: buttons
		});
	},

	onSelectChange: function(selModel, selections){
		this.down('#delete').setDisabled(selections.length === 0);
	},

	onAddClick: function(){
		this.getDataSourceTypeFormWindow().show();
	},

	onDeleteClick: function() {
		var selection = this.getView().getSelectionModel().getSelection()[0];
		if (selection) {
			var dataSourceName = (selection.data ? selection.data.dataSourceName : 'UNKNOWN');
			var confirm = Ext.MessageBox.confirm(
				'Confirm',
				'The layers provided by this data source will still be available '+
				'for the clients that are using it, until those clients are re-generated.<br/>'+
				'This operation can not be undone.<br/>'+
				'<br/>'+
				'Are you sure you want to delete the data source <b>'+dataSourceName+'</b>?',
				function(btn) {
					if (btn == 'yes') {
						this.onDeleteConfirmed();
					}
				},
				this
			);
			// Set "No" as default
			confirm.defaultButton = 2;
		} else {
			frameset.setError('Can not find the client to delete');
		}
	},

	onDeleteConfirmed: function() {
		frameset.setSavingMessage('Deleting a data source...');
		var selection = this.getView().getSelectionModel().getSelection()[0];
		if (selection) {
			var dataSourceName = (selection.data ? selection.data.dataSourceName : 'UNKNOWN');
			this.store.remove(selection);
			frameset.setSavedMessage('Data source <b>'+dataSourceName+'</b> deleted');
		} else {
			frameset.setError('Can not find the data source to delete');
		}
	}
});

Ext.define('Writer.LayerServerConfig', {
	extend: 'Ext.data.Model',
	// Grids records must have an unmutable ID

	// Default values:
	// Set default values in the defaultValues instance (lines 80~100).
	// Only set default value "false" for checkboxes:
	// The form do not return the value when they are unchecked,
	// forcing the model to set them to their 'model default value'.
	fields: [
		{name: 'id', type: 'int', useNull: true},
		{name: 'dataSourceId', sortType: 'asUCString'},
		{name: 'dataSourceName', sortType: 'asUCString'},
		{name: 'dataSourceType', type: 'string'},
		'wmsServiceUrl',
		'extraWmsServiceUrls',
		'kmlUrls',
		'webCacheUrl',
		'webCacheParameters',
		'featureRequestsUrl',
		'legendUrl',
		'legendParameters',
		'blacklistedLayers',
		'baseLayers',
		'globalManualOverride',
		{name: 'showInLegend', type: 'boolean', defaultValue: false},
		'comment'
	],
	validations: [{
		field: 'dataSourceId',
		type: 'length',
		min: 1
	}, {
		field: 'dataSourceName',
		type: 'length',
		min: 1
	}, {
		field: 'dataSourceType',
		type: 'length',
		min: 1
	}]
});

Ext.require([
	'Ext.data.*',
	'Ext.tip.QuickTipManager',
	'Ext.window.MessageBox'
]);


Ext.onReady(function(){
	Ext.tip.QuickTipManager.init();

	frameset = new Frameset();
	frameset.setContentTitle('Data sources configuration');
	frameset.addContentDescription('<img src="../resources/images/json-small.jpg" style="float:right; width: 200px; height: 132px"> Each <i>data source</i> specifies a set of layers that can be used in one or more of the <i>clients</i>. It specifies the type (<i>WMS</i>, <i>ncWMS</i>, <i>Google</i>, <i>KML</i>, etc) and source of the data as well as any manual settings for the layers, such as modifying the <i>title</i>, <i>description</i> or <i>path</i> in the tree. For <i>WMS</i> data sources a <i>tile cache service</i> can be linked with a full <i>WMS service</i> so that imagery is requested from the tile server, but feature requests are sent to the full <i>WMS</i>.');
	frameset.render(document.body);

	var store = Ext.create('Ext.data.Store', {
		model: 'Writer.LayerServerConfig',
		autoLoad: true,
		autoSync: true,
		proxy: {
			type: 'ajax',
			api: {
				read: 'dataSourcesConfig.jsp?action=read',
				create: 'dataSourcesConfig.jsp?action=create',
				update: 'dataSourcesConfig.jsp?action=update',
				destroy: 'dataSourcesConfig.jsp?action=destroy'
			},
			reader: {
				type: 'json',
				successProperty: 'success',
				root: 'data',
				messageProperty: 'message'
			},
			writer: {
				type: 'json',
				writeAllFields: false,
				root: 'data'
			},
			listeners: {
				exception: function(proxy, response, operation){
					var responseObj = null;
					var statusCode = response ? response.status : null;
					if (response && response.responseText) {
						try {
							responseObj = Ext.decode(response.responseText);
						} catch (err) {
							responseObj = {errors: [err.toString()]};
						}
					}
					var operStr = 'UNKNOWN';
					if (operation && operation.action) { operStr = operation.action; }
					frameset.setErrors('An error occurred while executing the operation ['+operStr+'] on data sources.', responseObj, statusCode);
				}
			}
		}
	});

	var layersConfigGrid = Ext.create('Writer.LayerServerConfigGrid', {
		itemId: 'grid',
		height: 400,
		resizable: true, resizeHandles: 's',
		store: store
	});

	frameset.addContent(layersConfigGrid);
});
