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

package au.gov.aims.atlasmapperserver.dataSourceConfig;

import au.gov.aims.atlasmapperserver.AbstractConfig;
import au.gov.aims.atlasmapperserver.ClientConfig;
import au.gov.aims.atlasmapperserver.Utils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * The aim of this class is to group methods that are common to every classes that implement
 * the AbstractDataSourceConfigInterface. An abstract class would have done the trick,
 * but it's not possible to extends multiple classes.
 */
public class AbstractDataSourceConfigInterfaceHelper {
	// TODO Remove clientConfig parameter!!
	public static JSONObject generateDataSourceInterface(AbstractDataSourceConfigInterface dataSourceInterface, ClientConfig clientConfig) throws JSONException {

		JSONObject dataSource = new JSONObject();

		if (Utils.isNotBlank(dataSourceInterface.getFeatureRequestsUrl())) {
			dataSource.put("featureRequestsUrl", dataSourceInterface.getFeatureRequestsUrl().trim());
		}

		if (Utils.isNotBlank(dataSourceInterface.getServiceUrl())) {
			// TODO remove wms from the property name
			dataSource.put("wmsServiceUrl", dataSourceInterface.getServiceUrl().trim());
		}

		if (Utils.isNotBlank(dataSourceInterface.getLegendUrl())) {
			dataSource.put("legendUrl", dataSourceInterface.getLegendUrl().trim());
		}

		if (Utils.isNotBlank(dataSourceInterface.getStylesUrl())) {
			dataSource.put("stylesUrl", dataSourceInterface.getStylesUrl().trim());
		}

		if (Utils.isNotBlank(dataSourceInterface.getDataSourceName())) {
			dataSource.put("dataSourceName", dataSourceInterface.getDataSourceName().trim());
		}

		if (Utils.isNotBlank(dataSourceInterface.getDataSourceType())) {
			dataSource.put("dataSourceType", dataSourceInterface.getDataSourceType().trim());
		}

		JSONObject legendParameters = AbstractDataSourceConfigInterfaceHelper.getDataSourceLegendParametersJson(dataSourceInterface.getLegendParameters());
		// merge with client legend parameters, if any
		// TODO Move this logic in the client generation!!
		if (clientConfig != null) {
			JSONObject clientLegendParameters = clientConfig.getLegendParametersJson();
			if (clientLegendParameters != null) {
				JSONObject mergeParameters = new JSONObject();
				if (legendParameters != null) {
					Iterator<String> keys = legendParameters.keys();
					while(keys.hasNext()) {
						String key = keys.next();
						if (!legendParameters.isNull(key)) {
							Object value = legendParameters.opt(key);
							if (value != null) {
								mergeParameters.put(key, value);
							}
						}
					}
				}
				Iterator<String> keys = clientLegendParameters.keys();
				while(keys.hasNext()) {
					String key = keys.next();
					if (clientLegendParameters.isNull(key)) {
						if (mergeParameters.has(key)) {
							mergeParameters.remove(key);
						}
					} else {
						Object value = clientLegendParameters.opt(key);
						if (value != null) {
							mergeParameters.put(key, value);
						}
					}
				}
				legendParameters = mergeParameters;
			}
		}
		if (legendParameters != null && legendParameters.length() > 0) {
			dataSource.put("legendParameters", legendParameters);
		}

		return dataSource;
	}

	private static JSONObject getDataSourceLegendParametersJson(String legendParameters) throws JSONException {
		if (legendParameters == null) {
			return null;
		}

		String trimmedLegendParameters = legendParameters.trim();
		if (trimmedLegendParameters.isEmpty()) {
			return null;
		}

		JSONObject legendParametersJson = new JSONObject();
		for (String legendParameter : AbstractConfig.toSet(trimmedLegendParameters)) {
			if (Utils.isNotBlank(legendParameter)) {
				String[] attribute = legendParameter.split(AbstractConfig.SPLIT_ATTRIBUTES_PATTERN);
				if (attribute != null && attribute.length >= 2) {
					legendParametersJson.put(
							attribute[0].trim(),  // Key
							attribute[1].trim()); // Value
				}
			}
		}

		return legendParametersJson;
	}
}
