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

import au.gov.aims.atlasmapperserver.dataSourceConfig.AbstractDataSourceConfig;
import au.gov.aims.atlasmapperserver.servlet.FileFinder;
import org.geotools.data.ows.HTTPClient;
import org.geotools.data.ows.HTTPResponse;
import org.geotools.data.ows.MultithreadedHttpClient;
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

// TODO IMPORTANT: The WHOLE class has to be synchronized (no one can access a method while one method is running)
// TODO IMPORTANT: Save caching files to disk! (GetCapabilities can be quite larges, having multiple of them hold in memory can kill the server)
public class URLCache {
	private static final Logger LOGGER = Logger.getLogger(URLCache.class.getName());

	protected static final long NB_MS_PER_MINUTE = 60000;
	protected static final long NB_MS_PER_HOUR = 60 * NB_MS_PER_MINUTE;
	protected static final long NB_MS_PER_DAY = 24 * NB_MS_PER_HOUR;

	// Cache timeout in millisecond
	// The response will be re-requested if the application request
	// information from it and its cached timestamp is older than this setting.
	protected static final int CACHE_TIMEOUT = 7; // In days
	protected static final long SEARCH_CACHE_TIMEOUT = 60 * NB_MS_PER_MINUTE;
	protected static final long SEARCH_CACHE_MAXSIZE = 10; // Maximum search responses

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

	private static HTTPClient httpClient = new MultithreadedHttpClient();

	private static File getApplicationFolder(AbstractDataSourceConfig dataSource) {
		if (dataSource == null) {
			// Can be used for running the tests
			return new File(System.getProperty("java.io.tmpdir"));
		}
		return dataSource.getConfigManager().getApplicationFolder();
	}

	public static File getURLFile(AbstractDataSourceConfig dataSource, String urlStr) throws IOException, JSONException {
		File applicationFolder = getApplicationFolder(dataSource);
		String dataSourceId = (dataSource == null ? "TMP" : dataSource.getDataSourceId());

		Boolean cachingDisabled = (dataSource == null ? null : dataSource.isCachingDisabled());
		if (cachingDisabled == null) {
			cachingDisabled = false;
		}

		if (diskCacheMap == null) {
			loadDiskCacheMap(applicationFolder);
		}

		File cacheFolder = FileFinder.getDiskCacheFolder(applicationFolder);
		CachedFile cachedFile = new CachedFile(cacheFolder, diskCacheMap.optJSONObject(urlStr));

		boolean hasChanged = false;

		// Check if the disk cache is valid
		if (!cachedFile.isEmpty()) {
			// Check if the file exists and is readable
			File cache = cachedFile.getFile();

			if (cache == null || !cache.exists() || !cache.canRead()) {
				diskCacheMap.remove(urlStr);
				hasChanged = true;
				cachedFile = null;
			} else {
				// Check if the file reach its expiry
				Date downloadedTime = null;
				downloadedTime = cachedFile.getDownloadedTime();

				if (downloadedTime != null) {
					long expiry = cachedFile.getExpiry();
					if (expiry >= 0) {
						long age = new Date().getTime() - downloadedTime.getTime();
						if (age >= expiry * NB_MS_PER_DAY || cachingDisabled) {
							String tmpFilename = CachedFile.generateFilename(cacheFolder, dataSourceId, urlStr);
							try {
								LOGGER.log(Level.INFO, "\n### DOWNLOADING ### Expired URL {0}\n", urlStr);
								loadURLToFile(urlStr, new File(cachedFile.getCachedFileFolder(), tmpFilename));

								cachedFile.setTemporaryFilename(tmpFilename);
								cachedFile.setTemporaryDownloadedTime(new Date());
								hasChanged = true;
							} catch (Exception ex) {
								LOGGER.log(Level.SEVERE, "Can not load the URL {0}\nError message: {1}", new String[]{ urlStr, ex.getMessage() });
								LOGGER.log(Level.INFO, "Stacktrace: ", ex);
							}
						}
					}
				}
			}
		}

		// The URL is not present in the cache. Load it!
		if (cachedFile == null || cachedFile.isEmpty()) {
			String filename = CachedFile.generateFilename(cacheFolder, dataSourceId, urlStr);
			try {
				LOGGER.log(Level.INFO, "\n### DOWNLOADING ### URL {0}\n", new String[]{urlStr});
				cachedFile = new CachedFile(cacheFolder, dataSourceId, filename, new Date(), CACHE_TIMEOUT);
				loadURLToFile(urlStr, new File(cachedFile.getCachedFileFolder(), filename));

				diskCacheMap.put(urlStr, cachedFile.toJSON());
				hasChanged = true;
			} catch (Exception ex) {
				LOGGER.log(Level.SEVERE, "Can not load the URL {0}\nError message: {1}", new String[]{ urlStr, ex.getMessage() });
				LOGGER.log(Level.INFO, "Stacktrace: ", ex);
			}
		}

		// The URL could not be loaded... There is nothing I can do about it...
		if (cachedFile == null || cachedFile.isEmpty()) {
			return null;
		}

		if (hasChanged) {
			saveDiskCacheMap(applicationFolder);
		}

		File file = cachedFile.getTemporaryFile();
		if (file == null || !file.exists()) {
			file = cachedFile.getFile();
		}

		if (file == null || !file.exists()) {
			return null;
		}
		return file;
	}

	/**
	 * Approve the last file sent for this URL. This has the effect
	 * of replacing the current cached file with the last sent file.
	 * @param urlStr
	 */
	public static void commitURLFile(AbstractDataSourceConfig dataSource, File approvedFile, String urlStr) throws IOException, JSONException {
		File applicationFolder = getApplicationFolder(dataSource);

		if (diskCacheMap == null) {
			loadDiskCacheMap(applicationFolder);
		}

		File cacheFolder = FileFinder.getDiskCacheFolder(applicationFolder);
		CachedFile cachedFile = new CachedFile(cacheFolder, diskCacheMap.optJSONObject(urlStr));

		if (!cachedFile.isEmpty()) {
			cachedFile.commit(approvedFile);
			saveDiskCacheMap(applicationFolder);
		}
	}

	/**
	 * This cancel and delete the latest downloaded file and send
	 * the previous downloaded file, which is the latest working
	 * state of the file.
	 * @param urlStr
	 * @return
	 */
	public static File rollbackURLFile(AbstractDataSourceConfig dataSource, File unapprovedFile, String urlStr) throws IOException, JSONException {
		File backupFile = unapprovedFile;
		File applicationFolder = getApplicationFolder(dataSource);

		if (diskCacheMap == null) {
			loadDiskCacheMap(applicationFolder);
		}

		File cacheFolder = FileFinder.getDiskCacheFolder(applicationFolder);
		CachedFile cachedFile = new CachedFile(cacheFolder, diskCacheMap.optJSONObject(urlStr));

		if (!cachedFile.isEmpty()) {
			backupFile = cachedFile.rollback(unapprovedFile);
			saveDiskCacheMap(applicationFolder);
		}

		return backupFile;
	}

	private static void loadURLToFile(String urlStr, File file) throws IOException {
		URL url = Utils.toURL(urlStr);
		HTTPResponse response = null;
		InputStream in = null;
		FileOutputStream out = null;
		try {
			response = httpClient.get(url);
			in = response.getResponseStream();
			out = new FileOutputStream(file);

			Utils.binaryCopy(in, out);
		} finally {
			if (response != null) {
				try { response.dispose(); } catch (Exception e) {
					LOGGER.log(Level.SEVERE, "Error occur while disposing of the HTTPClient response");
					LOGGER.log(Level.INFO, "Stack trace:", e);
				}
			}
			if (in != null) {
				try { in.close(); } catch (Exception e) {
					LOGGER.log(Level.SEVERE, "Error occur while closing the URL");
					LOGGER.log(Level.INFO, "Stack trace:", e);
				}
			}
			if (out != null) {
				try { out.close(); } catch (Exception e) {
					LOGGER.log(Level.SEVERE, "Error occur while closing the file");
					LOGGER.log(Level.INFO, "Stack trace:", e);
				}
			}
		}
	}

	private static void saveDiskCacheMap(File applicationFolder) throws JSONException, IOException {
		File configFile = FileFinder.getDiskCacheFile(applicationFolder);
		JSONObject jsonCache = diskCacheMap == null ? new JSONObject() : diskCacheMap;

		Writer writer = null;
		BufferedWriter bw = null;
		try {
			writer = new FileWriter(configFile);
			bw = new BufferedWriter(writer);
			String jsonStr = Utils.jsonToStr(jsonCache);
			if (Utils.isNotBlank(jsonStr)) {
				bw.write(jsonStr);
			}
		} finally {
			if (bw != null) {
				try {
					bw.close();
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE, "Can not close the cache map buffered writer.");
					LOGGER.log(Level.INFO, "Stack trace:", e);
				}
			}
			if (writer != null) {
				try {
					writer.close();
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE, "Can not close the cache map writer.");
					LOGGER.log(Level.INFO, "Stack trace:", e);
				}
			}
		}

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
			LOGGER.log(Level.INFO, "Stack trace:", ex);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception ex) {
					LOGGER.log(Level.SEVERE, "Can not close the cache map reader.");
					LOGGER.log(Level.INFO, "Stack trace:", ex);
				}
			}
		}

		purgeCache(applicationFolder);
	}

	public static JSONObject getJSONResponse(AbstractDataSourceConfig dataSource, String urlStr) throws IOException, JSONException {
		File jsonFile = getURLFile(dataSource, urlStr);

		JSONObject jsonResponse = null;
		try {
			jsonResponse = parseFile(jsonFile, urlStr);
			commitURLFile(dataSource, jsonFile, urlStr);
		} catch(Exception ex) {
			File rollbackFile = rollbackURLFile(dataSource, jsonFile, urlStr);
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
			LOGGER.log(Level.SEVERE, "Can not load the JSON Object returning from the URL {0}.", urlStr);
			LOGGER.log(Level.INFO, "Stack trace:", ex);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception ex) {
					LOGGER.log(Level.SEVERE, "Can not close the JSON file {0}.", jsonFile.getAbsoluteFile());
					LOGGER.log(Level.INFO, "Stack trace:", ex);
				}
			}
		}

		return jsonResponse;
	}

	public static JSONObject getSearchJSONResponse(String urlStr) throws IOException, JSONException {
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

	public static JSONArray getSearchJSONArrayResponse(String urlStr) throws IOException, JSONException {
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

	public static String getUncachedResponse(String urlStr) throws IOException, JSONException {
		URL url = new URL(urlStr);

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
					LOGGER.log(Level.WARNING, "Can not close the URL input stream.");
					LOGGER.log(Level.INFO, "Stack trace:", e);
				}
			}
			if (reader != null) {
				try { reader.close(); } catch(Exception e) {
					LOGGER.log(Level.WARNING, "Can not close the URL reader.");
					LOGGER.log(Level.INFO, "Stack trace:", e);
				}
			}
		}

		return sb.toString();
	}




	public static WMSCapabilities getWMSCapabilitiesResponse(AbstractDataSourceConfig dataSource, String urlStr) throws IOException, ServiceException, JSONException, URISyntaxException {
		File capabilitiesFile;
		WMSCapabilities wmsCapabilities;

		if (urlStr.startsWith("file://")) {
			// Local file URL
			capabilitiesFile = new File(new URI(urlStr));
			wmsCapabilities = getCapabilities(capabilitiesFile);

		} else {
			// TODO Find a nicer way to detect if the URL is a complete URL to a GetCapabilities document
			if (!urlStr.contains("?")) {
				// URL pointing at a WMS service
				urlStr = Utils.addUrlParameter(urlStr, "SERVICE", "WMS");
				urlStr = Utils.addUrlParameter(urlStr, "REQUEST", "GetCapabilities");
				urlStr = Utils.addUrlParameter(urlStr, "VERSION", "1.3.0");
			}

			capabilitiesFile = getURLFile(dataSource, urlStr);
			try {
				wmsCapabilities = getCapabilities(capabilitiesFile);
				commitURLFile(dataSource, capabilitiesFile, urlStr);
			} catch (Exception ex) {
				File rollbackFile = rollbackURLFile(dataSource, capabilitiesFile, urlStr);
				wmsCapabilities = getCapabilities(rollbackFile);
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
	private static WMSCapabilities getCapabilities(File file) throws IOException, ServiceException {
		WMSCapabilities capabilities = null;

		InputStream inputStream = null;
		try {
			Map hints = new HashMap();
			hints.put(DocumentHandler.DEFAULT_NAMESPACE_HINT_KEY, WMSSchema.getInstance());
			hints.put(DocumentFactory.VALIDATION_HINT, Boolean.FALSE);

			Object object;
			try {
				inputStream = new FileInputStream(file);
				object = DocumentFactory.getInstance(inputStream, hints, Level.WARNING);
			} catch (SAXException e) {
				throw (ServiceException) new ServiceException("Error while parsing XML.").initCause(e);
			}

			if (object instanceof ServiceException) {
				throw (ServiceException) object;
			}

			capabilities = (WMSCapabilities)object;
		} finally {
			if (inputStream != null) {
				inputStream.close();
			}
		}

		return capabilities;
	}


	public static void clearCache(File applicationFolder) throws IOException, JSONException {
		searchResponseCache.clear();

		// Clear cached files
		if (applicationFolder == null) return;

		diskCacheMap = new JSONObject();
		saveDiskCacheMap(applicationFolder);
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
			for (File file : files) {
				file.delete();
			}
			folder.delete();
		}
	}

	public static void clearSearchCache(String urlStr) {
		searchResponseCache.remove(urlStr);
	}

	public static void clearCache(File applicationFolder, AbstractDataSourceConfig dataSource) throws JSONException, IOException {
		if (dataSource == null || applicationFolder == null) {
			return;
		}
		File diskCacheFolder = FileFinder.getDiskCacheFolder(applicationFolder);
		String dataSourceId = (dataSource == null ? "TMP" : dataSource.getDataSourceId());

		if (diskCacheMap == null) {
			loadDiskCacheMap(applicationFolder);
		}

		if (diskCacheMap != null && diskCacheMap.length() > 0) {
			List<String> urlsToDelete = new ArrayList<String>();
			Iterator<String> urls = diskCacheMap.keys();
			String url;
			boolean hasChanged = false;
			while (urls.hasNext()) {
				url = urls.next();
				CachedFile cachedFile = new CachedFile(diskCacheFolder, diskCacheMap.optJSONObject(url));
				if (cachedFile.isEmpty()) {
					// Remove null entries
					urlsToDelete.add(url);
					hasChanged = true;
				} else if (dataSourceId.equals(cachedFile.getDataSourceId())) {
					File file = cachedFile.getFile();
					if (file != null && file.exists()) {
						file.delete();
					}
					File tmpFile = cachedFile.getTemporaryFile();
					if (tmpFile != null && tmpFile.exists()) {
						tmpFile.delete();
					}

					urlsToDelete.add(url);
					hasChanged = true;
				}
			}

			if (!urlsToDelete.isEmpty()) {
				for (String urlToDelete : urlsToDelete) {
					diskCacheMap.remove(urlToDelete);
				}
			}

			if (hasChanged) {
				saveDiskCacheMap(applicationFolder);
			}
		}

		// Delete the reminding files
		File folder = new File(diskCacheFolder, dataSourceId);
		if (folder.exists()) {
			File[] files = folder.listFiles();
			for (File file : files) {
				file.delete();
			}
			folder.delete();
		}
	}


	/**
	 * Delete cached files that are not in the map and
	 * delete map entry that represent deleted files.
	 */
	public static void purgeCache(File applicationFolder) throws IOException, JSONException {
		if (applicationFolder == null) return;
		final File diskCacheFolder = FileFinder.getDiskCacheFolder(applicationFolder);

		if (diskCacheMap == null) {
			loadDiskCacheMap(applicationFolder);
		}

		// Remove the cache entry that list missing files
		if (diskCacheMap != null && diskCacheMap.length() > 0) {
			List<String> urlsToDelete = new ArrayList<String>();
			Iterator<String> urls = diskCacheMap.keys();
			String url;
			boolean hasChanged = false;
			while (urls.hasNext()) {
				url = urls.next();
				CachedFile cachedFile = new CachedFile(diskCacheFolder, diskCacheMap.optJSONObject(url));
				if (cachedFile.isEmpty()) {
					// Remove null entries
					urlsToDelete.add(url);
					hasChanged = true;
				} else {
					File file = cachedFile.getFile();
					File tmpFile = cachedFile.getTemporaryFile();

					if (file == null || !file.exists()) {
						if (tmpFile == null || !tmpFile.exists()) {
							// This entry has no file, it has to be removed
							urlsToDelete.add(url);
							hasChanged = true;
						} else {
							// The file does not exists but the tmp file does...
							// Switch the tmp file to the file.
							Date tmpDownloadedTime = cachedFile.getTemporaryDownloadedTime();
							cachedFile.setFilename(cachedFile.getTemporaryFilename());
							if (tmpDownloadedTime != null) {
								cachedFile.setDownloadedTime(tmpDownloadedTime);
							}
							cachedFile.discardTemporaryData();
							hasChanged = true;
						}
					} else {
						if (tmpFile != null && !tmpFile.exists()) {
							cachedFile.discardTemporaryData();
							hasChanged = true;
						}
					}
				}
			}

			if (!urlsToDelete.isEmpty()) {
				for (String urlToDelete : urlsToDelete) {
					diskCacheMap.remove(urlToDelete);
				}
			}

			if (hasChanged) {
				saveDiskCacheMap(applicationFolder);
			}
		}

		File[] folders = diskCacheFolder.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.isDirectory();
			}
		});

		// Remove the files that are not listed in the cache map
		for (File folder : folders) {
			File[] files = folder.listFiles();
			String dataSourceId = folder.getName();

			for (File file : files) {
				String cachedFilename = file.getName();

				// Try to find the file in the cache
				boolean found = false;
				if (diskCacheMap != null && diskCacheMap.length() > 0) {
					Iterator<String> urls = diskCacheMap.keys(); // reset the iterator
					while (!found && urls.hasNext()) {
						String url = urls.next();
						CachedFile cachedFile = new CachedFile(diskCacheFolder, diskCacheMap.optJSONObject(url));
						if (!cachedFile.isEmpty()) {
							if (dataSourceId.equals(cachedFile.getDataSourceId())) {
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
				}

				if (!found) {
					file.delete();
				}
			}
		}
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
				searchResponseCache.remove(oldestResponseEntry.getKey());
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


	/**
	 * {
	 *     url: {
	 *         file: "path/to/the/file",
	 *         dataSourceId: "ea",
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
	private static class CachedFile {
		// Date format: "2012-09-24 14:06:49"
		private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		JSONObject jsonCachedFile;
		File cacheFolder;

		public CachedFile(File cacheFolder, String dataSourceId, String filename, Date downloadedTime, int expiry) throws JSONException {
			this.cacheFolder = cacheFolder;
			this.jsonCachedFile = new JSONObject();
			this.jsonCachedFile.put("dataSourceId", dataSourceId);
			this.jsonCachedFile.put("file", filename);
			this.jsonCachedFile.put("downloadedTime", dateFormat.format(downloadedTime));
			this.jsonCachedFile.put("expiry", expiry);
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
			return this.jsonCachedFile.length() <= 0;
		}

		public static String generateFilename(File cacheFolder, String dataSourceId, String urlStr) {
			File folder = dataSourceId == null ? cacheFolder : new File(cacheFolder, dataSourceId);
			folder.mkdirs();

			String host = null;
			try {
				URL url = new URL(urlStr);
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

		public String getDataSourceId() {
			return this.jsonCachedFile.optString("dataSourceId", null);
		}
		public void setDataSourceId(String dataSourceId) throws JSONException {
			this.jsonCachedFile.put("dataSourceId", dataSourceId);
		}

		public File getCachedFileFolder() {
			File folder = new File(this.cacheFolder, this.getDataSourceId());
			folder.mkdirs();
			return folder;
		}

		public String getFilename() {
			return this.jsonCachedFile.optString("file", null);
		}
		public void setFilename(String file) throws JSONException {
			this.jsonCachedFile.put("file", file);
		}

		public File getFile() {
			String filename = this.getFilename();
			if (filename == null) {
				return null;
			}

			File file = new File(this.getCachedFileFolder(), filename);
			if (file == null || !file.exists()) {
				return null;
			}
			return file;
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
				LOGGER.log(Level.WARNING, "Can not parse the downloaded time \"{0}\"", downloadedTimeStr);
				LOGGER.log(Level.INFO, "Stacktrace: ", e);
			}

			return downloadedTime;
		}
		public void setDownloadedTime(Date downloadedTime) throws JSONException {
			this.jsonCachedFile.put("downloadedTime", dateFormat.format(downloadedTime));
		}

		public int getExpiry() {
			return this.jsonCachedFile.optInt("expiry", CACHE_TIMEOUT);
		}
		public void setExpiry(int expiry) throws JSONException {
			this.jsonCachedFile.put("expiry", expiry);
		}

		public String getTemporaryFilename() {
			JSONObject jsonTmpFile = this.jsonCachedFile.optJSONObject("tmpFile");
			if (jsonTmpFile == null) {
				return null;
			}
			return jsonTmpFile.optString("file", null);
		}
		public void setTemporaryFilename(String file) throws JSONException {
			JSONObject jsonTmpFile = this.jsonCachedFile.optJSONObject("tmpFile");
			if (jsonTmpFile == null) {
				jsonTmpFile = new JSONObject();
				this.jsonCachedFile.put("tmpFile", jsonTmpFile);
			}
			jsonTmpFile.put("file", file);
		}

		public File getTemporaryFile() {
			String temporaryFilename = this.getTemporaryFilename();
			if (temporaryFilename == null) {
				return null;
			}

			File file = new File(this.getCachedFileFolder(), temporaryFilename);
			if (file == null || !file.exists()) {
				return null;
			}
			return file;
		}

		public Date getTemporaryDownloadedTime() {
			JSONObject jsonTmpFile = this.jsonCachedFile.optJSONObject("tmpFile");
			if (jsonTmpFile == null) {
				return null;
			}
			String downloadedTimeStr = jsonTmpFile.optString("downloadedTime", null);
			if (downloadedTimeStr == null) {
				return null;
			}

			Date downloadedTime = null;
			try {
				downloadedTime = dateFormat.parse(downloadedTimeStr);
			} catch (ParseException e) {
				LOGGER.log(Level.WARNING, "Can not parse the downloaded time \"{0}\"", downloadedTimeStr);
				LOGGER.log(Level.INFO, "Stacktrace: ", e);
			}

			return downloadedTime;
		}
		public void setTemporaryDownloadedTime(Date downloadedTime) throws JSONException {
			JSONObject jsonTmpFile = this.jsonCachedFile.optJSONObject("tmpFile");
			if (jsonTmpFile == null) {
				jsonTmpFile = new JSONObject();
				this.jsonCachedFile.put("tmpFile", jsonTmpFile);
			}
			jsonTmpFile.put("downloadedTime", dateFormat.format(downloadedTime));
		}

		public void discardTemporaryData() {
			this.jsonCachedFile.remove("tmpFile");
		}

		/**
		 * Approve the last file sent for this URL. This has the effect
		 * of replacing the current cached file with the last sent file.
		 */
		public void commit(File approvedFile) throws IOException, JSONException {
			File oldFile = this.getFile();

			String tmpFilename = this.getTemporaryFilename();
			Date tmpDownloadedTime = this.getTemporaryDownloadedTime();
			if (tmpFilename != null && approvedFile != null && tmpFilename.equals(approvedFile.getName())) {
				this.setFilename(tmpFilename);
				this.setDownloadedTime(tmpDownloadedTime);

				this.discardTemporaryData();

				// Clean the directory
				if (oldFile != null && oldFile.exists()) {
					oldFile.delete();
				}
			}
		}

		/**
		 * This cancel and delete the latest downloaded file and send
		 * the previous downloaded file, which is the latest working
		 * state of the file.
		 * @return
		 */
		public File rollback(File unapprovedFile) throws IOException, JSONException {
			File backupFile = this.getFile();

			// Clean-up the directory
			File tmpFile = this.getTemporaryFile();
			if (backupFile != null && backupFile.exists() && tmpFile != null && unapprovedFile != null && tmpFile.equals(unapprovedFile)) {
				tmpFile.delete();

				this.discardTemporaryData();
			}

			if (backupFile != null && backupFile.exists()) {
				return backupFile;
			}
			// If we have nothing better...
			return unapprovedFile;
		}

	}
}
