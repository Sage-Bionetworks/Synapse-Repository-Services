package org.sagebionetworks.authutil;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.apache.commons.lang3.StringUtils;

/**
 * Creates a copy of an HttpServlet request with a new set of headers and/or parameters
 * 
 * @author bhoff
 *
 */
public class ModHttpServletRequest extends HttpServletRequestWrapper {
	private CaseInsensitiveMap<String,String[]> headers;
	private CaseInsensitiveMap<String,String[]> params;
	
	/**
	 * 
	 * @param request the request to be 'cloned'
	 * @param headers the headers for the new request, or null to use the existing ones
	 * @param params the parameters for the new request, or null to use the existing ones
	 */
	public ModHttpServletRequest(HttpServletRequest request, 
			Map<String,String[]> headers,
			Map<String,String[]> params
			) {
		super(request);
		this.headers=headers==null?null:new CaseInsensitiveMap<String,String[]>(headers);
		this.params=params==null?null:new CaseInsensitiveMap<String,String[]>(params);
	}
	
	private static String getFirstValue(String[] values) {
		if (values==null || values.length<1) return null;
		return values[0];
	}
	
    /**
     * The default behavior of this method is to return getDateHeader(String name)
     * on the wrapped request object.
     */
	@Override
	public long getDateHeader(String name) {
		if (headers==null) return super.getDateHeader(name);
		String value = getFirstValue(this.headers.get(name));
		if (StringUtils.isEmpty(value)) return -1L;
		return Long.parseLong(value);
	}
        	
    /**
     * The default behavior of this method is to return getHeader(String name)
     * on the wrapped request object.
     */
	@Override
    public String getHeader(String name) {
		if (headers==null) return super.getHeader(name);
		return getFirstValue(this.headers.get(name));
    }
    
    /**
     * The default behavior of this method is to return getHeaders(String name)
     * on the wrapped request object.
     */
	@Override
    public Enumeration<String> getHeaders(String name) {
		if (headers==null) return super.getHeaders(name);
		String[] values = this.headers.get(name);
		if (values==null) return Collections.emptyEnumeration();
		return Collections.enumeration(Arrays.asList(values));
    }  

    /**
     * The default behavior of this method is to return getHeaderNames()
     * on the wrapped request object.
     */
	@Override
    public Enumeration<String> getHeaderNames() {
		if (headers==null) return super.getHeaderNames();
	    return Collections.enumeration(this.headers.keySet());
    }
    
    /**
     * The default behavior of this method is to return
     * getIntHeader(String name) on the wrapped request object.
     */
	@Override
     public int getIntHeader(String name) {
 		if (headers==null) return super.getIntHeader(name);
 		String value = getFirstValue(this.headers.get(name));
 		if (StringUtils.isEmpty(value)) return -1;
 		return Integer.parseInt(value);
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
	    return Collections.enumeration(params.keySet());
	}
	
	@Override
	public String[] getParameterValues(String name) {
		if (params==null) return super.getParameterValues(name);
		return params.get(name);
	}
}
