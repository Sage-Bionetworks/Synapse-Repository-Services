package org.sagebionetworks.auth.filter;

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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.Minutes;
import org.sagebionetworks.auth.HttpAuthUtil;
import org.sagebionetworks.auth.services.AuthenticationService;
import org.sagebionetworks.authutil.ModHttpServletRequest;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.oauth.OAuthClientNotVerifiedException;
import org.sagebionetworks.repo.manager.oauth.OIDCTokenHelper;
import org.sagebionetworks.repo.manager.oauth.OpenIDConnectManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UnauthenticatedException;
import org.sagebionetworks.repo.web.ForbiddenException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.OAuthException;
import org.sagebionetworks.securitytools.HMACUtils;
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
	private AuthenticationService authenticationService;

	@Autowired
	private UserManager userManager;

	@Autowired
	private OIDCTokenHelper oidcTokenHelper;

	@Autowired
	private OpenIDConnectManager oidcManager;

	@Override
	public void destroy() { }

	@Override
	public void doFilter(ServletRequest servletRqst, ServletResponse servletResponse,
			FilterChain filterChain) throws IOException, ServletException {
		// First look for a session token in the header or as a parameter
		HttpServletRequest req = (HttpServletRequest) servletRqst;
		String sessionToken = req.getHeader(AuthorizationConstants.SESSION_TOKEN_PARAM);
		if (isTokenEmptyOrNull(sessionToken)) {
			// Check for a session token as a parameter
			sessionToken = req.getParameter(AuthorizationConstants.SESSION_TOKEN_PARAM);
		}

		// Determine the caller's identity
		Long userId = null;
		String accessToken = null;

		// A session token maps to a specific user
		if (!isTokenEmptyOrNull(sessionToken)) {
			String failureReason = "Invalid session token";
			try {
				userId = authenticationService.revalidate(sessionToken, false);
			} catch (UnauthenticatedException | NotFoundException  e) {
				HttpAuthUtil.reject((HttpServletResponse) servletResponse, failureReason);
				log.warn(failureReason, e);
				return;
			}
			accessToken=oidcTokenHelper.createTotalAccessToken(userId);
			// If there is no session token, then check for a HMAC signature
		} else if (isSigned(req)) {
			String failureReason = "Invalid HMAC signature";
			String username = req.getHeader(AuthorizationConstants.USER_ID_HEADER);
			try {
				userId = userManager.lookupUserByUsernameOrEmail(username).getPrincipalId();
				String secretKey = authenticationService.getSecretKey(userId);
				matchHMACSHA1Signature(req, secretKey);
			} catch (UnauthenticatedException | NotFoundException e) {
				HttpAuthUtil.reject((HttpServletResponse) servletResponse, e.getMessage());
				log.warn(failureReason, e);
				return;
			}
			accessToken=oidcTokenHelper.createTotalAccessToken(userId);
		} else {
			accessToken=HttpAuthUtil.getBearerTokenFromStandardAuthorizationHeader(req);
			if (!isTokenEmptyOrNull(accessToken)) {
				try {
					// validate token and get userid parameter
					userId = Long.parseLong(oidcManager.validateAccessToken(accessToken));
				} catch (IllegalArgumentException | ForbiddenException | OAuthClientNotVerifiedException e) {
					String failureReason = "Invalid access token";
					if (StringUtils.isNotEmpty(e.getMessage())) {
						failureReason = e.getMessage();
					}
					HttpAuthUtil.reject((HttpServletResponse)servletResponse, failureReason);
					log.warn(failureReason, e);
					return;
				} catch (OAuthException e) {
					HttpAuthUtil.rejectWithOAuthError((HttpServletResponse)servletResponse, e.getError(), e.getErrorDescription(), HttpStatus.UNAUTHORIZED);
					log.warn(e.getMessage(), e);
					return;
				}
			} else { // anonymous
				userId = BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId();
			}
		}
		
		// there are multiple paths to this point, but all require creating a userId
		ValidateArgument.required(userId, "userId");

		// Put the userId on thread local, so this thread always knows who is calling
		currentUserIdThreadLocal.set(userId);
		
		// Pass the request along, including the user Id and access token
		try {
			Map<String, String[]> modParams = new HashMap<String, String[]>(req.getParameterMap());
			modParams.put(AuthorizationConstants.USER_ID_PARAM, new String[] { userId.toString() });
			Map<String, String[]> modHeaders = HttpAuthUtil.filterAuthorizationHeaders(req);
			if (accessToken!=null) {
				HttpAuthUtil.setBearerTokenHeader(modHeaders, accessToken);
			}
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

	// One more min than max wait at client
	private static final long MAX_TIMESTAMP_DIFF_MIN = 31;

	public static boolean isSigned(HttpServletRequest request) {
		String username = request.getHeader(AuthorizationConstants.USER_ID_HEADER);
		String date = request.getHeader(AuthorizationConstants.SIGNATURE_TIMESTAMP);
		String signature = request.getHeader(AuthorizationConstants.SIGNATURE);
		return username != null && date != null && signature != null;
	}

	/**
	 * Tries to create the HMAC-SHA1 hash.  If it doesn't match the signature
	 * passed in then an UnauthorizedException is thrown.
	 */
	public static void matchHMACSHA1Signature(HttpServletRequest request, String secretKey) throws UnauthenticatedException {
		String username = request.getHeader(AuthorizationConstants.USER_ID_HEADER);
		String uri = request.getRequestURI();
		String signature = request.getHeader(AuthorizationConstants.SIGNATURE);
		String date = request.getHeader(AuthorizationConstants.SIGNATURE_TIMESTAMP);

		// Compute the difference between what time this machine thinks it is (in UTC)
		//   vs. the timestamp in the header of the request (also in UTC)
		DateTime timeStamp = new DateTime(date); 
		int timeDiff = Minutes.minutesBetween(new DateTime(), timeStamp).getMinutes();

		if (Math.abs(timeDiff) > MAX_TIMESTAMP_DIFF_MIN) {
			throw new UnauthenticatedException("Timestamp in request, " + date + ", is out of date.");
		}

		String expectedSignature = HMACUtils.generateHMACSHA1Signature(username, uri, date, secretKey);
		if (!expectedSignature.equals(signature)) {
			throw new UnauthenticatedException("Invalid digital signature: " + signature);
		}
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {}
}

