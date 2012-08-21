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

import au.gov.aims.atlasmapperserver.annotation.Module;
import au.gov.aims.atlasmapperserver.layerConfig.LayerCatalog;
import au.gov.aims.atlasmapperserver.module.AbstractModule;
import au.gov.aims.atlasmapperserver.module.Info;
import au.gov.aims.atlasmapperserver.module.Tree;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author glafond
 */
public class ModuleHelper {
	/**
	 * 1. Look for moduleName through the list of modules (from a specific package)
	 * 2. Instanciate the moduleName
	 * 3. Call generateCongifuration for the module (define in an Abstract Class)
	 * @param moduleName
	 * @return
	 * @throws JSONException
	 */
	public static JSONObject generateModuleConfiguration(String moduleName, ClientConfig clientConfig, LayerCatalog layerCatalog) throws JSONException {
		AbstractModule moduleObj = getModules().get(moduleName);
		if (moduleObj != null) {
			JSONObject moduleJSon = new JSONObject();
			moduleJSon.put("version", moduleObj.getVersion());
			moduleJSon.put("config", moduleObj.getJSONConfiguration(clientConfig, layerCatalog));
			return moduleJSon;
		}
		return null;
	}

	// Reflexion!!
	public static Map<String, AbstractModule> getModules() {
		Map<String, AbstractModule> moduleClasses = new HashMap<String, AbstractModule>();
		moduleClasses.put("Tree", new Tree());
		moduleClasses.put("Info", new Info());

		return moduleClasses;
	}

	public static List<AbstractModule> getSortedModules() {
		List<AbstractModule> moduleClasses = new ArrayList<AbstractModule>(getModules().values());

		Collections.sort(moduleClasses, new ModuleComparator());

		return moduleClasses;
	}

	private static class ModuleComparator implements Comparator<AbstractModule> {
		@Override
		public int compare(AbstractModule o1, AbstractModule o2) {
			// Same memory address (same instance) or both null
			if (o1 == o2) { return 0; }
			// Move nulls at the end
			if (o1 == null) { return 1; }
			if (o2 == null) { return -1; }

			Class c1 = o1.getClass();
			Class c2 = o2.getClass();

			String t1 = null;
			Module a1 = (Module)c1.getAnnotation(Module.class);
			if (a1 != null) {
				t1 = a1.title();
			}

			String t2 = null;
			Module a2 = (Module)c2.getAnnotation(Module.class);
			if (a2 != null) {
				t2 = a2.title();
			}

			if (t1 != null && t2 != null) {
				int annotationNameCmp = t1.compareTo(t2);
				if (annotationNameCmp != 0) {
					return annotationNameCmp;
				}
			}

			// Both have the same annotation name or both annotations are null
			// => compare using class name
			return c1.getName().compareTo(c2.getName());
		}
	}
}
