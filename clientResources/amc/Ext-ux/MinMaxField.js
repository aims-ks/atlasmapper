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
		this.maxField.on('change', onChange);

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
		values.sort();

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
		values.sort();

		// Start with an empty string to avoid mathematical addition
		return ''+values[0]+this.spacer+values[1];
	}
});

Ext.reg('ux-minmaxfield', Ext.ux.form.MinMaxField);
