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
import au.gov.aims.atlasmapperserver.layerConfig.LayerCatalog;

/**
 *
 * @author glafond
 */
public class GoogleLayerGenerator extends AbstractLayerGenerator<GoogleLayerConfig, GoogleDataSourceConfig> {
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
	 * NOTE: Harvest is ignored since there is nothing to harvest.
	 */
	@Override
	public LayerCatalog generateRawLayerCatalog(GoogleDataSourceConfig dataSourceConfig, boolean redownloadPrimaryFiles, boolean redownloadSecondaryFiles) {
		LayerCatalog layerCatalog = new LayerCatalog();

		layerCatalog.addLayer(this.createGoogleLayer(dataSourceConfig, "TERRAIN", "Google Physical", null, 16));

		// This layer goes up to 22, but it's pointless to go that close... 20 is good enough
		layerCatalog.addLayer(this.createGoogleLayer(dataSourceConfig, "ROADMAP", "Google Streets", null, 20));

		// The number of zoom level is a mix of 20 - 22, depending on the location, OpenLayers do not support that very well...
		layerCatalog.addLayer(this.createGoogleLayer(dataSourceConfig, "HYBRID", "Google Hybrid", null, 20));

		// The number of zoom level is a mix of 20 - 22, depending on the location, OpenLayers do not support that very well...
		layerCatalog.addLayer(this.createGoogleLayer(dataSourceConfig, "SATELLITE", "Google Satellite", null, 20));

		return layerCatalog;
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

		this.ensureUniqueLayerId(layerConfig, dataSourceConfig);

		return layerConfig;
	}



}
