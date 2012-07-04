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

/**
 * TODO for layer groups
 * [X] Moving layer inside folders
 *     -> Delete the ConstrainTreePanel
 * [X] Delete folder
 *     -> Recursively delete layers + give a layer count in the warning window
 * [X] Layer count also count dead layers... Weird!!
 *     -> Added a node.remove(true); and better handling of layer removal (using ol events)
 * [X] Moving a layer after deleting a sub-layer make everything disapear from the tree
 *     -> Related to bad event handling, solve with previous point
 * [X] Add layer Group + layer Group config (layer override)
 * [X] Add layer Groups to the tree
 *     -> Using wmsPath of layer override - only works with datasources
 * [X] Auto select folder after add
 *     -> Was working after implementation of "Add Group"
 * [X] Description / Options on folder (like Opacity, multi-checkbox for legend, etc.)
 *     -> No multi checkboxes for now
 * [ ] Opacity slider calculate ratio of parents (layer opacity * parent opacity * grand-parent opacity * etc.)
 * [ ] The tree do not take highlight info from the map... issue when layers removed from other mean than the remove button
 * [ ] WMS Queryable for group
 */
Atlas.LayersPanel = Ext.extend(Ext.Panel, {
	DRUPAL_MAX_URL_LENGTH: 256,
	BROWSER_MAX_URL_LENGTH: 2000,

	mapPanel: null,

	// Private
	addLayersWindow: null,

	// Default attribute
	width: 250,
	layout: "border",
	layersPanelHeader: null,
	layersPanelFooter: null,
	// Resizable
	split: true,
	// collapseMode mini is more stable
	collapseMode: 'mini',
	// Animation on collapse is not smooth because the map require a lot of resources to resize.
	animCollapse: false,

	removeButton: null,

	// private
	_layersListPanel: null,

	initComponent: function() {
		// Used to call event without loosing the "this" reference.
		var that = this;

		if (this.layersPanelHeader == null && Atlas.conf['layersPanelHeader'] != null) {
			this.layersPanelHeader = Atlas.conf['layersPanelHeader'];
		}
		if (this.layersPanelFooter == null && Atlas.conf['layersPanelFooter'] != null) {
			this.layersPanelFooter = Atlas.conf['layersPanelFooter'];
		}

		var layersPanel = new Ext.Panel({
			title: 'Layers <div style="float:right" id="layers-ctl_'+this.mapPanel.mapId+'"></div>',
			region: 'center',
			layout: 'border',
			border: false
		});

		if (Ext.isIE6) {
			// IE6 can't display the input widgets if the width is smaller than 300
			this.width = 300;
			// Not Resizable - On IE 6, resize this panel to a smaller size cause some inner items to disappear.
			this.split = false;
		}

		// The collapseMode has to be set in the initialConfig as well...
		if (this.initialConfig == null) {
			this.initialConfig = {};
		}
		if (typeof(this.initialConfig.collapseMode) === 'undefined') {
			this.initialConfig.collapseMode = this.collapseMode;
		}

		Atlas.LayersPanel.superclass.initComponent.call(this);
		var infoObj = new Atlas.Info({mapPanel: this.mapPanel});

		// Inspire on: http://www.geoext.org/tutorials/layertree-tutorial.html
		// See also this for layer list with groups:
		//     https://www.geoext.org/browser/sandbox/mapgears/geoext.ux/ux/LayerTreeBuilder/lib/GeoExt.ux/widgets/tree/LayerTreeBuilder.js?rev=2468
		var layerTree = new Ext.tree.TreeNode({
			text: 'All layers', // The root is hidden
			leaf: false,
			allowDrag: false
		});

		var deleteLayerFct = function(event, layer) {
			Ext.MessageBox.show({
				title: String.format('Removing "{0}"', layer.name),
				msg: String.format('Are you sure you want to remove the layer {0} '+
					'from your list of layers?', '<i><b>' + layer.name + '</b></i>'),
				buttons: Ext.Msg.YESNO,
				fn: function(btn){
					if(btn == 'yes'){
						that.mapPanel.ol_fireEvent('removeLayer', {layer: layer});
					}
				},
				icon: Ext.MessageBox.QUESTION,
				maxWidth: 300
			});
		};

		var onCheckChange = function(node, checked) {
			if (checked) {
				node.select();
			}
		};

		var overlayList = new GeoExt.tree.OverlayLayerContainer({
			text: 'Overlays',
			layerStore: this.mapPanel.layers,
			loader: new GeoExt.ux.tree.GroupLayerLoader({
				// Do not show the base layers
				filterBaseLayers: true,
				store: this.mapPanel.layers
			}),
			deleteLayerFunction: deleteLayerFct,
			leaf: false,
			parentNode: layerTree,
			expandable: true,
			expanded: true,
			allowDrag: false
		});
		// Remove the icons and auto-select layers when needed
		overlayList.loader.createNode = function(attr) {
			attr.cls += ' x-tree-noicon';
			var layerNode = GeoExt.tree.LayerLoader.prototype.createNode.call(this, attr);

			// Select the node when it check box get checked
			layerNode.on("checkchange", onCheckChange, that);
			// Select the node after it is added
			Ext.defer(function() {
				if (layerNode.ui && layerNode.ui.rendered) {
					layerNode.select();
				}
			}, 1);
			return layerNode;
		};

		var baselayerList = new GeoExt.tree.BaseLayerContainer({
			text: 'Base Layers',
			layerStore: this.mapPanel.layers,
			loader: new GeoExt.ux.tree.GroupLayerLoader({
				// Do not show the overlay layers
				filterOverlays: true,
				store: this.mapPanel.layers
			}),
			deleteLayerFunction: deleteLayerFct,
			leaf: false,
			parentNode: layerTree,
			expandable: true,
			expanded: true,
			allowDrag: false,
			allowDrop: false
		});
		// Remove the icons and auto-select layers when needed
		baselayerList.loader.createNode = function(attr) {
			attr.cls += ' x-tree-noicon';
			var layerNode = GeoExt.tree.LayerLoader.prototype.createNode.call(this, attr);

			// Select the node when it check box get checked
			layerNode.on("checkchange", onCheckChange, that);
			// Select the node after it is added
			Ext.defer(function() {
				if (layerNode.ui && layerNode.ui.rendered) {
					layerNode.select();
				}
			}, 1);
			return layerNode;
		};

		// Unsure the radio group name is unique for each map
		if (baselayerList.loader && baselayerList.loader.baseAttrs && baselayerList.loader.baseAttrs.checkedGroup) {
			baselayerList.loader.baseAttrs.checkedGroup += this.mapPanel.id;
		}

		layerTree.appendChild(overlayList);
		layerTree.appendChild(baselayerList);

		// For TreeGrid, see: http://max-bazhenov.com/dev/ux.maximgb.tg/index.php
		// The other TreeGrid (http://dev.sencha.com/deploy/dev/examples/treegrid/treegrid.html)
		// is difficult to used without external config
		this._layersListPanel = new Ext.tree.TreePanel({
			// The title of this element goes with the
			// main one to have the title in the same row
			// as the collapse button.
			region: 'north',
			height: 200,
			border: false,
			split: true,
			rootVisible: false,
			autoScroll: true,

			enableDD: true,

			root: layerTree
		});

		layersPanel.add(this._layersListPanel);

		this._layersListPanel.getSelectionModel().addListener('selectionchange', function(selectionModel, node) {
			// Disable / Enable the remove button
			if (that.removeButton != null) {
				that.removeButton.setDisabled(!node || !node.layer);
			}
			// Change the content of the layer info panel
			infoObj.selectionChange(node);
		});

		var tabPanel = new Ext.TabPanel({
			anchor: '100% 100%',
			activeTab: infoObj.startingTab,
			defaults:{ autoScroll:true },
			items: infoObj.tabs
		});

		// Force the client to redraw the tab after it get displayed,
		// which recalculate the width of inner elements.
		tabPanel.on('tabchange', function (tabPanel, tab) {
			tab.doLayout();
		});

		layersPanel.add({
			region: 'center',
			//title: 'Information',
			border: false, // No border for this element
			hideBorders: true, // No border for his tabs
			autoScroll: false,

			layout: 'anchor',
			items: [tabPanel]
		});

		if (this.layersPanelHeader != null && this.layersPanelHeader !== '') {
			this.add({
				region: 'north',
				border: false,
				html: this.layersPanelHeader
			});
		}
		this.add(layersPanel); // Center region
		if (this.layersPanelFooter != null && this.layersPanelFooter !== '') {
			this.add({
				region: 'south',
				border: false,
				html: this.layersPanelFooter
			});
		}
	},

	showAddLayersWindow: function() {
		if (!this.addLayersWindow) {
			this.addLayersWindow = new Atlas.AddLayersWindow({mapPanel: this.mapPanel});
		}
		var that = this;
		this.addLayersWindow.show(null, function(){
			// Center the window in the map (including the LayerPanel); useful for multimaps.
			this.alignTo(that.ownerCt.getEl(), 'c-c');
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

		fullLink.innerHTML = fullUrlStr;
		embeddedCode.innerHTML = '<iframe src="' + embeddedUrlStr + '" style="border:none;width:'+width+'px;height:'+height+'px"></iframe>';
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
				'which is too long for some DMS like Drupal.</span><br/>\n';
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
			bodyStyle: 'padding: 4px',

			html: 'Copy / Paste URL in email<br/>\n' +
				'<textarea onClick="this.select()" id="fullLink'+uid+'" readonly="true" style="width:500px; height:40px;">' +
				fullUrlStr +
				'</textarea><br/><br/>\n' +
				'Copy / Paste <b>HTML</b> to create an <i>Embedded map</i><br/>\n' +
				'<textarea onClick="this.select()" id="embeddedCode'+uid+'" readonly="true" style="width:500px; height:100px;">' +
				'<iframe src="' + embeddedUrlStr + '" style="border:none;width:500px;height:500px"></iframe>' +
				'</textarea><br/><br/>\n' + 
				warningMsg +
				'Size: <input id="w'+uid+'" type="text" value="500" style="width:50px"/>px'+
				' X <input id="h'+uid+'" type="text" value="500" style="width:50px"/>px<br/><br/>\n'+
				'<iframe id="previewEmbeddedMap'+uid+'" src="' + embeddedUrlStr + '&pullState=true" style="border:none;width:500px;height:500px"></iframe>'
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
			width: 530,
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

	// Override
	afterRender: function() {
		Atlas.LayersPanel.superclass.afterRender.call(this, arguments);
		var that = this;

		// Wait to be sure the 'layers-ctl' element is present in the DOM.
		Ext.defer(function() {
			var el = Ext.get('layers-ctl_'+that.mapPanel.mapId);

			new Ext.Button({
				renderTo: el,
				iconCls: 'link',
				tooltip: 'Link to the embedded map',
				cls: 'layers-btn link-btn',
				handler: function() {that.showEmbeddedLinkWindow();}
			});

			new Ext.Button({
				renderTo: el,
				iconCls: 'add',
				tooltip: 'Add layer',
				cls: 'layers-btn',
				handler: function() {that.showAddLayersWindow();}
			});
			that.removeButton = new Ext.Button({
				renderTo: el,
				iconCls: 'remove',
				tooltip: 'Remove the selected layer',
				cls: 'layers-btn',
				disabled: true,
				handler: function() {
					var node = (that._layersListPanel && that._layersListPanel.getSelectionModel() ? that._layersListPanel.getSelectionModel().getSelectedNode() : null);
					if (node) {
						var layer = node.layer;
						var atlasLayer = layer.atlasLayer;
						if (layer) {
							// Special message for Group Layers
							if (atlasLayer.isGroup()) {
								var nbLayers = that._countRealLayers(node);
								var msg = 'Are you sure you want to remove the ';
								if (nbLayers > 1) {
									msg += 'folder {0}, and its '+nbLayers+' layers, from your list of layers?';
								} else if (nbLayers > 0) {
									msg += 'folder {0}, and its '+nbLayers+' layer, from your list of layers?';
								} else {
									msg += 'empty folder {0} from your list of layers?';
								}

								Ext.MessageBox.show({
									title: String.format('Removing folder "{0}"', layer.name),
									msg: String.format(msg, '<i><b>' + layer.name + '</b></i>'),
									buttons: Ext.Msg.YESNO,
									fn: function(btn){
										if(btn == 'yes'){
//											that._deleteLayerNode(node);
											that.mapPanel.ol_fireEvent('removeLayer', {layer: layer});
										}
									},
									icon: Ext.MessageBox.QUESTION,
									maxWidth: 300
								});
							} else {
								Ext.MessageBox.show({
									title: String.format('Removing layer "{0}"', layer.name),
									msg: String.format('Are you sure you want to remove the layer {0} '+
										'from your list of layers?', '<i><b>' + layer.name + '</b></i>'),
									buttons: Ext.Msg.YESNO,
									fn: function(btn){
										if(btn == 'yes'){
//											that._deleteLayerNode(node);
											that.mapPanel.ol_fireEvent('removeLayer', {layer: layer});
										}
									},
									icon: Ext.MessageBox.QUESTION,
									maxWidth: 300
								});
							}
							// Center the window in the map (including the LayerPanel); useful for multimaps.
							// NOTE: MessageBox in ExtJS 4 extend Window, so it can be align directly using alignTo.
							Ext.MessageBox.getDialog().alignTo(that.ownerCt.getEl(), 'c-c');
						} else {
							// TODO Show an error - No layer selected
						}
					}
				}
			});
			new Ext.Button({
				renderTo: el,
				iconCls: 'hide',
				tooltip: 'Hide the layer panel',
				cls: 'layers-btn',
				handler: function() {
					that.collapse();
				}
			});
		}, 1);
	},

/*
	_deleteLayerNode: function(node) {
		if (node.hasChildNodes()) {
			node.eachChild(function(child) {
				this._deleteLayerNode(child);
			}, this);
		}
		if (node.layer) {
			this.mapPanel.ol_fireEvent('removeLayer', {layer: node.layer});
			node.layer = null;
		}
		node.remove(true);
	},
*/

	_countRealLayers: function(node) {
		var count = 0;
		if (node.hasChildNodes()) {
			node.eachChild(function(child) {
				count += this._countRealLayers(child);
			}, this);
		}
		if (node.layer && !node.layer.atlasLayer.isGroup()) {
			count += 1;
		}
		return count;
	}
});
Ext.reg('layer-panel', Atlas.LayersPanel);
