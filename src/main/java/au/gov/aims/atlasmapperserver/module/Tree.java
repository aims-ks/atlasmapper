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

package au.gov.aims.atlasmapperserver.module;

import au.gov.aims.atlasmapperserver.ClientConfig;
import au.gov.aims.atlasmapperserver.Utils;
import au.gov.aims.atlasmapperserver.annotation.Module;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import au.gov.aims.atlasmapperserver.dataSourceConfig.AbstractDataSourceConfig;
import au.gov.aims.atlasmapperserver.layerConfig.AbstractLayerConfig;
import au.gov.aims.atlasmapperserver.layerConfig.LayerCatalog;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author glafond
 */
@Module(
	description="Display layers in a Tree structure; required by the 'Add layer' feature"
)
public class Tree extends AbstractModule {
	private static final String BASE_LAYERS_TAB_LABEL = "Base layers";
	private static final String OVERLAY_LAYERS_TAB_LABEL = "Overlay layers";

	@Override
	// NOTE: The version must match the version in the client /clientResources/amc/modules/Trees/Trees.js
	public double getVersion() {
		return 1.0;
	}

	@Override
	public JSONObject getJSONConfiguration(ClientConfig clientConfig, LayerCatalog layerCatalog) throws JSONException {
		try {
			List<AbstractDataSourceConfig> dataSourcesList = clientConfig.getDataSourceConfigs();
			if (dataSourcesList != null) {
				Map<String, AbstractDataSourceConfig> dataSourcesMap = new HashMap<String, AbstractDataSourceConfig>();
				for (AbstractDataSourceConfig dataSourceConfig : dataSourcesList) {
					dataSourcesMap.put(dataSourceConfig.getDataSourceId(), dataSourceConfig);
				}

				JSONObject treeConfig = new JSONObject();
				if (layerCatalog != null) {
					for (AbstractLayerConfig layerConfig : layerCatalog.getLayers()) {
						if (layerConfig.isShownOnlyInLayerGroup() == null || !layerConfig.isShownOnlyInLayerGroup()) {
							this.addLayer(treeConfig, clientConfig, dataSourcesMap.get(layerConfig.getDataSourceId()), layerConfig);
						}
					}
				}
				return treeConfig;
			}

			return null;
		} catch (Exception ex) {
			throw new JSONException(ex);
		}
	}

	// Add a layer to the tree
	private void addLayer(
			JSONObject treeRoot,
			ClientConfig clientConfig,
			AbstractDataSourceConfig dataSourceConfig,
			AbstractLayerConfig layerConfig) throws JSONException {

		JSONObject currentBranch = treeRoot;

		// Find the data source tab OR Base Layer tab
		if (clientConfig.isBaseLayersInTab() && layerConfig.isIsBaseLayer() != null && layerConfig.isIsBaseLayer()) {
			currentBranch = this.getTreeBranch(currentBranch, BASE_LAYERS_TAB_LABEL);
		} else {
			currentBranch = this.getTreeBranch(currentBranch, OVERLAY_LAYERS_TAB_LABEL);
			if (dataSourceConfig != null) {
				currentBranch = this.getTreeBranch(currentBranch, dataSourceConfig.getDataSourceName());
			}
		}

		// Find the right folder in the tab
		String wmsPath = layerConfig.getWmsPath();
		if (Utils.isNotBlank(wmsPath)) {
			String[] wmsPathParts = wmsPath.split("/");
			if (wmsPathParts != null) {
				for (int i=0; i<wmsPathParts.length; i++) {
					currentBranch = this.getTreeBranch(currentBranch, wmsPathParts[i]);
				}
			}
		}

		currentBranch.put(
				layerConfig.getLayerId(),
				this.getTreeLeaf(layerConfig));
	}

	private JSONObject getTreeBranch(JSONObject currentBranch, String branchName) throws JSONException {
		if (Utils.isNotBlank(branchName)) {
			if (!currentBranch.has(branchName)) {
				currentBranch.put(branchName, new JSONObject());
			}
			JSONObject foundBranch = currentBranch.optJSONObject(branchName);
			if (foundBranch != null) {
				return foundBranch;
			}
		}
		return currentBranch;
	}

	// In the future, we might want to return a more complex object.
	private String getTreeLeaf(AbstractLayerConfig layerConfig) {
		String layerTitle = layerConfig.getTitle();
		if (layerTitle == null) {
			layerTitle = layerConfig.getLayerId();
		}
		return layerTitle;
	}
}
