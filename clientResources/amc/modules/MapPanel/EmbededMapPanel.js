// Namespace declaration (equivalent to Ext.namespace("Atlas");)
window["Atlas"] = window["Atlas"] || {};

Atlas.MapPanel = OpenLayers.Class(Atlas.AbstractMapPanel, {
	initialize: function(config) {
		for (att in config) {
			if (config.hasOwnProperty(att)) {
				this[att] = config[att];
			}
		}

		this.initComponent();
	}
});
