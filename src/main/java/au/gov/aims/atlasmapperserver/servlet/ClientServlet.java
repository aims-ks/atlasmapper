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

import au.gov.aims.atlasmapperserver.ServletUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
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
		// http://localhost:12080/atlasmapperserver/www/images/test.jpg?param=value
		// RequestURI:  [/atlasmapperserver/www/images/test.jpg]
		// ContextPath: [/atlasmapperserver]
		// ServletPath: [/www]
		// PathInfo:    [/images/test.jpg]  <=  This is the one
		// QueryString: [param=value]
		// PathTranslated: [/home/reefatlas/e-atlas_site/maps/tomcat/webapps/atlasmapperserver/images/test.jpg]  <=  Useful, but not quite what we are looking for
		// System.out.println("RequestURI: [" + request.getRequestURI() + "]  ServletPath: [" + request.getServletPath() + "]  PathInfo: [" + request.getPathInfo() + "]  ContextPath: [" + request.getContextPath() + "]");
		File file = null;

		// Return the file
		try {
			// Get the file to view
			String fileRelativePath = request.getPathInfo();
			file = FileFinder.getClientFile(this.getServletContext(), fileRelativePath);

			// No file, nothing to view
			if (file == null) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				ServletUtils.sendResponse(response, "No file to view");
				return;
			}

			// Get and set the type of the file
			String contentType = getServletContext().getMimeType(file.getCanonicalPath());
			response.setContentType(contentType);
			response.setStatus(HttpServletResponse.SC_OK);

			ServletUtils.sendResponse(response, file);
		} catch (FileNotFoundException e) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			if (file != null) {
				LOGGER.log(Level.WARNING, "File not found [{0}]", file.getAbsolutePath());
			} else {
				LOGGER.log(Level.WARNING, "File not found - file path unknown?");
			}
			ServletUtils.sendResponse(response, "File not found");
		} catch (IOException e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			if (file != null) {
				LOGGER.log(Level.SEVERE, "Problem sending file [" + file.getAbsolutePath() + "]: ", e);
			} else {
				LOGGER.log(Level.SEVERE, "Problem sending file: ", e);
			}
			ServletUtils.sendResponse(response, "Problem sending file: " + e.getMessage());
		}
	}
}
