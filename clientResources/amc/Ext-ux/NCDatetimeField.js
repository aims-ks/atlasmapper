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

Ext.namespace("Ext.ux");
Ext.namespace("Ext.ux.form");

/**
 * @class Ext.ux.form.NCDatetimeField
 * @extends Ext.ux.form.CompositeFieldAnchor
 * Provides a date input field with a {@link Ext.DatePicker} dropdown and
 * automatic date validation, and a time input field. The allowed values are
 * determined by the NCWMS server provided by the layer.
 * @constructor
 * Create a new NCDatetimeField
 * @param {Object} config
 * @xtype ux-ncdatetimefield
 */
// http://www.sencha.com/forum/showthread.php?7385-DateField-limit-to-available-dates-from-array
Ext.ux.form.NCDatetimeField = Ext.extend(Ext.ux.form.CompositeFieldAnchor, {
	/**
	 * @cfg {String} format
	 * The default date format string used to display the date in the User Interface, which can be overridden for localisation support. The format must be
	 * valid according to {@link Date#parseDate} (defaults to <tt>'d/m/Y'</tt>).
	 */
	// NOTE: The date format m/d/Y (Middle-endian) is an illogical date
	//     format (least-significant value in the middle) almost only
	//     used in USA and is very confusing for the rest of the world.
	//     In my opinion, it should only be used for application that
	//     are only used in USA (which exclude most of Web applications).
	//     See: http://en.wikipedia.org/wiki/Date_notation_by_country

	// Date format used to display the date after selecting it from the calendar.
	format: "d/m/Y",

	// Date format used to display the times in the dropdown.
	timeFormat: "H:i:s",

	// Date format used to format the date used to request the times.
	dateRequestFormat: 'Y-m-d',

	// Date format used to parse the times returned by the request.
	// NOTE: the literal 'Z' is used to avoid using javascript timezone.
	// Example: "14:00:00.000Z"
	timeResponseFormat: 'H:i:s.u\\Z',

	// Date format used to request the ncWMS layer.
	// NOTE: the literal 'Z' is used to avoid using javascript timezone.
	// Example: "2010-01-12T14:00:00.000Z"
	outputFormat: 'Y-m-d\\TH:i:s.u\\Z',

	layer: null,

	disabledDatesText: "Layer not available for that date.",

	// private
	dateField: null,
	timeField: null,

	initComponent: function() {
		var that = this;

		if (this.layer && !this.disabledDates) {
			// Disable all dates until the service answer which dates are available.
			this.disabledDates = ["^.*$"];
		}

		var dateConfig = {
			fieldLabel: "Date",
			format: this.format,
			outputFormat: this.outputFormat,
			style: {
				marginBottom: '4px'
			},
			disabledDates: this.disabledDates,
			disabledDatesText: this.disabledDatesText
		};

		var timeConfig = {
			fieldLabel: "Time",
			format: this.timeFormat,
			store: {
				xtype: "arraystore",
				// store configs
				autoDestroy: true,
				// reader configs
				idIndex: 0,  
				data: [],
				fields: [0,1]
			}
		};

		if (typeof(this.minValue) != 'undefined') {
			dateConfig.minValue = this.minValue;
		}
		if (typeof(this.maxValue) != 'undefined') {
			dateConfig.maxValue = this.maxValue;
		}

		if (typeof(this.itemsWidth) != 'undefined') {
			if (Ext.isIE6) {
				// IE6 need smaller widgets
				this.itemsWidth = this.itemsWidth - 5;
			}
			dateConfig.width = this.itemsWidth;
			timeConfig.width = this.itemsWidth;
		} else {
			if (typeof(this.itemsAnchor) == 'undefined') {
				// IE6 can't display this widgets if its width is as large as the panel.
				this.itemsAnchor = (Ext.isIE6 ? '-5' : '100%');
			}
			dateConfig.anchor = this.itemsAnchor;
			timeConfig.anchor = this.itemsAnchor;
		}

		this.dateField = new Ext.ux.form.DateField(dateConfig);

		// Override the dateField setValue method to reload timeField values at the same time
		this.dateField.setValue = function(date) {
			// NOTE: "this" refer to the dateField instance
			var dateObj = this.parseDate(date);
			that.reloadTimes(dateObj, false);
			Ext.ux.form.DateField.superclass.setValue.call(this, date);
		}

		this.timeField = new Ext.form.TimeField(timeConfig);

		this.items = [this.dateField, this.timeField];

		function onChange() {
			that.fireEvent('change', arguments);
		}
		this.timeField.on('select', onChange);

		Ext.ux.form.NCDatetimeField.superclass.initComponent.call(this);

		this.on('render', function() {
			// Load the dates as soon as the field is ready
			that.reloadDates();
		});
	},

	// override
	destroy: function() {
		if (this.dateField != null) {
			this.dateField.destroy(); this.dateField = null;
		}
		if (this.timeField != null) {
			this.timeField.destroy(); this.timeField = null;
		}
		Ext.ux.form.NCDatetimeField.superclass.destroy.call(this);
	},

	setLayer: function(layer) {
		this.layer = layer;
		this.reloadDates();
	},

	setValue: function(date) {
		var dateObj = this.dateField.parseDate(date);
		this.reloadTimes(dateObj, true);
		this.dateField.setValue(date);
		return this;
	},

	reloadDates: function() {
		if (!this.dateField) {
			// The component has not been initialised yet
			return;
		}
		if (!this.layer) {
			// This should not append
			return;
		}

		var serviceUrl = this.layer.json['wmsServiceUrl'];

		var url = serviceUrl + '?' + Ext.urlEncode({
			item: 'layerDetails',
			layerName: this.layer.json['layerId'],
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
		var that = this;
		OpenLayers.loadURL(
			url,
			"",
			this,
			function (result, request) {
				that._setAvailableDates(result, request);
			},
			function (result, request) {
				var resultMessage = 'Unknown error';
				try {
					var jsonData = Ext.util.JSON.decode(result.responseText);
					resultMessage = jsonData.data.result;
				} catch (err) {
					resultMessage = result.responseText;
				}
				// TODO Error on the page
				alert('Error while loading the calendar: ' + resultMessage);

				that.dateField.setDisabledDates([]);
			}
		);
	},
	// private
	_setAvailableDates: function(result, request) {
		if (!this.dateField) {
			// The component has not been initialised yet
			return;
		}

		var jsonData = null;
		try {
			jsonData = Ext.util.JSON.decode(result.responseText);
		} catch (err) {
			var resultMessage = result.responseText;
			// TODO Error on the page
			alert('Error while loading the calendar: ' + resultMessage);
			return;
		}
		
		if (jsonData == null) {
			return;
		}

		if (jsonData.exception) {
			// TODO Error on the page
			alert('Error while loading the calendar: ' +
				(jsonData.exception.message ? jsonData.exception.message : jsonData.exception));
			return;
		}

		var dateArray = [];

		if (jsonData['datesWithData']) {
			Ext.iterate(jsonData.datesWithData, function(year, months) {
				Ext.iterate(months, function(month, days) {
					Ext.each(days, function(day) {
						// Create a date object to format it in the desire format.

						// Month is from 0-11 instead of 1-12.
						var monthInt = (parseInt(month, 10)) + 1;

						// Y-m-d, without leading zeros => Y-n-j
						var date = Date.parseDate(year+'-'+monthInt+'-'+day, 'Y-n-j');

						// Available dates format must match the display date format.
						dateArray.push(date.format(this.format));
					}, this);
				}, this);
			}, this);
		}

		this.dateField.setDisabledDates(["^(?!"+dateArray.join("|")+").*$"]);

		// If there is no value set, set the default value.
		if (!this.dateField.getValue()) {
			var defaultDate = jsonData['nearestTimeIso'];
			this.setValue(defaultDate);
		}
	},

	reloadTimes: function(date, setTime) {
		if (!this.layer) {
			// This should not append
			return;
		}

		var serviceUrl = this.layer.json['wmsServiceUrl'];
		var dateStr = date.format(this.dateRequestFormat).trim();

		var url = serviceUrl + '?' + Ext.urlEncode({
			item: 'timesteps',
			layerName: this.layer.json['layerId'],
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
		var that = this;
		OpenLayers.loadURL(
			url,
			"",
			this,
			function (result, request) {
				that._setAvailableTimes(result, request, date, setTime);
			},
			function (result, request) {
				var resultMessage = 'Unknown error';
				try {
					var jsonData = Ext.util.JSON.decode(result.responseText);
					resultMessage = jsonData.data.result;
				} catch (err) {
					resultMessage = result.responseText;
				}
				// TODO Error on the page
				alert('Error while loading the times: ' + resultMessage);

				that.dateField.setDisabledDates([]);
			}
		);
	},
	// private
	_setAvailableTimes: function(result, request, selectedTime, setTime) {
		if (!this.timeField || !this.timeField.getStore()) {
			// The component has not been initialised yet
			return;
		}

		var jsonData = null;
		try {
			jsonData = Ext.util.JSON.decode(result.responseText);
		} catch (err) {
			var resultMessage = result.responseText;
			// TODO Error on the page
			alert('Error while loading the times: ' + resultMessage);
			return;
		}
		
		if (jsonData == null) {
			return;
		}

		if (jsonData.exception) {
			// TODO Error on the page
			alert('Error while loading the times: ' +
				(jsonData.exception.message ? jsonData.exception.message : jsonData.exception));
			return;
		}

		var timesArray = [];

		var selectedTimeStr = selectedTime.format(this.timeFormat);
		var timeFound = false;
		var firstTimeStr = null;

		if (jsonData['timesteps']) {
			Ext.each(jsonData['timesteps'], function(time) {
				var timeObj = Date.parseDate(time, this.timeResponseFormat);
				var timeStr = timeObj.format(this.timeFormat);
				if (firstTimeStr == null) {
					firstTimeStr = timeStr;
				}
				if (timeStr == selectedTimeStr) {
					timeFound = true;
				}
				timesArray.push([time, timeStr]);
			}, this);
		}
		this.timeField.getStore().loadData(timesArray);

		if (timeFound) {
			if (setTime) {
				this.timeField.setValue(selectedTimeStr);
			}
		} else if (firstTimeStr) {
			this.timeField.setValue(firstTimeStr);
		}

		if (jsonData['timesteps'].length > 1) {
			//this.timeField.show();
			this.timeField.enable();
		} else {
			//this.timeField.hide();
			this.timeField.disable();
		}
		this.fireEvent('change', arguments);
	},

	getValue: function() {
		if (!this.dateField) {
			// The component has not been initialised yet
			return null;
		}

		var dateObj = this.dateField.getDateObject();
		if (!dateObj) {
			return null;
		}

		// Create a date using date string and time string, and parse it using a format build with date format and time format.
		var datetime = null;
		if (this.timeField) {
			datetime = Date.parseDate(
					dateObj.dateFormat(this.format) + ' ' + this.timeField.getValue(),
					this.format + ' ' + this.timeFormat);
		} else {
			// This should not append
			datetime = dateObj;
		}

		// Return the full date using the output format
		return this.dateField.formatOutputDate(datetime);
	}
});

Ext.reg('ux-ncdatetimefield', Ext.ux.form.NCDatetimeField);
