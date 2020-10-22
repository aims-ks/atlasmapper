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

import au.gov.aims.atlasmapperserver.ConfigManager;
import au.gov.aims.atlasmapperserver.Utils;
import au.gov.aims.atlasmapperserver.thread.RevivableThread;
import au.gov.aims.atlasmapperserver.thread.RevivableThreadInterruptedException;
import au.gov.aims.atlasmapperserver.thread.ThreadLogger;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class URLCache {
    private static final Logger LOGGER = Logger.getLogger(URLCache.class.getName());
    protected static final long MAX_CACHED_FILE_SIZE = 50 * 1024 * 1024; // in Bytes

    // Minimum delay to wait between expiry entry clean-up
    // NOTE: If the clean up request is sent for every URL request, that would slow down the system considerably
    private static long EXPIRY_CLEANUP_MINIMUM_DELAY = 1000 * 60 * 5; // 5 minutes

    private CacheDatabase cacheDatabase;
    private static long lastExpiryCleanupTimestamp = 0;

    private Long runStartTimestamp;

    public URLCache(ConfigManager configManager) {
        this.cacheDatabase = new CacheDatabase(configManager);
    }

    public URLCache(ConfigManager configManager, String databaseName) {
        this.cacheDatabase = new CacheDatabase(configManager, databaseName);
    }

    public void startRun() {
        LOGGER.log(Level.INFO, "New run started");
        this.runStartTimestamp = CacheEntry.getCurrentTimestamp();
    }

    public long endRun() {
        if (this.runStartTimestamp == null) {
            return -1;
        }

        long endRunTimestamp = CacheEntry.getCurrentTimestamp();
        long elapseTime = endRunTimestamp - this.runStartTimestamp;
        this.runStartTimestamp = null;

        LOGGER.log(Level.INFO, String.format("End of run (%d ms)", elapseTime));
        return elapseTime;
    }

    /**
     * Used with JUnit tests
     * @return
     */
    protected CacheDatabase getCacheDatabase() {
        return this.cacheDatabase;
    }

    public CacheEntry getCacheEntry(URL url) throws SQLException, IOException, ClassNotFoundException {
        CacheEntry cacheEntry;

        try {
            this.cacheDatabase.openConnection();
            cacheEntry = this.cacheDatabase.get(url);
        } finally {
            this.cacheDatabase.close();
        }

        if (cacheEntry == null) {
            cacheEntry = new CacheEntry(url);
        }

        return cacheEntry;
    }

    public void getHttpHead(CacheEntry cacheEntry, String entityId)
            throws RevivableThreadInterruptedException, SQLException, IOException, ClassNotFoundException, URISyntaxException {

        this.getHttpHead(cacheEntry, entityId, null);
    }

    /**
     *
     * @param cacheEntry
     * @param entityId
     * @param redownload Null to let the system decide. false to get what is in the DB. true to force a re-download.
     * @return
     * @throws RevivableThreadInterruptedException
     * @throws SQLException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws URISyntaxException
     */
    public void getHttpHead(CacheEntry cacheEntry, String entityId, Boolean redownload)
            throws RevivableThreadInterruptedException, SQLException, IOException, ClassNotFoundException, URISyntaxException {

        if (entityId != null) {
            cacheEntry.addUsage(entityId);
        }

        // First, delete expired entries if any
        this.deleteExpired();
        RevivableThread.checkForInterruption();

        // If re-download is required, re-download the URL.
        // Otherwise, simply return what we got from the database.
        if (this.isDownloadRequired(cacheEntry.getUrl(), redownload, RequestMethod.HEAD)) {
            this.requestHttpHead(cacheEntry);
        }
    }

    public void getHttpDocument(CacheEntry cacheEntry, String entityId)
            throws RevivableThreadInterruptedException, SQLException, IOException, ClassNotFoundException, URISyntaxException {

        this.getHttpDocument(cacheEntry, entityId, null);
    }

    /**
     *
     * @param cacheEntry
     * @param entityId
     * @param redownload Null to let the system decide. false to get what is in the DB. true to force a re-download.
     * @return
     * @throws RevivableThreadInterruptedException
     * @throws SQLException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws URISyntaxException
     */
    public void getHttpDocument(CacheEntry cacheEntry, String entityId, Boolean redownload)
            throws RevivableThreadInterruptedException, SQLException, IOException, ClassNotFoundException, URISyntaxException {

        if (entityId != null) {
            cacheEntry.addUsage(entityId);
        }

        // First, delete expired entries if any
        this.deleteExpired();
        RevivableThread.checkForInterruption();

        URL url = cacheEntry.getUrl();

        // If re-download is required, re-download the URL.
        // Otherwise, simply return what we got from the database.
        if (this.isDownloadRequired(url, redownload, RequestMethod.GET)) {
            this.requestHttpDocument(cacheEntry);
        } else {
            try {
                this.cacheDatabase.openConnection();
                this.cacheDatabase.loadDocument(cacheEntry);
            } finally {
                this.cacheDatabase.close();
            }
        }
    }

    public void save(CacheEntry cacheEntry, boolean valid)
            throws RevivableThreadInterruptedException, SQLException, IOException, ClassNotFoundException {

        this.save(cacheEntry, valid, null);
    }

    public void save(CacheEntry cacheEntry, boolean valid, Long expiryTimestamp)
            throws RevivableThreadInterruptedException, SQLException, IOException, ClassNotFoundException {

        RevivableThread.checkForInterruption();

        if (cacheEntry != null) {
            try {
                this.cacheDatabase.openConnection();

                cacheEntry.setValid(valid);
                cacheEntry.setExpiryTimestamp(expiryTimestamp);
                cacheEntry.setLastAccessTimestamp(CacheEntry.getCurrentTimestamp());

                this.cacheDatabase.save(cacheEntry);
            } finally {
                this.cacheDatabase.close();
            }
        }
    }

    public void deleteExpired()
            throws SQLException, IOException, ClassNotFoundException {

        this.deleteExpired(false);
    }

    public void deleteExpired(boolean force)
            throws SQLException, IOException, ClassNotFoundException {

        long currentTimestamp = CacheEntry.getCurrentTimestamp();
        if (force || URLCache.lastExpiryCleanupTimestamp + URLCache.EXPIRY_CLEANUP_MINIMUM_DELAY < currentTimestamp) {
            URLCache.lastExpiryCleanupTimestamp = currentTimestamp;

            try {
                this.cacheDatabase.openConnection();
                this.cacheDatabase.deleteExpired();
            } finally {
                this.cacheDatabase.close();
            }
        }
    }

    /**
     * Remove association with entityId for entries that were not visited since the beginning of the run
     * @param entityId
     * @throws RevivableThreadInterruptedException
     * @throws SQLException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void cleanUp(String entityId)
            throws RevivableThreadInterruptedException, SQLException, IOException, ClassNotFoundException {

        RevivableThread.checkForInterruption();

        if (this.runStartTimestamp != null) {
            this.cleanUp(entityId, this.runStartTimestamp);
        } else {
            LOGGER.log(Level.WARNING, "Clean up called outside of a run!");
        }
    }

    public void deleteEntity(String entityId)
            throws SQLException, IOException, ClassNotFoundException {

        // cleanUp will remove all association with entityId, then will removed entries that are unused.
        this.cleanUp(entityId, 0);
    }

    private void cleanUp(String entityId, long expiryTimestamp)
            throws SQLException, IOException, ClassNotFoundException {

        try {
            this.cacheDatabase.openConnection();
            this.cacheDatabase.cleanUp(entityId, expiryTimestamp);
        } finally {
            this.cacheDatabase.close();
        }
    }

    private void requestHttpHead(CacheEntry cacheEntry) throws URISyntaxException, RevivableThreadInterruptedException, IOException {
        cacheEntry.setRequestMethod(RequestMethod.HEAD);
        cacheEntry.setRequestTimestamp(CacheEntry.getCurrentTimestamp());

        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        HttpUriRequest headRequest = null;
        URL url = cacheEntry.getUrl();
        try {
            httpClient = Utils.createHttpClient();
            headRequest = RequestBuilder.head(url.toURI()).build();

            LOGGER.log(Level.INFO, "HTTP HEAD " + url.toString());
            response = httpClient.execute(headRequest);
            RevivableThread.checkForInterruption();

            StatusLine httpStatus = response.getStatusLine();
            if (httpStatus != null) {
                cacheEntry.setHttpStatusCode(httpStatus.getStatusCode());
            }
        } finally {
            if (headRequest != null) {
                // Cancel the connection, if it's still alive
                headRequest.abort();
            }
            if (response != null) {
                try { response.close(); } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error occur while closing the HttpResponse: " + Utils.getExceptionMessage(e), e);
                }
            }
            if (httpClient != null) {
                try { httpClient.close(); } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error occur while closing the HttpClient: " + Utils.getExceptionMessage(e), e);
                }
            }
        }
    }

    private void requestHttpDocument(CacheEntry cacheEntry) throws URISyntaxException, RevivableThreadInterruptedException, IOException {
        cacheEntry.setRequestMethod(RequestMethod.GET);
        cacheEntry.setRequestTimestamp(CacheEntry.getCurrentTimestamp());

        HttpGet httpGet = null;
        InputStream in = null;
        FileOutputStream out = null;
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        URL url = cacheEntry.getUrl();
        try {
            httpClient = Utils.createHttpClient();
            httpGet = new HttpGet(url.toURI());

            LOGGER.log(Level.INFO, "HTTP GET " + url.toString());
            response = httpClient.execute(httpGet);
            RevivableThread.checkForInterruption();

            StatusLine httpStatus = response.getStatusLine();
            if (httpStatus != null) {
                cacheEntry.setHttpStatusCode(httpStatus.getStatusCode());
            }

            // The entity is streamed
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                File tmpFile = File.createTempFile("atlasmapper-download_", ".bin");

                in = entity.getContent();
                out = new FileOutputStream(tmpFile);

                // The file size may be unknown on the server. This method stop streaming when the file size reach the limit.
                Utils.binaryCopy(in, out, MAX_CACHED_FILE_SIZE);

                cacheEntry.setDocumentFile(tmpFile);
            }
        } finally {
            if (in != null) {
                try { in.close(); } catch (Exception e) {
                    LOGGER.log(Level.WARNING, String.format("Error occur while closing the URL %s: %s", url, Utils.getExceptionMessage(e)), e);
                }
            }
            if (out != null) {
                try { out.close(); } catch (Exception e) {
                    LOGGER.log(Level.WARNING, String.format("Error occur while closing the file: %s", Utils.getExceptionMessage(e)), e);
                }
            }
            if (response != null) {
                try { response.close(); } catch (Exception e) {
                    LOGGER.log(Level.WARNING, String.format("Error occur while closing the HttpResponse: %s", Utils.getExceptionMessage(e)), e);
                }
            }
            if (httpGet != null) {
                // Cancel the connection, if it's still alive
                httpGet.abort();
                // Close connections
                httpGet.reset();
            }
            if (httpClient != null) {
                try { httpClient.close(); } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error occur while closing the HttpClient: " + Utils.getExceptionMessage(e), e);
                }
            }
        }
    }

    public boolean isDownloadRequired(URL url)
            throws SQLException, IOException, ClassNotFoundException {

        return this.isDownloadRequired(url, null, RequestMethod.GET);
    }

    private boolean isDownloadRequired(URL url, Boolean redownload, RequestMethod requestMethod)
            throws SQLException, IOException, ClassNotFoundException {

        CacheEntry cacheEntry = null;

        try {
            this.cacheDatabase.openConnection();
            cacheEntry = this.cacheDatabase.get(url);

            // If the user requested a redownload (or explicitly requested no download)
            if (redownload != null) {
                if (redownload) {
                    // Return true if the file has not been re-downloaded since the beginning of the run (rebuild)
                    if (this.runStartTimestamp == null ||
                            cacheEntry == null || cacheEntry.getRequestTimestamp() == null ||
                            cacheEntry.getRequestTimestamp() < this.runStartTimestamp) {
                        return true;
                    }
                } else {
                    return false;
                }
            }

            // The URL has never been downloaded or it's expired
            if (cacheEntry == null || cacheEntry.isExpired()) {
                return true;
            }

            // The URL has been requested as a HTTP HEAD only. It needs to be re-requested again with a HTTP GET request.
            if (!RequestMethod.HEAD.equals(requestMethod) && RequestMethod.HEAD.equals(cacheEntry.getRequestMethod())) {
                return true;
            }

        } finally {
            this.cacheDatabase.close();
            if (cacheEntry != null) {
                cacheEntry.close();
            }
        }

        return false;
    }

    public static JSONObject parseJSONObjectFile(File jsonFile, ThreadLogger logger, String urlStr) {
        JSONObject jsonResponse = null;
        Reader reader = null;
        try {
            reader = new FileReader(jsonFile);
            jsonResponse = new JSONObject(new JSONTokener(reader));
        } catch(Exception ex) {
            if (logger != null) {
                logger.log(Level.WARNING, String.format("Can not load the [JSON Object](%s): %s",
                        urlStr, Utils.getExceptionMessage(ex)), ex);
            } else {
                LOGGER.log(Level.WARNING, String.format("Can not load the JSON Object %s: %s",
                        urlStr, Utils.getExceptionMessage(ex)), ex);
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, String.format("Can not close the JSON file %s: %s",
                            jsonFile.getAbsoluteFile().getAbsolutePath(), Utils.getExceptionMessage(ex)), ex);
                }
            }
        }

        return jsonResponse;
    }

    public static JSONArray parseJSONArrayFile(File jsonFile, ThreadLogger logger, String urlStr) {
        JSONArray jsonResponse = null;
        Reader reader = null;
        try {
            reader = new FileReader(jsonFile);
            jsonResponse = new JSONArray(new JSONTokener(reader));
        } catch(Exception ex) {
            if (logger != null) {
                logger.log(Level.WARNING, String.format("Can not load the [JSON Array](%s): %s",
                        urlStr, Utils.getExceptionMessage(ex)), ex);
            } else {
                LOGGER.log(Level.WARNING, String.format("Can not load the JSON Array %s: %s",
                        urlStr, Utils.getExceptionMessage(ex)), ex);
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, String.format("Can not close the JSON file %s: %s",
                            jsonFile.getAbsoluteFile().getAbsolutePath(), Utils.getExceptionMessage(ex)), ex);
                }
            }
        }

        return jsonResponse;
    }

}
