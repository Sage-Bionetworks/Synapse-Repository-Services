package org.sagebionetworks.repo.web.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.audit.Method;
import org.sagebionetworks.repo.model.audit.RequestMetadata;

/**
 * A very simple filter that captures basic information about all requests.
 * 
 * @author John
 *
 */
public class AuditFilter implements Filter {
	
	private static long NANOSECONDS_PER_MILLISECOND = 1000000;

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		StatusExposingServletResponse httpResponse = new StatusExposingServletResponse((HttpServletResponse) response);
		// Get the userId from the parameters
		String userIdString = httpRequest.getParameter(AuthorizationConstants.USER_ID_PARAM);
		if(userIdString == null) throw new IllegalArgumentException("This filter must be after the Authentication Filter");
		long userId = Long.parseLong(userIdString);
		RequestMetadata data = new RequestMetadata();
		data.setUserId(userId);
		data.setTimestamp(System.currentTimeMillis());
		data.setRequestURL(httpRequest.getRequestURL().toString());
		data.setMethod(Method.valueOf(httpRequest.getMethod())); 		
		// Gather the basic data
		long start = System.nanoTime();
		// pass it along
		chain.doFilter(httpRequest, httpResponse);
		long elapse = System.nanoTime()-start;
		// convert to ms
		elapse = elapse/NANOSECONDS_PER_MILLISECOND;
		data.setElapseMS(elapse);
		data.setResponseCode((long) httpResponse.getStatus());
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		// TODO Auto-generated method stub

	}

}
