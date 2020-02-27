package org.sagebionetworks.repo.model.dbo.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AUTHORIZATION_CONSENT_CLIENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AUTHORIZATION_CONSENT_GRANTED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AUTHORIZATION_CONSENT_SCOPE_HASH;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AUTHORIZATION_CONSENT_USER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_AUTHORIZATION_CONSENT;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.auth.OAuthDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAuthorizationConsent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
class OAuthDaoImplTest {
	
	@Autowired
	private OAuthDao oauthDao;
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	private static final long USER_ID = 101L;
	private static final long CLIENT_ID = 999L;
	private static final String SCOPE_HASH;
	private static final Date GRANTED_ON = new Date();
	
	static {
		String text = "some dummy text";
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
		SCOPE_HASH = new String(hash, StandardCharsets.UTF_8);
	}

	@AfterEach
	public void tearDown() throws Exception {
		oauthDao.deleteAuthorizationConsent(USER_ID, CLIENT_ID, SCOPE_HASH);
	}
	
	private static final RowMapper<DBOAuthorizationConsent> ROW_MAPPER = (new DBOAuthorizationConsent()).getTableMapping();
	
	private static final String LOOKUP_SQL = "SELECT * FROM "+TABLE_AUTHORIZATION_CONSENT+
			" WHERE "+
			COL_AUTHORIZATION_CONSENT_USER_ID+"=? AND "+
			COL_AUTHORIZATION_CONSENT_CLIENT_ID+"=? AND "+
			COL_AUTHORIZATION_CONSENT_SCOPE_HASH+"=?";
	
	
	@Test
	void testRoundtrip() {
		// method under test
		assertNull(oauthDao.lookupAuthorizationConsent(USER_ID, CLIENT_ID, SCOPE_HASH));
		// method under test
		oauthDao.saveAuthorizationConsent(USER_ID, CLIENT_ID, SCOPE_HASH, GRANTED_ON);
		
		DBOAuthorizationConsent origDbo = jdbcTemplate.queryForObject(LOOKUP_SQL, new Object[] {USER_ID, CLIENT_ID, SCOPE_HASH}, ROW_MAPPER);
		
		// method under test
		assertEquals(GRANTED_ON, oauthDao.lookupAuthorizationConsent(USER_ID, CLIENT_ID, SCOPE_HASH));
		// method under test
		assertNull(oauthDao.lookupAuthorizationConsent(USER_ID, CLIENT_ID, "some other hash"));
		
		Date differentTime = new Date(GRANTED_ON.getTime()+1000L);
		oauthDao.saveAuthorizationConsent(USER_ID, CLIENT_ID, SCOPE_HASH, differentTime);
		
		// method under test
		assertEquals(differentTime, oauthDao.lookupAuthorizationConsent(USER_ID, CLIENT_ID, SCOPE_HASH));
		
		// check that etag has changed, ID has not
		DBOAuthorizationConsent updatedDbo = jdbcTemplate.queryForObject(LOOKUP_SQL, new Object[] {USER_ID, CLIENT_ID, SCOPE_HASH}, ROW_MAPPER);
		assertNotEquals(origDbo.geteTag(), updatedDbo.geteTag());
		assertEquals(origDbo.getId(), updatedDbo.getId());
		
		// method under test
		oauthDao.deleteAuthorizationConsent(USER_ID, CLIENT_ID, SCOPE_HASH);
		
		assertNull(oauthDao.lookupAuthorizationConsent(USER_ID, CLIENT_ID, SCOPE_HASH));
	}

}
