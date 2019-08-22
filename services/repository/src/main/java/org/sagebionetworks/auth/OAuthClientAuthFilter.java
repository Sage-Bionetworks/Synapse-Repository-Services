package org.sagebionetworks.auth;

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

import org.apache.commons.lang3.StringUtils;
import org.sagebionetworks.authutil.ModParamHttpServletRequest;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.auth.OAuthClientDao;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class OAuthClientAuthFilter implements Filter {
	
	@Autowired
	private OAuthClientDao oauthClientDao;

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {

		HttpServletRequest httpRequest = (HttpServletRequest)request;

		UserNameAndPassword up = BasicAuthUtils.getBasicAuthenticationCredentials(httpRequest);
		
		Map<String, String[]> modParams = new HashMap<String, String[]>(httpRequest.getParameterMap());
		// strip out verifiedOAuthClientId request param so that the sender can't 'sneak it past us'
		modParams.remove(AuthorizationConstants.OAUTH_VERIFIED_CLIENT_ID_PARAM);

		if (up!=null && StringUtils.isNotEmpty(up.getUserName())) {
			try {
				String oauthClientId = up.getUserName();
				String oauthClientSecret = oauthClientDao.getOAuthClientSecret(oauthClientId);
				// add in the clientId as a request param
				if (oauthClientSecret.equals(up.getPassword())) {
					modParams.put(AuthorizationConstants.OAUTH_VERIFIED_CLIENT_ID_PARAM, new String[] {oauthClientId});
				}
				// let the request continue, but unauthenticated
			} catch (NotFoundException e) {
				// let the request continue, but unauthenticated
			}
		}
		
		HttpServletRequest modRqst = new ModParamHttpServletRequest(httpRequest, modParams);
		chain.doFilter(modRqst, response);
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// Nothing to do
	}

	@Override
	public void destroy() {
		// Nothing to do
	}

}
