package org.sagebionetworks.repo.manager.oauth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.json.JSONArray;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.impl.DefaultClaims;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class OIDCTokenUtilTest {

	private static final String SUBJECT_ID = "101";
	private static final String CLIENT_ID = "client-01234";
	private static final long NOW = System.currentTimeMillis();
	private static final long AUTH_TIME = (new Date()).getTime()/1000L;
	private static final String TOKEN_ID = UUID.randomUUID().toString();
	private static final String NONCE = UUID.randomUUID().toString();
	private static final JSONArray TEAM_IDS = new JSONArray();
	private static final Map<OIDCClaimName, String> USER_CLAIMS;
	private static OIDCClaimsRequestDetails ESSENTIAL;
	private static OIDCClaimsRequestDetails NON_ESSENTIAL;

	static {
		TEAM_IDS.put("9876543");

		USER_CLAIMS = new HashMap<OIDCClaimName, String>();
		USER_CLAIMS.put(OIDCClaimName.team, TEAM_IDS.toString());
		USER_CLAIMS.put(OIDCClaimName.given_name, "User");
		USER_CLAIMS.put(OIDCClaimName.email, "user@synapse.org");
		USER_CLAIMS.put(OIDCClaimName.company, "University of Example");
		
		ESSENTIAL = new OIDCClaimsRequestDetails();
		ESSENTIAL.setEssential(true);
		NON_ESSENTIAL = new OIDCClaimsRequestDetails();
		NON_ESSENTIAL.setEssential(false);
	}
	
	private String OIDC_TOKEN = null;
	
	@Autowired
	OIDCTokenHelper oidcTokenHelper;
	
	@Before
	public void setUp() throws Exception {
		oidcTokenHelper.createOIDCIdToken("https://repo-prod.prod.sagebase.org/auth/v1", 
				SUBJECT_ID, CLIENT_ID, NOW, NONCE, AUTH_TIME, TOKEN_ID, USER_CLAIMS);
	}

	@Test
	public void testGetJSONWebKeySet() throws Exception {
		// below we test that the keys can be used to verify a signature.  Here we just check that they were generated
		List<JWK> jwks = oidcTokenHelper.getJSONWebKeySet();
		assertFalse(jwks.isEmpty());
	}
	
	@Test
	public void testCreateSignedJWT() throws Exception {
		Claims claims = new DefaultClaims();
		claims.put("claimName", "claimValue");
		
		// method under test
		String jwtString = oidcTokenHelper.createSignedJWT(claims);
		
		JWT jwt = JWTParser.parse(jwtString);
		assertTrue(jwt instanceof SignedJWT);

	    SignedJWT signedJWT = (SignedJWT)jwt;
	    assertEquals(Header.JWT_TYPE, signedJWT.getHeader().getType());
	    JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
	    assertEquals("claimValue", claimsSet.getClaim("claimName"));
	    assertTrue(oidcTokenHelper.validateSignedJWT(jwtString));
	}
	
	@Test
	public void testValidateSignedJWT() throws Exception {
		assertTrue(oidcTokenHelper.validateSignedJWT(OIDC_TOKEN));
	}

	
	@Test
	public void testOIDCTokenGeneration() throws Exception {
		JWT jwt = JWTParser.parse(OIDC_TOKEN);
		
		assertTrue(jwt instanceof SignedJWT);

	    SignedJWT signedJWT = (SignedJWT)jwt;
	    
	    // TODO check the claims, any biz logic in createOIDCIdToken()
		
		// Validate, per https://openid.net/specs/openid-connect-core-1_0.html#IDTokenValidation
	    
		// The Issuer Identifier for the OpenID Provider (which is typically obtained during Discovery) MUST exactly match the value of the iss (issuer) Claim.
	    JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
	    assertEquals("https://repo-prod.prod.sagebase.org/auth/v1", claimsSet.getIssuer());

	    // The Client MUST validate that the aud (audience) Claim contains its client_id value registered at the Issuer identified by 
	    // the iss (issuer) Claim as an audience. The aud (audience) Claim MAY contain an array with more than one element. The ID 
	    // Token MUST be rejected if the ID Token does not list the Client as a valid audience, or if it contains additional 
	    // audiences not trusted by the Client.
	    assertEquals(Collections.singletonList(CLIENT_ID), claimsSet.getAudience());
		// If the ID Token contains multiple audiences, the Client SHOULD verify that an azp Claim is present.
	    // 	(Note the above verifies that there are NOT multiple audiences.)

		// If an azp (authorized party) Claim is present, the Client SHOULD verify that its client_id is the Claim Value.
	    assertNull(claimsSet.getClaim("azp")); // i.e. verify no azp Claim

		// If the ID Token is received via direct communication between the Client and the Token Endpoint (which it is in this flow), 
	    // the TLS server validation MAY be used to validate the issuer in place of checking the token signature. The Client MUST 
	    // validate the signature of all other ID Tokens according to JWS using the algorithm specified in the JWT alg Header 
	    // Parameter. The Client MUST use the keys provided by the Issuer.
	    List<JWK> jwks = oidcTokenHelper.getJSONWebKeySet();
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
	    assertEquals(NONCE, claimsSet.getClaim("nonce"));
	    
		// If the acr Claim was requested, the Client SHOULD check that the asserted Claim Value is appropriate. 
	    // The meaning and processing of acr Claim Values is out of scope for this specification.
	    assertNull(claimsSet.getClaim("acr"));
	    
		// If the auth_time Claim was requested, either through a specific request for this Claim or by using the max_age parameter, 
	    // the Client SHOULD check the auth_time Claim value and request re-authentication if it determines too much time has elapsed 
	    // since the last End-User authentication.
	    assertEquals(AUTH_TIME, claimsSet.getLongClaim(OIDCClaimName.auth_time.name()).longValue());
	}
		
	private static OIDCClaimsRequestDetails createListClaimsDetails(List<String> l) {
		OIDCClaimsRequestDetails result = new OIDCClaimsRequestDetails();
		result.setValues(l);
		return result;
	}
	
	@Test
	public void testGetScopeAndClaims() throws Exception {		
		List<OAuthScope> grantedScopes = Collections.singletonList(OAuthScope.openid);
		Map<OIDCClaimName,OIDCClaimsRequestDetails> expectedClaims = new HashMap<OIDCClaimName,OIDCClaimsRequestDetails>();
		expectedClaims.put(OIDCClaimName.email, ESSENTIAL);
		expectedClaims.put(OIDCClaimName.given_name, NON_ESSENTIAL);
		expectedClaims.put(OIDCClaimName.family_name, null);
		expectedClaims.put(OIDCClaimName.team, createListClaimsDetails(Collections.singletonList("101")));
		
		String accessToken = oidcTokenHelper.createOIDCaccessToken(
				"https://repo-prod.prod.sagebase.org/auth/v1",
				SUBJECT_ID, 
				CLIENT_ID,
				NOW, 
				AUTH_TIME,
				TOKEN_ID,
				grantedScopes,
				expectedClaims);
		
		JWT jwt = JWTParser.parse(accessToken);

		List<OAuthScope> actualScopes = ClaimsJsonUtil.getScopeFromClaims(jwt.getJWTClaimsSet());
		Map<OIDCClaimName,OIDCClaimsRequestDetails> actualClaims = ClaimsJsonUtil.getOIDCClaimsFromClaimSet(jwt.getJWTClaimsSet());
		
		assertEquals(grantedScopes, actualScopes);
		assertEquals(expectedClaims, actualClaims);
	}
	
}
