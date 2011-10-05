/**
 * Used for the AddLayersWindow
 */

Ext.namespace("Ext.ux");
Ext.namespace("Ext.ux.tree");

Ext.ux.tree.LayerNode = Ext.extend(Ext.tree.AsyncTreeNode, {
	isHighlighted: false,

	// Set node json config - in a layer Object to be the same as the GeoExt nodes
	layer: null,
	layerId: null,

	constructor: function(attributes) {
		Ext.ux.tree.LayerNode.superclass.constructor.call(this, attributes);
		this.layerId = attributes.layerId;
		this.layer = {
			json: attributes
		};
	},

	getLoader: function() {
		var owner;
		// The loader should never be null. If it's the case (bug?), create a LayerTreeLoader without layerStore (needed by the highlight feature).
		return this.loader || ((owner = this.getOwnerTree()) && owner.loader ? owner.loader : (this.loader = new Ext.ux.tree.LayerTreeLoader(null)));
	},

	setHighlight: function(highlight) {
		if (highlight) {
			this.getUI().addClass('highlighted');
			this.isHighlighted = true;
		} else {
			this.getUI().removeClass('highlighted');
			this.isHighlighted = false;
		}
	}
});
Ext.tree.TreePanel.nodeTypes.ux_layernode = Ext.ux.tree.LayerNode;


Ext.ux.tree.LayerLeaf = Ext.extend(Ext.tree.TreeNode, {
	isHighlighted: false,

	// Set node json config - in a layer Object to be the same as the GeoExt nodes
	layer: null,
	layerId: null,

	constructor: function(attributes) {
		Ext.ux.tree.LayerLeaf.superclass.constructor.call(this, attributes);
		this.layerId = attributes.layerId;
		this.layer = {
			json: attributes
		};
	},

	getLoader : function(){
		var owner;
		// The loader should never be null. If it's the case (bug?), create a LayerTreeLoader without layerStore (needed by the highlight feature).
		return this.loader || ((owner = this.getOwnerTree()) && owner.loader ? owner.loader : (this.loader = new Ext.ux.tree.LayerTreeLoader(null)));
	},

	setHighlight : function(highlight){
		if (highlight) {
			this.getUI().addClass('highlighted');
			this.isHighlighted = true;
		} else {
			this.getUI().removeClass('highlighted');
			this.isHighlighted = false;
		}
	}
});
Ext.tree.TreePanel.nodeTypes.ux_layerleaf = Ext.ux.tree.LayerLeaf;
