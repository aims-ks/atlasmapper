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
import au.gov.aims.atlasmapperserver.jsonWrappers.client.LayerWrapper;
import org.json.JSONException;
import org.json.JSONObject;

public class ArcGISCacheLayerConfig extends ArcGISMapServerLayerConfig {
	@ConfigField
	private Boolean forcePNG24;

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

	public Boolean isForcePNG24() {
		return this.forcePNG24;
	}

	public void setForcePNG24(Boolean forcePNG24) {
		this.forcePNG24 = forcePNG24;
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

	@Override
	public LayerWrapper generateLayer() throws JSONException {
		LayerWrapper layerWrapper = super.generateLayer();

		if (this.getTileCols() != null && this.getTileRows() != null) {
			layerWrapper.setArcGISCacheTileCols(this.getTileCols());
			layerWrapper.setArcGISCacheTileRows(this.getTileRows());
		}
		if (this.getTileOriginX() != null && this.getTileOriginY() != null) {
			layerWrapper.setArcGISCacheTileOriginX(this.getTileOriginX());
			layerWrapper.setArcGISCacheTileOriginY(this.getTileOriginY());
		}
		if (this.getTileResolutions() != null) {
			layerWrapper.setArcGISCacheTileResolutions(this.getTileResolutions());
		}

		return layerWrapper;
	}
}
