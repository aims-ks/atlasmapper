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

import au.gov.aims.atlasmapperserver.URLCache;
import au.gov.aims.atlasmapperserver.dataSourceConfig.AbstractDataSourceConfig;
import org.json.JSONException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Stack;

// Basic SAX example, to have something to starts with.
// http://www.mkyong.com/java/how-to-read-xml-file-in-java-sax-parser/
public class Parser {

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
	public static Document parseURL(AbstractDataSourceConfig dataSource, URL url)
			throws SAXException, ParserConfigurationException, IOException, JSONException {

		String urlStr = url.toString();
		File cachedDocumentFile = URLCache.getURLFile(dataSource, urlStr);

		Document tc211Document = null;
		try {
			tc211Document = parseFile(cachedDocumentFile, urlStr);
			URLCache.commitURLFile(dataSource, cachedDocumentFile, urlStr);
		} catch (Exception ex) {
			File rollbackFile = URLCache.rollbackURLFile(dataSource, cachedDocumentFile, urlStr);
			tc211Document = parseFile(rollbackFile, urlStr);
		}

		return tc211Document;
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

		if (file == null) {
			return null;
		}

		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser saxParser = factory.newSAXParser();

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

		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser saxParser = factory.newSAXParser();

		Document doc = new Document(location);
		TC211Handler handler = new TC211Handler(doc);

		saxParser.parse(inputStream, handler);

		return doc;
	}



	private static class TC211Handler extends DefaultHandler {
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

				if (XMLPathMarker.LINKS.equals(this.xmlPathMarker)) {
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
				String previousQName = this.xmlPath.get(this.xmlPath.size() - 2);

				if (XMLPathMarker.ABSTRACT.equals(this.xmlPathMarker)) {
					this.doc.setAbstract(this.collectedChars.toString());

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
						this.currentLink.setProtocol(this.collectedChars.toString());

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
}
