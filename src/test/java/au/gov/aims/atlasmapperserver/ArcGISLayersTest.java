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

package au.gov.aims.atlasmapperserver;

import au.gov.aims.atlasmapperserver.cache.URLCache;
import au.gov.aims.atlasmapperserver.dataSourceConfig.ArcGISMapServerDataSourceConfig;
import au.gov.aims.atlasmapperserver.layerConfig.AbstractLayerConfig;
import au.gov.aims.atlasmapperserver.layerConfig.LayerCatalog;
import au.gov.aims.atlasmapperserver.layerGenerator.ArcGISMapServerLayerGenerator;
import au.gov.aims.atlasmapperserver.thread.RevivableThreadInterruptedException;
import au.gov.aims.atlasmapperserver.thread.ThreadLogger;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class ArcGISLayersTest {

    // GBRMPA MapServer doesn't exists anymore...
    @Test
    @Ignore
    public void testGetLayerConfigs() throws RevivableThreadInterruptedException, IOException {
        ThreadLogger logger = new ThreadLogger();
        ArcGISMapServerDataSourceConfig dataSourceConfig = new ArcGISMapServerDataSourceConfig(null);
        dataSourceConfig.setDataSourceId("gbrmpa");
        dataSourceConfig.setServiceUrl("http://www.gbrmpa.gov.au/spatial_services/gbrmpaBounds/MapServer");

        ArcGISMapServerLayerGenerator arcGISLayers = new ArcGISMapServerLayerGenerator();
        LayerCatalog layerCatalogue;
        try (URLCache urlCache = new URLCache(null)) {
            layerCatalogue = arcGISLayers.generateRawLayerCatalog(logger, urlCache, dataSourceConfig, false, false);
        }
        List<AbstractLayerConfig> layers = layerCatalogue.getLayers();

        for (AbstractLayerConfig layer : layers) {
            System.out.println(layer.toString() + "\n");
        }
    }
}
