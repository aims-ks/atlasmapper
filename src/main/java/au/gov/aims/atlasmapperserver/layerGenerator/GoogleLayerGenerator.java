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

import au.gov.aims.atlasmapperserver.ClientConfig;
import au.gov.aims.atlasmapperserver.dataSourceConfig.GoogleDataSourceConfig;
import au.gov.aims.atlasmapperserver.layerConfig.GoogleLayerConfig;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author glafond
 */
public class GoogleLayerGenerator extends AbstractLayerGenerator<GoogleLayerConfig, GoogleDataSourceConfig> {
	// The layers do not changes often enough to develop some sort of parser.
	private static Map<String, GoogleLayerConfig> googleLayersCache = null;

	/**
	 * The number of Google Layers is fix and they already have unique IDs. Nothing to do here.
	 * @param layer
	 * @param dataSourceConfig
	 * @return
	 */
	@Override
	protected String getUniqueLayerId(GoogleLayerConfig layer, GoogleDataSourceConfig dataSourceConfig) {
		return layer.getLayerId();
	}

	/**
	 * Create and return the 4 google layers.
	 *     * Google Physical
	 *     * Google Streets
	 *     * Google Hybrid
	 *     * Google Satellite
	 * @param clientConfig
	 * @return
	 */
	@Override
	public Map<String, GoogleLayerConfig> generateLayerConfigs(ClientConfig clientConfig, GoogleDataSourceConfig dataSourceConfig) {
		if (googleLayersCache == null) {
			googleLayersCache = new HashMap<String, GoogleLayerConfig>();

			GoogleLayerConfig terrain = this.createGoogleLayer(dataSourceConfig, "TERRAIN", "Google Physical", null, 16);
			googleLayersCache.put(terrain.getLayerId(), terrain);

			// This layer goes up to 22, but it's pointless to go that close... 20 is good enough
			GoogleLayerConfig roadmap = this.createGoogleLayer(dataSourceConfig, "ROADMAP", "Google Streets", null, 20);
			googleLayersCache.put(roadmap.getLayerId(), roadmap);

			// The number of zoom level is a mix of 20 - 22, depending on the location, OpenLayers do not support that very well...
			GoogleLayerConfig hybrid = this.createGoogleLayer(dataSourceConfig, "HYBRID", "Google Hybrid", null, 20);
			googleLayersCache.put(hybrid.getLayerId(), hybrid);

			// The number of zoom level is a mix of 20 - 22, depending on the location, OpenLayers do not support that very well...
			GoogleLayerConfig satellite = this.createGoogleLayer(dataSourceConfig, "SATELLITE", "Google Satellite", null, 20);
			googleLayersCache.put(satellite.getLayerId(), satellite);
		}
		return googleLayersCache;
	}

	@Override
	public GoogleDataSourceConfig applyOverrides(GoogleDataSourceConfig dataSourceConfig) {
		return dataSourceConfig;
	}

	private GoogleLayerConfig createGoogleLayer(GoogleDataSourceConfig dataSourceConfig, String googleLayerType, String name, String description, Integer numZoomLevels) {
		GoogleLayerConfig layerConfig = new GoogleLayerConfig(dataSourceConfig.getConfigManager());
		layerConfig.setDataSourceId(dataSourceConfig.getDataSourceId());

		layerConfig.setLayerId(googleLayerType);
		layerConfig.setTitle(name);
		layerConfig.setDescription(description);
		layerConfig.setIsBaseLayer(true);
		layerConfig.setLayerBoundingBox(new double[]{-180, -90, 180, 90});

		if (numZoomLevels != null) {
			layerConfig.setNumZoomLevels(numZoomLevels);
		}

		this.ensureUniqueLayerId(layerConfig, dataSourceConfig);

		return layerConfig;
	}
}
