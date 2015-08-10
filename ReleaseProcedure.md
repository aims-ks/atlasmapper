# Introduction #

This document contains a list of steps to follow as part of a release of a new version of the AtlasMapper. The objective is to ensure everything as been done properly and to avoid, as much as possible, introduction of cryptic bugs or reintroduction of old bugs.

# Details #

  1. Change the version number in the project file "pom.xml"
  1. Compile a new war file with the new version
  1. Pass through the list of [manual tests](ManualTests.md)
  1. Go through the list of Issues and test the one that are supposed to be fixed in the current release
  1. Tag the new release in the local repository
  1. Branch the new release, if needed (NOTE: a branch should contains all minor releases. For example, the branch 1.2.x should contains 1.2-rc1, 1.2, 1.2.1, etc.)
  1. Commit the code to the main repository
  1. Create a new packaged version:
    1. Download the latest packaged version
    1. Unzip it
    1. Change the version number in the project folder and in the VERSION.txt file
    1. Change the atlasmapper.war with the newly compiled one
    1. Zip the folder into a zip file containing the new version number in the name
  1. Upload the package version in the Downloads page, with a similar description as the one of the previous release
  1. Add the labels for current version number in the Administer - Issue Tracking
    1. Add `"IssueOpened-<version>"`
    1. Add `"IssueClosed-<version>"`
  1. Mark the fixed issues as fixed, and add the label `"IssueClosed-<version>"`
  1. Update the [Potential Enhancement Ideas](PotentialEnhancements.md) and the [Changelog](Changelog.md) in the Wiki
  1. Update the latest release link in the project home page
  1. [Redeploy](Redeploy.md) the demo version