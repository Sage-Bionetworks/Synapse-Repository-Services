package org.sagebionetworks.authutil;

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
@SuppressWarnings("unchecked")
public class ModParamHttpServletRequest extends HttpServletRequestWrapper {
	private Map<String,String[]> params;
	
	/**
	 * 
	 * @param request the request to be 'cloned'
	 * @param params the parameters for the new request
	 */
	public ModParamHttpServletRequest(HttpServletRequest request, 
			Map<String,String[]> params) {
		super(request);
		this.params=params;
	}
	
	@Override
	public String getParameter(String name) {
		if (params==null) return super.getParameter(name);
		return (params.containsKey(name) ? params.get(name)[0]: null);
	}
	
	@Override
	public Map<String,String[]> getParameterMap() {
		if (params==null) return (Map<String,String[]>)super.getParameterMap();
		return params;
	}
	
	@Override
	public Enumeration<String> getParameterNames() {
		if (params==null) return (Enumeration<String>)super.getParameterNames();
		final Iterator<String> it = params.keySet().iterator();
		return new Enumeration<String>() {
			public boolean hasMoreElements() { return it.hasNext();}
			public String nextElement() {return it.next();}
		};
	}
	
	@Override
	public String[] getParameterValues(String name) {
		if (params==null) return super.getParameterValues(name);
		return params.get(name);
	}
}
