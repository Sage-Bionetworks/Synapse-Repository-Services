package org.sagebionetworks.repo.manager.oauth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.StackEncrypter;
import org.sagebionetworks.repo.manager.UserAuthorization;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.manager.oauth.claimprovider.EmailClaimProvider;
import org.sagebionetworks.repo.manager.oauth.claimprovider.EmailVerifiedClaimProvider;
import org.sagebionetworks.repo.manager.oauth.claimprovider.OIDCClaimProvider;
import org.sagebionetworks.repo.manager.oauth.claimprovider.TeamClaimProvider;
import org.sagebionetworks.repo.manager.oauth.claimprovider.UserIdClaimProvider;
import org.sagebionetworks.repo.manager.oauth.claimprovider.ValidatedAtClaimProvider;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.auth.AuthenticationDAO;
import org.sagebionetworks.repo.model.auth.OAuthClientDao;
import org.sagebionetworks.repo.model.oauth.OAuthAuthorizationResponse;
import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.repo.model.oauth.OAuthResponseType;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.oauth.OIDCAuthorizationRequest;
import org.sagebionetworks.repo.model.oauth.OIDCAuthorizationRequestDescription;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;
import org.sagebionetworks.repo.model.oauth.OIDCSigningAlgorithm;
import org.sagebionetworks.repo.model.oauth.OIDCTokenResponse;
import org.sagebionetworks.repo.model.verification.VerificationState;
import org.sagebionetworks.repo.model.verification.VerificationStateEnum;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.securitytools.EncryptionUtils;
import org.sagebionetworks.util.Clock;

import com.google.common.collect.ImmutableList;

@RunWith(MockitoJUnitRunner.class)
public class OpenIDConnectManagerImplUnitTest {
	private static final String USER_ID = "101";
	private static final Long USER_ID_LONG = Long.parseLong(USER_ID);
	private static final List<String> REDIRCT_URIS = Collections.singletonList("https://client.com/redir");
	private static final String OAUTH_CLIENT_ID = "123";
	private static final String OAUTH_ENDPOINT = "https://repo-prod.prod.sagebase.org/auth/v1";
	private static final String EMAIL = "me@domain.com";
	private String ppid;

	@Mock
	private StackEncrypter mockStackEncrypter;

	@Mock
	private OAuthClientDao mockOauthClientDao;

	@Mock
	private AuthenticationDAO mockAuthDao;

	@Mock
	private OIDCTokenHelper oidcTokenHelper;
	
	@InjectMocks
	private OpenIDConnectManagerImpl openIDConnectManagerImpl;
	
	@Mock
	private StackConfiguration mockStackConfigurations;
	
	@Mock
	private UserProfileManager userProfileManager;
	
	@Mock
	private GroupMembersDAO mockGroupMembersDAO;

	@Mock
	private UserManager mockUserManager;
	
	@Mock
	private Clock mockClock;
	
	@InjectMocks
	private EmailClaimProvider mockEmailClaimProvider;

	@InjectMocks
	private EmailVerifiedClaimProvider mockEmailVerifiedClaimProvider;

	@InjectMocks
	private UserIdClaimProvider mockUserIdClaimProvider;

	@InjectMocks
	private ValidatedAtClaimProvider mockValidatedAtClaimProvider;

	@InjectMocks
	private TeamClaimProvider mockTeamClaimProvider;

	@Captor
	private ArgumentCaptor<Map<OIDCClaimName, Object>> userInfoCaptor;
	
	@Captor
	private ArgumentCaptor<List<OAuthScope>> scopesCaptor;
	
	@Captor
	private ArgumentCaptor<Map<OIDCClaimName, OIDCClaimsRequestDetails>> claimsCaptor;
	
	@Captor
	private ArgumentCaptor<OIDCClaimName> oidcClaimNameCaptor;
	
	private UserInfo userInfo;
	private UserInfo anonymousUserInfo;
	private Date now;
	private String clientSpecificEncodingSecret;
	private OAuthClient oauthClient;
	private Map<OIDCClaimName, OIDCClaimProvider> mockClaimProviders;
	private UserProfile userProfile;
	
	
	@Before
	public void setUp() throws Exception {
		userInfo = new UserInfo(false);
		userInfo.setId(USER_ID_LONG);
		userInfo.setGroups(new HashSet<Long>());

		anonymousUserInfo = new UserInfo(false);
		anonymousUserInfo.setId(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
		
		oauthClient = new OAuthClient();
		oauthClient.setClient_id(OAUTH_CLIENT_ID);
		oauthClient.setRedirect_uris(REDIRCT_URIS);
		oauthClient.setUserinfo_signed_response_alg(OIDCSigningAlgorithm.RS256);

		when(mockOauthClientDao.getOAuthClient(OAUTH_CLIENT_ID)).thenReturn(oauthClient);	
		when(mockOauthClientDao.doesSectorIdentifierExistForURI(anyString())).thenReturn(true);
		when(mockOauthClientDao.getOAuthClientCreator(OAUTH_CLIENT_ID)).thenReturn(USER_ID);
		
		when(mockStackEncrypter.encryptAndBase64EncodeStringWithStackKey(anyString())).then(returnsFirstArg());	
		when(mockStackEncrypter.decryptStackEncryptedAndBase64EncodedString(anyString())).then(returnsFirstArg());	
		
		clientSpecificEncodingSecret = EncryptionUtils.newSecretKey();
		when(mockOauthClientDao.getSectorIdentifierSecretForClient(OAUTH_CLIENT_ID)).thenReturn(clientSpecificEncodingSecret);
		this.ppid = EncryptionUtils.encrypt(USER_ID, clientSpecificEncodingSecret);
		
		now = new Date();
		when(mockAuthDao.getSessionValidatedOn(USER_ID_LONG)).thenReturn(now);
		
		when(mockUserManager.getUserInfo(USER_ID_LONG)).thenReturn(userInfo);
		
		when(mockClock.currentTimeMillis()).thenReturn(System.currentTimeMillis());
		when(mockClock.now()).thenReturn(new Date());

		// we don't wire up all the claim providers, just a representative sample returning a variety of object types
		mockClaimProviders = new HashMap<OIDCClaimName, OIDCClaimProvider>();
		userProfile = new UserProfile();
		userProfile.setEmails(ImmutableList.of(EMAIL, "secondary email"));
		when(userProfileManager.getUserProfile(USER_ID)).thenReturn(userProfile);
		mockClaimProviders.put(OIDCClaimName.email, mockEmailClaimProvider);
		
		mockClaimProviders.put(OIDCClaimName.email_verified, mockEmailVerifiedClaimProvider);
		
		mockClaimProviders.put(OIDCClaimName.userid, mockUserIdClaimProvider);
		
		VerificationState verificationState = new VerificationState();
		verificationState.setState(VerificationStateEnum.APPROVED);
		verificationState.setCreatedOn(now);
		VerificationSubmission verificationSubmission = new VerificationSubmission();
		verificationSubmission.setStateHistory(Collections.singletonList(verificationState));
		when(userProfileManager.getCurrentVerificationSubmission(USER_ID_LONG)).thenReturn(verificationSubmission);
		mockClaimProviders.put(OIDCClaimName.validated_at, mockValidatedAtClaimProvider);
		
		when(mockGroupMembersDAO.filterUserGroups(eq(USER_ID), (List<String>)any())).thenReturn(Collections.singletonList("101"));

		mockClaimProviders.put(OIDCClaimName.team, mockTeamClaimProvider);
		
		openIDConnectManagerImpl.setClaimProviders(mockClaimProviders);
	}
	
	@Test
	public void testParseScopeString() throws Exception {
		// method under test (happy case)
		List<OAuthScope> scopes = OpenIDConnectManagerImpl.parseScopeString("openid openid");
		List<OAuthScope> expected = new ArrayList<OAuthScope>();
		expected.add(OAuthScope.openid);
		expected.add(OAuthScope.openid);
		assertEquals(expected, scopes);
		
		try {
			// method under test
			OpenIDConnectManagerImpl.parseScopeString("openid foo");
			fail("IllegalArgumentException expected.");
		} catch (IllegalArgumentException e) {
			// as expected
		}
		
		// what if url encoded?
		// method under test
		scopes = OpenIDConnectManagerImpl.parseScopeString("openid+openid");
		assertEquals(expected, scopes);
		
		// method under test
		scopes = OpenIDConnectManagerImpl.parseScopeString("openid%20openid");
		assertEquals(expected, scopes);
		
	}
	
	@Test
	public void testParseScopeString_Empty() throws Exception {
		assertEquals(Collections.EMPTY_LIST, OpenIDConnectManagerImpl.parseScopeString(null));
		assertEquals(Collections.EMPTY_LIST, OpenIDConnectManagerImpl.parseScopeString(""));
	}
	
	@Test
	public void testValidateAuthenticationRequest() {
		OAuthClient client = new OAuthClient();
		client.setRedirect_uris(REDIRCT_URIS);

		OIDCAuthorizationRequest authorizationRequest = new OIDCAuthorizationRequest();
		authorizationRequest.setRedirectUri(REDIRCT_URIS.get(0));
		authorizationRequest.setResponseType(OAuthResponseType.code);
		
		// method under test
		OpenIDConnectManagerImpl.validateAuthenticationRequest(authorizationRequest, client);
	}

	@Test
	public void testValidateAuthenticationRequest_invalidUri() {
		OAuthClient client = new OAuthClient();
		client.setRedirect_uris(REDIRCT_URIS);

		OIDCAuthorizationRequest authorizationRequest = new OIDCAuthorizationRequest();
		authorizationRequest.setRedirectUri("some invalid uri");
		authorizationRequest.setResponseType(OAuthResponseType.code);
		
		try {
			// method under test
			OpenIDConnectManagerImpl.validateAuthenticationRequest(authorizationRequest, client);
			fail("Exception expected.");
		} catch (IllegalArgumentException e) {
			// as expected
		}
	}

	@Test
	public void testValidateAuthenticationRequest_invalidResponseType() {
		OAuthClient client = new OAuthClient();
		client.setRedirect_uris(REDIRCT_URIS);

		OIDCAuthorizationRequest authorizationRequest = new OIDCAuthorizationRequest();
		authorizationRequest.setRedirectUri(REDIRCT_URIS.get(0));
		authorizationRequest.setResponseType(null);
		
		try {
			// method under test
			OpenIDConnectManagerImpl.validateAuthenticationRequest(authorizationRequest, client);
			fail("Exception expected.");
		} catch (IllegalArgumentException e) {
			// as expected
		}
	}

	private static final String NONCE = UUID.randomUUID().toString();
	
	// claimsTag must be 'id_token' or 'userinfo'
	private OIDCAuthorizationRequest createAuthorizationRequest(List<String> claimsTags) {
		OIDCAuthorizationRequest authorizationRequest = new OIDCAuthorizationRequest();
		authorizationRequest.setClientId(OAUTH_CLIENT_ID);
		authorizationRequest.setScope(OAuthScope.openid.name());
		StringBuilder claims = new StringBuilder("{");
		boolean firstTag = true;
		for (String claimsTag : claimsTags) {
			if (firstTag) {firstTag=false;} else {claims.append(",");}
			claims.append("\""+claimsTag+"\":{");
			boolean firstClaim = true;
			for (OIDCClaimName claimName : mockClaimProviders.keySet()) {
				if (firstClaim) {firstClaim=false;} else {claims.append(",");}
				claims.append("\""+claimName.name()+"\":\"null\"");
			}
			claims.append("}");
		}
		claims.append("}");
		authorizationRequest.setClaims(claims.toString());
		authorizationRequest.setResponseType(OAuthResponseType.code);
		authorizationRequest.setRedirectUri(REDIRCT_URIS.get(0));
		authorizationRequest.setNonce(NONCE);
		return authorizationRequest;
	}
	
	private OIDCAuthorizationRequest createAuthorizationRequest() {
		return createAuthorizationRequest(Collections.EMPTY_LIST);
	}
		
	@Test
	public void testGetAuthenticationRequestDescription_HappyCase_IdTokenClaims() {
		OIDCAuthorizationRequest authorizationRequest = createAuthorizationRequest(Collections.singletonList("id_token"));
		OIDCAuthorizationRequestDescription description = 
				// method under test
				openIDConnectManagerImpl.getAuthenticationRequestDescription(authorizationRequest);
		assertEquals(OAUTH_CLIENT_ID, description.getClientId());
		assertEquals(REDIRCT_URIS.get(0), description.getRedirect_uri());
		Set<String> expectedScope = new HashSet<String>();
		for (OIDCClaimProvider provider : mockClaimProviders.values()) {
			expectedScope.add(provider.getDescription());
		}
		// Note, we compare sets, not lists, since we don't enforce order
		assertEquals(expectedScope, new HashSet<String>(description.getScope()));
	}

	@Test
	public void testGetAuthenticationRequestDescription_HappyCase_UserInfoClaims() {
		OIDCAuthorizationRequest authorizationRequest = createAuthorizationRequest(Collections.singletonList("userinfo"));
		OIDCAuthorizationRequestDescription description = 
				// method under test
				openIDConnectManagerImpl.getAuthenticationRequestDescription(authorizationRequest);
		assertEquals(OAUTH_CLIENT_ID, description.getClientId());
		assertEquals(REDIRCT_URIS.get(0), description.getRedirect_uri());
		Set<String> expectedScope = new HashSet<String>();
		for (OIDCClaimProvider provider : mockClaimProviders.values()) {
			expectedScope.add(provider.getDescription());
		}
		// Note, we compare sets, not lists, since we don't enforce order
		assertEquals(expectedScope, new HashSet<String>(description.getScope()));
	}

	@Test
	public void testGetAuthenticationRequestDescription_NoOAuthScope() {
		OIDCAuthorizationRequest authorizationRequest = createAuthorizationRequest();
		authorizationRequest.setScope(null);
		OIDCAuthorizationRequestDescription description = 
				// method under test
				openIDConnectManagerImpl.getAuthenticationRequestDescription(authorizationRequest);
		assertEquals(OAUTH_CLIENT_ID, description.getClientId());
		assertEquals(REDIRCT_URIS.get(0), description.getRedirect_uri());
		// Note, we compare sets, not lists, since we don't enforce order
		assertTrue(description.getScope().isEmpty());
	}

	@Test
	public void testGetAuthenticationRequestDescription_MissingOrInvalidClientId() {
		OIDCAuthorizationRequest authorizationRequest = createAuthorizationRequest();
		authorizationRequest.setClientId("42");
		when(mockOauthClientDao.getOAuthClient("42")).thenThrow(new NotFoundException());

		try {
			// method under test
			openIDConnectManagerImpl.getAuthenticationRequestDescription(authorizationRequest);
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// as expected
		}

		authorizationRequest.setClientId(null);
		try {
			// method under test
			openIDConnectManagerImpl.getAuthenticationRequestDescription(authorizationRequest);
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// as expected
		}

	}

	@Test
	public void testGetAuthenticationRequestDescription_BadRedirectURI() {
		OIDCAuthorizationRequest authorizationRequest = createAuthorizationRequest();
		authorizationRequest.setRedirectUri("some other redir uri");

		try {
			// method under test
			openIDConnectManagerImpl.getAuthenticationRequestDescription(authorizationRequest);
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// as expected
		}

	}

	@Test
	public void testAuthorizeClient() throws Exception {
		OIDCAuthorizationRequest authorizationRequest = createAuthorizationRequest();

		// method under test
		OAuthAuthorizationResponse authResponse = openIDConnectManagerImpl.authorizeClient(userInfo, authorizationRequest);
		String code = authResponse.getAccess_code();
		assertNotNull(code);
		String decrypted = mockStackEncrypter.decryptStackEncryptedAndBase64EncodedString(code);
		
		OIDCAuthorizationRequest authRequestFromCode = new OIDCAuthorizationRequest();
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(decrypted);
		try {
			authRequestFromCode.initializeFromJSONObject(adapter);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
		
		// make sure authorizedAt was set
		assertNotNull(authRequestFromCode.getAuthorizedAt());
		
		// if we update the original request with the fields set by the server, it should match the retrieved value
		authorizationRequest.setUserId(USER_ID);
		authorizationRequest.setAuthorizedAt(authRequestFromCode.getAuthorizedAt());
		authorizationRequest.setAuthenticatedAt(now);
		assertEquals(authorizationRequest, authRequestFromCode);
	}

	@Test
	public void testAuthorizeClient_anonynmous() throws Exception {
		OIDCAuthorizationRequest authorizationRequest = createAuthorizationRequest();

		// method under test
		try {
			openIDConnectManagerImpl.authorizeClient(anonymousUserInfo, authorizationRequest);
			fail("UnauthorizedException expected");
		} catch (UnauthorizedException e) {
			// as expected
		}
	}	

	@Test
	public void testAuthorizeClient_InvalidClientId() {
		OIDCAuthorizationRequest authorizationRequest = createAuthorizationRequest();
		authorizationRequest.setClientId("42");
		when(mockOauthClientDao.getOAuthClient("42")).thenThrow(new NotFoundException());

		try {
			// method under test
			openIDConnectManagerImpl.authorizeClient(userInfo, authorizationRequest);
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// as expected
		}
	}
	
	@Test
	public void testPPID() {
		// method under test
		String ppid = openIDConnectManagerImpl.ppid(USER_ID, OAUTH_CLIENT_ID);
		assertEquals(USER_ID, EncryptionUtils.decrypt(ppid, clientSpecificEncodingSecret));
	}

	@Test
	public void testPPIDForSynapse() {
		// method under test
		String ppid = openIDConnectManagerImpl.ppid(USER_ID, OAuthClientManager.SYNAPSE_OAUTH_CLIENT_ID);
		assertEquals(USER_ID, ppid);
	}

	@Test
	public void testGetUserIdFromPPID() {
		String ppid = openIDConnectManagerImpl.ppid(USER_ID, OAUTH_CLIENT_ID);

		// method under test		
		assertEquals(USER_ID, openIDConnectManagerImpl.getUserIdFromPPID(ppid, OAUTH_CLIENT_ID));
	}

	@Test
	public void testGetUserIdFromPPIDForSynapse() {
		// method under test		
		assertEquals(USER_ID, openIDConnectManagerImpl.getUserIdFromPPID(USER_ID, OAuthClientManager.SYNAPSE_OAUTH_CLIENT_ID));
	}
	
	@Test
	public void testGetUserInfo_internal() {
		
		Map<OIDCClaimName, OIDCClaimsRequestDetails> oidcClaims = new HashMap<OIDCClaimName, OIDCClaimsRequestDetails>();
		oidcClaims.put(OIDCClaimName.email, null);
		oidcClaims.put(OIDCClaimName.email_verified, null);
		oidcClaims.put(OIDCClaimName.userid, null);
		oidcClaims.put(OIDCClaimName.validated_at, null);
		OIDCClaimsRequestDetails teamRequest = new OIDCClaimsRequestDetails();
		teamRequest.setValues(ImmutableList.of("101", "102"));
		oidcClaims.put(OIDCClaimName.team, teamRequest);
		
		// method under test
		Map<OIDCClaimName, Object> result=openIDConnectManagerImpl.getUserInfo(USER_ID, Collections.singletonList(OAuthScope.openid), oidcClaims);

		assertEquals(EMAIL, result.get(OIDCClaimName.email));
		assertTrue((Boolean)result.get(OIDCClaimName.email_verified));
		assertEquals(USER_ID, result.get(OIDCClaimName.userid));
		assertEquals(new Long(now.getTime()/1000L), (Long)result.get(OIDCClaimName.validated_at));
		assertEquals(Collections.singletonList("101"), result.get(OIDCClaimName.team));
	}

	// make sure we correctly handle when information is unavailable
	@Test
	public void testGetUserInfo_internal_missing_info() {
		VerificationSubmission verificationSubmission = new VerificationSubmission();
		when(userProfileManager.getCurrentVerificationSubmission(USER_ID_LONG)).thenReturn(verificationSubmission);
		when(mockGroupMembersDAO.filterUserGroups(eq(USER_ID), (List<String>)any())).thenReturn(Collections.EMPTY_LIST);

		Map<OIDCClaimName, OIDCClaimsRequestDetails> oidcClaims = new HashMap<OIDCClaimName, OIDCClaimsRequestDetails>();
		oidcClaims.put(OIDCClaimName.validated_at, null);
		OIDCClaimsRequestDetails teamRequest = new OIDCClaimsRequestDetails();
		teamRequest.setValues(ImmutableList.of("101", "102"));
		oidcClaims.put(OIDCClaimName.team, teamRequest);
		
		// method under test
		Map<OIDCClaimName, Object> result=openIDConnectManagerImpl.getUserInfo(USER_ID, Collections.singletonList(OAuthScope.openid), oidcClaims);

		assertFalse(result.containsKey(OIDCClaimName.validated_at));
		assertEquals(Collections.EMPTY_LIST, result.get(OIDCClaimName.team));
	}

	@Test
	public void testGetUserInfo_internal_noOpenIDScope() {
		Map<OIDCClaimName, OIDCClaimsRequestDetails> oidcClaims = new HashMap<OIDCClaimName, OIDCClaimsRequestDetails>();
		Map<OIDCClaimName, String> result=openIDConnectManagerImpl.getUserInfo(USER_ID, Collections.EMPTY_LIST, oidcClaims);
		assertTrue(result.isEmpty());
	}

	@Test
	public void testGetAccessToken() {
		OIDCAuthorizationRequest authorizationRequest = createAuthorizationRequest(ImmutableList.of("id_token", "userinfo"));

		OAuthAuthorizationResponse authResponse = openIDConnectManagerImpl.authorizeClient(userInfo, authorizationRequest);
		String code = authResponse.getAccess_code();

		String expectedIdToken = "ID-TOKEN";
		when(oidcTokenHelper.createOIDCIdToken(eq(OAUTH_ENDPOINT), eq(ppid), eq(OAUTH_CLIENT_ID), anyLong(), 
				eq(NONCE), eq(now), anyString(), userInfoCaptor.capture())).thenReturn(expectedIdToken);
		
		String expectedAccessToken = "ACCESS-TOKEN";
		when(oidcTokenHelper.createOIDCaccessToken(eq(OAUTH_ENDPOINT), eq(ppid), eq(OAUTH_CLIENT_ID), anyLong(),
				eq(now), anyString(), scopesCaptor.capture(), claimsCaptor.capture())).thenReturn(expectedAccessToken);
		
		// elsewhere we test that we correctly build up the requested user-info
		// here we just spot check a few fields to make sure everything's wired up

		// method under test
		OIDCTokenResponse tokenResponse = openIDConnectManagerImpl.getAccessToken(code, OAUTH_CLIENT_ID, REDIRCT_URIS.get(0), OAUTH_ENDPOINT);
		
		// verifying the mock token indirectly verifies all param's were correctly passed to oidcTokenHelper.createOIDCIdToken()
		assertEquals(expectedIdToken, tokenResponse.getId_token());
		// just spot check a few fields to make sure everything's wired up
		Map<OIDCClaimName, Object> userInfo = userInfoCaptor.getValue();
		assertEquals(EMAIL, userInfo.get(OIDCClaimName.email));
		assertTrue((Boolean)userInfo.get(OIDCClaimName.email_verified));
		assertEquals(USER_ID, userInfo.get(OIDCClaimName.userid));

		assertEquals(expectedAccessToken, tokenResponse.getAccess_token());
		assertEquals(Collections.singletonList(OAuthScope.openid), scopesCaptor.getValue());
		for (OIDCClaimName claimName : mockClaimProviders.keySet()) {
			assertTrue(claimsCaptor.getValue().containsKey(claimName));
			assertNull(claimsCaptor.getValue().get(claimName));
		}
	
		assertNull(tokenResponse.getRefresh_token());  // in the future we will provide a refresh token
	}
	
	@Test
	public void testGetAccessToken_invalidAuthCode() {
		String incorrectlyEncryptedCode = "some invalid code";
		when(mockStackEncrypter.decryptStackEncryptedAndBase64EncodedString(incorrectlyEncryptedCode)).thenThrow(new RuntimeException());
		try {
			// method under test
			openIDConnectManagerImpl.getAccessToken(incorrectlyEncryptedCode, OAUTH_CLIENT_ID, REDIRCT_URIS.get(0), OAUTH_ENDPOINT);
			fail("IllegalArgumentException expected");
		}  catch (IllegalArgumentException e) {
			// as expected
		}
		
		// this code is not a valid AuthorizationRequest object
		String invalidAuthorizationObjectCode = "not correctly serialized json";
		try {
			// method under test
			openIDConnectManagerImpl.getAccessToken(invalidAuthorizationObjectCode, OAUTH_CLIENT_ID, REDIRCT_URIS.get(0), OAUTH_ENDPOINT);
			fail("IllegalArgumentException expected");
		}  catch (IllegalStateException e) {
			// as expected
		}
	}

	@Test
	public void testGetAccessToken_expiredAuthCode() throws Exception {
		when(mockClock.now()).thenReturn(new Date(System.currentTimeMillis()-100000L));
		OIDCAuthorizationRequest authorizationRequest = createAuthorizationRequest();

		OAuthAuthorizationResponse authResponse = openIDConnectManagerImpl.authorizeClient(userInfo, authorizationRequest);
		
		// now let's doctor the authorization time stamp:
		String code = authResponse.getAccess_code();

		// now let's return the clock to normal, making the token expire
		when(mockClock.now()).thenReturn(new Date());

		// method under test
		try {
			openIDConnectManagerImpl.getAccessToken(code, OAUTH_CLIENT_ID, REDIRCT_URIS.get(0), OAUTH_ENDPOINT);
			fail("IllegalArgumentException expected");
		}  catch (IllegalArgumentException e) {
			// as expected
		}

	}

	@Test
	public void testGetAccessToken_mismatchRedirectURI() {
		OIDCAuthorizationRequest authorizationRequest = createAuthorizationRequest();

		OAuthAuthorizationResponse authResponse = openIDConnectManagerImpl.authorizeClient(userInfo, authorizationRequest);
		// method under test
		try {
			openIDConnectManagerImpl.getAccessToken(authResponse.getAccess_code(), OAUTH_CLIENT_ID, "wrong redirect uri", OAUTH_ENDPOINT);
			fail("IllegalArgumentException expected");
		}  catch (IllegalArgumentException e) {
			// as expected
		}
		
	}
	
	@Test
	public void testGetAccessToken_noOpenIdScope() {
		OIDCAuthorizationRequest authorizationRequest = createAuthorizationRequest();
		authorizationRequest.setScope(null); // omit the 'openid' scope.  This should suppress the idToken

		OAuthAuthorizationResponse authResponse = openIDConnectManagerImpl.authorizeClient(userInfo, authorizationRequest);
		String code = authResponse.getAccess_code();

		String expectedAccessToken = "ACCESS-TOKEN";
		when(oidcTokenHelper.createOIDCaccessToken(eq(OAUTH_ENDPOINT), anyString(), eq(OAUTH_CLIENT_ID), anyLong(),
				eq(now), anyString(), (List<OAuthScope>)any(), (Map<OIDCClaimName, OIDCClaimsRequestDetails>)any())).thenReturn(expectedAccessToken);
		
		// method under test
		OIDCTokenResponse tokenResponse = openIDConnectManagerImpl.getAccessToken(code, OAUTH_CLIENT_ID, REDIRCT_URIS.get(0), OAUTH_ENDPOINT);
		
		verify(oidcTokenHelper, never()).
			createOIDCIdToken(anyString(), anyString(), anyString(), anyLong(), anyString(), (Date)any(), anyString(), (Map)any());
		
		assertNull(tokenResponse.getId_token());

		assertNotNull(tokenResponse.getAccess_token());
	}
	
	private UserAuthorization createUserAuthorization() {
		UserAuthorization userAuthorization = new UserAuthorization();
		Map<OIDCClaimName, OIDCClaimsRequestDetails> oidcClaims = new HashMap<OIDCClaimName, OIDCClaimsRequestDetails>();
		oidcClaims.put(OIDCClaimName.userid, null);
		oidcClaims.put(OIDCClaimName.email, null);
		oidcClaims.put(OIDCClaimName.email_verified, null);
		userAuthorization.setOidcClaims(oidcClaims);
		userAuthorization.setScopes(Arrays.asList(OAuthScope.values()));
		userAuthorization.setUserInfo(userInfo);
		return userAuthorization;
	}
	
	@Test
	public void testGetUserInfoAsMap() {
		// if the client omits a signing algorithm it means it wants the UserInfo as json
		oauthClient.setUserinfo_signed_response_alg(null);
		
		// method under test
		Map<OIDCClaimName,Object> userInfo = (Map<OIDCClaimName,Object>)openIDConnectManagerImpl.getUserInfo(createUserAuthorization(), OAUTH_CLIENT_ID, OAUTH_ENDPOINT);

		assertEquals(USER_ID, userInfo.get(OIDCClaimName.userid));
		assertEquals(EMAIL, userInfo.get(OIDCClaimName.email));
		assertTrue((Boolean)userInfo.get(OIDCClaimName.email_verified));
	}

	@Test
	public void testGetUserInfoAsJWT() {
		// if the client sets a signing algorithm it means it wants the UserInfo json
		// to be encoded as a JWT and signed
		oauthClient.setUserinfo_signed_response_alg(OIDCSigningAlgorithm.RS256);
		
		String expectedIdToken = "ID-TOKEN";
		when(oidcTokenHelper.createOIDCIdToken(eq(OAUTH_ENDPOINT), anyString(), eq(OAUTH_CLIENT_ID), anyLong(), 
				eq(null), eq(now), anyString(), userInfoCaptor.capture())).thenReturn(expectedIdToken);

		// method under test
		JWTWrapper jwt = (JWTWrapper)openIDConnectManagerImpl.getUserInfo(createUserAuthorization(), OAUTH_CLIENT_ID, OAUTH_ENDPOINT);
		
		assertEquals(expectedIdToken, jwt.getJwt());
		
		Map<OIDCClaimName, Object> userInfo = userInfoCaptor.getValue();
		assertEquals(USER_ID, userInfo.get(OIDCClaimName.userid));
		assertEquals(EMAIL, userInfo.get(OIDCClaimName.email));
		assertTrue((Boolean)userInfo.get(OIDCClaimName.email_verified));
	}

}
