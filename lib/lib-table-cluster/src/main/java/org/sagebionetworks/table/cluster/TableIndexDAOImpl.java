package org.sagebionetworks.table.cluster;

import static org.sagebionetworks.repo.model.table.TableConstants.ANNOTATION_KEYS_PARAM_NAME;
import static org.sagebionetworks.repo.model.table.TableConstants.ANNOTATION_REPLICATION_COL_KEY;
import static org.sagebionetworks.repo.model.table.TableConstants.ANNOTATION_REPLICATION_COL_OBJECT_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ANNOTATION_REPLICATION_COL_STRING_LIST_VALUE;
import static org.sagebionetworks.repo.model.table.TableConstants.ANNOTATION_REPLICATION_COL_TYPE;
import static org.sagebionetworks.repo.model.table.TableConstants.BATCH_INSERT_REPLICATION_SYNC_EXP;
import static org.sagebionetworks.repo.model.table.TableConstants.CRC_ALIAS;
import static org.sagebionetworks.repo.model.table.TableConstants.EXCLUSION_LIST_PARAM_NAME;
import static org.sagebionetworks.repo.model.table.TableConstants.EXPIRES_PARAM_NAME;
import static org.sagebionetworks.repo.model.table.TableConstants.ID_PARAM_NAME;
import static org.sagebionetworks.repo.model.table.TableConstants.OBEJCT_REPLICATION_COL_ETAG;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_BENEFACTOR_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_CREATED_BY;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_CREATED_ON;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_FILE_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_FILE_MD5;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_FILE_SIZE_BYTES;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_IN_SYNAPSE_STORAGE;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_MODIFIED_BY;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_MODIFIED_ON;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_NAME;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_OBJECT_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_PARENT_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_PROJECT_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_SUBTYPE;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_VERSION;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_TABLE;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_TYPE_PARAM_NAME;
import static org.sagebionetworks.repo.model.table.TableConstants.PARENT_ID_PARAM_NAME;
import static org.sagebionetworks.repo.model.table.TableConstants.P_LIMIT;
import static org.sagebionetworks.repo.model.table.TableConstants.P_OFFSET;
import static org.sagebionetworks.repo.model.table.TableConstants.SELECT_NON_EXPIRED_IDS;
import static org.sagebionetworks.repo.model.table.TableConstants.SELECT_OBJECT_CHILD_CRC;
import static org.sagebionetworks.repo.model.table.TableConstants.SELECT_OBJECT_CHILD_ID_ETAG;
import static org.sagebionetworks.repo.model.table.TableConstants.SUBTYPE_PARAM_NAME;
import static org.sagebionetworks.repo.model.table.TableConstants.TRUNCATE_ANNOTATION_REPLICATION_TABLE;
import static org.sagebionetworks.repo.model.table.TableConstants.TRUNCATE_OBJECT_REPLICATION_TABLE;
import static org.sagebionetworks.repo.model.table.TableConstants.TRUNCATE_REPLICATION_SYNC_EXPIRATION_TABLE;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.json.JSONArray;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.IdAndEtag;
import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.report.SynapseStorageProjectStats;
import org.sagebionetworks.repo.model.table.AnnotationType;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.ObjectAnnotationDTO;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.repo.model.table.ObjectField;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.repo.model.table.ViewScopeFilter;
import org.sagebionetworks.repo.model.table.ViewScopeType;
import org.sagebionetworks.table.cluster.SQLUtils.TableType;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldModelResolver;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldModelResolverFactory;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldTypeMapper;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.model.Grouping;
import org.sagebionetworks.table.query.util.ColumnTypeListMappings;
import org.sagebionetworks.util.Callback;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.util.csv.CSVWriterStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class TableIndexDAOImpl implements TableIndexDAO {

	private static final String SQL_SUM_FILE_SIZES = "SELECT SUM(" + OBJECT_REPLICATION_COL_FILE_SIZE_BYTES + ")"
			+ " FROM " + OBJECT_REPLICATION_TABLE 
			+ " WHERE " + OBJECT_REPLICATION_COL_OBJECT_TYPE + " =:" + OBJECT_TYPE_PARAM_NAME 
			+ " AND " + OBJECT_REPLICATION_COL_OBJECT_ID + " IN (:" +ID_PARAM_NAME+ ")";

	public static final String SQL_SELECT_PROJECTS_BY_SIZE =
			"SELECT t1."+OBJECT_REPLICATION_COL_PROJECT_ID + ", t2." + OBJECT_REPLICATION_COL_NAME + ", t1.PROJECT_SIZE_BYTES "
			+ " FROM (SELECT " + OBJECT_REPLICATION_COL_PROJECT_ID + ", "
					+ " SUM(" + OBJECT_REPLICATION_COL_FILE_SIZE_BYTES + ") AS PROJECT_SIZE_BYTES"
					+ " FROM " + OBJECT_REPLICATION_TABLE 
					+ " WHERE " 
					+ OBJECT_REPLICATION_COL_OBJECT_TYPE + " = :" + OBJECT_TYPE_PARAM_NAME
					+ " AND " + OBJECT_REPLICATION_COL_IN_SYNAPSE_STORAGE+" = 1"
					+ " GROUP BY " + OBJECT_REPLICATION_COL_PROJECT_ID + ") t1," + OBJECT_REPLICATION_TABLE + " t2"
					+ " WHERE"
					+ " t2." + OBJECT_REPLICATION_COL_OBJECT_TYPE + " = :" + OBJECT_TYPE_PARAM_NAME
					+ " AND t1." + OBJECT_REPLICATION_COL_PROJECT_ID + " = t2." + OBJECT_REPLICATION_COL_OBJECT_ID
					+ " ORDER BY t1.PROJECT_SIZE_BYTES DESC";

	/**
	 * The MD5 used for tables with no schema.
	 */
	public static final String EMPTY_SCHEMA_MD5 = TableModelUtils.createSchemaMD5Hex(new LinkedList<String>());
	
	private static final String KEY_NAME = "Key_name";
	private static final String COLUMN_NAME = "Column_name";
	private static final String SHOW_INDEXES_FROM = "SHOW INDEXES FROM ";
	private static final String KEY = "Key";
	private static final String SQL_SHOW_COLUMNS = "SHOW FULL COLUMNS FROM ";
	private static final String FIELD = "Field";

	private DataSourceTransactionManager transactionManager;
	private TransactionTemplate writeTransactionTemplate;
	private TransactionTemplate readTransactionTemplate;
	private JdbcTemplate template;
	private NamedParameterJdbcTemplate namedTemplate;
	private ObjectFieldModelResolverFactory objectFieldModelResolverFactory;
	
	
	@Autowired
	public TableIndexDAOImpl(ObjectFieldModelResolverFactory objectFieldModelResolverFactory) {
		this.objectFieldModelResolverFactory = objectFieldModelResolverFactory;
	}

	@Override
	public void setDataSource(DataSource dataSource) {
		if(template != null) {
			throw new IllegalStateException("DataSource can only be set once");
		}
		this.transactionManager = new DataSourceTransactionManager(dataSource);
		// This will manage transactions for calls that need it.
		this.writeTransactionTemplate = createTransactionTemplate(this.transactionManager, false);
		this.readTransactionTemplate = createTransactionTemplate(this.transactionManager, true);
		/*
		 * By default the MySQL driver will read all query results into memory which can
		 * cause memory problems for large query results. (see: <a hreft=
		 * "http://dev.mysql.com/doc/connector-j/en/connector-j-reference-implementation-notes.html"
		 * />) According to the MySQL driver docs the only way to get the driver to
		 * change this default behavior is to create a statement with TYPE_FORWARD_ONLY
		 * & CONCUR_READ_ONLY and then set statement fetch size to Integer.MIN_VALUE.
		 * With Spring 4.3 {@link JdbcTemplate#setFetchSize()} allows the fetch size to
		 * be set to Integer.MIN_VALUE. See: PLFM-3429
		 */
		this.template = new JdbcTemplate(dataSource);
		// See comments above.
		this.template.setFetchSize(Integer.MIN_VALUE);
		this.namedTemplate = new NamedParameterJdbcTemplate(this.template);
	}

	private static TransactionTemplate createTransactionTemplate(DataSourceTransactionManager transactionManager, boolean readOnly) {
		// This will define how transaction are run for this instance.
		DefaultTransactionDefinition transactionDef;
		transactionDef = new DefaultTransactionDefinition();
		transactionDef.setIsolationLevel(Connection.TRANSACTION_READ_COMMITTED);
		transactionDef.setReadOnly(readOnly);
		transactionDef.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
		transactionDef.setName("TableIndexDAOImpl");
		return new TransactionTemplate(transactionManager, transactionDef);
	}

	@Override
	public void deleteTable(IdAndVersion tableId) {
		boolean alterTemp = false;
		deleteMultiValueTablesForTable(tableId, alterTemp);
		template.update(SQLUtils.dropTableSQL(tableId, SQLUtils.TableType.INDEX));
		deleteSecondaryTables(tableId);
	}
	
	/**
	 * Delete secondary table associated with the given tableId.
	 * @param tableId
	 */
	void deleteSecondaryTables(IdAndVersion tableId) {
		for(TableType type: SQLUtils.SECONDARY_TYPES){
			String dropStatusTableDML = SQLUtils.dropTableSQL(tableId, type);
			template.update(dropStatusTableDML);
		}	
	}
	
	/**
	 * Delete all multi-value tables associated with the given tableId.
	 * @param tableId
	 * @param alterTemp
	 */
	void deleteMultiValueTablesForTable(IdAndVersion tableId, boolean alterTemp) {
		List<String> tablesToDelete = getMultivalueColumnIndexTableNames(tableId, alterTemp);
		for(String tableNames: tablesToDelete) {
			template.update("DROP TABLE IF EXISTS "+tableNames);
		}
	}

	@Override
	public void createOrUpdateOrDeleteRows(final IdAndVersion tableId, final Grouping grouping) {
		ValidateArgument.required(grouping, "grouping");
		// Execute this within a transaction
		this.writeTransactionTemplate.execute(new TransactionCallback<Void>() {
			@Override
			public Void doInTransaction(TransactionStatus status) {
				// We need a named template for this case.
				NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(template);
				List<ColumnModel> groupingColumns = grouping.getColumnsWithValues();
				if(groupingColumns.isEmpty()){
					// This is a delete
					String deleteSql = SQLUtils.buildDeleteSQL(tableId);
					SqlParameterSource batchDeleteBinding = SQLUtils
							.bindParameterForDelete(grouping.getRows());
					namedTemplate.update(deleteSql, batchDeleteBinding);
				}else{
					// this is a create or update
					String createOrUpdateSql = SQLUtils.buildCreateOrUpdateRowSQL(groupingColumns, tableId);
					SqlParameterSource[] batchUpdateOrCreateBinding = SQLUtils
							.bindParametersForCreateOrUpdate(grouping);
					namedTemplate.batchUpdate(createOrUpdateSql,
							batchUpdateOrCreateBinding);
				}
				return null;
			}
		});
	}
	
	@Override
	public Long getRowCountForTable(IdAndVersion tableId) {
		String sql = SQLUtils.getCountSQL(tableId);
		try {
			return template.queryForObject(sql,new SingleColumnRowMapper<Long>());
		} catch (BadSqlGrammarException e) {
			// Spring throws this when the table does not
			return null;
		}
	}

	@Override
	public Long getMaxCurrentCompleteVersionForTable(IdAndVersion tableId) {
		String sql = SQLUtils.getStatusMaxVersionSQL(tableId);
		try {
			return template.queryForObject(sql, new SingleColumnRowMapper<Long>());
		} catch (Exception e) {
			// Spring throws this when the table is empty
			return -1L;
		}
	}

	@Override
	public void setMaxCurrentCompleteVersionForTable(IdAndVersion tableId, Long version) {
		String createOrUpdateStatusSql = SQLUtils.buildCreateOrUpdateStatusSQL(tableId);
		template.update(createOrUpdateStatusSql, version, version);
	}
	
	@Override
	public void setCurrentSchemaMD5Hex(IdAndVersion tableId, String schemaMD5Hex) {
		String createOrUpdateStatusSql = SQLUtils.buildCreateOrUpdateStatusHashSQL(tableId);
		template.update(createOrUpdateStatusSql, schemaMD5Hex, schemaMD5Hex);
	}
	
	@Override
	public void setIndexVersionAndSchemaMD5Hex(IdAndVersion tableId, Long viewCRC,
			String schemaMD5Hex) {
		String createOrUpdateStatusSql = SQLUtils.buildCreateOrUpdateStatusVersionAndHashSQL(tableId);
		template.update(createOrUpdateStatusSql, viewCRC, schemaMD5Hex, viewCRC, schemaMD5Hex);
	}

	@Override
	public String getCurrentSchemaMD5Hex(IdAndVersion tableId) {
		String sql = SQLUtils.getSchemaHashSQL(tableId);
		try {
			return template.queryForObject(sql, new SingleColumnRowMapper<String>());
		} catch (Exception e) {
			// Spring throws this when the table is empty
			return EMPTY_SCHEMA_MD5;
		}
	}
	
	@Override
	public void createSecondaryTables(IdAndVersion tableId) {
		for(TableType type: SQLUtils.SECONDARY_TYPES){
			String sql = SQLUtils.createTableSQL(tableId, type);
			template.update(sql);
		}	
	}

	@Override
	public JdbcTemplate getConnection() {
		return template;
	}

	@Override
	public RowSet query(ProgressCallback callback, final SqlQuery query) {
		if (query == null)
			throw new IllegalArgumentException("SqlQuery cannot be null");
		final List<Row> rows = new LinkedList<Row>();
		final RowSet rowSet = new RowSet();
		rowSet.setRows(rows);
		rowSet.setHeaders(query.getSelectColumns());
		// Stream over the results and save the results in a a list
		queryAsStream(callback, query, new RowHandler() {
			@Override
			public void nextRow(Row row) {
				rows.add(row);
			}
		});
		rowSet.setTableId(query.getTableId());
		return rowSet;
	}
	
	@Override
	public Long countQuery(String sql, Map<String, Object> parameters) {
		ValidateArgument.required(sql, "sql");
		ValidateArgument.required(parameters, "parameters");
		// We use spring to create create the prepared statement
		return namedTemplate.queryForObject(sql, new MapSqlParameterSource(parameters), Long.class);
	}
	
	@Override
	public boolean queryAsStream(final ProgressCallback callback, final SqlQuery query, final RowHandler handler) {
		ValidateArgument.required(query, "Query");
		final ColumnTypeInfo[] infoArray = SQLTranslatorUtils.getColumnTypeInfoArray(query.getSelectColumns());
		// We use spring to create create the prepared statement
		namedTemplate.query(query.getOutputSQL(), new MapSqlParameterSource(query.getParameters()), new RowCallbackHandler() {
			@Override
			public void processRow(ResultSet rs) throws SQLException {
				Row row = SQLTranslatorUtils.readRow(rs, query.includesRowIdAndVersion(), query.includeEntityEtag(), infoArray);
				handler.nextRow(row);
			}
		});
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.table.cluster.TableIndexDAO#executeInReadTransaction(org.springframework.transaction.support.TransactionCallback)
	 */
	@Override
	public <T> T executeInReadTransaction(TransactionCallback<T> callable) {
		return readTransactionTemplate.execute(callable);
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.table.cluster.TableIndexDAO#executeInWriteTransaction(org.springframework.transaction.support.TransactionCallback)
	 */
	@Override
	public <T> T executeInWriteTransaction(TransactionCallback<T> callable) {
		return writeTransactionTemplate.execute(callable);
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.table.cluster.TableIndexDAO#applyFileHandleIdsToTable(java.lang.String, java.util.Set)
	 */
	@Override
	public void applyFileHandleIdsToTable(final IdAndVersion tableId,
			final Set<Long> fileHandleIds) {
	
		this.writeTransactionTemplate.execute(new TransactionCallback<Void>() {
			@Override
			public Void doInTransaction(
					TransactionStatus status) {
				String sql = SQLUtils.createSQLInsertIgnoreFileHandleId(tableId);
				final Long[] args = fileHandleIds.toArray(new Long[fileHandleIds.size()]);
				template.batchUpdate(sql, new BatchPreparedStatementSetter() {
					@Override
					public void setValues(PreparedStatement ps, int parameterIndex) throws SQLException {
						ps.setLong(1, args[parameterIndex]);
					}
					
					@Override
					public int getBatchSize() {
						return fileHandleIds.size();
					}
				});
				return null;
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.table.cluster.TableIndexDAO#getFileHandleIdsAssociatedWithTable(java.util.Set, java.lang.String)
	 */
	@Override
	public Set<Long> getFileHandleIdsAssociatedWithTable(
			final Set<Long> fileHandleIds, final IdAndVersion tableId) {
		try {
			return this.readTransactionTemplate.execute(new TransactionCallback<Set<Long>>() {
				@Override
				public Set<Long> doInTransaction(TransactionStatus status) {
					String sql = SQLUtils.createSQLGetBoundFileHandleId(tableId);
					NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(template);
					Map<String, Set<Long>> params = Maps.newHashMap();
					params.put(SQLUtils.FILE_ID_BIND, fileHandleIds);
					final Set<Long> intersection = Sets.newHashSet();
					namedTemplate.query(sql, params, new RowCallbackHandler() {
						@Override
						public void processRow(ResultSet rs) throws SQLException {
							intersection.add(rs.getLong(TableConstants.FILE_ID));
						}
					});
					return intersection;
				}
			});
		} catch (BadSqlGrammarException e) {
			// thrown when the table does not exist
			return new HashSet<Long>(0);
		}
	}
	

	@Override
	public boolean doesIndexStateMatch(IdAndVersion tableId, long versionNumber, String schemaMD5Hex) {
		long indexVersion = getMaxCurrentCompleteVersionForTable(tableId);
		if(indexVersion != versionNumber){
			return false;
		}
		String indexMD5Hex = getCurrentSchemaMD5Hex(tableId);
		return indexMD5Hex.equals(schemaMD5Hex);
	}

	@Override
	public Set<Long> getDistinctLongValues(IdAndVersion tableId, String columnName) {
		String sql = SQLUtils.createSQLGetDistinctValues(tableId, columnName);
		List<Long> results = template.queryForList(sql, Long.class);
		return new HashSet<Long>(results);
	}

	@Override
	public void createTableIfDoesNotExist(IdAndVersion tableId, boolean isView) {
		String sql = SQLUtils.createTableIfDoesNotExistSQL(tableId, isView);
		template.update(sql);
	}

	@Override
	public boolean alterTableAsNeeded(IdAndVersion tableId, List<ColumnChangeDetails> changes, boolean alterTemp) {
		String sql = SQLUtils.createAlterTableSql(changes, tableId, alterTemp);
		if(sql == null){
			// no change are needed.
			return false;
		}
		// apply the update
		template.update(sql);

		return true;
	}

	@Override
	public Set<Long> getMultivalueColumnIndexTableColumnIds(IdAndVersion tableId){
		boolean alterTemp = false;
		return getMultivalueColumnIndexTableNames(tableId, alterTemp)
				.stream()
				.map((String indexTableName) -> SQLUtils.getColumnIdFromMultivalueColumnIndexTableName(tableId, indexTableName))
				.collect(Collectors.toSet());
	}

	private List<String> getMultivalueColumnIndexTableNames(IdAndVersion tableId, boolean alterTemp){
		String multiValueTableNamePrefix = SQLUtils.getTableNamePrefixForMultiValueColumns(tableId, alterTemp);
		return template.queryForList("SHOW TABLES LIKE '"+multiValueTableNamePrefix+"%'", String.class);
	}

	@Override
	public void createMultivalueColumnIndexTable(IdAndVersion tableId, ColumnModel columnModel, boolean alterTemp){
		template.update(SQLUtils.createListColumnIndexTable(tableId, columnModel, alterTemp));
	}

	@Override
	public void deleteMultivalueColumnIndexTable(IdAndVersion tableId, Long columnId, boolean alterTemp){
		String tableName = SQLUtils.getTableNameForMultiValueColumnIndex(tableId, columnId.toString(), alterTemp);
		template.update("DROP TABLE IF EXISTS " + tableName);
	}


	@Override
	public void updateMultivalueColumnIndexTable(IdAndVersion tableId, Long oldColumnId, ColumnModel newColumn, boolean alterTemp){
		String sql = SQLUtils.createAlterListColumnIndexTable(tableId, oldColumnId, newColumn, alterTemp);
		template.update(sql);
	}


	@Override
	public void truncateTable(IdAndVersion tableId) {
		String sql = SQLUtils.createTruncateSql(tableId);
		template.update(sql);
	}

	@Override
	public List<DatabaseColumnInfo> getDatabaseInfo(IdAndVersion tableId) {
		try {
			String tableName = SQLUtils.getTableNameForId(tableId, SQLUtils.TableType.INDEX);
			// Bind variables do not seem to work here
			return template.query(SQL_SHOW_COLUMNS + tableName, new RowMapper<DatabaseColumnInfo>() {
				@Override
				public DatabaseColumnInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
					DatabaseColumnInfo info = new DatabaseColumnInfo();
					info.setColumnName(rs.getString(FIELD));
					String key = rs.getString(KEY);
					info.setHasIndex(!"".equals(key));
					String typeString = rs.getString("Type");
					info.setType(MySqlColumnType.parserType(typeString));
					if(info.getType() != null && info.getType().hasSize()){
						info.setMaxSize(MySqlColumnType.parseSize(typeString));
					}
					String comment = rs.getString("Comment");
					if(comment != null && !"".equals(comment)){
						info.setColumnType(ColumnType.valueOf(comment));
					}
					return info;
				}
			});
		} catch (BadSqlGrammarException e) {
			// Spring throws this when the table does not
			return new LinkedList<DatabaseColumnInfo>();
		}
	}


	@Override
	public void provideCardinality(final List<DatabaseColumnInfo> list,
			IdAndVersion tableId) {
		ValidateArgument.required(list, "list");
		ValidateArgument.required(tableId, "tableId");
		if(list.isEmpty()){
			return;
		}
		String sql = SQLUtils.createCardinalitySql(list, tableId);
		template.query(sql, new RowCallbackHandler() {
			@Override
			public void processRow(ResultSet rs) throws SQLException {
				for (DatabaseColumnInfo info : list) {
					info.setCardinality(rs.getLong(info.getColumnName()));
				}
			}
		});
	}

	@Override
	public void provideIndexName(List<DatabaseColumnInfo> list, IdAndVersion tableId) {
		ValidateArgument.required(list, "list");
		ValidateArgument.required(tableId, "tableId");
		if(list.isEmpty()){
			return;
		}
		final Map<String, DatabaseColumnInfo> nameToInfoMap = new HashMap<String, DatabaseColumnInfo>(list.size());
		for(DatabaseColumnInfo info: list){
			nameToInfoMap.put(info.getColumnName(), info);
		}
		String tableName = SQLUtils.getTableNameForId(tableId, SQLUtils.TableType.INDEX);
		template.query(SHOW_INDEXES_FROM+tableName, new RowCallbackHandler() {
			@Override
			public void processRow(ResultSet rs) throws SQLException {
				String columnName = rs.getString(COLUMN_NAME);
				String indexName = rs.getString(KEY_NAME);
				DatabaseColumnInfo info = nameToInfoMap.get(columnName);
				if(info == null){
					throw new IllegalArgumentException("Provided List<DatabaseColumnInfo> has no match for column: "+columnName);
				}
				info.setIndexName(indexName);
			}
		});
	}

	@Override
	public void optimizeTableIndices(List<DatabaseColumnInfo> list,
			IdAndVersion tableId, int maxNumberOfIndex) {
		String alterSql = SQLUtils.createOptimizedAlterIndices(list, tableId, maxNumberOfIndex);
		if(alterSql == null){
			// No changes are needed.
			return;
		}
		template.update(alterSql);
	}
	

	@Override
	public void populateListColumnIndexTable(IdAndVersion tableId, ColumnModel listColumn, Set<Long> rowIds, boolean alterTemp){
		ValidateArgument.required(tableId, "tableId");
		ValidateArgument.required(listColumn, "listColumn");
		//only operate on list column types
		if (!ColumnTypeListMappings.isList(listColumn.getColumnType())) {
			throw new IllegalArgumentException("Only valid for List type columns");
		}
		if(rowIds != null && rowIds.isEmpty()) {
			throw new IllegalArgumentException("When rowIds is provided (not null) it cannot be empty");
		}
		boolean filterRows = rowIds != null;
		String insertIntoSql = SQLUtils.insertIntoListColumnIndexTable(tableId, listColumn, filterRows, alterTemp);
		MapSqlParameterSource param = new MapSqlParameterSource();
		if(filterRows) {
			param.addValue(ID_PARAM_NAME, rowIds);
		}
		try {
			namedTemplate.update(insertIntoSql, param);
		} catch (DataIntegrityViolationException e){
			throw new IllegalArgumentException("The size of the column '"+ listColumn.getName()+ "' is too small." +
					" Unable to automatically determine the necessary size to fit all values in a STRING_LIST column", e);
		}
	}

	@Override
	public void deleteFromListColumnIndexTable(IdAndVersion tableId, ColumnModel listColumn, Set<Long> rowIds){
		ValidateArgument.required(tableId, "tableId");
		ValidateArgument.required(listColumn, "listColumn");
		ValidateArgument.required(listColumn.getId(), "listColumn.id");
		ValidateArgument.requiredNotEmpty(rowIds, "rowIds");
		ValidateArgument.requirement(ColumnTypeListMappings.isList(listColumn.getColumnType()), "Only valid for List type columns");

		String rowIdsParameter = "rowIds";

		namedTemplate.update("DELETE FROM " + SQLUtils.getTableNameForMultiValueColumnIndex(tableId, listColumn.getId()) +
				" WHERE " + SQLUtils.getRowIdRefColumnNameForId(listColumn.getId()) + " IN (:"+rowIdsParameter+")" ,
				Collections.singletonMap(rowIdsParameter, rowIds));
	}


	@Override
	public void createTemporaryTable(IdAndVersion tableId) {
		String sql = SQLUtils.createTempTableSql(tableId);
		template.update(sql);
	}

	@Override
	public void copyAllDataToTemporaryTable(IdAndVersion tableId) {
		String sql = SQLUtils.copyTableToTempSql(tableId);
		template.update(sql);
	}

	@Override
	public void deleteTemporaryTable(IdAndVersion tableId) {
		String sql = SQLUtils.deleteTempTableSql(tableId);
		template.update(sql);
	}

	@Override
	public long getTempTableCount(IdAndVersion tableId) {
		String sql = SQLUtils.countTempRowsSql(tableId);
		try {
			return template.queryForObject(sql, Long.class);
		} catch (DataAccessException e) {
			return 0L;
		}
	}

	@Override
	public long getTempTableMultiValueColumnIndexCount(IdAndVersion tableId, String columnName) {
		boolean alterTemp = true;
		String sql = "SELECT COUNT(*) FROM " + SQLUtils.getTableNameForMultiValueColumnIndex(tableId, columnName, alterTemp);
		try {
			return template.queryForObject(sql, Long.class);
		} catch (DataAccessException e) {
			return 0L;
		}
	}

	@Override
	public void createTemporaryMultiValueColumnIndexTable(IdAndVersion tableId, String columnId){
		String[] sqlBatch = SQLUtils.createTempMultiValueColumnIndexTableSql(tableId, columnId);
		template.batchUpdate(sqlBatch);
	}

	@Override
	public void copyAllDataToTemporaryMultiValueColumnIndexTable(IdAndVersion tableId, String columnId) {
		String sql = SQLUtils.copyMultiValueColumnIndexTableToTempSql(tableId, columnId);
		template.update(sql);
	}

	@Override
	public void deleteAllTemporaryMultiValueColumnIndexTable(IdAndVersion tableId) {
		boolean alterTemp = true;
		deleteMultiValueTablesForTable(tableId, alterTemp);
	}

	@Override
	public void createObjectReplicationTablesIfDoesNotExist(){
		template.update(TableConstants.OBJECT_REPLICATION_TABLE_CREATE);
		template.update(TableConstants.ANNOTATION_REPLICATION_TABLE_CREATE);
		template.update(TableConstants.REPLICATION_SYNCH_EXPIRATION_TABLE_CREATE);
	}

	@Override
	public void deleteObjectData(ViewObjectType objectsType, List<Long> objectIds) {
		final List<Long> sorted = new ArrayList<Long>(objectIds);
		// sort to prevent deadlock.
		Collections.sort(sorted);
		// Batch delete.
		template.batchUpdate(TableConstants.OBJECT_REPLICATION_DELETE_ALL, new BatchPreparedStatementSetter(){

			@Override
			public void setValues(PreparedStatement ps, int i)
					throws SQLException {
				int parameterIndex = 1;
				ps.setString(parameterIndex++, objectsType.name());
				ps.setLong(parameterIndex++, sorted.get(i));
			}

			@Override
			public int getBatchSize() {
				return sorted.size();
			}});
		
	}

	@Override
	public void addObjectData(ViewObjectType objectType, List<ObjectDataDTO> objectDtos) {
		
		Map<Long, ObjectDataDTO> deduplicated = objectDtos.stream().collect(
				Collectors.toMap(ObjectDataDTO::getId, Function.identity(), (a, b) -> b)
		);
		
		final List<ObjectDataDTO> sorted = new ArrayList<ObjectDataDTO>(deduplicated.values());
		
		Collections.sort(sorted);
		
		// batch update the object replication table
		template.batchUpdate(TableConstants.OBJECT_REPLICATION_INSERT_OR_UPDATE, new BatchPreparedStatementSetter(){

			@Override
			public void setValues(PreparedStatement ps, int i)
					throws SQLException {
				ObjectDataDTO dto = sorted.get(i);
				int parameterIndex = 1;
				int updateOffset = 14;
				
				ps.setString(parameterIndex++, objectType.name());
				ps.setLong(parameterIndex++, dto.getId());
				
				ps.setLong(parameterIndex++, dto.getCurrentVersion());
				ps.setLong(parameterIndex + updateOffset, dto.getCurrentVersion());
				
				ps.setLong(parameterIndex++, dto.getCreatedBy());
				ps.setLong(parameterIndex + updateOffset, dto.getCreatedBy());
				
				ps.setLong(parameterIndex++, dto.getCreatedOn().getTime());
				ps.setLong(parameterIndex + updateOffset, dto.getCreatedOn().getTime());
				
				ps.setString(parameterIndex++, dto.getEtag());
				ps.setString(parameterIndex + updateOffset, dto.getEtag());
				
				ps.setString(parameterIndex++, dto.getName());
				ps.setString(parameterIndex + updateOffset, dto.getName());
				
				ps.setString(parameterIndex++, dto.getSubType());
				ps.setString(parameterIndex + updateOffset, dto.getSubType());
				
				if(dto.getParentId() != null){
					ps.setLong(parameterIndex++, dto.getParentId());
					ps.setLong(parameterIndex + updateOffset, dto.getParentId());
				}else{
					ps.setNull(parameterIndex++, java.sql.Types.BIGINT);
					ps.setNull(parameterIndex + updateOffset, java.sql.Types.BIGINT);
				}
				if(dto.getBenefactorId() != null){
					ps.setLong(parameterIndex++, dto.getBenefactorId());
					ps.setLong(parameterIndex + updateOffset, dto.getBenefactorId());
				}else{
					ps.setNull(parameterIndex++, java.sql.Types.BIGINT);
					ps.setNull(parameterIndex + updateOffset, java.sql.Types.BIGINT);
				}
				if(dto.getProjectId() != null){
					ps.setLong(parameterIndex++, dto.getProjectId());
					ps.setLong(parameterIndex + updateOffset, dto.getProjectId());
				}else{
					ps.setNull(parameterIndex++, java.sql.Types.BIGINT);
					ps.setNull(parameterIndex + updateOffset, java.sql.Types.BIGINT);
				}
				
				ps.setLong(parameterIndex++, dto.getModifiedBy());
				ps.setLong(parameterIndex + updateOffset, dto.getModifiedBy());
				
				ps.setLong(parameterIndex++, dto.getModifiedOn().getTime());
				ps.setLong(parameterIndex + updateOffset, dto.getModifiedOn().getTime());
				
				if(dto.getFileHandleId() != null){
					ps.setLong(parameterIndex++, dto.getFileHandleId());
					ps.setLong(parameterIndex + updateOffset, dto.getFileHandleId());
				}else{
					ps.setNull(parameterIndex++, java.sql.Types.BIGINT);
					ps.setNull(parameterIndex + updateOffset, java.sql.Types.BIGINT);
				}
				if(dto.getFileSizeBytes() != null) {
					ps.setLong(parameterIndex++, dto.getFileSizeBytes());
					ps.setLong(parameterIndex + updateOffset, dto.getFileSizeBytes());
				}else{
					ps.setNull(parameterIndex++, java.sql.Types.BIGINT);
					ps.setNull(parameterIndex + updateOffset, java.sql.Types.BIGINT);
				}
				if(dto.getIsInSynapseStorage() != null) {
					ps.setBoolean(parameterIndex++, dto.getIsInSynapseStorage());
					ps.setBoolean(parameterIndex + updateOffset, dto.getIsInSynapseStorage());
				}else{
					ps.setNull(parameterIndex++, java.sql.Types.BOOLEAN);
					ps.setNull(parameterIndex + updateOffset, java.sql.Types.BOOLEAN);
				}
				if(dto.getFileMD5() != null) {
					ps.setString(parameterIndex++, dto.getFileMD5());
					ps.setString(parameterIndex + updateOffset, dto.getFileMD5());
				}else {
					ps.setNull(parameterIndex++, java.sql.Types.VARCHAR);
					ps.setNull(parameterIndex + updateOffset, java.sql.Types.VARCHAR);
				}
			}

			@Override
			public int getBatchSize() {
				return sorted.size();
			}});
		// map the entities with annotations
		final List<ObjectAnnotationDTO> annotations = new ArrayList<ObjectAnnotationDTO>();
		for(int i=0; i<sorted.size(); i++){
			ObjectDataDTO dto = sorted.get(i);
			if(dto.getAnnotations() != null && !dto.getAnnotations().isEmpty()){
				// this index has annotations.
				annotations.addAll(dto.getAnnotations());
			}
		}
		// update the annotations
		template.batchUpdate(TableConstants.ANNOTATION_REPLICATION_INSERT_OR_UPDATE, new BatchPreparedStatementSetter(){

			@Override
			public void setValues(PreparedStatement ps, int i)
					throws SQLException {
				ObjectAnnotationDTO dto = annotations.get(i);
				SQLUtils.writeAnnotationDtoToPreparedStatement(objectType, ps, dto);
			}

			@Override
			public int getBatchSize() {
				return annotations.size();
			}});
		
	}

	@Override
	public ObjectDataDTO getObjectData(ViewObjectType objectType, Long objectId) {
		// query for the template.
		ObjectDataDTO dto;
		try {
			dto = template.queryForObject(TableConstants.OBJECT_REPLICATION_GET, (ResultSet rs, int rowNum) -> {
				ObjectDataDTO dto1 = new ObjectDataDTO();
				dto1.setId(rs.getLong(OBJECT_REPLICATION_COL_OBJECT_ID));
				dto1.setCurrentVersion(rs.getLong(OBJECT_REPLICATION_COL_VERSION));
				dto1.setCreatedBy(rs.getLong(OBJECT_REPLICATION_COL_CREATED_BY));
				dto1.setCreatedOn(new Date(rs.getLong(OBJECT_REPLICATION_COL_CREATED_ON)));
				dto1.setEtag(rs.getString(OBEJCT_REPLICATION_COL_ETAG));
				dto1.setName(rs.getString(OBJECT_REPLICATION_COL_NAME));
				dto1.setSubType(rs.getString(OBJECT_REPLICATION_COL_SUBTYPE));
				dto1.setParentId(rs.getLong(OBJECT_REPLICATION_COL_PARENT_ID));
				if (rs.wasNull()) {
					dto1.setParentId(null);
				}
				dto1.setBenefactorId(rs.getLong(OBJECT_REPLICATION_COL_BENEFACTOR_ID));
				if (rs.wasNull()) {
					dto1.setBenefactorId(null);
				}
				dto1.setProjectId(rs.getLong(OBJECT_REPLICATION_COL_PROJECT_ID));
				if (rs.wasNull()) {
					dto1.setProjectId(null);
				}
				dto1.setModifiedBy(rs.getLong(OBJECT_REPLICATION_COL_MODIFIED_BY));
				dto1.setModifiedOn(new Date(rs.getLong(OBJECT_REPLICATION_COL_MODIFIED_ON)));
				dto1.setFileHandleId(rs.getLong(OBJECT_REPLICATION_COL_FILE_ID));
				if (rs.wasNull()) {
					dto1.setFileHandleId(null);
				}
				dto1.setFileSizeBytes(rs.getLong(OBJECT_REPLICATION_COL_FILE_SIZE_BYTES));
				if (rs.wasNull()) {
					dto1.setFileSizeBytes(null);
				}
				dto1.setIsInSynapseStorage(rs.getBoolean(OBJECT_REPLICATION_COL_IN_SYNAPSE_STORAGE));
				if (rs.wasNull()) {
					dto1.setIsInSynapseStorage(null);
				}
				dto1.setFileMD5(rs.getString(OBJECT_REPLICATION_COL_FILE_MD5));

				return dto1;
			}, objectType.name(), objectId);
		} catch (DataAccessException e) {
			return null;
		}
		// get the annotations.
		@SuppressWarnings("unchecked")
		List<ObjectAnnotationDTO> annotations = template.query(TableConstants.ANNOTATION_REPLICATION_GET, (ResultSet rs, int rowNum) -> {
			ObjectAnnotationDTO dto1 = new ObjectAnnotationDTO();
			dto1.setObjectId(rs.getLong(ANNOTATION_REPLICATION_COL_OBJECT_ID));
			dto1.setKey(rs.getString(ANNOTATION_REPLICATION_COL_KEY));
			dto1.setType(AnnotationType.valueOf(rs.getString(ANNOTATION_REPLICATION_COL_TYPE)));
			dto1.setValue((List<String>) (List<?>) new JSONArray(rs.getString(ANNOTATION_REPLICATION_COL_STRING_LIST_VALUE))
							.toList());
			return dto1;
		}, objectType.name(), objectId);
		
		if (!annotations.isEmpty()) {
			dto.setAnnotations(annotations);
		}
		
		return dto;
	}
	
	@Override
	public long calculateCRC32ofTableView(Long viewId){
		String sql = SQLUtils.buildTableViewCRC32Sql(viewId);
		Long result = this.template.queryForObject(sql, Long.class);
		if(result == null){
			return -1L;
		}
		return result;
	}

	/***
	 * Get the maximum number of elements in a list for each annotation column
	 * @param objectType
	 * @param viewId
	 * @param viewTypeMask
	 * @param allContainersInScope
	 * @param objectIdFilter
	 * @return Map where the key is the columnId found in curentSchema,
	 * and value is the maximum number of list elements for that annotation
	 */
	Map<String, Long> getMaxListSizeForAnnotations(ViewScopeFilter scopeFilter, Set<String> annotationNames, Set<Long> objectIdFilter){
		ValidateArgument.required(scopeFilter, "scopeFilter");
		ValidateArgument.required(scopeFilter.getContainerIds(), "scopeFilter.containerIds");
		ValidateArgument.required(annotationNames, "annotationNames");
		
		Set<Long> allContainersInScope = scopeFilter.getContainerIds();
		
		if(allContainersInScope.isEmpty() || annotationNames.isEmpty()){
			// nothing to do if the scope or annotation names are empty.
			return Collections.emptyMap();
		}
		if(objectIdFilter != null && objectIdFilter.isEmpty()) {
			throw new IllegalArgumentException("When objectIdFilter is provided (not null) it cannot be empty");
		}

		boolean filterByRows = objectIdFilter != null;
		
		MapSqlParameterSource param = getMapSqlParameterSourceForScopeFilter(scopeFilter, true, objectIdFilter);

		//additional param
		param.addValue(ANNOTATION_KEYS_PARAM_NAME, annotationNames);
		String sql = SQLUtils.createAnnotationMaxListLengthSQL(scopeFilter, annotationNames, filterByRows);
		return namedTemplate.query(sql,param, resultSet -> {
			Map<String, Long> result = new HashMap<>();
			while(resultSet.next()){
				result.put(resultSet.getString(1),resultSet.getLong(2));
			}
			return result;
		});
	}

	void validateMaxListLengthInAnnotationReplication(ViewScopeFilter scopeFilter, List<ColumnModel> currentSchema, Set<Long> objectIdFilter){
		Map<String,Long> listAnnotationListLengthMaximum = currentSchema.stream()
				.filter(cm -> ColumnTypeListMappings.isList(cm.getColumnType()))
				.collect(Collectors.toMap(
						ColumnModel::getName,
						ColumnModel::getMaximumListLength
				));
		if(listAnnotationListLengthMaximum.isEmpty()){
			//nothing to validate
			return;
		}

		Map<String,Long> maxLengthsInReplication = this.getMaxListSizeForAnnotations(scopeFilter, listAnnotationListLengthMaximum.keySet(), objectIdFilter);

		for(Entry<String,Long> entry : listAnnotationListLengthMaximum.entrySet()){
			String annotationName = entry.getKey();
			long maxListLength = entry.getValue();
			long maxLengthInReplication = maxLengthsInReplication.getOrDefault(annotationName,0L);
			if(maxLengthInReplication > maxListLength){
				throw new IllegalArgumentException("maximumListLength for ColumnModel \""
						+ annotationName + "\" must be at least: " + maxLengthInReplication);
			}
		}
	}
	@Override
	public long tempTableListColumnMaxLength(IdAndVersion tableId, String columnId){
		String sql = "SELECT IFNULL(MAX(JSON_LENGTH(" + SQLUtils.getColumnNameForId(columnId) + ")),0) " +
				"FROM " + SQLUtils.getTemporaryTableName(tableId);
		return template.queryForObject(sql, Long.class);
	}

	@Override
	public void copyObjectReplicationToView(Long viewId, ViewScopeFilter scopeFilter, List<ColumnModel> currentSchema, ObjectFieldTypeMapper fieldTypeMapper) {
		Set<Long> rowIdsToCopy = null;
		copyObjectReplicationToView(viewId, scopeFilter, currentSchema, fieldTypeMapper, rowIdsToCopy);
	}
	
	@Override
	public void copyObjectReplicationToView(Long viewId, ViewScopeFilter scopeFilter, List<ColumnModel> currentSchema, ObjectFieldTypeMapper fieldTypeMapper, Set<Long> rowIdsToCopy) {
		ValidateArgument.required(scopeFilter, "scopeFilter");
		ValidateArgument.required(scopeFilter.getContainerIds(), "scopeFilter.containerIds");
		
		Set<Long> allContainersInScope = scopeFilter.getContainerIds();
		
		if (allContainersInScope.isEmpty()){
			// nothing to do if the scope is empty.
			return;
		}
		
		if(rowIdsToCopy != null && rowIdsToCopy.isEmpty()) {
			throw new IllegalArgumentException("When objectIdFilter is provided (not null) it cannot be empty");
		}
		
		// before updating. verify that all rows that would be changed won't exceed the user-specified maxListLength,
		// which is used for query row size estimation
		validateMaxListLengthInAnnotationReplication(scopeFilter, currentSchema, rowIdsToCopy);
		
		// Filter by rows only if provided.
		boolean filterByRows = rowIdsToCopy != null;
		
		MapSqlParameterSource param = getMapSqlParameterSourceForScopeFilter(scopeFilter, true, rowIdsToCopy);
		
		List<ColumnMetadata> metadata = translateSchema(currentSchema, fieldTypeMapper);
		
		String sql = SQLUtils.createSelectInsertFromObjectReplication(viewId, metadata, scopeFilter, filterByRows);
		
		namedTemplate.update(sql, param);
	}

	@Override
	public void createViewSnapshotFromObjectReplication(Long viewId, ViewScopeFilter scopeFilter, List<ColumnModel> currentSchema,
			ObjectFieldTypeMapper fieldTypeMapper, CSVWriterStream outputStream) {
		ValidateArgument.required(scopeFilter, "scopeFilter");
		ValidateArgument.required(scopeFilter.getContainerIds(), "scopeFilter.containerIds");
		
		Set<Long> allContainersInScope = scopeFilter.getContainerIds();
		
		if (allContainersInScope.isEmpty()) {
			// nothing to do if the scope is empty.
			throw new IllegalArgumentException("Scope has not been defined for this view.");
		}
		
		MapSqlParameterSource param = getMapSqlParameterSourceForScopeFilter(scopeFilter, true, null);
		
		StringBuilder builder = new StringBuilder();
		boolean filterByRows = false;
		
		List<ColumnMetadata> metadata = translateSchema(currentSchema, fieldTypeMapper);
		
		List<String> headers = SQLUtils.createSelectFromObjectReplication(builder, metadata, scopeFilter, filterByRows);
		
		// push the headers to the stream
		outputStream.writeNext(headers.toArray(new String[headers.size()]));
		
		namedTemplate.query(builder.toString(), param, (ResultSet rs) -> {
			// Push each row to the callback
			String[] row = new String[headers.size()];
			for (int i = 0; i < headers.size(); i++) {
				row[i] = rs.getString(i + 1);
			}
			outputStream.writeNext(row);
		});
	}
	
	// Translates the Column Model schema into a column metadata schema, that maps to the object/annotation replication index
	List<ColumnMetadata> translateSchema(List<ColumnModel> schema, ObjectFieldTypeMapper fieldTypeMapper) {
		ValidateArgument.required(schema, "schema");
		ValidateArgument.required(fieldTypeMapper, "fieldTypeMapper");
		
		if (schema.isEmpty()) {
			return Collections.emptyList();
		}
		
		ObjectFieldModelResolver fieldModelResolver = objectFieldModelResolverFactory.getObjectFieldModelResolver(fieldTypeMapper);
		
		return schema.stream()
			.map((ColumnModel columnModel) -> translateColumnModel(columnModel, fieldModelResolver))
			.collect(Collectors.toList());
	}
	
	ColumnMetadata translateColumnModel(ColumnModel model, ObjectFieldModelResolver objectFieldModelResolver) {
		// First determine if this an object column or an annotation
		Optional<ObjectField> objectField = objectFieldModelResolver.findMatch(model);
		
		String selectColumnForId = SQLUtils.getColumnNameForId(model.getId());
		boolean isObjectReplicationField;
		String selectColumnName;
		
		if (objectField.isPresent()) {
			isObjectReplicationField = true;
			selectColumnName = objectField.get().getDatabaseColumnName();
		} else {
			isObjectReplicationField = false;
			selectColumnName = SQLUtils.translateColumnTypeToAnnotationValueName(model.getColumnType());
		}
		
		return new ColumnMetadata(model, selectColumnName, selectColumnForId, isObjectReplicationField);
	}

	@Override
	public List<ColumnModel> getPossibleColumnModelsForContainers(ViewScopeFilter scopeFilter, List<String> excludeKeys, Long limit, Long offset) {
		ValidateArgument.required(scopeFilter, "scopeFilter");
		ValidateArgument.required(limit, "limit");
		ValidateArgument.required(offset, "offset");
		
		Set<Long> containerIds = scopeFilter.getContainerIds();
		
		if(containerIds.isEmpty()){
			return Collections.emptyList();
		}
		
		MapSqlParameterSource param = getMapSqlParameterSourceForScopeFilter(scopeFilter, true, null);
		
		param.addValue(P_LIMIT, limit);
		param.addValue(P_OFFSET, offset);
		
		boolean withExclusionList = false;
		
		if (excludeKeys != null && !excludeKeys.isEmpty()) {
			withExclusionList = true;
			param.addValue(EXCLUSION_LIST_PARAM_NAME, excludeKeys);
		}
		
		String sql = SQLUtils.getDistinctAnnotationColumnsSql(scopeFilter, withExclusionList);
		
		List<ColumnAggregation> results = namedTemplate.query(sql, param, (ResultSet rs, int rowNum) -> {
			ColumnAggregation aggregation = new ColumnAggregation();
			aggregation.setColumnName(rs.getString(ANNOTATION_REPLICATION_COL_KEY));
			aggregation.setColumnTypeConcat(rs.getString(2));
			aggregation.setMaxStringElementSize(rs.getLong(3));
			aggregation.setMaxListSize(rs.getLong(4));
			return aggregation;
		});
		// convert from the aggregation to column models.
		return expandFromAggregation(results);
	}
	
	/**
	 * Expand the given column aggregations into column model objects.
	 * This was added for PLFM-5034
	 * 
	 * @param aggregations
	 * @return
	 */
	public static List<ColumnModel> expandFromAggregation(List<ColumnAggregation> aggregations){
		List<ColumnModel> results = new LinkedList<>();
		for(ColumnAggregation aggregation: aggregations) {
			String[] typeSplit = aggregation.getColumnTypeConcat().split(",");
			for(String typeString: typeSplit) {
				ColumnModel model = new ColumnModel();
				model.setName(aggregation.getColumnName());
				ColumnType type = AnnotationType.valueOf(typeString).getColumnType();

				//check if a LIST columnType needs to be used
				if(aggregation.getMaxListSize() > 1){
					try {
						type = ColumnTypeListMappings.listType(type);
						model.setMaximumListLength(aggregation.getMaxListSize());
					} catch (IllegalArgumentException e){
						//do nothing because a list type mapping does not exist
					}
				}

				model.setColumnType(type);
				if(ColumnType.STRING == type || ColumnType.STRING_LIST==type) {
					model.setMaximumSize(aggregation.getMaxStringElementSize());
				}
				results.add(model);
			}
		}
		return results;
	}
	
	@Override
	public Map<Long, Long> getSumOfChildCRCsForEachParent(ViewObjectType objectType, List<Long> parentIds) {
		ValidateArgument.required(objectType, "objectType");
		ValidateArgument.required(parentIds, "parentIds");
		final Map<Long, Long> results = new HashMap<>();
		if(parentIds.isEmpty()){
			return results;
		}
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(OBJECT_TYPE_PARAM_NAME, objectType.name());
		param.addValue(PARENT_ID_PARAM_NAME, parentIds);
		namedTemplate.query(SELECT_OBJECT_CHILD_CRC, param, (ResultSet rs) -> {
			Long parentId = rs.getLong(OBJECT_REPLICATION_COL_PARENT_ID);
			Long crc = rs.getLong(CRC_ALIAS);
			results.put(parentId, crc);
		});
		return results;
	}

	@Override
	public List<IdAndEtag> getObjectChildren(ViewObjectType objectType, Long parentId) {
		ValidateArgument.required(objectType, "objectType");
		ValidateArgument.required(parentId, "parentId");
		return this.template.query(SELECT_OBJECT_CHILD_ID_ETAG, (ResultSet rs, int rowNum) -> {
			Long id = rs.getLong(TableConstants.OBJECT_REPLICATION_COL_OBJECT_ID);
			String etag = rs.getString(OBEJCT_REPLICATION_COL_ETAG);
			Long benefactorId = rs.getLong(OBJECT_REPLICATION_COL_BENEFACTOR_ID);
			if (rs.wasNull()) {
				benefactorId = null;
			}
			return new IdAndEtag(id, etag, benefactorId);
		}, objectType.name(), parentId);
	}

	@Override
	public List<Long> getExpiredContainerIds(ViewObjectType objectType, List<Long> containerIds) {
		ValidateArgument.required(containerIds, "entityContainerIds");
		if(containerIds.isEmpty()){
			return new LinkedList<Long>();
		}
		/*
		 * An ID that does not exist, should be treated the same as an expired
		 * ID. Therefore, start off with all of the IDs expired, so the
		 * non-expired IDs can be removed.
		 */
		LinkedHashSet<Long> expiredId = new LinkedHashSet<Long>(containerIds);
		// Query for those that are not expired.
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(OBJECT_TYPE_PARAM_NAME, objectType.name());
		param.addValue(ID_PARAM_NAME, containerIds);
		param.addValue(EXPIRES_PARAM_NAME, System.currentTimeMillis());
		List<Long> nonExpiredIds =  namedTemplate.queryForList(SELECT_NON_EXPIRED_IDS, param, Long.class);
		// remove all that are not expired.
		expiredId.removeAll(nonExpiredIds);
		// return the remain.
		return new LinkedList<Long>(expiredId);
	}

	@Override
	public void setContainerSynchronizationExpiration(ViewObjectType objectType, final List<Long> toSet,
			final long newExpirationDateMS) {
		ValidateArgument.required(objectType, "objectType");
		ValidateArgument.required(toSet, "toSet");
		if(toSet.isEmpty()){
			return;
		}
		template.batchUpdate(BATCH_INSERT_REPLICATION_SYNC_EXP, new BatchPreparedStatementSetter() {
			
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				Long idToSet = toSet.get(i);
				int index = 1;
				ps.setString(index++, objectType.name());
				ps.setLong(index++, idToSet);
				ps.setLong(index++, newExpirationDateMS);
				ps.setLong(index++, newExpirationDateMS);
			}
			
			@Override
			public int getBatchSize() {
				return toSet.size();
			}
		});
		
	}

	@Override
	public List<Long> getRowIds(String sql, Map<String, Object> parameters) {
		ValidateArgument.required(sql, "sql");
		ValidateArgument.required(parameters, "parameters");
		// We use spring to create create the prepared statement
		return namedTemplate.queryForList(sql, new MapSqlParameterSource(parameters), Long.class);
	}

	@Override
	public long getSumOfFileSizes(ViewObjectType objectType, List<Long> rowIds) {
		ValidateArgument.required(rowIds, "rowIds");
		if(rowIds.isEmpty()) {
			return 0L;
		}
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(OBJECT_TYPE_PARAM_NAME, objectType.name());
		params.addValue(ID_PARAM_NAME, rowIds);

		Long sum = namedTemplate.queryForObject(SQL_SUM_FILE_SIZES, params, Long.class);
		
		if(sum == null) {
			sum =  0L;
		}
		
		return sum;
	}

	@Override
	public void streamSynapseStorageStats(ViewObjectType objectType, Callback<SynapseStorageProjectStats> callback) {
		MapSqlParameterSource params = new MapSqlParameterSource(OBJECT_TYPE_PARAM_NAME, objectType.name());
		
		// We use spring to create create the prepared statement
		namedTemplate.query(SQL_SELECT_PROJECTS_BY_SIZE, params, (ResultSet rs) -> {
			SynapseStorageProjectStats stats = new SynapseStorageProjectStats();
			stats.setId(rs.getString(1));
			stats.setProjectName(rs.getString(2));
			stats.setSizeInBytes(rs.getLong(3));
			callback.invoke(stats);
		});
	}

	@Override
	public void populateViewFromSnapshot(IdAndVersion idAndVersion, Iterator<String[]> input, long maxBytesPerBatch) {
		ValidateArgument.required(idAndVersion, "idAndVersion");
		ValidateArgument.required(idAndVersion.getVersion().isPresent(), "idAndVersion.version");
		ValidateArgument.required(input, "input");
		ValidateArgument.required(input.hasNext(), "input is empty");
		// The first row is the header
		String[] headers = input.next();
		String sql = SQLUtils.createInsertViewFromSnapshot(idAndVersion, headers);

		// push the data in batches
		List<Object[]> batch = new LinkedList<>();
		int batchSize = 0;
		while (input.hasNext()) {
			String[] row = input.next();
			long rowSize = SQLUtils.calculateBytes(row);
			if (batchSize + rowSize > maxBytesPerBatch) {
				template.batchUpdate(sql, batch);
				batch.clear();
			}
			batch.add(row);
			batchSize += rowSize;
		}

		if (!batch.isEmpty()) {
			template.batchUpdate(sql, batch);
		}
	}
	
	@Override
	public Set<Long> getOutOfDateRowsForView(IdAndVersion viewId, ViewScopeFilter scopeFilter, long limit) {
		ValidateArgument.required(viewId, "viewId");
		ValidateArgument.required(scopeFilter, "scopeFilter");
		ValidateArgument.required(scopeFilter.getContainerIds(), "scopeFilter.containerIds");
		
		Set<Long> allContainersInScope = scopeFilter.getContainerIds();
		
		if(allContainersInScope.isEmpty()) {
			return Collections.emptySet();
		}
		
		String sql = SQLUtils.getOutOfDateRowsForViewSql(viewId, scopeFilter);
		
		MapSqlParameterSource param = getMapSqlParameterSourceForScopeFilter(scopeFilter, true, null);
		
		param.addValue(P_LIMIT, limit);
		
		List<Long> deltas = namedTemplate.queryForList(sql, param, Long.class);
		return new LinkedHashSet<Long>(deltas);
	}

	@Override
	public void deleteRowsFromViewBatch(IdAndVersion viewId, Long...idsToDelete) {
		ValidateArgument.required(viewId, "viewId");
		ValidateArgument.required(idsToDelete, "batch");
		String sql = SQLUtils.getDeleteRowsFromViewSql(viewId);
		this.template.batchUpdate(sql, new BatchPreparedStatementSetter() {
			
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				ps.setLong(1, idsToDelete[i]);
			}
			
			@Override
			public int getBatchSize() {
				return idsToDelete.length;
			}
		});
	}
	
	@Override
	public void truncateReplicationSyncExpiration() {
		template.update(TRUNCATE_REPLICATION_SYNC_EXPIRATION_TABLE);
	}
	
	@Override
	public void truncateIndex() {
		truncateReplicationSyncExpiration();
		template.update(TRUNCATE_ANNOTATION_REPLICATION_TABLE);
		template.update(TRUNCATE_OBJECT_REPLICATION_TABLE);
	}
	
	private MapSqlParameterSource getMapSqlParameterSourceForScopeFilter(ViewScopeFilter scopeFilter, boolean filterBySubTypes, Set<Long> rowIdsToCopy) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		
		param.addValue(OBJECT_TYPE_PARAM_NAME, scopeFilter.getObjectType().name());
		param.addValue(PARENT_ID_PARAM_NAME, scopeFilter.getContainerIds());
		
		if (rowIdsToCopy != null) {
			param.addValue(ID_PARAM_NAME, rowIdsToCopy);
		}
		
		if (filterBySubTypes) {
			param.addValue(SUBTYPE_PARAM_NAME, scopeFilter.getSubTypes());
		}
		
		return param;
	}

	@Override
	public void refreshViewBenefactors(IdAndVersion viewId, ViewObjectType objectType) {
		ValidateArgument.required(viewId, "viewId");
		ValidateArgument.required(objectType, "objectType");
		String sql = SQLUtils.generateSqlToRefreshViewBenefactors(viewId);
		template.update(sql, objectType.name());
	}
	
}
