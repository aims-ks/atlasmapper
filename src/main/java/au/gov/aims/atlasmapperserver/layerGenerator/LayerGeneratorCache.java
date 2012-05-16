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

package au.gov.aims.atlasmapperserver.layerGenerator;

import au.gov.aims.atlasmapperserver.Utils;
import au.gov.aims.atlasmapperserver.dataSourceConfig.AbstractDataSourceConfig;
import au.gov.aims.atlasmapperserver.dataSourceConfig.WMSDataSourceConfig;
import org.geotools.ows.ServiceException;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LayerGeneratorCache {
	private static final Logger LOGGER = Logger.getLogger(LayerGeneratorCache.class.getName());

	private static Map<String, AbstractLayerGenerator> capabilitiesDocumentsCache = null;

	// Cache timeout in millisecond
	// The GetCapabilities will be redownloaded if the application request
	// information from it and its cached version is older than this setting.
	protected static final long WMS_CAPABILITIES_CACHE_TIMEOUT = 60*60000; // X*60000 = X minutes

	public static AbstractLayerGenerator getInstance(String getCapabilitiesURL, Class layerGeneratorClass, Boolean cachingDisabled) throws IOException, ServiceException {
		if (getCapabilitiesURL == null) { return null; }

		// cachingDisabled is a checkbox, so it is either TRUE of NULL.
		if (cachingDisabled == null) { cachingDisabled = false; }

		if (capabilitiesDocumentsCache == null) {
			capabilitiesDocumentsCache = new HashMap<String, AbstractLayerGenerator>();
		}

		long timeoutTimestamp = Utils.getCurrentTimestamp() - WMS_CAPABILITIES_CACHE_TIMEOUT;
		// If not in cache or its cache has timed out
		if (cachingDisabled ||
				!capabilitiesDocumentsCache.containsKey(getCapabilitiesURL) ||
				capabilitiesDocumentsCache.get(getCapabilitiesURL).instanceTimestamp <= timeoutTimestamp) {

			try {
				Constructor constructor = layerGeneratorClass.getConstructor(String.class);
				AbstractLayerGenerator layerGenerator = (AbstractLayerGenerator)constructor.newInstance(getCapabilitiesURL);
				layerGenerator.instanceTimestamp = Utils.getCurrentTimestamp();

				if (cachingDisabled) {
					return layerGenerator;
				}

				capabilitiesDocumentsCache.put(getCapabilitiesURL, layerGenerator);
			} catch (NoSuchMethodException ex) {
				LOGGER.log(Level.SEVERE, "Can not create the layer Generator for class ["+layerGeneratorClass.getName()+"]", ex);
			} catch (IllegalAccessException ex) {
				LOGGER.log(Level.SEVERE, "Can not create the layer Generator for class ["+layerGeneratorClass.getName()+"]", ex);
			} catch (InstantiationException ex) {
				LOGGER.log(Level.SEVERE, "Can not create the layer Generator for class ["+layerGeneratorClass.getName()+"]", ex);
			} catch (InvocationTargetException ex) {
				LOGGER.log(Level.SEVERE, "Can not create the layer Generator for class ["+layerGeneratorClass.getName()+"]", ex);
			}
		}

		return capabilitiesDocumentsCache.get(getCapabilitiesURL);
	}

	/**
	 * Remove out of date cached documents
	 * @param dataSources
	 */
	public static synchronized void cleanupCapabilitiesDocumentsCache(Collection<AbstractDataSourceConfig> dataSources) {
		if (capabilitiesDocumentsCache != null) {
			long timeout = Utils.getCurrentTimestamp() - WMS_CAPABILITIES_CACHE_TIMEOUT;
			Map<String, AbstractLayerGenerator> cleanCache = new HashMap<String, AbstractLayerGenerator>();
			for (AbstractDataSourceConfig dataSource : dataSources) {
				String capUrl = dataSource.getServiceUrl();
				AbstractLayerGenerator wmsLayers = capabilitiesDocumentsCache.get(capUrl);
				if (wmsLayers != null && wmsLayers.instanceTimestamp > timeout) {
					cleanCache.put(capUrl, wmsLayers);
				}
			}
			capabilitiesDocumentsCache = cleanCache;
		}
	}

	public static synchronized void clearCapabilitiesDocumentsCache() {
		if (capabilitiesDocumentsCache != null) {
			capabilitiesDocumentsCache.clear();
		}
		capabilitiesDocumentsCache = null;
	}

	public static synchronized void clearCapabilitiesDocumentsCache(String getCapabilitiesURL) {
		if (capabilitiesDocumentsCache != null && capabilitiesDocumentsCache.containsKey(getCapabilitiesURL)) {
			capabilitiesDocumentsCache.remove(getCapabilitiesURL);
		}
	}
}
