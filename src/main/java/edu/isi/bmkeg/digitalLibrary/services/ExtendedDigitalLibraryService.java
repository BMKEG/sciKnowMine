package edu.isi.bmkeg.digitalLibrary.services;

import java.util.List;

import edu.isi.bmkeg.digitalLibrary.model.citations.ArticleCitation;
import edu.isi.bmkeg.ftd.model.FTDFragmentBlock;

public interface ExtendedDigitalLibraryService {

	ArticleCitation addPmidEncodedPdfToCorpus(byte[] pdfFileData, 
			String fileName, String corpusName) throws Exception;

	boolean removeFragmentBlock(FTDFragmentBlock frgBlk) throws Exception;
	
	List<String> listTermViews() throws Exception;

	int addArticlesToCorpus(List<Long> articleIds, Long corpusId) throws Exception;
	
	int removeArticlesFromCorpus(List<Long> articleIds, Long corpusId) throws Exception;

	boolean fullyDeleteArticle(Long articleId) throws Exception;

}