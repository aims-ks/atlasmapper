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
package au.gov.aims.atlasmapperserver.xml.WMTS;

import au.gov.aims.atlasmapperserver.Utils;
import au.gov.aims.atlasmapperserver.cache.CacheEntry;
import au.gov.aims.atlasmapperserver.cache.URLCache;
import au.gov.aims.atlasmapperserver.dataSourceConfig.AbstractDataSourceConfig;
import au.gov.aims.atlasmapperserver.thread.RevivableThread;
import au.gov.aims.atlasmapperserver.thread.RevivableThreadInterruptedException;
import au.gov.aims.atlasmapperserver.thread.ThreadLogger;
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

public class WMTSParser {
    private static final Logger LOGGER = Logger.getLogger(WMTSParser.class.getName());

    /**
     *
     * @param logger
     * @param urlCache
     * @param dataSource Data source associated to that URL, for caching purpose
     * @param url Url of the document to parse
     * @param forceDownload True to redownload required files. False to use the file from the cache, if available.
     * @return
     * @throws RevivableThreadInterruptedException
     */
    public static WMTSDocument parseURL(
            ThreadLogger logger,
            URLCache urlCache,
            AbstractDataSourceConfig dataSource,
            URL url,
            boolean forceDownload
    ) throws RevivableThreadInterruptedException {

        RevivableThread.checkForInterruption();

        String urlStr = url.toString();

        WMTSDocument wmtsDocument = null;

        // Download GetCapabilities document (or get from cache)
        CacheEntry capabilitiesCacheEntry = null;
        CacheEntry rollbackCacheEntry = null;
        try {
            try {
                Boolean redownload = null;
                if (forceDownload) {
                    redownload = true;
                }

                capabilitiesCacheEntry = urlCache.getHttpDocument(url, dataSource.getDataSourceId(), redownload);
                if (capabilitiesCacheEntry != null) {
                    File wmtsDocumentFile = capabilitiesCacheEntry.getDocumentFile();
                    if (wmtsDocumentFile != null) {
                        logger.log(Level.INFO, "Parsing WMTS GetCapabilities document");
                        wmtsDocument = parseFile(wmtsDocumentFile, urlStr);
                        if (wmtsDocument != null) {
                            urlCache.save(capabilitiesCacheEntry, true);
                        }
                    }
                }
            } catch (Exception ex) {
                // The GetCapibilities document was not good. Use the previous version if possible
                logger.log(Level.WARNING, String.format("Error occurred while parsing the [WMTS GetCapabilities document](%s): %s",
                        urlStr, Utils.getExceptionMessage(ex)), ex);
            }

            // Could not get a working GetCapabilities document
            // Rollback to previous version
            if (wmtsDocument == null) {
                try {
                    rollbackCacheEntry = urlCache.getHttpDocument(url, dataSource.getDataSourceId(), false);
                    if (rollbackCacheEntry != null) {
                        Boolean valid = rollbackCacheEntry.getValid();
                        if (valid != null && valid) {
                            File rollbackFile = rollbackCacheEntry.getDocumentFile();
                            if (rollbackFile != null) {
                                wmtsDocument = parseFile(rollbackFile, urlStr);
                                if (wmtsDocument != null) {
                                    // Save last access timestamp, usage, etc
                                    urlCache.save(rollbackCacheEntry, true);
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                    // This should not happen
                    logger.log(Level.WARNING, String.format("Error occurred while getting the previous [WMTS GetCapabilities document](%s): %s",
                            urlStr, Utils.getExceptionMessage(ex)), ex);
                }
            }

            // Even the rollback didn't work
            if (wmtsDocument == null) {
                // Save what we have in DB
                try {
                    urlCache.save(capabilitiesCacheEntry, false);
                } catch (Exception ex) {
                    logger.log(Level.WARNING, String.format("Error occurred while saving the entry into the cache database [WMTS GetCapabilities document](%s): %s",
                            urlStr, Utils.getExceptionMessage(ex)), ex);
                }
            }

        } finally {
            if (capabilitiesCacheEntry != null) capabilitiesCacheEntry.close();
            if (rollbackCacheEntry != null) rollbackCacheEntry.close();
        }

        RevivableThread.checkForInterruption();

        return wmtsDocument;
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
     * @param location For debugging purpose
     * @return
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws RevivableThreadInterruptedException
     */
    public static WMTSDocument parseFile(File file, String location)
            throws SAXException, ParserConfigurationException, IOException, RevivableThreadInterruptedException {

        RevivableThread.checkForInterruption();

        if (file == null || !file.exists()) {
            return null;
        }

        SAXParser saxParser = getSAXParser();

        WMTSDocument doc = new WMTSDocument(location);
        WMTSHandler handler = new WMTSHandler(doc);

        saxParser.parse(file, handler);
        RevivableThread.checkForInterruption();

        if (doc.getLayer() == null) {
            return null;
        }

        return doc;
    }

    /**
     * NOT Cached (Used for tests)
     * @param inputStream
     * @param location For debugging purpose
     * @return
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws IOException
     */
    public static WMTSDocument parseInputStream(InputStream inputStream, String location)
            throws SAXException, ParserConfigurationException, IOException {

        if (inputStream == null) {
            throw new IllegalArgumentException("Can not parse null XML stream. " + location);
        }

        SAXParser saxParser = getSAXParser();

        WMTSDocument doc = new WMTSDocument(location);
        WMTSHandler handler = new WMTSHandler(doc);

        saxParser.parse(inputStream, handler);

        if (doc.getLayer() == null) {
            return null;
        }

        return doc;
    }
}
