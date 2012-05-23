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
import org.geotools.data.ows.GetCapabilitiesRequest;
import org.geotools.data.ows.GetCapabilitiesResponse;
import org.geotools.data.ows.Request;
import org.geotools.data.ows.WMSCapabilities;
import org.geotools.data.wms.WMS1_3_0;
import org.geotools.data.wms.WebMapServer;
import org.geotools.ows.ServiceException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

// TODO IMPORTANT: The WHOLE class has to be synchronized (no one can access a method while one method is running)
// TODO IMPORTANT: Save caching files to disk! (GetCapabilities can be quite larges, having multiple of them hold in memory can kill the server)
public class URLCache {
	private static final Logger LOGGER = Logger.getLogger(URLCache.class.getName());

	// Cache timeout in millisecond
	// The response will be re-requested if the application request
	// information from it and its cached timestamp is older than this setting.
	protected static final long CACHE_TIMEOUT = 60*60000; // X*60000 = X minutes

	// HashMap<String urlString, ResponseWrapper response>
	private static HashMap<String, ResponseWrapper> responseCache = new HashMap<String, ResponseWrapper>();

	public static JSONObject getJSONResponse(AbstractDataSourceConfig dataSource, String urlStr) throws IOException, JSONException {
		ResponseWrapper response = getCachedResponse(urlStr);

		if (response == null) {
			response = new ResponseWrapper();
			if (dataSource.isCachingDisabled() == null || !dataSource.isCachingDisabled()) {
				// Set the wrapper in the cache now, it will be filled before the end of the method
				setCachedResponse(urlStr, response);
			}
		}

		// Link this URL to the dataSource
		// NOTE: Duplicate ids are ignored
		response.dataSourceIds.add(dataSource.getDataSourceId());

		if (response.jsonResponse == null) {

			LOGGER.log(Level.INFO, "\n### DOWNLOADING ### ArcGIS JSON Document {0}\nFor dataSource {1}\n",
					new String[]{ urlStr, dataSource.getDataSourceId() });

			URL url = new URL(urlStr);

			URLConnection connection = url.openConnection();
			InputStream in = null;
			BufferedReader reader = null;
			try {
				in = connection.getInputStream();
				if (in != null) {
					reader = new BufferedReader(new InputStreamReader(in));

					StringBuilder sb = new StringBuilder();
					int cp;
					while ((cp = reader.read()) != -1) {
						sb.append((char) cp);
					}
					response.jsonResponse = new JSONObject(sb.toString());
				}
			} finally {
				if (in != null) {
					try { in.close(); } catch(Exception e) {
						LOGGER.log(Level.WARNING, "Can not close the URL input stream.", e);
					}
				}
				if (reader != null) {
					try { reader.close(); } catch(Exception e) {
						LOGGER.log(Level.WARNING, "Can not close the URL reader.", e);
					}
				}
			}
		}

		return response.jsonResponse;
	}

	public static WMSCapabilities getWMSCapabilitiesResponse(AbstractDataSourceConfig dataSource, String urlStr) throws IOException, ServiceException {
		ResponseWrapper response = getCachedResponse(urlStr);

		if (response == null) {
			response = new ResponseWrapper();
			if (dataSource.isCachingDisabled() == null || !dataSource.isCachingDisabled()) {
				// Set the wrapper in the cache now, it will be filled before the end of the method
				setCachedResponse(urlStr, response);
			}
		}

		// Link this URL to the dataSource
		// NOTE: Duplicate ids are ignored
		response.dataSourceIds.add(dataSource.getDataSourceId());

		if (response.wmsResponse == null) {
			LOGGER.log(Level.INFO, "\n### DOWNLOADING ### Capabilities Document {0}\nFor dataSource {1}\n",
					new String[]{ urlStr, dataSource.getDataSourceId() });

			URL url = new URL(urlStr);

			// TODO Find a nicer way to detect if the URL is a complete URL to a GetCapabilities document
			if (urlStr.startsWith("file://")) {
				// Local file URL
				GetCapabilitiesRequest req = getCapRequest(url);
				GetCapabilitiesResponse resp = issueFileRequest(req);
				response.wmsResponse = (WMSCapabilities)resp.getCapabilities();

			} else if (urlStr.contains("?")) {
				// URL pointing directly at a Capabilities Document
				WebMapServer server = new WebMapServer(url);
				GetCapabilitiesRequest req = getCapRequest(url);
				GetCapabilitiesResponse resp = server.issueRequest(req);
				response.wmsResponse = (WMSCapabilities)resp.getCapabilities();

			} else {
				// URL pointing at a WMS service, or anything else that GeoTools supports
				WebMapServer server = new WebMapServer(url);
				response.wmsResponse = server.getCapabilities();
			}
		}

		return response.wmsResponse;
	}
	private static GetCapabilitiesRequest getCapRequest(final URL url) {
		return new WMS1_3_0.GetCapsRequest(url) {
			@Override
			public URL getFinalURL() {
				return url;
			}
		};
	}
	private static GetCapabilitiesResponse issueFileRequest(Request request) throws IOException, ServiceException {
		URL finalURL = request.getFinalURL();
		URLConnection connection = finalURL.openConnection();
		InputStream inputStream = connection.getInputStream();

		String contentType = connection.getContentType();

		return (GetCapabilitiesResponse)request.createResponse(contentType, inputStream);
	}

	public static void clearCache() {
		responseCache.clear();
	}

	public static void clearCache(String urlStr) {
		responseCache.remove(urlStr);
	}

	public static void clearCache(AbstractDataSourceConfig dataSource) {
		if (dataSource != null) {
			String dataSourceId = dataSource.getDataSourceId();
			if (dataSourceId != null) {
				Iterator<Map.Entry<String, ResponseWrapper>> itr = responseCache.entrySet().iterator();
				while (itr.hasNext()) {
					Map.Entry<String, ResponseWrapper> entry = itr.next();
					if (entry.getValue() == null || entry.getValue().dataSourceIds == null) {
						itr.remove();
					} else {
						// If one of the dataSourceIds represent the dataSource, the URL must be cleared.
						for (String cachedDataSourceId : entry.getValue().dataSourceIds) {
							if (dataSourceId.equals(cachedDataSourceId)) {
								itr.remove();
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Remove cache entries associated with deleted data sources.
	 * This method can optionally be called to prevent memory leak,
	 * which may append if the clearCache method do not get called
	 * when a data source is updated or deleted.
	 * @param dataSources
	 */
	public static void purgeCache(Collection<AbstractDataSourceConfig> dataSources) {
		if (dataSources != null) {
			Iterator<Map.Entry<String, ResponseWrapper>> itr = responseCache.entrySet().iterator();
			while (itr.hasNext()) {
				Map.Entry<String, ResponseWrapper> entry = itr.next();
				if (entry.getValue() == null ||
						entry.getValue().dataSourceIds == null) {
					itr.remove();
				} else {
					boolean found = false;
					// If one of the dataSourceIds is found in the collection of dataSources, the cache is still valid.
					for (String cachedDataSourceId : entry.getValue().dataSourceIds) {
						for (AbstractDataSourceConfig dataSource : dataSources) {
							if (dataSource != null && cachedDataSourceId.equals(dataSource.getDataSourceId())) {
								found = true;
								break;
							}
						}
					}
					if (!found) {
						itr.remove();
					}
				}
			}
		}
	}

	private static ResponseWrapper getCachedResponse(String urlStr) {
		if (!responseCache.containsKey(urlStr)) {
			return null;
		}

		ResponseWrapper response = responseCache.get(urlStr);
		if (response == null) {
			return null;
		}

		long timeoutTimestamp = Utils.getCurrentTimestamp() - CACHE_TIMEOUT;
		if (response.timestamp <= timeoutTimestamp) {
			clearCache(urlStr);
			return null;
		}

		return response;
	}

	private static void setCachedResponse(String urlStr, ResponseWrapper response) {
		if (urlStr != null && response != null) {
			responseCache.put(urlStr, response);
		}
	}

	private static class ResponseWrapper {
		// List of dataSource that use this URL
		public Set<String> dataSourceIds;

		// Response; either json or wms (or both?)
		public JSONObject jsonResponse;
		public WMSCapabilities wmsResponse;

		// Log the creation time, to knows when it times out
		public long timestamp;

		public ResponseWrapper() {
			this.dataSourceIds = new HashSet<String>();
			this.jsonResponse = null;
			this.wmsResponse = null;
			this.timestamp = Utils.getCurrentTimestamp();
		}
	}
}
