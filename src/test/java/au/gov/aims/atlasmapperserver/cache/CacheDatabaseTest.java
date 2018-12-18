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

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.URL;

public class CacheDatabaseTest {

    @Test
    public void testInsertUpdateDelete() throws Exception {
        CacheDatabase cacheDatabase = new CacheDatabase(null);

        // Minimum fields
        URL eatlasUrl = new URL("https://eatlas.org.au");

        CacheEntry eatlasCacheEntry = new CacheEntry(eatlasUrl);


        // All fields
        URL googleUrl = new URL("https://google.com");
        long googleRequestTimestamp = 1500000000L;
        long googleLastAccessTimestamp = 1544760879L;
        long googleExpiryTimestamp = 1600000000L;
        int googleHttpStatusCode = 200;
        boolean googleValid = true;
        File googleDocument = new File(CacheDatabaseTest.class.getClassLoader().getResource("google.html").getFile());

        CacheEntry googleCacheEntry = new CacheEntry(googleUrl);
        googleCacheEntry.setRequestTimestamp(googleRequestTimestamp);
        googleCacheEntry.setLastAccessTimestamp(googleLastAccessTimestamp);
        googleCacheEntry.setExpiryTimestamp(googleExpiryTimestamp);
        googleCacheEntry.setHttpStatusCode(googleHttpStatusCode);
        googleCacheEntry.setValid(googleValid);
        googleCacheEntry.setDocumentFile(googleDocument);

        // Try deleting the entries just in case they were left behind by another test
        try {
            cacheDatabase.openConnection();

            cacheDatabase.delete(eatlasCacheEntry.getUrl());
            cacheDatabase.delete(googleCacheEntry.getUrl());
        } finally {
            cacheDatabase.close();
        }

        // Ensure the URL are not in the DB.
        try {
            cacheDatabase.openConnection();

            Assert.assertFalse("The eAtlas cache entry was found in the database after been deleted.",
                    cacheDatabase.exists(eatlasCacheEntry.getUrl()));

            Assert.assertFalse("The Google cache entry was found in the database after been deleted.",
                    cacheDatabase.exists(googleCacheEntry.getUrl()));
        } finally {
            cacheDatabase.close();
        }

        // Ensure Get returns null
        try {
            cacheDatabase.openConnection();

            Assert.assertNull("Retrieving the eAtlas cache entry from the DataBase didn't return null when it was not found in the DataNase.",
                    cacheDatabase.get(eatlasCacheEntry.getUrl()));

            Assert.assertNull("Retrieving the Google cache entry from the DataBase didn't return null when it was not found in the DataNase.",
                    cacheDatabase.get(googleCacheEntry.getUrl()));
        } finally {
            cacheDatabase.close();
        }

        // Insert the data - Select to be sure it has been inserted properly.
        try {
            cacheDatabase.openConnection();

            cacheDatabase.save(eatlasCacheEntry);
            cacheDatabase.save(googleCacheEntry);

            CacheEntry retrievedEatlasCacheEntry = cacheDatabase.get(eatlasUrl);
            CacheEntry retrievedGoogleCacheEntry = cacheDatabase.get(googleUrl);

            Assert.assertEquals("Retrieved eAtlas entry is different from the saved one.", eatlasCacheEntry, retrievedEatlasCacheEntry);
            Assert.assertEquals("Retrieved Google entry is different from the saved one.", googleCacheEntry, retrievedGoogleCacheEntry);
        } finally {
            cacheDatabase.close();
        }

        // Ensure the URL are in the DB.
        try {
            cacheDatabase.openConnection();

            Assert.assertTrue(cacheDatabase.exists(eatlasCacheEntry.getUrl()));
            Assert.assertTrue(cacheDatabase.exists(googleCacheEntry.getUrl()));
        } finally {
            cacheDatabase.close();
        }

        // Try to Select them back after closing the DB connection, to be sure they are really in the DB.
        try {
            cacheDatabase.openConnection();

            CacheEntry retrievedEatlasCacheEntry = cacheDatabase.get(eatlasUrl);
            CacheEntry retrievedGoogleCacheEntry = cacheDatabase.get(googleUrl);

            Assert.assertEquals("Retrieved eAtlas entry is different from the saved one.", eatlasCacheEntry, retrievedEatlasCacheEntry);
            Assert.assertEquals("Retrieved Google entry is different from the saved one.", googleCacheEntry, retrievedGoogleCacheEntry);
        } finally {
            cacheDatabase.close();
        }

        // Update
        try {
            cacheDatabase.openConnection();

            eatlasCacheEntry.setLastAccessTimestamp(5000L);
            eatlasCacheEntry.setHttpStatusCode(200);

            googleCacheEntry.setRequestTimestamp(1000L);
            googleCacheEntry.setLastAccessTimestamp(2000L);
            googleCacheEntry.setExpiryTimestamp(3000L);
            googleCacheEntry.setHttpStatusCode(500);
            googleCacheEntry.setValid(false);
            googleCacheEntry.setDocumentFile(null);

            cacheDatabase.save(eatlasCacheEntry);
            cacheDatabase.save(googleCacheEntry);

            CacheEntry retrievedEatlasCacheEntry = cacheDatabase.get(eatlasUrl);
            CacheEntry retrievedGoogleCacheEntry = cacheDatabase.get(googleUrl);

            Assert.assertEquals("Retrieved eAtlas entry is different from the saved one.", eatlasCacheEntry, retrievedEatlasCacheEntry);
            Assert.assertEquals("Retrieved Google entry is different from the saved one.", googleCacheEntry, retrievedGoogleCacheEntry);
        } finally {
            cacheDatabase.close();
        }

        // Cleanup
        try {
            cacheDatabase.openConnection();

            cacheDatabase.delete(eatlasCacheEntry.getUrl());
            cacheDatabase.delete(googleCacheEntry.getUrl());
        } finally {
            cacheDatabase.close();
        }

        // Ensure the DB is clean
        try {
            cacheDatabase.openConnection();

            Assert.assertFalse(cacheDatabase.exists(eatlasCacheEntry.getUrl()));
            Assert.assertFalse(cacheDatabase.exists(googleCacheEntry.getUrl()));
        } finally {
            cacheDatabase.close();
        }
    }
}
