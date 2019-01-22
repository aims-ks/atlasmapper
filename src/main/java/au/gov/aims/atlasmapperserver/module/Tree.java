/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2011 Australian Institute of Marine Science
 *
 *  Contact: Gael Lafond <g.lafond@aims.gov.au>
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

import java.util.Iterator;
import java.util.Map;

import au.gov.aims.atlasmapperserver.jsonWrappers.client.DataSourceWrapper;
import au.gov.aims.atlasmapperserver.jsonWrappers.client.LayerWrapper;
import org.json.JSONArray;
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
        return 2.0;
    }

    @Override
    public JSONObject getJSONConfiguration(ClientConfig clientConfig, DataSourceWrapper layerCatalog, Map<String, DataSourceWrapper> dataSources) throws JSONException {
        try {
            if (dataSources != null) {
                JSONObject treeConfig = new JSONObject();
                if (layerCatalog != null) {
                    JSONObject layers = layerCatalog.getLayers();
                    if (layers != null) {
                        Iterator<String> layerIds = layers.keys();
                        while (layerIds.hasNext()) {
                            String layerId = layerIds.next();
                            if (!layers.isNull(layerId)) {
                                JSONObject jsonLayer = layers.optJSONObject(layerId);
                                if (jsonLayer != null) {
                                    LayerWrapper layer = new LayerWrapper(jsonLayer);
                                    if (!layer.isShownOnlyInLayerGroup(false)) {
                                        this.addLayer(
                                                treeConfig,
                                                clientConfig,
                                                dataSources.get(layer.getDataSourceId()),
                                                layerId,
                                                layer);
                                    }
                                }
                            }
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
            DataSourceWrapper dataSourceWrapper,
            String layerId,
            LayerWrapper layer) throws JSONException {

        JSONObject currentBranch = treeRoot;

        // Find the data source tab OR Base Layer tab
        boolean isBaseLayer = layer.isIsBaseLayer(false);
        if (clientConfig.isBaseLayersInTab() && isBaseLayer) {
            currentBranch = this.getTreeBranch(currentBranch, BASE_LAYERS_TAB_LABEL);
        } else {
            currentBranch = this.getTreeBranch(currentBranch, OVERLAY_LAYERS_TAB_LABEL);

            // Find the tree root
            // First check if the tree root has been overwritten in the layer config.
            String manualTreeRoot = layer.getTreeRoot();
            if (Utils.isBlank(manualTreeRoot) && dataSourceWrapper != null) {
                // Get the tree root from the data source config.
                manualTreeRoot = dataSourceWrapper.getTreeRoot();
            }

            if (Utils.isNotBlank(manualTreeRoot)) {
                currentBranch = this.getTreeBranch(currentBranch, manualTreeRoot);
            } else {
                // There is no tree root defined. Use the data source name.
                // First check if the data source name has been overwritten in the layer config.
                String dataSourceName = layer.getDataSourceName();
                if (Utils.isBlank(dataSourceName) && dataSourceWrapper != null) {
                    // Get the data source name from the data source config.
                    dataSourceName = dataSourceWrapper.getDataSourceName();
                }
                if (Utils.isNotBlank(dataSourceName)) {
                    currentBranch = this.getTreeBranch(currentBranch, dataSourceName);
                }
            }
        }

        // Find the right folder in the tab
        String treePath = layer.getTreePath();
        if (Utils.isNotBlank(treePath)) {
            String[] treePathParts = treePath.split("/");
            if (treePathParts != null) {
                for (int i=0; i<treePathParts.length; i++) {
                    currentBranch = this.getTreeBranch(currentBranch, treePathParts[i]);
                }
            }
        }

        currentBranch.put(layerId, this.getTreeLeaf(layer));
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

    private JSONObject getTreeLeaf(LayerWrapper layer) throws JSONException {
        JSONObject leaf = new JSONObject();
        String title = layer.getTitle();
        if (Utils.isNotBlank(title)) {
            leaf.put("title", title);
        }
        JSONArray bbox = layer.getLayerBoundingBox();
        if (bbox != null) {
            leaf.put("bbox", bbox);
        }

        return leaf;
    }
}
