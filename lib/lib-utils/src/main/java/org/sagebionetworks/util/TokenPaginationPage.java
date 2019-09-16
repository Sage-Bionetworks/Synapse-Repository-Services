package org.sagebionetworks.util;

import java.util.List;

/**
 * Represents the result of a page fetch
 * 
 * @author Marco
 */
public class TokenPaginationPage<T> {

	private String nextToken;
	private List<T> results;

	public TokenPaginationPage(List<T> results, String nextToken) {
		this.results = results;
		this.nextToken = nextToken;
	}

	/**
	 * @return The token for the next page, might be null if no more results can be fetched
	 */
	public String getNextToken() {
		return nextToken;
	}

	/**
	 * @return The page results
	 */
	public List<T> getResults() {
		return results;
	}
}
