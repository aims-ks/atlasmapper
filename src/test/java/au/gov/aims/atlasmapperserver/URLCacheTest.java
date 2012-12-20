/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2012 Australian Institute of Marine Science
 *
 *  Contact: Gael Lafond <g.lafond@aims.org.au>
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

/**
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

/**
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

import au.gov.aims.atlasmapperserver.xml.TC211.Document;
import au.gov.aims.atlasmapperserver.xml.TC211.Parser;
import junit.framework.TestCase;
import org.geotools.data.ows.WMSCapabilities;
import org.geotools.ows.ServiceException;
import org.json.JSONException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

public class URLCacheTest extends TestCase {
	private static final Logger LOGGER = Logger.getLogger(URLCacheTest.class.getName());
	private static final String HTTPMOCKUP_SERVICE_URL = "http://localhost:8080/httpmockup/";
	private static final String NONE_EXISTING_URL = "http://localhost:1/thisUrlDontExists/";

	private static final Integer SC_OK = 200;

	private Boolean serviceExists = null;


	// 1st request of a URL
	//     Succeed
	//         Commit                            [test_New_Commit]
	public void test_New_Commit() throws IOException, JSONException {
		if (this.serviceExists()) {
			URLCache.clearCache(getTestApplicationFolder());
			ConfigManager configManager = getConfigManager();

			String urlStr = HTTPMOCKUP_SERVICE_URL + "?content=abcd";

			File file = URLCache.getURLFile(configManager, null, urlStr, false);

			// Validate the downloaded info
			String content = readFile(file);
			assertEquals("abcd", content);

			// Validate the cached info
			URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), urlStr);
			assertEquals(SC_OK, cachedFile.getHttpStatusCode());
			assertEquals(file.getName(), cachedFile.getFilename());
			assertEquals(URLCache.CACHE_TIMEOUT, cachedFile.getExpiry());
			assertNull(cachedFile.getLatestErrorMessage());
			assertFalse(cachedFile.isApproved());

			URLCache.commitURLFile(configManager, file, urlStr);

			assertEquals(SC_OK, cachedFile.getHttpStatusCode());
			assertEquals(file.getName(), cachedFile.getFilename());
			assertEquals(URLCache.CACHE_TIMEOUT, cachedFile.getExpiry());
			assertFalse(cachedFile.hasTemporaryData());
			assertNull(cachedFile.getLatestErrorMessage());
			assertTrue(cachedFile.isApproved());

			assertTrue(file.exists());
			assertEquals(1, URLCache.countFile(getTestApplicationFolder()));
		}
	}

	// 1st request of a URL
	//     Succeed
	//         Rollback                          [test_New_Rollback]
	public void test_New_Rollback() throws IOException, JSONException {
		if (this.serviceExists()) {
			URLCache.clearCache(getTestApplicationFolder());
			ConfigManager configManager = getConfigManager();

			String urlStr = HTTPMOCKUP_SERVICE_URL + "?content=abcd";

			File file = URLCache.getURLFile(configManager, null, urlStr, false);

			// Validate the downloaded info
			String content = readFile(file);
			assertEquals("abcd", content);

			// Validate the cached info
			URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), urlStr);
			assertEquals(SC_OK, cachedFile.getHttpStatusCode());
			assertEquals(file.getName(), cachedFile.getFilename());
			assertEquals(URLCache.CACHE_TIMEOUT, cachedFile.getExpiry());
			assertNull(cachedFile.getLatestErrorMessage());
			assertFalse(cachedFile.isApproved());

			URLCache.rollbackURLFile(configManager, file, urlStr, "Invalid");

			assertEquals(SC_OK, cachedFile.getHttpStatusCode());
			assertNull(cachedFile.getFilename()); // Deleted file
			assertEquals(URLCache.INVALID_FILE_CACHE_TIMEOUT, cachedFile.getExpiry());
			assertFalse(cachedFile.hasTemporaryData());
			assertEquals("Invalid", cachedFile.getLatestErrorMessage());
			assertFalse(cachedFile.isApproved());

			assertFalse(file.exists());
			assertEquals(0, URLCache.countFile(getTestApplicationFolder()));
		}
	}

	// 1st request of a URL
	//     Failed (an error occurred and null is returned)
	//         Commit                            [test_New_FailedCommit]
	public void test_New_FailedCommit() throws IOException, JSONException {
		if (this.serviceExists()) {
			URLCache.clearCache(getTestApplicationFolder());
			ConfigManager configManager = getConfigManager();

			String urlStr = NONE_EXISTING_URL;

			File file = URLCache.getURLFile(configManager, null, urlStr, false);

			// Validate the downloaded info
			assertNull(file);

			// Validate the cached info
			URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), urlStr);
			assertNull(cachedFile.getHttpStatusCode()); // The server do not exists, therefor it didn't returned a status code.
			//assertEquals(file.getName(), cachedFile.getFilename());
			assertEquals(URLCache.INVALID_FILE_CACHE_TIMEOUT, cachedFile.getExpiry());
			assertNotNull(cachedFile.getLatestErrorMessage());
			assertFalse(cachedFile.isApproved());

			URLCache.commitURLFile(configManager, file, urlStr);

			assertNull(cachedFile.getHttpStatusCode());
			//assertEquals(file.getName(), cachedFile.getFilename());
			assertEquals(URLCache.CACHE_TIMEOUT, cachedFile.getExpiry());
			assertFalse(cachedFile.hasTemporaryData());
			assertNotNull(cachedFile.getLatestErrorMessage());
			assertTrue(cachedFile.isApproved());

			assertNull(file);
			assertEquals(0, URLCache.countFile(getTestApplicationFolder()));
		}
	}

	// 1st request of a URL
	//     Failed (an error occurred and null is returned)
	//         Rollback                          [test_New_FailedRollback]
	public void test_New_FailedRollback() throws IOException, JSONException {
		if (this.serviceExists()) {
			URLCache.clearCache(getTestApplicationFolder());
			ConfigManager configManager = getConfigManager();

			String urlStr = NONE_EXISTING_URL;

			File file = URLCache.getURLFile(configManager, null, urlStr, false);

			// Validate the downloaded info
			assertNull(file);

			// Validate the cached info
			URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), urlStr);
			assertNull(cachedFile.getHttpStatusCode()); // The server do not exists, therefor it didn't returned a status code.
			//assertEquals(file.getName(), cachedFile.getFilename());
			assertEquals(URLCache.INVALID_FILE_CACHE_TIMEOUT, cachedFile.getExpiry());
			assertNotNull(cachedFile.getLatestErrorMessage());
			assertFalse(cachedFile.isApproved());

			URLCache.rollbackURLFile(configManager, file, urlStr, "New error message");

			assertNull(cachedFile.getHttpStatusCode());
			//assertEquals(file.getName(), cachedFile.getFilename());
			assertEquals(URLCache.INVALID_FILE_CACHE_TIMEOUT, cachedFile.getExpiry());
			assertFalse(cachedFile.hasTemporaryData());
			assertNotNull(cachedFile.getLatestErrorMessage());
			assertFalse(cachedFile.isApproved());

			assertNull(file);
			assertEquals(0, URLCache.countFile(getTestApplicationFolder()));
		}
	}




	// 1st request was approved
	//     Before timeout (should return what is in the cache, no re-download)
	//         2nd request (cached) - Commit     [test_Approved_Commit]
	public void test_Approved_Commit() throws IOException, JSONException {
		if (this.serviceExists()) {
			URLCache.clearCache(getTestApplicationFolder());
			ConfigManager configManager = getConfigManager();

			String urlStr = HTTPMOCKUP_SERVICE_URL + "?content=abcd";

			// Download the file for the 1st time
			File file1 = URLCache.getURLFile(configManager, null, urlStr, false);

			// Commit (approve) the file
			URLCache.commitURLFile(configManager, file1, urlStr);


			// Re-request the file - the timeout hasn't expired, it must give the same cached file
			File file2 = URLCache.getURLFile(configManager, null, urlStr, false);

			// Ensure the returned file is the cached file
			assertEquals(file1, file2);

			// Validate the downloaded info
			String content = readFile(file2);
			assertEquals("abcd", content);

			// Validate the cached info
			URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), urlStr);
			assertEquals(SC_OK, cachedFile.getHttpStatusCode());
			assertEquals(file1.getName(), cachedFile.getFilename());
			assertEquals(URLCache.CACHE_TIMEOUT, cachedFile.getExpiry());
			assertNull(cachedFile.getLatestErrorMessage());
			assertTrue(cachedFile.isApproved());

			URLCache.commitURLFile(configManager, file2, urlStr);

			assertEquals(SC_OK, cachedFile.getHttpStatusCode());
			assertEquals(file2.getName(), cachedFile.getFilename());
			assertEquals(URLCache.CACHE_TIMEOUT, cachedFile.getExpiry());
			assertFalse(cachedFile.hasTemporaryData());
			assertNull(cachedFile.getLatestErrorMessage());
			assertTrue(cachedFile.isApproved());

			assertTrue(file1.exists());
			assertTrue(file2.exists());
			assertEquals(1, URLCache.countFile(getTestApplicationFolder()));
		}
	}

	// 1st request was approved
	//     Before timeout (should return what is in the cache, no re-download)
	//         2nd request (cached) - Rollback   [test_Approved_Rollback]
	public void test_Approved_Rollback() throws IOException, JSONException {
		if (this.serviceExists()) {
			URLCache.clearCache(getTestApplicationFolder());
			ConfigManager configManager = getConfigManager();

			String urlStr = HTTPMOCKUP_SERVICE_URL + "?content=abcd";

			// Download the file for the 1st time
			File file1 = URLCache.getURLFile(configManager, null, urlStr, false);

			// Commit (approve) the file
			URLCache.commitURLFile(configManager, file1, urlStr);


			// Re-request the file - the timeout hasn't expired, it must give the same cached file
			File file2 = URLCache.getURLFile(configManager, null, urlStr, false);

			// Ensure the returned file is the cached file
			assertEquals(file1, file2);

			// Validate the downloaded info
			String content = readFile(file2);
			assertEquals("abcd", content);

			// Validate the cached info
			URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), urlStr);
			assertEquals(SC_OK, cachedFile.getHttpStatusCode());
			assertEquals(file1.getName(), cachedFile.getFilename());
			assertEquals(URLCache.CACHE_TIMEOUT, cachedFile.getExpiry());
			assertNull(cachedFile.getLatestErrorMessage());
			assertTrue(cachedFile.isApproved());

			File file3 = URLCache.rollbackURLFile(configManager, file2, urlStr, "New error message");

			// Ensure the returned file is the first one (proper rollback)
			assertEquals(file1, file3);

			assertEquals(SC_OK, cachedFile.getHttpStatusCode());
			assertEquals(file1.getName(), cachedFile.getFilename());
			assertEquals(URLCache.INVALID_FILE_CACHE_TIMEOUT, cachedFile.getExpiry());
			assertFalse(cachedFile.hasTemporaryData());
			assertNotNull(cachedFile.getLatestErrorMessage());
			assertTrue(cachedFile.isApproved());

			assertTrue(file1.exists());
			// File2 exists even after rollback, since it's the same file as file1
			assertTrue(file2.exists());
			assertTrue(file3.exists());
			assertEquals(1, URLCache.countFile(getTestApplicationFolder()));
		}
	}

	// 1st request was approved
	//     After timeout (re-download)
	//         2nd request - Succeed
	//             Commit                        [test_ApprovedTimeout_Commit]
	public void test_ApprovedTimeout_Commit() throws IOException, JSONException {
		if (this.serviceExists()) {
			URLCache.clearCache(getTestApplicationFolder());
			ConfigManager configManager = getConfigManager();

			String urlStr = HTTPMOCKUP_SERVICE_URL + "?content=abcd";

			// Download the file for the 1st time
			File file1 = URLCache.getURLFile(configManager, null, urlStr, false);

			// Commit (approve) the file
			URLCache.commitURLFile(configManager, file1, urlStr);

			// Set expiry to trigger the re-download
			URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), urlStr);
			cachedFile.setExpiry(0);
			assertEquals(0, cachedFile.getExpiry());


			// Re-request the file - the timeout has expired, it must re-download the file
			File file2 = URLCache.getURLFile(configManager, null, urlStr, false);

			// Ensure the returned file is NOT the cached file
			assertFalse(file1.equals(file2));

			// Validate the downloaded info
			String content = readFile(file2);
			assertEquals("abcd", content);

			// Validate the cached info
			cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), urlStr);
			assertEquals(SC_OK, cachedFile.getHttpStatusCode());
			assertEquals(file1.getName(), cachedFile.getFilename());
			//assertEquals(URLCache.CACHE_TIMEOUT, cachedFile.getExpiry());
			assertTrue(cachedFile.hasTemporaryData());
			assertEquals(SC_OK, cachedFile.getTemporaryHttpStatusCode());
			assertEquals(file2.getName(), cachedFile.getTemporaryFilename());
			assertNull(cachedFile.getLatestErrorMessage());
			assertTrue(cachedFile.isApproved());

			URLCache.commitURLFile(configManager, file2, urlStr);

			assertEquals(SC_OK, cachedFile.getHttpStatusCode());
			assertEquals(file2.getName(), cachedFile.getFilename());
			assertEquals(URLCache.CACHE_TIMEOUT, cachedFile.getExpiry());
			assertFalse(cachedFile.hasTemporaryData());
			assertNull(cachedFile.getLatestErrorMessage());
			assertTrue(cachedFile.isApproved());

			assertFalse(file1.exists());
			assertTrue(file2.exists());
			assertEquals(1, URLCache.countFile(getTestApplicationFolder()));
		}
	}

	// 1st request was approved
	//     After timeout (re-download)
	//         2nd request - Succeed
	//             Rollback                      [test_ApprovedTimeout_Rollback]
	public void test_ApprovedTimeout_Rollback() throws IOException, JSONException {
		if (this.serviceExists()) {
			URLCache.clearCache(getTestApplicationFolder());
			ConfigManager configManager = getConfigManager();

			String urlStr = HTTPMOCKUP_SERVICE_URL + "?content=abcd";

			// Download the file for the 1st time
			File file1 = URLCache.getURLFile(configManager, null, urlStr, false);

			// Commit (approve) the file
			URLCache.commitURLFile(configManager, file1, urlStr);

			// Set expiry to trigger the re-download
			URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), urlStr);
			cachedFile.setExpiry(0);
			assertEquals(0, cachedFile.getExpiry());


			// Re-request the file - the timeout hasn't expired, it must give the same cached file
			File file2 = URLCache.getURLFile(configManager, null, urlStr, false);

			// Ensure the returned file is NOT the cached file
			assertFalse(file1.equals(file2));

			// Validate the downloaded info
			String content = readFile(file2);
			assertEquals("abcd", content);

			// Validate the cached info
			cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), urlStr);
			assertEquals(SC_OK, cachedFile.getHttpStatusCode());
			assertEquals(file1.getName(), cachedFile.getFilename());
			//assertEquals(URLCache.CACHE_TIMEOUT, cachedFile.getExpiry());
			assertTrue(cachedFile.hasTemporaryData());
			assertEquals(SC_OK, cachedFile.getTemporaryHttpStatusCode());
			assertEquals(file2.getName(), cachedFile.getTemporaryFilename());
			assertNull(cachedFile.getLatestErrorMessage());
			assertTrue(cachedFile.isApproved());

			File file3 = URLCache.rollbackURLFile(configManager, file2, urlStr, "New error message");

			// Ensure the returned file is the first one (proper rollback)
			assertEquals(file1, file3);

			assertEquals(SC_OK, cachedFile.getHttpStatusCode());
			assertEquals(file1.getName(), cachedFile.getFilename());
			assertEquals(URLCache.INVALID_FILE_CACHE_TIMEOUT, cachedFile.getExpiry());
			assertFalse(cachedFile.hasTemporaryData());
			assertNotNull(cachedFile.getLatestErrorMessage());
			assertTrue(cachedFile.isApproved());

			assertTrue(file1.exists());
			assertFalse(file2.exists());
			assertTrue(file3.exists());
			assertEquals(1, URLCache.countFile(getTestApplicationFolder()));
		}
	}

	// 1st request was approved
	//     After timeout (re-download)
	//         2nd request - Failed (an error occurred and null is returned)
	//             Commit                        [test_ApprovedTimeout_FailedCommit]
	public void test_ApprovedTimeout_FailedCommit() throws IOException, JSONException {
		if (this.serviceExists()) {
			URLCache.clearCache(getTestApplicationFolder());
			ConfigManager configManager = getConfigManager();

			String validUrlStr = HTTPMOCKUP_SERVICE_URL + "?content=abcd";
			String invalidUrlStr = NONE_EXISTING_URL;

			// Download the file for the 1st time
			File file1 = URLCache.getURLFile(configManager, null, validUrlStr, false);

			// Commit (approve) the file
			URLCache.commitURLFile(configManager, file1, validUrlStr);

			// Set expiry to trigger the re-download
			URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), validUrlStr);
			cachedFile.setExpiry(0);
			assertEquals(0, cachedFile.getExpiry());

			// The URL will always succeed. We need to change the URL in order to make the 2nd request fail.
			URLCache.diskCacheMap.put(invalidUrlStr, cachedFile.toJSON());


			// Re-request the file - the timeout has expired, it must re-download the file
			File file2 = URLCache.getURLFile(configManager, null, invalidUrlStr, false);

			// The URLCache can not download the new file, so it automatically rollback and return the first file
			assertEquals(file1, file2);

			// Validate the cached info
			cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), invalidUrlStr);
			assertEquals(SC_OK, cachedFile.getHttpStatusCode());
			assertEquals(file1.getName(), cachedFile.getFilename());
			//assertEquals(URLCache.CACHE_TIMEOUT, cachedFile.getExpiry());
			assertFalse(cachedFile.hasTemporaryData());
			assertNotNull(cachedFile.getLatestErrorMessage());
			assertTrue(cachedFile.isApproved());

			URLCache.commitURLFile(configManager, file2, invalidUrlStr);

			assertEquals(SC_OK, cachedFile.getHttpStatusCode());
			assertEquals(file2.getName(), cachedFile.getFilename());
			assertEquals(URLCache.CACHE_TIMEOUT, cachedFile.getExpiry());
			assertFalse(cachedFile.hasTemporaryData());
			assertNotNull(cachedFile.getLatestErrorMessage());
			assertTrue(cachedFile.isApproved());

			assertTrue(file1.exists());
			assertTrue(file2.exists());
			assertEquals(1, URLCache.countFile(getTestApplicationFolder()));
		}
	}

	// 1st request was approved
	//     After timeout (re-download)
	//         2nd request - Failed (an error occurred and null is returned)
	//             Rollback                      [test_ApprovedTimeout_FailedRollback]
	public void test_ApprovedTimeout_FailedRollback() throws IOException, JSONException {
		if (this.serviceExists()) {
			URLCache.clearCache(getTestApplicationFolder());
			ConfigManager configManager = getConfigManager();

			String validUrlStr = HTTPMOCKUP_SERVICE_URL + "?content=abcd";
			String invalidUrlStr = NONE_EXISTING_URL;

			// Download the file for the 1st time
			File file1 = URLCache.getURLFile(configManager, null, validUrlStr, false);

			// Commit (approve) the file
			URLCache.commitURLFile(configManager, file1, validUrlStr);

			// Set expiry to trigger the re-download
			URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), validUrlStr);
			cachedFile.setExpiry(0);
			assertEquals(0, cachedFile.getExpiry());

			// The URL will always succeed. We need to change the URL in order to make the 2nd request fail.
			URLCache.diskCacheMap.put(invalidUrlStr, cachedFile.toJSON());


			// Re-request the file - the timeout has expired, it must re-download the file
			File file2 = URLCache.getURLFile(configManager, null, invalidUrlStr, false);

			// The URLCache can not download the new file, so it automatically rollback and return the first file
			assertEquals(file1, file2);

			// Validate the cached info
			cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), invalidUrlStr);
			assertEquals(SC_OK, cachedFile.getHttpStatusCode());
			assertEquals(file1.getName(), cachedFile.getFilename());
			//assertEquals(URLCache.CACHE_TIMEOUT, cachedFile.getExpiry());
			assertFalse(cachedFile.hasTemporaryData());
			assertNotNull(cachedFile.getLatestErrorMessage());
			assertTrue(cachedFile.isApproved());

			File file3 = URLCache.rollbackURLFile(configManager, file2, invalidUrlStr, "New error message");

			// Ensure the returned file is the first one (proper rollback)
			assertEquals(file1, file3);

			assertEquals(SC_OK, cachedFile.getHttpStatusCode());
			assertEquals(file1.getName(), cachedFile.getFilename());
			assertEquals(URLCache.INVALID_FILE_CACHE_TIMEOUT, cachedFile.getExpiry());
			assertFalse(cachedFile.hasTemporaryData());
			assertNotNull(cachedFile.getLatestErrorMessage());
			assertTrue(cachedFile.isApproved());

			assertTrue(file1.exists());
			// File2 exists even after rollback, since it's the same file as file1
			assertTrue(file2.exists());
			assertTrue(file3.exists());
			assertEquals(1, URLCache.countFile(getTestApplicationFolder()));
		}
	}




	// 1st request was NOT approved
	//     Before timeout (should return what is in the cache, no re-download)
	//         2nd request (cached) - Commit     [test_NotApproved_Commit]
	public void test_NotApproved_Commit() throws IOException, JSONException {
		if (this.serviceExists()) {
			URLCache.clearCache(getTestApplicationFolder());
			ConfigManager configManager = getConfigManager();

			String urlStr = HTTPMOCKUP_SERVICE_URL + "?content=abcd";

			// Download the file for the 1st time
			File file1 = URLCache.getURLFile(configManager, null, urlStr, false);

			// Rollback (refuse) the file
			File file2 = URLCache.rollbackURLFile(configManager, file1, urlStr, "New error message");

			// Ensure the returned file is the cached file
			assertNull(file2);


			// Re-request the file - the timeout hasn't expired, it must give the same cached file
			File file3 = URLCache.getURLFile(configManager, null, urlStr, false);

			// Ensure the returned file is the cached file (refused => null)
			assertNull(file3);

			// Validate the cached info
			URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), urlStr);
			assertEquals(SC_OK, cachedFile.getHttpStatusCode());
			//assertEquals(file1.getName(), cachedFile.getFilename());
			assertEquals(URLCache.INVALID_FILE_CACHE_TIMEOUT, cachedFile.getExpiry());
			assertNotNull(cachedFile.getLatestErrorMessage());
			assertFalse(cachedFile.isApproved());

			URLCache.commitURLFile(configManager, file3, urlStr);

			assertEquals(SC_OK, cachedFile.getHttpStatusCode());
			//assertEquals(file3.getName(), cachedFile.getFilename());
			assertEquals(URLCache.CACHE_TIMEOUT, cachedFile.getExpiry());
			assertFalse(cachedFile.hasTemporaryData());
			assertNotNull(cachedFile.getLatestErrorMessage());
			assertTrue(cachedFile.isApproved());

			assertFalse(file1.exists());
			assertNull(file2);
			assertNull(file3);
			assertEquals(0, URLCache.countFile(getTestApplicationFolder()));
		}
	}

	// 1st request was NOT approved
	//     Before timeout (should return what is in the cache, no re-download)
	//         2nd request (cached) - Rollback   [test_NotApproved_Rollback]
	public void test_NotApproved_Rollback() throws IOException, JSONException {
		if (this.serviceExists()) {
			URLCache.clearCache(getTestApplicationFolder());
			ConfigManager configManager = getConfigManager();

			String urlStr = HTTPMOCKUP_SERVICE_URL + "?content=abcd";

			// Download the file for the 1st time
			File file1 = URLCache.getURLFile(configManager, null, urlStr, false);

			// Rollback (refuse) the file
			File file2 = URLCache.rollbackURLFile(configManager, file1, urlStr, "New error message");

			// Ensure the returned file is the cached file
			assertNull(file2);


			// Re-request the file - the timeout hasn't expired, it must give the same cached file
			File file3 = URLCache.getURLFile(configManager, null, urlStr, false);

			// Ensure the returned file is the cached file (refused => null)
			assertNull(file3);

			// Validate the cached info
			URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), urlStr);
			assertEquals(SC_OK, cachedFile.getHttpStatusCode());
			//assertEquals(file1.getName(), cachedFile.getFilename());
			assertEquals(URLCache.INVALID_FILE_CACHE_TIMEOUT, cachedFile.getExpiry());
			assertNotNull(cachedFile.getLatestErrorMessage());
			assertFalse(cachedFile.isApproved());

			File file4 = URLCache.rollbackURLFile(configManager, file3, urlStr, "New error message");

			// Ensure the returned file is the cached file (refused => null)
			assertNull(file4);

			assertEquals(SC_OK, cachedFile.getHttpStatusCode());
			//assertEquals(file3.getName(), cachedFile.getFilename());
			assertEquals(URLCache.INVALID_FILE_CACHE_TIMEOUT, cachedFile.getExpiry());
			assertFalse(cachedFile.hasTemporaryData());
			assertNotNull(cachedFile.getLatestErrorMessage());
			assertFalse(cachedFile.isApproved());

			assertFalse(file1.exists());
			assertNull(file2);
			assertNull(file3);
			assertNull(file4);
			assertEquals(0, URLCache.countFile(getTestApplicationFolder()));
		}
	}

	// 1st request was NOT approved
	//     After timeout (re-download)
	//         2nd request - Succeed
	//             Commit                        [test_NotApprovedTimeout_Commit]
	public void test_NotApprovedTimeout_Commit() throws IOException, JSONException {
		if (this.serviceExists()) {
			URLCache.clearCache(getTestApplicationFolder());
			ConfigManager configManager = getConfigManager();

			String urlStr = HTTPMOCKUP_SERVICE_URL + "?content=abcd";

			// Download the file for the 1st time
			File file1 = URLCache.getURLFile(configManager, null, urlStr, false);

			// Rollback (refuse) the file
			File file2 = URLCache.rollbackURLFile(configManager, file1, urlStr, "New error message");

			// Ensure the returned file is the cached file
			assertNull(file2);

			// Set expiry to trigger the re-download
			URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), urlStr);
			cachedFile.setExpiry(0);
			assertEquals(0, cachedFile.getExpiry());


			// Re-request the file - the timeout has expired, it must re-download the file
			File file3 = URLCache.getURLFile(configManager, null, urlStr, false);

			// Validate the downloaded info
			String content = readFile(file3);
			assertEquals("abcd", content);

			// Validate the cached info
			cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), urlStr);
			assertEquals(SC_OK, cachedFile.getHttpStatusCode());
			//assertEquals(file1.getName(), cachedFile.getFilename());
			//assertEquals(0, cachedFile.getExpiry());
			assertTrue(cachedFile.hasTemporaryData());
			assertEquals(SC_OK, cachedFile.getTemporaryHttpStatusCode());
			assertEquals(file3.getName(), cachedFile.getTemporaryFilename());
			assertNull(cachedFile.getLatestErrorMessage());
			assertFalse(cachedFile.isApproved());

			URLCache.commitURLFile(configManager, file3, urlStr);

			assertEquals(SC_OK, cachedFile.getHttpStatusCode());
			assertEquals(file3.getName(), cachedFile.getFilename());
			assertEquals(URLCache.CACHE_TIMEOUT, cachedFile.getExpiry());
			assertFalse(cachedFile.hasTemporaryData());
			assertNull(cachedFile.getLatestErrorMessage());
			assertTrue(cachedFile.isApproved());

			// File1 has been deleted, its name removed from the cache. Then an other file (file3)
			// has been downloaded with the same name as file1 (the application re-used file names).
			// Therefor, file1.exists() is true even if it has been deleted.
			//assertFalse(file1.exists());
			assertNull(file2);
			assertTrue(file3.exists());
			assertEquals(1, URLCache.countFile(getTestApplicationFolder()));
		}
	}

	// 1st request was NOT approved
	//     After timeout (re-download)
	//         2nd request - Succeed
	//             Rollback                      [test_NotApprovedTimeout_Rollback]
	public void test_NotApprovedTimeout_Rollback() throws IOException, JSONException {
		if (this.serviceExists()) {
			URLCache.clearCache(getTestApplicationFolder());
			ConfigManager configManager = getConfigManager();

			String urlStr = HTTPMOCKUP_SERVICE_URL + "?content=abcd";

			// Download the file for the 1st time
			File file1 = URLCache.getURLFile(configManager, null, urlStr, false);

			// Rollback (refuse) the file
			File file2 = URLCache.rollbackURLFile(configManager, file1, urlStr, "New error message");

			// Ensure the returned file is the cached file
			assertNull(file2);

			// Set expiry to trigger the re-download
			URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), urlStr);
			cachedFile.setExpiry(0);
			assertEquals(0, cachedFile.getExpiry());


			// Re-request the file - the timeout has expired, it must re-download the file
			File file3 = URLCache.getURLFile(configManager, null, urlStr, false);

			// Validate the downloaded info
			String content = readFile(file3);
			assertEquals("abcd", content);

			// Validate the cached info
			cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), urlStr);
			assertEquals(SC_OK, cachedFile.getHttpStatusCode());
			//assertEquals(file1.getName(), cachedFile.getFilename());
			//assertEquals(0, cachedFile.getExpiry());
			assertTrue(cachedFile.hasTemporaryData());
			assertEquals(SC_OK, cachedFile.getTemporaryHttpStatusCode());
			assertEquals(file3.getName(), cachedFile.getTemporaryFilename());
			assertNull(cachedFile.getLatestErrorMessage());
			assertFalse(cachedFile.isApproved());

			File file4 = URLCache.rollbackURLFile(configManager, file3, urlStr, "New error message");

			assertEquals(SC_OK, cachedFile.getHttpStatusCode());
			assertNull(file4);
			assertEquals(URLCache.INVALID_FILE_CACHE_TIMEOUT, cachedFile.getExpiry());
			assertFalse(cachedFile.hasTemporaryData());
			assertNotNull(cachedFile.getLatestErrorMessage());
			assertFalse(cachedFile.isApproved());

			assertFalse(file1.exists());
			assertNull(file2);
			assertFalse(file3.exists());
			assertNull(file4);
			assertEquals(0, URLCache.countFile(getTestApplicationFolder()));
		}
	}

	// 1st request was NOT approved
	//     After timeout (re-download)
	//         2nd request - Failed (an error occurred and null is returned)
	//             Commit                        [test_NotApprovedTimeout_FailedCommit]
	public void test_NotApprovedTimeout_FailedCommit() throws IOException, JSONException {
		if (this.serviceExists()) {
			URLCache.clearCache(getTestApplicationFolder());
			ConfigManager configManager = getConfigManager();

			String validUrlStr = HTTPMOCKUP_SERVICE_URL + "?content=abcd";
			String invalidUrlStr = NONE_EXISTING_URL;

			// Download the file for the 1st time
			File file1 = URLCache.getURLFile(configManager, null, validUrlStr, false);

			// Rollback (refuse) the file
			File file2 = URLCache.rollbackURLFile(configManager, file1, validUrlStr, "New error message");

			// Ensure the returned file is the cached file
			assertNull(file2);

			// Set expiry to trigger the re-download
			URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), validUrlStr);
			cachedFile.setExpiry(0);
			assertEquals(0, cachedFile.getExpiry());

			// The URL will always succeed. We need to change the URL in order to make the 2nd request fail.
			URLCache.diskCacheMap.put(invalidUrlStr, cachedFile.toJSON());


			// Re-request the file - the timeout has expired, it must re-download the file
			File file3 = URLCache.getURLFile(configManager, null, invalidUrlStr, false);

			// The URLCache can not download the new file, so it automatically rollback and return the first file
			assertNull(file3);

			// Validate the cached info
			cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), invalidUrlStr);
			assertEquals(SC_OK, cachedFile.getHttpStatusCode());
			//assertEquals(file1.getName(), cachedFile.getFilename());
			//assertEquals(0, cachedFile.getExpiry());
			//assertTrue(cachedFile.hasTemporaryData());
			//assertEquals(SC_OK, cachedFile.getTemporaryHttpStatusCode());
			//assertEquals(file3.getName(), cachedFile.getTemporaryFilename());
			assertNotNull(cachedFile.getLatestErrorMessage());
			assertFalse(cachedFile.isApproved());

			URLCache.commitURLFile(configManager, file3, invalidUrlStr);

			assertEquals(SC_OK, cachedFile.getHttpStatusCode());
			assertNull(cachedFile.getFilename());
			assertEquals(URLCache.CACHE_TIMEOUT, cachedFile.getExpiry());
			assertFalse(cachedFile.hasTemporaryData());
			assertNotNull(cachedFile.getLatestErrorMessage());
			assertTrue(cachedFile.isApproved());

			assertFalse(file1.exists());
			assertNull(file2);
			assertNull(file3);
			assertEquals(0, URLCache.countFile(getTestApplicationFolder()));
		}
	}

	// 1st request was NOT approved
	//     After timeout (re-download)
	//         2nd request - Failed (an error occurred and null is returned)
	//             Rollback                      [test_NotApprovedTimeout_FailedRollback]
	public void test_NotApprovedTimeout_FailedRollback() throws IOException, JSONException {
		if (this.serviceExists()) {
			URLCache.clearCache(getTestApplicationFolder());
			ConfigManager configManager = getConfigManager();

			String validUrlStr = HTTPMOCKUP_SERVICE_URL + "?content=abcd";
			String invalidUrlStr = NONE_EXISTING_URL;

			// Download the file for the 1st time
			File file1 = URLCache.getURLFile(configManager, null, validUrlStr, false);

			// Rollback (refuse) the file
			File file2 = URLCache.rollbackURLFile(configManager, file1, validUrlStr, "New error message");

			// Ensure the returned file is the cached file
			assertNull(file2);

			// Set expiry to trigger the re-download
			URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), validUrlStr);
			cachedFile.setExpiry(0);
			assertEquals(0, cachedFile.getExpiry());

			// The URL will always succeed. We need to change the URL in order to make the 2nd request fail.
			URLCache.diskCacheMap.put(invalidUrlStr, cachedFile.toJSON());


			// Re-request the file - the timeout has expired, it must re-download the file
			File file3 = URLCache.getURLFile(configManager, null, invalidUrlStr, false);

			// The URLCache can not download the new file, so it automatically rollback and return the first file
			assertNull(file3);

			// Validate the cached info
			cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), invalidUrlStr);
			assertEquals(SC_OK, cachedFile.getHttpStatusCode());
			//assertEquals(file1.getName(), cachedFile.getFilename());
			//assertEquals(0, cachedFile.getExpiry());
			//assertTrue(cachedFile.hasTemporaryData());
			//assertEquals(SC_OK, cachedFile.getTemporaryHttpStatusCode());
			//assertEquals(file3.getName(), cachedFile.getTemporaryFilename());
			assertNotNull(cachedFile.getLatestErrorMessage());
			assertFalse(cachedFile.isApproved());

			File file4 = URLCache.rollbackURLFile(configManager, file3, invalidUrlStr, "New error message");

			// The URLCache can not download the new file, so it automatically rollback and return the first file
			assertNull(file3);

			assertEquals(SC_OK, cachedFile.getHttpStatusCode());
			assertNull(cachedFile.getFilename());
			assertEquals(URLCache.INVALID_FILE_CACHE_TIMEOUT, cachedFile.getExpiry());
			assertFalse(cachedFile.hasTemporaryData());
			assertNotNull(cachedFile.getLatestErrorMessage());
			assertFalse(cachedFile.isApproved());

			assertFalse(file1.exists());
			assertNull(file2);
			assertNull(file3);
			assertNull(file4);
			assertEquals(0, URLCache.countFile(getTestApplicationFolder()));
		}
	}




	// 1st request returned null
	//     Before timeout (should return what is in the cache, no re-download)
	//         2nd request (cached) - Commit     [test_Null_Commit]
	public void test_Null_Commit() throws IOException, JSONException {
		if (this.serviceExists()) {
			URLCache.clearCache(getTestApplicationFolder());
			ConfigManager configManager = getConfigManager();

			String urlStr = NONE_EXISTING_URL;

			// Download the file for the 1st time
			File file1 = URLCache.getURLFile(configManager, null, urlStr, false);

			// Ensure the returned file is null
			assertNull(file1);

			// Rollback (refuse) the file
			File file2 = URLCache.rollbackURLFile(configManager, file1, urlStr, "New error message");

			// Ensure the returned file is the cached file
			assertNull(file2);


			// Re-request the file - the timeout hasn't expired, it must give the same cached file
			File file3 = URLCache.getURLFile(configManager, null, urlStr, false);

			// Ensure the returned file is the cached file (refused => null)
			assertNull(file3);

			// Validate the cached info
			URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), urlStr);
			assertNull(cachedFile.getHttpStatusCode());
			//assertEquals(file1.getName(), cachedFile.getFilename());
			assertEquals(URLCache.INVALID_FILE_CACHE_TIMEOUT, cachedFile.getExpiry());
			assertNotNull(cachedFile.getLatestErrorMessage());
			assertFalse(cachedFile.isApproved());

			URLCache.commitURLFile(configManager, file3, urlStr);

			assertNull(cachedFile.getHttpStatusCode());
			//assertEquals(file3.getName(), cachedFile.getFilename());
			assertEquals(URLCache.CACHE_TIMEOUT, cachedFile.getExpiry());
			assertFalse(cachedFile.hasTemporaryData());
			assertNotNull(cachedFile.getLatestErrorMessage());
			assertTrue(cachedFile.isApproved());

			assertNull(file1);
			assertNull(file2);
			assertNull(file3);
			assertEquals(0, URLCache.countFile(getTestApplicationFolder()));
		}
	}

	// 1st request returned null
	//     Before timeout (should return what is in the cache, no re-download)
	//         2nd request (cached) - Rollback   [test_Null_Rollback]
	public void test_Null_Rollback() throws IOException, JSONException {
		if (this.serviceExists()) {
			URLCache.clearCache(getTestApplicationFolder());
			ConfigManager configManager = getConfigManager();

			String urlStr = NONE_EXISTING_URL;

			// Download the file for the 1st time
			File file1 = URLCache.getURLFile(configManager, null, urlStr, false);

			// Ensure the returned file is null
			assertNull(file1);

			// Rollback (refuse) the file
			File file2 = URLCache.rollbackURLFile(configManager, file1, urlStr, "New error message");

			// Ensure the returned file is the cached file
			assertNull(file2);


			// Re-request the file - the timeout hasn't expired, it must give the same cached file
			File file3 = URLCache.getURLFile(configManager, null, urlStr, false);

			// Ensure the returned file is the cached file (refused => null)
			assertNull(file3);

			// Validate the cached info
			URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), urlStr);
			assertNull(cachedFile.getHttpStatusCode());
			//assertEquals(file1.getName(), cachedFile.getFilename());
			assertEquals(URLCache.INVALID_FILE_CACHE_TIMEOUT, cachedFile.getExpiry());
			assertNotNull(cachedFile.getLatestErrorMessage());
			assertFalse(cachedFile.isApproved());

			File file4 = URLCache.rollbackURLFile(configManager, file3, urlStr, "New error message");

			// Ensure the returned file is the cached file (refused => null)
			assertNull(file4);

			assertNull(cachedFile.getHttpStatusCode());
			//assertEquals(file3.getName(), cachedFile.getFilename());
			assertEquals(URLCache.INVALID_FILE_CACHE_TIMEOUT, cachedFile.getExpiry());
			assertFalse(cachedFile.hasTemporaryData());
			assertNotNull(cachedFile.getLatestErrorMessage());
			assertFalse(cachedFile.isApproved());

			assertNull(file1);
			assertNull(file2);
			assertNull(file3);
			assertNull(file4);
			assertEquals(0, URLCache.countFile(getTestApplicationFolder()));
		}
	}

	// 1st request returned null
	//     After timeout (re-download)
	//         2nd request - Succeed
	//             Commit                        [test_NullTimeout_Commit]
	public void test_NullTimeout_Commit() throws IOException, JSONException {
		if (this.serviceExists()) {
			URLCache.clearCache(getTestApplicationFolder());
			ConfigManager configManager = getConfigManager();

			String validUrlStr = HTTPMOCKUP_SERVICE_URL + "?content=abcd";
			String invalidUrlStr = NONE_EXISTING_URL;

			// Download the file for the 1st time
			File file1 = URLCache.getURLFile(configManager, null, invalidUrlStr, false);

			// Ensure the returned file is null
			assertNull(file1);

			// Rollback (refuse) the file
			File file2 = URLCache.rollbackURLFile(configManager, file1, invalidUrlStr, "New error message");

			// Ensure the returned file is the cached file
			assertNull(file2);

			// Set expiry to trigger the re-download
			URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), invalidUrlStr);
			cachedFile.setExpiry(0);
			assertEquals(0, cachedFile.getExpiry());

			// The URL will always fail. We need to change the URL in order to make the 2nd request succeed.
			URLCache.diskCacheMap.put(validUrlStr, cachedFile.toJSON());


			// Re-request the file - the timeout has expired, it must re-download the file
			File file3 = URLCache.getURLFile(configManager, null, validUrlStr, false);

			// Validate the downloaded info
			String content = readFile(file3);
			assertEquals("abcd", content);

			// Validate the cached info
			cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), validUrlStr);
			assertNull(cachedFile.getHttpStatusCode());
			//assertEquals(file1.getName(), cachedFile.getFilename());
			//assertEquals(0, cachedFile.getExpiry());
			assertTrue(cachedFile.hasTemporaryData());
			assertEquals(SC_OK, cachedFile.getTemporaryHttpStatusCode());
			assertEquals(file3.getName(), cachedFile.getTemporaryFilename());
			assertNull(cachedFile.getLatestErrorMessage());
			assertFalse(cachedFile.isApproved());

			URLCache.commitURLFile(configManager, file3, validUrlStr);

			assertEquals(SC_OK, cachedFile.getHttpStatusCode());
			assertEquals(file3.getName(), cachedFile.getFilename());
			assertEquals(URLCache.CACHE_TIMEOUT, cachedFile.getExpiry());
			assertFalse(cachedFile.hasTemporaryData());
			assertNull(cachedFile.getLatestErrorMessage());
			assertTrue(cachedFile.isApproved());

			assertNull(file1);
			assertNull(file2);
			assertTrue(file3.exists());
			assertEquals(1, URLCache.countFile(getTestApplicationFolder()));
		}
	}

	// 1st request returned null
	//     After timeout (re-download)
	//         2nd request - Succeed
	//             Rollback                      [test_NullTimeout_Rollback]
	public void test_NullTimeout_Rollback() throws IOException, JSONException {
		if (this.serviceExists()) {
			URLCache.clearCache(getTestApplicationFolder());
			ConfigManager configManager = getConfigManager();

			String validUrlStr = HTTPMOCKUP_SERVICE_URL + "?content=abcd";
			String invalidUrlStr = NONE_EXISTING_URL;

			// Download the file for the 1st time
			File file1 = URLCache.getURLFile(configManager, null, invalidUrlStr, false);

			// Ensure the returned file is null
			assertNull(file1);

			// Rollback (refuse) the file
			File file2 = URLCache.rollbackURLFile(configManager, file1, invalidUrlStr, "New error message");

			// Ensure the returned file is the cached file
			assertNull(file2);

			// Set expiry to trigger the re-download
			URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), invalidUrlStr);
			cachedFile.setExpiry(0);
			assertEquals(0, cachedFile.getExpiry());

			// The URL will always fail. We need to change the URL in order to make the 2nd request succeed.
			URLCache.diskCacheMap.put(validUrlStr, cachedFile.toJSON());


			// Re-request the file - the timeout has expired, it must re-download the file
			File file3 = URLCache.getURLFile(configManager, null, validUrlStr, false);

			// Validate the downloaded info
			String content = readFile(file3);
			assertEquals("abcd", content);

			// Validate the cached info
			cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), validUrlStr);
			assertNull(cachedFile.getHttpStatusCode());
			//assertEquals(file1.getName(), cachedFile.getFilename());
			//assertEquals(0, cachedFile.getExpiry());
			assertTrue(cachedFile.hasTemporaryData());
			assertEquals(SC_OK, cachedFile.getTemporaryHttpStatusCode());
			assertEquals(file3.getName(), cachedFile.getTemporaryFilename());
			assertNull(cachedFile.getLatestErrorMessage());
			assertFalse(cachedFile.isApproved());

			File file4 = URLCache.rollbackURLFile(configManager, file3, validUrlStr, "New error message");

			assertNull(cachedFile.getHttpStatusCode());
			assertNull(file4);
			assertEquals(URLCache.INVALID_FILE_CACHE_TIMEOUT, cachedFile.getExpiry());
			assertFalse(cachedFile.hasTemporaryData());
			assertNotNull(cachedFile.getLatestErrorMessage());
			assertFalse(cachedFile.isApproved());

			assertNull(file1);
			assertNull(file2);
			assertFalse(file3.exists());
			assertNull(file4);
			assertEquals(0, URLCache.countFile(getTestApplicationFolder()));
		}
	}

	// 1st request returned null
	//     After timeout (re-download)
	//         2nd request - Failed (an error occurred and null is returned)
	//             Commit                        [test_NullTimeout_FailedCommit]
	public void test_NullTimeout_FailedCommit() throws IOException, JSONException {
		if (this.serviceExists()) {
			URLCache.clearCache(getTestApplicationFolder());
			ConfigManager configManager = getConfigManager();

			String urlStr = NONE_EXISTING_URL;

			// Download the file for the 1st time
			File file1 = URLCache.getURLFile(configManager, null, urlStr, false);

			// Ensure the returned file is null
			assertNull(file1);

			// Rollback (refuse) the file
			File file2 = URLCache.rollbackURLFile(configManager, file1, urlStr, "New error message");

			// Ensure the returned file is the cached file
			assertNull(file2);

			// Set expiry to trigger the re-download
			URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), urlStr);
			cachedFile.setExpiry(0);
			assertEquals(0, cachedFile.getExpiry());


			// Re-request the file - the timeout has expired, it must re-download the file
			File file3 = URLCache.getURLFile(configManager, null, urlStr, false);

			// The URLCache can not download the new file, so it automatically rollback and return the first file
			assertNull(file3);

			// Validate the cached info
			cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), urlStr);
			assertNull(cachedFile.getHttpStatusCode());
			//assertEquals(file1.getName(), cachedFile.getFilename());
			//assertEquals(0, cachedFile.getExpiry());
			//assertTrue(cachedFile.hasTemporaryData());
			//assertEquals(SC_OK, cachedFile.getTemporaryHttpStatusCode());
			//assertEquals(file3.getName(), cachedFile.getTemporaryFilename());
			assertNotNull(cachedFile.getLatestErrorMessage());
			assertFalse(cachedFile.isApproved());

			URLCache.commitURLFile(configManager, file3, urlStr);

			assertNull(cachedFile.getHttpStatusCode());
			assertNull(cachedFile.getFilename());
			assertEquals(URLCache.CACHE_TIMEOUT, cachedFile.getExpiry());
			assertFalse(cachedFile.hasTemporaryData());
			assertNotNull(cachedFile.getLatestErrorMessage());
			assertTrue(cachedFile.isApproved());

			assertNull(file1);
			assertNull(file2);
			assertNull(file3);
			assertEquals(0, URLCache.countFile(getTestApplicationFolder()));
		}
	}

	// 1st request returned null
	//     After timeout (re-download)
	//         2nd request - Failed (an error occurred and null is returned)
	//             Rollback                      [test_NullTimeout_FailedRollback]
	public void test_NullTimeout_FailedRollback() throws IOException, JSONException {
		if (this.serviceExists()) {
			URLCache.clearCache(getTestApplicationFolder());
			ConfigManager configManager = getConfigManager();

			String urlStr = NONE_EXISTING_URL;

			// Download the file for the 1st time
			File file1 = URLCache.getURLFile(configManager, null, urlStr, false);

			// Ensure the returned file is null
			assertNull(file1);

			// Rollback (refuse) the file
			File file2 = URLCache.rollbackURLFile(configManager, file1, urlStr, "New error message");

			// Ensure the returned file is the cached file
			assertNull(file2);

			// Set expiry to trigger the re-download
			URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), urlStr);
			cachedFile.setExpiry(0);
			assertEquals(0, cachedFile.getExpiry());


			// Re-request the file - the timeout has expired, it must re-download the file
			File file3 = URLCache.getURLFile(configManager, null, urlStr, false);

			// The URLCache can not download the new file, so it automatically rollback and return the first file
			assertNull(file3);

			// Validate the cached info
			cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), urlStr);
			assertNull(cachedFile.getHttpStatusCode());
			//assertEquals(file1.getName(), cachedFile.getFilename());
			//assertEquals(0, cachedFile.getExpiry());
			//assertTrue(cachedFile.hasTemporaryData());
			//assertEquals(SC_OK, cachedFile.getTemporaryHttpStatusCode());
			//assertEquals(file3.getName(), cachedFile.getTemporaryFilename());
			assertNotNull(cachedFile.getLatestErrorMessage());
			assertFalse(cachedFile.isApproved());

			File file4 = URLCache.rollbackURLFile(configManager, file3, urlStr, "New error message");

			assertNull(cachedFile.getHttpStatusCode());
			assertNull(cachedFile.getFilename());
			assertEquals(URLCache.INVALID_FILE_CACHE_TIMEOUT, cachedFile.getExpiry());
			assertFalse(cachedFile.hasTemporaryData());
			assertNotNull(cachedFile.getLatestErrorMessage());
			assertFalse(cachedFile.isApproved());

			assertNull(file1);
			assertNull(file2);
			assertNull(file3);
			assertNull(file4);
			assertEquals(0, URLCache.countFile(getTestApplicationFolder()));
		}
	}





	//     [testDownloadFailThenSucceed]
	//         Receive null at first request, rollback - timeout - receive something at 2nd request, commit
	public void testDownloadFailThenSucceed() throws IOException, JSONException {
		if (this.serviceExists()) {
			URLCache.clearCache(getTestApplicationFolder());
			ConfigManager configManager = getConfigManager();

			String invalidUrlStr = NONE_EXISTING_URL;
			String capUrlStr = HTTPMOCKUP_SERVICE_URL + "?resource=getCapabilities.xml";

			// Download the file for the 1st time
			File nullCap = URLCache.getURLFile(configManager, null, invalidUrlStr, false);
			assertNull(nullCap);

			// Rollback (refuse) the file
			File stillNullCap = URLCache.rollbackURLFile(configManager, nullCap, invalidUrlStr, "The capabilities document is empty.");
			assertNull(stillNullCap);

			// Timeout
			URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), invalidUrlStr);
			assertNotNull(cachedFile.getLatestErrorMessage());
			cachedFile.setExpiry(0);
			assertEquals(0, cachedFile.getExpiry());

			// Change URL - duplicate the cache info to the valid URL
			URLCache.diskCacheMap.put(capUrlStr, cachedFile.toJSON());


			// Download the file for the 1st time
			File validCap = URLCache.getURLFile(configManager, null, capUrlStr, false);

			// Validate the cached info
			cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), capUrlStr);
			assertNull(cachedFile.getHttpStatusCode());
			assertNull(cachedFile.getFilename());
			assertNull(cachedFile.getLatestErrorMessage());
			assertFalse(cachedFile.isApproved());

			URLCache.commitURLFile(configManager, validCap, capUrlStr);

			assertEquals(SC_OK, cachedFile.getHttpStatusCode());
			assertEquals(validCap.getName(), cachedFile.getFilename());
			assertEquals(URLCache.CACHE_TIMEOUT, cachedFile.getExpiry());
			assertFalse(cachedFile.hasTemporaryData());
			assertNull(cachedFile.getLatestErrorMessage());
			assertTrue(cachedFile.isApproved());

			assertNull(nullCap);
			assertNull(stillNullCap);
			assertTrue(validCap.exists());
			assertEquals(1, URLCache.countFile(getTestApplicationFolder()));
		}
	}

	//     [testSucceedThenTooLong]
	//         Request file, commit - timeout - request same file, it's now too large, return null, rollback
	public void testSucceedThenTooLong() throws IOException, JSONException {
		if (this.serviceExists()) {
			URLCache.clearCache(getTestApplicationFolder());
			ConfigManager configManager = getConfigManager();

			String capUrlStr = HTTPMOCKUP_SERVICE_URL + "?resource=getCapabilities.xml";
			String hundredMbUrlStr = HTTPMOCKUP_SERVICE_URL + "?randomascii=" + (100 * 1024 * 1024);

			// Download the file for the 1st time
			File validFile = URLCache.getURLFile(configManager, null, capUrlStr, false);
			assertNotNull(validFile);

			URLCache.commitURLFile(configManager, validFile, capUrlStr);

			// Timeout
			URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), capUrlStr);
			assertNull(cachedFile.getLatestErrorMessage());
			cachedFile.setExpiry(0);
			assertEquals(0, cachedFile.getExpiry());

			// Change URL - duplicate the cache info to the valid URL
			URLCache.diskCacheMap.put(hundredMbUrlStr, cachedFile.toJSON());


			// The file is too big, auto-rollback, return previous file.
			File previousFile = URLCache.getURLFile(configManager, null, hundredMbUrlStr, false);
			assertEquals(validFile, previousFile);

			assertEquals(SC_OK, cachedFile.getHttpStatusCode());
			assertEquals(previousFile.getName(), cachedFile.getFilename());
			assertEquals(URLCache.INVALID_FILE_CACHE_TIMEOUT, cachedFile.getExpiry());
			assertFalse(cachedFile.hasTemporaryData());
			assertNotNull(cachedFile.getLatestErrorMessage());
			assertTrue(cachedFile.isApproved());

			assertTrue(validFile.exists());
			assertTrue(previousFile.exists());
			assertEquals(1, URLCache.countFile(getTestApplicationFolder()));
		}
	}

	//     [testSucceedTwiceWithSameResult]
	//         Get XML GetCapabilities document, parse, commit - timeout - receive same doc after timeout, parse, commit
	public void testSucceedTwiceWithSameResult() throws IOException, JSONException, URISyntaxException, ServiceException {
		if (this.serviceExists()) {
			URLCache.clearCache(getTestApplicationFolder());
			ConfigManager configManager = getConfigManager();

			String capUrlStr = HTTPMOCKUP_SERVICE_URL + "?resource=getCapabilities.xml";

			// Download the file for the 1st time
			WMSCapabilities cap = URLCache.getWMSCapabilitiesResponse(configManager, null, capUrlStr, false);
			URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), capUrlStr);
			File capFile = cachedFile.getFile();

			assertNull(cachedFile.getLatestErrorMessage());
			assertNotNull(capFile);
			assertNotNull(cap);

			// Timeout
			cachedFile.setExpiry(0);
			assertEquals(0, cachedFile.getExpiry());


			// Download the file for the 2nd time
			WMSCapabilities newCap = URLCache.getWMSCapabilitiesResponse(configManager, null, capUrlStr, false);
			cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), capUrlStr);
			File newCapFile = cachedFile.getFile();

			assertNull(cachedFile.getLatestErrorMessage());
			assertNotNull(newCapFile);
			assertNotNull(newCap);

			assertFalse(capFile.equals(newCapFile));

			assertFalse(capFile.exists());
			assertTrue(newCapFile.exists());
			assertEquals(1, URLCache.countFile(getTestApplicationFolder()));
		}
	}

	//     [testParseFailThenSucceed]
	//         Get XML GetCapabilities document with stacktrace, unable to parse, rollback - timeout - receive valid document, parse, commit
	public void testParseFailThenSucceed() throws IOException, JSONException, URISyntaxException, ServiceException {
		if (this.serviceExists()) {
			URLCache.clearCache(getTestApplicationFolder());
			ConfigManager configManager = getConfigManager();

			String brokenCapUrlStr = HTTPMOCKUP_SERVICE_URL + "?resource=getBrokenCapabilities.xml";
			String capUrlStr = HTTPMOCKUP_SERVICE_URL + "?resource=getCapabilities.xml";

			// Download the file for the 1st time (the file is broken)
			WMSCapabilities brokenCap = URLCache.getWMSCapabilitiesResponse(configManager, null, brokenCapUrlStr, false);
			URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), brokenCapUrlStr);
			File brokenCapFile = cachedFile.getFile();

			assertNotNull(cachedFile.getLatestErrorMessage());
			assertNull(brokenCapFile);
			assertNull(brokenCap);

			// Timeout
			cachedFile.setExpiry(0);
			assertEquals(0, cachedFile.getExpiry());

			// Change URL - duplicate the cache info to the valid URL
			URLCache.diskCacheMap.put(capUrlStr, cachedFile.toJSON());


			// Download the file for the 2nd time (not broken this time)
			WMSCapabilities newCap = URLCache.getWMSCapabilitiesResponse(configManager, null, capUrlStr, false);
			cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), capUrlStr);
			File newCapFile = cachedFile.getFile();

			assertNull(cachedFile.getLatestErrorMessage());
			assertNotNull(newCapFile);
			assertNotNull(newCap);

			assertNull(brokenCapFile);
			assertTrue(newCapFile.exists());
			assertEquals(1, URLCache.countFile(getTestApplicationFolder()));
		}
	}

	//     [testSucceedThenParseFail]
	//         Get XML GetCapabilities document, parse, commit - timeout - receive document with stacktrace, unable to parse, rollback
	public void testSucceedThenParseFail() throws IOException, JSONException, URISyntaxException, ServiceException {
		if (this.serviceExists()) {
			URLCache.clearCache(getTestApplicationFolder());
			ConfigManager configManager = getConfigManager();

			String brokenCapUrlStr = HTTPMOCKUP_SERVICE_URL + "?resource=getBrokenCapabilities.xml";
			String capUrlStr = HTTPMOCKUP_SERVICE_URL + "?resource=getCapabilities.xml";

			// Download the file for the 1st time
			WMSCapabilities cap = URLCache.getWMSCapabilitiesResponse(configManager, null, capUrlStr, false);
			URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), capUrlStr);
			File capFile = cachedFile.getFile();

			assertNull(cachedFile.getLatestErrorMessage());
			assertNotNull(capFile);
			assertNotNull(cap);

			// Timeout
			cachedFile.setExpiry(0);
			assertEquals(0, cachedFile.getExpiry());

			// Change URL - duplicate the cache info to the valid URL
			URLCache.diskCacheMap.put(brokenCapUrlStr, cachedFile.toJSON());


			// Download the file for the 2nd time
			WMSCapabilities previousCap = URLCache.getWMSCapabilitiesResponse(configManager, null, brokenCapUrlStr, false);
			cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), brokenCapUrlStr);
			File previousCapFile = cachedFile.getFile();

			assertNotNull(cachedFile.getLatestErrorMessage());
			assertNotNull(previousCapFile);
			assertNotNull(previousCap);

			assertEquals(capFile, previousCapFile);

			assertTrue(capFile.exists());
			assertTrue(previousCapFile.exists());
			assertEquals(1, URLCache.countFile(getTestApplicationFolder()));
		}
	}

	//     [testTC211ParseFailThenTC211Succeed]
	//         Request TC211 document, receive HTML, unable to parse, rollback - timeout - receive valid TC211 document, parse, commit
	public void testTC211ParseFailThenTC211Succeed() throws IOException, JSONException, SAXException, ParserConfigurationException {
		if (this.serviceExists()) {
			URLCache.clearCache(getTestApplicationFolder());
			ConfigManager configManager = getConfigManager();

			String htmlTC211UrlStr = HTTPMOCKUP_SERVICE_URL + "?resource=tc211.html";
			String xmlTC211UrlStr = HTTPMOCKUP_SERVICE_URL + "?resource=tc211.xml";

			Document brokenTC211Document = Parser.parseURL(configManager, null, new URL(htmlTC211UrlStr), false);
			URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), htmlTC211UrlStr);
			File brokenTC211File = cachedFile.getFile();

			assertNotNull(cachedFile.getLatestErrorMessage());
			assertNull(brokenTC211File);
			assertNull(brokenTC211Document);

			// Timeout
			cachedFile.setExpiry(0);
			assertEquals(0, cachedFile.getExpiry());

			// Change URL - duplicate the cache info to the valid URL
			URLCache.diskCacheMap.put(xmlTC211UrlStr, cachedFile.toJSON());


			// Download the file for the 2nd time (not broken this time)
			Document tc211Document = Parser.parseURL(configManager, null, new URL(xmlTC211UrlStr), false);
			cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), xmlTC211UrlStr);
			File tc211File = cachedFile.getFile();

			assertNull(cachedFile.getLatestErrorMessage());
			assertNotNull(tc211File);
			assertNotNull(tc211Document);

			assertNull(brokenTC211File);
			assertTrue(tc211File.exists());
			assertEquals(1, URLCache.countFile(getTestApplicationFolder()));
		}
	}

	//     [testTC211SucceedThenTC211ParseFail]
	//         Request TC211 document, parse, commit - timeout - receive HTML doc instead of TC211 document, unable to parse, rollback
	public void testTC211SucceedThenTC211ParseFail() throws IOException, JSONException, SAXException, ParserConfigurationException {
		if (this.serviceExists()) {
			URLCache.clearCache(getTestApplicationFolder());
			ConfigManager configManager = getConfigManager();

			String htmlTC211UrlStr = HTTPMOCKUP_SERVICE_URL + "?resource=tc211.html";
			String xmlTC211UrlStr = HTTPMOCKUP_SERVICE_URL + "?resource=tc211.xml";

			Document tc211Document = Parser.parseURL(configManager, null, new URL(xmlTC211UrlStr), false);
			URLCache.CachedFile cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), xmlTC211UrlStr);
			File tc211File = cachedFile.getFile();

			assertNull(cachedFile.getLatestErrorMessage());
			assertNotNull(tc211File);
			assertNotNull(tc211Document);

			// Timeout
			cachedFile.setExpiry(0);
			assertEquals(0, cachedFile.getExpiry());

			// Change URL - duplicate the cache info to the valid URL
			URLCache.diskCacheMap.put(htmlTC211UrlStr, cachedFile.toJSON());


			Document previousTC211Document = Parser.parseURL(configManager, null, new URL(htmlTC211UrlStr), false);
			cachedFile = URLCache.getCachedFile(getTestApplicationFolder(), htmlTC211UrlStr);
			File previousTC211File = cachedFile.getFile();

			assertNotNull(cachedFile.getLatestErrorMessage());
			assertNotNull(previousTC211File);
			assertNotNull(previousTC211Document);

			assertEquals(tc211File, previousTC211File);

			assertTrue(tc211File.exists());
			assertTrue(previousTC211File.exists());
			assertEquals(1, URLCache.countFile(getTestApplicationFolder()));
		}
	}

	//     [testMultiUrl_Fail1_Success2_Fail1_WithRollback]
	//         URL1 return null, rollback - URL2 (same domain) return file, commit - request URL1 again (before timeout), expect null
	// NOTE: This test may fail if the URL Cache class do not remove the filename when the file is deleted or not created.
	public void testMultiUrl_Fail1_Success2_Fail1_WithRollback() throws IOException, JSONException {
		if (this.serviceExists()) {
			URLCache.clearCache(getTestApplicationFolder());
			ConfigManager configManager = getConfigManager();

			String url1Str = NONE_EXISTING_URL;
			String url2Str = HTTPMOCKUP_SERVICE_URL + "?content=abcd";


			// *** DOWNLOAD URL 1 ***

			// Download the file for the 1st time
			File file1_1 = URLCache.getURLFile(configManager, null, url1Str, false);

			// Ensure the returned file is null
			assertNull(file1_1);

			// Rollback (refuse) the file
			File file1_2 = URLCache.rollbackURLFile(configManager, file1_1, url1Str, "Server not found");

			// Ensure the returned file is the cached file
			assertNull(file1_2);


			// *** DOWNLOAD URL 2 ***

			// Download the file for the 1st time
			File file2_1 = URLCache.getURLFile(configManager, null, url2Str, false);

			// Ensure the returned file is not null
			assertNotNull(file2_1);

			// Validate the downloaded info
			String content = readFile(file2_1);
			assertEquals("abcd", content);

			URLCache.commitURLFile(configManager, file2_1, url2Str);


			// *** DOWNLOAD URL 1 AGAIN ***

			// Download the file for the 1st time
			File file1_3 = URLCache.getURLFile(configManager, null, url1Str, false);

			// Ensure the returned file is null
			assertNull(file1_3);

			// Rollback (refuse) the file
			File file1_4 = URLCache.rollbackURLFile(configManager, file1_3, url1Str, "Server not found");

			// Ensure the returned file is the cached file
			assertNull(file1_4);


			assertNull(file1_1);
			assertNull(file1_2);
			assertNull(file1_3);
			assertNull(file1_4);
			assertTrue(file2_1.exists());
			assertEquals(1, URLCache.countFile(getTestApplicationFolder()));
		}
	}

	//     [testMultiUrl_Fail1_Success2_Fail1_WithCommit]
	//         URL1 return null, commit - URL2 (same domain) return file, commit - request URL1 again (before timeout), expect null
	// NOTE: This test may fail if the URL Cache class do not remove the filename when the file is deleted or not created.
	public void testMultiUrl_Fail1_Success2_Fail1_WithCommit() throws IOException, JSONException {
		if (this.serviceExists()) {
			URLCache.clearCache(getTestApplicationFolder());
			ConfigManager configManager = getConfigManager();

			String url1Str = NONE_EXISTING_URL;
			String url2Str = HTTPMOCKUP_SERVICE_URL + "?content=abcd";


			// *** DOWNLOAD URL 1 ***

			// Download the file for the 1st time
			File file1_1 = URLCache.getURLFile(configManager, null, url1Str, false);

			// Ensure the returned file is null
			assertNull(file1_1);

			// Commit (accept) the file
			URLCache.commitURLFile(configManager, file1_1, url1Str);


			// *** DOWNLOAD URL 2 ***

			// Download the file for the 1st time
			File file2_1 = URLCache.getURLFile(configManager, null, url2Str, false);

			// Ensure the returned file is not null
			assertNotNull(file2_1);

			// Validate the downloaded info
			String content = readFile(file2_1);
			assertEquals("abcd", content);

			URLCache.commitURLFile(configManager, file2_1, url2Str);


			// *** DOWNLOAD URL 1 AGAIN ***

			// Download the file for the 1st time
			File file1_3 = URLCache.getURLFile(configManager, null, url1Str, false);

			// Ensure the returned file is null
			assertNull(file1_3);

			// Rollback (refuse) the file
			File file1_4 = URLCache.rollbackURLFile(configManager, file1_3, url1Str, "Server not found");

			// Ensure the returned file is the cached file
			assertNull(file1_4);


			assertNull(file1_1);
			assertNull(file1_3);
			assertNull(file1_4);
			assertTrue(file2_1.exists());
			assertEquals(1, URLCache.countFile(getTestApplicationFolder()));
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
					errorMsg = ex.getMessage();
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
			fail("The temporary folder \"" + tmpFolderPath + "\" is needed for this test but it is not accessible");
		}

		return tmpFolder;
	}

	private ConfigManager getConfigManager() {
		ConfigManager configManager = new ConfigManager(null, null);
		configManager.setApplicationFolder(getTestApplicationFolder());

		return configManager;
	}
}
