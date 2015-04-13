package org.sagebionetworks.ids;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.transactions.NewWriteTransaction;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.sagebionetworks.repo.transactions.WriteTransaction;

/**
 * This class creates domain unique ids using a MySql sequence via AUTO_INCREMENT of a primary key.
 * For a full explanation of how this works @See http://dev.mysql.com/doc/refman/5.1/en/information-functions.html#function_last-insert-id.
 * @author jmhill
 *
 */
public class IdGeneratorImpl implements IdGenerator, InitializingBean{
	
	// Create table template
	private static final String CREATE_TABLE_TEMPLATE = "CREATE TABLE %1$S (ID bigint(20) NOT NULL AUTO_INCREMENT, CREATED_ON bigint(20) NOT NULL, PRIMARY KEY (ID)) ENGINE=InnoDB AUTO_INCREMENT=0";

	// The file that defines the table
	public static String SCHEMA_FILE = "domain-id-schema.sql";
	// Insert a single row into the database
	public static final String INSERT_SQL = "INSERT INTO %1$S (CREATED_ON) VALUES (?)";
	
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

	/**
	 * This call occurs in its own transaction.
	 */
	@NewWriteTransaction
	@Override
	public Long generateNewId() {
		// Use the default domain
		return generateNewId(TYPE.DOMAIN_IDS);
	}

	/**
	 * This call occurs in its own transaction.
	 */
	@NewWriteTransaction
	@Override
	public Long generateNewId(TYPE type) {
		// Create a new time
		final long now = System.currentTimeMillis();
		idGeneratorJdbcTemplate.update(String.format(INSERT_SQL, type.name()), new PreparedStatementSetter(){
			@Override
			public void setValues(PreparedStatement ps) throws SQLException {
				ps.setLong(1, now);
				
			}});
		// Get the ID we just created.
		return idGeneratorJdbcTemplate.queryForLong(String.format(GET_ID_SQL, type.name()));
	}
	
	@NewWriteTransaction
	@Override
	public void reserveId(final Long idToLock, TYPE type) {
		if(idToLock == null) throw new IllegalArgumentException("ID to reserve cannot be null");
		// First check if this value is greater than the last value
		Long max = idGeneratorJdbcTemplate.queryForLong(String.format(MAX_ID, type.name()));
		if(idToLock > max){
			final long now = System.currentTimeMillis();
			idGeneratorJdbcTemplate.update(String.format(INSERT_SQL_INCREMENT, type.name()), new PreparedStatementSetter(){
				@Override
				public void setValues(PreparedStatement ps) throws SQLException {
					ps.setLong(1, idToLock);
					ps.setLong(2, now);
				}});
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// Validate that the transacion manager is using auto-commit
		DataSource ds = idGeneratorJdbcTemplate.getDataSource();
		if(ds == null) throw new RuntimeException("Failed to get the datasource from the transaction manager");
		Connection con = ds.getConnection();
		if(con == null) throw new RuntimeException("Failed get a connecion from the datasource");
		if(!con.getAutoCommit()) throw new RuntimeException("The connections from this datasources should be set to auto-commit");
		// First make sure the table exists
		String connectionString = stackConfiguration.getIdGeneratorDatabaseConnectionUrl();
		String schema = getSchemaFromConnectionString(connectionString);
		// Make sure we have a table for each type
		for(TYPE type: TYPE.values()){
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

}
