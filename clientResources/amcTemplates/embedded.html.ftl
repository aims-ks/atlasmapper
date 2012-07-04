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

<!-- Generated with AtlasMapper version ${version} -->
<head>
	<meta http-equiv="X-UA-Compatible" content="IE=EmulateIE8" />
	<!-- IE9 is not support by GeoExt yet, the emulation mode is supposed to fix this...
		IMPORTANT!!! The IE-EmulateIE8 MUST be the first line of the header otherwise IE9 ignore it. -->

	<title>${clientName}</title>
	<link rel="icon" type="image/png" href="resources/favicon.png" />
	<meta http-equiv="content-type" content="text/html;charset=utf-8" />


	<link rel="stylesheet" type="text/css" href="resources/css/styles.css" />
	<!--[if lte IE 6 ]>
	<link rel="stylesheet" type="text/css" href="resources/css/styles-ie6.css" />
	<![endif]-->
<#if (theme?? && theme != "")>
	<link rel="stylesheet" type="text/css" href="extjs/3.3.0/ext-3.3.0/resources/css/ext-all-notheme.css" />
	<link rel="stylesheet" type="text/css" href="extjs/3.3.0/ext-3.3.0/resources/css/${theme}.css" />
<#else>
	<link rel="stylesheet" type="text/css" href="extjs/3.3.0/ext-3.3.0/resources/css/ext-all.css" />
</#if>
</head>

<body id="embeddedClient">
	<div id="loading"></div>

	<div id="goToMap"></div>

	<noscript>
		<p>
			You need to have JavaScript enabled to use the Map.
		</p>
	</noscript>
	<script type="text/javascript">
		var loadingObj = document.getElementById('loading');
		loadingObj.style.display = 'block';
	</script>

	<!-- IE 9+ conditional comment - this will only be executed by IE 9 and above. -->
	<!--[if gte IE 9]>
	<script type="text/javascript">
		var ie9plus = true;
	</script>
	<![endif]-->

	<script type="text/javascript" src="OpenLayers/OpenLayers-2.11/OpenLayers.js"></script>
	<script type="text/javascript" src="OpenLayers-ux/NCWMS.js"></script>
	<script type="text/javascript" src="OpenLayers-ux/NCTimeSeriesClickControl.js"></script>
	<script type="text/javascript" src="OpenLayers-ux/NCTransectDrawControl.js"></script>

	<!-- OpenLayers support for Google layer, in version <= 2.11, has to be patched to support V > 3.6
			   (since google do not support V <= 3.6 anymore)
		   Bug: http://trac.osgeo.org/openlayers/ticket/3614
		   Patch: https://github.com/openlayers/openlayers/commit/92f04a7a4277a6c818ef2d40a2856910ed72d3d6
		   Date: 18-05-2012
	   -->
	<script type="text/javascript" src="OpenLayers-ux/Google-v3.js"></script>

	<#if (useGoogle)>
	<!-- If the client use any Google Layers -->
	<script type="text/javascript" src="http://maps.google.com/maps/api/js?v=3.7&amp;sensor=false"></script>
	</#if>

	<script type="text/javascript" src="extjs/3.3.0/ext-3.3.0/adapter/ext/ext-base.js"></script>
	<script type="text/javascript" src="extjs/3.3.0/ext-3.3.0/ext-all.js"></script>
	<!-- The un-minimized version (in folder lib) do not works with FF4 (it's components are loaded async) -->
	<!--<script type="text/javascript" src="GeoExt/lib/GeoExt.js"></script> -->
	<script type="text/javascript" src="GeoExt/script/GeoExt.js"></script>

	<!-- Personal addition to GeoExt -->
	<script type="text/javascript" src="Ext-ux/CompositeFieldAnchor.js"></script>
	<script type="text/javascript" src="Ext-ux/IFramePanel.js"></script>
	<script type="text/javascript" src="Ext-ux/LayerTreeLoader.js"></script>
	<script type="text/javascript" src="Ext-ux/LayerNode.js"></script>
	<script type="text/javascript" src="Ext-ux/MinMaxField.js"></script>
	<script type="text/javascript" src="Ext-ux/DateField.js"></script>
	<script type="text/javascript" src="Ext-ux/NCDatetimeField.js"></script>
	<script type="text/javascript" src="Ext-ux/NCPlotPanel.js"></script>

	<script type="text/javascript" src="GeoExt-ux/LayerLegend.js"></script>
	<script type="text/javascript" src="GeoExt-ux/WMSLegend.js"></script>
	<script type="text/javascript" src="GeoExt-ux/NCWMSLegend.js"></script>
	<script type="text/javascript" src="GeoExt-ux/VectorLegend.js"></script>
	<script type="text/javascript" src="GeoExt-ux/LegendImage.js"></script>
	<script type="text/javascript" src="GeoExt-ux/LegendGroup.js"></script>
	<script type="text/javascript" src="GeoExt-ux/GroupLayerOpacitySlider.js"></script>
	<script type="text/javascript" src="GeoExt-ux/GroupLayerLoader.js"></script>

	<script type="text/javascript" src="modules/Core/Core.js"></script>
	<script type="text/javascript" src="modules/MapPanel/Layer/AbstractLayer.js"></script>
		<script type="text/javascript" src="modules/MapPanel/Layer/ArcGISMapServer.js"></script>
			<script type="text/javascript" src="modules/MapPanel/Layer/ArcGISCache.js"></script>
		<script type="text/javascript" src="modules/MapPanel/Layer/Dummy.js"></script>
		<script type="text/javascript" src="modules/MapPanel/Layer/Folder.js"></script>
		<script type="text/javascript" src="modules/MapPanel/Layer/Group.js"></script>
		<script type="text/javascript" src="modules/MapPanel/Layer/Google.js"></script>
		<script type="text/javascript" src="modules/MapPanel/Layer/KML.js"></script>
		<script type="text/javascript" src="modules/MapPanel/Layer/WMS.js"></script>
			<script type="text/javascript" src="modules/MapPanel/Layer/NCWMS.js"></script>
			<script type="text/javascript" src="modules/MapPanel/Layer/WMTS.js"></script>
		<script type="text/javascript" src="modules/MapPanel/Layer/XYZ.js"></script>
	<script type="text/javascript" src="modules/MapPanel/Layer/LayerHelper.js"></script>
	<script type="text/javascript" src="modules/MapPanel/AbstractMapPanel.js"></script>
	<script type="text/javascript" src="modules/MapPanel/EmbeddedMapPanel.js"></script>
	<script type="text/javascript" src="modules/MapPanel/MultiWMSGetFeatureInfo.js"></script>
	<script type="text/javascript" src="modules/Legend/Legend.js"></script>
	<script type="text/javascript" src="modules/Legend/LegendPanel.js"></script>
	<script type="text/javascript" src="modules/LayersPanel/LayersPanel.js"></script>
	<script type="text/javascript" src="modules/LayersPanel/AddLayersWindow.js"></script>
	<script type="text/javascript" src="modules/Trees/Trees.js"></script>
	<script type="text/javascript" src="modules/Info/Info.js"></script>
	<script type="text/javascript" src="modules/Info/OptionsPanel.js"></script>

	<script type="text/javascript">

		var parameters = OpenLayers.Util.getParameters();

		// Multi-maps
		var nbMaps = 1;
		if (parameters.maps) {
			nbMaps = parseInt(parameters.maps);
		}
		if (nbMaps < 1) { nbMaps = 1; }
		if (nbMaps > 4) { nbMaps = 4; }

		var legend = false;
		if (parameters.leg) {
			legend = (parameters.leg.toLowerCase() === 'true');
		}

		Ext.onReady(function() {
			new Ext.Button({
				renderTo : "goToMap",
				scale: 'medium',
				iconCls: 'goToMapIcon',
				tooltip: 'View larger map',
				handler: function(button, evt) {
					var url = "index.html";
					var rawParameters = window.location.search;
					if (rawParameters == null || rawParameters.length == 0) {
						rawParameters = '?'
					} else {
						rawParameters += '&'
					}
					rawParameters += 'intro=false'

					window.open(url + rawParameters);
				}
			});

			Atlas.core = new Atlas.Core("config/${mainConfig}", "config/${layersConfig}", "${timestamp}");
			Atlas.core.afterLoad = function() {
				document.getElementById('loading').style.display = 'none';

				mapLayoutItems = [];
				for (var i=0; i<nbMaps; i++) {
					var mapPanel = Atlas.core.createNewMapPanel();
					if (legend) {
						new Atlas.Legend({mapPanel: mapPanel});
					}

					mapLayoutItems.push(
							{
								flex: 1,
								layout: "border",
								deferredRender: false,
								items: [mapPanel]
							}
					);
				}

				new Ext.Viewport({
					layout: "border",
					hideBorders: true,
					items: [
						{
							region: 'center',
							layout: "hbox",
							layoutConfig: {
								align : 'stretch',
								pack  : 'start'
							},
							hideBorders: true,
							items: mapLayoutItems
						}
					]
				});
			};

			Ext.QuickTips.init();
		});
	</script>
</body>

</html>
