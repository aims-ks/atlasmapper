/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2018 Australian Institute of Marine Science
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
package au.gov.aims.atlasmapperserver.thread;

import java.text.DecimalFormat;

public abstract class AbstractConfigThread extends RevivableThread {
    // Used to format the elapse time (always put at lease 1 digit before the dot, with maximum 2 digits after)
    public static final DecimalFormat ELAPSE_TIME_FORMAT = new DecimalFormat("0.##");

    private ThreadLogger logger;

    public AbstractConfigThread() {
        this.logger = new ThreadLogger();
    }

    @Override
    public void reset() {
        this.logger.reset();
    }

    // Status

    public ThreadLogger getLogger() {
        return this.logger;
    }
}
