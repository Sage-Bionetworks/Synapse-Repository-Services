package org.sagebionetworks;

import java.util.UUID;

import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.auth.AccessToken;
import org.sagebionetworks.repo.model.auth.NewIntegrationTestUser;
import org.sagebionetworks.repo.model.oauth.JsonWebKey;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;

/**
 * Holds helpers for setting up integration tests
 */
public class SynapseClientHelper {
	public static void setEndpoints(SynapseClient client) {
		client.setAuthEndpoint(StackConfigurationSingleton.singleton().getAuthenticationServicePrivateEndpoint());
		client.setRepositoryEndpoint(StackConfigurationSingleton.singleton().getRepositoryServiceEndpoint());
		client.setFileEndpoint(StackConfigurationSingleton.singleton().getFileServiceEndpoint());
	}
	
	/**
	 * Creates a user that can login with a session token
	 * 
	 * @param newUserClient The client to log the new user in
	 * @return The ID of the user
	 */
	public static Long createUser(SynapseAdminClient client, SynapseClient newUserClient) throws SynapseException, JSONObjectAdapterException {
		return createUser(client, newUserClient, UUID.randomUUID().toString());
	}

	public static Long createUser(SynapseAdminClient client, SynapseClient newUserClient, String username) throws SynapseException, JSONObjectAdapterException {
		return createUser(client, newUserClient, username, "password"+UUID.randomUUID().toString());
	}
	
	public static Long createUser(SynapseAdminClient client, SynapseClient newUserClient, String username, String password) throws SynapseException, JSONObjectAdapterException {
		return createUser(client, newUserClient, username, password, UUID.randomUUID().toString() + "@sagebase.org");
	}
	
	public static String getSubjectFromJWTAccessToken(String accessToken) {
		String[] pieces = accessToken.split("\\.");
		if (pieces.length!=3) throw new IllegalArgumentException("Expected three sections of the token but found "+pieces.length);
		String unsignedToken = pieces[0]+"."+pieces[1]+".";
		Jwt<Header,Claims> unsignedJwt = Jwts.parser().parseClaimsJwt(unsignedToken);

		return unsignedJwt.getBody().getSubject();
	}

	public static Long createUser(SynapseAdminClient client, SynapseClient newUserClient, String username, String password, String email) throws SynapseException, JSONObjectAdapterException {
		if (newUserClient == null) {
			newUserClient = new SynapseClientImpl();
		}
		setEndpoints(newUserClient);

		NewIntegrationTestUser nu = new NewIntegrationTestUser();
		nu.setTou(true);
		nu.setEmail(email);
		nu.setUsername(username);
		nu.setPassword(password);
		AccessToken accessToken = client.createUser(nu);
		
		String accessTokenSubject = getSubjectFromJWTAccessToken(accessToken.getAccessToken());
		Long principalId = Long.parseLong(accessTokenSubject);
		
		newUserClient.setBearerAuthorizationToken(accessToken.getAccessToken());
		client.setCertifiedUserStatus(accessTokenSubject, true);
		return principalId;
	}
}
