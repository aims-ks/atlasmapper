package au.gov.aims.atlasmapperserver;

import au.gov.aims.atlasmapperserver.jsonWrappers.client.ClientWrapper;
import au.gov.aims.atlasmapperserver.jsonWrappers.client.DataSourceWrapper;
import au.gov.aims.atlasmapperserver.jsonWrappers.client.LayerWrapper;
import au.gov.aims.atlasmapperserver.servlet.FileFinder;
import au.gov.aims.atlasmapperserver.servlet.Proxy;
import au.gov.aims.atlasmapperserver.thread.AbstractConfigThread;
import au.gov.aims.atlasmapperserver.thread.ThreadLogger;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class ClientConfigThread extends AbstractConfigThread {

    private ClientConfig clientConfig;

    private boolean complete;

    public ClientConfig getClientConfig() {
        return this.clientConfig;
    }

    public void setClientConfig(ClientConfig clientConfig) {
        this.clientConfig = clientConfig;
    }

    public boolean isComplete() {
        return this.complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    @Override
    public void run() {
        // Collect error messages
        ThreadLogger logger = this.getLogger();

        // Load data sources
        Map<String, DataSourceWrapper> dataSources = null;
        DataSourceWrapper googleDataSource = null;
        try {
            dataSources = this.clientConfig.loadDataSources();

            // Find a google data source, if any (to find out if we need to add google support)
            googleDataSource = this.clientConfig.getFirstGoogleDataSource(dataSources);
        } catch(Exception ex) {
            logger.log(Level.SEVERE, "Error occurred while loading the datasource: " + Utils.getExceptionMessage(ex), ex);
        }

        // Check for write access before doing any processing,
        File clientFolder = FileFinder.getClientFolder(this.clientConfig.getConfigManager().getApplicationFolder(), this.clientConfig);
        String tomcatUser = System.getProperty("user.name");
        if (clientFolder.exists()) {
            // The client do exists, check if we have write access to it.
            if (!clientFolder.canWrite()) {
                logger.log(Level.SEVERE, "The client could not be generated; The AtlasMapper do not have write access to the client folder [" + clientFolder.getAbsolutePath() + "]. " +
                        "Give write access to the user \"" + tomcatUser + "\" to the client folder and try regenerating the client.");
            }
        } else {
            // The client do not exists, check if it can be created.
            if (!Utils.recursiveIsWritable(clientFolder)) {
                logger.log(Level.SEVERE, "The client could not be generated; The AtlasMapper can not create the client folder [" + clientFolder.getAbsolutePath() + "]. " +
                        "Give write access to the user \"" + tomcatUser + "\" to the parent folder or " +
                        "create the client folder manually with write access to the user \"" + tomcatUser + "\" and try regenerating the client.");
            }
        }

        // Get the layer catalog from the data source save state and the client layer overrides.
        DataSourceWrapper layerCatalog = null;
        int nbLayers = 0;
        if (logger.getErrorCount() == 0) {
            try {
                layerCatalog = this.clientConfig.getLayerCatalog(dataSources);
                nbLayers = layerCatalog.getLayers() == null ? 0 : layerCatalog.getLayers().length();

                if (nbLayers <= 0) {
                    logger.log(Level.WARNING, "The client has no available layers");
                }
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "An IO exception occurred while generating the layer catalog: " + Utils.getExceptionMessage(ex), ex);
            } catch (JSONException ex) {
                logger.log(Level.SEVERE, "A JSON exception occurred while generating the layer catalog: " + Utils.getExceptionMessage(ex), ex);
            }

            try {
                this.copyClientFilesIfNeeded(this.complete);
            } catch (IOException ex) {
                // Those error are very unlikely to happen since we already checked the folder write access.
                if (clientFolder.exists()) {
                    logger.log(Level.SEVERE, "An unexpected exception occurred while copying the client files to the folder [" + clientFolder.getAbsolutePath() + "]: " + Utils.getExceptionMessage(ex), ex);
                } else {
                    logger.log(Level.SEVERE, "The client could not be generated; The AtlasMapper were not able to create the client folder [" + clientFolder.getAbsolutePath() + "]. " +
                            "Give write access to the user \"" + tomcatUser + "\" to the parent folder or " +
                            "create the client folder manually with write access to the user \"" + tomcatUser + "\" and try regenerating the client.", ex);
                }
            }
        }

        ClientWrapper generatedMainConfig = null;
        ClientWrapper generatedEmbeddedConfig = null;
        JSONObject generatedLayers = null;
        if (logger.getErrorCount() == 0) {
            try {
                generatedMainConfig = new ClientWrapper(this.clientConfig.getConfigManager().getClientConfigFileJSon(layerCatalog, dataSources, this.clientConfig, ConfigType.MAIN, true));
                generatedEmbeddedConfig = new ClientWrapper(this.clientConfig.getConfigManager().getClientConfigFileJSon(layerCatalog, dataSources, this.clientConfig, ConfigType.EMBEDDED, true));
                generatedLayers = this.clientConfig.getConfigManager().getClientConfigFileJSon(layerCatalog, dataSources, this.clientConfig, ConfigType.LAYERS, true);

                // Show warning for each default layers that are not defined in the layer catalog.
                List<String> defaultLayerIds = this.clientConfig.getDefaultLayersList();
                if (layerCatalog != null && defaultLayerIds != null && !defaultLayerIds.isEmpty()) {
                    JSONObject jsonLayers = layerCatalog.getLayers();
                    if (jsonLayers != null) {
                        for (String defaultLayerId : defaultLayerIds) {
                            if (!jsonLayers.has(defaultLayerId)) {
                                logger.log(Level.WARNING, "The layer ID [" + defaultLayerId + "], specified in the default layers, could not be found in the layer catalog.");
                            }
                        }
                    }
                }
            } catch (IOException ex) {
                // Very unlikely to happen
                logger.log(Level.SEVERE, "An IO exception occurred while generating the client config: " + Utils.getExceptionMessage(ex), ex);
            } catch (JSONException ex) {
                // Very unlikely to happen
                logger.log(Level.SEVERE, "A JSON exception occurred while generating the client config: " + Utils.getExceptionMessage(ex), ex);
            }
        }

        // Transfer layer's errors to the client
        /*
        clientErrors.addErrors(layerCatalog.getErrors());
        clientErrors.addWarnings(layerCatalog.getWarnings());
        clientErrors.addMessages(layerCatalog.getMessages());
        */

        // Verify if there is error (it may contains only warnings)
        if (logger.getErrorCount() == 0) {
            try {
                this.generateTemplateFiles(layerCatalog, generatedMainConfig, googleDataSource);
                this.saveGeneratedConfigs(generatedMainConfig, generatedEmbeddedConfig, generatedLayers);

                // Flush the proxy cache
                Proxy.reloadConfig(generatedMainConfig, generatedLayers, this.clientConfig);

                this.clientConfig.setLastGeneratedDate(new Date());
                // Write the changes to disk
                this.clientConfig.getConfigManager().saveServerConfig();
            } catch (TemplateException ex) {
                // May happen if a template is modified.
                logger.log(Level.SEVERE, "Can not process the client templates: " + Utils.getExceptionMessage(ex), ex);
            } catch (IOException ex) {
                // May happen if a template is modified.
                logger.log(Level.SEVERE, "An IO exception occurred while generating the client config: " + Utils.getExceptionMessage(ex), ex);
            } catch (JSONException ex) {
                // Very unlikely to happen
                logger.log(Level.SEVERE, "A JSON exception occurred while generating the client config: " + Utils.getExceptionMessage(ex), ex);
            }
        }

        // Generation - Conclusion message
        if (logger.getErrorCount() == 0) {
            if (logger.getWarningCount() == 0) {
                logger.log(Level.INFO, "Client generated successfully.");
            } else {
                logger.log(Level.INFO, "Client generation passed.");
            }
            logger.log(Level.INFO, "The client has " + nbLayers + " layer" + (nbLayers > 1 ? "s" : "") + " available.");
        } else {
            logger.log(Level.INFO, "Client generation failed.");
        }
    }

    /**
     * Copy the client files from clientResources/amc to the client location.
     * @param force Force file copy even if they are already there (used with complete regeneration)
     * @throws IOException
     */
    private void copyClientFilesIfNeeded(boolean force) throws IOException {
        File atlasMapperClientFolder =
                FileFinder.getAtlasMapperClientFolder(this.clientConfig.getConfigManager().getApplicationFolder(), this.clientConfig);
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

    // Create all files that required a template processing
    private void generateTemplateFiles(DataSourceWrapper layerCatalog, ClientWrapper generatedMainConfig, DataSourceWrapper googleDataSource) throws IOException, TemplateException {
        File atlasMapperClientFolder =
                FileFinder.getAtlasMapperClientFolder(this.clientConfig.getConfigManager().getApplicationFolder(), this.clientConfig);
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
            indexValues.put("mainConfig", this.clientConfig.getConfigManager().getClientMainConfigFile(this.clientConfig).getName());
            indexValues.put("layersConfig", this.clientConfig.getConfigManager().getClientLayersConfigFile(this.clientConfig).getName());
            indexValues.put("clientId", this.clientConfig.getClientId());
            indexValues.put("clientName", this.clientConfig.getClientName() != null ? this.clientConfig.getClientName() : this.clientConfig.getClientId());
            indexValues.put("theme", this.clientConfig.getTheme());
            indexValues.put("pageHeader", Utils.safeJsStr(this.clientConfig.getPageHeader()));
            indexValues.put("pageFooter", Utils.safeJsStr(this.clientConfig.getPageFooter()));
            indexValues.put("timestamp", ""+Utils.getCurrentTimestamp());
            indexValues.put("useGoogle", googleDataSource != null
                    && (googleDataSource.getGoogleJavaScript() == null || !googleDataSource.getGoogleJavaScript().contains("maps.google.com/maps/api")));
            if (googleDataSource != null) {
                indexValues.put("googleJavaScript", googleDataSource.getGoogleJavaScript());
            } else {
                indexValues.put("googleJavaScript", "");
            }
            indexValues.put("welcomeMsg", this.clientConfig.getWelcomeMsg());
            indexValues.put("headExtra", this.clientConfig.getHeadExtra());
            Utils.processTemplate(templatesConfig, "index.html", indexValues, atlasMapperClientFolder);

            Map<String, Object> embeddedValues = new HashMap<String, Object>();
            embeddedValues.put("version", ProjectInfo.getVersion());
            embeddedValues.put("mainConfig", this.clientConfig.getConfigManager().getClientMainConfigFile(this.clientConfig).getName());
            embeddedValues.put("layersConfig", this.clientConfig.getConfigManager().getClientLayersConfigFile(this.clientConfig).getName());
            embeddedValues.put("clientId", this.clientConfig.getClientId());
            embeddedValues.put("clientName", this.clientConfig.getClientName() != null ? this.clientConfig.getClientName() : this.clientConfig.getClientId());
            embeddedValues.put("theme", this.clientConfig.getTheme());
            embeddedValues.put("pageHeader", Utils.safeJsStr(this.clientConfig.getPageHeader()));
            embeddedValues.put("pageFooter", Utils.safeJsStr(this.clientConfig.getPageFooter()));
            embeddedValues.put("timestamp", ""+Utils.getCurrentTimestamp());
            embeddedValues.put("useGoogle", googleDataSource != null
                    && (googleDataSource.getGoogleJavaScript() == null || !googleDataSource.getGoogleJavaScript().contains("maps.google.com/maps/api")));
            if (googleDataSource != null) {
                embeddedValues.put("googleJavaScript", googleDataSource.getGoogleJavaScript());
            } else {
                embeddedValues.put("googleJavaScript", "");
            }
            // No welcome message
            Utils.processTemplate(templatesConfig, "embedded.html", embeddedValues, atlasMapperClientFolder);

            int width = 200;
            int height = 180;
            if (Utils.isNotBlank(this.clientConfig.getListLayerImageWidth())) {
                width = Integer.valueOf(this.clientConfig.getListLayerImageWidth());
            }
            if (Utils.isNotBlank(this.clientConfig.getListLayerImageHeight())) {
                height = Integer.valueOf(this.clientConfig.getListLayerImageHeight());
            }

            Map<String, Object> listValues = new HashMap<String, Object>();
            listValues.put("version", ProjectInfo.getVersion());
            listValues.put("clientName", this.clientConfig.getClientName() != null ? this.clientConfig.getClientName() : this.clientConfig.getClientId());
            listValues.put("layers", this.generateLayerList(layerCatalog, generatedMainConfig));
            listValues.put("layerBoxWidth", width + 2); // +2 for the 1 px border - This value can be overridden using CSS
            listValues.put("layerBoxHeight", height + 45); // +45 to let some room for the text bellow the layer - This value can be overridden using CSS
            listValues.put("listPageHeader", this.clientConfig.getListPageHeader());
            listValues.put("listPageFooter", this.clientConfig.getListPageFooter());
            Utils.processTemplate(templatesConfig, "list.html", listValues, atlasMapperClientFolder);
        } catch (URISyntaxException ex) {
            throw new IOException("Can not get a File reference to the AtlasMapperClient", ex);
        }
    }

    private void saveGeneratedConfigs(
            ClientWrapper mainConfig,
            ClientWrapper embeddedConfig,
            JSONObject layers) throws JSONException, IOException {

        File mainClientFile = this.clientConfig.getConfigManager().getClientMainConfigFile(this.clientConfig);
        if (mainClientFile == null) {
            throw new IllegalArgumentException("No file provided for the Main client configuration.");
        } else {
            this.clientConfig.getConfigManager().saveJSONConfig(mainConfig.getJSON(), mainClientFile);
        }

        File embeddedClientFile = this.clientConfig.getConfigManager().getClientEmbeddedConfigFile(this.clientConfig);
        if (embeddedClientFile == null) {
            throw new IllegalArgumentException("No file provided for the Embedded client configuration.");
        } else {
            this.clientConfig.getConfigManager().saveJSONConfig(embeddedConfig.getJSON(), embeddedClientFile);
        }

        File layersClientFile = this.clientConfig.getConfigManager().getClientLayersConfigFile(this.clientConfig);
        if (layersClientFile == null) {
            throw new IllegalArgumentException("No file provided for the layers configuration.");
        } else {
            this.clientConfig.getConfigManager().saveJSONConfig(layers, layersClientFile);
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
        ThreadLogger logger = this.getLogger();

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
        if (Utils.isNotBlank(this.clientConfig.getListLayerImageWidth())) {
            defaultWidth = Integer.valueOf(this.clientConfig.getListLayerImageWidth());
        }
        if (Utils.isNotBlank(this.clientConfig.getListLayerImageHeight())) {
            defaultHeight = Integer.valueOf(this.clientConfig.getListLayerImageHeight());
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
                        logger.log(Level.WARNING, String.format("The client [%s] define a layer [%s] using an invalid data source [%s].",
                                this.clientConfig.getClientName(), layerName, dataSourceId));
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

                        // https://eatlas.localhost/maps/ea/wms
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

                                    String baseLayerServiceUrl = this.clientConfig.getListBaseLayerServiceUrl();
                                    String baseLayerId = this.clientConfig.getListBaseLayerId();
                                    if (Utils.isNotBlank(baseLayerServiceUrl) && Utils.isNotBlank(baseLayerId)) {
                                        // Base layer - Hardcoded
                                        // https://maps.eatlas.org.au/maps/gwc/service/wms
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
