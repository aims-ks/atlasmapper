/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2012 Australian Institute of Marine Science
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
package au.gov.aims.atlasmapperserver.jsonWrappers.server;

import au.gov.aims.atlasmapperserver.jsonWrappers.AbstractWrapper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ServerConfigWrapper extends AbstractWrapper {
	public ServerConfigWrapper() { super(); }
	public ServerConfigWrapper(JSONObject json) { super(json); }

	public Boolean isDemoMode() {
		return this.isDemoMode(null);
	}
	public Boolean isDemoMode(Boolean defaultValue) {
		if (this.json.isNull("demoMode")) {
			return defaultValue;
		}
		return this.json.optBoolean("demoMode");
	}
	public void setDemoMode(Boolean demoMode) throws JSONException {
		if (demoMode == null && !this.json.isNull("demoMode")) {
			this.json.remove("demoMode");
		} else {
			this.json.put("demoMode", demoMode);
		}
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

	public JSONArray getDataSources() {
		return this.json.optJSONArray("dataSources");
	}
	public void setDataSources(JSONArray dataSources) throws JSONException {
		this.json.put("dataSources", dataSources);
	}

	public JSONArray getClients() {
		return this.json.optJSONArray("clients");
	}
	public void setClients(JSONArray clients) throws JSONException {
		this.json.put("clients", clients);
	}

}
