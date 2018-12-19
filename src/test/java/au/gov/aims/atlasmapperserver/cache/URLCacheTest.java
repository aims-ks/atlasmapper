/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2018 Australian Institute of Marine Science
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

package au.gov.aims.atlasmapperserver.cache;

import au.gov.aims.atlasmapperserver.thread.RevivableThreadInterruptedException;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class URLCacheTest {
    private static final Logger LOGGER = Logger.getLogger(URLCacheTest.class.getName());

    @Test
    public void testGetHttpHead() throws Exception, RevivableThreadInterruptedException {
        URLCache urlCache = new URLCache(null);
        CacheDatabase cacheDatabase = urlCache.getCacheDatabase();

        URL eatlasUrl = new URL("https://eatlas.org.au");
        String entityId = "ea";
        // Ensure there is no entry for it in the database
        try {
            cacheDatabase.openConnection();
            cacheDatabase.delete(eatlasUrl);
        } finally {
            cacheDatabase.close();
        }

        CacheEntry cacheEntry = null;
        try {
            // Get Head
            long timeBeforeRequest = CacheEntry.getCurrentTimestamp();
            cacheEntry = urlCache.getHttpHead(eatlasUrl, entityId);

            Assert.assertNotNull(String.format("Could not get HTTP head for %s", eatlasUrl), cacheEntry);

            // Ensure it's not in the data base yet (haven't been saved)
            try {
                cacheDatabase.openConnection();
                Assert.assertFalse(String.format("The HTTP head request for %s was saved in the cache database before the save method was called.", eatlasUrl),
                        cacheDatabase.exists(eatlasUrl));
            } finally {
                cacheDatabase.close();
            }

            // Save it in the database and check that it's really there
            urlCache.save(cacheEntry, true);
            long timeAfterRequest = CacheEntry.getCurrentTimestamp();
            try {
                cacheDatabase.openConnection();
                Assert.assertTrue(String.format("The HTTP head request for %s was not saved in the cache database.", eatlasUrl),
                        cacheDatabase.exists(eatlasUrl));
            } finally {
                cacheDatabase.close();
            }

            LOGGER.log(Level.INFO, cacheEntry.toString());

            Assert.assertEquals(eatlasUrl, cacheEntry.getUrl());
            Assert.assertEquals(RequestMethod.HEAD, cacheEntry.getRequestMethod());
            Assert.assertEquals(new Integer(200), cacheEntry.getHttpStatusCode());

            Long requestTimestamp = cacheEntry.getRequestTimestamp();
            Assert.assertNotNull("RequestTimestamp is null", requestTimestamp);
            Assert.assertTrue("Invalid RequestTimestamp: " + requestTimestamp,
                    requestTimestamp >= timeBeforeRequest && requestTimestamp <= timeAfterRequest);

            Long lastAccessTimestamp = cacheEntry.getLastAccessTimestamp();
            Assert.assertNotNull("LastAccessTimestamp is null", lastAccessTimestamp);
            Assert.assertTrue("Invalid LastAccessTimestamp: " + lastAccessTimestamp,
                    lastAccessTimestamp >= timeBeforeRequest && lastAccessTimestamp <= timeAfterRequest);
        } finally {
            if (cacheEntry != null) cacheEntry.close();
        }
    }

    @Test
    public void getHttpDocument() throws Exception, RevivableThreadInterruptedException {
        URLCache urlCache = new URLCache(null);
        CacheDatabase cacheDatabase = urlCache.getCacheDatabase();

        URL eatlasUrl = new URL("https://eatlas.org.au");
        String entityId = "ea";
        // Ensure there is no entry for it in the database
        try {
            cacheDatabase.openConnection();
            cacheDatabase.delete(eatlasUrl);
        } finally {
            cacheDatabase.close();
        }

        Long downloadTimestamp;
        CacheEntry cacheEntry = null;
        try {
            // Download
            long timeBeforeRequest = CacheEntry.getCurrentTimestamp();
            cacheEntry = urlCache.getHttpDocument(eatlasUrl, entityId);
            LOGGER.log(Level.INFO, "Download: " + cacheEntry.toString());


            Assert.assertNotNull(String.format("Could not get HTTP document for %s", eatlasUrl), cacheEntry);

            try {
                cacheDatabase.openConnection();
                Assert.assertFalse(String.format("The HTTP document request for %s was saved in the cache database before the save method was called.", eatlasUrl),
                        cacheDatabase.exists(eatlasUrl));
            } finally {
                cacheDatabase.close();
            }

            urlCache.save(cacheEntry, true);
            long timeAfterRequest = CacheEntry.getCurrentTimestamp();
            try {
                cacheDatabase.openConnection();
                Assert.assertTrue(String.format("The HTTP document request for %s was not saved in the cache database.", eatlasUrl),
                        cacheDatabase.exists(eatlasUrl));
            } finally {
                cacheDatabase.close();
            }

            Assert.assertEquals(eatlasUrl, cacheEntry.getUrl());
            Assert.assertEquals(RequestMethod.GET, cacheEntry.getRequestMethod());
            Assert.assertEquals(new Integer(200), cacheEntry.getHttpStatusCode());

            downloadTimestamp = cacheEntry.getRequestTimestamp();
            Assert.assertNotNull("RequestTimestamp is null", downloadTimestamp);
            Assert.assertTrue("Invalid RequestTimestamp: " + downloadTimestamp,
                    downloadTimestamp >= timeBeforeRequest && downloadTimestamp <= timeAfterRequest);

            Long lastAccessTimestamp = cacheEntry.getLastAccessTimestamp();
            Assert.assertNotNull("LastAccessTimestamp is null", lastAccessTimestamp);
            Assert.assertTrue("Invalid LastAccessTimestamp: " + lastAccessTimestamp,
                    lastAccessTimestamp >= timeBeforeRequest && lastAccessTimestamp <= timeAfterRequest);

            // Check document
            this.checkEatlasDownloadedDocument(cacheEntry.getDocumentFile(), eatlasUrl);

        } finally {
            // Clean-up
            if (cacheEntry != null ) {
                cacheEntry.close();
                cacheEntry = null;
            }
        }


        try {
            // Get the document, from cache
            long timeBeforeRequest = CacheEntry.getCurrentTimestamp();
            cacheEntry = urlCache.getHttpDocument(eatlasUrl, entityId);
            urlCache.save(cacheEntry, true);
            LOGGER.log(Level.INFO, "Get: " + cacheEntry.toString());
            long timeAfterRequest = CacheEntry.getCurrentTimestamp();

            Assert.assertEquals(eatlasUrl, cacheEntry.getUrl());
            Assert.assertEquals(RequestMethod.GET, cacheEntry.getRequestMethod());
            Assert.assertEquals(new Integer(200), cacheEntry.getHttpStatusCode());

            Long requestTimestamp = cacheEntry.getRequestTimestamp();
            Assert.assertNotNull("RequestTimestamp is null", requestTimestamp);
            Assert.assertEquals("Unexpected RequestTimestamp: " + requestTimestamp,
                    downloadTimestamp, requestTimestamp);

            Long lastAccessTimestamp = cacheEntry.getLastAccessTimestamp();
            Assert.assertNotNull("LastAccessTimestamp is null", lastAccessTimestamp);
            Assert.assertTrue("Invalid LastAccessTimestamp: " + lastAccessTimestamp,
                    lastAccessTimestamp >= timeBeforeRequest && lastAccessTimestamp <= timeAfterRequest);

            // Check document
            this.checkEatlasDownloadedDocument(cacheEntry.getDocumentFile(), eatlasUrl);

        } finally {
            // Clean-up
            if (cacheEntry != null ) {
                cacheEntry.close();
                cacheEntry = null;
            }
        }


        try {
            // Re-download the document
            long timeBeforeRequest = CacheEntry.getCurrentTimestamp();
            cacheEntry = urlCache.getHttpDocument(eatlasUrl, entityId, true);
            urlCache.save(cacheEntry, true);
            LOGGER.log(Level.INFO, "Re-download: " + cacheEntry.toString());
            long timeAfterRequest = CacheEntry.getCurrentTimestamp();

            Assert.assertEquals(eatlasUrl, cacheEntry.getUrl());
            Assert.assertEquals(RequestMethod.GET, cacheEntry.getRequestMethod());
            Assert.assertEquals(new Integer(200), cacheEntry.getHttpStatusCode());

            Long requestTimestamp = cacheEntry.getRequestTimestamp();
            Assert.assertNotNull("RequestTimestamp is null", requestTimestamp);
            Assert.assertTrue("Invalid RequestTimestamp: " + requestTimestamp,
                    requestTimestamp >= timeBeforeRequest && requestTimestamp <= timeAfterRequest);

            Long lastAccessTimestamp = cacheEntry.getLastAccessTimestamp();
            Assert.assertNotNull("LastAccessTimestamp is null", lastAccessTimestamp);
            Assert.assertTrue("Invalid LastAccessTimestamp: " + lastAccessTimestamp,
                    lastAccessTimestamp >= timeBeforeRequest && lastAccessTimestamp <= timeAfterRequest);

            // Check document
            this.checkEatlasDownloadedDocument(cacheEntry.getDocumentFile(), eatlasUrl);

        } finally {
            // Clean-up
            if (cacheEntry != null ) {
                cacheEntry.close();
            }
        }
    }


    @Test
    public void test_getHttpHead_getHttpDocument_getHttpHead() throws Exception, RevivableThreadInterruptedException {
        // Test that HEAD has no document
        // Test document after requesting GET
        // Test that request is still GET after requesting HEAD and document file is still there

        URLCache urlCache = new URLCache(null);
        CacheDatabase cacheDatabase = urlCache.getCacheDatabase();

        URL eatlasUrl = new URL("https://eatlas.org.au");
        String entityId = "ea";
        // Ensure there is no entry for it in the database
        try {
            cacheDatabase.openConnection();
            cacheDatabase.delete(eatlasUrl);
        } finally {
            cacheDatabase.close();
        }

        CacheEntry cacheEntry = null;
        Long headTimestamp;
        try {
            // Get Head
            long timeBeforeRequest = CacheEntry.getCurrentTimestamp();
            cacheEntry = urlCache.getHttpHead(eatlasUrl, entityId);
            LOGGER.log(Level.INFO, "Head: " + cacheEntry.toString());

            Assert.assertNotNull(String.format("Could not get HTTP head for %s", eatlasUrl), cacheEntry);

            urlCache.save(cacheEntry, true);
            long timeAfterRequest = CacheEntry.getCurrentTimestamp();

            try {
                cacheDatabase.openConnection();
                Assert.assertTrue(String.format("The HTTP head request for %s was not saved in the cache database.", eatlasUrl),
                        cacheDatabase.exists(eatlasUrl));
            } finally {
                cacheDatabase.close();
            }

            Assert.assertEquals(eatlasUrl, cacheEntry.getUrl());
            Assert.assertEquals(RequestMethod.HEAD, cacheEntry.getRequestMethod());
            Assert.assertEquals(new Integer(200), cacheEntry.getHttpStatusCode());

            headTimestamp = cacheEntry.getRequestTimestamp();
            Assert.assertNotNull("RequestTimestamp is null", headTimestamp);
            Assert.assertTrue("Invalid RequestTimestamp: " + headTimestamp,
                    headTimestamp >= timeBeforeRequest && headTimestamp <= timeAfterRequest);

            Long lastAccessTimestamp = cacheEntry.getLastAccessTimestamp();
            Assert.assertNotNull("LastAccessTimestamp is null", lastAccessTimestamp);
            Assert.assertTrue("Invalid LastAccessTimestamp: " + lastAccessTimestamp,
                    lastAccessTimestamp >= timeBeforeRequest && lastAccessTimestamp <= timeAfterRequest);

            // Check document
            Assert.assertNull("HEAD request produced a document file: " + cacheEntry.getDocumentFile(),
                    cacheEntry.getDocumentFile());

        } finally {
            // Clean-up
            if (cacheEntry != null) {
                cacheEntry.close();
                cacheEntry = null;
            }
        }


        Long downloadTimestamp;
        try {
            // Download the document
            long timeBeforeRequest = CacheEntry.getCurrentTimestamp();
            cacheEntry = urlCache.getHttpDocument(eatlasUrl, entityId);
            urlCache.save(cacheEntry, true);
            LOGGER.log(Level.INFO, "Download: " + cacheEntry.toString());
            long timeAfterRequest = CacheEntry.getCurrentTimestamp();

            Assert.assertEquals(eatlasUrl, cacheEntry.getUrl());
            Assert.assertEquals(RequestMethod.GET, cacheEntry.getRequestMethod());
            Assert.assertEquals(new Integer(200), cacheEntry.getHttpStatusCode());

            downloadTimestamp = cacheEntry.getRequestTimestamp();
            Assert.assertNotNull("RequestTimestamp is null", downloadTimestamp);
            Assert.assertTrue("Invalid RequestTimestamp: " + downloadTimestamp,
                    downloadTimestamp >= timeBeforeRequest && downloadTimestamp <= timeAfterRequest);

            Long lastAccessTimestamp = cacheEntry.getLastAccessTimestamp();
            Assert.assertNotNull("LastAccessTimestamp is null", lastAccessTimestamp);
            Assert.assertTrue("Invalid LastAccessTimestamp: " + lastAccessTimestamp,
                    lastAccessTimestamp >= timeBeforeRequest && lastAccessTimestamp <= timeAfterRequest);

            // Check document
            this.checkEatlasDownloadedDocument(cacheEntry.getDocumentFile(), eatlasUrl);

        } finally {
            // Clean-up
            if (cacheEntry != null) {
                cacheEntry.close();
                cacheEntry = null;
            }
        }


        try {
            // Get HEAD, all required info are already cached
            long timeBeforeRequest = CacheEntry.getCurrentTimestamp();
            cacheEntry = urlCache.getHttpHead(eatlasUrl, entityId);
            urlCache.save(cacheEntry, true);
            LOGGER.log(Level.INFO, "Re-head: " + cacheEntry.toString());
            long timeAfterRequest = CacheEntry.getCurrentTimestamp();

            Assert.assertEquals(eatlasUrl, cacheEntry.getUrl());
            Assert.assertEquals(RequestMethod.GET, cacheEntry.getRequestMethod());
            Assert.assertEquals(new Integer(200), cacheEntry.getHttpStatusCode());

            Long requestTimestamp = cacheEntry.getRequestTimestamp();
            Assert.assertNotNull("RequestTimestamp is null", requestTimestamp);
            Assert.assertEquals("Unexpected RequestTimestamp: " + requestTimestamp,
                    downloadTimestamp, requestTimestamp);

            Long lastAccessTimestamp = cacheEntry.getLastAccessTimestamp();
            Assert.assertNotNull("LastAccessTimestamp is null", lastAccessTimestamp);
            Assert.assertTrue("Invalid LastAccessTimestamp: " + lastAccessTimestamp,
                    lastAccessTimestamp >= timeBeforeRequest && lastAccessTimestamp <= timeAfterRequest);

            // Check document
            Assert.assertNull("HEAD request produced a document file: " + cacheEntry.getDocumentFile(),
                    cacheEntry.getDocumentFile());

        } finally {
            if (cacheEntry != null) {
                cacheEntry.close();
            }
        }
    }


    private void checkEatlasDownloadedDocument(File documentFile, URL url) throws IOException {
        Assert.assertNotNull("HttpDocument is null", documentFile);

        String documentStr = FileUtils.readFileToString(documentFile, "UTF-8");

        String dotAll = "(?s)"; // Start the regexp with this to make the dot (.) include new lines (\n)
        String noTag = "[^<>]*";
        String any = ".*";

        String pattern = dotAll + any +
                "<!DOCTYPE html>" + any + // Could have html comment before the <html> tag
                "<html" + noTag + ">" + any +
                "<head" + noTag + ">" + any +
                "<title>" + noTag + "eAtlas" + noTag + "</title>" + any +
                "</head>" + any +
                "<body" + noTag + ">" + any +
                "</body>" + any +
                "</html>" + any;

        Assert.assertTrue(String.format("The download document for eAtlas is not as expected. " +
                "URL: %s, downloaded document: %s",
                url, documentFile.getAbsolutePath()),
                documentStr.matches(pattern));
    }
}
