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

import java.io.Closeable;
import java.io.File;
import java.net.URL;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple bean used to represent an entry in the database
 */
public class CacheEntry implements Closeable {
    private static final Logger LOGGER = Logger.getLogger(CacheEntry.class.getName());

    private URL url;
    private RequestMethod requestMethod; // HEAD or GET
    private Long requestTimestamp;
    private Long lastAccessTimestamp;
    private Long expiryTimestamp;
    private Integer httpStatusCode;
    private Boolean valid;

    // List of entities (datasources) which are using this entry.
    // This is used to cleanup entries that are not in used anymore.
    private Set<String> usage;

    /**
     * The document File needs to be set before saving the CacheEntity.
     * When loading the CacheEntity using CacheDatabase.get(), the document
     * is not loaded (set to null). It's loaded using a second query when
     * requested, using CacheDatabase.loadDocument(). This is equivalent to
     * "lazy" loading in Hibernate.
     */
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

    public RequestMethod getRequestMethod() {
        return this.requestMethod;
    }

    public void setRequestMethod(RequestMethod requestMethod) {
        this.requestMethod = requestMethod;
    }

    public Long getRequestTimestamp() {
        return this.requestTimestamp;
    }

    public void setRequestTimestamp(Long requestTimestamp) {
        this.requestTimestamp = requestTimestamp;
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

    public Integer getHttpStatusCode() {
        return this.httpStatusCode;
    }

    public void setHttpStatusCode(Integer httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    public Boolean getValid() {
        return this.valid;
    }

    public boolean isValid(boolean defaultValue) {
        return this.valid == null ? defaultValue : this.valid;
    }

    public void setValid(Boolean valid) {
        this.valid = valid;
    }

    public File getDocumentFile() {
        return this.documentFile;
    }

    public void setDocumentFile(File documentFile) {
        if (this.documentFile != null) {
            this.documentFile.delete();
        }
        this.documentFile = documentFile;
    }

    public Set<String> getUsage() {
        return this.usage;
    }

    public void setUsage(Set<String> usage) {
        this.usage = usage;
    }

    // Helper methods

    public boolean addUsage(String entityId) {
        if (entityId == null || entityId.isEmpty()) {
            return false;
        }

        if (this.usage == null) {
            this.usage = new HashSet<String>();
        }

        return this.usage.add(entityId);
    }

    public boolean removeUsage(String entityId) {
        if (entityId == null || entityId.isEmpty()) {
            return false;
        }

        if (this.usage == null || this.usage.isEmpty()) {
            return false;
        }

        return this.usage.remove(entityId);
    }

    public boolean isExpired() {
        if (this.expiryTimestamp == null) {
            // Never expires
            return false;
        }

        long currentTimestamp = CacheEntry.getCurrentTimestamp();
        return this.expiryTimestamp < currentTimestamp;
    }

    public boolean isPageNotFound() {
        return this.httpStatusCode != null && this.httpStatusCode == 404;
    }

    public boolean isSuccess() {
        return this.httpStatusCode != null && this.httpStatusCode >= 200 && this.httpStatusCode < 300;
    }

    public static long getCurrentTimestamp() {
        return new Date().getTime();
    }


    public JSONObject toJSON() {
        JSONObject json = new JSONObject();

        try {
            json.put("url", this.url);
            json.put("requestMethod", this.requestMethod);
            json.put("requestTimestamp", this.requestTimestamp);
            json.put("lastAccessTimestamp", this.lastAccessTimestamp);
            json.put("expiryTimestamp", this.expiryTimestamp);
            json.put("httpStatusCode", this.httpStatusCode);
            json.put("valid", this.valid);
            json.put("documentFile", this.documentFile);
            json.put("usage", this.usage);
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
        return Utils.equals(this.url.toString(), that.url.toString()) &&
                Utils.equals(this.requestMethod, that.requestMethod) &&
                Utils.equals(this.requestTimestamp, that.requestTimestamp) &&
                Utils.equals(this.lastAccessTimestamp, that.lastAccessTimestamp) &&
                Utils.equals(this.expiryTimestamp, that.expiryTimestamp) &&
                Utils.equals(this.httpStatusCode, that.httpStatusCode) &&
                Utils.equals(this.valid, that.valid) &&
                Utils.equals(this.usage, that.usage);
    }

    @Override
    public int hashCode() {
        // *IMPORTANT* Never use URL.hashCode()! It will load the document associated with the URL
        int result = this.url == null ? 0 : this.url.toString().hashCode();
        result = 31 * result + (this.requestMethod == null ? 0 : this.requestMethod.hashCode());
        result = 31 * result + (this.requestTimestamp == null ? 0 : this.requestTimestamp.hashCode());
        result = 31 * result + (this.lastAccessTimestamp == null ? 0 : this.lastAccessTimestamp.hashCode());
        result = 31 * result + (this.expiryTimestamp == null ? 0 : this.expiryTimestamp.hashCode());
        result = 31 * result + (this.httpStatusCode == null ? 0 : this.httpStatusCode.hashCode());
        result = 31 * result + (this.valid == null ? 0 : this.valid.hashCode());
        result = 31 * result + (this.usage == null ? 0 : this.usage.hashCode());
        return result;
    }

    @Override
    public void close() {
        if (this.documentFile != null) {
            this.documentFile.delete();
            this.documentFile = null;
        }
    }
}
