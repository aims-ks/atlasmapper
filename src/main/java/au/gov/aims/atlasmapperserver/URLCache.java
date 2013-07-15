/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2011 Australian Institute of Marine Science
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

package au.gov.aims.atlasmapperserver;

import au.gov.aims.atlasmapperserver.collection.MultiKeyHashMap;
import au.gov.aims.atlasmapperserver.dataSourceConfig.AbstractDataSourceConfig;
import au.gov.aims.atlasmapperserver.servlet.FileFinder;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.impl.conn.SchemeRegistryFactory;
import org.geotools.data.ows.WMSCapabilities;
import org.geotools.data.wms.xml.WMSSchema;
import org.geotools.ows.ServiceException;
import org.geotools.xml.DocumentFactory;
import org.geotools.xml.XMLHandlerHints;
import org.geotools.xml.XMLSAXHandler;
import org.geotools.xml.handlers.DocumentHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
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
import java.io.StringReader;
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

// TODO IMPORTANT: The WHOLE class has to be synchronized (no one can access a method while one method is running)
public class URLCache {
	private static final Logger LOGGER = Logger.getLogger(URLCache.class.getName());

	protected static final long NB_MS_PER_MINUTE = 60000;

	// Cache timeout in millisecond
	// The response will be re-requested if the application request
	// information from it and its cached timestamp is older than this setting.
	protected static final int CACHE_TIMEOUT = -1; // In minutes; -1 = never times out (the application has to force harvest to re-download it)
	protected static final int INVALID_FILE_CACHE_TIMEOUT = -1; // In minutes; Invalid files are re-downloaded by checking a checkbox on the re-building of the data source.
	protected static final long SEARCH_CACHE_TIMEOUT = 60 * NB_MS_PER_MINUTE;
	protected static final long SEARCH_CACHE_MAXSIZE = 10; // Maximum search responses

	protected static final String CACHE_FILES_FOLDER = "files";
	protected static final int MAX_CACHED_FILE_SIZE = 50; // in megabytes (Mb)

	protected static final int MAX_FOLLOW_REDIRECTION = 50; // Maximum number of URL follow allowed. Over passing this amount will be considered as a cycle in the cache and will throw IOException.

	// HashMap<String urlString, ResponseWrapper response>
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
	private static JSONObject diskCacheMap = null;
	/**
	 * Reload the disk cache when the disk cache file is manually modified;
	 *     Every time the disk cache is accessed, the last modified date of the disk cache map file is
	 *     checked against this attribute. If the file is newer, that the file is reloaded.
	 *     TODO: Do not reload while modifying the disk cache in memory. Use a DB for better handling & thread safe
	 */
 	private static long loadedTime = -1;

	private static HttpClient httpClient = null;
	static {
		// Set a pool of multiple connections so more than one client can be generated simultaneously
		// See: http://stackoverflow.com/questions/12799006/how-to-solve-error-invalid-use-of-basicclientconnmanager-make-sure-to-release
		PoolingClientConnectionManager cxMgr = new PoolingClientConnectionManager(SchemeRegistryFactory.createDefault());
		cxMgr.setMaxTotal(100);
		cxMgr.setDefaultMaxPerRoute(20);

		httpClient = new DefaultHttpClient(cxMgr);
	}

	private static File getApplicationFolder(ConfigManager configManager) {
		if (configManager == null) {
			// Can be used for running the tests
			return new File(System.getProperty("java.io.tmpdir"));
		}
		return configManager.getApplicationFolder();
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
	public static File getURLFile(ConfigManager configManager, AbstractDataSourceConfig dataSource, String urlStr, Category category, boolean mandatory) throws IOException, JSONException {
		return getURLFile(configManager, dataSource, urlStr, category, mandatory, 0);
	}
	private static File getURLFile(ConfigManager configManager, AbstractDataSourceConfig dataSource, String urlStr, Category category, boolean mandatory, int followRedirectionCount) throws IOException, JSONException {
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
					return getURLFile(configManager, dataSource, redirectionUrl, category, mandatory, followRedirectionCount++);
				} else {
					// Hopefully this error will never occurred
					LOGGER.log(Level.SEVERE, "Maximum URL follow reach. There is probably a cycle in the cache, which create potential infinite loops.");
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

				LOGGER.log(Level.INFO, "\n### DOWNLOADING ### Re-download URL {0}\n", urlStr);

				ResponseStatus responseStatus = loadURLToFile(urlStr, tmpFile);
				cachedFile.setMarkedForReDownload(false);
				cachedFile.setTemporaryHttpStatusCode(responseStatus.getStatusCode());
				cachedFile.setLatestErrorMessage(responseStatus.getErrorMessage());
				cachedFile.cleanUpFilenames();
			}
		}

		// The URL is not present in the cache. Load it!
		if (cachedFile.isEmpty()) {
			String filename = CachedFile.generateFilename(cacheFolder, urlStr);

			cachedFile = new CachedFile(cacheFolder, dataSourceId, filename, category, new Date(), CACHE_TIMEOUT, mandatory);
			jsonCache.put(urlStr, cachedFile.toJSON());

			File file = new File(cachedFile.getCachedFileFolder(), filename);

			LOGGER.log(Level.INFO, "\n### DOWNLOADING ### URL {0}\n", urlStr);

			ResponseStatus responseStatus = loadURLToFile(urlStr, file);
			cachedFile.setHttpStatusCode(responseStatus.getStatusCode());
			cachedFile.setLatestErrorMessage(responseStatus.getErrorMessage());
			cachedFile.cleanUpFilenames();
			if (Utils.isNotBlank(responseStatus.getErrorMessage())) {
				cachedFile.setApproved(false);
			}
		}

		File file = null;
		if (!cachedFile.isEmpty()) {
			// Touch the cache entry; set the last access date to "now"
			cachedFile.setLastAccessDate();

			file = cachedFile.hasTemporaryData() ? cachedFile.getTemporaryFile() : cachedFile.getFile();

			// If we already know that something went wrong, rollback.
			if (Utils.isNotBlank(cachedFile.getLatestErrorMessage()) || file == null || !file.exists()) {
				file = rollbackURLFile(configManager, file, urlStr, (String) null);
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
	public static boolean isRecursiveApproved(File applicationFolder, CachedFile cachedFile) throws IOException, JSONException {
		return isRecursiveApproved(applicationFolder, cachedFile, 0);
	}
	private static boolean isRecursiveApproved(File applicationFolder, CachedFile cachedFile, int followRedirectionCount) throws IOException, JSONException {
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

	private static String getErrorMessage(Throwable ex) {
		String errorMsg = ex.getMessage();
		if (Utils.isBlank(errorMsg)) {
			Throwable cause = ex.getCause();
			if (cause != null) {
				errorMsg = getErrorMessage(cause);
			}
		}
		if (Utils.isBlank(errorMsg)) {
			errorMsg = "Unexpected error.";
		}
		return errorMsg;
	}

	public static void setRedirection(ConfigManager configManager, String invalidUrl, String craftedUrl) throws IOException, JSONException {
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
	public static void commitURLFile(ConfigManager configManager, File approvedFile, String urlStr) throws IOException, JSONException {
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
	public static File rollbackURLFile(ConfigManager configManager, File unapprovedFile, String urlStr, Exception reason) throws IOException, JSONException {
		return rollbackURLFile(configManager, unapprovedFile, urlStr, getErrorMessage(reason));
	}
	public static File rollbackURLFile(ConfigManager configManager, File unapprovedFile, String urlStr, String reasonStr) throws IOException, JSONException {
		File backupFile = unapprovedFile;
		File applicationFolder = getApplicationFolder(configManager);
		CachedFile cachedFile = getCachedFile(applicationFolder, urlStr);

		if (!cachedFile.isEmpty()) {
			backupFile = cachedFile.rollback(unapprovedFile, reasonStr);
			// NOTE: The logger use MessageText to parse its message;
			//     Everything between "single quote" are interpreted as literal string.
			//     To print a "single quote", you have to use two "single quote".
			if (INVALID_FILE_CACHE_TIMEOUT >= 0) {
				LOGGER.log(Level.INFO, "Invalid downloaded file: {0}\nThe application won''t try to re-download it for {1} minutes.",
						new String[]{ urlStr, "" + INVALID_FILE_CACHE_TIMEOUT });
			}
		}

		return backupFile;
	}

	private static ResponseStatus loadURLToFile(String urlStr, File file) {
		return loadURLToFile(urlStr, file, MAX_CACHED_FILE_SIZE);
	}

	private static ResponseStatus loadURLToFile(String urlStr, File file, int maxFileSizeMb) {
		ResponseStatus responseStatus = new ResponseStatus();

		URI uri = null;
		try {
			uri = Utils.toURL(urlStr).toURI();
		} catch (Exception ex) {
			responseStatus.setStatusCode(HttpStatus.SC_BAD_REQUEST);
			responseStatus.setErrorMessage("Can not parse the URL: " + urlStr);
			return responseStatus;
		}

		HttpGet httpGet = new HttpGet(uri);
		HttpEntity entity = null;
		InputStream in = null;
		FileOutputStream out = null;

		try {
			// Java DOC:
			//     http://hc.apache.org/httpcomponents-core-ga/httpcore/apidocs/index.html
			//     http://hc.apache.org/httpcomponents-client-ga/httpclient/apidocs/index.html
			// Example: http://hc.apache.org/httpcomponents-client-ga/tutorial/html/fundamentals.html#d5e37
			HttpResponse response = httpClient.execute(httpGet);

			StatusLine httpStatus = response.getStatusLine();
			if (httpStatus != null) {
				responseStatus.setStatusCode(httpStatus.getStatusCode());
			}

			// The entity is streamed
			entity = response.getEntity();
			if (entity != null) {
				long contentSizeMb = entity.getContentLength() / (1024*1024); // in megabytes
				// long value can go over 8 millions terabytes

				if (contentSizeMb < maxFileSizeMb) {
					in = entity.getContent();
					out = new FileOutputStream(file);
					// The file size may be unknown on the server. This method stop streaming when the file size reach the limit.
					Utils.binaryCopy(in, out, maxFileSizeMb * (1024*1024));
				} else {
					LOGGER.log(Level.WARNING, "File size exceeded for URL {0}\n" +
							"      File size is {1} Mb, expected less than {2} Mb.", new Object[]{urlStr, entity.getContentLength(), maxFileSizeMb});
					responseStatus.setErrorMessage("File size exceeded. File size is " + entity.getContentLength() + " Mb, expected less than " + maxFileSizeMb + " Mb.");
				}
			}
		} catch (IOException ex) {
			// An error occur while writing the file. It's not reliable. It's better to delete it.
			if (file != null && file.exists()) {
				file.delete();
			}
			responseStatus.setErrorMessage(getErrorMessage(ex));
		} finally {
			if (httpGet != null) {
				// Cancel the connection, if it's still alive
				httpGet.abort();
				// Close connections
				httpGet.reset();
			}
			if (in != null) {
				try { in.close(); } catch (Exception e) {
					LOGGER.log(Level.SEVERE, "Error occur while closing the URL: {0}", Utils.getExceptionMessage(e));
					LOGGER.log(Level.FINE, "Stack trace:", e);
				}
			}
			if (out != null) {
				try { out.close(); } catch (Exception e) {
					LOGGER.log(Level.SEVERE, "Error occur while closing the file: {0}", Utils.getExceptionMessage(e));
					LOGGER.log(Level.FINE, "Stack trace:", e);
				}
			}
		}

		return responseStatus;
	}

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
					LOGGER.log(Level.SEVERE, "Can not close the cache map buffered writer: {0}", Utils.getExceptionMessage(e));
					LOGGER.log(Level.FINE, "Stack trace:", e);
				}
			}
			if (writer != null) {
				try {
					writer.close();
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE, "Can not close the cache map writer: {0}", Utils.getExceptionMessage(e));
					LOGGER.log(Level.FINE, "Stack trace:", e);
				}
			}
		}
		loadedTime = new Date().getTime();
	}

	private static void loadDiskCacheMap(File applicationFolder) throws IOException, JSONException {
		File configFile = FileFinder.getDiskCacheFile(applicationFolder);

		Reader reader = null;
		try {
			reader = new FileReader(configFile);
			diskCacheMap = new JSONObject(new JSONTokener(reader));
		} catch(Exception ex) {
			diskCacheMap = new JSONObject();
			LOGGER.log(Level.SEVERE, "Can not load the cache map. The cache has been reset.");
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception ex) {
					LOGGER.log(Level.SEVERE, "Can not close the cache map reader: {0}", Utils.getExceptionMessage(ex));
					LOGGER.log(Level.FINE, "Stack trace:", ex);
				}
			}
		}

		loadedTime = new Date().getTime();
		purgeCache(applicationFolder);
	}

	protected static JSONObject getDiskCacheMap(File applicationFolder) throws IOException, JSONException {
		if (diskCacheMap == null || (isDiskCacheIsExpired(applicationFolder))) {
			loadDiskCacheMap(applicationFolder);
		}
		return diskCacheMap;
	}

	public static void reloadDiskCacheMapIfNeeded(File applicationFolder) throws IOException, JSONException {
		if (diskCacheMap == null || (isDiskCacheIsExpired(applicationFolder))) {
			loadDiskCacheMap(applicationFolder);
		}
	}

	private static boolean isDiskCacheIsExpired(File applicationFolder) throws IOException {
		File configFile = FileFinder.getDiskCacheFile(applicationFolder);

		if (!configFile.exists()) {
			return true;
		}

		return loadedTime < configFile.lastModified();
	}

	public static JSONObject getJSONResponse(ConfigManager configManager, AbstractDataSourceConfig dataSource, String urlStr, Category category, boolean mandatory) throws IOException, JSONException {
		File jsonFile = null;

		JSONObject jsonResponse = null;
		try {
			jsonFile = getURLFile(configManager, dataSource, urlStr, category, mandatory);
			jsonResponse = parseFile(jsonFile, urlStr);
			commitURLFile(configManager, jsonFile, urlStr);
		} catch(Exception ex) {
			File rollbackFile = rollbackURLFile(configManager, jsonFile, urlStr, ex);
			jsonResponse = parseFile(rollbackFile, urlStr);
		}

		return jsonResponse;
	}

	private static JSONObject parseFile(File jsonFile, String urlStr) {
		JSONObject jsonResponse = null;
		Reader reader = null;
		try {
			reader = new FileReader(jsonFile);
			jsonResponse = new JSONObject(new JSONTokener(reader));
		} catch(Exception ex) {
			LOGGER.log(Level.SEVERE, "Can not load the JSON Object returning from the URL {0}: {1}",
					new String[]{ urlStr, Utils.getExceptionMessage(ex) });
			LOGGER.log(Level.FINE, "Stack trace:", ex);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception ex) {
					LOGGER.log(Level.SEVERE, "Can not close the JSON file {0}: {1}",
							new String[]{ jsonFile.getAbsoluteFile().getAbsolutePath(), Utils.getExceptionMessage(ex) });
					LOGGER.log(Level.FINE, "Stack trace:", ex);
				}
			}
		}

		return jsonResponse;
	}

	public static JSONObject getSearchJSONResponse(String urlStr) throws IOException, JSONException, URISyntaxException {
		ResponseWrapper response = getSearchCachedResponse(urlStr);

		if (response == null) {
			response = new ResponseWrapper();
			// Set the wrapper in the cache now, it will be filled before the end of the method
			setSearchCachedResponse(urlStr, response);
		}

		if (response.jsonResponse == null) {
			LOGGER.log(Level.INFO, "\n### DOWNLOADING ### JSON Document {0}\n",
					new String[]{ urlStr });

			response.jsonResponse = new JSONObject(getUncachedResponse(urlStr));
		}

		return response.jsonResponse;
	}

	public static JSONArray getSearchJSONArrayResponse(String urlStr) throws IOException, JSONException, URISyntaxException {
		ResponseWrapper response = getSearchCachedResponse(urlStr);

		if (response == null) {
			response = new ResponseWrapper();
			// Set the wrapper in the cache now, it will be filled before the end of the method
			setSearchCachedResponse(urlStr, response);
		}

		if (response.jsonArrayResponse == null) {
			LOGGER.log(Level.INFO, "\n### DOWNLOADING ### JSON Document {0}\n",
					new String[]{ urlStr });

			response.jsonArrayResponse = new JSONArray(getUncachedResponse(urlStr));
		}

		return response.jsonArrayResponse;
	}

	public static String getUncachedResponse(String urlStr) throws IOException, JSONException, URISyntaxException {
		URL url = Utils.toURL(urlStr);

		URLConnection connection = url.openConnection();
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
					LOGGER.log(Level.WARNING, "Can not close the URL input stream: {0}", Utils.getExceptionMessage(e));
					LOGGER.log(Level.FINE, "Stack trace:", e);
				}
			}
			if (reader != null) {
				try { reader.close(); } catch(Exception e) {
					LOGGER.log(Level.WARNING, "Can not close the URL reader: {0}", Utils.getExceptionMessage(e));
					LOGGER.log(Level.FINE, "Stack trace:", e);
				}
			}
		}

		return sb.toString();
	}


	public static Errors getDataSourceErrors(AbstractDataSourceConfig dataSourceConfig, File applicationFolder) throws IOException, JSONException {
		File diskCacheFolder = FileFinder.getDiskCacheFolder(applicationFolder);
		JSONObject jsonCache = getDiskCacheMap(applicationFolder);

		// Collect warnings
		Errors errors = new Errors();

		// Add errors reported by the disk cache utility (filter by specified data source)
		if (jsonCache != null && jsonCache.length() > 0) {
			Iterator<String> urls = jsonCache.keys();
			String url;
			boolean hasChanged = false;
			while (urls.hasNext()) {
				url = urls.next();
				CachedFile cachedFile = getCachedFile(applicationFolder, url);
				// Ignore empty entries (should not have any) or MEST entry concerning brute force records (these produce heaps of useless warnings)
				if (!cachedFile.isEmpty() && !Category.BRUTEFORCE_MEST_RECORD.equals(cachedFile.getCategory())) {
					String errorMsg = cachedFile.getLatestErrorMessage();
					if (Utils.isNotBlank(errorMsg)) {
						for (String dataSourceId : cachedFile.getDataSourceIds()) {
							if (dataSourceConfig.getDataSourceId().equals(dataSourceId)) {
								if (cachedFile.isMandatory()) {
									errors.addError(url, errorMsg);
								} else {
									errors.addWarning(url, errorMsg);
								}
							}
						}
					}
				}
			}
		}

		return errors;
	}

	public static WMSCapabilities getWMSCapabilitiesResponse(
			ConfigManager configManager,
			String wmsVersion,
			AbstractDataSourceConfig dataSource,
			String urlStr,
			Category category,
			boolean mandatory) throws IOException, SAXException, JSONException, URISyntaxException {

		File capabilitiesFile = null;
		WMSCapabilities wmsCapabilities;

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

			try {
				capabilitiesFile = URLCache.getURLFile(configManager, dataSource, urlStr, category, mandatory);
				wmsCapabilities = URLCache.getCapabilities(capabilitiesFile);
				URLCache.commitURLFile(configManager, capabilitiesFile, urlStr);
			} catch (Exception ex) {
				File rollbackFile = URLCache.rollbackURLFile(configManager, capabilitiesFile, urlStr, ex);
				wmsCapabilities = URLCache.getCapabilities(rollbackFile);
			}
		}

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
	private static WMSCapabilities getCapabilities(File file) throws IOException, SAXException {
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

	private static WMSCapabilities getCapabilities(InputStream inputStream) throws IOException, SAXException {
		Map<String, Object> hints = new HashMap<String, Object>();
		hints.put(DocumentHandler.DEFAULT_NAMESPACE_HINT_KEY, WMSSchema.getInstance());
		hints.put(DocumentFactory.VALIDATION_HINT, Boolean.FALSE);

		Object object = DocumentFactory.getInstance(inputStream, hints, Level.WARNING);

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

	public static void deleteCache(ConfigManager configManager, AbstractDataSourceConfig dataSource) throws JSONException, IOException {
		File applicationFolder = configManager.getApplicationFolder();

		if (dataSource == null || applicationFolder == null) {
			return;
		}
		String dataSourceId = (dataSource == null ? null : dataSource.getDataSourceId());

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

	public static void markCacheForReDownload(ConfigManager configManager, AbstractDataSourceConfig dataSource, boolean removeBrokenEntry, Category category) throws JSONException, IOException {
		File applicationFolder = configManager.getApplicationFolder();

		if (dataSource == null || applicationFolder == null) {
			return;
		}
		String dataSourceId = (dataSource == null ? null : dataSource.getDataSourceId());

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
					if (removeBrokenEntry && !isRecursiveApproved(applicationFolder, cachedFile)) {
						cachedFile.setMarkedForReDownload(true);
					} else if (category != null && (category.equals(Category.ALL) || cachedFile.getCategory() == null || category.equals(cachedFile.getCategory()))) {
						cachedFile.setMarkedForReDownload(true);
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

	public static void clearSearchCache(String urlStr) {
		searchResponseCache.remove(urlStr);
	}

	/**
	 * Delete cached files that are not in the map and
	 * delete map entry that represent deleted files.
	 */
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

	public static void deleteOldEntries(AbstractDataSourceConfig dataSourceConfig, Date thresholdDate, List<URLCache.Category> categories) throws IOException, JSONException {
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
	public static int countFile(File applicationFolder) {
		final File diskCacheFolder = FileFinder.getDiskCacheFolder(applicationFolder);
		final File diskCacheFileFolder = CachedFile.getCachedFileFolder(diskCacheFolder);
		if (diskCacheFileFolder == null || !diskCacheFileFolder.exists()) {
			return 0;
		}
		String[] files = diskCacheFileFolder.list();
		return files == null ? 0 : files.length;
	}

	protected static CachedFile getCachedFile(File applicationFolder, String urlStr) throws JSONException, IOException {
		JSONObject jsonCache = getDiskCacheMap(applicationFolder);

		final File diskCacheFolder = FileFinder.getDiskCacheFolder(applicationFolder);
		return new CachedFile(diskCacheFolder, jsonCache.optJSONObject(urlStr));
	}

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

	private static void setSearchCachedResponse(String urlStr, ResponseWrapper response) {
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

	private static class ResponseWrapper {
		// List of dataSource that use this URL
		public Set<String> dataSourceIds;

		// Response; either json or wms (or both?)
		public JSONObject jsonResponse;
		public JSONArray jsonArrayResponse;
		public WMSCapabilities wmsResponse;

		// Log the creation time, to knows when it times out
		public long timestamp;

		public ResponseWrapper() {
			this.dataSourceIds = new HashSet<String>();
			this.jsonResponse = null;
			this.jsonArrayResponse = null;
			this.wmsResponse = null;
			this.timestamp = Utils.getCurrentTimestamp();
		}
	}

	private static class ResponseStatus {
		private Integer statusCode;
		private String errorMessage;

		public ResponseStatus() {
			this.statusCode = null;
			this.errorMessage = null;
		}

		public void setStatusCode(Integer statusCode) {
			this.statusCode = statusCode;
		}
		public void setErrorMessage(String errorMessage) {
			this.errorMessage = errorMessage;
		}


		public Integer getStatusCode() {
			return this.statusCode;
		}
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
	protected static class CachedFile {
		// Date format: "2012-09-24 14:06:49"
		private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		// Precise to the millisecond "2012-09-24 14:06:49:125"
		private static SimpleDateFormat lastAccessDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:S");

		JSONObject jsonCachedFile;
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

		public CachedFile(File cacheFolder, JSONObject json) throws JSONException {
			this.cacheFolder = cacheFolder;
			if (json == null) {
				json = new JSONObject();
			}
			this.jsonCachedFile = json;
		}

		public JSONObject toJSON() {
			if (this.isEmpty()) {
				return null;
			}
			return this.jsonCachedFile;
		}

		public boolean isEmpty() {
			int nbIgnoredAttribute = 0;
			if (this.jsonCachedFile.has("lastAccessDate")) {
				nbIgnoredAttribute++;
			}

			return this.jsonCachedFile.length() <= nbIgnoredAttribute;
		}

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

		public File getCachedFileFolder() {
			return CachedFile.getCachedFileFolder(this.cacheFolder);
		}
		private static File getCachedFileFolder(File cacheFolder) {
			File cacheFileFolder = (URLCache.CACHE_FILES_FOLDER == null || URLCache.CACHE_FILES_FOLDER.isEmpty() ? cacheFolder : new File(cacheFolder, URLCache.CACHE_FILES_FOLDER));
			cacheFileFolder.mkdirs();
			return cacheFileFolder;
		}

		public String getFilename() {
			return this.jsonCachedFile.optString("file", null);
		}
		public void setFilename(String file) throws JSONException {
			this.jsonCachedFile.put("file", file);
		}

		public Category getCategory() {
			String categoryStr = this.jsonCachedFile.optString("category", null);
			return categoryStr == null ? null : Category.valueOf(categoryStr);
		}
		public void setCategory(Category category) throws JSONException {
			this.jsonCachedFile.put("category", category.name());
		}

		public File getFile() {
			String filename = this.getFilename();
			if (filename == null) {
				return null;
			}
			return new File(this.getCachedFileFolder(), filename);
		}

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
		public void setDownloadedTime(Date downloadedTime) throws JSONException {
			this.jsonCachedFile.put("downloadedTime", dateFormat.format(downloadedTime));
		}

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
		public void setLastAccessDate() throws JSONException {
			this.setLastAccessDate(new Date());
		}
		public void setLastAccessDate(Date date) throws JSONException {
			this.setLastAccessDate(lastAccessDateFormat.format(date));
		}
		public void setLastAccessDate(String date) throws JSONException {
			this.jsonCachedFile.put("lastAccessDate", date);
		}

		public int getExpiry() {
			return this.jsonCachedFile.optInt("expiry", CACHE_TIMEOUT);
		}
		public void setExpiry(int expiry) throws JSONException {
			this.jsonCachedFile.put("expiry", expiry);
		}

		public boolean isMarkedForReDownload() {
			return this.jsonCachedFile.optBoolean("markedForReDownload", false);
		}
		public void setMarkedForReDownload(boolean markedForReDownload) throws JSONException {
			if (markedForReDownload) {
				this.jsonCachedFile.put("markedForReDownload", true);
			} else {
				// Missing = false.
				this.jsonCachedFile.remove("markedForReDownload");
			}
		}

		public Integer getHttpStatusCode() {
			if (!this.jsonCachedFile.has("httpStatusCode")) {
				return null;
			}
			return this.jsonCachedFile.optInt("httpStatusCode");
		}
		public void setHttpStatusCode(Integer statusCode) throws JSONException {
			if (statusCode == null) {
				this.jsonCachedFile.remove("httpStatusCode");
			} else {
				this.jsonCachedFile.put("httpStatusCode", statusCode);
			}
		}

		public boolean isApproved() {
			return this.jsonCachedFile.optBoolean("approved", false);
		}
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
		public String getRedirection() {
			return this.jsonCachedFile.optString("redirection", null);
		}
		public void setRedirection(String url) throws JSONException {
			this.jsonCachedFile.put("redirection", url);
		}

		public boolean isMandatory() {
			return this.jsonCachedFile.optBoolean("mandatory", false);
		}

		/**
		 * @param mandatory True to cancel the client generation if the file cause problem
		 * @throws JSONException
		 */
		public void setMandatory(boolean mandatory) throws JSONException {
			this.jsonCachedFile.put("mandatory", mandatory);
		}

		public String getLatestErrorMessage() {
			return this.jsonCachedFile.optString("errorMsg", null);
		}
		public void setLatestErrorMessage(String errorMsg) throws JSONException {
			if (Utils.isBlank(errorMsg)) {
				this.jsonCachedFile.remove("errorMsg");
			} else {
				this.jsonCachedFile.put("errorMsg", errorMsg);
			}
		}

		public String getTemporaryFilename() {
			JSONObject jsonTmpFile = this.jsonCachedFile.optJSONObject("tmpData");
			if (jsonTmpFile == null) {
				return null;
			}
			return jsonTmpFile.optString("file", null);
		}
		public void setTemporaryFilename(String file) throws JSONException {
			JSONObject jsonTmpFile = this.jsonCachedFile.optJSONObject("tmpData");
			if (jsonTmpFile == null) {
				jsonTmpFile = new JSONObject();
				this.jsonCachedFile.put("tmpData", jsonTmpFile);
			}
			jsonTmpFile.put("file", file);
		}

		public File getTemporaryFile() {
			String temporaryFilename = this.getTemporaryFilename();
			if (temporaryFilename == null) {
				return null;
			}
			return new File(this.getCachedFileFolder(), temporaryFilename);
		}

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

		public boolean hasTemporaryData() {
			return this.jsonCachedFile.has("tmpData");
		}

		public void discardTemporaryData() {
			this.jsonCachedFile.remove("tmpData");
		}

		/**
		 * Approve the last file sent for this URL. This has the effect
		 * of replacing the current cached file with the last sent file.
		 */
		public void commit(File approvedFile) throws IOException, JSONException {
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
		public File rollback(File unapprovedFile, String errorMessage) throws IOException, JSONException {
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

	public enum Category {
		ALL, // Used to clear all cache of a data source
		CAPABILITIES_DOCUMENT, // Capabilities document (WMS, ncWMS), JSON document (ARC Gis), ...
		MEST_RECORD, // For WMS layer that have a valid TC211 file associated with it
		BRUTEFORCE_MEST_RECORD // For WMS layers that have a TC211 file associated with a wrong mime type
	}
}
