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
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

// Basic SAX example, to have something to starts with.
// http://www.mkyong.com/java/how-to-read-xml-file-in-java-sax-parser/
public class Parser {
	private static final Logger LOGGER = Logger.getLogger(Parser.class.getName());

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
	 * @param dataSource
	 * @param url
	 * @return
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws JSONException
	 */
	public static Document parseURL(ConfigManager configManager, AbstractDataSourceConfig dataSource, URL url, boolean mandatory)
			throws SAXException, ParserConfigurationException, IOException, JSONException {

		String urlStr = url.toString();
		File cachedDocumentFile = null;

		Document tc211Document = null;
		try {
			cachedDocumentFile = URLCache.getURLFile(configManager, dataSource, urlStr, mandatory);
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
	public static Document parseFile(File file, String location)
			throws SAXException, ParserConfigurationException, IOException, JSONException {

		if (file == null || !file.exists()) {
			return null;
		}

		SAXParser saxParser = getSAXParser();

		Document doc = new Document(location);
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
	public static Document parseInputStream(InputStream inputStream, String location)
			throws SAXException, ParserConfigurationException, IOException, JSONException {

		if (inputStream == null) {
			throw new IllegalArgumentException("Can not parse null XML stream. " + location);
		}

		SAXParser saxParser = getSAXParser();

		Document doc = new Document(location);
		TC211Handler handler = new TC211Handler(doc);

		saxParser.parse(inputStream, handler);

		return doc;
	}



	private static class TC211Handler extends DefaultHandler {
		private static String BBOX_CONTAINER = "gmd:EX_GeographicBoundingBox";
		private static String BBOX_WEST_CONTAINER = "gmd:westBoundLongitude";
		private static String BBOX_EAST_CONTAINER = "gmd:eastBoundLongitude";
		private static String BBOX_SOUTH_CONTAINER = "gmd:southBoundLatitude";
		private static String BBOX_NORTH_CONTAINER = "gmd:northBoundLatitude";
		private static String BBOX_DECIMAL = "gco:Decimal";

		private static String LINK_CONTAINER = "gmd:CI_OnlineResource";

		private static String LINK_URL_CONTAINER = "gmd:linkage";
		private static String LINK_URL_STRING = "gmd:URL";

		private static String LINK_PROTOCOL_CONTAINER = "gmd:protocol";
		private static String LINK_PROTOCOL_STRING = "gco:CharacterString";

		private static String LINK_NAME_CONTAINER = "gmd:name";
		private static String LINK_NAME_STRING = "gco:CharacterString";

		private static String LINK_DESCRIPTION_CONTAINER = "gmd:description";
		private static String LINK_DESCRIPTION_STRING = "gco:CharacterString";

		private static String LINK_APPLICATION_PROFILE_CONTAINER = "gmd:applicationProfile";
		private static String LINK_APPLICATION_PROFILE_STRING = "gco:CharacterString";

		private Document doc;
		private StringBuilder collectedChars;
		private String west, east, south, north;

		private Document.Link currentLink;
		private Stack<String> xmlPath;
		// Marker to highlight important path, without having to check the current path every time.
		private XMLPathMarker xmlPathMarker;

		public TC211Handler(Document doc) {
			super();
			this.doc = doc;
			this.xmlPath = new Stack<String>();

			this.xmlPathMarker = null;
		}

		public void startElement(
				String uri,
				String localName,
				String qName,
				Attributes attributes) throws SAXException {

			this.xmlPath.push(qName);
			this.xmlPathMarker = XMLPathMarker.get(this.xmlPath);

			if (this.xmlPathMarker != null) {
				this.collectedChars = new StringBuilder();

				if (XMLPathMarker.BBOXES.equals(this.xmlPathMarker)) {
					if (BBOX_CONTAINER.equalsIgnoreCase(qName)) {
						this.west = null; this.east = null; this.south = null; this.north = null;
					}

				} else if (XMLPathMarker.LINKS.equals(this.xmlPathMarker)) {
					if (LINK_CONTAINER.equalsIgnoreCase(qName)) {
						this.currentLink = new Document.Link();
					}
				}
			}
		}

		public void endElement(
				String uri,
				String localName,
				String qName) throws SAXException {

			if (this.xmlPathMarker != null) {
				String previousQName = (this.xmlPath.size() < 2 ? null : this.xmlPath.get(this.xmlPath.size() - 2));

				if (XMLPathMarker.ABSTRACT.equals(this.xmlPathMarker)) {
					this.doc.setAbstract(this.collectedChars.toString());

				} else if (XMLPathMarker.BBOXES.equals(this.xmlPathMarker)) {
					/*
					<gmd:EX_GeographicBoundingBox>
						<gmd:westBoundLongitude>
							<gco:Decimal>122.2115</gco:Decimal>
						</gmd:westBoundLongitude>
						<gmd:eastBoundLongitude>
							<gco:Decimal>122.2115</gco:Decimal>
						</gmd:eastBoundLongitude>
						<gmd:southBoundLatitude>
							<gco:Decimal>-18.0158</gco:Decimal>
						</gmd:southBoundLatitude>
						<gmd:northBoundLatitude>
							<gco:Decimal>-18.0158</gco:Decimal>
						</gmd:northBoundLatitude>
					</gmd:EX_GeographicBoundingBox>
					*/

					if (BBOX_WEST_CONTAINER.equalsIgnoreCase(previousQName) && BBOX_DECIMAL.equalsIgnoreCase(qName)) {
						this.west = this.collectedChars.toString();

					} else if (BBOX_EAST_CONTAINER.equalsIgnoreCase(previousQName) && BBOX_DECIMAL.equalsIgnoreCase(qName)) {
						this.east = this.collectedChars.toString();

					} else if (BBOX_SOUTH_CONTAINER.equalsIgnoreCase(previousQName) && BBOX_DECIMAL.equalsIgnoreCase(qName)) {
						this.south = this.collectedChars.toString();

					} else if (BBOX_NORTH_CONTAINER.equalsIgnoreCase(previousQName) && BBOX_DECIMAL.equalsIgnoreCase(qName)) {
						this.north = this.collectedChars.toString();

					} else if (BBOX_CONTAINER.equalsIgnoreCase(qName)) {
						if (this.west != null && this.east != null && this.south != null && this.north != null) {
							try {
								Double west = Double.valueOf(this.west);
								Double east = Double.valueOf(this.east);
								Double south = Double.valueOf(this.south);
								Double north = Double.valueOf(this.north);

								if (west.equals(east) && south.equals(north)) {
									// It's a point
									this.doc.addPoint(new Document.Point(west, north));
								} else {
									// It's a bbox
									Document.Polygon polygon = new Document.Polygon();
									polygon.addPoint(new Document.Point(west, north));
									polygon.addPoint(new Document.Point(east, north));
									polygon.addPoint(new Document.Point(east, south));
									polygon.addPoint(new Document.Point(west, south));

									this.doc.addPolygon(polygon);
								}
							} catch(Exception ex) {
								LOGGER.log(Level.WARNING, "Can not parse the bounding box: [{0}, {1}, {2}, {3}]: {4}", new String[]{
									this.west, this.east, this.south, this.north, Utils.getExceptionMessage(ex)
								});
								LOGGER.log(Level.FINE, "Stack trace:", ex);
							}
						}

						this.west = null; this.east = null; this.south = null; this.north = null;
					}

				} else if (XMLPathMarker.POLYGONS.equals(this.xmlPathMarker)) {
					/*
					lon,lat,elevation lon,lat,elevation etc.
					<gml:coordinates>
						122.2115,-18.0158,0 121.9344,-15.0683,0 121.9669,-13.6502,0 121.9654,-13.6815,0 122.0005,-13.7115,0 122.0398,-13.7039,0 122.0347,-13.7721,0 122.0585,-13.6669,0 122.0209,-13.6215,0 122.0320,-13.6327,0 122.0219,-13.6227,0 122.0283,-13.6267,0 122.0265,-13.6294,0 121.8303,-16.2410,0 122.2170,-18.0027,0 121.8303,-16.2410,0 122.0265,-13.6294,0 122.0283,-13.6267,0 122.0219,-13.6227,0 122.0320,-13.6327,0 122.0209,-13.6215,0 122.0585,-13.6669,0 122.0347,-13.7721,0 122.0398,-13.7039,0 122.0005,-13.7115,0 121.9654,-13.6815,0 121.9669,-13.6502,0 121.9344,-15.0683,0 122.2115,-18.0158,0
					</gml:coordinates>
					*/
					String coords = this.collectedChars.toString();
					if (Utils.isNotBlank(coords)) {
						String[] coordArray = coords.split("\\s+");
						if (coordArray != null && coordArray.length > 0) {
							try {
								Document.Polygon polygon = new Document.Polygon();
								for (String coord : coordArray) {
									String[] coordParts = coord.split(",");
									if (coordParts != null) {
										Double[] parsedCoords = new Double[coordParts.length];
										for (int i=0; i<coordParts.length; i++) {
											parsedCoords[i] = Double.valueOf(coordParts[i]);
										}

										if (parsedCoords.length == 3) {
											polygon.addPoint(parsedCoords[0], parsedCoords[1], parsedCoords[2]);
										} else if (coordParts.length == 2) {
											polygon.addPoint(parsedCoords[0], parsedCoords[1]);
										}
									}
								}
								this.doc.addPolygon(polygon);
							} catch(Exception ex) {
								LOGGER.log(Level.WARNING, "Can not parse the polygon: [{0}]: {1}",
										new String[]{ coords, Utils.getExceptionMessage(ex) });
								LOGGER.log(Level.FINE, "Stack trace:", ex);
							}
						}
					}

				} else if (XMLPathMarker.LINKS.equals(this.xmlPathMarker)) {
					/*
					<gmd:CI_OnlineResource>
						<gmd:linkage>
							<gmd:URL>http://mest.aodn.org.au:80/geonetwork/srv/en/metadata.show?uuid=87263960-92f0-4836-b8c5-8486660ddfe0</gmd:URL>
						</gmd:linkage>
						<gmd:protocol>
							<gco:CharacterString>WWW:LINK-1.0-http--metadata-URL</gco:CharacterString>
						</gmd:protocol>
						<gmd:name gco:nilReason="missing">
							<gco:CharacterString/>
						</gmd:name>
						<gmd:description>
							<gco:CharacterString>Point of truth URL of this metadata record</gco:CharacterString>
						</gmd:description>
					</gmd:CI_OnlineResource>
					*/

					if (LINK_URL_CONTAINER.equalsIgnoreCase(previousQName) && LINK_URL_STRING.equalsIgnoreCase(qName)) {
						this.currentLink.setUrl(this.collectedChars.toString());

					} else if (LINK_PROTOCOL_CONTAINER.equalsIgnoreCase(previousQName) && LINK_PROTOCOL_STRING.equalsIgnoreCase(qName)) {
						this.currentLink.setProtocolStr(this.collectedChars.toString());

					} else if (LINK_NAME_CONTAINER.equalsIgnoreCase(previousQName) && LINK_NAME_STRING.equalsIgnoreCase(qName)) {
						this.currentLink.setName(this.collectedChars.toString());

					} else if (LINK_DESCRIPTION_CONTAINER.equalsIgnoreCase(previousQName) && LINK_DESCRIPTION_STRING.equalsIgnoreCase(qName)) {
						this.currentLink.setDescription(this.collectedChars.toString());

					} else if (LINK_APPLICATION_PROFILE_CONTAINER.equalsIgnoreCase(previousQName) && LINK_APPLICATION_PROFILE_STRING.equalsIgnoreCase(qName)) {
						this.currentLink.setApplicationProfile(this.collectedChars.toString());

					} else if (LINK_CONTAINER.equalsIgnoreCase(qName)) {
						this.doc.addLink(this.currentLink);
						this.currentLink = null;
					}
				}
			}

			// The current QName is not needed (kept for debugging), but the call to the pop method is mandatory...
			String currentQName = this.xmlPath.pop();
			this.xmlPathMarker = XMLPathMarker.get(this.xmlPath);

			/*
			// Log any anomaly in the balance of the document.
			// NOTE: This never happen since a unbalanced document trigger an exception.
			if (!currentQName.equalsIgnoreCase(qName)) {
				LOGGER.log(Level.WARNING, "The document [{0}] has a unbalanced tag:\n# Path: {1}\n# Opening tag: {2}\n# Closing tag: {3}",
						new String[]{this.doc.getUri(), this.xmlPath.toString(), qName, currentQName});
			}
			*/
		}

		public void characters(
				char ch[],
				int start,
				int length) throws SAXException {

			if (this.xmlPathMarker != null) {
				this.collectedChars.append(ch, start, length);
			}
		}
	}

	private static enum XMLPathMarker {
		ABSTRACT (new String[]{"mcp:MD_Metadata", "gmd:identificationInfo", "mcp:MD_DataIdentification", "gmd:abstract", "gco:CharacterString"}),
		BBOXES (new String[]{"mcp:MD_Metadata", "gmd:identificationInfo", "mcp:MD_DataIdentification", "gmd:extent", "gmd:EX_Extent", "gmd:geographicElement", "gmd:EX_GeographicBoundingBox"}),
		POLYGONS (new String[]{"mcp:MD_Metadata", "gmd:identificationInfo", "mcp:MD_DataIdentification", "gmd:extent", "gmd:EX_Extent", "gmd:geographicElement", "gmd:EX_BoundingPolygon", "gmd:polygon", "gml:Polygon", "gml:exterior", "gml:LinearRing", "gml:coordinates"}),
		LINKS (new String[]{"mcp:MD_Metadata", "gmd:distributionInfo", "gmd:MD_Distribution", "gmd:transferOptions", "gmd:MD_DigitalTransferOptions", "gmd:onLine", "gmd:CI_OnlineResource"});

		private final String[] path;

		XMLPathMarker(String[] path) {
			this.path = path;
		}

		public static XMLPathMarker get(List<String> xmlPath) {
			if (xmlPath == null || xmlPath.isEmpty()) {
				return null;
			}

			XMLPathMarker[] markers = XMLPathMarker.values();
			for (XMLPathMarker marker : markers) {
				boolean inPath = true;
				if (xmlPath.size() < marker.path.length) {
					inPath = false;
				}
				for (int i=0, len=marker.path.length; inPath && i<len; i++) {
					if (!marker.path[i].equalsIgnoreCase(xmlPath.get(i))) {
						inPath = false;
					}
				}

				if (inPath) {
					return marker;
				}
			}

			return null;
		}
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
	public static AbstractLayerConfig createLayer(ConfigManager configManager, Document document, Document.Link link) throws JSONException {
		Document.Protocol protocol = link.getProtocol();
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
		Map<String, StringBuilder> parsedDescription = Parser.parseMestDescription(layerDescription);

		// The layer title is replace with the title from the MEST link description.
		String titleStr = linkName;
		if (parsedDescription.containsKey(Parser.TITLE_KEY) && parsedDescription.get(Parser.TITLE_KEY) != null) {
			titleStr = parsedDescription.get(Parser.TITLE_KEY).toString().trim();
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
		if (parsedDescription.containsKey(Parser.DESCRIPTION_KEY) && parsedDescription.get(Parser.DESCRIPTION_KEY) != null) {
			StringBuilder descriptionSb = parsedDescription.get(Parser.DESCRIPTION_KEY);
			if (descriptionSb.length() > 0) {

				String datasetDescription = document.getAbstract();
				if (Utils.isNotBlank(datasetDescription)) {
					descriptionSb.append(Parser.NL); descriptionSb.append(Parser.NL);
					descriptionSb.append("*Dataset description*"); descriptionSb.append(Parser.NL);
					descriptionSb.append(datasetDescription);
				}

				layer.setDescription(descriptionSb.toString().trim());
				layer.setDescriptionFormat("wiki");
			}
		}

		// The path found in the MEST link description override the WMS path in the layer.
		if (parsedDescription.containsKey(Parser.PATH_KEY) && parsedDescription.get(Parser.PATH_KEY) != null) {
			String pathStr = parsedDescription.get(Parser.PATH_KEY).toString().trim();
			if (!pathStr.isEmpty()) {
				layer.setWmsPath(pathStr);
			}
		}

		// *** MEST Application profile ***

		String applicationProfileStr = link.getApplicationProfile();
		if (applicationProfileStr != null && !applicationProfileStr.isEmpty()) {
			JSONObject mestOverrides = Parser.parseMestApplicationProfile(applicationProfileStr);
			if (mestOverrides != null && mestOverrides.length() > 0) {
				layer.applyOverrides(mestOverrides);
			}
		}

		return layer;
	}
}
