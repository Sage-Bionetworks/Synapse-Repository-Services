package org.sagebionetworks.repo.web.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

public class HSTSFilter implements Filter {
	
	public static final String MAX_AGE = "max-age=";
	public static final String STRICT_TRANSPORT_SECURITY = "Strict-Transport-Security";
	public static final long MAX_AGE_SECONDS=31536000;  //a year
	
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain filterChain) throws IOException, ServletException {
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		//PLFM-4966: add hsts header to response.  It only has an effect if connection is https, so this will have no impact in the client dev environment.
		httpResponse.setHeader(STRICT_TRANSPORT_SECURITY, MAX_AGE + MAX_AGE_SECONDS);
		filterChain.doFilter(request, response);
	}
	
	public void init(FilterConfig config) throws ServletException {
		// Nothing to do
	}

	@Override
	public void destroy() {
		// Nothing to do
	}
}
