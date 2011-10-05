Ext.namespace("Ext.ux");
Ext.namespace("Ext.ux.form");

/**
 * A Ext.form.DateField with a distinct format for display and for output.
 */
Ext.ux.form.DateField = Ext.extend(Ext.form.DateField,  {
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
	format: "d/m/Y",

	/**
	 * @cfg {String} altFormats
	 * Multiple date formats separated by "<tt>|</tt>" to try when parsing a user input value and it
	 * does not match the defined format (defaults to
	 * <tt>'d/m/Y|j/n/Y|j/n/y|j/m/y|d/n/y|j/m/Y|d/n/Y|d-m-y|d-m-Y|d/m|d-m|dm|dmy|dmY|d|Y-m-d|n-j|n/j'</tt>).
	 */
	altFormats: "d/m/Y|j/n/Y|j/n/y|j/m/y|d/n/y|j/m/Y|d/n/Y|d-m-y|d-m-Y|d/m|d-m|dm|dmy|dmY|d|Y-m-d|n-j|n/j",

	/**
	 * @cfg {String} format
	 * The default date format string used for output value, which can be overridden for application support. The format must be
	 * valid according to {@link Date#parseDate} (defaults to <tt>'d/m/Y'</tt>).
	 */
	outputFormat: "d/m/Y",

	initComponent: function() {
		// Add support to parse the output format
		if (this.outputFormat) {
			this.altFormats = this.outputFormat + '|' + this.altFormats;
		}

		Ext.ux.form.DateField.superclass.initComponent.call(this);
	},

	getDateObject: function() {
		return Ext.ux.form.DateField.superclass.getValue.call(this);
	},

	getValue: function() {
		return this.formatOutputDate(this.getDateObject());
	},

	/**
	 * @method onTriggerClick
	 * @hide
	 */
	// private
	// Implements the default empty TriggerField.onTriggerClick function to display the DatePicker
	onTriggerClick : function(){
		if(this.disabled){
			return;
		}
		if(this.menu == null){
			this.menu = new Ext.menu.DateMenu({
				hideOnClick: false,
				focusOnSelect: false
			});
		}
		this.onFocus();
		Ext.apply(this.menu.picker,  {
			minDate : this.minValue,
			maxDate : this.maxValue,
			disabledDatesRE : this.disabledDatesRE,
			disabledDatesText : this.disabledDatesText,
			disabledDays : this.disabledDays,
			disabledDaysText : this.disabledDaysText,
			format : this.format,
			showToday : this.showToday,
			startDay: this.startDay,
			minText : String.format(this.minText, this.formatDate(this.minValue)),
			maxText : String.format(this.maxText, this.formatDate(this.maxValue))
		});
		this.menu.picker.setValue(this.getDateObject() || new Date());
		this.menu.show(this.el, "tl-bl?");
		this.menuEvents('on');
	},

	// private
	formatOutputDate: function(date) {
		var format = this.outputFormat ? this.outputFormat : this.format;
		return Ext.isDate(date) ? date.dateFormat(format) : date;
	},

	setDisabledDates: function(dd) {
		var result = Ext.ux.form.DateField.superclass.setDisabledDates.call(this, dd);
		// Retrigger the validation to reprocess errors about invalid dates.
		this.validate();
		return result;
	},
	setDisabledDays: function(dd) {
		var result = Ext.ux.form.DateField.superclass.setDisabledDays.call(this, dd);
		// Retrigger the validation to reprocess errors about invalid days.
		this.validate();
		return result;
	}
});
Ext.reg('ux-datefield', Ext.ux.form.DateField);
