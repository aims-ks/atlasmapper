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
import org.geotools.data.ows.Service;
import org.geotools.data.ows.StyleImpl;
import org.geotools.data.ows.WMSCapabilities;
import org.geotools.data.ows.WMSRequest;
import org.geotools.data.wms.xml.MetadataURL;
import org.json.JSONException;
import org.json.JSONObject;
import org.opengis.util.InternationalString;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

	private WMTSDocument gwcCapabilities = null;
	private URL gwcServiceUrl = null;

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
		this.wmsCapabilities = URLCache.getWMSCapabilitiesResponse(dataSource.getConfigManager(), dataSource, dataSource.getServiceUrl(), true, harvest);
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

		return this.getLayersInfoFromCaps(dataSource, harvest);
	}

	@Override
	public Collection<L> generateCachedLayerConfigs(D dataSource, boolean harvest) throws Exception {
		String gwcUrl = dataSource.getWebCacheUrl();
		if (gwcUrl != null && !gwcUrl.isEmpty()) {
			this.gwcServiceUrl = new URL(gwcUrl);
		}
		this.gwcCapabilities = this.getGWCDocument(dataSource.getConfigManager(), dataSource, harvest);

		Collection<L> layerConfigs = new ArrayList<L>();

		if (this.gwcCapabilities != null) {
			// http://docs.geotools.org/stable/javadocs/org/geotools/data/wms/WebMapServer.html
			Layer rootLayer = this.gwcCapabilities.getLayer();

			if (rootLayer != null) {
				// The boolean at the end is use to ignore the root from the capabilities document. It can be added (change to false) if some users think it's useful to see the root...
				this._getLayersInfoFromGeoToolRootLayer(layerConfigs, rootLayer, new LinkedList<String>(), dataSource, true, harvest);
			}
		}

		return layerConfigs;
	}

	/**
	 * Try to find and parse the WMTS document associated with this WMS service.
	 * Algo:
	 *     If a GWC URL is provided
	 *         Try to parse it as a WMTS capabilities document.
	 *         If that didn't work, try to craft a WMTS URL from the GWC URL and parse it.
	 *         If that didn't work, return null and add an error
	 *     Else
	 *         Try to craft a WMTS URL from the WMS URL, assuming it follows GeoServer standards.
	 *         If that didn't work, return null
	 */
	public WMTSDocument getGWCDocument(ConfigManager configManager, D dataSource, boolean harvest) {
		boolean gwcMandatory = false;
		String gwcSubPath = "gwc";
		URL gwcBaseURL = null;

		// Message from the most explicit exception (hopefully), to be add to the error sent back to the admin.
		String exceptionMessage = null;

		WMTSDocument document = null;
		if (this.gwcServiceUrl != null) {
			URL gwcUrl = this.gwcServiceUrl;
			try {
				document = WMTSParser.parseURL(configManager, dataSource, gwcUrl, gwcMandatory, harvest);
			} catch (Exception ex) {
				// This happen every time the admin set a GWC base URL instead of a WMTS capabilities document.
				// The next block try to work around this by crafting a WMTS URL.
				LOGGER.log(Level.FINE, "Fail to parse the given GWC URL as a WMTS capabilities document.", ex);
				exceptionMessage = ex.getMessage();
			}
			if (document != null) {
				// The given GWC URL was pointing at a valid WMTS document.
				return document;
			}

			// Try to add some parameters
			String urlPath = gwcUrl.getPath() + "/"; // Add a slash a the end, just in case the URL ends like this: .../geoserver/gwc
			int gwcIndex = urlPath.indexOf("/"+gwcSubPath+"/");
			if (gwcIndex >= 0) {
				// Remove everything after /gwc/
				try {
					gwcBaseURL = new URL(gwcUrl.getProtocol(), gwcUrl.getHost(), gwcUrl.getPort(), urlPath.substring(0, gwcIndex + gwcSubPath.length() + 2));
				} catch (Exception ex) {
					// Error occurred while crafting the GWC URL. This is unlikely to happen.
					LOGGER.log(Level.FINE, "Fail to craft a GWC URL using the given GWC URL", ex);
				}
			}

		} else {
			// Try to resolve the GWC URL, assuming it works just like GeoServer
			//     We have WMS URL: http://domain.com:80/geoserver/ows?SERVICE=WMS&amp;
			//     We want GWC URL: http://domain.com:80/geoserver/gwc/
			URL gwcUrl = this.wmsServiceUrl;
			if (gwcUrl != null) {
				try {
					// baseURL = http://domain.com:80/geoserver/
					URL baseURL = new URL(gwcUrl.getProtocol(), gwcUrl.getHost(), gwcUrl.getPort(), gwcUrl.getPath());

					// gwcBaseURL = http://domain.com:80/geoserver/gwc/
					gwcBaseURL = new URL(baseURL, gwcSubPath+"/");
				} catch (Exception ex) {
					// Error occurred while crafting the GWC URL. This is unlikely to happen.
					LOGGER.log(Level.FINE, "Fail to craft a GWC URL using the WMS URL", ex);
				}
			}
		}

		if (gwcBaseURL != null) {
			// At this point, we should have something like:
			//     http://domain.com:80/geoserver/gwc/
			// Assuming the service follows GeoServer "standards", we can build the URL by adding:
			//     service/wmts?REQUEST=getcapabilities
			// Result:
			//     http://domain.com:80/geoserver/gwc/service/wmts?REQUEST=getcapabilities
			// If the service have a GWC but is not following GeoServer standards, the admin can always provide a
			// complete WMTS URL to the capabilities document.
			URL gwcUrl = null;
			try {
				gwcUrl = new URL(gwcBaseURL, "service/wmts?REQUEST=getcapabilities");
				document = WMTSParser.parseURL(configManager, dataSource, gwcUrl, gwcMandatory, harvest);
			} catch (Exception ex) {
				LOGGER.log(Level.FINE, "Fail to parse the crafted WMTS URL as a WMTS capabilities document.\n" +
						"WMTS URL: " + (gwcUrl == null ? "NULL" : gwcUrl.toString()), ex);
				if (exceptionMessage == null) { exceptionMessage = ex.getMessage(); }
			}
		}

		// Error: Could not get any GWC capabilities document, but a URL as been provided by the admin.
		if (this.gwcServiceUrl != null && document == null) {
			this.addError(dataSource.getDataSourceId(),
				"Could not find a valid WMTS capability document at the given GWC URL. Assuming all layers are cached. If the caching feature do not work properly, disable it in the data source config and report a bug." +
						(exceptionMessage == null ? "" : "\nError: " + exceptionMessage));
		}

		// Return the document. Note that it may be NULL if the parser didn't managed to parse it.
		return document;
	}

	private URL getOperationUrl(OperationType op) {
		if (op == null) {
			return null;
		}
		return op.getGet();
	}

	/**
	 * @param dataSourceConfig
	 * @param harvest True do download associated metadata documents (TC211), false to use the cached ones.
	 * @return
	 */
	private Collection<L> getLayersInfoFromCaps(
			D dataSourceConfig, // Data source of layers (to link the layer to its data source)
			boolean harvest
	) {
		if (this.wmsCapabilities == null) {
			return null;
		}

		// http://docs.geotools.org/stable/javadocs/org/geotools/data/wms/WebMapServer.html
		Layer rootLayer = this.wmsCapabilities.getLayer();

		Collection<L> layerConfigs = new ArrayList<L>();
		// The boolean at the end is use to ignore the root from the capabilities document. It can be added (change to false) if some users think it's useful to see the root...
		this._getLayersInfoFromGeoToolRootLayer(layerConfigs, rootLayer, new LinkedList<String>(), dataSourceConfig, true, harvest);

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
				this._getLayersInfoFromGeoToolRootLayer(layerConfigs, childLayer, childrenWmsPath, dataSourceConfig, false, harvest);
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

			L layerConfig = this.layerToLayerConfig(layer, wmsPathBuf.toString(), dataSourceConfig, harvest);
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
			D dataSourceConfig,
			boolean harvest) {

		L layerConfig = this.createLayerConfig(dataSourceConfig.getConfigManager());

		String layerName = layer.getName();
		if (Utils.isBlank(layerName)) {
			String serviceTitle = "unknown";
			Service service = this.wmsCapabilities.getService();
			if (service != null) {
				serviceTitle = service.getTitle();
			}

			LOGGER.log(Level.WARNING, "The Capabilities Document [{0}] contains layers without name (other than the root layer).", serviceTitle);
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
		String description = null;
		JSONObject mestOverrides = null;

		// Get the description from the metadata document, if available
		if (tc211Document != null) {
			description = tc211Document.getAbstract();

			List<TC211Document.Link> links = tc211Document.getLinks();
			if (links != null && !links.isEmpty()) {
				// Set the Online Resources, using wiki format
				StringBuilder onlineResources = new StringBuilder("\n\n*Online Resources*\n");
				for (TC211Document.Link link : links) {
					// Only display links with none null URL.
					if (Utils.isNotBlank(link.getUrl())) {
						TC211Document.Protocol linkProtocol = link.getProtocol();
						if (linkProtocol != null) {
							if (linkProtocol.isOGC()) {
								// If the link is a OGC url (most likely WMS GetMap) and the url match the layer url, parse its description.
								if (layerName.equalsIgnoreCase(link.getName())) {
									String applicationProfileStr = link.getApplicationProfile();
									if (applicationProfileStr != null && !applicationProfileStr.isEmpty()) {
										mestOverrides = TC211Parser.parseMestApplicationProfile(applicationProfileStr);
									}

									String layerDescription = link.getDescription();
									// The layer description found in the MEST is added to the description of the AtlasMapper layer.
									// The description may also specified a title for the layer, and other attributes,
									// using the following format:
									//     Title: Coral sea Plateau
									//     Description: Plateau is a flat or nearly flat area...
									//     Subcategory: 2. GBRMPA features
									Map<String, StringBuilder> parsedDescription = TC211Parser.parseMestDescription(layerDescription);

									// The layer title is replace with the title from the MEST link description.
									if (parsedDescription.containsKey(TC211Parser.TITLE_KEY) && parsedDescription.get(TC211Parser.TITLE_KEY) != null) {
										String titleStr = parsedDescription.get(TC211Parser.TITLE_KEY).toString().trim();
										if (!titleStr.isEmpty()) {
											title = titleStr;
										}
									}

									// The description found in the MEST link description (i.e. Layer description) is added
									// at the beginning of the layer description (with a "Dataset description" label to
									//     divide the layer description from the rest).
									if (parsedDescription.containsKey(TC211Parser.DESCRIPTION_KEY) && parsedDescription.get(TC211Parser.DESCRIPTION_KEY) != null) {
										StringBuilder descriptionSb = parsedDescription.get(TC211Parser.DESCRIPTION_KEY);
										if (descriptionSb.length() > 0) {
											if (description != null && !description.isEmpty()) {
												descriptionSb.append(TC211Parser.NL); descriptionSb.append(TC211Parser.NL);
												descriptionSb.append("*Dataset description*"); descriptionSb.append(TC211Parser.NL);
												descriptionSb.append(description);
											}
											description = descriptionSb.toString().trim();
										}
									}

									// The path found in the MEST link description override the WMS path in the layer.
									if (parsedDescription.containsKey(TC211Parser.PATH_KEY) && parsedDescription.get(TC211Parser.PATH_KEY) != null) {
										String pathStr = parsedDescription.get(TC211Parser.PATH_KEY).toString().trim();
										if (pathStr.isEmpty()) {
											pathStr = null;
										}
										wmsPath = pathStr;
									}
								}
							} else if (linkProtocol.isWWW()) {
								// Dataset links such as point of truth, data download, etc.
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
					}
				}

				description += onlineResources.toString();
			}
		}

		if (Utils.isNotBlank(title)) {
			layerConfig.setTitle(title);
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
				LayerStyleConfig styleConfig = styleToLayerStyleConfig(dataSourceConfig.getConfigManager(), layerName, styleImpl);

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

		// Apply layer overrides found in the MEST
		if (mestOverrides != null && mestOverrides.length() > 0) {
			try {
				layerConfig = (L)layerConfig.applyOverrides(mestOverrides);
			} catch (JSONException e) {
				LOGGER.log(Level.SEVERE, "Unable to apply layer overrides found in the application profile field of the MEST server for layer id \"{0}\": {1}",
						new String[]{ layerConfig.getLayerName(), Utils.getExceptionMessage(e) });
				LOGGER.log(Level.FINE, "Stack trace: ", e);
			}
		}

		return layerConfig;
	}

	private LayerStyleConfig styleToLayerStyleConfig(ConfigManager configManager, String layerName, StyleImpl style) {
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
