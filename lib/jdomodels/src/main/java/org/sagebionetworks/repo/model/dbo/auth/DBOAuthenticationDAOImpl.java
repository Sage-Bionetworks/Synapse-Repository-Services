package org.sagebionetworks.repo.model.dbo.auth;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AUTHENTICATED_ON_AUTHENTICATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AUTHENTICATED_ON_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CREDENTIAL_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CREDENTIAL_EXPIRES_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CREDENTIAL_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CREDENTIAL_PASS_HASH;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CREDENTIAL_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CREDENTIAL_SECRET_KEY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TOS_AGREEMENT_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TOS_AGREEMENT_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TOS_AGREEMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TOS_AGREEMENT_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TOS_LATEST_VERSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TOS_LATEST_VERSION_UPDATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TOS_LATEST_VERSION_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TOS_REQUIREMENTS_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TOS_REQUIREMENTS_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TOS_REQUIREMENTS_ENFORCED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TOS_REQUIREMENTS_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TOS_REQUIREMENTS_MIN_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TWO_FA_STATUS_ENABLED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TWO_FA_STATUS_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_GROUP_CREATION_DATE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_AUTHENTICATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_CREDENTIAL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_TOS_AGREEMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_TOS_LATEST_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_TOS_REQUIREMENTS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_TWO_FA_STATUS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_USER_GROUP;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.auth.AuthenticationDAO;
import org.sagebionetworks.repo.model.auth.TermsOfServiceAgreement;
import org.sagebionetworks.repo.model.auth.TermsOfServiceRequirements;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.SinglePrimaryKeySqlParameterSource;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAuthenticatedOn;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.model.principal.BootstrapGroup;
import org.sagebionetworks.repo.model.principal.BootstrapPrincipal;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.securitytools.HMACUtils;
import org.sagebionetworks.securitytools.PBKDF2Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

public class DBOAuthenticationDAOImpl implements AuthenticationDAO {
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private DBOBasicDao basicDAO;
	
	@Autowired
	private IdGenerator idGenerator;
	
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
			" SET "+COL_CREDENTIAL_PASS_HASH+"= ?, " + COL_CREDENTIAL_ETAG + " = UUID(), " + COL_CREDENTIAL_MODIFIED_ON +" = NOW(), " + COL_CREDENTIAL_EXPIRES_ON + " = NOW() + INTERVAL ? DAY" +
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
		jdbcTemplate.update(UPDATE_PASSWORD, passHash, DBOCredential.MAX_PASSWORD_VALIDITY_DAYS, principalId);
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
	@WriteTransaction
	public TermsOfServiceAgreement addTermsOfServiceAgreement(long principalId, String version, Date agreedOn) {
		// We need an ignore here for backward compatibility, since clients try to sign multiple times
		String sql = "INSERT IGNORE INTO " + TABLE_TOS_AGREEMENT + "("
			+ COL_TOS_AGREEMENT_ID + ", "
			+ COL_TOS_AGREEMENT_CREATED_ON + ", "
			+ COL_TOS_AGREEMENT_CREATED_BY + ", "
			+ COL_TOS_AGREEMENT_VERSION + ")" 
			+ " VALUES (?, ?, ?, ?)";
		
		Long id = idGenerator.generateNewId(IdType.TOS_AGREEMENT_ID);

		jdbcTemplate.update(sql, id, agreedOn, principalId, version);
		
		return getLatestTermsOfServiceAgreement(principalId).orElseThrow();
	}
	
	@Override
	public Optional<TermsOfServiceAgreement> getLatestTermsOfServiceAgreement(long principalId) {
		String sql = "SELECT " 
				+ COL_TOS_AGREEMENT_VERSION + " , " 
				+ COL_TOS_AGREEMENT_CREATED_ON
				+ " FROM " + TABLE_TOS_AGREEMENT
				+ " WHERE " + COL_TOS_AGREEMENT_CREATED_BY + "=?"
				+ " ORDER BY " + COL_TOS_AGREEMENT_ID + " DESC"
				+ " LIMIT 1";
			
		return jdbcTemplate
				.query(sql, (rs, i) -> new TermsOfServiceAgreement()
						.setUserId(principalId)
						.setVersion(rs.getString(COL_TOS_AGREEMENT_VERSION))
						.setAgreedOn(new Date(rs.getTimestamp(COL_TOS_AGREEMENT_CREATED_ON).getTime())),
					principalId)
				.stream().findFirst();
	}
	
	@Override
	public List<UserGroup> getUsersWithoutAgreement(List<Long> userIds) {
		String sql = "SELECT"
			+ " U." + COL_USER_GROUP_ID + ","
			+ " U." + COL_USER_GROUP_CREATION_DATE 
			+ " FROM " + TABLE_USER_GROUP + " U LEFT JOIN " + TABLE_TOS_AGREEMENT + " A"
			+ " ON U." + COL_USER_GROUP_ID + " = A." + COL_TOS_AGREEMENT_CREATED_BY
			+ " WHERE U." + COL_USER_GROUP_ID + " IN (" + String.join(",", Collections.nCopies(userIds.size(), "?")) + ")"
			+ " AND A." + COL_TOS_AGREEMENT_ID + " IS NULL";
			
		return jdbcTemplate.query(sql, (rs,  i) -> {
			Timestamp creationDate = rs.getTimestamp(COL_USER_GROUP_CREATION_DATE);
			
			return new UserGroup()
				.setId(rs.getString(COL_USER_GROUP_ID))
				.setCreationDate(creationDate == null ? null : new Date(creationDate.getTime()));
			
		}, userIds.toArray());
		
	}
	
	@Override
	@WriteTransaction
	public void batchAddTermsOfServiceAgreement(List<TermsOfServiceAgreement> batch) {
		String sql = "INSERT INTO " + TABLE_TOS_AGREEMENT + "("
			+ COL_TOS_AGREEMENT_ID + ", "
			+ COL_TOS_AGREEMENT_CREATED_ON + ", "
			+ COL_TOS_AGREEMENT_CREATED_BY + ", "
			+ COL_TOS_AGREEMENT_VERSION + ")" 
			+ " VALUES (?, ?, ?, ?)";
		
		List<Long> batchIds = batch.stream().map( agreement -> 
			idGenerator.generateNewId(IdType.TOS_AGREEMENT_ID)
		).collect(Collectors.toList());
		
		jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
			
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				TermsOfServiceAgreement agreement = batch.get(i);
				
				ps.setLong(1, batchIds.get(i));
				ps.setTimestamp(2, new Timestamp(agreement.getAgreedOn().getTime()));
				ps.setLong(3, agreement.getUserId());
				ps.setString(4, agreement.getVersion());
			}
			
			@Override
			public int getBatchSize() {
				return batch.size();
			}
		});
	}
	
	@Override
	@WriteTransaction
	public TermsOfServiceRequirements setCurrentTermsOfServiceRequirements(long principalId, String minVersion, Date enforceOn) {
		String sql = "INSERT INTO " + TABLE_TOS_REQUIREMENTS + "("
			+ COL_TOS_REQUIREMENTS_ID + ", "
			+ COL_TOS_REQUIREMENTS_CREATED_ON + ", "
			+ COL_TOS_REQUIREMENTS_CREATED_BY + ", "
			+ COL_TOS_REQUIREMENTS_MIN_VERSION + ", "
			+ COL_TOS_REQUIREMENTS_ENFORCED_ON + ")" 
			+ " VALUES (?, NOW(), ?, ?, ?)";
		
		Long id = idGenerator.generateNewId(IdType.TOS_REQUIREMENT_ID);
		
		jdbcTemplate.update(sql, id, principalId, minVersion, enforceOn);
		
		return getCurrentTermsOfServiceRequirements();
	}
	
	@Override
	public TermsOfServiceRequirements getCurrentTermsOfServiceRequirements() {
		String sql = "SELECT " 
			+ COL_TOS_REQUIREMENTS_MIN_VERSION+ " , " 
			+ COL_TOS_REQUIREMENTS_ENFORCED_ON
			+ " FROM " + TABLE_TOS_REQUIREMENTS 
			+ " ORDER BY " + COL_TOS_REQUIREMENTS_ID + " DESC"
			+ " LIMIT 1";
		
		return jdbcTemplate
				.query(sql, (rs, i) -> new TermsOfServiceRequirements()
					.setMinimumTermsOfServiceVersion(rs.getString(COL_TOS_REQUIREMENTS_MIN_VERSION))
					.setRequirementDate(new Date(rs.getTimestamp(COL_TOS_REQUIREMENTS_ENFORCED_ON).getTime())))
				.stream().findFirst().orElseThrow(() -> new NotFoundException("Terms of service requirements not found."));
	}
	
	@Override
	public String getTermsOfServiceLatestVersion() {
		String sql = "SELECT " + COL_TOS_LATEST_VERSION_VERSION + " FROM " + TABLE_TOS_LATEST_VERSION + " WHERE " + COL_TOS_LATEST_VERSION_ID + "=?";
		
		return jdbcTemplate.queryForList(sql, String.class, DBOTermsOfServiceLatestVersion.LATEST_VERSION_ID)
				.stream().findFirst().orElseThrow(() -> new NotFoundException("Terms of service latest version not found."));
	}
	
	@Override
	@WriteTransaction
	public void setTermsOfServiceLatestVersion(String version) {
		String sql = "INSERT INTO " + TABLE_TOS_LATEST_VERSION + "("
			+ COL_TOS_LATEST_VERSION_ID + ","
			+ COL_TOS_LATEST_VERSION_UPDATED_ON + ","
			+ COL_TOS_LATEST_VERSION_VERSION + ")"
			+ " VALUES (" + DBOTermsOfServiceLatestVersion.LATEST_VERSION_ID + ", NOW(), ?)"
			+ " ON DUPLICATE KEY UPDATE " 
			+ COL_TOS_LATEST_VERSION_UPDATED_ON + " = NOW(), "
			+ COL_TOS_LATEST_VERSION_VERSION + " = ?";
		
		jdbcTemplate.update(sql, version, version);
			
	}
	
	@Override
	public void clearTermsOfServiceData() {
		jdbcTemplate.update("TRUNCATE TABLE " + TABLE_TOS_REQUIREMENTS);
		jdbcTemplate.update("TRUNCATE TABLE " + TABLE_TOS_LATEST_VERSION);
		jdbcTemplate.update("TRUNCATE TABLE " + TABLE_TOS_AGREEMENT);
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
	public Optional<Date> getPasswordModifiedOn(long principalId) {
		String sql = "SELECT " + COL_CREDENTIAL_MODIFIED_ON + " FROM " + TABLE_CREDENTIAL + " WHERE " + COL_CREDENTIAL_PRINCIPAL_ID + "=?";
		
		Date modifiedOn = jdbcTemplate.queryForObject(sql, Date.class, principalId);
		
		return Optional.ofNullable(modifiedOn);
	}
	
	@Override
	public Optional<Date> getPasswordExpiresOn(long principalId) {
		String sql = "SELECT " + COL_CREDENTIAL_EXPIRES_ON + " FROM " + TABLE_CREDENTIAL + " WHERE " + COL_CREDENTIAL_PRINCIPAL_ID + "=?";
		
		Date expiresOn = jdbcTemplate.queryForObject(sql, Date.class, principalId);
		
		return Optional.ofNullable(expiresOn);
	}
	
	@Override
	@WriteTransaction
	public void bootstrap() throws NotFoundException {
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
		}
		// The migration admin should only be used in specific, non-development stacks
		Long migrationAdminId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		changeSecretKey(migrationAdminId, StackConfigurationSingleton.singleton().getMigrationAdminAPIKey());
		
		// Makes sure we have the default TOS requirements
		TermsOfServiceRequirements tosRequirements;
		
		try {
			tosRequirements = getCurrentTermsOfServiceRequirements();
		} catch (NotFoundException e) {
			tosRequirements = setCurrentTermsOfServiceRequirements(migrationAdminId, DEFAULT_TOS_REQUIREMENTS.getMinimumTermsOfServiceVersion(), DEFAULT_TOS_REQUIREMENTS.getRequirementDate());
		}
		
		// Makes sure we have at least one latest version
		try {
			getTermsOfServiceLatestVersion();
		} catch (NotFoundException e) {
			setTermsOfServiceLatestVersion(tosRequirements.getMinimumTermsOfServiceVersion());
		}
	}

}
