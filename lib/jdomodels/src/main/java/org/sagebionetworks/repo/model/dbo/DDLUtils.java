package org.sagebionetworks.repo.model.dbo;

import java.io.IOException;
import java.util.List;

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

	/**
	 * Creates the read-only user in the repository database
	 */
	public void createRepoitoryDatabaseReadOnlyUser();

	/**
	 * Creates a user
	 *
	 * @param userName		the name of the user
	 * @param password		the password for the user
	 */
	public void createReadOnlyUser(String userName, String password, String schema);

	/**
	 * Drops a user
	 *
	 * @param userName		the name of the user
	 */
	public void dropUser(String userName);

	/**
	 * Checks if a user exists
	 *
	 * @param userName
	 * @return
	 */
	public boolean doesUserExist(String userName);

	public List<String> showGrantsForUser(String userName);

}
