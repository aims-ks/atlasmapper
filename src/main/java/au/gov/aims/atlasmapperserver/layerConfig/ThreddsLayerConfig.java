/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2020 Australian Institute of Marine Science
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

import au.gov.aims.atlasmapperserver.ConfigManager;
import au.gov.aims.atlasmapperserver.annotation.ConfigField;

public class ThreddsLayerConfig extends WMSLayerConfig {
    @ConfigField
    private String datasetId;

    @ConfigField
    private String serviceUrl;

    @ConfigField
    private String featureRequestsUrl;

    @ConfigField
    private String legendUrl;

    @ConfigField
    private String wmsVersion;

    public ThreddsLayerConfig(ConfigManager configManager) {
        super(configManager);
    }

    public String getDatasetId() {
        return this.datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public String getServiceUrl() {
        return this.serviceUrl;
    }

    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    public String getFeatureRequestsUrl() {
        return featureRequestsUrl;
    }

    public void setFeatureRequestsUrl(String featureRequestsUrl) {
        this.featureRequestsUrl = featureRequestsUrl;
    }

    public String getLegendUrl() {
        return legendUrl;
    }

    public void setLegendUrl(String legendUrl) {
        this.legendUrl = legendUrl;
    }

    public String getWmsVersion() {
        return wmsVersion;
    }

    public void setWmsVersion(String wmsVersion) {
        this.wmsVersion = wmsVersion;
    }
}
