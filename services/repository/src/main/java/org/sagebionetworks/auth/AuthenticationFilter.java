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
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.NotFoundException;
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
		resp.setHeader("WWW-Authenticate", "authenticate Crowd");
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
				reject(req, (HttpServletResponse)servletResponse, reason);
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
				reject(req, (HttpServletResponse)servletResponse, e.getMessage());
				log.warn("Invalid HMAC signature", e);
				return;
			} catch (NotFoundException e) {
				reject(req, (HttpServletResponse)servletResponse, e.getMessage());
				log.warn("Invalid HMAC signature", e);
				return;
			}
		}
		if (username == null && !allowAnonymous) {
			String reason = "The session token provided was missing, invalid or expired.";
			reject(req, (HttpServletResponse)servletResponse, reason);
			log.warn("Anonymous not allowed");
			return;
		}
		if (username == null) {
			username = AuthorizationConstants.ANONYMOUS_USER_ID;
		}

		// Pass along, including the user ID
		@SuppressWarnings("unchecked")
		Map<String,String[]> modParams = new HashMap<String,String[]>(req.getParameterMap());
		modParams.put(AuthorizationConstants.USER_ID_PARAM, new String[]{username});
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
	 * passed in then an AuthenticationException is thrown.
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

