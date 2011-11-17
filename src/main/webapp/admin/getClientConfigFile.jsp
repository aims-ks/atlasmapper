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

	Document   : clientsConfig
	Created on : 28/06/2011, 4:08:39 PM
	Author     : glafond

	NOTE: This page return text/plain data, so the empty line at the beginning
	of the file become visible to the client. Putting the new line in the ASP
	tag fix that problem.

--%><%@page import="au.gov.aims.atlasmapperserver.ConfigType"
%><%@page import="au.gov.aims.atlasmapperserver.Utils"
%><%@page import="java.util.List"
%><%@page import="org.json.JSONArray"
%><%@page import="java.util.logging.Level"
%><%@page import="java.util.logging.Logger"
%><%@page import="au.gov.aims.atlasmapperserver.ClientConfig"
%><%@page import="org.json.JSONObject"
%><%@page import="au.gov.aims.atlasmapperserver.ConfigHelper"
%><%@page import="au.gov.aims.atlasmapperserver.ConfigManager"
%><%@page import="au.gov.aims.atlasmapperserver.ActionType"
%><%@page contentType="text/plain" pageEncoding="UTF-8"
%><%
	Logger LOGGER = Logger.getLogger("getClientConfigFile.jsp");

	ConfigManager configManager = ConfigHelper.getConfigManager(this.getServletContext());

	String clientIdStr = request.getParameter("clientId");
	String clientName = request.getParameter("clientName");
	String configTypeStr = request.getParameter("configType");
	boolean live = Boolean.parseBoolean(request.getParameter("live"));

	String output = "";

	if (Utils.isBlank(clientIdStr) && Utils.isBlank(clientName)) {
		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		output = "Missing parameter [clientId] or [clientName].";
	} else if (Utils.isBlank(configTypeStr)) {
		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		output = "Missing parameter [configType].";
	} else {
		ClientConfig foundClientConfig = null;
		if (Utils.isNotBlank(clientIdStr)) {
			try {
				Integer clientId = Integer.valueOf(clientIdStr);
				foundClientConfig = configManager.getClientConfig(clientId);
				if (foundClientConfig == null) {
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					output = "Client id ["+clientId+"] not found.";
				}
			} catch(Exception e) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				output = "Invalid clientId format.";
			}
		} else if (Utils.isNotBlank(clientName)) {
			foundClientConfig = configManager.getClientConfig(clientName);
			if (foundClientConfig == null) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				output = "Client name ["+clientName+"] not found.";
			}
		}

		ConfigType configType = null;
		try {
			configType = ConfigType.valueOf(configTypeStr.toUpperCase());
		} catch(Exception e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			output = "Invalid configType.";
		}

		if (foundClientConfig != null && configType != null) {
			try {
				JSONObject configs = configManager.getClientConfigFileJSon(foundClientConfig, configType, live, live);
				if (configs == null) {
					response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					output = "An error occured while retrieving/generating the Client configurations. Check your server log.";
				} else {
					response.setStatus(HttpServletResponse.SC_OK);
					output = Utils.jsonToStr(configs);
				}
			} catch(Exception e) {
				LOGGER.log(Level.SEVERE, "An error occured while retrieving/generating the Client configurations.", e);
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				output = "An error occured while retrieving/generating the Client configurations. Check your server log.";
			}
		}
	}
%><%=output %>
