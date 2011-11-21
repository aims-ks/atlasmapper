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
 * TreeLoader that initialise LayerNode and handle highlighting of nodes and
 * leaves describe in the treePaths config option.
 */
Ext.namespace("Ext.ux");
Ext.namespace("Ext.ux.tree");

Ext.ux.tree.LayerTreeLoader = Ext.extend(Ext.tree.TreeLoader, {
	layerStore: null,
	treePaths: null,

	load: function(node, callback, scope){
		// super.load(node, callback, scope);
		Ext.ux.tree.LayerTreeLoader.superclass.load.call(this, node, callback, scope);

		// Construct the path of the current node, from the root
		var nodeTreePath = [node];
		var tmpNode = node;
		while (tmpNode.parentNode) {
			tmpNode = tmpNode.parentNode;
			nodeTreePath.push(tmpNode);
		}
		// Reverse the order of the node's path.
		nodeTreePath.reverse();

		// The node has children that may or may not need to be highlighted.
		// To find out, we have th check if the child node is on the list
		// of selected layers. The layer itself can not be used alone,
		// highlighting it parents backward, because the layers may not
		// be loaded at the time (the tree is Async).
		// The solution is to check if any of the node's children match
		// any of the tree paths of any loaded layers.
		if (this.treePaths) {
			var loadedLayersJSon = this.getLoadedLayersJSon();
			Ext.each(node.childNodes, function(childNode) {
				Ext.each(loadedLayersJSon, function(loadedLayerJSon) {
					var loadedLayerTreePaths = this.treePaths[loadedLayerJSon['layerId']];
					Ext.each(loadedLayerTreePaths, function(loadedLayerTreePath) {
						// 2 nodes may have the same name at the same level,
						// but not the same hierarchy (parent, grand-parent, etc.)
						if (this.hasSameParents(nodeTreePath, loadedLayerTreePath)) {
							// Find child nodes/layers to highlight
							var loadedJSonNode = loadedLayerTreePath[nodeTreePath.length];
							if ((
									// Selected node
									childNode.text
									&& loadedJSonNode == childNode.text
								) || (
									// Selected layer
									loadedJSonNode == childNode.layerId
								)) {
								childNode.setHighlight(true);
							}
						}
					}, this);
				}, this);
			}, this);
		}
	},

	hasSameParents: function(treeNodePath, jsonNodePath) {
		for (var i=0; i<treeNodePath.length; i++) {
			if (!jsonNodePath[i]
					|| treeNodePath[i].text != jsonNodePath[i]) {
				return false;
			}
		}
		return true;
	},

	createNode : function(attr){
		// apply baseAttrs, nice idea Corey!
		if(this.baseAttrs){
			Ext.applyIf(attr, this.baseAttrs);
		}
		if(this.applyLoader !== false && !attr.loader){
			attr.loader = this;
		}
		if(Ext.isString(attr.uiProvider)){
			attr.uiProvider = this.uiProviders[attr.uiProvider] || eval(attr.uiProvider);
		}

		var node = null;
		if(attr.nodeType){
			node = new Ext.tree.TreePanel.nodeTypes[attr.nodeType](attr);
		} else {
			node = attr.leaf ?
						// Create a LayerLeaf instead of a TreeNode
						new Ext.ux.tree.LayerLeaf(attr) :
						// Create a LayerNode instead of a AsyncTreeNode
						new Ext.ux.tree.LayerNode(attr);
		}
		node.on('checkchange', function(node, checked) { node.select(); });
		return node;
	},

	getLoadedLayersJSon: function() {
		var layersJSon = [];
		if (this.layerStore) {
			this.layerStore.each(function(record) {
				var layer = record.getLayer();
				if (layer && layer.json) {
					layersJSon.push(layer.json);
				}
			});
		}
		return layersJSon;
	}
});
