package org.sagebionetworks.table.cluster;

import static org.sagebionetworks.repo.model.table.TableConstants.*;

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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.repo.model.table.AnnotationDTO;
import org.sagebionetworks.repo.model.table.AnnotationType;
import org.sagebionetworks.repo.model.table.ColumnChangeDetails;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.EntityDTO;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.repo.model.table.ViewType;
import org.sagebionetworks.table.cluster.SQLUtils.TableType;
import org.sagebionetworks.table.cluster.utils.ColumnConstants;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.model.Grouping;
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
		 * By default the MySQL driver will read all query results into memory
		 * which can cause memory problems for large query results. (see:
		 * <a hreft="http://dev.mysql.com/doc/connector-j/en/connector-j-reference-implementation-notes.html"/>) 
		 * According to the MySQL driver docs the only way to get
		 * the driver to change this default behavior is to create a statement
		 * with TYPE_FORWARD_ONLY & CONCUR_READ_ONLY and then set statement
		 * fetch size to Integer.MIN_VALUE. However, JdbcTemplate will not
		 * set a fetch size less than zero. Therefore, we must override the
		 * JdbcTemplate to force the fetch size of Integer.MIN_VALUE. See:
		 * PLFM-3429
		 */
		this.template = new JdbcTemplate(dataSource) {
			@Override
			protected void applyStatementSettings(Statement stmt) throws SQLException {
				super.applyStatementSettings(stmt);
				if (getFetchSize() == Integer.MIN_VALUE) {
					stmt.setFetchSize(getFetchSize());
				}
			}
		};
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
	public boolean deleteTable(String tableId) {
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
	public void createOrUpdateOrDeleteRows(final Grouping grouping) {
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
					String deleteSql = SQLUtils.buildDeleteSQL(grouping.getTableId());
					SqlParameterSource batchDeleteBinding = SQLUtils
							.bindParameterForDelete(grouping.getRows());
					namedTemplate.update(deleteSql, batchDeleteBinding);
				}else{
					// this is a create or update
					String createOrUpdateSql = SQLUtils.buildCreateOrUpdateRowSQL(groupingColumns,
							grouping.getTableId());
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
	public Long getRowCountForTable(String tableId) {
		String sql = SQLUtils.getCountSQL(tableId);
		try {
			return template.queryForObject(sql,new SingleColumnRowMapper<Long>());
		} catch (BadSqlGrammarException e) {
			// Spring throws this when the table does not
			return null;
		}
	}

	@Override
	public Long getMaxCurrentCompleteVersionForTable(String tableId) {
		String sql = SQLUtils.getStatusMaxVersionSQL(tableId);
		try {
			return template.queryForObject(sql, new SingleColumnRowMapper<Long>());
		} catch (Exception e) {
			// Spring throws this when the table is empty
			return -1L;
		}
	}

	@Override
	public void setMaxCurrentCompleteVersionForTable(String tableId, Long version) {
		String createOrUpdateStatusSql = SQLUtils.buildCreateOrUpdateStatusSQL(tableId);
		template.update(createOrUpdateStatusSql, version, version);
	}
	
	@Override
	public void setCurrentSchemaMD5Hex(String tableId, String schemaMD5Hex) {
		String createOrUpdateStatusSql = SQLUtils.buildCreateOrUpdateStatusHashSQL(tableId);
		template.update(createOrUpdateStatusSql, schemaMD5Hex, schemaMD5Hex);
	}
	
	@Override
	public void setIndexVersionAndSchemaMD5Hex(String tableId, Long viewCRC,
			String schemaMD5Hex) {
		String createOrUpdateStatusSql = SQLUtils.buildCreateOrUpdateStatusVersionAndHashSQL(tableId);
		template.update(createOrUpdateStatusSql, viewCRC, schemaMD5Hex, viewCRC, schemaMD5Hex);
	}

	@Override
	public String getCurrentSchemaMD5Hex(String tableId) {
		String sql = SQLUtils.getSchemaHashSQL(tableId);
		try {
			return template.queryForObject(sql, new SingleColumnRowMapper<String>());
		} catch (Exception e) {
			// Spring throws this when the table is empty
			return EMPTY_SCHEMA_MD5;
		}
	}

	@Override
	public void deleteSecondaryTables(String tableId) {
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
	public void createSecondaryTables(String tableId) {
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
	public RowSet query(ProgressCallback<Void> callback, final SqlQuery query) {
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
	public boolean queryAsStream(final ProgressCallback<Void> callback, final SqlQuery query, final RowHandler handler) {
		ValidateArgument.required(query, "Query");
		final ColumnTypeInfo[] infoArray = SQLTranslatorUtils.getColumnTypeInfoArray(query.getSelectColumns());
		// We use spring to create create the prepared statement
		NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(this.template);
		namedTemplate.query(query.getOutputSQL(), new MapSqlParameterSource(query.getParameters()), new RowCallbackHandler() {
			@Override
			public void processRow(ResultSet rs) throws SQLException {
				// refresh the lock.
				if(callback != null){
					callback.progressMade(null);
				}
				Row row = SQLTranslatorUtils.readRow(rs, query.includesRowIdAndVersion(), infoArray);
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
	public void applyFileHandleIdsToTable(final String tableId,
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
			final Set<Long> fileHandleIds, final String tableId) {
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
	public boolean doesIndexStateMatch(String tableId, long versionNumber, String schemaMD5Hex) {
		long indexVersion = getMaxCurrentCompleteVersionForTable(tableId);
		if(indexVersion != versionNumber){
			return false;
		}
		String indexMD5Hex = getCurrentSchemaMD5Hex(tableId);
		return indexMD5Hex.equals(schemaMD5Hex);
	}

	@Override
	public Set<Long> getDistinctLongValues(String tableId, String columnIds) {
		String sql = SQLUtils.createSQLGetDistinctValues(tableId, columnIds);
		List<Long> results = template.queryForList(sql, Long.class);
		return new HashSet<Long>(results);
	}

	@Override
	public void createTableIfDoesNotExist(String tableId) {
		String sql = SQLUtils.createTableIfDoesNotExistSQL(tableId);
		template.update(sql);
	}

	@Override
	public boolean alterTableAsNeeded(String tableId, List<ColumnChangeDetails> changes, boolean alterTemp) {
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
	public void truncateTable(String tableId) {
		String sql = SQLUtils.createTruncateSql(tableId);
		template.update(sql);
	}

	@Override
	public List<DatabaseColumnInfo> getDatabaseInfo(String tableId) {
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
			String tableId) {
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
	public void provideIndexName(List<DatabaseColumnInfo> list, String tableId) {
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
			String tableId, int maxNumberOfIndex) {
		String alterSql = SQLUtils.createOptimizedAlterIndices(list, tableId, maxNumberOfIndex);
		if(alterSql == null){
			// No changes are needed.
			return;
		}
		template.update(alterSql);
	}

	@Override
	public void createTemporaryTable(String tableId) {
		String sql = SQLUtils.createTempTableSql(tableId);
		template.update(sql);
	}

	@Override
	public void copyAllDataToTemporaryTable(String tableId) {
		String sql = SQLUtils.copyTableToTempSql(tableId);
		template.update(sql);
	}

	@Override
	public void deleteTemporaryTable(String tableId) {
		String sql = SQLUtils.deleteTempTableSql(tableId);
		template.update(sql);
	}

	@Override
	public long getTempTableCount(String tableId) {
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
	}

	@Override
	public void deleteEntityData(final ProgressCallback<Void> progressCallback, List<Long> entityIds) {
		final List<Long> sorted = new ArrayList<Long>(entityIds);
		// sort to prevent deadlock.
		Collections.sort(sorted);
		// Batch delete.
		template.batchUpdate(TableConstants.ENTITY_REPLICATION_DELETE_ALL, new BatchPreparedStatementSetter(){

			@Override
			public void setValues(PreparedStatement ps, int i)
					throws SQLException {
				progressCallback.progressMade(null);
				ps.setLong(1, sorted.get(i));
			}

			@Override
			public int getBatchSize() {
				return sorted.size();
			}});
		
	}

	@Override
	public void addEntityData(final ProgressCallback<Void> progressCallback, List<EntityDTO> entityDTOs) {
		final List<EntityDTO> sorted = new ArrayList<EntityDTO>(entityDTOs);
		Collections.sort(sorted);
		// batch update the entity table
		template.batchUpdate(TableConstants.ENTITY_REPLICATION_INSERT, new BatchPreparedStatementSetter(){

			@Override
			public void setValues(PreparedStatement ps, int i)
					throws SQLException {
				// progress for each row.
				progressCallback.progressMade(null);
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
				// progress for each row.
				progressCallback.progressMade(null);
				AnnotationDTO dto = annotations.get(i);
				int parameterIndex = 1;
				ps.setLong(parameterIndex++, dto.getEntityId());
				ps.setString(parameterIndex++, dto.getKey());
				ps.setString(parameterIndex++, dto.getType().name());
				ps.setString(parameterIndex++, dto.getValue());
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
				dto.setValue(rs.getString(ANNOTATION_REPLICATION_COL_VALUE));
				return dto;
			}}, entityId);
		if(!annotations.isEmpty()){
			dto.setAnnotations(annotations);
		}
		return dto;
	}

	@Override
	public long calculateCRC32ofEntityReplicationScope(ViewType viewType,
			Set<Long> allContainersInScope) {
		ValidateArgument.required(viewType, "viewType");
		ValidateArgument.required(allContainersInScope, "allContainersInScope");
		if(allContainersInScope.isEmpty()){
			return -1L;
		}
		NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(this.template);
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(TYPE_PARAMETER_NAME, viewType.name());
		param.addValue(PARENT_ID_PARAMETER_NAME, allContainersInScope);
		Long crc32 = namedTemplate.queryForObject(SQL_ENTITY_REPLICATION_CRC_32, param, Long.class);
		if(crc32 == null){
			return -1L;
		}
		return crc32;
	}
	
	@Override
	public long calculateCRC32ofTableView(String viewId, String etagColumnId){
		String sql = SQLUtils.buildTableViewCRC32Sql(viewId, etagColumnId);
		Long result = this.template.queryForObject(sql, Long.class);
		if(result == null){
			return -1L;
		}
		return result;
	}

	@Override
	public void copyEntityReplicationToTable(String viewId, ViewType viewType,
			Set<Long> allContainersInScope, List<ColumnModel> currentSchema) {
		ValidateArgument.required(viewType, "viewType");
		ValidateArgument.required(allContainersInScope, "allContainersInScope");
		if(allContainersInScope.isEmpty()){
			// nothing to do if the scope is empty.
			return;
		}
		NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(this.template);
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(TYPE_PARAMETER_NAME, viewType.name());
		param.addValue(PARENT_ID_PARAMETER_NAME, allContainersInScope);
		String sql = SQLUtils.createSelectInsertFromEntityReplication(viewId, currentSchema);
		namedTemplate.update(sql, param);
	}

	@Override
	public List<ColumnModel> getPossibleColumnModelsForContainers(
			Set<Long> containerIds, Long limit, Long offset) {
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
		return namedTemplate.query(SELECT_DISTINCT_ANNOTATION_COLUMNS, param, new RowMapper<ColumnModel>() {

			@Override
			public ColumnModel mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				String name = rs.getString(ANNOTATION_REPLICATION_COL_KEY);
				ColumnType type = AnnotationType.valueOf(rs.getString(ANNOTATION_REPLICATION_COL_TYPE)).getColumnType();
				ColumnModel cm = new ColumnModel();
				cm.setName(name);
				cm.setColumnType(type);
				if(ColumnType.STRING.equals(type)){
					long maxLength = rs.getLong(3);
					if(maxLength < 1){
						maxLength = ColumnConstants.DEFAULT_STRING_SIZE;
					}
					cm.setMaximumSize(maxLength);
				}
				return cm;
			}
		});
	}

}
