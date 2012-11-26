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

// Namespace declaration (equivalent to Ext.namespace("Atlas.Layer");)
window["Atlas"] = window["Atlas"] || {};
window["Atlas"]["Layer"] = window["Atlas"]["Layer"] || {};

Atlas.Layer.Google = OpenLayers.Class(Atlas.Layer.AbstractLayer, {
	supportLoadEvents: false,

	// private
	_attributionsElement: null,
	_previousAttributions: null,

	/**
	 * Constructor: Atlas.Layer.Google
	 *
	 * Parameters:
	 * jsonLayer - {Object} Hashtable of layer attributes
	 * mapPanel - {Object} Instance of the MapPanel in which the layer is used
	 */
	initialize: function(mapPanel, jsonLayer, parent) {
		Atlas.Layer.AbstractLayer.prototype.initialize.apply(this, arguments);

		if (this.json != null) {
			var layerOptions = {
				// google.maps.MapTypeId.TERRAIN, google.maps.MapTypeId.ROADMAP, google.maps.MapTypeId.HYBRID, google.maps.MapTypeId.SATELLITE
				type: google.maps.MapTypeId[this.json['layerName'] || this.json['layerId']]
			};

			if (typeof(this.json['olOptions']) !== 'undefined') {
				layerOptions = this.applyOlOverrides(layerOptions, this.json['olOptions']);
			}

			var layer = new OpenLayers.Layer.Google(
				// "Google Physical", "Google Streets", "Google Hybrid", "Google Satellite"
				this.getTitle(),
				layerOptions
			);
			layer.events.on({
				'added': function(e) {
					// Try until it works (maximum 15 tries =~ 2^(20+1) ms =~ 30 minute).
					this._tryToSetAttributionsElement(20);
				},
				scope: this
			});

			this.setLayer(layer);
		}
	},

	/*
	// override
	getPreviewUrl: function() {
		if (this._endsWith(this.json['layerId'], 'HYBRID')) {
			return 'resources/preview/hybrid.png';
		} else if (this._endsWith(this.json['layerId'], 'SATELLITE')) {
			return 'resources/preview/satellite.png';
		} else if (this._endsWith(this.json['layerId'], 'ROADMAP')) {
			return 'resources/preview/roadmap.png';
		} else if (this._endsWith(this.json['layerId'], 'TERRAIN')) {
			return 'resources/preview/terrain.png';
		}
	},
	*/

	_endsWith: function(str, suffix) {
		return str.indexOf(suffix, str.length - suffix.length) !== -1;
	},

	/**
	 * Try to initialise the attribution DOM element.
	 * This will probably fail a few times since Google take its time to create the DOM elements.
	 * This method will try again and again until it succeed, or reach the trial limit.
	 * The delay between tries increase exponentially (*2), starting at 1ms.
	 */
	// private
	_tryToSetAttributionsElement: function(triesLeft, wait) {
		wait = wait || 1;
		
		if (triesLeft <= 0) {
			if (typeof(console) !== 'undefined' && typeof(console.log) === 'function') {
				console.log('ERROR: Google attributions SPAN element can not be found.');
			}
		} else {
			if (!this._setAttributionsElement()) {
				var that = this;
				window.setTimeout(function() {
					that._tryToSetAttributionsElement(triesLeft-1, wait*2);
				}, wait);
			}
		}
	},


	/**
	 * This method set the "_attributionsElement" class property
	 * and add an event listener on the attributions element,
	 * to automatically change the attributions when Google
	 * change them; it usually occur about 1/2 sec after the
	 *     map is panned or zoomed. Google probably use an Ajax
	 *     query to render it. This event is fired immediatelly
	 *     after, solving that problem.
	 * NOTE: The "DOMSubtreeModified" event is not supported by
	 *     Opera nor IE (IE 9 has some support but it doesn't work
	 *     in this context). There is not alternative for those
	 *     browsers, so they will have to call "getAttributions"
	 *     again to have up-to-date attributions.
	 *
	 * Event:
	 *     'attributionsChange'
	 *         Attributes:
	 *             layer: this instance,
	 *             attributions: the new attribution string
	 */
	// private
	_setAttributionsElement: function() {
		var cache = OpenLayers.Layer.Google.cache[this.mapPanel.map.id];
		if (!cache) {
			// Cache not ready yet...
			return false;
		}
		
		var termsOfUseDiv = cache.termsOfUse;
		if (!termsOfUseDiv) {
			// "Terms of use" DOM element not ready yet...
			return false;
		}

		var spans = termsOfUseDiv.getElementsByTagName("span");
		if (!spans || !spans[0]) {
			// "Terms of use" children elements not ready yet...
			return false;
		}

		// Attribution element is now ready!
		this._attributionsElement = spans[0];

		// Add the event listener on the attributions element.
		// (this is equivalent to an "onChange", for a DOM element)
		if (this._attributionsElement.addEventListener) {
			var that = this;
			function onChange(evt) {
				if (that.layer.visibility && that.layer.opacity > 0) {
					var newAttributions = that.getAttributions();
					if (newAttributions != that._previousAttributions) {
						that.mapPanel.map.events.triggerEvent('attributionsChange', {layer: this, attributions: newAttributions});
						that._previousAttributions = newAttributions;
					}
				}
			}

			this._attributionsElement.addEventListener('DOMSubtreeModified', onChange);
		}

		// Ready and initialised
		return true;
	},


	// override
	getAttributions: function() {
		if (this._attributionsElement) {
			// textContent: W3C properties supported by Chrome, Firefox and all major browsers, except IE 8 and earlier.
			// innerText: Supported by all browser (all version of IE) except Firefox (all versions).
			// innerHTML: Fallback that works well in all browsers (it has some flaw when used as a setter, but not as a getter).
			var spanContent = this._attributionsElement.textContent || this._attributionsElement.innerText || this._attributionsElement.innerHTML;

			// Add "Google" to the attributions string, since it's not always part of it
			if (spanContent) {
				return 'Google: [' + spanContent.replace(/^\s\s*/, '').replace(/\s\s*$/, '') + ']';
			}
		}
		return 'Google';
	}
});
