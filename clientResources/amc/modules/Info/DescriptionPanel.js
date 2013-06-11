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

Atlas.DescriptionPanel = Ext.extend(Ext.Panel, {
	layer: null,
	iframePanel: null,
	downloadButton: null,
	downloadLinks: null,

	layout: {
		type: 'vbox',
		align: 'stretch'  // Child items are stretched to full width
	},

	initComponent: function() {
		Atlas.DescriptionPanel.superclass.initComponent.call(this);

		this.iframe.xtype = 'ux_iframepanel';
		this.iframe.flex = 1;
		this.iframe.autoScroll = true;

		this.iframePanel = this.add(this.iframe);
		this.downloadButton = this.add({
			xtype:'button',
			text: 'Downloads',
			hidden: true,
			cls: 'downloadLinksButton',
			height: 40,
			handler: this.showDownloads,
			scope: this
		});

		// Spacer; ExtJS have some problem calculating the height of this panel... it always add 6 pixels for some reason.
		//     This spacer ensure the 6 last pixels of the panel wont be used.
		this.add({
			border: false,
			height: 6
		});
	},

	setLayer: function(atlasLayer) {
		this.layer = atlasLayer;
		var description = null;
		var downloadLinks = null;
		if (atlasLayer) {
			description = atlasLayer.getDescription();
			downloadLinks = atlasLayer.getDownloadLinks();
		}

		this.setDownloadLinks(downloadLinks);
		if (description) {
			this.iframePanel.setContent(description);
		} else {
			// Reset the tab's SRC
			this.iframePanel.setSrc();
		}
	},
	setContent: function(content) {
		this.setLayer(null);
		this.iframePanel.setContent(content);
	},

	setDownloadLinks: function(links) {
		this.downloadLinks = links;
		if (links) {
			this.downloadButton.show();
		} else {
			this.downloadButton.hide();
		}
		this.doLayout();
	},

	showDownloads: function(button, evt) {
		var layerTitle = 'unknown';
		if (this.layer) {
			layerTitle = this.layer.getTitle();
		}
		var downloadWindow = new Ext.Window({
			title: layerTitle + ' downloads',
			closable: true,
			width: 500,
			height: 350,
			plain: true,
			layout: 'fit',
			border: false,
			items: [{
				autoScroll: true,
				html: '<div class="downloadLinks">' +
					'<div class="title">Resources available for download:</div>' +
					this.downloadLinks +
					'</div>'
			}]
		});
		downloadWindow.show();
	}
});

Ext.reg('atlas_descriptionpanel', Atlas.DescriptionPanel);
