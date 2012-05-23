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

package au.gov.aims.atlasmapperserver.collection;

import junit.framework.TestCase;

import java.util.HashMap;
import java.util.List;

public class BlackAndWhiteListFilterTest extends TestCase {

	public void testFilterParsing() throws Exception {
		String filtersStr = "-*, +ea_*, +wt_*, -ea_ab*, +ea_abc*";
		BlackAndWhiteListFilter<Object> bwList = new BlackAndWhiteListFilter<Object>(filtersStr);

		List<String> bwListFilters = bwList.getFilters();

		// Print out the actual list
		/*
		for (String filter : bwListFilters) {
			System.out.println(filter);
		}
		*/

		assertEquals(5, bwListFilters.size());
		assertEquals("[-] *", bwListFilters.get(0));
		assertEquals("[+] ea_*", bwListFilters.get(1));
		assertEquals("[+] wt_*", bwListFilters.get(2));
		assertEquals("[-] ea_ab*", bwListFilters.get(3));
		assertEquals("[+] ea_abc*", bwListFilters.get(4));
	}

	/**
	 * Initial list:
	 *     ea_layer1, ea_layer2, ea_layer3
	 *     ea_ab1, ea_ab2, ea_ab3
	 *     ea_abc1, ea_abc2, ea_abc3
	 *     wt_layer1, wt_layer2, wt_layer3
	 *     wt_ab1, wt_ab2, wt_ab3
	 *     xx_layer1, xx_layer2, xx_layer3
	 *
	 * Filter -*
	 * Current list:
	 *
	 * Filter +ea_*
	 * Current list:
	 *     ea_layer1, ea_layer2, ea_layer3
	 *     ea_ab1, ea_ab2, ea_ab3
	 *     ea_abc1, ea_abc2, ea_abc3
	 *
	 * Filter +wt_*
	 * Current list:
	 *     ea_layer1, ea_layer2, ea_layer3
	 *     ea_ab1, ea_ab2, ea_ab3
	 *     ea_abc1, ea_abc2, ea_abc3
	 *     wt_layer1, wt_layer2, wt_layer3
	 *     wt_ab1, wt_ab2, wt_ab3
	 *
	 * Filter -ea_ab*
	 * Current list:
	 *     ea_layer1, ea_layer2, ea_layer3
	 *     wt_layer1, wt_layer2, wt_layer3
	 *     wt_ab1, wt_ab2, wt_ab3
	 *
	 * Filter +ea_abc*
	 * Current list:
	 *     ea_layer1, ea_layer2, ea_layer3
	 *     wt_layer1, wt_layer2, wt_layer3
	 *     wt_ab1, wt_ab2, wt_ab3
	 *     ea_abc1, ea_abc2, ea_abc3
	 */
	public void testFiltering() throws Exception {
		HashMap<String, String> layerCatalog = new HashMap<String, String>();
		layerCatalog.put("ea_layer1", "EA Layer 1");
		layerCatalog.put("ea_layer2", "EA Layer 2");
		layerCatalog.put("ea_layer3", "EA Layer 3");

		layerCatalog.put("ea_ab1", "EA Layer AB 1");
		layerCatalog.put("ea_ab2", "EA Layer AB 2");
		layerCatalog.put("ea_ab3", "EA Layer AB 3");

		layerCatalog.put("ea_abc1", "EA Layer ABC 1");
		layerCatalog.put("ea_abc2", "EA Layer ABC 2");
		layerCatalog.put("ea_abc3", "EA Layer ABC 3");

		layerCatalog.put("wt_layer1", "WT Layer 1");
		layerCatalog.put("wt_layer2", "WT Layer 2");
		layerCatalog.put("wt_layer3", "WT Layer 3");

		layerCatalog.put("wt_ab1", "WT Layer AB 1");
		layerCatalog.put("wt_ab2", "WT Layer AB 2");
		layerCatalog.put("wt_ab3", "WT Layer AB 3");

		layerCatalog.put("xx_layer1", "XX Layer 1");
		layerCatalog.put("xx_layer2", "XX Layer 2");
		layerCatalog.put("xx_layer3", "XX Layer 3");

		// Start with fill list
		String filtersStr = "-*, +ea_*, +wt_*, -ea_ab*, +ea_abc*";
		BlackAndWhiteListFilter<String> bwList = new BlackAndWhiteListFilter<String>(filtersStr);

		HashMap filteredCatalog = bwList.filter(layerCatalog);

		assertTrue(filteredCatalog.containsKey("ea_layer1"));
		assertEquals("EA Layer 1", filteredCatalog.get("ea_layer1"));
		assertTrue(filteredCatalog.containsKey("ea_layer2"));
		assertEquals("EA Layer 2", filteredCatalog.get("ea_layer2"));
		assertTrue(filteredCatalog.containsKey("ea_layer3"));
		assertEquals("EA Layer 3", filteredCatalog.get("ea_layer3"));

		assertFalse(filteredCatalog.containsKey("ea_ab1"));
		assertFalse(filteredCatalog.containsKey("ea_ab2"));
		assertFalse(filteredCatalog.containsKey("ea_ab3"));

		assertTrue(filteredCatalog.containsKey("ea_abc1"));
		assertEquals("EA Layer ABC 1", filteredCatalog.get("ea_abc1"));
		assertTrue(filteredCatalog.containsKey("ea_abc2"));
		assertEquals("EA Layer ABC 2", filteredCatalog.get("ea_abc2"));
		assertTrue(filteredCatalog.containsKey("ea_abc3"));
		assertEquals("EA Layer ABC 3", filteredCatalog.get("ea_abc3"));

		assertTrue(filteredCatalog.containsKey("wt_layer1"));
		assertEquals("WT Layer 1", filteredCatalog.get("wt_layer1"));
		assertTrue(filteredCatalog.containsKey("wt_layer2"));
		assertEquals("WT Layer 2", filteredCatalog.get("wt_layer2"));
		assertTrue(filteredCatalog.containsKey("wt_layer3"));
		assertEquals("WT Layer 3", filteredCatalog.get("wt_layer3"));

		assertTrue(filteredCatalog.containsKey("wt_ab1"));
		assertEquals("WT Layer AB 1", filteredCatalog.get("wt_ab1"));
		assertTrue(filteredCatalog.containsKey("wt_ab2"));
		assertEquals("WT Layer AB 2", filteredCatalog.get("wt_ab2"));
		assertTrue(filteredCatalog.containsKey("wt_ab3"));
		assertEquals("WT Layer AB 3", filteredCatalog.get("wt_ab3"));

		assertFalse(filteredCatalog.containsKey("xx_layer1"));
		assertFalse(filteredCatalog.containsKey("xx_layer2"));
		assertFalse(filteredCatalog.containsKey("xx_layer3"));
	}

	/**
	 * Initial list:
	 *     ea_layer1, ea_layer2, ea_layer3
	 *     ea_ab1, ea_ab2, ea_ab3
	 *     ea_abc1, ea_abc2, ea_abc3
	 *     wt_layer1, wt_layer2, wt_layer3
	 *     wt_ab1, wt_ab2, wt_ab3
	 *     xx_layer1, xx_layer2, xx_layer3
	 *
	 * Filter -ea_*
	 * Current list:
	 *     wt_layer1, wt_layer2, wt_layer3
	 *     wt_ab1, wt_ab2, wt_ab3
	 *     xx_layer1, xx_layer2, xx_layer3
	 *
	 * Filter -wt_*
	 * Current list:
	 *     xx_layer1, xx_layer2, xx_layer3
	 *
	 * Filter +ea_ab*
	 * Current list:
	 *     xx_layer1, xx_layer2, xx_layer3
	 *     ea_ab1, ea_ab2, ea_ab3
	 *     ea_abc1, ea_abc2, ea_abc3
	 *
	 * Filter -ea_abc*
	 * Current list:
	 *     xx_layer1, xx_layer2, xx_layer3
	 *     ea_ab1, ea_ab2, ea_ab3
	 */
	public void testFiltering2() throws Exception {
		HashMap<String, String> layerCatalog = new HashMap<String, String>();
		layerCatalog.put("ea_layer1", "EA Layer 1");
		layerCatalog.put("ea_layer2", "EA Layer 2");
		layerCatalog.put("ea_layer3", "EA Layer 3");

		layerCatalog.put("ea_ab1", "EA Layer AB 1");
		layerCatalog.put("ea_ab2", "EA Layer AB 2");
		layerCatalog.put("ea_ab3", "EA Layer AB 3");

		layerCatalog.put("ea_abc1", "EA Layer ABC 1");
		layerCatalog.put("ea_abc2", "EA Layer ABC 2");
		layerCatalog.put("ea_abc3", "EA Layer ABC 3");

		layerCatalog.put("wt_layer1", "WT Layer 1");
		layerCatalog.put("wt_layer2", "WT Layer 2");
		layerCatalog.put("wt_layer3", "WT Layer 3");

		layerCatalog.put("wt_ab1", "WT Layer AB 1");
		layerCatalog.put("wt_ab2", "WT Layer AB 2");
		layerCatalog.put("wt_ab3", "WT Layer AB 3");

		layerCatalog.put("xx_layer1", "XX Layer 1");
		layerCatalog.put("xx_layer2", "XX Layer 2");
		layerCatalog.put("xx_layer3", "XX Layer 3");

		// Start with fill list
		String filtersStr = "-ea_*, -wt_*, +ea_ab*, -ea_abc*";
		BlackAndWhiteListFilter<String> bwList = new BlackAndWhiteListFilter<String>(filtersStr);

		HashMap filteredCatalog = bwList.filter(layerCatalog);

		assertFalse(filteredCatalog.containsKey("ea_layer1"));
		assertFalse(filteredCatalog.containsKey("ea_layer2"));
		assertFalse(filteredCatalog.containsKey("ea_layer3"));

		assertTrue(filteredCatalog.containsKey("ea_ab1"));
		assertEquals("EA Layer AB 1", filteredCatalog.get("ea_ab1"));
		assertTrue(filteredCatalog.containsKey("ea_ab2"));
		assertEquals("EA Layer AB 2", filteredCatalog.get("ea_ab2"));
		assertTrue(filteredCatalog.containsKey("ea_ab3"));
		assertEquals("EA Layer AB 3", filteredCatalog.get("ea_ab3"));

		assertFalse(filteredCatalog.containsKey("ea_abc1"));
		assertFalse(filteredCatalog.containsKey("ea_abc2"));
		assertFalse(filteredCatalog.containsKey("ea_abc3"));

		assertFalse(filteredCatalog.containsKey("wt_layer1"));
		assertFalse(filteredCatalog.containsKey("wt_layer2"));
		assertFalse(filteredCatalog.containsKey("wt_layer3"));

		assertFalse(filteredCatalog.containsKey("wt_ab1"));
		assertFalse(filteredCatalog.containsKey("wt_ab2"));
		assertFalse(filteredCatalog.containsKey("wt_ab3"));

		assertTrue(filteredCatalog.containsKey("xx_layer1"));
		assertEquals("XX Layer 1", filteredCatalog.get("xx_layer1"));
		assertTrue(filteredCatalog.containsKey("xx_layer2"));
		assertEquals("XX Layer 2", filteredCatalog.get("xx_layer2"));
		assertTrue(filteredCatalog.containsKey("xx_layer3"));
		assertEquals("XX Layer 3", filteredCatalog.get("xx_layer3"));
	}
}
