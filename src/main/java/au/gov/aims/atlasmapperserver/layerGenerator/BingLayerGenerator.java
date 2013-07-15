/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2012 Australian Institute of Marine Science
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

import au.gov.aims.atlasmapperserver.dataSourceConfig.BingDataSourceConfig;
import au.gov.aims.atlasmapperserver.layerConfig.BingLayerConfig;
import au.gov.aims.atlasmapperserver.layerConfig.LayerCatalog;

public class BingLayerGenerator extends AbstractLayerGenerator<BingLayerConfig, BingDataSourceConfig> {
	/**
	 * The number of Bing Layers is fix and they already have unique IDs. Nothing to do here.
	 * @param layer
	 * @param dataSourceConfig
	 * @return
	 */
	@Override
	protected String getUniqueLayerId(BingLayerConfig layer, BingDataSourceConfig dataSourceConfig) {
		return layer.getLayerId();
	}

	/**
	 * Create and return the 3 Bing layers.
	 *     * Bing Road
	 *     * Bing Hybrid
	 *     * Bing Aerial
	 * @return
	 * NOTE: Harvest is ignored since there is nothing to harvest.
	 */
	@Override
	public LayerCatalog generateRawLayerCatalog(BingDataSourceConfig dataSourceConfig, boolean redownloadPrimaryFiles, boolean redownloadSecondaryFiles) {
		LayerCatalog layerCatalog = new LayerCatalog();

		layerCatalog.addLayer(this.createBingLayer(dataSourceConfig, "Road", "Bing Road", null));
		layerCatalog.addLayer(this.createBingLayer(dataSourceConfig, "AerialWithLabels", "Bing Hybrid", null));
		layerCatalog.addLayer(this.createBingLayer(dataSourceConfig, "Aerial", "Bing Aerial", null));

		return layerCatalog;
	}

	private BingLayerConfig createBingLayer(BingDataSourceConfig dataSourceConfig, String bingLayerType, String name, String description) {
		BingLayerConfig layerConfig = new BingLayerConfig(dataSourceConfig.getConfigManager());

		layerConfig.setLayerId(bingLayerType);
		layerConfig.setTitle(name);
		layerConfig.setDescription(description);
		layerConfig.setIsBaseLayer(true);
		layerConfig.setLayerBoundingBox(new double[]{-180, -90, 180, 90});

		this.ensureUniqueLayerId(layerConfig, dataSourceConfig);

		return layerConfig;
	}
}
