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
package au.gov.aims.atlasmapperserver.layerGenerator;

import junit.framework.TestCase;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.logging.Logger;

public class ApplicationProfileParsingTest extends TestCase {
	private static final Logger LOGGER = Logger.getLogger(ApplicationProfileParsingTest.class.getName());


	public void testSingleApplicationParsing() throws JSONException {
		String applicationProfileStr = "AtlasMapper:{\"hasLegend\": false,\"wmsQueryable\": false }";

		JSONObject parsedApplicationProfile = AbstractWMSLayerGenerator.parseApplicationProfile(applicationProfileStr);

		JSONObject expectedValue = new JSONObject("{\"hasLegend\": false,\"wmsQueryable\": false }");
		assertEquals(expectedValue.toString(), parsedApplicationProfile.toString());
	}

	public void testMultipleApplicationsParsing() throws JSONException {
		String applicationProfileStr = "AtlasMapper:{\"hasLegend\": false,\"wmsQueryable\": false } Metadataviewer:{\"onlyShow\":true}";

		JSONObject parsedApplicationProfile = AbstractWMSLayerGenerator.parseApplicationProfile(applicationProfileStr);

		JSONObject expectedValue = new JSONObject("{\"hasLegend\": false,\"wmsQueryable\": false }");
		assertEquals(expectedValue.toString(), parsedApplicationProfile.toString());
	}

	public void testScrewedUpApplicationsParsing() throws JSONException {
		String applicationProfileStr = "Okay1: {\"onlyShow\":true}, Broken: {{\"hasLegend\": false,\"wmsQueryable\": false }, AtlasMapper:{\"hasLegend\": false,\"wmsQueryable\": false } Okay2: {\"onlyShow\":true}";

		JSONObject parsedApplicationProfile = AbstractWMSLayerGenerator.parseApplicationProfile(applicationProfileStr);

		JSONObject expectedValue = new JSONObject("{\"hasLegend\": false,\"wmsQueryable\": false }");
		assertEquals(expectedValue.toString(), parsedApplicationProfile.toString());
	}

	public void testPossibleParsingClashes() throws JSONException {
		JSONObject expectedValue = new JSONObject("{\"hasLegend\": false,\"wmsQueryable\": false }");

		String applicationProfileStr = "AtlasMapper:{\"hasLegend\": false,\"wmsQueryable\": false }, DangerousApp:{\"message\":\"AtlasMapper: A mapping application.\"}";

		JSONObject parsedApplicationProfile = AbstractWMSLayerGenerator.parseApplicationProfile(applicationProfileStr);
		assertEquals(expectedValue.toString(), parsedApplicationProfile.toString());

		applicationProfileStr = "DangerousApp:{\"message\":\"AtlasMapper: A mapping application.\"}, AtlasMapper:{\"hasLegend\": false,\"wmsQueryable\": false }";

		parsedApplicationProfile = AbstractWMSLayerGenerator.parseApplicationProfile(applicationProfileStr);
		assertEquals(expectedValue.toString(), parsedApplicationProfile.toString());
	}

	/**
	 * This test try multiple values found on the Internet for "gmd:applicationProfile",
	 * mixed with the AtlasMapper configuration.
	 * @throws JSONException
	 */
	public void testMixedWithUnrelatedApplicationsParsing() throws JSONException {
		JSONObject expectedValue = new JSONObject("{\"hasLegend\": false,\"wmsQueryable\": false }");

		String applicationProfileStr = "Web Browser, AtlasMapper:{\"hasLegend\": false,\"wmsQueryable\": false }";
		JSONObject parsedApplicationProfile = AbstractWMSLayerGenerator.parseApplicationProfile(applicationProfileStr);
		assertEquals(expectedValue.toString(), parsedApplicationProfile.toString());

		applicationProfileStr = "AtlasMapper:{\"hasLegend\": false,\"wmsQueryable\": false }, Web Browser";
		parsedApplicationProfile = AbstractWMSLayerGenerator.parseApplicationProfile(applicationProfileStr);
		assertEquals(expectedValue.toString(), parsedApplicationProfile.toString());

		applicationProfileStr = "OGC:EOP-profile-0.3.3, AtlasMapper:{\"hasLegend\": false,\"wmsQueryable\": false }";
		parsedApplicationProfile = AbstractWMSLayerGenerator.parseApplicationProfile(applicationProfileStr);
		assertEquals(expectedValue.toString(), parsedApplicationProfile.toString());

		applicationProfileStr = "OGC:EOP-profile-0.3.3, AtlasMapper:{\"hasLegend\": false,\"wmsQueryable\": false }, OGC:EOP-profile-0.3.4";
		parsedApplicationProfile = AbstractWMSLayerGenerator.parseApplicationProfile(applicationProfileStr);
		assertEquals(expectedValue.toString(), parsedApplicationProfile.toString());

		applicationProfileStr = "Webpage, AtlasMapper:{\"hasLegend\": false,\"wmsQueryable\": false }";
		parsedApplicationProfile = AbstractWMSLayerGenerator.parseApplicationProfile(applicationProfileStr);
		assertEquals(expectedValue.toString(), parsedApplicationProfile.toString());

		applicationProfileStr = "BoreholeTemperature, AtlasMapper:{\"hasLegend\": false,\"wmsQueryable\": false }";
		parsedApplicationProfile = AbstractWMSLayerGenerator.parseApplicationProfile(applicationProfileStr);
		assertEquals(expectedValue.toString(), parsedApplicationProfile.toString());

		applicationProfileStr = "INSPIRE (EC) 976/2009, AtlasMapper:{\"hasLegend\": false,\"wmsQueryable\": false }";
		parsedApplicationProfile = AbstractWMSLayerGenerator.parseApplicationProfile(applicationProfileStr);
		assertEquals(expectedValue.toString(), parsedApplicationProfile.toString());

		applicationProfileStr = "DataFed.net, AtlasMapper:{\"hasLegend\": false,\"wmsQueryable\": false }";
		parsedApplicationProfile = AbstractWMSLayerGenerator.parseApplicationProfile(applicationProfileStr);
		assertEquals(expectedValue.toString(), parsedApplicationProfile.toString());

		applicationProfileStr = "string, AtlasMapper:{\"hasLegend\": false,\"wmsQueryable\": false }";
		parsedApplicationProfile = AbstractWMSLayerGenerator.parseApplicationProfile(applicationProfileStr);
		assertEquals(expectedValue.toString(), parsedApplicationProfile.toString());

		applicationProfileStr = "xml, AtlasMapper:{\"hasLegend\": false,\"wmsQueryable\": false }";
		parsedApplicationProfile = AbstractWMSLayerGenerator.parseApplicationProfile(applicationProfileStr);
		assertEquals(expectedValue.toString(), parsedApplicationProfile.toString());

		// Special char
		applicationProfileStr = "Special chars: /*-+`~!@#$%^&*()_+=\\|[]{};:'\",.<>?, AtlasMapper:{\"hasLegend\": false,\"wmsQueryable\": false }";
		parsedApplicationProfile = AbstractWMSLayerGenerator.parseApplicationProfile(applicationProfileStr);
		assertEquals(expectedValue.toString(), parsedApplicationProfile.toString());
	}

	public void testComplexApplicationsParsing() throws JSONException {
		String applicationProfileStr = "RelatedXMLApp:<applicationName>AtlasMapper</applicationName>" +
				"AtlasMapper: {" +
				"\"hasLegend\": false, " +
				"\"wmsQueryable\": false, " +
				"\"legendParameters\": { " +
					"\"FORMAT\": \"image/gif\"" +
				"}, " +
				"\"options\": [" +
					"{" +
						"\"name\": \"comment\", " +
						"\"title\": \"Comment\", " +
						"\"type\": \"textarea\"" +
					"}" +
				"]}, " +
				"Metadataviewer:{\"onlyShow\":false} " +
				"AnotherXMLApp:<tag>value</tag>";

		JSONObject parsedApplicationProfile = AbstractWMSLayerGenerator.parseApplicationProfile(applicationProfileStr);

		JSONObject expectedValue = new JSONObject("{" +
				"\"hasLegend\": false, " +
				"\"wmsQueryable\": false, " +
				"\"legendParameters\": { " +
					"\"FORMAT\": \"image/gif\"" +
				"}, " +
				"\"options\": [" +
					"{" +
						"\"name\": \"comment\", " +
						"\"title\": \"Comment\", " +
						"\"type\": \"textarea\"" +
					"}" +
				"]}");
		assertEquals(expectedValue.toString(), parsedApplicationProfile.toString());
	}

	public void testParsingNoWhiteSpaces() throws JSONException {
		String applicationProfileStr = "AtlasMapper:{\"hasLegend\":false,\"wmsQueryable\":false},Metadataviewer:{\"onlyShow\":true}";

		JSONObject parsedApplicationProfile = AbstractWMSLayerGenerator.parseApplicationProfile(applicationProfileStr);

		JSONObject expectedValue = new JSONObject("{\"hasLegend\": false,\"wmsQueryable\": false }");
		assertEquals(expectedValue.toString(), parsedApplicationProfile.toString());
	}

	public void testParsingWithExtraWhiteSpaces() throws JSONException {
		String applicationProfileStr = " 	 AtlasMapper:	 {   \"hasLegend\"	: false,   	\"wmsQueryable\":    false	 }	,\n		Metadataviewer:  {  \"onlyShow\" :  true  } 	\n	";

		JSONObject parsedApplicationProfile = AbstractWMSLayerGenerator.parseApplicationProfile(applicationProfileStr);

		JSONObject expectedValue = new JSONObject("{\"hasLegend\": false,\"wmsQueryable\": false }");
		assertEquals(expectedValue.toString(), parsedApplicationProfile.toString());
	}
}
