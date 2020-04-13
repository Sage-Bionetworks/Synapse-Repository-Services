package org.sagebionetworks.repo.web;

import static org.sagebionetworks.repo.model.AuthorizationConstants.SYNAPSE_AUTHORIZATION_HEADER_NAME;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.auth.HttpAuthUtil;
import org.sagebionetworks.repo.manager.oauth.ClaimsJsonUtil;
import org.sagebionetworks.repo.manager.oauth.OIDCTokenHelper;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.web.controller.RequiredScope;
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
	private static final Set<OAuthScope> DEFAULT_SCOPES = new HashSet<OAuthScope>(
			Arrays.asList(new OAuthScope[] {OAuthScope.view, OAuthScope.download, OAuthScope.modify}));

	private static final String ERROR_MESSAGE_PREFIX  = "Request lacks scope(s) required by this service: ";
	
	@Autowired
	private OIDCTokenHelper oidcTokenHelper;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		Set<OAuthScope> missingScopes;
		{
			List<OAuthScope> requestScopes = Collections.EMPTY_LIST;
			Set<OAuthScope> requiredScopes = new HashSet<OAuthScope>(DEFAULT_SCOPES);

			if (handler instanceof HandlerMethod) {
				HandlerMethod handlerMethod = (HandlerMethod) handler;
				RequiredScope requiredScopeAnnotation = handlerMethod.getMethodAnnotation(RequiredScope.class);
				if (requiredScopeAnnotation != null) {
					requiredScopes = new HashSet<OAuthScope>(Arrays.asList(requiredScopeAnnotation.scope()));
				}

				String synapseAuthorizationHeader = request.getHeader(SYNAPSE_AUTHORIZATION_HEADER_NAME);
				if (synapseAuthorizationHeader!=null) {
					String accessToken = HttpAuthUtil.getBearerTokenFromAuthorizationHeader(synapseAuthorizationHeader);
					Jwt<JwsHeader, Claims> jwt = oidcTokenHelper.parseJWT(accessToken);
					requestScopes = ClaimsJsonUtil.getScopeFromClaims(jwt.getBody());
				}
			}

			requiredScopes.removeAll(requestScopes);
			missingScopes = requiredScopes;
		}

		if (missingScopes.isEmpty()) {
			return true;
		}
		
		StringBuilder sb = new StringBuilder(ERROR_MESSAGE_PREFIX);
		boolean first = true;
		for (OAuthScope missingScope : missingScopes) {
			if (first) first=false; else sb.append(", ");
			sb.append(missingScope.name());
		}

		HttpAuthUtil.reject(response, sb.toString(), HttpStatus.FORBIDDEN);
		
		return false;
	}

}
