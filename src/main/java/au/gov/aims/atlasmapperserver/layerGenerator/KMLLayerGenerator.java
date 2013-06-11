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

import au.gov.aims.atlasmapperserver.dataSourceConfig.KMLDataSourceConfig;
import au.gov.aims.atlasmapperserver.layerConfig.KMLLayerConfig;
import au.gov.aims.atlasmapperserver.layerConfig.LayerCatalog;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class KMLLayerGenerator extends AbstractLayerGenerator<KMLLayerConfig, KMLDataSourceConfig> {
	/**
	 * We thrust the Admin to choose Unique IDs for all it's KMLs. Nothing to do here.
	 * @param layer
	 * @param dataSourceConfig
	 * @return
	 */
	@Override
	protected String getUniqueLayerId(KMLLayerConfig layer, KMLDataSourceConfig dataSourceConfig) {
		return layer.getLayerId();
	}

	/**
	 * @param dataSourceConfig
	 * @param harvest
	 * @return
	 * @throws Exception
	 * NOTE: Harvest is ignored since there is nothing to harvest.
	 */
	@Override
	public LayerCatalog generateLayerCatalog(KMLDataSourceConfig dataSourceConfig, boolean clearCapabilitiesCache, boolean clearMetadataCache) throws Exception {
		LayerCatalog layerCatalog = new LayerCatalog();

		JSONArray kmlData = dataSourceConfig.getKmlData();
		if (kmlData != null && kmlData.length() > 0) {
			for (int i=0, len=kmlData.length(); i<len; i++) {
				JSONObject kmlInfo = kmlData.optJSONObject(i);
				if (kmlInfo != null) {
					KMLLayerConfig layer = new KMLLayerConfig(dataSourceConfig.getConfigManager());
					layer.setLayerId(kmlInfo.optString("id", null));
					layer.setKmlUrl(kmlInfo.optString("url", null));
					layer.setTitle(kmlInfo.optString("title", null));

					String description = kmlInfo.optString("description", null);
					if (description != null) {
						layer.setDescription(description);
						layer.setDescriptionFormat("wiki");
					}

					dataSourceConfig.bindLayer(layer);
					// Do not call ensure unique layer ID, we thrust the admin to choose unique ID.
					//this.ensureUniqueLayerId(layer, dataSourceConfig);

					layerCatalog.addLayer(layer);
				}
			}
		}

		return layerCatalog;
	}
}
