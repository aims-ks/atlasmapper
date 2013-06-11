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
package au.gov.aims.atlasmapperserver.jsonWrappers.server;

import au.gov.aims.atlasmapperserver.jsonWrappers.AbstractWrapper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class UsersConfigWrapper extends AbstractWrapper {
	public UsersConfigWrapper(JSONObject json) {
		super(json);
	}

	public Double getVersion() {
		return this.getVersion(null);
	}
	public Double getVersion(Double defaultValue) {
		if (this.json.isNull("version")) {
			return defaultValue;
		}
		return this.json.optDouble("version");
	}
	public void setVersion(Double version) throws JSONException {
		if (version == null && !this.json.isNull("version")) {
			this.json.remove("version");
		} else {
			this.json.put("version", version);
		}
	}

	public JSONArray getUsers() {
		return this.json.optJSONArray("users");
	}
	public void setUsers(JSONArray users) throws JSONException {
		this.json.put("users", users);
	}
}
