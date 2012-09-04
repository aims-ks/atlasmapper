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
	<meta http-equiv="content-type" content="text/html;charset=utf-8" />

	<title>${clientName}</title>
	<link rel="icon" type="image/png" href="resources/favicon.png?atlasmapperVer=${version}" />

	<#if (theme?? && theme != "")>
		<link rel="stylesheet" type="text/css" href="extjs/3.3.0/ext-3.3.0/resources/css/ext-all-notheme.css?atlasmapperVer=${version}" />
		<link rel="stylesheet" type="text/css" href="extjs/3.3.0/ext-3.3.0/resources/css/${theme}.css?atlasmapperVer=${version}" />
	<#else>
		<link rel="stylesheet" type="text/css" href="extjs/3.3.0/ext-3.3.0/resources/css/ext-all.css?atlasmapperVer=${version}" />
	</#if>

	<link rel="stylesheet" type="text/css" href="resources/css/styles.css?atlasmapperVer=${version}" />
	<!--[if lte IE 6 ]>
		<link rel="stylesheet" type="text/css" href="resources/css/styles-ie6.css?atlasmapperVer=${version}" />
	<![endif]-->
</head>

<body id="fullClient">
	<div id="loading"></div>

	<!-- This div is used to increase the view port size, to do high-res print screenshot -->
	<div id="highRes" style=""></div>

	<div id="welcomeMsg">
		${welcomeMsg!''}
	</div>
	<noscript>
		<hr/>
		<p class="noJavaScript">
			Error: <strong>JavaScript is disabled</strong>.<br/>
			You need to have <em>JavaScript enabled</em> to use the Map.
		</p>
	</noscript>
	<script type="text/javascript">
		var loadingObj = document.getElementById('loading');
		loadingObj.style.display = 'block';

		var welcomeMsgObj = document.getElementById('welcomeMsg');
		// NOTE: Visibility: hidden reserve a space to render the object.
		welcomeMsgObj.style.display = 'none';
	</script>

	<!-- IE 9+ conditional comment - this will only be executed by IE 9 and above. -->
	<!--[if gte IE 9]>
	<script type="text/javascript">
		var ie9plus = true;
	</script>
	<![endif]-->

	<script type="text/javascript" src="OpenLayers/OpenLayers-2.12/OpenLayers.js?atlasmapperVer=${version}"></script>
	<script type="text/javascript" src="OpenLayers-ux/PrintFrame.js?atlasmapperVer=${version}"></script>
	<script type="text/javascript" src="OpenLayers-ux/SearchResults.js?atlasmapperVer=${version}"></script>
	<script type="text/javascript" src="OpenLayers-ux/NCWMS.js?atlasmapperVer=${version}"></script>
	<script type="text/javascript" src="OpenLayers-ux/NCTimeSeriesClickControl.js?atlasmapperVer=${version}"></script>
	<script type="text/javascript" src="OpenLayers-ux/NCTransectDrawControl.js?atlasmapperVer=${version}"></script>

	<!-- OpenLayers support for Google layer, in version <= 2.11, has to be patched to support V > 3.6
			(since google do not support V <= 3.6 anymore)
		Bug: http://trac.osgeo.org/openlayers/ticket/3614
		Patch: https://github.com/openlayers/openlayers/commit/92f04a7a4277a6c818ef2d40a2856910ed72d3d6
		Date: 18-05-2012
	-->
	<!-- <script type="text/javascript" src="OpenLayers-ux/Google-v3.js?atlasmapperVer=${version}"></script> -->

	<#if (useGoogle)>
		<!-- If the client use any Google Layers -->
		<script type="text/javascript" src="http://maps.google.com/maps/api/js?v=3.7&amp;sensor=false&amp;atlasmapperVer=${version}"></script>
	</#if>

	<script type="text/javascript" src="extjs/3.3.0/ext-3.3.0/adapter/ext/ext-base.js?atlasmapperVer=${version}"></script>
	<script type="text/javascript" src="extjs/3.3.0/ext-3.3.0/ext-all.js?atlasmapperVer=${version}"></script>
	<!-- The un-minimized version (in folder lib) do not works with FF4 (it's components are loaded async) -->
	<!--<script type="text/javascript" src="GeoExt/lib/GeoExt.js?atlasmapperVer=${version}"></script> -->
	<script type="text/javascript" src="GeoExt/script/GeoExt.js?atlasmapperVer=${version}"></script>

	<!-- Personal addition to GeoExt -->
	<script type="text/javascript" src="Ext-ux/CompositeFieldAnchor.js?atlasmapperVer=${version}"></script>
	<script type="text/javascript" src="Ext-ux/IFramePanel.js?atlasmapperVer=${version}"></script>
	<script type="text/javascript" src="Ext-ux/LayerTreeLoader.js?atlasmapperVer=${version}"></script>
	<script type="text/javascript" src="Ext-ux/LayerNode.js?atlasmapperVer=${version}"></script>
	<script type="text/javascript" src="Ext-ux/MinMaxField.js?atlasmapperVer=${version}"></script>
	<script type="text/javascript" src="Ext-ux/DateField.js?atlasmapperVer=${version}"></script>
	<script type="text/javascript" src="Ext-ux/NCDatetimeField.js?atlasmapperVer=${version}"></script>
	<script type="text/javascript" src="Ext-ux/NCPlotPanel.js?atlasmapperVer=${version}"></script>

	<script type="text/javascript" src="GeoExt-ux/LayerLegend.js?atlasmapperVer=${version}"></script>
	<script type="text/javascript" src="GeoExt-ux/WMSLegend.js?atlasmapperVer=${version}"></script>
	<script type="text/javascript" src="GeoExt-ux/NCWMSLegend.js?atlasmapperVer=${version}"></script>
	<script type="text/javascript" src="GeoExt-ux/VectorLegend.js?atlasmapperVer=${version}"></script>
	<script type="text/javascript" src="GeoExt-ux/LegendImage.js?atlasmapperVer=${version}"></script>
	<script type="text/javascript" src="GeoExt-ux/LegendGroup.js?atlasmapperVer=${version}"></script>
	<script type="text/javascript" src="GeoExt-ux/GroupLayerOpacitySlider.js?atlasmapperVer=${version}"></script>
	<script type="text/javascript" src="GeoExt-ux/GroupLayerLoader.js?atlasmapperVer=${version}"></script>

	<script type="text/javascript" src="modules/Core/Core.js?atlasmapperVer=${version}"></script>
	<script type="text/javascript" src="modules/MapPanel/Layer/LayerState.js?atlasmapperVer=${version}"></script>
	<script type="text/javascript" src="modules/MapPanel/Layer/AbstractLayer.js?atlasmapperVer=${version}"></script>
		<script type="text/javascript" src="modules/MapPanel/Layer/ArcGISMapServer.js?atlasmapperVer=${version}"></script>
			<script type="text/javascript" src="modules/MapPanel/Layer/ArcGISCache.js?atlasmapperVer=${version}"></script>
		<script type="text/javascript" src="modules/MapPanel/Layer/Dummy.js?atlasmapperVer=${version}"></script>
		<script type="text/javascript" src="modules/MapPanel/Layer/Group.js?atlasmapperVer=${version}"></script>
		<script type="text/javascript" src="modules/MapPanel/Layer/Google.js?atlasmapperVer=${version}"></script>
		<script type="text/javascript" src="modules/MapPanel/Layer/Bing.js?atlasmapperVer=${version}"></script>
		<script type="text/javascript" src="modules/MapPanel/Layer/KML.js?atlasmapperVer=${version}"></script>
		<script type="text/javascript" src="modules/MapPanel/Layer/PrintFrame.js?atlasmapperVer=${version}"></script>
		<script type="text/javascript" src="modules/MapPanel/Layer/SearchResults.js?atlasmapperVer=${version}"></script>
		<script type="text/javascript" src="modules/MapPanel/Layer/WMS.js?atlasmapperVer=${version}"></script>
			<script type="text/javascript" src="modules/MapPanel/Layer/NCWMS.js?atlasmapperVer=${version}"></script>
			<script type="text/javascript" src="modules/MapPanel/Layer/WMTS.js?atlasmapperVer=${version}"></script>
		<script type="text/javascript" src="modules/MapPanel/Layer/XYZ.js?atlasmapperVer=${version}"></script>
	<script type="text/javascript" src="modules/MapPanel/Layer/LayerHelper.js?atlasmapperVer=${version}"></script>
	<script type="text/javascript" src="modules/MapPanel/AbstractMapPanel.js?atlasmapperVer=${version}"></script>
	<script type="text/javascript" src="modules/MapPanel/GeoExtMapPanel.js?atlasmapperVer=${version}"></script>
	<script type="text/javascript" src="modules/MapPanel/GetFeatureInfo.js?atlasmapperVer=${version}"></script>
	<script type="text/javascript" src="modules/Legend/Legend.js?atlasmapperVer=${version}"></script>
	<script type="text/javascript" src="modules/Legend/LegendPanel.js?atlasmapperVer=${version}"></script>
	<script type="text/javascript" src="modules/LayersPanel/LayersPanel.js?atlasmapperVer=${version}"></script>
	<script type="text/javascript" src="modules/LayersPanel/AddLayersWindow.js?atlasmapperVer=${version}"></script>
	<script type="text/javascript" src="modules/Trees/Trees.js?atlasmapperVer=${version}"></script>
	<script type="text/javascript" src="modules/Info/Info.js?atlasmapperVer=${version}"></script>
	<script type="text/javascript" src="modules/Info/OptionsPanel.js?atlasmapperVer=${version}"></script>

	<script type="text/javascript">

		var parameters = OpenLayers.Util.getParameters();

		var printMode = 0;
		if (parameters.printMode) {
			printMode = parseInt(parameters.printMode);
		}

		// Multi-maps
		var nbMaps = 1;
		if (parameters.maps) {
			nbMaps = parseInt(parameters.maps);
		}
		if (nbMaps < 1) { nbMaps = 1; }
		if (nbMaps > 4) { nbMaps = 4; }

		var intro = true;
		if (parameters.intro) {
			intro = (parameters.intro.toLowerCase() === 'true');
		}

		Ext.onReady(function() {
			var welcomeWindow = null;

			if (intro) {
				var showWelcomeWindow = function() {
					var welcomeMsg = welcomeMsgObj.innerHTML;
					if (typeof(welcomeMsg.trim) === 'function') {
						welcomeMsg = welcomeMsg.trim();
					} else {
						welcomeMsg = welcomeMsg.replace(/^\s+/,'').replace(/\s+$/,'');
					}

					if (welcomeMsg) {
						welcomeWindow = Ext.Msg.show({
							title:'Welcome',
							msg: welcomeMsg,
							cls: 'welcomeCls',
							minWidth: 500,
							buttons: Ext.Msg.OK
						});
					}
				};

				if (typeof(ie9plus) !== 'undefined' && ie9plus === true) {
					// This Warning window will only show up if IE is not running in compatibility mode (if it ignores the directive in the header)
					Ext.Msg.show({
						title:'WARNING',
						msg: '<p>Your browser is not well supported. It\'s strongly recommended to activate the browser compatibility mode!</p><img src="resources/images/IE9-compatibility-mode.png?atlasmapperVer=${version}">',
						cls: 'welcomeCls',
						width: 750,
						minWidth: 750,
						buttons: Ext.Msg.OK,
						icon: Ext.MessageBox.WARNING,
						fn: showWelcomeWindow
					});
				} else {
					showWelcomeWindow();
				}
			}

			Atlas.core = new Atlas.Core("config/${mainConfig}", "config/${layersConfig}", "${timestamp}");
			Atlas.core.afterLoad = function() {
				document.getElementById('loading').style.display = 'none';

// TODO
// 1. Put this somewhere else
// 2. Try to find a way to implement this in a modular way
// 3. Allow arrows, home, end keys to move the cursor in the search field
// 4. Config to specify which layer are used for search (ArcGIS only, for now)
// 5. Do a real search (ArcGIS API)
var _mapPanel = null;
search = function(form) {
	var query = form.query.value;

	var searchResultsLayer = new Atlas.Layer.SearchResults(_mapPanel, {query: query});

	if (searchResultsLayer.layer) {
		_mapPanel.map.addLayer(searchResultsLayer.layer);
	}
};

				mapLayoutItems = [];
				for (var i=0; i<nbMaps; i++) {
					var mapPanel = Atlas.core.createNewMapPanel();
if (_mapPanel == null) { _mapPanel = mapPanel; }
					new Atlas.Legend({mapPanel: mapPanel});

					mapLayoutItems.push(
						{
							flex: 1,
							layout: "border",
							deferredRender: false,
							items: [
								{
									layout: 'border',
									border: false,
									region: 'center',
									items: [
										{
											xtype: 'form',
											layout: 'hbox',
											defaultType: 'textfield',
											defaults: {
												margins: {
													top: 2,
													right: 0,
													bottom: 2,
													left: 4
												}
											},
											// Add a shadow
											floating: true, shadowOffset: 6,
											items: [
												{
													ref: 'searchField',
													name: 'search',
													hideLabel: true,
													margins: {
														top: 6,
														right: 0,
														bottom: 6,
														left: 6
													},
													// Needed to be able to catch 'keydown' event
													enableKeyEvents: true,
													listeners: {
														'specialkey': function(field, evt) {
															if (evt.getKey() == evt.ENTER) {
																search(field.getValue());
															}
														},
														// Prevent the Map from grabbing the keys (-/+ to zoom, arrows to pan, etc.)
														'keydown': function(field, evt) {
															evt.stopPropagation();
														}
													}
												}, {
													xtype: 'button',
													iconCls: 'searchButton',
													scale: 'medium',
													handler: function(btn, evt) {
														search(btn.ownerCt.searchField.getValue());
													}
												}
											],
											region: 'north'
										},
										mapPanel
									]
								},
								new Atlas.LayersPanel({
									minWidth: 180,
									mapPanel: mapPanel,
									region: 'west'
								})
							]
						}
					);
				}

				var viewportConfig = {
					layout: "border",
					hideBorders: true,
					items: [
						<#if (pageHeader?? && pageHeader != "")>
							{
								region: 'north',
								html: "${pageHeader}"
							},
						</#if>
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
						<#if (pageFooter?? && pageFooter != "")>
							,{
								region: 'south',
								html: "${pageFooter}"
							}
						</#if>
					],
					listeners: {
						// OpenLayers steal the focus of the welcome window, preventing the user from closing it using ESC or Enter.
						'afterrender': function() {
							if (welcomeWindow && welcomeWindow.getDialog()) {
								welcomeWindow.getDialog().focus.defer(1, welcomeWindow.getDialog());
							}
						}
					}
				};


				if (printMode > 1) {
					var body = document.getElementsByTagName('body')[0];
					body.style.overflow = 'auto';

					var highRes = document.getElementById('highRes');
					highRes.style.width = '5000px';
					highRes.style.height = '3300px';

					viewportConfig.height = 3300;
					viewportConfig.renderTo = 'highRes';
					new Ext.Panel(viewportConfig);
				} else {
					new Ext.Viewport(viewportConfig);
				}
			};

			Ext.QuickTips.init();
		});
	</script>
</body>

</html>
