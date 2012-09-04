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

Ext.namespace("Atlas");

Atlas.MapToolsPanel = Ext.extend(Ext.form.FormPanel, {

	mapPanel: null,

	constructor: function(attributes) {
		var that = this;

		var newAttributes = Ext.apply({
			xtype: 'form',
			layout: 'hbox',
			defaultType: 'textfield',
			defaults: {
				margins: {
					top: 2,
					right: 0,
					bottom: 2,
					left: 4
				}
			},

			// Add a shadow
			floating: true, shadowOffset: 6,

			items: [
				{
					ref: 'searchField',
					name: 'search',
					hideLabel: true,
					margins: {
						top: 6,
						right: 0,
						bottom: 6,
						left: 6
					},
					// Needed to be able to catch 'keydown' event
					enableKeyEvents: true,
					listeners: {
						'specialkey': function(field, evt) {
							if (evt.getKey() == evt.ENTER) {
								that.search(field.getValue());
							}
						},
						// Prevent the Map from grabbing the keys (-/+ to zoom, arrows to pan, etc.)
						'keydown': function(field, evt) {
							evt.stopPropagation();
						}
					}
				}, {
					xtype: 'button',
					iconCls: 'searchButton',
					scale: 'medium',
					handler: function(btn, evt) {
						that.search(btn.ownerCt.searchField.getValue());
					}
				}
			],
			region: 'north'
		}, attributes);

		Atlas.MapToolsPanel.superclass.constructor.call(this, newAttributes);
	},

	// TODO
	// [X] 1. Put this somewhere else
	// [X] 2. Try to find a way to implement this in a modular way
	// [X] 3. Allow arrows, home, end keys to move the cursor in the search field
	// [X] 4. Config to specify which layer are used for search (ArcGIS only, for now)
	// [X] 5. Do a real search (ArcGIS API)
	search: function(query) {
		var searchResultsLayer = new Atlas.Layer.SearchResults(this.mapPanel, {query: query});

		if (searchResultsLayer.layer) {
			this.mapPanel.map.addLayer(searchResultsLayer.layer);
		}
	}
});
