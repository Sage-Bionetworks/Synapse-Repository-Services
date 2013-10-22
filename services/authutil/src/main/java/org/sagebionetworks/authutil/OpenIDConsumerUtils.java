package org.sagebionetworks.authutil;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.openid4java.OpenIDException;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.discovery.Discovery;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.AuthSuccess;
import org.openid4java.message.ParameterList;
import org.openid4java.message.ax.AxMessage;
import org.openid4java.message.ax.FetchRequest;
import org.openid4java.message.ax.FetchResponse;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.auth.DiscoveryInfo;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

/**
 * Modified "Relying Party" implementation
 * Taken from: http://code.google.com/p/openid4java/wiki/QuickStart
 */
public class OpenIDConsumerUtils {

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
	 * Determines the redirect URL needed to perform the first part of the OpenID handshake
	 */
	public static String authRequest(String openIdProvider,
			String returnToUrl) throws IOException, OpenIDException {
		ensureManagerExists();

		// Perform discovery on the user-supplied identifier
		@SuppressWarnings("unchecked")
		List<Discovery> discoveries = (List<Discovery>) manager.discover(openIdProvider);

		// Attempt to associate with the OpenID provider
		// and retrieve one service endpoint for authentication
		DiscoveryInformation discovered = manager.associate(discoveries);
		
		// Convert the information into a URL-parameter friendly form
		DiscoveryInfo dto = DiscoveryInfoUtils.convertObjectToDTO(discovered);
		String discInfo;
		try {
			discInfo = DiscoveryInfoUtils.zipDTO(dto);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
		
		System.out.println("A " + returnToUrl);
		try {
			returnToUrl = addRequestParameter(returnToUrl, OpenIDInfo.DISCOVERY_INFO_PARAM_NAME + "=" + discInfo);
			System.out.println("B " + returnToUrl);
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

		String discoveryParam = parameters.getParameterValue(OpenIDInfo.DISCOVERY_INFO_PARAM_NAME);
		if (discoveryParam == null) {
			throw new RuntimeException(
					"OpenID authentication failure: Missing required discovery information.");
		}
		
		// Convert the information into the form taken by the OpenID library
		DiscoveryInfo discInfo;
		try {
			discInfo = DiscoveryInfoUtils.unzipDTO(discoveryParam);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
		DiscoveryInformation discovered = DiscoveryInfoUtils.convertDTOToObject(discInfo);
		
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