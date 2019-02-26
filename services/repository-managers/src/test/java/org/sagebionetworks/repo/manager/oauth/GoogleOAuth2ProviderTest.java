package org.sagebionetworks.repo.manager.oauth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.sagebionetworks.repo.manager.oauth.GoogleOAuth2Provider.EMAIL;
import static org.sagebionetworks.repo.manager.oauth.GoogleOAuth2Provider.FAMILY_NAME;
import static org.sagebionetworks.repo.manager.oauth.GoogleOAuth2Provider.GIVEN_NAME;
import static org.sagebionetworks.repo.manager.oauth.GoogleOAuth2Provider.ID;
import static org.sagebionetworks.repo.manager.oauth.GoogleOAuth2Provider.VERIFIED_EMAIL;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.oauth.ProvidedUserInfo;

public class GoogleOAuth2ProviderTest {
	
	String apiKey;
	String apiSecret;
	GoogleOAuth2Provider provider;
	
	@Before
	public void before(){
		apiKey = "fake key";
		apiSecret = "fake secret";
		provider = new GoogleOAuth2Provider(apiKey, apiSecret);
	}

	
	@Test
	public void testGetAuthorizationUrl(){
		String redirectUrl = "https://domain.com";
		String authUrl = provider.getAuthorizationUrl(redirectUrl);
		assertEquals("https://accounts.google.com/o/oauth2/auth?response_type=code&client_id=fake key&redirect_uri=https%3A%2F%2Fdomain.com&scope=email", authUrl);
	}
	
	@Test
	public void testParserResponseBody() throws JSONException{
		JSONObject json = new JSONObject();
		json.put(FAMILY_NAME, "last");
		json.put(GIVEN_NAME, "first");
		json.put(VERIFIED_EMAIL, true);
		json.put(EMAIL, "first.last@domain.com");
		json.put(ID, "123");
		ProvidedUserInfo info = GoogleOAuth2Provider.parserResponseBody(json.toString());
		assertNotNull(info);
		assertEquals("last", info.getLastName());
		assertEquals("first", info.getFirstName());
		assertEquals("first.last@domain.com", info.getUsersVerifiedEmail());
		assertEquals("123", info.getProvidersUserId());
	}
	
	@Test
	public void testParserResponseBodyVerifiedNull() throws JSONException{
		JSONObject json = new JSONObject();
		// This case does not have a verified email so no email should be returned.
		json.put(EMAIL, "first.last@domain.com");
		ProvidedUserInfo info = GoogleOAuth2Provider.parserResponseBody(json.toString());
		assertNotNull(info);
		assertEquals("Email was not verified and should not have been returned.", null, info.getUsersVerifiedEmail());
	}
	
	@Test
	public void testParserResponseBodyEmailNotVerifeid() throws JSONException{
		JSONObject json = new JSONObject();
		// email not verified.
		json.put(VERIFIED_EMAIL, false);
		json.put(EMAIL, "first.last@domain.com");
		ProvidedUserInfo info = GoogleOAuth2Provider.parserResponseBody(json.toString());
		assertNotNull(info);
		assertEquals("Email was not verified and should not have been returned.",null, info.getUsersVerifiedEmail());
	}
}
