/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2012 Australian Institute of Marine Science
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

/*
 TODO Add comment about Folder reverse order

 Save node state:

 Each node has its state saved in its layer, so it can be re-created
 after it get deleted from the tree (it happen every time the tree is
 refreshed).

 NOTE: The state can not be save in the node itself since it get
 destroyed. The object "_state" can be used in the future to store
 more information, if needed.

 node.layer._state: {
 disabled: boolean, true to
 grey out the layer node; it can't be select/unselect
 (usually mean that a parent folder is disabling it)
 expanded: boolean, true to recreate the folder in open mode
 (only apply to folders).
 checked: boolean, store the value of the ckeck box of
 the node before it is moved (layer.getVisibility() is
 always false after unchecking its layer group because the
 layer has been hidden from the map)
 visible: boolean, visibility of the layer, before it get disabled.
 }
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
			this.filter = function(record){
				var layer = record.getLayer();
				return layer.displayInLayerSwitcher === true &&
					layer.isBaseLayer === false;
			}
		}

		if (this.filterOverlays) {
			// Show only Base layers
			this.baseAttrs = Ext.applyIf(this.baseAttrs || {}, {
				iconCls: 'gx-tree-baselayer-icon',
				checkedGroup: 'baselayer'
			}),
				this.filter = function(record) {
					var layer = record.getLayer();
					return layer.displayInLayerSwitcher === true &&
						layer.isBaseLayer === true;
				}
		}

		this.store.map.events.on({
			'preaddlayer': this._beforeAddLayer,
			scope: this
		});
	},

	_registerEvents: function(node) {
		this._unregisterEvents(node);

		node.on("beforemove", this.onBeforeMove, this);
		node.on("move", this.onChildMove, this);
		node.on("checkchange", this.onCheckChange, this);

		if (typeof(node.layer) != 'undefined' && node.layer != null) {
			if (node.layer.visibility) {
				this.onLayerLoadStart(node);
			}
			node.layer.events.on({
				'removed': function(event) {
					// event.map, event.layer
					this.onLayerDelete(event.map, node);
				},
				'loadstart': function(event) {
					this.onLayerLoadStart(node);
				},
				'loadend': function(event) {
					this.onLayerLoadEnd(node);
				},
				// This event is not triggered anywhere, but maybe someday it will be...
				'loadcancel': function(event) {
					this.onLayerLoadCancel(node);
				},
				scope: this
			});

			if (typeof(node.layer._groupLayer) != 'undefined' && node.layer._groupLayer != null) {
				node.on("checkchange", this.onFolderCheckChange, this);
			}
		}
	},

	_unregisterEvents: function(node) {
		node.un("beforemove", this.onBeforeMove, this);
		node.un("move", this.onChildMove, this);
		node.un("checkchange", this.onCheckChange, this);

		if (typeof(node.layer) != 'undefined' && node.layer != null) {
			node.layer.events.remove('removed');
			node.layer.events.remove('loadstart');
			node.layer.events.remove('loadend');
			node.layer.events.remove('loadcancel');
			if (typeof(node.layer._groupLayer) != 'undefined' && node.layer._groupLayer != null) {
				node.un("checkchange", this.onFolderCheckChange, this);
			}
		}
	},

	onLayerDelete: function(map, node) {
		if (node && !this._reordering) {
			if (node.hasChildNodes()) {
				node.eachChild(function(child) {
					if (child && child.layer) {
						// This line will trigger this event with the child
						// to remove all children recursively
						map.removeLayer(child.layer);
					} else {
						// Every nodes has a layer, this should not be called...
						this.onLayerDelete(map, child);
					}
				}, this);
			}

			if (node.layer) {
				node.layer = null;
			}
			node.remove(true);
		}
	},

	onLayerLoadStart: function(node) {
		if (node && node.ui && this._supportLoadEvents(node)) {
			node.ui.addClass('layerLoading');
			// Just in case it generate an error last time
			node.ui.removeClass('layerError');
		}
	},

	onLayerLoadEnd: function(node) {
		if (node && node.ui && this._supportLoadEvents(node)) {
			node.ui.removeClass('layerLoading');
			// Just in case it generate an error last time
			node.ui.removeClass('layerError');
		}
	},

	onLayerLoadCancel: function(node) {
		if (node && node.ui && this._supportLoadEvents(node)) {
			this.onLayerLoadEnd(node);
			node.ui.addClass('layerError');
		}
	},

	/**
	 * Google do not support the loading feature...
	 */
	_supportLoadEvents: function(node) {
		if (node && node.layer && node.layer.json) {
			var type = node.layer.json['dataSourceType'];
			if (type != 'FOLDER' && type != 'GROUP' && type != 'GOOGLE') {
				return true;
			}
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

	/**
	 * Add group layers to all folders so they stay in the tree when
	 * they are empty.
	 */
	_beforeAddLayer: function(event) {
		var newLayer = event.layer;
		if (newLayer && newLayer.path && newLayer.path.length > 0 && !newLayer._groupLayer) {
			var groupLayer = null;
			var parentGroup = null;
			for (var i=0; i<newLayer.path.length; i++) {
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
					if (parentGroup != null && !parentGroup._state.checked) {
						checked = false;
						disabled = true;
					}

					groupLayer = this.createLayerGroup(newLayer.path.slice(0,i), groupPathConf);

					groupLayer._state = {
						disabled: disabled,
						expanded: false,
						checked: checked
					};

					this.store.map.addLayer(groupLayer);
				}
				parentGroup = groupLayer;
			}

			// Layer initial state
			if (groupLayer != null && !groupLayer._state.checked) {
				newLayer.setVisibility(false);
				newLayer._state = {
					disabled: true,
					checked: false
				};
			}
		}
	},

	createLayerGroup: function(path, config) {
		var options = {
			_groupLayer: true,
			path: path
		};

		if (typeof(config['olOptions']) !== 'undefined') {
			options = this._applyOlOverrides(options, config['olOptions']);
		}

		var groupLayer = new OpenLayers.Layer(config['title'], options);

		// Those attributes have to be set after initialisation.
		groupLayer.id = config['id'];
		groupLayer.json = config;

		return groupLayer;
	},
	_applyOlOverrides: function(config, overrides) {
		if (overrides == null) {
			return config;
		}
		for (var key in overrides) {
			if (overrides.hasOwnProperty(key)) {
				config[key] = overrides[key];
			}
		}
		return config;
	},

	addLayerNode: function(node, layerRecord, index) {
		index = index || 0;

		if (this.filter(layerRecord) === true) {
			var layer = layerRecord.getLayer();

			var childLayerNodeConfig = {
				nodeType: 'gx_layer',
				layer: layer,
				layerStore: this.store
			};
			if (layer._groupLayer) {
				childLayerNodeConfig.cls = 'layerGroup';
				childLayerNodeConfig.loader = new GeoExt.ux.tree.GroupLoader();
			}

			// Restore state
			if (layer._state != null) {
				if (typeof(layer._state.disabled) == 'boolean') {
					childLayerNodeConfig.disabled = layer._state.disabled;
				}
				if (typeof(layer._state.expanded) == 'boolean') {
					childLayerNodeConfig.expanded = layer._state.expanded;
				}
				if (typeof(layer._state.checked) == 'boolean') {
					childLayerNodeConfig.checked = layer._state.checked;
				}
			}

			var childLayerNode = this.createNode(childLayerNodeConfig);

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

			var that = this;

			this._registerEvents(childLayerNode);
		}
	},

	onFolderCheckChange: function(node, checked) {
		if (!node.layer._state) {
			node.layer._state = {};
		}
		// Keep the state in sync.
		// Note: this operation is only to help other module that would like to get information about the layer's state.
		//     The important operation to save the node state is _saveNodeState in onBeforeMove method.
		node.layer._state.checked = checked;
		node.layer._state.disabled = !checked;

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
		if (!node.layer._state) {
			node.layer._state = {};
		}
		// Keep the state in sync.
		// Note: this operation is only to help other module that would like to get information about the layer's state.
		//     The important operation to save the node state is _saveNodeState in onBeforeMove method.
		node.layer._state.disabled = false;

		if (node.disabled) {
			node.enable();

			var checked = true;
			if (node.ui && node.ui.rendered) {
				checked = node.ui.isChecked();
			}

			if (node.hasChildNodes() && checked) {
				node.eachChild(this._enableNode, this);
			} else if (node.layer != null && !node.layer._groupLayer) {
				node.layer.setVisibility(node.layer._state && node.layer._state.visible);
			}
		}
	},
	_disableNode: function(node) {
		if (!node.layer._state) {
			node.layer._state = {};
		}
		// Keep the state in sync.
		// Note: this operation is only to help other module that would like to get information about the layer's state.
		//     The important operation to save the node state is _saveNodeState in onBeforeMove method.
		node.layer._state.disabled = true;

		if (!node.disabled) {
			node.disable();
			if (node.hasChildNodes()) {
				node.eachChild(this._disableNode, this);
			} else if (node.layer != null && !node.layer._groupLayer) {
				node.layer._state.visible = node.layer.getVisibility();
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
			if (node.layer != null) {
				// Save the state in the layer, since the layer do not
				// get deleted when the tree is refreshed.
				if (!node.layer._state) {
					node.layer._state = {};
				}

				node.layer._state.disabled = node.disabled;
				node.layer._state.expanded = node.isExpanded();

				if (node.ui != null) {
					node.layer._state.checked = node.ui.isChecked();
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
	_insertInOrder: function(parent, node, index) {
		var sibling = parent.item(index);
		if(sibling) {
			parent.insertBefore(node, sibling);
		} else {
			parent.appendChild(node);
		}
		this._adjustNodePath(node, parent);
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
			this._onFolderMove(tree, node, oldParent, newParent, node);
		} else {
			this._onChildMove(tree, node, oldParent, newParent, node);
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
	_onFolderMove: function(tree, node, oldParent, newParent, movedNode) {
		this._onChildMove(tree, node, oldParent, newParent, movedNode);
		if (node.hasChildNodes()) {
			for (var i = node.childNodes.length; i >= 0; i--) {
				var childNode = node.childNodes[i];
				if (childNode) {
					if (childNode.hasChildNodes()) {
						this._onFolderMove(tree, childNode, node, node, movedNode);
					} else {
						this._onChildMove(tree, childNode, node, node, movedNode);
					}
				}
			}
		}
	},

	_onChildMove: function(tree, node, oldParent, newParent, movedNode) {
		this._adjustNodePath(node, newParent);

		this._reordering = true;
		// remove the record and re-insert it at the correct index
		var record = this.store.getByLayer(node.layer);

		var nextNode = this._getNextTreeLayerNode(node);
		var siblingNode = nextNode;
		if (siblingNode == null) {
			if (movedNode != null && movedNode.hasChildNodes()) {
				// The node that has been moved is a Folder and it has been moved on top so we can not get a reference to the previous layer.
				// Calling _getNextTreeLayerNode will give the next element inside that Folder.
				// What we need is the next "layer node" present AFTER the Folder.
				siblingNode = movedNode.previousSibling; // Next node after the folder... this may not be a "layer node"
				if (siblingNode != null && typeof(siblingNode.layer) == 'undefined') {
					// The node found was not a "layer node". Find the next "layer node" from this node.
					siblingNode = this._getPreviousTreeLayerNode(siblingNode);
				}
			} else {
				siblingNode = this._getPreviousTreeLayerNode(node);
			}
		}

		this.store.remove(record);

		var newRecordIndex = this.store.findBy(function(r) {
			return siblingNode.layer === r.getLayer();
		});

		nextNode != null && newRecordIndex++;

		if(newRecordIndex !== undefined) {
			this.store.insert(newRecordIndex, [record]);
		} else {
			// This line seems to be dead code; the variable
			// oldRecordIndex is not initialised. I'm initialising
			// it just in case it get called.
			if (typeof(oldRecordIndex) == 'undefined') { oldRecordIndex = 0 }
			this.store.insert(oldRecordIndex, [record]);
		}

		delete this._reordering;
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
				if (newParent.layer && newParent.layer.json) {
					pathPart = newParent.layer.json
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
