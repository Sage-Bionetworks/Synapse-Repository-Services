package org.sagebionetworks.auth.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.sagebionetworks.repo.manager.OIDCTokenUtil;
import org.sagebionetworks.repo.model.oauth.JsonWebKey;
import org.sagebionetworks.repo.model.oauth.JsonWebKeyRSA;
import org.sagebionetworks.repo.model.oauth.JsonWebKeySet;
import org.sagebionetworks.repo.model.oauth.OAuthAuthorizationResponse;
import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.repo.model.oauth.OAuthClientList;
import org.sagebionetworks.repo.model.oauth.OAuthGrantType;
import org.sagebionetworks.repo.model.oauth.OAuthResponseType;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCSigningAlgorithm;
import org.sagebionetworks.repo.model.oauth.OIDCSubjectIdentifierType;
import org.sagebionetworks.repo.model.oauth.OIDConnectConfiguration;
import org.sagebionetworks.repo.web.UrlHelpers;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;

public class OpenIDConnectServiceImpl implements OpenIDConnectService {

	@Override
	public OAuthClient createOpenIDConnectClient(Long userId, OAuthClient oauthClient) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OAuthClient getOpenIDConnectClient(Long userId, String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OAuthClientList listOpenIDConnectClients(Long userId, String nextPageToken) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OAuthClient updateOpenIDConnectClient(Long userId, OAuthClient oauthClient) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteOpenIDConnectClient(Long userId, String id) {
		// TODO Auto-generated method stub

	}

	private static final String ISSUER = "https://repo-prod.prod.sagebase.org"+UrlHelpers.AUTH_PATH; // TODO should this be passed in?
	
	@Override
	public OIDConnectConfiguration getOIDCConfiguration() {
		OIDConnectConfiguration result = new OIDConnectConfiguration();
		result.setIssuer(ISSUER);
		result.setAuthorization_endpoint("https://www.login.synapse.org/authorize"); // TODO this must be the URL of the login app'
		result.setToken_endpoint(ISSUER+UrlHelpers.OAUTH_2_TOKEN);
//		result.setRevocation_endpoint(); // TODO
		result.setUserinfo_endpoint(ISSUER+UrlHelpers.OAUTH_2_USER_INFO);
		result.setJwks_uri(ISSUER+UrlHelpers.OAUTH_2_JWKS);
		result.setRegistration_endpoint(ISSUER+UrlHelpers.OAUTH_2_CLIENT);
		result.setScopes_supported(Arrays.asList(OAuthScope.values()));
		result.setResponse_types_supported(Arrays.asList(OAuthResponseType.values()));
		result.setGrant_types_supported(Arrays.asList(OAuthGrantType.values()));
		result.setSubject_types_supported(Arrays.asList(OIDCSubjectIdentifierType.values()));
		result.setId_token_signing_alg_values_supported(Arrays.asList(OIDCSigningAlgorithm.values()));
		result.setClaims_supported(Arrays.asList(OIDCClaimName.values()));
		result.setService_documentation("https://docs.synapse.org");
		result.setClaims_parameter_supported(true);
		return result;
	}

	// TODO: when evaluating the claims object, how do we differentiate between a null value and a missing key?
	// They mean different things https://openid.net/specs/openid-connect-core-1_0.html#ClaimsParameter
	@Override
	public OAuthAuthorizationResponse authorizeClient() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JsonWebKeySet getOIDCJsonWebKeySet() {
		List<JWK> jwks = OIDCTokenUtil.extractJSONWebKeySet();
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

}
