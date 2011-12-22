package org.sagebionetworks.web.server.servlet;

import java.util.logging.Logger;

import org.sagebionetworks.web.client.LinkedInService;
import org.sagebionetworks.web.server.RestTemplateProvider;
import org.sagebionetworks.web.shared.LinkedInInfo;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.LinkedInApi;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.inject.Inject;

public class LinkedInServiceImpl extends RemoteServiceServlet implements LinkedInService {
	private static Logger logger = Logger.getLogger(LinkedInServiceImpl.class.getName());
	
	// OAuth service for authentication and integration with LinkedIn
	private OAuthService oAuthService;

	
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
	 * Injected via Gin
	 * @param provider
	 */
	@Inject
	public void setServiceUrlProvider(ServiceUrlProvider provider){
		this.urlProvider = provider;
	}
	
	/**
	 * Returns the authorization URL for LinkedIn as well as any exception that occurs and the 
	 * requestToken secret
	 */
	@Override
	public LinkedInInfo returnAuthUrl() {
		validateService();
		
		Token requestToken = oAuthService.getRequestToken();
		String authUrl = oAuthService.getAuthorizationUrl(requestToken);
		LinkedInInfo linkedInInfo = new LinkedInInfo(authUrl, requestToken.getSecret(), null);
		return linkedInInfo;
	}
	
	@Override
	public String getCurrentUserInfo(String requestToken, String secret, String verifier) {
		validateService();
		
		// Create the access token
		Token rToken = new Token(requestToken, secret);
		Verifier v = new Verifier(verifier);
		Token accessToken = oAuthService.getAccessToken(rToken, v);
		
		// Post a request to LinkedIn to get the user's public information
		OAuthRequest request = new OAuthRequest(Verb.GET, "http://api.linkedin.com/v1/people/~");
		oAuthService.signRequest(accessToken, request);
		Response response = request.send();
		return response.getBody();
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
		if(oAuthService == null) {
			oAuthService = new ServiceBuilder().provider(LinkedInApi.class)
											   .apiKey("0oq37ippxz8c")
											   .apiSecret("2JpVsFPqHqT0Xou4")
											   .callback(urlProvider.getPortalBaseUrl() + "/#Profile:")
											   .build();
		}
	}
}