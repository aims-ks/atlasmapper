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

import au.gov.aims.atlasmapperserver.ConfigManager;
import au.gov.aims.atlasmapperserver.Utils;
import au.gov.aims.atlasmapperserver.annotation.ConfigField;
import au.gov.aims.atlasmapperserver.layerGenerator.AbstractLayerGenerator;
import au.gov.aims.atlasmapperserver.layerGenerator.ArcGISMapServerLayerGenerator;

import java.io.IOException;

public class ArcGISMapServerDataSourceConfig extends AbstractDataSourceConfig implements ArcGISMapServerDataSourceConfigInterface {
	@ConfigField
	private Boolean forcePNG24;

	// This field is used to work around a none standard configuration on the GBRMPA ArcGIS server.
	@ConfigField
	private String ignoredArcGISPath;

	public ArcGISMapServerDataSourceConfig(ConfigManager configManager) {
		super(configManager);
	}

	public Boolean isForcePNG24() {
		return this.forcePNG24;
	}

	public void setForcePNG24(Boolean forcePNG24) {
		this.forcePNG24 = forcePNG24;
	}

	public String getIgnoredArcGISPath() {
		return this.ignoredArcGISPath;
	}

	public void setIgnoredArcGISPath(String ignoredArcGISPath) {
		this.ignoredArcGISPath = ignoredArcGISPath;
	}

	@Override
	public AbstractLayerGenerator getLayerGenerator() throws IOException {
		return new ArcGISMapServerLayerGenerator();
	}

	@Override
	public String toString() {
		return "ArcGISMapServerDataSourceConfig {\n" +
				(this.getId()==null ? "" :                             "	id=" + this.getId() + "\n") +
				(Utils.isBlank(ignoredArcGISPath) ? "" :               "	ignoredArcGISPath=" + ignoredArcGISPath + "\n") +
				(Utils.isBlank(this.getDataSourceId()) ? "" :          "	dataSourceId=" + this.getDataSourceId() + "\n") +
				(Utils.isBlank(this.getDataSourceName()) ? "" :        "	dataSourceName=" + this.getDataSourceName() + "\n") +
				(Utils.isBlank(this.getDataSourceType()) ? "" :        "	dataSourceType=" + this.getDataSourceType() + "\n") +
				(Utils.isBlank(this.getServiceUrl()) ? "" :            "	serviceUrl=" + this.getServiceUrl() + "\n") +
				(Utils.isBlank(this.getFeatureRequestsUrl()) ? "" :    "	featureRequestsUrl=" + this.getFeatureRequestsUrl() + "\n") +
				(Utils.isBlank(this.getLegendUrl()) ? "" :             "	legendUrl=" + this.getLegendUrl() + "\n") +
				(this.getLegendParameters()==null ? "" :               "	legendParameters=" + this.getLegendParameters() + "\n") +
				(Utils.isBlank(this.getBlacklistedLayers()) ? "" :     "	blacklistedLayers=" + this.getBlacklistedLayers() + "\n") +
				(this.isShowInLegend()==null ? "" :                    "	showInLegend=" + this.isShowInLegend() + "\n") +
				(Utils.isBlank(this.getComment()) ? "" :               "	comment=" + this.getComment() + "\n") +
				'}';
	}
}
