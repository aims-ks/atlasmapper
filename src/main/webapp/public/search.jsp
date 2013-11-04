<%--
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

	Document   : layersInfo
	Created on : 11/07/2011, 10:20:45 AM
	Author     : glafond
	Description: Return complete information in JSON format, about one of more layers.

	return: JSONObject {
		length: (Number) Total amount of results returned by the search.
		results: (JSONArray of Result) Part fo the results, according to the parameters offset and qte.
	}

	Result: JSONObject {
		title: (String) Display result
		id: (String) Unique identifier for the result
		polygon: (JSONArray of Coordinates) Array of coordinates. Example: "polygon: [[0,0], [0,10], [10,10], [10,0]]"
		center: (Coordinate) Coordinate of the center of the polygon, used to locate the marker on the map.
	}

	Coordinate: JSONArray containing 2 doubles; [longitude, latitude].
--%>

<%@page import="au.gov.aims.atlasmapperserver.ClientConfig"%>
<%@page import="au.gov.aims.atlasmapperserver.ConfigHelper"%>
<%@page import="au.gov.aims.atlasmapperserver.ConfigManager"%>
<%@page import="au.gov.aims.atlasmapperserver.Utils"%>
<%@page import="org.json.JSONArray"%>
<%@page import="org.json.JSONObject"%>
<%
	// Needed by StripTagProxy:
	//     http://docs.sencha.com/extjs/3.4.0/#!/api/Ext.data.ScriptTagProxy
	String callback = request.getParameter("callback");

	response.setCharacterEncoding("UTF-8");
	if (callback != null) {
		// Equivalent to: <@page contentType="text/javascript" pageEncoding="UTF-8">
		response.setContentType("text/javascript");
	} else {
		// Equivalent to: <@page contentType="application/json" pageEncoding="UTF-8">
		response.setContentType("application/json");
	}

	String clientId = request.getParameter("client");

	// Search type: Currently only support location search (default: LOCATION).
	String searchTypeStr = request.getParameter("type");

	// The query string, as entered by the user in the search field.
	String query = request.getParameter("query");
	// Map bounds, to help the server to order the results.
	String bounds = request.getParameter("bounds");

	// Start from result (default: 0 => Start from the first result).
	int offset = (request.getParameter("offset") != null ? Integer.parseInt(request.getParameter("offset")) :
			(request.getParameter("start") != null ? Integer.parseInt(request.getParameter("start")) : 0));
	// Maximum number of results that has to be returned (default: 10).
	int qty = (request.getParameter("qty") != null ? Integer.parseInt(request.getParameter("qty")) :
			(request.getParameter("limit") != null ? Integer.parseInt(request.getParameter("limit")) : 10));

	ConfigManager configManager = ConfigHelper.getConfigManager(this.getServletConfig().getServletContext());

	int indent = (request.getParameter("indent") != null ? Integer.parseInt(request.getParameter("indent")) : 0);
	JSONObject jsonObj = new JSONObject();

	if (Utils.isBlank(clientId)) {
		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		jsonObj.put("success", false);
		jsonObj.put("errors", new JSONArray().put("Missing parameter [client]."));
	} else {
		ClientConfig clientConfig = configManager.getClientConfig(clientId);
		if (clientConfig == null) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			jsonObj.put("success", false);
			jsonObj.put("errors", new JSONArray().put("The client "+clientId+" do not exists."));
		} else {
			if (Utils.isBlank(searchTypeStr) || "LOCATION".equalsIgnoreCase(searchTypeStr)) {
				JSONObject results = null;

				try {
					results = clientConfig.locationSearch(query, bounds, offset, qty);
				} catch (Exception ex) {
					response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					jsonObj.put("success", false);
					jsonObj.put("errors", new JSONArray().put("Exception while performing the location search."));
					ex.printStackTrace();
				}

				response.setStatus(HttpServletResponse.SC_OK);
				jsonObj.put("success", true);
				jsonObj.put("message", "Search results");
				jsonObj.put("data", results);
			} else if ("LAYER".equalsIgnoreCase(searchTypeStr)) {
				JSONObject results = null;

				try {
					results = clientConfig.layerSearch(query, offset, qty);
				} catch (Exception ex) {
					response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					jsonObj.put("success", false);
					jsonObj.put("errors", new JSONArray().put("Exception while performing the layer search."));
					ex.printStackTrace();
				}

				if (results == null) {
					response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					jsonObj.put("success", false);
					jsonObj.put("errors", new JSONArray().put("Layer search result object is empty."));
				} else {
					response.setStatus(HttpServletResponse.SC_OK);
					jsonObj = results;
					jsonObj.put("success", true);
				}
			} else {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				jsonObj.put("success", false);
				jsonObj.put("errors", new JSONArray().put("Invalid search type "+searchTypeStr+"."));
			}
		}
	}

	String output = "";

	if (callback != null) {
		output += callback + "(";
	}

	if (indent > 0) {
		output += jsonObj.toString(indent);
	} else {
		output += jsonObj.toString();
	}

	if (callback != null) {
		output += ");";
	}
%>
<%=output %>
