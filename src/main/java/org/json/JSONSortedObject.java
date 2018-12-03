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

package org.json;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Exact same class as JSONObject, but using a TreeMap (Sorted) instead of a HashMap.
 * Instead of copying the code of all constructor, there is a constructor that take
 * a JSONObject and copy it's map.
 *
 * NOTE: This class handle a JSON Object which has sorted keys. This can be useful
 *     to print readable JSON, but there is no guaranty that the browser will keep
 *     the order, so it can not be used to sort elements for the client (browser).
 *     If the order is important, a JSONArray may be a more appropriate element.
 */
public class JSONSortedObject extends JSONObject {
	private boolean recursiveSort = true;

	public JSONSortedObject() {
		Comparator<Object> ignoreCaseComparator = new Comparator<Object>() {
			@Override
			public int compare(Object o1, Object o2) {
				// Same instance or both null
				if (o1 == o2) { return 0; }
				// Put nulls value a the end (should not happen)
				if (o1 == null) { return -1; }
				if (o2 == null) { return 1; }

				int cmp = o1.toString().compareToIgnoreCase(o2.toString());

				// Return case insensitive comparison if it's significant.
				// Return case sensitive comparison otherwise.
				return cmp != 0 ? cmp : o1.toString().compareTo(o2.toString());
			}
		};
		this.map = new TreeMap(ignoreCaseComparator);
	}

	public JSONSortedObject(Map map) {
		this();
		if (map != null) {
			Iterator i = map.entrySet().iterator();
			while (i.hasNext()) {
				Map.Entry e = (Map.Entry)i.next();
				Object value = e.getValue();
				if (value != null) {
					this.map.put(e.getKey(), this.sortValue(wrap(value)));
				}
			}
		}
	}

	public JSONSortedObject(JSONObject jsonObject) {
		this(jsonObject == null ? null : jsonObject.map);
	}

	public boolean isRecursiveSort() {
		return this.recursiveSort;
	}

	public void setRecursiveSort(boolean recursiveSort) {
		this.recursiveSort = recursiveSort;
	}

	/**
	 * Put a key/value pair in the JSONObject. If the value is null,
	 * then the key will be removed from the JSONObject if it is present.
	 * @param key   A key string.
	 * @param value An object which is the value. It should be of one of these
	 *  types: Boolean, Double, Integer, JSONArray, JSONObject, Long, String,
	 *  or the JSONObject.NULL object.
	 * @return this.
	 * @throws JSONException If the value is non-finite number
	 *  or if the key is null.
	 */
	public JSONObject put(String key, Object value) throws JSONException {
		return super.put(key, this.sortValue(value));
	}

	private Object sortValue(Object value) {
		if (this.recursiveSort && value instanceof JSONObject && !(value instanceof JSONSortedObject)) {
			return new JSONSortedObject((JSONObject)value);
		}
		return value;
	}
}
