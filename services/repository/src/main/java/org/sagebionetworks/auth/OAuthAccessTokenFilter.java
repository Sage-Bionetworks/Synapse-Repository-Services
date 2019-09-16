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
import org.sagebionetworks.repo.manager.oauth.OIDCTokenHelper;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.springframework.beans.factory.annotation.Autowired;

public class OAuthAccessTokenFilter implements Filter {
	
	@Autowired
	private OIDCTokenHelper oidcTokenHelper;

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {

		HttpServletRequest httpRequest = (HttpServletRequest)request;

		String bearerToken = HttpAuthUtil.getBearerToken(httpRequest);
		
		boolean verified=false;
		IllegalArgumentException validationException = null;
		if (bearerToken!=null) {
			try {
				oidcTokenHelper.validateJWT(bearerToken);
				verified=true;
			} catch (IllegalArgumentException e) {
				verified=false;
				validationException = e;
			}
		}
		
		if (verified) {
			Map<String, String[]> modParams = new HashMap<String, String[]>(httpRequest.getParameterMap());
			modParams.put(AuthorizationConstants.OAUTH_VERIFIED_ACCESS_TOKEN, new String[] {bearerToken});
			HttpServletRequest modRqst = new ModParamHttpServletRequest(httpRequest, modParams);
			chain.doFilter(modRqst, response);
		} else {
			HttpServletResponse httpResponse = (HttpServletResponse)response;
			httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			httpResponse.setContentType("application/json");
			String reason = "Missing or invalid access token";
			if (validationException!=null && StringUtils.isNotEmpty(validationException.getMessage())) {
				reason = validationException.getMessage();
			}
			httpResponse.getOutputStream().println("{\"reason\":\""+reason+"\"}");
			httpResponse.getOutputStream().flush();
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
