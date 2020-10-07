/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2011 Australian Institute of Marine Science
 *
 *  Contact: Gael Lafond <g.lafond@aims.gov.au>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
// http://dev.sencha.com/deploy/ext-4.0.2/examples/writer/writer.html

var dataSourceLayerTypes = {
    'WMS': {
        display: 'WMS',
        qtipHtml: 'Web Map Service (WMS) is a standard protocol for serving georeferenced map images provided by geospatial data applications such as <strong>GeoServer</strong>. This is the most common protocol. You will be asked to provide a GetCapabilities document URL.',
        // Default choice when the window is created
        checked: true,
        // Bold to show the user that this is a common choice
        style: {
            fontWeight: 'bold'
        }
    },
    'NCWMS': {
        display: 'ncWMS',
        qtipHtml: 'ncWMS is a Web Map Service for geospatial data that are stored in <strong>CF-compliant NetCDF</strong> files. This type of data source is not very common. You will be asked to provide a GetCapabilities document URL.'
    },
    'THREDDS': {
        display: 'THREDDS',
        qtipHtml: 'THREDDS is a Web Service for <strong>CF-compliant NetCDF</strong> data file. This type of data source is not very common. You will be asked to provide the URL of the THREDDS server.'
    },
    'ARCGIS_MAPSERVER': {
        display: 'ArcGIS MapServer',
        qtipHtml: 'ArcGIS Map servers, which provide ESRI layers. It use the ArcGIS cache feature when available.'
    },
    'GOOGLE': {
        display: 'Google',
        qtipHtml: 'Google base layers:<ul><li>Google Physical</li><li>Google Streets</li><li>Google Hybrid</li><li>Google Satellite</li></ul>'
    },
    'BING': {
        display: 'Bing',
        qtipHtml: 'Bing base layers:<ul><li>Bing Road</li><li>Bing Hybrid</li><li>Bing Aerial</li></ul>'
    },
    'WMTS': {
        display: 'WMTS',
        disabled: true,
        qtipHtml: 'Sorry, not supported yet'
    },
    'KML': {
        display: 'KML',
        qtipHtml: 'KML is the file format used to display geographic data in Earth browser such as <strong>Google Earth</strong>, <strong>Google Maps</strong>, and <strong>Google Maps for mobile</strong>. This application has basic support for this type of data source.'
    },
    'TILES': {
        display: 'Static tiles',
        disabled: true,
        qtipHtml: 'Sorry, not supported yet'
    },
    'XYZ': {
        display: 'XYZ / OSM',
        qtipHtml: 'XYZ and OSM (Open Street Map) layers.'
    }
};

// Validation type usually used with EditArea
Ext.apply(Ext.form.field.VTypes, {
    uniquedatasourceid: function(val) {
        if (val == null || val === '') {
            // The form will return an error for this field if it doesn't allow null value.
            return false;
        }

        Ext.Ajax.request({
            url: 'dataSourcesConfig.jsp?action=validateNewId',
            method: 'POST',
            params: {
                'dataSourceId': val,
                'jsonResponse': true
            },
            success: function(o) {
                if (o.responseText === 'true') {
                    resetDataSourceIdValidator(true);
                } else {
                    Ext.apply(Ext.form.VTypes, {
                        uniquedatasourceidText: 'Data source ID already in use.'
                    });
                    resetDataSourceIdValidator(false);
                }
            },
            failure: function(o) {
                Ext.apply(Ext.form.VTypes, {
                    uniquedatasourceidText: 'Communication with the server failed.'
                });
                resetDataSourceIdValidator(false);
            }
        });

        Ext.apply(Ext.form.VTypes, {
            uniquedatasourceidText: 'Validation in progress. Please wait...'
        });
        return false;
    },

    uniquedatasourceidText: 'Invalid data source ID.'
});

function resetDataSourceIdValidator(is_error) {
    Ext.apply(Ext.form.VTypes, {
        uniquedatasourceid : function(val) {
            Ext.Ajax.request({
                url: 'dataSourcesConfig.jsp?action=validateNewId',
                method: 'POST',
                params: {
                    'dataSourceId': val,
                    'jsonResponse': true
                },
                success: function(o) {
                    if (o.responseText === 'true') {
                        resetDataSourceIdValidator(true);
                    } else {
                        resetDataSourceIdValidator(false);
                    }
                },
                failure: function(o) {
                    resetDataSourceIdValidator(false);
                }
            });
            return is_error;
        }
    });
}

var frameset = null;
var rebuildTimeout = 900000; // 15 minutes

Ext.define('Writer.LayerServerConfigForm', {
    extend: 'Ext.form.Panel',
    alias: 'widget.writerlayerserverconfigform',

    requires: ['Ext.form.field.Text'],
    layerType: null,

    defaultValues: null,
    kmlStore: null,

    initComponent: function() {
        this.defaultValues = Ext.create('Writer.LayerServerConfig', {
            'layerType': 'WMS',
            'activeDownload': false,
            'webCacheEnable': true,
            'showInLegend': true,
            'forcePNG24': true,
            'legendDpiSupport': true,
            'legendParameters': 'FORMAT=image/png\nHEIGHT=10\nWIDTH=20',
            'webCacheSupportedParameters': 'LAYERS, TRANSPARENT, SERVICE, VERSION, REQUEST, EXCEPTIONS, FORMAT, CRS, SRS, BBOX, WIDTH, HEIGHT, STYLES'
        });
        this.addEvents('create');

        var notAvailableInDemoMode = demoMode ? "<br/><strong>This function is not available in the Demo version.</strong>" : "";

        /** Define items that appear on all Data Source Types **/

        var items = [
            {
                // Grids records must have an un-mutable ID
                name: 'id',
                xtype: 'hiddenfield'
            }, {
                fieldLabel: 'Data source type',
                qtipHtml: 'Protocol used by the server to provide the layers. The data source type can not be modified. If you have to change it, you have to create a new data source entry using the values of this one.',
                xtype: 'displayfield',
                value: dataSourceLayerTypes[this.layerType].display
            }, {
                name: 'layerType',
                xtype: 'hiddenfield',
                value: this.layerType
            }, {
                fieldLabel: 'Data source ID',
                qtipHtml: 'This field is used internally to associate layers with a data source. It must be short, to minimise data transfer, and unique amount the other <em>Data source ID</em>. It\'s a good practice to only use lower case letters and number for this field.<br/><strong>WARNING:</strong> Modifying the value of this field will remove the data source from clients that are using it.',
                name: 'dataSourceId',
                xtype: 'ajaxtextfield',
                ajax: {
                    url: 'dataSourcesConfig.jsp',
                    params: {
                        action: 'validateId',
                        'jsonResponse': true
                    },
                    formValues: ['id'], formPanel: this
                },
                anchor: null, size: 15,
                allowBlank: false
            }, {
                fieldLabel: 'Data source name',
                qtipHtml: 'A human readable name for this data source. Must be short and descriptive. This field is used as a title for the tab in the <em>Add layer</em> window',
                name: 'dataSourceName',
                allowBlank: false
            }, {
                fieldLabel: 'Tree root',
                qtipHtml: 'The root element in which the layers of this data source will appear in the client. This can be used to group multiple data sources together.<br/>' +
                    'Default: value of <em>Data source name</em>',
                name: 'treeRoot'
            }
        ];

        var advancedItems = [];


        /** Define the KML file URLs grid **/

        this.kmlStore = Ext.create('Ext.data.Store', {
            xtype: 'store',
            storeId: 'kmlStore',
            fields: ['id', 'url', 'title', 'description'],
            // "data" is loaded by this.setActiveRecord(record), called by the action of the "Edit" / "Add" buttons of the data source's grid.
            proxy: {
                type: 'memory',
                reader: {
                    type: 'json',
                    root: 'items'
                }
            }
        });
        var kmlDataGrid = Ext.create('Ext.grid.Panel', {
            fieldLabel: 'KML file URLs',
            qtipHtml: 'List of KML file URLs, separated by coma or new line. The file name (without extension) is used for the layer ID and the layer title. The layer title and many other attributes can be modified using the <em>Manual Override</em>.',
            name: 'kmlDataGrid',
            bodyPadding: 0,
            border: false,
            title: 'KML file URLs',
            store: this.kmlStore,
            columns: [
                { text: 'Layer ID',  dataIndex: 'id' },
                { text: 'URL', dataIndex: 'url', flex: 1 },
                { text: 'Display name', dataIndex: 'title', width: 200 },
                {
                    // http://docs.sencha.com/ext-js/4-0/#/api/Ext.grid.column.Action
                    header: 'Actions',
                    xtype: 'actioncolumn',
                    width: 50,
                    defaults: {
                        iconCls: 'grid-icon'
                    },
                    items: [
                        {
                            icon: '../resources/icons/edit.png',

                            // This attribute has to by repeated due to a bug:
                            //     defaults is ignored (http://www.sencha.com/forum/showthread.php?138446-actioncolumn-ignore-defaults)
                            iconCls: 'grid-icon',

                            tooltip: 'Edit',
                            scope: this,
                            handler: function(grid, rowIndex, colIndex) {
                                var rec = grid.getStore().getAt(rowIndex);
                                if (rec) {
                                    var editKMLWindow = this.getEditKMLWindow('save');
                                    editKMLWindow.child('.form').loadRecord(rec);
                                    editKMLWindow.show();
                                } else {
                                    frameset.setError('No record has been selected.');
                                }
                            }
                        }
                    ]
                }
            ],
            dockedItems: [
                {
                    xtype: 'toolbar',
                    items: [
                        {
                            iconCls: 'icon-add',
                            text: 'Add',
                            scope: this,
                            handler: this.onKMLAddClick
                        }, {
                            iconCls: 'icon-delete',
                            text: 'Delete',
                            disabled: true,
                            itemId: 'deleteKML',
                            scope: this,
                            handler: this.onKMLDeleteClick
                        }
                    ]
                }
            ]
        });
        kmlDataGrid.getSelectionModel().on('selectionchange', this.onKMLSelectChange, this);
        var kmlData = {
            name: 'kmlData',
            xtype: 'hiddenfield'
        };


        /** Define items that appears on some Data Source Types **/

        var browserSpecificEditAreaConfig = {
            xtype: 'editareafield',
            syntax: 'atlasmapperconfig',
            valueType: 'string',
            resizable: {transparent: true}, resizeHandles: 's'
        };
        // As usual, IE required specific workaround. This time, we simply disable the feature.
        if (Ext.isIE) {
            browserSpecificEditAreaConfig = {
                xtype: 'textareafield' // IE takes ages to display the EditArea
            }
        }

        var globalManualOverride = Ext.apply({
                fieldLabel: 'Layers\' global manual override (<a href="manualOverrideDoc.html" target="_blank">doc</a>)',
                qtipTitle: 'Layers\' global manual override',
                qtipHtml: 'JSON configuration used to override the layers information retrieved from the GetCapabilities documents. Every Atlas Mapper clients are affected by these changes.<br/>See the documentation for more information.',
                vtype: 'jsonfield',
                name: 'globalManualOverride',
                height: 400
            }, browserSpecificEditAreaConfig
        );

        var googleAPIKey = {
            fieldLabel: 'Google API Key (<a href="manualOverrideDoc.html#google" target="_blank">doc</a>)',
            qtipTitle: 'Google API Key',
            qtipHtml: 'Google API Key used to request Google map layers',
            name: 'googleAPIKey',
            allowBlank: false
        };

        // If the behaviour of this field is altered, don't forget to also modify the
        // documentation associated with it in "manualOverrideDoc.html".
        var googleJavaScript = {
            fieldLabel: 'Google Java Script (<a href="manualOverrideDoc.html#google" target="_blank">doc</a>)',
            qtipTitle: 'Google Java Script',
            qtipHtml: 'Any extra java script to include for this Google Data Source' +
                notAvailableInDemoMode,
            disabled: demoMode,
            xtype: 'textareafield',
            name: 'googleJavaScript',
            height: 300
        };

        var legendParameters = {
            // For special GeoServer legend params, see:
            // http://docs.geoserver.org/latest/en/user/services/wms/get_legend_graphic/legendgraphic.html#raster-legends-explained
            fieldLabel: 'Legend parameters',
            qtipHtml: 'List of URL parameters <i>key=value</i>, separated by coma or new line. Those parameters are added to the URL sent to request the legend graphics.',
            name: 'legendParameters',
            xtype: 'textareafield',
            resizable: {transparent: true}, resizeHandles: 's',
            height: 100
        };

        var blackAndWhiteListedLayers = {
            fieldLabel: 'Black/White layer filter',
            qtipHtml: 'List of layer ids or id patterns, separated by coma or new line. A star (<b>*</b>) can be used as a wild card, anywhere in the id string. More than one star can be used.<br/><br/>'+
                    'Ids that start with a minus (<b>-</b>) are <b>black listed</b> (removed), and those starting with a plus (<b>+</b>) are <b>white listed</b> (added). '+
                    'The AtlasMapper server does not generate any layer information for any black listed layers, so they are invisible for the AtlasMapper clients.<br/><br/>' +
                    'Initially all layers are included, then the filters are applied to this list.<br/><br/>' +
                    '<b>Example:</b> This filter will remove all <i>ea_</i> layers that do not start with <i>ea_base</i>, and will also remove the one starting with <i>ea_baselayer</i>:<br/>' +
                    '<pre>    -ea_*\n    +ea_base*\n    -ea_baselayer*</pre>',
            name: 'blackAndWhiteListedLayers',
            xtype: 'textareafield',
            resizable: {transparent: true}, resizeHandles: 's',
            height: 100
        };
        var activeDownload = {
            qtipHtml: 'Check this box to force the generation of the client to re-download the file on every generation. This is usually used with local servers, during development.<br/><b>WARNING</b> Active download slow down the generation of the client considerably.',
            boxLabel: 'Active download',
            name: 'activeDownload',
            xtype: 'checkboxfield'
        };
        var showInLegend = {
            qtipHtml: 'Uncheck this box to disable the legend for all layers provided by this data source. This mean that the layers will not have its legend displayed in the AtlasMapper clients, and they will not have a check box in the layer <em>Options</em> to show its legend.',
            boxLabel: 'Show layers in legend',
            name: 'showInLegend',
            xtype: 'checkboxfield'
        };
        var legendUrl = {
            fieldLabel: 'Legend URL',
            qtipHtml: 'This field override the legend URL provided by the capabilities document. It\'s possible to override this URL by doing one of these:'+
                '<ul>'+
                    '<li>leaving this field blank and setting a legend URL (attribute <i>legendUrl</i>) for each layer of this data source, using the <em>Layers\' global manual override</em> field</li>'+
                    '<li>providing a legend URL to a static image here, that will be used for each layers of this data source</li>'+
                '</ul>',
            name: 'legendUrl'
        };
        var legendDpiSupport = {
            boxLabel: 'Legend support DPI',
            qtipHtml: 'Uncheck this box to stretch the legend graphic instead of requesting a high DPI one, when the map DPI is changed.',
            name: 'legendDpiSupport',
            xtype: 'checkboxfield'
        };

        var wmsServiceUrl = {
            fieldLabel: 'WMS service URL',
            qtipHtml: 'URL to the layer service. This URL is used by a java library to download the WMS capabilities document. Setting this field alone with <em>Data source ID</em> and <em>Data source name</em> is usually enough. Note that a full URL to the capabilities document can also be provided, including additional parameters.<br/>NOTE: WMS services may use the file protocol here.<br/>Example: file:///somepath/capabilities.xml',
            name: 'serviceUrl'
        };
        var threddsServiceUrl = {
            fieldLabel: 'THREDDS service URL',
            qtipHtml: 'URL to the root of the THREDDS service. The AtlasMapper will harvest every layers available from that URL. Setting this field alone with <em>Data source ID</em> and <em>Data source name</em> is usually enough. Note that a full URL to a <em>catalog.xml</em> document can also be provided.<br/>Example: https://domain.com/thredds/',
            name: 'serviceUrl'
        };
        var arcGISServiceUrl = {
            fieldLabel: 'ArcGIS service URL',
            qtipHtml: 'URL to the layer service. This URL is used to download the JSON files describing the layers. Setting this field alone with <em>Data source ID</em> and <em>Data source name</em> is usually enough.<br/>Example: http://ao.com/ArcGIS/rest/services',
            name: 'serviceUrl'
        };

        var serviceUrls = {
            fieldLabel: '<a href="http://dev.openlayers.org/docs/files/OpenLayers/Layer/OSM-js.html" target="_blank">Service URLs</a>',
            qtipTitle: 'Service URLs',
            qtipHtml: 'List of URLs, separated by coma or new line, used for the layer services. Place holders may by used to specify the layer name and the format:\nhttp://tile.stamen.com/{layerName}/${z}/${x}/${y}.{format}',
            name: 'serviceUrls',
            xtype: 'textareafield',
            resizable: {transparent: true}, resizeHandles: 's',
            height: 100
        };
        var baseLayers = {
            fieldLabel: 'Base layers',
            qtipHtml: 'List of layer ids, separated by coma or new line. The layers listed here will considered as base layers by all the AtlasMapper clients.',
            name: 'baseLayers',
            xtype: 'textareafield',
            resizable: {transparent: true}, resizeHandles: 's',
            height: 100
        };
        var overlayLayers = {
            fieldLabel: 'Overlay layers',
            qtipHtml: 'List of layer ids, separated by coma or new line. The layers listed here will considered as overlay layers by all the AtlasMapper clients, meaning that all other layers will be base layers.',
            name: 'overlayLayers',
            xtype: 'textareafield',
            resizable: {transparent: true}, resizeHandles: 's',
            height: 100
        };
        var extraWmsServiceUrls = {
            fieldLabel: 'Secondary WMS service URLs',
            qtipHtml: 'List of URLs, separated by coma or new line. This field can be used to provide additional URLs to access the tiles, which is useful for doing server load balancing and allow the client\'s browser to download more tiles simultaneously.',
            name: 'extraWmsServiceUrls'
        };

        var webCacheEnable = {
            qtipHtml: 'Uncheck this box to disable tile caching (from the Web Cache server).',
            boxLabel: 'Enable Web cache',
            name: 'webCacheEnable',
            xtype: 'checkboxfield'
        };

        var webCacheCapabilitiesUrl = {
            fieldLabel: 'Web Cache capabilities document URL (WMTS)',
            qtipHtml: 'URL to the Web Cache capabilities document (WMTS). This URL will be used to get discover which layers and which styles are cached.<br/>NOTE: File protocol is allowed here.<br/>Example: file:///somepath/capabilities.xml',
            name: 'webCacheCapabilitiesUrl'
        };
        var webCacheUrl = {
            fieldLabel: 'Web Cache URL',
            qtipHtml: 'URL to the Web Cache server. This URL will be used to get the tiles for the layers. If the Web Cache server do not support all URL parameters, you have to provide a list of supported parameters in the next field.',
            name: 'webCacheUrl'
        };
        var webCacheSupportedParameters = {
            fieldLabel: 'Web Cache supported URL parameters',
            qtipHtml: 'Coma separated list of URL parameter supported by the Web Cache server. Leave this field blank if the Web Cache server support all parameters. The list of supported URL parameters for GeoWebCache is listed bellow as an example.',
            name: 'webCacheSupportedParameters'
        };
        var getMapUrl = {
            fieldLabel: 'GetMap URL',
            qtipHtml: 'This field override the GetMap URL provided by the GetCapabilities document.',
            name: 'getMapUrl'
        };
        var featureRequestsUrl = {
            fieldLabel: 'Feature requests URL',
            qtipHtml: 'This field override the feature requests URL provided by the GetCapabilities document.',
            name: 'featureRequestsUrl'
        };
        var comment = {
            fieldLabel: 'Comment',
            qtipHtml: 'Comment for administrators. This field is not display anywhere else.',
            name: 'comment',
            xtype: 'textareafield',
            resizable: {transparent: true}, resizeHandles: 's',
            height: 100
        };

        var forcePNG24 = {
            qtipHtml: 'Force PNG24 format for tiles requests. Uncheck this box to use OpenLayers default format (PNG), which is rendered as PNG8 on most ArcGIS server and give poor image quality for imagery layers.',
            boxLabel: 'Force PNG24',
            name: 'forcePNG24',
            xtype: 'checkboxfield'
        };

        var ignoredArcGISPath = {
            fieldLabel: 'Ignored ArcGIS path',
            qtipHtml: 'This field is used to work around a none standard configuration on the GBRMPA ArcGIS MapServer. If this data source is pulling it\'s configuration from GBRMPA ArcGIS MapServer, set this field to "Public/".',
            name: 'ignoredArcGISPath'
        };

        var bingAPIKey = {
            fieldLabel: 'Bing <a href="http://bingmapsportal.com" target="_blank">API key</a>',
            qtipTitle: 'Bing API key',
            qtipHtml: 'Bing layers need an API key. Get your own API key from bingmapsportal.com.',
            name: 'bingAPIKey',
            allowBlank: false
        };

        var osm = {
            fieldLabel: 'OSM',
            qtipHtml: 'Check this box if the layer is a OSM layer (OpenStreetMap). This is used to pre-define common options used with OSM layers.',
            name: 'osm',
            xtype: 'checkboxfield'
        };

        var crossOriginKeyword = {
            fieldLabel: '<a href="http://dev.openlayers.org/apidocs/files/OpenLayers/Tile/Image-js.html#OpenLayers.Tile.Image.crossOriginKeyword" target="_blank">Cross Origin Keyword</a>',
            qtipTitle: 'Cross Origin Keyword',
            qtipHtml: 'The value of the crossorigin keyword to use when loading images. Leave blank (null) for "Stamen" layers, or set it to either "anonymous" or "use-credentials".',
            name: 'crossOriginKeyword'
        };

        // Set the Form items for the different data source types
        switch(this.layerType) {
            case 'WMS':
                items.push(wmsServiceUrl);
                items.push(baseLayers);
                items.push(activeDownload);
                items.push(comment);

                advancedItems.push(globalManualOverride);
                advancedItems.push(blackAndWhiteListedLayers);
                advancedItems.push(showInLegend);
                advancedItems.push(legendParameters);
                advancedItems.push(legendUrl);
                advancedItems.push(legendDpiSupport);
                //advancedItems.push(extraWmsServiceUrls);
                advancedItems.push(webCacheEnable);
                advancedItems.push(webCacheCapabilitiesUrl);
                advancedItems.push(webCacheUrl);
                advancedItems.push(webCacheSupportedParameters);
                advancedItems.push(getMapUrl);
                advancedItems.push(featureRequestsUrl);
                break;

            case 'NCWMS':
                items.push(Ext.apply(wmsServiceUrl, { fieldLabel: 'ncWMS service URL' }));
                items.push(baseLayers);
                items.push(activeDownload);
                items.push(comment);

                advancedItems.push(globalManualOverride);
                advancedItems.push(blackAndWhiteListedLayers);
                advancedItems.push(showInLegend);
                advancedItems.push(legendParameters);
                advancedItems.push(legendUrl);
                //advancedItems.push(extraWmsServiceUrls);
                advancedItems.push(getMapUrl);
                advancedItems.push(featureRequestsUrl);
                break;

            case 'THREDDS':
                items.push(threddsServiceUrl);

                advancedItems.push(globalManualOverride);
                advancedItems.push(blackAndWhiteListedLayers);
                break;

            case 'WMTS':
                items.push(Ext.apply(wmsServiceUrl, { fieldLabel: 'WMTS service URL' }));
                items.push(baseLayers);
                items.push(activeDownload);
                items.push(comment);

                advancedItems.push(globalManualOverride);
                advancedItems.push(blackAndWhiteListedLayers);
                advancedItems.push(showInLegend);
                advancedItems.push(legendParameters);
                advancedItems.push(legendUrl);
                //advancedItems.push(extraWmsServiceUrls);
                advancedItems.push(getMapUrl);
                advancedItems.push(featureRequestsUrl);
                break;

            case 'KML':
                items.push(comment);
                items.push(kmlData);

                advancedItems.push(globalManualOverride);
                break;

            case 'GOOGLE':
                items.push(googleAPIKey);
                items.push(comment);

                advancedItems.push(globalManualOverride);
                advancedItems.push(blackAndWhiteListedLayers);
                advancedItems.push(googleJavaScript);

                break;

            case 'BING':
                items.push(bingAPIKey);
                items.push(comment);

                advancedItems.push(globalManualOverride);
                advancedItems.push(blackAndWhiteListedLayers);
                break;

            case 'ARCGIS_MAPSERVER':
                items.push(arcGISServiceUrl);
                items.push(baseLayers);
                items.push(activeDownload);
                items.push(comment);

                advancedItems.push(globalManualOverride);
                advancedItems.push(blackAndWhiteListedLayers);
                advancedItems.push(showInLegend);
                advancedItems.push(legendUrl);
                advancedItems.push(forcePNG24);
                advancedItems.push(ignoredArcGISPath);
                break;

            case 'XYZ':
                items.push(serviceUrls);
                items.push(crossOriginKeyword);
                items.push(osm);
                items.push(globalManualOverride);
                items.push(overlayLayers);
                items.push(comment);
                break;
        }

        var tabs = [{
            // Normal config options
            title: 'General',
            items: items
        }];

        if (this.layerType === 'KML') {
            tabs.push(kmlDataGrid);
        }

        if (advancedItems.length > 0) {
            tabs.push({
                // Advanced config options
                title: 'Advanced',
                items: advancedItems
            });
        }

        Ext.apply(this, {
            activeRecord: null,
            border: false,
            autoScroll: true,
            defaultType: 'textfield',
            fieldDefaults: {
                msgTarget: 'side', // Display an icon next to the field when it's not valid
                anchor: '100%',
                qtipMaxWidth: 200,
                labelAlign: 'right',
                labelWidth: 150,
                hideEmptyLabel: false // Align fields with no label
            },
            items: [{
                xtype: 'tabpanel',
                border: false,
                height: '100%', width: '100%',
                defaults: {
                    layout: 'anchor',
                    autoScroll: true,
                    border: false,
                    bodyPadding: 5,
                    defaultType: 'textfield'
                },
                items: tabs
            }]
        });

        this.callParent();
    },

    onKMLSelectChange: function(selModel, selections){
        this.down('#deleteKML').setDisabled(selections.length === 0);
    },

    onKMLAddClick: function() {
        var editKMLWindow = this.getEditKMLWindow('add');
        editKMLWindow.show();
    },
    onKMLDeleteClick: function(button) {
        var grid = button.ownerCt.ownerCt;

        var selection = grid.getView().getSelectionModel().getSelection()[0];
        if (selection) {
            grid.store.remove(selection);
            this._saveKMLRecords();
        } else {
            frameset.setError('Can not find the KML entry to delete');
        }
    },

    getEditKMLWindow: function(action) {
        var that = this;
        var buttons = [];
        var title = '';
        if (action === 'save') {
            title = 'Edit KML layer';
            buttons = [
                {
                    iconCls: 'icon-okay',
                    text: 'Apply',
                    handler: function() {
                        var window = this.ownerCt.ownerCt;
                        var form = window.child('.form');
                        var newValues = form.getValues();
                        var record = form.getRecord();

                        // Get the new KML values, update the current KML record and save it to the data source form
                        Ext.iterate(newValues, function(key, value) {
                            record.set(key, value);
                        });
                        that._saveKMLRecords();

                        window.close();
                    }
                }, {
                    iconCls: 'icon-cancel',
                    text: 'Cancel',
                    handler: function() {
                        var window = this.ownerCt.ownerCt;
                        window.close();
                    }
                }
            ];
        } else {
            title = 'Add KML layer';
            buttons = [
                {
                    iconCls: 'icon-add',
                    text: 'Create',
                    handler: function() {
                        var window = this.ownerCt.ownerCt;
                        var form = window.child('.form');
                        var newValues = form.getValues();

                        // Add the new entry to the KML store and save it to the data source form
                        that.kmlStore.add(newValues);
                        that._saveKMLRecords();

                        window.close();
                    }
                }, {
                    iconCls: 'icon-cancel',
                    text: 'Cancel',
                    handler: function() {
                        var window = this.ownerCt.ownerCt;
                        window.close();
                    }
                }
            ];
        }

        return new Ext.create('Ext.window.Window', {
            title: title,
            width: 500,
            layout: 'fit',
            constrainHeader: true,
            closeAction: 'destroy',

            items: [
                {
                    xtype: 'form',
                    border: false,
                    bodyPadding: 5,
                    defaultType: 'textfield',
                    fieldDefaults: {
                        anchor: '100%',
                        labelAlign: 'right',
                        labelWidth: 100
                    },
                    items: [
                        {
                            fieldLabel: 'Layer ID',
                            qtipHtml: 'Unique layer ID.<br/>\n<b>WARNING</b> The value of this field must be unique amount ' +
                                    'all layers of all datasets. Using the dataset ID as a prefix is a good way to avoid clashes.',
                            name: 'id'
                        }, {
                            fieldLabel: 'URL',
                            qtipTitle: 'URL <i>(kmlUrl)</i>',
                            qtipHtml: 'URL to the KML file. It can be a static file or a online service. Note that <i>KMZ</i> are <i>not supported</i>.',
                            name: 'url'
                        }, {
                            fieldLabel: 'Display name',
                            qtipTitle: 'Display name <i>(title)</i>',
                            qtipHtml: 'Name of the layer, as displayed in the client; in the add layer tree, layer description and layer switcher.',
                            name: 'title'
                        }, {
                            fieldLabel: 'Description (<a href="manualOverrideDoc.html#wiki" target="_blank">doc</a>)',
                            qtipTitle: 'Description <i>(description)</i>',
                            qtipHtml: 'Description in wiki format. For information about the wiki format, refer to the documentation.',
                            name: 'description',
                            xtype: 'textareafield',
                            resizable: {transparent: true}, resizeHandles: 's',
                            height: 100
                        }
                    ]
                }
            ],
            buttons: buttons
        });
    },

    // Save the content of the grid into the kmlData hidden field.
    _saveKMLRecords: function() {
        // Update the current data source record using the data from the KML grid store
        var kmlData = [];
        this.kmlStore.each(function(record) {
            kmlData.push(record.data);
        }, this);

        this.getForm().setValues({
            'kmlData': Ext.JSON.encode(kmlData)
        });
    },

    setActiveRecord: function(record){
        var form = this.getForm();
        this.kmlStore.loadData([]);
        if (record) {
            this.activeRecord = record;
            form.loadRecord(record);
            var kmlData = record.get('kmlData');
            if (kmlData) {
                // Remove the proxy (proxy = transparent object wrapper to remember modification)
                // ExtJS will recreate a new proxy if needed.
                // Logically, the following operation should have no effect... but it really get rid of the proxy.
                var cleanKMLData = [];
                Ext.each(kmlData, function(_kmlData) {
                    cleanKMLData.push(_kmlData);
                });

                this.getForm().setValues({
                    'kmlData': Ext.JSON.encode(cleanKMLData)
                });

                this.kmlStore.loadData(cleanKMLData);
            }

            // "serviceUrls" is used by XYZ layers (but may be used by other data source type in the future).
            // "baseLayers" and "overlayLayers" are used with all data sources.
            // They are saved as JSONArray in the AtlasMapper layer catalog.
            // By default, ExtJS format it as a coma separated string,
            // which do not looks nice in a text area.
            var serviceUrls = record.get('serviceUrls');
            if (serviceUrls) {
                var serviceUrlsStr = '';
                for (var i=0, len=serviceUrls.length; i<len; i++) {
                    if (serviceUrlsStr) {
                        serviceUrlsStr += ',\n';
                    }
                    serviceUrlsStr += serviceUrls[i];
                }
                this.getForm().setValues({
                    'serviceUrls': serviceUrlsStr
                });
            }
            var baseLayers = record.get('baseLayers');
            if (baseLayers) {
                var baseLayersStr = '';
                for (var i=0, len=baseLayers.length; i<len; i++) {
                    if (baseLayersStr) {
                        baseLayersStr += ',\n';
                    }
                    baseLayersStr += baseLayers[i];
                }
                this.getForm().setValues({
                    'baseLayers': baseLayersStr
                });
            }
            var overlayLayers = record.get('overlayLayers');
            if (overlayLayers) {
                var overlayLayersStr = '';
                for (var i=0, len=overlayLayers.length; i<len; i++) {
                    if (overlayLayersStr) {
                        overlayLayersStr += ',\n';
                    }
                    overlayLayersStr += overlayLayers[i];
                }
                this.getForm().setValues({
                    'overlayLayers': overlayLayersStr
                });
            }
        } else if (this.defaultValues) {
            this.activeRecord = this.defaultValues;
            form.loadRecord(this.defaultValues);
        } else {
            this.activeRecord = null;
            form.reset();
        }
        // The defaultValues record (and the reset) also change the layerType.
        // It seems to be no way to prevent that (I can't remove the value
        // from the defaultValues instance since the model is unmutable),
        // but it can be restore here.
        form.setValues({layerType: this.layerType});
    },

    onSave: function(){
        frameset.setSavingMessage('Saving...');
        var active = this.activeRecord,
            form = this.getForm();

        if (!active) {
            frameset.setError('The record can not be found.');
            return false;
        }
        if (form.isValid()) {
            // Resync the model instance
            // NOTE: The "active" instance become out of sync everytime the grid get refresh; by creating an instance or updating an other one, for example. The instance can not be saved when it's out of sync.
            active = active.store.getById(active.internalId);
            form.updateRecord(active);
            frameset.setSavedMessage('Data source saved', 500);
            return true;
        }
        frameset.setError('Some fields contains errors.');
        return false;
    },

    onCreate: function(){
        frameset.setSavingMessage('Creating...');
        var form = this.getForm();
        if (form.isValid()) {
            this.fireEvent('create', this, form.getValues());
            form.reset();
            frameset.setSavedMessage('Data source created', 500);
            return true;
        }
        frameset.setError('Some fields contains errors.');
        return false;
    },

    reset: function(){
        this.getForm().reset();
        this.setActiveRecord(this.defaultValues);
    },

    onReset: function(){
        this.reset();
    }
});

// Definition of the Grid (the table that show the list of data sources)
Ext.define('Writer.LayerServerConfigGrid', {
    extend: 'Ext.grid.Panel',
    alias: 'widget.writerlayerserverconfiggrid',

    requires: [
        'Ext.form.field.Text',
        'Ext.toolbar.TextItem'
    ],

    renderStatus: function(status) {
        var htmlStatus = '<span class="grid-false" title="Invalid - Run harvest to be able to use it"><span class="text">Invalid</span></span>';
        if (status !== null) {
            if (status === 'OKAY') {
                htmlStatus = '<span class="grid-true" title="Valid - Can be used by clients"><span class="text">Valid</span></span>';
            } else if (status === 'PASSED') {
                htmlStatus = '<span class="grid-warning"><span class="text" title="Usable - There were errors during the last rebuild, but the data source may still be used">Usable</span></span>';
            }
        }
        return htmlStatus;
    },

    renderLayerCount: function(layerCount) {
        return layerCount <= 0 ? '<span class="grid-error">0</span>' : '<span class="grid-success">' + layerCount + '</span>';
    },

    initComponent: function(){
        var that = this;

        Ext.apply(this, {
            iconCls: 'icon-grid',
            dockedItems: [
                {
                    xtype: 'toolbar',
                    items: [
                        {
                            iconCls: 'icon-add',
                            text: 'Add',
                            scope: this,
                            handler: this.onAddClick
                        }, {
                            iconCls: 'icon-delete',
                            text: 'Delete',
                            disabled: true,
                            itemId: 'delete',
                            scope: this,
                            handler: this.onDeleteClick
                        }
                    ]
                }
            ],
            columns: [
                {
                    width: 30,
                    align: 'center',
                    sortable: true,
                    // The icon is placed using CSS and the text is hidden with CSS.
                    renderer: that.renderStatus,
                    dataIndex: 'status'
                }, {
                    header: 'Data source ID',
                    width: 100,
                    sortable: true,
                    dataIndex: 'dataSourceId'
                }, {
                    header: 'Type',
                    width: 80,
                    sortable: true,
                    dataIndex: 'layerType'
                }, {
                    header: 'Data source name',
                    flex: 1,
                    sortable: true,
                    dataIndex: 'dataSourceName'
                }, {
                    header: 'Layers',
                    width: 70,
                    sortable: true,
                    dataIndex: 'layerCount',
                    renderer: that.renderLayerCount
                }, {
                    header: 'Last build',
                    width: 130,
                    sortable: true,
                    dataIndex: 'lastHarvested'
                }, {
                    header: 'Modified',
                    width: 55,
                    align: 'center',
                    sortable: true,
                    xtype: 'booleancolumn',
                    // The icon is placed using CSS and the text is hidden with CSS.
                    trueText: '<span class="modified-true"><span class="text">True</span></span>',
                    falseText: '<span class="modified-false"><span class="text">False</span></span>',
                    dataIndex: 'modified'
                }, {
                    // http://docs.sencha.com/ext-js/4-0/#/api/Ext.grid.column.Action
                    header: 'Actions',
                    xtype: 'actioncolumn',
                    width: 85, // padding of 8px between icons
                    defaults: {
                        iconCls: 'grid-icon'
                    },
                    items: [
                        {
                            icon: '../resources/icons/edit.png',

                            // Bug: defaults is ignored (http://www.sencha.com/forum/showthread.php?138446-actioncolumn-ignore-defaults)
                            iconCls: 'grid-icon',

                            tooltip: 'Edit',
                            scope: this,
                            handler: function(grid, rowIndex, colIndex) {
                                var rec = grid.getStore().getAt(rowIndex);
                                if (rec) {
                                    var layerType = rec.get('layerType');
                                    if (layerType) {
                                        // Close the all windows and show the edit window.
                                        var window = this.getLayerConfigFormWindow(layerType, rec);
                                        if (window) {
                                            window.child('.form').setActiveRecord(rec);
                                            window.show();
                                        } else {
                                            frameset.setError('Unsupported data source layer type ['+layerType+']');
                                        }
                                    }
                                } else {
                                    frameset.setError('No record has been selected.');
                                }
                            }

                        }, {
                            icon: '../resources/icons/cog_go.png',
                            // Bug: defaults is ignored (http://www.sencha.com/forum/showthread.php?138446-actioncolumn-ignore-defaults)
                            iconCls: 'grid-icon',
                            tooltip: 'Rebuild',
                            scope: this,
                            handler: function(that) {
                                return function(grid, rowIndex, colIndex) {
                                    var rec = grid.getStore().getAt(rowIndex);
                                    if (rec) {
                                        that.handleRebuild(rec);
                                    } else {
                                        frameset.setError('No record has been selected.');
                                    }
                                };
                            } (this)

                        }, {
                            icon: '../resources/icons/application_xp_terminal.png',
                            // Bug: defaults is ignored (http://www.sencha.com/forum/showthread.php?138446-actioncolumn-ignore-defaults)
                            iconCls: 'grid-icon',
                            tooltip: 'View logs',
                            scope: this,
                            handler: function(that) {
                                return function(grid, rowIndex, colIndex) {
                                    var rec = grid.getStore().getAt(rowIndex);
                                    if (rec) {
                                        that.handleShowLogs(rec);
                                    } else {
                                        frameset.setError('No record has been selected.');
                                    }
                                };
                            } (this)
                        }

                    ]
                }
            ]
        });
        this.callParent();
        this.getSelectionModel().on('selectionchange', this.onSelectChange, this);
    },

    handleRebuild: function(rec) {
        if (rec) {
            var dataSourceId = rec.get('dataSourceId');
            var dataSourceName = rec.get('dataSourceName') + ' (' + dataSourceId + ')';

            // If running, show the logs instead
            frameset.isThreadRunning(
                'dataSourcesConfig.jsp',
                {
                    'action': 'GETLOGS',
                    'dataSourceId': dataSourceId
                },

                function(that, rec) {
                    return function(isRunning) {
                        if (isRunning) {
                            that.showDataSourceRebuildLogWindow(frameset, dataSourceId, dataSourceName);
                        } else {
                            that.getRebuildWindow(rec).show();
                        }
                    };
                }(this, rec)
            );
        }
    },

    handleShowLogs: function(rec) {
        if (rec) {
            var dataSourceId = rec.get('dataSourceId');
            var dataSourceName = rec.get('dataSourceName') + ' (' + dataSourceId + ')';

            this.showDataSourceRebuildLogWindow(frameset, dataSourceId, dataSourceName);
        }
    },

    getRebuildWindow: function(rec) {
        var that = this;
        var layerType = 'ALL';
        var windowTitle = 'Rebuild all data source';
        var dataSourceId = null;
        var dataSourceName = null;
        if (rec) {
            layerType = rec.get('layerType');
            dataSourceId = rec.get('dataSourceId');
            dataSourceName = rec.get('dataSourceName') + ' (' + dataSourceId + ')';
            windowTitle = 'Rebuild <i>' + dataSourceName + '</i>';
        }

        var windowContent = [];
        switch (layerType) {
            case 'WMS':
                windowContent = [
                    {
                        xtype: 'displayfield',
                        value: 'Rebuild the data source information with the latest settings and re-harvest documents.'
                    }, {
                        xtype: 'checkboxfield',
                        qtipHtml: 'Redownload the capabilities document.',
                        boxLabel: 'Redownload the capabilities document',
                        checked: true,
                        name: 'clearCapCache'
                    }, {
                        xtype: 'checkboxfield',
                        qtipHtml: 'Redownload the MEST records. This operation may take several minutes.',
                        boxLabel: 'Redownload the MEST records.',
                        name: 'clearMestCache'
                    }
                ];
                break;

            case 'NCWMS':
                windowContent = [
                    {
                        xtype: 'displayfield',
                        value: 'Rebuild the data source information with the latest settings and re-harvest capabilities documents.'
                    }, {
                        xtype: 'checkboxfield',
                        qtipHtml: 'Redownload the capabilities document.',
                        boxLabel: 'Redownload the capabilities document',
                        checked: true,
                        name: 'clearCapCache'
                    }
                ];
                break;

            case 'THREDDS':
                windowContent = [
                    {
                        xtype: 'displayfield',
                        value: 'Rebuild the data source information with the latest settings and re-harvest the catalogue.'
                    }, {
                        xtype: 'checkboxfield',
                        qtipHtml: 'Redownload the catalogue.',
                        boxLabel: 'Redownload the catalogue',
                        checked: true,
                        name: 'clearCapCache'
                    }
                ];
                break;

            case 'ARCGIS_MAPSERVER':
                windowContent = [
                    {
                        xtype: 'displayfield',
                        value: 'Rebuild the data source information with the latest settings and re-harvest JSON documents.'
                    }, {
                        xtype: 'checkboxfield',
                        qtipHtml: 'Redownload the JSON documents (equivalent to WMS capabilities document for ArcGIS services).',
                        boxLabel: 'Redownload the JSON documents',
                        checked: true,
                        name: 'clearCapCache'
                    }
                ];
                break;

            case 'KML':
                windowContent = [
                    {
                        xtype: 'displayfield',
                        value: 'Rebuild the data source information with the latest settings and re-check KML URLs.'
                    }, {
                        xtype: 'checkboxfield',
                        qtipHtml: 'Send a request to the server to verify if the KML files exist on the server. Only uncheck this option if you know that the URLs are valid and the server providing the KMLs is temporary down or overloaded, or you are on a very limited internet connection.',
                        boxLabel: 'Re-check KML URLs',
                        checked: true,
                        name: 'clearCapCache'
                    }
                ];
                break;

            default:
                windowContent = [
                    {
                        xtype: 'displayfield',
                        value: 'Rebuild the data source information with the latest settings.'
                    }
                ];
                break;
        }

        return Ext.create('Ext.window.Window', {
            title: windowTitle,
            width: 450,
            layout: 'fit',
            constrainHeader: true,
            closeAction: 'destroy',

            items: [
                {
                    xtype: 'form',
                    border: false,
                    bodyPadding: 5,
                    fieldDefaults: {
                        anchor: '100%',
                        labelAlign: 'right',
                        labelWidth: 100
                    },
                    items: windowContent
                }
            ],
            buttons: [
                {
                    text: 'Rebuild',
                    handler: function() {
                        var window = this.ownerCt.ownerCt;
                        var form = window.child('form').getForm();
                        if (form.isValid()) {
                            var values = form.getFieldValues();

                            var ajaxParams = {
                                'clearCapCache': !!values['clearCapCache'],
                                'clearMestCache': !!values['clearMestCache'],
                                'jsonResponse': true
                            };
                            if (rec) {
                                ajaxParams['action'] = 'PROCESS';
                                ajaxParams['id'] = rec.get('id');
                                frameset.setSavingMessage('Rebuilding <i>' + dataSourceName + '</i>.');
                            }

                            Ext.Ajax.request({
                                url: 'dataSourcesConfig.jsp',
                                timeout: rebuildTimeout,
                                params: ajaxParams,
                                success: function(that, dataSourceId, dataSourceName) {
                                    return function(response) {
                                        var responseObj = null;
                                        var statusCode = response ? response.status : null;
                                        if (response && response.responseText) {
                                            try {
                                                responseObj = Ext.decode(response.responseText);
                                            } catch (err) {
                                                responseObj = {errors: [err.toString()]};
                                            }
                                        }
                                        if(responseObj && responseObj.success){
                                            if (responseObj.errors || responseObj.warnings) {
                                                frameset.setErrorsAndWarnings('Data source rebuild passed for <i>'+dataSourceName+'</i>', 'Warning(s) occurred while rebuilding the data source.', responseObj, statusCode);
                                            } else if (responseObj.messages) {
                                                frameset.setErrorsAndWarnings('Data source rebuild succeed for <i>'+dataSourceName+'</i>', null, responseObj, statusCode);
                                            } else {
                                                frameset.setSavedMessage('Data source rebuild successfully for <i>'+dataSourceName+'</i>.');
                                                that.showDataSourceRebuildLogWindow(frameset, dataSourceId, dataSourceName);
                                            }
                                        } else {
                                            frameset.setErrorsAndWarnings('Rebuild failed for <i>'+dataSourceName+'</i>', 'Error(s) / warning(s) occurred while rebuilding the data source.', responseObj, statusCode);
                                        }
                                        that.onReload();
                                    };
                                }(that, dataSourceId, dataSourceName),

                                failure: function(that, dataSourceName) {
                                    return function(response) {
                                        if (response.timedout) {
                                            frameset.setError('Request timed out.', 408);
                                        } else {
                                            var statusCode = response ? response.status : null;
                                            var responseObj = null;
                                            if (response && response.responseText) {
                                                try {
                                                    responseObj = Ext.decode(response.responseText);
                                                } catch (err) {
                                                    responseObj = {errors: [err.toString()]};
                                                }
                                            }
                                            frameset.setErrors('An error occurred while rebuilding the data source <i>'+dataSourceName+'</i>.', responseObj, statusCode);
                                        }
                                        that.onReload();
                                    };
                                }(that, dataSourceName)
                            });

                            window.close();
                        } else {
                            frameset.setError('Some fields contains errors.');
                        }
                    }
                }, {
                    iconCls: 'icon-cancel',
                    text: 'Cancel',
                    handler: function() {
                        var window = this.ownerCt.ownerCt;
                        window.close();
                    }
                }
            ]
        });
    },

    /**
     * Open the log window and periodically request logs to the server until the process is done.
     * @param frameset The frameset object used to handle the logs window
     * @param dataSourceId The Data Source string ID chosen by the user
     *     (this is more robust than the numerical ID since the grid may change during the build process)
     * @param dataSourceName The name of the Data Source, to be shown in the window title.
     */
    showDataSourceRebuildLogWindow: function(frameset, dataSourceId, dataSourceName) {
        frameset.setLogsRequest(
            'Rebuilding data source <i>'+dataSourceName+'</i>',
            'Rebuilding data source',
            'dataSourcesConfig.jsp',
            {
                'action': 'GETLOGS',
                'dataSourceId': dataSourceId
            },
            'dataSourcesConfig.jsp',
            {
                'action': 'STOP_PROCESS',
                'dataSourceId': dataSourceId
            },
            function(that) {
                return function(responseObj) {
                    that.onReload();
                };
            }(this)
        );
    },

    showAllDataSourceRebuildLogWindow: function(frameset) {
    },

    getDataSourceLayerTypeFormWindow: function() {
        // Create the radio buttons for each layer type
        var dataSourceLayerTypesItems = [];
        for (var layerType in dataSourceLayerTypes) {
            if (dataSourceLayerTypes.hasOwnProperty(layerType)) {
                var item = {
                    name: 'layerType',
                    boxLabel: dataSourceLayerTypes[layerType].display,
                    inputValue: layerType
                };
                // Apply the other fields to the item
                // qtipHtml, checked, style, disabled, etc.
                Ext.apply(item, dataSourceLayerTypes[layerType]);
                dataSourceLayerTypesItems.push(item);
            }
        }

        var that = this;
        return Ext.create('Ext.window.Window', {
            title: 'New data source',
            width: 300,
            layout: 'fit',
            constrainHeader: true,
            closeAction: 'destroy',

            items: [
                {
                    xtype: 'form',
                    border: false,
                    bodyPadding: 5,
                    fieldDefaults: {
                        anchor: '100%',
                        labelAlign: 'right',
                        labelWidth: 100
                    },
                    items: [
                        {
                            xtype: 'displayfield',
                            value: 'This window configure the type of the new data source. It\'s used by the application to know which protocol to use to get the layers from the server.'
                        }, {
                            xtype: 'radiogroup',
                            fieldLabel: 'Data source type',
                            columns: 1,
                            items: dataSourceLayerTypesItems
                        }
                    ]
                }
            ],
            buttons: [
                {
                    text: 'Next',
                    handler: function() {
                        var window = this.ownerCt.ownerCt;
                        var form = window.child('form').getForm();
                        if (form.isValid()) {
                            // Close the current form window and show the next one
                            var values = form.getFieldValues();
                            var newWindow = that.getLayerConfigFormWindow(values.layerType);
                            if (newWindow) {
                                // Apply default values
                                newWindow.child('.form').reset();
                                newWindow.show();
                                window.close();
                            } else {
                                frameset.setError('Unsupported data source layer type ['+layerType+']');
                            }
                        } else {
                            frameset.setError('Some fields contains errors.');
                        }
                    }
                }, {
                    iconCls: 'icon-cancel',
                    text: 'Cancel',
                    handler: function() {
                        var window = this.ownerCt.ownerCt;
                        window.close();
                    }
                }
            ]
        });
    },

    getLayerConfigFormWindow: function(layerType, rec) {
        if (!dataSourceLayerTypes[layerType]) { return null; }

        var that = this;
        var windowTitle = null;
        var buttons = [];
        if (typeof(rec) !== 'undefined') {
            var dataSourceName = rec.get('dataSourceName') + ' (' + rec.get('dataSourceId') + ')';
            windowTitle = 'Configuration of <i>'+dataSourceName+'</i>';
            buttons = [
                {
                    iconCls: 'icon-save',
                    text: 'Apply',
                    handler: function() {
                        var window = this.ownerCt.ownerCt;
                        var form = window.child('.form');
                        if (form.onSave()) {
                            window.close();
                        }
                    }
                }, {
                    iconCls: 'icon-cancel',
                    text: 'Cancel',
                    handler: function() {
                        var window = this.ownerCt.ownerCt;
                        var form = window.child('.form');
                        form.onReset();
                        window.close();
                    }
                }
            ];
        } else {
            windowTitle = 'New data source configuration';
            buttons = [
                {
                    //iconCls: 'icon-back',
                    text: 'Back',
                    handler: function() {
                        that.getDataSourceLayerTypeFormWindow().show();
                        this.ownerCt.ownerCt.close();
                    }
                }, {
                    iconCls: 'icon-add',
                    text: 'Create',
                    handler: function() {
                        var window = this.ownerCt.ownerCt;
                        var form = window.child('.form');
                        if (form.onCreate()) {
                            window.close();
                        }
                    }
                }, {
                    iconCls: 'icon-cancel',
                    text: 'Cancel',
                    handler: function() {
                        var window = this.ownerCt.ownerCt;
                        window.close();
                    }
                }
            ];
        }

        return Ext.create('Ext.window.Window', {
            title: windowTitle,
            width: 700,
            height: 450,
            maxHeight: Ext.getBody().getViewSize().height,
            layout: 'fit',
            constrainHeader: true,
            closeAction: 'hide',
            layerType: layerType,

            items: [
                {
                    xtype: 'writerlayerserverconfigform',
                    layerType: layerType,
                    listeners: {
                        scope: this,
                        create: function(form, data){
                            // Insert the data into the model.
                            // If something goes wrong, the store will
                            // manage the exception.
                            this.store.insert(0, data);
                        }
                    }
                }
            ],
            buttons: buttons
        });
    },

    onReload: function() {
        this.store.load();
    },

    onSelectChange: function(selModel, selections){
        this.down('#delete').setDisabled(selections.length === 0);
    },

    onAddClick: function(){
        this.getDataSourceLayerTypeFormWindow().show();
    },

    onDeleteClick: function() {
        var selection = this.getView().getSelectionModel().getSelection()[0];
        if (selection) {
            var dataSourceName = (selection.data ? selection.data.dataSourceName : 'UNKNOWN');
            var confirm = Ext.MessageBox.confirm(
                'Confirm',
                'The layers provided by this data source will still be available '+
                'for the clients that are using it, until those clients are re-generated.<br/>'+
                'This operation can not be undone.<br/>'+
                '<br/>'+
                'Are you sure you want to delete the data source <b>'+dataSourceName+'</b>?',
                function(btn) {
                    if (btn === 'yes') {
                        this.onDeleteConfirmed();
                    }
                },
                this
            );
            // Set "No" as default
            confirm.defaultButton = 2;
        } else {
            frameset.setError('Can not find the client to delete');
        }
    },

    onDeleteConfirmed: function() {
        frameset.setSavingMessage('Deleting a data source...');
        var selection = this.getView().getSelectionModel().getSelection()[0];
        if (selection) {
            var dataSourceName = (selection.data ? selection.data.dataSourceName : 'UNKNOWN');
            this.store.remove(selection);
            frameset.setSavedMessage('Data source <b>'+dataSourceName+'</b> deleted');
        } else {
            frameset.setError('Can not find the data source to delete');
        }
    }
});

Ext.define('Writer.LayerServerConfig', {
    extend: 'Ext.data.Model',
    // Grids records must have an unmutable ID

    // Default values:
    // Set default values in the defaultValues instance (lines 140~150).
    // Only set default value "false" for checkboxes:
    // The form do not return the value when they are unchecked,
    // forcing the model to set them to their 'model default value'.
    //
    // In other words, the default values here are the values send when
    // the field is not present in the form (which append when a checkbox
    // is unchecked).
    fields: [
        {name: 'id', type: 'int', useNull: true},
        {name: 'dataSourceId', sortType: 'asUCString'},
        {name: 'dataSourceName', sortType: 'asUCString'},
        {name: 'layerType', type: 'string'},
        {name: 'lastHarvested', type: 'string'},
        {name: 'layerCount', type: 'int'},
        'treeRoot',
        'status',
        {name: 'modified', type: 'boolean', defaultValue: false},
        'serviceUrl',
        'serviceUrls', // XYZ layers
        'getMapUrl',
        'extraWmsServiceUrls',
        'kmlData',
        {name: 'webCacheEnable', type: 'boolean', defaultValue: false},
        'webCacheCapabilitiesUrl',
        'webCacheUrl',
        'webCacheSupportedParameters',
        'featureRequestsUrl',
        'legendUrl',
        {name: 'legendDpiSupport', type: 'boolean', defaultValue: false},
        'legendParameters',
        'blackAndWhiteListedLayers',
        'googleAPIKey',
        'googleJavaScript',
        'baseLayers',
        'overlayLayers',
        'globalManualOverride',
        {name: 'activeDownload', type: 'boolean', defaultValue: false},
        {name: 'showInLegend', type: 'boolean', defaultValue: false},
        {name: 'forcePNG24', type: 'boolean', defaultValue: false},
        'ignoredArcGISPath',
        'comment',
        'bingAPIKey',
        'osm',
        'crossOriginKeyword'
    ],
    validations: [{
        field: 'dataSourceId',
        type: 'length',
        min: 1
    }, {
        field: 'dataSourceName',
        type: 'length',
        min: 1
    }, {
        field: 'layerType',
        type: 'length',
        min: 1
    }]
});

Ext.require([
    'Ext.data.*',
    'Ext.window.MessageBox'
]);


Ext.onReady(function(){
    frameset = new Frameset();
    frameset.setContentTitle('Data sources configuration');
    frameset.addContentDescription('<img src="../resources/images/json-small.jpg" style="float:right; width: 200px; height: 132px"> Each <i>data source</i> specifies a set of layers that can be used in one or more of the <i>clients</i>. It specifies the type (<i>WMS</i>, <i>ncWMS</i>, <i>Google</i>, <i>KML</i>, etc) and source of the data as well as any manual settings for the layers, such as modifying the <i>title</i>, <i>description</i> or <i>path</i> in the tree. For <i>WMS</i> data sources a <i>tile cache service</i> can be linked with a full <i>WMS service</i> so that imagery is requested from the tile server, but feature requests are sent to the full <i>WMS</i>.');
    frameset.render(document.body);

    var store = Ext.create('Ext.data.Store', {
        model: 'Writer.LayerServerConfig',
        autoLoad: true,
        autoSync: true,
        sorters: {
            property: 'dataSourceName',
            direction: 'ASC' // or 'DESC' (case sensitive for local sorting)
        },
        proxy: {
            type: 'ajax',
            api: {
                read: 'dataSourcesConfig.jsp?action=read',
                create: 'dataSourcesConfig.jsp?action=create',
                update: 'dataSourcesConfig.jsp?action=update',
                destroy: 'dataSourcesConfig.jsp?action=destroy'
            },
            reader: {
                type: 'json',
                successProperty: 'success',
                root: 'data',
                messageProperty: 'message'
            },
            writer: {
                type: 'json',
                writeAllFields: false,
                root: 'data'
            },
            listeners: {
                exception: function(proxy, response, operation){
                    var responseObj = null;
                    var statusCode = response ? response.status : null;
                    if (response && response.responseText) {
                        try {
                            responseObj = Ext.decode(response.responseText);
                        } catch (err) {
                            responseObj = {errors: [err.toString()]};
                        }
                    }
                    var operStr = 'UNKNOWN';
                    if (operation && operation.action) { operStr = operation.action; }
                    frameset.setErrors('An error occurred while executing the operation ['+operStr+'] on data sources.', responseObj, statusCode);
                }
            }
        }
    });

    var layersConfigGrid = Ext.create('Writer.LayerServerConfigGrid', {
        itemId: 'grid',
        height: 400,
        resizable: true, resizeHandles: 's',
        store: store
    });

    frameset.addContent(layersConfigGrid);
});
