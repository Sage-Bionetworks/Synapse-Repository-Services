package org.sagebionetworks.authutil;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joda.time.DateTime;
import org.joda.time.Minutes;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.securitytools.HMACUtils;
import org.springframework.http.HttpStatus;

/**
 *
 */
public class CrowdAuthenticationFilter implements Filter {
	private static final Logger log = Logger.getLogger(CrowdAuthenticationFilter.class
			.getName());
	
	private boolean allowAnonymous = false;
	private boolean acceptAllCerts = true;
	private boolean usingMockCrowd;
	
//	CrowdAuthUtil crowdAuthUtil = new CrowdAuthUtil();
	
	@Override
	public void destroy() {
	}
	
	private static void reject(HttpServletRequest req, HttpServletResponse resp, String reason) throws IOException {
		resp.setStatus(401);
		resp.setHeader("WWW-Authenticate", "authenticate Crowd");
		resp.getWriter().println("{\"reason\", \""+reason+"\"}");
	}
	
	
	private static Map<String,String> tokenCache = null; // maps authenticated tokens to userIds
	private static Map<String,String> secretKeyCache = null; // maps userIds to secret keys
	private static Long cacheTimeout = null;
	private static Date lastCacheDump = null;
	
	private void initCaches() {
		tokenCache = Collections.synchronizedMap(new HashMap<String,String>());
		secretKeyCache = Collections.synchronizedMap(new HashMap<String,String>());
		lastCacheDump = new Date();
		String s = System.getProperty(AuthorizationConstants.AUTH_CACHE_TIMEOUT_MILLIS);
		if (s!=null && s.length()>0) {
			cacheTimeout = Long.parseLong(s);
		} else {
			cacheTimeout = AuthorizationConstants.AUTH_CACHE_TIMEOUT_DEFAULT;
		}
	}
	
	private void checkCacheDump() {
		Date now = new Date();
		if (lastCacheDump.getTime()+cacheTimeout<now.getTime()) {
			tokenCache.clear();
			secretKeyCache.clear();
			lastCacheDump = now;
		}
	}

	@Override
	public void doFilter(ServletRequest servletRqst, ServletResponse servletResponse,
			FilterChain filterChain) throws IOException, ServletException {

		// If token present, ask Crowd to validate and get user id
		HttpServletRequest req = (HttpServletRequest)servletRqst;
		String sessionToken = req.getHeader(AuthorizationConstants.SESSION_TOKEN_PARAM);
		String userId = null;
		if(usingMockCrowd){
			// Some tests provide a user name.
			userId = req.getParameter(AuthorizationConstants.USER_ID_PARAM);
		}
		if (null!=sessionToken) {
			// validate against crowd
			try {
				if (usingMockCrowd) {
					userId= sessionToken;
				} else {
					userId = null;
					if (cacheTimeout>0) { // then use cache
						checkCacheDump();
						userId = tokenCache.get(sessionToken);
					}
					if (userId==null) { // not using cache or not found in cache
						userId = (new CrowdAuthUtil()).revalidate(sessionToken);
						if (cacheTimeout>0) {
							tokenCache.put(sessionToken, userId);
						}
					}
				}
			} catch (Exception xee) {
				String reason = "The session token is invalid.";
				reject(req, (HttpServletResponse)servletResponse, reason);
				log.log(Level.WARNING, "invalid session token", xee);
				return;
			}
		} else if (isSigned(req)) {  // if no session token, then check for a HMAC signature
			userId = req.getHeader(AuthorizationConstants.USER_ID_HEADER);
			try {
				String secretKey = getUsersSecretKey(userId);
				matchHMACSHA1Signature(req, secretKey);
			} catch (AuthenticationException e) {
				reject(req, (HttpServletResponse)servletResponse, e.getMessage());
				return;
			}
		}
		if (userId==null && !allowAnonymous) {
			String reason = "The session token provided was missing, invalid or expired.";
			reject(req, (HttpServletResponse)servletResponse, reason);
			return;
		}
		if (userId==null) userId = AuthorizationConstants.ANONYMOUS_USER_ID;

		// pass along, including the user id
		@SuppressWarnings("unchecked")
		Map<String,String[]> modParams = new HashMap<String,String[]>(req.getParameterMap());
		modParams.put(AuthorizationConstants.USER_ID_PARAM, new String[]{userId});
		HttpServletRequest modRqst = new ModParamHttpServletRequest(req, modParams);
		filterChain.doFilter(modRqst, servletResponse);
	}
	
	public String getUsersSecretKey(String userId) throws AuthenticationException, IOException {
		Map<String,Collection<String>> userAttrs = null;
		String secretKey = null;
		if (cacheTimeout>0) { // then use cache
			checkCacheDump();
			secretKey = secretKeyCache.get(userId);
		}
		if (secretKey!=null) return secretKey;
		
		try {
			userAttrs = (new CrowdAuthUtil()).getUserAttributes(userId);
		} catch (NotFoundException nfe) {
			throw new AuthenticationException(HttpStatus.UNAUTHORIZED.value(), "User "+userId+" not found.", nfe);
		}
		Collection<String> secretKeyCollection = userAttrs.get(AuthorizationConstants.CROWD_SECRET_KEY_ATTRIBUTE);
		if (secretKeyCollection==null || secretKeyCollection.isEmpty()) {
			throw new AuthenticationException(HttpStatus.UNAUTHORIZED.value(), "Authentication server has no secret key registered for "+userId, null);
		}
		secretKey = secretKeyCollection.iterator().next();
		if (cacheTimeout>0) {
			secretKeyCache.put(userId, secretKey);
		}
		return secretKey;
	}


	private static final long MAX_TIMESTAMP_DIFF_MIN = 15;
	
	public static boolean isSigned(HttpServletRequest request) {
		String username = request.getHeader(AuthorizationConstants.USER_ID_HEADER);
		String date = request.getHeader(AuthorizationConstants.SIGNATURE_TIMESTAMP);
		String signature = request.getHeader(AuthorizationConstants.SIGNATURE);
		return username!=null && date!=null && signature!=null;
	}
	
	/**
	 * Tries to create the HMAC-SHA1 hash.  If it doesn't match the signature
	 * passed in then an AuthenticationException is thrown.
	 */
	public static void matchHMACSHA1Signature(HttpServletRequest request, String secretKey) throws AuthenticationException {
		String username = request.getHeader(AuthorizationConstants.USER_ID_HEADER);
		String uri = request.getRequestURI();
//		StringBuffer sb = request.getRequestURL();
//		String url = sb.toString();
		String signature = request.getHeader(AuthorizationConstants.SIGNATURE);
		String date = request.getHeader(AuthorizationConstants.SIGNATURE_TIMESTAMP);
		

    	// compute the difference between what time this machine thinks it is (in UTC)
    	// vs. the timestamp in the header of the request (also in UTC)

    	DateTime timeStamp = new DateTime(date); 
    	int timeDiff = Minutes.minutesBetween(new DateTime(), timeStamp).getMinutes();

    	if (Math.abs(timeDiff)>MAX_TIMESTAMP_DIFF_MIN) {
    		throw new AuthenticationException(HttpStatus.UNAUTHORIZED.value(), 
    				"Timestamp in request, "+date+", is out of date.", null);
    	}

    	String expectedSignature = HMACUtils.generateHMACSHA1Signature(username, uri, date, secretKey);
    	if (!expectedSignature.equals(signature)) {
       		throw new AuthenticationException(HttpStatus.UNAUTHORIZED.value(), 
       				"Invalid digital signature: "+signature, null);
    	}
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		initCaches();
		@SuppressWarnings("unchecked")
        Enumeration<String> paramNames = filterConfig.getInitParameterNames();
        while (paramNames.hasMoreElements()) {
        	String paramName = paramNames.nextElement();
        	String paramValue = filterConfig.getInitParameter(paramName);
           	if ("allow-anonymous".equalsIgnoreCase(paramName)) allowAnonymous = Boolean.parseBoolean(paramValue);
           	if ("accept-all-certificates".equalsIgnoreCase(paramName)) acceptAllCerts = Boolean.parseBoolean(paramValue);
        }
        
        String certsProperty = System.getProperty(AuthorizationConstants.ACCEPT_ALL_CERTS);
        
        if (certsProperty!=null && "true".equalsIgnoreCase(certsProperty.trim())) acceptAllCerts=true;
        if (acceptAllCerts) CrowdAuthUtil.acceptAllCertificates2();
       
       
		String implementingClassName = System.getProperty(AuthorizationConstants.USER_DAO_INTEGRATION_TEST_SWITCH);
		if (implementingClassName!=null && implementingClassName.length()>0) {
			usingMockCrowd = true;
		}else{
			usingMockCrowd = false;
		}
  	}
}

