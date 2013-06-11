/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2012 Australian Institute of Marine Science
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
package au.gov.aims.atlasmapperserver.jsonWrappers.client;

import au.gov.aims.atlasmapperserver.jsonWrappers.AbstractWrapper;
import org.json.JSONException;
import org.json.JSONObject;

public class LayerStyleWrapper extends AbstractWrapper {
	public LayerStyleWrapper(JSONObject json) {
		super(json);
	}

	public String getTitle() {
		return this.json.optString("title", null);
	}
	public void setTitle(String title) throws JSONException {
		this.json.put("title", title);
	}

	public Boolean isDefault() {
		return this.isDefault(null);
	}
	public Boolean isDefault(Boolean defaultValue) {
		if (this.json.isNull("default")) {
			return defaultValue;
		}
		return this.json.optBoolean("default");
	}
	public void setDefault(Boolean _default) throws JSONException {
		if (_default == null && !this.json.isNull("default")) {
			this.json.remove("default");
		} else {
			this.json.put("default", _default);
		}
	}

	public Boolean isCached() {
		return this.isCached(null);
	}
	public Boolean isCached(Boolean defaultValue) {
		if (this.json.isNull("cached")) {
			return defaultValue;
		}
		return this.json.optBoolean("cached");
	}
	public void setCached(Boolean cached) throws JSONException {
		if (cached == null && !this.json.isNull("cached")) {
			this.json.remove("cached");
		} else {
			this.json.put("cached", cached);
		}
	}

	public String getDescription() {
		return this.json.optString("description", null);
	}
	public void setDescription(String description) throws JSONException {
		this.json.put("description", description);
	}


	public String getLegendUrl() {
		return this.json.optString("legendUrl", null);
	}
	public void setLegendUrl(String legendUrl) throws JSONException {
		this.json.put("legendUrl", legendUrl);
	}

	public String getLegendFilename() {
		return this.json.optString("legendFilename", null);
	}
	public void setLegendFilename(String legendFilename) throws JSONException {
		this.json.put("legendFilename", legendFilename);
	}
}
