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

import au.gov.aims.atlasmapperserver.dataSourceConfig.AbstractDataSourceConfigInterface;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONSortedObject;

/**
 *
 * @author glafond
 */
// NOTE Layers can override any fields of it's data source's interface
// TODO WMSLayerConfig extends AbstractLayerConfig
public abstract class AbstractLayerConfig extends AbstractConfig implements AbstractDataSourceConfigInterface {
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

	@ConfigField
	private String htmlDescription;

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
	private String blacklistedLayers;

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


	// Fields for DataSource override
	@Override
	public String getBlacklistedLayers() {
		return this.blacklistedLayers;
	}

	@Override
	public void setBlacklistedLayers(String blacklistedLayers) {
		this.blacklistedLayers = blacklistedLayers;
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

	public String getHtmlDescription() {
		return this.htmlDescription;
	}

	public void setHtmlDescription(String htmlDescription) {
		this.htmlDescription = htmlDescription;
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
		return options;
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

	public AbstractLayerConfig applyOverrides(
			JSONObject globalOverrides,
			JSONObject clientOverrides) throws JSONException {

		AbstractLayerConfig layerGlobalOverride = null;
		if (globalOverrides != null && globalOverrides.length() > 0) {
			JSONObject globalOverride = globalOverrides.optJSONObject(this.layerId);
			if (globalOverride != null && globalOverride.length() > 0) {
				layerGlobalOverride = LayerConfigHelper.createLayerConfig(
						globalOverride.optString("dataSourceType", this.getDataSourceType()), globalOverride, this.getConfigManager());
			}
		}

		AbstractLayerConfig layerClientOverride = null;
		if (clientOverrides != null && clientOverrides.length() > 0) {
			JSONObject clientOverride = clientOverrides.optJSONObject(this.layerId);
			if (clientOverride != null && clientOverride.length() > 0) {
				if (layerGlobalOverride != null) {
					layerGlobalOverride.update(clientOverride);
				} else {
					layerGlobalOverride = LayerConfigHelper.createLayerConfig(
							clientOverride.optString("dataSourceType", this.getDataSourceType()), clientOverride, this.getConfigManager());
				}
			}
		}

		return applyOverrides(layerGlobalOverride, layerClientOverride);
	}

	public AbstractLayerConfig applyOverrides(
			AbstractLayerConfig globalOverride,
			AbstractLayerConfig clientOverride) {

		AbstractLayerConfig clone = (AbstractLayerConfig)this.clone();

		if (clone != null) {
			clone.applyOverrides(globalOverride);
			clone.applyOverrides(clientOverride);
		}

		return clone;
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
