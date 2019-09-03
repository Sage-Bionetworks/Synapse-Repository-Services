package org.sagebionetworks.repo.manager.oauth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.junit.Test;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

public class ClaimsJsonUtilTest {

	@Test
	public void testGetClaimsMapFromJSONObject() {
		JSONObject claimsJson = new JSONObject();
		claimsJson.put(OIDCClaimName.team.name(), "{\"values\":[\"101\"]}");
		claimsJson.put(OIDCClaimName.given_name.name(), "null");
		claimsJson.put(OIDCClaimName.family_name.name(), "{\"essential\":true,\"value\":\"foo\"}");
		claimsJson.put("unrecognized-key", "some value");
		
		// method under test
		Map<OIDCClaimName, OIDCClaimsRequestDetails> actual = ClaimsJsonUtil.getClaimsMapFromJSONObject(claimsJson);
		
		OIDCClaimsRequestDetails expected = new OIDCClaimsRequestDetails();
		expected.setValues(Collections.singletonList("101"));
		assertEquals(expected, actual.get(OIDCClaimName.team));
		
		assertNull(actual.get(OIDCClaimName.given_name));
		assertTrue(actual.containsKey(OIDCClaimName.given_name)); // given_name IS in the map, but with a null value
		
		expected = new OIDCClaimsRequestDetails();
		expected.setEssential(true);
		expected.setValue("foo");
		assertEquals(expected, actual.get(OIDCClaimName.family_name));
	}
	
	@Test
	public void testAddAndExtractScopeAndClaims() throws Exception {		
		// first we encode scope and claims into a JWT claims field...
		List<OAuthScope> scopes = new ArrayList<OAuthScope>();
		scopes.add(OAuthScope.openid);
		
		Map<OIDCClaimName, OIDCClaimsRequestDetails> oidcClaims = new HashMap<OIDCClaimName, OIDCClaimsRequestDetails>();
		{
			OIDCClaimsRequestDetails details = new OIDCClaimsRequestDetails();
			details.setValues(Collections.singletonList("101"));
			oidcClaims.put(OIDCClaimName.team, details);
		}
		oidcClaims.put(OIDCClaimName.given_name, (OIDCClaimsRequestDetails)null);
		{
			OIDCClaimsRequestDetails details = new OIDCClaimsRequestDetails();
			details.setValue("foo");
			details.setEssential(true);
			oidcClaims.put(OIDCClaimName.family_name, details);
		}
		Claims claims = Jwts.claims();
		
		// method under test
		ClaimsJsonUtil.addAccessClaims(scopes, oidcClaims, claims);
		
		// ... then we extract the results and show they match
		assertEquals(scopes, ClaimsJsonUtil.getScopeFromClaims(claims));
		assertEquals(oidcClaims, ClaimsJsonUtil.getOIDCClaimsFromClaimSet(claims));
	}
	

}
