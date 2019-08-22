package org.sagebionetworks.auth;

import org.apache.commons.lang3.StringUtils;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UnauthenticatedException;
import org.sagebionetworks.repo.model.oauth.JsonWebKeySet;
import org.sagebionetworks.repo.model.oauth.OAuthAuthorizationResponse;
import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.repo.model.oauth.OAuthClientIdAndSecret;
import org.sagebionetworks.repo.model.oauth.OAuthClientList;
import org.sagebionetworks.repo.model.oauth.OAuthGrantType;
import org.sagebionetworks.repo.model.oauth.OIDCAuthorizationRequest;
import org.sagebionetworks.repo.model.oauth.OIDCAuthorizationRequestDescription;
import org.sagebionetworks.repo.model.oauth.OIDCTokenResponse;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.util.UriComponentsBuilder;

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
	
	public static String getEndpoint(UriComponentsBuilder uriComponentsBuilder) {
		return uriComponentsBuilder.fragment(null).replaceQuery(null).path(UrlHelpers.AUTH_PATH).build().toString();	
	}

	/**
	 * 
	 * @return
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.WELL_KNOWN_OPENID_CONFIGURATION, method = RequestMethod.GET)
	public @ResponseBody
	OIDConnectConfiguration getOIDCConfiguration(UriComponentsBuilder uriComponentsBuilder) throws NotFoundException {
		return serviceProvider.getOpenIDConnectService().
				getOIDCConfiguration(getEndpoint(uriComponentsBuilder));
	}
	
	/**
	 * 
	 * @return
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.OAUTH_2_JWKS, method = RequestMethod.GET)
	public @ResponseBody
	JsonWebKeySet getOIDCJsonWebKeySet() throws NotFoundException {
		return serviceProvider.getOpenIDConnectService().
				getOIDCJsonWebKeySet();
	}
	
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
	OAuthClientIdAndSecret createOpenIDConnectClient(
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
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.OAUTH_2_AUTH_REQUEST_DESCRIPTION, method = RequestMethod.POST)
	public @ResponseBody
	OIDCAuthorizationRequestDescription getAuthenticationRequestDescription(
			@RequestBody OIDCAuthorizationRequest authorizationRequest 
			) {
		return serviceProvider.getOpenIDConnectService().getAuthenticationRequestDescription(authorizationRequest);
	}
	
	/**
	 * 
	 * get access code for a given client, scopes, response type(s), and extra claim(s).
	 * See:
	 * https://openid.net/specs/openid-connect-core-1_0.html#Consent
	 * https://openid.net/specs/openid-connect-core-1_0.html#AuthRequest
	 *
	 * @param userId
	 * @param authorizationRequest
	 * @return
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.OAUTH_2_CONSENT, method = RequestMethod.POST)
	public @ResponseBody
	OAuthAuthorizationResponse authorizeClient(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody OIDCAuthorizationRequest authorizationRequest 
			) throws NotFoundException {
		return serviceProvider.getOpenIDConnectService().authorizeClient(userId, authorizationRequest);
	}
	
	// TODO how to suppress verifiedClientId from showing up in API docs
	/**
	 * 
	 *  Get access, refresh and id tokens, as per https://openid.net/specs/openid-connect-core-1_0.html#TokenResponse
	 *  
	 *  Request must include client ID and Secret in an Authentication header
	 *  
	 * @param verifiedClientId id of the OAuth Client, verified via Basic Authentication
	 * @param grant_type  authorization_code or refresh_token
	 * @param code required if grant_type is authorization_code
	 * @param redirectUri required if grant_type is authorization_code
	 * @param refresh_token required if grant_type is refresh_token
	 * @param scope required if grant_type is refresh_token
	 * @param claims optional if grant_type is refresh_token
	 * @return
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.OAUTH_2_TOKEN, method = RequestMethod.POST)
	public @ResponseBody
	OIDCTokenResponse getTokenResponse(
			@RequestParam(value = AuthorizationConstants.OAUTH_VERIFIED_CLIENT_ID_PARAM, required=false) String verifiedClientId,
			@RequestParam(value = AuthorizationConstants.OAUTH2_GRANT_TYPE_PARAM) OAuthGrantType grant_type,
			@RequestParam(value = AuthorizationConstants.OAUTH2_CODE_PARAM, required=false) String code,
			@RequestParam(value = AuthorizationConstants.OAUTH2_REDIRECT_URI_PARAM, required=false) String redirectUri,
			@RequestParam(value = AuthorizationConstants.OAUTH2_REFRESH_TOKEN_PARAM, required=false) String refresh_token,
			@RequestParam(value = AuthorizationConstants.OAUTH2_SCOPE_PARAM, required=false) String scope,
			@RequestParam(value = AuthorizationConstants.OAUTH2_CLAIMS_PARAM, required=false) String claims,
			UriComponentsBuilder uriComponentsBuilder
			)  throws NotFoundException {
		if (StringUtils.isEmpty(verifiedClientId)) {
			throw new UnauthenticatedException("OAuth Client ID and secret must be passed via Basic Authentication.  Credentials are missing or invalid.");
		}
		return serviceProvider.getOpenIDConnectService().getTokenResponse(verifiedClientId, grant_type, code, redirectUri, refresh_token, scope, claims, getEndpoint(uriComponentsBuilder));
	}
		
	// TODO add a token validation filter that validates the access token
	// TODO I think the content type has to be application/jwt
	/**
	 * The result is either a JSON Object or a JSON Web Token, depending on whether the client registered a
	 * signing algorithm in its userinfo_signed_response_alg field.  
	 * https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata
	 * 
	 * @param accessTokenHeader
	 * @return
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.OAUTH_2_USER_INFO, method = {RequestMethod.GET})
	public @ResponseBody
	Object getUserInfoGET(
			@RequestHeader(value = AuthorizationConstants.OAUTH2_ACCESS_TOKEN_HEADER, required=true) String accessToken,
			UriComponentsBuilder uriComponentsBuilder
			)  throws NotFoundException {
		return serviceProvider.getOpenIDConnectService().getUserInfo(accessToken, getEndpoint(uriComponentsBuilder));
	}

	/**
	 * The result is either a JSON Object or a JSON Web Token, depending on whether the client registered a
	 * signing algorithm in its userinfo_signed_response_alg field.  
	 * https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata
	 * 
	 * @param accessTokenHeader
	 * @return
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.OAUTH_2_USER_INFO, method = {RequestMethod.POST})
	public @ResponseBody
	Object getUserInfoPOST(
			@RequestHeader(value = AuthorizationConstants.OAUTH2_ACCESS_TOKEN_HEADER, required=true) String accessToken,
			UriComponentsBuilder uriComponentsBuilder
			)  throws NotFoundException {
		return serviceProvider.getOpenIDConnectService().getUserInfo(accessToken, getEndpoint(uriComponentsBuilder));
	}

}
