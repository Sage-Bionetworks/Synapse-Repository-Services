package org.sagebionetworks.auth.filter;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.auth.HttpAuthUtil;
import org.sagebionetworks.auth.UserNameAndPassword;
import org.sagebionetworks.authutil.ModHttpServletRequest;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.repo.manager.oauth.OAuthClientManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.oauth.OAuthClientIdAndSecret;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implements the client_secret_basic and client_secret_post protocols for authenticating an OAuth client
 * See: https://tools.ietf.org/html/rfc6749#section-2.3.1
 */
@Component("oauthClientAuthFilter")
public class OAuthClientAuthFilter implements Filter {
	private static final String INVALID_CREDENTIALS_MSG = "OAuth Client ID and secret must be passed via Basic Authentication. Credentials are missing or invalid.";


	private StackConfiguration config;

	private Consumer consumer;

	private OAuthClientManager oauthClientManager;

	private static final boolean REPORT_BAD_CREDENTIALS_METRIC = true;
	
	@Autowired
	public OAuthClientAuthFilter(StackConfiguration config, Consumer consumer, OAuthClientManager oauthClientManager) {
		this.config=config;
		this.consumer=consumer;
		this.oauthClientManager=oauthClientManager;
	}

	private boolean validCredentials(UserNameAndPassword credentials) {
		OAuthClientIdAndSecret clientCreds = new OAuthClientIdAndSecret();

		clientCreds.setClient_id(credentials.getUserName());
		clientCreds.setClient_secret(credentials.getPassword());

		return this.oauthClientManager.validateClientCredentials(clientCreds);
	}
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
			throws IOException, ServletException {

		if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
			throw new ServletException("Only HTTP requests are supported");
		}

		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;

		Optional<UserNameAndPassword> credentials;
		
		FilterHelper filterHelper = new FilterHelper(config, consumer);

		try {
			credentials = HttpAuthUtil.getBasicAuthenticationCredentials(httpRequest);
		} catch (IllegalArgumentException e) {
			filterHelper.rejectRequest(REPORT_BAD_CREDENTIALS_METRIC, httpResponse, e);
			return;
		}

		if (!credentials.isPresent()) {
			filterHelper.rejectRequest(REPORT_BAD_CREDENTIALS_METRIC, httpResponse, "Missing required credentials in the authorization header.");
			return;
		}

		if (credentials.isPresent() && !validCredentials(credentials.get())) {
			filterHelper.rejectRequest(REPORT_BAD_CREDENTIALS_METRIC, httpResponse, INVALID_CREDENTIALS_MSG);
			return;
		}

		if (!credentials.isPresent()) {
			throw new IllegalStateException("Credentials were expected but not supplied");
		}

		String oauthClientId = credentials.get().getUserName();

		// get the current headers, but be sure to leave behind anything that might be
		// mistaken for a valid
		// authentication header 'down the filter chain'

		Map<String, String[]> modHeaders = HttpAuthUtil.filterAuthorizationHeaders(httpRequest);
		modHeaders.put(AuthorizationConstants.OAUTH_VERIFIED_CLIENT_ID_HEADER, new String[] { oauthClientId });
		HttpServletRequest modRqst = new ModHttpServletRequest(httpRequest, modHeaders, null);

		filterChain.doFilter(modRqst, httpResponse);
	}
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {

	}

	@Override
	public void destroy() {

	}
}
