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
