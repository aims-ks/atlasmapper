<%--
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2011 Australian Institute of Marine Science
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

	Document   : dataSourcesConfigPage
	Created on : 28/06/2011, 3:57:23 PM
	Author     : glafond
--%>

<%@ page import="au.gov.aims.atlasmapperserver.Utils"%>
<%@ page import="au.gov.aims.atlasmapperserver.ConfigManager"%>
<%@ page import="au.gov.aims.atlasmapperserver.ConfigHelper"%>

<%@ page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
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
		<script type="text/javascript" src="../javascript/extjs_ux/FieldSetResize.js"></script>
		<script type="text/javascript" src="../javascript/extjs_ux/FieldWithQTip.js"></script>
		<script type="text/javascript" src="../javascript/extjs_ux/Checkbox.js"></script>
		<script type="text/javascript" src="../javascript/Frameset.js"></script>

		<script type="text/javascript">
			<% ConfigManager manager = ConfigHelper.getConfigManager(this.getServletConfig().getServletContext()); %>
			var demoMode = <%=manager.isDemoMode() %>;
		</script>
		<script type="text/javascript" src="../javascript/dataSourcesConfigPage.js"></script>

		<link rel="stylesheet" type="text/css" href="../resources/statusbar.css" />
		<link rel="stylesheet" type="text/css" href="../resources/style.css" />

		<!-- AreaEdit: http://www.cdolivet.com/editarea/?page=editArea -->
		<script type="text/javascript" src="../edit_area/edit_area_full.js"></script>
		<script type="text/javascript" src="../javascript/extjs_ux/EditArea.js"></script>

		<script type="text/javascript">
			var userName = '<%=Utils.safeJsStr(request.getAttribute("loggedUser.name")) %>';
		</script>
	</head>

	<body>
	</body>
</html>
