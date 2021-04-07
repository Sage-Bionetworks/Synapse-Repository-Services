package org.sagebionetworks.repo.model.dbo.auth;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AUTHENTICATED_ON_AUTHENTICATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AUTHENTICATED_ON_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AUTHENTICATED_ON_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CREDENTIAL_PASS_HASH;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CREDENTIAL_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CREDENTIAL_SECRET_KEY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SESSION_TOKEN_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SESSION_TOKEN_SESSION_TOKEN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TERMS_OF_USE_AGREEMENT_AGREEMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TERMS_OF_USE_AGREEMENT_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_AUTHENTICATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_CREDENTIAL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_SESSION_TOKEN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_TERMS_OF_USE_AGREEMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_USER_GROUP;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;

import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.auth.AuthenticationDAO;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAuthenticatedOn;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.model.dbo.persistence.DBOSessionToken;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTermsOfUseAgreement;
import org.sagebionetworks.repo.model.principal.BootstrapGroup;
import org.sagebionetworks.repo.model.principal.BootstrapPrincipal;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.securitytools.HMACUtils;
import org.sagebionetworks.securitytools.PBKDF2Utils;
import org.sagebionetworks.util.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;


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
	
	public static final String SELECT_AUTHENTICATED_ON_FOR_PRINCIPAL_ID = 
			"SELECT "+COL_AUTHENTICATED_ON_AUTHENTICATED_ON+
			" FROM "+TABLE_AUTHENTICATED_ON+
			" WHERE "+COL_AUTHENTICATED_ON_PRINCIPAL_ID+" = ?";
	
	private static final String SELECT_COUNT_BY_EMAIL_AND_PASSWORD =
			"SELECT COUNT(*)"+
			" FROM "+TABLE_CREDENTIAL+", "+TABLE_USER_GROUP+
			" WHERE "+COL_CREDENTIAL_PRINCIPAL_ID+"="+COL_USER_GROUP_ID+
			" AND "+COL_USER_GROUP_ID+"= ?"+
			" AND "+COL_CREDENTIAL_PASS_HASH+"=?";
	
	private static final String UPDATE_AUTHENTICATED_ON = 
			"UPDATE "+TABLE_AUTHENTICATED_ON+
			" SET "+COL_AUTHENTICATED_ON_AUTHENTICATED_ON+"= ?, "+COL_AUTHENTICATED_ON_ETAG+"=UUID()"+
			" WHERE "+COL_AUTHENTICATED_ON_PRINCIPAL_ID+"= ?";
	
	private static final String SELECT_SESSION_TOKEN_BY_PRINCIPAL_ID_IF_VALID = 
			"SELECT st."+COL_SESSION_TOKEN_SESSION_TOKEN+", tou."+COL_TERMS_OF_USE_AGREEMENT_AGREEMENT+
			" FROM "+TABLE_SESSION_TOKEN+" st, "+TABLE_TERMS_OF_USE_AGREEMENT+" tou, "+TABLE_AUTHENTICATED_ON+" ao "+
			" WHERE tou."+COL_TERMS_OF_USE_AGREEMENT_PRINCIPAL_ID+"=st."+COL_SESSION_TOKEN_PRINCIPAL_ID+
			" AND st."+COL_SESSION_TOKEN_PRINCIPAL_ID+"= ? "+
			" AND ao."+COL_AUTHENTICATED_ON_PRINCIPAL_ID+"="+"st."+COL_SESSION_TOKEN_PRINCIPAL_ID+
			" AND ao."+COL_AUTHENTICATED_ON_AUTHENTICATED_ON+" > ?";
	
	private static final String NULLIFY_SESSION_TOKEN =
			"UPDATE "+TABLE_SESSION_TOKEN+
			" SET "+COL_SESSION_TOKEN_SESSION_TOKEN+"=NULL"+
			" WHERE "+COL_SESSION_TOKEN_SESSION_TOKEN+"= ?";

	private static final String NULLIFY_SESSION_TOKEN_FOR_PRINCIPAL_ID =
			"UPDATE "+TABLE_SESSION_TOKEN+
			" SET "+COL_SESSION_TOKEN_SESSION_TOKEN+"=NULL"+
			" WHERE "+ COL_SESSION_TOKEN_PRINCIPAL_ID+"= ?";

	private static final String SELECT_PRINCIPAL_BY_TOKEN = 
			"SELECT "+COL_SESSION_TOKEN_PRINCIPAL_ID+
			" FROM "+TABLE_SESSION_TOKEN+
			" WHERE "+COL_SESSION_TOKEN_SESSION_TOKEN+"= ?";
	
	private static final String SELECT_PRINCIPAL_BY_TOKEN_IF_VALID = 
			"SELECT st."+COL_SESSION_TOKEN_PRINCIPAL_ID+
			" FROM "+TABLE_SESSION_TOKEN+" st, "+TABLE_AUTHENTICATED_ON+" ao "+
			" WHERE "+COL_SESSION_TOKEN_SESSION_TOKEN+"= ?"+
			" AND ao."+COL_AUTHENTICATED_ON_PRINCIPAL_ID+"="+"st."+COL_SESSION_TOKEN_PRINCIPAL_ID+
			" AND "+COL_AUTHENTICATED_ON_AUTHENTICATED_ON+"> ?";
	
	private static final String SELECT_PASSWORD = 
			"SELECT "+COL_CREDENTIAL_PASS_HASH+
			" FROM "+TABLE_CREDENTIAL+", "+TABLE_USER_GROUP+
			" WHERE "+COL_CREDENTIAL_PRINCIPAL_ID+"="+COL_USER_GROUP_ID+
			" AND "+COL_USER_GROUP_ID+"= ?";
	
	private static final String UPDATE_PASSWORD = 
			"UPDATE "+TABLE_CREDENTIAL+
			" SET "+COL_CREDENTIAL_PASS_HASH+"= ?"+
			" WHERE "+COL_CREDENTIAL_PRINCIPAL_ID+"= ?";
	
	private static final String SELECT_SECRET_KEY = 
			"SELECT "+COL_CREDENTIAL_SECRET_KEY+
			" FROM "+TABLE_CREDENTIAL+
			" WHERE "+COL_CREDENTIAL_PRINCIPAL_ID+"= ?";
	
	private static final String UPDATE_SECRET_KEY = 
			"UPDATE "+TABLE_CREDENTIAL+
			" SET "+COL_CREDENTIAL_SECRET_KEY+"= ?"+
			" WHERE "+COL_CREDENTIAL_PRINCIPAL_ID+"= ?";
	
	private static final String SELECT_TOU_ACCEPTANCE = 
			"SELECT "+COL_TERMS_OF_USE_AGREEMENT_AGREEMENT+
			" FROM "+TABLE_TERMS_OF_USE_AGREEMENT+
			" WHERE "+COL_TERMS_OF_USE_AGREEMENT_PRINCIPAL_ID+"= ?";
	
	private RowMapper<Session> sessionRowMapper = new RowMapper<Session>() {
		@Override
		public Session mapRow(ResultSet rs, int rowNum) throws SQLException {
			Session session = new Session();
			session.setSessionToken(rs.getString(COL_SESSION_TOKEN_SESSION_TOKEN));
			session.setAcceptsTermsOfUse(rs.getBoolean(COL_TERMS_OF_USE_AGREEMENT_AGREEMENT));
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
	public boolean checkUserCredentials(long principalId, String passHash) {
		return jdbcTemplate.queryForObject(SELECT_COUNT_BY_EMAIL_AND_PASSWORD, Long.class, principalId, passHash) > 0;
	}

	@Override
	@WriteTransaction
	public boolean revalidateSessionTokenIfNeeded(long principalId) {
		// Determine the last time the token was re-validate.
		Date lastValidatedOn = jdbcTemplate.queryForObject(SELECT_AUTHENTICATED_ON_FOR_PRINCIPAL_ID, new SingleColumnRowMapper<Date>(), principalId);
		long now = clock.currentTimeMillis();
		/*
		 * Only revalidate a token if it is past its half-life.
		 * See: PLFM-3202 & PLFM-3206
		 */
		if(lastValidatedOn.getTime() + HALF_SESSION_EXPIRATION < now){
			// The session token needs to be revaldiated.
			userGroupDAO.touch(principalId);
			jdbcTemplate.update(UPDATE_AUTHENTICATED_ON, clock.now(), principalId);
			return true;
		}else{
			// no need to update.
			return false;
		}
	}
	
	@Override
	@WriteTransaction
	public String changeSessionToken(long principalId, String sessionToken) {
		userGroupDAO.touch(principalId);
		
		if (sessionToken == null) {
			sessionToken = UUID.randomUUID().toString();
		}
		
		DBOSessionToken dboSession = new DBOSessionToken();
		dboSession.setPrincipalId(principalId);
		dboSession.setSessionToken(sessionToken);
		basicDAO.createOrUpdate(dboSession);
		
		DBOAuthenticatedOn dboAuthOn = new DBOAuthenticatedOn();
		dboAuthOn.setPrincipalId(principalId);
		dboAuthOn.setAuthenticatedOn(new Date());
		dboAuthOn.setEtag(UUID.randomUUID().toString());
		basicDAO.createOrUpdate(dboAuthOn);
		
		return sessionToken;
	}

	@Override
	public Session getSessionTokenIfValid(long principalId) {
		return getSessionTokenIfValid(principalId, new Date());
	}
	
	@Override
	public Date getAuthenticatedOn(long principalId) {
		try {
			return jdbcTemplate.queryForObject(SELECT_AUTHENTICATED_ON_FOR_PRINCIPAL_ID, Date.class, principalId);
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}
	
	@Override
	public Session getSessionTokenIfValid(long principalId, Date now) {
		long time = now.getTime() - SESSION_EXPIRATION_TIME;
		try {
			return jdbcTemplate.queryForObject(SELECT_SESSION_TOKEN_BY_PRINCIPAL_ID_IF_VALID, sessionRowMapper,
					principalId, new Date(time));
		} catch(EmptyResultDataAccessException e) {
			return null;
		}
	}

	@Override
	@WriteTransaction
	public void deleteSessionToken(String sessionToken) {
		Long principalId = getPrincipal(sessionToken);
		if (principalId != null) {
			userGroupDAO.touch(principalId);
		}
		jdbcTemplate.update(NULLIFY_SESSION_TOKEN, sessionToken);
	}

	@Override
	@WriteTransaction
	public void deleteSessionToken(long principalId) {
		userGroupDAO.touch(principalId);
		jdbcTemplate.update(NULLIFY_SESSION_TOKEN_FOR_PRINCIPAL_ID, principalId);
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
		String passHash = getPasswordHash(principalId);
		if (passHash == null) {
			return null;
		}
		return PBKDF2Utils.extractSalt(passHash);
	}

	@Override
	public String getPasswordHash(long principalId) {
		try {
			return jdbcTemplate.queryForObject(SELECT_PASSWORD, new SingleColumnRowMapper<String>(), principalId);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("User (" + principalId + ") does not exist");
		}
	}

	@Override
	@WriteTransaction
	public void changePassword(long principalId, String passHash) {
		userGroupDAO.touch(principalId);
		jdbcTemplate.update(UPDATE_PASSWORD, passHash, principalId);
	}
	
	@Override
	public String getSecretKey(long principalId) throws NotFoundException {
		try {
			return jdbcTemplate.queryForObject(SELECT_SECRET_KEY, new SingleColumnRowMapper<String>(), principalId);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("");
		}
	}
	
	@Override
	@WriteTransaction
	public void changeSecretKey(long principalId) {
		changeSecretKey(principalId, HMACUtils.newHMACSHA1Key());
	}
	
	@Override
	@WriteTransaction
	public void changeSecretKey(long principalId, String secretKey) {
		userGroupDAO.touch(principalId);
		jdbcTemplate.update(UPDATE_SECRET_KEY, secretKey, principalId);
	}

	@Override
	public boolean hasUserAcceptedToU(long principalId) throws NotFoundException {
		Boolean acceptance;
		try {
			acceptance = jdbcTemplate.queryForObject(SELECT_TOU_ACCEPTANCE, new SingleColumnRowMapper<Boolean>(), principalId);
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
	@WriteTransaction
	public void setTermsOfUseAcceptance(long principalId, Boolean acceptance) {
		if (acceptance == null) {
			acceptance = Boolean.FALSE;
		}
		userGroupDAO.touch(principalId);
		
		DBOTermsOfUseAgreement agreement = new DBOTermsOfUseAgreement();
		agreement.setPrincipalId(principalId);
		agreement.setAgreesToTermsOfUse(acceptance);
		basicDAO.createOrUpdate(agreement);
	}
	
	@Override
	@WriteTransaction
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
				setTermsOfUseAcceptance(abs.getId(), true);
			}
		}
		// The migration admin should only be used in specific, non-development stacks
		Long migrationAdminId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		changeSecretKey(migrationAdminId, StackConfigurationSingleton.singleton().getMigrationAdminAPIKey());
	}

}
