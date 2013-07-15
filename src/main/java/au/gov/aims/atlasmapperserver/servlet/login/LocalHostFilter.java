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

package au.gov.aims.atlasmapperserver.servlet.login;

import au.gov.aims.atlasmapperserver.Utils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Protect pages under the private folder
 * @author glafond
 */
public class LocalHostFilter implements Filter {
	private static final Logger LOGGER = Logger.getLogger(SecureFilter.class.getName());

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {}

	@Override
	public void destroy() {}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		if (request == null || !(request instanceof HttpServletRequest) ||
				response == null || !(response instanceof HttpServletResponse)) {

			LOGGER.log(Level.SEVERE, "A page was requested using an unsupported protocol.");
			throw new IllegalArgumentException("A page was requested using an unsupported protocol.");
		}
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;

		this.doFilter(httpRequest, httpResponse, chain);
	}

	public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
		String uri = request.getRequestURI();
		String userIP = request.getRemoteAddr();

		if (this.isAllowedIP(userIP)) {
			LOGGER.log(Level.FINE, "ALLOWED: The protected resource [{0}] has been access by [{1}].", new Object[]{
				uri,
				userIP
			});
			chain.doFilter(request, response);
		} else {
			LOGGER.log(Level.FINE, "BLOCKED: An user [{0}] has tried to access the protected resource [{1}].", new Object[]{
				userIP,
				uri
			});
			this.sendUnauthorizedResponse(response);
		}
	}

	private boolean isAllowedIP(String ip) {
		// Requested from localhost (IPv4 or IPv6)
		return "127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip);
	}

	private void sendUnauthorizedResponse(HttpServletResponse response) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		ServletOutputStream out = null;

		JSONObject jsonError = new JSONObject();
		try {
			jsonError.put("success", false);
			jsonError.put("errors", new JSONArray().put("Access denied: This page can only be accessed from localhost."));
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
}
