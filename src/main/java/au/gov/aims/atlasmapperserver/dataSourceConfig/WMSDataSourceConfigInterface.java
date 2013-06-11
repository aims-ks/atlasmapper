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

/**
 * All fields defined in this Interface are defined in the WMS DataSources and are "overridable" by the WMS layers
 */
public interface WMSDataSourceConfigInterface {
	public String getExtraWmsServiceUrls();
	public void setExtraWmsServiceUrls(String extraWmsServiceUrls);

	public String getWebCacheSupportedParameters();
	public void setWebCacheSupportedParameters(String webCacheSupportedParameters);

	public String getWebCacheUrl();
	public void setWebCacheUrl(String webCacheUrl);

	public String getWmsRequestMimeType();
	public void setWmsRequestMimeType(String wmsRequestMimeType);

	public Boolean isWmsTransectable();
	public void setWmsTransectable(Boolean wmsTransectable);

	public String getWmsVersion();
	public void setWmsVersion(String wmsVersion);
}
