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

import au.gov.aims.atlasmapperserver.ConfigManager;
import au.gov.aims.atlasmapperserver.Utils;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CacheDatabase implements Closeable {
    private static final Logger LOGGER = Logger.getLogger(CacheDatabase.class.getName());

    private ConfigManager configManager;

    // Configurable
    private String dbDriver = "org.h2.Driver";
    private String dbName = "downloadDatabase";
    private String dbConnectionString = null;
    private String dbUser = null;
    private String dbPassword = null;

    private Connection connection = null;

    public CacheDatabase(ConfigManager configManager) {
        this(configManager, null);
    }

    public CacheDatabase(ConfigManager configManager, String databaseName) {
        this.configManager = configManager;
        if (Utils.isNotBlank(databaseName)) {
            this.dbName = databaseName.trim();
        }
    }

    public void openConnection() throws SQLException, ClassNotFoundException, IOException {
        if (this.dbConnectionString == null) {
            File dbDir = this.getDatabaseDirectory();
            this.dbConnectionString = String.format("jdbc:h2:%s/" + this.dbName, dbDir.getAbsolutePath());
        }

        if (this.connection == null) {
            this.connection = getDBConnection();
            this.createTables();
        }
    }

    private synchronized void createTables() throws SQLException {
        this.createCacheTable();
        this.createCacheUsageTable();
    }

    private synchronized void createCacheTable() throws SQLException {
        // http://www.h2database.com/html/datatypes.html
        // We need to use BIGINT for timestamp. INT would stop working after 19/01/2038 (max INT value: 2'147'483'647)
        //     https://www.unixtimestamp.com/index.php
        String createQuery = "CREATE TABLE IF NOT EXISTS cache(" +
                "url VARCHAR(2048), " +
                "requestMethod VARCHAR(32), " + // HEAD, GET, etc
                "requestTimestamp BIGINT, " +   // Max value: 9'223'372'036'854'775'807 (equivalent to Java long)
                "lastAccessTimestamp BIGINT, " +
                "expiryTimestamp BIGINT, " +
                "httpStatusCode SMALLINT, " +    // Max value: 32'767
                "valid BOOLEAN, " +              // TRUE or FALSE
                "document BLOB, " +              // Very large object, not stored in memory. Handled with InputStream
                "PRIMARY KEY (url))";

        PreparedStatement createPreparedStatement = null;
        try {
            createPreparedStatement = this.connection.prepareStatement(createQuery);
            createPreparedStatement.executeUpdate();
        } finally {
            if (createPreparedStatement != null) {
                try {
                    createPreparedStatement.close();
                } catch(SQLException ex) {
                    LOGGER.log(Level.SEVERE, String.format("Can not close the create cache table statement: %s",
                            Utils.getExceptionMessage(ex)), ex);
                }
            }
        }
    }

    private synchronized void createCacheUsageTable() throws SQLException {
        String createQuery = "CREATE TABLE IF NOT EXISTS cacheUsage(" +
                "url VARCHAR(2048), " +
                "entityId VARCHAR(1024), " +
                "PRIMARY KEY (url, entityId)," +
                "FOREIGN KEY (url) REFERENCES cache(url))";

        PreparedStatement createPreparedStatement = null;
        try {
            createPreparedStatement = this.connection.prepareStatement(createQuery);
            createPreparedStatement.executeUpdate();
        } finally {
            if (createPreparedStatement != null) {
                try {
                    createPreparedStatement.close();
                } catch(SQLException ex) {
                    LOGGER.log(Level.SEVERE, String.format("Can not close the create cache usage table statement: %s",
                            Utils.getExceptionMessage(ex)), ex);
                }
            }
        }
    }

    public synchronized void save(CacheEntry cacheEntry) throws IOException, SQLException {
        if (this.connection == null) {
            throw new IllegalStateException("Database connection is closed.");
        }

        if (cacheEntry.getUrl() == null) {
            throw new IllegalArgumentException("CacheEntry URL is null.");
        }

        if (this.exists(cacheEntry.getUrl())) {
            this.update(cacheEntry);
        } else {
            this.insert(cacheEntry);
        }
    }

    public synchronized void insert(CacheEntry cacheEntry) throws SQLException, IOException {
        if (this.connection == null) {
            throw new IllegalStateException("Database connection is closed.");
        }

        if (cacheEntry.getUrl() == null) {
            throw new IllegalArgumentException("CacheEntry URL is null.");
        }

        // Insert
        String insertQuery = "INSERT INTO cache " +
                "(url, requestMethod, requestTimestamp, lastAccessTimestamp, expiryTimestamp, httpStatusCode, valid, document) " +
                "values (?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement insertPreparedStatement = null;
        InputStream inputStream = null;
        try {
            Integer intStatusCode = cacheEntry.getHttpStatusCode();
            Short statusCode = intStatusCode == null ? null : intStatusCode.shortValue();

            RequestMethod requestMethod = cacheEntry.getRequestMethod();
            String requestMethodStr = requestMethod == null ? null : requestMethod.toString();

            insertPreparedStatement = this.connection.prepareStatement(insertQuery);
            insertPreparedStatement.setString(1, cacheEntry.getUrl().toString());
            insertPreparedStatement.setString(2, requestMethodStr);
            insertPreparedStatement.setObject(3, cacheEntry.getRequestTimestamp(), Types.BIGINT);
            insertPreparedStatement.setObject( 4, cacheEntry.getLastAccessTimestamp(), Types.BIGINT);
            insertPreparedStatement.setObject( 5, cacheEntry.getExpiryTimestamp(), Types.BIGINT);
            insertPreparedStatement.setObject(6, statusCode, Types.INTEGER);
            insertPreparedStatement.setObject(7, cacheEntry.getValid(), Types.BOOLEAN);
            inputStream = this.setBlob(insertPreparedStatement, 8, cacheEntry);

            insertPreparedStatement.executeUpdate();

            this.saveCacheUsage(cacheEntry);

            this.connection.commit();
        } finally {
            if (insertPreparedStatement != null) {
                try {
                    insertPreparedStatement.close();
                } catch(SQLException ex) {
                    LOGGER.log(Level.SEVERE, String.format("Can not close the insert cache statement: %s",
                            Utils.getExceptionMessage(ex)), ex);
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch(IOException ex) {
                    LOGGER.log(Level.SEVERE, String.format("Can not close the inserted document file input stream: %s",
                            Utils.getExceptionMessage(ex)), ex);
                }
            }
        }
    }

    public synchronized void update(CacheEntry cacheEntry) throws SQLException, IOException {
        if (this.connection == null) {
            throw new IllegalStateException("Database connection is closed.");
        }

        if (cacheEntry.getUrl() == null) {
            throw new IllegalArgumentException("CacheEntry URL is null.");
        }

        // Update
        String updateQuery = "UPDATE cache SET " +
                "requestMethod = ?, requestTimestamp = ?, lastAccessTimestamp = ?, expiryTimestamp = ?, httpStatusCode = ?, valid = ?, document = ? " +
                "WHERE url = ?";

        PreparedStatement updatePreparedStatement = null;
        InputStream inputStream = null;
        try {
            Integer intStatusCode = cacheEntry.getHttpStatusCode();
            Short statusCode = intStatusCode == null ? null : intStatusCode.shortValue();

            RequestMethod requestMethod = cacheEntry.getRequestMethod();
            String requestMethodStr = requestMethod == null ? null : requestMethod.toString();

            updatePreparedStatement = this.connection.prepareStatement(updateQuery);
            updatePreparedStatement.setString(1, requestMethodStr);
            updatePreparedStatement.setObject(2, cacheEntry.getRequestTimestamp(), Types.BIGINT);
            updatePreparedStatement.setObject(3, cacheEntry.getLastAccessTimestamp(), Types.BIGINT);
            updatePreparedStatement.setObject(4, cacheEntry.getExpiryTimestamp(), Types.BIGINT);
            updatePreparedStatement.setObject(5, statusCode, Types.INTEGER);
            updatePreparedStatement.setObject(6, cacheEntry.getValid(), Types.BOOLEAN);
            inputStream = this.setBlob(updatePreparedStatement, 7, cacheEntry);

            updatePreparedStatement.setString(8, cacheEntry.getUrl().toString());

            updatePreparedStatement.executeUpdate();

            this.saveCacheUsage(cacheEntry);

            this.connection.commit();
        } finally {
            if (updatePreparedStatement != null) {
                try {
                    updatePreparedStatement.close();
                } catch(SQLException ex) {
                    LOGGER.log(Level.SEVERE, String.format("Can not close the update cache statement: %s",
                            Utils.getExceptionMessage(ex)), ex);
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch(IOException ex) {
                    LOGGER.log(Level.SEVERE, String.format("Can not close the updated document file input stream: %s",
                            Utils.getExceptionMessage(ex)), ex);
                }
            }
        }
    }

    public synchronized void delete(URL url) throws SQLException {
        if (this.connection == null) {
            throw new IllegalStateException("Database connection is closed.");
        }

        if (url == null) {
            throw new IllegalArgumentException("URL is null.");
        }

        this.deleteCacheUsage(url.toString());

        String deleteQuery = "DELETE FROM cache WHERE url = ?";

        PreparedStatement deletePreparedStatement = null;
        try {
            this.saveCacheUsage(url, null);

            deletePreparedStatement = this.connection.prepareStatement(deleteQuery);
            deletePreparedStatement.setString(1, url.toString());
            deletePreparedStatement.executeUpdate();

            this.connection.commit();
        } finally {
            if (deletePreparedStatement != null) {
                try {
                    deletePreparedStatement.close();
                } catch(SQLException ex) {
                    LOGGER.log(Level.SEVERE, String.format("Can not close the delete cache statement: %s",
                            Utils.getExceptionMessage(ex)), ex);
                }
            }
        }
    }

    public synchronized void deleteExpired() throws SQLException {
        if (this.connection == null) {
            throw new IllegalStateException("Database connection is closed.");
        }

        long now = CacheEntry.getCurrentTimestamp();

        String deleteCacheUsageQuery = "DELETE FROM cacheUsage WHERE url IN " +
            "(SELECT DISTINCT c.url FROM cache c WHERE expiryTimestamp < ?)";

        PreparedStatement deleteCacheUsageStmt = null;
        try {
            deleteCacheUsageStmt = this.connection.prepareStatement(deleteCacheUsageQuery);
            deleteCacheUsageStmt.setLong(1, now);
            deleteCacheUsageStmt.executeUpdate();
        } finally {
            if (deleteCacheUsageStmt != null) {
                try {
                    deleteCacheUsageStmt.close();
                } catch(SQLException ex) {
                    LOGGER.log(Level.SEVERE, String.format("Can not close the delete expired cache usage statement: %s",
                            Utils.getExceptionMessage(ex)), ex);
                }
            }
        }

        String deleteQuery = "DELETE FROM cache " +
            "WHERE expiryTimestamp < ?";

        PreparedStatement deletePreparedStatement = null;
        try {
            deletePreparedStatement = this.connection.prepareStatement(deleteQuery);
            deletePreparedStatement.setLong(1, now);

            deletePreparedStatement.executeUpdate();
        } finally {
            if (deletePreparedStatement != null) {
                try {
                    deletePreparedStatement.close();
                } catch(SQLException ex) {
                    LOGGER.log(Level.SEVERE, String.format("Can not close the delete expired cache statement: %s",
                            Utils.getExceptionMessage(ex)), ex);
                }
            }
        }

        this.connection.commit();
    }

    /**
     * Remove association with entityId for entries that were not visited since expiryTimestamp
     * @param entityId
     * @param expiryTimestamp
     * @throws SQLException
     */
    public synchronized void cleanUp(String entityId, long expiryTimestamp) throws SQLException {
        if (this.connection == null) {
            throw new IllegalStateException("Database connection is closed.");
        }

        if (entityId == null || entityId.isEmpty()) {
            throw new IllegalArgumentException("EntityId is null.");
        }

        // Find deprecated entries
        Set<String> deprecatedUrls = this.findDeprecatedUrls(entityId, expiryTimestamp);

        // Remove usage of entityId for each deprecatedUrl found
        if (deprecatedUrls != null && !deprecatedUrls.isEmpty()) {
            for (String deprecatedUrl : deprecatedUrls) {
                this.deleteCacheUsage(deprecatedUrl, entityId);
            }
        }

        // Delete unused entries
        this.deleteUnused();

        this.connection.commit();
    }

    // Return a set of url which are deprecated for the given entity ID.
    // The Entity ID should be removed from those cache entries.
    private Set<String> findDeprecatedUrls(String entityId, long expiryTimestamp) throws SQLException {
        Set<String> deprecatedUrls = new HashSet<String>();

        String selectQuery = "SELECT c.url AS url " +
                "FROM cache AS c " +
                "LEFT JOIN cacheUsage AS cu ON c.url = cu.url " +
                "WHERE c.lastAccessTimestamp < ? " +
                    "AND cu.entityId = ?";

        PreparedStatement selectPreparedStatement = null;
        try {
            selectPreparedStatement = this.connection.prepareStatement(selectQuery);
            selectPreparedStatement.setLong(1, expiryTimestamp);
            selectPreparedStatement.setString(2, entityId);

            ResultSet resultSet = selectPreparedStatement.executeQuery();
            while (resultSet.next()) {
                deprecatedUrls.add(resultSet.getString("url"));
            }
        } finally {
            if (selectPreparedStatement != null) {
                try {
                    selectPreparedStatement.close();
                } catch(SQLException ex) {
                    LOGGER.log(Level.SEVERE, String.format("Can not close the select deprecated cache usage statement: %s",
                            Utils.getExceptionMessage(ex)), ex);
                }
            }
        }

        return deprecatedUrls;
    }

    private void deleteUnused() throws SQLException {

        // Delete cache entries that are not found in cacheUsage table
        //   https://stackoverflow.com/questions/3384127/delete-sql-rows-where-ids-do-not-have-a-match-from-another-table
        String deleteQuery = "DELETE FROM cache " +
            "WHERE url NOT IN " +
            "(SELECT DISTINCT cu.url FROM cacheUsage cu)";

        // NOTE: H2 does not like DELETE with JOIN
        /*
        String deleteQuery = "DELETE c " +
            "FROM cache c " +
            "LEFT JOIN cacheUsage cu ON c.url = cu.url " +
            "WHERE cu.url IS NULL";
        */

        PreparedStatement deletePreparedStatement = null;
        try {
            deletePreparedStatement = this.connection.prepareStatement(deleteQuery);
            deletePreparedStatement.executeUpdate();
        } finally {
            if (deletePreparedStatement != null) {
                try {
                    deletePreparedStatement.close();
                } catch(SQLException ex) {
                    LOGGER.log(Level.SEVERE, String.format("Can not close the delete unused cache statement: %s",
                            Utils.getExceptionMessage(ex)), ex);
                }
            }
        }
    }

    /**
     * Set the blob input stream in the PreparedStatement.
     * @param preparedStatement
     * @param cacheEntry
     * @return The InputStream that was used for the blob.
     * @throws SQLException
     * @throws IOException
     */
    private InputStream setBlob(PreparedStatement preparedStatement, int parameterIndex, CacheEntry cacheEntry) throws SQLException, IOException {
        InputStream inputStream = null;

        File documentFile = cacheEntry.getDocumentFile();
        if (documentFile == null) {
            preparedStatement.setNull(parameterIndex, Types.BLOB);
        } else {
            long fileSize = documentFile.length();
            if (fileSize <= URLCache.MAX_CACHED_FILE_SIZE) {
                inputStream = new FileInputStream(documentFile);
                preparedStatement.setBinaryStream(parameterIndex, inputStream, documentFile.length());
            } else {
                throw new IOException(String.format("Document returned by URL %s is too big (larger than %d bytes)",
                        cacheEntry.getUrl().toString(), URLCache.MAX_CACHED_FILE_SIZE));
            }
        }
        return inputStream;
    }

    private void saveCacheUsage(CacheEntry cacheEntry) throws SQLException {
        this.saveCacheUsage(cacheEntry.getUrl(), cacheEntry.getUsage());
    }

    // It is safe to close the PreparedStatement before commit
    //   https://coderanch.com/t/303444/databases/Closing-Statement-object-prior-committing
    private void saveCacheUsage(URL url, Set<String> usage) throws SQLException {
        if (usage == null || usage.isEmpty()) {
            String deleteAllCacheUsageQuery = "DELETE FROM cacheUsage WHERE url = ?";

            PreparedStatement deleteAllCacheUsageStmt = null;
            try {
                deleteAllCacheUsageStmt = this.connection.prepareStatement(deleteAllCacheUsageQuery);
                deleteAllCacheUsageStmt.setString(1, url.toString());
                deleteAllCacheUsageStmt.executeUpdate();
            } finally {
                if (deleteAllCacheUsageStmt != null) {
                    try {
                        deleteAllCacheUsageStmt.close();
                    } catch(SQLException ex) {
                        LOGGER.log(Level.SEVERE, String.format("Can not close the delete cache usage statement: %s",
                                Utils.getExceptionMessage(ex)), ex);
                    }
                }
            }

        } else {
            // Get existing usage
            Set<String> oldUsage = this.getCacheUsage(url);

            // Delete deprecated usage
            for (String oldId : oldUsage) {
                if (!usage.contains(oldId)) {
                    this.deleteCacheUsage(url.toString(), oldId);
                }
            }

            // Insert new usage
            for (String newId : usage) {
                if (!oldUsage.contains(newId)) {
                    this.insertCacheUsage(url, newId);
                }
            }
        }
    }

    private void deleteCacheUsage(String urlStr) throws SQLException {
        String deleteCacheUsageQuery = "DELETE FROM cacheUsage WHERE url = ?";

        PreparedStatement deleteCacheUsageStmt = null;
        try {
            deleteCacheUsageStmt = this.connection.prepareStatement(deleteCacheUsageQuery);
            deleteCacheUsageStmt.setString(1, urlStr);
            deleteCacheUsageStmt.executeUpdate();
        } finally {
            if (deleteCacheUsageStmt != null) {
                try {
                    deleteCacheUsageStmt.close();
                } catch(SQLException ex) {
                    LOGGER.log(Level.SEVERE, String.format("Can not close the delete cache usage statement: %s",
                            Utils.getExceptionMessage(ex)), ex);
                }
            }
        }
    }

    private void deleteCacheUsage(String urlStr, String entityId) throws SQLException {
        String deleteCacheUsageQuery = "DELETE FROM cacheUsage WHERE url = ? AND entityId = ?";

        PreparedStatement deleteCacheUsageStmt = null;
        try {
            deleteCacheUsageStmt = this.connection.prepareStatement(deleteCacheUsageQuery);
            deleteCacheUsageStmt.setString(1, urlStr);
            deleteCacheUsageStmt.setString(2, entityId);
            deleteCacheUsageStmt.executeUpdate();
        } finally {
            if (deleteCacheUsageStmt != null) {
                try {
                    deleteCacheUsageStmt.close();
                } catch(SQLException ex) {
                    LOGGER.log(Level.SEVERE, String.format("Can not close the delete cache usage statement: %s",
                            Utils.getExceptionMessage(ex)), ex);
                }
            }
        }
    }

    private void insertCacheUsage(URL url, String entityId) throws SQLException {
        String insertCacheUsageQuery = "INSERT INTO cacheUsage " +
                "(url, entityId) " +
                "values (?, ?)";

        PreparedStatement insertCacheUsageStmt = null;
        try {
            insertCacheUsageStmt = this.connection.prepareStatement(insertCacheUsageQuery);
            insertCacheUsageStmt.setString(1, url.toString());
            insertCacheUsageStmt.setString(2, entityId);

            insertCacheUsageStmt.executeUpdate();
        } finally {
            if (insertCacheUsageStmt != null) {
                try {
                    insertCacheUsageStmt.close();
                } catch(SQLException ex) {
                    LOGGER.log(Level.SEVERE, String.format("Can not close the insert cache usage statement: %s",
                            Utils.getExceptionMessage(ex)), ex);
                }
            }
        }
    }

    private Set<String> getCacheUsage(URL url) throws SQLException {
        if (this.connection == null) {
            throw new IllegalStateException("Database connection is closed.");
        }

        if (url == null) {
            throw new IllegalArgumentException("URL is null.");
        }

        Set<String> cacheUsage = new HashSet<String>();
        String selectQuery = "SELECT entityId FROM cacheUsage WHERE url = ?";

        PreparedStatement selectPreparedStatement = null;
        try {
            selectPreparedStatement = this.connection.prepareStatement(selectQuery);
            selectPreparedStatement.setString(1, url.toString());

            ResultSet resultSet = selectPreparedStatement.executeQuery();
            while (resultSet.next()) {
                cacheUsage.add(resultSet.getString("entityId"));
            }
        } finally {
            if (selectPreparedStatement != null) {
                try {
                    selectPreparedStatement.close();
                } catch(SQLException ex) {
                    LOGGER.log(Level.SEVERE, String.format("Can not close the select cache usage statement: %s",
                            Utils.getExceptionMessage(ex)), ex);
                }
            }
        }

        return cacheUsage;
    }

    public synchronized boolean exists(URL url) throws SQLException {
        if (this.connection == null) {
            throw new IllegalStateException("Database connection is closed.");
        }

        if (url == null) {
            throw new IllegalArgumentException("URL is null.");
        }

        boolean exists = false;
        String selectQuery = "SELECT COUNT(*) AS cnt FROM cache WHERE url = ?";

        PreparedStatement selectPreparedStatement = null;
        try {
            selectPreparedStatement = this.connection.prepareStatement(selectQuery);
            selectPreparedStatement.setString(1, url.toString());

            ResultSet resultSet = selectPreparedStatement.executeQuery();
            boolean found = resultSet.next();
            int count = 0;
            if (found) {
                count = resultSet.getInt("cnt");
            }

            exists = count > 0;
        } finally {
            if (selectPreparedStatement != null) {
                try {
                    selectPreparedStatement.close();
                } catch(SQLException ex) {
                    LOGGER.log(Level.SEVERE, String.format("Can not close the select cache statement: %s",
                            Utils.getExceptionMessage(ex)), ex);
                }
            }
        }

        return exists;
    }

    public synchronized CacheEntry get(URL url) throws SQLException {
        if (this.connection == null) {
            throw new IllegalStateException("Database connection is closed.");
        }

        if (url == null) {
            throw new IllegalArgumentException("URL is null.");
        }

        CacheEntry cacheEntry = null;
        String selectQuery = "SELECT * FROM cache WHERE url = ?";

        PreparedStatement selectPreparedStatement = null;
        try {
            selectPreparedStatement = this.connection.prepareStatement(selectQuery);
            selectPreparedStatement.setString(1, url.toString());

            ResultSet resultSet = selectPreparedStatement.executeQuery();
            boolean found = resultSet.next();

            if (found) {
                Short statusCode = (Short)resultSet.getObject("httpStatusCode");
                Integer intStatusCode = statusCode == null ? null : statusCode.intValue();

                String requestMethodStr = resultSet.getString("requestMethod");
                RequestMethod requestMethod = requestMethodStr == null ? null : RequestMethod.valueOf(requestMethodStr);

                cacheEntry = new CacheEntry(url);
                cacheEntry.setRequestMethod(requestMethod);
                cacheEntry.setRequestTimestamp((Long)resultSet.getObject("requestTimestamp"));
                cacheEntry.setLastAccessTimestamp((Long)resultSet.getObject("lastAccessTimestamp"));
                cacheEntry.setExpiryTimestamp((Long)resultSet.getObject("expiryTimestamp"));
                cacheEntry.setHttpStatusCode(intStatusCode);
                cacheEntry.setValid((Boolean)resultSet.getObject("valid"));

                cacheEntry.setDocumentFile(null);

                cacheEntry.setUsage(this.getCacheUsage(url));
            }
        } finally {
            if (selectPreparedStatement != null) {
                try {
                    selectPreparedStatement.close();
                } catch(SQLException ex) {
                    LOGGER.log(Level.SEVERE, String.format("Can not close the select cache statement: %s",
                            Utils.getExceptionMessage(ex)), ex);
                }
            }
        }

        return cacheEntry;
    }

    public synchronized void loadDocument(CacheEntry cacheEntry) throws SQLException, IOException {
        if (this.connection == null) {
            throw new IllegalStateException("Database connection is closed.");
        }

        URL url = cacheEntry.getUrl();
        if (url == null) {
            throw new IllegalArgumentException("CacheEntry URL is null.");
        }

        String selectQuery = "SELECT document FROM cache WHERE url = ?";

        PreparedStatement selectPreparedStatement = null;
        FileOutputStream fileOutputStream = null;
        InputStream blobInputStream = null;
        try {
            selectPreparedStatement = this.connection.prepareStatement(selectQuery);
            selectPreparedStatement.setString(1, url.toString());

            ResultSet resultSet = selectPreparedStatement.executeQuery();
            boolean found = resultSet.next();

            if (found) {
                // Get document Blob
                blobInputStream = resultSet.getBinaryStream("document");
                if (blobInputStream == null) {
                    cacheEntry.setDocumentFile(null);
                } else {
                    File tmpDocument = File.createTempFile("cachedTmpFile_",".bin");
                    fileOutputStream = new FileOutputStream(tmpDocument);
                    byte[] buffer = new byte[1];

                    while (blobInputStream.read(buffer) > 0) {
                        fileOutputStream.write(buffer);
                    }
                    fileOutputStream.flush();

                    cacheEntry.setDocumentFile(tmpDocument);
                }
            }

        } finally {
            if (selectPreparedStatement != null) {
                try {
                    selectPreparedStatement.close();
                } catch(SQLException ex) {
                    LOGGER.log(Level.SEVERE, String.format("Can not close the select document statement: %s",
                            Utils.getExceptionMessage(ex)), ex);
                }
            }
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch(IOException ex) {
                    LOGGER.log(Level.SEVERE, String.format("Can not close the selected document file output stream: %s",
                            Utils.getExceptionMessage(ex)), ex);
                }
            }
            if (blobInputStream != null) {
                try {
                    blobInputStream.close();
                } catch(IOException ex) {
                    LOGGER.log(Level.SEVERE, String.format("Can not close the selected document blob input stream: %s",
                            Utils.getExceptionMessage(ex)), ex);
                }
            }
        }
    }

    private Connection getDBConnection() throws ClassNotFoundException, SQLException {
        Class.forName(this.dbDriver);

        Connection connection = DriverManager.getConnection(
                this.dbConnectionString, this.dbUser, this.dbPassword);

        // Insert / Update / Delete affect multiple tables.
        // Manually commit when everything is properly set to avoid corruption.
        connection.setAutoCommit(false);

        return connection;
    }

    private File getDatabaseDirectory() throws IOException {
        if (this.configManager != null) {
            File appDir =  this.configManager.getApplicationFolder();
            if (appDir != null) {
                File databaseDir = new File(appDir, "cache");
                if (!databaseDir.exists()) {
                    if (!databaseDir.mkdirs()) {
                        throw new IOException(String.format("Can not create the database directory: %s",
                                databaseDir.getAbsolutePath()));
                    }
                }

                if (databaseDir.exists()) {
                    if (databaseDir.isDirectory() && databaseDir.canRead() && databaseDir.canWrite()) {
                        return databaseDir;
                    } else {
                        throw new IOException(String.format("The database directory is not writable: %s",
                                databaseDir.getAbsolutePath()));
                    }
                }
            }
        }

        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        File tmpDatabaseDir = new File(tmpDir, "atlasmapper/cache");
        if (!tmpDatabaseDir.exists()) {
            if (!tmpDatabaseDir.mkdirs()) {
                throw new IOException(String.format("Could not create temporary database directory: %s",
                        tmpDatabaseDir.getAbsolutePath()));
            }
        }
        LOGGER.log(Level.WARNING, String.format("Using temporary database directory: %s",
                tmpDatabaseDir.getAbsolutePath()));
        return tmpDatabaseDir;
    }

    @Override
    public void close() {
        if (this.connection != null) {
            try {
                this.connection.close();
            } catch(SQLException ex) {
                LOGGER.log(Level.SEVERE, String.format("Can not close the database connection: %s", Utils.getExceptionMessage(ex)), ex);
            }
        }

        this.connection = null;
    }
}
