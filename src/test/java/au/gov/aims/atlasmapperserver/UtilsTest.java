/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2011 Australian Institute of Marine Science
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

package au.gov.aims.atlasmapperserver;

import java.io.UnsupportedEncodingException;
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
}
