/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2012 Australian Institute of Marine Science
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
package au.gov.aims.atlasmapperserver.jsonWrappers.client;

import au.gov.aims.atlasmapperserver.AbstractConfig;
import au.gov.aims.atlasmapperserver.Utils;
import au.gov.aims.atlasmapperserver.jsonWrappers.AbstractWrapper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.util.Iterator;
import java.util.Map;

/**
 * This class is wrapping a JSONObject representing a data source to be sent to the AtlasMapper client.
 * It has been made to manage the Json keys in one location and simplify maintenance.
 */
public class DataSourceWrapper extends AbstractWrapper {
    public DataSourceWrapper() { super(); }
    public DataSourceWrapper(JSONObject json) { super(json); }

    public Integer getId() {
        if (this.json.isNull("id")) {
            return null;
        }
        return this.json.optInt("id");
    }
    public void setId(Integer id) throws JSONException {
        this.setValue("id", id);
    }

    public String getDataSourceId() {
        return this.json.optString("dataSourceId", null);
    }
    public void setDataSourceId(String dataSourceId) throws JSONException {
        this.json.put("dataSourceId", dataSourceId);
    }

    public String getDataSourceName() {
        return this.json.optString("dataSourceName", null);
    }
    public void setDataSourceName(String dataSourceName) throws JSONException {
        this.json.put("dataSourceName", dataSourceName);
    }

    public String getLayerType() {
        return this.json.optString("layerType",
            // Backward compatibility
            this.json.optString("dataSourceType", null));
    }
    public void setLayerType(String layerType) throws JSONException {
        this.json.put("layerType", layerType);
    }

    public String getServiceUrl() {
        return this.json.optString("serviceUrl", null);
    }
    public void setServiceUrl(URL serviceUrl) throws JSONException {
        this.setServiceUrl(serviceUrl == null ? null : serviceUrl.toString());
    }
    public void setServiceUrl(String serviceUrl) throws JSONException {
        this.json.put("serviceUrl", serviceUrl);
    }

    public String getFeatureRequestsUrl() {
        return this.json.optString("featureRequestsUrl", null);
    }
    public void setFeatureRequestsUrl(URL featureRequestsUrl) throws JSONException {
        this.setFeatureRequestsUrl(featureRequestsUrl == null ? null : featureRequestsUrl.toString());
    }
    public void setFeatureRequestsUrl(String featureRequestsUrl) throws JSONException {
        this.json.put("featureRequestsUrl", featureRequestsUrl);
    }

    public String getLegendUrl() {
        return this.json.optString("legendUrl", null);
    }
    public void setLegendUrl(URL legendUrl) throws JSONException {
        this.setLegendUrl(legendUrl == null ? null : legendUrl.toString());
    }
    public void setLegendUrl(String legendUrl) throws JSONException {
        this.json.put("legendUrl", legendUrl);
    }

    public String getWmsVersion() {
        return this.json.optString("wmsVersion", null);
    }
    public void setWmsVersion(String wmsVersion) throws JSONException {
        this.json.put("wmsVersion", wmsVersion);
    }

    // Used only on server side, the value is removed before saving the data source save state.
    public String getGetMapUrl() {
        return this.json.optString("getMapUrl", null);
    }
    public void setGetMapUrl(String getMapUrl) throws JSONException {
        this.setValue("getMapUrl", getMapUrl);
    }

    public String getCacheWmsVersion() {
        return this.json.optString("cacheWmsVersion", null);
    }
    public void setCacheWmsVersion(String cacheWmsVersion) throws JSONException {
        this.json.put("cacheWmsVersion", cacheWmsVersion);
    }

    public JSONObject getLayers() {
        return this.json.optJSONObject("layers");
    }
    public void setLayers(JSONObject layers) throws JSONException {
        if (layers == null) {
            this.json.remove("layers");
        } else {
            this.json.put("layers", layers);
        }
    }

    public void setLayers(Map<String, LayerWrapper> layers) throws JSONException {
        this.json.remove("layers");
        this.addLayers(layers);
    }

    public void addLayers(Map<String, LayerWrapper> layers) throws JSONException {
        if (layers != null && !layers.isEmpty()) {
            for (Map.Entry<String, LayerWrapper> layerEntry : layers.entrySet()) {
                LayerWrapper layerWrapper = layerEntry.getValue();
                String layerId = layerEntry.getKey();
                if (layerId != null && !layerId.isEmpty() && layerWrapper != null) {
                    JSONObject jsonLayer = layerWrapper.getJSON();
                    if (jsonLayer != null && jsonLayer.length() > 0) {
                        this.addLayer(layerId, jsonLayer);
                    }
                }
            }
        }
    }

    public void addLayer(String layerId, LayerWrapper layer) throws JSONException {
        this.addLayer(layerId, layer.getJSON());
    }

    public void addLayers(JSONObject newLayers) throws JSONException {
        if (newLayers != null && newLayers.length() > 0) {
            Iterator<String> layerIDsItr = newLayers.keys();
            while (layerIDsItr.hasNext()) {
                String layerId = layerIDsItr.next();
                if (!newLayers.isNull(layerId)) {
                    this.addLayer(layerId, newLayers.optJSONObject(layerId));
                }
            }
        }
    }

    private void addLayer(String layerId, JSONObject layer) throws JSONException {
        JSONObject layers = this.getLayers();
        if (layers == null) {
            layers = new JSONObject();
            this.setLayers(layers);
        }
        layers.put(layerId, layer);
    }

    public String getGlobalManualOverride() {
        return this.json.optString("globalManualOverride", null);
    }
    public void setGlobalManualOverride(String globalManualOverride) throws JSONException {
        this.setValue("globalManualOverride", globalManualOverride);
    }

    public String getBlackAndWhiteListedLayers() {
        return this.json.optString("blackAndWhiteListedLayers", null);
    }
    public void setBlackAndWhiteListedLayers(String blackAndWhiteListedLayers) throws JSONException {
        this.setValue("blackAndWhiteListedLayers", blackAndWhiteListedLayers);
    }

    public String getGoogleJavaScript() {
        return this.json.optString("googleJavaScript", null);
    }
    public void setGoogleJavaScript(String googleJavaScript) throws JSONException {
        this.setValue("googleJavaScript", googleJavaScript);
    }

    public String getGoogleAPIKey() {
        return this.json.optString("googleAPIKey", null);
    }
    public void setGoogleAPIKey(String googleAPIKey) throws JSONException {
        this.setValue("googleAPIKey", googleAPIKey);
    }

    public JSONArray getWebCacheSupportedParameters() {
        return this.json.optJSONArray("webCacheSupportedParameters");
    }
    public void setWebCacheSupportedParameters(JSONArray webCacheSupportedParameters) throws JSONException {
        this.setValue("webCacheSupportedParameters", webCacheSupportedParameters);
    }
    public void setWebCacheSupportedParameters(String[] webCacheSupportedParameters) throws JSONException {
        this.setValue("webCacheSupportedParameters", webCacheSupportedParameters);
    }

    public JSONArray getBaseLayers() {
        return this.json.optJSONArray("baseLayers");
    }
    public void setBaseLayers(JSONArray baseLayers) throws JSONException {
        this.setValue("baseLayers", baseLayers);
    }
    public void setBaseLayers(String[] baseLayers) throws JSONException {
        this.setValue("baseLayers", baseLayers);
    }

    public JSONArray getOverlayLayers() {
        return this.json.optJSONArray("overlayLayers");
    }
    public void setOverlayLayers(JSONArray overlayLayers) throws JSONException {
        this.setValue("overlayLayers", overlayLayers);
    }
    public void setOverlayLayers(String[] overlayLayers) throws JSONException {
        this.setValue("overlayLayers", overlayLayers);
    }

    public String getLastHarvested() {
        return this.json.optString("lastHarvested", null);
    }
    public void setLastHarvested(String lastHarvested) throws JSONException {
        this.setValue("lastHarvested", lastHarvested);
    }

    // INVALID (or null), PASSED, OKAY
    public String getStatus() {
        return this.json.optString("status", "INVALID");
    }
    public void setStatus(String status) throws JSONException {
        this.setValue("status", status);
    }

    public boolean isModified() {
        return this.json.optBoolean("modified", false);
    }
    public void setModified(boolean modified) throws JSONException {
        if (modified) {
            this.setValue("modified", true);
        } else {
            this.json.remove("modified");
        }
    }

    public String getComment() {
        return this.json.optString("comment", null);
    }
    public void setComment(String comment) throws JSONException {
        this.setValue("comment", comment);
    }

    public JSONObject getLegendParameters() {
        return this.json.optJSONObject("legendParameters");
    }
    public void setLegendParameters(String legendParametersStr) throws JSONException {
        JSONObject legendParameters = null;
        if (Utils.isNotBlank(legendParametersStr)) {
            String trimmedLegendParameters = legendParametersStr.trim();

            legendParameters = new JSONObject();
            for (String legendParameter : AbstractConfig.toSet(trimmedLegendParameters)) {
                if (Utils.isNotBlank(legendParameter)) {
                    String[] attribute = legendParameter.split(AbstractConfig.SPLIT_ATTRIBUTES_PATTERN);
                    if (attribute != null && attribute.length >= 2) {
                        legendParameters.put(
                                attribute[0].trim(),  // Key
                                attribute[1].trim()); // Value
                    }
                }
            }
        }

        this.setValue("legendParameters", legendParameters);
    }
    public void setLegendParameters(JSONObject legendParameters) throws JSONException {
        this.setValue("legendParameters", legendParameters);
    }

    public boolean isExtendWMS() {
        return this.isWMS() || this.isNCWMS();
    }
    public boolean isArcGISMapServer() {
        return "ARCGIS_MAPSERVER".equals(this.getLayerType());
    }
    public boolean isGoogle() {
        return "GOOGLE".equals(this.getLayerType());
    }
    public boolean isBing() {
        return "BING".equals(this.getLayerType());
    }
    public boolean isKML() {
        return "KML".equals(this.getLayerType());
    }
    public boolean isNCWMS() {
        return "NCWMS".equals(this.getLayerType());
    }
    public boolean isTiles() {
        return "TILES".equals(this.getLayerType());
    }
    public boolean isXYZ() {
        return "XYZ".equals(this.getLayerType());
    }
    public boolean isWMS() {
        return "WMS".equals(this.getLayerType());
    }

    public boolean isLayerCatalogEmpty() {
        JSONObject layers = this.getLayers();
        return layers == null || layers.length() <= 0;
    }

    // The data source has many attributes unneeded for the main config; they only enlarge the file.
    // It is better to remove them in this context.
    public JSONObject getMainConfigJSON() throws JSONException {
        DataSourceWrapper dataSourceClone = (DataSourceWrapper)this.clone();
        if (dataSourceClone == null) {
            // Unlikely to happen
            dataSourceClone = new DataSourceWrapper();
        }

        // Remove unwanted values
        dataSourceClone.setId(null);
        dataSourceClone.setLayers((JSONObject) null);
        dataSourceClone.setGlobalManualOverride(null);
        dataSourceClone.setBlackAndWhiteListedLayers(null);
        dataSourceClone.setLastHarvested(null);
        dataSourceClone.setComment(null);
        dataSourceClone.setGoogleAPIKey(null);
        dataSourceClone.setGoogleJavaScript(null);

        return dataSourceClone.getJSON();
    }
}
