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

package au.gov.aims.atlasmapperserver.dataSourceConfig;

import au.gov.aims.atlasmapperserver.ConfigManager;
import au.gov.aims.atlasmapperserver.Utils;
import au.gov.aims.atlasmapperserver.annotation.ConfigField;
import au.gov.aims.atlasmapperserver.jsonWrappers.client.DataSourceWrapper;
import au.gov.aims.atlasmapperserver.layerGenerator.AbstractLayerGenerator;
import au.gov.aims.atlasmapperserver.layerGenerator.WMSLayerGenerator;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class WMSDataSourceConfig extends AbstractDataSourceConfig {
	@ConfigField
	private String getMapUrl;

	@ConfigField
	private String[] extraWmsServiceUrls;

	@ConfigField
	private String webCacheCapabilitiesUrl;

	@ConfigField
	private Boolean webCacheEnable;

	@ConfigField
	private String webCacheUrl;

	// This field used to be called "webCacheParameters"
	@ConfigField(alias="webCacheParameters")
	private String[] webCacheSupportedParameters;

	@ConfigField
	private String wmsRequestMimeType;

	@ConfigField
	private Boolean legendDpiSupport;

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

	public String[] getExtraWmsServiceUrls() {
		return this.extraWmsServiceUrls;
	}
	public void setExtraWmsServiceUrls(String[] rawExtraWmsServiceUrls) {
		if (rawExtraWmsServiceUrls == null || rawExtraWmsServiceUrls.length <= 0) {
			this.extraWmsServiceUrls = null;
		} else {
			List<String> extraWmsServiceUrls = new ArrayList<String>(rawExtraWmsServiceUrls.length);
			for (String extraWmsServiceUrl : rawExtraWmsServiceUrls) {
				// When the value come from the form (or an old config file), it's a coma separated String instead of an Array
				Pattern regex = Pattern.compile(".*" + SPLIT_PATTERN + ".*", Pattern.DOTALL);
				if (regex.matcher(extraWmsServiceUrl).matches()) {
					for (String splitUrl : extraWmsServiceUrl.split(SPLIT_PATTERN)) {
						extraWmsServiceUrls.add(splitUrl.trim());
					}
				} else {
					extraWmsServiceUrls.add(extraWmsServiceUrl.trim());
				}
			}
			this.extraWmsServiceUrls = extraWmsServiceUrls.toArray(new String[extraWmsServiceUrls.size()]);
		}
	}

	public String[] getWebCacheSupportedParameters() {
		return this.webCacheSupportedParameters;
	}

	public void setWebCacheSupportedParameters(String[] rawWebCacheSupportedParameters) {
		if (rawWebCacheSupportedParameters == null || rawWebCacheSupportedParameters.length <= 0) {
			this.webCacheSupportedParameters = null;
		} else {
			List<String> webCacheSupportedParameters = new ArrayList<String>(rawWebCacheSupportedParameters.length);
			for (String rawWebCacheSupportedParameter : rawWebCacheSupportedParameters) {
				// When the value come from the form (or an old config file), it's a coma separated String instead of an Array
				if (rawWebCacheSupportedParameter.contains(",")) {
					for (String splitParameter : rawWebCacheSupportedParameter.split(",")) {
						webCacheSupportedParameters.add(splitParameter.trim());
					}
				} else {
					webCacheSupportedParameters.add(rawWebCacheSupportedParameter.trim());
				}
			}
			this.webCacheSupportedParameters = webCacheSupportedParameters.toArray(new String[webCacheSupportedParameters.size()]);
		}
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

	public Boolean isLegendDpiSupport() {
		return this.legendDpiSupport;
	}

	public void setLegendDpiSupport(Boolean legendDpiSupport) {
		this.legendDpiSupport = legendDpiSupport;
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

	// Generate the config to be display in the admin page, or saved as a data source saved state
	@Override
	public JSONObject toJSonObject(boolean forSavedState) throws JSONException {
		DataSourceWrapper dataSourceWrapper = new DataSourceWrapper(super.toJSonObject(forSavedState));
		if (forSavedState) {
			dataSourceWrapper.setGetMapUrl(null);
		}
		return dataSourceWrapper.getJSON();
	}

	@Override
	public String toString() {
		return "WMSDataSourceConfig {\n" +
				(this.getId()==null ? "" :                             "	id=" + this.getId() + "\n") +
				(extraWmsServiceUrls.length <= 0 ? "" :                "	extraWmsServiceUrls=" + extraWmsServiceUrls + "\n") +
				(Utils.isBlank(webCacheUrl) ? "" :                     "	webCacheUrl=" + webCacheUrl + "\n") +
				(webCacheSupportedParameters==null ? "" :              "	webCacheSupportedParameters=" + webCacheSupportedParameters + "\n") +
				(Utils.isBlank(wmsRequestMimeType) ? "" :              "	wmsRequestMimeType=" + wmsRequestMimeType + "\n") +
				(wmsTransectable==null ? "" :                          "	wmsTransectable=" + wmsTransectable + "\n") +
				(Utils.isBlank(wmsVersion) ? "" :                      "	wmsVersion=" + wmsVersion + "\n") +
				(Utils.isBlank(this.getDataSourceId()) ? "" :          "	dataSourceId=" + this.getDataSourceId() + "\n") +
				(Utils.isBlank(this.getDataSourceName()) ? "" :        "	dataSourceName=" + this.getDataSourceName() + "\n") +
				(Utils.isBlank(this.getLayerType()) ? "" :             "	layerType=" + this.getLayerType() + "\n") +
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
