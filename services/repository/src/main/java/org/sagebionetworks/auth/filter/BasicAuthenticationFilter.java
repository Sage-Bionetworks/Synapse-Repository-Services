package org.sagebionetworks.auth.filter;

import java.io.IOException;
import java.util.Date;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.auth.HttpAuthUtil;
import org.sagebionetworks.auth.UserNameAndPassword;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.springframework.web.filter.OncePerRequestFilter;

import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.google.common.collect.ImmutableMap;

/**
 * Implementation of a filter that extracts base64 encoded credentials from the
 * Authorization header using the basic scheme.
 * 
 * @author Marco Marasca
 *
 */
public abstract class BasicAuthenticationFilter extends OncePerRequestFilter {
	
	private static final String INVALID_CREDENTIALS_MSG = "Credentials are missing or invalid.";
	private static final String CLOUD_WATCH_NAMESPACE = "Authentication";
	private static final String CLOUD_WATCH_METRIC_NAME = "BadCredentials";
	private static final String CLOUD_WATCH_DIMENSION_FILTER = "filterClass";
	private static final String CLOUD_WATCH_UNIT_COUNT = StandardUnit.Count.toString();
	
	private Consumer consumer;
	
	protected BasicAuthenticationFilter(Consumer consumer) {
		this.consumer = consumer;
	}

	@Override
	protected final void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {

		UserNameAndPassword credentials = HttpAuthUtil.getBasicAuthenticationCredentials(request);

		if (credentialsRequired() && credentials == null) {
			rejectRequest(response);
			return;
		}

		if (credentials != null && !validCredentials(credentials)) {
			rejectRequest(response);
			return;
		}

		proceed(request, response, filterChain, credentials);
	}
	
	protected final void rejectRequest(HttpServletResponse response) throws IOException {
		
		if (reportBadCredentialsMetric()) {
			
			ProfileData logEvent = new ProfileData();
			
			logEvent.setNamespace(CLOUD_WATCH_NAMESPACE);
			logEvent.setName(CLOUD_WATCH_METRIC_NAME);
			logEvent.setValue(1.0);
			logEvent.setUnit(CLOUD_WATCH_UNIT_COUNT);
			logEvent.setTimestamp(new Date());
			
			logEvent.setDimension(ImmutableMap.of(CLOUD_WATCH_DIMENSION_FILTER, getClass().getName()));
			
			consumer.addProfileData(logEvent);
		}
		
		HttpAuthUtil.reject(response, getInvalidCredentialMessage());
	}

	/**
	 * Proceeds with the request invoking the
	 * {@link FilterChain#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse)}
	 * method. Can be overridden to alter the behavior in the filter chain. 
	 * 
	 * @param request
	 * @param response
	 * @param filterChain
	 * @param credentials
	 * @throws ServletException
	 * @throws IOException
	 */
	protected void proceed(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain,
			UserNameAndPassword credentials) throws ServletException, IOException {
		filterChain.doFilter(request, response);
	}

	/**
	 * @param credentials The credentials extracted from the Authorization header,
	 *                    can be null if {@link #credentialsRequired()} is false
	 * @return True if the given credentials are valid, false otherwise
	 */
	protected abstract boolean validCredentials(UserNameAndPassword credentials);

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
	 * @return The message returned if the credentials are missing
	 */
	protected String getInvalidCredentialMessage() {
		return INVALID_CREDENTIALS_MSG;
	}

}
