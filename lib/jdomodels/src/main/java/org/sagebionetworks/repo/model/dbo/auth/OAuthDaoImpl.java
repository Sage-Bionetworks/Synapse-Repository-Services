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

public class OAuthDaoImpl implements OAuthDao {

	@Autowired
	private DBOBasicDao basicDao;	

	@Autowired
	private IdGenerator idGenerator;

	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	private static final String LOOKUP_SQL = "";

	private static final String DELETE_SQL = "";

	@Override
	public void saveAuthorizationConsent(Long userId, Long clientId, String scopeHash, Date date) {
		DBOAuthorizationConsent dbo = new DBOAuthorizationConsent();
		Long id = idGenerator.generateNewId(IdType.OAUTH_AUTHORIZATION_CONSENT); // TODO what if it's an update rather than a create?
		dbo.setId(id);
		dbo.seteTag(UUID.randomUUID().toString());
		dbo.setUserId(userId);
		dbo.setClientId(clientId);
		dbo.setScopeHash(scopeHash);
		dbo.setGrantedOn(date.getTime());
		basicDao.createOrUpdate(dbo); 
		
	}

	@Override
	public Date lookupAuthorizationConsent(Long userId, Long clientId, String scopeHash) {
		DBOAuthorizationConsent result = jdbcTemplate.queryForObject(LOOKUP_SQL, DBOAuthorizationConsent.class);
		return result==null ? null : new Date(result.getGrantedOn());
	}

	@Override
	public void deleteAuthorizationConsent(Long userId, Long clientId, String scopeHash) {
		jdbcTemplate.execute(DELETE_SQL);
		
	}

	
	

}
