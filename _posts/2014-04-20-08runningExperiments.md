---
title: The SciKnowMine Triage Application
description:  We present here a user manual for running and maintaining a web-based system for peforming document triage given a corpus of PDF files. We will describe processes for installation, execution and maintenance of the system. 
layout: defaultTOC
prevPage: 07machineLearning.html
nextPage: 09webApp.html
---

8. ommand Line Tools - Running Experiments
---

We designed the system to permit developers to run machine learning natural language 
experiments on their data. We expect that we will need to design specialized components
for specific document classification tasks to optimize performance. This is where the 
system permits this process. 

At this point, we make extensive use of the ClearTk 'evaluation' (`cleartk-eval`) module 
(see https://code.google.com/p/cleartk/wiki/Modules). ClearTk-Eval provides pipeline 
functions within [UIMA](http://uima.apache.org/) that automatically run N-Fold validation
tests, calculate Tf-Idf, Mutual Information, _etc._ and calculate standard metrics of 
performance (Precision, Recall, F-Score, _etc._). The reason we currently work with 
ClearTk is that is written in Java and provides a common development platform for other 
popular machine learning libraries such as [SVMlight](http://svmlight.joachims.org/), 
[LIBSVM](http://www.csie.ntu.edu.tw/~cjlin/libsvm/), 
[LIBLINEAR](http://www.csie.ntu.edu.tw/~cjlin/liblinear/), 
[OpenNLPMaxEnt] (http://incubator.apache.org/opennlp/), [Weka](http://www.cs.waikato.ac.nz/ml/weka/)  
and [Mallet](http://mallet.cs.umass.edu/). Thus using (and maybe helping to extend) the
ClearTk API could provide a wide range of tools for SciKnowMine.   

Note that this work _currently requires working with source code_, since developers 
must create their own ClearTk features and pipelines. This is a work in progress and we
describe it here in it's early, development form. These are not commands that are 
packaged in the `skmTriage-1-1-5-SNAPSHOT` installer, but are available from within the 
codebase but may be run from the command line using the following structure:

``
java -classpath skmTriage-1.1.5-SNAPSHOT-jar-with-dependencies.jar <path.to.command> <arguments>
``

### Preprocessing the data.

```
java -classpath skmTriage-1.1.5-SNAPSHOT-jar-with-dependencies.jar 
edu.isi.bmkeg.skm.triage.cleartk.bin.PreprocessTriageScores
-triageCorpus <triage-corpus-name> -targetCorpus <target-corpus-name>
-dir <target-directory> -prop <proportion-of-docs-held-out> 
-l <login> -p <password> -db <database> -wd <workingDirectory>
              
 -targetCorpus NAME : The target corpus that we're linking to
 -triageCorpus NAME : The triage corpus to be evaluated. 
 -dir NAME 			: The directory where the ML data is to be extracted to 
 -prop FLOAT		: The proportion of documents to be held out (_e.g._ 0.1)
 -db DBNAME         : Database name
 -l LOGIN           : Database login
 -p PASSWD          : Database password
 -wd WORKINGDIR     : Working Directory 
```

This runs through the available text from each paper in the system and extracts it 
to the named directory in the following structure:

``
+ directory
|-+ <target_corpus_name> (e.g., 'Allele_Phenotype')
  |-+ <triage_corpus_name> (e.g., 'Hiroaki_Onda')
    |-+ test
      |-+ in.txt
      |-+ out.txt
    |-+ train
      |-+ in.txt
      |-+ out.txt
`` 

Each `in.txt` and `out.txt` file contains data formatted in the following way:

``
88177	< > During neural development , programmed cell death ... <all-text-from-the-paper> ... the insets in i and l. 
88302	< > Mitochondrial dysfunction has long been implicated ... <all-text-from-the-paper> ... to generate sufficient ATP . 
`` 

Each line starts with the database id value for the article citation and then contains the 
text of the article, extracted from the PDF and converted to XML through the LAPDF-Text 
library (and then rendered to text through the JATS XSLT system). This provides a standard
format for the data that may be processed by any NLP text mining system. 

### Running Experiments.

We provide the `RunEvaluationAcrossFeatures` class as a method for running evaluations
across multiple feature annotators. This is essentially a scripting program which 
runs the `PreprocessTriageScores` command a number of times followed by a set of feature 
extraction pipelines.

_This is a current focus of work within the system and is likely to change_. 

```
java -classpath skmTriage-1.1.5-SNAPSHOT-jar-with-dependencies.jar 
edu.isi.bmkeg.skm.triage.cleartk.bin.RunEvaluationAcrossFeatures 
-triageCorpus <triage-corpus-name> -targetCorpus <target-corpus-name>
-dir <target-directory> -prop <proportion-of-docs-held-out> 
-nRepeats <number-of-times-whole-process-will-repeat>
-l <login> -p <password> -db <database> -wd <workingDirectory>
                  
 -targetCorpus NAME : The target corpus that we're linking to
 -triageCorpus NAME : The triage corpus to be evaluated. 
 -dir NAME 			: The directory where the ML data is to be extracted to 
 -prop FLOAT		: The proportion of documents to be held out (_e.g._ 0.1)
 -nRepeats INT		: The number of times the pipeline will be run
 -db DBNAME         : Database name
 -l LOGIN           : Database login
 -p PASSWD          : Database password
 -wd WORKINGDIR     : Working Directory 
```

Currently, the system runs through a number of feature sets to check performance of 
document classification pipelines. These include (1) unigrams, (2) bigrams, (3) combined
uni- and bi-gram data, (4) tf-idf counts from unigrams only. 

The system generates a tab-delimited file called `results.txt` in the output directory,
where the evaluation metrics are delivered. 