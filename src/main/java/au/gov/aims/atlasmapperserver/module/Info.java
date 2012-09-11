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

package au.gov.aims.atlasmapperserver.module;

import au.gov.aims.atlasmapperserver.ClientConfig;
import au.gov.aims.atlasmapperserver.annotation.Module;
import au.gov.aims.atlasmapperserver.layerConfig.LayerCatalog;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 *
 * @author glafond
 */
@Module(
	description="Display layers information is a separate panel."
)
public class Info extends AbstractModule {

	@Override
	// NOTE: The version must match the version in the client /clientResources/amc/modules/Info/Info.js
	public double getVersion() {
		return 1.0;
	}

	@Override
	public JSONObject getJSONConfiguration(ClientConfig clientConfig, LayerCatalog layerCatalog) throws JSONException {
		JSONObject layerTabConfig = new JSONObject();
		layerTabConfig.put("type", "description");
		layerTabConfig.put("startingTab", true);
		layerTabConfig.put("defaultContent", "There is no <b>layer information</b> for the selected layer.");

		JSONObject optionsTabConfig = new JSONObject();
		optionsTabConfig.put("type", "options");
		//optionsTabConfig.put("startingTab", true);
		optionsTabConfig.put("defaultContent", "The selected layer do not have any <b>options</b>.");

		JSONObject tabsConfig = new JSONObject();
		tabsConfig.put("Description", layerTabConfig);
		tabsConfig.put("Options", optionsTabConfig);

		return tabsConfig;
	}
}
