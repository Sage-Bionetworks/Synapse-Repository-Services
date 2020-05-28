package org.sagebionetworks.repo.model.dbo.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.auth.OAuthClientDao;
import org.sagebionetworks.repo.model.auth.OAuthRefreshTokenDao;
import org.sagebionetworks.repo.model.auth.SectorIdentifier;
import org.sagebionetworks.repo.model.dbo.persistence.DBOOAuthRefreshToken;
import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.repo.model.oauth.OAuthRefreshTokenInformation;
import org.sagebionetworks.repo.model.oauth.OAuthRefreshTokenInformationList;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Transactional;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:jdomodels-test-context.xml"})
public class OAuthRefreshTokenDaoAutowiredTest {

	private List<String> tokenIdsToDelete;
	private List<String> clientIdsToDelete;
	private List<String> sectorIdentifiersToDelete;
	private OAuthClient client;

	private static final String CLIENT_NAME = "An OAuth 2 Client";
	private static final String SECTOR_IDENTIFIER = "https://foo.bar";
	private static final String SECTOR_IDENTIFIER_URI = "https://client.uri.com/path/to/json/file";

	private static final String userId = AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString();

	private static final Long ONE_HOUR_MILLIS = 1000L * 60 * 60;
	private static final Long ONE_DAY_MILLIS = ONE_HOUR_MILLIS * 24;
	private static final Long ONE_YEAR_MILLIS = ONE_DAY_MILLIS * 365;

	@Autowired
	private OAuthRefreshTokenDao oauthRefreshTokenDao;

	@Autowired
	private OAuthClientDao oauthClientDao;

	private OAuthClient createClient(String clientName, String sectorIdentifierUri) {
		SectorIdentifier sectorIdentifier = new SectorIdentifier();
		sectorIdentifier.setSectorIdentifierUri(sectorIdentifierUri);
		sectorIdentifier.setSecret(UUID.randomUUID().toString());
		sectorIdentifier.setCreatedBy(Long.valueOf(userId));
		sectorIdentifier.setCreatedOn((new Date()).getTime());
		oauthClientDao.createSectorIdentifier(sectorIdentifier);


		OAuthClient client = new OAuthClient();
		client.setClient_name(clientName);
		client.setCreatedBy(userId);
		client.setSector_identifier(sectorIdentifierUri);
		client.setSector_identifier_uri(SECTOR_IDENTIFIER_URI);
		client.setVerified(true);
		client.setEtag(UUID.randomUUID().toString());
		client = oauthClientDao.createOAuthClient(client);

		clientIdsToDelete.add(client.getClient_id());
		sectorIdentifiersToDelete.add(sectorIdentifierUri);
		return client;
	}

	@BeforeEach
	public void beforeEach() {
		tokenIdsToDelete = new ArrayList<>();
		clientIdsToDelete = new ArrayList<>();
		sectorIdentifiersToDelete = new ArrayList<>();

		client = createClient(CLIENT_NAME, SECTOR_IDENTIFIER);

		assertNotNull(oauthRefreshTokenDao);
	}

	@AfterEach
	public void afterEach() {
		for (String id : clientIdsToDelete) {
			try {
				oauthClientDao.deleteOAuthClient(id);
			} catch (NotFoundException e) {
				// Ignore
			}
		}
		for (String sectorIdentifier : sectorIdentifiersToDelete) {
			try {
				oauthClientDao.deleteSectorIdentifer(sectorIdentifier);
			} catch (NotFoundException e) {
				// Ignore
				}
		}
		for (String id : tokenIdsToDelete) {
			try {
				oauthRefreshTokenDao.deleteToken(id);
			} catch (NotFoundException e) {
				// Ignore
			}
		}
	}

	private OAuthRefreshTokenInformation createRefreshToken(String hash, Date lastUsedDate, String clientId, String userId) {
		OAuthRefreshTokenInformation metadata = new OAuthRefreshTokenInformation();
		metadata.setName(UUID.randomUUID().toString());
		metadata.setPrincipalId(userId);
		metadata.setClientId(clientId);
		metadata.setScopes(Collections.singletonList(OAuthScope.view));
		metadata.setModifiedOn(new Date());
		metadata.setLastUsed(lastUsedDate);
		metadata.setAuthorizedOn(new Date());
		metadata.setEtag(UUID.randomUUID().toString());

		OAuthRefreshTokenInformation token = oauthRefreshTokenDao.createRefreshToken(hash, metadata);
		tokenIdsToDelete.add(token.getTokenId());
		return token;
	}

	private OAuthRefreshTokenInformation createRefreshToken(String clientId, String userId) {
		return createRefreshToken("abcdef", new Date(), clientId, userId);
	}

	private OAuthRefreshTokenInformation createRefreshToken(String hash, Date lastUsedDate) {
		return createRefreshToken(hash, lastUsedDate, client.getClient_id(), userId);
	}

	private OAuthRefreshTokenInformation createRefreshToken(Date lastUsedDate) {
		return createRefreshToken("abcdef", lastUsedDate);
	}

	private OAuthRefreshTokenInformation createRefreshToken() {
		return createRefreshToken(new Date());
	}


	@Test
	void testDtoToDbo() {
		OAuthRefreshTokenInformation dto = new OAuthRefreshTokenInformation();
		dto.setName(UUID.randomUUID().toString());
		dto.setTokenId("111111");
		dto.setScopes(Arrays.asList(OAuthScope.modify, OAuthScope.authorize));
		dto.setClientId("888888");
		dto.setPrincipalId("999999");
		dto.setModifiedOn(new Date());
		dto.setAuthorizedOn(new Date());
		dto.setLastUsed(new Date());
		dto.setEtag(UUID.randomUUID().toString());

		// Call under test -- map to DBO and back
		OAuthRefreshTokenInformation mapped = OAuthRefreshTokenDaoImpl.refreshTokenDboToDto(
				OAuthRefreshTokenDaoImpl.refreshTokenDtoToDbo(dto)
		);

		assertEquals(dto, mapped);
	}

	@Test
	void testDboToDto() {
		DBOOAuthRefreshToken dbo = new DBOOAuthRefreshToken();
		dbo.setName(UUID.randomUUID().toString());
		dbo.setId(11111L);
		dbo.setScopes(Arrays.asList(OAuthScope.modify, OAuthScope.authorize));
		dbo.setClientId(888888L);
		dbo.setPrincipalId(999999L);
		dbo.setModifiedOn(new Timestamp(System.currentTimeMillis()));
		dbo.setCreatedOn(new Timestamp(System.currentTimeMillis()));
		dbo.setLastUsed(new Timestamp(System.currentTimeMillis()));
		dbo.setEtag(UUID.randomUUID().toString());
		// The hash will be dropped, it isn't in the DTO
		dbo.setTokenHash("abcdef");

		// Call under test -- map to DBO and back
		DBOOAuthRefreshToken mapped = OAuthRefreshTokenDaoImpl.refreshTokenDtoToDbo(
				OAuthRefreshTokenDaoImpl.refreshTokenDboToDto(dbo)
		);
		dbo.setTokenHash(null);
		assertEquals(dbo, mapped);
	}

	@Test
	void testRefreshTokenCRUD() {
		String hash = "abcdef";
		OAuthRefreshTokenInformation metadata = new OAuthRefreshTokenInformation();
		metadata.setName(UUID.randomUUID().toString());
		metadata.setPrincipalId(userId);
		metadata.setClientId(client.getClient_id());
		metadata.setScopes(Arrays.asList(OAuthScope.view, OAuthScope.download));
		metadata.setModifiedOn(new Date());
		metadata.setLastUsed(new Date());
		metadata.setAuthorizedOn(new Date());
		metadata.setEtag(UUID.randomUUID().toString());

		// Call under test -- Create
		OAuthRefreshTokenInformation createResult = oauthRefreshTokenDao.createRefreshToken(hash, metadata);
		assertNotNull(createResult.getTokenId());
		metadata.setTokenId(createResult.getTokenId());
		assertEquals(metadata, createResult);

		String newTokenName = "A new token name";
		metadata.setName(newTokenName);
		// Call under test -- Update
		oauthRefreshTokenDao.updateRefreshTokenMetadata(metadata);
		// Call under test -- Get
		Optional<OAuthRefreshTokenInformation> updated = oauthRefreshTokenDao.getRefreshTokenMetadata(metadata.getTokenId());
		assertTrue(updated.isPresent());
		assertEquals(metadata, updated.get());

		// Call under test -- Delete
		oauthRefreshTokenDao.deleteToken(metadata.getTokenId());
		assertFalse(oauthRefreshTokenDao.getRefreshTokenMetadata(metadata.getTokenId()).isPresent());
	}

	@Test
	void getActiveTokenInformation() {
		// Create 2 active, 1 expired refresh tokens between the client and user
		OAuthRefreshTokenInformation activeToken1 = createRefreshToken(new Date(System.currentTimeMillis() - ONE_HOUR_MILLIS));
		OAuthRefreshTokenInformation activeToken2 = createRefreshToken(new Date(System.currentTimeMillis() - ONE_DAY_MILLIS));
		OAuthRefreshTokenInformation expiredToken = createRefreshToken(new Date(System.currentTimeMillis() - ONE_YEAR_MILLIS));

		// Call under test
		OAuthRefreshTokenInformationList retrieved = oauthRefreshTokenDao.getActiveTokenInformation(userId, client.getClient_id(), null, 180L);
		assertEquals(2, retrieved.getResults().size());
		// Should be in reverse chronological order always
		assertEquals(activeToken1, retrieved.getResults().get(0));
		assertEquals(activeToken2, retrieved.getResults().get(1));
		assertNull(retrieved.getNextPageToken());
	}

	@Transactional
	@Test
	void getMatchingTokenByHash() {
		String hashValue = "matching hash";
		OAuthRefreshTokenInformation expected = createRefreshToken(hashValue, new Date());

		// call under test
		Optional<OAuthRefreshTokenInformation> actual = oauthRefreshTokenDao.getMatchingTokenByHashForUpdate(hashValue, client.getClient_id());
		assertEquals(expected, actual.get());
	}

	@Test
	void getMatchingTokenByHashForUpdate_noTransaction() {
		assertThrows(IllegalTransactionStateException.class,() ->
				oauthRefreshTokenDao.getMatchingTokenByHashForUpdate("hash", "clientid")

		);
	}

	@Transactional
	@Test
	void updateTokenHash() {
		String oldHash = "old hash";
		String newHash = "new hash";
		OAuthRefreshTokenInformation metadata = createRefreshToken(oldHash, new Date());

		// call under test
		oauthRefreshTokenDao.updateTokenHash(metadata.getTokenId(), newHash);

		OAuthRefreshTokenInformation updated = oauthRefreshTokenDao.getMatchingTokenByHashForUpdate(newHash, client.getClient_id()).get();
		assertEquals(metadata.getTokenId(), updated.getTokenId());
		assertNotNull(updated);
	}

	@Test
	void updateTokenHash_noTransaction() {
		assertThrows(IllegalTransactionStateException.class,() ->
				oauthRefreshTokenDao.updateTokenHash("hash", "clientid")

		);
	}

	@Test
	void deleteTokensForUserClientPair() {
		// Create a new client to verify that tokens belonging to other clients are not deleted
		OAuthClient client2 = createClient(UUID.randomUUID().toString(), "https://baz.bat");
		// We will create tokens for a second user to verify that tokens for other users are not deleted
		final String OTHER_USER_ID = AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId().toString();

		OAuthRefreshTokenInformation tokenToRevoke1 = createRefreshToken();
		OAuthRefreshTokenInformation tokenToRevoke2 = createRefreshToken();
		// Belongs to same user, different client:
		OAuthRefreshTokenInformation tokenToNotRevoke1 = createRefreshToken(client2.getClient_id(), userId);
		// Belongs to different user, same client:
		OAuthRefreshTokenInformation tokenToNotRevoke2 = createRefreshToken(client.getClient_id(), OTHER_USER_ID);

		// Call under test
		oauthRefreshTokenDao.deleteAllTokensForUserClientPair(userId, client.getClient_id());

		// Verify that the tokens to revoke are gone
		assertFalse(oauthRefreshTokenDao.getRefreshTokenMetadata(tokenToRevoke1.getTokenId()).isPresent());
		assertFalse(oauthRefreshTokenDao.getRefreshTokenMetadata(tokenToRevoke2.getTokenId()).isPresent());
		// Verify that the other tokens are still present
		assertTrue(oauthRefreshTokenDao.getRefreshTokenMetadata(tokenToNotRevoke1.getTokenId()).isPresent());
		assertTrue(oauthRefreshTokenDao.getRefreshTokenMetadata(tokenToNotRevoke2.getTokenId()).isPresent());
	}

}
