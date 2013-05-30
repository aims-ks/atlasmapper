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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

/**
 * Class used to hold a list of all layers for a client / data source, before and after manual overrides.
 */
public class LayerCatalog {
	private static final Logger LOGGER = Logger.getLogger(LayerCatalog.class.getName());

	private SortedSet<AbstractLayerConfig> layers;
	private HashMap<String, AbstractLayerConfig> cachedLayers;
	private Map<String, LayerErrors> errors;

	public LayerCatalog() {
		this.errors = new HashMap<String, LayerErrors>();

		// Instantiate a TreeSet, using a comparator that sort layers in alphabetical order.
		this.layers = new TreeSet<AbstractLayerConfig>(new LayerComparator());
		this.cachedLayers = new HashMap<String, AbstractLayerConfig>();
	}

	public boolean isEmpty() {
		return this.layers.isEmpty() && this.errors.isEmpty();
	}

	public void addCatalog(LayerCatalog catalog) {
		this.addLayers(catalog.getLayers());
		this.addCachedLayers(catalog.getCachedLayers());
		this.addAllErrors(catalog.getErrors());
	}

	public void addLayers(Collection<? extends AbstractLayerConfig> layers) {
		if (layers != null && !layers.isEmpty()) {
			this.layers.addAll(layers);
		}
	}

	public void addCachedLayers(Collection<? extends AbstractLayerConfig> layers) {
		if (layers != null && !layers.isEmpty()) {
			for (AbstractLayerConfig layer : layers) {
				this.cachedLayers.put(layer.getLayerId(), layer);
			}
		}
	}

	public List<AbstractLayerConfig> getLayers() {
		return new ArrayList<AbstractLayerConfig>(this.layers);
	}

	public List<AbstractLayerConfig> getCachedLayers() {
		return new ArrayList<AbstractLayerConfig>(this.cachedLayers.values());
	}

	public AbstractLayerConfig getCachedLayer(AbstractLayerConfig layer) {
		if (layer == null) {
			return null;
		}
		return this.cachedLayers.get(layer.getLayerId());
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

	public Map<String, LayerErrors> getErrors() {
		return this.errors;
	}
	public void addAllErrors(Map<String, LayerErrors> errors) {
		for (Map.Entry<String, LayerErrors> error : errors.entrySet()) {
			this.addAllErrors(error.getKey(), error.getValue());
		}
	}

	public void addAllErrors(String dataSourceId, LayerErrors errors) {
		for (LayerError error : errors.getErrors()) {
			this.addError(dataSourceId, error.getMsg());
		}
		for (LayerError warn : errors.getWarnings()) {
			this.addWarning(dataSourceId, warn.getMsg());
		}
		for (LayerError msg : errors.getMessages()) {
			this.addMessage(dataSourceId, msg.getMsg());
		}
	}

	public void addError(String dataSourceId, String err) {
		this.getErrors(dataSourceId).addError(err);
	}
	public void addWarning(String dataSourceId, String warn) {
		this.getErrors(dataSourceId).addWarning(warn);
	}
	public void addMessage(String dataSourceId, String msg) {
		this.getErrors(dataSourceId).addMessage(msg);
	}
	private LayerErrors getErrors(String dataSourceId) {
		LayerErrors errors = this.errors.get(dataSourceId);
		if (errors == null) {
			errors = new LayerErrors();
			this.errors.put(dataSourceId, errors);
		}

		return errors;
	}

	public static class LayerError extends Errors.Error {
		private String msg;

		public LayerError(String msg) {
			this.msg = msg;
		}

		public String getMsg() { return this.msg; }

		@Override
		public JSONObject toJSON() throws JSONException {
			JSONObject json = new JSONObject();
			JSONArray msgArray = json.optJSONArray(null);
			if (msgArray == null) {
				msgArray = new JSONArray();
				json.put("", msgArray);
			}
			msgArray.put(this.msg);
			return json;
		}

		@Override
		public boolean equals(Errors.Error error) {
			// Same instance
			if (this == error) {
				return true;
			}
			// Instance of the wrong class
			if (!(error instanceof LayerError)) {
				return false;
			}
			LayerError layerError = (LayerError)error;
			// Same msg instance or both null
			if (this.msg == layerError.msg) {
				return true;
			}
			// Only one is null
			if (this.msg == null || layerError.msg == null) {
				return false;
			}
			return this.msg.equals(layerError.msg);
		}
	}

	public static class LayerErrors extends Errors<LayerError> {
		public void addError(String err) {
			this.addError(new LayerError(err));
		}

		public void addWarning(String warn) {
			this.addWarning(new LayerError(warn));
		}

		public void addMessage(String msg) {
			this.addMessage(new LayerError(msg));
		}
	}
}
