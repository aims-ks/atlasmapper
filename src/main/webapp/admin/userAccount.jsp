<%--
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

	ConfigManager configManager = ConfigHelper.getConfigManager(this.getServletContext());
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
						LOGGER.log(Level.SEVERE, "An error occured while retriving the user configuration.", e);
						response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						jsonObj.put("success", false);
						jsonObj.put("errors", new JSONArray().put("An error occured while retriving the user configuration. Check your server log."));
					}
					break;

				case UPDATE:
					// Get data from the form, update the config, save it, display the result
					try {
						loggedUser.update(request.getParameterMap());
						String newPassword = request.getParameter("password");
						if (Utils.isNotBlank(newPassword)) {
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
						LOGGER.log(Level.SEVERE, "An error occured while updating user the configuration.", e);
						response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						jsonObj.put("success", false);
						jsonObj.put("errors", new JSONArray().put("An error occured while updating the user configuration. Check your server log."));
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
