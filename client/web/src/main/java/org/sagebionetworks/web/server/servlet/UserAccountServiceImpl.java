package org.sagebionetworks.web.server.servlet;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;

import org.sagebionetworks.web.client.UserAccountService;
import org.sagebionetworks.web.client.security.AuthenticationException;
import org.sagebionetworks.web.server.RestTemplateProvider;
import org.sagebionetworks.web.shared.exceptions.RestServiceException;
import org.sagebionetworks.web.shared.users.UserData;
import org.sagebionetworks.web.shared.users.UserLogin;
import org.sagebionetworks.web.shared.users.UserRegistration;
import org.sagebionetworks.web.shared.users.UserSession;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.inject.Inject;

public class UserAccountServiceImpl extends RemoteServiceServlet implements UserAccountService {

	private static Logger logger = Logger.getLogger(UserAccountServiceImpl.class.getName());

	private static final String SEND_PASSWORD_CHANGE_PATH = "userPasswordEmail";

	private static final String INITIATE_SESSION_PATH = "session";
	
	private static final String CREATE_USER_PATH = "user";

	private static final String TERMINATE_SESSION_PATH = "session";

	
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
		HttpEntity<String> entity = new HttpEntity<String>(userId, headers);

		// Make the actual call.
		long start = System.currentTimeMillis();		
		try {
			ResponseEntity<Void> response = templateProvider.getTemplate().exchange(uri, HttpMethod.POST, entity, Void.class);
		} catch (RestClientException ex) {
			// ignore these. template provider can not handle a no content responses
		} catch (Exception ex) {
			throw new RestServiceException("Send password change request failed");
		}
		long end = System.currentTimeMillis();
		logger.info("Url GET: " + uri.toString()+" in "+(end-start)+" ms");
				
	}

	@Override
	public UserData initiateSession(String username, String password) throws AuthenticationException {
		URI uri = null;
		try {
			uri = new URI(urlProvider.getAuthBaseUrl() + INITIATE_SESSION_PATH);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<UserLogin> entity = new HttpEntity<UserLogin>(new UserLogin(username, password), headers);

		// Make the actual call.
		long start = System.currentTimeMillis();
		ResponseEntity<UserSession> response = null;
		try {
			response = templateProvider.getTemplate().exchange(uri, HttpMethod.POST, entity, UserSession.class);
		} catch (HttpClientErrorException ex) {
			throw new AuthenticationException("Unable to authenticate.");
		}
		long end = System.currentTimeMillis();
		logger.info("Url GET: " + uri.toString()+" in "+(end-start)+" ms");
		
		UserData userData = null;		
		if((response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK) && response.hasBody()) {
			UserSession initSession = response.getBody();
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
		HttpEntity<UserRegistration> entity = new HttpEntity<UserRegistration>(userInfo, headers);

		// Make the actual call.
		long start = System.currentTimeMillis();
		
		try {
			ResponseEntity<Void> response = templateProvider.getTemplate().exchange(uri, HttpMethod.POST, entity, Void.class);
			if((response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK) && response.hasBody()) {
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

	@Override
	public void terminateSession(String sessionToken) throws RestServiceException {
		URI uri = null;
		try {
			uri = new URI(urlProvider.getAuthBaseUrl() + TERMINATE_SESSION_PATH);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<String>(sessionToken, headers);

		// Make the actual call.
		long start = System.currentTimeMillis();		
		try {
			ResponseEntity<Object> response = templateProvider.getTemplate().exchange(uri.toString(), HttpMethod.DELETE, entity, Object.class);
			if(response.getStatusCode() != HttpStatus.NO_CONTENT && response.hasBody()) {
				throw new RestServiceException("Unable to terminate session. Please try again.");
			}
		} catch (RestClientException ex) {
			// ignore these. template provider can not handle a no content responses
		} catch (Exception ex) {
			throw new RestServiceException("Logout failed. Please try again.");
		}
		
		long end = System.currentTimeMillis();
		logger.info("Url GET: " + uri.toString()+" in "+(end-start)+" ms");		
	}

	/**
	 * Validate that the service is ready to go. If any of the injected data is
	 * missing then it cannot run. Public for tests.
	 */
	public void validateService() {
		if (templateProvider == null)
			throw new IllegalStateException(
					"The org.sagebionetworks.web.server.RestTemplateProvider was not injected into this service");
		if (templateProvider.getTemplate() == null)
			throw new IllegalStateException(
					"The org.sagebionetworks.web.server.RestTemplateProvider returned a null template");
		if (urlProvider == null)
			throw new IllegalStateException(
					"The org.sagebionetworks.rest.api.root.url was not set");
	}

}

