# Introduction #

Sometimes the AtlasMapper (and Tomcat) will need to be installed on an internal network that requires connecting to a proxy to get external internet access. This internet access is needed for bring in any services (WMS, ArcGIS, etc) into the AtlasMapper as the server must be able to make outgoing GetCapabilities requests to these services.

**Note:** This documentation has not been tested yet. Please let us know if this approach works in your situation.

# Details #

To configure the HTTP Proxy settings for Tomcat add the following switches.
```
-Dhttp.proxyHost=webcache.mydomain.com
-Dhttp.proxyPort=80
-Dhttp.proxyUser=someUserName
-Dhttp.proxyPassword=somePassword
```

This can be done by adding these switches to to the tomcat/bin/setenv.bat or tomcat/bin/setenv.sh. The following shows a typical setup, including setting of the available memory and the paths to the GeoServer and the AtlasMapper configuration. Adjust these examples to your situation.

Setenv.bat (Windows):
```
  @echo off
  set CATALINA_OPTS=-Xms64m -Xmx512m ^
      -XX:PermSize=32m -XX:MaxPermSize=128m ^
      -server ^
      -Djava.awt.headless=true ^
      -Dhttp.proxyHost=webcache.mydomain.com ^
      -Dhttp.proxyPort=80 ^
      -Dhttp.proxyUser=someUserName ^
      -Dhttp.proxyPassword=somePassword ^
      -DGEOSERVER_DATA_DIR="C:\Path to the GeoServer data directory\data" ^
      -DATLASMAPPER_DATA_DIR="C:\Path to my AtlasMapper config\atlasmapper"
```

Setenv.sh (Unix):
```
  #!/bin/sh
  CATALINA_OPTS=" \
      -Xms64m -Xmx512m \
      -XX:PermSize=32m -XX:MaxPermSize=128m \
      -server \
      -Djava.awt.headless=true \
      -Dhttp.proxyHost=webcache.mydomain.com ^
      -Dhttp.proxyPort=80 ^
      -Dhttp.proxyUser=someUserName ^
      -Dhttp.proxyPassword=somePassword ^
      -DGEOSERVER_DATA_DIR=/Path to the GeoServer data directory/data \
      -DATLASMAPPER_DATA_DIR=/Path to my AtlasMapper config/atlasmapper"
    export CATALINA_OPTS
```

Restart tomcat

Note: Getting the application to work with a username and password does not currently work.
```
Oct 31, 2012 3:46:09 PM org.apache.commons.httpclient.HttpMethodDirector isAuthe
nticationNeeded
INFO: Authentication requested but doAuthentication is disabled
Oct 31, 2012 3:46:09 PM au.gov.aims.atlasmapperserver.URLCache getURLFile
SEVERE: Can not load the URL http://maps.e-atlas.org.au/maps/wms?REQUEST=GetCapabilities&VERSION=1.3.0
Error message: Server returned HTTP error code 407 for URL http://maps.e-atlas.org.au/maps/wms?REQUEST=GetCapabilities&VERSION=1.3.0
Oct 31, 2012 3:46:09 PM au.gov.aims.atlasmapperserver.URLCache getURLFile
INFO: Stacktrace:
java.io.IOException: Server returned HTTP error code 407 for URL http://maps.e-atlas.org.au/maps/wms?REQUEST=GetCapabilities&VERSION=1.3.0
        at org.geotools.data.ows.MultithreadedHttpClient.get(MultithreadedHttpCl
ient.java:145)
        at au.gov.aims.atlasmapperserver.URLCache.loadURLToFile(URLCache.java:26
1)
        at au.gov.aims.atlasmapperserver.URLCache.getURLFile(URLCache.java:178)
        at au.gov.aims.atlasmapperserver.URLCache.getWMSCapabilitiesResponse(URL
Cache.java:477)
```