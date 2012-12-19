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
// Add the additional 'advanced' validation type (VTypes)
// to validate json syntaxe.
// ExtJS implement a nice tool to decode JSON. Unfortunatly it's errors
// description are useless.

Ext.define('Frameset', {
	extend: 'Ext.container.Viewport',

	layout: 'border',
	contentPanel: null,

	waitingTitle: null,
	waitingContentItems: null,

	defaults: {
		split: true
	},

	statusBar: null,
	statusWindow: null,

	initComponent: function() {
		var contentItems = [];
		if (this.waitingContentItems) {
			contentItems = this.waitingContentItems;
			this.waitingContentItems = null;
		}

		var that = this;
		var contentPanelConfig = {
			autoScroll: true,
			collapsible: false,
			region:'center',
			bodyPadding: 10,
			items: contentItems,

			// - Items takes 100% of horizontal space by default -
			//   Without this, items takes 100% after render,
			//   but do not adjust their width on window resize.
			layout: 'anchor'
		};

		if (this.waitingTitle) {
			contentPanelConfig.title = this.waitingTitle;
			this.waitingTitle = null;
		}

		this.contentPanel = Ext.create('Ext.panel.Panel', contentPanelConfig);

		var logoutHTML = '<p style="float: right">';
		if (userName) {
			logoutHTML += 'Logged as <a href="../admin/userAccountPage.jsp">' + userName + '</a>. ';
		}
		logoutHTML += '<a href="../public/login.jsp?action=logout">[Logout]</a></p>';

		this.statusBar = Ext.create('Ext.ux.StatusBar', {
			height: 25,
			minHeight: 25,
			maxHeight: 250,
			region: 'south',
			defaultText: 'Ready'
		});

		this.items = [
			{
				html: logoutHTML +
					'<p><img src="../resources/images/AtlasMapper_logo_shadow_252x50px.png" /></p>',
				border: false,
				region: 'north',
				height: 70,
				minHeight: 50,
				maxHeight: 250,
				bodyPadding: '5',
				cmargins: '0 0 5 0'
			}, {
				html: '<ul class="bullet-list">'+
						'<li><a href="../admin/dataSourcesConfigPage.jsp">Data sources</a></li>'+
						'<li><a href="../admin/clientsConfigPage.jsp">AtlasMapper clients</a></li>'+
						'<li><a href="../admin/manualOverrideDoc.html" target="_blank">Documentation</a></li>'+
						'<li><a href="../admin/aboutPage.jsp">About</a></li>'+
					'</ul>',
				title: 'Navigation',
				region:'west',
				width: 200,
				minWidth: 100,
				maxWidth: 250,
				cmargins: '0 5 0 0'
			},
			this.contentPanel,
			this.statusBar
		];

		this.callParent(arguments);
	},

	setContentTitle: function(contentTitle) {
		if (!this.contentPanel) {
			this.waitingTitle = contentTitle;
		} else {
			this.contentPanel.setTitle(contentTitle);
		}
	},

	addContentDescription: function(contentDesc) {
		this.addContent({
			xtype: 'panel',
			margin: '0 0 15 0',
			border: false,
			html: contentDesc
		});
	},

	addContent: function(contentItem) {
		if (!this.contentPanel) {
			// Add the content to the waiting list if the panel is not ready.
			if (!this.waitingContentItems) {
				this.waitingContentItems = [];
			}
			this.waitingContentItems.push(contentItem);
		} else {
			this.contentPanel.add(contentItem);
		}
	},


	redirectIfNeeded: function(statusCode) {
		// 401 = UNAUTHORIZED
		if (statusCode == 401) {
			Ext.Msg.show({
				 title: 'Failure',
				 msg: 'Your session has timed out.<br/>Please wait...',
				 icon: Ext.Msg.WARNING
			});
			// Redirect to the login page
			window.location = '../public/admin.jsp';
			return true;
		}
		return false;
	},

	beforeShow: function() {
		if (this.statusWindow) {
			this.statusWindow.close();
			this.statusWindow = null;
		}
	},

	showBusy: function() {
		this.beforeShow();
		this.statusBar.showBusy();
	},

	/**
	 * Display the error message in the status bar and popup a window
	 * using the error message as a title and the errors from the
	 * response as content, formated using a bullet list.
	 * msg: A brief error message
	 * response: The response object contains errors
	 */
	setErrors: function(msg, response, statusCode) {
		this.beforeShow();
		if (!this.redirectIfNeeded(statusCode)) {
			var errorMessages = '';
			if (response && response.errors) {
				errorMessages = '<ul class="bullet-list">\n';
				Ext.each(response.errors, function(error) {
					errorMessages += '<li>' + error + '</li>\n';
				});
				errorMessages += '</ul>\n';
			}

			var alertMsg = '<b>' + msg + '</b>';
			if (errorMessages) {
				alertMsg += '<br/>\n' + errorMessages;
			}
			Ext.Msg.alert('Failure', alertMsg);
			this.statusBar.setStatus({
				text: msg,
				iconCls: 'x-status-error',
				clear: true // auto-clear after a set interval
			});
		}
	},
	setErrorsAndWarnings: function(title, msg, response) {
		this.beforeShow();
		var errorMessages = '';
		if (response && response.errors) {
			errorMessages += '<div><b>Errors:</b>';
			errorMessages += '<ul class="bullet-list">\n';
			if (response.errors[""]) {
				Ext.each(response.errors[""], function(error) {
					Ext.iterate(error, function(url, errorStr) {
						errorMessages += '<li>';
						if (typeof(errorStr) === 'string') {
							errorMessages += Ext.String.htmlEncode(errorStr);
						} else {
							var first = true;
							Ext.each(errorStr, function(errorElStr) {
								if (first) { first = false; } else { errorMessages += '<br/>\n';}
								errorMessages += Ext.String.htmlEncode(errorElStr);
							}, this);
						}
						if (url) {
							errorMessages += '<br/>\n' + this.urlToHtmlLink(url);
						}
						errorMessages += '</li>\n';
					}, this);
				}, this);
			}
			Ext.iterate(response.errors, function(dataSourceId, errors) {
				if (dataSourceId) {
					errorMessages += '<li>' + dataSourceId;
					errorMessages += '<ul class="bullet-list">\n';
					Ext.each(errors, function(error) {
						Ext.iterate(error, function(url, errorStr) {
							errorMessages += '<li>';
							if (typeof(errorStr) === 'string') {
								errorMessages += Ext.String.htmlEncode(errorStr);
							} else {
								var first = true;
								Ext.each(errorStr, function(errorElStr) {
									if (first) { first = false; } else { errorMessages += '<br/>\n';}
									errorMessages += Ext.String.htmlEncode(errorElStr);
								}, this);
							}
							if (url) {
								errorMessages += '<br/>\n' + this.urlToHtmlLink(url);
							}
							errorMessages += '</li>\n';
						}, this);
					}, this);
					errorMessages += '</ul>\n';
					errorMessages += '</li>\n';
				}
			}, this);
			errorMessages += '</ul></div>\n';
		}
		if (response && response.warnings) {
			errorMessages += '<div><b>Warnings:</b>';
			errorMessages += '<ul class="bullet-list">\n';
			if (response.warnings[""]) {
				Ext.each(response.warnings[""], function(warning) {
					Ext.iterate(warning, function(url, warningStr) {
						errorMessages += '<li>';
						if (typeof(warningStr) === 'string') {
							errorMessages += Ext.String.htmlEncode(warningStr);
						} else {
							var first = true;
							Ext.each(warningStr, function(warningElStr) {
								if (first) { first = false; } else { errorMessages += '<br/>\n';}
								errorMessages += Ext.String.htmlEncode(warningElStr);
							}, this);
						}
						if (url) {
							errorMessages += '<br/>\n' + this.urlToHtmlLink(url);
						}
						errorMessages += '</li>\n';
					}, this);
				}, this);
			}
			Ext.iterate(response.warnings, function(dataSourceId, warnings) {
				if (dataSourceId) {
					errorMessages += '<li>' + dataSourceId;
					errorMessages += '<ul class="bullet-list">\n';
					Ext.each(warnings, function(warning) {
						Ext.iterate(warning, function(url, warningStr) {
							errorMessages += '<li>';
							if (typeof(warningStr) === 'string') {
								errorMessages += Ext.String.htmlEncode(warningStr);
							} else {
								var first = true;
								Ext.each(warningStr, function(warningElStr) {
									if (first) { first = false; } else { errorMessages += '<br/>\n';}
									errorMessages += Ext.String.htmlEncode(warningElStr);
								}, this);
							}
							if (url) {
								errorMessages += '<br/>\n' + this.urlToHtmlLink(url);
							}
							errorMessages += '</li>\n';
						}, this);
					}, this);
					errorMessages += '</ul>\n';
					errorMessages += '</li>\n';
				}
			}, this);
			errorMessages += '</ul></div>\n';
		}

		var alertMsg = '<b>' + msg + '</b><br/>\n';
		if (errorMessages) {
			alertMsg += '<br/>\n' + errorMessages;
		}

		Ext.create('Ext.window.Window', {
			layout: 'fit',
			title: title,
			closable: true,
			resizable: true,
			plain: true,
			border: false,
			width: 550,
			height: 400,
			items: [{
				xtype: 'panel',
				autoScroll: true,
				bodyPadding: 5,
				html: alertMsg
			}],
			dockedItems: [{
				xtype: 'toolbar',
				dock: 'bottom',
				ui: 'footer',
				layout:{
					pack: 'center'
				},
				defaults: { minWidth: 75 },
				items: [
					{
						xtype: 'button',
						text: 'Close',
						padding: '2 10',
						handler: function() {
							var window = this.ownerCt.ownerCt;
							window.close();
						}
					}
				]
			}]
		}).show();

		this.statusBar.setStatus({
			text: msg,
			iconCls: 'x-status-error',
			clear: true // auto-clear after a set interval
		});
	},
	urlToHtmlLink: function(url) {
		var urlDisplay = url;

		var maxUrlLength = 60;
		if (maxUrlLength > 0 && maxUrlLength < url.length) {
			var beginningLength = Math.round((maxUrlLength-3) * 3.0/4);
			var endingLength = maxUrlLength - beginningLength - 3; // 3 is for the "..."
			if (beginningLength > 1 && endingLength == 0) {
				beginningLength--;
				endingLength = 1;
			}
			urlDisplay = url.substring(0, beginningLength) + "..." + url.substring(url.length - endingLength);
		}

		return '<a href="'+url+'">'+Ext.String.htmlEncode(urlDisplay)+'</a>';
	},

	setError: function(error, statusCode) {
		this.beforeShow();
		if (!this.redirectIfNeeded(statusCode)) {
			Ext.Msg.alert('Failure', error);
			this.statusBar.setStatus({
				text: error,
				iconCls: 'x-status-error',
				clear: true // auto-clear after a set interval
			});
		}
	},

	setValidMessage: function(status) {
		this.beforeShow();
		this.statusBar.setStatus({
			text: status,
			iconCls: 'x-status-valid',
			clear: true // auto-clear after a set interval
		});
	},

	setSavingMessage: function(msg) {
		this.beforeShow();
		this.statusWindow = Ext.Msg.show({
			title: msg,
			msg: 'Please wait...',
			closable: false,
			icon: Ext.Msg.INFO,
			closeAction: 'destroy'
		});

		this.statusBar.setStatus({
			text: msg,
			iconCls: 'x-status-saving'
		});
	},

	setSavedMessage: function(msg, delay, windowTitle) {
		this.beforeShow();

		if (typeof(windowTitle) != 'undefined') {
			this.statusWindow = Ext.Msg.show({
				title: windowTitle,
				msg: msg,
				icon: Ext.Msg.INFO,
				closeAction: 'destroy'
			});
		}

		var config = {
			text: msg,
			iconCls: 'x-status-saved',
			clear: true // auto-clear after a set interval
		};
		if (delay && delay > 0) {
			var that = this;
			Ext.defer(function() {
				that.statusBar.setStatus(config);
			}, delay);
		} else {
			this.statusBar.setStatus(config);
		}
	},

	setTextMessage: function(status) {
		this.beforeShow();
		this.statusBar.setStatus({
			text: status,
			iconCls: 'x-status-text',
			clear: true // auto-clear after a set interval
		});
	},

	clearStatus: function() {
		this.beforeShow();
		this.statusBar.clearStatus({useDefaults:true});
	}
});
