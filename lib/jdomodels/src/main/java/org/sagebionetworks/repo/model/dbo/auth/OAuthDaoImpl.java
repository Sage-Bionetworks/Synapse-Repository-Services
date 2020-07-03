package org.sagebionetworks.repo.model.dbo.auth;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AUTHORIZATION_CONSENT_CLIENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AUTHORIZATION_CONSENT_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AUTHORIZATION_CONSENT_GRANTED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AUTHORIZATION_CONSENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AUTHORIZATION_CONSENT_SCOPE_HASH;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AUTHORIZATION_CONSENT_USER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_AUTHORIZATION_CONSENT;

import java.util.Date;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.auth.OAuthDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OAuthDaoImpl implements OAuthDao {


	@Autowired
	private IdGenerator idGenerator;

	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	private static final String INSERT_OR_UPDATE_SQL = 
			"INSERT INTO "+TABLE_AUTHORIZATION_CONSENT+" ("+
				COL_AUTHORIZATION_CONSENT_ID+","+
				COL_AUTHORIZATION_CONSENT_ETAG+","+
				COL_AUTHORIZATION_CONSENT_USER_ID+","+
				COL_AUTHORIZATION_CONSENT_CLIENT_ID+","+
				COL_AUTHORIZATION_CONSENT_SCOPE_HASH+","+
				COL_AUTHORIZATION_CONSENT_GRANTED_ON+
			") VALUES (?,UUID(),?,?,?,?) ON DUPLICATE KEY UPDATE "+
				COL_AUTHORIZATION_CONSENT_ETAG+"= UUID(),"+
				COL_AUTHORIZATION_CONSENT_GRANTED_ON+"= ?";
	
	private static final String LOOKUP_SQL = "SELECT "+
			COL_AUTHORIZATION_CONSENT_CLIENT_ID+
			" FROM "+TABLE_AUTHORIZATION_CONSENT+
			" WHERE "+
			COL_AUTHORIZATION_CONSENT_USER_ID+"=? AND "+
			COL_AUTHORIZATION_CONSENT_CLIENT_ID+"=? AND "+
			COL_AUTHORIZATION_CONSENT_SCOPE_HASH+"=? AND "+
			COL_AUTHORIZATION_CONSENT_GRANTED_ON+" >= ?";

	private static final String DELETE_SQL = "DELETE FROM "+TABLE_AUTHORIZATION_CONSENT+
			" WHERE "+
			COL_AUTHORIZATION_CONSENT_USER_ID+"=? AND "+
			COL_AUTHORIZATION_CONSENT_CLIENT_ID+"=? AND "+
			COL_AUTHORIZATION_CONSENT_SCOPE_HASH+"=?";

	private static final String DELETE_ALL_FOR_USER_CLIENT_SQL = "DELETE FROM "+TABLE_AUTHORIZATION_CONSENT+
			" WHERE "+
			COL_AUTHORIZATION_CONSENT_USER_ID+"=? AND "+
			COL_AUTHORIZATION_CONSENT_CLIENT_ID+"=?";

	@Override
	public void saveAuthorizationConsent(Long userId, Long clientId, String scopeHash, Date date) {
		Long id = idGenerator.generateNewId(IdType.OAUTH_AUTHORIZATION_CONSENT);
		jdbcTemplate.update(INSERT_OR_UPDATE_SQL, id, userId, clientId, scopeHash, date.getTime(), date.getTime());
	}

	@Override
	public boolean lookupAuthorizationConsent(Long userId, Long clientId, String scopeHash, Date notBefore) {
		try {
			// query for the ID of the consent record.  If found, return 'true'
			jdbcTemplate.queryForObject(LOOKUP_SQL, Long.class, userId, clientId, scopeHash, notBefore.getTime());
			return true;
		} catch (EmptyResultDataAccessException e) {
			return false;
		}
	}

	@Override
	public void deleteAuthorizationConsent(Long userId, Long clientId, String scopeHash) {
		jdbcTemplate.update(DELETE_SQL, userId, clientId, scopeHash);
		
	}

	@Override
	public void deleteAuthorizationConsentForClient(Long userId, Long clientId) {
		jdbcTemplate.update(DELETE_ALL_FOR_USER_CLIENT_SQL, userId, clientId);
	}
}
