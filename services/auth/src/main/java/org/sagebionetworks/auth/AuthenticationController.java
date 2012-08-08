package org.sagebionetworks.auth;

import static org.sagebionetworks.repo.model.AuthorizationConstants.ACCEPTS_TERMS_OF_USE_ATTRIBUTE;
import static org.sagebionetworks.repo.model.ServiceConstants.ACCEPTS_TERMS_OF_USE_PARAM;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openid4java.consumer.ConsumerManager;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.StringEncrypter;
import org.sagebionetworks.authutil.AuthenticationException;
import org.sagebionetworks.authutil.CrowdAuthUtil;
import org.sagebionetworks.authutil.SendMail;
import org.sagebionetworks.authutil.Session;
import org.sagebionetworks.authutil.User;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.auth.RegistrationInfo;
import org.sagebionetworks.repo.web.ForbiddenException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.securitytools.HMACUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;


@Controller
public class AuthenticationController extends BaseController {
	private static final Logger log = Logger.getLogger(AuthenticationController.class
			.getName());
	
	private static Map<User,Session> sessionCache = null;
	private static Long cacheTimeout = null;
	private static Date lastCacheDump = null;
//	   a special userId that's used for integration testing
//	   we need a way to specify a 'back door' userId for integration testing
//	   the authentication servlet
//	   this should not be present in the production deployment
//	   The behavior is as follows
//	  	If passed to the user creation service, there is no confirmation email generated.
//	  	Instead the password is taken from the incoming request
	private String integrationTestUser = null;

	private static final String REGISTRATION_TOKEN_PREFIX = "register_";

	private void initCache() {
		sessionCache = Collections.synchronizedMap(new HashMap<User,Session>());
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
			sessionCache.clear();
			lastCacheDump = now;
		}
	}

	/**
	 * @return the integrationTestUser
	 */
	public String getIntegrationTestUser() {
		return integrationTestUser;
	}

	/**
	 * @param integrationTestUser the integrationTestUser to set
	 */
	public void setIntegrationTestUser(String integrationTestUser) {
		this.integrationTestUser = integrationTestUser;
	}

	public AuthenticationController() {
		initCache();
        // optional, only used for testing
        setIntegrationTestUser(StackConfiguration.getIntegrationTestUserThreeName());
	}
	
	public static boolean getAcceptsTermsOfUse(String userId) throws NotFoundException, IOException {
		Map<String,Collection<String>> attributes = CrowdAuthUtil.getUserAttributes(userId);
		Collection<String> values = attributes.get(ACCEPTS_TERMS_OF_USE_ATTRIBUTE);
		return values!=null && values.size()>0 && Boolean.parseBoolean(values.iterator().next());
	}
	
	public static void setAcceptsTermsOfUse(String userId, boolean accepts) throws IOException {
		Map<String,Collection<String>> attributes = new HashMap<String,Collection<String>>();
		attributes.put(ACCEPTS_TERMS_OF_USE_ATTRIBUTE, Arrays.asList(new String[]{""+accepts}));
		CrowdAuthUtil.setUserAttributes(userId, attributes);
	}
	
	/**
	 * 
	 * @param userId -- the ID/email address of the user
	 * @param acceptsTermsOfUse -- says whether the request explicitly accepts the terms (false=acceptance is omitted in request, may have been given previously)
	 * @throws NotFoundException
	 * @throws IOException
	 * @throws ForbiddenException thrown if user doesn't accept terms in this request or previously
	 */
	public static boolean acceptsTermsOfUse(String userId, Boolean acceptsTermsOfUse) throws NotFoundException, IOException {
		if (CrowdAuthUtil.isAdmin(userId)) return true; // administrator need not sign terms of use
		if (!getAcceptsTermsOfUse(userId)) {
			if (acceptsTermsOfUse!=null && acceptsTermsOfUse==true) {
				setAcceptsTermsOfUse(userId, true);
				return true;
			} else {
				return false;
			}
		}	
		return true;
	}
	
	private static final String TERMS_OF_USE_ERROR_MESSAGE = "You need to sign the Synapse Terms of Use.   This may be done by logging in to Synapse on the Web.";
	
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = "/session", method = RequestMethod.POST)
	public @ResponseBody
	Session authenticate(@RequestBody User credentials,
			HttpServletRequest request) throws Exception {
		try { 
			Session session = null;
			if (cacheTimeout>0) { // then use cache
				checkCacheDump();
				session = sessionCache.get(credentials);
			}
			if (session==null) { // not using cache or not found in cache
				session = CrowdAuthUtil.authenticate(credentials, true);
				if (!acceptsTermsOfUse(credentials.getEmail(), credentials.isAcceptsTermsOfUse()))
					throw new ForbiddenException(TERMS_OF_USE_ERROR_MESSAGE);
				if (cacheTimeout>0) {
					sessionCache.put(credentials, session);
				}
			}
			return session;
		} catch (AuthenticationException ae) {
			// include the URL used to authenticate
			ae.setAuthURL(request.getRequestURL().toString());
			throw ae;
		}
	}
	
	private static final String OPEN_ID_URI = "/openid";
	
	private static final String OPENID_CALLBACK_URI = "/openidcallback";
	
	private static final String OPEN_ID_PROVIDER = "OPEN_ID_PROVIDER";
	// 		e.g. https://www.google.com/accounts/o8/id
	
	// this is the parameter name for the value of the final redirect
	private static final String RETURN_TO_URL_PARAM = "RETURN_TO_URL";
	
	private static final String OPEN_ID_ATTRIBUTE = "OPENID";
	
	private static final String RETURN_TO_URL_COOKIE_NAME = "org.sagebionetworks.auth.returnToUrl";
	private static final String ACCEPTS_TERMS_OF_USE_COOKIE_NAME = "org.sagebionetworks.auth.acceptsTermsOfUse";
	private static final int RETURN_TO_URL_COOKIE_MAX_AGE_SECONDS = 60; // seconds
	
	private static final String authenticationServicePublicEndpoint = StackConfiguration.getAuthenticationServicePublicEndpoint();
	
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = OPEN_ID_URI, method = RequestMethod.POST)
	public void openID(
			@RequestParam(value = OPEN_ID_PROVIDER, required = true) String openIdProvider,
			@RequestParam(value = ACCEPTS_TERMS_OF_USE_PARAM, required = false) Boolean acceptsTermsOfUse,
			@RequestParam(value = RETURN_TO_URL_PARAM, required = true) String returnToURL,
              HttpServletRequest request,
              HttpServletResponse response) throws Exception {

		HttpServlet servlet = null;
		
		ConsumerManager manager = new ConsumerManager();
		SampleConsumer sampleConsumer = new SampleConsumer(manager);
		
		String thisUrl = request.getRequestURL().toString();
		int i = thisUrl.indexOf(OPEN_ID_URI);
		if (i<0) throw new RuntimeException("Current URI, "+OPEN_ID_URI+", not found in "+thisUrl);
		String openIDCallbackURL = authenticationServicePublicEndpoint+OPENID_CALLBACK_URI;
//	    Here is an alternate way to do it which makes sure that the domain of the call back URL matches
//		that of the incoming request:
//		String openIDCallbackURL = thisUrl.substring(0, i)+OPENID_CALLBACK_URI;
		Cookie cookie = new Cookie(RETURN_TO_URL_COOKIE_NAME, returnToURL);
		cookie.setMaxAge(RETURN_TO_URL_COOKIE_MAX_AGE_SECONDS);
		response.addCookie(cookie);
		
		cookie = new Cookie(ACCEPTS_TERMS_OF_USE_COOKIE_NAME, ""+acceptsTermsOfUse);
		cookie.setMaxAge(RETURN_TO_URL_COOKIE_MAX_AGE_SECONDS);
		response.addCookie(cookie);
		
		sampleConsumer.authRequest(openIdProvider, openIDCallbackURL, servlet, request, response);
	}

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = OPENID_CALLBACK_URI, method = RequestMethod.GET)
	public
	void openIDCallback(
			HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		try {
			
			ConsumerManager manager = new ConsumerManager();
			
			SampleConsumer sampleConsumer = new SampleConsumer(manager);

			OpenIDInfo openIDInfo = sampleConsumer.verifyResponse(request);
			String openID = openIDInfo.getIdentifier();
						
			List<String> emails = openIDInfo.getMap().get(SampleConsumer.AX_EMAIL);
			String email = (emails==null || emails.size()<1 ? null : emails.get(0));
			List<String> fnames = openIDInfo.getMap().get(SampleConsumer.AX_FIRST_NAME);
			String fname = (fnames==null || fnames.size()<1 ? null : fnames.get(0));
			List<String> lnames = openIDInfo.getMap().get(SampleConsumer.AX_LAST_NAME);
			String lname = (lnames==null || lnames.size()<1 ? null : lnames.get(0));
			
			if (email==null) throw new AuthenticationException(400, "Unable to authenticate", null);
			
			User credentials = new User();			
			credentials.setEmail(email);

			Map<String,Collection<String>> attrs = null;
			try {
				attrs = new HashMap<String,Collection<String>>(CrowdAuthUtil.getUserAttributes(email));
			} catch (NotFoundException nfe) {
				// user doesn't exist yet, so create them
				credentials.setPassword((new Long(rand.nextLong())).toString());
				credentials.setFirstName(fname);
				credentials.setLastName(lname);
				if (fname!=null && lname!=null) credentials.setDisplayName(fname+" "+lname);
				CrowdAuthUtil.createUser(credentials);
				attrs = new HashMap<String,Collection<String>>(CrowdAuthUtil.getUserAttributes(email));
			}
			// save the OpenID in Crowd
			Collection<String> openIDs = attrs.get(OPEN_ID_ATTRIBUTE);
			if (openIDs==null) {
				attrs.put(OPEN_ID_ATTRIBUTE, Arrays.asList(new String[]{openID}));
			} else {
				Set<String> modOpenIDs = new HashSet<String>(openIDs);
				modOpenIDs.add(openID);
				attrs.put(OPEN_ID_ATTRIBUTE, modOpenIDs);
			}

			CrowdAuthUtil.setUserAttributes(email, attrs);
			
			Session crowdSession = CrowdAuthUtil.authenticate(credentials, false);


			String returnToURL = null;
			Boolean acceptsTermsOfUse = null;
			Cookie[] cookies = request.getCookies();
			for (Cookie c : cookies) {
				if (RETURN_TO_URL_COOKIE_NAME.equals(c.getName())) {
					returnToURL = c.getValue();
				}
				if (ACCEPTS_TERMS_OF_USE_COOKIE_NAME.equals(c.getName())) {
					acceptsTermsOfUse = Boolean.parseBoolean(c.getValue());
				}
			}
			if (returnToURL==null) throw new RuntimeException("Missing required return-to URL.");
			
			String redirectUrl = returnToURL+":";
			if (acceptsTermsOfUse(email, acceptsTermsOfUse)) {
				redirectUrl += crowdSession.getSessionToken();
			} else {
				redirectUrl += ServiceConstants.ACCEPTS_TERMS_OF_USE_REQUIRED_TOKEN;
			}
			String location = response.encodeRedirectURL(redirectUrl);
			response.sendRedirect(location);
			
		} catch (AuthenticationException ae) {
			// include the URL used to authenticate
			ae.setAuthURL(request.getRequestURL().toString());
			throw ae;
		}
	}
	
	// this is just for testing
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/sso", method = RequestMethod.GET)
	public
	void redirectTarget(
			HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		PrintWriter pw = response.getWriter();
		pw.println(request.getRequestURI());
	}

	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = "/session", method = RequestMethod.PUT)
	public void revalidate(@RequestBody Session session) throws Exception {
		String userId = CrowdAuthUtil.revalidate(session.getSessionToken());
		if (!acceptsTermsOfUse(userId, false /*i.e. may have accepted TOU previously, but acceptance is not given in this request*/)) {
			throw new ForbiddenException(TERMS_OF_USE_ERROR_MESSAGE);
		}
	}

	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = "/session", method = RequestMethod.DELETE)
	public void deauthenticate(HttpServletRequest request) throws Exception {
		String sessionToken = request.getHeader(AuthorizationConstants.SESSION_TOKEN_PARAM);
		if (null == sessionToken) throw new AuthenticationException(HttpStatus.BAD_REQUEST.value(), "Not authorized.", null);

			CrowdAuthUtil.deauthenticate(sessionToken);

			if (cacheTimeout>0) { // if using cache
				checkCacheDump();
				for (User user : sessionCache.keySet()) {
					Session cachedSession = sessionCache.get(user);
					if (sessionToken.equals(cachedSession.getSessionToken())) {
						sessionCache.remove(user);
						break;
					}
				}
			}
	}
	
	private Random rand = new Random();
	
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = "/user", method = RequestMethod.POST)
	public void createUser(@RequestBody User user) throws Exception {
		String itu = getIntegrationTestUser();
		boolean isITU = (itu!=null && user.getEmail().equals(itu));
		if (!isITU) {
			user.setPassword(""+rand.nextLong());
		}
		CrowdAuthUtil.createUser(user);
		if (!isITU) {
			//encrypt user session
			Session session = CrowdAuthUtil.authenticate(user, false);
			sendUserPasswordEmail(user.getEmail(), PW_MODE.SET_PW, REGISTRATION_TOKEN_PREFIX+encryptString(session.getSessionToken()));
		}
	}
	
	private String encryptString(String s)
	{
		String stackEncryptionKey = StackConfiguration.getEncryptionKey();
		if (stackEncryptionKey == null || stackEncryptionKey.length() == 0)
			throw new RuntimeException(
					"Expected system property org.sagebionetworks.stackEncryptionKey");
		StringEncrypter se = new StringEncrypter(stackEncryptionKey);
		return se.encrypt(s);
	}

	private String decryptedString(String encryptedS)
	{
		String stackEncryptionKey = StackConfiguration.getEncryptionKey();
		if (stackEncryptionKey == null || stackEncryptionKey.length() == 0)
			throw new RuntimeException(
					"Expected system property org.sagebionetworks.stackEncryptionKey");
		StringEncrypter se = new StringEncrypter(stackEncryptionKey);
		return se.decrypt(encryptedS);
	}	

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/user", method = RequestMethod.GET)
	public @ResponseBody User getUser(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId) throws Exception {
		if (AuthorizationConstants.ANONYMOUS_USER_ID.equals(userId)) 
			throw new AuthenticationException(HttpStatus.BAD_REQUEST.value(), "No user info for "+AuthorizationConstants.ANONYMOUS_USER_ID, null);
		User user = CrowdAuthUtil.getUser(userId);
		return user;
	}
	
	// for integration testing
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = "/user", method = RequestMethod.DELETE)
	public void deleteUser(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId) throws Exception {
		String itu = getIntegrationTestUser();
		boolean isITU = (itu!=null && userId!=null && userId.equals(itu));
		if (!isITU) throw new AuthenticationException(HttpStatus.BAD_REQUEST.value(), "Not allowed outside of integration testing.", null);
		CrowdAuthUtil.deleteUser(userId);
	}
	
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = "/user", method = RequestMethod.PUT)
	public void updateUser(@RequestBody User user,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId) throws Exception {
		if (user.getEmail()==null) user.setEmail(userId);

		if (userId==null) 
				throw new AuthenticationException(HttpStatus.BAD_REQUEST.value(), "Not authorized.", null);
		if (!userId.equals(user.getEmail())) 
				throw new AuthenticationException(HttpStatus.BAD_REQUEST.value(), "Changing email address is not permitted.", null);
		
		CrowdAuthUtil.updateUser(user);
	}
	
	static enum PW_MODE {
		SET_PW,
		RESET_PW,
		SET_API_PW
	}
	

	// reset == true means send the 'reset' message; reset== false means send the 'set' message
	private static void sendUserPasswordEmail(String userEmail, PW_MODE mode) throws Exception {
		// need a session token
		User user = new User();
		user.setEmail(userEmail);
		Session session = CrowdAuthUtil.authenticate(user, false);
		
		sendUserPasswordEmail(userEmail, mode, session.getSessionToken());
	}
	
	// reset == true means send the 'reset' message; reset== false means send the 'set' message
	private static void sendUserPasswordEmail(String userEmail, PW_MODE mode, String sessiontoken) throws Exception {
		// need a session token
		User user = new User();
		user.setEmail(userEmail);
		Session session = CrowdAuthUtil.authenticate(user, false);
		// need the rest of the user's fields
		user = CrowdAuthUtil.getUser(user.getEmail());
		// now send the reset password email, filling in the user name and session token
		SendMail sendMail = new SendMail();
		switch (mode) {
			case SET_PW:
				sendMail.sendSetPasswordMail(user, sessiontoken);
				break;
			case RESET_PW:
				sendMail.sendResetPasswordMail(user, sessiontoken);
				break;
			case SET_API_PW:
				sendMail.sendSetAPIPasswordMail(user, sessiontoken);
				break;
		}
	}
	
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = "/userPasswordEmail", method = RequestMethod.POST)
	public void sendChangePasswordEmail(@RequestBody User user) throws Exception {
		sendUserPasswordEmail(user.getEmail(), PW_MODE.RESET_PW);
	}
	
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = "/apiPasswordEmail", method = RequestMethod.POST)
	public void sendSetAPIPasswordEmail(@RequestBody User user) throws Exception {
		sendUserPasswordEmail(user.getEmail(), PW_MODE.SET_API_PW);
	}
	
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = "/userPassword", method = RequestMethod.POST)
	public void setPassword(@RequestBody User user,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId) throws Exception {
		if ((userId==null || !userId.equals(user.getEmail()))) 
			throw new AuthenticationException(HttpStatus.BAD_REQUEST.value(), "Not authorized.", null);
		if (user.getPassword()==null) 			
			throw new AuthenticationException(HttpStatus.BAD_REQUEST.value(), "New password is required.", null);

		CrowdAuthUtil.updatePassword(user);
	}
	

	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = "/registeringUserPassword", method = RequestMethod.POST)
	public void setRegisteringUserPassword(@RequestBody RegistrationInfo registrationInfo,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId) throws Exception {
		String registrationToken = registrationInfo.getRegistrationToken();
		if (registrationToken==null) 
			throw new AuthenticationException(HttpStatus.BAD_REQUEST.value(), "Missing registration token.", null);
		
		String sessionToken  = decryptedString(registrationToken.substring(REGISTRATION_TOKEN_PREFIX.length()));
		String realUserId = CrowdAuthUtil.revalidate(sessionToken);
		if (realUserId==null) 
			throw new AuthenticationException(HttpStatus.BAD_REQUEST.value(), "Not authorized.", null);
		User user = new User();
		user.setEmail(realUserId);
		user.setPassword(registrationInfo.getPassword());
		CrowdAuthUtil.updatePassword(user);
		CrowdAuthUtil.deauthenticate(sessionToken);
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/secretKey", method = RequestMethod.GET)
	public @ResponseBody SecretKey newSecretKey(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId) throws Exception {
		if (userId==null) 
			throw new AuthenticationException(HttpStatus.BAD_REQUEST.value(), "Not authorized.", null);

		Map<String,Collection<String>> userAttributes = 
			 new HashMap<String,Collection<String>>(CrowdAuthUtil.getUserAttributes(userId));
		Collection<String> secretKeyCollection = userAttributes.get(AuthorizationConstants.CROWD_SECRET_KEY_ATTRIBUTE);
		String secretKey = null;
		// if there is no key, then make one
		if (secretKeyCollection==null || secretKeyCollection.isEmpty()) {
			secretKey = HMACUtils.newHMACSHA1Key();
			secretKeyCollection = new HashSet<String>();
			secretKeyCollection.add(secretKey);
			userAttributes.put(AuthorizationConstants.CROWD_SECRET_KEY_ATTRIBUTE, secretKeyCollection);
			CrowdAuthUtil.setUserAttributes(userId, userAttributes);
		} else {
			// else return the current one
			secretKey = secretKeyCollection.iterator().next();
		}
		return new SecretKey(secretKey);
	}
	

	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = "/secretKey", method = RequestMethod.DELETE)
	public void invalidateSecretKey(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId) throws Exception {
		if (userId==null) 
			throw new AuthenticationException(HttpStatus.BAD_REQUEST.value(), "Not authorized.", null);

		Map<String,Collection<String>> userAttributes = 
			 new HashMap<String,Collection<String>>(CrowdAuthUtil.getUserAttributes(userId));
		Collection<String> secretKeyCollection = userAttributes.get(AuthorizationConstants.CROWD_SECRET_KEY_ATTRIBUTE);
		if (secretKeyCollection!=null && !secretKeyCollection.isEmpty()) {
			// then overwrite the data with an empty set
			secretKeyCollection = new HashSet<String>();
			userAttributes.put(AuthorizationConstants.CROWD_SECRET_KEY_ATTRIBUTE, secretKeyCollection);
			CrowdAuthUtil.setUserAttributes(userId, userAttributes);
		}
	}
	
	

}


