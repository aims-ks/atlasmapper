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

package au.gov.aims.atlasmapperserver.servlet.login;

import au.gov.aims.atlasmapperserver.ConfigHelper;
import au.gov.aims.atlasmapperserver.User;
import au.gov.aims.atlasmapperserver.Utils;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Generate response to requests sent to public/login.jsp.
 * @author glafond
 * TODO Study javax.servlet.http.HttpServletRequest.authenticate / login / logout
 * Example: http://www.avajava.com/tutorials/lessons/how-do-i-log-out-of-an-application-that-uses-form-authentication.html
 * Google Cache (Web Site down): http://webcache.googleusercontent.com/search?q=cache:AMSbQox-2cQJ:www.avajava.com/tutorials/lessons/how-do-i-log-out-of-an-application-that-uses-form-authentication.html+javax+servlet+login+logout+authenticate&cd=9&hl=en&ct=clnk&client=ubuntu&source=www.google.com
 */
public class LoginServlet extends HttpServlet {
	private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());
	private static final String LOGGED_USER_KEY = "logged.user";
	protected static final String REDIRECT_PAGE = "../public/admin.jsp";

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		try {
			this.performTask(request, response);
		} catch (JSONException ex) {
			LOGGER.log(Level.SEVERE, "Can not create a JSON Response.", ex);
		}
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		try {
			this.performTask(request, response);
		} catch (JSONException ex) {
			LOGGER.log(Level.SEVERE, "Can not create a JSON Response.", ex);
		}
	}

	private void performTask(HttpServletRequest request, HttpServletResponse response) throws IOException, JSONException {
		JSONObject result = new JSONObject();
		HttpSession session = request.getSession();
		String actionStr = request.getParameter("action");
		if ("logout".equalsIgnoreCase(actionStr)) {
			this.logout(session, response);
		} else if ("login".equalsIgnoreCase(actionStr)) {
			String loginUsername = request.getParameter("loginUsername");
			String loginPassword = request.getParameter("loginPassword");
			if (loginUsername != null && loginUsername.length() > 0 && loginPassword != null && loginPassword.length() > 0 ) {
				User user = this.login(session, loginUsername, loginPassword);
				if (user != null) {
					result.put("success", true);
				} else {
					result.put("success", false);
					result.put("errors", new JSONObject().put("reason", "Login failed. Try again."));
				}
			} else {
				result.put("success", false);
				result.put("errors", new JSONObject().put("reason", "You must enter a Username and a Password."));
			}

			response.setContentType("application/json");
			response.setStatus(HttpServletResponse.SC_OK);
			this.setResponseContent(response, result.toString());
		} else {
			// Unknown action. Redirect to the home page.
			response.sendRedirect(REDIRECT_PAGE);
		}
	}

	private void setResponseContent(HttpServletResponse response, String content) throws IOException {
		ServletOutputStream out = null;
		try {
			out = response.getOutputStream();
			byte[] bytes = content.getBytes();
			out.write(bytes);
		} finally {
			if (out != null) {
				try {
					out.flush();
				} catch(Exception e) {
					LOGGER.log(Level.SEVERE, "Can not flush the servlet response", e);
				}
				try {
					out.close();
				} catch(Exception e) {
					LOGGER.log(Level.SEVERE, "Can not close the servlet response", e);
				}
			}
		}
	}

	private User login(HttpSession session, String loginName, String password) {
		if (session == null || loginName == null || password == null) {
			return null;
		}

		User user = null;
		try {
			user = ConfigHelper.getConfigManager(session.getServletContext()).getUser(loginName);
		} catch (Exception ex) {
			LOGGER.log(Level.SEVERE, "Can not retrieved users", ex);
		}
		if (user == null) {
			// The user do not exists
			return null;
		}

		boolean isValid = user.verifyPassword(password);

		if (isValid) {
			// TODO Initiate the user session
			LOGGER.log(Level.INFO, "User [{0}] has log in", user.getLoginName());
			session.setAttribute(LOGGED_USER_KEY, user.getLoginName());
			return user;
		}

		return null;
	}

	private void logout(HttpSession session, HttpServletResponse response) {
		if (session == null) { return; }
		String loginName = (String)session.getAttribute(LOGGED_USER_KEY);
		session.removeAttribute(LOGGED_USER_KEY);
		session.invalidate();

		if (Utils.isNotBlank(loginName)) {
			User user = null;
			try {
				user = ConfigHelper.getConfigManager(session.getServletContext()).getUser(loginName);
			} catch (Exception ex) {
				LOGGER.log(Level.SEVERE, "Can not retrieved users", ex);
			}
			if (user != null) {
				LOGGER.log(Level.INFO, "User [{0}] has log out", user.getLoginName());
			}
		} else {
			LOGGER.log(Level.INFO, "Unknown user has log out");
		}

		try {
			response.sendRedirect(REDIRECT_PAGE);
		} catch (IOException ex) {
			LOGGER.log(Level.SEVERE, "Can not redirect the user after a logout!");
		}
	}

	public static User getLoggedUser(HttpSession session) {
		if (session == null) { return null; }

		User user = null;
		String loginName = (String)session.getAttribute(LOGGED_USER_KEY);
		try {
			user = ConfigHelper.getConfigManager(session.getServletContext()).getUser(loginName);
		} catch (Exception ex) {
			LOGGER.log(Level.SEVERE, "Can not retrieved users", ex);
		}
		return user;
	}
}
