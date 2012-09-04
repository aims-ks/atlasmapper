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
 * Panel that contains an IFrame (set using setSrc) or HTML content
 * (set using setContent).
 */
Ext.namespace("Ext.ux");

Ext.ux.IFramePanel = function(config) {
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
		} else if (this.defaultContent) {
			newContent = this.defaultContent;
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
	};

	this.setContent = function(newContent) {
		content = newContent ? newContent : this.defaultContent;

		if (that.isVisible()) {
			that.update();
		}
	};

	this.update = function(htmlOrData, loadScripts, cb) {
		if (htmlOrData || loadScripts || cb) {
			Ext.ux.IFramePanel.superclass.update.call(that, htmlOrData, loadScripts, cb);
		} else {
			if (loadedContent != content) {
				// Free some memory (manually remove DOM elements and event listeners)
				// and FIX Internet Explorer bug re-using pre-rendered ExtJS elements
				try {
					// The DOM method "hasChildNodes" may not be available on all browsers
					while (this.body.dom.hasChildNodes()) {
						try {
							// The ExtJS method "removeAllListeners" crash sometime on IE
							// This DOM attribute "lastChild" may not be available on all browsers
							Ext.fly(this.body.dom.lastChild).removeAllListeners();
						} catch(e) {}
						try {
							// This DOM method "removeChild" or attribute "lastChild" may not be available on all browsers
							this.body.dom.removeChild(this.body.dom.lastChild);
						} catch(e) {}
					}
				} catch(e) {}

				if (content &&
						typeof(content) === 'object' &&
						content instanceof Ext.Element &&
						typeof(content.appendTo) === 'function') {
					// Content is a Ext.Element
					// IMPORTANT: The Ext.Element must be create properly.
					//     Example:
					//         content = new Ext.Element(Ext.DomHelper.createDom({
					//             tag: 'div',
					//             children: [{
					//                 tag: 'div',
					//                 html: 'Content'
					//             }, {
					//                 tag: 'div',
					//                 html: 'More content'
					//             }],
					//             cls: 'new-div-cls',
					//             id: 'new-div-id'
					//         }));
					//
					//         Available attributes: [tag, children OR cn, html, function, style, cls, htmlFor]
					//             Anything else will be apply strait to the element.

					// Clear content
					Ext.ux.IFramePanel.superclass.update.call(that, null);
					// Add the Ext.Element to the panel body 
					content.appendTo(this.body);
				} else {
					// Content is a HTML String
					Ext.ux.IFramePanel.superclass.update.call(that, content);
				}
				loadedContent = content;
			}
		}
	};

	Ext.ux.IFramePanel.superclass.constructor.call(this, config);

	// ExtJS event
	this.on("activate", function(event) {this.update()});
};

Ext.extend(Ext.ux.IFramePanel, Ext.Panel, {
	setContent: function(html) {
		this.update(html);
	}
});

Ext.reg('ux_iframepanel', Ext.ux.IFramePanel);
