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

Ext.namespace("Atlas");

Atlas.MapToolsPanel = Ext.extend(Ext.form.FormPanel, {
	DRUPAL_MAX_URL_LENGTH: 256,
	BROWSER_MAX_URL_LENGTH: 2000,

	mapPanel: null,

	constructor: function(attributes) {
		var that = this;

		// TODO Remove this after implementing tools for all maps (instead of tools per maps)
		var parameters = OpenLayers.Util.getParameters();
		var nbMaps = 1;
		if (parameters.maps) {
			nbMaps = parseInt(parameters.maps);
		}
		if (nbMaps < 1) { nbMaps = 1; }
		if (nbMaps > 4) { nbMaps = 4; }

		var toolsEnabled = (nbMaps == 1);

		var tools = [];
		if (toolsEnabled) {
			if (Atlas.conf['searchEnabled']) {
				tools.push({
					xtype: 'textfield',
					ref: 'searchField',
					name: 'search',
					hideLabel: true,
					margins: {
						top: 6,
						right: 0,
						bottom: 6,
						left: 6
					},
					// Needed to be able to catch 'keydown' event
					enableKeyEvents: true,
					listeners: {
						'specialkey': function(field, evt) {
							if (evt.getKey() == evt.ENTER) {
								that.search(field.getValue());
							}
						},
						// Prevent the Map from grabbing the keys (-/+ to zoom, arrows to pan, etc.)
						'keydown': function(field, evt) {
							evt.stopPropagation();
						}
					}
				});
				tools.push({
					iconCls: 'searchButton',
					tooltip: 'Search a location; city, reef name, etc.',
					margins: {
						top: 2,
						right: 20,
						bottom: 2,
						left: 4
					},
					handler: function(btn, evt) {
						that.search(btn.ownerCt.searchField.getValue());
					}
				});
			}

			if (Atlas.conf['printEnabled']) {
				tools.push({
					iconCls: 'printFrameButton',
					tooltip: 'Prepare map for printing',
					handler: function() {
						that.addPrintFrame();
					}
				});
			}

			if (Atlas.conf['saveMapEnabled']) {
				tools.push({
					iconCls: 'linkButton',
					tooltip: 'Link to the embedded map',
					handler: function() {
						that.showEmbeddedLinkWindow();
					}
				});
			}

			if (Atlas.conf['mapConfigEnabled']) {
				tools.push({
					iconCls: 'configButton',
					tooltip: 'Map options',
					handler: function() {
						that.showMapConfigWindow();
					}
				});
			}

			if (Atlas.conf['mapMeasurementEnabled']) {
				var CTRL = 17; // I can't find this config in OL... It's not the same as OpenLayers.Handler.MOD_CTRL
				var SHIFT = 16;

				// For some reason, rules can not be added to a style inline,
				// so this style can not be defined in the definition of the
				// measurement control.
				var measurementStyle = new OpenLayers.Style();
				measurementStyle.addRules([
					new OpenLayers.Rule({symbolizer: {
						// http://dev.openlayers.org/docs/files/OpenLayers/Symbolizer/Point-js.html
						"Point": {
							pointRadius: 16,
							fillOpacity: 1,
							// Position the ruler under the mouse cursor. The cursor look different
							// depending on the OS or the theme, so the position is a bit cryptic.
							graphicXOffset: -5, graphicYOffset: 15,
							externalGraphic: "resources/images/ruler-cursor.png"
						},
						// http://dev.openlayers.org/docs/files/OpenLayers/Symbolizer/Line-js.html
						"Line": {
							strokeWidth: 3,
							strokeColor: "#000000"
						},
						"Polygon": {
							strokeWidth: 3,
							strokeOpacity: 1,
							strokeColor: "#000000",
							fillColor: "#FFFFFF",
							fillOpacity: 0.3
						}
					}})
				]);
				// Object describing the OpenLayers.Style (it doesn't work with an instance of OpenLayers.Style)
				var lineHighlightStyle = {
					strokeWidth: 7,
					strokeColor: "#FFFFFF"
				};

				var measurementText = new Ext.form.TextField({
					cls: 'measurementText',
					readOnly: true,
					width: 200
				});

				var lineMeasurementControl = null, areaMeasurementControl = null,
					measurementLineButton = null, measurementAreaButton = null;

				if (Atlas.conf['mapMeasurementLineEnabled']) {
					function enableLineMeasurement(button) {
						// The control can only be added to the map when the map is ready.
						// It's safe to say that the map is ready when the user click the button.
						if (!lineMeasurementControl.map && that.mapPanel && that.mapPanel.map) {
							that.mapPanel.map.addControl(lineMeasurementControl);
						}

						if (lineMeasurementControl.map) {
							measurementText.setValue();
							if (button.pressed) {
								if (measurementAreaButton) {
									measurementAreaButton.toggle(false);
								}
								if (areaMeasurementControl) {
									areaMeasurementControl.deactivate();
								}
								lineMeasurementControl.activate();
							} else {
								lineMeasurementControl.deactivate();
							}
						}
					}

					function lineMeasurementHandler(event) {
						var geometry = event.geometry;
						var units = event.units;
						var order = event.order;
						var measure = event.measure;

						if (measurementText) {
							// 1 kilometre = 0.539956803 nautical miles
							var nauticalMiles = null;
							var nmRatio = 0.539956803;
							if (units === 'm') {
								nauticalMiles = measure / 1000 * nmRatio;
							} else if (units === 'km') {
								nauticalMiles = measure * nmRatio;
							}
							measurementText.setValue(measure.toFixed(1) + " " + units +
									(nauticalMiles === null ? '' : ' [' + nauticalMiles.toFixed(1) + ' nautical miles]'));
						}
					}

					// Enable multi-segment ruler when CTRL is pressed
					var ctrlHandler = new OpenLayers.Handler.Keyboard(
						new OpenLayers.Control(),
						{
							// The clean way to do this (using the API) do not work, it loose the
							// config values of the handler that are not set in handlerOptions,
							// like "persist: true"
							//     handlerOptions.maxVertices = null;
							//     lineMeasurementControl.updateHandler(OpenLayers.Handler.Path, handlerOptions);
							keydown: function(evt) {
								// SHIFT key is used for freehand (freehand crash when maxVertices is set)
								if (evt.keyCode === CTRL || evt.keyCode === SHIFT) {
									lineMeasurementControl.handler.maxVertices = null;
								}
							},
							keyup: function(evt) {
								// Restore the vertices mode to default when a key is released
								// NOTE: Some browsers (Chrome) do not trigger the correct key code on key up
								lineMeasurementControl.handler.maxVertices = 2;
							}
						}
					);

					// This control has to be activated and deactivated, so it has
					// to be defined in a variable to keep a reference to it.
					lineMeasurementControl = new OpenLayers.Control.Measure(
						OpenLayers.Handler.ux.HighlightedPath,
						{
							// persist: The line stay on the map after drawn.
							persist: true,
							// immediate: Give the value as the mouse move, using "measurepartial" event.
							immediate: true,
							geodesic: true,
							handlerOptions: {
								maxVertices: 2, // Draw a single line, don't have to double click
								lineHighlightStyle: lineHighlightStyle,
								layerOptions: {
									styleMap: new OpenLayers.StyleMap(measurementStyle)
								}
							},
							eventListeners: {
								"activate": function() {
									this.mapPanel.featureRequestsEnabled = false;
									ctrlHandler.activate();
								},
								"deactivate": function() {
									this.mapPanel.featureRequestsEnabled = true;
									ctrlHandler.deactivate();
								},
								"measure": lineMeasurementHandler,
								"measurepartial": lineMeasurementHandler,
								"scope": this
							}
						}
					);

					measurementLineButton = new Ext.Button({
						iconCls: 'measurementLineButton',
						tooltip: 'Map measurement<br/>For multiple points, hold <b>CTRL</b><br/>For freehand, hold <b>SHIFT</b>',
						ref: 'measurementLineButton',
						enableToggle: true,
						handler: function(button) {
							enableLineMeasurement(button);
						}
					});
					tools.push(measurementLineButton);
				}


				if (Atlas.conf['mapMeasurementAreaEnabled']) {
					function enableAreaMeasurement(button) {
						// The control can only be added to the map when the map is ready.
						// It's safe to say that the map is ready when the user click the button.
						if (!areaMeasurementControl.map && that.mapPanel && that.mapPanel.map) {
							that.mapPanel.map.addControl(areaMeasurementControl);
						}

						if (areaMeasurementControl.map) {
							measurementText.setValue();
							if (button.pressed) {
								if (measurementLineButton) {
									measurementLineButton.toggle(false);
								}
								if (lineMeasurementControl) {
									lineMeasurementControl.deactivate();
								}
								areaMeasurementControl.activate();
							} else {
								areaMeasurementControl.deactivate();
							}
						}
					}

					function areaMeasurementHandler(event) {
						var geometry = event.geometry;
						var units = event.units;
						var order = event.order;
						var measure = event.measure;

						if (measurementText) {
							measurementText.setValue(measure.toFixed(1) + " " + units + "²");
						}
					}

					// This control has to be activated and deactivated, so it has
					// to be defined in a variable to keep a reference to it.
					areaMeasurementControl = new OpenLayers.Control.Measure(
						OpenLayers.Handler.Polygon,
						{
							// persist: The line stay on the map after drawn.
							persist: true,
							// immediate: Give the value as the mouse move, using "measurepartial" event.
							immediate: true,
							geodesic: true,
							handlerOptions: {
								layerOptions: {
									styleMap: new OpenLayers.StyleMap(measurementStyle)
								}
							},
							eventListeners: {
								"activate": function() {
									this.mapPanel.featureRequestsEnabled = false;
								},
								"deactivate": function() {
									this.mapPanel.featureRequestsEnabled = true;
								},
								"measure": areaMeasurementHandler,
								"measurepartial": areaMeasurementHandler,
								"scope": this
							}
						}
					);

					measurementAreaButton = new Ext.Button({
						iconCls: 'measurementAreaButton',
						tooltip: 'Map measurement<br/>For multiple points, hold <b>CTRL</b><br/>For freehand, hold <b>SHIFT</b>',
						ref: 'measurementAreaButton',
						enableToggle: true,
						handler: function(button) {
							enableAreaMeasurement(button);
						}
					});
					tools.push(measurementAreaButton);
				}


				tools.push(measurementText);
			}

			if (tools.length) {
				var newAttributes = Ext.apply({
					xtype: 'form',
					layout: 'hbox',
					defaultType: 'button',
					defaults: {
						scale: 'medium',
						margins: {
							top: 2,
							right: 4,
							bottom: 2,
							left: 4
						}
					},

					// Add a shadow - cause problem with modal window (this panel stay on top...)
					//floating: true, shadowOffset: 6,

					items: tools,
					region: 'north'
				}, attributes);
			}
		}

		Atlas.MapToolsPanel.superclass.constructor.call(this, newAttributes);
	},

	search: function(query) {
		if (query) {
			var searchResultsLayer = new Atlas.Layer.SearchResults(this.mapPanel, {query: query});

			if (searchResultsLayer.layer) {
				this.mapPanel.map.addLayer(searchResultsLayer.layer);
			}
		} else {
			alert('Enter a keyword in the search field.');
		}
	},

	addPrintFrame: function() {
		var printFrameLayer = new Atlas.Layer.PrintFrame(this.mapPanel);
		this.mapPanel.map.addLayer(printFrameLayer.layer);
	},

	showMapConfigWindow: function() {
		var items = [];

		// DPI
		var dpiOptions = [[90], [150], [180], [240]];
		var currentDpiValue = 90;
		if (this.mapPanel) {
			currentDpiValue = this.mapPanel.dpi || this.mapPanel.DEFAULT_DPI;
		}

		if (dpiOptions.length > 1) {
			var dpiSelectConfig = {
				xtype: 'combo',
				fieldLabel: 'DPI',
				value: currentDpiValue,
				typeAhead: false,
				editable: true,
				triggerAction: 'all',
				lazyRender: true,
				mode: 'local',
				store: new Ext.data.ArrayStore({
					fields: ['name'],
					data: dpiOptions
				}),
				valueField: 'name',
				displayField: 'name',
				allowBlank: false,
				listeners: {
					select: this.changeDPI,
					change: this.changeDPI, // Fired when manually edited
					specialkey: function(field, event) {
						if (event.getKey() == event.ENTER) {
							if (field.validate()) {
								// "assertValue" is not in the API doc (private), but I could not find anything
								// better to set the value in the field.
								field.assertValue();
								this.changeDPI(field);
							}
						}
					},
					scope: this
				}
			};

			// IE is awful with width calculation. Better give it a safe value.
			if (Ext.isIE && (!Ext.ieVersion || Ext.ieVersion < 8)) {
				dpiSelectConfig.width = 115;
			}

			items.push(dpiSelectConfig);
		}

		// Gutter
		var gutterOptions = [[0], [20], [50], [100]];
		var currentGutterValue = 0;
		if (this.mapPanel) {
			currentGutterValue = this.mapPanel.gutter || 0;
		}

		if (gutterOptions.length > 1) {
			var gutterSelectConfig = {
				xtype: 'combo',
				fieldLabel: 'Gutter',
				value: currentGutterValue,
				typeAhead: false,
				editable: true,
				triggerAction: 'all',
				lazyRender: true,
				mode: 'local',
				store: new Ext.data.ArrayStore({
					fields: ['name'],
					data: gutterOptions
				}),
				valueField: 'name',
				displayField: 'name',
				allowBlank: false,
				listeners: {
					select: this.changeGutter,
					change: this.changeGutter, // Fired when manually edited
					scope: this
				}
			};

			// IE is awful with width calculation. Better give it a safe value.
			if (Ext.isIE && (!Ext.ieVersion || Ext.ieVersion < 8)) {
				gutterSelectConfig.width = 115;
			}

			// TODO Fix _gutterChange in Layer/WMS.js before enabling this option
			// IMPORTANT: Gutter can not be changed on the fly because
			//     1. They need to be in a "frame". The image frame is only added when a layer is created with gutter > 0.
			//     2. The method "layer.setTileSize" do not works when gutter is specified (so changing the DPI breaks everything)
			//         See: MapPanel.Layer.WMS._setTileSizeForDPI
			//items.push(gutterSelectConfig);
		}

		var configWindow = new Ext.Window({
			title: 'Map options',
			layout:'form',
			modal: true,
			width: 500,
			padding: 5,
			constrainHeader: true,
			closeAction: 'destroy',

			items: items,

			buttons: [{
				text: 'Close',
				handler: function(){
					var window = this.ownerCt.ownerCt;
					window.close();
				}
			}]
		}).show();
	},

	changeDPI: function(field) {
		if (field && this.mapPanel) {
			this.mapPanel.changeDpi(parseInt(field.getValue()));
		}
	},
	changeGutter: function(field) {
		if (field && this.mapPanel) {
			this.mapPanel.changeGutter(parseInt(field.getValue()));
		}
	},

	showEmbeddedLinkWindow: function() {
		var that = this;
		var fullUrlStr = this._getUrlForSaveState(null, false);
		var embeddedUrlStr = this._getUrlForSaveState(null, true);

		var warningMsg = "";
		if (embeddedUrlStr.length > this.BROWSER_MAX_URL_LENGTH) {
			warningMsg = '<span style="color:#CC0000;"><b>WARNING:' +
				'</b> The URL length is <b>' + embeddedUrlStr.length + '</b> characters, ' +
				'which is too long for some browsers.</span><br/>\n';
		} else if (embeddedUrlStr.length > this.DRUPAL_MAX_URL_LENGTH) {
			warningMsg = '<span style="color:#CC0000;"><b>WARNING:' +
				'</b> The URL length is <b>' + embeddedUrlStr.length + '</b> characters, ' +
				'which is too long for some CMS like Drupal.</span><br/>\n';
		}
		if (warningMsg) {
			warningMsg += 'You can fix the problem by removing some layers, including the one ' +
				'that are in the list but are not visible on the map, or set a more basic setting ' +
				'for your layers, such as opacity to 100% and using default styles.<br/><br/>\n';
		}

		var uid = Ext.id();

		var saveStateChangeFct = function(evt) {
			fullUrlStr = that._getUrlForSaveState(evt.urlSaveState, false);
			embeddedUrlStr = that._getUrlForSaveState(evt.urlSaveState, true);
			that._updateValues(uid, fullUrlStr, embeddedUrlStr);
		};

		Atlas.core.mapPanels[0].ol_on('saveStateChange', saveStateChangeFct);

		var windowContent = new Ext.Panel({
			autoScroll: true,
			// For some reason, the input widget need a large (10px) right padding when set with 100% width.
			bodyStyle: 'padding: 4px 10px 4px 4px',

			// IE8 need frameborder attribute for the iframe (it do not understand CSS border)
			html: 'Copy / Paste URL in email\n' +
				'<div><input type="text" onClick="this.select()" id="fullLink'+uid+'" style="width:100%;" value="Loading..."></div>\n' +
				'Copy / Paste <b>HTML</b> to create an <i>Embedded map</i>\n' +
				'<div><input type="text" onClick="this.select()" id="embeddedCode'+uid+'" style="width:100%;" value="Loading..."></div>\n' +
				warningMsg +
				'Size: <input id="w'+uid+'" type="text" value="500" style="width:50px"/>px'+
				' X <input id="h'+uid+'" type="text" value="400" style="width:50px"/>px<br/><br/>\n'+
				'<iframe id="previewEmbeddedMap'+uid+'" src="' + embeddedUrlStr + '&pullState=true" frameborder="0" style="border:none;width:500px;height:400px"></iframe>'
		});

		// Add some event listeners on the size input fields
		windowContent.on('afterrender', function() {
			// IMPORTANT: Only one element retrieved with fly can be used at the time;
			//     the element retrieved with fly can not be used after fly is called again.
			var widthFieldEl = Ext.fly('w' + uid);
			widthFieldEl.on('change', function() {
				that._updateValues(uid, fullUrlStr, embeddedUrlStr);
			});

			var heightFieldEl = Ext.fly('h' + uid);
			heightFieldEl.on('change', function() {
				that._updateValues(uid, fullUrlStr, embeddedUrlStr);
			});
		});

		var linksWindow = new Ext.Window({
			title: 'Map URL and Embedded map',
			layout:'fit',
			modal: true,
			width: 524,
			constrainHeader: true,
			closeAction: 'destroy',

			items: windowContent,

			buttons: [{
				text: 'Close',
				handler: function(){
					var window = this.ownerCt.ownerCt;
					window.close();
				}
			}]
		}).show();

		linksWindow.on('destroy', function() {
			Atlas.core.mapPanels[0].ol_un('saveStateChange', saveStateChangeFct);
		});
	},

	_getUrlForSaveState: function(saveState, embedded) {
		if (typeof(saveState) === 'undefined' || saveState == null) {
			saveState = this.mapPanel == null ? null : this.mapPanel._createUrlSaveState();
		}

		// *** Create the embedded client URL ***
		var location = window.location;

		// Get the current URL
		var urlStr = location.href;

		// Remove the file name (index.html) and the parameters
		urlStr = urlStr.substring(0, urlStr.lastIndexOf("/") + 1);

		// Add embedded file name (embedded.html)
		var search = "";
		if (!embedded) {
			urlStr += 'index.html';
			search = '?intro=false';
		} else {
			urlStr += 'embedded.html';
		}

		// Add params to the url
		for (param in saveState) {
			if (saveState.hasOwnProperty(param)) {
				search += (search.length <= 0 ? '?' : '&') +
					param + '=' + saveState[param];
			}
		}
		urlStr += search;

		return urlStr;
	},

	_updateValues: function(uid, fullUrlStr, embeddedUrlStr) {
		var widthField = document.getElementById('w' + uid);
		var width = widthField.value;

		var heightField = document.getElementById('h' + uid);
		var height = heightField.value;

		var embeddedMap = document.getElementById('previewEmbeddedMap' + uid);
		var fullLink = document.getElementById('fullLink' + uid);
		var embeddedCode = document.getElementById('embeddedCode' + uid);

		embeddedMap.style.width = width+'px';
		embeddedMap.style.height = height+'px';

		fullLink.value = fullUrlStr;
		// IE8 need frameborder (it do not understand CSS border)
		embeddedCode.value = '<iframe src="' + embeddedUrlStr + '" frameborder="0" style="border:none;width:'+width+'px;height:'+height+'px"></iframe>';
	}
});
