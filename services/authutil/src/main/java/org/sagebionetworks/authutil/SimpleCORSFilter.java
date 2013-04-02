package org.sagebionetworks.authutil;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is a very simple filter to allow Cross-Origin Resource Sharing (CORS).
 * This type of filter is required to allow direct javascript calls.
 * 
 * See: http://enable-cors.org/index.html
 * 
 * @author John
 *
 */
public class SimpleCORSFilter implements Filter {

	static private Log log = LogFactory.getLog(SimpleCORSFilter.class);
	
	@Override
	public void destroy() {
		
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res,
			FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		// Add this header to the response of every call.
		response.addHeader("Access-Control-Allow-Origin", "*");
		// Is this a pre-flight request?
		if(isPreFlightRequest(request)){
			// header indicates how long the results of a preflight request can be cached in seconds
			response.addHeader("Access-Control-Max-Age", "300");
			// We do not pass along the pre-flight requests, we just return with the header
			log.info("Pre-flight request headers: ");
			logHeaders(request);
		}else{
			// pass along all non-pre-flight requests.
			chain.doFilter(request, response);
		}
	}

	/**
	 * Write the headers to the log
	 * @param request
	 */
	private void logHeaders(HttpServletRequest request) {
		Enumeration headers = request.getHeaderNames();
		while(headers.hasMoreElements()){
			String key = (String) headers.nextElement();
			String value = request.getHeader(key);
			log.info("\t "+key+" = "+value);
		}
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {

	}
	
	/**
	 * Is this a pre-flight request.
	 * @param request
	 * @return
	 */
	public boolean isPreFlightRequest(HttpServletRequest request){
		return request.getHeader("Access-Control-Request-Method") != null && 
				request.getMethod() != null &&
				request.getMethod().equals("OPTIONS");
	}

}
