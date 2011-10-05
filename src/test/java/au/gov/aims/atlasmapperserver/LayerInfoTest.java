/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.gov.aims.atlasmapperserver;

import java.net.URL;
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

	public void _testToJsonObject() {
		try {
			LayerConfig layerInfo = new LayerConfig();

			double[] layerBoundingBox = {141.116, -29.384, 160.884, -9.616};

			JSONObject legendParameters = new JSONObject();
			legendParameters.put("FORMAT", "image/png");
			legendParameters.put("HEIGHT", "10");
			legendParameters.put("WIDTH", "20");

			layerInfo.setLayerId("ea:test");

			layerInfo.setTitle("Test layer title");
			layerInfo.setDescription("Test layer description");
			layerInfo.setDatasourceId("test-srv-id");
			layerInfo.setLayerBoundingBox(layerBoundingBox);
			layerInfo.setIsBaseLayer(true);
			layerInfo.setHasLegend(false);
			layerInfo.setLegendParameters(legendParameters);
			layerInfo.setWmsQueryable(true);

			JSONObject jsonObject = layerInfo.toJSonObject();

			// TODO Test the value of each of the jsonObject fields
			System.out.println(jsonObject.toString(4));
		} catch (JSONException ex) {
			fail("The layer can not be convert to a JSON Object:\n" + ex.getMessage());
			ex.printStackTrace();
		}
	}
}
