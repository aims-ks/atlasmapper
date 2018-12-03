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
package au.gov.aims.atlasmapperserver.jsonWrappers.client;

import au.gov.aims.atlasmapperserver.jsonWrappers.AbstractWrapper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class URLSaveState extends AbstractWrapper {
	public URLSaveState() { super(); }
	public URLSaveState(JSONObject json) { super(json); }

	public JSONArray getLayers() {
		return this.json.optJSONArray("layers");
	}
	public void setLayers(JSONArray layers) throws JSONException {
		this.json.put("layers", layers);
	}

	public JSONArray getBounds() {
		return this.json.optJSONArray("bounds");
	}
	public void setBounds(JSONArray bounds) throws JSONException {
		this.json.put("bounds", bounds);
	}

}
