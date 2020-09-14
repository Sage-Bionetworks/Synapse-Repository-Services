package org.sagebionetworks.repo.manager.oauth;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.security.PublicKey;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.auth.AccessTokenRecord;
import org.sagebionetworks.repo.model.auth.JSONWebTokenHelper;
import org.sagebionetworks.repo.model.auth.TokenType;
import org.sagebionetworks.repo.model.oauth.JsonWebKey;
import org.sagebionetworks.repo.model.oauth.JsonWebKeyRSA;
import org.sagebionetworks.repo.model.oauth.JsonWebKeySet;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;
import org.sagebionetworks.repo.web.OAuthUnauthenticatedException;
import org.sagebionetworks.util.Clock;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;

@ExtendWith(MockitoExtension.class)
public class OIDCTokenHelperImplTest {

	private static final String ISSUER = "https://repo-prod.prod.sagebase.org/auth/v1";
	private static final String SUBJECT_ID = "101";
	private static final String CLIENT_ID = "client-01234";
	private static final long NOW = System.currentTimeMillis();
	private static final Date AUTH_TIME = new Date();
	private static final long ONE_DAY_MILLIS = 1000L * 60 * 60 * 24;
	private static final long ONE_YEAR_MILLIS = ONE_DAY_MILLIS * 365;
	private static final String REFRESH_TOKEN_ID = "123456";
	private static final String TOKEN_ID = UUID.randomUUID().toString();
	private static final String NONCE = UUID.randomUUID().toString();
	private static final List<String> TEAM_IDS = new ArrayList<String>();
	private static final Map<OIDCClaimName, Object> USER_CLAIMS;
	private static OIDCClaimsRequestDetails ESSENTIAL;
	private static OIDCClaimsRequestDetails NON_ESSENTIAL;

	static {
		TEAM_IDS.add("9876543");

		USER_CLAIMS = new HashMap<OIDCClaimName, Object>();
		USER_CLAIMS.put(OIDCClaimName.team, TEAM_IDS);
		USER_CLAIMS.put(OIDCClaimName.given_name, "User");
		USER_CLAIMS.put(OIDCClaimName.email, "user@synapse.org");
		USER_CLAIMS.put(OIDCClaimName.email_verified, true);
		USER_CLAIMS.put(OIDCClaimName.company, "University of Example");
		
		ESSENTIAL = new OIDCClaimsRequestDetails();
		ESSENTIAL.setEssential(true);
		NON_ESSENTIAL = new OIDCClaimsRequestDetails();
		NON_ESSENTIAL.setEssential(false);
	}
	
	@Mock
	private StackConfiguration stackConfiguration;

	@Mock
	private Clock mockClock;

	@InjectMocks
	OIDCTokenHelperImpl oidcTokenHelper;
	
	@BeforeEach
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
	
	// get the public side of the current signing key
	private PublicKey getPublicSigningKey() {
		JsonWebKey jwk = oidcTokenHelper.getJSONWebKeySet().getKeys().get(0);
		return JSONWebTokenHelper.getRSAPublicKeyForJsonWebKeyRSA((JsonWebKeyRSA)jwk);
	}
	
	@Test
	public void testGenerateOIDCIdentityToken() throws Exception {
		String oidcToken = oidcTokenHelper.createOIDCIdToken(ISSUER, 
				SUBJECT_ID, CLIENT_ID, NOW, NONCE, AUTH_TIME, TOKEN_ID, USER_CLAIMS);
		Jwt<JwsHeader,Claims> jwt = Jwts.parser().setSigningKey(getPublicSigningKey()).parse(oidcToken);
		Claims claims = jwt.getBody();
		assertEquals(TEAM_IDS, claims.get(OIDCClaimName.team.name()));
		assertEquals("User", claims.get(OIDCClaimName.given_name.name(), String.class));
		assertEquals("user@synapse.org", claims.get(OIDCClaimName.email.name(), String.class));
		assertTrue(claims.get(OIDCClaimName.email_verified.name(), Boolean.class));
		assertEquals("University of Example", claims.get(OIDCClaimName.company.name(), String.class));
		assertEquals(TOKEN_ID, claims.getId());
		assertEquals(TokenType.OIDC_ID_TOKEN.name(), claims.get(OIDCClaimName.token_type.name(), String.class));
		
		// This checks the other fields set in the method under test
		jwtValidation(oidcToken, false, NONCE);
	}

	/**
	 * This method lets us check that our JWTs fulfill the OIDC spec by checking that they meet the requirements for client validation.
	 * See https://openid.net/specs/openid-connect-core-1_0.html#IDTokenValidation
	 *
	 * For code reuse, we also use this method to check the validity of personal access tokens, but by design, PATs are not OIDC-compliant.
	 * @param jwtString
	 * @param isPersonalAccessToken
	 * @param nonce
	 * @throws ParseException
	 */
	private void jwtValidation(String jwtString, boolean isPersonalAccessToken, String nonce) throws ParseException {
		// If the ID Token is received via direct communication between the Client and the Token Endpoint (which it is in this flow), 
	    // the TLS server validation MAY be used to validate the issuer in place of checking the token signature. The Client MUST 
	    // validate the signature of all other ID Tokens according to JWS using the algorithm specified in the JWT alg Header 
	    // Parameter. The Client MUST use the keys provided by the Issuer.
	    Jwt<JwsHeader,Claims> signedJWT = JSONWebTokenHelper.parseJWT(jwtString, oidcTokenHelper.getJSONWebKeySet());
		assertNotNull(signedJWT);
	    
	    
		// The Issuer Identifier for the OpenID Provider (which is typically obtained during Discovery) MUST exactly match the value of the iss (issuer) Claim.
	    Claims claimsSet = signedJWT.getBody();
	    assertEquals(ISSUER, claimsSet.getIssuer());

	    // The Client MUST validate that the aud (audience) Claim contains its client_id value registered at the Issuer identified by 
	    // the iss (issuer) Claim as an audience. The aud (audience) Claim MAY contain an array with more than one element. The ID 
	    // Token MUST be rejected if the ID Token does not list the Client as a valid audience, or if it contains additional 
	    // audiences not trusted by the Client.
		if (!isPersonalAccessToken) {
			assertEquals(CLIENT_ID, claimsSet.getAudience());
		} else {
			// The audience for personal access tokens is the internal OAuth client
			assertEquals(AuthorizationConstants.SYNAPSE_OAUTH_CLIENT_ID, claimsSet.getAudience());
		}
		// If the ID Token contains multiple audiences, the Client SHOULD verify that an azp Claim is present.
	    // 	(Note the above verifies that there are NOT multiple audiences.)

		// If an azp (authorized party) Claim is present, the Client SHOULD verify that its client_id is the Claim Value.
	    assertNull(claimsSet.get("azp")); // i.e. verify no azp Claim

	    // by the way, the key ID in the token header should match that in the JWK
	    assertEquals(signedJWT.getHeader().getKeyId(), oidcTokenHelper.getJSONWebKeySet().getKeys().get(0).getKid());

		// The alg value SHOULD be the default of RS256 or the algorithm sent by the Client in the id_token_signed_response_alg parameter during Registration.
	    assertEquals("RS256", signedJWT.getHeader().getAlgorithm());
	    
		// The current time MUST be before the time represented by the exp Claim.
		if (!isPersonalAccessToken) {
			Date exp = claimsSet.getExpiration();
			assertTrue(exp.getTime() > System.currentTimeMillis());
		} else {
			// personal access tokens have no expiration
			assertNull(claimsSet.getExpiration());
		}

		// The iat Claim can be used to reject tokens that were issued too far away from the current time, limiting the amount of time
	    // that nonces need to be stored to prevent attacks. The acceptable range is Client specific.
	    Date iat = claimsSet.getIssuedAt();
	    // just check that iat was within the last minute
	    assertTrue(iat.getTime()<=System.currentTimeMillis());
	    assertTrue(iat.getTime()>System.currentTimeMillis()-60000L);

		// If a nonce value was sent in the Authentication Request, a nonce Claim MUST be present and its value checked to verify 
	    // that it is the same value as the one that was sent in the Authentication Request. The Client SHOULD check the 
	    // nonce value for replay attacks. The precise method for detecting replay attacks is Client specific.
	    assertEquals(nonce, claimsSet.get("nonce", String.class));
	    
		// If the acr Claim was requested, the Client SHOULD check that the asserted Claim Value is appropriate. 
	    // The meaning and processing of acr Claim Values is out of scope for this specification.
	    assertNull(claimsSet.get("acr"));
	    
		// If the auth_time Claim was requested, either through a specific request for this Claim or by using the max_age parameter, 
	    // the Client SHOULD check the auth_time Claim value and request re-authentication if it determines too much time has elapsed 
	    // since the last End-User authentication.
		if (!isPersonalAccessToken) {
			assertEquals((int)(AUTH_TIME.getTime()/1000L), claimsSet.get(OIDCClaimName.auth_time.name(), Integer.class));
		} else {
			// personal access tokens do not contain the auth time
			assertNull(claimsSet.get(OIDCClaimName.auth_time.name()));
		}
	    // The access token (not the ID token) will contain a refresh token ID, if it is associated with one
		if (claimsSet.containsKey(OIDCClaimName.refresh_token_id.name())) {
			assertEquals(REFRESH_TOKEN_ID, claimsSet.get(OIDCClaimName.refresh_token_id.name(), String.class));
		}
	}
		
	@Test
	public void testGenerateOIDCAccessToken() throws Exception {
		List<OAuthScope> grantedScopes = Collections.singletonList(OAuthScope.openid);
		Map<OIDCClaimName,OIDCClaimsRequestDetails> expectedClaims = new HashMap<OIDCClaimName,OIDCClaimsRequestDetails>();
		expectedClaims.put(OIDCClaimName.email, ESSENTIAL);
		expectedClaims.put(OIDCClaimName.given_name, NON_ESSENTIAL);
		expectedClaims.put(OIDCClaimName.family_name, null);
		OIDCClaimsRequestDetails details = new OIDCClaimsRequestDetails();
		details.setValues(Collections.singletonList("101"));
		expectedClaims.put(OIDCClaimName.team, details);

		String accessToken = oidcTokenHelper.createOIDCaccessToken(
				ISSUER,
				SUBJECT_ID, 
				CLIENT_ID,
				NOW, 
				ONE_DAY_MILLIS,
				AUTH_TIME,
				REFRESH_TOKEN_ID,
				TOKEN_ID,
				grantedScopes,
				expectedClaims);
		
		Jwt<JwsHeader,Claims> jwt = Jwts.parser().setSigningKey(getPublicSigningKey()).parse(accessToken);
		Claims claims = jwt.getBody();
		// here we just check that the 'access' claim has been added
		// in the test for ClaimsJsonUtil.addAccessClaims() we check 
		// that the content is correct
		assertNotNull(claims.get("access"));
		assertEquals(TokenType.OIDC_ACCESS_TOKEN.name(), claims.get(OIDCClaimName.token_type.name(), String.class));

		// This checks the other fields set in the method under test
		jwtValidation(accessToken, false, null/* no nonce */);
	}

	
	@Test
	public void testOIDCSignatureValidation() throws Exception {
		String oidcToken = oidcTokenHelper.createOIDCIdToken("https://repo-prod.prod.sagebase.org/auth/v1", 
				SUBJECT_ID, CLIENT_ID, NOW, NONCE, AUTH_TIME, TOKEN_ID, USER_CLAIMS);
		oidcTokenHelper.validateJWT(oidcToken);
	}
	
	@Test
	public void testCreateTotalAccessToken() {
		when(mockClock.currentTimeMillis()).thenReturn(System.currentTimeMillis());
		Long principalId = 101L;
		String accessToken = oidcTokenHelper.createTotalAccessToken(principalId);
		Jwt<JwsHeader,Claims> jwt = Jwts.parser().setSigningKey(getPublicSigningKey()).parse(accessToken);
		Claims claims = jwt.getBody();
		assertNull(claims.getIssuer());
		assertEquals(principalId.toString(), claims.getSubject());
		assertEquals(""+AuthorizationConstants.SYNAPSE_OAUTH_CLIENT_ID, claims.getAudience());
		assertNotNull(claims.getId());
		assertEquals(Arrays.asList(OAuthScope.values()), ClaimsJsonUtil.getScopeFromClaims(claims));
	}

	@Test
	public void testParseExpiredJWTException() {
		when(mockClock.currentTimeMillis()).thenReturn(System.currentTimeMillis() - ONE_YEAR_MILLIS);
		Long principalId = 101L;
		String expiredAccessToken = oidcTokenHelper.createTotalAccessToken(principalId);
		assertThrows(OAuthUnauthenticatedException.class, () ->
				oidcTokenHelper.parseJWT(expiredAccessToken));
	}

	@Test
	public void testCreatePersonalAccessToken() throws Exception {
		List<OAuthScope> grantedScopes = Collections.singletonList(OAuthScope.openid);
		Map<String,OIDCClaimsRequestDetails> expectedClaims = new HashMap<>();
		expectedClaims.put(OIDCClaimName.email.name(), ESSENTIAL);
		expectedClaims.put(OIDCClaimName.given_name.name(), NON_ESSENTIAL);
		expectedClaims.put(OIDCClaimName.family_name.name(), null);
		OIDCClaimsRequestDetails details = new OIDCClaimsRequestDetails();
		details.setValues(Collections.singletonList("101"));
		expectedClaims.put(OIDCClaimName.team.name(), details);

		AccessTokenRecord personalAccessTokenRecord = new AccessTokenRecord();
		personalAccessTokenRecord.setId("1234");
		personalAccessTokenRecord.setCreatedOn(new Date());
		personalAccessTokenRecord.setScopes(grantedScopes);
		personalAccessTokenRecord.setUserInfoClaims(expectedClaims);

		// method under test
		String accessToken = oidcTokenHelper.createPersonalAccessToken(
				ISSUER,
				personalAccessTokenRecord);

		Jwt<JwsHeader,Claims> jwt = Jwts.parser().setSigningKey(getPublicSigningKey()).parse(accessToken);
		Claims claims = jwt.getBody();
		// here we just check that the 'access' claim has been added
		// in the test for ClaimsJsonUtil.addAccessClaims() we check
		// that the content is correct
		assertNotNull(claims.get("access"));
		assertEquals(TokenType.PERSONAL_ACCESS_TOKEN.name(), claims.get(OIDCClaimName.token_type.name(), String.class));

		// This checks the other fields set in the method under test
		jwtValidation(accessToken, true, null/* no nonce */);
	}

}
