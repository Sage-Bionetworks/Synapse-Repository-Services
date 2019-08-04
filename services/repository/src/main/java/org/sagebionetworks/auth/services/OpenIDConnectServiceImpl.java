package org.sagebionetworks.auth.services;

import java.util.Arrays;

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

	// TODO: when evaluating the claims object, how do we differentiate between a null value and a missing key?  They mean different things https://openid.net/specs/openid-connect-core-1_0.html#ClaimsParameter
	@Override
	public OAuthAuthorizationResponse authorizeClient() {
		// TODO Auto-generated method stub
		return null;
	}

}
