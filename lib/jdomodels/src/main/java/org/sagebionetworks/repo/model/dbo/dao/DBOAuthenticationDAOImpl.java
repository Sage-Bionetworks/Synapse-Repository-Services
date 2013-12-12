package org.sagebionetworks.repo.model.dbo.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.AuthenticationDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserGroupInt;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.securitytools.HMACUtils;
import org.sagebionetworks.securitytools.PBKDF2Utils;
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
	private static final String EMAIL_PARAM_NAME = "email";
	private static final String PASSWORD_PARAM_NAME = "password";
	private static final String TOKEN_PARAM_NAME = "token";
	private static final String TIME_PARAM_NAME = "time";
	private static final String TOU_PARAM_NAME = "tou";
	
	private static final String SELECT_ID_BY_EMAIL_AND_PASSWORD = 
			"SELECT "+SqlConstants.COL_CREDENTIAL_PRINCIPAL_ID+
				" FROM "+SqlConstants.TABLE_CREDENTIAL+", "+SqlConstants.TABLE_USER_GROUP+
			" WHERE "+SqlConstants.COL_CREDENTIAL_PRINCIPAL_ID+"="+SqlConstants.COL_USER_GROUP_ID+
				" AND "+SqlConstants.COL_USER_GROUP_NAME+"=:"+EMAIL_PARAM_NAME+
				" AND "+SqlConstants.COL_CREDENTIAL_PASS_HASH+"=:"+PASSWORD_PARAM_NAME;
	
	private static final String UPDATE_VALIDATION_TIME = 
			"UPDATE "+SqlConstants.TABLE_CREDENTIAL+" SET "+
					SqlConstants.COL_CREDENTIAL_VALIDATED_ON+"=:"+TIME_PARAM_NAME+
			" WHERE "+SqlConstants.COL_CREDENTIAL_PRINCIPAL_ID+"=:"+ID_PARAM_NAME;
	
	private static final String UPDATE_SESSION_TOKEN = 
			"UPDATE "+SqlConstants.TABLE_CREDENTIAL+" SET "+
					SqlConstants.COL_CREDENTIAL_VALIDATED_ON+"=:"+TIME_PARAM_NAME+","+
					SqlConstants.COL_CREDENTIAL_SESSION_TOKEN+"=:"+TOKEN_PARAM_NAME+
			" WHERE "+SqlConstants.COL_CREDENTIAL_PRINCIPAL_ID+"=:"+ID_PARAM_NAME;
	
	private static final String IF_VALID_SUFFIX = 
			" AND "+SqlConstants.COL_CREDENTIAL_VALIDATED_ON+">:"+TIME_PARAM_NAME;
	
	private static final String SELECT_SESSION_TOKEN_BY_USERNAME_IF_VALID = 
			"SELECT "+SqlConstants.COL_CREDENTIAL_SESSION_TOKEN+","+SqlConstants.COL_CREDENTIAL_TOU+
			" FROM "+SqlConstants.TABLE_CREDENTIAL+", "+SqlConstants.TABLE_USER_GROUP+
			" WHERE "+SqlConstants.COL_CREDENTIAL_PRINCIPAL_ID+"="+SqlConstants.COL_USER_GROUP_ID+
					" AND "+SqlConstants.COL_USER_GROUP_NAME+"=:"+EMAIL_PARAM_NAME+
					IF_VALID_SUFFIX;
	
	private static final String NULLIFY_SESSION_TOKEN = 
			"UPDATE "+SqlConstants.TABLE_CREDENTIAL+" SET "+
					SqlConstants.COL_CREDENTIAL_SESSION_TOKEN+"=NULL"+
			" WHERE "+SqlConstants.COL_CREDENTIAL_SESSION_TOKEN+"=:"+TOKEN_PARAM_NAME;
	
	private static final String SELECT_PRINCIPAL_BY_TOKEN = 
			"SELECT "+SqlConstants.COL_CREDENTIAL_PRINCIPAL_ID+" FROM "+SqlConstants.TABLE_CREDENTIAL+
			" WHERE "+SqlConstants.COL_CREDENTIAL_SESSION_TOKEN+"=:"+TOKEN_PARAM_NAME;
	
	private static final String SELECT_PRINCIPAL_BY_TOKEN_IF_VALID = 
			SELECT_PRINCIPAL_BY_TOKEN+IF_VALID_SUFFIX;
	
	private static final String SELECT_PASSWORD = 
			"SELECT "+SqlConstants.COL_CREDENTIAL_PASS_HASH+
				" FROM "+SqlConstants.TABLE_CREDENTIAL+", "+SqlConstants.TABLE_USER_GROUP+
			" WHERE "+SqlConstants.COL_CREDENTIAL_PRINCIPAL_ID+"="+SqlConstants.COL_USER_GROUP_ID+
			" AND "+SqlConstants.COL_USER_GROUP_NAME+"=:"+EMAIL_PARAM_NAME;
	
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
	
	private static final String SELECT_TOU_ACCEPTANCE = 
			"SELECT "+SqlConstants.COL_CREDENTIAL_TOU+
			" FROM "+SqlConstants.TABLE_CREDENTIAL+
		" WHERE "+SqlConstants.COL_CREDENTIAL_PRINCIPAL_ID+"=:"+ID_PARAM_NAME;
	
	private static final String UPDATE_TERMS_OF_USE_ACCEPTANCE = 
			"UPDATE "+SqlConstants.TABLE_CREDENTIAL+" SET "+
			SqlConstants.COL_CREDENTIAL_TOU+"=:"+TOU_PARAM_NAME+
			" WHERE "+SqlConstants.COL_CREDENTIAL_PRINCIPAL_ID+"=:"+ID_PARAM_NAME;
			
	
	private RowMapper<Session> sessionRowMapper = new RowMapper<Session>() {

		@Override
		public Session mapRow(ResultSet rs, int rowNum)
				throws SQLException {
			Session session = new Session();
			session.setSessionToken(rs.getString(SqlConstants.COL_CREDENTIAL_SESSION_TOKEN));
			session.setAcceptsTermsOfUse(rs.getBoolean(SqlConstants.COL_CREDENTIAL_TOU));
			return session;
		}
		
	};
	
	@Override
	public void createNew(long principalId) {
		DBOCredential cred = new DBOCredential();
		cred.setPrincipalId(principalId);
		cred.setSecretKey(HMACUtils.newHMACSHA1Key());
		basicDAO.createNew(cred);
	}
	
	@Override
	public Long checkEmailAndPassword(String email, String passHash) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(EMAIL_PARAM_NAME, email);
		param.addValue(PASSWORD_PARAM_NAME, passHash);
		try {
			return simpleJdbcTemplate.queryForLong(SELECT_ID_BY_EMAIL_AND_PASSWORD, param);
		} catch (EmptyResultDataAccessException e) {
			throw new UnauthorizedException("The provided username/password combination is incorrect");
		}
	}
	
	@Override
	@Transactional(readOnly=false, propagation=Propagation.REQUIRED)
	public void revalidateSessionToken(String principalId) {
		userGroupDAO.touch(principalId);
		
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID_PARAM_NAME, principalId);
		param.addValue(TIME_PARAM_NAME, new Date());
		simpleJdbcTemplate.update(UPDATE_VALIDATION_TIME, param);
	}
	
	@Override
	@Transactional(readOnly=false, propagation=Propagation.REQUIRED)
	public String changeSessionToken(String principalId, String sessionToken) {
		userGroupDAO.touch(principalId);
		
		if (sessionToken == null) {
			sessionToken = UUID.randomUUID().toString();
		}
		
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID_PARAM_NAME, principalId);
		param.addValue(TIME_PARAM_NAME, new Date());
		param.addValue(TOKEN_PARAM_NAME, sessionToken);
		simpleJdbcTemplate.update(UPDATE_SESSION_TOKEN, param);
		
		return sessionToken;
	}

	@Override
	public Session getSessionTokenIfValid(String username) {
		return getSessionTokenIfValid(username, new Date());
	}
	
	@Override
	public Session getSessionTokenIfValid(String username, Date now) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(EMAIL_PARAM_NAME, username);
		param.addValue(TIME_PARAM_NAME, new Date(now.getTime() - SESSION_EXPIRATION_TIME));
		try {
			return simpleJdbcTemplate.queryForObject(SELECT_SESSION_TOKEN_BY_USERNAME_IF_VALID, 
					sessionRowMapper, param);
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	@Override
	@Transactional(readOnly=false, propagation=Propagation.REQUIRED)
	public void deleteSessionToken(String sessionToken) {
		Long principalId = getPrincipal(sessionToken);
		if (principalId != null) {
			userGroupDAO.touch(principalId.toString());
		}
		
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(TOKEN_PARAM_NAME, sessionToken);
		simpleJdbcTemplate.update(NULLIFY_SESSION_TOKEN, param);
	}

	@Override
	public Long getPrincipal(String sessionToken) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(TOKEN_PARAM_NAME, sessionToken);
		
		try {
			return simpleJdbcTemplate.queryForLong(SELECT_PRINCIPAL_BY_TOKEN, param); 
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}
	
	@Override
	public Long getPrincipalIfValid(String sessionToken) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(TOKEN_PARAM_NAME, sessionToken);
		param.addValue(TIME_PARAM_NAME, new Date(new Date().getTime() - SESSION_EXPIRATION_TIME));
		
		try {
			return simpleJdbcTemplate.queryForLong(SELECT_PRINCIPAL_BY_TOKEN_IF_VALID, param); 
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}
	
	@Override
	public byte[] getPasswordSalt(String username) throws NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(EMAIL_PARAM_NAME, username);
		String passHash;
		try {
			passHash = simpleJdbcTemplate.queryForObject(SELECT_PASSWORD, String.class, param);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("User (" + username + ") does not exist");
		}
		if (passHash == null) {
			return null;
		}
		return PBKDF2Utils.extractSalt(passHash);
	}
	
	@Override
	@Transactional(readOnly=false, propagation=Propagation.REQUIRED)
	public void changePassword(String id, String passHash) {
		userGroupDAO.touch(id);
		
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID_PARAM_NAME, id);
		param.addValue(PASSWORD_PARAM_NAME, passHash);
		simpleJdbcTemplate.update(UPDATE_PASSWORD, param);
	}
	
	@Override
	public String getSecretKey(String id) throws NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID_PARAM_NAME, id);
		try {
			return simpleJdbcTemplate.queryForObject(SELECT_SECRET_KEY, String.class, param);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException(e);
		}
	}
	
	@Override
	@Transactional(readOnly=false, propagation=Propagation.REQUIRED)
	public void changeSecretKey(String id) {
		changeSecretKey(id, HMACUtils.newHMACSHA1Key());
	}
	
	@Override
	@Transactional(readOnly=false, propagation=Propagation.REQUIRED)
	public void changeSecretKey(String id, String secretKey) {
		userGroupDAO.touch(id);
		
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID_PARAM_NAME, id);
		param.addValue(TOKEN_PARAM_NAME, secretKey);
		simpleJdbcTemplate.update(UPDATE_SECRET_KEY, param);
	}

	@Override
	public boolean hasUserAcceptedToU(String id) throws NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID_PARAM_NAME, id);
		Boolean acceptance;
		try {
			acceptance = simpleJdbcTemplate.queryForObject(SELECT_TOU_ACCEPTANCE, Boolean.class, param);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException(e);
		}
		if (acceptance == null) {
			return false;
		}
		return acceptance;
	}
	
	@Override
	@Transactional(readOnly=false, propagation=Propagation.REQUIRED)
	public void setTermsOfUseAcceptance(String id, Boolean acceptance) {
		userGroupDAO.touch(id);
		
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID_PARAM_NAME, id);
		param.addValue(TOU_PARAM_NAME, acceptance);
		simpleJdbcTemplate.update(UPDATE_TERMS_OF_USE_ACCEPTANCE, param);
	}
	
	@Override
	@Transactional(readOnly=false, propagation=Propagation.REQUIRED)
	public void bootstrapCredentials() throws NotFoundException {
		List<UserGroupInt> ugs = userGroupDAO.getBootstrapUsers();
		for (UserGroupInt ug : ugs) {
			if (!ug.getIsIndividual()) {
				continue;
			}
			
			// If the user has a secret key, then the user has a row in the credentials table
			try {
				getSecretKey(ug.getId());
			} catch (NotFoundException e) {
				createNew(Long.parseLong(ug.getId()));
			}
			
			// With the exception of anonymous, bootstrapped users should not need to sign the terms of use
			if (!AuthorizationUtils.isUserAnonymous(ug.getId())) {
				setTermsOfUseAcceptance(ug.getId(), true);
			}
		}
		// The migration admin should only be used in specific, non-development stacks
		Long migrationAdminId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		changeSecretKey(migrationAdminId.toString(), StackConfiguration.getMigrationAdminAPIKey());
	}
}
