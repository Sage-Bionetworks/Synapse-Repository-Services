package org.sagebionetworks.repo.model.dbo.auth;

import java.util.Date;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.auth.OAuthDao;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAuthorizationConsent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

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
			") VALUES (?,?,?,?,?,?) ON DUPLICATE KEY UPDATE "+
				COL_AUTHORIZATION_CONSENT_ETAG+"= ?,"+
				COL_AUTHORIZATION_CONSENT_GRANTED_ON+"= ?";
	
	private static final String LOOKUP_SQL = "SELECT "+
			COL_AUTHORIZATION_CONSENT_GRANTED_ON+
			" FROM "+TABLE_AUTHORIZATION_CONSENT+
			" WHERE "+
			COL_AUTHORIZATION_CONSENT_USER_ID+"=? AND "+
			COL_AUTHORIZATION_CONSENT_CLIENT_ID+"=? AND "+
			COL_AUTHORIZATION_CONSENT_SCOPE_HASH+"=?";

	private static final String DELETE_SQL = "DELETE FROM "+TABLE_AUTHORIZATION_CONSENT+
			" WHERE "+
			COL_AUTHORIZATION_CONSENT_USER_ID+"=? AND "+
			COL_AUTHORIZATION_CONSENT_CLIENT_ID+"=? AND "+
			COL_AUTHORIZATION_CONSENT_SCOPE_HASH+"=?";

	@Override
	public void saveAuthorizationConsent(Long userId, Long clientId, String scopeHash, Date date) {
		Long id = idGenerator.generateNewId(IdType.OAUTH_AUTHORIZATION_CONSENT);
		String etag = UUID.randomUUID().toString();
		jdbcTemplate.update(INSERT_OR_UPDATE_SQL, id, etag, userId, clientId, scopeHash, date, etag, date);
	}

	@Override
	public Date lookupAuthorizationConsent(Long userId, Long clientId, String scopeHash) {
		Long result = jdbcTemplate.queryForObject(LOOKUP_SQL, Long.class, userId, clientId, scopeHash);
		return result==null ? null : new Date(result);
	}

	@Override
	public void deleteAuthorizationConsent(Long userId, Long clientId, String scopeHash) {
		jdbcTemplate.update(DELETE_SQL, userId, clientId, scopeHash);
		
	}
}
