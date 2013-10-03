package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_COLUMN_MODEL;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.evaluation.dbo.DBOConstants;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.table.ColumnModelUtlis;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOColumnModel;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Database implementation of the ColumnModelDAO interface.
 * 
 * @author John
 *
 */
public class DBOColumnModelDAOImpl implements ColumnModelDAO {

	private static final String SQL_SELECT_ID_WHERE_HASH_EQUALS = "SELECT "+COL_CM_ID+" FROM "+TABLE_COLUMN_MODEL+" WHERE "+COL_CM_HASH+" = ?";
	@Autowired
	private DBOBasicDao basicDao;
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	@Autowired
	private IdGenerator idGenerator;
	
	private static RowMapper<DBOColumnModel> ROW_MAPPER = new DBOColumnModel().getTableMapping();

	@Override
	public List<ColumnModel> listColumnModels(String namePrefix, long limit,
			long offset) {
		// TODO Auto-generated method stub
		return null;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public ColumnModel createColumnModel(ColumnModel model) throws DatastoreException, NotFoundException {
		// Convert to the DBO
		DBOColumnModel dbo = ColumnModelUtlis.createDBOFromDTO(model);
		// check to see if a column model already exists with this hash.
		String existingId = getColumnForHash(dbo.getHash());
		if(existingId != null){
			// a column already exists with this same hash.
			return getColumnModel(existingId);
		}
		// This is a new unique hash.
		Long id = idGenerator.generateNewId(TYPE.COLUMN_MODEL_ID);
		dbo.setId(id);
		// Save it.
		basicDao.createNew(dbo);
		return getColumnModel(Long.toString(id));
	}

	@Override
	public ColumnModel getColumnModel(String id) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(DBOConstants.PARAM_EVALUATION_ID, id);
		DBOColumnModel dbo = basicDao.getObjectByPrimaryKey(DBOColumnModel.class, param);
		return ColumnModelUtlis.createDTOFromDBO(dbo);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void delete(String id) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(DBOConstants.PARAM_EVALUATION_ID, id);
		basicDao.deleteObjectByPrimaryKey(DBOColumnModel.class, param);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public boolean bindColumnToObject(String columnId, String objectId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<String> listObjectsBoundToColumn(Set<String> columnIds,
			long limit, long offest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getColumnForHash(String hash) {
		try {
			long id = simpleJdbcTemplate.queryForLong(SQL_SELECT_ID_WHERE_HASH_EQUALS, hash);
			return Long.toString(id);
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	@Override
	public List<ColumnModel> getColumnModel(List<String> ids) throws DatastoreException, NotFoundException {
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		parameters.addValue("ids", ids);
		List<DBOColumnModel> dbos = simpleJdbcTemplate.query("SELECT * FROM "+TABLE_COLUMN_MODEL+" WHERE "+COL_CM_ID+" IN ( :ids ) ORDER BY "+COL_CM_NAME, ROW_MAPPER, parameters);
		// Convert to DTOs
		List<ColumnModel> results = new LinkedList<ColumnModel>();
		for(DBOColumnModel dbo: dbos){
			results.add(ColumnModelUtlis.createDTOFromDBO(dbo));
		}
		return results;
	}

}
