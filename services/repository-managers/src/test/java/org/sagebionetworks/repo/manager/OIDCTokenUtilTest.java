package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.json.JSONArray;
import org.junit.Test;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;

import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;

public class OIDCTokenUtilTest {

	
	@Test
	public void testOIDCTokenGeneration() throws Exception {
		String user = "101";
		String oauthClientId = "client-01234";
		long now = System.currentTimeMillis();
		long auth_time = (new Date()).getTime()/1000L;
		String tokenId = UUID.randomUUID().toString();
		String nonce = UUID.randomUUID().toString();
		JSONArray teamIds = new JSONArray();
		teamIds.put("9876543");
		Map<OIDCClaimName, String> userClaims = new HashMap<OIDCClaimName, String>();
		userClaims.put(OIDCClaimName.team, teamIds.toString());
		userClaims.put(OIDCClaimName.given_name, "A User");
		userClaims.put(OIDCClaimName.email, "user@synapse.org");
		userClaims.put(OIDCClaimName.company, "University of Example");
		
		

		String oidcToken = OIDCTokenUtil.createOIDCIdToken(user, oauthClientId, now, nonce, auth_time, tokenId, userClaims);
		JWT jwt = JWTParser.parse(oidcToken);
		
		assertTrue(jwt instanceof SignedJWT);

	    SignedJWT signedJWT = (SignedJWT)jwt;
		
		// Validate, per https://openid.net/specs/openid-connect-core-1_0.html#IDTokenValidation
	    
		// The Issuer Identifier for the OpenID Provider (which is typically obtained during Discovery) MUST exactly match the value of the iss (issuer) Claim.
	    JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
	    assertEquals("https://repo-prod.prod.sagebase.org/auth/v1", claimsSet.getIssuer());

	    // The Client MUST validate that the aud (audience) Claim contains its client_id value registered at the Issuer identified by 
	    // the iss (issuer) Claim as an audience. The aud (audience) Claim MAY contain an array with more than one element. The ID 
	    // Token MUST be rejected if the ID Token does not list the Client as a valid audience, or if it contains additional 
	    // audiences not trusted by the Client.
	    assertEquals(Collections.singletonList(oauthClientId), claimsSet.getAudience());
		// If the ID Token contains multiple audiences, the Client SHOULD verify that an azp Claim is present.
	    // 	(Note the above verifies that there are NOT multiple audiences.)

		// If an azp (authorized party) Claim is present, the Client SHOULD verify that its client_id is the Claim Value.
	    assertNull(claimsSet.getClaim("azp")); // i.e. verify no azp Claim

		// If the ID Token is received via direct communication between the Client and the Token Endpoint (which it is in this flow), 
	    // the TLS server validation MAY be used to validate the issuer in place of checking the token signature. The Client MUST 
	    // validate the signature of all other ID Tokens according to JWS using the algorithm specified in the JWT alg Header 
	    // Parameter. The Client MUST use the keys provided by the Issuer.
	    List<JWK> jwks = OIDCTokenUtil.getJSONWebKeySet();
	    JWSVerifier verifier = new RSASSAVerifier((RSAKey)jwks.get(0));
	    assertTrue(signedJWT.verify(verifier));
	    
	    // by the way, the key ID in the token header should match that in the JWK
	    assertEquals(signedJWT.getHeader().getKeyID(), jwks.get(0).getKeyID());

		// The alg value SHOULD be the default of RS256 or the algorithm sent by the Client in the id_token_signed_response_alg parameter during Registration.
	    assertEquals("RS256", signedJWT.getHeader().getAlgorithm().getName());
	    
		// The current time MUST be before the time represented by the exp Claim.
	    Date exp = claimsSet.getExpirationTime();
	    assertTrue(exp.getTime()>System.currentTimeMillis());

		// The iat Claim can be used to reject tokens that were issued too far away from the current time, limiting the amount of time
	    // that nonces need to be stored to prevent attacks. The acceptable range is Client specific.
	    Date iat = claimsSet.getIssueTime();
	    // just check that iat was within the last minute
	    assertTrue(iat.getTime()<=System.currentTimeMillis());
	    assertTrue(iat.getTime()>System.currentTimeMillis()-60000L);

		// If a nonce value was sent in the Authentication Request, a nonce Claim MUST be present and its value checked to verify 
	    // that it is the same value as the one that was sent in the Authentication Request. The Client SHOULD check the 
	    // nonce value for replay attacks. The precise method for detecting replay attacks is Client specific.
	    assertEquals(nonce, claimsSet.getClaim("nonce"));
	    
		// If the acr Claim was requested, the Client SHOULD check that the asserted Claim Value is appropriate. 
	    // The meaning and processing of acr Claim Values is out of scope for this specification.
	    assertNull(claimsSet.getClaim("acr"));
	    
		// If the auth_time Claim was requested, either through a specific request for this Claim or by using the max_age parameter, 
	    // the Client SHOULD check the auth_time Claim value and request re-authentication if it determines too much time has elapsed 
	    // since the last End-User authentication.
	    assertEquals(auth_time, claimsSet.getLongClaim("auth_time").longValue());
	    
	    // Elsewhere we take the token and JWK printed below and verify they can be used to check
	    // the signature using standard Python libraries.
	    System.out.println("Token: "+oidcToken);
	    System.out.println("JWK: "+OIDCTokenUtil.getJSONWebKeySet());
	}

}
