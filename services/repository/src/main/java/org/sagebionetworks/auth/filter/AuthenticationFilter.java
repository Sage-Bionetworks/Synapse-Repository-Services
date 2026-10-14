package org.sagebionetworks.auth.filter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.auth.HttpAuthUtil;
import org.sagebionetworks.authutil.ModHttpServletRequest;
import org.sagebionetworks.repo.manager.oauth.OAuthClientNotVerifiedException;
import org.sagebionetworks.repo.manager.oauth.OpenIDConnectManager;
import org.sagebionetworks.repo.model.AuthenticationMethod;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.RealmDao;
import org.sagebionetworks.repo.web.ForbiddenException;
import org.sagebionetworks.repo.web.OAuthException;
import org.sagebionetworks.util.ThreadLocalProvider;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * This filter authenticates incoming requests:
 * (1) Checks for session token.  If present, validates the token and determines the user's identification;
 * (2) If no session token, checks whether the request has an HMAC-SHA1 signature.  If so, validates the signature;
 * (3) If neither of the above, passes the request through as anonymous.  (It is then the service's responsibility
 * 		to reject requests that cannot be made anonymously.)
 */
@Component("authFilter")
public class AuthenticationFilter implements Filter {
	
	private static final Log log = LogFactory.getLog(AuthenticationFilter.class);
	
	private static final ThreadLocal<Long> currentUserIdThreadLocal = ThreadLocalProvider.getInstance(AuthorizationConstants.USER_ID_PARAM, Long.class);

	@Autowired
	private OpenIDConnectManager oidcManager;
	
	@Autowired
	private RealmDao realmDao;

	@Override
	public void destroy() { }
	
	@Override
	public void doFilter(ServletRequest servletRqst, ServletResponse servletResponse,
			FilterChain filterChain) throws IOException, ServletException {

		AuthenticationMethod authenticationMethod = null;

		// First look for a session token in the header or as a parameter
		HttpServletRequest req = (HttpServletRequest) servletRqst;
		
		// access token can be passed in a sessionToken header
		String accessToken = req.getHeader(AuthorizationConstants.SESSION_TOKEN_PARAM);
		if (isTokenEmptyOrNull(accessToken)) {
			accessToken = HttpAuthUtil.getBearerTokenFromStandardAuthorizationHeader(req);
		} else {
			authenticationMethod = AuthenticationMethod.SESSIONTOKEN;
		}
		
		Long userId = null;
		boolean isAnonymous = false;

			if (!isTokenEmptyOrNull(accessToken)) {
				try {
					// validate token and get userid parameter
					userId = Long.parseLong(oidcManager.validateAccessToken(accessToken));
					if (authenticationMethod == null) { // accessToken came in as sessionToken
						authenticationMethod = AuthenticationMethod.BEARERTOKEN;
					}
					Optional<String> anonymousUserRealm = realmDao.getRealmForAnonymousPrincipal(userId.toString());
					isAnonymous = anonymousUserRealm.isPresent(); // true if userId is the anonymous user in some realm
				} catch (IllegalArgumentException | ForbiddenException | OAuthClientNotVerifiedException e) {
					String failureReason = "Invalid access token";
					HttpAuthUtil.reject((HttpServletResponse)servletResponse, failureReason);
					log.warn(failureReason + ": " + e.getMessage());
					return;
				} catch (OAuthException e) {
					HttpAuthUtil.rejectWithOAuthError((HttpServletResponse)servletResponse, e.getError(), e.getErrorDescription(), HttpStatus.UNAUTHORIZED);
					log.warn(e.getMessage());
					return;
				}
			} else { // anonymous
				userId = BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId();
				isAnonymous = true;
			}

		if (authenticationMethod == null && HttpAuthUtil.usesBasicAuthentication(req)) {
			authenticationMethod = AuthenticationMethod.BASIC;
		} // else it's anonymous

		// there are multiple paths to this point, but all require creating a userId
		ValidateArgument.required(userId, "userId");

		// Put the userId on thread local, so this thread always knows who is calling
		currentUserIdThreadLocal.set(userId);
		
		// Pass the request along, including the user Id and access token
		try {
			Map<String, String[]> modParams = new HashMap<String, String[]>(req.getParameterMap());
			modParams.put(AuthorizationConstants.USER_ID_PARAM, new String[] { userId.toString() });
			modParams.put(AuthorizationConstants.ANONYMOUS_PARAM, new String[] { ""+isAnonymous });
			Map<String, String[]> modHeaders = HttpAuthUtil.filterAuthorizationHeaders(req);
			if (accessToken!=null) {
				HttpAuthUtil.setBearerTokenHeader(modHeaders, accessToken);
			}
			HttpAuthUtil.setAuthenticationMethod(modHeaders, authenticationMethod);
			HttpServletRequest modRqst = new ModHttpServletRequest(req, modHeaders, modParams);
			filterChain.doFilter(modRqst, servletResponse);
		} finally {
			// not strictly necessary, but just in case
			currentUserIdThreadLocal.set(null);
		}
	}

	/**
	 * Is a session token empty or null?
	 * This is part of the fix for PLFM-2422.
	 * @param sessionToken
	 * @return
	 */
	private boolean isTokenEmptyOrNull(String sessionToken){
		if(sessionToken == null) return true;
		if("".equals(sessionToken.trim())) return true;
		return false;
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {}
}

