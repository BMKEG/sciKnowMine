---
title: The SciKnowMine Triage Application
description:  We present here a user manual for running and maintaining a web-based system for peforming document triage given a corpus of PDF files. We will describe processes for installation, execution and maintenance of the system. 
layout: defaultTOC
prevPage: 06deletingData.html
nextPage: 07machineLearning.html
---

Command Line Tools - Using Machine learning classifiers
---

Before we can use the classifiers, we need to train them. This is done by the following command:

### Train Classifier.

```
triageDocumentsClassifier -train -targetCorpus "GO" [-homeDir /path/to/directory/for/model] 
                  -db <name-of-database> -l <login> -p <password> -wd <workingDirectory>
                  
 -db DBNAME         : Database name
 -homeDir DIR       : Directory where application data will be persisted
 -l LOGIN           : Database login
 -p PASSWD          : Database password
 -wd WORKINGDIR     : Working Directory 
 -targetCorpus NAME : The target corpus that we're linking to
 -train             : If present will train and generate model, if absent will
                      compute and update prediction scores in Triage Document.
                      Either -train or -predict should be specified.
 -triageCorpus NAME : The triage corpus to be evaluated. It is required if
                      -predict is used.
```

This runs through all the example data from all the triage corpora where the in-out code is set to 
either `in` or `out` and trains an SVM classifier (derived from a baseline set of features). There is an
option argument for where the model should be placed, if this is not set then the model will be saved in 
the home directory of the user running the command. 

### Use classifier to assign papers to target corpora.

Applying the classifier is accomplished with the following command:

```
triageDocumentsClassifier -predict -targetCorpus "GO" [-homeDir /path/to/directory/for/model] 
                  -db <name-of-database> -l <login> -p <password> -wd <workingDirectory>
                  
 -db DBNAME         : Database name
 -homeDir DIR       : Directory where application data will be persisted
 -l LOGIN           : Database login
 -p PASSWD          : Database password
 -wd WORKINGDIR     : Working Directory 
 -targetCorpus NAME : The target corpus that we're linking to
 -predict           : If present will compute and update prediction scores in
                      Triage Document. Either -train or -predict should be
                      specified.
 -triageCorpus NAME : The triage corpus to be evaluated. It is required if
                      -predict is used.
```

Note that execution of this command is exactly like the training step with a single option changed. This will 
generate scores for each paper in each category.  
This runs through all the example data from all the triage corpora where the in-out code is set to 
either `in` or `out` and trains an SVM classifier (derived from a baseline set of features). There is an
option argument for where the model should be placed, if this is not set then the model will be saved in 
the home directory of the user running the command. 
