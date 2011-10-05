<%--
    Document   : clientsConfig
    Created on : 28/06/2011, 4:08:39 PM
    Author     : glafond
--%>

<%@page import="au.gov.aims.atlasmapperserver.Utils"%>
<%@page import="java.util.List"%>
<%@page import="org.json.JSONArray"%>
<%@page import="java.util.logging.Level"%>
<%@page import="java.util.logging.Logger"%>
<%@page import="au.gov.aims.atlasmapperserver.ClientConfig"%>
<%@page import="org.json.JSONObject"%>
<%@page import="au.gov.aims.atlasmapperserver.ConfigHelper"%>
<%@page import="au.gov.aims.atlasmapperserver.ConfigManager"%>
<%@page import="au.gov.aims.atlasmapperserver.ActionType"%>
<%@page contentType="application/json" pageEncoding="UTF-8"%>
<%
	Logger LOGGER = Logger.getLogger("clientsConfig.jsp");

	ConfigManager configManager = ConfigHelper.getConfigManager(this.getServletContext());

	String actionStr = request.getParameter("action");
	String clientIdStr = request.getParameter("clientId");

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
						JSONArray clientConfigs = configManager.getClientConfigsJSonWithClientUrls(getServletContext());
						if (clientConfigs == null) {
							response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
							jsonObj.put("success", false);
							jsonObj.put("errors", new JSONArray().put("An error occured while loading the configuration. Check your server log."));
						} else {
							response.setStatus(HttpServletResponse.SC_OK);
							jsonObj.put("success", true);
							jsonObj.put("message", "Loaded data");
							jsonObj.put("data", clientConfigs);
						}
					} catch (Exception e) {
						LOGGER.log(Level.SEVERE, "An error occured while retriving the client configuration.", e);
						response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						jsonObj.put("success", false);
						jsonObj.put("errors", new JSONArray().put("An error occured while retriving the client configuration. Check your server log."));
					}
					break;

				case CREATE:
					// Get data from the form, create the config entry, save it, display the result
					try {
						List<ClientConfig> clientConfigs = configManager.createClientConfig(request);
						if (clientConfigs == null || clientConfigs.isEmpty()) {
							response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
							jsonObj.put("success", false);
							jsonObj.put("errors", new JSONArray().put("An error occured while creating the client. Check your server log."));
						} else {
							JSONArray clientJSonArr = new JSONArray();
							for (ClientConfig clientConfig : clientConfigs) {
								JSONObject clientJSon = clientConfig.toJSonObjectWithClientUrls(getServletContext());
								if (clientJSon != null) {
									clientJSonArr.put(clientJSon);
								}
							}
							if (clientJSonArr == null) {
								response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
								jsonObj.put("success", false);
								jsonObj.put("errors", new JSONArray().put("An error occured while generating the client JSON configuration. Check your server log."));
							} else {
								ConfigHelper.save();
								response.setStatus(HttpServletResponse.SC_OK);
								jsonObj.put("success", true);
								jsonObj.put("message", "Created record");
								jsonObj.put("data", clientJSonArr);
							}
						}
					} catch (Exception e) {
						LOGGER.log(Level.SEVERE, "An error occured while creating the client.", e);
						response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						jsonObj.put("success", false);
						jsonObj.put("errors", new JSONArray().put("An error occured while creating the client. Check your server log."));
					}
					break;

				case UPDATE:
					// Get data from the form, update the config entry, save it, display the result
					try {
						configManager.updateClientConfig(request);
						ConfigHelper.save();
						response.setStatus(HttpServletResponse.SC_OK);
						jsonObj.put("success", true);
						jsonObj.put("message", "Updated record");
						jsonObj.put("data", configManager.getClientConfigsJSonWithClientUrls(getServletContext()));
					} catch (Exception e) {
						LOGGER.log(Level.SEVERE, "An error occured while updating the client.", e);
						response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						jsonObj.put("success", false);
						jsonObj.put("errors", new JSONArray().put("An error occured while updating the client. Check your server log."));
					}
					break;

				case DESTROY:
					// Get data from the form, delete the config entry, save it, display the result
					try {
						boolean success = configManager.destroyClientConfig(request);
						ConfigHelper.save();
						if (success) {
							response.setStatus(HttpServletResponse.SC_OK);
							jsonObj.put("success", true);
							jsonObj.put("message", "Deleted record");
							jsonObj.put("data", configManager.getClientConfigsJSonWithClientUrls(getServletContext()));
						} else {
							response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
							jsonObj.put("success", false);
							jsonObj.put("errors", new JSONArray().put("Some files could not be deleted."));
						}
					} catch (Exception e) {
						LOGGER.log(Level.SEVERE, "An error occured while deleting the client.", e);
						response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						jsonObj.put("success", false);
						jsonObj.put("errors", new JSONArray().put("An error occured while deleting the client. Check your server log."));
					}
					break;

				case GETPROJECTIONS:
					try {
						JSONArray projections = Utils.getSupportedProjections();
						if (projections != null) {
							jsonObj.put("success", true);
							jsonObj.put("message", "Received projections");
							jsonObj.put("data", projections);
						} else {
							jsonObj.put("success", false);
							jsonObj.put("errors", new JSONArray().put("There is no supported projections."));
						}
					} catch (Exception e) {
						LOGGER.log(Level.SEVERE, "An error occured while getting the supported projections.", e);
						response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						jsonObj.put("success", false);
						jsonObj.put("errors", new JSONArray().put("An error occured while getting the supported projections. Check your server log."));
					}
					break;

				case GENERATE:
					if (Utils.isBlank(clientIdStr)) {
						response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						jsonObj.put("success", false);
						jsonObj.put("errors", new JSONArray().put("Missing parameter [clientId]."));
					} else {
						Integer clientId = null;
						try {
							clientId = Integer.valueOf(clientIdStr);
						} catch(Exception e) {
							response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
							jsonObj.put("success", false);
							jsonObj.put("errors", new JSONArray().put("Invalid clientid format."));
						}

						if (clientId != null) {
							try {
								ClientConfig foundClientConfig = configManager.getClientConfig(clientId);
								if (foundClientConfig == null) {
									response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
									jsonObj.put("success", false);
									jsonObj.put("errors", new JSONArray().put("Client id ["+clientId+"] not found."));
								} else {
									configManager.generateClient(clientId);
									response.setStatus(HttpServletResponse.SC_OK);
									jsonObj.put("success", true);
									jsonObj.put("message", "Config Generated");
								}
							} catch(Exception e) {
								LOGGER.log(Level.SEVERE, "An error occured while generating the Client configuration.", e);
								response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
								jsonObj.put("success", false);
								jsonObj.put("errors", new JSONArray().put("An error occured while generating the Client configuration. Check your server log."));
							}
						}
					}
					break;

				case GENERATEALL:
					try {
						configManager.generateAllClients();
						response.setStatus(HttpServletResponse.SC_OK);
						jsonObj.put("success", true);
						jsonObj.put("message", "Config saved for all clients");
					} catch (Exception e) {
						LOGGER.log(Level.SEVERE, "An error occured while generating the Client configurations.", e);
						response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						jsonObj.put("success", false);
						jsonObj.put("errors", new JSONArray().put("An error occured while generating the Client configurations. Check your server log."));
					}
					break;

				case DEBUG:
					if (Utils.isBlank(clientIdStr)) {
						response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						jsonObj.put("success", false);
						jsonObj.put("errors", new JSONArray().put("Missing parameter [clientId]."));
					} else {
						Integer clientId = null;
						try {
							clientId = Integer.valueOf(clientIdStr);
						} catch(Exception e) {
							response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
							jsonObj.put("success", false);
							jsonObj.put("errors", new JSONArray().put("Invalid clientid format."));
						}

						if (clientId != null) {
							try {
								ClientConfig foundClientConfig = configManager.getClientConfig(clientId);
								if (foundClientConfig == null) {
									response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
									jsonObj.put("success", false);
									jsonObj.put("errors", new JSONArray().put("Client id ["+clientId+"] not found."));
								} else {
									JSONObject configs = configManager.debugClientConfigJSon(foundClientConfig);
									if (configs == null) {
										response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
										jsonObj.put("success", false);
										jsonObj.put("errors", new JSONArray().put("An error occured while retrieving/generating the Client configurations. Check your server log."));
									} else {
										response.setStatus(HttpServletResponse.SC_OK);
										jsonObj.put("success", true);
										jsonObj.put("message", "Config Generated");
										jsonObj.put("data", configs);
									}
								}
							} catch(Exception e) {
								LOGGER.log(Level.SEVERE, "An error occured while retrieving/generating the Client configurations.", e);
								response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
								jsonObj.put("success", false);
								jsonObj.put("errors", new JSONArray().put("An error occured while retrieving/generating the Client configurations. Check your server log."));
							}
						}
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
