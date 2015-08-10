# Upgrading Tips for the AtlasMapper #

This document records tips and pitfalls when upgrading from one version of the AtlasMapper to another. While we try to make the process as painless as possible, some upgrades require some manual steps to take advantage of newly added features.

# General Notes #
When upgrading the version of the AtlasMapper you need to perform a "Complete" regenerate each of the clients to the current version, rather than the normal "Minimal" regeneration. This "Complete" generation rebuilds the client code from the current version of the server. Without this the client will still be running an old version of the code with the new version of the server.

# 1.3 #

This version adds the ability to slightly customise the features that are included in the client. This includes the _Add and Remove_ buttons, the _Location Search_ tool, _Save map_, _Map Options_ and the _Print Frame tool_. By default these tools are off and so upgrading to version 1.3 from a previous version will result in a client with none of these features and so you will not be able to even add new layers.

To fix this:
  * Under _Client Configuration/Advanced_ enable _Show Add [+] and Remove [-] layer buttons_
  * Under _Client Configuration/Map toolbar_ enable the features you wish to include.

For the basic location search to work enable _Show Google search results_