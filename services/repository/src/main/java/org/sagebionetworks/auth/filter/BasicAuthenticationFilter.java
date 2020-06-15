package org.sagebionetworks.auth.filter;

import java.io.IOException;
import java.util.Optional;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.auth.HttpAuthUtil;
import org.sagebionetworks.auth.UserNameAndPassword;

/**
 * Implementation of a filter that extracts base64 encoded credentials from the
 * Authorization header using the basic scheme.
 * 
 * @author Marco Marasca
 *
 */
public abstract class BasicAuthenticationFilter implements Filter {
	private static final String MISSING_CREDENTIALS_MSG = "Missing required credentials in the authorization header.";
	private static final String INVALID_CREDENTIALS_MSG = "Invalid credentials.";
	
	protected FilterHelper filterHelper;
	
	public BasicAuthenticationFilter(FilterHelper filterHelper) {
		this.filterHelper = filterHelper;
	}
	
	@Override
	public final void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
			throws IOException, ServletException {

		if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
			throw new ServletException("Only HTTP requests are supported");
		}

		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;

		Optional<UserNameAndPassword> credentials;

		try {
			credentials = HttpAuthUtil.getBasicAuthenticationCredentials(httpRequest);
		} catch (IllegalArgumentException e) {
			filterHelper.rejectRequest(reportBadCredentialsMetric(), httpResponse, e);
			return;
		}

		if (credentialsRequired() && !credentials.isPresent()) {
			filterHelper.rejectRequest(reportBadCredentialsMetric(), httpResponse, MISSING_CREDENTIALS_MSG);
			return;
		}

		validateCredentialsAndDoFilterInternal(httpRequest, httpResponse, filterChain, credentials);
	}
	
	/**
	 * Validates the credentials (if required and present) and proceeds with the request invoking the
	 * {@link FilterChain#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse)}
	 * method.
	 * 
	 * @param httpRequest
	 * @param httpResponse
	 * @param filterChain
	 * @param credentials The credentials extracted from the Authorization header,
	 *                    might be missing, if credentials are not required
	 * @throws IOException
	 * @throws ServletException
	 */
	protected abstract void validateCredentialsAndDoFilterInternal(
			HttpServletRequest httpRequest, HttpServletResponse httpResponse, 
			FilterChain filterChain, Optional<UserNameAndPassword> credentials) throws IOException, ServletException;
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void destroy() {
	}

	/**
	 * @return True if the credentials are required, false otherwise (e.g. anonymous
	 *         access)
	 */
	protected boolean credentialsRequired() {
		return true;
	}

	/**
	 * @return True if the invoking the filter with bad or missing credentials
	 *         should lead to a report in cloud watch (e.g. for services that are
	 *         managed by the platform)
	 */
	protected boolean reportBadCredentialsMetric() {
		return false;
	}

	/**
	 * @return The message returned if the credentials are invalid
	 */
	protected String getInvalidCredentialsMessage() {
		return INVALID_CREDENTIALS_MSG;
	}
}
