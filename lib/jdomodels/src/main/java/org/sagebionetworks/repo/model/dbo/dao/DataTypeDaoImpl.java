package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_TYPE_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_TYPE_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_TYPE_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DATA_TYPE;

import java.util.Date;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.DataType;
import org.sagebionetworks.repo.model.DataTypeResponse;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBODataType;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

public class DataTypeDaoImpl implements DataTypeDao {

	/**
	 * The default type returned when an object's type has not been set.
	 */
	public static final DataType DEFAULT_DATA_TYPE = DataType.SENSITIVE_DATA;

	private static final String SQL_SELECT_TYPE = "SELECT " + COL_DATA_TYPE_TYPE + " FROM " + TABLE_DATA_TYPE
			+ " WHERE " + COL_DATA_TYPE_OBJECT_ID + " = ? AND " + COL_DATA_TYPE_OBJECT_TYPE + " = ?";

	private static final String SQL_TRUNCATE_ALL = "DELETE FROM " + TABLE_DATA_TYPE + " WHERE "
			+ COL_DATA_TYPE_OBJECT_ID + " > -1";

	private static final String SQL_DELETE_OBJECT = "DELETE FROM " + TABLE_DATA_TYPE + " WHERE "
			+ COL_DATA_TYPE_OBJECT_ID + " = ? AND " + COL_DATA_TYPE_OBJECT_TYPE + " = ?";

	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private DBOBasicDao dboBasicDao;
	@Autowired
	private IdGenerator idgenerator;

	@WriteTransaction
	@Override
	public DataTypeResponse changeDataType(Long userId, String objectIdString, ObjectType objectType,
			DataType dataType) {
		ValidateArgument.required(userId, "userId");
		ValidateArgument.required(objectIdString, "objectIdString");
		ValidateArgument.required(objectType, "objectType");
		ValidateArgument.required(dataType, "dataType");
		Long objectId = KeyFactory.stringToKey(objectIdString);
		// remove any existing row for this object.
		jdbcTemplate.update(SQL_DELETE_OBJECT, objectId, objectType.name());
		DBODataType dbo = new DBODataType();
		dbo.setId(idgenerator.generateNewId(IdType.DATA_TYPE_ID));
		dbo.setObjectId(objectId);
		dbo.setObjectType(objectType.name());
		dbo.setDataType(dataType.name());
		dbo.setUpdatedBy(userId);
		dbo.setUpdatedOn(System.currentTimeMillis());
		dboBasicDao.createNew(dbo);
		return getDataTypeResponse(objectIdString, objectType);
	}

	/**
	 * Get the full DataTypeResponse for the given object.
	 * 
	 * @param objectIdString
	 * @param objectType
	 * @return
	 */
	private DataTypeResponse getDataTypeResponse(String objectIdString, ObjectType objectType) {
		Long objectId = KeyFactory.stringToKey(objectIdString);
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("objectId", objectId);
		params.addValue("objectType", objectType.name());
		DBODataType dbo = dboBasicDao.getObjectByPrimaryKey(DBODataType.class, params);
		DataTypeResponse dto = new DataTypeResponse();
		dto.setObjectId(objectIdString);
		dto.setObjectType(ObjectType.valueOf(dbo.getObjectType()));
		dto.setDataType(DataType.valueOf(dbo.getDataType()));
		dto.setUpdatedBy(dbo.getUpdatedBy().toString());
		dto.setUpdatedOn(new Date(dbo.getUpdatedOn()));
		return dto;
	}

	@Override
	public void truncateAllData() {
		jdbcTemplate.update(SQL_TRUNCATE_ALL);
	}

	@Override
	public DataType getObjectDataType(String objectIdString, ObjectType objectType) {
		ValidateArgument.required(objectIdString, "objectIdString");
		ValidateArgument.required(objectType, "objectType");
		Long objectId = KeyFactory.stringToKey(objectIdString);
		try {
			return DataType
					.valueOf(jdbcTemplate.queryForObject(SQL_SELECT_TYPE, String.class, objectId, objectType.name()));
		} catch (EmptyResultDataAccessException e) {
			return DEFAULT_DATA_TYPE;
		}
	}

}
