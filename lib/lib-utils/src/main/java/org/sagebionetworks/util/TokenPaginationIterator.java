package org.sagebionetworks.util;

import java.util.Iterator;

/**
 * Generic wrapper that allows paginated results that use a next page token to be iterated over.
 *
 * @author Marco
 *
 * @param <T>
 */
public class TokenPaginationIterator<T> implements Iterator<T> {

	private TokenPaginationProvider<T> resulsProvider;
	private Iterator<T> currentPage;
	private String nextToken;

	public TokenPaginationIterator(TokenPaginationProvider<T> resultsProvider) {
		ValidateArgument.required(resultsProvider, "provider");
		this.resulsProvider = resultsProvider;
	}

	@Override
	public boolean hasNext() {
		// Load the next page only if the iterator is at the end and there is a next token
		if (currentPage == null || (!currentPage.hasNext() && nextToken != null)) {
			TokenPaginationPage<T> nextPage = resulsProvider.getNextPage(nextToken);
			currentPage = nextPage.getResults().iterator();
			nextToken = nextPage.getNextToken();
		}
		return currentPage.hasNext();
	}

	@Override
	public T next() {
		if (currentPage == null) {
			throw new IllegalStateException("hasNext() must be called before next()");
		}
		return currentPage.next();
	}

}
