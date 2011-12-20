/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2011 Australian Institute of Marine Science
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
 * @author Greg Coleman
 */
Ext.namespace("Ext.ux");

Ext.ux.NCPlotPanel = Ext.extend(Ext.ux.form.CompositeFieldAnchor, {
	format: "d/m/Y",
	outputFormat: 'Y-m-d',
	fromDateField: null,
	thruDateField: null,
	disabledDatesText: "Layer not available for that date.",
	layer: null,
	timeSeriesText: null,
	mode: "",
	timeSeriesClickControl: null,
	mapPanel: null,
	timeSeriesButton: null,
	transectButton: null,
	transectDrawControl: null,

	initComponent: function() {
		var that = this;

		this.timeSeriesClickControl = new OpenLayers.Control.ux.NCTimeSeriesClickControl();

		this.transectDrawControl = new OpenLayers.Control.ux.NCTransectDrawControl();

		this.timeSeriesClickControl.map = this.mapPanel.map;
		this.timeSeriesClickControl.layer = this.layer;

		this.transectDrawControl.map = this.mapPanel.map;
		this.transectDrawControl.ncLayer = this.layer;

		this.mapPanel.map.addControl(this.timeSeriesClickControl);
		this.timeSeriesClickControl.deactivate();

		this.mapPanel.map.addControl(this.transectDrawControl);
		this.transectDrawControl.deactivate();

		this.timeSeriesButton = new Ext.Button({
			text: 'Time Series Plot',
			handler: function() {
				if (that.mode == "TS") {
					that.timeSeriesClickControl.deactivate();
					that.mapPanel.wmsFeatureInfo.activate();
					that.timeSeriesPanel.setVisible(false);
					that.timeSeriesButton.setText("Time Series Plot");
					that.mode = "";
					that.transectButton.setVisible(true);
				} else {
					that.timeSeriesClickControl.activate();
					that.mapPanel.wmsFeatureInfo.deactivate();
					that.timeSeriesPanel.setVisible(true);
					that.timeSeriesButton.setText("Cancel Time Series");
					that.mode = "TS";
					that.transectButton.setVisible(false);
				}
			}
		});

		this.transectButton = new Ext.Button({
			text: 'Transect Plot',
			handler: function() {
				if (that.mode == "TRAN") {
					that.transectDrawControl.deactivate();
					that.mapPanel.wmsFeatureInfo.activate();
					that.transectButton.setText("Transect Plot");
					that.transectDrawControl.hideTransect();
					that.mode = "";
					that.timeSeriesButton.setVisible(true);
				} else {
					that.transectDrawControl.activate();
					that.transectDrawControl.showTransect();
					that.mapPanel.wmsFeatureInfo.deactivate();
					that.transectButton.setText("Cancel Transect");
					that.mode = "TRAN";
					that.timeSeriesButton.setVisible(false);
				}
			}
		});

		if (this.layer && !this.disabledDates) {
			// Disable all dates until the service answer which dates are available.
			this.disabledDates = ["^.*$"];
		}

		var fromDateConfig = {
			format: this.format,
			outputFormat: this.outputFormat,
			style: {
				marginBottom: '4px'
			},
			width: 100,
			disabledDatesText: this.disabledDatesText,
			disabledDates: this.disabledDates
		};
		var thruDateConfig = {
			format: this.format,
			outputFormat: this.outputFormat,
			style: {
				marginBottom: '4px'
			},
			width: 100,
			disabledDatesText: this.disabledDatesText,
			disabledDates: this.disabledDates
		};

		this.fromDateField = new Ext.ux.form.DateField(fromDateConfig);
		this.thruDateField = new Ext.ux.form.DateField(thruDateConfig);

		this.fromDateField.setValue = function(date) {
			// NOTE: "this" refer to the dateField instance

			Ext.ux.form.DateField.superclass.setValue.call(this, date);

			var serviceUrl = that.layer.json['wmsServiceUrl'];
			var dateStr = that.fromDateField.getValue();;

			var url = serviceUrl + '?' + Ext.urlEncode({
				item: 'timesteps',
				layerName: that.layer.json['layerId'],
				day: dateStr,
				request: 'GetMetadata'
			});

			/**
				Parameters
					uri         {String} URI of source doc
					params      {String} Params on get (doesnt seem to work)
					caller      {Object} object which gets callbacks
					onComplete  {Function} callback for success
					onFailure   {Function} callback for failure
				Both callbacks optional (though silly)
			*/
			OpenLayers.loadURL(
				url,
				"",
				this,
				function (result, request) {
					that._setFromDateTime(result, request, date, true);
				},
				function (result, request) {
					// TODO Error on the page
					var jsonData = Ext.util.JSON.decode(result.responseText);
					var resultMessage = jsonData.data.result;
					alert('Error while loading the times: ' + resultMessage);
				}
			);
		};

		this._setFromDateTime = function(result, request, selectedDate, setTime) {
			var jsonData = Ext.util.JSON.decode(result.responseText);
			if (jsonData['timesteps'][0]) {
				that.timeSeriesClickControl.fromDate = that.fromDateField.getValue() + 'T' + jsonData['timesteps'][0];
			} else {
				that.timeSeriesClickControl.fromDate = that.fromDateField.getValue();
			}
		};

		this.thruDateField.setValue = function(date) {
			// NOTE: "this" refer to the dateField instance
			Ext.ux.form.DateField.superclass.setValue.call(this, date);

			var serviceUrl = that.layer.json['wmsServiceUrl'];
			var dateStr = that.thruDateField.getValue();;

			var url = serviceUrl + '?' + Ext.urlEncode({
				item: 'timesteps',
				layerName: that.layer.json['layerId'],
				day: dateStr,
				request: 'GetMetadata'
			});

			/**
				Parameters
					uri         {String} URI of source doc
					params      {String} Params on get (doesnt seem to work)
					caller      {Object} object which gets callbacks
					onComplete  {Function} callback for success
					onFailure   {Function} callback for failure
				Both callbacks optional (though silly)
			*/
			OpenLayers.loadURL(
				url,
				"",
				this,
				function (result, request) {
					that._setThruDateTime(result, request, date, true);
				},
				function (result, request) {
					// TODO Error on the page
					var jsonData = Ext.util.JSON.decode(result.responseText);
					var resultMessage = jsonData.data.result;
					alert('Error while loading the times: ' + resultMessage);
				}
			);
		};

		this._setThruDateTime = function(result, request, selectedDate, setTime) {
			var jsonData = Ext.util.JSON.decode(result.responseText);
			if (jsonData['timesteps'][0]) {
				that.timeSeriesClickControl.thruDate = that.thruDateField.getValue() + 'T' + jsonData['timesteps'][0];
			} else {
				that.timeSeriesClickControl.thruDate = that.thruDateField.getValue();
			}
		};

		this.timeSeriesText = new Ext.form.Label({
			text: "Choose a from and through date above and click on the map to generate a time series plot."
		});

		this.fromDateLabel = new Ext.form.Label({text: "From Date"});
		this.thruDateLabel = new Ext.form.Label({text: "Through Date"});

		this.timeSeriesPanel = new Ext.Panel({
			layout: "auto",
			items : [
				that.fromDateLabel,
				that.fromDateField,
				that.thruDateLabel,
				that.thruDateField,
				that.timeSeriesText
			]
		});

		this.items = [
			that.timeSeriesButton,
			that.timeSeriesPanel,
			that.transectButton
		];
		Ext.ux.NCPlotPanel.superclass.initComponent.call(this);

		this.timeSeriesPanel.setVisible(false);
	},

	setLayer: function(layer) {
		this.layer = layer;
	},

	setMapPanel: function(mapPanel) {
		this.mapPanel = mapPanel;
	},

	setTime: function(time) {
		this.transectDrawControl.time=time;
	},

	setDisabledDates: function(disabledDates) {
		this.fromDateField.setDisabledDates(disabledDates);
		this.thruDateField.setDisabledDates(disabledDates);
	},

	setDefaultDate: function(defaultDate) {
		if (!this.fromDateField.getValue()) {
			this.thruDateField.setValue(defaultDate);
			this.fromDateField.setValue(defaultDate.add(Date.MONTH, -1));

			this.timeSeriesClickControl.fromDate = this.fromDateField.getValue();
			this.timeSeriesClickControl.thruDate = this.thruDateField.getValue();
		}
	},

	cleanup: function() {
		this.timeSeriesClickControl.deactivate();
		this.mapPanel.wmsFeatureInfo.activate();
		this.transectDrawControl.deactivate();
		if (this.mode == "TRAN") {
			this.transectDrawControl.hideTransect();
		}
	}
});

Ext.reg('ux-ncplotpanel', Ext.ux.NCPlotPanel);
