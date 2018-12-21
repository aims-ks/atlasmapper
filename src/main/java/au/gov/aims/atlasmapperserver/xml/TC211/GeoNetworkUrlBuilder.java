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
package au.gov.aims.atlasmapperserver.xml.TC211;

import au.gov.aims.atlasmapperserver.Utils;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Take a broken / deprecated GeoNetwork MEST URL and attempt to craft a working URL with it.
 */
public class GeoNetworkUrlBuilder {
    private URL originalUrl;        // https://www.domain.com/records/metadata/geonetwork/srv/eng/xml_iso19139.mcp?uuid=1a46774e-a3ac-4982-b08b-94ce1ad8d45c&styleSheet=xml_iso19139.mcp.xsl
    private String protocol;        // https
    private Integer port;           // null (using default)
    private String host;            // www.domain.com

    private String path;            // records/metadata/geonetwork/srv/eng/xml_iso19139.mcp
    private String pathPrefix;      // records/metadata
    private String pathWebappName;  // geonetwork
    private String pathServer;      // srv
    private String pathLang;        // eng
    private String pathEndPoint;    // xml_iso19139.mcp

    private String queryString;     // uuid=1a46774e-a3ac-4982-b08b-94ce1ad8d45c&styleSheet=xml_iso19139.mcp.xsl
    private Map<String, String> queryParameters; // { "uuid": "1a46774e-a3ac-4982-b08b-94ce1ad8d45c", "styleSheet": "xml_iso19139.mcp.xsl" }

    public GeoNetworkUrlBuilder(URL brokenUrl) throws UnsupportedEncodingException {
        this.originalUrl = brokenUrl;
        this.parse();
    }

    private void parse() throws UnsupportedEncodingException {
        if (this.originalUrl == null) {
            return;
        }

        // Protocol (http or https)
        this.protocol = this.originalUrl.getProtocol();

        // Port (80, 443, null if using default for given protocol)
        int rawPort = this.originalUrl.getPort();
        this.port = rawPort > 0 ? rawPort : null;

        // Host (www.domain.com)
        this.host = this.originalUrl.getHost();

        // Path (geonetwork/srv/en/metadata.show)
        this.path = this.originalUrl.getPath();
        if (Utils.isNotBlank(this.path)) {
            String pathParts[] = this.path.split("/");
            if (pathParts.length >= 4) {
                this.pathEndPoint = pathParts[pathParts.length-1];
                this.pathLang = pathParts[pathParts.length-2];
                this.pathServer = pathParts[pathParts.length-3];
                this.pathWebappName = pathParts[pathParts.length-4];

                // Build the prefix with the unused parts at the beginning
                if (pathParts.length > 4) {
                    StringBuilder prefixBuilder = new StringBuilder(pathParts[0]);
                    for (int i=1; i<pathParts.length-4; i++) {
                        prefixBuilder.append("/").append(pathParts[i]);
                    }

                    this.pathPrefix = prefixBuilder.toString();
                }
            }
        }

        this.queryString = this.originalUrl.getQuery();
        if (Utils.isNotBlank(this.queryString)) {
            this.queryParameters = new HashMap<String, String>();
            String[] parameters = this.queryString.split("&");
            for (String parameter : parameters) {
                int equalIdx = parameter.indexOf("=");
                String key = null;
                String value = null;
                if (equalIdx < 0) {
                    key = URLDecoder.decode(parameter, "UTF-8");
                } else {
                    key = URLDecoder.decode(parameter.substring(0, equalIdx), "UTF-8");
                    value = URLDecoder.decode(parameter.substring(equalIdx + 1), "UTF-8");
                }
                if (key != null) {
                    this.queryParameters.put(key.trim().toLowerCase(), value);
                }
            }
        }
    }

    public URL getOriginalUrl() {
        return this.originalUrl;
    }

    public boolean isValidGeoNetworkUrl() {
        boolean valid = this.originalUrl != null &&
                this.protocol != null &&
                this.host != null &&
                this.path != null &&
                this.pathWebappName != null &&
                this.pathServer != null &&
                this.pathLang != null &&
                this.pathEndPoint != null &&
                this.queryString != null &&
                this.queryParameters != null;

        if (!valid) {
            return false;
        }

        // Check for parameter ID or UUID
        return this.queryParameters.containsKey("id") || this.queryParameters.containsKey("uuid");
    }

    // URL for GeoNetwork prior to version 2.10
    // Example: http://www.cmar.csiro.au/geonetwork/srv/en/iso19139.xml?id=44003&currTab=full
    public URL craftGeoNetworkLegacyMestUrl() throws UnsupportedEncodingException, MalformedURLException {
        if (!this.isValidGeoNetworkUrl()) {
            return null;
        }

        StringBuilder urlFileSb = new StringBuilder();
        if (this.pathPrefix != null) {
            urlFileSb.append(this.pathPrefix).append("/");
        }
        urlFileSb.append(this.pathWebappName).append("/")
                .append(this.pathServer)
                .append("/en/iso19139.xml");

        // Add query parameters
        urlFileSb.append("?");
        if (this.queryParameters.containsKey("uuid")) {
            urlFileSb.append("uuid=").append(URLEncoder.encode(this.queryParameters.get("uuid"), "UTF-8"));
        } else if (this.queryParameters.containsKey("id")) {
            urlFileSb.append("id=").append(URLEncoder.encode(this.queryParameters.get("id"), "UTF-8"));
        }

        // currTab parameter is unnecessary

        return this.craftUrl(urlFileSb.toString());
    }

    // URL for GeoNetwork version 2.10 and upward
    // Example: https://eatlas.org.au/geonetwork/srv/eng/xml_iso19139.mcp?uuid=1a46774e-a3ac-4982-b08b-94ce1ad8d45c&styleSheet=xml_iso19139.mcp.xsl
    public URL craftGeoNetwork2_10MestUrl() throws UnsupportedEncodingException, MalformedURLException {
        if (!this.isValidGeoNetworkUrl()) {
            return null;
        }

        StringBuilder urlFileSb = new StringBuilder();
        if (this.pathPrefix != null) {
            urlFileSb.append(this.pathPrefix).append("/");
        }
        urlFileSb.append(this.pathWebappName).append("/")
                .append(this.pathServer)
                .append("/eng/xml_iso19139.mcp");

        // Add query parameters
        urlFileSb.append("?");
        if (this.queryParameters.containsKey("uuid")) {
            urlFileSb.append("uuid=").append(URLEncoder.encode(this.queryParameters.get("uuid"), "UTF-8"));
        } else if (this.queryParameters.containsKey("id")) {
            urlFileSb.append("id=").append(URLEncoder.encode(this.queryParameters.get("id"), "UTF-8"));
        }

        // The styleSheet parameter is mandatory
        urlFileSb.append("&styleSheet=xml_iso19139.mcp.xsl");

        return this.craftUrl(urlFileSb.toString());
    }

    private URL craftUrl(String file) throws MalformedURLException {
        if (this.port == null) {
            return new URL(
                this.protocol,
                this.host,
                file
            );
        } else {
            return new URL(
                this.protocol,
                this.host,
                this.port,
                file
            );
        }
    }
}
