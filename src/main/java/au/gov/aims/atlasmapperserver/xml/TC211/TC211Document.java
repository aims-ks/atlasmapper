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
package au.gov.aims.atlasmapperserver.xml.TC211;

import au.gov.aims.atlasmapperserver.Utils;

import java.util.ArrayList;
import java.util.List;

public class TC211Document {
	// For logging purpose
	private String uri;

	// Attributes from the XML doc
	private String _abstract;
	private List<Link> links;
	private List<Point> points;
	// List of polygons, not closed. It's more efficient to close them with OpenLayers
	private List<Polygon> polygons;

	public TC211Document(String uri) {
		this.uri = uri;
	}

	public String getUri() {
		return this.uri;
	}

	public String getAbstract() {
		return this._abstract;
	}
	public void setAbstract(String _abstract) {
		this._abstract = _abstract;
	}

	public List<Link> getLinks() {
		return this.links;
	}
	public void setLinks(List<Link> links) {
		this.links = links;
	}
	public void addLink(Link link) {
		if (this.links == null) {
			this.links = new ArrayList<Link>();
		}
		this.links.add(link);
	}

	public List<Point> getPoints() {
		return this.points;
	}
	public void setPoints(List<Point> points) {
		this.points = points;
	}
	public void addPoint(Point point) {
		if (this.points == null) {
			this.points = new ArrayList<Point>();
		}
		this.points.add(point);
	}

	public List<Polygon> getPolygons() {
		return this.polygons;
	}
	public void setPolygons(List<Polygon> polygons) {
		this.polygons = polygons;
	}
	public void addPolygon(Polygon polygon) {
		if (this.polygons == null) {
			this.polygons = new ArrayList<Polygon>();
		}
		this.polygons.add(polygon);
	}

	@Override
	public String toString() {
		String linksStr = "";
		List<Link> _links = this.getLinks();
		if (_links != null) {
			for (Link link : this.getLinks()) {
				linksStr += link.toString() + "\n";
			}
		}

		return "Document {\n" +
				(Utils.isBlank(this.getUri()) ? "" :       "	uri=" + this.getUri() + "\n") +
				(Utils.isBlank(this.getAbstract()) ? "" :  "	abstract=" + this.getAbstract() + "\n") +
				(Utils.isBlank(linksStr) ? "" :            "	links=[\n" + linksStr + "	]\n") +
				"}";
	}

	public boolean isEmpty() {
		// Attributes from the XML doc
		if (Utils.isNotBlank(this._abstract)) {
			return false;
		}
		if (this.links != null && !this.links.isEmpty()) {
			return false;
		}
		if (this.points != null && !this.points.isEmpty()) {
			return false;
		}
		if (this.polygons != null && !this.polygons.isEmpty()) {
			return false;
		}

		return true;
	}

	public static class Link {
		private static final String WMS_GET_MAP_PROTOCOL = "OGC:WMS-1.1.1-http-get-map";

		private String url;
		private String protocolStr;
		private Protocol protocol;
		private String name;
		private String description;
		private String applicationProfile;

		public String getUrl() {
			return this.url;
		}
		public void setUrl(String url) {
			this.url = url;
		}

		public String getProtocolStr() {
			return this.protocolStr;
		}
		public void setProtocolStr(String protocolStr) {
			this.protocolStr = protocolStr;
			this.protocol = Protocol.parseProtocol(protocolStr);
		}

		public Protocol getProtocol() {
			return this.protocol;
		}

		// The Protocol is not an obvious String. This helper is used to help determine what is what

		public String getName() {
			return this.name;
		}
		public void setName(String name) {
			this.name = name;
		}

		public String getDescription() {
			return this.description;
		}
		public void setDescription(String description) {
			this.description = description;
		}

		public String getApplicationProfile() {
			return this.applicationProfile;
		}
		public void setApplicationProfile(String applicationProfile) {
			this.applicationProfile = applicationProfile;
		}

		@Override
		public String toString() {
			return "		Document.Link {\n" +
					(Utils.isBlank(this.getUrl()) ? "" :               "			url=" + this.getUrl() + "\n") +
					(this.getProtocol() == null ? "" :                 "			protocol=" + this.getProtocol().toString() + "\n") +
					(Utils.isBlank(this.getName()) ? "" :              "			name=" + this.getName() + "\n") +
					(Utils.isBlank(this.getDescription()) ? "" :       "			description=" + this.getDescription() + "\n") +
					(Utils.isBlank(this.getApplicationProfile()) ? "" :"			applicationProfile=" + this.getApplicationProfile() + "\n") +
					"		}";
		}
	}

	public static enum Protocol {
		METADATA_URL                              ("WWW:LINK-1.0-http--metadata-URL"),      // Metadata URL (usually used for the "Point of truth")
		WEB_ADDRESS_URL                           ("WWW:LINK-1.0-http--link"),              // Web address (URL)
		DATA_FOR_DOWNLOAD_URL                     ("WWW:LINK-1.0-http--downloaddata"),      // Data for download (URL)
		SHOWCASE_PRODUCT_URL                      ("WWW:LINK-1.0-http--samples"),           // Showcase product (URL)
		RELATED_LINK_URL                          ("WWW:LINK-1.0-http--related"),           // Related link (URL)
		PARTNER_WEB_ADDRESS_URL                   ("WWW:LINK-1.0-http--partners"),          // Partner web address (URL)
		RSS_NEWS_FEED_URL                         ("WWW:LINK-1.0-http--rss"),               // RSS News feed (URL)
		ICALENDAR_URL                             ("WWW:LINK-1.0-http--ical"),              // iCalendar (URL)
		FILE_FOR_DOWNLOAD                         ("WWW:DOWNLOAD-1.0-http--download"),      // File for download
		DATA_FILE_FOR_DOWNLOAD                    ("WWW:DOWNLOAD-1.0-http--downloaddata"),  // Data File for download
		OTHER_FILE_FOR_DOWNLOAD                   ("WWW:DOWNLOAD-1.0-http--downloadother"), // Other File for download
		DATA_FILE_FOR_DOWNLOAD_THROUGH_FTP        ("WWW:DOWNLOAD-1.0-ftp--downloaddata"),   // Data File for download through FTP
		OTHER_FILE_FOR_DOWNLOAD_THROUGH_FTP       ("WWW:DOWNLOAD-1.0-ftp--downloadother"),  // Other File for download through FTP
		OGC_WEB_MAP_SERVICE_VER_1_1_1             ("OGC:WMS-1.1.1-http-get-map"),           // OGC Web Map Service (ver 1.1.1)
		OGC_WEB_MAP_SERVICE_FILTERED_VER_1_1_1    ("OGC:WMS-1.1.1-http-get-map-filtered"),  // OGC Web Map Service Filtered (ver 1.1.1)
		OGC_WMS_CAPABILITIES_SERVICE_VER_1_1_1    ("OGC:WMS-1.1.1-http-get-capabilities"),  // OGC-WMS Capabilities service (ver 1.1.1)
		OGC_WFS_WEB_FEATURE_SERVICE_VER_1_0_0     ("OGC:WFS-1.0.0-http-get-capabilities"),  // OGC-WFS Web Feature Service (ver 1.0.0)
		OGC_WCS_WEB_COVERAGE_SERVICE_VER_1_1_0    ("OGC:WCS-1.1.0-http-get-capabilities"),  // OGC-WCS Web Coverage Service (ver 1.1.0)
		OGC_WMC_WEB_MAP_CONTEXT_VER_1_1           ("OGC:WMC-1.1.0-http-get-capabilities"),  // OGC-WMC Web Map Context (ver 1.1)
		GOOGLE_EARTH_KML_SERVICE_VER_2_0          ("GLG:KML-2.0-http-get-map"),             // Google Earth KML service (ver 2.0)
		// Some none standard protocols
		ARCIMS_MAP_SERVICE_CONFIGURATION_FILE_AXL ("ESRI:AIMS--http--configuration"),       // ArcIMS Map Service Configuration File (*.AXL)
		ARCIMS_INTERNET_IMAGE_MAP_SERVICE         ("ESRI:AIMS--http-get-image"),            // ArcIMS Internet Image Map Service
		ARCIMS_INTERNET_FEATURE_MAP_SERVICE       ("ESRI:AIMS--http-get-feature");          // ArcIMS Internet Feature Map Service

		private final String identifier;
		private Protocol(String identifier) {
			this.identifier = identifier;
		}

		public static Protocol parseProtocol(String identifier) {
			if (identifier == null) {
				return null;
			}

			for (Protocol p : Protocol.values()) {
				if (identifier.equalsIgnoreCase(p.identifier)) {
					return p;
				}
			}

			return null;
		}

		public boolean isWWW() {
			return this.identifier.startsWith("WWW:");
		}

		public boolean isOGC() {
			return this.identifier.startsWith("OGC:");
		}

		public boolean isKML() {
			return this.identifier.startsWith("GLG:KML");
		}
	}

	public static class Point {
		private double lon, lat, elevation;

		public Point(double lon, double lat) {
			this(lon, lat, 0);
		}
		public Point(double lon, double lat, double elevation) {
			this.lon = lon;
			this.lat = lat;
			this.elevation = elevation;
		}

		public double getLon() {
			return this.lon;
		}
		public double getLat() {
			return this.lat;
		}
		public double getElevation() {
			return this.elevation;
		}
	}

	public static class Polygon {
		List<Point> points;

		public Polygon() {
			this.points = new ArrayList<Point>();
		}

		public void addPoint(double lon, double lat) {
			this.addPoint(new Point(lon, lat));
		}
		public void addPoint(double lon, double lat, double elevation) {
			this.addPoint(new Point(lon, lat, elevation));
		}
		public void addPoint(Point point) {
			this.points.add(point);
		}

		public List<Point> getPoints() {
			return this.points;
		}
	}
}
