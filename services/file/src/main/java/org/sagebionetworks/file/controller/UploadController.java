package org.sagebionetworks.file.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.Min;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
public class UploadController {

	static private Log log = LogFactory.getLog(UploadController.class);

	@RequestMapping("/echo")
	void echo(HttpServletRequest request, HttpServletResponse response, @RequestHeader HttpHeaders headers)	throws FileUploadException, IOException {
		// the first thing we need to determine is the encoding
		StringBuilder builder = new StringBuilder();
		builder.append("CharacterEncoding = ").append(request.getCharacterEncoding()).append("\n");
		builder.append("ContentLength = ").append(request.getContentLength()).append("\n");
		builder.append("ContextPath = ").append(request.getContextPath()).append("\n");
		
		// Print the headers
		builder.append("Headers: \n");
		for(String key: headers.keySet()){
			builder.append("\t").append(key).append(" = ").append(request.getHeader(key)).append("\n");
		}
		builder.append("ContentType.type = ").append(headers.getContentType().getType()).append("\n");
		builder.append("ContentType.subType = ").append(headers.getContentType().getSubtype()).append("\n");
		builder.append("ContentType.QualityValue = ").append(headers.getContentType().getQualityValue()).append("\n");
		builder.append("ContentType.charSet = ").append(headers.getContentType().getCharSet()).append("\n");
		
		log.debug(builder.toString());
//		ServletInputStream in = request.getInputStream();
//		try{
////			long contentLength = request.getContentLength();
//			// The buffer size cannot be larger than the content length.
//			byte[] buffer = new byte[1024];
//			int read = 0;
//			while((read = in.read(buffer) ) > 0){
////				String value = new String(buffer, 0, read, "UTF-8");
////				log.debug(value);
//				log.debug(read);
//			}
//			// We do not want to read past the passed length.
//			
//		}finally{
////			in.close();
//		}

		
		// Is this a multipart/form-data
		if(ServletFileUpload.isMultipartContent(request)){
			builder.append("multipart/form-data\n");
			ServletFileUpload upload = new ServletFileUpload();
			FileItemIterator it = upload.getItemIterator(request);
			while(it.hasNext()){
				FileItemStream stream = it.next();
				builder.append("\t FieldName = ").append(stream.getFieldName()).append("\n");
				builder.append("\t ContentType = ").append(stream.getContentType()).append("\n");
				// If the content type is null try to read it
			    if (stream.isFormField()) {
					builder.append("\t Form field = ").append(stream.getName()).append(" = ").append(Streams.asString(stream.openStream())).append("\n");
			    }
				builder.append("--------\n");
			}
		}
		// setup the response.
		log.debug(builder.toString());
		response.setStatus(201);
		response.getWriter().append(builder.toString());
	}
	
	/**
	 * 
	 * @param request
	 * @param response
	 * @param headers
	 * @throws FileUploadException
	 * @throws IOException
	 */
	@RequestMapping("/single")
	void single(HttpServletRequest request, HttpServletResponse response, @RequestHeader HttpHeaders headers)	throws FileUploadException, IOException {
		
	}



}
