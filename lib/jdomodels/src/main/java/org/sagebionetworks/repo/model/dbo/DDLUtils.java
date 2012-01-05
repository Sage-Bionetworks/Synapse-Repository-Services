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

}
