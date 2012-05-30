<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<!--
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
-->
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

<!-- Generated with AtlasMapper version ${version!} -->
<head>
	<title>${clientName!} layers</title>
	<link rel="icon" type="image/png" href="resources/favicon.png" />
	<meta http-equiv="content-type" content="text/html;charset=utf-8" />

	<link rel="stylesheet" type="text/css" href="resources/css/styles.css" />
	<!--[if lte IE 6 ]>
		<link rel="stylesheet" type="text/css" href="resources/css/styles-ie6.css" />
	<![endif]-->
</head>

<body id="list">
	${listPageHeader!}

	<table>
		<tr>
			<th>Title</th>
			<th>Description</th>
			<th>Image URL</th>
		</tr>
		<#list layers?keys as dataSourceName>
			<tr>
				<td colspan="3"><h2>$ {dataSourceName}</h2></td>
			</tr>

			<#list layers[dataSourceName] as layer>
				<tr>
					<td class="title">${layer["title"]!"Untitled"}<!-- ${layer["id"]!} --></td>
					<td class="desc">${layer["description"]!}</td>
					<td class="preview">
						<#if layer["imageUrl"]??>
							<div style="border: 1px solid #000000; width:${layer["imageWidth"]!200}px; height:${layer["imageHeight"]!200}px; background-image:url('${layer["baseLayerUrl"]!}'); background-repeat: no-repeat;"><a href="${layer["mapUrl"]!}" target="_blank"><img alt="${layer["title"]}" src="${layer["imageUrl"]!}" style="border: none" /></a></div>
						</#if>
					</td>
				</tr>
			</#list>
		</#list>
	</table>

	${listPageFooter!}
</body>

</html>
