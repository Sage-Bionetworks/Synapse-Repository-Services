package org.sagebionetworks.repo.manager.oauth;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.junit.Test;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

public class ClaimsJsonUtilTest {

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
		Claims claims = ClaimsWithAuthTime.newClaims();
		
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
