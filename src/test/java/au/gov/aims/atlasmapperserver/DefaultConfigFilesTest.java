/*
 *  This file is part of AtlasMapper server and clients.
 * 
 *  Copyright (C) 2011 Australian Institute of Marine Science
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

package au.gov.aims.atlasmapperserver;

import au.gov.aims.atlasmapperserver.collection.MultiKeyHashMap;
import java.io.FileNotFoundException;
import java.util.Map;
import junit.framework.TestCase;
import org.json.JSONException;

/**
 *
 * @author Gael Lafond <g.lafond@aims.org.au>
 */
public class DefaultConfigFilesTest extends TestCase  {

	public void testDefaultServerConfig() throws JSONException, FileNotFoundException {
		ConfigManager cm = new ConfigManager(null, null);
		cm.reloadDefaultServerConfig();

		assertFalse(cm.isDemoMode());
		assertEquals("1.0", cm.getConfigVersion());


		MultiKeyHashMap<Integer, String, DatasourceConfig> datasourceConfigs = cm.getDatasourceConfigs();
		assertNotNull(datasourceConfigs);
		assertEquals(2, datasourceConfigs.size());

		DatasourceConfig eatlas = datasourceConfigs.get2("ea");
		assertEquals("e-Atlas", eatlas.getDatasourceName());

		DatasourceConfig opengeo = datasourceConfigs.get2("og");
		assertEquals("OpenGeo", opengeo.getDatasourceName());


		MultiKeyHashMap<Integer, String, ClientConfig> clientConfigs = cm.getClientConfigs();
		assertNotNull(clientConfigs);
		assertEquals(1, clientConfigs.size());

		ClientConfig demo = clientConfigs.get2("demo");
		assertEquals("Demo client", demo.getClientName());
	}

	public void testDefaultUsersConfig() throws JSONException, FileNotFoundException {
		ConfigManager cm = new ConfigManager(null, null);
		cm.reloadDefaultUsersConfig();

		assertEquals("1.0", cm.getUsersConfigVersion());
		Map<String, User> users = cm.getUsers();
		assertEquals(1, users.size());

		User admin = users.get("admin");
		assertEquals("Administrator", admin.getFirstName());
	}
}
