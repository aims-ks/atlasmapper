// Add the additional 'advanced' validation type (VTypes)
// to validate json syntaxe.
// ExtJS implement a nice tool to decode JSON. Unfortunatly it's errors
// description are useless.
Ext.apply(Ext.form.field.VTypes, {
	password: function(val, field) {
		if (field.initialPassField) {
			var pwd = field.up('form').down('#' + field.initialPassField);
			return (val == pwd.getValue());
		}
		return true;
	},

	passwordText: 'Passwords do not match'
});

Ext.onReady(function() {
	var frameset = new Frameset();
	frameset.setContentTitle('User account');
	frameset.addContentDescription('This page allow you to configure the information about the administrator user of this application. If the account is still using the default password, it would be a good idea to change it now. The password is store in the configuration file is encrypted, so there is no way to get it back. If you forgot your password, you can delete the <i>users.conf</i> file to reset the account to its default values.<br/><b>Note:</b> The application do not currently allow you to create new account.');
	frameset.render(document.body);

	var userAccountForm = Ext.create('Ext.form.Panel', {
		url: 'userAccount.jsp?action=update',
		bodyStyle: 'padding:5px 5px 0',
		width: 500,
		border: false,
		fieldDefaults: {
			msgTarget: 'side',
			labelAlign: 'right',
			labelWidth: 150
		},
		defaultType: 'textfield',
		defaults: {
			qtipMaxWidth: 200,
			anchor: '100%'
		},

		items: [
			{
				fieldLabel: 'User login',
				xtype: 'displayfield',
				value: userLogin
			}, {
				fieldLabel: 'First name',
				qtipHtml: 'First name of the user. This information is only used to give you a pretty welcome header.',
				name: 'firstName'
			}, {
				fieldLabel: 'Last name',
				qtipHtml: 'Last name of the user. This information is only used to give you a pretty welcome header.',
				name: 'lastName'
			}, {
				title: 'Change password',
				xtype:'fieldset',
				defaultType: 'textfield',
				collapsible: false,
				defaults: {
					qtipMaxWidth: 200,
					anchor: '100%'
				},
				items: [
					{
						fieldLabel: 'Current Password',
						qtipHtml: 'Current password used to log to the AtlasMapper server (this application).',
						inputType:'password',
						name: 'currentPassword'
					}, {
						fieldLabel: 'New Password',
						qtipHtml: 'New password that will be use to log to the AtlasMapper server (this application).',
						inputType:'password',
						name: 'password',
						id: 'password'
					}, {
						fieldLabel: 'Confirm Password',
						qtipHtml: 'Repeat the new password.',
						name: 'passwordConfirm',
						inputType:'password',
						vtype: 'password',
						initialPassField: 'password' // id of the initial password field
					}
				]
			}
		],

		buttons: [
			{
				text: 'Save',
				handler: function() {
					frameset.setSavingMessage('Saving...');
					userAccountForm.getForm().submit({
						success: function (form, action) {
							frameset.setSavedMessage('Configuration saved');
							// Reload the page to refresh the header (and reset the form)
							location.reload(true);
						},
						failure: function (form, action) {
							var responseObj = null;
							var statusCode = null;
							if (action && action.response) {
								responseObj = Ext.JSON.decode(action.response.responseText);
								statusCode = action.response.status;
							}
							frameset.setErrors('The user information could not be saved.', responseObj, statusCode);
						}
					});
				}
			}
		]
	});


	// ** Load data using Ajax **

	// http://www.sencha.com/forum/showthread.php?35014-ExtJS-2.1-and-Java-Demo-ExtJS-Ajax-Communication-Best-Practices
	frameset.showBusy();
	userAccountForm.form.load({
		// The form automatically load the data into the form when the response is successful.
		url: 'userAccount.jsp?action=read',
		waitMsg: 'Loading',
		// The function to call when the response from the server was a failed
		// attempt (load in this case), or when an error occurred in the Ajax
		// communication.
		failure: loadFailed,
		// The function to call when the response from the server was a successful
		// attempt (load in this case).
		success: loadSuccessful
	});

	function loadSuccessful(form, action) {
		frameset.clearStatus();
	}

	function loadFailed(form, action) {
		var failureMessage = "An error occurred while trying to retrieve the data.";

		// Failure type returned when a communication error happens when
		// attempting to send a request to the remote server.
		if (action.failureType == Ext.form.Action.CONNECT_FAILURE) {

			// The XMLHttpRequest object containing the
			// response data. See http://www.w3.org/TR/XMLHttpRequest/ for
			// details about accessing elements of the response.
			failureMessage = "Please contact support with the following:<br/>" +
				"Error (" + action.response.status + "): " +
				action.response.statusText;

			frameset.setError(failureMessage, action.response.status);
		} else {
			var responseObj = null;
			var statusCode = null;
			if (action && action.response) {
				responseObj = Ext.JSON.decode(action.response.responseText);
				statusCode = action.response.status;
			}
			frameset.setErrors(failureMessage, responseObj, statusCode);
		}
	}

	frameset.addContent(userAccountForm);
});
