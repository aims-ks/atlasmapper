## Introduction ##

This pages contains a list of ideas for potential features or improvements to the AtlasMapper. They are ideas based on what we have found difficult to use in the AtlasMapper, limitations we have found and other changes that should be made in the next couple of versions. The following is not a road map of features. Many of these ideas will never be implemented, however at the start of the development of each version of the software we will revisit this list to look for changes that should be incorporated into the roadmap.

As ideas become old and irrelevant they will be deleted from this list.

## October 2012 current dev version 1.3-rc3 ##
  * Add the ability to create a data source from a config JSON so that settings can be easily copied and shared around via email. Same for client. This would also require a way to export the settings.
## June 2012 current version 1.2.1 ##
  * Add a service on the data source to generate a white list of all layers for the data source (this would give a list of all the layer names)
  * Add black and white list capability in the client and not just in the data source.
  * Generating the client should only be one button that checks the version of the client config and code to see what needs upgrading.
## May 2012 current version 1.2 ##
  * When updating a client show a dialog listing each of the data sources being downloaded. This could also display errors relevant for each data source. Also download the new data sources in parallel.
  * List the version of the client (in the Admin GUI) and indicate whether a complete rebuild of the client is necessary.
  * Make it easy to duplicate a client or data source through the AtlasMapper server administration GUI.
  * ~~Add white list filtering (in addition to the black list) to data sources. This will allow the create of a data source that includes a limited set of layers, without needing to create a workspace.~~ Available in version 1.2
  * Need to be able to limit the panning bounds and zoom range for the map client. This is important if you only have data for a limited area.

## April 2012 current version 1.2 ##
  * Ability to create virtual layers using manual overwrites, where each virtual layer corresponds to a source layer with a style applied to it. Having one layer per layer/style combo allows these maps to be organised with a WMS path, layer grouping, descriptions, etc. This will be useful for fixing up species distribution layers where there is a common data layer with different styles for each species. At the moment changing the style for each species does not change the legend or layer description.
  * Add the ability to insert images in the layer abstract.
  * Add the style abstract at the bottom of the layer description panel. This is useful for creating layers that select data by styles, i.e. changing the species or year based on style.
  * Allow limited wiki text in the layer descriptions so that hypertext links can be added without showing the URL text. Also ties in with adding images.
  * ~~Have a splash screen for the Atlasmapper. On this page we could display an intro for what you can do with the tool.~~ (Available in version 1.2). This intro or home page should be accessible from the mapping client after its initial viewing.
  * Should be able to specify the default tab (data source) that comes up by default when the add layer dialogue is displayed.
  * Should be able to associate a description with each data source that is viewable by the end user in the client, so for example if I have a data source called "TRaCK" that users can find out more information about this.
  * When a problem occurs with a data source (such as a broken GetCapabilities document) the AtlasMapper server interface should display an informative error message.
  * Add better support for NASA's wms service: http://neowms.sci.gsfc.nasa.gov/wms. This needs time dimension support and extracting information from meta-data links and data links.
  * When many data sources are used having tabs for each source is unwieldy. They should be presented in a single tree.
  * Add a preview image from the layer in the add layer window, similar to GeoExplorer.
  * Add the ability to search for layers. This becomes more important when there are lots of layers. May need to consider how this ties in with GetNetworks.
  * Need to allow the client to dynamically switch between projections from say Google 900913 to WGS84. This is needed if you want to have Google layers included, but also wish to allow access to a service that does not support the Google project such as the http://neowms.sci.gsfc.nasa.gov/wms service. The client would not support reprojection, but would allow access to both these data sources, just not at the same time.
  * Allow layers to be tagged using keywords so that the tree can be rearranged by that keyword. Tagging with ISO19115-Elevation, e-Atlas-Physical Environment And Geography, would allow sorting into two trees, ISO19115 and e-Atlas. The data source would then indicate wish keyword prefixes to make into potential trees.
  * ~~Allow URL addressible maps, allowing layers to be added directly in the URL something like: http://maps.e-atlas.org.au/?layers=ea_ea:GBR_UQ_Inshore-pesticides_AvDryPSHeq-2005-6. This would work well when used in combination with layer grouping.~~ Available in version 1.2.
  * Display Attribution text in the information panel. Create a popup dialog that lists all the attribution text for all the layers in the map. (Suggested by Libby)
  * Allow the GetCapabilities cache period to be set for a data source. ~~A local GeoServer could then be set to a cache period of 0 sec so that it is always fresh.~~ Caching can be disabled in version 1.2.