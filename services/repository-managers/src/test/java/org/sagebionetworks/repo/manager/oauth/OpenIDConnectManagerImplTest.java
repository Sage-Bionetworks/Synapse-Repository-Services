package org.sagebionetworks.repo.manager.oauth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.repo.model.oauth.OAuthResponseType;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.oauth.OIDCAuthorizationRequest;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;

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
	public void testParseScopeString() throws Exception {
		// happy case
		List<OAuthScope> scopes = OpenIDConnectManagerImpl.parseScopeString("openid openid");
		List<OAuthScope> expected = new ArrayList<OAuthScope>();
		expected.add(OAuthScope.openid);
		expected.add(OAuthScope.openid);
		assertEquals(expected, scopes);
		
		try {
			OpenIDConnectManagerImpl.parseScopeString("openid foo");
			fail("IllegalArgumentException expected.");
		} catch (IllegalArgumentException e) {
			System.out.println(e.getMessage());
			// as expected
		}
		
		// what if url encoded?
		scopes = OpenIDConnectManagerImpl.parseScopeString("openid+openid");
		assertEquals(expected, scopes);
		
		scopes = OpenIDConnectManagerImpl.parseScopeString("openid%20openid");
		assertEquals(expected, scopes);
		
	}
	
	@Test
	public void testGetClaimsMapFromClaimsRequestParam() throws Exception {
		String claimsString = "{\"somekey\":{\"team\":{\"values\":[\"101\"]},\"given_name\":null,\"family_name\":{\"essential\":true,\"value\":\"foo\"}}}";
		Map<OIDCClaimName,OIDCClaimsRequestDetails> map = OpenIDConnectManagerImpl.getClaimsMapFromClaimsRequestParam(claimsString, "somekey");
		{
			assertTrue(map.containsKey(OIDCClaimName.team));
			OIDCClaimsRequestDetails details = map.get(OIDCClaimName.team);
			OIDCClaimsRequestDetails expectedDetails = new OIDCClaimsRequestDetails();
			expectedDetails.setValues(Collections.singletonList("101"));
			assertEquals(expectedDetails, details);
		}
		{
			assertTrue(map.containsKey(OIDCClaimName.given_name));
			assertNull(map.get(OIDCClaimName.given_name));
		}
		{
			assertTrue(map.containsKey(OIDCClaimName.family_name));
			OIDCClaimsRequestDetails details = map.get(OIDCClaimName.family_name);
			OIDCClaimsRequestDetails expectedDetails = new OIDCClaimsRequestDetails();
			expectedDetails.setEssential(true);
			expectedDetails.setValue("foo");
			assertEquals(expectedDetails, details);
		}
		// what if key is omitted?
		claimsString = "{\"somekey\":{\"team\":{\"values\":[\"101\"]},\"given_name\":null,\"family_name\":{\"essential\":true,\"value\":\"foo\"}}}";
		map = OpenIDConnectManagerImpl.getClaimsMapFromClaimsRequestParam(claimsString, "some other key");
		assertTrue(map.isEmpty());
	}

}
