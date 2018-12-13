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

/**
 * This class is wrapping a JSONObject representing the a client to be sent to the AtlasMapper client.
 * It has been made to manage the Json keys in one location and simplify maintenance.
 */
public class ClientWrapper extends AbstractWrapper {
	public ClientWrapper() { super(); }
	public ClientWrapper(JSONObject json) { super(json); }

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

	public String getClientId() {
		return this.json.optString("clientId", null);
	}
	public void setClientId(String clientId) throws JSONException {
		this.json.put("clientId", clientId);
	}

	public String getClientName() {
		return this.json.optString("clientName", null);
	}
	public void setClientName(String clientName) throws JSONException {
		this.json.put("clientName", clientName);
	}

	public String getAttributions() {
		return this.json.optString("attributions", null);
	}
	public void setAttributions(String attributions) throws JSONException {
		this.json.put("attributions", attributions);
	}

	public JSONArray getDefaultLayers() {
		return this.json.optJSONArray("defaultLayers");
	}
	public void setDefaultLayers(JSONArray defaultLayers) throws JSONException {
		this.json.put("defaultLayers", defaultLayers);
	}

	public String getProjection() {
		return this.json.optString("projection", null);
	}
	public void setProjection(String projection) throws JSONException {
		this.json.put("projection", projection);
	}

	public JSONObject getMapOptions() {
		return this.json.optJSONObject("mapOptions");
	}
	public void setMapOptions(JSONObject mapOptions) throws JSONException {
		this.json.put("mapOptions", mapOptions);
	}

	public String getLayerInfoServiceUrl() {
		return this.json.optString("layerInfoServiceUrl", null);
	}
	public void setLayerInfoServiceUrl(String layerInfoServiceUrl) throws JSONException {
		this.json.put("layerInfoServiceUrl", layerInfoServiceUrl);
	}

	public String getDownloadLoggerServiceUrl() {
		return this.json.optString("downloadLoggerServiceUrl", null);
	}
	public void setDownloadLoggerServiceUrl(String downloadLoggerServiceUrl) throws JSONException {
		this.json.put("downloadLoggerServiceUrl", downloadLoggerServiceUrl);
	}

	public Boolean isShowAddRemoveLayerButtons() {
		return this.isShowAddRemoveLayerButtons(null);
	}
	public Boolean isShowAddRemoveLayerButtons(Boolean defaultValue) {
		if (this.json.isNull("showAddRemoveLayerButtons")) {
			return defaultValue;
		}
		return this.json.optBoolean("showAddRemoveLayerButtons");
	}
	public void setShowAddRemoveLayerButtons(Boolean showAddRemoveLayerButtons) throws JSONException {
		if (showAddRemoveLayerButtons == null && !this.json.isNull("showAddRemoveLayerButtons")) {
			this.json.remove("showAddRemoveLayerButtons");
		} else {
			this.json.put("showAddRemoveLayerButtons", showAddRemoveLayerButtons);
		}
	}

	public String getManualOverride() {
		return this.json.optString("manualOverride", null);
	}
	public void setManualOverride(String manualOverride) throws JSONException {
		this.json.put("manualOverride", manualOverride);
	}

	public Boolean isSearchEnabled() {
		return this.isSearchEnabled(null);
	}
	public Boolean isSearchEnabled(Boolean defaultValue) {
		if (this.json.isNull("searchEnabled")) {
			return defaultValue;
		}
		return this.json.optBoolean("searchEnabled");
	}
	public void setSearchEnabled(Boolean searchEnabled) throws JSONException {
		if (searchEnabled == null && !this.json.isNull("searchEnabled")) {
			this.json.remove("searchEnabled");
		} else {
			this.json.put("searchEnabled", searchEnabled);
		}
	}

	public Boolean isPrintEnabled() {
		return this.isPrintEnabled(null);
	}
	public Boolean isPrintEnabled(Boolean defaultValue) {
		if (this.json.isNull("printEnabled")) {
			return defaultValue;
		}
		return this.json.optBoolean("printEnabled");
	}
	public void setPrintEnabled(Boolean printEnabled) throws JSONException {
		if (printEnabled == null && !this.json.isNull("printEnabled")) {
			this.json.remove("printEnabled");
		} else {
			this.json.put("printEnabled", printEnabled);
		}
	}

	public Boolean isSaveMapEnabled() {
		return this.isSaveMapEnabled(null);
	}
	public Boolean isSaveMapEnabled(Boolean defaultValue) {
		if (this.json.isNull("saveMapEnabled")) {
			return defaultValue;
		}
		return this.json.optBoolean("saveMapEnabled");
	}
	public void setSaveMapEnabled(Boolean saveMapEnabled) throws JSONException {
		if (saveMapEnabled == null && !this.json.isNull("saveMapEnabled")) {
			this.json.remove("saveMapEnabled");
		} else {
			this.json.put("saveMapEnabled", saveMapEnabled);
		}
	}

	public Boolean isMapConfigEnabled() {
		return this.isMapConfigEnabled(null);
	}
	public Boolean isMapConfigEnabled(Boolean defaultValue) {
		if (this.json.isNull("mapConfigEnabled")) {
			return defaultValue;
		}
		return this.json.optBoolean("mapConfigEnabled");
	}
	public void setMapConfigEnabled(Boolean mapConfigEnabled) throws JSONException {
		if (mapConfigEnabled == null && !this.json.isNull("mapConfigEnabled")) {
			this.json.remove("mapConfigEnabled");
		} else {
			this.json.put("mapConfigEnabled", mapConfigEnabled);
		}
	}

	public Boolean isMapMeasurementEnabled() {
		return this.isMapMeasurementEnabled(null);
	}
	public Boolean isMapMeasurementEnabled(Boolean defaultValue) {
		if (this.json.isNull("mapMeasurementEnabled")) {
			return defaultValue;
		}
		return this.json.optBoolean("mapMeasurementEnabled");
	}
	public void setMapMeasurementEnabled(Boolean mapMeasurementEnabled) throws JSONException {
		if (mapMeasurementEnabled == null && !this.json.isNull("mapMeasurementEnabled")) {
			this.json.remove("mapMeasurementEnabled");
		} else {
			this.json.put("mapMeasurementEnabled", mapMeasurementEnabled);
		}
	}

	public Boolean isMapMeasurementLineEnabled() {
		return this.isMapMeasurementLineEnabled(null);
	}
	public Boolean isMapMeasurementLineEnabled(Boolean defaultValue) {
		if (this.json.isNull("mapMeasurementLineEnabled")) {
			return defaultValue;
		}
		return this.json.optBoolean("mapMeasurementLineEnabled");
	}
	public void setMapMeasurementLineEnabled(Boolean mapMeasurementLineEnabled) throws JSONException {
		if (mapMeasurementLineEnabled == null && !this.json.isNull("mapMeasurementLineEnabled")) {
			this.json.remove("mapMeasurementLineEnabled");
		} else {
			this.json.put("mapMeasurementLineEnabled", mapMeasurementLineEnabled);
		}
	}

	public Boolean isMapMeasurementAreaEnabled() {
		return this.isMapMeasurementAreaEnabled(null);
	}
	public Boolean isMapMeasurementAreaEnabled(Boolean defaultValue) {
		if (this.json.isNull("mapMeasurementAreaEnabled")) {
			return defaultValue;
		}
		return this.json.optBoolean("mapMeasurementAreaEnabled");
	}
	public void setMapMeasurementAreaEnabled(Boolean mapMeasurementAreaEnabled) throws JSONException {
		if (mapMeasurementAreaEnabled == null && !this.json.isNull("mapMeasurementAreaEnabled")) {
			this.json.remove("mapMeasurementAreaEnabled");
		} else {
			this.json.put("mapMeasurementAreaEnabled", mapMeasurementAreaEnabled);
		}
	}

	public String getClientUrl() {
		return this.json.optString("clientUrl", null);
	}
	public void setClientUrl(String clientUrl) throws JSONException {
		this.json.put("clientUrl", clientUrl);
	}

	public String getLayerListUrl() {
		return this.json.optString("layerListUrl", null);
	}
	public void setLayerListUrl(String layerListUrl) throws JSONException {
		this.json.put("layerListUrl", layerListUrl);
	}

	public String getSearchServiceUrl() {
		return this.json.optString("searchServiceUrl", null);
	}
	public void setSearchServiceUrl(String searchServiceUrl) throws JSONException {
		this.json.put("searchServiceUrl", searchServiceUrl);
	}

	public JSONArray getStartingLocation() {
		return this.json.optJSONArray("startingLocation");
	}
	public void setStartingLocation(JSONArray startingLocation) throws JSONException {
		this.json.put("startingLocation", startingLocation);
	}

	public String getLayersPanelHeader() {
		return this.json.optString("layersPanelHeader", null);
	}
	public void setLayersPanelHeader(String layersPanelHeader) throws JSONException {
		this.json.put("layersPanelHeader", layersPanelHeader);
	}

	public String getLayersPanelFooter() {
		return this.json.optString("layersPanelFooter", null);
	}
	public void setLayersPanelFooter(String layersPanelFooter) throws JSONException {
		this.json.put("layersPanelFooter", layersPanelFooter);
	}

	public JSONObject getDataSources() {
		return this.json.optJSONObject("dataSources");
	}
	public void setDataSources(JSONObject dataSources) throws JSONException {
		this.json.put("dataSources", dataSources);
	}
	public void addDataSource(String dataSourceId, JSONObject dataSource) throws JSONException {
		JSONObject dataSources = this.getDataSources();
		if (dataSources == null) {
			dataSources = new JSONObject();
			this.setDataSources(dataSources);
		}
		dataSources.put(dataSourceId, dataSource);
	}


	public JSONObject getLayers() {
		return this.json.optJSONObject("layers");
	}
	public void setLayers(JSONObject layers) throws JSONException {
		this.json.put("layers", layers);
	}

	public JSONObject getModules() {
		return this.json.optJSONObject("modules");
	}
	public void setModules(JSONObject modules) throws JSONException {
		this.json.put("modules", modules);
	}

	public String getProxyUrl() {
		return this.json.optString("proxyUrl", null);
	}
	public void setProxyUrl(String proxyUrl) throws JSONException {
		this.json.put("proxyUrl", proxyUrl);
	}
}
