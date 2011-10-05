/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.gov.aims.atlasmapperserver;

import java.io.File;
import java.io.FilenameFilter;

/**
 *
 * @author glafond
 */
public class MatchFilenameFilter implements FilenameFilter {
	private String filename;

	public MatchFilenameFilter(String filename) {
		this.filename = filename;
	}

	@Override
	public boolean accept(File dir, String name) {
        return (name != null) && name.equals(this.filename);
	}
}
