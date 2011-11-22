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

import au.gov.aims.atlasmapperserver.ConfigHelper;
import au.gov.aims.atlasmapperserver.ConfigManager;
import au.gov.aims.atlasmapperserver.ServletUtils;
import au.gov.aims.atlasmapperserver.Utils;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geotools.ows.ServiceException;
import org.json.JSONException;

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

	private List<String> allowedHosts = null;
	private static final List<String> DEFAULT_ALLOWED_HOSTS = new ArrayList<String>();
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
	@Override
	public void init(ServletConfig sc) throws ServletException {
		super.init(sc);
	}

	public synchronized void reloadConfig(String clientId, boolean live) {
		List<String> foundAllowedHosts = null;
		try {
			ConfigManager configManager = ConfigHelper.getConfigManager(this.getServletContext());
			foundAllowedHosts = configManager.getProxyAllowedHosts(clientId, live);
			if (foundAllowedHosts == null) {
				LOGGER.log(Level.WARNING, "No allowed hosts found in AtlasMapperServer configuration.");
			}
		} catch (JSONException ex) {
			LOGGER.log(Level.SEVERE, "Error while retriving the allowed hosts.", ex);
		} catch (IOException ex) {
			LOGGER.log(Level.SEVERE, "Error while retriving the allowed hosts.", ex);
		} catch (ServiceException ex) {
			LOGGER.log(Level.SEVERE, "Error while retriving the allowed hosts.", ex);
		} finally {
			if (foundAllowedHosts != null) {
				this.allowedHosts = foundAllowedHosts;
			} else {
				LOGGER.log(Level.WARNING, "Could not retrieved the proxy allowed hosts. Default allowed hosts will be used.");
				this.allowedHosts = DEFAULT_ALLOWED_HOSTS;
			}
		}
	}

	private boolean isHostAllowed(String rawhost) {
		if (this.allowedHosts == null || Utils.isBlank(rawhost)) { return false; }
		String host = rawhost.trim();

		for (String allowedHost: this.allowedHosts) {
			// Case-insensitive: http://en.wikipedia.org/wiki/Domain_name#Technical_requirements_and_process
			if (host.equalsIgnoreCase(allowedHost.trim())) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		this.performTask(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		this.performTask(request, response);
	}

	private void performTask(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String liveStr = request.getParameter("live");
		boolean live = liveStr != null && Boolean.parseBoolean(liveStr);

		String clientId = request.getParameter("client");
		this.reloadConfig(clientId, live);

		String urlStr = request.getParameter(URL_PARAM);
		if (urlStr != null && !urlStr.isEmpty()) {
			LOGGER.log(Level.INFO, "Proxy URL: {0}", urlStr);
			try {
				String decodedUrl = URLDecoder.decode(urlStr, "UTF-8");
				if (decodedUrl != null) { decodedUrl = decodedUrl.trim(); }

				URL url = new URL(decodedUrl);
				if (url != null) {
					String protocol = url.getProtocol();
					String host = url.getHost();
					if (!isHostAllowed(host)) {
						response.setContentType("text/plain");
						response.setStatus(HttpServletResponse.SC_FORBIDDEN);

						String responseTxt = "This proxy does not allow you to access that location (" + host + ").";
						LOGGER.log(Level.WARNING, responseTxt);

						ServletUtils.sendResponse(request, response, responseTxt);
					} else if (protocol.equals("http") || protocol.equals("https")) {
						URLConnection conn = url.openConnection();

						if (conn != null) {
							HttpURLConnection httpConn = (HttpURLConnection)conn;
							int responseCode = httpConn.getResponseCode();

							if (responseCode > 0) {
								response.setStatus(responseCode);
							}

							if (responseCode < 400) {
								// -1: Unknown status code
								// 1XX: Informational
								// 2XX: Successful
								// 3XX: Redirection
								String contentType = conn.getContentType();
								if (contentType == null || contentType.isEmpty()) {
									response.setContentType("text/plain");
									LOGGER.log(Level.INFO, "Can not retrieved the content type, falling back to: {0}", response.getContentType());
								} else {
									response.setContentType(contentType);
									LOGGER.log(Level.INFO, "Set content type using URL connection content type: {0}", response.getContentType());
								}

								InputStream inputStream = null;
								try {
									inputStream = conn.getInputStream();
									ServletUtils.sendResponse(request, response, inputStream);
								} finally {
									if (inputStream != null) {
										try { inputStream.close(); } catch (Exception e) { LOGGER.log(Level.WARNING, "Cant close the URL input stream.", e); }
									}
								}
							} else if (responseCode == HttpServletResponse.SC_BAD_REQUEST) {
								// 400: Bad Request
								response.setContentType("text/plain");
								String responseTxt = "Error "+responseCode+" - Bad Request: "+decodedUrl;
								LOGGER.log(Level.WARNING, responseTxt);

								ServletUtils.sendResponse(request, response, responseTxt);
							} else if (responseCode == HttpServletResponse.SC_NOT_FOUND) {
								// 404: Not Found
								response.setContentType("text/plain");
								String responseTxt = "Error "+responseCode+" - Not Found: "+decodedUrl;
								LOGGER.log(Level.WARNING, responseTxt);

								ServletUtils.sendResponse(request, response, responseTxt);
							} else if (responseCode == HttpServletResponse.SC_INTERNAL_SERVER_ERROR) {
								// 500: Internal Server Error
								response.setContentType("text/plain");
								String responseTxt = "Error "+responseCode+" - Internal Server Error: "+decodedUrl;
								LOGGER.log(Level.WARNING, responseTxt);

								ServletUtils.sendResponse(request, response, responseTxt);
							} else {
								// Any other errors
								response.setContentType("text/plain");
								String responseTxt = "Error "+responseCode+": "+decodedUrl;
								LOGGER.log(Level.WARNING, responseTxt);

								ServletUtils.sendResponse(request, response, responseTxt);
							}
						} else {
							LOGGER.log(Level.WARNING, "Can not open the URL connection.");
							throw new ServletException("Can not open the URL connection.");
						}
					} else {
						response.setContentType("text/plain");
						response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

						String responseTxt = "Error - Unsupported protocol: "+decodedUrl;
						LOGGER.log(Level.WARNING, responseTxt);

						ServletUtils.sendResponse(request, response, responseTxt);
					}
				}
			} catch (Exception e) {
				response.setContentType("text/plain");
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

				String responseTxt = "An unexpected error occurred: " + e.getMessage();
				LOGGER.log(Level.WARNING, responseTxt, e);

				ServletUtils.sendResponse(request, response, responseTxt);
			}
		} else {
			LOGGER.log(Level.WARNING, "Can't get url value from request.");
			throw new ServletException("Can't get url value from request.");
		}
	}
}
