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
import au.gov.aims.atlasmapperserver.jsonWrappers.client.ClientWrapper;
import au.gov.aims.atlasmapperserver.jsonWrappers.client.DataSourceWrapper;
import au.gov.aims.atlasmapperserver.jsonWrappers.client.LayerWrapper;
import au.gov.aims.atlasmapperserver.layerConfig.AbstractLayerConfig;
import au.gov.aims.atlasmapperserver.layerConfig.LayerCatalog;
import au.gov.aims.atlasmapperserver.servlet.FileFinder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletContext;

import au.gov.aims.atlasmapperserver.servlet.Proxy;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONSortedObject;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

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
	private String headExtra;

	@ConfigField
	private String attributions;

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
	private boolean showAddRemoveLayerButtons;

	@ConfigField
	private boolean baseLayersInTab;

	@ConfigField
	private String defaultLayers;
	// Cache - avoid parsing baseLayers string every times.
	private List<String> defaultLayersList = null;

	@ConfigField
	private Double version = null;

	@ConfigField
	private boolean useLayerService;


	@ConfigField
	private boolean searchEnabled;

	@ConfigField
	private boolean showGoogleResults;

	@ConfigField
	private boolean showArcGISResults;

	@ConfigField
	private String arcGISSearchUrl;

	@ConfigField
	private boolean showOSMResults;

	@ConfigField
	private String searchServiceUrl;


	@ConfigField
	private boolean printEnabled;

	@ConfigField
	private boolean saveMapEnabled;

	@ConfigField
	private boolean mapConfigEnabled;

	@ConfigField
	private boolean mapMeasurementEnabled;

	@ConfigField
	private boolean mapMeasurementLineEnabled;

	@ConfigField
	private boolean mapMeasurementAreaEnabled;

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

	@ConfigField
	private String[] extraAllowedHosts;


	// Read only values also need to be disabled in the form (clientsConfigPage.js)
	@ConfigField(demoReadOnly = true)
	private String generatedFileLocation;

	@ConfigField(demoReadOnly = true)
	private String baseUrl;

	@ConfigField(demoReadOnly = true)
	private String layerInfoServiceUrl;

	@ConfigField(demoReadOnly = true)
	private String downloadLoggerServiceUrl;


	@ConfigField
	private String lastGenerated;


	public ClientConfig(ConfigManager configManager) {
		super(configManager);
	}

	public Map<String, DataSourceWrapper> loadDataSources() throws FileNotFoundException, JSONException {
		Map<String, DataSourceWrapper> dataSources = new HashMap<String, DataSourceWrapper>();
		JSONArray dataSourcesArray = this.getDataSources();
		if (dataSourcesArray != null) {
			for (int i=0; i < dataSourcesArray.length(); i++) {
				String clientDataSourceId = dataSourcesArray.optString(i, null);
				if (Utils.isNotBlank(clientDataSourceId)) {
					DataSourceWrapper dataSourceWrapper = AbstractDataSourceConfig.load(
							this.getConfigManager().getApplicationFolder(),
							clientDataSourceId);

					dataSources.put(clientDataSourceId, dataSourceWrapper);
				}
			}
		}

		return dataSources;
	}
	public DataSourceWrapper getFirstGoogleDataSource(Map<String, DataSourceWrapper> dataSources) {
		if (dataSources != null) {
			for (DataSourceWrapper dataSourceWrapper : dataSources.values()) {
				if (dataSourceWrapper.isGoogle()) {
					return dataSourceWrapper;
				}
			}
		}
		return null;
	}

	public Errors process(boolean complete) throws Exception {
		// Collect error messages
		Errors clientErrors = new Errors();

		// Load data sources
		Map<String, DataSourceWrapper> dataSources = this.loadDataSources();

		// Find a google data source, if any (to find out if we need to add google support)
		DataSourceWrapper googleDataSource = getFirstGoogleDataSource(dataSources);

		// Check for write access before doing any processing,
		File clientFolder = FileFinder.getClientFolder(this.getConfigManager().getApplicationFolder(), this);
		String tomcatUser = System.getProperty("user.name");
		if (clientFolder.exists()) {
			// The client do exists, check if we have write access to it.
			if (!clientFolder.canWrite()) {
				clientErrors.addError("The client could not be generated; The AtlasMapper do not have write access to the client folder [" + clientFolder.getAbsolutePath() + "]. " +
						"Give write access to the user \"" + tomcatUser + "\" to the client folder and try regenerating the client.");

				// No write access. No need to continue.
				return clientErrors;
			}
		} else {
			// The client do not exists, check if it can be created.
			if (!Utils.recursiveIsWritable(clientFolder)) {
				clientErrors.addError("The client could not be generated; The AtlasMapper can not create the client folder [" + clientFolder.getAbsolutePath() + "]. " +
						"Give write access to the user \"" + tomcatUser + "\" to the parent folder or " +
						"create the client folder manually with write access to the user \"" + tomcatUser + "\" and try regenerating the client.");

				// Inappropriate write access. No need to continue.
				return clientErrors;
			}
		}

		// Get the layer catalog from the data source save state and the client layer overrides.
		DataSourceWrapper layerCatalog = null;
		int nbLayers = 0;
		try {
			layerCatalog = this.getLayerCatalog(dataSources);
			nbLayers = layerCatalog.getLayers() == null ? 0 : layerCatalog.getLayers().length();
		} catch (IOException ex) {
			clientErrors.addError("An IO exception occurred while generating the layer catalog: " + Utils.getExceptionMessage(ex) + "\nSee your server logs.");
			LOGGER.log(Level.SEVERE, "An IO exception occurred while generating the layer catalog: {0}", Utils.getExceptionMessage(ex));
			LOGGER.log(Level.WARNING, "Stack trace: ", ex);

			// No catalog, no need to continue.
			return clientErrors;
		} catch (JSONException ex) {
			clientErrors.addError("A JSON exception occurred while generating the layer catalog: " + Utils.getExceptionMessage(ex) + "\nSee your server logs.");
			LOGGER.log(Level.SEVERE, "A JSON exception occurred while generating the layer catalog: {0}", Utils.getExceptionMessage(ex));
			LOGGER.log(Level.WARNING, "Stack trace: ", ex);

			// No catalog, no need to continue.
			return clientErrors;
		}

		if (nbLayers <= 0) {
			clientErrors.addWarning("The client has no available layers");
		}

		try {
			this.copyClientFilesIfNeeded(complete);
		} catch (IOException ex) {
			// Those error are very unlikely to happen since we already checked the folder write access.
			if (clientFolder.exists()) {
				clientErrors.addError("An unexpected exception occurred while copying the client files to the folder [" + clientFolder.getAbsolutePath() + "]: " + Utils.getExceptionMessage(ex) + "\nSee your server logs.");
				LOGGER.log(Level.SEVERE, "An unexpected exception occurred while copying the client files to the folder [" + clientFolder.getAbsolutePath() + "]: {0}", Utils.getExceptionMessage(ex));
				LOGGER.log(Level.WARNING, "Stack trace: ", ex);
			} else {
				clientErrors.addError("The client could not be generated; The AtlasMapper were not able to create the client folder [" + clientFolder.getAbsolutePath() + "]. " +
						"Give write access to the user \"" + tomcatUser + "\" to the parent folder or " +
						"create the client folder manually with write access to the user \"" + tomcatUser + "\" and try regenerating the client.");
			}

			// There was an error while copying the client files. No need to continue.
			return clientErrors;
		}

		ClientWrapper generatedMainConfig = null;
		ClientWrapper generatedEmbeddedConfig = null;
		JSONObject generatedLayers = null;
		try {
			generatedMainConfig = new ClientWrapper(this.getConfigManager().getClientConfigFileJSon(layerCatalog, dataSources, this, ConfigType.MAIN, true));
			generatedEmbeddedConfig = new ClientWrapper(this.getConfigManager().getClientConfigFileJSon(layerCatalog, dataSources, this, ConfigType.EMBEDDED, true));
			generatedLayers = this.getConfigManager().getClientConfigFileJSon(layerCatalog, dataSources, this, ConfigType.LAYERS, true);

			// Show warning for each default layers that are not defined in the layer catalog.
			List<String> defaultLayerIds = this.getDefaultLayersList();
			if (layerCatalog != null && defaultLayerIds != null && !defaultLayerIds.isEmpty()) {
				JSONObject jsonLayers = layerCatalog.getLayers();
				if (jsonLayers != null) {
					for (String defaultLayerId : defaultLayerIds) {
						if (!jsonLayers.has(defaultLayerId)) {
							clientErrors.addWarning("The layer ID [" + defaultLayerId + "], specified in the default layers, could not be found in the layer catalog.");
						}
					}
				}
			}
		} catch (IOException ex) {
			// Very unlikely to happen
			clientErrors.addError("An IO exception occurred while generating the client config: " + Utils.getExceptionMessage(ex) + "\nSee your server logs.");
			LOGGER.log(Level.SEVERE, "An IO exception occurred while generating the client config: {0}", Utils.getExceptionMessage(ex));
			LOGGER.log(Level.WARNING, "Stack trace: ", ex);

			// No catalog, no need to continue.
			return clientErrors;
		} catch (JSONException ex) {
			// Very unlikely to happen
			clientErrors.addError("A JSON exception occurred while generating the client config: " + Utils.getExceptionMessage(ex) + "\nSee your server logs.");
			LOGGER.log(Level.SEVERE, "A JSON exception occurred while generating the client config: {0}", Utils.getExceptionMessage(ex));
			LOGGER.log(Level.WARNING, "Stack trace: ", ex);

			// No catalog, no need to continue.
			return clientErrors;
		}

		// Transfer layer's errors to the client
		clientErrors.addErrors(layerCatalog.getErrors());
		clientErrors.addWarnings(layerCatalog.getWarnings());
		clientErrors.addMessages(layerCatalog.getMessages());

		// Verify if there is error (it may contains only warnings)
		if (clientErrors.getErrors().isEmpty()) {
			try {
				this.generateTemplateFiles(layerCatalog, generatedMainConfig, googleDataSource);
				this.saveGeneratedConfigs(generatedMainConfig, generatedEmbeddedConfig, generatedLayers);

				// Flush the proxy cache
				Proxy.reloadConfig(generatedMainConfig, generatedLayers, this);

				this.setLastGeneratedDate(new Date());
				// Write the changes to disk
				this.getConfigManager().saveServerConfig();
			} catch (TemplateException ex) {
				// May happen if a template is modified.
				clientErrors.addError("Can not process the client templates: " + Utils.getExceptionMessage(ex) + "\nSee your server logs.");
				LOGGER.log(Level.SEVERE, "Can not process the client templates: {0}", Utils.getExceptionMessage(ex));
				LOGGER.log(Level.WARNING, "Stack trace: ", ex);
			} catch (IOException ex) {
				// May happen if a template is modified.
				clientErrors.addError("An IO exception occurred while generating the client config: " + Utils.getExceptionMessage(ex) + "\nSee your server logs.");
				LOGGER.log(Level.SEVERE, "An IO exception occurred while generating the client config: {0}", Utils.getExceptionMessage(ex));
				LOGGER.log(Level.WARNING, "Stack trace: ", ex);
			} catch (JSONException ex) {
				// Very unlikely to happen
				clientErrors.addError("A JSON exception occurred while generating the client config: " + Utils.getExceptionMessage(ex) + "\nSee your server logs.");
				LOGGER.log(Level.SEVERE, "A JSON exception occurred while generating the client config: {0}", Utils.getExceptionMessage(ex));
				LOGGER.log(Level.WARNING, "Stack trace: ", ex);
			}
		}

		// Generation - Conclusion message
		if (clientErrors.getErrors().isEmpty()) {
			if (clientErrors.getWarnings().isEmpty()) {
				clientErrors.addMessage("Client generated successfully.");
			} else {
				clientErrors.addMessage("Client generation passed.");
			}
			clientErrors.addMessage("The client has " + nbLayers + " layer" + (nbLayers > 1 ? "s" : "") + " available.");
		} else {
			clientErrors.addMessage("Client generation failed.");
		}

		return clientErrors;
	}

	// LayerCatalog - After data source overrides
	public DataSourceWrapper getLayerCatalog(Map<String, DataSourceWrapper> dataSources) throws IOException, JSONException {
		DataSourceWrapper layerCatalog = new DataSourceWrapper();

		JSONObject clientOverrides = this.manualOverride;

		// Apply manual overrides, if needed, and add the layer to the catalog
		if (dataSources != null && !dataSources.isEmpty()) {
			for (Map.Entry<String, DataSourceWrapper> dataSourceEntry : dataSources.entrySet()) {
				String dataSourceId = dataSourceEntry.getKey();
				DataSourceWrapper dataSourceWrapper = dataSourceEntry.getValue();
				if (dataSourceWrapper == null) {
					layerCatalog.addWarning("Could not add the data source [" + dataSourceId + "] because it has never been generated.");
				} else {
					JSONObject rawLayers = dataSourceWrapper.getLayers();
					if (rawLayers != null && rawLayers.length() > 0) {
						Iterator<String> rawLayerIds = rawLayers.keys();
						while (rawLayerIds.hasNext()) {
							String rawLayerId = rawLayerIds.next();

							if (clientOverrides != null && clientOverrides.has(rawLayerId) && clientOverrides.optJSONObject(rawLayerId) == null) {
								layerCatalog.addWarning("Invalid manual override for layer: " + rawLayerId);
							}

							LayerWrapper layerWrapper = new LayerWrapper(rawLayers.optJSONObject(rawLayerId));
							// Associate the layer to its data source (NOTE: This property may already been overridden)
							if (layerWrapper.getDataSourceId() == null) {
								layerWrapper.setDataSourceId(dataSourceId);
							}
							layerCatalog.addLayer(rawLayerId, AbstractLayerConfig.applyGlobalOverrides(rawLayerId, layerWrapper, clientOverrides));
						}
					}
				}
			}
		}

		// Create manual layers defined for this client
		if (clientOverrides != null && clientOverrides.length() > 0) {
			Iterator<String> layerIds = clientOverrides.keys();
			JSONObject layers = layerCatalog.getLayers();
			if (layers == null) {
				layers = new JSONObject();
				layerCatalog.setLayers(layers);
			}
			while (layerIds.hasNext()) {
				String layerId = layerIds.next();
				if (layers.isNull(layerId)) {
					LayerWrapper jsonClientLayerOverride = new LayerWrapper(clientOverrides.optJSONObject(layerId));
					if (jsonClientLayerOverride.getJSON() == null) {
						layerCatalog.addWarning("Invalid manual override for new layer: " + layerId);
					} else {
						try {
							DataSourceWrapper dataSource = null;
							String dataSourceId = jsonClientLayerOverride.getDataSourceId();
							String layerType = jsonClientLayerOverride.getLayerType();

							if (Utils.isNotBlank(dataSourceId)) {
								dataSource = dataSources.get(dataSourceId);

								if (dataSource == null) {
									layerCatalog.addWarning("Invalid manual override for new layer: " + layerId);
									LOGGER.log(Level.WARNING, "The manual override for the new layer {0} of the client {1} is defining an invalid data source {2}.",
											new String[]{layerId, this.getClientName(), dataSourceId});
									continue;
								}
							}

							if (Utils.isBlank(layerType)) {
								if (dataSource != null) {
									layerType = dataSource.getLayerType();
								} else {
									layerCatalog.addWarning("Invalid manual override for new layer: " + layerId);
									LOGGER.log(Level.WARNING, "The manual override for the new layer {0} of the client {1} can not be created because it do not define its data source type.",
											new String[]{layerId, this.getClientName()});
									continue;
								}
							}

							AbstractLayerConfig manualLayer = LayerCatalog.createLayer(
									layerType, jsonClientLayerOverride, this.getConfigManager());

							LayerWrapper layerWrapper = new LayerWrapper(manualLayer.toJSonObject());

							if (dataSource != null) {
								layerWrapper.setDataSourceId(dataSource.getDataSourceId());
							}

							layers.put(
									layerId,
									layerWrapper.getJSON());
						} catch(Exception ex) {
							layerCatalog.addError("Unexpected error occurred while parsing the layer override for the layer: " + layerId);
							LOGGER.log(Level.SEVERE, "Unexpected error occurred while parsing the following layer override for the client [{0}]: {1}\n{2}",
									new String[]{this.getClientName(), Utils.getExceptionMessage(ex), jsonClientLayerOverride.getJSON().toString(4)});
							LOGGER.log(Level.FINE, "Stack trace: ", ex);
						}
					}
				}
			}
		}

		// Set some layer attributes
		JSONObject layers = layerCatalog.getLayers();
		if (layers != null) {
			Iterator<String> layerIds = layers.keys();
			while (layerIds.hasNext()) {
				String layerId = layerIds.next();
				if (!layers.isNull(layerId)) {
					LayerWrapper layerWrapper = new LayerWrapper(layers.optJSONObject(layerId));

					// Add layer group content in the description, and set flag to hide children layers in the layer tree (Add layer window).
					// I.E. Layers that are present in a layer group are not shown in the tree. This can be overridden by
					//     setting "shownOnlyInLayerGroup" to true in the layers overrides.
					if (layerWrapper.isGroup()) {
						JSONArray childrenLayers = layerWrapper.getLayers();
						if (childrenLayers != null && childrenLayers.length() > 0) {
							String layerGroupHTMLList = this.getHTMLListAndHideChildrenLayers(layers, childrenLayers);

							if (Utils.isNotBlank(layerGroupHTMLList)) {
								StringBuilder groupHtmlDescription = new StringBuilder();
								groupHtmlDescription.append("This layer regroup the following list of layers:");
								groupHtmlDescription.append(layerGroupHTMLList);

								layerWrapper.setSystemDescription(groupHtmlDescription.toString());
							}
						}
					}

					// Set base layer attribute
					if (this.isBaseLayer(layerId)) {
						layerWrapper.setIsBaseLayer(true);
					} else {
						String layerName = layerWrapper.getLayerName();
						if (this.isBaseLayer(layerName)) {
							// Backward compatibility
							layerCatalog.addError("Deprecated layer ID used for base layers of client " + this.getClientName() + ": " +
									"layer id [" + layerName + "] should be [" + layerId + "]");
							LOGGER.log(Level.WARNING, "DEPRECATED LAYER ID USED FOR BASE LAYERS OF CLIENT {0}: Layer id [{1}] should be [{2}].",
									new String[]{this.getClientName(), layerName, layerId});
							layerWrapper.setIsBaseLayer(true);
						}
					}
				}
			}
		}

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

	public boolean isShowAddRemoveLayerButtons() {
		return this.showAddRemoveLayerButtons;
	}

	public void setShowAddRemoveLayerButtons(boolean showAddRemoveLayerButtons) {
		this.showAddRemoveLayerButtons = showAddRemoveLayerButtons;
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

	public String getHeadExtra() {
		return this.headExtra;
	}

	public void setHeadExtra(String headExtra) {
		this.headExtra = headExtra;
	}

	public String getAttributions() {
		return this.attributions;
	}

	public void setAttributions(String attributions) {
		this.attributions = attributions;
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

	public boolean isSearchEnabled() {
		return this.searchEnabled;
	}

	public void setSearchEnabled(boolean searchEnabled) {
		this.searchEnabled = searchEnabled;
	}

	public boolean isShowGoogleResults() {
		return this.showGoogleResults;
	}

	public void setShowGoogleResults(boolean showGoogleResults) {
		this.showGoogleResults = showGoogleResults;
	}

	public boolean isShowArcGISResults() {
		return this.showArcGISResults;
	}

	public void setShowArcGISResults(boolean showArcGISResults) {
		this.showArcGISResults = showArcGISResults;
	}

	public boolean isShowOSMResults() {
		return this.showOSMResults;
	}

	public void setShowOSMResults(boolean showOSMResults) {
		this.showOSMResults = showOSMResults;
	}

	public boolean isPrintEnabled() {
		return this.printEnabled;
	}

	public void setPrintEnabled(boolean printEnabled) {
		this.printEnabled = printEnabled;
	}

	public boolean isSaveMapEnabled() {
		return this.saveMapEnabled;
	}

	public void setSaveMapEnabled(boolean saveMapEnabled) {
		this.saveMapEnabled = saveMapEnabled;
	}

	public boolean isMapConfigEnabled() {
		return this.mapConfigEnabled;
	}

	public void setMapConfigEnabled(boolean mapConfigEnabled) {
		this.mapConfigEnabled = mapConfigEnabled;
	}

	public boolean isMapMeasurementEnabled() {
		return this.mapMeasurementEnabled;
	}

	public void setMapMeasurementEnabled(boolean mapMeasurementEnabled) {
		this.mapMeasurementEnabled = mapMeasurementEnabled;
	}

	public boolean isMapMeasurementLineEnabled() {
		return this.mapMeasurementLineEnabled;
	}

	public void setMapMeasurementLineEnabled(boolean mapMeasurementLineEnabled) {
		this.mapMeasurementLineEnabled = mapMeasurementLineEnabled;
	}

	public boolean isMapMeasurementAreaEnabled() {
		return this.mapMeasurementAreaEnabled;
	}

	public void setMapMeasurementAreaEnabled(boolean mapMeasurementAreaEnabled) {
		this.mapMeasurementAreaEnabled = mapMeasurementAreaEnabled;
	}

	public Double getVersion() {
		return this.version;
	}

	public void setVersion(Double version) {
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

	public String[] getExtraAllowedHosts() {
		return this.extraAllowedHosts;
	}
	public void setExtraAllowedHosts(String[] rawExtraAllowedHosts) {
		if (rawExtraAllowedHosts == null || rawExtraAllowedHosts.length <= 0) {
			this.extraAllowedHosts = null;
		} else {
			List<String> extraAllowedHosts = new ArrayList<String>(rawExtraAllowedHosts.length);
			for (String extraAllowedHost : rawExtraAllowedHosts) {
				// When the value come from the form (or an old config file), it's a coma separated String instead of an Array
				Pattern regex = Pattern.compile(".*" + SPLIT_PATTERN + ".*", Pattern.DOTALL);
				if (regex.matcher(extraAllowedHost).matches()) {
					for (String splitHost : extraAllowedHost.split(SPLIT_PATTERN)) {
						extraAllowedHosts.add(splitHost.trim());
					}
				} else {
					extraAllowedHosts.add(extraAllowedHost.trim());
				}
			}
			this.extraAllowedHosts = extraAllowedHosts.toArray(new String[extraAllowedHosts.size()]);
		}
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

	public String getDownloadLoggerServiceUrl() {
		return this.downloadLoggerServiceUrl;
	}

	public void setDownloadLoggerServiceUrl(String downloadLoggerServiceUrl) {
		this.downloadLoggerServiceUrl = downloadLoggerServiceUrl;
	}

	public String getSearchServiceUrl() {
		return this.searchServiceUrl;
	}

	public void setSearchServiceUrl(String searchServiceUrl) {
		this.searchServiceUrl = searchServiceUrl;
	}


	public String getArcGISSearchUrl() {
		return this.arcGISSearchUrl;
	}

	public void setArcGISSearchUrl(String arcGISSearchUrl) {
		this.arcGISSearchUrl = arcGISSearchUrl;
	}


	public String getLastGenerated() {
		if (this.lastGenerated == null || this.lastGenerated.isEmpty()) {
			return "Unknown";
		}
		return this.lastGenerated;
	}

	public void setLastGenerated(String lastGenerated) {
		this.lastGenerated = lastGenerated;
	}

	public void setLastGeneratedDate(Date lastGenerated) {
		this.setLastGenerated(
				lastGenerated == null ? null : ConfigManager.DATE_FORMATER.format(lastGenerated));
	}


	public JSONObject toJSonObjectWithClientUrls(ServletContext context) throws JSONException {
		ClientWrapper jsonClient = new ClientWrapper(this.toJSonObject());
		jsonClient.setClientUrl(this.getClientUrl(context));
		jsonClient.setLayerListUrl(this.getLayerListUrl(context));
		return jsonClient.getJSON();
	}

	@Override
	public JSONObject toJSonObject() throws JSONException {
		ClientWrapper jsonClient = new ClientWrapper(super.toJSonObject());
		jsonClient.setManualOverride(Utils.jsonToStr(this.manualOverride));
		return jsonClient.getJSON();
	}

	// Helper
	public String getClientUrl(ServletContext context) {
		return FileFinder.getAtlasMapperClientURL(context, this);
	}

	// Helper
	public String getLayerListUrl(ServletContext context) {
		return FileFinder.getAtlasMapperLayerListUrl(context, this);
	}

	// Helper
	public boolean isBaseLayer(String layerId) {
		String baseLayersStr = this.getBaseLayers();
		if (Utils.isBlank(layerId) || Utils.isBlank(baseLayersStr)) {
			return false;
		}

		if (this.baseLayersSet == null) {
			this.baseLayersSet = new HashSet<String>();
			String[] baseLayers = baseLayersStr.split(SPLIT_PATTERN);
			if (baseLayers != null) {
				for (int i=0; i<baseLayers.length; i++) {
					String baseLayer = baseLayers[i];
					if (Utils.isNotBlank(baseLayer)) {
						this.baseLayersSet.add(baseLayer.trim());
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
			String defaultLayersStr = this.getDefaultLayers();
			if (Utils.isNotBlank(defaultLayersStr)) {
				String[] defaultLayers = defaultLayersStr.split(SPLIT_PATTERN);
				if (defaultLayers != null) {
					for (String defaultLayer : defaultLayers) {
						if (Utils.isNotBlank(defaultLayer)) {
							this.defaultLayersList.add(defaultLayer.trim());
						}
					}
				}
			}
		}

		return this.defaultLayersList;
	}

	private String getHTMLListAndHideChildrenLayers(JSONObject layers, JSONArray childrenLayersIds) throws JSONException {
		if (childrenLayersIds == null || childrenLayersIds.length() <= 0) {
			return "";
		}
		StringBuilder htmlList = new StringBuilder();
		htmlList.append("<ul class=\"bullet-list\">");
		for (int i=0, len=childrenLayersIds.length(); i<len; i++) {
			String childId = childrenLayersIds.optString(i, null);
			if (childId != null) {
				LayerWrapper child = new LayerWrapper(layers.optJSONObject(childId));
				htmlList.append("<li>");
				String title = child.getTitle(child.getLayerName());
				if (title != null) {
					title = title.trim();
				}
				if (Utils.isBlank(title)) {
					title = "NO NAME";
				}
				title = Utils.safeHTMLStr(title);
				htmlList.append(title);

				if (child.isGroup()) {
					JSONArray childrenLayers = child.getLayers();
					if (childrenLayers != null && childrenLayers.length() > 0) {
						htmlList.append(this.getHTMLListAndHideChildrenLayers(layers, childrenLayers));
					}
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


	public JSONObject locationSearch(String query, String mapBounds, int offset, int qty) throws JSONException, IOException, TransformException, FactoryException, URISyntaxException {
		if (Utils.isBlank(query) || qty <= 0) {
			return null;
		}

		// If the query is not in UTF-8, it's probably a server config problem:
		// add the following property to all your connectors, in server.xml: URIEncoding="UTF-8"
		String encodedQuery = URLEncoder.encode(query.trim(), "UTF-8");

		// The results are sorted alphabetically (order by id for same title)
		TreeSet<JSONObject> resultsSet = new TreeSet<JSONObject>(new Comparator<JSONObject>() {
			@Override
			public int compare(JSONObject o1, JSONObject o2) {
				String title1 = Utils.getComparableTitle(o1.optString("title", ""));
				String title2 = Utils.getComparableTitle(o2.optString("title", ""));

				int cmp = title1.compareToIgnoreCase(title2);

				if (cmp == 0) {
					String id1 = o1.optString("id", "");
					String id2 = o2.optString("id", "");

					cmp = id1.compareTo(id2);
				}

				return cmp;
			}
		});

		if (this.isShowGoogleResults()) {
			List<JSONObject> googleResults = LocationSearch.googleSearch(encodedQuery, mapBounds);
			if (googleResults != null && !googleResults.isEmpty()) {
				resultsSet.addAll(googleResults);
			}
		}

		if (this.isShowOSMResults()) {
			List<JSONObject> osmNominatimResults = LocationSearch.osmNominatimSearch(encodedQuery, mapBounds);
			if (osmNominatimResults != null && !osmNominatimResults.isEmpty()) {
				resultsSet.addAll(osmNominatimResults);
			}
		}

		String arcGISSearchUrl = this.getArcGISSearchUrl();
		if (this.isShowArcGISResults() && Utils.isNotBlank(arcGISSearchUrl)) {
			List<JSONObject> arcGISResults = LocationSearch.arcGISSearch(arcGISSearchUrl, encodedQuery, mapBounds);
			if (arcGISResults != null && !arcGISResults.isEmpty()) {
				resultsSet.addAll(arcGISResults);
			}
		}

		JSONObject[] results = resultsSet.toArray(new JSONObject[resultsSet.size()]);

		// The server can not always return what the user ask...
		// If the user ask for the Xth page of a search that now
		// returns less than X pages, the server will jump to the
		// first page (very rare case).
		if (offset >= results.length) {
			offset = 0;
		}

		int to = offset + qty;
		if (to > results.length) {
			to = results.length;
		}

		// TODO Use bounds (and maybe other parameters) to order the results by pertinence.

		JSONObject[] subResults = null;
		// TODO Use something else than Arrays.copyOfRange (it's java 6 only...)
		subResults = Arrays.copyOfRange(results, offset, to);

		return new JSONObject()
				.put("length", results.length)
				.put("offset", offset)
				.put("results", subResults);
	}

	/**
	 * LayerFound: {
	 *     layerId: 'ea_...',
	 *     title: '...',
	 *     excerpt: '...',
	 *     rank: 0
	 * }
	 * @param query
	 * @param offset
	 * @param qty
	 * @return
	 * @throws JSONException
	 * @throws IOException
	 */
	public JSONObject layerSearch(String query, int offset, int qty) throws JSONException, IOException {
		int maxLength = 200;
		JSONObject layers = this.getConfigManager().getClientConfigFileJSon(null, null, this, ConfigType.LAYERS, false);

		List<JSONObject> layersFound = new ArrayList<JSONObject>();
		String[] terms = new String[0];
		if (Utils.isNotBlank(query)) {
			terms = query.trim().split("\\s+");
		}

		Iterator<String> layerIds = layers.keys();
		String title, textDescription;
		int rank;
		SortedSet<Utils.Occurrence> titleResults, descResults;
		while (layerIds.hasNext()) {
			String layerId = layerIds.next();
			if (!layers.isNull(layerId)) {
				LayerWrapper layer = new LayerWrapper(layers.optJSONObject(layerId));

				title = Utils.safeHTMLStr(layer.getTitle());
				textDescription = Utils.safeHTMLStr(layer.getTextDescription());

				titleResults = null; descResults = null;
				if (terms.length > 0) {
					titleResults = Utils.findOccurrences(title, terms);
					descResults = Utils.findOccurrences(textDescription, terms);
					rank = titleResults.size() * 5 + descResults.size();
				} else {
					// No search term, return everything
					rank = 1;
				}

				if (rank > 0) {
					JSONObject layerFound = new JSONObject();
					layerFound.put("layerId", layerId);
					layerFound.put("title", Utils.getHighlightChunk(titleResults, title, 0));
					layerFound.put("excerpt", Utils.getHighlightChunk(descResults, textDescription, maxLength));
					layerFound.put("rank", rank);
					layersFound.add(layerFound);
				}
			}
		}

		// Order the result
		Collections.sort(layersFound, new Comparator<JSONObject>() {
			@Override
			public int compare(JSONObject o1, JSONObject o2) {
				// Descending order - higher rank first
				int rankOrder = o2.optInt("rank", 0) - o1.optInt("rank", 0);
				if (rankOrder == 0) {
					// Ascending order - same rank: alphabetic order
					return Utils.getComparableTitle(o1.optString("title", "")).compareTo(Utils.getComparableTitle(o2.optString("title", "")));
				} else {
					return rankOrder;
				}
			}
		});

		JSONArray layersFoundJSON = new JSONArray();
		for (int i = offset; i < layersFound.size() && i < offset + qty; i++) {
			layersFoundJSON.put(layersFound.get(i));
		}

		JSONObject results = new JSONObject();
		results.put("count", layersFound.size());
		results.put("data", layersFoundJSON);

		return results;
	}

	/**
	 * Copy the client files from clientResources/amc to the client location.
	 * @param force Force file copy even if they are already there (used with complete regeneration)
	 * @throws IOException
	 */
	private void copyClientFilesIfNeeded(boolean force) throws IOException {
		File atlasMapperClientFolder =
				FileFinder.getAtlasMapperClientFolder(this.getConfigManager().getApplicationFolder(), this);
		if (atlasMapperClientFolder == null) { return; }

		// Return if the folder is not empty
		String[] folderContent = atlasMapperClientFolder.list();
		if (!force && folderContent != null && folderContent.length > 0) {
			return;
		}

		// The folder is Empty, copying the files
		try {
			File src = FileFinder.getAtlasMapperClientSourceFolder();
			Utils.recursiveFileCopy(src, atlasMapperClientFolder, force);
		} catch (URISyntaxException ex) {
			throw new IOException("Can not get a File reference to the AtlasMapperClient", ex);
		}
	}

	private void saveGeneratedConfigs(
			ClientWrapper mainConfig,
			ClientWrapper embeddedConfig,
			JSONObject layers) throws JSONException, IOException {

		File mainClientFile = this.getConfigManager().getClientMainConfigFile(this);
		if (mainClientFile == null) {
			throw new IllegalArgumentException("No file provided for the Main client configuration.");
		} else {
			this.getConfigManager().saveJSONConfig(mainConfig.getJSON(), mainClientFile);
		}

		File embeddedClientFile = this.getConfigManager().getClientEmbeddedConfigFile(this);
		if (embeddedClientFile == null) {
			throw new IllegalArgumentException("No file provided for the Embedded client configuration.");
		} else {
			this.getConfigManager().saveJSONConfig(embeddedConfig.getJSON(), embeddedClientFile);
		}

		File layersClientFile = this.getConfigManager().getClientLayersConfigFile(this);
		if (layersClientFile == null) {
			throw new IllegalArgumentException("No file provided for the layers configuration.");
		} else {
			this.getConfigManager().saveJSONConfig(layers, layersClientFile);
		}
	}

	// Create all files that required a template processing
	private void generateTemplateFiles(DataSourceWrapper layerCatalog, ClientWrapper generatedMainConfig, DataSourceWrapper googleDataSource) throws IOException, TemplateException {
		File atlasMapperClientFolder =
				FileFinder.getAtlasMapperClientFolder(this.getConfigManager().getApplicationFolder(), this);
		if (atlasMapperClientFolder == null) { return; }

		// Find template, process it and save it
		try {
			File templatesFolder = FileFinder.getAtlasMapperClientTemplatesFolder();
			Configuration templatesConfig = Utils.getTemplatesConfig(templatesFolder);

			// NOTE: To make a new template for the file "amc/x/y/z.ext",
			// create the file "amcTemplates/x/y/z.ext.flt" and add an entry
			// here for the template named "x/y/z.ext".
			// WARNING: Don't forget to use System.getProperty("file.separator")
			// instead of "/"!

			// Process all templates, one by one, because they are all unique
			Map<String, Object> indexValues = new HashMap<String, Object>();
			indexValues.put("version", ProjectInfo.getVersion());
			indexValues.put("mainConfig", this.getConfigManager().getClientMainConfigFile(this).getName());
			indexValues.put("layersConfig", this.getConfigManager().getClientLayersConfigFile(this).getName());
			indexValues.put("clientId", this.getClientId());
			indexValues.put("clientName", this.getClientName() != null ? this.getClientName() : this.getClientId());
			indexValues.put("theme", this.getTheme());
			indexValues.put("pageHeader", Utils.safeJsStr(this.getPageHeader()));
			indexValues.put("pageFooter", Utils.safeJsStr(this.getPageFooter()));
			indexValues.put("timestamp", ""+Utils.getCurrentTimestamp());
			indexValues.put("useGoogle", googleDataSource != null);
			indexValues.put("welcomeMsg", this.getWelcomeMsg());
			indexValues.put("headExtra", this.getHeadExtra());
			Utils.processTemplate(templatesConfig, "index.html", indexValues, atlasMapperClientFolder);

			Map<String, Object> embeddedValues = new HashMap<String, Object>();
			embeddedValues.put("version", ProjectInfo.getVersion());
			embeddedValues.put("mainConfig", this.getConfigManager().getClientMainConfigFile(this).getName());
			embeddedValues.put("layersConfig", this.getConfigManager().getClientLayersConfigFile(this).getName());
			embeddedValues.put("clientId", this.getClientId());
			embeddedValues.put("clientName", this.getClientName() != null ? this.getClientName() : this.getClientId());
			embeddedValues.put("theme", this.getTheme());
			embeddedValues.put("pageHeader", Utils.safeJsStr(this.getPageHeader()));
			embeddedValues.put("pageFooter", Utils.safeJsStr(this.getPageFooter()));
			embeddedValues.put("timestamp", ""+Utils.getCurrentTimestamp());
			embeddedValues.put("useGoogle", googleDataSource != null);
			// No welcome message
			Utils.processTemplate(templatesConfig, "embedded.html", embeddedValues, atlasMapperClientFolder);

			int width = 200;
			int height = 180;
			if (Utils.isNotBlank(this.getListLayerImageWidth())) {
				width = Integer.valueOf(this.getListLayerImageWidth());
			}
			if (Utils.isNotBlank(this.getListLayerImageHeight())) {
				height = Integer.valueOf(this.getListLayerImageHeight());
			}

			Map<String, Object> listValues = new HashMap<String, Object>();
			listValues.put("version", ProjectInfo.getVersion());
			listValues.put("clientName", this.getClientName() != null ? this.getClientName() : this.getClientId());
			listValues.put("layers", this.generateLayerList(layerCatalog, generatedMainConfig));
			listValues.put("layerBoxWidth", width + 2); // +2 for the 1 px border - This value can be overridden using CSS
			listValues.put("layerBoxHeight", height + 45); // +45 to let some room for the text bellow the layer - This value can be overridden using CSS
			listValues.put("listPageHeader", this.getListPageHeader());
			listValues.put("listPageFooter", this.getListPageFooter());
			Utils.processTemplate(templatesConfig, "list.html", listValues, atlasMapperClientFolder);
		} catch (URISyntaxException ex) {
			throw new IOException("Can not get a File reference to the AtlasMapperClient", ex);
		}
	}

	/**
	 * Return a Map of info used to generate a list of layers (for the list.html page):
	 * Map of
	 *     Key: DataSource name
	 *     Value: List of Map of
	 *         id: Layer ID, as used in the AtlasMapper (with the data source ID)
	 *         title: Displayed name of the layer
	 *         description: Displayed name of the layer
	 *         imageUrl: URL of the preview image for the layer
	 *         baseLayerUrl: URL of the background image to display under the layer image
	 *         imageWidth: Image width
	 *         imageHeight: Image height
	 *         mapUrl: URL of the AtlasMapper map that display that layer
	 * @param layerCatalog LayerCatalog, after overrides
	 * @param generatedMainConfig Client JSON config, to get the data sources (after overrides), the client projection and the default layers.
	 * @return
	 */
	private Map<String, List<Map<String, String>>> generateLayerList(DataSourceWrapper layerCatalog, ClientWrapper generatedMainConfig)
			throws UnsupportedEncodingException {
		if (layerCatalog == null) {
			return null;
		}
		JSONObject layers = layerCatalog.getLayers();
		if (layers == null || layers.length() <= 0) {
			return null;
		}

		Map<String, List<Map<String, String>>> layersMap = new LinkedHashMap<String, List<Map<String, String>>>();

		JSONObject dataSources = generatedMainConfig.getDataSources();
		String projection = "EPSG:4326";

		// Maximum width x height
		int defaultWidth = 200;
		int defaultHeight = 180;
		if (Utils.isNotBlank(this.getListLayerImageWidth())) {
			defaultWidth = Integer.valueOf(this.getListLayerImageWidth());
		}
		if (Utils.isNotBlank(this.getListLayerImageHeight())) {
			defaultHeight = Integer.valueOf(this.getListLayerImageHeight());
		}

		Iterator<String> layerIds = layers.keys();
		while (layerIds.hasNext()) {
			String layerId = layerIds.next();
			if (!layers.isNull(layerId)) {
				LayerWrapper layer = new LayerWrapper(layers.optJSONObject(layerId));
				// Ignore layer groups
				if (layer.getLayerName() != null) {
					String dataSourceId = layer.getDataSourceId();
					String layerName = layer.getLayerName();
					String layerTitle = layer.getTitle(layerName);

					String description = layer.getDescription();
					String descriptionFormat = layer.getDescriptionFormat();
					String systemDescription = layer.getSystemDescription();

					String serviceUrl = layer.getServiceUrl();
					JSONArray jsonBbox = layer.getLayerBoundingBox();

					// Data source object containing overridden values
					DataSourceWrapper dataSource = null;
					// Raw data source object containing values before override

					if (dataSources != null) {
						dataSource = new DataSourceWrapper(dataSources.optJSONObject(dataSourceId));
					}

					if (dataSource == null) {
						LOGGER.log(Level.WARNING, "The client [{0}] define a layer [{1}] using an invalid data source [{2}].",
								new String[]{ this.getClientName(), layerName, dataSourceId });
					} else {
						String dataSourceName = dataSource.getDataSourceName();

						// Find (or create) the layer list for this data source
						List<Map<String, String>> dataSourceLayerList = layersMap.get(dataSourceName);
						if (dataSourceLayerList == null) {
							dataSourceLayerList = new ArrayList<Map<String, String>>();
							layersMap.put(dataSourceName, dataSourceLayerList);
						}

						Map<String, String> layerMap = new HashMap<String, String>();

						if (serviceUrl == null || serviceUrl.isEmpty()) {
							serviceUrl = dataSource.getServiceUrl();
						}

						// http://e-atlas.localhost/maps/ea/wms
						// LAYERS=ea%3AQLD_DEEDI_Coastal-wetlands
						// FORMAT=image%2Fpng
						// SERVICE=WMS
						// VERSION=1.1.1
						// REQUEST=GetMap
						// EXCEPTIONS=application%2Fvnd.ogc.se_inimage
						// SRS=EPSG%3A4326
						// BBOX=130.20938085938,-37.1985,161.23261914062,-1.0165
						// WIDTH=439
						// HEIGHT=512
						if (serviceUrl != null && !serviceUrl.isEmpty()) {
							if (dataSource.isExtendWMS()) {
								if (jsonBbox != null && jsonBbox.length() == 4) {
									double[] bbox = new double[4];
									// Left, Bottom, Right, Top
									bbox[0] = jsonBbox.optDouble(0, -180);
									bbox[1] = jsonBbox.optDouble(1, -90);
									bbox[2] = jsonBbox.optDouble(2, 180);
									bbox[3] = jsonBbox.optDouble(3, 90);

									StringBuilder imageUrl = new StringBuilder(serviceUrl);
									if (!serviceUrl.endsWith("&") && !serviceUrl.endsWith("?")) {
										imageUrl.append(serviceUrl.contains("?") ? "&" : "?");
									}
									imageUrl.append("LAYERS="); imageUrl.append(URLEncoder.encode(layerName, "UTF-8"));
									imageUrl.append("&STYLES="); // Some servers need this parameter, even set to nothing
									imageUrl.append("&FORMAT="); imageUrl.append(URLEncoder.encode("image/png", "UTF-8"));
									imageUrl.append("&TRANSPARENT=true");
									imageUrl.append("&SERVICE=WMS");
									imageUrl.append("&VERSION=1.1.1"); // TODO Use version from config (and set the parameters properly; 1.3.0 needs CRS instead of SRS, inverted BBOX, etc.)
									imageUrl.append("&REQUEST=GetMap");
									imageUrl.append("&EXCEPTIONS="); imageUrl.append(URLEncoder.encode("application/vnd.ogc.se_inimage", "UTF-8"));
									imageUrl.append("&SRS="); imageUrl.append(URLEncoder.encode(projection, "UTF-8")); // TODO Use client projection

									imageUrl.append("&BBOX=");
									imageUrl.append(bbox[0]); imageUrl.append(",");
									imageUrl.append(bbox[1]); imageUrl.append(",");
									imageUrl.append(bbox[2]); imageUrl.append(",");
									imageUrl.append(bbox[3]);

									// Lon x Lat ratio (width / height  or  lon / lat)
									double ratio = (bbox[2] - bbox[0]) / (bbox[3] - bbox[1]);

									int width = defaultWidth;
									int height = defaultHeight;

									if (ratio > (((double)width)/height)) {
										// Reduce height
										height = (int)Math.round(width / ratio);
									} else {
										// Reduce width
										width = (int)Math.round(height * ratio);
									}

									imageUrl.append("&WIDTH=" + width);
									imageUrl.append("&HEIGHT=" + height);

									layerMap.put("imageUrl", imageUrl.toString());
									layerMap.put("imageWidth", ""+width);
									layerMap.put("imageHeight", ""+height);

									String baseLayerServiceUrl = this.getListBaseLayerServiceUrl();
									String baseLayerId = this.getListBaseLayerId();
									if (Utils.isNotBlank(baseLayerServiceUrl) && Utils.isNotBlank(baseLayerId)) {
										// Base layer - Hardcoded
										// http://maps.e-atlas.org.au/maps/gwc/service/wms
										// LAYERS=ea%3AWorld_NED_NE2
										// TRANSPARENT=FALSE
										// SERVICE=WMS
										// VERSION=1.1.1
										// REQUEST=GetMap
										// FORMAT=image%2Fjpeg
										// SRS=EPSG%3A4326
										// BBOX=149.0625,-22.5,151.875,-19.6875
										// WIDTH=256
										// HEIGHT=256
										StringBuilder baseLayerUrl = new StringBuilder(baseLayerServiceUrl);
										if (!baseLayerServiceUrl.endsWith("&") && !baseLayerServiceUrl.endsWith("?")) {
											baseLayerUrl.append(baseLayerServiceUrl.contains("?") ? "&" : "?");
										}
										baseLayerUrl.append("LAYERS="); baseLayerUrl.append(URLEncoder.encode(baseLayerId, "UTF-8"));
										baseLayerUrl.append("&STYLES="); // Some servers need this parameter, even set to nothing
										baseLayerUrl.append("&FORMAT="); baseLayerUrl.append(URLEncoder.encode("image/jpeg", "UTF-8"));
										baseLayerUrl.append("&TRANSPARENT=false");
										baseLayerUrl.append("&SERVICE=WMS");
										baseLayerUrl.append("&VERSION=1.1.1"); // TODO Use version from config (and set the parameters properly; 1.3.0 needs CRS instead of SRS, inverted BBOX, etc.)
										baseLayerUrl.append("&REQUEST=GetMap");
										baseLayerUrl.append("&EXCEPTIONS="); baseLayerUrl.append(URLEncoder.encode("application/vnd.ogc.se_inimage", "UTF-8"));
										baseLayerUrl.append("&SRS="); baseLayerUrl.append(URLEncoder.encode(projection, "UTF-8")); // TODO Use client projection

										baseLayerUrl.append("&BBOX=");
										baseLayerUrl.append(bbox[0]); baseLayerUrl.append(",");
										baseLayerUrl.append(bbox[1]); baseLayerUrl.append(",");
										baseLayerUrl.append(bbox[2]); baseLayerUrl.append(",");
										baseLayerUrl.append(bbox[3]);

										baseLayerUrl.append("&WIDTH=" + width);
										baseLayerUrl.append("&HEIGHT=" + height);

										layerMap.put("baseLayerUrl", baseLayerUrl.toString());
									}
								}
							}
						}

						layerMap.put("id", layerId);
						layerMap.put("title", layerTitle);
						layerMap.put("description", description);
						layerMap.put("descriptionFormat", descriptionFormat);
						layerMap.put("systemDescription", systemDescription);
						String encodedLayerId = URLEncoder.encode(layerId, "UTF-8");
						layerMap.put("mapUrl", "index.html?intro=f&dl=t&loc=" + encodedLayerId + "&l0=" + encodedLayerId);

						dataSourceLayerList.add(layerMap);
					}

				}
			}
		}

		return layersMap;
	}
}
