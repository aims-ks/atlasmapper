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
import au.gov.aims.atlasmapperserver.jsonWrappers.client.LayerWrapper;
import org.json.JSONException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

// Basic SAX example, to have something to starts with.
// http://www.mkyong.com/java/how-to-read-xml-file-in-java-sax-parser/
public class TC211Parser {
	private static final Logger LOGGER = Logger.getLogger(TC211Parser.class.getName());

	/**
	 * Cached
	 * @param configManager Config manager associated to that URL, for caching purpose
	 * @param dataSource Data source associated to that URL, for caching purpose
	 * @param url Url of the document to parse
	 * @param mandatory True to cancel the client generation if the file cause problem
	 * @param validMimeType True if the mime type specified in the document is TC211. This parameter is used to ignored warnings concerning MEST document with invalid mime type.
	 * @return
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws JSONException
	 */
	public static TC211Document parseURL(ConfigManager configManager, AbstractDataSourceConfig dataSource, URL url, boolean mandatory, boolean validMimeType)
			throws SAXException, ParserConfigurationException, IOException, JSONException {

		String urlStr = url.toString();
		File cachedDocumentFile = null;

		TC211Document tc211Document = null;
		try {
			cachedDocumentFile = URLCache.getURLFile(
					configManager,
					dataSource,
					urlStr,
					validMimeType ? URLCache.Category.MEST_RECORD : URLCache.Category.BRUTEFORCE_MEST_RECORD,
					mandatory);
			tc211Document = parseFile(cachedDocumentFile, urlStr);
			if (tc211Document == null) {
				File rollbackFile = URLCache.rollbackURLFile(configManager, cachedDocumentFile, urlStr, "Invalid TC211 document");
				tc211Document = parseFile(rollbackFile, urlStr);
			} else {
				URLCache.commitURLFile(configManager, cachedDocumentFile, urlStr);
			}
		} catch (Exception ex) {
			// Parsing a file that has already been accepted - Very unlikely to throw an exception here
			File rollbackFile = URLCache.rollbackURLFile(configManager, cachedDocumentFile, urlStr, ex);
			tc211Document = parseFile(rollbackFile, urlStr);
		}

		// Still no document? Assuming the MEST service is GeoNetwork, try to craft a better URL
		if (tc211Document == null) {
			URL craftedUrl = null;
			try {
				craftedUrl = craftGeoNetworkMestUrl(url);
			} catch (Exception ex) {
				// This should not happen
				LOGGER.log(Level.WARNING, "Unexpected error occurred while crafting a GeoNetwork URL", ex);
			}
			if (craftedUrl != null) {
				String craftedUrlStr = craftedUrl.toString();
				try {

					cachedDocumentFile = URLCache.getURLFile(
							configManager,
							dataSource,
							craftedUrlStr,
							validMimeType ? URLCache.Category.MEST_RECORD : URLCache.Category.BRUTEFORCE_MEST_RECORD,
							mandatory);
					tc211Document = parseFile(cachedDocumentFile, craftedUrlStr);
					if (tc211Document == null) {
						File rollbackFile = URLCache.rollbackURLFile(configManager, cachedDocumentFile, craftedUrlStr, "Invalid TC211 document");
						tc211Document = parseFile(rollbackFile, craftedUrlStr);
					} else {
						// NOTE: The capabilities document refer to a MEST document, but the URL is not
						//     an actual TC211 MEST record. Using some basic URL crafting, the AtlasMapper
						//     managed to find a URL that returns a valid MEST record. Therefore, the
						//     invalid URL should be linked (redirection) to the valid crafted one,
						//     so the application will not try to re-download the HTML one again.
						URLCache.setRedirection(configManager, urlStr, craftedUrlStr);
						URLCache.commitURLFile(configManager, cachedDocumentFile, craftedUrlStr);
					}
				} catch (Exception ex) {
					// Parsing a file that has already been accepted - Very unlikely to throw an exception here
					File rollbackFile = URLCache.rollbackURLFile(configManager, cachedDocumentFile, craftedUrlStr, "Invalid TC211 document");
					tc211Document = parseFile(rollbackFile, craftedUrlStr);
				}
			}
		}

		return tc211Document;
	}

	/**
	 * 1. Find pattern in the URL that would be good indication that we have a GeoNetwork URL.
	 * 2. Craft a URL to the XML document.
	 *
	 * Example:
	 *     http://www.cmar.csiro.au/geonetwork/srv/en/metadata.show?uuid=urn:cmar.csiro.au:dataset:13028&currTab=full
	 * should return:
	 *     http://www.cmar.csiro.au/geonetwork/srv/en/iso19139.xml?uuid=urn:cmar.csiro.au:dataset:13028&currTab=full
	 *
	 * Example:
	 *     http://www.cmar.csiro.au/geonetwork/srv/en/metadata.show?id=44003&currTab=full
	 * should return:
	 *     http://www.cmar.csiro.au/geonetwork/srv/en/iso19139.xml?id=44003&currTab=full
	 * @param brokenUrl
	 * @return
	 */
	protected static URL craftGeoNetworkMestUrl(URL brokenUrl) throws URISyntaxException, MalformedURLException {
		if (brokenUrl == null) {
			return null;
		}

		String brokenUrlQuery = brokenUrl.getQuery();
		if (Utils.isNotBlank(brokenUrlQuery)) {
			// The Scheme is the URL's protocol (http)
			String scheme = brokenUrl.getProtocol();

			// The Authority is the URL's host and the port number if needed
			int port = brokenUrl.getPort(); // 80, 443, etc. -1 if not set
			String authority = brokenUrl.getHost() + (port > 0 ? ":"+port : ""); // www.domain.com:80

			// URI and URL agree to call this a path
			String path = brokenUrl.getPath(); // geonetwork/srv/en/metadata.show

			String[] brokenUrlParams = brokenUrlQuery.split("&");
			for (String paramStr : brokenUrlParams) {
				String[] param = paramStr.split("=");
				if ("id".equalsIgnoreCase(param[0]) || "uuid".equalsIgnoreCase(param[0])) {
					// Transform "geonetwork/srv/en/metadata.show" into "geonetwork/srv/en/iso19139.xml"
					int slashIdx = path.lastIndexOf("/");

					String newPath = slashIdx <= 0 ? "/iso19139.xml" :
							path.substring(0, slashIdx) + "/iso19139.xml";

					URI uri = new URI(
							scheme,
							authority,
							newPath,
							brokenUrlQuery,
							null);

					String cleanUrlStr = uri.toASCIIString();

					// Return null if the crafted URL is the same as the input one.
					if (cleanUrlStr.equals(brokenUrl.toString())) {
						return null;
					}

					return new URL(cleanUrlStr);
				}
			}
		}

		return null;
	}

	private static SAXParser getSAXParser() throws SAXException, ParserConfigurationException {
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

		return doc.isEmpty() ? null : doc;
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

	private static int seq = 0; // tmp layer id sequence
	public static LayerWrapper createLayer(ConfigManager configManager, TC211Document document, TC211Document.Link link) throws JSONException {
		TC211Document.Protocol protocol = link.getProtocol();
		if (protocol == null) { return null; }

		String serviceUrl = link.getUrl();
		String linkName = link.getName();

		// Create a custom WMS layer, with all the info available in the metadata document
		LayerWrapper layer = new LayerWrapper();
		if (protocol.isOGC()) {
			layer.setLayerType("WMS");
			layer.setServiceUrl(serviceUrl);
			if (Utils.isBlank(linkName)) {
				return null;
			}

		} else if (protocol.isKML()) {
			layer.setLayerType("KML");
			layer.setKmlUrl(serviceUrl);
			if (Utils.isBlank(linkName)) {
				linkName = "KML";
			}

		} else {
			return null;
		}

		seq++;
		layer.setLayerId("TMP_" + seq + "_" + linkName);
		layer.setLayerName(linkName);

		// *** MEST Description ***
		String layerDescription = link.getDescription();
		if (Utils.isNotBlank(layerDescription)) {
			layer.setDescription(layerDescription);
			layer.setDescriptionFormat("wiki");
		}

		// The layer title is replace with the title from the MEST link description.
		String titleStr = linkName;
		if (Utils.isNotBlank(titleStr)) {
			layer.setTitle(titleStr);
		}

		return layer;
	}
}
