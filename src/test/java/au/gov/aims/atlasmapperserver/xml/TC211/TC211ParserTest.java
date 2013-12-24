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

import junit.framework.TestCase;
import org.xml.sax.SAXParseException;

import java.io.InputStream;
import java.net.URL;
import java.util.List;

public class TC211ParserTest extends TestCase {

	public void testParseURIFullDocument() throws Exception {
		URL url = TC211ParserTest.class.getClassLoader().getResource("tc211_iso19139_full.xml"); // "http://mest.aodn.org.au/geonetwork/srv/en/iso19139.xml?uuid=87263960-92f0-4836-b8c5-8486660ddfe0";

		InputStream inputStream = null;
		TC211Document doc = null;
		try {
			inputStream = url.openStream();
			doc = TC211Parser.parseInputStream(inputStream, "tc211_iso19139_full.xml");
		} finally {
			if (inputStream != null) {
				inputStream.close();
			}
		}

		assertEquals("Abstract do not match",
				"This dataset is an acid sulfate soils map for the coastline from Tannum Sands to Gladstone, Central Queensland. It shows the presence of...",
				doc.getAbstract());

		List<TC211Document.Link> links = doc.getLinks();
		assertEquals("Number of read links do not match", 2, links.size());

		for (TC211Document.Link link : links) {
			String linkUrl = link.getUrl();

			if (linkUrl.equals("")) {
				assertNull("Link protocol miss match for link URL: " + linkUrl,
						link.getProtocol());

				assertNull("Link name miss match for link URL: " + linkUrl,
						link.getName());

				assertNull("Link description miss match for link URL: " + linkUrl,
						link.getDescription());

			} else if (linkUrl.equals("http://www.environment.gov.au/metadataexplorer/download_test_form.jsp?dataTitle=Key Ecological Features within the Coral Sea&dataPoCemail=marine.metadata@environment.gov.au&dataFormat=Shapefile")) {
				assertEquals("Link protocol miss match for link URL: " + linkUrl,
						TC211Document.Protocol.WWW_LINK_1_0_HTTP_DOWNLOADDATA, link.getProtocol());

				assertNull("Link name miss match for link URL: " + linkUrl,
						link.getName());

				assertEquals("Link description miss match for link URL: " + linkUrl,
						"Downloadable Data", link.getDescription());

			} else {
				fail("Unexpected link URL: " + linkUrl);
			}
		}
	}



	public void testParseURIFullMcpDocument() throws Exception {
		URL url = TC211ParserTest.class.getClassLoader().getResource("tc211_iso19139-mcp_full.xml"); // "http://mest.aodn.org.au/geonetwork/srv/en/iso19139.xml?uuid=87263960-92f0-4836-b8c5-8486660ddfe0";

		InputStream inputStream = null;
		TC211Document doc = null;
		try {
			inputStream = url.openStream();
			doc = TC211Parser.parseInputStream(inputStream, "tc211_iso19139-mcp_full.xml");
		} finally {
			if (inputStream != null) {
				inputStream.close();
			}
		}

		assertEquals("Abstract do not match",
				"A number of ecological features that are of conservation value because of the role they play in the environment of the Coral Sea have been identified and mapped. Key ecological features (KEFs) meet one or more of the following criteria:\n1. a species, group of species or a community with a regionally important ecological role (e.g. a predator, prey that affects a large biomass or a number of marine species);\n2. a species, group of species or a community that is nationally or regionally important for biodiversity;\n3. an area or habitat that is nationally or regionally important for:\na) enhanced or high productivity (such as predictable upwellings - an upwelling occurs when cold nutrient rich waters from the bottom of the ocean rise to the surface);\nb) aggregations of marine life (such as feeding, resting, breeding or nursery areas);\nc) biodiversity and endemism (species which only occur in a specific area); or\n4. a unique seafloor feature, with known or presumed ecological properties of regional significance.\n\n\nKEFs have been identified by the Australian Government on the basis of advice from scientists about the ecological processes and characteristics of the area. A workshop held in Perth in September 2006 also contributed to this scientific advice and helped to underpin the identification of key ecological features. Three KEFs have been identified in the Coral Sea:\n\n1. Tasmantid seamount chain\n2. Reefs, cays and hebivorous fish of the Queensland Plateau\n3. Reefs, cays and hebivorous fish of the Marion Plateau\n\nIn order to create a spatial representation of KEFs for the Coral Sea, some interpretation of the information was required. DSEWPaC has made every effort to use the best available spatial information and best judgement on how to spatially represent the features based on the scientific advice provided. This does not preclude others from making their own interpretation of available information.",
				doc.getAbstract());

		List<TC211Document.Link> links = doc.getLinks();
		assertEquals("Number of read links do not match", 3, links.size());

		for (TC211Document.Link link : links) {
			String linkUrl = link.getUrl();

			if (linkUrl.equals("http://mest.aodn.org.au:80/geonetwork/srv/en/metadata.show?uuid=87263960-92f0-4836-b8c5-8486660ddfe0")) {
				assertEquals("Link protocol miss match for link URL: " + linkUrl,
						TC211Document.Protocol.WWW_LINK_1_0_HTTP_METADATA_URL, link.getProtocol());

				assertNull("Link name miss match for link URL: " + linkUrl,
						link.getName());

				assertEquals("Link description miss match for link URL: " + linkUrl,
						"Point of truth URL of this metadata record", link.getDescription());

			} else if (linkUrl.equals("")) {
				assertEquals("Link protocol miss match for link URL: " + linkUrl,
						TC211Document.Protocol.OGC_WMS_1_1_1_HTTP_GET_MAP, link.getProtocol());

				assertEquals("Link name miss match for link URL: " + linkUrl,
						"", link.getName());

				assertEquals("Link description miss match for link URL: " + linkUrl,
						"", link.getDescription());

			} else if (linkUrl.equals("http://www.environment.gov.au/metadataexplorer/download_test_form.jsp?dataTitle=Key Ecological Features within the Coral Sea&dataPoCemail=marine.metadata@environment.gov.au&dataFormat=Shapefile")) {
				assertEquals("Link protocol miss match for link URL: " + linkUrl,
						TC211Document.Protocol.WWW_LINK_1_0_HTTP_DOWNLOADDATA, link.getProtocol());

				assertNull("Link name miss match for link URL: " + linkUrl,
						link.getName());

				assertEquals("Link description miss match for link URL: " + linkUrl,
						"Downloadable Data", link.getDescription());

			} else {
				fail("Unexpected link URL: " + linkUrl);
			}
		}
	}


	public void testParseURIMinimalMcpDocument() throws Exception {
		URL url = TC211ParserTest.class.getClassLoader().getResource("tc211_iso19139-mcp_minimal.xml");

		InputStream inputStream = null;
		TC211Document doc = null;
		try {
			inputStream = url.openStream();
			doc = TC211Parser.parseInputStream(inputStream, "tc211_iso19139-mcp_minimal.xml");
		} finally {
			if (inputStream != null) {
				inputStream.close();
			}
		}

		assertEquals("Abstract do not match",
				"Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
				doc.getAbstract());

		List<TC211Document.Link> links = doc.getLinks();
		assertEquals("Number of read links do not match", 1, links.size());

		for (TC211Document.Link link : links) {
			String linkUrl = link.getUrl();

			assertEquals("Link URL miss match",
					"http://www.lipsum.com/", linkUrl);

			assertEquals("Link protocol miss match",
					TC211Document.Protocol.WWW_LINK_1_0_HTTP_METADATA_URL, link.getProtocol());

			assertEquals("Link name miss match",
					"Lorem Ipsum", link.getName());

			assertEquals("Link description miss match",
					"Point of truth URL of this metadata record", link.getDescription());
		}
	}



	public void testParseURIAODNMcpExample() throws Exception {
		URL url = TC211ParserTest.class.getClassLoader().getResource("tc211_iso19139-mcp_AODN-example.xml");

		InputStream inputStream = null;
		TC211Document doc = null;
		try {
			inputStream = url.openStream();
			doc = TC211Parser.parseInputStream(inputStream, "tc211_iso19139-mcp_AODN-example.xml");
		} finally {
			if (inputStream != null) {
				inputStream.close();
			}
		}

		assertEquals("Abstract do not match",
				"Link provided as an example in the AODN Cookbook",
				doc.getAbstract());

		List<TC211Document.Link> links = doc.getLinks();
		assertEquals("Number of read links do not match", 1, links.size());

		for (TC211Document.Link link : links) {
			String linkUrl = link.getUrl();

			assertEquals("Link URL miss match",
					"http://imos2.ersa.edu.au/geo2/imos/wms", linkUrl);

			assertEquals("Link protocol miss match",
					TC211Document.Protocol.OGC_WMS_1_1_1_HTTP_GET_MAP, link.getProtocol());

			assertEquals("Link name miss match",
					"imos:ctd_profile_vw", link.getName());

			assertEquals("Link description miss match",
					"AATAMS Realtime Satellite Animal Tracks", link.getDescription());
		}
	}



	public void testParseURIUnbalancedMcpDocument() throws Exception {
		URL url = TC211ParserTest.class.getClassLoader().getResource("tc211_iso19139-mcp_unbalanced.xml");
		SAXParseException expectedException = null;
		try {
			InputStream inputStream = null;
			TC211Document doc = null;
			try {
				inputStream = url.openStream();
				doc = TC211Parser.parseInputStream(inputStream, "tc211_iso19139-mcp_unbalanced.xml");
			} finally {
				if (inputStream != null) {
					inputStream.close();
				}
			}
		} catch (SAXParseException ex) {
			expectedException = ex;
		}

		assertNotNull("The SAX Parser didn't throw an exception from an unbalanced XML document.", expectedException);
	}


	public void testCraftGeoNetworkMestUrl() throws Exception {
		URL craftedUrl;

		// Strait forward
		craftedUrl = TC211Parser.craftGeoNetworkMestUrl(new URL("http://www.cmar.csiro.au/geonetwork/srv/en/metadata.show?id=44003"));
		assertEquals("http://www.cmar.csiro.au/geonetwork/srv/en/iso19139.xml?id=44003", craftedUrl.toString());

		craftedUrl = TC211Parser.craftGeoNetworkMestUrl(new URL("http://www.cmar.csiro.au/geonetwork/srv/en/metadata.show?uuid=urn:cmar.csiro.au:dataset:13028"));
		assertEquals("http://www.cmar.csiro.au/geonetwork/srv/en/iso19139.xml?uuid=urn:cmar.csiro.au:dataset:13028", craftedUrl.toString());

		// Do not return the same thing
		craftedUrl = TC211Parser.craftGeoNetworkMestUrl(new URL("http://www.cmar.csiro.au/geonetwork/srv/en/iso19139.xml?currTab=full&uuid=urn:cmar.csiro.au:dataset:13028"));
		assertNull(craftedUrl);

		// Different schema / port
		craftedUrl = TC211Parser.craftGeoNetworkMestUrl(new URL("https://www.cmar.csiro.au:8443/geonetwork/srv/en/metadata.show?id=44003"));
		assertEquals("https://www.cmar.csiro.au:8443/geonetwork/srv/en/iso19139.xml?id=44003", craftedUrl.toString());

		// ID or UUID mixed with other parameters
		craftedUrl = TC211Parser.craftGeoNetworkMestUrl(new URL("http://www.cmar.csiro.au/geonetwork/srv/en/metadata.show?uuid=urn:cmar.csiro.au:dataset:13028&currTab=full"));
		assertEquals("http://www.cmar.csiro.au/geonetwork/srv/en/iso19139.xml?uuid=urn:cmar.csiro.au:dataset:13028&currTab=full", craftedUrl.toString());

		craftedUrl = TC211Parser.craftGeoNetworkMestUrl(new URL("http://www.cmar.csiro.au/geonetwork/srv/en/metadata.show?id=44003&currTab=full"));
		assertEquals("http://www.cmar.csiro.au/geonetwork/srv/en/iso19139.xml?id=44003&currTab=full", craftedUrl.toString());

		craftedUrl = TC211Parser.craftGeoNetworkMestUrl(new URL("http://www.cmar.csiro.au/geonetwork/srv/en/metadata.show?currTab=full&id=44003"));
		assertEquals("http://www.cmar.csiro.au/geonetwork/srv/en/iso19139.xml?currTab=full&id=44003", craftedUrl.toString());

		// Parameter ID / UUID missing
		craftedUrl = TC211Parser.craftGeoNetworkMestUrl(new URL("http://www.cmar.csiro.au/geonetwork/srv/en/metadata.show?currTab=full&not-uuid=urn:cmar.csiro.au:dataset:13028"));
		assertNull(craftedUrl);

		craftedUrl = TC211Parser.craftGeoNetworkMestUrl(new URL("http://www.cmar.csiro.au/geonetwork/srv/en/metadata.show?currTab=full&not-id=44003"));
		assertNull(craftedUrl);

		craftedUrl = TC211Parser.craftGeoNetworkMestUrl(new URL("http://www.google.com"));
		assertNull(craftedUrl);

		// ID / UUID present, in a URL that do not quite looks like a GeoNetwork URL
		craftedUrl = TC211Parser.craftGeoNetworkMestUrl(new URL("http://www.google.com?id=44003"));
		assertEquals("http://www.google.com/iso19139.xml?id=44003", craftedUrl.toString());

		// Ensure stability (no exception) - Do not really care about the output...
		TC211Parser.craftGeoNetworkMestUrl(null);
		TC211Parser.craftGeoNetworkMestUrl(new URL("http://www.google.com/"));
		TC211Parser.craftGeoNetworkMestUrl(new URL("http://www.google.com/id=12"));
		TC211Parser.craftGeoNetworkMestUrl(new URL("http://www.google.com/?id=12"));
		TC211Parser.craftGeoNetworkMestUrl(new URL("http://www.google.com/?"));
		TC211Parser.craftGeoNetworkMestUrl(new URL("http://www.google.com/?&"));
		TC211Parser.craftGeoNetworkMestUrl(new URL("http://www.google.com/?&id=12"));
		TC211Parser.craftGeoNetworkMestUrl(new URL("http://www.google.com/?&/"));
		TC211Parser.craftGeoNetworkMestUrl(new URL("http://www.google.com/?&/id=12"));
		TC211Parser.craftGeoNetworkMestUrl(new URL("http://www.google.com//?&/"));
		TC211Parser.craftGeoNetworkMestUrl(new URL("http://www.google.com//?id=12&/"));
	}
}
