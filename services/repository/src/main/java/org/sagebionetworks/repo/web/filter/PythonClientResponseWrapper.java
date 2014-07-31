package org.sagebionetworks.repo.web.filter;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.entity.ContentType;

/**
 * This wrapper is necessary to counteract the 'stickiness' of the wrapped
 * class in maintaining an explicit charset field in the response's content type.
 * We avoid letting the 'downstream' servlet set the encoding, lest we be unable to 
 * 'blank it out', as required by the Python client.
 * 
 * @author brucehoff
 *
 */
public class PythonClientResponseWrapper extends GenericResponseWrapper {
	private String characterEncoding;
	private String contentType;

	public PythonClientResponseWrapper(HttpServletResponse response) {
		super(response);
	}
	
	private void setContentTypeAndEncodingFromContentTypeString(String value) {
		this.contentType = value;
		ContentType contentType = ContentType.parse(value);
		Charset charset = contentType.getCharset();
		setCharacterEncoding(charset==null?null:charset.name());
	}

	
	public void setContentType(String contentType) {
		setContentTypeAndEncodingFromContentTypeString(contentType);
	}
	
	public String getContentType() {
		return contentType;
	}
	
	public void setCharacterEncoding(String characterEncoding) {
		this.characterEncoding = characterEncoding;
	}
	
	public String getCharacterEncoding() {
		// TODO what happens if the specified character encoding conflicts with the specified content type?
		return characterEncoding;
	}
	
	public void setHeader(String name, String value) {
		if (name.equalsIgnoreCase("Content-Type")) {
			setContentTypeAndEncodingFromContentTypeString(value);
		} else {
			super.setHeader(name, value);
		}
	}
	
	public void addHeader(String name, String value) {
		if (name.equalsIgnoreCase("Content-Type")) {
			setContentTypeAndEncodingFromContentTypeString(value);
		} else {
			super.setHeader(name, value);
		}
	}
	
	public PrintWriter getWriter() {
		try {
			String charsetName = getCharacterEncoding();
			if (charsetName==null) charsetName= "ISO-8859-1";
			return new PrintWriter(new OutputStreamWriter(getOutputStream(), charsetName));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

}
