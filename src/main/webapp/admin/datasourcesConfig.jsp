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

	Document   : datasourcesConfig
	Created on : 28/06/2011, 4:08:39 PM
	Author     : glafond
--%>

<%@page import="au.gov.aims.atlasmapperserver.Utils"%>
<%@page import="java.util.List"%>
<%@page import="java.util.logging.Level"%>
<%@page import="java.util.logging.Logger"%>
<%@page import="org.json.JSONArray"%>
<%@page import="au.gov.aims.atlasmapperserver.DatasourceConfig"%>
<%@page import="au.gov.aims.atlasmapperserver.ConfigHelper"%>
<%@page import="org.json.JSONObject"%>
<%@page import="au.gov.aims.atlasmapperserver.ConfigManager"%>
<%@page import="au.gov.aims.atlasmapperserver.ActionType"%>
<%@page contentType="application/json" pageEncoding="UTF-8"%>
<%
	Logger LOGGER = Logger.getLogger("datasourcesConfig.jsp");

	ConfigManager configManager = ConfigHelper.getConfigManager(this.getServletContext());

	String actionStr = request.getParameter("action");

	JSONObject jsonObj = new JSONObject();

	if (Utils.isNotBlank(actionStr)) {
		ActionType action = null;
		try {
			action = ActionType.valueOf(actionStr.toUpperCase());
		} catch (Exception ex) {}

		if (action == null) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			jsonObj.put("success", false);
			jsonObj.put("errors", new JSONArray().put("Unknown action ["+actionStr+"]."));
		} else {
			switch(action) {
				case READ:
					try {
						response.setStatus(HttpServletResponse.SC_OK);
						jsonObj.put("success", true);
						jsonObj.put("message", "Loaded data");
						jsonObj.put("data", configManager.getDatasourceConfigsJSon());
					} catch (Exception e) {
						LOGGER.log(Level.SEVERE, "An error occured while retriving the datasource configuration.", e);
						response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						jsonObj.put("success", false);
						jsonObj.put("errors", new JSONArray().put("An error occured while retriving the datasource configuration. Check your server log."));
					}
					break;

				case CREATE:
					// Get data from the form, create the config entry, save it, display the result
					try {
						List<DatasourceConfig> datasourceConfigs = configManager.createDatasourceConfig(request);
						JSONArray datasourceJSonArr = new JSONArray();;
						if (datasourceConfigs != null) {
							for (DatasourceConfig datasourceConfig : datasourceConfigs) {
								JSONObject datasourceJSon = datasourceConfig.toJSonObject();
								if (datasourceJSon != null) {
									datasourceJSonArr.put(datasourceJSon);
								}
							}
						}
						ConfigHelper.save();
						response.setStatus(HttpServletResponse.SC_OK);
						jsonObj.put("success", true);
						jsonObj.put("message", "Created record");
						jsonObj.put("data", datasourceJSonArr);
					} catch (Exception e) {
						LOGGER.log(Level.SEVERE, "An error occured while creating a new datasource.", e);
						response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						jsonObj.put("success", false);
						jsonObj.put("errors", new JSONArray().put("An error occured while creating a new datasource. Check your server log."));
					}
					break;

				case UPDATE:
					// Get data from the form, update the config entry, save it, display the result
					try {
						configManager.updateDatasourceConfig(request);
						ConfigHelper.save();
						response.setStatus(HttpServletResponse.SC_OK);
						jsonObj.put("success", true);
						jsonObj.put("message", "Updated record");
						jsonObj.put("data", configManager.getDatasourceConfigsJSon());
					} catch (Exception e) {
						LOGGER.log(Level.SEVERE, "An error occured while updating the datasource.", e);
						response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						jsonObj.put("success", false);
						jsonObj.put("errors", new JSONArray().put("An error occured while updating the datasource. Check your server log."));
					}
					break;

				case DESTROY:
					// Get data from the form, delete the config entry, save it, display the result
					try {
						configManager.destroyDatasourceConfig(request);
						ConfigHelper.save();
						response.setStatus(HttpServletResponse.SC_OK);
						jsonObj.put("success", true);
						jsonObj.put("message", "Deleted record");
						jsonObj.put("data", configManager.getDatasourceConfigsJSon());
					} catch (Exception e) {
						LOGGER.log(Level.SEVERE, "An error occured while deleting the datasource.", e);
						response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						jsonObj.put("success", false);
						jsonObj.put("errors", new JSONArray().put("An error occured while deleting the datasource. Check your server log."));
					}
					break;

				default:
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					jsonObj.put("success", false);
					jsonObj.put("errors", new JSONArray().put("Unknown action '"+actionStr+"'."));
					break;
			}
		}
	} else {
		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		jsonObj.put("success", false);
		jsonObj.put("errors", new JSONArray().put("Missing parameter [action]."));
	}
%>
<%=jsonObj.toString() %>
