package org.sagebionetworks.auth;

import java.util.Enumeration;
import java.util.Map;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * Creates a copy of an HttpServlet request with extra header(s)
 * 
 * @author bhoff
 *
 */
public class ExtraHeadersHttpServletRequest extends HttpServletRequestWrapper {
	private Map<String,String> extraHeaders;
	
	/**
	 * 
	 * @param request the request to be 'cloned'
	 * @param extraHeaders the additional headers
	 */
	public ExtraHeadersHttpServletRequest(HttpServletRequest request, 
			Map<String,String> extraHeaders) {
		super(request);
		if (extraHeaders==null) throw new IllegalArgumentException("extraHeaders may not be null");
		this.extraHeaders=extraHeaders;
	}
	
	@Override
    public String getHeader(String name) {
		String result = super.getHeader(name);
		if (result==null) result = extraHeaders.get(name);
		return result;
	}
	
	@Override
	public Enumeration<String> getHeaderNames() {
		Vector<String> v = new Vector<String>();
		Enumeration<String> en = super.getHeaderNames();
		while (en.hasMoreElements()) v.add(en.nextElement());
		for (String key : extraHeaders.keySet()) v.add(extraHeaders.get(key));
		return v.elements();
    }
    
	@Override
	public int getIntHeader(String name) {
		int result = super.getIntHeader(name);
		if (result==-1) {
			String headerString = extraHeaders.get(name);
			if (headerString!=null) result = Integer.parseInt(headerString);
		}
		return result;
   }
}
