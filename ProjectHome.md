# Download latest [Atlasmapper 1.5.5](http://eatlas.org.au/pydio/data/public/atlasmapper-1-5-5-zip.php) (2nd Jul 2015) #
[See what's new](http://code.google.com/p/atlasmapper/wiki/Changelog)

# AtlasMapper #
The AtlasMapper is a cross platform JavaScript/Java mapping application that allows a catalogue of map layers (WMS, NCWMS, ArcGIS Server, XYZ / OSM tiles, Google, Bing, basic KML and eventually other formats supported by OpenLayers) from one or more sources to be easily imported, browsed, and layered in a web browser. The AtlasMapper configures the map layers from a given service automatically by reading its capabilities document. If any parts of the services are external and not in your control but need some tweaking there are powerful over write capabilities to manually change titles, descriptions, legends, etc. for any imported layer.

For WMS services if a map layer is linked to a ANZLIC metadata record then this record is automatically harvested and shown with the layer description, reducing the effort to describe layers.

The AtlasMapper consists of a Java servlet server for layer catalogue management and a Javascript client based on GeoExt, Ext-JS and OpenLayers.

## Demo ##
To see what the mapping client looks like try out the [demo version of the client](http://atlasmapper.org/atlasmapperdemo). The AtlasMapper server allows this client to be configured with Web Map Server data sources. Try out the [demo version of the server](http://atlasmapper.org/atlasmapperdemo/admin) (username: admin, password: admin).

Note: The demo can only connect to web services that are on the public internet and so will not work with those hosted on **localhost**. To work with localhost services download the AtlasMapper and run it locally.

## Example Sites ##
The following is a list of sites using the AtlasMapper.
  * [e-Atlas maps](http://maps.eatlas.org.au/). This shows the full client with a range of data sources.
  * [e-Atlas BRUVS](http://eatlas.org.au/content/gbr-aims-bruvs). This shows the embedded version of the client in a webpage.

## Installation ##
The AtlasMapper runs as a standard Java Servlet. See the [install](install.md) guide.

## Important notice ##
Version 1.4.8 store data sources information in a different format than previous version. If you are upgrading from a old version, you will need to re-generate all your data sources to take advantage of the new features. Note that you will not need to redownload the capabilities files or the MEST records.

## About Us ##
The AtlasMapper is being developed as part of the [eAtlas project](http://eatlas.org.au) which is run by the Australian Institute of Marine Science and funded under the National Environment Research Program.

[http://eatlas.org.au/sites/default/files/styles/m\_logo/public/shared/Logos/NERP-logo.JPG?itok=Kz7h-rKF](http://www.nerptropical.edu.au/)