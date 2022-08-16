AtlasMapper
===========

The AtlasMapper is a cross platform JavaScript/Java mapping application that allows a catalogue of map layers (WMS, NCWMS, ArcGIS Server, XYZ / OSM tiles, Google, Bing, basic KML and eventually other formats supported by OpenLayers) to be easily browsed, layered, re-styled and compared side-by-side in a web browser. It consists of a Java servlet server for layer catalogue management and a Javascript client based on GeoExt, Ext-JS and OpenLayers.

Demo
----

To see what the mapping client looks like try out the demo version of the client. The AtlasMapper server allows this client to be configured with Web Map Server data sources. Try out the demo version of the server (username: admin, password: admin).

Note: The demo can only connect to web services that are on the public internet and so will not work with those hosted on localhost. To work with localhost services download the AtlasMapper and run it locally.

More info
---------

For more info, consult the <a href="https://github.com/atlasmapper/atlasmapper/blob/master/INSTALL.md">AtlasMapper Guide</a> or the <a href="http://atlasmapper.org/">AtlasMapper website</a>.

Building the AtlasMapper from Source
------------------------------------

This section will eventually describe the building of the source to produce an executable WAR file.

Compiling OpenLayers
--------------------

To compile OpenLayers:

1. Modify the OpenLayers configuration file
    ```
    $ vim atlasmapper/clientResources/amc/OpenLayers/OpenLayers-2.12/build/atlasmapper.cfg
    ```

2. Install Python 2
    ```
    $ sudo apt-get install python2
    ```

3. Compile OpenLayers
    ```
    $ cd atlasmapper/clientResources/amc/OpenLayers/OpenLayers-2.12/build
    $ python2 build.py atlasmapper.cfg
    ```

4. Deploy
    ```
    $ cd atlasmapper/clientResources/amc/OpenLayers/OpenLayers-2.12/build
    $ cp OpenLayers.js ..
    ```

OpenLayers 2.12 API doc
-----------------------

OpenLayers have stopped supporting OpenLayers 2.

The API docs is only accessible using the Web Archive:
https://web.archive.org/web/20160215075209/http://dev.openlayers.org/apidocs/files/OpenLayers-js.html
