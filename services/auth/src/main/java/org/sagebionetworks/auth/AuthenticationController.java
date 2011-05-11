package org.sagebionetworks.auth;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.authutil.AuthenticationException;
import org.sagebionetworks.authutil.CrowdAuthUtil;
import org.sagebionetworks.authutil.Session;
import org.sagebionetworks.authutil.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;


@Controller
public class AuthenticationController {
	private static final Logger log = Logger.getLogger(AuthenticationController.class
			.getName());
	
//	private String repositoryServicesURL;  // this is to be discontinued
	private String adminPW;
	
	private CrowdAuthUtil crowdAuthUtil = new CrowdAuthUtil();
	
//	   a special userId that's used for integration testing
//	   we need a way to specify a 'back door' userId for integration testing
//	   the authentication servlet
//	   this should not be present in the production deployment
//	   The behavior is as follows
//	  	If passed to the user creation service, there is no confirmation email generated.
//	  	Instead the userId becomes the password.
	private String integrationTestUser = null;

	
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
        Properties props = new Properties();
        InputStream is = AuthenticationController.class.getClassLoader().getResourceAsStream("mirrorservice.properties");
        try {
        	props.load(is);
        } catch (IOException e) {
        	throw new RuntimeException(e);
        }
//        repositoryServicesURL = props.getProperty("repositoryServicesURL");
        adminPW=props.getProperty("adminPW");
        try {
	        is.close();
	    } catch (IOException e) {
	    	throw new RuntimeException(e);
	    }
        // optional, only used for testing
        props = new Properties();
        is = AuthenticationController.class.getClassLoader().getResourceAsStream("authenticationcontroller.properties");
        if (is!=null) {
	        try {
	        	props.load(is);
	        } catch (IOException e) {
	        	throw new RuntimeException(e);
	        }
	        setIntegrationTestUser(props.getProperty("integrationTestUser"));
        }
	}

	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = "/session", method = RequestMethod.POST)
	public @ResponseBody
	Session authenticate(@RequestBody User credentials,
			HttpServletRequest request) throws Exception {
		try { 
			return crowdAuthUtil.authenticate(credentials);
		} catch (AuthenticationException ae) {
			// include the URL used to authenticate
			ae.setAuthURL(request.getRequestURL().toString());
			throw ae;
		}
	}

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/session", method = RequestMethod.PUT)
	public @ResponseBody
	void revalidate(@RequestBody Session session) throws Exception {
			crowdAuthUtil.revalidate(session.getSessionToken());
	}

	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = "/session", method = RequestMethod.DELETE)
	public void deauthenticate(@RequestBody Session session) throws Exception {
			crowdAuthUtil.deauthenticate(session.getSessionToken());
	}
	
	private Random rand = new Random();
	
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = "/user", method = RequestMethod.POST)
	public void createUser(@RequestBody User user) throws Exception {
		String itu = getIntegrationTestUser();
		boolean isITU = (itu!=null && user.getUserId().equals(itu));
		if (!isITU) {
			user.setPassword(""+rand.nextLong());
		}
		crowdAuthUtil.createUser(user);
		if (!isITU) {
			crowdAuthUtil.sendResetPWEmail(user);
		}
//		mirrorToPersistenceLayer();
	}
	
//	// call repo' services to perform mirror
//	private void mirrorToPersistenceLayer() throws Exception {
//		// log-in as admin and get session token
//		User adminCredentials = new User();
//		adminCredentials.setUserId(AuthUtilConstants.ADMIN_USER_ID);
//		adminCredentials.setPassword(adminPW);
//		Session adminSession = crowdAuthUtil.authenticate(adminCredentials);
//		// execute mirror 
//		byte[] sessionXML = null;
//		int rc = 0;
//		{
//			URL url = new URL(repositoryServicesURL+"/userMirror");
//			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
//			conn.setRequestMethod("POST");
//			conn.setRequestProperty("Accept", "application/json");
//			conn.setRequestProperty("Content-Type", "application/json");
//			conn.setRequestProperty("sessionToken", adminSession.getSessionToken());
//			CrowdAuthUtil.setBody(conn, "{}\n");
//			try {
//				rc = conn.getResponseCode();
//				// unfortunately the next line throws an IOException when there is no content
////				InputStream is = (InputStream)conn.getContent();
////				if (is!=null) sessionXML = (CrowdAuthUtil.readInputStream(is)).getBytes();
//			} catch (IOException e) {
//				InputStream is = (InputStream)conn.getErrorStream();
//				if (is!=null) sessionXML = (CrowdAuthUtil.readInputStream(is)).getBytes();
//				throw new Exception(url+"\nsessionToken: "+adminSession.getSessionToken()+"\nrc: "+rc+"\n"+
//						(sessionXML==null?null:new String(sessionXML)));
//			}
//			if (rc!=HttpStatus.CREATED.value()) throw new Exception("Failed to mirror users.  status="+rc);
//		}
//		// log-out
//		crowdAuthUtil.deauthenticate(adminSession.getSessionToken());
//	}
	
	// for integration testing
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = "/user", method = RequestMethod.DELETE)
	public void deleteUser(@RequestBody User user) throws Exception {
		String itu = getIntegrationTestUser();
		boolean isITU = (itu!=null && user.getUserId().equals(itu));
		if (!isITU) throw new AuthenticationException(HttpStatus.BAD_REQUEST.value(), "Not allowed outside of integration testing.", null);
		crowdAuthUtil.deleteUser(user);
//		mirrorToPersistenceLayer();
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/user", method = RequestMethod.PUT)
	public void updateUser(@RequestBody User user,
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId) throws Exception {
		String itu = getIntegrationTestUser();
		boolean isITU = (itu!=null && user.getUserId().equals(itu));
		if (!isITU) {
			user.setPassword(null);
		}
		if (!isITU && (userId==null || !userId.equals(user.getUserId()))) 
			throw new AuthenticationException(HttpStatus.BAD_REQUEST.value(), "Not authorized.", null);
		crowdAuthUtil.updateUser(user);
	}
	
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = "/userPasswordEmail", method = RequestMethod.POST)
	public void sendChangePasswordEmail(@RequestBody User user) throws Exception {
			crowdAuthUtil.sendResetPWEmail(user);
	}
	
	/**
	 * This is thrown when there are problems authenticating the user
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
		if (null!=ex.getAuthURL()) response.setHeader("AuthenticationURL", ex.getAuthURL());
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


