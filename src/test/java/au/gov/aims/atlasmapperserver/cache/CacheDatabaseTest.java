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

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

public class CacheDatabaseTest {

    @Test
    public void testInsertUpdateDelete() throws Exception {
        CacheDatabase cacheDatabase = new CacheDatabase(null);

        String entityId = "ea";

        // Minimum fields
        URL eatlasUrl = new URL("https://eatlas.org.au");

        CacheEntry eatlasCacheEntry = new CacheEntry(eatlasUrl);
        eatlasCacheEntry.addUsage(entityId);

        // All fields
        URL googleUrl = new URL("https://google.com");
        long googleRequestTimestamp = 1500000000L;
        long googleLastAccessTimestamp = 1544760879L;
        long googleExpiryTimestamp = 1600000000L;
        int googleHttpStatusCode = 200;
        boolean googleValid = true;

        // Copy the InputStream into a temporary File.
        // NOTE: CacheEntry files are temporary and are automatically deleted when the instance is closed.
        InputStream googleDocumentInputStream = CacheDatabaseTest.class.getClassLoader().getResourceAsStream("google.html");
        File tmpGoogleDocument = File.createTempFile("google_", ".html");
        // The InputStream is closed by FileUtils
        FileUtils.copyToFile(googleDocumentInputStream, tmpGoogleDocument);

        CacheEntry googleCacheEntry = new CacheEntry(googleUrl);
        googleCacheEntry.setRequestTimestamp(googleRequestTimestamp);
        googleCacheEntry.setLastAccessTimestamp(googleLastAccessTimestamp);
        googleCacheEntry.setExpiryTimestamp(googleExpiryTimestamp);
        googleCacheEntry.setHttpStatusCode(googleHttpStatusCode);
        googleCacheEntry.setValid(googleValid);
        googleCacheEntry.setDocumentFile(tmpGoogleDocument);
        googleCacheEntry.addUsage(entityId);

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

            Assert.assertNull("Retrieving the eAtlas cache entry from the database didn't return null when it was not found in the DataNase.",
                    cacheDatabase.get(eatlasCacheEntry.getUrl()));

            Assert.assertNull("Retrieving the Google cache entry from the database didn't return null when it was not found in the DataNase.",
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

        googleCacheEntry.close();
        eatlasCacheEntry.close();
    }

    @Test
    public void testCleanUp() throws Exception {
        // 1. Insert a few URLs, associated to 2 distinct entity ID ("ea" and "csiro"), with 2 URLs in commor
        // 2. Run a harvest simulation on "ea", to set the lastAccessTimestamps of some of the URLs
        // 3. Run the clean-up for "ea"
        // 4. Check - Expect all "csiro" URLs to still be there, and the untouched URLs of "ea" to be gone.

        CacheDatabase cacheDatabase = new CacheDatabase(null);

        String eaId = "ea";
        String csiroId = "csiro";

        // URLs
        URL eatlasUrl = new URL("https://eatlas.org.au");
        URL eatlasMestUrl1 = new URL("https://www.doi.org/10.1234/2017.00001"); // Common with CSIRO
        URL eatlasMestUrl2 = new URL("https://www.doi.org/10.1234/2017.00002");
        URL eatlasMestUrl3 = new URL("https://www.doi.org/10.1234/2017.00003"); // Common with CSIRO
        URL eatlasMestUrl4 = new URL("https://www.doi.org/10.1234/2017.00004");
        URL eatlasMestUrl5 = new URL("https://www.doi.org/10.1234/2017.00005");
        URL eatlasMestUrl6 = new URL("https://www.doi.org/10.1234/2017.00006");
        URL eatlasMestUrl7 = new URL("https://www.doi.org/10.1234/2017.00007");

        URL csiroUrl = new URL("https://www.csiro.au/");
        URL csiroMestUrl1 = new URL("https://www.doi.org/10.1234/2017.00001"); // Common with eAtlas
        URL csiroMestUrl2 = new URL("https://www.doi.org/10.1234/2017.00012");
        URL csiroMestUrl3 = new URL("https://www.doi.org/10.1234/2017.00003"); // Common with eAtlas
        URL csiroMestUrl4 = new URL("https://www.doi.org/10.1234/2017.00014");
        URL csiroMestUrl5 = new URL("https://www.doi.org/10.1234/2017.00015");
        URL csiroMestUrl6 = new URL("https://www.doi.org/10.1234/2017.00016");
        URL csiroMestUrl7 = new URL("https://www.doi.org/10.1234/2017.00017");


        // First, ensure none of those URLs are in the Database
        try {
            cacheDatabase.openConnection();

            // eAtlas
            cacheDatabase.delete(eatlasUrl);
            cacheDatabase.delete(eatlasMestUrl1);
            cacheDatabase.delete(eatlasMestUrl2);
            cacheDatabase.delete(eatlasMestUrl3);
            cacheDatabase.delete(eatlasMestUrl4);
            cacheDatabase.delete(eatlasMestUrl5);
            cacheDatabase.delete(eatlasMestUrl6);
            cacheDatabase.delete(eatlasMestUrl7);

            // CSIRO
            cacheDatabase.delete(csiroUrl);
            cacheDatabase.delete(csiroMestUrl1);
            cacheDatabase.delete(csiroMestUrl2);
            cacheDatabase.delete(csiroMestUrl3);
            cacheDatabase.delete(csiroMestUrl4);
            cacheDatabase.delete(csiroMestUrl5);
            cacheDatabase.delete(csiroMestUrl6);
            cacheDatabase.delete(csiroMestUrl7);
        } finally {
            cacheDatabase.close();
        }


        // 1. Insert a few URLs
        try {
            cacheDatabase.openConnection();

            // Insert eAtlas urls
            CacheEntry eatlasCacheEntry = this.getCacheEntry(eatlasUrl);
            eatlasCacheEntry.addUsage(eaId);
            cacheDatabase.save(eatlasCacheEntry);

            eatlasCacheEntry = this.getCacheEntry(eatlasMestUrl2);
            eatlasCacheEntry.addUsage(eaId);
            cacheDatabase.save(eatlasCacheEntry);

            eatlasCacheEntry = this.getCacheEntry(eatlasMestUrl4);
            eatlasCacheEntry.addUsage(eaId);
            cacheDatabase.save(eatlasCacheEntry);

            eatlasCacheEntry = this.getCacheEntry(eatlasMestUrl5);
            eatlasCacheEntry.addUsage(eaId);
            cacheDatabase.save(eatlasCacheEntry);

            eatlasCacheEntry = this.getCacheEntry(eatlasMestUrl6);
            eatlasCacheEntry.addUsage(eaId);
            cacheDatabase.save(eatlasCacheEntry);

            eatlasCacheEntry = this.getCacheEntry(eatlasMestUrl7);
            eatlasCacheEntry.addUsage(eaId);
            cacheDatabase.save(eatlasCacheEntry);


            // Insert CSIRO urls
            CacheEntry csiroCacheEntry = this.getCacheEntry(csiroUrl);
            csiroCacheEntry.addUsage(csiroId);
            cacheDatabase.save(csiroCacheEntry);

            csiroCacheEntry = this.getCacheEntry(csiroMestUrl2);
            csiroCacheEntry.addUsage(csiroId);
            cacheDatabase.save(csiroCacheEntry);

            csiroCacheEntry = this.getCacheEntry(csiroMestUrl4);
            csiroCacheEntry.addUsage(csiroId);
            cacheDatabase.save(csiroCacheEntry);

            csiroCacheEntry = this.getCacheEntry(csiroMestUrl5);
            csiroCacheEntry.addUsage(csiroId);
            cacheDatabase.save(csiroCacheEntry);

            csiroCacheEntry = this.getCacheEntry(csiroMestUrl6);
            csiroCacheEntry.addUsage(csiroId);
            cacheDatabase.save(csiroCacheEntry);

            csiroCacheEntry = this.getCacheEntry(csiroMestUrl7);
            csiroCacheEntry.addUsage(csiroId);
            cacheDatabase.save(csiroCacheEntry);


            // Insert commun URLs
            CacheEntry commonCacheEntry = this.getCacheEntry(eatlasMestUrl1);
            commonCacheEntry.addUsage(eaId);
            commonCacheEntry.addUsage(csiroId);
            cacheDatabase.save(commonCacheEntry);

            commonCacheEntry = this.getCacheEntry(eatlasMestUrl3);
            commonCacheEntry.addUsage(eaId);
            commonCacheEntry.addUsage(csiroId);
            cacheDatabase.save(commonCacheEntry);

        } finally {
            cacheDatabase.close();
        }

        // Little pause to be sure the last access is greated
        Thread.sleep(10);


        // 2. Run a harvest simulation on "ea"
        // We will access eAtlas, mest2, mest3, and mest4
        long beforeHarvest = CacheEntry.getCurrentTimestamp();
        try {
            cacheDatabase.openConnection();

            CacheEntry cacheEntry = cacheDatabase.get(eatlasUrl);
            cacheEntry.setLastAccessTimestamp(CacheEntry.getCurrentTimestamp());
            cacheDatabase.save(cacheEntry);

            cacheEntry = cacheDatabase.get(eatlasMestUrl2);
            cacheEntry.setLastAccessTimestamp(CacheEntry.getCurrentTimestamp());
            cacheDatabase.save(cacheEntry);

            cacheEntry = cacheDatabase.get(eatlasMestUrl3);
            cacheEntry.setLastAccessTimestamp(CacheEntry.getCurrentTimestamp());
            cacheDatabase.save(cacheEntry);

            cacheEntry = cacheDatabase.get(eatlasMestUrl4);
            cacheEntry.setLastAccessTimestamp(CacheEntry.getCurrentTimestamp());
            cacheDatabase.save(cacheEntry);

        } finally {
            cacheDatabase.close();
        }


        // 3. Run the clean-up for "ea"
        try {
            cacheDatabase.openConnection();

            cacheDatabase.cleanUp(eaId, beforeHarvest);
        } finally {
            cacheDatabase.close();
        }


        // 4. Check
        try {
            cacheDatabase.openConnection();

            // eAtlas
            CacheEntry cacheEntry = cacheDatabase.get(eatlasUrl); // Visited
            Assert.assertNotNull("eAtlas URL missing from the cache", cacheEntry);
            Assert.assertTrue("eAtlas URL is not used by " + eaId, cacheEntry.getUsage().contains(eaId));
            Assert.assertFalse("eAtlas URL is used by " + csiroId, cacheEntry.getUsage().contains(csiroId));

            cacheEntry = cacheDatabase.get(eatlasMestUrl1); // NOT Visited, shared
            Assert.assertNotNull("eAtlas MEST URL 1 missing from the cache", cacheEntry);
            Assert.assertFalse("eAtlas MEST URL 1 is used by " + eaId, cacheEntry.getUsage().contains(eaId));
            Assert.assertTrue("eAtlas MEST URL 1 is not used by " + csiroId, cacheEntry.getUsage().contains(csiroId));

            cacheEntry = cacheDatabase.get(eatlasMestUrl2); // Visited
            Assert.assertNotNull("eAtlas MEST URL 2 missing from the cache", cacheEntry);
            Assert.assertTrue("eAtlas MEST URL 2 is not used by " + eaId, cacheEntry.getUsage().contains(eaId));
            Assert.assertFalse("eAtlas MEST URL 2 is used by " + csiroId, cacheEntry.getUsage().contains(csiroId));

            cacheEntry = cacheDatabase.get(eatlasMestUrl3); // Visited, shared
            Assert.assertNotNull("eAtlas MEST URL 3 missing from the cache", cacheEntry);
            Assert.assertTrue("eAtlas MEST URL 3 is not used by " + eaId, cacheEntry.getUsage().contains(eaId));
            Assert.assertTrue("eAtlas MEST URL 3 is not used by " + csiroId, cacheEntry.getUsage().contains(csiroId));

            cacheEntry = cacheDatabase.get(eatlasMestUrl4); // Visited
            Assert.assertNotNull("eAtlas MEST URL 4 missing from the cache", cacheEntry);
            Assert.assertTrue("eAtlas MEST URL 4 is not used by " + eaId, cacheEntry.getUsage().contains(eaId));
            Assert.assertFalse("eAtlas MEST URL 4 is used by " + csiroId, cacheEntry.getUsage().contains(csiroId));

            cacheEntry = cacheDatabase.get(eatlasMestUrl5); // NOT Visited
            Assert.assertNull("eAtlas MEST URL 5 is still present in the cache", cacheEntry);

            cacheEntry = cacheDatabase.get(eatlasMestUrl6); // NOT Visited
            Assert.assertNull("eAtlas MEST URL 6 is still present in the cache", cacheEntry);

            cacheEntry = cacheDatabase.get(eatlasMestUrl7); // NOT Visited
            Assert.assertNull("eAtlas MEST URL 7 is still present in the cache", cacheEntry);

            // CSIRO
            cacheEntry = cacheDatabase.get(csiroUrl);
            Assert.assertNotNull("CSIRO URL missing from the cache", cacheEntry);
            Assert.assertFalse("CSIRO URL is used by " + eaId, cacheEntry.getUsage().contains(eaId));
            Assert.assertTrue("CSIRO URL is not used by " + csiroId, cacheEntry.getUsage().contains(csiroId));

            cacheEntry = cacheDatabase.get(csiroMestUrl1);
            Assert.assertNotNull("CSIRO MEST URL 1 missing from the cache", cacheEntry);
            Assert.assertFalse("CSIRO MEST URL 1 is used by " + eaId, cacheEntry.getUsage().contains(eaId));
            Assert.assertTrue("CSIRO MEST URL 1 is not used by " + csiroId, cacheEntry.getUsage().contains(csiroId));

            cacheEntry = cacheDatabase.get(csiroMestUrl2);
            Assert.assertNotNull("CSIRO MEST URL 2 missing from the cache", cacheEntry);
            Assert.assertFalse("CSIRO MEST URL 2 is used by " + eaId, cacheEntry.getUsage().contains(eaId));
            Assert.assertTrue("CSIRO MEST URL 2 is not used by " + csiroId, cacheEntry.getUsage().contains(csiroId));

            cacheEntry = cacheDatabase.get(csiroMestUrl3); // Visited
            Assert.assertNotNull("CSIRO MEST URL 3 missing from the cache", cacheEntry);
            Assert.assertTrue("CSIRO MEST URL 3 is not used by " + eaId, cacheEntry.getUsage().contains(eaId));
            Assert.assertTrue("CSIRO MEST URL 3 is not used by " + csiroId, cacheEntry.getUsage().contains(csiroId));

            cacheEntry = cacheDatabase.get(csiroMestUrl4);
            Assert.assertNotNull("CSIRO MEST URL 4 missing from the cache", cacheEntry);
            Assert.assertFalse("CSIRO MEST URL 4 is used by " + eaId, cacheEntry.getUsage().contains(eaId));
            Assert.assertTrue("CSIRO MEST URL 4 is not used by " + csiroId, cacheEntry.getUsage().contains(csiroId));

            cacheEntry = cacheDatabase.get(csiroMestUrl5);
            Assert.assertNotNull("CSIRO MEST URL 5 missing from the cache", cacheEntry);
            Assert.assertFalse("CSIRO MEST URL 5 is used by " + eaId, cacheEntry.getUsage().contains(eaId));
            Assert.assertTrue("CSIRO MEST URL 5 is not used by " + csiroId, cacheEntry.getUsage().contains(csiroId));

            cacheEntry = cacheDatabase.get(csiroMestUrl6);
            Assert.assertNotNull("CSIRO MEST URL 6 missing from the cache", cacheEntry);
            Assert.assertFalse("CSIRO MEST URL 6 is used by " + eaId, cacheEntry.getUsage().contains(eaId));
            Assert.assertTrue("CSIRO MEST URL 6 is not used by " + csiroId, cacheEntry.getUsage().contains(csiroId));

            cacheEntry = cacheDatabase.get(csiroMestUrl7);
            Assert.assertNotNull("CSIRO MEST URL 7 missing from the cache", cacheEntry);
            Assert.assertFalse("CSIRO MEST URL 7 is used by " + eaId, cacheEntry.getUsage().contains(eaId));
            Assert.assertTrue("CSIRO MEST URL 7 is not used by " + csiroId, cacheEntry.getUsage().contains(csiroId));

        } finally {
            cacheDatabase.close();
        }
    }

    private CacheEntry getCacheEntry(URL url) {
        CacheEntry cacheEntry = new CacheEntry(url);
        cacheEntry.setRequestMethod(RequestMethod.GET);
        cacheEntry.setHttpStatusCode(200);
        cacheEntry.setRequestTimestamp(CacheEntry.getCurrentTimestamp());
        cacheEntry.setLastAccessTimestamp(CacheEntry.getCurrentTimestamp());

        return cacheEntry;
    }
}
