package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOI_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOI_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOI_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOI_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOI_OBJECT_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DOI;

import java.sql.Timestamp;
import java.util.UUID;

import org.joda.time.DateTime;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
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
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@Deprecated
public class DBODoiDaoImpl implements DoiDao {

	private static final String SELECT_DOI_BY_ID =
			"SELECT * FROM " + TABLE_DOI + " WHERE "
					+ COL_DOI_ID + " = :" + COL_DOI_ID;

	private static final String SELECT_DOI_BY_ASSOCIATED_OBJECT =
			"SELECT * FROM " + TABLE_DOI + " WHERE "
					+ COL_DOI_OBJECT_ID + " = :" + COL_DOI_OBJECT_ID + " AND "
					+ COL_DOI_OBJECT_TYPE + " = :" + COL_DOI_OBJECT_TYPE + " AND "
					+ COL_DOI_OBJECT_VERSION + " = :" + COL_DOI_OBJECT_VERSION;

	private static final String SELECT_DOI_ETAG_FOR_UPDATE =
			"SELECT " + COL_DOI_ETAG + " FROM " + TABLE_DOI + " WHERE "
					+ COL_DOI_OBJECT_ID + " = :" + COL_DOI_OBJECT_ID + " AND "
					+ COL_DOI_OBJECT_TYPE + " = :" + COL_DOI_OBJECT_TYPE + " AND "
					+ COL_DOI_OBJECT_VERSION + " = :" + COL_DOI_OBJECT_VERSION +
					" FOR UPDATE ";

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
	public Doi createDoi(Doi dto) {
		Long newId = idGenerator.generateNewId(IdType.DOI_ID);
		dto.setId(newId.toString());
		dto.setEtag(UUID.randomUUID().toString());
		// MySQL TIMESTAMP only keeps seconds (not ms)
		// so for consistency we only write seconds
		DateTime dt = DateTime.now();
		long nowInSeconds = dt.getMillis() - dt.getMillisOfSecond();
		Timestamp now = new Timestamp(nowInSeconds);

		dto.setCreatedOn(now);
		dto.setUpdatedOn(now);
		DBODoi dbo = DoiUtils.convertToDbo(dto);
		basicDao.createNew(dbo);

		return getDoi(newId.toString());
	}

	/**
	 * Limits the transaction boundary to within the DOI DAO and runs with a new transaction.
	 * DOI client updating the DOI is an asynchronous call and must happen outside the transaction to
	 * avoid race conditions.
	 */
	@NewWriteTransaction
	@Override
	public Doi updateDoiStatus(String id, DoiStatus status) throws NotFoundException {
		// Etag was checked in manager, so we just have to update the object.
		Doi dto = getDoi(id);
		dto.setDoiStatus(status);
		dto.setEtag(UUID.randomUUID().toString());
		DBODoi dbo = DoiUtils.convertToDbo(dto);
		boolean success = basicDao.update(dbo);
		if (!success) {
			throw new DatastoreException("Update failed for " + dbo);
		}
		return getDoi(id);
	}

	@Override
	public Doi getDoi(String id) throws NotFoundException {
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue(COL_DOI_ID, KeyFactory.stringToKey(id));
		DBODoi dbo = null;
		try {
			dbo = namedJdbcTemplate.queryForObject(SELECT_DOI_BY_ID, paramMap, rowMapper);
		} catch (IncorrectResultSizeDataAccessException e) {
			handleIncorrectResultSizeException(e);
		}
		return DoiUtils.convertToDto(dbo);
	}

	@Override
	public Doi getDoi(String objectId, ObjectType objectType, Long versionNumber) throws NotFoundException {
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue(COL_DOI_OBJECT_ID, KeyFactory.stringToKey(objectId));
		paramMap.addValue(COL_DOI_OBJECT_TYPE, objectType.name());
		if (versionNumber == null) {
			paramMap.addValue(COL_DOI_OBJECT_VERSION, DBODoi.NULL_OBJECT_VERSION);
		} else {
			paramMap.addValue(COL_DOI_OBJECT_VERSION, versionNumber);
		}
		DBODoi dbo = null;
		try {
			dbo = namedJdbcTemplate.queryForObject(SELECT_DOI_BY_ASSOCIATED_OBJECT, paramMap, rowMapper);
		} catch (IncorrectResultSizeDataAccessException e) {
			handleIncorrectResultSizeException(e);
		}
		return DoiUtils.convertToDto(dbo);
	}

	@NewWriteTransaction
	@Override
	public String getEtagForUpdate(String objectId, ObjectType objectType, Long versionNumber) throws NotFoundException {
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue(COL_DOI_OBJECT_ID, KeyFactory.stringToKey(objectId));
		paramMap.addValue(COL_DOI_OBJECT_TYPE, objectType.name());
		if (versionNumber == null) {
			paramMap.addValue(COL_DOI_OBJECT_VERSION, DBODoi.NULL_OBJECT_VERSION);
		} else {
			paramMap.addValue(COL_DOI_OBJECT_VERSION, versionNumber);
		}
		String etag = null;
		try {
			etag = namedJdbcTemplate.queryForObject(SELECT_DOI_ETAG_FOR_UPDATE, paramMap, String.class);
		} catch (IncorrectResultSizeDataAccessException e) {
			handleIncorrectResultSizeException(e);
		}
		return etag;
	}

	static void handleIncorrectResultSizeException(IncorrectResultSizeDataAccessException e) throws NotFoundException {
		if (e.getActualSize() == 0) {
			throw new NotFoundException("Retrieved 0 rows when expecting exactly 1");
		} else {
			throw new IllegalStateException("Retrieved 2+ rows when expecting exactly 1");
		}
	}
}