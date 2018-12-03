/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2011 Australian Institute of Marine Science
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
package au.gov.aims.atlasmapperserver.collection;

import au.gov.aims.atlasmapperserver.AbstractConfig;
import au.gov.aims.atlasmapperserver.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class BlackAndWhiteListFilter<E> {
	private static final Logger LOGGER = Logger.getLogger(BlackAndWhiteListFilter.class.getName());

	public static final String BLACK_LIST_PREFIX = "-";
	public static final String WHITE_LIST_PREFIX = "+";
	private List<BlackOrWhiteFilter<E>> filters;

	/**
	 * FiltersStr is a coma/white space separated list of rules;
	 *     Rules starting with a - are Black list rules.
	 *     Rules starting with a + are White list rules.
	 * A suite a rules of the same type (black or white) are part of the same filter.
	 * Example:
	 *     +ea_*
	 *     +wt_*
	 *     -ea_fishes*
	 *     This will add all layers that starts with "ea_" or "wt_" but not "ea_fishes".
	 * @param filtersStr
	 */
	public BlackAndWhiteListFilter(String filtersStr) {
		if (Utils.isNotBlank(filtersStr)) {
			String[] filtersStrArr = filtersStr.split(AbstractConfig.SPLIT_PATTERN);
			this.filters = new ArrayList<BlackOrWhiteFilter<E>>();

			for (String filterStr : filtersStrArr) {
				if (Utils.isNotBlank(filterStr)) {
					filterStr = filterStr.trim();

					if (filterStr.startsWith(WHITE_LIST_PREFIX)) {
						String whiteFilter = filterStr.substring(WHITE_LIST_PREFIX.length());
						if (Utils.isNotBlank(whiteFilter)) {
							this.filters.add(new WhiteFilter<E>(whiteFilter));
						}
					} else if (filterStr.startsWith(BLACK_LIST_PREFIX)) {
						String blackFilter = filterStr.substring(BLACK_LIST_PREFIX.length());
						if (Utils.isNotBlank(blackFilter)) {
							this.filters.add(new BlackFilter<E>(filterStr.substring(BLACK_LIST_PREFIX.length())));
						}
					} else {
						LOGGER.log(Level.WARNING, "DEPRECATED BLACK/WHITE FILTER [{0}]: Filter must start with \"-\" or \"+\". The current filter is considered as a black list filter.", filterStr);
						this.filters.add(new BlackFilter<E>(filterStr));
					}
				}
			}
		}
	}

	/**
	 * For debugging
	 * @return
	 */
	protected List<String> getFilters() {
		if (this.filters == null) {
			return null;
		}
		List<String> filtersCopy = new ArrayList<String>(this.filters.size());
		for (BlackOrWhiteFilter<E> filter : this.filters) {
			String prefix = filter instanceof WhiteFilter ? "[+]" : "[-]";
			filtersCopy.add(prefix + " " + filter.getFilter());
		}
		return filtersCopy;
	}

	public HashMap<String, E> filter(HashMap<String, E> initialSet) {
		HashMap<String, E> unfilteredSet = (HashMap<String, E>)initialSet.clone();
		this.filterInPlace(initialSet, unfilteredSet);
		return unfilteredSet;
	}

	/**
	 * Filter the unfiltered set, altering the input.
	 * @param initialMap Complete set used to add elements, according to white list filters.
	 * @param unfilteredMap Set to filter out
	 */
	public void filterInPlace(HashMap<String, E> initialMap, HashMap<String, E> unfilteredMap) {
		if (this.filters != null) {
			for (BlackOrWhiteFilter filter : this.filters) {
				filter.filter(initialMap, unfilteredMap);
			}
		}
	}

	private static abstract class BlackOrWhiteFilter<E> {
		protected String filterStr;
		protected Pattern filterPattern = null;

		public BlackOrWhiteFilter(String filterStr) {
			this.filterStr = filterStr;
			if (filterStr.contains("*")) {
				this.filterPattern = toPattern(filterStr);
			}
		}

		public String getFilter() {
			return this.filterStr;
		}

		public abstract void filter(final HashMap<String, E> initialSet, HashMap<String, E> unfilteredSet);

		// Very basic pattern generator that use * as a wildcard.
		private static Pattern toPattern(String patternStr) {
			StringBuilder sb = new StringBuilder("^");

			if ("*".equals(patternStr)) {
				sb.append(".*");
			} else {
				String[] patternParts = patternStr.split("\\*");

				if (patternStr.startsWith("*")) {
					sb.append(".*");
				}

				// This take an array of String and join them together using ".*" as glue
				// and "Pattern.quote" on string parts:
				// Example:
				// ["str1", "str2", "str3"] => Pattern.quote("str1") + ".*" + Pattern.quote("str2") + ".*" + Pattern.quote("str3")
				// NOTE: Java API do not have a native "join" method to do this.
				boolean firstPart = true;
				for (String patternPart : patternParts) {
					if (firstPart) {
						firstPart = false;
					} else {
						sb.append(".*");
					}
					// Quote everything between * (quote mean that it escape everything)
					sb.append(Pattern.quote(patternPart));
				}

				if (patternStr.endsWith("*")) {
					sb.append(".*");
				}
			}
			sb.append("$");

			return Pattern.compile(sb.toString());
		}
	}


	private static class BlackFilter<E> extends BlackOrWhiteFilter<E> {
		public BlackFilter(String filterStr) {
			super(filterStr);
		}

		public void filter(final HashMap<String, E> initialMap, HashMap<String, E> unfilteredMap) {
			// Remove all layers that match the filter from the unfiltered collection. Ignore the initial collection.
			if (this.filterPattern != null) {
				Iterator<Map.Entry<String, E>> keysItr = unfilteredMap.entrySet().iterator();
				while (keysItr.hasNext()) {
					Map.Entry<String, E> elementEntry = keysItr.next();
					if (elementEntry == null || this.filterPattern.matcher(elementEntry.getKey()).matches()) {
						keysItr.remove();
					}
				}
			} else {
				unfilteredMap.remove(this.filterStr);
			}
		}
	}


	private static class WhiteFilter<E> extends BlackOrWhiteFilter<E> {
		public WhiteFilter(String filterStr) {
			super(filterStr);
		}

		public void filter(final HashMap<String, E> initialMap, HashMap<String, E> unfilteredMap) {
			// All all layers that match the filter from the initial collection to the unfiltered collection.
			if (this.filterPattern != null) {
				for (Map.Entry<String, E> elementEntry : initialMap.entrySet()) {
					if (this.filterPattern.matcher(elementEntry.getKey()).matches()) {
						unfilteredMap.put(elementEntry.getKey(), elementEntry.getValue());
					}
				}
			} else {
				E element = initialMap.get(this.filterStr);
				if (element != null) {
					unfilteredMap.put(this.filterStr, element);
				}
			}
		}
	}
}
