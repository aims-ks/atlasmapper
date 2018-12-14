/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2018 Australian Institute of Marine Science
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

package au.gov.aims.atlasmapperserver.cache;

import au.gov.aims.atlasmapperserver.Utils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CacheEntry {
    private static final Logger LOGGER = Logger.getLogger(CacheEntry.class.getName());

    private URL url;
    private Long downloadTimestamp;
    private Long lastAccessTimestamp;
    private Long expiryTimestamp;
    private Short httpStatusCode;
    private Boolean valid;
    private File documentFile;

    public CacheEntry(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("URL must not be null.");
        }
        this.url = url;
    }

    public URL getUrl() {
        return this.url;
    }

    public Long getDownloadTimestamp() {
        return this.downloadTimestamp;
    }

    public void setDownloadTimestamp(Long downloadTimestamp) {
        this.downloadTimestamp = downloadTimestamp;
    }

    public Long getLastAccessTimestamp() {
        return this.lastAccessTimestamp;
    }

    public void setLastAccessTimestamp(Long lastAccessTimestamp) {
        this.lastAccessTimestamp = lastAccessTimestamp;
    }

    public Long getExpiryTimestamp() {
        return this.expiryTimestamp;
    }

    public void setExpiryTimestamp(Long expiryTimestamp) {
        this.expiryTimestamp = expiryTimestamp;
    }

    public Short getHttpStatusCode() {
        return this.httpStatusCode;
    }

    public void setHttpStatusCode(Short httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }
    // Helper method because it's annoying to cast everything into "short"
    public void setHttpStatusCode(Integer httpStatusCode) {
        this.httpStatusCode = httpStatusCode == null ? null : (short)httpStatusCode.intValue();
    }

    public Boolean getValid() {
        return this.valid;
    }

    public void setValid(Boolean valid) {
        this.valid = valid;
    }

    // Document

    public File getDocumentFile() {
        return this.documentFile;
    }

    public void setDocumentFile(File documentFile) {
        this.documentFile = documentFile;
    }



    public JSONObject toJSON() {
        JSONObject json = new JSONObject();

        try {
            json.put("url", this.url);
            json.put("downloadTimestamp", this.downloadTimestamp);
            json.put("lastAccessTimestamp", this.lastAccessTimestamp);
            json.put("expiryTimestamp", this.expiryTimestamp);
            json.put("httpStatusCode", this.httpStatusCode);
            json.put("valid", this.valid);
        } catch(JSONException ex) {
            // Very unlikely to happen
            LOGGER.log(Level.WARNING, String.format("Error occurred while creating the JSONObject: %s", Utils.getExceptionMessage(ex)), ex);
        }

        return json;
    }

    @Override
    public String toString() {
        try {
            return this.toJSON().toString(4);
        } catch (JSONException ex) {
            // Very unlikely to happen
            LOGGER.log(Level.WARNING, String.format("Error occurred while converting the JSONObject to String: %s", Utils.getExceptionMessage(ex)), ex);
        }

        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CacheEntry that = (CacheEntry) o;
        return Utils.equals(url, that.url) &&
                Utils.equals(downloadTimestamp, that.downloadTimestamp) &&
                Utils.equals(lastAccessTimestamp, that.lastAccessTimestamp) &&
                Utils.equals(expiryTimestamp, that.expiryTimestamp) &&
                Utils.equals(httpStatusCode, that.httpStatusCode) &&
                Utils.equals(valid, that.valid);
    }

    @Override
    public int hashCode() {
        int result = this.url == null ? 0 : this.url.hashCode();
        result = 31 * result + (this.downloadTimestamp == null ? 0 : this.downloadTimestamp.hashCode());
        result = 31 * result + (this.lastAccessTimestamp == null ? 0 : this.lastAccessTimestamp.hashCode());
        result = 31 * result + (this.expiryTimestamp == null ? 0 : this.expiryTimestamp.hashCode());
        result = 31 * result + (this.httpStatusCode == null ? 0 : this.httpStatusCode.hashCode());
        result = 31 * result + (this.valid == null ? 0 : this.valid.hashCode());
        return result;
    }
}
