package org.sagebionetworks.table.cluster;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

public class TableIndexDAOImpl implements TableIndexDAO{

	private static final String FIELD = "Field";
	private static final String SQL_SHOW_COLUMNS = "SHOW COLUMNS FROM ";

	@Override
	public boolean createOrUpdateTable(SimpleJdbcTemplate connection, List<ColumnModel> schema, String tableId) {
		// First determine if we have any columns for this table yet
		List<String> columns = getCurrentTableColumns(connection, tableId);
		return false;
	}

	@Override
	public boolean deleteTable(SimpleJdbcTemplate connection, String tableId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<String> getCurrentTableColumns(SimpleJdbcTemplate connection, String tableId) {
		String tableName = SQLUtils.getTableNameForId(tableId);
		// Bind variables do not seem to work here
		try {
			return connection.query(SQL_SHOW_COLUMNS+tableName, new RowMapper<String>() {
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
	

}
