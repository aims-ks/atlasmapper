// This file override Fields and other similar objects
// to add the missing qtipTitle and qtipHtml attributes.

// A function that return a function
// Object can not share the same instance of a function.
// A fresh copy has to be created for each class.
function getAfterRenderFct() {
	return function() {
		// Has to be done after render to be able to access the rendered element (this.getEl())
		if(this.qtipHtml) {
			// Tooltip that stay there on mouse over the tooltip itself:
			// http://www.sencha.com/forum/showthread.php?138224-Tooltip-that-stay-active-on-mouse-over-the-tooltip-itself&p=618164
			var config = {
				target:  this.getEl(),
				// Use the label as title, or the radio/checkbox display value. Can be override using qtipTitle
				title: this.qtipTitle || this.boxLabel || this.fieldLabel,
				html: '<div class="tooltip">'+this.qtipHtml+'</div>',
				// Display the tooltip at the left of the element, if possible (put the anchor to the right side of the tooltip)
				anchor: 'right',
				// Small Tooltip doesn't looks good
				enabled: true,

				// Delay config
				showDelay: 100, // Show the tooltip fast! (default: 500)
				hideDelay: 100, // Hide the tooltip before the next one appear (default: 200)
				dismissDelay: 0 // Show the tool tip as long as the mouse stay over the element (default: 5000)
			};

			// The validity of the values (minWidth <= width <= maxWidth)
			// is done in the ToolTip object.
			if (this.qtipWidth) {
				config.width = this.qtipWidth;
			}
			if (this.qtipMinWidth) {
				config.minWidth = this.qtipMinWidth;
			}
			if (this.qtipMaxWidth) {
				config.maxWidth = this.qtipMaxWidth;
				// IE 6 & 7 do not support "maxWidth"
				if (Ext.isIE && (!Ext.ieVersion || Ext.ieVersion < 8) && !config.width) {
					config.width = config.maxWidth;
				}
			}

			Ext.create('Ext.tip.ToolTip', config);
		}
		this.callOverridden();
	};
}

// Initialise the tooltips for fields / check boxes / radio buttons / field set
Ext.override(Ext.form.Field, { afterRender: getAfterRenderFct() });
Ext.override(Ext.form.FieldContainer, { afterRender: getAfterRenderFct() });
Ext.override(Ext.form.FieldSet, { afterRender: getAfterRenderFct() });

// NOTE: Init do not need to be called (it's called by the form panel?)
// No need to call "Ext.tip.QuickTipManager.init()" or "Ext.QuickTips.init()"
