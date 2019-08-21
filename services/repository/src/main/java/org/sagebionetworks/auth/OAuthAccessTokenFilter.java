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

import org.sagebionetworks.authutil.ModParamHttpServletRequest;
import org.sagebionetworks.repo.manager.oauth.OIDCTokenUtil;
import org.sagebionetworks.repo.model.AuthorizationConstants;

public class OAuthAccessTokenFilter implements Filter {
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {

		HttpServletRequest httpRequest = (HttpServletRequest)request;

		String bearerToken = httpRequest.getHeader("Bearer");

		Map<String, String[]> modParams = new HashMap<String, String[]>(httpRequest.getParameterMap());
		// strip out clientId request param so that the sender can't 'sneak it past us'
		modParams.remove(AuthorizationConstants.OAUTH_VERIFIED_CLIENT_ID_PARAM);

		boolean verified=false;
		if (bearerToken!=null) {
			verified = OIDCTokenUtil.validateSignedJWT(bearerToken);
		}
		
		if (verified) {
			// TODO pass the token along. Should it be as a header or as a request parameter?
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
