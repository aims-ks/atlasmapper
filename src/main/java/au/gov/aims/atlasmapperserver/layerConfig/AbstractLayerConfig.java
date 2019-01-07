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

package au.gov.aims.atlasmapperserver.layerConfig;

import au.gov.aims.atlasmapperserver.AbstractConfig;
import au.gov.aims.atlasmapperserver.ConfigManager;
import au.gov.aims.atlasmapperserver.Utils;
import au.gov.aims.atlasmapperserver.annotation.ConfigField;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import au.gov.aims.atlasmapperserver.jsonWrappers.client.LayerWrapper;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONSortedObject;

/**
 *
 * @author glafond
 */
// NOTE Layers can override any fields of it's data source's interface
public abstract class AbstractLayerConfig extends AbstractConfig {
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
    private JSONSortedObject downloadLinks;

    @ConfigField
    private String systemDescription;

    @ConfigField(alias="wmsPath") // Also require special case in "applyGlobalOverrides"
    private String treePath;

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

    @ConfigField(alias="dataSourceType")
    private String layerType;

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

    public String getLayerType() {
        return this.layerType;
    }

    public void setLayerType(String layerType) {
        this.layerType = layerType;
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

    public JSONSortedObject getDownloadLinks() {
        return this.downloadLinks;
    }

    public void setDownloadLinks(JSONSortedObject downloadLinks) {
        this.downloadLinks = downloadLinks;
    }

    public String getSystemDescription() {
        return this.systemDescription;
    }

    public void setSystemDescription(String systemDescription) {
        this.systemDescription = systemDescription;
    }

    public String getTreePath() {
        return this.treePath;
    }

    public void setTreePath(String treePath) {
        this.treePath = treePath;
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
        return this.styles;
    }

    public void setStyles(List<LayerStyleConfig> styles) {
        this.styles = styles;
    }

    public LayerStyleConfig getStyle(String styleName) {
        if (styleName == null || this.styles == null || this.styles.isEmpty()) {
            return null;
        }
        for (LayerStyleConfig style : this.styles) {
            if (styleName.equals(style.getName())) {
                return style;
            }
        }
        return null;
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

    public static LayerWrapper applyGlobalOverrides(String layerId, LayerWrapper layerWrapper, JSONObject globalOverrides)
            throws JSONException {

        JSONObject jsonLayer = layerWrapper.getJSON();
        if (globalOverrides != null && jsonLayer != null) {
            JSONObject layerOverride = globalOverrides.optJSONObject(layerId);
            if (layerOverride != null && layerOverride.length() > 0) {
                Iterator<String> keys = layerOverride.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    if (!layerOverride.isNull(key)) {
                        // Backward compatibility
                        if ("wmsPath".equals(key)) {
                            jsonLayer.put("treePath", layerOverride.opt(key));
                        } else {
                            jsonLayer.put(key, layerOverride.opt(key));
                        }
                    }
                }
            }
        }
        return new LayerWrapper(jsonLayer);
    }

    @Override
    public String toString() {
        return "AbstractLayerConfig {\n" +
                (Utils.isBlank(this.layerId) ? "" :         "	layerId=" + this.layerId + "\n") +
                (Utils.isBlank(this.layerName) ? "" :       "	layerName=" + this.layerName + "\n") +
                (this.aliasIds==null ? "" :                 "	aliasIds=" + Arrays.toString(this.aliasIds) + "\n") +
                (Utils.isBlank(this.title) ? "" :           "	title=" + this.title + "\n") +
                (Utils.isBlank(this.description) ? "" :     "	description=" + this.description + "\n") +
                (Utils.isBlank(this.treePath) ? "" :        "	treePath=" + this.treePath + "\n") +
                (this.layerBoundingBox==null ? "" :         "	layerBoundingBox=" + Arrays.toString(this.layerBoundingBox) + "\n") +
                (this.infoHtmlUrls==null ? "" :             "	infoHtmlUrls=" + Arrays.toString(this.infoHtmlUrls) + "\n") +
                (this.isBaseLayer==null ? "" :              "	isBaseLayer=" + this.isBaseLayer + "\n") +
                (this.hasLegend==null ? "" :                "	hasLegend=" + this.hasLegend + "\n") +
                (Utils.isBlank(this.legendGroup) ? "" :     "	legendGroup=" + this.legendGroup + "\n") +
                (Utils.isBlank(this.legendTitle) ? "" :     "	legendTitle=" + this.legendTitle + "\n") +
                (this.styles==null ? "" :                   "	styles=" + this.styles + "\n") +
                (this.options==null ? "" :                  "	options=" + this.options + "\n") +
                (this.selected==null ? "" :                 "	selected=" + this.selected + "\n") +
            '}';
    }
}
