package org.sagebionetworks.table.cluster;

import static org.sagebionetworks.repo.model.table.TableConstants.ANNOTATION_REPLICATION_COL_ENTITY_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ANNOTATION_REPLICATION_COL_KEY;
import static org.sagebionetworks.repo.model.table.TableConstants.ANNOTATION_REPLICATION_COL_STRING_VALUE;
import static org.sagebionetworks.repo.model.table.TableConstants.ANNOTATION_REPLICATION_COL_TYPE;
import static org.sagebionetworks.repo.model.table.TableConstants.BATCH_INSERT_REPLICATION_SYNC_EXP;
import static org.sagebionetworks.repo.model.table.TableConstants.CRC_ALIAS;
import static org.sagebionetworks.repo.model.table.TableConstants.ENTITY_REPLICATION_COL_BENEFACTOR_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ENTITY_REPLICATION_COL_CRATED_BY;
import static org.sagebionetworks.repo.model.table.TableConstants.ENTITY_REPLICATION_COL_CRATED_ON;
import static org.sagebionetworks.repo.model.table.TableConstants.ENTITY_REPLICATION_COL_ETAG;
import static org.sagebionetworks.repo.model.table.TableConstants.ENTITY_REPLICATION_COL_FILE_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ENTITY_REPLICATION_COL_FILE_SIZE_BYTES;
import static org.sagebionetworks.repo.model.table.TableConstants.ENTITY_REPLICATION_COL_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ENTITY_REPLICATION_COL_IN_SYNAPSE_STORAGE;
import static org.sagebionetworks.repo.model.table.TableConstants.ENTITY_REPLICATION_COL_MODIFIED_BY;
import static org.sagebionetworks.repo.model.table.TableConstants.ENTITY_REPLICATION_COL_MODIFIED_ON;
import static org.sagebionetworks.repo.model.table.TableConstants.ENTITY_REPLICATION_COL_NAME;
import static org.sagebionetworks.repo.model.table.TableConstants.ENTITY_REPLICATION_COL_PARENT_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ENTITY_REPLICATION_COL_PROJECT_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ENTITY_REPLICATION_COL_TYPE;
import static org.sagebionetworks.repo.model.table.TableConstants.ENTITY_REPLICATION_COL_VERSION;
import static org.sagebionetworks.repo.model.table.TableConstants.ENTITY_REPLICATION_TABLE;
import static org.sagebionetworks.repo.model.table.TableConstants.EXPIRES_PARAM;
import static org.sagebionetworks.repo.model.table.TableConstants.ID_PARAMETER_NAME;
import static org.sagebionetworks.repo.model.table.TableConstants.PARENT_ID_PARAMETER_NAME;
import static org.sagebionetworks.repo.model.table.TableConstants.P_LIMIT;
import static org.sagebionetworks.repo.model.table.TableConstants.P_OFFSET;
import static org.sagebionetworks.repo.model.table.TableConstants.SELECT_ENTITY_CHILD_CRC;
import static org.sagebionetworks.repo.model.table.TableConstants.SELECT_ENTITY_CHILD_ID_ETAG;
import static org.sagebionetworks.repo.model.table.TableConstants.SELECT_NON_EXPIRED_IDS;
import static org.sagebionetworks.repo.model.table.TableConstants.TRUNCATE_REPLICATION_SYNC_EXPIRATION_TABLE;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.IdAndEtag;
import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.report.SynapseStorageProjectStats;
import org.sagebionetworks.repo.model.table.AnnotationDTO;
import org.sagebionetworks.repo.model.table.AnnotationType;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.EntityDTO;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.table.cluster.SQLUtils.TableType;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.model.Grouping;
import org.sagebionetworks.util.Callback;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.dao.DataAccessException;
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


	private static final String SQL_SUM_FILE_SIZES = "SELECT SUM(" + ENTITY_REPLICATION_COL_FILE_SIZE_BYTES + ") FROM "
			+ ENTITY_REPLICATION_TABLE + " WHERE " + ENTITY_REPLICATION_COL_ID + " IN (:rowIds)";

	public static final String SQL_SELECT_PROJECTS_BY_SIZE =
			"SELECT t1."+ENTITY_REPLICATION_COL_PROJECT_ID + ", t2." + ENTITY_REPLICATION_COL_NAME + ", t1.PROJECT_SIZE_BYTES "
		+ " FROM (SELECT " + ENTITY_REPLICATION_COL_PROJECT_ID + ", "
					+ " SUM(" + ENTITY_REPLICATION_COL_FILE_SIZE_BYTES + ") AS PROJECT_SIZE_BYTES"
					+ " FROM " + ENTITY_REPLICATION_TABLE + " WHERE " + ENTITY_REPLICATION_COL_IN_SYNAPSE_STORAGE+" = 1"
					+ " GROUP BY " + ENTITY_REPLICATION_COL_PROJECT_ID + ") t1," + ENTITY_REPLICATION_TABLE + " t2"
					+ " WHERE t1." + ENTITY_REPLICATION_COL_PROJECT_ID + " = t2." + ENTITY_REPLICATION_COL_ID
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

	private final DataSourceTransactionManager transactionManager;
	private final TransactionTemplate writeTransactionTemplate;
	private final TransactionTemplate readTransactionTemplate;
	private final JdbcTemplate template;

	/**
	 * The IoC constructor.
	 * 
	 * @param template
	 * @param transactionManager
	 */
	public TableIndexDAOImpl(DataSource dataSource) {
		super();
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
	public boolean deleteTable(IdAndVersion tableId) {
		String dropTableDML = SQLUtils.dropTableSQL(tableId, SQLUtils.TableType.INDEX);
		try {
			template.update(dropTableDML);
			return true;
		} catch (BadSqlGrammarException e) {
			// This is thrown when the table does not exist
			return false;
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
	public void deleteSecondaryTables(IdAndVersion tableId) {
		for(TableType type: SQLUtils.SECONDARY_TYPES){
			String dropStatusTableDML = SQLUtils.dropTableSQL(tableId, type);
			try {
				template.update(dropStatusTableDML);
			} catch (BadSqlGrammarException e) {
				// This is thrown when the table does not exist
			}
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
		NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(this.template);
		return namedTemplate.queryForObject(sql, new MapSqlParameterSource(parameters), Long.class);
	}
	
	@Override
	public boolean queryAsStream(final ProgressCallback callback, final SqlQuery query, final RowHandler handler) {
		ValidateArgument.required(query, "Query");
		final ColumnTypeInfo[] infoArray = SQLTranslatorUtils.getColumnTypeInfoArray(query.getSelectColumns());
		// We use spring to create create the prepared statement
		NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(this.template);
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
					info.setMaxSize(MySqlColumnType.parseSize(typeString));
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
			return 0l;
		}
	}
	
	@Override
	public void createEntityReplicationTablesIfDoesNotExist(){
		template.update(TableConstants.ENTITY_REPLICATION_TABLE_CREATE);
		template.update(TableConstants.ANNOTATION_REPLICATION_TABLE_CREATE);
		template.update(TableConstants.REPLICATION_SYNCH_EXPIRATION_TABLE_CREATE);
	}

	@Override
	public void deleteEntityData(final ProgressCallback progressCallback, List<Long> entityIds) {
		final List<Long> sorted = new ArrayList<Long>(entityIds);
		// sort to prevent deadlock.
		Collections.sort(sorted);
		// Batch delete.
		template.batchUpdate(TableConstants.ENTITY_REPLICATION_DELETE_ALL, new BatchPreparedStatementSetter(){

			@Override
			public void setValues(PreparedStatement ps, int i)
					throws SQLException {
				ps.setLong(1, sorted.get(i));
			}

			@Override
			public int getBatchSize() {
				return sorted.size();
			}});
		
	}

	@Override
	public void addEntityData(final ProgressCallback progressCallback, List<EntityDTO> entityDTOs) {
		final List<EntityDTO> sorted = new ArrayList<EntityDTO>(entityDTOs);
		Collections.sort(sorted);
		// batch update the entity table
		template.batchUpdate(TableConstants.ENTITY_REPLICATION_INSERT, new BatchPreparedStatementSetter(){

			@Override
			public void setValues(PreparedStatement ps, int i)
					throws SQLException {
				EntityDTO dto = sorted.get(i);
				int parameterIndex = 1;
				ps.setLong(parameterIndex++, dto.getId());
				ps.setLong(parameterIndex++, dto.getCurrentVersion());
				ps.setLong(parameterIndex++, dto.getCreatedBy());
				ps.setLong(parameterIndex++, dto.getCreatedOn().getTime());
				ps.setString(parameterIndex++, dto.getEtag());
				ps.setString(parameterIndex++, dto.getName());
				ps.setString(parameterIndex++, dto.getType().name());
				if(dto.getParentId() != null){
					ps.setLong(parameterIndex++, dto.getParentId());
				}else{
					ps.setNull(parameterIndex++, java.sql.Types.BIGINT);
				}
				if(dto.getBenefactorId() != null){
					ps.setLong(parameterIndex++, dto.getBenefactorId());
				}else{
					ps.setNull(parameterIndex++, java.sql.Types.BIGINT);
				}
				if(dto.getProjectId() != null){
					ps.setLong(parameterIndex++, dto.getProjectId());
				}else{
					ps.setNull(parameterIndex++, java.sql.Types.BIGINT);
				}
				ps.setLong(parameterIndex++, dto.getModifiedBy());
				ps.setLong(parameterIndex++, dto.getModifiedOn().getTime());
				if(dto.getFileHandleId() != null){
					ps.setLong(parameterIndex++, dto.getFileHandleId());
				}else{
					ps.setNull(parameterIndex++, java.sql.Types.BIGINT);
				}
				if(dto.getFileSizeBytes() != null) {
					ps.setLong(parameterIndex++, dto.getFileSizeBytes());
				}else{
					ps.setNull(parameterIndex++, java.sql.Types.BIGINT);
				}
				if(dto.getIsInSynapseStorage() != null) {
					ps.setBoolean(parameterIndex++, dto.getIsInSynapseStorage());
				}else{
					ps.setNull(parameterIndex++, java.sql.Types.BOOLEAN);
				}
			}

			@Override
			public int getBatchSize() {
				return sorted.size();
			}});
		// map the entities with annotations
		final List<AnnotationDTO> annotations = new ArrayList<AnnotationDTO>();
		for(int i=0; i<sorted.size(); i++){
			EntityDTO dto = sorted.get(i);
			if(dto.getAnnotations() != null && !dto.getAnnotations().isEmpty()){
				// this index has annotations.
				annotations.addAll(dto.getAnnotations());
			}
		}
		// update the annotations
		template.batchUpdate(TableConstants.ANNOTATION_REPLICATION_INSERT, new BatchPreparedStatementSetter(){

			@Override
			public void setValues(PreparedStatement ps, int i)
					throws SQLException {
				AnnotationDTO dto = annotations.get(i);
				SQLUtils.writeAnnotationDtoToPreparedStatement(ps, dto);
			}

			@Override
			public int getBatchSize() {
				return annotations.size();
			}});
		
	}

	@Override
	public EntityDTO getEntityData(Long entityId) {
		// query for the template.
		EntityDTO dto;
		try {
			dto = template.queryForObject(TableConstants.ENTITY_REPLICATION_GET, new RowMapper<EntityDTO>(){

				@Override
				public EntityDTO mapRow(ResultSet rs, int rowNum)
						throws SQLException {
					EntityDTO dto = new EntityDTO();
					dto.setId(rs.getLong(ENTITY_REPLICATION_COL_ID));
					dto.setCurrentVersion(rs.getLong(ENTITY_REPLICATION_COL_VERSION));
					dto.setCreatedBy(rs.getLong(ENTITY_REPLICATION_COL_CRATED_BY));
					dto.setCreatedOn(new Date(rs.getLong(ENTITY_REPLICATION_COL_CRATED_ON)));
					dto.setEtag(rs.getString(ENTITY_REPLICATION_COL_ETAG));
					dto.setName(rs.getString(ENTITY_REPLICATION_COL_NAME));
					dto.setType(EntityType.valueOf(rs.getString(ENTITY_REPLICATION_COL_TYPE)));
					dto.setParentId(rs.getLong(ENTITY_REPLICATION_COL_PARENT_ID));
					if(rs.wasNull()){
						dto.setParentId(null);
					}
					dto.setBenefactorId(rs.getLong(ENTITY_REPLICATION_COL_BENEFACTOR_ID));
					if(rs.wasNull()){
						dto.setBenefactorId(null);
					}
					dto.setProjectId(rs.getLong(ENTITY_REPLICATION_COL_PROJECT_ID));
					if(rs.wasNull()){
						dto.setProjectId(null);
					}
					dto.setModifiedBy(rs.getLong(ENTITY_REPLICATION_COL_MODIFIED_BY));
					dto.setModifiedOn(new Date(rs.getLong(ENTITY_REPLICATION_COL_MODIFIED_ON)));
					dto.setFileHandleId(rs.getLong(ENTITY_REPLICATION_COL_FILE_ID));
					if(rs.wasNull()){
						dto.setFileHandleId(null);
					}
					dto.setFileSizeBytes(rs.getLong(ENTITY_REPLICATION_COL_FILE_SIZE_BYTES));
					if(rs.wasNull()){
						dto.setFileSizeBytes(null);
					}
					dto.setIsInSynapseStorage(rs.getBoolean(ENTITY_REPLICATION_COL_IN_SYNAPSE_STORAGE));
					if(rs.wasNull()) {
						dto.setIsInSynapseStorage(null);
					}

					return dto;
				}}, entityId);
		} catch (DataAccessException e) {
			return null;
		}
		// get the annotations.
		List<AnnotationDTO> annotations = template.query(TableConstants.ANNOTATION_REPLICATION_GET, new RowMapper<AnnotationDTO>(){

			@Override
			public AnnotationDTO mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				AnnotationDTO dto = new AnnotationDTO();
				dto.setEntityId(rs.getLong(ANNOTATION_REPLICATION_COL_ENTITY_ID));
				dto.setKey(rs.getString(ANNOTATION_REPLICATION_COL_KEY));
				dto.setType(AnnotationType.valueOf(rs.getString(ANNOTATION_REPLICATION_COL_TYPE)));
				dto.setValue(rs.getString(ANNOTATION_REPLICATION_COL_STRING_VALUE));
				return dto;
			}}, entityId);
		if(!annotations.isEmpty()){
			dto.setAnnotations(annotations);
		}
		return dto;
	}

	@Override
	public long calculateCRC32ofEntityReplicationScope(Long viewTypeMask,
			Set<Long> allContainersInScope) {
		ValidateArgument.required(viewTypeMask, "viewTypeMask");
		ValidateArgument.required(allContainersInScope, "allContainersInScope");
		if(allContainersInScope.isEmpty()){
			return -1L;
		}
		NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(this.template);
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(PARENT_ID_PARAMETER_NAME, allContainersInScope);
		String sql = SQLUtils.getCalculateCRC32Sql(viewTypeMask);
		Long crc32 = namedTemplate.queryForObject(sql, param, Long.class);
		if(crc32 == null){
			return -1L;
		}
		return crc32;
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

	@Override
	public void copyEntityReplicationToTable(Long viewId, Long viewTypeMask,
			Set<Long> allContainersInScope, List<ColumnModel> currentSchema) {
		ValidateArgument.required(viewTypeMask, "viewTypeMask");
		ValidateArgument.required(allContainersInScope, "allContainersInScope");
		if(allContainersInScope.isEmpty()){
			// nothing to do if the scope is empty.
			return;
		}
		NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(this.template);
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(PARENT_ID_PARAMETER_NAME, allContainersInScope);
		String sql = SQLUtils.createSelectInsertFromEntityReplication(viewId, viewTypeMask, currentSchema);
		namedTemplate.update(sql, param);
	}

	@Override
	public List<ColumnModel> getPossibleColumnModelsForContainers(
			Set<Long> containerIds, Long viewTypeMask, Long limit, Long offset) {
		ValidateArgument.required(containerIds, "containerIds");
		ValidateArgument.required(limit, "limit");
		ValidateArgument.required(offset, "offset");
		if(containerIds.isEmpty()){
			return new LinkedList<>();
		}
		NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(this.template);
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(PARENT_ID_PARAMETER_NAME, containerIds);
		param.addValue(P_LIMIT, limit);
		param.addValue(P_OFFSET, offset);
		String sql = SQLUtils.getDistinctAnnotationColumnsSql(viewTypeMask);
		List<ColumnAggregation> results = namedTemplate.query(sql, param, new RowMapper<ColumnAggregation>() {

			@Override
			public ColumnAggregation mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				ColumnAggregation aggregation = new ColumnAggregation();
				aggregation.setColumnName(rs.getString(ANNOTATION_REPLICATION_COL_KEY));
				aggregation.setColumnTypeConcat(rs.getString(2));
				aggregation.setMaxSize(rs.getLong(3));
				return aggregation;
			}
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
				model.setColumnType(type);
				if(ColumnType.STRING == type) {
					model.setMaximumSize(aggregation.getMaxSize());
				}
				results.add(model);
			}
		}
		return results;
	}
	
	@Override
	public Map<Long, Long> getSumOfChildCRCsForEachParent(List<Long> parentIds) {
		ValidateArgument.required(parentIds, "parentIds");
		final Map<Long, Long> results = new HashMap<>();
		if(parentIds.isEmpty()){
			return results;
		}
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(PARENT_ID_PARAMETER_NAME, parentIds);
		NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(this.template);
		namedTemplate.query(SELECT_ENTITY_CHILD_CRC, param, new RowCallbackHandler() {
			
			@Override
			public void processRow(ResultSet rs) throws SQLException {
				Long parentId = rs.getLong(ENTITY_REPLICATION_COL_PARENT_ID);
				Long crc = rs.getLong(CRC_ALIAS);
				results.put(parentId, crc);
			}
		});
		return results;
	}

	@Override
	public List<IdAndEtag> getEntityChildren(Long parentId) {
		ValidateArgument.required(parentId, "parentId");
		return this.template.query(SELECT_ENTITY_CHILD_ID_ETAG, new RowMapper<IdAndEtag>(){

			@Override
			public IdAndEtag mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				Long id = rs.getLong(TableConstants.ENTITY_REPLICATION_COL_ID);
				String etag = rs.getString(ENTITY_REPLICATION_COL_ETAG);
				Long benefactorId = rs.getLong(ENTITY_REPLICATION_COL_BENEFACTOR_ID);
				if(rs.wasNull()) {
					benefactorId = null;
				}
				return new IdAndEtag(id, etag, benefactorId);
			}}, parentId);
	}

	@Override
	public List<Long> getExpiredContainerIds(List<Long> entityContainerIds) {
		ValidateArgument.required(entityContainerIds, "entityContainerIds");
		if(entityContainerIds.isEmpty()){
			return new LinkedList<Long>();
		}
		/*
		 * An ID that does not exist, should be treated the same as an expired
		 * ID. Therefore, start off with all of the IDs expired, so the
		 * non-expired IDs can be removed.
		 */
		LinkedHashSet<Long> expiredId = new LinkedHashSet<Long>(entityContainerIds);
		// Query for those that are not expired.
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID_PARAMETER_NAME, entityContainerIds);
		param.addValue(EXPIRES_PARAM, System.currentTimeMillis());
		NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(this.template);
		List<Long> nonExpiredIds =  namedTemplate.queryForList(SELECT_NON_EXPIRED_IDS, param, Long.class);
		// remove all that are not expired.
		expiredId.removeAll(nonExpiredIds);
		// return the remain.
		return new LinkedList<Long>(expiredId);
	}

	@Override
	public void setContainerSynchronizationExpiration(final List<Long> toSet,
			final long newExpirationDateMS) {
		ValidateArgument.required(toSet, "toSet");
		if(toSet.isEmpty()){
			return;
		}
		template.batchUpdate(BATCH_INSERT_REPLICATION_SYNC_EXP, new BatchPreparedStatementSetter() {
			
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				Long idToSet = toSet.get(i);
				int index = 1;
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
	public void truncateReplicationSyncExpiration() {
		template.update(TRUNCATE_REPLICATION_SYNC_EXPIRATION_TABLE);
	}

	@Override
	public List<Long> getRowIds(String sql, Map<String, Object> parameters) {
		ValidateArgument.required(sql, "sql");
		ValidateArgument.required(parameters, "parameters");
		// We use spring to create create the prepared statement
		NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(this.template);
		return namedTemplate.queryForList(sql, new MapSqlParameterSource(parameters), Long.class);
	}

	@Override
	public long getSumOfFileSizes(List<Long> rowIds) {
		ValidateArgument.required(rowIds, "rowIds");
		if(rowIds.isEmpty()) {
			return 0L;
		}
		NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(this.template);
		Long sum = namedTemplate.queryForObject(SQL_SUM_FILE_SIZES, new MapSqlParameterSource("rowIds", rowIds), Long.class);
		if(sum == null) {
			sum =  0L;
		}
		return sum;
	}

	@Override
	public void streamSynapseStorageStats(Callback<SynapseStorageProjectStats> callback) {
		// We use spring to create create the prepared statement
		NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(this.template);
		namedTemplate.query(SQL_SELECT_PROJECTS_BY_SIZE, rs -> {
			SynapseStorageProjectStats stats = new SynapseStorageProjectStats();
			stats.setId(rs.getString(1));
			stats.setProjectName(rs.getString(2));
			stats.setSizeInBytes(rs.getLong(3));
			callback.invoke(stats);
		});
	}
}
