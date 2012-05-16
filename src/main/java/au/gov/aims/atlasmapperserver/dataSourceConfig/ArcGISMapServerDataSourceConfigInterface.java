package au.gov.aims.atlasmapperserver.dataSourceConfig;

/**
 * All fields defined in this Interface are defined in the AcrGIS DataSources and are "overridable" by the ArcGIS layers
 */
public interface ArcGISMapServerDataSourceConfigInterface {
	public String getIgnoredArcGISPath();
	public void setIgnoredArcGISPath(String ignoredArcGISPath);
}
