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

package au.gov.aims.atlasmapperserver.dataSourceConfig;

import au.gov.aims.atlasmapperserver.AbstractConfig;
import au.gov.aims.atlasmapperserver.ClientConfig;
import au.gov.aims.atlasmapperserver.ConfigManager;
import au.gov.aims.atlasmapperserver.layerConfig.AbstractLayerConfig;
import au.gov.aims.atlasmapperserver.Utils;
import au.gov.aims.atlasmapperserver.annotation.ConfigField;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import au.gov.aims.atlasmapperserver.layerConfig.LayerConfigHelper;
import au.gov.aims.atlasmapperserver.layerGenerator.AbstractLayerGenerator;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONSortedObject;

/**
 *
 * @author glafond
 */
public abstract class AbstractDataSourceConfig extends AbstractConfig implements AbstractDataSourceConfigInterface, Comparable<AbstractDataSourceConfig>, Cloneable {
	private static final Logger LOGGER = Logger.getLogger(AbstractDataSourceConfig.class.getName());

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
	private String serviceUrl;

	@ConfigField
	private String featureRequestsUrl;

	@ConfigField
	private String legendUrl;

	@ConfigField
	private String legendParameters;

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
	private JSONSortedObject globalManualOverride;

	@ConfigField
	private Boolean cachingDisabled;

	@ConfigField
	private Boolean showInLegend;

	@ConfigField
	private String comment;


	protected AbstractDataSourceConfig(ConfigManager configManager) {
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

	public JSONSortedObject getGlobalManualOverride() {
		return globalManualOverride;
	}

	public void setGlobalManualOverride(JSONSortedObject globalManualOverride) {
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

	public String getServiceUrl() {
		return serviceUrl;
	}

	public void setServiceUrl(String serviceUrl) {
		this.serviceUrl = serviceUrl;
	}

	public String getLegendUrl() {
		return legendUrl;
	}

	public void setLegendUrl(String legendUrl) {
		this.legendUrl = legendUrl;
	}

	public String getLegendParameters() {
		return this.legendParameters;
	}

	public void setLegendParameters(String legendParameters) {
		this.legendParameters = legendParameters;
	}

	public String getDataSourceId() {
		// Error protection against erroneous manual config file edition
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

	public Boolean isCachingDisabled() {
		return this.cachingDisabled;
	}

	public void setCachingDisabled(Boolean cachingDisabled) {
		this.cachingDisabled = cachingDisabled;
	}

	public Boolean isShowInLegend() {
		return showInLegend;
	}

	public void setShowInLegend(Boolean showInLegend) {
		this.showInLegend = showInLegend;
	}

	public String getComment() {
		return this.comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	// Helper
	public boolean isBaseLayer(String layerId) {
		String baseLayersStr = this.getBaseLayers();
		if (Utils.isBlank(layerId) || Utils.isBlank(baseLayersStr)) {
			return false;
		}

		if (this.baseLayersSet == null) {
			this.baseLayersSet = toSet(baseLayersStr);
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

	public abstract AbstractLayerGenerator getLayerGenerator() throws IOException;

	// Helper
	public Map<String, AbstractLayerConfig> generateLayerConfigs(ClientConfig clientConfig) throws Exception {
		Map<String, AbstractLayerConfig> overriddenLayerConfigs = new HashMap<String, AbstractLayerConfig>();

		JSONSortedObject globalOverrides = this.globalManualOverride;
		JSONObject clientOverrides = clientConfig.getManualOverride();

		// Retrieved raw layers, according to the data source type
		Map<String, AbstractLayerConfig> layerConfigs = null;

		AbstractLayerGenerator layerGenerator = this.getLayerGenerator();

		if (layerGenerator != null) {
			layerConfigs = layerGenerator.generateLayerConfigs(clientConfig, this);
		}

		// Remove blacklisted layers and apply manual overrides, if needed
		if (layerConfigs != null) {
			for (AbstractLayerConfig layerConfig : layerConfigs.values()) {
				if (layerConfig != null && !this.isBlacklisted(layerConfig.getLayerId())) {
					AbstractLayerConfig overriddenLayerConfig =
							layerConfig.applyOverrides(
									globalOverrides,
									clientOverrides);
					overriddenLayerConfigs.put(
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
				if (!overriddenLayerConfigs.containsKey(layerId) && !this.isBlacklisted(layerId)) {
					JSONObject jsonGlobalOverride = globalOverrides.optJSONObject(layerId);
					if (jsonGlobalOverride != null && jsonGlobalOverride.length() > 0) {
						AbstractLayerConfig manualLayer = LayerConfigHelper.createLayerConfig(
								jsonGlobalOverride.optString("dataSourceType"), jsonGlobalOverride, this.getConfigManager());

						manualLayer.setLayerId(layerId);

						// Apply client override if any
						if (clientOverrides != null && clientOverrides.has(layerId)) {
							JSONObject jsonClientOverride = clientOverrides.optJSONObject(layerId);
							if (jsonClientOverride != null && jsonClientOverride.length() > 0) {
								AbstractLayerConfig clientOverride = LayerConfigHelper.createLayerConfig(
										jsonClientOverride.optString("dataSourceType"), jsonClientOverride, this.getConfigManager());

								manualLayer.applyOverrides(clientOverride);
							}
						}

						// Add data source info if omitted
						if (Utils.isBlank(manualLayer.getDataSourceId())) {
							manualLayer.setDataSourceId(this.dataSourceId);
						}

						overriddenLayerConfigs.put(
								manualLayer.getLayerId(),
								manualLayer);
					}
				}
			}
		}

		return overriddenLayerConfigs;
	}

	@Override
	// Order data sources by data source name
	public int compareTo(AbstractDataSourceConfig o) {
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
	public AbstractDataSourceConfig applyOverrides() {
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
		return "AbstractDataSourceConfig {\n" +
				(id==null ? "" :                           "	id=" + id + "\n") +
				(Utils.isBlank(dataSourceId) ? "" :        "	dataSourceId=" + dataSourceId + "\n") +
				(Utils.isBlank(dataSourceName) ? "" :      "	dataSourceName=" + dataSourceName + "\n") +
				(Utils.isBlank(dataSourceType) ? "" :      "	dataSourceType=" + dataSourceType + "\n") +
				(Utils.isBlank(serviceUrl) ? "" :          "	serviceUrl=" + serviceUrl + "\n") +
				(Utils.isBlank(featureRequestsUrl) ? "" :  "	featureRequestsUrl=" + featureRequestsUrl + "\n") +
				(Utils.isBlank(legendUrl) ? "" :           "	legendUrl=" + legendUrl + "\n") +
				(legendParameters==null ? "" :             "	legendParameters=" + legendParameters + "\n") +
				(Utils.isBlank(blacklistedLayers) ? "" :   "	blacklistedLayers=" + blacklistedLayers + "\n") +
				(showInLegend==null ? "" :                 "	showInLegend=" + showInLegend + "\n") +
				(Utils.isBlank(comment) ? "" :             "	comment=" + comment + "\n") +
			'}';
	}
}
