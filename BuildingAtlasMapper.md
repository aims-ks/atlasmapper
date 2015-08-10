# Introduction #
This page describes checking out the project source and building the AtlasMapper. In these instructions I will assume the use of Netbeans 7.0.1 as the IDE, however since the AtlasMapper uses Maven for its project construction it should be able to be built using a different IDE.

## Setup ##
  * Install Java Development Kit.
  * Install Netbeans 7.0.1 or IntelliJ  12.0
  * Install TortoiseHg (for Mecurial)
  * Install Apache Maven

## Working with IntelliJ on Windows ##
There are probably much better ways of working with the code with IntelliJ than is described here, however the following is one method that should work.
  1. Download and install TortoiseHg
  1. Select a directory for the repository and go to that directory in a command line.
  1. Download the repository using the command line. If necessary refer to [source tab](http://code.google.com/p/atlasmapper/source/checkout)
```
hg clone https://code.google.com/p/atlasmapper/ 
```
  1. Start IntelliJ. Select Open Project and select the pom.xml from the downloaded repo.
  1. Fix any problems with the path to hg.exe if IntelliJ complains.
  1. Setup Maven so you don't get "No valid Maven installation found". [Download maven](http://maven.apache.org/download.cgi) and unpack the zip to a place like:
```
C:\Programs\apache-maven-3.0.4
```
  1. Open the Maven Projects panel (from the button on the right hand side). Then open the Maven Settings and set the _Maven home directory_ to:
```
C:\Programs\apache-maven-3.0.4
```
  1. In the Maven Projects panel you should have an item call "AtlasMapper server and clients". Open this item and under Lifecycle there should be _clean_, _validate_,_compile_,etc. To build the source double click on _package_. This will start the build and download any of the dependencies.
  1. The first time the package is built, it might fail as the dependency on javaee-web-api is not downloaded. This dependency is marked as _provided_ in the pom so that it will not be included in the WAR, but as a result it is not downloaded automatically as part of the build. To fix this edit the pom.xml to comment out the scope tag:
```
<dependency>
  <groupId>javax</groupId>
  <artifactId>javaee-web-api</artifactId>
  <version>6.0</version>
  <scope>provided</scope>
</dependency>
```
> To:
```
<dependency>
  <groupId>javax</groupId>
  <artifactId>javaee-web-api</artifactId>
  <version>6.0</version>
  <!--<scope>provided</scope>-->
</dependency>
```
> Save the pom and rebuild the package. This will download the javaee-web-api dependency. Once this is done, reedit the pom.xml to restore the `<scope>provided</scope>`
  1. **This description is not complete**

## Working with Netbeans ##
### Get the source using Netbeans ###

You can check out the repository using Netbeans. The disadvantage is that if an error occurs it does not get reported very well.

  1. Start Netbeans
  1. Team/Mercurial/Clone other...
  1. Repository URL: https://code.google.com/p/atlasmapper/
  1. Choose a place to save the repository locally

If you wish to check modifications back into the repository you will need to get full project access. Once you have done this set your user name to match your Google account and the password to that shown on your [Profile/Settings page](https://code.google.com/hosting/settings). DON'T use your Google login password. It will not work.

Note: The repository is approximately 25 MB and so it can take a few minutes to download.

### Getting the source with the command line ###
  1. Follow the instruction on the [source tab](http://code.google.com/p/atlasmapper/source/checkout)
```
hg clone https://code.google.com/p/atlasmapper/ 
```

### Loading the project in Netbeans ###
If you downloaded the source directly in Netbeans then it will by default load the source as a project. If you downloaded the source using the command line then use File/Open Project the find the source directory, it should appear as a Maven project called `atlasmapper`.

Some of the source folders come up as showing warnings.

Right clicking on the project and running a Clean and Build prompts Maven to download all the relevant dependencies and to build the WAR file.

### Build Problems and Errors ###
This describes some of the problems that occurred while build the project.

#### Clean and Build Failure (windows) ####
I had trouble with the Clean and Build failing due to Netbeans failing to be able to delete items in the target folder. An application (unknown) was holding a lock on the files preventing then from being deleted. Closing Netbeans and all user applications didn't help. In the end I had to restart the machine. After which I could manually delete the target folder and perform a successful build.

#### Build Failure ####
The first build resulted in a number of build failures including:
```
Failed to execute goal org.apache.maven.plugins:maven-surefire-plugin:2.7.2:test (default-test) on project atlasmapper: There are test failures.
```
In this case there was a unit test that failed due to a difference between Linux and Windows. To handle this problem the offending Unit test was fixed.

### Source Cloning Problems and Errors ###
These are some of the errors I cam across while initially checking out the code. They are probably a very small subset of the potential problems that could occur.

#### Attempt 1: Loading using Neatbeans ####
On my initial attempt after 10 mins I got an error message:
`Command Failed See Output Window for further details` with no visible output window.

#### Attempt 2: Loading using Command line ####
After rechecking out using the command line I found out the problem was a case-folding name collision (menu.html and Menu.html). This is a problem with checking out to windows that is case insensitive. The fix is to check out to a Unix box, change the name of the file, then recommit.

#### Attempt 3: Connection ended ####
```
transaction abort!
rollback completed
abort: connection ended unexpectedly
```
The problem here seemed to be that the repository was too large, causing download problems. To solve this problem we removed all the documentation and examples from the project libraries, then rebuilt the repository. This shrunk the repository from 250 MB to 27 MB.