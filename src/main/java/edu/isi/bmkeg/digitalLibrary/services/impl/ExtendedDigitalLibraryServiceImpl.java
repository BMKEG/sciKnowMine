package edu.isi.bmkeg.digitalLibrary.services.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.flex.remoting.RemotingDestination;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.io.Files;

import edu.isi.bmkeg.digitalLibrary.controller.DigitalLibraryEngine;
import edu.isi.bmkeg.digitalLibrary.dao.ExtendedDigitalLibraryDao;
import edu.isi.bmkeg.digitalLibrary.dao.impl.DigitalLibraryDaoImpl;
import edu.isi.bmkeg.digitalLibrary.model.citations.ArticleCitation;
import edu.isi.bmkeg.digitalLibrary.model.citations.Journal;
import edu.isi.bmkeg.digitalLibrary.services.ExtendedDigitalLibraryService;
import edu.isi.bmkeg.ftd.model.FTDFragmentBlock;
import edu.isi.bmkeg.lapdf.dao.vpdmf.LAPDFTextDaoImpl;
import edu.isi.bmkeg.utils.Converters;
import edu.isi.bmkeg.vpdmf.dao.CoreDao;
import edu.isi.bmkeg.vpdmf.model.definitions.VPDMf;
import edu.isi.bmkeg.vpdmf.model.definitions.ViewDefinition;

@RemotingDestination
@Transactional
@Service
public class ExtendedDigitalLibraryServiceImpl implements ExtendedDigitalLibraryService {

	private static final Logger logger = Logger.getLogger(ExtendedDigitalLibraryServiceImpl.class);

	@Autowired
	private ExtendedDigitalLibraryDao extDigLibDao;

	@Autowired
	private ApplicationContext ctx;
	
	private DigitalLibraryEngine de;
	
	public void setExtDigLibDao(ExtendedDigitalLibraryDao extDigLibDao) {
		this.extDigLibDao = extDigLibDao;
	}

	public void init() throws Exception {
		
		if( de == null ) {
			
			de = new DigitalLibraryEngine();
			de.setCitDao(extDigLibDao);
			CoreDao core = extDigLibDao.getCoreDao();
			de.setDigLibDao( new DigitalLibraryDaoImpl(core));
			de.setFtdDao(new LAPDFTextDaoImpl(core));
			
			File jLookupFile = ctx.getResource("classpath:edu/isi/bmkeg/digitalLibrary/journalAbbrLookup.jObj").getFile();
			byte[] jLookupBytes = Converters.fileContentsToBytesArray(jLookupFile);
			Object jLookupPObj = Converters.byteArrayToObject(jLookupBytes);
			de.setjLookup((Map<String,Journal>) jLookupPObj);
			
		}
		
	}
	
	public ArticleCitation addPmidEncodedPdfToCorpus(byte[] pdfFileData, 
			String fileName, String corpusName) throws Exception {
		
		init();
		
		//
		// TODO: 
		// pretty clunky way of doing this: dump to a file and then invoke command-line 
		// functions on that file. Need better solution based on data.
		//
		File tempDir = Files.createTempDir();
		File tempFile = new File(tempDir.getPath() + "/" + fileName);
		FileOutputStream output = new FileOutputStream(tempFile.getPath());
		IOUtils.write(pdfFileData, output);
		
		ArticleCitation ac = de.insertCodedPdfFile(tempFile, "pmid");
	
// 		TODO: Need to add the articles to the named corpus if the name is set. 
//		if( corpusName != null )
//			de.( 
//					mapPmidsToVpdmfids.keySet(), 
//					corpusName);
		
		Converters.recursivelyDeleteFiles(tempDir);

		return ac;

	}

	public boolean removeFragmentBlock(FTDFragmentBlock frgBlk) throws Exception {
		
		return extDigLibDao.removeFragmentBlock(frgBlk);
		
	}
	
	public List<String> listTermViews() throws Exception {

		VPDMf top = this.extDigLibDao.getCoreDao().getTop();
		List<String> termTrees = new ArrayList<String>();
	
		Iterator<ViewDefinition> vdIt = top.getViews().values().iterator();
		while( vdIt.hasNext() ) {
			ViewDefinition vd = vdIt.next();
			
			String addr = vd.getName();
			boolean termFlag = false;
			
			ViewDefinition tempVd = vd;
			while( tempVd.getParent() != null ) {
				tempVd = tempVd.getParent();				
				addr = tempVd.getName() + " > " + addr;
				if( tempVd.getName().equals("Term") )
					termFlag = true;
			}
			
			if(termFlag)
				termTrees.add(addr);
			
		}
		
		return termTrees;
		
	}
	
	public int addArticlesToCorpus(List<Long> articleIds, Long corpusId) throws Exception {
				
		// Hacky Bugfix
		List<Long> fixedIds = new ArrayList<Long>();
		Iterator articleIt = articleIds.iterator();
		while (articleIt.hasNext()) {
			 Object o = articleIt.next();
			 Long l = new Long(o.toString());
			 fixedIds.add(l);
		}
		
		return this.extDigLibDao.addArticlesToCorpusWithIds(fixedIds, corpusId);		
			
	}

	public int removeArticlesFromCorpus(List<Long> articleIds, Long corpusId) throws Exception {
		
		// Hacky Bugfix
		List<Long> fixedIds = new ArrayList<Long>();
		Iterator articleIt = articleIds.iterator();
		while (articleIt.hasNext()) {
			 Object o = articleIt.next();
			 Long l = new Long(o.toString());
			 fixedIds.add(l);
		}
		
		return this.extDigLibDao.removeArticlesFromCorpusWithIds(fixedIds, corpusId);		
			
	}
	
	public boolean fullyDeleteArticle(Long articleId) throws Exception {
		
		return this.extDigLibDao.fullyDeleteArticle(articleId);		
			
	}

}