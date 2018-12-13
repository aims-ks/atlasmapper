/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2011 Australian Institute of Marine Science
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

package au.gov.aims.atlasmapperserver;

import au.gov.aims.atlasmapperserver.annotation.ConfigField;
import au.gov.aims.atlasmapperserver.dataSourceConfig.AbstractDataSourceConfig;
import au.gov.aims.atlasmapperserver.jsonWrappers.client.ClientWrapper;
import au.gov.aims.atlasmapperserver.jsonWrappers.client.DataSourceWrapper;
import au.gov.aims.atlasmapperserver.jsonWrappers.client.LayerWrapper;
import au.gov.aims.atlasmapperserver.layerConfig.AbstractLayerConfig;
import au.gov.aims.atlasmapperserver.layerConfig.LayerCatalog;
import au.gov.aims.atlasmapperserver.servlet.FileFinder;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.servlet.ServletContext;

import au.gov.aims.atlasmapperserver.thread.AbstractRunnableConfig;
import au.gov.aims.atlasmapperserver.thread.ClientConfigThread;
import au.gov.aims.atlasmapperserver.thread.ThreadLogger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONSortedObject;

/**
 *
 * @author glafond
 */
public class ClientConfig extends AbstractRunnableConfig<ClientConfigThread> {
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
    private String googleSearchAPIKey;

    @ConfigField
    private String osmSearchAPIKey;

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
        super(configManager, new ClientConfigThread());
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

    public void process(boolean complete) {
        if (this.isIdle()) {
            this.configThread.setClientConfig(this);
            this.configThread.setCompleteGeneration(complete);

            this.start();
        }
    }

    // LayerCatalog - After data source overrides
    public DataSourceWrapper getLayerCatalog(ThreadLogger logger, Map<String, DataSourceWrapper> dataSources) throws IOException, JSONException {
        DataSourceWrapper layerCatalog = new DataSourceWrapper();

        JSONObject clientOverrides = this.manualOverride;

        // Apply manual overrides, if needed, and add the layer to the catalog
        if (dataSources != null && !dataSources.isEmpty()) {
            for (Map.Entry<String, DataSourceWrapper> dataSourceEntry : dataSources.entrySet()) {
                String dataSourceId = dataSourceEntry.getKey();
                DataSourceWrapper dataSourceWrapper = dataSourceEntry.getValue();
                if (dataSourceWrapper == null) {
                    logger.log(Level.WARNING, String.format("Could not add the data source %s because it has never been generated.",
                            dataSourceId));
                } else {
                    JSONObject rawLayers = dataSourceWrapper.getLayers();
                    if (rawLayers != null && rawLayers.length() > 0) {
                        Iterator<String> rawLayerIds = rawLayers.keys();
                        while (rawLayerIds.hasNext()) {
                            String rawLayerId = rawLayerIds.next();

                            if (clientOverrides != null && clientOverrides.has(rawLayerId) && clientOverrides.optJSONObject(rawLayerId) == null) {
                                logger.log(Level.WARNING, String.format("Invalid manual override for layer: %s",
                                        rawLayerId));
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
                        logger.log(Level.WARNING, String.format("Invalid manual override for new layer: %s",
                                layerId));
                    } else {
                        try {
                            DataSourceWrapper dataSource = null;
                            String dataSourceId = jsonClientLayerOverride.getDataSourceId();
                            String layerType = jsonClientLayerOverride.getLayerType();

                            if (Utils.isNotBlank(dataSourceId)) {
                                dataSource = dataSources.get(dataSourceId);

                                if (dataSource == null) {
                                    logger.log(Level.WARNING, String.format("The manual override for the new layer %s is defining an invalid data source %s.",
                                            layerId, dataSourceId));
                                    continue;
                                }
                            }

                            if (Utils.isBlank(layerType)) {
                                if (dataSource != null) {
                                    layerType = dataSource.getLayerType();
                                } else {
                                    logger.log(Level.WARNING, String.format("The manual override for the new layer %s can not be created because it do not define its data source type.",
                                            layerId));
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
                            logger.log(Level.SEVERE, String.format("Unexpected error occurred while parsing the layer override for the layer %s: %s",
                                    layerId, Utils.getExceptionMessage(ex)), ex);
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
                            logger.log(Level.WARNING, String.format("Deprecated layer ID used for base layers. Layer id %s should be %s",
                                    layerName, layerId));
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


    public String getGoogleSearchAPIKey() {
        return this.googleSearchAPIKey;
    }

    public void setGoogleSearchAPIKey(String googleSearchAPIKey) {
        this.googleSearchAPIKey = googleSearchAPIKey;
    }


    public String getOsmSearchAPIKey() {
        return this.osmSearchAPIKey;
    }

    public void setOsmSearchAPIKey(String osmSearchAPIKey) {
        this.osmSearchAPIKey = osmSearchAPIKey;
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


    public JSONObject locationSearch(String query, String referer, String mapBounds, int offset, int qty) throws JSONException, IOException {
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

        try {
            String googleSearchAPIKey = this.getGoogleSearchAPIKey();
            if (this.isShowGoogleResults() && Utils.isNotBlank(googleSearchAPIKey)) {
                List<JSONObject> googleResults = LocationSearch.googleSearch(googleSearchAPIKey, referer, encodedQuery, mapBounds);
                if (googleResults != null && !googleResults.isEmpty()) {
                    resultsSet.addAll(googleResults);
                }
            }
        } catch(Exception ex) {
            LOGGER.log(Level.SEVERE, Utils.getExceptionMessage(ex), ex);
        }

        try {
            String osmSearchAPIKey = this.getOsmSearchAPIKey();
            if (this.isShowOSMResults() && Utils.isNotBlank(osmSearchAPIKey)) {
                List<JSONObject> osmNominatimResults = LocationSearch.osmNominatimSearch(osmSearchAPIKey, referer, encodedQuery, mapBounds);
                if (osmNominatimResults != null && !osmNominatimResults.isEmpty()) {
                    resultsSet.addAll(osmNominatimResults);
                }
            }
        } catch(Exception ex) {
            LOGGER.log(Level.SEVERE, Utils.getExceptionMessage(ex), ex);
        }

        try {
            String arcGISSearchUrl = this.getArcGISSearchUrl();
            if (this.isShowArcGISResults() && Utils.isNotBlank(arcGISSearchUrl)) {
                List<JSONObject> arcGISResults = LocationSearch.arcGISSearch(referer, arcGISSearchUrl, encodedQuery, mapBounds);
                if (arcGISResults != null && !arcGISResults.isEmpty()) {
                    resultsSet.addAll(arcGISResults);
                }
            }
        } catch(Exception ex) {
            LOGGER.log(Level.SEVERE, Utils.getExceptionMessage(ex), ex);
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
}
