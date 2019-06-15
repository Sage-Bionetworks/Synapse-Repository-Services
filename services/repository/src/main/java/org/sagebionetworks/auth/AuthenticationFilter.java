package org.sagebionetworks.auth;

import java.io.IOException;
import java.util.Enumeration;
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.Minutes;
import org.sagebionetworks.auth.services.AuthenticationService;
import org.sagebionetworks.authutil.ModParamHttpServletRequest;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ErrorResponse;
import org.sagebionetworks.repo.model.UnauthenticatedException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.securitytools.HMACUtils;
import org.sagebionetworks.util.ThreadLocalProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

/**
 * This filter authenticates incoming requests:
 * (1) Checks for session token.  If present, validates the token and determines the user's identification;
 * (2) If no session token, checks whether the request has an HMAC-SHA1 signature.  If so, validates the signature;
 * (3) If neither of the above, passes the request through as anonymous.  (It is then the service's responsibility
 * 		to reject requests that cannot be made anonymously.)
 */
public class AuthenticationFilter implements Filter {
	
	private static final Log log = LogFactory.getLog(AuthenticationFilter.class);
	
	private static final ThreadLocal<Long> currentUserIdThreadLocal = ThreadLocalProvider.getInstance(AuthorizationConstants.USER_ID_PARAM, Long.class);

	@Autowired
	private AuthenticationService authenticationService;

	@Autowired
	private UserManager userManager;
	
	private boolean allowAnonymous = false;
	
	@Override
	public void destroy() { }
	
	private static void reject(HttpServletRequest req, HttpServletResponse resp, String reason) throws IOException {
		reject(req, resp, reason, HttpStatus.UNAUTHORIZED);
	}
	
	private static void reject(HttpServletRequest req, HttpServletResponse resp, String reason, HttpStatus status) throws IOException {
		resp.setStatus(status.value());

		// This header is required according to RFC-2612
		// See: http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.2
		//      http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.47
		//      http://www.ietf.org/rfc/rfc2617.txt
		resp.setHeader("WWW-Authenticate", "\"Digest\" your email");
		ErrorResponse er = new ErrorResponse();
		er.setReason(reason);
		JSONObjectAdapter joa = new JSONObjectAdapterImpl();
		try {
			er.writeToJSONObject(joa);
			resp.getWriter().println(joa.toJSONString());
		} catch (JSONObjectAdapterException e) {
			// give up here, use old method, so we at least send something back
			resp.getWriter().println("{\"reason\": \"" + reason + "\"}");
		}
	}

	@Override
	public void doFilter(ServletRequest servletRqst, ServletResponse servletResponse,
			FilterChain filterChain) throws IOException, ServletException {
		// First look for a session token in the header or as a parameter
		HttpServletRequest req = (HttpServletRequest) servletRqst;
		String sessionToken = req.getHeader(AuthorizationConstants.SESSION_TOKEN_PARAM);
		if (isSessionTokenEmptyOrNull(sessionToken)) {
			// Check for a session token as a parameter
			sessionToken = req.getParameter(AuthorizationConstants.SESSION_TOKEN_PARAM);
		}
		
		// Determine the caller's identity
		Long userId = null;
		
		// A session token maps to a specific user
		if (!isSessionTokenEmptyOrNull(sessionToken)) {
			String failureReason = "Invalid session token";
			try {
				userId = authenticationService.revalidate(sessionToken, false);
			} catch (UnauthenticatedException e) {
				reject(req, (HttpServletResponse) servletResponse, failureReason);
				log.warn(failureReason, e);
				return;
			} catch (NotFoundException e) {
				reject(req, (HttpServletResponse) servletResponse, failureReason);
				log.warn(failureReason, e);
				return;
			}
		
		// If there is no session token, then check for a HMAC signature
		} else if (isSigned(req)) {
			String failureReason = "Invalid HMAC signature";
			String username = req.getHeader(AuthorizationConstants.USER_ID_HEADER);
			try {
				userId = userManager.lookupUserByUsernameOrEmail(username).getPrincipalId();
				String secretKey = authenticationService.getSecretKey(userId);
				matchHMACSHA1Signature(req, secretKey);
			} catch (UnauthenticatedException e) {
				reject(req, (HttpServletResponse) servletResponse, e.getMessage());
				log.warn(failureReason, e);
				return;
			} catch (NotFoundException e) {
				reject(req, (HttpServletResponse) servletResponse, e.getMessage());
				log.warn(failureReason, e);
				return;
			}
		}
		
		if (userId == null && !allowAnonymous) {
			String reason = "The session token provided was missing, invalid or expired.";
			reject(req, (HttpServletResponse) servletResponse, reason);
			log.warn("Anonymous not allowed");
			return;
		}
		
		// If the user has been identified, check if they have accepted the terms of use
		if (userId != null) {
			boolean toUCheck = false;
			try {
				toUCheck = authenticationService.hasUserAcceptedTermsOfUse(userId);
			} catch (NotFoundException e) {
				String reason = "User " + userId + " does not exist";
				reject(req, (HttpServletResponse) servletResponse, reason, HttpStatus.NOT_FOUND);
				log.error("This should be unreachable", e);
				return;
			}
			if (!toUCheck) {
				String reason = "Terms of use have not been signed";
				reject(req, (HttpServletResponse) servletResponse, reason, HttpStatus.FORBIDDEN);
				return;
			}	
		}
		
		if (userId == null) {
			userId = BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId();
		}

		// Put the userId on thread local, so this thread always knows who is calling
		currentUserIdThreadLocal.set(userId);
		try {
			// Pass along, including the user ID
			Map<String, String[]> modParams = new HashMap<String, String[]>(req.getParameterMap());
			modParams.put(AuthorizationConstants.USER_ID_PARAM, new String[] { userId.toString() });
			HttpServletRequest modRqst = new ModParamHttpServletRequest(req, modParams);
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
	private boolean isSessionTokenEmptyOrNull(String sessionToken){
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
    		throw new UnauthenticatedException("Timestamp in request, " + date + ", is out of date");
    	}

    	String expectedSignature = HMACUtils.generateHMACSHA1Signature(username, uri, date, secretKey);
    	if (!expectedSignature.equals(signature)) {
       		throw new UnauthenticatedException("Invalid digital signature: " + signature);
    	}
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		Enumeration<String> paramNames = filterConfig.getInitParameterNames();
        while (paramNames.hasMoreElements()) {
        	String paramName = paramNames.nextElement();
        	String paramValue = filterConfig.getInitParameter(paramName);
           	if ("allow-anonymous".equalsIgnoreCase(paramName)) allowAnonymous = Boolean.parseBoolean(paramValue);
        }
  	}
}

