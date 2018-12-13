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

	Document   : admin
	Created on : 05/10/2011, 4:13:32 PM
	Author     : glafond
--%>
<%@page import="au.gov.aims.atlasmapperserver.ProjectInfo"%>
<%@page import="java.io.File"%>
<%@page import="au.gov.aims.atlasmapperserver.Utils"%>
<%@page import="au.gov.aims.atlasmapperserver.servlet.FileFinder"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
		<title>AtlasMapper server - Login</title>
		<link rel="icon" type="image/png" href="../resources/favicon.png" />
		<!--<script type="text/javascript" src="../extjs/4.0.2/ext-4.0.2/ext-all.js"></script>-->
		<script type="text/javascript" src="../extjs/4.0.2/ext-4.0.2/ext-all-debug.js"></script>
		<link rel="stylesheet" type="text/css" href="../extjs/4.0.2/ext-4.0.2/resources/css/ext-all.css" />

		<%
		// Verify it the application has access to the configuration folder.
		// If not, a error message, saying that the application can not work
		// properly, is displayed.
		File applicationFolder = FileFinder.getApplicationFolder(this.getServletConfig().getServletContext(), true);
		boolean isWritable = Utils.recursiveIsWritable(applicationFolder);
		if (applicationFolder == null || !isWritable) {
			String dataDirProperty = FileFinder.getDataDirProperty(this.getServletConfig().getServletContext());
			String dataDirPropertyValue = FileFinder.getDataDirPropertyValue(this.getServletConfig().getServletContext());
			boolean isDefined = true;
			if (dataDirPropertyValue == null) {
				dataDirPropertyValue = "UNDEFINED";
				isDefined = false;
			}
			%>
			<script type="text/javascript">
				var dataDirProperty = '<%=Utils.safeJsStr(dataDirProperty) %>';
				var dataDirPropertyValue = '<%=Utils.safeJsStr(dataDirPropertyValue) %>';
				var dataDirIsDefined = <%=isDefined %>;
				var dataDirIsWritable = <%=isWritable %>;
			</script>
			<script type="text/javascript" src="../javascript/noConfigFolder.js"></script>
			<%
		} else {
			%>
			<script type="text/javascript" src="../javascript/login.js"></script>
			<%
		}
		%>
		<link rel="stylesheet" type="text/css" href="../resources/style.css" />
	</head>
	<body>
		<p>version: <%=ProjectInfo.getVersion() %></p>

		<noscript>
			<div style="margin: 1em;">
				<span style="color: #CC0000">ERROR: JavaScript is disabled.</span><br/>
				This application needs JavaScript to function properly.
			</div>
		</noscript>
	</body>
</html>
