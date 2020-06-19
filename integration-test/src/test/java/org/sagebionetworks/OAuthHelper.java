package org.sagebionetworks;

import java.util.HashMap;
import java.util.UUID;

import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.oauth.OAuthAuthorizationResponse;
import org.sagebionetworks.repo.model.oauth.OAuthGrantType;
import org.sagebionetworks.repo.model.oauth.OAuthResponseType;
import org.sagebionetworks.repo.model.oauth.OIDCAuthorizationRequest;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequest;
import org.sagebionetworks.repo.model.oauth.OIDCTokenResponse;

public class OAuthHelper {
	
	public static String getAccessToken(
			SynapseClient authenticatedClient, 
			SynapseClient anonymousClient, 
			String oauthClientId, 
			String oauthClientSecret,
			String redirectUri, 
			String scopes) throws SynapseException {
		OIDCAuthorizationRequest authorizationRequest = new OIDCAuthorizationRequest();
		authorizationRequest.setClientId(oauthClientId);
		authorizationRequest.setRedirectUri(redirectUri);
		authorizationRequest.setResponseType(OAuthResponseType.code);
		authorizationRequest.setScope(scopes);
		OIDCClaimsRequest claims = new OIDCClaimsRequest();
		claims.setId_token(new HashMap<>());
		claims.setUserinfo(new HashMap<>());
		authorizationRequest.setClaims(claims);
		String nonce = UUID.randomUUID().toString();
		authorizationRequest.setNonce(nonce);
		
		// Note that here we use the authenticated client to create an access token
		// for User1
		OAuthAuthorizationResponse oauthAuthorizationResponse = authenticatedClient.authorizeClient(authorizationRequest);

		// Note, we use Basic auth to authorize the client when asking for an access token
		OIDCTokenResponse tokenResponse = null;
		try {
			anonymousClient.setBasicAuthorizationCredentials(oauthClientId, oauthClientSecret);
			tokenResponse = anonymousClient.getTokenResponse(OAuthGrantType.authorization_code, 
					oauthAuthorizationResponse.getAccess_code(), redirectUri, null, null, null);
			return tokenResponse.getAccess_token();
		} finally {
			anonymousClient.removeAuthorizationHeader();
		}

	}
	
}
