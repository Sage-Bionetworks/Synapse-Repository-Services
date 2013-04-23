package org.sagebionetworks.repo.model.dbo.migration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.dbo.DMLUtils;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.migration.MigratableTableType;
import org.sagebionetworks.repo.model.migration.RowMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * This DAO can be used for any 
 * @author John
 *
 */
@SuppressWarnings("rawtypes")
public class MigatableTableDAOImpl implements MigatableTableDAO {

	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	
	/**
	 * Injected via Spring
	 */
	private List<MigratableDatabaseObject> databaseObjectRegister;
	
	
	/**
	 * Injected via Spring
	 * @param databaseObjectRegister
	 */
	public void setDatabaseObjectRegister(List<MigratableDatabaseObject> databaseObjectRegister) {
		this.databaseObjectRegister = databaseObjectRegister;
	}
	
	private Map<MigratableTableType, String> deleteSqlMap = new HashMap<MigratableTableType, String>();
	private Map<MigratableTableType, String> countSqlMap = new HashMap<MigratableTableType, String>();
	private Map<MigratableTableType, String> listSqlMap = new HashMap<MigratableTableType, String>();
	private Map<MigratableTableType, String> deltaListSqlMap = new HashMap<MigratableTableType, String>();
	
	private Map<MigratableTableType, String> backupSqlMap = new HashMap<MigratableTableType, String>();
	
	private Map<MigratableTableType, String> insertOrUpdateSqlMap = new HashMap<MigratableTableType, String>();
	
	private Map<MigratableTableType, FieldColumn> etagColumns = new HashMap<MigratableTableType, FieldColumn>();
	private Map<MigratableTableType, FieldColumn> backupIdColumns = new HashMap<MigratableTableType, FieldColumn>();
	
	private Map<MigratableTableType, RowMapper<RowMetadata>> rowMetadataMappers = new HashMap<MigratableTableType, RowMapper<RowMetadata>>();
	
	/**
	 * We cache the mapping for each object type.
	 */
	private Map<Class<? extends DatabaseObject>, MigratableTableType> classToMapping = new HashMap<Class<? extends DatabaseObject>, MigratableTableType>();
	
	private Map<MigratableTableType, MigratableDatabaseObject> typeTpObject = new HashMap<MigratableTableType, MigratableDatabaseObject>();
	
	/**
	 * Called when this bean is ready.
	 */
	public void initialize(){
		// Make sure we have a table for all registered objects
		if(databaseObjectRegister == null) throw new IllegalArgumentException("databaseObjectRegister bean cannot be null");
		// Create the schema for each 
		for(MigratableDatabaseObject dbo: databaseObjectRegister){
			TableMapping mapping = dbo.getTableMapping();
			DMLUtils.validateMigratableTableMapping(mapping);
			MigratableTableType type = dbo.getMigratableTableType();
			// Build up the SQL cache.
			String delete = DMLUtils.createBatchDelete(mapping);
			deleteSqlMap.put(type, delete);
			String count = DMLUtils.createGetCountStatement(mapping);
			countSqlMap.put(type, count);
			String listRowMetadataSQL = DMLUtils.listRowMetadata(mapping);
			listSqlMap.put(type, listRowMetadataSQL);
			String deltalistRowMetadataSQL = DMLUtils.deltaListRowMetadata(mapping);
			deltaListSqlMap.put(type, deltalistRowMetadataSQL);
			// Does this type have an etag?
			FieldColumn etag = DMLUtils.getEtagColumn(mapping);
			if(etag != null){
				etagColumns.put(type, etag);
			}
			FieldColumn backupId = DMLUtils.getBackupIdColumnName(mapping);
			this.backupIdColumns.put(type, backupId);
			RowMapper<RowMetadata> rowMetadataMapper = DMLUtils.getRowMetadataRowMapper(mapping);
			rowMetadataMappers.put(type, rowMetadataMapper);
			// Backup batch SQL
			String batchBackup = DMLUtils.getBackupBatch(mapping);
			backupSqlMap.put(type, batchBackup);
			// map the class to the object
			this.classToMapping.put(mapping.getDBOClass(), type);
			this.typeTpObject.put(type, dbo);
			// The batch insert or update sql
			String sql = DMLUtils.getBatchInsertOrUdpate(mapping);
			this.insertOrUpdateSqlMap.put(type, sql);
		}
	}

	@Override
	public long getCount(MigratableTableType type) {
		if(type == null) throw new IllegalArgumentException("type cannot be null");
		String countSql = this.countSqlMap.get(type);
		if(countSql == null) throw new IllegalArgumentException("Cannot find count SQL for "+type);
		return simpleJdbcTemplate.queryForLong(countSql);
	}

	@Override
	public QueryResults<RowMetadata> listRowMetadata(MigratableTableType type, long limit, long offset) {
		if(type == null) throw new IllegalArgumentException("type cannot be null");
		String sql = this.getListSql(type);
		RowMapper<RowMetadata> mapper = this.getRowMetadataRowMapper(type);
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(DMLUtils.BIND_VAR_LIMIT, limit);
		params.addValue(DMLUtils.BIND_VAR_OFFSET, offset);
		List<RowMetadata> page = simpleJdbcTemplate.query(sql, mapper, params);
		long count = this.getCount(type);
		return new QueryResults<RowMetadata>(page, count);
	}
	
	@Override
	public List<RowMetadata> listDeltaRowMetadata(MigratableTableType type,	List<String> idList) {
		if(type == null) throw new IllegalArgumentException("type cannot be null");
		String sql = this.getDeltaListSql(type);
		RowMapper<RowMetadata> mapper = this.getRowMetadataRowMapper(type);
		SqlParameterSource params = new MapSqlParameterSource(DMLUtils.BIND_VAR_ID_lIST, idList);
		List<RowMetadata> page = simpleJdbcTemplate.query(sql, mapper, params);
		return page;
	}


	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public int deleteObjectsById(MigratableTableType type, List<String> idList) {
		if(type == null) throw new IllegalArgumentException("type cannot be null");
		if(idList == null) throw new IllegalArgumentException("idList cannot be null");
		String deleteSQL = this.deleteSqlMap.get(type);
		if(deleteSQL == null) throw new IllegalArgumentException("Cannot find batch delete SQL for "+type);
		SqlParameterSource params = new MapSqlParameterSource(DMLUtils.BIND_VAR_ID_lIST, idList);
		return simpleJdbcTemplate.update(deleteSQL, params);
	}
	

	@Override
	public <T extends DatabaseObject<T>> List<T> getBackupBatch(Class<? extends T> clazz, List<String> rowIds) {
		MigratableTableType type = getTypeForClass(clazz);
		String sql = getBatchBackupSql(type);
		MigratableDatabaseObject<T, ?> object = getMigratableObject(type);
		SqlParameterSource params = new MapSqlParameterSource(DMLUtils.BIND_VAR_ID_lIST, rowIds);
		List<T> page = simpleJdbcTemplate.query(sql, object.getTableMapping(), params);
		return page;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <T extends DatabaseObject<T>> int[] createOrUpdateBatch(List<T> batch) {
		if(batch == null) throw new IllegalArgumentException("Batch cannot be null");
		// nothing to do with an empty batch
		if(batch.size() < 1) return new int[0]; 
		MigratableTableType type = getTypeForClass(batch.get(0).getClass());
		String sql = getInsertOrUpdateSql(type);
		SqlParameterSource[] namedParameters = new BeanPropertySqlParameterSource[batch.size()];
		for(int i=0; i<batch.size(); i++){
			namedParameters[i] = new BeanPropertySqlParameterSource(batch.get(i));
		}
		return  simpleJdbcTemplate.batchUpdate(sql, namedParameters);
	}
	
	/**
	 * The the list sql for this type.
	 * @param type
	 * @return
	 */
	private String getListSql(MigratableTableType type){
		String sql = this.listSqlMap.get(type);
		if(sql == null) throw new IllegalArgumentException("Cannot find list SQL for type: "+type);
		return sql;
	}
	
	/**
	 * The the list sql for this type.
	 * @param type
	 * @return
	 */
	private String getDeltaListSql(MigratableTableType type){
		String sql = this.deltaListSqlMap.get(type);
		if(sql == null) throw new IllegalArgumentException("Cannot find delta list SQL for type: "+type);
		return sql;
	}
	
	/**
	 * The  RowMapper<RowMetadata> for this type.
	 * @param type
	 * @return
	 */
	private RowMapper<RowMetadata> getRowMetadataRowMapper(MigratableTableType type){
		RowMapper<RowMetadata> mapper = this.rowMetadataMappers.get(type);
		if(mapper == null) throw new IllegalArgumentException("Cannot find RowMetadataRowMapper for type: "+type);
		return mapper;
	}
	
	/**
	 * Get the type for a class.
	 * @param clazz
	 * @return
	 */
	private MigratableTableType getTypeForClass(Class<? extends DatabaseObject> clazz){
		if(clazz == null) throw new IllegalArgumentException("Class cannot be null");
		MigratableTableType type = this.classToMapping.get(clazz);
		if(type == null) throw new IllegalArgumentException("Cannot find the Type for Class: "+clazz.getName());
		return type;
	}

	private String getBatchBackupSql(MigratableTableType type){
		String sql = this.backupSqlMap.get(type);
		if(sql == null) throw new IllegalArgumentException("Cannot find the batch backup SQL for type: "+type);
		return sql;
	}
	
	private String getInsertOrUpdateSql(MigratableTableType type){
		String sql = this.insertOrUpdateSqlMap.get(type);
		if(sql == null) throw new IllegalArgumentException("Cannot find the insert/update backup SQL for type: "+type);
		return sql;
	}
	
	private MigratableDatabaseObject getMigratableObject(MigratableTableType type){
		MigratableDatabaseObject ob = this.typeTpObject.get(type);
		if(ob == null) throw new IllegalArgumentException("Cannot find the MigratableDatabaseObject for type: "+type);
		return ob;
	}
}
