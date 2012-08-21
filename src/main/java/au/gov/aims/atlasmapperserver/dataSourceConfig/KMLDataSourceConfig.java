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
import au.gov.aims.atlasmapperserver.Utils;
import au.gov.aims.atlasmapperserver.annotation.ConfigField;
import au.gov.aims.atlasmapperserver.layerGenerator.AbstractLayerGenerator;
import au.gov.aims.atlasmapperserver.layerGenerator.KMLLayerGenerator;

import java.util.Set;

public class KMLDataSourceConfig extends AbstractDataSourceConfig {
	@ConfigField
	private String kmlUrls;
	// Cache - avoid parsing baseLayers string every times.
	private Set<String> kmlUrlsSet = null;

	public KMLDataSourceConfig(ConfigManager configManager) {
		super(configManager);
	}

	public String getKmlUrls() {
		return kmlUrls;
	}
	public Set<String> getKmlUrlsSet() {
		if (this.kmlUrlsSet == null && Utils.isNotBlank(this.kmlUrls)) {
			this.kmlUrlsSet = AbstractConfig.toSet(this.kmlUrls);
		}

		return this.kmlUrlsSet;
	}

	public void setKmlUrls(String kmlUrls) {
		this.kmlUrls = kmlUrls;
		this.kmlUrlsSet = null;
	}

	@Override
	public AbstractLayerGenerator getLayerGenerator() {
		return new KMLLayerGenerator(this);
	}

	@Override
	public String toString() {
		return "KMLDataSourceConfig {\n" +
				(this.getId()==null ? "" :                             "	id=" + this.getId() + "\n") +
				(Utils.isBlank(kmlUrls) ? "" :                         "	kmlUrls=" + kmlUrls + "\n") +
				(Utils.isBlank(this.getDataSourceId()) ? "" :          "	dataSourceId=" + this.getDataSourceId() + "\n") +
				(Utils.isBlank(this.getDataSourceName()) ? "" :        "	dataSourceName=" + this.getDataSourceName() + "\n") +
				(Utils.isBlank(this.getDataSourceType()) ? "" :        "	dataSourceType=" + this.getDataSourceType() + "\n") +
				(Utils.isBlank(this.getServiceUrl()) ? "" :            "	serviceUrl=" + this.getServiceUrl() + "\n") +
				(Utils.isBlank(this.getFeatureRequestsUrl()) ? "" :    "	featureRequestsUrl=" + this.getFeatureRequestsUrl() + "\n") +
				(Utils.isBlank(this.getLegendUrl()) ? "" :             "	legendUrl=" + this.getLegendUrl() + "\n") +
				(this.getLegendParameters()==null ? "" :               "	legendParameters=" + this.getLegendParameters() + "\n") +
				(Utils.isBlank(this.getBlackAndWhiteListedLayers()) ? "" :     "	blackAndWhiteListedLayers=" + this.getBlackAndWhiteListedLayers() + "\n") +
				(this.isShowInLegend()==null ? "" :                    "	showInLegend=" + this.isShowInLegend() + "\n") +
				(Utils.isBlank(this.getComment()) ? "" :               "	comment=" + this.getComment() + "\n") +
				'}';
	}
}
