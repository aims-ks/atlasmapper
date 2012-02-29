package au.gov.aims.atlasmapperserver.dataSourceConfig;

/**
 * All fields defined in this Interface are defined in the WMS DataSources and are "overridable" by the WMS layers
 */
public interface WMSDataSourceConfigInterface {
	public String getExtraWmsServiceUrls();
	public void setExtraWmsServiceUrls(String extraWmsServiceUrls);

	public String getWebCacheParameters();
	public void setWebCacheParameters(String webCacheParameters);

	public String getWebCacheUrl();
	public void setWebCacheUrl(String webCacheUrl);

	public String getWmsRequestMimeType();
	public void setWmsRequestMimeType(String wmsRequestMimeType);

	public Boolean isWmsTransectable();
	public void setWmsTransectable(Boolean wmsTransectable);

	public String getWmsVersion();
	public void setWmsVersion(String wmsVersion);
}
