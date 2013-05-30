package au.gov.aims.atlasmapperserver;/*
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Errors<E extends Errors.Error> {
	private List<E> errors;
	private List<E> warnings;
	private List<E> messages;

	public Errors() {
		this.errors = new ArrayList<E>();
		this.warnings = new ArrayList<E>();
		this.messages = new ArrayList<E>();
	}

	public void addError(E error) {
		// Only add that error if we don't already have it
		for (E err : this.errors) {
			if (err.equals(error)) {
				return;
			}
		}
		this.errors.add(error);
	}
	public List<E> getErrors() {
		return this.errors;
	}

	public void addWarning(E warning) {
		// Only add that warning if we don't already have it
		for (E warn : this.warnings) {
			if (warn.equals(warning)) {
				return;
			}
		}
		this.warnings.add(warning);
	}
	public List<E> getWarnings() {
		return this.warnings;
	}

	public void addMessage(E message) {
		// Only add that message if we don't already have it
		for (E msg : this.messages) {
			if (msg.equals(message)) {
				return;
			}
		}
		this.messages.add(message);
	}
	public List<E> getMessages() {
		return this.messages;
	}

	public void addAll(Errors<E> errors) {
		for (E error : errors.errors) {
			this.addError(error);
		}
		for (E warning : errors.warnings) {
			this.addWarning(warning);
		}
		for (E message : errors.messages) {
			this.addMessage(message);
		}
	}

	/**
	 * {
	 *     "errors": {
	 *         "ea": ["err1", "err2", ...],
	 *         "g": ["err1", "err2", ...]
	 *     },
	 *     "warnings": {
	 *         "ea": ["warn1", "warn2", ...],
	 *         "g": ["warn1", "warn2", ...]
	 *     },
	 *     "messages": {
	 *         "ea": ["msg1", "msg2", ...],
	 *         "g": ["msg1", "msg2", ...]
	 *     }
	 * }
	 * @param errorsMap
	 * @return
	 */
	public static JSONObject toJSON(Map<String, Errors> errorsMap) throws JSONException {
		JSONObject json = new JSONObject();
		if (!errorsMap.isEmpty()) {
			for (Map.Entry<String, Errors> errorsEntry : errorsMap.entrySet()) {
				String dataSourceId = errorsEntry.getKey();
				List<Error> errors = errorsEntry.getValue().getErrors();
				List<Error> warnings = errorsEntry.getValue().getWarnings();
				List<Error> messages = errorsEntry.getValue().getMessages();

				if (errors != null && !errors.isEmpty()) {
					if (!json.has("errors")) {
						json.put("errors", new JSONObject());
					}
					JSONObject jsonErrors = json.optJSONObject("errors");
					if (jsonErrors != null) {
						if (!jsonErrors.has(dataSourceId)) {
							jsonErrors.put(dataSourceId, new JSONArray());
						}
						JSONArray dataSourceJsonErrors = jsonErrors.optJSONArray(dataSourceId);
						if (dataSourceJsonErrors != null) {
							for (Error error : errors) {
								dataSourceJsonErrors.put(error.toJSON());
							}
						}
					}
				}

				if (warnings != null && !warnings.isEmpty()) {
					if (!json.has("warnings")) {
						json.put("warnings", new JSONObject());
					}
					JSONObject jsonWarnings = json.optJSONObject("warnings");
					if (jsonWarnings != null) {
						if (!jsonWarnings.has(dataSourceId)) {
							jsonWarnings.put(dataSourceId, new JSONArray());
						}
						JSONArray dataSourceJsonWarnings = jsonWarnings.optJSONArray(dataSourceId);
						if (dataSourceJsonWarnings != null) {
							for (Error warning : warnings) {
								dataSourceJsonWarnings.put(warning.toJSON());
							}
						}
					}
				}

				if (messages != null && !messages.isEmpty()) {
					if (!json.has("messages")) {
						json.put("messages", new JSONObject());
					}
					JSONObject jsonMessages = json.optJSONObject("messages");
					if (jsonMessages != null) {
						if (!jsonMessages.has(dataSourceId)) {
							jsonMessages.put(dataSourceId, new JSONArray());
						}
						JSONArray dataSourceJsonMessages = jsonMessages.optJSONArray(dataSourceId);
						if (dataSourceJsonMessages != null) {
							for (Error message : messages) {
								dataSourceJsonMessages.put(message.toJSON());
							}
						}
					}
				}
			}
		}
		return json.length() > 0 ? json : null;
	}

	public static abstract class Error {
		public abstract JSONObject toJSON() throws JSONException;
		public abstract boolean equals(Error error);
	}
}
