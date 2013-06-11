/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2011 Australian Institute of Marine Science
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

package au.gov.aims.atlasmapperserver.dataSourceConfig;

import au.gov.aims.atlasmapperserver.AbstractConfig;
import au.gov.aims.atlasmapperserver.ConfigManager;
import au.gov.aims.atlasmapperserver.Errors;
import au.gov.aims.atlasmapperserver.URLCache;
import au.gov.aims.atlasmapperserver.collection.BlackAndWhiteListFilter;
import au.gov.aims.atlasmapperserver.Utils;
import au.gov.aims.atlasmapperserver.annotation.ConfigField;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import au.gov.aims.atlasmapperserver.jsonWrappers.client.DataSourceWrapper;
import au.gov.aims.atlasmapperserver.layerConfig.AbstractLayerConfig;
import au.gov.aims.atlasmapperserver.layerConfig.LayerCatalog;
import au.gov.aims.atlasmapperserver.layerGenerator.AbstractLayerGenerator;
import au.gov.aims.atlasmapperserver.servlet.FileFinder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONSortedObject;
import org.json.JSONTokener;

/**
 *
 * @author glafond
 */
public abstract class AbstractDataSourceConfig extends AbstractConfig implements AbstractDataSourceConfigInterface, Comparable<AbstractDataSourceConfig>, Cloneable {
	private static final Logger LOGGER = Logger.getLogger(AbstractDataSourceConfig.class.getName());

	// Grids records must have an unmutable ID
	@ConfigField
	private Integer id;

	@ConfigField
	private String dataSourceId;

	@ConfigField
	private String dataSourceName;

	@ConfigField
	private String dataSourceType;

	// Used to be called "wmsServiceUrl", renamed to "serviceUrl" since it apply many type of layers, not just WMS.
	@ConfigField(alias="wmsServiceUrl")
	private String serviceUrl;

	@ConfigField
	private String featureRequestsUrl;

	@ConfigField
	private String legendUrl;

	@ConfigField
	private String legendParameters;

	@ConfigField
	private String stylesUrl;

	@ConfigField
	private String blackAndWhiteListedLayers;

	@ConfigField
	private String baseLayers;
	// Cache - avoid parsing baseLayers string every times.
	private Set<String> baseLayersSet = null;

	@ConfigField
	private String overlayLayers;
	// Cache - avoid parsing overlayLayers string every times.
	private Set<String> overlayLayersSet = null;

	@ConfigField
	private JSONSortedObject globalManualOverride;

	@ConfigField
	private Boolean activeDownload;

	@ConfigField
	private Boolean showInLegend;

	@ConfigField
	private String comment;

	protected AbstractDataSourceConfig(ConfigManager configManager) {
		super(configManager);
	}

	// Called with all layers generated with generateLayerConfigs
	public AbstractLayerConfig bindLayer(AbstractLayerConfig layer) {
		if (Utils.isBlank(layer.getDataSourceId())) {
			layer.setDataSourceId(this.dataSourceId);
		}
		if (Utils.isBlank(layer.getDataSourceType())) {
			layer.setDataSourceType(this.dataSourceType);
		}
		return layer;
	}

	public void save(LayerCatalog layerCatalog) throws JSONException, IOException {
		File applicationFolder = this.getConfigManager().getApplicationFolder();
		File dataSourceCatalogFile = FileFinder.getDataSourcesCatalogFile(applicationFolder, this.dataSourceId);

		DataSourceWrapper dataSourceWrapper = new DataSourceWrapper(this.toJSonObject(true));

		JSONObject jsonLayers = new JSONObject();
		for (AbstractLayerConfig layer : layerCatalog.getLayers()) {
			jsonLayers.put(layer.getLayerId(), layer.toJSonObject());
		}
		dataSourceWrapper.setLayers(jsonLayers);
		boolean valid = true;
		if (layerCatalog.getErrors() != null) {
			JSONObject jsonErrors = layerCatalog.getErrors().toJSON();

			JSONArray errors = jsonErrors.optJSONArray("errors");
			if (errors != null && errors.length() > 0) {
				dataSourceWrapper.setErrors(errors);
				valid = false;
			}

			JSONArray warnings = jsonErrors.optJSONArray("warnings");
			if (warnings != null && warnings.length() > 0) {
				dataSourceWrapper.setWarnings(warnings);
			}

			JSONArray messages = jsonErrors.optJSONArray("messages");
			if (messages != null && messages.length() > 0) {
				dataSourceWrapper.setMessages(messages);
			}
		}
		dataSourceWrapper.setValid(valid);

		Writer writer = null;
		BufferedWriter bw = null;
		try {
			writer = new FileWriter(dataSourceCatalogFile);
			bw = new BufferedWriter(writer);
			String jsonStr = Utils.jsonToStr(dataSourceWrapper.getJSON());
			if (Utils.isNotBlank(jsonStr)) {
				bw.write(jsonStr);
			}
		} finally {
			if (bw != null) {
				try {
					bw.close();
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE, "Can not close the data source catalog buffered writer: {0}", Utils.getExceptionMessage(e));
					LOGGER.log(Level.FINE, "Stack trace:", e);
				}
			}
			if (writer != null) {
				try {
					writer.close();
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE, "Can not close the data source catalog writer: {0}", Utils.getExceptionMessage(e));
					LOGGER.log(Level.FINE, "Stack trace:", e);
				}
			}
		}
	}

	public static DataSourceWrapper load(File applicationFolder, String dataSourceId) throws FileNotFoundException, JSONException {
		return AbstractDataSourceConfig.load(FileFinder.getDataSourcesCatalogFile(applicationFolder, dataSourceId));
	}

	public static DataSourceWrapper load(File dataSourceSavedStateFile) throws FileNotFoundException, JSONException {
		if (!dataSourceSavedStateFile.exists()) {
			return null;
		}

		DataSourceWrapper dataSourceWrapper = null;
		Reader reader = null;
		try {
			reader = new FileReader(dataSourceSavedStateFile);
			dataSourceWrapper = new DataSourceWrapper(new JSONObject(new JSONTokener(reader)));
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception ex) {
					LOGGER.log(Level.SEVERE, "Can not close the data source catalog reader: {0}", Utils.getExceptionMessage(ex));
					LOGGER.log(Level.FINE, "Stack trace:", ex);
				}
			}
		}

		return dataSourceWrapper;
	}

	public void deleteCachedState() {
		File applicationFolder = this.getConfigManager().getApplicationFolder();
		File dataSourceCatalogFile = FileFinder.getDataSourcesCatalogFile(applicationFolder, this.dataSourceId);

		if (dataSourceCatalogFile.exists()) {
			dataSourceCatalogFile.delete();
		}
	}

	/**
	 * 1. Clone myself
	 * 2. Download / parse the capabilities doc
	 * 3. Set the layers and capabilities overrides into the clone
	 * 4. Save the state into a file
	 * 5*. Modify myself (change harvested date) - TODO Discover that info from the saved state file.
	 * @return
	 * @throws Exception
	 */
	public Errors process(boolean clearCapabilitiesCache, boolean clearMetadataCache) throws Exception {
		// 1. Clone myself
		AbstractDataSourceConfig clone = (AbstractDataSourceConfig) this.clone();

		// 2. Download / parse the capabilities doc
		// 3. Set the layers and capabilities overrides into the clone
		LayerCatalog layerCatalog = clone.getLayerCatalog(clearCapabilitiesCache, clearMetadataCache);

		// 4. Save the data source state into a file
		clone.save(layerCatalog);

		return layerCatalog.getErrors();
	}

	// LayerCatalog - Before data source overrides
	private LayerCatalog getRawLayerCatalog(boolean clearCapabilitiesCache, boolean clearMetadataCache) throws Exception {
		LayerCatalog rawLayerCatalog = null;

		AbstractLayerGenerator layerGenerator = this.createLayerGenerator();
		if (layerGenerator != null) {
			rawLayerCatalog = layerGenerator.generateLayerCatalog(this, clearCapabilitiesCache, clearMetadataCache);

			// TODO Do this in the layer generator
			Errors errorMessages = URLCache.getDataSourceErrors(this, this.getConfigManager().getApplicationFolder());
			rawLayerCatalog.addAllErrors(errorMessages);
		}

		return rawLayerCatalog;
	}

	// LayerCatalog - After data source overrides

	public LayerCatalog getLayerCatalog(boolean clearCapabilitiesCache, boolean clearMetadataCache) throws Exception {
		// LayerCatalog before overrides
		LayerCatalog rawLayerCatalog = this.getRawLayerCatalog(clearCapabilitiesCache, clearMetadataCache);

		// Map of layers, after overrides, used to create the final layer catalog
		HashMap<String, AbstractLayerConfig> layersMap = new HashMap<String, AbstractLayerConfig>();

		JSONSortedObject globalOverrides = this.globalManualOverride;

		// Apply manual overrides, if needed
		if (!rawLayerCatalog.isEmpty()) {
			for (AbstractLayerConfig layerConfig : rawLayerCatalog.getLayers()) {
				if (layerConfig != null) {
					AbstractLayerConfig overriddenLayerConfig =
							layerConfig.applyGlobalOverrides(globalOverrides);
					layersMap.put(
							overriddenLayerConfig.getLayerId(),
							overriddenLayerConfig);
				}
			}
		}


		// Create manual layers defined for this data source
		if (globalOverrides != null && globalOverrides.length() > 0) {
			Iterator<String> layerIds = globalOverrides.keys();
			while (layerIds.hasNext()) {
				String layerId = layerIds.next();
				if (!layersMap.containsKey(layerId)) {
					JSONObject jsonGlobalOverride = globalOverrides.optJSONObject(layerId);
					if (jsonGlobalOverride != null && jsonGlobalOverride.length() > 0) {
						try {
							AbstractLayerConfig manualLayer = LayerCatalog.createLayer(
									jsonGlobalOverride.optString("dataSourceType"), jsonGlobalOverride, this.getConfigManager());

							manualLayer.setLayerId(layerId);

							// Add data source info if omitted
							this.bindLayer(manualLayer);

							layersMap.put(
									manualLayer.getLayerId(),
									manualLayer);
						} catch(Exception ex) {
							rawLayerCatalog.addWarning("Invalid layer override for layer id: " + layerId);
							LOGGER.log(Level.SEVERE, "Unexpected error occurred while parsing the following layer override for the data source [{0}], layer id [{1}]: {2}\n{3}",
									new String[]{this.getDataSourceName(), layerId, Utils.getExceptionMessage(ex), jsonGlobalOverride.toString(4)});
							LOGGER.log(Level.FINE, "Stack trace: ", ex);
						}
					}
				}
			}
		}

		// Set base layer attribute
		for (AbstractLayerConfig layerConfig : layersMap.values()) {
			// Set Baselayer flag if the layer is defined as a base layer in the client OR the client do not define any base layers and the layer is defined as a baselayer is the global config
			if (this.isDefaultAllBaseLayers()) {
				// Only set the attribute if the layer is NOT a base layer
				if (!this.isBaseLayer(layerConfig.getLayerId())) {
					layerConfig.setIsBaseLayer(false);
				}
			} else {
				// Only set the attribute if the layer IS a base layer
				if (this.isBaseLayer(layerConfig.getLayerId())) {
					layerConfig.setIsBaseLayer(true);
				} else if (this.isBaseLayer(layerConfig.getLayerName())) {
					// Backward compatibility for AtlasMapper client ver. 1.2
					rawLayerCatalog.addWarning("Deprecated layer ID used for base layers: " +
							"layer id [" + layerConfig.getLayerName() + "] should be [" + layerConfig.getLayerId() + "]");
					LOGGER.log(Level.WARNING, "DEPRECATED LAYER ID USED FOR BASE LAYERS: Layer id [{0}] should be [{1}].",
							new String[]{ layerConfig.getLayerName(), layerConfig.getLayerId() });
					layerConfig.setIsBaseLayer(true);
				}
			}
		}

		// Remove blacklisted layers
		BlackAndWhiteListFilter<AbstractLayerConfig> blackAndWhiteFilter =
				new BlackAndWhiteListFilter<AbstractLayerConfig>(this.getBlackAndWhiteListedLayers());
		layersMap = blackAndWhiteFilter.filter(layersMap);

		if (layersMap.isEmpty()) {
			rawLayerCatalog.addError("The data source contains no layer.");
		}

		// LayerCatalog after overrides
		LayerCatalog layerCatalog = new LayerCatalog();
		layerCatalog.addLayers(layersMap.values());
		layerCatalog.addAllErrors(rawLayerCatalog.getErrors());

		int nbLayers = layerCatalog.getLayers().size();

		// TODO Add nb cached layers
		//layerCatalog.addMessage(this.getDataSourceId(), "The data source contains " + nbLayers + " layer" + (nbLayers > 1 ? "s" : "") +
		//		" and " + nbCachedLayers + " cached layer" + (nbCachedLayers > 1 ? "s" : ""));
		layerCatalog.addMessage("The data source contains " + nbLayers + " layer" + (nbLayers > 1 ? "s" : ""));

		return layerCatalog;
	}

	@Override
	public void setJSONObjectKey(String key) {
		if (Utils.isBlank(this.dataSourceId)) {
			this.dataSourceId = key;
		}
	}

	@Override
	public String getJSONObjectKey() {
		return this.dataSourceId;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getBlackAndWhiteListedLayers() {
		return this.blackAndWhiteListedLayers;
	}

	public void setBlackAndWhiteListedLayers(String blackAndWhiteListedLayers) {
		this.blackAndWhiteListedLayers = blackAndWhiteListedLayers;
	}

	public String getBaseLayers() {
		return baseLayers;
	}

	public void setBaseLayers(String baseLayers) {
		this.baseLayers = baseLayers;
		this.baseLayersSet = null;
	}

	public String getOverlayLayers() {
		return this.overlayLayers;
	}

	public void setOverlayLayers(String overlayLayers) {
		this.overlayLayers = overlayLayers;
		this.overlayLayersSet = null;
	}

	public JSONSortedObject getGlobalManualOverride() {
		return this.globalManualOverride;
	}

	public void setGlobalManualOverride(JSONSortedObject globalManualOverride) {
		this.globalManualOverride = globalManualOverride;
	}

	public String getDataSourceType() {
		return this.dataSourceType;
	}

	public void setDataSourceType(String dataSourceType) {
		this.dataSourceType = dataSourceType;
	}

	public String getFeatureRequestsUrl() {
		return this.featureRequestsUrl;
	}
	public void setFeatureRequestsUrl(URL featureRequestsUrl) {
		this.setFeatureRequestsUrl(featureRequestsUrl == null ? null : featureRequestsUrl.toString());
	}
	public void setFeatureRequestsUrl(String featureRequestsUrl) {
		this.featureRequestsUrl = featureRequestsUrl;
	}

	public String getServiceUrl() {
		return this.serviceUrl;
	}
	public void setServiceUrl(URL serviceUrl) {
		this.setServiceUrl(serviceUrl == null ? null : serviceUrl.toString());
	}
	public void setServiceUrl(String serviceUrl) {
		this.serviceUrl = serviceUrl;
	}

	public String getLegendUrl() {
		return this.legendUrl;
	}
	public void setLegendUrl(URL legendUrl) {
		this.setLegendUrl(legendUrl == null ? null : legendUrl.toString());
	}
	public void setLegendUrl(String legendUrl) {
		this.legendUrl = legendUrl;
	}

	public String getLegendParameters() {
		return this.legendParameters;
	}

	public void setLegendParameters(String legendParameters) {
		this.legendParameters = legendParameters;
	}

	public String getStylesUrl() {
		return stylesUrl;
	}

	public void setStylesUrl(String stylesUrl) {
		this.stylesUrl = stylesUrl;
	}

	public String getDataSourceId() {
		// Error protection against erroneous manual config file edition
		if (this.dataSourceId == null && this.id != null) {
			return this.id.toString();
		}
		return this.dataSourceId;
	}

	public void setDataSourceId(String dataSourceId) {
		this.dataSourceId = dataSourceId;
	}

	public String getDataSourceName() {
		return dataSourceName;
	}

	public void setDataSourceName(String dataSourceName) {
		this.dataSourceName = dataSourceName;
	}

	public Boolean isActiveDownload() {
		return this.activeDownload;
	}

	public void setActiveDownload(Boolean activeDownload) {
		this.activeDownload = activeDownload;
	}

	public Boolean isShowInLegend() {
		return showInLegend;
	}

	public void setShowInLegend(Boolean showInLegend) {
		this.showInLegend = showInLegend;
	}

	public String getComment() {
		return this.comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	// Most data source, like WMS, will have all layers as overlay and some exceptions as base layers.
	// Some data source, like XYZ, will have all layers as base layers and some exceptions as overlay (set this to true).
	public boolean isDefaultAllBaseLayers() {
		return false;
	}

	// Helper
	private boolean isBaseLayer(String layerId) {
		if (Utils.isBlank(layerId)) {
			return false;
		}

		if (this.isDefaultAllBaseLayers()) {
			String overlayLayersStr = this.getOverlayLayers();

			if (this.overlayLayersSet == null) {
				if (Utils.isNotBlank(overlayLayersStr)) {
					this.overlayLayersSet = toSet(overlayLayersStr);
				}
			}

			if (this.overlayLayersSet == null || this.overlayLayersSet.isEmpty()) {
				return true;
			}

			return !this.overlayLayersSet.contains(layerId);
		} else {
			String baseLayersStr = this.getBaseLayers();

			if (this.baseLayersSet == null) {
				if (Utils.isNotBlank(baseLayersStr)) {
					this.baseLayersSet = toSet(baseLayersStr);
				}
			}

			if (this.baseLayersSet == null || this.baseLayersSet.isEmpty()) {
				return false;
			}

			return this.baseLayersSet.contains(layerId);
		}
	}

	public abstract AbstractLayerGenerator createLayerGenerator() throws Exception;

	@Override
	// Order data sources by data source name
	public int compareTo(AbstractDataSourceConfig o) {
		// Compare memory address and both null value
		if (this == o || this.getDataSourceName() == o.getDataSourceName()) {
			return 0;
		}

		String srvName = this.getDataSourceName();
		String othName = o.getDataSourceName();
		// Move null a the end of the list. (Just in case; Null values should not append...)
		if (srvName == null) { return -1; }
		if (othName == null) { return 1; }

		return srvName.toLowerCase().compareTo(othName.toLowerCase());
	}

	// Generate the config to be display in the admin page
	@Override
	public JSONObject toJSonObject() throws JSONException {
		return this.toJSonObject(false);
	}

	// Generate the config to be display in the admin page, or saved as a data source saved state
	public JSONObject toJSonObject(boolean forSavedState) throws JSONException {
		DataSourceWrapper dataSourceWrapper = new DataSourceWrapper(super.toJSonObject());

		if (forSavedState) {
			// Remove attributes that are not needed for the saved state

			// Overrides are not needed; they have already been processed at this point
			dataSourceWrapper.setGlobalManualOverride(null);

			// Black and white list is not needed, the layers has already been filtered at this point
			dataSourceWrapper.setBlackAndWhiteListedLayers(null);

			// Comments are only useful for the admin interface.
			dataSourceWrapper.setComment(null);

			// Save the legend parameters as a JSONObject (the wrapper do the conversion from String to JSON)
			dataSourceWrapper.setLegendParameters(this.getLegendParameters());
		} else {
			if (this.globalManualOverride != null) {
				dataSourceWrapper.setGlobalManualOverride(this.globalManualOverride.toString(4));
			}

			// Add lastHarvested date and the valid flag to the JSON object.
			boolean valid = false;
			File applicationFolder = this.getConfigManager().getApplicationFolder();
			File dataSourceCatalogFile = FileFinder.getDataSourcesCatalogFile(applicationFolder, this.dataSourceId);
			if (dataSourceCatalogFile.exists()) {
				try {
					DataSourceWrapper dataSourceSavedState = AbstractDataSourceConfig.load(dataSourceCatalogFile);

					if (dataSourceSavedState != null) {
						Boolean validObj = dataSourceSavedState.getValid();
						valid = validObj != null && validObj;

						// lastModified() returns 0L if the file do not exists of an exception occurred.
						long timestamp = dataSourceCatalogFile.lastModified();
						if (timestamp > 0) {
							dataSourceWrapper.setLastHarvested(ConfigManager.DATE_FORMATER.format(timestamp));
						}
					}
				} catch (FileNotFoundException ex) {
					// This should not happen, there is already a check to see if the file exists.
					LOGGER.log(Level.FINE, "Can not load the data source [" + this.dataSourceId + "] saved state");
				}
			}
			dataSourceWrapper.setValid(valid);
		}

		return dataSourceWrapper.getJSON();
	}

	@Override
	public String toString() {
		return "AbstractDataSourceConfig {\n" +
				(id==null ? "" :                                   "	id=" + id + "\n") +
				(Utils.isBlank(dataSourceId) ? "" :                "	dataSourceId=" + dataSourceId + "\n") +
				(Utils.isBlank(dataSourceName) ? "" :              "	dataSourceName=" + dataSourceName + "\n") +
				(Utils.isBlank(dataSourceType) ? "" :              "	dataSourceType=" + dataSourceType + "\n") +
				(Utils.isBlank(serviceUrl) ? "" :                  "	serviceUrl=" + serviceUrl + "\n") +
				(Utils.isBlank(featureRequestsUrl) ? "" :          "	featureRequestsUrl=" + featureRequestsUrl + "\n") +
				(Utils.isBlank(legendUrl) ? "" :                   "	legendUrl=" + legendUrl + "\n") +
				(legendParameters==null ? "" :                     "	legendParameters=" + legendParameters + "\n") +
				(Utils.isBlank(blackAndWhiteListedLayers) ? "" :   "	blackAndWhiteListedLayers=" + blackAndWhiteListedLayers + "\n") +
				(showInLegend==null ? "" :                         "	showInLegend=" + showInLegend + "\n") +
				(Utils.isBlank(comment) ? "" :                     "	comment=" + comment + "\n") +
			'}';
	}
}
