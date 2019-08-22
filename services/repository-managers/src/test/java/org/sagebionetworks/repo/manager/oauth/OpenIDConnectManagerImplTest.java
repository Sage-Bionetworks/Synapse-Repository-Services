package org.sagebionetworks.repo.manager.oauth;

import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.repo.model.oauth.OAuthResponseType;
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
	public void testGetClaimsMapFromClaimsRequestParam() throws Exception {
		String claims = "{\"id_token\":{\"team\":{\"values\":[\"101\"]},\"given_name\":{\"essential\":true},\"family_name\":{\"essential\":true},\"email\":{\"essential\":true},\"company\":{\"essential\":false}},"+
				"\"userinfo\":{\"team\":{\"values\":[\"101\"]},\"given_name\":{\"essential\":true},\"family_name\":{\"essential\":true},\"email\":{\"essential\":true},\"company\":{\"essential\":false}}}";
		Map<OIDCClaimName,OIDCClaimsRequestDetails> map = OpenIDConnectManagerImpl.getClaimsMapFromClaimsRequestParam(claims, "id_token");
	}

}
