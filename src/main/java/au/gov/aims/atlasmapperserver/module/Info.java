/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.gov.aims.atlasmapperserver.module;

import au.gov.aims.atlasmapperserver.ClientConfig;
import au.gov.aims.atlasmapperserver.annotation.Module;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author glafond
 */
@Module(
	description="Display layers information is a separate panel."
)
public class Info extends AbstractModule {
	@Override
	public JSONObject getJSONConfiguration(ClientConfig clientConfig) throws JSONException {
		JSONObject layerTabConfig = new JSONObject();
		layerTabConfig.put("type", "description");
		layerTabConfig.put("startingTab", true);
		layerTabConfig.put("defaultContent", "There is no <b>layer information</b> for the selected layer.");

		JSONObject optionsTabConfig = new JSONObject();
		optionsTabConfig.put("type", "options");
		//optionsTabConfig.put("startingTab", true);
		optionsTabConfig.put("defaultContent", "The selected layer do not have any <b>options</b>.");

		JSONObject tabsConfig = new JSONObject();
		tabsConfig.put("Layer", layerTabConfig);
		tabsConfig.put("Options", optionsTabConfig);

		JSONObject config = new JSONObject();
		config.put("tabs", tabsConfig);

		return config;
	}
}
