package org.sagebionetworks.auth;

import java.io.IOException;
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
import org.sagebionetworks.authutil.ModHttpServletRequest;
import org.sagebionetworks.repo.manager.oauth.OIDCTokenHelper;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UnauthenticatedException;
import org.springframework.beans.factory.annotation.Autowired;

public class OAuthAccessTokenFilter implements Filter {
	
	@Autowired
	private OIDCTokenHelper oidcTokenHelper;

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {

		HttpServletRequest httpRequest = (HttpServletRequest)request;

		String bearerToken = HttpAuthUtil.getBearerToken(httpRequest);
		
		if (StringUtils.isEmpty(bearerToken)) {
			// Check for session token.  If present then it becomes the bearer token.
			// Once clients stop using session token then we can remove this temporary logic.
			bearerToken = httpRequest.getHeader(AuthorizationConstants.SESSION_TOKEN_PARAM);
		}
		
		boolean reject=true;
		Exception validationException = null;
		String reason = "Unauthenticated";
		if (!StringUtils.isEmpty(bearerToken)) {
			try {
				oidcTokenHelper.validateJWT(bearerToken);
				reject=false;
			} catch (IllegalArgumentException e) {
				reject=true;
				validationException = e;
				reason = "Invalid access token";
			}
		} else if (HttpAuthUtil.isDigitalSignaturePresent(httpRequest)){
			try {
				// if valid, then create total access bearer token
				long principalId = HttpAuthUtil.getDigitalSignaturePrincipalId(httpRequest);
				bearerToken = oidcTokenHelper.createTotalAccessToken(principalId);
				reject=false;
			} catch (UnauthenticatedException e) {
				// otherwise reject
				reject=true;
				validationException = e;
				reason = "Invalid digital signature";
			}
		} else {
			// there is no bearer token (as an Auth or sessionToken header) or digital signature
			// so create an 'anonymous' bearer token
			bearerToken = oidcTokenHelper.createAnonymousAccessToken();
			reject=false;
		}
		
		if (!reject) {
			Map<String, String[]> modHeaders = HttpAuthUtil.filterAuthorizationHeaders(httpRequest);
			HttpAuthUtil.setBearerTokenHeader(modHeaders, bearerToken);
			HttpServletRequest modRqst = new ModHttpServletRequest(httpRequest, modHeaders, null);
			chain.doFilter(modRqst, response);
		} else {
			if (validationException!=null && StringUtils.isNotEmpty(validationException.getMessage())) {
				reason = validationException.getMessage();
			}
			HttpAuthUtil.reject((HttpServletResponse)response, reason);
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
