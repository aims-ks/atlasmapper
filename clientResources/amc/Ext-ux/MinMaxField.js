/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2011 Australian Institute of Marine Science
 *
 *  Contact: Gael Lafond <g.lafond@aims.gov.au>
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
 * ExtJS form field with 2 NumberFields. The getValues order the fields to
 * ensure the first one has the smalest value and the second one has the
 * highest value, than return both value glue together using the value of the
 * spacer option (coma "," by default).
 */

Ext.namespace("Ext.ux");
Ext.namespace("Ext.ux.form");

Ext.ux.form.MinMaxField = Ext.extend(Ext.ux.form.CompositeFieldAnchor, {
	// String used to put between the two values - can be override
	spacer: ',',

	// private
	minValue: null,
	maxValue: null,
	minField: null,
	maxField: null,

	initComponent: function() {
		// This attribute can not be set in the class definition
		// since it define an object (otherwise the object will be
		// shared between all instances)

		var minFieldConfig = {
			minMaxField: this,
			fieldLabel: 'Min',
			anchor: '50%'
		};
		if (this.decimalPrecision) {
			minFieldConfig.decimalPrecision = this.decimalPrecision
		}
		if (this.minValue) {
			minFieldConfig.value = this.minValue
		}
		this.minField = new Ext.form.NumberField(minFieldConfig);


		var maxFieldConfig = {
			minMaxField: this,
			fieldLabel: 'Max',
			anchor: '50%'
		};
		if (this.decimalPrecision) {
			maxFieldConfig.decimalPrecision = this.decimalPrecision
		}
		if (this.maxValue) {
			maxFieldConfig.value = this.maxValue
		}
		this.maxField = new Ext.form.NumberField(maxFieldConfig);


		this.items = [this.minField, this.maxField];

		var that = this;
		function onChange() {
			that.fireEvent('change', arguments);
		}

		this.minField.on('change', onChange);
		this.minField.on('specialkey', function(field, event) {
			if (event.getKey() == event.ENTER) {
				onChange(field, event);
			}
		});

		this.maxField.on('change', onChange);
		this.maxField.on('specialkey', function(field, event) {
			if (event.getKey() == event.ENTER) {
				onChange(field, event);
			}
		});

		Ext.ux.form.MinMaxField.superclass.initComponent.call(this);
	},

	/**
	 * Call with ('min,max') OR (min, max)
	 */
	setValue: function(minValue, maxValue) {
		var values = null;
		if (typeof(maxValue) == 'undefined') {
			values = minValue.split(this.spacer);
		} else {
			values = [minValue, maxValue];
		}
		// Ensure the min value is the first one and the max is the last one
		values.sort(function(a, b) {
			// Sort as numerical value
			return parseFloat(a) - parseFloat(b);
		});

		this.minValue = values[0];
		// It's possible that the user try to set values with a string that contains more than 2 values.
		this.maxValue = values[values.length-1];

		if (this.minField) {
			this.minField.setValue(this.minValue);
		}
		if (this.maxField) {
			this.maxField.setValue(this.maxValue);
		}

		return this;
	},

	getValue: function() {
		var min = this.minField.getValue();
		var max = this.maxField.getValue();

		var minIsNull = min == null || min == '';
		var maxIsNull = max == null || max == '';

		// Injection protection
		if (isNaN(min)) { min = 0; }
		if (isNaN(max)) { max = 0; }

		// Empty values should return an empty string (or null?)
		if (minIsNull && maxIsNull) {
			// Either an empty string or null
			// NOTE: I don't know any browser that return null, but I prefer to keep the output of a textfield to keep the current browser's behaviour.
			return min;
		}

		// Ensure the values are numbers. The validate method allow empty strings.
		if (minIsNull) { min = 0; }
		if (maxIsNull) { max = 0; }

		// sort the numbers
		var values = [min, max]; //new Array(min,max);
		values.sort(function(a, b) {
			// Sort as numerical value
			return parseFloat(a) - parseFloat(b);
		});

		// Start with an empty string to avoid mathematical addition
		return ''+values[0]+this.spacer+values[1];
	}
});

Ext.reg('ux-minmaxfield', Ext.ux.form.MinMaxField);
