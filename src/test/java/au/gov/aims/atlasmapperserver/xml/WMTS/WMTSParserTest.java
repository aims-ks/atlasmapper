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
package au.gov.aims.atlasmapperserver.xml.WMTS;

import junit.framework.TestCase;
import org.geotools.data.ows.Layer;
import org.geotools.data.ows.StyleImpl;

import java.io.InputStream;
import java.net.URL;
import java.util.List;

public class WMTSParserTest extends TestCase {
	public void testParsing() throws Exception {
		URL url = WMTSParserTest.class.getClassLoader().getResource("geoWebCache1-4_wmts.xml");

		InputStream inputStream = null;
		WMTSDocument doc = null;
		try {
			inputStream = url.openStream();
			doc = WMTSParser.parseInputStream(inputStream, "tc211_full.xml");
		} finally {
			if (inputStream != null) {
				inputStream.close();
			}
		}

		Layer rootLayer = doc.getLayer();

		List<Layer> layers = rootLayer.getLayerChildren();

		// Check layer count
		assertEquals("Layer count do not match.",
			21, layers.size());

		for (Layer layer : layers) {
			if ("nurc:Arc_Sample".equals(layer.getName())) {
				List<StyleImpl> styles = layer.getStyles();
				assertEquals("Style count do not match for layer: " + layer.getName(), 1, styles.size());
				for (StyleImpl style : styles) {
					if (!"raster".equals(style.getName())) {
						fail("Unexpected style found for layer: " + layer.getName());
					}
				}
			} else if ("nurc:Img_Sample".equals(layer.getName())) {
				List<StyleImpl> styles = layer.getStyles();
				assertTrue("Style count do not match for layer: " + layer.getName(), styles == null || styles.isEmpty());
			} else if ("nurc:mosaic".equals(layer.getName())) {
				List<StyleImpl> styles = layer.getStyles();
				assertTrue("Style count do not match for layer: " + layer.getName(), styles == null || styles.isEmpty());
			} else if ("sf:archsites".equals(layer.getName())) {
				List<StyleImpl> styles = layer.getStyles();
				assertEquals("Style count do not match for layer: " + layer.getName(), 2, styles.size());
				for (StyleImpl style : styles) {
					if (!"burg".equals(style.getName()) && !"capitals".equals(style.getName())) {
						fail("Unexpected style found for layer: " + layer.getName());
					}
				}
			} else if ("sf:bugsites".equals(layer.getName())) {
				List<StyleImpl> styles = layer.getStyles();
				assertEquals("Style count do not match for layer: " + layer.getName(), 2, styles.size());
				for (StyleImpl style : styles) {
					if (!"burg".equals(style.getName()) && !"point".equals(style.getName())) {
						fail("Unexpected style found for layer: " + layer.getName());
					}
				}
			} else if ("sf:restricted".equals(layer.getName())) {
				List<StyleImpl> styles = layer.getStyles();
				assertEquals("Style count do not match for layer: " + layer.getName(), 1, styles.size());
				for (StyleImpl style : styles) {
					if (!"polygon".equals(style.getName())) {
						fail("Unexpected style found for layer: " + layer.getName());
					}
				}
			} else if ("sf:roads".equals(layer.getName())) {
				List<StyleImpl> styles = layer.getStyles();
				assertEquals("Style count do not match for layer: " + layer.getName(), 1, styles.size());
				for (StyleImpl style : styles) {
					if (!"line".equals(style.getName())) {
						fail("Unexpected style found for layer: " + layer.getName());
					}
				}
			} else if ("sf:sfdem".equals(layer.getName())) {
				List<StyleImpl> styles = layer.getStyles();
				assertTrue("Style count do not match for layer: " + layer.getName(), styles == null || styles.isEmpty());
			} else if ("sf:streams".equals(layer.getName())) {
				List<StyleImpl> styles = layer.getStyles();
				assertEquals("Style count do not match for layer: " + layer.getName(), 1, styles.size());
				for (StyleImpl style : styles) {
					if (!"line".equals(style.getName())) {
						fail("Unexpected style found for layer: " + layer.getName());
					}
				}
			} else if ("spearfish".equals(layer.getName())) {
				List<StyleImpl> styles = layer.getStyles();
				assertTrue("Style count do not match for layer: " + layer.getName(), styles == null || styles.isEmpty());
			} else if ("tasmania".equals(layer.getName())) {
				List<StyleImpl> styles = layer.getStyles();
				assertTrue("Style count do not match for layer: " + layer.getName(), styles == null || styles.isEmpty());
			} else if ("tiger-ny".equals(layer.getName())) {
				List<StyleImpl> styles = layer.getStyles();
				assertTrue("Style count do not match for layer: " + layer.getName(), styles == null || styles.isEmpty());
			} else if ("tiger:giant_polygon".equals(layer.getName())) {
				List<StyleImpl> styles = layer.getStyles();
				assertTrue("Style count do not match for layer: " + layer.getName(), styles == null || styles.isEmpty());
			} else if ("tiger:poi".equals(layer.getName())) {
				List<StyleImpl> styles = layer.getStyles();
				assertEquals("Style count do not match for layer: " + layer.getName(), 2, styles.size());
				for (StyleImpl style : styles) {
					if (!"burg".equals(style.getName()) && !"point".equals(style.getName())) {
						fail("Unexpected style found for layer: " + layer.getName());
					}
				}
			} else if ("tiger:poly_landmarks".equals(layer.getName())) {
				List<StyleImpl> styles = layer.getStyles();
				assertEquals("Style count do not match for layer: " + layer.getName(), 3, styles.size());
				for (StyleImpl style : styles) {
					if (!"grass".equals(style.getName()) && !"polygon".equals(style.getName()) && !"restricted".equals(style.getName())) {
						fail("Unexpected style found for layer: " + layer.getName());
					}
				}
			} else if ("tiger:tiger_roads".equals(layer.getName())) {
				List<StyleImpl> styles = layer.getStyles();
				assertEquals("Style count do not match for layer: " + layer.getName(), 2, styles.size());
				for (StyleImpl style : styles) {
					if (!"line".equals(style.getName()) && !"simple_roads".equals(style.getName())) {
						fail("Unexpected style found for layer: " + layer.getName());
					}
				}
			} else if ("topp:states".equals(layer.getName())) {
				List<StyleImpl> styles = layer.getStyles();
				assertEquals("Style count do not match for layer: " + layer.getName(), 2, styles.size());
				for (StyleImpl style : styles) {
					if (!"polygon".equals(style.getName()) && !"pophatch".equals(style.getName())) {
						fail("Unexpected style found for layer: " + layer.getName());
					}
				}
			} else if ("topp:tasmania_cities".equals(layer.getName())) {
				List<StyleImpl> styles = layer.getStyles();
				assertTrue("Style count do not match for layer: " + layer.getName(), styles == null || styles.isEmpty());
			} else if ("topp:tasmania_roads".equals(layer.getName())) {
				List<StyleImpl> styles = layer.getStyles();
				assertTrue("Style count do not match for layer: " + layer.getName(), styles == null || styles.isEmpty());
			} else if ("topp:tasmania_state_boundaries".equals(layer.getName())) {
				List<StyleImpl> styles = layer.getStyles();
				assertTrue("Style count do not match for layer: " + layer.getName(), styles == null || styles.isEmpty());
			} else if ("topp:tasmania_water_bodies".equals(layer.getName())) {
				List<StyleImpl> styles = layer.getStyles();
				assertTrue("Style count do not match for layer: " + layer.getName(), styles == null || styles.isEmpty());
			} else {
				fail("Unexpected layer found: " + layer.getName());
			}
		}
	}
}
