package org.sagebionetworks.repo.manager.oauth;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class OrcidOAuth2ProviderTest {
	
	private String apiKey;
	private String apiSecret;
	private OrcidOAuth2Provider provider;
	private OIDCConfig mockConfig;
	private String authEndpoint = "https://auth_url.org";
	private String tokenEndpoint = "https://token_url.org";
	
	@BeforeEach
	public void before(){
		apiKey = "fake-key";
		apiSecret = "fake-secret";
		mockConfig = Mockito.mock(OIDCConfig.class);
		
		when(mockConfig.getAuthorizationEndpoint()).thenReturn(authEndpoint);
		when(mockConfig.getTokenEndpoint()).thenReturn(tokenEndpoint);
		
		provider = new OrcidOAuth2Provider(apiKey, apiSecret, mockConfig);
	}

	
	@Test
	public void testGetAuthorizationUrl() {
		String redirectUrl = "https://domain.com";
		String authUrl = provider.getAuthorizationUrl(redirectUrl);
		assertEquals(authEndpoint + "?response_type=code&client_id=fake-key&redirect_uri=https%3A%2F%2Fdomain.com&scope=%2Fauthenticate", authUrl);
	}
	
	@Test
	public void testParseORCID() throws JSONException{
		JSONObject json = new JSONObject();
		json.put("orcid", "0000-1111-2222-3333");
		String orcid = OrcidOAuth2Provider.parseOrcidId(json.toString());
		assertEquals("0000-1111-2222-3333", orcid);
	}
	
	@Test
	public void testParseORCIDNull() throws JSONException{
		JSONObject json = new JSONObject();
		
		assertThrows(RuntimeException.class, () -> {			
			OrcidOAuth2Provider.parseOrcidId(json.toString());
		});
	}
	
}
