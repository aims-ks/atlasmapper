Ext.namespace("Ext.ux");
Ext.namespace("Ext.ux.tree");

if (Ext.tree.TreeDropZone) {

Ext.ux.tree.ConstrainTreeDropZone = function(tree, config){
	this.constrainDDParent = config.constrainDDParent || false;

	Ext.ux.tree.ConstrainTreeDropZone.superclass.constructor.call(this, tree, config);
};

Ext.extend(Ext.ux.tree.ConstrainTreeDropZone, Ext.tree.TreeDropZone, {

	/**
	 * Override Ext.tree.TreeDropZone.isValidDropPoint to add
	 * the parent validation
	 */
	isValidDropPoint : function(n, pt, dd, e, data){
		if(!n || !data){ return false; }
		var targetNode = n.node;
		var dropNode = data.node;

		// Ensure the new parent node is the same as the old parent node
		if (this.constrainDDParent === true
				&& targetNode && dropNode
				&& targetNode.parentNode != dropNode.parentNode) {
			return false;
		}

		// return super.isValidDropPoint(n, pt, dd, e, data);
		return Ext.ux.tree.ConstrainTreeDropZone.superclass.isValidDropPoint.call(this, n, pt, dd, e, data);
	}
});

}
