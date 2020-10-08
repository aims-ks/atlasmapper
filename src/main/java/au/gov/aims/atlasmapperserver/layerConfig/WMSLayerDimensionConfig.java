/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2020 Australian Institute of Marine Science
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

import au.gov.aims.atlasmapperserver.AbstractConfig;
import au.gov.aims.atlasmapperserver.ConfigManager;
import au.gov.aims.atlasmapperserver.Utils;
import au.gov.aims.atlasmapperserver.annotation.ConfigField;

public class WMSLayerDimensionConfig extends AbstractConfig {
    public static final String[] TIME_DIMENSION_UNITS = new String[]{
        "ISO8601"
    };

    @ConfigField
    private String name;

    @ConfigField
    private String units;

    @ConfigField
    private String unitSymbol;

    @ConfigField
    private Boolean timeDimension;

    public WMSLayerDimensionConfig(ConfigManager configManager) {
        super(configManager);
    }

    @Override
    public void setJSONObjectKey(String key) {
        if (Utils.isBlank(this.name)) {
            this.name = key;
        }
    }

    @Override
    public String getJSONObjectKey() {
        return this.name;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUnits() {
        return this.units;
    }

    public void setUnits(String units) {
        this.units = units;
        this.timeDimension = null;
    }

    public String getUnitSymbol() {
        return this.unitSymbol;
    }

    public void setUnitSymbol(String unitSymbol) {
        this.unitSymbol = unitSymbol;
    }

    public boolean isTimeDimension() {
        if (this.timeDimension == null) {
            this.timeDimension = false;
            if (this.units != null) {
                for (String timeUnit : TIME_DIMENSION_UNITS) {
                    if (this.units.equalsIgnoreCase(timeUnit)) {
                        this.timeDimension = true;
                        break;
                    }
                }
            }
        }
        return this.timeDimension;
    }
}
