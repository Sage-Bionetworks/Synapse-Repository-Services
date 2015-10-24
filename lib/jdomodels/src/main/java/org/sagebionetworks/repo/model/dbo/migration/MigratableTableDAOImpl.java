package org.sagebionetworks.repo.model.dbo.migration;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.dbo.AutoIncrementDatabaseObject;
import org.sagebionetworks.repo.model.dbo.AutoTableMapping;
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

import org.sagebionetworks.repo.transactions.WriteTransaction;

/**
 * This is a generic dao like DBOBasicDao that provides data migration functions for individual tables.
 * @author John
 *
 */
@SuppressWarnings("rawtypes")
public class MigratableTableDAOImpl implements MigratableTableDAO {
	
	private static final String SET_FOREIGN_KEY_CHECKS = "SET FOREIGN_KEY_CHECKS = ?";

	Logger log = LogManager.getLogger(MigratableTableDAOImpl.class);

	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	
	/**
	 * For unit testing
	 */
	public MigratableTableDAOImpl(SimpleJdbcTemplate simpleJdbcTemplate,
			List<MigratableDatabaseObject> databaseObjectRegister) {
		super();
		this.simpleJdbcTemplate = simpleJdbcTemplate;
		this.databaseObjectRegister = databaseObjectRegister;
	}

	/**
	 * Default used by Spring
	 */
	public MigratableTableDAOImpl() { }
	
	/**
	 * Injected via Spring
	 */
	private List<MigratableDatabaseObject> databaseObjectRegister;

	/**
	 * Injected via Spring
	 */
	private List<Long> userGroupIdsExemptFromDeletion;
	
	/**
	 * Injected via Spring
	 */
	public void setDatabaseObjectRegister(List<MigratableDatabaseObject> databaseObjectRegister) {
		this.databaseObjectRegister = databaseObjectRegister;
	}
	
	/**
	 * Injected via Spring
	 */
	public void setUserGroupIdsExemptFromDeletion(List<Long> userGroupIdsExemptFromDeletion) {
		this.userGroupIdsExemptFromDeletion = userGroupIdsExemptFromDeletion;
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
	public void initialize() {
		// Make sure we have a table for all registered objects
		if(databaseObjectRegister == null) throw new IllegalArgumentException("databaseObjectRegister bean cannot be null");
		// Create the schema for each 
		// This index is used to validate the order of migration.
		int lastIndex = 0;
		for(MigratableDatabaseObject dbo: databaseObjectRegister){
			// Root objects are registered here.
			boolean isRoot = true;
			registerObject(dbo, isRoot);
			// Validate that the backupId column meets the criteria.
			validateBackupColumn(dbo.getTableMapping());
			// What is the index of this type
			int typeIndex= typeIndex(dbo.getMigratableTableType());
			if(typeIndex < lastIndex) throw new IllegalArgumentException("The order of the primary MigrationType must match the order for the MigrationType enumeration.  Type:  "+dbo.getMigratableTableType().name()+" is out of order");
			lastIndex = typeIndex;
		}
		
		// Change must always be last
		if(!MigrationType.CHANGE.equals(MigrationType.values()[lastIndex])){
			throw new IllegalArgumentException("The migration type: "+MigrationType.CHANGE+" must always be last since it migration triggers asynchronous message processing of the stack");
		}
	}

	/**
	 * What is the index of this type in the enumeration?
	 * This is used to determine if the order of primary types is different than the enumeration order.
	 * @param type
	 * @return
	 */
	private int typeIndex(MigrationType type){
		for(int i=0; i<MigrationType.values().length; i++){
			if(MigrationType.values()[i].equals(type)) return i;
		}
		throw new IllegalArgumentException("Did not find type: "+type);
	}
	/**
	 * Register a MigratableDatabaseObject with this DAO.
	 * @param dbo
	 */
	@SuppressWarnings("unchecked")
	private void registerObject(MigratableDatabaseObject dbo, boolean isRoot) {
		if(dbo == null) throw new IllegalArgumentException("MigratableDatabaseObject cannot be null");
		if(dbo instanceof AutoIncrementDatabaseObject<?>) throw new IllegalArgumentException("AUTO_INCREMENT tables cannot be migrated.  Please use the ID generator instead for DBO: "+dbo.getClass().getName());
		TableMapping mapping = dbo.getTableMapping();
		DMLUtils.validateMigratableTableMapping(mapping);
		MigrationType type = dbo.getMigratableTableType();
		if(type == null) throw new IllegalArgumentException("MigrationType was null for class: "+dbo.getClass().getName());
		// Build up the SQL cache.
		String delete = DMLUtils.createBatchDelete(mapping);
		deleteSqlMap.put(type, delete);
		String count = DMLUtils.createGetCountByPrimaryKeyStatement(mapping);
		countSqlMap.put(type, count);
		String mx = DMLUtils.createGetMaxByBackupKeyStatement(mapping);
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
		// If this object has a sub table then register the sub table as well
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
	
	/**
	 * All backupIds columns of primary tables must have a uniqueness constraint (primary key or unique key).
	 * If a non-unique column were allowed as a backupId there there would be data lost during
	 * migration.  See: PLFM-2512.
	 * 
	 * Note: This requirement does NOT extend to secondary tables.
	 * 
	 * @param mapping
	 */
	public void validateBackupColumn(TableMapping mapping) {
		String backupColumnName = DMLUtils.getBackupIdColumnName(mapping)
				.getColumnName();
		String sql = DMLUtils.getBackupUniqueValidation(mapping);
		List<String> names = simpleJdbcTemplate.query(sql,
				new RowMapper<String>() {
					@Override
					public String mapRow(ResultSet rs, int rowNum)
							throws SQLException {
						return rs.getString("Key_name");
					}
				});
		if (names.isEmpty()) {
			throw new IllegalArgumentException(
					"BackupId columns must have a uniqueness constraint.  Could not find such a constraint for table: "
							+ mapping.getTableName()
							+ " column: "
							+ backupColumnName);
		}
		if (log.isDebugEnabled()) {
			log.debug("The following uniqueness constraint were found for table: "
					+ mapping.getTableName() + ":");
			log.debug("\t" + names.toString());
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


	@WriteTransaction
	@Override
	public int deleteObjectsById(MigrationType type, List<Long> idList) {
		if (type == null) {
			throw new IllegalArgumentException("type cannot be null");
		}
		if (idList == null) {
			throw new IllegalArgumentException("idList cannot be null");
		}
		
		// Migration should not delete the user handling the migration
		if (type == MigrationType.PRINCIPAL 
				|| type == MigrationType.CREDENTIAL
				|| type == MigrationType.GROUP_MEMBERS) {
			idList.removeAll(userGroupIdsExemptFromDeletion);
		}
		
		if (idList.size() < 1) {
			return 0;
		}
		
		String deleteSQL = this.deleteSqlMap.get(type);
		if (deleteSQL == null) {
			throw new IllegalArgumentException(
					"Cannot find batch delete SQL for " + type);
		}
		SqlParameterSource params = new MapSqlParameterSource(
				DMLUtils.BIND_VAR_ID_lIST, idList);
		return simpleJdbcTemplate.update(deleteSQL, params);
	}
	

	@Override
	public <D extends DatabaseObject<D>> List<D> getBackupBatch(Class<? extends D> clazz, List<Long> rowIds) {
		if(clazz == null) throw new IllegalArgumentException("clazz cannot be null");
		if(rowIds == null) throw new IllegalArgumentException("idList cannot be null");
		if(rowIds.size() < 1) return new LinkedList<D>();
		MigrationType type = getTypeForClass(clazz);
		String sql = getBatchBackupSql(type);
		
		@SuppressWarnings("unchecked")
		MigratableDatabaseObject<D, ?> object = getMigratableObject(type);
		SqlParameterSource params = new MapSqlParameterSource(DMLUtils.BIND_VAR_ID_lIST, rowIds);
		List<D> page = simpleJdbcTemplate.query(sql, object.getTableMapping(), params);
		return page;
	}

	@WriteTransaction
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
			namedParameters[i] = getSqlParameterSource(batch.get(i), batch.get(i).getTableMapping());
			Object obj = namedParameters[i].getValue(backukpIdColumn.getFieldName());
			if(!(obj instanceof Long)) throw new IllegalArgumentException("Cannot get backup ID for type : "+type);
			Long id = (Long) obj;
			createOrUpdateIds.add(id);
		}
		// execute the batch
		simpleJdbcTemplate.batchUpdate(sql, namedParameters);
		return createOrUpdateIds;
	}

	private <T> SqlParameterSource getSqlParameterSource(T toCreate, TableMapping<T> mapping) {
		if (mapping instanceof AutoTableMapping) {
			return ((AutoTableMapping) mapping).getSqlParameterSource(toCreate);
		}
		return new BeanPropertySqlParameterSource(toCreate);
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

	
	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.dbo.migration.MigratableTableDAO#runWithForeignKeyIgnored(java.util.concurrent.Callable)
	 */
	@Override
	public <T> T runWithForeignKeyIgnored(Callable<T> call) throws Exception{
		try{
			// unconditionally turn off foreign key checks.
			setForeignKeyChecks(false);
			return call.call();
		}finally{
			// unconditionally turn on foreign key checks.
			setForeignKeyChecks(true);
		}
	}
	
	/**
	 * Helper to enable/disable foreign keys.
	 * @param enabled
	 */
	private void setForeignKeyChecks(boolean enabled) {
		int value;
		if(enabled){
			// trun it on.
			value = 1;
		}else{
			// turn it off
			value = 0;
		}
		simpleJdbcTemplate.update(SET_FOREIGN_KEY_CHECKS, value);
	}
}
