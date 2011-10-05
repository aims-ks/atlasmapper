package au.gov.aims.atlasmapperserver.servlet;

import au.gov.aims.atlasmapperserver.ConfigHelper;
import au.gov.aims.atlasmapperserver.ConfigManager;
import au.gov.aims.atlasmapperserver.ServletUtils;
import au.gov.aims.atlasmapperserver.Utils;
import java.io.IOException;
import java.io.InputStream;
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

	public synchronized void reloadConfig(String clientName, boolean live) {
		List<String> foundAllowedHosts = null;
		try {
			ConfigManager configManager = ConfigHelper.getConfigManager(this.getServletContext());
			foundAllowedHosts = configManager.getProxyAllowedHosts(clientName, live);
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
			if (host.equals(allowedHost.trim())) {
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

		String clientName = request.getParameter("client");
		this.reloadConfig(clientName, live);

		String urlStr = request.getParameter(URL_PARAM);
		if (urlStr != null && !urlStr.isEmpty()) {
			LOGGER.log(Level.INFO, "Proxy URL: " + urlStr);
			try {
			    URL url = new URL(URLDecoder.decode(urlStr, "UTF-8"));
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
						String contentType = conn.getContentType();
						if (contentType == null || contentType.isEmpty()) {
							response.setContentType("text/plain");
							LOGGER.log(Level.INFO, "Can not retrieved the content, falling back to: " + response.getContentType());
						} else {
							response.setContentType(contentType);
							LOGGER.log(Level.INFO, "Set content type using URL connection content type: " + response.getContentType());
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
					} else {
						LOGGER.log(Level.WARNING, "Can not open the URL connection.");
						throw new ServletException("Can not open the URL connection.");
					}
				} else {
					response.setContentType("text/plain");
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

					String responseTxt = "Bad request, url: " + urlStr;
					LOGGER.log(Level.WARNING, responseTxt);

					ServletUtils.sendResponse(request, response, responseTxt);
				}
			} catch (Exception e) {
				response.setContentType("text/plain");
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

				String responseTxt = "Some unexpected error occurred. Error text was: " + e.getMessage();
				LOGGER.log(Level.WARNING, responseTxt, e);

				ServletUtils.sendResponse(request, response, responseTxt);
			}
		} else {
			LOGGER.log(Level.WARNING, "Can't get url value from request.");
			throw new ServletException("Can't get url value from request.");
		}
	}
}
