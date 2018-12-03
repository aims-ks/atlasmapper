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

import au.gov.aims.atlasmapperserver.servlet.FileFinder;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import org.json.JSONException;

/**
 * Class that retain a static instance of a ConfigManager,
 * to avoid loading the config from the file after each click.
 * @author glafond
 */
public class ConfigHelper {
	private static final Logger LOGGER = Logger.getLogger(ConfigHelper.class.getName());

	private static final String CLIENT_MAIN_CONFIG = "main.json";
	private static final String CLIENT_EMBEDDED_CONFIG = "embedded.json";
	private static final String CLIENT_LAYERS_CONFIG = "layers.json";

	public static final String SERVER_MAIN_CONFIG = "server.json";
	public static final String SERVER_USERS_CONFIG = "users.json";

	private static ConfigManager configManager;

	/**
	 * Reload the config from the default file
	 * @throws JSONException
	 * @throws IOException
	 */
	public static void load(ServletContext context) {
		if (configManager == null) {
			configManager = new ConfigManager(
					findServerConfigFile(context),
					findUsersConfigFile(context),
					context);

			configManager.setApplicationFolder(FileFinder.getApplicationFolder(context));
			configManager.setClientMainConfigFilename(CLIENT_MAIN_CONFIG);
			configManager.setClientEmbeddedConfigFilename(CLIENT_EMBEDDED_CONFIG);
			configManager.setClientLayersConfigFilename(CLIENT_LAYERS_CONFIG);
		}
	}

	public static void save() throws JSONException, IOException {
		saveServerConfig();
		saveUsersConfig();
	}

	public static void saveServerConfig() throws JSONException, IOException {
		if (configManager == null) {
			throw new IllegalArgumentException("The Config helper has never been initialised.");
		}
		configManager.saveServerConfig();
	}

	public static void saveUsersConfig() throws JSONException, IOException {
		if (configManager == null) {
			throw new IllegalArgumentException("The Config helper has never been initialised.");
		}
		configManager.saveUsersConfig();
	}

	// Use in test
	public static void saveServerConfig(Writer serverConfigWriter) throws JSONException, IOException {
		if (serverConfigWriter == null) {
			throw new IllegalArgumentException("The server configuration writer is null.");
		}
		if (configManager == null) {
			throw new IllegalArgumentException("The Config helper has never been initialised.");
		}
		configManager.saveServerConfig(serverConfigWriter);
	}
	public static void saveUsersConfig(Writer usersConfigWriter) throws JSONException, IOException {
		if (usersConfigWriter == null) {
			throw new IllegalArgumentException("The users configuration writer is null.");
		}
		if (configManager == null) {
			throw new IllegalArgumentException("The Config helper has never been initialised.");
		}
		configManager.saveUsersConfig(usersConfigWriter);
	}

	public static ConfigManager getConfigManager(ServletContext context) throws JSONException {
		if (configManager == null) {
			load(context);
		}
		return configManager;
	}

	private static File findServerConfigFile(ServletContext context) {
		return new File(FileFinder.getApplicationFolder(context), SERVER_MAIN_CONFIG);
	}

	private static File findUsersConfigFile(ServletContext context) {
		return new File(FileFinder.getApplicationFolder(context), SERVER_USERS_CONFIG);
	}
}
