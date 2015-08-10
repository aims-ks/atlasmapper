Define the OpenLayers options overrides.

All OpenLayers layer class has options. Those options can be overridden by setting a value for olOptions.

For example, a layer can be request as a single tile, and be displayed at 50% opacity by default:

```
"ea_ea:bluemarble" : {
	"olOptions": {
		"opacity": 0.5,
		"singleTile": true
	}
}
```

List of some useful options:
| **Option** | **Type** | **Description** | **Example** |
|:-----------|:---------|:----------------|:------------|
| opacity    | Real value, between 0 and 1. Default: 1 | The layer's opacity. | "opacity": 0.5 |
| singleTile | Boolean. Default: false | Moves the layer into single-tile mode, meaning that one tile will be loaded. The tile's size will be determined by the 'ratio' property. When the tile is dragged such that it does not cover the entire viewport, it is reloaded.<br />**NOTE**: this option should not be used with ncWMS since the tiles can not exceed 1024x1024. | "singleTile": true |
| gutter     | Integer (in pixels). Default: 0 | Determines the width (in pixels) of the gutter around image tiles to ignore. By setting this property to a non-zero value, images will be requested that are wider and taller than the tile size by a value of 2 x gutter. This allows artifacts of rendering at tile edges to be ignored. Set a gutter value that is equal to half the size of the widest symbol that needs to be displayed. Defaults to zero. Non-tiled layers always have zero gutter.<br />**IMPORTANT**: This attribute have a strange behaviour when the map DPI is modified. | "gutter": 20 |
| numZoomLevels | Integer. Default: 16 | When specified on a base layer, it set the number of zoom level for the map.<br />**NOTE**: It could be dangerous to use a value higher than 22; beyond that point, the amount of tiles needed to represent the whole world is astronomical. | "numZoomLevels": 20 |

See: [OpenLayers Grid layer API](http://dev.openlayers.org/releases/OpenLayers-2.12/doc/apidocs/files/OpenLayers/Layer/Grid-js.html), list of Properties. The list is spread out between all layers sub-classes / super-classes. It's often a matter of trial and error to find out what works and what doesn't.