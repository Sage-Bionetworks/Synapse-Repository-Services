package org.sagebionetworks.repo.model.athena;

import java.util.List;

import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;

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
	 * @return                             The list of glue {@link Table}s that belong to this stack and that have some
	 *                                     partition keys defined
	 * @throws ServiceUnavailableException If an error occurs contacting AWS glue
	 */
	List<Table> getPartitionedTables() throws ServiceUnavailableException;

	/**
	 * Run the repair table command for athena (MSCK REPAIR TABLE) on the given table
	 * 
	 * @param  table                       The table to repair
	 * @return                             The {@link AthenaQueryStatistics} containing data about the byte scanned and the
	 *                                     running time
	 * @throws ServiceUnavailableException If an error occurs contacting AWS Athena
	 */
	AthenaQueryStatistics repairTable(Table table) throws ServiceUnavailableException;

	/**
	 * Returns the glue {@link Database} with the given name, the database name will be automatically parameterized by the
	 * current stack configuration.
	 * 
	 * @param  database
	 * @return                             The glue database with the given name
	 * @throws NotFoundException           If the given database does not exist
	 * @throws IllegalArgumentException    If the given database name is null or empty
	 * @throws ServiceUnavailableException If an error occurs contacting AWS Glue
	 */
	Database getDatabase(String databaseName) throws ServiceUnavailableException;

	/**
	 * Returns the glue {@link Table} with the given name within the given database, the table name and will automatically
	 * be parameterized by the current stack configuration
	 * 
	 * @param  database
	 * @param  tableName
	 * @return                             The glue table with the given name within the given database
	 * @throws NotFoundException           If the given table does not exist
	 * @throws IllegalArgumentException    If the given database or table are null or empty
	 * @throws ServiceUnavailableException If an error occurs contacting AWS Glue
	 */
	Table getTable(Database database, String tableName) throws ServiceUnavailableException;

	/**
	 * See {@link #executeQuery(String, String, RowMapper, boolean)}, this method excludes the header from the results by
	 * default.
	 * 
	 * @param  <T>                         The type for the mapped {@link Row} in a {@link ResultSet}
	 * @param  database                    The glue {@link Database} where the query will be run against
	 * @param  query                       The query to run
	 * @param  rowMapper                   A row mapper to map a {@link Row} to T
	 * @return                             A wrapper around the query execution that allows access to the iterator over the
	 *                                     results and other informations about the query
	 * @throws NotFoundException           If the given database does not exist
	 * @throws IllegalArgumentException    If any of the arguments is null
	 * @throws ServiceUnavailableException If an error occurs contacting AWS Athena
	 */
	<T> AthenaQueryResult<T> executeQuery(Database database, String query, RowMapper<T> rowMapper) throws ServiceUnavailableException;

	/**
	 * Executes the given query, waiting for the results and extracting them from the result set
	 * 
	 * @param  <T>                         The type for the mapped {@link Row} in a {@link ResultSet}
	 * @param  database                    The glue {@link Database} where the query will be run against
	 * @param  query                       The query to run
	 * @param  rowMapper                   A row mapper to map a {@link Row} to T
	 * @param  excludeHeader               True if the header of the query results should be excluded, if false the first
	 *                                     row in the iterator will contain the column names of the query
	 * @return                             A wrapper around the query execution that allows access to the iterator over the
	 *                                     results and other informations about the query
	 * @throws NotFoundException           If the given database does not exist
	 * @throws IllegalArgumentException    If any of the arguments is null
	 * @throws ServiceUnavailableException If an error occurs contacting AWS Athena
	 */
	<T> AthenaQueryResult<T> executeQuery(Database database, String query, RowMapper<T> rowMapper, boolean excludeHeader)
			throws ServiceUnavailableException;

	/**
	 * Parameterizes the given table name by the current stack instance configuration, can be used to perform queries
	 * 
	 * @param  tableName
	 * @return                          The table name parameterized by the current stack configuration
	 * @throws IllegalArgumentException If the given table name is null or empty
	 */
	String getTableName(String tableName);
}
