package org.sagebionetworks.repo.web.filter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class SessionIdCookieFilter implements Filter {
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		///do nothing
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		for(Cookie cookie : httpRequest.getCookies()){

		}
	}

	@Override
	public void destroy() {
		//do nothing
	}
}
