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
import au.gov.aims.atlasmapperserver.thread.ThreadLogger;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class CacheDatabaseTest {

    @Test
    public void testInsertUpdateDelete() throws RevivableThreadInterruptedException, MalformedURLException, JSONException {
        ThreadLogger logger = new ThreadLogger();

        // Minimum fields
        URL eatlasUrl = new URL("https://eatlas.org.au");

        CacheEntry eatlasCacheEntry = new CacheEntry(eatlasUrl);


        // All fields
        URL googleUrl = new URL("https://google.com");
        long googleDownloadTimestamp = 1500000000L;
        long googleLastAccessTimestamp = 1544760879L;
        long googleExpiryTimestamp = 1600000000L;
        short googleHttpStatusCode = 200;
        boolean googleValid = true;
        File googleDocument = new File(CacheDatabaseTest.class.getClassLoader().getResource("google.html").getFile());

        CacheEntry googleCacheEntry = new CacheEntry(googleUrl);
        googleCacheEntry.setDownloadTimestamp(googleDownloadTimestamp);
        googleCacheEntry.setLastAccessTimestamp(googleLastAccessTimestamp);
        googleCacheEntry.setExpiryTimestamp(googleExpiryTimestamp);
        googleCacheEntry.setHttpStatusCode(googleHttpStatusCode);
        googleCacheEntry.setValid(googleValid);
        googleCacheEntry.setDocumentFile(googleDocument);

        CacheDatabase cacheDatabase = new CacheDatabase(null);

        // Try deleting the entries just in case they were left behind by another test
        try {
            cacheDatabase.openConnection(logger);

            cacheDatabase.delete(logger, eatlasCacheEntry.getUrl());
            cacheDatabase.delete(logger, googleCacheEntry.getUrl());
        } finally {
            cacheDatabase.close();
        }
        Assert.assertTrue("Error occurred: " + logger.toJSON().toString(4), logger.getErrorCount() == 0);

        // Ensure the URL are not in the DB.
        try {
            cacheDatabase.openConnection(logger);

            Assert.assertFalse(cacheDatabase.exists(logger, eatlasCacheEntry.getUrl()));
            Assert.assertFalse(cacheDatabase.exists(logger, googleCacheEntry.getUrl()));
        } finally {
            cacheDatabase.close();
        }
        Assert.assertTrue("Error occurred: " + logger.toJSON().toString(4), logger.getErrorCount() == 0);

        // Insert the data - Select to be sure it has been inserted properly.
        try {
            cacheDatabase.openConnection(logger);

            cacheDatabase.save(logger, eatlasCacheEntry);
            cacheDatabase.save(logger, googleCacheEntry);

            CacheEntry retrievedEatlasCacheEntry = cacheDatabase.get(logger, eatlasUrl);
            CacheEntry retrievedGoogleCacheEntry = cacheDatabase.get(logger, googleUrl);

            Assert.assertEquals("Retrieved eAtlas entry is different from the saved one.", eatlasCacheEntry, retrievedEatlasCacheEntry);
            Assert.assertEquals("Retrieved Google entry is different from the saved one.", googleCacheEntry, retrievedGoogleCacheEntry);
        } finally {
            cacheDatabase.close();
        }
        Assert.assertTrue("Error occurred: " + logger.toJSON().toString(4), logger.getErrorCount() == 0);

        // Ensure the URL are in the DB.
        try {
            cacheDatabase.openConnection(logger);

            Assert.assertTrue(cacheDatabase.exists(logger, eatlasCacheEntry.getUrl()));
            Assert.assertTrue(cacheDatabase.exists(logger, googleCacheEntry.getUrl()));
        } finally {
            cacheDatabase.close();
        }
        Assert.assertTrue("Error occurred: " + logger.toJSON().toString(4), logger.getErrorCount() == 0);

        // Try to Select them back after closing the DB connection, to be sure they are really in the DB.
        try {
            cacheDatabase.openConnection(logger);

            CacheEntry retrievedEatlasCacheEntry = cacheDatabase.get(logger, eatlasUrl);
            CacheEntry retrievedGoogleCacheEntry = cacheDatabase.get(logger, googleUrl);

            Assert.assertEquals("Retrieved eAtlas entry is different from the saved one.", eatlasCacheEntry, retrievedEatlasCacheEntry);
            Assert.assertEquals("Retrieved Google entry is different from the saved one.", googleCacheEntry, retrievedGoogleCacheEntry);
        } finally {
            cacheDatabase.close();
        }
        Assert.assertTrue("Error occurred: " + logger.toJSON().toString(4), logger.getErrorCount() == 0);

        // Update
        try {
            cacheDatabase.openConnection(logger);

            eatlasCacheEntry.setLastAccessTimestamp(5000L);
            eatlasCacheEntry.setHttpStatusCode(200);

            googleCacheEntry.setDownloadTimestamp(1000L);
            googleCacheEntry.setLastAccessTimestamp(2000L);
            googleCacheEntry.setExpiryTimestamp(3000L);
            googleCacheEntry.setHttpStatusCode(500);
            googleCacheEntry.setValid(false);
            googleCacheEntry.setDocumentFile(null);

            cacheDatabase.save(logger, eatlasCacheEntry);
            cacheDatabase.save(logger, googleCacheEntry);

            CacheEntry retrievedEatlasCacheEntry = cacheDatabase.get(logger, eatlasUrl);
            CacheEntry retrievedGoogleCacheEntry = cacheDatabase.get(logger, googleUrl);

            Assert.assertEquals("Retrieved eAtlas entry is different from the saved one.", eatlasCacheEntry, retrievedEatlasCacheEntry);
            Assert.assertEquals("Retrieved Google entry is different from the saved one.", googleCacheEntry, retrievedGoogleCacheEntry);
        } finally {
            cacheDatabase.close();
        }
        Assert.assertTrue("Error occurred: " + logger.toJSON().toString(4), logger.getErrorCount() == 0);

        // Cleanup
        try {
            cacheDatabase.openConnection(logger);

            cacheDatabase.delete(logger, eatlasCacheEntry.getUrl());
            cacheDatabase.delete(logger, googleCacheEntry.getUrl());
        } finally {
            cacheDatabase.close();
        }
        Assert.assertTrue("Error occurred: " + logger.toJSON().toString(4), logger.getErrorCount() == 0);

        // Ensure the DB is clean
        try {
            cacheDatabase.openConnection(logger);

            Assert.assertFalse(cacheDatabase.exists(logger, eatlasCacheEntry.getUrl()));
            Assert.assertFalse(cacheDatabase.exists(logger, googleCacheEntry.getUrl()));
        } finally {
            cacheDatabase.close();
        }
        Assert.assertTrue("Error occurred: " + logger.toJSON().toString(4), logger.getErrorCount() == 0);
    }
}
