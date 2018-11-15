package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_TYPE_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_TYPE_OBJECT_TYPE;
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
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

public class DataTypeDaoImpl implements DataTypeDao {
	
	private static final String SQL_TRUNCATE_ALL = "DELETE FROM "+TABLE_DATA_TYPE+" WHERE "+COL_DATA_TYPE_OBJECT_ID+" > -1";

	private static final String SQL_DELETE_OBJECT = "DELETE FROM "+TABLE_DATA_TYPE+" WHERE "+COL_DATA_TYPE_OBJECT_ID+" = ? AND "+COL_DATA_TYPE_OBJECT_TYPE+" = ?";

	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private DBOBasicDao dboBasicDao;
	@Autowired
	private IdGenerator idgenerator;

	@WriteTransactionReadCommitted
	@Override
	public DataTypeResponse changeDataType(Long userId, String objectIdString, ObjectType objectType, DataType dataType) {
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
	 * @param objectIdString
	 * @param objectType
	 * @return
	 */
	private DataTypeResponse getDataTypeResponse(String objectIdString, ObjectType objectType) {
		Long objectId = KeyFactory.stringToKey(objectIdString);
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("objectId", objectId);
		params.addValue("objectType", objectType.name());
		DBODataType dbo =  dboBasicDao.getObjectByPrimaryKey(DBODataType.class, params);
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

}
