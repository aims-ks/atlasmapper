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
import au.gov.aims.atlasmapperserver.ClientConfig;
import au.gov.aims.atlasmapperserver.ConfigManager;
import au.gov.aims.atlasmapperserver.Errors;
import au.gov.aims.atlasmapperserver.URLCache;
import au.gov.aims.atlasmapperserver.collection.BlackAndWhiteListFilter;
import au.gov.aims.atlasmapperserver.Utils;
import au.gov.aims.atlasmapperserver.annotation.ConfigField;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import au.gov.aims.atlasmapperserver.layerConfig.AbstractLayerConfig;
import au.gov.aims.atlasmapperserver.layerConfig.LayerCatalog;
import au.gov.aims.atlasmapperserver.layerGenerator.AbstractLayerGenerator;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONSortedObject;

/**
 *
 * @author glafond
 */
public abstract class AbstractDataSourceConfig extends AbstractConfig implements AbstractDataSourceConfigInterface, Comparable<AbstractDataSourceConfig>, Cloneable {
	private static final Logger LOGGER = Logger.getLogger(AbstractDataSourceConfig.class.getName());

	// Date in big endian format, so the alphabetic order is chronological: 2013 / 10 / 30 - 23:31
	private static final SimpleDateFormat DATE_FORMATER = new SimpleDateFormat("yyyy / MM / dd - HH:mm");

	// Grids records must have an unmutable ID
	@ConfigField
	private Integer id;

	@ConfigField
	private String dataSourceId;

	@ConfigField
	private String dataSourceName;

	@ConfigField
	private String dataSourceType;

	@ConfigField
	private String lastHarvested;

	@ConfigField
	private boolean valid;

	@ConfigField
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
	private Boolean cachingDisabled;

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

	public Map<String, Errors> process() throws Exception {
		LayerCatalog layerCatalog = this.getLayerCatalog(true);

		// Collect error messages
		Map<String, LayerCatalog.LayerErrors> layerErrors = layerCatalog.getErrors();

		Map<String, Errors> errorMessages = URLCache.getDataSourceErrors(this, this.getConfigManager().getApplicationFolder());
		for (Map.Entry<String, LayerCatalog.LayerErrors> errors: layerErrors.entrySet()) {
			if (!errorMessages.containsKey(errors.getKey())) {
				errorMessages.put(errors.getKey(), errors.getValue());
			} else {
				errorMessages.get(errors.getKey()).addAll(errors.getValue());
			}
		}

		// Verify if there is error (it may contains only warnings)
		boolean hasError = false;
		for (Errors errors : errorMessages.values()) {
			if (!errors.getErrors().isEmpty()) {
				hasError = true;
				break;
			}
		}

		if (!hasError) {
			// Do not change it if the last download fail, the previous one may still be usable.
			this.setValid(true);
		}

		// Change last update date
		this.setLastHarvestedDate(new Date());
		// Write the changes to disk
		this.getConfigManager().saveServerConfig();

		return errorMessages;
	}

	// LayerCatalog - Before data source overrides
	private LayerCatalog getRawLayerCatalog(boolean harvest) throws Exception {
		LayerCatalog rawLayerCatalog = new LayerCatalog();
		AbstractLayerGenerator layerGenerator = this.getLayerGenerator();

		if (layerGenerator != null) {
			rawLayerCatalog.addLayers(layerGenerator.generateLayerConfigs(this, harvest));
			rawLayerCatalog.addCachedLayers(layerGenerator.generateCachedLayerConfigs(this, harvest));
		}
		rawLayerCatalog.addAllErrors(layerGenerator.getErrors());

		return rawLayerCatalog;
	}

	// LayerCatalog - After data source overrides

	/**
	 *
	 * @param harvest True to download the associated documents (like the capabilities documents),
	 *     false to use the cached documents.
	 * @return
	 * @throws Exception
	 */
	public LayerCatalog getLayerCatalog(boolean harvest) throws Exception {
		// LayerCatalog before overrides
		LayerCatalog rawLayerCatalog = this.getRawLayerCatalog(harvest);

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
							rawLayerCatalog.addWarning(this.getDataSourceId(), "Invalid layer override for layer id: " + layerId);
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
					rawLayerCatalog.addWarning(this.getDataSourceId(), "Deprecated layer ID used for base layers: " +
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
			rawLayerCatalog.addError(this.getDataSourceId(), "The data source contains no layer.");
		}

		// LayerCatalog after overrides
		LayerCatalog layerCatalog = new LayerCatalog();
		layerCatalog.addLayers(layersMap.values());
		layerCatalog.addCachedLayers(rawLayerCatalog.getCachedLayers());
		layerCatalog.addAllErrors(rawLayerCatalog.getErrors());

		int nbLayers = layerCatalog.getLayers().size();
		int nbCachedLayers = layerCatalog.getCachedLayers().size();

		layerCatalog.addMessage(this.getDataSourceId(), "The data source contains " + nbLayers + " layer" + (nbLayers > 1 ? "s" : "") +
				" and " + nbCachedLayers + " cached layer" + (nbCachedLayers > 1 ? "s" : ""));

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

	public String getLastHarvested() {
		if (this.lastHarvested == null || this.lastHarvested.isEmpty()) {
			return "Unknown";
		}
		return this.lastHarvested;
	}

	public void setLastHarvested(String lastHarvested) {
		this.lastHarvested = lastHarvested;
	}

	public void setLastHarvestedDate(Date lastHarvestedDate) {
		this.setLastHarvested(
				lastHarvestedDate == null ? null : DATE_FORMATER.format(lastHarvestedDate));
	}

	public boolean isValid() {
		return this.valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public String getFeatureRequestsUrl() {
		return featureRequestsUrl;
	}

	public void setFeatureRequestsUrl(String featureRequestsUrl) {
		this.featureRequestsUrl = featureRequestsUrl;
	}

	public String getServiceUrl() {
		return serviceUrl;
	}

	public void setServiceUrl(String serviceUrl) {
		this.serviceUrl = serviceUrl;
	}

	public String getLegendUrl() {
		return legendUrl;
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

	public Boolean isCachingDisabled() {
		return this.cachingDisabled;
	}

	public void setCachingDisabled(Boolean cachingDisabled) {
		this.cachingDisabled = cachingDisabled;
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

	public abstract AbstractLayerGenerator getLayerGenerator() throws Exception;

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

	// Nothing to do here
	public AbstractDataSourceConfig applyOverrides() {
		return this;
	}

	// Generate the config to be sent to the clients
	// TODO Remove clientConfig parameter!!
	public JSONObject generateDataSource(ClientConfig clientConfig) throws JSONException {
		JSONObject dataSource = AbstractDataSourceConfigInterfaceHelper.generateDataSourceInterface(this, clientConfig);

		return dataSource;
	}

	@Override
	public JSONObject toJSonObject() throws JSONException {
		JSONObject json = super.toJSonObject();
		if (this.globalManualOverride != null) {
			json.put("globalManualOverride", this.globalManualOverride.toString(4));
		}
		return json;
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
