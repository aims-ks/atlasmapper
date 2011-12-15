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

// Inspired on: http://www.sencha.com/forum/showthread.php?4807-TextField-with-remote-validation
Ext.define('Ext.ux.form.field.AjaxText', {
	extend: 'Ext.form.field.Text',
	alias: 'widget.ajaxtextfield',

	ajax: null,
	loadingCls: 'ajaxLoading',

	// By default, only validate on blur (on change trigger a lot of requests)
	// NOTE: This setting can be change by adding "validateOnChange: true"
	//     to the field configuration.
	validateOnChange: false,

	// Override the template to add the loading element
	fieldSubTpl: [ // note: {id} here is really {inputId}, but {cmpId} is available
		'<input id="{id}" type="{type}" ',
		'<tpl if="name">name="{name}" </tpl>',
		'<tpl if="size">size="{size}" </tpl>',
		'<tpl if="tabIdx">tabIndex="{tabIdx}" </tpl>',
		'class="{fieldCls} {typeCls}" autocomplete="off" />',
		'<div id="{id}-loadingEl" class="{loadingCls}" style="display:none"></div>',
		{
			compiled: true,
			disableFormats: true
		}
	],

	// private
	_errors: null,

	constructor: function() {
		this.callParent(arguments);
		this._errors = [];

		if (this.ajax == null) { this.ajax = {}; }

		Ext.applyIf(this.ajax, {
			// url
			// params
			// formValues: ['id'], formPanel: this
			timeout: 30000,
			method: 'POST',
			responseError: 'Unable to validate the field: The response returned by the server is invalid.',
			failureError: 'Unable to validate the field: Internal server error.'
		});

		if (this.ajax.url) {
			if (this.validateOnChange) {
				this.on('change', this.ajaxValidate);
			} else {
				this.on('change', this.resetValidationState);
			}
			if (this.validateOnBlur) {
				this.on('blur', this.ajaxValidate);
			}
		}
	},

	// Get a reference to the loading graphic element (used when the validator
	// is waiting for a response)
	// override
	onRender : function() {
		this.callParent(arguments);
		this.loadingEl = Ext.get(this.inputEl.id + '-loadingEl');
	},

	// Add loading CSS class to the template
	// override
	getSubTplData: function() {
		var me = this;
		return Ext.applyIf(
			this.callParent(arguments), {
				loadingCls: me.loadingCls
			}
		);
	},

	// Return an JSON Object representing all URL parameters (Ext.Ajax can deal
	// with JSON Objects now)
	// private
	_getParams: function() {
		var fieldParameter = {};
		fieldParameter[this.name || this.id] = this.getValue();

		if (this.ajax) {
			if (this.ajax.params) {
				Ext.apply(fieldParameter, this.ajax.params);
			}

			if (this.ajax.formValues && this.ajax.formPanel) {
				var form = this.ajax.formPanel.form;
				Ext.each(this.ajax.formValues, function(formValue) {
					fieldParameter[formValue] = form.getValues()[formValue];
				});
			}
		}

		return fieldParameter;
	},

	// public
	ajaxValidate: function() {
		this.resetValidationState();

		var v = this.getValue();
		if (this.transaction) {
			this.abort();
		}

		var params = this._getParams();
		// Show the loading graphic before sending the request
		this.loadingEl.setVisible(true);
		this.transaction = Ext.Ajax.request({
			method: this.ajax.method,
			url: this.ajax.url,
			params: params,
			success: this._success,
			failure: this._failure,
			scope: this,
			timeout: this.ajax.timeout
		});
	},

	// Reset the validation state
	resetValidationState: function() {
		if (this._errors != null && this._errors.length > 0) {
			this._errors = [];
			this.clearInvalid();
		}
	},

	// public
	abort: function(){
		if (this.transaction) {
			Ext.Ajax.abort(this.transaction);
		}
	},

	// private
	_success: function(response) {
		if (this.transaction != null && this.transaction.aborted === true) {
			this.transaction = null;
			return;
		}
		this.transaction = null;
		var result = null;
		if (response.responseText) {
			try {
				result = Ext.decode(response.responseText);
			} catch(err) {
				//console.log(err);
			}
		}
		if (result != null) {
			if (!result.success) {
				// Test if "errors" is an array. NOTE: Arrays are objects
				if (typeof(result.errors) === 'object' && result.errors.join) {
					this.currentErrorTxt = result.errors.join(', ');
				} else {
					this.currentErrorTxt = '' + result.errors
				}
				this.markInvalid(this.currentErrorTxt);
				this._errors[this._errors.length] = this.currentErrorTxt;
			}
		} else {
			this.currentErrorTxt = this.ajax.responseError;
			this.markInvalid(this.currentErrorTxt);
			this._errors[this._errors.length] = this.currentErrorTxt;
		}

		// Trigger the validation process
		this.validate();
		// Hide the loading graphic
		this.loadingEl.setVisible(false);
	},

	// private
	_failure: function(response) {
		if (this.transaction != null && this.transaction.aborted === true) {
			this.transaction = null;
			return;
		}
		this.transaction = null;
		this.currentErrorTxt = this.ajax.failureError;
		this.markInvalid(this.currentErrorTxt);
		this._errors[this._errors.length] = this.currentErrorTxt;

		// Trigger the validation process
		this.validate();
		// Hide the loading graphic
		this.loadingEl.setVisible(false);
	},

	// override
	isValid: function() {
		// Do not allow submission of the form if the server didn't sent a response for this field.
		if (this.transaction != null) {
			return false;
		}
		return this.callParent(arguments);
	},

	// override
	getErrors: function() {
		var parentErrors = this.callParent(arguments);

		if (this._errors == null || this._errors.length <= 0) {
			return parentErrors;
		}

		if (parentErrors == null || parentErrors.length <= 0) {
			return this._errors;
		}

		return parentErrors.concat(this._errors);
	}
});
