package org.sagebionetworks.repo.manager.oauth;

import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.repo.model.oauth.OAuthResponseType;
import org.sagebionetworks.repo.model.oauth.OIDCAuthorizationRequest;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestParameter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

public class OpenIDConnectManagerImplTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
	
	private static final String REDIRECT_URI = "https://data.braincommons.org/user/login/synapse/login";

	@Test
	public void testValidateAuthenticationRequest() {
		OAuthClient client = new OAuthClient();
		client.setRedirect_uris(Collections.singletonList(REDIRECT_URI));

		OIDCAuthorizationRequest authorizationRequest = new OIDCAuthorizationRequest();
		authorizationRequest.setRedirectUri(REDIRECT_URI);
		authorizationRequest.setResponseType(OAuthResponseType.code);
		
		OpenIDConnectManagerImpl.validateAuthenticationRequest(authorizationRequest, client);
		
		authorizationRequest.setRedirectUri("some invalid uri");
		
		try {
			OpenIDConnectManagerImpl.validateAuthenticationRequest(authorizationRequest, client);
			fail("Exception expected.");
		} catch (IllegalArgumentException e) {
			// as expected
		}
	}
	
	@Test
	public void testDeserializeClaims() throws Exception {
		OIDCAuthorizationRequest ar = new OIDCAuthorizationRequest();
		ar.setClientId("100001");
		ar.setScope("openid");
		OIDCClaimsRequestParameter rp = new OIDCClaimsRequestParameter();
		Map<OIDCClaimName, OIDCClaimsRequestDetails> claims = new HashMap<OIDCClaimName, OIDCClaimsRequestDetails>();
		OIDCClaimsRequestDetails teamList = new OIDCClaimsRequestDetails();
		teamList.setValues(Collections.singletonList("101"));
		claims.put(OIDCClaimName.team, teamList);
		OIDCClaimsRequestDetails essential = new OIDCClaimsRequestDetails();
		essential.setEssential(true);
		claims.put(OIDCClaimName.given_name, essential);
		claims.put(OIDCClaimName.family_name, essential);
		claims.put(OIDCClaimName.email, essential);
		claims.put(OIDCClaimName.company, essential);
		rp.setId_token(claims);
		rp.setUserinfo(claims);
		ar.setClaims(rp);
		ar.setResponseType(OAuthResponseType.code);
		ar.setRedirectUri("foo");
		
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
		try {
			ar.writeToJSONObject(adapter);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}

		System.out.println(adapter.toJSONString());
		
		String serializedAuthorizationRequest = "{\"clientId\": \"100001\", \"scope\": \"openid\", \"claims\": {\"id_token\": {\"team\": {\"values\": [\"101\"]}, \"given_name\": {\"essential\": \"true\"}, \"family_name\": {\"essential\": \"true\"}, \"email\": {\"essential\": \"true\"}, \"company\": {\"essential\": \"false\"}}, \"userinfo\": {\"team\": {\"values\": [\"101\"]}, \"given_name\": {\"essential\": \"true\"}, \"family_name\": {\"essential\": \"true\"}, \"email\": {\"essential\": \"true\"}, \"company\": {\"essential\": \"false\"}}}, \"responseType\": \"code\", \"redirectUri\": \"https://data.braincommons.org/user/login/synapse/login\", \"nonce\": \"abcdefg\"}";
		adapter = new JSONObjectAdapterImpl(serializedAuthorizationRequest);
		ar.initializeFromJSONObject(adapter);
	}

}
