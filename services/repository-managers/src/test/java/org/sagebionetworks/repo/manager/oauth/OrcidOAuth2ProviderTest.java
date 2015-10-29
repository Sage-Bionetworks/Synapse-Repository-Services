package org.sagebionetworks.repo.manager.oauth;

import static org.junit.Assert.assertEquals;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class OrcidOAuth2ProviderTest {
	
	String apiKey;
	String apiSecret;
	OrcidOAuth2Provider provider;
	
	@Before
	public void before(){
		apiKey = "fake-key";
		apiSecret = "fake-secret";
		provider = new OrcidOAuth2Provider(apiKey, apiSecret);
	}

	
	@Test
	public void testGetAuthorizationUrl() {
		String redirectUrl = "https://domain.com";
		String authUrl = provider.getAuthorizationUrl(redirectUrl);
		assertEquals("https://orcid.org/oauth/authorize?response_type=code&client_id=fake-key&redirect_uri=https%3A%2F%2Fdomain.com&scope=%2Fauthenticate", authUrl);
	}
	
	@Test
	public void testParseORCID() throws JSONException{
		JSONObject json = new JSONObject();
		json.put("orcid", "0000-1111-2222-3333");
		String orcid = OrcidOAuth2Provider.parseOrcidId(json.toString());
		assertEquals("0000-1111-2222-3333", orcid);
	}
	
	@Test(expected=RuntimeException.class)
	public void testParseORCIDNull() throws JSONException{
		JSONObject json = new JSONObject();
		OrcidOAuth2Provider.parseOrcidId(json.toString());
	}
	
}
