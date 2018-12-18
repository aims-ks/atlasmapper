/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2011 Australian Institute of Marine Science
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

package au.gov.aims.atlasmapperserver;

import au.gov.aims.atlasmapperserver.collection.MultiKeyHashMap;
import au.gov.aims.atlasmapperserver.dataSourceConfig.AbstractDataSourceConfig;
import au.gov.aims.atlasmapperserver.servlet.FileFinder;
import au.gov.aims.atlasmapperserver.thread.RevivableThread;
import au.gov.aims.atlasmapperserver.thread.RevivableThreadInterruptedException;
import au.gov.aims.atlasmapperserver.thread.ThreadLogger;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.geotools.data.ows.WMSCapabilities;
import org.geotools.data.wms.xml.WMSSchema;
import org.geotools.ows.ServiceException;
import org.geotools.xml.DocumentFactory;
import org.geotools.xml.handlers.DocumentHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@Deprecated
public class URLCache {
    @Deprecated
    private static final Logger LOGGER = Logger.getLogger(URLCache.class.getName());

    @Deprecated
    protected static final long NB_MS_PER_MINUTE = 60000;

    // Cache timeout in millisecond
    // The response will be re-requested if the application request
    // information from it and its cached timestamp is older than this setting.
    @Deprecated
    protected static final int CACHE_TIMEOUT = -1; // In minutes; -1 = never times out (the application has to force harvest to re-download it)
    @Deprecated
    protected static final int INVALID_FILE_CACHE_TIMEOUT = -1; // In minutes; Invalid files are re-downloaded by checking a checkbox on the re-building of the data source.
    @Deprecated
    protected static final long SEARCH_CACHE_TIMEOUT = 60 * NB_MS_PER_MINUTE;
    @Deprecated
    protected static final long SEARCH_CACHE_MAXSIZE = 10; // Maximum search responses

    @Deprecated
    protected static final String CACHE_FILES_FOLDER = "files";
    @Deprecated
    protected static final int MAX_CACHED_FILE_SIZE = 50; // in megabytes (Mb)

    @Deprecated
    protected static final int MAX_FOLLOW_REDIRECTION = 50; // Maximum number of URL follow allowed. Over passing this amount will be considered as a cycle in the cache and will throw IOException.

    // HashMap<String urlString, ResponseWrapper response>
    @Deprecated
    private static HashMap<String, ResponseWrapper> searchResponseCache = new HashMap<String, ResponseWrapper>();

    /**
     * {
     *     url: {
     *         file: "path/to/the/file",
     *         dataSourceIds: ["ea", "g", ...],
     *         downloadedTime: "2012-09-24 14:06:49",
     *         expiry: 60, // In minutes
     *
     *         // Set when the file expired, the actual file is replace with this if it's approved by the application.
     *         tmpFile: {
     *             file: "path/to/the/tmpFile",
     *             downloadedTime: "2012-09-24 15:07:34"
     *         }
     *     }
     * }
     */
    @Deprecated
    private static JSONObject diskCacheMap = null;
    /**
     * Reload the disk cache when the disk cache file is manually modified;
     *     Every time the disk cache is accessed, the last modified date of the disk cache map file is
     *     checked against this attribute. If the file is newer, that the file is reloaded.
     *     TODO: Do not reload while modifying the disk cache in memory. Use a DB for better handling & thread safe
     */
    @Deprecated
    private static long loadedTime = -1;

    @Deprecated
    private static File getApplicationFolder(ConfigManager configManager) {
        if (configManager == null) {
            // Can be used for running the tests
            return new File(System.getProperty("java.io.tmpdir"));
        }
        return configManager.getApplicationFolder();
    }

    @Deprecated
    public static ResponseStatus getHttpHead(String urlStr) throws RevivableThreadInterruptedException {
        RevivableThread.checkForInterruption();

        ResponseStatus responseStatus = new ResponseStatus();

        URI uri = null;
        try {
            uri = Utils.toURL(urlStr).toURI();
        } catch (Exception ex) {
            responseStatus.setStatusCode(HttpStatus.SC_BAD_REQUEST);
            final String message = "Can not parse the URL: " + urlStr;
            responseStatus.setErrorMessage(message);
            LOGGER.log(Level.FINE, message, ex);
            return responseStatus;
        }

        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        HttpUriRequest headRequest = null;
        try {
            httpClient = Utils.createHttpClient();
            headRequest = RequestBuilder.head(uri).build();
            response = httpClient.execute(headRequest);
            RevivableThread.checkForInterruption();

            StatusLine httpStatus = response.getStatusLine();
            if (httpStatus != null) {
                responseStatus.setStatusCode(httpStatus.getStatusCode());
            }
        } catch (IOException ex) {
            final String message = Utils.getExceptionMessage(ex);
            responseStatus.setErrorMessage(message);
            LOGGER.log(Level.FINE, message, ex);

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

        return responseStatus;
    }

    /**
     * This method have to by used along with commitURLFile and rollbackURLFile:
     *     File jsonFile = getURLFile(configManager, dataSource, urlStr);
     *     JSONObject jsonResponse = null;
     *     try {
     *         jsonResponse = parseFile(jsonFile, urlStr);
     *         commitURLFile(configManager, jsonFile, urlStr);
     *     } catch(Exception ex) {
     *         File rollbackFile = rollbackURLFile(configManager, jsonFile, urlStr);
     *         jsonResponse = parseFile(rollbackFile, urlStr);
     *     }
     *
     * @param logger
     * @param downloadedEntityName String representing the entity to download, used in log messages.
     *     For example: "layer ea:baselayer" or "GetCapabilities document"
     * @param configManager
     * @param dataSource
     * @param urlStr
     * @param category Category used to clear the cache partially; Capabilities documents, MEST records...
     * @param mandatory True to generate an error if something goes wrong, False to generate a warning instead.
     *     The errors / warnings are recorded in the "CachedFile" object and saved in the cache data base.
     *     (the DB is a JSON file on disk).
     * @return
     * @throws IOException
     * @throws JSONException
     */
    @Deprecated
    public static File getURLFile(
            ThreadLogger logger,
            String downloadedEntityName,
            ConfigManager configManager,
            AbstractDataSourceConfig dataSource,
            String urlStr,
            Category category,
            boolean mandatory
    ) throws IOException, JSONException, RevivableThreadInterruptedException {

        return getURLFile(logger, downloadedEntityName, configManager, dataSource, urlStr, category, mandatory, 0);
    }

    @Deprecated
    private static File getURLFile(
            ThreadLogger logger,
            String downloadedEntityName,
            ConfigManager configManager,
            AbstractDataSourceConfig dataSource,
            String urlStr,
            Category category,
            boolean mandatory,
            int followRedirectionCount
    ) throws IOException, JSONException, RevivableThreadInterruptedException {

        File applicationFolder = getApplicationFolder(configManager);
        String dataSourceId = null;
        Boolean activeDownload = null;
        if (dataSource != null) {
            dataSourceId = dataSource.getDataSourceId();
            activeDownload = dataSource.isActiveDownload();
        }

        if (activeDownload == null) {
            activeDownload = false;
        }

        JSONObject jsonCache = getDiskCacheMap(applicationFolder);
        RevivableThread.checkForInterruption();

        File cacheFolder = FileFinder.getDiskCacheFolder(applicationFolder);
        CachedFile cachedFile = getCachedFile(applicationFolder, urlStr);

        // Check if the disk cache is valid (we might have to do a little clean-up of the text file if it has been left in a inconsistent state).
        if (!cachedFile.isEmpty()) {
            if (dataSourceId != null && !cachedFile.hasDataSourceId(dataSourceId)) {
                cachedFile.addDataSourceId(dataSourceId);
            }

            // Follow redirection - WARNING: A maximum number of redirection is allowed to avoid infinite loops (this may occurred if there is a loop in the redirections).
            String redirectionUrl = cachedFile.getRedirection();
            if (Utils.isNotBlank(redirectionUrl)) {
                if (followRedirectionCount < MAX_FOLLOW_REDIRECTION) {
                    // Touch the cache entry; set the last access date to "now"
                    cachedFile.setLastAccessDate();

                    return getURLFile(logger, downloadedEntityName, configManager, dataSource, redirectionUrl, category, mandatory, followRedirectionCount+1);
                } else {
                    // Hopefully this error will never occurred
                    logger.log(Level.SEVERE, String.format("Maximum URL redirection reach for [%s](%s). " +
                                    "There is probably a cycle in the cache, which create potential infinite loops.",
                            downloadedEntityName, urlStr));
                    throw new IOException("Cycle in the cache follow URLs");
                }
            }

            boolean timedOut = true;
            int expiry = cachedFile.getExpiry();
            if (expiry < 0) {
                timedOut = false;
            } else {
                Date downloadTime = cachedFile.getDownloadedTime();
                if (downloadTime != null) {
                    long ageInMs = new Date().getTime() - downloadTime.getTime();
                    // Expiry (in hour) * (60*60*1000) = Expiry in ms
                    if (ageInMs < expiry * NB_MS_PER_MINUTE) {
                        timedOut = false;
                    }
                }
            }

            boolean markedForReDownload = cachedFile.isMarkedForReDownload();

            if (timedOut || markedForReDownload || activeDownload) {
                String tmpFilename = CachedFile.generateFilename(cacheFolder, urlStr);
                cachedFile.setTemporaryFilename(tmpFilename);
                // Set the time of the last download tentative; which is now
                cachedFile.setDownloadedTime(new Date());

                File tmpFile = new File(cachedFile.getCachedFileFolder(), tmpFilename);

                RevivableThread.checkForInterruption();
                logger.log(Level.INFO, String.format("Re-downloading [%s](%s)", downloadedEntityName, urlStr));

                ResponseStatus responseStatus = loadURLToFile(logger, urlStr, tmpFile);
                RevivableThread.checkForInterruption();

                String errorMessage = responseStatus.getErrorMessage();
                cachedFile.setMarkedForReDownload(false);
                cachedFile.setTemporaryHttpStatusCode(responseStatus.getStatusCode());
                cachedFile.setLatestErrorMessage(errorMessage);
                cachedFile.cleanUpFilenames();
            }
        }

        // The URL is not present in the cache. Load it!
        if (cachedFile.isEmpty()) {
            String filename = CachedFile.generateFilename(cacheFolder, urlStr);

            cachedFile = new CachedFile(cacheFolder, dataSourceId, filename, category, new Date(), CACHE_TIMEOUT, mandatory);
            jsonCache.put(urlStr, cachedFile.toJSON());

            File file = new File(cachedFile.getCachedFileFolder(), filename);

            RevivableThread.checkForInterruption();
            logger.log(Level.INFO, String.format("Downloading [%s](%s)", downloadedEntityName, urlStr));

            ResponseStatus responseStatus = loadURLToFile(logger, urlStr, file);
            RevivableThread.checkForInterruption();

            String errorMessage = responseStatus.getErrorMessage();
            cachedFile.setHttpStatusCode(responseStatus.getStatusCode());
            cachedFile.setLatestErrorMessage(errorMessage);
            cachedFile.cleanUpFilenames();
            if (Utils.isNotBlank(errorMessage)) {
                cachedFile.setApproved(false);
            }
        }

        RevivableThread.checkForInterruption();

        File file = null;
        if (!cachedFile.isEmpty()) {
            // Touch the cache entry; set the last access date to "now"
            cachedFile.setLastAccessDate();

            file = cachedFile.hasTemporaryData() ? cachedFile.getTemporaryFile() : cachedFile.getFile();

            RevivableThread.checkForInterruption();
            String errorMessage = cachedFile.getLatestErrorMessage();
            if (Utils.isNotBlank(errorMessage)) {
                logger.log(Level.WARNING, String.format("Error received from [%s](%s), %s", downloadedEntityName, urlStr, errorMessage));
            }

            // If we already know that something went wrong, rollback.
            if (Utils.isNotBlank(errorMessage) || file == null || !file.exists()) {
                RevivableThread.checkForInterruption();
                File rollbackFile = rollbackURLFile(logger, downloadedEntityName, configManager, file, urlStr, (String) null);
                if (rollbackFile != null && rollbackFile.exists()) {
                    file = rollbackFile;
                }
            }
        }

        return file;
    }

    /**
     * Follow redirections to find out if the entry at the end of the chain is approved.
     * @param applicationFolder
     * @param cachedFile
     * @return
     * @throws IOException
     * @throws JSONException
     */
    @Deprecated
    public static boolean isRecursiveApproved(File applicationFolder, CachedFile cachedFile)
            throws IOException, JSONException, RevivableThreadInterruptedException {

        return isRecursiveApproved(applicationFolder, cachedFile, 0);
    }
    @Deprecated
    private static boolean isRecursiveApproved(File applicationFolder, CachedFile cachedFile, int followRedirectionCount)
            throws IOException, JSONException, RevivableThreadInterruptedException {

        RevivableThread.checkForInterruption();

        String redirectionUrl = cachedFile.getRedirection();
        if (Utils.isBlank(redirectionUrl)) {
            return cachedFile.isApproved();
        }

        CachedFile redirectedCachedFile = getCachedFile(applicationFolder, redirectionUrl);
        if (redirectedCachedFile == null) {
            return false;
        }

        if (followRedirectionCount < MAX_FOLLOW_REDIRECTION) {
            return isRecursiveApproved(applicationFolder, redirectedCachedFile, followRedirectionCount + 1);
        } else {
            // Hopefully this error will never occurred
            LOGGER.log(Level.SEVERE, "Maximum URL follow reach. There is probably a cycle in the cache, which create potential infinite loops.");
            throw new IOException("Cycle in the cache follow URLs");
        }
    }

    @Deprecated
    public static void setRedirection(ConfigManager configManager, String invalidUrl, String craftedUrl)
            throws IOException, JSONException {

        File applicationFolder = getApplicationFolder(configManager);
        CachedFile cachedFile = getCachedFile(applicationFolder, invalidUrl);
        if (!cachedFile.isEmpty()) {
            cachedFile.setRedirection(craftedUrl);
        }
    }

    /**
     * Approve the last file sent for this URL. This has the effect
     * of replacing the current cached file with the last sent file.
     * @param urlStr
     */
    @Deprecated
    public static void commitURLFile(ConfigManager configManager, File approvedFile, String urlStr)
            throws IOException, JSONException {

        File applicationFolder = getApplicationFolder(configManager);
        CachedFile cachedFile = getCachedFile(applicationFolder, urlStr);

        if (!cachedFile.isEmpty()) {
            cachedFile.commit(approvedFile);
        }
    }

    /**
     * This cancel and delete the latest downloaded file and send
     * the previous downloaded file, which is the latest working
     * state of the file.
     * @param urlStr
     * @return
     */
    @Deprecated
    public static File rollbackURLFile(
            ThreadLogger logger,
            String downloadedEntityName,
            ConfigManager configManager,
            File unapprovedFile,
            String urlStr,
            Exception reason
    ) throws IOException, JSONException, RevivableThreadInterruptedException {
        return rollbackURLFile(logger, downloadedEntityName, configManager, unapprovedFile, urlStr, Utils.getExceptionMessage(reason));
    }

    @Deprecated
    public static File rollbackURLFile(
            ThreadLogger logger,
            String downloadedEntityName,
            ConfigManager configManager,
            File unapprovedFile,
            String urlStr,
            String reasonStr
    ) throws IOException, JSONException, RevivableThreadInterruptedException {

        File backupFile = unapprovedFile;
        File applicationFolder = getApplicationFolder(configManager);
        CachedFile cachedFile = getCachedFile(applicationFolder, urlStr);

        RevivableThread.checkForInterruption();

        if (!cachedFile.isEmpty()) {
            backupFile = cachedFile.rollback(unapprovedFile, reasonStr);
            // NOTE: The logger use MessageText to parse its message;
            //     Everything between "single quote" are interpreted as literal string.
            //     To print a "single quote", you have to use two "single quote".
            if (INVALID_FILE_CACHE_TIMEOUT >= 0) {
                logger.log(Level.WARNING, String.format("Invalid downloaded file: %s, the application won't try to re-download it for %s minutes.",
                        urlStr, "" + INVALID_FILE_CACHE_TIMEOUT));
            }
        }

        RevivableThread.checkForInterruption();

        if (backupFile != null && backupFile.exists()) {
            logger.log(Level.INFO, String.format("Invalid response received for [%s](%s), used last good cached file: %s",
                    downloadedEntityName, urlStr, backupFile.getName()));
        } else {
            logger.log(Level.WARNING, String.format("Invalid response received for [%s](%s), no cached version found.",
                    downloadedEntityName, urlStr));
        }

        RevivableThread.checkForInterruption();

        return backupFile;
    }

    @Deprecated
    private static ResponseStatus loadURLToFile(ThreadLogger logger, String urlStr, File file) throws RevivableThreadInterruptedException {
        return loadURLToFile(logger, urlStr, file, MAX_CACHED_FILE_SIZE);
    }

    @Deprecated
    private static ResponseStatus loadURLToFile(ThreadLogger logger, String urlStr, File file, int maxFileSizeMb) throws RevivableThreadInterruptedException {
        RevivableThread.checkForInterruption();

        ResponseStatus responseStatus = new ResponseStatus();

        URI uri = null;
        try {
            uri = Utils.toURL(urlStr).toURI();
        } catch (Exception ex) {
            responseStatus.setStatusCode(HttpStatus.SC_BAD_REQUEST);
            final String message = "Can not parse the URL: " + urlStr;
            responseStatus.setErrorMessage(message);
            logger.log(Level.WARNING, String.format("Can not parse the [URL](%s): %s", urlStr, Utils.getExceptionMessage(ex)), ex);
            return responseStatus;
        }

        HttpGet httpGet = null;
        InputStream in = null;
        FileOutputStream out = null;
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        try {
            httpClient = Utils.createHttpClient();
            httpGet = new HttpGet(uri);

            response = httpClient.execute(httpGet);
            RevivableThread.checkForInterruption();

            StatusLine httpStatus = response.getStatusLine();
            if (httpStatus != null) {
                responseStatus.setStatusCode(httpStatus.getStatusCode());
            }

            // The entity is streamed
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                long contentSizeMb = entity.getContentLength() / (1024*1024); // in megabytes
                // long value can go over 8 millions terabytes

                if (contentSizeMb < maxFileSizeMb) {
                    in = entity.getContent();
                    out = new FileOutputStream(file);
                    // The file size may be unknown on the server. This method stop streaming when the file size reach the limit.
                    Utils.binaryCopy(in, out, maxFileSizeMb * (1024*1024));
                } else {
                    logger.log(Level.WARNING, String.format("File size exceeded for URL %s. " +
                            "File size is %d Mb, expected less than %d Mb.", urlStr, entity.getContentLength(), maxFileSizeMb));
                    responseStatus.setErrorMessage("File size exceeded. File size is " + entity.getContentLength() + " Mb, expected less than " + maxFileSizeMb + " Mb.");
                }
            }
        } catch (IOException ex) {
            // An error occur while writing the file. It's not reliable. It's better to delete it.
            if (file != null && file.exists()) {
                file.delete();
            }
            final String message = String.format("Error reading [URL](%s), %s", uri, Utils.getExceptionMessage(ex));
            responseStatus.setErrorMessage(message);
            logger.log(Level.WARNING, message, ex);
        } finally {
            if (httpGet != null) {
                // Cancel the connection, if it's still alive
                httpGet.abort();
                // Close connections
                httpGet.reset();
            }
            if (in != null) {
                try { in.close(); } catch (Exception e) {
                    logger.log(Level.WARNING, String.format("Error occur while closing the [URL](%s): %s", uri, Utils.getExceptionMessage(e)), e);
                }
            }
            if (out != null) {
                try { out.close(); } catch (Exception e) {
                    logger.log(Level.WARNING, "Error occur while closing the file: " + Utils.getExceptionMessage(e), e);
                }
            }
            if (response != null) {
                try { response.close(); } catch (Exception e) {
                    logger.log(Level.WARNING, "Error occur while closing the HttpResponse: " + Utils.getExceptionMessage(e), e);
                }
            }
            if (httpClient != null) {
                try { httpClient.close(); } catch (Exception e) {
                    logger.log(Level.WARNING, "Error occur while closing the HttpClient: " + Utils.getExceptionMessage(e), e);
                }
            }
        }

        return responseStatus;
    }

    @Deprecated
    public static void saveDiskCacheMap(File applicationFolder) throws JSONException, IOException {
        File configFile = FileFinder.getDiskCacheFile(applicationFolder);
        if (diskCacheMap == null) {
            diskCacheMap = new JSONObject();
        }

        Writer writer = null;
        BufferedWriter bw = null;
        try {
            writer = new FileWriter(configFile);
            bw = new BufferedWriter(writer);
            String jsonStr = Utils.jsonToStr(diskCacheMap);

            if (Utils.isNotBlank(jsonStr)) {
                bw.write(jsonStr);
            }
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, String.format("Can not close the cache map buffered writer: %s", Utils.getExceptionMessage(e)), e);
                }
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, String.format("Can not close the cache map writer: %s", Utils.getExceptionMessage(e)), e);
                }
            }
        }
        loadedTime = new Date().getTime();
    }

    @Deprecated
    private static void loadDiskCacheMap(File applicationFolder) throws IOException, JSONException {
        File configFile = FileFinder.getDiskCacheFile(applicationFolder);

        Reader reader = null;
        try {
            reader = new FileReader(configFile);
            diskCacheMap = new JSONObject(new JSONTokener(reader));
        } catch(Exception ex) {
            diskCacheMap = new JSONObject();
            LOGGER.log(Level.SEVERE, "Can not load the cache map. The cache has been reset.", ex);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, String.format("Can not close the cache map reader: %s", Utils.getExceptionMessage(ex)), ex);
                }
            }
        }

        loadedTime = new Date().getTime();
        purgeCache(applicationFolder);
    }

    @Deprecated
    protected static JSONObject getDiskCacheMap(File applicationFolder) throws IOException, JSONException {
        if (diskCacheMap == null || (isDiskCacheIsExpired(applicationFolder))) {
            loadDiskCacheMap(applicationFolder);
        }
        return diskCacheMap;
    }

    @Deprecated
    public static void reloadDiskCacheMapIfNeeded(File applicationFolder) throws IOException, JSONException {
        if (diskCacheMap == null || (isDiskCacheIsExpired(applicationFolder))) {
            loadDiskCacheMap(applicationFolder);
        }
    }

    @Deprecated
    private static boolean isDiskCacheIsExpired(File applicationFolder) throws IOException {
        File configFile = FileFinder.getDiskCacheFile(applicationFolder);

        if (!configFile.exists()) {
            return true;
        }

        return loadedTime < configFile.lastModified();
    }

    @Deprecated
    public static JSONObject getJSONResponse(
            ThreadLogger logger,
            String downloadedEntityName,
            ConfigManager configManager,
            AbstractDataSourceConfig dataSource,
            String urlStr,
            Category category,
            boolean mandatory
    ) throws IOException, JSONException, RevivableThreadInterruptedException {

        File jsonFile = null;

        RevivableThread.checkForInterruption();

        JSONObject jsonResponse = null;
        try {
            jsonFile = getURLFile(logger, downloadedEntityName, configManager, dataSource, urlStr, category, mandatory);
            jsonResponse = parseFile(logger, jsonFile, urlStr);
            commitURLFile(configManager, jsonFile, urlStr);
        } catch(Exception ex) {
            File rollbackFile = rollbackURLFile(logger, downloadedEntityName, configManager, jsonFile, urlStr, ex);
            jsonResponse = parseFile(logger, rollbackFile, urlStr);
        }

        RevivableThread.checkForInterruption();

        return jsonResponse;
    }

    @Deprecated
    private static JSONObject parseFile(ThreadLogger logger, File jsonFile, String urlStr) {
        JSONObject jsonResponse = null;
        Reader reader = null;
        try {
            reader = new FileReader(jsonFile);
            jsonResponse = new JSONObject(new JSONTokener(reader));
        } catch(Exception ex) {
            logger.log(Level.WARNING, String.format("Can not load the [JSON Object](%s): %s",
                    urlStr, Utils.getExceptionMessage(ex)), ex);
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

    @Deprecated
    public static JSONObject getSearchJSONResponse(String urlStr, String referer) throws IOException, JSONException, URISyntaxException {
        ResponseWrapper response = getSearchCachedResponse(urlStr);

        if (response == null) {
            response = new ResponseWrapper();
            // Set the wrapper in the cache now, it will be filled before the end of the method
            setSearchCachedResponse(urlStr, response, referer);
        }

        if (response.jsonResponse == null) {
            response.jsonResponse = new JSONObject(getUncachedResponse(urlStr, referer));
        }

        return response.jsonResponse;
    }

    @Deprecated
    public static JSONArray getSearchJSONArrayResponse(String urlStr, String referer) throws IOException, JSONException, URISyntaxException {
        ResponseWrapper response = getSearchCachedResponse(urlStr);

        if (response == null) {
            response = new ResponseWrapper();
            // Set the wrapper in the cache now, it will be filled before the end of the method
            setSearchCachedResponse(urlStr, response, referer);
        }

        if (response.jsonArrayResponse == null) {
            response.jsonArrayResponse = new JSONArray(getUncachedResponse(urlStr, referer));
        }

        return response.jsonArrayResponse;
    }

    @Deprecated
    public static String getUncachedResponse(String urlStr, String referer) throws IOException, JSONException, URISyntaxException {
        URL url = Utils.toURL(urlStr);

        URLConnection connection = url.openConnection();
        if (Utils.isNotBlank(referer)) {
            connection.setRequestProperty("Referer", referer);
        }
        InputStream in = null;
        BufferedReader reader = null;
        StringBuilder sb = new StringBuilder();
        try {
            in = connection.getInputStream();
            if (in != null) {
                reader = new BufferedReader(new InputStreamReader(in));

                int cp;
                while ((cp = reader.read()) != -1) {
                    sb.append((char) cp);
                }
            }
        } finally {
            if (in != null) {
                try { in.close(); } catch(Exception e) {
                    LOGGER.log(Level.WARNING, String.format("Can not close the URL input stream: %s", Utils.getExceptionMessage(e)), e);
                }
            }
            if (reader != null) {
                try { reader.close(); } catch(Exception e) {
                    LOGGER.log(Level.WARNING, String.format("Can not close the URL reader: %s", Utils.getExceptionMessage(e)), e);
                }
            }
        }

        return sb.toString();
    }

    @Deprecated
    public static WMSCapabilities getWMSCapabilitiesResponse(
            ThreadLogger logger,
            ConfigManager configManager,
            String wmsVersion,
            AbstractDataSourceConfig dataSource,
            String urlStr,
            Category category,
            boolean mandatory) throws IOException, SAXException, JSONException, URISyntaxException, RevivableThreadInterruptedException {

        File capabilitiesFile = null;
        WMSCapabilities wmsCapabilities;

        RevivableThread.checkForInterruption();

        if (urlStr.startsWith("file://")) {
            // Local file URL
            capabilitiesFile = new File(new URI(urlStr));
            wmsCapabilities = URLCache.getCapabilities(capabilitiesFile);

        } else {
            // TODO Find a nicer way to detect if the URL is a complete URL to a GetCapabilities document
            if (!urlStr.contains("?")) {
                if (Utils.isBlank(wmsVersion)) {
                    wmsVersion = "1.3.0";
                }

                // URL pointing at a WMS service
                urlStr = Utils.addUrlParameter(urlStr, "SERVICE", "WMS");
                urlStr = Utils.addUrlParameter(urlStr, "REQUEST", "GetCapabilities");
                urlStr = Utils.addUrlParameter(urlStr, "VERSION", wmsVersion);
            }

            RevivableThread.checkForInterruption();

            try {
                capabilitiesFile = URLCache.getURLFile(logger, "WMS GetCapabilities document", configManager, dataSource, urlStr, category, mandatory);
                logger.log(Level.INFO, String.format("Parsing [WMS GetCapabilities document](%s)", urlStr));
                wmsCapabilities = URLCache.getCapabilities(capabilitiesFile);
                URLCache.commitURLFile(configManager, capabilitiesFile, urlStr);
            } catch (Exception ex) {
                logger.log(Level.WARNING, String.format("Error occurred while parsing the [WMS GetCapabilities document](%s): %s",
                        urlStr, Utils.getExceptionMessage(ex)), ex);
                File rollbackFile = URLCache.rollbackURLFile(logger, "WMS GetCapabilities document", configManager, capabilitiesFile, urlStr, ex);
                wmsCapabilities = URLCache.getCapabilities(rollbackFile);
            }
        }

        RevivableThread.checkForInterruption();

        return wmsCapabilities;
    }

    /**
     * GetCapabilities from a local file. The latest GeoTools library seems to only be good at
     * doing this using the HTTP protocol.
     * This method is a slightly modified copy of:
     *     org.geotools.data.wms.response.WMSGetCapabilitiesResponse(HTTPResponse response)
     * @param file
     * @return
     * @throws IOException
     * @throws ServiceException
     */
    @Deprecated
    private static WMSCapabilities getCapabilities(File file) throws IOException, SAXException, RevivableThreadInterruptedException {
        RevivableThread.checkForInterruption();

        if (file == null || !file.exists()) {
            return null;
        }

        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            return URLCache.getCapabilities(inputStream);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    @Deprecated
    private static WMSCapabilities getCapabilities(InputStream inputStream)
            throws SAXException, RevivableThreadInterruptedException {

        RevivableThread.checkForInterruption();

        Map<String, Object> hints = new HashMap<String, Object>();
        hints.put(DocumentHandler.DEFAULT_NAMESPACE_HINT_KEY, WMSSchema.getInstance());
        hints.put(DocumentFactory.VALIDATION_HINT, Boolean.FALSE);

        Object object = DocumentFactory.getInstance(inputStream, hints, Level.WARNING);
        RevivableThread.checkForInterruption();

        if (object instanceof ServiceException) {
            throw (ServiceException)object;
        }

        return (WMSCapabilities)object;
    }

    /**
     *
     * @param configManager
     * @param updateDataSources Use with unit tests only.
     * @throws IOException
     * @throws JSONException
     */
    @Deprecated
    protected static void deleteCache(ConfigManager configManager, boolean updateDataSources) throws IOException, JSONException {
        searchResponseCache.clear();

        File applicationFolder = configManager.getApplicationFolder();

        // Clear cached files
        if (applicationFolder == null) return;

        diskCacheMap = new JSONObject();
        File diskCacheFolder = FileFinder.getDiskCacheFolder(applicationFolder);

        File[] folders = diskCacheFolder.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });

        // Remove the files that are not listed in the cache map
        for (File folder : folders) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            folder.delete();
        }

        if (updateDataSources) {
            // Delete data source cached files
            MultiKeyHashMap<Integer, String, AbstractDataSourceConfig> dataSources = configManager.getDataSourceConfigs();
            AbstractDataSourceConfig dataSource = null;
            for (Map.Entry<Integer, AbstractDataSourceConfig> dataSourceEntry : dataSources.entrySet()) {
                dataSource = dataSourceEntry.getValue();
                dataSource.deleteCachedState();
            }
        }
    }

    @Deprecated
    public static void deleteCache(ConfigManager configManager, AbstractDataSourceConfig dataSource)
            throws JSONException, IOException {

        File applicationFolder = configManager.getApplicationFolder();

        if (dataSource == null || applicationFolder == null) {
            return;
        }
        String dataSourceId = dataSource.getDataSourceId();

        JSONObject jsonCache = getDiskCacheMap(applicationFolder);

        if (jsonCache != null && jsonCache.length() > 0) {
            List<String> urlsToDelete = new ArrayList<String>();
            Iterator<String> urls = jsonCache.keys();
            String url;
            while (urls.hasNext()) {
                url = urls.next();
                CachedFile cachedFile = getCachedFile(applicationFolder, url);
                if (cachedFile.isEmpty()) {
                    // Remove null entries - This should not happen
                    urlsToDelete.add(url);
                } else if (cachedFile.hasDataSourceId(dataSourceId)) {
                    // Flag entries associated with the data source to be deleted, if not associated with other data sources
                    File file = cachedFile.getFile();
                    if (file != null && file.exists()) {
                        file.delete();
                    }
                    File tmpFile = cachedFile.getTemporaryFile();
                    if (tmpFile != null && tmpFile.exists()) {
                        tmpFile.delete();
                    }

                    if (cachedFile.removeDataSourceId(dataSourceId)) {
                        urlsToDelete.add(url);
                    }
                }
            }

            if (!urlsToDelete.isEmpty()) {
                for (String urlToDelete : urlsToDelete) {
                    jsonCache.remove(urlToDelete);
                }
            }
        }

        // Delete the reminding files
        File diskCacheFolder = FileFinder.getDiskCacheFolder(applicationFolder);
        File folder = new File(diskCacheFolder, dataSourceId);
        if (folder.exists()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            folder.delete();
        }
    }

    @Deprecated
    public static void markCacheForReDownload(ConfigManager configManager, AbstractDataSourceConfig dataSource, boolean removeBrokenEntry, Category category)
            throws JSONException, IOException, RevivableThreadInterruptedException {

        RevivableThread.checkForInterruption();

        File applicationFolder = configManager.getApplicationFolder();

        if (dataSource == null || applicationFolder == null) {
            return;
        }
        String dataSourceId = dataSource.getDataSourceId();

        JSONObject jsonCache = getDiskCacheMap(applicationFolder);
        RevivableThread.checkForInterruption();

        if (jsonCache != null && jsonCache.length() > 0) {
            List<String> urlsToDelete = new ArrayList<String>();
            Iterator<String> urls = jsonCache.keys();
            String url;
            while (urls.hasNext()) {
                url = urls.next();
                CachedFile cachedFile = getCachedFile(applicationFolder, url);
                if (cachedFile.isEmpty()) {
                    // Remove null entries - This should not happen
                    urlsToDelete.add(url);
                } else if (cachedFile.hasDataSourceId(dataSourceId)) {
                    if (removeBrokenEntry && !isRecursiveApproved(applicationFolder, cachedFile)) {
                        cachedFile.setMarkedForReDownload(true);
                    } else if (category != null && (category.equals(Category.ALL) || cachedFile.getCategory() == null || category.equals(cachedFile.getCategory()))) {
                        cachedFile.setMarkedForReDownload(true);
                    }
                }
            }
            RevivableThread.checkForInterruption();

            if (!urlsToDelete.isEmpty()) {
                for (String urlToDelete : urlsToDelete) {
                    jsonCache.remove(urlToDelete);
                }
            }
        }

        RevivableThread.checkForInterruption();
    }

    @Deprecated
    public static void clearSearchCache(String urlStr) {
        searchResponseCache.remove(urlStr);
    }

    /**
     * Delete cached files that are not in the map and
     * delete map entry that represent deleted files.
     */
    @Deprecated
    public static void purgeCache(File applicationFolder) throws IOException, JSONException {
        if (applicationFolder == null) return;
        final File diskCacheFolder = FileFinder.getDiskCacheFolder(applicationFolder);
        final File diskFileCacheFolder = CachedFile.getCachedFileFolder(diskCacheFolder);

        JSONObject jsonCache = getDiskCacheMap(applicationFolder);

        // Remove the cache entry that are out of date
        if (jsonCache != null && jsonCache.length() > 0) {
            List<String> urlsToDelete = new ArrayList<String>();
            Iterator<String> urls = jsonCache.keys();
            String url;
            boolean hasChanged = false;
            while (urls.hasNext()) {
                url = urls.next();
                CachedFile cachedFile = getCachedFile(applicationFolder, url);
                if (cachedFile.isEmpty()) {
                    // Remove null entries
                    urlsToDelete.add(url);
                } else {
                    // Check if the file reach its expiry
                    Date downloadedTime = cachedFile.getDownloadedTime();
                    if (downloadedTime != null) {
                        long expiry = cachedFile.getExpiry();
                        if (expiry >= 0) {
                            long age = new Date().getTime() - downloadedTime.getTime();
                            if (age >= expiry * NB_MS_PER_MINUTE) {
                                urlsToDelete.add(url);
                            }
                        }
                    }
                }
            }

            if (!urlsToDelete.isEmpty()) {
                for (String urlToDelete : urlsToDelete) {
                    jsonCache.remove(urlToDelete);
                }
                hasChanged = true;
            }

            if (hasChanged) {
                saveDiskCacheMap(applicationFolder);
            }
        }

        // Remove the files that are not listed in the cache map
        File[] files = diskFileCacheFolder.listFiles();

        if (files != null) {
            for (File file : files) {
                String cachedFilename = file.getName();

                // Try to find the file in the cache
                boolean found = false;
                if (jsonCache != null && jsonCache.length() > 0) {
                    Iterator<String> urls = jsonCache.keys(); // reset the iterator
                    while (!found && urls.hasNext()) {
                        String url = urls.next();
                        CachedFile cachedFile = getCachedFile(applicationFolder, url);
                        if (!cachedFile.isEmpty()) {
                            if (cachedFilename.equals(cachedFile.getFilename())) {
                                found = true;
                            } else {
                                if (cachedFilename.equals(cachedFile.getTemporaryFilename())) {
                                    found = true;
                                }
                            }
                        }
                    }
                }

                if (!found) {
                    file.delete();
                }
            }
        }
    }

    @Deprecated
    public static void deleteOldEntries(AbstractDataSourceConfig dataSourceConfig, Date thresholdDate, List<URLCache.Category> categories)
            throws IOException, JSONException {

        if (thresholdDate != null && categories != null && !categories.isEmpty()) {
            File applicationFolder = dataSourceConfig.getConfigManager().getApplicationFolder();
            boolean allCategories = categories.contains(Category.ALL);
            String dataSourceId = dataSourceConfig.getDataSourceId();

            JSONObject jsonCache = getDiskCacheMap(applicationFolder);

            // Remove the cache entry that are out of date
            if (jsonCache != null && jsonCache.length() > 0) {
                List<String> urlsToDelete = new ArrayList<String>();
                Iterator<String> urls = jsonCache.keys();
                String url;
                while (urls.hasNext()) {
                    url = urls.next();
                    CachedFile cachedFile = getCachedFile(applicationFolder, url);
                    if (cachedFile.isEmpty()) {
                        // Remove null entries
                        urlsToDelete.add(url);
                    } else if (cachedFile.hasDataSourceId(dataSourceId)) {
                        // Check if the file has been access since the threshold date
                        // NOTE: There is more optimal ways to do this (all the condition in a if, without a boolean)
                        //     but I think it's easier to understand as it is.
                        Date accessDate = cachedFile.getLastAccessDate();
                        boolean hasBeenAccessed = false;
                        if (accessDate != null && accessDate.getTime() >= thresholdDate.getTime()) {
                            hasBeenAccessed = true;
                        }
                        if (!hasBeenAccessed) {
                            Category cachedFileCategory = cachedFile.getCategory();
                            if (allCategories || cachedFileCategory == null || categories.contains(cachedFileCategory)) {
                                if (cachedFile.removeDataSourceId(dataSourceId)) {
                                    urlsToDelete.add(url);
                                }
                            }
                        }
                    }
                }

                if (!urlsToDelete.isEmpty()) {
                    for (String urlToDelete : urlsToDelete) {
                        jsonCache.remove(urlToDelete);
                    }
                }
            }
        }
    }

    /**
     * Return the number of files contained by the cache folder.
     * This method is used by Unit Tests to ensure the URLCache do not leak.
     * @return
     */
    @Deprecated
    public static int countFile(File applicationFolder) {
        final File diskCacheFolder = FileFinder.getDiskCacheFolder(applicationFolder);
        final File diskCacheFileFolder = CachedFile.getCachedFileFolder(diskCacheFolder);
        if (diskCacheFileFolder == null || !diskCacheFileFolder.exists()) {
            return 0;
        }
        String[] files = diskCacheFileFolder.list();
        return files == null ? 0 : files.length;
    }

    @Deprecated
    protected static CachedFile getCachedFile(File applicationFolder, String urlStr)
            throws JSONException, IOException {

        JSONObject jsonCache = getDiskCacheMap(applicationFolder);

        final File diskCacheFolder = FileFinder.getDiskCacheFolder(applicationFolder);
        return new CachedFile(diskCacheFolder, jsonCache.optJSONObject(urlStr));
    }

    @Deprecated
    private static ResponseWrapper getSearchCachedResponse(String urlStr) {
        if (!searchResponseCache.containsKey(urlStr)) {
            return null;
        }

        ResponseWrapper response = searchResponseCache.get(urlStr);
        if (response == null) {
            return null;
        }

        long timeoutTimestamp = Utils.getCurrentTimestamp() - SEARCH_CACHE_TIMEOUT;
        if (response.timestamp <= timeoutTimestamp) {
            clearSearchCache(urlStr);
            return null;
        }

        return response;
    }

    @Deprecated
    private static void setSearchCachedResponse(String urlStr, ResponseWrapper response, String referer) {
        if (urlStr != null && response != null) {
            // Max cache size reach...
            if (searchResponseCache.size() >= SEARCH_CACHE_MAXSIZE) {
                // Delete the oldest entry
                Map.Entry<String, ResponseWrapper> oldestResponseEntry = null;
                for (Map.Entry<String, ResponseWrapper> responseEntry : searchResponseCache.entrySet()) {
                    if (oldestResponseEntry == null || responseEntry.getValue().timestamp < oldestResponseEntry.getValue().timestamp) {
                        oldestResponseEntry = responseEntry;
                    }
                }
                if (oldestResponseEntry != null) {
                    searchResponseCache.remove(oldestResponseEntry.getKey());
                }
            }
            searchResponseCache.put(urlStr, response);
        }
    }

    @Deprecated
    private static class ResponseWrapper {
        // List of dataSource that use this URL
        @Deprecated
        public Set<String> dataSourceIds;

        // Response; either json or wms (or both?)
        @Deprecated
        public JSONObject jsonResponse;
        @Deprecated
        public JSONArray jsonArrayResponse;
        @Deprecated
        public WMSCapabilities wmsResponse;

        // Log the creation time, to knows when it times out
        @Deprecated
        public long timestamp;

        @Deprecated
        public ResponseWrapper() {
            this.dataSourceIds = new HashSet<String>();
            this.jsonResponse = null;
            this.jsonArrayResponse = null;
            this.wmsResponse = null;
            this.timestamp = Utils.getCurrentTimestamp();
        }
    }

    @Deprecated
    public static class ResponseStatus {
        @Deprecated
        private Integer statusCode;
        @Deprecated
        private String errorMessage;

        @Deprecated
        public ResponseStatus() {
            this.statusCode = null;
            this.errorMessage = null;
        }

        @Deprecated
        public void setStatusCode(Integer statusCode) {
            this.statusCode = statusCode;
        }
        @Deprecated
        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }


        @Deprecated
        public Integer getStatusCode() {
            return this.statusCode;
        }

        @Deprecated
        public boolean isPageNotFound() {
            return this.statusCode != null && this.statusCode == 404;
        }

        @Deprecated
        public boolean isSuccess() {
            return this.statusCode != null && this.statusCode >= 200 && this.statusCode < 300;
        }

        @Deprecated
        public String getErrorMessage() {
            return this.errorMessage;
        }
    }

    /**
     * {
     *     url: {
     *         file: "path/to/the/file",
     *         dataSourceId: "ea",
     *         downloadedTime: "2012-09-24 14:06:49",
     *         expiry: 60, // In minutes
     *
     *         // Set when the file expired, the actual file is replace with this if it's approved by the application.
     *         tmpData: {
     *             file: "path/to/the/tmpFile",
     *             downloadedTime: "2012-09-24 15:07:34"
     *         }
     *     }
     * }
     * This class is protected to be used in URLCache class and in URLCacheTest class only.
     */
    @Deprecated
    protected static class CachedFile {
        // Date format: "2012-09-24 14:06:49"
        @Deprecated
        private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // Precise to the millisecond "2012-09-24 14:06:49:125"
        @Deprecated
        private static SimpleDateFormat lastAccessDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:S");

        @Deprecated
        JSONObject jsonCachedFile;
        @Deprecated
        File cacheFolder;

        /**
         * @param cacheFolder
         * @param dataSourceId
         * @param filename
         * @param downloadedTime
         * @param expiry
         * @param mandatory True to cancel the client generation if the file cause problem
         * @throws JSONException
         */
        @Deprecated
        public CachedFile(File cacheFolder, String dataSourceId, String filename, Category category, Date downloadedTime, int expiry, boolean mandatory) throws JSONException {
            this.cacheFolder = cacheFolder;

            this.jsonCachedFile = new JSONObject();
            if (dataSourceId != null) {
                this.addDataSourceId(dataSourceId);
            }
            this.setFilename(filename);
            this.setCategory(category);
            this.setDownloadedTime(downloadedTime);
            this.setExpiry(expiry);
            this.setMandatory(mandatory);
        }

        @Deprecated
        public CachedFile(File cacheFolder, JSONObject json) throws JSONException {
            this.cacheFolder = cacheFolder;
            if (json == null) {
                json = new JSONObject();
            }
            this.jsonCachedFile = json;
        }

        @Deprecated
        public JSONObject toJSON() {
            if (this.isEmpty()) {
                return null;
            }
            return this.jsonCachedFile;
        }

        @Deprecated
        public boolean isEmpty() {
            int nbIgnoredAttribute = 0;
            if (this.jsonCachedFile.has("lastAccessDate")) {
                nbIgnoredAttribute++;
            }

            return this.jsonCachedFile.length() <= nbIgnoredAttribute;
        }

        @Deprecated
        public static String generateFilename(File cacheFolder, String urlStr) {
            File folder = CachedFile.getCachedFileFolder(cacheFolder);

            String host = null;
            try {
                URL url = Utils.toURL(urlStr);
                host = url.getHost();
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Malformed URL: " + urlStr);
            }
            if (host == null) {
                host = "unknown";
            }

            String prefix = host.replace('.', '_').replaceAll("[^a-zA-Z0-9]", "-");
            String extension = ".bin";

            int counter = 0;
            String filename = prefix + extension;
            File testedFile = new File(folder, filename);
            while (testedFile.exists()) {
                counter++;
                filename = prefix + "_" + counter + extension;
                testedFile = new File(folder, filename);
            }

            return filename;
        }

        @Deprecated
        public String[] getDataSourceIds() throws JSONException {
            JSONArray dataSourceIds = this.jsonCachedFile.optJSONArray("dataSourceIds");
            if (dataSourceIds == null) {
                dataSourceIds = new JSONArray();
                this.jsonCachedFile.put("dataSourceIds", dataSourceIds);
            }

            int len = dataSourceIds.length();
            String[] dataSourceIdsArray = new String[len];
            for (int i=0; i<len; i++) {
                dataSourceIdsArray[i] = dataSourceIds.optString(i, null);
            }

            return dataSourceIdsArray;
        }
        @Deprecated
        public void addDataSourceId(String dataSourceId) throws JSONException {
            JSONArray dataSourceIds = this.jsonCachedFile.optJSONArray("dataSourceIds");
            if (dataSourceIds == null) {
                dataSourceIds = new JSONArray();
                this.jsonCachedFile.put("dataSourceIds", dataSourceIds);
            }

            dataSourceIds.put(dataSourceId);
        }

        /**
         *
         * @param dataSourceId
         * @return True if the list of data source is empty after the remove.
         */
        @Deprecated
        public boolean removeDataSourceId(String dataSourceId) {
            boolean isEmpty = true;
            JSONArray dataSourceIds = this.jsonCachedFile.optJSONArray("dataSourceIds");
            if (dataSourceIds != null) {
                int len = dataSourceIds.length();
                if (dataSourceId != null) {
                    for (int i=0; i<len; i++) {
                        if (dataSourceId.equals(dataSourceIds.optString(i, null))) {
                            dataSourceIds.remove(i);
                            i--;
                            len--;
                        }
                    }
                }
                isEmpty = len <= 0;
            }

            return isEmpty;
        }
        @Deprecated
        public boolean hasDataSourceId(String dataSourceId) {
            if (dataSourceId == null) { return false; }

            JSONArray dataSourceIds = this.jsonCachedFile.optJSONArray("dataSourceIds");
            if (dataSourceIds == null) { return false; }

            int len = dataSourceIds.length();
            for (int i=0; i<len; i++) {
                if (dataSourceId.equals(dataSourceIds.optString(i, null))) {
                    return true;
                }
            }

            return false;
        }

        @Deprecated
        public File getCachedFileFolder() {
            return CachedFile.getCachedFileFolder(this.cacheFolder);
        }
        @Deprecated
        private static File getCachedFileFolder(File cacheFolder) {
            File cacheFileFolder = (URLCache.CACHE_FILES_FOLDER == null || URLCache.CACHE_FILES_FOLDER.isEmpty() ?
                    cacheFolder :
                    new File(cacheFolder, URLCache.CACHE_FILES_FOLDER));

            cacheFileFolder.mkdirs();
            return cacheFileFolder;
        }

        @Deprecated
        public String getFilename() {
            return this.jsonCachedFile.optString("file", null);
        }
        @Deprecated
        public void setFilename(String file) throws JSONException {
            this.jsonCachedFile.put("file", file);
        }

        @Deprecated
        public Category getCategory() {
            String categoryStr = this.jsonCachedFile.optString("category", null);
            return categoryStr == null ? null : Category.valueOf(categoryStr);
        }
        public void setCategory(Category category) throws JSONException {
            this.jsonCachedFile.put("category", category.name());
        }

        @Deprecated
        public File getFile() {
            String filename = this.getFilename();
            if (filename == null) {
                return null;
            }
            return new File(this.getCachedFileFolder(), filename);
        }

        @Deprecated
        public Date getDownloadedTime() {
            String downloadedTimeStr = this.jsonCachedFile.optString("downloadedTime", null);
            if (downloadedTimeStr == null) {
                return null;
            }

            Date downloadedTime = null;
            try {
                downloadedTime = dateFormat.parse(downloadedTimeStr);
            } catch (ParseException e) {
                LOGGER.log(Level.WARNING, "Can not parse the downloaded time \"{0}\": {1}",
                        new String[]{ downloadedTimeStr, Utils.getExceptionMessage(e) });
                LOGGER.log(Level.FINE, "Stack trace: ", e);
            }

            return downloadedTime;
        }
        @Deprecated
        public void setDownloadedTime(Date downloadedTime) throws JSONException {
            this.jsonCachedFile.put("downloadedTime", dateFormat.format(downloadedTime));
        }

        @Deprecated
        public Date getLastAccessDate() {
            String dateStr = this.jsonCachedFile.optString("lastAccessDate", null);
            if (dateStr == null) {
                return null;
            }

            Date date = null;
            try {
                date = lastAccessDateFormat.parse(dateStr);
            } catch (ParseException e) {
                LOGGER.log(Level.WARNING, "Can not parse the last access time \"{0}\": {1}",
                        new String[]{ dateStr, Utils.getExceptionMessage(e) });
                LOGGER.log(Level.FINE, "Stack trace: ", e);
            }
            return date;
        }

        // Set last access date to "Now"
        @Deprecated
        public void setLastAccessDate() throws JSONException {
            this.setLastAccessDate(new Date());
        }
        @Deprecated
        public void setLastAccessDate(Date date) throws JSONException {
            this.setLastAccessDate(lastAccessDateFormat.format(date));
        }
        @Deprecated
        public void setLastAccessDate(String date) throws JSONException {
            this.jsonCachedFile.put("lastAccessDate", date);
        }

        @Deprecated
        public int getExpiry() {
            return this.jsonCachedFile.optInt("expiry", CACHE_TIMEOUT);
        }
        @Deprecated
        public void setExpiry(int expiry) throws JSONException {
            this.jsonCachedFile.put("expiry", expiry);
        }

        @Deprecated
        public boolean isMarkedForReDownload() {
            return this.jsonCachedFile.optBoolean("markedForReDownload", false);
        }
        @Deprecated
        public void setMarkedForReDownload(boolean markedForReDownload) throws JSONException {
            if (markedForReDownload) {
                this.jsonCachedFile.put("markedForReDownload", true);
            } else {
                // Missing = false.
                this.jsonCachedFile.remove("markedForReDownload");
            }
        }

        @Deprecated
        public Integer getHttpStatusCode() {
            if (!this.jsonCachedFile.has("httpStatusCode")) {
                return null;
            }
            return this.jsonCachedFile.optInt("httpStatusCode");
        }
        @Deprecated
        public void setHttpStatusCode(Integer statusCode) throws JSONException {
            if (statusCode == null) {
                this.jsonCachedFile.remove("httpStatusCode");
            } else {
                this.jsonCachedFile.put("httpStatusCode", statusCode);
            }
        }

        @Deprecated
        public boolean isApproved() {
            return this.jsonCachedFile.optBoolean("approved", false);
        }
        @Deprecated
        public void setApproved(boolean approved) throws JSONException {
            this.jsonCachedFile.put("approved", approved);
        }

        /**
         * Redirection do not refer to HTTP redirection, it refers to
         * URL that returns broken document, than are re-build (crafted)
         * to create a URL that provide a valid document.
         *
         * For example, GetCapabilities document often refers to MEST
         * records using TC211 mimetype, but actually refers to HTML pages.
         * When the file parsing fail (and it will fail since HTML can not
         * be parsed using a TC211 parser), the AtlasMapper try to craft a proper
         * TC211 MEST URL, than if the URL returns what we expect, it create
         * a redirection from the HTML URL to the valid MEST URL.
         * The next time the application sees the HTML URL, it will use the
         * file associated with the redirection URL instead of trying to
         * re-download unparsable the HTML file again.
         * @return
         */
        @Deprecated
        public String getRedirection() {
            return this.jsonCachedFile.optString("redirection", null);
        }
        @Deprecated
        public void setRedirection(String url) throws JSONException {
            this.jsonCachedFile.put("redirection", url);
        }

        @Deprecated
        public boolean isMandatory() {
            return this.jsonCachedFile.optBoolean("mandatory", false);
        }

        /**
         * @param mandatory True to cancel the client generation if the file cause problem
         * @throws JSONException
         */
        @Deprecated
        public void setMandatory(boolean mandatory) throws JSONException {
            this.jsonCachedFile.put("mandatory", mandatory);
        }

        @Deprecated
        public String getLatestErrorMessage() {
            return this.jsonCachedFile.optString("errorMsg", null);
        }
        @Deprecated
        public void setLatestErrorMessage(String errorMsg) throws JSONException {
            if (Utils.isBlank(errorMsg)) {
                this.jsonCachedFile.remove("errorMsg");
            } else {
                this.jsonCachedFile.put("errorMsg", errorMsg);
            }
        }

        @Deprecated
        public String getTemporaryFilename() {
            JSONObject jsonTmpFile = this.jsonCachedFile.optJSONObject("tmpData");
            if (jsonTmpFile == null) {
                return null;
            }
            return jsonTmpFile.optString("file", null);
        }
        @Deprecated
        public void setTemporaryFilename(String file) throws JSONException {
            JSONObject jsonTmpFile = this.jsonCachedFile.optJSONObject("tmpData");
            if (jsonTmpFile == null) {
                jsonTmpFile = new JSONObject();
                this.jsonCachedFile.put("tmpData", jsonTmpFile);
            }
            jsonTmpFile.put("file", file);
        }

        @Deprecated
        public File getTemporaryFile() {
            String temporaryFilename = this.getTemporaryFilename();
            if (temporaryFilename == null) {
                return null;
            }
            return new File(this.getCachedFileFolder(), temporaryFilename);
        }

        @Deprecated
        public Integer getTemporaryHttpStatusCode() {
            JSONObject jsonTmpFile = this.jsonCachedFile.optJSONObject("tmpData");
            if (jsonTmpFile == null) {
                return null;
            }
            if (!jsonTmpFile.has("httpStatusCode")) {
                return null;
            }
            return jsonTmpFile.optInt("httpStatusCode");
        }
        @Deprecated
        public void setTemporaryHttpStatusCode(Integer statusCode) throws JSONException {
            JSONObject jsonTmpFile = this.jsonCachedFile.optJSONObject("tmpData");
            if (jsonTmpFile == null) {
                jsonTmpFile = new JSONObject();
                this.jsonCachedFile.put("tmpData", jsonTmpFile);
            }
            if (statusCode == null) {
                jsonTmpFile.remove("httpStatusCode");
            } else {
                jsonTmpFile.put("httpStatusCode", statusCode);
            }
        }

        @Deprecated
        public boolean hasTemporaryData() {
            return this.jsonCachedFile.has("tmpData");
        }

        @Deprecated
        public void discardTemporaryData() {
            this.jsonCachedFile.remove("tmpData");
        }

        /**
         * Approve the last file sent for this URL. This has the effect
         * of replacing the current cached file with the last sent file.
         */
        @Deprecated
        public void commit(File approvedFile) throws JSONException {
            File oldFile = this.getFile();

            String tmpFilename = this.getTemporaryFilename();
            Integer tmpHttpStatusCode = this.getTemporaryHttpStatusCode();
            if (tmpFilename != null && approvedFile != null && tmpFilename.equals(approvedFile.getName())) {
                this.setFilename(tmpFilename);
                this.setHttpStatusCode(tmpHttpStatusCode);

                this.discardTemporaryData();

                // Clean the directory
                if (oldFile != null && oldFile.exists()) {
                    oldFile.delete();
                    this.cleanUpFilenames();
                }
            }

            // The file has been approved, reset the timeout
            this.setExpiry(URLCache.CACHE_TIMEOUT);
            this.setApproved(true);
        }

        /**
         * This cancel and delete the latest downloaded file and send
         * the previous downloaded file, which is the latest working
         * state of the file.
         * @return
         */
        @Deprecated
        public File rollback(File unapprovedFile, String errorMessage) throws JSONException {
            File rollbackFile = null;

            // If there is not already a logged error, log the new error.
            if (Utils.isBlank(this.getLatestErrorMessage()) && Utils.isNotBlank(errorMessage)) {
                this.setLatestErrorMessage(errorMessage);
            }

            // The latest downloaded file didn't work.
            if (this.hasTemporaryData()) {
                // A file has been previously downloaded for this URL. Send that file.
                File backupFile = this.getFile();

                // Clean-up the cache map - watch out for multi-threads; the temporary info may
                // has been written by an other thread. Only delete it if it's the one related
                // with the bad file.
                File tmpFile = this.getTemporaryFile();
                if ((tmpFile == null && unapprovedFile == null) || (tmpFile != null && tmpFile.equals(unapprovedFile))) {
                    this.discardTemporaryData();
                }

                // Send the previous version of the file... Hopefully it was better.
                rollbackFile = backupFile;
            }

            // Clean-up the directory - delete the bad file
            if (unapprovedFile != null && unapprovedFile.exists()) {
                // Delete the bad file, if it's not the approved file we already got for this URL
                // NOTE: if the application if properly used, an approved file should not get un-approved later
                //     so this case should only append during Unit tests; in other words, the bad file always
                //     get deleted.
                if (unapprovedFile.equals(this.getFile()) && this.isApproved()) {
                    // Should never happen elsewhere than in Unit tests
                    rollbackFile = unapprovedFile;
                } else {
                    // Normal behaviour
                    unapprovedFile.delete();
                    this.cleanUpFilenames();
                }
            }

            if (rollbackFile == null) {
                // There is no valid file for that URL.
                this.setApproved(false);
            }

            // Reduce the timeout to trigger a re-download soon.
            this.setExpiry(URLCache.INVALID_FILE_CACHE_TIMEOUT);

            return rollbackFile;
        }

        @Deprecated
        public void cleanUpFilenames() throws JSONException {
            File file = this.getFile();
            File tmpFile = this.getTemporaryFile();

            if (file != null && !file.exists()) {
                this.setFilename(null);
            }
            if (tmpFile != null && !tmpFile.exists()) {
                this.setTemporaryFilename(null);
            }
        }
    }

    @Deprecated
    public enum Category {
        @Deprecated
        ALL, // Used to clear all cache of a data source
        @Deprecated
        CAPABILITIES_DOCUMENT, // Capabilities document (WMS, ncWMS), JSON document (ARC Gis), ...
        @Deprecated
        MEST_RECORD, // For WMS layer that have a valid TC211 file associated with it
        @Deprecated
        BRUTEFORCE_MEST_RECORD // For WMS layers that have a TC211 file associated with a wrong mime type
    }
}
