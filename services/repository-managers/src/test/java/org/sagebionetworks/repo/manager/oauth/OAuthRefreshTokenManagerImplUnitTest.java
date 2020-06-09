package org.sagebionetworks.repo.manager.oauth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.OAuthClientDao;
import org.sagebionetworks.repo.model.auth.OAuthRefreshTokenDao;
import org.sagebionetworks.repo.model.oauth.OAuthClientAuthorizationHistoryList;
import org.sagebionetworks.repo.model.oauth.OAuthRefreshTokenInformation;
import org.sagebionetworks.repo.model.oauth.OAuthRefreshTokenInformationList;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.oauth.OAuthTokenRevocationRequest;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequest;
import org.sagebionetworks.repo.model.oauth.TokenTypeHint;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.Clock;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwt;

@ExtendWith(MockitoExtension.class)
public class OAuthRefreshTokenManagerImplUnitTest {

	@Mock
	OAuthClientDao mockOAuthClientDao;

	@Mock
	OAuthRefreshTokenDao mockOAuthRefreshTokenDao;

	@Mock
	OIDCTokenHelper mockOidcTokenHelper;

	@Mock
	Clock clock;

	@InjectMocks
	OAuthRefreshTokenManagerImpl oauthRefreshTokenManager;

	@Mock
	Jwt<JwsHeader, Claims> mockJwt;

	@Mock
	Claims mockClaims;


	private static final Long EXPECTED_LEASE_DURATION = 180L;
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
		when(mockOAuthRefreshTokenDao.createRefreshToken(anyString(), any(OAuthRefreshTokenInformation.class)))
			.thenAnswer(i -> {
				OAuthRefreshTokenInformation tokenInfo = i.getArgument(1, OAuthRefreshTokenInformation.class);

				// Make sure the fields in the object the DAO receives match what should be set
				assertEquals(USER_ID, tokenInfo.getPrincipalId());
				assertEquals(CLIENT_ID, tokenInfo.getClientId());
				assertEquals(scopes, tokenInfo.getScopes());
				assertEquals(claimsRequest, tokenInfo.getClaims());
				assertTrue(StringUtils.isNotBlank(tokenInfo.getName()));
				assertTrue(StringUtils.isNotBlank(tokenInfo.getEtag()));
				assertEquals(clock.now(), tokenInfo.getLastUsed());
				assertEquals(clock.now(), tokenInfo.getAuthorizedOn());
				assertEquals(clock.now(), tokenInfo.getModifiedOn());

				tokenInfo.setTokenId(TOKEN_ID); // The DAO will set an ID
				return tokenInfo;
			});


		// Call under test
		OAuthRefreshTokenAndId actual = oauthRefreshTokenManager.createRefreshToken(USER_ID, CLIENT_ID, scopes, claimsRequest);
		assertNotNull(actual);
		assertEquals(TOKEN_ID, actual.getTokenId());
		assertTrue(StringUtils.isNotBlank(actual.getRefreshToken()));

		verify(mockOAuthRefreshTokenDao).deleteLeastRecentlyUsedTokensOverLimit(USER_ID, CLIENT_ID, EXPECTED_MAX_REFRESH_TOKENS);
		verify(mockOAuthRefreshTokenDao).createRefreshToken(anyString(), any(OAuthRefreshTokenInformation.class));
	}

	@Test
	public void testGetTokenMetadataForUpdate() {
		String token = "token val";

		OAuthRefreshTokenInformation expected = new OAuthRefreshTokenInformation();
		expected.setClientId(CLIENT_ID);

		when(mockOAuthRefreshTokenDao.getMatchingTokenByHashForUpdate(DigestUtils.sha256Hex(token), CLIENT_ID)).thenReturn(Optional.of(expected));

		// Call under test
		Optional<OAuthRefreshTokenInformation> actual = oauthRefreshTokenManager.getRefreshTokenMetadataForUpdate(token, CLIENT_ID);

		assertEquals(expected, actual.get());
		verify(mockOAuthRefreshTokenDao).getMatchingTokenByHashForUpdate(DigestUtils.sha256Hex(token), CLIENT_ID);
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
	public void testGetClientHistory() {
		String nextPageToken = "initial npt";

		OAuthClientAuthorizationHistoryList expected = new OAuthClientAuthorizationHistoryList();
		expected.setResults(Collections.emptyList());
		expected.setNextPageToken("some new nextpagetoken");

		when(mockOAuthClientDao.getAuthorizedClientHistory(USER_ID, nextPageToken, EXPECTED_LEASE_DURATION)).thenReturn(expected);

		// Call under test
		OAuthClientAuthorizationHistoryList actual = oauthRefreshTokenManager.getAuthorizedClientHistory(USER_INFO, nextPageToken);

		assertEquals(expected, actual);
		verify(mockOAuthClientDao).getAuthorizedClientHistory(USER_ID, nextPageToken, EXPECTED_LEASE_DURATION);
	}

	@Test
	public void testGetActiveRefreshTokensMetadata() {
		String nextPageToken = "initial npt";

		OAuthRefreshTokenInformationList expected = new OAuthRefreshTokenInformationList();
		expected.setResults(Collections.emptyList());
		expected.setNextPageToken("some new nextpagetoken");

		when(mockOAuthRefreshTokenDao.getActiveTokenInformation(USER_ID, CLIENT_ID, nextPageToken, EXPECTED_LEASE_DURATION)).thenReturn(expected);

		// Call under test
		OAuthRefreshTokenInformationList actual = oauthRefreshTokenManager.getMetadataForActiveRefreshTokens(USER_INFO, CLIENT_ID, nextPageToken);

		assertEquals(expected, actual);
		verify(mockOAuthRefreshTokenDao).getActiveTokenInformation(USER_ID, CLIENT_ID, nextPageToken, EXPECTED_LEASE_DURATION);
	}

	@Test
	public void testRevokeTokensForUserClientPair() {
		OAuthRefreshTokenInformationList existingTokens = new OAuthRefreshTokenInformationList();
		existingTokens.setResults(Collections.singletonList(new OAuthRefreshTokenInformation())); // need a list of length >= 1

		when(mockOAuthRefreshTokenDao.getActiveTokenInformation(USER_ID, CLIENT_ID, null, EXPECTED_LEASE_DURATION)).thenReturn(existingTokens);

		// Call under test
		oauthRefreshTokenManager.revokeRefreshTokensForUserClientPair(USER_INFO, CLIENT_ID);

		verify(mockOAuthRefreshTokenDao).getActiveTokenInformation(USER_ID, CLIENT_ID, null, EXPECTED_LEASE_DURATION);
		verify(mockOAuthRefreshTokenDao).deleteAllTokensForUserClientPair(USER_ID, CLIENT_ID);
	}

	@Test
	public void testRevokeTokensForUserClientPair_NotFound() {
		OAuthRefreshTokenInformationList existingTokens = new OAuthRefreshTokenInformationList();
		existingTokens.setResults(Collections.emptyList()); // No tokens exist

		when(mockOAuthRefreshTokenDao.getActiveTokenInformation(USER_ID, CLIENT_ID, null, EXPECTED_LEASE_DURATION)).thenReturn(existingTokens);

		// Call under test
		assertThrows(NotFoundException.class, () ->	oauthRefreshTokenManager.revokeRefreshTokensForUserClientPair(USER_INFO, CLIENT_ID));

		verify(mockOAuthRefreshTokenDao).getActiveTokenInformation(USER_ID, CLIENT_ID, null, EXPECTED_LEASE_DURATION);
		verify(mockOAuthRefreshTokenDao, never()).deleteAllTokensForUserClientPair(USER_ID, CLIENT_ID);
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
	public void testRotateRefreshToken() {
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

		when(mockOAuthRefreshTokenDao.getRefreshTokenMetadata(TOKEN_ID)).thenReturn(Optional.of(existingToken));

		doAnswer(invocation -> {
			// Verify that a hashed token was generated and passed
			String hashedToken = invocation.getArgument(1, String.class);
			assertTrue(StringUtils.isNotBlank(hashedToken));

			// Verify that the token was modified
			OAuthRefreshTokenInformation tokenInfo = invocation.getArgument(0, OAuthRefreshTokenInformation.class);
			assertEquals(TOKEN_ID, tokenInfo.getTokenId());
			assertEquals(USER_ID, tokenInfo.getPrincipalId());
			assertEquals(clock.now(), tokenInfo.getLastUsed());
			assertEquals(oldDate, tokenInfo.getModifiedOn());
			assertNotEquals(oldEtag, tokenInfo.getEtag());
			return null;
		}).when(mockOAuthRefreshTokenDao).updateTokenHash(any(OAuthRefreshTokenInformation.class), anyString());

		// Call under test
		String newToken = oauthRefreshTokenManager.rotateRefreshToken(TOKEN_ID);

		assertTrue(StringUtils.isNotBlank(newToken));

		verify(mockOAuthRefreshTokenDao).getRefreshTokenMetadata(TOKEN_ID);
		verify(mockOAuthRefreshTokenDao).updateTokenHash(any(OAuthRefreshTokenInformation.class), anyString());
	}

	@Test
	public void testRotateRefreshToken_IllegalState() {
		when(mockOAuthRefreshTokenDao.getRefreshTokenMetadata(TOKEN_ID)).thenReturn(Optional.empty());

		// Call under test
		assertThrows(IllegalStateException.class, () -> oauthRefreshTokenManager.rotateRefreshToken(TOKEN_ID));
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

		doAnswer(invocation -> {
			// Verify that the fields were updated by the manager when passed to DAO
			OAuthRefreshTokenInformation toUpdate = invocation.getArgument(0, OAuthRefreshTokenInformation.class);
			assertNotNull(toUpdate);

			assertNotEquals(oldEtag, toUpdate.getEtag());
			assertEquals(newName, toUpdate.getName());
			assertEquals(clock.now(), toUpdate.getModifiedOn());
			return null;
		}).when(mockOAuthRefreshTokenDao).updateRefreshTokenMetadata(any());

		// Call under test
		OAuthRefreshTokenInformation updated = oauthRefreshTokenManager.updateRefreshTokenMetadata(USER_INFO, newMetadata);

		// Just check that the updated object came from the DAO.getRefreshTokenMetadata method. The fields won't be updated because it's a mock
		assertEquals(updated, retrievedFromDao);

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
	public void testRevokeRefreshTokenWithAccessToken() {
		String accessToken = "this would be a signed JWT string";
		OAuthTokenRevocationRequest revocationRequest = new OAuthTokenRevocationRequest();
		revocationRequest.setToken(accessToken);
		revocationRequest.setToken_type_hint(TokenTypeHint.access_token);

		when(mockOidcTokenHelper.parseJWT(accessToken)).thenReturn(mockJwt);
		when(mockJwt.getBody()).thenReturn(mockClaims);
		when(mockClaims.get(OIDCClaimName.refresh_token_id.name(), String.class)).thenReturn(TOKEN_ID);

		// Call under test
		oauthRefreshTokenManager.revokeRefreshToken(CLIENT_ID, revocationRequest);

		verify(mockOidcTokenHelper).parseJWT(accessToken);
		verify(mockOAuthRefreshTokenDao).deleteToken(TOKEN_ID);
	}

	@Test
	public void testRevokeRefreshTokenWithAccessToken_NoRefreshTokenId() {
		String accessToken = "this would be a signed JWT string";
		OAuthTokenRevocationRequest revocationRequest = new OAuthTokenRevocationRequest();
		revocationRequest.setToken(accessToken);
		revocationRequest.setToken_type_hint(TokenTypeHint.access_token);

		when(mockOidcTokenHelper.parseJWT(accessToken)).thenReturn(mockJwt);
		when(mockJwt.getBody()).thenReturn(mockClaims);
		when(mockClaims.get(OIDCClaimName.refresh_token_id.name(), String.class)).thenReturn(null);

		// Call under test
		assertThrows(IllegalArgumentException.class, () -> oauthRefreshTokenManager.revokeRefreshToken(CLIENT_ID, revocationRequest));

		verify(mockOidcTokenHelper).parseJWT(accessToken);
		verify(mockOAuthRefreshTokenDao, never()).deleteToken(anyString());
	}


	@Test
	public void testRevokeRefreshTokenWithRefreshToken() {
		String refreshToken = "this would be an opaque string";
		OAuthTokenRevocationRequest revocationRequest = new OAuthTokenRevocationRequest();
		revocationRequest.setToken(refreshToken);
		revocationRequest.setToken_type_hint(TokenTypeHint.refresh_token);

		OAuthRefreshTokenInformation retrievedMetadata = new OAuthRefreshTokenInformation();
		retrievedMetadata.setTokenId(TOKEN_ID);

		when(mockOAuthRefreshTokenDao.getMatchingTokenByHashForUpdate(anyString(), eq(CLIENT_ID))).thenReturn(Optional.of(retrievedMetadata));

		// Call under test
		oauthRefreshTokenManager.revokeRefreshToken(CLIENT_ID, revocationRequest);

		verify(mockOAuthRefreshTokenDao).getMatchingTokenByHashForUpdate(anyString(), eq(CLIENT_ID));
		verify(mockOidcTokenHelper, never()).parseJWT(anyString());
		verify(mockOAuthRefreshTokenDao).deleteToken(TOKEN_ID);

	}

	@Test
	public void testRevokeRefreshTokenWithRefreshToken_NotFound() {
		String refreshToken = "this would be an opaque string";
		OAuthTokenRevocationRequest revocationRequest = new OAuthTokenRevocationRequest();
		revocationRequest.setToken(refreshToken);
		revocationRequest.setToken_type_hint(TokenTypeHint.refresh_token);

		when(mockOAuthRefreshTokenDao.getMatchingTokenByHashForUpdate(anyString(), eq(CLIENT_ID))).thenReturn(Optional.empty());

		// Call under test
		assertThrows(NotFoundException.class, () -> oauthRefreshTokenManager.revokeRefreshToken(CLIENT_ID, revocationRequest));

		verify(mockOAuthRefreshTokenDao).getMatchingTokenByHashForUpdate(anyString(), eq(CLIENT_ID));
		verify(mockOidcTokenHelper, never()).parseJWT(anyString());
		verify(mockOAuthRefreshTokenDao, never()).deleteToken(anyString());

	}


	@Test
	public void testRevokeRefreshTokenWithUnknownHint() {
		OAuthTokenRevocationRequest revocationRequest = new OAuthTokenRevocationRequest();
		revocationRequest.setToken("token");

		// This test will fail if a new token type hint is added without a branch in the method under test.

		for (TokenTypeHint hint : TokenTypeHint.values()) {
			revocationRequest.setToken_type_hint(hint);
			try {
				oauthRefreshTokenManager.revokeRefreshToken(CLIENT_ID, revocationRequest);
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
}

