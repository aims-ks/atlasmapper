<%-- 
    Document   : userAccount
    Created on : 08/08/2011, 9:38:36 AM
    Author     : glafond
--%>

<%@page import="au.gov.aims.atlasmapperserver.Utils"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<html>
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
		<link rel="icon" type="image/png" href="../resources/favicon.png" />
		<title>User account</title>
		<script type="text/javascript" src="../extjs/4.0.2/ext-4.0.2/ext-all-debug.js"></script>
		<link rel="stylesheet" type="text/css" href="../extjs/4.0.2/ext-4.0.2/resources/css/ext-all.css" />

		<script type="text/javascript" src="../javascript/extjs_ux/StatusBar.js"></script>
		<script type="text/javascript" src="../javascript/extjs_ux/FieldWithQTip.js"></script>
		<script type="text/javascript" src="../javascript/Frameset.js"></script>
		<script type="text/javascript" src="../javascript/userAccountPage.js"></script>

		<link rel="stylesheet" type="text/css" href="../resources/statusbar.css" />
		<link rel="stylesheet" type="text/css" href="../resources/style.css" />

		<script type="text/javascript">
			var userName = '<%=Utils.safeJsStr(request.getAttribute("loggedUser.name")) %>';
			var userLogin = '<%=Utils.safeJsStr(request.getAttribute("loggedUser.login")) %>';
		</script>
	</head>

	<body>
	</body>
</html>
