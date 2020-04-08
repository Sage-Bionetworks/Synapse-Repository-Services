package org.sagebionetworks.auth.filter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.auth.UserNameAndPassword;
import org.sagebionetworks.auth.services.AuthenticationService;
import org.sagebionetworks.authutil.ModHttpServletRequest;
import org.sagebionetworks.cloudwatch.Consumer;
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
	
	@Autowired
	public DockerClientAuthFilter(Consumer consumer, AuthenticationService authenticationService) {
		super(consumer);
		this.authenticationService = authenticationService;
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
		
		if (credentials == null) {
			return true;
		}
		
		LoginRequest credential = new LoginRequest();
		
		credential.setUsername(credentials.getUserName());
		credential.setPassword(credentials.getPassword());
		
		try {
			authenticationService.login(credential);
		} catch (UnauthenticatedException e) {
			return false;
		}
		
		return true;

	}
	
	@Override
	protected void proceed(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain,
			UserNameAndPassword credentials) throws ServletException, IOException {
		Long userId = null;
		
		if (credentials == null) {
			userId = BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId();
		} else {
			try {
				PrincipalAlias alias = authenticationService.lookupUserForAuthentication(credentials.getUserName());
				userId = alias.getPrincipalId();
			} catch (NotFoundException e) {
				rejectRequest(response);
				return;
			}
		}

		Map<String, String[]> modParams = new HashMap<String, String[]>(request.getParameterMap());
		modParams.put(AuthorizationConstants.USER_ID_PARAM, new String[] { userId.toString() });
		HttpServletRequest modRqst = new ModHttpServletRequest(request, null, modParams);
		
		filterChain.doFilter(modRqst, response);
	}

}
