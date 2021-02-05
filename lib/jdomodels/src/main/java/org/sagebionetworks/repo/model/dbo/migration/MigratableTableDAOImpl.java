package org.sagebionetworks.repo.model.dbo.migration;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Function;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.database.StreamingJdbcTemplate;
import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.backup.FileHandleBackup;
import org.sagebionetworks.repo.model.daemon.BackupAliasType;
import org.sagebionetworks.repo.model.dbo.AutoIncrementDatabaseObject;
import org.sagebionetworks.repo.model.dbo.AutoTableMapping;
import org.sagebionetworks.repo.model.dbo.DMLUtils;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMessageToUser;
import org.sagebionetworks.repo.model.migration.BatchChecksumRequest;
import org.sagebionetworks.repo.model.migration.IdRange;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.RangeChecksum;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.TemporaryCode;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

/**
 * This is a generic dao like DBOBasicDao that provides data migration functions for individual tables.
 * @author John
 *
 */
@SuppressWarnings("rawtypes")
public class MigratableTableDAOImpl implements MigratableTableDAO {
	
	private static final String REFERENCED_TABLE_NAME = "REFERENCED_TABLE_NAME";
	private static final String TABLE_NAME = "TABLE_NAME";
	private static final String DELETE_RULE = "DELETE_RULE";
	private static final String CONSTRAINT_NAME = "CONSTRAINT_NAME";

	private static final String SQL_SELECT_NONRESTRICTED_FOREIGN_KEYS = 
			"SELECT CONSTRAINT_NAME, DELETE_RULE, TABLE_NAME, REFERENCED_TABLE_NAME"
			+ " FROM information_schema.REFERENTIAL_CONSTRAINTS "
			+ "WHERE"
			+ " DELETE_RULE != 'RESTRICT' AND UNIQUE_CONSTRAINT_SCHEMA = ?";

	private static final String SET_FOREIGN_KEY_CHECKS = "SET FOREIGN_KEY_CHECKS = ?";
	private static final String SET_UNIQUE_KEY_CHECKS = "SET UNIQUE_CHECKS = ?";

	private static UnmodifiableXStream TABLE_NAME_ALIAS_X_STREAM;
	private static UnmodifiableXStream MIGRATION_TYPE_NAME_ALIAS_X_STREAM;


	Logger log = LogManager.getLogger(MigratableTableDAOImpl.class);

	private JdbcTemplate jdbcTemplate;

	private StackConfiguration stackConfiguration;

	@Autowired
	public MigratableTableDAOImpl(JdbcTemplate jdbcTemplate, StackConfiguration stackConfiguration) {
		this.jdbcTemplate = jdbcTemplate;
		this.stackConfiguration = stackConfiguration;
	}
	
	/**
	 * Injected via Spring
	 */
	List<MigratableDatabaseObject> databaseObjectRegister;

	/**
	 * Injected via Spring
	 */
	@Deprecated
	private List<Long> userGroupIdsExemptFromDeletion;
	
	/**
	 * Injected via Spring
	 */
	public void setDatabaseObjectRegister(List<MigratableDatabaseObject> databaseObjectRegister) {
		this.databaseObjectRegister = databaseObjectRegister;
	}

	public List<MigratableDatabaseObject> getDatabaseObjectRegister() {
		return Collections.unmodifiableList(this.databaseObjectRegister);
	}


	/**
	 * Injected via Spring
	 */
	public void setUserGroupIdsExemptFromDeletion(List<Long> userGroupIdsExemptFromDeletion) {
		this.userGroupIdsExemptFromDeletion = userGroupIdsExemptFromDeletion;
	}

	// SQL
	private Map<MigrationType, String> deleteByRangeMap = new HashMap<MigrationType, String>();
	private Map<MigrationType, String> countSqlMap = new HashMap<MigrationType, String>();
	private Map<MigrationType, String> maxSqlMap = new HashMap<MigrationType, String>();
	private Map<MigrationType, String> minSqlMap = new HashMap<MigrationType, String>();
	private Map<MigrationType, String> backupSqlRangeMap = new HashMap<MigrationType, String>();
	private Map<MigrationType, String> insertOrUpdateSqlMap = new HashMap<MigrationType, String>();
	private Map<MigrationType, String> checksumRangeSqlMap = new HashMap<MigrationType, String>();
	private Map<MigrationType, String> checksumTableSqlMap = new HashMap<MigrationType, String>();
	private Map<MigrationType, String> batchChecksumSqlMap = new HashMap<>();
	private Map<MigrationType, String> migrationTypeCountSqlMap = new HashMap<MigrationType, String>();
	
	private Map<MigrationType, FieldColumn> etagColumns = new HashMap<MigrationType, FieldColumn>();
	private Map<MigrationType, FieldColumn> backupIdColumns = new HashMap<MigrationType, FieldColumn>();
	
	private List<MigrationType> rootTypes = new LinkedList<MigrationType>();
	
	private Set<MigrationType> registeredMigrationTypes = new HashSet<MigrationType>();
	
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

		initializeAliasTypeToXStreamMap(databaseObjectRegister);
	}

	static void initializeAliasTypeToXStreamMap(List<MigratableDatabaseObject> databaseObjectRegister) {
		//create maps for alias type to xstream
		UnmodifiableXStream.Builder tableNameXStreamBuilder = UnmodifiableXStream.builder();
		tableNameXStreamBuilder.allowTypeHierarchy(MigratableDatabaseObject.class);
		UnmodifiableXStream.Builder migrationTypeNameXStreamBuilder = UnmodifiableXStream.builder();
		migrationTypeNameXStreamBuilder.allowTypeHierarchy(MigratableDatabaseObject.class);
		
		Function<MigratableDatabaseObject<?, ?>, String> tableAliasProvider = (dbo) -> dbo.getTableMapping().getTableName();
		Function<MigratableDatabaseObject<?, ?>, String> typeAliasProvider = (dbo) -> dbo.getMigratableTableType().name();

		for(MigratableDatabaseObject<?, ?> dbo: databaseObjectRegister) {
			// Add aliases to XStream for each alias type
			//BackupAliasType.TABLE_NAME
			addAlias(tableNameXStreamBuilder, dbo, tableAliasProvider);
			//BackupAliasType.MIGRATION_TYPE_NAME
			addAlias(migrationTypeNameXStreamBuilder, dbo, typeAliasProvider);
		}
		
		//add map entries once the builders are done
		TABLE_NAME_ALIAS_X_STREAM =  tableNameXStreamBuilder.build();
		MIGRATION_TYPE_NAME_ALIAS_X_STREAM = migrationTypeNameXStreamBuilder.build();
	}
	
	static void addAlias(UnmodifiableXStream.Builder streamBuilder, MigratableDatabaseObject<?, ?> dbo, Function<MigratableDatabaseObject<?, ?>, String> aliasProvider) {
		streamBuilder.alias(aliasProvider.apply(dbo), dbo.getBackupClass());
		// Also add the alias for the secondary objects
		if (dbo.getSecondaryTypes() != null) {
			dbo.getSecondaryTypes().forEach( secondaryType -> {				
				addAlias(streamBuilder, secondaryType, aliasProvider);
			});
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.dbo.migration.MigratableTableDAO#mapSecondaryTablesToPrimaryGroups()
	 */
	@Override
	public Map<String, Set<String>> mapSecondaryTablesToPrimaryGroups(){
		Map<String, Set<String>> results = new HashMap<>();
		for(MigratableDatabaseObject migratable: databaseObjectRegister) {
			List<MigratableDatabaseObject> secondaryTypes = migratable.getSecondaryTypes();
			if(secondaryTypes != null) {
				Set<String> primaryGroupNames = new HashSet<>(secondaryTypes.size()+1);
				// Add the primary table to the group
				primaryGroupNames.add(migratable.getTableMapping().getTableName().toUpperCase());
				// Add each secondary type to the map
				for(MigratableDatabaseObject secondary: secondaryTypes) {
					String secondaryName = secondary.getTableMapping().getTableName().toUpperCase();
					// add this secondary to the primary group
					primaryGroupNames.add(secondaryName);
					results.put(secondaryName, primaryGroupNames);
				}
			}
		}
		return results;
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
		String deleteByRange = DMLUtils.createDeleteByBackupIdRange(mapping);
		deleteByRangeMap.put(type, deleteByRange);
		String count = DMLUtils.createGetCountByPrimaryKeyStatement(mapping);
		countSqlMap.put(type, count);
		String mx = DMLUtils.createGetMaxByBackupKeyStatement(mapping);
		maxSqlMap.put(type, mx);
		String mi = DMLUtils.createGetMinByBackupKeyStatement(mapping);
		minSqlMap.put(type,  mi);
		String mtc = DMLUtils.createGetMinMaxCountByKeyStatement(mapping);
		migrationTypeCountSqlMap.put(type, mtc);
		String sumCrc = DMLUtils.createSelectChecksumStatement(mapping);
		checksumRangeSqlMap.put(type, sumCrc);
		String checksumTable = DMLUtils.createChecksumTableStatement(mapping);
		checksumTableSqlMap.put(type, checksumTable);
		String batchChecksumSql = DMLUtils.createSelectBatchChecksumStatement(mapping);
		this.batchChecksumSqlMap.put(type, batchChecksumSql);
		// Does this type have an etag?
		FieldColumn etag = DMLUtils.getEtagColumn(mapping);
		if(etag != null){
			validateEtagColumn(mapping.getTableName(), etag.getColumnName());
			etagColumns.put(type, etag);
		}
		FieldColumn backupId = DMLUtils.getBackupIdColumnName(mapping);
		this.backupIdColumns.put(type, backupId);
		
		String backupRangeSql = DMLUtils.getBackupRangeBatch(mapping);
		this.backupSqlRangeMap.put(type, backupRangeSql);

		// map the class to the object
		this.classToMapping.put(mapping.getDBOClass(), type);
		if (typeTpObject.containsKey(type)) {
			throw new IllegalArgumentException("Each DBO should have its own MigrationType. Found duplicated type for: "+dbo.getClass().getName());
		}
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
		
		registeredMigrationTypes.add(dbo.getMigratableTableType());

	}

	private void validateEtagColumn(String tableName, String columnName) {
		String query =
				"SELECT IS_NULLABLE \n" +
				"FROM INFORMATION_SCHEMA.COLUMNS \n" +
				"WHERE TABLE_NAME = ? \n" +
				"AND COLUMN_NAME = ? \n";
		try {
			String isNullable = jdbcTemplate.queryForObject(query, String.class, tableName, columnName);
			if (!"NO".equals(isNullable)) {
				throw new IllegalArgumentException("etag column " + columnName + " must be NOT NULL for table " + tableName);
			}
		} catch (EmptyResultDataAccessException e){
			throw new IllegalStateException("Could not find row for table="+tableName + " columnName="+columnName , e);
		}
	}
	
	/**
	 * All backupIds columns of primary tables must have a uniqueness constraint (primary key or unique key).
	 * If a non-unique column were allowed as a backupId there there would be data lost during
	 * migration.  See: PLFM-2512.
	 * 
	 * Note: This requirement does NOT extend to secondary tables.
	 * 
	 * Additionally the data type of a backup column must be a bigint.
	 * 
	 * @param mapping
	 */
	public void validateBackupColumn(TableMapping mapping) {
		String backupColumnName = DMLUtils.getBackupIdColumnName(mapping)
				.getColumnName();
		String sql = DMLUtils.getBackupUniqueValidation(mapping);
		List<String> names = jdbcTemplate.query(sql,
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
		
		sql = DMLUtils.getColumnDataType(mapping.getTableName(), backupColumnName);
		
		String dataType = jdbcTemplate.queryForObject(sql, String.class);
		
		if (!"bigint".equalsIgnoreCase(dataType)) {
			throw new IllegalArgumentException("Backup columns must be of \"bigint\" type. Found " + dataType + " for table: " 
					+ mapping.getTableName()
					+ " column: "
					+ backupColumnName);
		}
	}
	
	
	@Override
	public long getCount(MigrationType type) {
		if(type == null) throw new IllegalArgumentException("type cannot be null");
		String countSql = this.countSqlMap.get(type);
		if(countSql == null) throw new IllegalArgumentException("Cannot find count SQL for "+type);
		return jdbcTemplate.queryForObject(countSql, Long.class);
	}
	
	@Override
	public long getMaxId(MigrationType type) {
		if(type == null) throw new IllegalArgumentException("type cannot be null");
		String maxSql = this.maxSqlMap.get(type);
		if(maxSql == null) throw new IllegalArgumentException("Cannot find max SQL for "+type);
		Long res = jdbcTemplate.queryForObject(maxSql, Long.class);
		if (res == null) {
			return 0;
		} else {
			return res;
		}
	}

	@Override
	public long getMinId(MigrationType type) {
		if(type == null) throw new IllegalArgumentException("type cannot be null");
		String minSql = this.minSqlMap.get(type);
		if(minSql == null) throw new IllegalArgumentException("Cannot find min SQL for "+type);
		Long res = jdbcTemplate.queryForObject(minSql, Long.class);
		if (res == null) {
			return 0;
		} else {
			return res;
		}
	}

	private <T> SqlParameterSource getSqlParameterSource(T toCreate, TableMapping mapping) {
		if (mapping instanceof AutoTableMapping) {
			return ((AutoTableMapping) mapping).getSqlParameterSource(toCreate);
		}
		return new BeanPropertySqlParameterSource(toCreate);
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
	
	private String getBatchBackupRangeSql(MigrationType type) {
		String sql = this.backupSqlRangeMap.get(type);
		if(sql == null) {
			throw new IllegalArgumentException("Cannot find the batch backup SQL for type: "+type);
		}
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
	public <T> T runWithKeyChecksIgnored(Callable<T> call) {
		try{
			// unconditionally turn off foreign key checks.
			setGlobalKeyChecks(false);
			try {
				return call.call();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}finally{
			// unconditionally turn on foreign key checks.
			setGlobalKeyChecks(true);
		}
	}
	
	/**
	 * Helper to enable/disable foreign keys.
	 * @param enabled
	 */
	private void setGlobalKeyChecks(boolean enabled) {
		int value;
		if(enabled){
			// trun it on.
			value = 1;
		} else {
			// turn it off
			value = 0;
		}
		jdbcTemplate.update(SET_FOREIGN_KEY_CHECKS, value);
		jdbcTemplate.update(SET_UNIQUE_KEY_CHECKS, value);
	}


	@Override
	public String getChecksumForIdRange(MigrationType type, String salt, long minId, long maxId) {
		String sql = this.checksumRangeSqlMap.get(type);
		if (sql == null) {
			throw new IllegalArgumentException("Cannot find the checksum SQL for type" + type);
		}
		if (minId > maxId) {
			throw new IllegalArgumentException("MaxId must be greater than minId");
		}
		SingleColumnRowMapper mapper = new SingleColumnRowMapper(String.class);
		List<String> l = jdbcTemplate.query(sql, mapper, salt, salt, minId, maxId);
		if ((l == null) || (l.size() != 1)) {
			return null;
		}
		String cs = l.get(0);
		if (cs == null) {
			return null;
		} else {
			return cs.toString();
		}
	}
	
	@Override
	public String getChecksumForType(MigrationType type) {
		String sql = this.checksumTableSqlMap.get(type);
		if (sql == null) {
			throw new IllegalArgumentException("Cannot find the checksum SQL for type" + type);
		}
		RowMapper<ChecksumTableResult> mapper = DMLUtils.getChecksumTableResultMapper();
		ChecksumTableResult checksum = jdbcTemplate.queryForObject(sql, mapper);
		String s = checksum.getChecksum();
		return s;
	}

	@Override
	public MigrationTypeCount getMigrationTypeCount(MigrationType type) {
		String sql = this.migrationTypeCountSqlMap.get(type);
		if (sql == null) {
			throw new IllegalArgumentException("Cannot find the migrationTypeCount SQL for type" + type);
		}
		RowMapper<MigrationTypeCount> mapper = DMLUtils.getMigrationTypeCountResultMapper();
		MigrationTypeCount mtc = jdbcTemplate.queryForObject(sql, mapper);
		mtc.setType(type);
		return mtc;
	}

	@Override
	public boolean isMigrationTypeRegistered(MigrationType type) {
		return this.registeredMigrationTypes.contains(type);
	}

	@Override
	public List<ForeignKeyInfo> listNonRestrictedForeignKeys() {
		String schema = stackConfiguration.getRepositoryDatabaseSchemaName();
		return jdbcTemplate.query(SQL_SELECT_NONRESTRICTED_FOREIGN_KEYS, new RowMapper<ForeignKeyInfo>() {

			@Override
			public ForeignKeyInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
				ForeignKeyInfo info = new ForeignKeyInfo();
				info.setConstraintName(rs.getString(CONSTRAINT_NAME));
				info.setDeleteRule(rs.getString(DELETE_RULE));
				info.setTableName(rs.getString(TABLE_NAME));
				info.setReferencedTableName(rs.getString(REFERENCED_TABLE_NAME));
				return info;
			}}, schema);
	}
	
	@Override
	public Iterable<MigratableDatabaseObject<?, ?>> streamDatabaseObjects(MigrationType type, Long minimumId,
			Long maximumId, Long batchSize) {
		String sql = getBatchBackupRangeSql(type);
		NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
		MigratableDatabaseObject object = getMigratableObject(type);
		Map<String, Object> parameters = new HashMap<>(4);
		parameters.put(DMLUtils.BIND_MIN_ID, minimumId);
		parameters.put(DMLUtils.BIND_MAX_ID, maximumId);
		return new QueryStreamIterable<MigratableDatabaseObject<?, ?>>(namedTemplate, object.getTableMapping(), sql, parameters, batchSize);
	}

	@WriteTransaction
	@Override
	public List<Long> createOrUpdate(final MigrationType type, final List<DatabaseObject<?>> batch) {
		ValidateArgument.required(batch, "batch");
		if(batch.isEmpty()) {
			return new LinkedList<>();
		}
		// Foreign Keys must be ignored for this operation.
		return this.runWithKeyChecksIgnored(() -> {
			List<Long> createOrUpdateIds = new LinkedList<>();
			FieldColumn backukpIdColumn = this.backupIdColumns.get(type);
			String sql = getInsertOrUpdateSql(type);
			SqlParameterSource[] namedParameters = new BeanPropertySqlParameterSource[batch.size()];
			int index = 0;
			for(DatabaseObject<?> databaseObject: batch){
				namedParameters[index] = getSqlParameterSource(databaseObject, databaseObject.getTableMapping());
				Object obj = namedParameters[index].getValue(backukpIdColumn.getFieldName());
				if(!(obj instanceof Long)) {
					throw new IllegalArgumentException("Cannot get backup ID for type : "+type);
				}
				Long id = (Long) obj;
				createOrUpdateIds.add(id);
				index++;
			}
			// execute the batch
			NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
			namedTemplate.batchUpdate(sql, namedParameters);
			return createOrUpdateIds;
		});
	}

	@WriteTransaction
	@Override
	public int deleteByRange(final MigrationType type, final long minimumId, final long maximumId) {
		ValidateArgument.required(type,"MigrationType");
		// Foreign Keys must be ignored for this operation.
		return this.runWithKeyChecksIgnored(() -> {
			String deleteSQL = this.deleteByRangeMap.get(type);
			NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
			Map<String, Object> parameters = new HashMap<>(2);
			parameters.put(DMLUtils.BIND_MIN_ID, minimumId);
			parameters.put(DMLUtils.BIND_MAX_ID, maximumId);
			return namedTemplate.update(deleteSQL, parameters);
		});
	}

	@Override
	public List<IdRange> calculateRangesForType(MigrationType migrationType, long minimumId, long maximumId,
			long optimalNumberOfRows) {
		// Build the ranges by scanning each primary ID and its secondary cardinality.
		final IdRangeBuilder builder = new IdRangeBuilder(optimalNumberOfRows);
		String sql = getPrimaryCardinalitySql(migrationType);
		// need to use a streaming template for this case.
		NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(new StreamingJdbcTemplate(jdbcTemplate.getDataSource()));
		Map<String, Object> parameters = new HashMap<>(2);
		parameters.put(DMLUtils.BIND_MIN_ID, minimumId);
		parameters.put(DMLUtils.BIND_MAX_ID, maximumId);
		// Stream over each primary row ID and its associated secondary cardinality.
		namedTemplate.query(sql, parameters, new RowCallbackHandler() {

			@Override
			public void processRow(ResultSet rs) throws SQLException {
				// pass each row to the builder
				long primaryRowId = rs.getLong(1);
				long cardinality = rs.getLong(2);
				builder.addRow(primaryRowId, cardinality);
			}});
		// The build collates the results
		return builder.collateResults();
	}

	/**
	 * Get the SQL for a primary cardinality.
	 * @param primaryType
	 * @return
	 */
	public String getPrimaryCardinalitySql(MigrationType primaryType) {
		MigratableDatabaseObject primaryObject = getMigratableObject(primaryType);
		TableMapping primaryMapping = primaryObject.getTableMapping();
		List<TableMapping<?>> secondaryMapping = new LinkedList<>();
		List<MigratableDatabaseObject> secondaryTypes = primaryObject.getSecondaryTypes();
		if(secondaryTypes != null) {
			for(MigratableDatabaseObject secondary: secondaryTypes) {
				secondaryMapping.add(secondary.getTableMapping());
			}
		}
		return DMLUtils.createPrimaryCardinalitySql(primaryMapping, secondaryMapping);
	}

	@Override
	public List<RangeChecksum> calculateBatchChecksums(BatchChecksumRequest request) {
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getMigrationType(), "request.migrationType");
		ValidateArgument.required(request.getMinimumId(), "request.minimumId");
		ValidateArgument.required(request.getMaximumId(), "request.maximumId");
		ValidateArgument.required(request.getBatchSize(), "request.batchSize");
		ValidateArgument.required(request.getSalt(), "request.salt");
		// Lookup the SQL for this type
		String sql = this.batchChecksumSqlMap.get(request.getMigrationType());
		return this.jdbcTemplate.query(sql, new RowMapper<RangeChecksum>() {

			@Override
			public RangeChecksum mapRow(ResultSet rs, int rowNum) throws SQLException {
				RangeChecksum result = new RangeChecksum();
				result.setBinNumber(rs.getLong(1));
				result.setCount(rs.getLong(2));
				result.setMinimumId(rs.getLong(3));
				result.setMaximumId(rs.getLong(4));
				result.setChecksum(rs.getString(5));
				return result;
			}
		}, request.getBatchSize(), request.getSalt(), request.getSalt(), request.getMinimumId(),
				request.getMaximumId());
	}

	@Override
	public UnmodifiableXStream getXStream(BackupAliasType backupAliasType) {
		switch (backupAliasType){
			case TABLE_NAME:
				return TABLE_NAME_ALIAS_X_STREAM;
			case MIGRATION_TYPE_NAME:
				return MIGRATION_TYPE_NAME_ALIAS_X_STREAM;
			default:
				throw new IllegalArgumentException("Unknown type: " + backupAliasType);
		}
	}
}
