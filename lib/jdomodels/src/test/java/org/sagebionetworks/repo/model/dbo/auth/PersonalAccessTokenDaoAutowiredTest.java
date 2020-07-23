package org.sagebionetworks.repo.model.dbo.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.auth.AccessTokenRecord;
import org.sagebionetworks.repo.model.auth.AccessTokenRecordList;
import org.sagebionetworks.repo.model.auth.PersonalAccessTokenDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOPersonalAccessToken;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:jdomodels-test-context.xml"})
public class PersonalAccessTokenDaoAutowiredTest {

	private List<String> tokenIdsToDelete;

	private static final String userId = AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString();

	private static final UnmodifiableXStream X_STREAM = UnmodifiableXStream.builder().build();

	private static final Long ONE_HOUR_MILLIS = 1000L * 60 * 60;
	private static final Long ONE_DAY_MILLIS = ONE_HOUR_MILLIS * 24;
	private static final Long ONE_YEAR_MILLIS = ONE_DAY_MILLIS * 365;

	@Autowired
	private PersonalAccessTokenDao personalAccessTokenDao;


	@BeforeEach
	public void beforeEach() {
		tokenIdsToDelete = new ArrayList<>();
	}

	@AfterEach
	public void afterEach() {
		for (String id : tokenIdsToDelete) {
			try {
				personalAccessTokenDao.deleteToken(id);
			} catch (NotFoundException e) {
				// Ignore
			}
		}
	}

	/**
	 * Creates a DTO for testing. Does not create a record in the database, so the ID will be null.
	 * @return
	 */
	private static AccessTokenRecord createDto(String userId, Date lastUsedDate) {
		AccessTokenRecord record = new AccessTokenRecord();
		record.setUserId(userId);
		record.setName(UUID.randomUUID().toString());
		record.setScopes(Collections.singletonList(OAuthScope.view));
		record.setUserInfoClaims(new HashMap<>());
		record.setCreatedOn(new Date());
		record.setLastUsed(lastUsedDate);
		return record;
	}

	private AccessTokenRecord createTokenRecord(String userId, Date lastUsedDate) {
		AccessTokenRecord record = createDto(userId, lastUsedDate);
		record = personalAccessTokenDao.createTokenRecord(record);
		tokenIdsToDelete.add(record.getId());
		return record;
	}

	@Test
	void testDtoToDboAndBack() {
		AccessTokenRecord dto = createDto(userId, new Date());
		dto.setId("55555");

		// Call under test -- map to DBO and back
		AccessTokenRecord mapped = PersonalAccessTokenDaoImpl.personalAccessTokenDboToDto(
				PersonalAccessTokenDaoImpl.personalAccessTokenDtoToDbo(dto)
		);

		assertEquals(dto, mapped);
	}

	@Test
	void testDboToDtoAndBack() throws Exception {
		DBOPersonalAccessToken dbo = new DBOPersonalAccessToken();
		dbo.setName(UUID.randomUUID().toString());
		dbo.setId(11111L);
		dbo.setPrincipalId(Long.valueOf(userId));
		dbo.setScopes(JDOSecondaryPropertyUtils.compressObject(X_STREAM, Arrays.asList(OAuthScope.modify, OAuthScope.authorize)));
		Map<String, OIDCClaimsRequestDetails> userInfoClaims = new HashMap<>();
		OIDCClaimsRequestDetails detail = new OIDCClaimsRequestDetails();
		detail.setEssential(true);
		userInfoClaims.put("userid", detail);
		dbo.setClaims(JDOSecondaryPropertyUtils.compressObject(X_STREAM, userInfoClaims));
		dbo.setCreatedOn(new Timestamp(System.currentTimeMillis()));
		dbo.setLastUsed(new Timestamp(System.currentTimeMillis()));

		// Call under test -- map to DBO and back
		DBOPersonalAccessToken mapped = PersonalAccessTokenDaoImpl.personalAccessTokenDtoToDbo(
				PersonalAccessTokenDaoImpl.personalAccessTokenDboToDto(dbo));
		assertEquals(dbo, mapped);
	}

	@Test
	void testCreateGetDelete() {
		AccessTokenRecord record = createDto(userId, new Date());

		// method under test -- create
		AccessTokenRecord created = personalAccessTokenDao.createTokenRecord(record);

		assertNotNull(created.getId());
		record.setId(created.getId());
		assertEquals(record, created);

		// method under test -- get
		AccessTokenRecord retrieved = personalAccessTokenDao.getTokenRecord(created.getId());
		assertEquals(created, retrieved);

		// method under test -- delete
		personalAccessTokenDao.deleteToken(retrieved.getId());
		assertThrows(NotFoundException.class, () -> personalAccessTokenDao.getTokenRecord(retrieved.getId()));
	}

	@Test
	void testUpdateLastUsed() throws Exception {
		AccessTokenRecord tokenRecord = createTokenRecord(userId, new Date(System.currentTimeMillis() - ONE_HOUR_MILLIS));

		Thread.sleep(100L);

		// method under test
		personalAccessTokenDao.updateLastUsed(tokenRecord.getId());

		AccessTokenRecord updated = personalAccessTokenDao.getTokenRecord(tokenRecord.getId());
		assertTrue(updated.getLastUsed().after(tokenRecord.getLastUsed()));
	}

	@Test
	void testGetTokensPaginated() {
		// Create two token records
		AccessTokenRecord activeToken1 = createTokenRecord(userId, new Date(System.currentTimeMillis() - ONE_HOUR_MILLIS));
		AccessTokenRecord activeToken2 = createTokenRecord(userId, new Date(System.currentTimeMillis() - ONE_DAY_MILLIS));

		String firstPageNpt = new NextPageToken(1, 0).toToken();

		// Call under test -- page 1
		AccessTokenRecordList pageOne = personalAccessTokenDao.getTokenRecords(userId, firstPageNpt);
		assertEquals(1, pageOne.getResults().size());
		assertNotNull(pageOne.getNextPageToken());

		// Call under test -- page 2
		AccessTokenRecordList pageTwo = personalAccessTokenDao.getTokenRecords(userId, pageOne.getNextPageToken());
		assertEquals(1, pageTwo.getResults().size());
		assertNull(pageTwo.getNextPageToken());

		// Tokens should be returned in reverse chronological order
		assertEquals(activeToken1, pageOne.getResults().get(0));
		assertEquals(activeToken2, pageTwo.getResults().get(0));
	}


	@Test
	void testDeleteLeastRecentlyUsedTokens() {
		Long tokenLimit = 2L; // Only keep the 2 newest tokens

		// Create a bunch of tokens
		// If we keep the newest 2 tokens, these will be deleted:
		AccessTokenRecord oldToken1 = createTokenRecord(userId, new Date(System.currentTimeMillis() - ONE_YEAR_MILLIS));
		AccessTokenRecord oldToken2 = createTokenRecord(userId, new Date(System.currentTimeMillis() - ONE_DAY_MILLIS));
		// and these will remain:
		AccessTokenRecord newToken1 = createTokenRecord(userId, new Date(System.currentTimeMillis() - ONE_HOUR_MILLIS));
		AccessTokenRecord newToken2 = createTokenRecord(userId, new Date());

		// Call under test
		personalAccessTokenDao.deleteLeastRecentlyUsedTokensOverLimit(userId, tokenLimit);
		assertThrows(NotFoundException.class, () -> personalAccessTokenDao.getTokenRecord(oldToken1.getId()));
		assertThrows(NotFoundException.class, () -> personalAccessTokenDao.getTokenRecord(oldToken2.getId()));
		assertNotNull(personalAccessTokenDao.getTokenRecord(newToken1.getId()));
		assertNotNull(personalAccessTokenDao.getTokenRecord(newToken2.getId()));
	}

	@Test
	void testGetLastUsedDate() {
		AccessTokenRecord token = createTokenRecord(userId, new Date(System.currentTimeMillis() - ONE_HOUR_MILLIS));

		// method under test
		Date lastUsedDate = personalAccessTokenDao.getLastUsedDate(token.getId());

		assertEquals(token.getLastUsed(), lastUsedDate);
	}

	@Test
	void testGetLastUsedDate_NotFound() {
		// method under test
		assertThrows(NotFoundException.class, () -> personalAccessTokenDao.getLastUsedDate("999999999999"));
	}
}
