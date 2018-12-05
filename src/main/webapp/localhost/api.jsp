<%--
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2013 Australian Institute of Marine Science
 *
 *  Contact: Gael Lafond <g.lafond@aims.gov.au>
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

    Document   : api
    Created on : 20/05/2013, 11:00:39 AM
    Author     : glafond

 * This API can be used to execute task on a periodical basis (from the crom for example).
 * NOTE: It can only be called from localhost, to avoid obvious DOS attack.
 *
 * REFRESH
 *     http://localhost:8080/atlasmapper/localhost/api.jsp?action=REFRESH&dataSourceIds=<COMA_SEPARATED_LIST_OF_DATA_SOURCE_ID>&clientIds=<COMA_SEPARATED_LIST_OF_CLIENT_ID>
 *
 *     NOTES:
 *         * Despite the order of the URL parameters, the data sources are always refreshed before the clients.
 *         * Requests to this service may takes some time to response. It's recommended to set a very long timeout
 *             with the client used to do the request.
 *             Examples (1 hour timeout):
 *                 curl --max-time 3600 "http://localhost:8080/atlasmapper/localhost/api.jsp?action=REFRESH&..."
 *                 wget --timeout=3600 "http://localhost:8080/atlasmapper/localhost/api.jsp?action=REFRESH&..."
 *
 *     Examples:
 *         To refresh the cache for the data sources ID "ea" and "imos":
 *             http://localhost:8080/atlasmapper/localhost/api.jsp?action=REFRESH&dataSourceIds=ea,imos
 *
 *         To regenerate the clients ID "demo" and "maps":
 *             http://localhost:8080/atlasmapper/localhost/api.jsp?action=REFRESH&clientIds=demo,maps
 *
 *         To refresh the cache for the data source "ea" and regenerate the client "demo":
 *             http://localhost:8080/atlasmapper/localhost/api.jsp?action=REFRESH&dataSourceIds=ea&clientIds=demo

NOTE: The strange arrangement of the import is to avoid unnecessary empty lines at the top of the generated file.
--%><%@
page import="au.gov.aims.atlasmapperserver.Utils" %><%@
page import="au.gov.aims.atlasmapperserver.APIActionType"%><%@
page import="au.gov.aims.atlasmapperserver.dataSourceConfig.AbstractDataSourceConfig" %><%@
page import="au.gov.aims.atlasmapperserver.ConfigManager" %><%@
page import="au.gov.aims.atlasmapperserver.ConfigHelper" %><%@
page import="au.gov.aims.atlasmapperserver.ClientConfig" %><%@
page import="org.json.JSONObject" %><%@
page import="au.gov.aims.atlasmapperserver.thread.AbstractConfigThread"%><%@
page import="au.gov.aims.atlasmapperserver.thread.ThreadLogger"%><%@
page import="java.util.logging.Level"%><%@
page contentType="application/json" pageEncoding="UTF-8"%><%

    ConfigManager configManager = ConfigHelper.getConfigManager(this.getServletConfig().getServletContext());

    String actionStr = request.getParameter("action");
    String clientIds = request.getParameter("clientIds");
    String dataSourceIds = request.getParameter("dataSourceIds");

    JSONObject jsonObj = new JSONObject();

    ThreadLogger logger = new ThreadLogger();
    if (Utils.isNotBlank(actionStr)) {
        APIActionType action = null;
        try {
            action = APIActionType.valueOf(actionStr.toUpperCase());
        } catch (Exception ex) {}

        if (action == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            logger.log(Level.SEVERE, "Unknown action [" + actionStr + "].");
            jsonObj.put("logs", logger.toJSON());
            jsonObj.put("success", false);
        } else {
            switch(action) {
                case REFRESH:
                    // Refresh data sources cache first
                    if (dataSourceIds != null && !dataSourceIds.isEmpty()) {
                        String[] dataSourceIdsArray = dataSourceIds.split(",");
                        for (String dataSourceId : dataSourceIdsArray) {
                            AbstractDataSourceConfig dataSource = configManager.getDataSourceConfig(dataSourceId);
                            if (dataSource == null) {
                                // Invalid data source ID
                                logger.log(Level.SEVERE, "Invalid data source ID: ["+dataSourceId+"]");
                            } else {
                                // Refresh cache and merging error messages
                                dataSource.process(
                                        true, // redownloadBrokenFiles
                                        true, // clearCapabilitiesCache
                                        false // clearMetadataCache
                                );
                                AbstractConfigThread thread = dataSource.getThread();
                                thread.join();
                                logger.addAll(thread.getLogger());
                            }
                        }
                    }

                    // Refresh (regenerate) clients
                    if (clientIds != null && !clientIds.isEmpty()) {
                        String[] clientIdsArray = clientIds.split(",");
                        for (String clientId : clientIdsArray) {
                            ClientConfig client = configManager.getClientConfig(clientId);
                            if (client == null) {
                                // Invalid client ID
                                logger.log(Level.SEVERE, "Invalid client ID: ["+clientId+"]");
                            } else {
                                // Regenerate client and merging error messages
                                client.process(false);
                                AbstractConfigThread thread = client.getThread();
                                thread.join();
                                logger.addAll(thread.getLogger());
                            }
                        }
                    }

                    response.setStatus(HttpServletResponse.SC_OK);
                    jsonObj.put("message", "Config Generated");
                    jsonObj.put("logs", logger.toJSON());
                    jsonObj.put("success", logger.getErrorCount() == 0);

                    break;

                default:
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    logger.log(Level.SEVERE, "Unknown action [" + actionStr + "].");
                    jsonObj.put("logs", logger.toJSON());
                    jsonObj.put("success", false);
                    break;
            }
        }
    } else {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        logger.log(Level.SEVERE, "Missing parameter [action].");
        jsonObj.put("logs", logger.toJSON());
        jsonObj.put("success", false);
    }

%><%=jsonObj.toString(4) %>
