package org.sagebionetworks.repo.model.athena;

/**
 * The status information of a query execution
 * 
 * @author Marco
 *
 */
public interface AthenaQueryExecution {

	/**
	 * @return The id of the query execution
	 */
	String getQueryExecutionId();

	/**
	 * @return The statistics about the current query execution
	 */
	AthenaQueryStatistics getStatistics();

	/**
	 * @return The query execution state
	 */
	AthenaQueryExecutionState getState();

	/**
	 * @return The reason of the state change if any
	 */
	String getStateChangeReason();

}
