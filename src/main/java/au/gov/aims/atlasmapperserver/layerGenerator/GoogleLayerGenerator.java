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

import au.gov.aims.atlasmapperserver.dataSourceConfig.GoogleDataSourceConfig;
import au.gov.aims.atlasmapperserver.layerConfig.GoogleLayerConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author glafond
 */
public class GoogleLayerGenerator extends AbstractLayerGenerator<GoogleLayerConfig, GoogleDataSourceConfig> {
	// The layers do not changes often enough to develop some sort of parser.
	private static Collection<GoogleLayerConfig> googleLayersCache = null;

	public GoogleLayerGenerator(GoogleDataSourceConfig dataSource) {
		super(dataSource);
	}

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
	 * @return
	 */
	@Override
	public Collection<GoogleLayerConfig> generateLayerConfigs(GoogleDataSourceConfig dataSourceConfig) {
		if (googleLayersCache == null) {
			googleLayersCache = new ArrayList<GoogleLayerConfig>();

			googleLayersCache.add(this.createGoogleLayer(dataSourceConfig, "TERRAIN", "Google Physical", null, 16));

			// This layer goes up to 22, but it's pointless to go that close... 20 is good enough
			googleLayersCache.add(this.createGoogleLayer(dataSourceConfig, "ROADMAP", "Google Streets", null, 20));

			// The number of zoom level is a mix of 20 - 22, depending on the location, OpenLayers do not support that very well...
			googleLayersCache.add(this.createGoogleLayer(dataSourceConfig, "HYBRID", "Google Hybrid", null, 20));

			// The number of zoom level is a mix of 20 - 22, depending on the location, OpenLayers do not support that very well...
			googleLayersCache.add(this.createGoogleLayer(dataSourceConfig, "SATELLITE", "Google Satellite", null, 20));
		}
		return googleLayersCache;
	}

	@Override
	public GoogleDataSourceConfig applyOverrides(GoogleDataSourceConfig dataSourceConfig) {
		return dataSourceConfig;
	}

	private GoogleLayerConfig createGoogleLayer(GoogleDataSourceConfig dataSourceConfig, String googleLayerType, String name, String description, Integer numZoomLevels) {
		GoogleLayerConfig layerConfig = new GoogleLayerConfig(dataSourceConfig.getConfigManager());

		layerConfig.setLayerId(googleLayerType);
		layerConfig.setTitle(name);
		layerConfig.setDescription(description);
		layerConfig.setIsBaseLayer(true);
		layerConfig.setLayerBoundingBox(new double[]{-180, -90, 180, 90});

		if (numZoomLevels != null) {
			layerConfig.setNumZoomLevels(numZoomLevels);
		}

		dataSourceConfig.bindLayer(layerConfig);
		this.ensureUniqueLayerId(layerConfig, dataSourceConfig);

		return layerConfig;
	}
}
