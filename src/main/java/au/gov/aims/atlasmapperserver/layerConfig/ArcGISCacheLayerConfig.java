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

public class ArcGISCacheLayerConfig extends ArcGISMapServerLayerConfig {
	@ConfigField
	private Integer tileCols;
	@ConfigField
	private Integer tileRows;

	@ConfigField
	private Double tileOriginX;
	@ConfigField
	private Double tileOriginY;

	@ConfigField
	private Double[] tileResolutions;

	public ArcGISCacheLayerConfig(ConfigManager configManager) {
		super(configManager);
	}

	public Integer getTileCols() {
		return tileCols;
	}

	public void setTileCols(Integer tileCols) {
		this.tileCols = tileCols;
	}

	public Integer getTileRows() {
		return tileRows;
	}

	public void setTileRows(Integer tileRows) {
		this.tileRows = tileRows;
	}

	public Double getTileOriginX() {
		return tileOriginX;
	}

	public void setTileOriginX(Double tileOriginX) {
		this.tileOriginX = tileOriginX;
	}

	public Double getTileOriginY() {
		return tileOriginY;
	}

	public void setTileOriginY(Double tileOriginY) {
		this.tileOriginY = tileOriginY;
	}

	public Double[] getTileResolutions() {
		return tileResolutions;
	}

	public void setTileResolutions(Double[] tileResolutions) {
		this.tileResolutions = tileResolutions;
	}
}
