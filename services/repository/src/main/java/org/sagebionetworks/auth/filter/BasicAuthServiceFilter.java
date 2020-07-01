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
import org.sagebionetworks.authutil.ModHttpServletRequest;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;

/**
 * Abstraction over a filter that authenticates a service using basic authentication
 * 
 * @author Marco Marasca
 */
public abstract class BasicAuthServiceFilter extends BasicAuthenticationFilter {
	
	private ServiceKeyAndSecretProvider keyAndSecretProvider;
	
	public BasicAuthServiceFilter(StackConfiguration config, Consumer consumer, ServiceKeyAndSecretProvider keyAndSecretProvider) {
		super(config, consumer);
		this.keyAndSecretProvider = keyAndSecretProvider;
	}
	
	@Override
	protected boolean credentialsRequired() {
		return true;
	}
	
	@Override
	protected boolean reportBadCredentialsMetric() {
		return true;
	}
	
	/**
	 * @return True if the filter should inject the admin user for controllers
	 */
	protected boolean isAdminService() {
		return false;
	}

	@Override
	protected void validateCredentialsAndDoFilterInternal(HttpServletRequest httpRequest,
			HttpServletResponse httpResponse, FilterChain filterChain, Optional<UserNameAndPassword> credentials)
			throws IOException, ServletException {
		
		if (!credentials.isPresent()) {
			rejectRequest(httpResponse, "Missing required basic authentication credentials.");
			return;
		}
		
		UserNameAndPassword keyAndSecret = credentials.get();
		
		if (!keyAndSecretProvider.validate(keyAndSecret.getUserName(), keyAndSecret.getPassword())) {
			rejectRequest(httpResponse, getInvalidCredentialsMessage());
			return;
		}
		
		// Makes sure to filter out potential malicious headers that could be used down the filter chain
		Map<String, String[]> modHeaders = HttpAuthUtil.filterAuthorizationHeaders(httpRequest);
		Map<String, String[]> modParams = new HashMap<String, String[]>(httpRequest.getParameterMap());
		
		// Adds the service name into the headers so that it can be processed down the filter chain
		HttpAuthUtil.setServiceNameHeader(modHeaders, keyAndSecretProvider.getServiceName());
		
		if (isAdminService()) {
			Long adminUser = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
			// Injects the admin user so that is is clear that it is an administrative service and that can be used in the controllers
			modParams.put(AuthorizationConstants.USER_ID_PARAM, new String[] { adminUser.toString() });
		}
		
		filterChain.doFilter(new ModHttpServletRequest(httpRequest, modHeaders, modParams), httpResponse);
	}
	
}
