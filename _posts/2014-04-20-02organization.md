---
title: The SciKnowMine Triage Application
description:  We present here a user manual for running and maintaining a web-based system for peforming document triage given a corpus of PDF files. We will describe processes for installation, execution and maintenance of the system. 
layout: defaultTOC
prevPage: 01installation.html
nextPage: 03setUp.html
---

2. System Organization 
---

The triage task is primarily concerned with sorting documents from an input document set assigned to 
a curator (called a '*triage corpus*') into a set of categories (where each category actually designates 
a set of documents that are each called a '*target corpus*'). The data that assigns each scientific article
from triage corpus to it's target corpus is it's '*in-out-code*' which can be one of three values: '*in*', '*out*' 
or '*unclassified*'. 

This simple construct forms the basis of the system and provides a relatively 
straightforward way to attach additional cues and information about each article's possible inclusion in
a target corpus based on NLP analysis of the document's contents. 

### The format of PDF file names.

Each PDF file being processed should start with it's pubmed id, followed by an underscore and a single letter 
denoting if it is to be included in a target corpus. Thus some examples of possible filenames are as follows:

* 19763139_A.pdf
* 19911007_AG.pdf
* 21470346.pdf

This indicates that the article with the PubMed id 19763139 is a member of the target corpus denoted by the
code 'A'. These codes are set when you create the target corpus. 
