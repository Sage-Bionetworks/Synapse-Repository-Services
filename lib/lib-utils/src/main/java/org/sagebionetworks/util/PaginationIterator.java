package org.sagebionetworks.util;

import java.util.Iterator;
import java.util.List;
/**
 * Generic wrapper that allows paginated results to be iterated over.
 *
 * @param <T>
 */
public class PaginationIterator<T> implements Iterator<T>  {
	
	private PaginationProvider<T> provider;
	private Iterator<T> currentPage;
	private final long limit;
	private long offset;
	private boolean isDone;

	/**
	 * Create a new Iterator to wrap the given provider, using the given page size.
	 * @param provider
	 * @param limit The limit to be used when fetching pages from the provider.
	 */
	public PaginationIterator(PaginationProvider<T> provider, long limit) {
		if(provider == null) {
			throw new IllegalArgumentException("Provider cannot be null");
		}
		this.limit = limit;
		this.provider = provider;
		offset = 0L;
		isDone = false;
	}
	
	@Override
	public boolean hasNext() {
		if(isDone) {
			return false;
		}
		if(currentPage == null || !currentPage.hasNext()) {
			// Fetch the next page
			List<T> page = provider.getNextPage(limit, offset);
			offset += limit;
			currentPage = page.iterator();
			boolean hasNext =  currentPage.hasNext();
			if(!hasNext) {
				// done when a page returns no results.
				isDone = true;
			}
			return hasNext;
		}
		return true;
	}

	@Override
	public T next() {
		if(currentPage == null) {
			throw new IllegalStateException("hasNext() must be called before next()"); 
		}
		return currentPage.next();
	}

}
