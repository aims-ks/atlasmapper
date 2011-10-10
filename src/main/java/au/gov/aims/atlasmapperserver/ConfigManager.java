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
import java.util.Collections;
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

	private File serverConfigFile = null;
	private long serverConfigFileLastModified = -1;

	private File usersConfigFile = null;
	private long usersConfigFileLastModified = -1;

	// Set by the ConfigHelper
	private String clientFullConfigFilename = null;
	private String clientEmbededConfigFilename = null;
	private String clientLayersConfigFilename = null;
	private File applicationFolder = null;

	private static final String DATASOURCES_KEY = "datasources";
	private static final String CLIENTS_KEY = "clients";

	private String defaultProxyUrl = null;
	private String defaultLayerInfoServiceUrl = null;

	private int lastDatasourceId;
	private int lastClientId;

	private Map<String, User> users = null;
	private MultiKeyHashMap<Integer, String, DatasourceConfig> datasourceConfigs = null;
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

	public File getUsersConfigFile() {
		return this.usersConfigFile;
	}

	public File getClientEmbededConfigFile(ClientConfig clientConfig) {
		if (this.clientEmbededConfigFilename == null) {
			return null;
		}
		File clientConfigFolder = this.getClientConfigFolder(clientConfig);
		if (clientConfigFolder == null) {
			return null;
		}
		return new File(clientConfigFolder, this.clientEmbededConfigFilename);
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

	public void setClientEmbededConfigFilename(String clientEmbededConfigFilename) {
		this.clientEmbededConfigFilename = clientEmbededConfigFilename;
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
		this.datasourceConfigs = null;
		this.clientConfigs = null;

		if (this.serverConfigFile != null) {
			if (this.serverConfigFile.exists() && this.serverConfigFile.canRead()) {
				FileReader serverConfigReader = null;
				try {
					serverConfigReader = new FileReader(this.serverConfigFile);
					this.reloadServerConfig(serverConfigReader);
				} finally {
					try { serverConfigReader.close(); } catch(Exception e) {}
				}
			} else {
				LOGGER.log(Level.SEVERE, "{0} is not readable", this.serverConfigFile.getAbsolutePath());
			}
		} else {
			LOGGER.log(Level.SEVERE, "Undefined server configuration file");
		}

		if ((this.datasourceConfigs == null || this.datasourceConfigs.isEmpty()) &&
				(this.clientConfigs == null || this.clientConfigs.isEmpty())) {
			LOGGER.log(Level.WARNING, "No datasources nor clients defined; fall back to default.");
			this.reloadDefaultServerConfig();
		}
	}

	private synchronized void reloadServerConfig(Reader serverConfigReader) throws JSONException {
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

		this.datasourceConfigs = new MultiKeyHashMap<Integer, String, DatasourceConfig>();
		this.lastDatasourceId = 0;
		JSONArray datasourceConfigsArray = jsonObj.optJSONArray(DATASOURCES_KEY);
		if (datasourceConfigsArray != null) {
			for (int i=0; i<datasourceConfigsArray.length(); i++) {
				JSONObject rawDatasourceConfig = datasourceConfigsArray.optJSONObject(i);
				if (rawDatasourceConfig != null) {
					DatasourceConfig datasourceConfig = new DatasourceConfig();
					datasourceConfig.update(rawDatasourceConfig);
					Integer datasourceId = datasourceConfig.getId();
					if (datasourceId != null && datasourceId > this.lastDatasourceId) {
						this.lastDatasourceId = datasourceId;
					}
					this.datasourceConfigs.put(
							datasourceId,
							datasourceConfig.getDatasourceId(),
							datasourceConfig);
				} else {
					LOGGER.log(Level.WARNING, "Malformated AtlasMapper JSON config file: a datasource is not set properly [{0}]", rawDatasourceConfig);
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
					ClientConfig clientConfig = new ClientConfig();
					clientConfig.update((JSONObject)rawClientConfig);
					Integer clientId = clientConfig.getId();
					if (clientId != null && clientId > this.lastClientId) {
						this.lastClientId = clientId;
					}
					this.clientConfigs.put(clientId,
							clientConfig.getClientName(),
							clientConfig);
				} else {
					LOGGER.log(Level.WARNING, "Malformated AtlasMapper JSON config file: a client is not set properly [{0}]", rawClientConfig);
				}
			}
		}

		// Prevent memory leak
		WMSCapabilitiesWrapper.cleanupCapabilitiesDocumentsCache(this.datasourceConfigs.values());
	}

	private synchronized void reloadDefaultServerConfig() throws JSONException {
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
					try { usersConfigReader.close(); } catch(Exception e) {}
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
			JSONArray jsonUsers = new JSONArray(new JSONTokener(usersConfigReader));

			if (jsonUsers != null) {
				for (int i=0; i<jsonUsers.length(); i++) {
					JSONObject jsonUser = jsonUsers.optJSONObject(i);
					if (jsonUser != null) {
						User user = new User();
						user.update(jsonUser);
						this.users.put(user.getLoginName(), user);
					}
				}
			}
		}
	}

	private synchronized void reloadDefaultUsersConfig() throws JSONException {
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
				try { writer.close(); } catch(Exception e) {}
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
		config.put(DATASOURCES_KEY, this._getDatasourceConfigsJSon(false));
		config.put(CLIENTS_KEY, this._getClientConfigsJSon(false));

		this.saveJSONConfig(config, serverConfigWriter);

		// Prevent memory leak
		WMSCapabilitiesWrapper.cleanupCapabilitiesDocumentsCache(this.datasourceConfigs.values());
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
				try { writer.close(); } catch(Exception e) {}
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

		this.saveJSONConfig(jsonUsers, usersConfigWriter);
	}

	public synchronized List<DatasourceConfig> createDatasourceConfig(ServletRequest request) throws JSONException, FileNotFoundException {
		if (request == null) {
			return null;
		}

		MultiKeyHashMap<Integer, String, DatasourceConfig> configs = this.getDatasourceConfigs();
		List<DatasourceConfig> newDatasourceConfigs = new ArrayList<DatasourceConfig>();
		JSONArray dataJSonArr = this.getPostedData(request);
		if (dataJSonArr != null) {
			for (int i=0; i<dataJSonArr.length(); i++) {
				JSONObject dataJSonObj = dataJSonArr.optJSONObject(i);
				if (dataJSonObj != null) {
					if (dataJSonObj.isNull("id") || dataJSonObj.optString("id", "").length() <= 0) {
						dataJSonObj.put("id", this.getNextDatasourceId());
					}
					DatasourceConfig datasourceConfig = new DatasourceConfig();
					datasourceConfig.update(dataJSonObj);
					if (datasourceConfig != null) {
						configs.put(datasourceConfig.getId(),
								datasourceConfig.getDatasourceId(),
								datasourceConfig);
						newDatasourceConfigs.add(datasourceConfig);
					}
				}
			}
		}
		return newDatasourceConfigs;
	}
	public synchronized void updateDatasourceConfig(ServletRequest request) throws JSONException, FileNotFoundException {
		if (request == null) {
			return;
		}

		MultiKeyHashMap<Integer, String, DatasourceConfig> configs = this.getDatasourceConfigs();
		JSONArray dataJSonArr = this.getPostedData(request);
		if (dataJSonArr != null) {
			for (int i=0; i<dataJSonArr.length(); i++) {
				JSONObject dataJSonObj = dataJSonArr.optJSONObject(i);
				if (dataJSonObj != null) {
					Integer datasourceId = dataJSonObj.optInt("id", -1);
					DatasourceConfig datasourceConfig = configs.get1(datasourceId);
					if (datasourceConfig != null) {
						datasourceConfig.update(dataJSonObj);
					}
				}
			}
		}
	}
	public synchronized void destroyDatasourceConfig(ServletRequest request) throws JSONException, FileNotFoundException {
		if (request == null) {
			return;
		}

		MultiKeyHashMap<Integer, String, DatasourceConfig> configs = this.getDatasourceConfigs();
		JSONArray dataJSonArr = this.getPostedData(request);
		if (dataJSonArr != null) {
			for (int i=0; i<dataJSonArr.length(); i++) {
				JSONObject dataJSonObj = dataJSonArr.optJSONObject(i);

				if (dataJSonObj != null) {
					Integer datasourceId = dataJSonObj.optInt("id", -1);
					configs.remove1(datasourceId);
				}
			}
		}
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
					ClientConfig clientConfig = new ClientConfig();
					clientConfig.update(dataJSonObj);
					if (clientConfig != null) {
						configs.put(clientConfig.getId(),
								clientConfig.getClientName(),
								clientConfig);
						newClientConfigs.add(clientConfig);
					}
				}
			}
		}

		return newClientConfigs;
	}
	public synchronized void updateClientConfig(ServletRequest request) throws JSONException, IOException, FileNotFoundException, TemplateException {
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
						File oldClientFolder = null;
						File oldConfigFolder = null;
						if (clientConfig != null) {
							oldClientFolder = FileFinder.getAtlasMapperClientFolder(this.applicationFolder, clientConfig, false);
							oldConfigFolder = FileFinder.getAtlasMapperClientConfigFolder(this.applicationFolder, clientConfig, false);
						}

						clientConfig.update(dataJSonObj);

						// Ensure there is only one default
/*
						if (clientConfig.isDefault()) {
							for (Map.Entry<Integer, ClientConfig> clientEntry : this.globalConfig.getClientConfigs().entrySet()) {
								if (!clientId.equals(clientEntry.getKey())) {
									clientConfig.setDefault(false);
								}
							}
						}
*/
						File newClientFolder = null;
						File newConfigFolder = null;
						if (clientConfig != null) {
							newClientFolder = FileFinder.getAtlasMapperClientFolder(this.applicationFolder, clientConfig, false);
							newConfigFolder = FileFinder.getAtlasMapperClientConfigFolder(this.applicationFolder, clientConfig, false);
						}

						if (oldClientFolder != null && !oldClientFolder.equals(newClientFolder)) {
							// The project generation path has changed. The client folder has to be move.
							if (oldClientFolder != null && newClientFolder != null && oldClientFolder.exists()) {
								File parentFolder = newClientFolder.getParentFile();
								if (parentFolder != null && !parentFolder.exists()) {
									parentFolder.mkdirs();
								}
								oldClientFolder.renameTo(newClientFolder);
							}
						}
						if (oldConfigFolder != null && !oldConfigFolder.equals(newConfigFolder)) {
							// The project generation path has changed. The client folder has to be move.
							if (oldConfigFolder != null && newConfigFolder != null && oldConfigFolder.exists()) {
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

	public JSONObject getClientLayers(String clientName, String[] layerIds, boolean live) throws JSONException, MalformedURLException, IOException, ServiceException {
		if (Utils.isBlank(clientName)) { return null; }

		return this.getClientLayers(this.getClientConfig(clientName), layerIds, live);
	}

	public JSONObject getClientLayers(ClientConfig clientConfig, String[] layerIds, boolean live) throws JSONException, MalformedURLException, IOException, ServiceException {
		if (clientConfig == null) { return null; }

		return this._getClientLayers(clientConfig, layerIds, live);
	}

	private JSONObject _getClientLayers(ClientConfig clientConfig, String[] layerIds, boolean live) throws JSONException, MalformedURLException, IOException, ServiceException {
		return _getClientLayers(clientConfig, Arrays.asList(layerIds), live);
	}
	private JSONObject _getClientLayers(ClientConfig clientConfig, Collection<String> layerIds, boolean live) throws JSONException, MalformedURLException, IOException, ServiceException {
		if (clientConfig == null) { return null; }

		JSONObject foundLayers = new JSONObject();

		JSONObject clientLayers = null;
		clientLayers = this.getClientConfigFileJSon(clientConfig, ConfigType.LAYERS, live, live);

		for (String rawLayerId : layerIds) {
			String layerId = rawLayerId.trim();
			if (clientLayers.has(layerId)) {
				foundLayers.put(layerId, clientLayers.get(layerId));
			}
		}

		return foundLayers;
	}

	public MultiKeyHashMap<Integer, String, DatasourceConfig> getDatasourceConfigs() throws JSONException, FileNotFoundException {
		this.reloadServerConfigIfNeeded();
		return this.datasourceConfigs;
	}

	public JSONArray getDatasourceConfigsJSon() throws JSONException, FileNotFoundException {
		return this._getDatasourceConfigsJSon(true);
	}
	private JSONArray _getDatasourceConfigsJSon(boolean reload) throws JSONException, FileNotFoundException {
		JSONArray datasourceConfigArray = null;
		datasourceConfigArray = new JSONArray();

		MultiKeyHashMap<Integer, String, DatasourceConfig> configs = reload ? this.getDatasourceConfigs() : this.datasourceConfigs;
		for (DatasourceConfig datasourceConfig : configs.values()) {
			datasourceConfigArray.put(datasourceConfig.toJSonObject());
		}
		return datasourceConfigArray;
	}

	private MultiKeyHashMap<Integer, String, ClientConfig> getClientConfigs() throws JSONException, FileNotFoundException {
		this.reloadServerConfigIfNeeded();
		return this.clientConfigs;
	}

	public ClientConfig getClientConfig(String clientName) throws JSONException, FileNotFoundException {
		if (Utils.isBlank(clientName)) {
			return null;
		}

		MultiKeyHashMap<Integer, String, ClientConfig> configs = this.getClientConfigs();
		if (configs == null) {
			return null;
		}

		return configs.get2(clientName);
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

	public List<String> getProxyAllowedHosts(String clientName, boolean live)
			throws JSONException, FileNotFoundException, MalformedURLException, IOException, ServiceException {

		ClientConfig clientConfig = this.getClientConfig(clientName);
		return getProxyAllowedHosts(clientConfig, live);
	}
	public List<String> getProxyAllowedHosts(ClientConfig clientConfig, boolean live)
			throws JSONException, FileNotFoundException, MalformedURLException, IOException, ServiceException {

		List<String> allowedHosts = null;

		JSONObject clientJSON = this.getClientConfigFileJSon(clientConfig, ConfigType.FULL, live, live);
		if (clientJSON != null && clientJSON.has("datasources")) {
			allowedHosts = new ArrayList<String>();
			JSONObject datasources = clientJSON.optJSONObject("datasources");
			Iterator<String> keys = datasources.keys();
			if (keys != null) {
				while (keys.hasNext()) {
					JSONObject datasource = datasources.optJSONObject(keys.next());

					// Only add the first one that successed
					boolean success =
							this.addProxyAllowedHost(allowedHosts, datasource.optString("featureRequestsUrl")) ||
							this.addProxyAllowedHost(allowedHosts, datasource.optString("serverUrls"));
				}
			}
		}

		return allowedHosts;
	}
	/*
	public List<String> getProxyAllowedHosts()
			throws JSONException, FileNotFoundException, MalformedURLException, IOException, ServiceException {

		List<String> allowedHosts = new ArrayList<String>();
		for (DatasourceConfig datasourceConfig : this.getDatasourceConfigs().values()) {
			// Get unset URLs from getCapabilities document
			DatasourceConfig overridenDatasourceConfig = WMSCapabilitiesWrapper.getInstance(
					datasourceConfig.getGetCapabilitiesUrl()).applyOverrides(
							datasourceConfig);
			overridenDatasourceConfig =
					datasourceConfig.applyOverrides();

			// Only add the first one that successed
			boolean success =
					this.addProxyAllowedHost(allowedHosts, overridenDatasourceConfig.getFeatureRequestsUrl()) ||
					this.addProxyAllowedHost(allowedHosts, overridenDatasourceConfig.getServerUrls()) ||
					this.addProxyAllowedHost(allowedHosts, overridenDatasourceConfig.getGetCapabilitiesUrl());
		}
		return allowedHosts;
	}
	*/

	private boolean addProxyAllowedHost(List<String> allowedHosts, String urlStr) {
		URL url = null;
		try {
			url = new URL(urlStr);
		} catch (MalformedURLException ex) {
			return false;
		}
		// It should not be null if it succeed, but better not taking chance.
		if (url == null) { return false; }

		allowedHosts.add(url.getHost());
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
		JSONArray clientConfigArray = null;
		clientConfigArray = new JSONArray();
		for (ClientConfig clientConfig : this.getClientConfigs().values()) {
			clientConfigArray.put(clientConfig.toJSonObjectWithClientUrls(context));
		}
		return clientConfigArray;
	}

	public JSONArray getClientConfigsJSon() throws JSONException, FileNotFoundException {
		return this._getClientConfigsJSon(true);
	}
	private JSONArray _getClientConfigsJSon(boolean reload) throws JSONException, FileNotFoundException {
		JSONArray clientConfigArray = null;
		clientConfigArray = new JSONArray();

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

	public User getUser(String loginName) throws JSONException, FileNotFoundException {
		if (loginName == null) { return null; }

		this.reloadUsersConfigIfNeeded();

		return this.users.get(loginName);
	}

	public void generateAllClients()
			throws JSONException, MalformedURLException, IOException, ServiceException, FileNotFoundException, TemplateException {

		// Emplty the capabilities cache before regenerating the configs
		WMSCapabilitiesWrapper.clearCapabilitiesDocumentsCache();

		for (ClientConfig clientConfig : this.getClientConfigs().values()) {
			this._generateClient(clientConfig);
		}
	}

	public void generateClient(Integer clientId)
			throws JSONException, MalformedURLException, IOException, ServiceException, FileNotFoundException, TemplateException {

		if (clientId == null) {
			return;
		}

		// Emplty the capabilities cache before regenerating the configs
		WMSCapabilitiesWrapper.clearCapabilitiesDocumentsCache();

		this._generateClient(this.getClientConfigs().get1(clientId));
	}

	public void generateClient(ClientConfig clientConfig)
			throws JSONException, MalformedURLException, IOException, ServiceException, FileNotFoundException, TemplateException {

		// Emplty the capabilities cache before regenerating the configs
		WMSCapabilitiesWrapper.clearCapabilitiesDocumentsCache();

		this._generateClient(clientConfig);
	}

	private void _generateClient(ClientConfig clientConfig)
			throws JSONException, MalformedURLException, IOException, ServiceException, FileNotFoundException, TemplateException {

		if (clientConfig == null) {
			return;
		}

		boolean useGoogle = clientConfig.useGoogle(this);

		this.copyClientFilesIfNeeded(clientConfig);
		JSONObject generatedFullConfig = this.getClientConfigFileJSon(clientConfig, ConfigType.FULL, false, true);
		JSONObject generatedEmbededConfig = this.getClientConfigFileJSon(clientConfig, ConfigType.EMBEDED, false, true);
		JSONObject generatedLayers = this.getClientConfigFileJSon(clientConfig, ConfigType.LAYERS, false, true);
		this.parseTemplates(clientConfig, useGoogle);
		this.saveGeneratedConfigs(clientConfig, generatedFullConfig, generatedEmbededConfig, generatedLayers);
	}

	private void copyClientFilesIfNeeded(ClientConfig clientConfig) throws IOException {
		if (clientConfig == null) { return; }

		File atlasMapperClientFolder =
				FileFinder.getAtlasMapperClientFolder(this.applicationFolder, clientConfig);
		if (atlasMapperClientFolder == null) { return; }

		// Return if the folder is not empty
		String[] folderContent = atlasMapperClientFolder.list();
		if (folderContent != null && folderContent.length > 0) {
			return;
		}

		// The folder is Empty, copying the files
		try {
			File src = FileFinder.getAtlasMapperClientSourceFolder();
			Utils.recursiveFileCopy(src, atlasMapperClientFolder);
		} catch (URISyntaxException ex) {
			throw new IOException("Can not get a File reference to the AtlasMapperClient", ex);
		}
	}

	// Create all files that required a template processing
	private void parseTemplates(ClientConfig clientConfig, boolean useGoogle) throws IOException, FileNotFoundException, TemplateException {
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
			indexValues.put("timestamp", ""+Utils.getCurrentTimestamp());
			indexValues.put("useGoogle", useGoogle);
			Utils.processTemplate(templatesConfig, "index.html", indexValues, atlasMapperClientFolder);

			Map<String, Object> embededValues = new HashMap<String, Object>();
			embededValues.put("timestamp", ""+Utils.getCurrentTimestamp());
			embededValues.put("useGoogle", useGoogle);
			Utils.processTemplate(templatesConfig, "embeded.html", embededValues, atlasMapperClientFolder);

			this.parsePreviewTemplate(clientConfig, useGoogle);
		} catch (URISyntaxException ex) {
			throw new IOException("Can not get a File reference to the AtlasMapperClient", ex);
		}
	}

	private void parsePreviewTemplate(ClientConfig clientConfig, boolean useGoogle) throws IOException, FileNotFoundException, TemplateException {
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
				previewValues.put("clientName", clientConfig.getClientName());
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
	 * @param clientId
	 * @return A JSONObject containing all the config for the current
	 * configuration and the generated one, in the following format:
	 * "fullClient": {
	 *     "current": {...},
	 *     "generated": {...}
	 * },
	 * "embededClient": {
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
			throws JSONException, MalformedURLException, IOException, ServiceException {

		if (clientConfig == null) {
			return null;
		}

		JSONObject debug = new JSONObject();

		JSONObject fullClientConfigs = new JSONObject();
		fullClientConfigs.put("current", Utils.jsonToStr(this.getClientConfigFileJSon(clientConfig, ConfigType.FULL, false, false)));
		fullClientConfigs.put("generated", Utils.jsonToStr(this.getClientConfigFileJSon(clientConfig, ConfigType.FULL, true, true)));

		JSONObject embededClientConfigs = new JSONObject();
		embededClientConfigs.put("current", Utils.jsonToStr(this.getClientConfigFileJSon(clientConfig, ConfigType.EMBEDED, false, false)));
		embededClientConfigs.put("generated", Utils.jsonToStr(this.getClientConfigFileJSon(clientConfig, ConfigType.EMBEDED, true, true)));

		JSONObject layers = new JSONObject();
		layers.put("current", Utils.jsonToStr(this.getClientConfigFileJSon(clientConfig, ConfigType.LAYERS, false, false)));
		layers.put("generated", Utils.jsonToStr(this.getClientConfigFileJSon(clientConfig, ConfigType.LAYERS, true, true)));

		debug.put("fullClient", fullClientConfigs);
		debug.put("embededClient", embededClientConfigs);
		debug.put("layers", layers);

		return debug;
	}

	public JSONObject getClientConfigFileJSon(ClientConfig clientConfig, ConfigType configType, boolean live, boolean generate)
			throws JSONException, MalformedURLException, IOException, ServiceException {

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

			case EMBEDED:
				JSONObject embededConfig = null;
				if (generate) {
					embededConfig = this._generateAbstractClientConfig(clientConfig);

					JSONObject modules = this.generateModules(
							clientConfig.getEmbededClientModules(),
							clientConfig);
					if (modules != null && modules.length() > 0) {
						embededConfig.put("modules", modules);
					}
				} else {
					embededConfig = this.loadExistingConfig(this.getClientEmbededConfigFile(clientConfig));
				}

				this._setProxyUrl(embededConfig, clientConfig, live);
				return embededConfig;

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

	// Use as a base for Full and Embeded config
	private JSONObject _generateAbstractClientConfig(ClientConfig clientConfig)
			throws JSONException, MalformedURLException, IOException, ServiceException {

		if (clientConfig == null) { return null; }

		JSONObject json = new JSONObject();
		json.put("clientName", clientConfig.getClientName());

		// TODO Remove when the default saved state will be implemented
		json.put("defaultLayers", this.getClientDefaultLayers(clientConfig.getClientName()));

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

		MultiKeyHashMap<Integer, String, DatasourceConfig> configs = this.getDatasourceConfigs();
		JSONArray datasourcesArray = clientConfig.getDatasources();
		if (datasourcesArray != null && datasourcesArray.length() > 0) {
			JSONObject datasources = new JSONObject();
			for (int i=0; i<datasourcesArray.length(); i++) {
				// https://github.com/douglascrockford/JSON-java/issues/24
				String datasourceName = datasourcesArray.optString(i, null);
				if (datasourceName != null) {
					DatasourceConfig datasourceConfig =
							configs.get2(datasourceName);
					if (datasourceConfig != null) {
						DatasourceConfig overridenDatasourceConfig = datasourceConfig;

						// Apply overrides from the capabilities document
						if (Utils.isNotBlank(datasourceConfig.getWmsServiceUrl())) {
							WMSCapabilitiesWrapper wmsCaps = WMSCapabilitiesWrapper.getInstance(
									datasourceConfig.getWmsServiceUrl());
							if (wmsCaps != null) {
								overridenDatasourceConfig = wmsCaps.applyOverrides(
												datasourceConfig);
							}
						}

						if (overridenDatasourceConfig != null) {
							// Apply overrides from the datasource
							overridenDatasourceConfig =
									overridenDatasourceConfig.applyOverrides();
						}

						if (overridenDatasourceConfig != null) {
							JSONObject datasource =
									this.generateDatasource(overridenDatasourceConfig);
							if (datasource != null) {
								datasources.put(overridenDatasourceConfig.getDatasourceId(), datasource);
							}
						}
					}
				}
			}

			if (datasources.length() > 0) {
				json.put("datasources", datasources);
			}
		}

		return json;
	}

	private void _setProxyUrl(JSONObject clientJSON, ClientConfig clientConfig, boolean live) throws JSONException, UnsupportedEncodingException {
		if (clientConfig.isUseLayerService()) {
			if (Utils.isNotBlank(this.defaultProxyUrl)) {
				String proxyUrl = Utils.addUrlParameter(
						this.defaultProxyUrl,
						"client",
						clientConfig.getClientName());

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

	private JSONObject getClientDefaultLayers(String clientName) throws JSONException, MalformedURLException, IOException, ServiceException {
		if (Utils.isBlank(clientName)) { return null; }

		return this._getClientLayers(
				this.getClientConfig(clientName),
				this._getClientDefaultLayerIds(clientName),
				true);
	}

	private Set<String> _getClientDefaultLayerIds(String clientName) throws JSONException, MalformedURLException, IOException, ServiceException {
		if (Utils.isBlank(clientName)) { return null; }

		ClientConfig clientConfig = this.getClientConfig(clientName);
		if (clientConfig == null) { return null; }

		return clientConfig.getDefaultLayersSet();
	}

	private JSONObject generateDatasource(DatasourceConfig datasourceConfig)
			throws JSONException, MalformedURLException, IOException, ServiceException {

		JSONObject datasource = new JSONObject();

		if (Utils.isNotBlank(datasourceConfig.getFeatureRequestsUrl())) {
			datasource.put("featureRequestsUrl", datasourceConfig.getFeatureRequestsUrl());
		}

		if (Utils.isNotBlank(datasourceConfig.getWmsServiceUrl())) {
			datasource.put("wmsServiceUrl", datasourceConfig.getWmsServiceUrl());
		}

		if (Utils.isNotBlank(datasourceConfig.getExtraWmsServiceUrls())) {
			datasource.put("extraWmsServiceUrls", datasourceConfig.getExtraWmsServiceUrls());
		}

		if (Utils.isNotBlank(datasourceConfig.getWebCacheUrl())) {
			datasource.put("webCacheUrl", datasourceConfig.getWebCacheUrl());
		}

		if (Utils.isNotBlank(datasourceConfig.getLegendUrl())) {
			datasource.put("legendUrl", datasourceConfig.getLegendUrl());
		}

		if (Utils.isNotBlank(datasourceConfig.getDatasourceName())) {
			datasource.put("datasourceName", datasourceConfig.getDatasourceName());
		}

		String[] webCacheParametersArray = datasourceConfig.getWebCacheParametersArray();
		if (webCacheParametersArray != null && webCacheParametersArray.length > 0) {
			JSONArray webCacheParameters = new JSONArray(webCacheParametersArray);
			datasource.put("webCacheSupportedParameters", webCacheParameters);
		}

		if (Utils.isNotBlank(datasourceConfig.getDatasourceType())) {
			datasource.put("datasourceType", datasourceConfig.getDatasourceType());
		}

		JSONObject legendParameters = datasourceConfig.getLegendParameters();
		if (legendParameters != null && legendParameters.length() > 0) {
			datasource.put("legendParameters", legendParameters);
		}

		if (Utils.isNotBlank(datasourceConfig.getWmsVersion())) {
			datasource.put("wmsVersion", datasourceConfig.getWmsVersion());
		}

		return datasource;
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
				ModuleHelper.generateModuleConfiguration(moduleConfig, this, clientConfig);

		if (moduleJSONConfig == null) {
			LOGGER.log(Level.SEVERE, "Can not generate the configuration for {0}", moduleConfig);
		}

		return moduleJSONConfig;
	}

	private JSONObject generateLayer(LayerConfig layerConfig) throws JSONException {
		JSONObject jsonLayer = new JSONObject();

		if (Utils.isNotBlank(layerConfig.getTitle())) {
			jsonLayer.put("title", layerConfig.getTitle());
		}

		if (Utils.isNotBlank(layerConfig.getDescription())) {
			jsonLayer.put("description", layerConfig.getDescription());
		}

		// serverId
		if (Utils.isNotBlank(layerConfig.getDatasourceId())) {
			jsonLayer.put("datasourceId", layerConfig.getDatasourceId());
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
			Collections.sort(styles);
			JSONObject jsonStyles = new JSONObject();
			if (styles != null && !styles.isEmpty()) {
				boolean hasDefaultStyle = false;
				JSONObject firstStyle = null;
				for (LayerStyleConfig style : styles) {
					if (style.isDefault() != null && style.isDefault()) {
						hasDefaultStyle = true;
					}

					String styleName = style.getName();
					if (Utils.isNotBlank(styleName)) {
						JSONObject jsonStyle = this.generateLayerStyle(style);
						if (jsonStyle != null && jsonStyle.length() > 0) {
							if (firstStyle == null) {
								firstStyle = jsonStyle;
							}
							jsonStyles.put(styleName, jsonStyle);
						}
					}
				}
				if (!hasDefaultStyle) {
					firstStyle.put("default", true);
				}
			}
			if (jsonStyles.length() > 0) {
				jsonLayer.put("wmsStyles", jsonStyles);
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
			JSONObject embededConfig,
			JSONObject layers) throws JSONException, IOException {

		File fullClientFile = this.getClientFullConfigFile(clientConfig);
		if (fullClientFile == null) {
			throw new IllegalArgumentException("No file provided for the Full client configuration.");
		} else {
			this.saveJSONConfig(fullConfig, fullClientFile);
		}

		File embededClientFile = this.getClientEmbededConfigFile(clientConfig);
		if (embededClientFile == null) {
			throw new IllegalArgumentException("No file provided for the Embeded client configuration.");
		} else {
			this.saveJSONConfig(embededConfig, embededClientFile);
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
				try { writer.close(); } catch(Exception e) {}
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
			try { bw.close(); } catch (Exception e) {}
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
			try { bw.close(); } catch (Exception e) {}
		}
	}

	private JSONObject loadExistingConfig(File configFile) throws JSONException, FileNotFoundException, IOException {
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
				try { reader.close(); } catch(Exception e) {}
			}
		} else {
			LOGGER.log(Level.SEVERE, "Can read the configuration file [{0}]", configFile.getAbsolutePath());
		}

		return existingConfig;
	}

	private Integer getNextDatasourceId() throws JSONException, FileNotFoundException {
		while (this.datasourceConfigs.containsKey1(this.lastDatasourceId)) {
			this.lastDatasourceId++;
		}
		return this.lastDatasourceId;
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
			try { reader.close(); } catch (Exception ex) {}
		}

		JSONObject jsonObj = null;
		String jsonStr = jsonStrBuf.toString();
		if (jsonStr != null && jsonStr.length() > 0) {
			jsonObj = new JSONObject(jsonStrBuf.toString());
		}
		return jsonObj;
	}
}
