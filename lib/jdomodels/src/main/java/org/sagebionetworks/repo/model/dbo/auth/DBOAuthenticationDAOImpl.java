package org.sagebionetworks.repo.model.dbo.auth;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AUTHENTICATED_ON_AUTHENTICATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AUTHENTICATED_ON_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CREDENTIAL_PASS_HASH;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CREDENTIAL_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CREDENTIAL_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CREDENTIAL_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CREDENTIAL_SECRET_KEY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TERMS_OF_USE_AGREEMENT_AGREEMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TERMS_OF_USE_AGREEMENT_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TWO_FA_STATUS_ENABLED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TWO_FA_STATUS_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_AUTHENTICATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_CREDENTIAL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_TERMS_OF_USE_AGREEMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_TWO_FA_STATUS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_USER_GROUP;

import java.sql.ResultSet;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.auth.AuthenticationDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.SinglePrimaryKeySqlParameterSource;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAuthenticatedOn;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTermsOfUseAgreement;
import org.sagebionetworks.repo.model.principal.BootstrapGroup;
import org.sagebionetworks.repo.model.principal.BootstrapPrincipal;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.securitytools.HMACUtils;
import org.sagebionetworks.securitytools.PBKDF2Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

public class DBOAuthenticationDAOImpl implements AuthenticationDAO {

	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private DBOBasicDao basicDAO;
	
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
	
	private static final String SELECT_PASSWORD = 
			"SELECT "+COL_CREDENTIAL_PASS_HASH+
			" FROM "+TABLE_CREDENTIAL+", "+TABLE_USER_GROUP+
			" WHERE "+COL_CREDENTIAL_PRINCIPAL_ID+"="+COL_USER_GROUP_ID+
			" AND "+COL_USER_GROUP_ID+"= ?";
	
	private static final String UPDATE_PASSWORD = 
			"UPDATE "+TABLE_CREDENTIAL+
			" SET "+COL_CREDENTIAL_PASS_HASH+"= ?, " + COL_CREDENTIAL_ETAG + " = UUID(), " + COL_CREDENTIAL_MODIFIED_ON +" = NOW()" +
			" WHERE "+COL_CREDENTIAL_PRINCIPAL_ID+"= ?";
	
	private static final String SELECT_SECRET_KEY = 
			"SELECT "+COL_CREDENTIAL_SECRET_KEY+
			" FROM "+TABLE_CREDENTIAL+
			" WHERE "+COL_CREDENTIAL_PRINCIPAL_ID+"= ?";
	
	private static final String UPDATE_SECRET_KEY = 
			"UPDATE "+TABLE_CREDENTIAL+
			// Note that we do not update the "MODIFIED_ON" since that applies only to passwords and the secret_key is deprecated
			" SET "+COL_CREDENTIAL_SECRET_KEY+"= ?, " + COL_CREDENTIAL_ETAG + " = UUID()" +
			" WHERE "+COL_CREDENTIAL_PRINCIPAL_ID+"= ?";
	
	private static final String SELECT_TOU_ACCEPTANCE = 
			"SELECT "+COL_TERMS_OF_USE_AGREEMENT_AGREEMENT+
			" FROM "+TABLE_TERMS_OF_USE_AGREEMENT+
			" WHERE "+COL_TERMS_OF_USE_AGREEMENT_PRINCIPAL_ID+"= ?";
	
	@Override
	public void createNew(long principalId) {
		DBOCredential cred = new DBOCredential();
		cred.setPrincipalId(principalId);
		cred.setSecretKey(HMACUtils.newHMACSHA1Key());
		cred.setEtag(UUID.randomUUID().toString());
		// Note that we do not set a modified_on date since that refers to the user password and we are just creating a secret_key (which is deprecated)
		basicDAO.createNew(cred);
	}
	
	@Override
	public boolean checkUserCredentials(long principalId, String passHash) {
		return jdbcTemplate.queryForObject(SELECT_COUNT_BY_EMAIL_AND_PASSWORD, Long.class, principalId, passHash) > 0;
	}

	@Override
	@WriteTransaction
	public void setAuthenticatedOn(long principalId, Date authTime) {
		DBOAuthenticatedOn dboAuthOn = new DBOAuthenticatedOn();
		dboAuthOn.setPrincipalId(principalId);
		dboAuthOn.setAuthenticatedOn(authTime);
		dboAuthOn.setEtag(UUID.randomUUID().toString());
		basicDAO.createOrUpdate(dboAuthOn);
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
			return jdbcTemplate.queryForObject(SELECT_PASSWORD, String.class, principalId);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("User (" + principalId + ") does not exist");
		}
	}

	@Override
	@WriteTransaction
	public void changePassword(long principalId, String passHash) {
		jdbcTemplate.update(UPDATE_PASSWORD, passHash, principalId);
	}
	
	@Override
	public String getSecretKey(long principalId) throws NotFoundException {
		try {
			return jdbcTemplate.queryForObject(SELECT_SECRET_KEY, String.class, principalId);
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
		jdbcTemplate.update(UPDATE_SECRET_KEY, secretKey, principalId);
	}

	@Override
	public boolean hasUserAcceptedToU(long principalId) throws NotFoundException {
		Boolean acceptance;
		try {
			acceptance = jdbcTemplate.queryForObject(SELECT_TOU_ACCEPTANCE, Boolean.class, principalId);
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
	public void setTwoFactorAuthState(long principalId, boolean enabled) {
		DBOUserTwoFaStatus status = new DBOUserTwoFaStatus();
		status.setPrincipalId(principalId);
		status.setEnabled(enabled);
		basicDAO.createOrUpdate(status);
		userGroupDAO.touch(principalId);
	}
	
	@Override
	public boolean isTwoFactorAuthEnabled(long principalId) {
		return basicDAO.getObjectByPrimaryKey(DBOUserTwoFaStatus.class, new SinglePrimaryKeySqlParameterSource(principalId))
			.map(DBOUserTwoFaStatus::getEnabled)
			.orElse(false);
	}
	
	@Override
	public Map<Long, Boolean> getTwoFactorAuthStateMap(Set<Long> principalIds) {
		
		Map<Long, Boolean> stateMap =  principalIds.stream()
			.collect(Collectors.toMap(Function.identity(), id -> false));
		
		String sql = "SELECT " + COL_TWO_FA_STATUS_PRINCIPAL_ID + ", " + COL_TWO_FA_STATUS_ENABLED 
			+ " FROM " + TABLE_TWO_FA_STATUS
			+ " WHERE " + COL_TWO_FA_STATUS_PRINCIPAL_ID + " IN ("
			+ String.join(",", Collections.nCopies(principalIds.size(), "?"))
			+ ")";
		
		jdbcTemplate.query(sql, (ResultSet rs) -> {
				Long principalId = rs.getLong(COL_TWO_FA_STATUS_PRINCIPAL_ID);
				Boolean twoFactorAuthEnabled = rs.getBoolean(COL_TWO_FA_STATUS_ENABLED);
				stateMap.put(principalId, twoFactorAuthEnabled);
			},
			principalIds.toArray()
		);

		return stateMap;
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
