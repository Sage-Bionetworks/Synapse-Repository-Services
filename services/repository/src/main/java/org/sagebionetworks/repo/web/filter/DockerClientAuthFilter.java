package org.sagebionetworks.repo.web.filter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.sagebionetworks.auth.BasicAuthUtils;
import org.sagebionetworks.auth.UserNameAndPassword;
import org.sagebionetworks.auth.services.AuthenticationService;
import org.sagebionetworks.authutil.ModParamHttpServletRequest;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.auth.LoginCredentials;
import org.sagebionetworks.repo.model.auth.LoginRequest;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class DockerClientAuthFilter implements Filter {
	@Autowired
	private AuthenticationService authenticationService;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest httpRequest = (HttpServletRequest)request;
		UserNameAndPassword up = BasicAuthUtils.getBasicAuthenticationCredentials(httpRequest);

		Long userId = null;
		if (up == null) {
			userId = BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId();
		} else {
			try {
				LoginRequest credential = new LoginRequest();
				credential.setUsername(up.getUserName());
				credential.setPassword(up.getPassword());
				authenticationService.login(credential);
				PrincipalAlias alias = authenticationService.lookupUserForAuthentication(up.getUserName());
				userId = alias.getPrincipalId();
			} catch (NotFoundException e) {
				HttpServletResponse httpResponse = (HttpServletResponse)response;
				httpResponse.setStatus(HttpStatus.SC_UNAUTHORIZED);
				return;
			}
		}

		Map<String, String[]> modParams = new HashMap<String, String[]>(httpRequest.getParameterMap());
		modParams.put(AuthorizationConstants.USER_ID_PARAM, new String[] { userId.toString() });
		HttpServletRequest modRqst = new ModParamHttpServletRequest(httpRequest, modParams);
		chain.doFilter(modRqst, response);
	}

	@Override
	public void destroy() {}

}
