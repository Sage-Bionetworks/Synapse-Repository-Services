package org.sagebionetworks.auth;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.openid4java.consumer.ConsumerManager;
import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.authutil.AuthenticationException;
import org.sagebionetworks.authutil.CrowdAuthUtil;
import org.sagebionetworks.authutil.ModParamHttpServletRequest;
import org.sagebionetworks.authutil.SendMail;
import org.sagebionetworks.authutil.Session;
import org.sagebionetworks.authutil.User;
import org.sagebionetworks.repo.web.NotFoundException;
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
//	private String adminPW;
	
	private CrowdAuthUtil crowdAuthUtil = new CrowdAuthUtil();
	private SendMail sendMail = new SendMail();
	
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
        // optional, only used for testing
        props = new Properties();
        InputStream is = AuthenticationController.class.getClassLoader().getResourceAsStream("authenticationcontroller.properties");
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
			return crowdAuthUtil.authenticate(credentials, true);
		} catch (AuthenticationException ae) {
			// include the URL used to authenticate
			ae.setAuthURL(request.getRequestURL().toString());
			throw ae;
		}
	}
	
	private static final String OPEN_ID_URI = "/openid";
	
	private static final String RETURN_TO_URI = "/openidcallback";
	
	private static final String OPEN_ID_PROVIDER = "OPEN_ID_PROVIDER";
	// 		e.g. https://www.google.com/accounts/o8/id
	
	private static final String MANAGER_KEY = AuthenticationController.class.getName()+".MANAGER_KEY";
	
	// this is the parameter name for the value of the final redirect
	private static final String RETURN_TO_URL_PARAM = "RETURN_TO_URL";
	
	// this is the key for the value of the final redirect
	private static final String RETURN_TO_URL_KEY = AuthenticationController.class.getName()+".RETURN_TO_URL_KEY";
		
	private static final String OPEN_ID_ATTRIBUTE = "OPENID";
	
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = OPEN_ID_URI, method = RequestMethod.POST)
	public String openID(
			@RequestParam(value = OPEN_ID_PROVIDER, required = true) String openIdProvider,
			@RequestParam(value = RETURN_TO_URL_PARAM, required = true) String returnToURL,
              HttpServletRequest request,
              HttpServletResponse response) throws Exception {

		HttpServlet servlet = null;
		
		request.getSession().setAttribute(RETURN_TO_URL_KEY, returnToURL);
		
		ConsumerManager manager = new ConsumerManager();
		request.getSession().setAttribute(MANAGER_KEY, manager);
		SampleConsumer sampleConsumer = new SampleConsumer(manager);
		
		String thisUrl = request.getRequestURL().toString();
		int i = thisUrl.indexOf(OPEN_ID_URI);
		if (i<0) throw new RuntimeException("Current URI, "+OPEN_ID_URI+", not found in "+thisUrl);
		String returnToUrl = thisUrl.substring(0, i)+RETURN_TO_URI;

		return sampleConsumer.authRequest(openIdProvider, returnToUrl, servlet, request, response);
	}

	private static String dumpParamsArray(Map<String,String[]> p, String prefix) {
		StringBuilder sb = new StringBuilder();
		for (String s : p.keySet()) {
			sb.append(prefix+s+" -> "+Arrays.asList(p.get(s))+"\n");
		}
		return sb.toString();
	}
	
	private static String dumpParamsList(Map<String,List<String>> p, String prefix) {
		StringBuilder sb = new StringBuilder();
		for (String s : p.keySet()) {
			sb.append(prefix+s+" -> "+p.get(s)+"\n");
		}
		return sb.toString();
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = RETURN_TO_URI, method = RequestMethod.GET)
	public @ResponseBody
	void openIDCallback(
			HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		try {
			
//			System.out.println(
//					"Request Params:\n"+
//					dumpParamsArray(request.getParameterMap(), "\t")
//					
//			);
			HttpSession session = request.getSession();
			ConsumerManager manager = (ConsumerManager)session.getAttribute(MANAGER_KEY);
			if (manager==null) throw new NullPointerException();
			
			session.removeAttribute(MANAGER_KEY);
			
			SampleConsumer sampleConsumer = new SampleConsumer(manager);
			
			OpenIDInfo openIDInfo = sampleConsumer.verifyResponse(request);
			String openID = openIDInfo.getIdentifier();
			
//			System.out.println("Identity: "+openID);
			
//			System.out.println("OpenIDInfo:\n"+dumpParamsList(openIDInfo.getMap(), "\t"));
			
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
				attrs = new HashMap<String,Collection<String>>(crowdAuthUtil.getUserAttributes(email));
			} catch (NotFoundException nfe) {
				// user doesn't exist yet, so create them
				credentials.setPassword((new Long(rand.nextLong())).toString());
				credentials.setFirstName(fname);
				credentials.setLastName(lname);
				if (fname!=null && lname!=null) credentials.setDisplayName(fname+" "+lname);
				crowdAuthUtil.createUser(credentials);
				attrs = new HashMap<String,Collection<String>>(crowdAuthUtil.getUserAttributes(email));
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

			crowdAuthUtil.setUserAttributes(email, attrs);
			
			Session crowdSession = crowdAuthUtil.authenticate(credentials, false);
			// get the SSO token 
//			return session;
						
			// instead of returning, redirect
			String redirectUrl = request.getSession().getAttribute(RETURN_TO_URL_KEY)+":"+
				crowdSession.getSessionToken()+":"+
				crowdSession.getDisplayName();
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
		boolean isITU = (itu!=null && user.getEmail().equals(itu));
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
		boolean isITU = (itu!=null && user.getEmail().equals(itu));
		if (!isITU) throw new AuthenticationException(HttpStatus.BAD_REQUEST.value(), "Not allowed outside of integration testing.", null);
		crowdAuthUtil.deleteUser(user);
//		mirrorToPersistenceLayer();
	}
	
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = "/user", method = RequestMethod.PUT)
	public void updateUser(@RequestBody User user,
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId) throws Exception {
		String itu = getIntegrationTestUser();
		boolean isITU = (itu!=null && user.getEmail().equals(itu));
		if (!isITU) {
			user.setPassword(null);
		}
		if (!isITU && (userId==null || !userId.equals(user.getEmail()))) 
			throw new AuthenticationException(HttpStatus.BAD_REQUEST.value(), "Not authorized.", null);
		crowdAuthUtil.updateUser(user);
	}
	
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = "/userPasswordEmail", method = RequestMethod.POST)
	public void sendChangePasswordEmail(@RequestBody User user) throws Exception {
			crowdAuthUtil.sendResetPWEmail(user); // << will no longer be used (sends email via Crowd)
			//sendMail.sendResetPasswordMail(user); << the new way
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


