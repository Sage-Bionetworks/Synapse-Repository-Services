package org.sagebionetworks.repo.manager.oauth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.UnsupportedEncodingException;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;

import io.jsonwebtoken.Jwts;

public class AccessTokenResponseTest {

	@Test
	public void testParseIdToken() throws JSONException {
		String idToken = Jwts.builder()
				.claim(OIDCClaimName.family_name.name(), "last")
				.claim(OIDCClaimName.given_name.name(), "first")
				.claim(OIDCClaimName.email_verified.name(), true)
				.claim(OIDCClaimName.email.name(), "first.last@domain.com")
				.claim(OIDCClaimName.sub.name(), "abcd")
				.compact() + "signature";
		
		// Call under test
		ProvidedUserInfo info = new AccessTokenResponse("", idToken, "").parseIdToken();
		
		assertEquals("last", info.getLastName());
		assertEquals("first", info.getFirstName());
		assertEquals("first.last@domain.com", info.getUsersVerifiedEmail());
		assertEquals("abcd", info.getSubject());
	}
	
	// Reproduce https://sagebionetworks.jira.com/browse/SYNSD-1231
	@Test
	public void testParseIdTokenBase64UrlEncoding() throws JSONException, UnsupportedEncodingException {
		// The following will encode into eyJhbGciOiJub25lIn0.eyJzdWIiOiJ-YWJjZCJ9.signature (A JWT is base64url encoded)
		// The ~ character in binary is 111111 (=63), using base64 url safe encoding it is translated to "-" (while it is translated to "/" in base64, see https://datatracker.ietf.org/doc/html/rfc4648#section-5)
		// A plain64 decoder cannot decode the character "-" correctly, so this ensures that we are using a base64 url safe decoder
		String idToken = Jwts.builder()
				.claim(OIDCClaimName.sub.name(), "~abcd")
				.compact() + "signature";
		
		// Call under test
		ProvidedUserInfo info = new AccessTokenResponse("", idToken, "").parseIdToken();
		
		assertEquals("~abcd", info.getSubject());
	}

	@Test
	public void testParseIdTokenWithEmailVerifiedNull() throws JSONException {
		String idToken = Jwts.builder()
				.claim(OIDCClaimName.email.name(), "first.last@domain.com")
				.compact() + "signature";

		ProvidedUserInfo info = new AccessTokenResponse("", idToken, "").parseIdToken();

		assertNull(info.getUsersVerifiedEmail());
	}

	@Test
	public void testParseIdTokenWithEmailVerifeidFalse() throws JSONException {
		String idToken = Jwts.builder()
				.claim(OIDCClaimName.email_verified.name(), false)
				.claim(OIDCClaimName.email.name(), "first.last@domain.com")
				.compact() + "signature";

		ProvidedUserInfo info = new AccessTokenResponse("", idToken, "").parseIdToken();

		assertNull(info.getUsersVerifiedEmail());
	}

	@Test
	public void testParseIdTokenWithNoIdToken() throws JSONException {

		String result = assertThrows(UnauthorizedException.class, () -> {
			new AccessTokenResponse("", null, "").parseIdToken();
		}).getMessage();

		assertEquals("The id_token was not included by the provider", result);

	}
	
	@Test
	public void testParseIdTokenWithMalformedIdToken() throws JSONException {

		String result = assertThrows(UnauthorizedException.class, () -> {
			new AccessTokenResponse("", "malformed", "").parseIdToken();
		}).getMessage();

		assertEquals("The id_token included by the provider was malformed", result);

	}
	
	@Test
	public void testParseIdTokenWithMalformedEncodingIdToken() throws JSONException {

		String result = assertThrows(UnauthorizedException.class, () -> {
			new AccessTokenResponse("", "malformed.token", "").parseIdToken();
		}).getMessage();

		assertEquals("The id_token included by the provider was malformed", result);

	}

}
