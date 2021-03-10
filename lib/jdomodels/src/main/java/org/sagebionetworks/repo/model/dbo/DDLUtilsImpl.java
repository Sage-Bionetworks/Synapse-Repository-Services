package org.sagebionetworks.repo.model.dbo;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * This is a utility for Data Definition Language (DDL) statements.
 * @author John
 *
 */
public class DDLUtilsImpl implements DDLUtils{

	private static final String DROP_FUNCTION = "DROP FUNCTION ";

	private static final String SHOW_CREATE_FUNCTION = "SHOW CREATE FUNCTION ";

	private static final String FUNCTION_ALREADY_EXISTS = "Function already exists: ";

	private static final String ALREADY_EXISTS = "already exists";

	static private Logger log = LogManager.getLogger(DDLUtilsImpl.class);
	
	// Determine if the table exists
	public static final String TABLE_EXISTS_SQL_FORMAT = "SELECT TABLE_NAME FROM Information_schema.tables WHERE TABLE_NAME = '%1$s' AND table_schema = '%2$s'";
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	StackConfiguration stackConfiguration;
	
	/**
	 * If the given table does not already exist, then create it using the provided SQL file
	 * @param databaseTableName
	 * @param DDLSqlFileName
	 * @throws IOException 
	 */
	public boolean validateTableExists(TableMapping mapping) throws IOException{
		String schema = stackConfiguration.getRepositoryDatabaseSchemaName();
		log.debug("Schema: "+schema);
		String sql = String.format(TABLE_EXISTS_SQL_FORMAT, mapping.getTableName(), schema);
		log.debug("About to execute: "+sql);
		List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
		// If the table does not exist then create it.
		if(list.size() > 1) throw new RuntimeException("Found more than one table named: "+mapping.getTableName());
		if(list.size() == 0){
			log.info("Creating table: "+mapping.getTableName());
			// Create the table 
			String tableDDL = loadSchemaSql(mapping);
			if(log.isDebugEnabled()){
				log.debug(tableDDL);
			}
			jdbcTemplate.update(tableDDL);
			// Make sure it exists
			List<Map<String, Object>> second = jdbcTemplate.queryForList(sql);
			if(second.size() != 1){
				throw new RuntimeException("Failed to create the table: "+mapping.getTableName()+" for schema: "+schema);
			}
			// the table did not exist until this call
			return false;
		}else{
			// the table already exists
			return true;
		}
	}
	
	/**
	 * Load the schema file from the mapper.
	 * 
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings("rawtypes")
	public static String loadSchemaSql(TableMapping mapping) throws IOException {
		if (mapping instanceof AutoTableMapping) {
			return ((AutoTableMapping) mapping).getDDL();
		} else {
			return loadSQLFromClasspath(mapping.getDDLFileName());
		}
	}

	/**
	 * Load a SQL file from the classpath.
	 * 
	 * @return
	 * @throws IOException
	 */
	public static String loadSQLFromClasspath(String fileName) {
		try(InputStream in = DDLUtilsImpl.class.getClassLoader().getResourceAsStream(fileName)){
			if(in == null){
				throw new RuntimeException("Failed to load the schema file from the classpath: "+fileName);
			}
			String sql =  IOUtils.toString(in, StandardCharsets.UTF_8.name());
			return removeSQLComments(sql);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Helper to remove multi-line SQL comments from the given SQL string.
	 * @param sql
	 * @return
	 */
	public static String removeSQLComments(String sql) {
		// simple state machine to ignore comments
		StringBuilder builder = new StringBuilder();
		boolean withinMultiLineComment = false;
		for (int i=0; i < sql.length(); i++) {
			char c = sql.charAt(i);
			if(i+1<sql.length() && c == '/' && sql.charAt(i+1) == '*') {
				withinMultiLineComment = true;
				i++;
				continue;
			}
			if(i+1<sql.length() && c == '*' && sql.charAt(i+1) == '/') {
				withinMultiLineComment = false;
				i++;
				continue;
			}
			if(!withinMultiLineComment) {
				builder.append(c);
			}
		}
		return builder.toString();
	}

	@Override
	public int dropTable(String tableName) {
		try{
			return jdbcTemplate.update("DROP TABLE "+tableName);
		}catch (BadSqlGrammarException e){
			// This means the table does not exist
			return 0;			
		}
	}
	

	@Override
	public void createFunction(String functionName, String fileName)
			throws IOException {
		// Only create the function if it does not exist (see PLFM-4393)
		if(doesFunctionExist(functionName)){
			log.info(FUNCTION_ALREADY_EXISTS+functionName);
			return;
		}
		String functionDefinition = loadSQLFromClasspath(fileName);
		try {
			// create the function from its definition
			createFunction(functionDefinition);
			log.info("Created function: "+functionName);
		} catch (DataAccessException e) {
			/*
			 * Even though we check if a function exists before creating it, a
			 * race condition could exist so we must handle the already-exists
			 * errors.
			 */
			if(e.getMessage().contains(ALREADY_EXISTS)){
				log.info("Race condition: "+FUNCTION_ALREADY_EXISTS+functionName);
			}else{
				throw e;
			}
		}
	}

	@Override
	public boolean doesFunctionExist(String functionName) {
		try{
			jdbcTemplate.queryForList(SHOW_CREATE_FUNCTION+functionName);
			return true;
		}catch (DataAccessException e){
			return false;
		}
	}

	@Override
	public void dropFunction(String functionName) {
		try{
			jdbcTemplate.update(DROP_FUNCTION+functionName);
		}catch (BadSqlGrammarException e){
			// function does not exist		
		}
	}

	@Override
	public void createFunction(String definition) throws IOException {
		// create the function from its definition
		jdbcTemplate.update(definition);
	}

}
