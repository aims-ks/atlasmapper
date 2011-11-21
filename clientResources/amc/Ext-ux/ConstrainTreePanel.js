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
 * TreePanel using ConstrainTreeDropZone.
 */

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
