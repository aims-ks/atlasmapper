/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2012 Australian Institute of Marine Science
 *
 *  Contact: Gael Lafond <g.lafond@aims.org.au>
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

package au.gov.aims.atlasmapperserver.layerConfig;

import au.gov.aims.atlasmapperserver.ConfigManager;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

/**
 * Class used to hold a list of all layers for a client / data source, before and after manual overrides.
 */
public class LayerCatalog {
	private static final Logger LOGGER = Logger.getLogger(LayerCatalog.class.getName());

	private SortedSet<AbstractLayerConfig> layers;

	/*
	public LayerCatalog() {
		// Instantiate a TreeSet, using a comparator that sort layers in alphabetical order.
		this.layers = new TreeSet<AbstractLayerConfig>(new Comparator<AbstractLayerConfig>() {
			@Override
			public int compare(AbstractLayerConfig layer1, AbstractLayerConfig layer2) {
				// Same instance or both null
				if (layer1 == layer2) { return 0; }

				// Place null at the end of the list
				if (layer1 == null) { return -1; }
				if (layer2 == null) { return 1; }

				// Order according to the layer title, or name (ID defined by the server) if layer name are identical
				String layerTitle1 = (layer1.getTitle() == null ? layer1.getLayerName() : layer1.getTitle());
				String layerTitle2 = (layer2.getTitle() == null ? layer2.getLayerName() : layer2.getTitle());

				// Compare the Title, so the layer will appear in alphabetic order
				int cmp = layerTitle1.compareToIgnoreCase(layerTitle2);

				// If both layers has the same spelling, ignoring case => compare them considering the case
				if (cmp == 0) {
					cmp = layerTitle1.compareTo(layerTitle2);
				}

				// If both layers has the same spelling, considering case => compare globally unique IDs (IDs unique for the whole application, not just unique for the service)
				if (cmp == 0) {
					String layerId1 = layer1.getLayerId();
					String layerId2 = layer2.getLayerId();

					if (layerId1 == layerId2) {
						// Same String instance or both null (this should only happen when the 2 instances represent the same layer)
						cmp = 0;
					} else if (layerId1 == null) {
						// Null at the end of the list (this should never happen, all layers has a unique ID)
						cmp = -1;
					} else if (layerId2 == null) {
						// Null at the end of the list (this should never happen, all layers has a unique ID)
						cmp = 1;
					} else {
						// Compare ID, considering case to minimise the probability of clashes
						cmp = layerId1.compareTo(layerId2);
					}
				}

				return cmp;
			}
		});
	}
	*/

	public LayerCatalog() {
		// Instantiate a TreeSet, using a comparator that sort layers in alphabetical order.
		this.layers = new TreeSet<AbstractLayerConfig>(new Comparator<AbstractLayerConfig>() {
			@Override
			public int compare(AbstractLayerConfig layer1, AbstractLayerConfig layer2) {
				// Same instance or both null
				if (layer1 == layer2) { return 0; }

				// Place null at the end of the list
				if (layer1 == null) { return -1; }
				if (layer2 == null) { return 1; }

				String layerId1 = layer1.getLayerId();
				String layerId2 = layer2.getLayerId();

				// Same String instance or both null (this should only happen when the 2 instances represent the same layer)
				if (layerId1 == layerId2) { return 0; }

				// Null at the end of the list (this should never happen, all layers has a unique ID)
				if (layerId1 == null) { return -1; }
				if (layerId2 == null) { return 1; }

				// Compare ID, considering case to minimise the probability of clashes
				return layerId1.compareTo(layerId2);
			}
		});
	}

	public boolean isEmpty() {
		return this.layers.isEmpty();
	}

	/*
	public void addLayer(AbstractLayerConfig layer) {
		this.layers.add(layer);
	}
	*/

	public void addLayers(Collection<? extends AbstractLayerConfig> layers) {
		if (layers != null && !layers.isEmpty()) {
			this.layers.addAll(layers);
		}
	}

	public List<AbstractLayerConfig> getLayers() {
		return new ArrayList(this.layers);
	}

	// Helper
	public static AbstractLayerConfig createLayer(String dataSourceType, JSONObject layerConfigJSON, ConfigManager configManager) throws JSONException {
		if (dataSourceType == null) {
			// Unsupported
			throw new IllegalArgumentException("No data source type provided:\n" + layerConfigJSON.toString(4));
		}

		AbstractLayerConfig layerConfig = null;
		if ("ARCGIS_MAPSERVER".equals(dataSourceType)) {
			layerConfig = new ArcGISMapServerLayerConfig(configManager);
		} else if ("GOOGLE".equals(dataSourceType)) {
			layerConfig = new GoogleLayerConfig(configManager);
		} else if ("BING".equals(dataSourceType)) {
			layerConfig = new BingLayerConfig(configManager);
		} else if ("KML".equals(dataSourceType)) {
			layerConfig = new KMLLayerConfig(configManager);
		} else if ("NCWMS".equals(dataSourceType)) {
			layerConfig = new NcWMSLayerConfig(configManager);
		} else if ("TILES".equals(dataSourceType)) {
			layerConfig = new TilesLayerConfig(configManager);
		} else if ("XYZ".equals(dataSourceType)) {
			layerConfig = new XYZLayerConfig(configManager);
		} else if ("WMS".equals(dataSourceType)) {
			layerConfig = new WMSLayerConfig(configManager);
		} else if ("GROUP".equals(dataSourceType) || "SERVICE".equals(dataSourceType)) {
			layerConfig = new GroupLayerConfig(configManager);
		} else {
			// Unsupported
			throw new IllegalArgumentException("Unsupported data source type [" + dataSourceType + "]");
		}

		// Set all data source values into the data source bean
		if (layerConfig != null) {
			layerConfig.update(layerConfigJSON);
		}

		return layerConfig;
	}
}
