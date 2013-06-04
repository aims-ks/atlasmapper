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
