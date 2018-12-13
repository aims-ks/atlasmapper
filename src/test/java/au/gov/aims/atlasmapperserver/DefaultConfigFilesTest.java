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

import au.gov.aims.atlasmapperserver.collection.MultiKeyHashMap;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import au.gov.aims.atlasmapperserver.dataSourceConfig.AbstractDataSourceConfig;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Gael Lafond <g.lafond@aims.gov.au>
 */
public class DefaultConfigFilesTest  {

    @Test
    public void testDefaultServerConfig() throws JSONException, IOException {
        ConfigManager cm = new ConfigManager(null, null);
        cm.reloadDefaultServerConfig();

        Assert.assertFalse(cm.isDemoMode());
        Assert.assertEquals(1.0, cm.getConfigVersion(), 0);

        MultiKeyHashMap<Integer, String, AbstractDataSourceConfig> dataSourceConfigs = cm.getDataSourceConfigs();
        Assert.assertNotNull(dataSourceConfigs);
        Assert.assertEquals(5, dataSourceConfigs.size());

        AbstractDataSourceConfig eatlas = dataSourceConfigs.get2("ea");
        Assert.assertEquals("eAtlas", eatlas.getDataSourceName());

        AbstractDataSourceConfig opengeo = dataSourceConfigs.get2("og");
        Assert.assertEquals("OpenGeo", opengeo.getDataSourceName());


        MultiKeyHashMap<Integer, String, ClientConfig> clientConfigs = cm.getClientConfigs();
        Assert.assertNotNull(clientConfigs);
        Assert.assertEquals(1, clientConfigs.size());

        ClientConfig demo = clientConfigs.get2("demo");
        Assert.assertEquals("Demo client", demo.getClientName());
    }

    public void testDefaultUsersConfig() throws JSONException, FileNotFoundException {
        ConfigManager cm = new ConfigManager(null, null);
        cm.reloadDefaultUsersConfig();

        Assert.assertEquals(1.0, cm.getUsersConfigVersion(), 0);
        Map<String, User> users = cm.getUsers();
        Assert.assertEquals(1, users.size());

        User admin = users.get("admin");
        Assert.assertEquals("Administrator", admin.getFirstName());
    }
}
