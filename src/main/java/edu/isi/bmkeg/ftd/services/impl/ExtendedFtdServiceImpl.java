package edu.isi.bmkeg.ftd.services.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.flex.remoting.RemotingDestination;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.isi.bmkeg.ftd.model.FTD;
import edu.isi.bmkeg.ftd.model.FTDRuleSet;
import edu.isi.bmkeg.ftd.model.qo.FTDRuleSet_qo;
import edu.isi.bmkeg.ftd.services.ExtendedFtdService;
import edu.isi.bmkeg.lapdf.controller.LapdfVpdmfEngine;
import edu.isi.bmkeg.lapdf.dao.LAPDFTextDao;
import edu.isi.bmkeg.vpdmf.dao.CoreDao;
import edu.isi.bmkeg.vpdmf.model.instances.LightViewInstance;

@RemotingDestination
@Transactional
@Service
public class ExtendedFtdServiceImpl implements ExtendedFtdService {

	@Autowired
	private LAPDFTextDao lapdftextDao;

	private LapdfVpdmfEngine eng;
	
	public LAPDFTextDao getLapdftextDao() {
		return lapdftextDao;
	}

	public void setLapdftextDao(LAPDFTextDao lapdftextDao) {
		this.lapdftextDao = lapdftextDao;
	}

	public void init() throws Exception {

		if (eng == null) {

			eng = new LapdfVpdmfEngine();
			eng.setFtdDao(lapdftextDao);
			CoreDao core = lapdftextDao.getCoreDao();
			
		}

	}
	
	@Override
	public long runRuleSet(FTD ftd, FTDRuleSet ftdRuleSet) throws Exception {
		FTD ftd2 = this.lapdftextDao.runRuleSetOnFtd(ftd, ftdRuleSet);
		return ftd2.getVpdmfId();
	}

	@Override
	public void uploadFtdRuleSet(byte[] data, FTDRuleSet ftdRuleSet)
			throws Exception {

		init();
		
		File workDir = new File(this.lapdftextDao.getCoreDao().getWorkingDirectory());
		
		try {
			
			CoreDao coreDao = this.lapdftextDao.getCoreDao();
			coreDao.getCe().connectToDB();
			coreDao.getCe().turnOffAutoCommit();

			File ruleDir = new File(workDir.getPath() + "/rules");
			boolean status = ruleDir.mkdirs();
			
			if( !status && !ruleDir.exists()) {
				throw new Exception("Could not create directories for Rule files. Is " 
						+ workDir + " writable?");
			}
			
			File pdfFile = new File(ruleDir.getPath() + "/" + ftdRuleSet.getFileName());
			
			// note: we always overwrite any existing files.
			FileOutputStream output = new FileOutputStream(pdfFile.getPath());
			IOUtils.write(data, output);
			
			FTDRuleSet_qo ftdQo = new FTDRuleSet_qo();
			ftdQo.setFileName(ftdRuleSet.getFileName());
			List<LightViewInstance> listFtdRules = coreDao.listInTrans(ftdQo, "FTDRuleSet");
			if( listFtdRules.size() == 0 ) {
				coreDao.insertInTrans(ftdRuleSet, "FTDRuleSet");
			} else if( listFtdRules.size() == 1 ) {
				LightViewInstance lvi = listFtdRules.get(0);
				ftdRuleSet.setVpdmfId(lvi.getVpdmfId());
				coreDao.updateInTrans(ftdRuleSet, "FTDRuleSet");
			} else {
				throw new Exception("Ambiguous FTDRuleSet: " + ftdRuleSet.getFileName());
			}
		
			coreDao.getCe().commitTransaction();
			
		} catch (Exception e) {

			e.printStackTrace();
			this.lapdftextDao.getCoreDao().getCe().rollbackTransaction();
			throw e;

		} finally {

			this.lapdftextDao.getCoreDao().getCe().closeDbConnection();

		}
	}

}
