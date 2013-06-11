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
package au.gov.aims.atlasmapperserver.jsonWrappers;

import au.gov.aims.atlasmapperserver.Utils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AbstractWrapper implements Cloneable {
	private static final Logger LOGGER = Logger.getLogger(AbstractWrapper.class.getName());

	protected JSONObject json;

	public AbstractWrapper(JSONObject json) {
		this.json = json;
	}

	public JSONObject getJSON() {
		return this.json;
	}

	protected void setValue(String key, Object value) throws JSONException {
		if (value == null) {
			this.json.remove(key);
		} else {
			this.json.put(key, value);
		}
	}

	public Object clone() {
		JSONObject jsonClone = new JSONObject();

		try {
			Iterator<String> keys = this.json.keys();
			if (keys != null) {
				while (keys.hasNext()) {
					String key = keys.next();
					jsonClone.put(key, this.json.opt(key));
				}
			}

			return this.getClass().getConstructor(JSONObject.class).newInstance(jsonClone);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Can not clone Object of class {0}: {1}",
					new String[] { this.getClass().getName(), Utils.getExceptionMessage(e) });
			LOGGER.log(Level.FINE, "Stack trace: ", e);
		}
		return null;
	}
}
