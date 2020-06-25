package org.sagebionetworks.repo.manager.oauth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONObject;
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
		claimsJson.put(OIDCClaimName.company.name(), "NULL");
		claimsJson.put(OIDCClaimName.family_name.name(), "{\"essential\":true,\"value\":\"foo\"}");
		claimsJson.put("unrecognized-key", "some value");

		// method under test
		Map<OIDCClaimName, OIDCClaimsRequestDetails> actual = ClaimsJsonUtil.getClaimsMapFromJSONObject(claimsJson);

		OIDCClaimsRequestDetails teamDetails = new OIDCClaimsRequestDetails();
		teamDetails.setValues(Collections.singletonList("101"));
		assertEquals(teamDetails, actual.get(OIDCClaimName.team));

		assertNull(actual.get(OIDCClaimName.given_name));
		assertTrue(actual.containsKey(OIDCClaimName.given_name)); // given_name IS in the map, but with a null value

		assertNull(actual.get(OIDCClaimName.company));
		assertTrue(actual.containsKey(OIDCClaimName.company)); // company IS in the map, but with a null value

		OIDCClaimsRequestDetails familyNameDetails = new OIDCClaimsRequestDetails();
		familyNameDetails.setEssential(true);
		familyNameDetails.setValue("foo");
		assertEquals(familyNameDetails, actual.get(OIDCClaimName.family_name));
	}

	@Test
	public void testGetClaimsMapFromClaimsRequestParam() throws Exception {
		String claimsString = "{\"somekey\":{\"team\":{\"values\":[\"101\"]},\"given_name\":null,\"family_name\":{\"essential\":true,\"value\":\"foo\"}}}";
		Map<OIDCClaimName,OIDCClaimsRequestDetails> map = ClaimsJsonUtil.getClaimsMapFromClaimsRequestParam(claimsString, "somekey");
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
		map = ClaimsJsonUtil.getClaimsMapFromClaimsRequestParam(claimsString, "some other key");
		assertTrue(map.isEmpty());
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
		
		// we need to simulate the round trip to a serialized form and back
		String token = Jwts.builder().setClaims(claims).compact();
		// while we're at it, let's check that the serialized representation is as expected
		String expectedSerializedBody = "{\"access\":{\"scope\":[\"openid\"],\"oidc_claims\":{\"team\":{\"values\":[\"101\"],\"value\":null,\"essential\":null},\"given_name\":null,\"family_name\":{\"values\":null,\"value\":\"foo\",\"essential\":true}}}}";
		String actualSerializedBody = new String(Base64.decodeBase64(token.split("\\.")[1]));
		assertEquals(expectedSerializedBody, actualSerializedBody);
		Claims parsedClaims = Jwts.parser().parseClaimsJwt(token).getBody();
		
		// ... Now we extract the results and show they match
		// method under test
		assertEquals(scopes, ClaimsJsonUtil.getScopeFromClaims(parsedClaims));
		// method under test
		assertEquals(oidcClaims, ClaimsJsonUtil.getOIDCClaimsFromClaimSet(parsedClaims));
	}
	

}
