package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.client.exceptions.SynapseUnauthorizedException;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.auth.AccessTokenGenerationRequest;
import org.sagebionetworks.repo.model.auth.AccessTokenRecord;
import org.sagebionetworks.repo.model.auth.AccessTokenRecordList;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;

public class ITPersonalAccessTokenTest {

	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapseOne;
	private static SynapseAdminClient synapseAnonymous;
	private static Long user1ToDelete;

	@BeforeAll
	public static void beforeClass() throws Exception {
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUsername(StackConfigurationSingleton.singleton().getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfigurationSingleton.singleton().getMigrationAdminAPIKey());
		adminSynapse.clearAllLocks();
		synapseOne = new SynapseClientImpl();
		SynapseClientHelper.setEndpoints(synapseOne);
		user1ToDelete = SynapseClientHelper.createUser(adminSynapse, synapseOne);
		
		synapseAnonymous = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(synapseAnonymous);
	}
	
	@AfterAll
	public static void afterClass() throws Exception {
		try {
			if (user1ToDelete!=null) adminSynapse.deleteUser(user1ToDelete);
		} catch (SynapseException e) { }
	}

	@Test
	public void testRoundTrip() throws Exception {
		AccessTokenGenerationRequest tokenRequest = new AccessTokenGenerationRequest();
		tokenRequest.setName("This is my token name");
		tokenRequest.setScope(Arrays.asList(OAuthScope.view, OAuthScope.openid));
		Map<String, OIDCClaimsRequestDetails> claims = new HashMap<>();
		claims.put(OIDCClaimName.userid.name(), null);
		claims.put(OIDCClaimName.email.name(), null);
		claims.put(OIDCClaimName.is_certified.name(), null);
		OIDCClaimsRequestDetails teamClaimRequestDetails = new OIDCClaimsRequestDetails();
		teamClaimRequestDetails.setValues(Collections.singletonList("2"));
		claims.put(OIDCClaimName.team.name(), teamClaimRequestDetails);
		tokenRequest.setUserInfoClaims(claims);

		// method under test - create token
		String personalAccessToken = synapseOne.createPersonalAccessToken(tokenRequest);
		assertTrue(StringUtils.isNotBlank(personalAccessToken));

		UserProfile profile = synapseOne.getMyProfile();

		// Use the personal access token as a bearer token
		try {
			synapseAnonymous.setBearerAuthorizationToken(personalAccessToken);
			JSONObject userInfo = synapseAnonymous.getUserInfoAsJSON();
			// check userInfo
			assertEquals(profile.getOwnerId(), (String)userInfo.get("userid"));
			assertEquals(profile.getEmails().get(0), (String)userInfo.get("email"));
			assertTrue((Boolean)userInfo.get("is_certified"));
			assertEquals(0, ((JSONArray)userInfo.get("team")).length());

			// attempt to make a request that requires scope that this token doesn't have
			assertThrows(SynapseForbiddenException.class, () -> synapseAnonymous.createPersonalAccessToken(new AccessTokenGenerationRequest()));
		} finally {
			synapseAnonymous.removeAuthorizationHeader();
		}

		// method under test -- get all records
		AccessTokenRecordList records = synapseOne.retrievePersonalAccessTokenRecords(null);
		assertEquals(1, records.getResults().size());
		assertNull(records.getNextPageToken());

		// method under test -- get one record
		AccessTokenRecord record = synapseOne.retrievePersonalAccessTokenRecord(records.getResults().get(0).getId());
		assertEquals(records.getResults().get(0), record);
		assertEquals(record.getName(), tokenRequest.getName());
		assertEquals(record.getScopes(), tokenRequest.getScope());
		assertEquals(record.getUserInfoClaims(), tokenRequest.getUserInfoClaims());

		// method under test -- revoke the token
		synapseOne.revokePersonalAccessToken(record.getId());

		assertThrows(SynapseNotFoundException.class, () -> synapseOne.retrievePersonalAccessTokenRecord(record.getId()));

		// The token should no longer work
		try {
			synapseAnonymous.setBearerAuthorizationToken(personalAccessToken);
			assertThrows(SynapseUnauthorizedException.class, () -> synapseAnonymous.getUserInfoAsJSON());
		} finally {
			synapseAnonymous.removeAuthorizationHeader();
		}

	}

}
