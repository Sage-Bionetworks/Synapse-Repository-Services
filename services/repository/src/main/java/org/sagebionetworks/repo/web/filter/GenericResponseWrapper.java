package org.sagebionetworks.repo.web.filter;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * 
 * Note: This class was provided by:  <a href="http://jpgmr.wordpress.com/2010/07/28/tutorial-implementing-a-servlet-filter-for-jsonp-callback-with-springs-delegatingfilterproxy/#1">Tutorial: Implementing a Servlet Filter for JSONP callback with Spring's DelegatingFilterProxy</a>
 *
 */
public class GenericResponseWrapper extends HttpServletResponseWrapper {

	private ByteArrayOutputStream output;
	private int contentLength;
	private String contentType;

	public GenericResponseWrapper(HttpServletResponse response) {
		super(response);

		output = new ByteArrayOutputStream();
	}

	public byte[] getData() {
		if (writer!=null) writer.flush();
		return output.toByteArray();
	}

	public ServletOutputStream getOutputStream() {
		return new FilterServletOutputStream(output);
	}

	private PrintWriter writer = null;
	
	public PrintWriter getWriter() {
		if (writer!=null) return writer;
		try {
			String charsetName = getCharacterEncoding();
			if (charsetName==null) charsetName= "ISO-8859-1";
			writer = new PrintWriter(new OutputStreamWriter(output, charsetName), true);
			return writer;
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}	

	public void setContentLength(int length) {
		this.contentLength = length;
		super.setContentLength(length);
	}

	public int getContentLength() {
		return contentLength;
	}

	public void setContentType(String type) {
		this.contentType = type;
		super.setContentType(type);
	}

	public String getContentType() {
		return contentType;
	}
}