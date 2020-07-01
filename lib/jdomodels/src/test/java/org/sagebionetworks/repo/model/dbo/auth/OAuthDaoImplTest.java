package org.sagebionetworks.repo.model.dbo.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AUTHORIZATION_CONSENT_CLIENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AUTHORIZATION_CONSENT_SCOPE_HASH;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AUTHORIZATION_CONSENT_USER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_AUTHORIZATION_CONSENT;

import java.util.Date;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.auth.OAuthClientDao;
import org.sagebionetworks.repo.model.auth.OAuthDao;
import org.sagebionetworks.repo.model.auth.SectorIdentifier;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAuthorizationConsent;
import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
class OAuthDaoImplTest {
	private static final String SECTOR_IDENTIFIER_ENCRYPTION_SECRET ="sector identifier secret";
	private static final String CLIENT_NAME = "Third paty app";
	private static final String SECTOR_IDENTIFIER = "https://foo.bar";
	
	@Autowired
	private OAuthDao oauthDao;
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	private UserGroupDAO userGroupDAO;

	@Autowired
	private OAuthClientDao oauthClientDao;

	private Long userId;
	private Long clientId;
	private static final String SCOPE_HASH = DigestUtils.sha256Hex("some dummy text");
	private static final String OTHER_SCOPE_HASH = DigestUtils.sha256Hex("a different hash");
	private static final Date GRANTED_ON = new Date();

	@BeforeEach
	public void setUp() throws Exception {
		
		// Initialize a UserGroup
		UserGroup ug = new UserGroup();
		ug.setIsIndividual(true);
		userId = userGroupDAO.create(ug);
		
		// create an OAuthClient to grant consent to
		SectorIdentifier sectorIdentifier = new SectorIdentifier();
		sectorIdentifier.setCreatedBy(userId);
		sectorIdentifier.setCreatedOn(System.currentTimeMillis());
		sectorIdentifier.setSecret(SECTOR_IDENTIFIER_ENCRYPTION_SECRET);
		sectorIdentifier.setSectorIdentifierUri(SECTOR_IDENTIFIER);
		oauthClientDao.createSectorIdentifier(sectorIdentifier);
		OAuthClient oauthClient = new OAuthClient();
		oauthClient.setClient_name(CLIENT_NAME);
		oauthClient.setEtag(UUID.randomUUID().toString());
		oauthClient.setCreatedBy(userId.toString());
		oauthClient.setSector_identifier(SECTOR_IDENTIFIER);
		oauthClient.setVerified(false);
		oauthClient = oauthClientDao.createOAuthClient(oauthClient);
		clientId = Long.valueOf(oauthClient.getClient_id());
	}
		
	@AfterEach
	public void tearDown() throws Exception {
		oauthDao.deleteAuthorizationConsent(userId, clientId, SCOPE_HASH);
		
		try {
			oauthClientDao.deleteOAuthClient(clientId.toString());
			oauthClientDao.deleteSectorIdentifer(SECTOR_IDENTIFIER);
		} catch (NotFoundException e) {
			// let pass
		}
		
		try {
			userGroupDAO.delete(userId.toString());
		} catch (NotFoundException e) {
			// let pass
		}
	}
	
	private static final RowMapper<DBOAuthorizationConsent> ROW_MAPPER = DBOAuthorizationConsent.TABLE_MAPPING;
	
	private static final String LOOKUP_SQL = "SELECT * FROM "+TABLE_AUTHORIZATION_CONSENT+
			" WHERE "+
			COL_AUTHORIZATION_CONSENT_USER_ID+"=? AND "+
			COL_AUTHORIZATION_CONSENT_CLIENT_ID+"=? AND "+
			COL_AUTHORIZATION_CONSENT_SCOPE_HASH+"=?";
	
	
	@Test
	void testRoundtrip() {
		// method under test (no consent recorded)
		Date notBefore = GRANTED_ON;
		assertFalse(oauthDao.lookupAuthorizationConsent(userId, clientId, SCOPE_HASH, notBefore));
		
		// method under test (save consent)
		oauthDao.saveAuthorizationConsent(userId, clientId, SCOPE_HASH, GRANTED_ON);
		
		DBOAuthorizationConsent origDbo = jdbcTemplate.queryForObject(LOOKUP_SQL, new Object[] {userId, clientId, SCOPE_HASH}, ROW_MAPPER);
		
		// method under test (consent was recorded)
		assertTrue(oauthDao.lookupAuthorizationConsent(userId, clientId, SCOPE_HASH, notBefore));
		
		// method under test (do not return true for some other hash)
		assertFalse(oauthDao.lookupAuthorizationConsent(userId, clientId, "some other hash", notBefore));
		
		notBefore = new Date(GRANTED_ON.getTime()+1000L);
		// method under test (test threshold)
		assertFalse(oauthDao.lookupAuthorizationConsent(userId, clientId, SCOPE_HASH, notBefore));
				
		Date differentTime = new Date(GRANTED_ON.getTime()+1000L);
		// method under test (test updating an existing record)
		oauthDao.saveAuthorizationConsent(userId, clientId, SCOPE_HASH, differentTime);
		
		// check that etag has changed, ID has not
		DBOAuthorizationConsent updatedDbo = jdbcTemplate.queryForObject(LOOKUP_SQL, new Object[] {userId, clientId, SCOPE_HASH}, ROW_MAPPER);
		assertNotEquals(origDbo.geteTag(), updatedDbo.geteTag());
		assertEquals(origDbo.getId(), updatedDbo.getId());
		
		// the record is there...
		assertTrue(oauthDao.lookupAuthorizationConsent(userId, clientId, SCOPE_HASH, notBefore));
		
		// method under test
		oauthDao.deleteAuthorizationConsent(userId, clientId, SCOPE_HASH);
		
		// ... now it's gone
		assertFalse(oauthDao.lookupAuthorizationConsent(userId, clientId, SCOPE_HASH, notBefore));
	}

	@Test
	public void testDeleteAllForUserClientPair() {
		oauthDao.saveAuthorizationConsent(userId, clientId, SCOPE_HASH, GRANTED_ON);
		oauthDao.saveAuthorizationConsent(userId, clientId, OTHER_SCOPE_HASH, GRANTED_ON);

		assertTrue(oauthDao.lookupAuthorizationConsent(userId, clientId, SCOPE_HASH, GRANTED_ON));
		assertTrue(oauthDao.lookupAuthorizationConsent(userId, clientId, OTHER_SCOPE_HASH, GRANTED_ON));

		// method under test
		oauthDao.deleteAuthorizationConsentForClient(userId, clientId);

		assertFalse(oauthDao.lookupAuthorizationConsent(userId, clientId, SCOPE_HASH, GRANTED_ON));
		assertFalse(oauthDao.lookupAuthorizationConsent(userId, clientId, OTHER_SCOPE_HASH, GRANTED_ON));

	}

}
