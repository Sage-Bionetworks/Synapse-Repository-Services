package org.sagebionetworks.auth;

import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
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
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.auth.services.AuthenticationService;
import org.sagebionetworks.authutil.ModParamHttpServletRequest;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.securitytools.HMACUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This filter authenticates incoming requests:
 * (1) Checks for session token.  If present, validates the token and determines the user's identification;
 * (2) If no session token, checks whether the request has an HMAC-SHA1 signature.  If so, validates the signature;
 * (3) If neither of the above, passes the request through as anonymous.  (It is then the service's responsibility
 * 		to reject requests that cannot be made anonymously.)
 */
public class AuthenticationFilter implements Filter {
	
	private static final Log log = LogFactory.getLog(AuthenticationFilter.class);
	
	@Autowired
	private AuthenticationService authenticationService;

	/**
	 * Defines a few security exceptions for a user representing the web port
	 * Allows handling of the OpenID handshake on the web client
	 */
	private static final String PORTAL_USER_NAME = StackConfiguration.getPortalUsername();
	private static final String AUTH_PATH = "/auth/v1";
	private static final List<String> VALID_PORTAL_CALLS = Arrays
			.asList(new String[] { UrlHelpers.AUTH_USER, UrlHelpers.AUTH_SESSION_PORTAL });
	
	private boolean allowAnonymous = false;
	
	public AuthenticationFilter() {}
	
	/**
	 * For testing
	 */
	public AuthenticationFilter(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}
	
	@Override
	public void destroy() { }
	
	private static void reject(HttpServletRequest req, HttpServletResponse resp, String reason) throws IOException {
		resp.setStatus(401);

		// This header is required according to RFC-2612
		// See: http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.2
		//      http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.47
		//      http://www.ietf.org/rfc/rfc2617.txt
		resp.setHeader("WWW-Authenticate", "\"Digest\" your email");
		resp.getWriter().println("{\"reason\", \""+reason+"\"}");
	}

	@Override
	public void doFilter(ServletRequest servletRqst, ServletResponse servletResponse,
			FilterChain filterChain) throws IOException, ServletException {
		// First look for a session token in the header or as a parameter
		HttpServletRequest req = (HttpServletRequest) servletRqst;
		String sessionToken = req.getHeader(AuthorizationConstants.SESSION_TOKEN_PARAM);
		if (sessionToken == null) {
			// Check for a session token as a parameter
			sessionToken = req.getParameter(AuthorizationConstants.SESSION_TOKEN_PARAM);
		}
		
		// Determine the caller's identity
		String username = null;
		
		// A session token maps to a specific user
		if (sessionToken != null) {
			try {
				String userId = authenticationService.revalidate(sessionToken);
				username = authenticationService.getUsername(userId);
			} catch (Exception xee) {
				String reason = "The session token is invalid.";
				reject(req, (HttpServletResponse) servletResponse, reason);
				log.warn("invalid session token", xee);
				return;
			}
		
		// If there is no session token, then check for a HMAC signature
		} else if (isSigned(req)) {  
			username = req.getHeader(AuthorizationConstants.USER_ID_HEADER);
			try {
				String secretKey = authenticationService.getSecretKey(username);
				matchHMACSHA1Signature(req, secretKey);
			} catch (UnauthorizedException e) {
				reject(req, (HttpServletResponse) servletResponse, e.getMessage());
				log.warn("Invalid HMAC signature", e);
				return;
			} catch (NotFoundException e) {
				reject(req, (HttpServletResponse) servletResponse, e.getMessage());
				log.warn("Invalid HMAC signature", e);
				return;
			}
		}
		if (username == null && !allowAnonymous) {
			String reason = "The session token provided was missing, invalid or expired.";
			reject(req, (HttpServletResponse) servletResponse, reason);
			log.warn("Anonymous not allowed");
			return;
		}
		if (username == null) {
			username = AuthorizationConstants.ANONYMOUS_USER_ID;
		}
		
		// Special case for the user corresponding to the portal
		// This user can pretend to be another user for a limited set of requests
		if (username.equals(PORTAL_USER_NAME)) {
			if (!isSigned(req)) {
				String reason = "Requests made by the portal must use a secret key";
				reject(req, (HttpServletResponse) servletResponse, reason);
				log.warn("Portal request not signed");
				return;
			}
			
			// Match the portion of the URI after /auth/v1 to a set of allowed values
			String reqURI = req.getRequestURI();
			int authPathIndex = reqURI.indexOf(AUTH_PATH);
			if (authPathIndex >= 0) {
				reqURI = reqURI.substring(authPathIndex + AUTH_PATH.length());
			}
			
			if (VALID_PORTAL_CALLS.contains(reqURI)) {
				// For some calls, the username of the caller is inferred from the session token or secret key signature
				// This extra parameter allows that inference to be overriden with the portal's choice of user
				String pretender = req.getParameter(AuthorizationConstants.PORTAL_MASQUERADE_PARAM);
				log.debug("Portal request made to " + req.getRequestURI() + " on behalf of " + pretender);
				if (pretender != null) {
					username = pretender;
				}
			} else {
				String reason = "The portal cannot make a call to " + req.getRequestURI();
				reject(req, (HttpServletResponse) servletResponse, reason);
				log.warn("Forbidden portal request made to " + req.getRequestURI());
				return;
			}
		}

		// Pass along, including the user ID
		@SuppressWarnings("unchecked")
		Map<String, String[]> modParams = new HashMap<String, String[]>(req.getParameterMap());
		modParams.put(AuthorizationConstants.USER_ID_PARAM, new String[] { username });
		HttpServletRequest modRqst = new ModParamHttpServletRequest(req, modParams);
		filterChain.doFilter(modRqst, servletResponse);
	}

	private static final long MAX_TIMESTAMP_DIFF_MIN = 15;
	
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
	public static void matchHMACSHA1Signature(HttpServletRequest request, String secretKey) throws UnauthorizedException {
		String username = request.getHeader(AuthorizationConstants.USER_ID_HEADER);
		String uri = request.getRequestURI();
		String signature = request.getHeader(AuthorizationConstants.SIGNATURE);
		String date = request.getHeader(AuthorizationConstants.SIGNATURE_TIMESTAMP);

    	// Compute the difference between what time this machine thinks it is (in UTC)
    	//   vs. the timestamp in the header of the request (also in UTC)
    	DateTime timeStamp = new DateTime(date); 
    	int timeDiff = Minutes.minutesBetween(new DateTime(), timeStamp).getMinutes();

    	if (Math.abs(timeDiff) > MAX_TIMESTAMP_DIFF_MIN) {
    		throw new UnauthorizedException("Timestamp in request, " + date + ", is out of date");
    	}

    	String expectedSignature = HMACUtils.generateHMACSHA1Signature(username, uri, date, secretKey);
    	if (!expectedSignature.equals(signature)) {
       		throw new UnauthorizedException("Invalid digital signature: " + signature);
    	}
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		@SuppressWarnings("unchecked")
        Enumeration<String> paramNames = filterConfig.getInitParameterNames();
        while (paramNames.hasMoreElements()) {
        	String paramName = paramNames.nextElement();
        	String paramValue = filterConfig.getInitParameter(paramName);
           	if ("allow-anonymous".equalsIgnoreCase(paramName)) allowAnonymous = Boolean.parseBoolean(paramValue);
        }
  	}
}

