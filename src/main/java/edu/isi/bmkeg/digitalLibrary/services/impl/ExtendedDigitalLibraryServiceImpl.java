package edu.isi.bmkeg.digitalLibrary.services.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.StringWriter;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
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
import edu.isi.bmkeg.digitalLibrary.model.citations.JournalEpoch;
import edu.isi.bmkeg.digitalLibrary.model.qo.citations.ArticleCitation_qo;
import edu.isi.bmkeg.digitalLibrary.model.qo.citations.Corpus_qo;
import edu.isi.bmkeg.digitalLibrary.model.qo.citations.JournalEpoch_qo;
import edu.isi.bmkeg.digitalLibrary.model.qo.citations.Journal_qo;
import edu.isi.bmkeg.digitalLibrary.services.ExtendedDigitalLibraryService;
import edu.isi.bmkeg.ftd.model.FTD;
import edu.isi.bmkeg.ftd.model.FTDFragmentBlock;
import edu.isi.bmkeg.ftd.model.FTDRuleSet;
import edu.isi.bmkeg.ftd.model.qo.FTD_qo;
import edu.isi.bmkeg.lapdf.controller.LapdfVpdmfEngine;
import edu.isi.bmkeg.lapdf.dao.vpdmf.LAPDFTextDaoImpl;
import edu.isi.bmkeg.lapdf.model.LapdfDocument;
import edu.isi.bmkeg.lapdf.pmcXml.PmcXmlArticle;
import edu.isi.bmkeg.lapdf.xml.model.LapdftextXMLDocument;
import edu.isi.bmkeg.triage.model.qo.TriageScore_qo;
import edu.isi.bmkeg.uml.model.UMLclass;
import edu.isi.bmkeg.utils.Converters;
import edu.isi.bmkeg.utils.xml.XmlBindingTools;
import edu.isi.bmkeg.vpdmf.dao.CoreDao;
import edu.isi.bmkeg.vpdmf.model.definitions.PrimitiveLink;
import edu.isi.bmkeg.vpdmf.model.definitions.VPDMf;
import edu.isi.bmkeg.vpdmf.model.definitions.ViewDefinition;
import edu.isi.bmkeg.vpdmf.model.instances.AttributeInstance;
import edu.isi.bmkeg.vpdmf.model.instances.ClassInstance;
import edu.isi.bmkeg.vpdmf.model.instances.LightViewInstance;
import edu.isi.bmkeg.vpdmf.model.instances.ViewBasedObjectGraph;
import edu.isi.bmkeg.vpdmf.model.instances.ViewInstance;

@RemotingDestination
@Transactional
@Service
public class ExtendedDigitalLibraryServiceImpl implements
		ExtendedDigitalLibraryService {

	private static final Logger logger = Logger
			.getLogger(ExtendedDigitalLibraryServiceImpl.class);

	@Autowired
	private ExtendedDigitalLibraryDao extDigLibDao;

	@Autowired
	private ApplicationContext ctx;

	private DigitalLibraryEngine de;

	public void setExtDigLibDao(ExtendedDigitalLibraryDao extDigLibDao) {
		this.extDigLibDao = extDigLibDao;
	}

	public void init() throws Exception {

		if (de == null) {

			de = new DigitalLibraryEngine();
			de.setCitDao(extDigLibDao);
			CoreDao core = extDigLibDao.getCoreDao();
			de.setDigLibDao(new DigitalLibraryDaoImpl(core));
			de.setFtdDao(new LAPDFTextDaoImpl(core));

			File jLookupFile = ctx
					.getResource(
							"classpath:edu/isi/bmkeg/digitalLibrary/journalAbbrLookup.jObj")
					.getFile();
			byte[] jLookupBytes = Converters
					.fileContentsToBytesArray(jLookupFile);
			Object jLookupPObj = Converters.byteArrayToObject(jLookupBytes);
			de.setjLookup((Map<String, Journal>) jLookupPObj);

		}

	}

	public ArticleCitation addPmidEncodedPdfToCorpus(byte[] pdfFileData,
			String fileName, String corpusName) throws Exception {

		init();

		//
		// TODO:
		// pretty clunky way of doing this: dump to a file and then invoke
		// command-line
		// functions on that file. Need better solution based on data.
		//
		File tempDir = Files.createTempDir();
		File tempFile = new File(tempDir.getPath() + "/" + fileName);
		FileOutputStream output = new FileOutputStream(tempFile.getPath());
		IOUtils.write(pdfFileData, output);

		ArticleCitation ac = de.insertCodedPdfFile(tempFile, "pmid");

		LapdfDocument doc = de.blockifyFile(tempFile);

		de.classifyDocument(doc, de.getRuleFile());

		de.getExtDigLibDao().addPdfToArticleCitation(doc, ac, tempFile,
				de.getRuleFile());

		// TODO: Need to add the articles to the named corpus if the name is
		// set.
		// if( corpusName != null )
		// de.(
		// mapPmidsToVpdmfids.keySet(),
		// corpusName);

		Converters.recursivelyDeleteFiles(tempDir);

		return ac;

	}

	public boolean removeFragmentBlock(FTDFragmentBlock frgBlk)
			throws Exception {

		return extDigLibDao.removeFragmentBlock(frgBlk);

	}

	public List<String> listTermViews() throws Exception {

		VPDMf top = this.extDigLibDao.getCoreDao().getTop();
		List<String> termTrees = new ArrayList<String>();

		Iterator<ViewDefinition> vdIt = top.getViews().values().iterator();
		while (vdIt.hasNext()) {
			ViewDefinition vd = vdIt.next();

			String addr = vd.getName();
			boolean termFlag = false;

			ViewDefinition tempVd = vd;
			while (tempVd.getParent() != null) {
				tempVd = tempVd.getParent();
				addr = tempVd.getName() + " > " + addr;
				if (tempVd.getName().equals("Term"))
					termFlag = true;
			}

			if (termFlag)
				termTrees.add(addr);

		}

		return termTrees;

	}

	public int addArticlesToCorpus(List<Long> articleIds, Long corpusId)
			throws Exception {

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

	public int removeArticlesFromCorpus(List<Long> articleIds, Long corpusId)
			throws Exception {

		// Hacky Bugfix
		List<Long> fixedIds = new ArrayList<Long>();
		Iterator articleIt = articleIds.iterator();
		while (articleIt.hasNext()) {
			Object o = articleIt.next();
			Long l = new Long(o.toString());
			fixedIds.add(l);
		}

		return this.extDigLibDao.removeArticlesFromCorpusWithIds(fixedIds,
				corpusId);

	}

	public boolean fullyDeleteArticle(Long articleId) throws Exception {

		return this.extDigLibDao.fullyDeleteArticle(articleId);

	}

	/**
	 * This function lists all existing Journal Epochs in the database (i.e.,
	 * those epochs that are specifically named and have a rule file associated
	 * with them). It also lists all possible epochs based on articles with
	 * journals.
	 */
	@Override
	public List<LightViewInstance> listExtendedJournalEpochs() throws Exception {

		Set<String> exitingEpochs = new HashSet<String>();

		List<LightViewInstance> l = null;
		Pattern numPatt = Pattern.compile("(\\d+)");

		// Strings in epochs are formatted '$journal$ ($start$-$end$)'
		Pattern epochPatt = Pattern.compile("^(.*) \\((\\d+)-(\\d+)\\)$");

		try {

			this.extDigLibDao.getCoreDao().getCe().connectToDB();

			// This is the data structure to keep track
			// of what epochs are defined in the database.
			Map<String, Map<Integer, LightViewInstance>> epochs = new HashMap<String, Map<Integer, LightViewInstance>>();

			l = this.extDigLibDao.getCoreDao().listInTrans(
					new JournalEpoch_qo(), "JournalEpoch");
			for (LightViewInstance lvi : l) {

				Matcher epochMatch = epochPatt.matcher(lvi.getVpdmfLabel());
				if (epochMatch.find()) {

					String j = epochMatch.group(1);
					Integer s = new Integer(epochMatch.group(2));
					Integer e = new Integer(epochMatch.group(3));

					Map<Integer, LightViewInstance> temp = new HashMap<Integer, LightViewInstance>();
					if (epochs.containsKey(j)) {
						temp = epochs.get(j);
					}
					for (int i = s; i <= e; i++) {
						temp.put(i, lvi);
					}
					epochs.put(j, temp);

				} else {

					throw new Exception("Can't match JournalEpoch label:"
							+ lvi.getVpdmfLabel());

				}

			}

			String sql = "SELECT DISTINCT JournalLU_0__Journal.abbr, "
					+ "JournalLU_0__Journal.vpdmfId, "
					+ "LiteratureCitation_0__ArticleCitation.volume "
					+ "FROM Journal AS JournalLU_0__Journal, "
					+ "ArticleCitation AS LiteratureCitation_0__ArticleCitation "
					+ "WHERE JournalLU_0__Journal.vpdmfId=LiteratureCitation_0__ArticleCitation.journal_id "
					+ "ORDER BY JournalLU_0__Journal.abbr, "
					+ "LiteratureCitation_0__ArticleCitation.volume";

			Map<String, Long> ids = new HashMap<String, Long>();

			int lowest = -1, highest = -1;
			JournalEpoch je = null;

			ResultSet rs = this.extDigLibDao.getCoreDao().getCe()
					.executeRawSqlQuery(sql);
			rs.first();
			while (!rs.isAfterLast()) {
				Long jId = rs.getLong("vpdmfId");
				String abbr = rs.getString("abbr");
				String vol = rs.getString("volume");

				Matcher numMatch = numPatt.matcher(vol);
				if (numMatch.find()) {
					String vStr = numMatch.group(1);
					Integer v = new Integer(vStr);

					if (je == null
							&& (!epochs.containsKey(abbr) || (epochs
									.containsKey(abbr) && !epochs.get(abbr)
									.containsKey(v)))) {
						je = this.generateNewJournalEpoch(abbr, jId);
						je.setStartVol(v);
					}

					if (je != null) {
						je.setEndVol(v);
					}

					if (je != null && epochs.containsKey(abbr)
							&& epochs.get(abbr).containsKey(v + 1)) {
						l.add(this.convertEpochToLvi(je));
						je = null;
					}

				}
				ids.put(abbr, jId);
				rs.next();
			}

			if (je != null) {
				l.add(this.convertEpochToLvi(je));
			}

		} finally {

			this.extDigLibDao.getCoreDao().getCe().closeDbConnection();

		}

		return l;

	}

	private JournalEpoch generateNewJournalEpoch(String jAbbr, Long id) {

		JournalEpoch je = new JournalEpoch();
		Journal j = new Journal();
		j.setAbbr(jAbbr);
		j.setVpdmfId(id);
		je.setJournal(j);
		je.setStartVol(-1);
		je.setEndVol(-1);

		return je;

	}

	private LightViewInstance convertEpochToLvi(JournalEpoch je)
			throws Exception {

		ViewBasedObjectGraph vbog = new ViewBasedObjectGraph(this.extDigLibDao
				.getCoreDao().getTop(), this.extDigLibDao.getCoreDao().getCl(),
				"JournalEpoch");
		ViewInstance vi = vbog.objectGraphToView(je, true);
		vi.updateIndexes();
		String[] completeIdxTuple = vi.generateCompleteIndexTuple();
		LightViewInstance lvi = vi.makeLightViewInstance();
		lvi.setIndexTupleFields(completeIdxTuple[0]);
		lvi.setIndexTuple(completeIdxTuple[1]);
		lvi.setDefinition(null);

		return lvi;

	}

	@Override
	public Long addRuleFileToJournalEpoch(Long ruleFileId, Long epochId,
			String epochJournal, int epochStart, int epochEnd) throws Exception {

		Long id = -1L;

		try {

			this.extDigLibDao.getCoreDao().getCe().connectToDB();

			FTDRuleSet ruleSet = this.extDigLibDao
					.getCoreDao()
					.findByIdInTrans(ruleFileId, new FTDRuleSet(), "FTDRuleSet");

			if (epochId != 0) {

				JournalEpoch epoch = this.extDigLibDao.getCoreDao()
						.findByIdInTrans(epochId, new JournalEpoch(),
								"JournalEpoch");

				epoch.setRules(ruleSet);

				id = this.extDigLibDao.getCoreDao().updateInTrans(epoch,
						"JournalEpoch");

			} else {

				JournalEpoch epoch = new JournalEpoch();
				epoch.setStartVol(epochStart);
				epoch.setEndVol(epochEnd);
				epoch.setRules(ruleSet);

				Journal j = new Journal();
				epoch.setJournal(j);
				j.setAbbr(epochJournal);

				/*
				 * Journal_qo jQo = new Journal_qo(); jQo.setAbbr(epochJournal);
				 * List<LightViewInstance> l =
				 * this.extDigLibDao.getCoreDao().listInTrans(jQo, "Journal");
				 */

				id = this.extDigLibDao.getCoreDao().insertInTrans(epoch,
						"JournalEpoch");

				int pause = 0;
				pause++;

			}

		} catch (Exception e) {

			e.printStackTrace();
			throw e;

		} finally {

			this.extDigLibDao.getCoreDao().getCe().closeDbConnection();

		}

		return id;
	}

	@Override
	public FTDRuleSet retrieveFTDRuleSetForArticleCitation(Long articleId)
			throws Exception {

		try {

			// Strings in epochs are formatted '$journal$ ($start$-$end$)'
			Pattern epochPatt = Pattern.compile("^(.*) \\((\\d+)-(\\d+)\\)$");
			Pattern numPatt = Pattern.compile("(\\d+)");

			this.extDigLibDao.getCoreDao().getCe().connectToDB();

			ArticleCitation ac = this.extDigLibDao.getCoreDao()
					.findByIdInTrans(articleId, new ArticleCitation(),
							"ArticleCitation");

			Matcher numMatch = numPatt.matcher(ac.getVolume());
			Integer v = -1;
			if (numMatch.find()) {
				String vStr = numMatch.group(1);
				v = new Integer(vStr);
			}

			JournalEpoch_qo jeQo = new JournalEpoch_qo();
			Journal_qo jQo = new Journal_qo();
			jeQo.setJournal(jQo);
			jQo.setAbbr(ac.getJournal().getAbbr());

			List<LightViewInstance> jeList = this.extDigLibDao.getCoreDao()
					.listInTrans(jeQo, "JournalEpoch");

			for (LightViewInstance lvi : jeList) {

				Matcher epochMatch = epochPatt.matcher(lvi.getVpdmfLabel());

				if (epochMatch.find()) {
					String j = epochMatch.group(1);
					Integer s = new Integer(epochMatch.group(2));
					Integer e = new Integer(epochMatch.group(3));

					// We have a match!
					// Go get that JournalEpoch and return the FTDRuleSet
					if (s <= v && e >= v) {
						JournalEpoch found = this.extDigLibDao.getCoreDao()
								.findByIdInTrans(lvi.getVpdmfId(),
										new JournalEpoch(), "JournalEpoch");
						return found.getRules();
					}

				}

				int pause = 0;
				pause++;

			}

		} catch (Exception e) {

			e.printStackTrace();
			throw e;

		} finally {

			this.extDigLibDao.getCoreDao().getCe().closeDbConnection();

		}

		return null;

	}

	@Override
	public Long runRuleSetOnArticleCitation(Long ruleSetId, Long articleId)
			throws Exception {

		File tempDir = null;
		try {

			this.extDigLibDao.getCoreDao().getCe().connectToDB();
			this.extDigLibDao.getCoreDao().getCe().turnOffAutoCommit();

			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// Note that we are retrieving an ArticleDocument view
			// which contains the FTD objects associated with this
			// ArticleCitation.
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			ArticleCitation_qo aQo = new ArticleCitation_qo();
			FTD_qo fQo = new FTD_qo();
			fQo.setCitation(aQo);
			aQo.setVpdmfId(articleId.toString());

			List<LightViewInstance> l = this.extDigLibDao.getCoreDao()
					.listInTrans(fQo, "ArticleDocument");

			FTD ftd = null;
			if (l.size() == 1) {
				ftd = this.extDigLibDao.getCoreDao().findByIdInTrans(
						l.get(0).getVpdmfId(), new FTD(), "ArticleDocument");
			} else {
				return -1L;
			}

			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// Get ready to run update query on this view.
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			ViewBasedObjectGraph vbog = new ViewBasedObjectGraph(
					this.extDigLibDao.getCoreDao().getTop(), this.extDigLibDao
							.getCoreDao().getCl(), "FTD");
			ViewInstance vi = vbog.objectGraphToView(ftd, true);
			this.extDigLibDao.getCoreDao().getCe()
					.storeViewInstanceForUpdate(vi);

			// ~~~~~~~~~~~~~~~~~~~~~~
			// Retrieve the rule set.
			// ~~~~~~~~~~~~~~~~~~~~~~
			FTDRuleSet ruleSet = this.extDigLibDao.getCoreDao()
					.findByIdInTrans(ruleSetId, new FTDRuleSet(), "FTDRuleSet");

			if (ruleSet == null) {
				return -1L;
			}

			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// Dump rulefile to disk on server
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			tempDir = Files.createTempDir();
			File ruleFile = new File(tempDir.getPath() + "/"
					+ ruleSet.getFileName());
			String s = ruleSet.getFileName();
			ruleSet.setRsName(s.substring(0, s.length() - 4));
			ruleSet.setRsDescription("");
			if (s.endsWith(".drl")) {
				FileUtils.writeStringToFile(ruleFile, ruleSet.getRuleBody());
			} else if (ruleSet.getFileName().endsWith("csv")) {
				FileUtils.writeStringToFile(ruleFile, ruleSet.getCsv());
			} else if (ruleSet.getFileName().endsWith("_drl.xls")) {
				FileUtils
						.writeByteArrayToFile(ruleFile, ruleSet.getExcelFile());
			}

			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// Get the original LAPDFtext Document
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			LapdfVpdmfEngine lapdfEng = new LapdfVpdmfEngine(ruleFile);
			LapdfDocument document = lapdfEng.blockifyXml(ftd.getXml());
			lapdfEng.classifyDocument(document, ruleFile);

			LapdftextXMLDocument xml = document.convertToLapdftextXmlFormat();
			StringWriter writer = new StringWriter();
			XmlBindingTools.generateXML(xml, writer);
			ftd.setXml(writer.toString());

			PmcXmlArticle xml2 = document.convertToPmcXmlFormat();
			StringWriter writer2 = new StringWriter();
			XmlBindingTools.generateXML(xml2, writer2);
			ftd.setPmcXml(writer2.toString());

			ftd.setRuleSet(ruleSet);

			FileWriter tempWriter = new FileWriter(new File(
					ruleFile.getParent() + "/temp.xml"));
			XmlBindingTools.generateXML(xml, tempWriter);

			this.extDigLibDao.getCoreDao()
					.updateInTrans(ftd, "ArticleDocument");

			this.extDigLibDao.getCoreDao().getCe().commitTransaction();

			return articleId;

		} catch (Exception e) {

			e.printStackTrace();

		} finally {

			this.extDigLibDao.getCoreDao().getCe().closeDbConnection();
			if (tempDir != null)
				Converters.recursivelyDeleteFiles(tempDir);

		}

		return -1L;

	}
	
	@Override
	public Long runRuleSetOnJournalEpoch(Long epochId)
			throws Exception {

		File tempDir = null;
		try {

			this.extDigLibDao.getCoreDao().getCe().connectToDB();
			this.extDigLibDao.getCoreDao().getCe().turnOffAutoCommit();

			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// Note that we are retrieving an ArticleDocument view
			// which contains the FTD objects associated with this
			// ArticleCitation.
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			JournalEpoch_qo jeQo = new JournalEpoch_qo();
			jeQo.setVpdmfId(epochId.toString());

			List<LightViewInstance> l = this.extDigLibDao.getCoreDao()
					.listInTrans(jeQo, "JournalEpoch");

			JournalEpoch epoch = null;
			if (l.size() == 1) {
				epoch = this.extDigLibDao.getCoreDao().findByIdInTrans(
						l.get(0).getVpdmfId(), new JournalEpoch(), "JournalEpoch");
			} else {
				return -1L;
			}

			FTD_qo fQo = new FTD_qo();
			ArticleCitation_qo aQo = new ArticleCitation_qo();
			fQo.setCitation(aQo);
			Journal_qo jQo = new Journal_qo();
			aQo.setJournal(jQo);
			aQo.setVolValue("<vpdmf-gteq>" + epoch.getStartVol() 
					+ "<vpdmf-and><vpdmf-lteq>" + epoch.getEndVol());
			jQo.setAbbr( epoch.getJournal().getAbbr() );

			List<LightViewInstance> l2 = this.extDigLibDao.getCoreDao()
					.listInTrans(fQo, "ArticleDocument");

			// ~~~~~~~~~~~~~~~~~~~~~~
			// Retrieve the rule set.
			// ~~~~~~~~~~~~~~~~~~~~~~
			FTDRuleSet ruleSet = this.extDigLibDao.getCoreDao()
					.findByIdInTrans(epoch.getRules().getVpdmfId(), 
							new FTDRuleSet(), "FTDRuleSet");

			if (ruleSet == null) {
				return -1L;
			}

			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// Dump rulefile to disk on server
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			tempDir = Files.createTempDir();
			File ruleFile = new File(tempDir.getPath() + "/"
					+ ruleSet.getFileName());
			String s = ruleSet.getFileName();
			ruleSet.setRsName(s.substring(0, s.length() - 4));
			ruleSet.setRsDescription("");
			if (s.endsWith(".drl")) {
				FileUtils.writeStringToFile(ruleFile, ruleSet.getRuleBody());
			} else if (ruleSet.getFileName().endsWith("csv")) {
				FileUtils.writeStringToFile(ruleFile, ruleSet.getCsv());
			} else if (ruleSet.getFileName().endsWith("_drl.xls")) {
				FileUtils
						.writeByteArrayToFile(ruleFile, ruleSet.getExcelFile());
			}

			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// Get the original LAPDFtext Document
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			for(LightViewInstance lvi : l2) {
				
				FTD ftd = this.extDigLibDao.getCoreDao().findByIdInTrans(
							lvi.getVpdmfId(), new FTD(), "ArticleDocument");
				
				// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
				// Get ready to run update query on this view.
				// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
				ViewBasedObjectGraph vbog = new ViewBasedObjectGraph(
						this.extDigLibDao.getCoreDao().getTop(), this.extDigLibDao
								.getCoreDao().getCl(), "FTD");
				ViewInstance vi = vbog.objectGraphToView(ftd, true);
				this.extDigLibDao.getCoreDao().getCe()
						.storeViewInstanceForUpdate(vi);
				
				LapdfVpdmfEngine lapdfEng = new LapdfVpdmfEngine(ruleFile);
			
				LapdfDocument document = lapdfEng.blockifyXml(ftd.getXml());
				lapdfEng.classifyDocument(document, ruleFile);

				LapdftextXMLDocument xml = document.convertToLapdftextXmlFormat();
				StringWriter writer = new StringWriter();
				XmlBindingTools.generateXML(xml, writer);
				ftd.setXml(writer.toString());

				PmcXmlArticle xml2 = document.convertToPmcXmlFormat();
				StringWriter writer2 = new StringWriter();
				XmlBindingTools.generateXML(xml2, writer2);
				ftd.setPmcXml(writer2.toString());

				ftd.setRuleSet(ruleSet);

				FileWriter tempWriter = new FileWriter(new File(
						ruleFile.getParent() + "/temp.xml"));
				XmlBindingTools.generateXML(xml, tempWriter);

				this.extDigLibDao.getCoreDao()
					.updateInTrans(ftd, "ArticleDocument");

				this.extDigLibDao.getCoreDao().getCe().commitTransaction();
			
			}
			
			return epochId;

		} catch (Exception e) {

			e.printStackTrace();

		} finally {

			this.extDigLibDao.getCoreDao().getCe().closeDbConnection();
			if (tempDir != null)
				Converters.recursivelyDeleteFiles(tempDir);

		}

		return -1L;

	}

	@Override
	public String generateRuleFileFromLapdf(Long articleId) throws Exception {

		try {

			this.extDigLibDao.getCoreDao().getCe().connectToDB();

			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// Note that we are retrieving an ArticleDocument view
			// which contains the FTD objects associated with this
			// ArticleCitation.
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			ArticleCitation_qo aQo = new ArticleCitation_qo();
			FTD_qo fQo = new FTD_qo();
			fQo.setCitation(aQo);
			aQo.setVpdmfId(articleId.toString());

			List<LightViewInstance> l = this.extDigLibDao.getCoreDao()
					.listInTrans(fQo, "ArticleDocument");

			FTD ftd = null;
			if (l.size() == 1) {
				ftd = this.extDigLibDao.getCoreDao().findByIdInTrans(
						l.get(0).getVpdmfId(), new FTD(), "ArticleDocument");
			} else {
				return "";
			}

			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// Get the original LAPDFtext Document
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			LapdfVpdmfEngine lapdfEng = new LapdfVpdmfEngine();
			LapdfDocument document = lapdfEng.blockifyXml(ftd.getXml());
			return lapdfEng.dumpFeaturesToSpreadsheetString(document);

		} catch (Exception e) {

			e.printStackTrace();

		} finally {

			this.extDigLibDao.getCoreDao().getCe().closeDbConnection();

		}

		return "";

	}

}
