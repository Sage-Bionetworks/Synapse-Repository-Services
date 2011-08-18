package org.sagebionetworks.web.server.servlet;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.sagebionetworks.web.client.UserAccountService;
import org.sagebionetworks.web.client.security.AuthenticationException;
import org.sagebionetworks.web.server.RestTemplateProvider;
import org.sagebionetworks.web.shared.exceptions.RestServiceException;
import org.sagebionetworks.web.shared.users.AclPrincipal;
import org.sagebionetworks.web.shared.users.GetUser;
import org.sagebionetworks.web.shared.users.UserData;
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
import com.google.gwt.user.server.rpc.UnexpectedException;
import com.google.inject.Inject;

public class UserAccountServiceImpl extends RemoteServiceServlet implements UserAccountService {

	private static Logger logger = Logger.getLogger(UserAccountServiceImpl.class.getName());
			
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
		// First make sure the service is ready to go.
		validateService();
		
		JSONObject obj = new JSONObject();
		try {
			obj.put("email", userId);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		// Build up the path
		String url = urlProvider.getAuthBaseUrl() + "/" + ServiceUtils.AUTHSVC_SEND_PASSWORD_CHANGE_PATH;
		String jsonString = obj.toString();
		
		// Setup the header
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<String>(jsonString, headers);
		HttpMethod method = HttpMethod.POST;
		
		logger.info(method.toString() + ": " + url + ", JSON: " + jsonString);
		
		// Make the actual call.
		try {
			@SuppressWarnings("unused")
			ResponseEntity<String> response = templateProvider.getTemplate().exchange(url, method, entity, String.class);
			if(response.getBody().equals("")) {
				return;
			}
		} catch (UnexpectedException ex) {
			return;
		} catch (NullPointerException nex) {
			// TODO : change this to properly deal with a 204!!!
			return; // this is expected
		}
		
		throw new RestClientException("An error occured. Please try again.");
		
//		if (response.getStatusCode() != HttpStatus.CREATED && response.getStatusCode() != HttpStatus.OK) {
//			throw new RestClientException("Status code:" + response.getStatusCode().value());
//		}						
	}

	public void sendSetApiPasswordEmail(String emailAddress) throws RestServiceException {
		// First make sure the service is ready to go.
		validateService();
		
		JSONObject obj = new JSONObject();
		try {
			obj.put("email", emailAddress);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		// Build up the path
		String url = urlProvider.getAuthBaseUrl() + "/" + ServiceUtils.AUTHSVC_SEND_API_PASSWORD_PATH;
		String jsonString = obj.toString();
		
		// Setup the header
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<String>(jsonString, headers);
		HttpMethod method = HttpMethod.POST;
		
		logger.info(method.toString() + ": " + url + ", JSON: " + jsonString);
		
		// Make the actual call.
		try {
			@SuppressWarnings("unused")
			ResponseEntity<String> response = templateProvider.getTemplate().exchange(url, method, entity, String.class);
			if(response.getBody().equals("")) {
				return;
			}
		} catch (UnexpectedException ex) {
			return;
		} catch (NullPointerException nex) {
			// TODO : change this to properly deal with a 204!!!
			return; // this is expected
		}
		
		throw new RestClientException("An error occured. Please try again.");		
	}

	@Override
	public void setPassword(String email, String newPassword) {
		// First make sure the service is ready to go.
		validateService();
		
		JSONObject obj = new JSONObject();
		try {
			obj.put("email", email);
			obj.put("password", newPassword);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		// Build up the path
		String url = urlProvider.getAuthBaseUrl() + "/" + ServiceUtils.AUTHSVC_SET_PASSWORD_PATH;
		String jsonString = obj.toString();
		
		// Setup the header
		HttpHeaders headers = new HttpHeaders();
		// If the user data is stored in a cookie, then fetch it and the session token to the header.
		UserDataProvider.addUserDataToHeader(this.getThreadLocalRequest(), headers);
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<String>(jsonString, headers);
		HttpMethod method = HttpMethod.POST;
		
		// NOTE: do not log the JSON as it includes the user's new clear text password!
		logger.info(method.toString() + ": " + url + ", for user " + email); 
		
		// Make the actual call.
		try {
			ResponseEntity<String> response = templateProvider.getTemplate().exchange(url, method, entity, String.class);
			if(response.getBody().equals("")) {
				return;
			}
		} catch (UnexpectedException ex) {
			return;
		} catch (NullPointerException nex) {
			// TODO : change this to properly deal with a 204!!!
			return; // this is expected
		}
		
		throw new RestClientException("An error occured. Please try again.");
	}

	
	@Override
	public UserData initiateSession(String username, String password) throws AuthenticationException {
		// First make sure the service is ready to go.
		validateService();
		
		JSONObject obj = new JSONObject();
		try {
			obj.put("email", username);
			obj.put("password", password);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		// Build up the path
		String url = urlProvider.getAuthBaseUrl() + "/" + ServiceUtils.AUTHSVC_INITIATE_SESSION_PATH;
		String jsonString = obj.toString();
		
		// Setup the header
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<String>(jsonString, headers);
		HttpMethod method = HttpMethod.POST;
		
		logger.info(method.toString() + ": " + url + ", for user " + username); // DO NOT log the entire json string as it includes the user's password
		
		ResponseEntity<UserSession> response = null;
		try {
			response = templateProvider.getTemplate().exchange(url, method, entity, UserSession.class);
		} catch (HttpClientErrorException ex) {
			throw new AuthenticationException("Unable to authenticate.");
		}
		
		UserData userData = null;		
		if((response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK) && response.hasBody()) {
			UserSession initSession = response.getBody();
			String displayName = initSession.getDisplayName();
			String sessionToken = initSession.getSessionToken();
			userData = new UserData(username, displayName, sessionToken, false);
		} else {			
			throw new AuthenticationException("Unable to authenticate.");
		}
		return userData;		
	}

	@Override
	public UserData getUser(String sessionToken) throws AuthenticationException {
		// First make sure the service is ready to go.
		validateService();
		
		// Build up the path
		String url = urlProvider.getAuthBaseUrl() + "/" + ServiceUtils.AUTHSVC_GET_USER_PATH;				
		
		// Setup the header
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.add(UserDataProvider.SESSION_TOKEN_KEY, sessionToken);
		HttpEntity<String> entity = new HttpEntity<String>("", headers);
		HttpMethod method = HttpMethod.GET;
		
		logger.info(method.toString() + ": " + url); 
		
		ResponseEntity<GetUser> response = null;
		try {
			response = templateProvider.getTemplate().exchange(url, method, entity, GetUser.class);
		} catch (HttpClientErrorException ex) {
			throw new AuthenticationException("Unable to authenticate.");
		}
		
		UserData userData = null;		
		if((response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK) && response.hasBody()) {
			GetUser getUser = response.getBody();
			userData = new UserData(getUser.getEmail(), getUser.getDisplayName(), sessionToken, false);
		} else {			
			throw new AuthenticationException("Unable to authenticate.");
		}
		return userData;		
	}	

	
	@Override
	public void createUser(UserRegistration userInfo) throws RestServiceException {
		// First make sure the service is ready to go.
		validateService();
		
		JSONObject obj = new JSONObject();
		try {
			obj.put("email", userInfo.getEmail());
			obj.put("firstName", userInfo.getFirstName());
			obj.put("lastName", userInfo.getLastName());
			obj.put("displayName", userInfo.getDisplayName());
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		// Build up the path
		String url = urlProvider.getAuthBaseUrl() + "/" + ServiceUtils.AUTHSVC_CREATE_USER_PATH;
		String jsonString = obj.toString();
		
		// Setup the header
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<String>(jsonString, headers);
		HttpMethod method = HttpMethod.POST;
		
		logger.info(method.toString() + ": " + url + ", JSON: " + jsonString);
		
		// Make the actual call.
		ResponseEntity<String> response = templateProvider.getTemplate().exchange(url, method, entity, String.class);

		if (response.getStatusCode() != HttpStatus.CREATED && response.getStatusCode() != HttpStatus.OK) {
			throw new RestClientException("Status code:" + response.getStatusCode().value());
		}		
	}

	@Override
	public void terminateSession(String sessionToken) throws RestServiceException {
		// First make sure the service is ready to go.
		validateService();
		
		// Build up the path
		String url = urlProvider.getAuthBaseUrl() + "/" + ServiceUtils.AUTHSVC_TERMINATE_SESSION_PATH;
		String jsonString = "{\"sessionToken\":\""+ sessionToken + "\"}";
		
		logger.info("DELETE: " + url + ", JSON: " + jsonString);
		
		// Setup the header
		HttpHeaders headers = new HttpHeaders();
		// If the user data is stored in a cookie, then fetch it and the session token to the header.
		UserDataProvider.addUserDataToHeader(this.getThreadLocalRequest(), headers);
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<String>(jsonString, headers);
		HttpMethod method = HttpMethod.DELETE;
		
		// Make the actual call.
		ResponseEntity<String> response = templateProvider.getTemplate().exchange(url, method, entity, String.class);

		if (response.getStatusCode() == HttpStatus.OK) {
		} else {
			throw new RestClientException("Status code:" + response.getStatusCode().value());
		}		
	}

	@Override
	public boolean ssoLogin(String sessionToken) throws RestServiceException {
		// First make sure the service is ready to go.
		validateService();
		
		JSONObject obj = new JSONObject();
		try {
			obj.put("sessionToken", sessionToken);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		// Build up the path
		String url = urlProvider.getAuthBaseUrl() + "/" + ServiceUtils.AUTHSVC_REFRESH_SESSION_PATH;
		String jsonString = obj.toString();
		
		// Setup the header
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<String>(jsonString, headers);
		HttpMethod method = HttpMethod.PUT;
		
		logger.info(method.toString() + ": " + url + ", JSON: " + jsonString);
		
		// Make the actual call.
		try {
			ResponseEntity<String> response = templateProvider.getTemplate().exchange(url, method, entity, String.class);
		} catch (UnexpectedException ex) {
			return true;
		} catch (NullPointerException nex) {
			// TODO : change this to properly deal with a 204!!!
			return true; // this is expected
		}
		return false;
	}

	@Override
	public List<AclPrincipal> getAllUsers() {
		// Build up the path
		StringBuilder builder = new StringBuilder();
		builder.append(urlProvider.getBaseUrl() + "/");
		builder.append(ServiceUtils.REPOSVC_GET_USERS_PATH);
		String url = builder.toString();	
		String userList = getJsonStringForUrl(url, HttpMethod.GET);
		return generateAclPrincipals(userList);
	}


	@Override
	public List<AclPrincipal> getAllGroups() {
		// Build up the path
		StringBuilder builder = new StringBuilder();
		builder.append(urlProvider.getBaseUrl() + "/");
		builder.append(ServiceUtils.AUTHSVC_GET_GROUPS_PATH);
		String url = builder.toString();	
		String groupList = getJsonStringForUrl(url, HttpMethod.GET);
		return generateAclPrincipals(groupList);
	}

	@Override
	public List<AclPrincipal> getAllUsersAndGroups() {
		List<AclPrincipal> users = getAllUsers();
		List<AclPrincipal> groups = getAllGroups();
		List<AclPrincipal> all = new ArrayList<AclPrincipal>();
		all.addAll(groups);
		all.addAll(users);
		return all;
	}


	@Override
	public String getAuthServiceUrl() {
		return urlProvider.getAuthBaseUrl();
	}

	@Override
	public String getSynapseWebUrl() {
		return urlProvider.getPortalBaseUrl();
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

	
	/*
	 * Private Methods
	 */
	private String getJsonStringForUrl(String url, HttpMethod method) {
		// First make sure the service is ready to go.
		validateService();

		logger.info(method.toString() + ": " + url);
		
		// Setup the header
		HttpHeaders headers = new HttpHeaders();
		// If the user data is stored in a cookie, then fetch it and the session token to the header.
		UserDataProvider.addUserDataToHeader(this.getThreadLocalRequest(), headers);
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<String>("", headers);
		
		// Make the actual call.
		ResponseEntity<String> response = templateProvider.getTemplate().exchange(url, method, entity, String.class);

		if (response.getStatusCode() == HttpStatus.OK) {			
			return response.getBody();
		} else {
			// TODO: better error handling
			throw new UnknownError("Status code:"
					+ response.getStatusCode().value());
		}
		
	}

	private List<AclPrincipal> generateAclPrincipals(String userList) {
		List<AclPrincipal> principals = new ArrayList<AclPrincipal>();
		JSONArray list = null;
		try {
			list = new JSONArray(userList);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		// create principal objects from JSON
		for(int i=0; i<list.length(); i++) {
			JSONObject principalObj;
			try {
				principalObj = list.getJSONObject(i);
				AclPrincipal principal = new AclPrincipal();
				if(principalObj.has(ServiceUtils.AUTHSVC_ACL_PRINCIPAL_NAME)) principal.setName(principalObj.getString(ServiceUtils.AUTHSVC_ACL_PRINCIPAL_NAME));
				if(principalObj.has(ServiceUtils.AUTHSVC_ACL_PRINCIPAL_ID)) principal.setId(principalObj.getString(ServiceUtils.AUTHSVC_ACL_PRINCIPAL_ID));
				if(principalObj.has(ServiceUtils.AUTHSVC_ACL_PRINCIPAL_URI)) principal.setUri(principalObj.getString(ServiceUtils.AUTHSVC_ACL_PRINCIPAL_URI));
				if(principalObj.has(ServiceUtils.AUTHSVC_ACL_PRINCIPAL_ETAG)) principal.setEtag(principalObj.getString(ServiceUtils.AUTHSVC_ACL_PRINCIPAL_ETAG));
				if(principalObj.has(ServiceUtils.AUTHSVC_ACL_PRINCIPAL_INDIVIDUAL)) principal.setIndividual(principalObj.getBoolean(ServiceUtils.AUTHSVC_ACL_PRINCIPAL_INDIVIDUAL));
				if(principalObj.has(ServiceUtils.AUTHSVC_ACL_PRINCIPAL_CREATION_DATE)) {
					Long creationDate = principalObj.getLong(ServiceUtils.AUTHSVC_ACL_PRINCIPAL_CREATION_DATE);
					if(creationDate != null)
						principal.setCreationDate(new Date(creationDate));
				}			
				principals.add(principal);
			} catch (JSONException e) {
				e.printStackTrace();
			}			
		}
		return principals;
	}
	
}
