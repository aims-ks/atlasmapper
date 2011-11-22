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
--%>

<%@page import="au.gov.aims.atlasmapperserver.ConfigType"%>
<%@page import="au.gov.aims.atlasmapperserver.ClientConfig"%>
<%@page import="au.gov.aims.atlasmapperserver.ConfigHelper"%>
<%@page import="au.gov.aims.atlasmapperserver.ConfigManager"%>
<%@page import="au.gov.aims.atlasmapperserver.Utils"%>
<%@page import="org.json.JSONArray"%>
<%@page import="java.util.List"%>
<%@page import="org.json.JSONObject"%>
<%@page contentType="application/json" pageEncoding="UTF-8"%>
<%
	String actionStr = request.getParameter("action");
	String layerIdsStr = request.getParameter("layerIds");
	String clientId = request.getParameter("client");

	ConfigManager configManager = ConfigHelper.getConfigManager(getServletContext());

	int indent = (request.getParameter("indent") != null ? Integer.parseInt(request.getParameter("indent")) : 0);
	JSONObject jsonObj = new JSONObject();

	// live:
	//     true: Get the config from the live server (slow)
	//     false (default): Get the config from generated config files (fast)
	// "live" is true only when it's value is the String "true", ignoring case.
	boolean live = (request.getParameter("live") == null ? false : Boolean.parseBoolean(request.getParameter("live")));

	if (Utils.isBlank(clientId)) {
		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		jsonObj.put("success", false);
		jsonObj.put("errors", new JSONArray().put("Missing parameter [client]."));
	} else {
		ClientConfig clientConfig = configManager.getClientConfig(clientId);
		if (clientConfig == null) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			jsonObj.put("success", false);
			jsonObj.put("errors", new JSONArray().put("This client "+clientId+" do not exists."));
		} else {
			if (Utils.isNotBlank(actionStr)) {
				if ("GET_LIVE_CONFIG".equalsIgnoreCase(actionStr)) {
					JSONObject fullConfig = null;
					try {
						fullConfig = configManager.getClientConfigFileJSon(clientConfig, ConfigType.FULL, true, true);
					} catch (Exception ex) {
						response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						jsonObj.put("success", false);
						jsonObj.put("errors", new JSONArray().put("Exception while generating the new Live Full config. Check the server logs."));
						ex.printStackTrace();
					}
					if (fullConfig == null || fullConfig.length() <= 0) {
						response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						jsonObj.put("success", false);
						jsonObj.put("errors", new JSONArray().put("The new Live Full config is empty."));
					} else {
						response.setStatus(HttpServletResponse.SC_OK);
						jsonObj.put("success", true);
						jsonObj.put("message", "Live Full config");
						jsonObj.put("data", fullConfig);
					}
				} else {
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					jsonObj.put("success", false);
					jsonObj.put("errors", new JSONArray().put("Unknown action ["+actionStr+"]."));
				}
			} else if (Utils.isNotBlank(layerIdsStr)) {
				String[] layerIds = layerIdsStr.split("\\s*,\\s*");
				JSONObject foundLayers = configManager.getClientLayers(clientConfig, layerIds, live);

				if (foundLayers == null || foundLayers.length() <= 0) {
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					jsonObj.put("success", false);
					jsonObj.put("errors", new JSONArray().put("Layers not found."));
				} else {
					response.setStatus(HttpServletResponse.SC_OK);
					jsonObj.put("success", true);
					jsonObj.put("message", "Layers found");
					jsonObj.put("data", foundLayers);
				}
			} else {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				jsonObj.put("success", false);
				jsonObj.put("errors", new JSONArray().put("Missing parameter [action] OR [layerIds]."));
			}
		}
	}

	String outout = "";
	if (indent > 0) {
		outout = jsonObj.toString(indent);
	} else {
		outout = jsonObj.toString();
	}
%>
<%=outout %>
