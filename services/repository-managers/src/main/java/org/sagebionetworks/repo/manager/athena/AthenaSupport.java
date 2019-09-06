package org.sagebionetworks.repo.manager.athena;

import java.util.List;

import org.sagebionetworks.repo.web.NotFoundException;

import com.amazonaws.services.athena.model.QueryExecutionStatistics;
import com.amazonaws.services.athena.model.ResultSet;
import com.amazonaws.services.athena.model.Row;
import com.amazonaws.services.glue.model.Table;

public interface AthenaSupport {

	/**
	 * @return The list of glue {@link Table}s that belong to this stack and that have some partition keys defined
	 */
	List<Table> getPartitionedTables();

	/**
	 * Run the repair table command for athena (MSCK REPAIR TABLE) on the given table
	 * 
	 * @param  table The table to repair
	 * @return       The {@link QueryExecutionStatistics} contianing data about the byte scanned and the running time
	 */
	QueryExecutionStatistics repairTable(Table table);

	/**
	 * Returns the glue {@link Table} with the given name within the given database, both the table name and the database
	 * name will automatically be parameterized by the current stack configuration
	 * 
	 * @param  databaseName
	 * @param  tableName
	 * @return              The glue table with the given name within the given database
	 */
	Table getTable(String databaseName, String tableName) throws NotFoundException;

	/**
	 * 
	 * @param  databaseName
	 * @return              The database name parameterized by the current stack configuration
	 */
	String getDatabaseName(String databaseName);

	/**
	 * @param  tableName
	 * @return           The table name parameterized by the current stack configuration
	 */
	String getTableName(String tableName);

	/**
	 * Executes the given query, waiting for the results and extracting them from the result set
	 * 
	 * @param  <T>           The type for the mapped {@link Row} in a {@link ResultSet}
	 * @param  databaseName  The database name, will be automatically parameterized by the current stack configuration
	 * @param  query         The query to run
	 * @param  rowMapper     A row mapper to map a {@link Row} to T
	 * @param  batchSize     The batch size, maximum is 500
	 * @param  excludeHeader True if the header of the query results should be excluded, if false the first row in the
	 *                       iterator will contain the column names of the query
	 * @return               A wrapper around the query execution that allows access to the iterator over the results and
	 *                       other informations about the query
	 */
	<T> AthenaQueryResult<T> executeQuery(String databaseName, String query, RowMapper<T> rowMapper, int batchSize, boolean excludeHeader);

}
