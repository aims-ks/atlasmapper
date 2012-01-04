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
--%>

<%@page import="au.gov.aims.atlasmapperserver.GetCapabilitiesExceptions"%>
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
	String clientId = request.getParameter("clientId");
	String idStr = request.getParameter("id");
	String completeStr = request.getParameter("complete");

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

				case VALIDATEID:
					try {
						Integer id = null;
						if (idStr != null && idStr.length() > 0) {
							id = Integer.parseInt(idStr);
						}
						boolean exists = configManager.clientExists(clientId, id);
						if (!exists) {
							// The client do not exists (or, in case of an update, it represent the same client), the client ID is valid
							response.setStatus(HttpServletResponse.SC_OK);
							jsonObj.put("success", true);
							jsonObj.put("message", "The client ID is valid");
						} else {
							response.setStatus(HttpServletResponse.SC_OK);
							jsonObj.put("success", false);
							jsonObj.put("errors", new JSONArray().put("The client ID '"+clientId+"' is already in used."));
						}
					} catch (Exception e) {
						LOGGER.log(Level.SEVERE, "An error occurred while validating the client ID.", e);
						response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						jsonObj.put("success", false);
						jsonObj.put("errors", new JSONArray().put("An error occurred while validating the client ID. Check your server log."));
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
					if (Utils.isBlank(idStr)) {
						response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						jsonObj.put("success", false);
						jsonObj.put("errors", new JSONArray().put("Missing parameter [clientId]."));
					} else {
						boolean complete = false;
						Integer id = null;
						try {
							complete = Boolean.parseBoolean(completeStr);
							id = Integer.valueOf(idStr);
						} catch(Exception e) {
							response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
							jsonObj.put("success", false);
							jsonObj.put("errors", new JSONArray().put("Invalid clientid format."));
						}

						if (id != null) {
							try {
								ClientConfig foundClientConfig = configManager.getClientConfig(id);
								if (foundClientConfig == null) {
									response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
									jsonObj.put("success", false);
									jsonObj.put("errors", new JSONArray().put("Client number ["+id+"] not found."));
								} else {
									configManager.generateClient(foundClientConfig, complete);
									response.setStatus(HttpServletResponse.SC_OK);
									jsonObj.put("success", true);
									jsonObj.put("message", "Config Generated");
								}
							} catch(GetCapabilitiesExceptions e) {
								LOGGER.log(Level.SEVERE, "Can not retrieved all Capabilities Documents. Generation aborded.", e);
								response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
								jsonObj.put("success", false);
								jsonObj.put("errors", new JSONArray().put("Can not retrieved all Capabilities Documents. Generation aborded.<br/>\n" + e.getMessage().replace("\n", "<br/>\n")));
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
						boolean complete = Boolean.parseBoolean(completeStr);

						configManager.generateAllClients(complete);
						response.setStatus(HttpServletResponse.SC_OK);
						jsonObj.put("success", true);
						jsonObj.put("message", "Config saved for all clients");
					} catch(GetCapabilitiesExceptions e) {
						LOGGER.log(Level.SEVERE, "Can not retrieved all Capabilities Documents. Generation aborded.", e);
						response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						jsonObj.put("success", false);
						jsonObj.put("errors", new JSONArray().put("Can not retrieved all Capabilities Documents. Generation aborded.<br/>\n" + e.getMessage().replace("\n", "<br/>\n")));
					} catch (Exception e) {
						LOGGER.log(Level.SEVERE, "An error occured while generating the Client configurations.", e);
						response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						jsonObj.put("success", false);
						jsonObj.put("errors", new JSONArray().put("An error occured while generating the Client configurations. Check your server log."));
					}
					break;

				case DEBUG:
					if (Utils.isBlank(idStr)) {
						response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						jsonObj.put("success", false);
						jsonObj.put("errors", new JSONArray().put("Missing parameter [clientId]."));
					} else {
						Integer id = null;
						try {
							id = Integer.valueOf(idStr);
						} catch(Exception e) {
							response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
							jsonObj.put("success", false);
							jsonObj.put("errors", new JSONArray().put("Invalid clientid format."));
						}

						if (id != null) {
							try {
								ClientConfig foundClientConfig = configManager.getClientConfig(id);
								if (foundClientConfig == null) {
									response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
									jsonObj.put("success", false);
									jsonObj.put("errors", new JSONArray().put("Client number ["+id+"] not found."));
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
							} catch(GetCapabilitiesExceptions e) {
								LOGGER.log(Level.SEVERE, "Can not retrieved all Capabilities Documents. Generation aborded.", e);
								response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
								jsonObj.put("success", false);
								jsonObj.put("errors", new JSONArray().put("Can not retrieved all Capabilities Documents. Generation aborded.<br/>\n" + e.getMessage().replace("\n", "<br/>\n")));
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
