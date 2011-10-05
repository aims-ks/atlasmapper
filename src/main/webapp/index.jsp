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

		ServletContext context = getServletContext();
		ConfigManager configManager = ConfigHelper.getConfigManager(context);
		ClientConfig defaultClient = configManager.getDefaultClientConfig();
		if (defaultClient != null) {
			String clientUrl = FileFinder.getAtlasMapperClientURL(context, defaultClient, false);
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
