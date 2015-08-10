# AtlasMapper Quick Install Guide #
## Tomcat ##
1. Ensure you have Java and [Tomcat](http://tomcat.apache.org/download-70.cgi) installed.

2. Setup the tomcat `bin\setenv.bat` (windows) or `bin/setenv.sh` (unix) to specify the location for the AtlasMapper to save its config.
`setenv.bat`
```
  @echo off
  set CATALINA_OPTS=-DATLASMAPPER_DATA_DIR="C:\path\to\config\folder"
```
`setenv.sh`
```
  #!/bin/sh
  CATALINA_OPTS="-DATLASMAPPER_DATA_DIR=/etc/atlasmapper"
    export CATALINA_OPTS
```
See the section on ATLASMAPPER\_DATA\_DIR for more details. Note: if you skip this step you will be prompted with instructions to setup this parameter when you first run the Atlasmapper.

3. Copy the atlasmapper.war into the webapps directory of Tomcat. Then start the server by running the Tomcat bin\startup.bat (windows) or bin/startup.sh (unix) scripts. If startup.bat does not work then run it from a commandline to see the error message.

## Jetty (Windows GeoServer Installer) ##
The windows installer for the GeoServer runs in Jetty. It is easy to add the AtlasMapper to run in the same Jetty installation.

1. Use 7zip (or other tool) to extract the `atlasmapper.war` file. Copy the resulting `atlasmapper` directory into the `GeoServer 2.1.x\webapps` directory. Note: you can also place the war directly in the webapps directory without extracting it and this will work too. The disadvantage of this is that Jetty has to unpack the war to a temp folder each time it starts. This slows the start-up time slightly.

2. Modify the `GeoServer 2.1.x\bin\startup.bat` to include the location of the AtlasMapper config. It should look something like:
```
call "C:\Program Files (x86)\Java\jre6\bin\java.exe" -DATLASMAPPER_DATA_DIR="C:\Program Files (x86)\GeoServer 2.1.3\data_dir\atlasmapper" -DGEOSERVER_DATA_DIR="C:\Program Files (x86)\GeoServer 2.1.0\data_dir" -Xmx512m -DSTOP.PORT=8079 -DSTOP.KEY=geoserver -Djetty.port=8080 -Djetty.logs="C:\Program Files (x86)\GeoServer 2.1.0\logs" -jar "C:\Program Files (x86)\GeoServer 2.1.0\start.jar"
```
Note: If you don't specify a location for the AtlasMapper to store its configuration it will place it in the `GEOSERVER_DATA_DIR` if this has been set.

3. Start the GeoServer and AtlasMapper by running `GeoServer 2.1.x\bin\startup.bat`. In this configuration you may wish to get the AtlasMapper to pull layers from the local GeoServer (See section on _Using AtlasMapper with local GeoServer_).

## Common to Tomcat and Jetty ##
4. Go to http://localhost:8080/atlasmapper. This will direct you to the admin login since no client has yet been created.

5. The initial login is: user: admin, password: admin

6. Click on the `Data source` link, in the `Navigation` panel in the left hand side.

7. Click the `Rebuild all` button to harvest the data source files, such as capabilities documents, MEST records, etc. and create the data source's layer catalog file used by the clients.

8. Click on the `AtlasMapper clients` link, in the `Navigation` panel in the left hand side.

9. Click the `Generate` icon to make the demo client, and use the links to access it.

10. The default client now takes the URL http://localhost:8080/atlasmapper. This was done to give it the shortest possible URL. To get back to the admin page go to http://localhost:8080/atlasmapper/admin

# AtlasMapper Web archive (WAR) #

The AtlasMapper is packaged as a standalone servlet for use with existing servlet container applications such as Apache Tomcat and Jetty.

Note: The AtlasMapper has mostly been tested with Tomcat and so these instruction may not work for other servlet container applications. If you find any bug while using the AtlasMapper, please create a new [issue](https://code.google.com/p/atlasmapper/issues/list) and specify which servlet container you are using.

## Java ##

The AtlasMapper requires Java to be installed on the system. Oracle Java SE 6 or newer is recommended. A Java Development Kit (JDK) is not required to run the AtlasMapper (at least when running from Tomcat).

The environment variable `JAVA_HOME` must be setup or Tomcat will not startup correctly. It will give the error: _Neither JAVA\_HOME nor the JRE\_HOME environment variable is defined._ On windows use
_Control Panel/System/Advanced/Environment variables_ to create an environment variable to point to the java installation.
For example: Variable name: `JAVA_HOME`, Variable value: `C:\Program Files\Java\jre6`

Note that when a new environmental variable is created it is not added to existing command prompts in windows. As a result starting tomcat from an existing command prompt will not work.

## Tomcat ##

Tomcat 6 or newer is recommended for running the AtlasMapper. It has principally tested in Tomcat 6.

There are many ways to install Tomcat depending on the platform and its intended use. The following is a set of instructions for installing Tomcat on Windows XP for testing purposes.

To install a test version on windows:

  1. Download the lastest [Tomcat 64-bit Windows zip](http://tomcat.apache.org/download-60.cgi) from apache.
  1. Extract the Tomcat download to `C:\Programs\apache-tomcat-6.0.33`
  1. To start Tomcat run `<tomcat install>\bin\startup.bat`. It is probably worth waiting to install the AtlasMapper before starting Tomcat.


## Installation ##

The AtlasMapper is packaged as a stardard WAR file.

  1. Navigate to the [AtlasMapper Download page](http://code.google.com/p/atlasmapper/downloads) and pick the appropriate version to download.
  1. Download the AtlasMapper zip and unpack the archive.  Copy the file `atlasmapper.war` to the directory that contains your container application's webapps.
  1. Your container application should unpack the web archive and automatically set up and run the AtlasMapper.
  1. If you now start the server and go to http://localhost:8080/atlasmapper you should see the AtlasMapper initial screen prompting you to setup the `ATLASMAPPER_DATA_DIR` parameter. This parameter is to specify the location that the AtlasMapper should save its configuration files and its clients.

> note: if you have trouble with the server starting due to an
> error it is best to run the start.bat (in windows) from a command
> line, using start/run/cmd then use cd to that Tomcat installation
> bin path.

> common errors: _Neither JAVA\_HOME nor the JRE\_HOME environment
> variable is defined._ In this case the environment variable to
> point to the Java installation has not been set, or Java is not
> installed. See the section on Java for more info.

## ATLASMAPPER\_DATA\_DIR ##
The `ATLASMAPPER_DATA_DIR` parameter specifies the location for the AtlasMapper to store is configuration files and the web clients. If this parameter is not setup correctly the AtlasMapper will display an information page detailing how to set it up. The preferred method is to set the parameter in the `<tomcat>\bin\setenv.bat` for windows or `<tomcat>/bin/setenv.sh` for Unix. This method has the advantage of not getting the settings wiped each time the WAR file is updated with a newer version.

If the AtlasMapper can not find the `ATLASMAPPER_DATA_DIR` parameter it will look for the `GEOSERVER_DATA_DIR`. If this is found then the AtlasMapper will store its configuration in GeoServer data directory.

In a new installation of Tomcat the setenv.bat or setenv.sh files do not exist and should be created in a text editor.

The minimum information needed to be specified in the `CATALINA_OPTS` is the `ATLASMAPPER_DATA_DIR` parameter. For windows save the following in `setenv.bat`, changing the path to a more appropriate one.:
```
  @echo off
  set CATALINA_OPTS=-DATLASMAPPER_DATA_DIR="C:\path\to\config\folder"
```

> note: Do not forget to change the path from
> `C:\path\to\config\folder` to an actual path you want.

For Unix save the following to `setenv.sh`:
```
  #!/bin/sh
  CATALINA_OPTS="-DATLASMAPPER_DATA_DIR=/etc/atlasmapper"
    export CATALINA_OPTS
```

## AtlasMapper and GeoServer ##

If you are running a GeoServer in the same Tomcat container then you might wish to setup its `GEOSERVER_DATA_DIR` parameter in the same `setenv` file along with other server optimisations such as maximum memory use. The following are more complex examples of these files that migh be useful for your reference:

Setenv.bat (Windows):
```
  @echo off
  set CATALINA_OPTS=-Xms64m -Xmx512m ^
      -XX:PermSize=32m -XX:MaxPermSize=128m ^
      -server ^
      -Djava.awt.headless=true ^
      -DGEOSERVER_DATA_DIR=X:\prod\home\reefatlas\e-atlas_site\maps\data ^
      -DATLASMAPPER_DATA_DIR="C:\Documents and Settings\elawrey\My Documents\tmp\atlasmapper"
```

Setenv.sh (Unix):
```
  #!/bin/sh
  CATALINA_OPTS=" \
      -Xms64m -Xmx512m \
      -XX:PermSize=32m -XX:MaxPermSize=128m \
      -server \
      -Djava.awt.headless=true \
      -DGEOSERVER_DATA_DIR=/home/reefatlas/e-atlas_site/maps/data \
      -DATLASMAPPER_DATA_DIR=/home/reefatlas/e-atlas_site/maps/atlasmapper"
    export CATALINA_OPTS
```

## Login ##
The initial default login is username: admin, password: admin

## Demo client ##
When you initially start a demo client has been setup but not generated, and therefore is not yet available. To create the demo client go to the AtlasMapper client configuration then click on the "Regenerate all", or the Generate button (second button in the Actions column).

After the demo client is created it becomes the default path for the atlasmapper. This is done to make the client have the shortest possible URL. As a result going to http://localhost:8080/atlasmapper will now display the default client. To get back to the administration page use the address:
http://localhost:8080/atlasmapper/public/admin.jsp

# Using AtlasMapper with local GeoServer #
The AtlasMapper can be installed into the same Servlet container (tomcat or Jetty) as a local running version of GeoServer. It can then be used to view all the local layers.

## Add local GeoServer as AtlasMapper data source ##
Under _Navigation/Data Sources_ create a new WMS source. Set up the following:
Data source ID: local (unique name)
Data source name: Local GeoServer (This will be the name of the tab in the client for this source)
WMS service URL: http://localhost:8080/geoserver/ows?service=wms&version=1.1.1&request=GetCapabilities

## Add the local GeoServer data source to client ##
Under _Navitation/AtlasMapper clients_ choose a client that you wish to add the local GeoServer to. If you are using the default installation use the demo client.
1. Under the _Actions_ column click the _Edit_ button. Then under the _Data source type_ section enable the _Local GeoServer_ data source and click _Apply_.
2. Under the _Actions_ column click _Generate_ the _Minimal_. This will remake the client using the new data source, setting up all the layers specified in the GetCapabilities from the local GeoServer.

## GetCapabilities Caching ##
Note: The GetCapabilities requests are disk cached (by default 1 week) by the AtlasMapper (with a 5 min timeout), and so if you are rapidly changing layer settings this cache may become out-of-date. To overcome this problem under there is a clear cache button in the _Action_ column of on the _Data sources_ page. You can also disable the caching for a given data source.