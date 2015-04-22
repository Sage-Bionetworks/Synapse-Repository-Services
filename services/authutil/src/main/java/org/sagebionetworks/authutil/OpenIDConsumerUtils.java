package org.sagebionetworks.authutil;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openid4java.OpenIDException;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.discovery.Discovery;
import org.openid4java.discovery.DiscoveryException;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.AuthSuccess;
import org.openid4java.message.MessageExtension;
import org.openid4java.message.ParameterList;
import org.openid4java.message.ax.AxMessage;
import org.openid4java.message.ax.FetchRequest;
import org.openid4java.message.ax.FetchResponse;
import org.openid4java.message.sreg.SRegMessage;
import org.openid4java.message.sreg.SRegRequest;
import org.openid4java.message.sreg.SRegResponse;
import org.sagebionetworks.repo.model.UnauthenticatedException;

/**
 * Modified "Relying Party" implementation
 * Taken from: http://code.google.com/p/openid4java/wiki/QuickStart
 */
public class OpenIDConsumerUtils {
	
	public static final Map<String, String> OPEN_ID_PROVIDERS = new HashMap<String, String>() {
		private static final long serialVersionUID = 1L;
	{
		put("GOOGLE", "https://www.google.com/accounts/o8/id");
		
		// Verisign is unsecure!
		// put("VERISIGN", "https://pip.verisignlabs.com/");
		
		// Yahoo will not return an email address even when requested
		// put("YAHOO", "https://me.yahoo.com/");
	}};
	
	public static final String OPEN_ID_PROVIDER_NAME_PARAM = "org.sagebionetworks.openid.provider";
	
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
	 * At this time only Google is supported
	 */
	@SuppressWarnings("unchecked")
	private static DiscoveryInformation getDiscoveryInfo(String providerName) throws DiscoveryException {
		if (providerName == null) {
			throw new IllegalArgumentException("OpenID provider name cannot be null");
		}
		
		if (!OPEN_ID_PROVIDERS.containsKey(providerName)) {
			throw new IllegalArgumentException("Unsupported OpenID provider: " + providerName);
		}

		// Attempt to associate with the OpenID provider
		// and retrieve one service endpoint for authentication
		String endpoint = OPEN_ID_PROVIDERS.get(providerName);
		List<Discovery> discoveries = manager.discover(endpoint);
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

		// Note: there are two ways of requesting attributes 
		// See: http://openid.net/specs/openid-attribute-exchange-1_0-04.html
		// See: http://openid.net/specs/openid-simple-registration-extension-1_0.html
		
		// Attach a fetch request to the authentication request
		FetchRequest fetch = FetchRequest.createFetchRequest();
		fetch.addAttribute(OpenIDInfo.AX_EMAIL, "http://axschema.org/contact/email", true);
		fetch.addAttribute(OpenIDInfo.AX_FIRST_NAME, "http://axschema.org/namePerson/first", true);
		fetch.addAttribute(OpenIDInfo.AX_LAST_NAME, "http://axschema.org/namePerson/last", true);
		authReq.addExtension(fetch);

		// Attach another fetch request
		SRegRequest sregReq = SRegRequest.createFetchRequest();
		sregReq.addAttribute(OpenIDInfo.AX_REG_EMAIL, true);
		sregReq.addAttribute(OpenIDInfo.AX_REG_FULL_NAME, true);
		authReq.addExtension(sregReq);

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
			throws IOException, UnauthenticatedException {
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
				
				if (authSuccess.hasExtension(SRegMessage.OPENID_NS_SREG))
				{
				    MessageExtension ext = authSuccess.getExtension(SRegMessage.OPENID_NS_SREG);

				    if (ext instanceof SRegResponse)
				    {
				        SRegResponse sregResp = (SRegResponse) ext;
				        
				        String fullName = sregResp.getAttributeValue("fullname");
				        String email = sregResp.getAttributeValue("email");
				        result.setFullName(fullName);
				        result.setEmail(email);
				    }
				}

				return result;
			}
		} catch (OpenIDException e) {
			throw new UnauthenticatedException(e);
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