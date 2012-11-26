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

public class Document {
	// For logging purpose
	private String uri;

	// Attributes from the XML doc
	private String _abstract;
	private List<Link> links;

	public Document(String uri) {
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


	public static class Link {
		private static final String WMS_GET_MAP_PROTOCOL = "OGC:WMS-1.1.1-http-get-map";

		private String url;
		private String protocol;
		private String name;
		private String description;
		private String applicationProfile;

		public String getUrl() {
			return this.url;
		}
		public void setUrl(String url) {
			this.url = url;
		}

		public String getProtocol() {
			return this.protocol;
		}
		public void setProtocol(String protocol) {
			this.protocol = protocol;
		}

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

		public boolean isWMSGetMapLink() {
			return WMS_GET_MAP_PROTOCOL.equalsIgnoreCase(this.protocol);
		}

		@Override
		public String toString() {
			return "		Document.Link {\n" +
					(Utils.isBlank(this.getUrl()) ? "" :               "			url=" + this.getUrl() + "\n") +
					(Utils.isBlank(this.getProtocol()) ? "" :          "			protocol=" + this.getProtocol() + "\n") +
					(Utils.isBlank(this.getName()) ? "" :              "			name=" + this.getName() + "\n") +
					(Utils.isBlank(this.getDescription()) ? "" :       "			description=" + this.getDescription() + "\n") +
					(Utils.isBlank(this.getApplicationProfile()) ? "" :"			applicationProfile=" + this.getApplicationProfile() + "\n") +
					"		}";
		}
	}
}
