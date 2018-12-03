/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2011 Australian Institute of Marine Science
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

package au.gov.aims.atlasmapperserver;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.SortedSet;

import junit.framework.TestCase;

/**
 *
 * @author glafond
 */
public class UtilsTest extends TestCase {
	public void testToHexWithBytes() throws UnsupportedEncodingException {
		byte[] firstBytes = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17};
		assertEquals("000102030405060708090A0B0C0D0E0F1011", Utils.toHex(firstBytes));

		byte[] lastBytes = {-17, -16, -15, -14, -13, -12, -11, -10, -9, -8, -7, -6, -5, -4, -3, -2, -1};
		assertEquals("EFF0F1F2F3F4F5F6F7F8F9FAFBFCFDFEFF", Utils.toHex(lastBytes));

		byte[] choosenBytes = {0, 10, 20, 30, 50, 100, 127, -128, -100, -50, -30, -20, -10, -1};
		assertEquals("000A141E32647F809CCEE2ECF6FF", Utils.toHex(choosenBytes));
	}

	public void testToHexWithString() throws UnsupportedEncodingException {
		String englishStr = "A String.";
		assertEquals("4120537472696E672E", Utils.toHex(englishStr.getBytes("UTF-8")));

		// Non ASCII characters has been represent as Unicode caracters to be
		// sure it will be interpreted correctly in different platforms.
		String frenchStr = "Une cha\u00EEne de caract\u00E8res."; // 00EE => i circ (C3AE in hexa), 00E8 => e grave (C3A8 in hexa)
		assertEquals("556E6520636861C3AE6E6520646520636172616374C3A87265732E", Utils.toHex(frenchStr.getBytes("UTF-8")));
	}

	public void testAddUrlParameter() throws UnsupportedEncodingException {
		String newUrl = Utils.addUrlParameter("http://www.google.com/index.jsp", "param", "newValue");
		assertEquals("http://www.google.com/index.jsp?param=newValue", newUrl);

		newUrl = Utils.addUrlParameter("http://www.google.com/index.jsp", "param{&?=;.,é}", "newValue{&?=;.,é}");
		assertEquals("http://www.google.com/index.jsp?param%7B%26%3F%3D%3B.%2C%C3%A9%7D=newValue%7B%26%3F%3D%3B.%2C%C3%A9%7D", newUrl);

		newUrl = Utils.addUrlParameter("http://www.google.com/index.jsp?param=value", "newParam", "newValue");
		assertEquals("http://www.google.com/index.jsp?param=value&newParam=newValue", newUrl);

		newUrl = Utils.addUrlParameter("http://www.google.com/index.jsp?param=value", "param", "newValue");
		assertEquals("http://www.google.com/index.jsp?param=value&param=newValue", newUrl);
	}

	public void testSetUrlParameter() throws UnsupportedEncodingException {
		String newUrl = Utils.setUrlParameter("http://www.google.com/index.jsp?param=value", "param", "newValue");
		assertEquals("http://www.google.com/index.jsp?param=newValue", newUrl);

		newUrl = Utils.setUrlParameter("http://www.google.com/index.jsp?param%7B%26%3F%3D%3B.%2C%C3%A9%7D=value", "param{&?=;.,é}", "newValue{&?=;.,é}");
		assertEquals("http://www.google.com/index.jsp?param%7B%26%3F%3D%3B.%2C%C3%A9%7D=newValue%7B%26%3F%3D%3B.%2C%C3%A9%7D", newUrl);

		newUrl = Utils.setUrlParameter("http://www.google.com/index.jsp?prefixparam=value&param=value", "param", "newValue");
		assertEquals("http://www.google.com/index.jsp?prefixparam=value&param=newValue", newUrl);

		newUrl = Utils.setUrlParameter("http://www.google.com/index.jsp?param1=value1&param2=value2&param3=value3", "param2", "newValue2");
		assertEquals("http://www.google.com/index.jsp?param1=value1&param2=newValue2&param3=value3", newUrl);

		newUrl = Utils.setUrlParameter("http://www.google.com/index.jsp?param=value", "newParam", "newValue");
		assertEquals("http://www.google.com/index.jsp?param=value&newParam=newValue", newUrl);
	}

	public void testRemoveUrlParameter() throws UnsupportedEncodingException {
		String newUrl = Utils.removeUrlParameter("http://www.google.com/index.jsp?param=value&oldParam=oldValue", "oldParam");
		assertEquals("http://www.google.com/index.jsp?param=value", newUrl);

		newUrl = Utils.removeUrlParameter("http://www.google.com/index.jsp?param%7B%26%3F%3D%3B.%2C%C3%A9%7D=value", "param{&?=;.,é}");
		assertEquals("http://www.google.com/index.jsp", newUrl);

		newUrl = Utils.removeUrlParameter("http://www.google.com/index.jsp?param=value&oldParam=oldValue", "param");
		assertEquals("http://www.google.com/index.jsp?oldParam=oldValue", newUrl);

		newUrl = Utils.removeUrlParameter("http://www.google.com/index.jsp?prefixparam=value&param=value&oldParam=oldValue", "param");
		assertEquals("http://www.google.com/index.jsp?prefixparam=value&oldParam=oldValue", newUrl);

		newUrl = Utils.removeUrlParameter("http://www.google.com/index.jsp?param1=value1&param2=value2&param3=value3", "param2");
		assertEquals("http://www.google.com/index.jsp?param1=value1&param3=value3", newUrl);

		newUrl = Utils.removeUrlParameter("http://www.google.com/index.jsp?param=value", "param");
		assertEquals("http://www.google.com/index.jsp", newUrl);
	}

	/*
	public void testBenchmarkSetUrlParameter2() throws UnsupportedEncodingException {
		Date before = new java.util.Date();

		for (int i=0; i<10000; i++) {
			String newUrl = Utils._setUrlParameter("http://www.google.com/index.jsp?param=value", "param", "newValue"+i);
		}
		Date after = new java.util.Date();

		System.out.println("Time for Loop: " + (after.getTime() - before.getTime()));
	}

	public void testBenchmarkSetUrlParameter() throws UnsupportedEncodingException {
		Date before = new java.util.Date();

		for (int i=0; i<10000; i++) {
			String newUrl = Utils.setUrlParameter("http://www.google.com/index.jsp?param=value", "param", "newValue"+i);
		}
		Date after = new java.util.Date();

		System.out.println("Time for Regex: " + (after.getTime() - before.getTime()));
	}
	*/

	public void testToURL() throws Exception {
		// Valid URL that should not change
		String validUrl1 = "http://domain.com/?val1=a%20space";
		String validUrl2 = "http://domain.com:8080/?val2=a+space#anchor";

		assertEquals(validUrl1, Utils.toURL(validUrl1).toString());
		assertEquals(validUrl2, Utils.toURL(validUrl2).toString());

		// Invalid URL that the browser support
		String invalidUrl1 = "http://domain.com/?val3=a space"; // Space
		String invalidUrl1Corrected = "http://domain.com/?val3=a%20space";
		assertEquals(invalidUrl1Corrected, Utils.toURL(invalidUrl1).toString());

		String invalidUrl2 = "http://domain.com/?val4=a	tab"; // tab
		String invalidUrl2Corrected = "http://domain.com/?val4=a%09tab"; // Encoded
		assertEquals(invalidUrl2Corrected, Utils.toURL(invalidUrl2).toString());

		String invalidUrl3 = "http://domain.com/?val5=Gaël"; // UTF-8 char
		String invalidUrl3Corrected = "http://domain.com/?val5=Ga%C3%ABl"; // Encoded
		assertEquals(invalidUrl3Corrected, Utils.toURL(invalidUrl3).toString());

		// Invalid URL found in IMOS capabilities doc
		String imosUrl = "http://imosmest.aodn.org.au/geonetwork/srv/en/iso19139.xml?uuid= 73089abf-9880-47f7-b6f7-5659522394ad";
		String imosUrlCorrected = "http://imosmest.aodn.org.au/geonetwork/srv/en/iso19139.xml?uuid=%2073089abf-9880-47f7-b6f7-5659522394ad";
		assertEquals(imosUrlCorrected, Utils.toURL(imosUrl).toString());
	}

	public void testHighlightResults() {
		SortedSet<Utils.Occurrence> positions;
		String str, newStr;

		str = "Hello World";
		positions = Utils.findOccurrences(str, new String[]{"missing"});
		assertEquals(0, positions.size());
		newStr = Utils.getHighlightChunk(positions, str, 0);
		assertEquals("Hello World", newStr);

		str = "Hello World";
		positions = Utils.findOccurrences(str, new String[]{"orl"});
		assertEquals(1, positions.size());
		newStr = Utils.getHighlightChunk(positions, str, 0);
		assertEquals("Hello W"+Utils.HIGHLIGHT_OPEN_TAG+"orl"+Utils.HIGHLIGHT_CLOSE_TAG+"d", newStr);

		str = "Hello World";
		positions = Utils.findOccurrences(str, new String[]{"hell"});
		assertEquals(1, positions.size());
		newStr = Utils.getHighlightChunk(positions, str, 0);
		assertEquals(Utils.HIGHLIGHT_OPEN_TAG+"Hell"+Utils.HIGHLIGHT_CLOSE_TAG+"o World", newStr);

		str = "\"Hello\" World";
		positions = Utils.findOccurrences(str, new String[]{"hell"});
		assertEquals(1, positions.size());
		newStr = Utils.getHighlightChunk(positions, str, 0);
		assertEquals("\""+Utils.HIGHLIGHT_OPEN_TAG+"Hell"+Utils.HIGHLIGHT_CLOSE_TAG+"o\" World", newStr);

		str = "Hello \"World\"";
		positions = Utils.findOccurrences(str, new String[]{"world"});
		assertEquals(1, positions.size());
		newStr = Utils.getHighlightChunk(positions, str, 0);
		assertEquals("Hello \""+Utils.HIGHLIGHT_OPEN_TAG+"World"+Utils.HIGHLIGHT_CLOSE_TAG+"\"", newStr);

		// Multiple results
		str = "Multiple results of the word \"result\" for a search for result and for.";
		positions = Utils.findOccurrences(str, new String[]{"result", "for"});
		assertEquals(6, positions.size());
		newStr = Utils.getHighlightChunk(positions, str, 0);
		assertEquals("Multiple "+Utils.HIGHLIGHT_OPEN_TAG+"result"+Utils.HIGHLIGHT_CLOSE_TAG+"s of the word \""+
				Utils.HIGHLIGHT_OPEN_TAG+"result"+Utils.HIGHLIGHT_CLOSE_TAG+"\" "+Utils.HIGHLIGHT_OPEN_TAG+"for"+
				Utils.HIGHLIGHT_CLOSE_TAG+" a search "+Utils.HIGHLIGHT_OPEN_TAG+"for"+Utils.HIGHLIGHT_CLOSE_TAG+" "+
				Utils.HIGHLIGHT_OPEN_TAG+"result"+Utils.HIGHLIGHT_CLOSE_TAG+" and "+Utils.HIGHLIGHT_OPEN_TAG+"for"+
				Utils.HIGHLIGHT_CLOSE_TAG+".", newStr);

		// Overlapping results
		str = "Hello World";
		positions = Utils.findOccurrences(str, new String[]{"hell", "llo"});
		assertEquals(2, positions.size());
		newStr = Utils.getHighlightChunk(positions, str, 0);
		assertEquals(Utils.HIGHLIGHT_OPEN_TAG+"Hello"+Utils.HIGHLIGHT_CLOSE_TAG+" World", newStr);
	}

	public void testChopAndHighlightResults() {
		SortedSet<Utils.Occurrence> positions;
		String str, newStr;

		// No result
		str = "Hello World";
		positions = Utils.findOccurrences(str, new String[]{"missing"});
		assertEquals(0, positions.size());
		newStr = Utils.getHighlightChunk(positions, str, 10);
		assertEquals("Hello W...", newStr);

		// Chopped highlighted result
		str = "Hello World";
		positions = Utils.findOccurrences(str, new String[]{"world"});
		assertEquals(1, positions.size());
		newStr = Utils.getHighlightChunk(positions, str, 10);
		assertEquals("Hello "+Utils.HIGHLIGHT_OPEN_TAG+"W"+Utils.HIGHLIGHT_CLOSE_TAG+"...", newStr);

		// Hidden result
		str = "Hello World";
		positions = Utils.findOccurrences(str, new String[]{"rld"});
		assertEquals(1, positions.size());
		newStr = Utils.getHighlightChunk(positions, str, 10);
		assertEquals("Hello W...", newStr);
	}
}
