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

package au.gov.aims.atlasmapperserver.servlet;

import au.gov.aims.atlasmapperserver.ClientConfig;
import au.gov.aims.atlasmapperserver.ServletUtils;
import au.gov.aims.atlasmapperserver.Utils;
import org.json.JSONException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Generate response to requests sent to www
 * @author glafond
 * References:
 * http://docstore.mik.ua/orelly/java-ent/servlet/ch04_04.htm#ch04-35758
 * http://stackoverflow.com/questions/417658/how-to-config-tomcat-to-serve-images-from-an-external-folder-outside-webapps
 */
public class ClientServlet extends HttpServlet {
	private static final Logger LOGGER = Logger.getLogger(ClientServlet.class.getName());

	// Don't define a constructor! Initialise the Servlet here.
	/*
	@Override
	public void init(ServletConfig sc) throws ServletException {
		super.init(sc);
	}
	*/

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		this.performTask(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		this.performTask(request, response);
	}

	private void performTask(HttpServletRequest request, HttpServletResponse response) throws IOException {
		// The request attributes can be very confusing.
		// There is an example of a request and it's attributes:
		//     Requested URL: "http://localhost:12080/atlasmapper/client/lg/arrow.gif?param=value"
		//     getRequestURI():     "/atlasmapper/client/lg/arrow.gif"
		//     getContextPath():    "/atlasmapper"
		//     getServletPath():    "client"
		//     getPathInfo():       "/lg/arrow.gif"  <=  This is the one needed by this method
		//     getQueryString():    "param=value"
		//     getPathTranslated(): "/home/reefatlas/e-atlas_site/maps/tomcat/webapps/atlasmapper/lg/arrow.gif"  <=  Useful, but not quite what we are looking for
		String fileRelativePath = null;

		// Only used for error messages
		String urlRelativePath = null;

		String filePath = fileRelativePath;
		File file = null;

		// Return the file
		try {
			// Get the file to view
			fileRelativePath = request.getPathInfo();

			// Only used for error messages
			urlRelativePath = request.getRequestURI();

			String clientId = FileFinder.getClientId(fileRelativePath);
			if (clientId != null) {
				if (FileFinder.PUBLIC_FOLDER.equals(clientId)) {
					file = FileFinder.getPublicFile(this.getServletContext(), fileRelativePath);
				} else {
					ClientConfig client = FileFinder.getClientConfig(this.getServletContext(), clientId);

					if (client == null) {
						response.setStatus(HttpServletResponse.SC_NOT_FOUND);
						ServletUtils.sendResponse(response, clientId + " do not exists.");
						return;
					}

					if (client.isEnable()) {
						file = FileFinder.getClientFile(this.getServletContext(), fileRelativePath);
					} else {
						response.setStatus(HttpServletResponse.SC_NOT_FOUND);
						ServletUtils.sendResponse(response, client.getClientName() + " mapping service has been disabled.");
						return;
					}

					// If the file is a folder, try to add "index.html".
					if (file != null && file.isDirectory()) {
						String indexURL = FileFinder.getAtlasMapperClientURL(this.getServletContext(), client);
						if (indexURL == null) {
							response.setStatus(HttpServletResponse.SC_NOT_FOUND);
							LOGGER.log(Level.WARNING, "{0} do not have an index.html file.", client.getClientName());
							ServletUtils.sendResponse(response, client.getClientName() + " do not have an index.html file.");
							return;
						}
						response.sendRedirect(indexURL);
						return;
					}
				}
			}

			// No file, nothing to view
			if (file == null) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				LOGGER.log(Level.INFO, "File not found [{0}]", fileRelativePath);
				ServletUtils.sendResponse(response, "File not found [" + urlRelativePath + "]");
				return;
			}

			filePath = file.getAbsolutePath();

			// Get and set the type of the file
			String contentType = getServletContext().getMimeType(file.getCanonicalPath());
			response.setContentType(contentType);
			response.setStatus(HttpServletResponse.SC_OK);

			// TODO Set ttl according to the request (that could help the browser to know when to clear it's cache) - I'm not sure how we would like to do that...
			/*
			String RFC1123_PATTERN = "EEE, dd MMM yyyy HH:mm:ss z";
			SimpleDateFormat rfc1123Format = new SimpleDateFormat(RFC1123_PATTERN, Locale.US);
			rfc1123Format.setTimeZone(TimeZone.getTimeZone("GMT"));

			Date expireDate = new Date((new Date().getTime()) + ttl);
			response.setHeader("Expires", rfc1123Format.format(expireDate));
			*/

			ServletUtils.sendResponse(response, file);
		} catch (FileNotFoundException e) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			LOGGER.log(Level.INFO, "File not found [{0}]", filePath);
			ServletUtils.sendResponse(response, "File not found [" + urlRelativePath + "]");
		} catch (IOException e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			LOGGER.log(Level.SEVERE, "Problem sending file [{0}]: {1}",
					new String[]{ filePath, Utils.getExceptionMessage(e) });
			LOGGER.log(Level.FINE, "Stack trace: ", e);
			ServletUtils.sendResponse(response, "Problem sending file [" + urlRelativePath + "]: " + Utils.getExceptionMessage(e));
		} catch (JSONException e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			LOGGER.log(Level.SEVERE, "Error occurred while loading the configuration file [{0}]: {1}",
					new String[]{ filePath, Utils.getExceptionMessage(e) });
			LOGGER.log(Level.FINE, "Stack trace: ", e);
			ServletUtils.sendResponse(response, "Error occurred while loading the configuration file [" + urlRelativePath + "]: " + Utils.getExceptionMessage(e));
		}
	}
}
