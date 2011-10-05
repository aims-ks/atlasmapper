/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.gov.aims.atlasmapperserver;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author glafond
 */
public class GoogleLayers {
	// The layers do not changes often enough to develop some sort of parser.
	private static Map<String, LayerConfig> googleLayersCache = null;

	/**
	 * Create and return the 4 google layers.
	 *     * Google Physical
	 *     * Google Streets
	 *     * Google Hybrid
	 *     * Google Satellite
	 * @param clientConfig
	 * @return
	 */
	public static synchronized Map<String, LayerConfig> getGoogleLayerConfigs(ClientConfig clientConfig, DatasourceConfig datasourceConfig) {
		if (googleLayersCache == null) {
			googleLayersCache = new HashMap<String, LayerConfig>();

			googleLayersCache.put("TERRAIN",
					createGoogleLayer(datasourceConfig, "TERRAIN", "Google Physical", null));

			googleLayersCache.put("ROADMAP",
					createGoogleLayer(datasourceConfig, "ROADMAP", "Google Streets", null));

			googleLayersCache.put("HYBRID",
					createGoogleLayer(datasourceConfig, "HYBRID", "Google Hybrid", null));

			googleLayersCache.put("SATELLITE",
					createGoogleLayer(datasourceConfig, "SATELLITE", "Google Satellite", null));
			/*
			var gphy = new OpenLayers.Layer.Google(
				"Google Physical",
				{type: google.maps.MapTypeId.TERRAIN}
			);
			var gmap = new OpenLayers.Layer.Google(
				"Google Streets", // the default
				// TYPE: google.maps.MapTypeId.ROADMAP
				{numZoomLevels: 20}
			);
			var ghyb = new OpenLayers.Layer.Google(
				"Google Hybrid",
				{type: google.maps.MapTypeId.HYBRID, numZoomLevels: 20}
			);
			var gsat = new OpenLayers.Layer.Google(
				"Google Satellite",
				{type: google.maps.MapTypeId.SATELLITE, numZoomLevels: 22}
			);
			*/
		}
		return googleLayersCache;
	}

	private static LayerConfig createGoogleLayer(DatasourceConfig datasourceConfig, String googleLayerType, String name, String description) {
		// Create a LayerConfig based on its datasource
		LayerConfig layerConfig = new LayerConfig(datasourceConfig);

		layerConfig.setLayerId(googleLayerType);
		layerConfig.setTitle(name);
		layerConfig.setDescription(description);
		layerConfig.setIsBaseLayer(true);
		layerConfig.setLayerBoundingBox(new double[]{-180, -90, 180, 90});

		return layerConfig;
	}
}
