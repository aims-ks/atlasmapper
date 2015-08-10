Define the styles of this layer.

List of available options for styles:
| **Attribute** | **Type** | **Description** |
|:--------------|:---------|:----------------|
| title         | String   | The displayed name for this style. It's shown in the dropdown list in the option panel of the layer. |
| description   | String   | A description for the layer. This field currently has no use in the AtlasMapper client. |
| default       | Boolean  | true to specified which style is considered as the default for GeoServer; the one that is used when the STYLES parameter is not specified in the request. You should only set this parameter if the AtlasMapper select the wrong style as the default. |
| selected      | Boolean  | true to load this style by default when the layer is loaded, false otherwise. (default value: false) |
| cached        | Boolean  | true to allow the application to request the tiles of this style through the cache, false otherwise. Default: determined from the WMTS document (available on most GeoServer) |

For more info, check the example above or the styles' definition for a layer in the file layers.json.