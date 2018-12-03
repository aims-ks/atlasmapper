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

Atlas.Info = Ext.extend(Ext.Component, {
	mapPanel: null,

	// Array of Ext.ux.IFramePanel
	tabs: null,
	startingTab: 0,
	optionsTab: -1,
	descriptionTab: -1,
	loadingLayerId: null,
	selectedLayer: null,

	// NOTE: The version must match the version in the server /src/main/java/au/gov/aims/atlasmapperserver/module/Info.java
	CURRENT_CONFIG_VERSION: 1.0,

	initComponent: function() {
		Atlas.Info.superclass.initComponent.call(this);
		this.tabs = [];
		if (this.config && this.config['config']) {
			if (typeof(this.config['version']) != 'undefined' && this.config['version'] > this.CURRENT_CONFIG_VERSION) {
				var err = "The version of the configuration of the Info module ("+this.config['version']+") is not supported by this client (support up to version: "+this.CURRENT_CONFIG_VERSION+").";
				alert(err);
				throw err;
			}

			Ext.iterate(this.config['config'], function(tabName, tab) {
				var tabObj = tab;
				if (typeof(tab) == 'string') {
					tabObj = {"defaultContent": tab};
				}

				var panelConfig = {
					mapPanel: this.mapPanel,
					startingTab: !!tabObj['startingTab']
				};

				if (typeof(this.header) === 'undefined' || this.header) {
					panelConfig.title = tabName;
				}

				if (tabObj.hasOwnProperty('order')) {
					panelConfig.order = tabObj['order'];
				} else {
					panelConfig.order = tabName
				}

				panelConfig.tabType = tabObj['type'];
				if (tabObj['type'] === 'options') {
					panelConfig.cls = 'infoTab';
					if (tabObj['defaultContent']) {
						panelConfig.html = tabObj['defaultContent'];
					}
					this.tabs.push(new Atlas.OptionsPanel(panelConfig));
				} else {
					panelConfig.iframe = {
						mapPanel: this.mapPanel,
						border: false,
						cls: 'iframeTab'
					};
					if (tabObj['defaultContent']) {
						panelConfig.iframe.html = tabObj['defaultContent'];
					}
					if (tabObj['defaultUrl']) {
						panelConfig.iframe.src = tabObj['defaultUrl'];
					}

					this.tabs.push(new Atlas.DescriptionPanel(panelConfig));
				}
			}, this);

			this.tabs.sort(function(a, b) {
				if (a.order < b.order) {
					return -1;
				}
				if (a.order > b.order) {
					return 1;
				}
				return 0;
			});

			Ext.iterate(this.tabs, function(panelConfig, index) {
				// Check if the current tab is the starting tab.
				if (panelConfig['startingTab']) {
					this.startingTab = index;
				}

				if (panelConfig['tabType'] === 'options') {
					this.optionsTab = index;
				} else if (panelConfig['tabType'] === 'description') {
					this.descriptionTab = index;
				}
			}, this);
		}
	},

	setLoadingLayerId: function(loadingLayerId) {
		this.loadingLayerId = loadingLayerId;
		if (loadingLayerId != null) {
			for (var i=0; i<this.tabs.length; i++) {
				// TODO Show a loading image
				this.tabs[i].setContent('<i>Loading...</i>');
			}
		}
	},
	getLoadingLayerId: function() {
		return this.loadingLayerId;
	},

	selectionChange: function(node) {
		// Check if the event concern this Info instance.
		if (!node) {
			// Nothing selected

			//this.setTabsSrc();
			this.setTabsContent();
			this.setOptions();
		} else {
			// Leaf or node

			var atlasLayer = (node && node.layer && node.layer.atlasLayer) ? node.layer.atlasLayer : null;

			this.setTabsContent(atlasLayer);
			// Show layer options (nodes are not layers)
			if (typeof(node.layer) != 'undefined' && node.layer != null) {
				this.setOptions(node);
			} else {
				this.setOptions();
			}
		}
	},

	setTabsContent: function(atlasLayer) {
		if (atlasLayer && atlasLayer.layer && atlasLayer.layer.events) {
			atlasLayer.layer.events.on({
				'layerupdate': function(evt) {
					if (this.selectedLayer === atlasLayer) {
						this._setTabsContent(atlasLayer);
					}
				},
				scope: this
			});
		}
		this._setTabsContent(atlasLayer);
	},

	// Set tab SRC, ignoring options tab
	_setTabsContent: function(atlasLayer) {
		this.selectedLayer = atlasLayer;
		var layerJSon = {};
		if (atlasLayer && atlasLayer.json) {
			layerJSon = atlasLayer.json;
		}
		var srcs;
		if (layerJSon && layerJSon['infoHtmlUrls']) {
			srcs = layerJSon['infoHtmlUrls'];
		}
		// Set the new SRC for all tabs except the Options' tab
		for (var i=0; i<this.tabs.length; i++) {
			var srcInd = (i < this.optionsTab) ? i : i-1;
			if (i != this.optionsTab) {
				if (srcs && srcs[srcInd] && srcs[srcInd].length > 0) {
					// Set the new tab's SRC
					this.tabs[i].setSrc(srcs[srcInd]);
				} else {
					if (i == this.descriptionTab) {
						this.tabs[i].setLayer(atlasLayer);
					} else {
						// Reset the tab's SRC
						this.tabs[i].setSrc();
					}
				}
			}
		}
	},

	setOptions: function(node) {
		if (this.optionsTab > 0 && this.tabs && this.tabs[this.optionsTab]) {
			this.tabs[this.optionsTab].setLayerOptions(node);
		}
	}
});
