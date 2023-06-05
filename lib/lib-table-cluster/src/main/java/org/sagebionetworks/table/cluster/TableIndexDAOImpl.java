package org.sagebionetworks.table.cluster;

import static org.sagebionetworks.repo.model.table.TableConstants.ANNOTATION_KEYS_PARAM_NAME;
import static org.sagebionetworks.repo.model.table.TableConstants.ANNOTATION_REPLICATION_COL_IS_DERIVED;
import static org.sagebionetworks.repo.model.table.TableConstants.ANNOTATION_REPLICATION_COL_KEY;
import static org.sagebionetworks.repo.model.table.TableConstants.ANNOTATION_REPLICATION_COL_OBJECT_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ANNOTATION_REPLICATION_COL_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.table.TableConstants.ANNOTATION_REPLICATION_COL_OBJECT_VERSION;
import static org.sagebionetworks.repo.model.table.TableConstants.ANNOTATION_REPLICATION_COL_STRING_LIST_VALUE;
import static org.sagebionetworks.repo.model.table.TableConstants.ANNOTATION_REPLICATION_COL_TYPE;
import static org.sagebionetworks.repo.model.table.TableConstants.ANNOTATION_REPLICATION_TABLE;
import static org.sagebionetworks.repo.model.table.TableConstants.BATCH_INSERT_REPLICATION_SYNC_EXP;
import static org.sagebionetworks.repo.model.table.TableConstants.CRC_ALIAS;
import static org.sagebionetworks.repo.model.table.TableConstants.EXPIRES_PARAM_NAME;
import static org.sagebionetworks.repo.model.table.TableConstants.ID_PARAM_NAME;
import static org.sagebionetworks.repo.model.table.TableConstants.OBEJCT_REPLICATION_COL_ETAG;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_BENEFACTOR_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_CREATED_BY;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_CREATED_ON;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_CUR_VERSION;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_DESCRIPTION;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_FILE_BUCKET;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_FILE_CONCRETE_TYPE;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_FILE_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_FILE_KEY;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_FILE_MD5;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_FILE_SIZE_BYTES;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_IN_SYNAPSE_STORAGE;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_ITEM_COUNT;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_MODIFIED_BY;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_MODIFIED_ON;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_NAME;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_OBJECT_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_OBJECT_VERSION;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_PARENT_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_PROJECT_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_SUBTYPE;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_TABLE;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_TYPE_PARAM_NAME;
import static org.sagebionetworks.repo.model.table.TableConstants.PARENT_ID_PARAM_NAME;
import static org.sagebionetworks.repo.model.table.TableConstants.P_LIMIT;
import static org.sagebionetworks.repo.model.table.TableConstants.P_OFFSET;
import static org.sagebionetworks.repo.model.table.TableConstants.SELECT_NON_EXPIRED_IDS;
import static org.sagebionetworks.repo.model.table.TableConstants.SELECT_OBJECT_CHILD_CRC;
import static org.sagebionetworks.repo.model.table.TableConstants.SELECT_OBJECT_CHILD_ID_ETAG;
import static org.sagebionetworks.repo.model.table.TableConstants.TRUNCATE_ANNOTATION_REPLICATION_TABLE;
import static org.sagebionetworks.repo.model.table.TableConstants.TRUNCATE_OBJECT_REPLICATION_TABLE;
import static org.sagebionetworks.repo.model.table.TableConstants.TRUNCATE_REPLICATION_SYNC_EXPIRATION_TABLE;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
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
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.apache.commons.lang3.RandomStringUtils;
import org.json.JSONArray;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.IdAndChecksum;
import org.sagebionetworks.repo.model.IdAndEtag;
import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.report.SynapseStorageProjectStats;
import org.sagebionetworks.repo.model.table.AnnotationType;
import org.sagebionetworks.repo.model.table.ColumnConstants;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.ObjectAnnotationDTO;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.repo.model.table.ObjectField;
import org.sagebionetworks.repo.model.table.ReplicationType;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SubType;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.table.cluster.SQLUtils.TableIndexType;
import org.sagebionetworks.table.cluster.description.IndexDescription;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldModelResolver;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldModelResolverFactory;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldTypeMapper;
import org.sagebionetworks.table.cluster.search.RowSearchContent;
import org.sagebionetworks.table.cluster.search.TableRowData;
import org.sagebionetworks.table.cluster.search.TypedCellValue;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.cluster.view.filter.ViewFilter;
import org.sagebionetworks.table.model.Grouping;
import org.sagebionetworks.table.query.util.ColumnTypeListMappings;
import org.sagebionetworks.util.Callback;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.util.csv.CSVWriterStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
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
import org.springframework.stereotype.Repository;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Note: This dao is created as a beans to support profiling calls to the dao. See: PLFM-5984.
 */
@Repository
public class TableIndexDAOImpl implements TableIndexDAO {
	
	private static String OBJECT_REPLICATION_TABLE_CREATE = SQLUtils.loadSQLFromClasspath("schema/ObjectReplication.sql");
	private static String ANNOTATION_REPLICATION_TABLE_CREATE = SQLUtils.loadSQLFromClasspath("schema/AnnotationReplication.sql");
	private static String REPLICATION_SYNCH_EXPIRATION_TABLE_CREATE = SQLUtils.loadSQLFromClasspath("schema/ReplicationSynchExpiration.sql");
	private static String GET_ID_AND_CHECKSUMS_SQL_TEMPLATE = SQLUtils.loadSQLFromClasspath("sql/GetIdAndChecksumsTemplate.sql");
	
	public static RowMapper<ObjectDataDTO> OBJECT_DATA_ROW_MAPPER = (ResultSet rs, int rowNum) -> {
		ObjectDataDTO dto = new ObjectDataDTO();
		dto.setId(rs.getLong(OBJECT_REPLICATION_COL_OBJECT_ID));
		dto.setVersion(rs.getLong(OBJECT_REPLICATION_COL_OBJECT_VERSION));
		dto.setCurrentVersion(rs.getLong(OBJECT_REPLICATION_COL_CUR_VERSION));
		dto.setCreatedBy(rs.getLong(OBJECT_REPLICATION_COL_CREATED_BY));
		dto.setCreatedOn(new Date(rs.getLong(OBJECT_REPLICATION_COL_CREATED_ON)));
		dto.setEtag(rs.getString(OBEJCT_REPLICATION_COL_ETAG));
		dto.setName(rs.getString(OBJECT_REPLICATION_COL_NAME));
		dto.setDescription(rs.getString(OBJECT_REPLICATION_COL_DESCRIPTION));
		dto.setSubType(SubType.valueOf(rs.getString(OBJECT_REPLICATION_COL_SUBTYPE)));
		dto.setParentId(rs.getLong(OBJECT_REPLICATION_COL_PARENT_ID));
		if (rs.wasNull()) {
			dto.setParentId(null);
		}
		dto.setBenefactorId(rs.getLong(OBJECT_REPLICATION_COL_BENEFACTOR_ID));
		if (rs.wasNull()) {
			dto.setBenefactorId(null);
		}
		dto.setProjectId(rs.getLong(OBJECT_REPLICATION_COL_PROJECT_ID));
		if (rs.wasNull()) {
			dto.setProjectId(null);
		}
		dto.setModifiedBy(rs.getLong(OBJECT_REPLICATION_COL_MODIFIED_BY));
		dto.setModifiedOn(new Date(rs.getLong(OBJECT_REPLICATION_COL_MODIFIED_ON)));
		dto.setFileHandleId(rs.getLong(OBJECT_REPLICATION_COL_FILE_ID));
		if (rs.wasNull()) {
			dto.setFileHandleId(null);
		}
		dto.setFileSizeBytes(rs.getLong(OBJECT_REPLICATION_COL_FILE_SIZE_BYTES));
		if (rs.wasNull()) {
			dto.setFileSizeBytes(null);
		}
		dto.setIsInSynapseStorage(rs.getBoolean(OBJECT_REPLICATION_COL_IN_SYNAPSE_STORAGE));
		if (rs.wasNull()) {
			dto.setIsInSynapseStorage(null);
		}
		dto.setFileMD5(rs.getString(OBJECT_REPLICATION_COL_FILE_MD5));
		dto.setItemCount(rs.getLong(OBJECT_REPLICATION_COL_ITEM_COUNT));
		if(rs.wasNull()){
			dto.setItemCount(null);
		}
		dto.setFileConcreteType(rs.getString(OBJECT_REPLICATION_COL_FILE_CONCRETE_TYPE));
		dto.setFileBucket(rs.getString(OBJECT_REPLICATION_COL_FILE_BUCKET));
		dto.setFileKey(rs.getString(OBJECT_REPLICATION_COL_FILE_KEY));
		return dto;
	};
	
	@SuppressWarnings("unchecked")
	public static RowMapper<ObjectAnnotationDTO> OBJECT_ANNOTATION_ROW_MAPPER = (ResultSet rs, int rowNum) -> {
		ObjectAnnotationDTO dto = new ObjectAnnotationDTO();
		dto.setObjectId(rs.getLong(ANNOTATION_REPLICATION_COL_OBJECT_ID));
		dto.setObjectVersion(rs.getLong(ANNOTATION_REPLICATION_COL_OBJECT_VERSION));
		dto.setKey(rs.getString(ANNOTATION_REPLICATION_COL_KEY));
		dto.setType(AnnotationType.valueOf(rs.getString(ANNOTATION_REPLICATION_COL_TYPE)));
		dto.setDerived(rs.getBoolean(ANNOTATION_REPLICATION_COL_IS_DERIVED));
		dto.setValue((List<String>) (List<?>) new JSONArray(rs.getString(ANNOTATION_REPLICATION_COL_STRING_LIST_VALUE))
						.toList());
		return dto;
	};

	private static final String SQL_SUM_FILE_SIZES = "SELECT SUM(" + OBJECT_REPLICATION_COL_FILE_SIZE_BYTES + ")"
			+ " FROM " + OBJECT_REPLICATION_TABLE 
			+ " WHERE " + OBJECT_REPLICATION_COL_OBJECT_TYPE + " =:" + OBJECT_TYPE_PARAM_NAME 
			+ " AND (" + OBJECT_REPLICATION_COL_OBJECT_ID + ", " + OBJECT_REPLICATION_COL_OBJECT_VERSION +") IN (:" +ID_PARAM_NAME+ ")";

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
	
	private static final String KEY_NAME = "Key_name";
	private static final String COLUMN_NAME = "Column_name";
	private static final String SHOW_INDEXES_FROM = "SHOW INDEXES FROM ";
	private static final String KEY = "Key";
	private static final String SQL_SHOW_COLUMNS = "SHOW FULL COLUMNS FROM ";
	private static final String FIELD = "Field";
	private static final String STALE_TABLE_PREFIX = "STALE_";

	private static final RowMapper<DatabaseColumnInfo> DB_COL_INFO_MAPPER = (rs, rowNum) -> {
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
	};
	
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
		template.update(SQLUtils.dropTableSQL(tableId, SQLUtils.TableIndexType.INDEX));
		deleteSecondaryTables(tableId);
	}
	
	/**
	 * Delete secondary table associated with the given tableId.
	 * @param tableId
	 */
	void deleteSecondaryTables(IdAndVersion tableId) {
		for(TableIndexType type: SQLUtils.SECONDARY_TYPES){
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
	public boolean isSearchEnabled(IdAndVersion tableId) {
		String sql = SQLUtils.getSearchStatusSQL(tableId);
		try {
			return template.queryForObject(sql, Long.class) > 0L;
		} catch (BadSqlGrammarException e) {
			// This is thrown if the status table was not created yet
			return false;
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
	public boolean doesIndexHashMatchSchemaHash(IdAndVersion tableId, List<ColumnModel> newSchema) {
		ValidateArgument.required(tableId, "tableId");
		ValidateArgument.required(newSchema, "newSchema");
		String newSchemaHash = TableModelUtils
				.createSchemaMD5Hex(newSchema.stream().map(ColumnModel::getId).collect(Collectors.toList()));
		Optional<String> indexHashOptional = getCurrentSchemaMD5Hex(tableId);
		if(indexHashOptional.isEmpty()) {
			return false;
		}
		return indexHashOptional.get().equals(newSchemaHash);
	}
	
	@Override
	public void setSearchEnabled(IdAndVersion tableId, boolean searchStatus) {
		String createOrUpdateStatusSql = SQLUtils.buildCreateOrUpdateSearchStatusSQL(tableId);
		template.update(createOrUpdateStatusSql, searchStatus, searchStatus);
	}

	@Override
	public Optional<String> getCurrentSchemaMD5Hex(IdAndVersion tableId) {
		String sql = SQLUtils.getSchemaHashSQL(tableId);
		try {
			return Optional.of(template.queryForObject(sql, new SingleColumnRowMapper<String>()));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		} catch (BadSqlGrammarException e) {
			// This is thrown if the status table was not created yet
			return Optional.empty();
		}
	}
	
	@Override
	public void createSecondaryTables(IdAndVersion tableId) {
		for(TableIndexType type: SQLUtils.SECONDARY_TYPES){
			String sql = SQLUtils.createTableSQL(tableId, type);
			template.update(sql);
		}	
	}

	@Override
	public JdbcTemplate getConnection() {
		return template;
	}

	@Override
	public RowSet query(ProgressCallback callback, final QueryTranslator query) {
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
		rowSet.setTableId(query.getSingleTableId().orElseThrow(TableConstants.JOIN_NOT_SUPPORTED_IN_THIS_CONTEXT));
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
	public boolean queryAsStream(final ProgressCallback callback, final QueryTranslator query, final RowHandler handler) {
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
	public boolean doesIndexStateMatch(IdAndVersion tableId, long versionNumber, String schemaMD5Hex, boolean searchEnabled) {
		long indexVersion = getMaxCurrentCompleteVersionForTable(tableId);
		if (indexVersion != versionNumber){
			return false;
		}
		Optional<String> indexMD5Optional = getCurrentSchemaMD5Hex(tableId);
		if(indexMD5Optional.isEmpty()) {
			return false;
		}
		if (!indexMD5Optional.get().equals(schemaMD5Hex)) {
			return false;
		}
		return searchEnabled == isSearchEnabled(tableId);
	}

	@Override
	public Set<Long> getDistinctLongValues(IdAndVersion tableId, String columnName) {
		String sql = SQLUtils.createSQLGetDistinctValues(tableId, columnName);
		List<Long> results = template.queryForList(sql, Long.class);
		return new HashSet<Long>(results);
	}

	@Override
	public void createTableIfDoesNotExist(IndexDescription description) {
		template.update(description.getCreateOrUpdateIndexSql());
	}

	@Override
	public boolean alterTableAsNeeded(IdAndVersion tableId, List<ColumnChangeDetails> changes, boolean alterTemp) {
		// get SQL statements for altering table
		String[] sqlStatements = SQLUtils.createAlterTableSql(changes, tableId, alterTemp);
		if (sqlStatements.length == 0) {
			// no changes made
			return false;
		}
		template.batchUpdate(sqlStatements);
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

	List<String> getMultivalueColumnIndexTableNames(IdAndVersion tableId, boolean alterTemp){
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
			String tableName = SQLUtils.getTableNameForId(tableId, SQLUtils.TableIndexType.INDEX);
			// Bind variables do not seem to work here
			return template.query(SQL_SHOW_COLUMNS + tableName, DB_COL_INFO_MAPPER);
		} catch (BadSqlGrammarException e) {
			// Spring throws this when the table does not exist
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
					if (rs.wasNull()) {
						// When we run the MAX(constant) and there are no rows, MySQL returns NULL
						info.setCardinality(TableConstants.COLUMN_NO_CARDINALITY);
					}
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
		String tableName = SQLUtils.getTableNameForId(tableId, SQLUtils.TableIndexType.INDEX);
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
	public void createObjectReplicationTablesIfDoesNotExist() {
		template.update(OBJECT_REPLICATION_TABLE_CREATE);
		template.update(ANNOTATION_REPLICATION_TABLE_CREATE);
		template.update(REPLICATION_SYNCH_EXPIRATION_TABLE_CREATE);
	}

	@Override
	public void deleteObjectData(ReplicationType mainType, List<Long> objectIds) {
		final List<Long> sorted = new ArrayList<Long>(objectIds);
		// sort to prevent deadlock.
		Collections.sort(sorted);
		// Batch delete.
		template.batchUpdate(TableConstants.OBJECT_REPLICATION_DELETE_ALL, new BatchPreparedStatementSetter(){

			@Override
			public void setValues(PreparedStatement ps, int i)
					throws SQLException {
				int parameterIndex = 1;
				ps.setString(parameterIndex++, mainType.name());
				ps.setLong(parameterIndex++, sorted.get(i));
			}

			@Override
			public int getBatchSize() {
				return sorted.size();
			}});
		
	}

	@Override
	public void addObjectData(ReplicationType mainType, List<ObjectDataDTO> objectDtos) {
		final List<ObjectDataDTO> sorted = new ArrayList<ObjectDataDTO>(ObjectDataDTO.deDuplicate(objectDtos));
		Collections.sort(sorted);
		
		// batch update the object replication table
		template.batchUpdate(TableConstants.OBJECT_REPLICATION_INSERT_OR_UPDATE, new BatchPreparedStatementSetter(){

			@Override
			public void setValues(PreparedStatement ps, int i)
					throws SQLException {
				ObjectDataDTO dto = sorted.get(i);
				int parameterIndex = 1;
				int updateOffset = 19;
				
				ps.setString(parameterIndex++, mainType.name());
				ps.setLong(parameterIndex++, dto.getId());
				ps.setLong(parameterIndex++, dto.getVersion());
				
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
				
				if (dto.getDescription() != null) {
					ps.setString(parameterIndex++, dto.getDescription());
					ps.setString(parameterIndex + updateOffset, dto.getDescription());
				} else {
					ps.setNull(parameterIndex++, java.sql.Types.VARCHAR);
					ps.setNull(parameterIndex + updateOffset, java.sql.Types.VARCHAR);
				}
				
				ps.setString(parameterIndex++, dto.getSubType().name());
				ps.setString(parameterIndex + updateOffset, dto.getSubType().name());
				
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
				if (dto.getItemCount() != null) {
					ps.setLong(parameterIndex++, dto.getItemCount());
					ps.setLong(parameterIndex + updateOffset, dto.getItemCount());
				} else {
					ps.setNull(parameterIndex++, java.sql.Types.BIGINT);
					ps.setNull(parameterIndex + updateOffset, java.sql.Types.BIGINT);
				}
				if (dto.getFileConcreteType() != null) {
					ps.setString(parameterIndex++, dto.getFileConcreteType());
					ps.setString(parameterIndex + updateOffset, dto.getFileConcreteType());
				}else {
					ps.setNull(parameterIndex++, java.sql.Types.VARCHAR);
					ps.setNull(parameterIndex + updateOffset, java.sql.Types.VARCHAR);
				}
				if (dto.getFileBucket() != null) {
					ps.setString(parameterIndex++, dto.getFileBucket());
					ps.setString(parameterIndex + updateOffset, dto.getFileBucket());
				}else {
					ps.setNull(parameterIndex++, java.sql.Types.VARCHAR);
					ps.setNull(parameterIndex + updateOffset, java.sql.Types.VARCHAR);
				}
				if (dto.getFileKey() != null) {
					ps.setString(parameterIndex++, dto.getFileKey());
					ps.setString(parameterIndex + updateOffset, dto.getFileKey());
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
				SQLUtils.writeAnnotationDtoToPreparedStatement(mainType, ps, dto);
			}

			@Override
			public int getBatchSize() {
				return annotations.size();
			}});
		
	}

	@Override
	public ObjectDataDTO getObjectData(ReplicationType mainType, Long objectId, Long objectVersion) {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("mainType", mainType.name());
		params.addValue("objectId", objectId);
		params.addValue("objectVersion", objectVersion);

		String objectSql = "SELECT * FROM "+ OBJECT_REPLICATION_TABLE + " WHERE "
				+ OBJECT_REPLICATION_COL_OBJECT_TYPE + " = :mainType AND "
				+ OBJECT_REPLICATION_COL_OBJECT_ID+" = :objectId AND "
				+ OBJECT_REPLICATION_COL_OBJECT_VERSION+" = :objectVersion";
		
		String annotationSql = "SELECT * FROM "+ANNOTATION_REPLICATION_TABLE+" WHERE "
				+ANNOTATION_REPLICATION_COL_OBJECT_TYPE+" = :mainType AND "
				+ANNOTATION_REPLICATION_COL_OBJECT_ID+" = :objectId AND "
				+ANNOTATION_REPLICATION_COL_OBJECT_VERSION+" = :objectVersion";	
		
		return getObjectData(objectSql, annotationSql, params);
	}
	
	@Override
	public ObjectDataDTO getObjectDataForCurrentVersion(ReplicationType mainType, Long objectId) {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("mainType", mainType.name());
		params.addValue("objectId", objectId);

		String objectSql = "SELECT * FROM " + OBJECT_REPLICATION_TABLE + " WHERE " + OBJECT_REPLICATION_COL_OBJECT_TYPE
				+ " = :mainType AND " + OBJECT_REPLICATION_COL_OBJECT_ID + " = :objectId AND "
				+ OBJECT_REPLICATION_COL_OBJECT_VERSION + " = " + OBJECT_REPLICATION_COL_CUR_VERSION;

		String annotationSql = "SELECT A.* FROM " + OBJECT_REPLICATION_TABLE + " O JOIN " + ANNOTATION_REPLICATION_TABLE
				+ " A ON (O." + OBJECT_REPLICATION_COL_OBJECT_TYPE + " = A." + ANNOTATION_REPLICATION_COL_OBJECT_TYPE
				+ " AND O." + OBJECT_REPLICATION_COL_OBJECT_ID + " = A." + ANNOTATION_REPLICATION_COL_OBJECT_ID
				+ " AND O." + OBJECT_REPLICATION_COL_OBJECT_VERSION + " = A."
				+ ANNOTATION_REPLICATION_COL_OBJECT_VERSION + ") " + " WHERE O." + OBJECT_REPLICATION_COL_OBJECT_TYPE
				+ " = :mainType AND O." + OBJECT_REPLICATION_COL_OBJECT_ID + " = :objectId AND O."
				+ OBJECT_REPLICATION_COL_OBJECT_VERSION + " = O." + OBJECT_REPLICATION_COL_CUR_VERSION;

		return getObjectData(objectSql, annotationSql, params);
	}
	
	ObjectDataDTO getObjectData(String objectSql, String annotationSql, MapSqlParameterSource params) {
		// query for the template.
		ObjectDataDTO dto;
		try {
			dto = namedTemplate.queryForObject(objectSql, params, OBJECT_DATA_ROW_MAPPER);
		} catch (DataAccessException e) {
			return null;
		}
		// get the annotations.
		List<ObjectAnnotationDTO> annotations = namedTemplate.query(annotationSql, params, OBJECT_ANNOTATION_ROW_MAPPER);
		if (!annotations.isEmpty()) {
			dto.setAnnotations(annotations);
		}
		return dto;
	}
	
	/***
	 * Get the maximum number of elements in a list for each annotation column
	 * @param mainType
	 * @param viewId
	 * @param viewTypeMask
	 * @param allContainersInScope
	 * @param objectIdFilter
	 * @return Map where the key is the columnId found in curentSchema,
	 * and value is the maximum number of list elements for that annotation
	 */
	Map<String, Long> getMaxListSizeForAnnotations(ViewFilter filter, Set<String> annotationNames){
		ValidateArgument.required(filter, "filter");
		ValidateArgument.required(annotationNames, "annotationNames");
		
		if(filter.isEmpty() || annotationNames.isEmpty()){
			// nothing to do if the scope or annotation names are empty.
			return Collections.emptyMap();
		}
		
		Map<String, Object> param = filter.getParameters();

		//additional param
		param.put(ANNOTATION_KEYS_PARAM_NAME, annotationNames);
		String sql = SQLUtils.createAnnotationMaxListLengthSQL(annotationNames, filter.getFilterSql());
		return namedTemplate.query(sql,param, resultSet -> {
			Map<String, Long> result = new HashMap<>();
			while(resultSet.next()){
				result.put(resultSet.getString(1),resultSet.getLong(2));
			}
			return result;
		});
	}

	void validateMaxListLengthInAnnotationReplication(ViewFilter filter, List<ColumnModel> currentSchema){
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

		Map<String,Long> maxLengthsInReplication = this.getMaxListSizeForAnnotations(filter, listAnnotationListLengthMaximum.keySet());

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
	public Optional<Long> tempTableColumnExceedsCharacterLimit(IdAndVersion tableId, String columnId, long characterLimit) {
		String sql = "SELECT " + TableConstants.ROW_ID + " FROM " + SQLUtils.getTemporaryTableName(tableId) 
			+ " WHERE CHAR_LENGTH(" + SQLUtils.getColumnNameForId(columnId) +") > ? LIMIT 1";
		
		List<Long> rowId = template.queryForList(sql, Long.class, characterLimit);
		
		return rowId.isEmpty() ? Optional.empty() : Optional.of(rowId.iterator().next());
	}
	
	@Override
	public void copyObjectReplicationToView(Long viewId, ViewFilter filter, List<ColumnModel> currentSchema, ObjectFieldTypeMapper fieldTypeMapper) {
		ValidateArgument.required(filter, "filter");
		
		if (filter.isEmpty()){
			// nothing to do if the scope is empty.
			return;
		}
		
		// before updating. verify that all rows that would be changed won't exceed the user-specified maxListLength,
		// which is used for query row size estimation
		validateMaxListLengthInAnnotationReplication(filter, currentSchema);
				
		Map<String, Object> param = filter.getParameters();
		
		List<ColumnMetadata> metadata = translateSchema(currentSchema, fieldTypeMapper);
		
		String sql = SQLUtils.createSelectInsertFromObjectReplication(viewId, metadata, filter.getFilterSql());
		
		namedTemplate.update(sql, param);
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
	public List<ColumnModel> getPossibleColumnModelsForContainers(ViewFilter filter, Long limit, Long offset) {
		ValidateArgument.required(filter, "filter");
		ValidateArgument.required(limit, "limit");
		ValidateArgument.required(offset, "offset");
		
		if(filter.isEmpty()){
			return Collections.emptyList();
		}
		
		Map<String, Object> param = filter.getParameters();
		
		param.put(P_LIMIT, limit);
		param.put(P_OFFSET, offset);
		
		String sql = SQLUtils.getDistinctAnnotationColumnsSql(filter.getFilterSql());
		
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
				if (ColumnType.STRING == type || ColumnType.STRING_LIST == type) {
					if (aggregation.getMaxStringElementSize() == null || 
							aggregation.getMaxStringElementSize() == 0L) {
						model.setMaximumSize(ColumnConstants.DEFAULT_STRING_SIZE);
					} else {
						model.setMaximumSize(aggregation.getMaxStringElementSize());
					}
				}
				results.add(model);
			}
		}
		return results;
	}
	
	@Override
	public Map<Long, Long> getSumOfChildCRCsForEachParent(ReplicationType mainType, List<Long> parentIds) {
		ValidateArgument.required(mainType, "mainType");
		ValidateArgument.required(parentIds, "parentIds");
		final Map<Long, Long> results = new HashMap<>();
		if(parentIds.isEmpty()){
			return results;
		}
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(OBJECT_TYPE_PARAM_NAME, mainType.name());
		param.addValue(PARENT_ID_PARAM_NAME, parentIds);
		namedTemplate.query(SELECT_OBJECT_CHILD_CRC, param, (ResultSet rs) -> {
			Long parentId = rs.getLong(OBJECT_REPLICATION_COL_PARENT_ID);
			Long crc = rs.getLong(CRC_ALIAS);
			results.put(parentId, crc);
		});
		return results;
	}

	@Override
	public List<IdAndEtag> getObjectChildren(ReplicationType mainType, Long parentId) {
		ValidateArgument.required(mainType, "mainType");
		ValidateArgument.required(parentId, "parentId");
		return this.template.query(SELECT_OBJECT_CHILD_ID_ETAG, (ResultSet rs, int rowNum) -> {
			Long id = rs.getLong(TableConstants.OBJECT_REPLICATION_COL_OBJECT_ID);
			String etag = rs.getString(OBEJCT_REPLICATION_COL_ETAG);
			Long benefactorId = rs.getLong(OBJECT_REPLICATION_COL_BENEFACTOR_ID);
			if (rs.wasNull()) {
				benefactorId = null;
			}
			return new IdAndEtag(id, etag, benefactorId);
		}, mainType.name(), parentId);
	}
	
	@Override
	public boolean isSynchronizationLockExpiredForObject(ReplicationType mainType, Long objectId) {
		ValidateArgument.required(objectId, "objectId");
		// Query for those that are not expired.
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(OBJECT_TYPE_PARAM_NAME, mainType.name());
		param.addValue(ID_PARAM_NAME, objectId);
		param.addValue(EXPIRES_PARAM_NAME, System.currentTimeMillis());
		List<Long> nonExpiredIds =  namedTemplate.queryForList(SELECT_NON_EXPIRED_IDS, param, Long.class);
		return nonExpiredIds.isEmpty();
	}

	@Override
	public void setSynchronizationLockExpiredForObject(ReplicationType mainType, Long objectId,
			Long newExpirationDateMS) {
		ValidateArgument.required(mainType, "mainType");
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.required(newExpirationDateMS, "newExpirationDateMS");
		template.update(BATCH_INSERT_REPLICATION_SYNC_EXP, mainType.name(),objectId, newExpirationDateMS, newExpirationDateMS);		
	}


	@Override
	public List<IdAndVersion> getRowIdAndVersions(String sql, Map<String, Object> parameters) {
		ValidateArgument.required(sql, "sql");
		ValidateArgument.required(parameters, "parameters");
		// We use spring to create create the prepared statement
		return namedTemplate.query(sql, new MapSqlParameterSource(parameters), (ResultSet rs, int rowNum) -> 
			IdAndVersion.newBuilder().setId(rs.getLong(1)).setVersion(rs.getLong(2)).build()
		);
	}

	@Override
	public long getSumOfFileSizes(ReplicationType mainType, List<IdAndVersion> rowIdAndVersions) {
		ValidateArgument.required(rowIdAndVersions, "rowIdAndVersions");
		if(rowIdAndVersions.isEmpty()) {
			return 0L;
		}
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(OBJECT_TYPE_PARAM_NAME, mainType.name());
		
		List<Long[]> idAndVersionParam = rowIdAndVersions.stream()
				.map((IdAndVersion id) -> new Long[] {id.getId(), id.getVersion().orElseThrow(() -> new IllegalArgumentException("The object with id " + id + " must specify a version."))})
				.collect(Collectors.toList());
		
		params.addValue(ID_PARAM_NAME, idAndVersionParam);

		Long sum = namedTemplate.queryForObject(SQL_SUM_FILE_SIZES, params, Long.class);
		
		if(sum == null) {
			sum =  0L;
		}
		
		return sum;
	}

	@Override
	public void streamSynapseStorageStats(ReplicationType mainType, Callback<SynapseStorageProjectStats> callback) {
		MapSqlParameterSource params = new MapSqlParameterSource(OBJECT_TYPE_PARAM_NAME, mainType.name());
		
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
	public Set<Long> getOutOfDateRowsForView(IdAndVersion viewId, ViewFilter filter, long limit) {
		ValidateArgument.required(viewId, "viewId");
		ValidateArgument.required(filter, "filter");
				
		if(filter.isEmpty()) {
			return Collections.emptySet();
		}
		
		String sql = SQLUtils.getOutOfDateRowsForViewSql(viewId, filter.getFilterSql());
		
		Map<String, Object> param = filter.getParameters();
		
		param.put(P_LIMIT, limit);
		
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

	@Override
	public boolean refreshViewBenefactors(IdAndVersion viewId, ReplicationType mainType) {
		ValidateArgument.required(viewId, "viewId");
		ValidateArgument.required(mainType, "mainType");
		String sql = SQLUtils.generateSqlToRefreshViewBenefactors(viewId);
		return template.update(sql, mainType.name()) > 0;
	}

	@Override
	public List<IdAndChecksum> getIdAndChecksumsForFilter(Long salt, ViewFilter filter, Long limit, Long offset) {
		ValidateArgument.required(salt, "salt");
		ValidateArgument.required(filter, "filter");
		if(filter.isEmpty()) {
			return Collections.emptyList();
		}
		ValidateArgument.required(limit, "limit");
		ValidateArgument.required(offset, "offset");
		
		String sql = String.format(GET_ID_AND_CHECKSUMS_SQL_TEMPLATE, filter.getObjectIdFilterSql());
		Map<String, Object> params = new HashMap<String, Object>(filter.getParameters());
		params.put("salt", salt);
		params.put("limit", limit);
		params.put("offset", offset);
		
		return namedTemplate.query(sql, params, (ResultSet rs, int rowNum) -> {
			return new IdAndChecksum().withId(rs.getLong("ID")).withChecksum(rs.getLong("CHECK_SUM"));
		});
	}
		
	@Override
	public List<TableRowData> getTableDataForRowIds(IdAndVersion idAndVersion, List<ColumnModel> selectColumns, Set<Long> rowIds) {
		ValidateArgument.required(idAndVersion, "idAndVersion");
		ValidateArgument.requiredNotEmpty(selectColumns, "selectColumns");
		ValidateArgument.required(rowIds, "rowIds");
		
		if (rowIds.isEmpty()) {
			return Collections.emptyList();
		}
		
		String sql = SQLUtils.buildSelectTableDataByRowIdSQL(idAndVersion, selectColumns);
		
		Map<String, Object> params = Collections.singletonMap(TableConstants.ROW_ID, rowIds);
		
		RowMapper<TableRowData> rowMapper = getTableDataMapper(selectColumns);
		
		return namedTemplate.query(sql, params, rowMapper);
	}
	
	@Override
	public List<TableRowData> getTableDataPage(IdAndVersion idAndVersion, List<ColumnModel> selectColumns, long limit, long offset) {
		ValidateArgument.required(idAndVersion, "idAndVersion");
		ValidateArgument.requiredNotEmpty(selectColumns, "selectColumns");
		
		String sql = SQLUtils.buildSelectTableDataPage(idAndVersion, selectColumns);
		
		Map<String, Object> params = ImmutableMap.of(
			TableConstants.P_LIMIT, limit,
			TableConstants.P_OFFSET, offset
		);
		
		RowMapper<TableRowData> rowMapper = getTableDataMapper(selectColumns);
		
		return namedTemplate.query(sql, params, rowMapper);
	}
	
	private RowMapper<TableRowData> getTableDataMapper(List<ColumnModel> selectColumns) {
		return (rs, rowNum) -> {
			int columnIndex = 1;
			
			// The first column is always the row id
			Long rowId = rs.getLong(columnIndex++);

			List<TypedCellValue> rowData = new ArrayList<>(selectColumns.size());
			
			for (ColumnModel columnModel : selectColumns) {
				String rawValue = rs.getString(columnIndex++);
				ColumnTypeInfo columnInfo = ColumnTypeInfo.getInfoForType(columnModel.getColumnType());
				String value = columnInfo.parseValueForDatabaseRead(rawValue);
				rowData.add(new TypedCellValue(columnModel.getColumnType(), value));
			}
			
			return new TableRowData(rowId, rowData);
		};
	}	

	@Override
	public List<String> streamTableIndexData(IdAndVersion tableId, CSVWriterStream stream) {
		List<DatabaseColumnInfo> columnList = getDatabaseInfo(tableId);
		
		String[] metadataColumns = columnList.stream()
			.filter(column -> column.isMetadata() && !TableConstants.ROW_SEARCH_CONTENT.equalsIgnoreCase(column.getColumnName()))
			.map(DatabaseColumnInfo::getColumnName)
			.collect(Collectors.toList())
			.toArray(String[]::new);
		
		List<ColumnModel> schema = SQLUtils.extractSchemaFromInfo(columnList);
		
		List<String> headers = SQLUtils.getSelectTableDataHeaders(schema, metadataColumns);

		String selectSql = SQLUtils.buildSelectTableData(tableId, schema, metadataColumns).toString();

		// Write the headers first
		stream.writeNext(headers.toArray(String[]::new));
		
		namedTemplate.query(selectSql, (ResultSet rs) -> {
			String[] row = new String[headers.size()];
			for (int i = 0; i < headers.size(); i++) {
				row[i] = rs.getString(i + 1);
			}
			stream.writeNext(row);
		});
		
		return schema.stream().map(ColumnModel::getId).collect(Collectors.toList());
	}

	@Override
	public void restoreTableIndexData(IdAndVersion idAndVersion, Iterator<String[]> input, long maxBytesPerBatch) {
		ValidateArgument.required(idAndVersion, "idAndVersion");
		ValidateArgument.required(idAndVersion.getVersion().isPresent(), "idAndVersion.version");
		ValidateArgument.required(input, "input");
		ValidateArgument.required(input.hasNext(), "input is empty");
		// The first row is the header
		String[] headers = input.next();
		String sql = SQLUtils.createInsertIntoTableIndex(idAndVersion, headers);

		// push the data in batches
		List<Object[]> batch = new LinkedList<>();
		int batchSize = 0;
		while (input.hasNext()) {
			String[] row = input.next();
			long rowSize = SQLUtils.calculateBytes(row);
			if (batchSize + rowSize > maxBytesPerBatch) {
				writeTransactionTemplate.executeWithoutResult(txStatus -> template.batchUpdate(sql, batch));
				batch.clear();
				batchSize = 0;
			}
			batch.add(row);
			batchSize += rowSize;
		}

		if (!batch.isEmpty()) {
			writeTransactionTemplate.executeWithoutResult(txStatus -> template.batchUpdate(sql, batch));
		}
	}
	
	@Override
	public void updateSearchIndex(IdAndVersion idAndVersion, List<RowSearchContent> searchContentRows) {
		ValidateArgument.required(idAndVersion, "idAndVersion");
		ValidateArgument.requiredNotEmpty(searchContentRows, "searchContentRows");
		
		String updateSql = SQLUtils.buildBatchUpdateSearchContentSql(idAndVersion);
		
		List<Object[]> batchUpdateArgs = new ArrayList<>(searchContentRows.size());
		
		for (RowSearchContent searchContent : searchContentRows) {
			batchUpdateArgs.add(new Object[] { 
				searchContent.getSearchContent(), 
				searchContent.getRowId()
			});
		}

		writeTransactionTemplate.executeWithoutResult( txStatus -> template.batchUpdate(updateSql, batchUpdateArgs));
	}
	
	@Override
	public void clearSearchIndex(IdAndVersion idAndVersion) {
		ValidateArgument.required(idAndVersion, "idAndVersion");
		
		String updateSql = SQLUtils.buildClearSearchContentSql(idAndVersion);
		
		writeTransactionTemplate.executeWithoutResult( txStatus -> template.update(updateSql));
		
	}
	
	@Override
	public List<RowSearchContent> fetchSearchContent(IdAndVersion id, Set<Long> rowIds) {
		String sql = "SELECT " + TableConstants.ROW_ID + ", " + TableConstants.ROW_SEARCH_CONTENT + " FROM " + SQLUtils.getTableNameForId(id, TableIndexType.INDEX) + " WHERE " + TableConstants.ROW_ID + " IN(:" + TableConstants.ROW_ID + ") ORDER BY " + TableConstants.ROW_ID;
		return namedTemplate.query(sql, Collections.singletonMap(TableConstants.ROW_ID, rowIds), (RowMapper<RowSearchContent>) (rs, rowNum) -> new RowSearchContent(rs.getLong(1), rs.getString(2)));
	}

	@Override
	public void update(String sql, Map<String, Object> parameters) {
		namedTemplate.update(sql, parameters);
	}
	
	@Override
	public void swapTableIndex(IdAndVersion sourceIndexId, IdAndVersion targetIndexId) {
		ValidateArgument.required(sourceIndexId, "sourceIndexId");
		ValidateArgument.required(targetIndexId, "targetIndexId");
		ValidateArgument.requirement(!sourceIndexId.equals(targetIndexId), "The source index id and the target index id cannot be the same");
		
		String randomPrefix = STALE_TABLE_PREFIX + RandomStringUtils.randomAlphanumeric(8) + "_";
		StringBuilder sqlBuilder = new StringBuilder("RENAME TABLE ");
		
		// First rename the main index tables
		Arrays.stream(TableIndexType.values()).forEach( indexType -> {
			String targetTableName = SQLUtils.getTableNameForId(targetIndexId, indexType);
			String sourceTableName = SQLUtils.getTableNameForId(sourceIndexId, indexType);
			String tmpTableName = randomPrefix + targetTableName;
			
			// First rename the target index to a random name
			sqlBuilder.append(targetTableName).append(" TO ").append(tmpTableName).append(",");
			// Now rename the source index to the target index
			sqlBuilder.append(sourceTableName).append(" TO ").append(targetTableName).append(",");
			// Now rename the random name to the source index
			sqlBuilder.append(tmpTableName).append(" TO ").append(sourceTableName).append(",");
		});
			
		// Now we also swap the multi value tables
		
		List<String> targetMultiValueTableNames = getMultivalueColumnIndexTableNames(targetIndexId, false);
		List<String> tmpMultiValueTableNames = new ArrayList<>(targetMultiValueTableNames.size());
		
		// First rename the target MV tables to random table names
		targetMultiValueTableNames.forEach( targetTableName -> {
			String tmpTableName = randomPrefix + targetTableName;
			sqlBuilder.append(targetTableName).append(" TO ").append(tmpTableName).append(",");
			tmpMultiValueTableNames.add(tmpTableName);
		});
		
		// We can now rename the source tables into the target
		List<String> sourceMultiValueTableNames = getMultivalueColumnIndexTableNames(sourceIndexId, false);
		
		String sourceMultiValueTableNamePrefix = SQLUtils.getTableNamePrefixForMultiValueColumns(sourceIndexId, false);
		String targetMultiValueTableNamePrefix = SQLUtils.getTableNamePrefixForMultiValueColumns(targetIndexId, false);
		
		// Now we also rename the source MV tables into the target index
		sourceMultiValueTableNames.forEach( sourceTableName -> {
			String targetTableName = sourceTableName.replace(sourceMultiValueTableNamePrefix, targetMultiValueTableNamePrefix);
			sqlBuilder.append(sourceTableName).append(" TO ").append(targetTableName).append(",");
		});
		
		// We can now rename all the tmp tables to the source index prefix
		tmpMultiValueTableNames.forEach( tmpTableName -> {
			String sourceTableName = tmpTableName.replace(randomPrefix, "").replace(targetMultiValueTableNamePrefix, sourceMultiValueTableNamePrefix);
			sqlBuilder.append(tmpTableName).append(" TO ").append(sourceTableName).append(",");
		});
		
		sqlBuilder.deleteCharAt(sqlBuilder.length() - 1);
	
		template.update(sqlBuilder.toString());
	}
	
}
