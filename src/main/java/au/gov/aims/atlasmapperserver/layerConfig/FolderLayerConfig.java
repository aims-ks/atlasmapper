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
import au.gov.aims.atlasmapperserver.annotation.ConfigField;

/**
 * This class represent a parent LayerGroup, in the tree defined by ArcGIS server. It's only distinction
 * with LayerGroup is it's representation on ArcGIS server.
 * The 2 classes are defined for future use (I.E. ability to display Folder in a different way than Groups)
 */
public class FolderLayerConfig extends AbstractLayerConfig {
	@ConfigField
	private String folderPath;

	// Layer group children
	@ConfigField
	private String[] layers;

	public FolderLayerConfig(ConfigManager configManager) {
		super(configManager);
	}

	public String getFolderPath() {
		return this.folderPath;
	}

	public void setFolderPath(String folderPath) {
		this.folderPath = folderPath;
	}

	public String[] getLayers() {
		return this.layers;
	}

	public void setLayers(String[] layers) {
		this.layers = layers;
	}
}
