/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2020 Australian Institute of Marine Science
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
import au.gov.aims.atlasmapperserver.Utils;
import au.gov.aims.atlasmapperserver.cache.URLCache;
import au.gov.aims.atlasmapperserver.dataSourceConfig.ThreddsDataSourceConfig;
import au.gov.aims.atlasmapperserver.layerConfig.LayerCatalog;
import au.gov.aims.atlasmapperserver.layerConfig.ThreddsLayerConfig;
import au.gov.aims.atlasmapperserver.thread.RevivableThread;
import au.gov.aims.atlasmapperserver.thread.RevivableThreadInterruptedException;
import au.gov.aims.atlasmapperserver.thread.ThreadLogger;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import thredds.client.catalog.Access;
import thredds.client.catalog.CatalogRef;
import thredds.client.catalog.Dataset;
import thredds.client.catalog.ServiceType;
import thredds.client.catalog.builder.CatalogBuilder;
import thredds.client.catalog.Catalog;
import thredds.client.catalog.tools.CatalogCrawler;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.CancelTaskImpl;
import ucar.nc2.util.Indent;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ThreddsLayerGenerator extends AbstractWMSLayerGenerator<ThreddsLayerConfig, ThreddsDataSourceConfig> {
    @Override
    protected ThreddsLayerConfig createLayerConfig(ConfigManager configManager) {
        return new ThreddsLayerConfig(configManager);
    }

    @Override
    public LayerCatalog generateRawLayerCatalog(
            ThreadLogger logger,
            URLCache urlCache,
            ThreddsDataSourceConfig dataSourceClone,
            boolean redownloadCatalogueFiles,
            boolean redownloadMestRecordFiles
    ) throws RevivableThreadInterruptedException {

        RevivableThread.checkForInterruption();

        LayerCatalog layerCatalog = new LayerCatalog();
        Map<String, ThreddsLayerConfig> layersMap = null;
        URL wmsServiceUrl = null;

        String dataSourceServiceUrlStr = dataSourceClone.getServiceUrl();

        Catalog catalog;
        try {
            catalog = this.getCatalog(logger, dataSourceServiceUrlStr);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, String.format("Error occurred while downloading the THREDDS catalogue URL (%s): %s",
                    dataSourceServiceUrlStr, Utils.getExceptionMessage(ex)), ex);
            return null;
        }

        if (catalog == null) {
            logger.log(Level.SEVERE, String.format("The THREDDS catalogue is null (%s)",
                    dataSourceServiceUrlStr));
            return null;
        }

        CatalogCrawler.Listener listener = new CatalogCrawler.Listener() {
            @Override
            public void getDataset(Dataset ds, Object context) {
                logger.log(Level.INFO, "FOUND DATASET: " + ds.getId());
            }
        };

        CancelTask cancelTask = new CancelTask() {
            @Override
            public boolean isCancel() {
                return RevivableThread.isInterrupted();
            }

            @Override
            public void setError(String msg) {
                logger.log(Level.SEVERE, msg);
            }

            @Override
            public void setProgress(String msg, int progress) {
                logger.log(Level.INFO, String.format("%s. Progress: %d", msg, progress));
            }
        };

        PrintWriter printWriter = new PrintWriter(new Writer() {
            private StringBuilder buffer = new StringBuilder();
            private final String nl = System.lineSeparator();
            private final String logTemplate = "CatalogCrawler: %s";

            @Override
            public void write(@Nonnull char[] cbuf, int off, int len) {
                this.buffer.append(cbuf, off, len);
                if (this.buffer.indexOf(this.nl) != -1) {
                    this.writeLogs();
                }
            }

            @Override
            public void flush() {
                this.buffer.append(this.nl);
                this.writeLogs();
            }

            public void writeLogs() {
                if (this.buffer.length() > 0) {
                    // Flush every string ending with a new line
                    String bufferStr = this.buffer.toString();
                    this.buffer = new StringBuilder();

                    String[] logs = bufferStr.split(this.nl);
                    if (logs.length > 0) {
                        for (int i=0; i<logs.length-1; i++) {
                            String log = logs[i];
                            if (!log.trim().isEmpty()) {
                                logger.log(Level.INFO, String.format(this.logTemplate, log));
                            }
                        }

                        // If the last bit didn't end with a new line, keep it for the next flush
                        String lastLog = logs[logs.length-1];
                        if (!lastLog.trim().isEmpty()) {
                            if (bufferStr.endsWith(nl)) {
                                logger.log(Level.INFO, String.format(this.logTemplate, lastLog));
                            } else {
                                this.buffer.append(lastLog);
                            }
                        }
                    }
                }
            }

            @Override
            public void close() { }
        });

        CatalogCrawler crawler = new CatalogCrawler(CatalogCrawler.Type.all, 0, null, listener, cancelTask, printWriter, null);
        try {
            int found = crawler.crawl(catalog);
            logger.log(Level.INFO, "FOUND: " + found);
            logger.log(Level.INFO, "FAILURES: " + crawler.getNumReadFailures());
        } catch (Exception ex) {
            logger.log(Level.SEVERE, String.format("Error occurred while downloading the THREDDS catalogue URL (%s): %s",
                    dataSourceServiceUrlStr, Utils.getExceptionMessage(ex)), ex);
            return null;
        }

/*
        Catalog catalog;
        try {
            catalog = this.getCatalog(logger, dataSourceServiceUrlStr);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, String.format("Error occurred while downloading the THREDDS catalogue URL (%s): %s",
                    dataSourceServiceUrlStr, Utils.getExceptionMessage(ex)), ex);
            return null;
        }

        if (catalog == null) {
            logger.log(Level.SEVERE, String.format("The THREDDS catalogue is null (%s)",
                    dataSourceServiceUrlStr));
            return null;
        }

        // TODO Recursive lookup!
        for (Dataset dataset : catalog.getAllDatasets()) {
            logger.log(Level.INFO, "FOUND DATASET ID: " + dataset.getId());

            URI wmsUri = this.getDatasetWmsUri(logger, dataset);
            // String urlPath = dataset.getUrlPath(); // null

            logger.log(Level.INFO, "CatalogUrl: " + dataset.getCatalogUrl());
            logger.log(Level.INFO, "CollectionType: " + dataset.getCollectionType());
            logger.log(Level.INFO, "FeatureTypeName: " + dataset.getFeatureTypeName());
            logger.log(Level.INFO, "DataFormatName: " + dataset.getDataFormatName());
            logger.log(Level.INFO, "Authority: " + dataset.getAuthority());
            logger.log(Level.INFO, "IdOrPath: " + dataset.getIdOrPath());
            logger.log(Level.INFO, "Name: " + dataset.getName());

            for (Dataset subDataset : dataset.getDatasets()) {
                logger.log(Level.INFO, "FOUND SUB DATASET ID: " + subDataset.getId());
                logger.log(Level.INFO, "SubCatalogUrl: " + subDataset.getCatalogUrl());
            }
        }

        ConfigManager configManager = dataSourceClone.getConfigManager();
*/

/*
        WMSCapabilities wmsCapabilities = null;
        try {
            wmsCapabilities = this.getWMSCapabilitiesResponse(
                    logger,
                    urlCache,
                    this.wmsVersion,
                    dataSourceClone,
                    dataSourceServiceUrlStr,
                    redownloadCatalogueFiles);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, String.format("Error occurred while parsing the [WMS GetCapabilities document](%s): %s",
                    dataSourceServiceUrlStr, Utils.getExceptionMessage(ex)), ex);
            return null;
        }

        if (wmsCapabilities == null) {
            logger.log(Level.SEVERE, String.format("Could not parse the [GetCapabilities document](%s).",
                    dataSourceServiceUrlStr));
            return null;
        }

        WMSRequest wmsRequestCapabilities = wmsCapabilities.getRequest();
        if (wmsRequestCapabilities != null) {
            if (Utils.isNotBlank(dataSourceClone.getGetMapUrl())) {
                try {
                    wmsServiceUrl = Utils.toURL(dataSourceClone.getGetMapUrl());
                } catch (Exception ex) {
                    logger.log(Level.WARNING, String.format("Can not create a URL object from the string %s: %s",
                            dataSourceClone.getGetMapUrl(), Utils.getExceptionMessage(ex)), ex);
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

        layersMap = this.getLayersInfoFromCaps(logger, urlCache, wmsCapabilities, dataSourceClone, redownloadMestRecordFiles);

        // Set default style of each layer
        if (layersMap != null && !layersMap.isEmpty()) {
            for (ThreddsLayerConfig layer : layersMap.values()) {
                this.setDefaultLayerStyle(configManager, layer);
            }
        }

        if (wmsServiceUrl == null && dataSourceServiceUrlStr != null) {
            try {
                wmsServiceUrl = Utils.toURL(dataSourceServiceUrlStr);
            } catch (Exception ex) {
                logger.log(Level.WARNING, String.format("Can not create a URL object from the string %s: %s",
                        dataSourceServiceUrlStr, Utils.getExceptionMessage(ex)), ex);
            }
        }

        RevivableThread.checkForInterruption();

        Collection<ThreddsLayerConfig> layers = null;
        if (layersMap != null && !layersMap.isEmpty()) {
            if (dataSourceClone.isWebCacheEnable() != null && dataSourceClone.isWebCacheEnable() && wmsServiceUrl != null) {
                layers = new ArrayList<ThreddsLayerConfig>(layersMap.size());
                Map<String, ThreddsLayerConfig> cachedLayers = this.generateRawCachedLayerConfigs(
                        logger, urlCache, dataSourceClone, wmsServiceUrl, redownloadCatalogueFiles, redownloadMestRecordFiles);

                RevivableThread.checkForInterruption();

                // Since we are not parsing the Cache server WMS capability document, we can not find which version of WMS it is using...
                // Fallback to 1.1.1, it's very well supported.
                dataSourceClone.setCacheWmsVersion("1.1.1");

                // Set cached flags
                boolean fallback = false;
                for (Map.Entry<String, ThreddsLayerConfig> layerEntry : layersMap.entrySet()) {
                    boolean cached = false;
                    ThreddsLayerConfig layer = layerEntry.getValue();
                    if (cachedLayers == null) {
                        // Empty list means no cached layers
                        // NULL means WMTS service not available. GeoServer 2.1.X use to have that problem...
                        // Fallback (GeoServer 2.1.X)  - assume GeoWebCache support cache for all layers, default style only
                        fallback = true;
                        cached = true;
                        this.setLayerStylesCacheFlag(layer.getStyles(), null);
                    } else if (cachedLayers.containsKey(layerEntry.getKey())) {
                        ThreddsLayerConfig cachedLayer = cachedLayers.get(layerEntry.getKey());
                        if (cachedLayer != null) {
                            cached = true;
                            this.setLayerStylesCacheFlag(layer.getStyles(), cachedLayer.getStyles());
                        }
                    }
                    layer.setCached(cached);

                    layers.add(layer);
                }
                RevivableThread.checkForInterruption();

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
*/

        RevivableThread.checkForInterruption();

        return layerCatalog;
    }

    private Catalog getCatalog(ThreadLogger logger, String catalogueUrlStr) throws Exception {
        logger.log(Level.INFO, String.format("Downloading THREDDS catalogue URL: %s", catalogueUrlStr));

        // 1st, try with the provided URL
        URI catalogueUri;
        try {
            String redirectUrlStr = Utils.getRedirectUrl(catalogueUrlStr);
            if (!catalogueUrlStr.equals(redirectUrlStr)) {
                logger.log(Level.INFO, String.format("Redirection URL: " + redirectUrlStr));
            }
            catalogueUri = new URL(redirectUrlStr).toURI();
        } catch (Exception ex) {
            logger.log(Level.SEVERE, String.format("Error occurred while parsing the [THREDDS catalogue URL](%s): %s",
                    catalogueUrlStr, Utils.getExceptionMessage(ex)), ex);
            return null;
        }

        // 2nd, if 1st didn't work, try adding "/catalog.xml" at the end of the URL
        Catalog catalog = this.getCatalog(catalogueUri);
        if (catalog == null) {
            if (!catalogueUrlStr.endsWith("/catalog.xml")) {
                if (!catalogueUrlStr.endsWith("/")) {
                    catalogueUrlStr = catalogueUrlStr + "/";
                }
                catalogueUrlStr = catalogueUrlStr + "catalog.xml";
                catalog = this.getCatalog(logger, catalogueUrlStr);
            } else {
                logger.log(Level.SEVERE, String.format("Invalid catalogue URL: %s (resolved as %s)", catalogueUrlStr, catalogueUri));
            }
        }

        return catalog;
    }

    private Catalog getCatalog(URI catalogueUri) throws Exception {
        CatalogBuilder catalogBuilder = new CatalogBuilder();

        HttpGet httpGet = null;
        try (CloseableHttpClient httpClient = Utils.createHttpClient()) {
            httpGet = new HttpGet(catalogueUri);
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                StatusLine httpStatus = response.getStatusLine();
                if (httpStatus != null) {
                    int statusCode = httpStatus.getStatusCode();
                    if (statusCode < 200 || statusCode >= 300) {
                        throw new IOException(String.format("Error occurred while downloading the THREDDS Catalogue %s: %s (%d)",
                                catalogueUri, httpStatus.getReasonPhrase(), statusCode));
                    }
                }

                // The entity is streamed
                HttpEntity entity = response.getEntity();
                if (entity == null) {
                    throw new IOException(String.format("Can not use the downloaded THREDDS Catalogue %s: The response entity is null",
                            catalogueUri));
                }

                try (InputStream in = entity.getContent()) {
                    return catalogBuilder.buildFromStream(in, catalogueUri);
                }
            }
        } finally {
            if (httpGet != null) {
                // Cancel the connection, if it's still alive
                httpGet.abort();
                // Close connections
                httpGet.reset();
            }
        }
    }

    private URI getDatasetWmsUri(ThreadLogger logger, Dataset dataset) {
        List<Access> accesses = dataset.getAccess();
        if (accesses != null && !accesses.isEmpty()) {
            for (Access access : accesses) {
logger.log(Level.INFO, access.getService().getType().name());
                if (ServiceType.WMS.equals(access.getService().getType())) {
                    return access.getStandardUri();
                }
            }
        }

        return null;
    }
}
