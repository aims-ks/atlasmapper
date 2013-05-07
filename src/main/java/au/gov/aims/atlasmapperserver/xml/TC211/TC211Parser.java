/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2012 Australian Institute of Marine Science
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
package au.gov.aims.atlasmapperserver.xml.TC211;

import au.gov.aims.atlasmapperserver.ConfigManager;
import au.gov.aims.atlasmapperserver.URLCache;
import au.gov.aims.atlasmapperserver.Utils;
import au.gov.aims.atlasmapperserver.dataSourceConfig.AbstractDataSourceConfig;
import au.gov.aims.atlasmapperserver.layerConfig.AbstractLayerConfig;
import au.gov.aims.atlasmapperserver.layerConfig.KMLLayerConfig;
import au.gov.aims.atlasmapperserver.layerConfig.WMSLayerConfig;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

// Basic SAX example, to have something to starts with.
// http://www.mkyong.com/java/how-to-read-xml-file-in-java-sax-parser/
public class TC211Parser {
	private static final Logger LOGGER = Logger.getLogger(TC211Parser.class.getName());

	public static final String NL = System.getProperty("line.separator");
	private static final String TITLE_ATTR = "Title:";
	public static final String TITLE_KEY = "TITLE";
	private static final String DESCRIPTION_ATTR = "Description:";
	public static final String DESCRIPTION_KEY = "DESCRIPTION";
	private static final String PATH_ATTR = "Path:";
	public static final String PATH_KEY = "PATH";

	private static final String APPLICATION_PROFILE_ATTR = "AtlasMapper:";

	/**
	 * Cached
	 * @param configManager Config manager associated to that URL, for caching purpose
	 * @param dataSource Data source associated to that URL, for caching purpose
	 * @param url Url of the document to parse
	 * @param mandatory True to cancel the client generation if the file cause problem
	 * @return
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws JSONException
	 */
	public static TC211Document parseURL(ConfigManager configManager, AbstractDataSourceConfig dataSource, URL url, boolean mandatory, boolean harvest)
			throws SAXException, ParserConfigurationException, IOException, JSONException {

		String urlStr = url.toString();
		File cachedDocumentFile = null;

		TC211Document tc211Document = null;
		try {
			cachedDocumentFile = URLCache.getURLFile(configManager, dataSource, urlStr, mandatory, harvest);
			tc211Document = parseFile(cachedDocumentFile, urlStr);
			URLCache.commitURLFile(configManager, cachedDocumentFile, urlStr);
		} catch (Exception ex) {
			File rollbackFile = URLCache.rollbackURLFile(configManager, cachedDocumentFile, urlStr, ex);
			tc211Document = parseFile(rollbackFile, urlStr);
		}

		return tc211Document;
	}

	private static SAXParser getSAXParser() throws SAXException, SAXNotRecognizedException, ParserConfigurationException {
		SAXParserFactory factory = SAXParserFactory.newInstance();

		// Disabling DTD loading & validation
		// Without those 2 lines, initialising XML files takes ages (about 10 minutes for 500kb, down to a few ms with those lines)
		factory.setFeature("http://apache.org/xml/features/validation/schema", false);
		factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		return factory.newSAXParser();
	}

	/**
	 * NOT Cached
	 * @param file
	 * @param location
	 * @return
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws JSONException
	 */
	public static TC211Document parseFile(File file, String location)
			throws SAXException, ParserConfigurationException, IOException, JSONException {

		if (file == null || !file.exists()) {
			return null;
		}

		SAXParser saxParser = getSAXParser();

		TC211Document doc = new TC211Document(location);
		TC211Handler handler = new TC211Handler(doc);

		saxParser.parse(file, handler);

		return doc;
	}

	/**
	 * NOT Cached (Used for tests)
	 * @param inputStream
	 * @param location
	 * @return
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws JSONException
	 */
	public static TC211Document parseInputStream(InputStream inputStream, String location)
			throws SAXException, ParserConfigurationException, IOException, JSONException {

		if (inputStream == null) {
			throw new IllegalArgumentException("Can not parse null XML stream. " + location);
		}

		SAXParser saxParser = getSAXParser();

		TC211Document doc = new TC211Document(location);
		TC211Handler handler = new TC211Handler(doc);

		saxParser.parse(inputStream, handler);

		return doc;
	}





	public static Map<String, StringBuilder> parseMestDescription(String descriptionStr) {
		Map<String, StringBuilder> descriptionMap = new HashMap<String, StringBuilder>();

		String currentKey = null;
		// Scanner is used to process each lines one by one. It's more efficient than using
		// the method split to generate an array of String.
		Scanner scanner = new Scanner(descriptionStr.trim());
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			// This should not happen...
			if (line == null) { line = ""; }
			// Remove trailing white spaces.
			line = line.trim();

			if (line.startsWith(TITLE_ATTR)) {
				currentKey = TITLE_KEY;
				descriptionMap.put(TITLE_KEY, new StringBuilder(
						line.substring(TITLE_ATTR.length()).trim()));

			} else if (line.startsWith(DESCRIPTION_ATTR)) {
				currentKey = DESCRIPTION_KEY;
				descriptionMap.put(DESCRIPTION_KEY, new StringBuilder(
						line.substring(DESCRIPTION_ATTR.length()).trim()));

			} else if (line.startsWith(PATH_ATTR)) {
				currentKey = PATH_KEY;
				descriptionMap.put(PATH_KEY, new StringBuilder(
						line.substring(PATH_ATTR.length()).trim()));

			} else {
				if (currentKey == null) {
					// Text was found before an attribute. No attributes are expected...
					descriptionMap.put(DESCRIPTION_KEY, new StringBuilder(descriptionStr));
					break;
				} else {
					StringBuilder value = descriptionMap.get(currentKey);
					value.append(NL);
					value.append(line);
				}
			}
		}

		return descriptionMap;
	}

	/**
	 * EXPERIMENTAL
	 * This method is used to parse the "gmd:applicationProfile" field of the Metadata document (MEST).
	 * This field is used by the AtlasMapper and other AtlasHub applications (such as the MetadataViewer)
	 * to input some application related values; in the case of the AtlasMapper, the field contains
	 * initial manual overrides.
	 * Example of expected value for the XML field:
	 *     AtlasMapper: {"hasLegend": false, "wmsQueryable": false}, Metadataviewer: {"onlyShow":true}
	 * @param applicationProfileStr The application profile String as found in the XML document.
	 * @return A map of Application name (as key) associated with it related configuration (JSONObject value)
	 */
	// AtlasMapper:{"hasLegend": false,"wmsQueryable": false } Metadataviewer:{"onlyShow":true}
	public static JSONObject parseMestApplicationProfile(String applicationProfileStr) {
		int applicationConfigIndex = applicationProfileStr.indexOf(APPLICATION_PROFILE_ATTR);

		if (applicationConfigIndex < 0) {
			// There is no configuration for the AtlasMapper in this application profile string.
			return null;
		}

		String rawAtlasMapperProfileStr = applicationProfileStr.substring(applicationConfigIndex + APPLICATION_PROFILE_ATTR.length());
		if (rawAtlasMapperProfileStr == null || rawAtlasMapperProfileStr.isEmpty()) {
			// A configuration has been found but it's empty.
			return null;
		}

		rawAtlasMapperProfileStr = rawAtlasMapperProfileStr.trim();
		if (!rawAtlasMapperProfileStr.startsWith("{")) {
			// A configuration has been found but it's not a valid JSON config.
			// Try to find a valid one on the rest of the string.
			return parseMestApplicationProfile(rawAtlasMapperProfileStr);
		}

		int end = findEndOfBloc(rawAtlasMapperProfileStr, 0);
		String atlasMapperProfileStr = rawAtlasMapperProfileStr.substring(0, end+1);

		JSONObject json = null;
		try {
			json = new JSONObject(atlasMapperProfileStr);
		} catch (Exception ex) {
			// Try with the rest of the line
			json = parseMestApplicationProfile(rawAtlasMapperProfileStr);

			if (json == null) {
				// It is possible, but very unlikely, that this error message get displayed more than once
				// for the same String (it may happen if the text "AtlasMapper:" occurred more once in the
				// applicationProfile string).
				LOGGER.log(Level.WARNING, "Invalid JSON configuration for the AtlasMapper \"{0}\" in the applicationProfile \"{1}\"\n{2}", new String[]{ atlasMapperProfileStr, applicationProfileStr, Utils.getExceptionMessage(ex) });
				LOGGER.log(Level.FINE, "Stack trace: ", ex);
			}
		}

		return json;
	}
	private static int findEndOfBloc(String str, int offset) {
		for (int i=offset+1, len=str.length(); i<len; i++) {
			if (str.charAt(i) == '{') {
				i = findEndOfBloc(str, i);
			} else if (str.charAt(i) == '}') {
				return i;
			}
		}
		// No end of block found. Return the index of the last char.
		return str.length()-1;
	}

	private static int seq = 0; // tmp layer id sequence
	public static AbstractLayerConfig createLayer(ConfigManager configManager, TC211Document document, TC211Document.Link link) throws JSONException {
		TC211Document.Protocol protocol = link.getProtocol();
		if (protocol == null) { return null; }

		String serviceUrl = link.getUrl();
		String linkName = link.getName();

		// Create a custom WMS layer, with all the info available in the metadata document
		AbstractLayerConfig layer = null;
		if (protocol.isOGC()) {
			WMSLayerConfig wmsLayer = new WMSLayerConfig(configManager);
			wmsLayer.setDataSourceType("WMS");
			wmsLayer.setServiceUrl(serviceUrl);
			if (Utils.isBlank(linkName)) {
				return null;
			}
			layer = wmsLayer;

		} else if (protocol.isKML()) {
			KMLLayerConfig kmlLayer = new KMLLayerConfig(configManager);
			kmlLayer.setDataSourceType("KML");
			kmlLayer.setKmlUrl(serviceUrl);
			if (Utils.isBlank(linkName)) {
				linkName = "KML";
			}
			layer = kmlLayer;

		} else {
			return null;
		}

		seq++;
		layer.setLayerId("TMP_" + seq + "_" + linkName);
		layer.setLayerName(linkName);

		// *** MEST Description ***

		String layerDescription = link.getDescription();
		// The layer description found in the MEST is added to the description of the AtlasMapper layer.
		// The description may also specified a title for the layer, and other attributes,
		// using the following format:
		//     Title: Coral sea Plateau
		//     Description: Plateau is a flat or nearly flat area...
		//     Subcategory: 2. GBRMPA features
		Map<String, StringBuilder> parsedDescription = TC211Parser.parseMestDescription(layerDescription);

		// The layer title is replace with the title from the MEST link description.
		String titleStr = linkName;
		if (parsedDescription.containsKey(TC211Parser.TITLE_KEY) && parsedDescription.get(TC211Parser.TITLE_KEY) != null) {
			titleStr = parsedDescription.get(TC211Parser.TITLE_KEY).toString().trim();
			if (!titleStr.isEmpty()) {
				layer.setTitle(titleStr);
			}
		}
		if (Utils.isNotBlank(titleStr)) {
			layer.setTitle(titleStr);
		}

		// The description found in the MEST link description (i.e. Layer description) is added
		// at the beginning of the layer description (with a "Dataset description" label to
		//     divide the layer description from the rest).
		if (parsedDescription.containsKey(TC211Parser.DESCRIPTION_KEY) && parsedDescription.get(TC211Parser.DESCRIPTION_KEY) != null) {
			StringBuilder descriptionSb = parsedDescription.get(TC211Parser.DESCRIPTION_KEY);
			if (descriptionSb.length() > 0) {

				String datasetDescription = document.getAbstract();
				if (Utils.isNotBlank(datasetDescription)) {
					descriptionSb.append(TC211Parser.NL); descriptionSb.append(TC211Parser.NL);
					descriptionSb.append("*Dataset description*"); descriptionSb.append(TC211Parser.NL);
					descriptionSb.append(datasetDescription);
				}

				layer.setDescription(descriptionSb.toString().trim());
				layer.setDescriptionFormat("wiki");
			}
		}

		// The path found in the MEST link description override the WMS path in the layer.
		if (parsedDescription.containsKey(TC211Parser.PATH_KEY) && parsedDescription.get(TC211Parser.PATH_KEY) != null) {
			String pathStr = parsedDescription.get(TC211Parser.PATH_KEY).toString().trim();
			if (!pathStr.isEmpty()) {
				layer.setWmsPath(pathStr);
			}
		}

		// *** MEST Application profile ***

		String applicationProfileStr = link.getApplicationProfile();
		if (applicationProfileStr != null && !applicationProfileStr.isEmpty()) {
			JSONObject mestOverrides = TC211Parser.parseMestApplicationProfile(applicationProfileStr);
			if (mestOverrides != null && mestOverrides.length() > 0) {
				layer.applyOverrides(mestOverrides);
			}
		}

		return layer;
	}
}
