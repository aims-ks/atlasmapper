<%@page import="au.gov.aims.atlasmapperserver.ProjectInfo"%>
<%@page import="java.io.File"%>
<%@page import="au.gov.aims.atlasmapperserver.Utils"%>
<%@page import="au.gov.aims.atlasmapperserver.servlet.FileFinder"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
		"http://www.w3.org/TR/html4/loose.dtd">

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
		File applicationFolder = FileFinder.getApplicationFolder(this.getServletContext(), true);
		boolean isWritable = Utils.recursiveIsWritable(applicationFolder);
		if (applicationFolder == null || !isWritable) {
			String dataDirProperty = FileFinder.DATA_DIR_PROPERTY;
			String dataDirPropertyValue = FileFinder.getDataDirPropertyValue(this.getServletContext());
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
	</body>
</html>
