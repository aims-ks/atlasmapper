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

	Document   : dataSourcesConfig
	Created on : 28/06/2011, 4:08:39 PM
	Author     : glafond
--%>

<%@page import="au.gov.aims.atlasmapperserver.Utils"%>
<%@page import="java.util.List"%>
<%@page import="java.util.logging.Level"%>
<%@page import="java.util.logging.Logger"%>
<%@page import="org.json.JSONArray"%>
<%@page import="au.gov.aims.atlasmapperserver.dataSourceConfig.AbstractDataSourceConfig"%>
<%@page import="au.gov.aims.atlasmapperserver.ConfigHelper"%>
<%@page import="org.json.JSONObject"%>
<%@page import="au.gov.aims.atlasmapperserver.ConfigManager"%>
<%@page import="au.gov.aims.atlasmapperserver.ActionType"%>
<%@page import="au.gov.aims.atlasmapperserver.URLCache" %>
<%@page import="java.util.Map"%>
<%@page import="au.gov.aims.atlasmapperserver.Errors"%>
<%@page contentType="application/json" pageEncoding="UTF-8"%>
<%
	Logger LOGGER = Logger.getLogger("dataSourcesConfig.jsp");

	ConfigManager configManager = ConfigHelper.getConfigManager(this.getServletConfig().getServletContext());

	String actionStr = request.getParameter("action");
	String dataSourceId = request.getParameter("dataSourceId");
	String idStr = request.getParameter("id");
	String redownloadBrokenFilesStr = request.getParameter("redownloadBrokenFiles");
	String clearCapabilitiesCacheStr = request.getParameter("clearCapCache");
	String clearMetadataCacheStr = request.getParameter("clearMestCache");

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
						jsonObj.put("data", configManager.getDataSourceConfigsJSon());
					} catch (Exception e) {
						LOGGER.log(Level.SEVERE, "An error occurred while retrieving the data source configuration: {0}", Utils.getExceptionMessage(e));
						LOGGER.log(Level.WARNING, "Stack trace: ", e);
						response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						jsonObj.put("success", false);
						jsonObj.put("errors", new JSONArray().put("An error occurred while retrieving the data source configuration: " + Utils.getExceptionMessage(e) + "\nCheck your server log."));
					}
					break;

				case CREATE:
					// Get data from the form, create the config entry, save it, display the result
					try {
						List<AbstractDataSourceConfig> dataSourceConfigs = configManager.createDataSourceConfig(request);
						JSONArray dataSourceJSonArr = new JSONArray();
						if (dataSourceConfigs != null) {
							for (AbstractDataSourceConfig dataSourceConfig : dataSourceConfigs) {
								JSONObject dataSourceJSon = dataSourceConfig.toJSonObject();
								if (dataSourceJSon != null) {
									dataSourceJSonArr.put(dataSourceJSon);
								}
							}
						}
						ConfigHelper.save();
						response.setStatus(HttpServletResponse.SC_OK);
						jsonObj.put("success", true);
						jsonObj.put("message", "Created record");
						jsonObj.put("data", dataSourceJSonArr);
					} catch (Exception e) {
						LOGGER.log(Level.SEVERE, "An error occurred while creating a new data source: {0}", Utils.getExceptionMessage(e));
						LOGGER.log(Level.WARNING, "Stack trace: ", e);
						response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						jsonObj.put("success", false);
						jsonObj.put("errors", new JSONArray().put("An error occurred while creating a new data source: " + Utils.getExceptionMessage(e) + "\nCheck your server log."));
					}
					break;

				case UPDATE:
					// Get data from the form, update the config entry, save it, display the result
					try {
						configManager.updateDataSourceConfig(request);
						ConfigHelper.save();
						response.setStatus(HttpServletResponse.SC_OK);
						jsonObj.put("success", true);
						jsonObj.put("message", "Updated record");
						jsonObj.put("data", configManager.getDataSourceConfigsJSon());
					} catch (Exception e) {
						LOGGER.log(Level.SEVERE, "An error occurred while updating the data source: {0}", Utils.getExceptionMessage(e));
						LOGGER.log(Level.WARNING, "Stack trace: ", e);
						response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						jsonObj.put("success", false);
						jsonObj.put("errors", new JSONArray().put("An error occurred while updating the data source: " + Utils.getExceptionMessage(e) + "\nCheck your server log."));
					}
					break;

				case DESTROY:
					// Get data from the form, delete the config entry, save it, display the result
					try {
						configManager.destroyDataSourceConfig(request);
						ConfigHelper.save();
						response.setStatus(HttpServletResponse.SC_OK);
						jsonObj.put("success", true);
						jsonObj.put("message", "Deleted record");
						jsonObj.put("data", configManager.getDataSourceConfigsJSon());
					} catch (Exception e) {
						LOGGER.log(Level.SEVERE, "An error occurred while deleting the data source: {0}", Utils.getExceptionMessage(e));
						LOGGER.log(Level.WARNING, "Stack trace: ", e);
						response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						jsonObj.put("success", false);
						jsonObj.put("errors", new JSONArray().put("An error occurred while deleting the data source: " + Utils.getExceptionMessage(e) + "\nCheck your server log."));
					}
					break;

				case VALIDATEID:
					try {
						Integer id = null;
						if (idStr != null && idStr.length() > 0) {
							id = Integer.parseInt(idStr);
						}
						boolean exists = configManager.dataSourceExists(dataSourceId, id);
						if (!exists) {
							// The data source do not exists (or, in case of an update, it represent the same data source), the data source ID is valid
							response.setStatus(HttpServletResponse.SC_OK);
							jsonObj.put("success", true);
							jsonObj.put("message", "The data source ID is valid");
						} else {
							response.setStatus(HttpServletResponse.SC_OK);
							jsonObj.put("success", false);
							jsonObj.put("errors", new JSONArray().put("The data source ID '"+dataSourceId+"' is already in used."));
						}
					} catch (Exception e) {
						LOGGER.log(Level.SEVERE, "An error occurred while validating the data source ID: {0}", Utils.getExceptionMessage(e));
						LOGGER.log(Level.WARNING, "Stack trace: ", e);
						response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						jsonObj.put("success", false);
						jsonObj.put("errors", new JSONArray().put("An error occurred while validating the data source ID: " + Utils.getExceptionMessage(e) + "\nCheck your server log."));
					}
					break;

				case PROCESS:
					try {
						if (Utils.isBlank(idStr)) {
							response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
							jsonObj.put("success", false);
							jsonObj.put("errors", new JSONArray().put("Missing parameter [id]."));
						} else {
							Integer id = null;
							boolean redownloadBrokenFiles = false;
							boolean clearCapabilitiesCache = false;
							boolean clearMetadataCache = false;

							try {
								id = Integer.valueOf(idStr);
							} catch(Exception e) {
								response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
								jsonObj.put("success", false);
								jsonObj.put("errors", new JSONArray().put("Invalid id format. Expected integer."));
							}
							if (redownloadBrokenFilesStr != null) {
								try {
									redownloadBrokenFiles = Boolean.parseBoolean(redownloadBrokenFilesStr);
								} catch(Exception e) {
									response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
									jsonObj.put("success", false);
									jsonObj.put("errors", new JSONArray().put("Invalid redownloadBrokenFiles format. Expected boolean."));
								}
							}
							if (clearCapabilitiesCacheStr != null) {
								try {
									clearCapabilitiesCache = Boolean.parseBoolean(clearCapabilitiesCacheStr);
								} catch(Exception e) {
									response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
									jsonObj.put("success", false);
									jsonObj.put("errors", new JSONArray().put("Invalid clearCapCache format. Expected boolean."));
								}
							}
							if (clearMetadataCacheStr != null) {
								try {
									clearMetadataCache = Boolean.parseBoolean(clearMetadataCacheStr);
								} catch(Exception e) {
									response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
									jsonObj.put("success", false);
									jsonObj.put("errors", new JSONArray().put("Invalid clearMestCache format. Expected boolean."));
								}
							}

							if (id == null) {
								response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
								jsonObj.put("success", false);
								jsonObj.put("errors", new JSONArray().put("Invalid id."));
							} else {
								AbstractDataSourceConfig foundDataSourceConfig = configManager.getDataSourceConfig(id);
								JSONObject jsonErrors = foundDataSourceConfig.process(redownloadBrokenFiles, clearCapabilitiesCache, clearMetadataCache);
								response.setStatus(HttpServletResponse.SC_OK);
								jsonObj.put("message", "Data source rebuilded");
								if (jsonErrors != null) {
									jsonObj.put("errors", jsonErrors.opt("errors"));
									jsonObj.put("warnings", jsonErrors.opt("warnings"));
									jsonObj.put("messages", jsonErrors.opt("messages"));
								}
								jsonObj.put("success", !jsonObj.has("errors"));
							}
						}
					} catch (Exception e) {
						LOGGER.log(Level.SEVERE, "An error occurred while rebuilding the data source: {0}", Utils.getExceptionMessage(e));
						LOGGER.log(Level.WARNING, "Stack trace: ", e);
						response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						jsonObj.put("success", false);
						jsonObj.put("errors", new JSONArray().put("An error occurred while rebuilding the data source: " + Utils.getExceptionMessage(e) + "\nCheck your server log."));
					}
					break;

				case PROCESSALL:
					try {
						boolean redownloadBrokenFiles = false;
						boolean clearCapabilitiesCache = false;
						boolean clearMetadataCache = false;
						if (redownloadBrokenFilesStr != null) {
							try {
								redownloadBrokenFiles = Boolean.parseBoolean(redownloadBrokenFilesStr);
							} catch(Exception e) {
								response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
								jsonObj.put("success", false);
								jsonObj.put("errors", new JSONArray().put("Invalid redownloadBrokenFiles format. Expected boolean."));
							}
						}
						if (clearCapabilitiesCacheStr != null) {
							try {
								clearCapabilitiesCache = Boolean.parseBoolean(clearCapabilitiesCacheStr);
							} catch(Exception e) {
								response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
								jsonObj.put("success", false);
								jsonObj.put("errors", new JSONArray().put("Invalid clearCapCache format. Expected boolean."));
							}
						}
						if (clearMetadataCacheStr != null) {
							try {
								clearMetadataCache = Boolean.parseBoolean(clearMetadataCacheStr);
							} catch(Exception e) {
								response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
								jsonObj.put("success", false);
								jsonObj.put("errors", new JSONArray().put("Invalid clearMestCache format. Expected boolean."));
							}
						}

						// TODO
						JSONObject jsonErrors = AbstractDataSourceConfig.processAll(configManager, redownloadBrokenFiles, clearCapabilitiesCache, clearMetadataCache);
						response.setStatus(HttpServletResponse.SC_OK);
						jsonObj.put("message", "Data source rebuilded");
						if (jsonErrors != null) {
							jsonObj.put("errors", jsonErrors.opt("errors"));
							jsonObj.put("warnings", jsonErrors.opt("warnings"));
							jsonObj.put("messages", jsonErrors.opt("messages"));
						}
						jsonObj.put("success", !jsonObj.has("errors"));
					} catch (Exception e) {
						LOGGER.log(Level.SEVERE, "An error occurred while rebuilding a data source: {0}", Utils.getExceptionMessage(e));
						LOGGER.log(Level.WARNING, "Stack trace: ", e);
						response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						jsonObj.put("success", false);
						jsonObj.put("errors", new JSONArray().put("An error occurred while rebuilding a data source: " + Utils.getExceptionMessage(e) + "\nCheck your server log."));
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
