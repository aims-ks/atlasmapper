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

package au.gov.aims.atlasmapperserver.dataSourceConfig;

import au.gov.aims.atlasmapperserver.ConfigManager;
import au.gov.aims.atlasmapperserver.layerGenerator.AbstractLayerGenerator;
import au.gov.aims.atlasmapperserver.layerGenerator.LayerGeneratorCache;
import au.gov.aims.atlasmapperserver.layerGenerator.WMTSLayerGenerator;
import org.geotools.ows.ServiceException;

import java.io.IOException;

public class WMTSDataSourceConfig extends WMSDataSourceConfig {
	public WMTSDataSourceConfig(ConfigManager configManager) {
		super(configManager);
	}

	@Override
	public AbstractLayerGenerator getLayerGenerator() throws IOException {
		AbstractLayerGenerator layerGenerator = null;
		try {
			layerGenerator = LayerGeneratorCache.getInstance(this.getServiceUrl(), WMTSLayerGenerator.class);
		} catch (ServiceException e) {
			throw new IOException("Service Exception occurred while retrieving the WMS layer generator.", e);
		}
		return layerGenerator;
	}
}
