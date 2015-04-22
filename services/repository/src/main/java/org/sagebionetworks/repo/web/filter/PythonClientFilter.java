package org.sagebionetworks.repo.web.filter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Per PLFM-2903  This filter ensures that the Content-Type field of the HTTP response 
 * for requests issued by affected Python clients does not include the 'charset' part.
 * @author brucehoff
 *
 */
public class PythonClientFilter implements Filter {

	private static Logger log = LogManager.getLogger(PythonClientFilter.class);

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// intentionally left blank
	}
	
	private static final String PYTHON_REQUEST_STRING = "python-requests";
	private static final String SYNAPSE_CLIENT_STRING = "synapseclient";
	private static final Pattern version_pattern = Pattern.compile("(\\d+(?:\\.\\d+){0,2})(?:\\.dev(\\d+))?", Pattern.CASE_INSENSITIVE);
	static final int DEV = -3000;

	
	/**
	 * Convert a string of the form "1.2.3" to an integer array, e.g. {1,2,3,0,0}.
	 * Also supports an optional "dev" tag, for example "1.2.3.dev4" yields {1,2,3,-3000,4}
	 * @param s
	 * @return
	 */
	public static int[] versionToTuple(String s) {
		int[] ans = new int[5];  // initialized to all zeros

		Matcher m = version_pattern.matcher(s);
		if (m.matches()) {
			String[] fields = m.group(1).split("\\.");
			for (int i=0; i<fields.length; i++) {
				ans[i] = Integer.parseInt(fields[i]);
			}
			if (m.group(2)!=null) {
				ans[3] = DEV;
				ans[4] = Integer.parseInt(m.group(2));
			}
		} else {
			throw new IllegalArgumentException("Expected version string but found "+s);
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
		try {
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
			}
		} catch (IllegalArgumentException exception) {
			// if we can't parse the version string, we assume it's not an affected version of the python client
			log.warn("PythonClientFilter unable to parse user agent string \""+userAgent+"\".", exception);
		}
		return false;
	}
	
	private static final String HTTP_1_1_DEFAULT_CHARACTER_ENCODING = Charset.forName("ISO-8859-1").name();

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		HttpServletRequest httpRequest = ((HttpServletRequest)request);
		HttpServletResponse httpResponse = ((HttpServletResponse)response);
		String header = httpRequest.getHeader("User-Agent");
		if (isAffectedPythonClient(header)) {
			PythonClientResponseWrapper wrapper = new PythonClientResponseWrapper(httpResponse);
			// Pass it along.
			chain.doFilter(request, wrapper);
			
			// now translate the character set and adjust the header accordingly 
			String responseContentTypeString = wrapper.getContentType();
			ContentType responseContentType = responseContentTypeString==null?null:ContentType.parse(responseContentTypeString);
			String responseCharacterEncoding = wrapper.getCharacterEncoding();
			// ensure that the content-type omits the character set
			if (responseContentType!=null) {
				// it implicitly now specifies ISO-8859-1
				String charsetFreeContentType = responseContentType.getMimeType();
				httpResponse.setContentType(charsetFreeContentType);
			}
			// ensure the body is written in ISO-8859-1
			OutputStream out = httpResponse.getOutputStream();
			PrintWriter pw = wrapper.getWriter();
			pw.flush();
			pw.close();
			try {
				if (responseCharacterEncoding!=null && !responseCharacterEncoding.equals(HTTP_1_1_DEFAULT_CHARACTER_ENCODING)) {
					// we have to change the character encoding to ISO-8859-1
					String responseContentAsString = new String(wrapper.getData(), responseCharacterEncoding);
					out.write(responseContentAsString.getBytes(HTTP_1_1_DEFAULT_CHARACTER_ENCODING));
				} else {
					// no need to change the encoding, just write out the body of the response
					out.write(wrapper.getData());
				}
			} finally {
				out.close();
			}
		} else { // request is not from affected Python client
			chain.doFilter(request, response);
		}
	}

	@Override
	public void destroy() {
		// intentionally left blank
	}

}
