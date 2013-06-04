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
import au.gov.aims.atlasmapperserver.xml.TC211.TC211Document;
import au.gov.aims.atlasmapperserver.xml.TC211.TC211Parser;
import au.gov.aims.atlasmapperserver.xml.WMTS.WMTSDocument;
import au.gov.aims.atlasmapperserver.xml.WMTS.WMTSParser;
import org.geotools.data.ows.CRSEnvelope;
import org.geotools.data.ows.Layer;
import org.geotools.data.ows.OperationType;
import org.geotools.data.ows.StyleImpl;
import org.geotools.data.ows.WMSCapabilities;
import org.geotools.data.ows.WMSRequest;
import org.geotools.data.wms.xml.MetadataURL;
import org.opengis.util.InternationalString;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractWMSLayerGenerator<L extends WMSLayerConfig, D extends WMSDataSourceConfig> extends AbstractLayerGenerator<L, D> {
	private static final Logger LOGGER = Logger.getLogger(AbstractWMSLayerGenerator.class.getName());

	private String wmsVersion = null;
	private URL featureRequestsUrl = null;
	private URL wmsServiceUrl = null;
	private URL legendUrl = null;
	private URL stylesUrl = null;

	public AbstractWMSLayerGenerator(D dataSource) {
		super(dataSource);
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
	public Collection<L> generateLayerConfigs(D dataSource, boolean harvest) throws Exception {
		Map<String, L> layersMap = this.generateRawLayerConfigs(dataSource, harvest);
		Collection<L> layers = null;

		if (layersMap != null && !layersMap.isEmpty()) {
			layers = new ArrayList<L>(layersMap.size());
			Map<String, L> cachedLayers = this.generateRawCachedLayerConfigs(dataSource, harvest);

			// Set cached flags
			for (Map.Entry<String, L> layerEntry : layersMap.entrySet()) {
				boolean cached = false;
				L layer = layerEntry.getValue();
				if (cachedLayers != null && cachedLayers.containsKey(layerEntry.getKey())) {
					L cachedLayer = cachedLayers.get(layerEntry.getKey());
					if (cachedLayer != null) {
						cached = true;
						this.setLayerStylesCacheFlag(layer.getStyles(), cachedLayer.getStyles());
					}
				}
				layer.setCached(cached);

				layers.add(layer);
			}
		}

		return layers;
	}

	private void setLayerStylesCacheFlag(List<LayerStyleConfig> layerStyles, List<LayerStyleConfig> cachedStyles) {
		if (layerStyles != null && !layerStyles.isEmpty()) {
			boolean cachedStyleNotEmpty = cachedStyles != null && !cachedStyles.isEmpty();

			for (LayerStyleConfig style : layerStyles) {
				boolean cached = false;
				boolean defaultStyle = style.isDefault() == null ? false : style.isDefault();

				if (defaultStyle) {
					cached = true;
				} else if (cachedStyleNotEmpty) {
					String styleName = style.getName();
					if (styleName != null) {
						for (LayerStyleConfig cachedStyle : cachedStyles) {
							String cachedStyleName = cachedStyle.getName();
							if (styleName.equals(cachedStyleName)) {
								cached = true;
							}
						}
					}
				}
				style.setCached(cached);
			}
		}
	}

	private Map<String, L> generateRawLayerConfigs(D dataSource, boolean harvest) throws Exception {
		WMSCapabilities wmsCapabilities = URLCache.getWMSCapabilitiesResponse(dataSource.getConfigManager(), dataSource, dataSource.getServiceUrl(), true, harvest);
		if (wmsCapabilities != null) {
			WMSRequest wmsRequestCapabilities = wmsCapabilities.getRequest();
			if (wmsRequestCapabilities != null) {
				this.featureRequestsUrl = this.getOperationUrl(wmsRequestCapabilities.getGetFeatureInfo());
				this.wmsServiceUrl = this.getOperationUrl(wmsRequestCapabilities.getGetMap());
				this.legendUrl = this.getOperationUrl(wmsRequestCapabilities.getGetLegendGraphic());
				this.wmsVersion = wmsCapabilities.getVersion();

				// GetStyles URL is in GeoTools API but not in the Capabilities document.
				//     GeoTools probably craft the URL. It's not very useful.
				//this.stylesUrl = this.getOperationUrl(wmsRequestCapabilities.getGetStyles());
			}

			return this.getLayersInfoFromCaps(wmsCapabilities, dataSource, harvest);
		}

		return null;
	}

	private Map<String, L> generateRawCachedLayerConfigs(D dataSource, boolean harvest) throws Exception {
		// When the webCacheEnable checkbox is unchecked, no layers are cached.
		if (dataSource.isWebCacheEnable() == null || dataSource.isWebCacheEnable() == false) {
			return null;
		}

		WMTSDocument gwcCapabilities = this.getGWCDocument(dataSource.getConfigManager(), dataSource, harvest);

		Map<String, L> layerConfigs = new HashMap<String, L>();

		if (gwcCapabilities != null) {
			// http://docs.geotools.org/stable/javadocs/org/geotools/data/wms/WebMapServer.html
			Layer rootLayer = gwcCapabilities.getLayer();

			if (rootLayer != null) {
				// The boolean at the end is use to ignore the root from the capabilities document. It can be added (change to false) if some users think it's useful to see the root...
				this._propagateLayersInfoMapFromGeoToolRootLayer(layerConfigs, rootLayer, new LinkedList<String>(), dataSource, true, harvest);
			}
		}

		return layerConfigs;
	}

	/**
	 * Try to find and parse the WMTS document associated with this WMS service.
	 * Algo:
	 *     Get GWC Capabilities Document URL  or  craft it from the WMS URL.
	 *     Get GWC URL  or  craft it from the WMS URL.
	 *         NOTE: This URL is used by the generated client only.
	 *         IMPORTANT: Ideally, this method would parse the GWC WMS Cap doc instead of the WMTS doc and
	 *             get the GWC URL from it. Unfortunately, the WMS Cap doc from GWC do not contains any info
	 *             about cached styles.
	 *     Try to parse it as a WMTS capabilities document.
	 *     If that didn't work, try to rectify the WMTS capabilities document URL and try again.
	 *     If that didn't work, return null and add an error.
	 */
	public WMTSDocument getGWCDocument(ConfigManager configManager, D dataSource, boolean harvest) {
		// GWC service is not mandatory; failing to parse this won't cancel the generation of the client.
		boolean gwcMandatory = false;

		// No WMS service = no need to cache anything.
		if (this.wmsServiceUrl == null) {
			return null;
		}

		// Used to craft GWC URL
		String gwcSubPath = "gwc";
		URL gwcBaseURL = null;

		// Message from the most explicit exception (hopefully), to be add to the error sent back to the admin.
		String exceptionMessage = null;

		// Try to resolve the GWC URL, assuming it works just like GeoServer
		//     We have WMS URL: http://domain.com:80/geoserver/ows?SERVICE=WMS&amp;
		//     We want GWC URL: http://domain.com:80/geoserver/gwc/
		try {
			// baseURL = http://domain.com:80/geoserver/
			// From the WMS Service URL provided by the capabilities document
			URL baseURL = new URL(this.wmsServiceUrl.getProtocol(), this.wmsServiceUrl.getHost(), this.wmsServiceUrl.getPort(), this.wmsServiceUrl.getPath());

			// gwcBaseURL = http://domain.com:80/geoserver/gwc/
			gwcBaseURL = new URL(baseURL, gwcSubPath+"/");
		} catch (Exception ex) {
			// Error occurred while crafting the GWC URL. This is unlikely to happen.
			LOGGER.log(Level.FINE, "Fail to craft a GWC URL using the WMS URL", ex);
		}

		// Get GWC URL or craft it from WMS URL
		URL gwcUrl = null;
		String gwcUrlStr = dataSource.getWebCacheUrl();
		try {
			if (gwcUrlStr == null || gwcUrlStr.isEmpty()) {
				if (gwcBaseURL != null) {
					gwcUrl = new URL(gwcBaseURL, "service/wms");
					dataSource.setWebCacheUrl(gwcUrl.toString());
				}
			}
		} catch (Exception ex) {
			// This should not happen
			LOGGER.log(Level.WARNING, "Fail craft the GWC URL.", ex);
		}

		// Get GWC Capabilities Document URL or craft it from WMS URL
		URL gwcCapUrl = null;
		File gwcCapFile = null;
		String gwcCapUrlStr = dataSource.getWebCacheCapabilitiesUrl();
		try {
			if (gwcCapUrlStr == null || gwcCapUrlStr.isEmpty()) {
				if (gwcBaseURL != null) {
					gwcCapUrl = new URL(gwcBaseURL, "service/wmts?REQUEST=getcapabilities");
				}
			} else {
				if (gwcCapUrlStr.startsWith("file://")) {
					// Local file URL
					gwcCapFile = new File(new URI(gwcCapUrlStr));
				} else {
					gwcCapUrl = new URL(gwcCapUrlStr);
				}
			}
		} catch (Exception ex) {
			// This should not happen
			LOGGER.log(Level.WARNING, "Fail craft the GWC Capabilities Document URL.", ex);
		}


		// Parsing of the GWC cap doc (WMTS)
		WMTSDocument document = null;

		if (gwcCapUrl == null && gwcCapFile == null) {
			exceptionMessage = "Can not determine the GWC Capabilities document URL.";
		} else {
			try {
				if (gwcCapFile != null) {
					document = WMTSParser.parseFile(gwcCapFile, gwcCapUrlStr);
				} else {
					document = WMTSParser.parseURL(configManager, dataSource, gwcCapUrl, gwcMandatory, harvest);
				}
			} catch (Exception ex) {
				// This happen every time the admin set a GWC base URL instead of a WMTS capabilities document.
				// The next block try to work around this by crafting a WMTS URL.
				LOGGER.log(Level.FINE, "Fail to parse the given GWC URL as a WMTS capabilities document.", ex);
				exceptionMessage = ex.getMessage();
			}

			// Try to add some parameters to the given GWC cap url (the provided URL may be incomplete, something like http://domain.com:80/geoserver/gwc/service/wmts)
			if (document == null) {
				// Add a slash a the end of the URL, just in case the URL ends like this: .../geoserver/gwc
				String urlPath = gwcCapUrl.getPath() + "/";
				// Look for "/gwc/"
				int gwcIndex = urlPath.indexOf("/"+gwcSubPath+"/");
				if (gwcIndex >= 0) {
					try {
						// Remove everything after "/gwc/"
						URL modifiedGwcBaseURL = new URL(gwcCapUrl.getProtocol(), gwcCapUrl.getHost(), gwcCapUrl.getPort(), urlPath.substring(0, gwcIndex + gwcSubPath.length() + 2));
						// Add WMTS URL part
						URL modifiedGwcCapUrl = new URL(modifiedGwcBaseURL, "service/wmts?REQUEST=getcapabilities");
						// Try to download the doc again
						document = WMTSParser.parseURL(configManager, dataSource, modifiedGwcCapUrl, gwcMandatory, harvest);

						if (document != null) {
							// If it works, save the crafted URL
							gwcCapUrl = modifiedGwcCapUrl;
						}
					} catch (Exception ex) {
						// Error occurred while crafting the GWC URL. This is unlikely to happen.
						LOGGER.log(Level.FINE, "Fail to craft a GWC URL using the given GWC URL", ex);
						exceptionMessage = ex.getMessage();
					}
				}
			}
		}

		if (document == null) {
			if (gwcCapUrlStr != null && !gwcCapUrlStr.isEmpty()) {
				String errorMsg = "Could not find a valid WMTS capability document at the given GWC URL. " +
						"Assuming all layers are cached. If the caching feature do not work properly, " +
						"disable it in the data source config and report a bug." +
						(exceptionMessage == null ? "" : "\nError: " + exceptionMessage);
				if (gwcMandatory) {
					this.addError(errorMsg);
				} else {
					this.addWarning(errorMsg);
				}
			}
		} else if (gwcCapUrlStr == null || gwcCapUrlStr.isEmpty()) {
			dataSource.setWebCacheCapabilitiesUrl(gwcCapUrl.toString());
		}

		return document;
	}

	private URL getOperationUrl(OperationType op) {
		if (op == null) {
			return null;
		}
		return op.getGet();
	}

	/**
	 * @param wmsCapabilities
	 * @param dataSourceConfig
	 * @param harvest True do download associated metadata documents (TC211), false to use the cached ones.
	 * @return
	 */
	private Map<String, L> getLayersInfoFromCaps(
			WMSCapabilities wmsCapabilities,
			D dataSourceConfig, // Data source of layers (to link the layer to its data source)
			boolean harvest
	) {
		if (wmsCapabilities == null) {
			return null;
		}

		// http://docs.geotools.org/stable/javadocs/org/geotools/data/wms/WebMapServer.html
		Layer rootLayer = wmsCapabilities.getLayer();

		Map<String, L> layerConfigs = new HashMap<String, L>();
		// The boolean at the end is use to ignore the root from the capabilities document. It can be added (change to false) if some users think it's useful to see the root...
		this._propagateLayersInfoMapFromGeoToolRootLayer(layerConfigs, rootLayer, new LinkedList<String>(), dataSourceConfig, true, harvest);

		// Set default styles
		for (L layer : layerConfigs.values()) {
			List<LayerStyleConfig> styles = layer.getStyles();
			if (styles != null && !styles.isEmpty()) {
				styles.get(0).setDefault(true);
			}
		}

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
	//     xmlLayers = getXmlLayersFromGeoToolRootLayer(xmlLayers, childLayer, childrenPath);
	// give the same result as
	//     getXmlLayersFromGeoToolRootLayer(xmlLayers, childLayer, childrenPath);
	// The first one is just visualy more easy to understand.
	private void _propagateLayersInfoMapFromGeoToolRootLayer(
			Map<String, L> layerConfigs,
			Layer layer,
			List<String> treePath,
			D dataSourceConfig,
			boolean isRoot,
			boolean harvest) {

		if (layer == null) {
			return;
		}

		// GeoTools API documentation is hopeless. The easiest way to know what the API do is to look at the sources:
		// http://svn.osgeo.org/geotools/trunk/modules/extension/wms/src/main/java/org/geotools/data/ows/Layer.java
		List<Layer> children = layer.getLayerChildren();

		if (children != null && !children.isEmpty()) {
			// The layer has children, so it is a Container;
			// If it's not the root, it's a part of the WMS Path, represented as a folder in the GUI.
			List<String> childrenTreePath = new LinkedList<String>(treePath);
			if (!isRoot) {
				String newTreePathPart = layer.getTitle();
				if (Utils.isBlank(newTreePathPart)) {
					newTreePathPart = layer.getName();
				}

				if (Utils.isNotBlank(newTreePathPart)) {
					childrenTreePath.add(newTreePathPart);
				}
			}

			for (Layer childLayer : children) {
				this._propagateLayersInfoMapFromGeoToolRootLayer(layerConfigs, childLayer, childrenTreePath, dataSourceConfig, false, harvest);
			}
		} else {
			// The layer do not have any children, so it is a real layer

			// Create a string representing the path
			StringBuilder treePathBuf = new StringBuilder();
			for (String treePathPart : treePath) {
				if (Utils.isNotBlank(treePathPart)) {
					if (treePathBuf.length() > 0) {
						treePathBuf.append("/");
					}
					treePathBuf.append(treePathPart);
				}
			}

			L layerConfig = this.layerToLayerConfig(layer, treePathBuf.toString(), dataSourceConfig, harvest);
			if (layerConfig != null) {
				layerConfigs.put(layerConfig.getLayerId(), layerConfig);
			}
		}
	}

	/**
	 * Convert a GeoTool Layer into a AtlasMapper Layer
	 * @param layer
	 * @param treePath
	 * @param dataSourceConfig
	 * @return
	 */
	private L layerToLayerConfig(
			Layer layer,
			String treePath,
			D dataSourceConfig,
			boolean harvest) {

		L layerConfig = this.createLayerConfig(dataSourceConfig.getConfigManager());

		String layerName = layer.getName();
		if (Utils.isBlank(layerName)) {
			LOGGER.log(Level.WARNING, "The Capabilities Document of the data source [{0}] contains layers without name (other than the root layer).", dataSourceConfig.getDataSourceName());
			return null;
		}

		TC211Document tc211Document = null;
		List<MetadataURL> metadataUrls = layer.getMetadataURL();
		if (metadataUrls != null && !metadataUrls.isEmpty()) {
			for (MetadataURL metadataUrl : metadataUrls) {
				if (tc211Document == null && "TC211".equalsIgnoreCase(metadataUrl.getType()) && "text/xml".equalsIgnoreCase(metadataUrl.getFormat())) {
					URL url = metadataUrl.getUrl();
					if (url != null) {
						try {
							tc211Document = TC211Parser.parseURL(dataSourceConfig.getConfigManager(), dataSourceConfig, url, false, harvest);
							if (tc211Document == null || tc211Document.isEmpty()) { tc211Document = null; }
						} catch (Exception e) {
							LOGGER.log(Level.SEVERE, "Unexpected exception while parsing the metadata document URL: {0}\n" +
									"The information provided by the GetCapabilities document indicate that the file is a " +
									"TC211 text/xml file, which seems to not be the case: {1}",
									new String[] { url.toString(), Utils.getExceptionMessage(e) });
							LOGGER.log(Level.FINE, "Stack trace: ", e);
						}
					}
				}
			}

			// There is metadata URL, but none of the one set with TC211 text/xml format are suitable.
			// Sometime, there is valid metadata URL but they have been entered incorrectly.
			// Brute force through all metadata URL and cross fingers to find one that will provide some usable info.
			if (tc211Document == null) {
				LOGGER.log(Level.FINE, "BRUTE FORCE: Could not find a valid TC211 text/xml metadata document for layer {0} of {1}. Try them all whatever their specified mime type.",
						new String[]{ layerName, dataSourceConfig.getDataSourceName() });
				MetadataURL validMetadataUrl = null;
				for (MetadataURL metadataUrl : metadataUrls) {
					if (tc211Document == null) {
						URL url = metadataUrl.getUrl();
						if (url != null) {
							try {
								tc211Document = TC211Parser.parseURL(dataSourceConfig.getConfigManager(), dataSourceConfig, url, false, harvest);
								if (tc211Document != null && !tc211Document.isEmpty()) {
									validMetadataUrl = metadataUrl;
								} else {
									LOGGER.log(Level.FINE, "FAILURE: Invalid metadata document: {0}\n      Identified as \"{1} - {2}\"", new String[]{
											url.toString(),
											metadataUrl.getType(),
											metadataUrl.getFormat()
									});
									tc211Document = null;
								}
							} catch (Exception ex) {
								LOGGER.log(Level.FINE, "FAILURE: Invalid metadata document: {0}\n      Identified as \"{1} - {2}\"\n      Exception message: {3}", new String[]{
										url.toString(),
										metadataUrl.getType(),
										metadataUrl.getFormat(),
										ex.getMessage()
								});
							}
						}
					}
				}
				if (tc211Document != null && validMetadataUrl != null) {
					LOGGER.log(Level.FINE, "SUCCESS: Valid metadata document: {0}\n      Identified as \"{1} - {2}\"", new String[]{
							validMetadataUrl.getUrl().toString(),
							validMetadataUrl.getType(),
							validMetadataUrl.getFormat()
					});
				}
			}
		}

		layerConfig.setLayerId(layerName);
		this.ensureUniqueLayerId(layerConfig, dataSourceConfig);
		layerName = layerConfig.getLayerName();

		String title = layer.getTitle();

		// Build the description using info found in the Capabilities document and the MEST document.
		StringBuilder descriptionSb = new StringBuilder();

		TC211Document.Link layerLink = this.getMetadataLayerLink(tc211Document, layerName);

		String layerDescription = layer.get_abstract();
		String metadataDescription = tc211Document == null ? null : tc211Document.getAbstract();
		String metadataLayerDescription = layerLink == null ? null : layerLink.getDescription();
		String metadataLinksWikiFormat = this.getMetadataLinksWikiFormat(tc211Document);

		// Clean-up
		if (layerDescription != null) { layerDescription = layerDescription.trim(); }
		if (metadataDescription != null) { metadataDescription = metadataDescription.trim(); }
		if (metadataLayerDescription != null) { metadataLayerDescription = metadataLayerDescription.trim(); }
		if (metadataLinksWikiFormat != null) { metadataLinksWikiFormat = metadataLinksWikiFormat.trim(); }

		// Layer description: Get from cap doc, or from MEST if cap doc do not have one.
		if (layerDescription != null && !layerDescription.isEmpty()) {
			descriptionSb.append(layerDescription);
		} else if (metadataLayerDescription != null && !metadataLayerDescription.isEmpty()) {
			descriptionSb.append(metadataLayerDescription);
		}

		if (metadataDescription != null && !metadataDescription.isEmpty()) {
			if (descriptionSb.length() > 0) {
				descriptionSb.append("\n\n*Dataset description*\n");
			}
			descriptionSb.append(metadataDescription);
		}

		// If cap doc has a description, the layer description from the MEST will appear here.
		if (layerDescription != null && !layerDescription.isEmpty() &&
				metadataLayerDescription != null && !metadataLayerDescription.isEmpty()) {
			if (descriptionSb.length() > 0) {
				descriptionSb.append("\n\n*Dataset layer description*\n");
			}
			descriptionSb.append(metadataLayerDescription);
		}

		if (metadataLinksWikiFormat != null && !metadataLinksWikiFormat.isEmpty()) {
			if (descriptionSb.length() > 0) {
				descriptionSb.append("\n\n");
			}
			descriptionSb.append("*Online resources*\n");
			descriptionSb.append(metadataLinksWikiFormat);
		}

		if (Utils.isNotBlank(title)) {
			layerConfig.setTitle(title);
		}

		// If a description has been found, either in the metadata or capabilities document, set it in the layerConfig.
		if (descriptionSb.length() > 0) {
			layerConfig.setDescription(descriptionSb.toString());
		}

		if (metadataLinksWikiFormat != null && !metadataLinksWikiFormat.isEmpty()) {
			layerConfig.setDownloadLinks(metadataLinksWikiFormat);
		}

		layerConfig.setWmsQueryable(layer.isQueryable());

		if (Utils.isNotBlank(treePath)) {
			layerConfig.setTreePath(treePath);
		}

		List<StyleImpl> styleImpls = layer.getStyles();
		if (styleImpls != null && !styleImpls.isEmpty()) {
			List<LayerStyleConfig> styles = new ArrayList<LayerStyleConfig>(styleImpls.size());
			for (StyleImpl styleImpl : styleImpls) {
				LayerStyleConfig styleConfig = this.styleToLayerStyleConfig(dataSourceConfig.getConfigManager(), styleImpl);
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

	private TC211Document.Link getMetadataLayerLink(TC211Document tc211Document, String layerName) {
		if (tc211Document != null) {
			List<TC211Document.Link> links = tc211Document.getLinks();
			if (links != null && !links.isEmpty()) {
				for (TC211Document.Link link : links) {
					// Only display links with none null URL.
					if (Utils.isNotBlank(link.getUrl())) {
						TC211Document.Protocol linkProtocol = link.getProtocol();
						if (linkProtocol != null) {
							if (linkProtocol.isOGC()) {
								// If the link is a OGC url (most likely WMS GetMap) and the url match the layer url, parse its description.
								if (layerName.equalsIgnoreCase(link.getName())) {
									return link;
								}
							}
						}
					}
				}
			}
		}
		return null;
	}

	private String getMetadataLinksWikiFormat(TC211Document tc211Document) {
		StringBuilder onlineResources = null;

		if (tc211Document != null) {
			// Add links found in the metadata document and layer description (if any)
			List<TC211Document.Link> links = tc211Document.getLinks();
			if (links != null && !links.isEmpty()) {
				// Set the Online Resources, using wiki format
				onlineResources = new StringBuilder();
				for (TC211Document.Link link : links) {
					// Only display links with none null URL.
					if (Utils.isNotBlank(link.getUrl())) {
						TC211Document.Protocol linkProtocol = link.getProtocol();
						if (linkProtocol != null) {
							if (linkProtocol.isWWW()) {
								// Dataset links such as point of truth, data download, etc.
								String linkUrl = link.getUrl();
								String linkTitle = link.getDescription();
								if (Utils.isBlank(linkTitle)) {
									linkTitle = link.getName();
								}

								// Clean-up the URL, to be sure it will be parsable by the wiki format parser.
								// NOTE: URLEncoder can not be used here since it would also encode parts that
								//     should not be encoded (like the "http://")
								if (Utils.isNotBlank(linkUrl)) {
									linkUrl = linkUrl
											// Replace square brackets with their URL encoding.
											//     (they may cause problem with the wiki format parser)
											.replace("[", "%5B").replace("]", "%5D")
											// Replace pipe with its URL encoding.
											//     (it may cause problem with the wiki format parser)
											.replace("|", "%7C")
											// Encore space using its URL encoding.
											// NOTE: "%20" is safer than "+" since the "+" is only valid in the
											//     query string (the part after the ?).
											.replace(" ", "%20")
											// Other white spaces should not be present in a URL anyway.
											.replace("\\s", "");
								}

								// Clean-up the title, to be sure it will be parsable by the wiki format parser.
								if (Utils.isNotBlank(linkTitle)) {
									linkTitle = linkTitle
											// Replace square brackets with their HTML entity.
											//     (they may cause problem with the wiki format parser)
											.replace("[", "&#91;").replace("]", "&#93;")
											// Replace pipe with its HTML entity.
											//     (it may cause problem with the wiki format parser)
											.replace("|", "&#124;")
											// Replace chain of whitespaces with one space.
											//     (newline in a link cause the wiki format parser to fail)
											.replaceAll("\\s+", " ");
								}

								// Bullet list of URLs, in Wiki format:
								// * [[url|title]]
								// * [[url|title]]
								// * [[url|title]]
								// ...
								onlineResources.append("* [[");
								onlineResources.append(linkUrl);
								if (Utils.isNotBlank(linkTitle)) {
									onlineResources.append("|");
									onlineResources.append(linkTitle);
								}
								onlineResources.append("]]\n");
							}
						}
					}
				}
			}
		}

		return onlineResources == null ? null : onlineResources.toString();
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
		//List<String> legendURLs = style.getLegendURLs();

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
