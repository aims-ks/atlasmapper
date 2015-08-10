This page contains a list of features that may be added in the future to the AtlasMapper. It is really a list of ideas of what we want, however there is no guarantee that any of the features listed here will ever be implemented.

# 1.5 (next version) #

The following are potential features that will be added to the AtlasMapper. These plans will be refined based on user demand.

### Layer catalogue filtering ###
The layer catalogue will be filtered to show or add emphasis on layers that are visible on the map.

### Embedded map improvements ###
The embedded map will become more useful, with all the features that most user use, with good support for mobile devices. The current map client will be considered as a "Map editor", for doing more advanced operations like adding layers, printing map, etc.

### Wiki format improvements ###
The wiki format will looks more like natural text when it's not parsed. It will be refactored to support only basic styles. The parsing will be done server side, from a library shared with other applications.

### Better printed maps ###
The printed map will be more strait forward, with less steps and more automation. Ideally, the user would only have to click a "Print" button, choose the DPI and then the application will propose the user to download a generated image file that can be printed directly or used in a publication document.


# Future releases #

### Multi map ###
Completion of UI for side-by-side maps, allowing users to easily add and remove map panels.

### Layer search improvements ###
Implement an indexation service to return better search results.

### Optimised embedded client ###
The existing embedded client is not optimised to fully remove all dependencies on the ExtJS library, greatly increasing its download size. The core AtlasMapper catalogue is not dependant on any libraries other than OpenLayers, removing the need to load the ExtJS library. This will make the embedded client approximately half the size of the full client.

### Polygon filtered report (GBRMPA) ###
Using a polygon, grabbed from a layer of hand drawn, to generate a report for the region; statistical mean, sum, standard deviation, histograms, etc.
Technical issues
Server side:
  * GUI to allow the user to create a report template with mathematical formula (for stats)
  * Filter layer data using a polygon "mask"
  * Report service that generate a report for one or more layers (using their own template)
Client side:
  * Retrieve polygon from a vector layer
  * Draw polygon
  * Request the report using a GUI (or similar idea) that allow the user to choose which layers has to be included in the report


### Save state ###
This feature is a more complex for of the URL addressable maps. This feature allows the current configuration of the map to be saved into a JSON configuration file that is stored on the server. Saving the state of the map as a JSON file on the server allows much more state information to be captured and saved.

### Style options (AIMS) ###
Add a GUI to pass some parameter for the style with the GetMap requests. This feature exploit the SLD parameter options of GeoServer, which multiply the capabilities of each styles. The specification of the URL parameters supported by an SLD will be specified in the abstract of the style. The AtlasMapper will then pick up this spec and display the appropriate widgets in the layers options when the particular style is selected. Needs to support a drop down list, checkbox, colour, numbers.
Technical issues
Server side:
  * Add a per style layer "layer override" attribute to set style options (similar to layer options)
Client side:
  * Show the appropriate form input when a style is choose.
  * Add the style parameter to the URL.
  * Need to rearrange options to be dependant on styles.
  * Requires an upgrade to layer options as well.

### Attribution text for the map ###
Panel containing all the attribution text associated with the displayed map. This is the license text from all the layers in the map. Could also support short and long form of text.
Technical issues
Server side:
  * May use the Attribution field in the WMS standard to implement the source text. Don't know where a short form would go.
Client side:
  * Add a new panel or window like the legend.

### Server side printable maps ###
Generate server site printable map, for map preview and indexable by Google image.

# 1.4 (Current Version) #
All these features were added as part of the 1.4.x series.

### Generation improvements ###
The generation of the clients have been split. Data sources now have to be generated before the clients, which speed the generation time considerably and make the caching logic more obvious.

### Layer caching improvements ###
The application is not able to determine which layers and which styles can be cached. A checkbox is also available in the layer options to force to disable the cache. This options is quite useful for debugging.

### Generation API (AIMS) ###
An URL can be called on localhost to automatically trigger data sources and clients generation on regular basis.

### Layer search ###
A layer search has been added to search layers through the layer catalogue. It is currently doing a linear search (direct matching) through the title and description.

### MEST support improvements ###
The application can now harvest information from standard ISO 19115 records which are not MCP. Also, the layers which has downloadable resources now have a large download button to access the download files. A logger service can be setup to monitor which resources has been downloaded by who and for which purpose.

### Better KML support ###
KML now support the DPI settings and looks a lot more like in Google earth.

### Client configuration improvement ###
Data sources configuration of clients was moved in separate tab, in a grid that can be re-ordered.

### Feature requests improvements ###
The feature requests balloon now have a dropdown menu showing all available layers, which allow the user to choose which layer he is interested in.

### Loading time improvements ###
The requests going through tomcat are now zipped, which improve the download speed up to 10 times. The cache directive has also been set properly in the file headers to encourage the browser to cache the files.

### Measurement tools ###
New measurement tools were added to the client, to measure distances and areas.

# 1.3 (Older Version) #
All these features were added as part of the 1.3.x series.

### Location search for ArcGIS and Google (GBRMPA) ###
Search engine to find polygons for one or more given layer(s). This feature currently works with the search services of ArcGIS server but will later be adapted to work with OGC sources. This feature also works with Google geolocation search API.

### Print frames (latitude and longitude frame) ###
Allow the user to create a picture frame around a section of the map showing the latitude and longitude marks. The user can then take a screenshot and crop the image to create a nice looking map.
This feature can also be combined with high DPI support in the GeoServer to allow the beginnings of producing high resolution printing.

### XYZ tile support ###
Allow tile based image layers to be added. This allow OpenStreetMap and Stamen map layers to be added.

### Layer abstract from linked metadata records ###
If a WMS map layer is associated with a ISO 19115 MCP metadata record then the abstract for the layer will be merged with the one from the metadata record. This is done in the GeoServer layer by setting the metadata type to TC211, the format to "text/xml" and the URL to point to the metadata XML.

### Data sources organised in tree view ###
In version 1.2 all of the data sources were shown in different tabs in the "Add Layers" dialogue. This was a problem if there were more than a few data sources. Version 1.3 now uses a single tree view to show data sources and layers.


# 1.2 (Older Version) #
All these features were added as part of the 1.2.x series.

### Layer Grouping Support ###
This feature allows layers to be organised into a tree structure (separate from the WMS path) and this whole tree of layers appears as a single item in the add layer panel. When the group of layers has been added to the map the group can be split apart to reveal its parts. This feature is useful for representing maps from ArcGIS servers where the logical unit is a map, which contains layers in a tree. It is also useful for create groups of layers that should be handled and added together, without forcing the user to manually add each of the layers.

### Basic ESRI ArcGIS Server support ###
This allows the AtlasMapper to load layers from an ArcGIS server MapServer service (using the REST API) as a data source. This does not yet support tiled caches.

### Branding (AIMS) ###
Customisation of the Clients, by adding logo and/or HTML chunk in some predefined areas (header, footer, top of side panel, etc.). Initally we will make the branding a panel in the layer switcher panel.

### IE9 support ###
The AtlasMapper does not work correctly in IE9 due to GeoExt using ExtJS 3 which does not support IE9. This task would be to find a workaround or to upgrade the elements of GeoExt used in the AtlasMapper to use ExtJS 4 which does support IE9.

### GetCapability caching ###
The request for the GetCapability from each of the data sources is cached, with a time-out of 60 minutes. This cache can now be manually cleared using the _Clear cache_ button associated with each _Data source_.

### URL Addressable maps ###
Maps created in the main AtlasMapper client should be addressable via a URL. This URL should embody the main state of the map, such as layers, visibility, transparency and style.

### Basic embedded maps ###
With URL addressable maps create an embedded map by placing it in an IFRAME. This should include an easy way for the user to preview this client. In the future the embedded client should be optimised to be compact with no dependencies other than OpenLayers.



