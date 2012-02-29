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
import au.gov.aims.atlasmapperserver.layerGenerator.LayerGeneratorCache;
import au.gov.aims.atlasmapperserver.layerGenerator.WMSLayerGenerator;
import org.geotools.ows.ServiceException;

import java.io.IOException;
import java.util.Set;

public class WMSDataSourceConfig extends AbstractDataSourceConfig implements WMSDataSourceConfigInterface {
	@ConfigField
	private String extraWmsServiceUrls;
	// Cache - avoid parsing extraWmsServiceUrls string every times.
	private Set<String> extraWmsServiceUrlsSet = null;

	@ConfigField
	private String webCacheUrl;

	@ConfigField
	private String webCacheParameters;

	@ConfigField
	private String wmsRequestMimeType;

	@ConfigField
	private Boolean wmsTransectable;

	@ConfigField
	private String wmsVersion;


	public WMSDataSourceConfig(ConfigManager configManager) {
		super(configManager);
	}

	@Override
	public AbstractLayerGenerator getLayerGenerator() throws IOException {
		AbstractLayerGenerator layerGenerator = null;
		try {
			layerGenerator = LayerGeneratorCache.getInstance(this.getServiceUrl(), WMSLayerGenerator.class);
		} catch (ServiceException e) {
			throw new IOException("Service Exception occurred while retrieving the WMS layer generator.", e);
		}
		return layerGenerator;
	}

	public String getExtraWmsServiceUrls() {
		return extraWmsServiceUrls;
	}
	public Set<String> getExtraWmsServiceUrlsSet() {
		if (this.extraWmsServiceUrlsSet == null && Utils.isNotBlank(this.extraWmsServiceUrls)) {
			this.extraWmsServiceUrlsSet = toSet(this.extraWmsServiceUrls);
		}

		return this.extraWmsServiceUrlsSet;
	}

	public void setExtraWmsServiceUrls(String extraWmsServiceUrls) {
		this.extraWmsServiceUrls = extraWmsServiceUrls;
		this.extraWmsServiceUrlsSet = null;
	}

	// Helper
	public String[] getWebCacheParametersArray() {
		if (this.webCacheParameters == null) {
			return null;
		}

		String trimedWebCacheParameters = this.webCacheParameters.trim();
		if (trimedWebCacheParameters.isEmpty()) {
			return null;
		}

		return trimedWebCacheParameters.split("\\s*,\\s*");
	}

	public String getWebCacheParameters() {
		return this.webCacheParameters;
	}

	public void setWebCacheParameters(String webCacheParameters) {
		this.webCacheParameters = webCacheParameters;
	}

	public String getWebCacheUrl() {
		return webCacheUrl;
	}

	public void setWebCacheUrl(String webCacheUrl) {
		this.webCacheUrl = webCacheUrl;
	}

	public String getWmsRequestMimeType() {
		return wmsRequestMimeType;
	}

	public void setWmsRequestMimeType(String wmsRequestMimeType) {
		this.wmsRequestMimeType = wmsRequestMimeType;
	}

	public Boolean isWmsTransectable() {
		return wmsTransectable;
	}

	public void setWmsTransectable(Boolean wmsTransectable) {
		this.wmsTransectable = wmsTransectable;
	}

	public String getWmsVersion() {
		return wmsVersion;
	}

	public void setWmsVersion(String wmsVersion) {
		this.wmsVersion = wmsVersion;
	}

	@Override
	public String toString() {
		return "WMSDataSourceConfig {\n" +
				(this.getId()==null ? "" :                             "	id=" + this.getId() + "\n") +
				(Utils.isBlank(extraWmsServiceUrls) ? "" :             "	extraWmsServiceUrls=" + extraWmsServiceUrls + "\n") +
				(Utils.isBlank(webCacheUrl) ? "" :                     "	webCacheUrl=" + webCacheUrl + "\n") +
				(Utils.isBlank(webCacheParameters) ? "" :              "	webCacheUrl=" + webCacheParameters + "\n") +
				(Utils.isBlank(wmsRequestMimeType) ? "" :              "	wmsRequestMimeType=" + wmsRequestMimeType + "\n") +
				(wmsTransectable==null ? "" :                          "	wmsTransectable=" + wmsTransectable + "\n") +
				(Utils.isBlank(wmsVersion) ? "" :                      "	wmsVersion=" + wmsVersion + "\n") +
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
