package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_BOUND_CM_COLUMN_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_BOUND_CM_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_BOUND_CM_ORD_COLUMN_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_BOUND_CM_ORD_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_BOUND_CM_ORD_ORDINAL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_BOUND_OWNER_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_BOUND_OWNER_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CM_HASH;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_BOUND_COLUMN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_BOUND_COLUMN_ORDINAL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_BOUND_COLUMN_OWNER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_COLUMN_MODEL;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.evaluation.dbo.DBOConstants;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.SinglePrimaryKeySqlParameterSource;
import org.sagebionetworks.repo.model.dbo.persistence.table.ColumnModelUtils;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOBoundColumn;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOBoundColumnOrdinal;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOBoundColumnOwner;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOColumnModel;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.sagebionetworks.repo.transactions.WriteTransaction;

import com.google.common.collect.Sets;

/**
 * Database implementation of the ColumnModelDAO interface.
 * 
 * @author John
 *
 */
public class DBOColumnModelDAOImpl implements ColumnModelDAO {

	private static final String SQL_SELECT_OWNER_ETAG_FOR_UPDATE = "SELECT "+COL_BOUND_OWNER_ETAG+" FROM "+TABLE_BOUND_COLUMN_OWNER+" WHERE "+COL_BOUND_OWNER_OBJECT_ID+" = ? FOR UPDATE";
	private static final String SQL_GET_COLUMN_MODELS_FOR_OBJECT = "SELECT CM.* FROM "+TABLE_BOUND_COLUMN_ORDINAL+" BO, "+TABLE_COLUMN_MODEL+" CM WHERE BO."+COL_BOUND_CM_ORD_COLUMN_ID+" = CM."+COL_CM_ID+" AND BO."+COL_BOUND_CM_ORD_OBJECT_ID+" = ? ORDER BY BO."+COL_BOUND_CM_ORD_ORDINAL+" ASC";
	private static final String SQL_GET_COLUMN_ID_FOR_OBJECT = "SELECT "+COL_BOUND_CM_ORD_COLUMN_ID+" FROM "+TABLE_BOUND_COLUMN_ORDINAL+" BO WHERE BO."+COL_BOUND_CM_ORD_OBJECT_ID+" = ? ORDER BY BO."+COL_BOUND_CM_ORD_ORDINAL+" ASC";
	private static final String SQL_DELETE_BOUND_ORDINAL = "DELETE FROM "+TABLE_BOUND_COLUMN_ORDINAL+" WHERE "+COL_BOUND_CM_ORD_OBJECT_ID+" = ?";
	private static final String SQL_DELETE_BOUND_COLUMNS = "DELETE FROM "+TABLE_BOUND_COLUMN+" WHERE "+COL_BOUND_CM_OBJECT_ID+" = ?";
	private static final String SQL_DELETE_COLUMN_MODEL = "DELETE FROM "+TABLE_COLUMN_MODEL+" WHERE "+COL_CM_ID+" = ?";
	private static final String SQL_SELECT_COLUMNS_WITH_NAME_PREFIX_COUNT = "SELECT COUNT(*) FROM "+TABLE_COLUMN_MODEL+" WHERE "+COL_CM_NAME+" LIKE ? ";
	private static final String SQL_SELECT_COLUMNS_WITH_NAME_PREFIX = "SELECT * FROM "+TABLE_COLUMN_MODEL+" WHERE "+COL_CM_NAME+" LIKE ? ORDER BY "+COL_CM_NAME+" LIMIT ? OFFSET ?";
	private static final String SQL_TRUNCATE_BOUND_COLUMNS = "DELETE FROM "+TABLE_BOUND_COLUMN+" WHERE "+COL_BOUND_CM_COLUMN_ID+" >= 0";
	private static final String SQL_TRUNCATE_BOUND_COLUMN_ORDINAL = "DELETE FROM "+TABLE_BOUND_COLUMN_ORDINAL+" WHERE "+COL_BOUND_CM_ORD_ORDINAL+" >= 0";
	private static final String SQL_TRUNCATE_COLUMN_MODEL= "DELETE FROM "+TABLE_COLUMN_MODEL+" WHERE "+COL_CM_ID+" >= 0";
	private static final String SQL_SELECT_COLUMNS_FOR_IDS = "SELECT * FROM "+TABLE_COLUMN_MODEL+" WHERE "+COL_CM_ID+" IN ( :ids ) ORDER BY "+COL_CM_NAME;
	private static final String SQL_SELECT_COLUMNS_FOR_IDS_IN_ORDER = "SELECT * FROM " + TABLE_COLUMN_MODEL + " WHERE " + COL_CM_ID
			+ " IN ( :ids ) ORDER BY FIELD(" + COL_CM_ID + ", :ids )";
	private static final String SQL_SELECT_ID_WHERE_HASH_EQUALS = "SELECT "+COL_CM_ID+" FROM "+TABLE_COLUMN_MODEL+" WHERE "+COL_CM_HASH+" = ?";
	
	@Autowired
	private DBOBasicDao basicDao;
	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private IdGenerator idGenerator;
	
	private static RowMapper<DBOColumnModel> ROW_MAPPER = new DBOColumnModel().getTableMapping();
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
		List<DBOColumnModel> dbos = jdbcTemplate.query(SQL_SELECT_COLUMNS_WITH_NAME_PREFIX, ROW_MAPPER, likeString, limit, offset);
		// Convert to DTOs
		return ColumnModelUtils.createDTOFromDBO(dbos);
	}
	@Override
	public List<ColumnModel> getColumnModelsForObject(String tableIdString)
			throws DatastoreException{
		long tableId = KeyFactory.stringToKey(tableIdString);
		List<DBOColumnModel> dbos = jdbcTemplate.query(SQL_GET_COLUMN_MODELS_FOR_OBJECT, ROW_MAPPER, tableId);
		// Convert to DTOs
		return ColumnModelUtils.createDTOFromDBO(dbos);
	}
	
	@Override
	public List<String> getColumnIdsForObject(String tableIdString) {
		long tableId = KeyFactory.stringToKey(tableIdString);
		return jdbcTemplate.queryForList(SQL_GET_COLUMN_ID_FOR_OBJECT, String.class, tableId);
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
		return jdbcTemplate.queryForObject(SQL_SELECT_COLUMNS_WITH_NAME_PREFIX_COUNT,new SingleColumnRowMapper<Long>(), likeString);
	}

	@WriteTransaction
	@Override
	public ColumnModel createColumnModel(ColumnModel model) throws DatastoreException, NotFoundException {
		// Convert to the DBO
		DBOColumnModel dbo = ColumnModelUtils.createDBOFromDTO(model, StackConfiguration.singleton().getTableMaxEnumValues());
		// check to see if a column model already exists with this hash.
		String existingId = getColumnForHash(dbo.getHash());
		if(existingId != null){
			// a column already exists with this same hash.
			return getColumnModel(existingId);
		}
		// This is a new unique hash.
		Long id = idGenerator.generateNewId(IdType.COLUMN_MODEL_ID);
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
		return ColumnModelUtils.createDTOFromDBO(dbo);
	}
	
	@WriteTransaction
	@Override
	public int deleteColumModel(String id) {
		if(id == null) throw new IllegalArgumentException("id cannot be null");
		return jdbcTemplate.update(SQL_DELETE_COLUMN_MODEL, id);
	}

	@WriteTransaction
	@Override
	public int unbindAllColumnsFromObject(String objectIdString) {
		if(objectIdString == null) throw new IllegalArgumentException("objectId cannot be null");
		Long objectId = KeyFactory.stringToKey(objectIdString);
		// Now replace the current current ordinal binding for this object.
		jdbcTemplate.update(SQL_DELETE_BOUND_ORDINAL, objectId);
		return jdbcTemplate.update(SQL_DELETE_BOUND_COLUMNS, objectId);
	}

	@WriteTransaction
	@Override
	public void deleteOwner(String objectId) {
		basicDao.deleteObjectByPrimaryKey(DBOBoundColumnOwner.class, new SinglePrimaryKeySqlParameterSource(KeyFactory.stringToKey(objectId)));
	}

	@WriteTransaction
	@Override
	public int bindColumnToObject(List<String> newCurrentColumnIds, String objectIdString) throws NotFoundException {
		if(objectIdString == null) throw new IllegalArgumentException("objectId cannot be null");
		if(newCurrentColumnIds == null || newCurrentColumnIds.isEmpty()){
			// delete all columns for this object
			return unbindAllColumnsFromObject(objectIdString);
		}
		// Get each model to valid they exist.
		List<ColumnModel> columns = getColumnModel(newCurrentColumnIds, false);
		// Validate that all names are unique within this object
		Set<String> nameSet = new HashSet<String>(columns.size());
		for(ColumnModel cm: columns){
			if(!nameSet.add(cm.getName())){
				throw new IllegalArgumentException("Cannot add two columns with the same name: '"+cm.getName()+"' to table: "+objectIdString);
			}
		}
		
		Long objectId = KeyFactory.stringToKey(objectIdString);
		try {
			// Create or update the owner.
			DBOBoundColumnOwner owner = new DBOBoundColumnOwner();
			owner.setObjectId(objectId);
			owner.setEtag(UUID.randomUUID().toString());
			basicDao.createOrUpdate(owner);
			
			// first bind these columns to the object. This binding is permanent and can only grow over time.
			List<DBOBoundColumn> permanent = ColumnModelUtils.createDBOBoundColumnList(objectId, newCurrentColumnIds);
			// Sort by columnId to prevent deadlock
			ColumnModelUtils.sortByColumnId(permanent);
			// Insert or update the batch
			basicDao.createOrUpdateBatch(permanent);
			// Now replace the current current ordinal binding for this object.
			jdbcTemplate.update(SQL_DELETE_BOUND_ORDINAL, objectId);
			// Now insert the ordinal values
			List<DBOBoundColumnOrdinal> ordinal = ColumnModelUtils.createDBOBoundColumnOrdinalList(objectId, newCurrentColumnIds);
			// this is just an insert
			basicDao.createBatch(ordinal);
			return newCurrentColumnIds.size();
		} catch (IllegalArgumentException e) {
			// Check to see if the COL_MODEL_FK constraint was triggered.
			if(e.getMessage().contains("COL_MODEL_FK")){
				throw new NotFoundException("One or more of the following ColumnModel IDs does not exist: "+newCurrentColumnIds.toString());
			}else{
				throw e;
			}
		}
	}

	@Override
	public String getColumnForHash(String hash) {
		try {
			long id = jdbcTemplate.queryForObject(SQL_SELECT_ID_WHERE_HASH_EQUALS,new SingleColumnRowMapper<Long>(), hash);
			return Long.toString(id);
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	@Override
	public List<ColumnModel> getColumnModel(List<String> ids, boolean keepOrder) throws DatastoreException, NotFoundException {
		if(ids == null) throw new IllegalArgumentException("Ids cannot be null");
		if(ids.isEmpty()){
			return new LinkedList<ColumnModel>();
		}
		MapSqlParameterSource parameters = new MapSqlParameterSource("ids", ids);
		String sql = keepOrder ? SQL_SELECT_COLUMNS_FOR_IDS_IN_ORDER : SQL_SELECT_COLUMNS_FOR_IDS;
		NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
		List<DBOColumnModel> dbos = namedTemplate.query(sql, parameters,ROW_MAPPER);
		// Convert to DTOs
		List<ColumnModel> results = ColumnModelUtils.createDTOFromDBO(dbos);
		if(results.size() < ids.size()){
			// this could be a case of duplicate ids, in which case we want to throw a more specific error
			Set<String> idSet = Sets.newHashSet();
			for (String id : ids) {
				if (!idSet.add(id)) {
					throw new IllegalArgumentException("Duplicate id in the list of column ids: " + id);
				}
			}
			throw new NotFoundException("One or more of the following ColumnModel IDs does not exist: "+ids.toString());
		}
		return results;
	}

	
	@WriteTransaction
	@Override
	public boolean truncateAllColumnData() {
		int count = jdbcTemplate.update(SQL_TRUNCATE_BOUND_COLUMN_ORDINAL);
		count = jdbcTemplate.update(SQL_TRUNCATE_BOUND_COLUMNS);
		count += jdbcTemplate.update(SQL_TRUNCATE_COLUMN_MODEL);
		return count >0;
	}

	@Override
	public List<String> listObjectsBoundToColumn(Set<String> columnIds,	boolean currentOnly, long limit, long offset) {
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		// With currently only we have one additional condition.
		String sql = builderListObjectsSql(columnIds, currentOnly, limit, offset, parameters, false);
		NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
		// Run the query
		return namedTemplate.query(sql, parameters, ENTITY_ID_MAPPER);
	}
	
	@Override
	public long listObjectsBoundToColumnCount(Set<String> columnIds, boolean currentOnly) {
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		// With currently only we have one additional condition.
		String sql = builderListObjectsSql(columnIds, currentOnly, -1, -1, parameters, true);
		NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
		return namedTemplate.queryForObject(sql, parameters, new SingleColumnRowMapper<Long>());
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
	private static String builderListObjectsSql(Set<String> columnIds, boolean currentOnly, long limit, long offset, MapSqlParameterSource parameters, boolean count){
		// We build the from, where, and join at the same time with a single loop.
		String tableName;
		String idColumnName;
		String objectIdColumnName;
		if(currentOnly){
			// Look in the ordinal table
			tableName = TABLE_BOUND_COLUMN_ORDINAL;
			idColumnName = COL_BOUND_CM_ORD_COLUMN_ID;
			objectIdColumnName = COL_BOUND_CM_ORD_OBJECT_ID;
		}else{
			// Look in the full history table
			tableName = TABLE_BOUND_COLUMN;
			idColumnName = COL_BOUND_CM_COLUMN_ID;
			objectIdColumnName = COL_BOUND_CM_OBJECT_ID;
		}
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
				join.append(" AND A"+(i-1)+"."+objectIdColumnName+" = "+tableAlias+"."+objectIdColumnName);
				where.append(" AND");
			}
			from.append(" "+tableName+" "+tableAlias);
			where.append(" "+tableAlias+"."+idColumnName+" = :c"+i);
			long colId = Long.parseLong(it.next());
			parameters.addValue("c"+i, colId);
		}
		// put all of the parts together.
		StringBuilder builder = new StringBuilder();
		builder.append("SELECT");
		if(count){
			builder.append(" COUNT(DISTINCT A0."+objectIdColumnName+")");
		}else{
			builder.append(" DISTINCT A0."+objectIdColumnName);
		}
		builder.append(from.toString());
		builder.append(where.toString());
		builder.append(join.toString());
		if(!count){
			builder.append(" ORDER BY A0."+objectIdColumnName+" LIMIT :limit OFFSET :offset");
			parameters.addValue("limit", limit);
			parameters.addValue("offset", offset);
		}
		return builder.toString();
	}
	
	@WriteTransaction
	@Override
	public String lockOnOwner(String objectIdString) {
		Long objectId = KeyFactory.stringToKey(objectIdString);
		return jdbcTemplate.queryForObject(SQL_SELECT_OWNER_ETAG_FOR_UPDATE, new RowMapper<String>() {
			@Override
			public String mapRow(ResultSet rs, int rowNum) throws SQLException {
				return rs.getString(COL_BOUND_OWNER_ETAG);
			}
		}, objectId);
	}

}
