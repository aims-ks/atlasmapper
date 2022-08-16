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

package au.gov.aims.atlasmapperserver.layerGenerator;

import au.gov.aims.atlasmapperserver.ConfigManager;
import au.gov.aims.atlasmapperserver.Utils;
import au.gov.aims.atlasmapperserver.cache.URLCache;
import au.gov.aims.atlasmapperserver.dataSourceConfig.WMTSDataSourceConfig;
import au.gov.aims.atlasmapperserver.layerConfig.LayerCatalog;
import au.gov.aims.atlasmapperserver.layerConfig.WMTSLayerConfig;
import au.gov.aims.atlasmapperserver.thread.RevivableThread;
import au.gov.aims.atlasmapperserver.thread.RevivableThreadInterruptedException;
import au.gov.aims.atlasmapperserver.thread.ThreadLogger;
import org.geotools.ows.wmts.model.TileMatrix;
import org.geotools.ows.wmts.model.TileMatrixSet;
import org.geotools.ows.wmts.model.TileMatrixSetLink;
import org.geotools.ows.wmts.model.WMTSCapabilities;
import org.geotools.ows.wmts.model.WMTSLayer;
import org.geotools.ows.wmts.model.WMTSRequest;
import org.geotools.referencing.CRS;

import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WMTSLayerGenerator extends AbstractWMSLayerGenerator<WMTSLayerConfig, WMTSDataSourceConfig> {
    private static final Logger LOGGER = Logger.getLogger(WMTSLayerGenerator.class.getName());

    @Override
    protected WMTSLayerConfig createLayerConfig(ConfigManager configManager) {
        return new WMTSLayerConfig(configManager);
    }

    @Override
    public LayerCatalog generateRawLayerCatalog(
            ThreadLogger logger,
            URLCache urlCache,
            WMTSDataSourceConfig dataSourceClone,
            boolean redownloadGetCapabilitiesFiles,
            boolean redownloadMestRecordFiles
    ) throws RevivableThreadInterruptedException {
        RevivableThread.checkForInterruption();

        LayerCatalog layerCatalog = new LayerCatalog();
        Map<String, WMTSLayerConfig> layersMap = null;
        URL wmtsServiceUrl = null;
        String dataSourceServiceUrlStr = dataSourceClone.getServiceUrl();
        ConfigManager configManager = dataSourceClone.getConfigManager();

        WMTSCapabilities wmtsCapabilities = null;
        try {
            wmtsCapabilities = this.getWMTSCapabilitiesResponse(
                    logger,
                    urlCache,
                    this.wmsVersion,
                    dataSourceClone,
                    dataSourceServiceUrlStr,
                    redownloadGetCapabilitiesFiles);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, String.format("Error occurred while parsing the [WMTS GetCapabilities document](%s): %s",
                    dataSourceServiceUrlStr, Utils.getExceptionMessage(ex)), ex);
            return null;
        }

        if (wmtsCapabilities == null) {
            logger.log(Level.SEVERE, String.format("Could not parse the [WMTS GetCapabilities document](%s).",
                    dataSourceServiceUrlStr));
            return null;
        }

        WMTSRequest wmtsRequestCapabilities = wmtsCapabilities.getRequest();
        if (wmtsRequestCapabilities != null) {
            if (Utils.isNotBlank(dataSourceClone.getGetMapUrl())) {
                try {
                    wmtsServiceUrl = Utils.toURL(dataSourceClone.getGetMapUrl());
                } catch (Exception ex) {
                    logger.log(Level.WARNING, String.format("Can not create a URL object from the string %s: %s",
                            dataSourceClone.getGetMapUrl(), Utils.getExceptionMessage(ex)), ex);
                }
            } else {
                wmtsServiceUrl = this.getOperationUrl(wmtsRequestCapabilities.getGetTile());
            }
            dataSourceClone.setServiceUrl(wmtsServiceUrl);

            if (Utils.isBlank(dataSourceClone.getFeatureRequestsUrl())) {
                dataSourceClone.setFeatureRequestsUrl(this.getOperationUrl(wmtsRequestCapabilities.getGetFeatureInfo()));
            }

            dataSourceClone.setWmsVersion(wmtsCapabilities.getVersion());

            // NOTE: Legend - WMTS does NOT define a getLegend operation.
        }

        layersMap = this.getLayersInfoFromCaps(logger, urlCache, wmtsCapabilities, dataSourceClone, redownloadMestRecordFiles);

        // Set default style of each layer
        if (layersMap != null && !layersMap.isEmpty()) {
            for (WMTSLayerConfig layer : layersMap.values()) {
                this.setDefaultLayerStyle(configManager, layer);
            }

            Collection<WMTSLayerConfig> layers = layersMap.values();
            layerCatalog.addLayers(layers);
        }

        RevivableThread.checkForInterruption();

        return layerCatalog;
    }

    /**
     * @param wmtsCapabilities
     * @param dataSourceClone
     * @return
     */
    protected Map<String, WMTSLayerConfig> getLayersInfoFromCaps(
            ThreadLogger logger,
            URLCache urlCache,
            WMTSCapabilities wmtsCapabilities,
            WMTSDataSourceConfig dataSourceClone, // Data source of layers (to link the layer to its data source)
            boolean forceMestDownload
    ) throws RevivableThreadInterruptedException {

        RevivableThread.checkForInterruption();

        if (wmtsCapabilities == null) {
            return null;
        }

        List<WMTSLayer> layerList = wmtsCapabilities.getLayerList();

        List<TileMatrixSet> matrixSetList = wmtsCapabilities.getMatrixSets();
        Map<String, TileMatrixSet> matrixSetMap = new HashMap<>();
        for (TileMatrixSet matrixSet : matrixSetList) {
            matrixSetMap.put(matrixSet.getIdentifier(), matrixSet);
        }

        Map<String, WMTSLayerConfig> layerConfigs = new HashMap<String, WMTSLayerConfig>();
        if (layerList != null && !layerList.isEmpty()) {
            for (WMTSLayer layer : layerList) {
                WMTSLayerConfig layerConfig = this.layerToLayerConfig(logger, urlCache, layer, null, dataSourceClone, forceMestDownload);
                if (layerConfig != null) {
                    this.addMatrices(layerConfig, layer, matrixSetMap);
                    layerConfigs.put(layerConfig.getLayerId(), layerConfig);
                }
            }
        }

        RevivableThread.checkForInterruption();

        return layerConfigs;
    }

    /**
     * Convert GeoTools layer matrices into AtlasMapper layer matrices.
     * AtlasMapper matrices are structured in a way that facilitate the
     * creation of OpenLayers WMTS layer.
     *
     * Matrices in WMTS capabilities document:
     *   <Capabilities>
     *     <Contents>
     *       <Layer>
     *         ...
     *         <TileMatrixSetLink>
     *           <TileMatrixSet>firstMatrixSetId</TileMatrixSet>
     *           <TileMatrixSetLimits>
     *             <TileMatrixLimits>
     *               <TileMatrix>firstMatrixSetId:0</TileMatrix>
     *               ...
     *             </TileMatrixLimits>
     *             <TileMatrixLimits>
     *               <TileMatrix>firstMatrixSetId:1</TileMatrix>
     *               ...
     *             </TileMatrixLimits>
     *           </TileMatrixSetLimits>
     *         </TileMatrixSetLink>
     *         <TileMatrixSetLink>
     *           <TileMatrixSet>secondMatrixSetId</TileMatrixSet>
     *           <TileMatrixSetLimits>
     *             <TileMatrixLimits>
     *               <TileMatrix>secondMatrixSetId:0</TileMatrix>
     *               ...
     *             </TileMatrixLimits>
     *             <TileMatrixLimits>
     *               <TileMatrix>secondMatrixSetId:1</TileMatrix>
     *               ...
     *             </TileMatrixLimits>
     *           </TileMatrixSetLimits>
     *         </TileMatrixSetLink>
     *         ...
     *       </Layer>
     *       <Layer> ... </Layer>
     *
     *       <TileMatrixSet>
     *         <TileMatrix>
     *           <ows:Identifier>firstMatrixSetId:0</ows:Identifier>
     *           <ows:SupportedCRS>EPSG:3857</ows:SupportedCRS>
     *           ...
     *         </TileMatrix>
     *         <TileMatrix>
     *           <ows:Identifier>firstMatrixSetId:1</ows:Identifier>
     *           <ows:SupportedCRS>EPSG:3857</ows:SupportedCRS>
     *           ...
     *         </TileMatrix>
     *         ...
     *         <TileMatrix>
     *           <ows:Identifier>secondMatrixSetId:0</ows:Identifier>
     *           <ows:SupportedCRS>EPSG:4326</ows:SupportedCRS>
     *           ...
     *         </TileMatrix>
     *         <TileMatrix>
     *           <ows:Identifier>secondMatrixSetId:1</ows:Identifier>
     *           <ows:SupportedCRS>EPSG:4326</ows:SupportedCRS>
     *           ...
     *         </TileMatrix>
     *         ...
     *       </TileMatrixSet>
     *     </Contents>
     *   </Capabilities>
     *
     * Matrices in WMTSLayerConfig (AtlasMapper)
     *   {
     *     "EPSG:3857": {
     *       "id": "firstMatrixSetId",
     *       "matrixMap": {
     *         0: "firstMatrixSetId:0",
     *         1: "firstMatrixSetId:1",
     *         ...
     *       }
     *     },
     *     "EPSG:4326": {
     *       "id": "secondMatrixSetId",
     *       "matrixMap": {
     *         0: "secondMatrixSetId:0",
     *         1: "secondMatrixSetId:1",
     *         ...
     *       }
     *     }
     *   }
     */
    private void addMatrices(WMTSLayerConfig layerConfig, WMTSLayer layer, Map<String, TileMatrixSet> matrixSetMap) {
        Map<String, TileMatrixSetLink> matrixSetLinks = layer.getTileMatrixLinks();
        for (TileMatrixSetLink matrixSetLink : matrixSetLinks.values()) {
            String matrixSetId = matrixSetLink.getIdentifier();

            TileMatrixSet matrixSet = matrixSetMap.get(matrixSetId);
            if (matrixSet != null) {
                String crs = null;

                // Try not using the fallback CRS. It's not compatible with AtlasMapper.
                // Example: urn:ogc:def:crs:EPSG::4326
                String fallbackCrs = matrixSet.getCrs();
                try {
                    Integer epsgCode = CRS.lookupEpsgCode(matrixSet.getCoordinateReferenceSystem(), false);
                    crs = String.format("EPSG:%d", epsgCode);
                } catch(Exception ex) {
                    LOGGER.log(Level.WARNING, String.format("Exception occurred while looking up the EPSG code for %s", fallbackCrs), ex);
                    crs = fallbackCrs;
                }

                if (crs != null) {
                    List<TileMatrix> tileMatrixList = matrixSet.getMatrices();
                    if (tileMatrixList != null && !tileMatrixList.isEmpty()) {
                        WMTSLayerConfig.MatrixSet configMatrixSet = new WMTSLayerConfig.MatrixSet(matrixSetId);
                        for (TileMatrix tileMatrix : tileMatrixList) {
                            // The matrixId is composed of the matrixSetId and the zoom level (GeoServer)
                            // matrixId = matrixSetId:zoomLevel
                            //     - EPSG:4326:4
                            //     - WebMercatorQuad:8
                            // Sometimes, it's only the zoom level (that seems to be the standard judging on how OpenLayers handles it)
                            //     - 4
                            //     - 8
                            String matrixId = tileMatrix.getIdentifier();
                            String zoomLevelStr = matrixId.substring(matrixId.lastIndexOf(":") + 1);
                            int zoomLevel = Integer.parseInt(zoomLevelStr);

                            configMatrixSet.addMatrix(zoomLevel, matrixId);
                        }
                        layerConfig.addMatrixSet(crs, configMatrixSet);
                    }
                }
            }
        }
    }
}
