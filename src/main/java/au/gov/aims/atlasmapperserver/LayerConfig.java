/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.gov.aims.atlasmapperserver;

import au.gov.aims.atlasmapperserver.annotation.ConfigField;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author glafond
 */
// NOTE Layers can override any fields of it's Datasource
public class LayerConfig extends DatasourceConfig {
	// The id is not a ConfigField to avoid having it in the JSon object
	private String layerId;

	@ConfigField
	private String kmlUrl;

	@ConfigField
	private String[] aliasIds;

	@ConfigField
	private String title;

	@ConfigField
	private String description;

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
	private Boolean wmsQueryable;

	@ConfigField
	private String wmsPath;

	@ConfigField
	private String[] wmsFeatureRequestLayers;

	@ConfigField
	private List<LayerStyleConfig> styles;

	@ConfigField
	private List<LayerOptionConfig> options;

	public LayerConfig() {}

	public LayerConfig(JSONObject json) {
		this();
		this.update(json);
	}

	public LayerConfig(DatasourceConfig datasource) {
		this();
		this.applyOverrides(datasource);
	}

	// Disable a few methods that should never be used.
	@Override
	public Integer getId() {
		// ID is used by ExtJS Grid. The ExtJS API offer a way to change this
		// field name but it cause problem with other parts of the library.
		return null;
	}
	@Override
	public void setId(Integer id) {
		// ID is used by ExtJS Grid. The ExtJS API offer a way to change this
		// field name but it cause problem with other parts of the library.
	}

	@Override
	public String getWmsServiceUrl() {
		return null;
	}

	@Override
	public void setWmsServiceUrl(String wmsServiceUrl) {
	}


	public String getKmlUrl() {
		return kmlUrl;
	}

	public void setKmlUrl(String kmlUrl) {
		this.kmlUrl = kmlUrl;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
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

	public Boolean isWmsQueryable() {
		return wmsQueryable;
	}

	public void setWmsQueryable(Boolean wmsQueryable) {
		this.wmsQueryable = wmsQueryable;
	}

	public String getWmsPath() {
		return wmsPath;
	}

	public void setWmsPath(String wmsPath) {
		this.wmsPath = wmsPath;
	}

	public String[] getWmsFeatureRequestLayers() {
		return wmsFeatureRequestLayers;
	}

	public void setWmsFeatureRequestLayers(String[] wmsFeatureRequestLayers) {
		this.wmsFeatureRequestLayers = wmsFeatureRequestLayers;
	}

	public List<LayerStyleConfig> getStyles() {
		return styles;
	}

	public void setStyles(List<LayerStyleConfig> styles) {
		this.styles = styles;
	}

	public List<LayerOptionConfig> getOptions() {
		return options;
	}

	public void setOptions(List<LayerOptionConfig> options) {
		this.options = options;
	}

	public LayerConfig applyOverrides(
			DatasourceConfig datasource,
			JSONObject globalOverrides,
			JSONObject clientOverrides) throws JSONException {

		LayerConfig layerGlobalOverride = null;
		if (globalOverrides != null && globalOverrides.length() > 0) {
			JSONObject globalOverride = globalOverrides.optJSONObject(this.layerId);
			if (globalOverride != null && globalOverride.length() > 0) {
				layerGlobalOverride = new LayerConfig(globalOverride);
			}
		}

		LayerConfig layerClientOverride = null;
		if (clientOverrides != null && clientOverrides.length() > 0) {
			JSONObject clientOverride = clientOverrides.optJSONObject(this.layerId);
			if (clientOverride != null && clientOverride.length() > 0) {
				layerClientOverride = new LayerConfig(clientOverride);
			}
		}

		return applyOverrides(datasource, layerGlobalOverride, layerClientOverride);
	}

	public LayerConfig applyOverrides(
			DatasourceConfig datasource,
			LayerConfig globalOverride,
			LayerConfig clientOverride) throws JSONException {

		LayerConfig clone = (LayerConfig)this.clone();

		if (clone != null) {
			clone.applyOverrides(globalOverride);
			clone.applyOverrides(clientOverride);
		}

		return clone;
	}

	@Override
	public String toString() {
		return "LayerConfig {\n" +
				(Utils.isBlank(layerId) ? "" :         "	layerId=" + layerId + "\n") +
				(aliasIds==null ? "" :                 "	aliasIds=" + aliasIds + "\n") +
				(Utils.isBlank(title) ? "" :           "	title=" + title + "\n") +
				(Utils.isBlank(description) ? "" :     "	description=" + description + "\n") +
				(layerBoundingBox==null ? "" :         "	layerBoundingBox=" + layerBoundingBox + "\n") +
				(infoHtmlUrls==null ? "" :             "	infoHtmlUrls=" + infoHtmlUrls + "\n") +
				(isBaseLayer==null ? "" :              "	isBaseLayer=" + isBaseLayer + "\n") +
				(hasLegend==null ? "" :                "	hasLegend=" + hasLegend + "\n") +
				(Utils.isBlank(legendGroup) ? "" :     "	legendGroup=" + legendGroup + "\n") +
				(Utils.isBlank(legendTitle) ? "" :     "	legendTitle=" + legendTitle + "\n") +
				(wmsQueryable==null ? "" :             "	wmsQueryable=" + wmsQueryable + "\n") +
				(Utils.isBlank(wmsPath) ? "" :         "	wmsPath=" + wmsPath + "\n") +
				(wmsFeatureRequestLayers==null ? "" :  "	wmsFeatureRequestLayers=" + wmsFeatureRequestLayers + "\n") +
				(styles==null ? "" :                   "	styles=" + styles + "\n") +
				(options==null ? "" :                  "	options=" + options + "\n") +
				"	datasource=" + super.toString() + "\n" +
			'}';
	}
}
