package au.gov.aims.atlasmapperserver.thread;

import au.gov.aims.atlasmapperserver.Utils;
import au.gov.aims.atlasmapperserver.collection.BlackAndWhiteListFilter;
import au.gov.aims.atlasmapperserver.dataSourceConfig.AbstractDataSourceConfig;
import au.gov.aims.atlasmapperserver.jsonWrappers.client.DataSourceWrapper;
import au.gov.aims.atlasmapperserver.jsonWrappers.client.LayerWrapper;
import au.gov.aims.atlasmapperserver.layerConfig.AbstractLayerConfig;
import au.gov.aims.atlasmapperserver.layerConfig.LayerCatalog;
import au.gov.aims.atlasmapperserver.layerGenerator.AbstractLayerGenerator;
import au.gov.aims.atlasmapperserver.URLCache;
import org.json.JSONObject;
import org.json.JSONSortedObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AbstractDataSourceConfigThread extends AbstractConfigThread {
    private static final Logger LOGGER = Logger.getLogger(AbstractDataSourceConfigThread.class.getName());

    private AbstractDataSourceConfig dataSourceConfig;
    private boolean redownloadBrokenFiles;
    private boolean clearCapabilitiesCache;
    private boolean clearMetadataCache;

    public AbstractDataSourceConfig getDataSourceConfig() {
        return this.dataSourceConfig;
    }

    public void setDataSourceConfig(AbstractDataSourceConfig dataSourceConfig) {
        this.dataSourceConfig = dataSourceConfig;
    }

    public boolean isRedownloadBrokenFiles() {
        return this.redownloadBrokenFiles;
    }

    public void setRedownloadBrokenFiles(boolean redownloadBrokenFiles) {
        this.redownloadBrokenFiles = redownloadBrokenFiles;
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

        try {
            // startDate: Used to log the elapse time
            Date startDate = new Date();
            logger.log(Level.INFO, "Generating data source: " + this.dataSourceConfig.getDataSourceName());

            logger.log(Level.INFO, "Refresh disk cache");
            try {
                URLCache.reloadDiskCacheMapIfNeeded(this.dataSourceConfig.getConfigManager().getApplicationFolder());
            } catch(Exception ex) {
                logger.log(Level.SEVERE, "An error occurred while reloading the disk cache: " + Utils.getExceptionMessage(ex), ex);
            }

            RevivableThread.checkForInterruption();

            // 1. Clear the cache
            // NOTE: I could set a complex logic here to call clearCache only once, but that would not save much processing time.
            try {
                if (this.redownloadBrokenFiles) {
                    URLCache.markCacheForReDownload(this.dataSourceConfig.getConfigManager(), this.dataSourceConfig, true, null);
                }
                if (this.clearCapabilitiesCache) {
                    URLCache.markCacheForReDownload(this.dataSourceConfig.getConfigManager(), this.dataSourceConfig, false, URLCache.Category.CAPABILITIES_DOCUMENT);
                }
                if (this.clearMetadataCache) {
                    URLCache.markCacheForReDownload(this.dataSourceConfig.getConfigManager(), this.dataSourceConfig, false, URLCache.Category.MEST_RECORD);
                    URLCache.markCacheForReDownload(this.dataSourceConfig.getConfigManager(), this.dataSourceConfig, false, URLCache.Category.BRUTEFORCE_MEST_RECORD);
                }
            } catch(Exception ex) {
                logger.log(Level.SEVERE, "An error occurred while invalidating the disk cache: " + Utils.getExceptionMessage(ex), ex);
            }

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
                layerCatalog = this.getLayerCatalog(logger, dataSourceConfigClone, this.clearCapabilitiesCache, this.clearMetadataCache);

                // Save the data source state into a file
                dataSourceConfigClone.save(logger, layerCatalog);
            } catch(Exception ex) {
                logger.log(Level.SEVERE, "An error occurred while generating the layer catalogue: " + Utils.getExceptionMessage(ex), ex);
            }

            // Create the elapse time message
            Date endDate = new Date();
            long elapseTimeMs = endDate.getTime() - startDate.getTime();
            double elapseTimeSec = elapseTimeMs / 1000.0;
            double elapseTimeMin = elapseTimeSec / 60.0;

            logger.log(Level.INFO, "Generation time: " + (elapseTimeMin >= 1 ?
                    AbstractConfigThread.ELAPSE_TIME_FORMAT.format(elapseTimeMin) + " min" :
                    AbstractConfigThread.ELAPSE_TIME_FORMAT.format(elapseTimeSec) + " sec"));

            try {
                URLCache.saveDiskCacheMap(this.dataSourceConfig.getConfigManager().getApplicationFolder());
            } catch(Exception ex) {
                logger.log(Level.SEVERE, "An error occurred while saving the disk cache: " + Utils.getExceptionMessage(ex), ex);
            }
        } catch (RevivableThreadInterruptedException ex) {
            logger.log(Level.SEVERE, "Data source generation cancelled by user.", ex);
        }
    }

    // LayerCatalog - Before data source overrides
    private DataSourceWrapper getRawLayerCatalog(
            ThreadLogger logger,
            AbstractDataSourceConfig dataSourceConfigClone,
            boolean redownloadPrimaryFiles,
            boolean redownloadSecondaryFiles
    ) throws Exception, RevivableThreadInterruptedException {

        DataSourceWrapper rawLayerCatalog = null;

        AbstractLayerGenerator layerGenerator = dataSourceConfigClone.createLayerGenerator();
        if (layerGenerator != null) {
            rawLayerCatalog = layerGenerator.generateLayerCatalog(logger, dataSourceConfigClone, redownloadPrimaryFiles, redownloadSecondaryFiles);
        }
        RevivableThread.checkForInterruption();

        return rawLayerCatalog;
    }

    // LayerCatalog - After data source overrides

    private DataSourceWrapper getLayerCatalog(
            ThreadLogger logger,
            AbstractDataSourceConfig dataSourceConfigClone,
            boolean redownloadPrimaryFiles,
            boolean redownloadSecondaryFiles
    ) throws Exception, RevivableThreadInterruptedException {

        RevivableThread.checkForInterruption();

        // LayerCatalog before overrides
        DataSourceWrapper rawLayerCatalog = this.getRawLayerCatalog(logger, dataSourceConfigClone, redownloadPrimaryFiles, redownloadSecondaryFiles);
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
                    LayerWrapper layerWrapper = new LayerWrapper(layers.optJSONObject(rawLayerId));
                    if (layerWrapper != null) {
                        layersMap.put(
                                rawLayerId,
                                AbstractLayerConfig.applyGlobalOverrides(rawLayerId, layerWrapper, globalOverrides));
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
                    LayerWrapper jsonLayerOverride = new LayerWrapper(globalOverrides.optJSONObject(layerId));
                    if (jsonLayerOverride != null && jsonLayerOverride.getJSON().length() > 0) {
                        try {
                            AbstractLayerConfig manualLayer = LayerCatalog.createLayer(
                                    jsonLayerOverride.getLayerType(), jsonLayerOverride, dataSourceConfigClone.getConfigManager());

                            LayerWrapper layerWrapper = new LayerWrapper(manualLayer.toJSonObject());
                            layersMap.put(
                                    layerId,
                                    layerWrapper);
                        } catch(Exception ex) {
                            logger.log(Level.WARNING, String.format("Invalid layer override for layer id %s: %s",
                                    layerId, Utils.getExceptionMessage(ex)), ex);
                            LOGGER.log(
                                Level.SEVERE,
                                String.format("Unexpected error occurred while parsing the override for layer %s: %s%n%s",
                                        layerId, Utils.getExceptionMessage(ex), jsonLayerOverride.getJSON().toString(4)),
                                ex
                            );
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
                    LOGGER.log(Level.WARNING, String.format("DEPRECATED LAYER ID USED FOR OVERLAY LAYERS: Layer id %s should be %s",
                            layerWrapper.getLayerName(), layerId));
                    layerWrapper.setIsBaseLayer(false);
                }
            } else {
                if (dataSourceConfigClone.isBaseLayer(layerWrapper.getLayerName())) {
                    logger.log(Level.WARNING, String.format("Deprecated layer ID used for base layer. Layer id %s should be %s",
                            layerWrapper.getLayerName(), layerId));
                    LOGGER.log(Level.WARNING, String.format("DEPRECATED LAYER ID USED FOR BASE LAYERS: Layer id %s should be %s",
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

        // TODO Log number of cached layers
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