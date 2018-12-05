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

import java.net.URL;

import au.gov.aims.atlasmapperserver.layerConfig.WMSLayerConfig;
import junit.framework.TestCase;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author glafond
 */
public class LayerInfoTest extends TestCase {
	public void test() {
		URL url = null;

		url = this.getClass().getResource(".");
		System.out.println(". ["+url+"]");

		url = this.getClass().getResource("/");
		System.out.println("/ ["+url+"]");

		url = this.getClass().getResource("/amc");
		System.out.println("/amc ["+url+"]");
	}

	public void testToJsonObject() {
		try {
			WMSLayerConfig layerInfo = new WMSLayerConfig(null);

			double[] layerBoundingBox = {141.116, -29.384, 160.884, -9.616};

			StringBuilder legendParameters = new StringBuilder();
			legendParameters.append("FORMAT=image/png\n");
			legendParameters.append("HEIGHT=10\n");
			legendParameters.append("WIDTH=20");

			layerInfo.setLayerId("ea:test");

			layerInfo.setTitle("Test layer title");
			layerInfo.setDescription("Test layer description");
			//layerInfo.setDataSourceId("test-srv-id");
			layerInfo.setLayerBoundingBox(layerBoundingBox);
			layerInfo.setIsBaseLayer(true);
			layerInfo.setHasLegend(false);
			//layerInfo.setLegendParameters(legendParameters.toString());
			layerInfo.setWmsQueryable(true);

			JSONObject jsonObject = layerInfo.toJSonObject();

			// TODO Test the value of each of the jsonObject fields
			System.out.println(jsonObject.toString(4));
		} catch (JSONException ex) {
			fail("The layer can not be convert to a JSON Object:\n" + Utils.getExceptionMessage(ex));
			ex.printStackTrace();
		}
	}
}
