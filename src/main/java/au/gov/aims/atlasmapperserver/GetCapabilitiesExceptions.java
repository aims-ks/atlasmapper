/*
 *  This file is part of AtlasMapper server and clients.
 * 
 *  Copyright (C) 2011 Australian Institute of Marine Science
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

package au.gov.aims.atlasmapperserver;

import au.gov.aims.atlasmapperserver.dataSourceConfig.AbstractDataSourceConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains a list of error message thrown when requesting a Capabilities Doc.
 * @author Gael Lafond <g.lafond@aims.org.au>
 */
public class GetCapabilitiesExceptions extends Throwable {
	private List<DataSourceException> exceptions;

	public GetCapabilitiesExceptions() {
		this.exceptions = new ArrayList<DataSourceException>();
	}

	@Override
	public String getMessage() {
		StringBuilder errorMsg = new StringBuilder("Could not parse ");
		errorMsg.append(this.exceptions.size());
		errorMsg.append(" capabilities document(s):\n");
		boolean first = true;
		for (DataSourceException dataSourceException : this.exceptions) {
			if (!first) { errorMsg.append("\n"); }
			first = false;
			errorMsg.append("- ");
			errorMsg.append(dataSourceException.dataSource.getDataSourceName());
			errorMsg.append(" [");
			errorMsg.append(dataSourceException.dataSource.getServiceUrl());
			errorMsg.append("]: ");
			errorMsg.append(dataSourceException.exception.getMessage());
		}
		return errorMsg.toString();
	}

	@Override
	public void printStackTrace() {
		for (DataSourceException dataSourceException : this.exceptions) {
			System.out.println("Stack trace for: " + dataSourceException.dataSource.getDataSourceName());
			dataSourceException.exception.printStackTrace();
		}
	}

	private class DataSourceException {
		public final AbstractDataSourceConfig dataSource;
		public final Exception exception;

		public DataSourceException(final AbstractDataSourceConfig dataSource, final Exception exception) {
			this.dataSource = dataSource;
			this.exception = exception;
		}
	}


	// List methods
	public int size() {
		return this.exceptions.size();
	}

	public boolean isEmpty() {
		return this.exceptions.isEmpty();
	}

	public void add(AbstractDataSourceConfig dataSource, Exception ex) {
		this.exceptions.add(new DataSourceException(dataSource, ex));
	}

	public void clear() {
		this.exceptions.clear();
	}
}
