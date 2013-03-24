package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOI_DOI_STATUS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOI_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOI_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOI_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOI_OBJECT_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DOI;

import java.sql.Timestamp;
import java.util.List;

import org.joda.time.DateTime;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.DoiDao;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBODoi;
import org.sagebionetworks.repo.model.doi.Doi;
import org.sagebionetworks.repo.model.doi.DoiObjectType;
import org.sagebionetworks.repo.model.doi.DoiStatus;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class DBODoiDaoImpl implements DoiDao {

	private static final String SELECT_DOI =
			"SELECT * FROM " + TABLE_DOI + " WHERE "
			+ COL_DOI_OBJECT_ID + " = :" + COL_DOI_OBJECT_ID + " AND "
			+ COL_DOI_OBJECT_TYPE + " = :" + COL_DOI_OBJECT_TYPE + " AND "
			+ COL_DOI_OBJECT_VERSION + " = :" + COL_DOI_OBJECT_VERSION;

	private static final String SELECT_DOI_ID =
			"SELECT " + COL_DOI_ID + " FROM " + TABLE_DOI + " WHERE "
			+ COL_DOI_OBJECT_ID + " = :" + COL_DOI_OBJECT_ID + " AND "
			+ COL_DOI_OBJECT_TYPE + " = :" + COL_DOI_OBJECT_TYPE + " AND "
			+ COL_DOI_OBJECT_VERSION + " = :" + COL_DOI_OBJECT_VERSION;

	private static final String UPDATE_DOI_STATUS =
			"UPDATE " + TABLE_DOI
			+ " SET " + COL_DOI_DOI_STATUS + " = :" + COL_DOI_DOI_STATUS
			+ " WHERE " + COL_DOI_ID + " = :" + COL_DOI_ID;

	private static final RowMapper<DBODoi> rowMapper = (new DBODoi()).getTableMapping();

	@Autowired private DBOBasicDao basicDao;
	@Autowired private SimpleJdbcTemplate simpleJdbcTemplate;

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public Doi createDoi(final String userGroupId, final String objectId,
			final DoiObjectType objectType, final Long versionNumber, final DoiStatus doiStatus)
			throws NotFoundException, UnauthorizedException, DatastoreException {

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
		dbo.setObjectId(KeyFactory.stringToKey(objectId));
		dbo.setDoiObjectType(objectType);
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
		dbo = basicDao.createNew(dbo);
		return convertToDto(dbo);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public Doi updateDoiStatus(final String objectId, final DoiObjectType objectType,
			final Long versionNumber, final DoiStatus doiStatus) throws NotFoundException,
			UnauthorizedException, DatastoreException {

		if (objectId == null || objectId.isEmpty()) {
			throw new IllegalArgumentException("Object ID cannot be null nor empty.");
		}
		if (objectType == null) {
			throw new IllegalArgumentException("Object type cannot be null.");
		}
		if (doiStatus == null) {
			throw new IllegalArgumentException("DOI status cannot be null.");
		}

		// Select the primary key first
		MapSqlParameterSource selectMap = new MapSqlParameterSource();
		selectMap.addValue(COL_DOI_OBJECT_ID, objectId);
		selectMap.addValue(COL_DOI_OBJECT_TYPE, objectType);
		selectMap.addValue(COL_DOI_OBJECT_VERSION, versionNumber);
		Object id = simpleJdbcTemplate.queryForObject(SELECT_DOI_ID, rowMapper, selectMap);

		// Then update by primary key
		MapSqlParameterSource updateMap = new MapSqlParameterSource();
		updateMap.addValue(COL_DOI_ID, id);
		updateMap.addValue(COL_DOI_DOI_STATUS, doiStatus);
		simpleJdbcTemplate.update(UPDATE_DOI_STATUS, updateMap);

		return getDoi(objectId, objectType, versionNumber);
	}

	@Override
	public Doi getDoi(String objectId, DoiObjectType objectType,
			Long versionNumber) throws NotFoundException,
			UnauthorizedException, DatastoreException {

		if (objectId == null || objectId.isEmpty()) {
			throw new IllegalArgumentException("Object ID cannot be null nor empty.");
		}
		if (objectType == null) {
			throw new IllegalArgumentException("Object type cannot be null.");
		}

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue(COL_DOI_OBJECT_ID, objectId);
		paramMap.addValue(COL_DOI_OBJECT_TYPE, objectType);
		paramMap.addValue(COL_DOI_OBJECT_VERSION, versionNumber);
		List<DBODoi> dboList = simpleJdbcTemplate.query(SELECT_DOI, rowMapper, paramMap);
		if (dboList == null ||dboList.size() == 0) {
			return null;
		}

		return convertToDto(dboList.get(0));
	}

	private Doi convertToDto(DBODoi dbo) {
		Doi dto = new Doi();
		dto.setCreatedBy(dbo.getCreatedBy().toString());
		dto.setCreatedOn(dbo.getCreatedOn());
		dto.setDoiStatus(DoiStatus.valueOf(dbo.getDoiStatus()));
		dto.setId(dbo.getId().toString());
		final DoiObjectType objectType = DoiObjectType.valueOf(dbo.getDoiObjectType());
		if (DoiObjectType.ENTITY.equals(objectType)) {
			dto.setObjectId(KeyFactory.keyToString(dbo.getObjectId()));
		} else {
			dto.setObjectId(dbo.getObjectId().toString());
		}
		dto.setDoiObjectType(objectType);
		dto.setObjectVersion(dbo.getObjectVersion());
		dto.setUpdatedOn(dbo.getUpdatedOn());
		return dto;
	}
}
