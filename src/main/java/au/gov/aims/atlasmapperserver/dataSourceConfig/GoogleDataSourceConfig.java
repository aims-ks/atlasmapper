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

import au.gov.aims.atlasmapperserver.ConfigManager;
import au.gov.aims.atlasmapperserver.annotation.ConfigField;
import au.gov.aims.atlasmapperserver.layerGenerator.AbstractLayerGenerator;
import au.gov.aims.atlasmapperserver.layerGenerator.GoogleLayerGenerator;

public class GoogleDataSourceConfig extends AbstractDataSourceConfig {
    @ConfigField
    private String googleAPIKey;

    @ConfigField
    private String googleJavaScript;

    public GoogleDataSourceConfig(ConfigManager configManager) {
        super(configManager);
    }

    @Override
    public AbstractLayerGenerator createLayerGenerator() {
        return new GoogleLayerGenerator();
    }

    public String getGoogleAPIKey() {
        return this.googleAPIKey;
    }

    public void setGoogleAPIKey(String googleAPIKey) {
        this.googleAPIKey = googleAPIKey;
    }

    public String getGoogleJavaScript() {
        return this.googleJavaScript;
    }

    public void setGoogleJavaScript(String googleJavaScript) {
        this.googleJavaScript = googleJavaScript;
    }
}
