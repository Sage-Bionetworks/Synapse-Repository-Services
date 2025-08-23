package org.sagebionetworks.repo.manager.oauth;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
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
import org.sagebionetworks.repo.model.SessionIdThreadLocal;
import org.sagebionetworks.repo.model.auth.AccessTokenRecord;
import org.sagebionetworks.repo.model.auth.JSONWebTokenHelper;
import org.sagebionetworks.repo.model.auth.TokenType;
import org.sagebionetworks.repo.model.dbo.auth.OAuthAccessTokenDaoImpl;
import org.sagebionetworks.repo.model.dbo.auth.OIDCAccessTokenData;
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
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;

@ExtendWith(MockitoExtension.class)
public class OIDCTokenManagerImplTest {

	private static final String ISSUER = "https://repo-prod.prod.sagebase.org/auth/v1";
	private static final Long USER_ID = 101L;
	private static final String SUBJECT_ID = "101";
	private static final String CLIENT_ID = "1234";
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
	
	@Mock
	private OAuthAccessTokenDaoImpl mockAccessTokenDao;
	
	@InjectMocks
	private JwtBuilder jwtBuilder;
	
	@Mock
	private JwtBuilder mockJwtBuilder;
	
	@InjectMocks
	private OIDCTokenManagerImpl oidcTokenManager;
	
	private JwtParser jwtParser;
	
	@BeforeEach
	public void setUp() {
		/*
		 * Since we mock stack configuration we have to reintroduce a (valid, though NOT production)
		 * RSA key that can be used to sign tokens.
		 */
		when(stackConfiguration.getOIDCSignatureRSAPrivateKeys()).thenReturn(
				Collections.singletonList(JWTTestHelper.TEST_RSA_KEY_PAIR));

		// takes the place of Spring set up
		oidcTokenManager.afterPropertiesSet();

		jwtParser = Jwts.parserBuilder()
				.setSigningKey(getPublicSigningKey())
				.build();
	}

	@Test
	public void testGetJSONWebKeySet() throws Exception {
		// below we test that the keys can be used to verify a signature.  Here we just check that they were generated
		JsonWebKeySet jwks = oidcTokenManager.getJSONWebKeySet();
		assertFalse(jwks.getKeys().isEmpty());
	}
	
	// get the public side of the current signing key
	private PublicKey getPublicSigningKey() {
		JsonWebKey jwk = oidcTokenManager.getJSONWebKeySet().getKeys().get(0);
		return JSONWebTokenHelper.getRSAPublicKeyForJsonWebKeyRSA((JsonWebKeyRSA)jwk);
	}
	
	@Test
	public void testGenerateOIDCIdentityToken() throws Exception {
		when(mockJwtBuilder.createSignedJWT(any()))
		.thenAnswer(invocation -> {
			Claims claims = (Claims) invocation.getArgument(0);
			return jwtBuilder.createSignedJWT(claims);});
	
		String oidcToken = oidcTokenManager.createOIDCIdToken(ISSUER, 
				SUBJECT_ID, CLIENT_ID, NOW, NONCE, AUTH_TIME, TOKEN_ID, USER_CLAIMS);
		Claims claims = jwtParser.parseClaimsJws(oidcToken).getBody();
		assertEquals(TEAM_IDS, claims.get(OIDCClaimName.team.name()));
		assertEquals("User", claims.get(OIDCClaimName.given_name.name(), String.class));
		assertEquals("user@synapse.org", claims.get(OIDCClaimName.email.name(), String.class));
		assertTrue(claims.get(OIDCClaimName.email_verified.name(), Boolean.class));
		assertEquals("University of Example", claims.get(OIDCClaimName.company.name(), String.class));
		assertEquals(TOKEN_ID, claims.getId());
		assertEquals(TokenType.OIDC_ID_TOKEN.name(), claims.get(OIDCClaimName.token_type.name(), String.class));
		
		// This checks the other fields set in the method under test
		jwtValidation(oidcToken, false, NONCE);
		
		verifyZeroInteractions(mockAccessTokenDao);
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
	    Jwt<JwsHeader,Claims> signedJWT = JSONWebTokenHelper.parseJWT(jwtString, oidcTokenManager.getJSONWebKeySet());
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
	    assertEquals(signedJWT.getHeader().getKeyId(), oidcTokenManager.getJSONWebKeySet().getKeys().get(0).getKid());

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
		when(mockJwtBuilder.createSignedJWT(any()))
		.thenAnswer(invocation -> {
			Claims claims = (Claims) invocation.getArgument(0);
			return jwtBuilder.createSignedJWT(claims);});
	
		List<OAuthScope> grantedScopes = Collections.singletonList(OAuthScope.openid);
		Map<OIDCClaimName,OIDCClaimsRequestDetails> expectedClaims = new HashMap<OIDCClaimName,OIDCClaimsRequestDetails>();
		expectedClaims.put(OIDCClaimName.email, ESSENTIAL);
		expectedClaims.put(OIDCClaimName.given_name, NON_ESSENTIAL);
		expectedClaims.put(OIDCClaimName.family_name, null);
		OIDCClaimsRequestDetails details = new OIDCClaimsRequestDetails();
		details.setValues(Collections.singletonList("101"));
		expectedClaims.put(OIDCClaimName.team, details);
		
		String sessionId = SessionIdThreadLocal.createNewSessionIdForThread();

		String accessToken = oidcTokenManager.createOIDCaccessToken(
				USER_ID,
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
		
		Claims claims = jwtParser.parseClaimsJws(accessToken).getBody();
		// here we just check that the 'access' claim has been added
		// in the test for ClaimsJsonUtil.addAccessClaims() we check 
		// that the content is correct
		assertNotNull(claims.get("access"));
		assertEquals(TokenType.OIDC_ACCESS_TOKEN.name(), claims.get(OIDCClaimName.token_type.name(), String.class));

		// This checks the other fields set in the method under test
		jwtValidation(accessToken, false, null/* no nonce */);
		
		OIDCAccessTokenData expectedRecord = new OIDCAccessTokenData()
			.setTokenId(TOKEN_ID)
			.setRefreshTokenId(Long.valueOf(REFRESH_TOKEN_ID))
			.setPrincipalId(USER_ID)
			.setClientId(Long.valueOf(CLIENT_ID))
			.setCreatedOn(claims.getIssuedAt())
			.setExpiresOn(claims.getExpiration())
			.setSessionId(sessionId);
		
		verify(mockAccessTokenDao).storeAccessTokenRecord(expectedRecord);
		
		SessionIdThreadLocal.clearThreadsSessionId();
	}
	
	@Test
	public void testGenerateOIDCAccessTokenWithPersistFalse() throws Exception {
		when(mockJwtBuilder.createSignedJWT(any()))
		.thenAnswer(invocation -> {
			Claims claims = (Claims) invocation.getArgument(0);
			return jwtBuilder.createSignedJWT(claims);});
	
		List<OAuthScope> grantedScopes = Collections.singletonList(OAuthScope.openid);
		Map<OIDCClaimName,OIDCClaimsRequestDetails> expectedClaims = new HashMap<OIDCClaimName,OIDCClaimsRequestDetails>();
		expectedClaims.put(OIDCClaimName.email, ESSENTIAL);
		expectedClaims.put(OIDCClaimName.given_name, NON_ESSENTIAL);
		expectedClaims.put(OIDCClaimName.family_name, null);
		OIDCClaimsRequestDetails details = new OIDCClaimsRequestDetails();
		details.setValues(Collections.singletonList("101"));
		expectedClaims.put(OIDCClaimName.team, details);

		boolean persistToken = false;
		
		String accessToken = oidcTokenManager.createOIDCaccessToken(
				USER_ID,
				ISSUER,
				SUBJECT_ID, 
				CLIENT_ID,
				NOW, 
				ONE_DAY_MILLIS,
				AUTH_TIME,
				REFRESH_TOKEN_ID,
				TOKEN_ID,
				grantedScopes,
				expectedClaims,
				persistToken);
		
		Claims claims = jwtParser.parseClaimsJws(accessToken).getBody();
		// here we just check that the 'access' claim has been added
		// in the test for ClaimsJsonUtil.addAccessClaims() we check 
		// that the content is correct
		assertNotNull(claims.get("access"));
		assertEquals(TokenType.OIDC_ACCESS_TOKEN.name(), claims.get(OIDCClaimName.token_type.name(), String.class));

		// This checks the other fields set in the method under test
		jwtValidation(accessToken, false, null/* no nonce */);
		
		verifyZeroInteractions(mockAccessTokenDao);

	}

	
	@Test
	public void testOIDCSignatureValidation() throws Exception {
		when(mockJwtBuilder.createSignedJWT(any()))
		.thenAnswer(invocation -> {
			Claims claims = (Claims) invocation.getArgument(0);
			return jwtBuilder.createSignedJWT(claims);});
	
		String oidcToken = oidcTokenManager.createOIDCIdToken("https://repo-prod.prod.sagebase.org/auth/v1", 
				SUBJECT_ID, CLIENT_ID, NOW, NONCE, AUTH_TIME, TOKEN_ID, USER_CLAIMS);
		oidcTokenManager.validateJWT(oidcToken);
	}
	
	@Test
	public void testCreateInternalTotalAccessToken() {
		long now = System.currentTimeMillis();
		when(mockClock.currentTimeMillis()).thenReturn(now);
		when(mockJwtBuilder.createSignedJWT(any()))
		.thenAnswer(invocation -> {
			Claims claims = (Claims) invocation.getArgument(0);
			return jwtBuilder.createSignedJWT(claims);});
	
		Long principalId = 101L;
		
		// method under test
		String accessToken = oidcTokenManager.createInternalTotalAccessToken(principalId);
		
		Claims claims = jwtParser.parseClaimsJws(accessToken).getBody();
		assertNull(claims.getIssuer());
		assertEquals(principalId.toString(), claims.getSubject());
		assertEquals(""+AuthorizationConstants.SYNAPSE_OAUTH_CLIENT_ID, claims.getAudience());
		assertNotNull(claims.getId());
		assertEquals(now/1000L+60, claims.getExpiration().getTime()/1000L);
		assertEquals(Arrays.asList(OAuthScope.values()), ClaimsJsonUtil.getScopeFromClaims(claims));
		
		verifyZeroInteractions(mockAccessTokenDao);
	}

	@Test
	public void testCreateClientTotalAccessToken() {
		long now = System.currentTimeMillis();
		when(mockClock.currentTimeMillis()).thenReturn(now);
		
		when(mockJwtBuilder.createSignedJWT(any()))
		.thenAnswer(invocation -> {
			Claims claims = (Claims) invocation.getArgument(0);
			return jwtBuilder.createSignedJWT(claims);});
	
		Long principalId = 101L;
		
		// method under test
		String accessToken = oidcTokenManager.createClientTotalAccessToken(principalId, ISSUER);
		
		Claims claims = jwtParser.parseClaimsJws(accessToken).getBody();
		
		assertEquals(ISSUER, claims.getIssuer());
		assertEquals(principalId.toString(), claims.getSubject());
		assertEquals(""+AuthorizationConstants.SYNAPSE_OAUTH_CLIENT_ID, claims.getAudience());
		assertNotNull(claims.getId());
		assertEquals(now/1000L+24*3600, claims.getExpiration().getTime()/1000L);
		assertEquals(Arrays.asList(OAuthScope.values()), ClaimsJsonUtil.getScopeFromClaims(claims));

		OIDCAccessTokenData expectedRecord = new OIDCAccessTokenData()
			.setTokenId(claims.getId())
			.setRefreshTokenId(null)
			.setPrincipalId(principalId)
			.setClientId(Long.valueOf(AuthorizationConstants.SYNAPSE_OAUTH_CLIENT_ID))
			.setCreatedOn(claims.getIssuedAt())
			.setExpiresOn(claims.getExpiration());
		
		verify(mockAccessTokenDao).storeAccessTokenRecord(expectedRecord);
	}

	@Test
	public void testParseExpiredJWTException() {
		when(mockClock.currentTimeMillis()).thenReturn(System.currentTimeMillis() - ONE_YEAR_MILLIS);
		when(mockJwtBuilder.createSignedJWT(any()))
		.thenAnswer(invocation -> {
			Claims claims = (Claims) invocation.getArgument(0);
			return jwtBuilder.createSignedJWT(claims);});
	
		Long principalId = 101L;
		String expiredAccessToken = oidcTokenManager.createInternalTotalAccessToken(principalId);
		assertThrows(OAuthUnauthenticatedException.class, () ->
				oidcTokenManager.parseJWT(expiredAccessToken));
	}

	@Test
	public void testCreatePersonalAccessToken() throws Exception {
		when(mockJwtBuilder.createSignedJWT(any()))
		.thenAnswer(invocation -> {
			Claims claims = (Claims) invocation.getArgument(0);
			return jwtBuilder.createSignedJWT(claims);});
	
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
		String accessToken = oidcTokenManager.createPersonalAccessToken(
				ISSUER,
				personalAccessTokenRecord);

		Claims claims = jwtParser.parseClaimsJws(accessToken).getBody();
		// here we just check that the 'access' claim has been added
		// in the test for ClaimsJsonUtil.addAccessClaims() we check
		// that the content is correct
		assertNotNull(claims.get("access"));
		assertEquals(TokenType.PERSONAL_ACCESS_TOKEN.name(), claims.get(OIDCClaimName.token_type.name(), String.class));

		// This checks the other fields set in the method under test
		jwtValidation(accessToken, true, null/* no nonce */);
		
		verifyZeroInteractions(mockAccessTokenDao);
	}
	
	@Test
	public void testIsOIDCAccessTokenExists() {
		// Call under test
		oidcTokenManager.doesOIDCAccessTokenExist(TOKEN_ID);
		
		verify(mockAccessTokenDao).doesAccessTokenRecordExist(TOKEN_ID);
	}
		
	@Test
	public void testRevokeOIDCAccessTokens() {
		// Call under test
		oidcTokenManager.revokeOIDCAccessTokens(USER_ID);
		
		verify(mockAccessTokenDao).deleteAccessTokenRecords(USER_ID);
	}
	
	@Test
	public void testRevokeOIDCAccessToken() {
		when(mockJwtBuilder.createSignedJWT(any()))
		.thenAnswer(invocation -> {
			Claims claims = (Claims) invocation.getArgument(0);
			return jwtBuilder.createSignedJWT(claims);});
	
		
		when(mockClock.currentTimeMillis()).thenReturn(System.currentTimeMillis());
		
		String token = oidcTokenManager.createClientTotalAccessToken(USER_ID, ISSUER);
		String tokenId = oidcTokenManager.parseJWT(token).getBody().getId();
		
		// Call under test
		oidcTokenManager.revokeOIDCAccessToken(token);
		
		verify(mockAccessTokenDao).deleteAccessTokenRecord(tokenId);
	}
	
	@Test
	public void testRevokeExpiredOIDCTokens() {
		
		when(oidcTokenManager.revokeExpiredOIDCAccessTokens()).thenReturn(10_000);
		
		// Call under test
		int deletedCount = oidcTokenManager.revokeExpiredOIDCAccessTokens();
		
		assertEquals(10_000, deletedCount);
		
		verify(mockAccessTokenDao).deleteExpiredTokens();
	}
	
	@Test
	public void testCreateWebhookMessageToken() throws Exception {
		Date now = new Date();
		
		when(mockClock.now()).thenReturn(now);
		
		when(mockJwtBuilder.createSignedJWT(any()))
		.thenAnswer(invocation -> {
			Claims claims = (Claims) invocation.getArgument(0);
			return jwtBuilder.createSignedJWT(claims);});
	
		String messageId = "123";
		String messageMd5 = "messageMd5";
		String webhookOwnerId = "456";
		int expirationsSec = 30;
		
		// Call under test
		String accessToken = oidcTokenManager.createWebhookMessageToken(ISSUER, messageId, messageMd5, webhookOwnerId, expirationsSec);
		
		Claims claims = jwtParser.parseClaimsJws(accessToken).getBody();
		
		assertEquals(TokenType.WEBHOOK_MESSAGE_TOKEN.name(), claims.get(OIDCClaimName.token_type.name(), String.class));
		assertEquals(ISSUER, claims.getIssuer());
		assertEquals(messageMd5, claims.get("message_md5"));
		assertEquals(webhookOwnerId, claims.getAudience());
		assertEquals(messageId, claims.getSubject());
		assertEquals(now.getTime()/1000, claims.getIssuedAt().getTime()/1000);
		assertEquals(now.getTime()/1000 + expirationsSec, claims.getExpiration().getTime()/1000);
		
		verifyZeroInteractions(mockAccessTokenDao);
	}
}
