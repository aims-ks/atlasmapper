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
import au.gov.aims.atlasmapperserver.dataSourceConfig.DataSourceConfigHelper;
import au.gov.aims.atlasmapperserver.jsonWrappers.client.ClientWrapper;
import au.gov.aims.atlasmapperserver.jsonWrappers.client.URLSaveState;
import au.gov.aims.atlasmapperserver.jsonWrappers.client.DataSourceWrapper;
import au.gov.aims.atlasmapperserver.jsonWrappers.client.LayerWrapper;
import au.gov.aims.atlasmapperserver.jsonWrappers.server.ServerConfigWrapper;
import au.gov.aims.atlasmapperserver.jsonWrappers.server.UsersConfigWrapper;
import au.gov.aims.atlasmapperserver.servlet.FileFinder;
import au.gov.aims.atlasmapperserver.xml.TC211.TC211Document;
import au.gov.aims.atlasmapperserver.xml.TC211.TC211Parser;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
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

	// Date in big endian format, so the alphabetic order is chronological: 2013 / 10 / 30 - 23:31
	public static final SimpleDateFormat DATE_FORMATER = new SimpleDateFormat("yyyy / MM / dd - HH:mm");

	// Used to generate and parse server config
	private static final double CURRENT_SERVER_CONFIG_VERSION = 1.0;
	private static final double CURRENT_USERS_CONFIG_VERSION = 1.0;

	// Used to generated clients config
	// NOTE: The version must match the version in the client /clientResources/amc/modules/Core/Core.js
	private static final double CURRENT_MAIN_CONFIG_VERSION = 1.1;
	public static final double CURRENT_LAYER_CONFIG_VERSION = 1.1;

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

	private Boolean demoMode = null;

	private String defaultProxyUrl = null;
	private String defaultLayerInfoServiceUrl = null;
	private String defaultSearchServiceUrl = null;

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
			this.defaultSearchServiceUrl = FileFinder.getDefaultSearchServiceURL(context);
		}
	}

	public File getApplicationFolder() {
		return this.applicationFolder;
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

	public void reloadServerConfigIfNeeded() throws JSONException, IOException {
		// Check file last modified date
		long lastModified = (this.serverConfigFile == null ? -1 : this.serverConfigFile.lastModified());
		if (lastModified < 0 || lastModified != this.serverConfigFileLastModified) {
			this.serverConfigFileLastModified = lastModified;
			this.reloadServerConfig();
		}
	}

	private synchronized void reloadServerConfig() throws JSONException, IOException {
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
							LOGGER.log(Level.SEVERE, "Can not close the server config file: {0}", Utils.getExceptionMessage(e));
							LOGGER.log(Level.FINE, "Stack trace: ", e);
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

	private synchronized void reloadServerConfig(Reader serverConfigReader) throws JSONException, IOException {
		if (serverConfigReader == null) {
			return;
		}
		ServerConfigWrapper jsonServerConfig;
		try {
			jsonServerConfig = new ServerConfigWrapper(new JSONObject(new JSONTokener(serverConfigReader)));
		} catch(JSONException ex) {
			LOGGER.log(Level.WARNING, "Malformed AtlasMapper JSON config file. The configuration file can not be parsed: {0}", Utils.getExceptionMessage(ex));
			LOGGER.log(Level.FINE, "Stack trace: ", ex);
			return;
		}

		this.demoMode = jsonServerConfig.isDemoMode(false);
		this.configVersion = jsonServerConfig.getVersion(0.1);

		if (this.configVersion > CURRENT_SERVER_CONFIG_VERSION) {
			throw new UnsupportedClassVersionError("The version of the server configuration file ("+this.configVersion+") is not supported by this server (support up to version: "+CURRENT_SERVER_CONFIG_VERSION+").");
		}

		this.dataSourceConfigs = new MultiKeyHashMap<Integer, String, AbstractDataSourceConfig>();
		this.lastDataSourceId = 0;
		JSONArray dataSourceConfigsArray = jsonServerConfig.getDataSources();

		if (dataSourceConfigsArray != null) {
			for (int i=0; i<dataSourceConfigsArray.length(); i++) {
				DataSourceWrapper rawDataSourceWrapper = new DataSourceWrapper(dataSourceConfigsArray.optJSONObject(i));
				if (rawDataSourceWrapper.getJSON() != null) {
					try {
						AbstractDataSourceConfig dataSourceConfig = DataSourceConfigHelper.createDataSourceConfig(rawDataSourceWrapper, this);

						Integer dataSourceId = dataSourceConfig.getId();
						if (dataSourceId != null && dataSourceId > this.lastDataSourceId) {
							this.lastDataSourceId = dataSourceId;
						}
						this.dataSourceConfigs.put(
								dataSourceId,
								dataSourceConfig.getDataSourceId(),
								dataSourceConfig);
					} catch (Exception ex) {
						LOGGER.log(Level.SEVERE, "Unexpected error while parsing the following data source: {0}\n{1}",
								new String[] { Utils.getExceptionMessage(ex), rawDataSourceWrapper.getJSON().toString(4) });
						LOGGER.log(Level.FINE, "Stack trace: ", ex);
					}
				} else {
					LOGGER.log(Level.WARNING, "Malformed AtlasMapper JSON config file: a data source is not set properly.");
				}
			}
		}

		this.clientConfigs = new MultiKeyHashMap<Integer, String, ClientConfig>();
		this.lastClientId = 0;
		JSONArray clientConfigsArray = jsonServerConfig.getClients();
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
	}

	protected synchronized void reloadDefaultServerConfig() throws JSONException, IOException {
		InputStream in = null;
		Reader reader = null;

		try {
			in = this.getClass().getClassLoader().getResourceAsStream(SERVER_DEFAULT_CONFIG_FILENAME);
			reader = new InputStreamReader(in);

			this.reloadServerConfig(reader);
		} finally {
			if (in != null) {
				try { in.close(); } catch(Exception ex) {
					LOGGER.log(Level.WARNING, "Can not close the InputStream: {0}", Utils.getExceptionMessage(ex));
					LOGGER.log(Level.FINE, "Stack trace: ", ex);
				}
			}
			if (reader != null) {
				try { reader.close(); } catch(Exception ex) {
					LOGGER.log(Level.WARNING, "Can not close the Reader: {0}", Utils.getExceptionMessage(ex));
					LOGGER.log(Level.FINE, "Stack trace: ", ex);
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
							LOGGER.log(Level.SEVERE, "Can not close the users config file: {0}", Utils.getExceptionMessage(e));
							LOGGER.log(Level.FINE, "Stack trace: ", e);
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

	private synchronized void reloadUsersConfig(Reader usersConfigReader) throws JSONException {
		this.users = new HashMap<String, User>();
		if (usersConfigReader != null) {
			UsersConfigWrapper usersConfig = new UsersConfigWrapper(new JSONObject(new JSONTokener(usersConfigReader)));

			this.usersConfigVersion = usersConfig.getVersion(0.0);
			if (this.usersConfigVersion > CURRENT_USERS_CONFIG_VERSION) {
				throw new UnsupportedClassVersionError("The version of the users configuration file ("+this.usersConfigVersion+") is not supported by this server (support up to version: "+CURRENT_USERS_CONFIG_VERSION+").");
			}

			JSONArray jsonUsers = usersConfig.getUsers();

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
					LOGGER.log(Level.WARNING, "Can not close the InputStream: {0}", Utils.getExceptionMessage(ex));
					LOGGER.log(Level.FINE, "Stack trace: ", ex);
				}
			}
			if (reader != null) {
				try { reader.close(); } catch(Exception ex) {
					LOGGER.log(Level.WARNING, "Can not close the Reader: {0}", Utils.getExceptionMessage(ex));
					LOGGER.log(Level.FINE, "Stack trace: ", ex);
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
						LOGGER.log(Level.SEVERE, "Can not close the server config file: {0}", Utils.getExceptionMessage(e));
						LOGGER.log(Level.FINE, "Stack trace: ", e);
					}
				}
				// Reload the configuration to refresh the state of the server with the config that is in the file
				try {
					this.reloadServerConfig();
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE, "Can not reload the server config file: {0}", Utils.getExceptionMessage(e));
					LOGGER.log(Level.FINE, "Stack trace: ", e);
				}
			}
		} else {
			// Reload the configuration to refresh the state of the server with the config that is in the file
			try {
				this.reloadServerConfig();
			} catch(Exception e) {
				LOGGER.log(Level.SEVERE, "Can not reload the server config file: {0}", Utils.getExceptionMessage(e));
				LOGGER.log(Level.FINE, "Stack trace: ", e);
			}
			throw new IOException(this.serverConfigFile.getCanonicalPath() + " is not writable.");
		}
	}

	public synchronized void saveServerConfig(Writer serverConfigWriter) throws JSONException, IOException {
		if (serverConfigWriter == null) {
			return;
		}
		ServerConfigWrapper config = new ServerConfigWrapper();

		if (this.demoMode != null && this.demoMode) {
			config.setDemoMode(this.demoMode);
		}

		config.setVersion(CURRENT_SERVER_CONFIG_VERSION);
		config.setDataSources(this.getDataSourceConfigsJSon(false));
		config.setClients(this.getClientConfigsJSon(false));

		this.saveJSONConfig(config.getJSON(), serverConfigWriter);
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
						LOGGER.log(Level.SEVERE, "Can not close the users config file: {0}", Utils.getExceptionMessage(e));
						LOGGER.log(Level.FINE, "Stack trace: ", e);
					}
				}
				// Reload the configuration to refresh the state of the server with the config that is in the file
				try {
					this.reloadUsersConfig();
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE, "Can not reload the users config file: {0}", Utils.getExceptionMessage(e));
					LOGGER.log(Level.FINE, "Stack trace: ", e);
				}
			}
		} else {
			// Reload the configuration to refresh the state of the server with the config that is in the file
			try {
				this.reloadUsersConfig();
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Can not reload the users config file: {0}", Utils.getExceptionMessage(e));
				LOGGER.log(Level.FINE, "Stack trace: ", e);
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

		UsersConfigWrapper usersConfig = new UsersConfigWrapper();
		usersConfig.setVersion(CURRENT_USERS_CONFIG_VERSION);
		usersConfig.setUsers(jsonUsers);

		this.saveJSONConfig(usersConfig.getJSON(), usersConfigWriter);
	}

	public boolean isDemoMode() {
		try {
			this.reloadServerConfigIfNeeded();
		} catch (Exception ex) {
			// This should not happen...
			LOGGER.log(Level.SEVERE, "Unexpected exception occurred while reloading the config. Fall back to demo mode. {0}", Utils.getExceptionMessage(ex));
			LOGGER.log(Level.FINE, "Stack trace: ", ex);
			return true;
		}
		return this.demoMode;
	}

	public synchronized List<AbstractDataSourceConfig> createDataSourceConfig(ServletRequest request) throws JSONException, IOException {
		if (request == null) {
			return null;
		}

		MultiKeyHashMap<Integer, String, AbstractDataSourceConfig> configs = this.getDataSourceConfigs();
		List<AbstractDataSourceConfig> newDataSourceConfigs = new ArrayList<AbstractDataSourceConfig>();
		JSONArray dataJSonArr = this.getPostedData(request);
		if (dataJSonArr != null) {
			for (int i=0; i<dataJSonArr.length(); i++) {
				DataSourceWrapper dataSourceWrapper = new DataSourceWrapper(dataJSonArr.optJSONObject(i));
				if (dataSourceWrapper.getJSON() != null) {
					Integer id = dataSourceWrapper.getId();
					if (id == null) {
						dataSourceWrapper.setId(this.getNextDataSourceId());
					}

					AbstractDataSourceConfig dataSourceConfig = DataSourceConfigHelper.createDataSourceConfig(dataSourceWrapper, this);

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
	public synchronized void updateDataSourceConfig(ServletRequest request) throws JSONException, IOException {
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
						// Update the object using the value from the form
						dataSourceConfig.update(dataJSonObj, true);
						dataSourceConfig.setModified(true);
						this.ensureUniqueness(dataSourceConfig);
					}
				}
			}
		}
	}
	public synchronized void destroyDataSourceConfig(ServletRequest request) throws JSONException, IOException {
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
					URLCache.reloadDiskCacheMapIfNeeded(this.getApplicationFolder());
					URLCache.deleteCache(this, dataSourceConfig);
				}
			}
		}
	}

	private void ensureUniqueness(AbstractDataSourceConfig dataSource) throws IOException, JSONException {
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
	public boolean dataSourceExists(String dataSourceId, Integer id) throws IOException, JSONException {
		if (dataSourceId == null || dataSourceId.isEmpty()) {
			return true; // We don't want empty data source ID, pretend that we already have this one.
		}
		// Look for a data source with a similar name, one that could create a clash in file names.
		String dataSourceFilename = FileFinder.safeFileName(dataSourceId);
		if (dataSourceFilename == null || dataSourceFilename.isEmpty()) {
			return true; // We don't want data source ID that will create empty file name, pretend that we already have this one.
		}

		MultiKeyHashMap<Integer, String, AbstractDataSourceConfig> _dataSourceConfigs = getDataSourceConfigs();

		for (String existingDataSourceId : _dataSourceConfigs.key2Set()) {
			// Ignore exact matching ID, that case is considered bellow the "for" loop
			if (!dataSourceId.equals(existingDataSourceId)) {
				String existingDataSourceFilename = FileFinder.safeFileName(existingDataSourceId);
				if (dataSourceFilename.equals(existingDataSourceFilename)) {
					return true; // The data source ID clash with an other one.
				}
			}
		}

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

	public synchronized List<ClientConfig> createClientConfig(ServletRequest request) throws JSONException, IOException {
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
				DataSourceWrapper dataSourceWrapper = new DataSourceWrapper(dataJSonArr.optJSONObject(i));
				if (dataSourceWrapper.getJSON() != null) {
					Integer clientId = dataSourceWrapper.getId();
					ClientConfig clientConfig = configs.get1(clientId);
					if (clientConfig != null) {
						//File oldClientFolder = FileFinder.getAtlasMapperClientFolder(this.applicationFolder, clientConfig, false);
						//File oldConfigFolder = FileFinder.getAtlasMapperClientConfigFolder(this.applicationFolder, clientConfig, false);

						clientConfig.update(dataSourceWrapper.getJSON(), true);
						this.ensureUniqueness(clientConfig);

						//File newClientFolder = FileFinder.getAtlasMapperClientFolder(this.applicationFolder, clientConfig, false);
						//File newConfigFolder = FileFinder.getAtlasMapperClientConfigFolder(this.applicationFolder, clientConfig, false);

						// Move the client folder. This feature works, but it's an unexpected behaviour.
						// The function may be re-added later, with a confirmation window asking the admin if that's what he want to do.
						//this.moveClientFolder(oldClientFolder, newClientFolder, oldConfigFolder, newConfigFolder);
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
				DataSourceWrapper dataSourceWrapper = new DataSourceWrapper(dataJSonArr.optJSONObject(i));
				if (dataSourceWrapper.getJSON() != null) {
					Integer clientId = dataSourceWrapper.getId();
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

	private void ensureUniqueness(ClientConfig client) throws IOException, JSONException {
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
	public boolean clientExists(String clientId, Integer id) throws IOException, JSONException {
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


	public URLSaveState getMapStateForDataset(ClientConfig clientConfig, String iso19115_19139url) throws Exception {
		JSONArray jsonLayers = new JSONArray();

		TC211Document tc211Document = TC211Parser.parseURL(this, null, Utils.toURL(iso19115_19139url), false, true);
		if (tc211Document == null) {
			return null;
		}

		// *** Layers ***

		List<TC211Document.Link> links = tc211Document.getLinks();
		for (TC211Document.Link link : links) {
			TC211Document.Protocol linkProtocol = link.getProtocol();
			if (linkProtocol != null) {
				LayerWrapper foundLayer = null;
				String foundLayerId = null;

				ClientWrapper fullConfig = new ClientWrapper(this.getClientConfigFileJSon(clientConfig, ConfigType.FULL, false));
				JSONObject dataSources = fullConfig.getDataSources();
				JSONObject clientLayers = fullConfig.getLayers();
				if (linkProtocol.isOGC()) {
					// WMS, ncWMS, etc. If not found, a basic WMS layer will be created with the info that we have.
					String serviceUrl = link.getUrl();
					String layerName = link.getName();

					if (Utils.isNotBlank(serviceUrl) && Utils.isNotBlank(layerName)) {
						// Suppress warnings: The JSON library do not use generics properly
						@SuppressWarnings("unchecked")
						Iterator<String> foundLayerIDs = clientLayers.keys();
						while(foundLayerIDs.hasNext() && foundLayer == null) {
							foundLayerId = foundLayerIDs.next();
							if (!clientLayers.isNull(foundLayerId)) {
								LayerWrapper layer = new LayerWrapper(clientLayers.optJSONObject(foundLayerId));
								String foundLayerName = layer.getLayerName();
								if (layerName.equals(foundLayerName)) {
									// We found a layer with the same layer ID. We now have to check its data source
									String foundDataSourceId = layer.getDataSourceId();
									if (foundDataSourceId != null) {
										DataSourceWrapper foundDataSource = new DataSourceWrapper(dataSources.optJSONObject(foundDataSourceId));
										String foundServiceUrl = foundDataSource.getServiceUrl();

										// Check if URLs are similar ("http://www.a.com/?a=b&b=c" == "http://www.a.com:80/?b=c&a=b")
										if (Utils.equalsWMSUrl(foundServiceUrl, serviceUrl)) {
											foundLayer = layer;
											foundLayer.setLayerId(foundLayerId);
										}
									}
								}
							}
						}
					}

				} else if (linkProtocol.isKML()) {
					// KML layers. If not found, a basic KML layer will be created with the info that we have.
					String kmlUrl = link.getUrl();

					if (Utils.isNotBlank(kmlUrl)) {
						// Suppress warnings: The JSON library do not use generics properly
						@SuppressWarnings("unchecked")
						Iterator<String> foundLayerIDs = clientLayers.keys();
						while(foundLayerIDs.hasNext() && foundLayer == null) {
							foundLayerId = foundLayerIDs.next();
							if (!clientLayers.isNull(foundLayerId)) {
								LayerWrapper layer = new LayerWrapper(clientLayers.optJSONObject(foundLayerId));
								String foundLayerKMLUrl = layer.getKmlUrl();
								if (kmlUrl.equals(foundLayerKMLUrl)) {
									// We found a layer with the same KML URL. Let assume it's the good one
									foundLayer = layer;
									foundLayer.setLayerId(foundLayerId);
								}
							}
						}
					}
				}

				if (foundLayer == null) {
					foundLayer = TC211Parser.createLayer(this, tc211Document, link);
				}

				if (foundLayer != null) {
					jsonLayers.put(foundLayer);
				}
			}
		}

		// *** Bounds ***


		// Calculate bounds
		// left, bottom, right, top
		// min lon, min lat, max lon, max lat
		double[] bounds = null;

		List<TC211Document.Point> points = tc211Document.getPoints();
		if (points != null) {
			for (TC211Document.Point point : points) {
				if (bounds == null) {
					bounds = new double[]{ point.getLon(), point.getLat(), point.getLon(), point.getLat() };
				} else {
					if (point.getLon() < bounds[0]) { bounds[0] = point.getLon(); }
					if (point.getLat() < bounds[1]) { bounds[1] = point.getLat(); }
					if (point.getLon() > bounds[2]) { bounds[2] = point.getLon(); }
					if (point.getLat() > bounds[3]) { bounds[3] = point.getLat(); }
				}

				// TODO Send the points to the client
			}
		}

		List<TC211Document.Polygon> polygons = tc211Document.getPolygons();
		if (polygons != null) {
			for (TC211Document.Polygon polygon : polygons) {
				for (TC211Document.Point point : polygon.getPoints()) {
					if (bounds == null) {
						bounds = new double[]{ point.getLon(), point.getLat(), point.getLon(), point.getLat() };
					} else {
						if (point.getLon() < bounds[0]) { bounds[0] = point.getLon(); }
						if (point.getLat() < bounds[1]) { bounds[1] = point.getLat(); }
						if (point.getLon() > bounds[2]) { bounds[2] = point.getLon(); }
						if (point.getLat() > bounds[3]) { bounds[3] = point.getLat(); }
					}
				}

				// TODO Send the polygons to the client
			}
		}

		JSONArray jsonBounds = null;
		if (bounds != null) {
			jsonBounds = new JSONArray(bounds);
		}

		// Validation
		if (jsonLayers.length() <= 0) {
			return null;
		}

		URLSaveState state = new URLSaveState();
		state.setLayers(jsonLayers);
		if (jsonBounds != null) {
			state.setBounds(jsonBounds);
		}

		return state;
	}


	public JSONObject getClientLayers(ClientConfig clientConfig, String[] layerIds) throws Exception {
		if (clientConfig == null) { return null; }

		// NOTE: The layer catalog is null, but generate is false, so the list of layers is taken from the existing client configuration.
		return (JSONObject)_getClientLayers(null, null, clientConfig, Arrays.asList(layerIds), false, "JSONObject");
	}

	// Sometime, layers are required to be in an Array to keep their order
	private Object _getClientLayers(DataSourceWrapper layerCatalog, Map<String, DataSourceWrapper> dataSources, ClientConfig clientConfig, Collection<String> layerIds, boolean generate, String jsonClass)
			throws IOException, JSONException {
		if (clientConfig == null) { return null; }

		JSONObject foundLayersObj = new JSONObject();
		JSONArray foundLayersArr = new JSONArray();

		JSONObject clientLayers = this.getClientConfigFileJSon(layerCatalog, dataSources, clientConfig, ConfigType.LAYERS, generate);

		boolean asJSONObject = !"JSONArray".equalsIgnoreCase(jsonClass);
		for (String rawLayerId : layerIds) {
			String layerId = rawLayerId.trim();
			if (clientLayers.has(layerId)) {
				LayerWrapper jsonLayer = new LayerWrapper(clientLayers.optJSONObject(layerId));
				if (asJSONObject) {
					foundLayersObj.put(layerId, jsonLayer.getJSON());
				} else {
					jsonLayer.setLayerId(layerId);
					foundLayersArr.put(jsonLayer.getJSON());
				}
			}
		}

		if (asJSONObject) {
			return foundLayersObj;
		}
		return foundLayersArr;
	}

	public MultiKeyHashMap<Integer, String, AbstractDataSourceConfig> getDataSourceConfigs() throws JSONException, IOException {
		this.reloadServerConfigIfNeeded();
		return this.dataSourceConfigs;
	}

	public AbstractDataSourceConfig getDataSourceConfig(Integer dataSourceId) throws JSONException, IOException {
		if (dataSourceId == null) {
			return null;
		}

		MultiKeyHashMap<Integer, String, AbstractDataSourceConfig> configs = this.getDataSourceConfigs();
		if (configs == null) {
			return null;
		}

		return configs.get1(dataSourceId);
	}

	public AbstractDataSourceConfig getDataSourceConfig(String dataSourceId) throws JSONException, IOException {
		if (Utils.isBlank(dataSourceId)) {
			return null;
		}

		MultiKeyHashMap<Integer, String, AbstractDataSourceConfig> configs = this.getDataSourceConfigs();
		if (configs == null) {
			return null;
		}

		return configs.get2(dataSourceId);
	}

	public JSONArray getDataSourceConfigsJSon(boolean reload) throws JSONException, IOException {
		JSONArray dataSourceConfigArray = new JSONArray();

		MultiKeyHashMap<Integer, String, AbstractDataSourceConfig> configs = reload ? this.getDataSourceConfigs() : this.dataSourceConfigs;
		for (AbstractDataSourceConfig dataSourceConfig : configs.values()) {
			dataSourceConfigArray.put(dataSourceConfig.toJSonObject());
		}
		return dataSourceConfigArray;
	}

	public MultiKeyHashMap<Integer, String, ClientConfig> getClientConfigs() throws JSONException, IOException {
		this.reloadServerConfigIfNeeded();
		return this.clientConfigs;
	}

	public ClientConfig getClientConfig(String clientId) throws JSONException, IOException {
		if (Utils.isBlank(clientId)) {
			return null;
		}

		MultiKeyHashMap<Integer, String, ClientConfig> configs = this.getClientConfigs();
		if (configs == null) {
			return null;
		}

		return configs.get2(clientId);
	}

	public ClientConfig getClientConfig(Integer clientId) throws JSONException, IOException {
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
	 * view the generated client.
	 * @param context The ServletContext, used to build the clients' URL
	 * @return A JSONArray of JSONObjects, representing the clients' config (with URLs)
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

	private JSONArray getClientConfigsJSon(boolean reload) throws JSONException, IOException {
		JSONArray clientConfigArray = new JSONArray();

		MultiKeyHashMap<Integer, String, ClientConfig> configs = reload ? this.getClientConfigs() : this.clientConfigs;
		for (ClientConfig clientConfig : configs.values()) {
			clientConfigArray.put(clientConfig.toJSonObject());
		}
		return clientConfigArray;
	}

	public ClientConfig getDefaultClientConfig() throws JSONException, IOException {
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
				if (oldestClientConfig == null || (configKey != null && oldestKey != null && oldestKey > configKey)) {
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

	// Return error messages, if any
	public Map<String, Errors> generateAllClients(boolean complete) throws Exception {
		Map<String, Errors> errorMessages = new HashMap<String, Errors>();
		// Can not loop on client instance directly (this one is quite tricky):
		//     Since the collection of instances is reloaded when the config is modified,
		//     the reference to the instance is not the same as the one in the loop,
		//     so the modified instance is not saved in the config on the next server save.
		for (String clientId : this.getClientConfigs().key2Set()) {
			ClientConfig clientConfig = this.getClientConfig(clientId);
			Errors clientErrors = clientConfig.process(complete);
			if (clientErrors != null && !clientErrors.isEmpty()) {
				String clientName = clientConfig.getClientName() + " (" + clientConfig.getClientId() + ")";
				errorMessages.put(clientName, clientErrors);
			}
		}

		return errorMessages;
	}

	public JSONObject getClientConfigFileJSon(ClientConfig clientConfig, ConfigType configType, boolean generate)
			throws Exception {

		DataSourceWrapper layerCatalog = null;
		Map<String, DataSourceWrapper> dataSources = null;
		if (generate) {
			layerCatalog = clientConfig.getLayerCatalog(clientConfig.loadDataSources());
			dataSources = clientConfig.loadDataSources();
		}
		return this.getClientConfigFileJSon(layerCatalog, dataSources, clientConfig, configType, generate);
	}

	/**
	 * TODO This method used to be almost the same for each cases. It evolved to a point where it would be better to split it into 4 methods.
	 * @param layerCatalog The client LayerCatalog, to avoid overhead with generation.
	 * @param clientConfig Client to generate
	 * @param configType Type of generation requested
	 * @param generate True to generate a new configuration (when generating a client), false to get the configuration saved in the file (when a client request info about a layer).
	 * @return
	 * @throws Exception
	 */
	public JSONObject getClientConfigFileJSon(
			DataSourceWrapper layerCatalog,
			Map<String, DataSourceWrapper> dataSources,
			ClientConfig clientConfig,
			ConfigType configType,
			boolean generate) throws IOException, JSONException {

		if (clientConfig == null || configType == null) { return null; }

		ClientWrapper mainConfig = null;
		ClientWrapper embeddedConfig = null;
		ClientWrapper fullConfig = null;
		JSONObject layersConfig = null;

		switch (configType) {
			case MAIN:
				if (generate) {
					mainConfig = this._generateClientConfigBase(layerCatalog, dataSources, clientConfig);
					if (mainConfig != null) {
						JSONObject modules = this.generateModules(
								clientConfig.getMainClientModules(),
								clientConfig,
								layerCatalog,
								dataSources
						);
						if (modules != null && modules.length() > 0) {
							mainConfig.setModules(modules);
						}
					}
				} else {
					mainConfig = new ClientWrapper(this.loadExistingConfig(this.getClientMainConfigFile(clientConfig)));
				}

				if (mainConfig != null) {
					this._setProxyUrl(mainConfig, clientConfig);
					return mainConfig.getJSON();
				}
				return null;

			case EMBEDDED:
				if (generate) {
					embeddedConfig = this._generateClientConfigBase(layerCatalog, dataSources, clientConfig);

					JSONObject modules = this.generateModules(
							clientConfig.getEmbeddedClientModules(),
							clientConfig,
							layerCatalog,
							dataSources
					);
					if (modules != null && modules.length() > 0) {
						embeddedConfig.setModules(modules);
					}
				} else {
					embeddedConfig = new ClientWrapper(this.loadExistingConfig(this.getClientEmbeddedConfigFile(clientConfig)));
				}

				this._setProxyUrl(embeddedConfig, clientConfig);
				return embeddedConfig.getJSON();

			case LAYERS:
				if (generate) {
					return layerCatalog == null ? null : layerCatalog.getLayers();
				} else {
					return this.loadExistingConfig(this.getClientLayersConfigFile(clientConfig));
				}

			case FULL:
				// FULL is only used by the data set URL (Embedded map created on the fly to display layers for a MEST URL)

				mainConfig = new ClientWrapper(this.getClientConfigFileJSon(layerCatalog, dataSources, clientConfig, ConfigType.MAIN, generate));
				layersConfig = this.getClientConfigFileJSon(layerCatalog, dataSources, clientConfig, ConfigType.LAYERS, generate);

				// Making a copy of the mainConfig variable (clone) would be better, but the variable is never used
				// after this, so it's faster (easier) to simply change it into the fullConfig.
				fullConfig = mainConfig;
				fullConfig.setLayers(layersConfig);
				return fullConfig.getJSON();
		}
		return null;
	}

	// Use as a base for Main and Embedded config
	// TODO Move in ClientConfig
	private ClientWrapper _generateClientConfigBase(DataSourceWrapper layerCatalog, Map<String, DataSourceWrapper> dataSources, ClientConfig clientConfig)
			throws IOException, JSONException {
		if (clientConfig == null) { return null; }

		ClientWrapper clientWrapper = new ClientWrapper();
		clientWrapper.setVersion(CURRENT_MAIN_CONFIG_VERSION);
		clientWrapper.setClientId(clientConfig.getClientId().trim());
		if (Utils.isNotBlank(clientConfig.getClientName())) {
			clientWrapper.setClientName(clientConfig.getClientName().trim());
		}

		if (Utils.isNotBlank(clientConfig.getAttributions())) {
			clientWrapper.setAttributions(clientConfig.getAttributions().trim());
		}

		// TODO Remove when the default saved state will be implemented
		clientWrapper.setDefaultLayers(this.getClientDefaultLayers(layerCatalog, dataSources, clientConfig.getClientId()));

		if (Utils.isNotBlank(clientConfig.getProjection())) {
			clientWrapper.setProjection(clientConfig.getProjection().trim());
			clientWrapper.setMapOptions(Utils.getMapOptions(clientConfig.getProjection().trim()));
		}

		if (clientConfig.isUseLayerService()) {
			if (Utils.isNotBlank(clientConfig.getLayerInfoServiceUrl())) {
				clientWrapper.setLayerInfoServiceUrl(clientConfig.getLayerInfoServiceUrl().trim());
			} else if (Utils.isNotBlank(this.defaultLayerInfoServiceUrl)) {
				clientWrapper.setLayerInfoServiceUrl(this.defaultLayerInfoServiceUrl.trim());
			}
		}

		if (Utils.isNotBlank(clientConfig.getDownloadLoggerServiceUrl())) {
			clientWrapper.setDownloadLoggerServiceUrl(clientConfig.getDownloadLoggerServiceUrl().trim());
		}

		clientWrapper.setShowAddRemoveLayerButtons(clientConfig.isShowAddRemoveLayerButtons());

		clientWrapper.setSearchEnabled(clientConfig.isSearchEnabled());
		clientWrapper.setPrintEnabled(clientConfig.isPrintEnabled());
		clientWrapper.setSaveMapEnabled(clientConfig.isSaveMapEnabled());
		clientWrapper.setMapConfigEnabled(clientConfig.isMapConfigEnabled());
		clientWrapper.setMapMeasurementEnabled(clientConfig.isMapMeasurementEnabled());
		clientWrapper.setMapMeasurementLineEnabled(clientConfig.isMapMeasurementLineEnabled());
		clientWrapper.setMapMeasurementAreaEnabled(clientConfig.isMapMeasurementAreaEnabled());

		if (clientConfig.isSearchEnabled()) {
			if (Utils.isNotBlank(clientConfig.getSearchServiceUrl())) {
				clientWrapper.setSearchServiceUrl(clientConfig.getSearchServiceUrl().trim());
			} else if (Utils.isNotBlank(this.defaultSearchServiceUrl)) {
				clientWrapper.setSearchServiceUrl(this.defaultSearchServiceUrl.trim());
			}
		}

		if (clientConfig.getVersion() != null) {
			clientWrapper.setVersion(clientConfig.getVersion());
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

			clientWrapper.setStartingLocation(startingLocation);
		}

		if (!dataSources.isEmpty()) {
			// Add data sources
			for (DataSourceWrapper dataSource : dataSources.values()) {
				if (dataSource != null) {
					clientWrapper.addDataSource(dataSource.getDataSourceId(), dataSource.getMainConfigJSON());
				}
			}
		}
		// TODO Apply legend parameters overrides here maybe?

		// Appearance
		// NOTE The ExtJS theme, pageHeader and pageFooter are used to generate
		// the template, the client do not need to know those variables.
		if (Utils.isNotBlank(clientConfig.getLayersPanelHeader())) {
			clientWrapper.setLayersPanelHeader(clientConfig.getLayersPanelHeader().trim());
		}
		if (Utils.isNotBlank(clientConfig.getLayersPanelFooter())) {
			clientWrapper.setLayersPanelFooter(clientConfig.getLayersPanelFooter().trim());
		}

		return clientWrapper;
	}

	private void _setProxyUrl(ClientWrapper clientWrapper, ClientConfig clientConfig) throws JSONException, UnsupportedEncodingException {
		if (clientConfig.isUseLayerService()) {
			if (Utils.isNotBlank(this.defaultProxyUrl)) {
				String proxyUrl = Utils.addUrlParameter(
						this.defaultProxyUrl.trim(),
						"client",
						clientConfig.getClientId());

				proxyUrl = Utils.addUrlParameter(proxyUrl, "url", "");

				clientWrapper.setProxyUrl(proxyUrl);
			}
		}
		if (Utils.isNotBlank(clientConfig.getProxyUrl())) {
			clientWrapper.setProxyUrl(clientConfig.getProxyUrl().trim());
		}
	}

	private JSONArray getClientDefaultLayers(DataSourceWrapper layerCatalog, Map<String, DataSourceWrapper> dataSources, String clientId) throws IOException, JSONException {
		if (Utils.isBlank(clientId)) { return null; }

		return (JSONArray)this._getClientLayers(
				layerCatalog,
				dataSources,
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

	private JSONObject generateModules(JSONArray modulesArray, ClientConfig clientConfig, DataSourceWrapper layerCatalog, Map<String, DataSourceWrapper> dataSources) throws JSONException {
		if (modulesArray != null && modulesArray.length() > 0) {
			JSONObject modules = new JSONObject();
			for (int i=0; i<modulesArray.length(); i++) {
				// https://github.com/douglascrockford/JSON-java/issues/24
				String moduleName = modulesArray.optString(i, null);
				if (moduleName != null) {
					JSONObject module = this.generateModule(moduleName, clientConfig, layerCatalog, dataSources);
					if (module != null) {
						modules.put(moduleName, module);
					}
				}
			}

			return modules;
		}
		return null;
	}

	private JSONObject generateModule(String moduleConfig, ClientConfig clientConfig, DataSourceWrapper layerCatalog, Map<String, DataSourceWrapper> dataSources) throws JSONException {
		JSONObject moduleJSONConfig =
				ModuleHelper.generateModuleConfiguration(moduleConfig, clientConfig, layerCatalog, dataSources);

		if (moduleJSONConfig == null) {
			LOGGER.log(Level.SEVERE, "Can not generate the configuration for {0}", moduleConfig);
		}

		return moduleJSONConfig;
	}

	public synchronized void saveJSONConfig(JSONObject config, File file) throws JSONException, IOException {
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
						LOGGER.log(Level.SEVERE, "Can not close the config file: {0}", Utils.getExceptionMessage(e));
						LOGGER.log(Level.FINE, "Stack trace: ", e);
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
					LOGGER.log(Level.SEVERE, "Can not close the config file: {0}", Utils.getExceptionMessage(e));
					LOGGER.log(Level.FINE, "Stack trace: ", e);
				}
			}
		}
	}

	/*
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
					LOGGER.log(Level.SEVERE, "Can not close the config file: {0}", Utils.getExceptionMessage(e));
					LOGGER.log(Level.FINE, "Stack trace: ", e);
				}
			}
		}
	}*/

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
						LOGGER.log(Level.SEVERE, "Can not close the config file: {0}", Utils.getExceptionMessage(e));
						LOGGER.log(Level.FINE, "Stack trace: ", e);
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
			LOGGER.log(Level.SEVERE, "Can not retrieve the posted JSON object: ", Utils.getExceptionMessage(ex));
			LOGGER.log(Level.FINE, "Stack trace: ", ex);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception ex) {
					LOGGER.log(Level.SEVERE, "Can not close the config file: {0}", Utils.getExceptionMessage(ex));
					LOGGER.log(Level.FINE, "Stack trace: ", ex);
				}
			}
		}

		JSONObject jsonObj = null;
		String jsonStr = jsonStrBuf.toString();
		if (jsonStr.length() > 0) {
			jsonObj = new JSONObject(jsonStr);
		}
		return jsonObj;
	}
}
