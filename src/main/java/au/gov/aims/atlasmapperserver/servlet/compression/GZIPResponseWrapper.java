/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2012 Australian Institute of Marine Science
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
/**
 * See: http://opensource.apple.com/source/blojsom/blojsom-67/src/org/blojsom/filter/GZIPResponseWrapper.java
 */
package au.gov.aims.atlasmapperserver.servlet.compression;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * GZIPResponseWrapper
 * <p/>
 * Copyright 2003 Jayson Falkner (jayson@jspinsider.com)
 * This code is from "Servlets and JavaServer pages; the J2EE Web Tier",
 * http://www.jspbook.com. You may freely use the code both commercially
 * and non-commercially. If you like the code, please pick up a copy of
 * the book and help support the authors, development of more free code,
 * and the JSP/Servlet/J2EE community.
 *
 * @version $Id: GZIPResponseWrapper.java,v 1.2.2.1 2005/07/21 14:11:03 johnan Exp $
 * @since blojsom 2.10
 */
public class GZIPResponseWrapper extends HttpServletResponseWrapper {

    protected HttpServletResponse origResponse = null;
    protected ServletOutputStream stream = null;
    protected PrintWriter writer = null;

    /**
     * Create a new GZIPResponseWrapper
     *
     * @param response Original HTTP servlet response
     */
    public GZIPResponseWrapper(HttpServletResponse response) {
        super(response);
        this.origResponse = response;
    }

    /**
     * Create a new ServletOutputStream which returns a GZIPResponseStream
     *
     * @return GZIPResponseStream object
     * @throws IOException If there is an error creating the response stream
     */
    public ServletOutputStream createOutputStream() throws IOException {
        return (new GZIPResponseStream(this.origResponse));
    }

    /**
     * Finish the response
     */
    public void finishResponse() throws IOException {
        try {
            if (this.writer != null) {
                this.writer.close();
            }
        } finally {
            if (this.stream != null) {
                this.stream.close();
            }
        }
    }

    /**
     * Flush the output buffer
     *
     * @throws IOException If there is an error flushing the buffer
     */
    @Override
    public void flushBuffer() throws IOException {
        if (this.stream != null) {
            this.stream.flush();
        }
    }

    /**
     * Retrieve the output stream for this response wrapper
     *
     * @return {@link #createOutputStream()}
     * @throws IOException If there is an error retrieving the output stream
     */
    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (this.writer != null) {
            throw new IllegalStateException("getWriter() has already been called.");
        }

        if (this.stream == null) {
            this.stream = this.createOutputStream();
        }

        return this.stream;
    }

    /**
     * Retrieve a writer for this response wrapper
     *
     * @return PrintWriter that wraps an OutputStreamWriter (using UTF-8 as encoding)
     * @throws IOException If there is an error retrieving the writer
     */
    @Override
    public PrintWriter getWriter() throws IOException {
        if (this.writer != null) {
            return (this.writer);
        }

        if (this.stream != null) {
            throw new IllegalStateException("getOutputStream() has already been called.");
        }

        this.stream = this.createOutputStream();
        this.writer = new PrintWriter(new OutputStreamWriter(this.stream, "UTF-8"));

        return this.writer;
    }

    /**
     * Set the content length for the response. Currently a no-op.
     *
     * @param length Content length
     */
    @Override
    public void setContentLength(int length) {
    }
}
