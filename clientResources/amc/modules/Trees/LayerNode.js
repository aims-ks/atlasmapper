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
/**
 * LayerNode and LayerLeaf are TreeNode that hold a reference to the
 * configuration object for a layer.
 * Used for the Tree, in the AddLayersWindow
 */

Atlas.Trees = Atlas.Trees || {};

Atlas.Trees.LayerNode = Ext.extend(Ext.tree.AsyncTreeNode, {
	isHighlighted: false,

	// Set node json config - in a layer Object to be the same as the GeoExt nodes
	layer: null,
	layerId: null,

	constructor: function(attributes) {
		Atlas.Trees.LayerNode.superclass.constructor.call(this, attributes);
		this.layerId = attributes.layerId;

		var jsonLayer = {
			'layerType': 'DUMMY',
			'title': attributes.text,
			'layerId': attributes.layerId
		};
		var atlasLayer = Atlas.Layer.LayerHelper.createLayer(null, jsonLayer);
		if (atlasLayer) {
			this.layer = atlasLayer.layer;
		}
	},

	getLoader: function() {
		var owner;
		// The loader should never be null. If it's the case (bug?), create a LayerTreeLoader without layerStore (needed by the highlight feature).
		return this.loader || ((owner = this.getOwnerTree()) && owner.loader ? owner.loader : (this.loader = new Atlas.Trees.LayerTreeLoader(null)));
	},

	setHighlight: function(highlight) {
		if (highlight) {
			this.getUI().addClass('highlighted');
			this.isHighlighted = true;
		} else {
			this.getUI().removeClass('highlighted');
			this.isHighlighted = false;
		}
	}
});

Atlas.Trees.LayerLeaf = Ext.extend(Ext.tree.TreeNode, {
	isHighlighted: false,

	// Set node json config - in a layer Object to be the same as the GeoExt nodes
	layer: null,
	layerId: null,

	constructor: function(attributes) {
		Atlas.Trees.LayerLeaf.superclass.constructor.call(this, attributes);
		this.layerId = attributes.layerId;

		var jsonLayer = {
			'layerType': 'DUMMY',
			'title': attributes.text,
			'layerId': attributes.layerId
		};
		var atlasLayer = Atlas.Layer.LayerHelper.createLayer(null, jsonLayer);
		if (atlasLayer) {
			this.layer = atlasLayer.layer;
		}
	},

	getLoader : function(){
		var owner;
		// The loader should never be null. If it's the case (bug?), create a LayerTreeLoader without layerStore (needed by the highlight feature).
		return this.loader || ((owner = this.getOwnerTree()) && owner.loader ? owner.loader : (this.loader = new Atlas.Trees.LayerTreeLoader(null)));
	},

	setHighlight : function(highlight){
		if (highlight) {
			this.getUI().addClass('highlighted');
			this.isHighlighted = true;
		} else {
			this.getUI().removeClass('highlighted');
			this.isHighlighted = false;
		}
	}
});
