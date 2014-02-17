package org.sagebionetworks.table.cluster;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.RowSet;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.RowMapper;
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
		// explicitly setting the transaction name is something that can only be
		// done programmatically
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


}
