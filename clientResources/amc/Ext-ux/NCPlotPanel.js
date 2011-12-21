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
	
	// private
	_timeSeriesEnabled: false,

	initComponent: function() {
		var that = this;

		// Possible events: loadstart, tileloaded, loadend
		if (that.layer.getCurrentTime) {
			this.layer.events.register("loadstart", this.layer, function() {
				that.transectDrawControl.time = that.layer.getCurrentTime();
			});
		}

		if (that.layer.getAvailableDates) {
			this.on('render', function() {
				that.layer.getAvailableDates(
					function(availableDates, defaultDate) {
						if (availableDates != null && availableDates.length > 0) {
							that._timeSeriesEnabled = true;
							that.timeSeriesButton.setVisible(true);
							that.setDisabledDates(["^(?!"+availableDates.join("|")+").*$"]);
						}
						that.setDefaultDate(Date.parseDate(defaultDate, that.layer.outputFormat));
					},
					function(errorMessage) {
						// TODO Error on page
						alert(errorMessage);
					}
				);
			});
		}

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
			// Hidden the time series button on start, switch the
			// visibility when there is dates for it
			hidden: !this._timeSeriesEnabled,
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
					if (that._timeSeriesEnabled) {
						that.timeSeriesButton.setVisible(true);
					}
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
			if (date != null) {
				if (typeof(that.layer.getAvailableTimes) === 'function') {
					that.layer.getAvailableTimes(
						date,
						that._setFromDateTime,
						function(errorMessage) {
							// TODO Error on the page
							alert('Error while loading the times: ' + errorMessage);
						},
						that
					);
				}
			}
		};

		this.thruDateField.setValue = function(date) {
			// NOTE: "this" refer to the dateField instance
			Ext.ux.form.DateField.superclass.setValue.call(this, date);
			if (date != null) {
				if (typeof(that.layer.getAvailableTimes) === 'function') {
					that.layer.getAvailableTimes(
						date,
						that._setThruDateTime,
						function(errorMessage) {
							// TODO Error on the page
							alert('Error while loading the times: ' + errorMessage);
						},
						that
					);
				}
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

	// private
	_setFromDateTime: function(availableTimes) {
		if (availableTimes !== null && availableTimes.length > 0) {
			this.timeSeriesClickControl.fromDate = this.fromDateField.getValue() + 'T' + availableTimes[0][0];
		} else {
			this.timeSeriesClickControl.fromDate = this.fromDateField.getValue();
		}
	},

	// private
	_setThruDateTime: function(availableTimes) {
		if (availableTimes !== null && availableTimes.length > 0) {
			this.timeSeriesClickControl.thruDate = this.thruDateField.getValue() + 'T' + availableTimes[0][0];
		} else {
			this.timeSeriesClickControl.thruDate = this.thruDateField.getValue();
		}
	},

	setLayer: function(layer) {
		this.layer = layer;
	},

	setMapPanel: function(mapPanel) {
		this.mapPanel = mapPanel;
	},

	setDisabledDates: function(disabledDates) {
		this.fromDateField.setDisabledDates(disabledDates);
		this.thruDateField.setDisabledDates(disabledDates);
	},

	setDefaultDate: function(defaultDate) {
		if (typeof(defaultDate) === 'undefined' || defaultDate === null || defaultDate === '') {
			return;
		}

		if (typeof(this.transectDrawControl.time) === 'undefined' || this.transectDrawControl.time === null || this.transectDrawControl.time === '') {
			this.transectDrawControl.time = defaultDate.format(this.layer.outputFormat);
		}

		var thruDate = this.thruDateField.getValue();
		if (typeof(thruDate) === 'undefined' || thruDate === null || thruDate === '') {
			this.thruDateField.setValue(defaultDate);
			this.timeSeriesClickControl.thruDate = thruDate;
		}

		var fromDate = this.fromDateField.getValue();
		if (typeof(fromDate) === 'undefined' || fromDate === null || fromDate === '') {
			this.fromDateField.setValue(defaultDate.add(Date.MONTH, -1));
			this.timeSeriesClickControl.fromDate = fromDate;
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
