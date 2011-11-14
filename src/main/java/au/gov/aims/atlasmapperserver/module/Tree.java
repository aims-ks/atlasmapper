/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.gov.aims.atlasmapperserver.module;

import au.gov.aims.atlasmapperserver.ClientConfig;
import au.gov.aims.atlasmapperserver.DatasourceConfig;
import au.gov.aims.atlasmapperserver.LayerConfig;
import au.gov.aims.atlasmapperserver.Utils;
import au.gov.aims.atlasmapperserver.annotation.Module;
import java.util.List;
import java.util.Map;
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
	private static final String BASE_LAYER_TAB_LABEL = "Base layers";

	@Override
	public JSONObject getJSONConfiguration(ClientConfig clientConfig) throws JSONException {
		try {
			List<DatasourceConfig> datasources = clientConfig.getDatasourceConfigs(clientConfig.getConfigManager());
			if (datasources != null) {
				JSONObject treeConfig = new JSONObject();
				for (DatasourceConfig datasourceConfig : datasources) {
					Map<String, LayerConfig> layers = datasourceConfig.getLayerConfigs(clientConfig);

					if (layers != null) {
						for (LayerConfig layerConfig : layers.values()) {
							this.addLayer(treeConfig, clientConfig, datasourceConfig, layerConfig);
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

	private void addLayer(
			JSONObject treeRoot,
			ClientConfig clientConfig,
			DatasourceConfig datasourceConfig,
			LayerConfig layerConfig) throws JSONException {

		JSONObject currentBranch = treeRoot;

		// Find the Datasource tab OR Base Layer tab
		if (clientConfig.isBaseLayersInTab() && layerConfig.isIsBaseLayer() != null && layerConfig.isIsBaseLayer()) {
			currentBranch = this.getTreeBranch(currentBranch, BASE_LAYER_TAB_LABEL);
		} else {
			if (datasourceConfig != null) {
				currentBranch = this.getTreeBranch(currentBranch, datasourceConfig.getDatasourceName());
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
	private String getTreeLeaf(LayerConfig layerConfig) {
		String layerTitle = layerConfig.getTitle();
		if (layerTitle == null) {
			layerTitle = layerConfig.getLayerId();
		}
		return layerTitle;
	}
}
