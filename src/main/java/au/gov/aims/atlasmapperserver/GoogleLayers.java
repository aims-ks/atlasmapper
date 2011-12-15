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

package au.gov.aims.atlasmapperserver;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author glafond
 */
public class GoogleLayers {
	// The layers do not changes often enough to develop some sort of parser.
	private static Map<String, LayerConfig> googleLayersCache = null;

	/**
	 * Create and return the 4 google layers.
	 *     * Google Physical
	 *     * Google Streets
	 *     * Google Hybrid
	 *     * Google Satellite
	 * @param clientConfig
	 * @return
	 */
	public static synchronized Map<String, LayerConfig> getGoogleLayerConfigs(ClientConfig clientConfig, DataSourceConfig dataSourceConfig) {
		if (googleLayersCache == null) {
			googleLayersCache = new HashMap<String, LayerConfig>();

			googleLayersCache.put("TERRAIN",
					createGoogleLayer(dataSourceConfig, "TERRAIN", "Google Physical", null));

			googleLayersCache.put("ROADMAP",
					createGoogleLayer(dataSourceConfig, "ROADMAP", "Google Streets", null));

			googleLayersCache.put("HYBRID",
					createGoogleLayer(dataSourceConfig, "HYBRID", "Google Hybrid", null));

			googleLayersCache.put("SATELLITE",
					createGoogleLayer(dataSourceConfig, "SATELLITE", "Google Satellite", null));
			/*
			var gphy = new OpenLayers.Layer.Google(
				"Google Physical",
				{type: google.maps.MapTypeId.TERRAIN}
			);
			var gmap = new OpenLayers.Layer.Google(
				"Google Streets", // the default
				// TYPE: google.maps.MapTypeId.ROADMAP
				{numZoomLevels: 20}
			);
			var ghyb = new OpenLayers.Layer.Google(
				"Google Hybrid",
				{type: google.maps.MapTypeId.HYBRID, numZoomLevels: 20}
			);
			var gsat = new OpenLayers.Layer.Google(
				"Google Satellite",
				{type: google.maps.MapTypeId.SATELLITE, numZoomLevels: 22}
			);
			*/
		}
		return googleLayersCache;
	}

	private static LayerConfig createGoogleLayer(DataSourceConfig dataSourceConfig, String googleLayerType, String name, String description) {
		LayerConfig layerConfig = new LayerConfig(dataSourceConfig.getConfigManager());
		layerConfig.setDataSourceId(dataSourceConfig.getDataSourceId());

		layerConfig.setLayerId(googleLayerType);
		layerConfig.setTitle(name);
		layerConfig.setDescription(description);
		layerConfig.setIsBaseLayer(true);
		layerConfig.setLayerBoundingBox(new double[]{-180, -90, 180, 90});

		return layerConfig;
	}
}
