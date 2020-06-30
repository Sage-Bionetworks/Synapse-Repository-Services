package org.sagebionetworks.repo.manager.oauth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.StringUtils;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.OAuthClientDao;
import org.sagebionetworks.repo.model.auth.OAuthDao;
import org.sagebionetworks.repo.model.auth.OAuthRefreshTokenDao;
import org.sagebionetworks.repo.model.oauth.OAuthClientAuthorizationHistoryList;
import org.sagebionetworks.repo.model.oauth.OAuthRefreshTokenInformation;
import org.sagebionetworks.repo.model.oauth.OAuthRefreshTokenInformationList;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequest;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.OAuthBadRequestException;
import org.sagebionetworks.util.Clock;

@ExtendWith(MockitoExtension.class)
public class OAuthRefreshTokenManagerImplUnitTest {

	@Mock
	OAuthDao mockOAuthDao;

	@Mock
	OAuthClientDao mockOAuthClientDao;

	@Mock
	OAuthRefreshTokenDao mockOAuthRefreshTokenDao;

	@Mock
	Clock clock;

	@InjectMocks
	OAuthRefreshTokenManagerImpl oauthRefreshTokenManager;

	private static final Long EXPECTED_LEASE_DURATION_DAYS = 180L;
	private static final Long EXPECTED_MAX_REFRESH_TOKENS = 100L;

	// Some constants to simplify test setup
	private static final String TOKEN_ID = "1234";
	private static final String USER_ID = "4321";
	private static final String CLIENT_ID = "67890";

	private static final UserInfo USER_INFO = new UserInfo(false, Long.valueOf(USER_ID));

	@Test
	public void testCreateRefreshToken() {
		List<OAuthScope> scopes = Arrays.asList(OAuthScope.authorize, OAuthScope.openid);
		OIDCClaimsRequest claimsRequest = new OIDCClaimsRequest();
		claimsRequest.setUserinfo(Collections.emptyMap());
		claimsRequest.setId_token(Collections.emptyMap());

		when(clock.now()).thenReturn(new Date()); // Ensure all 'clock.now()' calls return the same time

		ArgumentCaptor<OAuthRefreshTokenInformation> captor = ArgumentCaptor.forClass(OAuthRefreshTokenInformation.class);

		OAuthRefreshTokenInformation createdToken = new OAuthRefreshTokenInformation();
		createdToken.setTokenId(TOKEN_ID);
		createdToken.setPrincipalId(USER_ID);
		createdToken.setClientId(CLIENT_ID);

		when(mockOAuthRefreshTokenDao.createRefreshToken(anyString(), captor.capture())).thenReturn(createdToken);


		// Call under test
		OAuthRefreshTokenAndMetadata actual = oauthRefreshTokenManager.createRefreshToken(USER_ID, CLIENT_ID, scopes, claimsRequest);
		assertNotNull(actual);
		assertEquals(TOKEN_ID, actual.getMetadata().getTokenId());
		assertTrue(StringUtils.isNotBlank(actual.getRefreshToken()));

		assertEquals(USER_ID, captor.getValue().getPrincipalId());
		assertEquals(CLIENT_ID, captor.getValue().getClientId());
		assertEquals(scopes, captor.getValue().getScopes());
		assertEquals(claimsRequest, captor.getValue().getClaims());
		assertTrue(StringUtils.isNotBlank(captor.getValue().getName()));
		assertTrue(StringUtils.isNotBlank(captor.getValue().getEtag()));
		assertEquals(clock.now(), captor.getValue().getLastUsed());
		assertEquals(clock.now(), captor.getValue().getAuthorizedOn());
		assertEquals(clock.now(), captor.getValue().getModifiedOn());

		verify(mockOAuthRefreshTokenDao).deleteLeastRecentlyUsedTokensOverLimit(USER_ID, CLIENT_ID, EXPECTED_MAX_REFRESH_TOKENS - 1);
		verify(mockOAuthRefreshTokenDao).createRefreshToken(anyString(), any(OAuthRefreshTokenInformation.class));
	}

	@Test
	public void testCreateRefreshToken_anonymousUser() {
		String anonymousUser = AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId().toString();

		List<OAuthScope> scopes = Arrays.asList(OAuthScope.authorize, OAuthScope.openid);
		OIDCClaimsRequest claimsRequest = new OIDCClaimsRequest();
		claimsRequest.setUserinfo(Collections.emptyMap());
		claimsRequest.setId_token(Collections.emptyMap());

		assertThrows(UnauthorizedException.class, () -> oauthRefreshTokenManager.createRefreshToken(anonymousUser, CLIENT_ID, scopes, claimsRequest));
	}

	@Test
	public void testRotateToken() {
		String token = "token val";
		String oldEtag = "an etag that should be changed";
		when(clock.now()).thenReturn(new Date()); // Ensure all 'clock.now()' calls return the same time
		Date oldDate = new Date(clock.now().getTime() - 10000); // For old dates, use different time than clock.now() returns
		assertNotEquals(oldDate, clock.now()); // sanity check
		OAuthRefreshTokenInformation existingToken = new OAuthRefreshTokenInformation();
		existingToken.setTokenId(TOKEN_ID);
		existingToken.setPrincipalId(USER_ID);
		existingToken.setModifiedOn(oldDate); // Should not be updated
		existingToken.setLastUsed(oldDate); // Should be updated
		existingToken.setEtag(oldEtag);

		when(mockOAuthRefreshTokenDao.getMatchingTokenByHashForUpdate(DigestUtils.sha256Hex(token))).thenReturn(Optional.of(existingToken));
		when(mockOAuthRefreshTokenDao.isTokenActive(TOKEN_ID, EXPECTED_LEASE_DURATION_DAYS)).thenReturn(true);

		ArgumentCaptor<OAuthRefreshTokenInformation> metadataCaptor = ArgumentCaptor.forClass(OAuthRefreshTokenInformation.class);
		ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);

		doNothing().when(mockOAuthRefreshTokenDao).updateTokenHash(metadataCaptor.capture(), tokenCaptor.capture());

		// Note that this object won't have the altered metadata, but we check that it has been altered in the above answer
		when(mockOAuthRefreshTokenDao.getRefreshTokenMetadata(TOKEN_ID)).thenReturn(Optional.of(existingToken));

		// Call under test
		OAuthRefreshTokenAndMetadata actual = oauthRefreshTokenManager.rotateRefreshToken(token);

		assertTrue(StringUtils.isNotBlank(tokenCaptor.getValue()));

		// Verify that the token was modified
		assertEquals(TOKEN_ID, metadataCaptor.getValue().getTokenId());
		assertEquals(USER_ID, metadataCaptor.getValue().getPrincipalId());
		assertEquals(clock.now(), metadataCaptor.getValue().getLastUsed());
		assertEquals(oldDate, metadataCaptor.getValue().getModifiedOn());
		assertNotEquals(oldEtag, metadataCaptor.getValue().getEtag());

		assertEquals(existingToken, actual.getMetadata());
		assertTrue(StringUtils.isNotBlank(actual.getRefreshToken()));
		assertNotEquals(token, actual.getRefreshToken());

		verify(mockOAuthRefreshTokenDao).getMatchingTokenByHashForUpdate(DigestUtils.sha256Hex(token));
		verify(mockOAuthRefreshTokenDao).updateTokenHash(any(OAuthRefreshTokenInformation.class), anyString());

	}

	@Test
	public void testRotateRefreshToken_IllegalArgumentOnNonmatchingToken() {
		when(mockOAuthRefreshTokenDao.getMatchingTokenByHashForUpdate(anyString())).thenReturn(Optional.empty());

		// Call under test
		assertThrows(IllegalArgumentException.class, () -> oauthRefreshTokenManager.rotateRefreshToken("token"));
	}

	@Test
	public void testRotateRefreshToken_expiredToken() {
		OAuthRefreshTokenInformation tokenMetadata = new OAuthRefreshTokenInformation();
		tokenMetadata.setTokenId(TOKEN_ID);
		when(mockOAuthRefreshTokenDao.getMatchingTokenByHashForUpdate(anyString())).thenReturn(Optional.of(
				tokenMetadata
		));
		when(mockOAuthRefreshTokenDao.isTokenActive(TOKEN_ID, EXPECTED_LEASE_DURATION_DAYS)).thenReturn(false);


		// Call under test
		assertThrows(OAuthBadRequestException.class, () -> oauthRefreshTokenManager.rotateRefreshToken("token"));
	}


	@Test
	public void testRotateRefreshToken_IllegalStateOnFailedRetrieval() {
		OAuthRefreshTokenInformation existingToken = new OAuthRefreshTokenInformation();
		existingToken.setTokenId(TOKEN_ID);
		existingToken.setPrincipalId(USER_ID);
		existingToken.setModifiedOn(new Date());
		existingToken.setLastUsed(new Date());
		existingToken.setEtag(UUID.randomUUID().toString());

		when(mockOAuthRefreshTokenDao.getMatchingTokenByHashForUpdate(anyString())).thenReturn(Optional.of(existingToken));
		when(mockOAuthRefreshTokenDao.isTokenActive(TOKEN_ID, EXPECTED_LEASE_DURATION_DAYS)).thenReturn(true);
		when(mockOAuthRefreshTokenDao.getRefreshTokenMetadata(TOKEN_ID)).thenReturn(Optional.empty());
		// Call under test
		assertThrows(IllegalStateException.class, () -> oauthRefreshTokenManager.rotateRefreshToken("token"));
	}

	@Test
	public void testGetTokenMetadataWithUserInfo() {
		OAuthRefreshTokenInformation expected = new OAuthRefreshTokenInformation();
		expected.setTokenId(TOKEN_ID);
		expected.setPrincipalId(USER_ID);

		when(mockOAuthRefreshTokenDao.getRefreshTokenMetadata(TOKEN_ID)).thenReturn(Optional.of(expected));

		// Call under test
		OAuthRefreshTokenInformation actual = oauthRefreshTokenManager.getRefreshTokenMetadata(USER_INFO, TOKEN_ID);

		assertEquals(expected, actual);
		verify(mockOAuthRefreshTokenDao).getRefreshTokenMetadata(TOKEN_ID);
	}

	@Test
	public void testGetTokenMetadataWithUserInfo_NotFound() {
		when(mockOAuthRefreshTokenDao.getRefreshTokenMetadata(TOKEN_ID)).thenReturn(Optional.empty());

		// Call under test
		assertThrows(NotFoundException.class, () -> oauthRefreshTokenManager.getRefreshTokenMetadata(USER_INFO, TOKEN_ID));

		verify(mockOAuthRefreshTokenDao).getRefreshTokenMetadata(TOKEN_ID);
	}

	@Test
	public void testGetTokenMetadataWithUserInfo_Unauthorized() {
		OAuthRefreshTokenInformation expected = new OAuthRefreshTokenInformation();
		expected.setTokenId(TOKEN_ID);
		expected.setPrincipalId("some other user ID");

		when(mockOAuthRefreshTokenDao.getRefreshTokenMetadata(TOKEN_ID)).thenReturn(Optional.of(expected));

		// Call under test
		assertThrows(UnauthorizedException.class, () -> oauthRefreshTokenManager.getRefreshTokenMetadata(USER_INFO, TOKEN_ID));

		verify(mockOAuthRefreshTokenDao).getRefreshTokenMetadata(TOKEN_ID);
	}

	@Test
	public void testGetTokenMetadataWithClientId() {
		OAuthRefreshTokenInformation expected = new OAuthRefreshTokenInformation();
		expected.setTokenId(TOKEN_ID);
		expected.setClientId(CLIENT_ID);

		when(mockOAuthRefreshTokenDao.getRefreshTokenMetadata(TOKEN_ID)).thenReturn(Optional.of(expected));

		// Call under test
		OAuthRefreshTokenInformation actual = oauthRefreshTokenManager.getRefreshTokenMetadata(CLIENT_ID, TOKEN_ID);

		assertEquals(expected, actual);
		verify(mockOAuthRefreshTokenDao).getRefreshTokenMetadata(TOKEN_ID);
	}

	@Test
	public void testGetTokenMetadataWithClientId_NotFound() {
		when(mockOAuthRefreshTokenDao.getRefreshTokenMetadata(TOKEN_ID)).thenReturn(Optional.empty());

		// Call under test
		assertThrows(NotFoundException.class, () -> oauthRefreshTokenManager.getRefreshTokenMetadata(CLIENT_ID, TOKEN_ID));

		verify(mockOAuthRefreshTokenDao).getRefreshTokenMetadata(TOKEN_ID);
	}

	@Test
	public void testGetTokenMetadataWithClientId_Unauthorized() {
		OAuthRefreshTokenInformation expected = new OAuthRefreshTokenInformation();
		expected.setTokenId(TOKEN_ID);
		expected.setClientId("some other client ID");

		when(mockOAuthRefreshTokenDao.getRefreshTokenMetadata(TOKEN_ID)).thenReturn(Optional.of(expected));

		// Call under test
		assertThrows(UnauthorizedException.class, () -> oauthRefreshTokenManager.getRefreshTokenMetadata(CLIENT_ID, TOKEN_ID));

		verify(mockOAuthRefreshTokenDao).getRefreshTokenMetadata(TOKEN_ID);
	}

	@Test
	public void testGetTokenMetadataWithToken() {
		String token = "opaque refresh token";

		OAuthRefreshTokenInformation expected = new OAuthRefreshTokenInformation();
		expected.setTokenId(TOKEN_ID);
		expected.setClientId(CLIENT_ID);

		when(mockOAuthRefreshTokenDao.getMatchingTokenByHash(anyString())).thenReturn(Optional.of(expected));

		// Call under test
		OAuthRefreshTokenInformation actual = oauthRefreshTokenManager.getRefreshTokenMetadataWithToken(CLIENT_ID, token);

		assertEquals(expected, actual);
		verify(mockOAuthRefreshTokenDao).getMatchingTokenByHash(anyString());
	}

	@Test
	public void testGetTokenMetadataWithToken_NotFound() {
		String token = "opaque refresh token";
		when(mockOAuthRefreshTokenDao.getMatchingTokenByHash(anyString())).thenReturn(Optional.empty());

		// Call under test
		assertThrows(NotFoundException.class, () -> oauthRefreshTokenManager.getRefreshTokenMetadataWithToken(CLIENT_ID, token));

		verify(mockOAuthRefreshTokenDao).getMatchingTokenByHash(anyString());
	}

	@Test
	public void testGetTokenMetadataWithToken_Unauthorized() {
		String token = "opaque refresh token";
		OAuthRefreshTokenInformation expected = new OAuthRefreshTokenInformation();
		expected.setTokenId(TOKEN_ID);
		expected.setClientId("some other client ID");

		when(mockOAuthRefreshTokenDao.getMatchingTokenByHash(anyString())).thenReturn(Optional.of(expected));

		// Call under test
		assertThrows(UnauthorizedException.class, () -> oauthRefreshTokenManager.getRefreshTokenMetadataWithToken(CLIENT_ID, token));

		verify(mockOAuthRefreshTokenDao).getMatchingTokenByHash(anyString());
	}

	@Test
	public void testGetClientHistory() {
		String nextPageToken = "initial npt";

		OAuthClientAuthorizationHistoryList expected = new OAuthClientAuthorizationHistoryList();
		expected.setResults(Collections.emptyList());
		expected.setNextPageToken("some new nextpagetoken");

		when(mockOAuthClientDao.getAuthorizedClientHistory(USER_ID, nextPageToken, EXPECTED_LEASE_DURATION_DAYS)).thenReturn(expected);

		// Call under test
		OAuthClientAuthorizationHistoryList actual = oauthRefreshTokenManager.getAuthorizedClientHistory(USER_INFO, nextPageToken);

		assertEquals(expected, actual);
		verify(mockOAuthClientDao).getAuthorizedClientHistory(USER_ID, nextPageToken, EXPECTED_LEASE_DURATION_DAYS);
	}

	@Test
	public void testGetActiveRefreshTokensMetadata() {
		String nextPageToken = "initial npt";

		OAuthRefreshTokenInformationList expected = new OAuthRefreshTokenInformationList();
		expected.setResults(Collections.emptyList());
		expected.setNextPageToken("some new nextpagetoken");

		when(mockOAuthRefreshTokenDao.getActiveTokenInformation(USER_ID, CLIENT_ID, nextPageToken, EXPECTED_LEASE_DURATION_DAYS)).thenReturn(expected);

		// Call under test
		OAuthRefreshTokenInformationList actual = oauthRefreshTokenManager.getMetadataForActiveRefreshTokens(USER_INFO, CLIENT_ID, nextPageToken);

		assertEquals(expected, actual);
		verify(mockOAuthRefreshTokenDao).getActiveTokenInformation(USER_ID, CLIENT_ID, nextPageToken, EXPECTED_LEASE_DURATION_DAYS);
	}

	@Test
	public void testRevokeTokensForUserClientPair() {
		OAuthRefreshTokenInformationList existingTokens = new OAuthRefreshTokenInformationList();
		existingTokens.setResults(Collections.singletonList(new OAuthRefreshTokenInformation())); // need a list of length >= 1

		// Call under test
		oauthRefreshTokenManager.revokeRefreshTokensForUserClientPair(USER_INFO, CLIENT_ID);

		verify(mockOAuthDao).deleteAuthorizationConsentForClient(Long.valueOf(USER_ID), Long.valueOf(CLIENT_ID));
		verify(mockOAuthRefreshTokenDao).deleteAllTokensForUserClientPair(USER_ID, CLIENT_ID);
	}

	@Test
	public void testRevokeTokenAsUser() {
		OAuthRefreshTokenInformation existingToken = new OAuthRefreshTokenInformation();
		existingToken.setTokenId(TOKEN_ID);
		existingToken.setPrincipalId(USER_ID);

		when(mockOAuthRefreshTokenDao.getRefreshTokenMetadata(TOKEN_ID)).thenReturn(Optional.of(existingToken));

		// Call under test
		oauthRefreshTokenManager.revokeRefreshToken(USER_INFO, TOKEN_ID);

		verify(mockOAuthRefreshTokenDao).getRefreshTokenMetadata(TOKEN_ID);
		verify(mockOAuthRefreshTokenDao).deleteToken(TOKEN_ID);
	}

	@Test
	public void testRevokeTokenAsUser_NotFound() {
		when(mockOAuthRefreshTokenDao.getRefreshTokenMetadata(TOKEN_ID)).thenReturn(Optional.empty());

		// Call under test
		assertThrows(NotFoundException.class, () -> oauthRefreshTokenManager.revokeRefreshToken(USER_INFO, TOKEN_ID));

		verify(mockOAuthRefreshTokenDao).getRefreshTokenMetadata(TOKEN_ID);
		verify(mockOAuthRefreshTokenDao, never()).deleteToken(TOKEN_ID);
	}

	@Test
	public void testRevokeTokenAsUser_Unauthorized() {
		OAuthRefreshTokenInformation existingToken = new OAuthRefreshTokenInformation();
		existingToken.setTokenId(TOKEN_ID);
		existingToken.setPrincipalId("some other user ID");

		when(mockOAuthRefreshTokenDao.getRefreshTokenMetadata(TOKEN_ID)).thenReturn(Optional.of(existingToken));

		// Call under test
		assertThrows(UnauthorizedException.class, () -> oauthRefreshTokenManager.revokeRefreshToken(USER_INFO, TOKEN_ID));

		verify(mockOAuthRefreshTokenDao).getRefreshTokenMetadata(TOKEN_ID);
		verify(mockOAuthRefreshTokenDao, never()).deleteToken(TOKEN_ID);
	}


	@Test
	public void testRevokeTokenAsClient() {
		OAuthRefreshTokenInformation existingToken = new OAuthRefreshTokenInformation();
		existingToken.setTokenId(TOKEN_ID);
		existingToken.setClientId(CLIENT_ID);

		when(mockOAuthRefreshTokenDao.getRefreshTokenMetadata(TOKEN_ID)).thenReturn(Optional.of(existingToken));

		// Call under test
		oauthRefreshTokenManager.revokeRefreshToken(CLIENT_ID, TOKEN_ID);

		verify(mockOAuthRefreshTokenDao).getRefreshTokenMetadata(TOKEN_ID);
		verify(mockOAuthRefreshTokenDao).deleteToken(TOKEN_ID);
	}

	@Test
	public void testRevokeTokenAsClient_NotFound() {
		when(mockOAuthRefreshTokenDao.getRefreshTokenMetadata(TOKEN_ID)).thenReturn(Optional.empty());

		// Call under test
		assertThrows(NotFoundException.class, () -> oauthRefreshTokenManager.revokeRefreshToken(CLIENT_ID, TOKEN_ID));

		verify(mockOAuthRefreshTokenDao).getRefreshTokenMetadata(TOKEN_ID);
		verify(mockOAuthRefreshTokenDao, never()).deleteToken(TOKEN_ID);
	}

	@Test
	public void testRevokeTokenAsClient_Unauthorized() {
		OAuthRefreshTokenInformation existingToken = new OAuthRefreshTokenInformation();
		existingToken.setTokenId(TOKEN_ID);
		existingToken.setClientId("some other client ID");

		when(mockOAuthRefreshTokenDao.getRefreshTokenMetadata(TOKEN_ID)).thenReturn(Optional.of(existingToken));

		// Call under test
		assertThrows(UnauthorizedException.class, () -> oauthRefreshTokenManager.revokeRefreshToken(CLIENT_ID, TOKEN_ID));

		verify(mockOAuthRefreshTokenDao).getRefreshTokenMetadata(TOKEN_ID);
		verify(mockOAuthRefreshTokenDao, never()).deleteToken(TOKEN_ID);
	}

	@Test
	public void testUpdateToken() {
		when(clock.now()).thenReturn(new Date());

		String oldName = "old name";
		String newName = "new name";
		String oldEtag = "abcd";

		OAuthRefreshTokenInformation newMetadata = new OAuthRefreshTokenInformation();
		newMetadata.setTokenId(TOKEN_ID);
		newMetadata.setPrincipalId(USER_ID);
		newMetadata.setName(newName);
		newMetadata.setEtag(oldEtag);
		newMetadata.setModifiedOn(new Date(clock.now().getTime() - 100000));

		OAuthRefreshTokenInformation retrievedFromDao = new OAuthRefreshTokenInformation();
		retrievedFromDao.setTokenId(TOKEN_ID);
		retrievedFromDao.setPrincipalId(USER_ID);
		retrievedFromDao.setName(oldName);
		retrievedFromDao.setEtag(oldEtag);
		newMetadata.setModifiedOn(new Date(clock.now().getTime() - 100000));


		when(mockOAuthRefreshTokenDao.getRefreshTokenMetadata(TOKEN_ID)).thenReturn(Optional.of(retrievedFromDao));

		ArgumentCaptor<OAuthRefreshTokenInformation> captor = ArgumentCaptor.forClass(OAuthRefreshTokenInformation.class);

		doNothing().when(mockOAuthRefreshTokenDao).updateRefreshTokenMetadata(captor.capture());

		// Call under test
		OAuthRefreshTokenInformation updated = oauthRefreshTokenManager.updateRefreshTokenMetadata(USER_INFO, newMetadata);

		// Just check that the updated object came from the DAO.getRefreshTokenMetadata method. The fields won't be updated because it's a mock
		assertEquals(updated, retrievedFromDao);

		assertNotNull(captor.getValue());
		assertNotEquals(oldEtag, captor.getValue().getEtag());
		assertEquals(newName, captor.getValue().getName());
		assertEquals(clock.now(), captor.getValue().getModifiedOn());

		verify(mockOAuthRefreshTokenDao, times(2)).getRefreshTokenMetadata(TOKEN_ID);
		verify(mockOAuthRefreshTokenDao).updateRefreshTokenMetadata(any(OAuthRefreshTokenInformation.class));
	}

	@Test
	public void testUpdateToken_RequiredArguments() {
		OAuthRefreshTokenInformation metadata = new OAuthRefreshTokenInformation();
		metadata.setTokenId(TOKEN_ID);
		metadata.setName(UUID.randomUUID().toString());
		metadata.setEtag(UUID.randomUUID().toString());

		// No metadata
		assertThrows(IllegalArgumentException.class,
				() -> oauthRefreshTokenManager.updateRefreshTokenMetadata(USER_INFO, null));

		// No token ID
		metadata.setTokenId(null);
		assertThrows(IllegalArgumentException.class,
				() -> oauthRefreshTokenManager.updateRefreshTokenMetadata(USER_INFO, metadata));
		metadata.setTokenId(TOKEN_ID);

		// No token name
		metadata.setName(null);
		assertThrows(IllegalArgumentException.class,
				() -> oauthRefreshTokenManager.updateRefreshTokenMetadata(USER_INFO, metadata));
		metadata.setName(UUID.randomUUID().toString());

		// No etag
		metadata.setEtag(null);
		assertThrows(IllegalArgumentException.class,
				() -> oauthRefreshTokenManager.updateRefreshTokenMetadata(USER_INFO, metadata));
	}

	@Test
	public void testUpdateToken_Unauthorized() {
		String differentUserId = UUID.randomUUID().toString();
		String oldName = "old name";
		String newName = "new name";
		String oldEtag = "abcd";

		OAuthRefreshTokenInformation newMetadata = new OAuthRefreshTokenInformation();
		newMetadata.setTokenId(TOKEN_ID);
		newMetadata.setPrincipalId(USER_ID);
		newMetadata.setName(newName);
		newMetadata.setEtag(oldEtag);

		OAuthRefreshTokenInformation retrievedFromDao = new OAuthRefreshTokenInformation();
		retrievedFromDao.setTokenId(TOKEN_ID);
		retrievedFromDao.setPrincipalId(differentUserId);
		retrievedFromDao.setName(oldName);
		retrievedFromDao.setEtag(oldEtag);


		when(mockOAuthRefreshTokenDao.getRefreshTokenMetadata(TOKEN_ID)).thenReturn(Optional.of(retrievedFromDao));

		// Call under test
		assertThrows(UnauthorizedException.class, () -> oauthRefreshTokenManager.updateRefreshTokenMetadata(USER_INFO, newMetadata));

		verify(mockOAuthRefreshTokenDao).getRefreshTokenMetadata(TOKEN_ID);
		verify(mockOAuthRefreshTokenDao, never()).updateRefreshTokenMetadata(any(OAuthRefreshTokenInformation.class));
	}

	@Test
	public void testUpdateToken_MismatchEtag() {
		String oldName = "old name";
		String newName = "new name";
		String etag1 = "abcd";
		String etag2 = "zyxw";

		OAuthRefreshTokenInformation newMetadata = new OAuthRefreshTokenInformation();
		newMetadata.setTokenId(TOKEN_ID);
		newMetadata.setPrincipalId(USER_ID);
		newMetadata.setName(newName);
		newMetadata.setEtag(etag1);

		OAuthRefreshTokenInformation retrievedFromDao = new OAuthRefreshTokenInformation();
		retrievedFromDao.setTokenId(TOKEN_ID);
		retrievedFromDao.setPrincipalId(USER_ID);
		retrievedFromDao.setName(oldName);
		retrievedFromDao.setEtag(etag2);


		when(mockOAuthRefreshTokenDao.getRefreshTokenMetadata(TOKEN_ID)).thenReturn(Optional.of(retrievedFromDao));

		// Call under test
		assertThrows(ConflictingUpdateException.class, () -> oauthRefreshTokenManager.updateRefreshTokenMetadata(USER_INFO, newMetadata));

		verify(mockOAuthRefreshTokenDao).getRefreshTokenMetadata(TOKEN_ID);
		verify(mockOAuthRefreshTokenDao, never()).updateRefreshTokenMetadata(any(OAuthRefreshTokenInformation.class));
	}

	@Test
	public void testIsRefreshTokenActive() {
		when(mockOAuthRefreshTokenDao.isTokenActive(TOKEN_ID, EXPECTED_LEASE_DURATION_DAYS)).thenReturn(true);

		// Call under test
		assertTrue(oauthRefreshTokenManager.isRefreshTokenActive(TOKEN_ID));
	}
}

