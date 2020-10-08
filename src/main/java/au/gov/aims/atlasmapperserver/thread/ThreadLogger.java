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

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class ThreadLogger {
    private List<ThreadLog> logs;
    private int warningCount;
    private int errorCount;

    public ThreadLogger() {
        this.logs = new ArrayList<ThreadLog>();
        this.warningCount = 0;
        this.errorCount = 0;
    }

    public synchronized void reset() {
        this.logs.clear();
        this.warningCount = 0;
        this.errorCount = 0;
    }

    public synchronized List<ThreadLog> getLogs() {
        return this.logs;
    }

    public synchronized void log(Level level, String message) {
        this.log(new ThreadLog(level, message));
    }

    public synchronized void log(Level level, String message, Throwable ex) {
        this.log(new ThreadLog(level, message, ex));
    }

    public synchronized void log(ThreadLog log) {
        this.logs.add(log);
        if (log.isError()) {
            this.errorCount++;
        }
        if (log.isWarning()) {
            this.warningCount++;
        }
    }

    public int getWarningCount() {
        return this.warningCount;
    }

    public int getErrorCount() {
        return this.errorCount;
    }

    public synchronized void addAll(ThreadLogger logger) {
        for (ThreadLog log : logger.getLogs()) {
            this.log(log);
        }
    }

    public synchronized JSONArray toJSON() throws JSONException {
        JSONArray json = new JSONArray();

        for (ThreadLog log : this.logs) {
            json.put(log.toJSON());
        }

        return json;
    }
}
