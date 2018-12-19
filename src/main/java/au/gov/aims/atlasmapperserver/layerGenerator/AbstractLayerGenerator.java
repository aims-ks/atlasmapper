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

import au.gov.aims.atlasmapperserver.cache.CacheEntry;
import au.gov.aims.atlasmapperserver.cache.URLCache;
import au.gov.aims.atlasmapperserver.dataSourceConfig.AbstractDataSourceConfig;
import au.gov.aims.atlasmapperserver.jsonWrappers.client.DataSourceWrapper;
import au.gov.aims.atlasmapperserver.jsonWrappers.client.LayerWrapper;
import au.gov.aims.atlasmapperserver.layerConfig.AbstractLayerConfig;
import au.gov.aims.atlasmapperserver.layerConfig.LayerCatalog;
import au.gov.aims.atlasmapperserver.thread.RevivableThread;
import au.gov.aims.atlasmapperserver.thread.RevivableThreadInterruptedException;
import au.gov.aims.atlasmapperserver.thread.ThreadLogger;
import org.json.JSONException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;

public abstract class AbstractLayerGenerator<L extends AbstractLayerConfig, D extends AbstractDataSourceConfig> {
    protected long instanceTimestamp = -1;

    protected abstract String getUniqueLayerId(L layer, D dataSourceConfig) throws RevivableThreadInterruptedException;

    public DataSourceWrapper generateLayerCatalog(
            ThreadLogger logger,
            URLCache urlCache,
            D dataSourceConfigClone,
            boolean redownloadPrimaryFiles,
            boolean redownloadSecondaryFiles
    ) throws IOException, JSONException, RevivableThreadInterruptedException, SQLException, ClassNotFoundException {

        // startTimestamp: Used to delete old entries in the cache (entries that do not get
        //     access after that date are considered unused and are removed)
        long startTimestamp = CacheEntry.getCurrentTimestamp();

        LayerCatalog rawLayerCatalog = this.generateRawLayerCatalog(logger, urlCache, dataSourceConfigClone, redownloadPrimaryFiles, redownloadSecondaryFiles);
        RevivableThread.checkForInterruption();

        urlCache.cleanUp(dataSourceConfigClone.getDataSourceId(), startTimestamp);
        RevivableThread.checkForInterruption();

        logger.log(Level.INFO, "Building data source layer catalogue");
        DataSourceWrapper catalogWrapper = new DataSourceWrapper();
        for (AbstractLayerConfig layer : rawLayerCatalog.getLayers()) {
            LayerWrapper layerWrapper = new LayerWrapper(layer.toJSonObject());
            catalogWrapper.addLayer(layer.getLayerId(), layerWrapper);
        }
        RevivableThread.checkForInterruption();

        return catalogWrapper;
    }

    // TODO Maybe return a DataSourceWrapper?
    // Redownload parameters are ignored by most data sources, but some use it (like KML)
    protected abstract LayerCatalog generateRawLayerCatalog(
            ThreadLogger logger,
            URLCache urlCache,
            D dataSourceConfig,
            boolean redownloadPrimaryFiles,
            boolean redownloadSecondaryFiles) throws RevivableThreadInterruptedException;

    // The layer name used to request the layer. Usually, the layerName is
    // the same as the layerId, so this field is let blank. This attribute
    // is only used when there is a duplication of layerId.
    protected void ensureUniqueLayerId(L layer, D dataSourceConfig) throws RevivableThreadInterruptedException {
        RevivableThread.checkForInterruption();

        String uniqueLayerId =
                dataSourceConfig.getDataSourceId() + "_" +
                this.getUniqueLayerId(layer, dataSourceConfig);

        layer.setLayerName(layer.getLayerId());
        layer.setLayerId(uniqueLayerId);
    }
}
