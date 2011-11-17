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
 * @class Ext.ux.form.field.EditArea
 * @extends Ext.form.field.TextArea

Form field that integrate the EditArea editor as a ExtJS form field.
It extends TextArea and should have all the feature of this widget.
The class handle element resize, validator and bridge the value between
the two widgets.
@cfg {String} syntax Syntax highlight. Default: 'js'. Possible values:
basic, brainfuck, c, coldfusion, cpp, css, html, java, js, pas, perl, php,
python, robotstxt, ruby, sql, tsql, vb, xml.
Check the files in edit_area/reg_syntax for all available syntaxes.
@cfg {int} fontSize Font size, in point, of the font used in EditArea.
Default: 8. Suggested values: 8, 9, 10, 11, 12, 14.
@cfg {String} valueType Type of the output value.
Default: 'string'. Possible values: 'string', 'json'.
@cfg {String/number} indent Indentation used when outputing as JSON.
Use a number to specify the number of space to use.
Default: '\t'. Possible values: '\t', 1, 2, ...

Example usage:

	Ext.create('Ext.form.FormPanel', {
		title      : 'Sample EditArea',
		width      : 400,
		bodyPadding: 10,
		renderTo   : Ext.getBody(),
		items: [{
			xtype     : 'editareafield',
			grow      : true,
			name      : 'message',
			fieldLabel: 'Message',
			anchor    : '100%',
			syntax    : 'xml'
		}]
	});

 * @docauthor Gael Lafond <g.lafond@aims.gov.au>
 */

// Validation type usually used with EditArea
Ext.apply(Ext.form.field.VTypes, {
	jsonfield: function(val, field) {
		if (val == null || val == '') {
			// The form will return an error for this field if it doesn't allow null value.
			return true;
		}

		var json = null;
		if (typeof(val) == 'object') {
			json = val
		} else {
			try {
				json = Ext.JSON.decode(val);
			} catch(error) {
				return false;
			}
		}

		if (!json) {
			return false;
		}
		return true;
	},

	jsonfieldText: 'Invalid JSON syntax. See the documentation for more info.'
});

Ext.define('Ext.ux.form.field.EditArea', {
	extend:'Ext.form.field.TextArea',
	alias: ['widget.editareafield'],

	// Add padding / margin for the checkbox at the bottom (it's set with a position absolute)
	padding: '0 0 25 0',
	margin: '0 0 25 0',

	syntax: null,
	fontSize: null,
	valueType: 'string',
	indent: '\t',
	loading: false, // Set to true to show a loading window at load

	//private
	isLoaded: false,
	displayLoading: false,

	// This template is similar to the TextArea, but make a distinction
	// between the input container and the input field.
	// This distinction is needed to have a input field with width 100%
	// instead of a width in pixel. The EditArea plugin need this.
	fieldSubTpl: [
		'<div class="{fieldCls}">',
			'<textarea id="{id}" ',
				'<tpl if="name">name="{name}" </tpl>',
				'<tpl if="rows">rows="{rows}" </tpl>',
				'<tpl if="cols">cols="{cols}" </tpl>',
				'<tpl if="tabIdx">tabIndex="{tabIdx}" </tpl>',
				'class="{typeCls}" ',
				'autocomplete="off" style="width: 100%; height: 100%">',
			'</textarea>',
		'</div>',
		{
			compiled: true,
			disableFormats: true
		}
	],

	// Add a renderSelector to specified the element to use for the input value.
	// It would be nicer to add a selector to everride the input container,
	// but that container is used everywhere.
	// this.inputEl => input container
	// this.valueEl => input value
	onRender : function() {
		var me = this;
		me.isLoaded = false;

		// This variable is an attribute of the class, but it's initialised later.
		var typeCls = Ext.baseCSSPrefix + 'form-' + (me.inputType === 'password' ? 'text' : me.inputType)
		Ext.applyIf(me.renderSelectors, {
			valueEl: '.' + typeCls
		});
		me.callParent(arguments);

		// The selectors should be initialised now
		if (!me.valueEl) {
			throw 'EditArea value element could not be initialised.';
		}
		var valueId = me.valueEl.id;
		var fctId = valueId.replace(/[^a-zA-Z0-9_]/,'');

		if (!me.syntax) {
			me.syntax = 'js';
		}
		if (!me.fontSize) {
			me.fontSize = 8;
		}

		// Definition of the callback needed to call the validator when
		// the content of EditArea is changed.
		parent['changeCallback_'+fctId] = function(id) {
			// Edit Area do not have a validator. Change to this widget
			// are sent to the text area to trigger the ExtJS validator,
			// and also set the value that will be returned by the form.
			me.valueEl.dom.value = me.getValue();
			me.validateValue(me.getValue());
		}

		parent['loadCallback_'+fctId] = function() {
			// NOTE: The event is called slightly before
			// everything is ready.
			window.setTimeout(function() {
				me.isLoaded = true;
			}, 1);
		}

		// See: http://www.cdolivet.com/editarea/editarea/docs/configuration.html
		var editAreaConfig = {
			id: valueId,            // Textarea id
			syntax: me.syntax,      // Syntax to be uses for highlighting
			start_highlight: true,  // To display with highlight mode on start-up
			allow_resize: 'no',     // It's better to let ExtJS manage the resizing
			font_size: me.fontSize,
			// EditArea do not use function pointer for some reason...
			change_callback: 'changeCallback_'+fctId,
			EA_load_callback: 'loadCallback_'+fctId,
			word_wrap: true,        // Solve most of the ghost problem in Chrome
			toolbar: "search, go_to_line, |, undo, redo, |, select_font, |, highlight, reset_highlight, word_wrap, |, help" // Remove fullscreen and smooth selection
		};

		if (this.readOnly) {
			editAreaConfig.is_editable = false;
		}

		// Init the EditArea widget
		// http://www.cdolivet.com/editarea/?page=editArea
		editAreaLoader.init(editAreaConfig);

		if (this.loading) {
			this.setLoading(true);
		}
	},

	// JSON Object or String => String
	valueToRaw: function(value) {
		if (this.valueType && this.valueType == 'json' && typeof(value) == 'object') {
			if (value == '') { return value; }
			if (typeof(JSON) != 'undefined' && this.indent) {
				// Not suitable for IE 6 and IE 7.
				return JSON.stringify(value, null, this.indent);
			} else {
				// Fall back to ExtJS, which as not indentation option.
				return Ext.JSON.encode(value);
			}
		}
		return this.callParent(arguments);
	},

	// String => JSON Object or String
	rawToValue: function(rawValue) {
		if (this.valueType && this.valueType == 'json' && typeof(rawValue) == 'string') {
			if (rawValue == '') { return rawValue; }
			try {
				return Ext.JSON.decode(rawValue);
			} catch(error) {
				return rawValue;
			}
		}
		return this.callParent(arguments);
	},

	// Override the methods that read/write the input value.
	getRawValue: function() {
		// v = EditArea value OR input value OR this.rawValue OR empty String.
		// It should always be EditArea value, even when the EditArea is off
		// (it return the value of the TextArea). The other values are there
		// for safety.
		var me = this, v = null;

		if (me.valueEl) {
			v = editAreaLoader.getValue(me.valueEl.id);
			if (v == null) {
				v = me.valueEl.getValue();
			}
		} else {
			v = Ext.value(me.rawValue, '');
		}

		me.rawValue = v;

		return v;
	},

	setRawValue: function(value) {
		var me = this;
		value = Ext.valueFrom(value, '');

		me.rawValue = value;

		// me.valueEl do not exists at load time (this operation do not need to be done at load time anyway)
		if (me.valueEl) {
			// Change the value of the textarea
			me.valueEl.dom.value = value;

			// Notify EditArea of the change, if it's initialised
			var id = me.valueEl.id;

			// Set the value to the EditArea, if the change do not already come from the EditArea.
			try {
				editAreaLoader.setValue(id, value);

				// NOTE: This command is executed only if the EditArea is ready.
				// No resync are needed when the widget is not loaded, it will
				// automatically sync itself with the textarea on load.
				// Those commands check if the EditArea is ready before
				// executing anything. It works most of the time, but,
				// with IE (again...), it always get called even when the
				// widget is not ready, which stop the execution. For some
				// reason, IE also ignore any try/catch in here.
				// That nice and clean "if" get around this bug.
				if (me.isLoaded) {
					// Reprocess the highlight to avoid Ghost from the
					// previous value.
					// NOTE: Forcing to highlight is usually enough,
					// but in some case (especially when multiple
					// instances of EditArea are present), a toggle off/on
					// is needed.
					editAreaLoader.toggle_off(id);
					editAreaLoader.toggle_on(id);
					// IE need both commands to be trigger
					// (toggle off/on and re-highlight)
					editAreaLoader.execCommand(id, 'resync_highlight', true);
				}
			} catch(err) {
				// Limitation in EditArea. Its value can not be set
				// when it's hidden.
				// (calling editAreaLoader.setValue generate an exception)
				// NOTE: show/hide event can not be used to defer the execution
				// of setRawValue since they are not fired with fieldset
				// collapse/expand and maybe other similar widget.
				// TODO Throw a nice understandable exception
				alert('ERROR: Trying to set the value of an hidden EditArea.');
			}
		}

		return value;
	},

	setLoading: function(load) {
		var me = this;

		if (load !== false && !me.collapsed && me.rendered) {
			me.displayLoading = true;
			if (me.isLoaded) {
				editAreaLoader.execCommand(me.valueEl.id, 'show_waiting_screen', false);
			} else {
				var fctId = me.valueEl.id.replace(/[^a-zA-Z0-9_]/,'');
				// Creating a new function because EditArea callback do
				// not allow class methods nor functions with parameters.
				// The name of the function is using the element ID,
				// after removing the characters that may cause problem
				// in a function name.
				//
				// NOTE: In some brownser, the event is called slightly
				// before everything is ready.
				parent['displayLoading_'+fctId] = function() {
					window.setTimeout(function() {
						me.isLoaded = true;
						me.setLoading(me.displayLoading);
					}, 1);
				}

				// Defer the execution of setLoading when the editor is loaded.
				// This function override the actual callback (EditArea
				// do not handle multiple callback per event) so the
				// function has to redefine what the previous one
				// was defining.
				editAreas[me.valueEl.id]['settings']["EA_load_callback"] = 'displayLoading_'+fctId
			}
		} else {
			me.displayLoading = false;
			// NOTE: Again, IE manage to execute this when it should not...
			// That IF get around this bug.
			if (me.isLoaded) {
				editAreaLoader.execCommand(me.valueEl.id, 'hide_waiting_screen', false);
			}
		}
	},

	// The following methods act as a wrapper to also send the command to the
	// EditArea widget.
	setReadOnly: function(readOnly) {
		this.valueEl.dom.readOnly = readOnly;
		editAreaLoader.execCommand(this.valueEl.id, 'set_editable', !readOnly);
		return this.callParent(arguments);
	},

	show: function() {
		if (this.valueEl) {
			editAreaLoader.show(this.valueEl.id);
		}
		return this.callParent(arguments);
	},

	hide: function() {
		if (this.valueEl) {
			editAreaLoader.hide(this.valueEl.id);
		}
		return this.callParent(arguments);
	},

	destroy: function() {
		if (this.valueEl) {
			editAreaLoader.delete_instance(this.valueEl.id);
		}
		return this.callParent(arguments);
	}
});
