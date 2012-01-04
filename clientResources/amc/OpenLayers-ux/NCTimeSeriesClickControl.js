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
 * @author Greg Coleman
 */
if (typeof(OpenLayers.Control.ux) == 'undefined') {
	OpenLayers.Control.ux = {};
}

OpenLayers.Control.ux.NCTimeSeriesClickControl = OpenLayers.Class(OpenLayers.Control, {
	defaultHandlerOptions: {
		'single': true,
		'double': false,
		'pixelTolerance': 0,
		'stopSingle': false,
		'stopDouble': false
	},

	map: null,
	layer: null,
	fromDate: "",
	thruDate: "",

	displayDateFormat: 'd/m/Y H:i:s',

	initialize: function(options) {
		// TODO Do we really need to call extend here?? If yes, add a comment
		this.handlerOptions = OpenLayers.Util.extend(
			{},
			this.defaultHandlerOptions
		);
		OpenLayers.Control.prototype.initialize.apply(
			this, arguments
		);
		this.handler = new OpenLayers.Handler.Click(
			this,
			{ 'click': this.trigger },
			this.handlerOptions
		);
	},

	trigger: function(e) {
		var lonlat = this.map.getLonLatFromViewPortPx(e.xy);
		var url=this.layer.getFullRequestString(
			{
				REQUEST: "GetFeatureInfo",
				TIME: this.fromDate + "/" + this.thruDate,
				BBOX: this.map.getExtent().toBBOX(null, this.layer.reverseAxisOrder()),
				X: e.xy.x, Y: e.xy.y, // WMS 1.1.1
				I: e.xy.x, J: e.xy.y, // WMS 1.3.0 and workaround a NCWMS server issue with WMS 1.1.1
				INFO_FORMAT: "image/png",
				height: this.map.getSize().h,
				width: this.map.getSize().w
			},
			null
		);
		// TODO Find a safer way to do this... For example, if the parameter is already called QUERY_LAYERS, it will be change to QUERY_QUERY_LAYERS and the query will not work
		url = url.replace("LAYERS=", "QUERY_LAYERS=");

		var title = '';
		try {
			// This will throw an exception if the date is erroneous.
			title = Date.parseDate(this.fromDate, this.layer.outputFormat).format(this.displayDateFormat) + " - " +
					Date.parseDate(this.thruDate, this.layer.outputFormat).format(this.displayDateFormat);
		} catch (err) {
			// This is just a title, no need to alarm the user.
		}

		new Ext.Window({
			title: title,
			constrainHeader: true,
			bodyStyle: {
				// Set window body size to the size of the image
				width: '400px',
				height: '300px',
				// Waiting image
				background: "#FFFFFF url('resources/images/loading.gif') no-repeat center center"
			},
			html: '<img src="' + url + '" />'
		}).show();
	}
});
