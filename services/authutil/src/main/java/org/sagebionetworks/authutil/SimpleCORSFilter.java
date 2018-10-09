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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.AuthorizationConstants;

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

	public static final String OPTIONS = "OPTIONS";
	public static final String ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";
	public static final String ALL_ORIGINS = "*";
	public static final String METHODS = "POST, GET, PUT, DELETE";
	public static final String HEADERS = "Origin, X-Requested-With, Content-Type, Accept, " + AuthorizationConstants.SESSION_TOKEN_PARAM;
	public static final String MAX_AGE = "600";
	public static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
	public static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
	public static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
	public static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";
	public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
	static private Logger log = LogManager.getLogger(SimpleCORSFilter.class);
	
	@Override
	public void destroy() {
		
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res,
			FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		// Add this header to the response of every call.
		response.addHeader(ACCESS_CONTROL_ALLOW_ORIGIN, ALL_ORIGINS);
		// Is this a pre-flight request?
		if(isPreFlightRequest(request)){
			// header indicates how long the results of a preflight request can be cached in seconds
			response.addHeader(ACCESS_CONTROL_MAX_AGE, MAX_AGE);
			// header indicates which custom header field names can be used during the actual request.
			response.addHeader(ACCESS_CONTROL_ALLOW_HEADERS, HEADERS);
			// header indicates the methods that can be used in the actual request.
			response.addHeader(ACCESS_CONTROL_ALLOW_METHODS, METHODS);
			// header indicates that the actual request can include user credentials (send cookies from another domain).
			response.addHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, Boolean.TRUE.toString());
			// We do not pass along the pre-flight requests, we just return with the header.
			log.info("Pre-flight request headers: ");
			logHeaders(request);
		} else {
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
		return request.getHeader(ACCESS_CONTROL_REQUEST_METHOD) != null && 
				request.getMethod() != null &&
				request.getMethod().equals(OPTIONS);
	}

}
