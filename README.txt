Installation Instructions
===

We recommend building and running from source. This should be straightforward and is the most functional in terms of troubleshooting and bug-fixing. 

1. Clone the `bmkeg-parent` project from github to set all the dependency versions and repositories: `mvn clone https://github.com/BMKEG/bmkeg-parent`
2. Clone the `sciKnowMine` project. This is just the wrapper for the webapplication. `mvn clone https://github.com/BMKEG/sciKnowMine`
	* _If you want to build all components from scratch, see our [sciKnowMineProject](https://github.com/BMKEG/sciKnowMineProject) Github page.  
 

Configuration Required for each basic installation of the template (without webservices)

* Setting up the basic project

Edit the following files
1: /pom.xml (change project name, add new repositories and dependencies)
2: /.settings/org.eclipse.wst.common.component (change project name)
3: src/main/webapp/WEB-INF/bmkegdefault.properties (configuration for running applications)
4: /src/test/resources/bmkegtest.properties (configuration for running testss)

* Application-specific configuration

Add new bean definitions in new files (if required)
1: under /src/resources/path/to/your/application/applicationContext-<appName>.xml
2: edit /src/main/webapp/WEB-INF/web.xml to add a pointer to this new file.

* Eclipse stuff
Right click on project in Navigator and 'add Spring Project nature''

