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

	Document   : aboutPage
	Created on : 08/11/2011, 5:24:25 PM
	Author     : glafond
--%>

<%@page import="au.gov.aims.atlasmapperserver.servlet.FileFinder"%>
<%@page import="au.gov.aims.atlasmapperserver.Utils"%>
<%@page import="au.gov.aims.atlasmapperserver.ProjectInfo"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
		"http://www.w3.org/TR/html4/loose.dtd">

<html>
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
		<link rel="icon" type="image/png" href="../resources/favicon.png" />
		<title>About</title>
		<script type="text/javascript" src="../extjs/4.0.2/ext-4.0.2/ext-all-debug.js"></script>
		<link rel="stylesheet" type="text/css" href="../extjs/4.0.2/ext-4.0.2/resources/css/ext-all.css" />

		<script type="text/javascript" src="../javascript/extjs_ux/StatusBar.js"></script>
		<script type="text/javascript" src="../javascript/extjs_ux/FieldWithQTip.js"></script>
		<script type="text/javascript" src="../javascript/Frameset.js"></script>
		<script type="text/javascript" src="../javascript/aboutPage.js"></script>

		<link rel="stylesheet" type="text/css" href="../resources/statusbar.css" />
		<link rel="stylesheet" type="text/css" href="../resources/style.css" />

		<script type="text/javascript">
			var userName = '<%=Utils.safeJsStr(request.getAttribute("loggedUser.name")) %>';
			var name = '<%=Utils.safeJsStr(ProjectInfo.getName()) %>';
			var description = '<%=Utils.safeJsStr(ProjectInfo.getDescription()) %>';
			var version = '<%=Utils.safeJsStr(ProjectInfo.getVersion()) %>';
			var url = '<%=Utils.safeJsStr(ProjectInfo.getUrl()) %>';
			var licenseName = '<%=Utils.safeJsStr(ProjectInfo.getLicenseName()) %>';
			var licenseUrl = '<%=Utils.safeJsStr(ProjectInfo.getLicenseUrl()) %>';
			var dataDirProperty = '<%=Utils.safeJsStr(FileFinder.DATA_DIR_PROPERTY) %>';
			var dataDirPropertyValue = '<%=Utils.safeJsStr(FileFinder.getDataDirPropertyValue(this.getServletConfig().getServletContext())) %>';
		</script>
	</head>
	<body id="about">
	</body>
</html>
