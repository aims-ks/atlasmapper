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

Atlas.LayersPanel = Ext.extend(Ext.Panel, {
	mapPanel: null,

	// Private
	addLayersWindow: null,

	// Default attribute
	title: 'Layers',
	width: 250,
	layout: "border",

	initComponent: function() {
		if (Ext.isIE6) {
			// IE6 can't display the input widgets if the width is smaller than 300
			this.width = 300;
		}

		// Resizable - On IE 6, resize this panel to a smaller size cause some inner items to disapear.
		if (!Ext.isIE6 && typeof(this.split) === 'undefined') {
			this.split = true;
		}
		if (typeof(this.collapsible) === 'undefined') {
			this.collapsible = true;
		}
		// collapseMode mini is more stable
		if (typeof(this.collapseMode) === 'undefined') {
			this.collapseMode = 'mini';
		}
		if (this.initialConfig && typeof(this.initialConfig.collapseMode) === 'undefined') {
			// The collapseMode has to be set in the initialConfig as well...
			this.initialConfig.collapseMode = 'mini';
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

		// Used to call event without loosing the "this" reference.
		var that = this;
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
			deleteLayerFunction: deleteLayerFct,
			leaf: false,
			parentNode: layerTree,
			expandable: true,
			expanded: true,
			allowDrag: false
		});
		// Remove the icons and auto-select layers when needed
		overlayList.loader.createNode = function(attr) {
			attr.cls = 'x-tree-noicon';
			var layerNode = GeoExt.tree.LayerLoader.prototype.createNode.call(this, attr);

			// Select the node when it check box get checked
			layerNode.on("checkchange", onCheckChange, that);
			// Select the node after it is added
			Ext.defer(layerNode.select, 1, layerNode);
			return layerNode;
		};

		var baselayerList = new GeoExt.tree.BaseLayerContainer({
			text: 'Base Layers',
			layerStore: this.mapPanel.layers,
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
			attr.cls = 'x-tree-noicon';
			var layerNode = GeoExt.tree.LayerLoader.prototype.createNode.call(this, attr);

			// Select the node when it check box get checked
			layerNode.on("checkchange", onCheckChange, that);
			// Select the node after it is added
			Ext.defer(layerNode.select, 1, layerNode);
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
		var layersListPanel = new Ext.ux.tree.ConstrainTreePanel({
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
			// Ensure the node do not get drag outside its parent node.
			constrainDDParent: true,

			root: layerTree,

			buttons: [
				{
					text: "Add layers",
					handler: function() {that.showAddLayersWindow();}
				}, {
					text: 'Remove',
					id: 'remove',
					disabled: true,
					handler: function() {
						var layer = (layersListPanel && layersListPanel.getSelectionModel() && layersListPanel.getSelectionModel().getSelectedNode() ? layersListPanel.getSelectionModel().getSelectedNode().layer : null);
						if (layer) {
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
						} else {
							// TODO Show an error - No layer selected
						}
					}
				}
			]
		});

		this.add(layersListPanel);

		layersListPanel.getSelectionModel().addListener('selectionchange', function(selectionModel, node) {
			// Disable / Enable the remove button
			Ext.getCmp('remove').setDisabled(!node || !node.layer);
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

		this.add({
			region: 'center',
			title: 'Information',
			border: false, // No border for this element
			hideBorders: true, // No border for his tabs
			autoScroll: false,

			layout: 'anchor',
			items: [tabPanel]
		});
	},

	showAddLayersWindow: function() {
		if (!this.addLayersWindow) {
			this.addLayersWindow = new Atlas.AddLayersWindow({mapPanel: this.mapPanel});
		}
		this.addLayersWindow.show();
	}
});
Ext.reg('layer-panel', Atlas.LayersPanel);
