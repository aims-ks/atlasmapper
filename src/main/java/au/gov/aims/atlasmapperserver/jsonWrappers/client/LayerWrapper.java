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

import au.gov.aims.atlasmapperserver.jsonWrappers.AbstractWrapper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONSortedObject;

/**
 * This class is wrapping a JSONObject representing a layer.
 * It has been made to manage the Json keys in one location and simplify maintenance.
 */
public class LayerWrapper extends AbstractWrapper {
	public LayerWrapper() { super(); }
	public LayerWrapper(JSONObject json) { super(json); }

	public Double getVersion() {
		return this.getVersion(null);
	}
	public Double getVersion(Double defaultValue) {
		if (this.json.isNull("version")) {
			return defaultValue;
		}
		return this.json.optDouble("version");
	}
	public void setVersion(Double version) throws JSONException {
		if (version == null && !this.json.isNull("version")) {
			this.json.remove("version");
		} else {
			this.json.put("version", version);
		}
	}

	// TODO Remove (the ID is the key of the JSONObject)
	public String getLayerId() {
		return this.json.optString("layerId", null);
	}
	public void setLayerId(String layerId) throws JSONException {
		this.json.put("layerId", layerId);
	}

	public String getDataSourceId() {
		return this.json.optString("dataSourceId", null);
	}
	public void setDataSourceId(String dataSourceId) throws JSONException {
		this.json.put("dataSourceId", dataSourceId);
	}

	// Only used when a layer override the data source name, to put a layer in a different root folder.
	// This option is quite useful to regroup data sources that provide only 1 layer.
	public String getDataSourceName() {
		return this.json.optString("dataSourceName", null);
	}
	public void setDataSourceName(String dataSourceName) throws JSONException {
		this.json.put("dataSourceName", dataSourceName);
	}

	public String getLayerType() {
		return this.getLayerType(null);
	}
	public String getLayerType(String defaultValue) {
		return this.json.optString("layerType",
			// Backward compatibility
			this.json.optString("dataSourceType", defaultValue));
	}
	public void setLayerType(String layerType) throws JSONException {
		this.json.put("layerType", layerType);
	}

	public String getServiceUrl() {
		return this.json.optString("serviceUrl", null);
	}
	public void setServiceUrl(String serviceUrl) throws JSONException {
		this.json.put("serviceUrl", serviceUrl);
	}

	// TODO delete after implementing Save State
	public Boolean isSelected() {
		return this.isSelected(null);
	}
	public Boolean isSelected(Boolean defaultValue) {
		if (this.json.isNull("selected")) {
			return defaultValue;
		}
		return this.json.optBoolean("selected");
	}
	public void setSelected(Boolean selected) throws JSONException {
		this.setValue("selected", selected);
	}

	public String getFeatureRequestsUrl() {
		return this.json.optString("featureRequestsUrl", null);
	}
	public void setFeatureRequestsUrl(String featureRequestsUrl) throws JSONException {
		this.json.put("featureRequestsUrl", featureRequestsUrl);
	}

	public String getLayerName() {
		return this.json.optString("layerName", null);
	}
	public void setLayerName(String layerName) throws JSONException {
		this.json.put("layerName", layerName);
	}

	public JSONArray getLayerBoundingBox() {
		return this.json.optJSONArray("layerBoundingBox");
	}
	public void setLayerBoundingBox(double[] layerBoundingBox) throws JSONException {
		if (layerBoundingBox != null) {
			JSONArray bboxArray = new JSONArray();
			for (double value : layerBoundingBox) {
				bboxArray.put(value);
			}
			this.setLayerBoundingBox(bboxArray);
		}
	}
	public void setLayerBoundingBox(JSONArray layerBoundingBox) throws JSONException {
		this.json.put("layerBoundingBox", layerBoundingBox);
	}

	public String getProjection() {
		return this.json.optString("projection", null);
	}
	public void setProjection(String projection) throws JSONException {
		this.json.put("projection", projection);
	}

	public String getTitle() {
		return this.getTitle(null);
	}
	public String getTitle(String defaultValue) {
		return this.json.optString("title", defaultValue);
	}
	public void setTitle(String title) throws JSONException {
		this.json.put("title", title);
	}

	public String getDescription() {
		return this.json.optString("description", null);
	}
	public void setDescription(String description) throws JSONException {
		this.json.put("description", description);
	}

	// text, wiki, html
	public String getDescriptionFormat() {
		return this.json.optString("descriptionFormat", null);
	}
	public void setDescriptionFormat(String descriptionFormat) throws JSONException {
		this.json.put("descriptionFormat", descriptionFormat);
	}

	// Remove HTML
	public String getTextDescription() {
		String description = this.getDescription();
		if ("html".equalsIgnoreCase(this.getDescriptionFormat())) {
			description.replaceAll("\\<.*?\\>","");
		}
		// TODO Remove wiki format??
		return description;
	}

	public String getSystemDescription() {
		return this.json.optString("systemDescription", null);
	}
	public void setSystemDescription(String systemDescription) throws JSONException {
		this.json.put("systemDescription", systemDescription);
	}

	public JSONSortedObject getDownloadLinks() {
		return new JSONSortedObject(this.json.optJSONObject("downloadLinks"));
	}
	public void setDownloadLinks(JSONSortedObject downloadLinks) throws JSONException {
		this.json.put("downloadLinks", downloadLinks);
	}

	public Boolean isShownOnlyInLayerGroup() {
		return this.isShownOnlyInLayerGroup(null);
	}
	public Boolean isShownOnlyInLayerGroup(Boolean defaultValue) {
		if (this.json.isNull("shownOnlyInLayerGroup")) {
			return defaultValue;
		}
		return this.json.optBoolean("shownOnlyInLayerGroup");
	}
	public void setShownOnlyInLayerGroup(Boolean shownOnlyInLayerGroup) throws JSONException {
		if (shownOnlyInLayerGroup == null && !this.json.isNull("shownOnlyInLayerGroup")) {
			this.json.remove("shownOnlyInLayerGroup");
		} else {
			this.json.put("shownOnlyInLayerGroup", shownOnlyInLayerGroup);
		}
	}

	public Boolean isIsBaseLayer() {
		return this.isIsBaseLayer(null);
	}
	public Boolean isIsBaseLayer(Boolean defaultValue) {
		if (this.json.isNull("isBaseLayer")) {
			return defaultValue;
		}
		return this.json.optBoolean("isBaseLayer");
	}
	public void setIsBaseLayer(Boolean isBaseLayer) throws JSONException {
		if (isBaseLayer == null && !this.json.isNull("isBaseLayer")) {
			this.json.remove("isBaseLayer");
		} else {
			this.json.put("isBaseLayer", isBaseLayer);
		}
	}

	public Boolean isHasLegend() {
		return this.isHasLegend(null);
	}
	public Boolean isHasLegend(Boolean defaultValue) {
		if (this.json.isNull("hasLegend")) {
			return defaultValue;
		}
		return this.json.optBoolean("hasLegend");
	}
	public void setHasLegend(Boolean hasLegend) throws JSONException {
		if (hasLegend == null && !this.json.isNull("hasLegend")) {
			this.json.remove("hasLegend");
		} else {
			this.json.put("hasLegend", hasLegend);
		}
	}

	public String getLegendUrl() {
		return this.json.optString("legendUrl", null);
	}
	public void setLegendUrl(String legendUrl) throws JSONException {
		this.json.put("legendUrl", legendUrl);
	}

	public String getLegendGroup() {
		return this.json.optString("legendGroup", null);
	}
	public void setLegendGroup(String legendGroup) throws JSONException {
		this.json.put("legendGroup", legendGroup);
	}

	public String getLegendTitle() {
		return this.json.optString("legendTitle", null);
	}
	public void setLegendTitle(String legendTitle) throws JSONException {
		this.json.put("legendTitle", legendTitle);
	}

	public String getStylesUrl() {
		return this.json.optString("stylesUrl", null);
	}
	public void setStylesUrl(String stylesUrl) throws JSONException {
		this.json.put("stylesUrl", stylesUrl);
	}

	public JSONObject getStyles() {
		return this.json.optJSONObject("styles");
	}
	public void setStyles(JSONObject styles) throws JSONException {
		this.json.put("styles", styles);
	}

	public JSONArray getInfoHtmlUrls() {
		return this.json.optJSONArray("infoHtmlUrls");
	}
	public void setInfoHtmlUrls(String[] infoHtmlUrls) throws JSONException {
		if (infoHtmlUrls != null) {
			this.setInfoHtmlUrls(new JSONArray(infoHtmlUrls));
		}
	}
	public void setInfoHtmlUrls(JSONArray infoHtmlUrls) throws JSONException {
		this.json.put("infoHtmlUrls", infoHtmlUrls);
	}

	public JSONArray getAliasIds() {
		return this.json.optJSONArray("aliasIds");
	}
	public void setAliasIds(String[] aliasIds) throws JSONException {
		if (aliasIds != null) {
			this.setAliasIds(new JSONArray(aliasIds));
		}
	}
	public void setAliasIds(JSONArray aliasIds) throws JSONException {
		this.json.put("aliasIds", aliasIds);
	}

	public Boolean isCached() {
		return this.isCached(null);
	}
	public Boolean isCached(Boolean defaultValue) {
		if (this.json.isNull("cached")) {
			return defaultValue;
		}
		return this.json.optBoolean("cached");
	}
	public void setCached(Boolean cached) throws JSONException {
		if (cached == null && !this.json.isNull("cached")) {
			this.json.remove("cached");
		} else {
			this.json.put("cached", cached);
		}
	}

	public JSONObject getOlParams() {
		return this.json.optJSONObject("olParams");
	}
	public void setOlParams(JSONObject olParams) throws JSONException {
		this.json.put("olParams", olParams);
	}

	public JSONObject getOlOptions() {
		return this.json.optJSONObject("olOptions");
	}
	public void setOlOptions(JSONObject olOptions) throws JSONException {
		this.json.put("olOptions", olOptions);
	}

	/**
	 * Extra fields to add in the layer options panel, like CQL filter, time, depth, etc.
	 * Usually used only with layer overrides.
	 * @return
	 */
	public JSONArray getOptions() {
		return this.json.optJSONArray("options");
	}
	public void setOptions(JSONArray options) throws JSONException {
		this.json.put("options", options);
	}

	public String getTreePath() {
		return this.json.optString("treePath", null);
	}
	public void setTreePath(String treePath) throws JSONException {
		this.json.put("treePath", treePath);
	}

	// Group layers (and ArcGIS special group of grouped layers called "Service")
	public boolean isGroup() {
		return "GROUP".equals(this.getLayerType()) || "SERVICE".equals(this.getLayerType());
	}

	public JSONArray getLayers() {
		return this.json.optJSONArray("layers");
	}
	public void setLayers(String[] layers) throws JSONException {
		if (layers != null) {
			this.setLayers(new JSONArray(layers));
		}
	}
	public void setLayers(JSONArray layers) throws JSONException {
		this.json.put("layers", layers);
	}

	// KML layers
	public String getKmlUrl() {
		return this.json.optString("kmlUrl", null);
	}
	public void setKmlUrl(String kmlUrl) throws JSONException {
		this.json.put("kmlUrl", kmlUrl);
	}

	// XYZ layers
	public String getFormat() {
		return this.json.optString("format", null);
	}
	public void setFormat(String format) throws JSONException {
		this.json.put("format", format);
	}

	// ArcGIS layers
	public String getArcGISPath() {
		return this.json.optString("arcGISPath", null);
	}
	public void setArcGISPath(String arcGISPath) throws JSONException {
		this.json.put("arcGISPath", arcGISPath);
	}

	public Boolean isForcePNG24() {
		return this.isForcePNG24(null);
	}
	public Boolean isForcePNG24(Boolean defaultValue) {
		if (this.json.isNull("forcePNG24")) {
			return defaultValue;
		}
		return this.json.optBoolean("forcePNG24");
	}
	public void setForcePNG24(Boolean forcePNG24) throws JSONException {
		if (forcePNG24 == null && !this.json.isNull("forcePNG24")) {
			this.json.remove("forcePNG24");
		} else {
			this.json.put("forcePNG24", forcePNG24);
		}
	}

	public Integer isArcGISCacheTileCols() {
		return this.isArcGISCacheTileCols(null);
	}
	public Integer isArcGISCacheTileCols(Integer defaultValue) {
		if (this.json.isNull("arcGISCacheTileCols")) {
			return defaultValue;
		}
		return this.json.optInt("arcGISCacheTileCols");
	}
	public void setArcGISCacheTileCols(Integer arcGISCacheTileCols) throws JSONException {
		if (arcGISCacheTileCols == null && !this.json.isNull("arcGISCacheTileCols")) {
			this.json.remove("arcGISCacheTileCols");
		} else {
			this.json.put("arcGISCacheTileCols", arcGISCacheTileCols);
		}
	}

	public Integer isArcGISCacheTileRows() {
		return this.isArcGISCacheTileRows(null);
	}
	public Integer isArcGISCacheTileRows(Integer defaultValue) {
		if (this.json.isNull("arcGISCacheTileRows")) {
			return defaultValue;
		}
		return this.json.optInt("arcGISCacheTileRows");
	}
	public void setArcGISCacheTileRows(Integer arcGISCacheTileRows) throws JSONException {
		if (arcGISCacheTileRows == null && !this.json.isNull("arcGISCacheTileRows")) {
			this.json.remove("arcGISCacheTileRows");
		} else {
			this.json.put("arcGISCacheTileRows", arcGISCacheTileRows);
		}
	}

	public Double isArcGISCacheTileOriginX() {
		return this.isArcGISCacheTileOriginX(null);
	}
	public Double isArcGISCacheTileOriginX(Double defaultValue) {
		if (this.json.isNull("arcGISCacheTileOriginX")) {
			return defaultValue;
		}
		return this.json.optDouble("arcGISCacheTileOriginX");
	}
	public void setArcGISCacheTileOriginX(Double arcGISCacheTileOriginX) throws JSONException {
		if (arcGISCacheTileOriginX == null && !this.json.isNull("arcGISCacheTileOriginX")) {
			this.json.remove("arcGISCacheTileOriginX");
		} else {
			this.json.put("arcGISCacheTileOriginX", arcGISCacheTileOriginX);
		}
	}

	public Double isArcGISCacheTileOriginY() {
		return this.isArcGISCacheTileOriginY(null);
	}
	public Double isArcGISCacheTileOriginY(Double defaultValue) {
		if (this.json.isNull("arcGISCacheTileOriginY")) {
			return defaultValue;
		}
		return this.json.optDouble("arcGISCacheTileOriginY");
	}
	public void setArcGISCacheTileOriginY(Double arcGISCacheTileOriginY) throws JSONException {
		if (arcGISCacheTileOriginY == null && !this.json.isNull("arcGISCacheTileOriginY")) {
			this.json.remove("arcGISCacheTileOriginY");
		} else {
			this.json.put("arcGISCacheTileOriginY", arcGISCacheTileOriginY);
		}
	}

	public JSONArray getArcGISCacheTileResolutions() {
		return this.json.optJSONArray("arcGISCacheTileResolutions");
	}
	public void setArcGISCacheTileResolutions(Double[] arcGISCacheTileResolutions) throws JSONException {
		if (arcGISCacheTileResolutions != null) {
			this.setArcGISCacheTileResolutions(new JSONArray(arcGISCacheTileResolutions));
		}
	}
	public void setArcGISCacheTileResolutions(JSONArray arcGISCacheTileResolutions) throws JSONException {
		this.json.put("arcGISCacheTileResolutions", arcGISCacheTileResolutions);
	}

	// WMS layers
	public String getWebCacheUrl() {
		return this.json.optString("webCacheUrl", null);
	}
	public void setWebCacheUrl(String webCacheUrl) throws JSONException {
		this.json.put("webCacheUrl", webCacheUrl);
	}

	public JSONArray getWebCacheSupportedParameters() {
		return this.json.optJSONArray("webCacheSupportedParameters");
	}
	public void setWebCacheSupportedParameters(JSONArray webCacheSupportedParameters) throws JSONException {
		this.json.put("webCacheSupportedParameters", webCacheSupportedParameters);
	}

	public String getWmsVersion() {
		return this.json.optString("wmsVersion", null);
	}
	public void setWmsVersion(String wmsVersion) throws JSONException {
		this.json.put("wmsVersion", wmsVersion);
	}

	public Boolean isWmsQueryable() {
		return this.isWmsQueryable(null);
	}
	public Boolean isWmsQueryable(Boolean defaultValue) {
		if (this.json.isNull("wmsQueryable")) {
			return defaultValue;
		}
		return this.json.optBoolean("wmsQueryable");
	}
	public void setWmsQueryable(Boolean wmsQueryable) throws JSONException {
		if (wmsQueryable == null && !this.json.isNull("wmsQueryable")) {
			this.json.remove("wmsQueryable");
		} else {
			this.json.put("wmsQueryable", wmsQueryable);
		}
	}

	public String getExtraWmsServiceUrls() {
		return this.json.optString("extraWmsServiceUrls", null);
	}
	public void setExtraWmsServiceUrls(String extraWmsServiceUrls) throws JSONException {
		this.json.put("extraWmsServiceUrls", extraWmsServiceUrls);
	}

	public String getWmsRequestMimeType() {
		return this.json.optString("wmsRequestMimeType", null);
	}
	public void setWmsRequestMimeType(String wmsRequestMimeType) throws JSONException {
		this.json.put("wmsRequestMimeType", wmsRequestMimeType);
	}

	public JSONArray getWmsFeatureRequestLayers() {
		return this.json.optJSONArray("wmsFeatureRequestLayers");
	}
	public void setWmsFeatureRequestLayers(String[] layers) throws JSONException {
		if (layers != null) {
			this.setWmsFeatureRequestLayers(new JSONArray(layers));
		}
	}
	public void setWmsFeatureRequestLayers(JSONArray wmsFeatureRequestLayers) throws JSONException {
		this.json.put("wmsFeatureRequestLayers", wmsFeatureRequestLayers);
	}

	public Boolean isWmsTransectable() {
		return this.isWmsTransectable(null);
	}
	public Boolean isWmsTransectable(Boolean defaultValue) {
		if (this.json.isNull("wmsTransectable")) {
			return defaultValue;
		}
		return this.json.optBoolean("wmsTransectable");
	}
	public void setWmsTransectable(Boolean wmsTransectable) throws JSONException {
		if (wmsTransectable == null && !this.json.isNull("wmsTransectable")) {
			this.json.remove("wmsTransectable");
		} else {
			this.json.put("wmsTransectable", wmsTransectable);
		}
	}
}
