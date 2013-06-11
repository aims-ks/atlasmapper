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
 * The only use of this interface is to ensure that the data source fields are "overridable" in layer manual overrides.
 */
public interface ArcGISMapServerDataSourceConfigInterface {
	public Boolean isForcePNG24();
	public void setForcePNG24(Boolean forcePNG24);

	public String getIgnoredArcGISPath();
	public void setIgnoredArcGISPath(String ignoredArcGISPath);
}
