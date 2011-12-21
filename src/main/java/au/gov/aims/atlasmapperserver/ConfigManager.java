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
import au.gov.aims.atlasmapperserver.servlet.FileFinder;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import org.geotools.ows.ServiceException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 *
 * @author glafond
 */
public class ConfigManager {
	private static final Logger LOGGER = Logger.getLogger(ConfigManager.class.getName());
	private static final String SERVER_DEFAULT_CONFIG_FILENAME = "defaultServer.conf";
	private static final String USERS_DEFAULT_CONFIG_FILENAME = "defaultUsers.conf";

	private static final String CONFIG_VERSION_KEY = "version";
	private static final String CURRENT_CONFIG_VERSION = "1.0";

	// Will eventually be used for backward compatibility
	private String configVersion;
	private String usersConfigVersion;

	private File serverConfigFile = null;
	private long serverConfigFileLastModified = -1;

	private File usersConfigFile = null;
	private long usersConfigFileLastModified = -1;

	// Set by the ConfigHelper
	private String clientFullConfigFilename = null;
	private String clientEmbeddedConfigFilename = null;
	private String clientLayersConfigFilename = null;
	private File applicationFolder = null;

	private static final String DATASOURCES_KEY = "dataSources";
	private static final String CLIENTS_KEY = "clients";

	// Demo: Atlas Mapper can run as a Demo application, with limited feature for better security.
	// The value can only be set by modifying the "server.conf" config file manually.
	private static final String DEMO_KEY = "demoMode";

	private Boolean demoMode = null;

	private String defaultProxyUrl = null;
	private String defaultLayerInfoServiceUrl = null;

	private int lastDataSourceId;
	private int lastClientId;

	private Map<String, User> users = null;
	private MultiKeyHashMap<Integer, String, DataSourceConfig> dataSourceConfigs = null;
	private MultiKeyHashMap<Integer, String, ClientConfig> clientConfigs = null;

	public ConfigManager(File serverConfigFile, File usersConfigFile) {
		this(serverConfigFile, usersConfigFile, null);
	}

	public ConfigManager(File serverConfigFile, File usersConfigFile, ServletContext context) {
		this.serverConfigFile = serverConfigFile;
		this.usersConfigFile = usersConfigFile;

		if (context != null) {
			this.defaultProxyUrl = FileFinder.getDefaultProxyURL(context);
			this.defaultLayerInfoServiceUrl = FileFinder.getDefaultLayerInfoServiceURL(context);
		}
	}

	public File getServerConfigFile() {
		return this.serverConfigFile;
	}
	public String getConfigVersion() {
		return this.configVersion;
	}

	public File getUsersConfigFile() {
		return this.usersConfigFile;
	}
	public String getUsersConfigVersion() {
		return this.usersConfigVersion;
	}

	public File getClientEmbeddedConfigFile(ClientConfig clientConfig) {
		if (this.clientEmbeddedConfigFilename == null) {
			return null;
		}
		File clientConfigFolder = this.getClientConfigFolder(clientConfig);
		if (clientConfigFolder == null) {
			return null;
		}
		return new File(clientConfigFolder, this.clientEmbeddedConfigFilename);
	}

	public File getClientFullConfigFile(ClientConfig clientConfig) {
		if (this.clientFullConfigFilename == null) {
			return null;
		}
		File clientConfigFolder = this.getClientConfigFolder(clientConfig);
		if (clientConfigFolder == null) {
			return null;
		}
		return new File(clientConfigFolder, this.clientFullConfigFilename);
	}

	public File getClientLayersConfigFile(ClientConfig clientConfig) {
		if (this.clientLayersConfigFilename == null) {
			return null;
		}
		File clientConfigFolder = this.getClientConfigFolder(clientConfig);
		if (clientConfigFolder == null) {
			return null;
		}
		return new File(clientConfigFolder, this.clientLayersConfigFilename);
	}

	private File getClientConfigFolder(ClientConfig clientConfig) {
		if (clientConfig == null || this.applicationFolder == null) {
			return null;
		}
		return FileFinder.getAtlasMapperClientConfigFolder(this.applicationFolder, clientConfig);
	}

	public void setApplicationFolder(File applicationFolder) {
		if (applicationFolder != null && !applicationFolder.exists()) {
			// Try to create the folder structure, if it doesn't exist
			applicationFolder.mkdirs();
		}

		this.applicationFolder = applicationFolder;
	}

	public void setClientEmbeddedConfigFilename(String clientEmbeddedConfigFilename) {
		this.clientEmbeddedConfigFilename = clientEmbeddedConfigFilename;
	}

	public void setClientFullConfigFilename(String clientFullConfigFilename) {
		this.clientFullConfigFilename = clientFullConfigFilename;
	}

	public void setClientLayersConfigFilename(String clientLayersConfigFilename) {
		this.clientLayersConfigFilename = clientLayersConfigFilename;
	}

	public void reloadServerConfigIfNeeded() throws JSONException, FileNotFoundException {
		// Check file last modified date
		long lastModified = (this.serverConfigFile == null ? -1 : this.serverConfigFile.lastModified());
		if (lastModified < 0 || lastModified != this.serverConfigFileLastModified) {
			this.serverConfigFileLastModified = lastModified;
			this.reloadServerConfig();
		}
	}

	private synchronized void reloadServerConfig() throws JSONException, FileNotFoundException {
		this.dataSourceConfigs = null;
		this.clientConfigs = null;

		if (this.serverConfigFile != null) {
			if (this.serverConfigFile.exists() && this.serverConfigFile.canRead()) {
				FileReader serverConfigReader = null;
				try {
					serverConfigReader = new FileReader(this.serverConfigFile);
					this.reloadServerConfig(serverConfigReader);
				} finally {
					try {
						if (serverConfigReader != null) { serverConfigReader.close(); }
					} catch(Exception e) {}
				}
			} else {
				LOGGER.log(Level.SEVERE, "{0} is not readable", this.serverConfigFile.getAbsolutePath());
			}
		} else {
			LOGGER.log(Level.SEVERE, "Undefined server configuration file");
		}

		if ((this.dataSourceConfigs == null || this.dataSourceConfigs.isEmpty()) &&
				(this.clientConfigs == null || this.clientConfigs.isEmpty())) {
			LOGGER.log(Level.WARNING, "No data sources nor clients defined; fall back to default.");
			this.reloadDefaultServerConfig();
		}
	}

	protected synchronized void reloadServerConfig(Reader serverConfigReader) throws JSONException {
		if (serverConfigReader == null) {
			return;
		}
		JSONObject jsonObj = null;
		try {
			jsonObj = new JSONObject(new JSONTokener(serverConfigReader));
		} catch(JSONException ex) {
			LOGGER.log(Level.WARNING, "Malformated AtlasMapper JSON config file. The configuration file can not be parsed.", ex);
			return;
		}

		this.demoMode = jsonObj.optBoolean(DEMO_KEY, false);
		this.configVersion = jsonObj.optString(CONFIG_VERSION_KEY, CURRENT_CONFIG_VERSION);

		this.dataSourceConfigs = new MultiKeyHashMap<Integer, String, DataSourceConfig>();
		this.lastDataSourceId = 0;
		JSONArray dataSourceConfigsArray = jsonObj.optJSONArray(DATASOURCES_KEY);

		if (dataSourceConfigsArray != null) {
			for (int i=0; i<dataSourceConfigsArray.length(); i++) {
				JSONObject rawDataSourceConfig = dataSourceConfigsArray.optJSONObject(i);
				if (rawDataSourceConfig != null) {
					DataSourceConfig dataSourceConfig = new DataSourceConfig(this);
					dataSourceConfig.update(rawDataSourceConfig);
					Integer dataSourceId = dataSourceConfig.getId();
					if (dataSourceId != null && dataSourceId > this.lastDataSourceId) {
						this.lastDataSourceId = dataSourceId;
					}
					this.dataSourceConfigs.put(
							dataSourceId,
							dataSourceConfig.getDataSourceId(),
							dataSourceConfig);
				} else {
					LOGGER.log(Level.WARNING, "Malformated AtlasMapper JSON config file: a data source is not set properly [{0}]", rawDataSourceConfig);
				}
			}
		}

		this.clientConfigs = new MultiKeyHashMap<Integer, String, ClientConfig>();
		this.lastClientId = 0;
		JSONArray clientConfigsArray = jsonObj.optJSONArray(CLIENTS_KEY);
		if (clientConfigsArray != null) {
			for (int i=0; i<clientConfigsArray.length(); i++) {
				Object rawClientConfig = clientConfigsArray.get(i);
				if (rawClientConfig instanceof JSONObject) {
					ClientConfig clientConfig = new ClientConfig(this);
					clientConfig.update((JSONObject)rawClientConfig);
					Integer id = clientConfig.getId();
					if (id != null && id > this.lastClientId) {
						this.lastClientId = id;
					}

					String clientId = clientConfig.getClientId();

					this.clientConfigs.put(id,
							clientId,
							clientConfig);
				} else {
					LOGGER.log(Level.WARNING, "Malformated AtlasMapper JSON config file: a client is not set properly [{0}]", rawClientConfig);
				}
			}
		}

		// Prevent memory leak
		WMSCapabilitiesWrapper.cleanupCapabilitiesDocumentsCache(this.dataSourceConfigs.values());
	}

	protected synchronized void reloadDefaultServerConfig() throws JSONException {
		InputStream in = null;
		Reader reader = null;

		try {
			in = this.getClass().getClassLoader().getResourceAsStream(SERVER_DEFAULT_CONFIG_FILENAME);
			reader = new InputStreamReader(in);

			this.reloadServerConfig(reader);
		} finally {
			if (in != null) {
				try { in.close(); } catch(Exception ex) {
					LOGGER.log(Level.WARNING, "Can not close the InputStream", ex);
				}
			}
			if (reader != null) {
				try { reader.close(); } catch(Exception ex) {
					LOGGER.log(Level.WARNING, "Can not close the Reader", ex);
				}
			}
		}
	}

	public synchronized void reloadUsersConfigIfNeeded() throws JSONException, FileNotFoundException {
		// Check file last modified date
		long lastModified = (this.usersConfigFile == null ? -1 : this.usersConfigFile.lastModified());
		if (lastModified < 0 || lastModified != this.usersConfigFileLastModified) {
			this.usersConfigFileLastModified = lastModified;
			this.reloadUsersConfig();
		}
	}

	private synchronized void reloadUsersConfig() throws JSONException, FileNotFoundException {
		if (this.usersConfigFile != null) {
			if (this.usersConfigFile.exists() && this.usersConfigFile.canRead()) {
				FileReader usersConfigReader = null;
				try {
					usersConfigReader = new FileReader(this.usersConfigFile);
					this.reloadUsersConfig(usersConfigReader);
				} finally {
					try {
						if (usersConfigReader != null) { usersConfigReader.close(); }
					} catch(Exception e) {}
				}
			} else {
				LOGGER.log(Level.WARNING, "{0} is not readable.", this.usersConfigFile.getAbsolutePath());
			}
		} else {
			LOGGER.log(Level.SEVERE, "Undefined users configuration file");
		}

		if (this.users == null || this.users.isEmpty()) {
			LOGGER.log(Level.WARNING, "No users defined; fall back to default users.");
			this.reloadDefaultUsersConfig();
		}
	}

	protected synchronized void reloadUsersConfig(Reader usersConfigReader) throws JSONException {
		this.users = new HashMap<String, User>();
		if (usersConfigReader != null) {
			JSONObject usersConfig = new JSONObject(new JSONTokener(usersConfigReader));
			this.usersConfigVersion = usersConfig.optString(CONFIG_VERSION_KEY, CURRENT_CONFIG_VERSION);
			JSONArray jsonUsers = usersConfig.optJSONArray("users");

			if (jsonUsers != null) {
				for (int i=0; i<jsonUsers.length(); i++) {
					JSONObject jsonUser = jsonUsers.optJSONObject(i);
					if (jsonUser != null) {
						User user = new User(this);
						user.update(jsonUser);
						this.users.put(user.getLoginName(), user);
					}
				}
			}
		}
	}

	protected synchronized void reloadDefaultUsersConfig() throws JSONException {
		InputStream in = null;
		Reader reader = null;

		try {
			in = this.getClass().getClassLoader().getResourceAsStream(USERS_DEFAULT_CONFIG_FILENAME);
			reader = new InputStreamReader(in);

			this.reloadUsersConfig(reader);
		} finally {
			if (in != null) {
				try { in.close(); } catch(Exception ex) {
					LOGGER.log(Level.WARNING, "Can not close the InputStream", ex);
				}
			}
			if (reader != null) {
				try { reader.close(); } catch(Exception ex) {
					LOGGER.log(Level.WARNING, "Can not close the Reader", ex);
				}
			}
		}
	}

	public synchronized void saveServerConfig() throws JSONException, IOException {
		if (this.serverConfigFile == null) {
			throw new IllegalArgumentException("The server configuration file is null.");
		}
		this.reloadServerConfigIfNeeded();
		Writer writer = null;
		if (Utils.recursiveIsWritable(this.serverConfigFile)) {
			try {
				writer = new FileWriter(this.serverConfigFile);
				this.saveServerConfig(writer);
				this.serverConfigFileLastModified = this.serverConfigFile.lastModified();
			} finally {
				try {
					if (writer != null) { writer.close(); }
				} catch(Exception e) {}
				// Reload the configuration to refresh the state of the server with the config that is in the file
				try { this.reloadServerConfig(); } catch(Exception e) {}
			}
		} else {
			// Reload the configuration to refresh the state of the server with the config that is in the file
			try { this.reloadServerConfig(); } catch(Exception e) {}
			throw new IOException(this.serverConfigFile.getCanonicalPath() + " is not writable.");
		}
	}
	public synchronized void saveServerConfig(Writer serverConfigWriter) throws JSONException, IOException {
		if (serverConfigWriter == null) {
			return;
		}
		JSONObject config = new JSONObject();

		if (this.demoMode != null && this.demoMode) {
			config.put(DEMO_KEY, this.demoMode);
		}

		config.put(CONFIG_VERSION_KEY, CURRENT_CONFIG_VERSION);
		config.put(DATASOURCES_KEY, this._getDataSourceConfigsJSon(false));
		config.put(CLIENTS_KEY, this._getClientConfigsJSon(false));

		this.saveJSONConfig(config, serverConfigWriter);

		// Prevent memory leak
		WMSCapabilitiesWrapper.cleanupCapabilitiesDocumentsCache(this.dataSourceConfigs.values());
	}

	public synchronized void saveUsersConfig() throws JSONException, IOException {
		if (this.usersConfigFile == null) {
			throw new IllegalArgumentException("The users configuration file is null.");
		}
		this.reloadUsersConfigIfNeeded();
		Writer writer = null;
		if (Utils.recursiveIsWritable(this.usersConfigFile)) {
			try {
				writer = new FileWriter(this.usersConfigFile);
				this.saveUsersConfig(writer);
				this.usersConfigFileLastModified = this.usersConfigFile.lastModified();
			} finally {
				try {
					if (writer != null) { writer.close(); }
				} catch(Exception e) {}
				// Reload the configuration to refresh the state of the server with the config that is in the file
				try { this.reloadUsersConfig(); } catch(Exception e) {}
			}
		} else {
			// Reload the configuration to refresh the state of the server with the config that is in the file
			try { this.reloadUsersConfig(); } catch(Exception e) {}
			throw new IOException(this.usersConfigFile.getCanonicalPath() + " is not writable.");
		}
	}
	public synchronized void saveUsersConfig(Writer usersConfigWriter) throws JSONException, IOException {
		if (usersConfigWriter == null) {
			return;
		}
		JSONArray jsonUsers = new JSONArray();

		for (User user : this.users.values()) {
			jsonUsers.put(user.toJSonObject());
		}

		JSONObject usersConfig = new JSONObject();
		usersConfig.put(CONFIG_VERSION_KEY, CURRENT_CONFIG_VERSION);
		usersConfig.put("users", jsonUsers);

		this.saveJSONConfig(usersConfig, usersConfigWriter);
	}

	public boolean isDemoMode() {
		try {
			this.reloadServerConfigIfNeeded();
		} catch (Exception ex) {
			// This should not happen...
			LOGGER.log(Level.SEVERE, "Unexpected exception occured while reloading the config. Fall back to demo mode.", ex);
			return true;
		}
		return this.demoMode;
	}

	public synchronized List<DataSourceConfig> createDataSourceConfig(ServletRequest request) throws JSONException, FileNotFoundException {
		if (request == null) {
			return null;
		}

		MultiKeyHashMap<Integer, String, DataSourceConfig> configs = this.getDataSourceConfigs();
		List<DataSourceConfig> newDataSourceConfigs = new ArrayList<DataSourceConfig>();
		JSONArray dataJSonArr = this.getPostedData(request);
		if (dataJSonArr != null) {
			for (int i=0; i<dataJSonArr.length(); i++) {
				JSONObject dataJSonObj = dataJSonArr.optJSONObject(i);
				if (dataJSonObj != null) {
					if (dataJSonObj.isNull("id") || dataJSonObj.optString("id", "").length() <= 0) {
						dataJSonObj.put("id", this.getNextDataSourceId());
					}
					DataSourceConfig dataSourceConfig = new DataSourceConfig(this);
					dataSourceConfig.update(dataJSonObj);

					this.ensureUniqueness(dataSourceConfig);

					configs.put(dataSourceConfig.getId(),
							dataSourceConfig.getDataSourceId(),
							dataSourceConfig);
					newDataSourceConfigs.add(dataSourceConfig);
				}
			}
		}
		return newDataSourceConfigs;
	}
	public synchronized void updateDataSourceConfig(ServletRequest request) throws JSONException, FileNotFoundException {
		if (request == null) {
			return;
		}

		MultiKeyHashMap<Integer, String, DataSourceConfig> configs = this.getDataSourceConfigs();
		JSONArray dataJSonArr = this.getPostedData(request);
		if (dataJSonArr != null) {
			for (int i=0; i<dataJSonArr.length(); i++) {
				JSONObject dataJSonObj = dataJSonArr.optJSONObject(i);
				if (dataJSonObj != null) {
					Integer dataSourceId = dataJSonObj.optInt("id", -1);
					DataSourceConfig dataSourceConfig = configs.get1(dataSourceId);
					if (dataSourceConfig != null) {
						// Update the object using the value from the form
						dataSourceConfig.update(dataJSonObj, true);

						this.ensureUniqueness(dataSourceConfig);
					}
				}
			}
		}
	}
	public synchronized void destroyDataSourceConfig(ServletRequest request) throws JSONException, FileNotFoundException {
		if (request == null) {
			return;
		}

		MultiKeyHashMap<Integer, String, DataSourceConfig> configs = this.getDataSourceConfigs();
		JSONArray dataJSonArr = this.getPostedData(request);
		if (dataJSonArr != null) {
			for (int i=0; i<dataJSonArr.length(); i++) {
				JSONObject dataJSonObj = dataJSonArr.optJSONObject(i);

				if (dataJSonObj != null) {
					Integer dataSourceId = dataJSonObj.optInt("id", -1);
					configs.remove1(dataSourceId);
				}
			}
		}
	}

	private void ensureUniqueness(DataSourceConfig dataSource) throws FileNotFoundException, JSONException {
		MultiKeyHashMap<Integer, String, DataSourceConfig> _dataSourceConfigs = getDataSourceConfigs();

		// Ensure the data source has a unique Integer ID (used in grid)
		if (dataSource.getId() == null) {
			dataSource.setId(this.getNextDataSourceId());
		}

		// Ensure the data source ID is unique (Note: there is a client side validation to avoid this problem)
		if (_dataSourceConfigs != null && this.dataSourceExists(dataSource.getDataSourceId(), dataSource.getId())) {
			// The data source exists, try to find a new data source ID.
			String dataSourceId = dataSource.getDataSourceId();
			String newDataSourceId = dataSourceId;
			if (newDataSourceId == null) {
				newDataSourceId = dataSource.getId().toString();
			}

			int suffix = 0;
			while (_dataSourceConfigs.get2(newDataSourceId) != null) {
				newDataSourceId = dataSourceId + "_" + ++suffix;
			}
			dataSource.setDataSourceId(newDataSourceId);
		}
	}

	/**
	 * Return true if there is an other data source (a data source with a
	 * different Integer ID) with the same data source ID.
	 * @param dataSourceId The chosen data source ID
	 * @param id The Integer ID used in the Grid, or null for new entries
	 * @return True if there is an other data source (a data source with a
	 * different Integer ID) with the same data source ID.
	 * @throws FileNotFoundException
	 * @throws JSONException
	 */
	public boolean dataSourceExists(String dataSourceId, Integer id) throws FileNotFoundException, JSONException {
		MultiKeyHashMap<Integer, String, DataSourceConfig> _dataSourceConfigs = getDataSourceConfigs();
		DataSourceConfig found = _dataSourceConfigs.get2(dataSourceId);

		// Most common case, the data source is new or it's data source ID has changed.
		if (found == null) { return false; }

		// Security: This case should not happen as long as the server.conf file is valid.
		if (found.getId() == null) { return true; }

		// Same data source ID AND Integer ID => Same data source
		if (found.getId().equals(id)) { return false; }

		// We found a data source with the same data source ID but with a different Integer ID.
		return true;
	}

	public synchronized List<ClientConfig> createClientConfig(ServletRequest request) throws JSONException, FileNotFoundException {
		if (request == null) {
			return null;
		}

		MultiKeyHashMap<Integer, String, ClientConfig> configs = this.getClientConfigs();
		List<ClientConfig> newClientConfigs = new ArrayList<ClientConfig>();
		JSONArray dataJSonArr = this.getPostedData(request);
		if (dataJSonArr != null) {
			for (int i=0; i<dataJSonArr.length(); i++) {
				JSONObject dataJSonObj = dataJSonArr.optJSONObject(i);
				if (dataJSonObj != null) {
					if (dataJSonObj.isNull("id") || dataJSonObj.optString("id", "").length() <= 0) {
						dataJSonObj.put("id", this.getNextClientId());
					}
					ClientConfig clientConfig = new ClientConfig(this);
					clientConfig.update(dataJSonObj);
					this.ensureUniqueness(clientConfig);

					configs.put(clientConfig.getId(),
							clientConfig.getClientId(),
							clientConfig);
					newClientConfigs.add(clientConfig);
				}
			}
		}

		return newClientConfigs;
	}
	public synchronized void updateClientConfig(ServletRequest request) throws JSONException, IOException, TemplateException {
		if (request == null) {
			return;
		}

		MultiKeyHashMap<Integer, String, ClientConfig> configs = this.getClientConfigs();
		JSONArray dataJSonArr = this.getPostedData(request);
		if (dataJSonArr != null) {
			for (int i=0; i<dataJSonArr.length(); i++) {
				JSONObject dataJSonObj = dataJSonArr.optJSONObject(i);
				if (dataJSonObj != null) {
					Integer clientId = dataJSonObj.optInt("id", -1);
					ClientConfig clientConfig = configs.get1(clientId);
					if (clientConfig != null) {
						File oldClientFolder = FileFinder.getAtlasMapperClientFolder(this.applicationFolder, clientConfig, false);
						File oldConfigFolder = FileFinder.getAtlasMapperClientConfigFolder(this.applicationFolder, clientConfig, false);

						clientConfig.update(dataJSonObj, true);
						this.ensureUniqueness(clientConfig);

						File newClientFolder = FileFinder.getAtlasMapperClientFolder(this.applicationFolder, clientConfig, false);
						File newConfigFolder = FileFinder.getAtlasMapperClientConfigFolder(this.applicationFolder, clientConfig, false);

						if (oldClientFolder != null && !oldClientFolder.equals(newClientFolder)) {
							// The project generation path has changed. The client folder has to be move.
							if (newClientFolder != null && oldClientFolder.exists()) {
								File parentFolder = newClientFolder.getParentFile();
								if (parentFolder != null && !parentFolder.exists()) {
									parentFolder.mkdirs();
								}
								oldClientFolder.renameTo(newClientFolder);
							}
						}
						if (oldConfigFolder != null && !oldConfigFolder.equals(newConfigFolder)) {
							// The project generation path has changed. The client folder has to be move.
							if (newConfigFolder != null && oldConfigFolder.exists()) {
								File parentFolder = newConfigFolder.getParentFile();
								if (parentFolder != null && !parentFolder.exists()) {
									parentFolder.mkdirs();
								}
								oldConfigFolder.renameTo(newConfigFolder);
							}
						}

						this.parsePreviewTemplate(clientConfig, clientConfig.useGoogle(this));
					}
				}
			}
		}
	}

	public synchronized boolean destroyClientConfig(ServletRequest request) throws JSONException, IOException {
		if (request == null) { return true; }

		MultiKeyHashMap<Integer, String, ClientConfig> configs = this.getClientConfigs();
		boolean success = true;
		JSONArray dataJSonArr = this.getPostedData(request);
		if (dataJSonArr != null) {
			for (int i=0; i<dataJSonArr.length(); i++) {
				JSONObject dataJSonObj = dataJSonArr.optJSONObject(i);
				if (dataJSonObj != null) {
					Integer clientId = dataJSonObj.optInt("id", -1);
					ClientConfig clientConfig =
							configs.remove1(clientId);

					// Delete client generated files - keep the server clean
					if (clientConfig != null) {
						File clientFolder = FileFinder.getClientFolder(this.applicationFolder, clientConfig, false);
						success = Utils.recursiveFileDelete(clientFolder) && success;
					}
				}
			}
		}
		return success;
	}

	private void ensureUniqueness(ClientConfig client) throws FileNotFoundException, JSONException {
		MultiKeyHashMap<Integer, String, ClientConfig> _clientConfigs = getClientConfigs();

		// Ensure the client has a unique Integer ID (used in grid)
		if (client.getId() == null) {
			client.setId(this.getNextClientId());
		}

		// Ensure the client ID is unique (Note: there is a client side validation to avoid this problem)
		if (_clientConfigs != null && this.clientExists(client.getClientId(), client.getId())) {
			// The client exists, try to find a new client ID.
			String clientId = client.getClientId();
			String newClientId = clientId;
			if (newClientId == null) {
				newClientId = client.getId().toString();
			}

			int suffix = 0;
			while (_clientConfigs.get2(newClientId) != null) {
				newClientId = clientId + "_" + ++suffix;
			}
			client.setClientId(newClientId);
		}
	}

	/**
	 * Return true if there is an other client (a client with a
	 * different Integer ID) with the same client ID.
	 * @param clientId The chosen client ID
	 * @param id The Integer ID used in the Grid, or null for new entries
	 * @return True if there is an other client (a client with a
	 * different Integer ID) with the same client ID.
	 * @throws FileNotFoundException
	 * @throws JSONException
	 */
	public boolean clientExists(String clientId, Integer id) throws FileNotFoundException, JSONException {
		MultiKeyHashMap<Integer, String, ClientConfig> _clientConfigs = getClientConfigs();
		ClientConfig found = _clientConfigs.get2(clientId);

		// Most common case, the client is new or it's client ID has changed.
		if (found == null) { return false; }

		// Security: This case should not happen as long as the server.conf file is valid.
		if (found.getId() == null) { return true; }

		// Same client ID AND Integer ID => Same client
		if (found.getId().equals(id)) { return false; }

		// We found a client with the same client ID but with a different Integer ID.
		return true;
	}

	/*
	public GlobalConfig getGlobalConfig() {
		return this.globalConfig;
	}
	public JSONObject getGlobalConfigJSon() throws JSONException {
		if (this.globalConfig != null) {
			return this.globalConfig.toJSonObject();
		}
		return null;
	}
	*/

	public JSONObject getClientLayers(String clientId, String[] layerIds, boolean live) throws JSONException, IOException, ServiceException, GetCapabilitiesExceptions {
		if (Utils.isBlank(clientId)) { return null; }

		return this.getClientLayers(this.getClientConfig(clientId), layerIds, live);
	}

	public JSONObject getClientLayers(ClientConfig clientConfig, String[] layerIds, boolean live) throws JSONException, IOException, ServiceException, GetCapabilitiesExceptions {
		if (clientConfig == null) { return null; }

		return this._getClientLayers(clientConfig, layerIds, live);
	}

	private JSONObject _getClientLayers(ClientConfig clientConfig, String[] layerIds, boolean live) throws JSONException, IOException, ServiceException, GetCapabilitiesExceptions {
		return _getClientLayers(clientConfig, Arrays.asList(layerIds), live);
	}
	private JSONObject _getClientLayers(ClientConfig clientConfig, Collection<String> layerIds, boolean live) throws JSONException, IOException, ServiceException, GetCapabilitiesExceptions {
		if (clientConfig == null) { return null; }

		JSONObject foundLayers = new JSONObject();

		JSONObject clientLayers = this.getClientConfigFileJSon(clientConfig, ConfigType.LAYERS, live, live);

		for (String rawLayerId : layerIds) {
			String layerId = rawLayerId.trim();
			if (clientLayers.has(layerId)) {
				foundLayers.put(layerId, clientLayers.get(layerId));
			}
		}

		return foundLayers;
	}

	public MultiKeyHashMap<Integer, String, DataSourceConfig> getDataSourceConfigs() throws JSONException, FileNotFoundException {
		this.reloadServerConfigIfNeeded();
		return this.dataSourceConfigs;
	}

	public DataSourceConfig getDataSourceConfig(String dataSourceId) throws JSONException, FileNotFoundException {
		if (Utils.isBlank(dataSourceId)) {
			return null;
		}

		MultiKeyHashMap<Integer, String, DataSourceConfig> configs = this.getDataSourceConfigs();
		if (configs == null) {
			return null;
		}

		return configs.get2(dataSourceId);
	}

	public JSONArray getDataSourceConfigsJSon() throws JSONException, FileNotFoundException {
		return this._getDataSourceConfigsJSon(true);
	}
	private JSONArray _getDataSourceConfigsJSon(boolean reload) throws JSONException, FileNotFoundException {
		JSONArray dataSourceConfigArray = new JSONArray();

		MultiKeyHashMap<Integer, String, DataSourceConfig> configs = reload ? this.getDataSourceConfigs() : this.dataSourceConfigs;
		for (DataSourceConfig dataSourceConfig : configs.values()) {
			dataSourceConfigArray.put(dataSourceConfig.toJSonObject());
		}
		return dataSourceConfigArray;
	}

	public MultiKeyHashMap<Integer, String, ClientConfig> getClientConfigs() throws JSONException, FileNotFoundException {
		this.reloadServerConfigIfNeeded();
		return this.clientConfigs;
	}

	public ClientConfig getClientConfig(String clientId) throws JSONException, FileNotFoundException {
		if (Utils.isBlank(clientId)) {
			return null;
		}

		MultiKeyHashMap<Integer, String, ClientConfig> configs = this.getClientConfigs();
		if (configs == null) {
			return null;
		}

		return configs.get2(clientId);
	}

	public ClientConfig getClientConfig(Integer clientId) throws JSONException, FileNotFoundException {
		if (clientId == null) {
			return null;
		}

		MultiKeyHashMap<Integer, String, ClientConfig> configs = this.getClientConfigs();
		if (configs == null) {
			return null;
		}

		return configs.get1(clientId);
	}

	public List<String> getProxyAllowedHosts(String clientId, boolean live)
			throws JSONException, IOException, ServiceException, GetCapabilitiesExceptions {

		ClientConfig clientConfig = this.getClientConfig(clientId);
		return this.getProxyAllowedHosts(clientConfig, live);
	}
	public List<String> getProxyAllowedHosts(ClientConfig clientConfig, boolean live)
			throws JSONException, IOException, ServiceException, GetCapabilitiesExceptions {

		List<String> allowedHosts = new ArrayList<String>();

		JSONObject clientJSON = this.getClientConfigFileJSon(clientConfig, ConfigType.FULL, live, live);
		if (clientJSON != null && clientJSON.has("dataSources")) {
			JSONObject dataSources = clientJSON.optJSONObject("dataSources");
			Iterator<String> keys = dataSources.keys();
			if (keys != null) {
				while (keys.hasNext()) {
					JSONObject dataSource = dataSources.optJSONObject(keys.next());

					// Only add the first one that successed
					boolean success =
							this.addProxyAllowedHost(allowedHosts, dataSource.optString("featureRequestsUrl")) ||
							this.addProxyAllowedHost(allowedHosts, dataSource.optString("wmsServiceUrl"));
				}
			}
		}

		JSONObject layersJSON = this.getClientConfigFileJSon(clientConfig, ConfigType.LAYERS, live, live);
		if (layersJSON != null) {
			Iterator<String> layerIds = layersJSON.keys();
			if (layerIds != null) {
				while (layerIds.hasNext()) {
					JSONObject layer = layersJSON.optJSONObject(layerIds.next());

					// Only add the first one that successed
					boolean success =
							this.addProxyAllowedHost(allowedHosts, layer.optString("kmlUrl")) ||
							this.addProxyAllowedHost(allowedHosts, layer.optString("featureRequestsUrl")) ||
							this.addProxyAllowedHost(allowedHosts, layer.optString("wmsServiceUrl"));
				}
			}
		}

		return allowedHosts.isEmpty() ? null : allowedHosts;
	}

	private boolean addProxyAllowedHost(List<String> allowedHosts, String urlStr) {
		URL url = null;
		try {
			url = new URL(urlStr);
		} catch (MalformedURLException ex) {
			return false;
		}
		// It should not be null if it succeed, but better not taking chance.
		if (url == null) { return false; }

		String host = url.getHost();
		if (host != null && !host.isEmpty() && !allowedHosts.contains(host)) {
			allowedHosts.add(host);
		}

		return true;
	}

	/**
	 * Used for the AtlasMapperServer config page. It has a link to
	 * view the actual client and a link to preview the client using
	 * the new config.
	 * @param context
	 * @return
	 * @throws JSONException
	 * @throws IOException
	 */
	public JSONArray getClientConfigsJSonWithClientUrls(ServletContext context) throws JSONException, IOException {
		JSONArray clientConfigArray = new JSONArray();
		for (ClientConfig clientConfig : this.getClientConfigs().values()) {
			clientConfigArray.put(clientConfig.toJSonObjectWithClientUrls(context));
		}
		return clientConfigArray;
	}

	public JSONArray getClientConfigsJSon() throws JSONException, FileNotFoundException {
		return this._getClientConfigsJSon(true);
	}
	private JSONArray _getClientConfigsJSon(boolean reload) throws JSONException, FileNotFoundException {
		JSONArray clientConfigArray = new JSONArray();

		MultiKeyHashMap<Integer, String, ClientConfig> configs = reload ? this.getClientConfigs() : this.clientConfigs;
		for (ClientConfig clientConfig : configs.values()) {
			clientConfigArray.put(clientConfig.toJSonObject());
		}
		return clientConfigArray;
	}

	// TODO Add a Default field
	public ClientConfig getDefaultClientConfig() throws JSONException, FileNotFoundException {
		Integer oldestKey = null;
		ClientConfig oldestClientConfig = null;

		for (Map.Entry<Integer, ClientConfig> configEntry : this.getClientConfigs().entrySet()) {
			ClientConfig clientConfig = configEntry.getValue();
			if (clientConfig.isEnable()) {
				// Return the first config that has default true and is enabled (should be maximum one)
				if (clientConfig.isDefault() != null && clientConfig.isDefault()) {
					return clientConfig;
				}

				// Find the enabled client that has the smallest key (oldest client)
				// The first client is used as a fallback if there is no default client that are enabled.
				Integer configKey = configEntry.getKey();
				if (oldestClientConfig == null || (configKey != null && oldestKey != null && configKey != null && oldestKey > configKey)) {
					oldestKey = configKey;
					oldestClientConfig = clientConfig;
				}
			}
		}

		return oldestClientConfig;
	}

	public Map<String, User> getUsers() throws JSONException, FileNotFoundException {
		this.reloadUsersConfigIfNeeded();
		return this.users;
	}

	public User getUser(String loginName) throws JSONException, FileNotFoundException {
		if (loginName == null) { return null; }

		this.reloadUsersConfigIfNeeded();

		return this.users.get(loginName);
	}

	public void generateAllClients(boolean complete)
			throws JSONException, IOException, ServiceException, TemplateException, GetCapabilitiesExceptions {

		// Emplty the capabilities cache before regenerating the configs
		WMSCapabilitiesWrapper.clearCapabilitiesDocumentsCache();

		for (ClientConfig clientConfig : this.getClientConfigs().values()) {
			this._generateClient(clientConfig, complete);
		}
	}

	public void generateClient(Integer clientId, boolean complete)
			throws JSONException, IOException, ServiceException, TemplateException, GetCapabilitiesExceptions {

		if (clientId == null) {
			return;
		}

		// Emplty the capabilities cache before regenerating the configs
		WMSCapabilitiesWrapper.clearCapabilitiesDocumentsCache();

		this._generateClient(this.getClientConfigs().get1(clientId), complete);
	}

	public void generateClient(ClientConfig clientConfig, boolean complete)
			throws JSONException, IOException, ServiceException, TemplateException, GetCapabilitiesExceptions {

		// Emplty the capabilities cache before regenerating the configs
		WMSCapabilitiesWrapper.clearCapabilitiesDocumentsCache();

		this._generateClient(clientConfig, complete);
	}

	private void _generateClient(ClientConfig clientConfig, boolean complete)
			throws JSONException, IOException, ServiceException, TemplateException, GetCapabilitiesExceptions {

		if (clientConfig == null) {
			return;
		}

		boolean useGoogle = clientConfig.useGoogle(this);

		this.copyClientFilesIfNeeded(clientConfig, complete);
		JSONObject generatedFullConfig = this.getClientConfigFileJSon(clientConfig, ConfigType.FULL, false, true);
		JSONObject generatedEmbeddedConfig = this.getClientConfigFileJSon(clientConfig, ConfigType.EMBEDDED, false, true);
		JSONObject generatedLayers = this.getClientConfigFileJSon(clientConfig, ConfigType.LAYERS, false, true);
		this.parseTemplates(clientConfig, useGoogle);
		this.saveGeneratedConfigs(clientConfig, generatedFullConfig, generatedEmbeddedConfig, generatedLayers);
	}

	/**
	 * Copy the client files from clientResources/amc to the client location.
	 * @param clientConfig The client to copy the files to
	 * @param force Force file copy even if they are already there (used with complete regeneration)
	 * @throws IOException
	 */
	private void copyClientFilesIfNeeded(ClientConfig clientConfig, boolean force) throws IOException {
		if (clientConfig == null) { return; }

		File atlasMapperClientFolder =
				FileFinder.getAtlasMapperClientFolder(this.applicationFolder, clientConfig);
		if (atlasMapperClientFolder == null) { return; }

		// Return if the folder is not empty
		String[] folderContent = atlasMapperClientFolder.list();
		if (!force && folderContent != null && folderContent.length > 0) {
			return;
		}

		// The folder is Empty, copying the files
		try {
			File src = FileFinder.getAtlasMapperClientSourceFolder();
			Utils.recursiveFileCopy(src, atlasMapperClientFolder, force);
		} catch (URISyntaxException ex) {
			throw new IOException("Can not get a File reference to the AtlasMapperClient", ex);
		}
	}

	// Create all files that required a template processing
	private void parseTemplates(ClientConfig clientConfig, boolean useGoogle) throws IOException, TemplateException {
		if (clientConfig == null) { return; }

		File atlasMapperClientFolder =
				FileFinder.getAtlasMapperClientFolder(this.applicationFolder, clientConfig);
		if (atlasMapperClientFolder == null) { return; }

		// Find template, process it and save it
		try {
			File templatesFolder = FileFinder.getAtlasMapperClientTemplatesFolder();
			Configuration templatesConfig = Utils.getTemplatesConfig(templatesFolder);

			// NOTE: To make a new template for the file "amc/x/y/z.ext",
			// create the file "amcTemplates/x/y/z.ext.flt" and add an entry
			// here for the template named "x/y/z.ext".
			// WARNING: Don't forget to use System.getProperty("file.separator")
			// instead of "/"!

			// Process all templates, one by one, because they are all unique
			Map<String, Object> indexValues = new HashMap<String, Object>();
			indexValues.put("version", ProjectInfo.getVersion());
			indexValues.put("clientId", clientConfig.getClientId());
			indexValues.put("clientName", clientConfig.getClientName() != null ? clientConfig.getClientName() : clientConfig.getClientId());
			indexValues.put("theme", clientConfig.getTheme());
			indexValues.put("timestamp", ""+Utils.getCurrentTimestamp());
			indexValues.put("useGoogle", useGoogle);
			Utils.processTemplate(templatesConfig, "index.html", indexValues, atlasMapperClientFolder);

			Map<String, Object> embeddedValues = new HashMap<String, Object>();
			embeddedValues.put("version", ProjectInfo.getVersion());
			embeddedValues.put("clientId", clientConfig.getClientId());
			embeddedValues.put("clientName", clientConfig.getClientName() != null ? clientConfig.getClientName() : clientConfig.getClientId());
			embeddedValues.put("timestamp", "" + Utils.getCurrentTimestamp());
			embeddedValues.put("useGoogle", useGoogle);
			Utils.processTemplate(templatesConfig, "embedded.html", embeddedValues, atlasMapperClientFolder);

			this.parsePreviewTemplate(clientConfig, useGoogle);
		} catch (URISyntaxException ex) {
			throw new IOException("Can not get a File reference to the AtlasMapperClient", ex);
		}
	}

	private void parsePreviewTemplate(ClientConfig clientConfig, boolean useGoogle) throws IOException, TemplateException {
		if (clientConfig == null) { return; }

		File atlasMapperClientFolder =
				FileFinder.getAtlasMapperClientFolder(this.applicationFolder, clientConfig);
		if (atlasMapperClientFolder == null) { return; }

		File[] indexFiles = atlasMapperClientFolder.listFiles(new MatchFilenameFilter("index.html"));

		// Find template, process it and save it
		if (indexFiles != null && indexFiles.length > 0) {
			try {
				File templatesFolder = FileFinder.getAtlasMapperClientTemplatesFolder();
				Configuration templatesConfig = Utils.getTemplatesConfig(templatesFolder);

				// Set the values that will be inserted in the template
				Map<String, Object> previewValues = new HashMap<String, Object>();
				previewValues.put("version", ProjectInfo.getVersion());
				previewValues.put("clientId", clientConfig.getClientId());
				previewValues.put("clientName", clientConfig.getClientName() != null ? clientConfig.getClientName() : clientConfig.getClientId());
				previewValues.put("theme", clientConfig.getTheme());
				previewValues.put("useGoogle", useGoogle);
				Utils.processTemplate(templatesConfig, "preview.html", previewValues, atlasMapperClientFolder);
			} catch (URISyntaxException ex) {
				throw new IOException("Can not get a File reference to the AtlasMapperClient", ex);
			}
		}
	}

	/**
	 * Return the current config and the generated config for the current client.
	 *
	 * *WARNING* This function return a lot of data and should only be used
	 * when the user want to debug the config!
	 *
	 * @param clientConfig
	 * @return A JSONObject containing all the config for the current
	 * configuration and the generated one, in the following format:
	 * "fullClient": {
	 *     "current": {...},
	 *     "generated": {...}
	 * },
	 * "embeddedClient": {
	 *     "current": {...},
	 *     "generated": {...}
	 * },
	 * "layers": {
	 *     "current": {...},
	 *     "generated": {...}
	 * }
	 * @throws JSONException
	 */
	public JSONObject debugClientConfigJSon(ClientConfig clientConfig)
			throws JSONException, IOException, ServiceException, GetCapabilitiesExceptions {

		if (clientConfig == null) {
			return null;
		}

		JSONObject debug = new JSONObject();

		JSONObject fullClientConfigs = new JSONObject();
		fullClientConfigs.put("current", Utils.jsonToStr(this.getClientConfigFileJSon(clientConfig, ConfigType.FULL, false, false)));
		fullClientConfigs.put("generated", Utils.jsonToStr(this.getClientConfigFileJSon(clientConfig, ConfigType.FULL, true, true)));

		JSONObject embeddedClientConfigs = new JSONObject();
		embeddedClientConfigs.put("current", Utils.jsonToStr(this.getClientConfigFileJSon(clientConfig, ConfigType.EMBEDDED, false, false)));
		embeddedClientConfigs.put("generated", Utils.jsonToStr(this.getClientConfigFileJSon(clientConfig, ConfigType.EMBEDDED, true, true)));

		JSONObject layers = new JSONObject();
		layers.put("current", Utils.jsonToStr(this.getClientConfigFileJSon(clientConfig, ConfigType.LAYERS, false, false)));
		layers.put("generated", Utils.jsonToStr(this.getClientConfigFileJSon(clientConfig, ConfigType.LAYERS, true, true)));

		debug.put("fullClient", fullClientConfigs);
		debug.put("embeddedClient", embeddedClientConfigs);
		debug.put("layers", layers);

		return debug;
	}

	public JSONObject getClientConfigFileJSon(ClientConfig clientConfig, ConfigType configType, boolean live, boolean generate)
			throws JSONException, IOException, ServiceException, GetCapabilitiesExceptions {

		if (clientConfig == null || configType == null) { return null; }
		switch (configType) {
			case FULL:
				JSONObject fullConfig = null;
				if (generate) {
					fullConfig = this._generateAbstractClientConfig(clientConfig);
					if (fullConfig != null) {
						JSONObject modules = this.generateModules(
								clientConfig.getFullClientModules(),
								clientConfig);
						if (modules != null && modules.length() > 0) {
							fullConfig.put("modules", modules);
						}
					}
				} else {
					fullConfig = this.loadExistingConfig(this.getClientFullConfigFile(clientConfig));
				}

				this._setProxyUrl(fullConfig, clientConfig, live);
				return fullConfig;

			case EMBEDDED:
				JSONObject embeddedConfig = null;
				if (generate) {
					embeddedConfig = this._generateAbstractClientConfig(clientConfig);

					JSONObject modules = this.generateModules(
							clientConfig.getEmbeddedClientModules(),
							clientConfig);
					if (modules != null && modules.length() > 0) {
						embeddedConfig.put("modules", modules);
					}
				} else {
					embeddedConfig = this.loadExistingConfig(this.getClientEmbeddedConfigFile(clientConfig));
				}

				this._setProxyUrl(embeddedConfig, clientConfig, live);
				return embeddedConfig;

			case LAYERS:
				if (generate) {
					JSONObject layersJSON = new JSONObject();
					Map<String, LayerConfig> layers = clientConfig.getLayerConfigs(this);
					if (layers != null) {
						for (LayerConfig layerConfig : layers.values()) {
							JSONObject layerJSON = this.generateLayer(layerConfig);

							if (layerJSON != null) {
								layersJSON.put(layerConfig.getLayerId(), layerJSON);
							}
						}
					}
					return layersJSON;
				} else {
					return this.loadExistingConfig(this.getClientLayersConfigFile(clientConfig));
				}
		}
		return null;
	}

	// Use as a base for Full and Embedded config
	private JSONObject _generateAbstractClientConfig(ClientConfig clientConfig)
			throws JSONException, IOException, ServiceException, GetCapabilitiesExceptions {

		if (clientConfig == null) { return null; }

		JSONObject json = new JSONObject();
		json.put(CONFIG_VERSION_KEY, CURRENT_CONFIG_VERSION);
		json.put("clientId", clientConfig.getClientId());
		json.put("clientName", clientConfig.getClientName());

		// TODO Remove when the default saved state will be implemented
		json.put("defaultLayers", this.getClientDefaultLayers(clientConfig.getClientId()));

		if (Utils.isNotBlank(clientConfig.getProjection())) {
			json.put("projection", clientConfig.getProjection());
			json.put("mapOptions", Utils.getMapOptions(clientConfig.getProjection()));
		}

		if (clientConfig.isUseLayerService()) {
			if (Utils.isNotBlank(clientConfig.getLayerInfoServiceUrl())) {
				json.put("layerInfoServiceUrl", clientConfig.getLayerInfoServiceUrl());
			} else if (Utils.isNotBlank(this.defaultLayerInfoServiceUrl)) {
				json.put("layerInfoServiceUrl", this.defaultLayerInfoServiceUrl);
			}
		}

		if (Utils.isNotBlank(clientConfig.getVersion())) {
			json.put("version", clientConfig.getVersion());
		}

		if (Utils.isNotBlank(clientConfig.getLatitude()) ||
				Utils.isNotBlank(clientConfig.getLongitude()) ||
				Utils.isNotBlank(clientConfig.getZoom())) {
			JSONArray startingLocation = new JSONArray();

			if (Utils.isNotBlank(clientConfig.getLongitude())) {
				startingLocation.put(Double.valueOf(clientConfig.getLongitude()));
			} else {
				startingLocation.put(0);
			}

			if (Utils.isNotBlank(clientConfig.getLatitude())) {
				startingLocation.put(Double.valueOf(clientConfig.getLatitude()));
			} else {
				startingLocation.put(0);
			}

			if (Utils.isNotBlank(clientConfig.getZoom())) {
				startingLocation.put(Integer.valueOf(clientConfig.getZoom()));
			} else {
				startingLocation.put(0);
			}

			json.put("startingLocation", startingLocation);
		}

		MultiKeyHashMap<Integer, String, DataSourceConfig> configs = this.getDataSourceConfigs();
		JSONArray dataSourcesArray = clientConfig.getDataSources();
		GetCapabilitiesExceptions errors = new GetCapabilitiesExceptions();
		if (dataSourcesArray != null && dataSourcesArray.length() > 0) {
			JSONObject dataSources = new JSONObject();
			for (int i=0; i<dataSourcesArray.length(); i++) {
				// https://github.com/douglascrockford/JSON-java/issues/24
				String dataSourceName = dataSourcesArray.optString(i, null);
				if (dataSourceName != null) {
					DataSourceConfig dataSourceConfig =
							configs.get2(dataSourceName);
					if (dataSourceConfig != null) {
						try {
							DataSourceConfig overridenDataSourceConfig = dataSourceConfig;

							// Apply overrides from the capabilities document
							if (Utils.isNotBlank(dataSourceConfig.getWmsServiceUrl())) {
								WMSCapabilitiesWrapper wmsCaps = WMSCapabilitiesWrapper.getInstance(
										dataSourceConfig.getWmsServiceUrl());
								if (wmsCaps != null) {
									overridenDataSourceConfig = wmsCaps.applyOverrides(
											dataSourceConfig);
								}
							}

							if (overridenDataSourceConfig != null) {
								// Apply overrides from the data source
								overridenDataSourceConfig =
										overridenDataSourceConfig.applyOverrides();
							}

							if (overridenDataSourceConfig != null) {
								JSONObject dataSource =
										this.generateDataSource(overridenDataSourceConfig, clientConfig);
								if (dataSource != null) {
									dataSources.put(overridenDataSourceConfig.getDataSourceId(), dataSource);
								}
							}
						} catch(IOException ex) {
							// Collect all errors
							errors.add(dataSourceConfig, ex);
						}
					}
				}
			}

			if (!errors.isEmpty()) {
				throw errors;
			}

			if (dataSources.length() > 0) {
				json.put("dataSources", dataSources);
			}
		}

		// Appearance
		// NOTE The ExtJS theme is used to generate the template,
		// the client do not need to know which theme it is using.
		if (Utils.isNotBlank(clientConfig.getPageHeader())) {
			json.put("pageHeader", clientConfig.getPageHeader());
		}
		if (Utils.isNotBlank(clientConfig.getPageFooter())) {
			json.put("pageFooter", clientConfig.getPageFooter());
		}
		if (Utils.isNotBlank(clientConfig.getLayerPanelHeader())) {
			json.put("layerPanelHeader", clientConfig.getLayerPanelHeader());
		}
		if (Utils.isNotBlank(clientConfig.getLayerPanelFooter())) {
			json.put("layerPanelFooter", clientConfig.getLayerPanelFooter());
		}

		return json;
	}

	private void _setProxyUrl(JSONObject clientJSON, ClientConfig clientConfig, boolean live) throws JSONException, UnsupportedEncodingException {
		if (clientConfig.isUseLayerService()) {
			if (Utils.isNotBlank(this.defaultProxyUrl)) {
				String proxyUrl = Utils.addUrlParameter(
						this.defaultProxyUrl,
						"client",
						clientConfig.getClientId());

				if (live) {
					proxyUrl = Utils.addUrlParameter(proxyUrl, "live", "true");
				}

				proxyUrl = Utils.addUrlParameter(proxyUrl, "url", "");

				clientJSON.put("proxyUrl", proxyUrl);
			}
		}
		if (Utils.isNotBlank(clientConfig.getProxyUrl())) {
			clientJSON.put("proxyUrl", clientConfig.getProxyUrl());
		}
	}

	private JSONObject getClientDefaultLayers(String clientId) throws JSONException, IOException, ServiceException, GetCapabilitiesExceptions {
		if (Utils.isBlank(clientId)) { return null; }

		return this._getClientLayers(
				this.getClientConfig(clientId),
				this._getClientDefaultLayerIds(clientId),
				true);
	}

	private Set<String> _getClientDefaultLayerIds(String clientId) throws JSONException, IOException, ServiceException {
		if (Utils.isBlank(clientId)) { return null; }

		ClientConfig clientConfig = this.getClientConfig(clientId);
		if (clientConfig == null) { return null; }

		return clientConfig.getDefaultLayersSet();
	}

	private JSONObject generateDataSource(DataSourceConfig dataSourceConfig, ClientConfig clientConfig) throws JSONException {

		JSONObject dataSource = new JSONObject();

		if (Utils.isNotBlank(dataSourceConfig.getFeatureRequestsUrl())) {
			dataSource.put("featureRequestsUrl", dataSourceConfig.getFeatureRequestsUrl());
		}

		if (Utils.isNotBlank(dataSourceConfig.getWmsServiceUrl())) {
			dataSource.put("wmsServiceUrl", dataSourceConfig.getWmsServiceUrl());
		}

		if (Utils.isNotBlank(dataSourceConfig.getExtraWmsServiceUrls())) {
			dataSource.put("extraWmsServiceUrls", dataSourceConfig.getExtraWmsServiceUrls());
		}

		if (Utils.isNotBlank(dataSourceConfig.getWebCacheUrl())) {
			dataSource.put("webCacheUrl", dataSourceConfig.getWebCacheUrl());
		}

		if (Utils.isNotBlank(dataSourceConfig.getLegendUrl())) {
			dataSource.put("legendUrl", dataSourceConfig.getLegendUrl());
		}

		if (Utils.isNotBlank(dataSourceConfig.getDataSourceName())) {
			dataSource.put("dataSourceName", dataSourceConfig.getDataSourceName());
		}

		String[] webCacheParametersArray = dataSourceConfig.getWebCacheParametersArray();
		if (webCacheParametersArray != null && webCacheParametersArray.length > 0) {
			JSONArray webCacheParameters = new JSONArray(webCacheParametersArray);
			dataSource.put("webCacheSupportedParameters", webCacheParameters);
		}

		if (Utils.isNotBlank(dataSourceConfig.getDataSourceType())) {
			dataSource.put("dataSourceType", dataSourceConfig.getDataSourceType());
		}

		JSONObject legendParameters = dataSourceConfig.getLegendParametersJson();
		// merge with client legend parameters, if any
		if (clientConfig != null) {
			JSONObject clientLegendParameters = clientConfig.getLegendParametersJson();
			if (clientLegendParameters != null) {
				JSONObject mergeParameters = new JSONObject();
				if (legendParameters != null) {
					Iterator<String> keys = legendParameters.keys();
					while(keys.hasNext()) {
						String key = keys.next();
						if (!legendParameters.isNull(key)) {
							Object value = legendParameters.opt(key);
							if (value != null) {
								mergeParameters.put(key, value);
							}
						}
					}
				}
				Iterator<String> keys = clientLegendParameters.keys();
				while(keys.hasNext()) {
					String key = keys.next();
					if (clientLegendParameters.isNull(key)) {
						if (mergeParameters.has(key)) {
							mergeParameters.remove(key);
						}
					} else {
						Object value = clientLegendParameters.opt(key);
						if (value != null) {
							mergeParameters.put(key, value);
						}
					}
				}
				legendParameters = mergeParameters;
			}
		}
		if (legendParameters != null && legendParameters.length() > 0) {
			dataSource.put("legendParameters", legendParameters);
		}

		if (Utils.isNotBlank(dataSourceConfig.getWmsVersion())) {
			dataSource.put("wmsVersion", dataSourceConfig.getWmsVersion());
		}

		return dataSource;
	}

	private JSONObject generateModules(JSONArray modulesArray, ClientConfig clientConfig) throws JSONException {
		if (modulesArray != null && modulesArray.length() > 0) {
			JSONObject modules = new JSONObject();
			for (int i=0; i<modulesArray.length(); i++) {
				// https://github.com/douglascrockford/JSON-java/issues/24
				String moduleName = modulesArray.optString(i, null);
				if (moduleName != null) {
					JSONObject module = this.generateModule(moduleName, clientConfig);
					if (module != null) {
						modules.put(moduleName, module);
					}
				}
			}

			return modules;
		}
		return null;
	}

	private JSONObject generateModule(String moduleConfig, ClientConfig clientConfig) throws JSONException {
		JSONObject moduleJSONConfig =
				ModuleHelper.generateModuleConfiguration(moduleConfig, clientConfig);

		if (moduleJSONConfig == null) {
			LOGGER.log(Level.SEVERE, "Can not generate the configuration for {0}", moduleConfig);
		}

		return moduleJSONConfig;
	}

	private JSONObject generateLayer(LayerConfig layerConfig) throws JSONException {
		// LayerConfig extends DataSourceConfig
		JSONObject jsonLayer = this.generateDataSource(layerConfig, null);

		jsonLayer.put(CONFIG_VERSION_KEY, CURRENT_CONFIG_VERSION);

		if (Utils.isNotBlank(layerConfig.getKmlUrl())) {
			jsonLayer.put("kmlUrl", layerConfig.getKmlUrl());
		}

		if (Utils.isNotBlank(layerConfig.getTitle())) {
			jsonLayer.put("title", layerConfig.getTitle());
		}

		if (Utils.isNotBlank(layerConfig.getDescription())) {
			jsonLayer.put("description", layerConfig.getDescription());
		}

		// serverId
		if (Utils.isNotBlank(layerConfig.getDataSourceId())) {
			jsonLayer.put("dataSourceId", layerConfig.getDataSourceId());
		}

		double[] boundingBox = layerConfig.getLayerBoundingBox();
		if (boundingBox != null && boundingBox.length > 0) {
			jsonLayer.put("layerBoundingBox", boundingBox);
		}

		if(layerConfig.isWmsQueryable() != null) {
			jsonLayer.put("wmsQueryable", layerConfig.isWmsQueryable());
		}

		if(Utils.isNotBlank(layerConfig.getWmsVersion())) {
			jsonLayer.put("wmsVersion", layerConfig.getWmsVersion());
		}

		if (layerConfig.isIsBaseLayer() != null) {
			jsonLayer.put("isBaseLayer", layerConfig.isIsBaseLayer());
		}

		if (layerConfig.isHasLegend() != null) {
			jsonLayer.put("hasLegend", layerConfig.isHasLegend());
		}

		// Initial state is related to the Client saved state

		// No need for a legend URL + Filename since there is no more Layer Groups
		if (Utils.isNotBlank(layerConfig.getLegendUrl())) {
			jsonLayer.put("legendUrl", layerConfig.getLegendUrl());
		}

		if(Utils.isNotBlank(layerConfig.getLegendGroup())) {
			jsonLayer.put("legendGroup", layerConfig.getLegendGroup());
		}

		if(Utils.isNotBlank(layerConfig.getLegendTitle())) {
			jsonLayer.put("legendTitle", layerConfig.getLegendTitle());
		}

		String[] infoHtmlUrls = layerConfig.getInfoHtmlUrls();
		if(infoHtmlUrls != null && infoHtmlUrls.length > 0) {
			jsonLayer.put("infoHtmlUrls", infoHtmlUrls);
		}

		String[] aliasIds = layerConfig.getAliasIds();
		if (aliasIds != null && aliasIds.length > 0) {
			jsonLayer.put("aliasIds", aliasIds);
		}

		if (Utils.isNotBlank(layerConfig.getExtraWmsServiceUrls())) {
			jsonLayer.put("extraWmsServiceUrls", layerConfig.getExtraWmsServiceUrls());
		}

		if (Utils.isNotBlank(layerConfig.getWmsRequestMimeType())) {
			jsonLayer.put("wmsRequestMimeType", layerConfig.getWmsRequestMimeType());
		}

		String[] wmsFeatureRequestLayers = layerConfig.getWmsFeatureRequestLayers();
		if (wmsFeatureRequestLayers != null && wmsFeatureRequestLayers.length > 0) {
			jsonLayer.put("wmsFeatureRequestLayers", wmsFeatureRequestLayers);
		}

		if(layerConfig.isWmsTransectable() != null) {
			jsonLayer.put("wmsTransectable", layerConfig.isWmsTransectable());
		}

		List<LayerStyleConfig> styles = layerConfig.getStyles();
		// Browsers do not have to keep the order in JavaScript objects, but they often do.
		if (styles != null && !styles.isEmpty()) {
			JSONObject jsonStyles = new JSONObject();
			if (!styles.isEmpty()) {
				boolean firstStyle = true;
				for (LayerStyleConfig style : styles) {
					String styleName = style.getName();
					if (firstStyle) {
						firstStyle = false;
						styleName = "";
					}
					if (styleName != null) {
						JSONObject jsonStyle = this.generateLayerStyle(style);
						if (jsonStyle != null && jsonStyle.length() > 0) {
							jsonStyles.put(styleName, jsonStyle);
						}
					}
				}
			}
			if (jsonStyles.length() > 0) {
				jsonLayer.put("styles", jsonStyles);
			}
		}

		List<LayerOptionConfig> options = layerConfig.getOptions();
		if (options != null && !options.isEmpty()) {
			JSONArray optionsArray = new JSONArray();

			for (LayerOptionConfig option : options) {
				JSONObject jsonOption = this.generateLayerOption(option);
				if (jsonOption != null && jsonOption.length() > 0) {
					optionsArray.put(jsonOption);
				}
			}

			if (optionsArray.length() > 0) {
				jsonLayer.put("layerOptions", optionsArray);
			}
		}

		/*
		XmlLayer.XmlLayerInitialState initialState = xmlLayer.getInitialState();
		if (initialState != null) {
			Map<String, Object> initialStateAtt = new HashMap<String, Object>();
			if (initialState.getLoaded() != null) {
				initialStateAtt.put("<loaded>", initialState.getLoaded());
			}

			if (initialState.getActivated() != null) {
				initialStateAtt.put("<activated>", initialState.getActivated());
			}
			if (initialState.getLegendActivated() != null) {
				initialStateAtt.put("<legendActivated>", initialState.getLegendActivated());
			}

			if (initialState.getOpacity() != null) {
				initialStateAtt.put("<opacity>", initialState.getOpacity());
			}

			layerAtt.put("<initialState>", initialStateAtt);
		}
		*/

		return jsonLayer;
	}

	private JSONObject generateLayerStyle(LayerStyleConfig style) throws JSONException {
		if (style == null) {
			return null;
		}

		JSONObject jsonStyle = new JSONObject();
		if (style.isDefault() != null) {
			jsonStyle.put("default", style.isDefault());
		}

		if (Utils.isNotBlank(style.getTitle())) {
			jsonStyle.put("title", style.getTitle());
		}

		if (Utils.isNotBlank(style.getDescription())) {
			jsonStyle.put("description", style.getDescription());
		}

		//if (Utils.isNotBlank(style.getLegendUrl())) {
		//	jsonStyle.put("legendUrl", style.getLegendUrl());
		//}

		//if (Utils.isNotBlank(style.getLegendFilename())) {
		//	jsonStyle.put("legendFilename", style.getLegendFilename());
		//}

		return jsonStyle;
	}

	/**
	 * "layerOptions": [
	 *     {
	 *         "name": "String, mandatory: name of the url parameter",
	 *         "title": "String, optional (default: name): title displayed in the layer options",
	 *         "type": "String, optional (default: text): type of the parameter, to specify which UI to use",
	 *         "mandatory": "Boolean, optional (default: false): set to true is the field is not allow to contains an empty string"
	 *         "defaultValue": "String, optional (default: empty string): default value"
	 *     },
	 *     ...
	 * ]
	 */
	private JSONObject generateLayerOption(LayerOptionConfig option) throws JSONException {
		if (option == null) {
			return null;
		}

		JSONObject jsonOption = new JSONObject();
		if (Utils.isNotBlank(option.getName())) {
			jsonOption.put("name", option.getName());
		}

		if (Utils.isNotBlank(option.getTitle())) {
			jsonOption.put("title", option.getTitle());
		}

		if (Utils.isNotBlank(option.getType())) {
			jsonOption.put("type", option.getType());
		}

		if (option.isMandatory() != null) {
			jsonOption.put("mandatory", option.isMandatory());
		}

		if (Utils.isNotBlank(option.getDefaultValue())) {
			jsonOption.put("defaultValue", option.getDefaultValue());
		}

		return jsonOption;
	}

	private void saveGeneratedConfigs(
			ClientConfig clientConfig,
			JSONObject fullConfig,
			JSONObject embeddedConfig,
			JSONObject layers) throws JSONException, IOException {

		File fullClientFile = this.getClientFullConfigFile(clientConfig);
		if (fullClientFile == null) {
			throw new IllegalArgumentException("No file provided for the Full client configuration.");
		} else {
			this.saveJSONConfig(fullConfig, fullClientFile);
		}

		File embeddedClientFile = this.getClientEmbeddedConfigFile(clientConfig);
		if (embeddedClientFile == null) {
			throw new IllegalArgumentException("No file provided for the Embedded client configuration.");
		} else {
			this.saveJSONConfig(embeddedConfig, embeddedClientFile);
		}

		File layersClientFile = this.getClientLayersConfigFile(clientConfig);
		if (layersClientFile == null) {
			throw new IllegalArgumentException("No file provided for the layers configuration.");
		} else {
			this.saveJSONConfig(layers, layersClientFile);
		}
	}

	private synchronized void saveJSONConfig(JSONObject config, File file) throws JSONException, IOException {
		if (config == null || file == null) {
			return;
		}

		// If the application can write in the file
		// NOTE: An unexisting file is not considered as writable.
		if (file.canWrite() || (!file.exists() && file.getParentFile().canWrite())) {
			FileWriter writer = null;
			try {
				writer = new FileWriter(file);
				this.saveJSONConfig(config, writer);
			} finally {
				try {
					if (writer != null) { writer.close(); }
				} catch(Exception e) {}
			}
		} else {
			LOGGER.log(Level.SEVERE, "The application can not write in the configuration file [{0}].", file.getAbsolutePath());
			throw new IOException("The application can not write in the configuration file [" + file.getAbsolutePath() + "].");
		}
	}

	private synchronized void saveJSONConfig(JSONObject config, Writer writer) throws JSONException, IOException {
		if (config == null || writer == null) {
			return;
		}
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(writer);
			String jsonStr = Utils.jsonToStr(config);
			if (Utils.isNotBlank(jsonStr)) {
				bw.write(jsonStr);
			}
		} finally {
			try {
				if (bw != null) { bw.close(); }
			} catch (Exception e) {}
		}
	}

	private synchronized void saveJSONConfig(JSONArray config, Writer writer) throws JSONException, IOException {
		if (config == null || writer == null) {
			return;
		}
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(writer);
			String jsonStr = Utils.jsonToStr(config);
			if (Utils.isNotBlank(jsonStr)) {
				bw.write(jsonStr);
			}
		} finally {
			try {
				if (bw != null) { bw.close(); }
			} catch (Exception e) {}
		}
	}

	private JSONObject loadExistingConfig(File configFile) throws JSONException, IOException {
		if (configFile == null) {
			return null;
		}

		JSONObject existingConfig = null;
		if (configFile.canRead()) {
			Reader reader = null;
			try {
				reader = new FileReader(configFile);
				existingConfig = new JSONObject(new JSONTokener(reader));
			} finally {
				try {
					if (reader != null) {reader.close();}
				} catch(Exception e) {}
			}
		} else {
			LOGGER.log(Level.SEVERE, "Can read the configuration file [{0}]", configFile.getAbsolutePath());
		}

		return existingConfig;
	}

	private Integer getNextDataSourceId() throws JSONException, FileNotFoundException {
		while (this.dataSourceConfigs.containsKey1(this.lastDataSourceId)) {
			this.lastDataSourceId++;
		}
		return this.lastDataSourceId;
	}
	private Integer getNextClientId() {
		while (this.clientConfigs.containsKey1(this.lastClientId)) {
			this.lastClientId++;
		}
		return this.lastClientId;
	}

	private JSONArray getPostedData(ServletRequest request) throws JSONException {
		JSONArray dataJSonArr = null;
		JSONObject postedJSonObj = this.getPostedJSon(request);
		if (postedJSonObj != null) {
			dataJSonArr = postedJSonObj.optJSONArray("data");
			if (dataJSonArr == null) {
				JSONObject dataJSonObj = postedJSonObj.optJSONObject("data");
				if (dataJSonObj != null) {
					dataJSonArr = new JSONArray().put(dataJSonObj);
				}
			}
		}
		return dataJSonArr;
	}

	private JSONObject getPostedJSon(ServletRequest request) throws JSONException {
		StringBuilder jsonStrBuf = new StringBuilder();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(request.getReader());
			String text = null;
			while ((text = reader.readLine()) != null) {
				jsonStrBuf.append(text);
			}
		} catch (Exception ex) {
			Logger.getLogger(ConfigManager.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			try {
				if (reader != null) { reader.close(); }
			} catch (Exception ex) {}
		}

		JSONObject jsonObj = null;
		String jsonStr = jsonStrBuf.toString();
		if (jsonStr != null && jsonStr.length() > 0) {
			jsonObj = new JSONObject(jsonStrBuf.toString());
		}
		return jsonObj;
	}
}
