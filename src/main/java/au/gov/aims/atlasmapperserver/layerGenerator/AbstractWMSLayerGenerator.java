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

package au.gov.aims.atlasmapperserver.layerGenerator;

import au.gov.aims.atlasmapperserver.ConfigManager;
import au.gov.aims.atlasmapperserver.URLCache;
import au.gov.aims.atlasmapperserver.Utils;
import au.gov.aims.atlasmapperserver.dataSourceConfig.WMSDataSourceConfig;
import au.gov.aims.atlasmapperserver.layerConfig.LayerCatalog;
import au.gov.aims.atlasmapperserver.layerConfig.LayerStyleConfig;
import au.gov.aims.atlasmapperserver.layerConfig.WMSLayerConfig;
import au.gov.aims.atlasmapperserver.thread.ThreadLogger;
import au.gov.aims.atlasmapperserver.xml.TC211.TC211Document;
import au.gov.aims.atlasmapperserver.xml.TC211.TC211Parser;
import au.gov.aims.atlasmapperserver.xml.WMTS.WMTSDocument;
import au.gov.aims.atlasmapperserver.xml.WMTS.WMTSParser;
import org.geotools.data.ows.CRSEnvelope;
import org.geotools.data.ows.Layer;
import org.geotools.data.ows.OperationType;
import org.geotools.data.ows.StyleImpl;
import org.geotools.data.ows.WMSCapabilities;
import org.geotools.data.ows.WMSRequest;
import org.geotools.data.wms.xml.MetadataURL;
import org.json.JSONException;
import org.json.JSONSortedObject;
import org.opengis.util.InternationalString;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractWMSLayerGenerator<L extends WMSLayerConfig, D extends WMSDataSourceConfig> extends AbstractLayerGenerator<L, D> {
    private static final Logger LOGGER = Logger.getLogger(AbstractWMSLayerGenerator.class.getName());

    protected abstract L createLayerConfig(ConfigManager configManager);

    // GeoTools library and ncWMS server has issues with 1.3.0
    // * GeoTools: It tries to download the schema of extended specifications. Often, those specifications
    //     are not standard and their schema are no longer available, producing long waiting delay, timeout and
    //     IOExceptions. The most common example is the addition of "<ms:GetStyle>", which was part of 1.1.1,
    //     removed in 1.3.0 and is re-added as a un-standard extension by some services.
    // * ncWMS: All versions of ncWMS are able to provide a 1.3.0 capabilities document but they do not
    //     support 1.3.0, returning server error for all requests.
    // NOTE: If a specific URL to the capabilities document version 1.3.0 is provided, WMS 1.3.0 will be used.
    //
    // WMS Versions history:
    // * 0.0.1 through 0.0.6 - WMT development versions, March-September 1999
    // * 0.1 - WMT demonstration - 1999-09-10
    // * 0.9 - RFC submission - 1999-11-15
    // * 0.9.3 - RFC resubmission - 2000-01-17
    // * 1.0.0 - First WMS Implementation Specification (OGC document #00-028) - 2000-04-19
    //     http://portal.opengeospatial.org/files/?artifact_id=7196
    // * 1.0.4 - Web Mapping Testbed 2 development version - 2000-10-13
    // * 1.0.6 - OGC Discussion paper #01-021 - 2001-01-29
    // * 1.0.7 - OGC Discussion Paper #01-021r1 - 2001-03-02
    // * 1.0.8 - WMS Revision Working Group submittal to OGC TC (document #01-047) - 2001-05-14
    // * 1.1.0 - Revised edition (OGC document #01-047r2) - 2001-06-21
    //     http://portal.opengeospatial.org/files/?artifact_id=1058
    // * 1.1.1 - Minor revision (OGC document #01-068r3) - 2002-01-16
    //     http://portal.opengeospatial.org/files/?artifact_id=1081&version=1&format=pdf
    // * 1.3.0 - (OGC document #06-042) - 2006-03-15
    //     http://portal.opengeospatial.org/files/?artifact_id=14416
    protected String wmsVersion = "1.1.1";

    /**
     * WMS Server already has a unique layer ID for each layers. Nothing to do here.
     * @param layer
     * @param dataSourceConfig
     * @return
     */
    @Override
    protected String getUniqueLayerId(WMSLayerConfig layer, WMSDataSourceConfig dataSourceConfig) {
        return layer.getLayerId();
    }

    @Override
    public LayerCatalog generateRawLayerCatalog(ThreadLogger logger, D dataSourceClone, boolean redownloadPrimaryFiles, boolean redownloadSecondaryFiles) {
        LayerCatalog layerCatalog = new LayerCatalog();
        Map<String, L> layersMap = null;
        URL wmsServiceUrl = null;
        String dataSourceServiceUrlStr = dataSourceClone.getServiceUrl();
        ConfigManager configManager = dataSourceClone.getConfigManager();

        WMSCapabilities wmsCapabilities = null;
        try {
            logger.log(Level.INFO, "Getting WMS GetCapabilities document");
            wmsCapabilities = URLCache.getWMSCapabilitiesResponse(logger, configManager, this.wmsVersion, dataSourceClone, dataSourceServiceUrlStr, URLCache.Category.CAPABILITIES_DOCUMENT, true);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Error occurred while parsing the capabilities document for the service URL [" + dataSourceServiceUrlStr + "]: " + Utils.getExceptionMessage(ex), ex);
        }

        if (wmsCapabilities != null) {
            WMSRequest wmsRequestCapabilities = wmsCapabilities.getRequest();
            if (wmsRequestCapabilities != null) {
                if (Utils.isNotBlank(dataSourceClone.getGetMapUrl())) {
                    try {
                        wmsServiceUrl = Utils.toURL(dataSourceClone.getGetMapUrl());
                    } catch (Exception ex) {
                        logger.log(Level.WARNING, "Can not create a URL object from the string [" + dataSourceClone.getGetMapUrl() + "]: " + Utils.getExceptionMessage(ex), ex);
                    }
                } else {
                    wmsServiceUrl = this.getOperationUrl(wmsRequestCapabilities.getGetMap());
                }
                dataSourceClone.setServiceUrl(wmsServiceUrl);

                if (Utils.isBlank(dataSourceClone.getFeatureRequestsUrl())) {
                    dataSourceClone.setFeatureRequestsUrl(this.getOperationUrl(wmsRequestCapabilities.getGetFeatureInfo()));
                }

                dataSourceClone.setLegendUrl(this.getOperationUrl(wmsRequestCapabilities.getGetLegendGraphic()));
                dataSourceClone.setWmsVersion(wmsCapabilities.getVersion());

                // GetStyles URL is in GeoTools API but not in the Capabilities document.
                //     GeoTools probably craft the URL. It's not very useful.
                //this.stylesUrl = this.getOperationUrl(wmsRequestCapabilities.getGetStyles());
            }

            layersMap = this.getLayersInfoFromCaps(logger, wmsCapabilities, dataSourceClone);

            // Set default style of each layer
            if (layersMap != null && !layersMap.isEmpty()) {
                for (L layer : layersMap.values()) {
                    this.setDefaultLayerStyle(configManager, layer);
                }
            }
        }
        if (wmsServiceUrl == null && dataSourceServiceUrlStr != null) {
            try {
                wmsServiceUrl = Utils.toURL(dataSourceServiceUrlStr);
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Can not create a URL object from the string [" + dataSourceServiceUrlStr + "]: " + Utils.getExceptionMessage(ex), ex);
            }
        }

        Collection<L> layers = null;
        if (layersMap != null && !layersMap.isEmpty()) {
            if (dataSourceClone.isWebCacheEnable() != null && dataSourceClone.isWebCacheEnable() && wmsServiceUrl != null) {
                layers = new ArrayList<L>(layersMap.size());
                Map<String, L> cachedLayers = null;
                try {
                    cachedLayers = this.generateRawCachedLayerConfigs(logger, dataSourceClone, wmsServiceUrl, layerCatalog);
                } catch (Exception ex) {
                    logger.log(Level.WARNING, "Error occurred while parsing the WMTS capabilities document for the service URL [" + wmsServiceUrl + "]: " + Utils.getExceptionMessage(ex), ex);
                }

                // Since we are not parsing the Cache server WMS capability document, we can not find which version of WMS it is using...
                // Fallback to 1.1.1, it's very well supported.
                dataSourceClone.setCacheWmsVersion("1.1.1");

                // Set cached flags
                boolean fallback = false;
                for (Map.Entry<String, L> layerEntry : layersMap.entrySet()) {
                    boolean cached = false;
                    L layer = layerEntry.getValue();
                    if (cachedLayers == null) {
                        // Empty list means no cached layers
                        // NULL means WMTS service not available. GeoServer 2.1.X use to have that problem...
                        // Fallback (GeoServer 2.1.X)  - assume GeoWebCache support cache for all layers, default style only
                        fallback = true;
                        cached = true;
                        this.setLayerStylesCacheFlag(layer.getStyles(), null);
                    } else if (cachedLayers.containsKey(layerEntry.getKey())) {
                        L cachedLayer = cachedLayers.get(layerEntry.getKey());
                        if (cachedLayer != null) {
                            cached = true;
                            this.setLayerStylesCacheFlag(layer.getStyles(), cachedLayer.getStyles());
                        }
                    }
                    layer.setCached(cached);

                    layers.add(layer);
                }

                if (fallback) {
                    logger.log(Level.WARNING, "Could not find a valid WMTS capability document. " +
                            "Assuming all layers are cached. If the caching feature do not work properly, " +
                            "disable it in the data source configuration.");
                }
            } else {
                // The cache is disabled, just get the layer list direct from the map.
                layers = layersMap.values();
            }
        }
        layerCatalog.addLayers(layers);

        return layerCatalog;
    }

    private void setLayerStylesCacheFlag(List<LayerStyleConfig> layerStyles, List<LayerStyleConfig> cachedStyles) {
        if (layerStyles != null && !layerStyles.isEmpty()) {
            boolean cachedStyleNotEmpty = cachedStyles != null && !cachedStyles.isEmpty();

            for (LayerStyleConfig style : layerStyles) {
                boolean cached = false;
                boolean defaultStyle = style.isDefault() == null ? false : style.isDefault();

                if (defaultStyle) {
                    cached = true;
                } else if (cachedStyleNotEmpty) {
                    String styleName = style.getName();
                    if (styleName != null) {
                        for (LayerStyleConfig cachedStyle : cachedStyles) {
                            String cachedStyleName = cachedStyle.getName();
                            if (styleName.equals(cachedStyleName)) {
                                cached = true;
                            }
                        }
                    }
                }
                style.setCached(cached);
            }
        }
    }

    private Map<String, L> generateRawCachedLayerConfigs(ThreadLogger logger, D dataSourceClone, URL wmsServiceUrl, LayerCatalog layerCatalog) throws Exception {
        // When the webCacheEnable checkbox is unchecked, no layers are cached.
        if (dataSourceClone.isWebCacheEnable() == null || dataSourceClone.isWebCacheEnable() == false) {
            return null;
        }

        WMTSDocument gwcCapabilities = this.getGWCDocument(logger, dataSourceClone.getConfigManager(), wmsServiceUrl, layerCatalog, dataSourceClone);

        Map<String, L> layerConfigs = null;

        if (gwcCapabilities != null) {
            layerConfigs = new HashMap<String, L>();

            // http://docs.geotools.org/stable/javadocs/org/geotools/data/wms/WebMapServer.html
            Layer rootLayer = gwcCapabilities.getLayer();

            if (rootLayer != null) {
                // The boolean at the end is use to ignore the root from the capabilities document. It can be added (change to false) if some users think it's useful to see the root...
                // NOTE: There should be no metadata document in GWC
                this._propagateLayersInfoMapFromGeoToolRootLayer(logger, layerConfigs, rootLayer, new LinkedList<String>(), dataSourceClone, true);
            }
        }

        return layerConfigs;
    }

    /**
     * Try to find and parse the WMTS document associated with this WMS service.
     * Algo:
     *     Get GWC Capabilities Document URL  or  craft it from the WMS URL.
     *     Get GWC URL  or  craft it from the WMS URL.
     *         NOTE: This URL is used by the generated client only.
     *         IMPORTANT: Ideally, this method would parse the GWC WMS Cap doc instead of the WMTS doc and
     *             get the GWC URL from it. Unfortunately, the WMS Cap doc from GWC do not contains any info
     *             about cached styles.
     *     Try to parse it as a WMTS capabilities document.
     *     If that didn't work, try to rectify the WMTS capabilities document URL and try again.
     *     If that didn't work, return null and add an error.
     */
    public WMTSDocument getGWCDocument(ThreadLogger logger, ConfigManager configManager, URL wmsServiceUrl, LayerCatalog layerCatalog, D dataSourceClone) {
        // GWC service is not mandatory; failing to parse this won't cancel the generation of the client.
        boolean gwcMandatory = false;

        // No WMS service = no need to cache anything.
        if (wmsServiceUrl == null) {
            return null;
        }

        // Used to craft GWC URL
        String gwcSubPath = "gwc";
        URL gwcBaseURL = null;

        // Try to resolve the GWC URL, assuming it works just like GeoServer
        //     We have WMS URL: http://domain.com:80/geoserver/ows?SERVICE=WMS&amp;
        //     We want GWC URL: http://domain.com:80/geoserver/gwc/
        try {
            // baseURL = http://domain.com:80/geoserver/
            // From the WMS Service URL provided by the capabilities document
            URL baseURL = new URL(wmsServiceUrl.getProtocol(), wmsServiceUrl.getHost(), wmsServiceUrl.getPort(), wmsServiceUrl.getPath());

            // gwcBaseURL = http://domain.com:80/geoserver/gwc/
            gwcBaseURL = new URL(baseURL, gwcSubPath+"/");
        } catch (Exception ex) {
            // Error occurred while crafting the GWC URL. This is unlikely to happen.
            logger.log(Level.FINE, "Fail to craft a GWC URL using the WMS URL", ex);
        }

        // Get GWC URL or craft it from WMS URL
        URL gwcUrl = null;
        String gwcUrlStr = dataSourceClone.getWebCacheUrl();
        if (Utils.isBlank(gwcUrlStr)) {
            try {
                if (gwcBaseURL != null) {
                    gwcUrl = new URL(gwcBaseURL, "service/wms");
                    dataSourceClone.setWebCacheUrl(gwcUrl.toString());
                }
            } catch (Exception ex) {
                // This should not happen
                logger.log(Level.WARNING, "Fail craft the GWC URL.", ex);
            }
        }

        // Get GWC Capabilities Document URL or craft it from WMS URL
        URL gwcCapUrl = null;
        File gwcCapFile = null;
        String gwcCapUrlStr = dataSourceClone.getWebCacheCapabilitiesUrl();
        try {
            if (Utils.isBlank(gwcCapUrlStr)) {
                if (gwcBaseURL != null) {
                    gwcCapUrl = new URL(gwcBaseURL, "service/wmts?REQUEST=getcapabilities");
                }
            } else {
                if (gwcCapUrlStr.startsWith("file://")) {
                    // Local file URL
                    gwcCapFile = new File(new URI(gwcCapUrlStr));
                } else {
                    gwcCapUrl = Utils.toURL(gwcCapUrlStr);
                }
            }
        } catch (Exception ex) {
            // This should not happen
            logger.log(Level.WARNING, "Fail craft the GWC Capabilities Document URL.", ex);
        }


        // Parsing of the GWC cap doc (WMTS)
        WMTSDocument document = null;

        Level errorLevel = gwcMandatory ? Level.SEVERE : Level.WARNING;

        if (gwcCapUrl == null && gwcCapFile == null) {
            logger.log(errorLevel, "Can not determine the GWC Capabilities document URL.");
        } else {
            try {
                if (gwcCapFile != null) {
                    document = WMTSParser.parseFile(gwcCapFile, gwcCapUrlStr);
                } else {
                    // Get HTTP headers first. Only try to parse it if it returns a 200 (or equivalent)
                    URLCache.ResponseStatus status = URLCache.getResponseStatus(gwcCapUrl.toString());
                    if (status == null) {
                        logger.log(errorLevel, "Invalid URL: " + gwcCapUrl.toString());
                    } else {
                        Integer statusCode = status.getStatusCode();
                        if (statusCode == null) {
                            logger.log(errorLevel, "Invalid URL: " + gwcCapUrl.toString());
                        } else {
                            if (status.isPageNotFound()) {
                                // Don't bother giving a warning for a URL not found if the document is not mandatory
                                logger.log(errorLevel, "Document not found (404): " + gwcCapUrl.toString());
                            } else if (!status.isSuccess()) {
                                logger.log(errorLevel, "Invalid URL (status code: " + statusCode + "): " + gwcCapUrl.toString());
                            } else {
                                document = WMTSParser.parseURL(logger, configManager, dataSourceClone, gwcCapUrl, gwcMandatory);
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                // This happen every time the admin set a GWC base URL instead of a WMTS capabilities document.
                // The next block try to work around this by crafting a WMTS URL.
                logger.log(errorLevel, "Fail to parse the GWC URL [" + gwcCapUrlStr + "] as a WMTS capabilities document: " + Utils.getExceptionMessage(ex), ex);
            }

            // Try to add some parameters to the given GWC cap url (the provided URL may be incomplete, something like http://domain.com:80/geoserver/gwc/service/wmts)
            if (document == null) {
                // Add a slash a the end of the URL, just in case the URL ends like this: .../geoserver/gwc
                String urlPath = gwcCapUrl.getPath() + "/";
                // Look for "/gwc/"
                int gwcIndex = urlPath.indexOf("/"+gwcSubPath+"/");
                if (gwcIndex >= 0) {
                    try {
                        // Remove everything after "/gwc/"
                        URL modifiedGwcBaseURL = new URL(gwcCapUrl.getProtocol(), gwcCapUrl.getHost(), gwcCapUrl.getPort(), urlPath.substring(0, gwcIndex + gwcSubPath.length() + 2));
                        // Add WMTS URL part
                        URL modifiedGwcCapUrl = new URL(modifiedGwcBaseURL, "service/wmts?REQUEST=getcapabilities");

                        // Get HTTP headers again, but no error message. This URL has been crafted, no need to bother the user with it.
                        URLCache.ResponseStatus status = URLCache.getResponseStatus(modifiedGwcCapUrl.toString());
                        if (status != null && status.isSuccess()) {
                            // Try to download the doc again
                            document = WMTSParser.parseURL(logger, configManager, dataSourceClone, modifiedGwcCapUrl, gwcMandatory);
                        }

                        if (document != null) {
                            URLCache.setRedirection(configManager, gwcCapUrl.toString(), modifiedGwcCapUrl.toString());
                            // If it works, save the crafted URL
                            gwcCapUrl = modifiedGwcCapUrl;
                        }
                    } catch (Exception ex) {
                        // Error occurred while crafting the GWC URL. This is unlikely to happen.
                        logger.log(errorLevel, "Fail to craft a GWC URL using the given GWC URL: " + Utils.getExceptionMessage(ex), ex);
                    }
                }
            }
        }

        if (document != null && (gwcCapUrlStr == null || gwcCapUrlStr.isEmpty())) {
            dataSourceClone.setWebCacheCapabilitiesUrl(gwcCapUrl.toString());
        }

        return document;
    }

    private URL getOperationUrl(OperationType op) {
        if (op == null) {
            return null;
        }
        return op.getGet();
    }

    /**
     * @param wmsCapabilities
     * @param dataSourceClone
     * @return
     */
    private Map<String, L> getLayersInfoFromCaps(
            ThreadLogger logger,
            WMSCapabilities wmsCapabilities,
            D dataSourceClone // Data source of layers (to link the layer to its data source)
    ) {
        if (wmsCapabilities == null) {
            return null;
        }

        // http://docs.geotools.org/stable/javadocs/org/geotools/data/wms/WebMapServer.html
        Layer rootLayer = wmsCapabilities.getLayer();

        Map<String, L> layerConfigs = new HashMap<String, L>();
        // The boolean at the end is use to ignore the root from the capabilities document. It can be added (change to false) if some users think it's useful to see the root...
        this._propagateLayersInfoMapFromGeoToolRootLayer(logger, layerConfigs, rootLayer, new LinkedList<String>(), dataSourceClone, true);

        return layerConfigs;
    }

    /**
     * Set default layer style - This method is overriden in some sub-classes.
     *     See: WMSLayerGenerator.java
     * When the default style is selected by the user, the attribute it is removed from the requests.
     *     This allow to use the cache with some old version of GeoServer.
     * NOTE: Default behaviour is to add a dummy style for the default, but is some case,
     *     the default style can be found so there is no need to add an extra style.
     * @param configManager
     * @param layer
     */
    protected void setDefaultLayerStyle(ConfigManager configManager, L layer) {
        LayerStyleConfig defaultDummyStyle = new LayerStyleConfig(configManager);
        defaultDummyStyle.setName("");
        defaultDummyStyle.setTitle("Default");
        defaultDummyStyle.setDefault(true);

        layer.getStyles().add(defaultDummyStyle);
    }

    // Internal recursive function that takes an actual map of layer and add more layers to it.
    // The method signature suggest that the parameter xmlLayers stay unchanged,
    // and a new map containing the layers is returned. If fact, it modify the
    // map receive as parameter. The other way would be inefficient.
    // I.E.
    // The line
    //     xmlLayers = getXmlLayersFromGeoToolRootLayer(xmlLayers, childLayer, childrenPath);
    // give the same result as
    //     getXmlLayersFromGeoToolRootLayer(xmlLayers, childLayer, childrenPath);
    // The first one is just visualy more easy to understand.
    private void _propagateLayersInfoMapFromGeoToolRootLayer(
            ThreadLogger logger,
            Map<String, L> layerConfigs,
            Layer layer,
            List<String> treePath,
            D dataSourceClone,
            boolean isRoot) {

        if (layer == null) {
            return;
        }

        // GeoTools API documentation is hopeless. The easiest way to know what the API do is to look at the sources:
        // http://svn.osgeo.org/geotools/trunk/modules/extension/wms/src/main/java/org/geotools/data/ows/Layer.java
        List<Layer> children = layer.getLayerChildren();

        if (children != null && !children.isEmpty()) {
            // The layer has children, so it is a Container;
            // If it's not the root, it's a part of the WMS Path, represented as a folder in the GUI.
            List<String> childrenTreePath = new LinkedList<String>(treePath);
            if (!isRoot) {
                String newTreePathPart = layer.getTitle();
                if (Utils.isBlank(newTreePathPart)) {
                    newTreePathPart = layer.getName();
                }

                if (Utils.isNotBlank(newTreePathPart)) {
                    childrenTreePath.add(newTreePathPart);
                }
            }

            for (Layer childLayer : children) {
                this._propagateLayersInfoMapFromGeoToolRootLayer(logger, layerConfigs, childLayer, childrenTreePath, dataSourceClone, false);
            }
        } else {
            // The layer do not have any children, so it is a real layer

            // Create a string representing the path
            StringBuilder treePathBuf = new StringBuilder();
            for (String treePathPart : treePath) {
                if (Utils.isNotBlank(treePathPart)) {
                    if (treePathBuf.length() > 0) {
                        treePathBuf.append("/");
                    }
                    treePathBuf.append(treePathPart);
                }
            }

            L layerConfig = this.layerToLayerConfig(logger, layer, treePathBuf.toString(), dataSourceClone);
            if (layerConfig != null) {
                layerConfigs.put(layerConfig.getLayerId(), layerConfig);
            }
        }
    }

    /**
     * Convert a GeoTool Layer into a AtlasMapper Layer
     * @param layer
     * @param treePath
     * @param dataSourceClone
     * @return
     */
    private L layerToLayerConfig(
            ThreadLogger logger,
            Layer layer,
            String treePath,
            D dataSourceClone) {

        L layerConfig = this.createLayerConfig(dataSourceClone.getConfigManager());

        String layerName = layer.getName();
        if (Utils.isBlank(layerName)) {
            logger.log(Level.WARNING, String.format("The Capabilities Document of the data source [%s] contains layers without name (other than the root layer).", dataSourceClone.getDataSourceName()));
            return null;
        }

        TC211Document tc211Document = null;
        List<MetadataURL> metadataUrls = layer.getMetadataURL();
        if (metadataUrls != null && !metadataUrls.isEmpty()) {
            for (MetadataURL metadataUrl : metadataUrls) {
                if (tc211Document == null && "TC211".equalsIgnoreCase(metadataUrl.getType()) && "text/xml".equalsIgnoreCase(metadataUrl.getFormat())) {
                    URL url = metadataUrl.getUrl();
                    if (url != null) {
                        try {
                            tc211Document = TC211Parser.parseURL(logger, dataSourceClone.getConfigManager(), dataSourceClone, url, false, true);
                            if (tc211Document == null || tc211Document.isEmpty()) { tc211Document = null; }
                        } catch (Exception e) {
                            logger.log(Level.WARNING, String.format("Unexpected exception while parsing the metadata document URL: %s%n" +
                                    "The information provided by the GetCapabilities document indicate that the file is a " +
                                    "TC211 text/xml file, which seems to not be the case: %s",
                                    url.toString(), Utils.getExceptionMessage(e)), e);
                        }
                    }
                }
            }

            // There is metadata URL, but none of the one set with TC211 text/xml format are suitable.
            // Sometime, there is valid metadata URL but they have been entered incorrectly.
            // Brute force through all metadata URL and cross fingers to find one that will provide some usable info.
            if (tc211Document == null) {
                logger.log(Level.WARNING, String.format("Could not find a valid TC211 text/xml metadata document for layer %s of %s. Trying them all whatever their specified mime type.",
                        layerName, dataSourceClone.getDataSourceName()));
                MetadataURL validMetadataUrl = null;
                for (MetadataURL metadataUrl : metadataUrls) {
                    if (tc211Document == null) {
                        URL url = metadataUrl.getUrl();
                        if (url != null) {
                            try {
                                tc211Document = TC211Parser.parseURL(logger, dataSourceClone.getConfigManager(), dataSourceClone, url, false, false);
                                if (tc211Document != null && !tc211Document.isEmpty()) {
                                    validMetadataUrl = metadataUrl;
                                } else {
                                    logger.log(Level.FINE, String.format("Invalid metadata document: %s%n      Identified as \"%s - %s\"",
                                            url.toString(),
                                            metadataUrl.getType(),
                                            metadataUrl.getFormat()
                                    ));
                                    tc211Document = null;
                                }
                            } catch (Exception ex) {
                                LOGGER.log(Level.FINE, String.format("Invalid metadata document: %s%n      Identified as \"%s - %s\"%n      Exception message: %s",
                                        url.toString(),
                                        metadataUrl.getType(),
                                        metadataUrl.getFormat(),
                                        Utils.getExceptionMessage(ex)
                                ), ex);
                            }
                        }
                    }
                }
                if (tc211Document != null && validMetadataUrl != null) {
                    logger.log(Level.INFO, String.format("Valid metadata document: %s%n      Identified as \"%s - %s\"",
                            validMetadataUrl.getUrl().toString(),
                            validMetadataUrl.getType(),
                            validMetadataUrl.getFormat()
                    ));
                }
            }
        }

        layerConfig.setLayerId(layerName);
        this.ensureUniqueLayerId(layerConfig, dataSourceClone);
        layerName = layerConfig.getLayerName();

        String title = layer.getTitle();

        // Build the description using info found in the Capabilities document and the MEST document.
        StringBuilder descriptionSb = new StringBuilder();

        TC211Document.Link layerLink = this.getMetadataLayerLink(tc211Document, layerName);

        String layerDescription = layer.get_abstract();
        String metadataDescription = tc211Document == null ? null : tc211Document.getAbstract();
        String metadataLayerDescription = layerLink == null ? null : layerLink.getDescription();
        String metadataLinksWikiFormat = this.getMetadataLinksWikiFormat(tc211Document);
        JSONSortedObject metadataDownloadLinks = this.getDownloadLinks(logger, tc211Document);

        // Clean-up
        if (layerDescription != null) { layerDescription = layerDescription.trim(); }
        if (metadataDescription != null) { metadataDescription = metadataDescription.trim(); }
        if (metadataLayerDescription != null) { metadataLayerDescription = metadataLayerDescription.trim(); }
        if (metadataLinksWikiFormat != null) { metadataLinksWikiFormat = metadataLinksWikiFormat.trim(); }

        // Layer description: Get from cap doc, or from MEST if cap doc do not have one.
        if (layerDescription != null && !layerDescription.isEmpty()) {
            descriptionSb.append(layerDescription);
        } else if (metadataLayerDescription != null && !metadataLayerDescription.isEmpty()) {
            descriptionSb.append(metadataLayerDescription);
        }

        if (metadataDescription != null && !metadataDescription.isEmpty()) {
            if (descriptionSb.length() > 0) {
                descriptionSb.append("\n\n*Dataset description*\n");
            }
            descriptionSb.append(metadataDescription);
        }

        // If cap doc has a description, the layer description from the MEST will appear here.
        if (layerDescription != null && !layerDescription.isEmpty() &&
                metadataLayerDescription != null && !metadataLayerDescription.isEmpty()) {
            if (descriptionSb.length() > 0) {
                descriptionSb.append("\n\n*Dataset layer description*\n");
            }
            descriptionSb.append(metadataLayerDescription);
        }

        if (metadataLinksWikiFormat != null && !metadataLinksWikiFormat.isEmpty()) {
            if (descriptionSb.length() > 0) {
                descriptionSb.append("\n\n");
            }
            descriptionSb.append("*Online resources*\n");
            descriptionSb.append(metadataLinksWikiFormat);
        }

        if (Utils.isNotBlank(title)) {
            layerConfig.setTitle(title);
        }

        // If a description has been found, either in the metadata or capabilities document, set it in the layerConfig.
        if (descriptionSb.length() > 0) {
            layerConfig.setDescription(descriptionSb.toString());
        }

        if (metadataDownloadLinks != null) {
            layerConfig.setDownloadLinks(metadataDownloadLinks);
        }

        layerConfig.setWmsQueryable(layer.isQueryable());

        if (Utils.isNotBlank(treePath)) {
            layerConfig.setTreePath(treePath);
        }

        List<StyleImpl> styleImpls = layer.getStyles();
        if (styleImpls != null && !styleImpls.isEmpty()) {
            List<LayerStyleConfig> styles = new ArrayList<LayerStyleConfig>(styleImpls.size());
            for (StyleImpl styleImpl : styleImpls) {
                LayerStyleConfig styleConfig = this.styleToLayerStyleConfig(dataSourceClone.getConfigManager(), styleImpl);
                if (styleConfig != null) {
                    styles.add(styleConfig);
                }
            }
            if (!styles.isEmpty()) {
                layerConfig.setStyles(styles);
            }
        }

        CRSEnvelope boundingBox = layer.getLatLonBoundingBox();
        if (boundingBox != null) {
            double[] boundingBoxArray = {
                    boundingBox.getMinX(), boundingBox.getMinY(),
                    boundingBox.getMaxX(), boundingBox.getMaxY()
            };
            layerConfig.setLayerBoundingBox(boundingBoxArray);
        }

        return layerConfig;
    }

    private TC211Document.Link getMetadataLayerLink(TC211Document tc211Document, String layerName) {
        if (tc211Document != null) {
            List<TC211Document.Link> links = tc211Document.getLinks();
            if (links != null && !links.isEmpty()) {
                for (TC211Document.Link link : links) {
                    // Only display links with none null URL.
                    if (Utils.isNotBlank(link.getUrl())) {
                        TC211Document.Protocol linkProtocol = link.getProtocol();
                        if (linkProtocol != null) {
                            if (linkProtocol.isOGC()) {
                                // If the link is a OGC url (most likely WMS GetMap) and the url match the layer url, parse its description.
                                if (layerName.equalsIgnoreCase(link.getName())) {
                                    return link;
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Return download links, in wiki format
     * @param tc211Document
     * @return
     */
    private String getMetadataLinksWikiFormat(TC211Document tc211Document) {
        StringBuilder onlineResources = new StringBuilder();

        if (tc211Document != null) {
            // Add links found in the metadata document and layer description (if any)
            List<TC211Document.Link> links = tc211Document.getLinks();
            if (links != null && !links.isEmpty()) {
                // Set the Online Resources, using wiki format
                for (TC211Document.Link link : links) {
                    // Only display links with none null URL.
                    if (Utils.isNotBlank(link.getUrl())) {
                        TC211Document.Protocol linkProtocol = link.getProtocol();
                        if (linkProtocol != null) {
                            if (linkProtocol.isWWW() && !linkProtocol.isDownloadable()) {
                                // Dataset links such as point of truth, data download, etc.
                                String linkUrl = link.getUrl();
                                String linkTitle = link.getDescription();
                                if (Utils.isBlank(linkTitle)) {
                                    linkTitle = link.getName();
                                }

                                // Clean-up the URL, to be sure it will be parsable by the wiki format parser.
                                // NOTE: URLEncoder can not be used here since it would also encode parts that
                                //     should not be encoded (like the "http://")
                                if (Utils.isNotBlank(linkUrl)) {
                                    linkUrl = linkUrl
                                            // Replace square brackets with their URL encoding.
                                            //     (they may cause problem with the wiki format parser)
                                            .replace("[", "%5B").replace("]", "%5D")
                                            // Replace pipe with its URL encoding.
                                            //     (it may cause problem with the wiki format parser)
                                            .replace("|", "%7C")
                                            // Encore space using its URL encoding.
                                            // NOTE: "%20" is safer than "+" since the "+" is only valid in the
                                            //     query string (the part after the ?).
                                            .replace(" ", "%20")
                                            // Other white spaces should not be present in a URL anyway.
                                            .replace("\\s", "");
                                }

                                // Clean-up the title, to be sure it will be parsable by the wiki format parser.
                                if (Utils.isNotBlank(linkTitle)) {
                                    linkTitle = linkTitle
                                            // Replace chain of whitespaces with one space.
                                            //     (newline in a link cause the wiki format parser to fail)
                                            .replaceAll("\\s+", " ");
                                }

                                // Bullet list of URLs, in Wiki format:
                                // * [[url|title]]
                                // * [[url|title]]
                                // * [[url|title]]
                                // ...
                                onlineResources.append("* [[");
                                onlineResources.append(linkUrl);
                                if (Utils.isNotBlank(linkTitle)) {
                                    onlineResources.append("|");
                                    onlineResources.append(linkTitle);
                                }
                                onlineResources.append("]]\n");
                            }
                        }
                    }
                }
            }
        }

        return onlineResources.length() > 0 ? onlineResources.toString() : null;
    }

    private JSONSortedObject getDownloadLinks(ThreadLogger logger, TC211Document tc211Document) {
        JSONSortedObject downloadLinks = new JSONSortedObject();

        if (tc211Document != null) {
            // Add links found in the metadata document and layer description (if any)
            List<TC211Document.Link> links = tc211Document.getLinks();
            if (links != null && !links.isEmpty()) {
                // Set the Online Resources, using wiki format
                for (TC211Document.Link link : links) {
                    // Only display links with none null URL.
                    if (Utils.isNotBlank(link.getUrl())) {
                        TC211Document.Protocol linkProtocol = link.getProtocol();
                        if (linkProtocol != null) {
                            if (linkProtocol.isDownloadable()) {

                                // Dataset links such as point of truth, data download, etc.
                                String linkUrl = link.getUrl();
                                String linkTitle = link.getDescription();
                                if (Utils.isBlank(linkTitle)) {
                                    linkTitle = link.getName();
                                }

                                try {
                                    downloadLinks.put(linkUrl, linkTitle);
                                } catch(JSONException ex) {
                                    // I don't think that exception can even occur.
                                    logger.log(Level.SEVERE, "Can not add an attribute to a JSON Object: " + Utils.getExceptionMessage(ex), ex);
                                }
                            }
                        }
                    }
                }
            }
        }

        return downloadLinks.length() > 0 ? downloadLinks : null;
    }

    private LayerStyleConfig styleToLayerStyleConfig(ConfigManager configManager, StyleImpl style) {
        String name = style.getName();
        InternationalString intTitle = style.getTitle();
        InternationalString intDescription = style.getAbstract();

        // style.getLegendURLs() is now implemented!! (gt-wms ver. 8.1) But there is an error in the code...
        // See modules/extension/wms/src/main/java/org/geotools/data/wms/xml/WMSComplexTypes.java line 4172
        // They use the hardcoded index "2" instead of "i":
        //     [...]
        //     if (sameName(elems[3], value[i])) {
        //         legendURLS.add((String)value[2].getValue());
        //     }
        //     [...]
        // Should be
        //     [...]
        //     if (sameName(elems[3], value[i])) {
        //         legendURLS.add((String)value[i].getValue());
        //     }
        //     [...]
        // http://osgeo-org.1560.n6.nabble.com/svn-r38810-in-branches-2-7-x-modules-extension-wms-src-main-java-org-geotools-data-wms-xml-test-javaa-tt4981882.html
        //List<String> legendURLs = style.getLegendURLs();

        LayerStyleConfig styleConfig = new LayerStyleConfig(configManager);
        styleConfig.setName(name);

        if (intTitle != null) {
            styleConfig.setTitle(intTitle.toString());
        }
        if (intDescription != null) {
            styleConfig.setDescription(intDescription.toString());
        }

        return styleConfig;
    }
}
