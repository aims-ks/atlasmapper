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

// A function that return a function
// Object can not share the same instance of a function.
// A fresh copy has to be created for each class.

/*
NOTE: Add a button to a grid field (better use a header toolbar...

-- Column item
{
	header: 'Config',
	width: 150,
	sortable: false,
	dataIndex: 'clientId',

	// http://techmix.net/blog/2010/11/25/add-button-to-extjs-gridpanel-cell-using-renderer/
	renderer: function(value, metaData, record) {
		var button = new Ext.Button({
			text: 'Preview',
			//iconCls: IconManager.getIcon('package_add'),
			handler : function(btn, e) {
				// do whatever you want here
			}
		});

		// Generate a new ID
		var newId = Ext.id();
		// Similar to window.setTimeout
		Ext.Function.defer(function() { button.render(document.body, newId); }, 1);

		return '<div id="' + newId + '"></div>';
	}
}
*/

var frameset = null;
var timeoutPerClient = 300000; // 5 minutes

// Define the model for a projection
Ext.regModel('projection', {
	fields: [
		{type: 'string', name: 'name'},
		{type: 'string', name: 'title'}
	]
});

// The data store holding the projections
var projectionsStore = Ext.create('Ext.data.Store', {
	model: 'projection',
	autoLoad: true,
	proxy: {
		type: 'ajax',
		api: {
			read: 'clientsConfig.jsp?action=GETPROJECTIONS'
		},
		reader: {
			type: 'json',
			successProperty: 'success',
			root: 'data',
			messageProperty: 'message'
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
				frameset.setErrors('An error occurred while executing the operation ['+operStr+'] on clients.', responseObj, statusCode);
			}
		}
	}
});

Ext.define('Writer.ClientConfigForm', {
	extend: 'Ext.form.Panel',
	alias: 'widget.writerclientconfigform',

	requires: ['Ext.form.field.Text'],

	defaultValues: null,

	initComponent: function(){
		this.defaultValues = Ext.create('Writer.ClientConfig', {
			'projection': 'EPSG:4326',
			'longitude': 0,
			'latitude': 0,
			'zoom': 6,
			'default': false,
			'fullClientEnable': true,
			'fullClientModules': ['Info', 'Tree'], // TODO Delete this once this field is implemented
			'baseLayersInTab': true,
			'useLayerService': true,
			'theme': '',
			'enable': true
		});
		this.addEvents('create');

		var dataSourcesItems = [];
		// NOTE: data sources variable is set in clientsConfigPage.jsp
		// I don't want to do an Ajax query to get those...
		Ext.iterate(dataSources, function(dataSourceId, dataSourceName) {
			dataSourcesItems.push({
				name: 'dataSources',
				boxLabel: dataSourceName,
				inputValue: dataSourceId
			});
		});

		var fullClientModules = [];
		var embeddedClientModules = [];
		// NOTE: modules variable is set in clientsConfigPage.jsp
		// I don't want to do an Ajax query to get those...
		Ext.iterate(modules, function(moduleId, moduleConfig) {
			fullClientModules.push(Ext.apply({
				name: 'fullClientModules',
				inputValue: moduleId
			}, moduleConfig));
			embeddedClientModules.push(Ext.apply({
				name: 'embeddedClientModules',
				inputValue: moduleId
			}, moduleConfig));
		});

		var browserSpecificEditAreaConfig = {
			xtype: 'editareafield',
			valueType: 'string',
			syntax: 'atlasmapperconfig',
			resizable: true, resizeHandles: 's'
		}
		// As usual, IE required specific workaround. This time, we simply disable the feature.
		if (Ext.isIE) {
			browserSpecificEditAreaConfig = {
				xtype: 'textareafield' // IE takes ages to display the EditArea
			}
		}

		var validateDependencies = function(value) {
			var field = this;
			if (value == '' && field.dependencies) {
				for (var i=0; i < field.dependencies.length; i++) {
					var dependField = field.up('form').down('#' + field.dependencies[i]);
					if (dependField != null) {
						if (dependField.getValue() != '') {
							return '<i>' + (field.fieldLabel ? field.fieldLabel : field.getName()) + '</i> can not be empty when <i>' + (dependField.fieldLabel ? dependField.fieldLabel : dependField.getName()) + '</i> is not empty.';
						}
					}
				}
			}
			return true;
		};

		var notAvailableInDemoMode = demoMode ? "<br/><strong>This function is not available in the Demo version.</strong>" : "";

		Ext.apply(this, {
			activeRecord: null,
			border: false,
			fieldDefaults: {
				msgTarget: 'side', // Display an (X) icon next to the field when it's not valid
				qtipMaxWidth: 200,
				anchor: '100%',
				labelAlign: 'right',
				labelWidth: 150
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
				items: [
					{
						// Normal config options
						title: 'General',
						items: [
							{
								// Grids records must have an unmutable ID
								name: 'id',
								xtype: 'hiddenfield'
							}, {
								fieldLabel: ' ', labelSeparator: '',
								boxLabel: 'Enable this client',
								qtipHtml: 'The application ignore the entries which has this box unchecked.',
								name: 'enable',
								xtype: 'checkboxfield'
							}, {
								fieldLabel: 'Client ID',
								name: 'clientId',
								qtipHtml: 'This field is used for the client folder and as a reference in this interface, for the administrator.',
								xtype: 'ajaxtextfield',
								ajax: {
									url: 'clientsConfig.jsp',
									params: { action: 'validateId' },
									formValues: ['id'], formPanel: this
								},
								allowBlank: false
							}, {
								fieldLabel: 'Client Name',
								name: 'clientName',
								qtipHtml: 'A human readable name for this client. This field is used as a title for the Atlas Mapper client and in error/warning messages.'
							}, {
								xtype: 'checkboxgroup',
								fieldLabel: 'Data source type',
								columns: 2,
								items: dataSourcesItems
							}, {
								title: 'Modules for the full client(s)',
								qtipTitle: 'Full client',
								qtipHtml: 'Selected modules will appear in the full client.',
								checkboxToggle: true,
								checkboxName: 'fullClientEnable',
								xtype:'fieldsetresize',
								defaultType: 'textfield',
								collapsible: true,
								collapsed: true,
								hidden: true, // TODO Delete this once this field is implemented
								items: [
									{
										xtype: 'checkboxgroup',
										columns: 2,
										items: fullClientModules
									}
								]
							}, {
								title: 'Modules for the embedded client(s)',
								qtipTitle: 'Embedded client',
								qtipHtml: 'Selected modules will appear in the embedded client.',
								checkboxToggle: true,
								checkboxName: 'embeddedClientEnable',
								xtype:'fieldsetresize',
								defaultType: 'textfield',
								collapsible: true,
								collapsed: true,
								hidden: true, // TODO Delete this once this field is implemented
								items: [
									{
										xtype: 'checkboxgroup',
										columns: 2,
										items: embeddedClientModules
									}
								]
							}, {
								fieldLabel: 'Projection (<a href="http://spatialreference.org/" target="_blank">?</a>)',
								qtipTitle: 'Projection',
								qtipHtml: 'Projection for this client. Default is <em>EPSG:4326</em>. See the spatial reference Web Site for more info.',
								name: 'projection',
								xtype: 'combobox',
								forceSelection: true, // Force the user to choose something from the list (do not allow random value)
								valueField: 'name',
								displayField: 'title',
								allowBlank: false,
								store: projectionsStore,
								queryMode: 'local'
							}, {
								fieldLabel: 'Save states',
								qtipHtml: 'Save states... - TODO -',
								name: 'saveStates',
								hidden: true, // TODO Delete this once this field is implemented
								xtype: 'displayfield',
								value: 'Grid of all save states, allow user to view, modify, delete, choose de default one'
							}, {
								xtype: 'fieldcontainer',
								fieldLabel: 'Starting Location',
								qtipHtml: 'The starting location of the map. Longitude and latitude indicate the centre of the map and the zoom indicate the resolution;<br/>'+
									'<ul>'+
										'<li>0 for a resolution of 1:221M</li>'+
										'<li>1 for a resolution of 1:111M</li>'+
										'<li>...</li>'+
										'<li>15 for a resolution of 1:6759</li>'+
									'</ul>'+
									'Example: longitude: 148.0, latitude: -18.0, zoom: 6',
								combineErrors: true,
								msgTarget: 'side',
								layout: 'hbox',
								defaults: {
									xtype: 'numberfield',
									// Remove spinner buttons, and arrow key and mouse wheel listeners
									hideTrigger: true,
									keyNavEnabled: false,

									labelWidth: 35,
									margin: '0 0 0 5',
									flex: 1
								},
								items: [
									{
										name: 'longitude',
										fieldLabel: 'Lon',
										decimalPrecision: 5
									}, {
										name: 'latitude',
										fieldLabel: 'Lat',
										decimalPrecision: 5
									}, {
										name: 'zoom',
										fieldLabel: 'Zoom',
										allowDecimals: false
									}
								]
							}, {
								fieldLabel: 'Default Layers',
								qtipHtml: 'List of layer ids, separated by coma or new line. The list define the layers that come up when the client is loaded.',
								name: 'defaultLayers',
								xtype: 'textareafield',
								resizable: {transparent: true}, resizeHandles: 's',
								height: 75
							}, {
								fieldLabel: 'Comment',
								qtipHtml: 'Comment for administrators. This field is not display anywhere else.',
								name: 'comment',
								xtype: 'textareafield',
								resizable: {transparent: true}, resizeHandles: 's',
								height: 50
							}
						]
					}, {
						// Advanced config options
						title: 'Advanced',
						items: [
							Ext.apply({
									fieldLabel: 'Layers\' manual override (<a href="manualOverrideDoc.html" target="_blank">doc</a>)',
									qtipTitle: 'Layers\' manual override',
									qtipHtml: 'JSON configuration used to override the layers information retrived from the GetCapabilities documents. These changes only affect the current client. They are apply over the <i>Layers\' global manual override</i> of the <i>Global configuration</i>.<br/>See the documentation for more information.',
									vtype: 'jsonfield',
									name: 'manualOverride',
									height: 300
								}, browserSpecificEditAreaConfig
							), {
							/*
								fieldLabel: 'Legend parameters',
								qtipHtml: 'List of URL parameters <i>key=value</i>, separated by coma or new line. Those parameters are added to the URL sent to request the legend graphics.',
								name: 'legendParameters',
								xtype: 'textareafield',
								resizable: {transparent: true}, resizeHandles: 's',
								height: 100
							}, {
							*/
								fieldLabel: 'Proxy URL',
								qtipHtml: 'The AtlasMapper clients have to send Ajax request (using javascript) for different features such as <em>feature requests</em>. '+
									'This server application is bundle with such a proxy.<br/>'+
									'<ul>'+
										'<li>If you want to use the integrated proxy, <strong>leave this field blank</strong>.</li>'+
										'<li>If you want to use a standalone proxy, you can use the one provided by OpenLayer in their example folder. In this case, the URL should looks like this:<br/><em>/cgi-bin/proxy.cgi?url=</em></li>'+
									'</ul>',
								name: 'proxyUrl'
							}, {
								fieldLabel: 'Generated file location',
								qtipHtml: 'Absolute file path, on the server\'s, to the folder where the client has to be generated. The application will try to create the folder if it doesn\'t exists.<br/>'+
									'<strong>Warning:</strong> Only set this field if you are setting a client outside the application. If you set this field, you will also have to set the <i>Client base URL</i> with the URL that allow users to access the folder.'+
									notAvailableInDemoMode,
								name: 'generatedFileLocation',
								id: 'generatedFileLocation',
								disabled: demoMode,
								validator: validateDependencies,
								dependencies: ['baseUrl']
							}, {
								fieldLabel: 'Client base URL',
								qtipHtml: 'URL to the client. This field is only needed to create the link to the client, in the Administration Interface. Failing to provide this information will not have any effect in the client itself. Default URL for this field is atlasmapper/client/<Client name>/'+
									'<strong>Warning:</strong> Only set this field if you are setting a client outside the application.'+
									notAvailableInDemoMode,
								name: 'baseUrl',
								id: 'baseUrl',
								disabled: demoMode,
								validator: validateDependencies,
								dependencies: ['generatedFileLocation']
							}, {
								fieldLabel: 'Layer info service URL',
								qtipHtml: 'URL used by the client to get information about layers. The default URL is: atlasmapper/public/layersInfo.jsp'+
									notAvailableInDemoMode,
								name: 'layerInfoServiceUrl',
								disabled: demoMode
							}, {
								fieldLabel: ' ', labelSeparator: '',
								boxLabel: 'Use layer service.',
								qtipHtml: 'Uncheck this box to save the configuration of all layers in the configuration file. This allow the client to run without any AtlasMapper server support. It\'s useful for server that do not have tomcat installed.<br/>'+
									'<b>Note:</b> Disabling the <i>layer service</i> also disable the <i>client preview</i> in the server and the <i>WMS Feature Requests</i> on the client, for layers that are from a different domain name to the client.',
								margin: '0 0 15 0',
								xtype: 'checkboxfield',
								name: 'useLayerService'
							}, {
								fieldLabel: 'Configuration Standard Version',
								qtipHtml: 'Version of the configuration used by the client, for backward compatibilities.<br/>'+
									'<strong>Warning:</strong> Only set this field if this application has to generate a configuration for a old AtlasMapper client.',
								name: 'version',
								anchor: null,
								hidden: true, // TODO Delete this once this field is implemented
								width: 200
							}, {
								fieldLabel: ' ', labelSeparator: '',
								margin: '0 0 15 0',
								boxLabel: 'Show <i>Base layers</i> in a separate tab, in <i>add layers</i> window.',
								qtipHtml: 'Check this box to show all <em>Base layers</em> in a separate tab, in the <em>add layers</em> window of this AtlasMapper client. Uncheck it to let the base layers appear in the tab of their appropriate server.',
								xtype: 'checkboxfield',
								name: 'baseLayersInTab'
							}
						]
					}, {
						// Config options to modify the apparence of the client
						title: 'Appearance',
						items: [
							{
								fieldLabel: 'Theme',
								qtipHtml: 'ExtJS Theme',
								name: 'theme',
								xtype: 'combobox',
								forceSelection: true, // Force the user to choose something from the list (do not allow random value)
								valueField: 'name',
								displayField: 'title',
								allowBlank: false,
								store: {
									fields: ['name', 'title'],
									data : [
										{"name": "", "title": "Blue (ExtJS default)"},
										// {"name": "xtheme-blue", "title": "Blue"}, // Same as default
										{"name": "xtheme-gray", "title": "Grey"},
										{"name": "xtheme-access", "title": "Accessibility"}
									]
								},
								queryMode: 'local'
							}, {
								fieldLabel: 'Background',
								qtipHtml: 'TODO',
								name: 'background',
								xtype: 'displayfield',
								value: 'Coming soon...'
							}, {
								fieldLabel: 'Fonts',
								qtipHtml: 'TODO',
								name: 'fonts',
								xtype: 'displayfield',
								value: 'Coming soon...'
							}
						]
					}
				]
			}]
		});
		this.callParent();
	},

	setActiveRecord: function(record){
		if (record) {
			this.activeRecord = record;
			this.getForm().loadRecord(record);
		} else if (this.defaultValues) {
			this.activeRecord = this.defaultValues;
			this.getForm().loadRecord(this.defaultValues);
		} else {
			this.activeRecord = null;
			this.getForm().reset();
		}
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
			frameset.setSavedMessage('Client saved', 500);
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
			frameset.setSavedMessage('Client created', 500);
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

Ext.define('Writer.ClientConfigGrid', {
	extend: 'Ext.grid.Panel',
	alias: 'widget.writerclientconfiggrid',

	requires: [
		'Ext.form.field.Text',
		'Ext.toolbar.TextItem'
	],

	initComponent: function(){
		var that = this;

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
				}, {
					weight: 1,
					xtype: 'toolbar',
					dock: 'bottom',
					ui: 'footer',
					items: [
						'->',
						{
							//iconCls: 'icon-save',
							text: 'Regenerate all',
							tooltip: 'Regenerate the configuration file for all clients',
							scope: this,
							handler: this.confirmRegenerateAll
						}
					]
				}
			],
			columns: [
				{
					header: 'Main',
					width: 35,
					xtype: 'radiocolumn',
					dataIndex: 'default'
				}, {
					header: 'Client ID',
					width: 100,
					sortable: true,
					dataIndex: 'clientId'
				}, {
					header: 'Client Name',
					width: 100,
					sortable: true,
					dataIndex: 'clientName'
				}, {
					header: 'Enable',
					width: 50,
					sortable: true,
					dataIndex: 'enable',
					align: 'center',
					renderer: function(val) {
						if (val) {
							return '<img src="../resources/icons/accept.png" />';
						}
						return '<img src="../resources/icons/cancel.png" />';
					}
				}, {
					header: 'Preview <i>(using the new config; this option is slower)</i>',
					flex: 1,
					sortable: true,
					dataIndex: 'previewClientUrl',
					renderer: function(val) {
						return val ? '<a href="'+val+'" target="_blank">'+val+'</a>' : '';
					}
				}, {
					header: 'Live client',
					flex: 1,
					sortable: true,
					dataIndex: 'clientUrl',
					renderer: function(val) {
						return val ? '<a href="'+val+'" target="_blank">'+val+'</a>' : '';
					}
				}, {
					// http://docs.sencha.com/ext-js/4-0/#/api/Ext.grid.column.Action
					header: 'Actions',
					xtype:'actioncolumn',
					width:80,
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
								// Show the "Edit" window.
								var rec = grid.getStore().getAt(rowIndex);
								if (rec) {
									var clientConfigFormWindow = this.getClientConfigFormWindow('save');
									clientConfigFormWindow.show();
									clientConfigFormWindow.child('.form').setActiveRecord(rec);
								}
							}
						}, {
							icon: '../resources/icons/generate2.png',

							// Bug: defaults is ignored (http://www.sencha.com/forum/showthread.php?138446-actioncolumn-ignore-defaults)
							iconCls: 'grid-icon',

							tooltip: 'Generate<br/>Push the modifications to the live client',
							handler: function(grid, rowIndex, colIndex) {
								var rec = grid.getStore().getAt(rowIndex);
								var isGenerated = !!rec.get('clientUrl');
								var clientName = 'UNKNOWN';
								if (rec) {
									if (rec.get('clientName')) {
										clientName = rec.get('clientName');
									} else {
										clientName = rec.get('clientId');
									}
								}
								that.confirmRegenerate(rec.get('id'), clientName, isGenerated);
							}
						}, {
							icon: '../resources/icons/cog-error.png',

							// Bug: defaults are ignored (http://www.sencha.com/forum/showthread.php?138446-actioncolumn-ignore-defaults)
							iconCls: 'grid-icon',

							tooltip: 'Debug<br/>For advanced users',
							handler: function(grid, rowIndex, colIndex) {
								var rec = grid.getStore().getAt(rowIndex);

								// Show the window now, with a loading message
								frameset.showBusy();
								var debugWindow = that.getClientConfigDebugWindow();
								debugWindow.show();
								debugWindow.setEditAreasLoading(true);
								// Request the data and update the window when we receive it
								Ext.Ajax.request({
									url: 'clientsConfig.jsp',
									params: {
										'action': 'DEBUG',
										'clientId': rec.get('id'),
										'jsonResponse': true
									},
									success: function(response){
										var responseObj = null;
										var statusCode = response ? response.status : null;
										if (response && response.responseText) {
											try {
												responseObj = Ext.decode(response.responseText);
											} catch (err) {
												responseObj = {errors: [err.toString()]};
											}
										}
										if(responseObj && responseObj.success){
											debugWindow.setEditAreasData(responseObj.data, rec.get('id'));
											debugWindow.setEditAreasLoading(false);
											frameset.clearStatus();
										} else {
											frameset.setErrors('An error occured while loading the client configurations.', responseObj, statusCode);
										}
									},
									failure: function(response) {
										var responseObj = null;
										var statusCode = response ? response.status : null;
										if (response && response.responseText) {
											try {
												responseObj = Ext.decode(response.responseText);
											} catch (err) {
												responseObj = {errors: [err.toString()]};
											}
										}
										frameset.setErrors('An error occured while loading the client configurations.', responseObj, statusCode);
									}
								});
							}
						}
					]
				}
			]
		});
		this.callParent();
		this.getSelectionModel().on('selectionchange', this.onSelectChange, this);
	},

	getClientConfigDebugWindow: function() {
		var browserSpecificEditAreaConfig = {
			xtype: 'editareafield',
			valueType: 'string',
			syntax: 'atlasmapperconfig',
			height: '100%'
		}
		// As usual, IE required specific workaround. This time, we simply disable the feature.
		if (Ext.isIE) {
			browserSpecificEditAreaConfig = {
				xtype: 'textareafield', // IE takes ages to display the EditArea
				height: '450' // IE do not understand 100%
			}
		}

		var debugWindow = Ext.create('Ext.window.Window', {
			title: 'Debug JSON Configuration',
			maximizable: true,
			width: 1000,
			height: 570,
			border: false,
			modal: true,
			layout: 'fit',
			constrainHeader: true,
			closeAction: 'destroy',

			items: [
				{
					xtype: 'tabpanel',
					items: [
						{
							// Layout that display items horizontally
							title: 'Full client configuration',
							xtype: 'fieldcontainer',
							layout: {
								type: 'hbox',
								align: 'stretch'
							},
							defaults: {
								flex: 1,
								hideLabel: true
							},
							items: [
								{
									// Layout used to group the EditArea with its title
									xtype: 'fieldcontainer',
									margin: '2 2 12 5', // Large bottom margin for EditArea checkbox
									layout: 'anchor',
									defaults: {
										anchor: '100%',
										hideLabel: true
									},
									items: [
										{
											xtype: 'displayfield',
											value: 'Current config'
										}, {
											xtype: 'displayfield',
											id: 'displayLinkCurrentFullConf',
											style: { border: 'solid 1px #888888' },
											height: '100%',
											padding: 5,
											hidden: true
										}, Ext.apply(
											{
												id: 'jsonEditAreaCurrentFullConf',
												readOnly: true
											}, browserSpecificEditAreaConfig
										)
									]
								}, {
									// Layout used to group the EditArea with its title
									xtype: 'fieldcontainer',
									margin: '2 5 12 2', // Large bottom margin for EditArea checkbox
									layout: 'anchor',
									defaults: {
										anchor: '100%',
										hideLabel: true
									},
									items: [
										{
											xtype: 'displayfield',
											value: 'New config'
										}, {
											xtype: 'displayfield',
											id: 'displayLinkNewFullConf',
											style: { border: 'solid 1px #888888' },
											height: '100%',
											padding: 5,
											hidden: true
										}, Ext.apply(
											{
												id: 'jsonEditAreaNewFullConf',
												readOnly: true
											}, browserSpecificEditAreaConfig
										)
									]
								}
							]
						}, {
							// Layout that display items horizontally
							title: 'Embedded client configuration',
							xtype: 'fieldcontainer',
							layout: {
								type: 'hbox',
								align: 'stretch'
							},
							defaults: {
								flex: 1,
								hideLabel: true
							},
							items: [
								{
									// Layout used to group the EditArea with its title
									xtype: 'fieldcontainer',
									margin: '2 2 12 5', // Large bottom margin for EditArea checkbox
									layout: 'anchor',
									defaults: {
										anchor: '100%',
										hideLabel: true
									},
									items: [
										{
											xtype: 'displayfield',
											value: 'Current config'
										}, {
											xtype: 'displayfield',
											id: 'displayLinkCurrentEmbeddedConf',
											style: { border: 'solid 1px #888888' },
											height: '100%',
											padding: 5,
											hidden: true
										}, Ext.apply(
											{
												id: 'jsonEditAreaCurrentEmbeddedConf',
												readOnly: true
											}, browserSpecificEditAreaConfig
										)
									]
								}, {
									// Layout used to group the EditArea with its title
									xtype: 'fieldcontainer',
									margin: '2 5 12 2', // Large bottom margin for EditArea checkbox
									layout: 'anchor',
									defaults: {
										anchor: '100%',
										hideLabel: true
									},
									items: [
										{
											xtype: 'displayfield',
											value: 'New config'
										}, {
											xtype: 'displayfield',
											id: 'displayLinkNewEmbeddedConf',
											style: { border: 'solid 1px #888888' },
											height: '100%',
											padding: 5,
											hidden: true
										}, Ext.apply(
											{
												id: 'jsonEditAreaNewEmbeddedConf',
												readOnly: true
											}, browserSpecificEditAreaConfig
										)
									]
								}
							]
						}, {
							title: 'Layers database',
							xtype: 'fieldcontainer',
							layout: {
								type: 'hbox',
								align: 'stretch'
							},
							defaults: {
								flex: 1,
								hideLabel: true
							},
							items: [
								{
									// Layout used to group the EditArea with its title
									xtype: 'fieldcontainer',
									margin: '2 2 12 5', // Large bottom margin for EditArea checkbox
									layout: 'anchor',
									defaults: {
										anchor: '100%',
										hideLabel: true
									},
									items: [
										{
											xtype: 'displayfield',
											value: 'Current config'
										}, {
											xtype: 'displayfield',
											id: 'displayLinkCurrentLayers',
											style: { border: 'solid 1px #888888' },
											height: '100%',
											padding: 5,
											hidden: true
										}, Ext.apply(
											{
												id: 'jsonEditAreaCurrentLayers',
												readOnly: true
											}, browserSpecificEditAreaConfig
										)
									]
								}, {
									// Layout used to group the EditArea with its title
									xtype: 'fieldcontainer',
									margin: '2 5 12 2', // Large bottom margin for EditArea checkbox
									layout: 'anchor',
									defaults: {
										anchor: '100%',
										hideLabel: true
									},
									items: [
										{
											xtype: 'displayfield',
											value: 'New config'
										}, {
											xtype: 'displayfield',
											id: 'displayLinkNewLayers',
											style: { border: 'solid 1px #888888' },
											height: '100%',
											padding: 5,
											hidden: true
										}, Ext.apply(
											{
												id: 'jsonEditAreaNewLayers',
												readOnly: true
											}, browserSpecificEditAreaConfig
										)
									]
								}
							]
						}
					]
				} // Tab panel
			],
			buttons: [
				{
					text: 'Close',
					handler: function() {
						this.ownerCt.ownerCt.close();
					}
				}
			]
		});
		debugWindow.setEditAreasLoading = function(load) {
			function _setLoading(el, load) {
				if (el) el.setLoading(load);
			}
			_setLoading(Ext.getCmp('jsonEditAreaCurrentFullConf'), load);
			_setLoading(Ext.getCmp('jsonEditAreaNewFullConf'), load);
			_setLoading(Ext.getCmp('jsonEditAreaCurrentEmbeddedConf'), load);
			_setLoading(Ext.getCmp('jsonEditAreaNewEmbeddedConf'), load);
			_setLoading(Ext.getCmp('jsonEditAreaCurrentLayers'), load);
			_setLoading(Ext.getCmp('jsonEditAreaNewLayers'), load);
		};
		debugWindow.setEditAreasData = function(jsonData, clientId) {
			if (jsonData && jsonData.fullClient && jsonData.embeddedClient && jsonData.layers) {
				debugWindow.setEditAreaData(
						Ext.getCmp('jsonEditAreaCurrentFullConf'),
						Ext.getCmp('displayLinkCurrentFullConf'),
						'getClientConfigFile.jsp?configType=full&clientId='+clientId,
						jsonData.fullClient.current);
				debugWindow.setEditAreaData(
						Ext.getCmp('jsonEditAreaNewFullConf'),
						Ext.getCmp('displayLinkNewFullConf'),
						'getClientConfigFile.jsp?configType=full&live=true&clientId='+clientId,
						jsonData.fullClient.generated);

				debugWindow.setEditAreaData(
						Ext.getCmp('jsonEditAreaCurrentEmbeddedConf'),
						Ext.getCmp('displayLinkCurrentEmbeddedConf'),
						'getClientConfigFile.jsp?configType=embedded&clientId='+clientId,
						jsonData.embeddedClient.current);
				debugWindow.setEditAreaData(
						Ext.getCmp('jsonEditAreaNewEmbeddedConf'),
						Ext.getCmp('displayLinkNewEmbeddedConf'),
						'getClientConfigFile.jsp?configType=embedded&live=true&clientId='+clientId,
						jsonData.embeddedClient.generated);

				debugWindow.setEditAreaData(
						Ext.getCmp('jsonEditAreaCurrentLayers'),
						Ext.getCmp('displayLinkCurrentLayers'),
						'getClientConfigFile.jsp?configType=layers&clientId='+clientId,
						jsonData.layers.current);
				debugWindow.setEditAreaData(
						Ext.getCmp('jsonEditAreaNewLayers'),
						Ext.getCmp('displayLinkNewLayers'),
						'getClientConfigFile.jsp?configType=layers&live=true&clientId='+clientId,
						jsonData.layers.generated);
			} else {
				frameset.setError('Can not request the configuration files. Try reloading the page.');
			}
		};
		debugWindow.setEditAreaData = function(editArea, displayField, configLink, data) {
			// If data > 200k
			if (roughSizeOfObject(data) > 204800) {
				//editArea.hide(); // Hiding the editArea cause exception later... Destroying it is an easy workaround
				editArea.destroy();
				displayField.show();

				displayField.setValue('The configuration file is too large to be display in this window. Try to download the <a href="'+configLink+'" target="_blank">configuration file</a> instead.');
			} else {
				editArea.setValue(data);
			}
		};

		function roughSizeOfObject(object) {
			var objectList = [];
			var recurse = function(value) {
				var bytes = 0;

				if (typeof(value) === 'boolean') {
					bytes = 4;
				} else if (typeof(value) === 'string') {
					bytes = value.length * 2;
				} else if (typeof(value) === 'number') {
					bytes = 8;
				} else if (typeof(value) === 'object' && objectList.indexOf(value) === -1) {
					objectList[objectList.length] = value;

					for (i in value) {
						if (value.hasOwnProperty(i)) {
							bytes += 8; // pointer overhead
							bytes += i.length;
							bytes += recurse(value[i]);
						}
					}
				}
				return bytes;
			}
			return recurse(object);
		}


		return debugWindow;
	},

	getClientConfigFormWindow: function(action) {
		var actionButton = null;
		if (action == 'add') {
			actionButton = Ext.create('Ext.button.Button', {
				iconCls: 'icon-add',
				text: 'Create',
				handler: function() {
					var window = this.ownerCt.ownerCt;
					var form = window.child('.form');
					if (form.onCreate()) {
						window.close();
					}
				}
			});
		} else {
			actionButton = Ext.create('Ext.button.Button', {
				iconCls: 'icon-save',
				text: 'Apply',
				handler: function() {
					var window = this.ownerCt.ownerCt;
					var form = window.child('.form');
					if (form.onSave()) {
						window.close();
					}
				}
			});
		}

		return Ext.create('Ext.window.Window', {
			title: 'Client configuration',
			width: 700,
			height: 500,
			maxHeight: Ext.getBody().getViewSize().height,
			layout: 'fit',
			constrainHeader: true,
			closeAction: 'destroy',

			items: [{
				xtype: 'writerclientconfigform',
				listeners: {
					scope: this,
					create: function(form, data){
						// Insert the data into the model.
						// If something goes wrong, the store will
						// manage the exception.
						this.store.insert(0, data);
					}
				}
			}],
			buttons: [
				actionButton,
				{
					text: 'Cancel',
					handler: function() {
						var window = this.ownerCt.ownerCt;
						window.close();
					}
				}
			]
		});
	},

	onSelectChange: function(selModel, selections) {
		this.down('#delete').setDisabled(selections.length === 0);
	},



	/**
	 * id: Numerical is of the client (used in the Grid)
	 * clientName: Display name of the client, for user friendly messages
	 */
	confirmRegenerate: function(id, clientName, isGenerated) {
		var that = this;
		Ext.create('Ext.window.Window', {
			layout:'fit',
			width: 400,
			title: 'Regenerate the client <i>'+clientName+'</i>',
			closable: true,
			resizable: false,
			plain: true,
			border: false,
			items: {
				bodyPadding: 5,
				html: 'Regenerate the client <b>'+clientName+'</b>.\n'+
					'<ul class="bullet-list">\n'+
						'<li><b>Minimal:</b> Regenerate the config and index files only. It\'s fast and it\'s usually enough.</li>\n'+
						'<li><b>Complete:</b> Recopy all client files and regenerate the configs. This operation is needed for clients that has never been generated, after an update of the AtlasMapper or when one or more files are corrupted.</li>\n'+
					'</ul>'
			},
			dockedItems: [{
				xtype: 'toolbar',
				dock: 'bottom',
				ui: 'footer',
				defaults: { minWidth: 75 },
				items: [
					'->', // Pseudo item to move the following items to the right (available with ui:footer)
					{
						xtype: 'button',
						text: 'Minimal',
						disabled: !isGenerated,
						padding: '2 10',
						handler: function() {
							that.onRegenerate(id, clientName, false);
							this.ownerCt.ownerCt.close();
						}
					}, {
						xtype: 'button',
						text: 'Complete',
						padding: '2 10',
						handler: function() {
							that.onRegenerate(id, clientName, true);
							this.ownerCt.ownerCt.close();
						}
					}, {
						xtype: 'button',
						text: 'Cancel',
						padding: '2 10',
						handler: function() {
							this.ownerCt.ownerCt.close();
						}
					}
				]
			}]
		}).show();
	},

	/**
	 * id: Numerical is of the client (used in the Grid)
	 * clientName: Display name of the client, for user friendly messages
	 * complete: boolean value. True to recopy every client files, false to copy only the config and the index pages.
	 */
	onRegenerate: function(id, clientName, complete) {
		var that = this;
		frameset.setSavingMessage(
				complete ?
					'Regenerating all '+clientName+' files...' :
					'Regenerating '+clientName+' configuration files...');

		// Request the data and update the window when we receive it
		Ext.Ajax.request({
			url: 'clientsConfig.jsp',
			timeout: timeoutPerClient,
			params: {
				'action': 'GENERATE',
				'complete': !!complete, // Ensure "complete" is boolean
				'id': id,
				'jsonResponse': true
			},
			success: function(response){
				var responseObj = null;
				var statusCode = response ? response.status : null;
				if (response && response.responseText) {
					try {
						responseObj = Ext.decode(response.responseText);
					} catch (err) {
						responseObj = {errors: [err.toString()]};
					}
				}
				if(responseObj && responseObj.success){
					frameset.setSavedMessage('client configuration generated successfully.');
					that.onReload();
				} else {
					frameset.setErrors('An error occured while generating the client configuration.', responseObj, statusCode);
				}
			},
			failure: function(response) {
				var responseObj = null;
				var statusCode = response ? response.status : null;
				if (response && response.responseText) {
					try {
						responseObj = Ext.decode(response.responseText);
					} catch (err) {
						responseObj = {errors: [err.toString()]};
					}
				}
				frameset.setErrors('An error occured while generating the client configuration.', responseObj, statusCode);
			}
		});
	},

	confirmRegenerateAll: function() {
		var that = this;
		Ext.create('Ext.window.Window', {
			layout:'fit',
			width: 400,
			title: 'Regenerate <i>all</i> clients',
			closable: true,
			resizable: false,
			plain: true,
			border: false,
			items: {
				bodyPadding: 5,
				html: 'Regenerate <b>all</b> clients can takes several minutes.\n'+
					'<ul class="bullet-list">\n'+
						'<li><b>Minimal:</b> Regenerate the config and index files only. It\'s fast and it\'s usually enough.<br/><i>Note that the complete generation will be executed for clients that has not been generated.</i></li>\n'+
						'<li><b>Complete:</b> Recopy all clients files and regenerate all configs. This operation is needed for clients that has never been generated, after an update of the AtlasMapper or when one or more files are corrupted.</li>\n'+
					'</ul>'
			},
			dockedItems: [{
				xtype: 'toolbar',
				dock: 'bottom',
				ui: 'footer',
				defaults: { minWidth: 75 },
				items: [
					'->', // Pseudo item to move the following items to the right (available with ui:footer)
					{
						xtype: 'button',
						text: 'Minimal',
						padding: '2 10',
						handler: function() {
							that.onRegenerateAll(false);
							this.ownerCt.ownerCt.close();
						}
					}, {
						xtype: 'button',
						text: 'Complete',
						padding: '2 10',
						handler: function() {
							that.onRegenerateAll(true);
							this.ownerCt.ownerCt.close();
						}
					}, {
						xtype: 'button',
						text: 'Cancel',
						padding: '2 10',
						handler: function() {
							this.ownerCt.ownerCt.close();
						}
					}
				]
			}]
		}).show();
	},

	onRegenerateAll: function(complete) {
		var that = this;
		var nbClients = 1;
		if (this.items && this.items.getCount) {
			nbClients = this.items.getCount();
		}
		frameset.setSavingMessage(
				complete ?
					'Regenerating all files of all clients...' :
					'Regenerating all configuration files...');

		// Request the data and update the window when we receive it
		Ext.Ajax.request({
			url: 'clientsConfig.jsp',
			timeout: timeoutPerClient * nbClients,
			params: {
				'action': 'GENERATEALL',
				'complete': !!complete, // Ensure "complete" is boolean
				'jsonResponse': true
			},
			success: function(response) {
				var responseObj = null;
				var statusCode = response ? response.status : null;
				if (response && response.responseText) {
					try {
						responseObj = Ext.decode(response.responseText);
					} catch (err) {
						responseObj = {errors: [err.toString()]};
					}
				}
				if (responseObj && responseObj.success) {
					frameset.setSavedMessage('Configuration files successfully generated');
					that.onReload();
				} else {
					frameset.setErrors('An error occured while generating the configuration files.', responseObj, statusCode);
				}
			},
			failure: function(response) {
				var responseObj = Ext.decode(response.responseText);
				var statusCode = response ? response.status : null;
				frameset.setErrors('An error occured while generating the configuration files.', responseObj, statusCode);
			}
		});
	},

	onReload: function() {
		this.store.load();
	},

	onAddClick: function() {
		var clientConfigFormWindow = this.getClientConfigFormWindow('add');
		clientConfigFormWindow.child('.form').reset();
		clientConfigFormWindow.show();
	},

	onDeleteClick: function() {
		var selection = this.getView().getSelectionModel().getSelection()[0];
		if (selection) {
			var clientName = 'UNKNOWN';
			if (selection.data) {
				if (selection.data.clientName) {
					clientName = selection.data.clientName;
				} else {
					clientName = selection.data.clientId;
				}
			}
			var confirm = Ext.MessageBox.confirm(
				'Confirm',
				'Deleting the client will also delete the generated files.<br/>'+
				'This operation can not be undone.<br/>'+
				'<br/>'+
				'Are you sure you want to delete the client <b>'+clientName+'</b>?',
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
		frameset.setSavingMessage('Deleting a client...');
		var selection = this.getView().getSelectionModel().getSelection()[0];
		if (selection) {
			var clientName = 'UNKNOWN';
			if (selection.data) {
				if (selection.data.clientName) {
					clientName = selection.data.clientName;
				} else {
					clientName = selection.data.clientId;
				}
			}
			this.store.remove(selection);
			frameset.setSavedMessage('Client <b>'+clientName+'</b> deleted');
		} else {
			frameset.setError('Can not find the client to delete');
		}
	}
});

Ext.define('Writer.ClientConfig', {
	extend: 'Ext.data.Model',
	// Grids must have an unmutable ID

	// Default values:
	// Set default values in the defaultValues instance (lines 70~90).
	// Only set default value "false" for checkboxes:
	// The form do not return the value when they are unchecked,
	// forcing the model to set them to their 'model default value'.
	fields: [
		{name: 'id', type: 'int', useNull: true},
		{name: 'default', type: 'boolean', defaultValue: false},
		{name: 'clientId', sortType: 'asUCString'},
		{name: 'clientName', sortType: 'asUCString'},

		'dataSources', // String or Array<String>
		{name: 'fullClientEnable', type: 'boolean', defaultValue: false},
		'fullClientModules', // String or Array<String>
		{name: 'embeddedClientEnable', type: 'boolean', defaultValue: false},
		'embeddedClientModules', // String or Array<String>

		'manualOverride',
		'legendParameters',
		'clientUrl',
		'previewClientUrl',
		'generatedFileLocation',
		'baseUrl',
		'proxyUrl',
		'layerInfoServiceUrl',
		{name: 'projection', type: 'string'},
		{name: 'longitude', type: 'float'},
		{name: 'latitude', type: 'float'},
		{name: 'zoom', type: 'int'},
		{name: 'baseLayersInTab', type: 'boolean', defaultValue: false},
		'defaultLayers',
		'version',
		{name: 'useLayerService', type: 'boolean', defaultValue: false},
		{name: 'enable', type: 'boolean', defaultValue: false},
		'theme',
		'comment'
	]/*,
	validations: [{
		field: 'clientId',
		type: 'length',
		min: 1
	}]*/
});

Ext.require([
	'Ext.data.*',
	'Ext.tip.QuickTipManager',
	'Ext.window.MessageBox'
]);


Ext.onReady(function(){
	Ext.tip.QuickTipManager.init();

	frameset = new Frameset();
	frameset.setContentTitle('Atlas Mapper clients configuration');
	frameset.addContentDescription('<p><img src="../resources/images/maps-small.jpg" style="float:right; width: 200px; height: 122px"> The <i>AtlasMapper server</i> allows you to set up multiple <i>clients</i>. For most installations only a single client is necessary. In this version each clients can be setup with their own source of layers and initial state (<i>starting position</i>, <i>zoom</i> and <i>layers</i>). In the future each client will be able to be branded differently and have different client <i>modules</i> enabled or disabled.</p><p>Each client is generated into their own folder (based on the <i>client name</i>) in <i>AtlasMapper</i> configuration directory. These clients can be made, if necessary, to run standalone without the <i>AtlasMapper server</i> using a only a web server such as <i>apache</i> or <i>IIS</i>. For this see the <i>Client configuration/Advanced options/Use layer service</i> option.</p>');
	frameset.render(document.body);

	var store = Ext.create('Ext.data.Store', {
		model: 'Writer.ClientConfig',
		autoLoad: true,
		autoSync: true,
		proxy: {
			type: 'ajax',
			api: {
				read: 'clientsConfig.jsp?action=read',
				create: 'clientsConfig.jsp?action=create',
				update: 'clientsConfig.jsp?action=update',
				destroy: 'clientsConfig.jsp?action=destroy'
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
					frameset.setErrors('An error occured while executing the operation ['+operStr+'] on clients.', responseObj, statusCode);
				}
			}
		}
	});

	var clientsConfigGrid = Ext.create('Writer.ClientConfigGrid', {
		itemId: 'grid',
		height: 400,
		resizable: true, resizeHandles: 's',
		store: store
	});

	frameset.addContent(clientsConfigGrid);
});
