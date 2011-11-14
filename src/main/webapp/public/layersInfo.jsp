<%-- 
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
	String clientName = request.getParameter("client");

	ConfigManager configManager = ConfigHelper.getConfigManager(getServletContext());

	int indent = (request.getParameter("indent") != null ? Integer.parseInt(request.getParameter("indent")) : 0);
	JSONObject jsonObj = new JSONObject();

	// live:
	//     true: Get the config from the live server (slow)
	//     false (default): Get the config from generated config files (fast)
	// "live" is true only when it's value is the String "true", ignoring case.
	boolean live = (request.getParameter("live") == null ? false : Boolean.parseBoolean(request.getParameter("live")));

	if (Utils.isBlank(clientName)) {
		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		jsonObj.put("success", false);
		jsonObj.put("errors", new JSONArray().put("Missing parameter [client]."));
	} else {
		ClientConfig clientConfig = configManager.getClientConfig(clientName);
		if (clientConfig == null) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			jsonObj.put("success", false);
			jsonObj.put("errors", new JSONArray().put("This client "+clientName+" do not exists."));
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
