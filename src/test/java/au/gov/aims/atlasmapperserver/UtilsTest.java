/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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

		String frenchStr = "Une chaîne de caractères.";
		assertEquals("556E6520636861C3AE6E6520646520636172616374C3A87265732E", Utils.toHex(frenchStr.getBytes("UTF-8")));

		String hangulStr = "한글";
		assertEquals("ED959CEAB880", Utils.toHex(hangulStr.getBytes("UTF-8")));
	}
}
