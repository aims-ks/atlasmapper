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

package au.gov.aims.atlasmapperserver.dataSourceConfig;

import au.gov.aims.atlasmapperserver.AbstractConfig;
import au.gov.aims.atlasmapperserver.ConfigManager;
import au.gov.aims.atlasmapperserver.Utils;
import au.gov.aims.atlasmapperserver.annotation.ConfigField;
import au.gov.aims.atlasmapperserver.layerGenerator.AbstractLayerGenerator;
import au.gov.aims.atlasmapperserver.layerGenerator.XYZLayerGenerator;

import java.util.Set;

public class XYZDataSourceConfig extends AbstractDataSourceConfig {
	/**
	 * List of URLs used to request the tiles (load balancing)
	 */
	@ConfigField
	private String serviceUrls;
	// Cache - avoid parsing baseLayers string every times.
	private Set<String> serviceUrlsSet = null;

	@ConfigField
	private Boolean osm;

	@ConfigField
	private String crossOriginKeyword;

	public XYZDataSourceConfig(ConfigManager configManager) {
		super(configManager);
	}

	public String getServiceUrls() {
		return this.serviceUrls;
	}
	public Set<String> getServiceUrlsSet() {
		if (this.serviceUrlsSet == null && Utils.isNotBlank(this.serviceUrls)) {
			this.serviceUrlsSet = AbstractConfig.toSet(this.serviceUrls);
		}

		return this.serviceUrlsSet;
	}

	public void setServiceUrls(String serviceUrls) {
		this.serviceUrls = serviceUrls;
		this.serviceUrlsSet = null;
	}

	public Boolean isOsm() {
		return this.osm;
	}

	public void setOsm(Boolean osm) {
		this.osm = osm;
	}

	public String getCrossOriginKeyword() {
		return this.crossOriginKeyword;
	}

	public void setCrossOriginKeyword(String crossOriginKeyword) {
		this.crossOriginKeyword = crossOriginKeyword;
	}

	@Override
	public boolean isDefaultAllBaseLayers() {
		return true; // default: all base layers
	}

	@Override
	public AbstractLayerGenerator getLayerGenerator() {
		return new XYZLayerGenerator();
	}
}
