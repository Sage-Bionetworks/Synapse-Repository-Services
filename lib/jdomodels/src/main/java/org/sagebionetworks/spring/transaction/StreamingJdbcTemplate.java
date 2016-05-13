package org.sagebionetworks.spring.transaction;

import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 
 * 
 * By default the MySQL driver will read all query results into memory which can
 * cause memory problems for large query results. (see: <a hreft=
 * "http://dev.mysql.com/doc/connector-j/en/connector-j-reference-implementation-notes.html"
 * />) According to the MySQL driver docs the only way to get the driver to
 * change this default behavior is to create a statement with TYPE_FORWARD_ONLY
 * & CONCUR_READ_ONLY and then set statement fetch size to Integer.MIN_VALUE.
 * However, JdbcTemplate will not set a fetch size less than zero. Therefore, we
 * must override the JdbcTemplate to force the fetch size of Integer.MIN_VALUE.
 * See: PLFM-3429
 * 
 *
 */
public class StreamingJdbcTemplate extends JdbcTemplate {

	public StreamingJdbcTemplate() {
		// See comments above.
		this.setFetchSize(Integer.MIN_VALUE);
	}

	public StreamingJdbcTemplate(DataSource dataSource) {
		super(dataSource);
		// See comments above.
		this.setFetchSize(Integer.MIN_VALUE);
	}

	public StreamingJdbcTemplate(DataSource dataSource, boolean lazyInit) {
		super(dataSource, lazyInit);
		// See comments above.
		this.setFetchSize(Integer.MIN_VALUE);
	}

	/**
	 * Overridden to allow a fetch size of Integer.MIN_VALUE to applied to a
	 * statement. By default JdbcTemplate will not apply a fetch size less than one.
	 * See comments above.
	 */
	@Override
	protected void applyStatementSettings(Statement stmt) throws SQLException {
		super.applyStatementSettings(stmt);
		if (getFetchSize() == Integer.MIN_VALUE) {
			stmt.setFetchSize(getFetchSize());
		}
	}

}
