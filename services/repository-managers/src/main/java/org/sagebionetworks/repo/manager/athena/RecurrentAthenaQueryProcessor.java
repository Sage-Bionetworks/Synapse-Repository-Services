package org.sagebionetworks.repo.manager.athena;

import java.util.List;

import org.sagebionetworks.repo.model.athena.RowMapper;

/**
 * Abstraction over a results processor for a specific "named" Athena query defined in the stack builder. Implementations will receive a page of results mapped with the given row mapper
 * 
 * @param <T> The resulting type of the mapped results
 */
public interface RecurrentAthenaQueryProcessor<T> {

	/**
	 * @return The name of the supported query (See the stack builder definition of athena queries)
	 */
	String getQueryName();
	
	/**
	 * @return A row mapper used to map each row in a result set
	 */
	RowMapper<T> getRowMapper();
	
	/**
	 * Process a page of results, the maximum amount of results passed is 1000 will never be null or empty
	 * 
	 * @param resultsPage A page of results mapped using the {@link #getRowMapper() provided row mapper}
	 */
	void processQueryResultsPage(List<T> resultsPage);
	
}
