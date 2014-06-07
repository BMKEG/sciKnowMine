---
title: The SciKnowMine Triage Application
description:  We present here a user manual for running and maintaining a web-based system for peforming document triage given a corpus of PDF files. We will describe processes for installation, execution and maintenance of the system. 
layout: defaultTOC
prevPage: 05reportingFunctions.html
nextPage: 07machineLearning.html
---
6. Command Line Tools - Deleting data 
----------------

We have three commands to edit data from the system

The *deleteTargetCorpus* will remove all traces of a given target corpus from the system. 

```
deleteTriageCorpus -db DBNAME -l LOGIN -p PASSWD -targetCorpus TARGET 

 -db DBNAME           : Database name
 -l LOGIN             : Database login
 -p PASSWD            : Database password
 -targetCorpus TARGET : Target Corpus Name
```

The *deleteTriageCorpus* will remove all traces of a given triage corpus from the system. 

```
deleteTriageCorpus -db DBNAME -l LOGIN -p PASSWD -targetCorpus TARGET

 -db DBNAME           : Database name
 -l LOGIN             : Database login
 -p PASSWD            : Database password
 -triageCorpus TRIAGE : Triage Corpus Name
```

The *deleteTriageScoresBasedOnCodefile* uses a code file (a list of formatted pmid_A.pdf file names) to remove paper's association with a given triage corpus. 

```
deleteTriageScoresBasedOnCodefile -codeList CODES -db DBNAME -l LOGIN -p PASSWD -triageCorpus CORPUS

 -codeList CODES      : Encoded files
 -db DBNAME           : Database name
 -l LOGIN             : Database login
 -p PASSWD            : Database password
 -triageCorpus CORPUS : Triage Corpus name
```
