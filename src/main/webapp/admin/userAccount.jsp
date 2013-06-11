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

	Document   : userAccount
	Created on : 28/06/2011, 4:08:39 PM
	Author     : glafond
--%>

<%@page import="java.util.logging.Level"%>
<%@page import="java.util.logging.Logger"%>
<%@page import="org.json.JSONArray"%>
<%@page import="au.gov.aims.atlasmapperserver.Utils"%>
<%@page import="au.gov.aims.atlasmapperserver.User"%>
<%@page import="au.gov.aims.atlasmapperserver.servlet.login.LoginServlet"%>
<%@page import="org.json.JSONObject"%>
<%@page import="au.gov.aims.atlasmapperserver.ActionType"%>
<%@page import="au.gov.aims.atlasmapperserver.ConfigHelper"%>
<%@page import="au.gov.aims.atlasmapperserver.ConfigManager"%>
<%@page contentType="application/json" pageEncoding="UTF-8"%>
<%
	Logger LOGGER = Logger.getLogger("userAccount.jsp");

	ConfigManager configManager = ConfigHelper.getConfigManager(this.getServletConfig().getServletContext());
	User loggedUser = LoginServlet.getLoggedUser(session);

	String actionStr = request.getParameter("action");

	JSONObject jsonObj = new JSONObject();

	if (Utils.isNotBlank(actionStr)) {
		ActionType action = null;
		try {
			action = ActionType.valueOf(actionStr.toUpperCase());
		} catch (Exception ex) { }

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
						jsonObj.put("data", loggedUser.toJSonObject());
					} catch (Exception e) {
						LOGGER.log(Level.SEVERE, "An error occurred while retrieving the user configuration: {0}", Utils.getExceptionMessage(e));
						LOGGER.log(Level.WARNING, "Stack trace: ", e);
						response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						jsonObj.put("success", false);
						jsonObj.put("errors", new JSONArray().put("An error occurred while retrieving the user configuration: " + Utils.getExceptionMessage(e) + "\nCheck your server log."));
					}
					break;

				case UPDATE:
					// Get data from the form, update the config, save it, display the result
					try {
						loggedUser.update(request.getParameterMap());
						String newPassword = request.getParameter("password");
						if (Utils.isNotBlank(newPassword) && !configManager.isDemoMode()) {
							String currentPassword = request.getParameter("currentPassword");
							String passwordConfirm = request.getParameter("passwordConfirm");
							if (loggedUser.verifyPassword(currentPassword) && newPassword.equals(passwordConfirm)) {
								loggedUser.setPassword(newPassword);

								configManager.saveUsersConfig();
								response.setStatus(HttpServletResponse.SC_OK);
								jsonObj.put("success", true);
								jsonObj.put("message", "Updated record");
								jsonObj.put("data", configManager.getUser(loggedUser.getLoginName()));
							} else {
								response.setStatus(HttpServletResponse.SC_OK);
								jsonObj.put("success", false);
								jsonObj.put("errors", new JSONArray().put("The current password is invalid."));
							}
						} else {
							configManager.saveUsersConfig();
							response.setStatus(HttpServletResponse.SC_OK);
							jsonObj.put("success", true);
							jsonObj.put("message", "Updated record");
							jsonObj.put("data", configManager.getUser(loggedUser.getLoginName()));
						}
					} catch (Exception e) {
						LOGGER.log(Level.SEVERE, "An error occurred while updating the user configuration: {0}", Utils.getExceptionMessage(e));
						LOGGER.log(Level.WARNING, "Stack trace: ", e);
						response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						jsonObj.put("success", false);
						jsonObj.put("errors", new JSONArray().put("An error occurred while updating the user configuration: " + Utils.getExceptionMessage(e) + "\nCheck your server log."));
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
