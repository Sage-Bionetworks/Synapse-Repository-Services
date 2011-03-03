package org.sagebionetworks.repo.web;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * Creates a copy of an HttpServelt request with a new set of parameters
 * 
 * @author bhoff
 *
 */
public class ModParamHttpServletRequest extends HttpServletRequestWrapper {
	private Map<String,String[]> params;
	
	/**
	 * 
	 * @param request the request to be 'cloned'
	 * @param params the parameters for the new request
	 */
	ModParamHttpServletRequest(HttpServletRequest request, Map<String,String[]> params) {
		super(request);
	}
	public String getParameter(String name) {return (params.containsKey(name) ? params.get(name)[0]: null);}
	public Map<String,String[]> getParameterMap() {return params;}
	public Enumeration<String> getParameterNames() {
		final Iterator<String> it = params.keySet().iterator();
		return new Enumeration<String>() {
			public boolean hasMoreElements() { return it.hasNext();}
			public String nextElement() {return it.next();}
		};
	}
	public String[] getParameterValues(String name) {return params.get(name);}
}
