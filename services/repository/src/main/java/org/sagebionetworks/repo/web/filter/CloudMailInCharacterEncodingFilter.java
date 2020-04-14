package org.sagebionetworks.repo.web.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * CloudMailIn does not specify the UTF-8 encoding but may still include UTF-8 characters in its JSON.
 * We manually override the character encoding to UTF-8 so that Spring can properly handle those characters.
 */
public class CloudMailInCharacterEncodingFilter implements Filter {
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		//nothing to do
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		chain.doFilter(new HttpServletRequestWrapper((HttpServletRequest) request) {
			@Override
			public String getCharacterEncoding() {
				return "utf-8";
			}
		}, response);
	}

	@Override
	public void destroy() {
		//nothing to do
	}
}
