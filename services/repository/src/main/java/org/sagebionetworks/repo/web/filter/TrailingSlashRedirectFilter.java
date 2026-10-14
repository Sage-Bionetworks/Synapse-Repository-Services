package org.sagebionetworks.repo.web.filter;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet filter that normalizes URLs by removing trailing slashes and whitespace.
 * After upgrading to Spring 6.2, the setUseTrailingSlashMatch() method was removed,
 * so we handle trailing slashes at the filter level by wrapping the request and
 * rewriting the URI transparently. This maintains backward compatibility with existing
 * clients that send requests with trailing slashes.
 */
public class TrailingSlashRedirectFilter implements Filter {

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// No initialization needed
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse resp = (HttpServletResponse) response;

		String requestURI = req.getRequestURI();

		// Root path "/" should pass through unchanged
		if ("/".equals(requestURI)) {
			chain.doFilter(request, response);
			return;
		}

		// Remove trailing slashes and whitespace
		String normalizedURI = requestURI.replaceAll("[/\\s]+$", "");

		// If nothing changed (no trailing slash/whitespace), pass through
		if (normalizedURI.equals(requestURI)) {
			chain.doFilter(request, response);
			return;
		}

		// Wrap the request to override the URI with the normalized version
		HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(req) {
			@Override
			public String getRequestURI() {
				return normalizedURI;
			}
		};

		// Continue the chain with the wrapped request
		chain.doFilter(wrappedRequest, response);
	}

	@Override
	public void destroy() {
		// No cleanup needed
	}

}
