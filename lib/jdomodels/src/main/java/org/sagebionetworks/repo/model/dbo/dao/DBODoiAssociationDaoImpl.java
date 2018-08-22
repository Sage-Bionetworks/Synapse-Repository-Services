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
import org.sagebionetworks.repo.model.DoiAssociationDao;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBODoi;
import org.sagebionetworks.repo.model.doi.v2.DoiAssociation;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.transactions.NewWriteTransaction;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * Implementation of the DAO for the DOI Association objects (DOI v2)
 */
public class DBODoiAssociationDaoImpl implements DoiAssociationDao {

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
	public DoiAssociation createDoiAssociation(DoiAssociation dto) {
		Long newId = idGenerator.generateNewId(IdType.DOI_ID);
		dto.setAssociationId(newId.toString());
		dto.setEtag(UUID.randomUUID().toString());
		// MySQL TIMESTAMP only keeps seconds (not ms)
		// so for consistency we only write seconds
		DateTime dt = DateTime.now();
		long nowInSeconds = dt.getMillis() - dt.getMillisOfSecond();
		Timestamp now = new Timestamp(nowInSeconds);

		dto.setAssociatedOn(now);
		dto.setUpdatedOn(now);
		DBODoi dbo = DoiUtils.convertToDbo(dto);
		basicDao.createNew(dbo);

		return getDoiAssociation(newId.toString());
	}

	@WriteTransaction
	@Override
	public DoiAssociation updateDoiAssociation(DoiAssociation dto) {
		// MySQL TIMESTAMP only keeps seconds (not ms)
		// so for consistency we only write seconds
		DateTime dt = DateTime.now();
		long nowInSeconds = dt.getMillis() - dt.getMillisOfSecond();
		Timestamp now = new Timestamp(nowInSeconds);

		dto.setEtag(UUID.randomUUID().toString());
		dto.setUpdatedOn(now);
		DBODoi dbo = DoiUtils.convertToDbo(dto);

		basicDao.update(dbo);

		return getDoiAssociation(dto.getAssociationId());
	}

	@Override
	public DoiAssociation getDoiAssociation(String id) throws NotFoundException {
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue(COL_DOI_ID, KeyFactory.stringToKey(id));
		DBODoi dbo = null;
		try {
			dbo = namedJdbcTemplate.queryForObject(SELECT_DOI_BY_ID, paramMap, rowMapper);
		} catch (IncorrectResultSizeDataAccessException e) {
			handleIncorrectResultSizeException(e);
		}
		return DoiUtils.convertToDtoV2(dbo);
	}

	@Override
	public DoiAssociation getDoiAssociation(String objectId, ObjectType objectType, Long versionNumber) throws NotFoundException {
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
		return DoiUtils.convertToDtoV2(dbo);
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