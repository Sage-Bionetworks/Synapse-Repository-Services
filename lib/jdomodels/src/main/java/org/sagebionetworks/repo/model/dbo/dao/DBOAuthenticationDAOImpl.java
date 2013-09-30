package org.sagebionetworks.repo.model.dbo.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import org.sagebionetworks.repo.model.AuthenticationDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.auth.Credential;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.securitytools.HMACUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


public class DBOAuthenticationDAOImpl implements AuthenticationDAO {
	
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private DBOBasicDao basicDAO;
	
	/**
	 * A session token expires after 1 day
	 */
	public static final Long SESSION_EXPIRATION_TIME = 1000 * 60 * 60 * 24L;
	
	private static final String ID_PARAM_NAME = "id";
	private static final String PASSWORD_PARAM_NAME = "password";
	private static final String TOKEN_PARAM_NAME = "token";
	private static final String TIME_PARAM_NAME = "time";
	
	private static final String SELECT_SESSION_TOKEN = 
			"SELECT "+SqlConstants.COL_CREDENTIAL_SESSION_TOKEN+", "+SqlConstants.COL_CREDENTIAL_VALIDATED_ON+
				" FROM "+SqlConstants.TABLE_CREDENTIAL+
			" WHERE "+SqlConstants.COL_CREDENTIAL_PRINCIPAL_ID+"=:"+ID_PARAM_NAME;
	
	private static final String SELECT_SESSION_TOKEN_BY_ID_PASSWORD = 
			SELECT_SESSION_TOKEN+
				" AND "+SqlConstants.COL_CREDENTIAL_PASS_HASH+"=:"+PASSWORD_PARAM_NAME;
	
	private static final String NULLIFY_SESSION_TOKEN = 
			"UPDATE "+SqlConstants.TABLE_CREDENTIAL+" SET "+
			SqlConstants.COL_CREDENTIAL_SESSION_TOKEN+"=NULL"+
			" WHERE "+SqlConstants.COL_CREDENTIAL_SESSION_TOKEN+"=:"+TOKEN_PARAM_NAME;
	
	private static final String UPDATE_SESSION_TOKEN_IF_EXPIRED = 
			"UPDATE "+SqlConstants.TABLE_CREDENTIAL+" SET "+
			SqlConstants.COL_CREDENTIAL_SESSION_TOKEN+"=:"+TOKEN_PARAM_NAME+
			" WHERE "+SqlConstants.COL_CREDENTIAL_PRINCIPAL_ID+"=:"+ID_PARAM_NAME+
			" AND ("+SqlConstants.COL_CREDENTIAL_VALIDATED_ON+"<:"+TIME_PARAM_NAME+
				" OR "+SqlConstants.COL_CREDENTIAL_SESSION_TOKEN+" IS NULL)";
	
	private static final String UPDATE_VALIDATION_TIME = 
			"UPDATE "+SqlConstants.TABLE_CREDENTIAL+" SET "+
			SqlConstants.COL_CREDENTIAL_VALIDATED_ON+"=:"+TIME_PARAM_NAME+
			" WHERE "+SqlConstants.COL_CREDENTIAL_PRINCIPAL_ID+"=:"+ID_PARAM_NAME;
	
	private static final String SELECT_PRINCIPAL_BY_TOKEN = 
			"SELECT "+SqlConstants.COL_CREDENTIAL_PRINCIPAL_ID+" FROM "+SqlConstants.TABLE_CREDENTIAL+
			" WHERE "+SqlConstants.COL_CREDENTIAL_SESSION_TOKEN+"=:"+TOKEN_PARAM_NAME;
	
	private static final String UPDATE_PASSWORD = 
			"UPDATE "+SqlConstants.TABLE_CREDENTIAL+" SET "+
			SqlConstants.COL_CREDENTIAL_PASS_HASH+"=:"+PASSWORD_PARAM_NAME+
			" WHERE "+SqlConstants.COL_CREDENTIAL_PRINCIPAL_ID+"=:"+ID_PARAM_NAME;
	
	private static final String SELECT_SECRET_KEY = 
			"SELECT "+SqlConstants.COL_CREDENTIAL_SECRET_KEY+
			" FROM "+SqlConstants.TABLE_CREDENTIAL+
		" WHERE "+SqlConstants.COL_CREDENTIAL_PRINCIPAL_ID+"=:"+ID_PARAM_NAME;
	
	private static final String UPDATE_SECRET_KEY = 
			"UPDATE "+SqlConstants.TABLE_CREDENTIAL+" SET "+
			SqlConstants.COL_CREDENTIAL_SECRET_KEY+"=:"+TOKEN_PARAM_NAME+
			" WHERE "+SqlConstants.COL_CREDENTIAL_PRINCIPAL_ID+"=:"+ID_PARAM_NAME;
			
	
	private RowMapper<Session> sessionRowMapper = new RowMapper<Session>() {

		@Override
		public Session mapRow(ResultSet rs, int rowNum)
				throws SQLException {
			Timestamp timestamp = rs.getTimestamp(SqlConstants.COL_CREDENTIAL_VALIDATED_ON);
			
			// No token was ever requested
			if (timestamp == null) {
				return null;
			}
			// Don't send back an expired token
			Long timeSinceIssued = new Date().getTime() - timestamp.getTime();
			if (timeSinceIssued > SESSION_EXPIRATION_TIME) {
				return null;
			}
			
			// The session token may be null, meaning that no token was ever issued
			Session session = new Session();
			session.setSessionToken(rs.getString(SqlConstants.COL_CREDENTIAL_SESSION_TOKEN));
			return session;
		}
		
	};
	
	private RowMapper<Long> principalRowMapper = new RowMapper<Long>() {
		
		@Override
		public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
			return rs.getLong(SqlConstants.COL_CREDENTIAL_PRINCIPAL_ID);
		}

	};
	
	private RowMapper<String> secretKeyRowMapper = new RowMapper<String>() {

		@Override
		public String mapRow(ResultSet rs, int rowNum) throws SQLException {
			return rs.getString(SqlConstants.COL_CREDENTIAL_SECRET_KEY);
		}
		
	};

	@Override
	@Transactional(readOnly=false, propagation=Propagation.REQUIRED)
	public Session authenticate(Credential credential) throws NotFoundException {
		UserGroup ug = userGroupDAO.findGroup(credential.getEmail(), true);
		if (ug == null) {
			throw new NotFoundException("The provided username does not exist");
		}
		String principalId = ug.getId();
		
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID_PARAM_NAME, principalId);
		param.addValue(PASSWORD_PARAM_NAME, credential.getPassword());
		List<Session> sessionToken = simpleJdbcTemplate.query(SELECT_SESSION_TOKEN_BY_ID_PASSWORD, 
				sessionRowMapper, param);
		
		if (sessionToken.size() == 1) {
			return revalidateSessionToken(principalId);
		} else if (sessionToken.size() == 0) {
			throw new UnauthorizedException("The provided username/password combination is incorrect");
		} else {
			// Unreachable
			throw new DatastoreException("The unique key "+credential.getEmail()+" maps to more than one value");
		}
	}

	@Override
	@Transactional(readOnly=false, propagation=Propagation.REQUIRED)
	public Session getSessionToken(String username) {
		String principalId = userGroupDAO.findGroup(username, true).getId();
		return revalidateSessionToken(principalId);
	}

	@Override
	@Transactional(readOnly=false, propagation=Propagation.REQUIRED)
	public void deleteSessionToken(String sessionToken) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(TOKEN_PARAM_NAME, null);
		simpleJdbcTemplate.update(NULLIFY_SESSION_TOKEN, param);
	}
	
	@Override
	@Transactional(readOnly=false, propagation=Propagation.REQUIRED)
	public Long getPrincipal(String sessionToken) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(TOKEN_PARAM_NAME, sessionToken);
		
		Long principal = null;
		try {
			principal = simpleJdbcTemplate.queryForObject(SELECT_PRINCIPAL_BY_TOKEN, 
					principalRowMapper, param); 
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
		
		// Renew the token before returning
		revalidateSessionToken(principal.toString());
		return principal;
	}
	
	@Override
	@Transactional(readOnly=false, propagation=Propagation.REQUIRED)
	public void create(String id, String passHash) {
		DBOCredential credential = new DBOCredential();
		credential.setPrincipalId(Long.parseLong(id));
		credential.setPassHash(passHash);
		credential.setSecretKey(HMACUtils.newHMACSHA1Key());
		basicDAO.createNew(credential);
	}
	
	@Override
	@Transactional(readOnly=false, propagation=Propagation.REQUIRED)
	public void changePassword(String id, String passHash) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID_PARAM_NAME, id);
		param.addValue(PASSWORD_PARAM_NAME, passHash);
		simpleJdbcTemplate.update(UPDATE_PASSWORD, param);
	}
	
	@Override
	public String getSecretKey(String id) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID_PARAM_NAME, id);
		return simpleJdbcTemplate.queryForObject(SELECT_SECRET_KEY, secretKeyRowMapper, param);
	}
	
	@Override
	@Transactional(readOnly=false, propagation=Propagation.REQUIRED)
	public void changeSecretKey(String id) {
		changeSecretKey(id, HMACUtils.newHMACSHA1Key());
	}
	
	@Override
	@Transactional(readOnly=false, propagation=Propagation.REQUIRED)
	public void changeSecretKey(String id, String secretKey) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID_PARAM_NAME, id);
		param.addValue(TOKEN_PARAM_NAME, secretKey);
		simpleJdbcTemplate.update(UPDATE_SECRET_KEY, param);
	}
	
	/**
	 * Either renews an existing session token 
	 *   or creates a new session token if the old one has expired
	 */
	private Session revalidateSessionToken(String principalId) {
		Date now = new Date();
		
		// Update the session token if it does not exist or has expired
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(TOKEN_PARAM_NAME, HMACUtils.newHMACSHA1Key());
		param.addValue(ID_PARAM_NAME, principalId);
		param.addValue(TIME_PARAM_NAME, new Date(now.getTime() - SESSION_EXPIRATION_TIME));
		simpleJdbcTemplate.update(UPDATE_SESSION_TOKEN_IF_EXPIRED, param);
		
		// Update when the session token was last validated
		param = new MapSqlParameterSource();
		param.addValue(ID_PARAM_NAME, principalId);
		param.addValue(TIME_PARAM_NAME, now);
		simpleJdbcTemplate.update(UPDATE_VALIDATION_TIME, param);
		
		// Fetch the session token
		param = new MapSqlParameterSource();
		param.addValue(ID_PARAM_NAME, principalId);
		Session session = simpleJdbcTemplate.queryForObject(SELECT_SESSION_TOKEN, 
				sessionRowMapper, param);
		if (session == null) {
			throw new DatastoreException("The session token that was just created should not be null");
		}
		return session;
	}
}
