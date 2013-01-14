package org.sagebionetworks.file.servlet;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * A simple wrapper that allows us to add a new value to the header.
 * @author John
 *
 */
public class HeaderRequestWraper extends HttpServletRequestWrapper {
	
	Map<String, String> extraHeaders = new HashMap<String, String>();

	public HeaderRequestWraper(HttpServletRequest request) {
		super(request);
	}
	
	public void addHeader(String key, String value){
		extraHeaders.put(key, value);
	}

	@Override
	public String getHeader(String name) {
		String header =  super.getHeader(name);
		if(header == null){
			header = extraHeaders.get(name);
		}
		return header;
	}	

}
