
Atlas.AddLayersWindow = Ext.extend(Ext.Window, {

	// Default configuration

	title: 'Add layers',
	closable:true, closeAction: 'hide',
	//modal: true,
	width:700, height:500,
	boxMinWidth: 400, boxMinHeight: 200,
	border:false,
	plain:true,
	layout: 'border',
	// Constrain the window in his render element
	// (inside the "document" DOM element).
	// Without this, the window can be dragged so far that
	// it become imposible to access the close button nor
	// move it again.
	constrainHeader: true,
	// There is a small bug with shadow + constrain
	// http://www.sencha.com/forum/showthread.php?118809-shadowOffset-is-undefined-in-Ext.Window
	shadow: false,
	mapPanel: null,

	initComponent: function() {
		Atlas.AddLayersWindow.superclass.initComponent.call(this);

		var treesObj = new Atlas.Trees({mapPanel: this.mapPanel});
		// Override selectionChange method
		treesObj.selectionChange = function(node) {
			if (node && node.layerId) {
				infoObj.setLoadingLayerId(node.layerId);
				Atlas.core.requestLayersJSon([node.layerId], function(layersJSon) {
					if (layersJSon && layersJSon[0]) {
						var layerJSon = layersJSon[0];
						node.layer.json = layerJSon;
						// Ensure the current Ajax response goes with the current active tab
						if (infoObj.getLoadingLayerId() == node.layerId) {
							infoObj.selectionChange(node);
							infoObj.setLoadingLayerId(null);
						}
					}
				});
			} else {
				infoObj.setLoadingLayerId(null);
				infoObj.selectionChange(node);
			}
		};

		var trees = treesObj.trees;
		/*
		// TODO Implement server side search
		trees.push({
			title: 'Search',
			html: 'Comming soon',
			getChecked: function(){}
		});
		*/
		var nav = new Ext.Panel({
			title: 'Navigation',
			region: 'center',
			layout: 'fit',
			items: new Ext.TabPanel({
				border:false,
				enableTabScroll: true,
				activeTab: treesObj.activeTab,
				items: trees
			})
		});

		this.add(nav);

		var infoObj = new Atlas.Info({mapPanel: this.mapPanel});

		var tabPanel = new Ext.TabPanel({
			activeTab: infoObj.startingTab,
			defaults:{autoScroll:true},
			items: infoObj.tabs
		});

		// Right panel
		var tabs = new Ext.Panel({
			title: 'Information',
			region: 'east',
			hideBorders: true,
			collapsible: true,
			collapseMode: 'mini', // More stable
			width: 250,
			split: true,
			layout: 'fit',

			items: tabPanel
		});

		this.add(tabs);

		// Used to call event without loosing the "this" reference.
		var that = this;

		this.addButton({
			text: "Ok",
			handler: function() {
				// Collect all checked layers from all tabs
				var layerIds = [];
				Ext.each(trees, function(tree) {
					var selNodes = tree.getChecked();
					Ext.each(selNodes, function(node) {
						// Uncheck the layers so it will not be
						// checked next time the window is shown.
						node.ui.toggleCheck(false);
						layerIds.push(node.layerId);
					});
				});
				if (layerIds.length > 0) {
					// Fire an event to request the new layers
					that.mapPanel.ol_fireEvent('addLayerIds', {layerIds: layerIds});
				}

				// Hide the window
				that.hide();
			}
		});

		this.addButton({
			text: "Cancel",
			handler: function() {
				that.hide();
			}
		});
	}
});
