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
import au.gov.aims.atlasmapperserver.Utils;
import au.gov.aims.atlasmapperserver.layerConfig.KMLLayerConfig;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class KMLLayerGenerator extends AbstractLayerGenerator<KMLLayerConfig, KMLDataSourceConfig> {

	public KMLLayerGenerator(KMLDataSourceConfig dataSource) {
		super(dataSource);
	}

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

	@Override
	public Collection<KMLLayerConfig> generateLayerConfigs(KMLDataSourceConfig dataSourceConfig) throws Exception {
		List<KMLLayerConfig> layersConfig = null;

		/*
		Set<String> _kmlUrlsSet = dataSourceConfig.getKmlUrlsSet();
		if (_kmlUrlsSet != null && !_kmlUrlsSet.isEmpty()) {
			for (String kmlUrl : _kmlUrlsSet) {
				if (Utils.isNotBlank(kmlUrl)) {
					int layerIdStart = kmlUrl.lastIndexOf('/')+1;
					int layerIdEnd = kmlUrl.lastIndexOf('.');
					layerIdEnd = (layerIdEnd > 0 ? layerIdEnd : kmlUrl.length());

					String layerId = kmlUrl.substring(layerIdStart, layerIdEnd);

					KMLLayerConfig layer = new KMLLayerConfig(dataSourceConfig.getConfigManager());
					layer.setLayerId(layerId);
					layer.setTitle(layerId);
					layer.setKmlUrl(kmlUrl);

					if (layersConfig == null) {
						layersConfig = new ArrayList<KMLLayerConfig>();
					}

					dataSourceConfig.bindLayer(layer);
					this.ensureUniqueLayerId(layer, dataSourceConfig);

					layersConfig.add(layer);
				}
			}
		}
		*/

		JSONArray kmlDatas = dataSourceConfig.getKmlDatas();
		if (kmlDatas != null && kmlDatas.length() > 0) {
			for (int i=0, len=kmlDatas.length(); i<len; i++) {
				JSONObject kmlData = kmlDatas.optJSONObject(i);
				if (kmlData != null) {
					KMLLayerConfig layer = new KMLLayerConfig(dataSourceConfig.getConfigManager());
					layer.setLayerId(kmlData.optString("id", null));
					layer.setKmlUrl(kmlData.optString("url", null));
					layer.setTitle(kmlData.optString("title", null));

					String description = kmlData.optString("description", null);
					if (description != null) {
						layer.setDescription(description);
						layer.setDescriptionFormat("wiki");
					}

					if (layersConfig == null) {
						layersConfig = new ArrayList<KMLLayerConfig>();
					}

					dataSourceConfig.bindLayer(layer);
					//this.ensureUniqueLayerId(layer, dataSourceConfig);

					layersConfig.add(layer);
				}
			}
		}

		return layersConfig;
	}

	@Override
	public KMLDataSourceConfig applyOverrides(KMLDataSourceConfig dataSourceConfig) {
		return dataSourceConfig;
	}
}
