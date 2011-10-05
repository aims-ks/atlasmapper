<%--
    Document   : clientsConfigPage
    Created on : 28/06/2011, 3:59:19 PM
    Author     : glafond
--%>

<%@page import="au.gov.aims.atlasmapperserver.User"%>
<%@page import="au.gov.aims.atlasmapperserver.module.AbstractModule"%>
<%@page import="au.gov.aims.atlasmapperserver.Utils"%>
<%@page import="au.gov.aims.atlasmapperserver.annotation.Module"%>
<%@page import="au.gov.aims.atlasmapperserver.ModuleHelper"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Collections"%>
<%@page import="au.gov.aims.atlasmapperserver.DatasourceConfig"%>
<%@page import="au.gov.aims.atlasmapperserver.ConfigManager"%>
<%@page import="au.gov.aims.atlasmapperserver.ConfigHelper"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
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

		<script type="text/javascript" src="../javascript/extjs_ux/StatusBar.js"></script>
		<script type="text/javascript" src="../javascript/extjs_ux/CheckColumn.js"></script>
		<script type="text/javascript" src="../javascript/extjs_ux/FieldSetResize.js"></script>
		<script type="text/javascript" src="../javascript/extjs_ux/FieldWithQTip.js"></script>
		<script type="text/javascript" src="../javascript/extjs_ux/Checkbox.js"></script>
		<script type="text/javascript" src="../javascript/Frameset.js"></script>
		<!-- Send the client name to the clients config page -->
		<script type="text/javascript">
			var userName = '<%=Utils.safeJsStr(request.getAttribute("loggedUser.name")) %>';
			var datasources = {
				<%
					ConfigManager manager = ConfigHelper.getConfigManager(this.getServletContext());
					List<DatasourceConfig> datasourceConfigs = new ArrayList<DatasourceConfig>(manager.getDatasourceConfigs().values());
					Collections.sort(datasourceConfigs);

					boolean first = true;
					for (DatasourceConfig cfg : datasourceConfigs) {
						if (cfg != null) {
							%><%=(first?"":",\n") + "'" + Utils.safeJsStr(cfg.getDatasourceId()) + "': '" + Utils.safeJsStr(cfg.getDatasourceName()) + "'" %><%
							first = false;
						}
					}
				%>
			}

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
