package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SESSION_TOKEN_DOMAIN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SESSION_TOKEN_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SESSION_TOKEN_SESSION_TOKEN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SESSION_TOKEN_VALIDATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TERMS_OF_USE_AGREEMENT_AGREEMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TERMS_OF_USE_AGREEMENT_DOMAIN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TERMS_OF_USE_AGREEMENT_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_SESSION_TOKEN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_TERMS_OF_USE_AGREEMENT;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.AuthenticationDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.DomainType;
import org.sagebionetworks.repo.model.UnauthenticatedException;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.model.dbo.persistence.DBOSessionToken;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTermsOfUseAgreement;
import org.sagebionetworks.repo.model.principal.BootstrapGroup;
import org.sagebionetworks.repo.model.principal.BootstrapPrincipal;
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
	private static final String PRINCIPAL_ID_PARAM_NAME = "principalId";
	private static final String PASSWORD_PARAM_NAME = "password";
	private static final String TOKEN_PARAM_NAME = "token";
	private static final String TIME_PARAM_NAME = "time";
	private static final String DOMAIN_PARAM_NAME = "domain";
	
	private static final String SELECT_ID_BY_EMAIL_AND_PASSWORD = 
			"SELECT "+SqlConstants.COL_CREDENTIAL_PRINCIPAL_ID+
				" FROM "+SqlConstants.TABLE_CREDENTIAL+", "+SqlConstants.TABLE_USER_GROUP+
			" WHERE "+SqlConstants.COL_CREDENTIAL_PRINCIPAL_ID+"="+SqlConstants.COL_USER_GROUP_ID+
				" AND "+SqlConstants.COL_USER_GROUP_ID+"=:"+PRINCIPAL_ID_PARAM_NAME+
				" AND "+SqlConstants.COL_CREDENTIAL_PASS_HASH+"=:"+PASSWORD_PARAM_NAME;
	
	private static final String UPDATE_VALIDATION_TIME = 
			"UPDATE "+SqlConstants.TABLE_SESSION_TOKEN+" SET "+
					SqlConstants.COL_SESSION_TOKEN_VALIDATED_ON+"=:"+TIME_PARAM_NAME+
			" WHERE "+SqlConstants.COL_SESSION_TOKEN_PRINCIPAL_ID+"=:"+ID_PARAM_NAME + 
			" AND " + SqlConstants.COL_SESSION_TOKEN_DOMAIN + "=:"+DOMAIN_PARAM_NAME;
	
	private static final String IF_VALID_SUFFIX = 
			" AND "+SqlConstants.COL_SESSION_TOKEN_VALIDATED_ON+">:"+TIME_PARAM_NAME;
	
	// NOTE: Neither in this version, or the prior version, were you selecting by user's name
	private static final String SELECT_SESSION_TOKEN_BY_USERNAME_IF_VALID = 
		String.format("SELECT %s, %s FROM %s st, %s tou WHERE tou.%s=st.%s AND tou.%s=st.%s AND st.%s=:%s AND tou.%s=:%s AND st.%s>:%s;",
			COL_SESSION_TOKEN_SESSION_TOKEN, COL_TERMS_OF_USE_AGREEMENT_AGREEMENT,
			TABLE_SESSION_TOKEN, TABLE_TERMS_OF_USE_AGREEMENT,
			COL_TERMS_OF_USE_AGREEMENT_PRINCIPAL_ID, COL_SESSION_TOKEN_PRINCIPAL_ID,
			COL_TERMS_OF_USE_AGREEMENT_DOMAIN, COL_SESSION_TOKEN_DOMAIN,
			COL_SESSION_TOKEN_PRINCIPAL_ID, PRINCIPAL_ID_PARAM_NAME,
			COL_TERMS_OF_USE_AGREEMENT_DOMAIN, DOMAIN_PARAM_NAME,
			COL_SESSION_TOKEN_VALIDATED_ON, TIME_PARAM_NAME
		);
	
	private static final String NULLIFY_SESSION_TOKEN = 
			"UPDATE "+SqlConstants.TABLE_SESSION_TOKEN+" SET "+
					SqlConstants.COL_SESSION_TOKEN_SESSION_TOKEN+"=NULL"+
			" WHERE "+SqlConstants.COL_SESSION_TOKEN_SESSION_TOKEN+"=:"+TOKEN_PARAM_NAME;

	private static final String SELECT_PRINCIPAL_BY_TOKEN = 
			"SELECT "+SqlConstants.COL_SESSION_TOKEN_PRINCIPAL_ID+" FROM "+SqlConstants.TABLE_SESSION_TOKEN+
			" WHERE "+SqlConstants.COL_SESSION_TOKEN_SESSION_TOKEN+"=:"+TOKEN_PARAM_NAME;
	
	private static final String SELECT_PRINCIPAL_BY_TOKEN_IF_VALID = 
			SELECT_PRINCIPAL_BY_TOKEN+IF_VALID_SUFFIX;
	
	private static final String SELECT_PASSWORD = 
			"SELECT "+SqlConstants.COL_CREDENTIAL_PASS_HASH+
				" FROM "+SqlConstants.TABLE_CREDENTIAL+", "+SqlConstants.TABLE_USER_GROUP+
			" WHERE "+SqlConstants.COL_CREDENTIAL_PRINCIPAL_ID+"="+SqlConstants.COL_USER_GROUP_ID+
			" AND "+SqlConstants.COL_USER_GROUP_ID+"=:"+PRINCIPAL_ID_PARAM_NAME;
	
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
			"SELECT "+SqlConstants.COL_TERMS_OF_USE_AGREEMENT_AGREEMENT+
			" FROM "+SqlConstants.TABLE_TERMS_OF_USE_AGREEMENT+
		" WHERE "+SqlConstants.COL_TERMS_OF_USE_AGREEMENT_PRINCIPAL_ID+"=:"+ID_PARAM_NAME+
		" AND "+SqlConstants.COL_TERMS_OF_USE_AGREEMENT_DOMAIN+"=:"+DOMAIN_PARAM_NAME;
	
	private RowMapper<Session> sessionRowMapper = new RowMapper<Session>() {
		@Override
		public Session mapRow(ResultSet rs, int rowNum) throws SQLException {
			Session session = new Session();
			session.setSessionToken(rs.getString(SqlConstants.COL_SESSION_TOKEN_SESSION_TOKEN));
			session.setAcceptsTermsOfUse(rs.getBoolean(SqlConstants.COL_TERMS_OF_USE_AGREEMENT_AGREEMENT));
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
	public Long checkUserCredentials(long principalId, String passHash) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(PRINCIPAL_ID_PARAM_NAME, principalId);
		param.addValue(PASSWORD_PARAM_NAME, passHash);
		try {
			return simpleJdbcTemplate.queryForLong(SELECT_ID_BY_EMAIL_AND_PASSWORD, param);
		} catch (EmptyResultDataAccessException e) {
			throw new UnauthenticatedException("The provided username/password combination is incorrect");
		}
	}
	
	@Override
	@Transactional(readOnly=false, propagation=Propagation.REQUIRED)
	public void revalidateSessionToken(long principalId, DomainType domain) {
		if (domain == null) {
			throw new UnauthenticatedException("Domain must be declared to revalidate session token");
		}
		userGroupDAO.touch(principalId);
		
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID_PARAM_NAME, principalId);
		param.addValue(TIME_PARAM_NAME, new Date().getTime());
		param.addValue(DOMAIN_PARAM_NAME, domain.name());
		simpleJdbcTemplate.update(UPDATE_VALIDATION_TIME, param);
	}
	
	@Override
	@Transactional(readOnly=false, propagation=Propagation.REQUIRED)
	public String changeSessionToken(long principalId, String sessionToken, DomainType domain) {
		userGroupDAO.touch(principalId);
		
		if (sessionToken == null) {
			sessionToken = UUID.randomUUID().toString();
		}
		
		DBOSessionToken dboSession = new DBOSessionToken();
		dboSession.setPrincipalId(principalId);
		dboSession.setDomain(domain);
		dboSession.setSessionToken(sessionToken);
		dboSession.setValidatedOn(new Date());
		basicDAO.createOrUpdate(dboSession);
		return sessionToken;
	}

	@Override
	public Session getSessionTokenIfValid(long principalsId, DomainType domain) {
		return getSessionTokenIfValid(principalsId, new Date(), domain);
	}
	
	@Override
	public Session getSessionTokenIfValid(long principalId, Date now, DomainType domain) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(PRINCIPAL_ID_PARAM_NAME, principalId);
		param.addValue(DOMAIN_PARAM_NAME, domain.name());
		param.addValue(TIME_PARAM_NAME, now.getTime() - SESSION_EXPIRATION_TIME);
		try {
			return simpleJdbcTemplate.queryForObject(SELECT_SESSION_TOKEN_BY_USERNAME_IF_VALID, sessionRowMapper,
					param);
		} catch(EmptyResultDataAccessException e) {
			return null;
		}
	}

	@Override
	@Transactional(readOnly=false, propagation=Propagation.REQUIRED)
	public void deleteSessionToken(String sessionToken) {
		Long principalId = getPrincipal(sessionToken);
		if (principalId != null) {
			userGroupDAO.touch(principalId);
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
		param.addValue(TIME_PARAM_NAME, new Date().getTime() - SESSION_EXPIRATION_TIME);
		
		try {
			return simpleJdbcTemplate.queryForLong(SELECT_PRINCIPAL_BY_TOKEN_IF_VALID, param); 
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}
	
	@Override
	public byte[] getPasswordSalt(long principalId) throws NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(PRINCIPAL_ID_PARAM_NAME, principalId);
		String passHash;
		try {
			passHash = simpleJdbcTemplate.queryForObject(SELECT_PASSWORD, String.class, param);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("User (" + principalId + ") does not exist");
		}
		if (passHash == null) {
			return null;
		}
		return PBKDF2Utils.extractSalt(passHash);
	}
	
	@Override
	@Transactional(readOnly=false, propagation=Propagation.REQUIRED)
	public void changePassword(long principalId, String passHash) {
		userGroupDAO.touch(principalId);
		
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID_PARAM_NAME, principalId);
		param.addValue(PASSWORD_PARAM_NAME, passHash);
		simpleJdbcTemplate.update(UPDATE_PASSWORD, param);
	}
	
	@Override
	public String getSecretKey(long principalId) throws NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID_PARAM_NAME, principalId);
		try {
			return simpleJdbcTemplate.queryForObject(SELECT_SECRET_KEY, String.class, param);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException(e);
		}
	}
	
	@Override
	@Transactional(readOnly=false, propagation=Propagation.REQUIRED)
	public void changeSecretKey(long principalId) {
		changeSecretKey(principalId, HMACUtils.newHMACSHA1Key());
	}
	
	@Override
	@Transactional(readOnly=false, propagation=Propagation.REQUIRED)
	public void changeSecretKey(long principalId, String secretKey) {
		userGroupDAO.touch(principalId);
		
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID_PARAM_NAME, principalId);
		param.addValue(TOKEN_PARAM_NAME, secretKey);
		simpleJdbcTemplate.update(UPDATE_SECRET_KEY, param);
	}

	@Override
	public boolean hasUserAcceptedToU(long principalId, DomainType domain) throws NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID_PARAM_NAME, principalId);
		param.addValue(DOMAIN_PARAM_NAME, domain.name());
		Boolean acceptance;
		try {
			acceptance = simpleJdbcTemplate.queryForObject(SELECT_TOU_ACCEPTANCE, Boolean.class, param);
		} catch (EmptyResultDataAccessException e) {
			// It's possible now that there is no record. That shouldn't be an
			// exception, that's a "false, not accepted".
			return false;
		}
		if (acceptance == null) {
			return false;
		}
		return acceptance;
	}
	
	@Override
	@Transactional(readOnly=false, propagation=Propagation.REQUIRED)
	public void setTermsOfUseAcceptance(long principalId, DomainType domain, Boolean acceptance) {
		if (acceptance == null) {
			acceptance = Boolean.FALSE;
		}
		userGroupDAO.touch(principalId);
		
		DBOTermsOfUseAgreement agreement = new DBOTermsOfUseAgreement();
		agreement.setPrincipalId(principalId);
		agreement.setDomain(domain);
		agreement.setAgreesToTermsOfUse(acceptance);
		basicDAO.createOrUpdate(agreement);
	}
	
	@Override
	@Transactional(readOnly=false, propagation=Propagation.REQUIRED)
	public void bootstrapCredentials() throws NotFoundException {
		if(this.userGroupDAO.getBootstrapPrincipals() == null) throw new IllegalStateException("bootstrapPrincipals must be initialized");
		for (BootstrapPrincipal abs : this.userGroupDAO.getBootstrapPrincipals()) {
			if (abs instanceof BootstrapGroup) {
				continue;
			}
			
			// If the user has a secret key, then the user has a row in the credentials table
			try {
				getSecretKey(abs.getId());
			} catch (NotFoundException e) {
				createNew(abs.getId());
			}
			
			// With the exception of anonymous, bootstrapped users should not need to sign the terms of use
			if (!AuthorizationUtils.isUserAnonymous(abs.getId())) {
				setTermsOfUseAcceptance(abs.getId(), DomainType.SYNAPSE, true);
				setTermsOfUseAcceptance(abs.getId(), DomainType.BRIDGE, true);
			}
		}
		// The migration admin should only be used in specific, non-development stacks
		Long migrationAdminId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		changeSecretKey(migrationAdminId, StackConfiguration.getMigrationAdminAPIKey());
	}

}
