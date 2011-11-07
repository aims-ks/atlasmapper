/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.gov.aims.atlasmapperserver;

import au.gov.aims.atlasmapperserver.annotation.ConfigField;
import java.io.IOException;
import java.net.MalformedURLException;
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
public class DatasourceConfig extends AbstractConfig implements Comparable<DatasourceConfig>, Cloneable {
	private static final Logger LOGGER = Logger.getLogger(DatasourceConfig.class.getName());
	private static final String SPLIT_PATTERN = "[,\r\n]";

	// Grids records must have an unmutable ID
	@ConfigField
	private Integer id;

	@ConfigField
	private String datasourceId;

	@ConfigField
	private String datasourceName;

	@ConfigField
	private String datasourceType;

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
	private JSONObject legendParameters;

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

	@Override
	public void setJSONObjectKey(String key) {
		if (Utils.isBlank(this.datasourceId)) {
			this.datasourceId = key;
		}
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

	public String getDatasourceType() {
		return datasourceType;
	}

	public void setDatasourceType(String datasourceType) {
		this.datasourceType = datasourceType;
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

	public JSONObject getLegendParameters() throws JSONException {
		//return legendParameters;
		// TODO - Not a stub!!!
		JSONObject params = new JSONObject();
		params.put("FORMAT", "image/png");
		params.put("HEIGHT", "10");
		params.put("WIDTH", "20");

		return params;
	}

	public void setLegendParameters(JSONObject legendParameters) {
		this.legendParameters = legendParameters;
	}

	public String getDatasourceId() {
		return datasourceId;
	}

	public void setDatasourceId(String datasourceId) {
		this.datasourceId = datasourceId;
	}

	public String getDatasourceName() {
		return datasourceName;
	}

	public void setDatasourceName(String datasourceName) {
		this.datasourceName = datasourceName;
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
		if (trimedWebCacheParameters.length() <= 0) {
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

		if (this.blacklistedLayerIdsSet.contains(layerId)) {
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
	public Map<String, LayerConfig> getLayerConfigs(ClientConfig clientConfig) throws MalformedURLException, IOException, ServiceException, JSONException {
		Map<String, LayerConfig> overridenLayerConfigs = new HashMap<String, LayerConfig>();

		JSONObject globalOverrides = this.globalManualOverride;
		JSONObject clientOverrides = clientConfig.getManualOverride();

		// Retrieved raw layers, according to the datasource type
		Map<String, LayerConfig> layersConfig = null;

		// 'WMS', 'NCWMS', 'ARCGISWMS', 'GOOGLE', 'WMTS', 'KML', 'tiles', 'XYZ'
		if ("GOOGLE".equals(this.datasourceType)) {
			layersConfig = GoogleLayers.getGoogleLayerConfigs(clientConfig, this);

		} else if ("KML".equals(this.datasourceType)) {
			Set<String> _kmlUrlsSet = this.getKmlUrlsSet();
			if (_kmlUrlsSet != null && !_kmlUrlsSet.isEmpty()) {
				for (String kmlUrl : _kmlUrlsSet) {
					if (Utils.isNotBlank(kmlUrl)) {
						int layerIdStart = kmlUrl.lastIndexOf('/')+1;
						int layerIdEnd = kmlUrl.lastIndexOf('.');
						layerIdEnd = (layerIdEnd > 0 ? layerIdEnd : kmlUrl.length());
						String layerId = kmlUrl.substring(layerIdStart, layerIdEnd);

						LayerConfig layer = new LayerConfig(this);
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
					LayerConfig overridenLayerConfig =
							layerConfig.applyOverrides(
									this,
									globalOverrides,
									clientOverrides);

					overridenLayerConfigs.put(
							overridenLayerConfig.getLayerId(),
							overridenLayerConfig);
				}
			}
		}

		// Create manual layers defined for this datasource
		if (globalOverrides != null && globalOverrides.length() > 0) {
			Iterator<String> layerIds = globalOverrides.keys();
			while (layerIds.hasNext()) {
				String layerId = layerIds.next();
				if (!overridenLayerConfigs.containsKey(layerId) && !this.isBlacklisted(layerId)) {
					JSONObject jsonGlobalOverride = globalOverrides.optJSONObject(layerId);
					if (jsonGlobalOverride != null && jsonGlobalOverride.length() > 0) {
						LayerConfig manualLayer = new LayerConfig(jsonGlobalOverride);
						manualLayer.setLayerId(layerId);

						// Apply client override if any
						if (clientOverrides != null && clientOverrides.has(layerId)) {
							JSONObject jsonClientOverride = clientOverrides.optJSONObject(layerId);
							if (jsonClientOverride != null && jsonClientOverride.length() > 0) {
								LayerConfig clientOverride = new LayerConfig(jsonClientOverride);
								manualLayer.applyOverrides(clientOverride);
							}
						}

						// Add datasource info if omitted
						if (Utils.isBlank(manualLayer.getDatasourceId())) {
							manualLayer.setDatasourceId(this.datasourceId);
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

	private static Set<String> toSet(String setStr) {
		Set<String> set = new HashSet<String>();
		String[] strArray = setStr.split(SPLIT_PATTERN);
		if (strArray != null) {
			for (int i=0; i<strArray.length; i++) {
				String str = strArray[i];
				if (Utils.isNotBlank(str)) {
					set.add(str.trim());
				}
			}
		}
		return set;
	}

	@Override
	// Order datasources by datasource name
	public int compareTo(DatasourceConfig o) {
		// Compare memory address and both null value
		if (this == o || this.getDatasourceName() == o.getDatasourceName()) {
			return 0;
		}

		String srvName = this.getDatasourceName();
		String othName = o.getDatasourceName();
		// Move null a the end of the list. (Just in case; Null values should not append...)
		if (srvName == null) { return -1; }
		if (othName == null) { return 1; }

		return srvName.toLowerCase().compareTo(othName.toLowerCase());
	}

	// Nothing to do here
	public DatasourceConfig applyOverrides() {
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
		return "DatasourceConfig {\n" +
				(id==null ? "" :                           "	id=" + id + "\n") +
				(Utils.isBlank(datasourceId) ? "" :        "	datasourceId=" + datasourceId + "\n") +
				(Utils.isBlank(datasourceName) ? "" :      "	datasourceName=" + datasourceName + "\n") +
				(Utils.isBlank(datasourceType) ? "" :      "	datasourceType=" + datasourceType + "\n") +
				(Utils.isBlank(extraWmsServiceUrls) ? "" : "	serverUrls=" + extraWmsServiceUrls + "\n") +
				(Utils.isBlank(kmlUrls) ? "" :             "	kmlUrls=" + kmlUrls + "\n") +
				(Utils.isBlank(wmsServiceUrl) ? "" :       "	wmsServiceUrl=" + wmsServiceUrl + "\n") +
				(Utils.isBlank(webCacheUrl) ? "" :         "	webCacheUrl=" + webCacheUrl + "\n") +
				(Utils.isBlank(webCacheParameters) ? "" :  "	webCacheParameters=" + webCacheParameters + "\n") +
				(Utils.isBlank(featureRequestsUrl) ? "" :  "	featureRequestsUrl=" + featureRequestsUrl + "\n") +
				(Utils.isBlank(legendUrl) ? "" :           "	legendUrl=" + legendUrl + "\n") +
				(legendParameters==null ? "" :             "	legendParameters=" + legendParameters.toString() + "\n") +
				(Utils.isBlank(blacklistedLayers) ? "" :   "	blacklistedLayers=" + blacklistedLayers + "\n") +
				(showInLegend==null ? "" :                 "	showInLegend=" + showInLegend + "\n") +
				(Utils.isBlank(wmsRequestMimeType) ? "" :  "	wmsRequestMimeType=" + wmsRequestMimeType + "\n") +
				(wmsTransectable==null ? "" :              "	wmsTransectable=" + wmsTransectable + "\n") +
				(Utils.isBlank(comment) ? "" :             "	comment=" + comment + "\n") +
			'}';
	}
}
