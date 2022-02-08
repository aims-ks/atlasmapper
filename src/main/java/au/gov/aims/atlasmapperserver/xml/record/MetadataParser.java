/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2022 Australian Institute of Marine Science
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
package au.gov.aims.atlasmapperserver.xml.record;

import au.gov.aims.atlasmapperserver.Utils;
import au.gov.aims.atlasmapperserver.cache.CacheEntry;
import au.gov.aims.atlasmapperserver.cache.URLCache;
import au.gov.aims.atlasmapperserver.dataSourceConfig.AbstractDataSourceConfig;
import au.gov.aims.atlasmapperserver.jsonWrappers.client.LayerWrapper;
import au.gov.aims.atlasmapperserver.thread.RevivableThread;
import au.gov.aims.atlasmapperserver.thread.RevivableThreadInterruptedException;
import au.gov.aims.atlasmapperserver.thread.ThreadLogger;
import au.gov.aims.atlasmapperserver.xml.record.iso19115_3_2018.Iso19115_3_2018Parser;
import au.gov.aims.atlasmapperserver.xml.record.iso19139.Iso19139Parser;
import org.json.JSONException;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;

public class MetadataParser {
    private static int seq = 0; // tmp layer id sequence

    private static MetadataParser instance;

    // Prevent instantiation
    private MetadataParser() {}

    public static MetadataParser getInstance() {
        if (MetadataParser.instance == null) {
            MetadataParser.instance = new MetadataParser();
        }
        return MetadataParser.instance;
    }

    public MetadataDocument parseURL(
        ThreadLogger logger,
        URLCache urlCache,
        AbstractDataSourceConfig dataSource,
        String layerId,
        URL url,
        boolean forceDownload
    ) throws Exception, RevivableThreadInterruptedException {
        MetadataDocument metadataDocument = null;
        CacheEntry mestCacheEntry = null;

        try {
            // 1. Get the file represented by the URL.
            mestCacheEntry = this.getCacheEntry(logger, urlCache, layerId, url, true);
            RevivableThread.checkForInterruption();

            // 2. Determine what type of record (schema) it is
            File metadataRecordFile = getMetadataRecordFile(logger, urlCache, dataSource, layerId, mestCacheEntry, forceDownload);
            MetadataSchema schema = this.getMetadataSchema(metadataRecordFile);
            if (MetadataSchema.UNPUBLISHED.equals(schema)) {
                logger.log(Level.WARNING, String.format("Unauthorised access to %s. Check if the document is published.", url.toString()));
            }
            RevivableThread.checkForInterruption();

            // 3. If the schema could not be determined, try to "fix" the URL.
            if (schema == null) {
                try {
                    GeoNetwork2UrlBuilder geoNetwork2UrlBuilder = new GeoNetwork2UrlBuilder(url);
                    if (geoNetwork2UrlBuilder.isValidGeoNetworkUrl()) {
                        // Try GeoNetwork 2.10 URL
                        mestCacheEntry.close();
                        URL geoNetwork2_10Url = geoNetwork2UrlBuilder.craftGeoNetwork2_10MestUrl();
                        mestCacheEntry = this.getCacheEntry(logger, urlCache, layerId, geoNetwork2_10Url, false);
                        metadataRecordFile = getMetadataRecordFile(logger, urlCache, dataSource, layerId, mestCacheEntry, forceDownload);
                        schema = this.getMetadataSchema(metadataRecordFile);
                        if (MetadataSchema.UNPUBLISHED.equals(schema)) {
                            logger.log(Level.WARNING, String.format("Unauthorised access to %s. Check if the document is published.", geoNetwork2_10Url.toString()));
                        }

                        RevivableThread.checkForInterruption();

                        if (schema == null) {
                            mestCacheEntry.close();
                            URL geoNetworkLegacyUrl = geoNetwork2UrlBuilder.craftGeoNetworkLegacyMestUrl();
                            mestCacheEntry = this.getCacheEntry(logger, urlCache, layerId, geoNetworkLegacyUrl, false);
                            metadataRecordFile = getMetadataRecordFile(logger, urlCache, dataSource, layerId, mestCacheEntry, forceDownload);
                            schema = this.getMetadataSchema(metadataRecordFile);
                            if (MetadataSchema.UNPUBLISHED.equals(schema)) {
                                logger.log(Level.WARNING, String.format("Unauthorised access to %s. Check if the document is published.", geoNetworkLegacyUrl.toString()));
                            }

                            RevivableThread.checkForInterruption();
                        }
                    }
                } catch (Exception ex) {
                    // This should not happen
                    logger.log(Level.WARNING, String.format("Unexpected error occurred while crafting a GeoNetwork URL: %s",
                            Utils.getExceptionMessage(ex)), ex);
                }
            }

            // 4. Parse the record
            if (metadataRecordFile != null) {
                URL recordUrl = mestCacheEntry.getUrl();
                String urlStr = recordUrl == null ? metadataRecordFile.getAbsolutePath() : recordUrl.toString();
                metadataDocument = this.parseFile(logger, metadataRecordFile, urlStr, schema);
                RevivableThread.checkForInterruption();
            }

            // 5. Register that the file is valid
            if (metadataDocument != null) {
                urlCache.save(mestCacheEntry, true);
            } else {
                // Try to get it from cache
                metadataDocument = this.parseCachedURL(logger, urlCache, dataSource, layerId, url);
            }
        } finally {
            if (mestCacheEntry != null) {
                mestCacheEntry.close();
            }
        }

        return metadataDocument;
    }

    public MetadataDocument parseCachedURL(
        ThreadLogger logger,
        URLCache urlCache,
        AbstractDataSourceConfig dataSource,
        String layerId,
        URL url
    ) throws Exception, RevivableThreadInterruptedException {
        File metadataRecordFile = null;
        CacheEntry mestCacheEntry = null;
        MetadataDocument metadataDocument = null;

        try {
            // 1. Get the file represented by the URL.
            mestCacheEntry = this.getCacheEntry(logger, urlCache, layerId, url, false);
            RevivableThread.checkForInterruption();

            // 2. Determine if it's valid
            if (mestCacheEntry.getValid()) {
                metadataRecordFile = mestCacheEntry.getDocumentFile();
            }

            // 3. If the schema could not be determined, try to "fix" the URL.
            if (metadataRecordFile == null) {
                try {
                    GeoNetwork2UrlBuilder geoNetwork2UrlBuilder = new GeoNetwork2UrlBuilder(url);
                    if (geoNetwork2UrlBuilder.isValidGeoNetworkUrl()) {
                        // Try GeoNetwork 2.10 URL
                        mestCacheEntry.close();
                        URL geoNetwork2_10Url = geoNetwork2UrlBuilder.craftGeoNetwork2_10MestUrl();
                        mestCacheEntry = this.getCacheEntry(logger, urlCache, layerId, geoNetwork2_10Url, false);
                        if (mestCacheEntry.getValid()) {
                            metadataRecordFile = mestCacheEntry.getDocumentFile();
                        }

                        if (metadataRecordFile == null) {
                            mestCacheEntry.close();
                            URL geoNetworkLegacyUrl = geoNetwork2UrlBuilder.craftGeoNetworkLegacyMestUrl();
                            mestCacheEntry = this.getCacheEntry(logger, urlCache, layerId, geoNetworkLegacyUrl, false);
                            if (mestCacheEntry.getValid()) {
                                metadataRecordFile = mestCacheEntry.getDocumentFile();
                            }
                            RevivableThread.checkForInterruption();
                        }
                    }
                } catch (Exception ex) {
                    // This should not happen
                    logger.log(Level.WARNING, String.format("Unexpected error occurred while crafting a GeoNetwork URL: %s",
                            Utils.getExceptionMessage(ex)), ex);
                }
            }

            // 4. Parse the record
            if (metadataRecordFile != null) {
                URL recordUrl = mestCacheEntry.getUrl();
                String urlStr = recordUrl == null ? metadataRecordFile.getAbsolutePath() : recordUrl.toString();
                metadataDocument = this.parseFile(logger, metadataRecordFile, urlStr);
                RevivableThread.checkForInterruption();
            }

            // 5. Register that the file is valid
            if (metadataDocument != null) {
                urlCache.save(mestCacheEntry, true);
            } else {
                urlCache.save(mestCacheEntry, false);
            }
        } finally {
            if (mestCacheEntry != null) {
                mestCacheEntry.close();
            }
        }

        return metadataDocument;
    }

    public MetadataDocument parseFile(
        ThreadLogger logger,
        File metadataRecordFile,
        String location
    ) throws Exception {
        MetadataSchema schema = this.getMetadataSchema(metadataRecordFile);
        if (MetadataSchema.UNPUBLISHED.equals(schema)) {
            logger.log(Level.WARNING, String.format("Unauthorised access to %s. Check if the document is published.", location));
        }

        return this.parseFile(logger, metadataRecordFile, location, schema);
    }

    public MetadataDocument parseFile(
        ThreadLogger logger,
        File metadataRecordFile,
        String location,
        MetadataSchema schema
    ) throws Exception {
        if (schema == null) {
            return null;
        }

        MetadataDocument metadataDocument = null;
        AbstractMetadataParser parser = null;
        switch(schema) {
            case DUBLIN_CORE:
                parser = null; // TODO
                break;

            case ISO19139:
            case ISO19139_MCP:
                parser = new Iso19139Parser(logger);
                break;

            case ISO19115_3_2018:
                parser = new Iso19115_3_2018Parser(logger);
                break;
        }

        if (parser != null) {
            metadataDocument =
                parser.parseFile(metadataRecordFile, location);
        }

        return metadataDocument;
    }

    private File getMetadataRecordFile(
        ThreadLogger logger,
        URLCache urlCache,
        AbstractDataSourceConfig dataSource,
        String layerId,
        CacheEntry mestCacheEntry,
        boolean forceDownload
    ) throws RevivableThreadInterruptedException {
        if (mestCacheEntry == null) {
            return null;
        }

        URL cachedUrl = mestCacheEntry.getUrl();
        String urlStr = cachedUrl == null ? null : cachedUrl.toString();

        Boolean reDownload = null;
        if (forceDownload) {
            reDownload = true;
        }

        boolean downloadRequired = forceDownload || mestCacheEntry.isExpired();
        if (downloadRequired) {
            logger.log(Level.INFO, String.format("Downloading [TC211 MEST record](%s) for layer %s",
                    urlStr, layerId));
        }

        // Avoid parsing document that are known to be unparsable
        boolean parsable = true;
        if (!downloadRequired) {
            Boolean valid = mestCacheEntry.getValid();
            if (valid != null && !valid) {
                parsable = false;
            }
        }

        if (parsable) {
            String dataSourceId = null;
            if (dataSource != null) {
                dataSourceId = dataSource.getDataSourceId();
            }

            try {
                urlCache.getHttpDocument(mestCacheEntry, dataSourceId, reDownload);
                return mestCacheEntry.getDocumentFile();
            } catch(Exception ex) {
                logger.log(Level.WARNING, String.format("Error occurred while downloading the [TC211 MEST record](%s) for layer %s: %s",
                        urlStr, layerId, Utils.getExceptionMessage(ex)), ex);
            }
        }

        return null;
    }

    protected CacheEntry getCacheEntry(
        ThreadLogger logger,
        URLCache urlCache,
        String layerId,
        URL url,
        boolean logError
    ) {
        CacheEntry cacheEntry = null;

        try {
            cacheEntry = urlCache.getCacheEntry(url);
        } catch (Exception ex) {
            // The MEST record was not good. Use the previous version if possible
            if (logError) {
                logger.log(Level.WARNING, String.format("Error occurred while parsing the [TC211 MEST record](%s) for layer %s: %s",
                        url.toString(), layerId, Utils.getExceptionMessage(ex)), ex);
            }
        }

        return cacheEntry;
    }

    protected MetadataSchema getMetadataSchema(File metadataRecordFile) {

        if (metadataRecordFile == null || !metadataRecordFile.canRead()) {
            return null;
        }

        MetadataSchema schema = null;
        try(InputStream inputStream = new FileInputStream(metadataRecordFile)) {
            schema = getMetadataSchema(inputStream);
        } catch(Exception ex) {
            // Can't read the file
        }

        return schema;
    }

    protected MetadataSchema getMetadataSchema(InputStream inputStream) {
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        XMLStreamReader streamReader = null;

        MetadataSchema schema = null;
        try {
            // Parse the header of the XML document
            streamReader = inputFactory.createXMLStreamReader(inputStream);
            streamReader.nextTag(); // Advance to the root element

            //String rootElement = streamReader.getLocalName();
            QName rootName = streamReader.getName();
            schema = MetadataSchema.find(rootName.getPrefix(), rootName.getLocalPart());
        } catch(Exception ex) {
            // Not a XML document
        } finally {
            if (streamReader != null) {
                try {
                    streamReader.close();
                } catch(Exception ex) {
                    // Silently close the stream
                }
            }
        }

        if (schema == null) {
            if (Utils.findInInputStream("Operation not allowed", inputStream)) {
                schema = MetadataSchema.UNPUBLISHED;
            }
        }

        return schema;
    }

    public static LayerWrapper createLayer(MetadataDocument.Link link) throws JSONException {
        MetadataDocument.Protocol protocol = link.getProtocol();
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

    public enum MetadataSchema {
        DUBLIN_CORE(null, "simpledc"),
        ISO19139("gmd", "MD_Metadata"),        // GeoNetwork 2.x +
        ISO19139_MCP("mcp", "MD_Metadata"),    // GeoNetwork 2.x +
        ISO19115_3_2018("mdb", "MD_Metadata"), // GeoNetwork 3.x +
        UNPUBLISHED(null, null);

        public final String prefix;
        public final String localPart;

        private MetadataSchema(String prefix, String localPart) {
            this.prefix = prefix;
            this.localPart = localPart;
        }

        public static MetadataSchema find(String prefix, String localPart) {
            for (MetadataSchema schema : MetadataSchema.values()) {
                if (stringEquals(schema.prefix, prefix) && stringEquals(schema.localPart, localPart)) {
                    return schema;
                }
            }
            return null;
        }

        private static boolean stringEquals(String str1, String str2) {
            // Same instance or both null
            if (str1 == str2) {
                return true;
            }
            // Only one is null
            if (str1 == null || str2 == null) {
                return false;
            }

            return str1.equals(str2);
        }
    }
}

