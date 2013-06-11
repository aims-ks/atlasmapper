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

Atlas.Layer.ArcGISMapServer = OpenLayers.Class(Atlas.Layer.AbstractLayer, {
	exportUrl: null,
	identifyUrl: null,

	/**
	 * Constructor: Atlas.Layer.ArcGISMapServer
	 *
	 * Parameters:
	 * jsonLayer - {Object} Hashtable of layer attributes
	 * mapPanel - {Object} Instance of the MapPanel in which the layer is used
	 */
	initialize: function(mapPanel, jsonLayer, parent) {
		Atlas.Layer.AbstractLayer.prototype.initialize.apply(this, arguments);

		var url = this.json['serviceUrl'];
		if (this.json['arcGISPath']) {
			url += '/' + this.json['arcGISPath'];
		}
		url += '/MapServer/';

		this.exportUrl = url + 'export';
		this.identifyUrl = url + 'identify';

		this.setLayer(new OpenLayers.Layer.ArcGIS93Rest(
			this.getTitle(),
			this.exportUrl,
			this.getArcGISLayerParams(),
			this.getArcGISLayerOptions()
		));
	},

	getServiceLayer: function() {
		var serviceLayer = this;
		while (serviceLayer != null && serviceLayer.json['dataSourceType'] != 'SERVICE') {
			serviceLayer = serviceLayer.parent;
		}
		return serviceLayer;
	},

	getArcGISLayerParams: function() {
		var layerParams = {
			layers: "show:" + (this.json['layerName'] || this.json['layerId']),
			transparent: true
		};

		if (typeof(this.json['forcePNG24']) !== 'undefined' && this.json['forcePNG24']) {
			layerParams['format'] = 'PNG24';
		}

		if (typeof(this.json['olParams']) !== 'undefined') {
			layerParams = this.applyOlOverrides(layerParams, this.json['olParams']);
		}

		return layerParams;
	},

	getArcGISLayerOptions: function() {
		if (this.json == null) {
			return null;
		}

		var layerOptions = {
			// Big tiles has less issues with labeling
			tileSize: new OpenLayers.Size(512, 512),
			isBaseLayer: false,
			wrapDateLine: true
			// Single tile make very nice map, with perfect labeling, but has some issues
			// near the date line and it cost more for the server (need more resources).
			//singleTile: true
		};
		// ESRI layers do not support
		// Google projection (EPSG:900913) but they support
		// ESRI projection (EPSG:102100) which is the same.
		if (this.json['projection']) {
			layerOptions.projection = this.json['projection'];
		} else if (this.mapPanel && this.mapPanel.map && this.mapPanel.map.getProjection() === 'EPSG:900913') {
			layerOptions.projection = 'EPSG:102100';
		}

		if (typeof(this.json['olOptions']) !== 'undefined') {
			layerOptions = this.applyOlOverrides(layerOptions, this.json['olOptions']);
		}

		return layerOptions;
	},

	/**
	 * Method: getFeatureInfoURL
	 * Build an object with the relevant options for the GetFeatureInfo request
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
		var layerProjection = this.layer.projection;

		// Remove EPSG from the projection code.
		var layerProjectionWKID = layerProjection.getCode().replace(/EPSG:\s*/gi, "");

		// clickPosition is in pixels
		var lonLatPoint = this.mapPanel.map.getLonLatFromPixel(clickPosition);

		// lonLatPoint is in the unit of the map, which is probably not in longitude-latitude. I prefer to make a point with it (x, y), to avoid confusion.
		var reprojectedPoint = new OpenLayers.Geometry.Point(lonLatPoint.lon, lonLatPoint.lat);
		var reprojectedMapExtent = this.mapPanel.map.getExtent();

		// The reprojectedPoint and reprojectedMapExtent are not reprojected yet.
		if (this.mapPanel.map.getProjectionObject() != layerProjection) {
			reprojectedPoint.transform(this.mapPanel.map.getProjectionObject(), layerProjection);
			reprojectedMapExtent.transform(this.mapPanel.map.getProjectionObject(), layerProjection);
		}

		// IMPORTANT: Spaces are not welcome in params values (ArcGIS has some problems reading values with encoded spaces)
		// API Doc: http://resources.esri.com/help/9.3/arcgisserver/apis/rest/identify.html
		var params = {
			f: "json",
			pretty: "true", // Some server don't return the same value without this...
			geometry: '{"x":'+reprojectedPoint.x+',"y":'+reprojectedPoint.y+',"spatialReference":{"wkid":'+layerProjectionWKID+'}}',
			tolerance: 5,
			returnGeometry: false, //true, TODO Enable geometry highlight
			mapExtent: '{"xmin":'+reprojectedMapExtent.left+',"ymin":'+reprojectedMapExtent.bottom+',"xmax":'+reprojectedMapExtent.right+',"ymax":'+reprojectedMapExtent.top+',"spatialReference":{"wkid":'+layerProjectionWKID+'}}',
			imageDisplay: '400,400,96', // ??
			geometryType: 'esriGeometryPoint',
			sr: layerProjectionWKID,
			layers: 'all:'+this.json['layerName'] // Query only the current layer, from the list of all layers from the service. See the API.
		};

		// ACTIVATE FOR SERVICE REQUESTS - Request all layers of the service at once
		//params.layers = 'all';

		// Since there is no OpenLayers API for this class, it can be useful to see the resulted URL.
		// If you want to do some debugging, activate the following line in the generated client.
		//console.log(OpenLayers.Util.urlAppend(this.identifyUrl, OpenLayers.Util.getParameterString(params || {})));

		return {
			url: this.identifyUrl,
			params: params
		};
	},

	// ACTIVATE FOR SERVICE REQUESTS - One request per service (instead of the same request repeated for all layers)
	/*
	getFeatureInfoLayerID: function() {
		var serviceLayer = this.getServiceLayer();
		if (serviceLayer == null) {
			return null;
		}
		return serviceLayer.json['layerId'];
	},
	*/

	// Override
	getFeatureInfoResponseFormat: function() {
		return new OpenLayers.Format.WMSGetFeatureInfo();
	},

	/**
	 * Return the HTML chunk that will be displayed in the balloon.
	 * @param xmlResponse RAW XML response
	 * @param textResponse RAW text response
	 * @return {String} The HTML content of the feature info balloon, or null if the layer info should not be shown.
	 */
	// Override
	processFeatureInfoResponse: function(responseEvent) {
		if (!responseEvent || !responseEvent.text) {
			return false;
		}

		var jsonResponse = eval("(" + responseEvent.text + ")");
		if (!jsonResponse || !jsonResponse['results'] || !jsonResponse['results'][0]) {
			return false;
		}

		var title = this.getTitle();
		/*
		// ACTIVATE FOR SERVICE REQUESTS - display service name instead of the top layer name
		var serviceLayer = this.getServiceLayer();
		if (serviceLayer != null) {
			title = serviceLayer.getTitle();
		}
		*/

		var htmlResponse = '';
		for (var i=0; i < jsonResponse['results'].length; i++) {
			var attributes = jsonResponse['results'][i]['attributes'];
			if (attributes) {
				htmlResponse += '<table>';
				var odd = true;
				for (var key in attributes) {
					if (attributes.hasOwnProperty(key)) {
						var value = attributes[key];
						htmlResponse += '<tr class="' + (odd?'odd':'even') + '">';
						htmlResponse += '<td class="key">'+key+'</td>';
						htmlResponse += '<td class="value">'+value+'</td>';
						htmlResponse += '</tr>';
						odd = !odd;
					}
				}
				htmlResponse += '</table>';
			}
		}

		// TODO Parse jsonResponse attributes according to a template specified for this layer / service / data source.
		return '<div class="arcgisFeatureInfo"><h3>' + title + '</h3>' + (htmlResponse ? htmlResponse : ('<pre>' + responseEvent.text + '</pre>')) + '</div>';
	}
});
