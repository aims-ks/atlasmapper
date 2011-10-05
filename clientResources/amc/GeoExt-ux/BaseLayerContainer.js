/**
 * @requires GeoExt/widgets/tree/LayerContainer.js
 */
Ext.namespace("GeoExt.ux");
Ext.namespace("GeoExt.ux.tree");

/** api: (define)
 *  module = GeoExt.tree
 *  class = BaseLayerContainer
 */

/** api: (extends)
 * GeoExt/widgets/tree/LayerContainer.js
 */

/** api: constructor
 *  .. class:: BaseLayerContainer
 * 
 *     A layer container that will collect all base layers of an OpenLayers
 *     map. Only layers that have displayInLayerSwitcher set to true will be
 *     included. The childrens' iconCls defaults to
 *     "gx-tree-baselayer-icon" and this node' text defaults to
 *     "Base Layer".
 *     
 *     Children will be rendered with a radio button instead of a checkbox,
 *     showing the user that only one base layer can be active at a time.
 * 
 *     To use this node type in ``TreePanel`` config, set nodeType to
 *     "gx_baselayercontainer".
 */
GeoExt.ux.tree.BaseLayerContainer = Ext.extend(GeoExt.tree.BaseLayerContainer, {

	constructor: function(config) {
		GeoExt.ux.tree.BaseLayerContainer.superclass.constructor.call(this,
			config);

		/**
		 * The baseAttrs is a good idea, but it do not allow to create
		 * attributes using layer the attributes.
		 */
		var that = this;
		this.loader.createNode = function(attr) {
			// hide icons
			attr.cls = 'x-tree-noicon';

			if (this.getBaseAttrs) {
				Ext.apply(attr, this.getBaseAttrs(attr));
			}
			if(this.baseAttrs){
				Ext.apply(attr, this.baseAttrs);
			}
			if(typeof attr.uiProvider == 'string'){
				attr.uiProvider = this.uiProviders[attr.uiProvider] || eval(attr.uiProvider);
			}
			if (config.deleteLayerFunction) {
				attr.deleteLayerFunction = config.deleteLayerFunction;
			}

			attr.nodeType = attr.nodeType || "gx_ux_layer";

			var layerNode = new Ext.tree.TreePanel.nodeTypes[attr.nodeType](attr);
			layerNode.on("checkchange", that.onCheckChange, that);
			// Select the node after it's created
			Ext.defer(layerNode.select, 1, layerNode);

			return layerNode;
		}

	},

	onCheckChange: function(node, checked) {
		if (checked) {
			node.select();
		}
	}
});

/**
 * NodeType: gx_ux_baselayercontainer
 */
Ext.tree.TreePanel.nodeTypes.gx_ux_baselayercontainer = GeoExt.ux.tree.BaseLayerContainer;
