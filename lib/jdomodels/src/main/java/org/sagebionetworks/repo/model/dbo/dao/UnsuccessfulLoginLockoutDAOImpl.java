package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CREDENTIAL_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_UNSUCCESSFUL_LOGIN_COUNT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_UNSUCCESSFUL_LOGIN_KEY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_UNSUCCESSFUL_LOGIN_LOCKOUT_EXPIRATION_TIMESTAMP_MILLIS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_CREDENTIAL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_UNSUCCESSFUL_LOGIN_LOCKOUT;

import org.apache.commons.lang.Validate;
import org.sagebionetworks.repo.model.UnsuccessfulLoginLockoutDAO;
import org.sagebionetworks.repo.model.UnsuccessfulLoginLockoutDTO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.SinglePrimaryKeySqlParameterSource;
import org.sagebionetworks.repo.model.dbo.loginlockout.DBOUnsuccessfulLoginLockout;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.transactions.MandatoryWriteReadCommittedTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

public class UnsuccessfulLoginLockoutDAOImpl implements UnsuccessfulLoginLockoutDAO {

	private static final String LOCK_FOR_UPDATE = "SELECT " + COL_CREDENTIAL_PRINCIPAL_ID + " FROM " + TABLE_CREDENTIAL +" WHERE " + COL_CREDENTIAL_PRINCIPAL_ID + "=?" + " FOR UPDATE";

	@Autowired
	DBOBasicDao basicDao;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@MandatoryWriteReadCommittedTransaction
	@Override
	public UnsuccessfulLoginLockoutDTO getUnsuccessfulLoginLockoutInfoIfExist(long userId) {
		// lock credentials table because we need to guarantee there exists an entry to lock per user.
		// The unsuccessful login table is not guaranteed to have a row for every user.
		jdbcTemplate.queryForObject(LOCK_FOR_UPDATE, Long.class, userId);
		return translateDBOToDTO(basicDao.getObjectByPrimaryKeyIfExists(DBOUnsuccessfulLoginLockout.class, new SinglePrimaryKeySqlParameterSource(userId)));
	}

	@MandatoryWriteReadCommittedTransaction
	public long getDatabaseTimestampMillis() {
		return basicDao.getDatabaseTimestampMillis();
	}

	@MandatoryWriteReadCommittedTransaction
	@Override
	public void createOrUpdateUnsuccessfulLoginLockoutInfo(UnsuccessfulLoginLockoutDTO dto) {
		ValidateArgument.required(dto, "dto");
		basicDao.createOrUpdate(translateDTOToDBO(dto));
	}

	@MandatoryWriteReadCommittedTransaction
	@Override
	public void deleteUnsuccessfulLoginLockoutInfo(long userId){
		basicDao.deleteObjectByPrimaryKey(DBOUnsuccessfulLoginLockout.class, new SinglePrimaryKeySqlParameterSource(userId));
	}

	static UnsuccessfulLoginLockoutDTO translateDBOToDTO(DBOUnsuccessfulLoginLockout dbo){
		if(dbo == null){
			return null;
		}

		UnsuccessfulLoginLockoutDTO dto = new UnsuccessfulLoginLockoutDTO(dbo.getUserId())
				.withLockoutExpiration(dbo.getLockoutExpiration())
				.withUnsuccessfulLoginCount(dbo.getUnsuccessfulLoginCount());
		return dto;
	}

	static DBOUnsuccessfulLoginLockout translateDTOToDBO(UnsuccessfulLoginLockoutDTO dto){
		DBOUnsuccessfulLoginLockout dbo = new DBOUnsuccessfulLoginLockout();
		dbo.setLockoutExpiration(dto.getLockoutExpiration());
		dbo.setUnsuccessfulLoginCount(dto.getUnsuccessfulLoginCount());
		dbo.setUserId(dto.getUserId());
		return dbo;
	}
}
