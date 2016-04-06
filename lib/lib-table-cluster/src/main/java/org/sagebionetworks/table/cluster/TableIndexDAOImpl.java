package org.sagebionetworks.table.cluster;

import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_VERSION;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.dao.table.RowAndHeaderHandler;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.table.cluster.SQLUtils.TableType;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
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

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class TableIndexDAOImpl implements TableIndexDAO {

	private static final String SQL_SHOW_COLUMNS = "SHOW COLUMNS FROM ";
	private static final String FIELD = "Field";
	private static final String TYPE = "Type";
	private static final Pattern VARCHAR = Pattern.compile("varchar\\((\\d+)\\)");

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
	public boolean createOrUpdateTable(List<ColumnModel> newSchema,
			String tableId) {
		// First determine if we have any columns for this table yet
		List<ColumnDefinition> oldColumnDefs = getCurrentTableColumns(tableId);
		List<String> oldColumns = oldColumnDefs == null ? null : Lists.transform(oldColumnDefs, new Function<ColumnDefinition, String>() {
			@Override
			public String apply(ColumnDefinition input) {
				return input.name;
			}
		});
		// Build the SQL to create or update the table
		String dml = SQLUtils.creatOrAlterTableSQL(oldColumns, newSchema, tableId);
		// If there is nothing to apply then do nothing
		if (dml == null)
			return false;
		// Execute the DML
		try {
			template.update(dml);
		} catch (BadSqlGrammarException e) {
			if (e.getCause() != null && e.getCause().getMessage() != null && e.getCause().getMessage().startsWith("Row size too large")) {
				throw new InvalidDataAccessResourceUsageException(
						"Too much data per column. The maximum size for a row is about 65000 bytes", e.getCause());
			} else {
				throw e;
			}
		}
		return true;
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
	public List<ColumnDefinition> getCurrentTableColumns(String tableId) {
		String tableName = SQLUtils.getTableNameForId(tableId, SQLUtils.TableType.INDEX);
		// Bind variables do not seem to work here
		try {
			return template.query(SQL_SHOW_COLUMNS + tableName, new RowMapper<ColumnDefinition>() {
				@Override
				public ColumnDefinition mapRow(ResultSet rs, int rowNum) throws SQLException {
					ColumnDefinition columnDefinition = new ColumnDefinition();
					columnDefinition.name = rs.getString(FIELD);
					columnDefinition.maxSize = null;
					String type = rs.getString(TYPE);
					Matcher m = VARCHAR.matcher(type);
					if (m.matches()) {
						columnDefinition.columnType = ColumnType.STRING;
						columnDefinition.maxSize = Long.parseLong(m.group(1));
					}
					return columnDefinition;
				}
			});
		} catch (BadSqlGrammarException e) {
			// Spring throws this when the table does not
			return null;
		}
	}

	@Override
	public void createOrUpdateOrDeleteRows(final RowSet rowset,
			final List<ColumnModel> schema) {
		if (rowset == null)
			throw new IllegalArgumentException("Rowset cannot be null");
		if (schema == null)
			throw new IllegalArgumentException("Current schema cannot be null");

		// Execute this within a transaction
		this.writeTransactionTemplate.execute(new TransactionCallback<Void>() {
			@Override
			public Void doInTransaction(TransactionStatus status) {
				// Within a transaction
				// Build the SQL
				String createOrUpdateSql = SQLUtils.buildCreateOrUpdateRowSQL(schema,
						rowset.getTableId());
				String deleteSql = SQLUtils.buildDeleteSQL(schema, rowset.getTableId());
				SqlParameterSource[] batchUpdateOrCreateBinding = SQLUtils
						.bindParametersForCreateOrUpdate(rowset, schema);
				SqlParameterSource batchDeleteBinding = SQLUtils
						.bindParameterForDelete(rowset, schema);
				// We need a named template for this case.
				NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(template);
				if (batchUpdateOrCreateBinding.length > 0) {
					namedTemplate.batchUpdate(createOrUpdateSql,
							batchUpdateOrCreateBinding);
				}
				if (batchDeleteBinding != null) {
					namedTemplate.update(deleteSql, batchDeleteBinding);
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
	public String getCurrentSchemaMD5Hex(String tableId) {
		String sql = SQLUtils.getSchemaHashSQL(tableId);
		try {
			return template.queryForObject(sql, new SingleColumnRowMapper<String>());
		} catch (Exception e) {
			// Spring throws this when the table is empty
			return "DEFAULT";
		}
	}

	@Override
	public void deleteSecondayTables(String tableId) {
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
		rowSet.setHeaders(query.getSelectColumnModels().getSelectColumns());
		// Stream over the results and save the results in a a list
		queryAsStream(callback, query, new RowAndHeaderHandler() {
			@Override
			public void writeHeader() {
				// no-op
			}

			@Override
			public void nextRow(Row row) {
				rows.add(row);
			}

			@Override
			public void setEtag(String etag) {
				rowSet.setEtag(etag);
			}
		});
		rowSet.setTableId(query.getTableId());
		return rowSet;
	}
	
	@Override
	public boolean queryAsStream(final ProgressCallback<Void> callback, final SqlQuery query, final RowAndHeaderHandler handler) {
		ValidateArgument.required(query, "Query");
		ValidateArgument.required(callback, "ProgressCallback");
		// We use spring to create create the prepared statement
		NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(this.template);
		handler.writeHeader();
		namedTemplate.query(query.getOutputSQL(), new MapSqlParameterSource(query.getParameters()), new RowCallbackHandler() {
			@Override
			public void processRow(ResultSet rs) throws SQLException {
				// refresh the lock.
				callback.progressMade(null);

				ResultSetMetaData metadata = rs.getMetaData();

				Row row = new Row();
				List<String> values = new LinkedList<String>();
				row.setValues(values);

				// ROW_ID and ROW_VERSION are always appended to the list of columns
				int selectModelIndex = 0;
				int columnCount = metadata.getColumnCount();
				List<SelectColumn> selectColumns = query.getSelectColumnModels().getSelectColumns();
				// result sets use 1-base indexing
				for (int i = 1; i <= columnCount; i++) {
					String name = metadata.getColumnName(i);
					if (ROW_ID.equals(name)) {
						row.setRowId(rs.getLong(i));
					} else if (ROW_VERSION.equals(name)) {
						row.setVersionNumber(rs.getLong(i));
					} else {
						SelectColumn selectColumn = selectColumns.get(selectModelIndex++);
						String value = rs.getString(i);
						value = TableModelUtils.translateRowValueFromQuery(value, selectColumn.getColumnType());
						values.add(value);
					}
				}
				if (selectModelIndex != selectColumns.size()) {
					throw new IllegalStateException("The number of columns returned (" + selectModelIndex
							+ ") is not the same as the number expected (" + selectColumns.size() + ")");
				}
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

}
