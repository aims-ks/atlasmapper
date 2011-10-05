/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.gov.aims.atlasmapperserver.module;

import au.gov.aims.atlasmapperserver.ClientConfig;
import au.gov.aims.atlasmapperserver.ConfigManager;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author glafond
 */
public abstract class AbstractModule {
	public abstract JSONObject getJSONConfiguration(ConfigManager configManager, ClientConfig clientConfig) throws JSONException;
}
