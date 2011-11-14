// Fix checkboxes
// The default value is "on" which can't be used with boolean variable.
// For checkboxes which set the inputValue to a String, the model expect
// that string when requesting for the value, not a true of false.
// This widget is a bit screwed...
//
// Without the fix:
// When inputValue is not set,
//     When the checkbox is checked, the form return:
//         "on" OR true
//     When the checkbox is NOT checked, the form return:
//         null OR false
// When inputValue is set to X, the form return:
//     When the checkbox is checked, the form return:
//         "X" OR true
//     When the checkbox is NOT checked, the form return:
//         null OR false
// For a checkboxgroup, the value returned is either something like this:
//     "X", "Y", "Z" (only occur when the proxy is not set; on object creation)
// OR
//     true, false, false, true, false, true, false (totaly useless in server side AND client side)
//
// This this fix:
// When inputValue is not set, the form return:
//     When the checkbox is checked, the form return:
//         "true"
//     When the checkbox is NOT checked, the form return:
//         null
// When inputValue is set to X, the form return:
//     When the checkbox is checked, the form return:
//         "X"
//     When the checkbox is NOT checked, the form return:
//         null
// For a checkboxgroup, the value returned is ALWAYS something like this:
//     "X", "Y", "Z"

Ext.override(Ext.form.field.Checkbox, {
	// 'true' instead of 'on' for boolean variables
	inputValue: 'true',

	// Return the value set with inputValue instead of true/false
	getValue: function() {
		return this.getSubmitValue();
	},

	setValue: function(checked) {
		var me = this;
		if (Ext.isArray(checked)) {
			// getByField is defined bellow
			me.getManager().getByField(me).each(function(cb) {
				cb.setValue(Ext.Array.contains(checked, cb.inputValue));
			});
		} else {
			me.callParent(arguments);
		}

		return me;
	}
});

// Checkboxes bug:
// http://www.sencha.com/forum/showthread.php?145132-Zombie-Checkboxes-bug-%28with-fix%29
Ext.form.field.Checkbox.implement({
	destroy: function(){
		this.getManager().remove(this);
		Ext.form.field.Checkbox.superclass.destroy.call(this);
	}
});

Ext.override(Ext.form.field.Radio, {
	onChange: function(newVal, oldVal) {
		var me = this;
		me.callParent(arguments);

		if (newVal) {
			this.getManager().getByField(me).each(function(item){
				if (item !== me) {
					item.setValue(false);
				}
			}, me);
		}
	}
});

// Singleton class - can not call implement nor override
Ext.form.CheckboxManager.getByField = function(field){
	return this.filterBy(function(item) {
		if (item.name != field.name) {
			return false;
		}

		var itemGroup = item.findParentByType('checkboxgroup');
		var fieldGroup = field.findParentByType('checkboxgroup');

		if (itemGroup == null || typeof(itemGroup.getId) == 'undefined') {
			return fieldGroup == null || typeof(fieldGroup.getId) == 'undefined';
		}
		return fieldGroup != null &&
				typeof(fieldGroup.getId) != 'undefined' &&
				itemGroup.getId() == fieldGroup.getId();
	});
};

Ext.form.RadioManager.getByField = function(field){
	return this.filterBy(function(item) {
		if (item.name != field.name) {
			return false;
		}

		var itemGroup = item.findParentByType('radiogroup');
		var fieldGroup = field.findParentByType('radiogroup');

		if (itemGroup == null || typeof(itemGroup.getId) == 'undefined') {
			return fieldGroup == null || typeof(fieldGroup.getId) == 'undefined';
		}
		return fieldGroup != null &&
				typeof(fieldGroup.getId) != 'undefined' &&
				itemGroup.getId() == fieldGroup.getId();
	});
};
