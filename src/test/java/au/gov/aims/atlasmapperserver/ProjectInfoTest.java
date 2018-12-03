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

import junit.framework.TestCase;

/**
 *
 * @author glafond
 */
public class ProjectInfoTest extends TestCase {
	public void testGetVersion() {
		assertTrue("Can not parse the project version", validProjectAttribute(ProjectInfo.getVersion()));
	}

	public void testGetLicenses() {
		assertTrue("Can not parse the project license", validProjectAttribute(ProjectInfo.getLicenseName()));
	}

	private static boolean validProjectAttribute(String value) {
		if (value == null) return false;

		String trimValue = value.trim();

		if (trimValue.startsWith("${") && trimValue.endsWith("}")) {
			return false;
		}

		return true;
	}
}
