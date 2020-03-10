package org.sagebionetworks.ids;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;

import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * This class creates domain unique ids using a MySql sequence via AUTO_INCREMENT of a primary key.
 * For a full explanation of how this works @See http://dev.mysql.com/doc/refman/5.1/en/information-functions.html#function_last-insert-id.
 * @author jmhill
 *
 */
public class IdGeneratorImpl implements IdGenerator, InitializingBean{
	
	private static final String SELECT_COUNT_FROM_TYPE = "SELECT COUNT(*) FROM %1$S";

	private static final String DELETE_LESS_THAN_MAX = "DELETE FROM %1$S WHERE ID < %2$d LIMIT %3$d";

	// Create table template
	private static final String CREATE_TABLE_TEMPLATE =
			"CREATE TABLE IF NOT EXISTS %1$S ("
			+ " ID BIGINT NOT NULL AUTO_INCREMENT,"
			+ " CREATED_ON BIGINT NOT NULL,"
			+ " PRIMARY KEY (ID)"
			+ ") ENGINE=InnoDB AUTO_INCREMENT=0";

	
	// This version is used to create a restore script.
	public static final String INSERT_SQL_INCREMENT_EXPORT = "INSERT IGNORE INTO %1$S (ID, CREATED_ON) VALUES (%2$d, UNIX_TIMESTAMP()*1000)";
	// Get the current max.
	public static final String MAX_ID = "SELECT MAX(ID) FROM %1$S";
	
	@Autowired
	JdbcTemplate idGeneratorJdbcTemplate;

	/**
	 * Note: This is a call to a separate database and does not participate in the
	 * caller's transaction. Do not add transaction annotations to this method.
	 */
	@Override
	public Long generateNewId(IdType type) {
		if (type == null) {
			throw new IllegalArgumentException("Type cannot be null");
		}
		return idGeneratorJdbcTemplate.queryForObject("CALL generateNewId(?)", Long.class, type.name());
	}
	
	/**
	 * Note: This is a call to a separate database and does not participate in the
	 * caller's transaction. Do not add transaction annotations to this method.
	 */
	@Override
	public void reserveId(final Long idToLock, IdType type) {
		if(idToLock == null) {
			throw new IllegalArgumentException("ID to reserve cannot be null");
		}
		if (type == null) {
			throw new IllegalArgumentException("Type cannot be null");
		}
		idGeneratorJdbcTemplate.queryForObject("CALL reserveId(?,?)", Long.class, idToLock, type.name());
	}

	/**
	 * Lookup the max value for a given type.
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
		if(ds == null) throw new RuntimeException("Failed to get the datasource from the transaction manager");
		Connection con = ds.getConnection();
		if(con == null) throw new RuntimeException("Failed get a connection from the datasource");
		if(!con.getAutoCommit()) throw new RuntimeException("The connections from this datasources should be set to auto-commit");
		
		createStoredProcedure("generateNewId.ddl.sql");
		createStoredProcedure("reserveId.ddl.sql");
		
		// Make sure we have a table for each type
		for(IdType type: IdType.values()){
			// Create the ID table for this type.
			idGeneratorJdbcTemplate.execute(String.format(CREATE_TABLE_TEMPLATE, type.name()));
			// If the type has a start id, then reserver it
			if(type.startingId != null){
				reserveId(type.startingId, type);
			}
		}
	}
	
	public void createStoredProcedure(String name) {
		String ddl = loadDDLString(name);
		try {
			idGeneratorJdbcTemplate.execute(ddl);
		} catch (DataAccessException e) {
			if(!e.getMessage().contains("already exists")) {
				throw e;
			}
		}
	}
	
	/**
	 * Load a DDL string from the classpath.
	 * @param name
	 * @return
	 */
	public static String loadDDLString(String name) {
		try (InputStream in = IdGeneratorImpl.class.getClassLoader().getResourceAsStream(name);) {
			if (in == null) {
				throw new IllegalArgumentException("Cannot find: " + name + " on the classpath");
			}
			return IOUtils.toString(in, "UTF-8");
		} catch (IOException e1) {
			throw new RuntimeException(e1);
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
	 * Note: This is a call to a separate database and does not participate in the
	 * caller's transaction. Do not add transaction annotations to this method.
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
