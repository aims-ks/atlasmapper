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
import au.gov.aims.atlasmapperserver.dataSourceConfig.KMLDataSourceConfig;
import au.gov.aims.atlasmapperserver.Utils;
import au.gov.aims.atlasmapperserver.layerConfig.KMLLayerConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

	@Override
	public Map<String, KMLLayerConfig> generateLayerConfigs(ClientConfig clientConfig, KMLDataSourceConfig dataSourceConfig) throws Exception {
		Map<String, KMLLayerConfig> layersConfig = null;

		Set<String> _kmlUrlsSet = dataSourceConfig.getKmlUrlsSet();
		if (_kmlUrlsSet != null && !_kmlUrlsSet.isEmpty()) {
			for (String kmlUrl : _kmlUrlsSet) {
				if (Utils.isNotBlank(kmlUrl)) {
					int layerIdStart = kmlUrl.lastIndexOf('/')+1;
					int layerIdEnd = kmlUrl.lastIndexOf('.');
					layerIdEnd = (layerIdEnd > 0 ? layerIdEnd : kmlUrl.length());

					String layerId = kmlUrl.substring(layerIdStart, layerIdEnd);

					KMLLayerConfig layer = new KMLLayerConfig(dataSourceConfig.getConfigManager());
					layer.setDataSourceId(dataSourceConfig.getDataSourceId());
					layer.setLayerId(layerId);
					layer.setTitle(layerId);
					layer.setKmlUrl(kmlUrl);

					if (layersConfig == null) {
						layersConfig = new HashMap<String, KMLLayerConfig>();
					}

					this.ensureUniqueLayerId(layer, dataSourceConfig);

					layersConfig.put(layer.getLayerId(), layer);
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
