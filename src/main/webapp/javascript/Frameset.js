// Add the additional 'advanced' validation type (VTypes)
// to validate json syntaxe.
// ExtJS implement a nice tool to decode JSON. Unfortunatly it's errors
// description are useless.
Ext.apply(Ext.form.field.VTypes, {
	jsonfield: function(val, field) {
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

// Checkboxes bug:
// http://www.sencha.com/forum/showthread.php?145132-Zombie-Checkboxes-bug-%28with-fix%29
Ext.form.field.Checkbox.implement({
	destroy: function(){
		this.getManager().remove(this);
		Ext.form.field.Checkbox.superclass.destroy.call(this);
	}
});
Ext.override(Ext.form.field.Checkbox, {
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

		var logoutHTML = '<p style="float: right">'
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
						//'<li><a href="../admin/globalConfigPage.jsp">Global configuration</a></li>'+
						'<li><a href="../admin/datasourcesConfigPage.jsp">Datasources</a></li>'+
						'<li><a href="../admin/clientsConfigPage.jsp">AtlasMapper clients</a></li>'+
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
			 icon: Ext.Msg.INFO,
			 closeAction: 'destroy'
		});

		this.statusBar.setStatus({
			text: msg,
			iconCls: 'x-status-saving'
		});
	},

	setSavedMessage: function(msg, delay) {
		this.beforeShow();
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
