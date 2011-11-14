/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.gov.aims.atlasmapperserver;

import java.util.ResourceBundle;

/**
 *
 * @author glafond
 */
public class ProjectInfo {
	private static ResourceBundle propreties = ResourceBundle.getBundle("project");

	// Singleton
	private ProjectInfo() {}

	/**
	 * Return the full version string of the present war,
	 * or <code>null</code> if it cannot be determined.
	 * @see java.lang.Package#getImplementationVersion()
	 */
	public static String getName() {
		return propreties.getString("project.name");
	}

	public static String getVersion() {
		return propreties.getString("project.version");
	}

	public static String getDescription() {
		return propreties.getString("project.description");
	}

	public static String getUrl() {
		return propreties.getString("project.url");
	}
}
