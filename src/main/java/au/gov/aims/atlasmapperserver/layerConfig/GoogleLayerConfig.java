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

package au.gov.aims.atlasmapperserver.layerConfig;

import au.gov.aims.atlasmapperserver.ConfigManager;
import au.gov.aims.atlasmapperserver.Utils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.logging.Level;
import java.util.logging.Logger;

public class GoogleLayerConfig extends AbstractLayerConfig {
	private static final Logger LOGGER = Logger.getLogger(GoogleLayerConfig.class.getName());
	private static final String NUM_ZOOM_LEVELS_KEY = "numZoomLevels";

	public GoogleLayerConfig(ConfigManager configManager) {
		super(configManager);
	}

	public void setNumZoomLevels(Integer numZoomLevels) {
		JSONObject olOptions = this.getOlOptions();
		if (olOptions == null) {
			olOptions = new JSONObject();
			this.setOlOptions(olOptions);
		}
		if (numZoomLevels == null) {
			if (olOptions.has(NUM_ZOOM_LEVELS_KEY)) {
				olOptions.remove(NUM_ZOOM_LEVELS_KEY);
			}
		} else {
			try {
				olOptions.put(NUM_ZOOM_LEVELS_KEY, numZoomLevels);
			} catch(JSONException ex) {
				// This will probably never happen...
				LOGGER.log(Level.SEVERE, "Can not set the {0} properties for a google layer: {1}",
						new String[] { NUM_ZOOM_LEVELS_KEY, Utils.getExceptionMessage(ex) });
				LOGGER.log(Level.FINE, "Stack trace: ", ex);
			}
		}
	}

	public Integer getNumZoomLevels() {
		JSONObject olOptions = this.getOlOptions();
		if (olOptions == null && olOptions.has(NUM_ZOOM_LEVELS_KEY)) {
			return olOptions.optInt(NUM_ZOOM_LEVELS_KEY);
		}
		return null;
	}
}
