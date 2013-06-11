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

if (typeof(OpenLayers.Layer.ux) == 'undefined') {
	OpenLayers.Layer.ux = {};
}

OpenLayers.Layer.ux.NCWMS = OpenLayers.Class(OpenLayers.Layer.WMS, {
	CLASS_NAME: "OpenLayers.Layer.ux.NCWMS",

	// Date format used to display the date after selecting it from the calendar.
	dateFormat: "d/m/Y",

	// Date format used to display the times in the dropdown.
	timeFormat: "H:i:s",

	// Date format used to request the times.
	dateRequestFormat: 'Y-m-d',

	// Date format used to parse the times returned by the request.
	// NOTE: the literal 'Z' is used to avoid using javascript timezone.
	// Example: "14:00:00.000Z"
	timeResponseFormat: 'H:i:s.u\\Z',

	// Date format used to request the ncWMS layer.
	// NOTE: the literal 'Z' is used to avoid using javascript timezone.
	// Example: "2010-01-12T14:00:00.000Z"
	outputFormat: 'Y-m-d\\TH:i:s.u\\Z',

	availableDates: null,
	defaultDate: null,

	getCurrentTime: function() {
		var param = 'TIME';
		if (!this.params) {
			return null;
		}

		if (typeof(this.params[param]) !== 'undefined') {
			return this.params[param];
		}

		// Try with to uppercase the parameter; OpenLayers usually put all parameters in uppercase.
		var uppercaseParam = param.toUpperCase();
		if (typeof(this.params[uppercaseParam]) !== 'undefined') {
			return this.params[uppercaseParam];
		}

		return null;
	},

	// Request available dates to the server and call the callback with them.
	// This method cache the dates so it do not have to request them again.
	// callback: function to be call with the array of dates
	//     parameters:
	//         availableDates (array of dateFormatted)
	//         defaultDate (string)
	// errorCallback: function to be call in case of an error
	//     parameter: error (string)
	// scope: Scope used to call the callbacks
	// force: boolean. True to get the dates from the server instead of the cache. Default: false
	getAvailableDates: function(callback, errorCallback, scope, force) {
		if (typeof(scope) === 'undefined' || scope === null) {
			scope = this;
		}

		if (this.availableDates == null || this.defaultDate == null || force) {
			this._reloadDates(callback, errorCallback, scope);
		} else {
			callback.call(scope, this.availableDates, this.defaultDate);
		}
	},

	// private
	_reloadDates: function(callback, errorCallback, scope) {
		if (!this.atlasLayer || !this.atlasLayer.json) {
			// This should not append
			return;
		}

		var serviceUrl = this.atlasLayer.json['serviceUrl'];

		var url = serviceUrl + '?' + Ext.urlEncode({
			item: 'layerDetails',
			layerName: this.atlasLayer.json['layerName'],
			request: 'GetMetadata'
		});
		/**
			Parameters
				uri         {String} URI of source doc
				params      {String} Params on get (doesnt seem to work)
				caller      {Object} object which gets callbacks
				onComplete  {Function} callback for success
				onFailure   {Function} callback for failure
			Both callbacks optional (though silly)
		*/
		var that = this;
		OpenLayers.Request.GET({
			url: url,
			scope: this,
			success: function (result, request) {
				that._reloadDatesCallback(result, request, callback, errorCallback, scope);
			},
			failure: function (result, request) {
				var errorMessage = 'Unknown error';
				try {
					var jsonData = Ext.util.JSON.decode(result.responseText);
					errorMessage = jsonData.data.result;
				} catch (err) {
					errorMessage = result.responseText;
				}
				if (errorCallback) {
					errorCallback.call(scope, errorMessage);
				} else {
					alert('Error while loading the dates for the NCWMS layer "'+this.atlasLayer.json['layerName']+'": ' + errorMessage);
				}
			}
		});
	},

	// private
	_reloadDatesCallback: function(result, request, callback, errorCallback, scope) {
		var jsonData = null;
		try {
			jsonData = Ext.util.JSON.decode(result.responseText);
		} catch (err) {
			var errorMessage = result.responseText;
			if (errorCallback) {
				errorCallback.call(scope, errorMessage);
			} else {
				alert('Error while loading the dates for the NCWMS layer "'+this.atlasLayer.json['layerName']+'": ' + errorMessage);
			}
			return;
		}

		if (jsonData == null) {
			return;
		}

		if (jsonData.exception) {
			var errorMessage = (jsonData.exception.message ? jsonData.exception.message : jsonData.exception);
			// TODO Error on the page
			if (errorCallback) {
				errorCallback.call(scope, errorMessage);
			} else {
				alert('Error while loading the dates for the NCWMS layer "'+this.atlasLayer.json['layerName']+'": ' + errorMessage);
			}
			return;
		}

		var dateArray = [];

		if (jsonData['datesWithData']) {
			Ext.iterate(jsonData.datesWithData, function(year, months) {
				Ext.iterate(months, function(month, days) {
					Ext.each(days, function(day) {
						// Create a date object to format it in the desire format.

						// Month is from 0-11 instead of 1-12.
						var monthInt = (parseInt(month, 10)) + 1;

						// Y-m-d, without leading zeros => Y-n-j
						var date = Date.parseDate(year+'-'+monthInt+'-'+day, 'Y-n-j');

						// Available dates format must match the display date format.
						dateArray.push(date.format(this.dateFormat));
					}, this);
				}, this);
			}, this);
		}

		this.availableDates = dateArray;
		this.defaultDate = jsonData['nearestTimeIso'];

		callback.call(scope, this.availableDates, this.defaultDate);
	},


	// Request available times to the server, for a given date, and call the
	// callback with them.
	// callback: function to be call with the array of times
	//     parameter: times (array of [timeReceived, timeFormatted])
	// errorCallback: function to be call in case of an error
	//     parameter: error (string)
	// scope: Scope used to call the callbacks
	getAvailableTimes: function(date, callback, errorCallback, scope) {
		if (!this.atlasLayer || !this.atlasLayer.json) {
			// This should not append
			return;
		}

		if (typeof(scope) === 'undefined' || scope === null) {
			scope = this;
		}

		var serviceUrl = this.atlasLayer.json['serviceUrl'];
		var dateStr = date.format(this.dateRequestFormat).trim();

		var url = serviceUrl + '?' + Ext.urlEncode({
			item: 'timesteps',
			layerName: this.atlasLayer.json['layerName'],
			day: dateStr,
			request: 'GetMetadata'
		});

		/**
			Parameters
				uri         {String} URI of source doc
				params      {String} Params on get (doesnt seem to work)
				caller      {Object} object which gets callbacks
				onComplete  {Function} callback for success
				onFailure   {Function} callback for failure
			Both callbacks optional (though silly)
		*/
		var that = this;
		OpenLayers.Request.GET({
			url: url,
			scope: this,
			success: function (result, request) {
				that._getAvailableTimesRawCallback(result, request, date, callback, errorCallback, scope);
			},
			failure: function (result, request) {
				var errorMessage = 'Unknown error';
				try {
					var jsonData = Ext.util.JSON.decode(result.responseText);
					errorMessage = jsonData.data.result;
				} catch (err) {
					errorMessage = result.responseText;
				}
				if (errorCallback) {
					errorCallback.call(scope, errorMessage);
				} else {
					alert('Error while loading the times for the NCWMS layer "'+that.atlasLayer.json['layerName']+'": ' + errorMessage);
				}
			}
		});
	},

	// private
	_getAvailableTimesRawCallback: function(result, request, date, callback, errorCallback, scope) {
		var jsonData = null;
		try {
			jsonData = Ext.util.JSON.decode(result.responseText);
		} catch (err) {
			var errorMessage = result.responseText;
			if (errorCallback) {
				errorCallback.call(scope, errorMessage);
			} else {
				alert('Error while loading the times for the NCWMS layer "'+this.atlasLayer.json['layerName']+'": ' + errorMessage);
			}
			return;
		}

		if (jsonData == null) {
			return;
		}

		if (jsonData.exception) {
			var errorMessage = (jsonData.exception.message ? jsonData.exception.message : jsonData.exception);
			if (errorCallback) {
				errorCallback.call(scope, errorMessage);
			} else {
				alert('Error while loading the times for the NCWMS layer "'+this.atlasLayer.json['layerName']+'": ' + errorMessage);
			}
			return;
		}

		var timesArray = [];

		if (jsonData['timesteps']) {
			Ext.each(jsonData['timesteps'], function(time) {
				var timeObj = Date.parseDate(time, this.timeResponseFormat);
				var timeStr = timeObj.format(this.timeFormat);
				timesArray.push([time, timeStr]);
			}, this);
		}

		callback.call(scope, timesArray);
	}
});
