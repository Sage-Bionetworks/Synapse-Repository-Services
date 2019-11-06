package org.sagebionetworks.repo.model.athena;

import java.util.Iterator;

import org.sagebionetworks.repo.web.NotFoundException;

import com.amazonaws.services.athena.model.ResultSet;
import com.amazonaws.services.athena.model.Row;
import com.amazonaws.services.glue.model.Database;
import com.amazonaws.services.glue.model.Table;

public interface AthenaSupport {

	/**
	 * @return The S3 bucket url where the query results are saved
	 */
	String getOutputResultLocation();

	/**
	 * @return An iterator over the glue {@link Database}s available
	 */
	Iterator<Database> getDatabases();

	/**
	 * @return An iterator over the glue {@link Table}s that belong to the given {@link Database}, that belong to this stack
	 *         (e.g. whose name is parameterized by stack and instance) and that have some partition keys defined
	 */
	Iterator<Table> getPartitionedTables(Database database);

	/**
	 * Run the repair table command for athena (MSCK REPAIR TABLE) on the given table
	 * 
	 * @param table The table to repair
	 * @return The {@link AthenaQueryStatistics} containing data about the byte scanned and the running time
	 */
	AthenaQueryStatistics repairTable(Table table);

	/**
	 * Submit the repair table commnd for athena (MSCK REPAIR TABLE) on the given table
	 * 
	 * @param table The table to repair
	 * @return A query execution id
	 */
	String submitRepairTable(Table table);

	/**
	 * Returns the glue {@link Database} with the given name, the database name will be automatically parameterized by the
	 * current stack configuration.
	 * 
	 * @param database
	 * @return The glue database with the given name
	 * @throws NotFoundException        If the given database does not exist
	 * @throws IllegalArgumentException If the given database name is null or empty
	 */
	Database getDatabase(String databaseName);
	
	/**
	 * Parameterizes the given database name by the current stack instance configuration
	 * 
	 * @param databaseName
	 * @return The database name parameterized by the current stack configuration
	 * @throws IllegalArgumentException If the given database name is null or empty
	 */
	String getDatabaseName(String databaseName);

	/**
	 * Returns the glue {@link Table} with the given name within the given database, the table name and will automatically
	 * be parameterized by the current stack configuration
	 * 
	 * @param database
	 * @param tableName
	 * @return The glue table with the given name within the given database
	 * @throws NotFoundException        If the given table does not exist
	 * @throws IllegalArgumentException If the given database or table are null or empty
	 */
	Table getTable(Database database, String tableName);

	/**
	 * Parameterizes the given table name by the current stack instance configuration, can be used to perform queries
	 * 
	 * @param tableName
	 * @return The table name parameterized by the current stack configuration
	 * @throws IllegalArgumentException If the given table name is null or empty
	 */
	String getTableName(String tableName);

	/**
	 * See {@link #executeQuery(String, String, RowMapper, boolean)}, this method excludes the header from the results by
	 * default.
	 * 
	 * @param <T>       The type for the mapped {@link Row} in a {@link ResultSet}
	 * @param database  The glue {@link Database} where the query will be run against
	 * @param query     The query to run
	 * @param rowMapper A row mapper to map a {@link Row} to T
	 * @return A wrapper around the query execution that allows access to the iterator over the results and other
	 *         informations about the query
	 * @throws NotFoundException        If the given database does not exist
	 * @throws IllegalArgumentException If any of the arguments is null
	 */
	<T> AthenaQueryResult<T> executeQuery(Database database, String query, RowMapper<T> rowMapper);

	/**
	 * Executes the given query, waiting for the results and extracting them from the result set
	 * 
	 * @param <T>           The type for the mapped {@link Row} in a {@link ResultSet}
	 * @param database      The glue {@link Database} where the query will be run against
	 * @param query         The query to run
	 * @param rowMapper     A row mapper to map a {@link Row} to T
	 * @param excludeHeader True if the header of the query results should be excluded, if false the first row in the
	 *                      iterator will contain the column names of the query
	 * @return A wrapper around the query execution that allows access to the iterator over the results and other
	 *         informations about the query
	 * @throws NotFoundException        If the given database does not exist
	 * @throws IllegalArgumentException If any of the arguments is null
	 */
	<T> AthenaQueryResult<T> executeQuery(Database database, String query, RowMapper<T> rowMapper, boolean excludeHeader);

	/**
	 * Submits the given query to Athena and provide the queryExecutionId of the query
	 * 
	 * @param database The glue {@link Database} where the query will be run against
	 * @param query    The query to run
	 * @return The queryExecutionId of the query
	 */
	String submitQuery(Database database, String query);

	/**
	 * Retrieve the status of the query identified by the given queryExecutionId
	 * 
	 * @param queryExecutionId The id of the query
	 * @return The status of the query with the given id
	 */
	AthenaQueryExecution getQueryExecutionStatus(String queryExecutionId);

	/**
	 * Waits for the results of the query identified by the given queryExecutionId, note that this method will hold the
	 * current thread until the results of the given query are ready (or the query failed)
	 * 
	 * @param queryExecutionId The id of the query
	 * @return The statistics about the executed query
	 */
	AthenaQueryStatistics waitForQueryResults(String queryExecutionId);

	/**
	 * Retrieves the results of the query identified by the given queryExecutionId, using the given mapper to transform the
	 * {@link Row rows} in the {@link ResultSet}.
	 * 
	 * @param <T>
	 * 
	 * @param queryExecutionId The id of the query
	 * @param rowMapper        A row mapper to map a {@link Row} to T
	 * @param excludeHeader    True if the header of the query results should be excluded, if false the first row in the
	 *                         iterator will contain the column names of the query
	 * @return A wrapper around the query execution that allows access to the iterator over the results and other
	 *         informations about the query
	 */
	<T> AthenaQueryResult<T> getQueryResults(String queryExecutionId, RowMapper<T> rowMapper, boolean excludeHeader);

}
