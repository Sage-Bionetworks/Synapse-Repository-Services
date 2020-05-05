package org.sagebionetworks.repo.manager.oauth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.manager.oauth.OpenIDConnectManager.getScopeHash;

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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.StackEncrypter;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.manager.oauth.claimprovider.EmailClaimProvider;
import org.sagebionetworks.repo.manager.oauth.claimprovider.EmailVerifiedClaimProvider;
import org.sagebionetworks.repo.manager.oauth.claimprovider.OIDCClaimProvider;
import org.sagebionetworks.repo.manager.oauth.claimprovider.TeamClaimProvider;
import org.sagebionetworks.repo.manager.oauth.claimprovider.UserIdClaimProvider;
import org.sagebionetworks.repo.manager.oauth.claimprovider.ValidatedAtClaimProvider;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.UnauthenticatedException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthenticationDAO;
import org.sagebionetworks.repo.model.auth.OAuthClientDao;
import org.sagebionetworks.repo.model.auth.OAuthDao;
import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
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
import com.google.common.collect.ImmutableSet;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;

@ExtendWith(MockitoExtension.class)
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
	private OAuthDao mockOauthDao;

	@Mock
	private AuthenticationDAO mockAuthDao;

	@Mock
	private OIDCTokenHelper oidcTokenHelper;
	
	@InjectMocks
	private OpenIDConnectManagerImpl openIDConnectManagerImpl;
	
	@Mock
	private StackConfiguration mockStackConfigurations;
	
	@Mock
	private NotificationEmailDAO mockNotificationEmailDao;
	
	@Mock
	private UserProfileManager mockUserProfileManager;
	
	@Mock
	private GroupMembersDAO mockGroupMembersDAO;

	@Mock
	private UserManager mockUserManager;
		
	@Mock
	private Jwt<JwsHeader,Claims> mockJWT;

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
	private VerificationSubmission verificationSubmission;
	
	@BeforeEach
	public void setUp() throws Exception {
		userInfo = new UserInfo(false);
		userInfo.setId(USER_ID_LONG);
		userInfo.setGroups(Collections.singleton(USER_ID_LONG));

		anonymousUserInfo = new UserInfo(false);
		anonymousUserInfo.setId(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
		
		oauthClient = new OAuthClient();
		oauthClient.setVerified(true);
		oauthClient.setClient_id(OAUTH_CLIENT_ID);
		oauthClient.setRedirect_uris(REDIRCT_URIS);
		oauthClient.setUserinfo_signed_response_alg(OIDCSigningAlgorithm.RS256);

		clientSpecificEncodingSecret = EncryptionUtils.newSecretKey();
		this.ppid = EncryptionUtils.encrypt(USER_ID, clientSpecificEncodingSecret);
		
		now = new Date();

		// we don't wire up all the claim providers, just a representative sample returning a variety of object types
		mockClaimProviders = new HashMap<OIDCClaimName, OIDCClaimProvider>();

		mockClaimProviders.put(OIDCClaimName.email, mockEmailClaimProvider);
		
		mockClaimProviders.put(OIDCClaimName.email_verified, mockEmailVerifiedClaimProvider);
		
		mockClaimProviders.put(OIDCClaimName.userid, mockUserIdClaimProvider);
		
		VerificationState verificationState = new VerificationState();
		verificationState.setState(VerificationStateEnum.APPROVED);
		verificationState.setCreatedOn(now);
		verificationSubmission = new VerificationSubmission();
		verificationSubmission.setStateHistory(Collections.singletonList(verificationState));
		mockClaimProviders.put(OIDCClaimName.validated_at, mockValidatedAtClaimProvider);
		
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
		client.setVerified(true);

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
		client.setVerified(true);

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
		client.setVerified(true);

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
		when(mockOauthClientDao.getOAuthClient(OAUTH_CLIENT_ID)).thenReturn(oauthClient);
		when(mockOauthClientDao.isOauthClientVerified(OAUTH_CLIENT_ID)).thenReturn(true);

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
		when(mockOauthClientDao.getOAuthClient(OAUTH_CLIENT_ID)).thenReturn(oauthClient);
		when(mockOauthClientDao.isOauthClientVerified(OAUTH_CLIENT_ID)).thenReturn(true);

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
		when(mockOauthClientDao.getOAuthClient(OAUTH_CLIENT_ID)).thenReturn(oauthClient);	
		when(mockOauthClientDao.isOauthClientVerified(OAUTH_CLIENT_ID)).thenReturn(true);

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
		when(mockOauthClientDao.getOAuthClient(OAUTH_CLIENT_ID)).thenReturn(oauthClient);
		when(mockOauthClientDao.isOauthClientVerified(OAUTH_CLIENT_ID)).thenReturn(true);
		
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
	public void testGetAuthenticationRequestDescription_UnverifiedClient() {
		when(mockOauthClientDao.getOAuthClient(OAUTH_CLIENT_ID)).thenReturn(oauthClient);
		when(mockOauthClientDao.isOauthClientVerified(OAUTH_CLIENT_ID)).thenReturn(false);

		OIDCAuthorizationRequest authorizationRequest = createAuthorizationRequest(Collections.singletonList("userinfo"));
		
		OAuthClientNotVerifiedException ex =  assertThrows(OAuthClientNotVerifiedException.class, () -> {
			// method under test
			openIDConnectManagerImpl.getAuthenticationRequestDescription(authorizationRequest);
		});
		
		assertEquals("The OAuth client (" + OAUTH_CLIENT_ID + ") is not verified.", ex.getMessage());

	}
	
	@Test
	public void tesHasUserGrantedConsent_NoConsentOrExpired() throws Exception {
		OIDCAuthorizationRequest authorizationRequest = createAuthorizationRequest();
		
		String scopeHash = getScopeHash(authorizationRequest);
		
		long now = System.currentTimeMillis();
		Date threshold = new Date(now-365L*24L*3600L*1000L);
		when(mockClock.currentTimeMillis()).thenReturn(now);

		when(mockOauthDao.lookupAuthorizationConsent(eq(userInfo.getId()), 
				eq(Long.valueOf(OAUTH_CLIENT_ID)), 
				eq(scopeHash), eq(threshold))).thenReturn(false);

		// method under test
		boolean result = openIDConnectManagerImpl.hasUserGrantedConsent(userInfo, authorizationRequest);
		
		verify(mockOauthDao).lookupAuthorizationConsent(eq(userInfo.getId()), 
				eq(Long.valueOf(OAUTH_CLIENT_ID)), 
				eq(scopeHash), eq(threshold));
		
		assertFalse(result);
	}

	@Test
	public void tesHasUserGrantedConsent_Granted() throws Exception {
		OIDCAuthorizationRequest authorizationRequest = createAuthorizationRequest();
		
		String scopeHash = getScopeHash(authorizationRequest);
		
		long now = System.currentTimeMillis();
		Date threshold = new Date(now-365L*24L*3600L*1000L);
		when(mockClock.currentTimeMillis()).thenReturn(now);

		when(mockOauthDao.lookupAuthorizationConsent(eq(userInfo.getId()), 
				eq(Long.valueOf(OAUTH_CLIENT_ID)), 
				eq(scopeHash), eq(threshold))).thenReturn(true);

		// method under test
		boolean result = openIDConnectManagerImpl.hasUserGrantedConsent(userInfo, authorizationRequest);
		
		verify(mockOauthDao).lookupAuthorizationConsent(eq(userInfo.getId()), 
				eq(Long.valueOf(OAUTH_CLIENT_ID)), 
				eq(scopeHash), eq(threshold));
		
		assertTrue(result);
	}

	@Test
	public void testAuthorizeClient() throws Exception {
		when(mockStackEncrypter.decryptStackEncryptedAndBase64EncodedString(anyString())).then(returnsFirstArg());	
		when(mockOauthClientDao.getOAuthClient(OAUTH_CLIENT_ID)).thenReturn(oauthClient);	
		when(mockOauthClientDao.isOauthClientVerified(OAUTH_CLIENT_ID)).thenReturn(true);
		when(mockStackEncrypter.encryptAndBase64EncodeStringWithStackKey(anyString())).then(returnsFirstArg());	
		when(mockAuthDao.getSessionValidatedOn(USER_ID_LONG)).thenReturn(now);
		when(mockClock.now()).thenReturn(new Date());

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
	public void testAuthorizeClient_unverified() throws Exception {
		when(mockOauthClientDao.getOAuthClient(OAUTH_CLIENT_ID)).thenReturn(oauthClient);
		when(mockOauthClientDao.isOauthClientVerified(OAUTH_CLIENT_ID)).thenReturn(false);

		OIDCAuthorizationRequest authorizationRequest = createAuthorizationRequest();

		assertThrows(OAuthClientNotVerifiedException.class, ()-> {
			// method under test
			openIDConnectManagerImpl.authorizeClient(userInfo, authorizationRequest);
		});
		
		verify(mockOauthClientDao).isOauthClientVerified(OAUTH_CLIENT_ID);
	}
	
	@Test
	public void testPPID() {
		when(mockOauthClientDao.getSectorIdentifierSecretForClient(OAUTH_CLIENT_ID)).thenReturn(clientSpecificEncodingSecret);

		// method under test
		String ppid = openIDConnectManagerImpl.ppid(USER_ID, OAUTH_CLIENT_ID);
		assertEquals(USER_ID, EncryptionUtils.decrypt(ppid, clientSpecificEncodingSecret));
	}

	@Test
	public void testPPIDForSynapse() {
		// method under test
		String ppid = openIDConnectManagerImpl.ppid(USER_ID, AuthorizationConstants.SYNAPSE_OAUTH_CLIENT_ID);
		assertEquals(USER_ID, ppid);
	}

	@Test
	public void testGetUserIdFromPPID() {
		when(mockOauthClientDao.getSectorIdentifierSecretForClient(OAUTH_CLIENT_ID)).thenReturn(clientSpecificEncodingSecret);
		when(mockOauthClientDao.isOauthClientVerified(OAUTH_CLIENT_ID)).thenReturn(true);
		String ppid = openIDConnectManagerImpl.ppid(USER_ID, OAUTH_CLIENT_ID);

		// method under test		
		assertEquals(USER_ID, openIDConnectManagerImpl.getUserIdFromPPID(ppid, OAUTH_CLIENT_ID));
	}

	@Test
	public void testGetUserIdFromPPIDForSynapse() {
		// method under test		
		assertEquals(USER_ID, openIDConnectManagerImpl.getUserIdFromPPID(USER_ID, AuthorizationConstants.SYNAPSE_OAUTH_CLIENT_ID));
	}
	
	@Test
	public void testGetUserInfo_internal() {
		when(mockUserProfileManager.getCurrentVerificationSubmission(USER_ID_LONG)).thenReturn(verificationSubmission);
		when(mockNotificationEmailDao.getNotificationEmailForPrincipal(USER_ID_LONG)).thenReturn(EMAIL);
		when(mockGroupMembersDAO.filterUserGroups(eq(USER_ID), (List<String>)any())).thenReturn(Collections.singletonList("101"));
		
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
		when(mockUserProfileManager.getCurrentVerificationSubmission(USER_ID_LONG)).thenReturn(verificationSubmission);
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
		when(mockStackEncrypter.decryptStackEncryptedAndBase64EncodedString(anyString())).then(returnsFirstArg());	
		when(mockOauthClientDao.getOAuthClient(OAUTH_CLIENT_ID)).thenReturn(oauthClient);
		when(mockOauthClientDao.isOauthClientVerified(OAUTH_CLIENT_ID)).thenReturn(true);
		when(mockStackEncrypter.encryptAndBase64EncodeStringWithStackKey(anyString())).then(returnsFirstArg());	
		when(mockOauthClientDao.getSectorIdentifierSecretForClient(OAUTH_CLIENT_ID)).thenReturn(clientSpecificEncodingSecret);
		when(mockAuthDao.getSessionValidatedOn(USER_ID_LONG)).thenReturn(now);
		when(mockClock.currentTimeMillis()).thenReturn(System.currentTimeMillis());
		when(mockClock.now()).thenReturn(new Date());
		when(mockNotificationEmailDao.getNotificationEmailForPrincipal(USER_ID_LONG)).thenReturn(EMAIL);
		when(mockUserProfileManager.getCurrentVerificationSubmission(USER_ID_LONG)).thenReturn(verificationSubmission);

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
		when(mockStackEncrypter.decryptStackEncryptedAndBase64EncodedString(anyString())).then(returnsFirstArg());	
		when(mockOauthClientDao.isOauthClientVerified(OAUTH_CLIENT_ID)).thenReturn(true);

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
		when(mockOauthClientDao.getOAuthClient(OAUTH_CLIENT_ID)).thenReturn(oauthClient);
		when(mockOauthClientDao.isOauthClientVerified(OAUTH_CLIENT_ID)).thenReturn(true);
		when(mockStackEncrypter.encryptAndBase64EncodeStringWithStackKey(anyString())).then(returnsFirstArg());	
		when(mockStackEncrypter.decryptStackEncryptedAndBase64EncodedString(anyString())).then(returnsFirstArg());	
		// let's doctor the authorization time stamp to be long ago
		when(mockClock.now()).thenReturn(new Date(System.currentTimeMillis()-100000L));
		
		OIDCAuthorizationRequest authorizationRequest = createAuthorizationRequest();

		OAuthAuthorizationResponse authResponse = openIDConnectManagerImpl.authorizeClient(userInfo, authorizationRequest);
		
		String code = authResponse.getAccess_code();

		// now let's return the clock to normal, making the token expire
		when(mockClock.currentTimeMillis()).thenReturn(System.currentTimeMillis());

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
		when(mockStackEncrypter.decryptStackEncryptedAndBase64EncodedString(anyString())).then(returnsFirstArg());	
		when(mockOauthClientDao.getOAuthClient(OAUTH_CLIENT_ID)).thenReturn(oauthClient);	
		when(mockOauthClientDao.isOauthClientVerified(OAUTH_CLIENT_ID)).thenReturn(true);
		when(mockStackEncrypter.encryptAndBase64EncodeStringWithStackKey(anyString())).then(returnsFirstArg());	
		when(mockAuthDao.getSessionValidatedOn(USER_ID_LONG)).thenReturn(now);
		when(mockClock.currentTimeMillis()).thenReturn(System.currentTimeMillis());
		when(mockClock.now()).thenReturn(new Date());

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
		when(mockStackEncrypter.decryptStackEncryptedAndBase64EncodedString(anyString())).then(returnsFirstArg());	
		when(mockOauthClientDao.getOAuthClient(OAUTH_CLIENT_ID)).thenReturn(oauthClient);	
		when(mockStackEncrypter.encryptAndBase64EncodeStringWithStackKey(anyString())).then(returnsFirstArg());	
		when(mockOauthClientDao.getSectorIdentifierSecretForClient(OAUTH_CLIENT_ID)).thenReturn(clientSpecificEncodingSecret);
		when(mockOauthClientDao.isOauthClientVerified(OAUTH_CLIENT_ID)).thenReturn(true);
		when(mockAuthDao.getSessionValidatedOn(USER_ID_LONG)).thenReturn(now);
		when(mockClock.currentTimeMillis()).thenReturn(System.currentTimeMillis());
		when(mockClock.now()).thenReturn(new Date());

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
	
	@Test
	public void testGetAccessToken_clientUnverified() {
		String code = "Some code";
		
		when(mockOauthClientDao.isOauthClientVerified(OAUTH_CLIENT_ID)).thenReturn(false);
		
		assertThrows(OAuthClientNotVerifiedException.class, () -> {
			// method under test
			openIDConnectManagerImpl.getAccessToken(code, OAUTH_CLIENT_ID, REDIRCT_URIS.get(0), OAUTH_ENDPOINT);
		});
		
		verify(mockOauthClientDao).isOauthClientVerified(OAUTH_CLIENT_ID);
		
	}
	
	private static final String ACCESS_TOKEN = "access token";

	private void mockAccessToken(String oAuthClientId) {
		when(oidcTokenHelper.parseJWT(ACCESS_TOKEN)).thenReturn(mockJWT);
		Claims claims = Jwts.claims();
		claims.setAudience(oAuthClientId);
		String ppid;
		if (AuthorizationConstants.SYNAPSE_OAUTH_CLIENT_ID.equals(oAuthClientId)) {
			ppid = USER_ID;
		} else {
			ppid = EncryptionUtils.encrypt(USER_ID, clientSpecificEncodingSecret);
		
		}
		claims.setSubject(ppid);
		List<OAuthScope> scopes = Arrays.asList(OAuthScope.values());
		Map<OIDCClaimName, OIDCClaimsRequestDetails> oidcClaims = new HashMap<OIDCClaimName, OIDCClaimsRequestDetails>();
		oidcClaims.put(OIDCClaimName.userid, null);
		oidcClaims.put(OIDCClaimName.email, null);
		oidcClaims.put(OIDCClaimName.email_verified, null);
		ClaimsJsonUtil.addAccessClaims(scopes, oidcClaims, claims);
		when(mockJWT.getBody()).thenReturn(claims);
	}
	
	@Test
	public void testGetUserInfoAsMap() {
		when(mockOauthClientDao.getOAuthClient(OAUTH_CLIENT_ID)).thenReturn(oauthClient);	
		when(mockOauthClientDao.getSectorIdentifierSecretForClient(OAUTH_CLIENT_ID)).thenReturn(clientSpecificEncodingSecret);
		when(mockNotificationEmailDao.getNotificationEmailForPrincipal(USER_ID_LONG)).thenReturn(EMAIL);
		when(mockOauthClientDao.isOauthClientVerified(OAUTH_CLIENT_ID)).thenReturn(true);
		mockAccessToken(OAUTH_CLIENT_ID);
		// if the client omits a signing algorithm it means it wants the UserInfo as json
		oauthClient.setUserinfo_signed_response_alg(null);
		
		// method under test
		Map<OIDCClaimName,Object> userInfo = (Map<OIDCClaimName,Object>)openIDConnectManagerImpl.
				getUserInfo(ACCESS_TOKEN, OAUTH_ENDPOINT);

		verify(mockJWT).getBody();
		verify(mockOauthClientDao).getSectorIdentifierSecretForClient(OAUTH_CLIENT_ID);
		
		assertEquals(this.ppid, userInfo.get(OIDCClaimName.sub));
		assertEquals(USER_ID, userInfo.get(OIDCClaimName.userid));
		assertEquals(EMAIL, userInfo.get(OIDCClaimName.email));
		assertTrue((Boolean)userInfo.get(OIDCClaimName.email_verified));
	}

	@Test
	public void testGetUserInfoAsJWT() {
		when(mockOauthClientDao.getOAuthClient(OAUTH_CLIENT_ID)).thenReturn(oauthClient);	
		when(mockOauthClientDao.getSectorIdentifierSecretForClient(OAUTH_CLIENT_ID)).thenReturn(clientSpecificEncodingSecret);
		when(mockAuthDao.getSessionValidatedOn(USER_ID_LONG)).thenReturn(now);
		when(mockClock.currentTimeMillis()).thenReturn(System.currentTimeMillis());
		when(mockNotificationEmailDao.getNotificationEmailForPrincipal(USER_ID_LONG)).thenReturn(EMAIL);
		when(mockOauthClientDao.isOauthClientVerified(OAUTH_CLIENT_ID)).thenReturn(true);
		mockAccessToken(OAUTH_CLIENT_ID);

		// if the client sets a signing algorithm it means it wants the UserInfo json
		// to be encoded as a JWT and signed
		oauthClient.setUserinfo_signed_response_alg(OIDCSigningAlgorithm.RS256);
		
		String expectedIdToken = "ID-TOKEN";
		when(oidcTokenHelper.createOIDCIdToken(eq(OAUTH_ENDPOINT), eq(this.ppid), eq(OAUTH_CLIENT_ID), anyLong(), 
				eq(null), eq(now), anyString(), userInfoCaptor.capture())).thenReturn(expectedIdToken);

		// method under test
		JWTWrapper jwt = (JWTWrapper)openIDConnectManagerImpl.getUserInfo(ACCESS_TOKEN, OAUTH_ENDPOINT);
		
		assertEquals(expectedIdToken, jwt.getJwt());
		
		Map<OIDCClaimName, Object> userInfo = userInfoCaptor.getValue();
		assertEquals(USER_ID, userInfo.get(OIDCClaimName.userid));
		assertEquals(EMAIL, userInfo.get(OIDCClaimName.email));
		assertTrue((Boolean)userInfo.get(OIDCClaimName.email_verified));
	}
	
	@Test
	public void testGetUserInfoDefaultClient() {
		mockAccessToken(AuthorizationConstants.SYNAPSE_OAUTH_CLIENT_ID);
		when(mockNotificationEmailDao.getNotificationEmailForPrincipal(USER_ID_LONG)).thenReturn(EMAIL);

		// method under test
		Map<OIDCClaimName,Object> userInfo = (Map<OIDCClaimName,Object>)
				openIDConnectManagerImpl.getUserInfo(ACCESS_TOKEN, OAUTH_ENDPOINT);

		assertEquals(USER_ID, userInfo.get(OIDCClaimName.sub));
		assertEquals(USER_ID, userInfo.get(OIDCClaimName.userid));
		assertEquals(EMAIL, userInfo.get(OIDCClaimName.email));
		assertTrue((Boolean)userInfo.get(OIDCClaimName.email_verified));
		verify(mockOauthClientDao, never()).getSectorIdentifierSecretForClient(OAUTH_CLIENT_ID);
		verify(mockOauthClientDao, never()).isOauthClientVerified(AuthorizationConstants.SYNAPSE_OAUTH_CLIENT_ID);
	}

	@Test
	public void testGetUserInfoNoAudience() {
		mockAccessToken(null);
		
		// method under test
		assertThrows(IllegalArgumentException.class, () -> openIDConnectManagerImpl.getUserInfo(ACCESS_TOKEN, OAUTH_ENDPOINT));
	}
	
	@Test
	public void testGetUserId() {
		String token = "access token";
		when(oidcTokenHelper.parseJWT(token)).thenReturn(mockJWT);
		Claims claims = Jwts.claims();
		ClaimsJsonUtil.addAccessClaims(Collections.EMPTY_LIST, Collections.EMPTY_MAP, claims);
		when(mockJWT.getBody()).thenReturn(claims);
		claims.setAudience(OAUTH_CLIENT_ID);
		when(mockOauthClientDao.getSectorIdentifierSecretForClient(OAUTH_CLIENT_ID)).thenReturn(clientSpecificEncodingSecret);
		when(mockOauthClientDao.isOauthClientVerified(OAUTH_CLIENT_ID)).thenReturn(true);
		String ppid = openIDConnectManagerImpl.ppid(USER_ID, OAUTH_CLIENT_ID);
		claims.setSubject(ppid);

		// method under test
		assertEquals(USER_ID, openIDConnectManagerImpl.getUserId(token));
		
		verify(oidcTokenHelper).parseJWT(token);
	}
	
	@Test
	public void testValidateClientVerificationStatusVerified() {
		String clientId = OAUTH_CLIENT_ID;
		
		when(mockOauthClientDao.isOauthClientVerified(clientId)).thenReturn(true);

		// Method under test
		openIDConnectManagerImpl.validateClientVerificationStatus(clientId);
		
		verify(mockOauthClientDao).isOauthClientVerified(OAUTH_CLIENT_ID);
		
	}
	
	@Test
	public void testValidateClientVerificationStatusUnverified() {
		String clientId = OAUTH_CLIENT_ID;
		
		when(mockOauthClientDao.isOauthClientVerified(clientId)).thenReturn(false);
		
		OAuthClientNotVerifiedException ex = assertThrows(OAuthClientNotVerifiedException.class, ()-> {
			// Method under test
			openIDConnectManagerImpl.validateClientVerificationStatus(clientId);
		});
		
		assertEquals("The OAuth client (" + OAUTH_CLIENT_ID + ") is not verified.", ex.getMessage());
		
		verify(mockOauthClientDao).isOauthClientVerified(OAUTH_CLIENT_ID);
	}

}
