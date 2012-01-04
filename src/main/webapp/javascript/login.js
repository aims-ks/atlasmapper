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
/*
http://www.sencha.com/learn/Tutorial:Basic_Login

Username: [    ]
Password: [    ]
[Login]
*/
Ext.onReady(function() {
	Ext.QuickTips.init();

	var submit = function() {
		var form = login.getForm();
		if (form.isValid()) {
			login.getForm().submit({
				method: 'POST',
				waitTitle: 'Connecting',
				waitMsg: 'Sending data...',

				// Functions that fire (success or failure) when the server responds.
				// The one that executes is determined by the
				// response that comes from login.asp as seen below. The server would
				// actually respond with valid JSON,
				// something like: response.write "{ success: true}" or
				// response.write "{ success: false, errors: { reason: 'Login failed. Try again.' }}"
				// depending on the logic contained within your server script.
				// If a success occurs, the user is notified with an alert messagebox,
				// and when they click "OK", they are redirected to whatever page
				// you define as redirect.

				success: function() {
					Ext.create('Ext.window.Window', {
						modal: true,
						width: 200,
						title: 'Loading...',
						html: 'Please wait...',
						bodyPadding: 15
					}).show();

					var redirect = '../admin';
					window.location = redirect;
				},

				// Failure function, see comment above re: success and failure.
				// You can see here, if login fails, it throws a messagebox
				// at the user telling him / her as much.

				failure: function (form, action) {
					if (action.failureType == 'server') {
						obj = Ext.JSON.decode(action.response.responseText);
						Ext.Msg.alert('Login Failed!', obj.errors.reason);
					} else if (action.response && action.response.responseText) {
						Ext.Msg.alert('Warning!', 'Authentication server is unreachable: ' + action.response.responseText);
					} else {
						Ext.Msg.alert('Warning!', 'Authentication server is unreachable.');
					}
					login.getForm().reset();
				}
			});
		}
	}

	var submitEnterKey = function(field, e) {
		if (e.getKey() == e.ENTER) {
			submit();
		}
	}

	// Create a variable to hold our EXT Form Panel.
	// Assign various config options as seen.
	// NOTE: login.jsp is a virtual file, pointing to the servlet:
	//    au.gov.aims.atlasmapperserver.servlet.login.LoginServlet
	var login = Ext.create('Ext.form.Panel', {
		labelWidth: 80,
		url: 'login.jsp?action=login',
		frame: true,
		defaultType: 'textfield',
		// Specific attributes for the text fields for username / password.
		// The "name" attribute defines the name of variables sent to the server.
		items: [
			{
				fieldLabel: 'Username',
				name: 'loginUsername',
				allowBlank: false,
				// submit using Enter key
				listeners: { specialkey: submitEnterKey }
			}, {
				fieldLabel: 'Password',
				name: 'loginPassword',
				inputType: 'password',
				allowBlank: false,
				// submit using Enter key
				listeners: { specialkey: submitEnterKey }
			}
		],

		// The button state change from disabled to enabled when the form is valid.
		// This feature only works for buttons in dockedItems
		dockedItems: [{
			xtype: 'toolbar',
			dock: 'bottom',
			ui: 'footer',
			defaults: { minWidth: 75 },
			items: [
				'->', // Pseudo item to move the following items to the right (available with ui:footer)
				{
					xtype: 'button',
					text: 'Help',
					padding: '2 10',
					handler: function() {
						Ext.create('Ext.window.Window', {
							modal: true,
							title: 'Help',
							width: 300,
							html: 'If you forgot your password, you can delete the file <i>users.json</i> on the server to reset the default account.',
							bodyPadding: 15,
							buttons: [
								{
									text: 'Ok',
									handler: function() {
										this.ownerCt.ownerCt.close();
									}
								}
							]
						}).show();
					}
				}, {
					xtype: 'button',
					text: 'Login',
					padding: '2 10',
					formBind: true, // only enabled once the form is valid
					disabled: true,
					// Function that fires when user clicks the button
					handler: submit
				}
			]
		}]
	});

	// This just creates a window to wrap the login form.
	// The login object is passed to the items collection.
	var winConfig = {
		layout:'fit',
		title: 'Please Login',
		closable: false,
		resizable: false,
		plain: true,
		border: false,
		items: [login]
	};

	// IE is awful with width calculation. Better give it a safe value.
	if (Ext.isIE && (!Ext.ieVersion || Ext.ieVersion < 8)) {
		winConfig.width = 300;
	}

	var win = Ext.create('Ext.window.Window', winConfig);
	win.show();
});
