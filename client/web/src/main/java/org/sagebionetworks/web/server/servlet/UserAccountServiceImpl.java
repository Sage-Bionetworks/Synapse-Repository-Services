package org.sagebionetworks.web.server.servlet;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.sagebionetworks.web.client.UserAccountService;
import org.sagebionetworks.web.client.security.AuthenticationException;
import org.sagebionetworks.web.server.RestTemplateProvider;
import org.sagebionetworks.web.shared.exceptions.RestServiceException;
import org.sagebionetworks.web.shared.users.InitiateSession;
import org.sagebionetworks.web.shared.users.UserData;
import org.sagebionetworks.web.shared.users.UserRegistration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.inject.Inject;

public class UserAccountServiceImpl extends RemoteServiceServlet implements UserAccountService {

	private static Logger logger = Logger.getLogger(UserAccountServiceImpl.class.getName());

	private static final String SEND_PASSWORD_CHANGE_PATH = "userPasswordEmail";
	private static final String SEND_PASSWORD_CHANGE_USER_ID_PARAM = "userId";

	private static final String INITIATE_SESSION_PATH = "session";
	private static final String INITIATE_SESSION_USERID_PARAM = "userId";
	private static final String INITIATE_SESSION_PASSWORD_PARAM = "password";
	
	private static final String CREATE_USER_PATH = "user";
	private static final String CREATE_USER_USERID_PARAM = "userId";
	private static final String CREATE_USER_EMAIL_PARAM = "email";
	private static final String CREATE_USER_FIRSTNAME_PARAM = "firstName";
	private static final String CREATE_USER_LASTNAME_PARAM = "lastName";
	private static final String CREATE_USER_DISPLAYNAME_PARAM = "displayName";
	
	/**
	 * The template is injected with Gin
	 */
	private RestTemplateProvider templateProvider;

	/**
	 * Injected with Gin
	 */
	private ServiceUrlProvider urlProvider;
	

	/**
	 * Injected via Gin.
	 * 
	 * @param template
	 */
	@Inject
	public void setRestTemplate(RestTemplateProvider template) {
		this.templateProvider = template;
	}
	
	/**
	 * Injected vid Gin
	 * @param provider
	 */
	@Inject
	public void setServiceUrlProvider(ServiceUrlProvider provider){
		this.urlProvider = provider;
	}

	@Override
	public void sendPasswordResetEmail(String userId) throws RestServiceException {
		URI uri = null;
		try {
			uri = new URI(urlProvider.getAuthBaseUrl() + SEND_PASSWORD_CHANGE_PATH);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		// Make the actual call.
		long start = System.currentTimeMillis();
		Map<String, String> params = new LinkedHashMap<String, String>();
		params.put(SEND_PASSWORD_CHANGE_USER_ID_PARAM, userId);
		
		try {
			ResponseEntity<Object> response = templateProvider.getTemplate().postForEntity(uri, params, Object.class);
		} catch (RestClientException ex) {
			// ignore these. template provider can not handle a no content responses
		} catch (Exception ex) {
			throw new RestServiceException("Send password change request failed");
		}
		long end = System.currentTimeMillis();
		logger.info("Url GET: " + uri.toString()+" in "+(end-start)+" ms");
				
	}

	@Override
	public UserData authenticateUser(String username, String password) throws AuthenticationException {
		URI uri = null;
		try {
			uri = new URI(urlProvider.getAuthBaseUrl() + INITIATE_SESSION_PATH);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		// Make the actual call.
		long start = System.currentTimeMillis();
		Map<String, String> params = new LinkedHashMap<String, String>();
		params.put(INITIATE_SESSION_USERID_PARAM, username);
		params.put(INITIATE_SESSION_PASSWORD_PARAM, password);
		ResponseEntity<InitiateSession> response = templateProvider.getTemplate().postForEntity(uri, params, InitiateSession.class);
		long end = System.currentTimeMillis();
		logger.info("Url GET: " + uri.toString()+" in "+(end-start)+" ms");
		
		UserData userData = null;		
		if(response.getStatusCode() == HttpStatus.CREATED && response.hasBody()) {
			InitiateSession initSession = response.getBody();
			String displayName = initSession.getDisplayName();
			String sessionToken = initSession.getSessionToken();
			userData = new UserData(username, displayName, sessionToken);
		} else {			
			throw new AuthenticationException("Unable to authenticate.");
		}
		return userData;		
	}

	@Override
	public void createUser(UserRegistration userInfo) throws RestServiceException {
		URI uri = null;
		try {
			uri = new URI(urlProvider.getAuthBaseUrl() + CREATE_USER_PATH);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		// Make the actual call.
		long start = System.currentTimeMillis();
		Map<String, String> params = new LinkedHashMap<String, String>();
		params.put(CREATE_USER_USERID_PARAM, userInfo.getUserId());
		params.put(CREATE_USER_EMAIL_PARAM, userInfo.getEmail());
		params.put(CREATE_USER_FIRSTNAME_PARAM, userInfo.getFirstName());
		params.put(CREATE_USER_LASTNAME_PARAM, userInfo.getLastName());
		params.put(CREATE_USER_DISPLAYNAME_PARAM, userInfo.getDisplayName());
		
		try {
			ResponseEntity<Void> response = templateProvider.getTemplate().postForEntity(uri, params, Void.class);
			if(response.getStatusCode() != HttpStatus.CREATED && response.hasBody()) {
				throw new RestServiceException("Unable to create user. Please try again.");
			}
		} catch (RestClientException ex) {
			// ignore these. template provider can not handle a no content responses
		} catch (Exception ex) {
			throw new RestServiceException("Unable to create user. Please try again.");
		}

		long end = System.currentTimeMillis();
		logger.info("Url GET: " + uri.toString()+" in "+(end-start)+" ms");		
	}

}

