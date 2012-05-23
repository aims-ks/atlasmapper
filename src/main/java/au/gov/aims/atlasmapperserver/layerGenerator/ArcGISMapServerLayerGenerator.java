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
import au.gov.aims.atlasmapperserver.URLCache;
import au.gov.aims.atlasmapperserver.dataSourceConfig.ArcGISMapServerDataSourceConfig;
import au.gov.aims.atlasmapperserver.Utils;
import au.gov.aims.atlasmapperserver.layerConfig.AbstractLayerConfig;
import au.gov.aims.atlasmapperserver.layerConfig.ArcGISCacheLayerConfig;
import au.gov.aims.atlasmapperserver.layerConfig.ArcGISMapServerLayerConfig;
import au.gov.aims.atlasmapperserver.layerConfig.FolderLayerConfig;
import au.gov.aims.atlasmapperserver.layerConfig.GroupLayerConfig;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opengis.referencing.NoSuchAuthorityCodeException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

// ArcGIS API:
// http://services.arcgisonline.com/ArcGIS/SDK/REST/index.html?mapserver.html
public class ArcGISMapServerLayerGenerator extends AbstractLayerGenerator<AbstractLayerConfig, ArcGISMapServerDataSourceConfig> {
	private static final Logger LOGGER = Logger.getLogger(ArcGISMapServerLayerGenerator.class.getName());

	/**
	 * ArcGIS server have a LOT of layer ID duplications. The server assume that the client
	 * will call the layers using distinct URLs for each folders. The path has to be added
	 * to the layer ID to ensure uniqueness.
	 * @param layer
	 * @param dataSourceConfig
	 * @return
	 */
	@Override
	protected String getUniqueLayerId(AbstractLayerConfig layer, ArcGISMapServerDataSourceConfig dataSourceConfig) {
		StringBuilder layerUniqueId = new StringBuilder();
		String arcGISPath = null;

		if (layer instanceof ArcGISMapServerLayerConfig) {
			arcGISPath = ((ArcGISMapServerLayerConfig)layer).getArcGISPath();
		} else if (layer instanceof FolderLayerConfig) {
			arcGISPath = ((FolderLayerConfig)layer).getFolderPath();
		} else if (layer instanceof GroupLayerConfig) {
			arcGISPath = ((GroupLayerConfig)layer).getGroupPath();
		}

		if (Utils.isNotBlank(arcGISPath)) {
			layerUniqueId.append(arcGISPath);
			layerUniqueId.append("/");
		}

		// Add the layer ID and the layer title for readability
		//     I.E. Animals/0_Turtle
		// NOTE: Only add those for layers (not folders)
		if (!"FOLDER".equals(layer.getDataSourceType())) {
			layerUniqueId.append(layer.getLayerId());
			layerUniqueId.append("_");
			layerUniqueId.append(layer.getTitle());
		}
		return layerUniqueId.toString().trim();
	}

	@Override
	public Map<String, AbstractLayerConfig> generateLayerConfigs(ClientConfig clientConfig, ArcGISMapServerDataSourceConfig dataSourceConfig) throws Exception {
		if (dataSourceConfig == null) {
			throw new IllegalArgumentException("ArcGIS Map Server generation requested for a null data source.");
		}

		Map<String, AbstractLayerConfig> layers = new HashMap<String, AbstractLayerConfig>();
		parseJSON(layers, null, null, dataSourceConfig);
		return layers;
	}

	@Override
	public ArcGISMapServerDataSourceConfig applyOverrides(ArcGISMapServerDataSourceConfig dataSourceConfig) {
		return dataSourceConfig;
	}

	private String getJSONUrl(String baseUrlStr, String arcGISPath, String type) throws UnsupportedEncodingException {
		return getJSONUrl(baseUrlStr, arcGISPath, type, null);
	}

	private String getJSONUrl(String baseUrlStr, String arcGISPath, String type, String layerId) throws UnsupportedEncodingException {
		StringBuilder url = new StringBuilder(baseUrlStr);

		if (Utils.isNotBlank(arcGISPath)) {
			url.append("/");
			url.append(arcGISPath);
		}

		if (this.isServiceSupported(type)) {
			url.append("/"+type);
		}

		if (Utils.isNotBlank(layerId)) {
			url.append("/"+layerId);
		}

		// IMPORTANT: Some version of ArcGIS give weird output without pretty=true:
		// Example: value of initialExtent (sometimes) as a wrong value without pretty=true
		//     http://services.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Light_Gray_Base/MapServer?f=json
		//     VS
		//     http://services.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Light_Gray_Base/MapServer?f=json&pretty=true
		return Utils.setUrlParameter(Utils.setUrlParameter(url.toString(), "f", "json"), "pretty", "true");
	}

	private List<AbstractLayerConfig> parseJSON(Map<String, AbstractLayerConfig> allLayers, String arcGISPath, String type, ArcGISMapServerDataSourceConfig dataSourceConfig) throws IOException, JSONException {
		return parseJSON(allLayers, arcGISPath, type, dataSourceConfig, null);
	}
	private List<AbstractLayerConfig> parseJSON(Map<String, AbstractLayerConfig> allLayers, String arcGISPath, String type, ArcGISMapServerDataSourceConfig dataSourceConfig, JSONObject jsonParentService) throws IOException, JSONException {
		// We currently only support MapServer. Other possible values: GlobeServer
		if (type != null && !this.isServiceSupported(type)) {
			return null;
		}

		if (dataSourceConfig.getServiceUrl() == null) {
			throw new IllegalArgumentException("The data source [" + dataSourceConfig.getDataSourceName() + "] as no service URL.");
		}

		JSONObject json = URLCache.getJSONResponse(
				dataSourceConfig,
				getJSONUrl(dataSourceConfig.getServiceUrl(), arcGISPath, type)
		);

		List<AbstractLayerConfig> children = new ArrayList<AbstractLayerConfig>();
		if (json != null) {
			JSONArray jsonLayers = json.optJSONArray("layers");
			JSONArray jsonFolders = json.optJSONArray("folders");
			JSONArray jsonServices = json.optJSONArray("services");

			if (jsonLayers != null) {
				for (int i = 0; i < jsonLayers.length(); i++) {
					JSONObject jsonLayer = jsonLayers.optJSONObject(i);
					AbstractLayerConfig layer = null;

					JSONArray jsonChildren = jsonLayer.optJSONArray("subLayerIds");
					if (jsonChildren != null && jsonChildren.length() > 0) {
						// Request more info about the layer group (Max extent, description, etc.)
						String groupId = jsonLayer.optString("id", null);
						JSONObject jsonGroupExtra = null;
						if (Utils.isNotBlank(groupId)) {
							jsonGroupExtra = URLCache.getJSONResponse(
									dataSourceConfig,
									getJSONUrl(dataSourceConfig.getServiceUrl(), arcGISPath, type, groupId)
							);
						}

						GroupLayerConfig groupLayer = this.getGroupLayerConfig(jsonLayer, jsonGroupExtra, jsonChildren, dataSourceConfig);

						if (Utils.isNotBlank(arcGISPath)) {
							groupLayer.setGroupPath(arcGISPath);
						}
						layer = groupLayer;
					} else {
						// Request more info about the layer (Max extent, description, etc.)
						String layerId = jsonLayer.optString("id", null);
						JSONObject jsonLayerExtra = null;
						if (Utils.isNotBlank(layerId)) {
							jsonLayerExtra = URLCache.getJSONResponse(
									dataSourceConfig,
									getJSONUrl(dataSourceConfig.getServiceUrl(), arcGISPath, type, layerId)
							);
						}

						ArcGISMapServerLayerConfig arcGISLayer = null;
						if (jsonParentService != null && jsonParentService.has("tileInfo")) {
							arcGISLayer = this.getLayerCacheConfig(jsonLayer, jsonLayerExtra, jsonParentService, dataSourceConfig);
						} else {
							arcGISLayer = this.getLayerConfig(jsonLayer, jsonLayerExtra, dataSourceConfig);
						}
						if (Utils.isNotBlank(arcGISPath)) {
							arcGISLayer.setArcGISPath(arcGISPath);
						}
						layer = arcGISLayer;
					}

					this.ensureUniqueLayerId(layer, dataSourceConfig);

					// Check the layer catalog for this data source to be sure that the layer ID do not already exists.
					if (this.isUniqueId(allLayers, layer)) {
						allLayers.put(layer.getLayerId(), layer);
						children.add(layer);
					} else {
						String errorMsg = "Two layers from the data source ["+dataSourceConfig.getDataSourceName()+"] are returning the same ID: ["+layer.getLayerId()+"]";
						LOGGER.log(Level.SEVERE, errorMsg);
						throw new IllegalStateException(errorMsg);
					}
				}

				// Set children layer ID properly and remove layers that are children of an other layer
				List<AbstractLayerConfig> childrenToRemove = new ArrayList<AbstractLayerConfig>();
				for (AbstractLayerConfig layer : children) {
					String[] subLayerIds = null;
					if (layer instanceof FolderLayerConfig) {
						subLayerIds = ((FolderLayerConfig)layer).getLayers();
					}
					if (layer instanceof GroupLayerConfig) {
						subLayerIds = ((GroupLayerConfig)layer).getLayers();
					}

					if (subLayerIds != null && subLayerIds.length > 0) {
						for (int i=0; i<subLayerIds.length; i++) {
							String searchId = subLayerIds[i];
							for (AbstractLayerConfig foundLayer : children) {
								String foundLayerId = foundLayer.getLayerName();
								if (foundLayerId != null && foundLayerId.equals(searchId)) {
									subLayerIds[i] = foundLayer.getLayerId();
									// The child is a child of a sub layer, not a child of the upper group.
									// It has to be removed, but not now. There is currently 2 iterations on that list.
									childrenToRemove.add(foundLayer);
								}
							}
						}
					}
				}
				for (AbstractLayerConfig childToRemove : childrenToRemove) {
					children.remove(childToRemove);
				}
			}

			if (jsonFolders != null) {
				for (int i = 0; i < jsonFolders.length(); i++) {
					String childArcGISPath = this.getArcGISPath(jsonFolders.optString(i, null), dataSourceConfig);

					// This "if" prevent a possible infinite loop:
					// If one of the folder name is null or an empty string, the URL will be the same, returning the
					// same folder name including the null / empty string.
					if (Utils.isNotBlank(childArcGISPath)) {
						List<AbstractLayerConfig> subChildren = parseJSON(allLayers, childArcGISPath, null, dataSourceConfig, null);
						if (subChildren != null) {
							AbstractLayerConfig layerFolder = this.getLayerFolderConfig(childArcGISPath, subChildren, null, dataSourceConfig);
							this.ensureUniqueLayerId(layerFolder, dataSourceConfig);

							// Check the layer catalog for this data source to be sure that the layer ID do not already exists.
							if (this.isUniqueId(allLayers, layerFolder)) {
								allLayers.put(layerFolder.getLayerId(), layerFolder);
								children.add(layerFolder);
							} else {
								String errorMsg = "Two layers from the data source ["+dataSourceConfig.getDataSourceName()+"] are returning the same ID: ["+layerFolder.getLayerId()+"]";
								LOGGER.log(Level.SEVERE, errorMsg);
								throw new IllegalStateException(errorMsg);
							}
						}
					}
				}
			}

			if (jsonServices != null) {
				for (int i = 0; i < jsonServices.length(); i++) {
					JSONObject jsonService = jsonServices.optJSONObject(i);
					String childArcGISPath = this.getArcGISPath(jsonService.optString("name", null), dataSourceConfig);
					String childType = jsonService.optString("type", null);

					// Request more info about the folder (Max extent, description, etc.)
					JSONObject jsonServiceExtra = null;
					if (this.isServiceSupported(childType)) {
						jsonServiceExtra = URLCache.getJSONResponse(
								dataSourceConfig,
								getJSONUrl(dataSourceConfig.getServiceUrl(), childArcGISPath, childType)
						);
					}

					List<AbstractLayerConfig> subChildren = parseJSON(allLayers, childArcGISPath, childType, dataSourceConfig, jsonServiceExtra);
					if (subChildren != null) {
						AbstractLayerConfig layerService = this.getLayerFolderConfig(childArcGISPath, subChildren, jsonServiceExtra, dataSourceConfig);
						this.ensureUniqueLayerId(layerService, dataSourceConfig);

						// Check the layer catalog for this data source to be sure that the layer ID do not already exists.
						if (this.isUniqueId(allLayers, layerService)) {
							allLayers.put(layerService.getLayerId(), layerService);
							children.add(layerService);
						} else {
							String errorMsg = "Two layers from the data source ["+dataSourceConfig.getDataSourceName()+"] are returning the same ID: ["+layerService.getLayerId()+"]";
							LOGGER.log(Level.SEVERE, errorMsg);
							throw new IllegalStateException(errorMsg);
						}
					}
				}
			}
		}

		return children.isEmpty() ? null : children;
	}

	private boolean isServiceSupported(String serviceType) {
		return "MapServer".equalsIgnoreCase(serviceType);
	}

	/**
	 * There is a slight chance that two layers returns the same ID.
	 * For example, if the ArcGIS server allow "/" in layer path and the 2 following layers exists,
	 * they will have the same ID:
	 *     "path/to"/"layer"/"0"   =>   Layer ID 0 under the path "path/to" > "layer"
	 *     "path"/"to/layer"/"0"   =>   Layer ID 0 under the path "path" > "to/layer"
	 * NOTE: This exception is only possible if ArcGIS allow "/" in folder name or service path, which do not seems to be the case.
	 * @param allLayers
	 * @param layer
	 * @return
	 */
	private boolean isUniqueId(Map<String, AbstractLayerConfig> allLayers, AbstractLayerConfig layer) {
		return !allLayers.containsKey(layer.getLayerId());
	}

	private String getArcGISPath(String rawPath, ArcGISMapServerDataSourceConfig dataSourceConfig) {
		// NOTE: GBRMPA ArcGIS server do not fully comply with the standard.
		//     To work around this problem, we have to remove the "Public/" string from the paths.
		if (dataSourceConfig != null) {
			String ignoredPath = dataSourceConfig.getIgnoredArcGISPath();
			if (Utils.isNotBlank(ignoredPath) && rawPath.startsWith(ignoredPath)) {
				rawPath = rawPath.substring(ignoredPath.length());
			}
		}

		return rawPath;
	}

	private ArcGISMapServerLayerConfig getLayerConfig(JSONObject jsonLayer, JSONObject jsonLayerExtra, ArcGISMapServerDataSourceConfig dataSourceConfig) {
		ArcGISMapServerLayerConfig layer = new ArcGISMapServerLayerConfig(dataSourceConfig.getConfigManager());

		String layerId = jsonLayer.optString("id", null);

		layer.setLayerId(layerId);
		layer.setDataSourceId(dataSourceConfig.getDataSourceId());

		layer.setTitle(jsonLayer.optString("name", null));
		layer.setSelected(jsonLayer.optBoolean("defaultVisibility", true));

		if (jsonLayerExtra != null) {
			layer.setDescription(jsonLayerExtra.optString("description", null));
			layer.setLayerBoundingBox(this.getExtent(jsonLayerExtra.optJSONObject("extent"), layer.getTitle(), dataSourceConfig.getDataSourceName()));
		}

		return layer;
	}

	private ArcGISCacheLayerConfig getLayerCacheConfig(JSONObject jsonLayer, JSONObject jsonLayerExtra, JSONObject jsonParentService, ArcGISMapServerDataSourceConfig dataSourceConfig) {
		ArcGISCacheLayerConfig layer = new ArcGISCacheLayerConfig(dataSourceConfig.getConfigManager());

		String layerId = jsonLayer.optString("id", null);

		layer.setLayerId(layerId);
		layer.setDataSourceId(dataSourceConfig.getDataSourceId());
		layer.setDataSourceType("ARCGIS_CACHE");

		layer.setTitle(jsonLayer.optString("name", null));
		layer.setSelected(jsonLayer.optBoolean("defaultVisibility", true));

		if (jsonLayerExtra != null) {
			layer.setDescription(jsonLayerExtra.optString("description", null));
			layer.setLayerBoundingBox(this.getExtent(jsonLayerExtra.optJSONObject("extent"), layer.getTitle(), dataSourceConfig.getDataSourceName()));
		}

		if (jsonParentService != null) {
			JSONObject tileInfo = jsonParentService.optJSONObject("tileInfo");
			if (tileInfo != null) {
				if (tileInfo.has("rows")) {
					layer.setTileRows(tileInfo.optInt("rows"));
				}
				if (tileInfo.has("cols")) {
					layer.setTileCols(tileInfo.optInt("cols"));
				}
				JSONObject origin = tileInfo.optJSONObject("origin");
				if (origin != null) {
					if (origin.has("x")) {
						layer.setTileOriginX(origin.optDouble("x"));
					}
					if (origin.has("y")) {
						layer.setTileOriginX(origin.optDouble("y"));
					}
				}
				JSONArray lods = tileInfo.optJSONArray("lods");
				if (lods != null) {
					ArrayList<Double> resolutions = new ArrayList<Double>();
					for (int i=0; i<lods.length(); i++) {
						JSONObject lod = lods.optJSONObject(i);
						if (lod != null) {
							if (lod.has("resolution")) {
								resolutions.add(lod.optDouble("resolution"));
							}
						}
					}
					if (!resolutions.isEmpty()) {
						layer.setTileResolutions(resolutions.toArray(new Double[resolutions.size()]));
					}
				}
			}
		}

		return layer;
	}

	private GroupLayerConfig getGroupLayerConfig(JSONObject jsonGroup, JSONObject jsonGroupExtra, JSONArray jsonChildren, ArcGISMapServerDataSourceConfig dataSourceConfig) {
		GroupLayerConfig groupLayer = new GroupLayerConfig(dataSourceConfig.getConfigManager());

		String layerId = jsonGroup.optString("id", null);

		groupLayer.setLayerId(layerId);
		groupLayer.setDataSourceId(dataSourceConfig.getDataSourceId());

		groupLayer.setTitle(jsonGroup.optString("name", null));
		groupLayer.setSelected(jsonGroup.optBoolean("defaultVisibility", true));

		if (jsonChildren != null && jsonChildren.length() > 0) {
			String[] children = new String[jsonChildren.length()];
			for (int i=0; i<jsonChildren.length(); i++) {
				// Temporary set to it's raw ID
				// NOTE: The real layer ID can not be found now because the layer that it represent may not have been generated yet.
				children[i] = jsonChildren.optString(i);
			}
			groupLayer.setLayers(children);
			groupLayer.setDataSourceType("GROUP");
		}

		if (jsonGroupExtra != null) {
			groupLayer.setDescription(jsonGroupExtra.optString("description", null));
			groupLayer.setLayerBoundingBox(this.getExtent(jsonGroupExtra.optJSONObject("extent"), groupLayer.getTitle(), dataSourceConfig.getDataSourceName()));
		}

		return groupLayer;
	}

	private FolderLayerConfig getLayerFolderConfig(String childArcGISPath, List<AbstractLayerConfig> children, JSONObject jsonFolderExtra, ArcGISMapServerDataSourceConfig dataSourceConfig) {
		if (childArcGISPath == null) {
			return null;
		}
		FolderLayerConfig folderLayer = new FolderLayerConfig(dataSourceConfig.getConfigManager());

		// Keep only the lase part of the path for the folder display title
		String groupName = childArcGISPath;
		String groupTitle = groupName;
		int lastSlashIndex = groupTitle.lastIndexOf('/');
		if (lastSlashIndex > -1) {
			groupTitle = groupTitle.substring(lastSlashIndex+1);
		}

		String[] layers = new String[children.size()];
		int i=0;
		for (AbstractLayerConfig layer : children) {
			layers[i++] = layer.getLayerId();
		}

		folderLayer.setLayerId(groupName);
		folderLayer.setDataSourceId(dataSourceConfig.getDataSourceId());
		folderLayer.setDataSourceType("FOLDER");

		folderLayer.setTitle(groupTitle);
		folderLayer.setLayers(layers);

		// TODO folderLayer.maxExtent
		if (jsonFolderExtra != null) {
			folderLayer.setDescription(jsonFolderExtra.optString("serviceDescription", null));

			double[] extent = this.getExtent(jsonFolderExtra.optJSONObject("initialExtent"), folderLayer.getTitle(), dataSourceConfig.getDataSourceName());
			if (extent == null) {
				extent = this.getExtent(jsonFolderExtra.optJSONObject("fullExtent"), folderLayer.getTitle(), dataSourceConfig.getDataSourceName());
			}
			if (extent != null) {
				folderLayer.setLayerBoundingBox(extent);
			}
		}

		// childArcGISPath contains the current layerGroup
		folderLayer.setFolderPath(childArcGISPath);

		return folderLayer;
	}

	/**
	 *
	 * @param jsonExtent
	 * @param layerTitle For nicer error logs
	 * @param dataSourceTitle For nicer error logs
	 * @return
	 */
	private double[] getExtent(JSONObject jsonExtent, String layerTitle, String dataSourceTitle) {
		// Left, Bottom, Right, Top
		double[] reprojectedExtent = null;

		if (jsonExtent != null
				&& jsonExtent.has("spatialReference")
				&& jsonExtent.has("xmin") && jsonExtent.has("ymin")
				&& jsonExtent.has("xmax") && jsonExtent.has("ymax")) {
			// NOTE If there is not info about the spatial reference of the extent, the extent is ignored.
			JSONObject jsonSourceCRS = jsonExtent.optJSONObject("spatialReference");

			double[] extent = new double[] {
				jsonExtent.optDouble("xmin"),
				jsonExtent.optDouble("ymin"),
				jsonExtent.optDouble("xmax"),
				jsonExtent.optDouble("ymax")
			};

			String sourceCRSStr = "EPSG:" + jsonSourceCRS.optString("wkid", null);

			try {
				reprojectedExtent = Utils.reprojectCoordinatesToDegrees(extent, sourceCRSStr);
			} catch (NoSuchAuthorityCodeException ex) {
				LOGGER.log(Level.WARNING, "The layer ["+layerTitle+"] from the data source ["+dataSourceTitle+"] has an unknown extent CRS. " + ex.getMessage());
			} catch (Exception ex) {
				LOGGER.log(Level.WARNING, "The layer ["+layerTitle+"] from the data source ["+dataSourceTitle+"] has an unsupported extent. " + ex.getMessage());
			}
		}

		if (reprojectedExtent != null) {
			// Ensure that the conversion was successful
			boolean valid = true;
			for (int i=0; i<reprojectedExtent.length; i++) {
				if (Double.isNaN(reprojectedExtent[i])) {
					valid = false;
				}
			}

			if (!valid) {
				return null;
			}
		}

		return reprojectedExtent;
	}
}
