package org.sagebionetworks.auth.filter;

import java.io.IOException;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
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

@Component("oauthClientAuthFilter")
public class OAuthClientAuthFilter extends BasicAuthenticationFilter {

	private static final String INVALID_CREDENTIAL_MSG = "OAuth Client ID and secret must be passed via Basic Authentication. Credentials are missing or invalid.";

	private OAuthClientManager oauthClientManager;

	@Autowired
	public OAuthClientAuthFilter(StackConfiguration config, Consumer consumer, OAuthClientManager oauthClientManager) {
		super(config, consumer);
		this.oauthClientManager = oauthClientManager;
	}

	@Override
	protected boolean credentialsRequired() {
		return true;
	}

	@Override
	protected boolean reportBadCredentialsMetric() {
		return false;
	}

	@Override
	protected String getInvalidCredentialsMessage() {
		return INVALID_CREDENTIAL_MSG;
	}

	@Override
	protected boolean validCredentials(UserNameAndPassword credentials) {
		OAuthClientIdAndSecret clientCreds = new OAuthClientIdAndSecret();

		clientCreds.setClient_id(credentials.getUserName());
		clientCreds.setClient_secret(credentials.getPassword());

		return oauthClientManager.validateClientCredentials(clientCreds);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain, UserNameAndPassword credentials) throws ServletException, IOException {
		
		if (credentials == null) {
			throw new IllegalStateException("Credentials were expected but not supplied");
		}

		String oauthClientId = credentials.getUserName();

		// get the current headers, but be sure to leave behind anything that might be
		// mistaken for a valid
		// authentication header 'down the filter chain'

		Map<String, String[]> modHeaders = HttpAuthUtil.filterAuthorizationHeaders(request);
		modHeaders.put(AuthorizationConstants.OAUTH_VERIFIED_CLIENT_ID_HEADER, new String[] { oauthClientId });
		HttpServletRequest modRqst = new ModHttpServletRequest(request, modHeaders, null);

		filterChain.doFilter(modRqst, response);
	}

}
