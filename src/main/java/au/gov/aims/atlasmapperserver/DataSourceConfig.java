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

package au.gov.aims.atlasmapperserver;

import au.gov.aims.atlasmapperserver.annotation.ConfigField;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.geotools.ows.ServiceException;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author glafond
 */
public class DataSourceConfig extends AbstractConfig implements Comparable<DataSourceConfig>, Cloneable {
	private static final Logger LOGGER = Logger.getLogger(DataSourceConfig.class.getName());

	// Grids records must have an unmutable ID
	@ConfigField
	private Integer id;

	@ConfigField
	private String dataSourceId;

	@ConfigField
	private String dataSourceName;

	@ConfigField
	private String dataSourceType;

	@ConfigField
	private String wmsServiceUrl;

	@ConfigField
	private String extraWmsServiceUrls;
	// Cache - avoid parsing baseLayers string every times.
	private Set<String> extraWmsServiceUrlsSet = null;

	@ConfigField
	private String kmlUrls;
	// Cache - avoid parsing baseLayers string every times.
	private Set<String> kmlUrlsSet = null;

	@ConfigField
	private String webCacheUrl;

	@ConfigField
	private String webCacheParameters;

	@ConfigField
	private String featureRequestsUrl;

	@ConfigField
	private String legendUrl;

	@ConfigField
	private String legendParameters;
	// Cache - avoid parsing legendParameters string every times.
	private JSONObject legendParametersJson;

	@ConfigField
	private String blacklistedLayers;
	// Cache - avoid parsing blacklistedLayers string every times.
	private Set<String> blacklistedLayerIdsSet = null;
	private Set<Pattern> blacklistedLayerRegexesSet = null;

	@ConfigField
	private String baseLayers;
	// Cache - avoid parsing baseLayers string every times.
	private Set<String> baseLayersSet = null;

	@ConfigField
	private JSONObject globalManualOverride;

	@ConfigField
	private Boolean showInLegend;

	@ConfigField
	private String wmsRequestMimeType;

	@ConfigField
	private Boolean wmsTransectable;

	@ConfigField
	private String wmsVersion;

	@ConfigField
	private String comment;

	public DataSourceConfig(ConfigManager configManager) {
		super(configManager);
	}

	@Override
	public void setJSONObjectKey(String key) {
		if (Utils.isBlank(this.dataSourceId)) {
			this.dataSourceId = key;
		}
	}

	@Override
	public String getJSONObjectKey() {
		return this.dataSourceId;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getBlacklistedLayers() {
		return blacklistedLayers;
	}

	public void setBlacklistedLayers(String blacklistedLayers) {
		this.blacklistedLayers = blacklistedLayers;
		this.blacklistedLayerIdsSet = null;
		this.blacklistedLayerRegexesSet = null;
	}

	public String getBaseLayers() {
		return baseLayers;
	}

	public void setBaseLayers(String baseLayers) {
		this.baseLayers = baseLayers;
		this.baseLayersSet = null;
	}

	public JSONObject getGlobalManualOverride() {
		return globalManualOverride;
	}

	public void setGlobalManualOverride(JSONObject globalManualOverride) {
		this.globalManualOverride = globalManualOverride;
	}

	public String getDataSourceType() {
		return dataSourceType;
	}

	public void setDataSourceType(String dataSourceType) {
		this.dataSourceType = dataSourceType;
	}

	public String getFeatureRequestsUrl() {
		return featureRequestsUrl;
	}

	public void setFeatureRequestsUrl(String featureRequestsUrl) {
		this.featureRequestsUrl = featureRequestsUrl;
	}

	public String getWmsServiceUrl() {
		return wmsServiceUrl;
	}

	public void setWmsServiceUrl(String wmsServiceUrl) {
		this.wmsServiceUrl = wmsServiceUrl;
		this.extraWmsServiceUrlsSet = null;
	}

	public String getLegendUrl() {
		return legendUrl;
	}

	public void setLegendUrl(String legendUrl) {
		this.legendUrl = legendUrl;
	}

	public String getLegendParameters() throws JSONException {
		return this.legendParameters;
	}

	public void setLegendParameters(String legendParameters) {
		this.legendParameters = legendParameters;
		this.legendParametersJson = null;
	}

	public JSONObject getLegendParametersJson() throws JSONException {
		if (this.legendParameters == null) {
			return null;
		}

		if (this.legendParametersJson == null) {
			String trimedLegendParameters = this.legendParameters.trim();
			if (trimedLegendParameters.isEmpty()) {
				return null;
			}

			this.legendParametersJson = new JSONObject();
			for (String legendParameter : toSet(trimedLegendParameters)) {
				if (Utils.isNotBlank(legendParameter)) {
					String[] attribute = legendParameter.split(SPLIT_ATTRIBUTES_PATTERN);
					if (attribute != null && attribute.length >= 2) {
						this.legendParametersJson.put(
								attribute[0],  // Key
								attribute[1]); // Value
					}
				}
			}
		}

		return this.legendParametersJson;
	}
			
	public String getDataSourceId() {
		// Error protection against erronous manual config file edition
		if (this.dataSourceId == null && this.id != null) {
			return this.id.toString();
		}
		return this.dataSourceId;
	}

	public void setDataSourceId(String dataSourceId) {
		this.dataSourceId = dataSourceId;
	}

	public String getDataSourceName() {
		return dataSourceName;
	}

	public void setDataSourceName(String dataSourceName) {
		this.dataSourceName = dataSourceName;
	}

	public String getExtraWmsServiceUrls() {
		return extraWmsServiceUrls;
	}
	public Set<String> getExtraWmsServiceUrlsSet() {
		if (this.extraWmsServiceUrlsSet == null && Utils.isNotBlank(this.extraWmsServiceUrls)) {
			this.extraWmsServiceUrlsSet = toSet(this.extraWmsServiceUrls);
		}

		return this.extraWmsServiceUrlsSet;
	}

	public void setExtraWmsServiceUrls(String extraWmsServiceUrls) {
		this.extraWmsServiceUrls = extraWmsServiceUrls;
	}

	public String getKmlUrls() {
		return kmlUrls;
	}
	public Set<String> getKmlUrlsSet() {
		if (this.kmlUrlsSet == null && Utils.isNotBlank(this.kmlUrls)) {
			this.kmlUrlsSet = toSet(this.kmlUrls);
		}

		return this.kmlUrlsSet;
	}

	public void setKmlUrls(String kmlUrls) {
		this.kmlUrls = kmlUrls;
		this.kmlUrlsSet = null;
	}

	public Boolean isShowInLegend() {
		return showInLegend;
	}

	public void setShowInLegend(Boolean showInLegend) {
		this.showInLegend = showInLegend;
	}

	public String getWmsRequestMimeType() {
		return wmsRequestMimeType;
	}

	public void setWmsRequestMimeType(String wmsRequestMimeType) {
		this.wmsRequestMimeType = wmsRequestMimeType;
	}

	public Boolean isWmsTransectable() {
		return wmsTransectable;
	}

	public void setWmsTransectable(Boolean wmsTransectable) {
		this.wmsTransectable = wmsTransectable;
	}

	public String getWmsVersion() {
		return wmsVersion;
	}

	public void setWmsVersion(String wmsVersion) {
		this.wmsVersion = wmsVersion;
	}

	public String[] getWebCacheParametersArray() {
		if (this.webCacheParameters == null) {
			return null;
		}

		String trimedWebCacheParameters = this.webCacheParameters.trim();
		if (trimedWebCacheParameters.isEmpty()) {
			return null;
		}

		return trimedWebCacheParameters.split("\\s*,\\s*");
	}

	public String getWebCacheParameters() {
		return this.webCacheParameters;
	}

	public void setWebCacheParameters(String webCacheParameters) {
		this.webCacheParameters = webCacheParameters;
	}

	public String getWebCacheUrl() {
		return webCacheUrl;
	}

	public void setWebCacheUrl(String webCacheUrl) {
		this.webCacheUrl = webCacheUrl;
	}

	public String getComment() {
		return this.comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	// Helper
	public boolean isBaseLayer(String layerId) {
		String baselayersStr = this.getBaseLayers();
		if (Utils.isBlank(layerId) || Utils.isBlank(baselayersStr)) {
			return false;
		}

		if (this.baseLayersSet == null) {
			this.baseLayersSet = toSet(baselayersStr);
		}

		return this.baseLayersSet.contains(layerId);
	}

	// Helper
	public boolean isBlacklisted(String layerId) {
		if (Utils.isBlank(layerId)) {
			return true;
		}
		String blacklistedLayersIdStr = this.getBlacklistedLayers();
		if (Utils.isBlank(blacklistedLayersIdStr)) {
			return false;
		}

		if (this.blacklistedLayerIdsSet == null && this.blacklistedLayerRegexesSet == null) {
			this.blacklistedLayerIdsSet = new HashSet<String>();
			this.blacklistedLayerRegexesSet = new HashSet<Pattern>();

			String[] blacklistedLayersId = blacklistedLayersIdStr.split(SPLIT_PATTERN);
			if (blacklistedLayersId != null) {
				for (int i=0; i<blacklistedLayersId.length; i++) {
					String blacklistedLayerId = blacklistedLayersId[i];
					if (Utils.isNotBlank(blacklistedLayerId)) {
						if (blacklistedLayerId.contains("*")) {
							this.blacklistedLayerRegexesSet.add(toPattern(blacklistedLayerId.trim()));
						} else {
							this.blacklistedLayerIdsSet.add(blacklistedLayerId.trim());
						}
					}
				}
			}
		}

		if (this.blacklistedLayerIdsSet != null && this.blacklistedLayerIdsSet.contains(layerId)) {
			return true;
		}
		for (Pattern pattern : this.blacklistedLayerRegexesSet) {
			if (pattern.matcher(layerId).matches()) {
				return true;
			}
		}

		return false;
	}

	// Very basic pattern generator that use * as a wildcard.
	private static Pattern toPattern(String layerPatternStr) {
		String[] layerPatternParts = layerPatternStr.split("\\*");

		boolean firstPart = true;
		StringBuilder sb = new StringBuilder();
		if (layerPatternStr.startsWith("*")) {
			sb.append(".*");
		}
		for (String layerPatternPart : layerPatternParts) {
			if (firstPart) {
				firstPart = false;
			} else {
				sb.append(".*");
			}
			// Quote everything between * (quote mean that it escape everything)
			sb.append(Pattern.quote(layerPatternPart));
		}
		if (layerPatternStr.endsWith("*")) {
			sb.append(".*");
		}

		return Pattern.compile(sb.toString());
	}

	// Helper
	public Map<String, LayerConfig> getLayerConfigs(ClientConfig clientConfig) throws IOException, ServiceException, JSONException {
		Map<String, LayerConfig> overridenLayerConfigs = new HashMap<String, LayerConfig>();

		JSONObject globalOverrides = this.globalManualOverride;
		JSONObject clientOverrides = clientConfig.getManualOverride();

		// Retrieved raw layers, according to the data source type
		Map<String, LayerConfig> layersConfig = null;

		// 'WMS', 'NCWMS', 'ARCGISWMS', 'GOOGLE', 'WMTS', 'KML', 'tiles', 'XYZ'
		if ("GOOGLE".equals(this.dataSourceType)) {
			layersConfig = GoogleLayers.getGoogleLayerConfigs(clientConfig, this);

		} else if ("KML".equals(this.dataSourceType)) {
			Set<String> _kmlUrlsSet = this.getKmlUrlsSet();
			if (_kmlUrlsSet != null && !_kmlUrlsSet.isEmpty()) {
				for (String kmlUrl : _kmlUrlsSet) {
					if (Utils.isNotBlank(kmlUrl)) {
						int layerIdStart = kmlUrl.lastIndexOf('/')+1;
						int layerIdEnd = kmlUrl.lastIndexOf('.');
						layerIdEnd = (layerIdEnd > 0 ? layerIdEnd : kmlUrl.length());
						String layerId = kmlUrl.substring(layerIdStart, layerIdEnd);

						LayerConfig layer = new LayerConfig(this.getConfigManager());
						layer.setDataSourceId(this.dataSourceId);
						layer.setLayerId(layerId);
						layer.setTitle(layerId);
						layer.setKmlUrl(kmlUrl);

						if (layersConfig == null) {
							layersConfig = new HashMap<String, LayerConfig>();
						}
						layersConfig.put(layerId, layer);
					}
				}
			}

		} else if (Utils.isNotBlank(this.wmsServiceUrl)) {
			// Assume the service is a WMS compliant service
			WMSCapabilitiesWrapper wmsCap =
					WMSCapabilitiesWrapper.getInstance(this.wmsServiceUrl);

			if (wmsCap != null) {
				layersConfig = wmsCap.getLayerConfigs(clientConfig, this);
			}

		} else {
			// Problem...
		}

		// Remove blacklisted layers and apply manual overrides, if needed
		if (layersConfig != null) {
			for (LayerConfig layerConfig : layersConfig.values()) {
				if (layerConfig != null && !this.isBlacklisted(layerConfig.getLayerId())) {
					LayerConfig overriddenLayerConfig =
							layerConfig.applyOverrides(
									globalOverrides,
									clientOverrides);
					overridenLayerConfigs.put(
							overriddenLayerConfig.getLayerId(),
							overriddenLayerConfig);
				}
			}
		}

		// Create manual layers defined for this data source
		if (globalOverrides != null && globalOverrides.length() > 0) {
			Iterator<String> layerIds = globalOverrides.keys();
			while (layerIds.hasNext()) {
				String layerId = layerIds.next();
				if (!overridenLayerConfigs.containsKey(layerId) && !this.isBlacklisted(layerId)) {
					JSONObject jsonGlobalOverride = globalOverrides.optJSONObject(layerId);
					if (jsonGlobalOverride != null && jsonGlobalOverride.length() > 0) {
						LayerConfig manualLayer = new LayerConfig(this.getConfigManager(), jsonGlobalOverride);
						manualLayer.setLayerId(layerId);

						// Apply client override if any
						if (clientOverrides != null && clientOverrides.has(layerId)) {
							JSONObject jsonClientOverride = clientOverrides.optJSONObject(layerId);
							if (jsonClientOverride != null && jsonClientOverride.length() > 0) {
								LayerConfig clientOverride = new LayerConfig(this.getConfigManager(), jsonClientOverride);
								manualLayer.applyOverrides(clientOverride);
							}
						}

						// Add data source info if omitted
						if (Utils.isBlank(manualLayer.getDataSourceId())) {
							manualLayer.setDataSourceId(this.dataSourceId);
						}

						overridenLayerConfigs.put(
								manualLayer.getLayerId(),
								manualLayer);
					}
				}
			}
		}

		return overridenLayerConfigs;
	}

	@Override
	// Order data sources by data source name
	public int compareTo(DataSourceConfig o) {
		// Compare memory address and both null value
		if (this == o || this.getDataSourceName() == o.getDataSourceName()) {
			return 0;
		}

		String srvName = this.getDataSourceName();
		String othName = o.getDataSourceName();
		// Move null a the end of the list. (Just in case; Null values should not append...)
		if (srvName == null) { return -1; }
		if (othName == null) { return 1; }

		return srvName.toLowerCase().compareTo(othName.toLowerCase());
	}

	// Nothing to do here
	public DataSourceConfig applyOverrides() {
		return this;
	}

	@Override
	public JSONObject toJSonObject() throws JSONException {
		JSONObject json = super.toJSonObject();
		if (this.globalManualOverride != null) {
			json.put("globalManualOverride", this.globalManualOverride.toString(4));
		}
		return json;
	}

	@Override
	public String toString() {
		return "DataSourceConfig {\n" +
				(id==null ? "" :                           "	id=" + id + "\n") +
				(Utils.isBlank(dataSourceId) ? "" :        "	dataSourceId=" + dataSourceId + "\n") +
				(Utils.isBlank(dataSourceName) ? "" :      "	dataSourceName=" + dataSourceName + "\n") +
				(Utils.isBlank(dataSourceType) ? "" :      "	dataSourceType=" + dataSourceType + "\n") +
				(Utils.isBlank(extraWmsServiceUrls) ? "" : "	serverUrls=" + extraWmsServiceUrls + "\n") +
				(Utils.isBlank(kmlUrls) ? "" :             "	kmlUrls=" + kmlUrls + "\n") +
				(Utils.isBlank(wmsServiceUrl) ? "" :       "	wmsServiceUrl=" + wmsServiceUrl + "\n") +
				(Utils.isBlank(webCacheUrl) ? "" :         "	webCacheUrl=" + webCacheUrl + "\n") +
				(Utils.isBlank(webCacheParameters) ? "" :  "	webCacheParameters=" + webCacheParameters + "\n") +
				(Utils.isBlank(featureRequestsUrl) ? "" :  "	featureRequestsUrl=" + featureRequestsUrl + "\n") +
				(Utils.isBlank(legendUrl) ? "" :           "	legendUrl=" + legendUrl + "\n") +
				(legendParameters==null ? "" :             "	legendParameters=" + legendParameters + "\n") +
				(Utils.isBlank(blacklistedLayers) ? "" :   "	blacklistedLayers=" + blacklistedLayers + "\n") +
				(showInLegend==null ? "" :                 "	showInLegend=" + showInLegend + "\n") +
				(Utils.isBlank(wmsRequestMimeType) ? "" :  "	wmsRequestMimeType=" + wmsRequestMimeType + "\n") +
				(wmsTransectable==null ? "" :              "	wmsTransectable=" + wmsTransectable + "\n") +
				(Utils.isBlank(comment) ? "" :             "	comment=" + comment + "\n") +
			'}';
	}
}
