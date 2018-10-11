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

package au.gov.aims.atlasmapperserver.layerGenerator;

import java.util.List;
import java.util.logging.Logger;

import au.gov.aims.atlasmapperserver.ConfigManager;
import au.gov.aims.atlasmapperserver.dataSourceConfig.WMSDataSourceConfig;
import au.gov.aims.atlasmapperserver.layerConfig.LayerStyleConfig;
import au.gov.aims.atlasmapperserver.layerConfig.WMSLayerConfig;

/**
 *
 * @author glafond
 */
public class WMSLayerGenerator extends AbstractWMSLayerGenerator<WMSLayerConfig, WMSDataSourceConfig> {
	private static final Logger LOGGER = Logger.getLogger(WMSLayerGenerator.class.getName());

	@Override
	protected WMSLayerConfig createLayerConfig(ConfigManager configManager) {
		return new WMSLayerConfig(configManager);
	}

	/**
	 * Set default styles
	 * The default style is always the first one.
	 * @param configManager
	 * @param layer
	 */
	@Override
	protected void setDefaultLayerStyle(ConfigManager configManager, WMSLayerConfig layer) {
		List<LayerStyleConfig> styles = layer.getStyles();
		if (styles != null && !styles.isEmpty()) {
			styles.get(0).setDefault(true);
		}
	}
}
