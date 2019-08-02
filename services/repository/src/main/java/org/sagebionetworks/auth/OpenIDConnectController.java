package org.sagebionetworks.auth;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.oauth.OAuthAuthorizationResponse;
import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.repo.model.oauth.OAuthClientList;
import org.sagebionetworks.repo.model.oauth.OAuthResponseType;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestParameter;
import org.sagebionetworks.repo.model.oauth.OIDConnectConfiguration;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.sagebionetworks.repo.web.service.ServiceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 
The OpenID Connect (OIDC) services implement OAuth 2.0 with the OpenID identity extensions.
 *
 */
@Controller
@ControllerInfo(displayName="OpenID Connect Services", path="auth/v1")
@RequestMapping(UrlHelpers.AUTH_PATH)
public class OpenIDConnectController {
	@Autowired
	private ServiceProvider serviceProvider;

	/**
	 * Create an OAuth 2.0 client.
	 * 
	 * @param userId
	 * @param oauthClient
	 * @return
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.OAUTH_2_CLIENT, method = RequestMethod.POST)
	public @ResponseBody
	OAuthClient createOpenIDConnectClient(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody OAuthClient oauthClient
			) throws NotFoundException {
		return serviceProvider.getOpenIDConnectService().
				createOpenIDConnectClient(userId, oauthClient);
	}
	
	/**
	 * Get an existing OAuth 2.0 client.
	 * 
	 * @param userId
	 * @param id
	 * @return
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.OAUTH_2_CLIENT_ID, method = RequestMethod.GET)
	public @ResponseBody
	OAuthClient getOpenIDConnectClient(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = UrlHelpers.ID_PATH_VARIABLE) String id
			) throws NotFoundException {
		return serviceProvider.getOpenIDConnectService().
				getOpenIDConnectClient(userId, id);
	}
	
	/**
	 * 
	 * List the OAuth 2.0 clients created by the current user.
	 * 
	 * @param userId
	 * @param nextPageToken
	 * @param id
	 * @return
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.OAUTH_2_CLIENT, method = RequestMethod.GET)
	public @ResponseBody
	OAuthClientList listOpenIDConnectClients(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = UrlHelpers.NEXT_PAGE_TOKEN_PARAM) String nextPageToken,
			@PathVariable(value = UrlHelpers.ID_PATH_VARIABLE) String id
			) throws NotFoundException {
		return serviceProvider.getOpenIDConnectService().
				listOpenIDConnectClients(userId, nextPageToken);
	}
	
	/**
	 * Update the metadata for an existing OAuth 2.0 client
	 * @param userId
	 * @param oauthClient
	 * @return
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.OAUTH_2_CLIENT_ID, method = RequestMethod.PUT)
	public @ResponseBody
	OAuthClient updateOpenIDConnectClient(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody OAuthClient oauthClient
			) throws NotFoundException {
		return serviceProvider.getOpenIDConnectService().
				updateOpenIDConnectClient(userId, oauthClient);
	}
	
	/**
	 * Delete OAuth 2.0 client
	 * 
	 * @param userId
	 * @param id
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.OAUTH_2_CLIENT_ID, method = RequestMethod.DELETE)
	public void deletedOpenIDClient(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = UrlHelpers.ID_PATH_VARIABLE) String id
			) throws NotFoundException {
		serviceProvider.getOpenIDConnectService().
				deleteOpenIDConnectClient(userId, id);
	}
	
	
	/**
	 * get access code for a given client, scopes, response type(s), and extra claim(s).
	 * See:
	 * https://openid.net/specs/openid-connect-core-1_0.html#Consent
	 * https://openid.net/specs/openid-connect-core-1_0.html#AuthRequest
	 * 
	 * @param scope
	 * @param claims
	 * @param response_type
	 * @param clientId
	 * @param redirectUri
	 * @param state
	 * @return
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.OAUTH_2_CONSENT, method = RequestMethod.POST)
	public @ResponseBody
	OAuthAuthorizationResponse authorizeClient(
			@RequestParam(value = AuthorizationConstants.OAUTH2_SCOPE) String scope,
			@RequestParam(value = AuthorizationConstants.OAUTH2_CLAIMS) OIDCClaimsRequestParameter claims,
			@RequestParam(value = AuthorizationConstants.OAUTH2_RESPONSE_TYPE) OAuthResponseType response_type,
			@RequestParam(value = AuthorizationConstants.OAUTH2_CLIENT_ID) String clientId,
			@RequestParam(value = AuthorizationConstants.OAUTH2_REDIRECT_URI) String redirectUri,
			@RequestParam(value = AuthorizationConstants.OAUTH2_STATE) String state 
			) throws NotFoundException {
		return serviceProvider.getOpenIDConnectService().authorizeClient();
	}
	
	
	
	
	
	
	
	// get session and id tokens
	// https://openid.net/specs/openid-connect-core-1_0.html#TokenResponse
	// /oauth2/token
// note the request is to be authenticated via basic auth
//	public OIDCTokenResponse
	
	// TODO revoke a session token
	
	// TODO exchange a refresh token for a new refresh token / access token pair
	
	
	/**
	 * 
	 * @return
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.WELL_KNOWN_OPENID_CONFIGURATION, method = RequestMethod.GET)
	public @ResponseBody
	OIDConnectConfiguration getOIDCConfiguration() throws NotFoundException {
		return serviceProvider.getOpenIDConnectService().
				getOIDCConfiguration();
	}
	
	// TODO JWKS
	//UrlHelpers.OAUTH_2_JWKS
	
	// TODO userinfo endpoint
	// TODO Must support both GET and POST
}
