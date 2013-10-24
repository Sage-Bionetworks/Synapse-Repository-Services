package org.sagebionetworks.authutil;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.openid4java.OpenIDException;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.discovery.Discovery;
import org.openid4java.discovery.DiscoveryException;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.AuthSuccess;
import org.openid4java.message.ParameterList;
import org.openid4java.message.ax.AxMessage;
import org.openid4java.message.ax.FetchRequest;
import org.openid4java.message.ax.FetchResponse;
import org.sagebionetworks.repo.model.UnauthorizedException;

/**
 * Modified "Relying Party" implementation
 * Taken from: http://code.google.com/p/openid4java/wiki/QuickStart
 */
public class OpenIDConsumerUtils {
	
	public static final String OPEN_ID_PROVIDER_GOOGLE_VALUE = "GOOGLE";
	public static final String OPEN_ID_PROVIDER_GOOGLE_ENDPOINT = "https://www.google.com/accounts/o8/id";
	public static final String OPEN_ID_PROVIDER_YAHOO_VALUE = "YAHOO";
	public static final String OPEN_ID_PROVIDER_YAHOO_ENDPOINT = "https://me.yahoo.com/";
	public static final String OPEN_ID_PROVIDER_VERISIGN_VALUE = "VERISIGN";
	public static final String OPEN_ID_PROVIDER_VERISIGN_ENDPOINT = "https://pip.verisignlabs.com/";
	
	public static final String OPEN_ID_PROVIDER_NAME_PARAM = "org.sagebionetworks.openid.provider";

	public static final String AX_EMAIL = "Email";
	public static final String AX_FIRST_NAME = "FirstName";
	public static final String AX_LAST_NAME = "LastName";
	
	private static ConsumerManager manager;
	
	/**
	 * Allows mocking of the underlying Open ID library
	 */
	public static void setConsumerManager(ConsumerManager otherManager) {
		manager = otherManager;
	}
	
	private static void ensureManagerExists() {
		if (manager == null) {
			manager = new ConsumerManager();
		}
	}
	
	/**
	 * This maps allowed provider names to their OpenID endpoints
	 * At this time only Google, Yahoo, and Verisign are supported
	 */
	@SuppressWarnings("unchecked")
	private static DiscoveryInformation getDiscoveryInfo(String providerName) throws DiscoveryException {
		List<Discovery> discoveries;
		if (providerName.equals(OPEN_ID_PROVIDER_GOOGLE_VALUE)) {
			discoveries = manager.discover(OPEN_ID_PROVIDER_GOOGLE_ENDPOINT);
			
		} else if (providerName.equals(OPEN_ID_PROVIDER_YAHOO_VALUE)) {
			discoveries = manager.discover(OPEN_ID_PROVIDER_YAHOO_ENDPOINT);
			
		} else if (providerName.equals(OPEN_ID_PROVIDER_VERISIGN_VALUE)) {
			discoveries = manager.discover(OPEN_ID_PROVIDER_VERISIGN_ENDPOINT);
			
		} else {
			throw new IllegalArgumentException("Unsupported OpenID provider: " + providerName);
		}

		// Attempt to associate with the OpenID provider
		// and retrieve one service endpoint for authentication
		return manager.associate(discoveries);
	}

	/**
	 * Determines the redirect URL needed to perform the first part of the OpenID handshake
	 */
	public static String authRequest(String openIdProviderName, String returnToUrl) throws IOException, OpenIDException {
		ensureManagerExists();

		// Perform discovery on the user-supplied provider name
		DiscoveryInformation discovered = getDiscoveryInfo(openIdProviderName);
		
		// If the provider is supported, stash it in a parameter
		try {
			returnToUrl = addRequestParameter(returnToUrl, OPEN_ID_PROVIDER_NAME_PARAM + "=" + openIdProviderName);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}

		// Obtain a AuthRequest message to be sent to the OpenID provider
		AuthRequest authReq = manager.authenticate(discovered, returnToUrl);

		// Request the 'email' attribute
		FetchRequest fetch = FetchRequest.createFetchRequest();
		fetch.addAttribute(AX_EMAIL, "http://axschema.org/contact/email", true);
		fetch.addAttribute(AX_FIRST_NAME, "http://axschema.org/namePerson/first", true);
		fetch.addAttribute(AX_LAST_NAME, "http://axschema.org/namePerson/last", true);

		// Attach the fetch request to the authentication request
		authReq.addExtension(fetch);

		// GET HTTP-redirect to the OpenID Provider endpoint
		// The only method supported in OpenID 1.x redirect-URL 
		// Usually limited ~2048 bytes
		return authReq.getDestinationUrl(true);
	}

	/**
	 * Fetches Open ID information from the request after verifying it
	 * 
	 * @throws UnauthorizedException If the request is invalid
	 */
	public static OpenIDInfo verifyResponse(ParameterList parameters)
			throws IOException, UnauthorizedException {
		ensureManagerExists();
		
		//TODO Modification is needed to get it working with hosted google apps
		// See: https://groups.google.com/forum/#!topic/openid4java/I0nl46KfXF0
		
		String openIdProviderName = parameters.getParameterValue(OPEN_ID_PROVIDER_NAME_PARAM);

		DiscoveryInformation discovered;
		try {
			discovered = getDiscoveryInfo(openIdProviderName);
		} catch (DiscoveryException e) {
			throw new RuntimeException(e);
		}
		
		try {
			AuthSuccess authSuccess = AuthSuccess.createAuthSuccess(parameters);
			boolean success = manager.verifyNonce(authSuccess, discovered);
			
			// Examine the verification result and extract the verified identifier
			if (success) {
				OpenIDInfo result = new OpenIDInfo();
				result.setIdentifier(parameters.getParameterValue("openid.identity"));
				
				if (authSuccess.hasExtension(AxMessage.OPENID_NS_AX)) {
					FetchResponse fetchResp = (FetchResponse) authSuccess
							.getExtension(AxMessage.OPENID_NS_AX);

					@SuppressWarnings("unchecked")
					Map<String, List<String>> attributes = (Map<String, List<String>>) fetchResp.getAttributes();
					result.setMap(attributes);
				}

				return result;
			}
		} catch (OpenIDException e) {
			throw new UnauthorizedException(e);
		}
		
		// not verified
		return null;
	}

	
	/**
	 * Add a new query parameter to an existing url
	 */
	public static String addRequestParameter(String urlString, String queryParameter) 
			throws URISyntaxException {
		URI uri = new URI(urlString);
		String query = uri.getQuery();
		if (query == null || query.length() == 0) {
			query = queryParameter;
		} else {
			query += "&" + queryParameter;
		}
		URI uriMod = new URI(uri.getScheme(), 
				uri.getUserInfo(),
				uri.getHost(),
				uri.getPort(), 
				uri.getPath(), 
				query, 
				uri.getFragment());
		return uriMod.toString();
	}
}