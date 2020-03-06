package org.sagebionetworks.ids;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;

/**
 * This class creates domain unique ids using a MySql sequence via
 * AUTO_INCREMENT of a primary key. For a full explanation of how this
 * works @See
 * http://dev.mysql.com/doc/refman/5.1/en/information-functions.html#function_last-insert-id.
 * 
 * @author jmhill
 *
 */
public class IdGeneratorImpl implements IdGenerator, InitializingBean {

	private static final String SELECT_COUNT_FROM_TYPE = "SELECT COUNT(*) FROM %1$S";

	private static final String DELETE_LESS_THAN_MAX = "DELETE FROM %1$S WHERE ID < %2$d LIMIT %3$d";

	private static final String CREATE_ID_GENERATOR_SEMAPHORE = "CREATE TABLE IF NOT EXISTS ID_GENERATOR_SEMAPHORE ("
			+ "TYPE_LOCK VARCHAR(100) NOT NULL," + " PRIMARY KEY (TYPE_LOCK)" + ")";

	// Create table template
	private static final String CREATE_TABLE_TEMPLATE = "CREATE TABLE IF NOT EXISTS %1$S ("
			+ " ID BIGINT NOT NULL AUTO_INCREMENT," + " CREATED_ON BIGINT NOT NULL," + " PRIMARY KEY (ID)"
			+ ") ENGINE=InnoDB AUTO_INCREMENT=0";

	// Insert a single row into the database
	public static final String INSERT_SQL = "INSERT INTO %1$S (CREATED_ON) VALUES (?)";

	// This version sets the value to insert. This is used to reserve the ID and all
	// values less than the ID.
	public static final String INSERT_SQL_INCREMENT = "INSERT INTO %1$S (ID, CREATED_ON) VALUES (?, ?)";
	// This version is used to create a restore script.
	public static final String INSERT_SQL_INCREMENT_EXPORT = "INSERT IGNORE INTO %1$S (ID, CREATED_ON) VALUES (%2$d, UNIX_TIMESTAMP()*1000)";
	// Get the current max.
	public static final String MAX_ID = "SELECT MAX(ID) FROM %1$S";

	// Fetch the newly created id.
	public static final String GET_ID_SQL = "SELECT LAST_INSERT_ID()";

	@Autowired
	JdbcTemplate idGeneratorJdbcTemplate;

	/**
	 * <p>
	 * This is a call to a separate database so it does not participate in the
	 * caller's transaction. Do not add transaction annotations to any method in
	 * this class. This class does not use a transaction manager and each call will
	 * auto-commit.
	 * <p>
	 * Note: A transaction is not required to call the LAST_INSERT_ID() method. The
	 * call is bound to each client's connection (not the transaction). See:
	 * https://dev.mysql.com/doc/refman/5.7/en/getting-unique-id.html
	 */
	@Override
	public Long generateNewId(IdType type) {
		// Create a new time
		final long now = System.currentTimeMillis();
		idGeneratorJdbcTemplate.update(String.format(INSERT_SQL, type.name()), new PreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps) throws SQLException {
				ps.setLong(1, now);

			}
		});
		// Get the ID we just created.
		return idGeneratorJdbcTemplate.queryForObject(String.format(GET_ID_SQL, type.name()), Long.class);
	}

	/**
	 * <p>
	 * This is a call to a separate database so it does not participate in the
	 * caller's transaction. Do not add transaction annotations to any method in
	 * this class. This class does not use a transaction manager and each call will
	 * auto-commit.
	 * </p>
	 */
	@Override
	public void reserveId(final Long idToLock, IdType type) {
		if (idToLock == null) {
			throw new IllegalArgumentException("ID to reserve cannot be null");
		}
		// First check if this value is greater than the last value
		long max = getMaxValueForType(type);
		if (idToLock > max) {
			final long now = System.currentTimeMillis();
			idGeneratorJdbcTemplate.update(String.format(INSERT_SQL_INCREMENT, type.name()),
					new PreparedStatementSetter() {
						@Override
						public void setValues(PreparedStatement ps) throws SQLException {
							ps.setLong(1, idToLock);
							ps.setLong(2, now);
						}
					});
		}
	}

	/**
	 * Lookup the max value for a given type.
	 * 
	 * @param type
	 * @return
	 */
	@Override
	public long getMaxValueForType(IdType type) {
		try {
			return idGeneratorJdbcTemplate.queryForObject(String.format(MAX_ID, type.name()), Long.class);
		} catch (NullPointerException e) {
			// max = 0
			return 0L;
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// Validate that the transaction manager is using auto-commit
		DataSource ds = idGeneratorJdbcTemplate.getDataSource();
		if (ds == null)
			throw new RuntimeException("Failed to get the datasource from the transaction manager");
		Connection con = ds.getConnection();
		if (con == null)
			throw new RuntimeException("Failed get a connection from the datasource");
		if (!con.getAutoCommit())
			throw new RuntimeException("The connections from this datasources should be set to auto-commit");

		// Create the table for semaphore locks
		idGeneratorJdbcTemplate.execute(CREATE_ID_GENERATOR_SEMAPHORE);

		// Make sure we have a table for each type
		for (IdType type : IdType.values()) {
			// Create the ID table for this type.
			idGeneratorJdbcTemplate.execute(String.format(CREATE_TABLE_TEMPLATE, type.name()));
			// If the type has a start id, then reserver it
			if (type.startingId != null) {
				reserveId(type.startingId, type);
			}
		}
	}

	@Override
	public String createRestoreScript() {
		StringBuilder builder = new StringBuilder();
		for (IdType type : IdType.values()) {
			createRestoreScript(builder, type);
		}
		return builder.toString();
	}

	/**
	 * Create a restore script for the given type.
	 * 
	 * @param builder
	 * @param type
	 */
	@Override
	public void createRestoreScript(StringBuilder builder, IdType type) {
		builder.append("# ").append(type.name()).append("\n");
		// Add the create statement for the table
		builder.append(String.format(CREATE_TABLE_TEMPLATE, type.name())).append(";\n");
		long maxValue = getMaxValueForType(type);
		builder.append(String.format(INSERT_SQL_INCREMENT_EXPORT, type.name(), maxValue)).append(";\n");
	}

	/**
	 * <p>
	 * This is a call to a separate database so it does not participate in the
	 * caller's transaction. Do not add transaction annotations to any method in
	 * this class. This class does not use a transaction manager and each call will
	 * auto-commit.
	 * </p>
	 */
	@Override
	public void cleanupType(IdType type, long rowLimit) {
		// Determine the max value
		long maxId = getMaxValueForType(type);
		String deleteSql = String.format(DELETE_LESS_THAN_MAX, type.name(), maxId, rowLimit);
		this.idGeneratorJdbcTemplate.execute(deleteSql);
	}

	@Override
	public long getRowCount(IdType type) {
		return idGeneratorJdbcTemplate.queryForObject(String.format(SELECT_COUNT_FROM_TYPE, type.name()), Long.class);
	}

}
