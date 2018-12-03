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

package au.gov.aims.atlasmapperserver.servlet.login;

import au.gov.aims.atlasmapperserver.User;
import au.gov.aims.atlasmapperserver.Utils;
import au.gov.aims.atlasmapperserver.servlet.FileFinder;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Protect pages under the private folder
 * @author glafond
 */
public class SecureFilter implements Filter {
	private static final Logger LOGGER = Logger.getLogger(SecureFilter.class.getName());
	private ServletContext context = null;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		this.context = filterConfig.getServletContext();

		// The filters are initialised when the application starts.
		// This logging has been put here simply because it's the only "user-defined" filter
		// used by the application, therefor the only place where it can log the variable
		// when the application loads.
		// It init the folders and prints the value of the AtlasMapper variable (for logging purpose only)
		// OR an "easy to spot" error message:
		// ---------------------------------------
		// - ATLASMAPPER_DATA_DIR: /path/to/atlasmapperFiles
		// ---------------------------------------
		FileFinder.init(this.context);
	}

	@Override
	public void destroy() {}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		if (request == null || !(request instanceof HttpServletRequest) ||
				response == null || !(response instanceof HttpServletResponse)) {

			LOGGER.log(Level.SEVERE, "BLOCKED: A page was requested using an unsupported protocol.");
			throw new IllegalArgumentException("BLOCKED: A page was requested using an unsupported protocol.");
		}
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;

		this.doFilter(httpRequest, httpResponse, chain);
	}

	public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
		User loggedUser = null;

		HttpSession session = request.getSession();
		if (session != null) {
			loggedUser = LoginServlet.getLoggedUser(session);
			if (loggedUser != null) {
				request.setAttribute("loggedUser.login", loggedUser.getLoginName());
				request.setAttribute("loggedUser.name", loggedUser.getName());
			}
		}

		String uri = request.getRequestURI();
		String userIP = request.getRemoteAddr();
		if (loggedUser == null) {
			LOGGER.log(Level.FINE, "BLOCKED: An anonymous user [{0}] has tried to access the protected resource [{1}].", new Object[]{
				userIP,
				uri
			});
			if (this.expectJSON(request)) {
				this.sendJSONTimeoutError(response);
			} else {
				this.sendRedirection(response);
			}
		} else if (!this.isServerStateValid()) {
			LOGGER.log(Level.WARNING, "BLOCKED: The server state is not valid. Redirect to the login page.");
			if (this.expectJSON(request)) {
				this.sendJSONInvalidServerState(response);
			} else {
				this.sendRedirection(response);
			}
		} else {
			LOGGER.log(Level.FINE, "ALLOWED: User [{0}] [{1}] is accessing the protected resource [{2}].", new Object[]{
				loggedUser.getLoginName(),
				userIP,
				uri
			});
			chain.doFilter(request, response);
		}
	}

	// - Check if the request expect a JSON response -
	// NOTE: There is not easy solution for this. The application
	// assume the client expect JSON response if:
	//     * the request parameter jsonResponse exists and is true;
	// OR
	//     * the request sent JSON data;
	private boolean expectJSON(HttpServletRequest request) {
		String requestContentType = request.getContentType();
		String jsonResponse = request.getParameter("jsonResponse");
		if (jsonResponse != null) {
			return Boolean.parseBoolean(jsonResponse);
		}

		return requestContentType != null &&
				requestContentType.toLowerCase().indexOf("application/json") != -1;
	}

	private boolean isServerStateValid() {
		File applicationFolder = FileFinder.getApplicationFolder(this.context, false);

		return applicationFolder != null && Utils.recursiveIsWritable(applicationFolder);
	}

	private void sendJSONTimeoutError(HttpServletResponse response) throws IOException {
		// The file Frameset.js redirect to the login when this status code is returned
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		ServletOutputStream out = null;
		JSONObject jsonError = new JSONObject();
		try {
			jsonError.put("success", false);
			jsonError.put("errors", new JSONArray().put("Session timed out. Please, re-log in prior to execute this operation."));
		} catch (JSONException ex) {
			LOGGER.log(Level.WARNING, "Can not create a JSON error message: {0}", Utils.getExceptionMessage(ex));
			LOGGER.log(Level.FINE, "Stack trace: ", ex);
		}
		try {
			out = response.getOutputStream();
			out.println(jsonError.toString());
		} finally {
			if (out != null) {
				try {
					out.flush();
				} finally {
					out.close();
				}
			}
		}
	}

	private void sendJSONInvalidServerState(HttpServletResponse response) throws IOException {
		// The file Frameset.js redirect to the login when this status code is returned
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		ServletOutputStream out = null;
		JSONObject jsonError = new JSONObject();
		try {
			jsonError.put("success", false);
			jsonError.put("errors", new JSONArray().put("The server is in an invalid state."));
		} catch (JSONException ex) {
			LOGGER.log(Level.WARNING, "Can not create a JSON error message: {0}", Utils.getExceptionMessage(ex));
			LOGGER.log(Level.FINE, "Stack trace: ", ex);
		}
		try {
			out = response.getOutputStream();
			out.println(jsonError.toString());
		} finally {
			if (out != null) {
				try {
					out.flush();
				} finally {
					out.close();
				}
			}
		}
	}

	private void sendRedirection(HttpServletResponse response) throws IOException {
		response.sendRedirect(LoginServlet.REDIRECT_PAGE);
	}
}
