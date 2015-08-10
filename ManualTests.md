# Introduction #

This page contains a list of tests that has to be manually run before every new release. Those tests are there because they can not be automated, or they haven't been automated yet.

# Details #

Initial Deploy
  * Unset the ATLASMAPPER\_DATA\_DIR, if setted.
  * Deploy the war and check if the documentation page about setting the ATLASMAPPER\_DATA\_DIR is shown.
  * Point the ATLASMAPPER\_DATA\_DIR environment variable at a empty folder and ensure the default config is used.

Data Source testing
  * Create a new data source using only the default fields
  * Create a new data source filling all fields

Client Testing
  * Create a new client filling only the default fields, with only one data source and generate it
  * Create a new client filling all fields, with at lease 2 data sources and generate it
  * Check the main page of the 2 new clients, the preview page and the layer list page
  * Load several maps by clicking on the layer previews in the layer list of one of the client