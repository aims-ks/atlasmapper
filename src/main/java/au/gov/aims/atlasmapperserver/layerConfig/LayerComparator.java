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
package au.gov.aims.atlasmapperserver.layerConfig;

import java.util.Comparator;

public class LayerComparator implements Comparator<AbstractLayerConfig> {
	@Override
	public int compare(AbstractLayerConfig layer1, AbstractLayerConfig layer2) {
		// Same instance or both null
		if (layer1 == layer2) { return 0; }

		// Place null at the end of the list
		if (layer1 == null) { return -1; }
		if (layer2 == null) { return 1; }

		String layerId1 = layer1.getLayerId();
		String layerId2 = layer2.getLayerId();

		// Same String instance or both null (this should only happen when the 2 instances represent the same layer)
		if (layerId1 == layerId2) { return 0; }

		// Null at the end of the list (this should never happen, all layers has a unique ID)
		if (layerId1 == null) { return -1; }
		if (layerId2 == null) { return 1; }

		// Compare ID, considering case to minimise the probability of clashes
		return layerId1.compareTo(layerId2);
	}
}
