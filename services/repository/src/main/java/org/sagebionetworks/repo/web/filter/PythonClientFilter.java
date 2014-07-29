package org.sagebionetworks.repo.web.filter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.entity.ContentType;

/**
 * Per PLFM-2903  This filter ensures that the Content-Type field of the HTTP response 
 * for requests issued by affected Python clients does not include the 'charset' part.
 * @author brucehoff
 *
 */
public class PythonClientFilter implements Filter {

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// intentionally left blank
	}
	
	private static final String PYTHON_REQUEST_STRING = "python-request";
	private static final String SYNAPSE_CLIENT_STRING = "synapseclient";
	
	/**
	 * convert a string of the form "1.2.3" to an integer array, e.g. {1,2,3}
	 * @param s
	 * @return
	 */
	public static int[] versionToTuple(String s) {
		String[] fields = s.split("\\.");
		if (fields.length<1 || fields.length>3) throw new IllegalArgumentException("Expected version string but found "+s);
		int[] ans = new int[3];
		for (int i=0; i<ans.length; i++) {
			if (i>=fields.length) {
				ans[i]=0;
			} else {
				ans[i] = Integer.parseInt(fields[i]);
			}
		}
		return ans;
	}
	
	/**
	 * compare two version string, both having the format x.y.z
	 * @param v1
	 * @param v2
	 * @return
	 */
	public static int compareVersions(String v1, String v2) {
		int[] t1 = versionToTuple(v1);
		int[] t2 = versionToTuple(v2);
		if (t1.length!=t2.length) throw new IllegalArgumentException("t1.length="+t1.length+" but t2.length="+t2.length);
		for (int i=0; i<t1.length; i++) {
			if (t1[i]<t2[i]) {
				return -1;
			} else if (t1[i]>t2[i]) {
				return 1;
			}
			// else continue on to the next field
		}
		// all the fields are the same
		return 0;
	}
	
	public static boolean isAffectedPythonClient(String userAgent) {
		if (userAgent==null) return false;
		if (userAgent.indexOf(PYTHON_REQUEST_STRING)>=0) {
			// if there's 'python-request' in the string and no 'synapseclient' then it's python client <0.5 and affected
			int scIndex = userAgent.indexOf(SYNAPSE_CLIENT_STRING);
			if (scIndex<0) return true;
			// if there's a 'python-request' and there IS a synapseclient entry and it's <=1.0.1 (latest affected release) then it's affected
			String foo = userAgent.substring(scIndex+SYNAPSE_CLIENT_STRING.length()+1);
			int scEnd = foo.indexOf(" ");
			String version;
			if (scEnd<0) {
				version = foo;
			} else {
				version = foo.substring(0, scEnd);
			}
			return(compareVersions(version, "1.0.1")<1);
		} else {
			return false;
		}
	}
	
	private static final Charset HTTP_1_1_DEFAULT_CHARACTER_ENCODING = Charset.forName("ISO-8859-1");

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		HttpServletRequest httpRequest = ((HttpServletRequest)request);
		HttpServletResponse httpResponse = ((HttpServletResponse)response);
		String header = httpRequest.getHeader("User-Agent");
		if (isAffectedPythonClient(header)) {
			OutputStream out = httpResponse.getOutputStream();
			GenericResponseWrapper wrapper = new GenericResponseWrapper(httpResponse);
			// Pass it along.
			chain.doFilter(request, wrapper);

			// now translate the character set and adjust the header accordingly 
			String responseContentTypeString = wrapper.getContentType();
			ContentType responseContentType = responseContentTypeString==null?null:ContentType.parse(responseContentTypeString);
			Charset responseCharacterEncoding = responseContentType==null?null:responseContentType.getCharset();
			if (responseCharacterEncoding!=null && !responseCharacterEncoding.equals(HTTP_1_1_DEFAULT_CHARACTER_ENCODING)) {
				// we have to change the character encoding to ISO-8895-1
				String responseContentAsString = new String(wrapper.getData(), responseCharacterEncoding);
				try {
					out.write(responseContentAsString.getBytes(HTTP_1_1_DEFAULT_CHARACTER_ENCODING));
				} finally {
					out.close();
				}
				// we change the response header to specify the mime type but not the character set
				// it implicitly now specifies ISO-8895-1
				httpResponse.setContentType(responseContentType.getMimeType());
			} else {
				// no need to change the encoding, just write out the body of the response
				try {
					out.write(wrapper.getData());
				} finally {
					out.close();
				}
				
			}
		} else {
			chain.doFilter(request, response);
		}
	}

	@Override
	public void destroy() {
		// intentionally left blank
	}

}
