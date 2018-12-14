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
import au.gov.aims.atlasmapperserver.thread.RevivableThreadInterruptedException;
import au.gov.aims.atlasmapperserver.thread.ThreadLogger;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
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

    public void openConnection(ThreadLogger logger) throws RevivableThreadInterruptedException {
        if (this.dbConnectionString == null) {
            File dbDir = this.getDatabaseDirectory(logger);
            this.dbConnectionString = String.format("jdbc:h2:%s/h2Database", dbDir.getAbsolutePath());
        }

        if (this.connection == null) {
            this.connection = getDBConnection(logger);
            this.createTable(logger);
        }
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

    public synchronized void save(ThreadLogger logger, CacheEntry cacheEntry) {
        if (this.connection == null) {
            logger.log(Level.SEVERE, "Database connection is closed.");
            return;
        }

        if (cacheEntry.getUrl() == null) {
            logger.log(Level.SEVERE, "URL is null.");
            return;
        }

        if (this.exists(logger, cacheEntry.getUrl())) {
            this.update(logger, cacheEntry);
        } else {
            this.insert(logger, cacheEntry);
        }
    }

    public synchronized void insert(ThreadLogger logger, CacheEntry cacheEntry) {
        if (this.connection == null) {
            logger.log(Level.SEVERE, "Database connection is closed.");
            return;
        }

        if (cacheEntry.getUrl() == null) {
            logger.log(Level.SEVERE, "URL is null.");
            return;
        }

        // Insert
        String insertQuery = "INSERT INTO cache " +
                "(url, downloadTimestamp, lastAccessTimestamp, expiryTimestamp, httpStatusCode, valid, document) " +
                "values (?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement insertPreparedStatement = null;
        FileInputStream fileInputStream = null;
        try {
            insertPreparedStatement = this.connection.prepareStatement(insertQuery);
            insertPreparedStatement.setString(1, cacheEntry.getUrl().toString());
            insertPreparedStatement.setObject(2, cacheEntry.getDownloadTimestamp(), Types.BIGINT);
            insertPreparedStatement.setObject( 3, cacheEntry.getLastAccessTimestamp(), Types.BIGINT);
            insertPreparedStatement.setObject( 4, cacheEntry.getExpiryTimestamp(), Types.BIGINT);
            insertPreparedStatement.setObject(5, cacheEntry.getHttpStatusCode(), Types.INTEGER);
            insertPreparedStatement.setObject(6, cacheEntry.getValid(), Types.BOOLEAN);

            // Insert document Blob
            File documentFile = cacheEntry.getDocumentFile();
            if (documentFile == null) {
                insertPreparedStatement.setNull(7, Types.BLOB);
            } else {
                fileInputStream = new FileInputStream(documentFile);
                insertPreparedStatement.setBinaryStream(7, fileInputStream, documentFile.length());
            }

            insertPreparedStatement.executeUpdate();

        } catch (SQLException ex) {
            logger.log(Level.SEVERE, String.format("SQL error occurred while inserting new cache entry: %s",
                    Utils.getExceptionMessage(ex)), ex);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, String.format("IO error occurred while inserting new cache entry: %s",
                    Utils.getExceptionMessage(ex)), ex);
        } finally {
            if (insertPreparedStatement != null) {
                try {
                    insertPreparedStatement.close();
                } catch(SQLException ex) {
                    LOGGER.log(Level.SEVERE, String.format("Can not close the insert statement: %s",
                            Utils.getExceptionMessage(ex)), ex);
                }
            }
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch(IOException ex) {
                    LOGGER.log(Level.SEVERE, String.format("Can not close the inserted document file input stream: %s",
                            Utils.getExceptionMessage(ex)), ex);
                }
            }
        }
    }

    public synchronized void update(ThreadLogger logger, CacheEntry cacheEntry) {
        if (this.connection == null) {
            logger.log(Level.SEVERE, "Database connection is closed.");
            return;
        }

        if (cacheEntry.getUrl() == null) {
            logger.log(Level.SEVERE, "URL is null.");
            return;
        }

        // Update
        String updateQuery = "UPDATE cache SET " +
                "downloadTimestamp = ?, lastAccessTimestamp = ?, expiryTimestamp = ?, httpStatusCode = ?, valid = ?, document = ? " +
                "WHERE url = ?";

        PreparedStatement updatePreparedStatement = null;
        FileInputStream fileInputStream = null;
        try {
            updatePreparedStatement = this.connection.prepareStatement(updateQuery);
            updatePreparedStatement.setObject(1, cacheEntry.getDownloadTimestamp(), Types.BIGINT);
            updatePreparedStatement.setObject(2, cacheEntry.getLastAccessTimestamp(), Types.BIGINT);
            updatePreparedStatement.setObject(3, cacheEntry.getExpiryTimestamp(), Types.BIGINT);
            updatePreparedStatement.setObject(4, cacheEntry.getHttpStatusCode(), Types.INTEGER);
            updatePreparedStatement.setObject(5, cacheEntry.getValid(), Types.BOOLEAN);

            // Update document Blob
            File documentFile = cacheEntry.getDocumentFile();
            if (documentFile == null) {
                updatePreparedStatement.setNull(6, Types.BLOB);
            } else {
                fileInputStream = new FileInputStream(documentFile);
                updatePreparedStatement.setBinaryStream(6, fileInputStream, documentFile.length());
            }

            updatePreparedStatement.setString(7, cacheEntry.getUrl().toString());

            updatePreparedStatement.executeUpdate();

        } catch (SQLException ex) {
            logger.log(Level.SEVERE, String.format("SQL error occurred while updating a cache entry: %s",
                    Utils.getExceptionMessage(ex)), ex);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, String.format("IO error occurred while updating a cache entry: %s",
                    Utils.getExceptionMessage(ex)), ex);
        } finally {
            if (updatePreparedStatement != null) {
                try {
                    updatePreparedStatement.close();
                } catch(SQLException ex) {
                    LOGGER.log(Level.SEVERE, String.format("Can not close the update statement: %s",
                            Utils.getExceptionMessage(ex)), ex);
                }
            }
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch(IOException ex) {
                    LOGGER.log(Level.SEVERE, String.format("Can not close the updated document file input stream: %s",
                            Utils.getExceptionMessage(ex)), ex);
                }
            }
        }
    }

    public synchronized boolean exists(ThreadLogger logger, URL url) {
        if (this.connection == null) {
            logger.log(Level.SEVERE, "Database connection is closed.");
            return false;
        }

        if (url == null) {
            logger.log(Level.SEVERE, "URL is null.");
            return false;
        }

        boolean exists = false;
        String selectQuery = "SELECT COUNT(*) AS cnt FROM cache WHERE url = ?";

        PreparedStatement selectPreparedStatement = null;
        try {
            selectPreparedStatement = this.connection.prepareStatement(selectQuery);
            selectPreparedStatement.setString(1, url.toString());

            ResultSet resultSet = selectPreparedStatement.executeQuery();
            resultSet.next();
            int count = resultSet.getInt("cnt");
            exists = count > 0;
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, String.format("Error occurred while checking the existence of a cache entry: %s",
                    Utils.getExceptionMessage(ex)), ex);
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

    public synchronized CacheEntry get(ThreadLogger logger, URL url) {
        if (this.connection == null) {
            logger.log(Level.SEVERE, "Database connection is closed.");
            return null;
        }

        if (url == null) {
            logger.log(Level.SEVERE, "URL is null.");
            return null;
        }

        CacheEntry cacheEntry = null;
        String selectQuery = "SELECT * FROM cache WHERE url = ?";

        PreparedStatement selectPreparedStatement = null;
        FileOutputStream fileOutputStream = null;
        InputStream blobInputStream = null;
        try {
            selectPreparedStatement = this.connection.prepareStatement(selectQuery);
            selectPreparedStatement.setString(1, url.toString());

            ResultSet resultSet = selectPreparedStatement.executeQuery();
            resultSet.next();
            cacheEntry = new CacheEntry(url);
            cacheEntry.setDownloadTimestamp((Long)resultSet.getObject("downloadTimestamp"));
            cacheEntry.setLastAccessTimestamp((Long)resultSet.getObject("lastAccessTimestamp"));
            cacheEntry.setExpiryTimestamp((Long)resultSet.getObject("expiryTimestamp"));
            cacheEntry.setHttpStatusCode((Short)resultSet.getObject("httpStatusCode"));
            cacheEntry.setValid((Boolean)resultSet.getObject("valid"));

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

        } catch (SQLException ex) {
            logger.log(Level.SEVERE, String.format("SQL error occurred while checking the existence of a cache entry: %s",
                    Utils.getExceptionMessage(ex)), ex);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, String.format("IO error occurred while checking the existence of a cache entry: %s",
                    Utils.getExceptionMessage(ex)), ex);
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

        return cacheEntry;
    }

    public synchronized void delete(ThreadLogger logger, URL url) {
        if (this.connection == null) {
            logger.log(Level.SEVERE, "Database connection is closed.");
            return;
        }

        if (url == null) {
            logger.log(Level.SEVERE, "URL is null.");
            return;
        }

        String deleteQuery = "DELETE FROM cache WHERE url = ?";

        PreparedStatement deletePreparedStatement = null;
        try {
            deletePreparedStatement = this.connection.prepareStatement(deleteQuery);
            deletePreparedStatement.setString(1, url.toString());
            deletePreparedStatement.executeUpdate();

            // TODO Delete Blob
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, String.format("Error occurred while deleting a cache entry: %s",
                    Utils.getExceptionMessage(ex)), ex);
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

    private synchronized void createTable(ThreadLogger logger) throws RevivableThreadInterruptedException {
        // http://www.h2database.com/html/datatypes.html
        // We need to use BIGINT for timestamp. INT would stop working after 19/01/2038 (max INT value: 2'147'483'647)
        //     https://www.unixtimestamp.com/index.php
        String createQuery = "CREATE TABLE IF NOT EXISTS cache(" +
                "url VARCHAR(2048) PRIMARY KEY, " +
                "downloadTimestamp BIGINT, " +   // Max value: 9'223'372'036'854'775'807 (equivalent to Java long)
                "lastAccessTimestamp BIGINT, " +
                "expiryTimestamp BIGINT, " +
                "httpStatusCode SMALLINT, " +    // Max value: 32'767
                "valid BOOLEAN, " +              // TRUE or FALSE
                "document BLOB)";                // Very large object, not stored in memory. Handled with InputStream

        PreparedStatement createPreparedStatement = null;
        try {
            createPreparedStatement = this.connection.prepareStatement(createQuery);
            createPreparedStatement.executeUpdate();
        } catch(SQLException ex) {
            logger.log(Level.SEVERE, String.format("Can not create the database table: %s",
                    Utils.getExceptionMessage(ex)), ex);
            throw new RevivableThreadInterruptedException();
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

    private Connection getDBConnection(ThreadLogger logger) {
        Connection dbConnection = null;
        try {
            Class.forName(this.dbDriver);
        } catch (ClassNotFoundException ex) {
            // Unlikely to happen
            logger.log(Level.SEVERE, String.format("Database driver not found: %s",
                    Utils.getExceptionMessage(ex)), ex);
        }

        try {
            dbConnection = DriverManager.getConnection(
                    this.dbConnectionString, this.dbUser, this.dbPassword);
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, String.format("Can not connect to the database: %s",
                    Utils.getExceptionMessage(ex)), ex);
        }

        return dbConnection;
    }

    private File getDatabaseDirectory(ThreadLogger logger) throws RevivableThreadInterruptedException {
        if (this.configManager != null) {
            File appDir =  this.configManager.getApplicationFolder();
            if (appDir != null) {
                File databaseDir = new File(appDir, "cache");
                if (!databaseDir.exists()) {
                    if (!databaseDir.mkdirs()) {
                        logger.log(Level.WARNING, String.format("Can not create the database directory: %s",
                                databaseDir.getAbsolutePath()));
                    }
                }

                if (databaseDir.exists()) {
                    if (databaseDir.isDirectory() && databaseDir.canRead() && databaseDir.canWrite()) {
                        return databaseDir;
                    } else {
                        logger.log(Level.WARNING, String.format("The database directory is not writable: %s",
                                databaseDir.getAbsolutePath()));
                    }
                }
            }
        }

        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        File tmpDatabaseDir = new File(tmpDir, "atlasmapper/cache");
        if (!tmpDatabaseDir.exists()) {
            if (!tmpDatabaseDir.mkdirs()) {
                logger.log(Level.SEVERE, String.format("Could not create temporary database directory: %s",
                        tmpDatabaseDir.getAbsolutePath()));
                throw new RevivableThreadInterruptedException();
            }
        }
        logger.log(Level.WARNING, String.format("Using temporary database directory: %s",
                tmpDatabaseDir.getAbsolutePath()));
        return tmpDatabaseDir;
    }
}
