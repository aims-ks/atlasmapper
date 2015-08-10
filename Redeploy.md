# Introduction #

This page explain how to redeploy the war on **our main server**. It is slightly more complicated than just copying the war into tomcat since it has multiple `AtlasMapper` instance running into the same tomcat.

# Details #

## Deploying the _production_ `AtlasMapper` ##

  1. Copy the atlasmapper.war into tomcat's webapp folder
  1. Look through the tomcat log (**catalina.out**) and wait until the redeploy is done. You should see an error message saying that the ATLASMAPPER\_DATA\_DIR variable is not set
```
#######################################
# ERROR: ATLASMAPPER_DATA_DIR HAS NOT BEEN SET
#######################################
```
  1. Edit the file atlasmapper/WEB-INF/web.xml and add the following lines at the beginning of the file, just after the `<display-name>`:
```
        <context-param>
                <param-name>ATLASMAPPER_DATA_DIR</param-name>
                <param-value>/home/reefatlas/e-atlas_site/maps/atlasmapper</param-value>
        </context-param>
```
  1. Look through the tomcat log (**catalina.out**) and wait until the redeploy is done. You should see a message showing the value of the new ATLASMAPPER\_DATA\_DIR variable
```
---------------------------------------
- ATLASMAPPER_DATA_DIR: /home/reefatlas/e-atlas_site/maps/atlasmapper
---------------------------------------
```
  1. Restart tomcat to avoid problem related to hot-redeployment on a production server



## Deploying the _demo_ `AtlasMapper` ##

  1. Copy the atlasmapper.war into tomcat's webapp folder, under the name of atlasmapperdemo.war
    * **IMPORTANT**: Do not copy than rename the war, tomcat may trigger the redeploy of atlasmapper.war. Use a command like this one:
> > `cp atlasmapper.war tomcat/webapp/atlasmapperdemo.war`
  1. Look through the tomcat log (**catalina.out**) and wait until the redeploy is done. You should see an error message saying that the ATLASMAPPER\_DATA\_DIR variable is not set
```
#######################################
# ERROR: ATLASMAPPER_DATA_DIR HAS NOT BEEN SET
#######################################
```
  1. Edit the file atlasmapper/WEB-INF/web.xml and add the following lines at the beginning of the file, just after the `<display-name>`:
```
        <context-param>
                <param-name>ATLASMAPPER_DATA_DIR</param-name>
                <param-value>/home/reefatlas/e-atlas_site/maps/atlasmapperdemo</param-value>
        </context-param>
```
  1. Look through the tomcat log (**catalina.out**) and wait until the redeploy is done. You should see a message showing the value of the new ATLASMAPPER\_DATA\_DIR variable
```
---------------------------------------
- ATLASMAPPER_DATA_DIR: /home/reefatlas/e-atlas_site/maps/atlasmapperdemo
---------------------------------------
```
  1. Restart tomcat to avoid problem related to hot-redeployment on a production server