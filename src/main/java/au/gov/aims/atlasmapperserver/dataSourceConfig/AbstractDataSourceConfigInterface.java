/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2013 Australian Institute of Marine Science
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

import org.json.JSONSortedObject;

/**
 * The only use of this interface is to ensure that the data source fields are "overridable" in layer manual overrides.
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
