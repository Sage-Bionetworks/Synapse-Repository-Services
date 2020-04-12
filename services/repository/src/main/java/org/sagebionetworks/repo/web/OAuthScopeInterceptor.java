package org.sagebionetworks.repo.web;

import static org.sagebionetworks.repo.model.AuthorizationConstants.SYNAPSE_AUTHORIZATION_HEADER_NAME;

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.auth.HttpAuthUtil;
import org.sagebionetworks.repo.manager.oauth.ClaimsJsonUtil;
import org.sagebionetworks.repo.manager.oauth.OIDCTokenHelper;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.web.controller.RequiredScope;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwt;

public class OAuthScopeInterceptor implements HandlerInterceptor {

	/*
	 * If a handler is not annotated with RequiredScope then, by default, it requires the following
	 */
	private static final OAuthScope[] DEFAULT_SCOPE = new OAuthScope[] {OAuthScope.view, OAuthScope.download, OAuthScope.modify};

	@Autowired
	private OIDCTokenHelper oidcTokenHelper;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		boolean accessTokenHasSufficientScope = false;

		if (handler instanceof HandlerMethod) {
			HandlerMethod handlerMethod = (HandlerMethod) handler;
			RequiredScope requiredScopeAnnotation = handlerMethod.getMethodAnnotation(RequiredScope.class);
			OAuthScope[] requiredScopeArray;
			if (requiredScopeAnnotation == null) {
				requiredScopeArray = DEFAULT_SCOPE;
			} else {
				requiredScopeArray = requiredScopeAnnotation.scope();
			}
			
			String synapseAuthorizationHeader = request.getHeader(SYNAPSE_AUTHORIZATION_HEADER_NAME);
			if (synapseAuthorizationHeader!=null) {
				String accessToken = HttpAuthUtil.getBearerTokenFromAuthorizationHeader(synapseAuthorizationHeader);
				Jwt<JwsHeader, Claims> jwt = oidcTokenHelper.parseJWT(accessToken);
				List<OAuthScope> scopes = ClaimsJsonUtil.getScopeFromClaims(jwt.getBody());
				accessTokenHasSufficientScope = scopes.containsAll(Arrays.asList(requiredScopeArray));
			}
		}
		
		if (accessTokenHasSufficientScope) {
			return true;
		}
		
		HttpAuthUtil.reject(response, "Request lacks required scope.", HttpStatus.FORBIDDEN); // TODO 'word smith' this error message
		return false;
	}

}
