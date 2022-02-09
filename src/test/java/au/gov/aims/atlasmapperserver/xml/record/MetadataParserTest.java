/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2012 Australian Institute of Marine Science
 *
 *  Contact: Gael Lafond <g.lafond@aims.gov.au>
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
package au.gov.aims.atlasmapperserver.xml.record;

import au.gov.aims.atlasmapperserver.thread.ThreadLogger;
import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXParseException;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetadataParserTest {

    @Test
    public void testGetMetadataSchema() throws Exception {
        Map<String, MetadataParser.MetadataSchema> expectedSchemaMap = new HashMap<>();
        expectedSchemaMap.put("TC211/tc211_iso19139_full.xml", MetadataParser.MetadataSchema.ISO19139);
        expectedSchemaMap.put("TC211/tc211_iso19139-mcp_full.xml", MetadataParser.MetadataSchema.ISO19139_MCP);
        expectedSchemaMap.put("TC211/tc211_iso19139-mcp_minimal.xml", MetadataParser.MetadataSchema.ISO19139_MCP);
        expectedSchemaMap.put("TC211/tc211_iso19139-mcp_AODN-example.xml", MetadataParser.MetadataSchema.ISO19139_MCP);
        expectedSchemaMap.put("TC211/tc211_iso19139-mcp_unbalanced.xml", MetadataParser.MetadataSchema.ISO19139_MCP);
        expectedSchemaMap.put("TC211/tc211_unpublished.xml", MetadataParser.MetadataSchema.UNPUBLISHED);
        expectedSchemaMap.put("TC211/tc211_notfound.xml", null);
        expectedSchemaMap.put("TC211_201803/tc211_201803_metadata_record.xml", MetadataParser.MetadataSchema.ISO19115_3_2018);
        // Not a metadata record
        expectedSchemaMap.put("geoWebCache1-4_wmts.xml", null);
        // HTML document
        expectedSchemaMap.put("google.html", null);
        // Not a XML document
        expectedSchemaMap.put("text_file.txt", null);
        // Binary file
        expectedSchemaMap.put("binary_file.png", null);

        for (Map.Entry<String, MetadataParser.MetadataSchema> expectedSchemaEntry : expectedSchemaMap.entrySet()) {
            String xmlFilepath = expectedSchemaEntry.getKey();
            MetadataParser.MetadataSchema expectedSchema = expectedSchemaEntry.getValue();

            URL url = MetadataParserTest.class.getClassLoader().getResource(xmlFilepath);
            Assert.assertNotNull(String.format("Can not find resource file: %s", xmlFilepath), url);

            MetadataParser.MetadataSchema actualSchema = null;
            try(InputStream inputStream = url.openStream()) {
                MetadataParser parser = MetadataParser.getInstance();
                actualSchema = parser.getMetadataSchema(inputStream);
            }

            Assert.assertEquals(String.format("Wrong metadata schema found with resource file: %s", xmlFilepath),
                expectedSchema, actualSchema);
        }
    }

    @Test
    public void testParseURIFullDocument() throws Exception {
        // "http://mest.aodn.org.au/geonetwork/srv/en/iso19139.xml?uuid=87263960-92f0-4836-b8c5-8486660ddfe0";
        String xmlFilepath = "TC211/tc211_iso19139_full.xml";
        URL url = MetadataParserTest.class.getClassLoader().getResource(xmlFilepath);
        Assert.assertNotNull(String.format("Can not find XML file: %s", xmlFilepath), url);
        File xmlFile = new File(url.toURI());

        ThreadLogger logger = new ThreadLogger();

        MetadataParser parser = MetadataParser.getInstance();
        MetadataParser.MetadataSchema schema = parser.getMetadataSchema(xmlFile);
        MetadataDocument doc = parser.parseFile(logger, xmlFile, xmlFilepath, schema);

        Assert.assertNotNull(String.format("The XML document is null: %s", xmlFilepath), doc);
        Assert.assertFalse(String.format("The XML document is empty: %s", xmlFilepath), doc.isEmpty());

        Assert.assertEquals("Abstract do not match",
                "This dataset is an acid sulfate soils map for the coastline from Tannum Sands to Gladstone, Central Queensland. It shows the presence of...",
                doc.getAbstract());

        List<MetadataDocument.Link> links = doc.getLinks();
        Assert.assertEquals("Number of read links do not match", 3, links.size());

        for (MetadataDocument.Link link : links) {
            String linkUrl = link.getUrl();

            if (linkUrl.equals("")) {
                Assert.assertNull("Link protocol miss match for link URL: " + linkUrl,
                        link.getProtocol());

                Assert.assertNull("Link name miss match for link URL: " + linkUrl,
                        link.getName());

                Assert.assertNull("Link description miss match for link URL: " + linkUrl,
                        link.getDescription());

            } else if (linkUrl.equals("http://www.environment.gov.au/metadataexplorer/download_test_form.jsp?dataTitle=Key Ecological Features within the Coral Sea&dataPoCemail=marine.metadata@environment.gov.au&dataFormat=Shapefile")) {
                Assert.assertEquals("Link protocol miss match for link URL: " + linkUrl,
                        MetadataDocument.Protocol.WWW_LINK_1_0_HTTP_DOWNLOADDATA, link.getProtocol());

                Assert.assertNull("Link name miss match for link URL: " + linkUrl,
                        link.getName());

                Assert.assertEquals("Link description miss match for link URL: " + linkUrl,
                        "Downloadable Data", link.getDescription());

            } else if (linkUrl.equals("TC211/tc211_iso19139_full.xml")) {
                Assert.assertEquals("Link protocol miss match for link URL: " + linkUrl,
                        MetadataDocument.Protocol.WWW_LINK_1_0_HTTP_METADATA_URL, link.getProtocol());

                Assert.assertEquals("Link name miss match",
                        "Original XML metadata record", link.getName());

                Assert.assertNull("Link description miss match", link.getDescription());

            } else {
                Assert.fail("Unexpected link URL: " + linkUrl);
            }
        }
    }


    @Test
    public void testParseURIFullMcpDocument() throws Exception {
        // "http://mest.aodn.org.au/geonetwork/srv/en/iso19139.xml?uuid=87263960-92f0-4836-b8c5-8486660ddfe0";
        String xmlFilepath = "TC211/tc211_iso19139-mcp_full.xml";
        URL url = MetadataParserTest.class.getClassLoader().getResource(xmlFilepath);
        Assert.assertNotNull(String.format("Can not find XML file: %s", xmlFilepath), url);
        File xmlFile = new File(url.toURI());

        ThreadLogger logger = new ThreadLogger();

        MetadataParser parser = MetadataParser.getInstance();
        MetadataParser.MetadataSchema schema = parser.getMetadataSchema(xmlFile);
        MetadataDocument doc = parser.parseFile(logger, xmlFile, xmlFilepath, schema);

        Assert.assertNotNull(String.format("The XML document is null: %s", xmlFilepath), doc);
        Assert.assertFalse(String.format("The XML document is empty: %s", xmlFilepath), doc.isEmpty());

        Assert.assertEquals("Abstract do not match",
                "A number of ecological features that are of conservation value because of the role they play in the environment of the Coral Sea have been identified and mapped. Key ecological features (KEFs) meet one or more of the following criteria:\n1. a species, group of species or a community with a regionally important ecological role (e.g. a predator, prey that affects a large biomass or a number of marine species);\n2. a species, group of species or a community that is nationally or regionally important for biodiversity;\n3. an area or habitat that is nationally or regionally important for:\na) enhanced or high productivity (such as predictable upwellings - an upwelling occurs when cold nutrient rich waters from the bottom of the ocean rise to the surface);\nb) aggregations of marine life (such as feeding, resting, breeding or nursery areas);\nc) biodiversity and endemism (species which only occur in a specific area); or\n4. a unique seafloor feature, with known or presumed ecological properties of regional significance.\n\n\nKEFs have been identified by the Australian Government on the basis of advice from scientists about the ecological processes and characteristics of the area. A workshop held in Perth in September 2006 also contributed to this scientific advice and helped to underpin the identification of key ecological features. Three KEFs have been identified in the Coral Sea:\n\n1. Tasmantid seamount chain\n2. Reefs, cays and hebivorous fish of the Queensland Plateau\n3. Reefs, cays and hebivorous fish of the Marion Plateau\n\nIn order to create a spatial representation of KEFs for the Coral Sea, some interpretation of the information was required. DSEWPaC has made every effort to use the best available spatial information and best judgement on how to spatially represent the features based on the scientific advice provided. This does not preclude others from making their own interpretation of available information.",
                doc.getAbstract());

        List<MetadataDocument.Link> links = doc.getLinks();
        Assert.assertEquals("Number of read links do not match", 3, links.size());

        for (MetadataDocument.Link link : links) {
            String linkUrl = link.getUrl();

            if (linkUrl.equals("http://mest.aodn.org.au:80/geonetwork/srv/en/metadata.show?uuid=87263960-92f0-4836-b8c5-8486660ddfe0")) {
                Assert.assertEquals("Link protocol miss match for link URL: " + linkUrl,
                        MetadataDocument.Protocol.WWW_LINK_1_0_HTTP_METADATA_URL, link.getProtocol());

                Assert.assertNull("Link name miss match for link URL: " + linkUrl,
                        link.getName());

                Assert.assertEquals("Link description miss match for link URL: " + linkUrl,
                        "Point of truth URL of this metadata record", link.getDescription());

            } else if (linkUrl.equals("")) {
                Assert.assertEquals("Link protocol miss match for link URL: " + linkUrl,
                        MetadataDocument.Protocol.OGC_WMS_1_1_1_HTTP_GET_MAP, link.getProtocol());

                Assert.assertEquals("Link name miss match for link URL: " + linkUrl,
                        "", link.getName());

                Assert.assertEquals("Link description miss match for link URL: " + linkUrl,
                        "", link.getDescription());

            } else if (linkUrl.equals("http://www.environment.gov.au/metadataexplorer/download_test_form.jsp?dataTitle=Key Ecological Features within the Coral Sea&dataPoCemail=marine.metadata@environment.gov.au&dataFormat=Shapefile")) {
                Assert.assertEquals("Link protocol miss match for link URL: " + linkUrl,
                        MetadataDocument.Protocol.WWW_LINK_1_0_HTTP_DOWNLOADDATA, link.getProtocol());

                Assert.assertNull("Link name miss match for link URL: " + linkUrl,
                        link.getName());

                Assert.assertEquals("Link description miss match for link URL: " + linkUrl,
                        "Downloadable Data", link.getDescription());

            } else {
                Assert.fail("Unexpected link URL: " + linkUrl);
            }
        }
    }

    @Test
    public void testParseURIMinimalMcpDocument() throws Exception {
        String xmlFilepath = "TC211/tc211_iso19139-mcp_minimal.xml";
        URL url = MetadataParserTest.class.getClassLoader().getResource(xmlFilepath);
        Assert.assertNotNull(String.format("Can not find XML file: %s", xmlFilepath), url);
        File xmlFile = new File(url.toURI());

        ThreadLogger logger = new ThreadLogger();

        MetadataParser parser = MetadataParser.getInstance();
        MetadataParser.MetadataSchema schema = parser.getMetadataSchema(xmlFile);
        MetadataDocument doc = parser.parseFile(logger, xmlFile, xmlFilepath, schema);

        Assert.assertNotNull(String.format("The XML document is null: %s", xmlFilepath), doc);
        Assert.assertFalse(String.format("The XML document is empty: %s", xmlFilepath), doc.isEmpty());

        Assert.assertEquals("Abstract do not match",
                "Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
                doc.getAbstract());

        List<MetadataDocument.Link> links = doc.getLinks();
        Assert.assertEquals("Number of read links do not match", 1, links.size());

        for (MetadataDocument.Link link : links) {
            String linkUrl = link.getUrl();

            Assert.assertEquals("Link URL miss match",
                    "http://www.lipsum.com/", linkUrl);

            Assert.assertEquals("Link protocol miss match",
                    MetadataDocument.Protocol.WWW_LINK_1_0_HTTP_METADATA_URL, link.getProtocol());

            Assert.assertEquals("Link name miss match",
                    "Lorem Ipsum", link.getName());

            Assert.assertEquals("Link description miss match",
                    "Point of truth URL of this metadata record", link.getDescription());
        }
    }


    @Test
    public void testParseURIAODNMcpExample() throws Exception {
        String xmlFilepath = "TC211/tc211_iso19139-mcp_AODN-example.xml";
        URL url = MetadataParserTest.class.getClassLoader().getResource(xmlFilepath);
        Assert.assertNotNull(String.format("Can not find XML file: %s", xmlFilepath), url);
        File xmlFile = new File(url.toURI());

        ThreadLogger logger = new ThreadLogger();

        MetadataParser parser = MetadataParser.getInstance();
        MetadataParser.MetadataSchema schema = parser.getMetadataSchema(xmlFile);
        MetadataDocument doc = parser.parseFile(logger, xmlFile, xmlFilepath, schema);

        Assert.assertNotNull(String.format("The XML document is null: %s", xmlFilepath), doc);
        Assert.assertFalse(String.format("The XML document is empty: %s", xmlFilepath), doc.isEmpty());

        Assert.assertEquals("Abstract do not match",
                "Link provided as an example in the AODN Cookbook",
                doc.getAbstract());

        List<MetadataDocument.Link> links = doc.getLinks();
        Assert.assertEquals("Number of read links do not match", 2, links.size());

        for (MetadataDocument.Link link : links) {
            String linkUrl = link.getUrl();

            if (linkUrl.equals("http://imos2.ersa.edu.au/geo2/imos/wms")) {
                Assert.assertEquals("Link protocol miss match",
                        MetadataDocument.Protocol.OGC_WMS_1_1_1_HTTP_GET_MAP, link.getProtocol());

                Assert.assertEquals("Link name miss match",
                        "imos:ctd_profile_vw", link.getName());

                Assert.assertEquals("Link description miss match",
                        "AATAMS Realtime Satellite Animal Tracks", link.getDescription());

            } else if (linkUrl.equals("TC211/tc211_iso19139-mcp_AODN-example.xml")) {
                Assert.assertEquals("Link protocol miss match for link URL: " + linkUrl,
                        MetadataDocument.Protocol.WWW_LINK_1_0_HTTP_METADATA_URL, link.getProtocol());

                Assert.assertEquals("Link name miss match",
                        "Original XML metadata record", link.getName());

                Assert.assertNull("Link description miss match", link.getDescription());

            } else {
                Assert.fail("Unexpected link URL: " + linkUrl);
            }
        }
    }


    @Test
    public void testParseURIUnbalancedMcpDocument() throws Exception {
        String xmlFilepath = "TC211/tc211_iso19139-mcp_unbalanced.xml";
        URL url = MetadataParserTest.class.getClassLoader().getResource(xmlFilepath);
        Assert.assertNotNull(String.format("Can not find XML file: %s", xmlFilepath), url);
        File xmlFile = new File(url.toURI());

        ThreadLogger logger = new ThreadLogger();

        MetadataParser parser = MetadataParser.getInstance();
        MetadataParser.MetadataSchema schema = parser.getMetadataSchema(xmlFile);
        SAXParseException expectedException = null;
        try {
            parser.parseFile(logger, xmlFile, xmlFilepath, schema);
        } catch (SAXParseException ex) {
            expectedException = ex;
        }

        Assert.assertNotNull("The SAX Parser didn't throw an exception from an unbalanced XML document.", expectedException);
    }

    @Test
    public void testParseURITC211_201803Document() throws Exception {
        // https://geonetwork.data.aims.gov.au/geonetwork/srv/api/records/0e5ca52f-5525-44da-b0ca-df758850963e/formatters/xml
        String xmlFilepath = "TC211_201803/tc211_201803_metadata_record.xml";
        URL url = MetadataParserTest.class.getClassLoader().getResource(xmlFilepath);
        Assert.assertNotNull(String.format("Can not find XML file: %s", xmlFilepath), url);
        File xmlFile = new File(url.toURI());

        ThreadLogger logger = new ThreadLogger();

        MetadataParser parser = MetadataParser.getInstance();
        MetadataParser.MetadataSchema schema = parser.getMetadataSchema(xmlFile);
        MetadataDocument doc = parser.parseFile(logger, xmlFile, xmlFilepath, schema);

        Assert.assertNotNull(String.format("The XML document is null: %s", xmlFilepath), doc);
        Assert.assertFalse(String.format("The XML document is empty: %s", xmlFilepath), doc.isEmpty());

        Assert.assertEquals("Abstract do not match",
                "Building knowledge of pearl oyster distribution, particularly their abundance in deep water adjacent to Eighty Mile Beach and connectivity between these deep water populations and populations within fished areas in shallow inshore areas.\\n",
                doc.getAbstract());

        List<MetadataDocument.Link> links = doc.getLinks();
        Assert.assertEquals("Number of read links do not match", 2, links.size());

        for (MetadataDocument.Link link : links) {
            String linkUrl = link.getUrl();

            if (linkUrl.equals("https://domain.com/path/service.xml")) {
                Assert.assertEquals("Link protocol miss match for link URL: " + linkUrl,
                        MetadataDocument.Protocol.WWW_LINK_1_0_HTTP_DOWNLOADDATA, link.getProtocol());

                Assert.assertEquals("Link name miss match",
                        "Download service", link.getName());

                Assert.assertEquals("Link description miss match",
                        "Download data service.", link.getDescription());

            } else if (linkUrl.equals("TC211_201803/tc211_201803_metadata_record.xml")) {
                Assert.assertEquals("Link protocol miss match for link URL: " + linkUrl,
                        MetadataDocument.Protocol.WWW_LINK_1_0_HTTP_METADATA_URL, link.getProtocol());

                Assert.assertEquals("Link name miss match",
                        "Original XML metadata record", link.getName());

                Assert.assertNull("Link description miss match", link.getDescription());

            } else {
                Assert.fail("Unexpected link URL: " + linkUrl);
            }
        }

        double delta = 0.00000000001;

        List<MetadataDocument.Point> points = doc.getPoints();
        Assert.assertNotNull("Points is null", points);
        Assert.assertEquals("Wrong points size", 2, points.size());
        for (MetadataDocument.Point point : points) {
            double lon = point.getLon();
            double lat = point.getLat();
            double z = point.getElevation();

            // Point name / description is ignored.
            // Use "lon" to identify which point we are at
            if (lon > 147.65 && lon < 147.66) {
                Assert.assertEquals("Wrong lon", 147.652213, lon, delta);
                Assert.assertEquals("Wrong lat", -18.831821, lat, delta);
                Assert.assertEquals("Wrong elevation", 0, z, delta);

            } else if (lon > 147.38 && lon < 147.39) {
                Assert.assertEquals("Wrong lon", 147.388955, lon, delta);
                Assert.assertEquals("Wrong lat", -18.255452, lat, delta);
                Assert.assertEquals("Wrong elevation", 0, z, delta);

            } else {
                Assert.fail(String.format("Unexpected point: [%.3f, %.3f] elevation: %.2f", lon, lat, z));
            }
        }

        List<MetadataDocument.Polygon> polygons = doc.getPolygons();
        Assert.assertNotNull("Polygons is null", polygons);
        Assert.assertEquals("Wrong polygons size", 1, polygons.size());
        for (MetadataDocument.Polygon polygon : polygons) {
            List<MetadataDocument.Point> polyPoints = polygon.getPoints();
            Assert.assertNotNull("Polygon points is null", polyPoints);
            Assert.assertEquals("Wrong polygon points size", 4, polyPoints.size());

            for (MetadataDocument.Point point : polyPoints) {
                double lon = point.getLon();
                double lat = point.getLat();
                double z = point.getElevation();

                if (lon > 119.55 && lon < 119.56 &&
                        lat > -17.72 && lat < -17.71) {
                    Assert.assertEquals("Wrong lon", 119.55322265625001, lon, delta);
                    Assert.assertEquals("Wrong lat", -17.712060974461494, lat, delta);
                    Assert.assertEquals("Wrong elevation", 0, z, delta);

                } else if (lon > 121.6 && lon < 121.61 &&
                        lat > -17.72 && lat < -17.71) {
                    Assert.assertEquals("Wrong lon", 121.60766601562501, lon, delta);
                    Assert.assertEquals("Wrong lat", -17.712060974461494, lat, delta);
                    Assert.assertEquals("Wrong elevation", 0, z, delta);

                } else if (lon > 121.6 && lon < 121.61 &&
                        lat > -19.4 && lat < -19.39) {
                    Assert.assertEquals("Wrong lon", 121.60766601562501, lon, delta);
                    Assert.assertEquals("Wrong lat", -19.3992492786023, lat, delta);
                    Assert.assertEquals("Wrong elevation", 0, z, delta);

                } else if (lon > 119.55 && lon < 119.56 &&
                        lat > -19.4 && lat < -19.39) {
                    Assert.assertEquals("Wrong lon", 119.55322265625001, lon, delta);
                    Assert.assertEquals("Wrong lat", -19.3992492786023, lat, delta);
                    Assert.assertEquals("Wrong elevation", 0, z, delta);

                } else {
                    Assert.fail(String.format("Unexpected polygon point: [%.3f, %.3f] elevation: %.2f", lon, lat, z));
                }
            }
        }
    }
}
