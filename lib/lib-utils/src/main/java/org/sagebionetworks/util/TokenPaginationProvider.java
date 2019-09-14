package org.sagebionetworks.util;

/**
 * Provider interface to load the next page of results given a next page token
 * 
 * @author Marco
 */
public interface TokenPaginationProvider<T> {

	/**
	 * Fetch the next page of results according to the given nextToken
	 * 
	 * @param nextToken The token for the next page
	 * @return The next page of results
	 */
	TokenPaginationPage<T> getNextPage(String nextToken);

}
