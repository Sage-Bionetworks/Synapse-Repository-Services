package org.sagebionetworks.repo.model.dbo.migration;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.dbo.AutoIncrementDatabaseObject;
import org.sagebionetworks.repo.model.dbo.DMLUtils;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.RowMetadata;
import org.sagebionetworks.repo.model.migration.RowMetadataResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * This is a generic dao like DBOBasicDao that provides data migration functions for individual tables.
 * @author John
 *
 */
@SuppressWarnings("rawtypes")
public class MigatableTableDAOImpl implements MigatableTableDAO {

	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	
	
	/**
	 * For unit testing.
	 * @param simpleJdbcTemplate
	 * @param databaseObjectRegister
	 * @param maxAllowedPacketBytes
	 */
	public MigatableTableDAOImpl(SimpleJdbcTemplate simpleJdbcTemplate,
			List<MigratableDatabaseObject> databaseObjectRegister,
			int maxAllowedPacketBytes) {
		super();
		this.simpleJdbcTemplate = simpleJdbcTemplate;
		this.databaseObjectRegister = databaseObjectRegister;
		this.maxAllowedPacketBytes = maxAllowedPacketBytes;
	}

	/**
	 * Default used by Spring
	 */
	public MigatableTableDAOImpl(){}
	/**
	 * Injected via Spring
	 */
	private List<MigratableDatabaseObject> databaseObjectRegister;
	
	int maxAllowedPacketBytes = -1;
	
	/**
	 * Injected via Spring
	 * @param databaseObjectRegister
	 */
	public void setDatabaseObjectRegister(List<MigratableDatabaseObject> databaseObjectRegister) {
		this.databaseObjectRegister = databaseObjectRegister;
	}
	
	/**
	 * Injected via Spring
	 * @param maxAllowedPacketBytes
	 */
	public void setMaxAllowedPacketBytes(int maxAllowedPacketBytes) {
		this.maxAllowedPacketBytes = maxAllowedPacketBytes;
	}

	// SQL
	private Map<MigrationType, String> deleteSqlMap = new HashMap<MigrationType, String>();
	private Map<MigrationType, String> countSqlMap = new HashMap<MigrationType, String>();
	private Map<MigrationType, String> maxSqlMap = new HashMap<MigrationType, String>();
	private Map<MigrationType, String> listSqlMap = new HashMap<MigrationType, String>();
	private Map<MigrationType, String> deltaListSqlMap = new HashMap<MigrationType, String>();
	private Map<MigrationType, String> backupSqlMap = new HashMap<MigrationType, String>();
	private Map<MigrationType, String> insertOrUpdateSqlMap = new HashMap<MigrationType, String>();
	
	private Map<MigrationType, FieldColumn> etagColumns = new HashMap<MigrationType, FieldColumn>();
	private Map<MigrationType, FieldColumn> backupIdColumns = new HashMap<MigrationType, FieldColumn>();
	private Map<MigrationType, RowMapper<RowMetadata>> rowMetadataMappers = new HashMap<MigrationType, RowMapper<RowMetadata>>();
	
	private List<MigrationType> rootTypes = new LinkedList<MigrationType>();
	
	/**
	 * We cache the mapping for each object type.
	 */
	private Map<Class<? extends DatabaseObject>, MigrationType> classToMapping = new HashMap<Class<? extends DatabaseObject>, MigrationType>();
	
	private Map<MigrationType, MigratableDatabaseObject> typeTpObject = new HashMap<MigrationType, MigratableDatabaseObject>();
	
	/**
	 * Called when this bean is ready.
	 */
	public void initialize(){
		// Make sure we have a table for all registered objects
		if(databaseObjectRegister == null) throw new IllegalArgumentException("databaseObjectRegister bean cannot be null");
		// Create the schema for each 
		for(MigratableDatabaseObject dbo: databaseObjectRegister){
			// Root objects are registered here.
			boolean isRoot = true;
			registerObject(dbo, isRoot);
		}
	}

	/**
	 * Register a MigratableDatabaseObject with this DAO.
	 * @param dbo
	 */
	private void registerObject(MigratableDatabaseObject dbo, boolean isRoot) {
		if(dbo instanceof AutoIncrementDatabaseObject<?>) throw new IllegalArgumentException("AUTO_INCREMENT tables cannot be migrated.  Please use the ID generator instead for DBO: "+dbo.getClass().getName());
		TableMapping mapping = dbo.getTableMapping();
		DMLUtils.validateMigratableTableMapping(mapping);
		MigrationType type = dbo.getMigratableTableType();
		// Build up the SQL cache.
		String delete = DMLUtils.createBatchDelete(mapping);
		deleteSqlMap.put(type, delete);
		String count = DMLUtils.createGetCountStatement(mapping);
		countSqlMap.put(type, count);
		String mx = DMLUtils.createGetMaxStatement(mapping);
		maxSqlMap.put(type, mx);
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
		// If this object has a sub table then regeister the sub table as well
		if(dbo.getSecondaryTypes() != null){
			Iterator<MigratableDatabaseObject> it = dbo.getSecondaryTypes().iterator();
			while(it.hasNext()){
				registerObject(it.next(), false);
			}
		}
		if(isRoot){
			this.rootTypes.add(type);
		}
	}

	@Override
	public long getCount(MigrationType type) {
		if(type == null) throw new IllegalArgumentException("type cannot be null");
		String countSql = this.countSqlMap.get(type);
		if(countSql == null) throw new IllegalArgumentException("Cannot find count SQL for "+type);
		return simpleJdbcTemplate.queryForLong(countSql);
	}
	
	@Override
	public long getMaxId(MigrationType type) {
		if(type == null) throw new IllegalArgumentException("type cannot be null");
		String maxSql = this.maxSqlMap.get(type);
		if(maxSql == null) throw new IllegalArgumentException("Cannot find max SQL for "+type);
		return simpleJdbcTemplate.queryForLong(maxSql);
	}

	@Override
	public RowMetadataResult listRowMetadata(MigrationType type, long limit, long offset) {
		if(type == null) throw new IllegalArgumentException("type cannot be null");
		String sql = this.getListSql(type);
		RowMapper<RowMetadata> mapper = this.getRowMetadataRowMapper(type);
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(DMLUtils.BIND_VAR_LIMIT, limit);
		params.addValue(DMLUtils.BIND_VAR_OFFSET, offset);
		List<RowMetadata> page = simpleJdbcTemplate.query(sql, mapper, params);
		long count = this.getCount(type);
		RowMetadataResult result = new RowMetadataResult();
		result.setList(page);
		result.setTotalCount(count);
		return result;
	}
	
	@Override
	public List<RowMetadata> listDeltaRowMetadata(MigrationType type, List<Long> idList) {
		if(type == null) throw new IllegalArgumentException("type cannot be null");
		// Fix for PLFM-1978
		if(idList.size() < 1) return new LinkedList<RowMetadata>();
		String sql = this.getDeltaListSql(type);
		RowMapper<RowMetadata> mapper = this.getRowMetadataRowMapper(type);
		SqlParameterSource params = new MapSqlParameterSource(DMLUtils.BIND_VAR_ID_lIST, idList);
		List<RowMetadata> page = simpleJdbcTemplate.query(sql, mapper, params);
		return page;
	}


	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public int deleteObjectsById(MigrationType type, List<Long> idList) {
		if(type == null) throw new IllegalArgumentException("type cannot be null");
		if(idList == null) throw new IllegalArgumentException("idList cannot be null");
		if(idList.size() < 1) return 0;
		String deleteSQL = this.deleteSqlMap.get(type);
		if(deleteSQL == null) throw new IllegalArgumentException("Cannot find batch delete SQL for "+type);
		SqlParameterSource params = new MapSqlParameterSource(DMLUtils.BIND_VAR_ID_lIST, idList);
		return simpleJdbcTemplate.update(deleteSQL, params);
	}
	

	@Override
	public <D extends DatabaseObject<D>> List<D> getBackupBatch(Class<? extends D> clazz, List<Long> rowIds) {
		if(clazz == null) throw new IllegalArgumentException("clazz cannot be null");
		if(rowIds == null) throw new IllegalArgumentException("idList cannot be null");
		if(rowIds.size() < 1) return new LinkedList<D>();
		MigrationType type = getTypeForClass(clazz);
		String sql = getBatchBackupSql(type);
		MigratableDatabaseObject<D, ?> object = getMigratableObject(type);
		SqlParameterSource params = new MapSqlParameterSource(DMLUtils.BIND_VAR_ID_lIST, rowIds);
		List<D> page = simpleJdbcTemplate.query(sql, object.getTableMapping(), params);
		return page;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <D extends DatabaseObject<D>> List<Long> createOrUpdateBatch(List<D> batch) {
		if(batch == null) throw new IllegalArgumentException("Batch cannot be null");
		if(batch.size() <1) return new LinkedList<Long>();
		List<Long> createOrUpdateIds = new LinkedList<Long>();
		// nothing to do with an empty batch
		if(batch.size() < 1) return createOrUpdateIds;

		MigrationType type = getTypeForClass(batch.get(0).getClass());
		FieldColumn backukpIdColumn = this.backupIdColumns.get(type);
		String sql = getInsertOrUpdateSql(type);
		SqlParameterSource[] namedParameters = new BeanPropertySqlParameterSource[batch.size()];
		for(int i=0; i<batch.size(); i++){
			namedParameters[i] = new BeanPropertySqlParameterSource(batch.get(i));
			Object obj = namedParameters[i].getValue(backukpIdColumn.getFieldName());
			if(!(obj instanceof Long)) throw new IllegalArgumentException("Cannot get backup ID for type : "+type);
			Long id = (Long) obj;
			createOrUpdateIds.add(id);
		}
		// execute the batch
		simpleJdbcTemplate.batchUpdate(sql, namedParameters);
		return createOrUpdateIds;
	}
	
	/**
	 * The the list sql for this type.
	 * @param type
	 * @return
	 */
	private String getListSql(MigrationType type){
		String sql = this.listSqlMap.get(type);
		if(sql == null) throw new IllegalArgumentException("Cannot find list SQL for type: "+type);
		return sql;
	}
	
	/**
	 * The the list sql for this type.
	 * @param type
	 * @return
	 */
	private String getDeltaListSql(MigrationType type){
		String sql = this.deltaListSqlMap.get(type);
		if(sql == null) throw new IllegalArgumentException("Cannot find delta list SQL for type: "+type);
		return sql;
	}
	
	/**
	 * The  RowMapper<RowMetadata> for this type.
	 * @param type
	 * @return
	 */
	private RowMapper<RowMetadata> getRowMetadataRowMapper(MigrationType type){
		RowMapper<RowMetadata> mapper = this.rowMetadataMappers.get(type);
		if(mapper == null) throw new IllegalArgumentException("Cannot find RowMetadataRowMapper for type: "+type);
		return mapper;
	}
	
	/**
	 * Get the type for a class.
	 * @param clazz
	 * @return
	 */
	private MigrationType getTypeForClass(Class<? extends DatabaseObject> clazz){
		if(clazz == null) throw new IllegalArgumentException("Class cannot be null");
		MigrationType type = this.classToMapping.get(clazz);
		if(type == null) throw new IllegalArgumentException("Cannot find the Type for Class: "+clazz.getName());
		return type;
	}

	private String getBatchBackupSql(MigrationType type){
		String sql = this.backupSqlMap.get(type);
		if(sql == null) throw new IllegalArgumentException("Cannot find the batch backup SQL for type: "+type);
		return sql;
	}
	
	private String getInsertOrUpdateSql(MigrationType type){
		String sql = this.insertOrUpdateSqlMap.get(type);
		if(sql == null) throw new IllegalArgumentException("Cannot find the insert/update backup SQL for type: "+type);
		return sql;
	}
	
	private MigratableDatabaseObject getMigratableObject(MigrationType type){
		MigratableDatabaseObject ob = this.typeTpObject.get(type);
		if(ob == null) throw new IllegalArgumentException("Cannot find the MigratableDatabaseObject for type: "+type);
		return ob;
	}

	@Override
	public MigratableDatabaseObject getObjectForType(MigrationType type) {
		return getMigratableObject(type);
	}

	@Override
	public List<MigrationType> getPrimaryMigrationTypes() {
		return rootTypes;
	}
	
}
