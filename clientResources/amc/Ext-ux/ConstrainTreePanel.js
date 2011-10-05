Ext.namespace("Ext.ux");
Ext.namespace("Ext.ux.tree");

Ext.ux.tree.ConstrainTreePanel = Ext.extend(Ext.tree.TreePanel, {

	// private
	initEvents : function() {
		if((this.enableDD || this.enableDrop) && !this.dropZone){
			/**
			 * The dropZone used by this tree if drop is enabled (see {@link #enableDD} or {@link #enableDrop})
			 * @property dropZone
			 * @type Ext.tree.TreeDropZone
			 */
			this.dropZone = new Ext.ux.tree.ConstrainTreeDropZone(this, this.dropConfig || {
				ddGroup: this.ddGroup || 'TreeDD',
				appendOnly: this.ddAppendOnly === true,
				// constrainDDParent default: true
				// Note: (true !== false) = true,
				//       (false !== false) = false,
				//       (undefined !== false) = true
				constrainDDParent: this.constrainDDParent !== false
			});
		}

		Ext.ux.tree.ConstrainTreePanel.superclass.initEvents.call(this);
	}
});

Ext.ux.tree.ConstrainTreePanel.nodeTypes = {};

Ext.reg('ux_constraintreepanel', Ext.ux.tree.ConstrainTreePanel);
