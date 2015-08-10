# 2015-07-02: Version 1.5.5 #
> Fixed issues
    * Fix bug introduced in 1.5.4 (URL parameters double encoded)
    * Fix "Legend Parameters" field, always apply those parameters to the legend even when they are defined with the layer tiles.
    * Print frame fix "Coordinate lines" scroller label (it used to be "Scale lines")
> New feature
    * Print frame: Keep widgets near the print frame (the North arrow and the scale line)

# 2015-06-25: Version 1.5.4 #
> Client
    * Added 2 sliders to the print frame option panel, to configure the coordinate lines around the print frame.

# 2015-06-23: Version 1.5.3 #
> Fixed issues
    * Fix [issue #49](https://code.google.com/p/atlasmapper/issues/detail?id=#49) : Feature request do not work when browser zoom is not 100%. The bug was resolved in ver 1.4 and somehow it came back...
    * Fix legend duplicated parameters (which only cause problem when TRANSPARENT=FALSE is set for legend parameters in the data source configuration)
    * Fix documentation explaining how to set ncWMS default colour scale, and colour scale field label (typo)
    * Fix endless loading event with print frame and location search

# 2014-09-04: Version 1.5.1 #
> Fixed issues
    * [Issue 99](https://code.google.com/p/atlasmapper/issues/detail?id=99): Features Requests not working with OpenLayers 2.13

# 2014-08-20: Version 1.5.0 #
> Fixed issues
    * Fixed touch device "double" zoom (removed PinchZoom control; it's already included in Navigation)
    * Fixed IE bug with parameter parsing (IE "split" method do not follow ECMA Script specification, it ignores empty value)
    * Fixed IE 10 tile scramble bug (tiles get completely random on IE 10 only, after a certain number of zooming events - fixed by upgrading to OpenLayers 2.13.1)
    * Fixed a few Java warnings and potentials Null pointer exceptions.

# 2013-12-24: Version 1.4.8 #
> Fixed issues
    * [Issue 26](https://code.google.com/p/atlasmapper/issues/detail?id=26): Feature selection popup ballons show unnecessary scrollbars in Chrome
    * [Issue 28](https://code.google.com/p/atlasmapper/issues/detail?id=28): Feature selection only works on top layer - Often not appropriate
    * [Issue 55](https://code.google.com/p/atlasmapper/issues/detail?id=55): High DPI mode doesn't work with gutter oloptions
    * [Issue 65](https://code.google.com/p/atlasmapper/issues/detail?id=65): Layer re-order sometime fail when moving to the top
    * [Issue 67](https://code.google.com/p/atlasmapper/issues/detail?id=67): HTML / JavaScript injection (Security)
    * [Issue 68](https://code.google.com/p/atlasmapper/issues/detail?id=68): NullPointerException when generating a client without default layers
    * [Issue 70](https://code.google.com/p/atlasmapper/issues/detail?id=70): Misleading error reporting for WMS source with no WMTS service.
    * [Issue 71](https://code.google.com/p/atlasmapper/issues/detail?id=71): Null pointer exception while generating client with none generated data source
    * [Issue 73](https://code.google.com/p/atlasmapper/issues/detail?id=73): Square brackets in download links appear encoded to user.
    * [Issue 75](https://code.google.com/p/atlasmapper/issues/detail?id=75): Typo in Documentation
    * [Issue 77](https://code.google.com/p/atlasmapper/issues/detail?id=77): "Enable Web cache" do not work as expected
    * [Issue 79](https://code.google.com/p/atlasmapper/issues/detail?id=79): Removing a layer while other layers are loading removes loading status
> Server
    * Added configuration for measurement tools and download service
    * TC211 Parser: Better distinction between normal resources and downloadable resources
      * Download resources are only shown in the download window
> Client
    * Added area measurement tool and fixed measurement line style
    * Extended wiki-format support (the wiki format will soon be move server side and the logic will change)
    * New feature requests system (see [issue #28](https://code.google.com/p/atlasmapper/issues/detail?id=#28))
    * Download window
      * Added a download logger service capability (configurable with the admin)
      * Added a close button
    * Doctype changed to HTML5
    * Added a crash warning for IE 6

# 2013-11-4: Version 1.4.7 #
> Fixed issues
    * [Issue 65](https://code.google.com/p/atlasmapper/issues/detail?id=65): Layer re-order sometime fail when moving to the top
    * [Issue 55](https://code.google.com/p/atlasmapper/issues/detail?id=55): High DPI mode doesn't work with gutter oloptions
    * [Issue 26](https://code.google.com/p/atlasmapper/issues/detail?id=26): Feature selection popup ballons show unnecessary scrollbars in Chrome
> Server
    * Improve transfer speed
      * Added ZIP capabilities for files requests going through the AtlasMapper
      * Fixed response header to tell the browsers how to cache the files
    * Fixed issue related to errors not reported correctly when generating a client
    * Implemented basic layer search (without indexation)
    * Client configuration - Data sources now displayed in a grid, on a separate tab
> Client
    * Added layer search "beta" (using basic regex server side, without indexation)
    * Added measurement tool "beta" (not configurable - in the future, the tool will create a configurable "layer")
    * Hide legend (WMS) that contains no images (or broken images)
    * Implement Hi-DPI with KML
    * Fixed usability issue related to location search results on the map (the icon used to highlight the result on mouseover. This functionality can not be used since it imply that all vector layers would have mouse over for feature requests, including KML. The click event on search results was not implemented correctly)
    * Disabled mouse wheel on embedded map
    * Fixed some issues with the Wiki format (the style delimiter regex has been redesigned to support more cases)
    * Fixed the look of the coordinate box (mouse position) on the bottom right corner of the map
    * Loading layer now show a blue animation when selected
    * Fixed locate feature on ArcGIS layers

# 2013-07-25: Version 1.4.6 #
> Server
    * Fixed issues related to downloading files through a https server that use an invalid or self-signed certificate.
> Client
    * Fixed issue related the to re-ordering of vector layers (KML layers and print frames)
    * Fixed KML feature requests showing "undefined" on top of the balloon when the feature do not have a defined name
    * Fixed KML feature labels to looks more like Google Earth:
      * Bigger font size (17 instead of 13)
      * Position to the right of the point instead of the top (this also fix the problem of multi-line label)
      * Added a black halo around the text, white halo when the text is black or really dark
      * Fixed the default text color: White text with black halo
      * Fixed dynamic label font size, when the attribute "scale" is specified on the KML styles.

# 2013-07-17: Version 1.4.5 #
> Fixed issues
    * [Issue 64](https://code.google.com/p/atlasmapper/issues/detail?id=64): Can not unset all base layers (and other collections)
> Server
    * Show an warning when a client define a default layer that is not present in the catalog.

# 2013-07-15: Version 1.4.4 #
> Fixed issues
    * [Issue 22](https://code.google.com/p/atlasmapper/issues/detail?id=22): Data source with bad WMS capabilities fails silently
    * [Issue 27](https://code.google.com/p/atlasmapper/issues/detail?id=27): Resizing the map window size resets the position of the legend
    * [Issue 43](https://code.google.com/p/atlasmapper/issues/detail?id=43): URL Save State do not save style correctly
    * [Issue 46](https://code.google.com/p/atlasmapper/issues/detail?id=46): Client generation - better error reporting
    * [Issue 50](https://code.google.com/p/atlasmapper/issues/detail?id=50): Can not work with ncWMS
    * [Issue 58](https://code.google.com/p/atlasmapper/issues/detail?id=58): Adding a layer multiple times with different styles is not saved correctly in Map URL
    * [Issue 59](https://code.google.com/p/atlasmapper/issues/detail?id=59): Deleting a print frame before it has been drawn results in broken state
    * [Issue 61](https://code.google.com/p/atlasmapper/issues/detail?id=61): URL linking to maps fail poorly when a layer is no longer available
    * [Issue 62](https://code.google.com/p/atlasmapper/issues/detail?id=62): Client build fails with poor error message when data source URL is missing "http://";
> Issues in progress
    * [Issue 23](https://code.google.com/p/atlasmapper/issues/detail?id=23): Unclear data source caching
> Server
    * Client Preview removed. It was not useful and was double the development change for all the generation processing.
    * WMS now use version 1.1.1 instead of 1.3.0 by default. 1.3.0 is not well supported by GeoTools and ncWMS.
    * KML URLs are now checked for validity; header request sent to the server. If receive a 200, the layer is added. Otherwise, a warning message is shown.
    * XYZ layers default as base layer; the logic as been moved server side to simplify the client logic, and fix the bug of XYZ layers not showing in base layers tab.
    * Layers can now be moved from a data source to an other using layer overrides (useful to group data sources that contains very few layers)
    * Some attributes renamed:
      * dataSourceType renamed as layerType
      * webCacheParameters renamed as webCacheSupportedParameters
      * layerOptions renamed as options (options represent the widgets in the optionsPanel. layerOptions was too much confusing with olOptions)
      * wmsServiceUrl renamed to serviceUrl
      * wmsPath renamed as treePath
    * Collection elements now saved as collection in server config, instead of String. The Application can convert String to Collection, for backward compatibility and to handle HTML form output. This update affect:
      * baseLayers
      * overlayLayers
      * serviceUrls (for XYZ)
    * Now showing warning when base layers are specified for layer that do not exists.
    * Now displaying generation time for data source.
    * Data source modification flag displayed in a different column (it was sharing the status column).
    * Status can now be: OKAY (everything went fine), PASSED (error occurred but some layers have been created), INVALID (no layer is present in the catalog).
    * Moved the data source override logic in JSONObjects, to avoid having to implement a data source interface in the layer classes. All data source interface has been removed.
    * The automated interface to rebuild clients (the API) now support IP v.6
    * The AtlasMapper now try to craft a MEST URL when there is no suitable URL in the list, assuming that the server providing the records is a GeoNetwork server.
    * Renaming a client ID do not move its generated folder anymore; users were not expecting that feature, so it was causing more problem than anything else.
    * Added a "redirection" feature in the URL cache, to provide an alternative URL when the given URL fail. This solution save a lot of processing time related with broken URLs.
    * Add the definition of an "ant plugin" in the "pom.xml" to automatically create the zip bundle file upon packaging containing the application "war", the licence files and the readme.
> Client
    * Enter key now trigger field change in the optionPanel.
    * Default value of options are automatically set in the fields.
    * "Add layer window" is smaller, with no unnecessary panel headers.
    * Better DPI support;
      * The layer name and layer group name scale according to DPI.
      * Margin between legend element scale according to DPI.
      * WMS legend graphics scale according to DPI, using the WMS dpi parameter (when the "Legend support DPI" options is checked).
      * Custom WMS legend graphics, ncWMS legend graphics and legend graphics for WMS server that do not support DPI parameter are stretched according to DPI.
    * Added a "Use server cache" check box, indicating if the layer can use GWC, if it is using it and allowing the user to control if the cache is used.
    * Changing KML title logic; do not integrate the document title to the layer title.
    * Added a dummy ncWMS style to be used as default style (since there is no easy way to find out which style is used as default)
    * ncWMS feature request logic now sharing WMS code (it was a almost 100% code copy)
    * Print Frame are now saved in Saved State URL.
    * Now supporting ArcGIS cached layers (the ArcGIS documentation is almost nonexistent, this feature may not work as expected in this version).

# 2013-06-11: Version 1.4-rc2 #
> Server
    * Major refactorisation in data source harvesting / client re-generation
      * Data sources save its state in a file after parsing its files (capabilities documents, MEST documents, etc) and the client use those files.
      * The parsing is done only once, which make everything a lot more stable and strait forward, and also speed considerably the generation.
      * The process is now easier to multi-thread since the client do not refer to the downloaded files, data sources harvesting and client generation are more independent.
      * The client generation now works with JSONObjects instead of a proper LayerCatalog object, to save unnecessary process time. I may add a wrapper around the JSONObject to manage the get / set in one location (get & set methods on JSONObjects must provide a key string, which may go out of sync easily without a wrapper, if the key changes)
      * Moves generated clients to a "clients" folder, to avoid possible clashes with application folders
    * Changed "wmsPath" attribute to "treePath", since this variable is not related to a WMS service. I will probably change the "slash separated String" with an array of String for better flexibility.
    * Added a window for data source harvesting; offer to refresh the cache for the capabilities doc and/or the metadata documents.
    * Removed data source attributes about harvesting, the last harvested data is now extracted from the last modification date of the data source saved state file, and the valid flag from the content of the file.
    * Removed Bing internal cache (it should never had been set that way)
    * Moved client generation from ConfigManager to ClientConfig
    * Better URL handling; it adds "http" to URLs without protocol.
    * Disabled the client debugging (that was too much trouble to maintain and that was not working well)
    * Added wrappers around JSONObjects to simplify and unify the attributes.
    * Removed the extra layer of generation that were apply only with clients. Now layers generate the same way (using the same method) to save the data source saved state and to save it in the client config file.
    * Added a level of backward compatibility (aliases), to be able to read configuration that use old attribute names.
    * Added attribute to WMS data source
      * cacheWmsVersion for version of the cache server. The value is hardcoded to 1.1.1 since it can not be extracted from the cache WMS capabilities (it do not have any info about cached styles, that's why we are using the WMTS document)
    * Renamed some config attribute
      * wmsServiceUrl renamed with serviceUrl (alias set to wmsServiceUrl)
      * webCacheParameters renamed with webCacheSupportedParameters (alias set to webCacheParameters)
      * "live" parameter renamed "preview", for clarification (no backward compatibility needed here since the preview mode is only used with freshly made clients)
    * Refactorisation of styles in config; they are now saved as an array, which ensure they keep their order.
> Client
    * Fix ncWMS reprojection to Google projection, using the official EPSG code "EPSG:3857"
      * So far, ncWMS and ArcGIS has problem with "EPSG:900913", in the future, we may switch to the "EPSG:3857", if all services response well to it.

# 2013-03-30: Version 1.4-rc1 #
> Server
    * Added support for TC211 metadata documents (other than MCP)
    * Split the generation; data sources interface manage the data source cache and client generation only manage the client generation.
      * Also added last generation dates
    * Added an API to trigger the refresh of data sources cache and the generation of clients. The API is accessible without authentication, through localhost only. See Manual Override documentation for more information.
    * Change the "Disable caching" option to "Active download", since the option do not disable the cache anymore, it simply download the files every time it's needed (for validating the data source and for generating the client)
      * The new feature is not fully functional, unnecessary download / parsing are done. This will be fixed in the next release (required an important refactorisation)
    * Added an option in the WMS data source config (server) to disable the use of the cache server.
    * Added capability to cache style (using the WMTS capability document; WMS document do not has info about cached styles), when the server support it (GeoWebCache now allow caching of the layer styles).
    * Added layer's bounding-box to the configuration of the layers in the tree, so the "add layer window" can filter which layer are visible in the map (this feature doesn't do anything yet, the layers gray out feature will be implemented in the future).
> Client
    * Modified the layer description in the client; now showing a combination of GeoServer and MEST information. Since logic started to be tricky, I moved it in it's own class (modules/Info/DescriptionPanel.js).
    * Added an option in the layer options (client), to disable the use of the cache server (in the future, that checkbox will have to be unchecked and gray out when the cache is not usable).
    * Added a button to show downloaded links in a separated window when the MEST record has downloadable document.
    * Added https support for Bing layers
    * Attempt to add an automatic gutter option (configurable); layers requested without cache looks better with a gutter. Unfortunately, it seems impossible to set the gutter of an existing OpenLayers layer. The feature may be added in the future.
    * Fixed the look of the print frame
      * Thinner lines
      * Bigger font size for the scale widget
    * Fix multiple bugs related to layers
      * Bugs related with layer names containing space (or coma)
      * Bugs with Google layers info cache; the cache could not be cleared

# 2013-05-08: Version 1.4-beta #
> Server
    * Move the harvesting logic to the Data Source.
      * The client always use the available downloaded file, and generate a new client even if some errors are generated.
      * Added harvested date and status (valid or not) for Data Source
      * Added Data Source status in the client's list of Data Source
    * Integrated GWC capabilities (WMTS document) to the layers, to know which one is cached, with which styles.
> Client
    * Improvements with the WikiFormater (client side), added HTML test file (manual test)

# 2012-12-21: Version 1.3.1 #
> Server
    * Added URL parameter to load dataset layers from it's TC211 XML document.
    * Moved the TC211 document parsing from AbstractWMSLayerGenerator.java to Parser.java, for better reusability
    * Important refactorisation of the URLCache (now using apache HTTP Client instead of the GeoTools version) for stability / better error handling, alone with unit tests.
    * Better error reporting while generation a client
    * Important refactorisation of error handling after client generation (shows errors + warnings, generation pass only when there is no error)
    * Better exception logging (showing error message from error or from first cause that has a message) and removed all stack trace from server log (logging.properties can be modified to show them if needed)
    * Login: log only failed login attempts instead of all login attempts (successful login + restricted file access can still be logged after modifying logging.properties)
    * KML Update: defining KML data source is now more strait forward and less error prone; using a list (ExtJS grid) with edition window to set: layer ID, URL, display name and description.
    * Added httpmockup.war in the test resources, to allow developers to run URLCache unit tests.
> Client
    * Added unofficial URL parameters to load arbitrary layers, even if the client do not support them (subject to change)
    * Modified the default application config files to add the license in the JSON (to make it valid) and missing configurations
    * Fix ncWMS issue with choosing options
    * Removing OpenLayers 2.11 (the application has been using 2.12 for quite a while, I don't see any reason to go back to 2.11)

# 2012-11-26: Version 1.3 #
> Fixed issues
    * [Issue 34](https://code.google.com/p/atlasmapper/issues/detail?id=34)  Folder get deselected on the add layer window
    * [Issue 36](https://code.google.com/p/atlasmapper/issues/detail?id=36)  Base layer opacity artefacts
    * [Issue 37](https://code.google.com/p/atlasmapper/issues/detail?id=37)  The layer preview border is wrong when there is no base layer
    * [Issue 40](https://code.google.com/p/atlasmapper/issues/detail?id=40)  Opacity slider's thumb appear on top of everything
    * [Issue 45](https://code.google.com/p/atlasmapper/issues/detail?id=45)  Demo KMLs do not show
> Server
    * Added support for metadata; the AtlasMapper complete its layer information using the information available in the related metadata record.
    * Added a layer attribute to KML, to be able to load them on other projections.
    * Added special support for Metadata document support (TC211); use the applicationProfile field to defile layer overrides.
    * Added disk cache for any downloaded document (server side)
> Client
    * Added location search, using Google API, OSM API and/or ArcGIS search service.
    * Added print frames, to facilitate the preparation of a map for printing.
    * Data sources organised in tree view, to reduce the number of tab in the "add layer" window.
    * Added a URL parameter (viewport) to control the size of the map (.../index.html?viewport=3000x2500)
    * Commented layer preview for Google layers (will be added back when the other layers will have their own preview)
    * Added an exception to disable map tools when there is more than one map (side by side).
    * PrintFrame: Added corner handle, to simplify the manipulation for the frame.
    * Added an appearance attribute to add arbitrary HTML code to the HEAD of the client HTML page.
> Both client and server
    * Added XYZ tiles support, for OSM and Stamen layers.

# 2012-05-30: Version 1.2.1 #
> Version release to fix that single cryptic bug that has been in the application for a little while.
> Fixed issues
    * [Issue 38](https://code.google.com/p/atlasmapper/issues/detail?id=38)  Can not create a new client using only one data source
> Server
    * Add a check in list.html to generate the template even if the layer list is empty, rather than giving a template error.

# Version 1.2 #
> Fixed issues
    * [Issue 24](https://code.google.com/p/atlasmapper/issues/detail?id=24)  Wrong projection for the default client
    * [Issue 29](https://code.google.com/p/atlasmapper/issues/detail?id=29)  Some fields become linked and copied when multiple client configuration dialogues are open
    * [Issue 31](https://code.google.com/p/atlasmapper/issues/detail?id=31)  Scale line is wrong (no geodesic calculation)
    * [Issue 32](https://code.google.com/p/atlasmapper/issues/detail?id=32)  Manual override on a ArcGIS Folder are modified by the generation
    * [Issue 33](https://code.google.com/p/atlasmapper/issues/detail?id=33)  Manual overrides are ignored on Layer Groups and ArcGIS folder
> Issues in progress
    * [Issue 23](https://code.google.com/p/atlasmapper/issues/detail?id=23) Unclear data source caching
> Server
    * Added black and white list to filter data source layers more efficiently
    * Added olParams and olOptions manual overrides
> Client
    * Added list.html page to list all layers
    * Added URL save state using multiple URL parameters:
      * "z" to specified the zoom level,
      * "ll" to specified the map center (lon/lat),
      * "lN" to specified the layers to load on map N (N is always 0 when no multimap is used),
      * "vN" to specified which layers are visible,
      * "sN" to choose a specific style for a layer,
      * "oN" to change the layers opacity,
      * "intro" to disable the welcome window,
      * "leg" to enable the legend with the Embedded map,
      * "dl" to load the default layers,
      * "loc" to locate a layer
    * Added Embedded map feature using a slightly simplified version of the main client (this feature will eventually be more light, without any ExtJS dependencies)
    * Added a public folder "www" accessible by all clients
    * Fixed Google map popup bug (Google do not support the v3.6 of their API anymore)
    * Added a loading background image on loading layers
    * Fixed IE 9 compatibility
    * Added a configurable Welcome Window for each clients (optional)

# Version 1.2 rc1 #
> Server
    * Changed config files extension (to .json)
> Client
    * Added ArcGIS support
    * Added layer group support
    * Added branding support (page header/footer and layer panel header/footer)
    * Better NCWMS support (transect drawing and time plot)
    * Added appearance tab in the clients configuration

# Version 1.0 rc1 #
> Fixed issues
    * [Issue 1](https://code.google.com/p/atlasmapper/issues/detail?id=1)  WMS service URL does not accept the getCapabilities URL
    * [Issue 2](https://code.google.com/p/atlasmapper/issues/detail?id=2)  ncWMS service URL does not accept localhost (also apply to other services)
    * [Issue 3](https://code.google.com/p/atlasmapper/issues/detail?id=3)  Options panel can be confusing - Show layer name for clarity
> Server
    * Fix manual overrides of the datasource in the layer overrides
    * Change the way the default styles works (better logic and easy override):
      * The style with "default: true" is the one loaded when the layer is loaded,
      * The one with no ID (empty string) is the one used by the client to tell the WMS server to use it's own default style (so it can be used with GWC).
    * Allow manual overrides on collections
    * Split clientName into:
      * clientId (for ID in server side and client folder name for generation)
      * clientName (for error / warning messages)
    * Added configuration version in all config files (server.conf, users.conf)
    * Added GPL license on every proprietary files
    * Added a confirmation window on regeneration of the client, with option to regenerate everything (not just the config files)
    * Fix some descriptions in the server forms
    * Reorganize form fields
    * Added version number in server login page
    * Added server About page with version number, license, dependencies, variable value, etc. (automatically takes values from pom.xml)
    * Disabled some dangerous features for the demo version (to make a demo client, add demoMode: true to the config server.conf)
    * Add legend parameters field to datasources
> Client
    * Added configuration version in all config files (full.js, embeded.js, layers.js)
    * Added GPL license on every proprietary files
    * Split clientName into:
      * clientId (shown in the client's default URL)
      * clientName (shown in page title)
    * "amc" removed from the clients URL
    * Added version number in client index files (as HTML comment)
    * Replace line breaks with BR in layer description and truncate long URLs
    * Reintroduce the feature "auto-select" layer on add (has been accidentally deleted after refactorisation)
    * Order layer styles and "highlight" default style (using "[" and "]" for the highlight since HTML is not allow in input fields)

# Version 1.0 b1 #
> Initial import