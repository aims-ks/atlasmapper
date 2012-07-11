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
import au.gov.aims.atlasmapperserver.dataSourceConfig.AbstractDataSourceConfig;
import au.gov.aims.atlasmapperserver.dataSourceConfig.AbstractDataSourceConfigInterface;
import au.gov.aims.atlasmapperserver.dataSourceConfig.ArcGISMapServerDataSourceConfigInterface;
import au.gov.aims.atlasmapperserver.dataSourceConfig.BingDataSourceConfig;
import au.gov.aims.atlasmapperserver.dataSourceConfig.DataSourceConfigHelper;
import au.gov.aims.atlasmapperserver.dataSourceConfig.WMSDataSourceConfig;
import au.gov.aims.atlasmapperserver.layerConfig.AbstractLayerConfig;
import au.gov.aims.atlasmapperserver.layerConfig.ArcGISCacheLayerConfig;
import au.gov.aims.atlasmapperserver.layerConfig.ArcGISMapServerLayerConfig;
import au.gov.aims.atlasmapperserver.layerConfig.GroupLayerConfig;
import au.gov.aims.atlasmapperserver.layerConfig.KMLLayerConfig;
import au.gov.aims.atlasmapperserver.layerConfig.LayerOptionConfig;
import au.gov.aims.atlasmapperserver.layerConfig.LayerStyleConfig;
import au.gov.aims.atlasmapperserver.layerConfig.WMSLayerConfig;
import au.gov.aims.atlasmapperserver.layerGenerator.AbstractLayerGenerator;
import au.gov.aims.atlasmapperserver.servlet.FileFinder;
import au.gov.aims.atlasmapperserver.servlet.Proxy;
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
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;

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
	private static final String SERVER_DEFAULT_CONFIG_FILENAME = "defaultServer.json";
	private static final String USERS_DEFAULT_CONFIG_FILENAME = "defaultUsers.json";

	private static final String CONFIG_VERSION_KEY = "version";

	// Used to generate and parse server config
	private static final double CURRENT_SERVER_CONFIG_VERSION = 1.0;
	private static final double CURRENT_USERS_CONFIG_VERSION = 1.0;

	// Used to generated clients config
	// NOTE: The version must match the version in the client /clientResources/amc/modules/Core/Core.js
	private static final double CURRENT_MAIN_CONFIG_VERSION = 1.1;
	private static final double CURRENT_LAYER_CONFIG_VERSION = 1.1;

	// Will eventually be used for backward compatibility
	private double configVersion;
	private double usersConfigVersion;

	private File serverConfigFile = null;
	private long serverConfigFileLastModified = -1;

	private File usersConfigFile = null;
	private long usersConfigFileLastModified = -1;

	// Set by the ConfigHelper
	private String clientMainConfigFilename = null;
	private String clientEmbeddedConfigFilename = null;
	private String clientLayersConfigFilename = null;
	private File applicationFolder = null;

	private static final String DATASOURCES_KEY = "dataSources";
	private static final String CLIENTS_KEY = "clients";

	// Demo: Atlas Mapper can run as a Demo application, with limited feature for better security.
	// The value can only be set by modifying the "server.json" config file manually.
	private static final String DEMO_KEY = "demoMode";

	private Boolean demoMode = null;

	private String defaultProxyUrl = null;
	private String defaultLayerInfoServiceUrl = null;

	private int lastDataSourceId;
	private int lastClientId;

	private Map<String, User> users = null;
	private MultiKeyHashMap<Integer, String, AbstractDataSourceConfig> dataSourceConfigs = null;
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
	public double getConfigVersion() {
		return this.configVersion;
	}

	public File getUsersConfigFile() {
		return this.usersConfigFile;
	}
	public double getUsersConfigVersion() {
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

	public File getClientMainConfigFile(ClientConfig clientConfig) {
		if (this.clientMainConfigFilename == null) {
			return null;
		}
		File clientConfigFolder = this.getClientConfigFolder(clientConfig);
		if (clientConfigFolder == null) {
			return null;
		}
		return new File(clientConfigFolder, this.clientMainConfigFilename);
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

	public void setClientMainConfigFilename(String clientMainConfigFilename) {
		this.clientMainConfigFilename = clientMainConfigFilename;
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
					if (serverConfigReader != null) {
						try {
							serverConfigReader.close();
						} catch(Exception e) {
							LOGGER.log(Level.SEVERE, "Can not close the server config file", e);
						}
					}
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
			LOGGER.log(Level.WARNING, "Malformed AtlasMapper JSON config file. The configuration file can not be parsed.", ex);
			return;
		}

		this.demoMode = jsonObj.optBoolean(DEMO_KEY, false);
		this.configVersion = jsonObj.optDouble(CONFIG_VERSION_KEY, CURRENT_SERVER_CONFIG_VERSION);

		if (this.configVersion > CURRENT_SERVER_CONFIG_VERSION) {
			throw new UnsupportedClassVersionError("The version of the server configuration file ("+this.configVersion+") is not supported by this server (support up to version: "+CURRENT_SERVER_CONFIG_VERSION+").");
		}

		this.dataSourceConfigs = new MultiKeyHashMap<Integer, String, AbstractDataSourceConfig>();
		this.lastDataSourceId = 0;
		JSONArray dataSourceConfigsArray = jsonObj.optJSONArray(DATASOURCES_KEY);

		if (dataSourceConfigsArray != null) {
			for (int i=0; i<dataSourceConfigsArray.length(); i++) {
				JSONObject rawDataSourceConfig = dataSourceConfigsArray.optJSONObject(i);
				if (rawDataSourceConfig != null) {
					AbstractDataSourceConfig dataSourceConfig = DataSourceConfigHelper.createDataSourceConfig(rawDataSourceConfig, this);
					Integer dataSourceId = dataSourceConfig.getId();
					if (dataSourceId != null && dataSourceId > this.lastDataSourceId) {
						this.lastDataSourceId = dataSourceId;
					}
					this.dataSourceConfigs.put(
							dataSourceId,
							dataSourceConfig.getDataSourceId(),
							dataSourceConfig);
				} else {
					LOGGER.log(Level.WARNING, "Malformed AtlasMapper JSON config file: a data source is not set properly [{0}]", rawDataSourceConfig);
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
					LOGGER.log(Level.WARNING, "Malformed AtlasMapper JSON config file: a client is not set properly [{0}]", rawClientConfig);
				}
			}
		}

		// Prevent memory leak
		URLCache.purgeCache(this.dataSourceConfigs.values());
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
					if (usersConfigReader != null) {
						try {
							usersConfigReader.close();
						} catch(Exception e) {
							LOGGER.log(Level.SEVERE, "Can not close the users config file", e);
						}
					}
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

			this.usersConfigVersion = usersConfig.optDouble(CONFIG_VERSION_KEY, CURRENT_USERS_CONFIG_VERSION);
			if (this.usersConfigVersion > CURRENT_USERS_CONFIG_VERSION) {
				throw new UnsupportedClassVersionError("The version of the users configuration file ("+this.usersConfigVersion+") is not supported by this server (support up to version: "+CURRENT_USERS_CONFIG_VERSION+").");
			}

			JSONArray jsonUsers = usersConfig.optJSONArray("users");

			if (jsonUsers != null) {
				for (int i=0; i<jsonUsers.length(); i++) {
					JSONObject jsonUser = jsonUsers.optJSONObject(i);
					if (jsonUser != null) {
						User user = new User(this);
						user.update(jsonUser);

						String clearTextPassword = jsonUser.optString("password", null);
						if (clearTextPassword != null) {
							user.setPassword(clearTextPassword);
						}

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
				if (writer != null) {
					try {
						writer.close();
					} catch (Exception e) {
						LOGGER.log(Level.SEVERE, "Can not close the server config file", e);
					}
				}
				// Reload the configuration to refresh the state of the server with the config that is in the file
				try {
					this.reloadServerConfig();
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE, "Can not reload the server config file", e);
				}
			}
		} else {
			// Reload the configuration to refresh the state of the server with the config that is in the file
			try {
				this.reloadServerConfig();
			} catch(Exception e) {
				LOGGER.log(Level.SEVERE, "Can not reload the server config file", e);
			}
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

		config.put(CONFIG_VERSION_KEY, CURRENT_SERVER_CONFIG_VERSION);
		config.put(DATASOURCES_KEY, this._getDataSourceConfigsJSon(false));
		config.put(CLIENTS_KEY, this._getClientConfigsJSon(false));

		this.saveJSONConfig(config, serverConfigWriter);

		// Prevent memory leak
		URLCache.purgeCache(this.dataSourceConfigs.values());
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
				if (writer != null) {
					try {
						writer.close();
					} catch(Exception e) {
						LOGGER.log(Level.SEVERE, "Can not close the users config file", e);
					}
				}
				// Reload the configuration to refresh the state of the server with the config that is in the file
				try {
					this.reloadUsersConfig();
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE, "Can not reload the users config file", e);
				}
			}
		} else {
			// Reload the configuration to refresh the state of the server with the config that is in the file
			try {
				this.reloadUsersConfig();
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Can not reload the users config file", e);
			}
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
		usersConfig.put(CONFIG_VERSION_KEY, CURRENT_USERS_CONFIG_VERSION);
		usersConfig.put("users", jsonUsers);

		this.saveJSONConfig(usersConfig, usersConfigWriter);
	}

	public boolean isDemoMode() {
		try {
			this.reloadServerConfigIfNeeded();
		} catch (Exception ex) {
			// This should not happen...
			LOGGER.log(Level.SEVERE, "Unexpected exception occurred while reloading the config. Fall back to demo mode.", ex);
			return true;
		}
		return this.demoMode;
	}

	public synchronized List<AbstractDataSourceConfig> createDataSourceConfig(ServletRequest request) throws JSONException, FileNotFoundException {
		if (request == null) {
			return null;
		}

		MultiKeyHashMap<Integer, String, AbstractDataSourceConfig> configs = this.getDataSourceConfigs();
		List<AbstractDataSourceConfig> newDataSourceConfigs = new ArrayList<AbstractDataSourceConfig>();
		JSONArray dataJSonArr = this.getPostedData(request);
		if (dataJSonArr != null) {
			for (int i=0; i<dataJSonArr.length(); i++) {
				JSONObject dataJSonObj = dataJSonArr.optJSONObject(i);
				if (dataJSonObj != null) {
					if (dataJSonObj.isNull("id") || dataJSonObj.optString("id", "").length() <= 0) {
						dataJSonObj.put("id", this.getNextDataSourceId());
					}

					AbstractDataSourceConfig dataSourceConfig = DataSourceConfigHelper.createDataSourceConfig(dataJSonObj, this);

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

		MultiKeyHashMap<Integer, String, AbstractDataSourceConfig> configs = this.getDataSourceConfigs();
		JSONArray dataJSonArr = this.getPostedData(request);
		if (dataJSonArr != null) {
			for (int i=0; i<dataJSonArr.length(); i++) {
				JSONObject dataJSonObj = dataJSonArr.optJSONObject(i);
				if (dataJSonObj != null) {
					Integer dataSourceId = dataJSonObj.optInt("id", -1);
					AbstractDataSourceConfig dataSourceConfig = configs.get1(dataSourceId);
					if (dataSourceConfig != null) {
						// Clear dataSource cache before modifying it
						URLCache.clearCache(dataSourceConfig);

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

		MultiKeyHashMap<Integer, String, AbstractDataSourceConfig> configs = this.getDataSourceConfigs();
		JSONArray dataJSonArr = this.getPostedData(request);
		if (dataJSonArr != null) {
			for (int i=0; i<dataJSonArr.length(); i++) {
				JSONObject dataJSonObj = dataJSonArr.optJSONObject(i);

				if (dataJSonObj != null) {
					Integer dataSourceId = dataJSonObj.optInt("id", -1);
					AbstractDataSourceConfig dataSourceConfig = configs.remove1(dataSourceId);

					// Clear dataSource cache since it doesn't exist anymore
					URLCache.clearCache(dataSourceConfig);
				}
			}
		}
	}

	private void ensureUniqueness(AbstractDataSourceConfig dataSource) throws FileNotFoundException, JSONException {
		MultiKeyHashMap<Integer, String, AbstractDataSourceConfig> _dataSourceConfigs = getDataSourceConfigs();

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
		MultiKeyHashMap<Integer, String, AbstractDataSourceConfig> _dataSourceConfigs = getDataSourceConfigs();
		AbstractDataSourceConfig found = _dataSourceConfigs.get2(dataSourceId);

		// Most common case, the data source is new or it's data source ID has changed.
		if (found == null) { return false; }

		// Security: This case should not happen as long as the server.json file is valid.
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
		// Reserved keywords
		if (FileFinder.PUBLIC_FOLDER.equals(clientId) ||
				ConfigHelper.SERVER_MAIN_CONFIG.equals(clientId) ||
				ConfigHelper.SERVER_USERS_CONFIG.equals(clientId)) {

			return true;
		}

		MultiKeyHashMap<Integer, String, ClientConfig> _clientConfigs = getClientConfigs();
		ClientConfig found = _clientConfigs.get2(clientId);

		// Most common case, the client is new or it's client ID has changed.
		if (found == null) { return false; }

		// Security: This case should not happen as long as the server.json file is valid.
		if (found.getId() == null) { return true; }

		// Same client ID AND Integer ID => Same client
		if (found.getId().equals(id)) { return false; }

		// We found a client with the same client ID but with a different Integer ID.
		return true;
	}

	public JSONObject getClientLayers(ClientConfig clientConfig, String[] layerIds) throws GetCapabilitiesExceptions, Exception {
		if (clientConfig == null) { return null; }

		return (JSONObject)_getClientLayers(null, clientConfig, Arrays.asList(layerIds), false, "JSONObject");
	}

	// Sometime, layers are required to be in an Array to keep their order
	private Object _getClientLayers(Map<String, AbstractLayerConfig> layers, ClientConfig clientConfig, Collection<String> layerIds, boolean live, String jsonClass) throws GetCapabilitiesExceptions, Exception {
		if (clientConfig == null) { return null; }

		JSONObject foundLayersObj = new JSONObject();
		JSONArray foundLayersArr = new JSONArray();

		JSONObject clientLayers = this.getClientConfigFileJSon(layers, clientConfig, ConfigType.LAYERS, live, live);

		boolean asJSONObject = !"JSONArray".equalsIgnoreCase(jsonClass);
		for (String rawLayerId : layerIds) {
			String layerId = rawLayerId.trim();
			if (clientLayers.has(layerId)) {
				JSONObject jsonLayer = clientLayers.optJSONObject(layerId);
				if (asJSONObject) {
					foundLayersObj.put(layerId, jsonLayer);
				} else {
					jsonLayer.put("layerId", layerId);
					foundLayersArr.put(jsonLayer);
				}
			}
		}

		if (asJSONObject) {
			return foundLayersObj;
		}
		return foundLayersArr;
	}

	public MultiKeyHashMap<Integer, String, AbstractDataSourceConfig> getDataSourceConfigs() throws JSONException, FileNotFoundException {
		this.reloadServerConfigIfNeeded();
		return this.dataSourceConfigs;
	}

	public AbstractDataSourceConfig getDataSourceConfig(Integer dataSourceId) throws JSONException, FileNotFoundException {
		if (dataSourceId == null) {
			return null;
		}

		MultiKeyHashMap<Integer, String, AbstractDataSourceConfig> configs = this.getDataSourceConfigs();
		if (configs == null) {
			return null;
		}

		return configs.get1(dataSourceId);
	}

	public AbstractDataSourceConfig getDataSourceConfig(String dataSourceId) throws JSONException, FileNotFoundException {
		if (Utils.isBlank(dataSourceId)) {
			return null;
		}

		MultiKeyHashMap<Integer, String, AbstractDataSourceConfig> configs = this.getDataSourceConfigs();
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

		MultiKeyHashMap<Integer, String, AbstractDataSourceConfig> configs = reload ? this.getDataSourceConfigs() : this.dataSourceConfigs;
		for (AbstractDataSourceConfig dataSourceConfig : configs.values()) {
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
			throws GetCapabilitiesExceptions, Exception {

		for (ClientConfig clientConfig : this.getClientConfigs().values()) {
			this._generateClient(clientConfig, complete);
		}
	}

	public void generateClient(Integer clientId, boolean complete)
			throws GetCapabilitiesExceptions, Exception {

		if (clientId == null) {
			return;
		}

		this._generateClient(this.getClientConfigs().get1(clientId), complete);
	}

	public void generateClient(ClientConfig clientConfig, boolean complete)
			throws GetCapabilitiesExceptions, Exception {

		this._generateClient(clientConfig, complete);
	}

	private void _generateClient(ClientConfig clientConfig, boolean complete)
			throws GetCapabilitiesExceptions, Exception {

		if (clientConfig == null) {
			return;
		}

		Map<String, AbstractLayerConfig> layers = clientConfig.generateLayerConfigs();

		boolean useGoogle = clientConfig.useGoogle(this);

		this.copyClientFilesIfNeeded(clientConfig, complete);
		JSONObject generatedMainConfig = this.getClientConfigFileJSon(layers, clientConfig, ConfigType.MAIN, false, true);
		JSONObject generatedEmbeddedConfig = this.getClientConfigFileJSon(layers, clientConfig, ConfigType.EMBEDDED, false, true);
		JSONObject generatedLayers = this.getClientConfigFileJSon(layers, clientConfig, ConfigType.LAYERS, false, true);
		this.parseTemplates(layers, generatedMainConfig, clientConfig, useGoogle);
		this.saveGeneratedConfigs(clientConfig, generatedMainConfig, generatedEmbeddedConfig, generatedLayers);

		// Flush the proxy cache for both the preview and the generated clients
		Proxy.reloadConfig(generatedMainConfig, generatedLayers, clientConfig.getClientId(), true);
		Proxy.reloadConfig(generatedMainConfig, generatedLayers, clientConfig.getClientId(), false);
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
	private void parseTemplates(Map<String, AbstractLayerConfig> layers, JSONObject generatedMainConfig, ClientConfig clientConfig, boolean useGoogle) throws IOException, TemplateException {
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
			indexValues.put("mainConfig", this.clientMainConfigFilename);
			indexValues.put("layersConfig", this.clientLayersConfigFilename);
			indexValues.put("clientId", clientConfig.getClientId());
			indexValues.put("clientName", clientConfig.getClientName() != null ? clientConfig.getClientName() : clientConfig.getClientId());
			indexValues.put("theme", clientConfig.getTheme());
			indexValues.put("pageHeader", Utils.safeJsStr(clientConfig.getPageHeader()));
			indexValues.put("pageFooter", Utils.safeJsStr(clientConfig.getPageFooter()));
			indexValues.put("timestamp", ""+Utils.getCurrentTimestamp());
			indexValues.put("useGoogle", useGoogle);
			indexValues.put("welcomeMsg", clientConfig.getWelcomeMsg());
			Utils.processTemplate(templatesConfig, "index.html", indexValues, atlasMapperClientFolder);

			Map<String, Object> embeddedValues = new HashMap<String, Object>();
			embeddedValues.put("version", ProjectInfo.getVersion());
			embeddedValues.put("mainConfig", this.clientMainConfigFilename);
			embeddedValues.put("layersConfig", this.clientLayersConfigFilename);
			embeddedValues.put("clientId", clientConfig.getClientId());
			embeddedValues.put("clientName", clientConfig.getClientName() != null ? clientConfig.getClientName() : clientConfig.getClientId());
			embeddedValues.put("theme", clientConfig.getTheme());
			embeddedValues.put("pageHeader", Utils.safeJsStr(clientConfig.getPageHeader()));
			embeddedValues.put("pageFooter", Utils.safeJsStr(clientConfig.getPageFooter()));
			embeddedValues.put("timestamp", ""+Utils.getCurrentTimestamp());
			embeddedValues.put("useGoogle", useGoogle);
			// No welcome message
			Utils.processTemplate(templatesConfig, "embedded.html", embeddedValues, atlasMapperClientFolder);

			int width = 200;
			int height = 180;
			if (Utils.isNotBlank(clientConfig.getListLayerImageWidth())) {
				width = Integer.valueOf(clientConfig.getListLayerImageWidth());
			}
			if (Utils.isNotBlank(clientConfig.getListLayerImageHeight())) {
				height = Integer.valueOf(clientConfig.getListLayerImageHeight());
			}

			Map<String, Object> listValues = new HashMap<String, Object>();
			listValues.put("version", ProjectInfo.getVersion());
			listValues.put("clientName", clientConfig.getClientName() != null ? clientConfig.getClientName() : clientConfig.getClientId());
			listValues.put("layers", this.generateLayerList(clientConfig, layers, generatedMainConfig));
			listValues.put("layerBoxWidth", width + 2); // +2 for the 1 px border - This value can be overridden using CSS
			listValues.put("layerBoxHeight", height + 45); // +45 to let some room for the text bellow the layer - This value can be overridden using CSS
			listValues.put("listPageHeader", clientConfig.getListPageHeader());
			listValues.put("listPageFooter", clientConfig.getListPageFooter());
			Utils.processTemplate(templatesConfig, "fullList.html", listValues, atlasMapperClientFolder);
			Utils.processTemplate(templatesConfig, "list.html", listValues, atlasMapperClientFolder);

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
				previewValues.put("pageHeader", Utils.safeJsStr(clientConfig.getPageHeader()));
				previewValues.put("pageFooter", Utils.safeJsStr(clientConfig.getPageFooter()));
				previewValues.put("useGoogle", useGoogle);
				previewValues.put("welcomeMsg", clientConfig.getWelcomeMsg());
				Utils.processTemplate(templatesConfig, "preview.html", previewValues, atlasMapperClientFolder);
			} catch (URISyntaxException ex) {
				throw new IOException("Can not get a File reference to the AtlasMapperClient", ex);
			}
		}
	}

	/**
	 * Return a list of info used to generate a list of layers:
	 * Map of
	 *     Key: DataSource name
	 *     Value: List of Map of
	 *         id: Layer ID, as used in the AtlasMapper (with the data source ID)
	 *         title: Displayed name of the layer
	 *         description: Displayed name of the layer
	 *         imageUrl: URL of the preview image for the layer
	 *         baseLayerUrl: URL of the background image to display under the layer image
	 *         imageWidth: Image width
	 *         imageHeight: Image height
	 *         mapUrl: URL of the AtlasMapper map that display that layer
	 * @param layers Map of layers, after overrides
	 * @param generatedMainConfig Client JSON config, to get the data sources (after overrides), the client projection and the default layers.
	 * @return
	 */
	private Map<String, List<Map<String, String>>> generateLayerList(ClientConfig clientConfig, Map<String, AbstractLayerConfig> layers, JSONObject generatedMainConfig) throws UnsupportedEncodingException {
		if (layers == null || layers.isEmpty()) {
			return null;
		}
		Map<String, List<Map<String, String>>> layersMap = new LinkedHashMap<String, List<Map<String, String>>>();

		// Order the layers in alphabetic order
		// Order it according to it's layer ID; it's easier that way...
		List<String> orderedLayerIds = new ArrayList<String>(layers.keySet());
		Collections.sort(orderedLayerIds);

		JSONObject dataSources = generatedMainConfig.optJSONObject("dataSources");
		String projection = "EPSG:4326";

		// Maximum width x height
		int defaultWidth = 200;
		int defaultHeight = 180;
		if (Utils.isNotBlank(clientConfig.getListLayerImageWidth())) {
			defaultWidth = Integer.valueOf(clientConfig.getListLayerImageWidth());
		}
		if (Utils.isNotBlank(clientConfig.getListLayerImageHeight())) {
			defaultHeight = Integer.valueOf(clientConfig.getListLayerImageHeight());
		}

		for (String layerId : orderedLayerIds) {
			AbstractLayerConfig layerConfig = layers.get(layerId);

			// Ignore layer groups
			if (layerConfig.getLayerName() != null) {
				// Data source object containing overridden values
				JSONObject dataSource = null;
				// Raw data source object containing values before override

				AbstractDataSourceConfig rawDataSourceConfig = null;
				if (dataSources != null) {
					dataSource = dataSources.optJSONObject(layerConfig.getDataSourceId());
					rawDataSourceConfig = this.dataSourceConfigs.get2(layerConfig.getDataSourceId());
				}

				String dataSourceName = rawDataSourceConfig.getDataSourceName();

				// Find (or create) the layer list for this data source
				List<Map<String, String>> dataSourceLayerList = layersMap.get(dataSourceName);
				if (dataSourceLayerList == null) {
					dataSourceLayerList = new ArrayList<Map<String, String>>();
					layersMap.put(dataSourceName, dataSourceLayerList);
				}

				Map<String, String> layerMap = new HashMap<String, String>();


				String serviceUrl = layerConfig.getServiceUrl();
				if (serviceUrl == null || serviceUrl.isEmpty()) {
					if (dataSource != null) {
						serviceUrl = dataSource.optString("wmsServiceUrl", null);
					}
				}

				// http://e-atlas.localhost/maps/ea/wms
				// LAYERS=ea%3AQLD_DEEDI_Coastal-wetlands
				// FORMAT=image%2Fpng
				// SERVICE=WMS
				// VERSION=1.1.1
				// REQUEST=GetMap
				// EXCEPTIONS=application%2Fvnd.ogc.se_inimage
				// SRS=EPSG%3A4326
				// BBOX=130.20938085938,-37.1985,161.23261914062,-1.0165
				// WIDTH=439
				// HEIGHT=512
				if (serviceUrl != null && !serviceUrl.isEmpty()) {
					if (rawDataSourceConfig instanceof WMSDataSourceConfig) {
						double[] bbox = layerConfig.getLayerBoundingBox();
						if (bbox != null && bbox.length == 4) {
							StringBuilder imageUrl = new StringBuilder(serviceUrl);
							if (!serviceUrl.endsWith("&") && !serviceUrl.endsWith("?")) {
								imageUrl.append(serviceUrl.contains("?") ? "&" : "?");
							}
							imageUrl.append("LAYERS="); imageUrl.append(URLEncoder.encode(layerConfig.getLayerName(), "UTF-8"));
							imageUrl.append("&STYLES="); // Some servers need this parameter, even set to nothing
							imageUrl.append("&FORMAT="); imageUrl.append(URLEncoder.encode("image/png", "UTF-8"));
							imageUrl.append("&TRANSPARENT=true");
							imageUrl.append("&SERVICE=WMS");
							imageUrl.append("&VERSION=1.1.1");
							imageUrl.append("&REQUEST=GetMap");
							imageUrl.append("&EXCEPTIONS="); imageUrl.append(URLEncoder.encode("application/vnd.ogc.se_inimage", "UTF-8"));
							imageUrl.append("&SRS="); imageUrl.append(URLEncoder.encode(projection, "UTF-8")); // TODO Use client projection

							imageUrl.append("&BBOX=");
							imageUrl.append(bbox[0]); imageUrl.append(",");
							imageUrl.append(bbox[1]); imageUrl.append(",");
							imageUrl.append(bbox[2]); imageUrl.append(",");
							imageUrl.append(bbox[3]);

							// Lon x Lat ratio (width / height  or  lon / lat)
							double ratio = (bbox[2] - bbox[0]) / (bbox[3] - bbox[1]);

							int width = defaultWidth;
							int height = defaultHeight;

							if (ratio > (((double)width)/height)) {
								// Reduce height
								height = (int)Math.round(width / ratio);
							} else {
								// Reduce width
								width = (int)Math.round(height * ratio);
							}

							imageUrl.append("&WIDTH=" + width);
							imageUrl.append("&HEIGHT=" + height);

							layerMap.put("imageUrl", imageUrl.toString());
							layerMap.put("imageWidth", ""+width);
							layerMap.put("imageHeight", ""+height);

							String baseLayerServiceUrl = clientConfig.getListBaseLayerServiceUrl();
							String baseLayerId = clientConfig.getListBaseLayerId();
							if (Utils.isNotBlank(baseLayerServiceUrl) && Utils.isNotBlank(baseLayerId)) {
								// Base layer - Hardcoded
								// http://maps.e-atlas.org.au/maps/gwc/service/wms
								// LAYERS=ea%3AWorld_NED_NE2
								// TRANSPARENT=FALSE
								// SERVICE=WMS
								// VERSION=1.1.1
								// REQUEST=GetMap
								// FORMAT=image%2Fjpeg
								// SRS=EPSG%3A4326
								// BBOX=149.0625,-22.5,151.875,-19.6875
								// WIDTH=256
								// HEIGHT=256
								StringBuilder baseLayerUrl = new StringBuilder(baseLayerServiceUrl);
								if (!baseLayerServiceUrl.endsWith("&") && !baseLayerServiceUrl.endsWith("?")) {
									baseLayerUrl.append(baseLayerServiceUrl.contains("?") ? "&" : "?");
								}
								baseLayerUrl.append("LAYERS="); baseLayerUrl.append(URLEncoder.encode(baseLayerId, "UTF-8"));
								baseLayerUrl.append("&STYLES="); // Some servers need this parameter, even set to nothing
								baseLayerUrl.append("&FORMAT="); baseLayerUrl.append(URLEncoder.encode("image/jpeg", "UTF-8"));
								baseLayerUrl.append("&TRANSPARENT=false");
								baseLayerUrl.append("&SERVICE=WMS");
								baseLayerUrl.append("&VERSION=1.1.1");
								baseLayerUrl.append("&REQUEST=GetMap");
								baseLayerUrl.append("&EXCEPTIONS="); baseLayerUrl.append(URLEncoder.encode("application/vnd.ogc.se_inimage", "UTF-8"));
								baseLayerUrl.append("&SRS="); baseLayerUrl.append(URLEncoder.encode(projection, "UTF-8")); // TODO Use client projection

								baseLayerUrl.append("&BBOX=");
								baseLayerUrl.append(bbox[0]); baseLayerUrl.append(",");
								baseLayerUrl.append(bbox[1]); baseLayerUrl.append(",");
								baseLayerUrl.append(bbox[2]); baseLayerUrl.append(",");
								baseLayerUrl.append(bbox[3]);

								baseLayerUrl.append("&WIDTH=" + width);
								baseLayerUrl.append("&HEIGHT=" + height);

								layerMap.put("baseLayerUrl", baseLayerUrl.toString());
							}
						}
					}
				}

				layerMap.put("id", layerId);
				layerMap.put("title", layerConfig.getTitle());
				layerMap.put("description", layerConfig.getDescription());
				layerMap.put("mapUrl", "index.html?intro=f&dl=t&loc=" + URLEncoder.encode(layerId, "UTF-8") + "&l0=" + URLEncoder.encode(layerId, "UTF-8"));

				dataSourceLayerList.add(layerMap);
			}
		}

		return layersMap;
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
	 * "mainClient": {
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
			throws GetCapabilitiesExceptions, Exception {

		if (clientConfig == null) {
			return null;
		}

		Map<String, AbstractLayerConfig> layers = clientConfig.generateLayerConfigs();

		JSONObject debug = new JSONObject();

		JSONObject mainClientJSON = new JSONObject();
		mainClientJSON.put("current", Utils.jsonToStr(this.getClientConfigFileJSon(layers, clientConfig, ConfigType.MAIN, false, false)));
		mainClientJSON.put("generated", Utils.jsonToStr(this.getClientConfigFileJSon(layers, clientConfig, ConfigType.MAIN, true, true)));

		JSONObject embeddedClientJSON = new JSONObject();
		embeddedClientJSON.put("current", Utils.jsonToStr(this.getClientConfigFileJSon(layers, clientConfig, ConfigType.EMBEDDED, false, false)));
		embeddedClientJSON.put("generated", Utils.jsonToStr(this.getClientConfigFileJSon(layers, clientConfig, ConfigType.EMBEDDED, true, true)));

		JSONObject layersJSON = new JSONObject();
		layersJSON.put("current", Utils.jsonToStr(this.getClientConfigFileJSon(layers, clientConfig, ConfigType.LAYERS, false, false)));
		layersJSON.put("generated", Utils.jsonToStr(this.getClientConfigFileJSon(layers, clientConfig, ConfigType.LAYERS, true, true)));

		debug.put("mainClient", mainClientJSON);
		debug.put("embeddedClient", embeddedClientJSON);
		debug.put("layers", layersJSON);

		return debug;
	}

	public JSONObject getClientConfigFileJSon(ClientConfig clientConfig, ConfigType configType, boolean live, boolean generate)
			throws GetCapabilitiesExceptions, Exception {

		Map<String, AbstractLayerConfig> layers = null;
		if (generate) {
			layers = clientConfig.generateLayerConfigs();
		}
		return this.getClientConfigFileJSon(layers, clientConfig, configType, live, generate);
	}

	/**
	 *
	 * @param layers Map of layers, to avoid overhead with generation.
	 * @param clientConfig Client to generate
	 * @param configType Type of generation requested
	 * @param live True to use the list of allowed for the current config, false to use the list for the list of layer from the saved config file.
	 * @param generate True to generate a new configuration, false to get the configuration saved in the file.
	 * @return
	 * @throws GetCapabilitiesExceptions
	 * @throws Exception
	 */
	public JSONObject getClientConfigFileJSon(Map<String, AbstractLayerConfig> layers, ClientConfig clientConfig, ConfigType configType, boolean live, boolean generate)
			throws GetCapabilitiesExceptions, Exception {

		if (clientConfig == null || configType == null) { return null; }
		JSONObject mainConfig = null;
		JSONObject embeddedConfig = null;
		JSONObject fullConfig = null;
		JSONObject layersConfig = null;

		switch (configType) {
			case MAIN:
				if (generate) {
					mainConfig = this._generateAbstractClientConfig(layers, clientConfig);
					if (mainConfig != null) {
						JSONObject modules = this.generateModules(
								clientConfig.getMainClientModules(),
								clientConfig,
								layers
						);
						if (modules != null && modules.length() > 0) {
							mainConfig.put("modules", modules);
						}
					}
				} else {
					mainConfig = this.loadExistingConfig(this.getClientMainConfigFile(clientConfig));
				}

				this._setProxyUrl(mainConfig, clientConfig, live);
				return mainConfig;

			case EMBEDDED:
				if (generate) {
					embeddedConfig = this._generateAbstractClientConfig(layers, clientConfig);

					JSONObject modules = this.generateModules(
							clientConfig.getEmbeddedClientModules(),
							clientConfig,
							layers
					);
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
					layersConfig = new JSONObject();
					if (layers != null) {
						for (AbstractLayerConfig layerConfig : layers.values()) {
							JSONObject layerJSON = this.generateLayer(layerConfig);

							String layerId = layerConfig.getLayerId();
							if (layerId != null) {
								layerId = layerId.trim();
							}
							if (layerJSON != null) {
								layersConfig.put(layerId, layerJSON);
							}
						}
					}
					return layersConfig;
				} else {
					return this.loadExistingConfig(this.getClientLayersConfigFile(clientConfig));
				}

			case FULL:
				// FULL is only used by the Preview

				mainConfig = this.getClientConfigFileJSon(layers, clientConfig, ConfigType.MAIN, live, generate);
				layersConfig = this.getClientConfigFileJSon(layers, clientConfig, ConfigType.LAYERS, live, generate);

				if (live) {
					// Every time a preview is loaded, we reload the proxy cache for that preview.
					// The cache for preview only retain the latest state. If 2 preview instances of the same client
					// are loaded, one may eventually have a out of sync proxy, which may block some feature requests.
					Proxy.reloadConfig(mainConfig, layersConfig, clientConfig.getClientId(), live);
				}

				// Making a copy of the mainConfig variable (clone) would be better, but the variable is never used
				// after this, so it's faster (easier) to simply change it into the fullConfig.
				fullConfig = mainConfig;
				if (fullConfig != null) {
					fullConfig.put("layers", layersConfig);
				}
				return fullConfig;
		}
		return null;
	}

	// Use as a base for Main and Embedded config
	private JSONObject _generateAbstractClientConfig(Map<String, AbstractLayerConfig> layers, ClientConfig clientConfig)
			throws GetCapabilitiesExceptions, Exception {
		if (clientConfig == null) { return null; }

		JSONObject json = new JSONObject();
		json.put(CONFIG_VERSION_KEY, CURRENT_MAIN_CONFIG_VERSION);
		json.put("clientId", clientConfig.getClientId().trim());
		if (Utils.isNotBlank(clientConfig.getClientName())) {
			json.put("clientName", clientConfig.getClientName().trim());
		}

		// TODO Remove when the default saved state will be implemented
		json.put("defaultLayers", this.getClientDefaultLayers(layers, clientConfig.getClientId()));

		if (Utils.isNotBlank(clientConfig.getProjection())) {
			json.put("projection", clientConfig.getProjection().trim());
			json.put("mapOptions", Utils.getMapOptions(clientConfig.getProjection().trim()));
		}

		if (clientConfig.isUseLayerService()) {
			if (Utils.isNotBlank(clientConfig.getLayerInfoServiceUrl())) {
				json.put("layerInfoServiceUrl", clientConfig.getLayerInfoServiceUrl().trim());
			} else if (Utils.isNotBlank(this.defaultLayerInfoServiceUrl)) {
				json.put("layerInfoServiceUrl", this.defaultLayerInfoServiceUrl.trim());
			}
		}

		if (Utils.isNotBlank(clientConfig.getVersion())) {
			json.put("version", clientConfig.getVersion().trim());
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

		MultiKeyHashMap<Integer, String, AbstractDataSourceConfig> configs = this.getDataSourceConfigs();
		JSONArray dataSourcesArray = clientConfig.getDataSources();
		GetCapabilitiesExceptions errors = new GetCapabilitiesExceptions();
		if (dataSourcesArray != null && dataSourcesArray.length() > 0) {
			JSONObject dataSources = new JSONObject();
			for (int i=0; i<dataSourcesArray.length(); i++) {
				// https://github.com/douglascrockford/JSON-java/issues/24
				String dataSourceName = dataSourcesArray.optString(i, null);
				if (dataSourceName != null) {
					AbstractDataSourceConfig dataSourceConfig =
							configs.get2(dataSourceName);
					if (dataSourceConfig != null) {
						try {
							AbstractDataSourceConfig overridenDataSourceConfig = dataSourceConfig;

							// Apply overrides from the capabilities document
							AbstractLayerGenerator layersGenerator = dataSourceConfig.getLayerGenerator();
							if (layersGenerator != null) {
								overridenDataSourceConfig = layersGenerator.applyOverrides(
										dataSourceConfig);
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
		// NOTE The ExtJS theme, pageHeader and pageFooter are used to generate
		// the template, the client do not need to know those variables.
		if (Utils.isNotBlank(clientConfig.getLayersPanelHeader())) {
			json.put("layersPanelHeader", clientConfig.getLayersPanelHeader().trim());
		}
		if (Utils.isNotBlank(clientConfig.getLayersPanelFooter())) {
			json.put("layersPanelFooter", clientConfig.getLayersPanelFooter().trim());
		}

		return json;
	}

	private void _setProxyUrl(JSONObject clientJSON, ClientConfig clientConfig, boolean live) throws JSONException, UnsupportedEncodingException {
		if (clientConfig.isUseLayerService()) {
			if (Utils.isNotBlank(this.defaultProxyUrl)) {
				String proxyUrl = Utils.addUrlParameter(
						this.defaultProxyUrl.trim(),
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
			clientJSON.put("proxyUrl", clientConfig.getProxyUrl().trim());
		}
	}

	private JSONArray getClientDefaultLayers(Map<String, AbstractLayerConfig> layers, String clientId) throws GetCapabilitiesExceptions, Exception {
		if (Utils.isBlank(clientId)) { return null; }

		return (JSONArray)this._getClientLayers(
				layers,
				this.getClientConfig(clientId),
				this._getClientDefaultLayerIds(clientId),
				true,
				"JSONArray");
	}

	private List<String> _getClientDefaultLayerIds(String clientId) throws JSONException, IOException {
		if (Utils.isBlank(clientId)) { return null; }

		ClientConfig clientConfig = this.getClientConfig(clientId);
		if (clientConfig == null) { return null; }

		return clientConfig.getDefaultLayersList();
	}

	private JSONObject generateDataSource(AbstractDataSourceConfig dataSourceConfig, ClientConfig clientConfig) throws JSONException {
		JSONObject dataSource = this.generateDataSourceInterface(dataSourceConfig, clientConfig);

		if (dataSourceConfig instanceof WMSDataSourceConfig) {
			WMSDataSourceConfig wmsDataSourceConfig = (WMSDataSourceConfig)dataSourceConfig;

			if (Utils.isNotBlank(wmsDataSourceConfig.getGetMapUrl())) {
				// TODO remove wms from the property name
				dataSource.put("wmsServiceUrl", wmsDataSourceConfig.getGetMapUrl().trim());
			}

			if (Utils.isNotBlank(wmsDataSourceConfig.getExtraWmsServiceUrls())) {
				dataSource.put("extraWmsServiceUrls", wmsDataSourceConfig.getExtraWmsServiceUrls().trim());
			}

			if (Utils.isNotBlank(wmsDataSourceConfig.getWebCacheUrl())) {
				dataSource.put("webCacheUrl", wmsDataSourceConfig.getWebCacheUrl().trim());
			}

			String[] webCacheParametersArray = wmsDataSourceConfig.getWebCacheParametersArray();
			if (webCacheParametersArray != null && webCacheParametersArray.length > 0) {
				JSONArray webCacheParameters = new JSONArray(webCacheParametersArray);
				dataSource.put("webCacheSupportedParameters", webCacheParameters);
			}

			if (Utils.isNotBlank(wmsDataSourceConfig.getWmsVersion())) {
				dataSource.put("wmsVersion", wmsDataSourceConfig.getWmsVersion().trim());
			}
		}

		if (dataSourceConfig instanceof ArcGISMapServerDataSourceConfigInterface) {
			ArcGISMapServerDataSourceConfigInterface arcGISDataSource = (ArcGISMapServerDataSourceConfigInterface)dataSourceConfig;

			if (arcGISDataSource.isForcePNG24() != null) {
				dataSource.put("forcePNG24", arcGISDataSource.isForcePNG24());
			}
		}

		if (dataSourceConfig instanceof BingDataSourceConfig) {
			BingDataSourceConfig bingDataSource = (BingDataSourceConfig)dataSourceConfig;

			if (bingDataSource.getBingAPIKey() != null) {
				dataSource.put("bingAPIKey", bingDataSource.getBingAPIKey());
			}
		}

		return dataSource;
	}

	private JSONObject generateDataSourceInterface(AbstractDataSourceConfigInterface dataSourceInterface, ClientConfig clientConfig) throws JSONException {

		JSONObject dataSource = new JSONObject();

		if (Utils.isNotBlank(dataSourceInterface.getFeatureRequestsUrl())) {
			dataSource.put("featureRequestsUrl", dataSourceInterface.getFeatureRequestsUrl().trim());
		}

		if (Utils.isNotBlank(dataSourceInterface.getServiceUrl())) {
			// TODO remove wms from the property name
			dataSource.put("wmsServiceUrl", dataSourceInterface.getServiceUrl().trim());
		}

		if (Utils.isNotBlank(dataSourceInterface.getLegendUrl())) {
			dataSource.put("legendUrl", dataSourceInterface.getLegendUrl().trim());
		}

		if (Utils.isNotBlank(dataSourceInterface.getDataSourceName())) {
			dataSource.put("dataSourceName", dataSourceInterface.getDataSourceName().trim());
		}

		if (Utils.isNotBlank(dataSourceInterface.getDataSourceType())) {
			dataSource.put("dataSourceType", dataSourceInterface.getDataSourceType().trim());
		}

		JSONObject legendParameters = this.getDataSourceLegendParametersJson(dataSourceInterface.getLegendParameters());
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

		return dataSource;
	}

	private JSONObject getDataSourceLegendParametersJson(String legendParameters) throws JSONException {
		if (legendParameters == null) {
			return null;
		}

		String trimmedLegendParameters = legendParameters.trim();
		if (trimmedLegendParameters.isEmpty()) {
			return null;
		}

		JSONObject legendParametersJson = new JSONObject();
		for (String legendParameter : AbstractConfig.toSet(trimmedLegendParameters)) {
			if (Utils.isNotBlank(legendParameter)) {
				String[] attribute = legendParameter.split(AbstractConfig.SPLIT_ATTRIBUTES_PATTERN);
				if (attribute != null && attribute.length >= 2) {
					legendParametersJson.put(
							attribute[0].trim(),  // Key
							attribute[1].trim()); // Value
				}
			}
		}

		return legendParametersJson;
	}

	private JSONObject generateModules(JSONArray modulesArray, ClientConfig clientConfig, Map<String, AbstractLayerConfig> layers) throws JSONException {
		if (modulesArray != null && modulesArray.length() > 0) {
			JSONObject modules = new JSONObject();
			for (int i=0; i<modulesArray.length(); i++) {
				// https://github.com/douglascrockford/JSON-java/issues/24
				String moduleName = modulesArray.optString(i, null);
				if (moduleName != null) {
					JSONObject module = this.generateModule(moduleName, clientConfig, layers);
					if (module != null) {
						modules.put(moduleName, module);
					}
				}
			}

			return modules;
		}
		return null;
	}

	private JSONObject generateModule(String moduleConfig, ClientConfig clientConfig, Map<String, AbstractLayerConfig> layers) throws JSONException {
		JSONObject moduleJSONConfig =
				ModuleHelper.generateModuleConfiguration(moduleConfig, clientConfig, layers);

		if (moduleJSONConfig == null) {
			LOGGER.log(Level.SEVERE, "Can not generate the configuration for {0}", moduleConfig);
		}

		return moduleJSONConfig;
	}

	private JSONObject generateLayer(AbstractLayerConfig layerConfig) throws JSONException {
		// AbstractLayerConfig implements AbstractDataSourceConfigInterface
		JSONObject jsonLayer = this.generateDataSourceInterface(layerConfig, null);

		if (layerConfig instanceof WMSLayerConfig) {
			WMSLayerConfig wmsLayerConfig = (WMSLayerConfig)layerConfig;
			if (Utils.isNotBlank(wmsLayerConfig.getExtraWmsServiceUrls())) {
				jsonLayer.put("extraWmsServiceUrls", wmsLayerConfig.getExtraWmsServiceUrls().trim());
			}

			if (Utils.isNotBlank(wmsLayerConfig.getWebCacheUrl())) {
				jsonLayer.put("webCacheUrl", wmsLayerConfig.getWebCacheUrl().trim());
			}

			String[] webCacheParametersArray = wmsLayerConfig.getWebCacheParametersArray();
			if (webCacheParametersArray != null && webCacheParametersArray.length > 0) {
				JSONArray webCacheParameters = new JSONArray(webCacheParametersArray);
				jsonLayer.put("webCacheSupportedParameters", webCacheParameters);
			}

			if (Utils.isNotBlank(wmsLayerConfig.getWmsVersion())) {
				jsonLayer.put("wmsVersion", wmsLayerConfig.getWmsVersion().trim());
			}
		}

		if (layerConfig.isCached() != null) {
			jsonLayer.put("cached", layerConfig.isCached());
		}

		if (layerConfig.getOlParams() != null) {
			jsonLayer.put("olParams", layerConfig.getOlParams());
		}
		if (layerConfig.getOlOptions() != null) {
			jsonLayer.put("olOptions", layerConfig.getOlOptions());
		}

		jsonLayer.put(CONFIG_VERSION_KEY, CURRENT_LAYER_CONFIG_VERSION);

		String layerName = layerConfig.getLayerName();
		if (Utils.isNotBlank(layerName) && !layerName.equals(layerConfig.getLayerId())) {
			jsonLayer.put("layerName", layerName.trim());
		}

		if (Utils.isNotBlank(layerConfig.getTitle())) {
			jsonLayer.put("title", layerConfig.getTitle().trim());
		}

		if (Utils.isNotBlank(layerConfig.getDescription())) {
			jsonLayer.put("description", layerConfig.getDescription().trim());
		}
		if (Utils.isNotBlank(layerConfig.getHtmlDescription())) {
			jsonLayer.put("htmlDescription", layerConfig.getHtmlDescription().trim());
		}

		if (Utils.isNotBlank(layerConfig.getProjection())) {
			jsonLayer.put("projection", layerConfig.getProjection().trim());
		}

		// serverId
		if (Utils.isNotBlank(layerConfig.getDataSourceId())) {
			jsonLayer.put("dataSourceId", layerConfig.getDataSourceId().trim());
		}

		double[] boundingBox = layerConfig.getLayerBoundingBox();
		if (boundingBox != null && boundingBox.length > 0) {
			jsonLayer.put("layerBoundingBox", boundingBox);
		}

		if (layerConfig.isIsBaseLayer() != null) {
			jsonLayer.put("isBaseLayer", layerConfig.isIsBaseLayer());
		}

		if (layerConfig.isHasLegend() != null) {
			jsonLayer.put("hasLegend", layerConfig.isHasLegend());
		}

		// No need for a legend URL + Filename since there is no more Layer Groups
		if (Utils.isNotBlank(layerConfig.getLegendUrl())) {
			jsonLayer.put("legendUrl", layerConfig.getLegendUrl().trim());
		}

		if(Utils.isNotBlank(layerConfig.getLegendGroup())) {
			jsonLayer.put("legendGroup", layerConfig.getLegendGroup().trim());
		}

		if(Utils.isNotBlank(layerConfig.getLegendTitle())) {
			jsonLayer.put("legendTitle", layerConfig.getLegendTitle().trim());
		}

		String[] infoHtmlUrls = layerConfig.getInfoHtmlUrls();
		if(infoHtmlUrls != null && infoHtmlUrls.length > 0) {
			jsonLayer.put("infoHtmlUrls", infoHtmlUrls);
		}

		String[] aliasIds = layerConfig.getAliasIds();
		if (aliasIds != null && aliasIds.length > 0) {
			jsonLayer.put("aliasIds", aliasIds);
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
							jsonStyles.put(styleName.trim(), jsonStyle);
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

		// Initial state is related to the Client saved state
		// TODO delete after implementing Save State
		if(layerConfig.isSelected() != null) {
			jsonLayer.put("selected", layerConfig.isSelected());
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


		if (layerConfig instanceof WMSLayerConfig) {
			WMSLayerConfig wmsLayerConfig = (WMSLayerConfig)layerConfig;
			if(wmsLayerConfig.isWmsQueryable() != null) {
				jsonLayer.put("wmsQueryable", wmsLayerConfig.isWmsQueryable());
			}

			if(Utils.isNotBlank(wmsLayerConfig.getWmsVersion())) {
				jsonLayer.put("wmsVersion", wmsLayerConfig.getWmsVersion().trim());
			}

			if (Utils.isNotBlank(wmsLayerConfig.getExtraWmsServiceUrls())) {
				jsonLayer.put("extraWmsServiceUrls", wmsLayerConfig.getExtraWmsServiceUrls().trim());
			}

			if (Utils.isNotBlank(wmsLayerConfig.getWmsRequestMimeType())) {
				jsonLayer.put("wmsRequestMimeType", wmsLayerConfig.getWmsRequestMimeType().trim());
			}

			String[] wmsFeatureRequestLayers = wmsLayerConfig.getWmsFeatureRequestLayers();
			if (wmsFeatureRequestLayers != null && wmsFeatureRequestLayers.length > 0) {
				jsonLayer.put("wmsFeatureRequestLayers", wmsFeatureRequestLayers);
			}

			if(wmsLayerConfig.isWmsTransectable() != null) {
				jsonLayer.put("wmsTransectable", wmsLayerConfig.isWmsTransectable());
			}
		}

		// Specific layer fields
		if (layerConfig instanceof ArcGISMapServerLayerConfig) {
			ArcGISMapServerLayerConfig arcGISLayerConfig = (ArcGISMapServerLayerConfig)layerConfig;
			if(Utils.isNotBlank(arcGISLayerConfig.getArcGISPath())) {
				jsonLayer.put("arcGISPath", arcGISLayerConfig.getArcGISPath().trim());
			}

			if (layerConfig instanceof ArcGISCacheLayerConfig) {
				ArcGISCacheLayerConfig arcGISCacheLayerConfig = (ArcGISCacheLayerConfig)arcGISLayerConfig;
				if (arcGISCacheLayerConfig.getTileCols() != null && arcGISCacheLayerConfig.getTileRows() != null) {
					jsonLayer.put("arcGISCacheTileCols", arcGISCacheLayerConfig.getTileCols());
					jsonLayer.put("arcGISCacheTileRows", arcGISCacheLayerConfig.getTileRows());
				}
				if (arcGISCacheLayerConfig.getTileOriginX() != null && arcGISCacheLayerConfig.getTileOriginY() != null) {
					jsonLayer.put("arcGISCacheTileOriginX", arcGISCacheLayerConfig.getTileOriginX());
					jsonLayer.put("arcGISCacheTileOriginY", arcGISCacheLayerConfig.getTileOriginY());
				}
				if (arcGISCacheLayerConfig.getTileResolutions() != null) {
					jsonLayer.put("arcGISCacheTileResolutions", arcGISCacheLayerConfig.getTileResolutions());
				}
			}

		} else if (layerConfig instanceof GroupLayerConfig) {
			GroupLayerConfig groupLayerConfig = (GroupLayerConfig)layerConfig;
			if(Utils.isNotBlank(groupLayerConfig.getGroupPath())) {
				// TODO groupPath instead of arcGISPath
				jsonLayer.put("arcGISPath", groupLayerConfig.getGroupPath().trim());
			}
			String[] groupLayers = groupLayerConfig.getLayers();
			if (groupLayers != null && groupLayers.length > 0) {
				jsonLayer.put("layers", groupLayers);
			}

/*
		// TODO DELETE
		} else if (layerConfig instanceof FolderLayerConfig) {
			FolderLayerConfig folderLayerConfig = (FolderLayerConfig)layerConfig;
			if(Utils.isNotBlank(folderLayerConfig.getFolderPath())) {
				// TODO folderPath instead of arcGISPath
				jsonLayer.put("arcGISPath", folderLayerConfig.getFolderPath().trim());
			}
			String[] groupLayers = folderLayerConfig.getLayers();
			if (groupLayers != null && groupLayers.length > 0) {
				jsonLayer.put("layers", groupLayers);
			}
*/

		} else if (layerConfig instanceof KMLLayerConfig) {
			KMLLayerConfig kmlLayerConfig = (KMLLayerConfig)layerConfig;
			if (Utils.isNotBlank(kmlLayerConfig.getKmlUrl())) {
				jsonLayer.put("kmlUrl", kmlLayerConfig.getKmlUrl().trim());
			}
		}

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
			jsonStyle.put("title", style.getTitle().trim());
		}

		if (Utils.isNotBlank(style.getDescription())) {
			jsonStyle.put("description", style.getDescription().trim());
		}

		//if (Utils.isNotBlank(style.getLegendUrl())) {
		//	jsonStyle.put("legendUrl", style.getLegendUrl().trim());
		//}

		//if (Utils.isNotBlank(style.getLegendFilename())) {
		//	jsonStyle.put("legendFilename", style.getLegendFilename().trim());
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
			jsonOption.put("name", option.getName().trim());
		}

		if (Utils.isNotBlank(option.getTitle())) {
			jsonOption.put("title", option.getTitle().trim());
		}

		if (Utils.isNotBlank(option.getType())) {
			jsonOption.put("type", option.getType().trim());
		}

		if (option.isMandatory() != null) {
			jsonOption.put("mandatory", option.isMandatory());
		}

		if (Utils.isNotBlank(option.getDefaultValue())) {
			jsonOption.put("defaultValue", option.getDefaultValue().trim());
		}

		return jsonOption;
	}

	private void saveGeneratedConfigs(
			ClientConfig clientConfig,
			JSONObject mainConfig,
			JSONObject embeddedConfig,
			JSONObject layers) throws JSONException, IOException {

		File mainClientFile = this.getClientMainConfigFile(clientConfig);
		if (mainClientFile == null) {
			throw new IllegalArgumentException("No file provided for the Main client configuration.");
		} else {
			this.saveJSONConfig(mainConfig, mainClientFile);
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
				if (writer != null) {
					try {
						writer.close();
					} catch(Exception e) {
						LOGGER.log(Level.SEVERE, "Can not close the config file", e);
					}
				}
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
			if (bw != null) {
				try {
					bw.close();
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE, "Can not close the config file", e);
				}
			}
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
			if (bw != null) {
				try {
					bw.close();
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE, "Can not close the config file", e);
				}
			}
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
				if (reader != null) {
					try {
						reader.close();
					} catch(Exception e) {
						LOGGER.log(Level.SEVERE, "Can not close the config file", e);
					}
				}
			}
		} else {
			LOGGER.log(Level.SEVERE, "Can read the configuration file [{0}]", configFile.getAbsolutePath());
		}

		return existingConfig;
	}

	private Integer getNextDataSourceId() {
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
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception ex) {
					LOGGER.log(Level.SEVERE, "Can not close the config file", ex);
				}
			}
		}

		JSONObject jsonObj = null;
		String jsonStr = jsonStrBuf.toString();
		if (jsonStr != null && jsonStr.length() > 0) {
			jsonObj = new JSONObject(jsonStrBuf.toString());
		}
		return jsonObj;
	}
}
