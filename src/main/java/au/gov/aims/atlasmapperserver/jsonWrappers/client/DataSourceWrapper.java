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
package au.gov.aims.atlasmapperserver.jsonWrappers.client;

import au.gov.aims.atlasmapperserver.AbstractConfig;
import au.gov.aims.atlasmapperserver.Utils;
import au.gov.aims.atlasmapperserver.jsonWrappers.AbstractWrapper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.util.Iterator;

/**
 * This class is wrapping a JSONObject representing a data source to be sent to the AtlasMapper client.
 * It has been made to manage the Json keys in one location and simplify maintenance.
 */
public class DataSourceWrapper extends AbstractWrapper {
	public DataSourceWrapper(JSONObject json) {
		super(json);
	}

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

	public String getDataSourceType() {
		return this.json.optString("dataSourceType", null);
	}
	public void setDataSourceType(String dataSourceType) throws JSONException {
		this.json.put("dataSourceType", dataSourceType);
	}

	public String getServiceUrl() {
		return this.json.optString("wmsServiceUrl", null);
	}
	public void setServiceUrl(URL serviceUrl) throws JSONException {
		this.setServiceUrl(serviceUrl == null ? null : serviceUrl.toString());
	}
	public void setServiceUrl(String serviceUrl) throws JSONException {
		this.json.put("wmsServiceUrl", serviceUrl);
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

	public void addLayer(String layerId, JSONObject layer) throws JSONException {
		JSONObject layers = this.getLayers();
		if (layers == null) {
			layers = new JSONObject();
			this.setLayers(layers);
		}
		layers.put(layerId, layer);
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

	public String getLastHarvested() {
		return this.json.optString("lastHarvested", null);
	}
	public void setLastHarvested(String lastHarvested) throws JSONException {
		this.setValue("lastHarvested", lastHarvested);
	}

	public Boolean getValid() {
		if (this.json.has("valid")) {
			return this.json.optBoolean("valid");
		}
		return null;
	}
	public void setValid(Boolean valid) throws JSONException {
		this.setValue("valid", valid);
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
		return "ARCGIS_MAPSERVER".equals(this.getDataSourceType());
	}
	public boolean isGoogle() {
		return "GOOGLE".equals(this.getDataSourceType());
	}
	public boolean isBing() {
		return "BING".equals(this.getDataSourceType());
	}
	public boolean isKML() {
		return "KML".equals(this.getDataSourceType());
	}
	public boolean isNCWMS() {
		return "NCWMS".equals(this.getDataSourceType());
	}
	public boolean isTiles() {
		return "TILES".equals(this.getDataSourceType());
	}
	public boolean isXYZ() {
		return "XYZ".equals(this.getDataSourceType());
	}
	public boolean isWMS() {
		return "WMS".equals(this.getDataSourceType());
	}

	public JSONArray getErrors() {
		return this.json.optJSONArray("errors");
	}
	public void setErrors(JSONArray errors) throws JSONException {
		this.json.put("errors", errors);
	}
	public void addError(String error) throws JSONException {
		JSONArray errors = this.getErrors();
		if (errors == null) {
			errors = new JSONArray();
			this.setErrors(errors);
		}
		errors.put(error);
	}

	public JSONArray getWarnings() {
		return this.json.optJSONArray("warnings");
	}
	public void setWarnings(JSONArray warnings) throws JSONException {
		this.json.put("warnings", warnings);
	}
	public void addWarning(String warning) throws JSONException {
		JSONArray warnings = this.getWarnings();
		if (warnings == null) {
			warnings = new JSONArray();
			this.setWarnings(warnings);
		}
		warnings.put(warning);
	}

	public JSONArray getMessages() {
		return this.json.optJSONArray("messages");
	}
	public void setMessages(JSONArray messages) throws JSONException {
		this.json.put("messages", messages);
	}
	public void addMessage(String message) throws JSONException {
		JSONArray messages = this.getMessages();
		if (messages == null) {
			messages = new JSONArray();
			this.setMessages(messages);
		}
		messages.put(message);
	}

	// The data source has many attributes unneeded for the main config; they only enlarge the file.
	// It is better to remove them in this context.
	public JSONObject getMainConfigJSON() throws JSONException {
		DataSourceWrapper dataSourceClone = (DataSourceWrapper)this.clone();
		if (dataSourceClone == null) {
			// Unlikely to happen
			dataSourceClone = new DataSourceWrapper(new JSONObject());
		}

		// Remove unwanted values
		dataSourceClone.setId(null);
		dataSourceClone.setLayers(null);
		dataSourceClone.setGlobalManualOverride(null);
		dataSourceClone.setWarnings(null);
		dataSourceClone.setErrors(null);
		dataSourceClone.setMessages(null);
		dataSourceClone.setBlackAndWhiteListedLayers(null);
		dataSourceClone.setLastHarvested(null);
		dataSourceClone.setValid(null);
		dataSourceClone.setComment(null);

		return dataSourceClone.getJSON();
	}
}
