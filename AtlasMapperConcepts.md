# Introduction #

Once you have an AtlasMapper installed there are several key concepts that are necessary to understand in-order to successfully manage data sources and clients. This page focuses on the concepts and is not a step by step guide.

## Data Source and Client ##
A data source corresponds to a connection to a mapping service such as a Web Map Service, Google and ArcMap service and even a directory of KMLs. Once a data source had been configured it can be used in multiple clients by simply enabling it in the "Data Sources" tab of the client.

The client is the mapping application that the end users see in their web browser. It allows the user to pan and zoom as well as add and remove layers.

A client is a mapping portal that is set up to show a chosen set of data sources. Each client can be given a different look and feel as well as different branding and splash screen. They are also useful for setting up test clients to try out data sources without effecting production clients.

## Setting up a client for the first time ##

Before you can have a useful client you need to set up one or more data sources for your client to use. This is done by from the _Data sources_ tab of the AtlasMapper administration menu. You will then need to click _Add_ and follow the prompts.

Once you have a data source it is **critical** that you click the _Rebuild_ button in Actions column.

For WMS and ArcMap data sources the AtlasMapper uses the capabilities documents of these services to discover all the information about the layers that are available. This process can be slow, taking minutes, for services with many layers.

Some clients can have many data sources and so it would be very slow to update a client if the AtlasMapper re-downloaded the capabilities from all the sources every time a client was rebuilt. For this reason the AtlasMapper uses a cache of the data source. Clicking on the _Rebuild_ builds this cache for the specified data source. Having a cache for each data source allows us to only have to download updates for data sources that have changed.

Building a client uses the data source cache to set up its configuration, making a copy of all the information for the layers in that client.

## Updating a data source and client ##

The basic process for building a client is to:
  1. Rebuild new and updated **data sources**.
  1. Rebuild the **client**.

The information about the layers for each data sources is cached and a rebuild updates this cache. Rebuilding a client takes the information from the data source cache and uses it to build a configuration about all the layers for that client. Each client has its own complete configuration including all the information about its layers. This is effectively a cache of the layer information that allows the server to quickly know exactly what information is available to each client and it means that clients can run quite independent of each other.

If you have a data source that has changed (such as a new layer or a change in a description) and you go straight to _Rebuilding_ the client, you will see no update as the client will be simply using the old cached settings for the data source. If you instead _Rebuild_ the data source, but not the client you will not see any update as the client will still be using its old copy of the layer information. You have to _Rebuild_ the data source, updating its cache, then _Rebuild_ the client, which will copy the information from the data source cache into its config.

Rebuilding a data source **DOES NOT** update the dependant clients, it simply updates the data source cache. You have to manually trigger a _Rebuild_ of clients that use the changed data source.

Rebuilding a client **DOES NOT** re-download changes from the data source service it simply builds the client configuration (set up and layer information) from the data source cache. If the cache is not up to date then changes won't be seen in the client. You have to make sure the data sources have been updated prior to the client _Rebuild_.

## Adding metadata to layers ##
For WMS layers the AtlasMapper gets their description from both the WMS layer description and any linked ISO19115 XML metadata. The WMS layer description is display at the top, followed by the abstract and links in the metadata record. If you are using GeoServer to set up your layers linking to a metadata record involves filling out the "Metadata links" section of the "Edit Layer" page. The
  * **Type**: TC211
  * **Format**: text/xml
  * **URL**:URL to the metadata XML. For GeoNetwork this will be something like: http://e-atlas.org.au/geonetwork/srv/en/iso19139.xml?uuid=cfa69354-e845-4409-85c1-030643a239a8

## Making pretty descriptions ##
The AtlasMapper has a wiki format that allows links, bullet lists and images to be included in the layer description. Full details of this wiki format can be seen by clicking on the _Documentation_ link in the main navigation of the AtlasMapper administration interface.

Some of the common wiki formats are:
  * Bold: `*`Expression`*`
  * Italic: /expression/
  * Underline: `_`expression`_`
  * Strike: -expression-
  * `==`Heading 1`==`
  * `===`Heading 2`===`
  * Bullet list: Start a line with a star then space.
  * Hyperlink: [[url|label]] example: `[[`http://www.google.com/|Google]]
  * Image: [[IMG|url|alternative text]] example: [[IMG|../resources/images/maps-small.jpg|Maps]]