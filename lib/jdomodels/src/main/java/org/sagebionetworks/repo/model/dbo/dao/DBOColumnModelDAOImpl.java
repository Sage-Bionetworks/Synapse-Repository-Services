package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_BOUND_CM_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CM_HASH;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_BOUND_COLUMN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.evaluation.dbo.DBOConstants;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.DMLUtils;
import org.sagebionetworks.repo.model.dbo.persistence.table.ColumnModelUtlis;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOBoundColumn;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOColumnModel;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
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

	private static final String SQL_SELECT_COLUMNS_WITH_NAME_PREFIX_COUNT = "SELECT COUNT(*) FROM "+TABLE_COLUMN_MODEL+" WHERE "+COL_CM_NAME+" LIKE ? ";
	private static final String SQL_SELECT_COLUMNS_WITH_NAME_PREFIX = "SELECT * FROM "+TABLE_COLUMN_MODEL+" WHERE "+COL_CM_NAME+" LIKE ? ORDER BY "+COL_CM_NAME+" LIMIT ? OFFSET ?";
	private static final String SQL_TRUNCATE_BOUND_COLUMNS = "DELETE FROM "+TABLE_BOUND_COLUMN+" WHERE "+COL_BOUND_CM_COLUMN_ID+" >= 0";
	private static final String SQL_SELECT_EXISTING_BOUND_FOR_OBJECT_ID = "SELECT * FROM "+TABLE_BOUND_COLUMN+" WHERE "+COL_BOUND_CM_OBJECT_ID+" = ? ";
	private static final String SQL_SELECT_COLUMNS_FOR_IDS = "SELECT * FROM "+TABLE_COLUMN_MODEL+" WHERE "+COL_CM_ID+" IN ( :ids ) ORDER BY "+COL_CM_NAME;
	private static final String SQL_SELECT_ID_WHERE_HASH_EQUALS = "SELECT "+COL_CM_ID+" FROM "+TABLE_COLUMN_MODEL+" WHERE "+COL_CM_HASH+" = ?";
	private static final String SQL_CREATE_OR_UPDATE_BOUND_COLUMN_BATCH = DMLUtils.getBatchInsertOrUdpate(new DBOBoundColumn().getTableMapping());
	
	@Autowired
	private DBOBasicDao basicDao;
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	@Autowired
	private IdGenerator idGenerator;
	
	private static RowMapper<DBOColumnModel> ROW_MAPPER = new DBOColumnModel().getTableMapping();
	private static RowMapper<DBOBoundColumn> BOUND_ROW_MAPPER = new DBOBoundColumn().getTableMapping();
	private static RowMapper<String> ENTITY_ID_MAPPER = new RowMapper<String>() {
		@Override
		public String mapRow(ResultSet rs, int rowNum) throws SQLException {
			long id =  rs.getLong(COL_BOUND_CM_OBJECT_ID);
			return KeyFactory.keyToString(id);
		}
	};

	@Override
	public List<ColumnModel> listColumnModels(String namePrefix, long limit, long offset) {
		String likeString = preparePrefix(namePrefix);
		List<DBOColumnModel> dbos = simpleJdbcTemplate.query(SQL_SELECT_COLUMNS_WITH_NAME_PREFIX, ROW_MAPPER, likeString, limit, offset);
		// Convert to DTOs
		List<ColumnModel> results = new LinkedList<ColumnModel>();
		for(DBOColumnModel dbo: dbos){
			results.add(ColumnModelUtlis.createDTOFromDBO(dbo));
		}
		return results;
	}

	/**
	 * @param namePrefix
	 * @return
	 */
	public String preparePrefix(String namePrefix) {
		if(namePrefix == null){
			namePrefix = "";
		}
		String likeString = namePrefix.toLowerCase()+"%";
		return likeString;
	}
	
	@Override
	public long listColumnModelsCount(String namePrefix) {
		String likeString = preparePrefix(namePrefix);
		return simpleJdbcTemplate.queryForLong(SQL_SELECT_COLUMNS_WITH_NAME_PREFIX_COUNT, likeString);
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
	public int bindColumnToObject(Set<String> newCurrentColumnIds, String objectIdString) {
		if(newCurrentColumnIds == null) throw new IllegalArgumentException("columnIds cannot be null");
		if(objectIdString == null) throw new IllegalArgumentException("objectId cannot be null");
		Long objectId = KeyFactory.stringToKey(objectIdString);
		// first get all of the existing rows bound to this object
		List<DBOBoundColumn> existing = simpleJdbcTemplate.query(SQL_SELECT_EXISTING_BOUND_FOR_OBJECT_ID, BOUND_ROW_MAPPER, objectId);
		// This utility will prepare the new values to insert/update into the database.
		List<DBOBoundColumn> newRows = ColumnModelUtlis.prepareNewBoundColumns(objectId, existing, newCurrentColumnIds);
		// Build up the batch
		SqlParameterSource[] namedParameters = new BeanPropertySqlParameterSource[newRows.size()];
		for(int i=0; i<newRows.size(); i++){
			namedParameters[i] = new BeanPropertySqlParameterSource(newRows.get(i));
		}
		// execute the batch
		int [] results = simpleJdbcTemplate.batchUpdate(SQL_CREATE_OR_UPDATE_BOUND_COLUMN_BATCH, namedParameters);
		// How many rows changed
		int count = 0;
		for(int rowcount: results){
			count+=rowcount;
		}
		return count;
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
		List<DBOColumnModel> dbos = simpleJdbcTemplate.query(SQL_SELECT_COLUMNS_FOR_IDS, ROW_MAPPER, parameters);
		// Convert to DTOs
		List<ColumnModel> results = new LinkedList<ColumnModel>();
		for(DBOColumnModel dbo: dbos){
			results.add(ColumnModelUtlis.createDTOFromDBO(dbo));
		}
		return results;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void truncateBoundColumns() {
		simpleJdbcTemplate.update(SQL_TRUNCATE_BOUND_COLUMNS);
	}

	@Override
	public List<String> listObjectsBoundToColumn(Set<String> columnIds,	boolean currentOnly, long limit, long offset) {
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		// With currently only we have one additional condition.
		String sql = builderListObjectsSql(columnIds, currentOnly, limit, offset, parameters, false);
		// Run the query
		return simpleJdbcTemplate.query(sql, ENTITY_ID_MAPPER, parameters);
	}
	
	@Override
	public long listObjectsBoundToColumnCount(Set<String> columnIds, boolean currentOnly) {
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		// With currently only we have one additional condition.
		String sql = builderListObjectsSql(columnIds, currentOnly, -1, -1, parameters, true);
		System.out.println(sql);
		return simpleJdbcTemplate.queryForLong(sql, parameters);
	}

	/**
	 * This is a fairly complicated query that involves an alias for each columnId.
	 * 
	 * @param columnIds
	 * @param currentOnly
	 * @param limit
	 * @param offset
	 * @param parameters
	 * @return
	 */
	private static String builderListObjectsSql(Set<String> columnIds,	boolean currentOnly, long limit, long offset, MapSqlParameterSource parameters, boolean count){
		// We build the from, where, and join at the same time with a single loop.
		StringBuilder from = new StringBuilder();
		from.append(" FROM");
		StringBuilder join = new StringBuilder();
		StringBuilder where = new StringBuilder();
		where.append(" WHERE");
		Iterator<String> it = columnIds.iterator();
		for(int i=0; i<columnIds.size(); i++){
			String tableAlias = "A"+i;
			if(i > 0){
				from.append(",");
				join.append(" AND A"+(i-1)+"."+COL_BOUND_CM_OBJECT_ID+" = "+tableAlias+"."+COL_BOUND_CM_OBJECT_ID);
				where.append(" AND");
			}
			from.append(" "+TABLE_BOUND_COLUMN+" "+tableAlias);
			where.append(" "+tableAlias+"."+COL_BOUND_CM_COLUMN_ID+" = :c"+i);
			long colId = Long.parseLong(it.next());
			parameters.addValue("c"+i, colId);
			if(currentOnly){
				where.append(" AND "+tableAlias+"."+COL_BOUND_CM_IS_CURRENT+" = TRUE");
			}
		}
		// put all of the parts together.
		StringBuilder builder = new StringBuilder();
		builder.append("SELECT");
		if(count){
			builder.append(" COUNT(DISTINCT A0."+COL_BOUND_CM_OBJECT_ID+")");
		}else{
			builder.append(" DISTINCT A0."+COL_BOUND_CM_OBJECT_ID);
		}
		builder.append(from.toString());
		builder.append(where.toString());
		builder.append(join.toString());
		if(!count){
			builder.append(" ORDER BY A0."+COL_BOUND_CM_OBJECT_ID+" LIMIT :limit OFFSET :offset");
			parameters.addValue("limit", limit);
			parameters.addValue("offset", offset);
		}
		return builder.toString();
	}
}
