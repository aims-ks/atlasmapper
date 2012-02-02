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
	dateFormat: "d/m/Y",

	// Date format used to display the times in the dropdown.
	timeFormat: "H:i:s",

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
			format: this.dateFormat,
			outputFormat: this.layer.outputFormat,
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
		};

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

		var that = this;
		this.layer.getAvailableDates(
			function(availableDates, defaultDate) {
				that.dateField.setDisabledDates(["^(?!"+availableDates.join("|")+").*$"]);

				// If there is no value set, set the default value.
				if (!that.dateField.getValue()) {
					that.setValue(defaultDate);
				}
			},
			function(errorMessage) {
				// TODO Error on page
				alert(errorMessage);
			}
		);
	},

	reloadTimes: function(date, setTime) {
		if (!this.layer) {
			// This should not append
			return;
		}

		var that = this;
		this.layer.getAvailableTimes(
			date,
			function(times) {
				var selectedTimeStr = date.format(that.timeFormat);
				var timeFound = false;
				var firstTimeStr = null;
				Ext.each(times, function(timeArr) {
					if (firstTimeStr == null) {
						firstTimeStr = timeArr[1];
					}
					if (timeArr[1] == selectedTimeStr) {
						timeFound = true;
					}
				});
				that.timeField.getStore().loadData(times);

				if (timeFound) {
					if (setTime) {
						that.timeField.setValue(selectedTimeStr);
					}
				} else if (firstTimeStr) {
					that.timeField.setValue(firstTimeStr);
				}

				if (times.length > 1) {
					that.timeField.enable();
				} else {
					that.timeField.disable();
				}
				that.fireEvent('change', arguments);
			},
			function(errorMessage) {
				// TODO Error on page
				alert(errorMessage);
			}
		);
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
					dateObj.dateFormat(this.dateFormat) + ' ' + this.timeField.getValue(),
					this.dateFormat + ' ' + this.timeFormat);
		} else {
			// This should not append
			datetime = dateObj;
		}

		// Return the full date using the output format
		return this.dateField.formatOutputDate(datetime);
	}
});

Ext.reg('ux-ncdatetimefield', Ext.ux.form.NCDatetimeField);
