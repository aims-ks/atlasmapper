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

package au.gov.aims.atlasmapperserver;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This utility class provide tools to simplify some operation related
 * to servlets. Unfortunately, this class can not be test since the servlet
 * libraries can not be load in the context of a Test Case.
 *
 * @author glafond
 */
public class ServletUtils {
	private static final Logger LOGGER = Logger.getLogger(ServletUtils.class.getName());

	public static void sendResponse(
			HttpServletResponse response,
			File file) throws IOException {

		if (response == null || file == null) {
			return;
		}

		InputStream responseStream = null;
		try {
			responseStream = new FileInputStream(file);
			ServletUtils.sendResponse(response, responseStream);
		} finally {
			if (responseStream != null) {
				try {
					responseStream.close();
				} catch (Exception ex) {
					LOGGER.log(Level.WARNING, "Cant close the FileInputStream: {0}", Utils.getExceptionMessage(ex));
					LOGGER.log(Level.FINE, "Stack trace: ", ex);
				}
			}
		}
	}

	public static void sendResponse(
			HttpServletResponse response,
			String responseTxt) throws IOException {

		if (response == null || responseTxt == null) {
			return;
		}

		InputStream responseStream = null;
		try {
			responseStream = new ByteArrayInputStream(responseTxt.getBytes());
			ServletUtils.sendResponse(response, responseStream);
		} finally {
			if (responseStream != null) {
				try {
					responseStream.close();
				} catch (Exception ex) {
					LOGGER.log(Level.WARNING, "Cant close the ByteArrayInputStream: {0}", Utils.getExceptionMessage(ex));
					LOGGER.log(Level.FINE, "Stack trace: ", ex);
				}
			}
		}
	}

	public static void sendResponse(
			HttpServletResponse response,
			InputStream responseStream) throws IOException {

		if (response == null || responseStream == null) {
			return;
		}

		OutputStream out = null;

		try {
			out = response.getOutputStream();

			Utils.binaryCopy(responseStream, out);
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch(Exception e) {
					LOGGER.log(Level.SEVERE, "Cant close the output: {0}", Utils.getExceptionMessage(e));
					LOGGER.log(Level.FINE, "Stack trace: ", e);
				}
			}
		}
	}

	public static String[] getComaSeparatedParameters(HttpServletRequest request, String parameter) throws UnsupportedEncodingException {
		String rawValue = ServletUtils.getRawParameter(request, parameter);
		if (rawValue == null) {
			return null;
		}

		String[] rawValueParts = rawValue.split("\\s*,\\s*");
		String[] valueParts = new String[rawValueParts.length];
		for (int i=0; i<rawValueParts.length; i++) {
			valueParts[i] = URLDecoder.decode(rawValueParts[i], "UTF-8");
		}

		return valueParts;
	}

	public static String getRawParameter(HttpServletRequest request, String parameter) throws UnsupportedEncodingException {
		if (Utils.isBlank(parameter)) {
			return null;
		}

		// query = the query string; <key>=<value>&<key>=<value>...
		String query = request.getQueryString();
		String[] queryParts = query.split("&");
		for (String queryPart : queryParts) {
			String[] queryPair = queryPart.split("=");
			String key = URLDecoder.decode(queryPair[0], "UTF-8");
			if (parameter.equals(key)) {
				return queryPair[1];
			}
		}
		return null;
	}
}
