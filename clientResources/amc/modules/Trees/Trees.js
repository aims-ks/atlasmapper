/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2011 Australian Institute of Marine Science
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

Atlas.Trees = Ext.extend(Ext.Component, {
	trees: null,
	treePaths: null,
	activeTab: 0,
	mapPanel: null,

	// NOTE: The version must match the version in the server /src/main/java/au/gov/aims/atlasmapperserver/module/Tree.java
	CURRENT_CONFIG_VERSION: 2.0,

	initComponent: function() {
		Atlas.Trees.superclass.initComponent.call(this);

		this.trees = [];
		this.treePaths = {};

		// Used to call local method in anonymous functions without loosing the "this" reference.
		var that = this;

		if (Atlas.conf && Atlas.conf['modules'] && Atlas.conf['modules']['Tree'] && Atlas.conf['modules']['Tree']['config']) {
			if (typeof(Atlas.conf['modules']['Tree']['version']) != 'undefined' && Atlas.conf['modules']['Tree']['version'] > this.CURRENT_CONFIG_VERSION) {
				var err = "The version of the configuration of the Tree module ("+Atlas.conf['modules']['Tree']['version']+") is not supported by this client (support up to version: "+this.CURRENT_CONFIG_VERSION+").";
				alert(err);
				throw err;
			}

			var orderedTrees = [];
			Ext.iterate(Atlas.conf['modules']['Tree']['config'], function(treeName, tree) {
				orderedTrees.push({
					name: treeName,
					tree: tree
				});
			});
			// Sort tabs in alphabetic order
			orderedTrees.sort(function(a, b) { return that.sortByName(a, b); });

			Ext.each(orderedTrees, function(treeObj) {
				var treeName = treeObj.name;
				var tree = treeObj.tree;
				var root = new Atlas.Trees.LayerNode(
					this.createTreeNode(tree, treeName, [treeName]));

				var treePanel = new Ext.tree.TreePanel({
					root: root,
					title: treeName,
					listeners: {
						activate: function(treePanel) {
							// Call selectionChange when the user switch tab
							if (treePanel && treePanel.getSelectionModel && treePanel.getSelectionModel().getSelectedNode) {
								this.selectionChange(treePanel.getSelectionModel().getSelectedNode());
							}
						},
						scope: this
					},
					// Hide the root and show only the children
					rootVisible: false,
					autoScroll: true
				});

				// Call selectionChange when the user select a new item
				treePanel.getSelectionModel().addListener('selectionchange', function(selectionModel, node) {
					this.selectionChange(node);
				}, this);

				this.trees.push(treePanel);
			}, this);

			var searchTab = new Atlas.Trees.SearchTab({
				mapPanel: this.mapPanel,
				listeners: {
					'searchResultSelectionChange': function(layerId) {
						this.searchResultSelectionChange(layerId);
					},
					'activate': function() {
						this.searchResultSelectionChange(null);
					},
					scope: this
				}
			});
			if (searchTab) {
				this.trees.push(searchTab);
			}
		}

		// Register the event listeners
		this.mapPanel.ol_on("layerAdded", function(evt) {
			if (evt.layerJSon) {
				this.highlightLayer(evt.layerJSon['layerId']);
			}
		}, this);
		this.mapPanel.ol_on("layerRemoved", function(evt) {
			this.unHighlightLayer();
		}, this);
	},

	// Ignore case sort
	sortByName: function(a, b) {
		// Move nulls at the end (this should not append)
		if (!a || !a.name) { return -1; }
		if (!b || !b.name) { return 1; }

		var lca = '';
		if (this.isLeaf(a.child) && a.child.title) {
			lca = a.child.title.toLowerCase();
		} else {
			lca = a.name.toLowerCase();
		}

		var lcb = '';
		if (this.isLeaf(b.child) && b.child.title) {
			lcb = b.child.title.toLowerCase();
		} else {
			lcb = b.name.toLowerCase();
		}

		if (lca > lcb) { return 1; }
		if (lcb > lca) { return -1; }
		return 0;
	},

	searchLayer: function(layerId) {
		// If in static mode
		// 1. Search the configuration to find all occurrences of the IDs
		// If in Dynamic mode
		// 1. Send a request to the server that provide layers info
		// 2. Return a list of Paths that contains the Text of node that has to be highlighted/unhighlighted
		// Example:
		// Topics, Fish, LTMP Fish - Abon...
		// Topics, Seabed Bio..., Nemipterus, Seabed Bio...
		// Institutions, ...
		return this.treePaths[layerId];
	},

	/**
	 * Create a tree structure containing normalised JSon configuration.
	 * json attribute for Layers:
	 *     {
	 *         <layerId>: ID or Alias ID
	 *         <title>: Title
	 *         All other layer fields found in the configuration file
	 *             for this layer, if any.
	 *     }
	 * json attribute for Nodes:
	 *     {
	 *         <title>: Title
	 *         All other node fields found in the configuration file
	 *             for this node config, if any.
	 *     }
	 */
	createTreeNode: function(node, nodeName, path) {
		var children = [];

		var orderedNode = [];
		Ext.iterate(node, function(childName, child) {
			orderedNode.push({
				name: childName,
				child: child
			});
		}, this);
		var that = this;
		orderedNode.sort(function(a, b) {
			// Move nulls at the end (this should not append)
			if (!a || !a.name || !a.child) { return -1; }
			if (!b || !b.name || !b.child) { return 1; }

			var aIsLeaf = that.isLeaf(a.child);
			var bIsLeaf = that.isLeaf(b.child);
			// Move files below folders
			if (aIsLeaf && !bIsLeaf) { return 1; }
			if (!aIsLeaf && bIsLeaf) { return -1; }
			// Sort folders / files in alphabetic order
			return that.sortByName(a, b);
		});

		Ext.each(orderedNode, function(nodeObj) {
			var childName = nodeObj.name;
			// Ignore the added attribute "__isLeaf__"
			if (childName !== '__isLeaf__') {
				var child = nodeObj.child;

				if (this.isLeaf(child)) {
					this.createTreeLeaf(child, childName, children, path);
				} else {
					// Clone the Path array
					var childPath = path.slice(0);
					childPath.push(childName);

					// Recursivity
					// Call createTreeNode with every children
					// TODO Use a TreeLoader to load branches asynchronously
					children.push(this.createTreeNode(child, childName, childPath));
				}
			}
		}, this);

		return {
			// Configuration of a tree node (folder)
			text: this._safeHtml(nodeName),
			children: children,
			loader: new Atlas.Trees.LayerTreeLoader({treePaths: this.treePaths, layerStore: this.mapPanel.layers})
		}
	},

	/**
	 * Return true if the node is a leaf.
	 * NOTE: This is not trivial; Node contains a collection of objects with arbitrary keys. Leaves are also
	 *     objects, that contains a collection of known keys, but the list of keys may expend in the future.
	 * NOTE 2: Since this operation is expensive, the result is cache in the node object.
	 * @param node
	 * @returns {boolean} Return true the node contains an attribute with a value that is not an object
	 *     (this is more robust than just checking for known attributes) or if the attribute is a known object
	 *     attribute (like 'bbox') and it's value is valid (for 'bbox', we check that it is an array of 4 number).
	 *     NOTE: An empty note is a valid leaf, since none of the attributes are mandatory.
	 */
	isLeaf: function(node) {
		if (node === null || typeof(node) !== 'object') {
			// This should not happen
			return false;
		}
		if (typeof(node.__isLeaf__) === 'undefined') {
			var isLeaf = true;
			Ext.iterate(node, function(key, value) {
				// Only leafs contains attributes that are not objects (like 'title', it's a String)
				if (typeof(value) !== 'object') {
					isLeaf = true;
					return false; // Stop Ext.iterate
				}
				// Special case for attribute that are object;
				// no attribute are mandatory so the leaf could contains only one of them. We have to check them all.
				if (key === 'bbox') {
					if (!this.isBBoxAttribute(value)) {
						isLeaf = false;
						return false; // Stop Ext.iterate
					}
				} else {
					// If it contains an object that is not defined here, it's not a leaf.
					isLeaf = false;
					return false; // Stop Ext.iterate
				}
				// In the future, leaves may contains other attributes. They will need to be tested here...
			}, this);
			// All attributes are valid (note that not attribute is also valid)

			// Cache the result in the object
			node.__isLeaf__ = isLeaf;
		}
		return node.__isLeaf__;
	},

	isBBoxAttribute: function(value) {
		if (typeof(value) !== 'object') {
			return false;
		}
		if (typeof(value.length) === 'undefined') {
			return false;
		}
		if (value.length !== 4) {
			return false;
		}
		var containsDigits = true;
		Ext.each(value, function(digit) {
			if (typeof(digit) !== 'number') {
				containsDigits = false;
				return false; // Stop Ext.each
			}
		});
		return containsDigits;
	},

	createTreeLeaf: function(leafNode, layerId, children, path) {
		this.cacheLayerTreePath(layerId, path);

		// LayerLeaf
		children.push({
			text: this._safeHtml(leafNode.title ? leafNode.title : layerId),
			bbox: leafNode.bbox ? leafNode.bbox : null,
			cls: 'x-tree-noicon',
			leaf: true,
			checked: false,

			// Proprietary additions
			layerId: layerId,
			bbox: leafNode.bbox
		});
	},

	cacheLayerTreePath: function(layerId, path) {
		// Clone the Path array
		var layerPath = path.slice(0);
		layerPath.push(layerId);

		if (!this.treePaths[layerId]) {
			this.treePaths[layerId] = [];
		}
		this.treePaths[layerId].push(layerPath);
	},

	grayoutOutOfBoundLayers: function(mapBBox) {
		Ext.each(this.trees, function(tree) {
			this._grayoutOutOfBoundLayers(tree, mapBBox);
		}, this);
	},
	_grayoutOutOfBoundLayers: function(node, mapBBox) {
		if (node.isLeaf()) {
			// Grayout, if needed
			//console.log(node.attributes.bbox);
		} else {
			// Recursion with children
			if (node.childNodes && node.childNodes.length > 0) {
				for (var i=0, len=node.childNodes.length; i<len; i++) {
					this._grayoutOutOfBoundLayers(node.childNodes[i]);
				}
			}
		}
	},

	// Method to override
	// See: Atlas.AddLayersWindow (modules/LayersPanel/AddLayersWindow.js)
	selectionChange: function(node) {},
	searchResultSelectionChange: function(layerId) {},

	highlightLayer: function(layerId) {
		if (this.trees == null || typeof(layerId) == 'undefined') {
			return;
		}

		var paths = this.searchLayer(layerId);
		Ext.each(this.trees, function(tree) {
			if (tree.root) {
				// Walk across the tree following the paths and call highlightNode
				this.highlight(tree.root, paths);
			}
		}, this);
	},

	unHighlightLayer: function() {
		if (this.trees == null) {
			return;
		}

		Ext.each(this.trees, function(tree) {
			if (tree.root) {
				this.resetHighlight(tree.root);
			}
		}, this);
	},

	/**
	 * Walkthrough a tree to highlight nodes and layers
	 */
	highlight: function(tree, paths) {
		Ext.each(paths, function(path) {
			if (tree.text == path[0]) {
				// Highlight/Unhighlight nodes
				var branch = tree;
				for (var i=1; i<path.length; i++) {
					if (branch == null || (branch.isLoaded && !branch.isLoaded())) {
						// Stop searching into the branch
						break;
					} else {
						var node = null;

						if (i < path.length-1) {
							node = branch.findChild('text', path[i], false);
						} else {
							node = branch.findChild('layerId', path[i], false);
						}

						if (!node) {
							break;
						}
						node.setHighlight(true);
						branch = node;
					}
				}
			}
		}, this);
	},

	resetHighlight: function(tree) {
		this._unHighlightAll(tree);
		this._highlightAll(tree);
	},

	/**
	 * Walkthrough all nodes of the tree and unhighlight everything.
	 * @param node
	 * @private
	 */
	// private
	_unHighlightAll: function(node) {
		node.setHighlight(false);

		if (!node.isLeaf() && node.childNodes && node.childNodes.length > 0) {
			for (var i=0, len=node.childNodes.length; i<len; i++) {
				this._unHighlightAll(node.childNodes[i]);
			}
		}
	},

	/**
	 * Walkthrough all nodes of the tree and highlight nodes / leafs that should be highlighted.
	 * @param node
	 * @private
	 */
	// private
	_highlightAll: function(node) {
		if (!node.isLeaf()) {
			if (node.attributes && node.attributes.loader && typeof(node.attributes.loader.highlightLoadedLayerNodes) === 'function') {
				node.attributes.loader.highlightLoadedLayerNodes(node);
			}

			if (node.childNodes && node.childNodes.length > 0) {
				for (var i=0, len=node.childNodes.length; i<len; i++) {
					this._highlightAll(node.childNodes[i]);
				}
			}
		}
	},

	_safeHtml: function(input) {
		if (input === null || typeof input !== 'string') { return null; }
		return input.replace(/&/gi, "&amp;").replace(/</gi, "&lt;").replace(/>/gi, "&gt;");
	}
});
