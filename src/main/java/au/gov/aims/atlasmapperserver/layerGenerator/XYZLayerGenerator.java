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

import au.gov.aims.atlasmapperserver.dataSourceConfig.XYZDataSourceConfig;
import au.gov.aims.atlasmapperserver.layerConfig.XYZLayerConfig;

import java.util.Collection;

public class XYZLayerGenerator extends AbstractLayerGenerator<XYZLayerConfig, XYZDataSourceConfig> {

	public XYZLayerGenerator(XYZDataSourceConfig dataSource) {
		super(dataSource);
	}

	/**
	 * We thrust the Admin to choose Unique IDs for all it's XYZ layers. Nothing to do here.
	 * @param layer
	 * @param dataSourceConfig
	 * @return
	 */
	@Override
	protected String getUniqueLayerId(XYZLayerConfig layer, XYZDataSourceConfig dataSourceConfig) {
		return layer.getLayerId();
	}

	@Override
	public Collection<XYZLayerConfig> generateLayerConfigs(XYZDataSourceConfig dataSourceConfig) throws Exception {
		return null; // All layers are created using the layer overrides
	}

	@Override
	public XYZDataSourceConfig applyOverrides(XYZDataSourceConfig dataSourceConfig) {
		return dataSourceConfig;
	}
}
