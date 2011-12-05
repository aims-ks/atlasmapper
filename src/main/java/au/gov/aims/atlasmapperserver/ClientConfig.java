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
	// Grids records must have an unmutable ID
	@ConfigField
	private Integer id;

	@ConfigField(name="default", getter="isDefault", setter="setDefault")
	private Boolean _default;

	@ConfigField
	private String clientId;

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
	private JSONObject manualOverride;

	@ConfigField
	private String legendParameters;
	// Cache - avoid parsing legendParameters string every times.
	private JSONObject legendParametersJson;

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
	// Cache - avoid parsing baseLayers string every times.
	private Set<String> baseLayersSet = null;

	@ConfigField
	private String proxyUrl;

	@ConfigField
	private String theme;

	// Read only values also need to be disabled in the form (clientsConfigPage.js)
	@ConfigField(demoReadOnly = true)
	private String generatedFileLocation;

	@ConfigField(demoReadOnly = true)
	private String baseUrl;

	@ConfigField(demoReadOnly = true)
	private String layerInfoServiceUrl;

	public ClientConfig(ConfigManager configManager) {
		super(configManager);
	}

	@Override
	public void setJSONObjectKey(String key) {
		if (Utils.isBlank(this.clientId)) {
			this.clientId = key;
		}
	}

	@Override
	public String getJSONObjectKey() {
		return this.clientId;
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

	public String getClientId() {
		// Error protection against erronous manual config file edition
		if (this.clientId == null) {
			if (this.clientName != null) {
				return this.clientName;
			}
			if (this.id != null) {
				return this.id.toString();
			}
		}
		return this.clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
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

	public JSONObject getManualOverride() {
		return manualOverride;
	}

	public void setManualOverride(JSONObject manualOverride) {
		this.manualOverride = manualOverride;
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

	public String getTheme() {
		return this.theme;
	}

	public void setTheme(String theme) {
		this.theme = theme;
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
	public Map<String, LayerConfig> getLayerConfigs(ConfigManager configManager) throws MalformedURLException, IOException, ServiceException, JSONException, GetCapabilitiesExceptions {
		Map<String, LayerConfig> overridenLayerConfigs = new HashMap<String, LayerConfig>();

		// Retrieved all layers for all datasources of this client
		GetCapabilitiesExceptions errors = new GetCapabilitiesExceptions();
		for (DatasourceConfig datasourceConfig : this.getDatasourceConfigs(configManager)) {
			try {
				if (datasourceConfig != null) {
					overridenLayerConfigs.putAll(
							datasourceConfig.getLayerConfigs(this));
				}
			} catch(IOException ex) {
				// Collect all errors
				errors.add(datasourceConfig, ex);
			}
		}

		if (!errors.isEmpty()) {
			throw errors;
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
						LayerConfig manualLayer = new LayerConfig(this.getConfigManager(), jsonClientOverride);
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
