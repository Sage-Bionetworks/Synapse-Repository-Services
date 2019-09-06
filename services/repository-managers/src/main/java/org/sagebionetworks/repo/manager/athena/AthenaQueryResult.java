package org.sagebionetworks.repo.manager.athena;

import java.util.Iterator;

import com.amazonaws.services.athena.model.QueryExecutionStatistics;

/**
 * Represent the results of an Athena query
 * 
 * @author Marco
 *
 * @param <T>
 */
public interface AthenaQueryResult<T> {
	
	/**
	 * @return The query execution id
	 */
	String getQueryExecutionId();
	
	/**
	 * @return The statistics about the query execution
	 */
	QueryExecutionStatistics getQueryExecutionStatistics();

	/**
	 * @return A (lazy) iterator over the results
	 */
	Iterator<T> iterator();

}
