// Field set that trigger doLayout on the parent when it get extend/collapse.
// This is needed to resize the element width when the extend/collapse
// show/hide the scroller.
Ext.define('Ext.ux.form.FieldSetResize', {
	extend: 'Ext.form.FieldSet',
	alias: 'widget.fieldsetresize',

	parentDepth: 1,

	setExpanded: function(expanded) {
		this.callParent(arguments);
		var parentElement = this;
		for (var i=0; i<this.parentDepth; i++) {
			if (parentElement.ownerCt) {
				parentElement = parentElement.ownerCt;
			}
		}
		parentElement.doLayout();
	}
});
