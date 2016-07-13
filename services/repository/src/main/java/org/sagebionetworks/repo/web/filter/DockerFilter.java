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

import org.sagebionetworks.auth.BasicAuthUtils;
import org.sagebionetworks.auth.UserNameAndPassword;
import org.sagebionetworks.auth.services.AuthenticationService;
import org.sagebionetworks.authutil.ModParamHttpServletRequest;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DomainType;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.auth.LoginCredentials;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class DockerFilter implements Filter {
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
		if (up != null) {
			try {
				LoginCredentials credential = new LoginCredentials();
				credential.setEmail(up.getUserName());
				credential.setPassword(up.getPassword());
				authenticationService.authenticate(credential, DomainType.SYNAPSE);
				PrincipalAlias alias = authenticationService.lookupUserForAuthentication(up.getUserName());
				userId = alias.getPrincipalId();
			} catch (NotFoundException e) {
			}
		}
		if (userId == null) {
			userId = BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId();
		}

		Map<String, String[]> modParams = new HashMap<String, String[]>(httpRequest.getParameterMap());
		modParams.put(AuthorizationConstants.USER_ID_PARAM, new String[] { userId.toString() });
		HttpServletRequest modRqst = new ModParamHttpServletRequest(httpRequest, modParams);
		chain.doFilter(modRqst, response);
	}

	@Override
	public void destroy() {}

}
