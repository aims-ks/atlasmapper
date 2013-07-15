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

import au.gov.aims.atlasmapperserver.ConfigManager;
import au.gov.aims.atlasmapperserver.Utils;
import au.gov.aims.atlasmapperserver.annotation.ConfigField;

import java.util.Arrays;

public class WMSLayerConfig extends AbstractLayerConfig {
	@ConfigField
	private Boolean wmsQueryable;

	@ConfigField
	private String[] wmsFeatureRequestLayers;

	public WMSLayerConfig(ConfigManager configManager) {
		super(configManager);
	}

	public Boolean isWmsQueryable() {
		return this.wmsQueryable;
	}

	public void setWmsQueryable(Boolean wmsQueryable) {
		this.wmsQueryable = wmsQueryable;
	}

	public String[] getWmsFeatureRequestLayers() {
		return wmsFeatureRequestLayers;
	}

	public void setWmsFeatureRequestLayers(String[] wmsFeatureRequestLayers) {
		this.wmsFeatureRequestLayers = wmsFeatureRequestLayers;
	}

	public String toString() {
		return "WMSLayerConfig {\n" +
				(Utils.isBlank(this.getLayerId()) ? "" :       "	layerId=" + this.getLayerId() + "\n") +
				(Utils.isBlank(this.getLayerName()) ? "" :     "	layerName=" + this.getLayerName() + "\n") +
				(this.getAliasIds()==null ? "" :               "	aliasIds=" + Arrays.toString(this.getAliasIds()) + "\n") +
				(Utils.isBlank(this.getTitle()) ? "" :         "	title=" + this.getTitle() + "\n") +
				(Utils.isBlank(this.getDescription()) ? "" :   "	description=" + this.getDescription() + "\n") +
				(this.getLayerBoundingBox()==null ? "" :       "	layerBoundingBox=" + Arrays.toString(this.getLayerBoundingBox()) + "\n") +
				(this.getInfoHtmlUrls()==null ? "" :           "	infoHtmlUrls=" + Arrays.toString(this.getInfoHtmlUrls()) + "\n") +
				(this.isIsBaseLayer()==null ? "" :             "	isBaseLayer=" + this.isIsBaseLayer() + "\n") +
				(this.isHasLegend()==null ? "" :               "	hasLegend=" + this.isHasLegend() + "\n") +
				(Utils.isBlank(this.getLegendGroup()) ? "" :   "	legendGroup=" + this.getLegendGroup() + "\n") +
				(Utils.isBlank(this.getLegendTitle()) ? "" :   "	legendTitle=" + this.getLegendTitle() + "\n") +
				(wmsQueryable==null ? "" :                     "	wmsQueryable=" + wmsQueryable + "\n") +
				(Utils.isBlank(this.getTreePath()) ? "" :      "	treePath=" + this.getTreePath() + "\n") +
				(wmsFeatureRequestLayers==null ? "" :          "	wmsFeatureRequestLayers=" + Arrays.toString(wmsFeatureRequestLayers) + "\n") +
				(this.getStyles()==null ? "" :                 "	styles=" + this.getStyles() + "\n") +
				(this.getOptions()==null ? "" :                "	options=" + this.getOptions() + "\n") +
				(this.isSelected()==null ? "" :                "	selected=" + this.isSelected() + "\n") +
				'}';
	}
}
