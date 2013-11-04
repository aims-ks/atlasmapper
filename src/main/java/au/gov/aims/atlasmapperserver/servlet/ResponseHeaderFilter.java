/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2012 Australian Institute of Marine Science
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

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;

/**
 * Allow arbitrary header configuration to be spliced into the response header.
 * The values are set in the filter configuration.
 *
 * Usage: (web.xml) - I know, this is not very java doc friendly...
 *     <filter>
 *       <filter-name>ResponseHeaderFilter</filter-name>
 *       <filter-class>au.gov.aims.atlasmapperserver.servlet.ResponseHeaderFilter</filter-class>
 *       <init-param>
 *         <param-name>Cache-Control</param-name>
 *         <param-value>max-age=3600</param-value>
 *       </init-param>
 *     </filter>
 *
 *     <filter-mapping>
 *       <filter-name>ResponseHeaderFilter</filter-name>
 *       <url-pattern>/logo.png</url-pattern>
 *     </filter-mapping>
 *
 * See: http://www.onjava.com/pub/a/onjava/2004/03/03/filters.html
 */
public class ResponseHeaderFilter implements Filter {
	private FilterConfig filterConfig;

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		if (res instanceof HttpServletResponse) {
			HttpServletResponse response = (HttpServletResponse) res;
			// set the provided HTTP response parameters
			Enumeration<String> e = this.filterConfig.getInitParameterNames();
			while (e.hasMoreElements()) {
				String headerName = e.nextElement();
				response.addHeader(headerName, this.filterConfig.getInitParameter(headerName));
			}
		}
		// pass the request/response on
		chain.doFilter(req, res);
	}

	@Override
	public void init(FilterConfig filterConfig) {
		this.filterConfig = filterConfig;
	}

	@Override
	public void destroy() {
		this.filterConfig = null;
	}
}