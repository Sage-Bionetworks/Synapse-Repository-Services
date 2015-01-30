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
import org.sagebionetworks.util.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


public class DBOAuthenticationDAOImpl implements AuthenticationDAO {

	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private DBOBasicDao basicDAO;
	@Autowired
	Clock clock;
	
	/**
	 * A session token expires after 1 day
	 */
	public static final Long SESSION_EXPIRATION_TIME = 1000 * 60 * 60 * 24L;
	public static final long HALF_SESSION_EXPIRATION = SESSION_EXPIRATION_TIME/2;
	
	public static final String SELECT_VALIDATED_ON_FROM_SESSION_TOKEN = "SELECT "+
			COL_SESSION_TOKEN_VALIDATED_ON+
			" FROM "+TABLE_SESSION_TOKEN+" WHERE "+COL_SESSION_TOKEN_PRINCIPAL_ID+
			" = ? AND "+COL_SESSION_TOKEN_DOMAIN+" = ? ";
	
	private static final String SELECT_ID_BY_EMAIL_AND_PASSWORD = 
			"SELECT "+SqlConstants.COL_CREDENTIAL_PRINCIPAL_ID+
				" FROM "+SqlConstants.TABLE_CREDENTIAL+", "+SqlConstants.TABLE_USER_GROUP+
			" WHERE "+SqlConstants.COL_CREDENTIAL_PRINCIPAL_ID+"="+SqlConstants.COL_USER_GROUP_ID+
				" AND "+SqlConstants.COL_USER_GROUP_ID+"= ?"+
				" AND "+SqlConstants.COL_CREDENTIAL_PASS_HASH+"=?";
	
	private static final String UPDATE_VALIDATION_TIME = 
			"UPDATE "+SqlConstants.TABLE_SESSION_TOKEN+" SET "+
					SqlConstants.COL_SESSION_TOKEN_VALIDATED_ON+"= ?"+
			" WHERE "+SqlConstants.COL_SESSION_TOKEN_PRINCIPAL_ID+"= ?"+ 
			" AND " + SqlConstants.COL_SESSION_TOKEN_DOMAIN + "= ?";
	
	private static final String IF_VALID_SUFFIX = 
			" AND "+SqlConstants.COL_SESSION_TOKEN_VALIDATED_ON+"> ?";
	
	// NOTE: Neither in this version, or the prior version, were you selecting by user's name
	private static final String SELECT_SESSION_TOKEN_BY_USERNAME_IF_VALID = 
			"SELECT st."+COL_SESSION_TOKEN_SESSION_TOKEN+", tou."+COL_TERMS_OF_USE_AGREEMENT_AGREEMENT+" FROM "+TABLE_SESSION_TOKEN+" st, "+TABLE_TERMS_OF_USE_AGREEMENT+
			" tou WHERE tou."+COL_TERMS_OF_USE_AGREEMENT_PRINCIPAL_ID+"=st."+COL_SESSION_TOKEN_PRINCIPAL_ID+" AND tou."+COL_TERMS_OF_USE_AGREEMENT_DOMAIN+
			"=st."+COL_SESSION_TOKEN_DOMAIN+" AND st."+COL_SESSION_TOKEN_PRINCIPAL_ID+"= ? AND tou."+COL_TERMS_OF_USE_AGREEMENT_DOMAIN+
			"= ? AND st."+COL_SESSION_TOKEN_VALIDATED_ON+" > ?";

	
	private static final String NULLIFY_SESSION_TOKEN = 
			"UPDATE "+SqlConstants.TABLE_SESSION_TOKEN+" SET "+
					SqlConstants.COL_SESSION_TOKEN_SESSION_TOKEN+"=NULL"+
			" WHERE "+SqlConstants.COL_SESSION_TOKEN_SESSION_TOKEN+"= ?";

	private static final String SELECT_PRINCIPAL_BY_TOKEN = 
			"SELECT "+SqlConstants.COL_SESSION_TOKEN_PRINCIPAL_ID+" FROM "+SqlConstants.TABLE_SESSION_TOKEN+
			" WHERE "+SqlConstants.COL_SESSION_TOKEN_SESSION_TOKEN+"= ?";
	
	private static final String SELECT_PRINCIPAL_BY_TOKEN_IF_VALID = 
			SELECT_PRINCIPAL_BY_TOKEN+IF_VALID_SUFFIX;
	
	private static final String SELECT_PASSWORD = 
			"SELECT "+SqlConstants.COL_CREDENTIAL_PASS_HASH+
				" FROM "+SqlConstants.TABLE_CREDENTIAL+", "+SqlConstants.TABLE_USER_GROUP+
			" WHERE "+SqlConstants.COL_CREDENTIAL_PRINCIPAL_ID+"="+SqlConstants.COL_USER_GROUP_ID+
			" AND "+SqlConstants.COL_USER_GROUP_ID+"= ?";
	
	private static final String UPDATE_PASSWORD = 
			"UPDATE "+SqlConstants.TABLE_CREDENTIAL+" SET "+
			SqlConstants.COL_CREDENTIAL_PASS_HASH+"= ?"+
			" WHERE "+SqlConstants.COL_CREDENTIAL_PRINCIPAL_ID+"= ?";
	
	private static final String SELECT_SECRET_KEY = 
			"SELECT "+SqlConstants.COL_CREDENTIAL_SECRET_KEY+
			" FROM "+SqlConstants.TABLE_CREDENTIAL+
		" WHERE "+SqlConstants.COL_CREDENTIAL_PRINCIPAL_ID+"= ?";
	
	private static final String UPDATE_SECRET_KEY = 
			"UPDATE "+SqlConstants.TABLE_CREDENTIAL+" SET "+
			SqlConstants.COL_CREDENTIAL_SECRET_KEY+"= ?"+
			" WHERE "+SqlConstants.COL_CREDENTIAL_PRINCIPAL_ID+"= ?";
	
	private static final String SELECT_TOU_ACCEPTANCE = 
			"SELECT "+SqlConstants.COL_TERMS_OF_USE_AGREEMENT_AGREEMENT+
			" FROM "+SqlConstants.TABLE_TERMS_OF_USE_AGREEMENT+
		" WHERE "+SqlConstants.COL_TERMS_OF_USE_AGREEMENT_PRINCIPAL_ID+"= ?"+
		" AND "+SqlConstants.COL_TERMS_OF_USE_AGREEMENT_DOMAIN+"= ?";
	
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
		try {
			return jdbcTemplate.queryForObject(SELECT_ID_BY_EMAIL_AND_PASSWORD, new SingleColumnRowMapper<Long>(), principalId, passHash);
		} catch (EmptyResultDataAccessException e) {
			throw new UnauthenticatedException("The provided username/password combination is incorrect");
		}
	}
	
	@Override
	@Transactional(readOnly=false, propagation=Propagation.REQUIRED)
	public boolean revalidateSessionTokenIfNeeded(long principalId, DomainType domain) {
		if (domain == null) {
			throw new UnauthenticatedException("Domain must be declared to revalidate session token");
		}
		// Determine the last time the token was re-validate.
		Long lastValidatedOn = jdbcTemplate.queryForObject(SELECT_VALIDATED_ON_FROM_SESSION_TOKEN, new SingleColumnRowMapper<Long>(), principalId, domain.name());
		long now = clock.currentTimeMillis();
		/*
		 * Only revalidate a token if it is past its half-life.
		 * See: PLFM-3202 & PLFM-3206
		 */
		if(lastValidatedOn + HALF_SESSION_EXPIRATION < now){
			// The session token needs to be revaldiated.
			userGroupDAO.touch(principalId);
			jdbcTemplate.update(UPDATE_VALIDATION_TIME, clock.currentTimeMillis(),principalId, domain.name());
			return true;
		}else{
			// no need to update.
			return false;
		}
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
		long time = now.getTime() - SESSION_EXPIRATION_TIME;
		try {
			return jdbcTemplate.queryForObject(SELECT_SESSION_TOKEN_BY_USERNAME_IF_VALID, sessionRowMapper,
					principalId, domain.name(), time);
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
		jdbcTemplate.update(NULLIFY_SESSION_TOKEN, sessionToken);
	}

	@Override
	public Long getPrincipal(String sessionToken) {		
		try {
			return jdbcTemplate.queryForObject(SELECT_PRINCIPAL_BY_TOKEN,new SingleColumnRowMapper<Long>(), sessionToken); 
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}
	
	@Override
	public Long getPrincipalIfValid(String sessionToken) {
		long time =clock.currentTimeMillis() - SESSION_EXPIRATION_TIME;
		try {
			return jdbcTemplate.queryForObject(SELECT_PRINCIPAL_BY_TOKEN_IF_VALID, new SingleColumnRowMapper<Long>(), sessionToken, time); 
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}
	
	@Override
	public byte[] getPasswordSalt(long principalId) throws NotFoundException {
		String passHash;
		try {
			passHash = jdbcTemplate.queryForObject(SELECT_PASSWORD, new SingleColumnRowMapper<String>(), principalId);
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
		jdbcTemplate.update(UPDATE_PASSWORD, passHash, principalId);
	}
	
	@Override
	public String getSecretKey(long principalId) throws NotFoundException {
		try {
			return jdbcTemplate.queryForObject(SELECT_SECRET_KEY, new SingleColumnRowMapper<String>(), principalId);
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
		jdbcTemplate.update(UPDATE_SECRET_KEY, secretKey, principalId);
	}

	@Override
	public boolean hasUserAcceptedToU(long principalId, DomainType domain) throws NotFoundException {
		Boolean acceptance;
		try {
			acceptance = jdbcTemplate.queryForObject(SELECT_TOU_ACCEPTANCE, new SingleColumnRowMapper<Boolean>(), principalId, domain.name());
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
