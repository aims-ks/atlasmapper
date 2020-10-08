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

import au.gov.aims.atlasmapperserver.AbstractConfig;
import au.gov.aims.atlasmapperserver.ConfigManager;

public abstract class AbstractRunnableConfig<T extends AbstractConfigThread> extends AbstractConfig {
    protected T configThread;

    public AbstractRunnableConfig (ConfigManager configManager, T configThread) {
        super(configManager);
        this.configThread = configThread;
    }

    public synchronized void start() {
        if (this.isIdle()) {
            this.configThread.start();
        }
    }

    public synchronized void stop() {
        if (!this.isIdle()) {
            this.configThread.interrupt();
        }
    }

    public boolean isIdle() {
        return this.configThread == null || !this.configThread.isAlive();
    }

    public T getThread() {
        return this.configThread;
    }

    public void setThread(T configThread) {
        this.configThread = configThread;
    }
}
