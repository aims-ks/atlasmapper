<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

<!-- Generated with AtlasMapper version ${version} -->
<head>
	<title>${clientName}</title>
	<link rel="icon" type="image/png" href="resources/favicon.png" />
	<meta http-equiv="content-type" content="text/html;charset=utf-8" />

	<!-- IE9 is not support by GeoExt yet, the emulation mode is supposed to fix this... -->
	<meta http-equiv="X-UA-Compatible" content="IE=EmulateIE8" />

	<link rel="stylesheet" type="text/css" href="resources/css/styles.css" />
	<link rel="stylesheet" type="text/css" href="extjs/3.3.0/ext-3.3.0/resources/css/ext-all.css" />
	<style>
		html, body, #loading { height: 100% }
		#loading {
			/*position: absolute;
			z-index: 65000;*/
			background: #FFFFFF url('resources/images/loading.gif') no-repeat center center;
		}
	</style>
</head>

<body>
	<div id="loading"></div>

	<script type="text/javascript" src="OpenLayers/OpenLayers-2.11/OpenLayers.js"></script>
	<script type="text/javascript" src="OpenLayers-ux/NCWMS.js"></script>

	<#if (useGoogle)>
		<!-- If the client use any Google Layers -->
		<script type="text/javascript" src="http://maps.google.com/maps/api/js?v=3.5&amp;sensor=false"></script>
	</#if>

	<script type="text/javascript" src="extjs/3.3.0/ext-3.3.0/adapter/ext/ext-base-debug.js"></script>
	<script type="text/javascript" src="extjs/3.3.0/ext-3.3.0/ext-all-debug.js"></script>
	<!-- The un-minimized version (in folder lib) do not works with FF4 (it's components are loaded async) -->
	<!--<script type="text/javascript" src="GeoExt/lib/GeoExt.js"></script> -->
	<script type="text/javascript" src="GeoExt/script/GeoExt.js"></script>

	<!-- Personal addition to ExtJS -->
	<script type="text/javascript" src="Ext-ux/CompositeFieldAnchor.js"></script>
	<script type="text/javascript" src="Ext-ux/IFramePanel.js"></script>
	<script type="text/javascript" src="Ext-ux/OptionsPanel.js"></script>
	<script type="text/javascript" src="Ext-ux/ConstrainTreePanel.js"></script>
	<script type="text/javascript" src="Ext-ux/ConstrainTreeDropZone.js"></script>
	<script type="text/javascript" src="Ext-ux/LayerTreeLoader.js"></script>
	<script type="text/javascript" src="Ext-ux/LayerNode.js"></script>
	<script type="text/javascript" src="Ext-ux/MinMaxField.js"></script>
	<script type="text/javascript" src="Ext-ux/DateField.js"></script>
	<script type="text/javascript" src="Ext-ux/NCDatetimeField.js"></script>

	<!-- Personal addition to GeoExt -->
	<script type="text/javascript" src="GeoExt-ux/LayerLegend.js"></script>
	<script type="text/javascript" src="GeoExt-ux/WMSLegend.js"></script>
	<script type="text/javascript" src="GeoExt-ux/NCWMSLegend.js"></script>
	<script type="text/javascript" src="GeoExt-ux/VectorLegend.js"></script>
	<script type="text/javascript" src="GeoExt-ux/LegendPanel.js"></script>
	<script type="text/javascript" src="GeoExt-ux/LegendGroup.js"></script>

	<script type="text/javascript" src="modules/Core/Core.js"></script>
	<script type="text/javascript" src="modules/MapPanel/AbstractMapPanel.js"></script>
	<script type="text/javascript" src="modules/MapPanel/GeoExtMapPanel.js"></script>
	<script type="text/javascript" src="modules/MapPanel/MultiWMSGetFeatureInfo.js"></script>
	<script type="text/javascript" src="modules/Legend/Legend.js"></script>
	<script type="text/javascript" src="modules/LayersPanel/LayersPanel.js"></script>
	<script type="text/javascript" src="modules/LayersPanel/AddLayersWindow.js"></script>
	<script type="text/javascript" src="modules/Trees/Trees.js"></script>
	<script type="text/javascript" src="modules/Info/Info.js"></script>

	<script type="text/javascript">

		var parameters = OpenLayers.Util.getParameters();

		var nbMaps = 1;
		if (parameters.maps) {
			nbMaps = parseInt(parameters.maps);
		}
		if (nbMaps < 1) { nbMaps = 1; }
		if (nbMaps > 4) { nbMaps = 4; }

		// This is the preview: Always download the config (no cache)
		var timestamp = new Date().getTime();

		Ext.onReady(function() {
			document.getElementById('loading').style.display = 'none';

			Atlas.core = new Atlas.Core("/atlasmapper/public/layersInfo.jsp?client=${clientName}&action=GET_LIVE_CONFIG", null, timestamp, true);
			Atlas.core.afterLoad = function() {
				mapLayoutItems = [];
				for (var i=0; i<nbMaps; i++) {
					var mapPanel = Atlas.core.createNewMapPanel();
					new Atlas.Legend({mapPanel: mapPanel});

					mapLayoutItems.push(
						{
							flex: 1,
							layout: "border",
							deferredRender: false,
							items: [
								mapPanel,
								new Atlas.LayersPanel({
									mapPanel: mapPanel,
									region: 'west'
								})
							]
						}
					);
				}

				new Ext.Viewport({
					layout: "hbox",
					layoutConfig: {
						align : 'stretch',
						pack  : 'start'
					},
					hideBorders: true,
					items: mapLayoutItems
				});
			};
		});
	</script>
</body>

</html>
