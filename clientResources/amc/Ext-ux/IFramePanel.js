Ext.namespace("Ext.ux");

Ext.ux.IFramePanel = function(config){
	var that = this;
	var defaultSrc = null;
	var defaultContent = null;
	// Content loaded in the UI
	var loadedContent = null;
	// New content, will be load in the UI when it's requested - only refresh it's if different from loadedContent
	var content = null;

	if (config.html) {
		this.defaultContent = '<div class="emptyInfo">'+config.html+'</div>';
		this.content = this.defaultContent;
	}
	if (config.src) {
		var params = config.params ? config.params : {};
		var src = Ext.urlEncode(params, config.src);
		this.defaultSrc = src;
		config.html = this.generateContent(src);
	}
	loadedContent = config.html;

	// TODO Use tpl (template)
	function generateContent(src) {
		if (!src) {
			src = defaultSrc;
		}

		var newContent = "";
		if (src) {
			newContent = '<iframe src="'+src+'" '+
					// IE rule to remove the border (IE6 do not support CSS very well)
					'frameBorder="0" '+
					'style="width:100%; height:100%; '+
						// CSS rule to remove the border
						'border:none; '+
						'margin: 0; padding: 0; '+
						// The default display has problem with the height calculation
						'display:block">'+
				'</iframe>';
		} else if (defaultContent) {
			newContent = defaultContent;
		}
		return newContent;
	}

	this.setSrc = function(rawSrc, parameters) {
		var src = null;
		if (rawSrc) {
			if (!parameters) {
				parameters = {};
			}
			src = Ext.urlEncode(parameters, rawSrc);
		}

		that.setContent(generateContent(src));
	}

	this.setContent = function(newContent) {
		content = newContent ? newContent : defaultContent;

		if (that.isVisible()) {
			that.update();
		}
	}

	this.update = function(htmlOrData, loadScripts, cb) {
		if (htmlOrData || loadScripts || cb) {
			Ext.ux.IFramePanel.superclass.update.call(that, htmlOrData, loadScripts, cb);
		} else {
			if (loadedContent != content) {
				Ext.ux.IFramePanel.superclass.update.call(that, content);
				loadedContent = content;
			}
		}
	}

	Ext.ux.IFramePanel.superclass.constructor.call(this, config);

	// ExtJS event
	this.on("activate", function(event) {this.update()});
}

Ext.extend(Ext.ux.IFramePanel, Ext.Panel, {
	setContent: function(html) {
		this.update(html);
	}
});

Ext.reg('ux_iframepanel', Ext.ux.IFramePanel);
