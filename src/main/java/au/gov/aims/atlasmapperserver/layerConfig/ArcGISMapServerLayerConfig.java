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
import au.gov.aims.atlasmapperserver.annotation.ConfigField;
import au.gov.aims.atlasmapperserver.dataSourceConfig.ArcGISMapServerDataSourceConfigInterface;
import au.gov.aims.atlasmapperserver.jsonWrappers.client.LayerWrapper;
import org.json.JSONException;

public class ArcGISMapServerLayerConfig extends AbstractLayerConfig implements ArcGISMapServerDataSourceConfigInterface {
	@ConfigField
	private Boolean forcePNG24;

	@ConfigField
	private String arcGISPath;

	@ConfigField
	private String ignoredArcGISPath;

	public ArcGISMapServerLayerConfig(ConfigManager configManager) {
		super(configManager);
	}

	public Boolean isForcePNG24() {
		return this.forcePNG24;
	}

	public void setForcePNG24(Boolean forcePNG24) {
		this.forcePNG24 = forcePNG24;
	}

	@Override
	public String getIgnoredArcGISPath() {
		return this.ignoredArcGISPath;
	}

	@Override
	public void setIgnoredArcGISPath(String ignoredArcGISPath) {
		this.ignoredArcGISPath = ignoredArcGISPath;
	}

	@Override
	public LayerWrapper generateLayer() throws JSONException {
		LayerWrapper jsonLayer = super.generateLayer();

		if (this.isForcePNG24() != null) {
			jsonLayer.setForcePNG24(this.isForcePNG24());
		}

		if(Utils.isNotBlank(this.getArcGISPath())) {
			jsonLayer.setArcGISPath(this.getArcGISPath().trim());
		}

		return jsonLayer;
	}

	public String getArcGISPath() {
		return this.arcGISPath;
	}

	public void setArcGISPath(String arcGISPath) {
		this.arcGISPath = arcGISPath;
	}
}
