package org.sagebionetworks.repo.model.athena;

import java.util.Iterator;

/**
 * Represent the results of an Athena query
 * 
 * @author     Marco
 *
 * @param  <T>
 */
public interface AthenaQueryResult<T> {

	/**
	 * @return True if the iterator includes the header as the first element
	 */
	boolean includeHeader();

	/**
	 * @return The query execution id
	 */
	String getQueryExecutionId();

	/**
	 * @return The statistics about the query execution
	 */
	AthenaQueryStatistics getQueryExecutionStatistics();

	/**
	 * @return A (lazy) iterator over the results
	 */
	Iterator<T> getQueryResultsIterator();

}
