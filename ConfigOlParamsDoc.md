Define the OGC URL parameters overrides.

Some OpenLayers layer class has an attribute called params. Those params are used in the request to get the map images (usually tiles). They can be overridden by setting a value for olParams.

For example, a layer can be request as a GIF using this override:

```
"ea_ea:bluemarble" : {
	"olParams": {
		"format": "image/gif"
	}
}
```

List of some useful params:
| **Param** | **Type** | **Description** | **Example** |
|:----------|:---------|:----------------|:------------|
| format    | Mimetype (String). Default: usually "image/png" or "image/gif", depend on the server |                 | "format": "image/jpeg" |

**NOTE**: For the "version" parameter, it's preferable to use the _wmsVersion_ and _cacheWmsVersion_ attribute, since the AtlasMapper need to be able to switch between the 2 for data source that has web cache enable.

See: [OGC WMS parameters](http://nsidc.org/data/atlas/ogc_services.html#WMS).