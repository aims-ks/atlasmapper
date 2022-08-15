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

import org.geotools.ows.wms.Layer;
import org.geotools.ows.wms.StyleImpl;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.logging.Logger;

/**
 * Handler used to parse the WMTS document.
 * It currently extracts the following info:
 *    Layers
 *        Layer name (Capabilities > Contents > Layer > ows:Identifier)
 *        Layer title (Capabilities > Contents > Layer > ows:Title)
 *        Styles
 *            Style name (Capabilities > Contents > Layer > Style > ows:Identifier)
 */
public class WMTSHandler extends DefaultHandler {
	private static final Logger LOGGER = Logger.getLogger(WMTSHandler.class.getName());

	private static final String LAYER = "Layer";
	private static final String LAYER_NAME = "ows:Identifier";
	private static final String LAYER_TITLE = "ows:Title";
	private static final String STYLE = "Style";
	private static final String STYLE_NAME = "ows:Identifier";

	// This value is considered as NULL when found in the cap doc.
	private static final String NULL_STYLE_VALUE = "_null";

	private WMTSDocument doc;
	private Layer rootLayer;
	private Layer childLayer;
	private List<StyleImpl> styles;
	private StyleImpl style;

	private StringBuilder collectedChars;
	private Stack<String> xmlPath;
	// Marker to highlight important paths, without having to check the current path every time.
	private XMLPathMarker xmlPathMarker;

	public WMTSHandler(WMTSDocument doc) {
		super();
		this.doc = doc;
		this.rootLayer = new Layer();
		this.childLayer = null;
		this.styles = null;
		this.style = null;

		this.xmlPath = new Stack<String>();

		this.xmlPathMarker = null;

		this.doc.setLayer(this.rootLayer);
	}



	@Override
	public void startElement(
			String uri,
			String localName,
			String qName,
			Attributes attributes) throws SAXException {

		this.xmlPath.push(qName);
		this.xmlPathMarker = XMLPathMarker.get(this.xmlPath);

		if (this.xmlPathMarker != null) {
			this.collectedChars = new StringBuilder();
		}
	}

	@Override
	public void endElement(
			String uri,
			String localName,
			String qName) throws SAXException {

		if (this.xmlPathMarker != null) {
			String previousQName = (this.xmlPath.size() < 2 ? null : this.xmlPath.get(this.xmlPath.size() - 2));

			if (XMLPathMarker.LAYER.equals(this.xmlPathMarker)) {
				// Anything under: Capabilities > Contents > Layer

				if (qName.equals(LAYER)) {
					// Path equals: Capabilities > Contents > Layer
					// End of the child layer
					if (this.childLayer != null) {
						if (this.styles != null && !this.styles.isEmpty()) {
							this.childLayer.setStyles(this.styles);
						}
						this.rootLayer.addChildren(this.childLayer);
					}
					this.childLayer = null;
					this.styles = null;

				} else if (previousQName.equals(STYLE)) {
					// Path starts with: Capabilities > Contents > Layer > Style > ...
					// Elements of interest inside the layer's Style element

					if (qName.equals(STYLE_NAME)) {
						// Path equals: Capabilities > Contents > Layer > Style > ows:Identifier
						String layerName = this.collectedChars.toString();
						// Some GeoServer installation (prior to 2.3.0) has some styles called "_null"
						// See: http://spatial.ala.org.au/geoserver/gwc/service/wmts?REQUEST=getcapabilities
						if (layerName != null && !layerName.equals(NULL_STYLE_VALUE)) {
							if (this.style == null) {
								this.style = new StyleImpl();
							}
							this.style.setName(layerName);
						}
					}

				} else if (previousQName.equals(LAYER)) {
					// Path starts with: Capabilities > Contents > Layer > ...
					// Elements of interest inside the Layer element

					if (qName.equals(STYLE)) {
						// Path equals: Capabilities > Contents > Layer > Style
						// End of a layer style
						if (this.style != null) {
							if (this.styles == null) {
								this.styles = new ArrayList<StyleImpl>();
							}
							this.styles.add(this.style);
						}
						this.style = null;

					} else if (qName.equals(LAYER_NAME)) {
						// Path equals: Capabilities > Contents > Layer > ows:Identifier
						if (this.childLayer == null) {
							this.childLayer = new Layer();
						}
						this.childLayer.setName(this.collectedChars.toString());

					} else if (qName.equals(LAYER_TITLE)) {
						// Path equals: Capabilities > Contents > Layer > ows:Title
						if (this.childLayer == null) {
							this.childLayer = new Layer();
						}
						this.childLayer.setTitle(this.collectedChars.toString());
					}
				}
			}
		}

		// The current QName is not needed (kept for debugging), but the call to the pop method is mandatory...
		String currentQName = this.xmlPath.pop();
		this.xmlPathMarker = XMLPathMarker.get(this.xmlPath);
	}

	@Override
	public void characters(
			char ch[],
			int start,
			int length) throws SAXException {

		if (this.xmlPathMarker != null && this.collectedChars != null) {
			this.collectedChars.append(ch, start, length);
		}
	}

	private static enum XMLPathMarker {
		LAYER (new String[]{"Capabilities", "Contents", "Layer"});

		private final String[] path;

		XMLPathMarker(String[] path) {
			this.path = path;
		}

		public static XMLPathMarker get(List<String> xmlPath) {
			if (xmlPath == null || xmlPath.isEmpty()) {
				return null;
			}

			XMLPathMarker[] markers = XMLPathMarker.values();
			for (XMLPathMarker marker : markers) {
				boolean inPath = true;
				if (xmlPath.size() < marker.path.length) {
					inPath = false;
				} else {
					for (int i=0, len=marker.path.length; inPath && i<len; i++) {
						if (!marker.path[i].equalsIgnoreCase(xmlPath.get(i))) {
							inPath = false;
						}
					}
				}

				if (inPath) {
					return marker;
				}
			}

			return null;
		}
	}
}
