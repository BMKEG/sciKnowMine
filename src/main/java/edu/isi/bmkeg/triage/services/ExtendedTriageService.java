package edu.isi.bmkeg.triage.services;

import java.io.File;
import java.util.List;

import edu.isi.bmkeg.digitalLibrary.model.citations.ArticleCitation;
import edu.isi.bmkeg.ftd.model.FTDFragmentBlock;
import edu.isi.bmkeg.ftd.model.FTDRuleSet;
import edu.isi.bmkeg.vpdmf.model.instances.LightViewInstance;

public interface ExtendedTriageService {

	boolean addPmidEncodedPdfToTriageCorpus(byte[] pdfFileData, 
			String fileName, String triageCorpusName, Long ruleSetId, 
			byte[] codeFileContents) throws Exception;

	boolean trainClassifier(String targetCorpus) throws Exception;

	boolean runClassifier(String targetCorpus, String triageCorpus) throws Exception;
	
	List<String> readAllCorpusCounts() throws Exception;
	
	void transferTriageInsToArticleCorpora() throws Exception;
	
}