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
		this.setLayer(new OpenLayers.Layer.ux.NCWMS(
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

	// Override
	setOptions: function(optionsPanel) {
		var serviceUrl = this.json['wmsServiceUrl'];

		// TODO Remove ExtJS dependency!!
		var url = serviceUrl + '?' + Ext.urlEncode({
			item: 'layerDetails',
			layerName: this.json['layerName'],
			request: 'GetMetadata'
		});

		var that = this;
		OpenLayers.Request.GET({
			url: url,
			scope: this,
			success: function (result, request) {
				this._setOptions(result, request, optionsPanel);
			},
			failure: function (result, request) {
				var resultMessage = 'Unknown error';
				try {
					var jsonData = Ext.util.JSON.decode(result.responseText);
					resultMessage = jsonData.data.result;
				} catch (err) {
					resultMessage = result.responseText;
				}
				// TODO Error on the page
				alert('Error while loading ncWMS options: ' + resultMessage);
			}
		});
	},

	/**
	 * Inspired on godiva2.js
	 * http://e-atlas.org.au/ncwms/js/godiva2.js
	 */
	_setOptions: function(result, request, optionsPanel) {
		if (this.layer == (optionsPanel.currentNode ? optionsPanel.currentNode.layer : null)) {
			var layerDetails = null;
			try {
				// TODO Remove ExtJS dependency!!
				layerDetails = Ext.util.JSON.decode(result.responseText);
			} catch (err) {
				var resultMessage = result.responseText;
				// TODO Error on the page
				alert('Error while loading ncWMS options: ' + resultMessage);
				return;
			}

			if (layerDetails == null) {
				return;
			}

			if (layerDetails.exception) {
				// TODO Error on the page
				alert('Error while loading ncWMS options: ' +
					(layerDetails.exception.message ? layerDetails.exception.message : layerDetails.exception));
				return;
			}

			// TODO Do something with those
			var units = layerDetails.units;
			var copyright = layerDetails.copyright;
			var palettes = layerDetails.palettes;

			// Z Axis (Elevation/Depth) option
			var zAxis = layerDetails.zaxis;
			if (zAxis != null) {
				var zAxisParam = 'ELEVATION';

				// Currently selected value.
				var zValue = this.getParameter(zAxisParam, 0);

				var zAxisOptions = [];
				var zAxisLabel = (zAxis.positive ? 'Elevation' : 'Depth') + ' (' + zAxis.units + ')';
				// Populate the drop-down list of z values
				// Make z range selector invisible if there are no z values
				var zValues = zAxis.values;
				var zDiff = 1e10; // Set to some ridiculously-high value
				var defaultValue = 0;
				for (var j = 0; j < zValues.length; j++) {
					// Create an item in the drop-down list for this z level
					var zLabel = zAxis.positive ? zValues[j] : -zValues[j];
					zAxisOptions[j] = [zValues[j], zLabel];
					// Find the nearest value to the currently-selected
					// depth level
					var diff = Math.abs(parseFloat(zValues[j]) - zValue);
					if (diff < zDiff) {
						zDiff = diff;
						defaultValue = zValues[j];
					}
				}

				if (zAxisOptions.length > 1) {
					var zAxisSelect = {
						xtype: "combo",
						name: zAxisParam,
						editable: false,
						fieldLabel: zAxisLabel,
						value: defaultValue,
						typeAhead: false,
						triggerAction: 'all',
						lazyRender: true,
						mode: 'local',
						store: new Ext.data.ArrayStore({
							id: 0,
							fields: [
								'name',
								'title'
							],
							data: zAxisOptions
						}),
						valueField: 'name',
						displayField: 'title',
						allowBlank: false,
						listeners: {
							scope: this,
							select: this.onZAxisChange
						}
					};
					optionsPanel.addOption(this, zAxisSelect);
				}
			}

			// Set the scale value if this is present in the metadata
			if (typeof(layerDetails.scaleRange) !== 'undefined' &&
					layerDetails.scaleRange != null &&
					layerDetails.scaleRange.length > 1 &&
					layerDetails.scaleRange[0] != layerDetails.scaleRange[1]) {
				var scaleParam = 'COLORSCALERANGE';

				var scaleMinVal = null;
				var scaleMaxVal = null;

				var minmaxField = new Ext.ux.form.MinMaxField({
					fieldLabel: 'Color ranges',
					name: scaleParam,
					decimalPrecision: 4,
					listeners: {
						scope: this,
						change: this.onMinMaxChange
					}
				});

				var actualValue = this.getParameter(scaleParam, null);

				if (actualValue != null) {
					var values = actualValue.split(minmaxField.spacer);
					scaleMinVal = values[0];
					scaleMaxVal = values[1];
				}

				if (scaleMinVal == null) {
					scaleMinVal = parseFloat(layerDetails.scaleRange[0]);
				}
				if (scaleMaxVal == null) {
					scaleMaxVal = parseFloat(layerDetails.scaleRange[1]);
				}

				minmaxField.setValue(scaleMinVal, scaleMaxVal);

				optionsPanel.addOption(this, minmaxField);
			}
		}
	},

	onZAxisChange: function(evt) {
		this._onOptionChange(evt, evt.value, evt.startValue);
	},
	onMinMaxChange: function(evt) {
		var field = evt[0];
		// Workaround - Listeners of minMaxField are sent to their children.
		if (field.minMaxField) {
			field = field.minMaxField;
		}
		this._onOptionChange(field, evt[1], evt[2]);
	},

	_onOptionChange: function(field, newValue, oldValue) {
		var newParams = {};

		var fieldName = field.getName();
		if (fieldName) {
			var fieldValue = field.getValue();

			// Set the new parameter, or unset it if it has a null value (don't remove STYLES - it's mandatory).
			if (typeof(fieldValue) == 'undefined' || fieldValue == null || fieldValue == '') {
				// Remove the param from the URL - Some server don't like to have en empty parameter
				// NOTE: OpenLayers filter null values
				newParams[fieldName] = null;
			} else {
				newParams[fieldName] = fieldValue;
			}
		}

		this.setParameters(newParams);
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
