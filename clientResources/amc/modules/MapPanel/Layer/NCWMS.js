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

Atlas.Layer.NCWMS = OpenLayers.Class(Atlas.Layer.WMS, {
	/**
	 * Constructor: Atlas.Layer.NCWMS
	 *
	 * Parameters:
	 * jsonLayer - {Object} Hashtable of layer attributes
	 * mapPanel - {Object} Instance of the MapPanel in which the layer is used
	 */
	initialize: function(mapPanel, jsonLayer, parent) {
		// Do not call initialize from WMS because it would create a useless WMS layer.
		Atlas.Layer.AbstractLayer.prototype.initialize.apply(this, arguments);

		// TODO Support Multiple URLS => this._getWMSExtraServiceUrls(),
		var layerParams = this.getWMSLayerParams();
		this.layer = this.extendLayer(new OpenLayers.Layer.ux.NCWMS(
			this.getTitle(),
			this.getServiceUrl(layerParams),
			layerParams,
			this.getWMSLayerOptions()
		));
	},

	/**
	 * Method: getFeatureInfoURL
	 * Build an object with the relevant WMS options for the GetFeatureInfo request
	 *
	 * Parameters:
	 * url - {String} The url to be used for sending the request
	 * layers - {Array(<OpenLayers.Layer.WMS)} An array of layers
	 * clickPosition - {<OpenLayers.Pixel>} The position on the map where the mouse
	 *     event occurred.
	 * format - {String} The format from the corresponding GetMap request
	 *
	 * return {
	 *     url: String
	 *     params: { String: String }
	 * }
	 */
	// Override
	getFeatureInfoURL: function(url, layer, clickPosition, format) {
		var layerId = layer.atlasLayer.json['layerId'];

		var params = {
			service: "WMS",
			version: layer.params.VERSION,
			request: "GetFeatureInfo",
			layers: [this.json['layerName']],
			query_layers: [this.json['layerName']],
			bbox: this.mapPanel.map.getExtent().toBBOX(null,
				layer.reverseAxisOrder()),
			height: this.mapPanel.map.getSize().h,
			width: this.mapPanel.map.getSize().w,
			format: format
		};

		if (parseFloat(layer.params.VERSION) >= 1.3) {
			params.crs = this.mapPanel.map.getProjection();
			params.i = clickPosition.x;
			params.j = clickPosition.y;
		} else {
			params.srs = this.mapPanel.map.getProjection();
			params.x = clickPosition.x;
			params.y = clickPosition.y;
			// Some NCWMS server has an issue with X, Y vs I, J
			params.i = clickPosition.x;
			params.j = clickPosition.y;
		}

		params.info_format = 'text/xml';

//		OpenLayers.Util.applyDefaults(params, this.vendorParams);

		return {
			url: url,
			params: OpenLayers.Util.upperCaseObject(params)
		};
	},

	/**
	 * Return the HTML chunk that will be displayed in the balloon.
	 * @param xmlResponse RAW XML response
	 * @param textResponse RAW text response
	 * @return {String} The HTML content of the feature info balloon, or null if the layer info should not be shown.
	 */
	// Override
	processFeatureInfoResponse: function(responseEvent) {
		if (!responseEvent || !responseEvent.request || !responseEvent.request.responseXML) {
			return null;
		}

		var xmlResponse = responseEvent.request.responseXML;
		if (!this._containsData(xmlResponse)) {
			return null;
		}

		return '<h3>' + this.getTitle() + '</h3>' + this.xmlToHtml(xmlResponse);
	},

	_containsData: function(xmlResponse) {
		var values = xmlResponse.getElementsByTagName('value');
		if (!values || !values[0]) {
			return false;
		}

		var childNodes = values[0].childNodes;
		if (!childNodes || !childNodes[0]) {
			return false;
		}

		var value = childNodes[0].nodeValue;
		if (typeof(value) === 'undefined' || value === '' || value === 'none') {
			return false;
		}

		return true;
	}
});
