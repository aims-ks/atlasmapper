<%--
    Document   : datasourcesConfigPage
    Created on : 28/06/2011, 3:57:23 PM
    Author     : glafond
--%>

<%@page import="au.gov.aims.atlasmapperserver.Utils"%>
<%@page import="au.gov.aims.atlasmapperserver.User"%>
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
		<script type="text/javascript" src="../javascript/extjs_ux/FieldSetResize.js"></script>
		<script type="text/javascript" src="../javascript/extjs_ux/FieldWithQTip.js"></script>
		<script type="text/javascript" src="../javascript/extjs_ux/Checkbox.js"></script>
		<script type="text/javascript" src="../javascript/Frameset.js"></script>
		<script type="text/javascript" src="../javascript/datasourcesConfigPage.js"></script>

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
