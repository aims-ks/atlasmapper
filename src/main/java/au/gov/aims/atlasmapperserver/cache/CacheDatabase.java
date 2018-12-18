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
import java.util.logging.Level;
import java.util.logging.Logger;

public class CacheDatabase implements Closeable {
    private static final Logger LOGGER = Logger.getLogger(CacheDatabase.class.getName());

    private ConfigManager configManager;

    // Configurable
    private String dbDriver = "org.h2.Driver";
    private String dbConnectionString = null;
    private String dbUser = null;
    private String dbPassword = null;

    private Connection connection = null;

    public CacheDatabase(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void openConnection() throws SQLException, ClassNotFoundException, IOException {
        if (this.dbConnectionString == null) {
            File dbDir = this.getDatabaseDirectory();
            this.dbConnectionString = String.format("jdbc:h2:%s/h2Database", dbDir.getAbsolutePath());
        }

        if (this.connection == null) {
            this.connection = getDBConnection();
            this.createTable();
        }
    }

    private synchronized void createTable() throws SQLException {
        // http://www.h2database.com/html/datatypes.html
        // We need to use BIGINT for timestamp. INT would stop working after 19/01/2038 (max INT value: 2'147'483'647)
        //     https://www.unixtimestamp.com/index.php
        String createQuery = "CREATE TABLE IF NOT EXISTS cache(" +
                "url VARCHAR(2048) PRIMARY KEY, " +
                "requestMethod VARCHAR(32), " + // HEAD, GET, etc
                "requestTimestamp BIGINT, " +   // Max value: 9'223'372'036'854'775'807 (equivalent to Java long)
                "lastAccessTimestamp BIGINT, " +
                "expiryTimestamp BIGINT, " +
                "httpStatusCode SMALLINT, " +    // Max value: 32'767
                "valid BOOLEAN, " +              // TRUE or FALSE
                "document BLOB)";                // Very large object, not stored in memory. Handled with InputStream

        PreparedStatement createPreparedStatement = null;
        try {
            createPreparedStatement = this.connection.prepareStatement(createQuery);
            createPreparedStatement.executeUpdate();
        } finally {
            if (createPreparedStatement != null) {
                try {
                    createPreparedStatement.close();
                } catch(SQLException ex) {
                    LOGGER.log(Level.SEVERE, String.format("Can not close the create table statement: %s",
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

        } finally {
            if (insertPreparedStatement != null) {
                try {
                    insertPreparedStatement.close();
                } catch(SQLException ex) {
                    LOGGER.log(Level.SEVERE, String.format("Can not close the insert statement: %s",
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

        } finally {
            if (updatePreparedStatement != null) {
                try {
                    updatePreparedStatement.close();
                } catch(SQLException ex) {
                    LOGGER.log(Level.SEVERE, String.format("Can not close the update statement: %s",
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
                    LOGGER.log(Level.SEVERE, String.format("Can not close the select statement: %s",
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
            }
        } finally {
            if (selectPreparedStatement != null) {
                try {
                    selectPreparedStatement.close();
                } catch(SQLException ex) {
                    LOGGER.log(Level.SEVERE, String.format("Can not close the select statement: %s",
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
                    LOGGER.log(Level.SEVERE, String.format("Can not close the select statement: %s",
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

    public synchronized void delete(URL url) throws SQLException {
        if (this.connection == null) {
            throw new IllegalStateException("Database connection is closed.");
        }

        if (url == null) {
            throw new IllegalArgumentException("URL is null.");
        }

        String deleteQuery = "DELETE FROM cache WHERE url = ?";

        PreparedStatement deletePreparedStatement = null;
        try {
            deletePreparedStatement = this.connection.prepareStatement(deleteQuery);
            deletePreparedStatement.setString(1, url.toString());
            deletePreparedStatement.executeUpdate();
        } finally {
            if (deletePreparedStatement != null) {
                try {
                    deletePreparedStatement.close();
                } catch(SQLException ex) {
                    LOGGER.log(Level.SEVERE, String.format("Can not close the delete statement: %s",
                            Utils.getExceptionMessage(ex)), ex);
                }
            }
        }
    }

    private Connection getDBConnection() throws ClassNotFoundException, SQLException {
        Class.forName(this.dbDriver);

        return DriverManager.getConnection(
                this.dbConnectionString, this.dbUser, this.dbPassword);
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
