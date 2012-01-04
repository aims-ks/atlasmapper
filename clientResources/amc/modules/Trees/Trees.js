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

Atlas.Trees = Ext.extend(Ext.Component, {
	trees: null,
	treePaths: null,
	activeTab: 0,
	mapPanel: null,

	// NOTE: The version must match the version in the server /src/main/java/au/gov/aims/atlasmapperserver/module/Tree.java
	CURRENT_CONFIG_VERSION: 1.0,

	initComponent: function() {
		Atlas.Trees.superclass.initComponent.call(this);

		this.trees = new Array();
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
			orderedTrees.sort(this.sortByName);

			Ext.each(orderedTrees, function(treeObj) {
				var treeName = treeObj.name;
				var tree = treeObj.tree;
				var root = new Ext.ux.tree.LayerNode(
						this.createTreeNode(tree, treeName, [treeName]));

				var treePanel = new Ext.tree.TreePanel({
					root: root,
					title: treeName,
					listeners: {
						activate: function(treePanel) {
							// Call selectionChange when the user switch tab
							if (treePanel && treePanel.getSelectionModel && treePanel.getSelectionModel().getSelectedNode) {
								that.selectionChange(treePanel.getSelectionModel().getSelectedNode());
							}
						}
					},
					// Hide the root and show only the children
					rootVisible: false,
					autoScroll: true
				});

				// Call selectionChange when the user select a new item
				treePanel.getSelectionModel().addListener('selectionchange', function(selectionModel, node) {
					that.selectionChange(node);
				});

				this.trees.push(treePanel);
			}, this);
		}

		// Register the event listeners
		this.mapPanel.ol_on("layerAdded", function(evt) {
			if (evt.layerJSon) {
				that.highlightLayer(evt.layerJSon['layerId']);
			}
		});
		this.mapPanel.ol_on("layerRemoved", function(evt) {
			if (evt.layerJSon) {
				that.unHighlightLayer(evt.layerJSon['layerId']);
			}
		});
	},

	// Ignore case sort
	sortByName: function(a, b) {
		// Move nulls at the end (this should not append)
		if (!a || !a.name) { return -1; }
		if (!b || !b.name) { return 1; }

		var lca = '';
		if (typeof(a.child) == 'string') {
			lca = a.child.toLowerCase();
		} else {
			lca = a.name.toLowerCase();
		}

		var lcb = '';
		if (typeof(b.child) == 'string') {
			lcb = b.child.toLowerCase();
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
		});
		var that = this;
		orderedNode.sort(function(a, b) {
			// Move nulls at the end (this should not append)
			if (!a || !a.name || !a.child) { return -1; }
			if (!b || !b.name || !b.child) { return 1; }

			var aIsLeaf = (typeof(a.child) == 'string');
			var bIsLeaf = (typeof(b.child) == 'string');
			// Move files below folders
			if (aIsLeaf && !bIsLeaf) { return 1; }
			if (!aIsLeaf && bIsLeaf) { return -1; }
			// Sort folders / files in alphabetic order
			return that.sortByName(a, b);
		});

		Ext.each(orderedNode, function(nodeObj) {
			var childName = nodeObj.name;
			var child = nodeObj.child;

			if (typeof(child) == 'string') {
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
		}, this);

		return {
			// Configuration of a tree node (folder)
//			text: node['<config>']['<title>'],
			text: nodeName,
//			qtip: Atlas.core.getNodeQTip(node['<config>']),
			children: children,
//			json: node['<config>'],
			loader: new Ext.ux.tree.LayerTreeLoader({treePaths: this.treePaths, layerStore: this.mapPanel.layers})
		}
	},


	createTreeLeaf: function(title, layerId, children, path) {
		this.cacheLayerTreePath(layerId, path);

		// LayerLeaf
		children.push({
			text: title,
			cls: 'x-tree-noicon',
			layerId: layerId,
			leaf: true,
			checked: false
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

	// Method to override
	selectionChange: function(node) {},

	highlightLayer: function(layerId) {
		if (this.trees == null || typeof(layerId) == 'undefined') {
			return;
		}

		var paths = this.searchLayer(layerId);
		Ext.each(this.trees, function(tree) {
			if (tree.root) {
				// Walk across the tree following the paths and call highlightNode
				this.highlight(tree.root, paths, true);
			}
		}, this);
	},

	unHighlightLayer: function(layerId) {
		if (this.trees == null || typeof(layerId) == 'undefined') {
			return;
		}

		var paths = this.searchLayer(layerId);
		Ext.each(this.trees, function(tree) {
			if (tree.root) {
				// Walk across the tree following the paths and call highlightNode
				this.highlight(tree.root, paths, false);
			}
		}, this);
	},

	/**
	 * Walkthrough a tree to highlight/unhighlight nodes and layers
	 *
	 * TODO Do not unhighlight the branch if there is still highlighted nodes/layers in the branch
	 */
	highlight: function(tree, paths, highlight) {
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
						node.setHighlight(highlight);
						branch = node;
					}
				}
			}
		}, this);
	}
});
