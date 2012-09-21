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

import au.gov.aims.atlasmapperserver.ConfigManager;
import au.gov.aims.atlasmapperserver.URLCache;
import au.gov.aims.atlasmapperserver.Utils;
import au.gov.aims.atlasmapperserver.dataSourceConfig.WMSDataSourceConfig;
import au.gov.aims.atlasmapperserver.layerConfig.LayerStyleConfig;
import au.gov.aims.atlasmapperserver.layerConfig.WMSLayerConfig;
import au.gov.aims.atlasmapperserver.xml.TC211.Document;
import au.gov.aims.atlasmapperserver.xml.TC211.Parser;
import org.geotools.data.ows.CRSEnvelope;
import org.geotools.data.ows.Layer;
import org.geotools.data.ows.OperationType;
import org.geotools.data.ows.Service;
import org.geotools.data.ows.StyleImpl;
import org.geotools.data.ows.WMSCapabilities;
import org.geotools.data.ows.WMSRequest;
import org.geotools.data.wms.xml.MetadataURL;
import org.geotools.data.wms.xml.WMSComplexTypes;
import org.geotools.ows.ServiceException;
import org.opengis.util.InternationalString;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractWMSLayerGenerator<L extends WMSLayerConfig, D extends WMSDataSourceConfig> extends AbstractLayerGenerator<L, D> {
	private static final Logger LOGGER = Logger.getLogger(AbstractWMSLayerGenerator.class.getName());

	private WMSCapabilities wmsCapabilities = null;
	private String wmsVersion = null;
	private URL featureRequestsUrl = null;
	private URL wmsServiceUrl = null;
	private URL legendUrl = null;
	private URL stylesUrl = null;

	public AbstractWMSLayerGenerator(D dataSource) throws IOException, ServiceException {
		super(dataSource);

		this.wmsCapabilities = URLCache.getWMSCapabilitiesResponse(dataSource, dataSource.getServiceUrl());
		if (this.wmsCapabilities != null) {
			WMSRequest wmsRequestCapabilities = this.wmsCapabilities.getRequest();
			if (wmsRequestCapabilities != null) {
				this.featureRequestsUrl = this.getOperationUrl(wmsRequestCapabilities.getGetFeatureInfo());
				this.wmsServiceUrl = this.getOperationUrl(wmsRequestCapabilities.getGetMap());
				this.legendUrl = this.getOperationUrl(wmsRequestCapabilities.getGetLegendGraphic());
				this.wmsVersion = this.wmsCapabilities.getVersion();
				this.stylesUrl = this.getOperationUrl(wmsRequestCapabilities.getGetStyles());
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
	public Collection<L> generateLayerConfigs(D dataSourceConfig) {
		return this.getLayersInfoFromCaps(dataSourceConfig);
	}

	private URL getOperationUrl(OperationType op) {
		if (op == null) {
			return null;
		}
		return op.getGet();
	}

	private Collection<L> getLayersInfoFromCaps(
			D dataSourceConfig // Data source of layers (to link the layer to its data source)
	) {

		// http://docs.geotools.org/stable/javadocs/org/geotools/data/wms/WebMapServer.html
		Layer rootLayer = this.wmsCapabilities.getLayer();

		Collection<L> layerConfigs = new ArrayList<L>();
		// The boolean at the end is use to ignore the root from the capabilities document. It can be added (change to false) if some users think it's useful to see the root...
		this._getLayersInfoFromGeoToolRootLayer(layerConfigs, rootLayer, new LinkedList<String>(), dataSourceConfig, true);

		return layerConfigs;
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

		if (this.stylesUrl != null && Utils.isBlank(clone.getStylesUrl())) {
			clone.setStylesUrl(this.stylesUrl.toString());
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
	private void _getLayersInfoFromGeoToolRootLayer(
			Collection<L> layerConfigs,
			Layer layer,
			List<String> wmsPath,
			D dataSourceConfig,
			boolean isRoot) {

		if (layer == null) {
			return;
		}

		// GeoTools API documentation is hopeless. The easiest way to know what the API do is to look at the sources:
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
				this._getLayersInfoFromGeoToolRootLayer(layerConfigs, childLayer, childrenWmsPath, dataSourceConfig, false);
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

			L layerConfig = this.layerToLayerConfig(layer, wmsPathBuf.toString(), dataSourceConfig);
			if (layerConfig != null) {
				layerConfigs.add(layerConfig);
			}
		}
	}

	/**
	 * Convert a GeoTool Layer into a AtlasMapper Layer
	 * @param layer
	 * @param wmsPath
	 * @param dataSourceConfig
	 * @return
	 */
	private L layerToLayerConfig(
			Layer layer,
			String wmsPath,
			D dataSourceConfig) {

		L layerConfig = this.createLayerConfig(dataSourceConfig.getConfigManager());

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

		Document tc211Document = null;
		List<MetadataURL> metadataUrls = layer.getMetadataURL();
		for (MetadataURL metadataUrl : metadataUrls) {
			if ("TC211".equalsIgnoreCase(metadataUrl.getType())) {
				URL url = metadataUrl.getUrl();
				if (url != null) {
					String urlString = url.toString();
					try {
						tc211Document = Parser.parseURI(urlString);
					} catch (Exception e) {
						LOGGER.log(Level.SEVERE, "Unexpected exception while parsing the document URL ["+urlString+"]", e);
					}
				}
			}
		}

		layerConfig.setLayerId(layerId);
		this.ensureUniqueLayerId(layerConfig, dataSourceConfig);
		layerId = layerConfig.getLayerId();

		String title = layer.getTitle();
		if (Utils.isNotBlank(title)) {
			layerConfig.setTitle(title);
		}


		String description = null;
		// Get the description from the metadata document, if available
		if (tc211Document != null) {
			description = tc211Document.getAbstract();

			List<Document.Link> links = tc211Document.getLinks();
			if (links != null && !links.isEmpty()) {
				// Set the Online Resources, using wiki format
				StringBuilder onlineResources = new StringBuilder("\n\n*Online Resources*\n");
				for (Document.Link link : links) {
					// Only display links with none null URL and that are not a WMS GetMap url.
					if (Utils.isNotBlank(link.getUrl()) && !link.isWMSGetMapLink()) {
						String linkTitle = link.getDescription();
						if (Utils.isBlank(linkTitle)) {
							linkTitle = link.getName();
						}
						if (Utils.isBlank(linkTitle)) {
							linkTitle = link.getUrl();
						}

						// Bullet list of URLs, in Wiki format:
						// * [[url|title]]
						// * [[url|title]]
						// * [[url|title]]
						// ...
						onlineResources.append("* [[");
						onlineResources.append(link.getUrl());
						onlineResources.append("|");
						onlineResources.append(linkTitle);
						onlineResources.append("]]\n");
					}
				}

				description += onlineResources.toString();
			}
		}

		// If no description were present in the metadata document, get the description from the capabilities document
		if (Utils.isBlank(description)) {
			description = layer.get_abstract();
		}

		// If a description has been found, either in the metadata or capabilities document, set it in the layerConfig.
		if (Utils.isNotBlank(description)) {
			layerConfig.setDescription(description);
		}


		layerConfig.setWmsQueryable(layer.isQueryable());

		if (Utils.isNotBlank(wmsPath)) {
			layerConfig.setWmsPath(wmsPath);
		}

		List<StyleImpl> styleImpls = layer.getStyles();
		if (styleImpls != null && !styleImpls.isEmpty()) {
			List<LayerStyleConfig> styles = new ArrayList<LayerStyleConfig>(styleImpls.size());
			for (StyleImpl styleImpl : styleImpls) {
				LayerStyleConfig styleConfig = styleToLayerStyleConfig(dataSourceConfig.getConfigManager(), styleImpl);

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

		// Link the layer to it's data source
		dataSourceConfig.bindLayer(layerConfig);

		return layerConfig;
	}

	private LayerStyleConfig styleToLayerStyleConfig(ConfigManager configManager, StyleImpl style) {
		String name = style.getName();
		InternationalString intTitle = style.getTitle();
		InternationalString intDescription = style.getAbstract();

		// style.getLegendURLs() is now implemented!! (gt-wms ver. 8.1) But there is an error in the code...
		// See modules/extension/wms/src/main/java/org/geotools/data/wms/xml/WMSComplexTypes.java line 4172
		// They use the hardcoded index "2" instead of "i":
		//     [...]
		//     if (sameName(elems[3], value[i])) {
		//         legendURLS.add((String)value[2].getValue());
		//     }
		//     [...]
		// Should be
		//     [...]
		//     if (sameName(elems[3], value[i])) {
		//         legendURLS.add((String)value[i].getValue());
		//     }
		//     [...]
		// http://osgeo-org.1560.n6.nabble.com/svn-r38810-in-branches-2-7-x-modules-extension-wms-src-main-java-org-geotools-data-wms-xml-test-javaa-tt4981882.html
		List<String> legendURLs = style.getLegendURLs();

		LayerStyleConfig styleConfig = new LayerStyleConfig(configManager);
		styleConfig.setName(name);
		if (intTitle != null) {
			styleConfig.setTitle(intTitle.toString());
		}
		if (intDescription != null) {
			styleConfig.setDescription(intDescription.toString());
		}

		/*
		if (legendURLs != null && !legendURLs.isEmpty()) {
			for (String legendURL : legendURLs) {
				System.out.println("############## legendURL: ["+legendURL+"]");
			}
		}
		*/

		return styleConfig;
	}
}
