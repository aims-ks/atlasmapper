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

/**
 * Exception thrown when the user click the stop button.
 * It extends Throwable instead of Exception therefore it's not catchable with "catch (Exception)".
 * It's harder to catch therefore it's more likely to bubble up and cancel the the thread execution.
 * Inspired from java.lang.Exception
 */
public class RevivableThreadInterruptedException extends Throwable {

    public RevivableThreadInterruptedException() {
        super();
    }

    public RevivableThreadInterruptedException(String message) {
        super(message);
    }

    public RevivableThreadInterruptedException(String message, Throwable cause) {
        super(message, cause);
    }

    public RevivableThreadInterruptedException(Throwable cause) {
        super(cause);
    }
}
