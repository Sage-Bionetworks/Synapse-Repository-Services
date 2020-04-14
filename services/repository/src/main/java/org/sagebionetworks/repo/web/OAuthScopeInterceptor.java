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
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.web.controller.RequiredScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestParam;
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
	
//	public static boolean isAnonymous(Jwt<JwsHeader, Claims> jwt) {
//		Claims claims = jwt.getBody();
//		return claims.getSubject().equals(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId().toString()) &&
//			claims.getAudience().equals(AuthorizationConstants.SYNAPSE_OAUTH_CLIENT_ID);
//	}
	
	public static boolean hasUserIdParameter(HandlerMethod handlerMethod) {
		for (MethodParameter methodParameter : handlerMethod.getMethodParameters()) {
			RequestParam requestParam = methodParameter.getParameterAnnotation(RequestParam.class);
			if (requestParam!=null && requestParam.value().equals(AuthorizationConstants.USER_ID_PARAM)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isAnonymous(HttpServletRequest request) {
		String userIdRequestParameter = request.getParameter(AuthorizationConstants.USER_ID_PARAM);
		return userIdRequestParameter ==null ||
				AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId()
					.equals(Long.parseLong(userIdRequestParameter));
	}

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
					requiredScopes = new HashSet<OAuthScope>(Arrays.asList(requiredScopeAnnotation.value()));
				}

				// if no 'userId' parameter then this is not an authenticated request, 
				// and no scope is required
				if (!hasUserIdParameter(handlerMethod)) {
					requiredScopes.clear();
				}

				// if anonymous, consider it to have 'full scope'
				if (isAnonymous(request)) {
					requestScopes = Arrays.asList(OAuthScope.values());
				} else {
					String synapseAuthorizationHeader = request.getHeader(SYNAPSE_AUTHORIZATION_HEADER_NAME);
					if (synapseAuthorizationHeader!=null) {
						String accessToken = HttpAuthUtil.getBearerTokenFromAuthorizationHeader(synapseAuthorizationHeader);
						Jwt<JwsHeader, Claims> jwt = oidcTokenHelper.parseJWT(accessToken);
						requestScopes = ClaimsJsonUtil.getScopeFromClaims(jwt.getBody());
					}
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
