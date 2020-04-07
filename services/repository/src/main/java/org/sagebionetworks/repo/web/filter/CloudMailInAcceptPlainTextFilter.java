package org.sagebionetworks.repo.web.filter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.http.entity.ContentType;
import org.sagebionetworks.auth.ExtraHeadersHttpServletRequest;

public class CloudMailInAcceptPlainTextFilter implements Filter {
		
	private static final Map<String,String> ACCEPT_PLAIN_TEXT_HEADER;
	
	static {
		ACCEPT_PLAIN_TEXT_HEADER = new HashMap<String,String>();
		ACCEPT_PLAIN_TEXT_HEADER.put("Accept", ContentType.TEXT_PLAIN.getMimeType());
	}
	
	public CloudMailInAcceptPlainTextFilter() {
	}
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		
		HttpServletRequest httpRequest = (HttpServletRequest)request;
		
		// To return error messages to the sender, CloudMailIn requires
		// that the response content type is text/plain.  It does not however add
		// the Accept header that Synapse requires to generate such a content-type.
		// To fill the gap we add the header here, before 
		// forwarding the CloudMailIn request down the chain.
		ExtraHeadersHttpServletRequest plainTextRequest = 
				new ExtraHeadersHttpServletRequest(httpRequest, ACCEPT_PLAIN_TEXT_HEADER);
		chain.doFilter(plainTextRequest, response);
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// Nothing to do
	}

	@Override
	public void destroy() {
		// Nothing to do
	}

}
