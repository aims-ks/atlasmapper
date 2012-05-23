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

import au.gov.aims.atlasmapperserver.ConfigManager;
import au.gov.aims.atlasmapperserver.dataSourceConfig.AbstractDataSourceConfig;
import au.gov.aims.atlasmapperserver.dataSourceConfig.NcWMSDataSourceConfig;
import au.gov.aims.atlasmapperserver.layerConfig.NcWMSLayerConfig;
import org.geotools.ows.ServiceException;

import java.io.IOException;
import java.util.logging.Logger;

public class NcWMSLayerGenerator extends AbstractWMSLayerGenerator<NcWMSLayerConfig, NcWMSDataSourceConfig> {
	private static final Logger LOGGER = Logger.getLogger(NcWMSLayerGenerator.class.getName());

	public NcWMSLayerGenerator(AbstractDataSourceConfig dataSource) throws IOException, ServiceException {
		super(dataSource);
	}

	@Override
	protected NcWMSLayerConfig createLayerConfig(ConfigManager configManager) {
		return new NcWMSLayerConfig(configManager);
	}
}
