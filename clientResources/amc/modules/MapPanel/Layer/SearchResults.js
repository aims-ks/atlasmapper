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

/**
 * This class do a search according to the jsonLayer.query attribute
 * and the search configuration (TODO), than send the search results
 * to a OpenLayers.Layer.ux.SearchResults, which display them using
 * a paging system (TODO).
 */

// Namespace declaration (equivalent to Ext.namespace("Atlas.Layer");)
window["Atlas"] = window["Atlas"] || {};
window["Atlas"]["Layer"] = window["Atlas"]["Layer"] || {};

Atlas.Layer.SearchResults = OpenLayers.Class(Atlas.Layer.AbstractLayer, {
	MAX_QUERY_DISPLAY_LENGTH: 30,
	MARKER_COLORS: ['red', 'yellow', 'green', 'blue', 'orange', 'purple', 'paleblue', 'pink', 'brown', 'darkgreen'],
	MARKER_LETTERS: 'ABCDEFGHIJKLMNOPQRSTUVWXYZ',
	NB_RESULTS_PER_PAGE: 10,
	query: null,
	results: null,
	color: null,
	searchServiceUrl: null,
	searchCount: 0,

	page: -1,
	nbPages: 0,

	// True when the layer is searching...
	searching: true,

	nbResults: null,

	/**
	 * Constructor: Atlas.Layer.SearchResults
	 *
	 * Parameters:
	 * jsonLayer - {Object} Hashtable of layer attributes
	 * mapPanel - {Object} Instance of the MapPanel in which the layer is used
	 */
	initialize: function(mapPanel, jsonLayer, parent) {
		if (Atlas.conf && Atlas.conf['searchServiceUrl'] && jsonLayer && jsonLayer.query) {
			this.searchServiceUrl = Atlas.conf['searchServiceUrl'];
		} else {
			// No search service - this class can not be initialised...
			return;
		}

		this.searchCount = Atlas.Layer.SearchResults.count;
		Atlas.Layer.SearchResults.count++;

		var colorNumber = (this.searchCount % this.MARKER_COLORS.length);
		this.color = this.MARKER_COLORS[colorNumber];

		// Clone the jsonLayer config
		jsonLayer = OpenLayers.Util.extend({}, jsonLayer);

		this.query = jsonLayer.query;

		var title = this.query;
		if (title.length > this.MAX_QUERY_DISPLAY_LENGTH) {
			title = title.substr(0, this.MAX_QUERY_DISPLAY_LENGTH-3) + '...'; // substr(from, length)
		}
		jsonLayer['title'] = title;

		Atlas.Layer.AbstractLayer.prototype.initialize.apply(this, arguments);

		this.searching = true;
		this.performSearch(0);

		this.setLayer(new OpenLayers.Layer.ux.SearchResults(this.getTitle()));

		this.layer.events.on({
			'featureselected': function(evt) {
				var resultId = evt.feature;
				if (resultId) {
					if (typeof(resultId) !== 'string') {
						resultId = resultId._id;
					}
					var resultEl = Ext.fly(resultId);
					if (resultEl) {
						resultEl.addClass('highlight');
					}
				}
			},
			'featureunselected': function(evt) {
				var resultId = evt.feature;
				if (resultId) {
					if (typeof(resultId) !== 'string') {
						resultId = resultId._id;
					}
					var resultEl = Ext.fly(resultId);
					if (resultEl) {
						resultEl.removeClass('highlight');
					}
				}
			},
			scope: this
		});
	},

	// Override
	getExtent: function() {
		// The layer may move or get resized. the extent can not be cached.
		if (!this.layer) {
			return null;
		}
		return this.layer.getDataExtent();
	},

	// Override
	canBeLocated: function() {
		// The extent is null during the drawing process, but the locate button should not be disabled.
		return true;
	},

	// Override
	getLocateClosest: function() {
		// Show every part of this layer, do not hide any search results.
		return false;
	},

	performSearch: function(page) {
		if (page !== this.page) {
			if (this.layer) {
				this.layer.setResults(null);
			}

			if (!this.searching) {
				this.searching = true;
				if (this.layer && this.layer.events) {
					this.layer.events.triggerEvent('layerupdate');
				}
			}

			this.page = page;
			var params = {
				client: Atlas.conf['clientId'],
				live: Atlas.core.live,
				query: this.query,
				bounds: null,
				offset: this.page * this.NB_RESULTS_PER_PAGE,
				qty: this.NB_RESULTS_PER_PAGE,
				noCache: (new Date()).getTime() // Anti-caching
			};

			OpenLayers.Request.GET({
				url: this.searchServiceUrl,
				params: params,
				scope: this,
				success: this.showResults,
				failure: function (result, request) {
					// TODO Error on the page
					alert('The application has failed to perform a search');
				}
			});
		}
	},

	showResults: function(response) {
		this.searching = false;

		// Decode the response
		var jsonResponse = eval("(" + response.responseText + ")");

		var _nbResults = 0;
		if (jsonResponse) {
			if (jsonResponse.success) {
				var value = jsonResponse.data;

				if (value) {
					_nbResults = value.length || 0;
					this.nbPages = Math.ceil(_nbResults / this.NB_RESULTS_PER_PAGE);

					if (value && value.results) {
						this.results = value.results;
						// Set the marker & bullet icon URLs in the search results and fix the ID
						for (var i=0, len=this.results.length; i<len; i++) {
							var letter = this.MARKER_LETTERS[i % this.MARKER_LETTERS.length];
							this.results[i].id = this.searchCount + '_' + this.results[i].id;
							this.results[i].markerUrl = 'resources/markers/'+this.color+letter+'.png';
							this.results[i].bulletUrl = 'resources/markers/'+this.color+letter+'.png';
						}

						if (this.layer) {
							this.layer.setResults(this.results);
						}
					}
				}
			} else {
				var errorMsg = 'No error message.';
				if (jsonResponse.errors) {
					errorMsg = jsonResponse.errors;
				}
				// TODO Error on the page
				alert('The application has failed to perform a search:\n' + errorMsg);
			}
		} else {
			// TODO Error on the page
			alert('The application has failed to perform a search:\nThe search service didn\'t returne anything.');
		}

		this.nbResults = _nbResults;
		this.layer.setName(this.getTitle());
	},

	// override
	getTitle: function() {
		var title = Atlas.Layer.AbstractLayer.prototype.getTitle.apply(this, arguments);

		// Add the number of result to the layer title
		if (this.nbResults != null) {
			title += ' ('+this.nbResults+')';
		}

		return title;
	},

	// override
	getDescription: function() {
		var that = this;

		// Main container
		var description = new Ext.Element(Ext.DomHelper.createDom({tag: 'div', cls: 'searchResultsDescription'}));

		// Title
		var title = new Ext.Element(Ext.DomHelper.createDom({tag: 'div', cls: 'query', html: '<span class="label">Query:</span> ' + this.query}));
		description.appendChild(title);

		if (this.searching) {
			description.appendChild(new Ext.Element(Ext.DomHelper.createDom({
				tag: 'div',
				cls: 'searching',
				html: '<span>Please wait...</span>'
			})));
		} else if (!this.results || !this.results.length) {
			description.appendChild(new Ext.Element(Ext.DomHelper.createDom({
				tag: 'div',
				cls: 'noresults',
				html: '<span>No result found.</span>'
			})));
		} else {
			// Pager
			// [<] 10/35 [>]
			var nbPages = this.nbPages;
			var currentPage = this.page;
			if (nbPages > 1) {
				var prevPage = currentPage > 0 ? currentPage - 1 : 0;
				var nextPage = currentPage < nbPages-1 ? currentPage + 1 : nbPages-1;

				var pager = new Ext.Element(Ext.DomHelper.createDom({tag: 'div', cls: 'pager'}));

				var prevPageBtn = new Ext.Element(Ext.DomHelper.createDom({tag: 'a', cls: 'prevPage', html: '<span>&lt;</span>' }));
				// This event listeners is removed by Ext.ux.IFramePanel.update()
				prevPageBtn.addListener('click', function() { that.performSearch(prevPage); });
				pager.appendChild(prevPageBtn);

				pager.appendChild(new Ext.Element(Ext.DomHelper.createDom({tag: 'span', html: (currentPage+1) + ' / ' + nbPages })));

				var nextPageBtn = new Ext.Element(Ext.DomHelper.createDom({tag: 'a', cls: 'nextPage', html: '<span>&gt;</span>' }));
				// This event listeners is removed by Ext.ux.IFramePanel.update()
				nextPageBtn.addListener('click', function() { that.performSearch(nextPage); });
				pager.appendChild(nextPageBtn);

				description.appendChild(pager);

				// Internet Explorer need this to display the pager correctly
				description.appendChild(new Ext.Element(Ext.DomHelper.createDom({tag: 'div', style: 'clear: both'})));
			}

			// Result list
			var listContainer = new Ext.Element(Ext.DomHelper.createDom({tag: 'div', cls: 'results', html: '<span>Results:</span>'}));
			var list = new Ext.Element(Ext.DomHelper.createDom({tag: 'ol'}));

			for (var i=0; i<this.results.length; i++) {
				var result = this.results[i];
				var id = this.results[i].id;
				var li = new Ext.Element(Ext.DomHelper.createDom({
						tag: 'li',
						id: id,
						html: result.title,
						style: "background-image: url('"+result.bulletUrl+"');"
				}));

				// Those event listeners are removed by Ext.ux.IFramePanel.update()
				li.addListener('mouseover', function() { that._mouseOver(this); });
				li.addListener('mouseout', function() { that._mouseOut(this); });
				li.addListener('click', function() { that._click(this); });

				list.appendChild(li);
			}

			listContainer.appendChild(list);
			description.appendChild(listContainer);
		}

		return description;
	},

	_mouseOver: function(li) {
		var featureId = li.id;
		this.layer.events.triggerEvent('featureselected', {feature: featureId});
	},
	_mouseOut: function(li) {
		var featureId = li.id;
		this.layer.events.triggerEvent('featureunselected', {feature: featureId});
	},
	_click: function(li) {
		var featureId = li.id;
		this.layer.locate(featureId);
	}
});

// Static attribute
Atlas.Layer.SearchResults.count = 0;
