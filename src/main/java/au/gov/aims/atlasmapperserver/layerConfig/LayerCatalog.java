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
import au.gov.aims.atlasmapperserver.Errors;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

/**
 * Class used to hold a list of all layers for a data source, before and after manual overrides.
 */
public class LayerCatalog {
	private static final Logger LOGGER = Logger.getLogger(LayerCatalog.class.getName());

	private SortedSet<AbstractLayerConfig> layers;
	private Errors errors;

	public LayerCatalog() {
		this.errors = new Errors();

		// Instantiate a TreeSet, using a comparator that sort layers in alphabetical order.
		this.layers = new TreeSet<AbstractLayerConfig>(new LayerComparator());
	}

	public boolean isEmpty() {
		return this.layers.isEmpty() && this.errors.isEmpty();
	}

	public void addCatalog(LayerCatalog catalog) {
		this.addLayers(catalog.getLayers());
		this.addAllErrors(catalog.getErrors());
	}

	public void addLayers(Collection<? extends AbstractLayerConfig> layers) {
		if (layers != null && !layers.isEmpty()) {
			this.layers.addAll(layers);
		}
	}

	public List<AbstractLayerConfig> getLayers() {
		return new ArrayList<AbstractLayerConfig>(this.layers);
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

	public Errors getErrors() {
		return this.errors;
	}

	public void addAllErrors(Errors errors) {
		this.errors.addAll(errors);
	}

	public void addError(String err) {
		this.getErrors().addError(err);
	}
	public void addWarning(String warn) {
		this.getErrors().addWarning(warn);
	}
	public void addMessage(String msg) {
		this.getErrors().addMessage(msg);
	}
}
