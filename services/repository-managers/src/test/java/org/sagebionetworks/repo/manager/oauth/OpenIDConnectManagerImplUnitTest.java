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
import static org.mockito.ArgumentMatchers.isNull;
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
import org.sagebionetworks.manager.util.OAuthPermissionUtils;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.manager.authentication.PersonalAccessTokenManager;
import org.sagebionetworks.repo.manager.oauth.claimprovider.EmailClaimProvider;
import org.sagebionetworks.repo.manager.oauth.claimprovider.EmailVerifiedClaimProvider;
import org.sagebionetworks.repo.manager.oauth.claimprovider.OIDCClaimProvider;
import org.sagebionetworks.repo.manager.oauth.claimprovider.TeamClaimProvider;
import org.sagebionetworks.repo.manager.oauth.claimprovider.UserIdClaimProvider;
import org.sagebionetworks.repo.manager.oauth.claimprovider.ValidatedAtClaimProvider;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthenticationDAO;
import org.sagebionetworks.repo.model.auth.OAuthClientDao;
import org.sagebionetworks.repo.model.auth.OAuthDao;
import org.sagebionetworks.repo.model.auth.TokenType;
import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
import org.sagebionetworks.repo.model.oauth.OAuthAuthorizationResponse;
import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.repo.model.oauth.OAuthRefreshTokenInformation;
import org.sagebionetworks.repo.model.oauth.OAuthResponseType;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.oauth.OAuthTokenRevocationRequest;
import org.sagebionetworks.repo.model.oauth.OIDCAuthorizationRequest;
import org.sagebionetworks.repo.model.oauth.OIDCAuthorizationRequestDescription;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequest;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;
import org.sagebionetworks.repo.model.oauth.OIDCSigningAlgorithm;
import org.sagebionetworks.repo.model.oauth.OIDCTokenResponse;
import org.sagebionetworks.repo.model.oauth.TokenTypeHint;
import org.sagebionetworks.repo.model.verification.VerificationState;
import org.sagebionetworks.repo.model.verification.VerificationStateEnum;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;
import org.sagebionetworks.repo.web.ForbiddenException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.OAuthBadRequestException;
import org.sagebionetworks.repo.web.OAuthErrorCode;
import org.sagebionetworks.repo.web.OAuthUnauthenticatedException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.securitytools.EncryptionUtils;
import org.sagebionetworks.util.Clock;

import com.google.common.collect.ImmutableList;

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
	private static final long EXPECTED_ACCESS_TOKEN_EXPIRATION_TIME_SECONDS = 3600*24L; // a day
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

	@Mock
	private OAuthRefreshTokenManager oauthRefreshTokenManager;

	@Mock
	private PersonalAccessTokenManager mockPersonalAccessTokenManager;

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
	private Claims mockClaims;

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
		
		OAuthBadRequestException ex = assertThrows(OAuthBadRequestException.class, () -> {
			// method under test
			OpenIDConnectManagerImpl.parseScopeString("openid foo");
		});
		assertEquals(OAuthErrorCode.invalid_scope, ex.getError());
		assertEquals("invalid_scope Unrecognized scope: foo", ex.getMessage());
		
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
		
		OAuthBadRequestException ex = assertThrows(OAuthBadRequestException.class, () -> {
			// method under test
			OpenIDConnectManagerImpl.validateAuthenticationRequest(authorizationRequest, client);
		});
		assertEquals(OAuthErrorCode.invalid_request, ex.getError());
		assertEquals("invalid_request Redirect URI is not a valid url: some invalid uri", ex.getMessage());
	}

	@Test
	public void testValidateAuthenticationRequest_invalidResponseType() {
		OAuthClient client = new OAuthClient();
		client.setRedirect_uris(REDIRCT_URIS);
		client.setVerified(true);

		OIDCAuthorizationRequest authorizationRequest = new OIDCAuthorizationRequest();
		authorizationRequest.setRedirectUri(REDIRCT_URIS.get(0));
		authorizationRequest.setResponseType(null);
		
		OAuthBadRequestException ex = assertThrows(OAuthBadRequestException.class, () -> {
			// method under test
			OpenIDConnectManagerImpl.validateAuthenticationRequest(authorizationRequest, client);
		});
		assertEquals(OAuthErrorCode.invalid_request, ex.getError());
		assertEquals("invalid_request Missing response_type.", ex.getMessage());
	}	

	private static final String NONCE = UUID.randomUUID().toString();
	
	// claimsTag must be 'id_token' or 'userinfo'
	private OIDCAuthorizationRequest createAuthorizationRequest(boolean includeIdTokenClaims, boolean includeUserInfoClaims) {
		OIDCAuthorizationRequest authorizationRequest = new OIDCAuthorizationRequest();
		authorizationRequest.setClientId(OAUTH_CLIENT_ID);
		authorizationRequest.setScope(OAuthScope.openid.name() + " " + OAuthScope.offline_access.name());
		OIDCClaimsRequest claims = new OIDCClaimsRequest();
		if (includeIdTokenClaims) {
			Map<String, OIDCClaimsRequestDetails> idTokenClaims = new HashMap<>();
			for (OIDCClaimName claimName : mockClaimProviders.keySet()) {
				idTokenClaims.put(claimName.name(), null);
			}
			claims.setId_token(idTokenClaims);
		}
		if (includeUserInfoClaims) {
			Map<String, OIDCClaimsRequestDetails> userInfoClaims = new HashMap<>();
			for (OIDCClaimName claimName : mockClaimProviders.keySet()) {
				userInfoClaims.put(claimName.name(), null);
			}
			claims.setUserinfo(userInfoClaims);
		}
		authorizationRequest.setClaims(claims);
		authorizationRequest.setResponseType(OAuthResponseType.code);
		authorizationRequest.setRedirectUri(REDIRCT_URIS.get(0));
		authorizationRequest.setNonce(NONCE);
		return authorizationRequest;
	}
	
	private OIDCAuthorizationRequest createAuthorizationRequest() {
		return createAuthorizationRequest(false, false);
	}
		
	@Test
	public void testGetAuthenticationRequestDescription_HappyCase_IdTokenClaims() {
		when(mockOauthClientDao.getOAuthClient(OAUTH_CLIENT_ID)).thenReturn(oauthClient);
		when(mockOauthClientDao.isOauthClientVerified(OAUTH_CLIENT_ID)).thenReturn(true);

		boolean includeIdToken = true;
		boolean includeUserInfo = false;

		OIDCAuthorizationRequest authorizationRequest = createAuthorizationRequest(includeIdToken, includeUserInfo);
		OIDCAuthorizationRequestDescription description = 
				// method under test
				openIDConnectManagerImpl.getAuthenticationRequestDescription(authorizationRequest);
		assertEquals(OAUTH_CLIENT_ID, description.getClientId());
		assertEquals(REDIRCT_URIS.get(0), description.getRedirect_uri());
		Set<String> expectedScope = new HashSet<String>();
		expectedScope.add(OAuthPermissionUtils.scopeDescription(OAuthScope.offline_access));
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

		boolean includeIdToken = false;
		boolean includeUserInfo = true;

		OIDCAuthorizationRequest authorizationRequest = createAuthorizationRequest(includeIdToken, includeUserInfo);
		OIDCAuthorizationRequestDescription description = 
				// method under test
				openIDConnectManagerImpl.getAuthenticationRequestDescription(authorizationRequest);
		assertEquals(OAUTH_CLIENT_ID, description.getClientId());
		assertEquals(REDIRCT_URIS.get(0), description.getRedirect_uri());
		Set<String> expectedScope = new HashSet<String>();
		expectedScope.add(OAuthPermissionUtils.scopeDescription(OAuthScope.offline_access));
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

		OAuthBadRequestException ex = assertThrows(OAuthBadRequestException.class, ()->{
				openIDConnectManagerImpl.getAuthenticationRequestDescription(authorizationRequest);
		});
		assertEquals(OAuthErrorCode.invalid_client, ex.getError());
		assertEquals("invalid_client Invalid OAuth Client ID: 42", ex.getMessage());
		
		authorizationRequest.setClientId(null);
		
		assertThrows(IllegalArgumentException.class, () -> {
			// method under test
			openIDConnectManagerImpl.getAuthenticationRequestDescription(authorizationRequest);
		});

	}

	@Test
	public void testGetAuthenticationRequestDescription_BadRedirectURI() {
		when(mockOauthClientDao.getOAuthClient(OAUTH_CLIENT_ID)).thenReturn(oauthClient);
		when(mockOauthClientDao.isOauthClientVerified(OAUTH_CLIENT_ID)).thenReturn(true);
		
		OIDCAuthorizationRequest authorizationRequest = createAuthorizationRequest();
		authorizationRequest.setRedirectUri("some other redir uri");

		OAuthBadRequestException ex = assertThrows(OAuthBadRequestException.class, () -> {
			// method under test
			openIDConnectManagerImpl.getAuthenticationRequestDescription(authorizationRequest);
		});
		assertEquals(OAuthErrorCode.invalid_request, ex.getError());
		assertEquals("invalid_request Redirect URI is not a valid url: some other redir uri", ex.getMessage());
	}
	
	@Test
	public void testGetAuthenticationRequestDescription_UnverifiedClient() {
		when(mockOauthClientDao.getOAuthClient(OAUTH_CLIENT_ID)).thenReturn(oauthClient);
		when(mockOauthClientDao.isOauthClientVerified(OAUTH_CLIENT_ID)).thenReturn(false);

		boolean includeIdToken = false;
		boolean includeUserInfo = true;

		OIDCAuthorizationRequest authorizationRequest = createAuthorizationRequest(includeIdToken, includeUserInfo);
		
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
		when(mockAuthDao.getAuthenticatedOn(USER_ID_LONG)).thenReturn(now);
		when(mockClock.now()).thenReturn(new Date());

		OIDCAuthorizationRequest authorizationRequest = createAuthorizationRequest();

		// method under test
		OAuthAuthorizationResponse authResponse = openIDConnectManagerImpl.authorizeClient(userInfo, authorizationRequest);
		String code = authResponse.getAccess_code();
		assertNotNull(code);
		String decrypted = mockStackEncrypter.decryptStackEncryptedAndBase64EncodedString(code);
		
		OIDCAuthorizationRequest authRequestFromCode = new OIDCAuthorizationRequest();
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(decrypted);
		authRequestFromCode.initializeFromJSONObject(adapter);
		
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

		assertThrows(OAuthUnauthenticatedException.class, () -> {
			// method under test
			openIDConnectManagerImpl.authorizeClient(anonymousUserInfo, authorizationRequest);
		});
	}

	@Test
	public void testAuthorizeClient_InvalidClientId() {
		OIDCAuthorizationRequest authorizationRequest = createAuthorizationRequest();
		authorizationRequest.setClientId("42");
		when(mockOauthClientDao.getOAuthClient("42")).thenThrow(new NotFoundException());

		OAuthBadRequestException ex = assertThrows(OAuthBadRequestException.class, () -> {
			// method under test
			openIDConnectManagerImpl.authorizeClient(userInfo, authorizationRequest);
		});
		assertEquals(OAuthErrorCode.invalid_client, ex.getError());
		assertEquals("invalid_client Invalid OAuth Client ID: 42", ex.getMessage());
	}

	@Test
	public void testAuthorizeClient_unverified() throws Exception {
		when(mockOauthClientDao.getOAuthClient(OAUTH_CLIENT_ID)).thenReturn(oauthClient);
		when(mockOauthClientDao.isOauthClientVerified(OAUTH_CLIENT_ID)).thenReturn(false);

		OIDCAuthorizationRequest authorizationRequest = createAuthorizationRequest();

		assertThrows(OAuthClientNotVerifiedException.class, () -> {
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
	public void testGetTokenResponseWithAuthorizationCode() {
		when(mockStackEncrypter.decryptStackEncryptedAndBase64EncodedString(anyString())).then(returnsFirstArg());	
		when(mockOauthClientDao.getOAuthClient(OAUTH_CLIENT_ID)).thenReturn(oauthClient);
		when(mockOauthClientDao.isOauthClientVerified(OAUTH_CLIENT_ID)).thenReturn(true);
		when(mockStackEncrypter.encryptAndBase64EncodeStringWithStackKey(anyString())).then(returnsFirstArg());
		when(mockOauthClientDao.getSectorIdentifierSecretForClient(OAUTH_CLIENT_ID)).thenReturn(clientSpecificEncodingSecret);
		when(mockAuthDao.getAuthenticatedOn(USER_ID_LONG)).thenReturn(now);
		when(mockClock.currentTimeMillis()).thenReturn(System.currentTimeMillis());
		when(mockClock.now()).thenReturn(new Date());
		when(mockNotificationEmailDao.getNotificationEmailForPrincipal(USER_ID_LONG)).thenReturn(EMAIL);
		when(mockUserProfileManager.getCurrentVerificationSubmission(USER_ID_LONG)).thenReturn(verificationSubmission);

		boolean includeIdToken = true;
		boolean includeUserInfo = true;

		OIDCAuthorizationRequest authorizationRequest = createAuthorizationRequest(includeIdToken, includeUserInfo);

		OAuthAuthorizationResponse authResponse = openIDConnectManagerImpl.authorizeClient(userInfo, authorizationRequest);
		String code = authResponse.getAccess_code();

		String expectedIdToken = "ID-TOKEN";
		when(oidcTokenHelper.createOIDCIdToken(eq(OAUTH_ENDPOINT), eq(ppid), eq(OAUTH_CLIENT_ID), anyLong(),
				eq(NONCE), eq(now), anyString(), userInfoCaptor.capture())).thenReturn(expectedIdToken);

		OAuthRefreshTokenAndMetadata expectedRefreshTokenAndId = new OAuthRefreshTokenAndMetadata();
		OAuthRefreshTokenInformation expectedMetadata = new OAuthRefreshTokenInformation();
		expectedMetadata.setTokenId("REFRESH-TOKEN-ID");
		expectedRefreshTokenAndId.setRefreshToken("REFRESH-TOKEN");
		expectedRefreshTokenAndId.setMetadata(expectedMetadata);
		when(oauthRefreshTokenManager.createRefreshToken(eq(USER_ID), eq(OAUTH_CLIENT_ID), any(), any())).thenReturn(expectedRefreshTokenAndId);

		String expectedAccessToken = "ACCESS-TOKEN";
		when(oidcTokenHelper.createOIDCaccessToken(eq(OAUTH_ENDPOINT), eq(ppid), eq(OAUTH_CLIENT_ID), anyLong(), anyLong(),
				eq(now), eq(expectedRefreshTokenAndId.getMetadata().getTokenId()), anyString(), scopesCaptor.capture(), claimsCaptor.capture())).thenReturn(expectedAccessToken);

		// elsewhere we test that we correctly build up the requested user-info
		// here we just spot check a few fields to make sure everything's wired up

		// method under test
		OIDCTokenResponse tokenResponse = openIDConnectManagerImpl.generateTokenResponseWithAuthorizationCode(code, OAUTH_CLIENT_ID, REDIRCT_URIS.get(0), OAUTH_ENDPOINT);

		// verifying the mock token indirectly verifies all param's were correctly passed to oidcTokenHelper.createOIDCIdToken()
		assertEquals(expectedIdToken, tokenResponse.getId_token());
		// just spot check a few fields to make sure everything's wired up
		Map<OIDCClaimName, Object> userInfo = userInfoCaptor.getValue();
		assertEquals(EMAIL, userInfo.get(OIDCClaimName.email));
		assertTrue((Boolean)userInfo.get(OIDCClaimName.email_verified));
		assertEquals(USER_ID, userInfo.get(OIDCClaimName.userid));

		assertEquals(expectedAccessToken, tokenResponse.getAccess_token());
		assertEquals(Arrays.asList(OAuthScope.openid, OAuthScope.offline_access), scopesCaptor.getValue());
		for (OIDCClaimName claimName : mockClaimProviders.keySet()) {
			assertTrue(claimsCaptor.getValue().containsKey(claimName));
			assertNull(claimsCaptor.getValue().get(claimName));
		}

		assertEquals(expectedRefreshTokenAndId.getRefreshToken(), tokenResponse.getRefresh_token());
		
		verify(oidcTokenHelper).createOIDCaccessToken(eq(OAUTH_ENDPOINT), eq(ppid), eq(OAUTH_CLIENT_ID), anyLong(), anyLong(),
				eq(now), eq(expectedRefreshTokenAndId.getMetadata().getTokenId()), anyString(), any(), any());

	}

	@Test
	public void testGetTokenResponseWithAuthorizationCode_noRefreshToken() {
		when(mockStackEncrypter.decryptStackEncryptedAndBase64EncodedString(anyString())).then(returnsFirstArg());
		when(mockOauthClientDao.getOAuthClient(OAUTH_CLIENT_ID)).thenReturn(oauthClient);
		when(mockOauthClientDao.isOauthClientVerified(OAUTH_CLIENT_ID)).thenReturn(true);
		when(mockStackEncrypter.encryptAndBase64EncodeStringWithStackKey(anyString())).then(returnsFirstArg());
		when(mockOauthClientDao.getSectorIdentifierSecretForClient(OAUTH_CLIENT_ID)).thenReturn(clientSpecificEncodingSecret);
		when(mockAuthDao.getAuthenticatedOn(USER_ID_LONG)).thenReturn(now);
		when(mockClock.currentTimeMillis()).thenReturn(System.currentTimeMillis());
		when(mockClock.now()).thenReturn(new Date());
		when(mockNotificationEmailDao.getNotificationEmailForPrincipal(USER_ID_LONG)).thenReturn(EMAIL);
		when(mockUserProfileManager.getCurrentVerificationSubmission(USER_ID_LONG)).thenReturn(verificationSubmission);

		boolean includeIdToken = true;
		boolean includeUserInfo = true;

		OIDCAuthorizationRequest authorizationRequest = createAuthorizationRequest(includeIdToken, includeUserInfo);
		// Remove the offline_access scope, which will prevent a refresh token from being issued
		authorizationRequest.setScope(OAuthScope.openid.name());

		OAuthAuthorizationResponse authResponse = openIDConnectManagerImpl.authorizeClient(userInfo, authorizationRequest);
		String code = authResponse.getAccess_code();

		String expectedIdToken = "ID-TOKEN";
		when(oidcTokenHelper.createOIDCIdToken(eq(OAUTH_ENDPOINT), eq(ppid), eq(OAUTH_CLIENT_ID), anyLong(),
				eq(NONCE), eq(now), anyString(), userInfoCaptor.capture())).thenReturn(expectedIdToken);

		String expectedAccessToken = "ACCESS-TOKEN";
		when(oidcTokenHelper.createOIDCaccessToken(eq(OAUTH_ENDPOINT), eq(ppid), eq(OAUTH_CLIENT_ID), anyLong(), anyLong(),
				eq(now), isNull(), anyString(), scopesCaptor.capture(), claimsCaptor.capture())).thenReturn(expectedAccessToken);
		
		// elsewhere we test that we correctly build up the requested user-info
		// here we just spot check a few fields to make sure everything's wired up

		// method under test
		OIDCTokenResponse tokenResponse = openIDConnectManagerImpl.generateTokenResponseWithAuthorizationCode(code, OAUTH_CLIENT_ID, REDIRCT_URIS.get(0), OAUTH_ENDPOINT);
		
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

		assertEquals("Bearer", tokenResponse.getToken_type());
		assertEquals(EXPECTED_ACCESS_TOKEN_EXPIRATION_TIME_SECONDS, tokenResponse.getExpires_in());

		verify(oauthRefreshTokenManager, never()).createRefreshToken(any(), any(), any(), any());
		assertNull(tokenResponse.getRefresh_token());
		
		verify(oidcTokenHelper).createOIDCIdToken(eq(OAUTH_ENDPOINT), eq(ppid), eq(OAUTH_CLIENT_ID), anyLong(),
				eq(NONCE), eq(now), anyString(), any());

		verify(oidcTokenHelper).createOIDCaccessToken(eq(OAUTH_ENDPOINT), eq(ppid), eq(OAUTH_CLIENT_ID), anyLong(), anyLong(),
				eq(now), isNull(), anyString(), any(), any());

	}
	
	@Test
	public void testGetTokenResponseWithAuthorizationCode_invalidAuthCode() {
		when(mockStackEncrypter.decryptStackEncryptedAndBase64EncodedString(anyString())).then(returnsFirstArg());	
		when(mockOauthClientDao.isOauthClientVerified(OAUTH_CLIENT_ID)).thenReturn(true);

		String incorrectlyEncryptedCode = "some invalid code";
		when(mockStackEncrypter.decryptStackEncryptedAndBase64EncodedString(incorrectlyEncryptedCode)).thenThrow(new RuntimeException());

		OAuthBadRequestException ex = assertThrows(OAuthBadRequestException.class, () -> {
			// method under test
			openIDConnectManagerImpl.generateTokenResponseWithAuthorizationCode(incorrectlyEncryptedCode, OAUTH_CLIENT_ID, REDIRCT_URIS.get(0), OAUTH_ENDPOINT);
		});
		assertEquals(OAuthErrorCode.invalid_grant, ex.getError());
		assertEquals("invalid_grant Invalid authorization code: some invalid code", ex.getMessage());

		// this code is not a valid AuthorizationRequest object
		String invalidAuthorizationObjectCode = "not correctly serialized json";

		assertThrows(IllegalStateException.class, () -> {
			// method under test
			openIDConnectManagerImpl.generateTokenResponseWithAuthorizationCode(invalidAuthorizationObjectCode, OAUTH_CLIENT_ID, REDIRCT_URIS.get(0), OAUTH_ENDPOINT);
		});
	}

	@Test
	public void testGetTokenResponseWithAuthorizationCode_expiredAuthCode() throws Exception {
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

		OAuthBadRequestException ex = assertThrows(OAuthBadRequestException.class, () -> {
			// method under test
			openIDConnectManagerImpl.generateTokenResponseWithAuthorizationCode(code, OAUTH_CLIENT_ID, REDIRCT_URIS.get(0), OAUTH_ENDPOINT);
		});
		assertEquals(OAuthErrorCode.invalid_grant, ex.getError());
		assertEquals("invalid_grant Authorization code has expired.", ex.getMessage());
	}

	@Test
	public void testGetTokenResponseWithAuthorizationCode_mismatchRedirectURI() {
		when(mockStackEncrypter.decryptStackEncryptedAndBase64EncodedString(anyString())).then(returnsFirstArg());	
		when(mockOauthClientDao.getOAuthClient(OAUTH_CLIENT_ID)).thenReturn(oauthClient);	
		when(mockOauthClientDao.isOauthClientVerified(OAUTH_CLIENT_ID)).thenReturn(true);
		when(mockStackEncrypter.encryptAndBase64EncodeStringWithStackKey(anyString())).then(returnsFirstArg());	
		when(mockAuthDao.getAuthenticatedOn(USER_ID_LONG)).thenReturn(now);
		when(mockClock.currentTimeMillis()).thenReturn(System.currentTimeMillis());
		when(mockClock.now()).thenReturn(new Date());

		OIDCAuthorizationRequest authorizationRequest = createAuthorizationRequest();

		OAuthAuthorizationResponse authResponse = openIDConnectManagerImpl.authorizeClient(userInfo, authorizationRequest);

		OAuthBadRequestException ex = assertThrows(OAuthBadRequestException.class, () -> {
			// method under test
			openIDConnectManagerImpl.generateTokenResponseWithAuthorizationCode(authResponse.getAccess_code(), OAUTH_CLIENT_ID, "wrong redirect uri", OAUTH_ENDPOINT);
		});
		assertEquals(OAuthErrorCode.invalid_grant, ex.getError());
		assertEquals("invalid_grant URI mismatch: https://client.com/redir vs. wrong redirect uri", ex.getMessage());
	}
	
	@Test
	public void testGetTokenResponseWithAuthorizationCode_noOpenIdScope() {
		when(mockStackEncrypter.decryptStackEncryptedAndBase64EncodedString(anyString())).then(returnsFirstArg());	
		when(mockOauthClientDao.getOAuthClient(OAUTH_CLIENT_ID)).thenReturn(oauthClient);	
		when(mockStackEncrypter.encryptAndBase64EncodeStringWithStackKey(anyString())).then(returnsFirstArg());	
		when(mockOauthClientDao.getSectorIdentifierSecretForClient(OAUTH_CLIENT_ID)).thenReturn(clientSpecificEncodingSecret);
		when(mockOauthClientDao.isOauthClientVerified(OAUTH_CLIENT_ID)).thenReturn(true);
		when(mockAuthDao.getAuthenticatedOn(USER_ID_LONG)).thenReturn(now);
		when(mockClock.currentTimeMillis()).thenReturn(System.currentTimeMillis());
		when(mockClock.now()).thenReturn(new Date());

		OIDCAuthorizationRequest authorizationRequest = createAuthorizationRequest();
		authorizationRequest.setScope(null); // omit the 'openid' scope.  This should suppress the idToken

		OAuthAuthorizationResponse authResponse = openIDConnectManagerImpl.authorizeClient(userInfo, authorizationRequest);
		String code = authResponse.getAccess_code();

		String expectedAccessToken = "ACCESS-TOKEN";
		when(oidcTokenHelper.createOIDCaccessToken(eq(OAUTH_ENDPOINT), anyString(), eq(OAUTH_CLIENT_ID), anyLong(), anyLong(),
				eq(now), isNull(), anyString(), (List<OAuthScope>)any(), (Map<OIDCClaimName, OIDCClaimsRequestDetails>)any())).thenReturn(expectedAccessToken);
		
		// method under test
		OIDCTokenResponse tokenResponse = openIDConnectManagerImpl.generateTokenResponseWithAuthorizationCode(code, OAUTH_CLIENT_ID, REDIRCT_URIS.get(0), OAUTH_ENDPOINT);
		
		verify(oidcTokenHelper).createOIDCaccessToken(eq(OAUTH_ENDPOINT), anyString(), eq(OAUTH_CLIENT_ID), anyLong(), anyLong(),
				eq(now), isNull(), anyString(), (List<OAuthScope>)any(), (Map<OIDCClaimName, OIDCClaimsRequestDetails>)any());

		verify(oidcTokenHelper, never()).
			createOIDCIdToken(anyString(), anyString(), anyString(), anyLong(), anyString(), (Date)any(), anyString(), (Map)any());
		
		assertNull(tokenResponse.getId_token());

		assertNotNull(tokenResponse.getAccess_token());
	}
	
	@Test
	public void testGetTokenResponseWithAuthorizationCode_clientUnverified() {
		String code = "Some code";
		
		when(mockOauthClientDao.isOauthClientVerified(OAUTH_CLIENT_ID)).thenReturn(false);
		
		assertThrows(OAuthClientNotVerifiedException.class, () -> {
			// method under test
			openIDConnectManagerImpl.generateTokenResponseWithAuthorizationCode(code, OAUTH_CLIENT_ID, REDIRCT_URIS.get(0), OAUTH_ENDPOINT);
		});
		
		verify(mockOauthClientDao).isOauthClientVerified(OAUTH_CLIENT_ID);
	}

	private OAuthRefreshTokenAndMetadata createRotatedToken() {
		OAuthRefreshTokenAndMetadata refreshToken = new OAuthRefreshTokenAndMetadata();
		refreshToken.setRefreshToken("new refresh token");
		refreshToken.setMetadata(new OAuthRefreshTokenInformation());
		refreshToken.getMetadata().setTokenId("1234567");
		refreshToken.getMetadata().setClientId(OAUTH_CLIENT_ID);
		refreshToken.getMetadata().setPrincipalId(USER_ID);
		refreshToken.getMetadata().setScopes(Arrays.asList(OAuthScope.openid, OAuthScope.offline_access));
		OIDCClaimsRequest grantedClaims = new OIDCClaimsRequest();
		Map<String, OIDCClaimsRequestDetails> claimsMap = new HashMap<>();
		for (OIDCClaimName claim : mockClaimProviders.keySet()) {
			claimsMap.put(claim.name(), null);
		}
		grantedClaims.setUserinfo(claimsMap);
		grantedClaims.setId_token(claimsMap);
		refreshToken.getMetadata().setClaims(grantedClaims);
		return refreshToken;
	}

	@Test
	public void testGetTokenResponseWithRefreshToken_clientUnverified() {
		String refreshToken = "some-refresh-token";

		when(mockOauthClientDao.isOauthClientVerified(OAUTH_CLIENT_ID)).thenReturn(false);

		assertThrows(OAuthClientNotVerifiedException.class, () -> {
			// method under test
			openIDConnectManagerImpl.generateTokenResponseWithRefreshToken(refreshToken, OAUTH_CLIENT_ID, null, OAUTH_ENDPOINT);
		});

		verify(mockOauthClientDao).isOauthClientVerified(OAUTH_CLIENT_ID);
	}


	@Test
	public void testGetTokenResponseWithRefreshToken() {
		when(mockOauthClientDao.isOauthClientVerified(OAUTH_CLIENT_ID)).thenReturn(true);
		when(mockOauthClientDao.getSectorIdentifierSecretForClient(OAUTH_CLIENT_ID)).thenReturn(clientSpecificEncodingSecret);
		when(mockClock.currentTimeMillis()).thenReturn(System.currentTimeMillis());
		when(mockNotificationEmailDao.getNotificationEmailForPrincipal(USER_ID_LONG)).thenReturn(EMAIL);
		when(mockUserProfileManager.getCurrentVerificationSubmission(USER_ID_LONG)).thenReturn(verificationSubmission);
		Date authenticationTime = new Date(now.getTime()-1000L*3600*24*7); // we logged in a week ago
		when(mockAuthDao.getAuthenticatedOn(USER_ID_LONG)).thenReturn(authenticationTime);

		// This will be the new token and metadata
		OAuthRefreshTokenAndMetadata expectedRefreshTokenAndId = createRotatedToken();
		Date initalAuthzOn = new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 24);
		expectedRefreshTokenAndId.getMetadata().setAuthorizedOn(initalAuthzOn);

		String refreshToken = "pre-generated refresh token";
		when(oauthRefreshTokenManager.rotateRefreshToken(refreshToken)).thenReturn(expectedRefreshTokenAndId);


		String expectedIdToken = "ID-TOKEN";
		when(oidcTokenHelper.createOIDCIdToken(eq(OAUTH_ENDPOINT), eq(ppid), eq(OAUTH_CLIENT_ID), anyLong(),
				isNull(), eq(authenticationTime), anyString(), userInfoCaptor.capture())).thenReturn(expectedIdToken);

		String expectedAccessToken = "ACCESS-TOKEN";
		when(oidcTokenHelper.createOIDCaccessToken(eq(OAUTH_ENDPOINT), eq(ppid), eq(OAUTH_CLIENT_ID), anyLong(), anyLong(),
				eq(authenticationTime), eq(expectedRefreshTokenAndId.getMetadata().getTokenId()), anyString(), scopesCaptor.capture(), claimsCaptor.capture())).thenReturn(expectedAccessToken);

		String scope = "openid offline_access";
		// elsewhere we test that we correctly build up the requested user-info
		// here we just spot check a few fields to make sure everything's wired up

		// method under test
		OIDCTokenResponse tokenResponse = openIDConnectManagerImpl.generateTokenResponseWithRefreshToken(refreshToken, OAUTH_CLIENT_ID, scope, OAUTH_ENDPOINT);

		// verifying the mock token indirectly verifies all param's were correctly passed to oidcTokenHelper.createOIDCIdToken()
		assertEquals(expectedIdToken, tokenResponse.getId_token());

		// just spot check a few fields to make sure everything's wired up
		Map<OIDCClaimName, Object> userInfo = userInfoCaptor.getValue();
		assertEquals(EMAIL, userInfo.get(OIDCClaimName.email));
		assertTrue((Boolean)userInfo.get(OIDCClaimName.email_verified));
		assertEquals(USER_ID, userInfo.get(OIDCClaimName.userid));

		assertEquals(expectedAccessToken, tokenResponse.getAccess_token());
		assertEquals(Arrays.asList(OAuthScope.openid, OAuthScope.offline_access), scopesCaptor.getValue());
		for (OIDCClaimName claimName : mockClaimProviders.keySet()) {
			assertTrue(claimsCaptor.getValue().containsKey(claimName));
			assertNull(claimsCaptor.getValue().get(claimName));
		}

		assertEquals(expectedRefreshTokenAndId.getRefreshToken(), tokenResponse.getRefresh_token());
		assertEquals("Bearer", tokenResponse.getToken_type());
		assertEquals(EXPECTED_ACCESS_TOKEN_EXPIRATION_TIME_SECONDS, tokenResponse.getExpires_in());
		
		verify(oidcTokenHelper).createOIDCIdToken(eq(OAUTH_ENDPOINT), eq(ppid), eq(OAUTH_CLIENT_ID), anyLong(),
				isNull(), eq(authenticationTime), anyString(), any());

		verify(oidcTokenHelper).createOIDCaccessToken(eq(OAUTH_ENDPOINT), eq(ppid), eq(OAUTH_CLIENT_ID), anyLong(), anyLong(),
				eq(authenticationTime), eq(expectedRefreshTokenAndId.getMetadata().getTokenId()), anyString(), any(), any());
	}


	@Test
	public void testGetTokenResponseWithRefreshToken_invalidOrExpiredRefreshToken() {
		when(mockOauthClientDao.isOauthClientVerified(OAUTH_CLIENT_ID)).thenReturn(true);

		// Just check that the IllegalArgumentException passes through
		String refreshToken = "some-refresh-token";
		when(oauthRefreshTokenManager.rotateRefreshToken(refreshToken)).thenThrow(new IllegalArgumentException());
		// Call under test
		assertThrows(IllegalArgumentException.class, () ->
				openIDConnectManagerImpl.generateTokenResponseWithRefreshToken(refreshToken, OAUTH_CLIENT_ID, "offline_access", OAUTH_ENDPOINT));
	}

	@Test
	public void testGetTokenResponseWithRefreshToken_exceedingScope() {
		when(mockOauthClientDao.isOauthClientVerified(OAUTH_CLIENT_ID)).thenReturn(true);

		// This will be the new token and metadata
		OAuthRefreshTokenAndMetadata expectedRefreshTokenAndId = createRotatedToken();

		String refreshToken = "pre-generated refresh token";
		when(oauthRefreshTokenManager.rotateRefreshToken(refreshToken)).thenReturn(expectedRefreshTokenAndId);

		String scope = "openid offline_access authorize"; // Authorize was not previously granted

		// method under test
		assertThrows(IllegalArgumentException.class, () -> openIDConnectManagerImpl.generateTokenResponseWithRefreshToken(refreshToken, OAUTH_CLIENT_ID, scope, OAUTH_ENDPOINT));
	}

	@Test
	public void testGetTokenResponseWithRefreshToken_noOpenIdScope() {
		when(mockOauthClientDao.isOauthClientVerified(OAUTH_CLIENT_ID)).thenReturn(true);
		when(mockOauthClientDao.getSectorIdentifierSecretForClient(OAUTH_CLIENT_ID)).thenReturn(clientSpecificEncodingSecret);
		when(mockClock.currentTimeMillis()).thenReturn(System.currentTimeMillis());
		Date authenticationTime = new Date(now.getTime()-1000L*3600*24*7); // we logged in a week ago
		when(mockAuthDao.getAuthenticatedOn(USER_ID_LONG)).thenReturn(authenticationTime);

		// This will be the new token and metadata
		OAuthRefreshTokenAndMetadata expectedRefreshTokenAndId = createRotatedToken();
		Date initalAuthzOn = new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 24);
		expectedRefreshTokenAndId.getMetadata().setAuthorizedOn(initalAuthzOn);


		String refreshToken = "pre-generated refresh token";
		when(oauthRefreshTokenManager.rotateRefreshToken(refreshToken)).thenReturn(expectedRefreshTokenAndId);

		String expectedAccessToken = "ACCESS-TOKEN";
		when(oidcTokenHelper.createOIDCaccessToken(eq(OAUTH_ENDPOINT), eq(ppid), eq(OAUTH_CLIENT_ID), anyLong(), anyLong(),
				eq(authenticationTime), eq(expectedRefreshTokenAndId.getMetadata().getTokenId()), 
				anyString(), scopesCaptor.capture(), claimsCaptor.capture())).thenReturn(expectedAccessToken);

		String scope = "offline_access"; // Do not request openid!
		// elsewhere we test that we correctly build up the requested user-info
		// here we just spot check a few fields to make sure everything's wired up

		// method under test
		OIDCTokenResponse tokenResponse = openIDConnectManagerImpl.generateTokenResponseWithRefreshToken(refreshToken, OAUTH_CLIENT_ID, scope, OAUTH_ENDPOINT);

		// verifying the mock token indirectly verifies all param's were correctly passed to oidcTokenHelper.createOIDCIdToken()
		assertNull(tokenResponse.getId_token());

		assertEquals(expectedAccessToken, tokenResponse.getAccess_token());
		assertEquals(Collections.singletonList(OAuthScope.offline_access), scopesCaptor.getValue());
		assertEquals(expectedRefreshTokenAndId.getRefreshToken(), tokenResponse.getRefresh_token());
		
		verify(oidcTokenHelper).createOIDCaccessToken(eq(OAUTH_ENDPOINT), eq(ppid), eq(OAUTH_CLIENT_ID), anyLong(), anyLong(),
				eq(authenticationTime), eq(expectedRefreshTokenAndId.getMetadata().getTokenId()), 
				anyString(), any(), any());
	}

	@Test
	public void testGetTokenResponseWithRefreshToken_nullOrEmptyScope() {
		when(mockOauthClientDao.isOauthClientVerified(OAUTH_CLIENT_ID)).thenReturn(true);
		when(mockOauthClientDao.getSectorIdentifierSecretForClient(OAUTH_CLIENT_ID)).thenReturn(clientSpecificEncodingSecret);
		when(mockClock.currentTimeMillis()).thenReturn(System.currentTimeMillis());
		when(mockNotificationEmailDao.getNotificationEmailForPrincipal(USER_ID_LONG)).thenReturn(EMAIL);
		when(mockUserProfileManager.getCurrentVerificationSubmission(USER_ID_LONG)).thenReturn(verificationSubmission);
		Date authenticationTime = new Date(now.getTime()-1000L*3600*24*7); // we logged in a week ago
		when(mockAuthDao.getAuthenticatedOn(USER_ID_LONG)).thenReturn(authenticationTime);

		// This will be the new token and metadata
		OAuthRefreshTokenAndMetadata expectedRefreshTokenAndId = createRotatedToken();
		Date initalAuthzOn = new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 24);
		expectedRefreshTokenAndId.getMetadata().setAuthorizedOn(initalAuthzOn);


		String refreshToken = "pre-generated refresh token";
		when(oauthRefreshTokenManager.rotateRefreshToken(refreshToken)).thenReturn(expectedRefreshTokenAndId);


		String expectedIdToken = "ID-TOKEN";
		when(oidcTokenHelper.createOIDCIdToken(eq(OAUTH_ENDPOINT), eq(ppid), eq(OAUTH_CLIENT_ID), anyLong(),
				isNull(), eq(authenticationTime), anyString(), userInfoCaptor.capture())).thenReturn(expectedIdToken);

		String expectedAccessToken = "ACCESS-TOKEN";
		when(oidcTokenHelper.createOIDCaccessToken(eq(OAUTH_ENDPOINT), eq(ppid), eq(OAUTH_CLIENT_ID), anyLong(), anyLong(),
				eq(authenticationTime), eq(expectedRefreshTokenAndId.getMetadata().getTokenId()), anyString(), scopesCaptor.capture(), claimsCaptor.capture())).thenReturn(expectedAccessToken);

		String scope = null; // Null scope = all previously granted scopes
		// elsewhere we test that we correctly build up the requested user-info
		// here we just spot check a few fields to make sure everything's wired up

		// method under test
		OIDCTokenResponse tokenResponse = openIDConnectManagerImpl.generateTokenResponseWithRefreshToken(refreshToken, OAUTH_CLIENT_ID, scope, OAUTH_ENDPOINT);

		// verifying the mock token indirectly verifies all param's were correctly passed to oidcTokenHelper.createOIDCIdToken()
		assertEquals(expectedIdToken, tokenResponse.getId_token());

		// just spot check a few fields to make sure everything's wired up
		Map<OIDCClaimName, Object> userInfo = userInfoCaptor.getValue();
		assertEquals(EMAIL, userInfo.get(OIDCClaimName.email));
		assertTrue((Boolean)userInfo.get(OIDCClaimName.email_verified));
		assertEquals(USER_ID, userInfo.get(OIDCClaimName.userid));

		assertEquals(expectedAccessToken, tokenResponse.getAccess_token());
		assertEquals(Arrays.asList(OAuthScope.openid, OAuthScope.offline_access), scopesCaptor.getValue());
		for (OIDCClaimName claimName : mockClaimProviders.keySet()) {
			assertTrue(claimsCaptor.getValue().containsKey(claimName));
			assertNull(claimsCaptor.getValue().get(claimName));
		}

		assertEquals(expectedRefreshTokenAndId.getRefreshToken(), tokenResponse.getRefresh_token());
		
		verify(oidcTokenHelper).createOIDCIdToken(eq(OAUTH_ENDPOINT), eq(ppid), eq(OAUTH_CLIENT_ID), anyLong(),
				isNull(), eq(authenticationTime), anyString(), any());
		
		verify(oidcTokenHelper).createOIDCaccessToken(eq(OAUTH_ENDPOINT), eq(ppid), eq(OAUTH_CLIENT_ID), anyLong(), anyLong(),
				eq(authenticationTime), eq(expectedRefreshTokenAndId.getMetadata().getTokenId()), anyString(), any(), any());
	}
	
	private static final String ACCESS_TOKEN = "access token";

	private void mockAccessToken(String oAuthClientId) {
		when(oidcTokenHelper.parseJWT(ACCESS_TOKEN)).thenReturn(mockJWT);
		Claims claims = ClaimsWithAuthTime.newClaims();
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
		when(mockAuthDao.getAuthenticatedOn(USER_ID_LONG)).thenReturn(now);
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
	public void testGetUserInfoNoAccessToken() {
		// method under test
		assertThrows(IllegalArgumentException.class, () -> openIDConnectManagerImpl.getUserInfo(null, OAUTH_ENDPOINT));
	}

	@Test
	public void testValidateOidcAccessToken() {
		String refreshTokenId = "12345";
		String token = "access token";
		when(oidcTokenHelper.parseJWT(token)).thenReturn(mockJWT);
		Claims claims = ClaimsWithAuthTime.newClaims();
		claims.put(OIDCClaimName.token_type.name(), TokenType.OIDC_ACCESS_TOKEN.name());
		claims.put(OIDCClaimName.refresh_token_id.name(), refreshTokenId);
		ClaimsJsonUtil.addAccessClaims(Collections.emptyList(), Collections.emptyMap(), claims);
		when(mockJWT.getBody()).thenReturn(claims);
		when(oauthRefreshTokenManager.isRefreshTokenActive(refreshTokenId)).thenReturn(true);
		claims.setAudience(OAUTH_CLIENT_ID);
		when(mockOauthClientDao.getSectorIdentifierSecretForClient(OAUTH_CLIENT_ID)).thenReturn(clientSpecificEncodingSecret);
		when(mockOauthClientDao.isOauthClientVerified(OAUTH_CLIENT_ID)).thenReturn(true);
		String ppid = openIDConnectManagerImpl.ppid(USER_ID, OAUTH_CLIENT_ID);
		claims.setSubject(ppid);

		// method under test
		assertEquals(USER_ID, openIDConnectManagerImpl.validateAccessToken(token));

		verify(oidcTokenHelper).parseJWT(token);
	}

	@Test
	public void testValidateAccessToken_noRefreshTokenId() {
		String token = "access token";
		when(oidcTokenHelper.parseJWT(token)).thenReturn(mockJWT);
		Claims claims = ClaimsWithAuthTime.newClaims();
		claims.put(OIDCClaimName.token_type.name(), TokenType.OIDC_ACCESS_TOKEN.name());
		ClaimsJsonUtil.addAccessClaims(Collections.emptyList(), Collections.emptyMap(), claims);
		when(mockJWT.getBody()).thenReturn(claims);
		claims.setAudience(OAUTH_CLIENT_ID);
		when(mockOauthClientDao.getSectorIdentifierSecretForClient(OAUTH_CLIENT_ID)).thenReturn(clientSpecificEncodingSecret);
		when(mockOauthClientDao.isOauthClientVerified(OAUTH_CLIENT_ID)).thenReturn(true);
		String ppid = openIDConnectManagerImpl.ppid(USER_ID, OAUTH_CLIENT_ID);
		claims.setSubject(ppid);

		// method under test
		assertEquals(USER_ID, openIDConnectManagerImpl.validateAccessToken(token));
		
		verify(oidcTokenHelper).parseJWT(token);
	}

	@Test
	public void testValidateAccessToken_expiredRefreshToken() {
		String refreshTokenId = "12345";
		String token = "access token";
		when(oidcTokenHelper.parseJWT(token)).thenReturn(mockJWT);
		Claims claims = ClaimsWithAuthTime.newClaims();
		claims.put(OIDCClaimName.token_type.name(), TokenType.OIDC_ACCESS_TOKEN.name());
		claims.put(OIDCClaimName.refresh_token_id.name(), refreshTokenId);
		ClaimsJsonUtil.addAccessClaims(Collections.emptyList(), Collections.emptyMap(), claims);
		when(mockJWT.getBody()).thenReturn(claims);
		when(oauthRefreshTokenManager.isRefreshTokenActive(refreshTokenId)).thenReturn(false); // inactive RT!
		claims.setAudience(OAUTH_CLIENT_ID);
		when(mockOauthClientDao.getSectorIdentifierSecretForClient(OAUTH_CLIENT_ID)).thenReturn(clientSpecificEncodingSecret);
		when(mockOauthClientDao.isOauthClientVerified(OAUTH_CLIENT_ID)).thenReturn(true);
		String ppid = openIDConnectManagerImpl.ppid(USER_ID, OAUTH_CLIENT_ID);
		claims.setSubject(ppid);

		// method under test
		assertThrows(OAuthUnauthenticatedException.class, () -> openIDConnectManagerImpl.validateAccessToken(token));

		verify(oidcTokenHelper).parseJWT(token);
	}

	@Test
	public void testValidateAccessToken_idToken() {
		String token = "id token";
		when(oidcTokenHelper.parseJWT(token)).thenReturn(mockJWT);
		Claims claims = Jwts.claims();
		claims.put(OIDCClaimName.token_type.name(), TokenType.OIDC_ID_TOKEN.name());
		when(mockJWT.getBody()).thenReturn(claims);
		claims.setAudience(OAUTH_CLIENT_ID);
		when(mockOauthClientDao.getSectorIdentifierSecretForClient(OAUTH_CLIENT_ID)).thenReturn(clientSpecificEncodingSecret);
		when(mockOauthClientDao.isOauthClientVerified(OAUTH_CLIENT_ID)).thenReturn(true);
		String ppid = openIDConnectManagerImpl.ppid(USER_ID, OAUTH_CLIENT_ID);
		claims.setSubject(ppid);

		// method under test
		assertThrows(OAuthUnauthenticatedException.class, () -> openIDConnectManagerImpl.validateAccessToken(token));

		verify(oidcTokenHelper).parseJWT(token);
	}

	@Test
	public void testValidateAccessToken_personalAccessToken_active() {
		String token = "personal access token";
		String tokenId = "9999";
		when(oidcTokenHelper.parseJWT(token)).thenReturn(mockJWT);
		Claims claims = ClaimsWithAuthTime.newClaims();
		claims.setId(tokenId);
		claims.put(OIDCClaimName.token_type.name(), TokenType.PERSONAL_ACCESS_TOKEN.name());
		ClaimsJsonUtil.addAccessClaims(Collections.emptyList(), Collections.emptyMap(), claims);
		when(mockJWT.getBody()).thenReturn(claims);
		claims.setAudience(OAUTH_CLIENT_ID);
		when(mockPersonalAccessTokenManager.isTokenActive(tokenId)).thenReturn(true);
		when(mockOauthClientDao.getSectorIdentifierSecretForClient(OAUTH_CLIENT_ID)).thenReturn(clientSpecificEncodingSecret);
		when(mockOauthClientDao.isOauthClientVerified(OAUTH_CLIENT_ID)).thenReturn(true);
		String ppid = openIDConnectManagerImpl.ppid(USER_ID, OAUTH_CLIENT_ID);
		claims.setSubject(ppid);

		// method under test
		assertEquals(USER_ID, openIDConnectManagerImpl.validateAccessToken(token));

		verify(oidcTokenHelper).parseJWT(token);
	}

	@Test
	public void testValidateAccessToken_personalAccessToken_inactive() {
		String token = "personal access token";
		String tokenId = "9999";
		when(oidcTokenHelper.parseJWT(token)).thenReturn(mockJWT);
		Claims claims = ClaimsWithAuthTime.newClaims();
		claims.setId(tokenId);
		claims.put(OIDCClaimName.token_type.name(), TokenType.PERSONAL_ACCESS_TOKEN.name());
		ClaimsJsonUtil.addAccessClaims(Collections.emptyList(), Collections.emptyMap(), claims);
		when(mockJWT.getBody()).thenReturn(claims);
		claims.setAudience(OAUTH_CLIENT_ID);
		when(mockPersonalAccessTokenManager.isTokenActive(tokenId)).thenReturn(false);
		when(mockOauthClientDao.getSectorIdentifierSecretForClient(OAUTH_CLIENT_ID)).thenReturn(clientSpecificEncodingSecret);
		when(mockOauthClientDao.isOauthClientVerified(OAUTH_CLIENT_ID)).thenReturn(true);
		String ppid = openIDConnectManagerImpl.ppid(USER_ID, OAUTH_CLIENT_ID);
		claims.setSubject(ppid);

		// method under test
		assertThrows(ForbiddenException.class, () -> openIDConnectManagerImpl.validateAccessToken(token));

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


	@Test
	public void testRevokeRefreshTokenWithAccessToken() {
		String accessToken = "this would be a signed JWT string";
		OAuthTokenRevocationRequest revocationRequest = new OAuthTokenRevocationRequest();
		revocationRequest.setToken(accessToken);
		revocationRequest.setToken_type_hint(TokenTypeHint.access_token);

		UserInfo adminUserInfo = new UserInfo(true);
		when(oidcTokenHelper.parseJWT(accessToken)).thenReturn(mockJWT);
		when(mockJWT.getBody()).thenReturn(mockClaims);
		String tokenId = "98765";
		when(mockClaims.get(OIDCClaimName.refresh_token_id.name(), String.class)).thenReturn(tokenId);

		// Call under test
		openIDConnectManagerImpl.revokeToken(oauthClient.getClient_id(), revocationRequest);

		verify(oidcTokenHelper).parseJWT(accessToken);
		verify(oauthRefreshTokenManager).revokeRefreshToken(oauthClient.getClient_id(), tokenId);
	}

	@Test
	public void testRevokeRefreshTokenWithAccessToken_NoRefreshTokenId() {
		String accessToken = "this would be a signed JWT string";
		OAuthTokenRevocationRequest revocationRequest = new OAuthTokenRevocationRequest();
		revocationRequest.setToken(accessToken);
		revocationRequest.setToken_type_hint(TokenTypeHint.access_token);

		when(oidcTokenHelper.parseJWT(accessToken)).thenReturn(mockJWT);
		when(mockJWT.getBody()).thenReturn(mockClaims);
		when(mockClaims.get(OIDCClaimName.refresh_token_id.name(), String.class)).thenReturn(null);

		// Call under test
		assertThrows(IllegalArgumentException.class, () -> openIDConnectManagerImpl.revokeToken(oauthClient.getClient_id(), revocationRequest));

		verify(oidcTokenHelper).parseJWT(accessToken);
		verify(oauthRefreshTokenManager, never()).revokeRefreshToken(anyString(), anyString());
	}

	@Test
	public void testRevokeRefreshTokenWithRefreshToken() {
		String refreshToken = "some-refresh-token";
		String tokenId = "1234567";
		OAuthRefreshTokenInformation retrievedMetadata = new OAuthRefreshTokenInformation();
		retrievedMetadata.setTokenId(tokenId);
		OAuthTokenRevocationRequest revocationRequest = new OAuthTokenRevocationRequest();
		revocationRequest.setToken(refreshToken);
		revocationRequest.setToken_type_hint(TokenTypeHint.refresh_token);

		when(oauthRefreshTokenManager.getRefreshTokenMetadataWithToken(oauthClient.getClient_id(), refreshToken)).thenReturn(retrievedMetadata);

		// Call under test
		openIDConnectManagerImpl.revokeToken(oauthClient.getClient_id(), revocationRequest);

		verify(oauthRefreshTokenManager).revokeRefreshToken(oauthClient.getClient_id(), tokenId);
	}

	@Test
	public void testRevokeRefreshTokenWithUnknownHint() {
		OAuthTokenRevocationRequest revocationRequest = new OAuthTokenRevocationRequest();
		revocationRequest.setToken("token");

		// This test will fail if a new token type hint is added without a branch in the method under test.

		for (TokenTypeHint hint : TokenTypeHint.values()) {
			revocationRequest.setToken_type_hint(hint);
			try {
				openIDConnectManagerImpl.revokeToken(oauthClient.getClient_id(), revocationRequest);
			} catch (IllegalArgumentException e) {
				if (e.getMessage().contains("Unable to revoke a token with token_type_hint=")) {
					fail();
				}
				// Otherwise let pass
			} catch (Exception e) {
				// Let pass
			}
		}
	}

	@Test
	public void testNormalizeClaims() {
		OIDCClaimsRequest claims = new OIDCClaimsRequest();
		Map<String, OIDCClaimsRequestDetails> idToken = new HashMap<>();
		idToken.put(OIDCClaimName.team.name(), new OIDCClaimsRequestDetails());
		idToken.put(OIDCClaimName.email.name(), null);
		idToken.put("some_nonsense_claim", new OIDCClaimsRequestDetails());
		claims.setId_token(idToken);
		Map<String, OIDCClaimsRequestDetails> userInfo = new HashMap<>();
		userInfo.put(OIDCClaimName.team.name(), new OIDCClaimsRequestDetails());
		userInfo.put(OIDCClaimName.userid.name(), null);
		userInfo.put("another_nonsense_claim", new OIDCClaimsRequestDetails());
		claims.setUserinfo(userInfo);

		// Expected: same but without the invalid claims
		OIDCClaimsRequest expectedClaims = new OIDCClaimsRequest();
		Map<String, OIDCClaimsRequestDetails> expectedIdToken = new HashMap<>();
		expectedIdToken.put(OIDCClaimName.team.name(), new OIDCClaimsRequestDetails());
		expectedIdToken.put(OIDCClaimName.email.name(), null);
		expectedClaims.setId_token(expectedIdToken);
		Map<String, OIDCClaimsRequestDetails> expectedUserInfo = new HashMap<>();
		expectedUserInfo.put(OIDCClaimName.team.name(), new OIDCClaimsRequestDetails());
		expectedUserInfo.put(OIDCClaimName.userid.name(), null);
		expectedClaims.setUserinfo(expectedUserInfo);

		// Call under test
		assertEquals(expectedClaims, OpenIDConnectManagerImpl.normalizeClaims(claims));
	}
	@Test
	public void testNormalizeClaims_null() {
		OIDCClaimsRequest emptyClaimsRequest = new OIDCClaimsRequest();
		emptyClaimsRequest.setUserinfo(Collections.emptyMap());
		emptyClaimsRequest.setId_token(Collections.emptyMap());
		assertEquals(emptyClaimsRequest, OpenIDConnectManagerImpl.normalizeClaims(null));
	}

	@Test
	public void testNormalizeClaims_nullFields() {
		OIDCClaimsRequest claimsRequestWithNullFields = new OIDCClaimsRequest();
		claimsRequestWithNullFields.setId_token(null);
		claimsRequestWithNullFields.setUserinfo(null);

		OIDCClaimsRequest emptyClaimsRequest = new OIDCClaimsRequest();
		emptyClaimsRequest.setUserinfo(Collections.emptyMap());
		emptyClaimsRequest.setId_token(Collections.emptyMap());
		assertEquals(emptyClaimsRequest, OpenIDConnectManagerImpl.normalizeClaims(claimsRequestWithNullFields));
	}
}
