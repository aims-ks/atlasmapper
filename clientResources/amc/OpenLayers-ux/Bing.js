/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2013 Australian Institute of Marine Science
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

/**
 * This method is a copy of OpenLayers 2.12.
 * The modification, removing the protocol from the URL, fixes issue with
 *     browsers (Google Chrome) that has problem loading http resources in
 *     a https page.
 * The fix has been integrated into the OpenLayers core, so it is safe to
 *     remove it with the next release of OpenLayers (2.13+).
 * Issue: https://github.com/openlayers/openlayers/pull/699
 * Patch: https://github.com/fredj/ol3/commit/c38889955c2f53eb9e78b7c7930233935db10b90
 */
OpenLayers.Layer.Bing.prototype.loadMetadata = function() {
	this._callbackId = "_callback_" + this.id.replace(/\./g, "_");
	// link the processMetadata method to the global scope and bind it
	// to this instance
	window[this._callbackId] = OpenLayers.Function.bind(
		OpenLayers.Layer.Bing.processMetadata, this
	);
	var params = OpenLayers.Util.applyDefaults({
		key: this.key,
		jsonp: this._callbackId,
		include: "ImageryProviders"
	}, this.metadataParams);
	/* NOTE: Relative URL without scheme (http or https) use the scheme or the current page.
		This is valid according to the RFC 3986 http://www.ietf.org/rfc/rfc3986.txt */
	var url = "//dev.virtualearth.net/REST/v1/Imagery/Metadata/" +
		this.type + "?" + OpenLayers.Util.getParameterString(params);
	var script = document.createElement("script");
	script.type = "text/javascript";
	script.src = url;
	script.id = this._callbackId;
	document.getElementsByTagName("head")[0].appendChild(script);
}
