# Workflow #

Description of the workflow followed to generate a client.

_Diagram_

### Building a data source ###
  1. When the button _Build_ is pressed, the AtlasMapper download or reuse cached document from the data source service to discover the layers. In the case of WMS service, it download the GetCapabilities document. For ArcGIS, it download the JSON files, etc.
  1. For each WMS layers, it also download its associated _TC211_ metadata document (also known as MEST record), when specified in the field _MetadataURL_ of the GetCapabilities document.
    * If the TC211 document has a _CI\_OnlineResource_ link with a name that match the layer name, than the _CI\_OnlineResource_ description is append the layer description, with the metadata description, and the links.
    * If the TC211 document do not have a _CI\_OnlineResource_ link with a name that match the layer name, than only the metadata description and the links are append to the layer description.
  1. The layer overrides described in the AtlasMapper data source's advanced tab are applied.
  1. The layers information and the data source information are saved on the disk in a _Data source saved state_ file.

### Generating a client ###
  1. When the button _Generate_ is pressed, the AtlasMapper collect the layers for each data source associated with the client, from the _Data source saved state_ files.
  1. The layer overrides described in the AtlasMapper client's advanced tab are applied.
  1. The client templates, client files, client configuration file and the layer catalog file are saved on disk, ready to use with the AtlasMapper client.

# Settings in AtlasMapper #

This field must respect the JSON syntax, a subset of JavaScript. If it contains errors, the form will refuse to submit. It always start with { and ends with }. To override an attribute of a layer, specified the layer ID following by a column : and the curly brackets { and }. Add the attributes inside the brackets.

The attributes for a layer are build from the capabilities document, for WMS layers, after applying the manual overrides from the layer data source than the manual overrides from the client.

### Examples ###

The following bloc override the mime type of the layer using the attribute _olParams_ and mime type of the legend graphics using _legendParameters_ for the layer ID _ea:bluemarble_ of the datasource _ea_ to request tiles and legend graphics as GIF:

```
{
	"ea_ea:bluemarble": {
		"olParams": {"format": "image/gif"},
		"legendParameters": {
			"FORMAT": "image/gif"
		}
	}
}
```

This example show how to deactivate the feature requests for the layer ID _ea:GBR\_GBRMPA\_GBR-features_ of the datasource _ea_ and change the default style and change the title of an other one.

```
{
	"ea_ea:GBR_GBRMPA_GBR-features": {
		"wmsQueryable": false,
		"styles": {
			"GBR-features_Reefs-outline-BW": {"default": true},
			"GBR-features_Baselayer": {
				"title": "GBR features for baselayer"
			}
		}
	}
}
```

The next example show how to disable the legend for multiple layers. This feature is useful for layer groups serve by GeoServer since this server can't provide legend graphics for grouped layers.

```
{
	"ea_GBR_JCU_Bathymetry-3DGBR_Land-and-sea": {"hasLegend": false},
	"ea_World_NE2-coast-cities-reefs_Baselayer": {"hasLegend": false},
	"ea_ea:World_NED_NE2": {"hasLegend": false}
}
```

This last example show how to move a layer from a data source to an other, in the _Add layers_ window. This is generally used to regroup layers from data source that provide only one or very few layers.

```
{
	"one_Unique-layer": {
		"dataSourceName": "e-Atlas"
	}
}
```

# Layer groups #

The _AtlasMapper_ also allow the admin to setup custom **layer groups**, that are added to the client as a single layer. That layer can be opened like a folder to set options on individual layers of the group. Since it is a client side group, each layers of the group has to be rendered separately on OpenLayers. It is advisable to keep the number of layers on a group bellow 5.

**Layer groups** have the same attributes as normal layers, but has to be entirely configured since the layer do not exist on the server. The important attributes to set are:


| **Attribute** | **Value (example)** | **Description** |
|:--------------|:--------------------|:----------------|
| layerType     | "GROUP"             | To tell AtlasMapper that it's a layer group |
| description   | "The layer's description" | This is shown in the layer info panel |
| layerBoundingBox | [134.91053, -29.91797, 156.53147, -8.29703] | The cumulative bounding box of all layers of the group, to enable the locate feature. |
| layers        | ["ea\_ea:QLD\_DEEDI\_Coastal-wetlands", "ea\_ea:QLD\_DEEDI\_Coastal-wetlands\_Study-regions"] | List of layers contained in the group, assuming they are provided by one of the data source of the client. |
| title         | "Mangroves"         | The name of the layer, as it will appear in the layer list, either in the tree and in the layer switcher. |
| version       | 1.1                 | This feature is only available with version 1.1 of the configuration standard. |
| treePath      | "Terrestrial biology/Coastal wetlands" | The layer group will appear under the folder Terrestrial biology > Coastal wetlands |
| wmsQueryable  | false               | Layer groups are not currently queryable. If this setting is not set to false, OpenLayers will try to send request to a unregistered layer, which may cause some errors. |

The following example set up a layer group titled _Mangroves_, having the following 2 layers: _ea\_ea:QLD\_DEEDI\_Coastal-wetlands_, _ea\_ea:QLD\_DEEDI\_Coastal-wetlands\_Study-regions_

```
{
	"ea_mangroves": {
		"layerType": "GROUP",
		"description": "Layer group for the layers \"QLD Coastal wetlands - Study regions\" and \"QLD Coastal wetlands (DEEDI)\"",
		"layerBoundingBox": [
			134.91053,
			-29.91797,
			156.53147,
			-8.29703
		],
		"layers": [
			"ea_ea:QLD_DEEDI_Coastal-wetlands",
			"ea_ea:QLD_DEEDI_Coastal-wetlands_Study-regions"
		],
		"title": "Mangroves",
		"version": 1.1,
		"treePath": "Terrestrial biology/Coastal wetlands",
		"wmsQueryable": false
	}
}
```

# Wiki format #

The wiki format will be modified in a near future... The [documentation](WikiFormatDoc.md) will be made available soon.

# Attributes #

The following table show the layers attributes that can be specified:

| **Attribute** | **Type** | **Description** |
|:--------------|:---------|:----------------|
| title         | String   | The displayed name of the layer, shown in multiple location in the client. |
| description   | String   | A description of the layer, shown in the layer info panel of the client. |
| descriptionFormat | String   | The format of the description field. Possible value: _wiki_, _text_, _html_. Default: _wiki_ <ul><li><b>text</b>: URL and new lines are automatically converted into HTML.</li><li><b>html</b>: No processing is done on the description. It's used directly in the <i>description</i> panel.</li><li><b>wiki</b>: The text is converted into HTML following <a href='WikiFormatDoc.md'>Wiki format rules</a>.</li></ul> |
| dataSourceId  | String   | The ID of the data source, as specified in the data source page. This field can be used to specified which URL to use to get the tiles, legend, feature requests, etc. |
| layerBoundingBox | Array of 4 decimal numbers | The coordinate of the top-left corner and the bottom-right corner of the layer. Those coordinates are used to locate the layer when the Locate button is pressed. |
| wmsQueryable  | Boolean  | true to allow a layer to receive WMS Feature requests, false otherwise. |
| cached        | Boolean  | true to allow the application to request the layer's tiles through the cache, false otherwise. Default: determined from the WMTS document (available on most GeoServer) |
| isBaseLayer   | Boolean  | true to make this layer a base layer, false otherwise. Note that it is better to use the Base layers field in the Data sources configuration page. |
| hasLegend     | Boolean  | false to disable the legend of this layer. |
| legendUrl     | URL      | URL to an image, to be use as a legend, to substitute the one provide by the server. This attribute is usually accompanied with "legendDpiSupport": false. |
| legendDpiSupport | Boolean  | False if the legend URL do not support DPI parameter. If this options is not set properly, the legend graphic will not be resized when the map DPI is changed, for layers of data sources that support DPI. |
| legendGroup   | String   | Name of the group that will be use to group legend together, in the legend window. |
| legendTitle   | String   | Name of the layer, to be display in the legend window. |
| infoHtmlUrls  | Array of URL | URLs of documents that will be displayed in the Layer info panel. |
| layers        | Array of String | Used with layer groups. List of layers, added as children in the layer switcher, when adding this layer to the map. |
| aliasIds      | Array of String | List of IDs that can be used to refer to this layer. |
| featureRequestsUrl | URL      | URL used to send the feature requests. Useful to redirect the feature requests of WebCache layers to a WMS server. |
| serverUrls    | Array of URL | URL used to request the tiles. The application currently only support one URL. The first one only is used. |
| webCacheUrl   | URL      | URL of the server used to request the layer from a cache, when the URL only contains supported parameters. |
| webCacheSupportedParameters | Array of String | List of the parameters supported by the WebCache server. |
| dataSourceName | String   | Specified a different data source name for this layer, to display it in a different tab in the Add layers window. |
| layerType     | String   | Override the type of layer (GROUP, KML, WMS, NCWMS, ARCGIS\_MAPSERVER, etc). This option should not be used in normals circumstances, other that defining a layer group.<br />NOTE: This attribute used to be called dataSourceType |
| legendParameters | String   | String of pairs parameter=value, separated by coma. Those parameters are sent with the request for the legend graphic.<br />Example: The following will increase the font size and change the font colour of a GeoServer legend (those parameters are not standard)<pre>"legendParameters": "FORMAT=image/png,LEGEND_OPTIONS=fontColor:ff0000;fontSize:16"</pre>NOTE: To add/set legends parameters to all layers of a client, use the field Legend parameters, found bellow the Layers' manual override in the advanced tab. |
| wmsFeatureRequestLayers | Array of Layer ID | List of layers to be request when a feature request is sent to this layer. |
| default       | Boolean  | true to load this layer when the client load, false otherwise. (default value: false) |
| styles        | JSON Object | Define the styles of this layer.<br />See: [List of available styles configuration](ConfigStyleDoc.md) |
| options       | Array of JSON Object | Define extra options to be added in the options panel of this layer.<br />See: [List of available options configuration](ConfigOptionsDoc.md) |
| selected      | Boolean  | false to add the layer in the layer switcher but not on the map (visibility false) when loaded for the first time.<br />NOTE: This is a temporary field that will be removed after the Save State feature has been developed. |
| shownOnlyInLayerGroup | Boolean  | true to hide the layer from the add layer tree. This is used with layers that are included in a layer group. |
| cached        | Boolean  | Default: true. Set to false if the layer should never be request to the cache (GWC). |
| olParams      | JSON Object | Define the OGC URL parameters overrides.<br />Some OpenLayers layer class has an attribute called params. Those params are used in the request to get the map images (usually tiles). They can be overridden by setting a value for olParams.<br />See: [List of available olParams configuration](ConfigOlParamsDoc.md) |
| olOptions     | JSON Object | Define the OpenLayers options overrides.<br />All OpenLayers layer class has options. Those options can be overridden by setting a value for olOptions.<br />See: [List of available olOptions configuration](ConfigOlOptionsDoc.md) |
| treePath      | String   | Path separated by "/", representing the path where the layer will be found in the add layer window. For WMS services, this attribute is usually the same as the wmsPath from WMS capabilities document. |
| wmsVersion    | String   | The WMS version send in the request, when requesting tiles direct from the WMS service; "1.1.1", "1.3.0" |
| cacheWmsVersion | String   | The WMS version send in the request, when requesting tiles through the web cache; "1.1.1", "1.3.0" |
| wmsRequestMimeType | Mime type | DEPRECATED by "olParams": {"format": "image/png"}<br />Mime type used to request the tiles. Usualy image/jpeg, image/png or image/gif |

### URL state support ###
The AtlasMapper support various URL parameters that allow linking capability to a certain map state. Not all state information can be set yet, which mean that the Save State feature is still required.

| **Parameter** | **Type** | **Description** |
|:--------------|:---------|:----------------|
| maps          | Integer  | Number of side-by-side maps the client should start with. |
| intro         | Boolean  | false to disable the welcome message (introduction splash window) (default: true).<br />NOTE: The welcome message window is not available with the embedded map. |
| leg           | Boolean  | true to enable the legend window in the embedded map (default: false). |
| ll            | Coma separated list of 2 Integer | Coordinates of the center of the map, in longitude,latitude format. |
| z             | Integer  | Zoom level      |
| l0            | Coma separated list of String | List of extra layer IDs to load on the map 0. lN can be used to specified the layers to a given map ID N, in case multi-maps are used.<br />NOTE: The client expect the layer IDs to start with the data source ID, as in the rest of the admin config. |
| s0            | Coma separated list of String | List of default style name for the extra layers of map 0. The style at index N in the list will be apply to the layer at index N in l0. The style names must match the name sent in the WMS request. sN can be used to specified the styles to a given map ID N, in case multi-maps are used. |
| o0            | Coma separated list of float | List of extra layer opacity for map 0. The layer opacity at index N in the list will be apply to the layer at index N in l0. The value must be include between 0 and 1 (default 1). oN can be used to specified the styles to a given map ID N, in case multi-maps are used. |
| v0            | Coma separated list of boolean | List of extra layer visibility for map 0. The value at index N in the list will be apply to the layer at index N in l0. The value must true or false, case insensitive (default: true). oN can be used to specified the styles to a given map ID N, in case multi-maps are used. |

**Example** of a URL for the client demo that load the map at zoom level 4 with the following extra layers:

```
ala_ALA:fire_frq
	Visible: false
ea_ea:GBR_UQ_Inshore-pesticides_AvDryPSHeq-2005-6
	Default style: Inshore-pesticides-AvDryPSHeq-Blue
	Opacity: 0.5
```

_http://domain.com/atlasmapper/client/demo/index.html?z=4&l0=ala\_ALA:fire\_frq,ea\_ea:GBR\_UQ\_Inshore-pesticides\_AvDryPSHeq-2005-6&s0=,Inshore-pesticides-AvDryPSHeq-Blue&o0=,0.5&v0=false_

# Experimental features #

The following list of features are experimental. They can be used but the interface is subject to change in the next releases, without backward compatibility.

### Web API ###
The Web API can be used to trigger action from a URL, without having to login. For security reason, the URL can only be called from localhost.

Usage: `http://localhost:8080/atlasmapper/localhost/api.jsp?action=<ACTION>&<PARAMETERS>`

| **Description** | **Action** | **Parameters** | **Examples** |
|:----------------|:-----------|:---------------|:-------------|
| Re-harvest data sources & re-generate clients.<br />**NOTE**: When data sources and clients are specified in the URL, the data sources are always harvested before generating the client. This may not be the case if they are called in individual URLs. | REFRESH    | <ul><li><b>dataSourceIds</b>: Optional coma separated list of data source ID.</li><li><b>clientIds</b>: Optional coma separated list of client ID.</li></ul> | <ul><li>Refresh the cache for the data sources ID "ea" and "imos":<br /><a href='http://localhost:8080/atlasmapper/localhost/api.jsp?action=REFRESH&dataSourceIds=ea,imos'>http://localhost:8080/atlasmapper/localhost/api.jsp?action=REFRESH&amp;dataSourceIds=ea,imos</a></li><li>Regenerate the clients ID "demo" and "maps":<br /><a href='http://localhost:8080/atlasmapper/localhost/api.jsp?action=REFRESH&clientIds=demo,maps'>http://localhost:8080/atlasmapper/localhost/api.jsp?action=REFRESH&amp;clientIds=demo,maps</a></li><li>Refresh the cache for the data source "ea", then regenerate the client "demo":<br /><a href='http://localhost:8080/atlasmapper/localhost/api.jsp?action=REFRESH&dataSourceIds=ea&clientIds=demo'>http://localhost:8080/atlasmapper/localhost/api.jsp?action=REFRESH&amp;dataSourceIds=ea&amp;clientIds=demo</a></li></ul> |