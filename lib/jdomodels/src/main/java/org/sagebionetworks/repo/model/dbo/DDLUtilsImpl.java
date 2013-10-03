package org.sagebionetworks.repo.model.dbo;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

/**
 * This is a utility for Data Definition Language (DDL) statements.
 * @author John
 *
 */
public class DDLUtilsImpl implements DDLUtils{
	
	static private Logger log = LogManager.getLogger(DDLUtilsImpl.class);
	
	// Determine if the table exists
	public static final String TABLE_EXISTS_SQL_FORMAT = "SELECT TABLE_NAME FROM Information_schema.tables WHERE TABLE_NAME = '%1$s' AND table_schema = '%2$s'";
	
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	@Autowired
	StackConfiguration stackConfiguration;
	
	/**
	 * If the given table does not already exist, then create it using the provided SQL file
	 * @param tableName
	 * @param DDLSqlFileName
	 * @throws IOException 
	 */
	public boolean validateTableExists(TableMapping mapping) throws IOException{
		String url = stackConfiguration.getRepositoryDatabaseConnectionUrl();
		String schema = getSchemaFromConnectionString(url);
		log.debug("Schema: "+schema);
		String sql = String.format(TABLE_EXISTS_SQL_FORMAT, mapping.getTableName(), schema);
		log.debug("About to execute: "+sql);
		List<Map<String, Object>> list = simpleJdbcTemplate.queryForList(sql);
		// If the table does not exist then create it.
		if(list.size() > 1) throw new RuntimeException("Found more than one table named: "+mapping.getTableName());
		if(list.size() == 0){
			log.info("Creating table: "+mapping.getTableName());
			// Create the table 
			String tableDDL = loadSchemaSql(mapping.getDDLFileName());
			if(log.isDebugEnabled()){
				log.debug(tableDDL);
			}
			simpleJdbcTemplate.update(tableDDL);
			// Make sure it exists
			List<Map<String, Object>> second = simpleJdbcTemplate.queryForList(sql);
			if(second.size() != 1){
				throw new RuntimeException("Failed to create the table: "+mapping.getTableName()+" using connection: "+url);
			}
			// the table did not exist until this call
			return false;
		}else{
			// the table already exists
			return true;
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
	public static String loadSchemaSql(String fileName) throws IOException{
		InputStream in = DDLUtilsImpl.class.getClassLoader().getResourceAsStream(fileName);
		if(in == null){
			throw new RuntimeException("Failed to load the schema file from the classpath: "+fileName);
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

	@Override
	public int dropTable(String tableName) {
		try{
			return simpleJdbcTemplate.update("DROP TABLE "+tableName);
		}catch (BadSqlGrammarException e){
			// This means the table does not exist
			return 0;			
		}

	}

}
