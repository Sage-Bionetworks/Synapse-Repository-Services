package org.sagebionetworks.repo.manager.oauth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.sagebionetworks.repo.manager.oauth.OpenIdApi.EMAIL;
import static org.sagebionetworks.repo.manager.oauth.OpenIdApi.EMAIL_VERIFIED;
import static org.sagebionetworks.repo.manager.oauth.OpenIdApi.FAMILY_NAME;
import static org.sagebionetworks.repo.manager.oauth.OpenIdApi.GIVEN_NAME;
import static org.sagebionetworks.repo.manager.oauth.OpenIdApi.SUB;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

public class OpenIdApiTest {

	@Test
	public void testParserResponseBody() throws JSONException{
		JSONObject json = new JSONObject();
		json.put(FAMILY_NAME, "last");
		json.put(GIVEN_NAME, "first");
		json.put(EMAIL_VERIFIED, true);
		json.put(EMAIL, "first.last@domain.com");
		json.put(SUB, "abcd");
		ProvidedUserInfo info = OpenIdApi.parseUserInfo(json.toString());
		assertNotNull(info);
		assertEquals("last", info.getLastName());
		assertEquals("first", info.getFirstName());
		assertEquals("first.last@domain.com", info.getUsersVerifiedEmail());
		assertEquals("abcd", info.getSubject());
	}
	
	@Test
	public void testParserResponseBodyVerifiedNull() throws JSONException{
		JSONObject json = new JSONObject();
		// This case does not have a verified email so no email should be returned.
		json.put(EMAIL, "first.last@domain.com");
		ProvidedUserInfo info = OpenIdApi.parseUserInfo(json.toString());
		assertNotNull(info);
		assertEquals("Email was not verified and should not have been returned.", null, info.getUsersVerifiedEmail());
	}
	
	@Test
	public void testParserResponseBodyEmailNotVerifeid() throws JSONException{
		JSONObject json = new JSONObject();
		// email not verified.
		json.put(EMAIL_VERIFIED, false);
		json.put(EMAIL, "first.last@domain.com");
		ProvidedUserInfo info = OpenIdApi.parseUserInfo(json.toString());
		assertNotNull(info);
		assertEquals("Email was not verified and should not have been returned.",null, info.getUsersVerifiedEmail());
	}

}
