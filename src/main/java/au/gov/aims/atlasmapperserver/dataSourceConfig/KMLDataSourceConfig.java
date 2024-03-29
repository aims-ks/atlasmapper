/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2011 Australian Institute of Marine Science
 *
 *  Contact: Gael Lafond <g.lafond@aims.gov.au>
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
import au.gov.aims.atlasmapperserver.ConfigManager;
import au.gov.aims.atlasmapperserver.Utils;
import au.gov.aims.atlasmapperserver.annotation.ConfigField;
import au.gov.aims.atlasmapperserver.layerConfig.KMLLayerConfig;
import au.gov.aims.atlasmapperserver.layerGenerator.AbstractLayerGenerator;
import au.gov.aims.atlasmapperserver.layerGenerator.KMLLayerGenerator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class KMLDataSourceConfig extends AbstractDataSourceConfig {
	private static final Logger LOGGER = Logger.getLogger(KMLDataSourceConfig.class.getName());

	@ConfigField
	private JSONArray kmlData;

	@Deprecated
	@ConfigField
	private String kmlUrls;

	public KMLDataSourceConfig(ConfigManager configManager) {
		super(configManager);
	}

	/**
	 * @deprecated  As of release 1.3.1, replaced by {@link #setKmlData(JSONArray kmlDatas)}
	 */
	@Deprecated
	public void setKmlUrls(String kmlUrls) throws JSONException {
		if (Utils.isNotBlank(kmlUrls)) {
			LOGGER.log(Level.WARNING, "DEPRECATED KmlUrls string. The KmlUrls has been converted:\n{0}", kmlUrls);
			Set<String> kmlUrlsSet = AbstractConfig.toSet(kmlUrls);

			this.kmlData = new JSONArray();
			int i=0;
			for (String kmlUrlStr : kmlUrlsSet) {
				JSONObject urlObj = new JSONObject();
				String id = this.getKmlUrlId(kmlUrlStr);
				if (id == null) {
					id = ""+(i++);
				}
				urlObj.put("id", id);
				urlObj.put("url", kmlUrlStr);
				urlObj.put("title", id);
				this.kmlData.put(urlObj);
			}
		}
	}
	@Deprecated
	private String getKmlUrlId(String kmlUrl) {
		if (Utils.isBlank(kmlUrl)) {
			return null;
		}

		int layerIdStart = kmlUrl.lastIndexOf('/');
		if (layerIdStart < 0) {
			return null;
		}
		layerIdStart++;

		int layerIdEnd = kmlUrl.lastIndexOf('.');
		layerIdEnd = (layerIdEnd >= 0 ? layerIdEnd : kmlUrl.length());

		return this.getDataSourceId() + "_" +  kmlUrl.substring(layerIdStart, layerIdEnd);
	}
	/**
	 * @deprecated  As of release 1.3.1, replaced by {@link #getKmlData()}
	 */
	@Deprecated
	public String getKmlUrls() {
		return null;
	}

	// Note to myself: The word data is the plural of datum
	public JSONArray getKmlData() {
		return this.kmlData;
	}
	public void setKmlData(JSONArray kmlData) {
		this.kmlData = kmlData;
	}

	@Override
	public AbstractLayerGenerator<KMLLayerConfig, KMLDataSourceConfig> createLayerGenerator() {
		return new KMLLayerGenerator();
	}

	@Override
	public String toString() {
		return "KMLDataSourceConfig {\n" +
				(this.getId()==null ? "" :                             "	id=" + this.getId() + "\n") +
				(this.kmlData == null || this.kmlData.length() <= 0 ? "" :     "	kmlData=" + this.kmlData.toString() + "\n") +
				(Utils.isBlank(this.getDataSourceId()) ? "" :          "	dataSourceId=" + this.getDataSourceId() + "\n") +
				(Utils.isBlank(this.getDataSourceName()) ? "" :        "	dataSourceName=" + this.getDataSourceName() + "\n") +
				(Utils.isBlank(this.getLayerType()) ? "" :             "	layerType=" + this.getLayerType() + "\n") +
				(Utils.isBlank(this.getServiceUrl()) ? "" :            "	serviceUrl=" + this.getServiceUrl() + "\n") +
				(Utils.isBlank(this.getFeatureRequestsUrl()) ? "" :    "	featureRequestsUrl=" + this.getFeatureRequestsUrl() + "\n") +
				(Utils.isBlank(this.getLegendUrl()) ? "" :             "	legendUrl=" + this.getLegendUrl() + "\n") +
				(this.getLegendParameters()==null ? "" :               "	legendParameters=" + this.getLegendParameters() + "\n") +
				(Utils.isBlank(this.getBlackAndWhiteListedLayers()) ? "" :     "	blackAndWhiteListedLayers=" + this.getBlackAndWhiteListedLayers() + "\n") +
				(this.isShowInLegend()==null ? "" :                    "	showInLegend=" + this.isShowInLegend() + "\n") +
				(Utils.isBlank(this.getComment()) ? "" :               "	comment=" + this.getComment() + "\n") +
				'}';
	}
}
