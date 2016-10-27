package org.sagebionetworks.repo.web.filter;

import java.io.IOException;
import java.net.URL;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HttpToHttpsRedirectFilter implements Filter {

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// TODO Auto-generated method stub

	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		
		HttpServletRequest req = (HttpServletRequest)request;
		HttpServletResponse resp = (HttpServletResponse)response;
		
		// If X-Forwarded-Proto header, then redirect else go through 
		String protocol = req.getHeader("X-Forwarded-Proto");
		if (null != protocol && protocol.equals("http")) {
			URL httpURL = new URL(req.getRequestURL().toString());
			URL httpsURL = new URL("https", httpURL.getHost(), httpURL.getPort(), httpURL.getFile());
			resp.sendRedirect(httpsURL.toString());
		} else {
			chain.doFilter(request, response);
		}
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}
	
}
