package edu.isi.bmkeg.digitalLibrary.rest;

import java.io.File;
import java.io.FileReader;
import java.io.StringWriter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.FileUtils;
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

import antlr.StringUtils;

import edu.isi.bmkeg.digitalLibrary.model.qo.citations.LiteratureCitation_qo;
import edu.isi.bmkeg.ftd.dao.FtdDao;
import edu.isi.bmkeg.ftd.model.FTD;
import edu.isi.bmkeg.ftd.model.qo.FTD_qo;
import edu.isi.bmkeg.lapdf.controller.LapdfEngine;
import edu.isi.bmkeg.lapdf.pmcXml.PmcXmlArticle;
import edu.isi.bmkeg.lapdf.xml.model.LapdftextXMLDocument;
import edu.isi.bmkeg.utils.Converters;
import edu.isi.bmkeg.utils.TextUtils;
import edu.isi.bmkeg.utils.xml.XmlBindingTools;
import edu.isi.bmkeg.vpdmf.model.instances.LightViewInstance;

//@Controller
public class PdfServer {

/*	private static final Logger logger = Logger.getLogger(PdfServer.class);

	@Autowired
	private FtdDao ftdDao;

	public void setftdDao(FtdDao ftdDao) {
		this.ftdDao = ftdDao;
	}
	
	@RequestMapping(value="/load", 
			method=RequestMethod.GET, 
			params="swfFile")
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
		LiteratureCitation_qo lc = new LiteratureCitation_qo();
		qFtd.setCitation(lc);
		lc.setVpdmfId(String.valueOf(vpdmfId));
		List<LightViewInstance> l = this.ftdDao.listArticleDocument(qFtd);
		
		if( l.size() > 1 ) {
			return new ResponseEntity<byte []>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		responseHeaders.setContentType(MediaType.valueOf("application/x-shockwave-flash"));

		Resource logoSwf = new ClassPathResource("edu/isi/bmkeg/digitalLibrary/rest/00000.swf");
		byte[] logoSwfBytes = IOUtils.toByteArray( logoSwf.getInputStream() );
		ResponseEntity<byte[]> response = new ResponseEntity<byte []>
			(logoSwfBytes, responseHeaders, HttpStatus.OK);
		
		if( l.size() == 1 ) {
			vpdmfId = l.get(0).getVpdmfId();
			FTD ftd = this.ftdDao.findArticleDocumentById(vpdmfId);

			String wd = this.ftdDao.getCoreDao().getWorkingDirectory();
			File laSwfFile = new File( wd + "/pdfs/" + ftd.getLaswfFile() );
			
			if( !laSwfFile.exists() ) {
				return new ResponseEntity<byte []>(HttpStatus.NOT_FOUND);
			}

			byte [] laSwf = Converters.fileContentsToBytesArray(laSwfFile);

//		    responseHeaders.set("Content-Disposition", "attachment; filename=\"" +  fileName + '\"');
			response = new ResponseEntity<byte []> (laSwf, responseHeaders, HttpStatus.OK);
    	} 
				
        return response;
		
	}
	
	@RequestMapping(value="/load", 
			method=RequestMethod.GET, 
			params="xmlFile")
	public ResponseEntity<String> byXmlParameter(@RequestParam("xmlFile") String fileName) throws Exception {
				
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.valueOf("application/xml"));
		
		Long vpdmfId = null;

		Pattern patt = Pattern.compile("(\\d+)\\.xml");
		Matcher m = patt.matcher(fileName);
		
		if( m.find() ) {
			vpdmfId = new Long(m.group(1));
		} else {
			return new ResponseEntity<String>(HttpStatus.BAD_REQUEST);
		}
				
		FTD_qo qFtd = new FTD_qo();
		LiteratureCitation_qo lc = new LiteratureCitation_qo();
		qFtd.setCitation(lc);
		lc.setVpdmfId(String.valueOf(vpdmfId));
		List<LightViewInstance> l = this.ftdDao.listArticleDocument(qFtd);
		
		if( l.size() > 1 ) {
			return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		LapdftextXMLDocument xmlDoc = new LapdftextXMLDocument();
		StringWriter writer = new StringWriter();
		XmlBindingTools.generateXML(xmlDoc, writer);				
		ResponseEntity<String> response = new ResponseEntity<String>(
				writer.toString(), responseHeaders, HttpStatus.OK
				);
						
		if( l.size() == 1 ) {
			LapdfEngine eng = new LapdfEngine();
			vpdmfId = l.get(0).getVpdmfId();
			FTD ftd = this.ftdDao.findArticleDocumentById(vpdmfId);
			
			String wd = this.ftdDao.getCoreDao().getWorkingDirectory();
			File xmlFile = new File( wd + "/pdfs/" + ftd.getXmlFile() );
			
			if( !xmlFile.exists() ) {
				return new ResponseEntity<String>(HttpStatus.NOT_FOUND);
			}

			String xml = FileUtils.readFileToString(xmlFile, "UTF-8");
			response = new ResponseEntity<String> (xml, responseHeaders, 
					HttpStatus.OK);
			
    	} 
				
        return response;
		
	}

	@RequestMapping(value="/load", 
			method=RequestMethod.GET, 
			params="pmcXml")
	public ResponseEntity<String> byPmcXmlParameter(@RequestParam("pmcXml") String fileName) throws Exception {
				
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.valueOf("application/xml"));
		
		Long vpdmfId = null;

		Pattern patt = Pattern.compile("(\\d+)_pmc\\.xml");
		Matcher m = patt.matcher(fileName);
		
		if( m.find() ) {
			vpdmfId = new Long(m.group(1));
		} else {
			return new ResponseEntity<String>(HttpStatus.BAD_REQUEST);
		}
				
		FTD_qo qFtd = new FTD_qo();
		LiteratureCitation_qo lc = new LiteratureCitation_qo();
		qFtd.setCitation(lc);
		lc.setVpdmfId(String.valueOf(vpdmfId));
		List<LightViewInstance> l = this.ftdDao.listArticleDocument(qFtd);
		
		if( l.size() > 1 ) {
			return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		LapdftextXMLDocument xmlDoc = new LapdftextXMLDocument();
		StringWriter writer = new StringWriter();
		XmlBindingTools.generateXML(xmlDoc, writer);				
		ResponseEntity<String> response = new ResponseEntity<String>(
				writer.toString(), responseHeaders, HttpStatus.OK
				);
				
		if( l.size() == 1 ) {
			LapdfEngine eng = new LapdfEngine();
			vpdmfId = l.get(0).getVpdmfId();
			FTD ftd = this.ftdDao.findArticleDocumentById(vpdmfId);
			
			String wd = this.ftdDao.getCoreDao().getWorkingDirectory();
			File pmcXmlFile = new File( wd + "/pdfs/" +  ftd.getPmcXmlFile() );
			
			if( !pmcXmlFile.exists() ) {
				return new ResponseEntity<String>(HttpStatus.NOT_FOUND);
			}

			String pmcXml = FileUtils.readFileToString(pmcXmlFile);
			response = new ResponseEntity<String> (pmcXml, responseHeaders, 
					HttpStatus.OK);

		} 
				
        return response;
		
	}
	
	@RequestMapping(value="/load", 
			method=RequestMethod.GET, 
			params="html")
	public ResponseEntity<String> byHtmlParameter(@RequestParam("html") String fileName) throws Exception {
				
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.valueOf("application/html"));
		
		Long vpdmfId = null;

		Pattern patt = Pattern.compile("(\\d+)\\.html");
		Matcher m = patt.matcher(fileName);
		
		if( m.find() ) {
			vpdmfId = new Long(m.group(1));
		} else {
			return new ResponseEntity<String>(HttpStatus.BAD_REQUEST);
		}
				
		FTD_qo qFtd = new FTD_qo();
		LiteratureCitation_qo lc = new LiteratureCitation_qo();
		qFtd.setCitation(lc);
		lc.setVpdmfId(String.valueOf(vpdmfId));
		List<LightViewInstance> l = this.ftdDao.listArticleDocument(qFtd);
		
		if( l.size() > 1 ) {
			return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		PmcXmlArticle xmlDoc = new PmcXmlArticle();
		StringWriter writer = new StringWriter();
		XmlBindingTools.generateXML(xmlDoc, writer);				
		ResponseEntity<String> response = new ResponseEntity<String>(
				writer.toString(), responseHeaders, HttpStatus.OK
				);
		
		
		if( l.size() == 1 ) {

			vpdmfId = l.get(0).getVpdmfId();
			FTD ftd = this.ftdDao.findArticleDocumentById(vpdmfId);

			String wd = this.ftdDao.getCoreDao().getWorkingDirectory();
			File pmcXmlFile = new File( wd + "/pdfs/" +  ftd.getPmcXmlFile() );
			
			if( !pmcXmlFile.exists() ) {
				return new ResponseEntity<String>(HttpStatus.NOT_FOUND);
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
			String html = outputWriter.toString();  
			
			response = new ResponseEntity<String> (
					html, responseHeaders, HttpStatus.OK
					);
    	
		} 
				
        return response;
		
	}*/

}
