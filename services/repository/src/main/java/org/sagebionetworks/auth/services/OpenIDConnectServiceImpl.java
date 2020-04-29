package org.sagebionetworks.auth.services;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.oauth.OAuthClientManager;
import org.sagebionetworks.repo.manager.oauth.OIDCTokenHelper;
import org.sagebionetworks.repo.manager.oauth.OpenIDConnectManager;
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
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OpenIDConnectServiceImpl implements OpenIDConnectService {
	
	private static final List<String> TOKEN_ENDPOINT_AUTHENTICATION_TYPES = Collections.singletonList("client_secret_basic");
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private OAuthClientManager oauthClientManager;

	@Autowired
	private OpenIDConnectManager oidcManager;

	@Autowired
	private OIDCTokenHelper oidcTokenHelper;

	@Override
	public OAuthClient createOpenIDConnectClient(Long userId, OAuthClient oauthClient) throws ServiceUnavailableException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return oauthClientManager.createOpenIDConnectClient(userInfo, oauthClient);
	}

	@Override
	public OAuthClientIdAndSecret createOAuthClientSecret(Long userId, String clientId) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return oauthClientManager.createClientSecret(userInfo, clientId);
	}
	
	@Override
	public OAuthClient getOpenIDConnectClient(Long userId, String id) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return oauthClientManager.getOpenIDConnectClient(userInfo, id);
	}

	@Override
	public OAuthClientList listOpenIDConnectClients(Long userId, String nextPageToken) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return oauthClientManager.listOpenIDConnectClients(userInfo, nextPageToken);
	}

	@Override
	public OAuthClient updateOpenIDConnectClient(Long userId, OAuthClient oauthClient) throws ServiceUnavailableException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return oauthClientManager.updateOpenIDConnectClient(userInfo, oauthClient);
	}
	
	@Override
	public OAuthClient updateOpenIDConnectClientVerifiedStatus(Long userId, String clientId, String etag, boolean verifiedStatus) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return oauthClientManager.updateOpenIDConnectClientVerifiedStatus(userInfo, clientId, etag, verifiedStatus);
	}

	@Override
	public void deleteOpenIDConnectClient(Long userId, String id) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		oauthClientManager.deleteOpenIDConnectClient(userInfo, id);
	}

	@Override
	public OIDConnectConfiguration getOIDCConfiguration(String endpoint) {
		ValidateArgument.required(endpoint, "OAuth Endpoint");
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
		result.setUserinfo_signing_alg_values_supported(Arrays.asList(OIDCSigningAlgorithm.values()));
		result.setToken_endpoint_auth_methods_supported(TOKEN_ENDPOINT_AUTHENTICATION_TYPES);
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
	public boolean hasUserGrantedConsent(Long userId, OIDCAuthorizationRequest authorizationRequest) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return oidcManager.hasUserGrantedConsent(userInfo, authorizationRequest);
		
	}
	
	@Override
	public OAuthAuthorizationResponse authorizeClient(Long userId, OIDCAuthorizationRequest authorizationRequest) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return oidcManager.authorizeClient(userInfo, authorizationRequest);
	}

	@Override
	public OIDCTokenResponse getTokenResponse(String verifiedClientId, OAuthGrantType grantType, 
			String authorizationCode, String redirectUri, String refreshToken, String scope, String claims, String oauthEndpoint) {
		if (OAuthGrantType.authorization_code==grantType) {
			return oidcManager.getAccessToken(authorizationCode, verifiedClientId, redirectUri, oauthEndpoint);
		} else {
			throw new IllegalArgumentException("Unsupported grant type"+grantType);
		}
	}

	@Override
	public Object getUserInfo(String accessToken, String oauthEndpoint) {
		return oidcManager.getUserInfo(accessToken, oauthEndpoint);
	}
}
