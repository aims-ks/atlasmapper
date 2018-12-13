/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2012 Australian Institute of Marine Science
 *
 *  Contact: Gael Lafond <g.lafond@aims.gov.au>
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

/*
TODO Add comment about Folder reverse order

Save node state:

Each node has its state saved in its layer, so it can be re-created
after it get deleted from the tree (it happen every time the tree is
refreshed).

NOTE: The state can not be save in the node itself since it get
destroyed. The object "Atlas.Layer.LayerState" can be used in the future to store
more information, if needed.
*/

Ext.namespace("GeoExt.ux.tree");

GeoExt.ux.tree.GroupLoader = Ext.extend(Ext.tree.TreeLoader, {
	load: function(node, callback) {
		if(typeof callback == "function"){
			callback();
		}
	}
});

// Calling Destroy (non silent) on a node do not destroy the children, so the event listeners stay alive.
// Solution for this bug:
//     * Call destroy with the silent boolean parameter set to true (which mean having no event triggered about the deletion of the nodes);
//     * Call removeChild with the destroy boolean parameter set to true.
Ext.tree.AsyncTreeNode.prototype.reload = function(callback, scope) {
	this.collapse(false, false);
	while(this.firstChild){
		// this.removeChild(this.firstChild).destroy();
		this.removeChild(this.firstChild, true);
	}
	this.childrenRendered = false;
	this.loaded = false;
	if(this.isHiddenRoot()){
		this.expanded = false;
	}
	this.expand(false, false, callback, scope);
};

GeoExt.ux.tree.GroupLayerLoader = Ext.extend(GeoExt.tree.LayerLoader, {
	filterOverlays: false,
	filterBaseLayers: false,
	path: null,

	constructor: function(config) {
		GeoExt.ux.tree.GroupLayerLoader.superclass.constructor.call(this, config);

		if (this.filterBaseLayers) {
			// Show only Overlay layers
			this.filter = function(record) {
				var layer = record.getLayer();
				return layer.displayInLayerSwitcher === true &&
					layer.isBaseLayer === false;
			};
		}

		if (this.filterOverlays) {
			// Show only Base layers
			this.baseAttrs = Ext.applyIf(this.baseAttrs || {}, {
				iconCls: 'gx-tree-baselayer-icon',
				checkedGroup: 'baselayer'
			});
			this.filter = function(record) {
				var layer = record.getLayer();
				return layer.displayInLayerSwitcher === true &&
					layer.isBaseLayer === true;
			};
		}

		this.store.map.events.on({
			'preaddlayer': this._beforeAddLayer,
			scope: this
		});
	},

	_registerEvents: function(node) {
		node.on("beforemove", this.onBeforeMove, this);
		node.on("move", this.onChildMove, this);
		node.on("checkchange", this.onCheckChange, this);
		node.on("beforechildrenrendered", this.onBeforeChildrenRendered, this);

		if (typeof(node.layer) != 'undefined' && node.layer != null && typeof(node.layer.atlasLayer) != 'undefined') {
			if (node.layer.atlasLayer.isLoading()) {
				this.onLayerLoadStart(node);
			}

			var isGroup = node.layer != null && node.layer.atlasLayer != null && node.layer.atlasLayer.isGroup();

			// Layer group listeners has to be registered at the end,
			// so the load event of child layers are trigger before
			// the group starts checking if its children are loading.
			var priority = !isGroup;

			node.layer.events.register('removed', this, function(event) {
				// event.map, event.layer
				this.onLayerDelete(event.map, node);
			}, priority);

			node.layer.events.register('loadstart', this, function(event) {
				this.onLayerLoadStart(node);
			}, priority);

			node.layer.events.register('loadend', this, function(event) {
				this.onLayerLoadEnd(node);
			}, priority);

			// This event is not triggered anywhere, but maybe someday it will be...
			node.layer.events.register('loadcancel', this, function(event) {
				this.onLayerLoadCancel(node);
			}, priority);

			if (isGroup) {
				node.on("checkchange", this.onFolderCheckChange, this);
			}
		}
	},

	_unregisterEvents: function(node) {
		node.un("beforemove", this.onBeforeMove, this);
		node.un("move", this.onChildMove, this);
		node.un("checkchange", this.onCheckChange, this);
		node.un("beforechildrenrendered", this.onBeforeChildrenRendered, this);

		if (typeof(node.layer) != 'undefined' && node.layer != null) {
			node.layer.events.remove('removed');
			node.layer.events.remove('loadstart');
			node.layer.events.remove('loadend');
			node.layer.events.remove('loadcancel');
			if (node.layer != null && node.layer.atlasLayer != null && node.layer.atlasLayer.isGroup()) {
				node.un("checkchange", this.onFolderCheckChange, this);
			}
		}
	},

	onLayerDelete: function(map, node) {
		if (!node || !node.layer || !node.layer.atlasLayer || typeof(node.text) == 'undefined') {
			return;
		}

		if (node && !this._reordering) {
			this._unregisterEvents(node);
			if (node.hasChildNodes()) {
				node.eachChild(function(child) {
					if (child && child.layer) {
						// This line will trigger this event with the child
						// to remove all children recursively

						try{
							map.removeLayer(child.layer);
						} catch(e) {}
					}
				}, this);
			}

			if (node.layer) {
				node.layer = null;
			}

			try{
				node.remove(true);
			} catch(e) {}
		}
	},

	onLayerLoadStart: function(node) {
		if (!node || !node.layer || !node.layer.atlasLayer) { return; }

		// TODO Set in AbstractLayer
		node.layer.atlasLayer.loaded = false;

		if (node && node.ui && node.layer.atlasLayer.isLoading()) {
			// setTimeout - Fix bug #79
			window.setTimeout(function() {
				node.ui.addClass('layerLoading');
				// Just in case it generate an error last time
				node.ui.removeClass('layerError');
			}, 1);
		}
	},

	onLayerLoadEnd: function(node) {
		if (!node || !node.layer || !node.layer.atlasLayer) { return; }

		// TODO Set in AbstractLayer
		node.layer.atlasLayer.loaded = true;

		if (node && node.ui && !node.layer.atlasLayer.isLoading()) {
			// setTimeout - Fix possible bugs related to bug #79
			window.setTimeout(function() {
				node.ui.removeClass('layerLoading');
				// Just in case it generate an error last time
				node.ui.removeClass('layerError');
			}, 1);
		}
	},

	onLayerLoadCancel: function(node) {
		if (!node || !node.layer || !node.layer.atlasLayer) { return; }

		// TODO Set in AbstractLayer
		node.layer.atlasLayer.loaded = true;

		if (node && node.ui && !node.layer.atlasLayer.isLoading()) {
			this.onLayerLoadEnd(node);
			// setTimeout - Fix possible bugs related to bug #79
			window.setTimeout(function() {
				node.ui.addClass('layerError');
			}, 2);
		}
	},

	/**
	 * Google do not support the loading feature...
	 */
	_supportLoadEvents: function(node) {
		if (node && node.layer && node.layer.atlasLayer) {
			return node.layer.atlasLayer.supportLoadEvents;
		}
		return false;
	},

	onCheckChange: function(node, checked) {
		if (checked) {
			this.onLayerLoadStart(node);
		} else {
			this.onLayerLoadEnd(node);
		}
	},

	onBeforeChildrenRendered: function(parentNode) {
		// Fire just after rendered
		Ext.defer(function() {
			this.onAfterChildrenRendered(parentNode);
		}, 1, this);
	},

	onAfterChildrenRendered: function(parentNode) {
		parentNode.eachChild(function(node) {
			if (node.getUI()) {
				if (node.layer.atlasLayer.isLoading()) {
					this.onLayerLoadStart(node);
				} else {
					this.onLayerLoadEnd(node);
				}
			}
		}, this);
	},

	/**
	 * Add group layers to all folders so they stay in the tree when
	 * they are empty.
	 */
	_beforeAddLayer: function(event) {
		var newLayer = event.layer;
		if (newLayer && newLayer.path && newLayer.path.length > 0 && !newLayer.atlasLayer.isGroup()) {
			var groupLayer = null;
			var parentGroup = null;
			for (var i=0; i<newLayer.path.length; i++) {
				parentGroup = groupLayer;
				var groupPathConf = newLayer.path[i];
				if (typeof(groupPathConf) == 'string') {
					groupPathConf = {
						id: groupPathConf,
						title: groupPathConf
					};
				}

				// Look if there is already a group layer for this folder
				groupLayer = this.store.map.getLayer(groupPathConf.id);
				// No group layer found, add one
				if (groupLayer == null) {
					// Layer group initial state
					var checked = true;

					// TODO Remove this after implementing Save State
					if (typeof(groupPathConf['selected']) !== 'undefined') {
						if (!groupPathConf['selected']) {
							checked = false;
						}
						delete(groupPathConf['selected']);
					}

					var disabled = false;
					if (parentGroup != null && !parentGroup.atlasLayer.layerState.checked) {
						checked = false;
						disabled = true;
					}

					var groupAtlasLayer = this.createAtlasLayerGroup(
							newLayer.atlasLayer.mapPanel,
							groupPathConf,
							newLayer.path.slice(0,i),
							parentGroup == null ? null : parentGroup.atlasLayer);

					if (parentGroup != null && typeof(parentGroup.atlasLayer.addChild) === 'function') {
						parentGroup.atlasLayer.addChild(groupAtlasLayer);
					}

					groupAtlasLayer.layerState = new Atlas.Layer.LayerState({
						disabled: disabled,
						expanded: false,
						checked: checked
					});

					groupLayer = groupAtlasLayer.layer;
					this.store.map.addLayer(groupLayer);
				}
			}

			if (groupLayer != null) {
				newLayer.atlasLayer.parent = groupLayer.atlasLayer;
				if (typeof(groupLayer.atlasLayer.addChild) === 'function') {
					groupLayer.atlasLayer.addChild(newLayer.atlasLayer);
				}

				// Layer initial state
				if (!groupLayer.atlasLayer.layerState.checked) {
					newLayer.setVisibility(false);
					newLayer.atlasLayer.layerState = new Atlas.Layer.LayerState({
						disabled: true,
						checked: false
					});
				}
			}
		}
	},

	// TODO Implement properly
	createAtlasLayerGroup: function(mapPanel, config, path, parent) {
		var layerJSon = config;
		layerJSon['path'] = path;
		layerJSon['layers'] = [];
		layerJSon['olOptions'] = layerJSon['olOptions'] || {};
		layerJSon['olOptions']['path'] = path;

		var atlasLayerGroup = Atlas.Layer.LayerHelper.createLayer(mapPanel, layerJSon, parent);

		return atlasLayerGroup;
	},

	addLayerNode: function(node, layerRecord, index) {
		index = index || 0;

		if (this.filter(layerRecord) === true) {
			var layer = layerRecord.getLayer();
			var atlasLayer = layer.atlasLayer;

			var childLayerNodeConfig = {
				nodeType: 'gx_layer',
				layer: layer,
				layerStore: this.store
			};

			if (atlasLayer) {
				if (atlasLayer.isGroup()) {
					childLayerNodeConfig.cls = 'layerGroup';
					childLayerNodeConfig.loader = new GeoExt.ux.tree.GroupLoader();
				}

				// Restore state
				if (atlasLayer.layerState != null) {
					if (typeof(atlasLayer.layerState.disabled) == 'boolean') {
						childLayerNodeConfig.disabled = atlasLayer.layerState.disabled;
					}
					if (typeof(atlasLayer.layerState.expanded) == 'boolean') {
						childLayerNodeConfig.expanded = atlasLayer.layerState.expanded;
					}
					if (typeof(atlasLayer.layerState.checked) == 'boolean') {
						childLayerNodeConfig.checked = atlasLayer.layerState.checked;
					}
				}
			}

			var childLayerNode = this.createNode(childLayerNodeConfig);
			this._registerEvents(childLayerNode);

			if (layer.path == null) {
				this._insertInOrder(node, childLayerNode, index);
			} else {
				var folder = node;

				for (var i=0; i<layer.path.length && folder != null; i++) {
					var currentPath = layer.path[i];
					if (typeof(currentPath) == 'string') {
						currentPath = {
							id: currentPath,
							title: currentPath
						};
					}

					folder.eachChild(function(child) {
						var childId = child.layer ? child.layer.id : child.text;
						if (childId == currentPath.id) {
							folder = child;
							return false;
						}
					}, this);
				}

				if (folder != null) {
					this._insertInOrder(folder, childLayerNode, index);
				}
			}
		}
	},

	onFolderCheckChange: function(node, checked) {
		if (!node || !node.layer || !node.layer.atlasLayer) { return; }
		if (!node.layer.atlasLayer.layerState) {
			node.layer.atlasLayer.layerState = new Atlas.Layer.LayerState();
		}
		// Keep the state in sync.
		// Note: this operation is only to help other module that would like to get information about the layer's state.
		//     The important operation to save the node state is _saveNodeState in onBeforeMove method.
		node.layer.atlasLayer.layerState.checked = checked;
		node.layer.atlasLayer.layerState.disabled = !checked;

		if (node.hasChildNodes()) {
			if (checked) {
				// Node has been ckecked, re-enable all its children
				node.eachChild(this._enableNode, this);
			} else {
				// Node has been unckecked, disable all its children
				node.eachChild(this._disableNode, this);
			}
		}
	},

	_enableNode: function(node) {
		if (!node || !node.layer || !node.layer.atlasLayer) { return; }
		if (!node.layer.atlasLayer.layerState) {
			node.layer.atlasLayer.layerState = new Atlas.Layer.LayerState();
		}
		// Keep the state in sync.
		// Note: this operation is only to help other module that would like to get information about the layer's state.
		//     The important operation to save the node state is _saveNodeState in onBeforeMove method.
		node.layer.atlasLayer.layerState.disabled = false;

		if (node.disabled) {
			node.enable();

			var checked = true;
			if (node.ui && node.ui.rendered) {
				checked = node.ui.isChecked();
			}

			if (node.hasChildNodes() && checked) {
				node.eachChild(this._enableNode, this);
			} else if (node.layer != null && node.layer.atlasLayer != null && !node.layer.atlasLayer.isGroup()) {
				node.layer.setVisibility(node.layer.atlasLayer.layerState && node.layer.atlasLayer.layerState.visible);
			}
		}
	},
	_disableNode: function(node) {
		if (!node || !node.layer || !node.layer.atlasLayer) { return; }
		if (!node.layer.atlasLayer.layerState) {
			node.layer.atlasLayer.layerState = new Atlas.Layer.LayerState();
		}
		// Keep the state in sync.
		// Note: this operation is only to help other module that would like to get information about the layer's state.
		//     The important operation to save the node state is _saveNodeState in onBeforeMove method.
		node.layer.atlasLayer.layerState.disabled = true;

		if (!node.disabled) {
			node.disable();
			if (node.hasChildNodes()) {
				node.eachChild(this._disableNode, this);
			} else if (node.layer != null && node.layer.atlasLayer != null && !node.layer.atlasLayer.isGroup()) {
				node.layer.atlasLayer.layerState.visible = node.layer.getVisibility();
				node.layer.setVisibility(false);
			}
		}
	},

	/**
	 * Save state of the node, and its children nodes, recursively.
	 * This operation has to be done every time, before the node
	 * get deleted / refreshed. The node can not be accessed to read
	 * those values, during those operations since it do not exists
	 * any more.
	 */
	_saveNodeState: function(node) {
		// All movable nodes have a layer. The only nodes that doesn't
		// are the LayerContainers (Overlays & Base layers containers)
		if (node != null) {
			if (node.layer != null && node.layer.atlasLayer != null) {
				// Save the state in the layer, since the layer do not
				// get deleted when the tree is refreshed.
				if (!node.layer.atlasLayer.layerState) {
					node.layer.atlasLayer.layerState = new Atlas.Layer.LayerState();
				}

				node.layer.atlasLayer.layerState.disabled = node.disabled;
				node.layer.atlasLayer.layerState.expanded = node.isExpanded();

				if (node.ui != null) {
					node.layer.atlasLayer.layerState.checked = node.ui.isChecked();
				}
			}

			if (node.hasChildNodes()) {
				node.eachChild(function(childNode) {
					this._saveNodeState(childNode);
				}, this);
			}
		}
	},

	/**
	 * Insert a node at the appropriate location in the tree.
	 */
	_insertInOrder: function(parentNode, node, index) {
		var sibling = parentNode.item(index);
		if(sibling) {
			parentNode.insertBefore(node, sibling);
		} else {
			parentNode.appendChild(node);
		}
		this._adjustNodePath(node, parentNode);
	},

	/**
	 * Return the previous layer node in the tree, seeing the whole tree
	 * as a flat list of node.
	 * NOTE: A layer node is a node that contains a none null layer attribute.
	 */
	_getPreviousTreeLayerNode: function(node) {
		if (node == null) { return null; }

		var previousNode = this._getPreviousTreeNode(node);
		while (previousNode != null && previousNode.layer == null) {
			previousNode = this._getPreviousTreeNode(previousNode);
		}
		return previousNode;
	},

	/**
	 * Return the previous node in the tree, seeing the whole tree as a
	 * flat list of node, giving folder before it's content.
	 */
	_getPreviousTreeNode: function(node) {
		if (node == null) { return null; }

		// NOTE: Sometimes (AsyncTreeNode), node.hasChildNodes() return true even when it contains no child...
		if (node.childNodes != null && node.childNodes.length > 0) {
			return node.lastChild;
		}

		// If the node has a sibling above, return it.
		if (node.previousSibling != null) {
			return node.previousSibling;
		}

		var parent = node;
		while (parent != null && parent.previousSibling == null) {
			parent = parent.parentNode;
			// Reach the end of the layer container
			if (parent instanceof GeoExt.tree.LayerContainer) {
				parent = null;
			}
		}
		if (parent != null) {
			return parent.previousSibling;
		}

		// Only the root will return null here.
		return null;
	},

	/**
	 * Return the previous node in the tree, seeing the whole tree as a
	 * flat list of node.
	 * NOTE: This method is not used, kept for your future usage
	 */
	_getPreviousTreeNodeNaturalOrder: function(node) {
		if (node == null) { return null; }

		// If the node has a sibling above, return the last of its last children.
		var previousNode = node.previousSibling;
		if (previousNode != null) {
			// NOTE: Sometimes (AsyncTreeNode), node.hasChildNodes() return true even when it contains no child...
			while (previousNode != null && previousNode.childNodes != null && previousNode.childNodes.length > 0) {
				previousNode = previousNode.lastChild;
			}
			return previousNode;
		}

		// There is no sibling above, return the parent (folder).
		if (node != null && node.parentNode != null) {
			return node.parentNode;
		}

		// Only the root will return null here.
		return null;
	},


	/**
	 * Return the next layer node in the tree, seeing the whole tree
	 * as a flat list of node.
	 * NOTE: A layer node is a node that contains a none null layer attribute.
	 */
	_getNextTreeLayerNode: function(node) {
		if (node == null) { return null; }

		var nextNode = this._getNextTreeNode(node);
		while (nextNode != null && nextNode.layer == null) {
			nextNode = this._getNextTreeNode(nextNode);
		}
		return nextNode;
	},

	_getNextTreeNode: function(node) {
		if (node == null) { return null; }

		// If the node has a sibling bellow, return the last of its last children.
		var nextNode = node.nextSibling;
		if (nextNode != null) {
			// NOTE: Sometimes (AsyncTreeNode), node.hasChildNodes() return true even when it contains no child...
			while (nextNode != null && nextNode.childNodes != null && nextNode.childNodes.length > 0) {
				nextNode = nextNode.firstChild;
			}
			return nextNode;
		}

		// There is no sibling bellow, return the parent (folder).
		if (node.parentNode != null && !(node.parentNode instanceof GeoExt.tree.LayerContainer)) {
			return node.parentNode;
		}

		// Return null if there is no more node bellow.
		return null;
	},

	/**
	 * Return the next node in the tree, seeing the whole tree as a
	 * flat list of node.
	 * NOTE: This method is not used, kept for your future usage
	 */
	_getNextTreeNodeNaturalOrder: function(node) {
		if (node == null) { return null; }

		// If the node contains children, return the first one.
		// NOTE: Sometimes (AsyncTreeNode), node.hasChildNodes() return true even when it contains no child...
		if (node.childNodes.length > 0) {
			return node.firstChild;
		}

		// Return the next sibling if any, event if it's a folder.
		if (node.nextSibling != null) {
			return node.nextSibling;
		}

		// There is no more sibling in the current node. Go up the tree.
		var nextNode = node;
		while (nextNode != null && nextNode.nextSibling == null) {
			nextNode = nextNode.parentNode;
		}
		if (nextNode != null) {
			return nextNode.nextSibling;
		}

		// Return null if there is no more node bellow.
		return null;
	},

	/**
	 * Return true is the move is done inside the same LayerContainer.
	 * I.E. The user is not allow to move a Overlay layer to the Base
	 * layers container and vice-versa.
	 * oldParent: The Ext.tree.AsyncTreeNode where the layer was before moving it.
	 * newParent: The Ext.tree.AsyncTreeNode where the layer will be after the move.
	 */
	onBeforeMove: function(tree, node, oldParent, newParent, index) {
		var oldParentRoot = oldParent;
		while (oldParentRoot != null && !(oldParentRoot instanceof GeoExt.tree.LayerContainer)) {
			oldParentRoot = oldParentRoot.parentNode;
		}

		var newParentRoot = newParent;
		var parentDisabled = false;
		while (newParentRoot != null && !(newParentRoot instanceof GeoExt.tree.LayerContainer)) {
			if (newParentRoot.ui && newParentRoot.ui.isChecked() === false) {
				parentDisabled = true;
			}
			newParentRoot = newParentRoot.parentNode;
		}

		return !parentDisabled && oldParentRoot != null
			&& oldParentRoot === newParentRoot
			&& this._saveNodeState(oldParentRoot);
	},

	/**
	 * Event called when an element is moved.
	 * NOTE: The index send by the ExtJS event is relative to the
	 *     parent node. This method ignore this parameter.
	 */
	onChildMove: function(tree, node, oldParent, newParent, index) {
		if (node.hasChildNodes()) {
			this._onFolderMove(tree, node, oldParent, newParent, node, index);
		} else {
			this._onChildMove(tree, node, oldParent, newParent, node, index);
		}

		// Adjust the opacity according to the opacity of the new parent and the opacity set by the layer slider (found by computing the opacity of the layer with the opacity of the old parent)
		if (node.layer) {
			var layerRealOpacity = (node.layer.opacity !== null ? node.layer.opacity : 1);
			if (oldParent.layer && oldParent.layer.opacity != null) {
				if (oldParent.layer.opacity > 0) {
					layerRealOpacity = layerRealOpacity / oldParent.layer.opacity;
				} else {
					layerRealOpacity = 1;
				}
			}
			var valueAfter = (layerRealOpacity > 0 ? ((newParent.layer && newParent.layer.opacity !== null ? newParent.layer.opacity : 1) * layerRealOpacity) : 1);

			// Correction due to real value imprecision.
			if (valueAfter > 1) { valueAfter = 1; }
			if (valueAfter < 0) { valueAfter = 0; }

			node.layer.setOpacity(valueAfter);
		}

		window.setTimeout(function() {
			var newParentRoot = newParent;
			while (newParentRoot != null && !(newParentRoot instanceof GeoExt.tree.LayerContainer)) {
				newParentRoot = newParentRoot.parentNode;
			}
			if (newParentRoot != null) {
				newParentRoot.reload();
			}

			var oldParentRoot = oldParent;
			while (oldParentRoot != null && !(oldParentRoot instanceof GeoExt.tree.LayerContainer)) {
				oldParentRoot = oldParentRoot.parentNode;
			}
			if (oldParentRoot != null && oldParentRoot != newParentRoot) {
				oldParentRoot.reload();
			}
		});
	},

	// This method need to keep a reference to the movedNode;
	// the node that triggered the event.
	// Since it's recursive, the parent nodes do not always represent
	// the node that initially trigger the event.
	_onFolderMove: function(tree, node, oldParent, newParent, movedNode, index) {
		this._onChildMove(tree, node, oldParent, newParent, movedNode, index);
		if (node.hasChildNodes()) {
			for (var i = node.childNodes.length; i >= 0; i--) {
				var childNode = node.childNodes[i];
				if (childNode) {
					if (childNode.hasChildNodes()) {
						this._onFolderMove(tree, childNode, node, node, movedNode, index);
					} else {
						this._onChildMove(tree, childNode, node, node, movedNode, index);
					}
				}
			}
		}
	},

	_onChildMove: function(tree, node, oldParent, newParent, movedNode, index) {
		this._adjustNodePath(node, newParent);

		// Prevent recursive call
		this._reordering = true;
		// remove the record and re-insert it at the correct index
		var record = this.store.getByLayer(node.layer);

		var nextNode = this._getNextTreeLayerNode(node);
		if (nextNode == null) {
			if (movedNode != null && movedNode.hasChildNodes()) {
				// The node that has been moved is a Folder and it has been moved
				// at the bottom so we can not get a reference to the next layer.
				// Calling _getNextTreeLayerNode will give the next element inside
				// that Folder, which is not what we need.
				// What we need is the next "layer node" present AFTER the Folder.
				nextNode = movedNode.nextSibling; // Node after the folder... this may not be a "layer node"
				if (nextNode != null && typeof(nextNode.layer) === 'undefined') {
					// The node found was not a "layer node". Find the next "layer node" from this node.
					nextNode = this._getNextTreeLayerNode(nextNode);
				}
			}
		}

		// Find the current index of the layer, on the map
		var oldLayerIndex = this.store.findBy(function(r) {
			return node.layer === r.getLayer();
		});
		if (oldLayerIndex === null || oldLayerIndex <= 0) {
			oldLayerIndex = 0;
		}

		// Find the current index of the layer next to where
		// we want to move the layer (the layer bellow the new location,
		//     in the tree - which is the layer above in the map).
		var newLayerIndex = nextNode == null ? 0 : this.store.findBy(function(r) {
			return nextNode.layer === r.getLayer();
		});
		if (newLayerIndex === null || newLayerIndex <= 0) {
			newLayerIndex = 0;
		}

		// NOTE: Index is the position of the element inside it's folder (we don't need that),
		//         considering sub-folders as 1 element (top = 0).
		//     oldLayerIndex is the current position of the layer on map (bottom = 0).
		//     newLayerIndex is the position where the layer has to be moved, on map (bottom = 0).
		var moveDown = oldLayerIndex >= newLayerIndex;

		// Index Correction: When moving down, the position occupied by
		//     the layer has to be considered in the new layer position.
		//     This correction is not trivial, so I will try to explain
		//     it using examples.
		//
		//     The index in the layer switcher (Tree index) is index
		//         from 0 (top) to N (bottom), independent index for
		//         each folder.
		//     The index in the map (Layer index) is indexed as a flat
		//         list from 0 (lower layer) to N (top layer).
		//     Example:
		//         +-------------+------------+------------+-------------+
		//         | Folder      | Tree index | Layer name | Layer index |
		//         +-------------+------------+------------+-------------+
		//         | Overlays    | 0          | aaa        | 4           |
		//         | Overlays    | 1          | bbb        | 3           |
		//         | Overlays    | 2          | ccc        | 2           |
		//         | Overlays    | 3          | ddd        | 1           |
		//         | Base layers | 0          | base layer | 5           |
		//         | N/A         | N/A        | base       | 0           | (fake base layer, invisible, to work around an OpenLayer limitation)
		//         | N/A         | N/A        | Marker     | 6           | (mouse marker, never in the tree, only visible on the map with multi-map)
		//         +-------------+------------+------------+-------------+
		//     Operation:
		//         Moving ccc up (between aaa and bbb):
		//             New layer index, if ccc was not removed: 4
		//             New layer index expected: 3
		//             New layer index returned by findBy: 3
		//             No correction needed (when moving up)
		//         Moving bbb down (between ccc and ddd):
		//             New layer index, if bbb was not removed: 2
		//             New layer index expected: 2
		//             New layer index returned by findBy: 1
		//             +1 correction needed (when moving down)
		if (moveDown) {
			newLayerIndex++;
		}

		this._moveRecord(newLayerIndex, record);

		delete this._reordering;
	},

	_moveRecord: function(index, record) {
		// GeoExt execute this operation by removing / re-inserting the layer.
		// That solution do not work:
		//     Removing with the events delete the DOM object and cause exception later.
		//     Removing without the events do not seems to do anything...
		//         this.store.suspendEvents(); this.store.remove(record); this.store.resumeEvents();

		// Inserting a record that is already there simply re-order it.
		// The 'add' event is triggered, which tells the map to re-order the layer on the map.
		// Unfortunately, some cases has to be considered in the legend window to avoid adding
		// a layer multiple times in the legend.
		this.store.insert(index, [record]);
	},

	_adjustNodePath: function(node, newParent) {
		if (node.layer) {
			var newPath = null;
			if (newParent instanceof GeoExt.tree.LayerNode) {
				// Layer / Folder placed on a folder

				// Clone the array
				if (newParent.layer && newParent.layer.path) {
					newPath = newParent.layer.path.slice(0);
				} else {
					newPath = [];
				}
				var pathPart = null;
				if (newParent.layer && newParent.layer.atlasLayer && newParent.layer.atlasLayer.json) {
					pathPart = newParent.layer.atlasLayer.json
				} else {
					pathPart = {
						id: newParent.layer ? newParent.layer.id : newParent.text,
						title: newParent.layer ? newParent.layer.name : newParent.text
					};
				}
				newPath.push(pathPart);
			}
			node.layer.path = newPath;
		}
	}
});
