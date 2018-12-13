/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2011 Australian Institute of Marine Science
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

Ext.namespace("Atlas");

Atlas.MapPanel = Ext.extend(GeoExt.MapPanel, Atlas.AbstractMapPanel);

// From here I have to modify the prototype directly... A second extend give an infinite loop for some reason...

/*
Atlas.MapPanel.prototype.destroy = function() {
	// TODO Using "call" instead of "apply" break the application when a map is removed. Try with apply to see if it works.
	Atlas.MapPanel.superclass.destroy.apply(this, arguments);
};
*/

Atlas.MapPanel.prototype.missingLayersCallback = function(missingLayerIds) {
	Ext.Msg.alert('Error', 'The application has failed to load the following layers:<ul class="bullet-list"><li>' + missingLayerIds.join('</li><li>') + '</li></ul>');
};
