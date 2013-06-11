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

public class LayerOptionWrapper extends AbstractWrapper {
	public LayerOptionWrapper(JSONObject json) {
		super(json);
	}

	public String getName() {
		return this.json.optString("name", null);
	}
	public void setName(String name) throws JSONException {
		this.json.put("name", name);
	}

	public String getTitle() {
		return this.json.optString("title", null);
	}
	public void setTitle(String title) throws JSONException {
		this.json.put("title", title);
	}

	public String getType() {
		return this.json.optString("type", null);
	}
	public void setType(String type) throws JSONException {
		this.json.put("type", type);
	}

	public Boolean isMandatory() {
		return this.isMandatory(null);
	}
	public Boolean isMandatory(Boolean defaultValue) {
		if (this.json.isNull("mandatory")) {
			return defaultValue;
		}
		return this.json.optBoolean("mandatory");
	}
	public void setMandatory(Boolean mandatory) throws JSONException {
		if (mandatory == null && !this.json.isNull("mandatory")) {
			this.json.remove("mandatory");
		} else {
			this.json.put("mandatory", mandatory);
		}
	}

	public String getDefaultValue() {
		return this.json.optString("defaultValue", null);
	}
	public void setDefaultValue(String defaultValue) throws JSONException {
		this.json.put("defaultValue", defaultValue);
	}
}
