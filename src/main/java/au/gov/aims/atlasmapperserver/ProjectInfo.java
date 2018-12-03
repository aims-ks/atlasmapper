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

package au.gov.aims.atlasmapperserver;

import java.util.ResourceBundle;

/**
 *
 * @author glafond
 */
public class ProjectInfo {
	private static final ResourceBundle PROPERTIES = ResourceBundle.getBundle("project");

	// Singleton
	private ProjectInfo() {}

	/**
	 * Return the full version string of the present war,
	 * or <code>null</code> if it cannot be determined.
	 * @see java.lang.Package#getImplementationVersion()
	 */
	public static String getName() {
		return PROPERTIES.getString("project.name");
	}

	public static String getVersion() {
		return PROPERTIES.getString("project.version");
	}

	public static String getDescription() {
		return PROPERTIES.getString("project.description");
	}

	public static String getUrl() {
		return PROPERTIES.getString("project.url");
	}

	public static String getLicenseName() {
		return PROPERTIES.getString("project.license.name");
	}
	public static String getLicenseUrl() {
		return PROPERTIES.getString("project.license.url");
	}
}
