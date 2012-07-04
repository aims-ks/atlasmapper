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

// Namespace declaration (equivalent to Ext.namespace("Atlas.Layer");)
window["Atlas"] = window["Atlas"] || {};
window["Atlas"]["Layer"] = window["Atlas"]["Layer"] || {};

Atlas.Layer.WMS = OpenLayers.Class(Atlas.Layer.AbstractLayer, {
	/**
	 * Constructor: Atlas.Layer.WMS
	 *
	 * Parameters:
	 * jsonLayer - {Object} Hashtable of layer attributes
	 * mapPanel - {Object} Instance of the MapPanel in which the layer is used
	 */
	initialize: function(mapPanel, jsonLayer) {
		Atlas.Layer.AbstractLayer.prototype.initialize.apply(this, arguments);

		// TODO Support Multiple URLS => this._getWMSExtraServiceUrls(),
		var layerParams = this.getWMSLayerParams();
		this.layer = this.extendLayer(new OpenLayers.Layer.WMS(
			this.getTitle(),
			this.getServiceUrl(layerParams),
			layerParams,
			this.getWMSLayerOptions()
		));
	},

	getWMSLayerParams: function() {
		if (this.json == null) {
			return null;
		}

		var isBaseLayer = !!this.json['isBaseLayer'];

		// Set the parameters used in the URL to request the tiles.
		var layerParams = {
			layers: this.json['layerName'] || this.json['layerId']
		};
		// The WMS version is also used in the WMS requests and the legend graphics.
		if (this.json['wmsVersion']) {
			layerParams.version = this.json['wmsVersion'];
		}

		if (this.json['wmsRequestMimeType']) {
			layerParams.format = this.json['wmsRequestMimeType'];
		}

		// Select default style if needed
		if (this.json['styles']) {
			for (var styleName in this.json['styles']) {
				var jsonStyle = this.json['styles'][styleName];
				if (styleName && jsonStyle["default"]) {
					layerParams.styles = styleName;
					break;
				}
			}
		}

		layerParams.transparent = !isBaseLayer;

		if (typeof(this.json['olParams']) !== 'undefined') {
			layerParams = this.applyOlOverrides(layerParams, this.json['olParams']);
		}

		return layerParams;
	},

	// TODO Use this method
	_getWMSExtraServiceUrls: function() {
		return this.json['extraWmsServiceUrls'];
	},

	getWMSLayerOptions: function() {
		if (this.json == null) {
			return null;
		}

		var isBaseLayer = !!this.json['isBaseLayer'];

		// Set the OpenLayer options, used by the library.
		var layerOptions = {
			isBaseLayer: isBaseLayer,
			displayInLayerSwitcher: true,
			wrapDateLine : true,
			buffer: 0
		};

		if (this.json['projection']) {
			layerOptions.projection = this.json['projection'];
		} else if (this.mapPanel.map) {
			layerOptions.projection = this.mapPanel.map.getProjectionObject()
		}

		if (Atlas.conf['mapOptions'] && Atlas.conf['mapOptions']['units']) {
			layerOptions.units = Atlas.conf['mapOptions']['units'];
		}

		if (isBaseLayer) {
			// Set the transition effect to something smooth.
			// This effect allow the user to view the base layer
			// when zooming in/out or panning, by resizing the
			// existing tiles and display them while waiting for
			// the new tiles.
			layerOptions.transitionEffect = 'resize';
		}

		if (typeof(this.json['olOptions']) !== 'undefined') {
			layerOptions = this.applyOlOverrides(layerOptions, this.json['olOptions']);
		}

		return layerOptions;
	}
});
