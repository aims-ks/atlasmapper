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

import au.gov.aims.atlasmapperserver.ClientConfig;
import au.gov.aims.atlasmapperserver.ServletUtils;
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
		// http://localhost:12080/atlasmapper/client/lg/arrow.gif?param=value
		// RequestURI:  [/atlasmapper/client/lg/arrow.gif]
		// ContextPath: [/atlasmapper]
		// ServletPath: [/client]
		// PathInfo:    [/lg/arrow.gif]  <=  This is the one
		// QueryString: [param=value]
		// PathTranslated: [/home/reefatlas/e-atlas_site/maps/tomcat/webapps/atlasmapper/lg/arrow.gif]  <=  Useful, but not quite what we are looking for
		//System.out.println("RequestURI: [" + request.getRequestURI() + "]  ServletPath: [" + request.getServletPath() + "]  PathInfo: [" + request.getPathInfo() + "]  ContextPath: [" + request.getContextPath() + "]  PathTranslated: [" + request.getPathTranslated() + "]");
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
					if (client != null) {
						if (client.isEnable()) {
							file = FileFinder.getClientFile(this.getServletContext(), fileRelativePath);
						} else {
							response.setStatus(HttpServletResponse.SC_NOT_FOUND);
							ServletUtils.sendResponse(response, client.getClientName() + " mapping service has been disabled.");
							return;
						}
					}

					// If the file is a folder, try to add "index.html".
					if (file.isDirectory()) {
						String indexURL = FileFinder.getAtlasMapperClientURL(this.getServletContext(), client, false);
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

			ServletUtils.sendResponse(response, file);
		} catch (FileNotFoundException e) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			LOGGER.log(Level.INFO, "File not found [{0}]", filePath);
			ServletUtils.sendResponse(response, "File not found [" + urlRelativePath + "]");
		} catch (IOException e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			LOGGER.log(Level.SEVERE, "Problem sending file [" + filePath + "]: ", e);
			ServletUtils.sendResponse(response, "Problem sending file [" + urlRelativePath + "]: " + e.getMessage());
		} catch (JSONException e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			LOGGER.log(Level.SEVERE, "Error occurred while loading the configuration file [" + filePath + "]: ", e);
			ServletUtils.sendResponse(response, "Error occurred while loading the configuration file [" + urlRelativePath + "]: " + e.getMessage());
		}
	}
}
