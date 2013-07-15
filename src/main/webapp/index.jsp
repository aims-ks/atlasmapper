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

	Document   : index
	Created on : 05/10/2011, 4:13:32 PM
	Author     : glafond
--%>
<%@page import="au.gov.aims.atlasmapperserver.servlet.FileFinder"%>
<%@page import="au.gov.aims.atlasmapperserver.ClientConfig"%>
<%@page import="au.gov.aims.atlasmapperserver.ConfigHelper"%>
<%@page import="au.gov.aims.atlasmapperserver.ConfigManager"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
		"http://www.w3.org/TR/html4/loose.dtd">

<html>
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
		<title>AtlasMapper</title>
		<link rel="icon" type="image/png" href="resources/favicon.png" />
		<%
		String urlString = "public/admin.jsp";

		ServletContext context = this.getServletConfig().getServletContext();
		ConfigManager configManager = ConfigHelper.getConfigManager(context);
		ClientConfig defaultClient = configManager.getDefaultClientConfig();
		if (defaultClient != null) {
			String clientUrl = FileFinder.getAtlasMapperClientURL(context, defaultClient);
			if (clientUrl != null) {
				urlString = clientUrl;
			}
		}
		%>
		<meta http-equiv="refresh" content="0;url=<%=urlString %>" />
	</head>
	<body>
	</body>
</html>
