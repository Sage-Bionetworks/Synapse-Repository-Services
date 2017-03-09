package org.sagebionetworks.repo.web.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.sagebionetworks.cloudwatch.Consumer;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Filter to recored memory usage after each web-service request.
 *
 */
public class MemoryLoggingFilter implements Filter {
	
	@Autowired
	Consumer consumer;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		try{
			chain.doFilter(request, response);
		}finally{
			long memoryBytes = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		}
	}

	@Override
	public void destroy() {
	}

}
