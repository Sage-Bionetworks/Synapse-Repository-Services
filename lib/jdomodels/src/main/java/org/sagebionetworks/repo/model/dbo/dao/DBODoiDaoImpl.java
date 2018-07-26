package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.dbo.dao.DoiUtils.convertToDbo;
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
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBODoi;
import org.sagebionetworks.repo.model.doi.Doi;
import org.sagebionetworks.repo.transactions.NewWriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class DBODoiDaoImpl implements DoiDao {

	private static final String SELECT_DOI =
			"SELECT * FROM " + TABLE_DOI + " WHERE "
			+ COL_DOI_OBJECT_ID + " = :" + COL_DOI_OBJECT_ID + " AND "
			+ COL_DOI_OBJECT_TYPE + " = :" + COL_DOI_OBJECT_TYPE + " AND "
			+ COL_DOI_OBJECT_VERSION + " = :" + COL_DOI_OBJECT_VERSION;

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
	public Doi createDoi(Doi dto)
			throws DatastoreException {

		if (dto.getCreatedBy() == null) {
			throw new IllegalArgumentException("User/group ID cannot be null.");
		}
		if (dto.getObjectId() == null) {
			throw new IllegalArgumentException("Object ID cannot be null.");
		}
		if (dto.getObjectType() == null) {
			throw new IllegalArgumentException("Object type cannot be null.");
		}
		if (dto.getDoiStatus() == null) {
			throw new IllegalArgumentException("DOI status cannot be null.");
		}

		DBODoi dbo = convertToDbo(dto);
		dbo.setId(idGenerator.generateNewId(IdType.DOI_ID));
		dbo.setETag(UUID.randomUUID().toString());
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
	public Doi updateDoiStatus(Doi dto)
			throws NotFoundException, DatastoreException, ConflictingUpdateException {

		if (dto.getObjectId() == null) {
			throw new IllegalArgumentException("Object ID cannot be null nor empty.");
		}
		if (dto.getObjectType() == null) {
			throw new IllegalArgumentException("Object type cannot be null.");
		}
		if (dto.getDoiStatus() == null) {
			throw new IllegalArgumentException("DOI status cannot be null.");
		}
		DBODoi dbo = getDbo(dto);

		if (!dbo.getETag().equals(dto.getEtag())) {
			throw new ConflictingUpdateException("Etags do not match.");
		}
		dbo.setDoiStatus(dto.getDoiStatus());
		dbo.setETag(UUID.randomUUID().toString());
		boolean success = basicDao.update(dbo);
		if (!success) {
			throw new DatastoreException("Update failed for " + dbo);
		}
		return getDoi(dto);
	}

	@Override
	public Doi getDoi(Doi dto) throws NotFoundException, DatastoreException {

		if (dto.getObjectId() == null) {
			throw new IllegalArgumentException("Object ID cannot be null nor empty.");
		}
		if (dto.getObjectType() == null) {
			throw new IllegalArgumentException("Object type cannot be null.");
		}
		DBODoi dbo = null;
		dbo = getDbo(dto);
		return DoiUtils.convertToDto(dbo);
	}

	/**
	 * Retrieves a DOI DBO corresponding to the DTO supplied as a parameter.
 	 * @param dto The DTO containing object ID, type, and version number that
	 *            corresponds to an existing DBO.
	 * @return The existing DBO.
	 * @throws NotFoundException The DOI was not found in the database
	 * @throws DatastoreException More than one DOI was found when exactly
	 *                            one should have been found.
	 */
	private DBODoi getDbo (Doi dto)
			throws NotFoundException, DatastoreException {
		DBODoi translatedDbo = convertToDbo(dto);

		String sql = SELECT_DOI;
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue(COL_DOI_OBJECT_ID, translatedDbo.getObjectId());
		paramMap.addValue(COL_DOI_OBJECT_TYPE, translatedDbo.getObjectType());
		paramMap.addValue(COL_DOI_OBJECT_VERSION, translatedDbo.getObjectVersion());
		List<DBODoi> dboList = namedJdbcTemplate.query(sql, paramMap, rowMapper);
		if (dboList == null || dboList.size() == 0) {
			throw new NotFoundException("DOI not found for type " + dto.getObjectType()
					+ ", ID " + dto.getObjectId() + ", Version " + dto.getObjectVersion());
		}
		if (dboList.size() > 1) {
			String error = "Fetched back more than 1 DOI data object where exactly 1 is expected "
					+ " for " + sql + " and parameters " + paramMap.getValues().toString();
			throw new DatastoreException(error);
		}
		return (dboList.get(0));
	}
}
