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

import au.gov.aims.atlasmapperserver.AbstractConfig;
import au.gov.aims.atlasmapperserver.ConfigManager;
import au.gov.aims.atlasmapperserver.Utils;
import au.gov.aims.atlasmapperserver.annotation.ConfigField;

/**
 *
 * @author glafond
 */
public class LayerStyleConfig extends AbstractConfig implements Comparable<LayerStyleConfig> {
	@ConfigField
	private String name;

	@ConfigField
	private String title;

	@ConfigField
	private String description;

	@ConfigField(name="default", getter="isDefault", setter="setDefault")
	private Boolean _default;

	@ConfigField
	private Boolean cached;

	public LayerStyleConfig(ConfigManager configManager) {
		super(configManager);
	}

	@Override
	public void setJSONObjectKey(String key) {
		if (Utils.isBlank(this.name)) {
			this.name = key;
		}
	}

	@Override
	public String getJSONObjectKey() {
		return this.name;
	}


	public Boolean isDefault() {
		return _default;
	}

	public void setDefault(Boolean _default) {
		this._default = _default;
	}

	public Boolean isCached() {
		return this.cached;
	}

	public void setCached(Boolean cached) {
		this.cached = cached;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@Override
	// Order by title, name
	// Null at the end
	public int compareTo(LayerStyleConfig o) {
		if (this == o) {
			return 0;
		}
		// Null at the end
		if (o == null) {
			return 1;
		}

		int cmp = this.title == null ? (o.title == null ? 0 : -1) : this.title.compareTo(o.title);
		if (cmp == 0) {
			cmp = this.name == null ? (o.name == null ? 0 : -1) : this.name.compareTo(o.name);
		}

		return cmp;
	}

	@Override
	public String toString() {
		return "LayerStyleConfig{\n" +
				(Utils.isBlank(name) ? "" :        "	name=" + name + "\n") +
				(Utils.isBlank(title) ? "" :       "	title=" + title + "\n") +
				(Utils.isBlank(description) ? "" : "	description=" + description + "\n") +
				(_default == null ? "" :           "	default=" + _default + "\n") +
				(cached == null ? "" :             "	cachable=" + cached + "\n") +
			'}';
	}
}
