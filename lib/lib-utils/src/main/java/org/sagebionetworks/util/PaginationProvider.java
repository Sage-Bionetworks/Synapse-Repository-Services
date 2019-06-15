package org.sagebionetworks.util;

import java.util.List;

/**
 * Abstraction for fetching pages of data using limit and offset to define the
 * page.
 *
 * @param <T>
 */
public interface PaginationProvider<T> {

	/**
	 * Get the next page for the given limit and offset.
	 * 
	 * @param limit  Limits the number of elements to be returned per call.
	 * @param offset Offset represent the address to start reading from (inclusive).
	 *               The first offset is zero.
	 * @return
	 */
	List<T> getNextPage(long limit, long offset);
}
