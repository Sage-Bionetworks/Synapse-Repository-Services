package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_BOUND_CM_ORD_COLUMN_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_BOUND_CM_ORD_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_BOUND_CM_ORD_OBJECT_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_BOUND_CM_ORD_ORDINAL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_BOUND_OWNER_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_BOUND_OWNER_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CM_HASH;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_BOUND_COLUMN_ORDINAL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_BOUND_COLUMN_OWNER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_COLUMN_MODEL;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.evaluation.dbo.DBOConstants;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.SinglePrimaryKeySqlParameterSource;
import org.sagebionetworks.repo.model.dbo.persistence.table.ColumnModelUtils;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOBoundColumnOrdinal;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOBoundColumnOwner;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOColumnModel;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * Database implementation of the ColumnModelDAO interface.
 * 
 * @author John
 *
 */
public class DBOColumnModelDAOImpl implements ColumnModelDAO {

	private static final String INPUT = "input";
	private static final String SELECT_COLUMN_NAME = "SELECT "+ COL_CM_ID+","+COL_CM_NAME+" FROM "+TABLE_COLUMN_MODEL+" WHERE "+COL_CM_ID+" IN (:"+INPUT+")";
	private static final String SQL_SELECT_OWNER_ETAG_FOR_UPDATE = "SELECT "+COL_BOUND_OWNER_ETAG+" FROM "+TABLE_BOUND_COLUMN_OWNER+" WHERE "+COL_BOUND_OWNER_OBJECT_ID+" = ? FOR UPDATE";
	
	private static final String SQL_GET_COLUMN_MODELS_FOR_OBJECT = "SELECT CM.* FROM " + TABLE_BOUND_COLUMN_ORDINAL
			+ " BO JOIN " + TABLE_COLUMN_MODEL + " CM ON (BO." + COL_BOUND_CM_ORD_COLUMN_ID + " = CM." + COL_CM_ID + ")"
			+ " WHERE BO." + COL_BOUND_CM_ORD_OBJECT_ID + " = ? AND BO." + COL_BOUND_CM_ORD_OBJECT_VERSION
			+ " = ? ORDER BY BO." + COL_BOUND_CM_ORD_ORDINAL + " ASC";
	
	private static final String SQL_GET_COLUMN_ID_FOR_OBJECT = "SELECT " + COL_BOUND_CM_ORD_COLUMN_ID + " FROM "
			+ TABLE_BOUND_COLUMN_ORDINAL + " BO WHERE BO." + COL_BOUND_CM_ORD_OBJECT_ID + " = ? AND BO."
			+ COL_BOUND_CM_ORD_OBJECT_VERSION + " = ? ORDER BY BO." + COL_BOUND_CM_ORD_ORDINAL + " ASC";
	
	private static final String SQL_DELETE_BOUND_ORDINAL = "DELETE FROM "+TABLE_BOUND_COLUMN_ORDINAL+" WHERE "+COL_BOUND_CM_ORD_OBJECT_ID+" = ? AND "+COL_BOUND_CM_ORD_OBJECT_VERSION+" = ?";
	private static final String SQL_DELETE_COLUMN_MODEL = "DELETE FROM "+TABLE_COLUMN_MODEL+" WHERE "+COL_CM_ID+" = ?";
	private static final String SQL_SELECT_COLUMNS_WITH_NAME_PREFIX_COUNT = "SELECT COUNT(*) FROM "+TABLE_COLUMN_MODEL+" WHERE "+COL_CM_NAME+" LIKE ? ";
	private static final String SQL_SELECT_COLUMNS_WITH_NAME_PREFIX = "SELECT * FROM "+TABLE_COLUMN_MODEL+" WHERE "+COL_CM_NAME+" LIKE ? ORDER BY "+COL_CM_NAME+" LIMIT ? OFFSET ?";
	private static final String SQL_TRUNCATE_BOUND_COLUMN_ORDINAL = "DELETE FROM "+TABLE_BOUND_COLUMN_ORDINAL+" WHERE "+COL_BOUND_CM_ORD_ORDINAL+" >= 0";
	private static final String SQL_TRUNCATE_COLUMN_MODEL= "DELETE FROM "+TABLE_COLUMN_MODEL+" WHERE "+COL_CM_ID+" >= 0";
	private static final String SQL_SELECT_COLUMNS_FOR_IDS = "SELECT * FROM "+TABLE_COLUMN_MODEL+" WHERE "+COL_CM_ID+" IN ( :ids ) ORDER BY "+COL_CM_NAME;
	private static final String SQL_SELECT_ID_WHERE_HASH_EQUALS = "SELECT "+COL_CM_ID+" FROM "+TABLE_COLUMN_MODEL+" WHERE "+COL_CM_HASH+" = ?";
	
	@Autowired
	private DBOBasicDao basicDao;
	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private NamedParameterJdbcTemplate namedJdbcTemplate;
	@Autowired
	private IdGenerator idGenerator;
	
	private static RowMapper<DBOColumnModel> ROW_MAPPER = new DBOColumnModel().getTableMapping();

	@Override
	public List<ColumnModel> listColumnModels(String namePrefix, long limit, long offset) {
		String likeString = preparePrefix(namePrefix);
		List<DBOColumnModel> dbos = jdbcTemplate.query(SQL_SELECT_COLUMNS_WITH_NAME_PREFIX, ROW_MAPPER, likeString, limit, offset);
		// Convert to DTOs
		return ColumnModelUtils.createDTOFromDBO(dbos);
	}

	@Override
	public List<ColumnModel> getColumnModelsForObject(IdAndVersion idAndVersion) throws DatastoreException {
		List<DBOColumnModel> dbos = jdbcTemplate.query(SQL_GET_COLUMN_MODELS_FOR_OBJECT, ROW_MAPPER,
				idAndVersion.getId(), idAndVersion.getVersion().orElse(DBOBoundColumnOrdinal.DEFAULT_NULL_VERSION));
		// Convert to DTOs
		return ColumnModelUtils.createDTOFromDBO(dbos);
	}
	
	@Override
	public List<String> getColumnModelIdsForObject(IdAndVersion idAndVersion) {
		return jdbcTemplate.queryForList(SQL_GET_COLUMN_ID_FOR_OBJECT, String.class, idAndVersion.getId(),
				idAndVersion.getVersion().orElse(DBOBoundColumnOrdinal.DEFAULT_NULL_VERSION));
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
		DBOColumnModel dbo = ColumnModelUtils.createDBOFromDTO(model, StackConfigurationSingleton.singleton().getTableMaxEnumValues());
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
	public void deleteOwner(String objectId) {
		basicDao.deleteObjectByPrimaryKey(DBOBoundColumnOwner.class, new SinglePrimaryKeySqlParameterSource(KeyFactory.stringToKey(objectId)));
	}

	@WriteTransaction
	@Override
	public void bindColumnToObject(final List<ColumnModel> newColumns, final IdAndVersion idAndVersion) throws NotFoundException {
		ValidateArgument.required(idAndVersion, "idAndVersion");	
		// Create or update the owner.
		DBOBoundColumnOwner owner = new DBOBoundColumnOwner();
		owner.setObjectId(idAndVersion.getId());
		owner.setEtag(UUID.randomUUID().toString());
		basicDao.createOrUpdate(owner);
		// Now replace the current current ordinal binding for this object.
		jdbcTemplate.update(SQL_DELETE_BOUND_ORDINAL, idAndVersion.getId(), idAndVersion.getVersion().orElse(DBOBoundColumnOrdinal.DEFAULT_NULL_VERSION));
		
		// bind the new columns if provided.
		if(newColumns != null && !newColumns.isEmpty()) {
			// Now insert the ordinal values
			List<DBOBoundColumnOrdinal> ordinal = ColumnModelUtils.createDBOBoundColumnOrdinalList(idAndVersion, newColumns);
			// this is just an insert
			basicDao.createBatch(ordinal);
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
	public List<ColumnModel> getColumnModel(List<String> ids) throws DatastoreException, NotFoundException {
		if(ids == null) throw new IllegalArgumentException("Ids cannot be null");
		if(ids.isEmpty()){
			return new LinkedList<ColumnModel>();
		}
		MapSqlParameterSource parameters = new MapSqlParameterSource("ids", ids);
		NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
		List<DBOColumnModel> dbos = namedTemplate.query(SQL_SELECT_COLUMNS_FOR_IDS, parameters,ROW_MAPPER);
		// Convert to DTOs
		return ColumnModelUtils.createDTOFromDBO(dbos);
	}

	
	@WriteTransaction
	@Override
	public boolean truncateAllColumnData() {
		int count = jdbcTemplate.update(SQL_TRUNCATE_BOUND_COLUMN_ORDINAL);
		count += jdbcTemplate.update(SQL_TRUNCATE_COLUMN_MODEL);
		return count >0;
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
	
	
	@Override
	public Map<Long, String> getColumnNames(Set<Long> columnIds) {
		ValidateArgument.required(columnIds, "columnIds");
		final Map<Long, String> results = new HashMap<>(columnIds.size());
		if(columnIds.isEmpty()) {
			return results;
		}
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(INPUT, columnIds);
		namedJdbcTemplate.query(SELECT_COLUMN_NAME, param, new RowCallbackHandler() {
			
			@Override
			public void processRow(ResultSet rs) throws SQLException {
				long id = rs.getLong(COL_CM_ID);
				String name = rs.getString(COL_CM_NAME);
				results.put(id, name);
			}
		});
		return results;
	}

}
