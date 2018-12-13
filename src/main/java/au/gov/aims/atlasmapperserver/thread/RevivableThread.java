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
 * This class spawn threads when "start" is called.
 * It can be used as a reusable thread.
 */
public abstract class RevivableThread implements Runnable {
    private Thread thread;

    public abstract void run();
    public abstract void reset();

    public synchronized void start() {
        if (this.thread == null || !this.thread.isAlive()) {
            this.thread = new Thread() {
                @Override
                public void run() {
                    RevivableThread.this.run();
                }
            };
            this.reset();
        }

        this.thread.start();
    }

    public void interrupt() {
        if (this.thread != null) {
            this.thread.interrupt();
        }
    }

    public final void join() throws RevivableThreadInterruptedException {
        if (this.thread != null) {
            try {
                this.thread.join();
            } catch (InterruptedException ex) {
                throw new RevivableThreadInterruptedException(ex);
            }
        }
    }

    public boolean isAlive() {
        return this.thread != null && this.thread.isAlive();
    }

    public static boolean isInterrupted() {
        return Thread.currentThread().isInterrupted();
    }

    public static void checkForInterruption() throws RevivableThreadInterruptedException {
        if (RevivableThread.isInterrupted()) {
            throw new RevivableThreadInterruptedException();
        }
    }
}
