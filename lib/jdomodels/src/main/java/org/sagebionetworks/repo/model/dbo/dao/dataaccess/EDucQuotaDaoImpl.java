package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_EDUC_QUOTA_ACCESS_REQUIREMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_EDUC_QUOTA_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_EDUC_QUOTA_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_EDUC_QUOTA_USER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_EDUC_QUOTA;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EDucQuotaDaoImpl implements EDucQuotaDao {

	private final IdGenerator idGenerator;
	private final JdbcTemplate jdbcTemplate;
	private final DBOBasicDao basicDao;

	public EDucQuotaDaoImpl(IdGenerator idGenerator, JdbcTemplate jdbcTemplate, DBOBasicDao basicDao) {
		this.idGenerator = idGenerator;
		this.jdbcTemplate = jdbcTemplate;
		this.basicDao = basicDao;
	}

	@WriteTransaction
	@Override
	public DBOEDucQuota create(Long userId, Long accessRequirementId, String envelopeId) {
		ValidateArgument.required(userId, "userId");
		ValidateArgument.required(accessRequirementId, "accessRequirementId");
		ValidateArgument.required(envelopeId, "envelopeId");

		DBOEDucQuota dbo = new DBOEDucQuota();
		dbo.setId(idGenerator.generateNewId(IdType.EDUC_QUOTA_ID));
		dbo.setUserId(userId);
		dbo.setAccessRequirementId(accessRequirementId);
		dbo.setCreatedOn(System.currentTimeMillis());
		dbo.setEnvelopeId(envelopeId);

		return basicDao.createNew(dbo);
	}

	@Override
	public long getCount(Long userId, Long accessRequirementId, long fromEpochMs, long toEpochMs) {
		ValidateArgument.required(userId, "userId");
		ValidateArgument.required(accessRequirementId, "accessRequirementId");

		return jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM " + TABLE_EDUC_QUOTA
						+ " WHERE " + COL_EDUC_QUOTA_USER_ID + " = ?"
						+ " AND " + COL_EDUC_QUOTA_ACCESS_REQUIREMENT_ID + " = ?"
						+ " AND " + COL_EDUC_QUOTA_CREATED_ON + " >= ?"
						+ " AND " + COL_EDUC_QUOTA_CREATED_ON + " < ?",
				Long.class,
				userId, accessRequirementId, fromEpochMs, toEpochMs);
	}

	@Override
	public long getGlobalCount(long fromEpochMs, long toEpochMs) {
		return jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM " + TABLE_EDUC_QUOTA
						+ " WHERE " + COL_EDUC_QUOTA_CREATED_ON + " >= ?"
						+ " AND " + COL_EDUC_QUOTA_CREATED_ON + " < ?",
				Long.class,
				fromEpochMs, toEpochMs);
	}

	@WriteTransaction
	@Override
	public void delete(Long id) {
		ValidateArgument.required(id, "id");
		jdbcTemplate.update("DELETE FROM " + TABLE_EDUC_QUOTA + " WHERE " + COL_EDUC_QUOTA_ID + " = ?", id);
	}

	@WriteTransaction
	@Override
	public int deleteByUserAndAccessRequirement(Long userId, Long accessRequirementId) {
		ValidateArgument.required(userId, "userId");
		ValidateArgument.required(accessRequirementId, "accessRequirementId");
		return jdbcTemplate.update("DELETE FROM " + TABLE_EDUC_QUOTA
				+ " WHERE " + COL_EDUC_QUOTA_USER_ID + " = ?"
				+ " AND " + COL_EDUC_QUOTA_ACCESS_REQUIREMENT_ID + " = ?",
				userId, accessRequirementId);
	}

	@Override
	public void truncateAll() {
		jdbcTemplate.update("DELETE FROM " + TABLE_EDUC_QUOTA + " WHERE " + COL_EDUC_QUOTA_ID + " > -1");
	}
}
