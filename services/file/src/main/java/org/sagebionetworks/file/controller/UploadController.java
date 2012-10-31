package org.sagebionetworks.file.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
public class UploadController {
	
	static private Log log = LogFactory.getLog(UploadController.class);
	
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = { "test" }, method = RequestMethod.POST)
	public @ResponseBody
	String testPost(@RequestHeader HttpHeaders header,
			@RequestParam(value = "sessionToken", required = false) String sessionToken,
			HttpServletRequest request) throws FileUploadException, IOException  {
		printHeader(header);
		Map parameters = request.getParameterMap();
		log.debug("parameters:"+parameters);
		ServletFileUpload upload = new ServletFileUpload();
		FileItemIterator iterator = upload.getItemIterator(request);
		log.debug("Iterator hasNext = "+iterator.hasNext());
		while(iterator.hasNext()){
			FileItemStream stream = iterator.next();
			log.debug("fieldname: "+stream.getFieldName());
			log.debug("name: "+stream.getName());
		}
		return "put";
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { "test" }, method = RequestMethod.GET)
	public @ResponseBody
	String testGet(@RequestHeader HttpHeaders header, HttpServletRequest request) throws FileUploadException, IOException  {
		// print the headers for this file.
		printHeader(header);
		return "get";
	}

	/**
	 * @param header
	 */
	public void printHeader(HttpHeaders header) {
		// Log the headers
		for(String key: header.keySet()){
			List<String> values = header.get(key);
			log.debug("header key: "+key);
			for(String value: values){
				log.debug("\t"+value);
			}
		}
	}

}
