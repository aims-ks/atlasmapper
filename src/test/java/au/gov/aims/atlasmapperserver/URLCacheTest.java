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

/*
 * This class test the URLCache application. It rely on stable URLs that always
 * return the same thing. No online services can be used since they are not stable
 * enough for this kind of tests. Instead, it rely on an other WebApp called
 * HttpMockup.
 *
 * 1. Deploy the HttpMockup web application (httpmockup.war) found in the test/resources folder.
 *     Note that the HttpMockup sources can be checked out from Google code. (currently not available)
 * 2. Change the HTTPMOCKUP_SERVICE_URL constant if the HttpMockup is deployed under a different URL.
 *     Currently: http://localhost:8080/httpmockup/
 * 3. Run the tests. If the application is not deployed properly, it will display an error message and
 *     it will skip all tests.
 */

/*
 * Tests for all cases:
 *     1st request of a URL
 *         Succeed
 *             Commit                            [test_New_Commit]
 *             Rollback                          [test_New_Rollback]
 *         Failed (an error occurred and null is returned)
 *             Commit                            [test_New_FailedCommit]
 *             Rollback                          [test_New_FailedRollback]
 *
 *     1st request was approved
 *         Before timeout (should return what is in the cache, no re-download)
 *             2nd request (cached) - Commit     [test_Approved_Commit]
 *             2nd request (cached) - Rollback   [test_Approved_Rollback]
 *         After timeout (re-download)
 *             2nd request - Succeed
 *                 Commit                        [test_ApprovedTimeout_Commit]
 *                 Rollback                      [test_ApprovedTimeout_Rollback]
 *             2nd request - Failed (an error occurred and null is returned)
 *                 Commit                        [test_ApprovedTimeout_FailedCommit]
 *                 Rollback                      [test_ApprovedTimeout_FailedRollback]
 *
 *     1st request was NOT approved
 *         Before timeout (should return what is in the cache, no re-download)
 *             2nd request (cached) - Commit     [test_NotApproved_Commit]
 *             2nd request (cached) - Rollback   [test_NotApproved_Rollback]
 *         After timeout (re-download)
 *             2nd request - Succeed
 *                 Commit                        [test_NotApprovedTimeout_Commit]
 *                 Rollback                      [test_NotApprovedTimeout_Rollback]
 *             2nd request - Failed (an error occurred and null is returned)
 *                 Commit                        [test_NotApprovedTimeout_FailedCommit]
 *                 Rollback                      [test_NotApprovedTimeout_FailedRollback]
 *
 *     1st request returned null
 *         Before timeout (should return what is in the cache, no re-download)
 *             2nd request (cached) - Commit     [test_Null_Commit]
 *             2nd request (cached) - Rollback   [test_Null_Rollback]
 *         After timeout (re-download)
 *             2nd request - Succeed
 *                 Commit                        [test_NullTimeout_Commit]
 *                 Rollback                      [test_NullTimeout_Rollback]
 *             2nd request - Failed (an error occurred and null is returned)
 *                 Commit                        [test_NullTimeout_FailedCommit]
 *                 Rollback                      [test_NullTimeout_FailedRollback]
 *
 * Tests with real data - they should all pass if the previous tests are exhaustive enough:
 *     [testFailTooLongNoFileSize]
 *         Request file, it's now too large, return null, rollback. Ensure it do not download the whole thing.
 *     [testDownloadFailThenSucceed]
 *         Receive null at first request, rollback - timeout - receive something at 2nd request, commit
 *     [testSucceedThenTooLong]
 *         Request file, commit - timeout - request same file, it's now too large, return null, rollback
 *     [testSucceedTwiceWithSameResult]
 *         Get XML GetCapabilities document, parse, commit - timeout - receive same doc after timeout, parse, commit
 *     [testParseFailThenSucceed]
 *         Get XML GetCapabilities document with stacktrace, unable to parse, rollback - timeout - receive valid document, parse, commit
 *     [testSucceedThenParseFail]
 *         Get XML GetCapabilities document, parse, commit - timeout - receive document with stacktrace, unable to parse, rollback
 *     [testTC211ParseFailThenTC211Succeed]
 *         Request TC211 document, receive HTML, unable to parse, rollback - timeout - receive valid TC211 document, parse, commit
 *     [testTC211SucceedThenTC211ParseFail]
 *         Request TC211 document, parse, commit - timeout - receive HTML doc instead of TC211 document, unable to parse, rollback
 *     [testMultiUrl_Fail1_Success2_Fail1_WithRollback]
 *         URL1 return null, rollback - URL2 (same domain) return file, commit - request URL1 again (before timeout), expect null
 *     [testMultiUrl_Fail1_Success2_Fail1_WithCommit]
 *         URL1 return null, commit - URL2 (same domain) return file, commit - request URL1 again (before timeout), expect null
 */
package au.gov.aims.atlasmapperserver;

import au.gov.aims.atlasmapperserver.dataSourceConfig.WMSDataSourceConfig;
import au.gov.aims.atlasmapperserver.thread.RevivableThreadInterruptedException;
import au.gov.aims.atlasmapperserver.thread.ThreadLogger;
import au.gov.aims.atlasmapperserver.xml.TC211.TC211Document;
import au.gov.aims.atlasmapperserver.xml.TC211.TC211Parser;
import org.geotools.data.ows.WMSCapabilities;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

// TODO UPDATE TESTS - THE CACHE LOGIC HAS CHANGED - NOW ENTIRELY MANAGED BY THE USER, NO MORE TIMEOUT
public class URLCacheTest {
    private static final Logger LOGGER = Logger.getLogger(URLCacheTest.class.getName());
    private static final String HTTPMOCKUP_SERVICE_URL = "http://localhost:8080/httpmockup/";
    private static final String NONE_EXISTING_URL = "http://localhost:1/thisUrlDontExists/";

    private static final Integer SC_OK = 200;

    private Boolean serviceExists = null;


    // 1st request of a URL
    //     Succeed
    //         Commit                            [test_New_Commit]
    @Test
    public void test_New_Commit() throws Exception, RevivableThreadInterruptedException {
        if (this.serviceExists()) {
            ThreadLogger logger = new ThreadLogger();
            ConfigManager configManager = getConfigManager();
            URLCache.deleteCache(configManager, false);
            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());

            String urlStr = HTTPMOCKUP_SERVICE_URL + "?content=abcd";

            File file = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, urlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // Validate the downloaded info
            String content = readFile(file);
            Assert.assertEquals("abcd", content);

            // Validate the cached info
            URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), urlStr);
            Assert.assertEquals(SC_OK, cachedFile.getHttpStatusCode());
            Assert.assertEquals(file.getName(), cachedFile.getFilename());
            Assert.assertEquals(URLCache.CACHE_TIMEOUT, cachedFile.getExpiry());
            Assert.assertNull(cachedFile.getLatestErrorMessage());
            Assert.assertFalse(cachedFile.isApproved());

            URLCache.commitURLFile(configManager, file, urlStr);

            Assert.assertEquals(SC_OK, cachedFile.getHttpStatusCode());
            Assert.assertEquals(file.getName(), cachedFile.getFilename());
            Assert.assertEquals(URLCache.CACHE_TIMEOUT, cachedFile.getExpiry());
            Assert.assertFalse(cachedFile.hasTemporaryData());
            Assert.assertNull(cachedFile.getLatestErrorMessage());
            Assert.assertTrue(cachedFile.isApproved());

            Assert.assertTrue(file.exists());
            Assert.assertEquals(1, URLCache.countFile(getTestApplicationFolder()));

            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());
        }
    }

    // 1st request of a URL
    //     Succeed
    //         Rollback                          [test_New_Rollback]
    @Test
    public void test_New_Rollback() throws Exception, RevivableThreadInterruptedException {
        if (this.serviceExists()) {
            ThreadLogger logger = new ThreadLogger();
            ConfigManager configManager = getConfigManager();
            URLCache.deleteCache(configManager, false);
            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());

            String urlStr = HTTPMOCKUP_SERVICE_URL + "?content=abcd";

            File file = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, urlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // Validate the downloaded info
            String content = readFile(file);
            Assert.assertEquals("abcd", content);

            // Validate the cached info
            URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), urlStr);
            Assert.assertEquals(SC_OK, cachedFile.getHttpStatusCode());
            Assert.assertEquals(file.getName(), cachedFile.getFilename());
            Assert.assertEquals(URLCache.CACHE_TIMEOUT, cachedFile.getExpiry());
            Assert.assertNull(cachedFile.getLatestErrorMessage());
            Assert.assertFalse(cachedFile.isApproved());

            URLCache.rollbackURLFile(logger, "WMS GetCapabilities document", configManager, file, urlStr, "Invalid");

            Assert.assertEquals(SC_OK, cachedFile.getHttpStatusCode());
            Assert.assertNull(cachedFile.getFilename()); // Deleted file
            Assert.assertEquals(URLCache.INVALID_FILE_CACHE_TIMEOUT, cachedFile.getExpiry());
            Assert.assertFalse(cachedFile.hasTemporaryData());
            Assert.assertEquals("Invalid", cachedFile.getLatestErrorMessage());
            Assert.assertFalse(cachedFile.isApproved());

            Assert.assertFalse(file.exists());
            Assert.assertEquals(0, URLCache.countFile(getTestApplicationFolder()));

            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());
        }
    }

    // 1st request of a URL
    //     Failed (an error occurred and null is returned)
    //         Commit                            [test_New_FailedCommit]
    @Test
    public void test_New_FailedCommit() throws Exception, RevivableThreadInterruptedException {
        if (this.serviceExists()) {
            ThreadLogger logger = new ThreadLogger();
            ConfigManager configManager = getConfigManager();
            URLCache.deleteCache(configManager, false);
            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());

            String urlStr = NONE_EXISTING_URL;

            File file = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, urlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // Validate the downloaded info
            Assert.assertNull(file);

            // Validate the cached info
            URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), urlStr);
            Assert.assertNull(cachedFile.getHttpStatusCode()); // The server do not exists, therefor it didn't returned a status code.
            //Assert.assertEquals(file.getName(), cachedFile.getFilename());
            Assert.assertEquals(URLCache.INVALID_FILE_CACHE_TIMEOUT, cachedFile.getExpiry());
            Assert.assertNotNull(cachedFile.getLatestErrorMessage());
            Assert.assertFalse(cachedFile.isApproved());

            URLCache.commitURLFile(configManager, file, urlStr);

            Assert.assertNull(cachedFile.getHttpStatusCode());
            //Assert.assertEquals(file.getName(), cachedFile.getFilename());
            Assert.assertEquals(URLCache.CACHE_TIMEOUT, cachedFile.getExpiry());
            Assert.assertFalse(cachedFile.hasTemporaryData());
            Assert.assertNotNull(cachedFile.getLatestErrorMessage());
            Assert.assertTrue(cachedFile.isApproved());

            Assert.assertNull(file);
            Assert.assertEquals(0, URLCache.countFile(getTestApplicationFolder()));

            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());
        }
    }

    // 1st request of a URL
    //     Failed (an error occurred and null is returned)
    //         Rollback                          [test_New_FailedRollback]
    @Test
    public void test_New_FailedRollback() throws Exception, RevivableThreadInterruptedException {
        if (this.serviceExists()) {
            ThreadLogger logger = new ThreadLogger();
            ConfigManager configManager = getConfigManager();
            URLCache.deleteCache(configManager, false);
            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());

            String urlStr = NONE_EXISTING_URL;

            File file = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, urlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // Validate the downloaded info
            Assert.assertNull(file);

            // Validate the cached info
            URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), urlStr);
            Assert.assertNull(cachedFile.getHttpStatusCode()); // The server do not exists, therefor it didn't returned a status code.
            //Assert.assertEquals(file.getName(), cachedFile.getFilename());
            Assert.assertEquals(URLCache.INVALID_FILE_CACHE_TIMEOUT, cachedFile.getExpiry());
            Assert.assertNotNull(cachedFile.getLatestErrorMessage());
            Assert.assertFalse(cachedFile.isApproved());

            URLCache.rollbackURLFile(logger, "WMS GetCapabilities document", configManager, file, urlStr, "New error message");

            Assert.assertNull(cachedFile.getHttpStatusCode());
            //Assert.assertEquals(file.getName(), cachedFile.getFilename());
            Assert.assertEquals(URLCache.INVALID_FILE_CACHE_TIMEOUT, cachedFile.getExpiry());
            Assert.assertFalse(cachedFile.hasTemporaryData());
            Assert.assertNotNull(cachedFile.getLatestErrorMessage());
            Assert.assertFalse(cachedFile.isApproved());

            Assert.assertNull(file);
            Assert.assertEquals(0, URLCache.countFile(getTestApplicationFolder()));

            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());
        }
    }




    // 1st request was approved
    //     Before timeout (should return what is in the cache, no re-download)
    //         2nd request (cached) - Commit     [test_Approved_Commit]
    @Test
    public void test_Approved_Commit() throws Exception, RevivableThreadInterruptedException {
        if (this.serviceExists()) {
            ThreadLogger logger = new ThreadLogger();
            ConfigManager configManager = getConfigManager();
            URLCache.deleteCache(configManager, false);
            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());

            String urlStr = HTTPMOCKUP_SERVICE_URL + "?content=abcd";

            // Download the file for the 1st time
            File file1 = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, urlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // Commit (approve) the file
            URLCache.commitURLFile(configManager, file1, urlStr);


            // Re-request the file - the timeout hasn't expired, it must give the same cached file
            File file2 = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, urlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // Ensure the returned file is the cached file
            Assert.assertEquals(file1, file2);

            // Validate the downloaded info
            String content = readFile(file2);
            Assert.assertEquals("abcd", content);

            // Validate the cached info
            URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), urlStr);
            Assert.assertEquals(SC_OK, cachedFile.getHttpStatusCode());
            Assert.assertEquals(file1.getName(), cachedFile.getFilename());
            Assert.assertEquals(URLCache.CACHE_TIMEOUT, cachedFile.getExpiry());
            Assert.assertNull(cachedFile.getLatestErrorMessage());
            Assert.assertTrue(cachedFile.isApproved());

            URLCache.commitURLFile(configManager, file2, urlStr);

            Assert.assertEquals(SC_OK, cachedFile.getHttpStatusCode());
            Assert.assertEquals(file2.getName(), cachedFile.getFilename());
            Assert.assertEquals(URLCache.CACHE_TIMEOUT, cachedFile.getExpiry());
            Assert.assertFalse(cachedFile.hasTemporaryData());
            Assert.assertNull(cachedFile.getLatestErrorMessage());
            Assert.assertTrue(cachedFile.isApproved());

            Assert.assertTrue(file1.exists());
            Assert.assertTrue(file2.exists());
            Assert.assertEquals(1, URLCache.countFile(getTestApplicationFolder()));

            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());
        }
    }

    // 1st request was approved
    //     Before timeout (should return what is in the cache, no re-download)
    //         2nd request (cached) - Rollback   [test_Approved_Rollback]
    @Test
    public void test_Approved_Rollback() throws Exception, RevivableThreadInterruptedException {
        if (this.serviceExists()) {
            ThreadLogger logger = new ThreadLogger();
            ConfigManager configManager = getConfigManager();
            URLCache.deleteCache(configManager, false);
            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());

            String urlStr = HTTPMOCKUP_SERVICE_URL + "?content=abcd";

            // Download the file for the 1st time
            File file1 = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, urlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // Commit (approve) the file
            URLCache.commitURLFile(configManager, file1, urlStr);


            // Re-request the file - the timeout hasn't expired, it must give the same cached file
            File file2 = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, urlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // Ensure the returned file is the cached file
            Assert.assertEquals(file1, file2);

            // Validate the downloaded info
            String content = readFile(file2);
            Assert.assertEquals("abcd", content);

            // Validate the cached info
            URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), urlStr);
            Assert.assertEquals(SC_OK, cachedFile.getHttpStatusCode());
            Assert.assertEquals(file1.getName(), cachedFile.getFilename());
            Assert.assertEquals(URLCache.CACHE_TIMEOUT, cachedFile.getExpiry());
            Assert.assertNull(cachedFile.getLatestErrorMessage());
            Assert.assertTrue(cachedFile.isApproved());

            File file3 = URLCache.rollbackURLFile(logger, "WMS GetCapabilities document", configManager, file2, urlStr, "New error message");

            // Ensure the returned file is the first one (proper rollback)
            Assert.assertEquals(file1, file3);

            Assert.assertEquals(SC_OK, cachedFile.getHttpStatusCode());
            Assert.assertEquals(file1.getName(), cachedFile.getFilename());
            Assert.assertEquals(URLCache.INVALID_FILE_CACHE_TIMEOUT, cachedFile.getExpiry());
            Assert.assertFalse(cachedFile.hasTemporaryData());
            Assert.assertNotNull(cachedFile.getLatestErrorMessage());
            Assert.assertTrue(cachedFile.isApproved());

            Assert.assertTrue(file1.exists());
            // File2 exists even after rollback, since it's the same file as file1
            Assert.assertTrue(file2.exists());
            Assert.assertTrue(file3.exists());
            Assert.assertEquals(1, URLCache.countFile(getTestApplicationFolder()));

            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());
        }
    }

    // 1st request was approved
    //     After timeout (re-download)
    //         2nd request - Succeed
    //             Commit                        [test_ApprovedTimeout_Commit]
    @Test
    public void test_ApprovedTimeout_Commit() throws Exception, RevivableThreadInterruptedException {
        if (this.serviceExists()) {
            ThreadLogger logger = new ThreadLogger();
            ConfigManager configManager = getConfigManager();
            URLCache.deleteCache(configManager, false);
            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());

            String urlStr = HTTPMOCKUP_SERVICE_URL + "?content=abcd";

            // Download the file for the 1st time
            File file1 = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, urlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // Commit (approve) the file
            URLCache.commitURLFile(configManager, file1, urlStr);

            // Set expiry to trigger the re-download
            URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), urlStr);
            cachedFile.setExpiry(0);
            Assert.assertEquals(0, cachedFile.getExpiry());


            // Re-request the file - the timeout has expired, it must re-download the file
            File file2 = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, urlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // Ensure the returned file is NOT the cached file
            Assert.assertFalse(file1.equals(file2));

            // Validate the downloaded info
            String content = readFile(file2);
            Assert.assertEquals("abcd", content);

            // Validate the cached info
            cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), urlStr);
            Assert.assertEquals(SC_OK, cachedFile.getHttpStatusCode());
            Assert.assertEquals(file1.getName(), cachedFile.getFilename());
            //Assert.assertEquals(URLCache.CACHE_TIMEOUT, cachedFile.getExpiry());
            Assert.assertTrue(cachedFile.hasTemporaryData());
            Assert.assertEquals(SC_OK, cachedFile.getTemporaryHttpStatusCode());
            Assert.assertEquals(file2.getName(), cachedFile.getTemporaryFilename());
            Assert.assertNull(cachedFile.getLatestErrorMessage());
            Assert.assertTrue(cachedFile.isApproved());

            URLCache.commitURLFile(configManager, file2, urlStr);

            Assert.assertEquals(SC_OK, cachedFile.getHttpStatusCode());
            Assert.assertEquals(file2.getName(), cachedFile.getFilename());
            Assert.assertEquals(URLCache.CACHE_TIMEOUT, cachedFile.getExpiry());
            Assert.assertFalse(cachedFile.hasTemporaryData());
            Assert.assertNull(cachedFile.getLatestErrorMessage());
            Assert.assertTrue(cachedFile.isApproved());

            Assert.assertFalse(file1.exists());
            Assert.assertTrue(file2.exists());
            Assert.assertEquals(1, URLCache.countFile(getTestApplicationFolder()));

            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());
        }
    }

    // 1st request was approved
    //     After timeout (re-download)
    //         2nd request - Succeed
    //             Rollback                      [test_ApprovedTimeout_Rollback]
    @Test
    public void test_ApprovedTimeout_Rollback() throws Exception, RevivableThreadInterruptedException {
        if (this.serviceExists()) {
            ThreadLogger logger = new ThreadLogger();
            ConfigManager configManager = getConfigManager();
            URLCache.deleteCache(configManager, false);
            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());

            String urlStr = HTTPMOCKUP_SERVICE_URL + "?content=abcd";

            // Download the file for the 1st time
            File file1 = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, urlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // Commit (approve) the file
            URLCache.commitURLFile(configManager, file1, urlStr);

            // Set expiry to trigger the re-download
            URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), urlStr);
            cachedFile.setExpiry(0);
            Assert.assertEquals(0, cachedFile.getExpiry());


            // Re-request the file - the timeout has expired, it must re-download the file
            File file2 = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, urlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // Ensure the returned file is NOT the cached file
            Assert.assertFalse(file1.equals(file2));

            // Validate the downloaded info
            String content = readFile(file2);
            Assert.assertEquals("abcd", content);

            // Validate the cached info
            cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), urlStr);
            Assert.assertEquals(SC_OK, cachedFile.getHttpStatusCode());
            Assert.assertEquals(file1.getName(), cachedFile.getFilename());
            //Assert.assertEquals(URLCache.CACHE_TIMEOUT, cachedFile.getExpiry());
            Assert.assertTrue(cachedFile.hasTemporaryData());
            Assert.assertEquals(SC_OK, cachedFile.getTemporaryHttpStatusCode());
            Assert.assertEquals(file2.getName(), cachedFile.getTemporaryFilename());
            Assert.assertNull(cachedFile.getLatestErrorMessage());
            Assert.assertTrue(cachedFile.isApproved());

            File file3 = URLCache.rollbackURLFile(logger, "WMS GetCapabilities document", configManager, file2, urlStr, "New error message");

            // Ensure the returned file is the first one (proper rollback)
            Assert.assertEquals(file1, file3);

            Assert.assertEquals(SC_OK, cachedFile.getHttpStatusCode());
            Assert.assertEquals(file1.getName(), cachedFile.getFilename());
            Assert.assertEquals(URLCache.INVALID_FILE_CACHE_TIMEOUT, cachedFile.getExpiry());
            Assert.assertFalse(cachedFile.hasTemporaryData());
            Assert.assertNotNull(cachedFile.getLatestErrorMessage());
            Assert.assertTrue(cachedFile.isApproved());

            Assert.assertTrue(file1.exists());
            Assert.assertFalse(file2.exists());
            Assert.assertTrue(file3.exists());
            Assert.assertEquals(1, URLCache.countFile(getTestApplicationFolder()));

            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());
        }
    }

    // 1st request was approved
    //     After timeout (re-download)
    //         2nd request - Failed (an error occurred and null is returned)
    //             Commit                        [test_ApprovedTimeout_FailedCommit]
    @Test
    public void test_ApprovedTimeout_FailedCommit() throws Exception, RevivableThreadInterruptedException {
        if (this.serviceExists()) {
            ThreadLogger logger = new ThreadLogger();
            ConfigManager configManager = getConfigManager();
            URLCache.deleteCache(configManager, false);
            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());

            String validUrlStr = HTTPMOCKUP_SERVICE_URL + "?content=abcd";
            String invalidUrlStr = NONE_EXISTING_URL;

            // Download the file for the 1st time
            File file1 = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, validUrlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // Commit (approve) the file
            URLCache.commitURLFile(configManager, file1, validUrlStr);

            // Set expiry to trigger the re-download
            URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), validUrlStr);
            cachedFile.setExpiry(0);
            Assert.assertEquals(0, cachedFile.getExpiry());

            // The URL will always succeed. We need to change the URL in order to make the 2nd request fail.
            URLCache.getDiskCacheMap(getTestApplicationFolder()).put(invalidUrlStr, cachedFile.toJSON());


            // Re-request the file - the timeout has expired, it must re-download the file
            File file2 = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, invalidUrlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // The URLCache can not download the new file, so it automatically rollback and return the first file
            Assert.assertEquals(file1, file2);

            // Validate the cached info
            cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), invalidUrlStr);
            Assert.assertEquals(SC_OK, cachedFile.getHttpStatusCode());
            Assert.assertEquals(file1.getName(), cachedFile.getFilename());
            //assertEquals(URLCache.CACHE_TIMEOUT, cachedFile.getExpiry());
            Assert.assertFalse(cachedFile.hasTemporaryData());
            Assert.assertNotNull(cachedFile.getLatestErrorMessage());
            Assert.assertTrue(cachedFile.isApproved());

            URLCache.commitURLFile(configManager, file2, invalidUrlStr);

            Assert.assertEquals(SC_OK, cachedFile.getHttpStatusCode());
            Assert.assertEquals(file2.getName(), cachedFile.getFilename());
            Assert.assertEquals(URLCache.CACHE_TIMEOUT, cachedFile.getExpiry());
            Assert.assertFalse(cachedFile.hasTemporaryData());
            Assert.assertNotNull(cachedFile.getLatestErrorMessage());
            Assert.assertTrue(cachedFile.isApproved());

            Assert.assertTrue(file1.exists());
            Assert.assertTrue(file2.exists());
            Assert.assertEquals(1, URLCache.countFile(getTestApplicationFolder()));

            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());
        }
    }

    // 1st request was approved
    //     After timeout (re-download)
    //         2nd request - Failed (an error occurred and null is returned)
    //             Rollback                      [test_ApprovedTimeout_FailedRollback]
    @Test
    public void test_ApprovedTimeout_FailedRollback() throws Exception, RevivableThreadInterruptedException {
        if (this.serviceExists()) {
            ThreadLogger logger = new ThreadLogger();
            ConfigManager configManager = getConfigManager();
            URLCache.deleteCache(configManager, false);
            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());

            String validUrlStr = HTTPMOCKUP_SERVICE_URL + "?content=abcd";
            String invalidUrlStr = NONE_EXISTING_URL;

            // Download the file for the 1st time
            File file1 = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, validUrlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // Commit (approve) the file
            URLCache.commitURLFile(configManager, file1, validUrlStr);

            // Set expiry to trigger the re-download
            URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), validUrlStr);
            cachedFile.setExpiry(0);
            Assert.assertEquals(0, cachedFile.getExpiry());

            // The URL will always succeed. We need to change the URL in order to make the 2nd request fail.
            URLCache.getDiskCacheMap(getTestApplicationFolder()).put(invalidUrlStr, cachedFile.toJSON());


            // Re-request the file - the timeout has expired, it must re-download the file
            File file2 = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, invalidUrlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // The URLCache can not download the new file, so it automatically rollback and return the first file
            Assert.assertEquals(file1, file2);

            // Validate the cached info
            cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), invalidUrlStr);
            Assert.assertEquals(SC_OK, cachedFile.getHttpStatusCode());
            Assert.assertEquals(file1.getName(), cachedFile.getFilename());
            //Assert.assertEquals(URLCache.CACHE_TIMEOUT, cachedFile.getExpiry());
            Assert.assertFalse(cachedFile.hasTemporaryData());
            Assert.assertNotNull(cachedFile.getLatestErrorMessage());
            Assert.assertTrue(cachedFile.isApproved());

            File file3 = URLCache.rollbackURLFile(logger, "WMS GetCapabilities document", configManager, file2, invalidUrlStr, "New error message");

            // Ensure the returned file is the first one (proper rollback)
            Assert.assertEquals(file1, file3);

            Assert.assertEquals(SC_OK, cachedFile.getHttpStatusCode());
            Assert.assertEquals(file1.getName(), cachedFile.getFilename());
            Assert.assertEquals(URLCache.INVALID_FILE_CACHE_TIMEOUT, cachedFile.getExpiry());
            Assert.assertFalse(cachedFile.hasTemporaryData());
            Assert.assertNotNull(cachedFile.getLatestErrorMessage());
            Assert.assertTrue(cachedFile.isApproved());

            Assert.assertTrue(file1.exists());
            // File2 exists even after rollback, since it's the same file as file1
            Assert.assertTrue(file2.exists());
            Assert.assertTrue(file3.exists());
            Assert.assertEquals(1, URLCache.countFile(getTestApplicationFolder()));

            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());
        }
    }




    // 1st request was NOT approved
    //     Before timeout (should return what is in the cache, no re-download)
    //         2nd request (cached) - Commit     [test_NotApproved_Commit]
    @Test
    public void test_NotApproved_Commit() throws Exception, RevivableThreadInterruptedException {
        if (this.serviceExists()) {
            ThreadLogger logger = new ThreadLogger();
            ConfigManager configManager = getConfigManager();
            URLCache.deleteCache(configManager, false);
            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());

            String urlStr = HTTPMOCKUP_SERVICE_URL + "?content=abcd";

            // Download the file for the 1st time
            File file1 = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, urlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // Rollback (refuse) the file
            File file2 = URLCache.rollbackURLFile(logger, "WMS GetCapabilities document", configManager, file1, urlStr, "New error message");

            // Ensure the returned file is the cached file
            Assert.assertNull(file2);


            // Re-request the file - the timeout hasn't expired, it must give the same cached file
            File file3 = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, urlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // Ensure the returned file is the cached file (refused => null)
            Assert.assertNull(file3);

            // Validate the cached info
            URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), urlStr);
            Assert.assertEquals(SC_OK, cachedFile.getHttpStatusCode());
            //Assert.assertEquals(file1.getName(), cachedFile.getFilename());
            Assert.assertEquals(URLCache.INVALID_FILE_CACHE_TIMEOUT, cachedFile.getExpiry());
            Assert.assertNotNull(cachedFile.getLatestErrorMessage());
            Assert.assertFalse(cachedFile.isApproved());

            URLCache.commitURLFile(configManager, file3, urlStr);

            Assert.assertEquals(SC_OK, cachedFile.getHttpStatusCode());
            //Assert.assertEquals(file3.getName(), cachedFile.getFilename());
            Assert.assertEquals(URLCache.CACHE_TIMEOUT, cachedFile.getExpiry());
            Assert.assertFalse(cachedFile.hasTemporaryData());
            Assert.assertNotNull(cachedFile.getLatestErrorMessage());
            Assert.assertTrue(cachedFile.isApproved());

            Assert.assertFalse(file1.exists());
            Assert.assertNull(file2);
            Assert.assertNull(file3);
            Assert.assertEquals(0, URLCache.countFile(getTestApplicationFolder()));

            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());
        }
    }

    // 1st request was NOT approved
    //     Before timeout (should return what is in the cache, no re-download)
    //         2nd request (cached) - Rollback   [test_NotApproved_Rollback]
    @Test
    public void test_NotApproved_Rollback() throws Exception, RevivableThreadInterruptedException {
        if (this.serviceExists()) {
            ThreadLogger logger = new ThreadLogger();
            ConfigManager configManager = getConfigManager();
            URLCache.deleteCache(configManager, false);
            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());

            String urlStr = HTTPMOCKUP_SERVICE_URL + "?content=abcd";

            // Download the file for the 1st time
            File file1 = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, urlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // Rollback (refuse) the file
            File file2 = URLCache.rollbackURLFile(logger, "WMS GetCapabilities document", configManager, file1, urlStr, "New error message");

            // Ensure the returned file is the cached file
            Assert.assertNull(file2);


            // Re-request the file - the timeout hasn't expired, it must give the same cached file
            File file3 = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, urlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // Ensure the returned file is the cached file (refused => null)
            Assert.assertNull(file3);

            // Validate the cached info
            URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), urlStr);
            Assert.assertEquals(SC_OK, cachedFile.getHttpStatusCode());
            //Assert.assertEquals(file1.getName(), cachedFile.getFilename());
            Assert.assertEquals(URLCache.INVALID_FILE_CACHE_TIMEOUT, cachedFile.getExpiry());
            Assert.assertNotNull(cachedFile.getLatestErrorMessage());
            Assert.assertFalse(cachedFile.isApproved());

            File file4 = URLCache.rollbackURLFile(logger, "WMS GetCapabilities document", configManager, file3, urlStr, "New error message");

            // Ensure the returned file is the cached file (refused => null)
            Assert.assertNull(file4);

            Assert.assertEquals(SC_OK, cachedFile.getHttpStatusCode());
            //Assert.assertEquals(file3.getName(), cachedFile.getFilename());
            Assert.assertEquals(URLCache.INVALID_FILE_CACHE_TIMEOUT, cachedFile.getExpiry());
            Assert.assertFalse(cachedFile.hasTemporaryData());
            Assert.assertNotNull(cachedFile.getLatestErrorMessage());
            Assert.assertFalse(cachedFile.isApproved());

            Assert.assertFalse(file1.exists());
            Assert.assertNull(file2);
            Assert.assertNull(file3);
            Assert.assertNull(file4);
            Assert.assertEquals(0, URLCache.countFile(getTestApplicationFolder()));

            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());
        }
    }

    // 1st request was NOT approved
    //     After timeout (re-download)
    //         2nd request - Succeed
    //             Commit                        [test_NotApprovedTimeout_Commit]
    @Test
    public void test_NotApprovedTimeout_Commit() throws Exception, RevivableThreadInterruptedException {
        if (this.serviceExists()) {
            ThreadLogger logger = new ThreadLogger();
            ConfigManager configManager = getConfigManager();
            URLCache.deleteCache(configManager, false);
            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());

            String urlStr = HTTPMOCKUP_SERVICE_URL + "?content=abcd";

            // Download the file for the 1st time
            File file1 = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, urlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // Rollback (refuse) the file
            File file2 = URLCache.rollbackURLFile(logger, "WMS GetCapabilities document", configManager, file1, urlStr, "New error message");

            // Ensure the returned file is the cached file
            Assert.assertNull(file2);

            // Set expiry to trigger the re-download
            URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), urlStr);
            cachedFile.setExpiry(0);
            Assert.assertEquals(0, cachedFile.getExpiry());


            // Re-request the file - the timeout has expired, it must re-download the file
            File file3 = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, urlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // Validate the downloaded info
            String content = readFile(file3);
            Assert.assertEquals("abcd", content);

            // Validate the cached info
            cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), urlStr);
            Assert.assertEquals(SC_OK, cachedFile.getHttpStatusCode());
            //Assert.assertEquals(file1.getName(), cachedFile.getFilename());
            //Assert.assertEquals(0, cachedFile.getExpiry());
            Assert.assertTrue(cachedFile.hasTemporaryData());
            Assert.assertEquals(SC_OK, cachedFile.getTemporaryHttpStatusCode());
            Assert.assertEquals(file3.getName(), cachedFile.getTemporaryFilename());
            Assert.assertNull(cachedFile.getLatestErrorMessage());
            Assert.assertFalse(cachedFile.isApproved());

            URLCache.commitURLFile(configManager, file3, urlStr);

            Assert.assertEquals(SC_OK, cachedFile.getHttpStatusCode());
            Assert.assertEquals(file3.getName(), cachedFile.getFilename());
            Assert.assertEquals(URLCache.CACHE_TIMEOUT, cachedFile.getExpiry());
            Assert.assertFalse(cachedFile.hasTemporaryData());
            Assert.assertNull(cachedFile.getLatestErrorMessage());
            Assert.assertTrue(cachedFile.isApproved());

            // File1 has been deleted, its name removed from the cache. Then an other file (file3)
            // has been downloaded with the same name as file1 (the application re-used file names).
            // Therefor, file1.exists() is true even if it has been deleted.
            //Assert.assertFalse(file1.exists());
            Assert.assertNull(file2);
            Assert.assertTrue(file3.exists());
            Assert.assertEquals(1, URLCache.countFile(getTestApplicationFolder()));

            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());
        }
    }

    // 1st request was NOT approved
    //     After timeout (re-download)
    //         2nd request - Succeed
    //             Rollback                      [test_NotApprovedTimeout_Rollback]
    @Test
    public void test_NotApprovedTimeout_Rollback() throws Exception, RevivableThreadInterruptedException {
        if (this.serviceExists()) {
            ThreadLogger logger = new ThreadLogger();
            ConfigManager configManager = getConfigManager();
            URLCache.deleteCache(configManager, false);
            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());

            String urlStr = HTTPMOCKUP_SERVICE_URL + "?content=abcd";

            // Download the file for the 1st time
            File file1 = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, urlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // Rollback (refuse) the file
            File file2 = URLCache.rollbackURLFile(logger, "WMS GetCapabilities document", configManager, file1, urlStr, "New error message");

            // Ensure the returned file is the cached file
            Assert.assertNull(file2);

            // Set expiry to trigger the re-download
            URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), urlStr);
            cachedFile.setExpiry(0);
            Assert.assertEquals(0, cachedFile.getExpiry());


            // Re-request the file - the timeout has expired, it must re-download the file
            File file3 = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, urlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // Validate the downloaded info
            String content = readFile(file3);
            Assert.assertEquals("abcd", content);

            // Validate the cached info
            cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), urlStr);
            Assert.assertEquals(SC_OK, cachedFile.getHttpStatusCode());
            //Assert.assertEquals(file1.getName(), cachedFile.getFilename());
            //Assert.assertEquals(0, cachedFile.getExpiry());
            Assert.assertTrue(cachedFile.hasTemporaryData());
            Assert.assertEquals(SC_OK, cachedFile.getTemporaryHttpStatusCode());
            Assert.assertEquals(file3.getName(), cachedFile.getTemporaryFilename());
            Assert.assertNull(cachedFile.getLatestErrorMessage());
            Assert.assertFalse(cachedFile.isApproved());

            File file4 = URLCache.rollbackURLFile(logger, "WMS GetCapabilities document", configManager, file3, urlStr, "New error message");

            Assert.assertEquals(SC_OK, cachedFile.getHttpStatusCode());
            Assert.assertNull(file4);
            Assert.assertEquals(URLCache.INVALID_FILE_CACHE_TIMEOUT, cachedFile.getExpiry());
            Assert.assertFalse(cachedFile.hasTemporaryData());
            Assert.assertNotNull(cachedFile.getLatestErrorMessage());
            Assert.assertFalse(cachedFile.isApproved());

            Assert.assertFalse(file1.exists());
            Assert.assertNull(file2);
            Assert.assertFalse(file3.exists());
            Assert.assertNull(file4);
            Assert.assertEquals(0, URLCache.countFile(getTestApplicationFolder()));

            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());
        }
    }

    // 1st request was NOT approved
    //     After timeout (re-download)
    //         2nd request - Failed (an error occurred and null is returned)
    //             Commit                        [test_NotApprovedTimeout_FailedCommit]
    @Test
    public void test_NotApprovedTimeout_FailedCommit() throws Exception, RevivableThreadInterruptedException {
        if (this.serviceExists()) {
            ThreadLogger logger = new ThreadLogger();
            ConfigManager configManager = getConfigManager();
            URLCache.deleteCache(configManager, false);
            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());

            String validUrlStr = HTTPMOCKUP_SERVICE_URL + "?content=abcd";
            String invalidUrlStr = NONE_EXISTING_URL;

            // Download the file for the 1st time
            File file1 = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, validUrlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // Rollback (refuse) the file
            File file2 = URLCache.rollbackURLFile(logger, "WMS GetCapabilities document", configManager, file1, validUrlStr, "New error message");

            // Ensure the returned file is the cached file
            Assert.assertNull(file2);

            // Set expiry to trigger the re-download
            URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), validUrlStr);
            cachedFile.setExpiry(0);
            Assert.assertEquals(0, cachedFile.getExpiry());

            // The URL will always succeed. We need to change the URL in order to make the 2nd request fail.
            URLCache.getDiskCacheMap(getTestApplicationFolder()).put(invalidUrlStr, cachedFile.toJSON());


            // Re-request the file - the timeout has expired, it must re-download the file
            File file3 = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, invalidUrlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // The URLCache can not download the new file, so it automatically rollback and return the first file
            Assert.assertNull(file3);

            // Validate the cached info
            cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), invalidUrlStr);
            Assert.assertEquals(SC_OK, cachedFile.getHttpStatusCode());
            //Assert.assertEquals(file1.getName(), cachedFile.getFilename());
            //Assert.assertEquals(0, cachedFile.getExpiry());
            //Assert.assertTrue(cachedFile.hasTemporaryData());
            //Assert.assertEquals(SC_OK, cachedFile.getTemporaryHttpStatusCode());
            //Assert.assertEquals(file3.getName(), cachedFile.getTemporaryFilename());
            Assert.assertNotNull(cachedFile.getLatestErrorMessage());
            Assert.assertFalse(cachedFile.isApproved());

            URLCache.commitURLFile(configManager, file3, invalidUrlStr);

            Assert.assertEquals(SC_OK, cachedFile.getHttpStatusCode());
            Assert.assertNull(cachedFile.getFilename());
            Assert.assertEquals(URLCache.CACHE_TIMEOUT, cachedFile.getExpiry());
            Assert.assertFalse(cachedFile.hasTemporaryData());
            Assert.assertNotNull(cachedFile.getLatestErrorMessage());
            Assert.assertTrue(cachedFile.isApproved());

            Assert.assertFalse(file1.exists());
            Assert.assertNull(file2);
            Assert.assertNull(file3);
            Assert.assertEquals(0, URLCache.countFile(getTestApplicationFolder()));

            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());
        }
    }

    // 1st request was NOT approved
    //     After timeout (re-download)
    //         2nd request - Failed (an error occurred and null is returned)
    //             Rollback                      [test_NotApprovedTimeout_FailedRollback]
    @Test
    public void test_NotApprovedTimeout_FailedRollback() throws Exception, RevivableThreadInterruptedException {
        if (this.serviceExists()) {
            ThreadLogger logger = new ThreadLogger();
            ConfigManager configManager = getConfigManager();
            URLCache.deleteCache(configManager, false);
            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());

            String validUrlStr = HTTPMOCKUP_SERVICE_URL + "?content=abcd";
            String invalidUrlStr = NONE_EXISTING_URL;

            // Download the file for the 1st time
            File file1 = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, validUrlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // Rollback (refuse) the file
            File file2 = URLCache.rollbackURLFile(logger, "WMS GetCapabilities document", configManager, file1, validUrlStr, "New error message");

            // Ensure the returned file is the cached file
            Assert.assertNull(file2);

            // Set expiry to trigger the re-download
            URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), validUrlStr);
            cachedFile.setExpiry(0);
            Assert.assertEquals(0, cachedFile.getExpiry());

            // The URL will always succeed. We need to change the URL in order to make the 2nd request fail.
            URLCache.getDiskCacheMap(getTestApplicationFolder()).put(invalidUrlStr, cachedFile.toJSON());


            // Re-request the file - the timeout has expired, it must re-download the file
            File file3 = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, invalidUrlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // The URLCache can not download the new file, so it automatically rollback and return the first file
            Assert.assertNull(file3);

            // Validate the cached info
            cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), invalidUrlStr);
            Assert.assertEquals(SC_OK, cachedFile.getHttpStatusCode());
            //Assert.assertEquals(file1.getName(), cachedFile.getFilename());
            //Assert.assertEquals(0, cachedFile.getExpiry());
            //Assert.assertTrue(cachedFile.hasTemporaryData());
            //Assert.assertEquals(SC_OK, cachedFile.getTemporaryHttpStatusCode());
            //Assert.assertEquals(file3.getName(), cachedFile.getTemporaryFilename());
            Assert.assertNotNull(cachedFile.getLatestErrorMessage());
            Assert.assertFalse(cachedFile.isApproved());

            File file4 = URLCache.rollbackURLFile(logger, "WMS GetCapabilities document", configManager, file3, invalidUrlStr, "New error message");

            // The URLCache can not download the new file, so it automatically rollback and return the first file
            Assert.assertNull(file3);

            Assert.assertEquals(SC_OK, cachedFile.getHttpStatusCode());
            Assert.assertNull(cachedFile.getFilename());
            Assert.assertEquals(URLCache.INVALID_FILE_CACHE_TIMEOUT, cachedFile.getExpiry());
            Assert.assertFalse(cachedFile.hasTemporaryData());
            Assert.assertNotNull(cachedFile.getLatestErrorMessage());
            Assert.assertFalse(cachedFile.isApproved());

            Assert.assertFalse(file1.exists());
            Assert.assertNull(file2);
            Assert.assertNull(file3);
            Assert.assertNull(file4);
            Assert.assertEquals(0, URLCache.countFile(getTestApplicationFolder()));

            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());
        }
    }




    // 1st request returned null
    //     Before timeout (should return what is in the cache, no re-download)
    //         2nd request (cached) - Commit     [test_Null_Commit]
    @Test
    public void test_Null_Commit() throws Exception, RevivableThreadInterruptedException {
        if (this.serviceExists()) {
            ThreadLogger logger = new ThreadLogger();
            ConfigManager configManager = getConfigManager();
            URLCache.deleteCache(configManager, false);
            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());

            String urlStr = NONE_EXISTING_URL;

            // Download the file for the 1st time
            File file1 = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, urlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // Ensure the returned file is null
            Assert.assertNull(file1);

            // Rollback (refuse) the file
            File file2 = URLCache.rollbackURLFile(logger, "WMS GetCapabilities document", configManager, file1, urlStr, "New error message");

            // Ensure the returned file is the cached file
            Assert.assertNull(file2);


            // Re-request the file - the timeout hasn't expired, it must give the same cached file
            File file3 = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, urlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // Ensure the returned file is the cached file (refused => null)
            Assert.assertNull(file3);

            // Validate the cached info
            URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), urlStr);
            Assert.assertNull(cachedFile.getHttpStatusCode());
            //Assert.assertEquals(file1.getName(), cachedFile.getFilename());
            Assert.assertEquals(URLCache.INVALID_FILE_CACHE_TIMEOUT, cachedFile.getExpiry());
            Assert.assertNotNull(cachedFile.getLatestErrorMessage());
            Assert.assertFalse(cachedFile.isApproved());

            URLCache.commitURLFile(configManager, file3, urlStr);

            Assert.assertNull(cachedFile.getHttpStatusCode());
            //Assert.assertEquals(file3.getName(), cachedFile.getFilename());
            Assert.assertEquals(URLCache.CACHE_TIMEOUT, cachedFile.getExpiry());
            Assert.assertFalse(cachedFile.hasTemporaryData());
            Assert.assertNotNull(cachedFile.getLatestErrorMessage());
            Assert.assertTrue(cachedFile.isApproved());

            Assert.assertNull(file1);
            Assert.assertNull(file2);
            Assert.assertNull(file3);
            Assert.assertEquals(0, URLCache.countFile(getTestApplicationFolder()));

            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());
        }
    }

    // 1st request returned null
    //     Before timeout (should return what is in the cache, no re-download)
    //         2nd request (cached) - Rollback   [test_Null_Rollback]
    @Test
    public void test_Null_Rollback() throws Exception, RevivableThreadInterruptedException {
        if (this.serviceExists()) {
            ThreadLogger logger = new ThreadLogger();
            ConfigManager configManager = getConfigManager();
            URLCache.deleteCache(configManager, false);
            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());

            String urlStr = NONE_EXISTING_URL;

            // Download the file for the 1st time
            File file1 = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, urlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // Ensure the returned file is null
            Assert.assertNull(file1);

            // Rollback (refuse) the file
            File file2 = URLCache.rollbackURLFile(logger, "WMS GetCapabilities document", configManager, file1, urlStr, "New error message");

            // Ensure the returned file is the cached file
            Assert.assertNull(file2);


            // Re-request the file - the timeout hasn't expired, it must give the same cached file
            File file3 = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, urlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // Ensure the returned file is the cached file (refused => null)
            Assert.assertNull(file3);

            // Validate the cached info
            URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), urlStr);
            Assert.assertNull(cachedFile.getHttpStatusCode());
            //Assert.assertEquals(file1.getName(), cachedFile.getFilename());
            Assert.assertEquals(URLCache.INVALID_FILE_CACHE_TIMEOUT, cachedFile.getExpiry());
            Assert.assertNotNull(cachedFile.getLatestErrorMessage());
            Assert.assertFalse(cachedFile.isApproved());

            File file4 = URLCache.rollbackURLFile(logger, "WMS GetCapabilities document", configManager, file3, urlStr, "New error message");

            // Ensure the returned file is the cached file (refused => null)
            Assert.assertNull(file4);

            Assert.assertNull(cachedFile.getHttpStatusCode());
            //Assert.assertEquals(file3.getName(), cachedFile.getFilename());
            Assert.assertEquals(URLCache.INVALID_FILE_CACHE_TIMEOUT, cachedFile.getExpiry());
            Assert.assertFalse(cachedFile.hasTemporaryData());
            Assert.assertNotNull(cachedFile.getLatestErrorMessage());
            Assert.assertFalse(cachedFile.isApproved());

            Assert.assertNull(file1);
            Assert.assertNull(file2);
            Assert.assertNull(file3);
            Assert.assertNull(file4);
            Assert.assertEquals(0, URLCache.countFile(getTestApplicationFolder()));

            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());
        }
    }

    // 1st request returned null
    //     After timeout (re-download)
    //         2nd request - Succeed
    //             Commit                        [test_NullTimeout_Commit]
    @Test
    public void test_NullTimeout_Commit() throws Exception, RevivableThreadInterruptedException {
        if (this.serviceExists()) {
            ThreadLogger logger = new ThreadLogger();
            ConfigManager configManager = getConfigManager();
            URLCache.deleteCache(configManager, false);
            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());

            String validUrlStr = HTTPMOCKUP_SERVICE_URL + "?content=abcd";
            String invalidUrlStr = NONE_EXISTING_URL;

            // Download the file for the 1st time
            File file1 = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, invalidUrlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // Ensure the returned file is null
            Assert.assertNull(file1);

            // Rollback (refuse) the file
            File file2 = URLCache.rollbackURLFile(logger, "WMS GetCapabilities document", configManager, file1, invalidUrlStr, "New error message");

            // Ensure the returned file is the cached file
            Assert.assertNull(file2);

            // Set expiry to trigger the re-download
            URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), invalidUrlStr);
            cachedFile.setExpiry(0);
            Assert.assertEquals(0, cachedFile.getExpiry());

            // The URL will always fail. We need to change the URL in order to make the 2nd request succeed.
            URLCache.getDiskCacheMap(getTestApplicationFolder()).put(validUrlStr, cachedFile.toJSON());


            // Re-request the file - the timeout has expired, it must re-download the file
            File file3 = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, validUrlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // Validate the downloaded info
            String content = readFile(file3);
            Assert.assertEquals("abcd", content);

            // Validate the cached info
            cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), validUrlStr);
            Assert.assertNull(cachedFile.getHttpStatusCode());
            //Assert.assertEquals(file1.getName(), cachedFile.getFilename());
            //Assert.assertEquals(0, cachedFile.getExpiry());
            Assert.assertTrue(cachedFile.hasTemporaryData());
            Assert.assertEquals(SC_OK, cachedFile.getTemporaryHttpStatusCode());
            Assert.assertEquals(file3.getName(), cachedFile.getTemporaryFilename());
            Assert.assertNull(cachedFile.getLatestErrorMessage());
            Assert.assertFalse(cachedFile.isApproved());

            URLCache.commitURLFile(configManager, file3, validUrlStr);

            Assert.assertEquals(SC_OK, cachedFile.getHttpStatusCode());
            Assert.assertEquals(file3.getName(), cachedFile.getFilename());
            Assert.assertEquals(URLCache.CACHE_TIMEOUT, cachedFile.getExpiry());
            Assert.assertFalse(cachedFile.hasTemporaryData());
            Assert.assertNull(cachedFile.getLatestErrorMessage());
            Assert.assertTrue(cachedFile.isApproved());

            Assert.assertNull(file1);
            Assert.assertNull(file2);
            Assert.assertTrue(file3.exists());
            Assert.assertEquals(1, URLCache.countFile(getTestApplicationFolder()));

            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());
        }
    }

    // 1st request returned null
    //     After timeout (re-download)
    //         2nd request - Succeed
    //             Rollback                      [test_NullTimeout_Rollback]
    @Test
    public void test_NullTimeout_Rollback() throws Exception, RevivableThreadInterruptedException {
        if (this.serviceExists()) {
            ThreadLogger logger = new ThreadLogger();
            ConfigManager configManager = getConfigManager();
            URLCache.deleteCache(configManager, false);
            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());

            String validUrlStr = HTTPMOCKUP_SERVICE_URL + "?content=abcd";
            String invalidUrlStr = NONE_EXISTING_URL;

            // Download the file for the 1st time
            File file1 = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, invalidUrlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // Ensure the returned file is null
            Assert.assertNull(file1);

            // Rollback (refuse) the file
            File file2 = URLCache.rollbackURLFile(logger, "WMS GetCapabilities document", configManager, file1, invalidUrlStr, "New error message");

            // Ensure the returned file is the cached file
            Assert.assertNull(file2);

            // Set expiry to trigger the re-download
            URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), invalidUrlStr);
            cachedFile.setExpiry(0);
            Assert.assertEquals(0, cachedFile.getExpiry());

            // The URL will always fail. We need to change the URL in order to make the 2nd request succeed.
            URLCache.getDiskCacheMap(getTestApplicationFolder()).put(validUrlStr, cachedFile.toJSON());


            // Re-request the file - the timeout has expired, it must re-download the file
            File file3 = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, validUrlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // Validate the downloaded info
            String content = readFile(file3);
            Assert.assertEquals("abcd", content);

            // Validate the cached info
            cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), validUrlStr);
            Assert.assertNull(cachedFile.getHttpStatusCode());
            //Assert.assertEquals(file1.getName(), cachedFile.getFilename());
            //Assert.assertEquals(0, cachedFile.getExpiry());
            Assert.assertTrue(cachedFile.hasTemporaryData());
            Assert.assertEquals(SC_OK, cachedFile.getTemporaryHttpStatusCode());
            Assert.assertEquals(file3.getName(), cachedFile.getTemporaryFilename());
            Assert.assertNull(cachedFile.getLatestErrorMessage());
            Assert.assertFalse(cachedFile.isApproved());

            File file4 = URLCache.rollbackURLFile(logger, "WMS GetCapabilities document", configManager, file3, validUrlStr, "New error message");

            Assert.assertNull(cachedFile.getHttpStatusCode());
            Assert.assertNull(file4);
            Assert.assertEquals(URLCache.INVALID_FILE_CACHE_TIMEOUT, cachedFile.getExpiry());
            Assert.assertFalse(cachedFile.hasTemporaryData());
            Assert.assertNotNull(cachedFile.getLatestErrorMessage());
            Assert.assertFalse(cachedFile.isApproved());

            Assert.assertNull(file1);
            Assert.assertNull(file2);
            Assert.assertFalse(file3.exists());
            Assert.assertNull(file4);
            Assert.assertEquals(0, URLCache.countFile(getTestApplicationFolder()));

            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());
        }
    }

    // 1st request returned null
    //     After timeout (re-download)
    //         2nd request - Failed (an error occurred and null is returned)
    //             Commit                        [test_NullTimeout_FailedCommit]
    @Test
    public void test_NullTimeout_FailedCommit() throws Exception, RevivableThreadInterruptedException {
        if (this.serviceExists()) {
            ThreadLogger logger = new ThreadLogger();
            ConfigManager configManager = getConfigManager();
            URLCache.deleteCache(configManager, false);
            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());

            String urlStr = NONE_EXISTING_URL;

            // Download the file for the 1st time
            File file1 = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, urlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // Ensure the returned file is null
            Assert.assertNull(file1);

            // Rollback (refuse) the file
            File file2 = URLCache.rollbackURLFile(logger, "WMS GetCapabilities document", configManager, file1, urlStr, "New error message");

            // Ensure the returned file is the cached file
            Assert.assertNull(file2);

            // Set expiry to trigger the re-download
            URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), urlStr);
            cachedFile.setExpiry(0);
            Assert.assertEquals(0, cachedFile.getExpiry());


            // Re-request the file - the timeout has expired, it must re-download the file
            File file3 = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, urlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // The URLCache can not download the new file, so it automatically rollback and return the first file
            Assert.assertNull(file3);

            // Validate the cached info
            cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), urlStr);
            Assert.assertNull(cachedFile.getHttpStatusCode());
            //Assert.assertEquals(file1.getName(), cachedFile.getFilename());
            //Assert.assertEquals(0, cachedFile.getExpiry());
            //Assert.assertTrue(cachedFile.hasTemporaryData());
            //Assert.assertEquals(SC_OK, cachedFile.getTemporaryHttpStatusCode());
            //Assert.assertEquals(file3.getName(), cachedFile.getTemporaryFilename());
            Assert.assertNotNull(cachedFile.getLatestErrorMessage());
            Assert.assertFalse(cachedFile.isApproved());

            URLCache.commitURLFile(configManager, file3, urlStr);

            Assert.assertNull(cachedFile.getHttpStatusCode());
            Assert.assertNull(cachedFile.getFilename());
            Assert.assertEquals(URLCache.CACHE_TIMEOUT, cachedFile.getExpiry());
            Assert.assertFalse(cachedFile.hasTemporaryData());
            Assert.assertNotNull(cachedFile.getLatestErrorMessage());
            Assert.assertTrue(cachedFile.isApproved());

            Assert.assertNull(file1);
            Assert.assertNull(file2);
            Assert.assertNull(file3);
            Assert.assertEquals(0, URLCache.countFile(getTestApplicationFolder()));

            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());
        }
    }

    // 1st request returned null
    //     After timeout (re-download)
    //         2nd request - Failed (an error occurred and null is returned)
    //             Rollback                      [test_NullTimeout_FailedRollback]
    @Test
    public void test_NullTimeout_FailedRollback() throws Exception, RevivableThreadInterruptedException {
        if (this.serviceExists()) {
            ThreadLogger logger = new ThreadLogger();
            ConfigManager configManager = getConfigManager();
            URLCache.deleteCache(configManager, false);
            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());

            String urlStr = NONE_EXISTING_URL;

            // Download the file for the 1st time
            File file1 = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, urlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // Ensure the returned file is null
            Assert.assertNull(file1);

            // Rollback (refuse) the file
            File file2 = URLCache.rollbackURLFile(logger, "WMS GetCapabilities document", configManager, file1, urlStr, "New error message");

            // Ensure the returned file is the cached file
            Assert.assertNull(file2);

            // Set expiry to trigger the re-download
            URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), urlStr);
            cachedFile.setExpiry(0);
            Assert.assertEquals(0, cachedFile.getExpiry());


            // Re-request the file - the timeout has expired, it must re-download the file
            File file3 = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, urlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // The URLCache can not download the new file, so it automatically rollback and return the first file
            Assert.assertNull(file3);

            // Validate the cached info
            cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), urlStr);
            Assert.assertNull(cachedFile.getHttpStatusCode());
            //Assert.assertEquals(file1.getName(), cachedFile.getFilename());
            //Assert.assertEquals(0, cachedFile.getExpiry());
            //Assert.assertTrue(cachedFile.hasTemporaryData());
            //Assert.assertEquals(SC_OK, cachedFile.getTemporaryHttpStatusCode());
            //Assert.assertEquals(file3.getName(), cachedFile.getTemporaryFilename());
            Assert.assertNotNull(cachedFile.getLatestErrorMessage());
            Assert.assertFalse(cachedFile.isApproved());

            File file4 = URLCache.rollbackURLFile(logger, "WMS GetCapabilities document", configManager, file3, urlStr, "New error message");

            Assert.assertNull(cachedFile.getHttpStatusCode());
            Assert.assertNull(cachedFile.getFilename());
            Assert.assertEquals(URLCache.INVALID_FILE_CACHE_TIMEOUT, cachedFile.getExpiry());
            Assert.assertFalse(cachedFile.hasTemporaryData());
            Assert.assertNotNull(cachedFile.getLatestErrorMessage());
            Assert.assertFalse(cachedFile.isApproved());

            Assert.assertNull(file1);
            Assert.assertNull(file2);
            Assert.assertNull(file3);
            Assert.assertNull(file4);
            Assert.assertEquals(0, URLCache.countFile(getTestApplicationFolder()));

            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());
        }
    }


    //     [testFailTooLongNoFileSize]
    //         Request file, it's now too large, return null, rollback. Ensure it do not download the whole thing.
    @Test
    public void testFailTooLongNoFileSize() throws Exception, RevivableThreadInterruptedException {
        if (this.serviceExists()) {
            ThreadLogger logger = new ThreadLogger();
            ConfigManager configManager = getConfigManager();
            URLCache.deleteCache(configManager, false);
            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());

            String hundredMbUrlStr = HTTPMOCKUP_SERVICE_URL + "?randomascii=" + (500 * 1024 * 1024);

            // Download the file for the 1st time
            File tooLarge = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, hundredMbUrlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);
            Assert.assertNull(tooLarge);

            File stillTooLarge = URLCache.rollbackURLFile(logger, "WMS GetCapabilities document", configManager, tooLarge, hundredMbUrlStr, "The file is too large.");
            Assert.assertNull(stillTooLarge);

            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());
        }
    }


    //     [testDownloadFailThenSucceed]
    //         Receive null at first request, rollback - timeout - receive something at 2nd request, commit
    @Test
    public void testDownloadFailThenSucceed() throws Exception, RevivableThreadInterruptedException {
        if (this.serviceExists()) {
            ThreadLogger logger = new ThreadLogger();
            ConfigManager configManager = getConfigManager();
            URLCache.deleteCache(configManager, false);
            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());


            String invalidUrlStr = NONE_EXISTING_URL;
            String capUrlStr = HTTPMOCKUP_SERVICE_URL + "?resource=getCapabilities.xml";

            // Download the file for the 1st time
            File nullCap = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, invalidUrlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);
            Assert.assertNull(nullCap);

            // Rollback (refuse) the file
            File stillNullCap = URLCache.rollbackURLFile(logger, "WMS GetCapabilities document", configManager, nullCap, invalidUrlStr, "The capabilities document is empty.");
            Assert.assertNull(stillNullCap);

            // Timeout
            URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), invalidUrlStr);
            Assert.assertNotNull(cachedFile.getLatestErrorMessage());
            cachedFile.setExpiry(0);
            Assert.assertEquals(0, cachedFile.getExpiry());

            // Change URL - duplicate the cache info to the valid URL
            URLCache.getDiskCacheMap(getTestApplicationFolder()).put(capUrlStr, cachedFile.toJSON());


            // Download the file for the 1st time
            File validCap = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, capUrlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // Validate the cached info
            cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), capUrlStr);
            Assert.assertNull(cachedFile.getHttpStatusCode());
            Assert.assertNull(cachedFile.getFilename());
            Assert.assertNull(cachedFile.getLatestErrorMessage());
            Assert.assertFalse(cachedFile.isApproved());

            URLCache.commitURLFile(configManager, validCap, capUrlStr);

            Assert.assertEquals(SC_OK, cachedFile.getHttpStatusCode());
            Assert.assertEquals(validCap.getName(), cachedFile.getFilename());
            Assert.assertEquals(URLCache.CACHE_TIMEOUT, cachedFile.getExpiry());
            Assert.assertFalse(cachedFile.hasTemporaryData());
            Assert.assertNull(cachedFile.getLatestErrorMessage());
            Assert.assertTrue(cachedFile.isApproved());

            Assert.assertNull(nullCap);
            Assert.assertNull(stillNullCap);
            Assert.assertTrue(validCap.exists());
            Assert.assertEquals(1, URLCache.countFile(getTestApplicationFolder()));

            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());
        }
    }

    //     [testSucceedThenTooLong]
    //         Request file, commit - timeout - request same file, it's now too large, return null, rollback
    // TODO REDO THIS TEST - LOGIC HAS CHANGED
    @Test
    public void testSucceedThenTooLong() throws Exception, RevivableThreadInterruptedException {
        if (this.serviceExists()) {
            ThreadLogger logger = new ThreadLogger();
            ConfigManager configManager = getConfigManager();
            URLCache.deleteCache(configManager, false);
            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());

            String capUrlStr = HTTPMOCKUP_SERVICE_URL + "?resource=getCapabilities.xml";
            String hundredMbUrlStr = HTTPMOCKUP_SERVICE_URL + "?randomascii=" + (100 * 1024 * 1024);

            // Download the file for the 1st time
            File validFile = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, capUrlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);
            Assert.assertNotNull(validFile);

            URLCache.commitURLFile(configManager, validFile, capUrlStr);

            // Timeout
            URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), capUrlStr);
            Assert.assertNull(cachedFile.getLatestErrorMessage());
            cachedFile.setExpiry(0);
            Assert.assertEquals(0, cachedFile.getExpiry());

            // Change URL - duplicate the cache info to the valid URL
            URLCache.getDiskCacheMap(getTestApplicationFolder()).put(hundredMbUrlStr, cachedFile.toJSON());


            // The file is too big, auto-rollback, return previous file.
            File previousFile = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, hundredMbUrlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);
            Assert.assertEquals(validFile, previousFile);

            Assert.assertEquals(SC_OK, cachedFile.getHttpStatusCode());
            Assert.assertEquals(previousFile.getName(), cachedFile.getFilename());
            //Assert.assertEquals(URLCache.INVALID_FILE_CACHE_TIMEOUT, cachedFile.getExpiry());
            Assert.assertFalse(cachedFile.hasTemporaryData());
            //Assert.assertNotNull(cachedFile.getLatestErrorMessage());
            Assert.assertTrue(cachedFile.isApproved());

            Assert.assertTrue(validFile.exists());
            Assert.assertTrue(previousFile.exists());
            Assert.assertEquals(1, URLCache.countFile(getTestApplicationFolder()));

            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());
        }
    }

    //     [testSucceedTwiceWithSameResult]
    //         Get XML GetCapabilities document, parse, commit - timeout - receive same doc after timeout, parse, commit
    @Test
    public void testSucceedTwiceWithSameResult() throws Exception, RevivableThreadInterruptedException {
        if (this.serviceExists()) {
            ThreadLogger logger = new ThreadLogger();
            ConfigManager configManager = getConfigManager();
            URLCache.deleteCache(configManager, false);
            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());

            String capUrlStr = HTTPMOCKUP_SERVICE_URL + "?resource=getCapabilities.xml";

            // Download the file for the 1st time
            WMSCapabilities cap = URLCache.getWMSCapabilitiesResponse(logger, configManager, "1.3.0", null, capUrlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);
            URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), capUrlStr);
            File capFile = cachedFile.getFile();

            Assert.assertNull(cachedFile.getLatestErrorMessage());
            Assert.assertNotNull(capFile);
            Assert.assertNotNull(cap);

            // Timeout
            cachedFile.setExpiry(0);
            Assert.assertEquals(0, cachedFile.getExpiry());

            // Download the file for the 2nd time
            WMSCapabilities newCap = URLCache.getWMSCapabilitiesResponse(logger, configManager, "1.3.0", null, capUrlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);
            cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), capUrlStr);
            File newCapFile = cachedFile.getFile();

            Assert.assertNull(cachedFile.getLatestErrorMessage());
            Assert.assertNotNull(newCapFile);
            Assert.assertNotNull(newCap);

            Assert.assertFalse(capFile.equals(newCapFile));

            Assert.assertFalse(capFile.exists());
            Assert.assertTrue(newCapFile.exists());
            Assert.assertEquals(1, URLCache.countFile(getTestApplicationFolder()));

            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());
        }
    }

    //     [testParseFailThenSucceed]
    //         Get XML GetCapabilities document with stacktrace, unable to parse, rollback - timeout - receive valid document, parse, commit
    @Test
    public void testParseFailThenSucceed() throws Exception, RevivableThreadInterruptedException {
        if (this.serviceExists()) {
            ThreadLogger logger = new ThreadLogger();
            ConfigManager configManager = getConfigManager();
            URLCache.deleteCache(configManager, false);
            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());

            String brokenCapUrlStr = HTTPMOCKUP_SERVICE_URL + "?resource=getBrokenCapabilities.xml";
            String capUrlStr = HTTPMOCKUP_SERVICE_URL + "?resource=getCapabilities.xml";

            // Download the file for the 1st time (the file is broken)
            WMSCapabilities brokenCap = URLCache.getWMSCapabilitiesResponse(logger, configManager, "1.3.0", null, brokenCapUrlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);
            URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), brokenCapUrlStr);
            File brokenCapFile = cachedFile.getFile();

            Assert.assertNotNull(cachedFile.getLatestErrorMessage());
            Assert.assertNull(brokenCapFile);
            Assert.assertNull(brokenCap);

            // Timeout
            cachedFile.setExpiry(0);
            Assert.assertEquals(0, cachedFile.getExpiry());

            // Change URL - duplicate the cache info to the valid URL
            URLCache.getDiskCacheMap(getTestApplicationFolder()).put(capUrlStr, cachedFile.toJSON());

            // Download the file for the 2nd time (not broken this time)
            WMSCapabilities newCap = URLCache.getWMSCapabilitiesResponse(logger, configManager, "1.3.0", null, capUrlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);
            cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), capUrlStr);
            File newCapFile = cachedFile.getFile();

            Assert.assertNull(cachedFile.getLatestErrorMessage());
            Assert.assertNotNull(newCapFile);
            Assert.assertNotNull(newCap);

            Assert.assertNull(brokenCapFile);
            Assert.assertTrue(newCapFile.exists());
            Assert.assertEquals(1, URLCache.countFile(getTestApplicationFolder()));

            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());
        }
    }

    //     [testSucceedThenParseFail]
    //         Get XML GetCapabilities document, parse, commit - timeout - receive document with stacktrace, unable to parse, rollback
    @Test
    public void testSucceedThenParseFail() throws Exception, RevivableThreadInterruptedException {
        if (this.serviceExists()) {
            ThreadLogger logger = new ThreadLogger();
            ConfigManager configManager = getConfigManager();
            URLCache.deleteCache(configManager, false);
            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());

            String brokenCapUrlStr = HTTPMOCKUP_SERVICE_URL + "?resource=getBrokenCapabilities.xml";
            String capUrlStr = HTTPMOCKUP_SERVICE_URL + "?resource=getCapabilities.xml";

            // Download the file for the 1st time
            WMSCapabilities cap = URLCache.getWMSCapabilitiesResponse(logger, configManager, "1.3.0", null, capUrlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);
            URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), capUrlStr);
            File capFile = cachedFile.getFile();

            Assert.assertNull(cachedFile.getLatestErrorMessage());
            Assert.assertNotNull(capFile);
            Assert.assertNotNull(cap);

            // Timeout
            cachedFile.setExpiry(0);
            Assert.assertEquals(0, cachedFile.getExpiry());

            // Change URL - duplicate the cache info to the valid URL
            URLCache.getDiskCacheMap(getTestApplicationFolder()).put(brokenCapUrlStr, cachedFile.toJSON());


            // Download the file for the 2nd time
            WMSCapabilities previousCap = URLCache.getWMSCapabilitiesResponse(logger, configManager, "1.3.0", null, brokenCapUrlStr, URLCache.Category.CAPABILITIES_DOCUMENT, false);
            cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), brokenCapUrlStr);
            File previousCapFile = cachedFile.getFile();

            Assert.assertNotNull(cachedFile.getLatestErrorMessage());
            Assert.assertNotNull(previousCapFile);
            Assert.assertNotNull(previousCap);

            Assert.assertEquals(capFile, previousCapFile);

            Assert.assertTrue(capFile.exists());
            Assert.assertTrue(previousCapFile.exists());
            Assert.assertEquals(1, URLCache.countFile(getTestApplicationFolder()));

            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());
        }
    }

    //     [testTC211ParseFailThenTC211Succeed]
    //         Request TC211 document, receive HTML, unable to parse, rollback - timeout - receive valid TC211 document, parse, commit
    @Test
    public void testTC211ParseFailThenTC211Succeed() throws Exception, RevivableThreadInterruptedException {
        if (this.serviceExists()) {
            ThreadLogger logger = new ThreadLogger();
            ConfigManager configManager = getConfigManager();
            URLCache.deleteCache(configManager, false);
            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());

            String htmlTC211UrlStr = HTTPMOCKUP_SERVICE_URL + "?resource=tc211.html";
            String xmlTC211UrlStr = HTTPMOCKUP_SERVICE_URL + "?resource=tc211.xml";

            TC211Document brokenTC211Document = TC211Parser.parseURL(logger, "WMS GetCapabilities document", configManager, null, new URL(htmlTC211UrlStr), false, true);
            URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), htmlTC211UrlStr);
            File brokenTC211File = cachedFile.getFile();

            Assert.assertNotNull(cachedFile.getLatestErrorMessage());
            Assert.assertNull(brokenTC211File);
            Assert.assertNull(brokenTC211Document);

            // Timeout
            cachedFile.setExpiry(0);
            Assert.assertEquals(0, cachedFile.getExpiry());

            // Change URL - duplicate the cache info to the valid URL
            URLCache.getDiskCacheMap(getTestApplicationFolder()).put(xmlTC211UrlStr, cachedFile.toJSON());


            // Download the file for the 2nd time (not broken this time)
            TC211Document tc211Document = TC211Parser.parseURL(logger, "WMS GetCapabilities document", configManager, null, new URL(xmlTC211UrlStr), false, true);
            cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), xmlTC211UrlStr);
            File tc211File = cachedFile.getFile();

            Assert.assertNull(cachedFile.getLatestErrorMessage());
            Assert.assertNotNull(tc211File);
            Assert.assertNotNull(tc211Document);

            Assert.assertNull(brokenTC211File);
            Assert.assertTrue(tc211File.exists());
            Assert.assertEquals(1, URLCache.countFile(getTestApplicationFolder()));

            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());
        }
    }

    //     [testTC211SucceedThenTC211ParseFail]
    //         Request TC211 document, parse, commit - timeout - receive HTML doc instead of TC211 document, unable to parse, rollback
    @Test
    public void testTC211SucceedThenTC211ParseFail() throws Exception, RevivableThreadInterruptedException {
        if (this.serviceExists()) {
            ThreadLogger logger = new ThreadLogger();
            ConfigManager configManager = getConfigManager();
            URLCache.deleteCache(configManager, false);
            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());

            String htmlTC211UrlStr = HTTPMOCKUP_SERVICE_URL + "?resource=tc211.html";
            String xmlTC211UrlStr = HTTPMOCKUP_SERVICE_URL + "?resource=tc211.xml";

            TC211Document tc211Document = TC211Parser.parseURL(logger, "WMS GetCapabilities document", configManager, null, new URL(xmlTC211UrlStr), false, true);
            URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), xmlTC211UrlStr);
            File tc211File = cachedFile.getFile();

            Assert.assertNull(cachedFile.getLatestErrorMessage());
            Assert.assertNotNull(tc211File);
            Assert.assertNotNull(tc211Document);

            // Timeout
            cachedFile.setExpiry(0);
            Assert.assertEquals(0, cachedFile.getExpiry());

            // Change URL - duplicate the cache info to the valid URL
            URLCache.getDiskCacheMap(getTestApplicationFolder()).put(htmlTC211UrlStr, cachedFile.toJSON());


            TC211Document previousTC211Document = TC211Parser.parseURL(logger, "WMS GetCapabilities document", configManager, null, new URL(htmlTC211UrlStr), false, true);
            cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), htmlTC211UrlStr);
            File previousTC211File = cachedFile.getFile();

            Assert.assertNotNull(cachedFile.getLatestErrorMessage());
            Assert.assertNotNull(previousTC211File);
            Assert.assertNotNull(previousTC211Document);

            Assert.assertEquals(tc211File, previousTC211File);

            Assert.assertTrue(tc211File.exists());
            Assert.assertTrue(previousTC211File.exists());
            Assert.assertEquals(1, URLCache.countFile(getTestApplicationFolder()));

            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());
        }
    }

    //     [testMultiUrl_Fail1_Success2_Fail1_WithRollback]
    //         URL1 return null, rollback - URL2 (same domain) return file, commit - request URL1 again (before timeout), expect null
    // NOTE: This test may fail if the URL Cache class do not remove the filename when the file is deleted or not created.
    @Test
    public void testMultiUrl_Fail1_Success2_Fail1_WithRollback() throws Exception, RevivableThreadInterruptedException {
        if (this.serviceExists()) {
            ThreadLogger logger = new ThreadLogger();
            ConfigManager configManager = getConfigManager();
            URLCache.deleteCache(configManager, false);
            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());

            String url1Str = NONE_EXISTING_URL;
            String url2Str = HTTPMOCKUP_SERVICE_URL + "?content=abcd";


            // *** DOWNLOAD URL 1 ***

            // Download the file for the 1st time
            File file1_1 = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, url1Str, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // Ensure the returned file is null
            Assert.assertNull(file1_1);

            // Rollback (refuse) the file
            File file1_2 = URLCache.rollbackURLFile(logger, "WMS GetCapabilities document", configManager, file1_1, url1Str, "Server not found");

            // Ensure the returned file is the cached file
            Assert.assertNull(file1_2);


            // *** DOWNLOAD URL 2 ***

            // Download the file for the 1st time
            File file2_1 = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, url2Str, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // Ensure the returned file is not null
            Assert.assertNotNull(file2_1);

            // Validate the downloaded info
            String content = readFile(file2_1);
            Assert.assertEquals("abcd", content);

            URLCache.commitURLFile(configManager, file2_1, url2Str);


            // *** DOWNLOAD URL 1 AGAIN ***

            // Download the file for the 1st time
            File file1_3 = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, url1Str, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // Ensure the returned file is null
            Assert.assertNull(file1_3);

            // Rollback (refuse) the file
            File file1_4 = URLCache.rollbackURLFile(logger, "WMS GetCapabilities document", configManager, file1_3, url1Str, "Server not found");

            // Ensure the returned file is the cached file
            Assert.assertNull(file1_4);


            Assert.assertNull(file1_1);
            Assert.assertNull(file1_2);
            Assert.assertNull(file1_3);
            Assert.assertNull(file1_4);
            Assert.assertTrue(file2_1.exists());
            Assert.assertEquals(1, URLCache.countFile(getTestApplicationFolder()));

            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());
        }
    }

    //     [testMultiUrl_Fail1_Success2_Fail1_WithCommit]
    //         URL1 return null, commit - URL2 (same domain) return file, commit - request URL1 again (before timeout), expect null
    // NOTE: This test may fail if the URL Cache class do not remove the filename when the file is deleted or not created.
    @Test
    public void testMultiUrl_Fail1_Success2_Fail1_WithCommit() throws Exception, RevivableThreadInterruptedException {
        if (this.serviceExists()) {
            ThreadLogger logger = new ThreadLogger();
            ConfigManager configManager = getConfigManager();
            URLCache.deleteCache(configManager, false);
            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());

            String url1Str = NONE_EXISTING_URL;
            String url2Str = HTTPMOCKUP_SERVICE_URL + "?content=abcd";


            // *** DOWNLOAD URL 1 ***

            // Download the file for the 1st time
            File file1_1 = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, url1Str, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // Ensure the returned file is null
            Assert.assertNull(file1_1);

            // Commit (accept) the file
            URLCache.commitURLFile(configManager, file1_1, url1Str);


            // *** DOWNLOAD URL 2 ***

            // Download the file for the 1st time
            File file2_1 = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, url2Str, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // Ensure the returned file is not null
            Assert.assertNotNull(file2_1);

            // Validate the downloaded info
            String content = readFile(file2_1);
            Assert.assertEquals("abcd", content);

            URLCache.commitURLFile(configManager, file2_1, url2Str);


            // *** DOWNLOAD URL 1 AGAIN ***

            // Download the file for the 1st time
            File file1_3 = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, null, url1Str, URLCache.Category.CAPABILITIES_DOCUMENT, false);

            // Ensure the returned file is null
            Assert.assertNull(file1_3);

            // Rollback (refuse) the file
            File file1_4 = URLCache.rollbackURLFile(logger, "WMS GetCapabilities document", configManager, file1_3, url1Str, "Server not found");

            // Ensure the returned file is the cached file
            Assert.assertNull(file1_4);


            Assert.assertNull(file1_1);
            Assert.assertNull(file1_3);
            Assert.assertNull(file1_4);
            Assert.assertTrue(file2_1.exists());
            Assert.assertEquals(1, URLCache.countFile(getTestApplicationFolder()));

            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());
        }
    }

    @Test
    public void testDeleteOldEntries() throws Exception, RevivableThreadInterruptedException {
        if (this.serviceExists()) {
            ThreadLogger logger = new ThreadLogger();
            ConfigManager configManager = getConfigManager();
            URLCache.deleteCache(configManager, false);
            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());

            JSONObject cache;
            URLCache.CachedFile entry;

            List<URLCache.Category> categories = new ArrayList<URLCache.Category>();
            categories.add(URLCache.Category.CAPABILITIES_DOCUMENT);
            categories.add(URLCache.Category.MEST_RECORD);
            categories.add(URLCache.Category.BRUTEFORCE_MEST_RECORD);

            WMSDataSourceConfig dataSource = new WMSDataSourceConfig(configManager);
            dataSource.setDataSourceId("test");

            // Document that will be remove
            String oldCapDocUrlStr = HTTPMOCKUP_SERVICE_URL + "?content=oldCapabilitiesDocument";
            // Document that will be added
            String newCapDocUrlStr = HTTPMOCKUP_SERVICE_URL + "?content=newCapabilitiesDocument";
            // Documents that will stay the same
            String capDocUrlStr = HTTPMOCKUP_SERVICE_URL + "?content=capabilitiesDocument";
            String mestRecord1UrlStr = HTTPMOCKUP_SERVICE_URL + "?content=mestRecord1";
            String mestRecord2UrlStr = HTTPMOCKUP_SERVICE_URL + "?content=mestRecord2";

            WMSDataSourceConfig otherDataSource = new WMSDataSourceConfig(configManager);
            otherDataSource.setDataSourceId("other");

            // Documents associated with an other data source - should stay untouched
            String otherCapDocUrlStr = HTTPMOCKUP_SERVICE_URL + "?content=otherCapabilitiesDocument";
            String otherMestRecord1UrlStr = HTTPMOCKUP_SERVICE_URL + "?content=otherMestRecord1";
            String otherMestRecord2UrlStr = HTTPMOCKUP_SERVICE_URL + "?content=otherMestRecord2";

            // Ensure the cache is empty at this point
            cache = URLCache.getDiskCacheMap(configManager.getApplicationFolder());
            Assert.assertEquals(cache.length(), 0);

            // Create some entries in the cache
            Date startDate1 = new Date();
            File otherCapabilitiesDoc = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, otherDataSource, otherCapDocUrlStr, URLCache.Category.CAPABILITIES_DOCUMENT, true);
            File otherMestRecord1 = URLCache.getURLFile(logger, "MEST record", configManager, otherDataSource, otherMestRecord1UrlStr, URLCache.Category.MEST_RECORD, false);
            File otherMestRecord2 = URLCache.getURLFile(logger, "MEST record", configManager, otherDataSource, otherMestRecord2UrlStr, URLCache.Category.BRUTEFORCE_MEST_RECORD, false);


            // Ensure that the cache now contains the 3 elements
            cache = URLCache.getDiskCacheMap(configManager.getApplicationFolder());
            Assert.assertEquals(cache.length(), 3);

            entry = new URLCache.CachedFile(otherCapabilitiesDoc, cache.optJSONObject(otherCapDocUrlStr));
            Assert.assertNotNull(entry.toJSON());
            Assert.assertTrue(entry.getLastAccessDate().getTime() >= startDate1.getTime());

            entry = new URLCache.CachedFile(otherMestRecord1, cache.optJSONObject(otherMestRecord1UrlStr));
            Assert.assertNotNull(entry.toJSON());
            Assert.assertTrue(entry.getLastAccessDate().getTime() >= startDate1.getTime());

            entry = new URLCache.CachedFile(otherMestRecord2, cache.optJSONObject(otherMestRecord2UrlStr));
            Assert.assertNotNull(entry.toJSON());
            Assert.assertTrue(entry.getLastAccessDate().getTime() >= startDate1.getTime());


            // Wait for at lease 1 ms, just to be sure the cache entries will be considered as been old.
            Thread.sleep(10);

            // Simulate a data source processing; download capabilities document (and other files) for a given data source and call the "deleteOldEntries" method.
            Date startDate2 = new Date();
            File oldCapabilitiesDoc = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, dataSource, oldCapDocUrlStr, URLCache.Category.CAPABILITIES_DOCUMENT, true);
            File capabilitiesDoc = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, dataSource, capDocUrlStr, URLCache.Category.CAPABILITIES_DOCUMENT, true);
            File mestRecord1 = URLCache.getURLFile(logger, "MEST record", configManager, dataSource, mestRecord1UrlStr, URLCache.Category.MEST_RECORD, false);
            File mestRecord2 = URLCache.getURLFile(logger, "MEST record", configManager, dataSource, mestRecord2UrlStr, URLCache.Category.BRUTEFORCE_MEST_RECORD, false);


            // Verify that all entries are in the cache
            cache = URLCache.getDiskCacheMap(configManager.getApplicationFolder());
            Assert.assertEquals(cache.length(), 7);

            entry = new URLCache.CachedFile(otherCapabilitiesDoc, cache.optJSONObject(otherCapDocUrlStr));
            Assert.assertNotNull(entry.toJSON());
            Assert.assertTrue(entry.getLastAccessDate().getTime() >= startDate1.getTime());
            Assert.assertTrue(entry.getLastAccessDate().getTime() < startDate2.getTime());

            entry = new URLCache.CachedFile(otherMestRecord1, cache.optJSONObject(otherMestRecord1UrlStr));
            Assert.assertNotNull(entry.toJSON());
            Assert.assertTrue(entry.getLastAccessDate().getTime() >= startDate1.getTime());
            Assert.assertTrue(entry.getLastAccessDate().getTime() < startDate2.getTime());

            entry = new URLCache.CachedFile(otherMestRecord2, cache.optJSONObject(otherMestRecord2UrlStr));
            Assert.assertNotNull(entry.toJSON());
            Assert.assertTrue(entry.getLastAccessDate().getTime() >= startDate1.getTime());
            Assert.assertTrue(entry.getLastAccessDate().getTime() < startDate2.getTime());

            entry = new URLCache.CachedFile(oldCapabilitiesDoc, cache.optJSONObject(oldCapDocUrlStr));
            Assert.assertNotNull(entry.toJSON());
            Assert.assertTrue(entry.getLastAccessDate().getTime() >= startDate2.getTime());

            entry = new URLCache.CachedFile(capabilitiesDoc, cache.optJSONObject(capDocUrlStr));
            Assert.assertNotNull(entry.toJSON());
            Assert.assertTrue(entry.getLastAccessDate().getTime() >= startDate2.getTime());

            entry = new URLCache.CachedFile(mestRecord1, cache.optJSONObject(mestRecord1UrlStr));
            Assert.assertNotNull(entry.toJSON());
            Assert.assertTrue(entry.getLastAccessDate().getTime() >= startDate2.getTime());

            entry = new URLCache.CachedFile(mestRecord2, cache.optJSONObject(mestRecord2UrlStr));
            Assert.assertNotNull(entry.toJSON());
            Assert.assertTrue(entry.getLastAccessDate().getTime() >= startDate2.getTime());


            URLCache.deleteOldEntries(dataSource, startDate2, categories);
            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());


            // Verify that all entries are still in the cache
            cache = URLCache.getDiskCacheMap(configManager.getApplicationFolder());
            Assert.assertEquals(cache.length(), 7);

            entry = new URLCache.CachedFile(otherCapabilitiesDoc, cache.optJSONObject(otherCapDocUrlStr));
            Assert.assertNotNull(entry.toJSON());
            Assert.assertTrue(entry.getLastAccessDate().getTime() >= startDate1.getTime());
            Assert.assertTrue(entry.getLastAccessDate().getTime() < startDate2.getTime());

            entry = new URLCache.CachedFile(otherMestRecord1, cache.optJSONObject(otherMestRecord1UrlStr));
            Assert.assertNotNull(entry.toJSON());
            Assert.assertTrue(entry.getLastAccessDate().getTime() >= startDate1.getTime());
            Assert.assertTrue(entry.getLastAccessDate().getTime() < startDate2.getTime());

            entry = new URLCache.CachedFile(otherMestRecord2, cache.optJSONObject(otherMestRecord2UrlStr));
            Assert.assertNotNull(entry.toJSON());
            Assert.assertTrue(entry.getLastAccessDate().getTime() >= startDate1.getTime());
            Assert.assertTrue(entry.getLastAccessDate().getTime() < startDate2.getTime());

            entry = new URLCache.CachedFile(oldCapabilitiesDoc, cache.optJSONObject(oldCapDocUrlStr));
            Assert.assertNotNull(entry.toJSON());
            Assert.assertTrue(entry.getLastAccessDate().getTime() >= startDate2.getTime());

            entry = new URLCache.CachedFile(capabilitiesDoc, cache.optJSONObject(capDocUrlStr));
            Assert.assertNotNull(entry.toJSON());
            Assert.assertTrue(entry.getLastAccessDate().getTime() >= startDate2.getTime());

            entry = new URLCache.CachedFile(mestRecord1, cache.optJSONObject(mestRecord1UrlStr));
            Assert.assertNotNull(entry.toJSON());
            Assert.assertTrue(entry.getLastAccessDate().getTime() >= startDate2.getTime());

            entry = new URLCache.CachedFile(mestRecord2, cache.optJSONObject(mestRecord2UrlStr));
            Assert.assertNotNull(entry.toJSON());
            Assert.assertTrue(entry.getLastAccessDate().getTime() >= startDate2.getTime());


            // Wait for at lease 1 ms, just to be sure the next starting date will be newer than the previous one.
            Thread.sleep(10);

            // Change the URL of the capabilities document and repeat the simulation. The "deleteOldEntries" method should delete the old capability document since it hasn't been accessed during the last simulation.
            Date startDate3 = new Date();
            File newCapabilitiesDoc = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, dataSource, newCapDocUrlStr, URLCache.Category.CAPABILITIES_DOCUMENT, true);
            capabilitiesDoc = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, dataSource, capDocUrlStr, URLCache.Category.CAPABILITIES_DOCUMENT, true);
            mestRecord1 = URLCache.getURLFile(logger, "MEST record", configManager, dataSource, mestRecord1UrlStr, URLCache.Category.MEST_RECORD, false);
            mestRecord2 = URLCache.getURLFile(logger, "MEST record", configManager, dataSource, mestRecord2UrlStr, URLCache.Category.BRUTEFORCE_MEST_RECORD, false);

            // At this point, the old and new data should be in the cache, including deprecated data.
            // The deprecated data do not need to be in the cache, so it's not needed to test the cache
            // state at this point. The important state is the one given after the next step.

            URLCache.deleteOldEntries(dataSource, startDate3, categories);
            URLCache.saveDiskCacheMap(configManager.getApplicationFolder());

            // Verify that old entries still in used are still there, new entry as been added and old entry has been deleted.
            cache = URLCache.getDiskCacheMap(configManager.getApplicationFolder());
            Assert.assertEquals(cache.length(), 7);

            entry = new URLCache.CachedFile(otherCapabilitiesDoc, cache.optJSONObject(otherCapDocUrlStr));
            Assert.assertNotNull(entry.toJSON());
            Assert.assertTrue(entry.getLastAccessDate().getTime() >= startDate1.getTime());
            Assert.assertTrue(entry.getLastAccessDate().getTime() < startDate2.getTime());

            entry = new URLCache.CachedFile(otherMestRecord1, cache.optJSONObject(otherMestRecord1UrlStr));
            Assert.assertNotNull(entry.toJSON());
            Assert.assertTrue(entry.getLastAccessDate().getTime() >= startDate1.getTime());
            Assert.assertTrue(entry.getLastAccessDate().getTime() < startDate2.getTime());

            entry = new URLCache.CachedFile(otherMestRecord2, cache.optJSONObject(otherMestRecord2UrlStr));
            Assert.assertNotNull(entry.toJSON());
            Assert.assertTrue(entry.getLastAccessDate().getTime() >= startDate1.getTime());
            Assert.assertTrue(entry.getLastAccessDate().getTime() < startDate2.getTime());

            Assert.assertNull(cache.optJSONObject(oldCapDocUrlStr));

            entry = new URLCache.CachedFile(newCapabilitiesDoc, cache.optJSONObject(newCapDocUrlStr));
            Assert.assertNotNull(entry.toJSON());
            Assert.assertTrue(entry.getLastAccessDate().getTime() >= startDate3.getTime());

            entry = new URLCache.CachedFile(capabilitiesDoc, cache.optJSONObject(capDocUrlStr));
            Assert.assertNotNull(entry.toJSON());
            Assert.assertTrue(entry.getLastAccessDate().getTime() >= startDate3.getTime());

            entry = new URLCache.CachedFile(mestRecord1, cache.optJSONObject(mestRecord1UrlStr));
            Assert.assertNotNull(entry.toJSON());
            Assert.assertTrue(entry.getLastAccessDate().getTime() >= startDate3.getTime());

            entry = new URLCache.CachedFile(mestRecord2, cache.optJSONObject(mestRecord2UrlStr));
            Assert.assertNotNull(entry.toJSON());
            Assert.assertTrue(entry.getLastAccessDate().getTime() >= startDate3.getTime());
        }
    }




    /**
     * This method show an error message when the HttpMockup service is not installed.
     * It's to prevent the tests to run and systematically failed, preventing the compilation.
     * Those tests ensure the stability of the URLCache, but they should not be mandatory
     * for compilation; otherwise they will end-up commented out by developers that
     * do not want to (or understand how to) set-up the HttpMockup service.
     * @return True if the HttpMockup application is deployed and works as expected.
     */
    private boolean serviceExists() {
        if (this.serviceExists == null) {
            this.serviceExists = false;

            String testContent = "abcd";
            InputStream inputStream = null;

            boolean success = true;
            String errorMsg = null;
            try {
                URL url = new URL(HTTPMOCKUP_SERVICE_URL + "?content=" + testContent);
                URLConnection con = url.openConnection();
                if (con == null) {
                    errorMsg = "[1] Can not open connection";
                    success = false;
                } else {

                    inputStream = con.getInputStream();
                    if (inputStream == null) {
                        errorMsg = "[2] Can not open URL input stream";
                        success = false;
                    } else {

                        byte[] bytes = new byte[1024];
                        int nbRead = inputStream.read(bytes);

                        if (nbRead <= 0) {
                            errorMsg = "[3] Can not read Web page bytes";
                            success = false;
                        } else {

                            String foundContent = new String(bytes, 0, nbRead);
                            if (!testContent.equals(foundContent)) {
                                errorMsg = "[4] Content missmatch. Expected ["+testContent+"] but was ["+foundContent+"]";
                                success = false;
                            }
                        }
                    }
                }

                this.serviceExists = success;
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Unexpected error occurred while testing the URLCache: {0}", Utils.getExceptionMessage(ex));
                LOGGER.log(Level.INFO, "Stack trace:", ex);
                if (errorMsg == null) {
                    errorMsg = Utils.getExceptionMessage(ex);
                }
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Exception ex) {
                        LOGGER.log(Level.SEVERE, "Can not close the input stream: {0}", Utils.getExceptionMessage(ex));
                        LOGGER.log(Level.INFO, "Stack trace:", ex);
                    }
                }
            }

            if (!success) {
                if (Utils.isBlank(errorMsg)) {
                    errorMsg = "[5] Unexpected error";
                }
                LOGGER.log(Level.SEVERE, "\n" +
                        "**************************************************************\n" +
                        "** URLCache tests can not be run, the HTTP Mockup application is not accessible. {0}\n" +
                        "**************************************************************", errorMsg);
            }
        }

        return this.serviceExists;
    }

    private static String readFile(File file) throws IOException {
        if (file == null || !file.exists()) {
            return null;
        }

        InputStream inputStream = null;
        ByteArrayOutputStream outputStream = null;
        String content = null;
        try {
            inputStream = new FileInputStream(file);
            outputStream = new ByteArrayOutputStream();
            Utils.binaryCopy(inputStream, outputStream);
            content = outputStream.toString("UTF-8");
        } finally {
            // Both stream will always be closed, even if the first close crash.
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } finally {
                if (outputStream != null) {
                    outputStream.close();
                }
            }
        }

        return content;
    }

    private static File getTestApplicationFolder() {
        String tmpFolderPath = System.getProperty("java.io.tmpdir");
        File tmpFolder = new File(tmpFolderPath);

        if (!tmpFolder.exists() || !tmpFolder.canWrite()) {
            Assert.fail("The temporary folder \"" + tmpFolderPath + "\" is needed for this test but it is not accessible");
        }

        return tmpFolder;
    }

    private ConfigManager getConfigManager() {
        ConfigManager configManager = new ConfigManager(null, null);
        configManager.setApplicationFolder(getTestApplicationFolder());

        return configManager;
    }
}
