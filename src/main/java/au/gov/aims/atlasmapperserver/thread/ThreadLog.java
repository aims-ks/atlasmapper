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

import au.gov.aims.atlasmapperserver.Utils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.logging.Level;

public class ThreadLog {
    private Level level;
    private String message;
    private Throwable exception;

    public ThreadLog(Level level, String message) {
        this(level, message, null);
    }

    public ThreadLog(Level level, String message, Throwable exception) {
        this.level = level;
        this.message = message;
        this.exception = exception;
    }

    public Level getLevel() {
        return this.level;
    }

    public String getMessage() {
        return this.message;
    }

    public Throwable getException() {
        return this.exception;
    }

    public boolean isError() {
        return Level.SEVERE.equals(this.level);
    }

    public boolean isWarning() {
        return Level.WARNING.equals(this.level);
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();

        json.put("level", this.level.getName());
        json.put("message", this.getMessage());

        Throwable ex = this.getException();
        if (ex != null) {
            json.put("exception", Utils.getExceptionMessage(ex));
            json.put("stacktrace", ex.getStackTrace());
        }

        return json;
    }
}
