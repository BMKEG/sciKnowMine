package edu.isi.bmkeg.triage.services.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.flex.messaging.MessageTemplate;
import org.springframework.flex.remoting.RemotingDestination;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.io.Files;

import edu.isi.bmkeg.digitalLibrary.dao.ExtendedDigitalLibraryDao;
import edu.isi.bmkeg.digitalLibrary.dao.impl.DigitalLibraryDaoImpl;
import edu.isi.bmkeg.digitalLibrary.model.citations.ArticleCitation;
import edu.isi.bmkeg.digitalLibrary.model.citations.Journal;
import edu.isi.bmkeg.digitalLibrary.model.qo.citations.ArticleCitation_qo;
import edu.isi.bmkeg.digitalLibrary.model.qo.citations.Corpus_qo;
import edu.isi.bmkeg.ftd.model.FTDRuleSet;
import edu.isi.bmkeg.lapdf.dao.vpdmf.LAPDFTextDaoImpl;
import edu.isi.bmkeg.lapdf.model.LapdfDocument;
import edu.isi.bmkeg.skm.triage.cleartk.bin.TriageDocumentsClassifier;
import edu.isi.bmkeg.skm.triage.controller.TriageEngine;
import edu.isi.bmkeg.skm.triage.dao.vpdmf.TriageDaoExImpl;
import edu.isi.bmkeg.triage.dao.impl.TriageDaoImpl;
import edu.isi.bmkeg.triage.model.TriageCorpus;
import edu.isi.bmkeg.triage.model.qo.TriageCorpus_qo;
import edu.isi.bmkeg.triage.model.qo.TriageScore_qo;
import edu.isi.bmkeg.triage.services.ExtendedTriageService;
import edu.isi.bmkeg.uml.model.UMLclass;
import edu.isi.bmkeg.utils.Converters;
import edu.isi.bmkeg.vpdmf.dao.CoreDao;
import edu.isi.bmkeg.vpdmf.model.definitions.PrimitiveLink;
import edu.isi.bmkeg.vpdmf.model.definitions.VPDMf;
import edu.isi.bmkeg.vpdmf.model.definitions.ViewDefinition;
import edu.isi.bmkeg.vpdmf.model.instances.LightViewInstance;
import edu.isi.bmkeg.vpdmf.model.instances.ViewInstance;

@RemotingDestination
@Transactional
@Service
public class ExtendedTriageServiceImpl implements
		ExtendedTriageService {

	private static final Logger logger = Logger
			.getLogger(ExtendedTriageServiceImpl.class);

	@Autowired
	private ExtendedDigitalLibraryDao extDigLibDao;

	@Autowired
	private ApplicationContext ctx;

	private MessageTemplate template;
	
	private TriageEngine te;

	public void setExtDigLibDao(ExtendedDigitalLibraryDao extDigLibDao) {
		this.extDigLibDao = extDigLibDao;
	}
	
	@Autowired
	public void setTemplate(MessageTemplate template) {
		this.template = template;
	}

	public void init() throws Exception {

		if (te == null) {

			te = new TriageEngine();
			te.setCitDao(extDigLibDao);
			CoreDao core = extDigLibDao.getCoreDao();
			te.setDigLibDao(new DigitalLibraryDaoImpl(core));
			te.setFtdDao(new LAPDFTextDaoImpl(core));
			te.setExTriageDao(new TriageDaoExImpl(core));
			te.setTriageDao(new TriageDaoImpl(core));

			File jLookupFile = ctx
					.getResource(
							"classpath:edu/isi/bmkeg/digitalLibrary/journalAbbrLookup.jObj")
					.getFile();
			byte[] jLookupBytes = Converters
					.fileContentsToBytesArray(jLookupFile);
			Object jLookupPObj = Converters.byteArrayToObject(jLookupBytes);
			te.setjLookup((Map<String, Journal>) jLookupPObj);
			
		}

	}

	@Override
	public boolean addPmidEncodedPdfToTriageCorpus(byte[] pdfFileData,
			String fileName, String triageCorpusName, Long ruleSetId,
			byte[] codeFileContents) throws Exception {

		init();

		template.send("serverUpdates", 
				"Triage Engine Initialization Complete");

		//
		// TODO:
		// pretty clunky way of doing this: dump to a file and then invoke
		// command-line
		// functions on that file. Need better solution based on data.
		//
		File tempDir = Files.createTempDir();
		File pdfFile = new File(tempDir.getPath() + "/" + fileName);
		FileOutputStream output = new FileOutputStream(pdfFile.getPath());
		IOUtils.write(pdfFileData, output);

		template.send("serverUpdates", 
				"Transferring PDF to Server");
		
		CoreDao coreDao = this.te.getExTriageDao().getCoreDao();

		try {

			coreDao.getCe().connectToDB();
			coreDao.getCe().turnOffAutoCommit();

			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// Go get the FTDRuleSet
			//
			String rsPath = null;
			if( ruleSetId != -1 ) {
				FTDRuleSet rs = coreDao.findByIdInTrans(
						ruleSetId, 
						new FTDRuleSet(), 
						"FTDRuleSet");
				if( rs.getExcelFile() != null ) {
					rsPath = tempDir.getPath() + "/" + rs.getFileName();
					FileOutputStream fos = new FileOutputStream(rsPath);
					fos.write(rs.getExcelFile());
					fos.close();
					te.setRuleFile(new File(rsPath));
					template.send("serverUpdates", 
							"Using Rule Set " 
							+ rs.getVpdmfLabel());
				} else {
					throw new Exception("Error with rule file " + 
							ruleSetId);	
				}
			} else {
				throw new Exception("Error with rule file, id: " + ruleSetId); 
			}
							
			File cc = null;
			if( codeFileContents != null && codeFileContents.length > 0) {
				cc = new File(tempDir.getPath() + "/classificationCodes.txt");
				FileUtils.writeByteArrayToFile(cc, codeFileContents);
			}
			
			template.send("serverUpdates", 
					"Retrieving Triage Corpus " 
					+ triageCorpusName);
			TriageCorpus tc = te.findTriageCorpusByNameInTrans(triageCorpusName);
			if( tc == null ) {
				throw new Exception("TriageCorpus " + triageCorpusName + " does not exist.");
			}
			
			template.send("serverUpdates", "Inserting citations for " 
					+ pdfFile.getName() );
			ArticleCitation ac = te.insertCodedPdfFile(pdfFile, "pmid");

			template.send("serverUpdates", "Finding text blocks (" 
					+ pdfFile.getName() + ")" );
			LapdfDocument doc = te.blockifyFile(pdfFile);

			template.send("serverUpdates", "Classifying text blocks (" 
					+ pdfFile.getName() + ")" );
			te.classifyDocument(doc, te.getRuleFile());
			
			template.send("serverUpdates", "Add PDF to article citation (" 
					+ pdfFile.getName() + ")" );
			this.extDigLibDao.addPdfToArticleCitation(doc, ac, pdfFile, te.getRuleFile());
			
			template.send("serverUpdates", "Assigning In / Out code codes to article (" 
					+ pdfFile.getName() + ")" );
			Map<Integer, String> codeList = te.compileCodeList(pdfFile);
			if (cc != null)
				codeList.putAll(te.compileCodeList(cc));
			te.addCodeListToCorpus(tc, codeList);			

			te.getDigLibDao().getCoreDao().commitTransaction();

		} catch(Exception e) {
			
			coreDao.getCe().rollbackTransaction();
			template.send("serverUpdates", pdfFile.getName() + " upload failed." );
			logger.error(e.toString());
			
			return false;
		
		} finally {
			
			if( coreDao != null && coreDao.getCe() != null)
				coreDao.getCe().closeDbConnection();
			
			Converters.recursivelyDeleteFiles(tempDir);
			
		}

		template.send("serverUpdates", pdfFile.getName() + " upload complete." );
		return true;

	}
	
	@Override
	public boolean trainClassifier(String targetCorpus) throws Exception {

		init();
		
		File homeDir = new File(System.getProperty("user.home")
				+ "/bmkeg");
		if (!homeDir.exists()) {
			homeDir.mkdirs();
		}
		File modelDir = new File(homeDir, this.extDigLibDao.getCoreDao().getUri() + "/"
			+ targetCorpus);
		
		TriageDocumentsClassifier cl = new TriageDocumentsClassifier(
				null, targetCorpus, modelDir,
				this.extDigLibDao.getCoreDao().getLogin(), 
				this.extDigLibDao.getCoreDao().getPassword(), 
				this.extDigLibDao.getCoreDao().getUri() );
		cl.setMsgTemplate(this.template);

		cl.run(true);
					
		return true;
		
	}

	@Override
	public boolean runClassifier(String targetCorpus, String triageCorpus) throws Exception {

		init();
		
		File homeDir = new File(System.getProperty("user.home")
				+ "/bmkeg");
		if (!homeDir.exists()) {
			homeDir.mkdirs();
		}
		File modelDir = new File(homeDir, this.extDigLibDao.getCoreDao().getUri() + "/"
			+ targetCorpus);
		
		TriageDocumentsClassifier cl = new TriageDocumentsClassifier(
				null, targetCorpus, modelDir,
				this.extDigLibDao.getCoreDao().getLogin(), 
				this.extDigLibDao.getCoreDao().getPassword(), 
				this.extDigLibDao.getCoreDao().getUri() );
		cl.setMsgTemplate(this.template);

		cl.run(false);
				
		return true;
	
	}
	
	@Override
	public List<String> readAllCorpusCounts() throws Exception {
	
		List<String> counts = new ArrayList<String>();
		
		init();

		CoreDao coreDao = this.te.getExTriageDao().getCoreDao();

		try {

			coreDao.getCe().connectToDB();
			coreDao.getCe().turnOffAutoCommit();

			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// Go get the FTDRuleSet
			//
			String rsPath = null;
			
			Corpus_qo cQo = new Corpus_qo();
			List<LightViewInstance> lviList = coreDao.listInTrans(cQo, "Corpus");

			List<String> targetCorpora = new ArrayList<String>();
			List<String> triageCorpora = new ArrayList<String>();
			for( LightViewInstance lvi : lviList ) {
				
				if( lvi.getDefName().contains( "TriageCorpus") ) {
					triageCorpora.add(lvi.getVpdmfLabel());
				} else {
					targetCorpora.add(lvi.getVpdmfLabel());
				}
				
			}
			
			for(String target: targetCorpora) {
				for(String triage: triageCorpora) {
					
					TriageScore_qo tsQo = new TriageScore_qo();
					Corpus_qo targetCorpusQo = new Corpus_qo();
					targetCorpusQo.setVpdmfLabel(target);
					tsQo.setTargetCorpus(targetCorpusQo);
					TriageCorpus_qo triageCorpusQo = new TriageCorpus_qo();
					tsQo.setTriageCorpus(triageCorpusQo);
					triageCorpusQo.setVpdmfLabel(triage);
					
					tsQo.setInOutCode("in");
					int inCount = coreDao.countViewInTrans(tsQo, "TriageScore");

					tsQo.setInOutCode("out");
					int outCount = coreDao.countViewInTrans(tsQo, "TriageScore");

					tsQo.setInOutCode("unclassified");
					int uncCount = coreDao.countViewInTrans(tsQo, "TriageScore");
					
					String c = target + "{|}" + 
							triage + "{|}" + 
							inCount + "{|}" + 
							outCount + "{|}" + 
							uncCount; 
					
					counts.add(c);

				}		

				ArticleCitation_qo articleQo = new ArticleCitation_qo();
				Corpus_qo articleCorpusQo = new Corpus_qo();
				articleQo.getCorpora().add(articleCorpusQo);
				articleCorpusQo.setName(target);
				
				int inCount = coreDao.countViewInTrans(articleQo, "ArticleCitation");

				String c = target + "{|}" + 
						"final" + "{|}" + 
						inCount + "{|}" + 
						-1 + "{|}" + 
						-1; 
				
				counts.add(c);
				
			}
			
		} catch(Exception e) {
			
			return null;
		
		} finally {
			
			if( coreDao != null && coreDao.getCe() != null)
				coreDao.getCe().closeDbConnection();
			
		}

		return counts;
		
	}
	
	@Override
	public void transferTriageInsToArticleCorpora() throws Exception {

		try {

			CoreDao core = this.extDigLibDao.getCoreDao();

			ViewDefinition vd = core.getCe().readTop().getViews()
					.get("ArticleCorpus");

			ViewInstance vi = new ViewInstance(vd);

			PrimitiveLink pl = (PrimitiveLink) vd.getSubGraph().getEdges()
					.iterator().next();
			UMLclass link = pl.getRole().getAss().getLinkClass();

			core.getCe().connectToDB();
			core.getCe().turnOffAutoCommit();

			VPDMf top = core.getCe().readTop();

			//
			// 1 - list all articles assigned 'in' to each targetCorpus
			//
			List<Integer> acIdList = new ArrayList<Integer>();
			Corpus_qo cQo = new Corpus_qo();
			List<LightViewInstance> cList = core.listInTrans(cQo,
					"ArticleCorpus");
			for (LightViewInstance cLvi : cList) {

				String corpusName = cLvi.getVpdmfLabel();
				Long corpusId = cLvi.getVpdmfId();
				String sql = " SELECT DISTINCT LiteratureCitation_0__ViewTable.vpdmfId " +
						"FROM ViewTable AS LiteratureCitation_0__ViewTable, " +
						" LiteratureCitation AS LiteratureCitation_0__LiteratureCitation, " +
						" ViewTable AS TargetCorpus_0__ViewTable, " +
						" Corpus AS TargetCorpus_0__Corpus, " +
						" ViewTable AS TriageScore_0__ViewTable, " +
						" TriageScore AS TriageScore_0__TriageScore " +
						"WHERE LiteratureCitation_0__LiteratureCitation.vpdmfId=LiteratureCitation_0__ViewTable.vpdmfId AND " +
						" TargetCorpus_0__Corpus.vpdmfId=TargetCorpus_0__ViewTable.vpdmfId AND " +
						" TargetCorpus_0__Corpus.name='" + corpusName + "' AND " +
						" TriageScore_0__ViewTable.viewType LIKE '%.TriageScore.%' AND " +
						" TriageScore_0__TriageScore.vpdmfId=TriageScore_0__ViewTable.vpdmfId AND " +
						" TriageScore_0__TriageScore.inOutCode='in' AND " +
						" LiteratureCitation_0__LiteratureCitation.vpdmfId=TriageScore_0__TriageScore.citation_id AND " +
						" TargetCorpus_0__Corpus.vpdmfId=TriageScore_0__TriageScore.targetCorpus_id";
				
				ResultSet rs = core.getCe().executeRawSqlQuery(sql);
				List<Long> citIds = new ArrayList<Long>();
				while( !rs.isLast() ) {
					rs.next();
					Long vpdmfId = rs.getLong("vpdmfId");
					citIds.add(vpdmfId);
				}
			
				//
				// 2 - clear all existing citations from the corpus
				// (note that means that *only* documents that have been assigned 'in' 
				// in at least one triage corpus would be assigned. 
				// All others would be removed.
				//
				sql = "DELETE Corpus_corpora__resources_LiteratureCitation__Corpus_0__LiteratureCitation_0.* " +
						" FROM LiteratureCitation AS LiteratureCitation_0__LiteratureCitation, " +
						" Corpus AS Corpus_0__Corpus, " +
						" Corpus_corpora__resources_LiteratureCitation AS Corpus_corpora__resources_LiteratureCitation__Corpus_0__LiteratureCitation_0 " +
						" WHERE Corpus_0__Corpus.vpdmfId=Corpus_corpora__resources_LiteratureCitation__Corpus_0__LiteratureCitation_0.corpora_id AND " + 
						" Corpus_0__Corpus.name = '" + corpusName + "'";
						
				core.getCe().executeRawUpdateQuery(sql);

				//
				// 3 - add all new assignments 
				//	
				for( Long citId : citIds ) {
					sql = "INSERT INTO Corpus_corpora__resources_LiteratureCitation " +
							" (corpora_id, resources_id) " +
							" VALUES (" + 
							corpusId+ ", " + 
							citId + ");";				
					core.getCe().executeRawUpdateQuery(sql);
				}
			
			}

			core.getCe().commitTransaction();

		} catch (Exception e) {

			e.printStackTrace();
			this.extDigLibDao.getCoreDao().getCe().rollbackTransaction();

		} finally {

			this.extDigLibDao.getCoreDao().getCe().closeDbConnection();

		}

	}

	
}