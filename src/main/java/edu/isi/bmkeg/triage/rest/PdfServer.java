package edu.isi.bmkeg.triage.rest;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

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
		List<LightViewInstance> l = this.ftdDao.listArticleDocument(qFtd);
		
		if( l.size() > 1 ) {
			return new ResponseEntity<byte []>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		responseHeaders.setContentType(MediaType.valueOf("application/x-shockwave-flash"));

		Resource logoSwf = new ClassPathResource("edu/isi/bmkeg/triage/rest/00000.swf");
		byte[] logoSwfBytes = IOUtils.toByteArray( logoSwf.getInputStream() );
		ResponseEntity<byte[]> response = new ResponseEntity<byte []>
			(logoSwfBytes, responseHeaders, HttpStatus.OK);
		
		if( l.size() == 1 ) {
			vpdmfId = l.get(0).getVpdmfId();
			FTD ftd = this.ftdDao.findArticleDocumentById(vpdmfId);
//		    responseHeaders.set("Content-Disposition", "attachment; filename=\"" +  fileName + '\"');
			response = new ResponseEntity<byte []> (ftd.getLaswf(), responseHeaders, HttpStatus.OK);
    	} 
				
        return response;
		
	}

}
