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
import org.geotools.ows.wmts.model.WMTSCapabilities;
import org.geotools.ows.wmts.model.WMTSRequest;

import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;

public class WMTSLayerGenerator extends AbstractWMSLayerGenerator<WMTSLayerConfig, WMTSDataSourceConfig> {
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
}
