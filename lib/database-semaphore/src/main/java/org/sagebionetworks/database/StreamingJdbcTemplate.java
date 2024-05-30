package org.sagebionetworks.database;

import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * By default the MySQL driver will read all query results into memory which can
 * cause memory problems for large query results. (see: <a hreft=
 * "http://dev.mysql.com/doc/connector-j/en/connector-j-reference-implementation-notes.html"
 * />) According to the MySQL driver docs the only way to get the driver to
 * change this default behavior is to create a statement with TYPE_FORWARD_ONLY
 * & CONCUR_READ_ONLY and then set statement fetch size to Integer.MIN_VALUE.
 * However, JdbcTemplate will not set a fetch size less than zero. Therefore, we
 * must override the JdbcTemplate to force the fetch size of Integer.MIN_VALUE.
 * See: PLFM-3429
 */
public class StreamingJdbcTemplate extends JdbcTemplate {

	public StreamingJdbcTemplate() {
		super();
		this.setFetchSize(Integer.MIN_VALUE);
	}

	public StreamingJdbcTemplate(DataSource dataSource, boolean lazyInit) {
		super(dataSource, lazyInit);
		this.setFetchSize(Integer.MIN_VALUE);
	}

	public StreamingJdbcTemplate(DataSource dataSource) {
		super(dataSource);
		this.setFetchSize(Integer.MIN_VALUE);
	}

	/**
	 * It is no longer necessary to override this method in spring 4.3.20.RELEASE +
	 * as {@link #applyStatementSettings(Statement)} method will now forward the
	 * fetch size of Integer.MIN_VALUE to the provided statement.
	 */
	@Override
	protected void applyStatementSettings(Statement stmt) throws SQLException {
		super.applyStatementSettings(stmt);
	}
}
