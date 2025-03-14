# IBM Watson Discovery Crawler Plug-in SDK

IBM Watson Discovery allows users to crawl the customers' data source servers, but it cannot support all cases with a wide variety of data structures. With crawler plug-ins, the customer is now enable to quickly develop relevant solutions with Java for their cases.

**Please note:**
- The customer is responsible for arranging the funds required to develop and maintain the developed crawler plug-in assets

## Ownership
- The customer owns the developed crawler plug-in

## Support Policy
- IBM support team will not accept a PMR about a crawler plug-in, unless the customer thinks a problem is caused by Watson Discovery product code

## Watson Discovery Product Team Responsibility
Watson Discovery product team provides
- SDK (the package including this document)
- Javadoc (lib/ama-zing-crawler-plugin-${build-version}-javadoc.jar)
- Sample code (src/main/java/com/ibm/es/ama/plugin/sample/SampleCrawlerPlugin.java)


## Supported Crawler Plug-in Functions

The following functions are supported by crawler plug-ins.
- Update the metadata list of the crawled document
- Update the content of the crawled document
- Exclude the crawled document
- Refer the crawler configurations (password values would be masked)
- Show notice messages on Discovery Tooling UI
- Output log messages to the crawler pod console

The following functions are NOT supported.
- Split one crawled document to multiple
- Add multiple contents in one document
- Modify Access Control List


## Development Guide (Quick)

### Requirements

To develop a crawler plug-in with this SDK, the following are required on the development server.

- JDK 1.8 or higher
- Gradle
- curl
- sed


### Implementing a crawler plug-in

#### Interfaces and JavaDoc

The interface library is available as `lib/ama-zing-crawler-plugin-${build-version}.jar` in the SDK directory. JavaDoc for the JAR file is available as `lib/ama-zing-crawler-plugin-${build-version}-javadoc.jar` in the same directory.

#### Initialization interface

Use the `com.ibm.es.ama.plugin.CrawlerPlugin` interface to initialize or terminate a crawler plug-in, or to update the crawled documents.

#### Dependency management

The Java dependency is managed by the file `build.gradle`.

#### Crawler plug-in example

The example crawler plug-in `src/main/java/com/ibm/es/ama/plugin/sample/SampleCrawlerPlugin.java` is to add, update, delete the metadata, also to update and delete the content of the crawled documents by Local File System connector.


### Assembling and compiling a crawler plug-in

1. Specify the class name of the crawler plug-in
Open the file `config/template.xml` and modify the `initial-value` of the `crawler_plugin_class` element.

2. Ensure you are in the crawler plug-in SDK directory on your development server

3. Use Gradle to compile your Java source code and to create a ZIP file that includes all of the required components for the crawler plug-in
```
$ gradle build packageCrawlerPlugin
```

4. Confirm the crawler plug-in package has been created as `build/distributions/wd-crawler-plugin-sample.zip`


### Managing crawler plug-ins on your Watson Discovery cluster

The script `scripts/manage_crawler_plugin.sh` is to manage crawler plug-ins. The script requires the endpoint URL of your Watson Discovery cluster, also the user name and the password of your Cloud Pak for Data.

1. Show the script helps
```
$ scripts/manage_crawler_plugin.sh --help

Usage: scripts/manage_crawler_plugin.sh --endpoint endpoint --user username [--password password] command

Watson Discovery Crawler Plug-in Manager

This script will help you deploy, undeploy, and list your crawler plug-ins for
Watson Discovery.

Commands:
  deploy        Add a new crawler plug-in to your Watson Discovery instance
  undeploy      Undeploy your crawler plug-in by ID
  list          List all crawler plug-ins for your Watson Discovery instance (default)

Options:
  -e --endpoint         The endpoint URL for your cluster and add-on service instance
                        (https://{cpd_cluster_host}{:port}/discovery/{release}/instances/{instance_id}/api)
  -u --user             The user name of your Cloud Pak instance
  -p --password         The user password of your Cloud Pak instance
                        If the password is not specified, the command line prompts to input
  -n --name             The name of the crawler plug-in to upload (deploy only)
  -f --file             The path of the crawler plug-in package to upload (deploy only)
  --id                  The crawler_resource_id value to delete the crawler plug-in (undeploy only)
  --help                Show this message
```

2. Deploy the crawler plug-in
```
$ scripts/manage_crawler_plugin.sh --endpoint endpoint --user username deploy --name plugin_name
```

3. List the deployed crawler plug-in
```
$ scripts/manage_crawler_plugin.sh --endpoint endpoint --user username list
```

4. Undeploy the crawler plug-in
```
$ scripts/manage_crawler_plugin.sh --endpoint endpoint --user username undeploy --id crawler_resource_id
```

## Contacts
### Discovery Product Manager
- Calin Furau/US/IBM

## History
- 2020-11-20 Initial
