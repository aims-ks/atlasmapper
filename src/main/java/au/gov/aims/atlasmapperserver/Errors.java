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
package au.gov.aims.atlasmapperserver;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Errors {
	private List<Errors.Error> errors;
	private List<Errors.Error> warnings;
	private List<Errors.Error> messages;

	public Errors() {
		this.errors = new ArrayList<Errors.Error>();
		this.warnings = new ArrayList<Errors.Error>();
		this.messages = new ArrayList<Errors.Error>();
	}

	public void addError(String err) {
		this.addError(new Error(null, err));
	}
	public void addError(String url, String err) {
		this.addError(new Error(url, err));
	}
	public void addError(Errors.Error error) {
		// Only add that error if we don't already have it
		for (Errors.Error err : this.errors) {
			if (err.equals(error)) {
				return;
			}
		}
		this.errors.add(error);
	}
	public List<Errors.Error> getErrors() {
		return this.errors;
	}

	public void addWarning(String warn) {
		this.addWarning(new Error(null, warn));
	}
	public void addWarning(String url, String warn) {
		this.addWarning(new Error(url, warn));
	}
	public void addWarning(Errors.Error warning) {
		// Only add that warning if we don't already have it
		for (Errors.Error warn : this.warnings) {
			if (warn.equals(warning)) {
				return;
			}
		}
		this.warnings.add(warning);
	}
	public List<Errors.Error> getWarnings() {
		return this.warnings;
	}

	public void addMessage(String message) {
		this.addMessage(new Error(null, message));
	}
	public void addMessage(String url, String message) {
		this.addMessage(new Error(url, message));
	}
	public void addMessage(Errors.Error message) {
		// Only add that message if we don't already have it
		for (Errors.Error msg : this.messages) {
			if (msg.equals(message)) {
				return;
			}
		}
		this.messages.add(message);
	}
	public List<Errors.Error> getMessages() {
		return this.messages;
	}

	public void addAll(Errors errors) {
		if (errors != null) {
			for (Errors.Error error : errors.errors) {
				this.addError(error);
			}
			for (Errors.Error warning : errors.warnings) {
				this.addWarning(warning);
			}
			for (Errors.Error message : errors.messages) {
				this.addMessage(message);
			}
		}
	}

	public void addErrors(JSONArray errors) throws JSONException {
		if (errors != null) {
			for (int i=0; i<errors.length(); i++) {
				this.addError(new Errors.Error(null, errors.optString(i, null)));
			}
		}
	}
	public void addWarnings(JSONArray warnings) throws JSONException {
		if (warnings != null) {
			for (int i=0; i<warnings.length(); i++) {
				this.addWarning(new Errors.Error(null, warnings.optString(i, null)));
			}
		}
	}
	public void addMessages(JSONArray messages) throws JSONException {
		if (messages != null) {
			for (int i=0; i<messages.length(); i++) {
				this.addMessage(new Errors.Error(null, messages.optString(i, null)));
			}
		}
	}

	public boolean isEmpty() {
		return this.errors.isEmpty() && this.warnings.isEmpty() && this.messages.isEmpty();
	}

	/**
	 * {
	 *     "errors": ["err1", "err2", ...],
	 *     "warnings": ["warn1", "warn2", ...],
	 *     "messages": ["msg1", "msg2", ...]
	 * }
	 * @return
	 */
	public JSONObject toJSON() throws JSONException {
		JSONObject json = new JSONObject();
		if (!this.isEmpty()) {
			List<Errors.Error> errors = this.getErrors();
			List<Errors.Error> warnings = this.getWarnings();
			List<Errors.Error> messages = this.getMessages();

			if (errors != null && !errors.isEmpty()) {
				JSONArray jsonErrors = new JSONArray();
				for (Error error : errors) {
					jsonErrors.put(error.toJSON());
				}
				json.put("errors", jsonErrors);
			}

			if (warnings != null && !warnings.isEmpty()) {
				JSONArray jsonWarnings = new JSONArray();
				for (Error warning : warnings) {
					jsonWarnings.put(warning.toJSON());
				}
				json.put("warnings", jsonWarnings);
			}

			if (messages != null && !messages.isEmpty()) {
				JSONArray jsonMessages = new JSONArray();
				for (Error message : messages) {
					jsonMessages.put(message.toJSON());
				}
				json.put("messages", jsonMessages);
			}
		}
		return json.length() > 0 ? json : null;
	}

	/**
	 * {
	 *     "errors": {
	 *         "client name 1 (id)": ["err1", "err2", ...],
	 *         "client name 2 (id)": ["err1", "err2", ...]
	 *     },
	 *     "warnings": {
	 *         "client name 1 (id)": ["warn1", "warn2", ...],
	 *         "client name 2 (id)": ["warn1", "warn2", ...]
	 *     },
	 *     "messages": {
	 *         "client name 1 (id)": ["msg1", "msg2", ...],
	 *         "client name 2 (id)": ["msg1", "msg2", ...]
	 *     }
	 * }
	 * @param errorsMap
	 * @return
	 */
	public static JSONObject toJSON(Map<String, Errors> errorsMap) throws JSONException {
		JSONObject json = new JSONObject();
		if (!errorsMap.isEmpty()) {
			for (Map.Entry<String, Errors> errorsEntry : errorsMap.entrySet()) {
				String clientName = errorsEntry.getKey();
				List<Error> errors = errorsEntry.getValue().getErrors();
				List<Error> warnings = errorsEntry.getValue().getWarnings();
				List<Error> messages = errorsEntry.getValue().getMessages();

				if (errors != null && !errors.isEmpty()) {
					if (!json.has("errors")) {
						json.put("errors", new JSONObject());
					}
					JSONObject jsonErrors = json.optJSONObject("errors");
					if (jsonErrors != null) {
						if (!jsonErrors.has(clientName)) {
							jsonErrors.put(clientName, new JSONArray());
						}
						JSONArray dataSourceJsonErrors = jsonErrors.optJSONArray(clientName);
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
						if (!jsonWarnings.has(clientName)) {
							jsonWarnings.put(clientName, new JSONArray());
						}
						JSONArray dataSourceJsonWarnings = jsonWarnings.optJSONArray(clientName);
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
						if (!jsonMessages.has(clientName)) {
							jsonMessages.put(clientName, new JSONArray());
						}
						JSONArray dataSourceJsonMessages = jsonMessages.optJSONArray(clientName);
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

	public static class Error {
		private String url;
		private String msg;

		public Error(String url, String msg) {
			this.url = url;
			this.msg = msg;
		}

		public String getUrl() { return this.url; }

		public String getMsg() { return this.msg; }

		/**
		 * Return a JSONObject { "url": "message" }
		 * or a String "message" when URL is null.
		 * @return
		 * @throws JSONException
		 */
		public Object toJSON() throws JSONException {
			if (this.url == null || this.url.isEmpty()) {
				return this.msg;
			} else {
				JSONObject json = new JSONObject();
				json.put(this.url, this.msg);
				return json;
			}
		}

		public boolean equals(Errors.Error error) {
			// Same instance
			if (this == error) {
				return true;
			}
			// Not same URL
			if ((this.url == null && error.url != null) ||
					(this.url != null && !this.url.equals(error.url))) {
				return false;
			}
			// Same URL, same msg instance or both null
			if (this.msg == error.msg) {
				return true;
			}
			// Same URL, only one msg is null
			if (this.msg == null || error.msg == null) {
				return false;
			}
			return this.msg.equals(error.msg);
		}
	}
}
