/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2013 Australian Institute of Marine Science
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
 * See: http://dev.sencha.com/deploy/ext-3.4.0/examples/form/custom.html
 */

Atlas.Trees = Atlas.Trees || {};

Atlas.Trees.SearchTab = Ext.extend(Ext.Panel, {
	title: 'Search',
	autoScroll: true,
	selected: null,
	mapPanel: null,
	resultTemplate: null,

	constructor: function(param) {
		this.mapPanel = param.mapPanel;

		// Custom rendering Template for the View
		// See: http://docs.sencha.com/extjs/4.0.7/#!/api/Ext.XTemplate
		this.resultTemplate = new Ext.XTemplate(
			'<tpl for=".">',
			'<div class="search-item" id="{[this.getElementId(values.layerId)]}">',
				'<div class="result-title">{title}</div>',
				'<tpl if="excerpt">',
					'<div class="result-excerpt">{excerpt}</div>',
				'</tpl>',
				 // use opposite if statement to simulate 'else' processing:
				'<tpl if="!excerpt">',
					'<div class="result-excerpt result-empty">No description available</div>',
				'</tpl>',
			'</div></tpl>',
			{
				getElementId: function(layerId) {
					return this.scope.getElementId(layerId);
				},
				scope: this
			}
		);

		Atlas.Trees.SearchTab.superclass.constructor.apply(this, arguments);
	},

	initComponent: function() {
		this.store = new Ext.data.Store({
			proxy: new Ext.data.ScriptTagProxy({
				// /atlasmapper/public/search.jsp
				url: Atlas.conf && Atlas.conf['searchServiceUrl'] ? Atlas.conf['searchServiceUrl'] : null
			}),
			reader: new Ext.data.JsonReader({
				root: 'data',
				totalProperty: 'count',
				id: 'layerId'
			}, [
				{name: 'layerId', mapping: 'layerId'},
				{name: 'title', mapping: 'title'},
				{name: 'excerpt', mapping: 'excerpt'}
			]),

			baseParams: {
				limit: 20,
				client: Atlas.conf['clientId'],
				type: 'LAYER'
			}
		});

		Atlas.Trees.SearchTab.superclass.initComponent.call(this);
		this.store.load({params: {
			start: 0,
			limit: 20,
			client: Atlas.conf['clientId'],
			type: 'LAYER'
		}});

		this.add(new Ext.DataView({
			tpl: this.resultTemplate,
			listeners: {
				'click': function(evt, index) {
					var layerId = evt.getStore().getAt(index).id;
					this._selectSearchResult(layerId);
				},
				scope: this
			},
			store: this.store,
			itemSelector: 'div.search-item'
		}));

		var that = this;
		this.getTopToolbar().items.add(
			new Ext.ux.form.SearchField({
				store: this.store,
				width: 320,

				// Needed to be able to catch 'keydown' event
				enableKeyEvents: true,
				listeners: {
					// Prevent the Map from grabbing the keys (-/+ to zoom, arrows to pan, etc.)
					'keydown': function(field, evt) {
						evt.stopPropagation();
					}
				},

				// Override the onTrigger1Click method. Not as good as an
				// event listener, but that's the best I could do.
				onTrigger1Click: function() {
					Ext.ux.form.SearchField.prototype.onTrigger1Click.apply(this, arguments);
					that._selectSearchResult(null);
				},
				onTrigger2Click: function() {
					Ext.ux.form.SearchField.prototype.onTrigger2Click.apply(this, arguments);
					that._selectSearchResult(null);
				}
			})
		);

		this.getBottomToolbar().bindStore(this.store);

		// Deselect the search result when leaving the search tab
		this.on('deactivate', function() {
			that._selectSearchResult(null);
		});
	},

	getElementId: function(layerId) {
		return this._getElementPrefix() + layerId;
	},
	getLayerIdFromElement: function(elementId) {
		return elementId.substring(this._getElementPrefix().length);
	},
	_getElementPrefix: function() {
		return this.mapPanel.mapId + '_';
	},

	getChecked: function() {
		var layerId = null;
		if (this.selected) {
			layerId = this.getLayerIdFromElement(this.selected.id);
			this.selected.removeClass("search-item-highlighted");
			this.selected = null;
		}
		return layerId;
	},

	_selectSearchResult: function(layerId) {
		// Highlight / Unhighlight search result
		if (this.selected) {
			this.selected.removeClass("search-item-highlighted");
		}

		var searchItem = Ext.get(this.getElementId(layerId));

		if (searchItem == null || this.selected == searchItem) {
			this.selected = null;
			// Fire event
			this.fireEvent('searchResultSelectionChange', null);
		} else {
			searchItem.addClass("search-item-highlighted");
			this.selected = searchItem;

			// Fire event
			this.fireEvent('searchResultSelectionChange', layerId);
		}
	},

	// The search field is added in the initComponent method
	tbar: [
		'Search: ', ' '
	],

	// The paging store is set in the initComponent method
	bbar: new Ext.PagingToolbar({
		pageSize: 20,
		displayInfo: true,
		displayMsg: 'Layers {0} - {1} of {2}',
		emptyMsg: "No result"
	})
});
