/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.gov.aims.atlasmapperserver;

import au.gov.aims.atlasmapperserver.annotation.ConfigField;
import au.gov.aims.atlasmapperserver.servlet.FileFinder;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletContext;
import org.geotools.ows.ServiceException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author glafond
 */
public class ClientConfig extends AbstractConfig {
	private static final String SPLIT_PATTERN = "[,\r\n]";

	// Grids records must have an unmutable ID
	@ConfigField
	private Integer id;

	@ConfigField(name="default", getter="isDefault", setter="setDefault")
	private Boolean _default;

	@ConfigField
	private String clientName;

	@ConfigField
	private JSONArray datasources;

	@ConfigField
	private boolean fullClientEnable;

	@ConfigField
	private JSONArray fullClientModules;

	@ConfigField
	private boolean embededClientEnable;

	@ConfigField
	private JSONArray embededClientModules;

	@ConfigField
	private String generatedFileLocation;

	@ConfigField
	private String baseUrl;

	@ConfigField
	private JSONObject manualOverride;

	@ConfigField
	private String projection;

	@ConfigField
	private String longitude;

	@ConfigField
	private String latitude;

	@ConfigField
	private String zoom;

	@ConfigField
	private boolean baseLayersInTab;

	@ConfigField
	private String defaultLayers;
	// Cache - avoid parsing baseLayers string every times.
	private Set<String> defaultLayersSet = null;

	@ConfigField
	private String version;

	@ConfigField
	private boolean useLayerService;

	@ConfigField
	private boolean enable;

	@ConfigField
	private String comment;

	@ConfigField
	private Boolean overrideBaseLayers;

	@ConfigField
	private String baseLayers;

	@ConfigField
	private String proxyUrl;

	@ConfigField
	private String layerInfoServiceUrl;
	// Cache - avoid parsing baseLayers string every times.
	private Set<String> baseLayersSet = null;

	public ClientConfig() { }

	@Override
	public void setJSONObjectKey(String key) {
		if (Utils.isBlank(this.clientName)) {
			this.clientName = key;
		}
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Boolean isDefault() {
		return _default;
	}

	public void setDefault(Boolean _default) {
		this._default = _default;
	}

	public boolean isBaseLayersInTab() {
		return baseLayersInTab;
	}

	public void setBaseLayersInTab(boolean baseLayersInTab) {
		this.baseLayersInTab = baseLayersInTab;
	}

	public String getClientName() {
		return clientName;
	}

	public void setClientName(String clientName) {
		this.clientName = clientName;
	}

	public JSONArray getDatasources() {
		return datasources;
	}

	public void setDatasources(JSONArray datasources) {
		this.datasources = datasources;
	}

	public boolean isEmbededClientEnable() {
		return embededClientEnable;
	}

	public void setEmbededClientEnable(boolean embededClientEnable) {
		this.embededClientEnable = embededClientEnable;
	}

	public JSONArray getEmbededClientModules() {
		return embededClientModules;
	}

	public void setEmbededClientModules(JSONArray embededClientModules) {
		this.embededClientModules = embededClientModules;
	}

	public boolean isFullClientEnable() {
		return fullClientEnable;
	}

	public void setFullClientEnable(boolean fullClientEnable) {
		this.fullClientEnable = fullClientEnable;
	}

	public JSONArray getFullClientModules() {
		return fullClientModules;
	}

	public void setFullClientModules(JSONArray fullClientModules) {
		this.fullClientModules = fullClientModules;
	}

	public String getDefaultLayers() {
		return defaultLayers;
	}

	public void setDefaultLayers(String defaultLayers) {
		this.defaultLayers = defaultLayers;
		this.defaultLayersSet = null;
	}

	public boolean isEnable() {
		return enable;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
	}

	public String getGeneratedFileLocation() {
		return generatedFileLocation;
	}

	public void setGeneratedFileLocation(String generatedFileLocation) {
		this.generatedFileLocation = generatedFileLocation;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public JSONObject getManualOverride() {
		return manualOverride;
	}

	public void setManualOverride(JSONObject manualOverride) {
		this.manualOverride = manualOverride;
	}

	public String getLatitude() {
		return latitude;
	}

	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}

	public String getLongitude() {
		return longitude;
	}

	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}

	public String getProjection() {
		return projection;
	}

	public void setProjection(String projection) {
		this.projection = projection;
	}

	public boolean isUseLayerService() {
		return useLayerService;
	}

	public void setUseLayerService(boolean useLayerService) {
		this.useLayerService = useLayerService;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getZoom() {
		return zoom;
	}

	public void setZoom(String zoom) {
		this.zoom = zoom;
	}

	public String getComment() {
		return this.comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public Boolean isOverrideBaseLayers() {
		return overrideBaseLayers;
	}

	public void setOverrideBaseLayers(Boolean overrideBaseLayers) {
		this.overrideBaseLayers = overrideBaseLayers;
	}

	public String getBaseLayers() {
		return baseLayers;
	}

	public void setBaseLayers(String baseLayers) {
		this.baseLayers = baseLayers;
		this.baseLayersSet = null;
	}

	public String getProxyUrl() {
		return proxyUrl;
	}

	public void setProxyUrl(String proxyUrl) {
		this.proxyUrl = proxyUrl;
	}

	public String getLayerInfoServiceUrl() {
		return layerInfoServiceUrl;
	}

	public void setLayerInfoServiceUrl(String layerInfoServiceUrl) {
		this.layerInfoServiceUrl = layerInfoServiceUrl;
	}

	public JSONObject toJSonObjectWithClientUrls(ServletContext context) throws JSONException {
		JSONObject json = this.toJSonObject();
		json.put("clientUrl", this.getClientUrl(context));
		json.put("previewClientUrl", this.getPreviewClientUrl(context));
		return json;
	}

	@Override
	public JSONObject toJSonObject() throws JSONException {
		JSONObject json = super.toJSonObject();
		json.put("manualOverride", Utils.jsonToStr(this.manualOverride));
		return json;
	}

	// Helper
	public String getClientUrl(ServletContext context) {
		return FileFinder.getAtlasMapperClientURL(context, this, false);
	}

	// Helper
	public String getPreviewClientUrl(ServletContext context) {
		return FileFinder.getAtlasMapperClientURL(context, this, true);
	}

	// Helper
	public boolean isBaseLayer(String layerId) {
		String baselayersStr = this.getBaseLayers();
		if (Utils.isBlank(layerId) || Utils.isBlank(baselayersStr)) {
			return false;
		}

		if (this.baseLayersSet == null) {
			this.baseLayersSet = new HashSet<String>();
			String[] baselayers = baselayersStr.split(SPLIT_PATTERN);
			if (baselayers != null) {
				for (int i=0; i<baselayers.length; i++) {
					String baselayer = baselayers[i];
					if (Utils.isNotBlank(baselayer)) {
						this.baseLayersSet.add(baselayer.trim());
					}
				}
			}
		}

		return this.baseLayersSet.contains(layerId);
	}

	// Helper
	public Set<String> getDefaultLayersSet() {
		if (this.defaultLayersSet == null) {
			this.defaultLayersSet = new HashSet<String>();
			String defaultlayersStr = this.getDefaultLayers();
			if (Utils.isNotBlank(defaultlayersStr)) {
				String[] defaultlayers = defaultlayersStr.split(SPLIT_PATTERN);
				if (defaultlayers != null) {
					for (int i=0; i<defaultlayers.length; i++) {
						String defaultlayer = defaultlayers[i];
						if (Utils.isNotBlank(defaultlayer)) {
							this.defaultLayersSet.add(defaultlayer.trim());
						}
					}
				}
			}
		}

		return this.defaultLayersSet;
	}

	// Helper
	public boolean useGoogle(ConfigManager configManager) throws JSONException, FileNotFoundException {
		JSONArray datasourcesArray = this.getDatasources();
		if (datasourcesArray != null) {
			for (int i=0; i < datasourcesArray.length(); i++) {
				String clientDatasourceId = datasourcesArray.optString(i, null);
				if (Utils.isNotBlank(clientDatasourceId)) {
					DatasourceConfig datasourceConfig =
							configManager.getDatasourceConfigs().get2(clientDatasourceId);
					if (datasourceConfig != null && "GOOGLE".equalsIgnoreCase(datasourceConfig.getDatasourceType())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	// Helper
	public List<DatasourceConfig> getDatasourceConfigs(ConfigManager configManager) throws JSONException, FileNotFoundException {
		List<DatasourceConfig> datasourceConfigs = new ArrayList<DatasourceConfig>();
		JSONArray datasourcesArray = this.getDatasources();
		if (datasourcesArray != null) {
			for (int i=0; i < datasourcesArray.length(); i++) {
				String clientDatasourceId = datasourcesArray.optString(i, null);
				if (Utils.isNotBlank(clientDatasourceId)) {
					DatasourceConfig datasourceConfig =
							configManager.getDatasourceConfigs().get2(clientDatasourceId);
					if (datasourceConfig != null) {
						datasourceConfigs.add(datasourceConfig);
					}
				}
			}
		}
		return datasourceConfigs;
	}

	// Helper
	public Map<String, LayerConfig> getLayerConfigs(ConfigManager configManager) throws MalformedURLException, IOException, ServiceException, JSONException {
		Map<String, LayerConfig> overridenLayerConfigs = new HashMap<String, LayerConfig>();

		for (DatasourceConfig datasourceConfig : this.getDatasourceConfigs(configManager)) {
			if (datasourceConfig != null) {
				overridenLayerConfigs.putAll(
						datasourceConfig.getLayerConfigs(this));
			}
		}

		// Create manual layers defined for this client
		JSONObject clientOverrides = this.manualOverride;
		if (clientOverrides != null && clientOverrides.length() > 0) {
			Iterator<String> layerIds = clientOverrides.keys();
			while (layerIds.hasNext()) {
				String layerId = layerIds.next();
				if (!overridenLayerConfigs.containsKey(layerId)) {
					JSONObject jsonClientOverride = clientOverrides.optJSONObject(layerId);
					if (jsonClientOverride != null && jsonClientOverride.length() > 0) {
						LayerConfig manualLayer = new LayerConfig(jsonClientOverride);
						manualLayer.setLayerId(layerId);

						overridenLayerConfigs.put(
								manualLayer.getLayerId(),
								manualLayer);
					}
				}
			}
		}

		return overridenLayerConfigs;
	}
}
