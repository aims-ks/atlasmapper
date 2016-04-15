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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import au.gov.aims.atlasmapperserver.collection.MultiKeyHashMap;
import au.gov.aims.atlasmapperserver.jsonWrappers.client.DataSourceWrapper;
import au.gov.aims.atlasmapperserver.jsonWrappers.client.LayerWrapper;
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
public abstract class AbstractDataSourceConfig extends AbstractConfig implements Comparable<AbstractDataSourceConfig>, Cloneable {
	private static final Logger LOGGER = Logger.getLogger(AbstractDataSourceConfig.class.getName());

	// Grids records must have an unmutable ID
	@ConfigField
	private Integer id;

	@ConfigField
	private String dataSourceId;

	@ConfigField
	private String dataSourceName;

	@ConfigField(alias="dataSourceType")
	private String layerType;

	// Used to be called "wmsServiceUrl", renamed to "serviceUrl" since it apply many type of layers, not just WMS.
	@ConfigField(alias="wmsServiceUrl")
	private String serviceUrl;

	@ConfigField
	private String featureRequestsUrl;

	@ConfigField
	private String legendUrl;

	// This parameter is save as text in the server config, parsed and saved as JSONObject in the client config.
	@ConfigField
	private String legendParameters;

	@ConfigField
	private String stylesUrl;

	@ConfigField
	private String blackAndWhiteListedLayers;

	@ConfigField
	private String[] baseLayers;

	@ConfigField
	private String[] overlayLayers;

	@ConfigField
	private JSONSortedObject globalManualOverride;

	@ConfigField
	private Boolean activeDownload;

	@ConfigField
	private Boolean showInLegend;

	@ConfigField
	private String comment;


	// Used to format the elapse time (always put at lease 1 digit before the dot, with maximum 2 digits after)
	private DecimalFormat elapseTimeFormat = new DecimalFormat("0.##");

	protected AbstractDataSourceConfig(ConfigManager configManager) {
		super(configManager);
	}

	public void save(DataSourceWrapper layerCatalog) throws JSONException, IOException {
		File applicationFolder = this.getConfigManager().getApplicationFolder();

		DataSourceWrapper dataSourceWrapper = new DataSourceWrapper(this.toJSonObject(true));

		JSONObject layers = layerCatalog.getLayers();
		int nbLayers = layers == null ? 0 : layers.length();
		dataSourceWrapper.setLayers(layers);

		JSONArray errors = layerCatalog.getErrors();
		int nbErrors = errors == null ? 0 : errors.length();

		String status = "OKAY";
		if (nbLayers <= 0) {
			// It do not contains any layers, there is nothing to do with it.
			status = "INVALID";
		} else if (nbErrors > 0) {
			// It contains error, but it also contains some layers so it is usable.
			status = "PASSED";
		}

		if (nbErrors > 0) {
			dataSourceWrapper.setErrors(errors);
		}

		JSONArray warnings = layerCatalog.getWarnings();
		if (warnings != null && warnings.length() > 0) {
			dataSourceWrapper.setWarnings(warnings);
		}

		JSONArray messages = layerCatalog.getMessages();
		if (messages != null && messages.length() > 0) {
			dataSourceWrapper.setMessages(messages);
		}
		dataSourceWrapper.setStatus(status);

		AbstractDataSourceConfig.write(applicationFolder, this.dataSourceId, dataSourceWrapper);
	}

	private static void write(File applicationFolder, String dataSourceId, DataSourceWrapper dataSourceWrapper) throws JSONException, IOException {
		File dataSourceCatalogFile = FileFinder.getDataSourcesCatalogFile(applicationFolder, dataSourceId);

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

	public void setModified(boolean modified) throws IOException, JSONException {
		File applicationFolder = this.getConfigManager().getApplicationFolder();
		// Load the old saved state
		DataSourceWrapper dataSourceWrapper = AbstractDataSourceConfig.load(applicationFolder, this.dataSourceId);
		// Change its status to MODIFIED
		if (dataSourceWrapper != null && modified != dataSourceWrapper.isModified()) {
			dataSourceWrapper.setModified(modified);
			// Save the old saved state with the status MODIFIED
			AbstractDataSourceConfig.write(applicationFolder, this.dataSourceId, dataSourceWrapper);
		}
	}

	public void deleteCachedState() {
		File applicationFolder = this.getConfigManager().getApplicationFolder();
		File dataSourceCatalogFile = FileFinder.getDataSourcesCatalogFile(applicationFolder, this.dataSourceId);

		if (dataSourceCatalogFile.exists()) {
			dataSourceCatalogFile.delete();
		}
	}

	public static JSONObject processAll(ConfigManager configManager, boolean redownloadBrokenFiles, boolean clearCapabilitiesCache, boolean clearMetadataCache) throws Exception {
		JSONObject errors = new JSONObject();

		MultiKeyHashMap<Integer, String, AbstractDataSourceConfig> dataSources = configManager.getDataSourceConfigs();
		AbstractDataSourceConfig dataSource = null;
		JSONObject dataSourceErrors = null;
		for (Map.Entry<Integer, AbstractDataSourceConfig> dataSourceEntry : dataSources.entrySet()) {
			dataSource = dataSourceEntry.getValue();
			dataSourceErrors = dataSource.process(redownloadBrokenFiles, clearCapabilitiesCache, clearMetadataCache);

			// Merge errors
			// Before:
			// {
			//     "errors": [errors...],
			//     "warnings": [warnings...],
			//     "messages": [messages...]
			// }
			//
			// After:
			// {
			//     "errors": { "dataSourceId": [errors...] },
			//     "warnings": { "dataSourceId": [warnings...] },
			//     "messages": { "dataSourceId": [messages...] }
			// }
			if (dataSourceErrors != null) {
				Object errorsObj = dataSourceErrors.opt("errors");
				Object warningsObj = dataSourceErrors.opt("warnings");
				Object messagesObj = dataSourceErrors.opt("messages");
				if (errorsObj != null) {
					JSONObject jsonErrors = errors.optJSONObject("errors");
					if (jsonErrors == null) {
						jsonErrors = new JSONObject();
						errors.put("errors", jsonErrors);
					}
					jsonErrors.put(dataSource.getDataSourceId(), errorsObj);
				}
				if (warningsObj != null) {
					JSONObject jsonWarnings = errors.optJSONObject("warnings");
					if (jsonWarnings == null) {
						jsonWarnings = new JSONObject();
						errors.put("warnings", jsonWarnings);
					}
					jsonWarnings.put(dataSource.getDataSourceId(), warningsObj);
				}
				if (messagesObj != null) {
					JSONObject jsonMessages = errors.optJSONObject("messages");
					if (jsonMessages == null) {
						jsonMessages = new JSONObject();
						errors.put("messages", jsonMessages);
					}
					jsonMessages.put(dataSource.getDataSourceId(), messagesObj);
				}
			}
		}

		return errors;
	}

	/**
	 * 1. Clone myself
	 * 2. Download / parse the capabilities doc
	 * 3. Set the layers and capabilities overrides into the clone
	 * 4. Save the state into a file
	 * @return
	 * @throws Exception
	 */
	public JSONObject process(boolean redownloadBrokenFiles, boolean clearCapabilitiesCache, boolean clearMetadataCache) throws Exception {
		// startDate: Used to log the elapse time
		Date startDate = new Date();

		URLCache.reloadDiskCacheMapIfNeeded(this.getConfigManager().getApplicationFolder());

		// 1. Clear the cache
		// NOTE: I could set a complex logic here to call clearCache only once, but that would not save much processing time.
		if (redownloadBrokenFiles) {
			URLCache.markCacheForReDownload(this.getConfigManager(), this, true, null);
		}
		if (clearCapabilitiesCache) {
			URLCache.markCacheForReDownload(this.getConfigManager(), this, false, URLCache.Category.CAPABILITIES_DOCUMENT);
		}
		if (clearMetadataCache) {
			URLCache.markCacheForReDownload(this.getConfigManager(), this, false, URLCache.Category.MEST_RECORD);
			URLCache.markCacheForReDownload(this.getConfigManager(), this, false, URLCache.Category.BRUTEFORCE_MEST_RECORD);
		}

		// 1. Clone myself
		AbstractDataSourceConfig clone = (AbstractDataSourceConfig) this.clone();

		// 2. Download / parse the capabilities doc
		// 3. Set the layers and capabilities overrides into the clone
		DataSourceWrapper layerCatalog = clone.getLayerCatalog(clearCapabilitiesCache, clearMetadataCache);

		// Create the elapse time message
		Date endDate = new Date();
		long elapseTimeMs = endDate.getTime() - startDate.getTime();
		double elapseTimeSec = elapseTimeMs / 1000.0;
		double elapseTimeMin = elapseTimeSec / 60.0;

		layerCatalog.addMessage("Rebuild time: " + (elapseTimeMin >= 1 ?
				this.elapseTimeFormat.format(elapseTimeMin) + " min" :
				this.elapseTimeFormat.format(elapseTimeSec) + " sec"));

		// 4. Save the data source state into a file
		clone.save(layerCatalog);


		JSONObject errors = new JSONObject();
		errors.put("errors", layerCatalog.getErrors());
		errors.put("warnings", layerCatalog.getWarnings());
		errors.put("messages", layerCatalog.getMessages());

		URLCache.saveDiskCacheMap(this.getConfigManager().getApplicationFolder());

		return errors;
	}

	// LayerCatalog - Before data source overrides
	private DataSourceWrapper getRawLayerCatalog(boolean redownloadPrimaryFiles, boolean redownloadSecondaryFiles) throws Exception {
		DataSourceWrapper rawLayerCatalog = null;

		AbstractLayerGenerator layerGenerator = this.createLayerGenerator();
		if (layerGenerator != null) {
			rawLayerCatalog = layerGenerator.generateLayerCatalog(this, redownloadPrimaryFiles, redownloadSecondaryFiles);
		}

		return rawLayerCatalog;
	}

	// LayerCatalog - After data source overrides

	public DataSourceWrapper getLayerCatalog(boolean redownloadPrimaryFiles, boolean redownloadSecondaryFiles) throws Exception {
		// LayerCatalog before overrides
		DataSourceWrapper rawLayerCatalog = this.getRawLayerCatalog(redownloadPrimaryFiles, redownloadSecondaryFiles);

		// Map of layers, after overrides, used to create the final layer catalog
		HashMap<String, LayerWrapper> layersMap = new HashMap<String, LayerWrapper>();

		JSONSortedObject globalOverrides = this.globalManualOverride;

		// Apply manual overrides, if needed
		if (!rawLayerCatalog.isLayerCatalogEmpty()) {
			JSONObject layers = rawLayerCatalog.getLayers();
			if (layers != null && layers.length() > 0) {
				Iterator<String> layersKeys = layers.keys();
				while (layersKeys.hasNext()) {
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


		// Create manual layers defined for this data source
		if (globalOverrides != null && globalOverrides.length() > 0) {
			Iterator<String> layerIds = globalOverrides.keys();
			while (layerIds.hasNext()) {
				String layerId = layerIds.next();
				if (!layersMap.containsKey(layerId)) {
					LayerWrapper jsonLayerOverride = new LayerWrapper(globalOverrides.optJSONObject(layerId));
					if (jsonLayerOverride != null && jsonLayerOverride.getJSON().length() > 0) {
						try {
							AbstractLayerConfig manualLayer = LayerCatalog.createLayer(
									jsonLayerOverride.getLayerType(), jsonLayerOverride, this.getConfigManager());

							LayerWrapper layerWrapper = new LayerWrapper(manualLayer.toJSonObject());
							layersMap.put(
									layerId,
									layerWrapper);
						} catch(Exception ex) {
							rawLayerCatalog.addWarning("Invalid layer override for layer id: " + layerId);
							LOGGER.log(Level.SEVERE, "Unexpected error occurred while parsing the following layer override for the data source [{0}], layer id [{1}]: {2}\n{3}",
									new String[]{this.getDataSourceName(), layerId, Utils.getExceptionMessage(ex), jsonLayerOverride.getJSON().toString(4)});
							LOGGER.log(Level.FINE, "Stack trace: ", ex);
						}
					}
				}
			}
		}

		// Set base layer attribute
		for (Map.Entry<String, LayerWrapper> layerWrapperEntry : layersMap.entrySet()) {
			String layerId = layerWrapperEntry.getKey();
			LayerWrapper layerWrapper = layerWrapperEntry.getValue();

			// Only set the attribute if the layer IS a base layer (i.e. the default is false)
			if (this.isBaseLayer(layerId)) {
				layerWrapper.setIsBaseLayer(true);
			}

			// Backward compatibility for AtlasMapper client ver. 1.2
			if (this.isDefaultAllBaseLayers()) {
				if (!this.isBaseLayer(layerWrapper.getLayerName())) {
					rawLayerCatalog.addWarning("Deprecated layer ID used for overlay layers: " +
							"layer id [" + layerWrapper.getLayerName() + "] should be [" + layerId + "]");
					LOGGER.log(Level.WARNING, "DEPRECATED LAYER ID USED FOR OVERLAY LAYERS: Layer id [{0}] should be [{1}].",
							new String[]{ layerWrapper.getLayerName(), layerId });
					layerWrapper.setIsBaseLayer(false);
				}
			} else {
				if (this.isBaseLayer(layerWrapper.getLayerName())) {
					rawLayerCatalog.addWarning("Deprecated layer ID used for base layers: " +
							"layer id [" + layerWrapper.getLayerName() + "] should be [" + layerId + "]");
					LOGGER.log(Level.WARNING, "DEPRECATED LAYER ID USED FOR BASE LAYERS: Layer id [{0}] should be [{1}].",
							new String[]{ layerWrapper.getLayerName(), layerId });
					layerWrapper.setIsBaseLayer(true);
				}
			}
		}

		// Show warning if a base layer / overlay layer is not in the layer catalog
		if (this.overlayLayers != null) {
			for (String layerId : this.overlayLayers) {
				if (!layersMap.containsKey(layerId)) {
					rawLayerCatalog.addWarning("The layer ID [" + layerId + "], specified in the overlay layers, could not be found in the layer catalog.");
				}
			}
		}
		if (this.baseLayers != null) {
			for (String layerId : this.baseLayers) {
				if (!layersMap.containsKey(layerId)) {
					rawLayerCatalog.addWarning("The layer ID [" + layerId + "], specified in the base layers, could not be found in the layer catalog.");
				}
			}
		}

		// Remove blacklisted layers
		BlackAndWhiteListFilter<LayerWrapper> blackAndWhiteFilter =
				new BlackAndWhiteListFilter<LayerWrapper>(this.getBlackAndWhiteListedLayers());
		layersMap = blackAndWhiteFilter.filter(layersMap);

		if (layersMap.isEmpty()) {
			rawLayerCatalog.addError("The data source contains no layer.");
		}

		// LayerCatalog after overrides
		DataSourceWrapper layerCatalog = new DataSourceWrapper();
		layerCatalog.addLayers(layersMap);
		layerCatalog.addErrors(rawLayerCatalog.getErrors());
		layerCatalog.addWarnings(rawLayerCatalog.getWarnings());
		layerCatalog.addMessages(rawLayerCatalog.getMessages());

		JSONObject layers = layerCatalog.getLayers();
		int nbLayers = layers == null ? 0 : layers.length();

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


	public String[] getBaseLayers() {
		return this.baseLayers;
	}
	public void setBaseLayers(String[] rawBaseLayers) {
		if (rawBaseLayers == null || rawBaseLayers.length <= 0) {
			this.baseLayers = null;
		} else {
			List<String> baseLayers = new ArrayList<String>(rawBaseLayers.length);
			for (String baseLayer : rawBaseLayers) {
				// When the value come from the form (or an old config file), it's a coma separated String instead of an Array
				Pattern regex = Pattern.compile(".*" + SPLIT_PATTERN + ".*", Pattern.DOTALL);
				if (regex.matcher(baseLayer).matches()) {
					for (String splitBaseLayer : baseLayer.split(SPLIT_PATTERN)) {
						baseLayers.add(splitBaseLayer.trim());
					}
				} else {
					baseLayers.add(baseLayer.trim());
				}
			}
			this.baseLayers = baseLayers.toArray(new String[baseLayers.size()]);
		}
	}

	public String[] getOverlayLayers() {
		return this.overlayLayers;
	}
	public void setOverlayLayers(String[] rawOverlayLayers) {
		if (rawOverlayLayers == null || rawOverlayLayers.length <= 0) {
			this.overlayLayers = null;
		} else {
			List<String> overlayLayers = new ArrayList<String>(rawOverlayLayers.length);
			for (String overlayLayer : rawOverlayLayers) {
				// When the value come from the form (or an old config file), it's a coma separated String instead of an Array
				Pattern regex = Pattern.compile(".*" + SPLIT_PATTERN + ".*", Pattern.DOTALL);
				if (regex.matcher(overlayLayer).matches()) {
					for (String splitOverlayLayer : overlayLayer.split(SPLIT_PATTERN)) {
						overlayLayers.add(splitOverlayLayer.trim());
					}
				} else {
					overlayLayers.add(overlayLayer.trim());
				}
			}
			this.overlayLayers = overlayLayers.toArray(new String[overlayLayers.size()]);
		}
	}


	public JSONSortedObject getGlobalManualOverride() {
		return this.globalManualOverride;
	}

	public void setGlobalManualOverride(JSONSortedObject globalManualOverride) {
		this.globalManualOverride = globalManualOverride;
	}

	public String getLayerType() {
		return this.layerType;
	}

	public void setLayerType(String layerType) {
		this.layerType = layerType;
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
			if (this.overlayLayers == null || this.overlayLayers.length <= 0) {
				return true;
			}

			return !arrayContains(this.overlayLayers, layerId);
		} else {
			if (this.baseLayers == null || this.baseLayers.length <= 0) {
				return false;
			}

			return arrayContains(this.baseLayers, layerId);
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

			// Base layers flag are set on layers at this stage. The client do not need those list.
			dataSourceWrapper.setBaseLayers((JSONArray)null);
			dataSourceWrapper.setOverlayLayers((JSONArray)null);

			// Save the legend parameters as a JSONObject (the wrapper do the conversion from String to JSON)
			dataSourceWrapper.setLegendParameters(this.getLegendParameters());
		} else {
			if (this.globalManualOverride != null) {
				dataSourceWrapper.setGlobalManualOverride(this.globalManualOverride.toString(4));
			}

			// Add lastHarvested date and the valid flag to the JSON object.
			String status = "INVALID";
			boolean modified = false;
			File applicationFolder = this.getConfigManager().getApplicationFolder();
			File dataSourceCatalogFile = FileFinder.getDataSourcesCatalogFile(applicationFolder, this.dataSourceId);
			if (dataSourceCatalogFile.exists()) {
				try {
					DataSourceWrapper dataSourceSavedState = AbstractDataSourceConfig.load(dataSourceCatalogFile);

					if (dataSourceSavedState != null) {
						status = dataSourceSavedState.getStatus();
						modified = dataSourceSavedState.isModified();

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
			dataSourceWrapper.setStatus(status);
			if (modified) {
				dataSourceWrapper.setModified(modified);
			}
		}

		return dataSourceWrapper.getJSON();
	}

	@Override
	public String toString() {
		return "AbstractDataSourceConfig {\n" +
				(id==null ? "" :                                   "	id=" + id + "\n") +
				(Utils.isBlank(dataSourceId) ? "" :                "	dataSourceId=" + dataSourceId + "\n") +
				(Utils.isBlank(dataSourceName) ? "" :              "	dataSourceName=" + dataSourceName + "\n") +
				(Utils.isBlank(layerType) ? "" :                   "	layerType=" + layerType + "\n") +
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
