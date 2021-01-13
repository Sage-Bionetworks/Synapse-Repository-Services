package org.sagebionetworks.repo.manager.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.StringUtils;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.oauth.ClaimsJsonUtil;
import org.sagebionetworks.repo.manager.oauth.OIDCTokenHelper;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UnauthenticatedException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AccessTokenGenerationRequest;
import org.sagebionetworks.repo.model.auth.AccessTokenRecord;
import org.sagebionetworks.repo.model.auth.AccessTokenRecordList;
import org.sagebionetworks.repo.model.auth.AccessTokenState;
import org.sagebionetworks.repo.model.auth.PersonalAccessTokenDao;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.Clock;
import org.springframework.dao.DuplicateKeyException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.impl.DefaultClaims;
import io.jsonwebtoken.impl.DefaultJwt;

@ExtendWith(MockitoExtension.class)
public class PersonalAccessTokenManagerImplUnitTest {

	@InjectMocks
	private PersonalAccessTokenManagerImpl personalAccessTokenManager;
	@Mock
	private PersonalAccessTokenDao mockPersonalAccessTokenDao;
	@Mock
	private OIDCTokenHelper mockTokenHelper;
	@Mock
	private Clock mockClock;
	
	private Jwt<JwsHeader, Claims> accessTokenJwt;

	private static final String OAUTH_ENDPOINT = "http://synapse.org/";
	private static final String TOKEN_ID = "999";
	private static final String ACCESS_TOKEN = "access-token";

	private UserInfo userInfo;
	private static final Long USER_ID = 123456L;
	private static final Long OTHER_USER_ID = 654321L;

	private static final Long ONE_YEAR_MILLIS = 1000L * 60 * 60 * 24 * 365;
	private static final Long EXPECTED_TOKEN_LIMIT = 100L;

	@BeforeEach
	void beforeEach() {
		userInfo = new UserInfo(false);
		userInfo.setId(USER_ID);
		
		Claims accessTokenClaims = new DefaultClaims();
		ClaimsJsonUtil.addAccessClaims(Arrays.asList(OAuthScope.values()), Collections.EMPTY_MAP, accessTokenClaims);
		accessTokenJwt = new DefaultJwt(null, accessTokenClaims);
	}

	@Test
	void testDetermineActiveState_active() {
		when(mockClock.currentTimeMillis()).thenReturn(System.currentTimeMillis());

		Date lastUsedDate = new Date(System.currentTimeMillis());

		// method under test
		assertEquals(AccessTokenState.ACTIVE, personalAccessTokenManager.determineActiveState(lastUsedDate));
	}


	@Test
	void testDetermineActiveState_expired() {
		when(mockClock.currentTimeMillis()).thenReturn(System.currentTimeMillis());

		Date lastUsedDate = new Date(System.currentTimeMillis() - ONE_YEAR_MILLIS);

		// method under test
		assertEquals(AccessTokenState.EXPIRED, personalAccessTokenManager.determineActiveState(lastUsedDate));
	}

	@Test
	void testIssueToken() {
		String tokenName = "My token name";
		List<OAuthScope> scopes = Arrays.asList(OAuthScope.openid, OAuthScope.view);
		Map<String, OIDCClaimsRequestDetails> claims = new HashMap<>();
		claims.put(OIDCClaimName.userid.name(), new OIDCClaimsRequestDetails());
		claims.put("some invalid claim", null);

		AccessTokenGenerationRequest request = new AccessTokenGenerationRequest();
		request.setName(tokenName);
		request.setScope(scopes);
		request.setUserInfoClaims(claims);

		ArgumentCaptor<AccessTokenRecord> recordCaptor = ArgumentCaptor.forClass(AccessTokenRecord.class);


		String expectedToken = "abc123";
		Map<String, OIDCClaimsRequestDetails> expectedClaims = new HashMap<>();
		expectedClaims.put(OIDCClaimName.userid.name(), new OIDCClaimsRequestDetails());

		AccessTokenRecord createdRecord = new AccessTokenRecord();
		when(mockClock.now()).thenReturn(new Date());
		when(mockPersonalAccessTokenDao.createTokenRecord(recordCaptor.capture())).thenReturn(createdRecord);
		when(mockTokenHelper.createPersonalAccessToken(OAUTH_ENDPOINT, createdRecord)).thenReturn(expectedToken);
		when(mockTokenHelper.parseJWT(ACCESS_TOKEN)).thenReturn(accessTokenJwt);
	
		// method under test
		String token = personalAccessTokenManager.issueToken(userInfo, ACCESS_TOKEN, request, OAUTH_ENDPOINT).getToken();

		AccessTokenRecord captured = recordCaptor.getValue();
		assertEquals(userInfo.getId().toString(), captured.getUserId());
		assertEquals(tokenName, captured.getName());
		assertEquals(scopes, captured.getScopes());
		assertEquals(expectedClaims, captured.getUserInfoClaims());
		assertNotNull(captured.getCreatedOn());
		assertNotNull(captured.getLastUsed());
		assertEquals(expectedToken, token);

		verify(mockPersonalAccessTokenDao).deleteLeastRecentlyUsedTokensOverLimit(userInfo.getId().toString(), EXPECTED_TOKEN_LIMIT);
	}

	@Test
	void testIssueTokenLimitedAccessTokenScope() {
		List<OAuthScope> requestedScopes = Arrays.asList(OAuthScope.openid, OAuthScope.view, OAuthScope.authorize, OAuthScope.authorize);
		List<OAuthScope> accessTokenScopes = Arrays.asList(OAuthScope.view, OAuthScope.authorize);
		// expected scope is the intersection of requestedScopes and accessTokenScopes, minus 'authorize'
		// so we expect 'view' but not 'openid'.  We do not expect 'authorize'
		List<OAuthScope> expectedScopes = Arrays.asList(OAuthScope.view);
		
		Claims accessTokenClaims = new DefaultClaims();
		// note, we omit 'modify'
		ClaimsJsonUtil.addAccessClaims(accessTokenScopes, Collections.EMPTY_MAP, accessTokenClaims);
		accessTokenJwt = new DefaultJwt(null, accessTokenClaims);
		
		AccessTokenGenerationRequest request = new AccessTokenGenerationRequest();
		request.setScope(requestedScopes);
		request.setUserInfoClaims(new HashMap<>());

		ArgumentCaptor<AccessTokenRecord> recordCaptor = ArgumentCaptor.forClass(AccessTokenRecord.class);

		String expectedToken = "abc123";

		AccessTokenRecord createdRecord = new AccessTokenRecord();
		when(mockClock.now()).thenReturn(new Date());
		when(mockPersonalAccessTokenDao.createTokenRecord(recordCaptor.capture())).thenReturn(createdRecord);
		when(mockTokenHelper.createPersonalAccessToken(OAUTH_ENDPOINT, createdRecord)).thenReturn(expectedToken);
		when(mockTokenHelper.parseJWT(ACCESS_TOKEN)).thenReturn(accessTokenJwt);
	
		// method under test
		personalAccessTokenManager.issueToken(userInfo, ACCESS_TOKEN, request, OAUTH_ENDPOINT).getToken();

		AccessTokenRecord captured = recordCaptor.getValue();
		assertEquals(userInfo.getId().toString(), captured.getUserId());
		assertEquals(expectedScopes, captured.getScopes());
	}

	@Test
	void testIssueToken_anonymous() {
		UserInfo anonymousUserInfo = new UserInfo(false);
		anonymousUserInfo.setId(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());

		// method under test
		assertThrows(UnauthenticatedException.class, () -> personalAccessTokenManager.issueToken(anonymousUserInfo, ACCESS_TOKEN, new AccessTokenGenerationRequest(), OAUTH_ENDPOINT));
	}

	@Test
	void testIssueToken_blankName() {
		List<OAuthScope> scopes = Arrays.asList(OAuthScope.openid, OAuthScope.authorize);
		Map<String, OIDCClaimsRequestDetails> claims = new HashMap<>();
		claims.put(OIDCClaimName.userid.name(), new OIDCClaimsRequestDetails());

		AccessTokenGenerationRequest request = new AccessTokenGenerationRequest();
		request.setName(null);
		request.setScope(scopes);
		request.setUserInfoClaims(claims);

		ArgumentCaptor<AccessTokenRecord> recordCaptor = ArgumentCaptor.forClass(AccessTokenRecord.class);

		String expectedToken = "abc123";

		AccessTokenRecord createdRecord = new AccessTokenRecord();
		when(mockPersonalAccessTokenDao.createTokenRecord(recordCaptor.capture())).thenReturn(createdRecord);
		when(mockTokenHelper.createPersonalAccessToken(OAUTH_ENDPOINT, createdRecord)).thenReturn(expectedToken);
		when(mockTokenHelper.parseJWT(ACCESS_TOKEN)).thenReturn(accessTokenJwt);

		// method under test
		personalAccessTokenManager.issueToken(userInfo, ACCESS_TOKEN, request, OAUTH_ENDPOINT);

		AccessTokenRecord captured = recordCaptor.getValue();
		assertTrue(StringUtils.isNotBlank(captured.getName()));

		verify(mockPersonalAccessTokenDao).deleteLeastRecentlyUsedTokensOverLimit(userInfo.getId().toString(), EXPECTED_TOKEN_LIMIT);
	}

	@Test
	void testIssueToken_emptyScope() {
		String tokenName = "My token name";

		Map<String, OIDCClaimsRequestDetails> claims = new HashMap<>();
		claims.put(OIDCClaimName.userid.name(), new OIDCClaimsRequestDetails());

		AccessTokenGenerationRequest request = new AccessTokenGenerationRequest();
		request.setName(tokenName);
		request.setScope(null);
		request.setUserInfoClaims(claims);

		ArgumentCaptor<AccessTokenRecord> recordCaptor = ArgumentCaptor.forClass(AccessTokenRecord.class);

		String expectedToken = "abc123";

		AccessTokenRecord createdRecord = new AccessTokenRecord();
		when(mockPersonalAccessTokenDao.createTokenRecord(recordCaptor.capture())).thenReturn(createdRecord);
		when(mockTokenHelper.createPersonalAccessToken(OAUTH_ENDPOINT, createdRecord)).thenReturn(expectedToken);
		when(mockTokenHelper.parseJWT(ACCESS_TOKEN)).thenReturn(accessTokenJwt);

		// method under test
		personalAccessTokenManager.issueToken(userInfo, ACCESS_TOKEN, request, OAUTH_ENDPOINT);

		Set<OAuthScope> expectedScope = new HashSet<>();
		Collections.addAll(expectedScope, OAuthScope.values());
		expectedScope.remove(OAuthScope.authorize);

		AccessTokenRecord captured = recordCaptor.getValue();
		Set<OAuthScope> actualScope = new HashSet<>(captured.getScopes());
		assertEquals(expectedScope, actualScope);

		verify(mockPersonalAccessTokenDao).deleteLeastRecentlyUsedTokensOverLimit(userInfo.getId().toString(), EXPECTED_TOKEN_LIMIT);
	}

	@Test
	void testIssueToken_nullClaims() {
		String tokenName = "My token name";
		List<OAuthScope> scopes = Arrays.asList(OAuthScope.openid, OAuthScope.authorize);

		AccessTokenGenerationRequest request = new AccessTokenGenerationRequest();
		request.setName(tokenName);
		request.setScope(scopes);
		request.setUserInfoClaims(null);

		ArgumentCaptor<AccessTokenRecord> recordCaptor = ArgumentCaptor.forClass(AccessTokenRecord.class);

		String expectedToken = "abc123";

		AccessTokenRecord createdRecord = new AccessTokenRecord();
		when(mockPersonalAccessTokenDao.createTokenRecord(recordCaptor.capture())).thenReturn(createdRecord);
		when(mockTokenHelper.createPersonalAccessToken(OAUTH_ENDPOINT, createdRecord)).thenReturn(expectedToken);
		when(mockTokenHelper.parseJWT(ACCESS_TOKEN)).thenReturn(accessTokenJwt);

		// method under test
		personalAccessTokenManager.issueToken(userInfo, ACCESS_TOKEN, request, OAUTH_ENDPOINT);

		Map<String, OIDCClaimsRequestDetails> expectedClaims = Collections.emptyMap();

		AccessTokenRecord captured = recordCaptor.getValue();
		assertEquals(expectedClaims, captured.getUserInfoClaims());

		verify(mockPersonalAccessTokenDao).deleteLeastRecentlyUsedTokensOverLimit(userInfo.getId().toString(), EXPECTED_TOKEN_LIMIT);
	}

	@Test
	void testIsTokenActive() {
		when(mockClock.currentTimeMillis()).thenReturn(System.currentTimeMillis());
		when(mockPersonalAccessTokenDao.getLastUsedDate(TOKEN_ID)).thenReturn(new Date(System.currentTimeMillis()));
		// method under test
		assertTrue(personalAccessTokenManager.isTokenActive(TOKEN_ID));
	}

	@Test
	void testIsTokenActive_notFound() {
		when(mockPersonalAccessTokenDao.getLastUsedDate(TOKEN_ID)).thenThrow(new NotFoundException());
		// method under test
		assertFalse(personalAccessTokenManager.isTokenActive(TOKEN_ID));

	}

	@Test
	void testUpdateLastUsedTimeHappyCase() {
		when(mockPersonalAccessTokenDao.getLastUsedDate(TOKEN_ID)).thenReturn(new Date(System.currentTimeMillis()));
		when(mockClock.currentTimeMillis()).thenReturn(System.currentTimeMillis()+70000);
		// method under test
		personalAccessTokenManager.updateLastUsedTime(TOKEN_ID);
		verify(mockPersonalAccessTokenDao).updateLastUsed(TOKEN_ID);
	}

	@Test
	void testUpdateLastUsedFirstTime() {
		when(mockPersonalAccessTokenDao.getLastUsedDate(TOKEN_ID)).thenThrow(new NotFoundException());
		// method under test
		personalAccessTokenManager.updateLastUsedTime(TOKEN_ID);
		verify(mockPersonalAccessTokenDao).updateLastUsed(TOKEN_ID);
	}

	@Test
	void testUpdateLastUsedTimeTooFrequentChange() {
		when(mockClock.currentTimeMillis()).thenReturn(System.currentTimeMillis());
		when(mockPersonalAccessTokenDao.getLastUsedDate(TOKEN_ID)).thenReturn(new Date(System.currentTimeMillis()));
		// method under test
		personalAccessTokenManager.updateLastUsedTime(TOKEN_ID);
		verify(mockPersonalAccessTokenDao, never()).updateLastUsed(TOKEN_ID);
	}
	
	@Test
	void testGetTokens() {
		String nextPageToken = "npt1";

		AccessTokenRecordList expected = new AccessTokenRecordList();
		expected.setResults(Collections.emptyList());
		expected.setNextPageToken(null);

		when(mockPersonalAccessTokenDao.getTokenRecords(USER_ID.toString(), nextPageToken)).thenReturn(expected);

		// Call under test
		AccessTokenRecordList actual = personalAccessTokenManager.getTokenRecords(userInfo, nextPageToken);
		assertEquals(expected, actual);
	}

	@Test
	void testGetTokens_anonymous() {
		UserInfo anonymousUserInfo = new UserInfo(false);
		anonymousUserInfo.setId(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());

		// method under test
		assertThrows(UnauthenticatedException.class, () -> personalAccessTokenManager.getTokenRecords(anonymousUserInfo, null));
	}

	@Test
	void testGetToken() {
		AccessTokenRecord tokenRecord = new AccessTokenRecord();
		tokenRecord.setId(TOKEN_ID);
		tokenRecord.setUserId(USER_ID.toString());
		tokenRecord.setLastUsed(new Date());
		when(mockClock.currentTimeMillis()).thenReturn(System.currentTimeMillis());
		when(mockPersonalAccessTokenDao.getTokenRecord(TOKEN_ID)).thenReturn(tokenRecord);

		// method under test
		AccessTokenRecord retrieved = personalAccessTokenManager.getTokenRecord(userInfo, TOKEN_ID);

		assertEquals(tokenRecord, retrieved);
	}

	@Test
	void testGetToken_admin() {
		UserInfo adminUserInfo = new UserInfo(true);
		adminUserInfo.setId(1L);

		AccessTokenRecord tokenRecord = new AccessTokenRecord();
		tokenRecord.setId(TOKEN_ID);
		tokenRecord.setUserId(USER_ID.toString());
		tokenRecord.setLastUsed(new Date());

		when(mockClock.currentTimeMillis()).thenReturn(System.currentTimeMillis());
		when(mockPersonalAccessTokenDao.getTokenRecord(TOKEN_ID)).thenReturn(tokenRecord);

		// method under test
		AccessTokenRecord retrieved = personalAccessTokenManager.getTokenRecord(userInfo, TOKEN_ID);

		assertEquals(tokenRecord, retrieved);
	}


	@Test
	void testGetToken_otherUser() {
		AccessTokenRecord tokenRecord = new AccessTokenRecord();
		tokenRecord.setId(TOKEN_ID);
		tokenRecord.setUserId(OTHER_USER_ID.toString());
		when(mockPersonalAccessTokenDao.getTokenRecord(TOKEN_ID)).thenReturn(tokenRecord);

		// method under test
		assertThrows(UnauthorizedException.class, () -> personalAccessTokenManager.getTokenRecord(userInfo, TOKEN_ID));

	}

	@Test
	void testRevokeToken() {
		AccessTokenRecord tokenRecord = new AccessTokenRecord();
		tokenRecord.setId(TOKEN_ID);
		tokenRecord.setUserId(USER_ID.toString());
		when(mockPersonalAccessTokenDao.getTokenRecord(TOKEN_ID)).thenReturn(tokenRecord);

		// method under test
		personalAccessTokenManager.revokeToken(userInfo, TOKEN_ID);

		verify(mockPersonalAccessTokenDao).deleteToken(TOKEN_ID);
	}

	@Test
	void testRevokeToken_admin() {
		UserInfo adminUserInfo = new UserInfo(true);
		adminUserInfo.setId(1L);

		AccessTokenRecord tokenRecord = new AccessTokenRecord();
		tokenRecord.setId(TOKEN_ID);
		tokenRecord.setUserId(USER_ID.toString());
		when(mockPersonalAccessTokenDao.getTokenRecord(TOKEN_ID)).thenReturn(tokenRecord);

		// method under test
		personalAccessTokenManager.revokeToken(adminUserInfo, TOKEN_ID);

		verify(mockPersonalAccessTokenDao).deleteToken(TOKEN_ID);
	}


	@Test
	void testRevokeToken_otherUser() {
		AccessTokenRecord tokenRecord = new AccessTokenRecord();
		tokenRecord.setId(TOKEN_ID);
		tokenRecord.setUserId(OTHER_USER_ID.toString());
		when(mockPersonalAccessTokenDao.getTokenRecord(TOKEN_ID)).thenReturn(tokenRecord);

		// method under test
		assertThrows(UnauthorizedException. class, () -> personalAccessTokenManager.revokeToken(userInfo, TOKEN_ID));

		verify(mockPersonalAccessTokenDao, never()).deleteToken(TOKEN_ID);
	}

	@Test // PLFM-6494
	void testCreateToken_DuplicateKeyException() {
		when(mockTokenHelper.parseJWT(ACCESS_TOKEN)).thenReturn(accessTokenJwt);
		when(mockPersonalAccessTokenDao.createTokenRecord(any())).thenThrow(new IllegalArgumentException(new DuplicateKeyException("message")));

		// Method under test
		assertThrows(
				IllegalArgumentException.class,
				() -> personalAccessTokenManager.issueToken(userInfo, ACCESS_TOKEN, new AccessTokenGenerationRequest(), OAUTH_ENDPOINT),
				PersonalAccessTokenManagerImpl.DUPLICATE_TOKEN_NAME_MSG);
	}
}
