package org.sagebionetworks.auth.filter;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
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
	private static final String CLIENT_ID_PARAM_NAME = "client_id";
	private static final String CLIENT_SECRET_PARAM_NAME = "client_secret";

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
		return true;
	}

	@Override
	protected String getInvalidCredentialsMessage() {
		return INVALID_CREDENTIAL_MSG;
	}

	@Override
	protected void validateCredentialsAndDoFilterInternal(
			HttpServletRequest httpRequest, HttpServletResponse httpResponse, 
			FilterChain filterChain, Optional<UserNameAndPassword> credentials) throws IOException, ServletException {
		if (credentials.isPresent() && !validCredentials(credentials.get())) {
			rejectRequest(httpResponse, getInvalidCredentialsMessage());
			return;
		}

		doFilterInternal(httpRequest, httpResponse, filterChain, credentials.orElse(null));
	}
	
	protected Optional<UserNameAndPassword> getCredentialsFromRequest(HttpServletRequest httpRequest) {
		Optional<UserNameAndPassword> credentials = HttpAuthUtil.getBasicAuthenticationCredentials(httpRequest);
		
		// if credentials are not in the authorization header (i.e. passed by client_secret_basic),
		// the might be passed as request parameters (i.e. passed by client_secret_post)
		if (!credentials.isPresent()) {
			String clientId = httpRequest.getParameter(CLIENT_ID_PARAM_NAME);
			String clientSecret = httpRequest.getParameter(CLIENT_SECRET_PARAM_NAME);
			if (StringUtils.isEmpty(clientId) || StringUtils.isBlank(clientSecret)) {
				credentials = Optional.empty();
			} else {
				if (StringUtils.contains(httpRequest.getQueryString(), clientSecret)) {
					throw new IllegalArgumentException("Client credentials must not be passed as query parameters.");
				}
				credentials = Optional.of(new UserNameAndPassword(clientId, clientSecret));
			}
		}
		
		return credentials;
	}


	private boolean validCredentials(UserNameAndPassword credentials) {
		OAuthClientIdAndSecret clientCreds = new OAuthClientIdAndSecret();

		clientCreds.setClient_id(credentials.getUserName());
		clientCreds.setClient_secret(credentials.getPassword());

		return oauthClientManager.validateClientCredentials(clientCreds);
	}

	private void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain, UserNameAndPassword credentials) throws ServletException, IOException {
		
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
