package org.sagebionetworks.table.cluster;

import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_VERSION;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.sagebionetworks.repo.model.dao.table.RowAndHeaderHandler;
import org.sagebionetworks.repo.model.dbo.SinglePrimaryKeySqlParameterSource;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.jdbc.BadSqlGrammarException;
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

public class TableIndexDAOImpl implements TableIndexDAO {

	private static final String FIELD = "Field";
	private static final String SQL_SHOW_COLUMNS = "SHOW COLUMNS FROM ";

	DataSourceTransactionManager transactionManager;
	TransactionTemplate transactionTemplate;
	JdbcTemplate template;

	/**
	 * The IoC constructor.
	 * 
	 * @param template
	 * @param transactionManager
	 */
	public TableIndexDAOImpl(DataSource dataSource) {
		super();
		this.transactionManager = new DataSourceTransactionManager(dataSource);
		// This will define how transaction are run for this instance.
		DefaultTransactionDefinition transactionDef = new DefaultTransactionDefinition();
		transactionDef.setIsolationLevel(Connection.TRANSACTION_READ_COMMITTED);
		transactionDef.setReadOnly(false);
		transactionDef.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
		transactionDef.setName("TableIndexDAOImpl");
		// This will manage transactions for calls that need it.
		this.transactionTemplate = new TransactionTemplate(this.transactionManager, transactionDef);
		this.template = new JdbcTemplate(dataSource);
	}

	@Override
	public boolean createOrUpdateTable(List<ColumnModel> newSchema,
			String tableId) {
		// First determine if we have any columns for this table yet
		List<String> columns = getCurrentTableColumns(tableId);
		// Convert the names to columnIDs
		List<String> oldSchema = SQLUtils.convertColumnNamesToColumnId(columns);
		// Build the SQL to create or update the table
		String dml = SQLUtils.creatOrAlterTableSQL(oldSchema, newSchema,
				tableId);
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
		String dropTableDML = SQLUtils.dropTableSQL(tableId);
		try {
			template.update(dropTableDML);
			return true;
		} catch (BadSqlGrammarException e) {
			// This is thrown when the table does not exist
			return false;
		}
	}

	@Override
	public List<String> getCurrentTableColumns(String tableId) {
		String tableName = SQLUtils.getTableNameForId(tableId);
		// Bind variables do not seem to work here
		try {
			return template.query(SQL_SHOW_COLUMNS + tableName,
					new RowMapper<String>() {
						@Override
						public String mapRow(ResultSet rs, int rowNum)
								throws SQLException {
							return rs.getString(FIELD);
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
		this.transactionTemplate.execute(new TransactionCallback<Void>() {
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
		} catch (BadSqlGrammarException e) {
			// Spring throws this when the table does not exist
			return -1L;
		}
	}

	@Override
	public void setMaxCurrentCompleteVersionForTable(String tableId, Long version) {
		String createStatusTableSql = SQLUtils.createStatusTableSQL(tableId);
		template.update(createStatusTableSql);

		String createOrUpdateStatusSql = SQLUtils.buildCreateOrUpdateStatusSQL(tableId);
		template.update(createOrUpdateStatusSql, version);
	}

	@Override
	public void deleteStatusTable(String tableId) {
		String dropStatusTableDML = SQLUtils.dropStatusTableSQL(tableId);
		try {
			template.update(dropStatusTableDML);
		} catch (BadSqlGrammarException e) {
			// This is thrown when the table does not exist
		}
	}

	@Override
	public JdbcTemplate getConnection() {
		return template;
	}

	@Override
	public RowSet query(final SqlQuery query) {
		if (query == null)
			throw new IllegalArgumentException("SqlQuery cannot be null");
		final List<Row> rows = new LinkedList<Row>();
		final RowSet rowSet = new RowSet();
		rowSet.setRows(rows);
		// Stream over the results and save the results in a a list
		queryAsStream(query, new RowAndHeaderHandler() {
			@Override
			public void nextRow(Row row) {
				rows.add(row);
			}
			
			@Override
			public void setHeaderColumnIds(List<String> headers) {
				rowSet.setHeaders(headers);
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
	public boolean queryAsStream(final SqlQuery query, final RowAndHeaderHandler handler) {
		if(query == null) throw new IllegalArgumentException("Query cannot be null");
		final List<String> headers = new LinkedList<String>();
		final List<Integer> nonMetadataColumnIndicies = new LinkedList<Integer>();
		final Map<Integer, ColumnModel> modeledColumns = Maps.newHashMap();
		// We use spring to create create the prepared statement
		NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(this.template);
		namedTemplate.query(query.getOutputSQL(), new MapSqlParameterSource(query.getParameters()), new RowCallbackHandler() {
			@Override
			public void processRow(ResultSet rs) throws SQLException {
				ResultSetMetaData metadata = rs.getMetaData();
				if (headers.isEmpty()) {
					// Read the headers from the result set
					populateHeadersFromResultsSet(headers,
							nonMetadataColumnIndicies, query, modeledColumns, metadata);
					handler.setHeaderColumnIds(headers);
				}
				// Read the results into a new list
				List<String> values = new LinkedList<String>();
				Row row = new Row();
				row.setValues(values);
				if (!query.isAggregatedResult()) {
					// Non-aggregate queries include two extra columns,
					// row id and row version.
					row.setRowId(rs.getLong(ROW_ID));
					row.setVersionNumber(rs.getLong(ROW_VERSION));
				}
				// fill the value list
				for (Integer index : nonMetadataColumnIndicies) {
					String value = rs.getString(index);
					ColumnModel columnModel = modeledColumns.get(index);
					value = TableModelUtils.translateRowValueFromQuery(value, columnModel);
					values.add(value);
				}
				handler.nextRow(row);
			}
		});
		return true;
	}

	static void populateHeadersFromResultsSet(List<String> headers, List<Integer> nonMetadataColumnIndicies, SqlQuery query,
			Map<Integer, ColumnModel> modeledColumns, ResultSetMetaData resultSetMetaData) throws SQLException {
		// There are three possibilities, column ID, aggregate function, or row
		// metadata.
		Map<Long, ColumnModel> columnIdToModelMap = TableModelUtils.createIDtoColumnModelMap(query.getcolumnNameToModelMap().values());
		int columnCount = resultSetMetaData.getColumnCount();
		for (int i = 1; i < columnCount + 1; i++) {
			String name = resultSetMetaData.getColumnName(i);
			// If this is a real column then we can extract the ColumnId.
			if (ROW_ID.equals(name) || ROW_VERSION.equals(name)) {
				// We do not include row id or row version in the headers.
				continue;
			}
			if (SQLUtils.isColumnName(name)) {
				// Extract the column ID
				String columnId = SQLUtils.getColumnIdForColumnName(name);
				headers.add(columnId);
				if (columnIdToModelMap != null) {
					ColumnModel columnModel = columnIdToModelMap.get(Long.parseLong(columnId));
					if (columnModel != null) {
						modeledColumns.put(i, columnModel);
					}
				}
			} else {
				// replace column names where possible
				name = SQLUtils.replaceColumnNames(name, columnIdToModelMap);
				headers.add(name);
			}
			// This is not a row_id or row_version column.
			nonMetadataColumnIndicies.add(i);
		}
	}

}
