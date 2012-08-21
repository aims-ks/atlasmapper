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
import au.gov.aims.atlasmapperserver.dataSourceConfig.AbstractDataSourceConfig;
import au.gov.aims.atlasmapperserver.layerConfig.AbstractLayerConfig;
import au.gov.aims.atlasmapperserver.layerConfig.GroupLayerConfig;
import au.gov.aims.atlasmapperserver.layerConfig.LayerCatalog;
import au.gov.aims.atlasmapperserver.servlet.FileFinder;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONSortedObject;

/**
 *
 * @author glafond
 */
public class ClientConfig extends AbstractConfig {
	private static final Logger LOGGER = Logger.getLogger(ClientConfig.class.getName());

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
	private String welcomeMsg;

	@ConfigField
	private JSONArray dataSources;

	@ConfigField
	private boolean mainClientEnable;

	@ConfigField
	private JSONArray mainClientModules;

	@ConfigField
	private boolean embeddedClientEnable;

	@ConfigField
	private JSONArray embeddedClientModules;

	@ConfigField
	private JSONSortedObject manualOverride;

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
	private List<String> defaultLayersList = null;

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

	@ConfigField
	private String pageHeader;

	@ConfigField
	private String pageFooter;

	@ConfigField
	private String layersPanelHeader;

	@ConfigField
	private String layersPanelFooter;


	@ConfigField
	private String listPageHeader;

	@ConfigField
	private String listPageFooter;

	@ConfigField
	private String listBaseLayerServiceUrl;

	@ConfigField
	private String listBaseLayerId;

	@ConfigField
	private String listLayerImageWidth;

	@ConfigField
	private String listLayerImageHeight;


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

	// LayerCatalog - Before data source overrides
	private LayerCatalog getRawLayerCatalog() throws GetCapabilitiesExceptions, FileNotFoundException, JSONException {
		LayerCatalog rawLayerCatalog = new LayerCatalog();

		// Retrieved all layers for all data sources of this client
		GetCapabilitiesExceptions errors = new GetCapabilitiesExceptions();

		for (AbstractDataSourceConfig dataSource : this.getDataSourceConfigs()) {
			try {
				if (dataSource != null) {
					rawLayerCatalog.addLayers(dataSource.getLayerCatalog().getLayers());
				}
			} catch(Exception ex) {
				// Collect all errors
				errors.add(dataSource, ex);
			}
		}

		if (!errors.isEmpty()) {
			throw errors;
		}

		return rawLayerCatalog;
	}

	// LayerCatalog - After data source overrides
	public LayerCatalog getLayerCatalog() throws GetCapabilitiesExceptions, FileNotFoundException, JSONException {
		LayerCatalog rawLayerCatalog = this.getRawLayerCatalog();

		// Map of layers, after overrides, used to create the final layer catalog
		HashMap<String, AbstractLayerConfig> layersMap = new HashMap<String, AbstractLayerConfig>();

		JSONObject clientOverrides = this.manualOverride;

		// Apply manual overrides, if needed
		if (!rawLayerCatalog.isEmpty()) {
			for (AbstractLayerConfig layerConfig : rawLayerCatalog.getLayers()) {
				if (layerConfig != null) {
					AbstractLayerConfig overriddenLayerConfig =
							layerConfig.applyOverrides(clientOverrides);
					layersMap.put(
							overriddenLayerConfig.getLayerId(),
							overriddenLayerConfig);
				}
			}
		}

		// Create manual layers defined for this client
		if (clientOverrides != null && clientOverrides.length() > 0) {
			Iterator<String> layerIds = clientOverrides.keys();
			while (layerIds.hasNext()) {
				String layerId = layerIds.next();
				if (!layersMap.containsKey(layerId)) {
					JSONObject jsonClientOverride = clientOverrides.optJSONObject(layerId);
					if (jsonClientOverride != null && jsonClientOverride.length() > 0) {
						try {
							String dataSourceId = jsonClientOverride.optString("dataSourceId");
							String dataSourceType = jsonClientOverride.optString("dataSourceType");

							AbstractDataSourceConfig dataSource = null;
							if (Utils.isNotBlank(dataSourceId)) {
								dataSource = this.getDataSourceConfig(dataSourceId);

								if (dataSource == null) {
									LOGGER.log(Level.WARNING, "The manual override for the layer {0} of the client {1} is defining an invalid data source {2}.",
											new String[]{layerId, this.getClientName(), dataSourceId});
									continue;
								}
							}

							if (Utils.isBlank(dataSourceType)) {
								if (dataSource != null) {
									dataSourceType = dataSource.getDataSourceType();
								} else {
									LOGGER.log(Level.WARNING, "The manual override for the layer {0} of the client {1} do not exists and can not be created because it do not define its data source type.",
											new String[]{layerId, this.getClientName(), dataSourceId});
									continue;
								}
							}

							AbstractLayerConfig manualLayer = LayerCatalog.createLayer(
									dataSourceType, jsonClientOverride, this.getConfigManager());

							manualLayer.setLayerId(layerId);

							if (dataSource != null) {
								dataSource.bindLayer(manualLayer);
							}

							layersMap.put(
									manualLayer.getLayerId(),
									manualLayer);
						} catch(Exception ex) {
							LOGGER.log(Level.SEVERE, "Unexpected error occurred while parsing the following layer override for the client ["+this.getClientName()+"]:\n" + jsonClientOverride.toString(4), ex);
						}
					}
				}
			}
		}

		// Add layer group content in the description, and hide children layers from the catalog (the add layer tree).
		// I.E. Layers that are present in a layer group are not shown in the tree. This can be overridden by
		//     setting "shownOnlyInLayerGroup" to true in the layers overrides.
		for (AbstractLayerConfig layer : layersMap.values()) {
			String[] layers = null;
			if (layer instanceof GroupLayerConfig) {
				layers = ((GroupLayerConfig)layer).getLayers();
			}

			if (layers != null && layers.length > 0) {
				String layerGroupHTMLList = this.getHTMLListAndHideChildrenLayers(layersMap, layers);
				if (Utils.isNotBlank(layerGroupHTMLList)) {
					StringBuilder groupHtmlDescription = new StringBuilder();
					groupHtmlDescription.append("<div class=\"descriptionLayerGroupContent\">");
					groupHtmlDescription.append("This layer regroup the following list of layers:");
					groupHtmlDescription.append(layerGroupHTMLList);
					groupHtmlDescription.append("</div>");

					layer.setHtmlDescription(groupHtmlDescription.toString());
				}
			}
		}

		// Set base layer attribute
		for (AbstractLayerConfig layerConfig : layersMap.values()) {
			// Set Baselayer flag if the layer is defined as a base layer in the client OR the client do not define any base layers and the layer is defined as a baselayer is the global config
			if (this.isBaseLayer(layerConfig.getLayerId())) {
				layerConfig.setIsBaseLayer(true);
			} else if (this.isBaseLayer(layerConfig.getLayerName())) {
				// Backward compatibility
				LOGGER.log(Level.WARNING, "DEPRECATED LAYER ID USED FOR BASE LAYERS: Layer id [{0}] should be [{1}].", new String[]{layerConfig.getLayerName(), layerConfig.getLayerId()});
				layerConfig.setIsBaseLayer(true);
			}
		}

		// LayerCatalog after overrides
		LayerCatalog layerCatalog = new LayerCatalog();
		layerCatalog.addLayers(layersMap.values());

		return layerCatalog;
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
		return this.id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Boolean isDefault() {
		return this._default;
	}

	public void setDefault(Boolean _default) {
		this._default = _default;
	}

	public boolean isBaseLayersInTab() {
		return this.baseLayersInTab;
	}

	public void setBaseLayersInTab(boolean baseLayersInTab) {
		this.baseLayersInTab = baseLayersInTab;
	}

	public String getClientId() {
		// Error protection against erroneous manual config file edition
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
		return this.clientName;
	}

	public void setClientName(String clientName) {
		this.clientName = clientName;
	}

	public String getWelcomeMsg() {
		return this.welcomeMsg;
	}

	public void setWelcomeMsg(String welcomeMsg) {
		this.welcomeMsg = welcomeMsg;
	}

	public JSONArray getDataSources() {
		return this.dataSources;
	}

	public void setDataSources(JSONArray dataSources) {
		this.dataSources = dataSources;
	}

	public boolean isEmbeddedClientEnable() {
		return this.embeddedClientEnable;
	}

	public void setEmbeddedClientEnable(boolean embeddedClientEnable) {
		this.embeddedClientEnable = embeddedClientEnable;
	}

	public JSONArray getEmbeddedClientModules() {
		return this.embeddedClientModules;
	}

	public void setEmbeddedClientModules(JSONArray embeddedClientModules) {
		this.embeddedClientModules = embeddedClientModules;
	}

	public boolean isMainClientEnable() {
		return this.mainClientEnable;
	}

	public void setMainClientEnable(boolean mainClientEnable) {
		this.mainClientEnable = mainClientEnable;
	}

	public JSONArray getMainClientModules() {
		return this.mainClientModules;
	}

	public void setMainClientModules(JSONArray mainClientModules) {
		this.mainClientModules = mainClientModules;
	}

	public String getDefaultLayers() {
		return this.defaultLayers;
	}

	public void setDefaultLayers(String defaultLayers) {
		this.defaultLayers = defaultLayers;
		this.defaultLayersList = null;
	}

	public boolean isEnable() {
		return this.enable;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
	}

	public JSONSortedObject getManualOverride() {
		return this.manualOverride;
	}

	public void setManualOverride(JSONSortedObject manualOverride) {
		this.manualOverride = manualOverride;
	}

	public String getLegendParameters() {
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
		return this.latitude;
	}

	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}

	public String getLongitude() {
		return this.longitude;
	}

	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}

	public String getProjection() {
		return this.projection;
	}

	public void setProjection(String projection) {
		this.projection = projection;
	}

	public boolean isUseLayerService() {
		return this.useLayerService;
	}

	public void setUseLayerService(boolean useLayerService) {
		this.useLayerService = useLayerService;
	}

	public String getVersion() {
		return this.version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getZoom() {
		return this.zoom;
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
		return this.overrideBaseLayers;
	}

	public void setOverrideBaseLayers(Boolean overrideBaseLayers) {
		this.overrideBaseLayers = overrideBaseLayers;
	}

	public String getBaseLayers() {
		return this.baseLayers;
	}

	public void setBaseLayers(String baseLayers) {
		this.baseLayers = baseLayers;
		this.baseLayersSet = null;
	}

	public String getProxyUrl() {
		return this.proxyUrl;
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

	public String getPageHeader() {
		return this.pageHeader;
	}

	public void setPageHeader(String pageHeader) {
		this.pageHeader = pageHeader;
	}

	public String getPageFooter() {
		return this.pageFooter;
	}

	public void setPageFooter(String pageFooter) {
		this.pageFooter = pageFooter;
	}

	public String getLayersPanelHeader() {
		return this.layersPanelHeader;
	}

	public void setLayersPanelHeader(String layersPanelHeader) {
		this.layersPanelHeader = layersPanelHeader;
	}

	public String getLayersPanelFooter() {
		return this.layersPanelFooter;
	}

	public void setLayersPanelFooter(String layersPanelFooter) {
		this.layersPanelFooter = layersPanelFooter;
	}


	public String getListPageHeader() {
		return this.listPageHeader;
	}
	public void setListPageHeader(String listPageHeader) {
		this.listPageHeader = listPageHeader;
	}

	public String getListPageFooter() {
		return this.listPageFooter;
	}
	public void setListPageFooter(String listPageFooter) {
		this.listPageFooter = listPageFooter;
	}

	public String getListBaseLayerServiceUrl() {
		return this.listBaseLayerServiceUrl;
	}
	public void setListBaseLayerServiceUrl(String listBaseLayerServiceUrl) {
		this.listBaseLayerServiceUrl = listBaseLayerServiceUrl;
	}

	public String getListBaseLayerId() {
		return this.listBaseLayerId;
	}
	public void setListBaseLayerId(String listBaseLayerId) {
		this.listBaseLayerId = listBaseLayerId;
	}

	public String getListLayerImageWidth() {
		return this.listLayerImageWidth;
	}
	public void setListLayerImageWidth(String listLayerImageWidth) {
		this.listLayerImageWidth = listLayerImageWidth;
	}

	public String getListLayerImageHeight() {
		return this.listLayerImageHeight;
	}
	public void setListLayerImageHeight(String listLayerImageHeight) {
		this.listLayerImageHeight = listLayerImageHeight;
	}


	public String getGeneratedFileLocation() {
		return this.generatedFileLocation;
	}

	public void setGeneratedFileLocation(String generatedFileLocation) {
		this.generatedFileLocation = generatedFileLocation;
	}

	public String getBaseUrl() {
		return this.baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getLayerInfoServiceUrl() {
		return this.layerInfoServiceUrl;
	}

	public void setLayerInfoServiceUrl(String layerInfoServiceUrl) {
		this.layerInfoServiceUrl = layerInfoServiceUrl;
	}


	public JSONObject toJSonObjectWithClientUrls(ServletContext context) throws JSONException {
		JSONObject json = this.toJSonObject();
		json.put("clientUrl", this.getClientUrl(context));
		json.put("previewClientUrl", this.getPreviewClientUrl(context));
		json.put("layerListUrl", this.getLayerListUrl(context));
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
	public String getLayerListUrl(ServletContext context) {
		return FileFinder.getAtlasMapperLayerListUrl(context, this);
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
	public List<String> getDefaultLayersList() {
		if (this.defaultLayersList == null) {
			this.defaultLayersList = new ArrayList<String>();
			String defaultlayersStr = this.getDefaultLayers();
			if (Utils.isNotBlank(defaultlayersStr)) {
				String[] defaultlayers = defaultlayersStr.split(SPLIT_PATTERN);
				if (defaultlayers != null) {
					for (int i=0; i<defaultlayers.length; i++) {
						String defaultlayer = defaultlayers[i];
						if (Utils.isNotBlank(defaultlayer)) {
							this.defaultLayersList.add(defaultlayer.trim());
						}
					}
				}
			}
		}

		return this.defaultLayersList;
	}

	// Helper
	public boolean useGoogle(ConfigManager configManager) throws JSONException, FileNotFoundException {
		JSONArray dataSourcesArray = this.getDataSources();
		if (dataSourcesArray != null) {
			for (int i=0; i < dataSourcesArray.length(); i++) {
				String clientDataSourceId = dataSourcesArray.optString(i, null);
				if (Utils.isNotBlank(clientDataSourceId)) {
					AbstractDataSourceConfig dataSourceConfig =
							configManager.getDataSourceConfigs().get2(clientDataSourceId);
					if (dataSourceConfig != null && "GOOGLE".equalsIgnoreCase(dataSourceConfig.getDataSourceType())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	// Helper
	public List<AbstractDataSourceConfig> getDataSourceConfigs() throws JSONException, FileNotFoundException {
		List<AbstractDataSourceConfig> dataSourceConfigs = new ArrayList<AbstractDataSourceConfig>();
		JSONArray dataSourcesArray = this.getDataSources();
		if (dataSourcesArray != null) {
			for (int i=0; i < dataSourcesArray.length(); i++) {
				String clientDataSourceId = dataSourcesArray.optString(i, null);
				if (Utils.isNotBlank(clientDataSourceId)) {
					AbstractDataSourceConfig dataSourceConfig =
							this.getConfigManager().getDataSourceConfigs().get2(clientDataSourceId);
					if (dataSourceConfig != null) {
						dataSourceConfigs.add(dataSourceConfig);
					}
				}
			}
		}
		return dataSourceConfigs;
	}

	public AbstractDataSourceConfig getDataSourceConfig(String dataSourceId) throws JSONException, FileNotFoundException {
		if (Utils.isBlank(dataSourceId)) {
			return null;
		}

		// Ensure that the client has the requested data source
		boolean valid = false;
		JSONArray dataSourcesArray = this.getDataSources();
		if (dataSourcesArray != null) {
			for (int i=0; i < dataSourcesArray.length(); i++) {
				String clientDataSourceId = dataSourcesArray.optString(i, null);
				if (dataSourceId.equals(clientDataSourceId)) {
					valid = true;
				}
			}
		}
		if (!valid) {
			return null;
		}

		// Get the data source
		AbstractDataSourceConfig dataSourceConfig =
				this.getConfigManager().getDataSourceConfigs().get2(dataSourceId);

		return dataSourceConfig;
	}

	private String getHTMLListAndHideChildrenLayers(Map<String, AbstractLayerConfig> completeLayerMap, String[] layersIds) {
		if (layersIds == null || layersIds.length <= 0) {
			return "";
		}
		StringBuilder htmlList = new StringBuilder();
		htmlList.append("<ul class=\"bullet-list\">");
		for (String childId : layersIds) {
			AbstractLayerConfig child = completeLayerMap.get(childId);
			if (child != null) {
				htmlList.append("<li>");
				String title = child.getTitle();
				if (title != null) {
					title = title.trim();
				}
				if (Utils.isBlank(title)) {
					title = "NO NAME";
				}
				title = Utils.safeHTMLStr(title);
				htmlList.append(title);

				String[] childLayers = null;
				if (child instanceof GroupLayerConfig) {
					childLayers = ((GroupLayerConfig)child).getLayers();
				}

				if (childLayers != null && childLayers.length > 0) {
					htmlList.append(this.getHTMLListAndHideChildrenLayers(completeLayerMap, childLayers));
				}
				htmlList.append("</li>");

				// Hide the children layer from the Catalog
				if (child.isShownOnlyInLayerGroup() == null) {
					child.setShownOnlyInLayerGroup(true);
				}
			}
		}
		htmlList.append("</ul>");
		return htmlList.toString();
	}
}
