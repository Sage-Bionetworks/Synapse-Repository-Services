package org.sagebionetworks.ids;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.sagebionetworks.StackConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class NamedIdGeneratorImpl implements NamedIdGenerator {
	// Create table template
	private static final String CREATE_TABLE_TEMPLATE = "CREATE TABLE %1$S (ID bigint(20) NOT NULL AUTO_INCREMENT, NAME varchar(256) CHARACTER SET latin1 COLLATE latin1_bin DEFAULT NULL, CREATED_ON bigint(20) NOT NULL, PRIMARY KEY (ID), UNIQUE KEY (NAME)) ENGINE=InnoDB AUTO_INCREMENT=0";

	// The file that defines the table
	public static String SCHEMA_FILE = "domain-id-schema.sql";
	// Insert a single row into the database
	public static final String INSERT_SQL = "INSERT INTO %1$S (NAME, CREATED_ON) VALUES (?, ?)";
	
	// This version sets the value to insert.  This is used to reserve the ID and all values less than the ID.
	public static final String INSERT_SQL_INCREMENT = "INSERT INTO %1$S (ID, CREATED_ON) VALUES (?, ?)";
	// Get the current max.
	public static final String MAX_ID = "SELECT MAX(ID) FROM %1$S";

	// Fetch the newly created id.
	public static final String GET_ID_SQL = "SELECT LAST_INSERT_ID()";
	// Determine if the table exists
	public static final String TABLE_EXISTS_SQL_PERFIX = "SELECT TABLE_NAME FROM Information_schema.tables WHERE table_name = '%1$S' AND table_schema = '%2$s'";
	
	@Autowired
	JdbcTemplate idGeneratorJdbcTemplate;
	@Autowired
	StackConfiguration stackConfiguration;
	@Autowired
	DataSourceTransactionManager idGeneratorTransactionManager;
	
	/**
	 * This call occurs in its own transaction.
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW)
	@Override
	public Long generateNewId(String name, NamedType type) {
		if(name == null) throw new IllegalArgumentException("Name cannot be null");
		if(type == null) throw new IllegalArgumentException("Type cannot be null");
		// Create a new time
		final long now = System.currentTimeMillis();
		try{
			idGeneratorJdbcTemplate.update(String.format(INSERT_SQL, type.name()), name, now);
			// Get the ID we just created.
			return idGeneratorJdbcTemplate.queryForLong(String.format(GET_ID_SQL, type.name()));
		}catch(DuplicateKeyException e){
			// When this occurs we already have a key for this name so return that
			return idGeneratorJdbcTemplate.queryForObject(String.format("SELECT ID FROM %1$s WHERE NAME = ?", type.name()), new RowMapper<Long>(){
				@Override
				public Long mapRow(ResultSet rs, int rowNum)throws SQLException {
					return rs.getLong("ID");
				}}, name);
		}

	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW)
	@Override
	public void unconditionallyAssignIdToName(final Long idToLock, String name, NamedType type) {
		if(idToLock == null) throw new IllegalArgumentException("ID to reserve cannot be null");
		if(idToLock < 1) throw new IllegalArgumentException("ID cannot be less than one because the database will treat it the same as a null and assign a new value from the auto-increment");
		if(name == null) throw new IllegalArgumentException("Name cannot be null");
		if(type == null) throw new IllegalArgumentException("Type cannot be null");
		// First we need to determine if this name has already been assigned a number?
		try{
			Long namesId =  idGeneratorJdbcTemplate.queryForLong(String.format("SELECT ID FROM %1$s where NAME = ? FOR UPDATE", type.name()), name);
			if(idToLock.equals(namesId)){
				// This name is already assigned to the given ID so there is nothing to do.
				return;
			}
			// Delete the row that is currently holding this name.
			int count =idGeneratorJdbcTemplate.update(String.format("DELETE FROM %1$s WHERE ID = ?", type.name()), namesId);
			System.out.println(count);
		}catch(EmptyResultDataAccessException e){
			// This just means the name is not already assigned.
		}
		// Now insert or update the row
		final long now = System.currentTimeMillis();
		idGeneratorJdbcTemplate.update(String.format("INSERT INTO %1$S (ID, NAME, CREATED_ON) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE NAME = ? , CREATED_ON = ?", type.name()), idToLock, name, now, name, now);
	}

	/**
	 * This will create any tables that are needed.
	 * 
	 * @throws SQLException
	 */
	public void initialize() throws SQLException {
		// Validate that the transaction manager is using auto-commit
		DataSource ds = idGeneratorTransactionManager.getDataSource();
		if(ds == null) throw new RuntimeException("Failed to get the datasource from the transaction manager");
		Connection con = ds.getConnection();
		if(con == null) throw new RuntimeException("Failed get a connecion from the datasource");
		if(!con.getAutoCommit()) throw new RuntimeException("The connections from this datasources should be set to auto-commit");
		// First make sure the table exists
		String connectionString = stackConfiguration.getIdGeneratorDatabaseConnectionUrl();
		String schema = getSchemaFromConnectionString(connectionString);
		// Make sure we have a table for each type
		for(NamedType type: NamedType.values()){
			// Does this table exist?
			String sql = String.format(TABLE_EXISTS_SQL_PERFIX, type.name(), schema);
			List<Map<String, Object>> list = idGeneratorJdbcTemplate.queryForList(sql);
			// If the table does not exist then create it.
			if(list.size() > 1) throw new RuntimeException("Found more than one table named: "+type.name());
			if(list.size() == 0){
				// Create the table 
				idGeneratorJdbcTemplate.execute(String.format(CREATE_TABLE_TEMPLATE, type.name()));
				// Make sure it exists
				List<Map<String, Object>> second = idGeneratorJdbcTemplate.queryForList(sql);
				if(second.size() != 1){
					throw new RuntimeException("Failed to create the domain table: "+type.name()+" using connection: "+connectionString);
				}
			}
		}

	}
	
	/**
	 * Extract the schema from the connection string.
	 * @param connection
	 * @return
	 */
	public static String getSchemaFromConnectionString(String connectionString){
		if(connectionString == null) throw new RuntimeException("StackConfiguration.getIdGeneratorDatabaseConnectionString() cannot be null");
		int index = connectionString.lastIndexOf("/");
		if(index < 0) throw new RuntimeException("Failed to extract the schema from the ID database connection string");
		return connectionString.substring(index+1, connectionString.length());
	}

	@Override
	public void truncateTable(NamedType type) {
		idGeneratorJdbcTemplate.execute(String.format("TRUNCATE TABLE %1$s", type.name()));
	}

}
