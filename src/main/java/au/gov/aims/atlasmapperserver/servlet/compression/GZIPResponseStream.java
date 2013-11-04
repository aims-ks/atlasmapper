/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2012 Australian Institute of Marine Science
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
/**
 * See: http://opensource.apple.com/source/blojsom/blojsom-67/src/org/blojsom/filter/GZIPResponseStream.java
 */
package au.gov.aims.atlasmapperserver.servlet.compression;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

/**
 * GZIPResponseStream
 * <p/>
 * Copyright 2003 Jayson Falkner (jayson@jspinsider.com)
 * This code is from "Servlets and JavaServer pages; the J2EE Web Tier",
 * http://www.jspbook.com. You may freely use the code both commercially
 * and non-commercially. If you like the code, please pick up a copy of
 * the book and help support the authors, development of more free code,
 * and the JSP/Servlet/J2EE community.
 *
 * @version $Id: GZIPResponseStream.java,v 1.2.2.1 2005/07/21 14:11:03 johnan Exp $
 * @since blojsom 2.10
 */
public class GZIPResponseStream extends ServletOutputStream {
	private static final Logger LOGGER = Logger.getLogger(GZIPResponseStream.class.getName());

	protected ByteArrayOutputStream baos = null;
	protected GZIPOutputStream gzipstream = null;
	protected boolean closed = false;
	protected HttpServletResponse response = null;
	protected ServletOutputStream output = null;

	/**
	 * Create a new GZIPResponseStream
	 *
	 * @param response Original HTTP servlet response
	 * @throws IOException If there is an error creating the response stream
	 */
	public GZIPResponseStream(HttpServletResponse response) throws IOException {
		super();

		this.closed = false;
		this.response = response;
		this.output = response.getOutputStream();
		this.baos = new ByteArrayOutputStream();
		this.gzipstream = new GZIPOutputStream(this.baos);
	}

	/**
	 * Close this response stream
	 *
	 * @throws IOException If the stream is already closed or there is an error closing the stream
	 */
	@Override
	public void close() throws IOException {
		if (this.closed) {
			LOGGER.log(Level.FINE, "This output stream has already been closed.");
			return;
		}

		try {
			this.gzipstream.finish();
			this.gzipstream.flush();

			byte[] bytes = this.baos.toByteArray();

			this.response.addHeader("Content-Length", Integer.toString(bytes.length));
			this.response.addHeader("Content-Encoding", "gzip");

			this.baos.flush();

			this.output.write(bytes);
			this.output.flush();

			this.closed = true;
		} finally {
			try {
				this.baos.close();
			} catch(Exception ex) {
				LOGGER.log(Level.FINE, "Exception occurred while closing the ByteArrayOutputStream.", ex);
			}
			try {
				this.gzipstream.close();
			} catch(Exception ex) {
				LOGGER.log(Level.FINE, "Exception occurred while closing the GZIP Stream.", ex);
			}
			try {
				this.output.close();
			} catch(Exception ex) {
				LOGGER.log(Level.FINE, "Exception occurred while closing the response output stream.", ex);
			}
		}
	}

	/**
	 * Flush the response stream
	 *
	 * @throws IOException If the stream is already closed or there is an error flushing the stream
	 */
	@Override
	public void flush() throws IOException {
		if (this.closed) {
			throw new IOException("Cannot flush a closed output stream.");
		}

		this.gzipstream.flush();
	}

	/**
	 * Write a byte to the stream
	 *
	 * @param b Byte to write
	 * @throws IOException If the stream is closed or there is an error in writing
	 */
	@Override
	public void write(int b) throws IOException {
		if (this.closed) {
			throw new IOException("Cannot write to a closed output stream.");
		}

		this.gzipstream.write((byte) b);
	}

	/**
	 * Write a byte array to the stream
	 *
	 * @param b Byte array to write
	 * @throws IOException If the stream is closed or there is an error in writing
	 */
	@Override
	public void write(byte[] b) throws IOException {
		this.write(b, 0, b.length);
	}

	/**
	 * Write a byte array to the stream
	 *
	 * @param b   Byte array to write
	 * @param off Offset of starting point in byte array to start writing
	 * @param len Length of bytes to write
	 * @throws IOException If the stream is closed or there is an error in writing
	 */
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if (this.closed) {
			throw new IOException("Cannot write to a closed output stream.");
		}

		this.gzipstream.write(b, off, len);
	}

	/**
	 * Returns <code>true</code> if the stream is closed, <code>false</code> otherwise
	 *
	 * @return <code>true</code> if the stream is closed, <code>false</code> otherwise
	 */
	public boolean closed() {
		return (this.closed);
	}

	/**
	 * Reset the stream. Currently a no-op.
	 */
	public void reset() {
	}
}
