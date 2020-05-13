package org.sagebionetworks.auth.filter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
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
import org.springframework.http.HttpStatus;
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
		return false;
	}

	@Override
	protected boolean validCredentials(UserNameAndPassword credentials) {
		try {
			// is the password actually an access token?
			oidcManager.getUserId(credentials.getPassword());
			return true;
		} catch (IllegalArgumentException iae) {
			// the password is NOT a (valid) access token,
			// but maybe it's a password
			LoginRequest credential = new LoginRequest();
			
			credential.setUsername(credentials.getUserName());
			credential.setPassword(credentials.getPassword());
			
			try {
				authenticationService.login(credential);
				return true;
			} catch (UnauthenticatedException e) {
				return false;
			}
		}
	}
	
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain, UserNameAndPassword credentials) throws ServletException, IOException {
		
		Long userId = BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId();
		
		Map<String, String[]> modHeaders = HttpAuthUtil.filterAuthorizationHeaders(request);
		
		if (credentials != null) {
			String accessToken = null;
			try {
				// is the password actually an access token?
				accessToken = credentials.getPassword();
				userId = Long.parseLong(oidcManager.getUserId(accessToken));
			} catch (IllegalArgumentException iae) {
				try {
					String username = credentials.getUserName();
					PrincipalAlias alias = authenticationService.lookupUserForAuthentication(username);
					userId = alias.getPrincipalId();
					accessToken = oidcTokenHelper.createTotalAccessToken(userId);
				} catch (NotFoundException e) {
					rejectRequest(response, getInvalidCredentialsMessage());
					return;
				}
			}
			HttpAuthUtil.setBearerTokenHeader(modHeaders, accessToken);
			if (!authenticationService.hasUserAcceptedTermsOfUse(userId)) {
				HttpAuthUtil.reject((HttpServletResponse) response, HttpAuthUtil.TOU_UNSIGNED_REASON, HttpStatus.FORBIDDEN);
				return;
			}
		}
		
		Map<String, String[]> modParams = new HashMap<String, String[]>(request.getParameterMap());
		modParams.put(AuthorizationConstants.USER_ID_PARAM, new String[] { userId.toString() });
		HttpServletRequest modRqst = new ModHttpServletRequest(request, modHeaders, modParams);
		
		filterChain.doFilter(modRqst, response);
	}

}
