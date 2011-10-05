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
	}
});
