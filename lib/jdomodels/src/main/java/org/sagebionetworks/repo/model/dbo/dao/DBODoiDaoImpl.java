package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOI_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOI_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOI_OBJECT_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DOI;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import org.joda.time.DateTime;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.DoiDao;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBODoi;
import org.sagebionetworks.repo.model.doi.Doi;
import org.sagebionetworks.repo.model.doi.DoiStatus;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.transactions.NewWriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.sagebionetworks.repo.transactions.WriteTransaction;

public class DBODoiDaoImpl implements DoiDao {

	private static final String SELECT_DOI =
			"SELECT * FROM " + TABLE_DOI + " WHERE "
			+ COL_DOI_OBJECT_ID + " = :" + COL_DOI_OBJECT_ID + " AND "
			+ COL_DOI_OBJECT_TYPE + " = :" + COL_DOI_OBJECT_TYPE + " AND "
			+ COL_DOI_OBJECT_VERSION + " = :" + COL_DOI_OBJECT_VERSION;

	private static final String SELECT_DOI_NULL_OBJECT_VERSION =
			"SELECT * FROM " + TABLE_DOI + " WHERE "
			+ COL_DOI_OBJECT_ID + " = :" + COL_DOI_OBJECT_ID + " AND "
			+ COL_DOI_OBJECT_TYPE + " = :" + COL_DOI_OBJECT_TYPE + " AND "
			+ COL_DOI_OBJECT_VERSION + " IS NULL";

	private static final RowMapper<DBODoi> rowMapper = (new DBODoi()).getTableMapping();

	@Autowired
	private IdGenerator idGenerator;

	@Autowired
	private DBOBasicDao basicDao;
	
	@Autowired
	private NamedParameterJdbcTemplate namedJdbcTemplate;

	/**
	 * Limits the transaction boundary to within the DOI DAO and runs with a new transaction.
	 * DOI client creating the DOI is an asynchronous call and must happen outside the transaction to
	 * avoid race conditions.
	 */
	@NewWriteTransaction
	@Override
	public Doi createDoi(final String userGroupId, final String objectId,
			final ObjectType objectType, final Long versionNumber, final DoiStatus doiStatus)
			throws DatastoreException {

		if (userGroupId == null || userGroupId.isEmpty()) {
			throw new IllegalArgumentException("User group ID cannot be null nor empty.");
		}
		if (objectId == null || objectId.isEmpty()) {
			throw new IllegalArgumentException("Object ID cannot be null nor empty.");
		}
		if (objectType == null) {
			throw new IllegalArgumentException("Object type cannot be null.");
		}
		if (doiStatus == null) {
			throw new IllegalArgumentException("DOI status cannot be null.");
		}

		DBODoi dbo = new DBODoi();
		dbo.setId(idGenerator.generateNewId(IdType.DOI_ID));
		dbo.setETag(UUID.randomUUID().toString());
		dbo.setObjectId(KeyFactory.stringToKey(objectId));
		dbo.setObjectType(objectType);
		dbo.setObjectVersion(versionNumber);
		dbo.setDoiStatus(doiStatus);
		dbo.setCreatedBy(KeyFactory.stringToKey(userGroupId));
		DateTime dt = DateTime.now();
		// MySQL TIMESTAMP only keeps seconds (not ms)
		// so for consistency we only write seconds
		long nowInSeconds = dt.getMillis() - dt.getMillisOfSecond();
		Timestamp now = new Timestamp(nowInSeconds);
		dbo.setCreatedOn(now);
		dbo.setUpdatedOn(now);
		basicDao.createNew(dbo);
		return DoiUtils.convertToDto(dbo);
	}

	/**
	 * Limits the transaction boundary to within the DOI DAO and runs with a new transaction.
	 * DOI client updating the DOI is an asynchronous call and must happen outside the transaction to
	 * avoid race conditions.
	 */
	@NewWriteTransaction
	@Override
	public Doi updateDoiStatus(final String objectId, final ObjectType objectType,
			final Long versionNumber, final DoiStatus doiStatus, String etag)
			throws NotFoundException, DatastoreException, ConflictingUpdateException {

		if (objectId == null || objectId.isEmpty()) {
			throw new IllegalArgumentException("Object ID cannot be null nor empty.");
		}
		if (objectType == null) {
			throw new IllegalArgumentException("Object type cannot be null.");
		}
		if (doiStatus == null) {
			throw new IllegalArgumentException("DOI status cannot be null.");
		}

		DBODoi dbo = getDbo(objectId, objectType, versionNumber);
		if (!dbo.getETag().equals(etag)) {
			throw new ConflictingUpdateException("Etags do not match.");
		}
		dbo.setDoiStatus(doiStatus);
		boolean success = basicDao.update(dbo);
		if (!success) {
			throw new DatastoreException("Update failed for " + dbo);
		}
		return getDoi(objectId, objectType, versionNumber);
	}

	@Override
	public Doi getDoi(final String objectId, final ObjectType objectType,
			final Long versionNumber) throws NotFoundException, DatastoreException {

		if (objectId == null || objectId.isEmpty()) {
			throw new IllegalArgumentException("Object ID cannot be null nor empty.");
		}
		if (objectType == null) {
			throw new IllegalArgumentException("Object type cannot be null.");
		}
		DBODoi dbo = getDbo(objectId, objectType, versionNumber);
		return DoiUtils.convertToDto(dbo);
	}

	private DBODoi getDbo (String objectId, ObjectType objectType, Long versionNumber)
			throws NotFoundException, DatastoreException {

		String sql = (versionNumber == null ? SELECT_DOI_NULL_OBJECT_VERSION : SELECT_DOI);
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue(COL_DOI_OBJECT_ID, KeyFactory.stringToKey(objectId));
		paramMap.addValue(COL_DOI_OBJECT_TYPE, objectType.name());
		if (versionNumber != null) {
			paramMap.addValue(COL_DOI_OBJECT_VERSION, versionNumber);
		}
		List<DBODoi> dboList = namedJdbcTemplate.query(sql, paramMap, rowMapper);
		if (dboList == null || dboList.size() == 0) {
			throw new NotFoundException("DOI not found for type " + objectType
					+ ", ID " + objectId + ", Version " + versionNumber);
		}
		if (dboList.size() > 1) {
			String error = "Fetched back more than 1 DOI data object where exactly 1 is expected "
					+ " for " + sql + " and parameters " + paramMap.getValues().toString();
			throw new DatastoreException(error);
		}
		return (dboList.get(0));
	}
}
