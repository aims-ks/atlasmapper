/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2018 Australian Institute of Marine Science
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
package au.gov.aims.atlasmapperserver.thread;

import au.gov.aims.atlasmapperserver.Utils;
import au.gov.aims.atlasmapperserver.cache.URLCache;
import au.gov.aims.atlasmapperserver.collection.BlackAndWhiteListFilter;
import au.gov.aims.atlasmapperserver.dataSourceConfig.AbstractDataSourceConfig;
import au.gov.aims.atlasmapperserver.jsonWrappers.client.DataSourceWrapper;
import au.gov.aims.atlasmapperserver.jsonWrappers.client.LayerWrapper;
import au.gov.aims.atlasmapperserver.layerConfig.AbstractLayerConfig;
import au.gov.aims.atlasmapperserver.layerConfig.LayerCatalog;
import au.gov.aims.atlasmapperserver.layerGenerator.AbstractLayerGenerator;
import org.json.JSONObject;
import org.json.JSONSortedObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AbstractDataSourceConfigThread extends AbstractConfigThread {
    private static final Logger LOGGER = Logger.getLogger(AbstractDataSourceConfigThread.class.getName());

    private AbstractDataSourceConfig dataSourceConfig;
    private boolean clearCapabilitiesCache;
    private boolean clearMetadataCache;

    public AbstractDataSourceConfig getDataSourceConfig() {
        return this.dataSourceConfig;
    }

    public void setDataSourceConfig(AbstractDataSourceConfig dataSourceConfig) {
        this.dataSourceConfig = dataSourceConfig;
    }

    public boolean isClearCapabilitiesCache() {
        return this.clearCapabilitiesCache;
    }

    public void setClearCapabilitiesCache(boolean clearCapabilitiesCache) {
        this.clearCapabilitiesCache = clearCapabilitiesCache;
    }

    public boolean isClearMetadataCache() {
        return this.clearMetadataCache;
    }

    public void setClearMetadataCache(boolean clearMetadataCache) {
        this.clearMetadataCache = clearMetadataCache;
    }

    @Override
    public void run() {
        ThreadLogger logger = this.getLogger();
        URLCache urlcache = new URLCache(this.dataSourceConfig.getConfigManager());

        try {
            // startDate: Used to log the elapse time
            urlcache.startRun();
            logger.log(Level.INFO, "Generating data source: " + this.dataSourceConfig.getDataSourceName());

            RevivableThread.checkForInterruption();

            DataSourceWrapper layerCatalog = null;
            try {
                // Clone the data source
                // - The rebuild process can take several minutes. Other user may modify the config
                //     during the rebuild. The clone ensure integrity of the config during the whole build.
                // - The rebuild process modify attributes of the data source config, such as service URL.
                //     This is a not the best design choice, but that was the simplest way to do it.
                //     Those modifications should not be applied to the live data source, which is
                //     another reason to justify the clone.
                // - The state of the data source is stored in the data source config clone,
                //     then saved to disk.
                AbstractDataSourceConfig dataSourceConfigClone = (AbstractDataSourceConfig)this.dataSourceConfig.clone();

                // Download / parse the capabilities doc
                // Set the layers and capabilities overrides into the clone
                layerCatalog = this.getLayerCatalog(logger, urlcache, dataSourceConfigClone, this.clearCapabilitiesCache, this.clearMetadataCache);

                if (layerCatalog != null) {
                    // Save the data source state into the generated datasource file (example: datasources/ea.json)
                    dataSourceConfigClone.save(logger, layerCatalog);

                    // Save the data source state into the server.json config file
                    int nbLayers = 0;
                    JSONObject layers = layerCatalog.getLayers();
                    if (layers != null) {
                        nbLayers = layers.length();
                    }
                    this.dataSourceConfig.setLayerCount(nbLayers);
                    this.dataSourceConfig.setModifiedForClients(true);
                    this.dataSourceConfig.getConfigManager().saveServerConfig();
                }
            } catch(Exception ex) {
                logger.log(Level.SEVERE, "An error occurred while generating the layer catalogue: " + Utils.getExceptionMessage(ex), ex);
            }

            if (layerCatalog == null) {
                // Save the data source error state into a file
                try {
                    // Save the data source state into the generated datasource file (example: datasources/ea.json)
                    this.dataSourceConfig.save(logger, null);

                    // Save the data source state into the server.json config file
                    this.dataSourceConfig.setLayerCount(0);
                    this.dataSourceConfig.setModifiedForClients(true);
                    this.dataSourceConfig.getConfigManager().saveServerConfig();
                } catch(Exception ex) {
                    logger.log(Level.SEVERE, "An error occurred while saving the data source state: " + Utils.getExceptionMessage(ex), ex);
                }
            }

            // Create the elapse time message
            long elapseTimeMs = urlcache.endRun();
            double elapseTimeSec = elapseTimeMs / 1000.0;
            double elapseTimeMin = elapseTimeSec / 60.0;

            logger.log(Level.INFO, "Generation time: " + (elapseTimeMin >= 1 ?
                    AbstractConfigThread.ELAPSE_TIME_FORMAT.format(elapseTimeMin) + " min" :
                    AbstractConfigThread.ELAPSE_TIME_FORMAT.format(elapseTimeSec) + " sec"));

        } catch (RevivableThreadInterruptedException ex) {
            logger.log(Level.SEVERE, "Data source generation cancelled by user.", ex);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error occurred while rebuilding the data source.", ex);
        }
    }

    // LayerCatalog - Before data source overrides
    private DataSourceWrapper getRawLayerCatalog(
            ThreadLogger logger,
            URLCache urlCache,
            AbstractDataSourceConfig dataSourceConfigClone,
            boolean redownloadPrimaryFiles,
            boolean redownloadSecondaryFiles
    ) throws Exception, RevivableThreadInterruptedException {

        DataSourceWrapper rawLayerCatalog = null;

        AbstractLayerGenerator layerGenerator = dataSourceConfigClone.createLayerGenerator();
        if (layerGenerator != null) {
            rawLayerCatalog = layerGenerator.generateLayerCatalog(logger, urlCache, dataSourceConfigClone, redownloadPrimaryFiles, redownloadSecondaryFiles);
        }
        RevivableThread.checkForInterruption();

        return rawLayerCatalog;
    }

    // LayerCatalog - After data source overrides

    private DataSourceWrapper getLayerCatalog(
            ThreadLogger logger,
            URLCache urlCache,
            AbstractDataSourceConfig dataSourceConfigClone,
            boolean redownloadPrimaryFiles,
            boolean redownloadSecondaryFiles
    ) throws Exception, RevivableThreadInterruptedException {

        RevivableThread.checkForInterruption();

        // LayerCatalog before overrides
        DataSourceWrapper rawLayerCatalog = this.getRawLayerCatalog(logger, urlCache, dataSourceConfigClone, redownloadPrimaryFiles, redownloadSecondaryFiles);
        if (rawLayerCatalog == null) {
            return null;
        }
        RevivableThread.checkForInterruption();

        // Map of layers, after overrides, used to create the final layer catalog
        HashMap<String, LayerWrapper> layersMap = new HashMap<String, LayerWrapper>();

        JSONSortedObject globalOverrides = dataSourceConfigClone.getGlobalManualOverride();

        RevivableThread.checkForInterruption();

        // Apply manual overrides, if needed
        if (!rawLayerCatalog.isLayerCatalogEmpty()) {
            JSONObject layers = rawLayerCatalog.getLayers();
            if (layers != null && layers.length() > 0) {
                Iterator<String> layersKeys = layers.keys();
                while (layersKeys.hasNext()) {
                    RevivableThread.checkForInterruption();

                    String rawLayerId = layersKeys.next();
                    JSONObject jsonLayer = layers.optJSONObject(rawLayerId);
                    if (jsonLayer != null) {
                        LayerWrapper layerWrapper = new LayerWrapper(jsonLayer);
                        LayerWrapper overrideLayerWrapper =
                                AbstractLayerConfig.applyGlobalOverrides(rawLayerId, layerWrapper, globalOverrides);
                        if (overrideLayerWrapper != null) {
                            layersMap.put(rawLayerId, overrideLayerWrapper);
                        }
                    }
                }
            }
        }

        RevivableThread.checkForInterruption();

        // Create manual layers defined for this data source
        if (globalOverrides != null && globalOverrides.length() > 0) {
            Iterator<String> layerIds = globalOverrides.keys();
            while (layerIds.hasNext()) {
                RevivableThread.checkForInterruption();

                String layerId = layerIds.next();
                if (!layersMap.containsKey(layerId)) {
                    JSONObject jsonLayerOverride = globalOverrides.optJSONObject(layerId);
                    if (jsonLayerOverride != null && jsonLayerOverride.length() > 0) {
                        LayerWrapper layerOverride = new LayerWrapper(jsonLayerOverride);
                        try {
                            AbstractLayerConfig manualLayer = LayerCatalog.createLayer(
                                    layerOverride.getLayerType(), layerOverride, dataSourceConfigClone.getConfigManager());

                            LayerWrapper layerWrapper = new LayerWrapper(manualLayer.toJSonObject());

                            logger.log(Level.INFO, String.format("Adding layer defined in layer overrides: %s", layerId));
                            layersMap.put(
                                    layerId,
                                    layerWrapper);
                        } catch(Exception ex) {
                            logger.log(Level.WARNING, String.format("Invalid layer override for layer id %s: %s",
                                    layerId, Utils.getExceptionMessage(ex)), ex);
                        }
                    }
                }
            }
        }

        RevivableThread.checkForInterruption();

        // Set base layer attribute
        for (Map.Entry<String, LayerWrapper> layerWrapperEntry : layersMap.entrySet()) {
            RevivableThread.checkForInterruption();

            String layerId = layerWrapperEntry.getKey();
            LayerWrapper layerWrapper = layerWrapperEntry.getValue();

            // Only set the attribute if the layer IS a base layer (i.e. the default is false)
            if (dataSourceConfigClone.isBaseLayer(layerId)) {
                layerWrapper.setIsBaseLayer(true);
            }

            // Backward compatibility for AtlasMapper client ver. 1.2
            if (dataSourceConfigClone.isDefaultAllBaseLayers()) {
                if (!dataSourceConfigClone.isBaseLayer(layerWrapper.getLayerName())) {
                    logger.log(Level.WARNING, String.format("Deprecated layer ID used for overlay layer. Layer id %s should be %s",
                            layerWrapper.getLayerName(), layerId));
                    layerWrapper.setIsBaseLayer(false);
                }
            } else {
                if (dataSourceConfigClone.isBaseLayer(layerWrapper.getLayerName())) {
                    logger.log(Level.WARNING, String.format("Deprecated layer ID used for base layer. Layer id %s should be %s",
                            layerWrapper.getLayerName(), layerId));
                    layerWrapper.setIsBaseLayer(true);
                }
            }
        }

        RevivableThread.checkForInterruption();

        // Show warning if a base layer / overlay layer is not in the layer catalog
        String[] overlayLayers = dataSourceConfigClone.getOverlayLayers();
        if (overlayLayers != null) {
            for (String layerId : overlayLayers) {
                RevivableThread.checkForInterruption();

                if (!layersMap.containsKey(layerId)) {
                    logger.log(Level.WARNING, String.format("The layer ID %s, specified in the overlay layers, could not be found in the layer catalog.",
                            layerId));
                }
            }
        }

        RevivableThread.checkForInterruption();

        String[] baseLayers = dataSourceConfigClone.getBaseLayers();
        if (baseLayers != null) {
            for (String layerId : baseLayers) {
                RevivableThread.checkForInterruption();

                if (!layersMap.containsKey(layerId)) {
                    logger.log(Level.WARNING, String.format("The layer ID %s, specified in the base layers, could not be found in the layer catalog.",
                            layerId));
                }
            }
        }

        RevivableThread.checkForInterruption();

        // Remove blacklisted layers
        BlackAndWhiteListFilter<LayerWrapper> blackAndWhiteFilter =
                new BlackAndWhiteListFilter<LayerWrapper>(dataSourceConfigClone.getBlackAndWhiteListedLayers());
        layersMap = blackAndWhiteFilter.filter(layersMap);
        RevivableThread.checkForInterruption();

        if (layersMap.isEmpty()) {
            logger.log(Level.SEVERE, "The data source contains no layer.");
        }

        // LayerCatalog after overrides
        DataSourceWrapper layerCatalog = new DataSourceWrapper();
        layerCatalog.addLayers(layersMap);

        JSONObject layers = layerCatalog.getLayers();
        int nbLayers = layers == null ? 0 : layers.length();

        // TODO Log number of cached layers (GWC)
        if (nbLayers > 1) {
            logger.log(Level.INFO, String.format("The data source contains %d layers.",
                    nbLayers));
        } else {
            logger.log(Level.INFO, String.format("The data source contains %d layer.",
                    nbLayers));
        }

        return layerCatalog;
    }
}
