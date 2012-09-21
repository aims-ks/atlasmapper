package au.gov.aims.atlasmapperserver.dataSourceConfig;

import org.json.JSONSortedObject;

/**
 * All fields defined in the Interface are defined in the DataSources and are "overridable" by the AbstractLayerConfig classes
 */
public interface AbstractDataSourceConfigInterface {
	public String getBaseLayers();
	public void setBaseLayers(String baseLayers);

	public JSONSortedObject getGlobalManualOverride();
	public void setGlobalManualOverride(JSONSortedObject globalManualOverride);

	public String getDataSourceType();
	public void setDataSourceType(String dataSourceType);

	public String getFeatureRequestsUrl();
	public void setFeatureRequestsUrl(String featureRequestsUrl);

	public String getServiceUrl();
	public void setServiceUrl(String serviceUrl);

	public String getLegendUrl();
	public void setLegendUrl(String legendUrl);

	public String getLegendParameters();
	public void setLegendParameters(String legendParameters);

	public String getStylesUrl();
	public void setStylesUrl(String stylesUrl);

	public String getDataSourceId();
	public void setDataSourceId(String dataSourceId);

	public String getDataSourceName();
	public void setDataSourceName(String dataSourceName);

	public Boolean isShowInLegend();
	public void setShowInLegend(Boolean showInLegend);

	public String getComment();
	public void setComment(String comment);
}
