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
import au.gov.aims.atlasmapperserver.dataSourceConfig.WMSDataSourceConfig;
import au.gov.aims.atlasmapperserver.layerConfig.AbstractLayerConfig;
import au.gov.aims.atlasmapperserver.layerConfig.LayerCatalog;
import au.gov.aims.atlasmapperserver.layerConfig.ThreddsLayerConfig;
import au.gov.aims.atlasmapperserver.layerConfig.WMSLayerConfig;
import au.gov.aims.atlasmapperserver.thread.RevivableThread;
import au.gov.aims.atlasmapperserver.thread.RevivableThreadInterruptedException;
import au.gov.aims.atlasmapperserver.thread.ThreadLogger;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import thredds.client.catalog.Access;
import thredds.client.catalog.Dataset;
import thredds.client.catalog.ServiceType;
import thredds.client.catalog.builder.CatalogBuilder;
import thredds.client.catalog.Catalog;
import thredds.client.catalog.tools.CatalogCrawler;
import ucar.nc2.util.CancelTask;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.util.logging.Level;

public class ThreddsLayerGenerator extends AbstractWMSLayerGenerator<ThreddsLayerConfig, ThreddsDataSourceConfig> {
    // NOTE: The layerIdPrefix is set by the generateRawLayerCatalog method.
    //   This is not thread safe, but it's OK since the layers are created sequentially.
    private String layerIdPrefix = "";

    @Override
    protected String getUniqueLayerId(WMSLayerConfig layer, WMSDataSourceConfig dataSourceConfig) throws RevivableThreadInterruptedException {
        RevivableThread.checkForInterruption();
        return this.layerIdPrefix + "_" + layer.getLayerId();
    }

    @Override
    protected ThreddsLayerConfig createLayerConfig(ConfigManager configManager) {
        return new ThreddsLayerConfig(configManager);
    }

    @Override
    public LayerCatalog generateRawLayerCatalog(
            ThreadLogger logger,
            URLCache urlCache,
            ThreddsDataSourceConfig dataSourceClone,
            boolean redownloadGetCapabilitiesFiles,
            boolean redownloadMestRecordFiles
    ) throws RevivableThreadInterruptedException {

        RevivableThread.checkForInterruption();

        LayerCatalog layerCatalog = new LayerCatalog();

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

        CatalogCrawlerCancelTask cancelTask = new CatalogCrawlerCancelTask(logger);

        CatalogCrawler.Listener listener = new CatalogCrawler.Listener() {
            @Override
            public void getDataset(Dataset dataset, Object context) {
                ThreddsLayerGenerator.this.layerIdPrefix = dataset.getId();
                try {
                    URI wmsUri = ThreddsLayerGenerator.getDatasetWmsUri(dataset);
                    if (wmsUri != null) {
                        String wmsURLStr = wmsUri.toString();
                        // Prepare the datasource before harvesting
                        // NOTE: Reset some values to be sure we get the ones
                        //   from the current GetCapabilities doc and not values
                        //   from the previous harvest.
                        dataSourceClone.setServiceUrl(wmsURLStr);
                        dataSourceClone.setFeatureRequestsUrl((String)null);
                        dataSourceClone.setLegendUrl((String)null);
                        dataSourceClone.setWmsVersion(null);

                        LayerCatalog childLayerCatalog = ThreddsLayerGenerator.super.generateRawLayerCatalog(
                                logger,
                                urlCache,
                                dataSourceClone,
                                redownloadGetCapabilitiesFiles,
                                redownloadMestRecordFiles
                        );

                        if (childLayerCatalog != null && !childLayerCatalog.isEmpty()) {
                            for (AbstractLayerConfig layerConfig : childLayerCatalog.getLayers()) {
                                if (layerConfig instanceof ThreddsLayerConfig) {
                                    ThreddsLayerConfig threddsLayerConfig = (ThreddsLayerConfig)layerConfig;
                                    threddsLayerConfig.setDatasetId(dataset.getId());

                                    // Copy some the datasource settings to the layers since not
                                    // all layers from a THREDDS server share the same values.
                                    threddsLayerConfig.setServiceUrl(dataSourceClone.getServiceUrl());
                                    threddsLayerConfig.setFeatureRequestsUrl(dataSourceClone.getFeatureRequestsUrl());
                                    threddsLayerConfig.setLegendUrl(dataSourceClone.getLegendUrl());
                                    threddsLayerConfig.setWmsVersion(dataSourceClone.getWmsVersion());
                                }
                                logger.log(Level.INFO, String.format("Adding layer: %s", layerConfig.getLayerId()));
                                layerCatalog.addLayer(layerConfig);
                            }
                        }
                    }
                } catch(RevivableThreadInterruptedException ex) {
                    cancelTask.cancel();
                }
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

        RevivableThread.checkForInterruption();

        // NOTE: The CatalogCrawler do not support S3Datasource
        //   I will need to extend it to add support for it.
        CatalogCrawler crawler = new CatalogCrawler(
                CatalogCrawler.Type.all, // Type of Crawler (Default: all).
                0, // Max dataset to parse: 0 to parse every dataset.
                null, // Dataset filter: Null for no filter.
                listener, // Listener: Callback function to call when new dataset is found.
                cancelTask, // CancelTask: Used to cancel the crawl at any point.
                printWriter, // PrintWriter: Used to output logs.
                null); // Context: Arbitrary object to send to the listener with every dataset found.
        try {
            int found = crawler.crawl(catalog);
            logger.log(Level.INFO, "Catalogues found: " + found);
            logger.log(Level.INFO, "Invalid catalogues: " + crawler.getNumReadFailures());
        } catch (Exception ex) {
            logger.log(Level.SEVERE, String.format("Error occurred while downloading the THREDDS catalogue URL (%s): %s",
                    dataSourceServiceUrlStr, Utils.getExceptionMessage(ex)), ex);
            return null;
        }

        RevivableThread.checkForInterruption();

        return layerCatalog;
    }

    private Catalog getCatalog(ThreadLogger logger, String catalogueUrlStr) throws Exception, RevivableThreadInterruptedException {
        logger.log(Level.INFO, String.format("Downloading THREDDS catalogue URL: %s", catalogueUrlStr));

        // 1st, try with the provided URL
        URI catalogueUri;
        try {
            String redirectUrlStr = Utils.getRedirectUrl(Utils.toURL(catalogueUrlStr));
            RevivableThread.checkForInterruption();
            if (!catalogueUrlStr.equals(redirectUrlStr)) {
                logger.log(Level.INFO, String.format("Redirection URL: %s", redirectUrlStr));
            }
            catalogueUri = new URL(redirectUrlStr).toURI();
        } catch (Exception ex) {
            logger.log(Level.SEVERE, String.format("Error occurred while parsing the [THREDDS catalogue URL](%s): %s",
                    catalogueUrlStr, Utils.getExceptionMessage(ex)), ex);
            return null;
        }

        // 2nd, if 1st didn't work, try adding "/catalog.xml" at the end of the URL
        Catalog catalog = this.getCatalog(catalogueUri);
        RevivableThread.checkForInterruption();

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

    private static URI getDatasetWmsUri(Dataset dataset) {
        Access wmsAccess = dataset.getAccess(ServiceType.WMS);
        if (wmsAccess != null) {
            return wmsAccess.getStandardUri();
        }

        return null;
    }

    public static class CatalogCrawlerCancelTask implements CancelTask {
        private final ThreadLogger logger;
        private boolean cancel = false;

        public CatalogCrawlerCancelTask(ThreadLogger logger) {
            this.logger = logger;
        }

        public void cancel() {
            this.cancel = true;
        }

        @Override
        public boolean isCancel() {
            return this.cancel || RevivableThread.isInterrupted();
        }

        @Override
        public void setError(String msg) {
            this.logger.log(Level.SEVERE, msg);
        }

        @Override
        public void setProgress(String msg, int progress) {
            this.logger.log(Level.INFO, String.format("%s. Progress: %d", msg, progress));
        }
    }
}
