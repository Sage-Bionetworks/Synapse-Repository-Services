package org.sagebionetworks.repo.manager.oauth;

import static org.junit.Assert.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.text.ParseException;
import java.util.Arrays;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.oauth.JsonWebKeySet;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;

//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(locations = { "classpath:test-context.xml" })
@RunWith(MockitoJUnitRunner.class)
public class OIDCTokenHelperImplTest {

	private static final String ISSUER = "https://repo-prod.prod.sagebase.org/auth/v1";
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
	
	@Mock
	private StackConfiguration stackConfiguration;
		
	@InjectMocks
	OIDCTokenHelperImpl oidcTokenHelper;
	
	@Before
	public void setUp() {
		/*
		 * Since we mock stack configuration we have to reintroduce a (valid, though NOT production)
		 * RSA key that can be used to sign tokens.
		 */
		String devPrivateKey = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDinbEq1zwFsfSA" + 
				"FCajV9qrk2Qok9EiF/jlp/bRknCMEgj8j5oYiDxmnFXvnrSHJVk1NUOo4ON8m3WR" + 
				"MCLpN7t0ygIKvx30L7xdVlnvoldv/lYx87KqGEVadEnl/lGEaGRxpdXkiwgJem9l" + 
				"Ht7V8TquJiIcqQviwCc1ZKjtKtSbZtcpA0MxT/YNATf7GBWWD5KH39qL6/hHrCIa" + 
				"BYCqbNQt594/UZEgxRfB6sKyb2744Jshiq41Y25TJc4gNUq69EBMkaQrC9V67as6" + 
				"TJG2GJhfmAPsXgr7Lbyk5/kTsC8YXAasAzZnpA+ljLNv6pIdpyizx9XNrz13DFrY" + 
				"RK8lbHkrAgMBAAECggEANIuT9PcLN9bXdos1mlJYpcf7RV1g9KLSV43msRlfd1sH" + 
				"Mmiptl6AgtplIraN7Xg/gxLiqVnb5Zy2Wf/rWGBP2visGInQDDq1Vn8bQ3FFDPbQ" + 
				"TazQFJikHCEysV2S0TzTbXaibee+6VO2WKAb00en75Fv/21DEES10q+Qa82ulomG" + 
				"THIy7RitkdhkKPv16Lz7EQFcrdbZieldppijm15EK+wAKl8TAShIyZaAC8QY61F1" + 
				"hqqLApigI1GL7knP4jnii6sF7ykEpHb5huf3RI18DGwtUZOWLqimHgvmfn3p5NLT" + 
				"d/Xo56SOazhTBlvkNlUQccIQDzA8M2pjLcxxnMLcoQKBgQD0eRY63R3ZJh72OLCm" + 
				"C1iRS8NnQmdC+iR3zw7J5jvOkHSDctI1Zf6X+D4iyPLr1+tjnLy5YgTDP9PUJi9h" + 
				"3FRbnGkPD7I5q+19cvV5bQvxn36+kKZunfgLQYYe1Zk/6BbItOli5fzyQK3QTSp8" + 
				"eIKdQ02VQiFNV4UESbrb00UWNQKBgQDtTQ/hun6eYCVW9n3TmTi5+WRBb1R4X3kd" + 
				"/tifYhzy2Eab2pgQ3BN9INENlBsPb8J1+86rWwY2KVO9vHhctK47ytEn1kv//KCo" + 
				"ugo870dEZuAqX9ACNtG/Ba52IkCSjcpTJRo3R3lTLASV7vLZs4q5AyKNWCJD49kr" + 
				"KC9nNpW93wKBgQDRkYtQ4oPXxin8gBROAqPlycC0H+RNMglY+xJ+WPMj3AlFNYSl" + 
				"ac2ZkKATSZeUPP/34ECX2kKi7XA1CJbNmQZnkektlBMABTYMuCNd9/CpLESGL5G8" + 
				"eYZMf9rtS8WXVulRHGSE9wqi0HcvfTbShKvTDALR1GKf3kqUpm+cSbuLkQKBgCfS" + 
				"hNXGrDT7wYhkeR0nW2OqPG7WtgA1VWf5OnUUy/Lc5IyHFHnP1N1swmha8GeYw7N0" + 
				"/Gu5LMOuD8WJeVFlaM/T62GaDsr4pCVsgwdSyEzsTrYNuiSE+pHp7Csa+GcfsFJf" + 
				"qZSZQ/z3KBXZMZvjC2ac5hF+NtHZzLn3Vm0ltd9VAoGBAJcs9PBjJ9XqAHo6x88g" + 
				"7KxK1mfXRrVoxMLZD2njx4xWTK6OWOAECdWBIPbsstT57BVUeSa5wAVy/SkgfZ3E" + 
				"MjjcxwFQoYuOR/cgYAFyXY/jMXb6kKSdc02437hTXjTotvkgAJfO7gxVy8s3ipI7" + 
				"pzvMOgD+6P3bCeoBuyKg2xhY";
		when(stackConfiguration.getOIDCSignatureRSAPrivateKeys()).thenReturn(Collections.singletonList(devPrivateKey));
		
		// takes the place of Spring set up
		oidcTokenHelper.afterPropertiesSet();
	}

	@Test
	public void testGetJSONWebKeySet() throws Exception {
		// below we test that the keys can be used to verify a signature.  Here we just check that they were generated
		JsonWebKeySet jwks = oidcTokenHelper.getJSONWebKeySet();
		assertFalse(jwks.getKeys().isEmpty());
	}
	
	@Test
	public void testGenerateOIDCIdentityToken() throws Exception {
		String oidcToken = oidcTokenHelper.createOIDCIdToken(ISSUER, 
				SUBJECT_ID, CLIENT_ID, NOW, NONCE, AUTH_TIME, TOKEN_ID, USER_CLAIMS);
		JWT jwt = JWTParser.parse(oidcToken);
		JWTClaimsSet claims = jwt.getJWTClaimsSet();
		
		assertEquals(TEAM_IDS.toString(), claims.getStringClaim(OIDCClaimName.team.name()));
		assertEquals("User", claims.getStringClaim(OIDCClaimName.given_name.name()));
		assertEquals("user@synapse.org", claims.getStringClaim(OIDCClaimName.email.name()));
		assertEquals("University of Example", claims.getStringClaim(OIDCClaimName.company.name()));
		assertEquals(TOKEN_ID, claims.getJWTID());
		
		// This checks the other fields set in the method under test
		clientValidation(oidcToken, NONCE);
	}

    // let's check that our JWTs fulfill the OIDC spec by checking that they meet the requirements for client validation:	
	// https://openid.net/specs/openid-connect-core-1_0.html#IDTokenValidation
	private void clientValidation(String jwtString, String nonce) throws ParseException {
		JWT jwt = JWTParser.parse(jwtString);
		assertTrue(jwt instanceof SignedJWT);

	    SignedJWT signedJWT = (SignedJWT)jwt;
	    
		// The Issuer Identifier for the OpenID Provider (which is typically obtained during Discovery) MUST exactly match the value of the iss (issuer) Claim.
	    JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
	    assertEquals(ISSUER, claimsSet.getIssuer());

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
		assertTrue(oidcTokenHelper.validateJWTSignature(jwtString));
	    
	    // by the way, the key ID in the token header should match that in the JWK
	    assertEquals(signedJWT.getHeader().getKeyID(), oidcTokenHelper.getJSONWebKeySet().getKeys().get(0).getKid());

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
	    assertEquals(AUTH_TIME, claimsSet.getLongClaim(OIDCClaimName.auth_time.name()).longValue());
	}
		
	
	private static OIDCClaimsRequestDetails createListClaimsDetails(List<String> l) {
		OIDCClaimsRequestDetails result = new OIDCClaimsRequestDetails();
		result.setValues(l);
		return result;
	}
	

	@Test
	public void testGeneateOIDCAccessToken() throws Exception {
		List<OAuthScope> grantedScopes = Collections.singletonList(OAuthScope.openid);
		Map<OIDCClaimName,OIDCClaimsRequestDetails> expectedClaims = new HashMap<OIDCClaimName,OIDCClaimsRequestDetails>();
		expectedClaims.put(OIDCClaimName.email, ESSENTIAL);
		expectedClaims.put(OIDCClaimName.given_name, NON_ESSENTIAL);
		expectedClaims.put(OIDCClaimName.family_name, null);
		expectedClaims.put(OIDCClaimName.team, createListClaimsDetails(Collections.singletonList("101")));
		
		String accessToken = oidcTokenHelper.createOIDCaccessToken(
				ISSUER,
				SUBJECT_ID, 
				CLIENT_ID,
				NOW, 
				AUTH_TIME,
				TOKEN_ID,
				grantedScopes,
				expectedClaims);
		
		JWT jwt = JWTParser.parse(accessToken);
		JWTClaimsSet claims = jwt.getJWTClaimsSet();
		// here we just check that the 'access' claim has been added
		// in the test for ClaimsJsonUtil.addAccessClaims() we check 
		// that the content is correct
		assertNotNull(claims.getClaim("access"));
		
		// This checks the other fields set in the method under test
		clientValidation(accessToken, null/* no nonce */);
	}

	
	@Test
	public void testOIDCSignatureValidation() throws Exception {
		String oidcToken = oidcTokenHelper.createOIDCIdToken("https://repo-prod.prod.sagebase.org/auth/v1", 
				SUBJECT_ID, CLIENT_ID, NOW, NONCE, AUTH_TIME, TOKEN_ID, USER_CLAIMS);
		assertTrue(oidcTokenHelper.validateJWTSignature(oidcToken));
	}
		

}
