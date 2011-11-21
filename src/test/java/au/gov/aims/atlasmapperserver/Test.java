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

import junit.framework.TestCase;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Gael Lafond <g.lafond@aims.org.au>
 */
public class Test extends TestCase {

	public void testJSONObject() throws JSONException {
		JSONObject json = new JSONObject();
		json.put("test1", "test1");
		json.put("emptyValue", "");
		json.put("nullValue", (String)null); // Allow but not kept
		json.put("", "emptyKey");
		//json.put(null, "nullKey"); // Not allow

		System.out.println("JSON:\n" + json.toString(4));
	}
}
