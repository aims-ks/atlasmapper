
Atlas.LayersPanel = Ext.extend(Ext.Panel, {
	mapPanel: null,

	// Private
	addLayersWindow: null,

	// Default attribute
	title: 'Layers',
	width: 250,
	layout: "border",

	initComponent: function() {
		if (typeof(this.split) === 'undefined') {
			this.split = true; // Resizable
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
		// Remove the icons
		overlayList.loader.createNode = function(attr) {
			attr.cls = 'x-tree-noicon';
			return GeoExt.tree.LayerLoader.prototype.createNode.call(this, attr);
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
		// Remove the icons
		baselayerList.loader.createNode = function(attr) {
			attr.cls = 'x-tree-noicon';
			return GeoExt.tree.LayerLoader.prototype.createNode.call(this, attr);
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
			autoScroll: true,

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
