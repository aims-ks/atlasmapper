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
import au.gov.aims.atlasmapperserver.layerGenerator.WMSLayerGenerator;

import java.util.Set;

public class WMSDataSourceConfig extends AbstractDataSourceConfig implements WMSDataSourceConfigInterface {
	@ConfigField
	private String getMapUrl;

	@ConfigField
	private String extraWmsServiceUrls;
	// Cache - avoid parsing extraWmsServiceUrls string every times.
	private Set<String> extraWmsServiceUrlsSet = null;

	@ConfigField
	private String webCacheCapabilitiesUrl;

	@ConfigField
	private Boolean webCacheEnable;

	@ConfigField
	private String webCacheUrl;

	// This field used to be called "webCacheParameters"
	@ConfigField(alias="webCacheParameters")
	private String webCacheSupportedParameters;

	@ConfigField
	private String wmsRequestMimeType;

	@ConfigField
	private Boolean wmsTransectable;

	@ConfigField
	private String wmsVersion;

	@ConfigField
	private String cacheWmsVersion;

	public WMSDataSourceConfig(ConfigManager configManager) {
		super(configManager);
	}

	@Override
	public AbstractLayerGenerator createLayerGenerator() throws Exception {
		return new WMSLayerGenerator();
	}

	public String getGetMapUrl() {
		return this.getMapUrl;
	}

	public void setGetMapUrl(String getMapUrl) {
		this.getMapUrl = getMapUrl;
	}

	public String getExtraWmsServiceUrls() {
		return this.extraWmsServiceUrls;
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
	public String[] getWebCacheSupportedParametersArray() {
		if (this.webCacheSupportedParameters == null) {
			return null;
		}

		String trimedWebCacheParameters = this.webCacheSupportedParameters.trim();
		if (trimedWebCacheParameters.isEmpty()) {
			return null;
		}

		return trimedWebCacheParameters.split("\\s*,\\s*");
	}

	public String getWebCacheSupportedParameters() {
		return this.webCacheSupportedParameters;
	}

	public void setWebCacheSupportedParameters(String webCacheSupportedParameters) {
		this.webCacheSupportedParameters = webCacheSupportedParameters;
	}

	public String getWebCacheCapabilitiesUrl() {
		return this.webCacheCapabilitiesUrl;
	}

	public void setWebCacheCapabilitiesUrl(String webCacheCapabilitiesUrl) {
		this.webCacheCapabilitiesUrl = webCacheCapabilitiesUrl;
	}

	public Boolean isWebCacheEnable() {
		return this.webCacheEnable;
	}

	public void setWebCacheEnable(Boolean webCacheEnable) {
		this.webCacheEnable = webCacheEnable;
	}

	public String getWebCacheUrl() {
		return this.webCacheUrl;
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
		return this.wmsTransectable;
	}

	public void setWmsTransectable(Boolean wmsTransectable) {
		this.wmsTransectable = wmsTransectable;
	}

	public String getWmsVersion() {
		return this.wmsVersion;
	}

	public void setWmsVersion(String wmsVersion) {
		this.wmsVersion = wmsVersion;
	}

	public String getCacheWmsVersion() {
		return this.cacheWmsVersion;
	}

	public void setCacheWmsVersion(String cacheWmsVersion) {
		this.cacheWmsVersion = cacheWmsVersion;
	}

	@Override
	public String toString() {
		return "WMSDataSourceConfig {\n" +
				(this.getId()==null ? "" :                             "	id=" + this.getId() + "\n") +
				(Utils.isBlank(extraWmsServiceUrls) ? "" :             "	extraWmsServiceUrls=" + extraWmsServiceUrls + "\n") +
				(Utils.isBlank(webCacheUrl) ? "" :                     "	webCacheUrl=" + webCacheUrl + "\n") +
				(Utils.isBlank(webCacheSupportedParameters) ? "" :     "	webCacheSupportedParameters=" + webCacheSupportedParameters + "\n") +
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
				(Utils.isBlank(this.getBlackAndWhiteListedLayers()) ? "" :     "	blackAndWhiteListedLayers=" + this.getBlackAndWhiteListedLayers() + "\n") +
				(this.isShowInLegend()==null ? "" :                    "	showInLegend=" + this.isShowInLegend() + "\n") +
				(Utils.isBlank(this.getComment()) ? "" :               "	comment=" + this.getComment() + "\n") +
				'}';
	}
}
