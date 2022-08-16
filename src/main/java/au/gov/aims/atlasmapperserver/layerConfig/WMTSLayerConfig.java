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

package au.gov.aims.atlasmapperserver.layerConfig;

import au.gov.aims.atlasmapperserver.ConfigManager;
import au.gov.aims.atlasmapperserver.Utils;
import au.gov.aims.atlasmapperserver.annotation.ConfigField;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class WMTSLayerConfig extends WMSLayerConfig {

    @ConfigField
    // Key = CRS. Example: "EPSG:4326"
    public Map<String, MatrixSet> matrixSets;

    public WMTSLayerConfig(ConfigManager configManager) {
        super(configManager);
        this.matrixSets = new HashMap<String, MatrixSet>();
    }

    public void addMatrixSet(String epsgCode, String matrixSetId, int zoomLevel, String matrixId) {
        MatrixSet matrixSet = this.matrixSets.get(epsgCode);
        if (matrixSet == null) {
            matrixSet = new MatrixSet(matrixSetId);
        }
        matrixSet.addMatrix(zoomLevel, matrixId);
    }

    public void addMatrixSet(String epsgCode, MatrixSet matrixSet) {
        this.matrixSets.put(epsgCode, matrixSet);
    }

    public Map<String, MatrixSet> getMatrixSets() {
        return this.matrixSets;
    }

    @Override
    public String toString() {
        String matrixSetsStr = null;
        if (this.matrixSets != null && !this.matrixSets.isEmpty()) {
            matrixSetsStr = "{" + "\n";
            for (Map.Entry<String, MatrixSet> matrixSetEntry : this.matrixSets.entrySet()) {
                matrixSetsStr += "		" + matrixSetEntry.getKey() + "={" + "\n";
                MatrixSet matrixSet = matrixSetEntry.getValue();
                matrixSetsStr += "			id=" + matrixSet.getId() + "," + "\n";
                matrixSetsStr += "			matrices={" + "\n";
                for (Map.Entry<Integer, String> matrixMapEntry : matrixSet.getMatrixMap().entrySet()) {
                    matrixSetsStr += "				" + matrixMapEntry.getKey() + "=" + matrixMapEntry.getValue() + "\n";
                }
                matrixSetsStr += "			}" + "\n";
                matrixSetsStr += "		}" + "\n";
            }
            matrixSetsStr += "	}";
        }

        return "WMTSLayerConfig {\n" +
                (Utils.isBlank(this.getLayerId()) ? "" :       "	layerId=" + this.getLayerId() + "\n") +
                (Utils.isBlank(this.getLayerName()) ? "" :     "	layerName=" + this.getLayerName() + "\n") +
                (this.getAliasIds()==null ? "" :               "	aliasIds=" + Arrays.toString(this.getAliasIds()) + "\n") +
                (Utils.isBlank(this.getTitle()) ? "" :         "	title=" + this.getTitle() + "\n") +
                (Utils.isBlank(this.getDescription()) ? "" :   "	description=" + this.getDescription() + "\n") +
                (this.getLayerBoundingBox()==null ? "" :       "	layerBoundingBox=" + Arrays.toString(this.getLayerBoundingBox()) + "\n") +
                (this.getInfoHtmlUrls()==null ? "" :           "	infoHtmlUrls=" + Arrays.toString(this.getInfoHtmlUrls()) + "\n") +
                (this.isIsBaseLayer()==null ? "" :             "	isBaseLayer=" + this.isIsBaseLayer() + "\n") +
                (this.isHasLegend()==null ? "" :               "	hasLegend=" + this.isHasLegend() + "\n") +
                (Utils.isBlank(this.getLegendGroup()) ? "" :   "	legendGroup=" + this.getLegendGroup() + "\n") +
                (Utils.isBlank(this.getLegendTitle()) ? "" :   "	legendTitle=" + this.getLegendTitle() + "\n") +
                (this.isWmsQueryable()==null ? "" :            "	wmsQueryable=" + this.isWmsQueryable() + "\n") +
                (Utils.isBlank(this.getTreePath()) ? "" :      "	treePath=" + this.getTreePath() + "\n") +
                (this.getWmsFeatureRequestLayers()==null ? "" : "	wmsFeatureRequestLayers=" + Arrays.toString(this.getWmsFeatureRequestLayers()) + "\n") +
                (this.getStyles()==null ? "" :                 "	styles=" + this.getStyles() + "\n") +
                (this.getOptions()==null ? "" :                "	options=" + this.getOptions() + "\n") +
                (this.isSelected()==null ? "" :                "	selected=" + this.isSelected() + "\n") +
                (matrixSetsStr==null ? "" :                    "	matrixSets=" + matrixSetsStr + "\n") +
            '}';
    }

    public static class MatrixSet {
        @ConfigField
        private final String id;

        @ConfigField
        private final Map<Integer, String> matrixMap;

        public MatrixSet(String id) {
            this.id = id;
            this.matrixMap = new HashMap<Integer, String>();
        }

        public void addMatrix(int zoomLevel, String matrixId) {
            this.matrixMap.put(zoomLevel, matrixId);
        }

        public String getId() {
            return this.id;
        }

        public Map<Integer, String> getMatrixMap() {
            return this.matrixMap;
        }
    }
}
