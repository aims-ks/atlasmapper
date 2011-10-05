/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.gov.aims.atlasmapperserver;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import junit.framework.TestCase;

/**
 *
 * @author glafond
 */
public class ConfigHelperTest extends TestCase {

	/**
	 * The purpose of this test is to ensure the JSon reader / writer
	 * do not alter the data.
	 *
	 * It save a config to a String, than load it in an other object.
	 * Than, compare both objects.
	 */
	public void testSaveAndReload() {
		//GlobalConfig config = new GlobalConfig();
		// TODO
	}

	/**
	 * -- TODO Rewrite --
	 * The purpose of this test is to ensure the JSon reader / writer
	 * do not alter the data.
	 *
	 * It load a config, from a String, than save it in an other String.
	 * Than, compare both string.
	 *
	 * This test is realy unstable since the HashMap can give elements
	 * in different order, producing a different output string, but
	 * still the same config (false negative).
	 *
	 * It should create a config using objects, save it, than reload it, than test it.
	 */
	public void testReloadAndSave() {
		String configStr =
				"{"+
					"\"baseLayers\":\""+
						"World_NE2-coast-cities-reefs_Baselayer,"+
						"ea:World_NED_NE2,"+
						"ea:Natural_Earth_2,"+
						"ea:GBRMPA_reefs-gbr_features-coast,"+
						"GBR_JCU_Bathymetry-3DGBR_Land-and-sea,"+
						"nasa:nasa-world-blue_marble_next_generation-2005-dec_nb-d1v1m1"+
					"\","+
					"\"datasources\":["+
						"{"+
							"\"id\":\"1\","+
							"\"serverUrls\":\"http://aaa.com\","+
							"\"datasourceType\":\"WMS\","+
							"\"enable\":true,"+
							"\"datasourceId\":\"test\","+
							"\"datasourceName\":\"Test\","+
							"\"showInLegend\":false"+
						"},{"+
							"\"id\":\"2\","+
							"\"serverUrls\":\"http://aaa2.com\","+
							"\"datasourceType\":\"NCWMS\","+
							"\"enable\":true,"+
							"\"datasourceId\":\"test2\","+
							"\"datasourceName\":\"Test2\","+
							"\"showInLegend\":false"+
						"}"+
					"],"+
					"\"manualOverride\":["+
						"{"+
							"\"layerOptions\":["+
								"{"+
									"\"title\":\"Color ranges (min,max)\","+
									"\"name\":\"COLORSCALERANGE\","+
									"\"type\":\"textfield\""+
								"},{"+
									"\"title\":\"Date\","+
									"\"name\":\"TIME\","+
									"\"type\":\"ux-ncdatetimefield\""+
								"}"+
							"],"+
							"\"layerId\":\"gbr4-28psu/exposure\""+
						"}"+
					"],"+
					"\"clients\":["+
						"{"+
							"\"id\":\"1\","+
							"\"useLayerService\":false,"+
							"\"baseLayersInTab\":true,"+
							"\"zoom\":\"6\","+
							"\"projection\":\"EPSG:4326\","+
							"\"clientName\":\"Test\","+
							"\"enable\":true,"+
							"\"longitude\":\"148.0\","+
							"\"latitude\":\"-18.0\","+
							"\"generatedFileLocation\":\"/home/blabla/www\","+
							"\"version\":\"1.0\""+
						"},{"+
							"\"id\":\"2\","+
							"\"useLayerService\":false,"+
							"\"baseLayersInTab\":true,"+
							"\"zoom\":\"6\","+
							"\"projection\":\"EPSG:4326\","+
							"\"clientName\":\"Test\","+
							"\"enable\":true,"+
							"\"longitude\":\"148.0\","+
							"\"latitude\":\"-18.0\","+
							"\"generatedFileLocation\":\"/home/blabla/www\","+
							"\"version\":\"1.0\""+
						"}"+
					"]"+
				"}";

		Reader stringReader = new StringReader(configStr);

		StringWriter stringWriter = new StringWriter();

		try {
			//ConfigHelper.reload(stringReader);
			//ConfigHelper.save(stringWriter);
		} catch (Exception ex) {
			ex.printStackTrace();
			fail(ex.getMessage());
		} finally {
			try { stringReader.close(); } catch (Exception e) {}
			try { stringWriter.close(); } catch (Exception e) {}
		}

		String resultStr = stringWriter.toString();

		if (!configStr.equals(resultStr)) {
			System.out.println("NOT!!!\n" + resultStr);
		}
	}
}
