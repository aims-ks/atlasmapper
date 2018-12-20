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

package au.gov.aims.atlasmapperserver;

import au.gov.aims.atlasmapperserver.cache.CacheEntry;
import au.gov.aims.atlasmapperserver.cache.URLCache;
import au.gov.aims.atlasmapperserver.thread.RevivableThreadInterruptedException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LocationSearch {
    private static final Logger LOGGER = Logger.getLogger(LocationSearch.class.getName());

    private static final String CACHE_ENTITY_ID = "LOCATION_SEARCH";

    // How long location search stays in cache
    // NOTE: Also see URLCache.EXPIRY_CLEANUP_MINIMUM_DELAY
    private static final long CACHE_EXPIRY = 1000 * 60 * 5; // 5 minutes

    private static long searchCount = 0;
    private static void incSearchCount() {
        // No way! That will never happen!!
        if (searchCount == Long.MAX_VALUE) {
            searchCount = 0;
        }
        searchCount++;
    }

    // Google
    // API: https://developers.google.com/maps/documentation/geocoding/
    // URL: http://maps.googleapis.com/maps/api/geocode/json?address={QUERY}&sensor=false
    public static List<JSONObject> googleSearch(URLCache urlCache, String googleSearchAPIKey, String referer, String encodedQuery, String mapBounds)
            throws JSONException, IOException, URISyntaxException, SQLException, RevivableThreadInterruptedException, ClassNotFoundException {

        String googleSearchUrl = "https://maps.googleapis.com/maps/api/geocode/json?address={QUERY}&sensor=false&key={APIKEY}";

        String encodedGoogleSearchAPIKey = URLEncoder.encode(googleSearchAPIKey.trim(), "UTF-8");
        String queryURLStr = googleSearchUrl
                .replace("{QUERY}", encodedQuery)
                .replace("{APIKEY}", encodedGoogleSearchAPIKey);

        JSONObject json = LocationSearch.getSearchJSONObjectResponse(urlCache, queryURLStr, referer);
        if (json == null) {
            return null;
        }

        // Using the API Key can be messy. The developer can't look at requests / responses in the browser console
        // since the request is sent by the server.
        String errorMessage = json.optString("error_message", null);
        if (errorMessage != null) {
            LOGGER.log(Level.SEVERE, "Google Search API returned an error: " + errorMessage);
        }

        JSONArray jsonResults = json.optJSONArray("results");
        if (jsonResults == null) {
            return null;
        }

        int length=jsonResults.length();
        if (length <= 0) {
            return null;
        }

        incSearchCount();
        List<JSONObject> results = new ArrayList<JSONObject>(length);
        for (int i=0; i<length; i++) {
            JSONObject jsonResult = jsonResults.optJSONObject(i);
            if (jsonResult != null) {
                JSONObject geometry = jsonResult.optJSONObject("geometry");

                String title = jsonResult.optString("formatted_address", null);
                if (title == null) {
                    LOGGER.log(Level.FINEST, "Search results:\n" + json.toString(4));
                    LOGGER.log(Level.WARNING, "UNSUPPORTED SEARCH RESPONSE");
                    title = "Unknown";
                }

                if (geometry != null) {
                    JSONObject location = geometry.optJSONObject("location");
                    JSONObject viewport = geometry.optJSONObject("viewport");
                    if (location != null || viewport != null) {

                        double[] center = null;
                        if (location != null) {
                            center = new double[]{
                                location.optDouble("lng"),
                                location.optDouble("lat")
                            };
                        }

                        double[] bbox = null;
                        if (viewport != null) {
                            JSONObject northeast = viewport.optJSONObject("northeast");
                            JSONObject southwest = viewport.optJSONObject("southwest");
                            if (northeast != null && southwest != null) {
                                // (left, bottom, right, top).
                                bbox = new double[]{
                                    southwest.optDouble("lng"), // West
                                    southwest.optDouble("lat"), // South
                                    northeast.optDouble("lng"), // East
                                    northeast.optDouble("lat")  // North
                                };
                            }
                        }

                        results.add(_createSearchResult(
                                title,
                                searchCount + "_" + results.size(),
                                center,
                                bbox
                        ));
                    }
                }
            }
        }

        return results;
    }

    // OSM (Should be used to give path from A to B, not for a location search...)
    // API: http://open.mapquestapi.com/geocoding/
    // URL: http://open.mapquestapi.com/geocoding/v1/address?location={QUERY}
    public static List<JSONObject> osmSearch(URLCache urlCache, String osmSearchAPIKey, String referer, String encodedQuery, String mapBounds)
            throws JSONException, IOException, URISyntaxException, SQLException, RevivableThreadInterruptedException, ClassNotFoundException {

        String osmSearchUrl = "http://open.mapquestapi.com/geocoding/v1/address?location={QUERY}&key={APIKEY}";

        String encodedOsmSearchAPIKey = URLEncoder.encode(osmSearchAPIKey.trim(), "UTF-8");
        String queryURLStr = osmSearchUrl
                .replace("{QUERY}", encodedQuery)
                .replace("{APIKEY}", encodedOsmSearchAPIKey);

        JSONObject json = LocationSearch.getSearchJSONObjectResponse(urlCache, queryURLStr, referer);
        if (json == null) {
            return null;
        }

        JSONArray jsonResults = json.optJSONArray("results");
        if (jsonResults == null) {
            return null;
        }

        int length=jsonResults.length();
        if (length <= 0) {
            return null;
        }

        incSearchCount();
        List<JSONObject> results = new ArrayList<JSONObject>(length);
        for (int i=0; i<length; i++) {
            JSONObject jsonResult = jsonResults.optJSONObject(i);
            if (jsonResult != null) {

                JSONArray locations = jsonResult.optJSONArray("locations");
                for (int j=0, locLen = locations.length(); j<locLen; j++) {
                    JSONObject location = locations.optJSONObject(j);

                    if (location != null) {
                        JSONObject latLng = location.optJSONObject("latLng");
                        if (latLng != null) {
                            double[] center = {
                                latLng.optDouble("lng"),
                                latLng.optDouble("lat")
                            };

                            String adminArea5 = location.optString("adminArea5", null);
                            String adminArea4 = location.optString("adminArea4", null);
                            String adminArea3 = location.optString("adminArea3", null);
                            String adminArea2 = location.optString("adminArea2", null); // This one seems to never be present...
                            String adminArea1 = location.optString("adminArea1", null);

                            StringBuilder titleBuf = new StringBuilder();
                            if (Utils.isNotBlank(adminArea5)) {
                                titleBuf.append(adminArea5.trim());
                            }
                            if (Utils.isNotBlank(adminArea4)) {
                                if (titleBuf.length() > 0) {
                                    titleBuf.append(", ");
                                }
                                titleBuf.append(adminArea4.trim());
                            }
                            if (Utils.isNotBlank(adminArea3)) {
                                if (titleBuf.length() > 0) {
                                    titleBuf.append(", ");
                                }
                                titleBuf.append(adminArea3.trim());
                            }
                            if (Utils.isNotBlank(adminArea2)) {
                                if (titleBuf.length() > 0) {
                                    titleBuf.append(", ");
                                }
                                titleBuf.append(adminArea2.trim());
                            }
                            if (Utils.isNotBlank(adminArea1)) {
                                if (titleBuf.length() > 0) {
                                    titleBuf.append(", ");
                                }
                                titleBuf.append(adminArea1.trim());
                            }

                            String title = titleBuf.toString();

                            if (Utils.isBlank(title)) {
                                LOGGER.log(Level.FINEST, "Search results:\n" + jsonResults.toString(4));
                                LOGGER.log(Level.WARNING, "UNSUPPORTED SEARCH RESPONSE");
                                title = "Unknown";
                            }

                            results.add(_createSearchResult(
                                    title,
                                    searchCount + "_" + results.size(),
                                    center
                            ));
                        }
                    }
                }
            }
        }

        return results;
    }

    // OSM Nominatim
    // API: https://developer.mapquest.com/documentation/open/nominatim-search/
    // URL: http://open.mapquestapi.com/nominatim/v1/search?format=json&q={QUERY}
    public static List<JSONObject> osmNominatimSearch(URLCache urlCache, String osmSearchAPIKey, String referer, String encodedQuery, String mapBounds)
            throws JSONException, IOException, URISyntaxException, SQLException, RevivableThreadInterruptedException, ClassNotFoundException {

        String osmSearchUrl = "http://open.mapquestapi.com/nominatim/v1/search?format=json&q={QUERY}&key={APIKEY}";

        String encodedOsmSearchAPIKey = URLEncoder.encode(osmSearchAPIKey.trim(), "UTF-8");
        String queryURLStr = osmSearchUrl
                .replace("{QUERY}", encodedQuery)
                .replace("{APIKEY}", encodedOsmSearchAPIKey);

        JSONArray jsonResults = LocationSearch.getSearchJSONArrayResponse(urlCache, queryURLStr, referer);
        if (jsonResults == null) {
            return null;
        }

        int length=jsonResults.length();
        if (length <= 0) {
            return null;
        }

        incSearchCount();
        List<JSONObject> results = new ArrayList<JSONObject>(length);
        for (int i=0; i<length; i++) {
            JSONObject jsonResult = jsonResults.optJSONObject(i);
            if (jsonResult != null) {
                String title = jsonResult.optString("display_name", null);
                if (title == null) {
                    LOGGER.log(Level.FINEST, "Search results:\n" + jsonResults.toString(4));
                    LOGGER.log(Level.WARNING, "UNSUPPORTED SEARCH RESPONSE");
                    title = "Unknown";
                }

                double[] center = {
                    jsonResult.optDouble("lon"),
                    jsonResult.optDouble("lat")
                };

                double[] bbox = null;
                JSONArray boundingbox = jsonResult.optJSONArray("boundingbox");
                if (boundingbox != null && boundingbox.length() == 4) {
                    // (left, bottom, right, top).
                    bbox = new double[]{
                        boundingbox.optDouble(2), // West
                        boundingbox.optDouble(0), // South
                        boundingbox.optDouble(3), // East
                        boundingbox.optDouble(1)  // North
                    };
                }

                results.add(_createSearchResult(
                        title,
                        searchCount + "_" + results.size(),
                        center,
                        bbox
                ));
            }
        }

        return results;
    }

    // Yahoo
    // API: ?
    // URL: ?

    // ArcGIS
    // API: http://resources.arcgis.com/en/help/rest/apiref/index.html?find.html
    // URL example: http://www.gbrmpa.gov.au/spatial_services/gbrmpaBounds/MapServer/find?f=json&contains=true&returnGeometry=true&layers=6%2C0&searchFields=LOC_NAME_L%2CNAME&searchText={QUERY}
    public static List<JSONObject> arcGISSearch(URLCache urlCache, String referer, String arcGISSearchUrl, String encodedQuery, String mapBounds)
            throws JSONException, IOException, TransformException, FactoryException, URISyntaxException, SQLException, RevivableThreadInterruptedException, ClassNotFoundException {

        if (Utils.isBlank(arcGISSearchUrl)) {
            return null;
        }

        arcGISSearchUrl = arcGISSearchUrl.trim();
        String queryURLStr = arcGISSearchUrl.replace("{QUERY}", encodedQuery);

        String searchFieldsStr = Utils.getUrlParameter(queryURLStr, "searchFields", true);
        String[] searchFields = searchFieldsStr.split(",");

        JSONObject json = LocationSearch.getSearchJSONObjectResponse(urlCache, queryURLStr, referer);
        if (json == null) {
            return null;
        }

        JSONArray jsonResults = json.optJSONArray("results");
        if (jsonResults == null) {
            return null;
        }

        int length=jsonResults.length();
        if (length <= 0) {
            return null;
        }

        incSearchCount();
        List<JSONObject> results = new ArrayList<JSONObject>(length);
        for (int i=0; i<length; i++) {
            JSONObject jsonResult = jsonResults.optJSONObject(i);
            if (jsonResult != null) {
                JSONObject attributes = jsonResult.optJSONObject("attributes");
                JSONObject geometry = jsonResult.optJSONObject("geometry");

                double[] center = {
                    geometry.optDouble("x"),
                    geometry.optDouble("y")
                };

                JSONObject spatialReference = geometry.optJSONObject("spatialReference");
                Integer wkid = spatialReference.optInt("wkid");
                double[] reprojectedCenter = Utils.reprojectWKIDCoordinatesToDegrees(center, "EPSG:" + wkid);

                String title = null;
                for (int f=0, flen=searchFields.length; f<flen && title == null; f++) {
                    if (attributes.has(searchFields[f])) {
                        title = attributes.optString(searchFields[f], null);
                    }
                }
                if (title == null) {
                    LOGGER.log(Level.FINEST, "Search results:\n" + json.toString(4));
                    LOGGER.log(Level.WARNING, "UNSUPPORTED SEARCH RESPONSE");
                    title = "Unknown";
                }

                results.add(_createSearchResult(
                        title,
                        searchCount + "_" + results.size(),
                        new double[]{reprojectedCenter[1], reprojectedCenter[0]}
                ));
            }
        }

        return results;
    }

    private static JSONObject getSearchJSONObjectResponse(URLCache urlcache, String urlStr, String referer)
            throws IOException, URISyntaxException, ClassNotFoundException, RevivableThreadInterruptedException, SQLException {

        JSONObject jsonResponse = null;

        URL url = new URL(urlStr);
        CacheEntry cacheEntry = null;
        try {
            cacheEntry = urlcache.getCacheEntry(url);
            if (cacheEntry != null) {
                urlcache.getHttpDocument(cacheEntry, CACHE_ENTITY_ID);
                File jsonFile = cacheEntry.getDocumentFile();
                if (jsonFile != null) {
                    jsonResponse = URLCache.parseJSONObjectFile(jsonFile, null, urlStr);
                }
            }

            long expiryTimestamp = CacheEntry.getCurrentTimestamp() + LocationSearch.CACHE_EXPIRY;
            boolean valid = jsonResponse != null;
            urlcache.save(cacheEntry, valid, expiryTimestamp);
        } finally {
            if (cacheEntry != null) {
                cacheEntry.close();
            }
        }

        return jsonResponse;
    }

    private static JSONArray getSearchJSONArrayResponse(URLCache urlcache, String urlStr, String referer)
            throws IOException, URISyntaxException, ClassNotFoundException, RevivableThreadInterruptedException, SQLException {

        JSONArray jsonResponse = null;

        URL url = new URL(urlStr);
        CacheEntry cacheEntry = null;
        try {
            cacheEntry = urlcache.getCacheEntry(url);
            if (cacheEntry != null) {
                urlcache.getHttpDocument(cacheEntry, CACHE_ENTITY_ID);
                File jsonFile = cacheEntry.getDocumentFile();
                if (jsonFile != null) {
                    jsonResponse = URLCache.parseJSONArrayFile(jsonFile, null, urlStr);
                }
            }

            long expiryTimestamp = CacheEntry.getCurrentTimestamp() + LocationSearch.CACHE_EXPIRY;
            boolean valid = jsonResponse != null;
            urlcache.save(cacheEntry, valid, expiryTimestamp);
        } finally {
            if (cacheEntry != null) {
                cacheEntry.close();
            }
        }

        return jsonResponse;
    }

    public static JSONObject _createSearchResult(String title, String id, double[] center) throws JSONException {
        return _createSearchResult(title, id, center, null);
    }

    /**
     *
     * @param title
     * @param id
     * @param center
     * @param bbox Follow OpenLayers format: left, bottom, right, top.
     * @return
     * @throws JSONException
     */
    public static JSONObject _createSearchResult(String title, String id, double[] center, double[] bbox) throws JSONException {
        JSONObject result = new JSONObject();
        result.put("title", title);
        result.put("id", id);
        if (bbox != null && bbox.length == 4) {
            result.put("bbox", bbox);
        }
        result.put("center", new JSONArray().put(center[0]).put(center[1]));

        return result;
    }
}
