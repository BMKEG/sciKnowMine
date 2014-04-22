---
title: The SciKnowMine Triage Application
description:  We present here a user manual for running and maintaining a web-based system for peforming document triage given a corpus of PDF files. We will describe processes for installation, execution and maintenance of the system. 
layout: defaultTOC
prevPage: 04workingWithData.html
nextPage: 06deletingData.html
---

Command Line Tools - Reporting functions
---------

The system has three query command line functions for an administrator to query the state of the system from the command line.

The *reportCorpusCounts* command returns a formatted count of the contents of each target and triage corpus.

```
reportCorpusCounts -db DBNAME -l LOGIN -p PASSWD 

 -db DBNAME            : Database name
 -l LOGIN              : Database login
 -p PASSWD             : Database password
```

The *reportTriageCorpusContents* command returns a formatted list of all the documents in a given triage corpus (relating to a defined target corpus). 

```
reportTargetCorpusContents  -db DBNAME -l LOGIN -p PASSWD -targetCorpus CNAME

 -db DBNAME          : Database name
 -l LOGIN            : Database login
 -p PASSWD           : Database password
 -targetCorpus CNAME : Target Corpus Name
```

The *reportTriageCorpusContents* command returns a formatted list of all the documents in a given triage corpus. 

```
reportTriageCorpusContents -db DBNAME -l LOGIN -p PASSWD -targetCorpus CNAME -triageCorpus CNAME

 -db DBNAME          : Database name
 -l LOGIN            : Database login
 -p PASSWD           : Database password
 -targetCorpus CNAME : Target Corpus Name
 -triageCorpus CNAME : Triage Corpus Name
```