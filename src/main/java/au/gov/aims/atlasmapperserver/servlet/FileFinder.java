package au.gov.aims.atlasmapperserver.servlet;

import au.gov.aims.atlasmapperserver.ClientConfig;
import au.gov.aims.atlasmapperserver.Utils;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;

/**
 * Library used to locate configuration files for the AtlasMapper server
 * and the AtlasMapper client.
 * @author glafond
 */
public class FileFinder {
	private static final Logger LOGGER = Logger.getLogger(FileFinder.class.getName());

	// DATA_DIR_PROPERTY can be set in many different ways (same as GeoServer)
	// 1. tomcat/bin/setenv.sh
	//     Add this line to CATALINA_OPTS variable (replace <path to the config file> with the desired absolute path to the config folder)
	//     -DATLASMAPPER_DATA_DIR=<path to the config file>
	// 2. ???
	// 3. ???
	// NOTE: Don't forget to restart tomcat after setting this variable.
	public static final String DATA_DIR_PROPERTY = "ATLASMAPPER_DATA_DIR";


	private static final String CLIENT_CONFIG_FOLDER = "config";
	private static final String ATLASMAPPERCLIENT_FOLDER = "amc";
	private static final String ATLASMAPPERCLIENT_TEMPLATES_FOLDER = "amcTemplates";

	// Must match web.xml, do not starts with a "/" nor ends with a "*" or "/".
	private static final String CLIENT_BASE_URL = "client";
	private static final String CLIENT_WELCOME_PAGE = "index.html";
	private static final String CLIENT_PREVIEW_PAGE = "preview.html";

	public static File getClientFile(ServletContext context, String fileRelativePathWithClientpath) {
		if (context == null || Utils.isBlank(fileRelativePathWithClientpath)) {
			return null;
		}

		return new File(getApplicationFolder(context, false), fileRelativePathWithClientpath);
	}

	public static String getAtlasMapperClientURL(ServletContext context, ClientConfig clientConfig, boolean preview) {
		if (clientConfig == null) {
			return null;
		}

		String welcomePage = CLIENT_WELCOME_PAGE;
		if (preview) {
			if (!clientConfig.isUseLayerService()) {
				// The preview need the layer info service to get live configuration.
				return null;
			}

			welcomePage = CLIENT_PREVIEW_PAGE;
		}

		// Check if the Welcome file exists on the file system
		File clientFolder = getAtlasMapperClientFolder(getApplicationFolder(context, false), clientConfig);
		if (clientFolder == null || !clientFolder.isDirectory()) {
			// The client has not been generated
			return null;
		}
		String[] content = clientFolder.list();
		Arrays.sort(content);
		if (Arrays.binarySearch(content, welcomePage) < 0) {
			// The Welcome file do not exists
			return null;
		}

		String baseUrl = getAtlasMapperClientBaseURL(context, clientConfig).trim();

		String url = null;
		if (Utils.isNotBlank(baseUrl)) {
			if (!baseUrl.endsWith("/")) {
				baseUrl += "/";
			}
			url = baseUrl + ATLASMAPPERCLIENT_FOLDER + "/" + welcomePage;
		}

		return url;
	}

	private static String getAtlasMapperClientBaseURL(ServletContext context, ClientConfig clientConfig) {
		if (clientConfig == null) { return null; }
		String clientBaseUrlOverride = clientConfig.getBaseUrl();

		if (Utils.isNotBlank(clientBaseUrlOverride)) {
			return clientBaseUrlOverride;
		}

		return context.getContextPath() +
				"/" + CLIENT_BASE_URL +
				"/" + safeClientFoldername(clientConfig.getClientName());
	}

	public static String getDefaultProxyURL(ServletContext context) {
		return context.getContextPath() + "/proxy";
	}
	public static String getDefaultLayerInfoServiceURL(ServletContext context) {
		return context.getContextPath() + "/public/layersInfo.jsp";
	}

	public static File getAtlasMapperClientFolder(File applicationFolder, ClientConfig clientConfig) {
		return getAtlasMapperClientFolder(applicationFolder, clientConfig, true);
	}
	public static File getAtlasMapperClientFolder(File applicationFolder, ClientConfig clientConfig, boolean create) {
		if (applicationFolder == null || clientConfig == null) {
			return null;
		}
		File clientFolder = getClientFolder(applicationFolder, clientConfig, false);
		if (clientFolder == null) {
			return null;
		}

		File amcFolder = new File(clientFolder, ATLASMAPPERCLIENT_FOLDER);

		if (create && amcFolder != null && !amcFolder.exists()) {
			// Try to create the folder structure, if it doesn't exist
			amcFolder.mkdirs();
		}

		return amcFolder;
	}

	public static File getAtlasMapperClientSourceFolder() throws URISyntaxException {
		URL url = FileFinder.class.getResource("/" + ATLASMAPPERCLIENT_FOLDER);
		if (url == null) { return null; }

		return new File(url.toURI());
	}

	public static File getAtlasMapperClientTemplatesFolder() throws URISyntaxException {
		URL url = FileFinder.class.getResource("/" + ATLASMAPPERCLIENT_TEMPLATES_FOLDER);
		if (url == null) { return null; }

		return new File(url.toURI());
	}

	public static File getAtlasMapperClientConfigFolder(File applicationFolder, ClientConfig clientConfig) {
		return getAtlasMapperClientConfigFolder(applicationFolder, clientConfig, true);
	}
	public static File getAtlasMapperClientConfigFolder(File applicationFolder, ClientConfig clientConfig, boolean create) {
		if (applicationFolder == null || clientConfig == null) {
			return null;
		}
		File clientFolder = getClientFolder(applicationFolder, clientConfig, false);
		if (clientFolder == null) {
			return null;
		}

		File clientConfigFolder = new File(clientFolder, CLIENT_CONFIG_FOLDER);

		if (create && clientConfigFolder != null && !clientConfigFolder.exists()) {
			// Try to create the folder structure, if it doesn't exist
			clientConfigFolder.mkdirs();
		}

		return clientConfigFolder;
	}

	public static File getClientFolder(File applicationFolder, ClientConfig clientConfig) {
		return getClientFolder(applicationFolder, clientConfig, true);
	}
	public static File getClientFolder(File applicationFolder, ClientConfig clientConfig, boolean create) {
		if (applicationFolder == null || clientConfig == null) {
			return null;
		}

		File clientFolder = null;
		String clientFolderOverrideStr = clientConfig.getGeneratedFileLocation();
		if (Utils.isNotBlank(clientFolderOverrideStr)) {
			clientFolder = new File(clientFolderOverrideStr);
		} else {
			clientFolder = new File(applicationFolder, safeClientFoldername(clientConfig.getClientName()));
		}

		if (create && clientFolder != null && !clientFolder.exists()) {
			// Try to create the folder structure, if it doesn't exist
			clientFolder.mkdirs();
		}

		return clientFolder;
	}

	private static String safeClientFoldername(String clientname) {
		if (Utils.isBlank(clientname)) {
			return null;
		}

		// Only allow "-", "_" and alphanumeric
		clientname.replaceAll("[^A-Za-z0-9-_]", "");

		return clientname;
	}

	public static File getApplicationFolder(ServletContext context) {
		return getApplicationFolder(context, true);
	}
	public static File getApplicationFolder(ServletContext context, boolean create) {
		if (context == null) {
			return null;
		}

		File applicationFolder = null;
		String dataDir = getDataDirPropertyValue(context);

		if (dataDir != null) {
			applicationFolder = new File(dataDir);
		}
		if (!Utils.recursiveIsWritable(applicationFolder)) {
			if (applicationFolder != null) {
				LOGGER.log(Level.SEVERE, "The application do not have write access to the folder: [" + applicationFolder.getAbsolutePath() + "] defined by the property " + DATA_DIR_PROPERTY + ".");
			}
		}

		if (create && applicationFolder != null && !applicationFolder.exists()) {
			// Try to create the folder structure, if it doesn't exist
			applicationFolder.mkdirs();
		}

		return applicationFolder;
	}

	// Similar to what GeoServer do
	public static String getDataDirPropertyValue(ServletContext context) {
		if (context == null) { return null; }

		String dataDir = context.getInitParameter(DATA_DIR_PROPERTY);
		if (Utils.isBlank(dataDir)) {
			dataDir = System.getProperty(DATA_DIR_PROPERTY);
		}
		if (Utils.isBlank(dataDir)) {
			dataDir = System.getenv(DATA_DIR_PROPERTY);
		}
		if (Utils.isNotBlank(dataDir)) {
			return dataDir.trim();
		}
		return null;
	}
}
