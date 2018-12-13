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

package au.gov.aims.atlasmapperserver.dataSourceConfig;

import au.gov.aims.atlasmapperserver.ConfigManager;
import au.gov.aims.atlasmapperserver.jsonWrappers.client.DataSourceWrapper;

import java.util.logging.Logger;

public class DataSourceConfigHelper {
	private static final Logger LOGGER = Logger.getLogger(DataSourceConfigHelper.class.getName());

	public static AbstractDataSourceConfig createDataSourceConfig(DataSourceWrapper dataSourceWrapper, ConfigManager configManager) {
		AbstractDataSourceConfig dataSourceConfig = null;
		if (dataSourceWrapper.isArcGISMapServer()) {
			dataSourceConfig = new ArcGISMapServerDataSourceConfig(configManager);
		} else if (dataSourceWrapper.isGoogle()) {
			dataSourceConfig = new GoogleDataSourceConfig(configManager);
		} else if (dataSourceWrapper.isBing()) {
			dataSourceConfig = new BingDataSourceConfig(configManager);
		} else if (dataSourceWrapper.isKML()) {
			dataSourceConfig = new KMLDataSourceConfig(configManager);
		} else if (dataSourceWrapper.isNCWMS()) {
			dataSourceConfig = new NcWMSDataSourceConfig(configManager);
		} else if (dataSourceWrapper.isTiles()) {
			dataSourceConfig = new TilesDataSourceConfig(configManager);
		} else if (dataSourceWrapper.isXYZ()) {
			dataSourceConfig = new XYZDataSourceConfig(configManager);
		} else if (dataSourceWrapper.isWMS()) {
			dataSourceConfig = new WMSDataSourceConfig(configManager);
		} else {
			// Unsupported
			throw new IllegalArgumentException("Unsupported layer type [" + dataSourceWrapper.getLayerType() + "]");
		}

		// Set all data source values into the data source bean
		if (dataSourceConfig != null) {
			dataSourceConfig.update(dataSourceWrapper.getJSON());
		}

		return dataSourceConfig;
	}
}
