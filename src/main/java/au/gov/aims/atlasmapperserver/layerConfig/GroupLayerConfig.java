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

package au.gov.aims.atlasmapperserver.layerConfig;

import au.gov.aims.atlasmapperserver.ConfigManager;
import au.gov.aims.atlasmapperserver.annotation.ConfigField;

public class GroupLayerConfig extends AbstractLayerConfig {
	@ConfigField
	private String groupPath;

	// Layer group children
	@ConfigField
	private String[] layers;

	// Used with ArcGIS
	@ConfigField
	private Boolean singleFusedMapCache;

	public GroupLayerConfig(ConfigManager configManager) {
		super(configManager);
	}

	public String getGroupPath() {
		return this.groupPath;
	}

	public void setGroupPath(String groupPath) {
		this.groupPath = groupPath;
	}

	public String[] getLayers() {
		return this.layers;
	}

	public void setLayers(String[] layers) {
		this.layers = layers;
	}

	public Boolean isSingleFusedMapCache() {
		return this.singleFusedMapCache;
	}

	public void setSingleFusedMapCache(Boolean singleFusedMapCache) {
		this.singleFusedMapCache = singleFusedMapCache;
	}
}
