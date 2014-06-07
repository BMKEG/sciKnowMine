---
title: The SciKnowMine Triage Application
description:  Instructions for installing the SciKnowMine system.
layout: defaultTOC
prevPage: 01installation.html
nextPage: 02organization.html
---

1. Installation Manual
===

*Note that this system is provided with no warranty or guarantee* 

The SciKnowMine system permits two modes of use: (A) using 
*command-line tools to administer the server, and upload/process scientific articles into corpora* and 
(B) use the *web interface to assign articles to specific corpora*. 

Here we describe installation instructions for the *command line tools* 

Pre-Installation Requirements 
----------------

* MySQL 5.1 (http://www.mysql.com/)
	* http://dev.mysql.com/downloads/mysql/5.1.html
* SwfTools (http://www.swftools.org/)
	* http://wiki.swftools.org/wiki/Installation

The Server:

* Must own a port number to process http requests from client web browsers.
* Must be able to send http requests to http://eutils.ncbi.nlm.nih.gov (PubMed's eCitation services).
* Must be able to login to MySql with a user defined login with privileges to create (and destroy) databases.

Installation
------------

### Installing and running the command line tools from prebuilt binaries.

This system is provided as a `\*.tar.gz` archive for Unix and Linux systems, 
a `\*.dmg` instalallable for Macs and an `\*.exe` installable for PCs.

* Mac: [skmTriage\_macos\_1\_1\_5\-SNAPSHOT.dmg](http://bmkeg2.s3-website-us-west-2.amazonaws.com/000_sciknowmine/skmTriage_macos_1_1_5-SNAPSHOT.dmg)
* Unix: [skmTriage\_unix\_1\_1\_5\-SNAPSHOT.tar.gz](http://bmkeg2.s3-website-us-west-2.amazonaws.com/000_sciknowmine/skmTriage_unix_1_1_5-SNAPSHOT.tar.gz)
* Windows: [skmTriage\_windows\-x64\_1\_1\_5-SNAPSHOT.exe](http://bmkeg2.s3-website-us-west-2.amazonaws.com/000_sciknowmine/skmTriage_windows-x64_1_1_5-SNAPSHOT.exe)

### Installing and running the command line tools from source

The command line tools are implemented within the `https://github.com/BMKEG/skmTriage` 
library. 

```
0. git clone https://github.com/BMKEG/bmkeg-parent
	# This pulls in the dependencies for libraries 
	# available through maven. 
1. git clone https://github.com/BMKEG/skmTriage
2. cd skmTriage
3. mvn -DskipTests assembly:assembly 
	# This builds the assembled library from all dependencies,
	# but skips unit tests within the code that could take a lot of time to run 
4. Running commands using the jar file follows exactly the same logic as for prebuilt 
   installed tools but requires each command to be issued using java directly based on: 
   $ java -jar target/skmTriage-1.1.5-SNAPSHOT-jar-with-dependencies.jar 
     [path.to.executable] [command.options]   
```

