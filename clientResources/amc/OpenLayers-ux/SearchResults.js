/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2012 Australian Institute of Marine Science
 *
 *  Contact: Gael Lafond <g.lafond@aims.gov.au>
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

OpenLayers.Layer.ux = OpenLayers.Layer.ux || {};
OpenLayers.Layer.ux.SearchResults = OpenLayers.Class(OpenLayers.Layer.Vector, {
	CLASS_NAME: "OpenLayers.Layer.ux.SearchResults",
	MARKER_SIZE: 20,
	MARKER_HIGHLIGHT_SIZE: 25,

	defaultLonLatProjection: new OpenLayers.Projection('EPSG:4326'),

	markerStyle: null,

	// hash map of features displayed on map
	_features: null,

	initialize: function(name, options) {
		OpenLayers.Layer.Vector.prototype.initialize.apply(this, arguments);

		this.options = this.options || {};

		this.markerStyle = {
			externalGraphic: 'resources/markers/redA.png',
			pointRadius: this.MARKER_SIZE,
			graphicYOffset: this.MARKER_SIZE * -2
		};

		this.events.on({
			'featureselected': function(evt) {
				this.highlight(evt.feature);
			},
			'featureunselected': function(evt) {
				this.unhighlight(evt.feature);
			},
			scope: this
		});
	},

	afterAdd: function() {
		OpenLayers.Layer.Vector.prototype.afterAdd.apply(this, arguments);

		// This method is also called every time the layer order is changed.
		// The following has to be called only once, after the layer
		// is added to the map.
		if (!this._initiated && this.map) {
			var multiSelectDragFeature = OpenLayers.Control.ux.MultiSelectDragFeature.getInstance(this.map);
			multiSelectDragFeature.addLayer(this);

			this._initiated = true;
		}
	},

	setResults: function(results) {
		this._features = {};
		this.removeAllFeatures();

		// loop through the results
		if (results && results.length) {
			var foundFeatures = [];

			for (var i=results.length-1; i>=0; i--) {
				var center = this._reproject(new OpenLayers.Geometry.Point(
						results[i].center[0],
						results[i].center[1]));
				var id = results[i].id;

				// NOTE: If polygons or other geometry than Points are added, also modify the locate method.

				// Clone the style object
				var style = OpenLayers.Util.extend({}, this.markerStyle);
				style.externalGraphic = results[i].markerUrl;

				var feature = new OpenLayers.Feature.Vector(center, null, style);
				// Store the ID and the result in the object, to be able to locate it and find it's associate element in the layer description.
				feature._id = id;
				feature._result = results[i];

				this._features[id] = feature;
				foundFeatures.push(feature);
			}
			this.addFeatures(foundFeatures);
		}

		// Trigger the layerupdate event to refresh the description
		this.events.triggerEvent('layerupdate');
	},

	/**
	 * feature: the layer point, or ID string of the point.
	 */
	// See: http://dev.openlayers.org/docs/files/OpenLayers/Feature/Vector-js.html#OpenLayers.Feature.Vector.OpenLayers.Feature.Vector.style
	highlight: function(feature) {
		if (typeof(feature) === 'string') {
			feature = this._features[feature];
		}
		if (feature) {
			feature.style.pointRadius = this.MARKER_HIGHLIGHT_SIZE;
			feature.style.graphicYOffset = this.MARKER_HIGHLIGHT_SIZE * -2;
			// Bring-to-front and redraw
			this.removeFeatures([feature]);
			this.addFeatures([feature]);
		}
	},

	unhighlight: function(feature) {
		if (typeof(feature) === 'string') {
			feature = this._features[feature];
		}
		if (feature) {
			feature.style.pointRadius = this.MARKER_SIZE;
			feature.style.graphicYOffset = this.MARKER_SIZE * -2;
			// Redraw
			this.drawFeature(feature);
		}
	},

	locate: function(feature) {
		if (typeof(feature) === 'string') {
			feature = this._features[feature];
		}
		if (feature && this.map) {
			// The layer only contains Points, but that will probably change in the future...
			if (feature._result && feature._result.bbox) {
				var bbox = feature._result.bbox;
				this.map.zoomToExtent(this._reproject(new OpenLayers.Bounds(bbox[0], bbox[1], bbox[2], bbox[3])));
			} else {
				if (feature.geometry instanceof OpenLayers.Geometry.Point) {
					// Since it's a Point, "zoomTo" has the tandancy to zoom to
					// the maximum zoom level, which is usually way too close.
					// "panTo" move the map without changing the zoom level,
					// which is more appropriate for this purpose. 
					this.map.panTo(new OpenLayers.LonLat(feature.geometry.x, feature.geometry.y));
				}
			}
		}
	},

	_reproject: function(geometry) {
		if (this.map.projection != this.defaultLonLatProjection) {
			return geometry.transform(this.defaultLonLatProjection, this.map.projection);
		}
		return geometry;
	}
});
