
Atlas.Info = Ext.extend(Ext.Component, {
	mapPanel: null,

	// Array of Ext.ux.IFramePanel
	tabs: null,
	startingTab: 0,
	optionsTab: -1,
	descriptionTab: -1,
	loadingLayerId: null,

	initComponent: function() {
		Atlas.Info.superclass.initComponent.call(this);
		this.tabs = [];
		if (Atlas.conf
				&& Atlas.conf['modules']
				&& Atlas.conf['modules']['Info']
				&& Atlas.conf['modules']['Info']['tabs']) {

			Ext.iterate(Atlas.conf['modules']['Info']['tabs'], function(tabName, tab) {
				var tabObj = tab;
				if (typeof(tab) == 'string') {
					tabObj = {"defaultContent": tab};
				}

				// Check if the current tab is the starting tab.
				if (tabObj['startingTab']) {
					this.startingTab = this.tabs.length;
				}

				var panelConfig = {
					mapPanel: this.mapPanel,
					title: tabName,
					cls: 'infoTab'
				};

				if (tabObj['type'] === 'options') {
					if (tabObj['defaultContent']) {
						panelConfig.html = tabObj['defaultContent'];
					}
					this.optionsTab = this.tabs.length;
					this.tabs.push(new Ext.ux.OptionsPanel(panelConfig));
				} else {
					if (tabObj['type'] === 'description') {
						this.descriptionTab = this.tabs.length;
					}
					if (tabObj['defaultContent']) {
						panelConfig.html = tabObj['defaultContent'];
					}
					if (tabObj['defaultUrl']) {
						panelConfig.src = tabObj['defaultUrl'];
					}

					this.tabs.push(new Ext.ux.IFramePanel(panelConfig));
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

			var json = (node && node.layer && node.layer.json) ?
					node.layer.json : {};

			//this.setTabsSrc(json['infoHtmlUrls']);
			this.setTabsContent(json);
			// Show layer options (nodes are not layers)
			if (node.isLeaf()) {
				this.setOptions(node.layer);
			} else {
				this.setOptions();
			}
		}
	},

	// Set tab SRC, ignoring options tab
	setTabsContent: function(layerJSon) {
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
						var description = Atlas.core.getLayerDescription(layerJSon);
						if (description) {
							this.tabs[i].setContent(description);
						} else {
							// Reset the tab's SRC
							this.tabs[i].setSrc();
						}
					} else {
						// Reset the tab's SRC
						this.tabs[i].setSrc();
					}
				}
			}
		}
	},

	setOptions: function(layer) {
		if (this.optionsTab > 0 && this.tabs && this.tabs[this.optionsTab]) {
			this.tabs[this.optionsTab].setLayerOptions(layer);
		}
	}
});
