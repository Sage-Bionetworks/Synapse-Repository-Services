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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.StringUtils;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
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

@ExtendWith(ITTestExtension.class)
public class ITPersonalAccessTokenTest {

	private static SynapseAdminClient synapseAnonymous;

	private SynapseClient synapse;
	
	public ITPersonalAccessTokenTest(SynapseClient synapse) {
		this.synapse = synapse;
	}
	
	@BeforeAll
	public static void beforeClass() throws Exception {	
		synapseAnonymous = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(synapseAnonymous);
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
		String personalAccessToken = synapse.createPersonalAccessToken(tokenRequest);
		assertTrue(StringUtils.isNotBlank(personalAccessToken));

		UserProfile profile = synapse.getMyProfile();

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
		AccessTokenRecordList records = synapse.retrievePersonalAccessTokenRecords(null);
		assertEquals(1, records.getResults().size());
		assertNull(records.getNextPageToken());

		// method under test -- get one record
		AccessTokenRecord record = synapse.retrievePersonalAccessTokenRecord(records.getResults().get(0).getId());
		assertEquals(records.getResults().get(0), record);
		assertEquals(record.getName(), tokenRequest.getName());
		assertEquals(record.getScopes(), tokenRequest.getScope());
		assertEquals(record.getUserInfoClaims(), tokenRequest.getUserInfoClaims());

		// method under test -- revoke the token
		synapse.revokePersonalAccessToken(record.getId());

		assertThrows(SynapseNotFoundException.class, () -> synapse.retrievePersonalAccessTokenRecord(record.getId()));

		// The token should no longer work
		try {
			synapseAnonymous.setBearerAuthorizationToken(personalAccessToken);
			assertThrows(SynapseUnauthorizedException.class, () -> synapseAnonymous.getUserInfoAsJSON());
		} finally {
			synapseAnonymous.removeAuthorizationHeader();
		}

	}

}
