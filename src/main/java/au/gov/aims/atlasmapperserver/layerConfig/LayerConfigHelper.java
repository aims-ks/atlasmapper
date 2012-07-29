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

package au.gov.aims.atlasmapperserver.layerConfig;

import au.gov.aims.atlasmapperserver.ConfigManager;
import org.json.JSONObject;

public class LayerConfigHelper {
	public static AbstractLayerConfig createLayerConfig(String dataSourceType, JSONObject layerConfigJSON, ConfigManager configManager) {
		AbstractLayerConfig layerConfig = null;
		if ("ARCGIS_MAPSERVER".equals(dataSourceType)) {
			layerConfig = new ArcGISMapServerLayerConfig(configManager);
		} else if ("GOOGLE".equals(dataSourceType)) {
			layerConfig = new GoogleLayerConfig(configManager);
		} else if ("BING".equals(dataSourceType)) {
			layerConfig = new BingLayerConfig(configManager);
		} else if ("KML".equals(dataSourceType)) {
			layerConfig = new KMLLayerConfig(configManager);
		} else if ("NCWMS".equals(dataSourceType)) {
			layerConfig = new NcWMSLayerConfig(configManager);
		} else if ("TILES".equals(dataSourceType)) {
			layerConfig = new TilesLayerConfig(configManager);
		} else if ("XYZ".equals(dataSourceType)) {
			layerConfig = new XYZLayerConfig(configManager);
		} else if ("WMS".equals(dataSourceType)) {
			layerConfig = new WMSLayerConfig(configManager);
		} else if ("GROUP".equals(dataSourceType)) {
			layerConfig = new GroupLayerConfig(configManager);
		} else {
			// Unsupported
			throw new IllegalArgumentException("Unsupported data source type [" + dataSourceType + "]");
		}

		// Set all data source values into the data source bean
		if (layerConfig != null) {
			layerConfig.update(layerConfigJSON);
		}

		return layerConfig;
	}
}
