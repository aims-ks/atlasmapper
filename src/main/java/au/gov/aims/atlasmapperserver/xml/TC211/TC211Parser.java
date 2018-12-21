/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2012 Australian Institute of Marine Science
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
package au.gov.aims.atlasmapperserver.xml.TC211;

import au.gov.aims.atlasmapperserver.ConfigManager;
import au.gov.aims.atlasmapperserver.Utils;
import au.gov.aims.atlasmapperserver.cache.CacheEntry;
import au.gov.aims.atlasmapperserver.cache.URLCache;
import au.gov.aims.atlasmapperserver.dataSourceConfig.AbstractDataSourceConfig;
import au.gov.aims.atlasmapperserver.jsonWrappers.client.LayerWrapper;
import au.gov.aims.atlasmapperserver.thread.RevivableThread;
import au.gov.aims.atlasmapperserver.thread.RevivableThreadInterruptedException;
import au.gov.aims.atlasmapperserver.thread.ThreadLogger;
import org.json.JSONException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

// Basic SAX example, to have something to starts with.
// http://www.mkyong.com/java/how-to-read-xml-file-in-java-sax-parser/
public class TC211Parser {
    private static final Logger LOGGER = Logger.getLogger(TC211Parser.class.getName());

    /**
     *
     * @param logger
     * @param urlCache
     * @param dataSource Data source associated to that URL, for caching purpose
     * @param url Url of the document to parse
     * @param forceDownload
     * @return
     * @throws RevivableThreadInterruptedException
     */
    public TC211Document parseURL(
            ThreadLogger logger,
            URLCache urlCache,
            AbstractDataSourceConfig dataSource,
            String layerId,
            URL url,
            boolean forceDownload
    ) throws RevivableThreadInterruptedException {

        RevivableThread.checkForInterruption();

        String urlStr = url.toString();
        TC211Document tc211Document = this.parseRawURL(logger, urlCache, dataSource, layerId, url, false, forceDownload);

        RevivableThread.checkForInterruption();

        // Still no metadata document found
        // Try different URLs
        if (tc211Document == null) {
            try {
                GeoNetworkUrlBuilder geoNetworkUrlBuilder = new GeoNetworkUrlBuilder(url);
                RevivableThread.checkForInterruption();

                if (geoNetworkUrlBuilder.isValidGeoNetworkUrl()) {
                    // Try GeoNetwork 2.10 URL
                    URL geoNetwork2_10Url = geoNetworkUrlBuilder.craftGeoNetwork2_10MestUrl();
                    try {
                        if (geoNetwork2_10Url != null) {
                            String geoNetwork2_10UrlStr = geoNetwork2_10Url.toString();
                            if (!geoNetwork2_10UrlStr.equals(urlStr)) {
                                tc211Document = this.parseRawURL(logger, urlCache, dataSource, layerId, geoNetwork2_10Url, true, forceDownload);
                            }
                        }
                    } catch(Exception ex) {
                        LOGGER.log(Level.WARNING, "Error occurred while parsing the GeoNetwork 2.10 MEST record URL: " + geoNetwork2_10Url);
                    }
                    RevivableThread.checkForInterruption();

                    // Still not good, try old Legacy GeoNetwork URL
                    if (tc211Document == null) {
                        URL geoNetworkLegacyUrl = geoNetworkUrlBuilder.craftGeoNetworkLegacyMestUrl();
                        try {
                            if (geoNetworkLegacyUrl != null) {
                                String geoNetworkLegacyUrlStr = geoNetworkLegacyUrl.toString();
                                if (!geoNetworkLegacyUrlStr.equals(urlStr)) {
                                    tc211Document = this.parseRawURL(logger, urlCache, dataSource, layerId, geoNetworkLegacyUrl, true, forceDownload);
                                }
                            }
                        } catch(Exception ex) {
                            LOGGER.log(Level.WARNING, "Error occurred while parsing the Legacy GeoNetwork MEST record URL: " + geoNetworkLegacyUrl);
                        }
                    }
                    RevivableThread.checkForInterruption();
                }
            } catch (Exception ex) {
                // This should not happen
                logger.log(Level.WARNING, String.format("Unexpected error occurred while crafting a GeoNetwork URL: %s",
                        Utils.getExceptionMessage(ex)), ex);
            }
        }

        return tc211Document;
    }

    private TC211Document parseRawURL(
            ThreadLogger logger,
            URLCache urlCache,
            AbstractDataSourceConfig dataSource,
            String layerId,
            URL url,
            boolean craftedUrl,
            boolean forceDownload
    ) throws RevivableThreadInterruptedException {

        RevivableThread.checkForInterruption();

        String urlStr = url.toString();
        String dataSourceId = null;
        if (dataSource != null) {
            dataSourceId = dataSource.getDataSourceId();
        }

        TC211Document tc211Document = null;

        // Download MEST record (or get from cache)
        CacheEntry mestCacheEntry = null;
        CacheEntry rollbackMestCacheEntry = null;
        try {
            try {
                Boolean reDownload = null;
                if (forceDownload) {
                    reDownload = true;
                }

                if (forceDownload || urlCache.isDownloadRequired(url)) {
                    if (!craftedUrl) {
                        logger.log(Level.INFO, String.format("Downloading [TC211 MEST record](%s) for layer %s",
                                urlStr, layerId));
                    }
                }

                mestCacheEntry = urlCache.getCacheEntry(url);
                if (mestCacheEntry != null) {
                    urlCache.getHttpDocument(mestCacheEntry, dataSourceId, reDownload);
                    File mestFile = mestCacheEntry.getDocumentFile();
                    if (mestFile != null) {
                        tc211Document = this.parseFile(mestFile, urlStr);
                        if (tc211Document != null) {
                            if (craftedUrl) {
                                logger.log(Level.WARNING, String.format("Using crafted URL for the [TC211 MEST record](%s) for layer %s. " +
                                        "You should modify the layer information to use this URL instead: %s",
                                        urlStr, layerId, urlStr));
                            }
                            urlCache.save(mestCacheEntry, true);
                        }
                    }
                }
            } catch (Exception ex) {
                if (!craftedUrl) {
                    // The MEST record was not good. Use the previous version if possible
                    logger.log(Level.WARNING, String.format("Error occurred while parsing the [TC211 MEST record](%s) for layer %s: %s",
                            urlStr, layerId, Utils.getExceptionMessage(ex)), ex);
                }
            }

            // Could not get a working JSON record
            // Rollback to previous version
            if (tc211Document == null) {
                try {
                    rollbackMestCacheEntry = urlCache.getCacheEntry(url);
                    if (rollbackMestCacheEntry != null) {
                        urlCache.getHttpDocument(rollbackMestCacheEntry, dataSourceId, false);
                        File mestFile = rollbackMestCacheEntry.getDocumentFile();
                        if (mestFile != null) {
                            tc211Document = this.parseFile(mestFile, urlStr);
                            if (tc211Document != null) {
                                if (!craftedUrl) {
                                    logger.log(Level.WARNING, String.format("Invalid [TC211 MEST record](%s) response used by layer %s. Using backup.",
                                            urlStr, layerId));
                                }
                                urlCache.save(rollbackMestCacheEntry, true);
                            }
                        }
                    }
                } catch (Exception ex) {
                    // The backup was also not good. The user do not need to know about that...
                    LOGGER.log(Level.FINEST, String.format("Error occurred while parsing the backup TC211 MEST record %s for layer %s: %s",
                            urlStr, layerId, Utils.getExceptionMessage(ex)), ex);
                }
            }

            // Even the rollback didn't work
            if (tc211Document == null) {
                // Save what we have in DB
                try {
                    urlCache.save(mestCacheEntry, false);
                } catch (Exception ex) {
                    logger.log(Level.WARNING, String.format("Error occurred while saving the entry into the " +
                            "cache database [TC211 MEST record](%s) for layer %s: %s",
                            urlStr, layerId, Utils.getExceptionMessage(ex)), ex);
                }
            }

        } finally {
            if (mestCacheEntry != null) mestCacheEntry.close();
            if (rollbackMestCacheEntry != null) rollbackMestCacheEntry.close();
        }

        return tc211Document;
    }

    private static SAXParser getSAXParser() throws SAXException, ParserConfigurationException {
        SAXParserFactory factory = SAXParserFactory.newInstance();

        // Disabling DTD loading & validation
        // Without those 2 lines, initialising XML files takes ages (about 10 minutes for 500kb, down to a few ms with those lines)
        factory.setFeature("http://apache.org/xml/features/validation/schema", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        return factory.newSAXParser();
    }

    /**
     * NOT Cached
     * @param file
     * @param location
     * @return
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws JSONException
     */
    private TC211Document parseFile(File file, String location)
            throws Exception, RevivableThreadInterruptedException {

        RevivableThread.checkForInterruption();

        if (file == null || !file.exists()) {
            return null;
        }

        TC211Document doc;
        try {
            SAXParser saxParser = getSAXParser();

            doc = new TC211Document(location);
            TC211Handler handler = new TC211Handler(doc);

            saxParser.parse(file, handler);
        } catch (Exception ex) {
            // Could not parse the document.
            // Check if GeoNetwork returned an access denied (we can't rely on response HTTP code, GeoNetwork does not follow standards)
            if (Utils.findInFile("Operation not allowed", file)) {
                throw new SAXException(String.format("Unauthorised access to %s. Check if the document is published.", location));

            } else {
                throw ex;
            }
        }
        RevivableThread.checkForInterruption();

        return doc.isEmpty() ? null : doc;
    }

    /**
     * NOT Cached (Used for tests)
     * @param inputStream
     * @param location
     * @return
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws JSONException
     */
    public static TC211Document parseInputStream(InputStream inputStream, String location)
            throws SAXException, ParserConfigurationException, IOException {

        if (inputStream == null) {
            throw new IllegalArgumentException("Can not parse null XML stream. " + location);
        }

        SAXParser saxParser = getSAXParser();

        TC211Document doc = new TC211Document(location);
        TC211Handler handler = new TC211Handler(doc);

        saxParser.parse(inputStream, handler);

        return doc;
    }

    private static int seq = 0; // tmp layer id sequence
    public static LayerWrapper createLayer(ConfigManager configManager, TC211Document document, TC211Document.Link link) throws JSONException {
        TC211Document.Protocol protocol = link.getProtocol();
        if (protocol == null) { return null; }

        String serviceUrl = link.getUrl();
        String linkName = link.getName();

        // Create a custom WMS layer, with all the info available in the metadata document
        LayerWrapper layer = new LayerWrapper();
        if (protocol.isOGC()) {
            layer.setLayerType("WMS");
            layer.setServiceUrl(serviceUrl);
            if (Utils.isBlank(linkName)) {
                return null;
            }

        } else if (protocol.isKML()) {
            layer.setLayerType("KML");
            layer.setKmlUrl(serviceUrl);
            if (Utils.isBlank(linkName)) {
                linkName = "KML";
            }

        } else {
            return null;
        }

        seq++;
        layer.setLayerId("TMP_" + seq + "_" + linkName);
        layer.setLayerName(linkName);

        // *** MEST Description ***
        String layerDescription = link.getDescription();
        if (Utils.isNotBlank(layerDescription)) {
            layer.setDescription(layerDescription);
            layer.setDescriptionFormat("wiki");
        }

        // The layer title is replace with the title from the MEST link description.
        String titleStr = linkName;
        if (Utils.isNotBlank(titleStr)) {
            layer.setTitle(titleStr);
        }

        return layer;
    }
}
