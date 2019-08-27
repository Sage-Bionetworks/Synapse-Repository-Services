package org.sagebionetworks.repo.manager.oauth;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import java.util.Map;

import org.junit.Test;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;

import net.minidev.json.JSONObject;

public class ClaimsJsonUtilTest {

	@Test
	public void testGetClaimsMapFromJSONObject() {
		JSONObject claimsJson = new JSONObject();
		claimsJson.put(OIDCClaimName.team.name(), "{\"values\":[\"101\"]}");
		claimsJson.put(OIDCClaimName.given_name.name(), null);
		claimsJson.put(OIDCClaimName.family_name.name(), "{\"essential\":true,\"value\":\"foo\"}");
		claimsJson.put("unrecognized-key", null);
		Map<OIDCClaimName, OIDCClaimsRequestDetails> actual = ClaimsJsonUtil.getClaimsMapFromJSONObject(claimsJson, true);
		OIDCClaimsRequestDetails expected = new OIDCClaimsRequestDetails();
		expected.setValues(Collections.singletonList("101"));
		assertEquals(expected, actual.get(OIDCClaimName.team));
		
		assertNull(actual.get(OIDCClaimName.given_name));
		
		expected = new OIDCClaimsRequestDetails();
		expected.setEssential(true);
		expected.setValue("foo");
		assertEquals(expected, actual.get(OIDCClaimName.family_name));
		
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGetClaimsMapFromJSONObjectStrict() {
		JSONObject claimsJson = new JSONObject();
		claimsJson.put("unrecognized-key", null);
		ClaimsJsonUtil.getClaimsMapFromJSONObject(claimsJson, false);
	}
	
//	@Test
//	public void testGetScopeAndClaims() throws Exception {		
//		
//		JWT jwt = JWTParser.parse(accessToken);
//
//		List<OAuthScope> actualScopes = ClaimsJsonUtil.getScopeFromClaims(jwt.getJWTClaimsSet());
//		Map<OIDCClaimName,OIDCClaimsRequestDetails> actualClaims = ClaimsJsonUtil.getOIDCClaimsFromClaimSet(jwt.getJWTClaimsSet());
//		
//		assertEquals(grantedScopes, actualScopes);
//		assertEquals(expectedClaims, actualClaims);
//	}
	

}
