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

	Document   : clientsConfigPage
	Created on : 28/06/2011, 3:59:19 PM
	Author     : glafond
--%>

<%@ page import="au.gov.aims.atlasmapperserver.module.AbstractModule"%>
<%@ page import="au.gov.aims.atlasmapperserver.Utils"%>
<%@ page import="au.gov.aims.atlasmapperserver.annotation.Module"%>
<%@ page import="au.gov.aims.atlasmapperserver.ModuleHelper"%>
<%@ page import="java.util.ArrayList"%>
<%@ page import="java.util.List"%>
<%@ page import="java.util.Collections"%>
<%@ page import="au.gov.aims.atlasmapperserver.dataSourceConfig.AbstractDataSourceConfig"%>
<%@ page import="au.gov.aims.atlasmapperserver.ConfigManager"%>
<%@ page import="au.gov.aims.atlasmapperserver.ConfigHelper"%>
<%@ page import="java.io.File" %>
<%@ page import="au.gov.aims.atlasmapperserver.servlet.FileFinder" %>
<%@ page import="au.gov.aims.atlasmapperserver.jsonWrappers.client.DataSourceWrapper" %>

<%@ page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
		"http://www.w3.org/TR/html4/loose.dtd">

<html>
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
		<link rel="icon" type="image/png" href="../resources/favicon.png" />
		<title>Main config</title>
		<!--<script type="text/javascript" src="extjs/4.0.2/ext-4.0.2/ext-all.js"></script>-->
		<script type="text/javascript" src="../extjs/4.0.2/ext-4.0.2/ext-all-debug.js"></script>
		<link rel="stylesheet" type="text/css" href="../extjs/4.0.2/ext-4.0.2/resources/css/ext-all.css" />

		<script type="text/javascript" src="../javascript/extjs_ux/AjaxTextField.js"></script>
		<script type="text/javascript" src="../javascript/extjs_ux/StatusBar.js"></script>
		<script type="text/javascript" src="../javascript/extjs_ux/CheckColumn.js"></script>
		<script type="text/javascript" src="../javascript/extjs_ux/FieldSetResize.js"></script>
		<script type="text/javascript" src="../javascript/extjs_ux/FieldWithQTip.js"></script>
		<script type="text/javascript" src="../javascript/extjs_ux/Checkbox.js"></script>
		<script type="text/javascript" src="../javascript/Frameset.js"></script>
		<!-- Send the client name to the clients config page -->
		<script type="text/javascript">
			<% ConfigManager manager = ConfigHelper.getConfigManager(this.getServletConfig().getServletContext()); %>

			var userName = '<%=Utils.safeJsStr(request.getAttribute("loggedUser.name")) %>';
			var demoMode = <%=manager.isDemoMode() %>;
			var dataSources = {
				<%
					// Data sources are also sorted on client side, but it save time to pre-sort them on server side.
					List<AbstractDataSourceConfig> dataSourceConfigs = new ArrayList<AbstractDataSourceConfig>(manager.getDataSourceConfigs().values());
					Collections.sort(dataSourceConfigs);

					boolean first = true;
					for (AbstractDataSourceConfig cfg : dataSourceConfigs) {
						if (cfg != null) {

							String status = "INVALID";
							File applicationFolder = manager.getApplicationFolder();
							File dataSourceCatalogFile = FileFinder.getDataSourcesCatalogFile(applicationFolder, cfg.getDataSourceId());
							if (dataSourceCatalogFile.exists()) {
								DataSourceWrapper dataSourceSavedState = AbstractDataSourceConfig.load(dataSourceCatalogFile);

								if (dataSourceSavedState != null) {
									status = dataSourceSavedState.getStatus();
								}
							}

							%><%=(first?"":",\n") +
									"'" + Utils.safeJsStr(cfg.getDataSourceId()) + "': {" +
										"name: '" + Utils.safeJsStr(cfg.getDataSourceName()) + "'," +
										"type: '" + Utils.safeJsStr(cfg.getLayerType()) + "'," +
										"status: '" + status + "'" +
									"}"
							%><%
							first = false;
						}
					}
				%>
			};

			var modules = {
				<%
					List<AbstractModule> modules = ModuleHelper.getSortedModules();

					first = true;
					for (AbstractModule module : modules) {
						if (module != null) {
							Class moduleClass = module.getClass();
							String name = moduleClass.getSimpleName(), title = name, description = null;
							Module annotation = (Module)moduleClass.getAnnotation(Module.class);
							if (annotation != null) {
								name = Utils.isNotBlank(annotation.name()) ? annotation.name() : name;
								title = Utils.isNotBlank(annotation.title()) ? annotation.title() : title;
								description = Utils.isNotBlank(annotation.description()) ? annotation.description() : description;
							}
							if (description == null) {
								%><%=(first?"":",\n") + "'" + Utils.safeJsStr(name) + "': {boxLabel: '" + Utils.safeJsStr(title) + "'}" %><%
							} else {
								%><%=(first?"":",\n") + "'" + Utils.safeJsStr(name) + "': {boxLabel: '" + Utils.safeJsStr(title) + "', qtipHtml: '" + Utils.safeJsStr(description) + "'}" %><%
							}
							first = false;
						}
					}
				%>
			};
		</script>
		<script type="text/javascript" src="../javascript/clientsConfigPage.js"></script>

		<link rel="stylesheet" type="text/css" href="../resources/statusbar.css" />
		<link rel="stylesheet" type="text/css" href="../resources/checkheader.css" />
		<link rel="stylesheet" type="text/css" href="../resources/style.css" />

		<!-- AreaEdit: http://www.cdolivet.com/editarea/?page=editArea -->
		<script type="text/javascript" src="../edit_area/edit_area_full.js"></script>
		<script type="text/javascript" src="../javascript/extjs_ux/EditArea.js"></script>
	</head>

	<body>
	</body>
</html>
