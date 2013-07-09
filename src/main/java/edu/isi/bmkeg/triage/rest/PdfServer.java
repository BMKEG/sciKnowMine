package edu.isi.bmkeg.triage.rest;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.isi.bmkeg.digitalLibrary.model.citations.LiteratureCitation;
import edu.isi.bmkeg.digitalLibrary.model.qo.citations.LiteratureCitation_qo;
import edu.isi.bmkeg.ftd.dao.FtdDao;
import edu.isi.bmkeg.ftd.model.FTD;
import edu.isi.bmkeg.ftd.model.qo.FTD_qo;
import edu.isi.bmkeg.vpdmf.model.instances.LightViewInstance;

@Controller
public class PdfServer {

	private static final Logger logger = Logger.getLogger(PdfServer.class);

	@Autowired
	private FtdDao ftdDao;

	public void setftdDao(FtdDao ftdDao) {
		this.ftdDao = ftdDao;
	}
	
	@RequestMapping(value="/load", method=RequestMethod.GET, params="swfFile")
	public ResponseEntity<byte []> byPdfParameter(@RequestParam("swfFile") String fileName) throws Exception {
		
		HttpHeaders responseHeaders = new HttpHeaders();
		
		Long vpdmfId = null;

		Pattern patt = Pattern.compile("(\\d+)\\.swf");
		Matcher m = patt.matcher(fileName);
		
		if( m.find() ) {
			vpdmfId = new Long(m.group(1));
		} else {
			return new ResponseEntity<byte []>(HttpStatus.BAD_REQUEST);
		}
				
		FTD_qo qFtd = new FTD_qo();
		LiteratureCitation_qo ac = new LiteratureCitation_qo();
		qFtd.setCitation(ac);
		ac.setVpdmfId(String.valueOf(vpdmfId));
		List<LightViewInstance> l = this.ftdDao.listFTD(qFtd);
		
		if( l.size() == 0 ) {
			return new ResponseEntity<byte []>(HttpStatus.NOT_FOUND);
		}

		if( l.size() > 1 ) {
			return new ResponseEntity<byte []>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		vpdmfId = l.get(0).getVpdmfId();

		FTD ftd = this.ftdDao.findArticleDocumentById(vpdmfId);

		responseHeaders.setContentType(MediaType.valueOf("application/x-shockwave-flash"));
//	    responseHeaders.set("Content-Disposition", "attachment; filename=\"" +  fileName + '\"');

		ResponseEntity<byte[]> response = new ResponseEntity<byte []>
        		(ftd.getLaswf(), responseHeaders, HttpStatus.OK);
        		
        return response;
		
	}

}
