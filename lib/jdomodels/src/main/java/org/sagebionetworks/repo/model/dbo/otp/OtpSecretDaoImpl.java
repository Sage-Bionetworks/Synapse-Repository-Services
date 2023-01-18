package org.sagebionetworks.repo.model.dbo.otp;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OTP_SECRET_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OTP_SECRET_ACTIVE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OTP_SECRET_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OTP_SECRET_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OTP_SECRET_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OTP_SECRET_SECRET;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_OTP_SECRET;

import java.util.Optional;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class OtpSecretDaoImpl implements OtpSecretDao {
	
	private static final RowMapper<DBOOtpSecret> ROW_MAPPER = new DBOOtpSecret().getTableMapping();

	private IdGenerator idGenerator;
	private JdbcTemplate jdbcTemplate;

	public OtpSecretDaoImpl(IdGenerator idGenerator, JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
		this.idGenerator = idGenerator;
	}

	@Override
	@WriteTransaction
	public DBOOtpSecret storeSecret(Long userId, String secret) {
		Long secretId = idGenerator.generateNewId(IdType.OTP_SECRET_ID);
		
		String createOrUpdateSql = "INSERT INTO " + TABLE_OTP_SECRET + "("
			+ COL_OTP_SECRET_ID + ","
			+ COL_OTP_SECRET_ETAG + ","
			+ COL_OTP_SECRET_PRINCIPAL_ID + ","
			+ COL_OTP_SECRET_CREATED_ON + ","
			+ COL_OTP_SECRET_SECRET + ","
			+ COL_OTP_SECRET_ACTIVE
			+ ") VALUES (?, UUID(), ?, NOW(), ?, FALSE) ON DUPLICATE KEY UPDATE "
			+ COL_OTP_SECRET_ID + "=?,"
			+ COL_OTP_SECRET_ETAG + "=UUID(),"
			+ COL_OTP_SECRET_CREATED_ON + "=NOW(),"
			+ COL_OTP_SECRET_SECRET + "=?";
		
		jdbcTemplate.update(createOrUpdateSql, secretId, userId, secret, secretId, secret);
		
		return getSecret(userId, secretId).get();
	}
	
	@Override
	public Optional<DBOOtpSecret> getSecret(Long userId, Long secretId) {
		String selectSql = "SELECT * FROM " + TABLE_OTP_SECRET + " WHERE " + COL_OTP_SECRET_ID + "=? AND " + COL_OTP_SECRET_PRINCIPAL_ID + "=?";
		
		try {
			return Optional.of(jdbcTemplate.queryForObject(selectSql, ROW_MAPPER, secretId, userId));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@Override
	public Optional<DBOOtpSecret> getActiveSecret(Long userId) {
		String selectSql = "SELECT * FROM " + TABLE_OTP_SECRET + " WHERE " 
			+ COL_OTP_SECRET_PRINCIPAL_ID + "=? AND " 
			+ COL_OTP_SECRET_ACTIVE + "=TRUE";
		
		try {
			return Optional.of(jdbcTemplate.queryForObject(selectSql, ROW_MAPPER, userId));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}
	
	@Override
	public boolean hasActiveSecret(Long userId) {
		String countSql = "SELECT COUNT(*) FROM " + TABLE_OTP_SECRET + " WHERE " 
			+ COL_OTP_SECRET_PRINCIPAL_ID + "=? AND " 
			+ COL_OTP_SECRET_ACTIVE + "=TRUE";
		
		return jdbcTemplate.queryForObject(countSql, Long.class, userId) > 0L;
	}

	@Override
	@WriteTransaction
	public DBOOtpSecret activateSecret(Long userId, Long secretId) {
		String updateSql = "UPDATE " + TABLE_OTP_SECRET + " SET " 
			+ COL_OTP_SECRET_ACTIVE + "=TRUE,"
			+ COL_OTP_SECRET_ETAG + "=UUID() WHERE "
			+ COL_OTP_SECRET_PRINCIPAL_ID + "=? AND " 
			+ COL_OTP_SECRET_ACTIVE + "=FALSE AND "
			+ COL_OTP_SECRET_ID + "=?";

		try {
			jdbcTemplate.update(updateSql, userId, secretId);
		} catch (DuplicateKeyException e) {
			throw new IllegalStateException("An active secret already exists", e);
		}
		
		return getSecret(userId, secretId).orElseThrow(() -> new IllegalArgumentException("Invalid secret id"));
	}
	
	@Override
	@WriteTransaction
	public void deleteSecret(Long userId, Long secretId) {
		String deleteSql = "DELETE FROM " + TABLE_OTP_SECRET + " WHERE " 
			+ COL_OTP_SECRET_PRINCIPAL_ID + "=? AND "
			+ COL_OTP_SECRET_ID + "=?";
		
		if (jdbcTemplate.update(deleteSql, userId, secretId) == 0) {
			throw new IllegalArgumentException("Invalid secret id");
		};
		
	}

	@Override
	@WriteTransaction
	public void deleteSecrets(Long userId) {
		String deleteSql = "DELETE FROM " + TABLE_OTP_SECRET + " WHERE " + COL_OTP_SECRET_PRINCIPAL_ID + "=?";
		
		jdbcTemplate.update(deleteSql, userId);
	}

	@Override
	public void truncateAll() {
		jdbcTemplate.update("DELETE FROM " + TABLE_OTP_SECRET);
	}

}
