/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.gov.aims.atlasmapperserver;

import au.gov.aims.atlasmapperserver.annotation.ConfigField;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author glafond
 */
public class User extends AbstractConfig {
	private static String DEFAULT_PASSWORD = "admin";

	@ConfigField
	private String loginName;

	@ConfigField
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
		return this.encryptedPassword.equals(
				Utils.encrypt(passwordAttempt));
	}
}
