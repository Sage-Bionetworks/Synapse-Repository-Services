package org.sagebionetworks.auth.services;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.repo.manager.OIDCTokenUtil;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.oauth.OpenIDConnectManager;
import org.sagebionetworks.repo.model.UnauthenticatedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.oauth.JsonWebKey;
import org.sagebionetworks.repo.model.oauth.JsonWebKeyRSA;
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
import org.sagebionetworks.repo.web.UrlHelpers;
import org.springframework.beans.factory.annotation.Autowired;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;

public class OpenIDConnectServiceImpl implements OpenIDConnectService {
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private OpenIDConnectManager oidcManager;

	@Override
	public OAuthClientIdAndSecret createOpenIDConnectClient(Long userId, OAuthClient oauthClient) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return oidcManager.createOpenIDConnectClient(userInfo, oauthClient);
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
	public OAuthClient updateOpenIDConnectClient(Long userId, OAuthClient oauthClient) {
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
		// result.setRevocation_endpoint(); // TODO
		result.setUserinfo_endpoint(issuer+UrlHelpers.OAUTH_2_USER_INFO);
		result.setJwks_uri(issuer+UrlHelpers.OAUTH_2_JWKS);
		result.setRegistration_endpoint(issuer+UrlHelpers.OAUTH_2_CLIENT);
		result.setScopes_supported(Arrays.asList(OAuthScope.values()));
		result.setResponse_types_supported(Arrays.asList(OAuthResponseType.values()));
		result.setGrant_types_supported(Collections.singletonList(OAuthGrantType.authorization_code)); // TODO support refresh_token grant type
		result.setSubject_types_supported(Arrays.asList(OIDCSubjectIdentifierType.values()));
		result.setId_token_signing_alg_values_supported(Arrays.asList(OIDCSigningAlgorithm.values()));
		result.setClaims_supported(Arrays.asList(OIDCClaimName.values()));
		result.setService_documentation("https://docs.synapse.org");
		result.setClaims_parameter_supported(true);
		return result;
	}

	@Override
	public JsonWebKeySet getOIDCJsonWebKeySet() {
		List<JWK> jwks = OIDCTokenUtil.getJSONWebKeySet();
		JsonWebKeySet result = new JsonWebKeySet();
		List<JsonWebKey> keys = new ArrayList<JsonWebKey>();
		result.setKeys(keys);
		for (JWK jwk : jwks) {
			if (JWSAlgorithm.RS256.equals(jwk.getAlgorithm())) {
				JsonWebKeyRSA rsaKey = new JsonWebKeyRSA();
				keys.add(rsaKey);
				// these would be set for all algorithms
				rsaKey.setKty(jwk.getKeyType().getValue());
				rsaKey.setUse(jwk.getKeyUse().toString());
				rsaKey.setKid(jwk.getKeyID());
				// these are specific to the RSA algorithm
				RSAKey jwkRsa = (RSAKey)jwk;
				rsaKey.setD(jwkRsa.getPrivateExponent().toString());
				rsaKey.setDp(jwkRsa.getFirstFactorCRTExponent().toString());
				rsaKey.setDq(jwkRsa.getSecondFactorCRTExponent().toString());
				rsaKey.setE(jwkRsa.getPublicExponent().toString());
				rsaKey.setN(jwkRsa.getModulus().toString());
				rsaKey.setP(jwkRsa.getFirstPrimeFactor().toString());
				rsaKey.setQ(jwkRsa.getSecondPrimeFactor().toString());
				rsaKey.setQi(jwkRsa.getFirstCRTCoefficient().toString());
			} else {
				// in the future we can add mappings for algorithms other than RSA
				throw new RuntimeException("Unsupported: "+jwk.getAlgorithm());
			}
		}
		return result;
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
			return oidcManager.getAccessToken(oauthEndpoint, verifiedClientId, authorizationCode, redirectUri);
		} else if (grantType==OAuthGrantType.refresh_token) {
			throw new IllegalArgumentException(OAuthGrantType.refresh_token+" unsupported.");
		} else {
			throw new IllegalArgumentException("Unsupported grant type"+grantType);
		}
	}
	
	private static JWT getAccessJWTFromAccessTokenHeader(String header) {
		try {
			return JWTParser.parse(header);
		} catch (ParseException e) {
			throw new UnauthenticatedException("Could not interpret access token.", e);
		}
	}

	@Override
	public Object getUserInfo(String accessTokenHeader, String oauthEndpoint) {
		JWT accessToken = getAccessJWTFromAccessTokenHeader(accessTokenHeader);
		return oidcManager.getUserInfo(accessToken, oauthEndpoint);
	}
	
	

}
