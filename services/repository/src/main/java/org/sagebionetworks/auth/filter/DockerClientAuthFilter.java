package org.sagebionetworks.auth.filter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.auth.HttpAuthUtil;
import org.sagebionetworks.auth.UserNameAndPassword;
import org.sagebionetworks.auth.services.AuthenticationService;
import org.sagebionetworks.authutil.ModHttpServletRequest;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.repo.manager.oauth.OIDCTokenHelper;
import org.sagebionetworks.repo.manager.oauth.OpenIDConnectManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UnauthenticatedException;
import org.sagebionetworks.repo.model.auth.LoginRequest;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("dockerClientAuthFilter")
public class DockerClientAuthFilter extends BasicAuthenticationFilter {	
	private AuthenticationService authenticationService;
	
	private OIDCTokenHelper oidcTokenHelper;

	private OpenIDConnectManager oidcManager;
	
	@Autowired
	public DockerClientAuthFilter(
			StackConfiguration config, 
			Consumer consumer, 
			AuthenticationService authenticationService,
			OIDCTokenHelper oidcTokenHelper,
			OpenIDConnectManager oidcManager) {
		super(config, consumer);
		this.authenticationService = authenticationService;
		this.oidcTokenHelper=oidcTokenHelper;
		this.oidcManager=oidcManager;
	}

	// The anonymous user can come in
	@Override
	protected boolean credentialsRequired() {
		return false;
	}
	
	@Override
	protected boolean reportBadCredentialsMetric() {
		return true;
	}
	
	@Override
	protected void validateCredentialsAndDoFilterInternal(
			HttpServletRequest httpRequest, HttpServletResponse httpResponse, 
			FilterChain filterChain, Optional<UserNameAndPassword> credentials) throws IOException, ServletException {
		
		Optional<UserIdAndAccessToken> userIdAndAccessToken = Optional.empty();
		
		if (credentials.isPresent()) {
			userIdAndAccessToken = getUserIdAndAccessToken(credentials.get());
			if (!userIdAndAccessToken.isPresent()) {
				rejectRequest(httpResponse, getInvalidCredentialsMessage());
				return;
			}
		}

		doFilterInternal(httpRequest, httpResponse, filterChain, userIdAndAccessToken);
	}

	/*
	 * return the user id and access token for the given credentials or nothing if credentials are invalid
	 */
	private Optional<UserIdAndAccessToken> getUserIdAndAccessToken(UserNameAndPassword credentials) {
		try {
			// is the password actually an access token?
			String userId = oidcManager.validateAccessToken(credentials.getPassword());
			return Optional.of(new UserIdAndAccessToken(userId, credentials.getPassword()));
		} catch (IllegalArgumentException iae) {
			// the password is NOT a (valid) access token,
			// but maybe it's a password
			LoginRequest credential = new LoginRequest();
			
			credential.setUsername(credentials.getUserName());
			credential.setPassword(credentials.getPassword());
			
			try {
				authenticationService.login(credential);
			} catch (UnauthenticatedException e) {
				return Optional.empty();
			}
			PrincipalAlias alias = null;
			try {
				String username = credentials.getUserName();
				alias = authenticationService.lookupUserForAuthentication(username);
			} catch (NotFoundException e) {
				return Optional.empty();
			}
			Long userId = alias.getPrincipalId();
			String accessToken = oidcTokenHelper.createTotalAccessToken(userId);
			return Optional.of(new UserIdAndAccessToken(userId.toString(), accessToken));
		}
	}
	
	/*
	 * userIdAndAccessToken is empty for anonymous requests
	 */
	private void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain, Optional<UserIdAndAccessToken> userIdAndAccessToken) throws ServletException, IOException {
		
		String userId = BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId().toString();
		
		Map<String, String[]> modHeaders = HttpAuthUtil.filterAuthorizationHeaders(request);
		
		if (userIdAndAccessToken.isPresent()) {
			userId = userIdAndAccessToken.get().getUserId();
			String accessToken = userIdAndAccessToken.get().getAccessToken();
			HttpAuthUtil.setBearerTokenHeader(modHeaders, accessToken);
		}
		
		Map<String, String[]> modParams = new HashMap<String, String[]>(request.getParameterMap());
		modParams.put(AuthorizationConstants.USER_ID_PARAM, new String[] { userId });
		HttpServletRequest modRqst = new ModHttpServletRequest(request, modHeaders, modParams);
		
		filterChain.doFilter(modRqst, response);
	}

}
