package org.sagebionetworks.auth;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.authutil.AuthenticationException;
import org.sagebionetworks.authutil.CrowdAuthUtil;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * The URIs are:
 *  /resourceAccess   -- CRUD on the access a User has to a resource
 *  /resourceSession  -- CR-D on a time limited session accessing the resource
 * 
 */
@ControllerInfo(displayName="Resource Access Services", path="auth/v1")
@Controller
public class ResourceAccessController {
	private static final Logger log = Logger.getLogger(ResourceAccessController.class
			.getName());
	
	public static final String RESOURCE_ACCESS_URI = "/resourceAccess";
	public static final String RESOURCE_NAME_PATH_VAR = "/{resourceName}";
	public static final String RESOURCE_USER_NAME_PARAM = "resourceUserName";
	public static final String RESOURCE_SESSION_URI = "/resourceSession";
	public static final String RESOURCE_ACCESS_TOKEN_PATH_VAR = "/{resourceAccessToken}";

	
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = RESOURCE_ACCESS_URI+RESOURCE_NAME_PATH_VAR, method = RequestMethod.POST)
	public void createResourceAccess(
			@RequestBody ResourceUserData ra,
			@PathVariable String resourceName,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userName,
			HttpServletRequest request) throws Exception {
		if (null==userName) throw new AuthenticationException(HttpStatus.UNAUTHORIZED.value(), "Not authorized.", null);
		
		if (!CrowdAuthUtil.isAdmin(userName)) {
			throw new AuthenticationException(HttpStatus.UNAUTHORIZED.value(), "Not authorized: "+userName+" is not an administrator.", null);
		}
		
		ResourceAccessManager.createResourceAccess(ra.getUserName(), resourceName, ra.getUserData());
	}
	
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = RESOURCE_ACCESS_URI+RESOURCE_NAME_PATH_VAR, method = RequestMethod.DELETE)
	public void deleteResourceAccess(
			@PathVariable String resourceName,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userName,
			@RequestParam(value = RESOURCE_USER_NAME_PARAM, required = false) String resourceUserName,
			HttpServletRequest request) throws Exception {
		if (null==userName) throw new AuthenticationException(HttpStatus.UNAUTHORIZED.value(), "Not authorized.", null);
		
		if (!CrowdAuthUtil.isAdmin(userName)) {
			throw new AuthenticationException(HttpStatus.UNAUTHORIZED.value(), "Not authorized: "+userName+" is not an administrator.", null);
		}
		
		ResourceAccessManager.deleteResourceAccess(resourceUserName, resourceName);
	}
	
	// TODO: READ, UPDATE ResourceAccess
	
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = RESOURCE_SESSION_URI+RESOURCE_NAME_PATH_VAR, method = RequestMethod.POST)
	public @ResponseBody
	ResourceAccessToken createResourceSession(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userName,
			@PathVariable String resourceName,
			HttpServletRequest request) throws Exception {
		if (null==userName) throw new AuthenticationException(HttpStatus.UNAUTHORIZED.value(), "Not authorized.", null);

		String sessionToken = request.getHeader(AuthorizationConstants.SESSION_TOKEN_PARAM);
		if (sessionToken==null) {
			// if there is no sessionToken but there IS a user name, 
			// then the request was authenticated by an HMAC-SHA1 signature
			// in this case we need to create a session token
			sessionToken = ResourceAccessManager.getSessionTokenFromUserName(userName);
		}

		// check resource authorization
		try {
			ResourceAccessManager.readResourceAccess(userName, resourceName);
		} catch (NotFoundException nfe) {
			throw new AuthenticationException(HttpStatus.UNAUTHORIZED.value(), "Not authorized.", null);
		}

		String token = ResourceAccessManager.createResourceAccessToken(sessionToken, resourceName);
		ResourceAccessToken resourceAccessToken = new ResourceAccessToken();
		resourceAccessToken.setResourceAccessToken(token);
		return resourceAccessToken;
	}
	

	/**
	 * Note: 'userName' is the name of the user making the request.
	 * 'resourceUserName' is the user referred to by the resource-access token.
	 * These two users need not be the same, i.e. one user (e.g. a service account)
	 * can execute the request on behalf of another user (the actual resource user).
	 * 
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = RESOURCE_SESSION_URI+RESOURCE_ACCESS_TOKEN_PATH_VAR, method = RequestMethod.GET)
	public @ResponseBody
	ResourceUserData getResourceSession(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false)  String userName,
			@PathVariable String resourceAccessToken,
			HttpServletRequest request) throws Exception {
		if (null==userName) throw new AuthenticationException(HttpStatus.UNAUTHORIZED.value(), "Not authorized.", null);
		
		String sessionToken = null;
		String resourceName = null;
		try {
			sessionToken = ResourceAccessManager.extractSessionToken(resourceAccessToken);
			resourceName = ResourceAccessManager.extractResourceName(resourceAccessToken);
		} catch (IllegalArgumentException e) {
			throw new AuthenticationException(HttpStatus.UNAUTHORIZED.value(), "Not authorized.", e);
		}
		
		// validate sessionToken (throw authentication exception if token isn't valid)
		String resourceUserName = ResourceAccessManager.getUserNameFromSessionToken(sessionToken);

		// check resource authorization
		String userData = null;
		try {
			userData = ResourceAccessManager.readResourceAccess(resourceUserName, resourceName);
		} catch (NotFoundException nfe) {
			throw new AuthenticationException(HttpStatus.UNAUTHORIZED.value(), "Not authorized.", null);
		}
		ResourceUserData resourceUserData = new ResourceUserData();
		resourceUserData.setUserName(resourceUserName);
		resourceUserData.setUserData(userData);
		return resourceUserData;
	}
	
	/**
	 * 
	 * @param ex
	 *            the exception to be handled
	 * @param request
	 *            the client request
	 * @return an ErrorResponse object containing the exception reason or some
	 *         other human-readable response
	 */
	@ExceptionHandler(AuthenticationException.class)
	public @ResponseBody
	ErrorResponse handleAuthenticationException(AuthenticationException ex,
			HttpServletRequest request,
			HttpServletResponse response) {
		response.setStatus(ex.getRespStatus());
		return handleException(ex, request);
	}


	/**
	 * Handle any exceptions not handled by specific handlers. Log an additional
	 * message with higher severity because we really do want to know what sorts
	 * of new exceptions are occurring.
	 * 
	 * @param ex
	 *            the exception to be handled
	 * @param request
	 *            the client request
	 * @return an ErrorResponse object containing the exception reason or some
	 *         other human-readable response
	 */
	@ExceptionHandler(Exception.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public @ResponseBody
	ErrorResponse handleAllOtherExceptions(Exception ex,
			HttpServletRequest request) {
		log.log(Level.SEVERE,
				"Consider specifically handling exceptions of type "
						+ ex.getClass().getName());
		return handleException(ex, request);
	}
	
	/**
	 * Log the exception at the warning level and return an ErrorResponse
	 * object. Child classes should override this method if they want to change
	 * the behavior for all exceptions.
	 * 
	 * @param ex
	 *            the exception to be handled
	 * @param request
	 *            the client request
	 * @return an ErrorResponse object containing the exception reason or some
	 *         other human-readable response
	 */
	protected ErrorResponse handleException(Throwable ex,
			HttpServletRequest request) {
		log.log(Level.WARNING, "Handling " + request.toString(), ex);
		return new ErrorResponse(ex.getMessage());
	}


}
