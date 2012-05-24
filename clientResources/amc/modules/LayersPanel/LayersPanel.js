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

	showEmbeddedLinkWindow: function() {
		// *** Create the embedded client URL ***
		var location = window.location;

		// Get the current URL
		var urlStr = location.href;

		// Remove the file name (index.html) and the parameters
		urlStr = urlStr.substring(0, urlStr.lastIndexOf("/") + 1);

		// Add embedded file name (embedded.html)
		urlStr += 'embedded.html';

		// Build an array of parameters
		var params = {};
		var map = this.mapPanel == null ? null : this.mapPanel.map;
		if (map != null) {
			// Zoom level (z)
			if (typeof(map.getZoom) === 'function') {
				params['z'] = map.getZoom();
			}

			// Center (ll)
			if (typeof(map.getCenter) === 'function') {
				var center = map.getCenter();
				if (center != null) {
					params['ll'] = center.lon + ',' + center.lat;
				}
			}

			// LAYERS - Note that the embedded map will have only one map, so the only needed parameters here are l0, s0, etc.
			if (typeof(map.layers) !== 'undefined') {
				var l0 = "";
				var s0 = "";
				var o0 = "";
				var v0 = "";
				var first = true;
				for (var i=0; i<map.layers.length; i++) {
					var layer = map.layers[i];
					if (layer != null && typeof(layer.json) !== 'undefined' && layer.json != null) {
						var jsonLayer = layer.json;
						if (this._isLayerNeededInUrl(jsonLayer)) {
							if (!first) {
								l0 += ',';
								s0 += ',';
								o0 += ',';
								v0 += ',';
							}
							// Layers (l0)
							l0 += jsonLayer['layerId']
							// Styles (s0)
							if (typeof(layer.params) !== 'undefined' && layer.params != null &&
									typeof(layer.params['STYLES']) !== 'undefined' && layer.params['STYLES'] != null &&
									layer.params['STYLES'].length > 0) {
								s0 += layer.params['STYLES'];
							}
							// Opacities (o0)
							if (typeof(layer.opacity) !== 'undefined' && layer.opacity != null && layer.opacity !== 1) {
								o0 += layer.opacity
							}
							// Visibilities (v0)
							if (typeof(layer.visibility) !== 'undefined' && layer.visibility === false) {
								v0 += layer.visibility
							}
							first = false;
						}
					}
				}
				// NOTE: ",,*" is twice faster than ",+"
				l0 = l0.replace(/,,*$/, '');
				s0 = s0.replace(/,,*$/, '');
				o0 = o0.replace(/,,*$/, '');
				v0 = v0.replace(/,,*$/, '');
				if (l0.length > 0) {
					params['l0'] = l0;
					if (s0.length > 0) { params['s0'] = s0; }
					if (o0.length > 0) { params['o0'] = o0; }
					if (v0.length > 0) { params['v0'] = v0; }
				}
			}
		}

		// Add params to the url
		var search = "";
		for (param in params) {
			if (params.hasOwnProperty(param)) {
				search += (search.length <= 0 ? '?' : '&') +
					param + '=' + params[param];
			}
		}
		urlStr += search;

		var warningMsg = "";
		if (urlStr.length > 2000) {
			warningMsg = '<span style="color:#CC0000;"><b>WARNING:' +
				'</b> The URL length is <b>' + urlStr.length + '</b> characters, ' +
				'which is too long for some browsers.</span><br/><br/>\n';
		}

		new Ext.Window({
			title: 'Embedded map code',
			layout:'fit',
			width:600,
			height:200,
			constrainHeader: true,
			closeAction: 'destroy',

			items: new Ext.Panel({
				bodyStyle: 'padding: 4px',
				html: 'Copy / Paste the following code into your <b>HTML page</b> to create an <i>Embedded map</i> similar to this one.<br/>\n' +
					'<a href="' + urlStr + '" target="_blank">Try it</a><br/><br/>\n' +
					warningMsg +
					'<div style="font-family: monospace">' +
					'&lt;iframe src="' + urlStr + '" style="width:500px;height:500px" /&gt;' +
					'</div>'
			}),

			buttons: [{
				text: 'Close',
				handler: function(){
					var window = this.ownerCt.ownerCt;
					window.close();
				}
			}]
		}).show();
	},

	_isLayerNeededInUrl: function(jsonLayer) {
		// Layer group are not added to the URL (but not their layers)
		if (jsonLayer['dataSourceType'] == 'FOLDER' || jsonLayer['dataSourceType'] == 'GROUP') {
			return false;
		}

		// Default layers are not added to the URL
		for(var i=0; i<Atlas.conf['defaultLayers'].length; i++){
			if (Atlas.conf['defaultLayers'][i].layerId === jsonLayer['layerId']) {
				return false;
			}
		}

		return true;
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
						if (layer) {
							// Special message for Group Layers
							if (layer._groupLayer) {
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
		if (node.layer && !node.layer._groupLayer) {
			count += 1;
		}
		return count;
	}
});
Ext.reg('layer-panel', Atlas.LayersPanel);
