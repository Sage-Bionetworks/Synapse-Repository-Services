package org.sagebionetworks.repo.manager.oauth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.TeamDAO;
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
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.securitytools.EncryptionUtils;

import com.google.common.collect.ImmutableList;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.impl.DefaultJws;
import io.jsonwebtoken.impl.DefaultJwsHeader;

@RunWith(MockitoJUnitRunner.class)
public class OpenIDConnectManagerImplUnitTest {
	private static final String USER_ID = "101";
	private static final Long USER_ID_LONG = Long.parseLong(USER_ID);
	private static final List<String> REDIRCT_URIS = Collections.singletonList("https://client.com/redir");
	private static final String OAUTH_CLIENT_ID = "123";
	private static final String OAUTH_ENDPOINT = "https://repo-prod.prod.sagebase.org/auth/v1";

	@Mock
	private StackEncrypter mockStackEncrypter;

	@Mock
	private OAuthClientDao mockOauthClientDao;

	@Mock
	private AuthenticationDAO mockAuthDao;

	@Mock
	private UserProfileManager mockUserProfileManager;

	@Mock
	private OIDCTokenHelper oidcTokenHelper;
	
	@Mock
	private TeamDAO mockTeamDAO;

	@InjectMocks
	private OpenIDConnectManagerImpl openIDConnectManagerImpl;
	
	@Mock
	private StackConfiguration mockStackConfigurations;
	
	@Captor
	private ArgumentCaptor<Map<OIDCClaimName, String>> userInfoCaptor;
	
	@Captor
	private ArgumentCaptor<List<OAuthScope>> scopesCaptor;
	
	@Captor
	private ArgumentCaptor<Map<OIDCClaimName, OIDCClaimsRequestDetails>> claimsCaptor;
	
	private UserInfo userInfo;
	private UserInfo anonymousUserInfo;
	private Date now;
	private String clientSpecificEncodingSecret;
	private OAuthClient oauthClient;
	
	@Before
	public void setUp() throws Exception {
		userInfo = new UserInfo(false);
		userInfo.setId(USER_ID_LONG);

		anonymousUserInfo = new UserInfo(false);
		anonymousUserInfo.setId(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
		
		oauthClient = new OAuthClient();
		oauthClient.setClientId(OAUTH_CLIENT_ID);
		oauthClient.setRedirect_uris(REDIRCT_URIS);

		when(mockOauthClientDao.getOAuthClient(OAUTH_CLIENT_ID)).thenReturn(oauthClient);	
		when(mockOauthClientDao.doesSectorIdentifierExistForURI(anyString())).thenReturn(true);
		when(mockOauthClientDao.getOAuthClientCreator(OAUTH_CLIENT_ID)).thenReturn(USER_ID);
		
		when(mockStackEncrypter.encryptAndBase64EncodeStringWithStackKey(anyString())).then(returnsFirstArg());	
		when(mockStackEncrypter.decryptStackEncryptedAndBase64EncodedString(anyString())).then(returnsFirstArg());	
		
		clientSpecificEncodingSecret = EncryptionUtils.newSecretKey();
		when(mockOauthClientDao.getSectorIdentifierSecretForClient(OAUTH_CLIENT_ID)).thenReturn(clientSpecificEncodingSecret);
		
		now = new Date();
		when(mockAuthDao.getSessionValidatedOn(USER_ID_LONG)).thenReturn(now);
		
		
		UserProfile userProfile = new UserProfile();
		when(mockUserProfileManager.getUserProfile(USER_ID)).thenReturn(userProfile);
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
	private static OIDCAuthorizationRequest createAuthorizationRequest(String claimsTag) {
		OIDCAuthorizationRequest authorizationRequest = new OIDCAuthorizationRequest();
		authorizationRequest.setClientId(OAUTH_CLIENT_ID);
		authorizationRequest.setScope(OAuthScope.openid.name());
		StringBuilder claims = new StringBuilder("{\""+claimsTag+"\":{");
		boolean firstTime = true;
		for (OIDCClaimName claimName : OpenIDConnectManagerImpl.CLAIM_DESCRIPTION.keySet()) {
			if (firstTime) {firstTime=false;} else {claims.append(",");}
			claims.append("\""+claimName.name()+"\":\"null\"");
		}
		claims.append("}}");
		authorizationRequest.setClaims(claims.toString());
		authorizationRequest.setResponseType(OAuthResponseType.code);
		authorizationRequest.setRedirectUri(REDIRCT_URIS.get(0));
		authorizationRequest.setNonce(NONCE);
		return authorizationRequest;
	}
	
	@Test
	public void testGetAuthenticationRequestDescription_HappyCase() {
		OIDCAuthorizationRequest authorizationRequest = createAuthorizationRequest("id_token");
		OIDCAuthorizationRequestDescription description = 
				// method under test
				openIDConnectManagerImpl.getAuthenticationRequestDescription(authorizationRequest);
		assertEquals(OAUTH_CLIENT_ID, description.getClientId());
		assertEquals(REDIRCT_URIS.get(0), description.getRedirect_uri());
		Set<String> expectedScope = new HashSet<String>(OpenIDConnectManagerImpl.CLAIM_DESCRIPTION.values());
		// Note, we compare sets, not lists, since we don't enforce order
		assertEquals(expectedScope, new HashSet<String>(description.getScope()));
	}

	@Test
	public void testGetAuthenticationRequestDescription_HappyCase_UserInfoToken() {
		OIDCAuthorizationRequest authorizationRequest = createAuthorizationRequest("userinfo");
		OIDCAuthorizationRequestDescription description = 
				// method under test
				openIDConnectManagerImpl.getAuthenticationRequestDescription(authorizationRequest);
		assertEquals(OAUTH_CLIENT_ID, description.getClientId());
		assertEquals(REDIRCT_URIS.get(0), description.getRedirect_uri());
		Set<String> expectedScope = new HashSet<String>(OpenIDConnectManagerImpl.CLAIM_DESCRIPTION.values());
		// Note, we compare sets, not lists, since we don't enforce order
		assertEquals(expectedScope, new HashSet<String>(description.getScope()));
	}

	@Test
	public void testGetAuthenticationRequestDescription_NoOAuthScope() {
		OIDCAuthorizationRequest authorizationRequest = createAuthorizationRequest("userinfo");
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
		OIDCAuthorizationRequest authorizationRequest = createAuthorizationRequest("id_token");
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
		OIDCAuthorizationRequest authorizationRequest = createAuthorizationRequest("id_token");
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
		OIDCAuthorizationRequest authorizationRequest = createAuthorizationRequest("id_token");

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
		OIDCAuthorizationRequest authorizationRequest = createAuthorizationRequest("id_token");

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
		OIDCAuthorizationRequest authorizationRequest = createAuthorizationRequest("id_token");
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
	public void testGetUserIdFromPPID() {
		String ppid = openIDConnectManagerImpl.ppid(USER_ID, OAUTH_CLIENT_ID);
		
		// method under test		
		assertEquals(USER_ID, openIDConnectManagerImpl.getUserIdFromPPID(ppid, OAUTH_CLIENT_ID));
	}

	@Test
	public void testGetUserInfo_internal() {
		// TODO
	}

	@Test
	public void testGetSerializedJSON() {
		String serJson = OpenIDConnectManagerImpl.asSerializedJSON(ImmutableList.of("a", "b", "c"));
		assertEquals("[\"a\",\"b\",\"c\"]", serJson);
	}

	@Test
	public void testGetAccessToken() {
		OIDCAuthorizationRequest authorizationRequest = createAuthorizationRequest("id_token");

		OAuthAuthorizationResponse authResponse = openIDConnectManagerImpl.authorizeClient(userInfo, authorizationRequest);
		String code = authResponse.getAccess_code();

		String ppid = EncryptionUtils.encrypt(USER_ID, clientSpecificEncodingSecret);
		String expectedIdToken = "ID-TOKEN";
		when(oidcTokenHelper.createOIDCIdToken(eq(OAUTH_ENDPOINT), eq(ppid), eq(OAUTH_CLIENT_ID), anyLong(), 
				eq(NONCE), eq(now.getTime()/1000L), anyString(), userInfoCaptor.capture())).thenReturn(expectedIdToken);
		
		String expectedAccessToken = "ACCESS-TOKEN";
		when(oidcTokenHelper.createOIDCaccessToken(eq(OAUTH_ENDPOINT), eq(ppid), eq(OAUTH_CLIENT_ID), anyLong(),
				eq(now.getTime()/1000L), anyString(), scopesCaptor.capture(), claimsCaptor.capture())).thenReturn(expectedAccessToken);
		
		// method under test
		OIDCTokenResponse tokenResponse = openIDConnectManagerImpl.getAccessToken(code, OAUTH_CLIENT_ID, REDIRCT_URIS.get(0), OAUTH_ENDPOINT);
		
		// verifying the mock token indirectly verifies all param's were correctly passed to oidcTokenHelper.createOIDCIdToken()
		assertEquals(expectedIdToken, tokenResponse.getId_token());
		System.out.println(userInfoCaptor.getValue());// TODO 

		assertEquals(expectedAccessToken, tokenResponse.getAccess_token());
		assertEquals(Collections.singletonList(OAuthScope.openid), scopesCaptor.getValue());
		System.out.println(claimsCaptor.getValue());// TODO 
	
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
		}  catch (IllegalArgumentException e) {
			// as expected
		}
	}

	@Test
	public void testGetAccessToken_expiredAuthCode() throws Exception {
		OIDCAuthorizationRequest authorizationRequest = createAuthorizationRequest("id_token");

		OAuthAuthorizationResponse authResponse = openIDConnectManagerImpl.authorizeClient(userInfo, authorizationRequest);
		
		// now let's doctor the authorization time stamp:
		String code = authResponse.getAccess_code();
		String decrypted = mockStackEncrypter.decryptStackEncryptedAndBase64EncodedString(code);
		
		OIDCAuthorizationRequest authRequestFromCode = new OIDCAuthorizationRequest();
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(decrypted);
		authRequestFromCode.initializeFromJSONObject(adapter);
		// authorized 100 sec ago (expiration is 60 sec)
		authRequestFromCode.setAuthorizedAt(new Date(authRequestFromCode.getAuthorizedAt().getTime()-100000L));
		// now turn back into an authorization code
		try {
			authRequestFromCode.writeToJSONObject(adapter);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
		String serializedAuthorizationRequest = adapter.toJSONString();
		String modifiedCode = mockStackEncrypter.encryptAndBase64EncodeStringWithStackKey(serializedAuthorizationRequest);

		// method under test
		try {
			openIDConnectManagerImpl.getAccessToken(modifiedCode, OAUTH_CLIENT_ID, REDIRCT_URIS.get(0), OAUTH_ENDPOINT);
			fail("IllegalArgumentException expected");
		}  catch (IllegalArgumentException e) {
			// as expected
		}

	}

	@Test
	public void testGetAccessToken_mismatchRedirectURI() {
		OIDCAuthorizationRequest authorizationRequest = createAuthorizationRequest("id_token");

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
		OIDCAuthorizationRequest authorizationRequest = createAuthorizationRequest("id_token");
		authorizationRequest.setScope(null); // omit the 'openid' scope.  This should suppress the idToken

		OAuthAuthorizationResponse authResponse = openIDConnectManagerImpl.authorizeClient(userInfo, authorizationRequest);
		String code = authResponse.getAccess_code();

		String expectedIdToken = "ID-TOKEN";
		when(oidcTokenHelper.createOIDCIdToken(eq(OAUTH_ENDPOINT), anyString(), eq(OAUTH_CLIENT_ID), anyLong(), 
				eq(NONCE), eq(now.getTime()/1000L), anyString(), userInfoCaptor.capture())).thenReturn(expectedIdToken);
		
		String expectedAccessToken = "ACCESS-TOKEN";
		when(oidcTokenHelper.createOIDCaccessToken(eq(OAUTH_ENDPOINT), anyString(), eq(OAUTH_CLIENT_ID), anyLong(),
				eq(now.getTime()/1000L), anyString(), scopesCaptor.capture(), claimsCaptor.capture())).thenReturn(expectedAccessToken);
		
		// method under test
		OIDCTokenResponse tokenResponse = openIDConnectManagerImpl.getAccessToken(code, OAUTH_CLIENT_ID, REDIRCT_URIS.get(0), OAUTH_ENDPOINT);
		
		assertNull(tokenResponse.getId_token());

		assertNotNull(tokenResponse.getAccess_token());
	}
	
	@Test
	public void testGetUserInfoAsJWT() {
		// if the client sets a signing algorithm it means it wants the UserInfo json
		// to be encoded as a JWT and signed
		oauthClient.setUserinfo_signed_response_alg(OIDCSigningAlgorithm.RS256);
		
		
		Claims claims = Jwts.claims();
		Jwt<JwsHeader,Claims> accessToken = new DefaultJws<Claims>(new DefaultJwsHeader(), claims, "signature");
		claims.setAudience(OAUTH_CLIENT_ID);
		claims.setSubject(openIDConnectManagerImpl.ppid(USER_ID, OAUTH_CLIENT_ID));
		claims.put(OIDCClaimName.auth_time.name(), now.getTime()/1000L);
		// TODO put 'access' in claims
		
		String expectedIdToken = "ID-TOKEN";
		when(oidcTokenHelper.createOIDCIdToken(eq(OAUTH_ENDPOINT), anyString(), eq(OAUTH_CLIENT_ID), anyLong(), 
				eq(null), eq(now.getTime()/1000L), anyString(), userInfoCaptor.capture())).thenReturn(expectedIdToken);

		// method under test
		String jwt = (String)openIDConnectManagerImpl.getUserInfo(accessToken, OAUTH_ENDPOINT);
		
		assertEquals(expectedIdToken, jwt);
		
		// TODO
		Map<OIDCClaimName, String> userInfo = userInfoCaptor.getValue();
	}

}
