/**
 * Used for the LayersSwitcher
 */

Ext.namespace("GeoExt.ux");
Ext.namespace("GeoExt.ux.tree");

GeoExt.ux.tree.LayerNodeUI = Ext.extend(GeoExt.tree.LayerNodeUI, {
	deleteNode: null,
	deleteLayerFunction: null,

	/** private: method[constructor]
	 */

	constructor: function(node) {
		// Ext.tree.TreeNodeUI set this.node = node
		// The node contains the a reference to the OpenLayer layer and layerStore.
		if (!this.deleteLayerFunction) {
			this.deleteLayerFunction = node.deleteLayerFunction;
		}

		GeoExt.ux.tree.LayerNodeUI.superclass.constructor.apply(this, arguments);
	},

	// private
	/**
	 * renderElements
	 * n: Node
	 * a: Attributes
	 * targetNode: Where to render the element
	 * bulkRender:
	 */
	renderElements : function(n, a, targetNode, bulkRender){
		// add some indent caching, this helps performance when rendering a large tree
		this.indentMarkup = n.parentNode ? n.parentNode.ui.getChildIndent() : '';

		var cb = Ext.isBoolean(a.checked),
			nel,
			href = this.getHref(a.href),
			buf = ['<li class="x-tree-node"><div ext:tree-node-id="',n.id,'" class="x-tree-node-el x-tree-node-leaf x-unselectable ', a.cls,'" unselectable="on">',
			// cs[0] : this.indentNode
			'<span class="x-tree-node-indent">',this.indentMarkup,"</span>",
			// cs[1] : this.ecNode
			'<img alt="" src="', this.emptyIcon, '" class="x-tree-ec-icon x-tree-elbow" />',
			// cs[2] : this.iconNode
			'<img alt="" src="', a.icon || this.emptyIcon, '" class="x-tree-node-icon',(a.icon ? " x-tree-node-inline-icon" : ""),(a.iconCls ? " "+a.iconCls : ""),'" unselectable="on" />',
			// cs[3] or none : this.checkbox
			cb ? ('<input class="x-tree-node-cb" type="checkbox" ' + (a.checked ? 'checked="checked" />' : '/>')) : '',
			// cs[3] or cs[4] : this.deleteNode
			'<img alt="" src="', this.emptyIcon, '" class="x-tree-node-delete',(a.iconCls ? " "+a.iconCls : ""),'" unselectable="on" />',
			// cs[4] or cs[5] : this.anchor
			'<a hidefocus="on" class="x-tree-node-anchor" href="',href,'" tabIndex="1" ',
			// this.anchor.firstChild : this.textNode
			 a.hrefTarget ? ' target="'+a.hrefTarget+'"' : "", '><span unselectable="on">',n.text,"</span></a></div>",
			'<ul class="x-tree-node-ct" style="display:none;"></ul>',
			"</li>"].join('');

		if(bulkRender !== true && n.nextSibling && (nel = n.nextSibling.ui.getEl())){
			this.wrap = Ext.DomHelper.insertHtml("beforeBegin", nel, buf);
		}else{
			this.wrap = Ext.DomHelper.insertHtml("beforeEnd", targetNode, buf);
		}

		this.elNode = this.wrap.childNodes[0];
		this.ctNode = this.wrap.childNodes[1];
		var cs = this.elNode.childNodes;
		this.indentNode = cs[0];
		this.ecNode = cs[1];
		this.iconNode = cs[2];
		var index = 3;
		if(cb){
			this.checkbox = cs[3];
			// fix for IE6
			this.checkbox.defaultChecked = this.checkbox.checked;
			index++;
		}
		this.deleteNode = cs[index];
		index++;
		this.anchor = cs[index];
		this.textNode = cs[index].firstChild;

		if (this.deleteLayerFunction) {
			this.setDeleteLayerFunction(this.deleteLayerFunction);
		}
	},

	getDeleteEl : function(){
		return this.deleteNode;
	},

	/**
	 * fct(event, layer);
	 * The function is called when the user click on the Delete icon.
	 */
	setDeleteLayerFunction : function(fct) {
		var layer = this.node.layer;
		var fctCall = function(event) {
			fct(event, layer);
		};

		var extDelNode = Ext.get(this.deleteNode);

		// Clear the previous ExtJS listeners
		extDelNode.un('click', this.deleteLayerFunction);

		// Register a new ExtJS listener
		extDelNode.on('click', fctCall);

		this.deleteLayerFunction = fctCall;
	},

	destroy : function(){
		if(this.deleteNode){
			Ext.fly(this.deleteNode).remove();
			delete this.deleteNode;
		}
		GeoExt.ux.tree.LayerNodeUI.superclass.destroy.apply(this);
	}
});

GeoExt.ux.tree.LayerNode = Ext.extend(GeoExt.tree.LayerNode, {

	constructor: function(config) {
		// GeoExt.tree.LayerNode constructor add this.layer and this.layerStore
		this.defaultUI = this.defaultUI || GeoExt.ux.tree.LayerNodeUI;

		if (!this.deleteLayerFunction) {
			this.deleteLayerFunction = config.deleteLayerFunction;
		}

		GeoExt.ux.tree.LayerNode.superclass.constructor.apply(this, arguments);
	}

});

/**
 * NodeType: gx_ux_layer
 */
Ext.tree.TreePanel.nodeTypes.gx_ux_layer = GeoExt.ux.tree.LayerNode;
