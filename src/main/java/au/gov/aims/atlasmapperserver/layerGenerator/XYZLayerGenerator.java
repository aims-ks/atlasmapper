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

import au.gov.aims.atlasmapperserver.cache.URLCache;
import au.gov.aims.atlasmapperserver.dataSourceConfig.XYZDataSourceConfig;
import au.gov.aims.atlasmapperserver.layerConfig.LayerCatalog;
import au.gov.aims.atlasmapperserver.layerConfig.XYZLayerConfig;
import au.gov.aims.atlasmapperserver.thread.RevivableThread;
import au.gov.aims.atlasmapperserver.thread.RevivableThreadInterruptedException;
import au.gov.aims.atlasmapperserver.thread.ThreadLogger;

public class XYZLayerGenerator extends AbstractLayerGenerator<XYZLayerConfig, XYZDataSourceConfig> {
    /**
     * We thrust the Admin to choose Unique IDs for all it's XYZ layers. Nothing to do here.
     * @param layer
     * @param dataSourceConfig
     * @return
     */
    @Override
    protected String getUniqueLayerId(XYZLayerConfig layer, XYZDataSourceConfig dataSourceConfig)
            throws RevivableThreadInterruptedException {

        RevivableThread.checkForInterruption();

        return layer.getLayerId();
    }

    /**
     * @param dataSourceConfig
     * @return
     * NOTE: Harvest is ignored since there is nothing to harvest.
     */
    @Override
    public LayerCatalog generateRawLayerCatalog(
            ThreadLogger logger,
            URLCache urlCache,
            XYZDataSourceConfig dataSourceConfig,
            boolean redownloadPrimaryFiles,
            boolean redownloadSecondaryFiles
    ) throws RevivableThreadInterruptedException {

        RevivableThread.checkForInterruption();

        return new LayerCatalog(); // All layers are created using the layer overrides
    }
}
