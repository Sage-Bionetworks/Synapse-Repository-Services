package org.sagebionetworks.auth.services;

import java.util.Arrays;
import java.util.Collections;

import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.oauth.OIDCTokenHelper;
import org.sagebionetworks.repo.manager.oauth.OpenIDConnectManager;
import org.sagebionetworks.repo.model.UnauthenticatedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.oauth.JsonWebKeySet;
import org.sagebionetworks.repo.model.oauth.OAuthAuthorizationResponse;
import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.repo.model.oauth.OAuthClientIdAndSecret;
import org.sagebionetworks.repo.model.oauth.OAuthClientList;
import org.sagebionetworks.repo.model.oauth.OAuthGrantType;
import org.sagebionetworks.repo.model.oauth.OAuthResponseType;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.oauth.OIDCAuthorizationRequest;
import org.sagebionetworks.repo.model.oauth.OIDCAuthorizationRequestDescription;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCSigningAlgorithm;
import org.sagebionetworks.repo.model.oauth.OIDCSubjectIdentifierType;
import org.sagebionetworks.repo.model.oauth.OIDCTokenResponse;
import org.sagebionetworks.repo.model.oauth.OIDConnectConfiguration;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.springframework.beans.factory.annotation.Autowired;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

public class OpenIDConnectServiceImpl implements OpenIDConnectService {
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private OpenIDConnectManager oidcManager;

	@Autowired
	private OIDCTokenHelper oidcTokenHelper;

	@Override
	public OAuthClient createOpenIDConnectClient(Long userId, OAuthClient oauthClient) throws ServiceUnavailableException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return oidcManager.createOpenIDConnectClient(userInfo, oauthClient);
	}

	@Override
	public OAuthClientIdAndSecret createOAuthClientSecret(Long userId, String clientId) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return oidcManager.createClientSecret(userInfo, clientId);
	}
	
	@Override
	public OAuthClient getOpenIDConnectClient(Long userId, String id) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return oidcManager.getOpenIDConnectClient(userInfo, id);
	}

	@Override
	public OAuthClientList listOpenIDConnectClients(Long userId, String nextPageToken) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return oidcManager.listOpenIDConnectClients(userInfo, nextPageToken);
	}

	@Override
	public OAuthClient updateOpenIDConnectClient(Long userId, OAuthClient oauthClient) throws ServiceUnavailableException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return oidcManager.updateOpenIDConnectClient(userInfo, oauthClient);
	}

	@Override
	public void deleteOpenIDConnectClient(Long userId, String id) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		oidcManager.deleteOpenIDConnectClient(userInfo, id);
	}

	@Override
	public OIDConnectConfiguration getOIDCConfiguration(String endpoint) {
		String issuer = endpoint;
		OIDConnectConfiguration result = new OIDConnectConfiguration();
		result.setIssuer(issuer);
		result.setAuthorization_endpoint(StackConfigurationSingleton.singleton().getOAuthAuthorizationEndpoint());
		result.setToken_endpoint(issuer+UrlHelpers.OAUTH_2_TOKEN);
		result.setUserinfo_endpoint(issuer+UrlHelpers.OAUTH_2_USER_INFO);
		result.setJwks_uri(issuer+UrlHelpers.OAUTH_2_JWKS);
		result.setRegistration_endpoint(issuer+UrlHelpers.OAUTH_2_CLIENT);
		result.setScopes_supported(Arrays.asList(OAuthScope.values()));
		result.setResponse_types_supported(Arrays.asList(OAuthResponseType.values()));
		result.setGrant_types_supported(Collections.singletonList(OAuthGrantType.authorization_code));
		result.setSubject_types_supported(Arrays.asList(OIDCSubjectIdentifierType.values()));
		result.setId_token_signing_alg_values_supported(Arrays.asList(OIDCSigningAlgorithm.values()));
		result.setClaims_supported(Arrays.asList(OIDCClaimName.values()));
		result.setService_documentation("https://docs.synapse.org");
		result.setClaims_parameter_supported(true);
		return result;
	}

	@Override
	public JsonWebKeySet getOIDCJsonWebKeySet() {
		return oidcTokenHelper.getJSONWebKeySet();
	}

	@Override
	public OIDCAuthorizationRequestDescription getAuthenticationRequestDescription(OIDCAuthorizationRequest authorizationRequest) {
		return oidcManager.getAuthenticationRequestDescription(authorizationRequest);
	}

	@Override
	public OAuthAuthorizationResponse authorizeClient(Long userId, OIDCAuthorizationRequest authorizationRequest) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return oidcManager.authorizeClient(userInfo, authorizationRequest);
	}

	@Override
	public OIDCTokenResponse getTokenResponse(String verifiedClientId, OAuthGrantType grantType, 
			String authorizationCode, String redirectUri, String refreshToken, String scope, String claims, String oauthEndpoint) {
		if (grantType==OAuthGrantType.authorization_code) {
			return oidcManager.getAccessToken(authorizationCode, verifiedClientId, redirectUri, oauthEndpoint);
		} else if (grantType==OAuthGrantType.refresh_token) {
			throw new IllegalArgumentException(OAuthGrantType.refresh_token+" unsupported.");
		} else {
			throw new IllegalArgumentException("Unsupported grant type"+grantType);
		}
	}

	@Override
	public Object getUserInfo(String accessTokenParam, String oauthEndpoint) {
		Jwt<JwsHeader,Claims> accessToken = null;
		try {
			accessToken = oidcTokenHelper.parseJWT(accessTokenParam);
		} catch (IllegalArgumentException e) {
			throw new UnauthenticatedException("Could not interpret access token.", e);
		}

		return oidcManager.getUserInfo(accessToken, oauthEndpoint);
	}
}
