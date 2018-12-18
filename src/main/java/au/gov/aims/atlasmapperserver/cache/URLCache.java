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
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class URLCache {
    private static final Logger LOGGER = Logger.getLogger(URLCache.class.getName());
    protected static final long MAX_CACHED_FILE_SIZE = 50 * 1024 * 1024; // in Bytes

    private CacheDatabase cacheDatabase;

    public URLCache(ConfigManager configManager) {
        this.cacheDatabase = new CacheDatabase(configManager);
    }

    /**
     * Used with JUnit tests
     * @return
     */
    protected CacheDatabase getCacheDatabase() {
        return this.cacheDatabase;
    }

    public CacheEntry getHttpHead(URL url)
            throws RevivableThreadInterruptedException, SQLException, IOException, ClassNotFoundException, URISyntaxException {

        return this.getHttpHead(url, false);
    }

    public CacheEntry getHttpHead(URL url, boolean redownload)
            throws RevivableThreadInterruptedException, SQLException, IOException, ClassNotFoundException, URISyntaxException {

        RevivableThread.checkForInterruption();

        CacheEntry cacheEntry = null;

        try {
            this.cacheDatabase.openConnection();
            cacheEntry = this.cacheDatabase.get(url);

            // If re-download is required, re-download the URL.
            // Otherwise, simply return what we got from the DataBase.
            if (this.isDownloadRequired(cacheEntry, redownload, RequestMethod.HEAD)) {
                cacheEntry = this.requestHttpHead(url);
            }

            cacheEntry.setLastAccessTimestamp(CacheEntry.getCurrentTimestamp());
            this.cacheDatabase.save(cacheEntry);
        } finally {
            this.cacheDatabase.close();
        }

        return cacheEntry;
    }

    public CacheEntry getHttpDocument(URL url)
            throws RevivableThreadInterruptedException, SQLException, IOException, ClassNotFoundException, URISyntaxException {

        return this.getHttpDocument(url, false);
    }

    public CacheEntry getHttpDocument(URL url, boolean redownload)
            throws RevivableThreadInterruptedException, SQLException, IOException, ClassNotFoundException, URISyntaxException {

        RevivableThread.checkForInterruption();

        CacheEntry cacheEntry = null;

        try {
            this.cacheDatabase.openConnection();
            cacheEntry = this.cacheDatabase.get(url);

            // If re-download is required, re-download the URL.
            // Otherwise, simply return what we got from the DataBase.
            if (this.isDownloadRequired(cacheEntry, redownload, RequestMethod.GET)) {
                cacheEntry = this.requestHttpDocument(url);
            } else {
                this.cacheDatabase.loadDocument(cacheEntry);
            }

            cacheEntry.setLastAccessTimestamp(CacheEntry.getCurrentTimestamp());
            this.cacheDatabase.save(cacheEntry);
        } finally {
            this.cacheDatabase.close();
        }

        return cacheEntry;
    }

    private CacheEntry requestHttpHead(URL url) throws URISyntaxException, RevivableThreadInterruptedException, IOException {
        CacheEntry cacheEntry = new CacheEntry(url);
        cacheEntry.setRequestMethod(RequestMethod.HEAD);
        cacheEntry.setRequestTimestamp(CacheEntry.getCurrentTimestamp());

        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        HttpUriRequest headRequest = null;
        try {
            httpClient = Utils.createHttpClient();
            headRequest = RequestBuilder.head(url.toURI()).build();
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

        return cacheEntry;
    }

    private CacheEntry requestHttpDocument(URL url) throws URISyntaxException, RevivableThreadInterruptedException, IOException {
        CacheEntry cacheEntry = new CacheEntry(url);
        cacheEntry.setRequestMethod(RequestMethod.GET);
        cacheEntry.setRequestTimestamp(CacheEntry.getCurrentTimestamp());

        HttpGet httpGet = null;
        InputStream in = null;
        FileOutputStream out = null;
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        try {
            httpClient = Utils.createHttpClient();
            httpGet = new HttpGet(url.toURI());

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
            if (httpGet != null) {
                // Cancel the connection, if it's still alive
                httpGet.abort();
                // Close connections
                httpGet.reset();
            }
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
            if (httpClient != null) {
                try { httpClient.close(); } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error occur while closing the HttpClient: " + Utils.getExceptionMessage(e), e);
                }
            }
        }

        return cacheEntry;
    }

    private boolean isDownloadRequired(CacheEntry cacheEntry, boolean redownload, RequestMethod requestMethod) {
        // The user specified a redownload
        if (redownload) {
            return true;
        }

        // The URL has never been downloaded or it's expired
        if (cacheEntry == null || cacheEntry.isExpired()) {
            return true;
        }

        // The URL has been requested as a HTTP HEAD only. It needs to be re-requested again with a HTTP GET request.
        if (!RequestMethod.HEAD.equals(requestMethod) && RequestMethod.HEAD.equals(cacheEntry.getRequestMethod())) {
            return true;
        }

        return false;
    }

}
