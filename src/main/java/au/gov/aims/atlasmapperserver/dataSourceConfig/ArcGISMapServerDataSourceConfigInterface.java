package au.gov.aims.atlasmapperserver.dataSourceConfig;

/**
 * All fields defined in this Interface are defined in the AcrGIS DataSources and are "overridable" by the ArcGIS layers
 */
public interface ArcGISMapServerDataSourceConfigInterface {
	public Boolean isForcePNG24();
	public void setForcePNG24(Boolean forcePNG24);

	public String getIgnoredArcGISPath();
	public void setIgnoredArcGISPath(String ignoredArcGISPath);
}
