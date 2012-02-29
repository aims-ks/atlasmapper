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
import org.json.JSONObject;

public class DataSourceConfigHelper {
	public static AbstractDataSourceConfig createDataSourceConfig(JSONObject dataSourceConfigJSON, ConfigManager configManager) {
		AbstractDataSourceConfig dataSourceConfig = null;
		String dataSourceType = dataSourceConfigJSON.optString("dataSourceType");
		if ("ARCGIS_MAPSERVER".equals(dataSourceType)) {
			dataSourceConfig = new ArcGISMapServerDataSourceConfig(configManager);
		} else if ("GOOGLE".equals(dataSourceType)) {
			dataSourceConfig = new GoogleDataSourceConfig(configManager);
		} else if ("KML".equals(dataSourceType)) {
			dataSourceConfig = new KMLDataSourceConfig(configManager);
		} else if ("NCWMS".equals(dataSourceType)) {
			dataSourceConfig = new NcWMSDataSourceConfig(configManager);
		} else if ("TILES".equals(dataSourceType)) {
			dataSourceConfig = new TilesDataSourceConfig(configManager);
		} else if ("XYZ".equals(dataSourceType)) {
			dataSourceConfig = new XYZDataSourceConfig(configManager);
		} else {
			// WMS, WMTS and other
			dataSourceConfig = new WMSDataSourceConfig(configManager);
		}

		// Set all data source values into the data source bean
		if (dataSourceConfig != null) {
			dataSourceConfig.update(dataSourceConfigJSON);
		}

		return dataSourceConfig;
	}
}
