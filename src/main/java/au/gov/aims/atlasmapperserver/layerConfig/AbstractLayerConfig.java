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

package au.gov.aims.atlasmapperserver.layerConfig;

import au.gov.aims.atlasmapperserver.AbstractConfig;
import au.gov.aims.atlasmapperserver.ConfigManager;
import au.gov.aims.atlasmapperserver.Utils;
import au.gov.aims.atlasmapperserver.annotation.ConfigField;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import au.gov.aims.atlasmapperserver.dataSourceConfig.AbstractDataSourceConfigInterface;
import au.gov.aims.atlasmapperserver.dataSourceConfig.AbstractDataSourceConfigInterfaceHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONSortedObject;

/**
 *
 * @author glafond
 */
// NOTE Layers can override any fields of it's data source's interface
public abstract class AbstractLayerConfig extends AbstractConfig implements AbstractDataSourceConfigInterface {
	private static final Logger LOGGER = Logger.getLogger(AbstractLayerConfig.class.getName());

	// Unique ID for the layer, it has to be unique between all layers used by the client.
	// The id is not a ConfigField to avoid having it in the JSon object
	// I.E. The layer ID is the ID of the object: layerId: { ... layer attributes... }
	private String layerId;

	// The layer name used to request the layer. Usually, the layerName is
	// the same as the layerId, so this field is let blank. This attribute
	// is only used when there is a duplication of layerId.
	@ConfigField
	private String layerName;

	@ConfigField
	private String[] aliasIds;

	@ConfigField
	private String title;

	@ConfigField
	private String description;

	// text, wiki, html
	@ConfigField
	private String descriptionFormat;

	@ConfigField
	private String systemDescription;

	// TODO Rename to Path
	@ConfigField
	private String wmsPath;

	@ConfigField
	private String projection;

	@ConfigField
	private Boolean shownOnlyInLayerGroup;

	// Left, Bottom, Right, Top
	@ConfigField
	private double[] layerBoundingBox;

	@ConfigField
	private String[] infoHtmlUrls;

	@ConfigField
	private Boolean isBaseLayer;

	@ConfigField
	private Boolean hasLegend;

	@ConfigField
	private String legendGroup;

	@ConfigField
	private String legendTitle;

	@ConfigField
	private List<LayerStyleConfig> styles;

	@ConfigField
	// Set this to false if the layer should never be requested to the cache (GWC)
	private Boolean cached;

	@ConfigField
	private List<LayerOptionConfig> options;

	@ConfigField
	private JSONObject olParams;

	@ConfigField
	private JSONObject olOptions;


	// TODO remove after implementing Save State
	@ConfigField
	private Boolean selected;


	// DataSource fields
	@ConfigField
	private String dataSourceId;

	@ConfigField
	private String dataSourceName;

	@ConfigField
	private String dataSourceType;

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
	private String baseLayers;

	@ConfigField
	private JSONSortedObject globalManualOverride;

	@ConfigField
	private Boolean showInLegend;

	@ConfigField
	private String comment;




	public AbstractLayerConfig(ConfigManager configManager) {
		super(configManager);
	}

	@Override
	public void setJSONObjectKey(String key) {
		if (Utils.isBlank(this.layerId)) {
			this.layerId = key;
		}
	}

	@Override
	public String getJSONObjectKey() {
		return this.layerId;
	}

	@Override
	public String getBaseLayers() {
		return this.baseLayers;
	}

	@Override
	public void setBaseLayers(String baseLayers) {
		this.baseLayers = baseLayers;
	}

	@Override
	public JSONSortedObject getGlobalManualOverride() {
		return this.globalManualOverride;
	}

	@Override
	public void setGlobalManualOverride(JSONSortedObject globalManualOverride) {
		this.globalManualOverride = globalManualOverride;
	}

	@Override
	public String getDataSourceType() {
		return this.dataSourceType;
	}

	@Override
	public void setDataSourceType(String dataSourceType) {
		this.dataSourceType = dataSourceType;
	}

	@Override
	public String getFeatureRequestsUrl() {
		return this.featureRequestsUrl;
	}

	@Override
	public void setFeatureRequestsUrl(String featureRequestsUrl) {
		this.featureRequestsUrl = featureRequestsUrl;
	}

	@Override
	public String getServiceUrl() {
		return this.serviceUrl;
	}

	@Override
	public void setServiceUrl(String serviceUrl) {
		this.serviceUrl = serviceUrl;
	}

	@Override
	public String getLegendUrl() {
		return this.legendUrl;
	}

	@Override
	public void setLegendUrl(String legendUrl) {
		this.legendUrl = legendUrl;
	}

	@Override
	public String getStylesUrl() {
		return this.legendUrl;
	}

	@Override
	public void setStylesUrl(String legendUrl) {
		this.legendUrl = legendUrl;
	}

	@Override
	public String getLegendParameters() {
		return this.legendParameters;
	}

	@Override
	public void setLegendParameters(String legendParameters) {
		this.legendParameters = legendParameters;
	}

	@Override
	public String getDataSourceId() {
		return this.dataSourceId;
	}

	@Override
	public void setDataSourceId(String dataSourceId) {
		this.dataSourceId = dataSourceId;
	}

	@Override
	public String getDataSourceName() {
		return this.dataSourceName;
	}

	@Override
	public void setDataSourceName(String dataSourceName) {
		this.dataSourceName = dataSourceName;
	}

	@Override
	public Boolean isShowInLegend() {
		return this.showInLegend;
	}

	@Override
	public void setShowInLegend(Boolean showInLegend) {
		this.showInLegend = showInLegend;
	}

	@Override
	public String getComment() {
		return this.comment;
	}

	@Override
	public void setComment(String comment) {
		this.comment = comment;
	}




	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescriptionFormat() {
		return this.descriptionFormat;
	}

	public void setDescriptionFormat(String descriptionFormat) {
		this.descriptionFormat = descriptionFormat;
	}

	public String getSystemDescription() {
		return this.systemDescription;
	}

	public void setSystemDescription(String systemDescription) {
		this.systemDescription = systemDescription;
	}

	public String getWmsPath() {
		return wmsPath;
	}

	public void setWmsPath(String wmsPath) {
		this.wmsPath = wmsPath;
	}

	public String getProjection() {
		return projection;
	}

	public void setProjection(String projection) {
		this.projection = projection;
	}

	public Boolean isHasLegend() {
		return hasLegend;
	}

	public void setHasLegend(Boolean hasLegend) {
		this.hasLegend = hasLegend;
	}

	public String getLegendGroup() {
		return legendGroup;
	}

	public void setLegendGroup(String legendGroup) {
		this.legendGroup = legendGroup;
	}

	public String getLegendTitle() {
		return legendTitle;
	}

	public void setLegendTitle(String legendTitle) {
		this.legendTitle = legendTitle;
	}

	public String getLayerId() {
		return this.layerId;
	}

	public void setLayerId(String layerId) {
		this.layerId = layerId;
	}

	public String getLayerName() {
		return this.layerName;
	}

	public void setLayerName(String layerName) {
		this.layerName = layerName;
	}

	public String[] getAliasIds() {
		return aliasIds;
	}

	public void setAliasIds(String[] aliasIds) {
		this.aliasIds = aliasIds;
	}

	public Boolean isIsBaseLayer() {
		return isBaseLayer;
	}

	public void setIsBaseLayer(Boolean isBaseLayer) {
		this.isBaseLayer = isBaseLayer;
	}

	public Boolean isShownOnlyInLayerGroup() {
		return this.shownOnlyInLayerGroup;
	}

	public void setShownOnlyInLayerGroup(Boolean shownOnlyInLayerGroup) {
		this.shownOnlyInLayerGroup = shownOnlyInLayerGroup;
	}

	public double[] getLayerBoundingBox() {
		return layerBoundingBox;
	}

	public void setLayerBoundingBox(double[] layerBoundingBox) {
		this.layerBoundingBox = layerBoundingBox;
	}

	public String[] getInfoHtmlUrls() {
		return infoHtmlUrls;
	}

	public void setInfoHtmlUrls(String[] infoHtmlUrls) {
		this.infoHtmlUrls = infoHtmlUrls;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public List<LayerStyleConfig> getStyles() {
		return styles;
	}

	public void setStyles(List<LayerStyleConfig> styles) {
		this.styles = styles;
	}

	public Boolean isCached() {
		return this.cached;
	}

	public void setCached(Boolean cached) {
		this.cached = cached;
	}

	public List<LayerOptionConfig> getOptions() {
		return this.options;
	}

	public void setOptions(List<LayerOptionConfig> options) {
		this.options = options;
	}

	public JSONObject getOlParams() {
		return this.olParams;
	}

	public void setOlParams(JSONObject olParams) {
		this.olParams = olParams;
	}

	public JSONObject getOlOptions() {
		return this.olOptions;
	}

	public void setOlOptions(JSONObject olOptions) {
		this.olOptions = olOptions;
	}

	public Boolean isSelected() {
		return selected;
	}

	public void setSelected(Boolean selected) {
		this.selected = selected;
	}

	public AbstractLayerConfig applyGlobalOverrides(
			JSONObject globalOverrides) throws JSONException {

		// Create an AbstractLayerConfig from the layer override
		AbstractLayerConfig layerGlobalOverride = null;
		if (globalOverrides != null && globalOverrides.length() > 0) {
			JSONObject globalOverride = globalOverrides.optJSONObject(this.layerId);

			return this.applyOverrides(globalOverride);
		}

		return this;
	}

	public AbstractLayerConfig applyOverrides(
			JSONObject layerOverrides) throws JSONException {

		// Create an AbstractLayerConfig from the layer override
		AbstractLayerConfig layerOverride = null;
		if (layerOverrides != null && layerOverrides.length() > 0) {
			try {
				String dataSourceType = layerOverrides.optString("dataSourceType", this.getDataSourceType());
				if (dataSourceType == null) {
					dataSourceType = this.getDataSourceType();
				}

				layerOverride = LayerCatalog.createLayer(dataSourceType, layerOverrides, this.getConfigManager());
			} catch(Exception ex) {
				LOGGER.log(Level.SEVERE, "Unexpected error occurred while parsing the following layer override for the data source ["+this.getDataSourceName()+"]:\n" + layerOverrides.toString(4), ex);
			}
		}

		// Apply the layer override on a clone of the current layer (do not modify the original layer)
		AbstractLayerConfig clone = (AbstractLayerConfig)this.clone();
		clone.applyOverrides(layerOverride);

		return clone;
	}

	// TODO Specific pieces into specific classes (example: WMSLayerConfig parts goes into WMSLayerConfig class)
	public JSONObject generateLayer() throws JSONException {
		// AbstractLayerConfig implements AbstractDataSourceConfigInterface
		JSONObject jsonLayer = AbstractDataSourceConfigInterfaceHelper.generateDataSourceInterface(this, null);

		if (this.isCached() != null) {
			jsonLayer.put("cached", this.isCached());
		}

		if (this.getOlParams() != null) {
			jsonLayer.put("olParams", this.getOlParams());
		}
		if (this.getOlOptions() != null) {
			jsonLayer.put("olOptions", this.getOlOptions());
		}

		jsonLayer.put(ConfigManager.CONFIG_VERSION_KEY, ConfigManager.CURRENT_LAYER_CONFIG_VERSION);

		String layerName = this.getLayerName();
		if (Utils.isNotBlank(layerName) && !layerName.equals(this.getLayerId())) {
			jsonLayer.put("layerName", layerName.trim());
		}

		if (Utils.isNotBlank(this.getTitle())) {
			jsonLayer.put("title", this.getTitle().trim());
		}

		if (Utils.isNotBlank(this.getDescription())) {
			jsonLayer.put("description", this.getDescription().trim());
		}
		if (Utils.isNotBlank(this.getDescriptionFormat())) {
			jsonLayer.put("descriptionFormat", this.getDescriptionFormat().trim());
		}
		if (Utils.isNotBlank(this.getSystemDescription())) {
			jsonLayer.put("systemDescription", this.getSystemDescription().trim());
		}

		if (Utils.isNotBlank(this.getProjection())) {
			jsonLayer.put("projection", this.getProjection().trim());
		}

		// serverId
		if (Utils.isNotBlank(this.getDataSourceId())) {
			jsonLayer.put("dataSourceId", this.getDataSourceId().trim());
		}

		double[] boundingBox = this.getLayerBoundingBox();
		if (boundingBox != null && boundingBox.length > 0) {
			jsonLayer.put("layerBoundingBox", boundingBox);
		}

		if (this.isIsBaseLayer() != null) {
			jsonLayer.put("isBaseLayer", this.isIsBaseLayer());
		}

		if (this.isHasLegend() != null) {
			jsonLayer.put("hasLegend", this.isHasLegend());
		}

		// No need for a legend URL + Filename since there is no more Layer Groups
		if (Utils.isNotBlank(this.getLegendUrl())) {
			jsonLayer.put("legendUrl", this.getLegendUrl().trim());
		}

		if (Utils.isNotBlank(this.getStylesUrl())) {
			jsonLayer.put("stylesUrl", this.getStylesUrl().trim());
		}

		if(Utils.isNotBlank(this.getLegendGroup())) {
			jsonLayer.put("legendGroup", this.getLegendGroup().trim());
		}

		if(Utils.isNotBlank(this.getLegendTitle())) {
			jsonLayer.put("legendTitle", this.getLegendTitle().trim());
		}

		String[] infoHtmlUrls = this.getInfoHtmlUrls();
		if(infoHtmlUrls != null && infoHtmlUrls.length > 0) {
			jsonLayer.put("infoHtmlUrls", infoHtmlUrls);
		}

		String[] aliasIds = this.getAliasIds();
		if (aliasIds != null && aliasIds.length > 0) {
			jsonLayer.put("aliasIds", aliasIds);
		}

		List<LayerStyleConfig> styles = this.getStyles();
		// Browsers do not have to keep the order in JavaScript objects, but they often do.
		if (styles != null && !styles.isEmpty()) {
			JSONObject jsonStyles = new JSONObject();
			if (!styles.isEmpty()) {
				boolean firstStyle = true;
				for (LayerStyleConfig style : styles) {
					String styleName = style.getName();
					if (styleName != null) {
						if (firstStyle) {
							firstStyle = false;
							styleName = "";
						}

						JSONObject jsonStyle = this.generateLayerStyle(style);
						if (jsonStyle != null && jsonStyle.length() > 0) {
							jsonStyles.put(styleName.trim(), jsonStyle);
						}
					}
				}
			}
			if (jsonStyles.length() > 0) {
				jsonLayer.put("styles", jsonStyles);
			}
		}

		List<LayerOptionConfig> options = this.getOptions();
		if (options != null && !options.isEmpty()) {
			JSONArray optionsArray = new JSONArray();

			for (LayerOptionConfig option : options) {
				JSONObject jsonOption = this.generateLayerOption(option);
				if (jsonOption != null && jsonOption.length() > 0) {
					optionsArray.put(jsonOption);
				}
			}

			if (optionsArray.length() > 0) {
				jsonLayer.put("layerOptions", optionsArray);
			}
		}

		// Initial state is related to the Client saved state
		// TODO delete after implementing Save State
		if(this.isSelected() != null) {
			jsonLayer.put("selected", this.isSelected());
		}

		return jsonLayer;
	}

	private JSONObject generateLayerStyle(LayerStyleConfig style) throws JSONException {
		if (style == null) {
			return null;
		}

		JSONObject jsonStyle = new JSONObject();

		// The title fallback to the style name, which should never be null, so no
		// style should have the title "Untitled" (this is just an extra security).
		String title = "Untitled";
		if (Utils.isNotBlank(style.getTitle())) {
			title = style.getTitle().trim();
		} else if (Utils.isNotBlank(style.getName())) {
			title = style.getName().trim();
		}
		jsonStyle.put("title", title);

		if (style.isDefault() != null) {
			jsonStyle.put("default", style.isDefault());
		}

		if (Utils.isNotBlank(style.getDescription())) {
			jsonStyle.put("description", style.getDescription().trim());
		}

		//if (Utils.isNotBlank(style.getLegendUrl())) {
		//	jsonStyle.put("legendUrl", style.getLegendUrl().trim());
		//}

		//if (Utils.isNotBlank(style.getLegendFilename())) {
		//	jsonStyle.put("legendFilename", style.getLegendFilename().trim());
		//}

		return jsonStyle;
	}

	/**
	 * "layerOptions": [
	 *     {
	 *         "name": "String, mandatory: name of the url parameter",
	 *         "title": "String, optional (default: name): title displayed in the layer options",
	 *         "type": "String, optional (default: text): type of the parameter, to specify which UI to use",
	 *         "mandatory": "Boolean, optional (default: false): set to true is the field is not allow to contains an empty string"
	 *         "defaultValue": "String, optional (default: empty string): default value"
	 *     },
	 *     ...
	 * ]
	 */
	private JSONObject generateLayerOption(LayerOptionConfig option) throws JSONException {
		if (option == null) {
			return null;
		}

		JSONObject jsonOption = new JSONObject();
		if (Utils.isNotBlank(option.getName())) {
			jsonOption.put("name", option.getName().trim());
		}

		if (Utils.isNotBlank(option.getTitle())) {
			jsonOption.put("title", option.getTitle().trim());
		}

		if (Utils.isNotBlank(option.getType())) {
			jsonOption.put("type", option.getType().trim());
		}

		if (option.isMandatory() != null) {
			jsonOption.put("mandatory", option.isMandatory());
		}

		if (Utils.isNotBlank(option.getDefaultValue())) {
			jsonOption.put("defaultValue", option.getDefaultValue().trim());
		}

		return jsonOption;
	}

	@Override
	public String toString() {
		return "AbstractLayerConfig {\n" +
				(Utils.isBlank(layerId) ? "" :         "	layerId=" + layerId + "\n") +
				(Utils.isBlank(layerName) ? "" :       "	layerName=" + layerName + "\n") +
				(aliasIds==null ? "" :                 "	aliasIds=" + Arrays.toString(aliasIds) + "\n") +
				(Utils.isBlank(title) ? "" :           "	title=" + title + "\n") +
				(Utils.isBlank(description) ? "" :     "	description=" + description + "\n") +
				(Utils.isBlank(wmsPath) ? "" :         "	wmsPath=" + wmsPath + "\n") +
				(layerBoundingBox==null ? "" :         "	layerBoundingBox=" + Arrays.toString(layerBoundingBox) + "\n") +
				(infoHtmlUrls==null ? "" :             "	infoHtmlUrls=" + Arrays.toString(infoHtmlUrls) + "\n") +
				(isBaseLayer==null ? "" :              "	isBaseLayer=" + isBaseLayer + "\n") +
				(hasLegend==null ? "" :                "	hasLegend=" + hasLegend + "\n") +
				(Utils.isBlank(legendGroup) ? "" :     "	legendGroup=" + legendGroup + "\n") +
				(Utils.isBlank(legendTitle) ? "" :     "	legendTitle=" + legendTitle + "\n") +
				(styles==null ? "" :                   "	styles=" + styles + "\n") +
				(options==null ? "" :                  "	options=" + options + "\n") +
				(selected==null ? "" :                 "	selected=" + selected + "\n") +
				// TODO Add interface fields
//				"	dataSourceConfig=" + super.toString() + "\n" +
			'}';
	}
}
