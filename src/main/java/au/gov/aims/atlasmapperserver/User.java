/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2011 Australian Institute of Marine Science
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

import au.gov.aims.atlasmapperserver.annotation.ConfigField;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author glafond
 */
public class User extends AbstractConfig {
	private static final Logger LOGGER = Logger.getLogger(User.class.getName());
	private static final String DEFAULT_PASSWORD = "admin";

	@ConfigField(demoReadOnly = true)
	private String loginName;

	@ConfigField(demoReadOnly = true)
	private String encryptedPassword;

	@ConfigField
	private String firstName;

	@ConfigField
	private String lastName;
	// TODO Add user info; email, birthdate, etc.

	public User(ConfigManager configManager) {
		super(configManager);
		this.encryptedPassword = Utils.encrypt(DEFAULT_PASSWORD);
	}

	public User(ConfigManager configManager, String loginName, String password) {
		super(configManager);
		this.loginName = loginName;
		this.encryptedPassword = Utils.encrypt(password);
	}

	@Override
	public void setJSONObjectKey(String key) {
		this.setLoginName(key);
	}

	@Override
	public String getJSONObjectKey() {
		return this.loginName;
	}

	@Override
	public JSONObject toJSonObject() throws JSONException {
		JSONObject json = super.toJSonObject();
		return json;
	}

	public void setLoginName(String loginName) {
		if (Utils.isNotBlank(loginName)) {
			this.loginName = loginName;
		}
	}

	public String getLoginName() {
		return this.loginName;
	}

	public void setName(String firstName, String lastName) {
		this.firstName = firstName;
		this.lastName = lastName;
	}

	public String getName() {
		String userName = ((this.firstName == null ? "" : this.firstName) + " " +
				(this.lastName == null ? "" : this.lastName)).trim();

		return Utils.isNotBlank(userName) ? userName : this.loginName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getEncryptedPassword() {
		return encryptedPassword;
	}

	public void setEncryptedPassword(String encryptedPassword) {
		if (Utils.isNotBlank(encryptedPassword)) {
			this.encryptedPassword = encryptedPassword;
		}
	}

	public void setPassword(String password) {
		if (Utils.isNotBlank(password)) {
			this.encryptedPassword = Utils.encrypt(password);
		}
	}

	public boolean verifyPassword(String passwordAttempt) {
		try {
			this.getConfigManager().reloadUsersConfigIfNeeded();
		} catch (Exception ex) {
			LOGGER.log(Level.SEVERE, "Could not reload Users: {0}", Utils.getExceptionMessage(ex));
			LOGGER.log(Level.FINE, "Stack trace:", ex);
			return false;
		}
		return this.encryptedPassword.equals(
				Utils.encrypt(passwordAttempt));
	}
}
