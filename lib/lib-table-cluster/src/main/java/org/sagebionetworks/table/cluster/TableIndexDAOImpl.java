package org.sagebionetworks.table.cluster;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.RowSet;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

public class TableIndexDAOImpl implements TableIndexDAO{

	private static final String FIELD = "Field";
	private static final String SQL_SHOW_COLUMNS = "SHOW COLUMNS FROM ";
	
	SimpleJdbcTemplate template;
	DataSourceTransactionManager transactionManager;
	
	/**
	 * The IoC constructor.
	 * @param template
	 * @param transactionManager
	 */
	public TableIndexDAOImpl(SimpleJdbcTemplate template,
			DataSourceTransactionManager transactionManager) {
		super();
		this.template = template;
		this.transactionManager = transactionManager;
	}

	@Override
	public boolean createOrUpdateTable(List<ColumnModel> newSchema, String tableId) {
		// First determine if we have any columns for this table yet
		List<String> columns = getCurrentTableColumns(tableId);
		// Convert the names to columnIDs
		List<String> oldSchema = SQLUtils.convertColumnNamesToColumnId(columns);
		// Build the SQL to create or update the table
		String dml = SQLUtils.creatOrAlterTableSQL(oldSchema, newSchema, tableId);
		// If there is nothing to apply then do nothing
		if(dml == null) return false;
		// Execute the DML
		template.update(dml);
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
			return template.query(SQL_SHOW_COLUMNS+tableName, new RowMapper<String>() {
				@Override
				public String mapRow(ResultSet rs, int rowNum) throws SQLException {
					return rs.getString(FIELD);
				}
			});
		} catch (BadSqlGrammarException e) {
			// Spring throws this when the table does not
			return null;
		}
	}

	@Override
	public int[] createOrUpdateRows(RowSet rowset, List<ColumnModel> schema) {
		if (rowset == null)
			throw new IllegalArgumentException("Rowset cannot be null");
		if (schema == null)
			throw new IllegalArgumentException("Current schema cannot be null");
		// Build the SQL
		String sql = SQLUtils.buildCreateOrUpdateRowSQL(schema,	rowset.getTableId());
		SqlParameterSource[] batchBinding = SQLUtils.bindParametersForCreateOrUpdate(rowset, schema);
		// Execute this within a transaction
		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		def.setName("createOrUpdateRows");
		def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
		TransactionStatus status = transactionManager.getTransaction(def);
		try {
			int[] results = template.batchUpdate(sql, batchBinding);
			transactionManager.commit(status);
			return results;
		} catch (Exception ex) {
			transactionManager.rollback(status);
			throw new RuntimeException(ex);
		}
	}
	
	@Override
	public Long getRowCountForTable(String tableId) {
		String sql = SQLUtils.getCountSQL(tableId);
		try {
			return template.queryForLong(sql);
		} catch (BadSqlGrammarException e) {
			// Spring throws this when the table does not
			return null;
		}
	}

	@Override
	public Long getMaxVersionForTable(String tableId) {
		// First we need to know if the table exists and is not empty
		Long count = getRowCountForTable(tableId);
		if(count == null){
			// the table does not exist so we return null for the max
			return null;
		}
		if(count < 1){
			// the table is empty we return -1 for the max
			return -1L;
		}
		String sql = SQLUtils.getMaxVersionSQL(tableId);
		try {
			return template.queryForLong(sql);
		}catch (BadSqlGrammarException e) {
			// Spring throws this when the table does not
			return null;
		}
	}

	@Override
	public SimpleJdbcTemplate getConnection() {
		return template;
	}

	@Override
	public RowSet query(final SqlQuery query) {
		if(query == null) throw new IllegalArgumentException("SqlQuery cannot be null");
		final List<String> headers = new LinkedList<String>();
		final List<Integer> nonMetadataColumnIndicies = new LinkedList<Integer>();
		// Get the rows for this query from the database.
		List<Row> rows = this.template.query(query.getOutputSQL(), new RowMapper<Row>() {
			@Override
			public Row mapRow(ResultSet rs, int rowNum) throws SQLException {
				ResultSetMetaData metadata =  rs.getMetaData();
				if(headers.isEmpty()){
					// Read the headers from the result set
					populateHeadersFromResultsSet(headers, nonMetadataColumnIndicies, metadata);
				}
				// Read the results into a new list
				List<String> values = new LinkedList<String>();
				Row row = new Row();
				row.setValues(values);
				if(!query.isAggregatedResult()){
					// Non-aggregate queries include two extra columns, row id and row version.
					row.setRowId(rs.getLong(SQLUtils.ROW_ID));
					row.setVersionNumber(rs.getLong(SQLUtils.ROW_VERSION));
				}
				// fill the value list
				for(Integer index: nonMetadataColumnIndicies){
					values.add(rs.getString(index));
				}
				return row;
			}
		}, new MapSqlParameterSource(query.getParameters()));
		RowSet rowSet = new RowSet();
		rowSet.setHeaders(headers);
		rowSet.setRows(rows);
		// Set the tableId
		rowSet.setTableId("syn"+query.getModel().getTableExpression().getFromClause().getTableReference().getTableName());
		
		return rowSet;
	}
	
	static void populateHeadersFromResultsSet(List<String> headers, List<Integer> nonMetadataColumnIndicies, ResultSetMetaData resultSetMetaData) throws SQLException{
		// There are three possibilities, column ID, aggregate function, or row metadata.
		int columnCount = resultSetMetaData.getColumnCount();
		for(int i=1; i<columnCount+1; i++){
			String name = resultSetMetaData.getColumnName(i);
			// If this is a real column then we can extract the ColumnId.
			if(SQLUtils.ROW_ID.equals(name) || SQLUtils.ROW_VERSION.equals(name)){
				// We do not include row id or row version in the headers.
				continue;
			}
			if(SQLUtils.hasColumnPrefixe(name) && !name.startsWith("COUNT")){
				// Extract the column ID
				headers.add(SQLUtils.getColumnIdForColumnName(name));
			}else{
				// Return what was provided unchanged
				headers.add(name);
			}
			// This is not a row_id or row_version column.
			nonMetadataColumnIndicies.add(new Integer(i));
		}
	}

}
