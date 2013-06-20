package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STORAGE_QUOTA_OWNER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_STORAGE_QUOTA;

import java.util.List;

import org.sagebionetworks.ids.ETagGenerator;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.StorageQuotaDao;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOStorageQuota;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.storage.StorageQuota;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class DBOStorageQuotaDaoImpl implements StorageQuotaDao {

	private static final String SELECT_QUOTA_FOR_USER =
			"SELECT * FROM " + TABLE_STORAGE_QUOTA +
			" WHERE " + COL_STORAGE_QUOTA_OWNER_ID + " = :" + COL_STORAGE_QUOTA_OWNER_ID;

	private static final RowMapper<DBOStorageQuota> rowMapper = (new DBOStorageQuota()).getTableMapping();

	@Autowired
	private ETagGenerator eTagGenerator;

	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void setQuota(StorageQuota quota) {

		if (quota == null) {
			throw new IllegalArgumentException("Storage quota cannot be null.");
		}
		final String userId = quota.getOwnerId();
		if (userId == null || userId.isEmpty()) {
			throw new IllegalArgumentException("User ID cannot be null or empty.");
		}
		final Long quotaInMb = quota.getQuotaInMb();
		if (quotaInMb == null) {
			throw new IllegalArgumentException("Quota cannot be null.");
		}
		if (quotaInMb < 0) {
			throw new IllegalArgumentException("Storage quota must be >= 0.");
		}

		final DBOStorageQuota dboInUpdate = new DBOStorageQuota();
		final Long userIdLong = KeyFactory.stringToKey(userId);
		dboInUpdate.setOwnerId(userIdLong);
		dboInUpdate.seteTag(eTagGenerator.generateETag());
		dboInUpdate.setQuotaInMb(quotaInMb.intValue());

		DBOStorageQuota dbo = getQuotaForUser(userIdLong);
		if (dbo != null) {
			String currEtag = dbo.geteTag();
			if (!currEtag.equals(quota.getEtag())) {
				throw new ConflictingUpdateException("E-tags do not match.");
			}
			basicDao.update(dboInUpdate);
		} else {
			basicDao.createNew(dboInUpdate);
		}
	}

	@Override
	public StorageQuota getQuota(String userId) {

		if (userId == null || userId.isEmpty()) {
			throw new IllegalArgumentException("User ID cannot be null or empty.");
		}

		final Long userIdLong = KeyFactory.stringToKey(userId);
		DBOStorageQuota dbo = getQuotaForUser(userIdLong);
		if (dbo == null) {
			return null;
		} else {
			StorageQuota dto = new StorageQuota();
			dto.setOwnerId(dbo.getOwnerId().toString());
			dto.setEtag(dbo.geteTag());
			dto.setQuotaInMb(dbo.getQuotaInMb().longValue());
			return dto;
		}
	}

	private DBOStorageQuota getQuotaForUser(Long userId) {
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue(COL_STORAGE_QUOTA_OWNER_ID, userId);
		List<DBOStorageQuota> dboList = simpleJdbcTemplate.query(
				SELECT_QUOTA_FOR_USER, rowMapper, paramMap);
		if (dboList == null || dboList.size() == 0) {
			return null;
		}
		return dboList.get(0);
	}
}
