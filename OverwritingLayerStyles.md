# Introduction #

This article describes using the manual overwrite features of the AtlasMapper to correct the styles associated with a layer. This article does not provide general documentation, but is a record of the steps that were required to correct the display of a number of layers from an external WMS service.

In this example the WMS service we wish to import has a layer containing data for a large number of species distribution maps. The species distribution map for each species was implemented as a common set of polygons that are turned on or off depending on an attribute for the particular species. A style was then set up for each species that performs enables the appropriate polygons for the species. Unfortunately with this service the species styles were NOT associated with the layer and thus do not appear in the GetCapabilities document for the WMS service. They were instead loaded directly into the client used by the site (in this case GeoExplorer).

Because the styles are not listed in the GetCapabilities document for the service the AtlasMapper does not automatically show any of these styles, preventing any access to the species distribution maps.

This tutorial shows how we can manually correct this problem in the AtlasMapper, without requiring the original service to be modified.

# Details of the WMS service #
The layer of interest (`gn:ro312_nawfa_fish_data` ) has no styles associated with it in the GetCapabilities document. As a result the AtlasMapper simply displays the layer using the default style which simply displays all the polygons for all the species.

However when we view this layer in the portal that was developed to display this data (it uses a GeoExplorer client) we see that it request the layer using a style specific for the species being displayed. In this case `ro312_gobi-glo_aure`:

```
http://atlas.track.gov.au:8080/geoserver/wms?SERVICE=WMS&STYLES=ro312_gobi-glo_aure&FORMAT=image%2Fpng&TRANSPARENT=TRUE&LAYERS=gn%3Aro312_nawfa_fish_data&EXCEPTIONS=application%2Fvnd.ogc.se_inimage&VERSION=1.1.1&REQUEST=GetMap&TILED=true&SRS=EPSG%3A3857&BBOX=14401959.119375,-1878516.406875,15028131.255,-1252344.27125&WIDTH=256&HEIGHT=256
```

Looking that the configuration of the GeoExplorer we find the names and titles of these styles can be seen in its config file.
`geoexplorer/config/31.config`:

```xml

gn:ro312_nawfa_fish_data
=ro312_clup-nem_ereb=Clupediae Nematalosa erebi (Bony bream)=false
=ro312_apog-glo_apri=Apogonidae Glossamia aprion (Mouth almighty)=false
=ro312_synb-oph_spp=Synbranchidae Ophisternon SPP (Swamp eel)=false
=ro312_oste-scl_jard=Osteoglossidae Scleropages jardinii (Saratoga)=false
=ro312_gobi-chl_ranu=Gobiidae Chlamydogobius ranunculus (Tadpole goby)=false
=ro312_gobi-glo_aure=Gobiidae Glossogobius aureus (Golden goby)=true
=ro312_gobi-glo_conc=Gobiidae Glossogobius concavifrons (Concave goby)=false
=ro312_gobi-glo_giur=Gobiidae Glossogobius giuris (Tank goby)=false
=ro312_gobi-glo_sp2m=Gobiidae Glossogobius sp2MUNROI (Munro's goby)=false
=ro312_gobi-glo_sp3d=Gobiidae Glossogobius sp3DWARF (Dwarf goby)=false
```

# Manual Overwrite #
In the AtlasMapper a Data Source was set up for this WMS service.
### General ###
  * Data source ID: track
  * Data source name: TRaCK
  * WMS service URL: http://atlas.track.gov.au/geoserver/wms
### Advanced ###
  * Web cache URL: http://atlas.track.gov.au/geoserver/gwc/service/wms
  * Layers' global manual override: See below
```json

{
"gn:ro312_nawfa_fish_data": {
"styles": {
"ro312_clup-nem_ereb": {
"title": "Clupediae Nematalosa erebi (Bony bream)",
"default": false
},
"ro312_apog-glo_apri": {
"title": "Apogonidae Glossamia aprion (Mouth almighty)",
"default": false
},
"ro312_synb-oph_spp": {
"title": "Synbranchidae Ophisternon SPP (Swamp eel)",
"default": false
},
"ro312_oste-scl_jard": {
"title": "Osteoglossidae Scleropages jardinii (Saratoga)",
"default": false
},
"ro312_gobi-chl_ranu": {
"title": "Gobiidae Chlamydogobius ranunculus (Tadpole goby)",
"default": false
},
"ro312_gobi-glo_aure": {
"title": "Gobiidae Glossogobius aureus (Golden goby)",
"default": true
},
"ro312_gobi-glo_conc": {
"title": "Gobiidae Glossogobius concavifrons (Concave goby)",
"default": false
},
"ro312_gobi-glo_giur": {
"title": "Gobiidae Glossogobius giuris (Tank goby)",
"default": false
},
"ro312_gobi-glo_sp2m": {
"title": "Gobiidae Glossogobius sp2MUNROI (Munro's goby)",
"default": false
},
"ro312_gobi-glo_sp3d": {
"title": "Gobiidae Glossogobius sp3DWARF (Dwarf goby)",
"default": false
}
}
}
}
```