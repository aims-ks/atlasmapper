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
// Update call setText on the element index 0... which can be the delete button...
// This patch loop through the legend items to find the label, rather than
// taking the first one and assuming that it is the label.
GeoExt.LayerLegend.prototype.update = function() {
	var title = this.getLayerTitle(this.layerRecord);
	this.items.each(function(item) {
		if (item.getXType() === 'label' && typeof(item.setText) === 'function') {
			// Label found
			if (item.text !== title) {
				item.setText(title);
			}
			// Stop the iteration
			return false;
		}
	});
};
