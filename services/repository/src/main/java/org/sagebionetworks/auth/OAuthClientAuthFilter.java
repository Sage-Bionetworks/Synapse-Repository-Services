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
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.sagebionetworks.authutil.ModParamHttpServletRequest;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.auth.OAuthClientDao;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.securitytools.PBKDF2Utils;
import org.springframework.beans.factory.annotation.Autowired;

public class OAuthClientAuthFilter implements Filter {
	
	@Autowired
	private OAuthClientDao oauthClientDao;

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {

		HttpServletRequest httpRequest = (HttpServletRequest)request;

		UserNameAndPassword up = HttpAuthUtil.getBasicAuthenticationCredentials(httpRequest);
		
		Map<String, String[]> modParams = new HashMap<String, String[]>(httpRequest.getParameterMap());
		modParams.remove(AuthorizationConstants.OAUTH_VERIFIED_CLIENT_ID_PARAM);
		boolean validCredentials=false;

		if (up!=null && StringUtils.isNotEmpty(up.getUserName())) {
			try {
				String oauthClientId = up.getUserName();
				byte[] secretSalt = oauthClientDao.getSecretSalt(oauthClientId);
				String hash = PBKDF2Utils.hashPassword(up.getPassword(), secretSalt);
				validCredentials = oauthClientDao.checkOAuthClientSecretHash(oauthClientId, hash);
				if (validCredentials) {
					// add in the clientId as a request param
					modParams.put(AuthorizationConstants.OAUTH_VERIFIED_CLIENT_ID_PARAM, new String[] {oauthClientId});
				}
			} catch (NotFoundException e) {
				validCredentials=false;
			}
		}
		
		if (validCredentials) {
			HttpServletRequest modRqst = new ModParamHttpServletRequest(httpRequest, modParams);
			chain.doFilter(modRqst, response);
		} else {
			HttpServletResponse httpResponse = (HttpServletResponse)response;
			httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			httpResponse.setContentType("application/json");
			httpResponse.getOutputStream().println("{\"reason\":\"Missing or invalid OAuth 2.0 client credentials.\"}");
			
		}
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
