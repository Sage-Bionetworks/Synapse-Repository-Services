package org.sagebionetworks.repo.model.dbo;

import java.io.IOException;

/**
 * This is a utility for Data Definition Language (DDL) statements.
 * 
 * @author jmhill
 *
 */
public interface DDLUtils {

	/**
	 * Validate that a table exists. If it does not create it using the passed schema file.
	 * @param mapping
	 * @return
	 * @throws IOException
	 */
	public boolean validateTableExists(TableMapping mapping) throws IOException;
	
	/**
	 * Drop the given table.
	 * @param tableName
	 * @return 
	 */
	public int dropTable(String tableName);

	/**
	 * Create a MySQL function from a DDL file that defines the function.
	 * 
	 * @param functionName The name of the function to create/update
	 * @param fileName
	 *            The name of the DDL file that defines the function.
	 * @throws IOException 
	 */
	void createFunction(String functionName, String fileName) throws IOException;
	
	/**
	 * Create a function using the provided definition.
	 * @param definition
	 * @throws IOException
	 */
	void createFunction(String definition) throws IOException;
	
	/**
	 * Drop a function.
	 * @param functionName
	 */
	void dropFunction(String functionName);
	
	/**
	 * Does a function with the given name exist?
	 * 
	 * @param functionName
	 * @return
	 */
	public boolean doesFunctionExist(String functionName);

}
