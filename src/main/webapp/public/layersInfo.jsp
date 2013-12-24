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
<%@page import="au.gov.aims.atlasmapperserver.ClientConfig"%>
<%@page import="au.gov.aims.atlasmapperserver.ConfigHelper"%>
<%@page import="au.gov.aims.atlasmapperserver.ConfigManager"%>
<%@page import="au.gov.aims.atlasmapperserver.Utils"%>
<%@page import="org.json.JSONArray"%>
<%@page import="org.json.JSONObject"%>
<%@page import="au.gov.aims.atlasmapperserver.ServletUtils"%><%@ page import="au.gov.aims.atlasmapperserver.jsonWrappers.client.URLSaveState"%>
<%@page contentType="application/json" pageEncoding="UTF-8"%>
<%
	String[] layerIds = ServletUtils.getComaSeparatedParameters(request, "layerIds");
	String iso19115_19139url = request.getParameter("iso19115_19139url");
	String clientId = request.getParameter("client");

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
			if (Utils.isNotBlank(iso19115_19139url)) {
				URLSaveState mapState = configManager.getMapStateForDataset(clientConfig, iso19115_19139url);

				if (mapState == null) {
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					jsonObj.put("success", false);
					jsonObj.put("errors", new JSONArray().put("Layers not found."));
				} else {
					response.setStatus(HttpServletResponse.SC_OK);
					jsonObj.put("success", true);
					jsonObj.put("message", "Layers found");
					jsonObj.put("data", mapState.getJSON());
				}
			} else if (layerIds != null) {
				JSONObject foundLayers = configManager.getClientLayers(clientConfig, layerIds);

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

	String output = "";
	if (indent > 0) {
		output = jsonObj.toString(indent);
	} else {
		output = jsonObj.toString();
	}
%>
<%=output %>
