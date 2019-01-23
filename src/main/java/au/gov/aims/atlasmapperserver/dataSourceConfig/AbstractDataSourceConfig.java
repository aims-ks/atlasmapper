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

package au.gov.aims.atlasmapperserver.dataSourceConfig;

import au.gov.aims.atlasmapperserver.thread.AbstractRunnableConfig;
import au.gov.aims.atlasmapperserver.ConfigManager;
import au.gov.aims.atlasmapperserver.Utils;
import au.gov.aims.atlasmapperserver.annotation.ConfigField;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import au.gov.aims.atlasmapperserver.jsonWrappers.client.DataSourceWrapper;
import au.gov.aims.atlasmapperserver.layerGenerator.AbstractLayerGenerator;
import au.gov.aims.atlasmapperserver.servlet.FileFinder;
import au.gov.aims.atlasmapperserver.thread.AbstractDataSourceConfigThread;
import au.gov.aims.atlasmapperserver.thread.ThreadLogger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONSortedObject;
import org.json.JSONTokener;

/**
 *
 * @author glafond
 */
public abstract class AbstractDataSourceConfig extends AbstractRunnableConfig<AbstractDataSourceConfigThread> implements Comparable<AbstractDataSourceConfig>, Cloneable {
    private static final Logger LOGGER = Logger.getLogger(AbstractDataSourceConfig.class.getName());

    // Grids records must have an unmutable ID
    @ConfigField
    private Integer id;

    @ConfigField
    private String dataSourceId;

    @ConfigField
    private String dataSourceName;

    @ConfigField
    private int layerCount;

    // Label used in the layer tree. Can be used to put multiple datasources into the same tree.
    // Default: dataSourceName
    @ConfigField
    private String treeRoot;

    @ConfigField(alias="dataSourceType")
    private String layerType;

    // Used to be called "wmsServiceUrl", renamed to "serviceUrl" since it apply many type of layers, not just WMS.
    @ConfigField(alias="wmsServiceUrl")
    private String serviceUrl;

    @ConfigField
    private String featureRequestsUrl;

    @ConfigField
    private String legendUrl;

    // This parameter is save as text in the server config, parsed and saved as JSONObject in the client config.
    @ConfigField
    private String legendParameters;

    @ConfigField
    private String stylesUrl;

    @ConfigField
    private String blackAndWhiteListedLayers;

    @ConfigField
    private String[] baseLayers;

    @ConfigField
    private String[] overlayLayers;

    @ConfigField
    private JSONSortedObject globalManualOverride;

    @ConfigField
    private Boolean activeDownload;

    @ConfigField
    private Boolean showInLegend;

    @ConfigField
    private String comment;

    protected AbstractDataSourceConfig(ConfigManager configManager) {
        super(configManager, new AbstractDataSourceConfigThread());
    }

    public void save(ThreadLogger logger, DataSourceWrapper layerCatalog) throws JSONException, IOException {
        File applicationFolder = this.getConfigManager().getApplicationFolder();

        DataSourceWrapper dataSourceWrapper = new DataSourceWrapper(this.toJSonObject(true));

        int nbLayers = 0;
        if (layerCatalog != null) {
            JSONObject layers = layerCatalog.getLayers();
            if (layers != null) {
                nbLayers = layers.length();
            }
            dataSourceWrapper.setLayers(layers);
        } else {
            dataSourceWrapper.setLayers((JSONObject) null);
        }

        int nbErrors = logger.getErrorCount();

        String status = "OKAY";
        if (nbLayers <= 0) {
            // It do not contains any layers, there is nothing to do with it.
            status = "INVALID";
        } else if (nbErrors > 0) {
            // It contains error, but it also contains some layers.
            // The datasource might be usable (PASSED), but better not take chances...
            status = "INVALID";
        }

        dataSourceWrapper.setLayerCount(nbLayers);
        dataSourceWrapper.setStatus(status);

        AbstractDataSourceConfig.write(applicationFolder, this.dataSourceId, dataSourceWrapper);
    }

    private static void write(File applicationFolder, String dataSourceId, DataSourceWrapper dataSourceWrapper) throws JSONException, IOException {
        File dataSourceCatalogFile = FileFinder.getDataSourcesCatalogFile(applicationFolder, dataSourceId);

        Writer writer = null;
        BufferedWriter bw = null;
        try {
            writer = new FileWriter(dataSourceCatalogFile);
            bw = new BufferedWriter(writer);
            String jsonStr = Utils.jsonToStr(dataSourceWrapper.getJSON());
            if (Utils.isNotBlank(jsonStr)) {
                bw.write(jsonStr);
            }
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Can not close the data source catalog buffered writer: {0}", Utils.getExceptionMessage(e));
                    LOGGER.log(Level.FINE, "Stack trace:", e);
                }
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Can not close the data source catalog writer: {0}", Utils.getExceptionMessage(e));
                    LOGGER.log(Level.FINE, "Stack trace:", e);
                }
            }
        }
    }

    public static DataSourceWrapper load(File applicationFolder, String dataSourceId) throws FileNotFoundException, JSONException {
        return AbstractDataSourceConfig.load(FileFinder.getDataSourcesCatalogFile(applicationFolder, dataSourceId));
    }

    public static DataSourceWrapper load(File dataSourceSavedStateFile) throws FileNotFoundException, JSONException {
        if (dataSourceSavedStateFile == null || !dataSourceSavedStateFile.exists()) {
            return null;
        }

        DataSourceWrapper dataSourceWrapper = null;
        Reader reader = null;
        try {
            reader = new FileReader(dataSourceSavedStateFile);
            dataSourceWrapper = new DataSourceWrapper(new JSONObject(new JSONTokener(reader)));
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Can not close the data source catalog reader: {0}", Utils.getExceptionMessage(ex));
                    LOGGER.log(Level.FINE, "Stack trace:", ex);
                }
            }
        }

        return dataSourceWrapper;
    }

    public void setModified(boolean modified) throws IOException, JSONException {
        File applicationFolder = this.getConfigManager().getApplicationFolder();
        // Load the old saved state
        DataSourceWrapper dataSourceWrapper = AbstractDataSourceConfig.load(applicationFolder, this.dataSourceId);
        // Change its status to MODIFIED
        if (dataSourceWrapper != null && modified != dataSourceWrapper.isModified()) {
            dataSourceWrapper.setModified(modified);
            // Save the old saved state with the status MODIFIED
            AbstractDataSourceConfig.write(applicationFolder, this.dataSourceId, dataSourceWrapper);
        }
    }

    public File getCacheStateFile() {
        File applicationFolder = this.getConfigManager().getApplicationFolder();
        return FileFinder.getDataSourcesCatalogFile(applicationFolder, this.dataSourceId);
    }

    public void deleteCachedStateFile() {
        File dataSourceCatalogFile = this.getCacheStateFile();
        if (dataSourceCatalogFile.exists()) {
            dataSourceCatalogFile.delete();
        }
    }

    public void process(boolean clearCapabilitiesCache, boolean clearMetadataCache) {
        if (this.isIdle()) {
            this.configThread.setDataSourceConfig(this);
            this.configThread.setClearCapabilitiesCache(clearCapabilitiesCache);
            this.configThread.setClearMetadataCache(clearMetadataCache);

            this.start();
        }
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

    public String getBlackAndWhiteListedLayers() {
        return this.blackAndWhiteListedLayers;
    }

    public void setBlackAndWhiteListedLayers(String blackAndWhiteListedLayers) {
        this.blackAndWhiteListedLayers = blackAndWhiteListedLayers;
    }


    public String[] getBaseLayers() {
        return this.baseLayers;
    }
    public void setBaseLayers(String[] rawBaseLayers) {
        if (rawBaseLayers == null || rawBaseLayers.length <= 0) {
            this.baseLayers = null;
        } else {
            List<String> baseLayers = new ArrayList<String>(rawBaseLayers.length);
            for (String baseLayer : rawBaseLayers) {
                // When the value come from the form (or an old config file), it's a coma separated String instead of an Array
                Pattern regex = Pattern.compile(".*" + SPLIT_PATTERN + ".*", Pattern.DOTALL);
                if (regex.matcher(baseLayer).matches()) {
                    for (String splitBaseLayer : baseLayer.split(SPLIT_PATTERN)) {
                        baseLayers.add(splitBaseLayer.trim());
                    }
                } else {
                    baseLayers.add(baseLayer.trim());
                }
            }
            this.baseLayers = baseLayers.toArray(new String[baseLayers.size()]);
        }
    }

    public String[] getOverlayLayers() {
        return this.overlayLayers;
    }
    public void setOverlayLayers(String[] rawOverlayLayers) {
        if (rawOverlayLayers == null || rawOverlayLayers.length <= 0) {
            this.overlayLayers = null;
        } else {
            List<String> overlayLayers = new ArrayList<String>(rawOverlayLayers.length);
            for (String overlayLayer : rawOverlayLayers) {
                // When the value come from the form (or an old config file), it's a coma separated String instead of an Array
                Pattern regex = Pattern.compile(".*" + SPLIT_PATTERN + ".*", Pattern.DOTALL);
                if (regex.matcher(overlayLayer).matches()) {
                    for (String splitOverlayLayer : overlayLayer.split(SPLIT_PATTERN)) {
                        overlayLayers.add(splitOverlayLayer.trim());
                    }
                } else {
                    overlayLayers.add(overlayLayer.trim());
                }
            }
            this.overlayLayers = overlayLayers.toArray(new String[overlayLayers.size()]);
        }
    }


    public JSONSortedObject getGlobalManualOverride() {
        return this.globalManualOverride;
    }

    public void setGlobalManualOverride(JSONSortedObject globalManualOverride) {
        this.globalManualOverride = globalManualOverride;
    }

    public String getLayerType() {
        return this.layerType;
    }

    public void setLayerType(String layerType) {
        this.layerType = layerType;
    }

    public String getFeatureRequestsUrl() {
        return this.featureRequestsUrl;
    }
    public void setFeatureRequestsUrl(URL featureRequestsUrl) {
        this.setFeatureRequestsUrl(featureRequestsUrl == null ? null : featureRequestsUrl.toString());
    }
    public void setFeatureRequestsUrl(String featureRequestsUrl) {
        this.featureRequestsUrl = featureRequestsUrl;
    }

    public String getServiceUrl() {
        return this.serviceUrl;
    }
    public void setServiceUrl(URL serviceUrl) {
        this.setServiceUrl(serviceUrl == null ? null : serviceUrl.toString());
    }
    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    public String getLegendUrl() {
        return this.legendUrl;
    }
    public void setLegendUrl(URL legendUrl) {
        this.setLegendUrl(legendUrl == null ? null : legendUrl.toString());
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

    public String getStylesUrl() {
        return stylesUrl;
    }

    public void setStylesUrl(String stylesUrl) {
        this.stylesUrl = stylesUrl;
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
        return this.dataSourceName;
    }

    public void setDataSourceName(String dataSourceName) {
        this.dataSourceName = dataSourceName;
    }

    public int getLayerCount() {
        return this.layerCount;
    }

    public void setLayerCount(int layerCount) {
        this.layerCount = layerCount;
    }

    public String getTreeRoot() {
        return this.treeRoot;
    }

    public void setTreeRoot(String treeRoot) {
        this.treeRoot = treeRoot;
    }

    public Boolean isActiveDownload() {
        return this.activeDownload;
    }

    public void setActiveDownload(Boolean activeDownload) {
        this.activeDownload = activeDownload;
    }

    public Boolean isShowInLegend() {
        return this.showInLegend;
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


    // Most data source, like WMS, will have all layers as overlay and some exceptions as base layers.
    // Some data source, like XYZ, will have all layers as base layers and some exceptions as overlay (set this to true).
    public boolean isDefaultAllBaseLayers() {
        return false;
    }

    // Helper
    public boolean isBaseLayer(String layerId) {
        if (Utils.isBlank(layerId)) {
            return false;
        }

        if (this.isDefaultAllBaseLayers()) {
            if (this.overlayLayers == null || this.overlayLayers.length <= 0) {
                return true;
            }

            return !arrayContains(this.overlayLayers, layerId);
        } else {
            if (this.baseLayers == null || this.baseLayers.length <= 0) {
                return false;
            }

            return arrayContains(this.baseLayers, layerId);
        }
    }

    public abstract AbstractLayerGenerator createLayerGenerator() throws Exception;

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

    // Generate the config to be display in the admin page
    @Override
    public JSONObject toJSonObject() throws JSONException {
        return this.toJSonObject(false);
    }

    // Generate the config to be display in the admin page, or saved as a data source saved state
    public JSONObject toJSonObject(boolean forSavedState) throws JSONException {
        DataSourceWrapper dataSourceWrapper = new DataSourceWrapper(super.toJSonObject());

        if (forSavedState) {
            // Remove attributes that are not needed for the saved state

            // Overrides are not needed; they have already been processed at this point
            dataSourceWrapper.setGlobalManualOverride(null);

            // Black and white list is not needed, the layers has already been filtered at this point
            dataSourceWrapper.setBlackAndWhiteListedLayers(null);

            // Comments are only useful for the admin interface.
            dataSourceWrapper.setComment(null);

            // Base layers flag are set on layers at this stage. The client do not need those list.
            dataSourceWrapper.setBaseLayers((JSONArray)null);
            dataSourceWrapper.setOverlayLayers((JSONArray)null);

            // Save the legend parameters as a JSONObject (the wrapper do the conversion from String to JSON)
            dataSourceWrapper.setLegendParameters(this.getLegendParameters());
        } else {
            if (this.globalManualOverride != null) {
                dataSourceWrapper.setGlobalManualOverride(this.globalManualOverride.toString(4));
            }

            // Add lastHarvested date and the valid flag to the JSON object.
            String status = "INVALID";
            boolean modified = false;
            File applicationFolder = this.getConfigManager().getApplicationFolder();
            File dataSourceCatalogFile = FileFinder.getDataSourcesCatalogFile(applicationFolder, this.dataSourceId);
            if (dataSourceCatalogFile.exists()) {
                try {
                    DataSourceWrapper dataSourceSavedState = AbstractDataSourceConfig.load(dataSourceCatalogFile);

                    if (dataSourceSavedState != null) {
                        status = dataSourceSavedState.getStatus();
                        modified = dataSourceSavedState.isModified();

                        dataSourceWrapper.setLayerCount(dataSourceSavedState.getLayerCount());

                        // lastModified() returns 0L if the file do not exists of an exception occurred.
                        long timestamp = dataSourceCatalogFile.lastModified();
                        if (timestamp > 0) {
                            dataSourceWrapper.setLastHarvested(ConfigManager.DATE_FORMATER.format(timestamp));
                        }
                    }
                } catch (FileNotFoundException ex) {
                    // This should not happen, there is already a check to see if the file exists.
                    LOGGER.log(Level.FINE, "Can not load the data source [" + this.dataSourceId + "] saved state");
                }
            }
            dataSourceWrapper.setStatus(status);
            if (modified) {
                dataSourceWrapper.setModified(modified);
            }
        }

        return dataSourceWrapper.getJSON();
    }

    @Override
    public String toString() {
        return "AbstractDataSourceConfig {\n" +
                (this.id==null ? "" :                                  "	id=" + this.id + "\n") +
                (Utils.isBlank(this.dataSourceId) ? "" :               "	dataSourceId=" + this.dataSourceId + "\n") +
                (Utils.isBlank(this.dataSourceName) ? "" :             "	dataSourceName=" + this.dataSourceName + "\n") +
                (Utils.isBlank(this.treeRoot) ? "" :                   "	treeRoot=" + this.treeRoot + "\n") +
                (Utils.isBlank(this.layerType) ? "" :                  "	layerType=" + this.layerType + "\n") +
                (Utils.isBlank(this.serviceUrl) ? "" :                 "	serviceUrl=" + this.serviceUrl + "\n") +
                (Utils.isBlank(this.featureRequestsUrl) ? "" :         "	featureRequestsUrl=" + this.featureRequestsUrl + "\n") +
                (Utils.isBlank(this.legendUrl) ? "" :                  "	legendUrl=" + this.legendUrl + "\n") +
                (this.legendParameters==null ? "" :                    "	legendParameters=" + this.legendParameters + "\n") +
                (Utils.isBlank(this.blackAndWhiteListedLayers) ? "" :  "	blackAndWhiteListedLayers=" + this.blackAndWhiteListedLayers + "\n") +
                (this.showInLegend==null ? "" :                        "	showInLegend=" + this.showInLegend + "\n") +
                (Utils.isBlank(this.comment) ? "" :                    "	comment=" + this.comment + "\n") +
            '}';
    }
}
