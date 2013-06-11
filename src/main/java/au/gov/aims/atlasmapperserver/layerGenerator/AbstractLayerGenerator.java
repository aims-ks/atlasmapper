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

package au.gov.aims.atlasmapperserver.layerGenerator;

import au.gov.aims.atlasmapperserver.dataSourceConfig.AbstractDataSourceConfig;
import au.gov.aims.atlasmapperserver.layerConfig.AbstractLayerConfig;
import au.gov.aims.atlasmapperserver.layerConfig.LayerCatalog;

public abstract class AbstractLayerGenerator<L extends AbstractLayerConfig, D extends AbstractDataSourceConfig> {
	protected long instanceTimestamp = -1;

	protected abstract String getUniqueLayerId(L layer, D dataSourceConfig);

	public abstract LayerCatalog generateLayerCatalog(D dataSourceConfig, boolean clearCapabilitiesCache, boolean clearMetadataCache) throws Exception;

	// The layer name used to request the layer. Usually, the layerName is
	// the same as the layerId, so this field is let blank. This attribute
	// is only used when there is a duplication of layerId.
	protected void ensureUniqueLayerId(L layer, D dataSourceConfig) {
		String uniqueLayerId =
				dataSourceConfig.getDataSourceId() + "_" +
				this.getUniqueLayerId(layer, dataSourceConfig);

		layer.setLayerName(layer.getLayerId());
		layer.setLayerId(uniqueLayerId);
	}
}
