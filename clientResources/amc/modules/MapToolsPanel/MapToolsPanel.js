/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2012 Australian Institute of Marine Science
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

Ext.namespace("Atlas");

Atlas.MapToolsPanel = Ext.extend(Ext.form.FormPanel, {
	DRUPAL_MAX_URL_LENGTH: 256,
	BROWSER_MAX_URL_LENGTH: 2000,

	mapPanel: null,

	constructor: function(attributes) {
		var that = this;

		var newAttributes = Ext.apply({
			xtype: 'form',
			layout: 'hbox',
			defaultType: 'button',
			defaults: {
				scale: 'medium',
				margins: {
					top: 2,
					right: 4,
					bottom: 2,
					left: 4
				}
			},

			// Add a shadow - cause problem with modal window (this panel stay on top...)
			//floating: true, shadowOffset: 6,

			items: [
				{
					xtype: 'textfield',
					ref: 'searchField',
					name: 'search',
					hideLabel: true,
					margins: {
						top: 6,
						right: 0,
						bottom: 6,
						left: 6
					},
					// Needed to be able to catch 'keydown' event
					enableKeyEvents: true,
					listeners: {
						'specialkey': function(field, evt) {
							if (evt.getKey() == evt.ENTER) {
								that.search(field.getValue());
							}
						},
						// Prevent the Map from grabbing the keys (-/+ to zoom, arrows to pan, etc.)
						'keydown': function(field, evt) {
							evt.stopPropagation();
						}
					}
				}, {
					iconCls: 'searchButton',
					tooltip: 'Search a location; city, reef name, etc.',
					margins: {
						top: 2,
						right: 20,
						bottom: 2,
						left: 4
					},
					handler: function(btn, evt) {
						that.search(btn.ownerCt.searchField.getValue());
					}
				}, {
					iconCls: 'printFrameButton',
					tooltip: 'Prepare map for printing',
					handler: function() {
						that.addPrintFrame();
					}
				}, {
					iconCls: 'linkButton',
					tooltip: 'Link to the embedded map',
					handler: function() {
						that.showEmbeddedLinkWindow();
					}
				}
			],
			region: 'north'
		}, attributes);

		Atlas.MapToolsPanel.superclass.constructor.call(this, newAttributes);
	},

	search: function(query) {
		if (query) {
			var searchResultsLayer = new Atlas.Layer.SearchResults(this.mapPanel, {query: query});

			if (searchResultsLayer.layer) {
				this.mapPanel.map.addLayer(searchResultsLayer.layer);
			}
		} else {
			alert('Enter a keyword in the search field.');
		}
	},

	addPrintFrame: function() {
		var printFrameLayer = new Atlas.Layer.PrintFrame(this.mapPanel, {
			title: 'Print frame',
			description: '<p>\n' +
				'	This layer is a tool to allow high quality maps to be created directly\n' +
				'	from a screenshot. It adds a moveable frame with labelled latitude and\n' +
				'	longitudes. It also includes a moveable north arrow and scale bar. A\n' +
				'	quality looking map can made by using a screenshot then cropping the\n' +
				'	image around the "Print frame".\n' +
				'</p>\n' +
				'<p>\n' +
				'	In the options you can also increase the Dots Per Inch (DPI) of the map\n' +
				'	to increase the size of the text and line rendering.\n' +
				'</p>\n' +
				'<div>\n' +
				'	To use the tool:\n' +
				'	<ol>\n' +
				'		<li>After clicking the "Print frame" button draw a rectangle on the map to select the region of interest.</li>\n' +
				'		<li>Adjust the "Print frame" to accurately frame desired location.</li>\n' +
				'		<li>Expand your browser window as necessary (larger is better).</li>\n' +
				'		<li>Use the "Locate" button in the "Print frame" Options to zoom as much as possible, without hiding the frame.</li>\n' +
				'		<li>Adjust the position of the north arrow and the scale line, if necessary.</li>\n' +
				'		<li>Adjust the styles of the layers to get the best map possible. Look in the Options tab of each layer to see available restyling options.</li>\n' +
				'		<li>Take a screenshot using the "Print Screen" key from your keyboard. On windows you can also use the Accessories/Snipping Tool application.</li>\n' +
				'		<li>Crop the image using an image editor software, to remove any part of the image exceeding the print frame. You can also directly paste the screenshot into Word or PowerPoint and use their crop tool.</li>\n' +
				'		<li>In Word or PowerPoint you can add additional text, arrows and titles.</li>\n' +
				'	</ol>\n' +
				'</div>\n' +
				'<br/>',
			descriptionFormat: 'html'
		});
		this.mapPanel.map.addLayer(printFrameLayer.layer);
	},

	showEmbeddedLinkWindow: function() {
		var that = this;
		var fullUrlStr = this._getUrlForSaveState(null, false);
		var embeddedUrlStr = this._getUrlForSaveState(null, true);

		var warningMsg = "";
		if (embeddedUrlStr.length > this.BROWSER_MAX_URL_LENGTH) {
			warningMsg = '<span style="color:#CC0000;"><b>WARNING:' +
				'</b> The URL length is <b>' + embeddedUrlStr.length + '</b> characters, ' +
				'which is too long for some browsers.</span><br/>\n';
		} else if (embeddedUrlStr.length > this.DRUPAL_MAX_URL_LENGTH) {
			warningMsg = '<span style="color:#CC0000;"><b>WARNING:' +
				'</b> The URL length is <b>' + embeddedUrlStr.length + '</b> characters, ' +
				'which is too long for some DMS like Drupal.</span><br/>\n';
		}
		if (warningMsg) {
			warningMsg += 'You can fix the problem by removing some layers, including the one ' +
				'that are in the list but are not visible on the map, or set a more basic setting ' +
				'for your layers, such as opacity to 100% and using default styles.<br/><br/>\n';
		}

		var uid = Ext.id();

		var saveStateChangeFct = function(evt) {
			fullUrlStr = that._getUrlForSaveState(evt.urlSaveState, false);
			embeddedUrlStr = that._getUrlForSaveState(evt.urlSaveState, true);
			that._updateValues(uid, fullUrlStr, embeddedUrlStr);
		};

		Atlas.core.mapPanels[0].ol_on('saveStateChange', saveStateChangeFct);

		var windowContent = new Ext.Panel({
			autoScroll: true,
			// For some reason, the input widget need a large (10px) right padding when set with 100% width.
			bodyStyle: 'padding: 4px 10px 4px 4px',

			// IE8 need frameborder attribute for the iframe (it do not understand CSS border)
			html: 'Copy / Paste URL in email\n' +
				'<div><input type="text" onClick="this.select()" id="fullLink'+uid+'" style="width:100%;" value="Loading..."></div>\n' +
				'Copy / Paste <b>HTML</b> to create an <i>Embedded map</i>\n' +
				'<div><input type="text" onClick="this.select()" id="embeddedCode'+uid+'" style="width:100%;" value="Loading..."></div>\n' +
				warningMsg +
				'Size: <input id="w'+uid+'" type="text" value="500" style="width:50px"/>px'+
				' X <input id="h'+uid+'" type="text" value="400" style="width:50px"/>px<br/><br/>\n'+
				'<iframe id="previewEmbeddedMap'+uid+'" src="' + embeddedUrlStr + '&pullState=true" frameborder="0" style="border:none;width:500px;height:400px"></iframe>'
		});

		// Add some event listeners on the size input fields
		windowContent.on('afterrender', function() {
			// IMPORTANT: Only one element retrieved with fly can be used at the time;
			//     the element retrieved with fly can not be used after fly is called again.
			var widthFieldEl = Ext.fly('w' + uid);
			widthFieldEl.on('change', function() {
				that._updateValues(uid, fullUrlStr, embeddedUrlStr);
			});

			var heightFieldEl = Ext.fly('h' + uid);
			heightFieldEl.on('change', function() {
				that._updateValues(uid, fullUrlStr, embeddedUrlStr);
			});
		});

		var linksWindow = new Ext.Window({
			title: 'Map URL and Embedded map',
			layout:'fit',
			modal: true,
			width: 524,
			constrainHeader: true,
			closeAction: 'destroy',

			items: windowContent,

			buttons: [{
				text: 'Close',
				handler: function(){
					var window = this.ownerCt.ownerCt;
					window.close();
				}
			}]
		}).show();

		linksWindow.on('destroy', function() {
			Atlas.core.mapPanels[0].ol_un('saveStateChange', saveStateChangeFct);
		});
	},

	_getUrlForSaveState: function(saveState, embedded) {
		if (typeof(saveState) === 'undefined' || saveState == null) {
			saveState = this.mapPanel == null ? null : this.mapPanel._createUrlSaveState();
		}

		// *** Create the embedded client URL ***
		var location = window.location;

		// Get the current URL
		var urlStr = location.href;

		// Remove the file name (index.html) and the parameters
		urlStr = urlStr.substring(0, urlStr.lastIndexOf("/") + 1);

		// Add embedded file name (embedded.html)
		var search = "";
		if (!embedded) {
			urlStr += 'index.html';
			search = '?intro=false';
		} else {
			urlStr += 'embedded.html';
		}

		// Add params to the url
		for (param in saveState) {
			if (saveState.hasOwnProperty(param)) {
				search += (search.length <= 0 ? '?' : '&') +
					param + '=' + saveState[param];
			}
		}
		urlStr += search;

		return urlStr;
	},

	_updateValues: function(uid, fullUrlStr, embeddedUrlStr) {
		var widthField = document.getElementById('w' + uid);
		var width = widthField.value;

		var heightField = document.getElementById('h' + uid);
		var height = heightField.value;

		var embeddedMap = document.getElementById('previewEmbeddedMap' + uid);
		var fullLink = document.getElementById('fullLink' + uid);
		var embeddedCode = document.getElementById('embeddedCode' + uid);

		embeddedMap.style.width = width+'px';
		embeddedMap.style.height = height+'px';

		fullLink.value = fullUrlStr;
		// IE8 need frameborder (it do not understand CSS border)
		embeddedCode.value = '<iframe src="' + embeddedUrlStr + '" frameborder="0" style="border:none;width:'+width+'px;height:'+height+'px"></iframe>';
	}
});
