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
package au.gov.aims.atlasmapperserver.xml.WMTS;

import au.gov.aims.atlasmapperserver.ConfigManager;
import au.gov.aims.atlasmapperserver.URLCache;
import au.gov.aims.atlasmapperserver.dataSourceConfig.AbstractDataSourceConfig;
import org.json.JSONException;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Logger;

public class WMTSParser {
	private static final Logger LOGGER = Logger.getLogger(WMTSParser.class.getName());

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
	public static WMTSDocument parseURL(ConfigManager configManager, AbstractDataSourceConfig dataSource, URL url, boolean mandatory, boolean clearCapabilitiesCache)
			throws SAXException, ParserConfigurationException, IOException, JSONException {

		String urlStr = url.toString();
		File cachedDocumentFile = null;

		WMTSDocument wmtsDocument = null;
		try {
			cachedDocumentFile = URLCache.getURLFile(configManager, dataSource, urlStr, mandatory, clearCapabilitiesCache);
			wmtsDocument = parseFile(cachedDocumentFile, urlStr);
			URLCache.commitURLFile(configManager, cachedDocumentFile, urlStr);
		} catch (Exception ex) {
			File rollbackFile = URLCache.rollbackURLFile(configManager, cachedDocumentFile, urlStr, ex);
			wmtsDocument = parseFile(rollbackFile, urlStr);
		}

		return wmtsDocument;
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
	 * @param location For debugging purpose
	 * @return
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws JSONException
	 */
	public static WMTSDocument parseFile(File file, String location)
			throws SAXException, ParserConfigurationException, IOException, JSONException {

		if (file == null || !file.exists()) {
			return null;
		}

		SAXParser saxParser = getSAXParser();

		WMTSDocument doc = new WMTSDocument(location);
		WMTSHandler handler = new WMTSHandler(doc);

		saxParser.parse(file, handler);

		if (doc.getLayer() == null) {
			return null;
		}

		return doc;
	}

	/**
	 * NOT Cached (Used for tests)
	 * @param inputStream
	 * @param location For debugging purpose
	 * @return
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws JSONException
	 */
	public static WMTSDocument parseInputStream(InputStream inputStream, String location)
			throws SAXException, ParserConfigurationException, IOException, JSONException {

		if (inputStream == null) {
			throw new IllegalArgumentException("Can not parse null XML stream. " + location);
		}

		SAXParser saxParser = getSAXParser();

		WMTSDocument doc = new WMTSDocument(location);
		WMTSHandler handler = new WMTSHandler(doc);

		saxParser.parse(inputStream, handler);

		if (doc.getLayer() == null) {
			return null;
		}

		return doc;
	}
}
