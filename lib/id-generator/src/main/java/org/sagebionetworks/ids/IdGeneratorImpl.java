package org.sagebionetworks.ids;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.sagebionetworks.StackConfiguration;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class creates domain unique ids using a MySql sequence via AUTO_INCREMENT of a primary key.
 * For a full explanation of how this works @See http://dev.mysql.com/doc/refman/5.1/en/information-functions.html#function_last-insert-id.
 * @author jmhill
 *
 */
@Transactional(readOnly = false)
public class IdGeneratorImpl implements IdGenerator, InitializingBean{
	
	// The table name
	public static String TABLE_DOMAIN_ID = "DOMAIN_IDS";
	// The file that defines the table
	public static String SCHEMA_FILE = "domain-id-schema.sql";
	// Insert a single row into the database
	public static final String INSERT_SQL = "INSERT INTO "+TABLE_DOMAIN_ID+" (CREATED_ON) VALUES (?)";
	// Fetch the newly created id.
	public static final String GET_ID_SQL = "SELECT LAST_INSERT_ID()";
	// Determine if the table exists
	public static final String TABLE_EXISTS_SQL_PERFIX = "SELECT TABLE_NAME FROM Information_schema.tables WHERE table_name = '"+TABLE_DOMAIN_ID+"' AND table_schema = '";
	
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
	public Long generateNewId() {
		// Create a new time
		final long now = System.currentTimeMillis();
		idGeneratorJdbcTemplate.update(INSERT_SQL, new PreparedStatementSetter(){
			@Override
			public void setValues(PreparedStatement ps) throws SQLException {
				ps.setLong(1, now);
				
			}});
		// Get the ID we just created.
		return idGeneratorJdbcTemplate.queryForLong(GET_ID_SQL);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// Validate that the transacion manager is using auto-commit
		DataSource ds = idGeneratorTransactionManager.getDataSource();
		if(ds == null) throw new RuntimeException("Failed to get the datasource from the transaction manager");
		Connection con = ds.getConnection();
		if(con == null) throw new RuntimeException("Failed get a connecion from the datasource");
		if(!con.getAutoCommit()) throw new RuntimeException("The connections from this datasources should be set to auto-commit");
		// First make sure the table exists
		String connectionString = stackConfiguration.getIdGeneratorDatabaseConnectionString();
		String schema = getSchemaFromConnectionString(connectionString);
		String sql = TABLE_EXISTS_SQL_PERFIX+schema+"'";
		List<Map<String, Object>> list = idGeneratorJdbcTemplate.queryForList(sql);
		// If the table does not exist then create it.
		if(list.size() > 1) throw new RuntimeException("Found more than one table named: "+TABLE_DOMAIN_ID);
		if(list.size() == 0){
			// Create the table 
			String tableDDL = loadSchemaSql();
			idGeneratorJdbcTemplate.execute(tableDDL);
			// Make sure it exists
			List<Map<String, Object>> second = idGeneratorJdbcTemplate.queryForList(sql);
			if(second.size() != 1){
				throw new RuntimeException("Failed to create the domain table: "+TABLE_DOMAIN_ID+" using connection: "+connectionString);
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
	
	/**
	 * Load the schema file from the classpath.
	 * @return
	 * @throws IOException 
	 */
	public static String loadSchemaSql() throws IOException{
		InputStream in = IdGeneratorImpl.class.getClassLoader().getResourceAsStream(SCHEMA_FILE);
		if(in == null){
			throw new RuntimeException("Failed to load the schema file from the classpath: "+SCHEMA_FILE);
		}
		try{
			StringWriter writer = new StringWriter();
			byte[] buffer = new byte[1024];
			int count = -1;
			while((count = in.read(buffer, 0, buffer.length)) >0){
				writer.write(new String(buffer, 0, count, "UTF-8"));
			}
			return writer.toString();
		}finally{
			in.close();
		}
	}

}
