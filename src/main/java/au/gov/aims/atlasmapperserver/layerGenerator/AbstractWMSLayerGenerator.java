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

import au.gov.aims.atlasmapperserver.ClientConfig;
import au.gov.aims.atlasmapperserver.ConfigManager;
import au.gov.aims.atlasmapperserver.URLCache;
import au.gov.aims.atlasmapperserver.Utils;
import au.gov.aims.atlasmapperserver.dataSourceConfig.AbstractDataSourceConfig;
import au.gov.aims.atlasmapperserver.dataSourceConfig.WMSDataSourceConfig;
import au.gov.aims.atlasmapperserver.layerConfig.LayerStyleConfig;
import au.gov.aims.atlasmapperserver.layerConfig.WMSLayerConfig;
import org.geotools.data.ows.CRSEnvelope;
import org.geotools.data.ows.Layer;
import org.geotools.data.ows.OperationType;
import org.geotools.data.ows.Service;
import org.geotools.data.ows.StyleImpl;
import org.geotools.data.ows.WMSCapabilities;
import org.geotools.data.ows.WMSRequest;
import org.geotools.ows.ServiceException;
import org.opengis.util.InternationalString;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractWMSLayerGenerator<L extends WMSLayerConfig, D extends WMSDataSourceConfig> extends AbstractLayerGenerator<L, D> {
	private static final Logger LOGGER = Logger.getLogger(AbstractWMSLayerGenerator.class.getName());

	// Layer config, for each datasource
	// Map<String datasourceId, Map<String layerId, AbstractLayerConfig layer>>
//	private Map<String, Map<String, L>> layerConfigsCache = null;

	private WMSCapabilities wmsCapabilities = null;
	private String wmsVersion = null;
	private URL featureRequestsUrl = null;
	private URL wmsServiceUrl = null;
	private URL legendUrl = null;

	public AbstractWMSLayerGenerator(AbstractDataSourceConfig dataSource) throws IOException, ServiceException {
//		this.layerConfigsCache = new HashMap<String, Map<String, L>>();

		this.wmsCapabilities = URLCache.getWMSCapabilitiesResponse(dataSource, dataSource.getServiceUrl());
		if (this.wmsCapabilities != null) {
			WMSRequest wmsRequestCapabilities = this.wmsCapabilities.getRequest();
			if (wmsRequestCapabilities != null) {
				this.featureRequestsUrl = this.getOperationUrl(wmsRequestCapabilities.getGetFeatureInfo());
				this.wmsServiceUrl = this.getOperationUrl(wmsRequestCapabilities.getGetMap());
				this.legendUrl = this.getOperationUrl(wmsRequestCapabilities.getGetLegendGraphic());
				this.wmsVersion = this.wmsCapabilities.getVersion();

				// Implemented in the API - Not implemented in GeoTools
				//this.stylesUrl = this.getOperationUrl(request.getGetStyles());
			}
		}
	}

	protected abstract L createLayerConfig(ConfigManager configManager);

	/**
	 * WMS Server already has a unique layer ID for each layers. Nothing to do here.
	 * @param layer
	 * @param dataSourceConfig
	 * @return
	 */
	@Override
	protected String getUniqueLayerId(WMSLayerConfig layer, WMSDataSourceConfig dataSourceConfig) {
		return layer.getLayerId();
	}

	@Override
	public Map<String, L> generateLayerConfigs(ClientConfig clientConfig, D dataSourceConfig) {
		return this.getLayersInfoFromCaps(clientConfig, dataSourceConfig);
	}

	private URL getOperationUrl(OperationType op) {
		if (op == null) {
			return null;
		}
		return op.getGet();
	}

	private Map<String, L> getLayersInfoFromCaps(
			ClientConfig clientConfig,        // Client's specific layers information (base layers list override, etc.)
			D dataSourceConfig // Data source of layers (to link the layer to its data source)
	) {

		// http://docs.geotools.org/stable/javadocs/org/geotools/data/wms/WebMapServer.html
		Layer rootLayer = this.wmsCapabilities.getLayer();

		Map<String, L> layerInfos = new HashMap<String, L>();
		// Can add it in the config if some users think it's usefull to see the root...

		return this._getLayersInfoFromGeoToolRootLayer(layerInfos, rootLayer, new LinkedList<String>(), clientConfig, dataSourceConfig, true);
	}

	/**
	 * Apply CapabilitiesDocuments overrides to the dataSourceConfig
	 * @param dataSourceConfig
	 * @return
	 */
	@Override
	public D applyOverrides(D dataSourceConfig) {
		D clone = (D) dataSourceConfig.clone();

		if (this.featureRequestsUrl != null && Utils.isBlank(clone.getFeatureRequestsUrl())) {
			clone.setFeatureRequestsUrl(this.featureRequestsUrl.toString());
		}

		// The wmsServiceUrl set in the AbstractDataSourceConfig is the one set in the
		// AtlasMapper server GUI. It may contains the capabilities URL.
		// The GetMap URL found in the capabilities document is always safer.
		if (this.wmsServiceUrl != null) {
			clone.setServiceUrl(this.wmsServiceUrl.toString());
		}

		if (this.legendUrl != null && Utils.isBlank(clone.getLegendUrl())) {
			clone.setLegendUrl(this.legendUrl.toString());
		}

		if (Utils.isBlank(this.wmsVersion) && Utils.isBlank(clone.getWmsVersion())) {
			clone.setWmsVersion(this.wmsVersion);
		}

		return clone;
	}

	// Internal recursive function that takes an actual map of layer and add more layers to it.
	// The method signature suggest that the parameter xmlLayers stay unchanged,
	// and a new map containing the layers is returned. If fact, it modify the
	// map receive as parameter. The other way would be inefficient.
	// I.E.
	// The line
	//     xmlLayers = getXmlLayersFromGeoToolRootLayer(xmlLayers, childLayer, childrenWmsPath);
	// give the same result as
	//     getXmlLayersFromGeoToolRootLayer(xmlLayers, childLayer, childrenWmsPath);
	// The first one is just visualy more easy to understand.
	private Map<String, L> _getLayersInfoFromGeoToolRootLayer(
			Map<String, L> layerConfigs,
			Layer layer,
			List<String> wmsPath,
			ClientConfig clientConfig,
			D dataSourceConfig,
			boolean isRoot) {

		if (layer == null) {
			return layerConfigs;
		}

		// GeoTools API documentation is hopeless and the code is build on compile time.
		// There is the most helpfull reference I found so far:
		// http://svn.osgeo.org/geotools/trunk/modules/extension/wms/src/main/java/org/geotools/data/ows/Layer.java
		List<Layer> children = layer.getLayerChildren();

		if (children != null && !children.isEmpty()) {
			// The layer has children, so it is a Container;
			// If it's not the root, it's a part of the WMS Path, represented as a folder in the GUI.
			List<String> childrenWmsPath = new LinkedList<String>(wmsPath);
			if (!isRoot) {
				String newWmsPart = layer.getTitle();
				if (Utils.isBlank(newWmsPart)) {
					newWmsPart = layer.getName();
				}

				if (Utils.isNotBlank(newWmsPart)) {
					childrenWmsPath.add(newWmsPart);
				}
			}

			for (Layer childLayer : children) {
				layerConfigs = _getLayersInfoFromGeoToolRootLayer(layerConfigs, childLayer, childrenWmsPath, clientConfig, dataSourceConfig, false);
			}
		} else {
			// The layer do not have any children, so it is a real layer

			// Create a string representing the wmsPath
			StringBuilder wmsPathBuf = new StringBuilder();
			for (String wmsPathPart : wmsPath) {
				if (Utils.isNotBlank(wmsPathPart)) {
					if (wmsPathBuf.length() > 0) {
						wmsPathBuf.append("/");
					}
					wmsPathBuf.append(wmsPathPart);
				}
			}

			L layerConfig = this.layerToLayerConfig(layer, wmsPathBuf.toString(), clientConfig, dataSourceConfig);
			if (layerConfig != null) {
				layerConfigs.put(layerConfig.getLayerId(), layerConfig);
			}
		}

		return layerConfigs;
	}

	private L layerToLayerConfig(
			Layer layer,
			String wmsPath,
			ClientConfig clientConfig,
			D dataSourceConfig) {

		L layerConfig = this.createLayerConfig(clientConfig.getConfigManager());

		// Link the layer to it's data source
		layerConfig.setDataSourceId(dataSourceConfig.getDataSourceId());

		String layerId = layer.getName();
		if (Utils.isBlank(layerId)) {
			String serviceTitle = "unknown";
			Service service = this.wmsCapabilities.getService();
			if (service != null) {
				serviceTitle = service.getTitle();
			}

			LOGGER.log(Level.WARNING, "The Capabilities Document [{0}] contains layers without name (other than the root layer).", serviceTitle);
			return null;
		}

		layerConfig.setLayerId(layerId);
		this.ensureUniqueLayerId(layerConfig, dataSourceConfig);
		layerId = layerConfig.getLayerId();

		String title = layer.getTitle();
		if (Utils.isNotBlank(title)) {
			layerConfig.setTitle(title);
		}

		String description = layer.get_abstract();
		if (Utils.isNotBlank(description)) {
			layerConfig.setDescription(description);
		}
		layerConfig.setWmsQueryable(layer.isQueryable());

		if (Utils.isNotBlank(wmsPath)) {
			layerConfig.setWmsPath(wmsPath);
		}

		// Set Baselayer flag if the layer is defined as a base layer in the client OR the client do not define any base layers and the layer is defined as a baselayer is the global config
		if (clientConfig.isOverrideBaseLayers() != null && clientConfig.isOverrideBaseLayers()) {
			if (clientConfig.isBaseLayer(layerId)) {
				layerConfig.setIsBaseLayer(true);
			}
		} else {
			if (dataSourceConfig.isBaseLayer(layerId)) {
				layerConfig.setIsBaseLayer(true);
			} else if (dataSourceConfig.isBaseLayer(layer.getName())) {
				// Backward compatibility
				LOGGER.log(Level.WARNING, "DEPRECATED LAYER ID USED FOR BASE LAYERS: Layer id [{0}] should be [{1}].", new String[]{layer.getName(), layerId});
				layerConfig.setIsBaseLayer(true);
			}
		}

		List<StyleImpl> styleImpls = layer.getStyles();
		if (styleImpls != null && !styleImpls.isEmpty()) {
			List<LayerStyleConfig> styles = new ArrayList<LayerStyleConfig>(styleImpls.size());
			for (StyleImpl styleImpl : styleImpls) {
				LayerStyleConfig styleConfig = styleToLayerStyleConfig(clientConfig.getConfigManager(), styleImpl);

				if (styleConfig != null) {
					styles.add(styleConfig);
				}
			}
			if (!styles.isEmpty()) {
				layerConfig.setStyles(styles);
			}
		}

		CRSEnvelope boundingBox = layer.getLatLonBoundingBox();
		if (boundingBox != null) {
			double[] boundingBoxArray = {
					boundingBox.getMinX(), boundingBox.getMinY(),
					boundingBox.getMaxX(), boundingBox.getMaxY()
			};
			layerConfig.setLayerBoundingBox(boundingBoxArray);
		}

		return layerConfig;
	}

	private LayerStyleConfig styleToLayerStyleConfig(ConfigManager configManager, StyleImpl style) {
		String name = style.getName();
		InternationalString intTitle = style.getTitle();
		InternationalString intDescription = style.getAbstract();

		// styleImpl.getLegendURLs() is not implemented yet (gt-wms ver. 2.7.2)
		// See modules/extension/wms/src/main/java/org/geotools/data/wms/xml/WMSComplexTypes.java line 4155
		//List u = styleImpl.getLegendURLs();

		LayerStyleConfig styleConfig = new LayerStyleConfig(configManager);
		styleConfig.setName(name);
		if (intTitle != null) {
			styleConfig.setTitle(intTitle.toString());
		}
		if (intDescription != null) {
			styleConfig.setDescription(intDescription.toString());
		}

		return styleConfig;
	}
}
