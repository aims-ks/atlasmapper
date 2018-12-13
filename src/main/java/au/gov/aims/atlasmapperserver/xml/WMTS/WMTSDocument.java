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
package au.gov.aims.atlasmapperserver.xml.WMTS;

import org.geotools.data.ows.Layer;
import org.geotools.data.ows.StyleImpl;

import java.util.List;

public class WMTSDocument {
	// uri: For logging purpose
	private String uri;
	private Layer rootLayer;

	public WMTSDocument(String uri) {
		this.uri = uri;
	}

	public String getUri() {
		return this.uri;
	}

	public void setLayer(Layer rootLayer) {
		this.rootLayer = rootLayer;
	}

	public Layer getLayer() {
		return this.rootLayer;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.getClass().getSimpleName());
		sb.append(" {\n");

		if (this.uri != null && !this.uri.isEmpty()) {
			sb.append("\turi: ");
			sb.append(this.uri);
			sb.append("\n");
		}
		if (this.rootLayer != null) {
			sb.append("\trootLayer: {\n");
			List<Layer> children = this.rootLayer.getLayerChildren();
			if (children != null && !children.isEmpty()) {
				for (Layer child : children) {
					sb.append("\t\t");
					sb.append(child.getName());
					List<StyleImpl> styles = child.getStyles();
					if (styles != null && !styles.isEmpty()) {
						sb.append(": {");
						boolean first = true;
						for (StyleImpl style : styles) {
							if (first) {
								first = false;
							} else {
								sb.append(", ");
							}
							sb.append(style.getName());
						}
						sb.append("}");
					}
					sb.append("\n");
				}
			}
			sb.append("\t}\n");
		}

		sb.append("}");

		return sb.toString();
	}
}
