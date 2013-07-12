package org.sagebionetworks.auth;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.auth.services.AuthenticationService;
import org.sagebionetworks.authutil.AuthenticationException;
import org.sagebionetworks.authutil.CrowdAuthUtil;
import org.sagebionetworks.authutil.CrowdAuthUtil.PW_MODE;
import org.sagebionetworks.authutil.Session;
import org.sagebionetworks.authutil.User;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ChangeUserPassword;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.auth.RegistrationInfo;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.sagebionetworks.securitytools.HMACUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerInfo(displayName="Authentication Services", path="auth/v1")
@Controller
public class AuthenticationController extends BaseController {	
	@Autowired
	AuthenticationService authenticationService;

	private Random rand = new Random();
	
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
				if (!CrowdAuthUtil.acceptsTermsOfUse(credentials.getEmail(), credentials.isAcceptsTermsOfUse()))
					throw new UnauthorizedException(ServiceConstants.TERMS_OF_USE_ERROR_MESSAGE);
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
		if (!CrowdAuthUtil.acceptsTermsOfUse(userId, false /*i.e. may have accepted TOU previously, but acceptance is not given in this request*/)) {
			throw new UnauthorizedException(ServiceConstants.TERMS_OF_USE_ERROR_MESSAGE);
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
			CrowdAuthUtil.sendUserPasswordEmail(user.getEmail(), PW_MODE.SET_PW, CrowdAuthUtil.REGISTRATION_TOKEN_PREFIX+session.getSessionToken());
		}
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
	

	// reset == true means send the 'reset' message; reset== false means send the 'set' message
	private static void sendUserPasswordEmail(String userEmail, PW_MODE mode) throws Exception {
		// need a session token
		User user = new User();
		user.setEmail(userEmail);
		Session session = CrowdAuthUtil.authenticate(user, false);
		
		CrowdAuthUtil.sendUserPasswordEmail(userEmail, mode, session.getSessionToken());
	}
	
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = "/userPasswordEmail", method = RequestMethod.POST)
	public void sendChangePasswordEmail(@RequestBody User user) throws Exception {
		sendUserPasswordEmail(user.getEmail(), PW_MODE.RESET_PW);
	}
	
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = "/apiPasswordEmail", method = RequestMethod.POST)
	public void sendSetAPIPasswordEmail(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId) throws Exception {
		if (userId == null)
			throw new AuthenticationException(HttpStatus.BAD_REQUEST.value(), "Not authorized.", null);
			
		sendUserPasswordEmail(userId, PW_MODE.SET_API_PW);
	}
	
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = "/userPassword", method = RequestMethod.POST)
	public void setPassword(@RequestBody ChangeUserPassword changeUserPassword,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId) throws Exception {
		if (userId==null) 
			throw new AuthenticationException(HttpStatus.BAD_REQUEST.value(), "Not authorized.", null);
		if (changeUserPassword.getNewPassword()==null) 			
			throw new AuthenticationException(HttpStatus.BAD_REQUEST.value(), "New password is required.", null);
		User user = new User();
		user.setEmail(userId);
		user.setPassword(changeUserPassword.getNewPassword());
		CrowdAuthUtil.updatePassword(user);
	}
	
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = "/changeEmail", method = RequestMethod.POST)
	public void changeEmail(@RequestBody RegistrationInfo registrationInfo,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId) throws Exception {
		//user must be logged in to make this request
		if (userId==null) 
			throw new AuthenticationException(HttpStatus.BAD_REQUEST.value(), "Not authorized.", null);
		String registrationToken = registrationInfo.getRegistrationToken();
		if (registrationToken==null) 
			throw new AuthenticationException(HttpStatus.BAD_REQUEST.value(), "Missing registration token.", null);
		
		String sessionToken  = registrationToken.substring(CrowdAuthUtil.CHANGE_EMAIL_TOKEN_PREFIX.length());
		String realUserId = CrowdAuthUtil.revalidate(sessionToken);
		if (realUserId==null) 
			throw new AuthenticationException(HttpStatus.BAD_REQUEST.value(), "Not authorized.", null);
		//set the password
		User user = new User();
		user.setEmail(realUserId);
		user.setPassword(registrationInfo.getPassword());
		CrowdAuthUtil.updatePassword(user);
		
		//and update the preexisting user to the new email address
		authenticationService.updateEmail(userId, realUserId);
		CrowdAuthUtil.deauthenticate(sessionToken);
	}
	

	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = "/registeringUserPassword", method = RequestMethod.POST)
	public void setRegisteringUserPassword(@RequestBody RegistrationInfo registrationInfo,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId) throws Exception {
		String registrationToken = registrationInfo.getRegistrationToken();
		if (registrationToken==null) 
			throw new AuthenticationException(HttpStatus.BAD_REQUEST.value(), "Missing registration token.", null);
		
		String sessionToken  = registrationToken.substring(CrowdAuthUtil.REGISTRATION_TOKEN_PREFIX.length());
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


