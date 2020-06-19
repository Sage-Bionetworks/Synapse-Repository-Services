package org.sagebionetworks.repo.model.dbo.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.auth.OAuthClientDao;
import org.sagebionetworks.repo.model.auth.OAuthRefreshTokenDao;
import org.sagebionetworks.repo.model.auth.SectorIdentifier;
import org.sagebionetworks.repo.model.dbo.persistence.DBOOAuthRefreshToken;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.repo.model.oauth.OAuthRefreshTokenInformation;
import org.sagebionetworks.repo.model.oauth.OAuthRefreshTokenInformationList;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequest;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;
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

	private static final UnmodifiableXStream X_STREAM = UnmodifiableXStream.builder().build();

	private static final Long ONE_HOUR_MILLIS = 1000L * 60 * 60;
	private static final Long ONE_DAY_MILLIS = ONE_HOUR_MILLIS * 24;
	private static final Long ONE_YEAR_MILLIS = ONE_DAY_MILLIS * 365;
	private static final Long HALF_YEAR_DAYS = 180L;

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
		metadata.setClaims(new OIDCClaimsRequest());
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
	void testDtoToDboAndBack() {
		OAuthRefreshTokenInformation dto = new OAuthRefreshTokenInformation();
		dto.setName(UUID.randomUUID().toString());
		dto.setTokenId("111111");
		dto.setScopes(Arrays.asList(OAuthScope.modify, OAuthScope.authorize));

		// Set up arbitrary claims
		Map<String, OIDCClaimsRequestDetails> userinfoClaims = new HashMap<>();
		Map<String, OIDCClaimsRequestDetails> idTokenClaims = new HashMap<>();

		OIDCClaimsRequestDetails userNameClaimDetail = new OIDCClaimsRequestDetails();
		userNameClaimDetail.setEssential(true);
		userinfoClaims.put(OIDCClaimName.user_name.name(), userNameClaimDetail);

		OIDCClaimsRequestDetails teamsClaimDetail = new OIDCClaimsRequestDetails();
		teamsClaimDetail.setEssential(false);
		teamsClaimDetail.setValues(Arrays.asList("4214124", "325325"));
		idTokenClaims.put(OIDCClaimName.team.name(), teamsClaimDetail);

		OIDCClaimsRequest claims = new OIDCClaimsRequest();
		claims.setUserinfo(userinfoClaims);
		claims.setId_token(idTokenClaims);

		dto.setClaims(claims);

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
	void testDboToDtoAndBack() throws Exception {
		DBOOAuthRefreshToken dbo = new DBOOAuthRefreshToken();
		dbo.setName(UUID.randomUUID().toString());
		dbo.setId(11111L);
		dbo.setScopes(JDOSecondaryPropertyUtils.compressObject(X_STREAM, Arrays.asList(OAuthScope.modify, OAuthScope.authorize)));
		Map<String, OIDCClaimsRequestDetails> idTokenClaims = new HashMap<>();
		OIDCClaimsRequestDetails detail = new OIDCClaimsRequestDetails();
		detail.setEssential(true);
		idTokenClaims.put(OIDCClaimName.userid.name(), detail);
		OIDCClaimsRequest claims = new OIDCClaimsRequest();
		claims.setId_token(idTokenClaims);
		dbo.setClaims(JDOSecondaryPropertyUtils.compressObject(X_STREAM, claims));
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
		metadata.setClaims(new OIDCClaimsRequest());
		metadata.setModifiedOn(new Date());
		metadata.setLastUsed(new Date());
		metadata.setAuthorizedOn(new Date());
		metadata.setEtag(UUID.randomUUID().toString());

		// Call under test -- Create
		OAuthRefreshTokenInformation createResult = oauthRefreshTokenDao.createRefreshToken(hash, metadata);
		assertNotNull(createResult.getTokenId());
		metadata.setTokenId(createResult.getTokenId());
		assertEquals(metadata, createResult);

		// Update fields
		String newTokenName = "A new token name";
		String newEtag = UUID.randomUUID().toString();
		Date newModifiedOn = new Date(System.currentTimeMillis() + 1000 * 60 * 60);
		metadata.setName(newTokenName);
		metadata.setEtag(newEtag);
		metadata.setModifiedOn(newModifiedOn);
		// Try to update a field that can't be updated
		metadata.setAuthorizedOn(new Date(System.currentTimeMillis() - 1000 * 60 * 60));
		// Call under test -- Update
		oauthRefreshTokenDao.updateRefreshTokenMetadata(metadata);
		// Call under test -- Get
		Optional<OAuthRefreshTokenInformation> updated = oauthRefreshTokenDao.getRefreshTokenMetadata(metadata.getTokenId());
		assertTrue(updated.isPresent());
		// Authorized on shouldn't be updated
		assertNotEquals(metadata, updated.get().getAuthorizedOn());
		metadata.setAuthorizedOn(updated.get().getAuthorizedOn());
		// Other fields should be updated
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
		OAuthRefreshTokenInformationList retrieved = oauthRefreshTokenDao.getActiveTokenInformation(userId, client.getClient_id(), null, HALF_YEAR_DAYS);
		assertEquals(2, retrieved.getResults().size());
		// Tokens should be returned in reverse chronological order
		assertEquals(activeToken1, retrieved.getResults().get(0));
		assertEquals(activeToken2, retrieved.getResults().get(1));
		assertNull(retrieved.getNextPageToken());
	}

	@Test
	void getActiveTokenInformation_pagination() {
		// Create 2 active refresh tokens between the client and user
		OAuthRefreshTokenInformation activeToken1 = createRefreshToken(new Date(System.currentTimeMillis() - ONE_HOUR_MILLIS));
		OAuthRefreshTokenInformation activeToken2 = createRefreshToken(new Date(System.currentTimeMillis() - ONE_DAY_MILLIS));

		String firstPageNpt = new NextPageToken(1, 0).toToken();

		// Call under test -- page 1
		OAuthRefreshTokenInformationList pageOne = oauthRefreshTokenDao.getActiveTokenInformation(userId, client.getClient_id(), firstPageNpt, HALF_YEAR_DAYS);
		assertEquals(1, pageOne.getResults().size());
		assertNotNull(pageOne.getNextPageToken());

		// Call under test -- page 2
		OAuthRefreshTokenInformationList pageTwo = oauthRefreshTokenDao.getActiveTokenInformation(userId, client.getClient_id(), pageOne.getNextPageToken(), HALF_YEAR_DAYS);
		assertEquals(1, pageTwo.getResults().size());
		assertNull(pageTwo.getNextPageToken());

		// Tokens should be returned in reverse chronological order
		assertEquals(activeToken1, pageOne.getResults().get(0));
		assertEquals(activeToken2, pageTwo.getResults().get(0));
	}

	@Test
	void getMatchingTokenByHash() {
		String hashValue = "matching hash";
		OAuthRefreshTokenInformation expected = createRefreshToken(hashValue, new Date());

		// call under test
		Optional<OAuthRefreshTokenInformation> actual = oauthRefreshTokenDao.getMatchingTokenByHash(hashValue);
		assertEquals(expected, actual.get());
	}

	@Test
	void getMatchingTokenByHash_EmptyResult() {
		String hashValue = "hash";
		createRefreshToken(hashValue, new Date());

		// call under test
		Optional<OAuthRefreshTokenInformation> actual = oauthRefreshTokenDao.getMatchingTokenByHash("value that doesn't match");
		assertFalse(actual.isPresent());
	}

	@Transactional
	@Test
	void getMatchingTokenByHashForUpdate() {
		String hashValue = "matching hash";
		OAuthRefreshTokenInformation expected = createRefreshToken(hashValue, new Date());

		// call under test
		Optional<OAuthRefreshTokenInformation> actual = oauthRefreshTokenDao.getMatchingTokenByHashForUpdate(hashValue);
		assertEquals(expected, actual.get());
	}

	@Transactional
	@Test
	void getMatchingTokenByHashForUpdate_EmptyResult() {
		String hashValue = "hash";
		createRefreshToken(hashValue, new Date());

		// call under test
		Optional<OAuthRefreshTokenInformation> actual = oauthRefreshTokenDao.getMatchingTokenByHashForUpdate("value that doesn't match");
		assertFalse(actual.isPresent());
	}

	@Test
	void getMatchingTokenByHashForUpdate_noTransaction() {
		assertThrows(IllegalTransactionStateException.class, () ->
				oauthRefreshTokenDao.getMatchingTokenByHashForUpdate("hash")
		);
	}

	@Transactional
	@Test
	void updateTokenHash() {
		String oldHash = "old hash";
		String newHash = "new hash";
		OAuthRefreshTokenInformation metadata = createRefreshToken(oldHash, new Date());

		// Update the metadata that should update
		String newEtag = UUID.randomUUID().toString();
		Date newLastUsedDate = new Date(System.currentTimeMillis() + ONE_DAY_MILLIS);
		metadata.setEtag(newEtag);
		metadata.setLastUsed(newLastUsedDate);

		// Fields like name should not update when updating the hash
		String newName = UUID.randomUUID().toString();
		metadata.setName(newName);

		// call under test
		oauthRefreshTokenDao.updateTokenHash(metadata, newHash);

		OAuthRefreshTokenInformation updated = oauthRefreshTokenDao.getMatchingTokenByHashForUpdate(newHash).get();
		assertEquals(metadata.getTokenId(), updated.getTokenId());
		assertNotNull(updated);
		assertEquals(newEtag, updated.getEtag());
		assertEquals(newLastUsedDate, updated.getLastUsed());
		assertNotEquals(newName, updated.getName()); // NOT equals
	}

	@Test
	void updateTokenHash_noTransaction() {
		assertThrows(IllegalTransactionStateException.class, () ->
				oauthRefreshTokenDao.updateTokenHash(new OAuthRefreshTokenInformation(), "clientid")

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

	@Test
	void deleteLeastRecentlyUsedActiveTokens() {
		Long tokenLimit = 2L; // Only keep the 2 newest tokens

		// Create a bunch of tokens
		// If we keep the newest 2 tokens, these will be deleted:
		OAuthRefreshTokenInformation oldToken1 = createRefreshToken("abcd", new Date(System.currentTimeMillis() - ONE_YEAR_MILLIS));
		OAuthRefreshTokenInformation oldToken2 = createRefreshToken("abcd", new Date(System.currentTimeMillis() - ONE_DAY_MILLIS));
		// and these will remain:
		OAuthRefreshTokenInformation newToken1 = createRefreshToken("abcd", new Date(System.currentTimeMillis() - ONE_HOUR_MILLIS));
		OAuthRefreshTokenInformation newToken2 = createRefreshToken("abcd", new Date());

		// Call under test'
		oauthRefreshTokenDao.deleteLeastRecentlyUsedTokensOverLimit(userId, client.getClient_id(), tokenLimit);
		assertFalse(oauthRefreshTokenDao.getRefreshTokenMetadata(oldToken1.getTokenId()).isPresent());
		assertFalse(oauthRefreshTokenDao.getRefreshTokenMetadata(oldToken2.getTokenId()).isPresent());
		assertTrue(oauthRefreshTokenDao.getRefreshTokenMetadata(newToken1.getTokenId()).isPresent());
		assertTrue(oauthRefreshTokenDao.getRefreshTokenMetadata(newToken2.getTokenId()).isPresent());
	}

	@Test
	public void testIsTokenActive() {
		OAuthRefreshTokenInformation token = createRefreshToken(new Date(System.currentTimeMillis()));
		// Call under test
		assertTrue(oauthRefreshTokenDao.isTokenActive(token.getTokenId(), HALF_YEAR_DAYS));
	}

	@Test
	public void testIsTokenActive_deleted() {
		OAuthRefreshTokenInformation token = createRefreshToken(new Date(System.currentTimeMillis()));
		oauthRefreshTokenDao.deleteToken(token.getTokenId());
		// Call under test
		assertFalse(oauthRefreshTokenDao.isTokenActive(token.getTokenId(), HALF_YEAR_DAYS));
	}

	@Test
	public void testIsTokenActive_expired() {
		// Create token last used one year ago
		OAuthRefreshTokenInformation token = createRefreshToken(new Date(System.currentTimeMillis() - ONE_YEAR_MILLIS));
		// Call under test
		assertFalse(oauthRefreshTokenDao.isTokenActive(token.getTokenId(), HALF_YEAR_DAYS));
	}
}
