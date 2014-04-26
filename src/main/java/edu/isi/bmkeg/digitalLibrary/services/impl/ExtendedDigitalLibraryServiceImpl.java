package edu.isi.bmkeg.digitalLibrary.services.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.flex.remoting.RemotingDestination;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.isi.bmkeg.digitalLibrary.controller.DigitalLibraryEngine;
import edu.isi.bmkeg.digitalLibrary.dao.ExtendedDigitalLibraryDao;
import edu.isi.bmkeg.digitalLibrary.dao.impl.DigitalLibraryDaoImpl;
import edu.isi.bmkeg.digitalLibrary.model.citations.ArticleCitation;
import edu.isi.bmkeg.digitalLibrary.model.citations.Journal;
import edu.isi.bmkeg.digitalLibrary.model.citations.JournalEpoch;
import edu.isi.bmkeg.digitalLibrary.model.qo.citations.ArticleCitation_qo;
import edu.isi.bmkeg.digitalLibrary.model.qo.citations.JournalEpoch_qo;
import edu.isi.bmkeg.digitalLibrary.model.qo.citations.Journal_qo;
import edu.isi.bmkeg.digitalLibrary.model.qo.citations.LiteratureCitation_qo;
import edu.isi.bmkeg.digitalLibrary.services.ExtendedDigitalLibraryService;
import edu.isi.bmkeg.ftd.dao.FtdDao;
import edu.isi.bmkeg.ftd.dao.impl.FtdDaoImpl;
import edu.isi.bmkeg.ftd.model.FTD;
import edu.isi.bmkeg.ftd.model.FTDFragmentBlock;
import edu.isi.bmkeg.ftd.model.FTDRuleSet;
import edu.isi.bmkeg.ftd.model.qo.FTD_qo;
import edu.isi.bmkeg.lapdf.controller.LapdfEngine;
import edu.isi.bmkeg.lapdf.controller.LapdfVpdmfEngine;
import edu.isi.bmkeg.lapdf.dao.vpdmf.LAPDFTextDaoImpl;
import edu.isi.bmkeg.lapdf.extraction.exceptions.ClassificationException;
import edu.isi.bmkeg.lapdf.model.LapdfDocument;
import edu.isi.bmkeg.lapdf.pmcXml.PmcXmlArticle;
import edu.isi.bmkeg.lapdf.xml.model.LapdftextXMLDocument;
import edu.isi.bmkeg.utils.Converters;
import edu.isi.bmkeg.utils.xml.XmlBindingTools;
import edu.isi.bmkeg.vpdmf.dao.CoreDao;
import edu.isi.bmkeg.vpdmf.model.definitions.VPDMf;
import edu.isi.bmkeg.vpdmf.model.definitions.ViewDefinition;
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

		File workDir = new File(this.extDigLibDao.getCoreDao()
				.getWorkingDirectory());

		ArticleCitation ac = null;

		try {

			this.extDigLibDao.getCoreDao().getCe().connectToDB();
			this.extDigLibDao.getCoreDao().getCe().turnOffAutoCommit();

			ac = de.insertCodedPdfFileName(fileName, "pmid");
			String pth = "pdfs/" + ac.getJournal().getAbbr() + "/"
					+ ac.getPubYear() + "/" + ac.getVolValue();
			pth = pth.replaceAll("\\s+", "_");
			File pdfDir = new File(workDir.getPath() + "/" + pth);
			boolean status = pdfDir.mkdirs();

			if (!status && !pdfDir.exists()) {
				throw new Exception(
						"Could not create directories for PDF file. Is "
								+ workDir + " writable?");
			}

			File pdfFile = new File(pdfDir.getPath() + "/" + fileName);

			// note: we always overwrite any existing files.
			FileOutputStream output = new FileOutputStream(pdfFile.getPath());
			IOUtils.write(pdfFileData, output);

			LapdfDocument doc = de.blockifyFile(pdfFile);

			/*
			 * FTDRuleSet rs = null; JournalEpoch je =
			 * this.extDigLibDao.retriveJournalEpochForCitation(ac); if( je !=
			 * null && je.getRules() != null) { rs = je.getRules(); File
			 * tempRuleFile = new File(workDir.getPath()+ "/" +
			 * rs.getFileName()); this.extDigLibDao.dumpRuleFileToDisk(rs,
			 * tempRuleFile); de.setRuleFile(tempRuleFile); } else { rs =
			 * this.extDigLibDao.readRuleFileFromDisk(de.getRuleFile()); }
			 * 
			 * de.classifyDocument(doc, de.getRuleFile());
			 */

			de.getExtDigLibDao().addPdfToArticleCitation(doc, ac, pdfFile);

			this.extDigLibDao.getCoreDao().getCe().commitTransaction();

		} catch (Exception e) {

			e.printStackTrace();
			this.extDigLibDao.getCoreDao().getCe().rollbackTransaction();
			throw e;

		} finally {

			this.extDigLibDao.getCoreDao().getCe().closeDbConnection();

		}

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
			Map<String, Map<Integer, Long>> epochs = new HashMap<String, Map<Integer, Long>>();

			l = this.extDigLibDao.getCoreDao().listInTrans(
					new JournalEpoch_qo(), "JournalEpoch");
			for (LightViewInstance lvi : l) {

				Matcher epochMatch = epochPatt.matcher(lvi.getVpdmfLabel());
				if (epochMatch.find()) {

					String j = epochMatch.group(1);
					Integer s = new Integer(epochMatch.group(2));
					Integer e = new Integer(epochMatch.group(3));

					Map<Integer, Long> temp = new HashMap<Integer, Long>();
					if (epochs.containsKey(j)) {
						temp = epochs.get(j);
					}
					for (int i = s; i <= e; i++) {
						temp.put(i, -1L);
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

			ResultSet rs = this.extDigLibDao.getCoreDao().getCe()
					.executeRawSqlQuery(sql);
			while (rs.next()) {
				Long jId = rs.getLong("vpdmfId");
				String abbr = rs.getString("abbr");
				String vol = rs.getString("volume");

				Matcher numMatch = numPatt.matcher(vol);
				if (numMatch.find()) {
					String vStr = numMatch.group(1);
					Integer v = new Integer(vStr);

					if (!epochs.containsKey(abbr)) {
						epochs.put(abbr, new HashMap<Integer, Long>());
					}
					if (!epochs.get(abbr).containsKey(v)
							|| epochs.get(abbr).get(v) != -1L) {
						epochs.get(abbr).put(v, jId);
					} else {
						int i = 0;
						i++;
					}
				}
			}

			JournalEpoch je = null;
			for (String abbr : epochs.keySet()) {

				Integer[] array = epochs.get(abbr).keySet()
						.toArray(new Integer[0]);
				Arrays.sort(array);

				for (int i = 0; i < array.length; i++) {
					Integer v = array[i];
					Long jId = epochs.get(abbr).get(v);

					if (je != null && epochs.containsKey(abbr)
							&& !epochs.get(abbr).containsKey(v - 1)) {
						l.add(this.convertEpochToLvi(je));
						je = null;
					}

					if (epochs.get(abbr).get(v) == -1L)
						continue;

					if (je == null) {
						je = this.generateNewJournalEpoch(abbr, jId);
						je.setStartVol(v);
					}

					je.setEndVol(v);

				}

			}
			rs.close();

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
			String wdPth = this.extDigLibDao.getCoreDao().getWorkingDirectory();
			File ruleDir = new File(wdPth + "/rules");
			File ruleFile = new File(ruleDir.getPath() + "/"
					+ ruleSet.getFileName());

			String s = ruleSet.getFileName();
			ruleSet.setRsName(s.substring(0, s.length() - 4));

			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// Get the original LAPDFtext Document
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			LapdfVpdmfEngine lapdfEng = new LapdfVpdmfEngine();

			File xmlFile = new File(wdPth + "/pdfs/" + ftd.getXmlFile());
			File pmcXmlFile = new File(wdPth + "/pdfs/" + ftd.getPmcXmlFile());
			String xml = FileUtils.readFileToString(xmlFile, "UTF-8");

			LapdfDocument document = lapdfEng.blockifyXml(xml);

			lapdfEng.classifyDocument(document, ruleFile);

			LapdftextXMLDocument lapdfXml = document
					.convertToLapdftextXmlFormat();
			FileWriter writer = new FileWriter(xmlFile);
			XmlBindingTools.generateXML(lapdfXml, writer);

			PmcXmlArticle xml2 = document.convertToPmcXmlFormat();
			FileWriter writer2 = new FileWriter(pmcXmlFile);
			XmlBindingTools.generateXML(xml2, writer2);

			ftd.setRuleSet(ruleSet);

			this.extDigLibDao.getCoreDao()
					.updateInTrans(ftd, "ArticleDocument");

			this.extDigLibDao.getCoreDao().getCe().commitTransaction();

			return articleId;

		} catch (Exception e) {

			e.printStackTrace();

		} finally {

			this.extDigLibDao.getCoreDao().getCe().closeDbConnection();

		}

		return -1L;

	}

	@Override
	public Long runRuleSetOnJournalEpoch(Long epochId) throws Exception {

		try {

			this.extDigLibDao.getCoreDao().getCe().connectToDB();
			this.extDigLibDao.getCoreDao().getCe().turnOffAutoCommit();

			runRulesOverEpochInTrans(epochId);

			this.extDigLibDao.getCoreDao().getCe().commitTransaction();

			return epochId;

		} catch (Exception e) {

			e.printStackTrace();

		} finally {

			this.extDigLibDao.getCoreDao().getCe().closeDbConnection();

		}

		return -1L;

	}

	@Override
	public void runRulesOverAllEpochs() throws Exception {

		List<LightViewInstance> epochs = listExtendedJournalEpochs();

		try {

			this.extDigLibDao.getCoreDao().getCe().connectToDB();
			this.extDigLibDao.getCoreDao().getCe().turnOffAutoCommit();

			for (LightViewInstance epochLvi : epochs) {
				logger.info("Epoch: " + epochLvi.getVpdmfLabel() );
				this.runRulesOverEpochInTrans(epochLvi.getVpdmfId());
			}

			this.extDigLibDao.getCoreDao().getCe().commitTransaction();

		} catch (Exception e) {

			e.printStackTrace();

		} finally {

			this.extDigLibDao.getCoreDao().getCe().closeDbConnection();

		}

	}

	private Long runRulesOverEpochInTrans(Long epochId) throws Exception,
			IOException, ClassificationException, JAXBException {
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Note that we are retrieving an ArticleDocument view
		// which contains the FTD objects associated with this
		// ArticleCitation.
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		JournalEpoch_qo jeQo = new JournalEpoch_qo();
		jeQo.setVpdmfId(epochId.toString());

		List<LightViewInstance> l = this.extDigLibDao.getCoreDao().listInTrans(
				jeQo, "JournalEpoch");

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
		jQo.setAbbr(epoch.getJournal().getAbbr());

		List<LightViewInstance> l2 = this.extDigLibDao.getCoreDao()
				.listInTrans(fQo, "ArticleDocument");

		// ~~~~~~~~~~~~~~~~~~~~~~
		// Retrieve the rule set.
		// ~~~~~~~~~~~~~~~~~~~~~~
		FTDRuleSet ruleSet = this.extDigLibDao.getCoreDao().findByIdInTrans(
				epoch.getRules().getVpdmfId(), new FTDRuleSet(), "FTDRuleSet");

		if (ruleSet == null) {
			return -1L;
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Dump rulefile to disk on server
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		String wdPth = this.extDigLibDao.getCoreDao().getWorkingDirectory();
		File ruleDir = new File(wdPth + "/rules");
		File ruleFile = new File(ruleDir.getPath() + "/"
				+ ruleSet.getFileName());

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Get the original LAPDFtext Document
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		for (LightViewInstance lvi : l2) {

			FTD ftd = this.extDigLibDao.getCoreDao().findByIdInTrans(
					lvi.getVpdmfId(), new FTD(), "ArticleDocument");

			//if( ftd.getPmcLoaded() )
			//	continue;
			
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// Get ready to run update query on this view.
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			ViewBasedObjectGraph vbog = new ViewBasedObjectGraph(
					this.extDigLibDao.getCoreDao().getTop(), this.extDigLibDao
							.getCoreDao().getCl(), "FTD");
			ViewInstance vi = vbog.objectGraphToView(ftd, true);
			this.extDigLibDao.getCoreDao().getCe()
					.storeViewInstanceForUpdate(vi);

			LapdfVpdmfEngine lapdfEng = new LapdfVpdmfEngine();

			String s = ftd.getName();
			ftd.setPmcXmlFile(s.substring(0, s.length() - 4) + "_pmc.xml");
			ftd.setPmcLoaded(true);

			File xmlFile = new File(wdPth + "/pdfs/" + ftd.getXmlFile());
			File pmcXmlFile = new File(wdPth + "/pdfs/" + ftd.getPmcXmlFile());

			String xml = FileUtils.readFileToString(xmlFile, "UTF-8");

			LapdfDocument document = lapdfEng.blockifyXml(xml);
			lapdfEng.classifyDocument(document, ruleFile);

			LapdftextXMLDocument lapdfXml = document
					.convertToLapdftextXmlFormat();
			FileWriter writer = new FileWriter(xmlFile);
			XmlBindingTools.generateXML(lapdfXml, writer);
			logger.info("Writing " + xmlFile.getPath() );

			PmcXmlArticle xml2 = null;
			try {
				xml2 = document.convertToPmcXmlFormat();
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}

			FileWriter writer2 = new FileWriter(pmcXmlFile);
			XmlBindingTools.generateXML(xml2, writer2);
			logger.info("Writing " + pmcXmlFile.getPath() );

			ftd.setRuleSet(ruleSet);

			this.extDigLibDao.getCoreDao()
					.updateInTrans(ftd, "ArticleDocument");

		}

		return epochId;

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

			String wdPth = this.extDigLibDao.getCoreDao().getWorkingDirectory();
			File xmlFile = new File(wdPth + "/" + ftd.getXmlFile());
			String xml = FileUtils.readFileToString(xmlFile, "UTF-8");

			LapdfDocument document = lapdfEng.blockifyXml(xml);
			return lapdfEng.dumpFeaturesToSpreadsheetString(document);

		} catch (Exception e) {

			e.printStackTrace();

		} finally {

			this.extDigLibDao.getCoreDao().getCe().closeDbConnection();

		}

		return "";

	}

	public byte[] loadSwf(Long vpdmfId) throws Exception {

		init();

		CoreDao coreDao = this.extDigLibDao.getCoreDao();
		FtdDao ftdDao = new FtdDaoImpl(coreDao);

		FTD_qo qFtd = new FTD_qo();
		LiteratureCitation_qo lc = new LiteratureCitation_qo();
		qFtd.setCitation(lc);
		lc.setVpdmfId(String.valueOf(vpdmfId));
		List<LightViewInstance> l = ftdDao.listArticleDocument(qFtd);

		if (l.size() > 1) {
			return null;
		}

		Resource logoSwf = new ClassPathResource(
				"edu/isi/bmkeg/digitalLibrary/rest/00000.swf");
		byte[] logoSwfBytes = IOUtils.toByteArray(logoSwf.getInputStream());

		if (l.size() == 1) {
			vpdmfId = l.get(0).getVpdmfId();
			FTD ftd = ftdDao.findArticleDocumentById(vpdmfId);

			String wd = coreDao.getWorkingDirectory();
			File laSwfFile = new File(wd + "/pdfs/" + ftd.getLaswfFile());
			byte[] laSwf = Converters.fileContentsToBytesArray(laSwfFile);

			return laSwf;

		}

		return logoSwfBytes;

	}
	
	public String loadXml(Long vpdmfId) throws Exception {

		init();

		CoreDao coreDao = this.extDigLibDao.getCoreDao();
		FtdDao ftdDao = new FtdDaoImpl(coreDao);

		FTD_qo qFtd = new FTD_qo();
		LiteratureCitation_qo lc = new LiteratureCitation_qo();
		qFtd.setCitation(lc);
		lc.setVpdmfId(String.valueOf(vpdmfId));
		List<LightViewInstance> l = ftdDao.listArticleDocument(qFtd);

		if (l.size() > 1) {
			return null;
		}

		LapdftextXMLDocument xmlDoc = new LapdftextXMLDocument();
		StringWriter writer = new StringWriter();
		XmlBindingTools.generateXML(xmlDoc, writer);				
		String xml = writer.toString();
		
		if (l.size() == 1) {
			vpdmfId = l.get(0).getVpdmfId();
			FTD ftd = ftdDao.findArticleDocumentById(vpdmfId);
			
			String wd = ftdDao.getCoreDao().getWorkingDirectory();
			File xmlFile = new File( wd + "/pdfs/" + ftd.getXmlFile() );
			
			if( !xmlFile.exists() ) {
				return null;
			}

			xml = FileUtils.readFileToString(xmlFile, "UTF-8");
			
		}

		return xml;

	}

	public String loadPmcXml(Long vpdmfId) throws Exception {

		init();

		CoreDao coreDao = this.extDigLibDao.getCoreDao();
		FtdDao ftdDao = new FtdDaoImpl(coreDao);

		FTD_qo qFtd = new FTD_qo();
		LiteratureCitation_qo lc = new LiteratureCitation_qo();
		qFtd.setCitation(lc);
		lc.setVpdmfId(String.valueOf(vpdmfId));
		List<LightViewInstance> l = ftdDao.listArticleDocument(qFtd);

		if (l.size() > 1) {
			return null;
		}

		LapdftextXMLDocument xmlDoc = new LapdftextXMLDocument();
		StringWriter writer = new StringWriter();
		XmlBindingTools.generateXML(xmlDoc, writer);				
		String pmcXml = writer.toString();
		
		if (l.size() == 1) {
			vpdmfId = l.get(0).getVpdmfId();
			FTD ftd = ftdDao.findArticleDocumentById(vpdmfId);
			
			String wd = ftdDao.getCoreDao().getWorkingDirectory();
			File pmcXmlFile = new File( wd + "/pdfs/" +  ftd.getPmcXmlFile() );
			
			if( !pmcXmlFile.exists() ) {
				return null;
			}

			pmcXml = FileUtils.readFileToString(pmcXmlFile, "UTF-8");
			
		}

		return pmcXml;

	}
	
	public String loadHtml(Long vpdmfId) throws Exception {

		init();

		CoreDao coreDao = this.extDigLibDao.getCoreDao();
		FtdDao ftdDao = new FtdDaoImpl(coreDao);

		FTD_qo qFtd = new FTD_qo();
		LiteratureCitation_qo lc = new LiteratureCitation_qo();
		qFtd.setCitation(lc);
		lc.setVpdmfId(String.valueOf(vpdmfId));
		List<LightViewInstance> l = ftdDao.listArticleDocument(qFtd);

		if (l.size() > 1) {
			return null;
		}

		LapdftextXMLDocument xmlDoc = new LapdftextXMLDocument();
		StringWriter writer = new StringWriter();
		XmlBindingTools.generateXML(xmlDoc, writer);				
		String html = writer.toString();
		
		if (l.size() == 1) {
			vpdmfId = l.get(0).getVpdmfId();
			FTD ftd = ftdDao.findArticleDocumentById(vpdmfId);
			
			String wd = ftdDao.getCoreDao().getWorkingDirectory();
			File pmcXmlFile = new File( wd + "/pdfs/" +  ftd.getPmcXmlFile() );
			
			if( !pmcXmlFile.exists() ) {
				return null;
			}
			FileReader inputReader = new FileReader(pmcXmlFile);
			StringWriter outputWriter = new StringWriter();
			
			TransformerFactory tf = TransformerFactory.newInstance();
			Resource xslResource = new ClassPathResource(
					"jatsPreviewStyleSheets/xslt/main/jats-html.xsl"
					);
			StreamSource xslt = new StreamSource(xslResource.getInputStream());
			Transformer transformer = tf.newTransformer(xslt);

			StreamSource source = new StreamSource(inputReader);
			StreamResult result = new StreamResult(outputWriter);
			transformer.transform(source, result);
			html = outputWriter.toString();  
						
		}

		return html;

	}


	
}
