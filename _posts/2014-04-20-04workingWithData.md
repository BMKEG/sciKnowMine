---
title: The SciKnowMine Triage Application
description:  We present here a user manual for running and maintaining a web-based system for peforming document triage given a corpus of PDF files. We will describe processes for installation, execution and maintenance of the system. 
layout: defaultTOC
prevPage: 03setUp.html
nextPage: 05reportingFunctions.html
---

Command Line Tools - Working with data
-----------------------

### Creating a Target Corpora 

The first step to running the system is to build corpora that the triaged articles are being sorted into. 

The following example would create a corpus named 'GO', owned by a user called 'Rocky' with the single letter code 'G'. 
Each target corpus could be all papers concerned with Gene Ontology curation or all papers curated into 
the database as a whole. 

```
editArticleCorpus -name "GO" -desc "Gene Ontology" -owner "Rocky" -regex "G" 
                  -db <name-of-database> -l <login> -p <password> -wd <workingDirectory>
 -db DBNAME        : Database name
 -desc DESCRIPTION : Corpus description
 -l LOGIN          : Database login
 -name NAME        : Corpus name
 -owner OWNER      : Corpus owner
 -p PASSWD         : Database password
 -wd WORKINGDIR    : Working Directory 
 -regex REGEX      : Regular expression to recognize incoming files (optional)
```

> Note that the first time you run a database command in this system, the system needs to generate a lookup object for the many Journals referenced in pubmed. This is quite slow the first time you run the command, but is a one-time step.

### Creating a Triage Corpora 

The next step is to build the triage corpora that hold the articles. 

A triage corpus is a special kind of corpus used to organize a collection of articles for the triaging procedure. Each triage corpus should denote a natural collection of papers (such as all those papers assigned to a specific individual or all papers from a given Journal). A triage corpus is the entry point for a paper in the system.

The following example would create a triage corpus named 'curator1', owned by a user called 'Curator 1'. 

```
editTriageCorpus -name "curator1" -desc "Curator 1's triage corpus" -owner "Curator 1"
                  -db <name-of-database> -l <login> -p <password> -wd <workingDirectory>

 -db DBNAME        : Database name
 -desc DESCRIPTION : Corpus description
 -l LOGIN          : Database login
 -name NAME        : Corpus name
 -owner OWNER      : Corpus owner
 -p PASSWD         : Database password  
 -wd WORKINGDIR    : Working Directory            
```

### Loading Articles into a Triage Corpus 
 
The crucial task of loading files into a triage corpus is performed by the following function:

```
buildTriageCorpusFromPdfDir -pdfs </complete/path/to/pdf/directory> -triageCorpus "<triage-corpus-name>" 
                  -rules </path/to/rules/file> -codeList <path/to/codeList/file>
                  -db <name-of-database> -l <login> -p <password> -wd <workingDirectory>

 -codeList CODES       : Encoded file names + codes (optional)
 -triageCorpus CORPUS        : Corpus name
 -db DBNAME            : Database name
 -l LOGIN              : Database login
 -p PASSWD             : Database password
 -pdfs PDF-DIR-OR-FILE : Pdfs directory or file
 -rules FILE           : Rules file (optional) 
 -wd WORKINGDIR        : Working Directory 

```

This will run through all files in the targeted directory and load them into the named triage corpus. Note 
that the system will iterate over *every target corpus* and assign an in-out code to every article. The user 
may supply formatted codes in a text file (using the `-codeList` option) rather than editing each file
name on disk. Thus, if a PDF file has the filename `19763139.pdf` with an entry `19763139_A.pdf` in the codeList
file, it would be assigned a code of `in` to the target corpus designated by the letter `A` and a code of `out` to 
all others. If no codes are assigned either from the file names or from the codeList then *all articles in the
upload will be assigned a code of `unassigned` for all corpora. 

Note also that the way that the text is extracted from the PDF files uses rule files for the `LAPDF-Text` system. 
You may use a specified rule file here to improve performance of the text extraction if necessary.  
