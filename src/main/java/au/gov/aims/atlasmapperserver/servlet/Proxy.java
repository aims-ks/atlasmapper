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

package au.gov.aims.atlasmapperserver.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import au.gov.aims.atlasmapperserver.ClientConfig;
import au.gov.aims.atlasmapperserver.ConfigHelper;
import au.gov.aims.atlasmapperserver.ConfigManager;
import au.gov.aims.atlasmapperserver.ConfigType;
import au.gov.aims.atlasmapperserver.ServletUtils;
import au.gov.aims.atlasmapperserver.Utils;
import au.gov.aims.atlasmapperserver.dataSourceConfig.AbstractDataSourceConfig;
import au.gov.aims.atlasmapperserver.dataSourceConfig.WMSDataSourceConfig;
import au.gov.aims.atlasmapperserver.jsonWrappers.client.LayerWrapper;
import au.gov.aims.atlasmapperserver.jsonWrappers.client.ClientWrapper;
import au.gov.aims.atlasmapperserver.jsonWrappers.client.DataSourceWrapper;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.impl.conn.SchemeRegistryFactory;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Redirect queries sent to external Web Site.
 * This is not an open proxy, it only redirect requests if the host
 * is in the list of allowed hosts.
 * Created by IntelliJ IDEA.
 * Updated by Gael Lafond.
 * User: kgunn
 * Date: 21/06/11
 * Time: 1:28 PM
 */
public class Proxy extends HttpServlet {
	private static final Logger LOGGER = Logger.getLogger(Proxy.class.getName());
	private static final String URL_PARAM = "url";

	// NOTE: The URL Cache has a very similar http client
	private static HttpClient httpClient = null;
	static {
		SchemeRegistry schemeRegistry = SchemeRegistryFactory.createDefault();
		// Try to set the SSL scheme factory: Accept all SSL certificates
		try {
			SSLSocketFactory sslSocketFactory = new SSLSocketFactory(
				// All certificates are trusted
				new TrustStrategy() {
					public boolean isTrusted(final X509Certificate[] chain, String authType) throws CertificateException {
						return true;
					}
				},
				// Do not check if the hostname match the certificate
				new AllowAllHostnameVerifier()
			);

			Scheme httpsScheme = new Scheme("https", 443, sslSocketFactory);
			schemeRegistry.register(httpsScheme);
		} catch(Exception ex) {
			LOGGER.log(Level.SEVERE, "Can not initiate the SSLSocketFactory, needed to accept all SSL self signed certificates.", ex);
		}

		// Set a pool of multiple connections so more than one client can be generated simultaneously
		// See: http://stackoverflow.com/questions/12799006/how-to-solve-error-invalid-use-of-basicclientconnmanager-make-sure-to-release
		PoolingClientConnectionManager cxMgr = new PoolingClientConnectionManager(schemeRegistry);
		cxMgr.setMaxTotal(100);
		cxMgr.setDefaultMaxPerRoute(20);

		httpClient = new DefaultHttpClient(cxMgr);
	}

	// Cached list of allowed hosts, for each clients
	private static Map<String, Set<String>> generatedClientsAllowedHostCache = null;

	// Cached list of all allowed hosts, used with the public folder
	private static Set<String> allAllowedHostCache = null;

	private static final Set<String> DEFAULT_ALLOWED_HOSTS = new HashSet<String>();
	static {
		DEFAULT_ALLOWED_HOSTS.add("www.openlayers.org");
		DEFAULT_ALLOWED_HOSTS.add("openlayers.org");
		DEFAULT_ALLOWED_HOSTS.add("labs.metacarta.com");
		DEFAULT_ALLOWED_HOSTS.add("world.freemap.in");
		DEFAULT_ALLOWED_HOSTS.add("prototype.openmnnd.org");
		DEFAULT_ALLOWED_HOSTS.add("geo.openplans.org");
		DEFAULT_ALLOWED_HOSTS.add("sigma.openplans.org");
		DEFAULT_ALLOWED_HOSTS.add("demo.opengeo.org");
		DEFAULT_ALLOWED_HOSTS.add("www.openstreetmap.org");
		DEFAULT_ALLOWED_HOSTS.add("sample.azavea.com");
		DEFAULT_ALLOWED_HOSTS.add("e-atlas.org.au");
		DEFAULT_ALLOWED_HOSTS.add("ningaloo-atlas.org.au");
	}

	// Don't define a constructor! Initialise the Servlet here.
	/*
	@Override
	public void init(ServletConfig sc) throws ServletException {
		super.init(sc);
	}
	*/

	private static synchronized void reloadConfig(ServletContext servletContext, String clientId) {
		try {
			ConfigManager configManager = ConfigHelper.getConfigManager(servletContext);

			if (FileFinder.PUBLIC_FOLDER.equals(clientId)) {
				allAllowedHostCache = getAllProxyAllowedHosts(configManager);
			} else {
				ClientConfig clientConfig = configManager.getClientConfig(clientId);
				// The main config may be incomplete without the list of layers, but it will contains enough info to configure the proxy.
				ClientWrapper jsonMainConfig = new ClientWrapper(configManager.getClientConfigFileJSon(clientConfig, ConfigType.MAIN, false));
				JSONObject jsonLayersConfig = configManager.getClientConfigFileJSon(clientConfig, ConfigType.LAYERS, false);
				Proxy.reloadConfig(jsonMainConfig, jsonLayersConfig, clientConfig);
			}
		} catch (Throwable ex) {
			LOGGER.log(Level.SEVERE, "Error occurred while reloading the proxy configuration: {0}", Utils.getExceptionMessage(ex));
			LOGGER.log(Level.FINE, "Stack trace: ", ex);
		}
	}

	public static synchronized void reloadConfig(ClientWrapper jsonMainConfig, JSONObject jsonLayersConfig, ClientConfig clientConfig) {
		if (generatedClientsAllowedHostCache == null) {
			generatedClientsAllowedHostCache = new HashMap<String, Set<String>>();
		}

		Set<String> foundAllowedHosts = null;
		try {
			foundAllowedHosts = getProxyAllowedHosts(jsonMainConfig, jsonLayersConfig, clientConfig);
			if (foundAllowedHosts == null) {
				LOGGER.log(Level.WARNING, "No allowed hosts found in AtlasMapperServer configuration.");
			}
		} catch (Exception ex) {
			LOGGER.log(Level.SEVERE, "Error while retrieving the allowed hosts: {0}", Utils.getExceptionMessage(ex));
			LOGGER.log(Level.FINE, "Stack trace: ", ex);
		} finally {
			if (foundAllowedHosts == null) {
				LOGGER.log(Level.WARNING, "Could not retrieved the proxy allowed hosts. Default allowed hosts will be used.");
				foundAllowedHosts = DEFAULT_ALLOWED_HOSTS;
			}
		}

		LOGGER.log(Level.INFO, "Reloading the proxy configuration for generated version of ["+clientConfig.getClientId()+"]");
		generatedClientsAllowedHostCache.put(clientConfig.getClientId(), foundAllowedHosts);
	}

	// Return all allowed hosts, at a data source level, before overrides...
	// This is only used with script that can be used with any clients / data source (even when no clients are defined)
	// Example: In the www folder
	private static Set<String> getAllProxyAllowedHosts(ConfigManager configManager) throws IOException, JSONException {
		Set<String> allowedHosts = new HashSet<String>();

		for (AbstractDataSourceConfig dataSource : configManager.getDataSourceConfigs().values()) {
			addProxyAllowedHost(allowedHosts, dataSource.getFeatureRequestsUrl());
			addProxyAllowedHost(allowedHosts, dataSource.getServiceUrl());
			if (dataSource instanceof WMSDataSourceConfig) {
				WMSDataSourceConfig wmsDataSource = (WMSDataSourceConfig)dataSource;
				addProxyAllowedHost(allowedHosts, wmsDataSource.getGetMapUrl());
				addProxyAllowedHost(allowedHosts, wmsDataSource.getWebCacheUrl());
			}
		}

		for (ClientConfig clientConfig : configManager.getClientConfigs().values()) {
			String[] extraAllowedHosts = clientConfig.getExtraAllowedHosts();
			if (extraAllowedHosts != null && extraAllowedHosts.length >= 0) {
				for (String extraAllowedHost : extraAllowedHosts) {
					allowedHosts.add(extraAllowedHost);
				}
			}
		}

		return allowedHosts;
	}

	private static Set<String> getProxyAllowedHosts(ClientWrapper mainConfig, JSONObject layersConfig, ClientConfig clientConfig)
			throws Exception {

		Set<String> allowedHosts = new HashSet<String>();

		// The config manager apply all the overrides during the generation of the config.
		if (mainConfig != null && mainConfig.getDataSources() != null) {
			JSONObject dataSources = mainConfig.getDataSources();
			Iterator<String> keys = dataSources.keys();
			if (keys != null) {
				while (keys.hasNext()) {
					DataSourceWrapper dataSource = new DataSourceWrapper(dataSources.optJSONObject(keys.next()));

					addProxyAllowedHost(allowedHosts, dataSource.getFeatureRequestsUrl());
					addProxyAllowedHost(allowedHosts, dataSource.getServiceUrl());
				}
			}
		}

		if (layersConfig != null) {
			Iterator<String> layerIds = layersConfig.keys();
			if (layerIds != null) {
				while (layerIds.hasNext()) {
					LayerWrapper layer = new LayerWrapper(layersConfig.optJSONObject(layerIds.next()));

					addProxyAllowedHost(allowedHosts, layer.getKmlUrl());
					addProxyAllowedHost(allowedHosts, layer.getFeatureRequestsUrl());
					addProxyAllowedHost(allowedHosts, layer.getServiceUrl());
				}
			}
		}

		String[] extraAllowedHosts = clientConfig.getExtraAllowedHosts();
		if (extraAllowedHosts != null && extraAllowedHosts.length >= 0) {
			for (String extraAllowedHost : extraAllowedHosts) {
				allowedHosts.add(extraAllowedHost);
			}
		}

		return allowedHosts.isEmpty() ? null : allowedHosts;
	}

	private static boolean addProxyAllowedHost(Set<String> allowedHosts, String urlStr) {
		URL url = null;
		try {
			url = Utils.toURL(urlStr);
		} catch (Exception ex) {
			return false;
		}
		// It should not be null if it succeed, but better not taking chance.
		if (url == null) { return false; }

		String host = url.getHost();
		if (Utils.isNotBlank(host)) {
			allowedHosts.add(host.trim());
		}

		return true;
	}

	private boolean isHostAllowed(String clientId, String rawHost) {
		if (Utils.isBlank(rawHost)) {
			return false;
		}
		String host = rawHost.trim();

		if (FileFinder.PUBLIC_FOLDER.equals(clientId)) {
			if (allAllowedHostCache == null) {
				reloadConfig(this.getServletContext(), clientId);
			}
			if (allAllowedHostCache == null) {
				return false;
			}
			for (String allowedHost: allAllowedHostCache) {
				// Case-insensitive: http://en.wikipedia.org/wiki/Domain_name#Technical_requirements_and_process
				if (host.equalsIgnoreCase(allowedHost)) {
					return true;
				}
			}
		} else {
			Map<String, Set<String>> allowedHostsMap = generatedClientsAllowedHostCache;

			// The proxy config are null after a reload of the WebApp
			if (allowedHostsMap == null || allowedHostsMap.get(clientId) == null) {
				reloadConfig(this.getServletContext(), clientId);

				allowedHostsMap = generatedClientsAllowedHostCache;
			}

			Set<String> allowedHosts = allowedHostsMap.get(clientId);

			if (allowedHosts == null) {
				return false;
			}

			for (String allowedHost: allowedHosts) {
				// Case-insensitive: http://en.wikipedia.org/wiki/Domain_name#Technical_requirements_and_process
				if (host.equalsIgnoreCase(allowedHost)) {
					return true;
				}
			}
		}

		return false;
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		this.performTask(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		this.performTask(request, response);
	}

	private void performTask(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String clientId = request.getParameter("client");

		String urlStr = request.getParameter(URL_PARAM);
		if (urlStr != null && !urlStr.isEmpty()) {
			LOGGER.log(Level.INFO, "Proxy URL: {0}", urlStr);
			try {
				String decodedUrl = URLDecoder.decode(urlStr, "UTF-8");
				if (decodedUrl != null) { decodedUrl = decodedUrl.trim(); }

				URL url = Utils.toURL(decodedUrl);
				if (url != null) {
					String protocol = url.getProtocol();
					String host = url.getHost();
					if (!isHostAllowed(clientId, host)) {
						response.setContentType("text/plain");
						response.setStatus(HttpServletResponse.SC_FORBIDDEN);

						String responseTxt = "This proxy does not allow you to access that location (" + host + ").";
						LOGGER.log(Level.WARNING, responseTxt);

						ServletUtils.sendResponse(response, responseTxt);
					} else if (protocol.equals("http") || protocol.equals("https")) {
						HttpGet httpGet = new HttpGet(url.toURI());
						HttpEntity entity = null;

						try {
							HttpResponse httpClientResponse = httpClient.execute(httpGet);
							StatusLine httpStatus = httpClientResponse.getStatusLine();
							int responseCode = -1;
							if (httpStatus != null) {
								responseCode = httpStatus.getStatusCode();
							}
							response.setStatus(responseCode);

							if (responseCode < 400) {
								// The entity is streamed
								entity = httpClientResponse.getEntity();
								String contentType = null;
								if (entity != null) {
									Header header = entity.getContentType();
									contentType = header.getValue();
								}

								// -1: Unknown status code
								// 1XX: Informational
								// 2XX: Successful
								// 3XX: Redirection
								if (contentType == null || contentType.isEmpty()) {
									response.setContentType("text/plain");
									LOGGER.log(Level.INFO, "Can not retrieved the content type, falling back to: {0}", response.getContentType());
								} else {
									response.setContentType(contentType);
									LOGGER.log(Level.INFO, "Set content type using URL connection content type: {0}", response.getContentType());
								}

								InputStream inputStream = null;
								try {
									inputStream = entity.getContent();
									ServletUtils.sendResponse(response, inputStream);
								} finally {
									if (inputStream != null) {
										try {
											inputStream.close();
										} catch (Exception e) {
											LOGGER.log(Level.WARNING, "Cant close the URL input stream: {0}", Utils.getExceptionMessage(e));
											LOGGER.log(Level.FINE, "Stack trace: ", e);
										}
									}
								}
							} else if (responseCode == HttpServletResponse.SC_BAD_REQUEST) {
								// 400: Bad Request
								response.setContentType("text/plain");
								String responseTxt = "Error "+responseCode+" - Bad Request: "+decodedUrl;
								LOGGER.log(Level.WARNING, responseTxt);

								ServletUtils.sendResponse(response, responseTxt);
							} else if (responseCode == HttpServletResponse.SC_NOT_FOUND) {
								// 404: Not Found
								response.setContentType("text/plain");
								String responseTxt = "Error "+responseCode+" - Not Found: "+decodedUrl;
								LOGGER.log(Level.WARNING, responseTxt);

								ServletUtils.sendResponse(response, responseTxt);
							} else if (responseCode == HttpServletResponse.SC_INTERNAL_SERVER_ERROR) {
								// 500: Internal Server Error
								response.setContentType("text/plain");
								String responseTxt = "Error "+responseCode+" - Internal Server Error: "+decodedUrl;
								LOGGER.log(Level.WARNING, responseTxt);

								ServletUtils.sendResponse(response, responseTxt);
							} else {
								// Any other errors
								response.setContentType("text/plain");
								String responseTxt = "Error "+responseCode+": "+decodedUrl;
								LOGGER.log(Level.WARNING, responseTxt);

								ServletUtils.sendResponse(response, responseTxt);
							}
						} finally {
							if (httpGet != null) {
								// Cancel the connection, if it's still alive
								httpGet.abort();
								// Close connections
								httpGet.reset();
							}
						}
					} else {
						response.setContentType("text/plain");
						response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

						String responseTxt = "Error - Unsupported protocol: "+decodedUrl;
						LOGGER.log(Level.WARNING, responseTxt);

						ServletUtils.sendResponse(response, responseTxt);
					}
				}
			} catch (Exception e) {
				response.setContentType("text/plain");
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

				String responseTxt = "An unexpected error occurred: " + Utils.getExceptionMessage(e);
				LOGGER.log(Level.WARNING, responseTxt);
				LOGGER.log(Level.FINE, "Stack trace: ", e);

				ServletUtils.sendResponse(response, responseTxt);
			}
		} else {
			response.setContentType("text/plain");
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

			String responseTxt = "Can't get url value from request.";
			LOGGER.log(Level.WARNING, responseTxt);

			ServletUtils.sendResponse(response, responseTxt);
		}
	}
}
