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

import au.gov.aims.atlasmapperserver.dataSourceConfig.AbstractDataSourceConfig;
import au.gov.aims.atlasmapperserver.layerConfig.AbstractLayerConfig;
import au.gov.aims.atlasmapperserver.layerConfig.LayerCatalog;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractLayerGenerator<L extends AbstractLayerConfig, D extends AbstractDataSourceConfig> {
	protected long instanceTimestamp = -1;

	private Map<String, LayerCatalog.LayerErrors> errors;

	public AbstractLayerGenerator(D dataSourceConfig) {
		this.errors = new HashMap<String, LayerCatalog.LayerErrors>();
	}

	protected abstract String getUniqueLayerId(L layer, D dataSourceConfig);
	public abstract Collection<L> generateLayerConfigs(D dataSourceConfig, boolean harvest) throws Exception;

	// Most data source type do not support caching. We don't want to override this everywhere.
	public Collection<L> generateCachedLayerConfigs(D dataSource, boolean harvest) throws Exception {
		return null;
	}

	// The layer name used to request the layer. Usually, the layerName is
	// the same as the layerId, so this field is let blank. This attribute
	// is only used when there is a duplication of layerId.
	protected void ensureUniqueLayerId(L layer, D dataSourceConfig) {
		String uniqueLayerId =
				dataSourceConfig.getDataSourceId() + "_" +
				this.getUniqueLayerId(layer, dataSourceConfig);

		layer.setLayerName(layer.getLayerId());
		layer.setLayerId(uniqueLayerId);
	}

	public Map<String, LayerCatalog.LayerErrors> getErrors() {
		return this.errors;
	}
	public void addError(String dataSourceId, String err) {
		this.getErrors(dataSourceId).addError(err);
	}
	public void addWarning(String dataSourceId, String warn) {
		this.getErrors(dataSourceId).addWarning(warn);
	}
	public void addMessage(String dataSourceId, String msg) {
		this.getErrors(dataSourceId).addMessage(msg);
	}
	private LayerCatalog.LayerErrors getErrors(String dataSourceId) {
		LayerCatalog.LayerErrors errors = this.errors.get(dataSourceId);
		if (errors == null) {
			errors = new LayerCatalog.LayerErrors();
			this.errors.put(dataSourceId, errors);
		}

		return errors;
	}

	public abstract D applyOverrides(D dataSourceConfig);
}
